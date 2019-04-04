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
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 25 (ohl): created
 */
package org.knime.workbench.editor2.directannotationedit;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.knime.workbench.editor2.ViewportPinningGraphicalViewer;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;
import org.knime.workbench.editor2.figures.NodeAnnotationFigure;

/**
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class StyledTextEditorLocator implements CellEditorLocator {
    private final NodeAnnotationFigure m_figure;

    private final Viewport m_viewport;
    private org.eclipse.draw2d.geometry.Point m_lastViewportLocation;

    private final ZoomManager m_zoomManager;
    private double m_lastBoundsImpactingZoom;

    private final ExecutorService m_executorService;

    /**
     * @param figure the <code>Figure</code> which represents the GEF-version of the annotation
     */
    public StyledTextEditorLocator(final NodeAnnotationFigure figure) {
        figure.getClass(); // must not be null
        m_figure = figure;

        final ViewportPinningGraphicalViewer viewer = ViewportPinningGraphicalViewer.getActiveViewer();
        m_viewport = ((FigureCanvas)viewer.getControl()).getViewport();
        m_lastViewportLocation = m_viewport.getViewLocation();

        m_zoomManager = (ZoomManager)viewer.getProperty(ZoomManager.class.toString());
        m_lastBoundsImpactingZoom = m_zoomManager.getZoom();

        final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(3);
        m_executorService =
            new ThreadPoolExecutor(1, 3, 30, TimeUnit.SECONDS, queue, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void relocate(final CellEditor celleditor) {
        final StyledTextEditor ste = ((StyledTextEditor)celleditor);
        final Rectangle textBounds = ste.getTextBounds();
        final Composite edit = (Composite)celleditor.getControl();
        final org.eclipse.draw2d.geometry.Rectangle figBounds = m_figure.getBounds().getCopy();
        final boolean zoomHasChanged = (m_lastBoundsImpactingZoom != m_zoomManager.getZoom());
        final boolean needsJitter = !m_viewport.getViewLocation().equals(m_lastViewportLocation);
        // add OS editor borders or insets -- never verified (result always 0)
        final Rectangle trim = edit.computeTrim(0, 0, 0, 0);

        figBounds.width += trim.width;
        figBounds.height += trim.height;

        if (ste.currentAnnotationIsNodeAnnotation()) {
            final org.eclipse.draw2d.geometry.Rectangle absoluteWithZoomBounds = figBounds.getCopy();

            // adapt to zoom level and viewport (shifts x,y to view port window and grows w,h with zoom level)
            m_figure.translateToAbsolute(absoluteWithZoomBounds);

            figBounds.height = Math.max(textBounds.height, NodeAnnotationEditPart.getNodeAnnotationMinHeight());
            // add 5 pixel width to avoid flickering in auto-wrapping editors
            figBounds.width = Math.max((textBounds.width + 5), NodeAnnotationEditPart.getNodeAnnotationMinWidth());

            // center editor in case zoom != 1 (important for node annotations)
            int x = absoluteWithZoomBounds.x + (absoluteWithZoomBounds.width - figBounds.width) / 2;

            // use x,y from viewport coordinates,
            // w,h are original figure coordinates as editor doesn't grow with zoom
            final Rectangle newBounds = new Rectangle(x, absoluteWithZoomBounds.y, figBounds.width, figBounds.height);
            if (!edit.getBounds().equals(newBounds)) {
                edit.setBounds(newBounds);
            }

            if (zoomHasChanged) {
                Runnable r = () -> {
                    // the problem here is that we need wait on the associated shifting of the viewport before we
                    //      can re-place (not replace) the toolbar in the viewport. if we do this ahead of the
                    //      viewport shift, the toolbar will end up in a location that is calculated correctly but
                    //      cemented in the old viewport.  SWT!
                    try {
                        Thread.sleep(175);
                    } catch (Exception e) { } // NOPMD

                    if (!edit.isDisposed() && (edit.getDisplay() != null) && !edit.getDisplay().isDisposed()) {
                        edit.getDisplay().asyncExec(() -> {
                            ste.placeToolbarAndEnsureVisible(false);
                        });
                    }
                };

                // We don't really care if some of these get rejected, and this method gets invoked a ridiculous
                //      number of times per UI action...
                try {
                    m_executorService.execute(r);
                } catch (RejectedExecutionException e) { } // NOPMD
            }

            m_lastBoundsImpactingZoom = m_zoomManager.getZoom();
        } else {
            figBounds.height = Math.max(figBounds.height, (textBounds.height + 5));

            final int x;
            final int y;
            // if we have changed zoom levels and are a workflow annotation then we should try to preserve the
            //          original viewport-relative-location rather than potentially jumping out the viewport due to
            //          the ~precession of the center of zoom.
            if (zoomHasChanged) {
                final Point location = edit.getLocation();

                x = location.x;
                y = location.y;
            } else {
                final org.eclipse.draw2d.geometry.Rectangle absoluteWithZoomBounds = figBounds.getCopy();

                // adapt to zoom level and viewport
                // (shifts x,y to view port window and grows w,h with zoom level)
                m_figure.translateToAbsolute(absoluteWithZoomBounds);
                absoluteWithZoomBounds.translate(trim.x, trim.y);

                x = absoluteWithZoomBounds.x + (absoluteWithZoomBounds.width - figBounds.width) / 2;
                y = absoluteWithZoomBounds.y;
            }

            // use x,y from viewport coordinates,
            // w,h are original figure coordinates as editor doesn't grow with zoom
            final Rectangle newBounds = new Rectangle(x, y, figBounds.width, figBounds.height);
            if (!edit.getBounds().equals(newBounds)) {
                edit.setBounds(newBounds);

                if (zoomHasChanged || needsJitter) {
                    ste.placeToolbarAndEnsureVisible(false);
                }

                m_lastBoundsImpactingZoom = m_zoomManager.getZoom();
            }
        }

        // The DirectEditManager will have no knowledge that we have "moved" and so will not move the cell editor's
        //      light-blue-with-shadow-drop border frame unless we notify it.
        if (needsJitter) {
            edit.notifyListeners(SWT.Move, null);
        }

        m_lastViewportLocation = m_viewport.getViewLocation();
    }
}
