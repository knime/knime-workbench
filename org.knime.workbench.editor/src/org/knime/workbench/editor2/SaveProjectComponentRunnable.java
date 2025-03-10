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
 *
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.LockFailedException;

/**
 * Runnable that saves the current component to a new or the same location.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class SaveProjectComponentRunnable extends AbstractSaveRunnable {
    private final WorkflowContextV2 m_newContext;

    SaveProjectComponentRunnable(final WorkflowEditor editor, final StringBuilder exceptionMessage,
        final IProgressMonitor monitor, final WorkflowContextV2 newContext) {
        super(editor, exceptionMessage, monitor);
        m_newContext = CheckUtils.checkArgumentNotNull(newContext, "workflow context of the target location");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected File getSaveLocation() {
        return m_newContext.getExecutorInfo().getLocalWorkflowPath().toFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final WorkflowManager wfm, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, LockFailedException {
        try {
            final File newLocation = getSaveLocation();
            final SubNodeContainer parent = (SubNodeContainer)wfm.getDirectNCParent();
            // AP-23528: Component Editor wrongly overwrites component directory when saving
            if (m_newContext.equals(wfm.getContextV2())) {
                CheckUtils.checkState(newLocation.equals(parent.getNodeContainerDirectory().getFile()),
                    "same context but save locations: %s vs %s", newLocation.getAbsolutePath(),
                    parent.getNodeContainerDirectory().getFile().getAbsolutePath());
                // calling the saveTemplate without folder argument will make sure:
                // - node folders of no longer existing nodes are deleted
                // - previous node folders are kept (e.g. "Table Row To Variable (#3)" vs. "Table Row to Variable (#3)")
                //   (watch case of "to")
                parent.saveTemplate(exec);
            } else {
                parent.saveAsTemplate(newLocation, exec, null);
            }

            // component is relocated, set the project workflow manager's context accordingly
            wfm.setWorkflowContext(m_newContext);
            wfm.getNodeContainerDirectory().changeRoot(newLocation);
        } catch (InvalidSettingsException e) {
            throw new IOException(e);
        }
    }
}
