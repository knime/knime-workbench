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
 *   Oct 14, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.workbench.editor2.commands;

import java.util.Collections;
import java.util.Map;

import org.eclipse.gef.RootEditPart;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class ReplaceNodePortCommand extends CreateNodeCommand {

    private final ModifiableNodeCreationConfiguration m_unmodifiedlConfig;

    private final NodeContainerEditPart m_nodeToReplace;

    private final RootEditPart m_root;

    private final DeleteCommand m_delete;

    private final ReplacePortConnectionHelper m_replaceHelper;

    /**
     * Constructor.
     *
     * @param nodeToReplace the node to be replaced
     * @param modifiedConfig the modified creation configuration
     */
    public ReplaceNodePortCommand(final NodeContainerEditPart nodeToReplace,
        final ModifiableNodeCreationConfiguration modifiedConfig) {
        super(nodeToReplace, modifiedConfig);
        m_nodeToReplace = nodeToReplace;
        m_unmodifiedlConfig = Wrapper.unwrap(m_nodeToReplace.getNodeContainer(), NativeNodeContainer.class).getNode()
            .getCopyOfCreationConfig().get();
        m_root = m_nodeToReplace.getRoot();
        m_delete = new DeleteCommand(Collections.singleton(m_nodeToReplace), getHostWFM());
        m_replaceHelper =
            new ReplacePortConnectionHelper(getHostWFM(), Wrapper.unwrapNC(m_nodeToReplace.getNodeContainer()));
    }

    @Override
    public boolean canExecute() {
        return super.canExecute() && m_delete.canExecute() && m_replaceHelper.replaceNode();
    }

    @Override
    public void execute() {
        // store the node's settings
        final NodeSettings settings = new NodeSettings("node settings");
        try {
            getHostWFM().saveNodeSettings(m_nodeToReplace.getNodeContainer().getID(), settings);
        } catch (InvalidSettingsException e) {
            // no valid settings available, skip
        }
        // delete the old node and create the new one
        m_delete.execute();
        super.execute();

        // load the previously stored settings
        final NodeContainer newNode = Wrapper.unwrapNC(m_container);
        try {
            getHostWFM().loadNodeSettings(newNode.getID(), settings);
        } catch (InvalidSettingsException e) {
            // ignore
        }

        // copy the node's annotation
        m_container.getNodeAnnotation().copyFrom(m_nodeToReplace.getNodeContainer().getNodeAnnotation().getData(),
            true);

        // move the node to the position of the deleted node and re-wire inputs and output
        m_replaceHelper.setConnectionUIInfoMap(m_delete.getConnectionUIInfo());
        m_replaceHelper.reconnect(newNode,
            m_unmodifiedlConfig.getPortConfig().get().mapInputPorts(getCreationConfig().getPortConfig().get()),
            m_unmodifiedlConfig.getPortConfig().get().mapOutputPorts(getCreationConfig().getPortConfig().get()));

        // the connections are not always properly re-drawn after "unmark". (Eclipse bug.) Repaint here.
        m_root.refresh();
    }

    @Override
    public boolean canUndo() {
        return super.canUndo() && m_delete.canUndo();
    }

    @Override
    public void undo() {
        super.undo();
        m_delete.undo();
    }

    private static class ReplacePortConnectionHelper extends ReplaceHelper {

        /**
         * @param wfm
         * @param oldNode
         */
        public ReplacePortConnectionHelper(final WorkflowManager wfm, final NodeContainer oldNode) {
            super(wfm, oldNode);
        }

        void reconnect(final NodeContainer container, final Map<Integer, Integer> inputPortMapping,
            final Map<Integer, Integer> outputPortMapping) {
            setUIInformation(container);
            // set incoming connections
            final NodeID newId = container.getID();
            for (final ConnectionContainer c : m_incomingConnections) {
                if (m_wfm.canAddConnection(c.getSource(), c.getSourcePort(), newId,
                    inputPortMapping.get(c.getDestPort()))) {
                    final ConnectionContainer cc = m_wfm.addConnection(c.getSource(), c.getSourcePort(), newId,
                        inputPortMapping.get(c.getDestPort()));
                    setConnectionUIInfo(c, cc);
                }
            }

            // set outgoing connections
            for (final ConnectionContainer c : m_outgoingConnections) {
                if (m_wfm.canAddConnection(newId, outputPortMapping.get(c.getSourcePort()), c.getDest(),
                    c.getDestPort())) {
                    final ConnectionContainer cc = m_wfm.addConnection(newId, outputPortMapping.get(c.getSourcePort()),
                        c.getDest(), c.getDestPort());
                    setConnectionUIInfo(c, cc);
                }
            }
        }

    }

}