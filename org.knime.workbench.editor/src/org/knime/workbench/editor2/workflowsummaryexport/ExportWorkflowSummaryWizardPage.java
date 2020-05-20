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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.knime.core.util.workflowsummary.WorkflowSummaryConfiguration.SummaryFormat;
import org.knime.workbench.core.util.ExportToFilePage;

/**
 * Export workflow summary page to select the destination file and additiona configurations.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class ExportWorkflowSummaryWizardPage extends ExportToFilePage {

    private Button m_includeExecInfo;

    private boolean m_containsNonExecutedNodes;

    ExportWorkflowSummaryWizardPage(final SummaryFormat format, final boolean containsNonExecutedNodes) {
        super("Export workflow summary as " + (format == SummaryFormat.XML ? "xml" : "json"),
            "Please select the destination");
        m_containsNonExecutedNodes = containsNonExecutedNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        super.createControl(parent);
        Composite container = (Composite)getControl();

        final Group group = new Group(container, SWT.NONE);
        group.setLayout(new GridLayout());
        final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);

        m_includeExecInfo = new Button(group, SWT.CHECK);
        m_includeExecInfo.setSelection(false);
        m_includeExecInfo.setText("Include execution information");
        Label desc = new Label(group, SWT.NONE);
        desc.setText("Execution information comprises:\n" + " KNIME setup with installed plugins;\n"
            + " Node execution statistics (start time, duration etc.);\n"
            + " Summary on ports (e.g. table dimensions).");
        if (m_containsNonExecutedNodes) {
            Label warning = new Label(group, SWT.NONE);
            warning.setText("Warning: some nodes are not executed.\n"
                + "Execution statistics and port summaries won't be available for those.");
            warning.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        }

    }

    boolean includeExecInfo() {
        return m_includeExecInfo.getSelection();
    }

}
