/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.workbench.explorer.view.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.quickform.in.QuickFormInputNode;
import org.knime.core.util.Pair;

/**
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @author Dominik Morent, KNIME.com AG, Zurich
 * @since 3.1
 */
public class QuickformExecuteWizard extends Wizard {
    private static final String EXEC_PAGE_NAME = "QuickformExecuteWizardPage_";
    private final WorkflowManager m_wfm;
    private int m_numExecPages = 0;
    private final Map<String, Pair<WorkflowManager, Map<NodeID, QuickFormInputNode>>> m_execPageMap;

    /**
     * Creates the wizard.
     * @param wfm underling (parent) workflow manager
     */
    public QuickformExecuteWizard(final WorkflowManager wfm) {
        super();
        setWindowTitle("Quickform wizard for " + wfm.getDisplayLabel());
        m_wfm = wfm;
        m_execPageMap = new HashMap<String, Pair<WorkflowManager, Map<NodeID, QuickFormInputNode>>>();
        setForcePreviousAndNextButtons(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        // show/init credentials and global variables
        addPage(new QuickformExecuteStartWizardPage(this));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        /* The wizard can be finished at any point. In this case the default
         * values are used for all remaining quickform nodes. */
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        if (m_execPageMap.containsKey(page.getName())) {
            Pair<WorkflowManager, Map<NodeID, QuickFormInputNode>> prevWaiting
                    = m_execPageMap.get(page.getName());
            WorkflowManager wfm = prevWaiting.getFirst();
            Map<NodeID, QuickFormInputNode> nodes = prevWaiting.getSecond();
            if (wfm != null && nodes != null) {
                executeUpToNodes(wfm, nodes.keySet().toArray(new NodeID[0]));
            }
        }
        Pair<WorkflowManager, Map<NodeID, QuickFormInputNode>> waiting
                = stepExecution();
        IWizardPage nextPage = super.getNextPage(page);
        if (nextPage == null) {
            // dynamically create new pages if necessary
            nextPage = new QuickformExecuteWizardPage(this, waiting.getSecond(),
                    EXEC_PAGE_NAME + m_numExecPages++);
            addPage(nextPage);
            m_execPageMap.put(nextPage.getName(), waiting);
        }
        return nextPage;
    }

    /**
     * @param page the wizard page to reset its predecessor nodes
     */
    public void resetNodesOfPreviousPage(final IWizardPage page) {
        IWizardPage previousPage = super.getPreviousPage(page);
        if (previousPage != null) {
            String name = previousPage.getName();
            if (m_execPageMap.containsKey(name)) {
                Pair<WorkflowManager, Map<NodeID, QuickFormInputNode>> pair
                        = m_execPageMap.get(name);
                resetNodes(pair.getFirst(),
                        pair.getSecond().keySet().toArray(new NodeID[0]));
            }
        }
    }


    /**
     * Performs one execution step. First, execute all non-quickform nodes
     * before finding the next set of quickform nodes.
     */
    private Pair<WorkflowManager, Map<NodeID, QuickFormInputNode>>
            stepExecution() {
        m_wfm.stepExecutionUpToNodeType(QuickFormInputNode.class,  QuickFormInputNode.NOT_HIDDEN_FILTER);
        // create new runnable
        final IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    // call the worker method
                    m_wfm.waitWhileInExecution(0, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    // no op
                } finally {
                    monitor.done();
                }
            }
        };
        runRun(op);
        WorkflowManager localWFM = m_wfm.findNextWaitingWorkflowManager(
                QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        Map<NodeID, QuickFormInputNode> waitingNodes = Collections.emptyMap();
        if (localWFM != null) {
            // find all quickform input nodes and update meta dialog
            waitingNodes = localWFM.findWaitingNodes(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        }
        return new Pair<WorkflowManager, Map<NodeID, QuickFormInputNode>>(
                localWFM, waitingNodes);
    }

    private void executeUpToNodes(final WorkflowManager wfm,
            final NodeID[] qnodes) {
        if (qnodes.length > 0 && wfm != null) {
            for (NodeID nodeID : qnodes) {
                wfm.resetAndConfigureNode(nodeID);
            }
            wfm.executeUpToHere(qnodes);
            // create new runnable
            final IRunnableWithProgress op = new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException {
                    try {
                        // call the worker method
                        wfm.waitWhileInExecution(0, TimeUnit.SECONDS);
                    } catch (InterruptedException ie) {
                        // no op
                    } finally {
                        monitor.done();
                    }
                }
            };
            runRun(op);
        }
    }

    /**
     * Performs one execution step backwards. Reset the last set of
     * quickform nodes.
     */
    private void resetNodes(final WorkflowManager wfm,
            final NodeID[] qnodes) {
        if (wfm != null && qnodes != null) {
            for (NodeID nodeID : qnodes) {
                NodeContainer nc = wfm.findNodeContainer(nodeID);
                nc.getParent().resetAndConfigureNode(nodeID);
            }
            // create new runnable
            final IRunnableWithProgress op = new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException {
                    try {
                        // call the worker method
                        wfm.waitWhileInExecution(0, TimeUnit.SECONDS);
                    } catch (InterruptedException ie) {
                        // no op
                    } finally {
                        monitor.done();
                    }
                }
            };
            runRun(op);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean performFinish() {
        // create new runnable
        final IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    // call the worker method
                    m_wfm.executeAll();
                } finally {
                    monitor.done();
                }
            }
        };
        return runRun(op);
    }

    private boolean runRun(final IRunnableWithProgress op) {
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // get the exception that issued this async exception
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error",
                    realException.getMessage());
            return true;
        }
        return true;
    }

    /**
     * @return the root workflow manager
     */
    WorkflowManager getWorkflowManager() {
        return m_wfm;
    }

}
