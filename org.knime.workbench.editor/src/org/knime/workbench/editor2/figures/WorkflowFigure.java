/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayeredPane;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/**
 * The root figure, containing potentially a progress tool tip helper and an image representing the job manager.
 *
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowFigure extends FreeformLayeredPane {
    private static final int WATERMARK_TRANSPARENCY = 10;

    private ProgressToolTipHelper m_progressToolTipHelper;

    private Image m_jobManagerFigure;

    private Image m_backgroundWatermark;

    int m_backgroundWatermarkImageWidth;

    int m_backgroundWatermarkImageHeight;

    private final TentStakeFigure m_northTentStakeFigure;
    private final TentStakeFigure m_southTentStakeFigure;
    private final TentStakeFigure m_eastTentStakeFigure;

    /**
     * New workflow root figure.
     */
    public WorkflowFigure() {
        this(null);
    }

    /**
     * New workflow root figure.
     *
     * @param backgroundWatermark an image set as a repeated background (wallpaper-like) or <code>null</code> if no
     *            background
     */
    public WorkflowFigure(final Image backgroundWatermark) {
        // not opaque, so that we can directly select on the "background" layer
        setOpaque(false);

        m_northTentStakeFigure = new TentStakeFigure();
        add(m_northTentStakeFigure);
        m_southTentStakeFigure = new TentStakeFigure();
        add(m_southTentStakeFigure);
        m_eastTentStakeFigure = new TentStakeFigure();
        add(m_eastTentStakeFigure);

        if (backgroundWatermark != null) {
            ImageData imgData = backgroundWatermark.getImageData();
            imgData.alpha = WATERMARK_TRANSPARENCY;
            m_backgroundWatermark = new Image(Display.getDefault(), imgData);
            m_backgroundWatermarkImageWidth = backgroundWatermark.getBounds().width;
            m_backgroundWatermarkImageHeight = backgroundWatermark.getBounds().height;
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics graphics) {
        super.paint(graphics);
        paintChildren(graphics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintFigure(final Graphics graphics) {
        super.paintFigure(graphics);
        if (m_jobManagerFigure != null) {
            final org.eclipse.swt.graphics.Rectangle imgBox = m_jobManagerFigure.getBounds();
            final Rectangle bounds2 = getBounds();
            graphics.drawImage(m_jobManagerFigure, 0, 0, imgBox.width, imgBox.height, bounds2.width - imgBox.width, 5,
                imgBox.width, imgBox.height + 5);
        }
        paintWatermarkWallpaper(graphics);
    }

    private void paintWatermarkWallpaper(final Graphics graphics) {
        if (m_backgroundWatermark != null) {
            Rectangle b = getBounds();
            int fromX = b.x / m_backgroundWatermarkImageWidth;
            int fromY = b.y / m_backgroundWatermarkImageHeight;
            int toX = fromX + b.width / m_backgroundWatermarkImageWidth;
            int toY = fromY + b.height / m_backgroundWatermarkImageHeight;
            for (int y = fromY; y <= toY; y++) {
                for (int x = fromX; x <= toX + y % 2; x++) {
                    //don't put the images in a grid but displace them by half the image-width in every row
                    int x_offset = y % 2 * -(m_backgroundWatermarkImageWidth / 2);
                    graphics.drawImage(m_backgroundWatermark, x * m_backgroundWatermarkImageWidth + x_offset,
                        y * m_backgroundWatermarkImageHeight);
                }
            }
        }
    }

    /**
     * @param jobManagerFigure the jobManagerFigure to set
     */
    public void setJobManagerFigure(final Image jobManagerFigure) {
        m_jobManagerFigure = jobManagerFigure;
        repaint();
    }

    /**
     * @return returns the instance of ProgressToolTipHelper set via
     *         <code>setProgressToolTipHelper(ProgressToolTipHelper)</code>
     */
    public ProgressToolTipHelper getProgressToolTipHelper() {
        return m_progressToolTipHelper;
    }

    /**
     * @param progressToolTipHelper an instance of ProgressToolTipHelper
     */
    public void setProgressToolTipHelper(final ProgressToolTipHelper progressToolTipHelper) {
        m_progressToolTipHelper = progressToolTipHelper;
    }

    /**
     * This drags out the tent stake northwards to create a top-side white space buffer in the canvas of the specified
     * pixel height (so, if the vertical scroll bar were to be moved to the zero location the user would see a
     * whitespace buffer of the specified pixel height before the start of the actual canvas at (0, 0)).
     *
     * @param pixelHeight the height in pixels of the white space buffer (a positive value;) a zero value means the
     *            stake will be move to a location where it doesn't affect the canvas' bounds.
     */
    public void placeTentStakeToAllowForTopWhitespaceBuffer(final int pixelHeight) {
        final int y = -pixelHeight;

        // only bother setting if we're "removing" the stake (returning the y-origin to 0) or if the stake is
        //      being moved into a more negative y-value
        if ((y == 0) || (m_northTentStakeFigure.getLocation().y > y)) {
            m_northTentStakeFigure.setLocation(new Point(0, y));
        }
    }

    /**
     * This drags out the tent stake eastwards to create a right-side white space buffer in the canvas of the specified
     * pixel width (so, if the horizontal scroll bar were to be moved to the previous edge of the workflow, the user
     * would find a whitespace buffer of the specified pixel width continuing rightward.)
     *
     * @param pixelWidth the width in pixels of the white space buffer (a positive value;) a zero value means the
     *            stake will be move to a location where it doesn't affect the canvas' bounds.
     */
    public void placeTentStakeToAllowForRightWhitespaceBuffer(final int pixelWidth) {
        if (pixelWidth == 0) {
            m_eastTentStakeFigure.setLocation(new Point(0, 0));

            return;
        }

        // We handle this logic differently than top since we are not tracking bounds prior to stake placement and
        //      so therefore do not know whether this width request would be satisfied by a prior stake placement
        //      (which we can judge in the top scenario, because we always have the negative space.) So we treat
        //      a call to this method as a request for an additional pixelWidth increase to the right.
        final Rectangle canvasBounds = getBounds();
        m_eastTentStakeFigure.setLocation(new Point((canvasBounds.width + pixelWidth), canvasBounds.height));
    }

    /**
     * This drags out the tent stake southwards to create a bottom-side white space buffer in the canvas of the
     * specified pixel height (so, if the vertical scroll bar were to be moved to the previous edge of the workflow, the
     * user would find a whitespace buffer of the specified pixel width continuing downward.)
     *
     * @param pixelHeight the height in pixels of the white space buffer (a positive value;) a zero value means the
     *            stake will be move to a location where it doesn't affect the canvas' bounds.
     */
    public void placeTentStakeToAllowForBottomWhitespaceBuffer(final int pixelHeight) {
        if (pixelHeight == 0) {
            m_southTentStakeFigure.setLocation(new Point(0, 0));

            return;
        }

        // We handle this logic differently than top since we are not tracking bounds prior to stake placement and
        //      so therefore do not know whether this height request would be satisfied by a prior stake placement
        //      (which we can judge in the top scenario, because we always have the negative space.) So we treat
        //      a call to this method as a request for an additional pixelHeight increase to the bottom.
        final Rectangle canvasBounds = getBounds();
        m_southTentStakeFigure.setLocation(new Point(canvasBounds.width, (canvasBounds.height + pixelHeight)));
    }

    /**
     * This will return <code>true</code> if there is a tent stake which is extending the 'canvas' at the top.
     *
     * @return <code>true</code> if the tent stake has been placed in a top-side canvas augmenting position.
     */
    public boolean northTentStakeHasBeenPlaced() {
        return (m_northTentStakeFigure.getLocation().y < 0);
    }

    /**
     * This will return <code>true</code> if there is a tent stake which is extending the 'canvas' at the bottom.
     *
     * @return <code>true</code> if the tent stake has been placed in a bottom-side canvas augmenting position.
     */
    public boolean southTentStakeHasBeenPlaced() {
        final Point location = m_southTentStakeFigure.getLocation();

        return ((location.x != 0) || (location.y != 0));
    }

    /**
     * This will return <code>true</code> if there is a tent stake which is extending the 'canvas' to the right.
     *
     * @return <code>true</code> if the tent stake has been placed in a right-side canvas augmenting position.
     */
    public boolean eastTentStakeHasBeenPlaced() {
        final Point location = m_eastTentStakeFigure.getLocation();

        return ((location.x != 0) || (location.y != 0));
    }

    /**
     * This figure is an invisible 1 x 1 figure which represent the stakes with which we 'stretch the canvas with by
     * continual re-placement (not replacement) in the workflow canvas.' We do this trivial subclassing only to allow
     * simple figure sussing when walking the children of the root figure.
     */
    private static class TentStakeFigure extends Figure {
        /**
         * Simply set the size of our figure to be 1 x 1.
         */
        public TentStakeFigure() {
            setSize(new Dimension(1, 1));
        }
    }
}
