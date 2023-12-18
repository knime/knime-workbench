/*
 * ------------------------------------------------------------------------
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
 *   Dec 18, 2006 (sieb): created
 */
package org.knime.workbench.ui.p2.actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.dialogs.UpdateSingleIUWizard;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.knime.core.eclipseUtil.UpdateChecker;
import org.knime.core.eclipseUtil.UpdateChecker.UpdateInfo;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.MutableBoolean;
import org.knime.core.util.Version;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.util.IRegisteredServerInfoService.ServerAndExecutorVersions;

/**
 * Custom action to open the install wizard. In addition to just opening the update manager, it also checks if there is
 * a new major or minor release available.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class InvokeUpdateAction extends AbstractP2Action {

    private static final Pattern AP_PROGRAM_NAME = Pattern.compile("KNIME Analytics Platform \\d+.*");

    private class NewReleaseCheckerJob extends Job {
        NewReleaseCheckerJob() {
            super("KNIME release checker");
            setUser(true);
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            final var provUI = ProvisioningUI.getDefaultUI();
            URI[] repositories = provUI.getRepositoryTracker().getKnownRepositories(provUI.getSession());

            monitor.beginTask("Checking for new KNIME release", repositories.length);

            final List<UpdateInfo> updateInfos = new ArrayList<>();
            for (URI uri : repositories) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && (uri.getHost() != null)
                    && (uri.getHost().endsWith(".knime.org") || uri.getHost().endsWith(".knime.com"))) {
                    try {
                        monitor.subTask("Checking " + uri.toString());
                        UpdateInfo newRelease = UpdateChecker.checkForNewRelease(uri);
                        if (newRelease != null) {
                            updateInfos.add(newRelease);
                        }
                    } catch (URISyntaxException ex) {
                        // should not happen
                        LOGGER.error("Error while checking for new update sites: " + ex.getMessage(), ex);
                    } catch (IOException ex) {
                        LOGGER.error("I/O error while checking for new update sites: " + ex.getMessage(), ex);
                        showFetchingErrorDialog(ex);
                        return Status.CANCEL_STATUS;
                    }
                }
                monitor.worked(1);
            }

            final var abortFlag = new MutableBoolean(false);
            if (!updateInfos.isEmpty()) {
                Display.getDefault().syncExec(() -> {
                    final var shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();

                    var updatePossible = true;
                    var messageBuilder = new StringBuilder();
                    messageBuilder.append("New releases of the following components are available:\n");
                    for (UpdateInfo ui : updateInfos) {
                        updatePossible &= ui.isUpdatePossible();
                        if (AP_PROGRAM_NAME.matcher(ui.getName()).matches()
                            && !showWarningIfServerConnectionsWithOldExecutorAreRegistered(shell, ui.getShortName())) {
                            abortFlag.setValue(true);
                            return;
                        }
                        messageBuilder.append("\t" + ui.getName() + "\n");
                    }

                    if (updatePossible) {
                        messageBuilder.append("Do you want to upgrade to the new version?");
                        final var message = messageBuilder.toString();
                        boolean yes = MessageDialog.openQuestion(shell, "New KNIME release available", message);
                        if (yes) {
                            updateInfos.forEach(ui -> provUI.getRepositoryTracker().addRepository(ui.getUri(), null,
                                provUI.getSession()));
                        }
                    } else {
                        messageBuilder.append("Unfortunately a direct update is not possible. "
                            + "Please download the new version from the KNIME web page.");
                        final var message = messageBuilder.toString();
                        MessageDialog.openInformation(shell, "New KNIME release available", message);
                    }

                });
            }
            if (!abortFlag.booleanValue()) {
                startLoadJob();
            }
            return Status.OK_STATUS;
        }
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(InvokeUpdateAction.class);

    private static final String ID = "INVOKE_UPDATE_ACTION";

    /**
     * Default constructor.
     */
    public InvokeUpdateAction() {
        super("Update KNIME...", "Checks for KNIME updates", ID);
    }

    @Override
    public void run() {
        if (!checkSDKAndReadOnly()) {
            return;
        }

        new NewReleaseCheckerJob().schedule();
    }

    private static boolean showWarningIfServerConnectionsWithOldExecutorAreRegistered(final Shell shell, // NOSONAR
        final String toBeInstalledAPVersion) {
        final var clientVersion = new Version(toBeInstalledAPVersion);
        final var olderServerList = KNIMEUIPlugin.getServerAndExecutorVersions().stream()
            .filter(info -> info.isServerOlderThan413() || info.hasAnalyticsPlatformOlderThan(clientVersion))
            .collect(Collectors.toList());

        if (olderServerList.isEmpty()) {
            return true;
        }

        final var b = new StringBuilder();
        if (olderServerList.size() == 1) {
            var s = olderServerList.get(0);
            if (s.isServerOlderThan413()) {
                b.append("This client is connecting to server \"").append(s.getServerMountId()) //
                    .append("\" with version \"").append(s.getServerVersion()).append("\". ");
            } else {
                b.append("This client is connecting to server \"").append(s.getServerMountId()) //
                    .append("\" communicating with at least one executor with")
                    .append(" an older Analytics Platform version (") //
                    .append(s.getAnalyticsPlatformVersions().stream().filter(v -> !v.isSameOrNewer(clientVersion))
                        .map(Object::toString).collect(Collectors.joining(", ", "\"", "\"")))
                    .append("), which is older than the version to be installed (\"") //
                    .append(toBeInstalledAPVersion).append("\"). ");
            }
        } else {
            b.append("This client is connecting to multiple servers (") //
                .append(olderServerList.stream().map(ServerAndExecutorVersions::getServerMountId)
                    .collect(Collectors.joining(", ", "\"", "\""))) //
                .append(") each communicating with at least one executor with an older") //
                .append(" Analytics Platform version, which is older than the version to be installed (\"") //
                .append(toBeInstalledAPVersion) //
                .append("\"). ");
        }
        b.append("Workflows created with new versions of KNIME Analytics Platform will not properly ")
            .append("run on older KNIME Servers.\n\n") //
            .append("Do you want to continue with the update?");
        return MessageDialog.openQuestion(shell, "Connections to older servers", b.toString());
    }

    @Override
    protected void openWizard(final LoadMetadataRepositoryJob job, final ProvisioningUI provUI) {
        final var operation = provUI.getUpdateOperation(null, null);
        // check for updates
        operation.resolveModal(null);

        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> openWizard(job, provUI, operation));
    }

    /**
     * Opens the dialog for updating and restarting the AP, if updates are available.
     *
     * @param job job that fetched the update sites
     * @param provUI defines provisioning session, and UI policy
     * @param operation update operation, performing the update
     */
    private void openWizard(final LoadMetadataRepositoryJob job, final ProvisioningUI provUI,
        final UpdateOperation operation) {
        final var shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
        if (!operation.hasResolved()) {
            MessageDialog.openInformation(shell, "Update KNIME...", "No updates were found");
        } else if (provUI.getPolicy().continueWorkingWithOperation(operation, shell)) {
            if (UpdateSingleIUWizard.validFor(operation)) {
                // Special case for only updating a single root
                final var wizard = new UpdateSingleIUWizard(provUI, operation);
                final var dialog = new WizardDialog(shell, wizard);
                dialog.create();
                if (dialog.open() == 0) {
                    clearOsgiAreaBeforeRestart();
                }
            } else {
                // Open the normal version of the update wizard
                if (provUI.openUpdateWizard(false, operation, job) == 0) {
                    clearOsgiAreaBeforeRestart();
                }
            }
        }
    }
}
