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
 */
package org.knime.workbench.repository.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.Test;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.DefaultNodeTemplate;

/**
 * Tests {@link NodeUtil}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeUtilTest {

    /**
     * Tests {@link NodeUtil#isStreamable(org.knime.workbench.repository.model.NodeTemplate)}.
     *
     * @throws Exception
     */
    @Test
    public void testIsStreamable() throws Exception {
        //a streamable source node
        checkIsStreamable("org.knime.base.node.io.filereader.FileReaderNodeFactory", true);

        //ordinary streamable node
        checkIsStreamable("org.knime.base.node.preproc.filter.row2.RowFilterNodeFactory", true);

        //non-streamable node
        checkIsStreamable("org.knime.base.node.preproc.transpose.TransposeTableNodeFactory", false);

        //non-streamable node without parameterized factory
        checkIsStreamable("org.knime.base.node.preproc.joiner.Joiner2NodeFactory", false);

        //a non-streamable dynamic node
        String factorySettings =
            "{\"name\":\"settings\",\"value\":{\"nodeDir\":{\"type\":\"string\",\"value\":\"org.knime.dynamic.js.base:nodes/:boxplot_v2\"}}}";
        checkIsStreamable("org.knime.dynamic.js.v30.DynamicJSNodeFactory", factorySettings, false);
    }

    private static void checkIsStreamable(final String nodeFactoryClassname, final boolean isExpectedToBeStreamable)
        throws Exception {
        checkIsStreamable(nodeFactoryClassname, null, isExpectedToBeStreamable);
    }

    private static void checkIsStreamable(final String nodeFactoryClassname, final String nodeFactorySettings,
        final boolean isExpectedToBeStreamable) throws Exception {
        NodeFactory<NodeModel> nodeFactory = RepositoryManager.loadNodeFactory(nodeFactoryClassname);
        if (nodeFactorySettings != null) {
            NodeSettings settings =
                JSONConfig.readJSON(new NodeSettings("settings"), new StringReader(nodeFactorySettings));
            nodeFactory.loadAdditionalFactorySettings(settings);
        }

        boolean isStreamable = NodeUtil.isStreamable(nodeFactory);
        isStreamable &= NodeUtil.isStreamable(new Node(nodeFactory));
        isStreamable &= NodeUtil
            .isStreamable(new DefaultNodeTemplate((Class<NodeFactory<? extends NodeModel>>)nodeFactory.getClass(),
                "TEST", "TEST", "/", nodeFactory.getType()));

        if (isExpectedToBeStreamable) {
            assertTrue("node expected to be stremable", isStreamable);
        } else {
            assertFalse("node not expected to be streamable", isStreamable);
        }
    }

    /**
     * Prints all node (factory and model) that need to be instantiated in order to determine whether they are
     * streamable or not.
     *
     * Might be useful in the future to investigate problems while trying to determine streamable-state of a node.
     */
    /*
    @Test
    public void testPrintFallbackNodeModels() {
        Root root = RepositoryManager.INSTANCE.getRoot();
        iterateNodes(root, nt -> {
            try {
                NodeUtil.isStreamable(nt.getFactory(), () -> {
                    try {
                        NodeFactory<? extends NodeModel> factory = nt.createFactoryInstance();


                        Class<? extends NodeModel> nodeModelClass =
                            new Node((NodeFactory<NodeModel>)factory).getNodeModel().getClass();

                        boolean implementsOnDispose = Arrays.stream(nodeModelClass.getDeclaredMethods())
                            .anyMatch(m -> m.getName().equals("onDispose"));

                        System.out.println("factory: " + factory.getClass().getCanonicalName() + "; model: "
                            + nodeModelClass.getCanonicalName() + "; implOnDispose: " + implementsOnDispose);

                        return factory;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void iterateNodes(final IRepositoryObject parent, final Consumer<NodeTemplate> callback) {
        if (parent instanceof IContainerObject) {
            IRepositoryObject[] children = ((IContainerObject)parent).getChildren();
            for (IRepositoryObject child : children) {
                iterateNodes(child, callback);
            }
            return;
        } else if (parent instanceof NodeTemplate) {
            NodeTemplate nodeTemplate = (NodeTemplate)parent;
            callback.accept(nodeTemplate);
        }
    }
    */
}
