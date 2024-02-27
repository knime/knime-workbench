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
 *   Feb 26, 2024 (leonard.woerteler): created
 */
package org.knime.workbench.editor2.commands;

import java.net.URI;
import java.time.Instant;

import org.apache.commons.lang3.Functions;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * GEF Command for changing the link (back to its template) of a sub node or metanode.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
abstract class ChangeTemplateLinkCommand extends AbstractKNIMECommand {

    record Link(URI linkUri, Instant lastModified) {}

    Link m_oldLink;
    Link m_newLink;

    // must not keep NodeContainer here to enable undo/redo, the node container instance may change if deleted and
    // the delete is undone
    final NodeID m_linkedContainerID;

    /**
     * @param hostWFM parent workflow
     * @param linkedContainerID node ID of the sub node or metanode
     * @param oldUri old link URI
     * @param oldLastModified old last-modified timestamp, {@code null} if timestamps should be left alone
     * @param newUri new link URI
     * @param newLastModified new last-modified timestamp, {@code null} if timestamps should be left alone
     */
    ChangeTemplateLinkCommand(final WorkflowManager hostWFM, final NodeID linkedContainerID, final URI oldUri,
        final Instant oldLastModified, final URI newUri, final Instant newLastModified) {
        super(hostWFM);
        m_linkedContainerID = linkedContainerID;
        m_oldLink = new Link(oldUri, oldLastModified);
        m_newLink = new Link(newUri, newLastModified);
    }

    /**
     * Set the modified link on the sub node or metanode.
     *
     * @param linkModifier function computing the new link information from the old one
     * @return {@code true} if the link info could be changed, {@code false} otherwise
     */
    abstract boolean setLink(final Functions.FailableFunction<MetaNodeTemplateInformation, MetaNodeTemplateInformation,
        InvalidSettingsException> linkModifier);

    @Override
    public final void execute() {
        if (!setLink(info -> info.createLinkWithUpdatedSource(m_newLink.linkUri(), m_newLink.lastModified()))) {
            m_oldLink = null; // disable undo
        }
    }

    @Override
    public final boolean canUndo() {
        return m_oldLink != null;
    }

    @Override
    public final boolean canExecute() {
        return m_newLink != null && m_linkedContainerID != null;
    }

    @Override
    public final void undo() {
        if (!setLink(info -> info.createLinkWithUpdatedSource(m_oldLink.linkUri(), m_oldLink.lastModified()))) {
            m_newLink = null;
        }
    }
}
