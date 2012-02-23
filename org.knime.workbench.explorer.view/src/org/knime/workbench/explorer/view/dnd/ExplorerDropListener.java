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
import org.knime.workbench.explorer.view.ExplorerView;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerDropListener extends ViewerDropAdapter {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExplorerDropListener.class);
    private String m_srcMountID;
    private final ExplorerView m_view;
    private int m_default;

    /**
     * @param view the viewer to which this drop support has been added
     */
    public ExplorerDropListener(final ExplorerView view) {
        super(view.getViewer());
        m_view = view;
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
        boolean result = acp.performDrop(m_view, data, dstFS,
                getCurrentOperation());
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
                if (getSelectedObject() == target) {
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
        /* Set move as default DND operation within a mount point and copy
         * as default operation between different mount points. */
        if (!isSameMountPoint(event)) {
              event.detail = DND.DROP_COPY;
              m_default = DND.DROP_COPY;
        } else {
            event.detail = DND.DROP_MOVE;
            m_default = DND.DROP_MOVE;
        }
        super.dragEnter(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(final DropTargetEvent event) {
        // reset the cached source mount id
        m_srcMountID = null;
        super.dragLeave(event);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetEvent event) {
        int previousDefault = m_default;
        // change the default operation on mount point changes if necessary
        if (!isSameMountPoint(event)) {
            m_default = DND.DROP_COPY;
        } else { // same mount point
            m_default = DND.DROP_MOVE;
        }
        if (m_default != previousDefault && event.detail == previousDefault) {
            /* If the default operation was performed change the event to the
             * new default, otherwise keep the current one. */
            event.detail = m_default;
        }
        super.dragOver(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOperationChanged(final DropTargetEvent event) {
            // modifier key released
        if (m_default == DND.DROP_COPY && event.detail == DND.DROP_MOVE) {
            /* Keep copy as default operation for DND operations between
             * different mount points. */
            event.detail = DND.DROP_COPY;
//            int prevDetail = event.detail;
//            LOGGER.debug("dragOperationChanged changed event.detail from "
//                    + getDNDOp(prevDetail) + " to "
//                    + getDNDOp(event.detail));
        }

        super.dragOperationChanged(event);
    }

//    private String getDNDOp(final int operation) {
//        switch (operation) {
//        case DND.DROP_DEFAULT:
//            return "DND.DROP_DEFAULT";
//        case DND.DROP_COPY:
//            return "DND.DROP_COPY";
//        case DND.DROP_MOVE:
//            return "DND.DROP_MOVE";
//        case DND.DROP_NONE:
//            return "DND.DROP_NONE";
//        default:
//            return operation + "";
//        }
//    }

    /**
     * @param event the drop target event
     * @return true if source and target of the event have the same mount id,
     *      false otherwise
     */
    private boolean isSameMountPoint(final DropTargetEvent event) {
        if (event.item == null) {
            // happens if drag is leaving the window
            return false;
        }
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
