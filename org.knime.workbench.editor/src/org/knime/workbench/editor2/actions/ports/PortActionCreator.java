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
 *   Oct 15, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.workbench.editor2.actions.ports;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Creates the port actions.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class PortActionCreator {

    private final NodeContainerEditPart m_editPart;

    private final ModifiableNodeCreationConfiguration m_creationConfig;

    private final NotSupportedPortAction m_notSupportedPortAction;

    /**
     * Constructor.
     *
     * @param editPart the node edit part of a {@code NativeNodeContainer}
     * @throws IllegalArgumentException - if the provided edit part is not an instance of {@code NativeNodeContainerUI}
     */
    public PortActionCreator(final NodeContainerEditPart editPart) {
        m_editPart = editPart;
        if (editPart.getModel() instanceof NativeNodeContainerUI) {
            final NativeNodeContainerUI nnc = (NativeNodeContainerUI)editPart.getModel();
            Optional<ModifiableNodeCreationConfiguration> creationConfig = null;
            try {
                 creationConfig = nnc.getCopyOfCreationConfig();
            } catch (UnsupportedOperationException e) {
                m_notSupportedPortAction = new NotSupportedPortAction(editPart, e.getMessage());
                m_creationConfig = null;
                return;
            }
            if (creationConfig.isPresent() && creationConfig.get().getPortConfig().isPresent()) {
                m_creationConfig = creationConfig.get();
            } else {
                m_creationConfig = null;
            }

        } else {
            m_creationConfig = null;
        }
        m_notSupportedPortAction = null;
    }

    /**
     * Flag indicating whether there are actions available or not.
     *
     * @return {@code true} if there are actions to be created, {@code false} otherwise
     */
    public boolean hasActions() {
        return m_creationConfig != null && m_creationConfig.getPortConfig()
            .map(c -> !c.getExtendablePorts().isEmpty() || !c.getExchangeablePorts().isEmpty()).orElse(false);
    }

    /**
     * Returns all add port actions.
     *
     * @return all add port actions
     */
    public List<? extends Action> getAddPortActions() {
        return m_creationConfig.getPortConfig()//
            .map(ModifiablePortsConfiguration::getExtendablePorts)//
            .map(Map::keySet)//
            .map(s -> {
                final boolean requiresPrefix = s.size() < 2;
                return s.stream()//
                    .map(grpName -> new NativeNodeAddPortAction(m_editPart, m_creationConfig, grpName,
                        requiresPrefix ? "Add " + grpName + " port" : grpName))//
                    .collect(Collectors.toList());
            }).orElse(Collections.emptyList());
    }

    /**
     * Returns all remove port actions.
     *
     * @return all remove port actions
     */
    public List<? extends Action> getRemovePortActions() {
        return m_creationConfig.getPortConfig()//
            .map(ModifiablePortsConfiguration::getExtendablePorts)//
            .map(Map::keySet)//
            .map(s -> {
                final boolean requiresPrefix = s.size() < 2;
                return s.stream()//
                    .map(grpName -> new NativeNodeRemovePortAction(m_editPart, m_creationConfig, grpName,
                        requiresPrefix ? "Remove " + grpName + " port" : grpName))//
                    .collect(Collectors.toList());
            }).orElse(Collections.emptyList());

    }

    /**
     * Returns all exchange port actions.
     *
     * @return all exchange port actions
     */
    public List<? extends Action> getExchangePortActions() {
        return m_creationConfig.getPortConfig()//
            .map(ModifiablePortsConfiguration::getExchangeablePorts)//
            .map(Map::keySet)//
            .map(s -> {
                final boolean requiresPrefix = s.size() < 2;
                return s.stream()//
                    .map(grpName -> new NativeNodeReplacePortAction(m_editPart, m_creationConfig, grpName,
                        requiresPrefix ? "Exchange " + grpName + " port" : grpName))//
                    .collect(Collectors.toList());
            }).orElse(Collections.emptyList());
    }

    /**
     * @return an non-empty optional if port actions should be available but are not supported by the given
     *         {@link NodeContainerUI}-implementation (e.g. because the AP is connected to a older server that doesn't
     *         support dynamic ports)
     */
    public Optional<NotSupportedPortAction> getNotSupportedAction() {
        return Optional.ofNullable(m_notSupportedPortAction);
    }

}
