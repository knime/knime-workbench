/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Nov 25, 2020 (hornm): created
 */
package org.knime.workbench.searchhubview;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.util.SWTUtilities;

/**
 * Job to either open or, if not available, install the hub view.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class InstallOrOpenHubViewJob extends Job {

    private static final String HUB_VIEW_FEATURE = "com.knime.features.workbench.cef";

    private static final String FEATURE_GROUP_SUFFIX = ".feature.group";

    private static final String HUB_VIEW_ID = "com.knime.workbench.hubview";

    private IEclipseContext m_context;

    /**
     * @param context the eclipse context to get access to the {@link EPartService}
     */
    public InstallOrOpenHubViewJob(final IEclipseContext context) {
        super("Loading Hub Integration");
        m_context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        if (!openHubView()) {
            Display.getDefault().asyncExec(() -> {
                String message = "The Hub Integration is not installed, yet. Do you want to install it?\n\n"
                    + "Note: Once the Hub Integration is installed it is recommended to rearrange your KNIME workbench."
                    + " This can be done automatically with 'View > Reset Perspective...'.";
                if (MessageDialog.openQuestion(SWTUtilities.getActiveShell(), "Install Hub Integration", message)) {
                    installHubViewExtension();
                }
            });
        }
        return Status.OK_STATUS;
    }

    private boolean openHubView() {
        EPartService partService = m_context.get(EPartService.class);
        AtomicReference<MPart> view = new AtomicReference<>(partService.findPart(HUB_VIEW_ID));
        if (view.get() == null) {
            view.set(partService.createPart(HUB_VIEW_ID));
            MApplication app = m_context.get(MApplication.class);
            EModelService modelService = m_context.get(EModelService.class);
            // try to open it next to the node description if it's opened for the first time
            Display.getDefault().syncExec(() -> {
                MUIElement el = modelService.find("org.knime.workbench.helpview", app).getParent();
                if (el instanceof MPartStack) {
                    ((MPartStack)el).getChildren().add(view.get());
                }
            });
        }
        if (view.get() == null) {
            return false;
        }
        Display.getDefault().syncExec(() -> partService.showPart(view.get(), PartState.ACTIVATE));
        return true;
    }

    private static void installHubViewExtension() {
        startInstallExtension(findExtension());
    }

    private static Collection<IInstallableUnit> findExtension() {
        try {
            final IMetadataRepositoryManager metadataManager = getMetadataRepositoryManager();
            final Set<IInstallableUnit> extensionsFound = new HashSet<>();

            for (URI uri : metadataManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)) {
                IMetadataRepository repo = metadataManager.loadRepository(uri, null);
                searchInRepository(repo, extensionsFound);
            }

            if (extensionsFound.isEmpty()) {
                Display.getDefault().syncExec(() -> MessageDialog.openWarning(Display.getDefault().getActiveShell(),
                    "Hub Integration Extension not found",
                    "The Hub Integration Extension ('" + HUB_VIEW_FEATURE + FEATURE_GROUP_SUFFIX + "') not found."));
            } else {
                //
            }
            return extensionsFound;

        } catch (ProvisionException ex) {
            Display.getDefault()
                .syncExec(() -> MessageDialog.openWarning(Display.getDefault().getActiveShell(),
                    "Error while installing extension", "Error while installign extension '" + HUB_VIEW_FEATURE
                        + FEATURE_GROUP_SUFFIX + "': " + ex.getMessage()));
            NodeLogger.getLogger(InstallOrOpenHubViewJob.class).error("Error while installing extension '"
                + HUB_VIEW_FEATURE + FEATURE_GROUP_SUFFIX + "': " + ex.getMessage(), ex);
            return Collections.emptySet();

        }
    }

    private static IMetadataRepositoryManager getMetadataRepositoryManager() {
        final ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
        return (IMetadataRepositoryManager)session.getProvisioningAgent()
            .getService(IMetadataRepositoryManager.SERVICE_NAME);
    }

    private static void searchInRepository(final IMetadataRepository repository,
        final Set<IInstallableUnit> featuresToInstall) throws ProvisionException {
        final IQuery<IInstallableUnit> query = createQuery();
        final IQueryResult<IInstallableUnit> result = repository.query(query, null);

        result.forEach(featuresToInstall::add);
    }

    private static IQuery<IInstallableUnit> createQuery() {
        final IQuery<IInstallableUnit> query =
            QueryUtil.createLatestQuery(QueryUtil.createIUQuery(HUB_VIEW_FEATURE + FEATURE_GROUP_SUFFIX));
        return query;
    }

    private static void startInstallExtension(final Collection<IInstallableUnit> featuresToInstall) {
        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
        final LoadMetadataRepositoryJob loadJob = new LoadMetadataRepositoryJob(provUI);
        loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));
        loadJob.setProperty(LoadMetadataRepositoryJob.SUPPRESS_AUTHENTICATION_JOB_MARKER, Boolean.toString(true));

        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(final IJobChangeEvent event) {
                if (PlatformUI.isWorkbenchRunning() && event.getResult().isOK()) {
                    Display.getDefault().asyncExec(() -> {
                        if (Display.getDefault().isDisposed()) {
                            NodeLogger.getLogger(InstallOrOpenHubViewJob.class)
                                .debug("Display disposed, aborting install action");
                            return;
                        }

                        provUI.getPolicy().setRepositoriesVisible(false);
                        provUI.openInstallWizard(featuresToInstall,
                            new InstallOperation(provUI.getSession(), featuresToInstall), loadJob);
                        provUI.getPolicy().setRepositoriesVisible(true);
                    });
                }
            }
        });

        loadJob.setUser(true);
        loadJob.schedule();
    }
}
