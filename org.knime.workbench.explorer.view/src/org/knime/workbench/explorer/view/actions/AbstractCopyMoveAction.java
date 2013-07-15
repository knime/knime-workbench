/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog.SelectionValidator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.MessageFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.explorer.view.DestinationChecker;
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
    private boolean m_performMove;
    /** The textual representation of the performed command. */
    private final String m_cmd;
    private List<AbstractExplorerFileStore> m_sources;
    private boolean m_success;

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
        this(viewer, menuText, null, null, performMove);
    }

    /**
     * Creates a new copy/move action that copies/moves the source files to
     * the target file store.
     *
     * @param viewer viewer of the space
     * @param menuText the text to be displayed in the menu
     * @param sources the file stores to copy
     * @param target the file store to copy/move the files to
     * @param performMove true to move the files, false to copy them
     */
    public AbstractCopyMoveAction(final ExplorerView viewer,
            final String menuText,
            final List<AbstractExplorerFileStore> sources,
            final AbstractExplorerFileStore target,
            final boolean performMove) {
        super(viewer, menuText);
        m_sources = sources;
        setTarget(target);
        m_performMove = performMove;
        m_cmd = m_performMove ? "Move" : "Copy";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (m_sources == null) {
            // retrieve the selected file stores
            m_sources = removeSelectedChildren(getAllSelectedFiles());
            String message = ExplorerFileSystemUtils.isLockable(m_sources,
                    !m_performMove);
            if (m_sources == null || m_sources.isEmpty()
                    || message != null) {
                MessageBox mb =
                    new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
                String action = m_performMove ? "Move" : "Copy";
                mb.setText("Can't " + action + " All Selected Items");
                mb.setMessage(message);
                mb.open();
                return;
            }
        }

        // sort sources by providers - needed for the checks done later
        HashMap<AbstractContentProvider, List<AbstractExplorerFileStore>> sourceProviders =
            new HashMap<AbstractContentProvider, List<AbstractExplorerFileStore>>();
        for (AbstractExplorerFileStore f : m_sources) {
            AbstractContentProvider acp = f.getContentProvider(); // could be null for local tmp files
            List<AbstractExplorerFileStore> files = sourceProviders.get(acp);
            if (files == null) {
                files = new LinkedList<AbstractExplorerFileStore>();
                sourceProviders.put(acp, files);
            }
            files.add(f);
        }
        if (!isEnabled(sourceProviders)) {
            return;
        }

        // open browse dialog for target selection if necessary
        if (m_target == null) {
            openTargetSelectionDialog();
        }

        if (m_target == null) {
            // user cancelled target selection
            return;
        }

        m_success = copyOrMove(m_sources);
        if (!m_success) {
            LOGGER.debug((m_performMove ? "Moving" : "Copying") + " to \"" + m_target.getFullName() + "\" failed.");
        } else {
            LOGGER.debug("Successfully " + (m_performMove ? "moved " : "copied ") + m_sources.size() + " item(s) to \""
                    + m_target.getFullName() + "\".");
        }
    }

    private void openTargetSelectionDialog() {
        boolean showServer = !isMultipleSelection();
        List<String> mountIDs = new ArrayList<String>();
        for (String mountID : getView().getMountedIds()) {
            AbstractContentProvider acp
            = ExplorerMountTable.getMountPoint(mountID).getProvider();
            /* Add servers only if a single file is selected and if the user
             * can write to it (e.g. is authenticated and the server is not
             * read-only. */
            if ((showServer && acp.isWritable())
                    || !acp.isRemote()) {
                mountIDs.add(mountID);
            }
        }
        ContentObject initialSelection = null;
        if (m_sources.size() == 1) {
            Object selection = ContentDelegator.getTreeObjectFor(
                    m_sources.get(0).getParent());
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
            setTarget(dialog.getSelection());
        }
    }

    /**
     * @return true if the copy/move operation was successful, false otherwise
     */
    public boolean isSuccessful() {
        return m_success;
    }

    /**
     * @param srcFileStores the file stores to copy/move
     * @return true if the operation was successful, false otherwise
     */
    protected boolean copyOrMove(final List<AbstractExplorerFileStore>
            srcFileStores) {
        /* Make sure that the target is not a child of any item in the
         * selection. */
        if (isChildOf(m_target,
                new LinkedHashSet<AbstractExplorerFileStore>(srcFileStores))) {
            String msg = "Cannot " + m_cmd + " the selected files into "
            + m_target + " because it is a child of the selection.";
            MessageDialog.openError(Display.getDefault()
                    .getActiveShell(), m_cmd + " Workflow", msg);
            LOGGER.info(msg);
            return false;
        }

        // collect the necessary information
        final DestinationChecker <AbstractExplorerFileStore,
                AbstractExplorerFileStore> destChecker = new DestinationChecker
                <AbstractExplorerFileStore, AbstractExplorerFileStore>(
                getParentShell(), m_cmd, srcFileStores.size() > 1,
                !m_performMove);
        for (final AbstractExplorerFileStore srcFS : srcFileStores) {
            final AbstractExplorerFileStore destFS =
                    destChecker.getAndCheckDestinationFlow(srcFS, m_target);
            if (destChecker.isAbort()) {
                LOGGER.info(m_cmd + " operation was aborted.");
                return false;
            }
            if (destFS == null) {
                // the user skipped the operation or it is not allowed
                LOGGER.info(m_cmd + " operation of "
                        + srcFS.getMountIDWithFullPath() + " was skipped.");
            }
        }

        /* Check for unlockable local destinations. Unfortunately this cannot
         * be done in the copy loop below as this runs in a non SWT-thread
         * but needs access to the workbench pages. */
        final Set<LocalExplorerFileStore> notOverwritableDest = new HashSet<LocalExplorerFileStore>();
        final List<LocalExplorerFileStore> lockedDest = new ArrayList<LocalExplorerFileStore>();
        HashMap<AbstractContentProvider, List<AbstractExplorerFileStore>> overWrittenFlows =
            new HashMap<AbstractContentProvider, List<AbstractExplorerFileStore>>();
        for (AbstractExplorerFileStore aefs : destChecker.getOverwriteFS()) {
            AbstractExplorerFileInfo info = aefs.fetchInfo();
            if (!info.isWorkflow()) {
                continue;
            }
            if (aefs instanceof LocalExplorerFileStore) {
                LocalExplorerFileStore lfs = (LocalExplorerFileStore)aefs;
                if (ExplorerFileSystemUtils.lockWorkflow(lfs)) {
                    lockedDest.add(lfs);
                    /* Flows opened in an editor cannot be overwritten as
                        well. */
                    if (ExplorerFileSystemUtils.hasOpenWorkflows(Arrays.asList(lfs))) {
                        notOverwritableDest.add(lfs);
                    }
                } else {
                    notOverwritableDest.add(lfs);
                }
            }
            // collect all overwritten flows for each content provider
            AbstractContentProvider acp = aefs.getContentProvider();
            List<AbstractExplorerFileStore> flowList = overWrittenFlows.get(acp);
            if (flowList == null) {
                flowList = new LinkedList<AbstractExplorerFileStore>();
                overWrittenFlows.put(acp, flowList);
            }
            flowList.add(aefs);
        }
        // confirm overwrite with each content provider (server is currently only one that pops up a dialog)
        for (AbstractContentProvider prov : overWrittenFlows.keySet()) {
            // TODO: how can we avoid that multiple confirm dialogs pop up?
            // Becomes only an issue of the server is not the only one popping up a dialog.
            AtomicBoolean confirm = prov.confirmOverwrite(getParentShell(), overWrittenFlows.get(prov));
            if (confirm != null && !confirm.get()) {
                LOGGER.info("User canceled overwrite in " + prov);
                return false;
            }
        }
        // confirm move (deletion of flows in source location)
        if (m_performMove) {
            HashMap<AbstractContentProvider, List<AbstractExplorerFileStore>> movedFlows =
                    new HashMap<AbstractContentProvider, List<AbstractExplorerFileStore>>();
            Map<AbstractExplorerFileStore, AbstractExplorerFileStore> srcToDest = destChecker.getMappings();
            for (AbstractExplorerFileStore aefs : srcFileStores) {
                if (srcToDest.get(aefs) == null) {
                    // user chose to skip this file during move/copy
                    continue;
                }
                AbstractContentProvider cp = aefs.getContentProvider();
                List<AbstractExplorerFileStore> list = movedFlows.get(cp);
                if (list == null) {
                    list = new LinkedList<AbstractExplorerFileStore>();
                    movedFlows.put(cp, list);
                }
                list.add(aefs);
            }
            for (AbstractContentProvider acp : movedFlows.keySet()) {
                AtomicBoolean confirmed = acp.confirmMove(getParentShell(), movedFlows.get(acp));
                if (confirmed != null && !confirmed.get()) {
                    LOGGER.info("User canceled move (flow del in source) in " + acp);
                    return false;
                }
            }
        }

        final Collection<AbstractExplorerFileStore> processedTargets
                = new HashSet<AbstractExplorerFileStore>(
                        destChecker.getMappings().values());
        final List<IStatus> result = new LinkedList<IStatus>();
        final AtomicBoolean success = new AtomicBoolean(true);
        try {
            // perform the copy/move operations en-bloc in the background
            PlatformUI.getWorkbench().getProgressService()
                    .busyCursorWhile(new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException,
                        InterruptedException {

                    int numFiles = srcFileStores.size();
                    monitor.beginTask(m_cmd + " " + numFiles
                            + " files to " + m_target.getFullName(),
                            numFiles);

                    for (final Map.Entry<AbstractExplorerFileStore,
                            AbstractExplorerFileStore> entry
                            : destChecker.getMappings().entrySet()) {
                        AbstractExplorerFileStore srcFS = entry.getKey();
                        AbstractExplorerFileStore destFS = entry.getValue();
                        if (destFS == null) {
                            // skip operations that have been marked to
                            // be skipped
                            continue;
                        }
                        String operation = m_cmd + " "
                                + srcFS.getMountIDWithFullPath()
                                + " to " + destFS.getFullName();
                        monitor.subTask(operation);
                        LOGGER.debug(operation);
                        try {
                            if (notOverwritableDest.contains(destFS)) {
                                throw new UnsupportedOperationException(
                                        "Cannot override \""
                                        + destFS.getFullName()
                                        + "\". Probably it is opened in the"
                                        + " editor or it is in use by another "
                                        + "user.");
                            }
                            boolean isOverwritten = destChecker.getOverwriteFS()
                                    .contains(destFS);
                            int options = isOverwritten ?
                                            EFS.OVERWRITE : EFS.NONE;

                            /* Make sure that a workflow group is not
                             * overwritten by a workflow or template and vice
                             * versa. */
                            if (isOverwritten && (
                                    srcFS.fetchInfo().isWorkflowGroup()
                                    != destFS.fetchInfo().isWorkflowGroup())) {
                                String msg = null;
                                if (srcFS.fetchInfo().isWorkflowGroup()) {
                                    msg = "Cannot override \""
                                        + destFS.getFullName()
                                        + "\". Workflows and MetaNode Templates"
                                        + " cannot be overwritten by a Workflow"
                                        + " Group.";
                                } else {
                                    msg = "Cannot override \""
                                        + destFS.getFullName()
                                        + "\". Workflow Groups can only be "
                                        + "overwritten by other Workflow"
                                        + " Groups.";
                                }
                                throw new UnsupportedOperationException(msg);
                            }


                            boolean isSrcRemote
                                = srcFS instanceof RemoteExplorerFileStore;
                            boolean isDstRemote
                                = destFS instanceof RemoteExplorerFileStore;
                            if (srcFS.fetchInfo().isWorkflowTemplate()
                                    && !destFS.getContentProvider()
                                            .canHostMetaNodeTemplates()) {
                                throw new UnsupportedOperationException(
                                        "Cannot " + m_cmd
                                        + " Meta Node template '"
                                        + srcFS.getFullName() + "' to "
                                        + destFS.getMountID() + "."
                                        + ". Unsupported operation.");
                            }

                            if (!isSrcRemote && isDstRemote) { // upload
                                destFS.getContentProvider().performUpload(
                                        (LocalExplorerFileStore)srcFS,
                                        (RemoteExplorerFileStore)destFS,
                                        monitor);
                                if (m_performMove) {
                                    srcFS.delete(options, monitor);
                                }
                            } else if (isSrcRemote && !isDstRemote) {
                                // download
                                destFS.getContentProvider().performDownload(
                                        (RemoteExplorerFileStore)srcFS,
                                        (LocalExplorerFileStore)destFS,
                                        monitor);
                                if (m_performMove) {
                                    srcFS.delete(options, monitor);
                                }
                            } else { // regular copy
                                if (m_performMove) {
                                    srcFS.move(destFS, options, monitor);
                                } else {
                                    srcFS.copy(destFS, options, monitor);
                                }
                            }
                        } catch (CoreException e) {
                            LOGGER.debug(m_cmd + " failed: "
                                    + e.getStatus().getMessage(), e);
                            result.add(e.getStatus());
                            success.set(false);
                            processedTargets.remove(destFS);
                        } catch (UnsupportedOperationException e) {
                            // illegal operation
                            LOGGER.debug(m_cmd + " failed: "
                                    + e.getMessage());
                            result.add(new Status(IStatus.WARNING,
                                    ExplorerActivator.PLUGIN_ID,
                                    e.getMessage()));
                            success.set(true);
                            processedTargets.remove(destFS);
                        } catch (Exception e) {
                            LOGGER.debug(m_cmd + " failed: "
                                    + e.getMessage(), e);
                            result.add(new Status(IStatus.ERROR,
                                    ExplorerActivator.PLUGIN_ID,
                                    e.getMessage(), e));
                            success.set(false);
                            processedTargets.remove(destFS);
                        }
                        monitor.worked(1);
                    }
                }
            });
            getView().setNextSelection(processedTargets);
            m_target.refresh();
        } catch (InvocationTargetException e) {
            LOGGER.debug("Invocation exception, " + e.getMessage(), e);
            result.add(new Status(IStatus.ERROR,
                    ExplorerActivator.PLUGIN_ID,
                    "invocation error: " + e.getMessage(), e));
            success.set(false);
        } catch (InterruptedException e) {
            LOGGER.debug(m_cmd + " failed: interrupted, " + e.getMessage(),
                    e);
            result.add(new Status(IStatus.ERROR,
                    ExplorerActivator.PLUGIN_ID,
                    "interrupted: " + e.getMessage(), e));
            success.set(false);
        } finally {
            // unlock all locked destinations
            ExplorerFileSystemUtils.unlockWorkflows(lockedDest);
        }

        if (result.size() > 1) {
            IStatus multiStatus = new MultiStatus(ExplorerActivator.PLUGIN_ID,
                    IStatus.ERROR, result.toArray(new IStatus[0]),
                    "Could not " + m_cmd + " all files.", null);
            ErrorDialog.openError(Display.getDefault().getActiveShell(),
                    m_cmd + " item",
                    "Some problems occurred during the operation.",
                    multiStatus);
            /* Don't show it as failure if only some of the items could not be
             * copied. */
            success.set(true);
        } else if (result.size() == 1) {
            ErrorDialog.openError(Display.getDefault().getActiveShell(),
                    m_cmd + " item",
                    "Some problems occurred during the operation.",
                    result.get(0));
        }
        return success.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        // checks whether copy/move is possible based on the current selection
        return isEnabled(getSelectedFiles());
    }

    private boolean isEnabled(final Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selectedProviders) {
        return m_target.fetchInfo().isModifiable() && isCopyOrMovePossible(selectedProviders, m_performMove);
    }

    /**
     * Determines if a copy/move operation is possible based on the selection.
     *
     * @param selProviders the selected content providers
     * @param performMove true if a move operation should be checked
     * @return true if the operation is possible, false otherwise
     */
    public static boolean isCopyOrMovePossible(
            final Map<AbstractContentProvider, List<AbstractExplorerFileStore>>
            selProviders, final boolean performMove) {
        if (selProviders.size() != 1) {
            // can only copy/move from one source content provider
            return false;
        }
        AbstractContentProvider acp = selProviders.keySet().iterator().next();
        if (acp != null && !acp.isWritable() && performMove) {
            return false;
        }
        List<AbstractExplorerFileStore> selections =
                selProviders.values().iterator().next();
        if (selections == null || selections.isEmpty()) {
            return false;
        }
        AbstractExplorerFileStore fileStore = selections.get(0);
        if (fileStore instanceof MessageFileStore) {
            return false;
        }
        if (fileStore instanceof RemoteExplorerFileStore) {
            // currently we can only download one workflow or metanode template
            if (selections.size() > 1) {
                return false;
            }
        }
        if (performMove) {
            return fileStore.canMove();
        } else {
            return fileStore.canCopy();
        }
    }

    protected boolean isPerformMove() {
        return m_performMove;
    }

    protected AbstractExplorerFileStore getTarget() {
        return m_target;
    }

    protected void setTarget(final AbstractExplorerFileStore target) {
        if (target == null || target.fetchInfo().isWorkflowGroup()) {
            m_target = target;
        } else {
            m_target = target.getParent();
        }
    }

    protected void setPerformMove(final boolean performMove) {
        m_performMove = performMove;
    }

    protected void setSuccess(final boolean success) {
        m_success = success;
    }
 }
