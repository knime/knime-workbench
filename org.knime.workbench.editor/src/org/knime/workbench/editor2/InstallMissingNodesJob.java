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
 *   14.03.2016 (thor): created
 */
package org.knime.workbench.editor2;

import static org.knime.core.node.util.ConvenienceMethods.distinctByKey;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.data.container.storage.TableStoreFormatInformation;
import org.knime.core.node.KNIMEComponentInformation;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.util.Version;
import org.knime.workbench.core.imports.UpdateSiteInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Job that checks all enabled update sites for extensions that provided missing nodes. It will open an installation
 * dialog when the features have been found.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public final class InstallMissingNodesJob extends Job {

    /**
     * Pattern to split a KNIME update site url into three groups: (i) the protocol, (ii) the main part and (iii) the
     * version. Example: https://update.knime.com/analytics-platform/4.2
     */
    private static final Pattern KNIME_UPDATE_SITE_URL_PATTERN =
        Pattern.compile("(http://|https://)(.+)(\\d+\\.\\d+\\.*\\d*)");

    final List<KNIMEComponentInformation> m_missingComponents = new ArrayList<>();

    private final UpdateSiteInfo m_updateSiteInfo;

    /**
     * Creates a new job.
     *
     * @param missingNodes a list of information about missing nodes
     * @param missingTableFormats list of missing table formats (fully qualified names), not null.
     */
    InstallMissingNodesJob(final List<NodeAndBundleInformationPersistor> missingNodes,
        final List<TableStoreFormatInformation> missingTableFormats) {
        super("Find missing extensions");
        missingNodes.stream().filter(distinctByKey(i -> i.getFactoryClass())).forEach(m_missingComponents::add);
        missingTableFormats.stream().distinct().collect(Collectors.toList()).forEach(m_missingComponents::add);
        m_updateSiteInfo = null;
    }

    /**
     * Creates a new job.
     *
     * @param missingComponents a list of missing components
     * @param siteInfo optional information about the update site that provides the missing extension, can be
     *            <code>null</code>
     */
    public InstallMissingNodesJob(final List<KNIMEComponentInformation> missingComponents,
        final UpdateSiteInfo siteInfo) {
        super("Find missing extensions");
        m_updateSiteInfo = siteInfo;
        m_missingComponents.addAll(missingComponents);
    }

    @Override
    public IStatus run(final IProgressMonitor monitor) {
        return doRun(monitor, false);
    }

    private IStatus doRun(final IProgressMonitor monitor, final boolean newUpdateSiteAdded) {
        Set<IInstallableUnit> featuresToInstall = new HashSet<>();
        IStatus status = findExtensions(monitor, m_missingComponents, featuresToInstall);
        if (!status.isOK()) {
            return status;
        } else if (featuresToInstall.isEmpty()) {
            if (!newUpdateSiteAdded && checkAndAddUpdateSite(m_updateSiteInfo)) {
                // try again with new a update site available
                return doRun(monitor, true);
            } else {
                StringBuilder message =
                    new StringBuilder("Could not find any extension(s) that provides the missing node(s).");
                if (isUpdateSiteOfNewerVersionThanAP(m_updateSiteInfo)) {
                    message.append("\n\nThe update site is of a newer version. Try updating your Analytics Platform.");
                }
                Display.getDefault().asyncExec(() -> {
                    MessageDialog.openWarning(SWTUtilities.getActiveShell(), "No suitable extension found",
                        message.toString());
                });
                return Status.OK_STATUS;
            }
        } else {
            if (!m_missingComponents.isEmpty()) {
                if (!newUpdateSiteAdded && checkAndAddUpdateSite(m_updateSiteInfo)) {
                    // try again with a new update site available
                    return doRun(monitor, true);
                } else {
                    StringBuilder message =
                        new StringBuilder("No extensions for the following nodes were found: " + m_missingComponents
                            .stream().map(i -> i.getComponentName()).collect(Collectors.joining(", ")));
                    if (isUpdateSiteOfNewerVersionThanAP(m_updateSiteInfo)) {
                        message
                            .append("\n\nThe update site is of a newer version. Try updating your Analytics Platform.");
                    }
                    Display.getDefault().syncExec(() -> {
                        MessageDialog.openWarning(SWTUtilities.getActiveShell(), "Not all extensions found",
                            message.toString());
                    });
                }
            }
            startInstallJob(featuresToInstall);
            return Status.OK_STATUS;
        }
    }

    private static void startInstallJob(final Set<IInstallableUnit> featuresToInstall) {
        final ProvisioningUI provUI = ProvisioningUI.getDefaultUI();
        Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
        final LoadMetadataRepositoryJob loadJob = new LoadMetadataRepositoryJob(provUI);
        loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));

        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(final IJobChangeEvent event) {
                if (PlatformUI.isWorkbenchRunning() && event.getResult().isOK()) {
                    Display.getDefault().asyncExec(() -> {
                        if (Display.getDefault().isDisposed()) {
                            NodeLogger.getLogger("Display disposed, aborting install action");
                            return; // fixes AP-8376, AP-8380, AP-7184
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

    private IStatus findExtensions(final IProgressMonitor monitor,
        final List<KNIMEComponentInformation> missingComponents, final Set<IInstallableUnit> featuresToInstall) {
        Bundle myself = FrameworkUtil.getBundle(getClass());
        try {
            IMetadataRepositoryManager metadataManager = getMetaRepositoryManager();

            for (URI uri : metadataManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL)) {
                IMetadataRepository repo = metadataManager.loadRepository(uri, monitor);

                if (!missingComponents.isEmpty()) {
                    for (Iterator<KNIMEComponentInformation> it = missingComponents.iterator(); it.hasNext();) {
                        KNIMEComponentInformation info = it.next();
                        if (searchInRepository(repo, info, monitor, featuresToInstall)) {
                            it.remove();
                        }
                    }
                }
            }
            return Status.OK_STATUS;
        } catch (ProvisionException ex) {
            NodeLogger.getLogger(getClass()).error("Could not create provisioning agent: " + ex.getMessage(), ex);
            return new Status(IStatus.ERROR, myself.getSymbolicName(),
                "Could not query updates site for missing extensions", ex);
        }
    }

    private static IMetadataRepositoryManager getMetaRepositoryManager() {
        ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
        return (IMetadataRepositoryManager)session.getProvisioningAgent()
            .getService(IMetadataRepositoryManager.SERVICE_NAME);
    }

    private static boolean searchInRepository(final IMetadataRepository repository,
        final KNIMEComponentInformation componentInfo, final IProgressMonitor monitor,
        final Set<IInstallableUnit> featuresToInstall) throws ProvisionException, OperationCanceledException {
        if (componentInfo.getFeatureSymbolicName().isPresent()) {
            IQuery<IInstallableUnit> query =
                QueryUtil.createLatestQuery(QueryUtil.createIUQuery(componentInfo.getFeatureSymbolicName().get()));
            IQueryResult<IInstallableUnit> result = repository.query(query, monitor);

            // the result is empty after the iterator has been used (Eclipse bug?)
            boolean empty = result.isEmpty();
            result.forEach(i -> featuresToInstall.add(i));
            return !empty;
        } else if (componentInfo.getBundleSymbolicName().isPresent()) {
            IQuery<IInstallableUnit> bundleQuery =
                QueryUtil.createLatestQuery(QueryUtil.createIUQuery(componentInfo.getBundleSymbolicName().get()));
            IQueryResult<IInstallableUnit> bundleResult = repository.query(bundleQuery, monitor);

            if (bundleResult.isEmpty()) {
                return false;
            }

            // try to find feature that contains the bundle
            IQuery<IInstallableUnit> featureQuery = QueryUtil.createLatestQuery(QueryUtil.createQuery(
                "everything.select(y | y.properties ~= filter(\"(org.eclipse.equinox.p2.type.group=true)\") "
                    + "&& everything.select(x | x.id == $0).exists(r | y.requirements.exists(v | r ~= v)))",
                bundleResult.iterator().next().getId()));
            IQueryResult<IInstallableUnit> featureResult = repository.query(featureQuery, monitor);

            // the result is empty after the iterator has been used (Eclipse bug?)
            if (featureResult.isEmpty()) {
                bundleResult.forEach(featuresToInstall::add);
            } else {
                featureResult.forEach(featuresToInstall::add);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks and potentially adds/enables an update site. Might open warning/question dialogs while doing it.
     *
     * @return <code>true</code> if a new update site has been added/enabled
     */
    private static boolean checkAndAddUpdateSite(final UpdateSiteInfo info) {
        if (info == null) {
            return false;
        }
        IMetadataRepositoryManager repoManager = getMetaRepositoryManager();
        if (Arrays.stream(repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL))
            .anyMatch(uri -> compareUpdateSiteUrls(uri.toString(), info.getUrl()))) {
            // update site already enabled
            return false;
        }

        Optional<URI> disabledUri =
            Arrays.stream(repoManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED))
                .filter(uri -> compareUpdateSiteUrls(uri.toString(), info.getUrl())).findFirst();
        if (disabledUri.isPresent()) {
            // enable the new update site
            if (askWhetherToProceedEnablingOrAddingUpdateSite(info, false)) {
                repoManager.setEnabled(disabledUri.get(), true);
                return true;
            }
            return false;
        } else {
            // check whether the new update site can be reached and add it
            URI newUri = getUri(info.getUrl());
            if (newUri != null) {
                if (askWhetherToProceedEnablingOrAddingUpdateSite(info, true)) {
                    repoManager.addRepository(newUri);
                    repoManager.setRepositoryProperty(newUri, IRepository.PROP_NAME, info.getName());
                    return true;
                }
                return false;
            } else {
                return false;
            }
        }
    }

    private static boolean askWhetherToProceedEnablingOrAddingUpdateSite(final UpdateSiteInfo info,
        final boolean addedAsNewUpdateSite) {
        String[] dialogButtonLabels = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
        String title = addedAsNewUpdateSite ? "Add update site?" : "Enable update site?";
        StringBuilder message =
            new StringBuilder("The required extension is not part of any available update site.");
        message.append("\nDo you want to" + (addedAsNewUpdateSite ? " add " : " enable ")
            + "the following update site?\n\n" + info.getName() + "\n" + info.getUrl());
        if (!info.isTrusted()) {
            message.append(
                "\n\nIMPORTANT: You are about to install a third-party extension that has not been reviewed by KNIME. "
                    + "It might not meet the quality or security criteria that are required for trusted extensions.");
        }
        AtomicBoolean res = new AtomicBoolean();
        Display.getDefault().syncExec(() -> {
            MessageDialog dialog = new MessageDialog(SWTUtilities.getActiveShell(), title, null, message.toString(),
                info.isTrusted() ? MessageDialog.INFORMATION : MessageDialog.WARNING, dialogButtonLabels, 0);
            res.set(dialog.open() == 0);
        });
        return res.get();
    }

    /**
     * If both provided urls match the {@link #KNIME_UPDATE_SITE_URL_PATTERN}-pattern, the main part of the urls will be compared. Otherwise
     * the urls are compared exactly as provided.
     *
     * @return <code>true</code> if the urls are regarded as equal
     */
    private static boolean compareUpdateSiteUrls(final String url1, final String url2) {
        Matcher matcher1 = KNIME_UPDATE_SITE_URL_PATTERN.matcher(url1);
        Matcher matcher2 = KNIME_UPDATE_SITE_URL_PATTERN.matcher(url2);
        if (!matcher1.matches() || !matcher2.matches() || matcher1.groupCount() != 3 || matcher2.groupCount() != 3) {
            return url1.equals(url2);
        }
        return matcher1.group(2).equals(matcher2.group(2));
    }

    /**
     * Checks for the existence of an URL by doing a 'head' request.
     *
     * @param url the url to test
     * @return <code>null</code> if the string couldn't be converted into an uri or the uri doesn't exists
     */
    private static URI getUri(final String s) {
        try {
            return new URL(s).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            NodeLogger.getLogger(InstallMissingNodesJob.class).warn("Not a valid update site url: " + s, e);
            Display.getDefault().syncExec(() -> {
                MessageDialog.openError(SWTUtilities.getActiveShell(), "Not a valid update site URL",
                    "The update site which contains the missing extension is invalid." + " The update site URL is:\n\n"
                        + s);
            });
            return null;
        }
    }

    private static boolean isUpdateSiteOfNewerVersionThanAP(final UpdateSiteInfo info) {
        if (info != null && info.getVersion() != null) {
            return new Version(info.getVersion()).compareTo(new Version(KNIMEConstants.VERSION)) > 0;
        } else {
            return false;
        }
    }

}

