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
package org.knime.workbench.editor2.actions.ports;

import java.util.NoSuchElementException;

import org.eclipse.jface.action.Action;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.editor2.commands.ReplaceNodePortCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.meta.MetaPortDialog;

/**
 * An abstract port action for native node container nodes that allows to change a node's ports.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
abstract class AbstractNativeNodePortAction extends Action {

    protected final String m_grpName;

    private final NodeContainerEditPart m_editPart;

    private final ModifiableNodeCreationConfiguration m_creationConfig;

    private final String m_text;

    /**
     * Constructor.
     *
     * @param editPart the node container edit part
     * @param creationConfig the node creation configuration
     * @param grpName the group name
     * @param text the name of the action
     */
    protected AbstractNativeNodePortAction(final NodeContainerEditPart editPart,
        final ModifiableNodeCreationConfiguration creationConfig, final String grpName, final String text) {
        m_editPart = editPart;
        m_creationConfig = creationConfig;
        m_grpName = grpName;
        m_text = text;
    }

    @Override
    public String getText() {
        return m_text;
    }

    @Override
    public void run() {
        if (modifyPorts()) {
            m_editPart.getViewer().getEditDomain().getCommandStack()
                .execute(new ReplaceNodePortCommand(m_editPart, m_creationConfig));
        }
    }

    /**
     * Returns the port group for the given group name.
     *
     * @param clazz the class of that group
     * @return the port group for the given group name
     * @throws NoSuchElementException If there is no port group for the given group name
     * @throws ClassCastException If the port group's class does not match the provided class
     */
    protected final <T extends PortGroupConfiguration> T getPortGroup(final Class<T> clazz) {
        return clazz.cast(m_creationConfig.getPortConfig().get().getGroup(m_grpName));
    }

    /**
     * Creates a dialog to select any port type from the given supported types.
     *
     * @param title the title of the dialog
     * @param supportedTypes the supported port types
     * @return the selected port type, can be null
     */
    protected static final PortType createDialog(final String title, final PortType[] supportedTypes) {
        return new MetaPortDialog(SWTUtilities.getKNIMEWorkbenchShell(), title, supportedTypes).open();
    }

    /**
     * Modifies the the port group for the given name.
     *
     * @return {@code true} if the port group has been modified, {@code false} otherwise
     */
    abstract protected boolean modifyPorts();

}
