/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
package org.knime.workbench.explorer.view.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
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
        if (id == Window.OK && m_validator != null) {
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
