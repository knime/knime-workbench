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
 *   Nov 7, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.descriptionview.metadata.PlatformSpecificUIisms;

/**
 * Common functionality for our edit-mode swatches.
 *
 * @author loki der quaeler
 */
abstract class AbstractSwatch extends Canvas {
    /** The length of a side of this component **/
    protected static final int SWATCH_SIZE = 48;

    private static final String N_ARY_TIMES = "\u2A09";

    private static final Cursor HAND_CURSOR = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_HAND);
    private static final Cursor DEFAULT_CURSOR = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_ARROW);


    private Rectangle m_nAryBounds;

    AbstractSwatch(final Composite parent, final Listener deleteListener) {
        super(parent, SWT.TRANSPARENT);

        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.CENTER;
        gd.heightHint = SWATCH_SIZE;
        gd.widthHint = SWATCH_SIZE;
        gd.exclude = true;
        setLayoutData(gd);

        addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(final MouseEvent me) {
                if ((m_nAryBounds != null) && m_nAryBounds.contains(me.x, me.y)) {
                    setCursor(HAND_CURSOR);
                } else {
                    setCursor(DEFAULT_CURSOR);
                }
            }
        });
        addMouseListener(new MouseListener() {
            @Override
            public void mouseDoubleClick(final MouseEvent me) { }

            @Override
            public void mouseDown(final MouseEvent me) { }

            @Override
            public void mouseUp(final MouseEvent me) {
                if ((m_nAryBounds != null) && (deleteListener != null) && m_nAryBounds.contains(me.x, me.y)) {
                    final Event e = new Event();
                    e.widget = AbstractSwatch.this;
                    deleteListener.handleEvent(e);
                }
            }
        });
        addPaintListener((e) -> {
            if (!hasContent()) {
                return;
            }

            final GC gc = e.gc;

            gc.setAntialias(SWT.ON);

            drawContent(gc);

            gc.setTextAntialias(SWT.ON);
            gc.setFont(AbstractMetaView.BOLD_CONTENT_FONT);

            if (m_nAryBounds == null) {
                final Point nArySize = gc.textExtent(N_ARY_TIMES);
                if (PlatformSpecificUIisms.OS_IS_MAC) {
                    nArySize.y = nArySize.x;    // it's an equi-sided X, but some platform fonts give it a bottom inset
                }

                final Point size = AbstractSwatch.this.getSize();
                m_nAryBounds =
                    new Rectangle((size.x - 4 - nArySize.x), ((size.y - nArySize.y) / 2), nArySize.x, nArySize.y);
            }

            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
            gc.fillOval((m_nAryBounds.x - 2), (m_nAryBounds.y - 1), (m_nAryBounds.width + 4),
                (m_nAryBounds.height + 4));

            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
            gc.drawString(N_ARY_TIMES, m_nAryBounds.x, m_nAryBounds.y, true);
        });
    }

    abstract void drawContent(final GC gc);

    abstract boolean hasContent();
}
