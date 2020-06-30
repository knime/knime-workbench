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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
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

    private static String LAST_DEST = null;

    private static SummaryFormat LAST_FORMAT = SummaryFormat.JSON;

    private static boolean LAST_INCLUDE_INFO = false;

    private Button m_includeExecInfo;

    private boolean m_containsNonExecutedNodes;

    private Button m_jsonFormat;

    private Button m_xmlFormat;

    ExportWorkflowSummaryWizardPage(final boolean containsNonExecutedNodes) {
        super("Export workflow summary as json or xml", "Please select the destination", LAST_DEST);
        m_containsNonExecutedNodes = containsNonExecutedNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        super.createControl(parent);
        Composite container = (Composite)getControl();

        Group format = new Group(container, SWT.NONE);
        format.setLayout(new RowLayout(SWT.HORIZONTAL));
        final GridData gridDataFormat = new GridData(GridData.FILL_HORIZONTAL);
        format.setLayoutData(gridDataFormat);
        Label formatLabel = new Label(format, SWT.NONE);
        formatLabel.setText("Export format:");
        m_jsonFormat = new Button(format, SWT.RADIO);
        m_jsonFormat.setText("JSON");
        m_xmlFormat = new Button(format, SWT.RADIO);
        m_xmlFormat.setText("XML");
        final ExportWorkflowSummaryWizardPage thisPage = this;
        m_jsonFormat.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectJSONFormat(thisPage);
            }
        });
        m_xmlFormat.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectXMLFormat(thisPage);
            }
        });
        if (LAST_FORMAT == SummaryFormat.JSON) {
            m_jsonFormat.setSelection(true);
            selectJSONFormat(this);
        } else {
            m_xmlFormat.setSelection(true);
            selectXMLFormat(this);
        }

        final Group group = new Group(container, SWT.NONE);
        group.setLayout(new GridLayout());
        final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);

        m_includeExecInfo = new Button(group, SWT.CHECK);
        m_includeExecInfo.setSelection(LAST_INCLUDE_INFO);
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

    private static void selectJSONFormat(final ExportWorkflowSummaryWizardPage page) {
        page.clearAllFileExtensionFilter();
        page.addFileExtensionFilter("*.json", "workflow summary json");
        page.setFile(adjustFileExtension(page.getFile(), SummaryFormat.JSON));
    }

    private static void selectXMLFormat(final ExportWorkflowSummaryWizardPage page) {
        page.clearAllFileExtensionFilter();
        page.addFileExtensionFilter("*.xml", "workflow summary xml");
        page.setFile(adjustFileExtension(page.getFile(), SummaryFormat.XML));
    }

    private static String adjustFileExtension(final String file, final SummaryFormat format) {
        if (StringUtils.isBlank(file)) {
            return file;
        }
        String fileExtension = format == SummaryFormat.JSON ? ".json" : ".xml";
        String otherExtension = format == SummaryFormat.JSON ? ".xml" : ".json";
        String res;
        if (file.endsWith(otherExtension)) {
            // replace extension
            res = file.substring(0, file.length() - otherExtension.length()) + fileExtension;
        } else if (!file.endsWith(fileExtension)) {
            // append extension
            res = file + fileExtension;
        } else {
            res = file;
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFile() {
        LAST_DEST = super.getFile();
        return LAST_DEST;
    }

    boolean includeExecInfo() {
        LAST_INCLUDE_INFO = m_includeExecInfo.getSelection();
        return LAST_INCLUDE_INFO;
    }

    SummaryFormat format() {
        LAST_FORMAT = m_jsonFormat.getSelection() ? SummaryFormat.JSON : SummaryFormat.XML;
        return LAST_FORMAT;
    }

}
