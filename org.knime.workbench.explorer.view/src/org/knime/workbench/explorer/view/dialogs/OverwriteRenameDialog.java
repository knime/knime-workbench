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


import java.util.Collections;
import java.util.Set;

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
public class OverwriteRenameDialog extends Dialog {
    private final AbstractExplorerFileStore m_destination;

    private String m_newName = "";

    private Text m_newNameGUI;

    private CLabel m_errMsg;

    private Button m_overwriteGUI;

    private boolean m_overwrite;

    private Button m_renameGUI;

    private boolean m_rename;

    private final boolean m_canWriteDestination;

    private final boolean m_multiple;

    private boolean m_isOverwriteDefault;

    private SnapshotPanel m_snapshotPanel;

    private final Set<AbstractExplorerFileStore> m_forbiddenStores;

    /**
     *
     * @param parentShell parent for the dialog
     * @param destination the destination to select
     */
    public OverwriteRenameDialog(final Shell parentShell,
            final AbstractExplorerFileStore destination) {
        this(parentShell, destination, true);
    }

    /**
    *
    * @param parentShell parent for the dialog
     * @param destination the destination to select
     * @param canWriteDest true, if the destination is writable
    */
   public OverwriteRenameDialog(final Shell parentShell,
           final AbstractExplorerFileStore destination,
           final boolean canWriteDest) {
       this(parentShell, destination, canWriteDest, false);
   }
    /**
     *
     * @param parentShell parent for the dialog
     * @param destination the destination to select
     * @param canWriteDest true, if the destination is writable
     * @param multiple true, if the dialog is potentially show for multiple
     *      files. For this case the cancel button is named cancel all and an
     *      additional skip option is added.
     */
    public OverwriteRenameDialog(final Shell parentShell,
            final AbstractExplorerFileStore destination,
            final boolean canWriteDest, final boolean multiple) {
        this(parentShell, destination, canWriteDest, multiple,
                Collections.EMPTY_SET);
    }

    /**
    *
    * @param parentShell parent for the dialog
    * @param destination the destination to select
    * @param canWriteDest true, if the destination is writable
    * @param multiple true, if the dialog is potentially show for multiple
    *      files. For this case the cancel button is named cancel all and an
    *      additional skip option is added.
     * @param forbiddenStores stores that cannot be chosen as destination
    */
    public OverwriteRenameDialog(final Shell parentShell,
            final AbstractExplorerFileStore destination,
            final boolean canWriteDest,
            final boolean multiple,
            final Set<AbstractExplorerFileStore> forbiddenStores) {
        super(parentShell);
        m_destination = destination;
        m_canWriteDestination = canWriteDest;
        m_multiple = multiple;
        m_forbiddenStores = forbiddenStores;
    }

    /** Can be called right after construction to programmatically set the
     * overwrite action to be the default (default is rename).
     * @param value ...
     * @since 3.0*/
    public void setOverwriteAsDefault(final boolean value) {
        m_isOverwriteDefault = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Confirm Overwrite/Rename");
    }

    /**
     * @param newName set a default new name (default new name is with
     *            additional (2))
     */
    public void setNewName(final String newName) {
        m_newName = newName;
    }

    /**
     * @return the password entered
     */
    public String getNewName() {
        return m_newName;
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
        getShell().setMinimumSize(470, height);

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
        exec.setText("Confirm overwrite or rename");
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
        txt.setText("The destination (" + m_destination.getName()
                + ") already exists.\n"
                + "Please select an option to proceed.");
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    }

