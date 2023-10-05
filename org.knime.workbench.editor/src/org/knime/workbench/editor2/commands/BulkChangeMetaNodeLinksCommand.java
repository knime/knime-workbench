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
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.dialogs.MessageDialog;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.TemplateType;
import org.knime.core.node.workflow.NodeContainerParent;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.workbench.editor2.actions.BulkChangeMetaNodeLinksAction.LinkChangeAction;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProvider.LinkType;

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

    private final LinkChangeAction m_linkChangeAction;

    private final LinkType m_oldLinkType;

    private LinkType m_newLinkType = LinkType.None;

    private final URI m_oldLinkURI;

    private URI m_newLinkURI = null;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager containing the links to be changed.
     * @param action the type of link-changing action
     * @param templates NodeContainerTemplates to be changed
     */
    public BulkChangeMetaNodeLinksCommand(final WorkflowManager manager, final LinkChangeAction action,
        final List<NodeContainerTemplate> templates) {
        super(manager);
        m_templatesToChange = templates;
        m_linkChangeAction = action;

        // determining existing (old) link properties
        if (templates.isEmpty()) {
            throw new IllegalArgumentException("Metanode templates to change must not be empty!");
        }
        var nc = m_templatesToChange.get(0);
        m_oldLinkURI = nc.getTemplateInformation().getSourceURI();
        m_oldLinkType = resolveLinkType(m_oldLinkURI);
        m_templateType = nc instanceof SubNodeContainer ? TemplateType.SubNode : TemplateType.MetaNode;
    }

    /**
     * Resolves a URI to a specific {@link LinkType}.
     *
     * @param link input URI
     * @return resolved link type
     */
    public static LinkType resolveLinkType(final URI link) {
        var linkType = LinkType.None;
        try {
            if (ResolverUtil.isMountpointRelativeURL(link)) {
                linkType = LinkType.MountpointRelative;
            } else if (ResolverUtil.isWorkflowRelativeURL(link)) {
                linkType = LinkType.WorkflowRelative;
            } else {
                linkType = LinkType.Absolute;
            }
        } catch (ResourceAccessException e) {
            LOGGER.error("Unable to resolve current link to template " + link + ": " + e.getMessage(), e);
        }
        return linkType;
    }

    /**
     * Generates a new link URI based on a given URI and a new link type. Does not change the link URI of the
     * contextNode!
     *
     * @param contextNode node used for resolving the template location
     * @param oldURI URI with the old link type
     * @param newLinkType new link type to be set
     * @return new URI with having the new link type
     */
    public static URI changeLinkType(final NodeContainerParent contextNode, final URI oldURI,
        final LinkType newLinkType) {
        URI newURI = null;
        NodeContext.pushContext(contextNode);
        try {
            var targetFile = ResolverUtil.resolveURItoLocalFile(oldURI);
            var targetfs = ExplorerFileSystem.INSTANCE.fromLocalFile(targetFile);
            newURI = AbstractContentProvider.createMetanodeLinkUri(contextNode, targetfs, newLinkType);
        } catch (ResourceAccessException | URISyntaxException | CoreException e) {
            LOGGER.error("Unable to resolve shared component URI " + oldURI + ": " + e.getMessage(), e);
        } finally {
            NodeContext.removeLastContext();
        }
        return newURI;
    }

    /**
     * Sets the new link type.
     *
     * @param linkType
     */
    public void setLinkType(final LinkType linkType) {
        m_newLinkType = linkType;
    }

    /**
     * Sets the new link URI.
     *
     * @param uri
     */
    public void setURI(final URI uri) {
        m_newLinkURI = uri;
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
        // link type change action cannot be executed
        if ((m_linkChangeAction == LinkChangeAction.TYPE_CHANGE
            && (m_oldLinkType == LinkType.None || m_newLinkType == LinkType.None))) {
            return false;
        }
        // link URI change action cannot be executed
        if ((m_linkChangeAction == LinkChangeAction.URI_CHANGE && (m_oldLinkURI == null || m_newLinkURI == null))) {
            return false;
        }
        var wfm = getHostWFM();
        return m_templatesToChange.stream().allMatch(nct -> wfm.findNodeContainer(nct.getID()) != null);
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        if (m_linkChangeAction == LinkChangeAction.TYPE_CHANGE) {
            doLinkTypeBulkChange();
        } else if (m_linkChangeAction == LinkChangeAction.URI_CHANGE) {
            doLinkURIBulkChange();

            // asking if all nodes should be updated subsequently
            var shell = SWTUtilities.getActiveShell();
            if (MessageDialog.openQuestion(shell, "Change Links",
                "Do you want to update all nodes where the links have been changed?\n"
                    + "The update will be fetched from \"" + m_newLinkURI + "\".")) {
                var templateIds = m_templatesToChange.stream().map(NodeContainerTemplate::getID).collect(Collectors.toList());
                var updateCommand = new UpdateMetaNodeLinkCommand(getHostWFM(), templateIds.toArray(new NodeID[0]));
                updateCommand.execute();
                m_commandRegistry.add(updateCommand);
                MessageDialog.openInformation(shell, "Change Links", "All selected nodes have been updated.");
            }
        }
    }

    /**
     * Uses the given, new link type to change the old URI into new one. After that, calls the bulk link URI change
     * method for setting this new URI.
     */
    private void doLinkTypeBulkChange() {
        // we can choose the first NodeContainerTemplate because it only acts as context node
        m_newLinkURI = changeLinkType((NodeContainerParent)m_templatesToChange.get(0), m_oldLinkURI, m_newLinkType);
        doLinkURIBulkChange();
    }

    /**
     * Depending on the TemplateType, this method changes the template link URI of all
     * {@link BulkChangeMetaNodeLinksCommand#m_templatesToChange} to the
     * {@link BulkChangeMetaNodeLinksCommand#m_newLinkURI}. Stores the executed commands in the command registry.
     */
    private void doLinkURIBulkChange() {
        if (m_templateType == TemplateType.SubNode) {
            for (NodeContainerTemplate template : m_templatesToChange) {
                var singleChangeCommand =
                    new ChangeSubNodeLinkCommand(getHostWFM(), (SubNodeContainer)template, m_oldLinkURI, m_newLinkURI);
                if (singleChangeCommand.canExecute()) {
                    singleChangeCommand.execute();
                    m_commandRegistry.add(singleChangeCommand);
                }
            }
        } else if (m_templateType == TemplateType.MetaNode) {
            for (NodeContainerTemplate template : m_templatesToChange) {
                var singleChangeCommand =
                    new ChangeMetaNodeLinkCommand(getHostWFM(), (WorkflowManager)template, m_oldLinkURI, m_newLinkURI);
                if (singleChangeCommand.canExecute()) {
                    singleChangeCommand.execute();
                    m_commandRegistry.add(singleChangeCommand);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_templatesToChange.isEmpty() || m_commandRegistry == null || m_commandRegistry.isEmpty()) {
            return false;
        }
        return m_commandRegistry.canUndo();
    }

    /** {@inheritDoc} */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Reverting metanode links (" + m_commandRegistry.size() + " metanode(s))");
        m_commandRegistry.undo();
    }

    /** {@inheritDoc} */
    @Override
    public boolean canRedo() {
        if (m_templatesToChange.isEmpty() || m_commandRegistry == null || m_commandRegistry.isEmpty()) {
            return false;
        }
        return m_commandRegistry.canRedo();
    }

    /** {@inheritDoc} */
    @Override
    public void redo() {
        LOGGER.debug("Redo: Reverting the undo of metanode links (" + m_commandRegistry.size() + " metanode(s))");
        m_commandRegistry.redo();
    }
}
