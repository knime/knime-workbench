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
 *   Nov 7, 2017 (ferry.abt): created
 */
package org.knime.workbench.editor2.subnode;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.port.report.ReportUtil;
import org.knime.core.node.workflow.SubNodeContainer;

/**
 * A composite to configure report generation (enable/disable, page size etc.)
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class ReportUsageComposite extends Composite {

    private Button m_enableReportOutputPortButton;

    /**
     * @param parent of the composite
     * @param subNodeContainer the container of the wrapped meta node
     */
    public ReportUsageComposite(final Composite parent,
        final SubNodeContainer subNodeContainer) {
        super(parent, SWT.NONE);

        setLayout(new GridLayout(1, true));
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
        gridData.grabExcessVerticalSpace = false;
        setLayoutData(gridData);
        createNodeGrid(subNodeContainer);
    }

    private void createNodeGrid(final SubNodeContainer subNodeContainer) {
        Composite composite = new Composite(this, SWT.NONE);
        final var layout = new RowLayout();
        layout.center = true;
        layout.justify = true;
        layout.pack = true;
        layout.fill = false;
        composite.setLayout(layout);
        final var reportConfig = subNodeContainer.getReportConfiguration();

        //titles
        m_enableReportOutputPortButton = new Button(composite, SWT.CHECK);
        final var isReportingExtensionInstalled = ReportUtil.isReportingExtensionInstalled();
        m_enableReportOutputPortButton.setEnabled(isReportingExtensionInstalled);
        String text = "Enable Report Output";
        if (!isReportingExtensionInstalled) {
            text = text.concat(" (requires Reporting extension)");
        }

        m_enableReportOutputPortButton.setText(text);
        m_enableReportOutputPortButton.setSelection(reportConfig.isPresent());
    }

    /**
     * @return true if the report generation checker is checked.
     */
    boolean isEnableReportOutput() {
        return m_enableReportOutputPortButton.getSelection();
    }
}
