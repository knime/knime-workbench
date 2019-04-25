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

import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.isParentOfAnyIncludedPath;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.isSubPathOfAnyIncludedPath;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.refreshNodeRepo;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.removeCorrespondingCategory;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.removeTemplateCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Bridge between the explorer's metanodes (i.e. templates) and the node repository. E.g. collects and populates
 * templates to the node repository.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class NodeRepoSynchronizer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeRepoSynchronizer.class);

    private static NodeRepoSynchronizer INSTANCE;

    private Map<String, NodeRepoSyncJob> m_syncJobs = new HashMap<>();

    private Map<String, List<String>> m_includedPathsPerMointPoint = new HashMap<>();

    private boolean m_isActivated;

    private NodeRepoSynchronizer() {
        loadFromPreferences();

        ExplorerActivator.getDefault().getPreferenceStore().addPropertyChangeListener(e -> {
            if (PreferenceConstants.P_EXPLORER_ADD_TEMPLATES_TO_NODE_REPO.equals(e.getProperty())
                || PreferenceConstants.P_EXPLORER_TEMPLATE_WORKFLOW_GROUPS_TO_NODE_REPO.equals(e.getProperty())) {
                Set<String> oldMountIDs = new HashSet<String>(m_includedPathsPerMointPoint.keySet());
                loadFromPreferences();
                if (m_isActivated) {
                    //remove removed mount points from node repo, too
                    oldMountIDs.removeAll(m_includedPathsPerMointPoint.keySet());
                    oldMountIDs.stream().forEach(id -> {
                        removeCorrespondingCategory(ExplorerMountTable.getMountedContent().get(id).getFileStore("/"));
                    });

                    if (m_includedPathsPerMointPoint.size() > 0) {
                        //update all remaining and added mount points in node repo
                        m_includedPathsPerMointPoint.keySet().stream().forEach(id -> {
                            AbstractContentProvider mountPointContent = ExplorerMountTable.getMountedContent().get(id);
                            m_syncJobs.clear();
                            syncWithNodeRepo(mountPointContent);
                        });
                    } else {
                        refreshNodeRepo();
                    }
                } else {
                    removeTemplateCategory();
                    refreshNodeRepo();
                }
            }
        });
    }

    private void loadFromPreferences() {
        IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        m_isActivated = prefStore.getBoolean(PreferenceConstants.P_EXPLORER_ADD_TEMPLATES_TO_NODE_REPO);
        String[] includedPaths =
            prefStore.getString(PreferenceConstants.P_EXPLORER_TEMPLATE_WORKFLOW_GROUPS_TO_NODE_REPO).split(",");
        m_includedPathsPerMointPoint.clear();
        for(String p : includedPaths) {
            if (p.contains(":")) {
                String[] split = p.split(":");
                assert split.length == 2;
                m_includedPathsPerMointPoint.computeIfAbsent(split[0], k -> new ArrayList<String>()).add(split[1]);
            }
        }
    }

    /**
     * @return the singleton instance
     */
    public static NodeRepoSynchronizer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NodeRepoSynchronizer();
        }
        return INSTANCE;
    }

    /**
     * Synchronizes metanode templates with the node repository, starting from an element the explorer tree that is
     * represented by the passed object.
     *
     * @param element represents an element in the explorer tree
     */
    public void syncWithNodeRepo(final Object element) {
        if (!m_isActivated) {
            return;
        }
        if (element instanceof AbstractContentProvider) {
            syncWithNodeRepo((AbstractContentProvider)element);
        } else if (element instanceof ContentObject) {
            syncWithNodeRepo(((ContentObject)element).getFileStore());
        } else if (element instanceof AbstractExplorerFileStore) {
            syncWithNodeRepo((AbstractExplorerFileStore)element);
        }
    }

    /**
     * Synchronizes metanode templates contained in the given mount point with the node repository.
     *
     * @param mountPoint the mount point to collect the templates from
     */
    public void syncWithNodeRepo(final AbstractContentProvider mountPoint) {
        if (!m_isActivated) {
            return;
        }
        syncWithNodeRepo(mountPoint.getFileStore("/"));
    }

    /**
     * Synchronizes metanode templates with the node repository, starting from an element the explorer tree that is
     * represented by the passed file store.
     *
     * @param fileStore represents an element in the explorer tree
     */
    public void syncWithNodeRepo(final AbstractExplorerFileStore fileStore) {
        if (!m_isActivated) {
            return;
        }

        List<String> includedPaths = m_includedPathsPerMointPoint.get(fileStore.getMountID());
        if (!isSyncJobStillRunningOrFileStoreAlreadyProcessed(fileStore)
            && isConfiguredToBeIncluded(fileStore, includedPaths)) {
            //TODO pass included paths to job
            NodeRepoSyncJob job = new NodeRepoSyncJob(fileStore, includedPaths);
            m_syncJobs.put(fileStore.getMountID(), job);
            job.schedule();
            LOGGER.debug("Explorer to Node Repo synchronization job scheduled on '" + fileStore + "'");
        }
    }

    private boolean isSyncJobStillRunningOrFileStoreAlreadyProcessed(final AbstractExplorerFileStore fileStore) {
        String id = fileStore.getMountID();
        if (m_syncJobs.get(id) != null) {
            if (m_syncJobs.get(id).getResult() == null) {
                //job still running
                return true;
            }

            //check whether file store has been already processed, recently
            Optional<AbstractExplorerFileStore> jobFileStore = m_syncJobs.get(id).getFileStore();
            return jobFileStore.map(fs -> fs == fileStore).orElse(false);
        } else {
            return false;
        }
    }

    private static boolean isConfiguredToBeIncluded(final AbstractExplorerFileStore fileStore,
        final List<String> includedPaths) {
        if (includedPaths == null || includedPaths.isEmpty()) {
            return false;
        }
        return isParentOfAnyIncludedPath(fileStore, includedPaths)
            || isSubPathOfAnyIncludedPath(fileStore, includedPaths);
    }
}
