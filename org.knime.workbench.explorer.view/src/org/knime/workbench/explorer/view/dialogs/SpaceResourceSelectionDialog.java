/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: May 25, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ISelectionValidator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.ui.navigator.actions.selection.TreeSelectionControl;
import org.knime.workbench.ui.navigator.actions.selection.TreeSelectionControl.TreeSelectionChangeListener;

/**
 *
 * @author ohl, University of Konstanz
 */
public class SpaceResourceSelectionDialog extends Dialog {

    private final String[] m_mountIDs;

    private ContentDelegator m_treeInput;

    private ExplorerFileStore m_selectedContainer;

    private final ContentObject m_initialSelection;

    private SelectionValidator m_validator = null;

    private String m_title = null;

    private String m_message = "";

    private boolean m_valid = true;

    public SpaceResourceSelectionDialog(final Shell parentShell,
            final String[] providerIDs, final ContentObject initialSelection,
            final String message) {
        super(parentShell);
        m_message = message;
        m_mountIDs = providerIDs;
        m_initialSelection = initialSelection;
        if (initialSelection != null) {
            m_selectedContainer = initialSelection.getObject();
        } else {
            m_selectedContainer = null;
        }
    }

    public void setTitle(final String title) {
        m_title = title;
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        if (m_title != null) {
            newShell.setText(m_title);
        }
    }

    public ExplorerFileStore getSelection() {
        return m_selectedContainer;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        m_treeInput = new ContentDelegator();
        for (String id : m_mountIDs) {
            MountPoint mp = ExplorerMountTable.getMountPoint(id);
            if (mp != null) {
                m_treeInput.addMountPoint(mp);
            }
        }

        TreeSelectionControl tree = new TreeSelectionControl();
        tree.setContentProvider(m_treeInput);
        tree.setLabelProvider(m_treeInput);
        if (m_initialSelection != null) {
            tree.setInitialSelection(
                    new StructuredSelection(m_initialSelection));
        }
        tree.setInput(m_treeInput);
        tree.setMessage(m_message);
        tree.setValidator(new ISelectionValidator() {

            @Override
            public String isValid(final Object selection) {
                if (m_validator != null) {
                    ExplorerFileStore selFile = getSelectedFile(selection);
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
                        b.setEnabled(m_valid);
                    }
                    return result;
                }
                return null;
            }
        });
        tree.setChangeListener(new TreeSelectionChangeListener() {
            @Override
            public void treeSelectionChanged(final Object newSelection,
                    final boolean valid) {
                m_selectedContainer = getSelectedFile(newSelection);
            }
        });
        return tree.createTreeControl(parent);
    }

    /**
     * Extracts the file store from the selection. Returns null if the type
     * is unexpected.
     */
    private ExplorerFileStore getSelectedFile(final Object selection) {
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
        if (id == Dialog.OK && m_validator != null) {
            // sometimes the validator gets called before the button is created
            b.setEnabled(m_valid);
        }
        return b;
    }

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
        public String isValid(final ExplorerFileStore selection);
    }

}
