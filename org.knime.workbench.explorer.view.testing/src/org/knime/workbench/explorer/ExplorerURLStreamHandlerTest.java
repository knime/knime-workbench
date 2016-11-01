/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   13.09.2016 (thor): created
 */
package org.knime.workbench.explorer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.ExplorerURLStreamHandler.ExplorerURLConnection;

/**
 * Testcases for {@link ExplorerURLStreamHandler}.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class ExplorerURLStreamHandlerTest {
    /**
     * Exception expected by some testcases.
     */
    @Rule
    public ExpectedException m_expectedException = ExpectedException.none();

    private ExplorerURLStreamHandler m_handler = new ExplorerURLStreamHandler();

    /**
     * Cleanup after each test.
     *
     * @throws Exception if an error occurs
     */
    @After
    public void cleanup() throws Exception {
        while (true) {
            try {
                NodeContext.removeLastContext();
            } catch (IllegalStateException ex) {
                break;
            }
        }

        Collection<NodeID> workflows =
            WorkflowManager.ROOT.getNodeContainers().stream().map(nc -> nc.getID()).collect(Collectors.toList());
        workflows.stream().forEach(id -> WorkflowManager.ROOT.removeProject(id));
    }

    /**
     * Checks if wrong protocols are rejected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testWrongProtocol() throws Exception {
        URL url = new URL("http://www.knime.org/");

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("Unexpected protocol"));
        m_handler.openConnection(url);
    }

    /**
     * Checks if a missing {@link NodeContext} is handled correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testNoNodeContext() throws Exception {
        URL url = new URL("knime://knime.workflow/workflow.knime");

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("No context"));
        m_handler.openConnection(url);
    }

    /**
     * Checks if a missing {@link WorkflowContext} is handled correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testNoWorkflowContext() throws Exception {
        URL url = new URL("knime://knime.workflow/workflow.knime");

        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("does not have a context"));
        m_handler.openConnection(url);
    }

    /**
     * Checks if workflow-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeLocal() throws Exception {
        URL url = new URL("knime://knime.workflow/workflow.knime");

        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(currentLocation.getParent().toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        Path expectedPath = currentLocation.resolve("workflow.knime");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));

        // path outside the workflow
        url = new URL("knime://knime.workflow/../test.txt");
        conn = m_handler.openConnection(url);
        expectedPath = currentLocation.resolve("..").resolve("test.txt");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));
    }

    /**
     * Checks if workflow-relative knime-URLs are resolved correctly for old executors.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeOriginalLocation() throws Exception {
        URL url = new URL("knime://knime.workflow/workflow.knime");

        // original location == current location
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setOriginalLocation(currentLocation.toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        Path expectedPath = currentLocation.resolve("workflow.knime");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));

        // original location != current location
        Path originalLocation = KNIMEConstants.getKNIMETempPath().resolve("original location");
        fac.setOriginalLocation(originalLocation.toFile());
        ch.setWorkflowContext(fac.createContext());
        wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        conn = m_handler.openConnection(url);
        expectedPath = currentLocation.resolve("workflow.knime");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));

        // path outside the workflow
        url = new URL("knime://knime.workflow/../test.txt");
        conn = m_handler.openConnection(url);
        expectedPath = originalLocation.resolve("..").resolve("test.txt");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));
    }

    /**
     * Checks that workflow-relative knime-URLs do not leave the mount point.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeObeyMountpointRoot() throws Exception {
        URL url = new URL("knime://knime.workflow/../../test.txt");

        // original location == current location
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(currentLocation.getParent().toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("Leaving the mount point is not allowed"));
        m_handler.openConnection(url);
    }

    /**
     * Checks if workflow-relative knime-URLs are resolved correctly to server addresses.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeToServer() throws Exception {
        URL url = new URL("knime://knime.workflow/workflow.knime");
        URI baseUri = new URI("http://localhost:8080/knime");

        // original location == current location
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setOriginalLocation(currentLocation.toFile());
        fac.setRemoteAddress(baseUri, "workflow");
        fac.setRemoteAuthToken("token");
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        // links inside the workflow must stay local links to the workflow copy
        URLConnection conn = m_handler.openConnection(url);
        URI expectedUri = new URI(currentLocation.toUri().toString() + "/workflow.knime").normalize();
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedUri));
        // we cannot check whether the Authorization header is set correctly, because HttpURLConnection
        // doesn't return it for security reasons

        // path outside the workflow
        url = new URL("knime://knime.workflow/../test 1.txt");
        conn = m_handler.openConnection(url);
        expectedUri = new URI(baseUri.toString() + "/test%201.txt:data");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedUri));
    }


    /**
     * Checks if mountpoint-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountPointRelativeLocal() throws Exception {
        URL url = new URL("knime://knime.mountpoint/test.txt");

        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(currentLocation.getParent().toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        Path expectedPath = currentLocation.getParent().resolve("test.txt");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));
    }

    /**
     * Checks if mountpoint-relative knime-URLs are resolved correctly for old executors.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountpointRelativeOriginalLocation() throws Exception {
        URL url = new URL("knime://knime.mountpoint/test.txt");

        // original location == current location
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        Path otherMountpointRoot = KNIMEConstants.getKNIMETempPath().resolve("some other root");
        fac.setMountpointRoot(otherMountpointRoot.toFile());
        fac.setOriginalLocation(currentLocation.toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        Path expectedPath = otherMountpointRoot.resolve("test.txt");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));
    }

    /**
     * Checks that mountpoint-relative knime-URLs do not leave the mount point.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountpointRelativeObeyMountpointRoot() throws Exception {
        URL url = new URL("knime://knime.mountpoint/../test.txt");

        // original location == current location
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(currentLocation.getParent().toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("Leaving the mount point is not allowed"));
        m_handler.openConnection(url);
    }

    /**
     * Checks if mountpoint-relative knime-URLs are resolved correctly to server addresses.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountpointRelativeToServer() throws Exception {
        URL url = new URL("knime://knime.mountpoint/some where/outside.txt");
        URI baseUri = new URI("http://localhost:8080/knime");

        // original location == current location
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setOriginalLocation(currentLocation.toFile());
        fac.setRemoteAddress(baseUri, "workflow");
        fac.setRemoteAuthToken("token");
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        URI expectedUri = new URI(baseUri.toString() + "/some%20where/outside.txt:data");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedUri));
        // we cannot check whether the Authorization header is set correctly, because HttpURLConnection
        // doesn't return it for security reasons
    }

    /**
     * Check if URLs with a local mount point are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveLocalMountpointURL() throws Exception {
        assertThat("NodeContext not expected to be set at this point", NodeContext.getContext(), is((NodeContext)null));
        URL url = new URL("knime://LOCAL/test.txt");

        URLConnection conn = m_handler.openConnection(url);
        assertThat("Unexpected connection returned", conn, is(instanceOf(ExplorerURLConnection.class)));
    }

    /**
     * Check if URLs with a remote mount point are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveRemoteMountpointURL() throws Exception {
        URL url = new URL("knime://Server/some where/outside.txt");
        URI baseUri = new URI("https://localhost:8080/knime");

        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setOriginalLocation(currentLocation.toFile());
        fac.setRemoteAddress(baseUri, "workflow");
        fac.setRemoteAuthToken("token");
        fac.setRemoteMountId("Server");
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        URI expectedUri = new URI(baseUri.toString() + "/some%20where/outside.txt:data");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedUri));
    }


    /**
     * Check if URLs with a remote mount point are resolved correctly for old jobs that don't have a remote token
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveRemoteMountpointURLWithoutToken() throws Exception {
        URL url = new URL("knime://Server/some where/outside.txt");
        URI baseUri = new URI("https://localhost:8080/knime");

        Path mpRoot = KNIMEConstants.getKNIMETempPath().resolve("root");
        Path currentLocation = mpRoot.resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(mpRoot.toFile());
        fac.setOriginalLocation(currentLocation.toFile());
        fac.setRemoteAddress(baseUri, "workflow");
        fac.setRemoteMountId("Server");
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        URI expectedUri = new URI(mpRoot.toUri() + "/some%20where/outside.txt").normalize();
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedUri));
    }


    /**
     * Checks if node-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveNodeRelativeLocal() throws Exception {
        URL url = new URL("knime://knime.node/test.txt");

        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(currentLocation.getParent().toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        URLConnection conn = m_handler.openConnection(url);
        Path expectedPath = currentLocation.resolve("test.txt");
        assertThat("Unexpected resolved URL", conn.getURL().toURI(), is(expectedPath.toUri()));
    }

    /**
     * Checks that node-relative knime-URLs only work on saved workflows.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveNodeRelativeLocalNotSaved() throws Exception {
        URL url = new URL("knime://knime.node/test.txt");

        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(currentLocation.getParent().toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        NodeContext.pushContext(wfm);

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("Workflow must be saved"));
        m_handler.openConnection(url);
    }

    /**
     * Checks if node-relative knime-URLs do not leave the workflow.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveNodeRelativeLocalDontLeaveWorkflow() throws Exception {
        URL url = new URL("knime://knime.node/../test.txt");

        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        WorkflowCreationHelper ch = new WorkflowCreationHelper();
        WorkflowContext.Factory fac = new WorkflowContext.Factory(currentLocation.toFile());
        fac.setMountpointRoot(currentLocation.getParent().toFile());
        ch.setWorkflowContext(fac.createContext());
        WorkflowManager wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), ch);
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("Leaving the workflow is not allowed"));
        m_handler.openConnection(url);
    }
}
