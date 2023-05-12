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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.knime.core.node.workflow.metadata.MetaInfoFile;
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

    private ZonedDateTime m_date;

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param value the displayed value of this atom.
     */
    public DateMetaInfoAtom (final String label, final String value) {
        super(MetadataItemType.CREATION_DATE, label, value, true);

        if (value == null) {
            m_date = null;
        } else {
            final var calendar = MetaInfoFile.calendarFromDateString(value);
            m_date = calendar instanceof GregorianCalendar gregorian ? gregorian.toZonedDateTime()
                : ZonedDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
        }
    }

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param date the {@link Calendar} instance representing the date represented by this instance
     */
    public DateMetaInfoAtom (final String label, final ZonedDateTime date) {
        super(MetadataItemType.CREATION_DATE, label, MetaInfoFile.dateToStorageString(date.getDayOfMonth(),
            date.getMonthValue(), date.getYear()), true);

        m_date = date;
    }

    @Override
    public String getValue() {
        if (m_date != null) {
            return MetaInfoFile.dateToStorageString(m_date.getDayOfMonth(), m_date.getMonthValue(),
                m_date.getYear()) + TIME_OF_DAY_SUFFIX;
        }

        return super.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return false;
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
        populateContainerForDisplay(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void focus() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        // nothing to save
    }

    private String displayFormatForCalendar() {
        if (m_date != null) {
            final var sb = new StringBuilder();
            sb.append(m_date.get(ChronoField.YEAR)).append('-');
            sb.append(m_date.get(ChronoField.MONTH_OF_YEAR)).append('-');
            sb.append(m_date.get(ChronoField.DAY_OF_MONTH));
            return sb.toString();
        }

        return "Not set.";
    }

    /**
     * @return the selected point in time
     */
    public ZonedDateTime getDateTime() {
        return m_date;
    }
}
