/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.repository.nodalizer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.context.ports.ConfigurablePortGroup;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.util.ConfigUtils;
import org.knime.core.util.Version;
import org.knime.core.util.workflowalizer.NodeAndBundleInformation;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.nodalizer.DynamicPortGroup.DynamicPortType;
import org.knime.workbench.repository.nodalizer.ExtensionInfo.LicenseInfo;
import org.knime.workbench.repository.nodalizer.NodeInfo.LinkInformation;
import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;
import org.knime.workbench.repository.util.NodeUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.w3c.dom.Element;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * An application which scans the node repository, and outputs a JSON file containing each encountered node's metadata.
 * Node's not in the repository can also be parsed by passing the node factory class in a file. This file should have
 * one node factory per line, for dynamic nodes the line should contain the 'factory-class-#-factory-settings-xml'.
 * <p>
 * If the application's extension metadata is also desired, the "-updateSite" flag must passed with a valid p2 update
 * site url. This flag will cause both node and extension metadata for that update site to be parsed. Other
 * nodes/extensions will not be read.
 * </p>
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
public class Nodalizer implements IApplication {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Nodalizer.class);

    private static final String PARAM_DIRECTORY = "-outDir";
    private static final String FACTORY_LIST = "-factoryListFile";
    private static final String UPDATE_SITE = "-updateSite";
    private static final String OWNERS = "-owners";
    private static final String DEFAULT_OWNER = "-defaultOwner";
    private static final String FEATURES = "-features";
    private static final String BLACKLIST = "-blacklist";

    /**
     * {@inheritDoc}
     * <p>
     * Parameters:
     * <ul>
     * <li>-outDir &lt;path-to-local-write-directory&gt;, this is a required parameter which specifies where the JSON
     * files should be written</li>
     * <li>-factoryListFile &lt;path-to-factory-file&gt;, this is a path to a file containing a single factory class per
     * line. This is used for deprecated nodes.</li>
     * <li>-updateSite &lt;update-site-url,update-site-url2,...&gt;, a comma delimited list of update sites with <b>no
     * spaces</b>. The nodalizer will read both nodes and extensions on the given update sites. All other
     * nodes/extensions in the given KNIME installation will be ignored. Node JSON will be written to outDir/nodes and
     * extensions to outDir/extensions.</li>
     * <li>-owners &lt;path-to-owners-file&gt;, a file mapping owner to extension symbolic name. Each line should
     * contain a single mapping, with the format symbolicName:owner</li>
     * <li>-defaultOwner &lt;owner-name&gt;, the owner name to assign to extensions which do not have a mapping in the
     * "owners" file</li>
     * <li>-features &lt;feature1,feature2,feature3,...&gt;, an optional list of feature symbolic names with <b>no
     * spaces</b>. If provided the nodalizer will only parse these features; otherwise, the nodalizer attempts to read
     * all nodes and extensions on the given update sites.</li>
     * <li>-blacklist &lt;path-to-blacklist-file&gt;, a file in which each line contains a <b>regex rule</b> for an
     * extension which should be "blacklisted" (not parsed). Also if a blacklist file is provided it may be written to
     * if an extension is found which does not have a category path AND contains no nodes</li>
     * </ul>
     */
    @Override
    @SuppressWarnings("null")
    public Object start(final IApplicationContext context) throws Exception {
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }

        final Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        File outputDir = null;
        Path factoryList = null;
        List<URI> updateSites = null;
        Map<String, String> owners = Collections.emptyMap();
        String defaultOwner = null;
        List<String> features = null;
        Path blacklistFile = null;
        List<String> blacklist = null;
        if (args instanceof String[]) {
            final String[] params = (String[])args;
            for (int i = 0; i < params.length; i++) {
                if (params[i].equalsIgnoreCase(PARAM_DIRECTORY) && (params.length > (i + 1))) {
                    outputDir = new File(params[i + 1]);
                }
                if (params[i].equalsIgnoreCase(FACTORY_LIST) && (params.length > (i + 1))) {
                    factoryList = Paths.get(params[i + 1]);
                }
                if (params[i].equalsIgnoreCase(UPDATE_SITE) && (params.length > (i + 1))) {
                    final String sites[] = params[i + 1].split(",");
                    updateSites = new ArrayList<>(sites.length);
                    for (final String site : sites) {
                        try {
                            updateSites.add(URI.create(site));
                        } catch (final Exception ex) {
                            LOGGER.warn("Invalid update site url: " + site + "\n" + site + " will be ignored.", ex);
                        }
                    }
                }
                if (params[i].equalsIgnoreCase(OWNERS) && (params.length > (i + 1))) {
                    final Path o = Paths.get(params[i + 1]);
                    if (Files.exists(o) && !Files.isDirectory(o)) {
                        owners = new HashMap<>();
                        for (final String line : Files.readAllLines(o)) {
                            final String[] pieces = line.split(":");
                            if (pieces.length == 2) {
                                final String id = pieces[0].trim();
                                final String owner = pieces[1].trim();
                                if (!StringUtils.isEmpty(id) && !StringUtils.isEmpty(owner)) {
                                    owners.put(id, owner);
                                } else {
                                    LOGGER.warn("Extension id AND owner must be non-empty: " + line);
                                }
                            } else {
                                LOGGER.warn("Entry contains too many colons: " + line);
                            }
                        }
                    } else {
                        LOGGER.warn("Invalid owner file: " + o);
                    }
                }
                if (params[i].equalsIgnoreCase(DEFAULT_OWNER) && (params.length > (i + 1))) {
                    defaultOwner = params[i + 1];
                }
                if (params[i].equalsIgnoreCase(FEATURES) && (params.length > (i + 1))) {
                    features = Arrays.asList(params[i + 1].split(","));
                }
                if (params[i].equalsIgnoreCase(BLACKLIST) && (params.length > (i + 1))) {
                    blacklistFile = Paths.get(params[i + 1]);
                    if (Files.exists(blacklistFile) && !Files.isDirectory(blacklistFile)) {
                        blacklist = Files.readAllLines(blacklistFile).stream().filter(l -> !StringUtils.isEmpty(l))
                            .collect(Collectors.toList());
                    } else {
                        LOGGER.warn("Invalid blacklist file: " + blacklistFile.toString());
                    }
                }
            }
        }

        if (outputDir == null) {
            LOGGER.fatal("No output directory specified. Please specify a valid output directory with "
                + PARAM_DIRECTORY + " flag");
            return IApplication.EXIT_OK;
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        if (!outputDir.isDirectory()) {
            LOGGER.fatal("Given path is not a directory: " + outputDir.getAbsolutePath());
            return IApplication.EXIT_OK;
        }

        if (factoryList != null) {
            if (!Files.exists(factoryList)) {
                LOGGER.fatal("Given factory list file does not exist: " + outputDir.getAbsolutePath());
                return IApplication.EXIT_OK;
            }
            if (Files.isDirectory(factoryList)) {
                LOGGER.fatal("Given factory list file cannot be a directory: " + outputDir.getAbsolutePath());
                return IApplication.EXIT_OK;
            }
        }

        File nodeDir = outputDir;
        File extDir = null;
        Map<String, ExtensionInfo> extensions = new HashMap<>();
        List<String> bundles = null;
        if (updateSites != null) {
            nodeDir = new File(outputDir, "nodes");
            extDir = new File(outputDir, "extensions");
            if (!nodeDir.exists()) {
                nodeDir.mkdir();
            }
            if (!extDir.exists()) {
                extDir.mkdir();
            }
            final BundleContext c = FrameworkUtil.getBundle(getClass()).getBundleContext();
            final ServiceReference<IProvisioningAgent> ref = c.getServiceReference(IProvisioningAgent.class);
            final IProvisioningAgent agent = c.getService(ref);
            bundles = new ArrayList<>();
            for (final URI site : updateSites) {
                getSymbolicBundleNames(site, agent, bundles);
            }
            if (bundles.isEmpty()) {
                return IApplication.EXIT_OK;
            }

            for (final URI site : updateSites) {
                parseExtensions(site, agent, owners, defaultOwner, features, blacklist, extensions);
            }
            if (extensions.isEmpty()) {
                return IApplication.EXIT_OK;
            }
            if (features != null) {
                for (final String feature : features) {
                    final String cleanedName = cleanSymbolicName(feature.split("/")[0]);
                    if (!extensions.containsKey(cleanedName)) {
                        LOGGER.warn(cleanedName + " extension not found on given update sites");
                    }
                }
            }
        }

        // unless the user specified this property, we set it to true here
        // (true means no icons etc will be loaded, if it is false, the
        // loading of the repository manager freezes
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        final Root root = RepositoryManager.INSTANCE.getCompleteRoot();

        parseNodesInRoot(root, null, nodeDir, extensions, bundles);
        if (factoryList != null) {
            parseDeprecatedNodeList(factoryList, nodeDir, extensions, bundles);
        }

        // Write extensions
        if (!extensions.isEmpty()) {
            for (final ExtensionInfo ext : extensions.values()) {
                if (ext.hasNodes() || !ext.getCategoryPath().isEmpty()) {
                    try {
                        final String fileName = ext.getSymbolicName().replaceAll("\\.", "_");
                        writeFile(extDir, fileName, ext);
                    } catch (final JsonProcessingException | FileNotFoundException ex) {
                        LOGGER.error("Failed to write extension " + ext.getName() + " " + ext.getSymbolicName(), ex);
                    }
                } else {
                    final String msg = "Extension " + ext.getName() + " " + ext.getSymbolicName() + " does not exist"
                        + " at any category path and has no nodes. Skipping ...";
                    // no blacklist file specified, print warning about skipping nodes
                    if (blacklistFile == null) {
                        LOGGER.warn(msg);
                    } else {
                        // create blacklist file, if file was specified but doesn't actually exist
                        if (!Files.exists(blacklistFile)) {
                            Files.createFile(blacklistFile);
                        }

                        final String blsn = ext.getSymbolicName() + ".feature.group";
                        final String escaped = blsn.replaceAll("\\.", "\\\\.");
                        if (!blacklist.contains(escaped)) {
                            LOGGER.warn(msg); // extension wasn't on blacklist, and is being skipped
                            try {
                                Files.write(blacklistFile, Collections.singletonList(escaped),
                                    StandardOpenOption.APPEND);
                            } catch (final Exception ex) {
                                LOGGER.error("Failed to write extension, " + blsn + ", to blacklist: "
                                    + blacklistFile.toString(), ex);
                            }
                        }
                    }
                }
            }
        }

        LOGGER.info("Node (and Extension) JSON generation complete!");
        return IApplication.EXIT_OK;
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
    }

    // -- Parse nodes --

    private void parseNodesInRoot(final IRepositoryObject object, final List<String> path, final File directory,
        final Map<String, ExtensionInfo> extensions, final List<String> bundles) {
        if (object instanceof NodeTemplate) {
            try {
                final NodeTemplate template = (NodeTemplate)object;
                final NodeFactory<? extends NodeModel> fac = template.createFactoryInstance();
                final NodeAndBundleInformation nodeAndBundleInfo = NodeAndBundleInformationPersistor.create(fac);
                parseNodeAndPrint(fac, fac.getClass().getName(), path, template.getCategoryPath(), template.getName(),
                    nodeAndBundleInfo, fac.isDeprecated(), directory, extensions, bundles);
            } catch (final Throwable e) {
                LOGGER.error("Failed to read node: " + object.getName() + ".", e);
            }
        } else if (object instanceof Root) {
            for (final IRepositoryObject child : ((Root)object).getChildren()) {
                parseNodesInRoot(child, new ArrayList<>(), directory, extensions, bundles);
            }
        } else if (object instanceof Category) {
            for (final IRepositoryObject child : ((Category)object).getChildren()) {
                final Category c = (Category)object;
                final List<String> p = new ArrayList<>(path);
                p.add(c.getName());
                parseNodesInRoot(child, p, directory, extensions, bundles);
            }
        } else {
            return;
        }
    }

    private static void parseDeprecatedNodeList(final Path factoryListFile, final File directory,
        final Map<String, ExtensionInfo> extensions, final List<String> bundles) {
        if (factoryListFile == null) {
            return;
        }
        List<String> factories = null;
        try {
            factories = Files.readAllLines(factoryListFile);
        } catch (final Exception e) {
            LOGGER.error("Failed to read additional factories file: " + factoryListFile, e);
            return;
        }

        for (final String factory : factories) {
            try {
                final String[] parts = factory.split("#");
                final NodeFactory<? extends NodeModel> fac = RepositoryManager.INSTANCE.loadNodeFactory(parts[0]);

                // Dynamic nodes require additional information to load the factory
                if ((fac instanceof DynamicNodeFactory) && (parts.length > 1)) {
                    final String s = parts[1];
                    final NodeSettingsRO ns =
                        NodeSettings.loadFromXML(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
                    fac.loadAdditionalFactorySettings(ns);
                }

                final NodeAndBundleInformationPersistor b = NodeAndBundleInformationPersistor.create(fac);
                final String categoryPath = "/uncategorized";
                final List<String> path = Collections.singletonList("Uncategorized");

                fac.init(); // Some factories must be initialized or name/description throws NPE
                if (b.getBundleName().isPresent() && b.getBundleVersion().isPresent()
                    && b.getBundleSymbolicName().isPresent()) {
                    // always pass true for isDeprecated, even though the factory may not say it is deprecated
                    // pass the factory name in the file, not the name of the loaded class - due to factory class
                    // mapping these may not match
                    parseNodeAndPrint(fac, parts[0], path, categoryPath, fac.getNodeName(), b, true, directory,
                        extensions, bundles);
                } else {
                    if (!b.getBundleName().isPresent()) {
                        LOGGER.warn("Bundle name is missing! " + factory);
                    }
                    if (!b.getBundleVersion().isPresent()) {
                        LOGGER.warn("Bundle version is missing! " + factory);
                    }
                    if (!b.getBundleSymbolicName().isPresent()) {
                        LOGGER.warn("Bundle symbolic name is missing! " + factory);
                    }
                    throw new IllegalArgumentException("Bundle information is missing!");
                }
            } catch (final Throwable e) {
                LOGGER.warn("Failed to read factory from list: " + factory + ". ", e);
            }
        }
    }

    private static void parseNodeAndPrint(final NodeFactory<?> fac, final String factoryString, final List<String> path,
        final String categoryPath, final String name, final NodeAndBundleInformation nodeAndBundleInfo,
        final boolean isDeprecated, final File directory, final Map<String, ExtensionInfo> extensions,
        final List<String> bundles) throws Exception {
        // Read update site info
        // Do this early to prevent instantiating unnecessary nodes.
        String extensionId = null;
        SiteInfo updateSite = null;
        String owner = null;
        NodeAndBundleInformation nabi = nodeAndBundleInfo;
        if (extensions != null && bundles != null) {
            // TODO: Check symbolic name and version once we support reading multiple extension versions
            String cleanedSymbolicName = cleanSymbolicName(nabi.getFeatureSymbolicName().orElse(null));
            if (extensions.containsKey(cleanedSymbolicName)) {
                // HACK: See https://knime-com.atlassian.net/browse/AP-13547 for details
                ExtensionInfo e;
                if (cleanedSymbolicName.equals("org.knime.features.testing.core")
                    && extensions.containsKey("org.knime.features.testing.application")) {
                    e = extensions.get("org.knime.features.testing.application");
                } else {
                    e = extensions.get(cleanedSymbolicName);
                }
                e.setHasNodes(true);
                updateSite = e.getUpdateSite();
                extensionId = e.getId();
                owner = e.getOwner();
                nabi = new NodeAndBundleInformation(nodeAndBundleInfo.getFactoryClass(),
                    nodeAndBundleInfo.getBundleSymbolicName(), nodeAndBundleInfo.getBundleName(),
                    nodeAndBundleInfo.getBundleVendor(), nodeAndBundleInfo.getNodeName(),
                    nodeAndBundleInfo.getBundleVersion(), Optional.of(e.getSymbolicName()),
                    Optional.ofNullable(e.getName()), Optional.ofNullable(e.getVendor()),
                    Optional.ofNullable(e.getVersion()));
            } else if (!nabi.getFeatureSymbolicName().isPresent()
                && bundles.contains(nabi.getBundleSymbolicName().orElse(null))) {
                LOGGER.warn(fac.getClass() + " does not contain extension information, skipping ...");
                return;
            } else {
                // Node doesn't belong to this update site, so skip. With any KNIME installation there will be
                // around 500 nodes installed. So it is not worth printing all the nodes that don't belong
                // to the update site being read.
                return;
            }
        }

        @SuppressWarnings("unchecked")
        final org.knime.core.node.Node kcn = new org.knime.core.node.Node((NodeFactory<NodeModel>)fac);
        final NodeInfo nInfo = new NodeInfo();
        nInfo.setAdditionalSiteInformation(updateSite);
        nInfo.setBundleInformation(nabi, extensionId);
        nInfo.setOwner(owner);

        // Read from node
        final NodeSettings settings = new NodeSettings("");
        fac.saveAdditionalFactorySettings(settings);
        final String factoryName = factoryString + ConfigUtils.contentBasedHashString(settings);
        nInfo.setFactoryName(factoryName);
        nInfo.setTitle(name.trim());
        nInfo.setNodeType(kcn.getType().toString());
        nInfo.setPath(path);
        nInfo.setDeprecated(isDeprecated);
        nInfo.setStreamable(NodeUtil.isStreamable(kcn));

        // Read icon
        URL imageURL = fac.getIcon();
        if (imageURL == null) {
            imageURL = NodeFactory.class.getResource("defaulticon.png");
        }
        final String mimeType = URLConnection.guessContentTypeFromName(imageURL.getFile());
        byte[] imageBytes = null;
        try (InputStream s = imageURL.openStream()) {
            imageBytes = IOUtils.toByteArray(s);
        }
        final String iconBase64 = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        nInfo.setIcon(iconBase64);

        // Parse HTML, and read fields
        final Element nodeXML = fac.getXMLDescription();
        Document nodeHTML = null;
        if (nodeXML == null) {
            LOGGER.warn("Node factory XML not found for " + fac.getClass() + ". Skipping ...");
            return;
        }
        final String s = NodeFactoryHTMLCreator.instance.readFullDescription(nodeXML);
        nodeHTML = Jsoup.parse(s);
        String descriptHTML = "";
        org.jsoup.nodes.Node n = nodeHTML.getElementsByTag("p").first();
        while (n != null) {
            if (n instanceof org.jsoup.nodes.Element) {
                final org.jsoup.nodes.Element e = (org.jsoup.nodes.Element)n;
                if (e.tagName().equalsIgnoreCase("h2")) {
                    n = null;
                } else if (e.hasText()) {
                    descriptHTML += e.outerHtml();
                    n = n.nextSibling();
                } else if (e.tagName().equalsIgnoreCase("br")) {
                    descriptHTML += e.outerHtml();
                    n = n.nextSibling();
                } else {
                    n = n.nextSibling();
                }
            } else if (n instanceof TextNode) {
                final TextNode tn = (TextNode)n;
                descriptHTML += tn.getWholeText();
                n = n.nextSibling();
            } else {
                n = n.nextSibling();
            }
        }
        nInfo.setDescription(descriptHTML);
        parseHTML(nodeHTML, nInfo, kcn.getInteractiveViewName());

        // Read PortInfo
        final PortInfo[] inports = new PortInfo[kcn.getNrInPorts() - 1];
        final PortInfo[] outports = new PortInfo[kcn.getNrOutPorts() - 1];
        for (int i = 1; i < kcn.getNrInPorts(); i++) {
            String portDescriptHTML = fac.getInportDescription(i - 1);
            if (!nodeHTML.getElementsMatchingOwnText("Input Ports").isEmpty()) {
                final org.jsoup.nodes.Element sibling =
                    nodeHTML.getElementsMatchingOwnText("Input Ports").first().nextElementSibling();
                if (sibling != null) {
                    final Elements matches = sibling.getElementsByAttributeValue("class", "dt");
                    for (final org.jsoup.nodes.Element match : matches) {
                        if (match.ownText().equals("" + (i - 1))) {
                            portDescriptHTML = cleanHTML(match.nextElementSibling());
                            break;
                        }
                    }
                }
            }
            final PortInfo port = new PortInfo(kcn.getInportName(i), portDescriptHTML, kcn.getInputType(i).isOptional(),
                kcn.getInputType(i).getName(), getColorAsHex(kcn.getInputType(i).getColor()),
                kcn.getInputType(i).getPortObjectClass().getCanonicalName());
            inports[i - 1] = port;
        }
        for (int i = 1; i < kcn.getNrOutPorts(); i++) {
            String portDescriptHTML = fac.getOutportDescription(i - 1);
            if (!nodeHTML.getElementsMatchingOwnText("Output Ports").isEmpty()) {
                final org.jsoup.nodes.Element sibling =
                    nodeHTML.getElementsMatchingOwnText("Output Ports").first().nextElementSibling();
                if (sibling != null) {
                    final Elements matches = sibling.getElementsByAttributeValue("class", "dt");
                    for (final org.jsoup.nodes.Element match : matches) {
                        if (match.ownText().equals("" + (i - 1))) {
                            portDescriptHTML = cleanHTML(match.nextElementSibling());
                            break;
                        }
                    }
                }
            }
            final PortInfo port =
                new PortInfo(kcn.getOutportName(i), portDescriptHTML, kcn.getOutputType(i).isOptional(),
                    kcn.getOutputType(i).getName(), getColorAsHex(kcn.getOutputType(i).getColor()),
                    kcn.getOutputType(i).getPortObjectClass().getCanonicalName());
            outports[i - 1] = port;
        }
        nInfo.setInPorts(inports);
        nInfo.setOutPorts(outports);

        if (kcn.getCopyOfCreationConfig().isPresent() && kcn.getCopyOfCreationConfig().get().getPortConfig().isPresent()
            && fac instanceof ConfigurableNodeFactory) {
            final ModifiablePortsConfiguration portConfigs = kcn.getCopyOfCreationConfig().get().getPortConfig().get();
            final List<DynamicPortGroup> dynInports = parseDynamicPorts(nodeXML, "dynInPort", nodeHTML,
                "Dynamic Input Ports", portConfigs, fac.getClass().getCanonicalName());
            final List<DynamicPortGroup> dynOutports = parseDynamicPorts(nodeXML, "dynOutPort", nodeHTML,
                "Dynamic Output Ports", portConfigs, fac.getClass().getCanonicalName());
            nInfo.setDynInPorts(dynInports);
            nInfo.setDynOutPorts(dynOutports);
        }

        // Write to file
        writeFile(directory, categoryPath + "/" + name, nInfo);
    }

    private static List<DynamicPortGroup> parseDynamicPorts(final Element nodeXML, final String xmlTag,
        final Document nodeHTML, final String sectionName, final ModifiablePortsConfiguration portConfigs,
        final String nodeFactoryName) {
        final int dynamicPortCount = nodeXML.getElementsByTagName(xmlTag).getLength();
        final org.jsoup.nodes.Element dynamicPortSection = nodeHTML.getElementsMatchingOwnText(sectionName).first();
        if (dynamicPortCount > 0 && dynamicPortSection != null) {
            final List<DynamicPortGroup> dynamicPorts = new ArrayList<>(dynamicPortCount);
            for (final org.jsoup.nodes.Element sibling : dynamicPortSection.siblingElements()) {
                for (final org.jsoup.nodes.Element group : sibling.getElementsByClass("dt")) {
                    final org.jsoup.nodes.Element description = group.nextElementSibling();
                    final String groupName = group.ownText();
                    try {
                        final PortGroupConfiguration groupConfig = portConfigs.getGroup(groupName);
                        if (description != null && groupConfig instanceof ConfigurablePortGroup) {
                            final ConfigurablePortGroup configurableGroupConfig = (ConfigurablePortGroup)groupConfig;
                            final PortType[] supportedTypes = configurableGroupConfig.getSupportedPortTypes();
                            final DynamicPortType[] types = new DynamicPortType[supportedTypes.length];
                            for (int i = 0; i < types.length; i++) {
                                final PortType t = supportedTypes[i];
                                types[i] = new DynamicPortType(t.getPortObjectClass().getCanonicalName(), t.getName(),
                                    getColorAsHex(t.getColor()));
                            }
                            final DynamicPortGroup port =
                                new DynamicPortGroup(groupName, cleanHTML(description), types);
                            dynamicPorts.add(port);
                        }
                    } catch (final NoSuchElementException exception) {
                        LOGGER.warn("No dynamic port group, " + groupName + ", for " + nodeFactoryName);
                    }
                }
            }
            if (dynamicPortCount != dynamicPorts.size()) {
                LOGGER.warn("The number of dynamic ports parsed does not match the number listed in the XML, "
                    + dynamicPorts.size() + " and " + dynamicPortCount + " respectively, for " + nodeFactoryName);
            }
            return dynamicPorts;
        }
        return Collections.emptyList();
    }

    private static void parseHTML(final Document nodeHTML, final NodeInfo nodeInfo, final String interactiveViewName) {
        final List<DialogOptionGroup> dialogOptions = new ArrayList<>();
        final List<NamedField> views = new ArrayList<>();
        NamedField interactiveView = null;
        LinkInformation[] moreInfoLinks = null;

        org.jsoup.nodes.Element doH2 = null;
        for (final org.jsoup.nodes.Element element : nodeHTML.getElementsMatchingOwnText("Dialog Options")) {
            if (element.tagName().equalsIgnoreCase("h2") && element.text().equals("Dialog Options")) {
                doH2 = element;
                break;
            }
        }
        if (doH2 != null) {
            org.jsoup.nodes.Element sibling = doH2.nextElementSibling();
            if (sibling.tagName().equalsIgnoreCase("dl")) {
                final List<NamedField> fields = new ArrayList<>();
                parseDLTag(sibling, fields, true);
                dialogOptions.add(new DialogOptionGroup(null, null, fields));
            }
            if (sibling.tagName().equalsIgnoreCase("div")) {
                while (sibling.attr("class").equalsIgnoreCase("group")) {
                    String nameHTML = null;
                    String descriptHTML = null;
                    final List<NamedField> fields = new ArrayList<>();
                    for (final org.jsoup.nodes.Element c : sibling.children()) {
                        if (c.attr("class").equals("groupname")) {
                            nameHTML = cleanHTML(c);
                        }
                        if (c.attr("class").equals("group-description")) {
                            descriptHTML = cleanHTML(c);
                        }
                        if (c.tagName().equals("dl")) {
                            parseDLTag(c, fields, true);
                        }
                    }
                    dialogOptions.add(new DialogOptionGroup(nameHTML, descriptHTML, fields));
                    sibling = sibling.nextElementSibling();
                }
            }
        }

        org.jsoup.nodes.Element viewH2 = null;
        for (final org.jsoup.nodes.Element element : nodeHTML.getElementsMatchingOwnText("Views")) {
            if (element.tagName().equalsIgnoreCase("h2") && element.text().equals("Views")) {
                viewH2 = element;
                break;
            }
        }
        if (viewH2 != null) {
            final org.jsoup.nodes.Element sib = viewH2.nextElementSibling();
            if (sib.tagName().equalsIgnoreCase("dl")) {
                parseDLTag(sib, views, false);
            }
        }

        org.jsoup.nodes.Element interactiveViewH2 = null;
        for (final org.jsoup.nodes.Element element : nodeHTML.getElementsMatchingOwnText("Interactive View:")) {
            if (element.tagName().equalsIgnoreCase("h2") && element.text().startsWith("Interactive View:")) {
                interactiveViewH2 = element;
                break;
            }
        }
        if (interactiveViewH2 != null) {
            final org.jsoup.nodes.Element sib = interactiveViewH2.nextElementSibling();
            if ((sib != null) && sib.tagName().equalsIgnoreCase("div")) {
                interactiveView = new NamedField(interactiveViewName, cleanHTML(sib));
            }
        }

        org.jsoup.nodes.Element moreInfoH2 = null;
        for (final org.jsoup.nodes.Element element : nodeHTML.getElementsMatchingOwnText("More Information")) {
            if (element.tagName().equalsIgnoreCase("h2") && element.text().equals("More Information")) {
                moreInfoH2 = element;
                break;
            }
        }
        if (moreInfoH2 != null) {
            org.jsoup.nodes.Element sibling = moreInfoH2.nextElementSibling();
            while ((sibling != null) && !sibling.tagName().equalsIgnoreCase("dd")) {
                sibling = sibling.nextElementSibling();
            }
            if ((sibling != null) && !sibling.getElementsByTag("ul").isEmpty()) {
                final Elements links = sibling.getElementsByTag("a");
                moreInfoLinks = new LinkInformation[links.size()];
                for (int i = 0; i < moreInfoLinks.length; i++) {
                    moreInfoLinks[i] = new LinkInformation(links.get(i).attr("href"), links.get(i).text());
                }
            }
        }
        nodeInfo.setDialog(dialogOptions);
        nodeInfo.setViews(views);
        nodeInfo.setInteractiveView(interactiveView);
        nodeInfo.setLinks(moreInfoLinks);
    }

    // -- Parse Extension --

    private Map<String, ExtensionInfo> parseExtensions(final URI updateSite, final IProvisioningAgent agent,
        final Map<String, String> owners, final String defaultOwner, final List<String> features,
        final List<String> blacklist, final Map<String, ExtensionInfo> extensions) {
        final IMetadataRepositoryManager metadataManager =
            (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
        final boolean uninstallMetadata = !metadataManager.contains(updateSite);
        try {
            IMetadataRepository mr = null;
            try {
                mr = metadataManager.loadRepository(updateSite, new NullProgressMonitor());
            } catch (final Exception ex) {
                LOGGER.error("Failed to read extensions for " + updateSite.toString(), ex);
                return null;
            }
            // TODO: Modify query once we support reading multiple extension versions
            final IQueryResult<IInstallableUnit> ius =
                mr.query(QueryUtil.createLatestQuery(QueryUtil.createIUGroupQuery()), new NullProgressMonitor());
            final SiteInfo siteInfo = parseUpdateSite(mr);
            // TODO: Mapping symbolic name to extension may not be sufficient once we support reading multiple versions
            for (final IInstallableUnit iu : ius) {
                if (features != null && !features.contains(iu.getId() + "/" + iu.getVersion().toString())) {
                    continue;
                }

                // Check blacklist if present. Items in the blacklist should be regex patterns with proper character
                // escaping
                if (blacklist != null
                    && blacklist.stream().anyMatch(blackListItem -> iu.getId().matches(blackListItem))) {
                    continue;
                }

                if (iu.getLicenses().size() > 1) {
                    LOGGER.warn(iu.getId() + " has multiple licenses. Skipping ...");
                    continue;
                }

                if (StringUtils.isEmpty(iu.getId())) {
                    LOGGER.warn("Extension has no ID " + iu.toString() + ". Skipping ...");
                    continue;
                }

                Version v = null;
                try {
                    v = new Version(iu.getVersion().toString());
                } catch (final Exception ex) {
                    // Once we want to extract multiple versions from a single update site, it will be necessary
                    // that the IU has a valid version
                    LOGGER.warn(
                        "Extension, " + iu.getId() + ", has invalid version " + iu.getVersion() + ". Skipping ...");
                    continue;
                }

                final ExtensionInfo ext = new ExtensionInfo();
                ext.setSymbolicName(cleanSymbolicName(iu.getId()));
                ext.setVersion(v);
                ext.setName(iu.getProperty("org.eclipse.equinox.p2.name"));
                ext.setDescription(iu.getProperty("org.eclipse.equinox.p2.description"));
                ext.setDescriptionUrl(iu.getProperty("org.eclipse.equinox.p2.description.url"));
                ext.setVendor(iu.getProperty("org.eclipse.equinox.p2.provider"));
                ext.setUpdateSite(siteInfo);

                if (iu.getCopyright() != null) {
                    ext.setCopyright(iu.getCopyright().getBody());
                }

                if (!iu.getLicenses().isEmpty()) {
                    // Take just the first license
                    final ILicense license = iu.getLicenses().iterator().next();
                    final LicenseInfo licenseInfo = new LicenseInfo();
                    final String licenseBody = license.getBody();
                    licenseInfo.setName(licenseBody.split("\\r?\\n")[0]);
                    licenseInfo.setText(licenseBody);
                    licenseInfo.setUrl(license.getLocation() != null ? license.getLocation().toString() : null);
                    ext.setLicense(licenseInfo);
                }

                final List<String> categories = new ArrayList<>();
                getCategoryPath(mr, iu, categories);
                ext.setCategoryPath(categories);
                ext.setOwner(owners.getOrDefault(ext.getSymbolicName(), defaultOwner));
                extensions.put(ext.getSymbolicName(), ext);
            }
            return extensions;
        } finally {
            // Without this the update site will be added to the update site in preferences in the KNIME AP instance
            if (uninstallMetadata && metadataManager.contains(updateSite)) {
                metadataManager.removeRepository(updateSite);
            }
        }
    }

    private static void getSymbolicBundleNames(final URI updateSite, final IProvisioningAgent agent,
        final List<String> bundles) {
        final IArtifactRepositoryManager artifactManager =
            (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
        final boolean uninstallArtifact = !artifactManager.contains(updateSite);
        try {
            IArtifactRepository ar = null;
            try {
                ar = artifactManager.loadRepository(updateSite, new NullProgressMonitor());
            } catch (Exception ex) {
                LOGGER.error("Failed to read bundles for " + updateSite.toString(), ex);
                return;
            }

            final IQueryResult<IArtifactKey> result = ar.query(
                QueryUtil
                    .createLatestQuery(QueryUtil.createMatchQuery(IArtifactKey.class, "classifier == 'osgi.bundle'")),
                new NullProgressMonitor());
            result.forEach(a -> bundles.add(a.getId()));
        } finally {
            if (uninstallArtifact && artifactManager.contains(updateSite)) {
                artifactManager.removeRepository(updateSite);
            }
        }
    }

    private void getCategoryPath(final IMetadataRepository repo, final IInstallableUnit iu,
        final List<String> category) {
        // TODO: Modify query once we support reading multiple extension versions
        final IQueryResult<IInstallableUnit> res = repo.query(
            QueryUtil.createLatestQuery(QueryUtil.createMatchQuery("requirements.exists(rc | $0 ~= rc)", iu)),
            new NullProgressMonitor());

        if (res.isEmpty()) {
            return;
        }

        for (final IInstallableUnit r : res) {
            if (QueryUtil.isCategory(r)) {
                category.add(0, r.getProperty("org.eclipse.equinox.p2.name"));
                getCategoryPath(repo, r, category);
                return;
            }
        }
    }

    // -- Parse Update Site --

    private static SiteInfo parseUpdateSite(final IMetadataRepository repo) {
        final String name = repo.getName();
        final String url = repo.getLocation().toString();
        final boolean enabledByDefault = siteEnabledByDefault(url);
        final boolean isTrusted = isTrusted(url);
        return new SiteInfo(url, enabledByDefault, isTrusted, name);
    }

    private static boolean siteEnabledByDefault(final String url) {
        return url.contains("://update.knime.com/analytics-platform")
            || url.contains("://update.knime.com/community-contributions/trusted/")
            || url.contains("://update.knime.com/partner/");
    }

    private static boolean isTrusted(final String url) {
        return siteEnabledByDefault(url);
    }

    // -- Helper methods --

    private static void parseDLTag(final org.jsoup.nodes.Element dl, final List<NamedField> fields,
        final boolean checkOptional) {
        String keyHTML = "";
        boolean optional = false;
        for (final org.jsoup.nodes.Element child : dl.children()) {
            if (child.tagName().equals("dd")) {
                NamedField f;
                if (checkOptional) {
                    f = new NamedField(keyHTML, cleanHTML(child), optional);
                } else {
                    f = new NamedField(keyHTML, cleanHTML(child));
                }
                fields.add(f);
            }
            if (child.tagName().equals("dt")) {
                keyHTML = cleanHTML(child);
                if (checkOptional) {
                    optional = false;
                    if (!StringUtils.isEmpty(keyHTML)) {
                        // strip out all HTML tags from field name
                        keyHTML = child.ownText();
                        for (final org.jsoup.nodes.Element e : child.children()) {
                            if (e.tagName().equalsIgnoreCase("span") && e.text().equalsIgnoreCase("(optional)")) {
                                optional = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static String cleanHTML(final org.jsoup.nodes.Element e) {
        if (e.children().isEmpty()) {
            return e.html();
        }
        for (final org.jsoup.nodes.Element child : e.children()) {
            if (!hasText(child) && !child.tagName().equalsIgnoreCase("br")) {
                child.remove();
            }
        }
        return e.html();
    }

    private static boolean hasText(final org.jsoup.nodes.Element e) {
        if (e.children().isEmpty()) {
            return e.hasText();
        }
        boolean hasText = false;
        for (final org.jsoup.nodes.Element child : e.children()) {
            hasText |= hasText(child);
        }
        return hasText;
    }

    private static String getColorAsHex(final int color) {
        final StringBuffer buf = new StringBuffer();
        buf.append(Integer.toHexString(color));
        while (buf.length() < 6) {
            buf.insert(0, "0");
        }
        String opacity = "";
        while (buf.length() > 6) {
            opacity += buf.charAt(0);
            buf.deleteCharAt(0);
        }
        if (!opacity.isEmpty() && !opacity.equalsIgnoreCase("ff")) {
            throw new IllegalArgumentException("Opacity is not supported for node port colors");
        }
        return buf.toString();
    }

    private static void writeFile(final File outputDir, final String baseFileName, final Object pojoToWrite)
        throws JsonProcessingException, FileNotFoundException, UnsupportedEncodingException {
        final ObjectMapper map = new ObjectMapper();
        map.setSerializationInclusion(Include.NON_ABSENT);
        map.enable(SerializationFeature.INDENT_OUTPUT);
        final String json = map.writeValueAsString(pojoToWrite);
        String fileName = baseFileName.replaceAll("\\W+", "_");
        File f = new File(outputDir, fileName + ".json");
        int count = 2;
        while (f.exists()) {
            f = new File(outputDir, fileName + count + ".json");
            count++;
        }
        try (final PrintWriter pw = new PrintWriter(f, StandardCharsets.UTF_8.displayName())) {
            pw.write(json);
        }
    }

    private static String cleanSymbolicName(final String symbolicName) {
        if (symbolicName != null && symbolicName.endsWith(".feature.group")) {
            return symbolicName.substring(0, symbolicName.length() - 14);
        }
        return symbolicName;
    }
}
