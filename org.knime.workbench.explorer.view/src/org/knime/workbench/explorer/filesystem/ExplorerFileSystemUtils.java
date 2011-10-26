/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 3, 2011 (wiswedel): created
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceFileStore;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;

/**
 * A set of static methods to deal with the creation/deletion of possibly locked
 * workflows in the explorer file system.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class ExplorerFileSystemUtils {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerFileSystemUtils.class);

    /** Utility class, no public constructor. */
    private ExplorerFileSystemUtils() {
        // no op
    }

    /**
     * Tries to lock the workflows passed as first argument.
     *
     * @param workflowsToLock the workflows to be locked
     * @param unlockableWF the workflows that could not be locked
     * @param lockedWF the workflows that could be locked
     */
    public static void lockWorkflows(
            final List<? extends LocalExplorerFileStore> workflowsToLock,
            final List<LocalExplorerFileStore> unlockableWF,
            final List<LocalExplorerFileStore> lockedWF) {
        assert unlockableWF.size() == 0; // the result lists should be empty
        assert lockedWF.size() == 0;
        // open workflows can be locked multiple times in one instance
        for (LocalExplorerFileStore wf : workflowsToLock) {
            boolean locked = lockWorkflow(wf);
            if (locked) {
                LOGGER.debug("Locked workflow " + wf);
                lockedWF.add(wf);
            } else {
                unlockableWF.add(wf);
            }
        }
    }

    /**
     * Tries to lock the workflow.
     *
     * @param workflow the workflow to be locked
     * @return true if the workflow could be locked, false otherwise
     */
    public static boolean lockWorkflow(final LocalExplorerFileStore workflow) {
        assert AbstractExplorerFileStore.isWorkflow(workflow);
        File loc;
        try {
            loc = workflow.toLocalFile(EFS.NONE, null);
        } catch (CoreException e) {
            loc = null;
        }
        if (loc != null && VMFileLocker.lockForVM(loc)) {
            LOGGER.debug("Locked workflow " + workflow);
            return true;
        } else {
            LOGGER.debug("Could not lock workflow " + workflow);
            return false;
        }
    }

    /**
     * Unlocks the specified workflows.
     *
     * @param workflows the workflows to be unlocked
     */
    public static void unlockWorkflows(
            final List<? extends LocalExplorerFileStore> workflows) {
        for (LocalExplorerFileStore lwf : workflows) {
            unlockWorkflow(lwf);
        }
    }

    /**
     * Unlocks the specified workflow.
     *
     * @param workflow the workflow to be unlocked
     */
    public static void unlockWorkflow(final LocalExplorerFileStore workflow) {
        File loc;
        try {
            loc = workflow.toLocalFile(EFS.NONE, null);
        } catch (CoreException e) {
            return;
        }
        if (!VMFileLocker.isLockedForVM(loc)) {
            LOGGER.debug("Nothing to unlock. \"" + workflow.getFullName()
                    + "\" is not locked.");
            return;
        }
        assert AbstractExplorerFileStore.isWorkflow(workflow);
        LOGGER.debug("Unlocking workflow " + workflow);
        VMFileLocker.unlockForVM(loc);
    }

    /**
     * Closes the editor of the specified workflows.
     *
     * @param workflows the workflows to be closed
     */
    public static void closeOpenWorkflows(
            final List<? extends AbstractExplorerFileStore> workflows) {
        IWorkbenchPage page =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
        for (AbstractExplorerFileStore wf : workflows) {
            File loc;
            try {
                loc = wf.toLocalFile(EFS.NONE, null);
            } catch (CoreException e) {
                loc = null;
            }
            if (loc == null) {
                // not a local workflow. Not open.
                continue;
            }
            NodeContainer wfm = ProjectWorkflowMap.getWorkflow(loc.toURI());
            if (wfm != null) {
                for (IEditorReference editRef : page.getEditorReferences()) {
                    IEditorPart editor = editRef.getEditor(false);
                    if (editor == null) {
                        // got closed in the mean time
                        continue;
                    }
                    WorkflowEditorAdapter wea =
                            (WorkflowEditorAdapter)editor
                                    .getAdapter(WorkflowEditorAdapter.class);
                    NodeContainer editWFM = null;
                    if (wea != null) {
                        editWFM = wea.getWorkflowManager();
                    }
                    if (wfm == editWFM) {
                        page.closeEditor(editor, false);
                    }
                }

            }
        }
    }

    /**
     * @param workflows the workflows to check
     * @return true if at least one of the specified workflows are open, false
     *         otherwise
     */
    public static boolean hasOpenWorkflows(
            final List<? extends AbstractExplorerFileStore> workflows) {
        IWorkbenchPage page =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage();
        for (AbstractExplorerFileStore wf : workflows) {
            File loc;
            try {
                loc = wf.toLocalFile(EFS.NONE, null);
            } catch (CoreException e) {
                loc = null;
            }
            if (loc == null) {
                // not a local workflow. Not open.
                continue;
            }
            NodeContainer wfm = ProjectWorkflowMap.getWorkflow(loc.toURI());
            if (wfm != null) {
                for (IEditorReference editRef : page.getEditorReferences()) {
                    IEditorPart editor = editRef.getEditor(false);
                    if (editor == null) {
                        // got closed in the mean time
                        continue;
                    }
                    WorkflowEditorAdapter wea =
                            (WorkflowEditorAdapter)editor
                                    .getAdapter(WorkflowEditorAdapter.class);
                    NodeContainer editWFM = null;
                    if (wea != null) {
                        editWFM = wea.getWorkflowManager();
                    }
                    if (wfm == editWFM) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    /**
     * Delete workflows from argument list. If the workflows are locked by this
     * VM, they will be unlocked after this method returns.
     *
     * @param toDelWFs The list of directories associate with the workflows.
     * @return true if that was successful, i.e. the workflow directory does not
     *         exist when this method returns, false if that fails (e.g. not
     *         locked by this VM)
     **/
    public static boolean deleteLockedWorkflows(
            final List<? extends AbstractExplorerFileStore> toDelWFs) {
        boolean success = true;
        for (AbstractExplorerFileStore wf : toDelWFs) {
            assert AbstractExplorerFileStore.isWorkflow(wf)
                    || AbstractExplorerFileStore.isWorkflowTemplate(wf);
            try {
                File loc = wf.toLocalFile(EFS.NONE, null);
                if (loc == null) {
                    // can't do any locking or fancy deletion
                    wf.delete(EFS.NONE, null);
                    continue;
                }
                assert VMFileLocker.isLockedForVM(loc);

                // delete the workflow file first
                File[] children = loc.listFiles();
                if (children == null) {
                    throw new CoreException(
                            new Status(Status.ERROR,
                                    ExplorerActivator.PLUGIN_ID,
                                    "Can't read location."));
                }

                // delete workflow file first
                File wfFile = new File(loc, WorkflowPersistor.WORKFLOW_FILE);
                if (wfFile.exists()) {
                    success &= wfFile.delete();
                } else {
                    File tempFile =
                            new File(loc, WorkflowPersistor.TEMPLATE_FILE);
                    success &= tempFile.delete();
                }

                children = loc.listFiles(); // get a list w/o workflow file
                for (File child : children) {
                    if (VMFileLocker.LOCK_FILE.equals(child.getName())) {
                        // delete the lock file last
                        continue;
                    }
                    boolean deletedIt = FileUtil.deleteRecursively(child);
                    success &= deletedIt;
                    if (!deletedIt) {
                        LOGGER.error("Unable to delete " + child.toString());
                    }
                }

                // release lock in order to delete lock file
                VMFileLocker.unlockForVM(loc);
                // lock file resource may not exist
                File lockFile = new File(loc, VMFileLocker.LOCK_FILE);
                if (lockFile.exists()) {
                    success &= lockFile.delete();
                }
                // delete the workflow directory itself
                success &= FileUtil.deleteRecursively(loc);
                if (wf instanceof LocalWorkspaceFileStore) {
                    ((LocalWorkspaceFileStore)wf).refreshParentResource();
                }
            } catch (CoreException e) {
                success = false;
                LOGGER.error("Error while deleting workflow " + wf.toString()
                        + ": " + e.getMessage(), e);
                // continue with next workflow...
            }
        }
        return success;
    }

    /**
     * Delete the files denoted by the argument list.
     *
     * @param toDel The list of files to be deleted.
     * @return true if the files/directories don't exist when this method
     *         returns.
     */
    public static boolean deleteTheRest(
            final List<? extends AbstractExplorerFileStore> toDel) {
        boolean success = true;
        for (AbstractExplorerFileStore f : toDel) {
            // go by the local file. (Does EFS.delete() delete recursively??)
            try {
                if (f.getName().equals("/")) {
                    // the root is represented by the mount point. Can't del it!
                    LOGGER.info("Can't delete the root of a mounted space. "
                            + "(Skipping " + f.getMountIDWithFullPath() + ")");
                    continue;
                }
                if (f.fetchInfo().exists()) {
                    File loc = f.toLocalFile(EFS.NONE, null);
                    if (loc == null) {
                        f.delete(EFS.NONE, null);
                    } else {
                        // if it is a workflow it would be gone already
                        if (loc.exists()) {
                            success &= FileUtil.deleteRecursively(loc);
                        }
                        if (f instanceof LocalWorkspaceFileStore) {
                            ((LocalWorkspaceFileStore)f)
                                    .refreshParentResource();
                        }
                    }
                }
            } catch (CoreException e) {
                success = false;
                LOGGER.error("Error while deleting file " + f.toString() + ": "
                        + e.getMessage(), e);
            }
        }
        return success;
    }

}
