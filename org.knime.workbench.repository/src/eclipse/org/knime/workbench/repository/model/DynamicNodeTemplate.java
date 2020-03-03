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
 *   Mar 20, 2012 (morent): created
 */

package org.knime.workbench.repository.model;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.extension.NodeSetFactoryExtension;

/**
 * A node template for dynamic nodes. Additional to the node factory class, a
 * {@link NodeSetFactory} class and the node factory ID is needed, to restore
 * the underlying node.
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 *
 */
public class DynamicNodeTemplate extends NodeTemplate {

    /**
     * Separator to separate the node factory's class name from the node's in order to construct the id:
     * <code>&#60;node-factory class name&#62;#&#60;node name&#62;</code>.
     */
    private static final String NODE_NAME_SEP = "#";

    private final NodeSetFactoryExtension m_nodeSetFactoryExtension;

    private final Class<? extends NodeFactory<? extends NodeModel>> m_factoryClass;

    private final String m_factoryId;

    /**
     * Constructs a new DynamicNodeTemplate.
     *
     * @param nodeSetFactoryExtension the factory class
     * @param factoryClass the NodeFactory class (instantiation of it done via
     *            {@link NodeSetFactoryExtension#createNodeFactory(String)}.
     * @param factoryId The id of the NodeFactory, must not be <code>null</code>
     * @param name the name of this repository entry, must not be <code>null</code>
     * @param categoryPath category path as per {@link NodeSetFactory#getCategoryPath(String)}
     * @param nodeType type as per node's (runtime generated) factory xml descriptin.
     */
    public DynamicNodeTemplate(final NodeSetFactoryExtension nodeSetFactoryExtension,
        final Class<? extends NodeFactory<? extends NodeModel>> factoryClass,
        final String factoryId, final String name, final String categoryPath, final NodeType nodeType) {
        super(factoryClass.getCanonicalName() + NODE_NAME_SEP + name, name,
            nodeSetFactoryExtension.getPlugInSymbolicName(), categoryPath, nodeType);
        m_factoryClass = factoryClass;
        m_factoryId = factoryId;
        m_nodeSetFactoryExtension = nodeSetFactoryExtension;
    }

    /**
     * Creates a copy of the given object.
     *
     * @param copy the object to copy
     */
    protected DynamicNodeTemplate(final DynamicNodeTemplate copy) {
        super(copy);
        m_nodeSetFactoryExtension = copy.m_nodeSetFactoryExtension;
        m_factoryClass = copy.m_factoryClass;
        m_factoryId = copy.m_factoryId;
    }

    @Override
    public Class<? extends NodeFactory<? extends NodeModel>> getFactory() {
        return m_factoryClass;
    }

    @Override
    public NodeFactory<? extends NodeModel> createFactoryInstance() throws Exception {
        // exception is unexpected here as the constructor was called with a concrete instance already
        // (which for some reason we never re-use but create a new factory instead)
        return m_nodeSetFactoryExtension.createNodeFactory(m_factoryId).orElseThrow(//
            () -> new RuntimeException(
                String.format("Can't create node for id \"%s\" from node set factory extension %s", m_factoryId,
                    m_nodeSetFactoryExtension)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DynamicNodeTemplate)) {
            return false;
        }
        return getID().equals(((DynamicNodeTemplate)obj).getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getID().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRepositoryObject deepCopy() {
        return new DynamicNodeTemplate(this);
    }
}
