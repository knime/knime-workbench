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

import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.knime.workbench.core.LayoutExemptingLayout;
import org.knime.workbench.core.util.ImageRepository;

/**
 * The message "component" - note that we are continuing to use Draw2D's {@link Label} class to calculate required
 * component height, as well as render the text, as the 'messages' have always done. I could foresee at somepoint,
 * should messages become more varied, that we would want to move to a different text rendering system which does not
 * truncate (ellipsis-ize) the text.
 *
 * This was originally an inner class to {@code ViewportPinningGraphicalViewer} but it was starting to turn into a silly
 * monolith class, so it's gotten its own package now.
 *
 * @author loki der quaeler
 */
class Message extends Canvas {
    private static final int HORIZONTAL_INSET = 10;
    private static final int VERTICAL_INSET = 9;
    private static final int INTER_BUTTON_WIDTH = 6;


    private final Long m_messageId;
    private final Label m_label;
    private final MessageAppearance m_messageAppearance;
    private final VeryLightFlatButton[] m_buttons;
    private final Rectangle[] m_buttonBounds;
    private final Dimension m_totalButtonSize;

    Message(final Composite parent, final Long id, final String message, final MessageAppearance appearance,
        final VeryLightFlatButton[] buttons) {
        super(parent, SWT.NONE);

        m_messageId = id;

        m_messageAppearance = appearance;
        m_buttons = buttons;

        m_label = new Label(message);
        m_label.setOpaque(false);
        m_label.setIcon(ImageRepository.getUnscaledIconImage(m_messageAppearance.getIcon()));
        m_label.setLabelAlignment(PositionConstants.LEFT);
        if (m_messageAppearance.getIcon() != null) {
            m_label.setIconTextGap(6);
        }

        addPaintListener((event) -> {
            final GC gc = event.gc;

            gc.setAdvanced(true);
            gc.setAntialias(SWT.ON);
            gc.setTextAntialias(SWT.ON);

            final SWTGraphics g = new SWTGraphics(gc);
            try {
                m_label.paint(g);

                if (m_buttons != null) {
                    for (final VeryLightFlatButton button : m_buttons) {
                        button.paint(gc);
                    }
                }
            } finally {
                g.dispose();
            }
        });
        if (m_buttons != null) {
            m_buttonBounds = new Rectangle[m_buttons.length];

            int totalButtonWidth = 0;
            int buttonHeight = Integer.MIN_VALUE;
            for (int i = 0; i < m_buttons.length; i++) {
                final VeryLightFlatButton button = m_buttons[i];
                final Point buttonSize = button.getSize();
                if (totalButtonWidth > 0) {
                    totalButtonWidth += INTER_BUTTON_WIDTH;
                }
                totalButtonWidth += buttonSize.x;
                buttonHeight = Math.max(buttonHeight, buttonSize.y);
            }
            m_totalButtonSize = new Dimension(totalButtonWidth, buttonHeight);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(final MouseEvent me) {
                    for (int i = 0; i < m_buttonBounds.length; i++) {
                        if (m_buttonBounds[i].contains(me.x, me.y)) {
                            // this is technically incorrect, but UX correct
                            m_buttons[i].mouseOutOfBounds();
                            redraw(m_buttonBounds[i].x, m_buttonBounds[i].y, m_buttonBounds[i].width,
                                m_buttonBounds[i].height, false);

                            final Runnable r = m_buttons[i].getAction();
                            if (r != null) {
                                getDisplay().asyncExec(r);
                            }

                            return;
                        }
                    }
                }
            });
            addMouseMoveListener((event) -> {
                for (int i = 0; i < m_buttonBounds.length; i++) {
                    final boolean repaint;
                    if (m_buttonBounds[i].contains(event.x, event.y)) {
                        repaint = m_buttons[i].mouseInBounds();
                    } else {
                        repaint = m_buttons[i].mouseOutOfBounds();
                    }

                    if (repaint) {
                        redraw(m_buttonBounds[i].x, m_buttonBounds[i].y, m_buttonBounds[i].width,
                            m_buttonBounds[i].height, false);
                    }
                }
            });
        } else {
            m_buttonBounds = null;
            m_totalButtonSize = new Dimension(0, 0);
        }

        calculateSpatialsAndReturnRequiredHeight(2 * HORIZONTAL_INSET);

        setBackground(m_messageAppearance.getFillColor());

        setVisible(false);
        moveBelow(null);

        LayoutExemptingLayout.exemptControlFromLayout(this);
    }

    int calculateSpatialsAndReturnRequiredHeight(final int width) {
        int height = 0;
        final int labelYLocation;
        final int labelWidth;
        final int labelHeight = m_label.getPreferredSize().height;

        if ((m_buttons != null) && (width > 0)) {
            final int totalButtonHeight = m_totalButtonSize.height;

            height = Math.max(totalButtonHeight, labelHeight);
            if (height == labelHeight) {
                labelYLocation = VERTICAL_INSET;
            } else {
                labelYLocation = VERTICAL_INSET + ((totalButtonHeight - labelHeight) / 2);
            }

            height += (2 * VERTICAL_INSET);

            int currentX = width - (m_totalButtonSize.width + HORIZONTAL_INSET);
            labelWidth = currentX - HORIZONTAL_INSET;
            for (int i = 0; i < m_buttons.length; i++) {
                final VeryLightFlatButton button = m_buttons[i];
                final Point buttonSize = button.getSize();
                final int buttonInset = (height - buttonSize.y) / 2;

                button.setLocation(new Point(currentX, buttonInset));
                m_buttonBounds[i] = new Rectangle(currentX, buttonInset, buttonSize.x, buttonSize.y);

                currentX += buttonSize.x + INTER_BUTTON_WIDTH;
            }
        } else {
            labelYLocation = VERTICAL_INSET;
            labelWidth = width - (2 * HORIZONTAL_INSET);
            height = labelHeight + (2 * VERTICAL_INSET);
        }

        m_label.setBounds(new Rectangle(HORIZONTAL_INSET, labelYLocation, labelWidth, labelHeight));

        setSize(width, height);

        return height;
    }

    Long getMessageId() {
        return m_messageId;
    }

    MessageAppearance getAppearance() {
        return m_messageAppearance;
    }

    @Override
    public void dispose() {
        if (!m_messageAppearance.isInternalConstant()) {
            m_messageAppearance.getFillColor().dispose();
        }

        if (m_buttons != null) {
            for (final VeryLightFlatButton button : m_buttons) {
                button.dispose();
            }
        }

        super.dispose();
    }
}
