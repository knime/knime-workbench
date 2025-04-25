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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.util.Version;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * POJO for extension information.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
@JsonAutoDetect
@JsonPropertyOrder({"id", "symbolicName", "name", "version", "vendor", "owner", "categoryPath", "updateSite",
    "description", "descriptionUrl", "copyright", "license"})
public class ExtensionInfo {

    private String m_name;
    private String m_description;
    private String m_descriptionUrl;
    private String m_symbolicName;
    private String m_vendor;
    private Version m_version;
    private SiteInfo m_updateSite;
    private LicenseInfo m_license;
    private String m_copyright;
    private List<String> m_categoryPath;
    private String m_owner;

    @JsonIgnore
    private boolean m_hasNodes;

    /**
     * Returns the extension's id.
     *
     * @return the id
     */
    public String getId() {
        // TODO: The ID is currently taken from the symbolic name. This means if the extension changes its symbolic
        // name the ID will change. But once we read multiple extension version this ID will no longer be unique.
        try {
            byte[] digest =
                MessageDigest.getInstance("SHA-256").digest(getSymbolicName().getBytes(StandardCharsets.UTF_8));
            return "*" + Base64.getUrlEncoder().encodeToString(Arrays.copyOf(digest, 12));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns the extension's name.
     *
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the extension's description.
     *
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * Returns the extension's description url.
     *
     * @return the description url
     */
    public String getDescriptionUrl() {
        return m_descriptionUrl;
    }

    /**
     * Returns the extension's symbolicName.
     *
     * @return the symbolicName
     */
    public String getSymbolicName() {
        return m_symbolicName;
    }

    /**
     * Returns the extension's vendor.
     *
     * @return the vendor
     */
    public String getVendor() {
        return m_vendor;
    }

    /**
     * Returns the extension's version.
     *
     * <p>
     * This getter is ignored by jackson.
     * </p>
     *
     * @return the version
     */
    @JsonIgnore
    public Version getVersion() {
        return m_version;
    }

    /**
     * Returns the extension's update site information.
     *
     * @return the update site information
     */
    public SiteInfo getUpdateSite() {
        return m_updateSite;
    }

    /**
     * Returns the extension's license information.
     *
     * @return the license information
     */
    public LicenseInfo getLicense() {
        return m_license;
    }

    /**
     * Returns the extension's copyright.
     *
     * @return the copyright
     */
    public String getCopyright() {
        return m_copyright;
    }

    /**
     * Returns the extension's category path.
     *
     * @return the category path
     */
    public List<String> getCategoryPath() {
        return m_categoryPath;
    }

    /**
     * Returns the owner of this extension.
     *
     * @return the owner
     */
    public String getOwner() {
        return StringUtils.isEmpty(m_owner) ? null : m_owner;
    }

    /**
     * Sets the extension's name.
     *
     * @param name the name to set
     */
    public void setName(final String name) {
        m_name = name;
    }

    /**
     * Sets the extension's description.
     *
     * @param description the description to set
     */
    public void setDescription(final String description) {
        m_description = description;
    }

    /**
     * Sets the extension's description url.
     *
     * @param descriptionUrl the description url to set
     */
    public void setDescriptionUrl(final String descriptionUrl) {
        m_descriptionUrl = descriptionUrl;
    }

    /**
     * Sets the extension's symbolicName.
     *
     * @param symbolicName the symbolicName to set
     */
    public void setSymbolicName(final String symbolicName) {
        m_symbolicName = symbolicName;
    }

    /**
     * Sets the extension's vendor.
     *
     * @param vendor the vendor to set
     */
    public void setVendor(final String vendor) {
        m_vendor = vendor;
    }

    /**
     * Sets the extension's version.
     *
     * @param version the version to set
     */
    public void setVersion(final Version version) {
        m_version = version;
    }

    /**
     * Sets the extension's update site information.
     *
     * @param updateSite the update site information to set
     */
    public void setUpdateSite(final SiteInfo updateSite) {
        m_updateSite = updateSite;
    }

    /**
     * Sets the extension's license information.
     *
     * @param license the license information to set
     */
    public void setLicense(final LicenseInfo license) {
        m_license = license;
    }

    /**
     * Sets the extension's copyright.
     *
     * @param copyright the copyright to set
     */
    public void setCopyright(final String copyright) {
        m_copyright = copyright;
    }

    /**
     * Sets the extension's category path.
     *
     * @param categoryPath the category path to set
     */
    public void setCategoryPath(final List<String> categoryPath) {
        m_categoryPath = categoryPath;
    }

    /**
     * Sets the extension's owner.
     *
     * @param owner the owner name to set
     */
    public void setOwner(final String owner) {
        m_owner = owner;
    }

    /**
     * Returns the version of this extension as a string.
     *
     * <p>
     * This getter is used by jackson for serialization.
     * <p>
     *
     * @return the version as a string
     */
    @JsonProperty(value = "version")
    String getVersionString() {
        return m_version == null ? null : m_version.toString();
    }

    @JsonIgnore
    boolean hasNodes() {
        return m_hasNodes;
    }

    @JsonIgnore
    void setHasNodes(final boolean hasNodes) {
        m_hasNodes = hasNodes;
    }

    /**
     * POJO for extension license information.
     */
    @JsonAutoDetect
    @JsonPropertyOrder({"name", "url", "text"})
    public static class LicenseInfo {

        private String m_licenseName;
        private String m_licenseText;
        private String m_licenseUrl;

        /**
         * Returns the license's name.
         *
         * @return the name
         */
        public String getName() {
            return m_licenseName;
        }

        /**
         * Returns the license's text.
         *
         * @return the text
         */
        public String getText() {
            return m_licenseText;
        }

        /**
         * Returns the license's url.
         *
         * @return the url
         */
        public String getUrl() {
            return m_licenseUrl;
        }

        /**
         * Sets the license's name.
         *
         * @param name the name to set
         */
        public void setName(final String name) {
            m_licenseName = name;
        }

        /**
         * Sets the license's text
         *
         * @param text the text to set
         */
        public void setText(final String text) {
            m_licenseText = text;
        }

        /**
         * Sets the license's url.
         *
         * @param url the url to set
         */
        public void setUrl(final String url) {
            m_licenseUrl = url;
        }
    }
}
