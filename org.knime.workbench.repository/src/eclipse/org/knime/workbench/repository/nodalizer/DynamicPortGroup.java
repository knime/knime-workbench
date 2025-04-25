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
 *   Nov 29, 2019 (awalter): created
 */
package org.knime.workbench.repository.nodalizer;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * POJO for representing a dynamic port group.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
@JsonAutoDetect
@JsonPropertyOrder({"groupName", "groupDescription", "types"})
public class DynamicPortGroup {

    private final String m_groupName;
    private final String m_groupDescription;
    private final DynamicPortType[] m_types;

    /**
     * Creates a new POJO representing a single dynamic port group.
     *
     * @param groupName the name of this group
     * @param groupDescription the HTML description of this group
     * @param types an array of the port types this group supports
     */
    public DynamicPortGroup(final String groupName, final String groupDescription, final DynamicPortType[] types) {
        m_groupName = groupName;
        m_groupDescription = groupDescription;
        if (types == null) {
            m_types = new DynamicPortType[0];
        } else {
            m_types = types;
            Arrays.sort(m_types, (t1, t2) -> t1.getObjectClass().compareTo(t2.getObjectClass())); // order by object class for serialization
        }
    }

    /**
     * Returns the group name of this group.
     *
     * @return the group name
     */
    public String getGroupName() {
        return m_groupName;
    }

    /**
     * Returns the HTML group description of this group.
     *
     * @return the description
     */
    public String getGroupDescription() {
        return m_groupDescription;
    }

    /**
     * Returns the {@link DynamicPortType}s supported by this dynamic port group.
     *
     * @return the types
     */
    public DynamicPortType[] getTypes() {
        return m_types;
    }

    /**
     * A POJO representing a single port type supported by dynamic port group.
     */
    @JsonAutoDetect
    @JsonPropertyOrder({"objectClass", "color", "dataType"})
    public static final class DynamicPortType {

        private final String m_objectClass;
        private final String m_dataType;
        private final String m_color;

        /**
         * Creates a single port type for a dynamic port.
         *
         * @param objectClass the object class
         * @param dataType the data type
         * @param color the HEX color
         */
        public DynamicPortType(final String objectClass, final String dataType, final String color) {
            m_objectClass = objectClass;
            m_dataType = dataType;
            m_color = color;
        }

        /**
         * Returns the object class for this port type.
         *
         * @return the object class
         */
        public String getObjectClass() {
            return m_objectClass;
        }

        /**
         * Returns the data type of this port type.
         *
         * @return the data type
         */
        public String getDataType() {
            return m_dataType;
        }

        /**
         * Returns the color of this port type as a HEX string.
         *
         * @return the color of the port type as a HEX string
         */
        public String getColor() {
            return m_color;
        }
    }
}
