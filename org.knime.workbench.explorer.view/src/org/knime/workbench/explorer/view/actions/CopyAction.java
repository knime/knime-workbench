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
  *   Oct 31, 2011 (morent): created
  */

package org.knime.workbench.explorer.view.actions;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class CopyAction extends AbstractCopyMoveAction {

    /** ID of the global rename action in the explorer menu. */
    public static final String ACTION_ID =
            "org.knime.workbench.explorer.action.copy";

    /**
     * @param viewer
     * @param menuText
     */
    public CopyAction(final TreeViewer viewer) {
        super(viewer, "Copy...");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {


    }

    @Override
    public boolean isEnabled() {
        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selections
                = getSelectedFiles();
        System.out.println(selections);
//        for (AbstractContentProvider acp : selections.keySet()) {
//
//        }

        return false;
    }


}
