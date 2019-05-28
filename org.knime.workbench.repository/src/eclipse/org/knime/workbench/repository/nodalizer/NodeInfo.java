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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.knime.core.util.workflowalizer.NodeAndBundleInformation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * POJO for KNIME Node information.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
@JsonAutoDetect
public class NodeInfo {

    private String m_title;
    private List<String> m_path;
    private String m_factoryName;
    private BundleInformation m_bundleInformation;
    private SiteInfo m_additionalSiteInformation;
    private String m_description;
    private List<DialogOptionGroup> m_dialog;
    private List<NamedField> m_views;
    private NamedField m_interactiveView;
    private LinkInformation[] m_links;
    private String m_icon;
    private String m_nodeType;
    private boolean m_deprecated;
    private boolean m_streamable;
    private PortInfo[] m_inPorts;
    private PortInfo[] m_outPorts;

    /**
     * Returns the title of this node.
     *
     * @return the title of the node
     */
    public String getTitle() {
        return m_title;
    }

    /**
     * Returns a {@code List} of this node's path components.
     *
     * @return a {@code List} of path components
     */
    public List<String> getPath() {
        return m_path == null ? Collections.emptyList() : m_path;
    }

    /**
     * Returns the node's factory name (factory class + factory_settings).
     *
     * @return the factory name
     */
    public String getFactoryName() {
        return m_factoryName;
    }

