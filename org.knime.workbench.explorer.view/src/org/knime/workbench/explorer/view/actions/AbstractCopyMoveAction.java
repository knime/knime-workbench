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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog.SelectionValidator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProvider;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class AbstractCopyMoveAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AbstractCopyMoveAction.class);

    /**
     * @param viewer
     * @param menuText
     */
    public AbstractCopyMoveAction(final TreeViewer viewer, final String menuText) {
        super(viewer, menuText);
    }

    /**
     * @param fileStores
     * @param performMove
     * @return
     */
    protected void run(final List<AbstractExplorerFileStore> fileStores,
            final boolean performMove) {
        run(fileStores, null, performMove);
    }

    /**
     * @param fileStores
     * @param target
     * @param performMove
     * @return
     */
    protected void run(final List<AbstractExplorerFileStore> fileStores,
            AbstractExplorerFileStore target, final boolean performMove) {
        // open browse dialog for target selection if necessary
        if (target == null) {
            String[] allMountIds = ExplorerMountTable.getAllMountIDs().toArray(
                    new String[0]);
            SpaceResourceSelectionDialog dialog =
                    new SpaceResourceSelectionDialog(Display
                            .getDefault().getActiveShell(),
                            allMountIds, null);
            dialog.setTitle("Target workflow group selection");
            dialog.setDescription(
                    "Please select the location to "
                    + (performMove ? "move" : "copy")
                    + " to.");
            dialog.setValidator(new SelectionValidator() {
                @Override
                public String isValid(
                        final AbstractExplorerFileStore selection) {
                    boolean isWFG = selection.fetchInfo().isWorkflowGroup();
                    return isWFG ? null
                            : "Only workflow groups can be selected as target.";
                }
            });
            dialog.scaleDialogSize(1, 2);
            if (Window.OK == dialog.open()) {
                target = dialog.getSelection();
            }

        }
        ((LocalWorkspaceContentProvider)target.getContentProvider()).
                copyOrMove(fileStores, target, performMove);
    }
}
