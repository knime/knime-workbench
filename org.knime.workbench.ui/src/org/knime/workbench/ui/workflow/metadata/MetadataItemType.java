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
 *   May 26, 2019 (loki): created
 */
package org.knime.workbench.ui.workflow.metadata;

import java.util.HashMap;

/**
 * An enum representing the different types of known workflow metadata 'atoms'.
 *
 * @author loki der quaeler
 */
public enum MetadataItemType {
    /*
     * TODO the 'type' String has proved non-divergent from the enum name, and so: meaningless; unless we find
     *              a use, we should stop using it and just use toString()
     */
    /** The author metainfo **/
    AUTHOR("AUTHOR"),
    /** The description metainfo **/
    CREATION_DATE("CREATION_DATE"),
    /** The description metainfo **/
    DESCRIPTION("DESCRIPTION"),
    /** The license metainfo **/
    LICENSE("LICENSE"),
    /** The link metainfo **/
    LINK("LINK"),
    /** The tag metainfo **/
    TAG("TAG"),
    /** The title metainfo **/
    TITLE("TITLE");


    private static final HashMap<String, MetadataItemType> TYPE_ENUM_MAP = new HashMap<>();

    private String m_type;

    private MetadataItemType(final String type) {
        m_type = type;
    }

    /**
     * @return the value of the 'type' attribute of the serialized metainfo element
     */
    public String getType() {
        return m_type;
    }


    /**
     * @param type the value returned by <code>getType()</code>
     * @return the instance of the enum or null if none can be found for the provided <code>type</code>
     */
    public static MetadataItemType getInfoTypeForType (final String type) {
        synchronized (TYPE_ENUM_MAP) {
            if (TYPE_ENUM_MAP.size() == 0) {
                for (final MetadataItemType it : MetadataItemType.values()) {
                    TYPE_ENUM_MAP.put(it.getType(), it);
                }
            }
        }

        return TYPE_ENUM_MAP.get(type);
    }
}
