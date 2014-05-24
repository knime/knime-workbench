/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
public class ExplorerDropListener extends ViewerDropAdapter {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExplorerDropListener.class);
    private String m_srcMountID;
    private final ExplorerView m_view;
    private int m_default;
    private Boolean m_canMove = null;

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
                Object selObj = getSelectedObject();
                if (selObj instanceof ContentObject) {
                    ContentObject selContent = (ContentObject)selObj;
                    if (selContent == target) {
                        return false;
                    }
                    if (operation == DND.DROP_MOVE && dstFS.equals(selContent.getObject().getParent())) {
                        //don't move to the own parent
                        return false;
                    }
                }
            }
            // delegate the validation to the content provider
            return acp.validateDrop(dstFS, operation, transferType);
        } else {
            LOGGER.warn("Only files and items of the KNIME Explorer or the filesystem can be dropped.");
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
        m_default = getDefaultOperation(event);
        event.detail = m_default;
        super.dragEnter(event);
    }

    private int getDefaultOperation(final DropTargetEvent event) {
        if (!isSameMountPoint(event)) {
            return DND.DROP_COPY;
        } else {
            if (srcCanMove(event)) {
                return DND.DROP_MOVE;
            } else {
                return DND.DROP_COPY;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragLeave(final DropTargetEvent event) {
        // reset the cached source mount id
        m_srcMountID = null;
        m_canMove = null;
        super.dragLeave(event);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dragOver(final DropTargetEvent event) {
        int previousDefault = m_default;
        // change the default operation on mount point changes if necessary
        m_default = getDefaultOperation(event);
        if (m_default != previousDefault // default has changed
                // option key is not pressed
                && (event.operations & DND.DROP_MOVE) != 0
                // detail was default or none
                && (event.detail == previousDefault
                        || event.detail == DND.DROP_NONE)) {
            /* If the default operation was performed and no option key was
             * pressed change the event to the
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
        }

        super.dragOperationChanged(event);
    }

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

    private boolean srcCanMove(final DropTargetEvent event) {
        if (m_canMove == null) {
            if (event.item != null) {
                TransferData transferType = event.currentDataType;
                LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
                boolean isLocalTransfer = transfer.isSupportedType(transferType);
                if (isLocalTransfer || FileTransfer.getInstance().isSupportedType(transferType)
                    || WorkflowManagerTransfer.getTransfer().isSupportedType(transferType)) {
                    ISelection selection = transfer.getSelection();
                    if (selection instanceof IStructuredSelection) {
                        IStructuredSelection ss = (IStructuredSelection)selection;
                        List<AbstractExplorerFileStore> srcFS = DragAndDropUtils.getExplorerFileStores(ss);
                        if (srcFS != null && srcFS.size() == 1) {
                            m_canMove = srcFS.get(0).canMove();
                        }
                    }
                }
            }
            if (m_canMove == null) {
                // default answer is yes
                m_canMove = Boolean.TRUE;
            }
        }
        return m_canMove.booleanValue();
    }

}
