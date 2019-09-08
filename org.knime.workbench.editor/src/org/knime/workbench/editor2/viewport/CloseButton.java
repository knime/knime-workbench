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

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * This is a subclass of the {@code VeryLightFlatButton} to support the fill-less, border-less, close button.
 *
 * @author loki der quaeler
 */
class CloseButton extends VeryLightFlatButton {
    private static final String N_ARY_TIMES = "\u2A09";
    private static final int N_ARY_RENDER_HORIZONTAL_FUDGE;
    private static final int N_ARY_RENDER_VERTICAL_FUDGE;

    static {
        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            N_ARY_RENDER_HORIZONTAL_FUDGE = 0;
            N_ARY_RENDER_VERTICAL_FUDGE = 1;
        } else if (Platform.OS_WIN32.equals(Platform.getOS())) {
            N_ARY_RENDER_HORIZONTAL_FUDGE = -1;
            N_ARY_RENDER_VERTICAL_FUDGE = -1;
        } else {
            N_ARY_RENDER_HORIZONTAL_FUDGE = 0;
            N_ARY_RENDER_VERTICAL_FUDGE = -2;
        }
    }


    private int m_horizontalInset;
    private int m_verticalInset;

    CloseButton(final Runnable closeAction, final Display display, final Color parentBackgroundColor) {
        super(N_ARY_TIMES, closeAction, display, parentBackgroundColor, false);
    }

    @Override
    protected Point calculateSizeForTitleSize(final Point textSize) {
        final int width = textSize.x + 4;
        final int height = textSize.y + 4;
        final int dimension = Math.max(width, height);

        m_horizontalInset = ((dimension - textSize.x) / 2) + N_ARY_RENDER_HORIZONTAL_FUDGE;
        m_verticalInset = ((dimension - textSize.y) / 2) + N_ARY_RENDER_VERTICAL_FUDGE;

        return new Point(dimension, dimension);
    }

    @Override
    protected void paintBackground(final GC gc) {
        if (!m_mouseInBounds) {
            return;
        }

        gc.setBackground(m_mouseOverColor);
        gc.fillOval(m_location.x, m_location.y, m_size.x, m_size.y);
    }

    @Override
    protected void paintText(final GC gc) {
        gc.setForeground(m_textColor);
        gc.drawText(m_buttonTitle, (m_location.x + m_horizontalInset), (m_location.y + m_verticalInset), true);
    }

    @Override
    protected void paintBorder(final GC gc) { }
}
