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
 *   03.10.2010 (meinl): created
 */
package org.knime.workbench.ui.p2.actions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.knime.core.eclipseUtil.UpdateChecker;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.PathUtils;
import org.knime.core.util.proxy.DisabledSchemesChecker;

/**
 * Abstract action for p2 related tasks (installing new extensions or updating, e.g.).
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class AbstractP2Action extends Action {
    /**
     * Creates a new action.
     *
     * @param text the actions name as shown in the menu
     * @param description a description for the action
     * @param id a unique id
     */
    protected AbstractP2Action(final String text, final String description, final String id) {
        super(text);
        setDescription(description);
        setId(id);
    }

    /**
     * Starts the repository load job. After the repositories have been loaded,
     * {@link #openWizard(LoadMetadataRepositoryJob, ProvisioningUI)} is called.
     */
    protected final void startLoadJob() {
        final var provUI = ProvisioningUI.getDefaultUI();
        Job.getJobManager().cancel(LoadMetadataRepositoryJob.LOAD_FAMILY);
        final var loadJob = new LoadMetadataRepositoryJob(provUI);
        loadJob.setProperty(LoadMetadataRepositoryJob.ACCUMULATE_LOAD_ERRORS, Boolean.toString(true));
        loadJob.setProperty(LoadMetadataRepositoryJob.SUPPRESS_AUTHENTICATION_JOB_MARKER, Boolean.toString(true));

        loadJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(final IJobChangeEvent event) {
                if (PlatformUI.isWorkbenchRunning() && event.getResult().isOK()) {
                    openWizard(loadJob, provUI);
                }
            }
        });
        loadJob.setUser(true);
        loadJob.schedule();
    }

    /**
     * Checks whether the current instance is run from an SDK and if the configuration area is writable.
     *
     * @return <code>true</code> if the action should continue, <code>false</code> if it should be aborted
     */
    public static final boolean checkSDKAndReadOnly() {
        final var provUI = ProvisioningUI.getDefaultUI();
        if (provUI.getRepositoryTracker() == null) {
            final var mbox = new MessageBox(ProvUI.getDefaultParentShell(), SWT.ICON_WARNING | SWT.OK);
            mbox.setText("Action impossible");
            mbox.setMessage("It seems you are running KNIME from an SDK. "
                + "Installing extension is not possible in this case.");
            mbox.open();
            return false;
        }

        final var installLocation = Platform.getInstallLocation().getURL().toString();
        final var configurationLocation = Platform.getConfigurationLocation().getURL().toString();

        if (!configurationLocation.contains(installLocation)) {
            final var mbox = new MessageBox(ProvUI.getDefaultParentShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
            mbox.setText("Permission problem");
            mbox.setMessage("Your KNIME installation directory seems to be read-only, maybe because KNIME was "
                + "installed by a different user, e.g., the system administrator. Installing extensions or updating "
                + "KNIME without write permissions may cause problems. Do you really want to continue?");
            return (mbox.open() == SWT.YES);
        }
        return true;
    }

    /**
     * Checks whether the current instance can fetch known repositories. Continues even when failing to reach
     * repositories - as long as it's not caused by disabled schemes.
     *
     * @return <code>true</code> if the action should continue, <code>false</code> if it should be aborted
     */
    protected static final boolean checkFetchingRepositories() {
        final var provUI = ProvisioningUI.getDefaultUI();
        final var repositories = provUI.getRepositoryTracker().getKnownRepositories(provUI.getSession());

        // use the first URI as representative
        final var remoteURI = Stream.of(repositories).filter(uri -> uri.getScheme().startsWith("http")).findFirst();
        // if there is nothing to fetch, continue
        if (remoteURI.isEmpty()) {
            return true;
        }
        try {
            UpdateChecker.checkForNewRelease(remoteURI.get());
            return true;
        } catch (IOException | URISyntaxException e) {
            return !checkAndShowDisabledSchemes(e);
        }
    }

    /**
     * Checks whether the current instance uses authenticated proxies but it is disabled.
     * In this case, it shows a helpful error message, pointing to our FAQ.
     *
     * @return <code>true</code> if the exception was caused by disabled schemes
     */
    protected static final boolean checkAndShowDisabledSchemes(final Throwable t) {
        if (DisabledSchemesChecker.isCausedByDisabledSchemes(t)) {
            Display.getDefault().syncExec(() -> {
                final var shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
                final var reason = DisabledSchemesChecker.FAQ_MESSAGE;
                final var message = "Error while fetching repositories because "
                    + reason.substring(0, 1).toLowerCase(Locale.ENGLISH) + reason.substring(1);
                MessageDialog.openError(shell, "Fetching failed", message);
            });
            return true;
        }
        return false;
    }

    /**
     * Displays the reason why the update check failed in a pop-up dialog.
     * Checks also if authentication schemes are disabled, see {@link #checkAndShowDisabledSchemes(Throwable)}.
     *
     * @param t Throwable holding the reason why the update check failed.
     */
    protected static void showFetchingErrorDialog(final Throwable t) {
        if (!checkAndShowDisabledSchemes(t)) {
            Display.getDefault().syncExec(() -> {
                final var message = String.format(//
                    "Error while fetching repositories because of the following reason:%n%n%s", t.getMessage());
                final var shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
                MessageDialog.openError(shell, "Fetching failed", message);
            });
        }
    }

    private final AtomicBoolean m_shutdownHookAdded = new AtomicBoolean();

    /**
     * Clears the OSGi configuration area in a shutdown hook. This forces re-initialization of all bundles information
     * as if Eclipse would be started with "-clean".
     */
    protected final void clearOsgiAreaBeforeRestart() {
        if (m_shutdownHookAdded.getAndSet(true)) {
            return;
        }

        try {
            @SuppressWarnings("restriction")
            final var configUrl = Platform.getConfigurationLocation().getDataArea(EquinoxContainer.NAME);
            final var configPath = Paths.get(new URI(configUrl.toString().replace(" ", "%20")));

            // make sure PathUtils is initialized, because it registers a shutdown hook itself and this is not possible
            // while the JVM shuts down
            PathUtils.RWX_ALL_PERMISSIONS.isEmpty();

            Runtime.getRuntime().addShutdownHook(new Thread("Clear OSGi configuration") {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    try {
                        if (Files.isWritable(configPath)) {
                            NodeLogger.getLogger(AbstractP2Action.this.getClass())
                                .debug("Deleting OSGi configuration area at " + configPath);
                            PathUtils.deleteDirectoryIfExists(configPath);
                        }
                    } catch (IOException ex) {
                        NodeLogger.getLogger(AbstractP2Action.this.getClass())
                            .error("Could not clean OSGi configuration area: " + ex.getMessage());
                    }
                }
            });
        } catch (IOException | URISyntaxException ex) {
            NodeLogger.getLogger(AbstractP2Action.this.getClass())
                .error("Could not acquire OSGi configuration area: " + ex.getMessage());
        }
    }

    /**
     * This is called when a wizard (install, update, ...) should be opened. Subclasses must override this method and
     * open the desired wizard.
     *
     * @param job the repository job
     * @param provUI the provisioning UI instance
     */
    protected abstract void openWizard(final LoadMetadataRepositoryJob job, ProvisioningUI provUI);
}
