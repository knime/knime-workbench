/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 * Actions used in the UserSpace view should derive from this. It provides some
 * convenient methods.
 *
 * @author ohl, University of Konstanz
 */
public abstract class ExplorerAction extends Action {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerAction.class);

    private final ExplorerView m_view;

    private boolean m_isRO;

    /**
     * @param view of the space
     * @param menuText
     */
    public ExplorerAction(final ExplorerView view, final String menuText) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        m_view = view;
        setText(menuText);
        m_isRO = false;
    }

    /**
     * @param isRO determines whether the action is called on a read-only space
     * or not. If true, all actions modifying the content should be disabled.
     */
    public void setReadOnly(final boolean isRO) {
        m_isRO = isRO;
    }

    /**
     * @return true, if this action is called/activated on a read-only space.
     */
    protected boolean isRO() {
        return m_isRO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String getId();

    /**
     * @return the current selection in the corresponding view, could be null
     */
    protected IStructuredSelection getSelection() {
        IStructuredSelection selection =
                (IStructuredSelection)getViewer().getSelection();
        return selection;
    }

    /**
     * @return the first selected element - or null, if non is selected
     */
    protected Object getFirstSelection() {
        ISelection selection = m_view.getViewer().getSelection();
        if (selection == null) {
            return null;
        }
        return ((IStructuredSelection)selection).getFirstElement();
    }

    /**
     * @return true if multiple items have been selected, false otherwise
     */
    protected boolean isMultipleSelection() {
        IStructuredSelection selection = getSelection();
        return selection != null && selection.size() > 1;
    }

    /**
     * Sorts the selected file stores by content provider.
     *
     * @return a map associating the selected file store(s) to the corresponding
     *         content provider(s)
     */
    protected Map<AbstractContentProvider,
            List<AbstractExplorerFileStore>> getSelectedFiles() {
        return DragAndDropUtils.getProviderMap(getSelection());
    }

    /**
     * Returns the selected file stores.
     *
     * @return a list containing all selected file store(s)
     */
    protected List<AbstractExplorerFileStore> getAllSelectedFiles() {
        IStructuredSelection selection = getSelection();
        if (selection == null) {
            return null;
        }
        return DragAndDropUtils.getExplorerFileStores(selection);
    }

    /** If the selected element represents a workflow or workflow group, or whatever is defined through the argument,
     * return it. Otherwise return an empty Optional.
     * @param fs a non-null predicate to check the element's property
     * @return The single selected element or an empty Optional.
     */
    protected Optional<AbstractExplorerFileStore>
        getSingleSelectedElement(final Predicate<AbstractExplorerFileStore> fs) {
        List<AbstractExplorerFileStore> fileStores = getAllSelectedFiles();
        if (fileStores == null || fileStores.size() != 1) {
            return Optional.empty();
        }
        AbstractExplorerFileStore fileStore = fileStores.get(0);
        if (fs.test(fileStore)) {
            return Optional.of(fileStore);
        }
        return Optional.empty();
    }

    /**
     * Returns a new list (a new list with references to the same file stores)
     * containing only "top level" files, i.e. it does not contain files that
     * are children (direct or some levels down) of other selected files in the
     * list. (Probably mostly useful for recursive operations.)
     *
     * @param selection list to filter out children from. The files must all be
     *            from the same content provider!
     * @return a new list only containing top level files (no children of other
     *         list members are in the list).
     * @throws IllegalArgumentException if the files are not from the same
     *             content provider (have a different mount ID)
     */
    protected static List<AbstractExplorerFileStore> removeSelectedChildren(
            final List<AbstractExplorerFileStore> selection)
            throws IllegalArgumentException {
        List<AbstractExplorerFileStore> result =
                new LinkedList<AbstractExplorerFileStore>();
        if (selection.size() <= 0) {
            return result;
        }

        String mountID = selection.get(0).getMountID();

        Set<AbstractExplorerFileStore> dir
                = new HashSet<AbstractExplorerFileStore>();
        for (AbstractExplorerFileStore file : selection) {
            if (!mountID.equals(file.getMountID())) {
                LOGGER.coding("Method must be called with identical mountIDs"
                        + " in the files.");
            }

            AbstractExplorerFileStore parent = file.getParent();
            boolean contained = false;
            while (parent != null) {
                if (dir.contains(parent)) {
                    contained = true;
                    break;
                }
                parent = parent.getParent();
            }

            if (AbstractExplorerFileStore.isWorkflowGroup(file)) {
                dir.add(file);
            }

            if (!contained) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Checks if a file store is a child of any file store in the selection.
     *
     * @param child the file store to check if it is a child of a file store
     *      in the selection
     * @param selection the selection of files to check against
     * @return true if child is a child of a file store in the selection,
     *      false otherwise
     */
    protected static boolean isChildOf(final AbstractExplorerFileStore child,
            final Set<AbstractExplorerFileStore> selection) {
        Set<AbstractExplorerFileStore> parents
                = new HashSet<AbstractExplorerFileStore>();
        AbstractExplorerFileStore parent = child;
        while (parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }

        parents.retainAll(selection);
        return !parents.isEmpty();
    }

    /**
     * Returns a new list with workflows that are contained in the parameter
     * list (either directly or in any sub directory of the list) and that are
     * in a local file system (implement {@link LocalExplorerFileStore}).
     *
     * @param selected the list to return contained workflows from
     * @return a new list with local workflows contained (directly or
     *         indirectly) in the argument
     */
    public static List<LocalExplorerFileStore> getContainedLocalWorkflows(
            final List<? extends AbstractExplorerFileStore> selected) {
        List<LocalExplorerFileStore> result =
                new LinkedList<LocalExplorerFileStore>();
        for (AbstractExplorerFileStore f : selected) {
            if (!(f instanceof LocalExplorerFileStore)) {
                // assuming that only local stores have local children!
                continue;
            }
            if (AbstractExplorerFileStore.isWorkflow(f)) {
                result.add((LocalExplorerFileStore)f);
            } else if (f.fetchInfo().isDirectory()) {
                try {
                    AbstractExplorerFileStore[] children =
                            f.childStores(EFS.NONE, null);
                    result.addAll(getContainedLocalWorkflows(Arrays
                            .asList(children)));
                } catch (CoreException e) {
                    // ignore - no workflows contained.
                }
            } // else ignore
        }
        return result;
    }

    /**
     * Returns a new list with workflows that are contained in the parameter
     * list (either directly or in any sub directory of the list).
     *
     * @param selected the list to return contained workflows from
     * @return a new list with workflows contained (directly or indirectly) in
     *         the argument
     */
    public static List<AbstractExplorerFileStore>
        getAllContainedWorkflows(final List<? extends AbstractExplorerFileStore> selected) {
        return getAllContainedObjectsRecursivelyThatComplyingWith(f -> AbstractExplorerFileStore.isWorkflow(f),
            selected);
    }

    /**
     * Returns a new list with components that are contained in the parameter list (either directly or in any sub
     * directory of the list).
     *
     * @param selected the list to return contained components from
     * @return a new list with components contained (directly or indirectly) in the argument
     * @since 8.5
     */
    public static List<AbstractExplorerFileStore>
        getAllContainedComponents(final List<? extends AbstractExplorerFileStore> selected) {
        return getAllContainedObjectsRecursivelyThatComplyingWith(f -> AbstractExplorerFileStore.isComponent(f),
            selected);
    }

    private static List<AbstractExplorerFileStore> getAllContainedObjectsRecursivelyThatComplyingWith(
        final Predicate<AbstractExplorerFileStore> test, final List<? extends AbstractExplorerFileStore> selected) {
        List<AbstractExplorerFileStore> result = new LinkedList<AbstractExplorerFileStore>();
        for (AbstractExplorerFileStore f : selected) {
            if (test.test(f)) {
                result.add(f);
            } else if (f.fetchInfo().isDirectory()) {
                try {
                    AbstractExplorerFileStore[] children = f.childStores(EFS.NONE, null);
                    result.addAll(getAllContainedObjectsRecursivelyThatComplyingWith(test, Arrays.asList(children)));
                } catch (CoreException e) {
                    // ignore - no workflows contained.
                }
            } // else ignore
        }
        return result;
    }

    /**
     * Returns a new list of workflow jobs that are contained in the parameter list.
     *
     * @param selected the list to return contained workflow jobs from
     * @return a new list with workflow jobs contained (only the directly selected ones) in the argument
     * @since 8.3
     */
    public static List<AbstractExplorerFileStore>
        getAllContainedJobs(final List<? extends AbstractExplorerFileStore> selected) {
        List<AbstractExplorerFileStore> result =
                new LinkedList<AbstractExplorerFileStore>();
        for (AbstractExplorerFileStore f : selected) {
            if (f instanceof RemoteExplorerFileStore && ((RemoteExplorerFileStore)f).fetchInfo().isWorkflowJob()) {
                result.add(f);
            } // else ignore
        }
        return result;
    }

    /**
     * @return the viewer associated with the action.
     */
    protected TreeViewer getViewer() {
        return m_view.getViewer();
    }

    /**
     * @return the view for this action.
     */
    protected ExplorerView getView() {
        return m_view;
    }

    /**
     * @return the parent shell of the viewer
     */
    protected Shell getParentShell() {
        return m_view.getViewer().getControl().getShell();
    }

    /**
     * @return the workflow manager for the selection or null if multiple file
     *         stores are selected or the file stores are not opened
     */
    protected WorkflowManager getWorkflow() {
        IStructuredSelection selection = getSelection();
        if (selection == null) {
            return null;
        }
        List<AbstractExplorerFileStore> fileStores =
                DragAndDropUtils.getExplorerFileStores(selection);
        if (fileStores == null || fileStores.size() != 1) {
            return null;
        }
        AbstractExplorerFileStore fileStore = fileStores.get(0);
        if (AbstractExplorerFileStore.isWorkflow(fileStore)) {
            try {
                File localFile = fileStore.toLocalFile();
                if (localFile != null) {
                    WorkflowManager workflow =
                            (WorkflowManager)ProjectWorkflowMap
                                    .getWorkflow(localFile.toURI());
                    return workflow;
                }
            } catch (CoreException e) {
                LOGGER.error("Could not retrieve workflow.", e);
            }
        }
        return null;
    }
}
