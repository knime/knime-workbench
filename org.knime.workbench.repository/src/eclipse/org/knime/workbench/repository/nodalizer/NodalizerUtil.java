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
 *   Jan 7, 2021 (soenke): created
 */
package org.knime.workbench.repository.nodalizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonWriter;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.knime.core.node.MapNodeFactoryClassMapper;
import org.knime.core.node.NodeFactoryClassMapper;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.RegexNodeFactoryClassMapper;
import org.knime.core.util.Pair;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility functions around {@link Nodalizer} functionality.
 *
 * @author Soenke Sobott, KNIME GmbH, Konstanz, Germany
 */
public final class NodalizerUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodalizerUtil.class);

    private NodalizerUtil() {
        // Prevent instantiation of util class
    }

    /**
     * Parses the update site info.
     *
     * @param repo the eclipse update site to parse
     * @return a new {@link SiteInfo}
     */
    public static SiteInfo parseUpdateSite(final IMetadataRepository repo) {
        final String name = repo.getName();
        final String url = repo.getLocation().toString();
        final boolean enabledByDefault = siteEnabledByDefault(url);
        final boolean isTrusted = isTrusted(url);
        return new SiteInfo(url, enabledByDefault, isTrusted, name);
    }

    /**
     * Parses the dl tag.
     *
     * @param dl an element representing a {@code <dl>} tag
     * @param fields list of {@link NamedField} to which parsed elements will be added
     * @param checkOptional if {@code true} then the method will check for an "optional" field, this is used with some
     *            node dialog options
     */
    public static void parseDLTag(final org.jsoup.nodes.Element dl, final List<NamedField> fields,
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
                    Pair<Boolean, String> result = checkOptional(keyHTML, child);
                    optional = result.getFirst();
                    keyHTML = result.getSecond();
                }
            }
        }
    }

    /**
     * Cleans the element by removing extraneous {@code br} tags.
     *
     * @param e the element to clean
     * @return the cleaned html string
     */
    public static String cleanHTML(final org.jsoup.nodes.Element e) {
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

    /**
     * Check if an element has text.
     *
     * @param e the element to check
     * @return boolean indicating whether the element has text or not
     */
    public static boolean hasText(final org.jsoup.nodes.Element e) {
        if (e.children().isEmpty()) {
            return e.hasText();
        }
        boolean hasText = e.hasText();
        for (final org.jsoup.nodes.Element child : e.children()) {
            hasText |= hasText(child);
        }
        return hasText;
    }

    /**
     * Get Hex string of an integer representing a color.
     *
     * @param color the integer value of the color
     * @return the Hex string of representation of the given color
     */
    public static String getColorAsHex(final int color) {
        final StringBuilder builder = new StringBuilder();
        builder.append(Integer.toHexString(color));
        while (builder.length() < 6) {
            builder.insert(0, "0");
        }

        StringBuilder opacity = new StringBuilder("");
        while (builder.length() > 6) {
            opacity.append(builder.charAt(0));
            builder.deleteCharAt(0);
        }
        if (!opacity.toString().isEmpty() && !opacity.toString().equalsIgnoreCase("ff")) {
            throw new IllegalArgumentException("Opacity is not supported for node port colors");
        }
        return builder.toString();
    }

    /**
     * Writes the node/extension POJO as JSON to the given output directory.
     *
     * @param outputDir the output directory to write to
     * @param baseFileName the "base" of the file name (i.e. factoryName or symbolicName)
     * @param pojoToWrite the POJO to write to a file (i.e. {@link NodeInfo} or {@link ExtensionInfo})
     * @throws JsonProcessingException if an error occurs when writing the POJOs to JSON
     * @throws FileNotFoundException if the given output directory does not exist
     * @throws UnsupportedEncodingException if the file encoding (UTF-8) is not supported
     */
    public static void writeFile(final File outputDir, final String baseFileName, final Object pojoToWrite)
        throws JsonProcessingException, FileNotFoundException, UnsupportedEncodingException {
        final ObjectMapper map = new ObjectMapper();
        map.setSerializationInclusion(Include.NON_ABSENT);
        map.enable(SerializationFeature.INDENT_OUTPUT);
        final String json = map.writeValueAsString(pojoToWrite);
        final String regex = "\\W+";
        String fileName = baseFileName.replaceAll(regex, "_");
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

    /**
     * Cleans a given symbolic name by stripping the trailing ".feature.group". This ending is added automatically to
     * extensions but is not actually in the {@code feature.xml}.
     *
     * @param symbolicName the string representing a symbolic name to clean
     * @return the cleaned symbolic name
     */
    public static String cleanSymbolicName(final String symbolicName) {
        if (symbolicName != null && symbolicName.endsWith(".feature.group")) {
            return symbolicName.substring(0, symbolicName.length() - 14);
        }
        return symbolicName;
    }

    /**
     * Creates the node mappings directory inside the output directory and writes all available node mappings
     * to a JSON file.
     *
     * The format of the node mappings, inside the JSON file, is the type of the mapping, source or regex and target or replacement.
     * e.g.:
     * <pre>
     *  [
     *      {
     *          "type":"FACTORY_NAME",
     *          "source":"org.knime.base.node.mine.treeensemble.node.learner.TreeEnsembleLearnerNodeFactory",
     *          "target":"org.knime.base.node.mine.treeensemble2.node.learner.classification.TreeEnsembleClassificationLearnerNodeFactory"
     *      },
     *      {
     *          "type": "REGEX",
     *          "matchPattern": "^com\\.knime\\.reporting.*",
     *          "replacePattern": "^com\\.knime\\.reporting",
     *          "replacement": "org.knime.reporting"
     *      }
     *  ]
     * </pre>
     *
     * @param outputDir the output directory to which the file is written
     * @throws IOException if error occurs when writing file
     */
    public static void writeNodeMappingsJson(final File outputDir) throws IOException {
        File nodeMappingsDir = new File(outputDir, "nodeMappings");
        if (!nodeMappingsDir.exists()) {
            nodeMappingsDir.mkdir();
        }

        JsonArrayBuilder nodeMappingsBuilder = Json.createArrayBuilder();

        List<NodeFactoryClassMapper> classMapperList = NodeFactoryClassMapper.getRegisteredMappers();
        for (NodeFactoryClassMapper nodeFactoryClassMapper : classMapperList) {
            if (nodeFactoryClassMapper instanceof MapNodeFactoryClassMapper) {
                MapNodeFactoryClassMapper nodeFactoryMapper = (MapNodeFactoryClassMapper)nodeFactoryClassMapper;
                nodeFactoryMapper.getMap().forEach((key, value) -> {
                    JsonObject nodeFactoryMapping = Json.createObjectBuilder()
                            .add("type", "FACTORY_NAME")
                            .add("source", key)
                            .add("target", value.getName())
                            .build();
                    nodeMappingsBuilder.add(nodeFactoryMapping);
                });
            } else if (nodeFactoryClassMapper instanceof RegexNodeFactoryClassMapper) {
                RegexNodeFactoryClassMapper regexMapper = (RegexNodeFactoryClassMapper)nodeFactoryClassMapper;
                regexMapper.getRegexRules().forEach((key, value) -> {
                    JsonObject regexMapping = Json.createObjectBuilder()
                            .add("type", "REGEX")
                            .add("matchPattern", key)
                            .add("replacePattern", value.getFirst())
                            .add("replacement", value.getSecond())
                            .build();
                    nodeMappingsBuilder.add(regexMapping);
                });
            } else {
                // Only NodeFactoryClassMappers that extend MapNodeFactoryClassMapper or RegexNodeFactoryClassMapper can be extracted
                LOGGER.info("Could not parse node mapper '" + nodeFactoryClassMapper.getClass()
                    + "'. Only node mappers which extend MapNodeFactoryClassMapper or RegexNodeFactoryClassMapper can be parsed.");
            }
        }

        JsonArray nodeMappings = nodeMappingsBuilder.build();

        File nodeMappingsJson = new File(nodeMappingsDir, "nodeMappings.json");
        try (FileOutputStream fileOutputStream = new FileOutputStream(nodeMappingsJson)) {
            try (JsonWriter writer = Json.createWriter(fileOutputStream)) {
                writer.writeArray(nodeMappings);
            }
        }
    }

    /**
     * Remove leading and trailing non-printing characters (i.e. ' ', \n, \t, etc.) from the given String.
     *
     * @param value the String to trim
     * @return the trimmed String
     */
    public static String trimWhiteSpace(final String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        int startIndex = 0;
        int endIndex = value.length();
        // remove leading whitespace characters i.e. \n, \t, \r, ' ', etc.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (CharUtils.isAsciiPrintable(c) && c != ' ') {
                startIndex = i;
                break;
            }
        }
        // remove trailing whitespace characters i.e. \n, \t, \r, ' ', etc.
        for (int i = value.length() - 1; i >= 0; i--) {
            char c = value.charAt(i);
            if (CharUtils.isAsciiPrintable(c) && c != ' ') {
                endIndex = i + 1;
                break;
            }
        }
        return value.substring(startIndex, endIndex);
    }

    // -- Helper methods --

    private static boolean isTrusted(final String url) {
        return siteEnabledByDefault(url);
    }

    private static boolean siteEnabledByDefault(final String url) {
        return url.contains("://update.knime.com/analytics-platform")
            || url.contains("://update.knime.com/community-contributions/trusted/")
            || url.contains("://update.knime.com/partner/");
    }

    private static Pair<Boolean, String> checkOptional(String keyHTML, final org.jsoup.nodes.Element child) {
        boolean optional = false;
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
        return new Pair<>(optional, keyHTML);
    }
}
