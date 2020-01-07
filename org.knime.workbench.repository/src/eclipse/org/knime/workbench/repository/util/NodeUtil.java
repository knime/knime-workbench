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
 *   Jan 17, 2019 (awalter): created
 */
package org.knime.workbench.repository.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * Utility class for working with nodes
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
public final class NodeUtil {

    /**
     * Checks if the given {@link NodeTemplate} is streamable.
     *
     * @param nodeTemplate the {@link NodeTemplate} to check if it is streamable
     * @return {@code true} if the given node is streamable, {@code false} otherwise
     * @throws Exception thrown if the node cannot be instantiated
     */
    public static boolean isStreamable(final NodeTemplate nodeTemplate) throws Exception {
        return isStreamable(nodeTemplate.getFactory(), () -> {
            try {
                return nodeTemplate.createFactoryInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Checks if the given {@link NodeFactory} is streamable.
     *
     * @param nodeFactory the {@code NodeFactory<? extends NodeModel>} to check if it is streamable
     * @return {@code true} if the given node is streamable, {@code false} otherwise
     * @throws Exception thrown if the node cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    public static boolean isStreamable(final NodeFactory<? extends NodeModel> nodeFactory) throws Exception {
        return isStreamable((Class<? extends NodeFactory<? extends NodeModel>>)nodeFactory.getClass(),
            () -> nodeFactory);
    }

    /**
     * Checks if the given {@link Node} is streamable.
     *
     * @param node the {@code Node} to check if it is streamable
     * @return {@code true} if the given node is streamable, {@code false} otherwise
     * @throws Exception if the node cannot be instantiated
     */
    public static boolean isStreamable(final Node node) throws Exception {
        return isStreamable(node.getNodeModel().getClass());
    }

    private static boolean isStreamable(final Class<? extends NodeFactory<? extends NodeModel>> nodeFactoryClass,
        final Supplier<NodeFactory<? extends NodeModel>> nodeFactoryCreator)
        throws NoSuchMethodException, SecurityException {
        Type genericSuperclass = nodeFactoryClass.getGenericSuperclass();
        Class<?> nodeModelClass = null;

        // try inferring node model class from the node factory's generic parameter (exclusively by reflection)
        if (genericSuperclass instanceof ParameterizedType) {
            Type type = ((ParameterizedType)nodeFactoryClass.getGenericSuperclass()).getActualTypeArguments()[0];
            if (type instanceof ParameterizedType) {
                nodeModelClass = (Class<?>)((ParameterizedType)type).getRawType();
            } else {
                nodeModelClass = (Class<?>)type;
            }

            //some node factory implementations are parameterized, but not with a node model
            if (!NodeModel.class.isAssignableFrom(nodeModelClass)) {
                nodeModelClass = null;
            }
        }

        //fall back if node model class couldn't be determined via reflection
        //-> create a node model instance
        if (nodeModelClass == null) {
            Node n = new Node((NodeFactory<NodeModel>)nodeFactoryCreator.get());
            nodeModelClass = n.getNodeModel().getClass();
            n.cleanup();
        }
        return isStreamable(nodeModelClass);
    }

    private static boolean isStreamable(final Class<?> nodeModelClass)
        throws NoSuchMethodException, SecurityException {
        Method m = nodeModelClass.getMethod("createStreamableOperator", PartitionInfo.class, PortObjectSpec[].class);
        return m.getDeclaringClass() != NodeModel.class;
    }

}
