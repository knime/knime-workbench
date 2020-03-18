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

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.xml.sax.SAXException;

/**
 * This supports atoms which provide a single line text field as their edit UI.
 *
 * @author loki der quaeler
 */
public class TextFieldMetaInfoAtom extends AbstractTextMetaInfoAtom {
    private static final int CHARACTER_COUNT_THRESHOLD_AT_WHICH_TO_SHRINK_FONT = 61;

    /* It's plausible we might want to keep a set of these decrementing every N characters past threshold. TODO */
    private static Font SHRUNKEN_FONT = null;

    private static synchronized Font getShrunkenFontBasedOnNormalFont(final Font normalFont) {
        if (SHRUNKEN_FONT == null) {
            final FontData[] normalFD = normalFont.getFontData();
            final FontData smallerFD =
                new FontData(normalFD[0].getName(), normalFD[0].getHeight() - 2, normalFD[0].getStyle());

            SHRUNKEN_FONT = new Font(PlatformUI.getWorkbench().getDisplay(), smallerFD);
        }

        return SHRUNKEN_FONT;
    }


    /**
     * A class for atoms whose edit-representation utilize a text field.
     *
     * @param type the atom type
     * @param label the label displayed with the value of this atom in some UI widget.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public TextFieldMetaInfoAtom(final MetadataItemType type, final String label, final String value, final boolean readOnly) {
        super(type, label, value, readOnly);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForDisplay(final Composite parent) {
        final Label l = new Label(parent, SWT.LEFT | SWT.WRAP);
        final int[] charAndLineCount = calculateLongestLineAndLineCount();
        final Font f;
        if ((charAndLineCount[1] > 1) || (charAndLineCount[0] > CHARACTER_COUNT_THRESHOLD_AT_WHICH_TO_SHRINK_FONT)) {
            f = getShrunkenFontBasedOnNormalFont(AbstractMetaView.VALUE_DISPLAY_FONT);
        } else {
            f = AbstractMetaView.VALUE_DISPLAY_FONT;
        }
        l.setFont(f);
        l.setForeground(AbstractMetaView.TEXT_COLOR);
        l.setText((m_value != null) ? m_value : "");
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForEdit(final Composite parent) {
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;

        createAndPlaceTextWidget(parent, SWT.BORDER, gd);
   }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        if (hasContent()) {
            save(parentElement, MetadataXML.TEXT);
        }
    }

    /**
     * @return index 0 is the character count of the longest line, index 1 is the total line count, derived by
     *              splitting on the newline character
     */
    private int[] calculateLongestLineAndLineCount() {
        if (m_value != null) {
            final String[] maybeMultiples = m_value.split("\n");
            int currentMaximum = Integer.MIN_VALUE;

            for (final String line : maybeMultiples) {
                final int length = line.length();
                if (length > currentMaximum) {
                    currentMaximum = length;
                }
            }

            return new int[]{currentMaximum, maybeMultiples.length};
        }

        return new int[]{0, 0};
    }
}
