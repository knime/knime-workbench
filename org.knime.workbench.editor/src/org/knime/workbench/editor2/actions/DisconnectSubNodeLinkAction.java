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
 *   25.05.2010 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.UI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.DisconnectSubNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to disconnect a sub node link from its template.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class DisconnectSubNodeLinkAction extends AbstractNodeAction {

    /** Action ID. */
    public static final String ID = "knime.action.sub_node_disconnect_link";

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public DisconnectSubNodeLinkAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Disconnect Link";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Removes the link to the shared component and keeps a local, editable copy of the component.";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_disconnect.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        NodeContainerEditPart[] nodes =
            getSelectedParts(NodeContainerEditPart.class);
        for (NodeContainerEditPart p : nodes) {
            Object model = p.getModel();
            if (Wrapper.wraps(model, SubNodeContainer.class)) {
                SubNodeContainer snc = Wrapper.unwrap((UI)model, SubNodeContainer.class);
                MetaNodeTemplateInformation i = snc.getTemplateInformation();
                if (Role.Link.equals(i.getRole())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        List<NodeID> idList = new ArrayList<NodeID>();
        for (NodeContainerEditPart p : nodeParts) {
            Object model = p.getModel();
            if (Wrapper.wraps(model, SubNodeContainer.class)) {
                SubNodeContainer snc = Wrapper.unwrap((UI)model, SubNodeContainer.class);
                MetaNodeTemplateInformation i = snc.getTemplateInformation();
                if (Role.Link.equals(i.getRole())) {
                    idList.add(snc.getID());
                }
            }
        }
        NodeID[] ids = idList.toArray(new NodeID[idList.size()]);
        DisconnectSubNodeLinkCommand disCmd =
            new DisconnectSubNodeLinkCommand(getManager(), ids);
        execute(disCmd);
    }

}
