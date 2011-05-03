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

package org.knime.workbench.explorer.view;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class FileDropListener extends ViewerDropAdapter {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            FileDropListener.class);
    /**
     * @param viewer the viewer to which this drop support has been added
     */
    protected FileDropListener(final Viewer viewer) {
        super(viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data) {
        LOGGER.debug("performDrop with data: " + data);
        if (getCurrentTarget() instanceof ContentObject) {
            return ((ContentObject)getCurrentTarget())
                    .getProvider().performDrop(data);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final Object target, final int operation,
            final TransferData transferType) {
        if (!LocalSelectionTransfer.getTransfer().isSupportedType(
                transferType)) {
            LOGGER.info("Only LocalSelectionTransfer can be dropped. Got "
                    + transferType + ".");
            return false;
        }
        boolean valid = false;
        if (target instanceof ContentObject) {
            // delegate the validation to the content provider
            valid = ((ContentObject)target).getProvider().validateDrop(
                    target, operation, transferType);
        }
        if (valid) {
            LOGGER.debug("Successfully completed drop validation for target \""
                    + target + "\"");
        } else {
            LOGGER.debug("Drop validation for target \"" + target
                    + "\" failed.");
        }
        return valid;
    }

}
