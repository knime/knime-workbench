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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.preferences.PreferenceConstants;

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
     *
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
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AbstractContentProvider provider) {
        return m_id.compareTo(provider.getMountID());
    }

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

    /* ---------------- view context menu methods ------------------- */
    /**
     * Add items to the context menu.
     *
     * @param manager the context menu manager
     * @param selection the current selection sorted by content provider (with
     *            all selected item for all providers!)
     */
    public abstract void addContextMenuActions(
            final TreeViewer viewer,
            final IMenuManager manager,
            final Map<AbstractContentProvider, List<ExplorerFileStore>> selection);

    /* ---------------- drag and drop methods ----------------------- */

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
     *            {@link ViewerDropAdapter#getCurrentOperation()}
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

    protected boolean performDropMetaNodeTemplate(final WorkflowManager
        metaNode, final ExplorerFileStore target) {
        File directory;
        try {
            directory = target.toLocalFile(EFS.NONE, null);
        } catch (CoreException e) {
            LOGGER.warn("Unable to convert \"" + target + "\" to local path "
                    + "(mount provider \"" + toString() + "\"");
            return false;
        }
        if (!directory.isDirectory() || ExplorerFileStore.isWorkflow(target)
                || ExplorerFileStore.isNode(target)
                || ExplorerFileStore.isMetaNode(target)) {
            return false;
        }
        Shell s = Display.getDefault().getActiveShell();
        String mountIDWithFullPath = target.getMountIDWithFullPath();
        if (!directory.canWrite()) {
            MessageDialog.openWarning(s, "No write permission", "You don't have"
                    + " sufficient privileges to write to the target"
                    + " directory \"" + mountIDWithFullPath + "\"");
            return false;
        }
        while (!VMFileLocker.lockForVM(directory)) {
            MessageDialog dialog = new MessageDialog(s,
                    "Unable to lock directory", null, "The target folder \""
                    + mountIDWithFullPath + "\" can currently not be locked. ",
                    MessageDialog.QUESTION,
                    new String[] {"&Try again", "&Cancel"}, 0);
            if (dialog.open() == 0) {
                continue; // next try
            } else {
                return false; // abort
            }
        }
        try {
            assert VMFileLocker.isLockedForVM(directory);
            String uniqueName = metaNode.getName();
            File wmDir = new File(directory, uniqueName);
            ExplorerFileStore efsDir = target.getChild(uniqueName);
            boolean doesTargetExist = wmDir.exists();
            boolean isOverwrite = false;
            if (doesTargetExist
                    && !ExplorerFileStore.isWorkflowTemplate(efsDir)) {
                StringBuilder eMsg = new StringBuilder();
                eMsg.append("The target directory \"");
                eMsg.append(mountIDWithFullPath).append("/").append(uniqueName);
                eMsg.append("\" already exists and can't be overwritten as it");
                eMsg.append(" does not represent a workflow template.\n\n");
                eMsg.append("The new template will be saved to a different ");
                eMsg.append("folder instead.");
                if (MessageDialog.openConfirm(
                        s, "Existing folder", eMsg.toString())) {
                    isOverwrite = false;
                } else {
                    return false;
                }
            } else if (doesTargetExist) {
                StringBuilder eMsg = new StringBuilder();
                eMsg.append("A template folder with the name \"");
                eMsg.append(mountIDWithFullPath).append("/");
                eMsg.append(uniqueName);
                eMsg.append("\" already exists.");
                MessageDialog md = new MessageDialog(s, "Existing folder",
                        null, eMsg.toString(), MessageDialog.WARNING,
                        new String[] {"&Overwrite", "&Rename", "&Cancel"}, 0);
                switch (md.open()) {
                case 0: // Overwrite
                    isOverwrite = true;
                    break;
                case 1: // Rename
                    isOverwrite = false;
                    break;
                case 2: // Cancel
                    return false;
                }
            }
            boolean linkMetaNodeToNewTemplate;
            switch (promptLinkMetaNodeTemplate()) {
            case IDialogConstants.YES_ID:
                linkMetaNodeToNewTemplate = true;
                break;
            case IDialogConstants.NO_ID:
                linkMetaNodeToNewTemplate = false;
                break;
            default: // Cancel
                return false;
            }
            if (doesTargetExist && !isOverwrite) { // generate new name
                Set<String> set;
                try {
                    set = new HashSet<String>(
                            Arrays.asList(target.childNames(EFS.NONE, null)));
                } catch (CoreException e) {
                    LOGGER.warn("Can't query child elements of target \""
                            + target + "\"", e);
                    set = Collections.emptySet();
                }
                UniqueNameGenerator nameGen = new UniqueNameGenerator(set);
                uniqueName = nameGen.newName(uniqueName);
                efsDir = target.getChild(uniqueName);
                wmDir = new File(directory, uniqueName);
            }

            if (isOverwrite) {
                boolean isWMLocked = VMFileLocker.lockForVM(wmDir);
                if (!isWMLocked) {
                    MessageDialog.openError(s, "Target directory locked",
                            "The folder \"" + efsDir.getMountIDWithFullPath()
                            + " is currently locked by another process");
                    return false;
                }
                ExplorerFileSystemUtils.deleteLockedWorkflows(
                        Collections.singletonList(efsDir));
            }
            try {
                MetaNodeTemplateInformation template =
                    metaNode.saveAsMetaNodeTemplate(
                            wmDir, new ExecutionMonitor());
                if (linkMetaNodeToNewTemplate) {
                    URI uri = efsDir.toURI();
                    MetaNodeTemplateInformation link = template.createLink(uri);
                    metaNode.getParent().setTemplateInformation(
                            metaNode.getID(), link);
                }
            } catch (Exception e) {
                String error = "Unable to save template: " + e.getMessage();
                LOGGER.warn(error, e);
                MessageDialog.openError(s, "Error while writing template",
                        error);
            }
        } finally {
            VMFileLocker.unlockForVM(directory);
        }
        return true;
    }

    private int promptLinkMetaNodeTemplate() {
        IPreferenceStore corePrefStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        String pref = corePrefStore.getString(
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE);
        if (MessageDialogWithToggle.ALWAYS.equals(pref)) {
            return IDialogConstants.YES_ID;
        } else if (MessageDialogWithToggle.NEVER.equals(pref)) {
            return IDialogConstants.NO_ID;
        }
        Shell activeShell = Display.getDefault().getActiveShell();
        MessageDialogWithToggle dlg =
            MessageDialogWithToggle.openYesNoCancelQuestion(activeShell,
                "Link Meta Node Template",
                "Update meta node to link to the template?",
                "Remember my decision", false, corePrefStore,
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE);
        return dlg.getReturnCode();
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
     *
     * @param template to return children for
     * @return children of the template.
     */
    public static ExplorerFileStore[] getWorkflowTemplateChildren(
            final ExplorerFileStore template) {
        // meta nodes have not children (as long as we don't show their nodes)
        return NO_CHILD;
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
     *
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
        if (ExplorerFileStore.isWorkflowTemplate(efs)) {
            return ICONS.workflowtemplate();
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
