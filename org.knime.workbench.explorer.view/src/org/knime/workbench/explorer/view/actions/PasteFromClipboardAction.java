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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ExplorerFileStoreTransfer;
import org.knime.workbench.explorer.view.ExplorerView;

public class PasteFromClipboardAction extends AbstractCopyMoveAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PasteFromClipboardAction.class);
//    private AbstractExplorerFileStore RemoteExplorerFileStore;
    /**
     * @param viewer the viewer
     */
    public PasteFromClipboardAction(final ExplorerView viewer) {
        super(viewer, "Paste", false);
        // Disable by default to make sure an event is fired when enabled the
        // first time. Otherwise an inconsistent state is possible when the
        // (system) clipboard contains already a valid object at the KNIME start
        setEnabled(false);
    }

    /** The id of this action. */
    public static final String ID
            = "org.knime.workbench.explorer.view.actions.PasteFromClipboard";
    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
//        String[] avail = getView().getClipboard().getAvailableTypeNames();
//        LOGGER.debug("Available type names:");
//        for (String t : avail) {
//            LOGGER.debug(t);
//        }
//        LOGGER.debug(getView().getClipboard().getAvailableTypes());
        // check the clipboard content first
        ExplorerFileStoreTransfer transfer
                = ExplorerFileStoreTransfer.getInstance();
        Object c = getView().getClipboard().getContents(transfer);
        URI[] uri = (URI[])c;
        if (c == null || !(c instanceof URI[])
                || uri.length == 0) {
            return false;
        }

        return isSelectionValid(uri);
    }


    /**
     * @return true if the selected target is valid for pasting content, false
     *      otherwise
     */
    private boolean isSelectionValid(final URI[] sourceURI) {
        // then check if the selected target is valid
        // only enabled if exactly on file is selected
        List<AbstractExplorerFileStore> files = getAllSelectedFiles();
        if (isRO() || files.size() == 0) {
            return false;
        }
        if (files.size() > 1) {
            // invalid if the files do not have a common parent
            AbstractExplorerFileStore parent = null;
            for (AbstractExplorerFileStore file : files) {
                if (parent != null && !parent.equals(file.getParent())) {
                    return false;
                }
                parent = file.getParent();
            }
        }

        AbstractExplorerFileStore file = files.get(0);
        AbstractExplorerFileInfo fileInfo = file.fetchInfo();
        AbstractContentProvider cp = file.getContentProvider();
        if ((cp.isRemote() && sourceURI.length > 1) || !cp.isWritable()) {
            return false;
        }
        // for workflow groups check if it is writable
        if (fileInfo.isWorkflowGroup()) {
            return fileInfo.isModifiable();
        } else {
            // for other types check if the parent is a writable workflow group
            final AbstractExplorerFileStore parent = file.getParent();
            if (parent == null) {
                // no parent = root
                return false;
            }
            AbstractExplorerFileInfo parentInfo = parent.fetchInfo();
            return parentInfo.isWorkflowGroup() && parentInfo.isModifiable();
        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        ExplorerFileStoreTransfer transfer
                = ExplorerFileStoreTransfer.getInstance();
        Clipboard clipboard = getView().getClipboard();
        Object c = clipboard.getContents(
                transfer);
        if (c == null || !(c instanceof URI[])
                || ((URI[])c).length == 0) {
            return;
        }
        List<AbstractExplorerFileStore> selection = getAllSelectedFiles();
        if (selection.size() == 1) {
            setTarget(selection.get(0));
        } else { // for multiple selection set the common parent as target
            setTarget(selection.get(0).getParent());
        }

        URI[] fileURI = (URI[]) c;
        List<AbstractExplorerFileStore> srcFileStores
        = new ArrayList<AbstractExplorerFileStore>(fileURI.length);
        for (URI uri : fileURI) {
            srcFileStores.add(ExplorerFileSystem.INSTANCE.getStore(uri));
        }

        // check if all affected flows can be copied/moved
        String message = ExplorerFileSystemUtils.isLockable(srcFileStores,
                !transfer.isCut());
        if (message != null) {
            LOGGER.warn("Can't paste from clipboard: " + message);
            MessageBox mb = new MessageBox(
                    Display.getCurrent().getActiveShell(),
                    SWT.ICON_ERROR | SWT.OK);
            mb.setText("Can't Paste All Selected Items.");
            mb.setMessage(message);
            mb.open();
            return;
        }
        setPerformMove(transfer.isCut());
        setSuccess(copyOrMove(srcFileStores));
        if (!isSuccessful()) {
            LOGGER.error(isPerformMove() ? "Moving" : "Copying" + " to \""
                    + getTarget().getFullName() + "\" failed.");
        } else {
            LOGGER.debug("Successfully "
                    + (isPerformMove() ? "moved " : "copied ")
                    + srcFileStores.size() + " item(s) to \""
                    + getTarget().getFullName() + "\".");
        }
        if (isPerformMove()) {
            clipboard.clearContents();
            updateSelection();
        }
    }


    /**
     * Updates this action in response to a selection change.
     */
    public void updateSelection() {
        setEnabled(isEnabled());
    }
}
