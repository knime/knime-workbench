/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   24.08.2011 (Peter Ohl): created
 */
package org.knime.workbench.explorer.view.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
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

    private Label m_errMsg;

    private Button m_mergeGUI;

    private boolean m_merge;

    private Button m_skipGUI;

//    private boolean m_skip;

    private Button m_overwriteGUI;

    private boolean m_overwrite;

    private Button m_renameGUI;

    private boolean m_rename;

    private Button m_cancel;

    private final boolean m_canWriteDestination;

    /**
     *
     * @param parentShell parent for the dialog
     * @param destination
     */
    public MergeRenameDialog(final Shell parentShell,
            final AbstractExplorerFileStore destination) {
        this(parentShell, destination, true);
    }

    /**
     *
     * @param parentShell parent for the dialog
     * @param destination the destination to merge to
     * @param canWriteDest true if the caller can write to the destination
     */
    public MergeRenameDialog(final Shell parentShell,
            final AbstractExplorerFileStore destination,
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
        createError(overall);

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
            txt.setText("The destination (" + m_destination.getName()
                    + ") already exists on the server. "
                    + "Please select an option to proceed.");
        } else {
            txt.setText("The destination (" + m_destination.getName()
                    + ") already exists on the server and you can't "
                    + "write into. Please provide a new destination name.");
        }
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    }

    private void createTextPanel(final Composite parent) {
        Group border = new Group(parent, SWT.SHADOW_IN);
        border.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        border.setLayout(new GridLayout(1, true));
        border.setText("Server name conflict resolution:");

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
        mergeOptionsPanel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true,
                false));
        mergeOptionsPanel.setLayout(new GridLayout(1, false));
//        m_skipGUI = new Button(mergeOptionsPanel, SWT.CHECK);
//        m_skipGUI.setText("ignore groups/flows without "
//                + "read/download permissions");
//        m_skipGUI.setToolTipText("If the source contains items you have no"
//                + " access to, copy doesn't start without this option");
//        m_skipGUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        m_skipGUI.addListener(SWT.Modify, new Listener() {
//            @Override
//            public void handleEvent(final Event event) {
//                m_skip = m_skipGUI.getSelection();
//            }
//        });
//        m_skipGUI.setEnabled(false);
//        m_skipGUI.setSelection(false);
//        m_skip = false;
        m_overwriteGUI = new Button(mergeOptionsPanel, SWT.CHECK);
        m_overwriteGUI.setText("overwrite existing workflows");
        m_overwriteGUI.setToolTipText("If workflows already exist, copy "
                + "doesn't start without this option.");
        m_overwriteGUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_overwriteGUI.addListener(SWT.Modify, new Listener() {
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
            m_mergeGUI.setToolTipText("You don't have the permissions "
                    + "to write to the destination");
        }

        Composite renamePanel = new Composite(border, SWT.NONE);
        renamePanel
                .setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
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
        m_newNameGUI =
                new Text(renamePanel, SWT.FILL | SWT.SINGLE | SWT.BORDER);
        m_newNameGUI.setText(getAlternativeName());
        m_newNameGUI.setLayoutData(new GridData(SWT.BEGINNING
                | GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
        m_newNameGUI.addListener(SWT.Modify, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                setSelectedAndValidate(m_renameGUI);
            }
        });

        Composite cancelPanel = new Composite(border, SWT.NONE);
        cancelPanel
                .setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
        cancelPanel.setLayout(new GridLayout(2, false));
        m_cancel = new Button(cancelPanel, SWT.RADIO);
        m_cancel.setText("Cancel the operation");
        m_cancel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_cancel.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                setSelectedAndValidate(m_cancel);
            }
        });
    }

    private void createError(final Composite parent) {
        m_errMsg = new Label(parent, SWT.LEFT);
        m_errMsg.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_errMsg.setText("");
        m_errMsg.setForeground(Display.getDefault().getSystemColor(
                SWT.COLOR_RED));
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

        m_cancel.setSelection(choice == m_cancel);
        m_renameGUI.setSelection(choice == m_renameGUI);
        m_newNameGUI.setEnabled(m_renameGUI.getSelection());
        if (m_mergeGUI != null && m_mergeGUI.isEnabled()) {
            m_mergeGUI.setSelection(choice == m_mergeGUI);
            m_skipGUI.setEnabled(m_mergeGUI.getSelection());
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
                if (!ExplorerFileSystem.isValidFilename(m_newName)) {
                    errMsg =
                            "New destination name contains invalid characters ("
                                  + ExplorerFileSystem.getIllegalFilenameChars()
                                  + ")";
                    return;
                }
                AbstractExplorerFileStore parent = m_destination.getParent();
                if (parent.getChild(m_newName).fetchInfo().exists()) {
                    errMsg = "New destination already exists on server.";
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
                m_errMsg.setText("");
                getButton(IDialogConstants.OK_ID).setEnabled(true);
            } else {
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
     * @return the new name or null if not applicable
     */
    public String rename() {
        if (m_rename) {
            return m_newName;
        } else {
            return null;
        }
    }

    /**
     * @return true if merging has been selected, false otherwise
     */
    public boolean merge() {
        return m_merge;
    }

    /**
     * @return true if overwriting has been selected, false otherwise
     */
    public boolean overwrite() {
        return m_merge && m_overwrite;
    }

//    public boolean skip() {
//        return m_merge && m_overwrite;
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

}
