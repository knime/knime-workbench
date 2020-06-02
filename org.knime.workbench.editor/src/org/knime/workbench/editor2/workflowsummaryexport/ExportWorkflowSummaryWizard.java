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
 *   May 20, 2020 (hornm): created
 */
package org.knime.workbench.editor2.workflowsummaryexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.workflowsummary.WorkflowSummaryConfiguration;
import org.knime.core.util.workflowsummary.WorkflowSummaryGenerator;

/**
 * Wizard to export the workflow summary file (xml and json) for the currently opened and selected workflow.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class ExportWorkflowSummaryWizard extends Wizard implements IExportWizard {

    private final ExportWorkflowSummaryWizardPage m_page;

    private WorkflowManager m_wfm;

    ExportWorkflowSummaryWizard(final WorkflowManager wfm) {
        super();
        m_wfm = wfm;
        setWindowTitle("Export workflow summary");
        m_page = new ExportWorkflowSummaryWizardPage(!wfm.getNodeContainerState().isExecuted());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        addPage(m_page);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_page.isPageComplete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        String fileDestination = m_page.getFile();
        if (fileDestination.isEmpty()) {
            m_page.setErrorMessage("No file specified!");
            return false;
        }

        File outFile = new File(fileDestination);
        if (outFile.exists()) {
            // if it exists we have to check if we can write to:
            if (!outFile.canWrite() || outFile.isDirectory()) {
                // display error
                m_page.setErrorMessage("Cannot write to specified file");
                return false;
            }
            boolean overwrite = MessageDialog.openQuestion(getShell(), "File already exists...",
                "File already exists.\nDo you want to overwrite the " + "specified file ?");
            if (!overwrite) {
                return false;
            }
        }

        // Do the actual export
        try (OutputStream out = new FileOutputStream(new File(fileDestination))) {
            WorkflowSummaryGenerator.generate(m_wfm, out, WorkflowSummaryConfiguration.builder(m_page.format())
                .includeExecutionInfo(m_page.includeExecInfo()).build());
            return true;
        } catch (IOException e) {
            String message = "A problem occurred while writing workflow summary: " + e.getMessage();
            NodeLogger.getLogger(getClass()).error(message, e);
            m_page.setErrorMessage(message);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
        m_wfm = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        //
    }

}
