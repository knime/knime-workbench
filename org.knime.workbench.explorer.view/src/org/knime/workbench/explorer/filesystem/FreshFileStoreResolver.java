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
 *   Aug 27, 2025 (magnus): created
 */
package org.knime.workbench.explorer.filesystem;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EclipseUtil;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 * Refreshes the remote content provider. This is a workaround for the fetcher not being used anymore
 * in nodes which use an old files selection dialog.
 * <p>
 * This is an expensive operation involving network I/O and should be avoided where possible.
 *
 * @since 9.2
 */
public final class FreshFileStoreResolver {

    private FreshFileStoreResolver() {
    }

    /**
     * Refresh if necessary remote, authenticated content providers. The progress is displayed in the
     * given active display shell which blocks calls until all jobs are finished or cancelled.
     */
    public static void refreshContentProvidersWithProgress(final Shell activeShell, final String... mountIds) {
        // don't refresh in non-AP contexts
        if (!EclipseUtil.determineAPUsage()) {
            return;
        }
        // don't refresh in classic UI
        if (EclipseUtil.determineClassicUIUsage()) {
            return;
        }

        var fileStores = findContentProviders(Arrays.stream(mountIds).collect(Collectors.toUnmodifiableSet()));
        if (fileStores.isEmpty()) {
            return;
        }
        refreshWithProgress(activeShell, fileStores);
    }

    private static List<RemoteExplorerFileStore> findContentProviders(final Set<String> mountIds) {
        return ExplorerMountTable.getMountedContent() //
                .values().stream() //
                // filter mounted with current space providers, to not refresh remotes that are not visible in
                // ModernUI, if any
                .filter(contentProvider -> mountIds.contains(contentProvider.getMountID())) //
                // we don't need to refresh local and currently unconnected
                .filter(contentProvider -> contentProvider.isRemote() && contentProvider.isAuthenticated()) //
                .map(AbstractContentProvider::getRootStore) //
                .filter(RemoteExplorerFileStore.class::isInstance) //)
                .map(RemoteExplorerFileStore.class::cast) //)
                .toList();
    }


    private static void refreshWithProgress(final Shell activeShell, final List<RemoteExplorerFileStore> fileStores) {
        PlatformUI.getWorkbench().getDisplay().syncCall(() -> { // NOSONAR
            fileStores.forEach(store -> new RefreshJob(store).schedule());
            return joinOnJobFamily(activeShell, RefreshJob.class);
        });
    }

    /**
     * Wait for jobs belonging to {@code family} to finish. Similar to `new RefreshJob(...).run(monitor)`, this
     * let's us cancel the whole thing. For some reason, the former does not cancel jobs on "Cancel".
     *
     * @param activeShell the shell to use for displaying the progress dialog
     * @param family see {@link Job#belongsTo(Object)} and
     *            {@link org.eclipse.core.runtime.jobs.IJobManager#join(Object, IProgressMonitor)}
     * @return Whether the job family execution has been cancelled.
     */
    private static boolean joinOnJobFamily(final Shell activeShell, final Object family) {
        final var canceled = new AtomicBoolean(false);
        try {
            ProgressMonitorDialog dialog = new ProgressMonitorDialog(activeShell);
            dialog.run(true, true, monitor -> { // NOSONAR complexity ok
                try {
                    // We wait for our refresh jobs to finish.
                    Job.getJobManager().join(family, monitor);
                } catch (final OperationCanceledException e) { // NOSONAR we handle this exception
                    // user hit cancel, suggest jobs to cancel as well. Unfortunately, it's not possible to
                    // find the job's thread to interrupt it... :(
                    for (final var job : Job.getJobManager().find(family)) {
                        job.cancel();
                    }
                    // handle refresh cancellation as complete cancellation, i.e. not opening the picker, too
                    canceled.set(true);
                    return;
                }
            });
        } catch (final InvocationTargetException e) {
            NodeLogger.getLogger(FreshFileStoreResolver.class)
                .warn("Failed to refresh remote content before opening dialog – content might be stale.", e);
        } catch (final InterruptedException e) { // NOSONAR we recover from that
            // we got interrupted while waiting for refresh, so we cancel the whole thing
            canceled.set(true);
        }
        return canceled.get();
    }

    /**
     * Eclipse workbench job to refresh a given {@link RemoteExplorerFileStore}.
     */
    private static final class RefreshJob extends Job {

        private final RemoteExplorerFileStore m_store;

        private RefreshJob(final RemoteExplorerFileStore store) {
            super("Refresh remote content");
            setUser(true);
            m_store = store;
        }

        @Override
        public boolean belongsTo(final Object family) {
            return family == RefreshJob.class;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            monitor.beginTask("Refreshing \"%s\"...".formatted(m_store.getMountID()), IProgressMonitor.UNKNOWN);
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            m_store.refresh(true);
            monitor.done();
            return Status.OK_STATUS;
        }

        @Override
        protected void canceling() {
            super.canceling();
            // we need to interrupt the thread, because the store refresh does not check the progress monitor for
            // cancellation
            getThread().interrupt();
        }

    }

}
