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
 *   May 13, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.atoms;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;

/**
 * This is the abstract superclass of atom classes whose ui is an instance of {@link Text}
 *
 * @author loki der quaeler
 */
public abstract class AbstractTextMetaInfoAtom extends MetaInfoAtom {
    private Text m_editTextWidget;

    private String m_editState;
    private final AtomicBoolean m_isDirty;

    /**
     * A class for atoms whose edit-representation utilize a text field.
     *
     * @param type the atom type
     * @param label the label displayed with the value of this atom in some UI widget.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public AbstractTextMetaInfoAtom(final MetadataItemType type, final String label, final String value, final boolean readOnly) {
        super(type, label, value, readOnly);

        m_isDirty = new AtomicBoolean(false);
    }

    /**
     * @param value the value which should be held by this atom
     */
    public void setValue(final String value) {
        m_value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() {
        m_editState = (m_value != null) ? m_value : "";
        m_isDirty.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() {
        m_editTextWidget = null;
        m_editState = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() {
        if (m_editTextWidget != null) {
            m_value = m_editTextWidget.getText();
        }
        m_editTextWidget = null;
        m_editState = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return m_isDirty.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void focus() {
        m_editTextWidget.setFocus();
    }

    /**
     * Subclasses should invoke this from their {@link #populateContainerForEdit(Composite)}
     *
     * @param parent the parent for the widget
     * @param style the SWT bitwise-OR'd style
     * @param gridData the {@link GridData} layout data for the widget
     */
    protected void createAndPlaceTextWidget(final Composite parent, final int style, final GridData gridData) {
        m_editTextWidget = new Text(parent, style);

        m_editTextWidget.setLayoutData(gridData);
        m_editTextWidget.setText((m_value == null) ? "" : m_value);
        m_editTextWidget.addKeyListener(new KeyAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void keyReleased(final KeyEvent ke) {
                boolean dirty = false;

                if (m_editState.length() != m_editTextWidget.getCharCount()) {
                    dirty = true;
                } else if (!m_editState.equals(m_editTextWidget.getText())) {
                    dirty = true;
                }

                if (dirty != m_isDirty.getAndSet(dirty)) {
                    messageListeners(dirty ? ListenerEventType.DIRTY : ListenerEventType.CLEAN);
                }
            }
        });
        m_editTextWidget.addFocusListener(new FocusAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void focusGained(final FocusEvent e) {
                m_editTextWidget.selectAll();
            }
        });
    }
}
