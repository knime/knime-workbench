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
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.ConfigUtils;
import org.knime.core.util.Version;
import org.knime.core.util.workflowalizer.NodeAndBundleInformation;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
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

    private static final String PARAM_DIRECTORY = "-outDir";
    private static final String FACTORY_LIST = "-factoryListFile";
    private static final String UPDATE_SITE = "-updateSite";

    /**
     * {@inheritDoc}
     * <p>
     * Parameters:
     * <ul>
     * <li>-outDir &lt;path-to-local-write-directory&gt;, this is a required parameter which specifies where the JSON
     * files should be written</li>
     * <li>-factoryListFile &lt;path-to-factory-file&gt;, this is a path to a file containing a single factory class per
     * line. This is used for deprecated nodes.</li>
     * <li>-updateSite &lt;update-site-url&gt;, causes the nodalizer to read both nodes and extensions of the given
     * update site. All other nodes/extensions in the given KNIME installation will be ignored. Node JSON will be
     * written to outDir/nodes and extensions to outDir/extensions.</li>
     * </ul>
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        final Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        File outputDir = null;
        Path factoryList = null;
        URI updateSite = null;
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
                    final String site = params[i + 1];
                    try {
                        updateSite = URI.create(site);
                    } catch (final Exception ex) {
                        System.out.println(
                            "Invalid update site url: " + site + "\n" + UPDATE_SITE + " parameter will be ignored.");
                    }
                }
            }
        }

        if (outputDir == null) {
            System.err.println("No output directory specified. Please specify a valid output directory with "
                + PARAM_DIRECTORY + " flag");
            return IApplication.EXIT_OK;
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        if (!outputDir.isDirectory()) {
            System.err.println("Given path is not a directory: " + outputDir.getAbsolutePath());
            return IApplication.EXIT_OK;
        }

        if (factoryList != null) {
            if (!Files.exists(factoryList)) {
                System.err.println("Given factory list file does not exist: " + outputDir.getAbsolutePath());
                return IApplication.EXIT_OK;
            }
            if (Files.isDirectory(factoryList)) {
                System.err.println("Given factory list file cannot be a directory: " + outputDir.getAbsolutePath());
                return IApplication.EXIT_OK;
            }
        }

        File nodeDir = outputDir;
        File extDir = null;
        Map<String, ExtensionInfo> extensions = null;
        List<String> bundles = null;
        if (updateSite != null) {
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
            bundles = getSymbolicBundleNames(updateSite, agent);
            if (bundles == null) {
                return IApplication.EXIT_OK;
            }
            extensions = parseExtensions(updateSite, agent);
            if (extensions == null) {
                return IApplication.EXIT_OK;
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
        if (extensions != null) {
            for (final ExtensionInfo ext : extensions.values()) {
                if (ext.hasNodes() || !ext.getCategoryPath().isEmpty()) {
                    try {
                        final String fileName = ext.getSymbolicName().replaceAll("\\.", "_");
                        writeFile(extDir, fileName, ext);
                    } catch (final JsonProcessingException | FileNotFoundException ex) {
                        System.out.println("Failed to write extension " + ext.getName() + " " + ext.getSymbolicName());
                        System.out.println(ex.getClass() + ": " + ex.getMessage());
                    }
                } else {
                    System.out.println("Extension " + ext.getName() + " " + ext.getSymbolicName()
                        + " does not exist at any category path and has no nodes. Skipping ...");
                }
            }
        }

        System.out.println("Node (and Extension) JSON generation complete!");
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
                System.out
                    .println("Failed to read node: " + object.getName() + ". " + e.getClass() + ": " + e.getMessage());
                e.printStackTrace();
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
            System.out.println("Failed to read additional factories file: " + factoryListFile);
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
                        System.out.println("Bundle name is missing! " + factory);
                    }
                    if (!b.getBundleVersion().isPresent()) {
                        System.out.println("Bundle version is missing! " + factory);
                    }
                    if (!b.getBundleSymbolicName().isPresent()) {
                        System.out.println("Bundle symbolic name is missing! " + factory);
                    }
                    throw new IllegalArgumentException("Bundle information is missing!");
                }
            } catch (final Throwable e) {
                System.out.println(
                    "Failed to read factory from list: " + factory + ". " + e.getClass() + ": " + e.getMessage());
                e.printStackTrace();
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
        if (extensions != null && bundles != null) {
            // TODO: Check symbolic name and version once we support reading multiple extension versions
            if (extensions.containsKey(nodeAndBundleInfo.getFeatureSymbolicName().orElse(null))) {
                final ExtensionInfo e = extensions.get(nodeAndBundleInfo.getFeatureSymbolicName().get());
                e.setHasNodes(true);
                updateSite = e.getUpdateSite();
                extensionId = e.getId();
            } else if (!nodeAndBundleInfo.getFeatureSymbolicName().isPresent()
                && bundles.contains(nodeAndBundleInfo.getBundleSymbolicName().orElse(null))) {
                System.out.println(fac.getClass() + " does not contain extension information, skipping ...");
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
        nInfo.setBundleInformation(nodeAndBundleInfo, extensionId);

        // Read from node
        final NodeSettings settings = new NodeSettings("");
        fac.saveAdditionalFactorySettings(settings);
        final String factoryName = factoryString + ConfigUtils.contentBasedHashString(settings);
        nInfo.setFactoryName(factoryName);
        nInfo.setTitle(name.trim());
        nInfo.setNodeType(kcn.getType().toString());
        nInfo.setPath(path);
        nInfo.setDeprecated(isDeprecated);
        nInfo.setStreamable(NodeUtil.isStreamable(fac));

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
            System.out.println("Node factory XML not found for " + fac.getClass() + ". Skipping ...");
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
                kcn.getInputType(i).getName(), getColorAsHex(kcn.getInputType(i).getColor()));
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
                    kcn.getOutputType(i).getName(), getColorAsHex(kcn.getOutputType(i).getColor()));
            outports[i - 1] = port;
        }
        nInfo.setInPorts(inports);
        nInfo.setOutPorts(outports);

        // Write to file
        writeFile(directory, categoryPath + "/" + name, nInfo);
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

    private Map<String, ExtensionInfo> parseExtensions(final URI updateSite, final IProvisioningAgent agent) {
        final IMetadataRepositoryManager metadataManager =
            (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
        final boolean uninstallMetadata = !metadataManager.contains(updateSite);
        try {
            IMetadataRepository mr = null;
            try {
                mr = metadataManager.loadRepository(updateSite, new NullProgressMonitor());
            } catch (final Exception ex) {
                System.out.println("Failed to read extensions for " + updateSite.toString() + ". See details below.");
                System.out.println(ex.getClass() + ": " + ex.getMessage());
                ex.printStackTrace();
                return null;
            }
            // TODO: Modify query once we support reading multiple extension versions
            final IQueryResult<IInstallableUnit> ius =
                mr.query(QueryUtil.createLatestQuery(QueryUtil.createIUGroupQuery()), new NullProgressMonitor());
            final SiteInfo siteInfo = parseUpdateSite(mr);
            // TODO: Mapping symbolic name to extension may not be sufficient once we support reading multiple versions
            final Map<String, ExtensionInfo> extensions = new HashMap<>();
            for (final IInstallableUnit iu : ius) {
                // Manual exclusions. Keep this despite node/category path check, because some exclusions (i.e. sources)
                // have a category path but we still don't want to read them
                if (!iu.getId().startsWith("org.eclipse") && !iu.getId().contains(".source.feature.")
                    && !iu.getId().startsWith("org.knime.binary.jre")
                    && !iu.getId().equals("org.knime.targetPlatform.feature.group")
                    && !iu.getId().endsWith(".externals.feature.group")
                    && !QueryUtil.isProduct(iu)) {
                    if (iu.getLicenses().size() > 1) {
                        System.out.println(iu.getId() + " has multiple licenses. Skipping ...");
                        continue;
                    }
                    if (StringUtils.isEmpty(iu.getId())) {
                        System.out.println("Extension has no ID " + iu.toString() + ". Skipping ...");
                        continue;
                    }
                    Version v = null;
                    try {
                        v = new Version(iu.getVersion().toString());
                    } catch (final Exception ex) {
                        // Once we want to extract multiple versions from a single update site, it will be necessary
                        // that the IU has a valid version
                        System.out.println(
                            "Extension, " + iu.getId() + ", has invalid version " + iu.getVersion() + ". Skipping ...");
                        continue;
                    }

                    final ExtensionInfo ext = new ExtensionInfo();
                    ext.setSymbolicName(iu.getId());
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
                    extensions.put(ext.getSymbolicName(), ext);
                }
            }
            return extensions;
        } finally {
            // Without this the update site will be added to the update site in preferences in the KNIME AP instance
            if (uninstallMetadata && metadataManager.contains(updateSite)) {
                metadataManager.removeRepository(updateSite);
            }
        }
    }

    private static List<String> getSymbolicBundleNames(final URI updateSite, final IProvisioningAgent agent) {
        final IArtifactRepositoryManager artifactManager =
            (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
        final boolean uninstallArtifact = !artifactManager.contains(updateSite);
        try {
            IArtifactRepository ar = null;
            try {
                ar = artifactManager.loadRepository(updateSite, new NullProgressMonitor());
            } catch (Exception ex) {
                System.out.println("Failed to read extensions for " + updateSite.toString() + ". See details below.");
                System.out.println(ex.getClass() + ": " + ex.getMessage());
                ex.printStackTrace();
                return null;
            }

            final IQueryResult<IArtifactKey> result = ar.query(
                QueryUtil
                    .createLatestQuery(QueryUtil.createMatchQuery(IArtifactKey.class, "classifier == 'osgi.bundle'")),
                new NullProgressMonitor());
            final List<String> bsn = new ArrayList<>();
            result.forEach(a -> bsn.add(a.getId()));
            return bsn;
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
            || url.contains("://update.knime.com/community-contributions/trusted/");
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
        throws JsonProcessingException, FileNotFoundException {
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
        try (final PrintWriter pw = new PrintWriter(f)) {
            pw.write(json);
        }
    }

}
