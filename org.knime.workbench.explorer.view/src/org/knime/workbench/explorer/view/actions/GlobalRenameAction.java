/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
 * KNIME.com, Zurich, Switzerland
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
 * History
 *   May 26, 2011 (morent): created
 */

package org.knime.workbench.explorer.view.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.validators.FileStoreNameValidator;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class GlobalRenameAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalRenameAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String RENAMEACTION_ID =
            "org.knime.workbench.explorer.action.rename";

    private static final ImageDescriptor ICON = KNIMEUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/rename.png");;

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalRenameAction(final ExplorerView viewer) {
        super(viewer, "Rename...");
        setImageDescriptor(ICON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return RENAMEACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (isMultipleSelection()) {
            return;
        }
        IStructuredSelection selection = getSelection();

        List<AbstractExplorerFileStore> stores =
                DragAndDropUtils.getExplorerFileStores(selection);
        AbstractExplorerFileStore srcFileStore = stores.get(0);

        // find affected workflows
        List<AbstractExplorerFileStore> affectedFlows =
                getAllContainedWorkflows(stores);

        // try locking all local workflows for renaming
        List<LocalExplorerFileStore> localWFs =
                getContainedLocalWorkflows(stores);
        LinkedList<LocalExplorerFileStore> lockedWFs =
                new LinkedList<LocalExplorerFileStore>();
        if (localWFs.size() > 0) {
            LinkedList<LocalExplorerFileStore> unlockableWFs =
                    new LinkedList<LocalExplorerFileStore>();
            ExplorerFileSystemUtils.lockWorkflows(localWFs, unlockableWFs,
                    lockedWFs);
            if (unlockableWFs.size() > 0) {
                // release locks acquired for renaming
                ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
                showCantRenameLockMessage();
                return;
            }
        }

        if (ExplorerFileSystemUtils.hasOpenWorkflows(affectedFlows)) {
            // release locks acquired for renaming
            ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
            showCantRenameOpenMessage();
            return;
        }

        AbstractExplorerFileStore dstFileStore = queryTargetName(srcFileStore);
        if (dstFileStore == null) {
            // dialog was cancelled
            ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
            return;
        }

        // rename only works on single selection - all flows are from the same content provider
        AbstractContentProvider cp = null;
        if (affectedFlows.size() > 0) {
            cp = affectedFlows.get(0).getContentProvider();
        }
        if (cp != null) {
            AtomicBoolean confirmed = cp.confirmMove(getParentShell(), affectedFlows);
            if (confirmed != null && !confirmed.get()) {
                ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
                return;
            }
        }

        /*
         * Unfortunately we have to unlock the workflows before moving. If we
         * move the workflows including the locks, we loose the possibility to
         * unlock them.
         */
        ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
        try {
            srcFileStore.move(dstFileStore, EFS.NONE, null);
            // unlockDstWorkflows(acp, srcFileStore, dstFileStore, lockedWFs);
            LOGGER.debug("Renamed \"" + srcFileStore + "\" to \""
                    + dstFileStore + "\".");
            getView().setNextSelection(dstFileStore);
        } catch (CoreException e) {
            String message =
                    "Could not rename \"" + srcFileStore + "\" to \""
                            + dstFileStore + "\": " + e.getMessage();
            LOGGER.error(message, e);
            ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
            MessageDialog.openError(getParentShell(), "Renaming failed",
                    message);
        }
    }

    /**
     * Constructs the new names of the locked workflows and removes the lock.
     *
     * @param acp the content provider of the file stores
     * @param srcFileStore the file store selected for renaming
     * @param lockedWFs the locked workflows
     */
//    @SuppressWarnings("unused")
//    // might be needed later
//    private void unlockDstWorkflows(final AbstractContentProvider acp,
//            final AbstractExplorerFileStore srcFileStore,
//            final AbstractExplorerFileStore dstFileStore,
//            final List<LocalExplorerFileStore> lockedWFs) {
//        String srcName = srcFileStore.getFullName();
//        String dstName = dstFileStore.getFullName();
//        List<LocalExplorerFileStore> dstStores =
//                new ArrayList<LocalExplorerFileStore>();
//        for (LocalExplorerFileStore fs : lockedWFs) {
//            String dstStoreName = fs.getFullName().replace(srcName, dstName);
//            LocalExplorerFileStore dstFs =
//                    (LocalExplorerFileStore)acp.getFileStore(dstStoreName);
//            dstStores.add(dstFs);
//        }
//        ExplorerFileSystemUtils.unlockWorkflows(dstStores);
//    }

    private AbstractExplorerFileStore queryTargetName(
            final AbstractExplorerFileStore fileStore) {
        Shell shell = Display.getDefault().getActiveShell();
        final String name = fileStore.getName();
        InputDialog dialog =
                new InputDialog(shell, "Rename",
                        "Please enter the new name for \"" + name + "\"", name,
                        new FileStoreNameValidator(name));
        dialog.setBlockOnOpen(true);

        if (dialog.open() == Window.CANCEL) {
            return null;
        }
        String newName = dialog.getValue().trim();
        AbstractExplorerFileStore dstFileStore =
                fileStore.getParent().getChild(newName);
        
        AbstractExplorerFileInfo srcInfo = fileStore.fetchInfo();
        AbstractExplorerFileInfo destInfo = dstFileStore.fetchInfo();

        // Disallow case correction
        if (destInfo.exists() && name.equalsIgnoreCase(newName)) {
            showDisallowCaseCorrectionMessage();
            return null;
        }


        if (destInfo.exists()) {
            if ((srcInfo.isDirectory() ^ destInfo.isDirectory()) || (srcInfo.isFile() ^ destInfo.isFile())
                || (srcInfo.isMetaNode() ^ destInfo.isMetaNode()) || (srcInfo.isNode() ^ destInfo.isNode())
                || (srcInfo.isSnapshot() ^ destInfo.isSnapshot()) || (srcInfo.isWorkflow() ^ destInfo.isWorkflow())
                || (srcInfo.isWorkflowGroup() ^ destInfo.isWorkflowGroup())
                || (srcInfo.isWorkflowTemplate() ^ destInfo.isWorkflowTemplate())) {
                MessageBox mb =
                        new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
                mb.setText("Can't Rename");
                mb.setMessage("An item with the same name but a different type already exists. Overriding items of a "
                    + "different type is not allowed.");
                mb.open();
                return null;
            } else if (!confirmOverride(newName)) {
                return queryTargetName(dstFileStore);
            }
        }
        return dstFileStore;
    }

    private void showCantRenameLockMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Renaming");
        mb.setMessage("At least one of the workflows affected by the renaming "
                + "is still in use by another user/instance.\n"
                + "Canceling renaming.");
        mb.open();
    }

    private void showCantRenameOpenMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Rename");
        mb.setMessage("At least one of the workflows affected by the renaming"
                + " are still opened in the editor and have to be closed.");
        mb.open();
    }

    private boolean confirmOverride(final String name) {
        return MessageDialog.openConfirm(getParentShell(), "Confirm Overwrite",
                "Rename target \"" + name + "\" already exists and will be "
                        + "deleted/overwritten!\n\n"
                        + "Click 'Ok' to overwrite existing target.\n"
                        + "Click 'Cancel' to enter a new target name.");
    }

    private void showDisallowCaseCorrectionMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Rename");
        mb.setMessage("The new name matches the old name.");
        mb.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        // only a single selected file store can be renamed
        List<AbstractExplorerFileStore> selFiles = getAllSelectedFiles();
        if (isRO() || selFiles == null || selFiles.size() != 1) {
            return false;
        }
        AbstractExplorerFileStore fs = selFiles.get(0);
        return fs.canRename();
    }
}
