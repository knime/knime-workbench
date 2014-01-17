/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * Created: May 25, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.explorer.view.ExplorerViewComparator;
import org.knime.workbench.ui.navigator.actions.selection.TreeSelectionControl;
import org.knime.workbench.ui.navigator.actions.selection.TreeSelectionControl.TreeSelectionChangeListener;

/**
 *
 * @author ohl, University of Konstanz
 */
public class SpaceResourceSelectionDialog extends Dialog {

    private final String[] m_mountIDs;

    private TreeSelectionControl m_tree;

    private ContentDelegator m_treeInput;

    private Label m_path;

    private AbstractExplorerFileStore m_selectedContainer;

    private final ContentObject m_initialSelection;

    private SelectionValidator m_validator = null;

    private String m_title = null;

    private String m_header = "Make a selection";

    private String m_description = "";

    private final String m_message = "";

    private boolean m_valid = true;

    private int m_xSizeFactor;

    private int m_ySizeFactor;

    private boolean m_nameFieldEnabled = false;

    private String m_nameFieldDefaultValue = "";

    private String m_nameFieldValue = null;

    private StringValidator m_nameFieldValidator = null;

    private boolean m_nameFieldValid = true;

    /**
     * Creates a new dialog showing the passed mount ids.
     *
     * @param parentShell the parent shell
     * @param mountIDs the ids of the mount points to show
     * @param initialSelection the object to select initially, maybe <code>null</code> if no group should be selected
     *            initially
     */
    public SpaceResourceSelectionDialog(final Shell parentShell,
            final String[] mountIDs, final ContentObject initialSelection) {
        super(parentShell);
        m_mountIDs = mountIDs;
        m_initialSelection = initialSelection;
        if (initialSelection != null) {
            m_selectedContainer = initialSelection.getObject();
        } else {
            m_selectedContainer = null;
        }
        m_xSizeFactor = 1;
        m_ySizeFactor = 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        Point size = super.getInitialSize();
        return  new Point(size.x * m_xSizeFactor,
                size.y * m_ySizeFactor);
    }

    /**
     * Sets the title of the dialog window (has no effect after window is
     * created).
     *
     * @param title the title to display
     */
    public void setTitle(final String title) {
        m_title = title;
    }

    /**
     * Sets the header (first line) displayed in the upper (white) part of the
     * dialog. Defaults to &quot;Make a selection&quot;. Has no effect after the
     * window is created.
     *
     * @param header the header to display
     */
    public void setHeader(final String header) {
        if (header != null && !header.isEmpty()) {
            m_header = header;
        }
    }

