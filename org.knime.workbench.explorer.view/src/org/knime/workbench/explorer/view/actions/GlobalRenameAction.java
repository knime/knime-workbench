/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.AbstractContentProvider;
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
    public GlobalRenameAction(final TreeViewer viewer) {
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
        AbstractContentProvider acp = DragAndDropUtils.getContentProvider(
                selection.iterator().next());

        List<ExplorerFileStore> stores
                = DragAndDropUtils.getExplorerFileStores(selection);
        ExplorerFileStore srcFileStore = stores.get(0);

        // find affected workflows
        List<ExplorerFileStore> affectedFlows = getContainedWorkflows(stores);

        // try locking all workflows for renaming
        LinkedList<ExplorerFileStore> lockedWFs =
                new LinkedList<ExplorerFileStore>();
        if (affectedFlows.size() > 0) {
            LinkedList<ExplorerFileStore> unlockableWFs =
                    new LinkedList<ExplorerFileStore>();
            ExplorerFileSystemUtils.lockWorkflows(affectedFlows, unlockableWFs, lockedWFs);
            if (unlockableWFs.size() > 0) {
                // release locks acquired for renaming
                ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
                showCantRenameLockMessage();
                return;
            }
        }

        ExplorerFileStore dstFileStore = queryTargetName(srcFileStore);
        if (dstFileStore == null) {
            // dialog was cancelled
            ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
            return;
        }

        if (ExplorerFileSystemUtils.hasOpenWorkflows(affectedFlows)) {
            // release locks acquired for renaming
            ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
            showCantRenameOpenMessage();
            return;
        }

        /* Unfortunately we have to unlock the workflows before moving. If we
         * move the workflows including the locks, we loose the possibility to
         * unlock them. */
        ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
        try {
            srcFileStore.move(dstFileStore, EFS.NONE, null);
//            unlockDstWorkflows(acp, srcFileStore, dstFileStore, lockedWFs);
            LOGGER.debug("Renamed \"" + srcFileStore + "\" to \""
                    + dstFileStore + "\".");
        } catch (CoreException e) {
            String message = "Could not rename \"" + srcFileStore + "\" to \""
                    + dstFileStore + "\".";
            LOGGER.error(message, e);
            ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
            MessageDialog.openError(getParentShell(), "Renaming failed",
                    message);
        }
        // TODO only refresh the updated part of the view
        getViewer().refresh(acp);


    }

    /**
     * Constructs the new names of the locked workflows and removes the lock.
     * @param acp the content provider of the file stores
     * @param srcFileStore the file store selected for renaming
     * @param lockedWFs the locked workflows
     */
    @SuppressWarnings("unused") // might be needed later
    private void unlockDstWorkflows(
            final AbstractContentProvider acp,
            final ExplorerFileStore srcFileStore,
            final ExplorerFileStore dstFileStore,
            final List<ExplorerFileStore> lockedWFs) {
        String srcName = srcFileStore.getFullName();
        String dstName = dstFileStore.getFullName();
        List<ExplorerFileStore> dstStores = new ArrayList<ExplorerFileStore>();
        for (ExplorerFileStore fs : lockedWFs) {
            String dstStoreName = fs.getFullName().replace(srcName, dstName);
            ExplorerFileStore dstFs = acp.getFileStore(dstStoreName);
            dstStores.add(dstFs);
        }
        ExplorerFileSystemUtils.unlockWorkflows(dstStores);
    }

    private ExplorerFileStore queryTargetName(
            final ExplorerFileStore fileStore) {
        Shell shell = Display.getDefault().getActiveShell();
        String name = fileStore.getName();
        InputDialog dialog = new InputDialog(shell,
                "Rename", "Please enter the new name for \""
                + name + "\"", name, new FileStoreNameValidator());
        dialog.setBlockOnOpen(true);

        if (dialog.open() == InputDialog.CANCEL) {
            return null;
        }
        String newName = dialog.getValue();
        ExplorerFileStore dstFileStore
                = fileStore.getParent().getChild(newName);
        if (dstFileStore.fetchInfo().exists() && !confirmOverride(newName)) {
            queryTargetName(dstFileStore);
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
        return MessageDialog.openConfirm(getParentShell(),
                "Confirm Overwrite",
                "Rename target \"" + name + "\" already exists and will be "
                        + "deleted/overwritten!\n\n"
                        + "Click 'Ok' to overwrite existing target.\n"
                        + "Click 'Cancel' to enter a new target name.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        // only a single selected file store can be renamed
        return !isMultipleSelection()
            && DragAndDropUtils.getExplorerFileStores(getSelection()) != null;
    }

    /**
     * Checks for valid {@link ExplorerFileStore} file names.
     *
     * @author Dominik Morent, KNIME.com, Zurich, Switzerland
     *
     */
    class FileStoreNameValidator implements IInputValidator {
        /**
         * {@inheritDoc}
         */
        @Override
        public String isValid(final String name) {
            if (!ExplorerFileSystem.isValidFilename(name)) {
                return "One of the following illegal characters is used: "
                        + ExplorerFileSystem.getIllegalFilenameChars();
            } else {
                return null;
            }
        }

    }

}
