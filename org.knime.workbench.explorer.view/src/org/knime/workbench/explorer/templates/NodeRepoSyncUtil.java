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

import static org.knime.workbench.core.util.ImageRepository.SharedImages.DefaultMetaNodeIcon;
import static org.knime.workbench.core.util.ImageRepository.SharedImages.WorkflowGroup;
import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowGroup;
import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.TemplateType;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.meta.TemplateInfo;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.AbstractContainerObject;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.ExplorerMetaNodeTemplate;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.view.AbstractRepositoryView;

/**
 * Utility methods to synchronize explorer templates with the node repository.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class NodeRepoSyncUtil {

    public static final String TEMPLATES_CAT_ID = "metanode_templates";

    private NodeRepoSyncUtil() {
        // utility class
    }

    /**
     * Triggers a refresh of the node repository.
     */
    static void refreshNodeRepo() {
        Display.getDefault().syncExec(() -> {
            IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            AbstractRepositoryView view =
                (AbstractRepositoryView)activePage.findView("org.knime.workbench.repository.view.RepositoryView");
            if (view != null) { //node repo view might not be available (e.g. in case of unit-tests or when closed)
                view.updateRepositoryViewInUIThread(RepositoryManager.INSTANCE.getRoot());
            }
        });
    }

    /**
     * Traverses the sub-tree of a workflow group in order to find metanode templates.
     *
     * @param fs the explorer item to start from
     * @param the path to be included, all others will be ignored
     * @param monitor for cancellation
     * @return the found direct children of the provided fs (yet, as node repo object)
     * @throws CoreException thrown if children of the filestore couldn't be fetched
     */
    static List<AbstractRepositoryObject> traverseTree(final AbstractExplorerFileStore fs,
        final List<String> includedPaths, final IProgressMonitor monitor) throws CoreException {
        if (monitor.isCanceled()) {
            return Collections.emptyList();
        }
        assert isWorkflowGroup(fs);

        String[] childNames = fs.childNames(EFS.NONE, monitor);
        List<AbstractRepositoryObject> newChildren = new ArrayList<>(childNames.length);
        for (String name : childNames) {
            AbstractExplorerFileStore child = fs.getChild(name);
            boolean isParent = isParentOfAnyIncludedPath(child, includedPaths);
            boolean isSubPath = isSubPathOfAnyIncludedPath(child, includedPaths);
            if (!isParent && !isSubPath) {
                continue;
            }
            if (isWorkflowGroup(child)) {
                List<AbstractRepositoryObject> objs = traverseTree(child, includedPaths, monitor);
                if (!objs.isEmpty()) {
                    Category cat = createCategory(child.getName(), child.getName(), WorkflowGroup);
                    cat.addAllChildren(objs);
                    newChildren.add(cat);
                }
            } else if (isSubPath && isWorkflowTemplate(child)) {
                Optional<TemplateType> type = child.fetchMetaInfo().flatMap(i -> ((TemplateInfo)i).getType());
                if (type.isPresent() && type.get() == TemplateType.MetaNode) {
                    // filter wrapped metanodes only (if this info is available)
                    continue;
                }

                ExplorerMetaNodeTemplate metanodeTemplate = new ExplorerMetaNodeTemplate(name, name, "", child);
                metanodeTemplate.setIcon(ImageRepository.getIconImage(SharedImages.MetanodeRepository));
                newChildren.add(metanodeTemplate);
                monitor.worked(1);
            }
        }
        return newChildren;
    }

    /**
     * Whether the path (represented by the file store) is a parent-path of at least one 'included path'.
     *
     * @param fileStore represents the path to check
     * @param includedPaths a list of paths to check against
     * @return <code>true</code> if is parent
     */
    static boolean isParentOfAnyIncludedPath(final AbstractExplorerFileStore fileStore,
        final List<String> includedPaths) {
        String path = fileStore.getFullName();
        return includedPaths.stream().anyMatch(inc -> inc.startsWith(path));
    }

    /**
     * Whether a path (represented by the file store) is a sub-path of at least one 'included path'.
     *
     * @param fileStore represents the path to check
     * @param includedPaths a list of paths to check against
     * @return <code>true</code> if is sub-path
     */
    static boolean isSubPathOfAnyIncludedPath(final AbstractExplorerFileStore fileStore,
        final List<String> includedPaths) {
        String path = fileStore.getFullName();
        return includedPaths.stream().anyMatch(inc -> path.startsWith(inc));
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
    static Category getOrCreatePathInNodeRepo(final AbstractExplorerFileStore fileStore,
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
     * @param parent the parent to look for the category, or, if it doesn't exist, the parent of the to be created
     *            category (i.e. the returned category is always a direct child of parent)
     * @param id the id of the category to look for (or of the new category if it doesn't exist)
     * @param name the name of the new category (if newly created)
     * @param icon the icon of the new category (if newly created)
     * @param createIfDoesntExist if <code>true</code> and the category for the given id doesn't exist in
     *            <code>parent</code>, a new category will be created. If <code>false</code>, <code>null</code> will be
     *            returned
     * @return the found or newly created category, or <code>null</code> if category wasn't found and
     *         <code>createIfDoesntExist</code> is <code>false</code>
     */
    static Category getOrCreateCategory(final AbstractContainerObject parent, final String id,
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

    /**
     * Creates a new category and returns it.
     *
     * @param id id of the new cat
     * @param name name of the new cat
     * @param icon icon of the new cat
     * @return the new cat
     */
    static Category createCategory(final String id, final String name, final SharedImages icon) {
        Category cat = new Category(id, name, "Category doesn't belong to a KNIME extension");
        cat.setIcon(ImageRepository.getIconImage(icon));
        return cat;
    }

    /**
     * Removes the entire template category from the node repository.
     */
    static void removeTemplateCategory() {
        Root root = RepositoryManager.INSTANCE.getRoot();
        root.removeChild((AbstractRepositoryObject)root.getChildByID(TEMPLATES_CAT_ID, false));
    }

    /**
     * Removes the category (and potentially empty parents) that corresponds to the given file store.
     *
     * @param fileStore the filestore to start at (and then recursively going upwards in the path hierarchy)
     * @return <code>true</code> if something has been removed
     */
    static boolean removeCorrespondingCategory(final AbstractExplorerFileStore fileStore) {
        Category cat = getOrCreatePathInNodeRepo(fileStore, false);
        if (cat != null) {
            cat.removeAllChildren();
            recursivelyRemoveEmptyCategories(cat);
            return true;
        }
        return false;
    }

    /**
     * Recursively removes empty categories from their parents.
     *
     * @param cat the category to start with
     */
    static void recursivelyRemoveEmptyCategories(final Category cat) {
        if (!cat.hasChildren()) {
            IContainerObject parent = cat.getParent();
            parent.removeChild(cat);
            if (parent instanceof Category) {
                recursivelyRemoveEmptyCategories((Category)parent);
            }
        }
    }

    /**
     * Blocks till the node repository has been loaded entirely. If loaded already it will return immediately.
     */
    static void waitForNodeRepoToBeLoaded() {
        RepositoryManager.INSTANCE.getRoot();
    }
}
