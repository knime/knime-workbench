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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.jobs.Job;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;

/**
 * Bridge between the explorer's metanodes (i.e. templates) and the node repository. E.g. collects and populates
 * templates to the node repository.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class NodeRepoSynchronizer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeRepoSynchronizer.class);

    private static NodeRepoSynchronizer INSTANCE;

    private final NodeRepoSyncSettings m_settings;

    private Map<String, NodeRepoSyncJob> m_syncJobs = new HashMap<>();

    private NodeRepoSynchronizer() {
        // singleton
        m_settings = NodeRepoSyncSettings.getInstance();
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
     * @return the scheduled job or an empty optional if job hasn't been scheduled (or the passed object is not of a
     *         known type)
     */
    public Optional<Job> syncWithNodeRepo(final Object element) {
        if (!m_settings.isActivated()) {
            return Optional.empty();
        }
        if (element instanceof AbstractContentProvider) {
            return syncWithNodeRepo((AbstractContentProvider)element);
        } else if (element instanceof ContentObject) {
            return syncWithNodeRepo(((ContentObject)element).getFileStore());
        } else if (element instanceof AbstractExplorerFileStore) {
            return syncWithNodeRepo((AbstractExplorerFileStore)element);
        }
        return Optional.empty();
    }

    /**
     * Synchronizes metanode templates contained in the given mount point with the node repository.
     *
     * @param mountPoint the mount point to collect the templates from
     * @return the scheduled job or an empty optional if job hasn't been scheduled
     */
    public Optional<Job> syncWithNodeRepo(final AbstractContentProvider mountPoint) {
        if (!m_settings.isActivated() || mountPoint == null) {
            return Optional.empty();
        }
        return syncWithNodeRepo(mountPoint.getFileStore("/"));
    }

    /**
     * Synchronizes metanode templates with the node repository, starting from an element in the explorer tree that is
     * represented by the passed file store.
     *
     * @param fileStore represents an element in the explorer tree
     * @return the scheduled job or an empty optional if job hasn't been scheduled
     */
    public Optional<Job> syncWithNodeRepo(final AbstractExplorerFileStore fileStore) {
        if (!m_settings.isActivated() || fileStore == null) {
            return Optional.empty();
        }

        if (!isSyncJobStillRunning(fileStore)) {
            List<String> includedPaths = m_settings.getIncludedPathsForMountID(fileStore.getContentProvider());
            if (isConfiguredToBeIncluded(fileStore, includedPaths)) {
                NodeRepoSyncJob job = new NodeRepoSyncJob(fileStore, includedPaths);
                m_syncJobs.put(fileStore.getMountID(), job);
                //delay job execution a bit to wait (and 'swallow' multiple sync-calls in quick succession)
                job.schedule(200);
                LOGGER.debug("Explorer to Node Repo synchronization job scheduled on '" + fileStore + "'");
                return Optional.of(job);
            }
        }
        return Optional.empty();
    }

    /**
     * Forgets all 'cached' synchronization jobs. Still running jobs are not canceled.
     */
    void clearAllSyncJobs() {
        m_syncJobs.clear();
    }

    private boolean isSyncJobStillRunning(final AbstractExplorerFileStore fileStore) {
        String id = fileStore.getMountID();
        if (m_syncJobs.get(id) != null) {
            if (m_syncJobs.get(id).getResult() == null) {
                //job still running
                return true;
            } else {
                m_syncJobs.remove(id);
                return false;
            }
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
