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
 */
package org.knime.workbench.editor2.figures;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.ConnectionProgress;

/**
 * PolylineConnection that can optionally show a label and provides animation to
 * produce a flowing effect.
 */
public class ProgressPolylineConnection extends PolylineConnection {
    /**
     * Defines whether highlighting (set via {@link #setHighlighted(boolean, boolean)}) is able to be displayed.
     */
    public static boolean PREFERENCE_DISPLAY_HIGHLIGHTING = false;
    /**
     * Defines how the usual width of a connection stroke is augmented when in highlight-mode.
     */
    public static int PREFERENCE_HIGHLIGHTED_WIDTH_DELTA = 0;

    private static Color PREFERENCE_HIGHLIGHTED_COLOR = null;
    private static Color PREFERENCE_FLOW_HIGHLIGHTED_COLOR = null;

    /**
     * Sets the color to be used by all instances of this class for stroke color when the connection is in
     *  highlighted mode; if this represents a flow variable, that color is set via
     *  {@link #setFlowVariableHighlightColor(RGB)}
     *
     * @param rgb
     * @see #setHighlighted(boolean, boolean)
     */
    public static void setHighlightColor(final RGB rgb) {
        Color oldColor = PREFERENCE_HIGHLIGHTED_COLOR;
        if (oldColor == null || !Objects.equals(rgb, oldColor.getRGB())) {
            PREFERENCE_HIGHLIGHTED_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), rgb);
            if (oldColor != null) {
                oldColor.dispose();
            }
        }
    }

    /**
     * Sets the color to be used by all instances of this class for stroke color when the connection is in
     *  highlighted mode and this instance represents a flow variable.
     *
     * @param rgb
     * @see #setHighlighted(boolean, boolean)
     */
    public static void setFlowVariableHighlightColor(final RGB rgb) {
        Color oldColor = PREFERENCE_FLOW_HIGHLIGHTED_COLOR;
        if (oldColor == null || !Objects.equals(rgb, oldColor.getRGB())) {
            PREFERENCE_FLOW_HIGHLIGHTED_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), rgb);
            if (oldColor != null) {
                oldColor.dispose();
            }
        }
    }


    /** Service to make the marching ants go slow ... not updating with each event. */
    private static ScheduledExecutorService UI_PROGESS_UPDATE_SERVICE = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactory() {
            private final AtomicInteger m_threadCreateCounter = new AtomicInteger();
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, "Delayed Progress Updater-" + m_threadCreateCounter.getAndIncrement());
            }
        });

    /** line dash style that we cycle through to create a flow animation--need 3
     * patterns to create a smooth animation. */
    protected static final int[][] DASHES = {
        {0x4, 0x4, 0x1},
        {0x4, 0x1, 0x4},
        {0x1, 0x4, 0x4}
    };

    private static final Color DEFAULT_COLOR = new Color(Display.getCurrent(), 150, 150, 150);


    /**
     * current state of animation--a value of -1 means we should go solid, otherwise; otherwise range is 0-2,
     * in which case the value is the offset in the dashes array.
     */
    protected int m_state = -1;

    /** Whether we are in highlight mode or not. */
    protected boolean m_inHighlightMode;

    private int m_currentLineWidth;

    /** display label for showing connection statistics. */
    private final Label m_label;

    /** Next to process update event or null ... used to avoid intermediate updates. */
    private final AtomicReference<ConnectionProgress> m_atomicConnectionProgressReference = new AtomicReference<>();

    /**
     * Creates a new connection.
     */
    public ProgressPolylineConnection() {
        ConnectionLocator locator = new ConnectionLocator(this);
        locator.setRelativePosition(PositionConstants.NORTH);
        locator.setGap(5);
        this.m_label = new Label("");
        add(m_label, locator);
        setForegroundColor(DEFAULT_COLOR);
        setLineWidth(1);
        m_inHighlightMode = false;
    }

    /** {@inheritDoc} */
    @Override
    protected void outlineShape(final Graphics g) {
        if (m_state < 0) {
            setLineStyle(SWT.LINE_SOLID);
        } else {
            g.setLineDash(DASHES[m_state]);
        }

        // set node connection color
        g.setForegroundColor(getForegroundColor());

        super.outlineShape(g);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLineWidth(final int w) {
        super.setLineWidth(w);

        m_currentLineWidth = w;
    }

    /**
     * Sets whether this connection should render as highlighted or regular default color; this will be ultimately
     *  ignored if the user has disabled connection highlighting via Preferences.
     *
     * @param flag if true, then render the line in the highlight color.
     * @param representsFlowVariable this should be true if this instance represents a flow variable connection
     */
    public void setHighlighted(final boolean flag, final boolean representsFlowVariable) {
        if (PREFERENCE_DISPLAY_HIGHLIGHTING || !flag) {
            setForegroundColor(flag ? (representsFlowVariable ? PREFERENCE_FLOW_HIGHLIGHTED_COLOR
                                                              : PREFERENCE_HIGHLIGHTED_COLOR)
                                    : (representsFlowVariable ? AbstractPortFigure.getFlowVarPortColor()
                                                              : DEFAULT_COLOR));
            super.setLineWidth(flag ? (m_currentLineWidth + PREFERENCE_HIGHLIGHTED_WIDTH_DELTA)
                                    : m_currentLineWidth);
            m_inHighlightMode = flag;
        }
    }

    /**
     * Update the progress. Calling this method serves two purposes. First, it
     * updates the label. Second it updates the animation.
     *
     * @param e the connection progress
     */
    public void progressChanged(final ConnectionProgress e) {
        if (m_atomicConnectionProgressReference.getAndSet(e) == null) {
            UI_PROGESS_UPDATE_SERVICE.schedule(new Runnable() {
                @Override
                public void run() {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!Display.getDefault().isDisposed()) {
                                progressChangedInternal(m_atomicConnectionProgressReference.getAndSet(null));
                            }
                        }
                    });
                }
            }, 250, TimeUnit.MILLISECONDS);
        }
    }

    private void progressChangedInternal(final ConnectionProgress e) {
        if (e.inProgress()) {
            // currently in-progress--advance to the next position in the animation
            step();
        } else {
            // not in-progress--set to solid
            setSolid();
        }
        setLabel(e.hasMessage() ? e.getMessage() : "");
    }

    private void step() {
        setLineStyle(SWT.LINE_CUSTOM);
        m_state++;
        if (m_state >= DASHES.length) {
            m_state = 0;
        }
        repaint();
    }

    private void setLabel(final String label) {
        this.m_label.setText(label);
    }

    private void setSolid() {
        m_state = -1;
        setLineStyle(SWT.LINE_SOLID);
    }
}