    private void createTextPanel(final Composite parent) {
        Group border = new Group(parent, SWT.NONE);
        border.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
        border.setLayout(new GridLayout(1, true));
        border.setText("Name conflict resolution:");

        Composite overwritePanel = new Composite(border, SWT.NONE);
        overwritePanel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true,
                false));
        overwritePanel.setLayout(new GridLayout(1, false));
        m_overwriteGUI = new Button(overwritePanel, SWT.RADIO);
        m_overwriteGUI.setText("Overwrite the existing item");
        m_overwriteGUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_overwriteGUI.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                setSelectedAndValidate(m_overwriteGUI);
            }
        });
        if (!m_canWriteDestination) {
            m_overwriteGUI.setEnabled(false);
            m_overwriteGUI.setToolTipText("You have no permissions "
                    + "to overwrite the destination");
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
        m_newNameGUI.setText(getAlternativeName(m_destination,
                m_forbiddenStores));
        m_newNameGUI.setLayoutData(new GridData(SWT.BEGINNING
                | GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL));
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
        }
    }

    private void createError(final Composite parent) {
        m_errMsg = new CLabel(parent, SWT.LEFT);
        m_errMsg.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_errMsg.setText("");
        m_errMsg.setForeground(Display.getDefault().getSystemColor(
                SWT.COLOR_RED));
        m_errMsg.setImage(ImageRepository.getImage(SharedImages.Error));
        m_errMsg.setVisible(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        if (m_multiple) {
            createButton(parent, IDialogConstants.YES_TO_ALL_ID, "Apply to all",
                    false);
        }

        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);

        String cancelLabel = IDialogConstants.CANCEL_LABEL;
        if (m_multiple) {
            cancelLabel = "Skip";
        }
        createButton(parent, IDialogConstants.CANCEL_ID,
                cancelLabel, false);
        if (m_multiple) {
            createButton(parent, IDialogConstants.ABORT_ID,
                    IDialogConstants.ABORT_LABEL, false);
        }
        Button defButton = m_isOverwriteDefault ? m_overwriteGUI : m_renameGUI;
        setSelectedAndValidate(defButton);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buttonPressed(final int buttonId) {
        super.buttonPressed(buttonId);
        if (IDialogConstants.YES_TO_ALL_ID == buttonId) {
            setReturnCode(IDialogConstants.YES_TO_ALL_ID);
            close();
        } else if (IDialogConstants.ABORT_ID == buttonId) {
            setReturnCode(IDialogConstants.ABORT_ID);
            close();
        }
    }


    private void setSelectedAndValidate(final Button choice) {
        String errMsg = "unknown error";

        m_renameGUI.setSelection(choice == m_renameGUI);
        m_newNameGUI.setEnabled(m_renameGUI.getSelection());
        if (m_overwriteGUI != null && m_overwriteGUI.isEnabled()) {
            m_overwriteGUI.setSelection(choice == m_overwriteGUI);
        }
        m_overwrite = m_overwriteGUI != null && m_overwriteGUI.getSelection();
        if (m_snapshotPanel != null) {
            m_snapshotPanel.setEnabled(m_overwrite);
        }

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
                AbstractExplorerFileStore dest = parent.getChild(m_newName);
                if (dest.fetchInfo().exists()) {
                    errMsg = "New destination already exists.";
                    return;
                } else if (m_forbiddenStores.contains(dest)) {
                    errMsg = "New destination has already been selected in a"
                        + " previous operation.";
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
            Button yesToAllButton = getButton(IDialogConstants.YES_TO_ALL_ID);
            if (errMsg == null || errMsg.isEmpty()) {
                m_errMsg.setVisible(false);
                getButton(IDialogConstants.OK_ID).setEnabled(true);
                if (yesToAllButton != null) {
                    yesToAllButton.setEnabled(m_overwrite);
                }
            } else {
                m_errMsg.setVisible(true);
                m_errMsg.setText(errMsg);
                getButton(IDialogConstants.OK_ID).setEnabled(false);
                if (yesToAllButton != null) {
                     getButton(IDialogConstants.YES_TO_ALL_ID).setEnabled(
                             m_overwrite);
                }
            }
        }
    }

    /**
     * Adds an index to the file name to make it unique. If the file store represents a file the index is inserted in
     * front of the last dot (to not change the file name extension).
     * @param fileStore the file store to get an alternative name for
     * @param forbiddenStores an optional list of file stores that should not
     *      be used, can be null
     * @return an alternative name for the filestore that does not exist yet
     */
    public static String getAlternativeName(
            final AbstractExplorerFileStore fileStore,
            final Set<AbstractExplorerFileStore> forbiddenStores) {
        String newName = fileStore.getName();
        AbstractExplorerFileStore parent = fileStore.getParent();
        if (parent == null) {
            return newName;
        }

        String origName = newName;
        String extension = "";
        if (fileStore.fetchInfo().isFile()) {
            // do not modify the extension of files
            int dotIdx = origName.lastIndexOf('.');
            if (dotIdx > 0 && dotIdx < origName.length() - 1) {
                extension = origName.substring(dotIdx); // including the dot
                origName = origName.substring(0, dotIdx);
            }
        }
        int cnt = 1;
        AbstractExplorerFileStore child = parent.getChild(newName);
        while (child.fetchInfo().exists() || (forbiddenStores != null
                        && forbiddenStores.contains(child))) {
            newName = origName + "(" + (++cnt) + ")" + extension;
            child = parent.getChild(newName);
        }
        return newName;
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
        return new OverwriteAndMergeInfo(m_rename ? m_newName : null, false, m_overwrite, m_snapshotPanel != null
            ? m_snapshotPanel.createSnapshot() : false, m_snapshotPanel != null ? m_snapshotPanel.getComment() : null);
    }
}
