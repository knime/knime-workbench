/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.knime.workbench.explorer.view.AbstractContentProvider;
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

    private final TreeViewer m_viewer;

    /**
     * @param viewer of the space
     */
    public ExplorerAction(final TreeViewer viewer, final String menuText) {
        assert viewer != null;
        m_viewer = viewer;
        setText(menuText);
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
        ISelection selection = m_viewer.getSelection();
        if (selection == null) {
            return null;
        }
        return ((IStructuredSelection)selection).getFirstElement();
    }

    /**
     * @return true if multiple items have been selected, false otherwise
     */
    protected boolean isMultipleSelection() {
        return getSelection().size() > 1;
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
    protected List<AbstractExplorerFileStore> removeSelectedChildren(
            final List<AbstractExplorerFileStore> selection)
            throws IllegalArgumentException {
        List<AbstractExplorerFileStore> result =
                new LinkedList<AbstractExplorerFileStore>();
        if (selection.size() <= 0) {
            return result;
        }

        String mountID = selection.get(0).getMountID();

        for (AbstractExplorerFileStore file : selection) {
            if (!mountID.equals(file.getMountID())) {
                LOGGER.coding("Method must be called with identical mountIDs"
                        + " in the files.");
            }
            // don't be case sensitive
            String p = file.getFullName().toLowerCase();
            boolean isChild = false;
            Iterator<AbstractExplorerFileStore> iter = result.iterator();
            while (iter.hasNext()) {
                AbstractExplorerFileStore resultF = iter.next();
                String resultP = resultF.getFullName().toLowerCase();
                if (p.startsWith(resultP)) {
                    isChild = true;
                    break;
                }
                if (resultP.startsWith(p)) {
                    iter.remove();
                }
            }
            if (!isChild) {
                result.add(file);
            }
        }
        return result;
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
    public static List<AbstractExplorerFileStore> getAllContainedWorkflows(
            final List<? extends AbstractExplorerFileStore> selected) {
        List<AbstractExplorerFileStore> result =
                new LinkedList<AbstractExplorerFileStore>();
        for (AbstractExplorerFileStore f : selected) {
            if (AbstractExplorerFileStore.isWorkflow(f)) {
                result.add(f);
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
     * @return the viewer associated with the action.
     */
    protected TreeViewer getViewer() {
        return m_viewer;
    }

    /**
     * @return the parent shell of the viewer
     */
    protected Shell getParentShell() {
        return m_viewer.getControl().getShell();
    }

    /**
     * @return the workflow manager for the selection or null if multiple file
     *         stores are selected or the file stores are not opened
     */
    protected WorkflowManager getWorkflow() {
        List<AbstractExplorerFileStore> fileStores =
                DragAndDropUtils.getExplorerFileStores(getSelection());
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
