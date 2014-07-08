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
 * ---------------------------------------------------------------------
 *
 * Created on 05.11.2013 by thor
 */
package org.knime.workbench.explorer.view.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * Panel that shows a checkbox asking the user whether a snapshot should be created and a textfield for the comment.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.8
 */
public class SnapshotPanel extends Composite {
    private final Button m_createSnapshotButton;

    private final Text m_commentField;

    private boolean m_createSnapshot;

    private String m_comment;

    /**
     * Creates a new panel.
     *
     * @param parent the parent component
     * @param style layout styles, the same as for {@link Composite}
     */
    public SnapshotPanel(final Composite parent, final int style) {
        super(parent, style);
        setLayout(new GridLayout());
        setLayoutData(new GridData(GridData.FILL_BOTH));

        m_createSnapshotButton = new Button(this, SWT.CHECK);
        m_createSnapshotButton.setText("Create snapshot before overwriting?");
        m_createSnapshotButton.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));

        m_commentField = new Text(this, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.heightHint = 2 * m_commentField.getLineHeight();
        m_commentField.setLayoutData(gridData);

        m_createSnapshotButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                m_commentField.setEnabled(m_createSnapshotButton.getSelection());
                m_createSnapshot = m_createSnapshotButton.getSelection();
            }
        });

        m_commentField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                m_comment = m_commentField.getText();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_createSnapshotButton.setEnabled(enabled);
        m_commentField.setEnabled(enabled && m_createSnapshotButton.getSelection());
    }

    /**
     * Returns whether a snapshot should be created or not.
     *
     * @return <code>true</code> if a snapshot should be created, <code>false</code> otherwise
     */
    public boolean createSnapshot() {
        return m_createSnapshot;
    }

    /**
     * Returns the comment for the snapshot. Maybe an empty string.
     *
     * @return a comment
     */
    public String getComment() {
        return m_comment;
    }
}
