/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class CopyMove {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CopyMove.class);

    private final ExplorerView m_view;
    private final AbstractExplorerFileStore m_target;
    private final boolean m_performMove;
    private final DestinationChecker<AbstractExplorerFileStore, AbstractExplorerFileStore> m_destChecker;

    private boolean m_excludeDataInWorkflows = false;
    private Set<LocalExplorerFileStore> m_notOverwritableDest = Collections.emptySet();


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
        for (final Map.Entry<AbstractExplorerFileStore, AbstractExplorerFileStore> entry
                : destCheckerMappings.entrySet()) {
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
                if (srcFSInfo.isWorkflowTemplate() && !destFS.getContentProvider().canHostMetaNodeTemplates()) {
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
                        m_target.refresh();
                    };
                }
                if (!isSrcRemote && isDstRemote) { // upload
                    destFS.getContentProvider().performUploadAsync((LocalExplorerFileStore)srcFS,
                        (RemoteExplorerFileStore)destFS, m_performMove, m_excludeDataInWorkflows, callback);
                } else if (isSrcRemote && !isDstRemote) { // download
                    CheckUtils.checkState(!m_excludeDataInWorkflows, "Download 'without data' not implemented");
                    destFS.getContentProvider().performDownloadAsync((RemoteExplorerFileStore)srcFS,
                        (LocalExplorerFileStore)destFS, m_performMove, callback);
                } else { // regular copy
                    CheckUtils.checkState(!m_excludeDataInWorkflows, "Copy/Move 'without data' not implement");
                    scheduleLocalCopyOrMove(srcFS, destFS, callback, m_performMove, options);
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
        return new CopyMoveResult(statusList, success);
    }

    /** Return "Move" or "Copy" for us in string literals such as log messages. */
    private String cmdAsTextual() {
        return m_performMove ? "Move" : "Copy";
    }

    private void scheduleLocalCopyOrMove(final AbstractExplorerFileStore source,
        final AbstractExplorerFileStore destination, final AfterRunCallback callback, final boolean move,
        final int options) {
        ExplorerJob job = new ExplorerJob(cmdAsTextual() + " of " + source.getMountIDWithFullPath() + " to "
                + destination.getMountIDWithFullPath()) {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    if (move) {
                        source.move(destination, options, monitor);
                    } else {
                        source.copy(destination, options, monitor);
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
