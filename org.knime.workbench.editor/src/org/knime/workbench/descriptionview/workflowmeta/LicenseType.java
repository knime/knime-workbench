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
 *   May 24, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.workflowmeta;

import java.util.ArrayList;

/**
 * This class embodies a license which may be applied to a workflow; it is non-instantiable outside of this class and
 * presently all license types are hard coded. Ideally this class has a static block that reads a properties file and
 * instantiates instances based on that.
 *
 * @author loki der quaeler
 */
public class LicenseType {
    /** The default license for not-yet-licensed metadata **/
    public static final String DEFAULT_LICENSE_NAME = "CC-BY-4.0";

    /**
     * @return the available licenses
     */
    public static ArrayList<LicenseType> getAvailableLicenses() {
        return AVAILABLE_LICENSES;
    }

    /**
     * @param name the display name of a license
     * @return the index within AVAILABLE_LICENSES or -1 if one could not be matched
     */
    public static int getIndexForLicenseWithName(final String name) {
        for (int i = 0; i < AVAILABLE_LICENSES.size(); i++) {
            if (AVAILABLE_LICENSES.get(i).getDisplayName().equals(name)) {
                return i;
            }
        }

        return -1;
    }

    private static final ArrayList<LicenseType> AVAILABLE_LICENSES;

    static {
        AVAILABLE_LICENSES = new ArrayList<>();

        AVAILABLE_LICENSES.add(new LicenseType(DEFAULT_LICENSE_NAME, "https://creativecommons.org/licenses/by/4.0/"));
        AVAILABLE_LICENSES.add(new LicenseType("CC0", "https://creativecommons.org/publicdomain/zero/1.0/"));
    }



    private final String m_displayName;
    private final String m_url;

    private LicenseType(final String name, final String url) {
        m_displayName = name;
        m_url = url;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return m_displayName;
    }

    /**
     * @return the url
     */
    public String getURL() {
        return m_url;
    }
}
