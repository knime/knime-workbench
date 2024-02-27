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
package org.knime.workbench.editor2.commands;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.dialogs.MessageDialog;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.TemplateType;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.util.SWTUtilities;

/**
 * Changes specific link settings on a list of NodeContainerTemplates.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 */
public class BulkChangeMetaNodeLinksCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BulkChangeMetaNodeLinksCommand.class);

    private CompoundCommand m_commandRegistry = new CompoundCommand();

    private final TemplateType m_templateType;

    private final List<NodeContainerTemplate> m_templatesToChange;

    private final URI m_oldLinkURI;

    private final URI m_newLinkURI;

    private final boolean m_askForUpdate;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager containing the links to be changed.
     * @param templates NodeContainerTemplates to be changed
     * @param newLinkURI new link URI
     * @param askForUpdate should the user be asked whether they want to update all components afterwards?
     */
    public BulkChangeMetaNodeLinksCommand(final WorkflowManager manager, final List<NodeContainerTemplate> templates,
        final URI newLinkURI, final boolean askForUpdate) {
        super(manager);
        m_templatesToChange = templates;
        m_askForUpdate = askForUpdate;

        // determining existing (old) link properties
        if (templates.isEmpty()) {
            throw new IllegalArgumentException("Metanode templates to change must not be empty!");
        }
        final var nc = m_templatesToChange.get(0);
        m_oldLinkURI = nc.getTemplateInformation().getSourceURI();
        m_newLinkURI = newLinkURI;
        m_templateType = nc instanceof SubNodeContainer ? TemplateType.SubNode : TemplateType.MetaNode;
    }

    /**
     * We can execute, if all selected metanodes are present and the selected action can execute.
     */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        // no templates to act on
        if (m_templatesToChange == null || m_templatesToChange.isEmpty()) {
            return false;
        }
        if (m_oldLinkURI == null || m_newLinkURI == null) {
            return false;
        }
        var wfm = getHostWFM();
        return m_templatesToChange.stream().allMatch(nct -> wfm.findNodeContainer(nct.getID()) != null);
    }

    @Override
    public void execute() {
        doLinkURIBulkChange();

        if (m_askForUpdate) {
            // asking if all nodes should be updated subsequently
            var shell = SWTUtilities.getActiveShell();
            if (MessageDialog.openQuestion(shell, "Change Links",
                "Do you want to update all nodes where the links have been changed?\n"
                    + "The update will be fetched from \"" + m_newLinkURI + "\".")) {
                var templateIds = m_templatesToChange.stream() //
                        .map(NodeContainerTemplate::getID) //
                        .toArray(NodeID[]::new);
                var updateCommand = new UpdateMetaNodeLinkCommand(getHostWFM(), templateIds);
                updateCommand.execute();
                m_commandRegistry.add(updateCommand);
                MessageDialog.openInformation(shell, "Change Links", "All selected nodes have been updated.");
            }
        }
    }

    /**
     * Depending on the TemplateType, this method changes the template link URI of all
     * {@link BulkChangeMetaNodeLinksCommand#m_templatesToChange} to the
     * {@link BulkChangeMetaNodeLinksCommand#m_newLinkURI}. Stores the executed commands in the command registry.
     */
    private void doLinkURIBulkChange() {
        if (m_templateType == TemplateType.SubNode) {
            for (NodeContainerTemplate template : m_templatesToChange) {
                final var snc = (SubNodeContainer)template;
                final var oldLastModified =
                        m_askForUpdate ? snc.getTemplateInformation().getTimestampInstant() : null;
                final var newLastModified = m_askForUpdate ? Instant.EPOCH : null;
                var singleChangeCommand = new ChangeSubNodeLinkCommand(getHostWFM(), snc, m_oldLinkURI, oldLastModified,
                    m_newLinkURI, newLastModified);
                if (singleChangeCommand.canExecute()) {
                    singleChangeCommand.execute();
                    m_commandRegistry.add(singleChangeCommand);
                }
            }
        } else if (m_templateType == TemplateType.MetaNode) {
            for (NodeContainerTemplate template : m_templatesToChange) {
                final var metanode = (WorkflowManager)template;
                final var oldLastModified =
                        m_askForUpdate ? metanode.getTemplateInformation().getTimestampInstant() : null;
                final var newLastModified = m_askForUpdate ? Instant.EPOCH : null;
                var singleChangeCommand = new ChangeMetaNodeLinkCommand(getHostWFM(), metanode,
                    m_oldLinkURI, oldLastModified, m_newLinkURI, newLastModified);
                if (singleChangeCommand.canExecute()) {
                    singleChangeCommand.execute();
                    m_commandRegistry.add(singleChangeCommand);
                }
            }
        }
    }

    @Override
    public boolean canUndo() {
        if (m_templatesToChange.isEmpty() || m_commandRegistry == null || m_commandRegistry.isEmpty()) {
            return false;
        }
        return m_commandRegistry.canUndo();
    }

    @Override
    public void undo() {
        LOGGER.debug("Undo: Reverting metanode links (" + m_commandRegistry.size() + " metanode(s))");
        m_commandRegistry.undo();
    }

    @Override
    public boolean canRedo() {
        if (m_templatesToChange.isEmpty() || m_commandRegistry == null || m_commandRegistry.isEmpty()) {
            return false;
        }
        return m_commandRegistry.canRedo();
    }

    @Override
    public void redo() {
        LOGGER.debug("Redo: Reverting the undo of metanode links (" + m_commandRegistry.size() + " metanode(s))");
        m_commandRegistry.redo();
    }
}
