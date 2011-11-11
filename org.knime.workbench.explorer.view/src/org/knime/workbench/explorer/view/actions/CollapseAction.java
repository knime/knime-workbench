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

import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class CollapseAction extends ExplorerAction {

    /**
     * The icon for the collapse action.
     */
    public static final ImageDescriptor IMG_COLL =
        AbstractUIPlugin.imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
        "icons/collapse.png");

    private static final String TOOLTIP = "Collapse the selected element";

    /**
     *
     * @param viewer the viewer
     */
    public CollapseAction(final ExplorerView viewer) {
        super(viewer, "Collapse");
        setImageDescriptor(IMG_COLL);
        setToolTipText(TOOLTIP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.workbench.explorer.view.action.collapse";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        IStructuredSelection sel = getSelection();
        if (sel == null) {
            return;
        }
        Iterator<Object> iter = sel.iterator();
        if (iter == null) {
            return;
        }
        while (iter.hasNext()) {
            getViewer().collapseToLevel(iter.next(),
                    AbstractTreeViewer.ALL_LEVELS);
        }
    }
}
