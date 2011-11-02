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
 */
package org.knime.workbench.explorer.localworkspace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.util.KnimeFileUtil;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.IconFactory;
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


    /**
     * @param factory the factory that created us.
     * @param id mount id
     */
    LocalWorkspaceContentProvider(
            final LocalWorkspaceContentProviderFactory factory,
            final String id) {
        super(factory, id);
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
        LocalWorkspaceFileStore parent = (LocalWorkspaceFileStore)parentElement;

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
     */
    public static AbstractExplorerFileStore[] getWorkflowgroupChildren(
            final LocalWorkspaceFileStore workflowGroup) {

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
        LocalWorkspaceFileStore e = (LocalWorkspaceFileStore)element;
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
            return IconFactory.instance.unknownRed();
        }
        LocalWorkspaceFileStore e = (LocalWorkspaceFileStore)element;
        Image img = getWorkspaceImage(e);
        if (img != null) {
            return img;
        } else {
            return IconFactory.instance.unknownRed();
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
        LocalWorkspaceFileStore f = (LocalWorkspaceFileStore)element;
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
            final org.eclipse.jface.viewers.TreeViewer viewer,
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
        return IconFactory.instance.localWorkspace();
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
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection != null && selection instanceof IStructuredSelection) {
            List<AbstractExplorerFileStore> fileStores =
                    DragAndDropUtils.getExplorerFileStores(
                            (IStructuredSelection)selection);
            for (AbstractExplorerFileStore fs : fileStores) {
                if (!(AbstractExplorerFileStore.isWorkflow(fs)
                        || AbstractExplorerFileStore.isWorkflowGroup(fs))) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data,
            final AbstractExplorerFileStore target, final int operation) {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            List<AbstractExplorerFileStore> fileStores =
                    DragAndDropUtils.getExplorerFileStores(ss);

            return copyOrMove(fileStores, target, DND.DROP_MOVE == operation);
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
                                + " copied into the User Space. Aborting "
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
                LOGGER.error("An error occured while copying files to the User"
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
     * @param fileStores
     * @param target
     * @param operation
     * @return
     */
    public boolean copyOrMove(final List<AbstractExplorerFileStore> fileStores,
            final AbstractExplorerFileStore target, final boolean performMove) {
        // check for existing files
        for (AbstractExplorerFileStore fs : fileStores) {
            String childName = fs.getName();
            //TODO: What if two selected stores have the same name?!?
            if (target.getChild(childName).fetchInfo().exists()) {
                MessageBox mb =
                    new MessageBox(Display.getCurrent().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
                mb.setText("Operation Cancelled");
                mb.setMessage("A resource with the name \"" + childName
                        +  "\" already exists in \"" + target.getFullName()
                        + "\". Cancelling operation...");
                mb.open();
                return false;
            }
        }

        for (AbstractExplorerFileStore fs : fileStores) {
            try {
                if (fs instanceof RemoteExplorerFileStore) {
                    RemoteExplorerFileStore rfs = (RemoteExplorerFileStore)fs;
                    if (!rfs.fetchInfo().isWorkflow()) {
                        LOGGER.error("Can only download workflows. "
                                + rfs.getMountIDWithFullPath() + " is no workflow.");
                        return false;
                    }
                    if (performDownload(rfs, (LocalExplorerFileStore)target)) {
                        if (performMove) {
                            rfs.delete(EFS.NONE, null);
                        }
                    } else {
                        return false;
                    }
                } else {
                    if (performMove) {
                        move(fs, target);
                    } else {
                        copy(fs, target);
                    }
                    DragAndDropUtils.refreshResource(target);
                }
            } catch (CoreException e) {
                String msg = "An error occured when transfering the file \""
                        + fs.getFullName() + "\". ";
                LOGGER.error(msg, e);
                MessageBox mb =
                    new MessageBox(Display.getCurrent().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
                mb.setText("An error occured during transfer");
                mb.setMessage(msg);
                mb.open();
                return false;
            }
        }
        return true;
    }

    /**
     * @param src the explorer file store to copy
     * @param the destination file store
     * @throws CoreException
     */
    private void copy(final AbstractExplorerFileStore src,
            final AbstractExplorerFileStore dest) throws CoreException {
        src.copy(dest, EFS.NONE, null);
    }

    /**
     * @param src the explorer file store to copy
     * @param the destination file store
     * @throws CoreException
     */
    private void move(final AbstractExplorerFileStore src,
            final AbstractExplorerFileStore dest) throws CoreException {
        src.move(dest, EFS.NONE, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dragStart(final List<AbstractExplorerFileStore> fileStores) {
        for (AbstractExplorerFileStore fs : fileStores) {
            if (DragAndDropUtils.isLinkedProject(fs)) {
                LOGGER.warn("Linked workflow project cannot be copied from the"
                        + " User Space.");
                return false;
            }
        }
        return true;
    }

    private boolean performDownload(final RemoteExplorerFileStore source,
            final LocalExplorerFileStore parent) {
        File parentDir;
        try {
            parentDir = parent.toLocalFile();
        } catch (CoreException e) {
            LOGGER.error("Could not get local target directory for download.",
                    e);
            return false;
        }
        LocalDownloadWorkflowAction downloadAction =
                new LocalDownloadWorkflowAction(source, parentDir);
        downloadAction.run();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemote() {
        return false;
    }

}
