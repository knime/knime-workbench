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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.knime.testing.explorer.LocalFileStoreTestUtils.createTemplate;
import static org.knime.testing.explorer.LocalFileStoreTestUtils.createWorkflowGroup;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.testing.explorer.LocalFileStoreTestUtils;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProvider;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.AbstractContainerObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.ExplorerMetaNodeTemplate;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.Root;

/**
 * Tests the {@link NodeRepoSynchronizer}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeRepoSynchronizerTest {

    private Root m_repoRoot;

    private LocalExplorerFileStore m_localExplorerRoot;

    private LocalWorkspaceContentProvider m_localWorkspace;

    /**
     * Setup of node repo, local mountpoint etc.
     *
     * @throws IOException
     */
    @Before
    public void setup() throws IOException {
        RepositoryManager.INSTANCE.doNotReadRepositoryForTesting();
        m_repoRoot = RepositoryManager.INSTANCE.getRoot();
        //add at least one dummy item such that the root is not empty
        m_repoRoot.addChild(new Category("dummy_id", "dummy_name", ""));
        m_localWorkspace = (LocalWorkspaceContentProvider)ExplorerMountTable.mount("LOCAL",
            LocalWorkspaceContentProviderFactory.ID, null);
        m_localExplorerRoot = getFileStore("/");
    }

    private LocalExplorerFileStore getFileStore(final String fullPath) {
        return (LocalExplorerFileStore)m_localWorkspace.getFileStore(fullPath);
    }

    /**
     * Tests whether adding a new template and removing it later is correctly synchronized with the node repo.
     *
     * @throws Exception
     */
    @Test
    public void testAddAndRemoveSingleTemplate() throws Exception {
        setIncludedPathsInPrefs(m_localWorkspace, "/");
        LocalExplorerFileStore wg1 = createWorkflowGroup(m_localExplorerRoot, "wg1");
        LocalExplorerFileStore wt1 = createTemplate(wg1, "wt1", true);
        LocalFileStoreTestUtils.createEmptyWorkflow(wg1, "wf1");
        Job job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(m_localWorkspace).get();
        job.join();
        checkPathInNodeRepo("/LOCAL/wg1/wt1", true);
        //make sure that workflows that are not templates are not considered
        checkPathInNodeRepo("/LOCAL/wg2/wf1", false);

        deleteFileStore(wt1);
        job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(m_localExplorerRoot).get();
        job.join();
        checkPathInNodeRepo("/LOCAL/wg1/wt1", false);
        //make sure that all empty parent categories have been removed, too
        checkPathInNodeRepo("/LOCAL/wg1/", false);
        checkPathInNodeRepo("/LOCAL/", false);
    }

    /**
     * Test whether synchronisation works as expected when the included path is not the root.
     *
     * @throws Exception
     */
    @Test
    public void testIncludedPathThatIsNotTheRoot() throws Exception {
        setIncludedPathsInPrefs(m_localWorkspace, "/wg2", "/wg3");
        LocalExplorerFileStore wg1 = createWorkflowGroup(m_localExplorerRoot, "wg1");
        createTemplate(wg1, "wt1", true);
        LocalExplorerFileStore wg2 = createWorkflowGroup(m_localExplorerRoot, "wg2");
        createTemplate(wg2, "wt2", true);
        LocalExplorerFileStore wg3 = createWorkflowGroup(m_localExplorerRoot, "wg3");
        createTemplate(wg3, "wt3", true);


        Job job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(m_localExplorerRoot).get();
        job.join();

        checkPathInNodeRepo("/LOCAL/wg2/wt2", true);
        checkPathInNodeRepo("/LOCAL/wg3/wt3", true);
        checkPathInNodeRepo("/LOCAL/wg1/", false);

        job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(wg1).orElse(null);
        assertNull("synchronization job not expected to be scheduled because wg1 is not in the included paths", job);
    }

    /**
     * Tests correct behavior when no included paths are set.
     *
     * @throws Exception
     */
    @Test
    public void testNoInlcudedPaths() throws Exception {
        setIncludedPathsInPrefs(m_localWorkspace);
        LocalExplorerFileStore wg1 = createWorkflowGroup(m_localExplorerRoot, "wg1");
        createTemplate(wg1, "wt1", true);
        LocalExplorerFileStore wg2 = createWorkflowGroup(m_localExplorerRoot, "wg2");
        createTemplate(wg2, "wt2", true);

        Optional<Job> noJob = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(m_localExplorerRoot);
        assertNull("synchronization job not expected to be scheduled", noJob.orElse(null));
    }

    /**
     * Tests correct behavior when the synchronization is triggered in short sequence.
     *
     * @throws Exception
     */
    @Test
    public void testSuccessiveSynchronization() throws Exception {
        setIncludedPathsInPrefs(m_localWorkspace, "/");
        LocalExplorerFileStore wg1 = createWorkflowGroup(m_localExplorerRoot, "wg1");
        createTemplate(wg1, "wt1", true);
        LocalExplorerFileStore wg2 = createWorkflowGroup(m_localExplorerRoot, "wg2");
        createTemplate(wg2, "wt2", true);

        Optional<Job> job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(wg1);
        assertNotNull("synchronization job expected to be scheduled", job.orElse(null));
        Optional<Job> noJob = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(wg1);
        assertNull("synchronization job not expected to be scheduled", noJob.orElse(null));
        noJob = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(wg1);
        assertNull("synchronization job not expected to be scheduled", noJob.orElse(null));
        job.get().join();
        job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo(wg1);
        assertNotNull("synchronization job expected to be scheduled", job.orElse(null));
    }

    /**
     * Test what happens when <code>null</code> is passed.
     */
    @Test
    public void testSyncOnNullObjects() {
        Optional<Job> job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo((AbstractExplorerFileStore)null);
        assertNull("synchronization job not expected to be scheduled", job.orElse(null));

        job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo((AbstractContentProvider)null);
        assertNull("synchronization job not expected to be scheduled", job.orElse(null));

        job = NodeRepoSynchronizer.getInstance().syncWithNodeRepo((Object)null);
        assertNull("synchronization job not expected to be scheduled", job.orElse(null));
    }

    /**
     * Tests that only wrapped metanodes (aka subnodes) are included but not ordinary metanodes.
     *
     * @throws Exception
     */
    @Test
    public void testWrappedMetanodesVsMetanodes() throws Exception {
        setIncludedPathsInPrefs(m_localWorkspace, "/");
        createTemplate(m_localExplorerRoot, "wt1", true);
        createTemplate(m_localExplorerRoot, "wt2", false);

        NodeRepoSynchronizer.getInstance().syncWithNodeRepo(m_localExplorerRoot).get().join();

        checkPathInNodeRepo("/LOCAL/wt1", true);
        checkPathInNodeRepo("/LOCAL/wt2", false);
    }

    /**
     * Tests that the server-configured included paths and the repsective fallbacks (to the preferences or mount point
     * default) work.
     *
     * @throws Exception
     */
    @Test
    public void testServerConfiguredIncludedPathsAndFallbacks() throws Exception {
        MockedLocalServerContentProvider server = (MockedLocalServerContentProvider)ExplorerMountTable
            .mount("mockserver", MockedLocalServerContentProviderFactory.ID, null);
        LocalExplorerFileStore root = (LocalExplorerFileStore)server.getFileStore("/");
        LocalExplorerFileStore prefsFS = createWorkflowGroup(root, "prefs");
        LocalExplorerFileStore serverConfigFS = createWorkflowGroup(root, "server-config");
        LocalExplorerFileStore defaultFS = createWorkflowGroup(root, "default");
        createTemplate(prefsFS, "wt", true);
        createTemplate(serverConfigFS, "wt", true);
        createTemplate(defaultFS, "wt", true);
        setIncludedPathsInPrefs(server, "/prefs/wt");

        //test that server-config is favored over prefs
        server.setServerConfiguredTemplatePaths(asList("/server-config/wt"));
        NodeRepoSynchronizer.getInstance().syncWithNodeRepo(server).get().join();
        checkPathInNodeRepo("/mockserver/server-config/wt", true);
        checkPathInNodeRepo("/mockserver/prefs/wt", false);
        checkPathInNodeRepo("/mockserver/default/wt", false);

        //test that server-config is cached
        server.setServerConfiguredTemplatePaths(emptyList());
        NodeRepoSynchronizer.getInstance().syncWithNodeRepo(server).get().join();
        checkPathInNodeRepo("/mockserver/server-config/wt", true);
        NodeRepoSyncSettings.getInstance().clearServerConfiguredPathsCache();
        assertNull("job not expected to be scheduled, because no included paths configured",
            NodeRepoSynchronizer.getInstance().syncWithNodeRepo(server).orElse(null));

        //test fallback to preferences
        server.setServerConfiguredTemplatePaths(null);
        NodeRepoSyncSettings.getInstance().clearServerConfiguredPathsCache();
        NodeRepoSynchronizer.getInstance().syncWithNodeRepo(server).get().join();
        checkPathInNodeRepo("/mockserver/server-config/wt", false);
        checkPathInNodeRepo("/mockserver/prefs/wt", true);
        checkPathInNodeRepo("/mockserver/default/wt", false);

        //test fallback to default
        server.setDefaultTemplatePaths(asList("/default/wt"));
        setIncludedPathsInPrefs(server);
        NodeRepoSynchronizer.getInstance().syncWithNodeRepo(server).get().join();
        checkPathInNodeRepo("/mockserver/server-config/wt", false);
        checkPathInNodeRepo("/mockserver/prefs/wt", false);
        checkPathInNodeRepo("/mockserver/default/wt", true);
    }

    private void checkPathInNodeRepo(final String path, final boolean checkExistence) {
        assert path.startsWith("/");
        String[] split = new String("/" + NodeRepoSyncUtil.TEMPLATES_CAT_ID + path).split("/");
        AbstractContainerObject cont = m_repoRoot;
        for (int i = 1; i < split.length - 1; i++) {
            IRepositoryObject child = cont.getChildByID(split[i], false);
            if (child instanceof Category) {
                cont = (Category)child;
            } else {
                cont = null;
                break;
            }
        }
        boolean exists;
        if (cont != null) {
            if (path.endsWith("/")) {
                exists = cont.getChildByID(split[split.length - 1], false) instanceof Category;
            } else {
                exists = cont.getChildByID(split[split.length - 1], false) instanceof ExplorerMetaNodeTemplate;
            }
        } else {
            exists = false;
        }

        if (checkExistence) {
            assertTrue("the path '" + path + "' was expected in the node repository but wasn't there", exists);
        } else {
            assertTrue("the path '" + path + "' was not expected in the node repository but was there", !exists);
        }
    }

    private static void setIncludedPathsInPrefs(final AbstractContentProvider mountPoint,
        final String... includedPaths) {
        Map<String, List<String>> includedPathsPerMountPoint = new HashMap<>();
        if (includedPaths != null && includedPaths.length > 0) {
            includedPathsPerMountPoint.put(mountPoint.getMountID(), Arrays.asList(includedPaths));
        }
        NodeRepoSyncSettings.getInstance().setPreferences(true, includedPathsPerMountPoint);
    }

    private static void deleteFileStore(final LocalExplorerFileStore fs) throws CoreException {
        for (AbstractExplorerFileStore fs2 : fs.childStores(EFS.NONE, null)) {
            deleteFileStore((LocalExplorerFileStore)fs2);
        }
        fs.delete(EFS.NONE, null);
    }

    /**
     * Deletes all created files stores.
     *
     * @throws Exception
     */
    @After
    public void cleanWorkspace() throws Exception {
        m_localExplorerRoot.refresh();
        for (AbstractExplorerFileStore fs2 : m_localExplorerRoot.childStores(EFS.NONE, null)) {
            deleteFileStore((LocalExplorerFileStore)fs2);
        }
    }



}