    /**
     * Returns this node's unique ID encoded as base64url.
     *
     * @return a 17 character unique id
     */
    public String getId() {
        // TODO the ID is currently derived from the factory class name. As soon as that changes, also the ID
        // changes. We still need to find a way to define a truly unique id for nodes.

        try {
            byte[] digest =
                MessageDigest.getInstance("SHA-256").digest(getFactoryName().getBytes(StandardCharsets.UTF_8));
            return "*" + Base64.getUrlEncoder().encodeToString(Arrays.copyOf(digest, 12));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns the bundle information for this node.
     *
     * @return the bundle information for this node
     */
    public BundleInformation getBundleInformation() {
        return m_bundleInformation;
    }

    /**
     * Returns the {@link SiteInfo} for this node.
     *
     * @return the site information
     */
    public SiteInfo getAdditionalSiteInformation() {
        return m_additionalSiteInformation;
    }

    /**
     * Returns this node's description.
     *
     * @return the node's description, may contain HTML
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * Returns the node's dialog groups' (tabs) information.
     *
     * @return the node's dialog groups' (tabs) information
     */
    public List<DialogOptionGroup> getDialog() {
        return m_dialog == null ? Collections.emptyList() : m_dialog;
    }

    /**
     * Returns a {@code List} of this node's views.
     *
     * @return the node's views (name and description)
     */
    public List<NamedField> getViews() {
        return m_views == null ? Collections.emptyList() : m_views;
    }

    /**
     * Returns the node's interactive view metadata.
     *
     * @return the node's interactive view (name and description)
     */
    public NamedField getInteractiveView() {
        return m_interactiveView;
    }

    /**
     * Returns an array of more information links.
     *
     * @return an array of more information links
     */
    public LinkInformation[] getLinks() {
        return m_links == null ? new LinkInformation[0] : m_links;
    }

    /**
     * Returns the node's icon as base64 encoded string.
     *
     * @return the node's icon as a base64 encoded string
     */
    public String getIcon() {
        return m_icon;
    }

    /**
     * Returns the node's type.
     *
     * @return the node's type
     */
    public String getNodeType() {
        return m_nodeType;
    }

    /**
     * Returns {@code true} if the node is deprecated.
     *
     * @return {@code true} if the node is deprecated
     */
    public boolean getDeprecated() {
        return m_deprecated;
    }

    /**
     * Returns {@code true} if the node is streamable.
     *
     * @return {@code true} if the node is streamable
     */
    public boolean getStreamable() {
        return m_streamable;
    }

    /**
     * Returns an array of {@link PortInfo} for node each of the node's inports.
     *
     * @return an array of {@link PortInfo} for node each of the node's inports
     */
    public PortInfo[] getInPorts() {
        return m_inPorts == null ? new PortInfo[0] : m_inPorts;
    }

    /**
     * Returns an array of {@link PortInfo} for node each of the node's outports.
     *
     * @return an array of {@link PortInfo} for node each of the node's outports
     */
    public PortInfo[] getOutPorts() {
        return m_outPorts == null ? new PortInfo[0] : m_outPorts;
    }

    /**
     * Sets the node's title.
     *
     * @param title the title
     */
    public void setTitle(final String title) {
        m_title = title;
    }

    /**
     * Sets the node's path
     *
     * @param path a {@code List<String>} containing the node's path, where the element at index 0 is the path root
     */
    public void setPath(final List<String> path) {
        m_path = path;

    }

    /**
     * Sets the node's factory name.
     *
     * @param factoryName a unique id including the factory class and factory_settings
     */
    public void setFactoryName(final String factoryName) {
        m_factoryName = factoryName;
    }

    /**
     * Sets this node's bundle information.
     *
     * @param nodeAndBundleInfo the {@link NodeAndBundleInformation} for this node, only the bundle information will be
     *            preserved.
     * @param extensionId unique ID of extension (feature) to set
     */
    public void setBundleInformation(final NodeAndBundleInformation nodeAndBundleInfo, final String extensionId) {
        m_bundleInformation = new BundleInformation(nodeAndBundleInfo, extensionId);
    }

    /**
     * Sets the node's {@link SiteInfo}.
     *
     * @param additionalSiteInformation update site information to set
     */
    public void setAdditionalSiteInformation(final SiteInfo additionalSiteInformation) {
        m_additionalSiteInformation = additionalSiteInformation;
    }

    /**
     * Sets the node's description
     *
     * @param description the node's description, may contain HTML
     */
    public void setDescription(final String description) {
        m_description = description;
    }

    /**
     * Sets the node's dialog information
     *
     * @param dialog a {@code List} of {@link DialogOptionGroup}s representing the dialog options per tab for the node
     */
    public void setDialog(final List<DialogOptionGroup> dialog) {
        if (dialog == null) {
            m_dialog = null;
            return;
        }
        final List<DialogOptionGroup> cleaned = new ArrayList<>();
        for (final DialogOptionGroup dog : dialog) {
            if (!dog.isEmpty()) {
                cleaned.add(dog);
            }
        }
        m_dialog = cleaned;
    }

    /**
     * Sets the node's views
     *
     * @param views the node's views (name and description)
     */
    public void setViews(final List<NamedField> views) {
        if (views == null) {
            m_views = null;
            return;
        }
        final List<NamedField> cleaned = new ArrayList<>();
        for (final NamedField nf : views) {
            if (!nf.isEmpty()) {
                cleaned.add(nf);
            }
        }
        m_views = cleaned;
    }

    /**
     * Sets the node's interactive view
     *
     * @param interactiveView the node's interactive view (name and description)
     */
    public void setInteractiveView(final NamedField interactiveView) {
        if (interactiveView != null && !interactiveView.isEmpty()) {
            m_interactiveView = interactiveView;
            return;
        }
        m_interactiveView = null;
    }

    /**
     * Sets the node's more information links
     *
     * @param links an array of {@code LinkInformation} representing the more information links for this node
     */
    public void setLinks(final LinkInformation[] links) {
        m_links = links;
    }

    /**
     * Sets the node's icon.
     *
     * @param icon the base64 encoded representation of the icon
     */
    public void setIcon(final String icon) {
        m_icon = icon;
    }

    /**
     * Sets the node's type.
     *
     * @param nodeType the node type to set
     */
    public void setNodeType(final String nodeType) {
        m_nodeType = nodeType;
    }

    /**
     * Sets if the node is deprecated or not.
     *
     * @param deprecated if the node is deprecated or not
     */
    public void setDeprecated(final boolean deprecated) {
        m_deprecated = deprecated;
    }

    /**
     * Sets if the node is streamable or not.
     *
     * @param streamable if the node is streamable or not
     */
    public void setStreamable(final boolean streamable) {
        m_streamable = streamable;
    }

    /**
     * Sets the metadata for the node's inports.
     *
     * @param inPorts an array of {@link PortInfo} representing the node's inports
     */
    public void setInPorts(final PortInfo[] inPorts) {
        m_inPorts = inPorts;
    }

    /**
     * Sets the metadata for the node's outports.
     *
     * @param outPorts an array of {@link PortInfo} representing the node's outports
     */
    public void setOutPorts(final PortInfo[] outPorts) {
        m_outPorts = outPorts;
    }

    // -- Helper Classes --

    @JsonAutoDetect(getterVisibility = Visibility.NON_PRIVATE)
    static final class BundleInformation {

        private final NodeAndBundleInformation m_nabi;
        private final String m_extensionId;

        private BundleInformation(final NodeAndBundleInformation nabi, final String extensionId) {
            m_nabi = nabi;
            m_extensionId = extensionId;
        }

        String getFeatureVersion() {
            if (m_nabi.getFeatureVersion().isPresent()) {
                return m_nabi.getFeatureVersion().get().toString();
            }
            return null;
        }

        String getFeatureVendor() {
            return m_nabi.getFeatureVendor().orElse(null);
        }

        String getFeatureName() {
            return m_nabi.getFeatureName().orElse(null);
        }

        String getFeatureSymbolicName() {
            return m_nabi.getFeatureSymbolicName().orElse(null);
        }

        String getBundleVersion() {
            if (m_nabi.getBundleVersion().isPresent()) {
                return m_nabi.getBundleVersion().get().toString();
            }
            return null;
        }

        String getBundleVendor() {
            return m_nabi.getBundleVendor().orElse(null);
        }

        String getBundleName() {
            return m_nabi.getBundleName().orElse(null);
        }

        String getBundleSymbolicName() {
            return m_nabi.getBundleSymbolicName().orElse(null);
        }

        String getExtensionId() {
            return m_extensionId;
        }
    }

    /**
     * POJO for links.
     *
     * @author Alison Walter, KNIME GmbH, Konstanz, Germany
     */
    @JsonAutoDetect(getterVisibility = Visibility.NON_PRIVATE)
    public static final class LinkInformation {

        private final String m_url;
        private final String m_text;

        LinkInformation(final String url, final String text) {
            m_url = url;
            m_text = text;
        }

        String getUrl() {
            return m_url;
        }

        String getText() {
            return m_text;
        }
    }
}
