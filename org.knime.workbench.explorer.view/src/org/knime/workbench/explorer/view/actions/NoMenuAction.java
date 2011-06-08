/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Jun 7, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import org.eclipse.jface.viewers.TreeViewer;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NoMenuAction extends ExplorerAction {

    /**
     *
     */
    public NoMenuAction(final TreeViewer viewer) {
        super(viewer, "No visible content");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "com.knime.workbench.explorer.view.action.nomenu";
    }

}
