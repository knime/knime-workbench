/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   17 Aug 2022 (leon.wenzler): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.BulkChangeMetaNodeLinksCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Allows for bulk changes of metanode and component links.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public class BulkChangeMetaNodeLinksAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BulkChangeMetaNodeLinksAction.class);

    /** Action ID. */
    public static final String ID = "knime.action.meta_node_bulk_change_links";

    /** Selected action in the dialog */
    public enum LinkChangeAction {

            /**
             * Indicates that the link type was changed, e.g. absolute, relative to, ...
             */
            TYPE_CHANGE,

            /**
             * Indicates that the link URI has changed.
             */
            URI_CHANGE,

            /**
             * Indicates that the KNIME Hub Item Version has changed.
             */
            VERSION_CHANGE,

            /**
             * Indicates that no changes have been made.
             */
            NO_CHANGE;
    }

    /** @param editor The host editor. */
    public BulkChangeMetaNodeLinksAction(final WorkflowEditor editor) {
        super(editor);
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return ID;
    }

    /** {@inheritDoc} */
    @Override
    public String getText() {
        return "Change Links...\t" + getHotkey("knime.commands.bulkChangeMetaNodeLinks");
    }

    /** {@inheritDoc} */
    @Override
    public String getToolTipText() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_change.png");
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends EditPart> T[] getSelectedParts(final Class<T> editPartClass) {
        return getAllParts(editPartClass);
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new IllegalStateException("Not to be called");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        return !getMetaNodesToCheck().isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public void runInSWT() {
        var shell = SWTUtilities.getActiveShell();
        var metaNodeList = getMetaNodesToCheck();
        var manager = getEditor().getWorkflowManager().orElse(null);
        if (manager == null) {
            MessageDialog.openInformation(shell, "Changing links",
                "Changing links is currently only possible for local workflows");
            return;
        }

        final var dialog = new BulkChangeMetaNodeLinksDialog(shell, metaNodeList, manager);
        if (dialog.open() != 0) {
            // dialog has been cancelled - no changes
            return;
        }

        BulkChangeMetaNodeLinksCommand changeCommand = null;
        var selectedMetaNodes = dialog.getNCsToChange();
        switch (dialog.getLinkChangeAction()) {
            case TYPE_CHANGE:
                changeCommand =
                    new BulkChangeMetaNodeLinksCommand(getManager(), LinkChangeAction.TYPE_CHANGE, selectedMetaNodes);
                changeCommand.setLinkType(dialog.getLinkType());
                break;
            case VERSION_CHANGE, URI_CHANGE: // same as URI change, the new URI will have the item version set as query parameter
                changeCommand =
                    new BulkChangeMetaNodeLinksCommand(getManager(), LinkChangeAction.URI_CHANGE, selectedMetaNodes);
                changeCommand.setURI(dialog.getURI());
                break;
            case NO_CHANGE:
                LOGGER.warn("No link properties will be changed");
                selectedMetaNodes = Collections.emptyList();
                break;
            default:
                throw new IllegalStateException("Cannot change metanode links: unknown link change action");
        }
        if (selectedMetaNodes.isEmpty()) {
            MessageDialog.openInformation(shell, "Change Links", "No link properties have been changed");
        } else {
            openConfirmationDialog(selectedMetaNodes, changeCommand);
        }
    }

    /**
     * Opens the dialog for confirming the link changes.
     *
     * @param selectedMetaNodes
     * @param changeCommand
     */
    private void openConfirmationDialog(final List<NodeContainerTemplate> selectedMetaNodes,
        final BulkChangeMetaNodeLinksCommand changeCommand) {
        var templateType = selectedMetaNodes.get(0) instanceof SubNodeContainer ? "Component" : "Metanode";
        var amount = selectedMetaNodes.size();
        var isSingle = amount == 1;
        var title = "Change " + (isSingle ? "one" : amount) + " " + templateType + (isSingle ? "" : "s");

        var messageBuilder = new StringBuilder();
        messageBuilder.append("The following nodes will be changed:");
        for (NodeContainerTemplate nct : selectedMetaNodes) {
            messageBuilder.append("\n- \"" + nct.getNameWithID() + "\"");
        }
        messageBuilder.append("\n\nChange now?");

        if (MessageDialog.openQuestion(SWTUtilities.getActiveShell(), title, messageBuilder.toString())) {
            LOGGER.debug("Changing links on " + selectedMetaNodes.size() + " node(s): " + selectedMetaNodes);
            execute(changeCommand);
        }
    }

    /**
     * Based on the selection in the workflow, recursively retrieves all linked metanodes/components which are
     * candidates for bulk changing.
     *
     * @return List of candidate NodeIDs
     */
    private List<NodeContainerTemplate> getMetaNodesToCheck() {
        List<NodeContainerTemplate> list = new ArrayList<>();
        for (NodeContainerEditPart p : getSelectedParts(NodeContainerEditPart.class)) {
            NodeContainerUI model = p.getNodeContainer();
            if (Wrapper.wraps(model, NodeContainerTemplate.class)) {
                NodeContainerTemplate tnc = Wrapper.unwrap(model, NodeContainerTemplate.class);
                if (tnc.getTemplateInformation().getRole() == Role.Link) {
                    list.add(tnc);
                } else {
                    // only recursively check in unlinked templates
                    list.addAll(getNCTemplatesToCheck(tnc));
                }
            }
        }
        return list;
    }

    /**
     * Does the recursive collection of node candidates for {@link BulkChangeMetaNodeLinksAction#getMetaNodesToCheck()}
     * for a given NodeContainerTemplate.
     *
     * @param template NodeContainerTemplate
     * @return List of candidate NodeIDs
     */
    private static List<NodeContainerTemplate> getNCTemplatesToCheck(final NodeContainerTemplate template) {
        List<NodeContainerTemplate> list = new ArrayList<>();
        for (NodeContainer nc : template.getNodeContainers()) {
            if (nc instanceof NodeContainerTemplate) {
                NodeContainerTemplate tnc = (NodeContainerTemplate)nc;
                if (tnc.getTemplateInformation().getRole() == Role.Link) {
                    list.add(tnc);
                } else {
                    // only recursively check in unlinked templates
                    list.addAll(getNCTemplatesToCheck(tnc));
                }

            }
        }
        return list;
    }
}