    /**
     * Sets the descriptive text that is displayed beneath the header. Has no
     * effect after the window is created.
     *
     * @param descr the description
     */
    public void setDescription(final String descr) {
        if (descr != null && !descr.isEmpty()) {
            m_description = descr;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        if (m_title != null) {
            newShell.setText(m_title);
        }
    }

    /**
     * @return the selected file store
     */
    public AbstractExplorerFileStore getSelection() {
        return m_selectedContainer;
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
        createTreeControl(overall);
        createResultPanel(overall);
        createNameField(overall);
        return overall;

    }

    /**
     * Creates the header composite.
     *
     * @param parent the parent composite
     */
    protected void createHeader(final Composite parent) {
        Composite header = new Composite(parent, SWT.FILL);
        Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
        header.setBackground(white);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        header.setLayoutData(gridData);
        header.setLayout(new GridLayout(2, false));
        Label exec = new Label(header, SWT.NONE);
        exec.setBackground(white);
        exec.setText(m_header);
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
        txt.setText(m_description);
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    }

    /**
     * Creates the tree selection panel.
     *
     * @param parent the parent composite
     */
    protected void createTreeControl(final Composite parent) {
        m_treeInput = new ContentDelegator();
        for (String id : m_mountIDs) {
            MountPoint mp = ExplorerMountTable.getMountPoint(id);
            if (mp != null) {
                m_treeInput.addMountPoint(mp);
            }
        }

        m_tree = new TreeSelectionControl();
        m_tree.setContentProvider(m_treeInput);
        m_tree.setLabelProvider(m_treeInput);
        m_tree.setComparator(new ExplorerViewComparator());
        if (m_initialSelection != null) {
            m_tree.setInitialSelection(new StructuredSelection(
                    m_initialSelection));
        }
        m_tree.setInput(m_treeInput);
        m_tree.setMessage(m_message);
        m_tree.setValidator(new ISelectionValidator() {

            @Override
            public String isValid(final Object selection) {
                if (m_validator != null) {
                    AbstractExplorerFileStore selFile =
                            getSelectedFile(selection);
                    String result;
                    if (selFile != null) {
                        result = m_validator.isValid(selFile);
                    } else {
                        result = "Invalid selection";
                    }
                    Button b = getButton(IDialogConstants.OK_ID);
                    // store it in case button is not created yet
                    m_valid = result == null;
                    if (b != null) {
                        b.setEnabled(m_valid && m_nameFieldValid);
                    }
                    return result;
                }
                return null;
            }
        });
        m_tree.setChangeListener(new TreeSelectionChangeListener() {
            @Override
            public void treeSelectionChanged(final Object newSelection,
                    final boolean valid) {
                m_selectedContainer = getSelectedFile(newSelection);
                updateResultPanel();
            }
        });
        m_tree.createTreeControl(parent);
    }

    /**
     * Creates the result panel.
     *
     * @param parent the parent composite
     */
    protected void createResultPanel(final Composite parent) {
        Composite panel = new Composite(parent, SWT.FILL);
        panel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        panel.setLayout(new GridLayout(1, true));
        m_path = new Label(parent, SWT.NONE);
        m_path.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        updateResultPanel();
    }

    /**
     * Update the result panel.
     */
    protected void updateResultPanel() {
        if (m_selectedContainer != null) {
            m_path.setText("knime://" + m_selectedContainer.getMountID() + m_selectedContainer.getFullName());
        } else {
            m_path.setText("");
        }
    }


    /**
     * Extracts the file store from the selection. Returns null if the type is
     * unexpected.
     */
    private AbstractExplorerFileStore getSelectedFile(final Object selection) {
        return ContentDelegator.getFileStore(selection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean close() {
        m_treeInput.dispose();
        return super.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Button createButton(final Composite parent, final int id,
            final String label, final boolean defaultButton) {
        Button b = super.createButton(parent, id, label, defaultButton);
        if (id == Window.OK && (m_validator != null || m_nameFieldValidator != null)) {
            // sometimes the validator gets called before the button is created
            b.setEnabled(m_valid && m_nameFieldValid);
        }
        return b;
    }

    /**
     * Sets the validator to use.
     *
     * @param validator the validator that is used to determin if a selected
     *          file is valid
     */
    public void setValidator(final SelectionValidator validator) {
        m_validator = validator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Allows to scale the initial dialog size.
     *
     * @param xFactor the factor to multiply the width with
     * @param yFactor the factor to multiply the height with
     */
    public void scaleDialogSize(final int xFactor, final int yFactor) {
        m_xSizeFactor = xFactor;
        m_ySizeFactor = yFactor;
    }

    /**
     * Used to validate a new selection in the
     * {@link SpaceResourceSelectionDialog}.
     *
     * @author ohl, University of Konstanz
     */
    public interface SelectionValidator {
        /**
         * Return null if the selection is valid. A user message if it is
         * invalid.
         *
         * @param selection to validate
         * @return null if the selection is valid. A user message if it is
         *         invalid.
         */
        public String isValid(final AbstractExplorerFileStore selection);
    }

    /**
     * Enable/disable the name field.
     *
     * @param enabled true if the name field should be shown, false otherwise
     * @since 6.2
     */
    public void setNameFieldEnabled(final boolean enabled) {
        m_nameFieldEnabled = enabled;
    }

    /**
     * Set the default value for the name field.
     *
     * @param defaultValue The default value
     * @since 6.2
     */
    public void setNameFieldDefaultValue(final String defaultValue) {
        m_nameFieldDefaultValue = defaultValue;
    }

    /**
     * Get the value of the name field.
     *
     * @return The value inside the name field or null if it is not valid according to the set validator
     * @since 6.2
     */
    public String getNameFieldValue() {
        return m_nameFieldValue;
    }

    /**
     * Set the validator for the name field.
     *
     * @param validator Validator that checks if the current name is valid
     * @since 6.2
     */
    public void setNameFieldValidator(final StringValidator validator) {
        m_nameFieldValidator = validator;
    }

    /**
     * Adds the name field to the parent (hiding it if it is not enabled).
     *
     * @param parent The composite to add the name field to
     */
    private void createNameField(final Composite parent) {
        if (m_nameFieldEnabled) {
            final Text nameField = new Text(parent, SWT.SINGLE | SWT.BORDER);
            nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            final Label nameFieldError = new Label(parent, SWT.NONE);
            Color red = Display.getDefault().getSystemColor(SWT.COLOR_RED);
            nameFieldError.setForeground(red);
            nameFieldError.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            // Check if string is valid and save value every time it changes
            nameField.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    if (m_nameFieldValidator != null) {
                        String result = m_nameFieldValidator.isValid(nameField.getText());
                        Button b = getButton(IDialogConstants.OK_ID);
                        m_nameFieldValid = result == null;
                        if (m_nameFieldValid) {
                            m_nameFieldValue = nameField.getText();
                            nameFieldError.setText("");
                        } else {
                            // If string is invalid the value to return in the getter is null
                            m_nameFieldValue = null;
                            nameFieldError.setText(result);
                        }
                        if (b != null) {
                            b.setEnabled(m_valid && m_nameFieldValid);
                        }
                    } else {
                        m_nameFieldValid = true;
                        m_nameFieldValue = nameField.getText();
                    }
                }
            });
            nameField.setText(m_nameFieldDefaultValue);
        }
    }

    /**
     * Validator that checks if a string is valid.
     *
     * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
     * @since 6.2
     */
    public interface StringValidator {
        /**
         * Return null if the string is valid. A user message if it is
         * invalid.
         *
         * @param string to validate
         * @return null if the string is valid. A user message if it is
         *         invalid.
         */
        public String isValid(final String string);
    }
}
