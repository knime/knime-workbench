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
 *   Jun 19, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.workbench.editor2.editparts.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

/**
 * Test the drag and drop functionality of {@link NewWorkflowContainerEditPolicy}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class DragAndDropTests {

    /**
     * Tests that already installed bundles are correctly identified.
     */
    @Test
    public void testBundleInstalled() {
        assertThat(NewWorkflowContainerEditPolicy.isFeatureInstalled("org.knime.features.base.feature.group"),
            is(true));
        assertThat(NewWorkflowContainerEditPolicy.isFeatureInstalled("org.knime.features.base"), is(true));

        assertThat(
            NewWorkflowContainerEditPolicy.isFeatureInstalled("org.knime.feature.group.features.base.feature.group"),
            is(false));
        assertThat(NewWorkflowContainerEditPolicy.isFeatureInstalled("org.knime.features.base.foo"), is(false));
        assertThat(NewWorkflowContainerEditPolicy.isFeatureInstalled("org.knime.features.base.foo.feature.group"),
            is(false));
    }

//    /**
//     * Tests that the proper command is returned when a node is dragged and dropped into KAP.
//     */
//    @Test
//    public void testValidNodeDnDCommand() {
//        Command com = execCommand("https://hubdev.knime.com/knime/extensions/ignored/latest/"
//            + "org.knime.base.node.preproc.filter.row.RowFilterNodeFactory");
//        assertThat("Wrong command", com != null && com instanceof CreateNodeCommand, is(true));
//    }
//
//    /**
//     * Tests that the proper command is returned when a component is dragged and dropped into KAP.
//     */
//    @Test
//    public void testValidComponentDnDCommand() {
//        final Command com = execCommand("https://hubdev.knime.com/knime/space/Examples/02_ETL_Data_Manipulation/"
//            + "01_Filtering/07_Four_Techniques_Outlier_Detection/_Templates/MapViz");
//        assertThat("Wrong command", com != null && com instanceof CreateMetaNodeTemplateCommand, is(true));
//    }
//
//    /**
//     * Tests that the proper command is returned when a workflow is dragged and dropped into KAP.
//     */
//    @Test
//    public void testValidWorkflowDnDCommand() {
//        final Command com = execCommand(
//            "https://hubdev.knime.com/knime/space/Examples/04_Analytics/13_Meta_Learning/02_Learning_a_Random_Forest");
//        // TODO: has to be changed once we can DnD workflows
//        assertThat("Wrong command", com == null, is(true));
//    }
//
//    /**
//     * Tests that the proper command is returned when a workflowgroup is dragged and dropped into KAP.
//     */
//    @Test
//    public void testValidWorkflowGroupDnDCommand() {
//        final Command com = execCommand("https://hubdev.knime.com/knime/space/Examples/04_Analytics/13_Meta_Learning");
//        // TODO: has to be changed once we can DnD workflowgroups
//        assertThat("Wrong command", com == null, is(true));
//    }
//
//    private Command execCommand(final String url) {
//        final WorkflowRootEditPart workflowRootEditPart = new WorkflowRootEditPart();
//        workflowRootEditPart.setModel(WorkflowManagerWrapper.wrap(WorkflowManager.ROOT));
//        final NewWorkflowContainerEditPolicy contEditPolicy = new NewWorkflowContainerEditPolicy();
//        contEditPolicy.setHost(workflowRootEditPart);
//        return contEditPolicy.getCommand(createDropRequest(url));
//    }
//
//    private CreateDropRequest createDropRequest(final String url) {
//        final CreateDropRequest req = new CreateDropRequest();
//        final CreationFactory urlFac = new CreationFactory() {
//
//            @Override
//            public Object getNewObject() {
//                try {
//                    return new URL(url);
//                } catch (MalformedURLException e) {
//                }
//                return null;
//            }
//
//            @Override
//            public Object getObjectType() {
//                return URL.class;
//            }
//
//        };
//        req.setFactory(urlFac);
//        req.setRequestType(RequestType.CREATE);
//        req.setType("create child");
//        req.setLocation(new Point());
//        return req;
//    }

}
