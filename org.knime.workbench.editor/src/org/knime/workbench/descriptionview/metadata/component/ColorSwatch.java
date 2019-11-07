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
 *   Nov 6, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

/**
 * A widget to display a color swatch, potentially with a 'delete' icon and listener functionality for that delete
 * click.
 *
 * @author loki der quaeler
 */
class ColorSwatch extends AbstractSwatch {
    private static final int ROUNDED_CORNER = 15;


    private Color m_currentColor;

    ColorSwatch(final Composite parent, final Listener deleteListener) {
        super(parent, deleteListener);
    }

    @Override
    void drawContent(final GC gc) {
        gc.setBackground(m_currentColor);
        final Point size = ColorSwatch.this.getSize();
        gc.fillRoundRectangle(0, 0, size.x, size.y, ROUNDED_CORNER, ROUNDED_CORNER);
    }

    @Override
    boolean hasContent() {
        return (m_currentColor != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        disposeOfColorIfPresent();
    }

    void setColor(final RGB newColor) {
        // we _could_ check for a setting of the same color, but it's not clear that saves us much
        disposeOfColorIfPresent();

        final GridData gd = (GridData)getLayoutData();
        if (newColor != null) {
            m_currentColor = new Color(getDisplay(), newColor);
            gd.exclude = false;
            setVisible(true);
        } else {
            gd.exclude = true;
            setVisible(false);
        }
        setLayoutData(gd);

        redraw();
    }

    private void disposeOfColorIfPresent() {
        if (m_currentColor != null) {
            m_currentColor.dispose();
            m_currentColor = null;
        }
    }
}
