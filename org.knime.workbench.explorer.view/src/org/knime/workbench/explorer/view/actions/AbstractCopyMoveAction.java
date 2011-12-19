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

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog.SelectionValidator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.explorer.view.ExplorerView;

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
    public AbstractCopyMoveAction(final ExplorerView viewer,
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
    public AbstractCopyMoveAction(final ExplorerView viewer,
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


        String message = ExplorerFileSystemUtils.isLockable(fileStores);
        if (fileStores == null || fileStores.isEmpty()
                || message != null) {
            MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
            String action = m_performMove ? "Move" : "Copy";
            mb.setText("Can't " + action + " All Selected Items");
            mb.setMessage(message);
            mb.open();
            return;
        }

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
            ContentObject initialSelection = null;
            if (fileStores.size() == 1) {
                Object selection = ContentDelegator.getTreeObjectFor(
                        fileStores.get(0).getParent());
                if (selection instanceof ContentObject) {
                    initialSelection = (ContentObject)selection;
                }
            }

            String[] shownMountIds = mountIDs.toArray(new String[0]);
            SpaceResourceSelectionDialog dialog =
                    new SpaceResourceSelectionDialog(Display
                            .getDefault().getActiveShell(),
                            shownMountIds, initialSelection);
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
        if (isRO() && m_performMove) {
            return false;
        }
        Map<AbstractContentProvider, List<AbstractExplorerFileStore>>
                selProviders = getSelectedFiles();
        if (selProviders.size() != 1) {
            // can only copy/move from one source content provider
            return false;
        }
        List<AbstractExplorerFileStore> selections =
                selProviders.values().iterator().next();
        if (selections == null || selections.isEmpty()) {
            return false;
        }
        AbstractExplorerFileStore fileStore = selections.get(0);
        if (fileStore instanceof RemoteExplorerFileStore) {
            // currently we can only download one workflow or metanode template
            if (selections.size() > 1) {
                return false;
            }

            AbstractExplorerFileInfo info = fileStore.fetchInfo();
            if (!(info.isWorkflow() || info.isWorkflowTemplate())) {
                return false;
            }
        }
        if (m_performMove) {
            return fileStore.canMove();
        } else {
            return fileStore.canCopy();
        }
    }

}
