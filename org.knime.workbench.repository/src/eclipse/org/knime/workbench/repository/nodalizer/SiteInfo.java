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
 *   Apr 5, 2019 (awalter): created
 */
package org.knime.workbench.repository.nodalizer;

import org.knime.core.util.Version;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * POJO for update site information. This includes the url and various boolean flags.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
@JsonAutoDetect
public class SiteInfo {

    private final String m_url;
    private final boolean m_enabledByDefault;
    private final boolean m_trusted;
    private final String m_name;
    private final String m_version;

    /**
     * @param url update site url
     * @param enabledByDefault if the site is enabled by default in KNIME AP
     * @param trusted if the site is trusted
     * @param name name of the update site
     * @param version version of the update site
     */
    public SiteInfo(final String url, final boolean enabledByDefault, final boolean trusted, final String name,
        final String version) {
        m_url = url;
        m_enabledByDefault = enabledByDefault;
        m_trusted = trusted;
        m_name = name;
        m_version = version;
    }

    /**
     * @param url update site url, the text following the final '/' will be used to determine the version if possible.
     *            If this text isn't a valid version, the version will be set to {@code null}
     * @param enabledByDefault if the site is enabled by default in KNIME AP
     * @param trusted if the site is trusted
     * @param name name of the update site
     */
    public SiteInfo(final String url, final boolean enabledByDefault, final boolean trusted, final String name) {
        m_url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        m_enabledByDefault = enabledByDefault;
        m_trusted = trusted;
        m_name = name;

        final String versionString = m_url.substring(m_url.lastIndexOf('/') + 1, m_url.length());
        Version v;
        try {
            v = new Version(versionString);
        } catch (final Exception ex) {
            v = null;
        }
        m_version = v == null ? null : versionString;
    }

    /**
     * Returns the update site url.
     *
     * @return the update site url
     */
    public String getURL() {
        return m_url;
    }

    /**
     * Returns if the site is enabled by default in KNIME AP.
     *
     * @return true if the site is enabled by default in KNIME AP, false otherwise
     */
    public boolean getEnabledByDefault() {
        return m_enabledByDefault;
    }

    /**
     * Returns if the site is trusted.
     *
     * @return true if the site is trusted, false otherwise
     */
    public boolean getTrusted() {
        return m_trusted;
    }

    /**
     * Returns the name of the update site.
     *
     * @return name of update site
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the version of the update site, if applicable.
     *
     * @return version of update site
     */
    public String getVersion() {
        return m_version;
    }

}
