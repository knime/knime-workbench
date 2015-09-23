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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class MergeRenameDialog extends Dialog {

    private final AbstractExplorerFileStore m_destination;

    private String m_newName = "";

    private Text m_newNameGUI;

    private CLabel m_errMsg;

    private Button m_mergeGUI;

    private boolean m_merge;

    private Button m_overwriteGUI;

    private boolean m_overwrite;

    private Button m_renameGUI;

    private boolean m_rename;

    private SnapshotPanel m_snapshotPanel;

    private final boolean m_canWriteDestination;

    /**
     *
     * @param parentShell parent for the dialog
     * @param destination
     */
    public MergeRenameDialog(final Shell parentShell, final AbstractExplorerFileStore destination) {
        this(parentShell, destination, true);
    }

    /**
     *
     * @param parentShell parent for the dialog
     * @param destination the destination to merge to
     * @param canWriteDest true if the caller can write to the destination
     */
    public MergeRenameDialog(final Shell parentShell, final AbstractExplorerFileStore destination,
        final boolean canWriteDest) {
        super(parentShell);
        m_destination = destination;
        m_canWriteDestination = canWriteDest;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Confirm Merge/Rename");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NONE);
        GridData fillBoth = new GridData(GridData.FILL_BOTH);
        overall.setLayoutData(fillBoth);
        overall.setLayout(new GridLayout(1, true));

        createHeader(overall);
        createTextPanel(overall);
        createSnapshotPanel(overall);
        createError(overall);

        int height = (m_snapshotPanel != null) ? 370 : 280;
        getShell().setMinimumSize(490, height);

        return overall;
    }

    private void createHeader(final Composite parent) {
        Composite header = new Composite(parent, SWT.FILL);
        Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
        header.setBackground(white);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        header.setLayoutData(gridData);
        header.setLayout(new GridLayout(2, false));
        Label exec = new Label(header, SWT.NONE);
        exec.setBackground(white);
        exec.setText("Confirm merge or rename");
        FontData[] fd = parent.getFont().getFontData();
        for (FontData f : fd) {
            f.setStyle(SWT.BOLD);
            f.setHeight(f.getHeight() + 2);
        }
        exec.setFont(new Font(parent.getDisplay(), fd));
        exec.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Label mkdirIcon = new Label(header, SWT.NONE);
        mkdirIcon.setBackground(white);
        Label txt = new Label(header, SWT.NONE);
        txt.setBackground(white);
        if (m_canWriteDestination) {
            txt.setText("The destination (" + m_destination.getName() + ") already exists. "
                + "Please select an option to proceed.");
        } else {
            txt.setText("The destination (" + m_destination.getName() + ") already exists and you can't "
                + "write into. Please provide a new destination name.");
        }
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    }

    private void createTextPanel(final Composite parent) {
        Group border = new Group(parent, SWT.SHADOW_NONE);
        border.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        border.setLayout(new GridLayout(1, true));

        Composite mergePanel = new Composite(border, SWT.NONE);
        mergePanel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        mergePanel.setLayout(new GridLayout(1, false));
        m_mergeGUI = new Button(mergePanel, SWT.RADIO);
        m_mergeGUI.setText("Merge destination and source workflow group");
        m_mergeGUI.setToolTipText("Not existing items will be added to the "
            + "destination, existing ones may be overwritten.");
        m_mergeGUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_mergeGUI.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                setSelectedAndValidate(m_mergeGUI);
            }
        });
        Composite mergeOptionsPanel = new Composite(mergePanel, SWT.NONE);
        mergeOptionsPanel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        mergeOptionsPanel.setLayout(new GridLayout(1, false));
        m_overwriteGUI = new Button(mergeOptionsPanel, SWT.CHECK);
        m_overwriteGUI.setText("overwrite existing workflows");
        m_overwriteGUI.setToolTipText("If workflows already exist, copy " + "doesn't start without this option.");
        m_overwriteGUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_overwriteGUI.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                m_overwrite = m_overwriteGUI.getSelection();
            }
        });
        m_overwriteGUI.setEnabled(false);
        m_overwriteGUI.setSelection(false);
        m_overwrite = false;
        if (!m_canWriteDestination) {
            m_mergeGUI.setEnabled(false);
            m_mergeGUI.setToolTipText("You don't have the permissions " + "to write to the destination");
        }

        Composite renamePanel = new Composite(border, SWT.NONE);
        renamePanel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        renamePanel.setLayout(new GridLayout(2, false));
        m_renameGUI = new Button(renamePanel, SWT.RADIO);
        m_renameGUI.setText("Store it with the new name:");
        m_renameGUI.setLayoutData(new GridData(SWT.BEGINNING));
        m_renameGUI.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                setSelectedAndValidate(m_renameGUI);
            }
        });
        m_newNameGUI = new Text(renamePanel, SWT.FILL | SWT.SINGLE | SWT.BORDER);
        m_newNameGUI.setText(getAlternativeName());
        m_newNameGUI.setLayoutData(new GridData(SWT.BEGINNING | GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        m_newNameGUI.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                setSelectedAndValidate(m_renameGUI);
            }
        });
    }

    private void createSnapshotPanel(final Composite parent) {
        if (m_destination.getContentProvider().supportsSnapshots()) {
            m_snapshotPanel = new SnapshotPanel(parent, SWT.NONE);
            m_snapshotPanel.setEnabled(m_overwriteGUI.getSelection());
            m_overwriteGUI.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(final Event event) {
                    m_snapshotPanel.setEnabled(m_overwriteGUI.getSelection());
                }
            });
        }
    }

    private void createError(final Composite parent) {
        m_errMsg = new CLabel(parent, SWT.LEFT);
        m_errMsg.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_errMsg.setText("");
        m_errMsg.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        m_errMsg.setImage(ImageRepository.getIconImage(SharedImages.Error));
        m_errMsg.setVisible(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        // default: choice rename
        setSelectedAndValidate(m_renameGUI);
    }

    private void setSelectedAndValidate(final Button choice) {
        String errMsg = "unknown error";

        m_renameGUI.setSelection(choice == m_renameGUI);
        m_newNameGUI.setEnabled(m_renameGUI.getSelection());
        if (m_mergeGUI != null && m_mergeGUI.isEnabled()) {
            m_mergeGUI.setSelection(choice == m_mergeGUI);
            //            m_skipGUI.setEnabled(m_mergeGUI.getSelection());
            m_overwriteGUI.setEnabled(m_mergeGUI.getSelection());
        }
        m_merge = m_mergeGUI != null && m_mergeGUI.getSelection();
        m_rename = m_renameGUI.getSelection();
        m_newName = m_newNameGUI.getText().trim();

        try {
            if (m_renameGUI.getSelection()) {
                if (m_newName.isEmpty()) {
                    errMsg = "Please enter a new destination name.";
                    // finalize sets the messages
                    return;
                }
                errMsg = ExplorerFileSystem.validateFilename(m_newName);
                if (errMsg != null) {
                    return;
                }
                AbstractExplorerFileStore parent = m_destination.getParent();
                if (parent.getChild(m_newName).fetchInfo().exists()) {
                    errMsg = "New destination already exists.";
                    return;
                }
            }
            errMsg = null;
            return;
        } catch (Throwable t) {
            if (t.getMessage() != null && !t.getMessage().isEmpty()) {
                errMsg = t.getMessage();
            }
            return;
        } finally {
            if (errMsg == null || errMsg.isEmpty()) {
                m_errMsg.setVisible(false);
                m_errMsg.setText("");
                getButton(IDialogConstants.OK_ID).setEnabled(true);
            } else {
                m_errMsg.setVisible(true);
                m_errMsg.setText(errMsg);
                getButton(IDialogConstants.OK_ID).setEnabled(false);
            }
        }
    }

    private String getAlternativeName() {
        String name = m_destination.getName();
        AbstractExplorerFileStore parent = m_destination.getParent();
        if (parent == null) {
            return name;
        }
        int cnt = 1;
        while (parent.getChild(name).fetchInfo().exists()) {
            name = m_destination.getName() + "(" + (++cnt) + ")";
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Returns information about the user selection.
     *
     * @return an overwrite-and-merge information object
     * @since 6.0
     */
    public OverwriteAndMergeInfo getInfo() {
        return new OverwriteAndMergeInfo(m_rename ? m_newName : null, m_merge, m_merge && m_overwrite,
            m_snapshotPanel != null ? m_snapshotPanel.createSnapshot() : false, m_snapshotPanel != null
                ? m_snapshotPanel.getComment() : null);
    }
}
