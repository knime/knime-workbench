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
 *   May 9, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.atoms;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.descriptionview.metadata.LicenseType;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.xml.sax.SAXException;

/**
 * Currently this atom is always typed to {@link MetadataItemType#LICENSE} as we have no other combobox reliant
 * types; i'm reluctant to call it something like <code>LicenseMetaInfoAtom</code> as, like {@link TextAreaMetaInfoAtom}, it
 * seems plausible that there may be other combo-box-UI dependent metadata in the future.
 *
 * @author loki der quaeler
 */
public class ComboBoxMetaInfoAtom extends MetaInfoAtom {
    private static final LicenseLabelProvider LABEL_PROVIDER = new LicenseLabelProvider();


    private LicenseType m_licenseType;
    private ComboViewer m_editComboViewer;

    private LicenseType m_editState;
    private final AtomicBoolean m_isDirty;

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public ComboBoxMetaInfoAtom (final String label, final String value, final boolean readOnly) {
        super(MetadataItemType.LICENSE, label, value, readOnly);

        final int index = LicenseType.getIndexForLicenseWithName(value);
        if (index == -1) {
            m_licenseType = LicenseType.getAvailableLicenses().get(0);
            m_value = m_licenseType.getDisplayName();
        } else {
            m_licenseType = LicenseType.getAvailableLicenses().get(index);
        }

        m_isDirty = new AtomicBoolean(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() {
        m_editState = m_licenseType;
        m_isDirty.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() {
        m_editComboViewer = null;
        m_editState = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() {
        if (m_editComboViewer != null) {
            m_licenseType = getCurrentSelection();

            m_value = m_licenseType.getDisplayName();
        }

        m_editComboViewer = null;
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
    public void populateContainerForDisplay(final Composite parent) {
        final HyperLinkLabel hll = new HyperLinkLabel(parent, false, m_licenseType.getURL());
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        hll.setLayoutData(gd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForEdit(final Composite parent) {
        m_editComboViewer = new ComboViewer(parent, SWT.READ_ONLY);

        m_editComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_editComboViewer.setLabelProvider(LABEL_PROVIDER);
        m_editComboViewer.setInput(LicenseType.getAvailableLicenses());

        int index = LicenseType.getIndexForLicenseWithName(m_value);
        if (index == -1) {
            index = 0;
        }
        m_editComboViewer.setSelection(new StructuredSelection(m_editComboViewer.getElementAt(index)), true);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        m_editComboViewer.getCombo().setLayoutData(gd);
        m_editComboViewer.addPostSelectionChangedListener((event) -> {
            final LicenseType license = getCurrentSelection();
            final boolean dirty = !license.equals(m_editState);

            if (dirty != m_isDirty.getAndSet(dirty)) {
                messageListeners(dirty ? ListenerEventType.DIRTY : ListenerEventType.CLEAN);
            }
        });

        gd = (GridData)parent.getLayoutData();
        gd.heightHint = m_editComboViewer.getCombo().computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 4;
        // re-set it again in case some platform does a clone on get
        parent.setLayoutData(gd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void focus() {
        m_editComboViewer.getCombo().setFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        if (hasContent()) {
            save(parentElement, MetadataXML.COMBOBOX);
        }
    }

    private LicenseType getCurrentSelection() {
        final StructuredSelection selection = (StructuredSelection)m_editComboViewer.getSelection();
        return (LicenseType)selection.getFirstElement();
    }


    private static class LicenseLabelProvider extends LabelProvider {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(final Object o) {
            if (o instanceof LicenseType) {
                return ((LicenseType)o).getDisplayName();
            }

            return null;
        }
    }
}
