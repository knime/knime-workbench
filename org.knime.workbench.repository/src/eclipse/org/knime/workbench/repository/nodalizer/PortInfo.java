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
 *   Jan 14, 2019 (awalter): created
 */
package org.knime.workbench.repository.nodalizer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * POJO for KNIME port information.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
@JsonAutoDetect
@JsonPropertyOrder({"objectClass", "color", "dataType", "optional", "name", "description"})
public class PortInfo {

    private final String m_name;
    private final String m_description;
    private final boolean m_optional;
    private final String m_dataType;
    private final String m_color;
    private final String m_objectClass;

    /**
     * Creates a POJO representing a single in/outport of a node.
     *
     * @param name name of the port
     * @param description description of the port, may contain HTML
     * @param optional if the port is optional
     * @param dataType type of port
     * @param color port color
     * @param objectClass port object class
     */
    public PortInfo(final String name, final String description, final boolean optional, final String dataType,
        final String color, final String objectClass) {
        m_name = name;
        m_description = description;
        m_optional = optional;
        m_color = color;
        m_dataType = dataType;
        m_objectClass = objectClass;
    }

    /**
     * Returns the name of the port.
     *
     * @return the name of the port
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns description of this port.
     *
     * @return description of the port, may contain HTML
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * Returns {@code true} if this port is optional, otherwise {@code false}.
     *
     * @return {@code true} if the port is optional, otherwise {@code false}
     */
    public boolean getOptional() {
        return m_optional;
    }

    /**
     * Returns the color of this port as a HEX string.
     *
     * @return the color of the port as a HEX string
     */
    public String getColor() {
        return m_color;
    }

    /**
     * Returns the data type of this port.
     *
     * @return the data type of the port
     */
    public String getDataType() {
        return m_dataType;
    }

    /**
     * Returns the port object class.
     *
     * @return the object class
     */
    public String getObjectClass() {
        return m_objectClass;
    }
}
