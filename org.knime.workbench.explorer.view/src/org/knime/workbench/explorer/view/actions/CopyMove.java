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
 *   May 1, 2017 (wiswedel): created
 */
package org.knime.workbench.explorer.view.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider.AfterRunCallback;
import org.knime.workbench.explorer.view.DestinationChecker;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dialogs.OverwriteAndMergeInfo;

/**
 * Main action implementation of copy/move of workflows or groups in the KNIME explorer. Used by different action
 * implementations, most notably {@link AbstractCopyMoveAction}.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class CopyMove {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CopyMove.class);

    private final ExplorerView m_view;

    private final AbstractExplorerFileStore m_target;

    private final boolean m_performMove;

    private final DestinationChecker<AbstractExplorerFileStore, AbstractExplorerFileStore> m_destChecker;

    private boolean m_excludeDataInWorkflows = false;

    private Set<LocalExplorerFileStore> m_notOverwritableDest = Collections.emptySet();

    private List<AbstractExplorerFileStore> m_srcFileStores = Collections.emptyList();

    /**
     * @param view
     * @param target
     * @param destChecker
     * @param performMove
     */
    public CopyMove(final ExplorerView view, final AbstractExplorerFileStore target,
        final DestinationChecker<AbstractExplorerFileStore, AbstractExplorerFileStore> destChecker,
        final boolean performMove) {
        m_view = CheckUtils.checkArgumentNotNull(view);
        m_target = CheckUtils.checkArgumentNotNull(target);
        m_destChecker = CheckUtils.checkArgumentNotNull(destChecker);
        m_performMove = performMove;
    }

    /**
     * @param notOverwritableDest the notOverwritableDest to set
     * @return this
     */
    public CopyMove setNotOverwritableDest(final Set<LocalExplorerFileStore> notOverwritableDest) {
        m_notOverwritableDest = CheckUtils.checkArgumentNotNull(notOverwritableDest);
        return this;
    }

    /**
     * @param excludeDataInWorkflows the excludeDataInWorkflows to set
     * @return this
     */
    public CopyMove setExcludeDataInWorkflows(final boolean excludeDataInWorkflows) {
        m_excludeDataInWorkflows = excludeDataInWorkflows;
        return this;
    }

    /**
     * @param monitor
     * @return
     */
    public CopyMoveResult run(final IProgressMonitor monitor) {
        final String cmd = cmdAsTextual();

        Map<AbstractExplorerFileStore, AbstractExplorerFileStore> destCheckerMappings = m_destChecker.getMappings();
        List<IStatus> statusList = new LinkedList<IStatus>();
        boolean success = true;

        // calculating the number of file transactions (copy/move/down/uploads)
        final ArrayList<AbstractExplorerFileStore> processedTargets = new ArrayList<>(destCheckerMappings.values());
        processedTargets.removeAll(Collections.singleton(null));

        int numFiles = processedTargets.size();
        monitor.beginTask(cmd + " " + numFiles + " files to " + m_target.getFullName(), numFiles);
        int iterationCount = processedTargets.size();
        List<ExplorerJob> moveJobs = new ArrayList<>();
        boolean uploadWarningShown = false;
        for (final Map.Entry<AbstractExplorerFileStore, AbstractExplorerFileStore> entry : destCheckerMappings
            .entrySet()) {
            AbstractExplorerFileStore srcFS = entry.getKey();
            AbstractExplorerFileStore destFS = entry.getValue();
            if (destFS == null) {
                // skip operations that have been marked to be skipped
                continue;
            }
            String operation = cmd + " " + srcFS.getMountIDWithFullPath() + " to " + destFS.getFullName();
            monitor.subTask(operation);
            LOGGER.debug(operation);
            try {
                if (m_notOverwritableDest.contains(destFS)) {
                    throw new UnsupportedOperationException("Cannot override \"" + destFS.getFullName()
                        + "\". Probably it is opened in the editor or it is in use by another user.");
                }
                boolean isOverwritten = m_destChecker.getOverwriteFS().contains(destFS);
                int options = isOverwritten ? EFS.OVERWRITE : EFS.NONE;

                /* Make sure that a workflow group is not
                 * overwritten by a workflow or template and vice
                 * versa. */
                AbstractExplorerFileInfo srcFSInfo = srcFS.fetchInfo();
                if (isOverwritten && (srcFSInfo.isWorkflowGroup() != destFS.fetchInfo().isWorkflowGroup())) {
                    String msg = null;
                    if (srcFSInfo.isWorkflowGroup()) {
                        msg = "Cannot override \"" + destFS.getFullName() + "\". Workflows and MetaNode Templates"
                            + " cannot be overwritten by a Workflow" + " Group.";
                    } else {
                        msg = "Cannot override \"" + destFS.getFullName() + "\". Workflow Groups can only be "
                            + "overwritten by other Workflow" + " Groups.";
                    }
                    throw new UnsupportedOperationException(msg);
                }

                boolean isSrcRemote = srcFS instanceof RemoteExplorerFileStore;
                boolean isDstRemote = destFS instanceof RemoteExplorerFileStore;
                if (srcFSInfo.isWorkflowTemplate() && !destFS.getContentProvider().canHostWorkflowTemplate(srcFS)) {
                    throw new UnsupportedOperationException("Cannot " + cmd + " metanode template '"
                        + srcFS.getFullName() + "' to " + destFS.getMountID() + "." + ". Unsupported operation.");
                }

                OverwriteAndMergeInfo info = m_destChecker.getOverwriteAndMergeInfos().get(destFS);
                if ((info != null) && (destFS instanceof RemoteExplorerFileStore) && info.createSnapshot()) {
                    ((RemoteExplorerFileStore)destFS).createSnapshot(info.getComment());
                }

                AfterRunCallback callback = null; // for async operations
                if (--iterationCount == 0) {
                    callback = t -> {
                        m_view.setNextSelection(processedTargets);

                        // update source folder as we removed an item from it.
                        if (!srcFS.equals(m_target) && m_performMove) {
                            srcFS.getParent().refresh();
                        }

                        m_target.refresh();
                    };
                }
                if (!isSrcRemote && isDstRemote) { // upload
                    AbstractExplorerFileStore destParentFS = ((RemoteExplorerFileStore)destFS).getParent();
                    if(destParentFS instanceof RemoteExplorerFileStore) {
                        RemoteExplorerFileInfo destParentFSInfo = ((RemoteExplorerFileStore)destParentFS).fetchInfo();
                        if (!uploadWarningShown && destParentFSInfo.isSpace() && !destParentFSInfo.isPrivateSpace()) {
                            IStatus userChoice = destFS.getContentProvider().showUploadWarning(destParentFSInfo.getName());
                            if (userChoice.isOK()) {
                                uploadWarningShown = true;
                            } else {
                                break;
                            }
                        }
                    }
                    destFS.getContentProvider().performUploadAsync((LocalExplorerFileStore)srcFS,
                        (RemoteExplorerFileStore)destFS, m_performMove, m_excludeDataInWorkflows, callback);
                } else if (isSrcRemote && !isDstRemote) { // download
                    CheckUtils.checkState(!m_excludeDataInWorkflows, "Download 'without data' not implemented");
                    destFS.getContentProvider().performDownloadAsync((RemoteExplorerFileStore)srcFS,
                        (LocalExplorerFileStore)destFS, m_performMove, callback);
                } else { // regular copy
                    CheckUtils.checkState(!m_excludeDataInWorkflows, "Copy/Move 'without data' not implement");
                    final boolean keepHistory = m_destChecker.getOverwriteAndMergeInfos().get(destFS) != null
                        ? m_destChecker.getOverwriteAndMergeInfos().get(destFS).keepHistory() : false;
                    if (m_performMove) {
                        moveJobs
                            .add(scheduleLocalCopyOrMove(srcFS, destFS, callback, m_performMove, options, keepHistory));
                    } else {
                        scheduleLocalCopyOrMove(srcFS, destFS, callback, m_performMove, options, keepHistory);
                    }
                }
            } catch (CoreException e) {
                LOGGER.debug(cmd + " failed: " + e.getStatus().getMessage(), e);
                statusList.add(e.getStatus());
                success = false;
                processedTargets.remove(destFS);
            } catch (UnsupportedOperationException e) {
                // illegal operation
                LOGGER.debug(cmd + " failed: " + e.getMessage());
                statusList.add(new Status(IStatus.WARNING, ExplorerActivator.PLUGIN_ID, e.getMessage()));
                success = true;
                processedTargets.remove(destFS);
            } catch (Exception e) {
                LOGGER.debug(cmd + " failed: " + e.getMessage(), e);
                statusList.add(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID, e.getMessage(), e));
                success = false;
                processedTargets.remove(destFS);
            }
            monitor.worked(1);
        }
        if (m_performMove && !m_srcFileStores.isEmpty()) {
            scheduleDeletionOfRemainingWorkflowGroups(destCheckerMappings, moveJobs);
        }
        return new CopyMoveResult(statusList, success);
    }

    private void scheduleDeletionOfRemainingWorkflowGroups(
        final Map<AbstractExplorerFileStore, AbstractExplorerFileStore> destCheckerMappings,
        final List<ExplorerJob> moveJobs) {
        ExplorerJob job = new ExplorerJob("Delete remaining workflow groups") {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                // wait for the move jobs to move all the files
                for (ExplorerJob moveJob : moveJobs) {
                    try {
                        moveJob.join(60000, monitor);
                    } catch (InterruptedException e) {
                        return Status.CANCEL_STATUS;
                    }
                }
                for (AbstractExplorerFileStore srcFileStore : m_srcFileStores) {
                    AfterRunCallback callback = t -> srcFileStore.getParent().refresh();
                    try {
                        // if sourceWorkFlowGroup is not moved because it was merged into an existing group, delete it
                        if (destCheckerMappings.get(srcFileStore) == null) {
                            deleteRemainingWorkflowGroups(srcFileStore, monitor);
                        }
                        AfterRunCallback.callCallbackInDisplayThread(callback, null);
                    } catch (CoreException ce) {
                        AfterRunCallback.callCallbackInDisplayThread(callback, ce);
                        return ce.getStatus();
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void deleteRemainingWorkflowGroups(final AbstractExplorerFileStore fileStore,
        final IProgressMonitor monitor) throws CoreException {
        // only workflowgroups should be deleted
        if (!AbstractExplorerFileStore.isWorkflowGroup(fileStore)) {
            return;
        }
        // do not delete workflowgroups that have workflows or other files as children...
        if (hasOnlyWorkflowGroupChidlren(fileStore, monitor)) {
            fileStore.delete(EFS.NONE, monitor);
        } else {
            // ... but delete contained workflow groups that are empty
            for (AbstractExplorerFileStore childFileStore : fileStore.childStores(EFS.NONE, monitor)) {
                deleteRemainingWorkflowGroups(childFileStore, monitor);
            }
        }

    }

    private boolean hasOnlyWorkflowGroupChidlren(final AbstractExplorerFileStore fileStore,
        final IProgressMonitor monitor) throws CoreException {
        if (AbstractExplorerFileStore.isWorkflowGroup(fileStore)) {
            for (AbstractExplorerFileStore childStore : fileStore.childStores(EFS.NONE, monitor)) {
                if (!AbstractExplorerFileStore.isWorkflowGroup(childStore)) {
                    return false;
                }
                if (!hasOnlyWorkflowGroupChidlren(childStore, monitor)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Return "Move" or "Copy" for us in string literals such as log messages. */
    private String cmdAsTextual() {
        return m_performMove ? "Move" : "Copy";
    }

    private ExplorerJob scheduleLocalCopyOrMove(final AbstractExplorerFileStore source,
        final AbstractExplorerFileStore destination, final AfterRunCallback callback, final boolean move,
        final int options, final boolean keepHistory) {
        ExplorerJob job = new ExplorerJob(
            cmdAsTextual() + " of " + source.getMountIDWithFullPath() + " to " + destination.getMountIDWithFullPath()) {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    if (move) {
                        source.move(destination, options, monitor, keepHistory);
                    } else {
                        source.copy(destination, options, monitor, keepHistory);
                    }
                    AfterRunCallback.callCallbackInDisplayThread(callback, null);
                    return Status.OK_STATUS;
                } catch (CoreException ce) {
                    AfterRunCallback.callCallbackInDisplayThread(callback, ce);
                    return ce.getStatus();
                }
            }
        };
        job.schedule();
        return job;
    }

    /**
     * Sets the source file stores that are going to be moved/copied.
     *
     * @param srcFileStores a potentially empty list of source file stores
     * @since 8.4
     */
    public void setSrcFileStores(final List<AbstractExplorerFileStore> srcFileStores) {
        m_srcFileStores = new ArrayList<>(srcFileStores);
    }

    /** Return value of the run method. */
    public static final class CopyMoveResult {
        private final List<IStatus> m_result;

        private final boolean m_success;

        CopyMoveResult(final List<IStatus> result, final boolean success) {
            m_result = result;
            m_success = success;
        }

        /** @return the result list, not null. */
        public List<IStatus> getStatusList() {
            return m_result;
        }

        /** @return the success flag. */
        public boolean isSuccess() {
            return m_success;
        }

    }

}
