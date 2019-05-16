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
 *   May 14, 2019 (hornm): created
 */
package org.knime.workbench.explorer.dbworkspace;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.OpenWorkflowAction;

/**
 *
 * @author hornm
 * @since 8.4
 */
public class DBContentProvider extends AbstractContentProvider {

    /**
     * @param myCreator
     * @param id
     */
    public DBContentProvider(final AbstractContentProviderFactory myCreator, final String id) {
        super(myCreator, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof DBFileStore) {
            String fullName = ((DBFileStore)element).getFullName();
            return "/".equals(fullName);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String saveState() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getMountID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DBFileStore getFileStore(final String fullPath) {
        if ("/".equals(fullPath)) {
            return new DBFileStore(getMountID(), fullPath);
        } else {
            return new DBFileStore(DB.getWorkflowFromMongo(fullPath).getWorkflow(), getMountID(), fullPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalExplorerFileStore fromLocalFile(final File file) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addContextMenuActions(final ExplorerView view, final IMenuManager manager, final Set<String> visibleMountIDs,
        final Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selection) {
        manager.add(new OpenWorkflowAction(view));
        manager.add(new Separator());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateDrop(final AbstractExplorerFileStore target, final int operation, final TransferData transferType) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performDrop(final ExplorerView view, final Object data, final AbstractExplorerFileStore target, final int operation) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dragStart(final List<AbstractExplorerFileStore> fileStores) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHostMetaNodeTemplates() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHostDataFiles() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsSnapshots() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore[] getChildren(final Object parentElement) {
        return getElements(parentElement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DBFileStore[] getElements(final Object inputElement) {
        if (inputElement instanceof DBFileStore) {
            String fullName = ((DBFileStore)inputElement).getFullName();
            if ("/".equals(fullName)) {
//                return DB.getAllWorkflows().stream().map(path -> {
//                    WorkflowBundle w = DB.getWorkflow(path);
//                    return new DBFileStore(w, getMountID(), "/" + w.getWorkflow().getName());
//                }).toArray(l -> new DBFileStore[l]);

                return DB.getAllWorkflowsFromMongo().stream().map(w -> {
                    return new DBFileStore(w, getMountID(), w.getId());
                }).toArray(l -> new DBFileStore[l]);

            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore getParent(final Object element) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemote() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWritable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean copyOrMove(final ExplorerView view, final List<AbstractExplorerFileStore> fileStores,
        final AbstractExplorerFileStore targetDir, final boolean performMove) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performDownloadAsync(final RemoteExplorerFileStore source, final LocalExplorerFileStore target,
        final boolean deleteSource, final AfterRunCallback afterRunCallback) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performUploadAsync(final LocalExplorerFileStore source, final RemoteExplorerFileStore target, final boolean deleteSource,
        final boolean excludeDataInWorkflows, final AfterRunCallback callback) throws CoreException {
    }

}
