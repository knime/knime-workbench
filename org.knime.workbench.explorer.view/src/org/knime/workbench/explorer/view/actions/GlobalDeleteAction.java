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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;

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
            ExplorerFileSystemUtils.lockWorkflows(toDelWorkflows, unlockableWFs,
                    lockedWFs);
            if (unlockableWFs.size() > 0) {
                // release locks acquired for deletion
                ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
                showCantDeleteMessage();
                return;
            }
        }

        assert lockedWFs.size() == toDelWorkflows.size();
        if (!confirmDeletion(allFiles, toDelWorkflows)) {
            // release locks acquired for deletion
            ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
            return;
        }

        ExplorerFileSystemUtils.closeOpenWorkflows(toDelWorkflows);

        // delete Workflows first (unlocks them too)
        boolean success = ExplorerFileSystemUtils.deleteLockedWorkflows(
                toDelWorkflows);
        success &= ExplorerFileSystemUtils.deleteTheRest(allFiles);

        if (!success) {
            showUnsuccessfulMessage();
        }

        for (ExplorerFileStore fileStore : toDelWorkflows) {
            Object parent = ContentDelegator.getTreeObjectFor(
                    fileStore.getParent());
            getViewer().refresh(parent);
        }
        for (ExplorerFileStore fileStore : allFiles) {
            Object parent = ContentDelegator.getTreeObjectFor(
                    fileStore.getParent());
            getViewer().refresh(parent);
        }
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
            if (toDelWFs.size() > 0) {
                msg += "It contains " + toDelWFs.size() + " workflow";
                if (toDelWFs.size() > 1) {
                    msg += "s";
                }
                msg += ".\n";
            }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getSelection().size() > 0
                && DragAndDropUtils.getExplorerFileStores(
                        getSelection()) != null;
    }

}
