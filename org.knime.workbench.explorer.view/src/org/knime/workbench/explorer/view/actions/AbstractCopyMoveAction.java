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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog.SelectionValidator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 * Abstract base class for copy and move actions for the Explorer. It contains
 * basically all necessary functionality. Derived classes only have to
 * override the name and set the move flag.
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class AbstractCopyMoveAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AbstractCopyMoveAction.class);
    private AbstractExplorerFileStore m_target;
    private final boolean m_performMove;

    /**
     * Creates a new copy/move action which displays a dialog to select the
     * target file store.
     *
     * @param viewer viewer of the space
     * @param menuText the text to be displayed in the menu
     * @param performMove true to move the files, false to copy them
     */
    public AbstractCopyMoveAction(final TreeViewer viewer,
            final String menuText, final boolean performMove) {
        this(viewer, menuText, null, performMove);
    }

    /**
     * Creates a new copy/move action that copies/moves the selected files to
     * the target file store.
     *
     * @param viewer viewer of the space
     * @param menuText the text to be displayed in the menu
     * @param target the file store to copy/move the files to
     * @param performMove true to move the files, false to copy them
     */
    public AbstractCopyMoveAction(final TreeViewer viewer,
            final String menuText, final AbstractExplorerFileStore target,
            final boolean performMove) {
        super(viewer, menuText);
        m_target = target;
        m_performMove = performMove;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<AbstractExplorerFileStore> fileStores = removeSelectedChildren(
                getAllSelectedFiles());
        // open browse dialog for target selection if necessary
        if (m_target == null) {
            boolean showServer = !isMultipleSelection();
            List<String> mountIDs = new ArrayList<String>();
            for (Map.Entry<String, AbstractContentProvider> entry
                    : ExplorerMountTable
                    .getMountedContent().entrySet()) {
                String mountID = entry.getKey();
                AbstractContentProvider acp = entry.getValue();
                if (showServer || !acp.isRemote()) {
                    mountIDs.add(mountID);
                }
            }
            String[] shownMountIds = mountIDs.toArray(new String[0]);
            SpaceResourceSelectionDialog dialog =
                    new SpaceResourceSelectionDialog(Display
                            .getDefault().getActiveShell(),
                            shownMountIds, null);
            dialog.setTitle("Target workflow group selection");
            dialog.setDescription(
                    "Please select the location to "
                    + (m_performMove ? "move" : "copy")
                    + " the selected files to.");
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
                m_target = dialog.getSelection();
            } else {
                return;
            }

        }
        boolean success = m_target.getContentProvider().copyOrMove(fileStores,
                m_target, m_performMove);
        if (!success) {
            LOGGER.error(m_performMove ? "Moving" : "Copying" + " to \""
                    + m_target.getFullName() + "\" failed.");
        } else {
            LOGGER.debug("Successfully "
                    + (m_performMove ? "moved " : "copied ")
                    + fileStores.size() + " item(s) to \""
                    + m_target.getFullName() + "\".");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        List<AbstractExplorerFileStore> selections = getAllSelectedFiles();
        return selections != null && !selections.isEmpty()
                && (ExplorerFileSystemUtils.isLockable(selections) == null);
    }

}
