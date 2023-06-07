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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.explorer.view.DestinationChecker;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dialogs.DestinationSelectionDialog;
import org.knime.workbench.explorer.view.dialogs.DestinationSelectionDialog.SelectedDestination;

/**
 * Deploys a selected workflow or workflow group (single selection) to a KNIME Server.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 7.4
 */
public class GlobalDeploytoServerAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(GlobalDeploytoServerAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String DEPLOY_TO_SERVER_ACTION_ID = "org.knime.workbench.explorer.action.deployToServer";

    private static final ImageDescriptor ICON =
        AbstractUIPlugin.imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID, "icons/deploy_to_server.png");


    private static AbstractExplorerFileStore lastUsedLocation;

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalDeploytoServerAction(final ExplorerView viewer) {
        super(viewer, "Upload to Server or Hub");
        setImageDescriptor(ICON);
    }

    @Override
    public String getId() {
        return DEPLOY_TO_SERVER_ACTION_ID;
    }

    @Override
    public void run() {
        final var srcFileStore = getSingleSelectedElement()
                .orElseThrow(() -> new IllegalStateException("No single workflow, group, or file selected"));
        final var dialogTitle = "Upload " + srcFileStore.getName();

        final var lockableMessage = ExplorerFileSystemUtils.isLockable(Collections.singletonList(srcFileStore), true);
        if (lockableMessage != null) {
            final var messageBox = new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
            messageBox.setText("Can't Upload All Selected Items");
            messageBox.setMessage(lockableMessage);
            messageBox.open();
            return;
        }

        final var destInfoOptional = promptForTargetLocation(!AbstractExplorerFileStore.isDataFile(srcFileStore));
        if (!destInfoOptional.isPresent()) {
            LOGGER.debug(getText() + "canceled");
            return;
        }
        SelectedDestination destInfo = destInfoOptional.get();
        AbstractExplorerFileStore destination = destInfo.getDestination();

        final DestinationChecker<AbstractExplorerFileStore, AbstractExplorerFileStore> destChecker =
                new DestinationChecker<>(getParentShell(), "Copy", false, true);
        destChecker.setIsOverwriteEnabled(true);
        destChecker.setIsOverwriteDefault(true);
        destChecker.getAndCheckDestinationFlow(srcFileStore, destination);
        if (destChecker.isAbort()) {
            return;
        }

        final var copyMove = new CopyMove(getView(), destination, destChecker, false);
        copyMove.setExcludeDataInWorkflows(destInfo.isExcludeData());
        final List<IStatus> statusList = new LinkedList<>();
        try {
            // perform the copy/move operations en-bloc in the background
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    final var copyMoveResult = copyMove.run(monitor);
                    statusList.addAll(copyMoveResult.getStatusList());
                }
            });
        } catch (InvocationTargetException e) {
            LOGGER.debug("Invocation exception, " + e.getMessage(), e);
            statusList.add(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                "invocation error: " + e.getMessage(), e));
        } catch (InterruptedException e) {
            LOGGER.debug("Upload failed: interrupted, " + e.getMessage(), e);
            statusList.add(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                "interrupted: " + e.getMessage(), e));
        }
        if (statusList.size() > 1) {
            final var multiStatus = new MultiStatus(ExplorerActivator.PLUGIN_ID, IStatus.ERROR,
                statusList.toArray(new IStatus[0]), "Could not upload all elements.", null);
            ErrorDialog.openError(Display.getDefault().getActiveShell(), dialogTitle,
                "Some problems occurred during the operation.", multiStatus);
        } else if (statusList.size() == 1) {
            ErrorDialog.openError(Display.getDefault().getActiveShell(), dialogTitle,
                "Some problems occurred during the operation.", statusList.get(0));
        }
        lastUsedLocation = destination;
    }

    /**
     * Opens the selection prompt and lets the user choose a remote workflow group.
     *
     * @param showResetOption if the reset option should be displayed
     * @return An empty if the prompt was cancelled, otherwise the selected target.
     */
    private static Optional<SelectedDestination> promptForTargetLocation(final boolean showResetOption) {
        String[] validMountIDs = getValidTargets().map(c -> c.getMountID()).toArray(String[]::new);
        final var shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();

        final var destinationDialog =
            new DestinationSelectionDialog(shell, validMountIDs, ContentObject.forFile(lastUsedLocation));
        destinationDialog.setShowExcludeDataOption(showResetOption);
        while (destinationDialog.open() == Window.OK) {
            final var destGroup = destinationDialog.getSelectedDestination();
            AbstractExplorerFileInfo destGroupInfo = destGroup.getDestination().fetchInfo();
            if (!destGroupInfo.isWriteable()) {
                boolean chooseNew = MessageDialog.openConfirm(shell, "Not writable",
                    "The selected group is not writable.\n\nChoose a new location.");
                if (chooseNew) {
                    continue;
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of(destGroup);
        }
        return Optional.empty();
    }

    @Override
    public boolean isEnabled() {
        return getSingleSelectedElement().filter(RemoteExplorerFileStore.class::isInstance).isEmpty()
                && getValidTargets().findAny().isPresent();
    }

    private Optional<AbstractExplorerFileStore> getSingleSelectedElement() {
        return super.getSingleSelectedElement(
            fs -> AbstractExplorerFileStore.isWorkflow(fs) || AbstractExplorerFileStore.isWorkflowGroup(fs)
                || AbstractExplorerFileStore.isWorkflowTemplate(fs) || AbstractExplorerFileStore.isDataFile(fs));
    }

    /** @return a stream of content providers that are remote and writable, i.e. server mount points. */
    private static Stream<AbstractContentProvider> getValidTargets() {
        return ExplorerMountTable.getMountedContent().values().stream().filter(c -> c.isRemote() && c.isWritable());
    }
}
