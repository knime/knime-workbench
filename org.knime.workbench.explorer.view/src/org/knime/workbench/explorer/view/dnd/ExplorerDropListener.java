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

import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.WorkflowManagerTransfer;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
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
    private String m_srcMountID;

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
        Object target = getCurrentTarget();
        AbstractExplorerFileStore dstFS = DragAndDropUtils.getFileStore(target);
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
            AbstractExplorerFileStore dstFS
                    = DragAndDropUtils.getFileStore(target);
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
        super.dragEnter(event);
        if (!isSameMountPoint(event)
                && (event.detail == DND.DROP_DEFAULT
                        || event.detail == DND.DROP_MOVE)) {
              event.detail = DND.DROP_COPY;
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(final DropTargetEvent event) {
        super.dragLeave(event);
        // reset the cached source mount id
        m_srcMountID = null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetEvent event) {
        super.dragOver(event);
        if (!isSameMountPoint(event)) {
            if (event.detail == DND.DROP_DEFAULT
                        || event.detail == DND.DROP_MOVE) {
                event.detail = DND.DROP_COPY;
            }
        } else { // same mount point
            if (event.detail == DND.DROP_COPY) {
                event.detail = DND.DROP_MOVE;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOperationChanged(final DropTargetEvent event) {
        super.dragOperationChanged(event);
        /* Swap copy and move operations when entering (modifier pressed or
         * released). */
        if (!isSameMountPoint(event)) {
            if (event.detail == DND.DROP_DEFAULT
                    || event.detail == DND.DROP_MOVE) {
                event.detail = DND.DROP_COPY;
            } else if (event.detail == DND.DROP_COPY) {
                event.detail = DND.DROP_MOVE;
            }
        }
    }

    /**
     * @param event the drop target event
     * @return true if source and target of the event have the same mount id,
     *      false otherwise
     */
    private boolean isSameMountPoint(final DropTargetEvent event) {
        TransferData transferType = event.currentDataType;
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        boolean isLocalTransfer = transfer.isSupportedType(transferType);
        if (isLocalTransfer
                || FileTransfer.getInstance().isSupportedType(transferType)
                || WorkflowManagerTransfer.getTransfer().isSupportedType(
                        transferType)) {
            String dstMountId = getDstMountID(event.item.getData());
            String srcMountId = getSrcMountID(transfer.getSelection());
            return srcMountId != null && dstMountId != null
                    && srcMountId.equals(dstMountId);
        }
        return false;
    }

    private String getDstMountID(final Object data) {
        if (data instanceof ContentObject) {
            return ((ContentObject)data).getProvider().getMountID();
        } else if (data instanceof AbstractContentProvider){
            return ((AbstractContentProvider)data).getMountID();
        }
        return null;
    }

    private String getSrcMountID(final ISelection selection) {
        if (m_srcMountID != null) {
            return m_srcMountID;
        }

        if (selection instanceof IStructuredSelection) {
          IStructuredSelection ss = (IStructuredSelection)selection;
          List<AbstractExplorerFileStore> srcFS =
                  DragAndDropUtils.getExplorerFileStores(ss);
          if (srcFS != null && srcFS.size() > 0) {
              m_srcMountID = srcFS.get(0).getMountID();
              return m_srcMountID;
          }
        }
        return null;
    }
}
