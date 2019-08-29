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
 *   Aug 28, 2019 (loki): created
 */
package org.knime.workbench.editor2;

import java.text.DecimalFormat;

import org.eclipse.draw2d.FreeformFigure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.editparts.ZoomManager;

/**
 * We do this lame subclassing because ZoomManager both makes its <code>zoom</code> ivar private, and provides no way to
 * set it without involving the re-setting of the viewport location, which we'd like to handle ourselves.
 *
 * @author loki der quaeler
 */
public class CustomZoomManager extends ZoomManager {
    private static final DecimalFormat ZOOM_TEXT_FORMATTER = new DecimalFormat("####%"); //$NON-NLS-1$


    private double m_zoom = 1.0;

    /**
     * Creates a new CustomZoomManager.
     *
     * @param pane The ScalableFigure associated with this ZoomManager
     * @param viewport The Viewport assoicated with this ZoomManager
     */
    public CustomZoomManager(final ScalableFigure pane, final Viewport viewport) {
        super(pane, viewport);
    }

    /**
     * @deprecated Use {@link #CustomZoomManager(ScalableFigure, Viewport)} instead. Creates a new ZoomManager
     * @param pane The ScalableFreeformLayeredPane associated with this ZoomManager
     * @param viewport The Viewport assoicated with this viewport
     */
    @Deprecated
    public CustomZoomManager(final ScalableFreeformLayeredPane pane, final Viewport viewport) {
        super(pane, viewport);
    }


    //
    //
    //
    // Overridden with new functionality
    //
    //
    //

    /**
     * Sets the zoom level to the given value. If the zoom is out of the min-max
     * range, it will be ignored.
     *
     * @param newZoom the new zoom level
     */
    @Override
    public void setZoom(final double newZoom) {
        setZoom(newZoom, null);
    }

    /**
     * Sets the zoom level to the given value. If the zoom is out of the min-max
     * range, it will be ignored.
     *
     * @param newZoom the new zoom level
     * @param mousePoint the point to keep under the mouse
     */
    public void setZoom(final double newZoom, final Point mousePoint) {
        double zoomToUse = Math.min(getMaxZoom(), newZoom);
        zoomToUse = Math.max(getMinZoom(), zoomToUse);
        if (m_zoom != zoomToUse) {
            final Viewport v = getViewport();
            final double originalZoom = m_zoom;
            final double invertedScaleRatio = (originalZoom / zoomToUse);

            m_zoom = zoomToUse;
            getScalableFigure().setScale(m_zoom);
            getViewport().validate();

            if (mousePoint != null) {
                Point workingMP = mousePoint.getCopy();
                final Point nScaledMP = workingMP.scale(m_zoom);

                workingMP = nScaledMP.getCopy();
                final Point ratioMP = workingMP.scale(invertedScaleRatio);
                final Dimension translate = new Dimension(nScaledMP.x - ratioMP.x, nScaledMP.y - ratioMP.y);

                Point viewLocation = v.getViewLocation();
                viewLocation.translate(translate);
                v.setViewLocation(viewLocation);
            }

            fireZoomChanged();
        }
    }


    //
    //
    //
    // Duplicated only to use our private zoom double value
    //
    //
    //

    private double getFitXZoomLevel(final int which) {
        IFigure fig = getScalableFigure();

        final Dimension available = getViewport().getClientArea().getSize();
        final Dimension desired;
        if (fig instanceof FreeformFigure) {
            desired = ((FreeformFigure)fig).getFreeformExtent().getCopy().union(0, 0).getSize();
        } else {
            desired = fig.getPreferredSize().getCopy();
        }

        desired.width -= fig.getInsets().getWidth();
        desired.height -= fig.getInsets().getHeight();

        while (fig != getViewport()) {
            available.width -= fig.getInsets().getWidth();
            available.height -= fig.getInsets().getHeight();
            fig = fig.getParent();
        }

        final double scaleX = Math.min(available.width * m_zoom / desired.width, getMaxZoom());
        final double scaleY = Math.min(available.height * m_zoom / desired.height, getMaxZoom());
        if (which == 0) {
            return scaleX;
        }
        if (which == 1) {
            return scaleY;
        }
        return Math.min(scaleX, scaleY);
    }

    /**
     * Calculates and returns the zoom percent required so that the entire height of the {@link #getScalableFigure()
     * scalable figure} is visible on the screen. This is the zoom level associated with {@link #FIT_HEIGHT}.
     *
     * Overridden only because the superclass' <code>getFitXZoomLevel</code> is private.
     *
     * @return zoom setting required to fit the scalable figure vertically on the screen
     */
    @Override
    protected double getFitHeightZoomLevel() {
        return getFitXZoomLevel(1);
    }

    /**
     * Calculates and returns the zoom percentage required to fit the entire {@link #getScalableFigure() scalable
     * figure} on the screen. This is the zoom setting associated with {@link #FIT_ALL}. It is the minimum of
     * {@link #getFitHeightZoomLevel()} and {@link #getFitWidthZoomLevel()}.
     *
     * Overridden only because the superclass' <code>getFitXZoomLevel</code> is private.
     *
     * @return zoom setting required to fit the entire scalable figure on the screen
     */
    @Override
    protected double getFitPageZoomLevel() {
        return getFitXZoomLevel(2);
    }

    /**
     * Calculates and returns the zoom percentage required so that the entire width of the {@link #getScalableFigure()
     * scalable figure} is visible on the screen. This is the zoom setting associated with {@link #FIT_WIDTH}.
     *
     * Overridden only because the superclass' <code>getFitXZoomLevel</code> is private.
     *
     * @return zoom setting required to fit the scalable figure horizontally on the screen
     */
    @Override
    protected double getFitWidthZoomLevel() {
        return getFitXZoomLevel(0);
    }

    /**
     * Returns the zoom level that is one level higher than the current level.
     * If zoom level is at maximum, returns the maximum.
     *
     * @return double The next zoom level
     */
    @Override
    public double getNextZoomLevel() {
        final double[] zoomLevels = getZoomLevels();
        for (int i = 0; i < zoomLevels.length; i++) {
            if (zoomLevels[i] > m_zoom) {
                return zoomLevels[i];
            }
        }
        return getMaxZoom();
    }

    /**
     * Returns the zoom level that is one level higher than the current level.
     * If zoom level is at maximum, returns the maximum.
     *
     * @return double The previous zoom level
     */
    @Override
    public double getPreviousZoomLevel() {
        final double[] zoomLevels = getZoomLevels();
        for (int i = 1; i < zoomLevels.length; i++) {
            if (zoomLevels[i] >= m_zoom) {
                return zoomLevels[i - 1];
            }
        }
        return getMinZoom();
    }

    /**
     * Returns the current zoom level.
     *
     * @return double the zoom level
     */
    @Override
    public double getZoom() {
        return m_zoom;
    }

    /**
     * Returns the current zoom level as a percentage formatted String
     *
     * @return String The current zoom level as a String
     */
    @Override
    public String getZoomAsText() {
        return ZOOM_TEXT_FORMATTER.format(m_zoom * getUIMultiplier());
    }
}
