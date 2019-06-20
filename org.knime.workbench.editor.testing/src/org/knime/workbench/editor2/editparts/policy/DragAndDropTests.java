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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.junit.Test;
import org.knime.core.util.KnimeURIUtil;

import com.fasterxml.jackson.databind.JsonNode;

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

    /**
     * Tests that hub extension API calls return the proper response.
     *
     * @throws URISyntaxException
     */
    @SuppressWarnings("null")
    @Test
    public void testHubExtensionAPI() throws URISyntaxException {

        // test base extension
        JsonNode extensionEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(
            KnimeURIUtil.getExtensionEndpointURI(
                new URI("https://hubdev.knime.com/knime/extensions/org.knime.features.base.feature.group")),
            new HashMap<>());
        assertThat("Wrong result (Base)", extensionEntityInfo != null, is(true));
        assertThat("Name field missing (Base)", extensionEntityInfo.has("name"), is(true));
        assertThat("Wrong name (Base)", extensionEntityInfo.get("name").asText(), is("KNIME Core"));

        assertThat("Symbolic name field missing (Base)", extensionEntityInfo.has("symbolicName"), is(true));
        assertThat("Wrong symbolic name (Base)", extensionEntityInfo.get("symbolicName").asText(),
            is("org.knime.features.base.feature.group"));

        // test seqan extension
        extensionEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(
            KnimeURIUtil.getExtensionEndpointURI(
                new URI("https://hubdev.knime.com/knime/extensions/de.seqan.seqanngstoolbox.feature.group")),
            new HashMap<>());
        assertThat("Wrong result (Seqan)", extensionEntityInfo != null, is(true));
        assertThat("Name field missing (Seqan)", extensionEntityInfo.has("name"), is(true));
        assertThat("Wrong name (Seqan)", extensionEntityInfo.get("name").asText(), is("SeqAn NGS ToolBox"));

        assertThat("Symbolic name field missing (Seqan)", extensionEntityInfo.has("symbolicName"), is(true));
        assertThat("Wrong symbolic name (Seqan)", extensionEntityInfo.get("symbolicName").asText(),
            is("de.seqan.seqanngstoolbox.feature.group"));

        // test non-existent extension
        extensionEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(
            KnimeURIUtil.getExtensionEndpointURI(
                new URI("https://hubdev.knime.com/knime/extensions/de.seqan.seqanngstoolbox.feature.group2")),
            new HashMap<>());

        assertThat("Wrong result (non-existent extension)", extensionEntityInfo == null, is(true));
    }

    /**
     * Tests that hub node API calls return the proper response.
     *
     * @throws URISyntaxException
     */
    @SuppressWarnings("null")
    @Test
    public void testHubNodeAPI() throws URISyntaxException {
        final HashMap<String, String> query = new HashMap<>();
        query.put("details", "full");

        // test row-filter
        JsonNode nodeEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(KnimeURIUtil
            .getNodeEndpointURI(new URI("https://hubdev.knime.com/knime/extensions/ignored/latest/org.knime."
                + "base.node.preproc.filter.row.RowFilterNodeFactory")),
            query);

        assertThat("Wrong result (Row Filter)", nodeEntityInfo != null, is(true));

        assertThat("Bundle information field missing " + "(Row Filter)", nodeEntityInfo.has("bundleInformation"),
            is(true));
        assertThat("Wrong bundle name (Row Filter)", nodeEntityInfo.get("bundleInformation") != null, is(true));

        JsonNode bundleInfo = nodeEntityInfo.get("bundleInformation");

        assertThat("Feature name field missing (Row Filter)", bundleInfo.has("featureName"), is(true));
        assertThat("Wrong feature name (Row Filter)", bundleInfo.get("featureName").asText(), is("KNIME Core"));

        assertThat("Feature symbolic name field missing (Row Filter)", bundleInfo.has("featureSymbolicName"), is(true));
        assertThat("Wrong feature symbolic name (Row Filter)", bundleInfo.get("featureSymbolicName").asText(),
            is("org.knime.features.base.feature.group"));

        assertThat("Factory name field missing (Row Filter)", nodeEntityInfo.has("factoryName"), is(true));
        assertThat("Wrong factory name (Row Filter)", nodeEntityInfo.get("factoryName").asText(),
            is("org.knime.base.node.preproc.filter.row.RowFilterNodeFactory"));

        assertThat("Title field missing (Row Filter)", nodeEntityInfo.has("title"), is(true));
        assertThat("Wrong title (Row Filter)", nodeEntityInfo.get("title").asText(), is("Row Filter"));

        // test dynamic node weka
        nodeEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(KnimeURIUtil
            .getNodeEndpointURI(new URI("https://hubdev.knime.com/knime/extensions/ignored/latest/org.knime.ext."
                + "weka37.classifier.WekaClassifierNodeFactory:ed021845")),
            query);

        assertThat("Wrong result (Weka J48)", nodeEntityInfo != null, is(true));

        assertThat("Bundle information field missing (Weka J48)", nodeEntityInfo.has("bundleInformation"), is(true));
        assertThat("Wrong bundle name (Weka J48)", nodeEntityInfo.get("bundleInformation") != null, is(true));

        bundleInfo = nodeEntityInfo.get("bundleInformation");

        assertThat("Feature name field missing (Weka J48)", bundleInfo.has("featureName"), is(true));
        assertThat("Wrong feature name (Weka J48)", bundleInfo.get("featureName").asText(),
            is("KNIME Weka Data Mining Integration (3.7)"));

        assertThat("Feature symbolic name field missing (Weka J48)", bundleInfo.has("featureSymbolicName"), is(true));
        assertThat("Wrong feature symbolic name (Weka J48)", bundleInfo.get("featureSymbolicName").asText(),
            is("org.knime.features.ext.weka_3.7.feature.group"));

        assertThat("Factory name field missing (Weka J48)", nodeEntityInfo.has("factoryName"), is(true));
        assertThat("Wrong factory name (Weka J48)", nodeEntityInfo.get("factoryName").asText(),
            is("org.knime.ext.weka37.classifier.WekaClassifierNodeFactory:ed021845"));

        assertThat("Title field missing (Weka J48)", nodeEntityInfo.has("title"), is(true));
        assertThat("Wrong title (Weka J48)", nodeEntityInfo.get("title").asText(), is("J48 (3.7)"));

        // test non-existent node
        nodeEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(KnimeURIUtil.getNodeEndpointURI(
            new URI("https://hubdev.knime.com/knime/extensions/weird/latest/NOTREALLYANODE")), query);

        assertThat("Wrong result (non-existent node)", nodeEntityInfo == null, is(true));
    }

    /**
     * Tests that hub node API calls return the proper response.
     *
     * @throws URISyntaxException
     */
    @SuppressWarnings("null")
    @Test
    public void testHubObjectAPI() throws URISyntaxException {

        // test component
        JsonNode objectEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(KnimeURIUtil.getObjectEntityEndpointURI(
            new URI("https://hubdev.knime.com/knime/space/Examples/02_ETL_Data_Manipulation/01_Filtering/"
                + "07_Four_Techniques_Outlier_Detection/_Templates/MapViz"),
            false), new HashMap<>());

        assertThat("Wrong result (MapViz)", objectEntityInfo != null, is(true));

        assertThat("Type information field missing (MapViz)", objectEntityInfo.has("type"), is(true));
        assertThat("Wrong type name (MapViz)", objectEntityInfo.get("type").asText(), is("WorkflowTemplate"));

        // test workflow
        objectEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(KnimeURIUtil.getObjectEntityEndpointURI(new URI(
            "https://hubdev.knime.com/knime/space/Examples/04_Analytics/13_Meta_Learning/02_Learning_a_Random_Forest"),
            false), new HashMap<>());

        assertThat("Wrong result (Learning Random Forest)", objectEntityInfo != null, is(true));

        assertThat("Type information field missing (Learning Random Forest)", objectEntityInfo.has("type"), is(true));
        assertThat("Wrong type name (Learning Random Forest)", objectEntityInfo.get("type").asText(), is("Workflow"));

        // test workflow group
        objectEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(
            KnimeURIUtil.getObjectEntityEndpointURI(
                new URI("https://hubdev.knime.com/knime/space/Examples/04_Analytics/13_Meta_Learning"), false),
            new HashMap<>());

        assertThat("Wrong result (Meta_Learning)", objectEntityInfo != null, is(true));

        assertThat("Type information field missing (Meta_Learning)", objectEntityInfo.has("type"), is(true));
        assertThat("Wrong type name (Meta_Learning)", objectEntityInfo.get("type").asText(), is("WorkflowGroup"));

        // test non-existent object
        objectEntityInfo = NewWorkflowContainerEditPolicy.callHubApi(KnimeURIUtil.getObjectEntityEndpointURI(
            new URI("https://hubdev.knime.com/knime/space/Examples/02_ETL_Data_Manipulation/01_Filtering/"
                + "07_Four_Techniques_Outlier_Detection/_Templates/MapViz23493"),
            false), new HashMap<>());

        assertThat("Wrong result (non-existent object)", objectEntityInfo == null, is(true));
    }
}
