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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.Validator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.explorer.view.DestinationChecker;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.CopyMove.CopyMoveResult;

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
        super(viewer, "Deploy to Server...");
        setImageDescriptor(ICON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return DEPLOY_TO_SERVER_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // first save dirty editors
        if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
            return;
        }
        AbstractExplorerFileStore srcFileStore = getSingleSelectedWorkflowOrGroup()
                .orElseThrow(() -> new IllegalStateException("No single workflow or group selected"));
        String dialogTitle = "Deploy " + srcFileStore.getName();

        Optional<SelectedDestination> destInfoOptional = promptForTargetLocation(srcFileStore);
        if (!destInfoOptional.isPresent()) {
            LOGGER.debug(getText() + "canceled");
            return;
        }
        SelectedDestination destInfo = destInfoOptional.get();
        AbstractExplorerFileStore destination = destInfo.getDestination();

        final DestinationChecker<AbstractExplorerFileStore, AbstractExplorerFileStore> destChecker =
                new DestinationChecker<AbstractExplorerFileStore, AbstractExplorerFileStore>(
                        getParentShell(), "Copy", false, true);
        destChecker.setIsOverwriteEnabled(true);
        destChecker.setIsOverwriteDefault(true);
        destChecker.getAndCheckDestinationFlow(srcFileStore, destination);
        if (destChecker.isAbort()) {
            return;
        }

        CopyMove copyMove = new CopyMove(getView(), destination, destChecker, false);
        copyMove.setExcludeDataInWorkflows(destInfo.isExcludeData());
        final List<IStatus> statusList = new LinkedList<>();
        try {
            // perform the copy/move operations en-bloc in the background
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    CopyMoveResult copyMoveResult = copyMove.run(monitor);
                    statusList.addAll(copyMoveResult.getStatusList());
                }
            });
        } catch (InvocationTargetException e) {
            LOGGER.debug("Invocation exception, " + e.getMessage(), e);
            statusList.add(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                "invocation error: " + e.getMessage(), e));
        } catch (InterruptedException e) {
            LOGGER.debug("Deploy failed: interrupted, " + e.getMessage(), e);
            statusList.add(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID,
                "interrupted: " + e.getMessage(), e));
        }
        if (statusList.size() > 1) {
            IStatus multiStatus = new MultiStatus(ExplorerActivator.PLUGIN_ID, IStatus.ERROR,
                statusList.toArray(new IStatus[0]), "Could not deploy all elements.", null);
            ErrorDialog.openError(Display.getDefault().getActiveShell(), dialogTitle,
                "Some problems occurred during the operation.", multiStatus);
        } else if (statusList.size() == 1) {
            ErrorDialog.openError(Display.getDefault().getActiveShell(), dialogTitle,
                "Some problems occurred during the operation.", statusList.get(0));
        }
        lastUsedLocation = destination;
    }

    /** Opens the selection prompt and lets the user choose a remote workflow group.
     * @return An empty if the prompt was cancelled, otherwise the selected target. */
    Optional<SelectedDestination> promptForTargetLocation(final AbstractExplorerFileStore srcFileStore) {
        String[] validMountIDs = getValidTargets().map(c -> c.getMountID()).toArray(String[]::new);
        Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();

        DestinationSelectionDialog destinationDialog =
                new DestinationSelectionDialog(shell, validMountIDs, ContentObject.forFile(lastUsedLocation));
        while (destinationDialog.open() == Window.OK) {
            SelectedDestination destGroup = destinationDialog.getSelectedDestination();
            AbstractExplorerFileInfo destGroupInfo = destGroup.getDestination().fetchInfo();
            if (!destGroupInfo.isModifiable()) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        if (!getSingleSelectedWorkflowOrGroup().isPresent()) {
            return false;
        }
        return getValidTargets().findAny().isPresent();
    }

    /** @return a stream of content providers that are remote and writable, i.e. server mount points. */
    private static Stream<AbstractContentProvider> getValidTargets() {
        return ExplorerMountTable.getMountedContent().values().stream().filter(c -> c.isRemote() && c.isWritable());
    }


    /** Dialog to select the server + destination folder. Also contains a checkbox whether to exclude the data. */
    static final class DestinationSelectionDialog extends SpaceResourceSelectionDialog {

        private Button m_excludeDataButton;
        private boolean m_isExcludeData;

        /**
         * @param parentShell
         * @param mountIDs
         * @param initialSelection
         */
        public DestinationSelectionDialog(final Shell parentShell, final String[] mountIDs,
            final ContentObject initialSelection) {
            super(parentShell, mountIDs, initialSelection);
            setValidator(new Validator() {
                @Override
                public String validateSelectionValue(final AbstractExplorerFileStore sel, final String currentName) {
                    if (!AbstractExplorerFileStore.isWorkflowGroup(sel)) {
                        return "Select the destination group to which the selected element will be uploaded";
                    }
                    return null;
                }
            });
            setTitle("Destination");
            setHeader("Deploy to...");
            setDescription("Select the destination workflow group.");
        }

        @Override
        protected void createCustomFooterField(final Composite parent) {
            m_excludeDataButton = new Button(parent, SWT.CHECK);
            m_isExcludeData = false;
            m_excludeDataButton.setSelection(m_isExcludeData);
            m_excludeDataButton.setText("Reset Workflow(s) before upload");
            m_excludeDataButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    Button b = (Button)e.widget;
                    m_isExcludeData = b.getSelection();
                }
            });
        }

        SelectedDestination getSelectedDestination() {
            return new SelectedDestination(getSelection(), m_isExcludeData);
        }
    }

    /** Represents a selected destination folder and the property whether the data is to be excluded before upload. */
    static final class SelectedDestination {
        private final AbstractExplorerFileStore m_destination;
        private final boolean m_isExcludeData;

        SelectedDestination(final AbstractExplorerFileStore destination, final boolean isExcludeData) {
            m_destination = CheckUtils.checkArgumentNotNull(destination, "Destination must not be null");
            m_isExcludeData = isExcludeData;
        }

        /** @return the destination, not null. */
        AbstractExplorerFileStore getDestination() {
            return m_destination;
        }

        /** @return the isExcludeData checkbox property. */
        boolean isExcludeData() {
            return m_isExcludeData;
        }


    }

}
