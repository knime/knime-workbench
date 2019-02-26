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
import java.io.InputStream;
import java.io.PrintWriter;
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
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
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
import org.knime.core.util.workflowalizer.NodeAndBundleInformation;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.nodalizer.NodeInfo.LinkInformation;
import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;
import org.knime.workbench.repository.util.NodeUtil;
import org.w3c.dom.Element;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * An application which scans the node repository, and outputs a JSON file containing each encountered node's metadata.
 * Node's not in the repository can also be parsed by passing the node factory class in a file. This file should have
 * one node factory per line, for dynamic nodes the line should contain the 'factory-class-#-factory-settings-xml'.
 * Update site information can also be included in the output JSON, via the "-manualUpdateSites" flag. This should be
 * used by passing the update site URL followed by a file containing the ids for the nodes which are part of that update
 * site.
 *
 * @author Alison Walter, KNIME GmbH, Konstanz, Germany
 */
public class Nodalizer implements IApplication {
    private static final String PARAM_DIRECTORY = "-outDir";
    private static final String FACTORY_LIST = "-factoryListFile";
    private static final String MANUAL_UPDATE_SITES = "-manualUpdateSites";

    /**
     * {@inheritDoc}
     * <p>
     * Parameters:
     * <ul>
     * <li>-outDir &lt;path-to-local-write-directory&gt;, this is a required parameter which specifies where the JSON
     * files should be written</li>
     * <li>-factoryListFile &lt;path-to-factory-file&gt;, this is a path to a file containing a single factory class per
     * line. This is used for deprecated nodes.</li>
     * <li>-manualUpdateSites &lt;update-site-1-url&gt; &lt;update-site-1-node-list&gt; &lt;update-site-2-url&gt;
     * &lt;update-site-2-node-list&gt; ... this is used to pass update site information to be included in the output
     * node JSON. The first parameter is the update site's url, and the second is a list of node ids (factory class +
     * factory settings hash see {@link ConfigUtils#contentBasedHashString(org.knime.core.node.config.base.ConfigBase)})
     * contained in that update site</li>
     * </ul>
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        final Object args =
                context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        File outputDir = null;
        Path factoryList = null;
        Map<String, List<String>> manualUpdateSites = null;
        if (args instanceof String[]) {
            final String[] params = (String[]) args;
            for (int i = 0; i < params.length; i++) {
                if (params[i].equalsIgnoreCase(PARAM_DIRECTORY) && (params.length > (i + 1))) {
                    outputDir = new File(params[i+1]);
                }
                if (params[i].equalsIgnoreCase(FACTORY_LIST) && (params.length > (i + 1))) {
                    factoryList = Paths.get(params[i + 1]);
                }
                if (params[i].equalsIgnoreCase(MANUAL_UPDATE_SITES) && (params.length > (i + 2))) {
                    manualUpdateSites = new HashMap<>();
                    int index = i + 1;
                    while (params.length > (index + 1) && (params[index].charAt(0) != '-')) {
                        manualUpdateSites.put(params[index], Files.readAllLines(Paths.get(params[index + 1])));
                        index += 2;
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

        // unless the user specified this property, we set it to true here
        // (true means no icons etc will be loaded, if it is false, the
        // loading of the repository manager freezes
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        final Root root = RepositoryManager.INSTANCE.getCompleteRoot();

        pasreNodesInRoot(root, null, outputDir, manualUpdateSites);
        if (factoryList != null) {
            parseDeprecatedNodeList(factoryList, outputDir, manualUpdateSites);
        }

        System.out.println("Node description generation successfully finished");
        return IApplication.EXIT_OK;
    }

    /** {@inheritDoc} */
    @Override
    public void stop() {
    }

    // -- Helper methods --

    private void pasreNodesInRoot(final IRepositoryObject object, final List<String> path, final File directory, final Map<String, List<String>> manualUpdateSites) {
        if (object instanceof NodeTemplate) {
            try {
                final NodeTemplate template = (NodeTemplate)object;
                final NodeFactory<? extends NodeModel> fac = template.createFactoryInstance();
                final NodeAndBundleInformation nodeAndBundleInfo = NodeAndBundleInformationPersistor.create(fac);
                parseNodeAndPrint(fac, fac.getClass().getName(), path, template.getCategoryPath(), template.getName(),
                    nodeAndBundleInfo, fac.isDeprecated(), directory, manualUpdateSites);
            } catch (final Exception e) {
                System.out.println("Failed to read node: " + object.getName());
                e.printStackTrace();
            }
        } else if (object instanceof Root) {
            for (final IRepositoryObject child : ((Root)object).getChildren()) {
                pasreNodesInRoot(child, new ArrayList<>(), directory, manualUpdateSites);
            }
        } else if (object instanceof Category) {
            for (final IRepositoryObject child : ((Category)object).getChildren()) {
                final Category c = (Category)object;
                final List<String> p = new ArrayList<>(path);
                p.add(c.getName());
                pasreNodesInRoot(child, p, directory, manualUpdateSites);
            }
        } else {
            return;
        }
    }

