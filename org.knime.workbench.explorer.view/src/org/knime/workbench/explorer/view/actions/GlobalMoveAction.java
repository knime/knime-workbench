/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright by 
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

import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 * Action to move files to another location in KNIME Explorer.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class GlobalMoveAction extends AbstractCopyMoveAction {

    /** ID of the global move action in the explorer menu. */
    public static final String ACTION_ID =
            "org.knime.workbench.explorer.action.move";

    /**
     * @param viewer the viewer
     */
    public GlobalMoveAction(final ExplorerView viewer) {
        super(viewer, "Move...", true);
    }

    /**
     * Creates a new copy/move action that copies/moves the source files to
     * the target file store.
     *
     * @param view viewer of the space
     * @param sources the file stores to copy
     * @param target the file store to copy/move the files to
     */
    public GlobalMoveAction(final ExplorerView view,
            final List<AbstractExplorerFileStore> sources,
            final AbstractExplorerFileStore target) {
        super(view, "Move...", sources, target, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ACTION_ID;
    }

}
