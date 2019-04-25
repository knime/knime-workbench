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
 *   Apr 11, 2019 (hornm): created
 */
package org.knime.workbench.explorer.templates;

import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowGroup;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.getOrCreatePathInNodeRepo;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.refreshNodeRepo;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.removeCorrespondingCategory;
import static org.knime.workbench.explorer.templates.NodeRepoSyncUtil.traverseTree;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;

/**
 * Collects the metanode templates recursively contained in a workflow group (represented by a
 * {@link AbstractExplorerFileStore}) and add/removes them to/from the node repository. Or put in different words: it
 * synchronizes the given workflow group with the respective category in the node repository.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class NodeRepoSyncJob extends ExplorerJob {

    private WeakReference<AbstractExplorerFileStore> m_weakReferencedFileStore;
    private AbstractExplorerFileStore m_explorerFileStore;
    private List<String> m_includedPaths;

    /**
     * @param explorerFileStore the starting point to start scanning for metanode templates
     * @param includedPaths only paths given in that list are included, all others are ignored
     */
    public NodeRepoSyncJob(final AbstractExplorerFileStore explorerFileStore, final List<String> includedPaths) {
        super("Collect metanode templates from " + explorerFileStore.getMountID());
        m_explorerFileStore = explorerFileStore;
        m_weakReferencedFileStore = new WeakReference<>(explorerFileStore);
        m_includedPaths = includedPaths;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        if (!isWorkflowGroup(m_explorerFileStore)) {
            if (m_explorerFileStore.getParent() == null) {
                //it's an mountpoint that has no children anymore
                if (removeCorrespondingCategory(m_explorerFileStore)) {
                    refreshNodeRepo();
                }
            }
            return Status.OK_STATUS;
        }

        try {
            //traverse entire sub-tree to find metanode templates and add it to the parent category
            List<AbstractRepositoryObject> children = traverseTree(m_explorerFileStore, m_includedPaths, monitor);
            if (!children.isEmpty()) {
                Category cat = getOrCreatePathInNodeRepo(m_explorerFileStore, true);
                cat.removeAllChildren();
                cat.addAllChildren(children);
            } else {
                //no children found
                removeCorrespondingCategory(m_explorerFileStore);
            }
        } catch (CoreException e) {
            //should not happen
            throw new RuntimeException(e);
        } finally {
            m_explorerFileStore = null;
        }
        refreshNodeRepo();
        return Status.OK_STATUS;
    }

    /**
     * @return the file store this sync job has been scheduled on or an empty optional if it the reference has been
     *         cleared
     */
    Optional<AbstractExplorerFileStore> getFileStore() {
        return Optional.ofNullable(m_weakReferencedFileStore.get());
    }
}
