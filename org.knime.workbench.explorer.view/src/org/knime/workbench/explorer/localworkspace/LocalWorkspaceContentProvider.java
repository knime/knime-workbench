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
package org.knime.workbench.explorer.localworkspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResource;
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
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.filesystem.TmpLocalExplorerFile;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.AbstractCopyMoveAction;
import org.knime.workbench.explorer.view.actions.GlobalCopyAction;
import org.knime.workbench.explorer.view.actions.GlobalMoveAction;
import org.knime.workbench.explorer.view.actions.WorkflowDownload;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportAction;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;

/**
 * Provides content for the user space view that shows the content (workflows and workflow groups) of the local
 * workspace of the workbench.
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceContentProvider extends AbstractContentProvider {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LocalWorkspaceContentProvider.class);

    /**
     * @param factory the factory that created us.
     * @param id mount id
     */
    LocalWorkspaceContentProvider(final LocalWorkspaceContentProviderFactory factory, final String id) {
        super(factory, id);
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
        if (AbstractExplorerFileStore.isWorkflowTemplate(parent)) {
            return getWorkflowTemplateChildren(parent);
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
                if (AbstractExplorerFileStore.isWorkflowGroup(c)) {
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
                        || AbstractExplorerFileStore.isWorkflow(c)
                        || AbstractExplorerFileStore.isWorkflowTemplate(c)
                        || AbstractExplorerFileStore.isDataFile(c)) {
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
            return ImageRepository.getIconImage(SharedImages.WorkflowUnknownRed);
        }
        LocalExplorerFileStore e = (LocalExplorerFileStore)element;
        Image img = getWorkspaceImage(e);
        if (img != null) {
            return img;
        } else {
            return ImageRepository.getIconImage(SharedImages.WorkflowUnknownRed);
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
        return ImageRepository.getIconImage(SharedImages.LocalSpaceIcon);
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
                        || AbstractExplorerFileStore.isWorkflowGroup(fs)
                        || AbstractExplorerFileStore.isWorkflowTemplate(fs)
                        || AbstractExplorerFileStore.isDataFile(fs))) {
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
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean canHostDataFiles() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsSnapshots() {
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
            List<AbstractExplorerFileStore> dropSource = new ArrayList<AbstractExplorerFileStore>();
            try {
                for (String path : files) {
                    File src = new File(path);
                    if (!src.isFile()) {
                        LOGGER.error("Only files can be dropped. " + path + " doesn't denote a file.");
                        return false;
                    }
                }
                for (String path : files) {
                    File src = new File(path);
                    if (src.exists() && !target.toLocalFile(EFS.NONE, null).equals(src.getParentFile())) {
                        if (path.endsWith("." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION)
                            || path.endsWith("." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION)) {
                            new WorkflowImportAction(view, target, path).run();
                        } else {
                            dropSource.add(new TmpLocalExplorerFile(new File(path)));
                        }
                    }
                }
                if (!dropSource.isEmpty()) {
                    return copyOrMove(view, dropSource, target, DND.DROP_MOVE == operation);
                } else {
                    return true;
                }
            } catch (CoreException e) {
                LOGGER.error("Could not get local file for item " + target.getFullName() + ".", e);
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
    public void performDownloadAsync(final RemoteExplorerFileStore source, final LocalExplorerFileStore target,
        final boolean deleteSource, final AfterRunCallback afterRunCallback) {
        WorkflowDownload downloadAction =
                new WorkflowDownload(source, target, deleteSource, afterRunCallback);
        downloadAction.schedule();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performUploadAsync(final LocalExplorerFileStore source, final RemoteExplorerFileStore target,
        final boolean deleteSource, final boolean excludeDataInWorkflows, final AfterRunCallback callback) throws CoreException {
        throw new UnsupportedOperationException("Cannot upload files to a local content provider.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dragStart(final List<AbstractExplorerFileStore> fileStores) {
        if (fileStores == null || fileStores.isEmpty()) {
            return false;
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
