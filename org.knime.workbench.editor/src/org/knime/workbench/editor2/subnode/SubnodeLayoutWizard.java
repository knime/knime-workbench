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
 *   24.03.2014 (Christian Albrecht, KNIME AG, Zurich, Switzerland): created
 */
package org.knime.workbench.editor2.subnode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.wizard.Wizard;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.wizard.page.WizardPageUtil;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.SubnodeContainerConfigurationStringProvider;
import org.knime.core.node.workflow.SubnodeContainerLayoutStringProvider;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 */
public class SubnodeLayoutWizard extends Wizard {

    private final SubNodeContainer m_subNodeContainer;
    private SubnodeLayoutJSONEditorPage m_page;

    /**
     * @param container the subnode container
     *
     */
    public SubnodeLayoutWizard(final SubNodeContainer container) {
        m_subNodeContainer = container;
        setHelpAvailable(false);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void addPages() {
        setWindowTitle("Node Usage and Layout");
        setDefaultPageImageDescriptor(
            ImageRepository.getImageDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout_55.png"));
        WorkflowManager wfManager = m_subNodeContainer.getWorkflowManager();
        Map<NodeID, SubNodeContainer> nestedSubnodes = WizardPageUtil.getSubPageNodes(wfManager);
        List<NativeNodeContainer> viewNodes = WizardPageUtil.getAllWizardPageNodes(wfManager, false);
        LinkedHashMap<NodeIDSuffix, SingleNodeContainer> resultMap = new LinkedHashMap<>();
        for (NativeNodeContainer nc : viewNodes) {
            NodeID.NodeIDSuffix idSuffix = NodeID.NodeIDSuffix.create(wfManager.getID(), nc.getID());
            resultMap.put(idSuffix, nc);
        }
        Map<NodeID, DialogNode> viewConfigurationNodes = wfManager.findNodes(DialogNode.class, false);
        LinkedHashMap<NodeIDSuffix, DialogNode> resultMapConfiguration = new LinkedHashMap<>();
        for (Map.Entry<NodeID, DialogNode> entry : viewConfigurationNodes.entrySet()) {
            NodeID.NodeIDSuffix idSuffix = NodeID.NodeIDSuffix.create(wfManager.getID(), entry.getKey());
            resultMapConfiguration.put(idSuffix, entry.getValue());
        }
        for (Map.Entry<NodeID, SubNodeContainer> entry : nestedSubnodes.entrySet()) {
            NodeID.NodeIDSuffix idSuffix = NodeID.NodeIDSuffix.create(wfManager.getID(), entry.getKey());
            resultMap.put(idSuffix, entry.getValue());
        }
        m_page = new SubnodeLayoutJSONEditorPage("Change the layout configuration");
        m_page.setNodes(wfManager, m_subNodeContainer, resultMap);
        m_page.setConfigurationNodes(wfManager, m_subNodeContainer, resultMapConfiguration);
        addPage(m_page);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        if (m_page == null || !m_page.isJSONValid()) {
            return false;
        }
        return super.canFinish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        if (m_page.isJSONValid()) {
            SubnodeContainerLayoutStringProvider layoutStringProvider =
                    m_subNodeContainer.getSubnodeLayoutStringProvider();
            SubnodeContainerConfigurationStringProvider configurationStringProvider =
                    m_subNodeContainer.getSubnodeConfigurationLayoutStringProvider();
            layoutStringProvider.setLayoutString(m_page.getJsonDocument());
            configurationStringProvider.setConfigurationLayoutString(m_page.getJsonConfigurationDocument());
            m_subNodeContainer.setSubnodeLayoutStringProvider(layoutStringProvider);
            m_subNodeContainer.setSubnodeConfigurationStringProvider(configurationStringProvider);
            return m_page.applyUsageChanges();
        } else {
            return false;
        }
    }

}
