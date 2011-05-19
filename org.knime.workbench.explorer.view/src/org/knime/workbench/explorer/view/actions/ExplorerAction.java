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
 */
package org.knime.workbench.explorer.view.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;

/**
 * Actions used in the UserSpace view should derive from this. It provides some
 * convenient methods.
 *
 * @author ohl, University of Konstanz
 */
public abstract class ExplorerAction extends Action {

    private final TreeViewer m_viewer;

    /**
     * @param viewer of the space
     */
    public ExplorerAction(final TreeViewer viewer, final String menuText) {
        assert viewer != null;
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getId();

    /**
     * @return the current selection in the corresponding view, could be null
     */
    protected IStructuredSelection getSelection() {
        IStructuredSelection selection =
                (IStructuredSelection)getViewer().getSelection();
        return selection;
    }

    /**
     * @return the viewer associated with the action.
     */
    protected TreeViewer getViewer() {
        return m_viewer;
    }

    /**
     * @return the parent shell of the viewer
     */
    protected Shell getParentShell() {
        return m_viewer.getControl().getShell();
    }

}
