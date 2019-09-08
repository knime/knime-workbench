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
 *   Sep 7, 2019 (loki): created
 */
package org.knime.workbench.editor2.viewport;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * This is a flat button class removed entirely from the SWT class hierarchy; it's basically a mouse state container
 * which knows how to paint itself.
 *
 * This was originally an inner class to {@code ViewportPinningGraphicalViewer} but it was starting to turn into a silly
 * monolith class, so it's gotten its own package now.
 *
 * @author loki der quaeler
 */
class VeryLightFlatButton {
    protected static final int HORIZONTAL_INSET = 15;
    protected static final int VERTICAL_INSET = 3;

    private static final Color BORDER_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 163, 163, 163);

    // TODO this belongs in ColorUtilities - but that's in knime-core which will trigger another refactor request
    //          to get all UI code out of knime-core which i'd rather get done in one fell swoop in its own
    //          ticket. So... at that point, consider moving this there.
    // @param darker should be [0.0, 1.0] where 1.0 is no darker, and 0.0 is 'black.'
    private static Color getDarkerColor(final Color originalColor, final Display display, final float darker) {
        final float[] hsb = java.awt.Color.RGBtoHSB(originalColor.getRed(), originalColor.getGreen(),
            originalColor.getBlue(), null);

        hsb[2] *= darker;

        final java.awt.Color awtDarker = java.awt.Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        return new Color(display, awtDarker.getRed(), awtDarker.getGreen(), awtDarker.getBlue());
    }


    protected volatile boolean m_mouseInBounds;
    protected final Point m_size;
    protected Point m_location;
    protected final Color m_fillColor;
    protected final Color m_mouseOverColor;
    protected final Color m_textColor;
    protected final String m_buttonTitle;

    private final Runnable m_action;

    VeryLightFlatButton(final String title, final Runnable action, final Display display,
            final Color parentBackgroundColor) {
        this(title, action, display, parentBackgroundColor, true);
    }

    VeryLightFlatButton(final String title, final Runnable action, final Display display,
        final Color parentBackgroundColor, final boolean paintFill) {
        m_buttonTitle = title;
        m_action = action;

        if (parentBackgroundColor != null) {
            m_fillColor = paintFill ? getDarkerColor(parentBackgroundColor, display, 0.93f) : null;
            m_mouseOverColor = getDarkerColor(parentBackgroundColor, display, 0.75f);
        } else {
            m_fillColor = null;
            m_mouseOverColor = null;
        }
        m_textColor = display.getSystemColor(SWT.COLOR_BLACK);

        m_mouseInBounds = false;

        final GC gc = new GC(display);
        try {
            final Point textSize = gc.stringExtent(m_buttonTitle);

            m_size = calculateSizeForTitleSize(textSize);
        } finally {
            gc.dispose();
        }
    }

    /*
     * An opportunity for subclasses to alter their size calculation.
     */
    protected Point calculateSizeForTitleSize(final Point textSize) {
        final int width = textSize.x + (2 * HORIZONTAL_INSET) + 2; // +2s for border
        final int height = textSize.y + (2 * VERTICAL_INSET) + 2; // +2s for border

        return new Point(width, height);
    }

    void dispose() {
        if (m_fillColor != null) {
            m_fillColor.dispose();
        }
        if (m_mouseOverColor != null) {
            m_mouseOverColor.dispose();
        }
    }

    Point getSize() {
        return m_size;
    }

    Runnable getAction() {
        return m_action;
    }

    void setLocation(final Point location) {
        m_location = location;
    }

    /* returns true if the mouse was previously considered out of bounds  */
    boolean mouseInBounds() {
        final boolean wasOutOfBounds = !m_mouseInBounds;
        m_mouseInBounds = true;
        return wasOutOfBounds;
    }

    /* returns true if the mouse was previously considered in bounds  */
    boolean mouseOutOfBounds() {
        final boolean wasInBounds = m_mouseInBounds;
        m_mouseInBounds = false;
        return wasInBounds;
    }

    void paint(final GC gc) {
        paintBackground(gc);

        paintText(gc);

        paintBorder(gc);
    }

    /*
     * An opportunity for subclasses to intercede; consumers of this class should call {@link #paint(GC)}
     */
    protected void paintBackground(final GC gc) {
        if ((m_mouseInBounds && (m_mouseOverColor == null))
                || (!m_mouseInBounds && (m_fillColor == null))) {
            return;
        }

        gc.setBackground(m_mouseInBounds ? m_mouseOverColor : m_fillColor);
        gc.fillRectangle(m_location.x, m_location.y, m_size.x, m_size.y);
    }

    /*
     * An opportunity for subclasses to intercede; consumers of this class should call {@link #paint(GC)}
     */
    protected void paintText(final GC gc) {
        gc.setForeground(m_textColor);
        gc.drawText(m_buttonTitle, (m_location.x + HORIZONTAL_INSET), (m_location.y + VERTICAL_INSET), true);
    }

    /*
     * An opportunity for subclasses to intercede; consumers of this class should call {@link #paint(GC)}
     */
    protected void paintBorder(final GC gc) {
        gc.setForeground(BORDER_COLOR);
        gc.drawRectangle(m_location.x, m_location.y, (m_size.x - 1), (m_size.y - 1));
    }
}
