/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: May 23, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 *
 * @author ohl, University of Konstanz
 */
public class GlobalDeleteAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalDeleteAction.class);

    /**
     * ID of the global delete action in the explorer menu.
     */
    public static final String DELETEACTION_ID =
            "org.knime.workbench.explorer.action.delete";

    private static final ImageDescriptor IMG_DELETE =
        PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                ISharedImages.IMG_TOOL_DELETE);

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalDeleteAction(final TreeViewer viewer) {
        super(viewer, "Delete...");
        setImageDescriptor(IMG_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return DELETEACTION_ID;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Map<AbstractContentProvider, List<ExplorerFileStore>> selectedFiles =
                getSelectedFiles();
        List<ExplorerFileStore> allFiles = new LinkedList<ExplorerFileStore>();

        // remove "double selection" (child whose parents are selected as well)
        for (Map.Entry<AbstractContentProvider, List<ExplorerFileStore>> e
                : selectedFiles.entrySet()) {
            List<ExplorerFileStore> sel = removeSelectedChildren(e.getValue());
            allFiles.addAll(sel);
        }
        // find workflows included in selection
        List<ExplorerFileStore> toDelWorkflows =
                getContainedWorkflows(allFiles);
        // try locking all workflows for deletion
        LinkedList<ExplorerFileStore> lockedWFs =
                new LinkedList<ExplorerFileStore>();
        if (toDelWorkflows.size() > 0) {
            LinkedList<ExplorerFileStore> unlockableWFs =
                    new LinkedList<ExplorerFileStore>();
            lockWorkflows(toDelWorkflows, unlockableWFs, lockedWFs);
            if (unlockableWFs.size() > 0) {
                // release locks acquired for deletion
                unlockWorkflows(lockedWFs);
                showCantDeleteMessage();
                return;
            }
        }

        assert lockedWFs.size() == toDelWorkflows.size();
        if (!confirmDeletion(allFiles, toDelWorkflows)) {
            // release locks acquired for deletion
            unlockWorkflows(lockedWFs);
            return;
        }

        closeOpenWorkflows(toDelWorkflows);

        // delete Workflows first (unlocks them too)
        boolean success = deleteLockedWorkflows(toDelWorkflows);
        success &= deleteTheRest(allFiles);

        if (!success) {
            showUnsuccessfulMessage();
        }
        //TODO: Refresh !
    }

    private boolean deleteLockedWorkflows(
            final List<ExplorerFileStore> toDelWFs) {
        boolean success = true;
        for (ExplorerFileStore wf : toDelWFs) {
            assert ExplorerFileStore.isWorkflow(wf);
            try {
                File loc = wf.toLocalFile(EFS.NONE, null);
                if (loc == null) {
                    // can't do any locking or fancy deletion
                    wf.delete(EFS.NONE, null);
                    return true;
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
                success &= wfFile.delete();

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
            } catch (CoreException e) {
                success = false;
                LOGGER.error("Error while deleting workflow " + wf.toString()
                        + ": " + e.getMessage(), e);
                // continue with next workflow...
            }
        }
        return success;
    }

    private boolean deleteTheRest(final List<ExplorerFileStore> toDel) {
        boolean success = true;
        for (ExplorerFileStore f : toDel) {
            // go by the local file. (Does EFS.delete() delete recursively??)
            try {
                File loc = f.toLocalFile(EFS.NONE, null);
                if (loc == null) {
                    f.delete(EFS.NONE, null);
                } else {
                    // if it is a workflow it would be gone already
                    if (loc.exists()) {
                        success &= FileUtil.deleteRecursively(loc);
                    }
                }
            } catch (CoreException e) {
                success = false;
                LOGGER.error("Error while deleting file " + f.toString()
                        + ": " + e.getMessage(), e);
            }
        }
        return success;
    }

    private boolean confirmDeletion(final List<ExplorerFileStore> toDel,
            final List<ExplorerFileStore> toDelWFs) {

        String msg = "";
        if (toDel.size() == 1) {
            if (ExplorerFileStore.isWorkflow(toDel.get(0))) {
                msg = "Do you want to delete the workflow ";
            } else if (ExplorerFileStore.isWorkflowGroup(toDel.get(0))) {
                msg = "Do you want to delete the workflow group ";
            } else {
                msg = "Do you want to delete the file ";
            }
            msg += "\"" + toDel.get(0).getName() + "\"?\n";
        } else {
            int stuff = toDel.size() - toDelWFs.size();
            if (stuff == 0) {
                msg =
                        "Do you want to delete all " + toDel.size()
                                + " selected workflows?\n";
            } else {
                if (stuff == 1) {
                    msg =
                            "Do you want to delete the selected item \""
                                    + toDel.get(0).getName() + "\"?\n";
                } else {
                    msg =
                            "Do you want to delete all " + toDel.size()
                                    + " selected items?\n";
                }
                if (toDelWFs.size() > 0) {
                    if (stuff == 1) {
                        msg += "It contains ";
                    } else {
                        msg += "They contain ";
                    }
                    msg += toDelWFs.size() + " workflow";
                    if (toDelWFs.size() > 1) {
                        msg += "s";
                    }
                    msg += ".\n";
                }
            }
        }
        msg += "\nThis operation cannot be undone.";

        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_QUESTION | SWT.OK
                        | SWT.CANCEL);
        mb.setMessage(msg);
        mb.setText("Confirm Deletion");
        if (mb.open() != SWT.OK) {
            LOGGER.debug("Deletion of " + toDel.size()
                    + " items canceled by user.");
            return false;
        } else {
            LOGGER.debug("Deletion of " + toDel.size()
                    + " items confirmed by user.");
            return true;
        }

    }

    private void showCantDeleteMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Deletion");
        mb.setMessage("At least one of the workflows can't be deleted.\n"
                + " It is probably in use by another user/instance.\n"
                + "Canceling deletion.");
        mb.open();
    }

    private void showUnsuccessfulMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Delete All Selected Items");
        mb.setMessage("At least one item could not be deleted.\n"
                + "Some might have been successfully deleted.");
        mb.open();
    }

}
