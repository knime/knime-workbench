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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
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
    public ExplorerDropListener(final Viewer viewer) {
        super(viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data) {
        LOGGER.debug("performDrop with data: " + data);
        Object target = getCurrentTarget();
        ExplorerFileStore dstFS = DragAndDropUtils.getFileStore(target);
        AbstractContentProvider acp = DragAndDropUtils.getContentProvider(
                target);
        return acp.performDrop(data, dstFS, getCurrentOperation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final Object target, final int operation,
            final TransferData transferType) {
        if (!LocalSelectionTransfer.getTransfer().isSupportedType(
                transferType)) {
            LOGGER.debug("Only LocalSelectionTransfer can be dropped. Got "
                    + transferType + ".");
            return false;
        }
        ExplorerFileStore dstFS = DragAndDropUtils.getFileStore(target);
        AbstractContentProvider acp = DragAndDropUtils.getContentProvider(
                target);
        if (dstFS == null || acp == null) {
            return false;
        }
        IFileStore parent = ((ContentObject)getSelectedObject()).getObject()
            .getParent();
        if (getSelectedObject() == target || dstFS.equals(parent)) {
            LOGGER.debug("Cannot drop an item on itself.");
            return false;
        }
        // delegate the validation to the content provider
        return acp.validateDrop(dstFS, operation, transferType);
    }

}
