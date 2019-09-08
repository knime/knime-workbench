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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.swt.graphics.Color;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

/**
 * Consumers of {@link ViewportPinningGraphicalViewer} who want to add pinned messages can either use the constants
 * defined in this inner class for 'standard' message types, or create a new instance providing their own color, and
 * potentially iconographic, scheme.
 *
 * This was originally an inner class to {@code ViewportPinningGraphicalViewer} but it was starting to turn into a silly
 * monolith class, so it's gotten its own package now.
 *
 * @author loki der quaeler
 * @see ViewportPinningGraphicalViewer#displayMessage(String, MessageAppearance)
 * @see ViewportPinningGraphicalViewer#displayMessage(String, MessageAppearance, String[], Runnable[])
 */
public class MessageAppearance {
    private static final int MESSAGE_BACKGROUND_OPACITY = 171;
    private static final Color WARN_ERROR_MESSAGE_BACKGROUND = new Color(null, 255, 249, 0, MESSAGE_BACKGROUND_OPACITY);
    private static final Color INFO_MESSAGE_BACKGROUND = new Color(null, 200, 200, 255, MESSAGE_BACKGROUND_OPACITY);

    /**
     * The standard message appearance for an "info" message.
     */
    public static final MessageAppearance INFO =
        new MessageAppearance(2, INFO_MESSAGE_BACKGROUND, SharedImages.Info, true);
    /**
     * The standard message appearance for an "warning" message.
     */
    public static final MessageAppearance WARNING =
        new MessageAppearance(1, WARN_ERROR_MESSAGE_BACKGROUND, SharedImages.Warning, true);
    /**
     * The standard message appearance for an "error" message.
     */
    public static final MessageAppearance ERROR =
        new MessageAppearance(0, WARN_ERROR_MESSAGE_BACKGROUND, SharedImages.Error, true);


    private int m_index;
    private final Color m_fillColor;
    private final SharedImages m_icon;
    private boolean m_internalConstant;

    /**
     * The available constructor for consumers who want to display a message with a non-predefined appearance.
     *
     * @param c the background color for the message. <b>NOTE:</b> this instance will be disposed when the
     *              message ceases to be displayed.
     * @param icon the icon to display, or null - it will not be disposed
     */
    public MessageAppearance(final Color c, final SharedImages icon) {
        assert(c != null);
        m_index = Integer.MIN_VALUE;
        m_fillColor = c;
        m_icon = icon;
        m_internalConstant = false;
    }

    private MessageAppearance(final int index, final Color c, final SharedImages icon, final boolean internal) {
        this(c, icon);
        m_index = index;
        m_internalConstant = internal;
    }

    /**
     * @return the fill color associated with this message type
     */
    public Color getFillColor() {
        return m_fillColor;
    }

    /**
     * @return the icon associated with this message type
     */
    public SharedImages getIcon() {
        return m_icon;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final HashCodeBuilder hcb = new HashCodeBuilder();
        return hcb.append(m_fillColor).append(m_icon).append(m_index).append(m_internalConstant).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final MessageAppearance other = (MessageAppearance)obj;
        final EqualsBuilder eb = new EqualsBuilder();
        return eb.append(m_fillColor, other.m_fillColor)
                 .append(m_icon, other.m_icon)
                 .append(m_index, other.m_index)
                 .append(m_internalConstant, other.m_internalConstant)
                 .isEquals();
    }

    /**
     * @return the internal ordering index for the message type; note that this index is a holdover from the original
     *              design in which message ordering was based on type (e.g info; warning; etc..) See the developer
     *              notes at the end of this class.
     */
    int getIndex() {
        return m_index;
    }

    /**
     * @return true if this is a 'constant' class (i.e one of INFO, ERROR, WARNING)
     */
    boolean isInternalConstant() {
        return m_internalConstant;
    }
}
