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
 *   Mar 15, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;

/**
 * This class came into existence to address AP-5062 which asks for the functionality of CTRL+MouseWheel should change
 * the zoom level on the workflow editor.
 *
 * There will be one of these listeners per WorkflowEditor instance (since there is a 1-1 between an instance of such
 * and a ZoomManager instance.)
 */
final class ZoomWheelListener implements MouseWheelListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ZoomWheelListener.class);


    private final CustomZoomManager m_zoomManager;

    private final FigureCanvas m_figureCanvas;

    private double m_alternateZoomDelta;

    /**
     * Default constructor.
     *
     * @param zm
     * @param fc the canvas from which we want wheel event notifications
     */
    ZoomWheelListener(final CustomZoomManager zm, final FigureCanvas fc) {
        m_zoomManager = zm;

        m_figureCanvas = fc;
        m_figureCanvas.addMouseWheelListener(this);

        m_alternateZoomDelta = 0;
    }

    /**
     * @param delta expected to be integer percentage points (i.e 5 => 5%)
     */
    void setZoomDelta(final int delta) {
        m_alternateZoomDelta = delta / 100.0;
    }

    /**
     * Should be called as part of workflow disposition, before the parent figure canvas has been disposed.
     */
    public void dispose() {
        Display.getCurrent().asyncExec(() -> {
            try {
                ZoomWheelListener outer = ZoomWheelListener.this;
                if (outer.m_figureCanvas.isDisposed()) {
                    // this otherwise causes a "widget disposed" while removing the mouse listener
                    return;
                }
                outer.m_figureCanvas.removeMouseWheelListener(this);
            } catch (Exception e) {
                // canvas has likely already gone.
                LOGGER.debug("We encountered an exception disposing of the zoom wheel listener.", e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseScrolled(final MouseEvent me) {
        if ((me.stateMask & SWT.MOD1) == SWT.MOD1) {
            final int scrollEventChange = me.count;
            final double newZoom;

            if ((me.stateMask & SWT.ALT) == SWT.ALT) {
                final double delta = m_alternateZoomDelta * ((scrollEventChange > 0) ? 1.0 : -1.0);
                newZoom = m_zoomManager.getZoom() + delta;
            } else {
                if (scrollEventChange < 0) {
                    newZoom = m_zoomManager.getPreviousZoomLevel();
                } else {
                    newZoom = m_zoomManager.getNextZoomLevel();
                }
            }

            m_zoomManager.setZoom(newZoom, new Point(me.x, me.y));
        }
    }

}
