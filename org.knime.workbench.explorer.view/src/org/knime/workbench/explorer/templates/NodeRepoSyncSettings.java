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
 *   May 2, 2019 (hornm): created
 */
package org.knime.workbench.explorer.templates;

import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.refreshNodeRepo;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.removeCorrespondingCategory;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.removeTemplateCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Central class that handles and provides the node synchronizer (global and mount-point specific) settings.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class NodeRepoSyncSettings {

    private static NodeRepoSyncSettings INSTANCE;

    private static final List<String> SERVER_CONFIG_NOT_AVAILABLE_PLACEHOLDER = new ArrayList<>(0);

    /**
     * @return the singleton instance
     */
    public static NodeRepoSyncSettings getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NodeRepoSyncSettings();
        }
        return INSTANCE;
    }

    private Map<String, List<String>> m_prefsConfiguredIncludedPaths = new HashMap<>();

    private Map<String, List<String>> m_serverConfiguredIncludedPaths = new HashMap<>();

    private boolean m_isActivated;

    private NodeRepoSyncSettings() {
        loadFromPreferences();

        ExplorerActivator.getDefault().getPreferenceStore().addPropertyChangeListener(e -> {
            if (PreferenceConstants.P_EXPLORER_ADD_TEMPLATES_TO_NODE_REPO.equals(e.getProperty())
                || PreferenceConstants.P_EXPLORER_TEMPLATE_WORKFLOW_GROUPS_TO_NODE_REPO.equals(e.getProperty())) {
                updateIncludedPathsForMountPointsConfiguredViaPreferences();
            }
        });
    }

    /**
     * @return <code>true</code> if synchronization of workflow templates is activated altogether, otherwise
     *         <code>false</code>
     */
    boolean isActivated() {
        return m_isActivated;
    }

    /**
     * Determines and returns the included paths for a specific mount point. It uses different 'sources' to determine
     * the paths in the following order:
     * <ul>
     * <li>1. ask server for included paths</li>
     * <li>2. if not configured/available, use the preferences</li>
     * <li>3. if no preferences set for given mount point, use the defaults of the mount point implementation</li>
     * <li>4. if no defaults given, return an empty list</li>
     * </ul>
     *
     * @param mountPoint the mount point to get the included paths for
     * @return list of included paths, might be empty but never <code>null</code>
     */
    List<String> getIncludedPathsForMountID(final AbstractContentProvider mountPoint) {
        String mountID = mountPoint.getMountID();
        Optional<List<String>> paths;

        if (mountPoint.isRemote()) {
            //1. check for cached server configured paths
            if (m_serverConfiguredIncludedPaths.containsKey(mountID)) {
                List<String> l = m_serverConfiguredIncludedPaths.get(mountID);
                if (l != SERVER_CONFIG_NOT_AVAILABLE_PLACEHOLDER) {
                    return l;
                }
            } else {
                //1.2 ask server for included paths if available/configured
                paths = mountPoint.fetchServerConfiguredTemplatePaths();
                if (paths.isPresent()) {
                    m_serverConfiguredIncludedPaths.put(mountID, paths.get());
                    return paths.get();
                } else {
                    //make a note that we've asked the server already
                    m_serverConfiguredIncludedPaths.put(mountID, SERVER_CONFIG_NOT_AVAILABLE_PLACEHOLDER);
                }
            }
        }

        //2. look in preferences for included paths
        if (m_prefsConfiguredIncludedPaths.containsKey(mountID)) {
            return m_prefsConfiguredIncludedPaths.get(mountID);
        }

        //3. use default in mount point implementation
        paths = mountPoint.getDefaultTemplatePaths();
        if (paths.isPresent()) {
            return paths.get();
        }

        //4. no included paths
        return Collections.emptyList();
    }

    /**
     * Clears the included paths that have been memorized for already accessed servers.
     */
    public void clearServerConfiguredPathsCache() {
        m_serverConfiguredIncludedPaths.clear();
    }

    /**
     * For unit-testing only!!!
     *
     * @param isActivated
     * @param includedPathsPerMountPoint
     */
    void setPreferences(final boolean isActivated, final Map<String, List<String>> includedPathsPerMountPoint) {
        m_isActivated = isActivated;
        m_prefsConfiguredIncludedPaths = includedPathsPerMountPoint;
    }

    private void updateIncludedPathsForMountPointsConfiguredViaPreferences() {
        Set<String> oldMountIDs = new HashSet<String>(m_prefsConfiguredIncludedPaths.keySet());
        loadFromPreferences();
        if (m_isActivated) {
            //remove removed mount points from node repo, too
            oldMountIDs.removeAll(m_prefsConfiguredIncludedPaths.keySet());
            oldMountIDs.stream().forEach(id -> {
                removeCorrespondingCategory(ExplorerMountTable.getMountedContent().get(id).getFileStore("/"));
            });

            if (m_prefsConfiguredIncludedPaths.size() > 0) {
                //update all remaining and added mount points in node repo
                m_prefsConfiguredIncludedPaths.keySet().stream().filter(mountID -> {
                    //ignore mount points configured via server
                    return !m_serverConfiguredIncludedPaths.containsKey(mountID);
                }).forEach(id -> {
                    AbstractContentProvider mountPointContent = ExplorerMountTable.getMountedContent().get(id);
                    NodeRepoSynchronizer.getInstance().clearAllSyncJobs();
                    NodeRepoSynchronizer.getInstance().syncWithNodeRepo(mountPointContent);
                });
            } else {
                refreshNodeRepo();
            }
        } else {
            removeTemplateCategory();
            refreshNodeRepo();
        }
    }

    private void loadFromPreferences() {
        IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        m_isActivated = prefStore.getBoolean(PreferenceConstants.P_EXPLORER_ADD_TEMPLATES_TO_NODE_REPO);
        String[] includedPaths =
            prefStore.getString(PreferenceConstants.P_EXPLORER_TEMPLATE_WORKFLOW_GROUPS_TO_NODE_REPO).split(",");
        m_prefsConfiguredIncludedPaths.clear();
        for(String p : includedPaths) {
            if (p.contains(":")) {
                String[] split = p.split(":");
                assert split.length == 2;
                m_prefsConfiguredIncludedPaths.computeIfAbsent(split[0], k -> new ArrayList<String>()).add(split[1]);
            }
        }
    }
}
