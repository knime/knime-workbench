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
 *   14.08.2009 (ohl): created
 */
package org.knime.workbench.explorer.view.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class CollapseAllAction extends ExplorerAction {

    private static final ImageDescriptor IMG_COLLALL =
        AbstractUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                "icons/fav/collapseall.png");

    private static final String TOOLTIP = "Collapses the entire tree";

    /**
     * @param viewer
     */
    public CollapseAllAction(final TreeViewer viewer) {
        super(viewer);
        setImageDescriptor(IMG_COLLALL);
        setToolTipText(TOOLTIP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        getViewer().collapseAll();
    }

}
