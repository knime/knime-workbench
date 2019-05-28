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
 *   Jan 16, 2019 (awalter): created
 */
package org.knime.workbench.repository.nodalizer;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * POJO for items which have a name and description.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
@JsonAutoDetect
public class NamedField {

    private final String m_name;
    private final String m_description;
    private final Boolean m_optional;

    /**
     * Creates a POJO for a JSON field which has a name and a description.
     *
     * @param name the name of the field, can include html tags
     * @param description description of the field, can include html tags
     */
    public NamedField(final String name, final String description) {
        m_name = name;
        m_description = description;
        m_optional = null;
    }

    /**
     * Creates a POJO for a potentially optional JSON field which has a name and a description.
     *
     * @param name the name of the field, can include html tags
     * @param description description of the field, can include html tags
     * @param optional determines if the field is optional or not. If this is {@code null} it means the field does not
     *            support optional.
     */
    public NamedField(final String name, final String description, final Boolean optional) {
        m_name = name;
        m_description = description;
        m_optional = optional;
    }

    /**
     * Returns name of this field.
     *
     * @return name of the field, can include html tags
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns description of this field.
     *
     * @return description of field, can include html tags
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * Returns {@code true} if the field is optional, {@code false} if the field is required, or {@code null} if this
     * field does not support optional.
     *
     * @return if the field is optional
     */
    public Boolean getOptional() {
        return m_optional;
    }

    @JsonIgnore
    boolean isEmpty() {
        return StringUtils.isEmpty(m_name) && StringUtils.isEmpty(m_description);
    }
}
