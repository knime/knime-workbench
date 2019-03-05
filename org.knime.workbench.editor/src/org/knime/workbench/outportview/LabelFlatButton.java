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
 *   Feb 14, 2019 (loki): created
 */
package org.knime.workbench.outportview;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * This is a flat button which has only text.
 *
 * @author loki der quaeler
 */
public class LabelFlatButton extends FlatButton {
    private static final int HORIZONTAL_PADDING = 5;
    private static final int VERTICAL_PADDING = 3;
    private static Font DEFAULT_FONT = null;

    private static synchronized void initializeSWTAssetsIfNecessary() {
        if (DEFAULT_FONT == null) {
            final Display d = Display.getDefault();

            DEFAULT_FONT = new Font(d, new FontData("Helvetica", 13, SWT.NORMAL));
        }
    }


    private final Font m_font;
    private final String m_labelText;

    /**
     * This constructor uses the default font setting.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param text the text which the label should display
     */
    public LabelFlatButton(final Composite parent, final String text) {
        this(parent, text, null, null, false);
    }

    /**
     * This constructor uses the default font setting.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param text the text which the label should display
     * @param stringPrototype if non-null, the bounds of this prototype will be used to set the size of the button;
     *            otherwise the button will fill horizontally if the parent is using <code>GridLayout</code>
     */
    public LabelFlatButton(final Composite parent, final String text, final String stringPrototype) {
        this(parent, text, null, stringPrototype, false);
    }

    /**
     * This constructor uses the default font setting.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param text the text which the label should display
     * @param stringPrototype if non-null, the bounds of this prototype will be used to set the size of the button;
     *            otherwise the button will fill horizontally if the parent is using <code>GridLayout</code>
     * @param shouldToggle if true, this button will be a toggle button
     */
    public LabelFlatButton(final Composite parent, final String text, final String stringPrototype,
        final boolean shouldToggle) {
        this(parent, text, null, stringPrototype, shouldToggle);
    }

    /**
     * This constructor does not fill width.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param text the text which the label should display
     * @param font the font which should be used to render the text
     */
    public LabelFlatButton(final Composite parent, final String text, final Font font) {
        this(parent, text, font, null, false);
    }

    /**
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param text the text which the label should display
     * @param font the font which should be used to render the text
     * @param stringPrototype if non-null and the parent is using <code>GridLayout</code>, the bounds of this prototype
     *            will be used to set the size of the button; otherwise the button will fill horizontally
     * @param shouldToggle if true, this button will be a toggle button
     */
    public LabelFlatButton(final Composite parent, final String text, final Font font, final String stringPrototype,
        final boolean shouldToggle) {
        super(parent, (shouldToggle ? SWT.TOGGLE : SWT.PUSH), null, null, 0, true, null, false);

        initializeSWTAssetsIfNecessary();

        m_labelText = text;
        m_font = (font == null) ? DEFAULT_FONT : font;

        final GC gc = new GC(parent);
        gc.setAdvanced(true);
        gc.setTextAntialias(SWT.ON);
        gc.setFont(m_font);

        final Point textSize = gc.textExtent(m_labelText);
        final Point size = new Point((textSize.x + (2 * HORIZONTAL_PADDING)), (textSize.y + (2 * VERTICAL_PADDING)));
        setSize(size.x, size.y);
        if (parent.getLayout() instanceof GridLayout) {
            final GridData gd;

            if (stringPrototype == null) {
                gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);

                gd.minimumWidth = size.x;
                gd.heightHint = size.y;
            } else {
                final Point prototypeSize = gc.textExtent(stringPrototype);
                final Point resize =
                    new Point((prototypeSize.x + (2 * HORIZONTAL_PADDING)), (prototypeSize.y + (2 * VERTICAL_PADDING)));

                setSize(resize.x, resize.y);
                gd = new GridData(resize.x, resize.y);
            }

            setLayoutData(gd);
        }

        gc.dispose();
    }

    /**
     * @return the display text of this label button
     */
    public String getText() {
        return m_labelText;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handlePaintingPostProcess(final PaintEvent pe) {
        final GC gc = pe.gc;

        gc.setAdvanced(true);
        gc.setForeground(ColorConstants.black);
        gc.setFont(m_font);
        gc.setTextAntialias(SWT.ON);

        gc.drawText(m_labelText, HORIZONTAL_PADDING, VERTICAL_PADDING);

        super.handlePaintingPostProcess(pe);
    }

}
