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
 *   May 9, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.atoms;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.xml.sax.SAXException;

/**
 * The atom representing tags.
 *
 * @author loki der quaeler
 */
public class TagMetaInfoAtom extends MetaInfoAtom {
    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    public TagMetaInfoAtom(final String label, final String value, final boolean readOnly) {
        super(MetadataItemType.TAG, label, value, readOnly);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
    @Override
    public void populateContainerForDisplay(final Composite parent) {
        new TagChiclet(parent, false);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
    @Override
    public void populateContainerForEdit(final Composite parent) {
        new TagChiclet(parent, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void focus() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        if (hasContent()) {
            save(parentElement, MetadataXML.TEXT);
        }
    }


    private class TagChiclet extends CloseableLabel {
        private static final int HORIZONTAL_INSET = 9;
        private static final int VERTICAL_INSET = 3;


        private TagChiclet(final Composite parent, final boolean forEdit) {
            super(parent, forEdit, HORIZONTAL_INSET, VERTICAL_INSET);

            addPaintListener((paintEvent) -> {
                final GC gc = paintEvent.gc;
                final Rectangle r = getClientArea();

                gc.setAdvanced(true);
                gc.setAntialias(SWT.ON);
                gc.setBackground(AbstractMetaView.GENERAL_FILL_COLOR);
                gc.fillRoundRectangle((r.x + 1), (r.y + 1), (r.width - 2), (r.height - 2), r.height, r.height);

                gc.setTextAntialias(SWT.ON);
                gc.drawString(m_value, (r.x + HORIZONTAL_INSET), (r.y + VERTICAL_INSET));

                paintNAry(gc);
            });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Point computeSize (final int wHint, final int hHint, final boolean changed) {
            return m_calculatedSize;
        }
    }
}
