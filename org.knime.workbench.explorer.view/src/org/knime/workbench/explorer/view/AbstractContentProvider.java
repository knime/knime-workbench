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
package org.knime.workbench.explorer.view;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 * Content and label provider for one source in the user space view. One
 * instance represents one mount point. It might be used by multiple views to
 * show the content of that one mount point.
 *
 * @author ohl, University of Konstanz
 */
public abstract class AbstractContentProvider extends LabelProvider implements
        IStructuredContentProvider, ITreeContentProvider,
        Comparable<AbstractContentProvider> {

    /**
     * Use icons from this class for a uniform look.
     */
    public static final IconFactory ICONS = new IconFactory();

    /**
     * Empty result array.
     */
    protected static final ExplorerFileStore[] NO_CHILD =
            new ExplorerFileStore[0];

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractContentProvider.class);

    private final AbstractContentProviderFactory m_creator;

    private final String m_id;

    /**
     * @param myCreator the factory creating this instance.
     * @param id mount id of this content provider
     *
     */
    public AbstractContentProvider(
            final AbstractContentProviderFactory myCreator, final String id) {
        if (myCreator == null) {
            throw new NullPointerException(
                    "The factory creating this object must be set");
        }
        if (id == null || id.isEmpty()) {
            throw new NullPointerException(
                    "The mount id can't be null nor empty");
        }
        m_creator = myCreator;
        m_id = id;
    }

    /**
     * @return the factory that created this object.
     */
    public AbstractContentProviderFactory getFactory() {
        return m_creator;
    }

    /**
     * Returns the ID this content is mounted with.
     *
     * @return the mount id of this content provider.
     */
    public String getMountID() {
        return m_id;
    }

    /**
     *
     */
    public final void refresh() {
        // TODO: create an event!
        fireLabelProviderChanged(null);
    }

    /**
     * Save state and parameters.
     * @return a string representation of this factory
     *
     * @see AbstractContentProviderFactory
     */
    public abstract String saveState();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void dispose();

    /**
     * @return displayed name of this instance. {@inheritDoc}
     */
    @Override
    public abstract String toString();

    /**
     * @return icon of this instance. Or null, if you don't have any.
     */
    public abstract Image getImage();

    /**
     * @param fullPath the path to the item.
     * @return the file store for the specified path.
     */
    public abstract ExplorerFileStore getFileStore(final String fullPath);

    /**
     * @param uri the uri of the item
     * @return the file store for the specified uri
     */
    public final ExplorerFileStore getFileStore(final URI uri) {
        return new ExplorerFileSystem().getStore(uri);
    }

    /**
     * @param target the target the data is dropped on
     * @param operation the operation to be performed
     * @param transferType the transfer type
     * @return true if the drop is valid, false otherwise
     */
    public abstract boolean validateDrop(final ExplorerFileStore target,
            final int operation, TransferData transferType);

    /**
     * Performs any work associated with the drop. Drop data might be null. In
     * this case the implementing classes should try to retrieve the data from
     * the {@link LocalSelectionTransfer}.
     *
     * @param data the drop data, might be null
     * @param operation the operation to be performed as received from
     *      {@link ViewerDropAdapter#getCurrentOperation()}
     * @param target the drop target
     * @return true if the drop was successful, false otherwise
     * @see ViewerDropAdapter#getCurrentOperation()
     */
    public abstract boolean performDrop(final Object data,
            final ExplorerFileStore target, int operation);

    /**
     * @param fileStores the dragged file stores of the content provider
     * @return true if dragging is allowed for the selection, false otherwise
     */
    public abstract boolean dragStart(List<ExplorerFileStore> fileStores);

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AbstractContentProvider provider) {
        return m_id.compareTo(provider.getMountID());
    }

    /* -------------- content provider methods ---------------------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ExplorerFileStore[] getChildren(Object parentElement);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ExplorerFileStore[] getElements(Object inputElement);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ExplorerFileStore getParent(Object element);

    /* ---------- helper methods for content provider ------------------- */
    /**
     * Helper method for content providers. Returns children of a workflow.
     *
     * @param workflow the workflow to return the children for
     * @return children of a workflow
     */
    public static ExplorerFileStore[] getWorkflowChildren(
            final ExplorerFileStore workflow) {
        assert ExplorerFileStore.isWorkflow(workflow);

        try {
            IFileStore[] childs = workflow.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            /*
             * currently we are not showing nodes
             */
            return NO_CHILD;
            // ArrayList<ExplorerFileStore> result =
            // new ArrayList<ExplorerFileStore>();
            // for (IFileStore c : childs) {
            // not adding nodes for now.
            // if (ExplorerFileStore.isMetaNode((ExplorerFileStore)c)) {
            // || ExplorerFileStore.isNode(childFile)) {
            // result.add((ExplorerFileStore)c);
            // }
            // }
            // return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }

    }

    /**
     * Helper method for content providers. Returns children of a workflowgroup.
     *
     * @param workflowGroup the workflow group to return the children for
     * @return the content of the workflow group
     */
    public static ExplorerFileStore[] getWorkflowgroupChildren(
            final ExplorerFileStore workflowGroup) {

        assert ExplorerFileStore.isWorkflowGroup(workflowGroup);

        try {
            IFileStore[] childs = workflowGroup.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            ArrayList<ExplorerFileStore> result =
                    new ArrayList<ExplorerFileStore>();
            for (IFileStore c : childs) {
                if (ExplorerFileStore.isWorkflowGroup((ExplorerFileStore)c)
                        || ExplorerFileStore.isWorkflow(c)) {
                    result.add((ExplorerFileStore)c);
                }
            }
            return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }
    }

    public static ExplorerFileStore[] getMetaNodeChildren(
            final ExplorerFileStore metaNode) {
        assert ExplorerFileStore.isMetaNode(metaNode);

        try {
            IFileStore[] childs = metaNode.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            /*
             * currently we are not showing nodes
             */
            return NO_CHILD;
            // ArrayList<ExplorerFileStore> result =
            // new ArrayList<ExplorerFileStore>();
            // for (IFileStore c : childs) {
            // not adding nodes for now.
            // if (ExplorerFileStore.isMetaNode((ExplorerFileStore)c)) {
            // || ExplorerFileStore.isNode(childFile)) {
            // result.add((ExplorerFileStore)c);
            // }
            // }
            // return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }

    }

    /* ------------ helper methods for label provider (icons) ------------- */
    /**
     * Returns an icon/image for the passed file, if it is something like a
     * workflow, group, node or meta node. If it is not a store representing one
     * of these, null is returned.
     * @param efs the explorer file store
     * @return the icon/image for the passed file store
     */
    public static Image getWorkspaceImage(final ExplorerFileStore efs) {

        if (ExplorerFileStore.isNode(efs)) {
            return ICONS.node();
        }
        if (ExplorerFileStore.isMetaNode(efs)) {
            return ICONS.node();
        }
        if (ExplorerFileStore.isWorkflowGroup(efs)) {
            return ICONS.workflowgroup();
        }
        if (!ExplorerFileStore.isWorkflow(efs)) {
            return null;
        }

        // if it is a local workflow return the correct icon for open flows
        File f;
        try {
            f = efs.toLocalFile(EFS.NONE, null);
        } catch (CoreException ce) {
            return ICONS.workflowClosed();
        }

        if (f == null) {
            return ICONS.workflowClosed();
        }
        URI wfURI = f.toURI();
        NodeContainer nc = ProjectWorkflowMap.getWorkflow(wfURI);
        if (nc == null) {
            return ICONS.workflowClosed();
        }
        if (nc instanceof WorkflowManager) {
            if (nc.getID().hasSamePrefix(WorkflowManager.ROOT.getID())) {
                // only show workflow directly off the root
                if (nc.getNodeMessage().getMessageType()
                        .equals(NodeMessage.Type.ERROR)) {
                    return ICONS.workflowError();
                }
                switch (nc.getState()) {
                case EXECUTED:
                    return ICONS.workflowExecuted();
                case PREEXECUTE:
                case EXECUTING:
                case EXECUTINGREMOTELY:
                case POSTEXECUTE:
                    return ICONS.workflowExecuting();
                case CONFIGURED:
                case IDLE:
                    return ICONS.workflowConfigured();
                default:
                    return ICONS.workflowConfigured();
                }
            } else {
                return ICONS.node();
            }
        } else {
            return ICONS.unknown();
        }
    }
}
