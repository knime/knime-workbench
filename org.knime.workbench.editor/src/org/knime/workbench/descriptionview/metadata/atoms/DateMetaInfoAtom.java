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
 *   June 4, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.atoms;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.knime.core.node.workflow.metadata.MetaInfoFile;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.xml.sax.SAXException;

/**
 * Currently this atom is always typed to {@link MetadataItemType#CREATION_DATE} as we have no other date reliant
 * types.
 *
 * @author loki der quaeler
 */
public class DateMetaInfoAtom extends MetaInfoAtom {
    private static final String TIME_OF_DAY_SUFFIX = "/12:00:01 +02:00";

    private static boolean compareCalendarDates(final Calendar c1, final Calendar c2) {
        if ((c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR))
                || (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH))
                || (c1.get(Calendar.DAY_OF_MONTH) != c2.get(Calendar.DAY_OF_MONTH))) {
            return false;
        }

        return true;
    }


    private Calendar m_date;
    private DateTime m_datePicker;

    // redundant to m_date but clearer
    private Calendar m_editState;
    private final AtomicBoolean m_isDirty;

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public DateMetaInfoAtom (final String label, final String value, final boolean readOnly) {
        super(MetadataItemType.CREATION_DATE, label, value, readOnly);

        if (value == null) {
            m_date = null;
        } else {
            m_date = MetaInfoFile.calendarFromDateString(value);
        }

        m_isDirty = new AtomicBoolean(false);
    }

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param date the {@link Calendar} instance representing the date represented by this instance
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public DateMetaInfoAtom (final String label, final Calendar date, final boolean readOnly) {
        super(MetadataItemType.CREATION_DATE, label, MetaInfoFile.dateToStorageString(date), readOnly);

        m_date = date;

        m_isDirty = new AtomicBoolean(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue() {
        if (m_date != null) {
            return MetaInfoFile.dateToStorageString(m_date) + TIME_OF_DAY_SUFFIX;
        }

        return super.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() {
        if (m_date != null) {
            m_editState = Calendar.getInstance();
            m_editState.set(m_date.get(Calendar.YEAR), m_date.get(Calendar.MONTH), m_date.get(Calendar.DAY_OF_MONTH));
        } else {
            m_editState = null;
        }

        m_isDirty.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() {
        m_editState = null;
        m_datePicker = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() {
        if (m_datePicker != null) {
            m_date = Calendar.getInstance();
            m_date.set(m_datePicker.getYear(), m_datePicker.getMonth(), m_datePicker.getDay());

            m_value = MetaInfoFile.dateToStorageString(m_date) + TIME_OF_DAY_SUFFIX;
        }

        m_datePicker = null;
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
        final Label l = new Label(parent, SWT.RIGHT);
        l.setFont(AbstractMetaView.VALUE_DISPLAY_FONT);
        l.setForeground(AbstractMetaView.TEXT_COLOR);
        l.setText(displayFormatForCalendar());

        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);

        gd = (GridData)parent.getLayoutData();
        gd.heightHint = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        // re-set it again in case some platform does a clone on get
        parent.setLayoutData(gd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForEdit(final Composite parent) {
        m_datePicker = new DateTime (parent, SWT.DROP_DOWN | SWT.DATE);
        m_datePicker.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final Calendar selected = getSelectedDate();
                final boolean dirty = !compareCalendarDates(selected, m_editState);

                if (dirty != m_isDirty.getAndSet(dirty)) {
                    messageListeners(dirty ? ListenerEventType.DIRTY : ListenerEventType.CLEAN);
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) { }
        });
        if (m_date != null) {
            m_datePicker.setYear(m_date.get(Calendar.YEAR));
            m_datePicker.setMonth(m_date.get(Calendar.MONTH));
            m_datePicker.setDay(m_date.get(Calendar.DAY_OF_MONTH));
        }

        final GridData gd = (GridData)parent.getLayoutData();
        gd.heightHint = m_datePicker.computeSize(SWT.DEFAULT, SWT.DEFAULT).y + 4;
        // re-set it again in case some platform does a clone on get
        parent.setLayoutData(gd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void focus() {
        m_datePicker.setFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        if (hasContent()) {
            save(parentElement, MetadataXML.DATE);
        }
    }

    private Calendar getSelectedDate() {
        final Calendar date = Calendar.getInstance();

        date.set(m_datePicker.getYear(), m_datePicker.getMonth(), m_datePicker.getDay());

        return date;
    }

    private String displayFormatForCalendar() {
        if (m_date != null) {
            final StringBuilder sb = new StringBuilder();
            sb.append(m_date.get(Calendar.YEAR)).append('-');
            sb.append(m_date.get(Calendar.MONTH) + 1).append('-');
            sb.append(m_date.get(Calendar.DAY_OF_MONTH));
            return sb.toString();
        }

        return "Not set.";
    }
}
