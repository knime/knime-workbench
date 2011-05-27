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
 *   May 23, 2011 (morent): created
 */

package org.knime.workbench.explorer.view.actions;

import org.eclipse.jface.viewers.TreeViewer;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class CopyAction extends ExplorerAction {
    private static final String TOOLTIP = "Copy the selected resource";

    /**
     *
     * @param viewer the viewer this action is attached to
     */
    public CopyAction(final TreeViewer viewer) {
        super(viewer, "Copy...");
        setToolTipText(TOOLTIP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.workbench.explorer.view.action.copy";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // TODO Auto-generated method stub
        super.run();
    }
}
