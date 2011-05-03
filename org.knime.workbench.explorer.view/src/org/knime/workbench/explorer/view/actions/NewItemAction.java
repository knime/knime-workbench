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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.ContentDelegator;


/**
 *
 * @author ohl, University of Konstanz
 */
public class NewItemAction extends ExplorerAction {

    private final ContentDelegator m_delegator;

    /**
     * @param viewer
     * @param delegator
     */
    public NewItemAction(final TreeViewer viewer,
            final ContentDelegator delegator) {
        super(viewer);
        assert delegator != null;
        m_delegator = delegator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // if a content is selected, make that the default for the new item
        AbstractContentProviderFactory acpf = null;
        IStructuredSelection sel = getSelection();
        if (sel != null && sel.size() == 1) {
            Object o = sel.getFirstElement();
            if (o instanceof AbstractContentProvider) {
                acpf = (AbstractContentProviderFactory)o;
            }
        }
        assert acpf == acpf;
       // m_delegator.createNewItem(acpf);
    }
}
