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
 *   13.09.2016 (thor): created
 */
package org.knime.workbench.explorer;

import static org.hamcrest.core.StringContains.containsString;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.knime.core.internal.knimeurl.ExplorerURLStreamHandler;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.ui.node.workflow.RemoteWorkflowContext;
import org.knime.core.ui.node.workflow.WorkflowContextUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.util.auth.SimpleTokenAuthenticator;

/**
 * Testcases for {@link ExplorerURLStreamHandler} if used on the context of {@link WorkflowContextUI} and therewith
 * {@link RemoteWorkflowContext}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class ExplorerURLStreamHandlerForWorkflowContextUITest {

    /**
     * Exception expected by some testcases.
     */
    @Rule
    public ExpectedException m_expectedException = ExpectedException.none();

    private ExplorerURLStreamHandler m_handler = new ExplorerURLStreamHandler();


    private Set<NodeID> m_staticWFMs;

    /**
     * Remember the WFMS that were known before any test ran. Don't touch them on {@link #cleanup()}.
     */
    @Before
    public void indexWFMsBefore() {
        m_staticWFMs =
                WorkflowManager.ROOT.getNodeContainers().stream().map(NodeContainer::getID).collect(Collectors.toSet());
    }

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

        Collection<NodeID> workflows = WorkflowManager.ROOT.getNodeContainers().stream().map(nc -> nc.getID())
                .filter(id -> !m_staticWFMs.contains(id)).collect(Collectors.toList());
        workflows.stream().forEach(id -> WorkflowManager.ROOT.removeProject(id));
    }

    /**
     * Checks if a missing {@link WorkflowContextUI} is handled correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testNoWorkflowContext() throws Exception {
        URL url = new URL("knime://knime.workflow/workflow.knime");
        URI mountpointUri = new URI(
                "knime://knime-server-mountpoint/test?exec=8443aad7-e59e-4be1-b31b-4b287f5bf466&name=test%2B2019-01-02%2B09.57.19");

        RemoteWorkflowContext ctx = new RemoteWorkflowContext(null, null, "path", new SimpleTokenAuthenticator("token"),
            "mount id", mountpointUri, null, null);
        NodeContext.pushContext(createWorkflowManagerUIMock(ctx));

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(containsString("without a workflow context"));
        m_handler.openConnection(url);
    }

    /**
     * Checks that a workflow-relative URI pointing to a resource within the workflow cannot be resolved.
     *
     * @throws Exception
     */
    @Test
    public void testResolveWithinWorkflowRelativURIFails() throws Exception {
        URL url = new URL("knime://knime.workflow/some where/inside.txt");
        URI mountpointUri = new URI(
            "knime://knime-server-mountpoint/test?exec=8443aad7-e59e-4be1-b31b-4b287f5bf466&name=test%2B2019-01-02%2B09.57.19");

        final var contextV2 = WorkflowContextV2.builder() //
                .withServerJobExecutor(exec -> exec //
                    .withUserId("user") //
                    .withLocalWorkflowPath(Path.of("some where/inside.txt")) //
                    .withJobId(UUID.randomUUID()) //
                    .withRemoteExecutor("knime-server-mountpoint", "executor-version")) //
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:8080/knime"))
                    .withWorkflowPath("/some where/inside.txt")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("knime-server-mountpoint"))
                .build();

        RemoteWorkflowContext ctx = new RemoteWorkflowContext(contextV2, null, "path", new SimpleTokenAuthenticator("token"),
            "mount id", mountpointUri, null, null);
        NodeContext.pushContext(createWorkflowManagerUIMock(ctx));

        m_expectedException.expect(IOException.class);
        m_expectedException.expectMessage(
            containsString("Workflow relative URL points to a resource within a workflow. Not accessible."));
        m_handler.openConnection(url);
    }

    private WorkflowManagerUI createWorkflowManagerUIMock(final RemoteWorkflowContext ctx) {
        return (WorkflowManagerUI)Proxy.newProxyInstance(this.getClass().getClassLoader(),
            new Class[]{WorkflowManagerUI.class}, (proxy, method, args) -> {
                if (method.getName().equals("getContext")) {
                    return ctx;
                } else if (method.getName().equals("isProject")) {
                    return true;
                } else if (method.getName().equals("toString")) {
                    return "WorkflowManagerUI";
                } else {
                    throw new UnsupportedOperationException();
                }
            });
    }
}
