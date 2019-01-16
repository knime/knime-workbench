/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * Created: May 23, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.DeletionConfirmationResult;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 *
 * @author ohl, University of Konstanz
 */
public class GlobalDeleteAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalDeleteAction.class);

    /**
     * ID of the global delete action in the explorer menu.
     */
    public static final String DELETEACTION_ID =
            "org.knime.workbench.explorer.action.delete";

    private static final ImageDescriptor IMG_DELETE = PlatformUI.getWorkbench()
            .getSharedImages()
            .getImageDescriptor(ISharedImages.IMG_TOOL_DELETE);

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalDeleteAction(final ExplorerView viewer) {
        super(viewer, "Delete...");
        setImageDescriptor(IMG_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return DELETEACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selectedFiles =
                getSelectedFiles();
        List<AbstractExplorerFileStore> allFiles =
                new LinkedList<AbstractExplorerFileStore>();

        // remove "double selection" (child whose parents are selected as well)
        for (Map.Entry<AbstractContentProvider, List<AbstractExplorerFileStore>> e : selectedFiles
                .entrySet()) {
            List<AbstractExplorerFileStore> sel =
                    removeSelectedChildren(e.getValue());
            allFiles.addAll(sel);
        }
        // find workflows included in selection
        List<AbstractExplorerFileStore> toDelWorkflows =
                getAllContainedWorkflows(allFiles);
        List<LocalExplorerFileStore> toDelLocalFlows =
                getContainedLocalWorkflows(allFiles);
        List<AbstractExplorerFileStore> toDelJobs =
                getAllContainedJobs(allFiles);
        // try locking all local workflows for deletion
        LinkedList<LocalExplorerFileStore> lockedWFs =
                new LinkedList<LocalExplorerFileStore>();
        if (toDelLocalFlows.size() > 0) {
            LinkedList<LocalExplorerFileStore> unlockableWFs =
                    new LinkedList<LocalExplorerFileStore>();
            ExplorerFileSystemUtils.lockWorkflows(toDelLocalFlows,
                    unlockableWFs, lockedWFs);
            if (unlockableWFs.size() > 0) {
                // release locks acquired for deletion
                ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
                showCantDeleteMessage();
                return;
            }
        }

        assert lockedWFs.size() == toDelLocalFlows.size();
        // ask all affected providers if it is OK to delete
        boolean ok = false;
        // TODO: avoid multiple dialogs bothering the user multiple times (is becomes an issue only when multiple
        // providers pop dialogs - currently the server is the only provides).
        Map<AbstractContentProvider, DeletionConfirmationResult> confResults =
            new TreeMap<AbstractContentProvider, DeletionConfirmationResult>();
        for (AbstractContentProvider acp : selectedFiles.keySet()) {
            DeletionConfirmationResult confirmed = acp.confirmDeletion(getParentShell(), allFiles, toDelWorkflows);
            if (confirmed != null) {
                if (!confirmed.confirmed()) {
                    LOGGER.info("User canceled deletion of " + allFiles.size() + " files incl. "
                        + toDelWorkflows.size() + " workflows");
                    ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
                    return;
                } else {
                    ok = true;
                    confResults.put(acp, confirmed);
                }
            }
        }
        if (!ok) { // none of the providers popped a confirm dialog
            if (!confirmDeletion(allFiles, toDelWorkflows)) {
                // release locks acquired for deletion
                ExplorerFileSystemUtils.unlockWorkflows(lockedWFs);
                return;
            }
        }

        ExplorerFileSystemUtils.closeOpenWorkflows(toDelJobs);
        ExplorerFileSystemUtils.closeOpenWorkflows(toDelWorkflows);

        new ExplorerJob("Deleting items.") {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                // delete Workflows first (unlocks them too)
                boolean success = ExplorerFileSystemUtils.deleteLockedWorkflows(lockedWFs, confResults);
                success &= ExplorerFileSystemUtils.deleteTheRest(allFiles, confResults);

                if (!success) {
                    Display.getDefault().syncExec(() -> showUnsuccessfulMessage());
                }

                Display.getDefault().syncExec(() -> {
                    for (AbstractExplorerFileStore fileStore : allFiles) {
                        AbstractExplorerFileStore parentStore = fileStore.getParent();

                        /* Select the correct parent for REST explorer. In case of a reserved system item it could be
                         * that the parent is visible for a split second without icon if its last job gets deleted. Thus
                         * it does not exist so we have to refresh the correct existing ancestor. */
                        while (parentStore.getParent() != null && !parentStore.fetchInfo().exists()) {
                            parentStore = parentStore.getParent();
                        }

                        Object parent = ContentDelegator.getTreeObjectFor(parentStore);
                        getViewer().refresh(parent);
                    }
                    ;
                });

                return Status.OK_STATUS;
            }
        }.schedule();
    }

    /**
     * @param toDel files to delete
     * @param toDelWFs flows contained in the files to delete (directly or indirectly)
     * @return true if user confirmed, false if user cancels.
     * @since 5.0
     */
    protected boolean confirmDeletion(
            final List<AbstractExplorerFileStore> toDel,
            final List<AbstractExplorerFileStore> toDelWFs) {

        String msg = "";
        if (toDel.size() == 1) {
            if (AbstractExplorerFileStore.isWorkflow(toDel.get(0))) {
                msg = "Do you want to delete the workflow ";
            } else if (AbstractExplorerFileStore.isWorkflowGroup(toDel.get(0))) {
                msg = "Do you want to delete the workflow group ";
            } else {
                msg = "Do you want to delete the file ";
            }
            msg += "\"" + toDel.get(0).getName() + "\"?\n";
            if (AbstractExplorerFileStore.isWorkflowGroup(toDel.get(0)) && (toDelWFs.size() > 0)) {
                msg += "It contains " + toDelWFs.size() + " workflow";
                if (toDelWFs.size() > 1) {
                    msg += "s";
                }
                msg += ".\n";
            }
        } else {
            int stuff = toDel.size() - toDelWFs.size();
            if (stuff == 0) {
                msg =
                        "Do you want to delete all " + toDel.size()
                                + " selected workflows?\n";
            } else {
                if (stuff == 1) {
                    msg =
                            "Do you want to delete the selected item \""
                                    + toDel.get(0).getName() + "\"?\n";
                } else {
                    msg =
                            "Do you want to delete all " + toDel.size()
                                    + " selected items?\n";
                }
                if (toDelWFs.size() > 0) {
                    if (stuff == 1) {
                        msg += "It contains ";
                    } else {
                        msg += "They contain ";
                    }
                    msg += toDelWFs.size() + " workflow";
                    if (toDelWFs.size() > 1) {
                        msg += "s";
                    }
                    msg += ".\n";
                }
            }
        }
        msg += "\nPlease confirm the deletion.";

        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_QUESTION | SWT.OK
                        | SWT.CANCEL);
        mb.setMessage(msg);
        mb.setText("Confirm Deletion");
        if (mb.open() != SWT.OK) {
            LOGGER.debug("Deletion of " + toDel.size()
                    + " items canceled by user.");
            return false;
        } else {
            LOGGER.debug("Deletion of " + toDel.size()
                    + " items confirmed by user.");
            return true;
        }

    }

    private void showCantDeleteMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Deletion");
        mb.setMessage("At least one of the workflows can't be deleted.\n"
                + " It is probably in use by another user/instance.\n"
                + "Canceling deletion.");
        mb.open();
    }

    private void showUnsuccessfulMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Delete All Selected Items");
        mb.setMessage("At least one item could not be deleted.\n"
                + "Some might have been successfully deleted.");
        mb.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        List<AbstractExplorerFileStore> selFiles = getAllSelectedFiles();
        if (isRO() || selFiles == null || selFiles.size() == 0) {
            return false;
        }

        final String mountID = selFiles.get(0).getMountID();

        for (AbstractExplorerFileStore fs : selFiles) {
            if (!fs.canDelete() || !mountID.equals(fs.getMountID())) {
                return false;
            }
        }
        return true;
    }
}
