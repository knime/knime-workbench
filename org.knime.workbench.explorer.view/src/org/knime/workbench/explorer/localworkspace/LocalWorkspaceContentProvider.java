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
 */
package org.knime.workbench.explorer.localworkspace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.KnimeFileUtil;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.AbstractCopyMoveAction;
import org.knime.workbench.explorer.view.actions.GlobalCopyAction;
import org.knime.workbench.explorer.view.actions.GlobalMoveAction;
import org.knime.workbench.explorer.view.actions.LocalDownloadWorkflowAction;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;

/**
 * Provides content for the user space view that shows the content (workflows
 * and workflow groups) of the local workspace of the workbench.
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceContentProvider extends AbstractContentProvider {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LocalWorkspaceContentProvider.class);

    private IResourceChangeListener m_workspaceResourceListener;

    /**
     * @param factory the factory that created us.
     * @param id mount id
     */
    LocalWorkspaceContentProvider(
            final LocalWorkspaceContentProviderFactory factory,
            final String id) {
        super(factory, id);
        registerListeners();
    }

    /**
     *  */
    private void registerListeners() {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        m_workspaceResourceListener = new IResourceChangeListener() {

            @Override
            public void resourceChanged(final IResourceChangeEvent event) {
                onResourceChanged(event);
            }
        };
        workspace.addResourceChangeListener(m_workspaceResourceListener,
                IResourceChangeEvent.POST_CHANGE);
    }

    private void onResourceChanged(final IResourceChangeEvent event) {
        if (getListeners().length == 0) { // no view registered, nothing to do
             return;
        }
        final IResourceDelta delta = event.getDelta();
        final List<IResource> refreshList = new ArrayList<IResource>();

        try {
            delta.accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(final IResourceDelta delta) {
                    IResource res = delta.getResource();
                    IResource refreshCandidate = res.getParent();
                    switch (delta.getKind()) {
                    case IResourceDelta.ADDED:
                    case IResourceDelta.REMOVED:
                        boolean isChild = false;
                        Iterator<IResource> iter = refreshList.iterator();
                        while (iter.hasNext()) {
                            IResource current = iter.next();
                            if (isChildOfOrSame(refreshCandidate, current)) {
                                isChild = true;
                                break;
                            }
                            if (isChildOfOrSame(current, refreshCandidate)) {
                                iter.remove();
                            }
                        }
                        if (!isChild) {
                            refreshList.add(refreshCandidate);
                            return false;
                        }
                        break;
                    default:
                    }
                    return true;
                }
            });
        } catch (CoreException e) {
            LOGGER.error("Failed to process resource events", e);
        }
        for (IResource r : refreshList) {
            final String path = r.getFullPath().toString();
            LOGGER.debug("Refreshing \"" + path + "\"");
            refresh(getFileStore(path));
        }
    }


    private static boolean isChildOfOrSame(
            final IResource candidate, final IResource parent) {
        if (candidate.equals(parent)) {
            return true;
        }
        IResource candidateParent = candidate.getParent();
        if (candidateParent == null) {
            return false;
        }
        return isChildOfOrSame(candidateParent, parent);
    }

    /*
     * ------------ Content Provider Methods --------------------
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
        // we don't care
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore[] getChildren(final Object parentElement) {
        if (!(parentElement instanceof LocalWorkspaceFileStore)) {
            return NO_CHILD;
        }
        LocalExplorerFileStore parent = (LocalExplorerFileStore)parentElement;

        if (AbstractExplorerFileStore.isNode(parent)) {
            return NO_CHILD;
        }
        if (AbstractExplorerFileStore.isWorkflow(parent)) {
            return AbstractContentProvider.getWorkflowChildren(parent);
        }
        if (AbstractExplorerFileStore.isWorkflowGroup(parent)) {
            return getWorkflowgroupChildren(parent);
        }
        // everything else: return dirs only
        try {
            AbstractExplorerFileStore[] children
                    = parent.childStores(EFS.NONE, null);
            ArrayList<AbstractExplorerFileStore> result =
                    new ArrayList<AbstractExplorerFileStore>();
            for (AbstractExplorerFileStore c : children) {
                if (AbstractExplorerFileStore.isWorkflow(c)) {
                    result.add(c);
                    continue;
                }
                if (AbstractExplorerFileStore.isWorkflowGroup(
                        c)) {
                    result.add(c);
                    continue;
                }
            }
            return result.toArray(new AbstractExplorerFileStore[result.size()]);
        } catch (CoreException e) {
            return NO_CHILD;
        }
    }

    /**
     * Returns children of a workflowgroup.
     *
     * @param workflowGroup the workflow group to return the children for
     * @return the content of the workflow group
     * @since 3.0
     */
    public static AbstractExplorerFileStore[] getWorkflowgroupChildren(
            final LocalExplorerFileStore workflowGroup) {

        assert AbstractExplorerFileStore.isWorkflowGroup(workflowGroup);

        try {
            AbstractExplorerFileStore[] childs =
                workflowGroup.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return AbstractContentProvider.NO_CHILD;
            }
            ArrayList<AbstractExplorerFileStore> result =
                    new ArrayList<AbstractExplorerFileStore>();
            for (AbstractExplorerFileStore c : childs) {
                if (AbstractExplorerFileStore.isWorkflowGroup(c)
                        || AbstractExplorerFileStore.isWorkflow(c)) {
                    result.add(c);
                }
            }
            return result.toArray(new AbstractExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return AbstractContentProvider.NO_CHILD;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore getParent(final Object element) {
        if (!(element instanceof LocalWorkspaceFileStore)) {
            return null;
        }
        LocalExplorerFileStore e = (LocalExplorerFileStore)element;
        return e.getParent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        return getChildren(element).length > 0;
    }

    /*
     * ------------ Label Provider Methods --------------------
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        if (!(element instanceof LocalWorkspaceFileStore)) {
            return ImageRepository.getImage(SharedImages.WorkflowUnknownRed);
        }
        LocalExplorerFileStore e = (LocalExplorerFileStore)element;
        Image img = getWorkspaceImage(e);
        if (img != null) {
            return img;
        } else {
            return ImageRepository.getImage(SharedImages.WorkflowUnknownRed);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {
        if (!(element instanceof LocalWorkspaceFileStore)) {
            return element.toString();
        }
        LocalExplorerFileStore f = (LocalExplorerFileStore)element;
        return f.fetchInfo().getName();
    }

    /*
     * -------------------------------------------------------
     */

    /*
     * ------------- view context menu -----------------------------
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public void addContextMenuActions(
            final ExplorerView viewer,
            final org.eclipse.jface.action.IMenuManager manager,
            final Set<String> visibleIDs,
            final Map<AbstractContentProvider,
            List<AbstractExplorerFileStore>> selection) {
        // nothing to add so far
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage() {
        return ImageRepository.getImage(SharedImages.LocalSpaceIcon);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String saveState() {
        return ""; // nothing to save here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Local Workspace";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.removeResourceChangeListener(m_workspaceResourceListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore getFileStore(final String fullPath) {
        return new LocalWorkspaceFileStore(getMountID(), fullPath);
    }

    /** {@inheritDoc} */
    @Override
    public LocalExplorerFileStore fromLocalFile(final File file) {
        IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        File root = rootPath.toFile();
        String s = getRelativePath(file, root);
        if (s == null) { // file not present in local workspace
            return null;
        }
        return new LocalWorkspaceFileStore(getMountID(), s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final AbstractExplorerFileStore target,
            final int operation, final TransferData transferType) {
        if (!(DND.DROP_COPY == operation || DND.DROP_MOVE == operation)) {
            return false;
        }
        if (!target.fetchInfo().isModifiable()) {
            return false;
        }
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection != null && selection instanceof IStructuredSelection) {
            List<AbstractExplorerFileStore> fileStores =
                    DragAndDropUtils.getExplorerFileStores(
                            (IStructuredSelection)selection);
            if (fileStores == null) {
                return false;
            }
            for (AbstractExplorerFileStore fs : fileStores) {
                if (!(AbstractExplorerFileStore.isWorkflow(fs)
                        || AbstractExplorerFileStore.isWorkflowGroup(fs))) {
                    return false;
                }
                if (!fs.fetchInfo().isReadable()) {
                    return false;
                }
            }
        } else if (!FileTransfer.getInstance().isSupportedType(transferType)) {
            return false;
        }
        boolean valid =
                !(AbstractExplorerFileStore.isNode(target)
                        || AbstractExplorerFileStore.isWorkflow(target)
                        || AbstractExplorerFileStore.isMetaNode(target));
        return valid;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean canHostMetaNodeTemplates() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canHostDataFiles() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final ExplorerView view, final Object data,
            final AbstractExplorerFileStore target, final int operation) {
        // read current selection and check whether it comes from the very same
        // view - disregard selection from other views (not ExploreFileStore)
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            List<AbstractExplorerFileStore> fileStores =
                    DragAndDropUtils.getExplorerFileStores(ss);
            boolean performMove = DND.DROP_MOVE == operation;
            String msg = checkOpenEditors(fileStores, performMove);
            if (msg != null) {
                LOGGER.warn(msg);
                return false;
            }
            return copyOrMove(view, fileStores, target, performMove);
        } else if (data instanceof String[]) { // we have a file transfer
            String[] files = (String[])data;
            try {
                File targetDir = target.toLocalFile(EFS.NONE, null);
                for (String path : files) {
                    File src = new File(path);
                    boolean isWorkflowOrGroup =
                            KnimeFileUtil.isWorkflow(src)
                                    || KnimeFileUtil.isWorkflowGroup(src);
                    if (!isWorkflowOrGroup) {
                        LOGGER.warn("Only workflows or workflow groups can be"
                                + " copied into the local workspace. Aborting "
                                + "operation.");
                        return false;
                    }
                }
                for (String path : files) {
                    File src = new File(path);
                    if (src.exists()
                            && !targetDir.equals(src.getParentFile())) {
                        File dir = new File(targetDir, src.getName());
                        FileUtils.copyDirectory(src, dir);
                        LOGGER.debug("Copied directory "
                                + src.getAbsolutePath() + " to directory "
                                + dir.getAbsolutePath() + ".");
                    }
                }
                return true;
            } catch (IOException e) {
                LOGGER.error("An error occurred while copying files to the User"
                        + " Space.", e);
            } catch (CoreException e) {
                LOGGER.error(
                        "Could not get local file for item "
                                + target.getFullName() + ".", e);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean copyOrMove(final ExplorerView view,
            final List<AbstractExplorerFileStore> fileStores,
            final AbstractExplorerFileStore targetDir,
            final boolean performMove) {
        if (!targetDir.fetchInfo().isDirectory()) {
            throw new IllegalArgumentException("Destination \""
                    + targetDir.getFullName() +  "\" is no directory.");
        }
        if (!(targetDir instanceof LocalExplorerFileStore)) {
            throw new IllegalArgumentException("Target file store \""
                    + targetDir.getMountIDWithFullPath() + "\" is no "
                    + "LocalExplorerFileStore.");
        }
        AbstractCopyMoveAction action;
        if (performMove) {
            action = new GlobalMoveAction(view, fileStores,
                    targetDir);
        } else {
            action = new GlobalCopyAction(view, fileStores,
                    targetDir);
        }
        action.run();
        return action.isSuccessful();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performDownload(final RemoteExplorerFileStore source,
            final LocalExplorerFileStore target, final IProgressMonitor monitor)
            throws CoreException {
        File parentDir = target.toLocalFile();
        LocalDownloadWorkflowAction downloadAction =
            new LocalDownloadWorkflowAction(source, parentDir);
        downloadAction.runSync(monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performUpload(final LocalExplorerFileStore source,
            final RemoteExplorerFileStore target,
            final IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Cannot upload files to a local"
                + " content provider.");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dragStart(final List<AbstractExplorerFileStore> fileStores) {
        if (fileStores == null || fileStores.isEmpty()) {
            return false;
        }
        for (AbstractExplorerFileStore fs : fileStores) {
            if (DragAndDropUtils.isLinkedProject(fs)) {
                LOGGER.warn("Linked workflow project cannot be copied from the"
                        + " User Space.");
                return false;
            }
        }
        String msg = ExplorerFileSystemUtils.isLockable(fileStores, true);
        if (msg != null) {
            MessageBox mb =
                    new MessageBox(Display.getCurrent().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setText("Dragging canceled");
            mb.setMessage(msg);
            mb.open();
            return false;
        } else {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemote() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWritable() {
        return true;
    }
}
