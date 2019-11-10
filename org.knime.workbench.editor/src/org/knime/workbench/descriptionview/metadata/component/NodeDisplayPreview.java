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
 *   Nov 8, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * This class is used in the display mode of the component metadata view to render a preview of the display of the
 * component node, given the potentially user specified icon and color.
 *
 * @author loki der quaeler
 */
class NodeDisplayPreview extends Canvas {
    private static final int SWATCH_SIZE = 50;
    private static final int ICON_INSET = 9;
    private static final int ICON_DIMENSION = 32;
    private static final int ROUNDED_CORNER = 15;

    private static final Color DEFAULT_FILL_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 189, 189, 189);


    private Image m_image;
    private Rectangle m_imageBounds;
    private Color m_currentColor;

    NodeDisplayPreview(final Composite parent) {
        super(parent, SWT.TRANSPARENT);

        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.CENTER;
        gd.heightHint = SWATCH_SIZE;
        gd.widthHint = SWATCH_SIZE;
        setLayoutData(gd);

        addPaintListener((e) -> {
            final GC gc = e.gc;

            gc.setAntialias(SWT.ON);

            final Color fillColor = (m_currentColor == null) ? DEFAULT_FILL_COLOR : m_currentColor;
            gc.setBackground(fillColor);
            final Point size = NodeDisplayPreview.this.getSize();
            gc.fillRoundRectangle(0, 0, size.x, size.y, ROUNDED_CORNER, ROUNDED_CORNER);

            if (m_image != null) {
                gc.setInterpolation(SWT.HIGH);
                gc.drawImage(m_image, 0, 0, m_imageBounds.width, m_imageBounds.height, ICON_INSET, ICON_INSET,
                    ICON_DIMENSION, ICON_DIMENSION);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        disposeOfColorIfPresent();
        disposeOfImageIfPresent();

        super.dispose();
    }

    void setImage(final ImageData newImage) {
        disposeOfImageIfPresent();

        if (newImage != null) {
            m_image = new Image(getDisplay(), newImage);
            m_imageBounds = m_image.getBounds();
        }

        redraw();
    }

    void setColor(final RGB newColor) {
        // we _could_ check for a setting of the same color, but it's not clear that saves us much
        disposeOfColorIfPresent();

        if (newColor != null) {
            m_currentColor = new Color(getDisplay(), newColor);
        }

        redraw();
    }

    private void disposeOfColorIfPresent() {
        if (m_currentColor != null) {
            m_currentColor.dispose();
            m_currentColor = null;
        }
    }

    private void disposeOfImageIfPresent() {
        if (m_image != null) {
            m_image.dispose();
            m_image = null;
        }
    }
}
