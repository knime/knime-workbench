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

import static org.knime.workbench.core.util.ImageRepository.SharedImages.DefaultMetaNodeIcon;
import static org.knime.workbench.core.util.ImageRepository.SharedImages.WorkflowGroup;
import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowGroup;
import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.AbstractContainerObject;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.ExplorerMetaNodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.view.AbstractRepositoryView;

/**
 * Collects the metanode templates recursively contained in a workflow group (represented by a
 * {@link AbstractExplorerFileStore}) and add/removes them to/from the node repository. Or put in different words: it
 * synchronizes the given workflow group with the respective category in the node repository.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class SyncTemplatesWithNodeRepoJob extends ExplorerJob {

    private static final String TEMPLATES_CAT_ID = "metanode_templates";

    private AbstractExplorerFileStore m_explorerFileStore;

    /**
     * @param explorerFileStore
     */
    public SyncTemplatesWithNodeRepoJob(final AbstractExplorerFileStore explorerFileStore) {
        super("Collect metanode templates from " + explorerFileStore.getMountID());
        m_explorerFileStore = explorerFileStore;
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
            List<AbstractRepositoryObject> children = traverseTree(m_explorerFileStore, monitor);
            if (!children.isEmpty()) {
                Category cat = getOrCreatePathInNodeRepo(m_explorerFileStore, true);
                cat.removeAllChildren();
                cat.addAllChildren(children);
            } else {
                //no children found
                removeCorrespondingCategory(m_explorerFileStore);
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        refreshNodeRepo();
        return Status.OK_STATUS;
    }

    /**
     * @return the file store this sync job has been scheduled on
     */
    AbstractExplorerFileStore getFileStore() {
        return m_explorerFileStore;
    }

    /**
     * Triggers a refresh of the node repository.
     */
    private static void refreshNodeRepo() {
        Display.getDefault().syncExec(() -> {
            IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            AbstractRepositoryView view =
                (AbstractRepositoryView)activePage.findView("org.knime.workbench.repository.view.RepositoryView");
            view.updateRepositoryViewInUIThread(RepositoryManager.INSTANCE.getRoot());
        });
    }

    /**
     * Traverses the sub-tree of a workflow group in order to find metanode templates.
     *
     * @param fs
     * @param monitor
     * @return the found direct children of the provided fs (yet, as node repo object)
     * @throws CoreException
     */
    private static List<AbstractRepositoryObject> traverseTree(final AbstractExplorerFileStore fs,
        final IProgressMonitor monitor) throws CoreException {
        if (monitor.isCanceled()) {
            return Collections.emptyList();
        }
        assert isWorkflowGroup(fs);
        String[] childNames = fs.childNames(EFS.NONE, monitor);
        List<AbstractRepositoryObject> newChildren = new ArrayList<>(childNames.length);
        for (String name : childNames) {
            AbstractExplorerFileStore child = fs.getChild(name);
            if (isWorkflowGroup(child)) {
                List<AbstractRepositoryObject> objs = traverseTree(child, monitor);
                if (!objs.isEmpty()) {
                    Category cat = createCategory(child.getName(), child.getName(), WorkflowGroup);
                    cat.addAllChildren(objs);
                    newChildren.add(cat);
                }
            } else if (isWorkflowTemplate(child)) {
                //TODO filter wrapped metanodes only
                ExplorerMetaNodeTemplate metanodeTemplate = new ExplorerMetaNodeTemplate(name, name, "", "TODO", child);
                metanodeTemplate.setIcon(ImageRepository.getIconImage(SharedImages.MetanodeRepository));
                newChildren.add(metanodeTemplate);
                monitor.worked(1);
            }
        }
        return newChildren;
    }

    /**
     * Get (and optionally create) the path (i.e. node categories) in the node repository as represented by the file
     * store.
     *
     * @param fileStore file store to get (and optionally create) the path from
     * @param createIfDoesntExist create the path if it doesn't exist
     * @return the category that corresponds to the given file store or <code>null</code> if it doesn't exist (and
     *         should not be created)
     */
    private static Category getOrCreatePathInNodeRepo(final AbstractExplorerFileStore fileStore,
        final boolean createIfDoesntExist) {
        Root root = RepositoryManager.INSTANCE.getRoot();

        Category templates =
            getOrCreateCategory(root, TEMPLATES_CAT_ID, "Templates", DefaultMetaNodeIcon, createIfDoesntExist);
        if (templates == null) {
            return null;
        }
        Category mountpoint = getOrCreateCategory(templates, fileStore.getMountID(), fileStore.getMountID(),
            WorkflowGroup, createIfDoesntExist);
        if (mountpoint == null) {
            return null;
        }

        //check the rest of the path
        String[] path = fileStore.getFullName().split("/");
        if (path.length > 1) {
            Category cat = getOrCreateCategory(mountpoint, path[1], path[1], WorkflowGroup, createIfDoesntExist);
            for (int i = 2; i < path.length; i++) {
                if (cat == null) {
                    return null;
                }
                cat = getOrCreateCategory(cat, path[i], path[i], WorkflowGroup, createIfDoesntExist);
            }
            return cat;
        }
        return mountpoint;
    }

    /**
     * Creates a new category in the node repository (if it doesn't exist, yet).
     *
     * @param parent
     * @param id
     * @param name
     * @param icon
     * @param createIfDoesntExist
     * @return
     */
    private static Category getOrCreateCategory(final AbstractContainerObject parent, final String id,
        final String name, final SharedImages icon, final boolean createIfDoesntExist) {
        Category cat = null;
        cat = (Category)parent.getChildByID(id, false);
        if (cat == null && createIfDoesntExist) {
            cat = new Category(id, name, "TODO");
            cat = createCategory(id, name, icon);
            cat.setIcon(ImageRepository.getIconImage(icon));
            parent.addChild(cat);
        }
        return cat;
    }

    private static Category createCategory(final String id, final String name, final SharedImages icon) {
        Category cat = new Category(id, name, "TODO");
        cat.setIcon(ImageRepository.getIconImage(icon));
        return cat;
    }

    /**
     * Removes the category (and potentially empty parents) that corresponds to the given file store.
     *
     * @param fileStore
     * @return <code>true</code> if something has been removed
     */
    private static boolean removeCorrespondingCategory(final AbstractExplorerFileStore fileStore) {
        Category cat = getOrCreatePathInNodeRepo(fileStore, false);
        if (cat != null) {
            cat.removeAllChildren();
            recursivelyRemoveEmptyCategories(cat);
            return true;
        }
        return false;
    }

    /**
     * @param cat the category to start with
     */
    private static void recursivelyRemoveEmptyCategories(final Category cat) {
        if (!cat.hasChildren()) {
            Category parent = (Category)cat.getParent();
            parent.removeChild(cat);
            recursivelyRemoveEmptyCategories(parent);
        }
    }
}
