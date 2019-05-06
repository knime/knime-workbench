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
package org.knime.workbench.explorer.templates;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.knime.workbench.explorer.templates.NodeRepoSynchronizerTest.setIncludedPathsInPrefs;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 * Tests the {@link NodeRepoSyncSettings}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeRepoSyncSettingsTest {

    private MockedLocalServerContentProvider m_server;

    /**
     * @throws IOException
     */
    @Before
    public void setup() throws IOException {
        m_server = (MockedLocalServerContentProvider)ExplorerMountTable.mount("mockserver",
            MockedLocalServerContentProviderFactory.ID, null);
    }

    /**
     * Tests the {@link NodeRepoSyncSettings#hasServerConfiguredPaths(AbstractContentProvider)} and
     * {@link NodeRepoSyncSettings#getServerConfiguredPaths(AbstractContentProvider)} methods.
     *
     * @throws IOException
     */
    @Test
    public void testHasAndGetServerConfiguredPaths() throws IOException {
        assertFalse("expected to return an empty optional because not touched, yet",
            NodeRepoSyncSettings.getInstance().hasServerConfiguredPaths(m_server.getMountID()).isPresent());
        assertFalse(NodeRepoSyncSettings.getInstance().getServerConfiguredPaths(m_server).isPresent());

        //set 'activated' to false to make sure that it still works when deactivated
        NodeRepoSyncSettings.getInstance().setPreferences(false, new HashMap<>());
        NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);

        assertFalse("expected to return false because no paths configured",
            NodeRepoSyncSettings.getInstance().hasServerConfiguredPaths(m_server.getMountID()).get());
        assertFalse(NodeRepoSyncSettings.getInstance().getServerConfiguredPaths(m_server).isPresent());

        m_server.setServerConfiguredTemplatePaths(asList("foobar"));
        NodeRepoSyncSettings.getInstance().clearServerConfiguredPathsCache();
        NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);
        assertTrue("expected to return true because a path is configured",
            NodeRepoSyncSettings.getInstance().hasServerConfiguredPaths(m_server.getMountID()).get());
        assertThat("unexpected server paths returned",
            NodeRepoSyncSettings.getInstance().getServerConfiguredPaths(m_server).get(), is(asList("foobar")));
    }

    /**
     * Tests the {@link NodeRepoSyncSettings#getDefaultPaths(AbstractContentProvider)}-method.
     *
     * @throws IOException
     */
    @Test
    public void testGetDefaultPaths() throws IOException {
        assertFalse("expected to return an empty optional because not touched, yet",
            NodeRepoSyncSettings.getInstance().getDefaultPaths(m_server).isPresent());

        NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);

        assertFalse("expected to return an empty optional because no paths configured",
            NodeRepoSyncSettings.getInstance().getDefaultPaths(m_server).isPresent());

        m_server.setDefaultTemplatePaths(asList("foo"));
        assertThat("unexpected default paths returned",
            NodeRepoSyncSettings.getInstance().getDefaultPaths(m_server).get(), is(asList("foo")));

        //check that server-configured paths take precedence
        m_server.setServerConfiguredTemplatePaths(asList("bar"));
        NodeRepoSyncSettings.getInstance().clearServerConfiguredPathsCache();
        NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);
        assertFalse("expected to return an empty optional because server paths take precedence",
            NodeRepoSyncSettings.getInstance().getDefaultPaths(m_server).isPresent());
    }

    /**
     * Tests the {@link NodeRepoSyncSettings#getAndCacheIncludedPathsForMountID(AbstractContentProvider)}-method.
     */
    @Test
    public void testGetAndCacheIncludedPathsForMountID() {
        m_server.setServerConfiguredTemplatePaths(asList("server"));
        m_server.setDefaultTemplatePaths(asList("default"));
        setIncludedPathsInPrefs(m_server, "custom");

        List<String> paths = NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);
        assertThat("unexpected path", paths, is(asList("server")));

        m_server.setServerConfiguredTemplatePaths(null);
        NodeRepoSyncSettings.getInstance().clearServerConfiguredPathsCache();
        paths = NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);
        assertThat("unexpected path", paths, is(asList("custom")));

        setIncludedPathsInPrefs(m_server);
        paths = NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);
        assertThat("unexpected path", paths, is(asList("default")));

        m_server.setDefaultTemplatePaths(null);
        paths = NodeRepoSyncSettings.getInstance().getAndCacheIncludedPathsForMountID(m_server);
        assertThat("unexpected path", paths, is(emptyList()));
    }

    /**
     * Reset server to init-state.
     */
    @After
    public void shutdown() {
        NodeRepoSyncSettings.getInstance().clearServerConfiguredPathsCache();
        m_server.setServerConfiguredTemplatePaths(null);
        m_server.setDefaultTemplatePaths(null);
        setIncludedPathsInPrefs(m_server);
    }
}
