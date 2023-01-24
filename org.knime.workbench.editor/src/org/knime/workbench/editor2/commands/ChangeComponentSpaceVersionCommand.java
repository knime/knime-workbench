/*
 * ------------------------------------------------------------------------
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
 * Created on Sep 21, 2022 by Carl Witt
 */
package org.knime.workbench.editor2.commands;

import java.net.URI;

import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.eclipse.gef.commands.CompoundCommand;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * A compound command that first changes the link (source URI) of a linked Component and then performs an update.
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class ChangeComponentSpaceVersionCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ChangeComponentSpaceVersionCommand.class);

    private final SubNodeContainer m_component;

    private final String m_targetVersion;

    /** To achieve a version change, the source URI is modified and then an update is performed. */
    private final CompoundCommand m_commandRegistry = new CompoundCommand();

    /**
     * @param manager
     * @param component
     * @param targetVersion
     */
    public ChangeComponentSpaceVersionCommand(final WorkflowManager manager, final SubNodeContainer component,
            final String targetVersion) {
        super(manager);

        m_component = component;
        m_targetVersion = targetVersion;
    }

    static URI setSpaceVersion(final URI oldUri, final String spaceVersion) {
        // this removes the parameter if spaceVersion is null
        return new UriBuilderImpl(oldUri)//
            .replaceQueryParam("spaceVersion", spaceVersion)//
            .build();
    }

    @Override
    public void execute() {
        doLinkURIChange();
        var updateComponentCommand = new UpdateMetaNodeLinkCommand(getHostWFM(), new NodeID[]{m_component.getID()});
        updateComponentCommand.execute();
        m_commandRegistry.add(updateComponentCommand);
    }

    private void doLinkURIChange() {
        final var oldUri = m_component.getTemplateInformation().getSourceURI();
        final var targetUri = setSpaceVersion(oldUri, m_targetVersion);
        final var changeCommand = new ChangeSubNodeLinkCommand(getHostWFM(), m_component, oldUri, targetUri);
        if (changeCommand.canExecute()) {
            changeCommand.execute();
            m_commandRegistry.add(changeCommand);
        }
    }

    @Override
    public boolean canUndo() {
        return m_commandRegistry.canUndo();
    }

    @Override
    public void undo() {
        m_commandRegistry.undo();
    }

}
