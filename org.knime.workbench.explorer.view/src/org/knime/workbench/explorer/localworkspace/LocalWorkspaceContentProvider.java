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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 * Provides content for the user space view that shows the content (workflows
 * and workflow groups) of the local workspace of the workbench.
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceContentProvider extends AbstractContentProvider {

    private static final Image LOCAL_WS_IMG = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_default.png").createImage();

    /**
     * @param factory the factory that created us.
     * @param id mount id
     */
    LocalWorkspaceContentProvider(
            final LocalWorkspaceContentProviderFactory factory, final String id) {
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
        File parentFile;
        try {
            parentFile = parent.toLocalFile(EFS.NONE, null);
        } catch (CoreException ce) {
            return NO_CHILD;
        }
        // we display nodes, metanodes and workflows - which are directories
        if (!parentFile.isDirectory()) {
            return NO_CHILD;
        }
        if (ExplorerFileStore.isNode(parentFile)) {
            return NO_CHILD;
        }
        try {
            IFileStore[] childs = parent.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            ArrayList<ExplorerFileStore> result =
                    new ArrayList<ExplorerFileStore>();
            for (IFileStore c : childs) {
                File childFile = c.toLocalFile(EFS.NONE, null);
                if (childFile == null) {
                    continue;
                }
                if (ExplorerFileStore.isWorkflowGroup(childFile)
                        || ExplorerFileStore.isWorkflow(childFile)
                        || ExplorerFileStore.isMetaNode(childFile)
                        || ExplorerFileStore.isNode(childFile)) {
                    result.add(new LocalWorkspaceFileStore(getMountID(),
                            new Path(parent.getFullName()).append(c.getName())
                                    .toString()));
                }
            }
            return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
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
        File f;
        try {
            f = e.toLocalFile(EFS.NONE, null);
        } catch (CoreException ce) {
            return ICONS.error();
        }
        if (ExplorerFileStore.isNode(f)) {
            return ICONS.node();
        }
        if (ExplorerFileStore.isMetaNode(f)) {
            return ICONS.node();
        }
        if (ExplorerFileStore.isWorkflowGroup(f)) {
            return ICONS.workflowgroup();
        }
        if (!ExplorerFileStore.isWorkflow(f)) {
            return ICONS.unknownRed();
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
    public void saveState(final IMemento memento) {
        // nothing to save here.
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
    public boolean validateDrop(final Object target, final int operation,
            final TransferData transferType) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final Object data) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dragStart(final List<ExplorerFileStore> fileStores) {
        // TODO Auto-generated method stub
        return true;
    }

}
