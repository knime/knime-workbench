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
  *   Apr 28, 2011 (morent): created
  */

package org.knime.workbench.explorer.view.dnd;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.WorkflowManagerTransfer;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ContentObject;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerDropListener extends ViewerDropAdapter {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExplorerDropListener.class);
    /**
     * @param viewer the viewer to which this drop support has been added
     */
    public ExplorerDropListener(final TreeViewer viewer) {
        super(viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data) {
        LOGGER.debug("performDrop with data: " + data);
        // open confirmation dialog if moving within the explorer
        if (LocalSelectionTransfer.getTransfer().isSupportedType(
                getCurrentEvent().currentDataType)
                && DND.DROP_MOVE == getCurrentOperation()) {
            MessageBox mb = new MessageBox(
                    Display.getDefault().getActiveShell(),
                    SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
            mb.setText("Confirm Move");
            mb.setMessage("Are you sure that you want to move the selected "
                    + "items?");
            if (mb.open() == SWT.CANCEL) {
                return false;
            }
        }
        Object target = getCurrentTarget();
        ExplorerFileStore dstFS = DragAndDropUtils.getFileStore(target);
        AbstractContentProvider acp = DragAndDropUtils.getContentProvider(
                target);
        boolean result = acp.performDrop(data, dstFS, getCurrentOperation());
        getViewer().refresh(ContentDelegator.getTreeObjectFor(dstFS));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final Object target, final int operation,
            final TransferData transferType) {
        boolean isLocalTransfer = LocalSelectionTransfer.getTransfer()
                .isSupportedType(transferType);

        if (isLocalTransfer
                || FileTransfer.getInstance().isSupportedType(transferType)
                || WorkflowManagerTransfer.getTransfer().isSupportedType(
                        transferType)) {
            ExplorerFileStore dstFS = DragAndDropUtils.getFileStore(target);
            AbstractContentProvider acp = DragAndDropUtils.getContentProvider(
                    target);
            if (dstFS == null || acp == null) {
                return false;
            }
            if (isLocalTransfer) {
                IFileStore parent = ((ContentObject)
                        getSelectedObject()).getObject().getParent();
                if (getSelectedObject() == target || dstFS.equals(parent)) {
                    return false;
                }
            }
            // delegate the validation to the content provider
            return acp.validateDrop(dstFS, operation, transferType);
        } else {
            LOGGER.warn("Only files and items of the KNIME Explorer can be "
                    + "dropped.");
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TreeViewer getViewer() {
        return (TreeViewer)super.getViewer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragEnter(final DropTargetEvent event) {
        // use copy as default behavior
        event.detail = DND.DROP_COPY;
        super.dragEnter(event);
    }
}
