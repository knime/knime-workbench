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
 * -------------------------------------------------------------------
 *
 * History
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.KNIMEJob;
import org.knime.core.util.workflowprogress.WorkflowProgress;
import org.knime.core.util.workflowprogress.WorkflowProgressMonitor;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.osgi.framework.FrameworkUtil;

/**
 * Shows the progress of the opened workflow.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class ShowProgressAction extends AbstractNodeAction {

    /** unique ID for this action. * */
    public static final String ID = "knime.action.showprogress";

    /**
     *
     * @param editor The workflow editor
     */
    public ShowProgressAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Show Progress";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/showProgress.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/showProgress.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Shows the overall progress of the currently executing workflow.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean internalCalculateEnabled() {
        return getManager() != null && getManager().getNodeContainerState().isExecutionInProgress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        final WorkflowManager wfm = getManager();
        final Job workflowProgressJob =
            new KNIMEJob("Workflow Progress '" + wfm.getName() + "'", FrameworkUtil.getBundle(getClass())) {

                private final Semaphore m_semaphore = new Semaphore(1);

                final WorkflowProgressMonitor m_wfProgressMon = new WorkflowProgressMonitor(wfm);

                {
                    m_wfProgressMon.addProgressChangedListener(() -> {
                        m_semaphore.release();
                    });
                }

                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    WorkflowProgress p = m_wfProgressMon.getProgress();
                    final int totalNumberOfNodeExecutions =
                        p.getNumberOfExecutingNodes() + p.getNumberOfExecutedNodes() + p.getNumberOfFailedNodes();
                    monitor.beginTask("Overall finished node executions", totalNumberOfNodeExecutions);
                    //TODO info when progress cannot be reported accurately

                    int lastNumberOfFinishedNodes = totalNumberOfNodeExecutions - p.getNumberOfExecutingNodes();
                    monitor.worked(lastNumberOfFinishedNodes);
                    while (true) {
                        try {
                            m_semaphore.acquire();
                        } catch (InterruptedException e) {
                            m_wfProgressMon.shutdown();
                            return Status.CANCEL_STATUS;
                        }
                        if (monitor.isCanceled()) {
                            m_wfProgressMon.shutdown();
                            return Status.CANCEL_STATUS;
                        }
                        int currentNumberOfFinishedNodes =
                            totalNumberOfNodeExecutions - m_wfProgressMon.getProgress().getNumberOfExecutingNodes();
                        if (currentNumberOfFinishedNodes == totalNumberOfNodeExecutions) {
                            m_wfProgressMon.shutdown();
                            return Status.OK_STATUS;
                        } else {
                            monitor.worked(currentNumberOfFinishedNodes - lastNumberOfFinishedNodes);
                            lastNumberOfFinishedNodes = currentNumberOfFinishedNodes;
                            m_semaphore.release();
                        }
                    }
                }
            };
        workflowProgressJob.schedule();
        //TODO show/open progress view
    }
}
