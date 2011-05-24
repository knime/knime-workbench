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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;

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
     * Sorts the selected file stores by content provider.
     *
     * @return a map associating the selected file store(s) to the corresponding
     *         content provider(s)
     */
    protected Map<AbstractContentProvider, List<ExplorerFileStore>> getSelectedFiles() {
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
    protected List<ExplorerFileStore> removeSelectedChildren(
            final List<ExplorerFileStore> selection)
            throws IllegalArgumentException {
        List<ExplorerFileStore> result = new LinkedList<ExplorerFileStore>();
        if (selection.size() <= 0) {
            return result;
        }

        String mountID = selection.get(0).getMountID();

        for (ExplorerFileStore file : selection) {
            if (!mountID.equals(file.getMountID())) {
                LOGGER.coding("Method must be called with identical mountIDs"
                        + " in the files.");
            }
            // don't be case sensitive
            String p = file.getFullName().toLowerCase();
            boolean isChild = false;
            Iterator<ExplorerFileStore> iter = result.iterator();
            while (iter.hasNext()) {
                ExplorerFileStore resultF = iter.next();
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
     * list (either directly or in any sub directory of the list)
     *
     * @param selected the list to return contained workflows from
     * @return a new list with workflows contained (directly or indirectly) in
     *         the argument
     */
    protected List<ExplorerFileStore> getContainedWorkflows(
            final List<? extends IFileStore> selected) {
        List<ExplorerFileStore> result = new LinkedList<ExplorerFileStore>();
        for (IFileStore f : selected) {
            if (!(f instanceof ExplorerFileStore)) {
                if (ExplorerFileStore.isWorkflow(f)) {
                    result.add((ExplorerFileStore)f);
                } else if (f.fetchInfo().isDirectory()) {
                    try {
                        IFileStore[] children = f.childStores(EFS.NONE, null);
                        result.addAll(getContainedWorkflows(Arrays
                                .asList(children)));
                    } catch (CoreException e) {
                        // ignore - no workflows contained.
                    }
                } // else ignore
            }
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

}
