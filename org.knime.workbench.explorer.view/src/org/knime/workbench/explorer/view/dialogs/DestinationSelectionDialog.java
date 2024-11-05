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
 *   Jan 31, 2023 (leonard.woerteler): created
 */
package org.knime.workbench.explorer.view.dialogs;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.util.CheckUtils;
import org.knime.workbench.explorer.dialogs.MessageJobFilter;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.Validator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;

/**
 * Dialog for selecting a destination in the Mount Table.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 8.9
 */
public final class DestinationSelectionDialog extends SpaceResourceSelectionDialog {

    private boolean m_showExcludeData = true;

    private Button m_excludeDataButton;
    private boolean m_isExcludeData;
    private Composite m_tooltipContainer;

    private AbstractContentProvider m_currentContentProvider;

    /**
     * @param parentShell parent shell
     * @param mountIDs mount IDs to be included
     * @param initialSelection initial selection, may be {@code null}
     */
    public DestinationSelectionDialog(final Shell parentShell, final String[] mountIDs,
        final ContentObject initialSelection) {
        this(parentShell, mountIDs, initialSelection, "Destination", "Upload to...",
            "Select the destination workflow group.",
            "Select the destination group to which the selected element will be uploaded");
    }

    /**
     * @param parentShell parent shell
     * @param mountIDs mount IDs to be included
     * @param initialSelection initial selection, may be {@code null}
     * @param title dialog title
     * @param header dialog header
     * @param description dialog description
     * @param selectWorkflowGroupPrompt prompt which is shown if the user selected something other than a gruop
     */
    public DestinationSelectionDialog(final Shell parentShell, final String[] mountIDs,
        final ContentObject initialSelection, final String title, final String header, final String description,
        final String selectWorkflowGroupPrompt) {
        super(parentShell, mountIDs, initialSelection);
        setValidator(new Validator() {
            @Override
            public String validateSelectionValue(final AbstractExplorerFileStore sel, final String currentName) {
                if (!AbstractExplorerFileStore.isWorkflowGroup(sel)) {
                    return selectWorkflowGroupPrompt;
                }
                return null;
            }
        });
        setFilter(new MessageJobFilter());
        setTitle(title);
        setHeader(header);
        setDescription(description);

        m_currentContentProvider = initialSelection == null || initialSelection.getFileStore() == null ? null
            : initialSelection.getFileStore().getContentProvider();
    }

    /**
     * Whether or not the "Reset Workflows(s) before upload" option should be shown (default is {@code true}).
     *
     * @param showExcludeData new setting
     */
    public void setShowExcludeDataOption(final boolean showExcludeData) {
        m_showExcludeData = showExcludeData;
    }

    @Override
    protected void createCustomFooterField(final Composite parent) {
        if (m_showExcludeData) {
            // Create marginless composite to show tooltip since a disabled checkbox can not trigger any events
            m_tooltipContainer = new Composite(parent, SWT.NONE);
            m_tooltipContainer.setLayout(new FillLayout());
            GridDataFactory.fillDefaults().applyTo(m_tooltipContainer);
            m_excludeDataButton = new Button(m_tooltipContainer, SWT.CHECK);
            m_isExcludeData = m_currentContentProvider != null && m_currentContentProvider.isForceResetOnUpload();
            m_excludeDataButton.setSelection(m_isExcludeData);
            m_excludeDataButton.setText("Reset Workflow(s) before upload");
            m_excludeDataButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    Button b = (Button)e.widget;
                    m_isExcludeData = b.getSelection();
                }
            });
        }
    }

    @Override
    protected void validateSelectionValue() {
        super.validateSelectionValue();
        final AbstractContentProvider ct = getSelection().getContentProvider();
        final boolean changedContentProvider = !ct.equals(m_currentContentProvider);
        m_currentContentProvider = ct;

        if (m_showExcludeData) {
            m_excludeDataButton.setSelection(
                (changedContentProvider && ct.isForceResetOnUpload()) || m_excludeDataButton.getSelection());
            m_excludeDataButton.setEnabled(!ct.isForceResetOnUpload() || ct.isEnableResetOnUploadCheckbox());
            m_isExcludeData = m_excludeDataButton.getSelection();
            m_tooltipContainer.setToolTipText(m_excludeDataButton.getEnabled() ? ""
                : "This option is selected by default as set by the Hub administrator.");
        }
    }

    /**
     * @return the selected destination
     */
    public SelectedDestination getSelectedDestination() {
        return new SelectedDestination(getSelection(), m_isExcludeData);
    }

    /** Represents a selected destination folder and the property whether the data is to be excluded before upload. */
    public static final class SelectedDestination {
        private final AbstractExplorerFileStore m_destination;
        private final boolean m_isExcludeData;

        SelectedDestination(final AbstractExplorerFileStore destination, final boolean isExcludeData) {
            m_destination = CheckUtils.checkArgumentNotNull(destination, "Destination must not be null");
            m_isExcludeData = isExcludeData;
        }

        /** @return the destination, not null. */
        public AbstractExplorerFileStore getDestination() {
            return m_destination;
        }

        /** @return the isExcludeData checkbox property. */
        public boolean isExcludeData() {
            return m_isExcludeData;
        }
    }
}