    private static void parseDeprecatedNodeList(final Path factoryListFile, final File directory, final Map<String, List<String>> manualUpdateSites) {
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
                if (b.getBundleName().isPresent() && b.getBundleVersion().isPresent()) {
                    // always pass true for isDeprecated, even though the factory may not say it is deprecated
                    // pass the factory name in the file, not the name of the loaded class - due to factory class mapping these may not match
                    parseNodeAndPrint(fac, parts[0], path, categoryPath, fac.getNodeName(), b, true, directory, manualUpdateSites);
                } else {
                    if (!b.getBundleName().isPresent()) {
                        System.out.println("Bundle name is missing! " + factory);
                    }
                    if (!b.getBundleVersion().isPresent()) {
                        System.out.println("Bundle version is missing! " + factory);
                    }
                    throw new IllegalArgumentException("Bundle information is missing!");
                }
            } catch (final Exception e) {
                System.out.println("Failed to read factory: " + factory);
                e.printStackTrace();
            }
        }
    }

    private static void parseNodeAndPrint(final NodeFactory<?> fac, final String factoryString, final List<String> path, final String categoryPath,
        final String name, final NodeAndBundleInformation nodeAndBundleInfo, final boolean isDeprecated,
        final File directory, final Map<String, List<String>> manualUpdateSites) throws Exception {
        @SuppressWarnings("unchecked")
        final org.knime.core.node.Node kcn = new org.knime.core.node.Node((NodeFactory<NodeModel>)fac);
        final NodeInfo nInfo = new NodeInfo();

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

        // Read extension info
        // HACK: Manually load update site
        if (manualUpdateSites != null) {
            final String url = getUpdateSiteUrl(factoryName, manualUpdateSites);
            final boolean enabledByDefault = siteEnabledByDefault(url);
            final String siteName = getUpdateSiteName(url);
            nInfo.setAdditionalSiteInformation(url, enabledByDefault, siteName);
        }
        nInfo.setBundleInformation(nodeAndBundleInfo);

        // Parse HTML, and read fields
        final Element nodeXML = fac.getXMLDescription();
        Document nodeHTML = null;
        if (nodeXML == null) {
            System.out.println("Node factory XML not found! " + fac.getClass().toString());
        } else {
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
        }

        // Read PortInfo
        final PortInfo[] inports = new PortInfo[kcn.getNrInPorts() - 1];
        final PortInfo[] outports = new PortInfo[kcn.getNrOutPorts() - 1];
        for (int i = 1; i < kcn.getNrInPorts(); i++) {
            String portDescriptHTML = fac.getInportDescription(i - 1);
            if (nodeHTML != null && !nodeHTML.getElementsMatchingOwnText("Input Ports").isEmpty()) {
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
            if (nodeHTML != null && !nodeHTML.getElementsMatchingOwnText("Output Ports").isEmpty()) {
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
        final ObjectMapper map = new ObjectMapper();
        map.setSerializationInclusion(Include.NON_ABSENT);
        map.enable(SerializationFeature.INDENT_OUTPUT);
        final String json = map.writeValueAsString(nInfo);
        String fileName = categoryPath + "/" + name;
        fileName = fileName.replaceAll("\\W+", "_");
        File f = new File(directory, fileName + ".json");
        int count = 2;
        while (f.exists()) {
            f = new File(directory, fileName + count + ".json");
            count++;
        }
        try (final PrintWriter pw = new PrintWriter(f)) {
            pw.write(json);
        }
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
                parseDLTag(sibling, fields);
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
                            parseDLTag(c, fields);
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
                parseDLTag(sib, views);
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

    private static void parseDLTag(final org.jsoup.nodes.Element dl, final List<NamedField> fields) {
        String keyHTML = "";
        for (final org.jsoup.nodes.Element child : dl.children()) {
            if (child.tagName().equals("dd")) {
                final NamedField f = new NamedField(keyHTML, cleanHTML(child));
                fields.add(f);
            }
            if (child.tagName().equals("dt")) {
                keyHTML = cleanHTML(child);
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

    private static String getUpdateSiteUrl(final String id, final Map<String, List<String>> sites) {
        for (String site : sites.keySet()) {
            for (String siteId : sites.get(site)) {
                if (siteId.equals(id)) {
                    return site;
                }
            }
        }
        return "";
    }

    private static boolean siteEnabledByDefault(final String url) {
        return url.contains("://update.knime.com/analytics-platform")
            || url.contains("://update.knime.com/community-contributions/trusted/");
    }

    /**
     * If this is one of the update site's in KNIME AP, this returns the name that is listed there by default.
     */
    private static String getUpdateSiteName(final String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        final int index = url.lastIndexOf('/');
        final String version = url.substring(index + 1, url.length());
        if (url.contains("://update.knime.com/analytics-platform")) {
            return "KNIME Analytics Platform " + version + " Update Site";
        }
        if (url.contains("://update.knime.com/community-contributions/trusted")) {
            return "KNIME Community Contributions (" + version + ")";
        }
        if (url.contains("://update.knime.com/partner")) {
            return "KNIME Partner Update Site";
        }
        if (url.contains("://update.knime.com/community-contributions")) {
            return "Stable Community Contributions";
        }
        return null;
    }
}
