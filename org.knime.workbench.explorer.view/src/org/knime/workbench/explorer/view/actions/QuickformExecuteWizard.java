/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.quickform.in.QuickFormInputNode;

/**
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 3.1
 */
public class QuickformExecuteWizard extends Wizard {


    /**
     * Creates the wizard.
     * @param wfm underling (parent) workflow manager
     */
    public QuickformExecuteWizard(final WorkflowManager wfm) {
        super();
        m_wfm = wfm;
        addPages();
        setWindowTitle("Quickform wizard for " + wfm.getDisplayLabel());
    }

    @Override
    public void addPages() {
        m_wfm.stepExecutionUpToNodeType(QuickFormInputNode.class);
        addPage(new QuickformExecuteStartWizardPage(this));
        // show/init credentials and global variables
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        addPage(page);
        return page;
    }

    private final WorkflowManager m_wfm;
    private Map<NodeID, QuickFormInputNode> m_qnodes = Collections.emptyMap();
    private WorkflowManager m_localWFM = null;

    /**
     * Performs one execution step. First, execute all waiting quickform nodes
     * and secondly, all non-quickform nodes before finding the next set of
     * quickform nodes.
     */
    void stepExecution() {
        final NodeID[] qnodes = m_qnodes.keySet().toArray(new NodeID[0]);
        if (qnodes.length > 0 && m_localWFM != null) {
            m_localWFM.executeUpToHere(qnodes);
            // create new runnable
            final IRunnableWithProgress op = new IRunnableWithProgress() {
                private final WorkflowManager localWFM = m_localWFM;
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException {
                    try {
                        // call the worker method
                        localWFM.waitWhileInExecution(0, TimeUnit.SECONDS);
                    } catch (InterruptedException ie) {
                        // no op
                    } finally {
                        monitor.done();
                    }
                }
            };
            runRun(op);
        }
        m_wfm.stepExecutionUpToNodeType(QuickFormInputNode.class);
        // create new runnable
        final IRunnableWithProgress op = new IRunnableWithProgress() {
           private final WorkflowManager wfm = m_wfm;
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
        m_localWFM =
            m_wfm.findNextWaitingWorkflowManager(QuickFormInputNode.class);
        if (m_localWFM == null) {
            m_qnodes = Collections.emptyMap();
        } else {
            // find all quickform input nodes and update meta dialog
            m_qnodes = m_localWFM.findWaitingNodes(QuickFormInputNode.class);
        }
    }

    /**
     * @return all QuickForm nodes currently waiting for being executed
     */
    Map<NodeID, QuickFormInputNode> findQuickformNodes() {
        return m_qnodes;
    }

    /**
     * @return underlying workflow manager (parent)
     */
    WorkflowManager getWorkflowManager() {
        return m_wfm;
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

}
