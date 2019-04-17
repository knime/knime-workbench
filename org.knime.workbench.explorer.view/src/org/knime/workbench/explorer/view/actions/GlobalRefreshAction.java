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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.async.AsyncWorkflowManagerUI;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.templates.CollectMetanodeTemplatesAndUpdateNodeRepoJob;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 *
 * @author morent, KNIME AG, Zurich, Switzerland
 */
public class GlobalRefreshAction extends ExplorerAction {
    private static final String TOOLTIP = "Refresh the view";


    /**
     * @param viewer the viewer containing the file stores
     */
    public GlobalRefreshAction(final ExplorerView viewer) {
        super(viewer, "Refresh");
        setImageDescriptor(ImageRepository.getIconDescriptor(SharedImages.Refresh));
        setToolTipText(TOOLTIP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.workbench.explorer.view.action.refresh";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        IStructuredSelection selection = getSelection();
        List<AbstractExplorerFileStore> stores =
                DragAndDropUtils.getExplorerFileStores(selection);
        if (stores == null) {
            getViewer().refresh();
        } else {
            new RefreshJob(stores).schedule();
        }

        //TODO move somewhere else
        new CollectMetanodeTemplatesAndUpdateNodeRepoJob().schedule();
    }


    private static class RefreshJob extends ExplorerJob {
        private final List<AbstractExplorerFileStore> m_stores;

        public RefreshJob(final List<AbstractExplorerFileStore> stores) {
            super("Refreshing " + stores.size() + " resources");
            m_stores = new ArrayList<>(stores);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            monitor.beginTask("Refreshing " + m_stores.size() + " resources", m_stores.size());

            for (AbstractExplorerFileStore file : m_stores) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                monitor.beginTask("Refreshing " + file, 1);
                file.refresh(monitor);

                //refresh remote jobs workflows
                if (file instanceof RemoteExplorerFileStore
                    && ((RemoteExplorerFileStore)file).fetchInfo().isWorkflowJob()) {
                    //if it's a remote workflow job, try refreshing its workflow in case it's opened in the editor
                    NodeContainerUI workflow = ProjectWorkflowMap.getWorkflowUI(file.toURI());
                    if (workflow != null && workflow instanceof AsyncWorkflowManagerUI) {
                        try {
                            ((AsyncWorkflowManagerUI)workflow).refreshAsync(true).get();
                        } catch (InterruptedException | ExecutionException e) {
                            return new Status(IStatus.ERROR, "unknown", "A problem occurred while refreshing workflow.",
                                e);
                        }
                    }
                }
            }

            return Status.OK_STATUS;
        }
    }
}
