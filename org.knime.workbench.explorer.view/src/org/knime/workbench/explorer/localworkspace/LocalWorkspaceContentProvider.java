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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.nature.KNIMEProjectNature;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * Provides content for the user space view that shows the content (workflows
 * and workflow groups) of the local workspace of the workbench.
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceContentProvider extends AbstractContentProvider {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            LocalWorkspaceContentProvider.class);
    private static final Image LOCAL_WS_IMG = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_default.png").createImage();

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
    public ExplorerFileStore[] getElements(final Object inputElement) {
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
    public ExplorerFileStore[] getChildren(final Object parentElement) {
        if (!(parentElement instanceof LocalWorkspaceFileStore)) {
            return NO_CHILD;
        }
        LocalWorkspaceFileStore parent = (LocalWorkspaceFileStore)parentElement;

        if (ExplorerFileStore.isNode(parent)) {
            return NO_CHILD;
        }
        if (ExplorerFileStore.isWorkflow(parent)) {
            return AbstractContentProvider.getWorkflowChildren(parent);
        }
        if (ExplorerFileStore.isWorkflowGroup(parent)) {
            return AbstractContentProvider.getWorkflowgroupChildren(parent);
        }
        // everything else: return dirs only
        try {
            IFileStore[] children = parent.childStores(EFS.NONE, null);
            ArrayList<ExplorerFileStore> result =
                    new ArrayList<ExplorerFileStore>();
            for (IFileStore c : children) {
                if (ExplorerFileStore.isWorkflow(c)) {
                    result.add((ExplorerFileStore)c);
                    continue;
                }
                if (ExplorerFileStore.isWorkflowGroup((ExplorerFileStore)c)) {
                    result.add((ExplorerFileStore)c);
                    continue;
                }
            }
            return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException e) {
            return NO_CHILD;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore getParent(final Object element) {
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
            return ICONS.unknownRed();
        }
        LocalWorkspaceFileStore e = (LocalWorkspaceFileStore)element;
        Image img = getWorkspaceImage(e);
        if (img != null) {
            return img;
        } else {
            return ICONS.unknownRed();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage() {
        return LOCAL_WS_IMG;
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
    public ExplorerFileStore getFileStore(final String fullPath) {
        return new LocalWorkspaceFileStore(getMountID(), fullPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final ExplorerFileStore target,
            final int operation, final TransferData transferType) {
        if (!(DND.DROP_COPY == operation || DND.DROP_MOVE == operation)) {
            return false;
        }
        return !(ExplorerFileStore.isNode(target)
                || ExplorerFileStore.isWorkflow(target)
                || ExplorerFileStore.isMetaNode(target));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data,
            final ExplorerFileStore target, final int operation) {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        ISelection selection = transfer.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            List<ExplorerFileStore> fileStores
                    = DragAndDropUtils.getExplorerFileStores(ss);
            for (ExplorerFileStore fs : fileStores) {
                /* On the drop receiver side there is no difference between copy
                 * and move. The removal of the src object has to be done by the
                 * drag source. */
                try {
//                    if (DND.DROP_COPY == operation) {
                        copy(fs, target);
                    // TODO use a move to be more efficient
//                   } else {
//                        move()
//                    }
                    DragAndDropUtils.refreshResource(target);
                } catch (CoreException e) {
                    LOGGER.error("An error occured when transfering the file \""
                            + fs.getFullName() + "\". ", e);
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @param src the explorer file store to copy
     * @param the destination file store
     * @throws CoreException
     */
    private void copy(final ExplorerFileStore src,
            final ExplorerFileStore dest) throws CoreException {
        src.copy(dest, EFS.NONE, null);

        File dstDir = dest.toLocalFile(EFS.NONE, null);
        IResource res = KnimeResourceUtil.getResourceForURI(dstDir.toURI());
        if (res != null && res instanceof IWorkspaceRoot) {
            /* The target is the workspace root. Therefore we have to create a
             * .project file. */
            IProject newProject =
                ((IWorkspaceRoot)res).getProject(src.getName());
            if (!newProject.exists()) {
                try {
                    newProject = MetaInfoFile.createKnimeProject(
                            newProject.getName(), KNIMEProjectNature.ID);
                } catch (Exception e) {
                    String message = "Could not create KNIME project in "
                            + "workspace root.";
                    throw new CoreException(new Status(Status.ERROR,
                            ExplorerActivator.PLUGIN_ID, message, e));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dragStart(final List<ExplorerFileStore> fileStores) {
        return true;
    }

}
