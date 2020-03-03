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
 * -------------------------------------------------------------------
 *
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

import java.util.Objects;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeModel;

/**
 * Class that realizes a (contributed) node in the repository tree. This is used
 * as a "template" for actual instances of a node in the workflow editor.
 *
 * Note: The type constants *must* match those defined in the "nodes"- extension
 * point (Node.exsd).
 *
 * TODO introduce new fields: provider, url, license-tag (free/commercial) ...
 * ???
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class NodeTemplate extends AbstractNodeTemplate {

    private NodeType m_type;

    /**
     * Creates a copy of the given node template.
     *
     * @param copy the object to copy
     */
    protected NodeTemplate(final NodeTemplate copy) {
        super(copy);
        this.m_type = copy.m_type;
    }

    /**
     * Constructs a new node template.
     *
     * @param id the (unique) id of the node template (fully qualified class name (fqcn) or "fqcn#Some Name" for dynamic
     *            nodes)
     * @param name a human-readable name for this node
     * @param contributingPlugin the contributing plug-in's ID
     * @param categoryPath category path as per ext point.
     * @param nodeType node type as per XYZNodeFactory.xml
     */
    NodeTemplate(final String id, final String name, final String contributingPlugin,
        final String categoryPath, final NodeType nodeType) {
        super(id, name, contributingPlugin, categoryPath);
        m_type = nodeType;
    }

    /**
     * @return Returns the factory.
     */
    public abstract Class<? extends NodeFactory<? extends NodeModel>> getFactory();

    /**
     * @return an instance of the factory.
     * @throws Exception if the creation of the factory instance fails
     */
    public abstract NodeFactory<? extends NodeModel> createFactoryInstance() throws Exception;

    /**
     * @return Returns the type.
     */
    public final NodeType getType() {
        return m_type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(m_type);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof NodeTemplate)) {
            return false;
        }
        return Objects.equals(m_type, ((NodeTemplate)obj).m_type);
    }

    @Override
    public String toString() {
        return getID();
    }
}
