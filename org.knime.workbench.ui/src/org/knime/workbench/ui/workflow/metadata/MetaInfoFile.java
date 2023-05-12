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
package org.knime.workbench.ui.workflow.metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.text.WordUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.core.util.Pair;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class for the meta info file which contains the meta information entered by the user for workflow groups and
 * workflows, such as author, date, comments.
 *
 * Fabian Dill wrote the original version off which this class is based.
 */
@Deprecated(since="5.1")
public final class MetaInfoFile {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MetaInfoFile.class);

    /** Metadata version constant to represent no specified version. **/
    public static final int METADATA_NO_VERSION = -1;
    /** Metadata version starting with a future release in which we start supporting the new XML format. **/
    public static final int METADATA_VERSION_NG_STORAGE = Integer.MAX_VALUE;

    /** File name of the legacy metadata files. */
    public static final String FILE_NAME = "workflowset.meta";

    /** the namespace prefix we use **/
    public static final String NAMESPACE_PREFIX = MetadataXML.NAMESPACE_PREFIX;
    /** the URI we attach to our namespace **/
    public static final String NAMESPACE_URI = MetadataXML.NAMESPACE_URI;

    /** This is the label which has been historically used to denote the author **/
    public static final String AUTHOR_LABEL = MetadataXML.AUTHOR_LABEL;
    /** This is the label which has been historically used to denote the description **/
    public static final String DESCRIPTION_LABEL = MetadataXML.DESCRIPTION_LABEL;
    /** This is the label which has been historically used to denote the creation dat **/
    public static final String CREATION_DATE_LABEL = MetadataXML.CREATION_DATE_LABEL;

    /** The sub-element name which describes a single atom of metadata.  */
    public static final String ATOM_ELEMENT = MetadataXML.ATOM_ELEMENT;
    /** The sub-element name which describes a single atom of metadata.  */
    public static final String ATOM_WRITE_ELEMENT = MetadataXML.ATOM_WRITE_ELEMENT;


    /** The root element name for describing metadata.  */
    public static final String METADATA_ELEMENT = MetadataXML.METADATA_ELEMENT;
    /** The root element name for describing metadata for writing.  */
    public static final String METADATA_WRITE_ELEMENT = MetadataXML.METADATA_WRITE_ELEMENT;

    /**
     * Attribute name for the UI element descriptor; this is a holdover from pre-3.8.0 metadata storage and is basically
     * useless now as we craft the UI on what metadata type we are displaying. That said, since we still write
     * "old style" metadata in the 3.8 release, this attribute is used in the writes.
     */
    public static final String FORM = MetadataXML.FORM;

    /** Attribute name for the display label of this element */
    public static final String NAME = MetadataXML.NAME;

    /** Attribute name for the metadata type of this element */
    public static final String TYPE = MetadataXML.TYPE;

    /** Attribute name for the "read-only" attribute. */
    public static final String READ_ONLY = MetadataXML.READ_ONLY;

    /** Valid 'form' attribute value for dates */
    public static final String DATE = MetadataXML.DATE;

    /** Valid 'form' attribute value for text fields */
    public static final String TEXT = MetadataXML.TEXT;

    /**
     * Attribute name for the root element tag which lists the version of the metadata; does not exist in pre-3.8 KAP
     * metadata.
     */
    public static final String METADATA_VERSION = MetadataXML.METADATA_VERSION;

    /**
     * We need some existent but actual-use-unlikely (but, still legible and unoffensive) text to denote
     * "no title" since we're still writing the old "cram everything into the description block and parse it"
     * format and so should not really have a blank first line.
     */
    public static final String NO_TITLE_PLACEHOLDER_TEXT = MetadataXML.NO_TITLE_PLACEHOLDER_TEXT;
    /** ... and similarly for the description block **/
    public static final String NO_DESCRIPTION_PLACEHOLDER_TEXT = MetadataXML.NO_DESCRIPTION_PLACEHOLDER_TEXT;

    // I've seen both "TAG:" and "TAGS:" - we write out the latter
    public static final String LEGACY_METADATA_TAG_KEYWORD = "TAG";
    public static final String LEGACY_METADATA_TAGS_KEYWORD = "TAGS";

    public static final String LEGACY_METADATA_LICENSE_KEYWORD = "LICENSE";

    /** The fixed acceptable link type for Blog links **/
    public static final String LEGACY_METADATA_LINK_BLOG_TYPE_KEYWORD = "BLOG";
    /** The fixed acceptable link type for general Website links **/
    public static final String LEGACY_METADATA_LINK_URL_TYPE_KEYWORD = "URL";
    /** The fixed acceptable link type for Video links **/
    public static final String LEGACY_METADATA_LINK_VIDEO_TYPE_KEYWORD = "VIDEO";

    // I've seen legacy metadata show up with this as a type:
    public static final String URL_LEGACY_KEYWORD_TYPE_NAME = MetadataXML.URL_LEGACY_KEYWORD_TYPE_NAME;

    // AP-12082
    public static final boolean WRITE_LICENSE_IN_OLD_STYLE_METADATA = false;

    /**
     * This set is different than LEGACY_LINK_TYPES as this contains what we have seen in terms of what people have
     * historically used, whereas LEGACY_LINK_TYPES is an attempt to clamp down on what is legal going forward (until we
     * switch over to a new format for the metadata storage.)
     **/
    public static final HashSet<String> LEGACY_VALID_INCOMING_TYPE_STRINGS;

    public static final DualHashBidiMap<String, String> DISPLAY_KEYWORD_BIDI_MAP;

    /**
     * A list in preferred combobox display order of link types; display name is on the left, metadata keyword is on the
     * right.
     **/
    public static final List<Pair<String, String>> LEGACY_LINK_TYPES;

    private static final HashSet<String> LEGACY_KEYWORDS;
    static {

        LEGACY_VALID_INCOMING_TYPE_STRINGS = new HashSet<>();
        LEGACY_VALID_INCOMING_TYPE_STRINGS.add(URL_LEGACY_KEYWORD_TYPE_NAME.toUpperCase());
        LEGACY_VALID_INCOMING_TYPE_STRINGS.add(LEGACY_METADATA_LINK_BLOG_TYPE_KEYWORD);
        LEGACY_VALID_INCOMING_TYPE_STRINGS.add(LEGACY_METADATA_LINK_URL_TYPE_KEYWORD);
        LEGACY_VALID_INCOMING_TYPE_STRINGS.add(LEGACY_METADATA_LINK_VIDEO_TYPE_KEYWORD);

        LEGACY_KEYWORDS = new HashSet<>();
        LEGACY_KEYWORDS.addAll(LEGACY_VALID_INCOMING_TYPE_STRINGS);
        LEGACY_KEYWORDS.add(LEGACY_METADATA_TAG_KEYWORD);
        LEGACY_KEYWORDS.add(LEGACY_METADATA_TAGS_KEYWORD);
        LEGACY_KEYWORDS.add(LEGACY_METADATA_LICENSE_KEYWORD);

        DISPLAY_KEYWORD_BIDI_MAP = new DualHashBidiMap<>();
        LEGACY_LINK_TYPES = new ArrayList<>();
        addDisplayKeywordPair(URL_LEGACY_KEYWORD_TYPE_NAME, LEGACY_METADATA_LINK_URL_TYPE_KEYWORD);
        addDisplayKeywordPair(LEGACY_METADATA_LINK_VIDEO_TYPE_KEYWORD, LEGACY_METADATA_LINK_VIDEO_TYPE_KEYWORD);
        addDisplayKeywordPair(LEGACY_METADATA_LINK_BLOG_TYPE_KEYWORD, LEGACY_METADATA_LINK_BLOG_TYPE_KEYWORD);
    }

    private static void addDisplayKeywordPair(final String display, final String keyword) {
        final String capitalized = WordUtils.capitalizeFully(display);

        LEGACY_LINK_TYPES.add(Pair.create(capitalized, keyword));
        DISPLAY_KEYWORD_BIDI_MAP.put(capitalized, keyword);
    }

    /** Preference key for a workflow template. */
    public static final String PREF_KEY_META_INFO_TEMPLATE_WF = "org.knime.ui.metainfo.template.workflow";

    /** Preference key for a workflow group template. */
    public static final String PREF_KEY_META_INFO_TEMPLATE_WFS = "org.knime.ui.metainfo.template.workflowset";

    /**
     * If there is a template for workflow metadata, it is attempted to be used to create the metadata for parent
     * directory; otherwise, if the metadata file already exists, nothing is none and if it does not exist a default
     * (author and creation date only) one is created.
     *
     * @param parent parent directory in which the metadata file should sit
     * @param isWorkflow true if it is a meta info for a workflow
     * @return a handle to the metainfo file
     */
    public static File createOrGetMetaInfoFileForDirectory(final File parent, final boolean isWorkflow) {
        final File meta = new File(parent, FILE_NAME);
        // look into preference store
        final File f = ((Supplier<File>)() -> getFileFromPreferences(isWorkflow)).get();
        if (f != null) {
            writeFileFromPreferences(meta, f);
        } else if (!meta.exists() || (meta.length() == 0)) { // Future TODO better detection of a corrupt file
            createDefaultFileFallback(meta);
        }

        return meta;
    }

    /**
     * If there is a template for workflow metadata, it is attempted to be used to create the metadata for parent
     * directory; otherwise, if the metadata file already exists, nothing is none and if it does not exist a default
     * (author and creation date only) one is created.
     *
     * @param meta the metadata file (it may or may not exist)
     * @param isWorkflow true if it is a meta info for a workflow
     * @return a handle to the metainfo file
     */
    public static File createOrGetMetaInfoFile(final File meta, final boolean isWorkflow) {
        // look into preference store
        final File f = getFileFromPreferences(isWorkflow);
        if (f != null) {
            writeFileFromPreferences(meta, f);
        } else if (!meta.exists() || (meta.length() == 0)) { // Future TODO better detection of a corrupt file
            createDefaultFileFallback(meta);
        }

        return meta;
    }

    private static void writeFileFromPreferences(final File meta, final File f) {
        try {
            Files.copy(meta.toPath(), f.toPath());
        } catch (final IOException io) {
            LOGGER.error("Error while creating meta info from template for " + meta.getParentFile().getName()
                    + ". Creating default file...", io);

            createDefaultFileFallback(meta);
        }
    }

    /**
     * A convenience method for consumers to ensure the correct root element gets set.
     *
     * @param handler the document handler
     * @throws SAXException
     */
    public static void startMetadataDocument(final TransformerHandler handler) throws SAXException {
        handler.startDocument();

        handler.startPrefixMapping(NAMESPACE_PREFIX, NAMESPACE_URI);

        // TODO including a version breaks server parsing of the metadata - 4.0.0
//        final AttributesImpl atts = new AttributesImpl();
//        atts.addAttribute(null, null, MetadataXML.METADATA_VERSION, "CDATA", Integer.toString(METADATA_VERSION_20190530));

        handler.startElement(null, null, METADATA_WRITE_ELEMENT, null);
    }

    /**
     * A convenience method to close out the document.
     *
     * @param handler the document handler
     * @throws SAXException
     */
    public static void endMetadataDocument(final TransformerHandler handler) throws SAXException {
        handler.endElement(null, null, METADATA_WRITE_ELEMENT);
        handler.endPrefixMapping(NAMESPACE_PREFIX);
        handler.endDocument();
    }

    private static File getFileFromPreferences(final boolean isWorkflow) {
        final var key = isWorkflow ? PREF_KEY_META_INFO_TEMPLATE_WF : PREF_KEY_META_INFO_TEMPLATE_WFS;
        final var fileName = KNIMEUIPlugin.getDefault().getPreferenceStore().getString(key);
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        final var template = new File(fileName);
        if (!template.exists() || (template.length() == 0)) {
            return null;
        }
        return template;
    }

    private static void createDefaultFileFallback(final File meta) {
        try {
            final SAXTransformerFactory fac = (SAXTransformerFactory)TransformerFactory.newInstance();
            final TransformerHandler handler = fac.newTransformerHandler();

            final Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            try (final OutputStream out = new FileOutputStream(meta)) {
                handler.setResult(new StreamResult(out));

                startMetadataDocument(handler);

                // author
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute(null, null, FORM, "CDATA", TEXT);
                atts.addAttribute(null, null, NAME, "CDATA", AUTHOR_LABEL);
                // TODO including a type breaks server parsing of the metadata - 4.0.0
//                atts.addAttribute(null, null, MetadataXML.TYPE, "CDATA", MetadataItemType.AUTHOR.getType());
                handler.startElement(null, null, ATOM_WRITE_ELEMENT, atts);
                final String userName = System.getProperty("user.name");
                if ((userName != null) && (userName.length() > 0)) {
                    final char[] value = userName.toCharArray();
                    handler.characters(value, 0, value.length);
                }
                handler.endElement(null, null, ATOM_WRITE_ELEMENT);

                // creation date
                atts = new AttributesImpl();
                atts.addAttribute(null, null, FORM, "CDATA", DATE);
                atts.addAttribute(null, null, NAME, "CDATA", CREATION_DATE_LABEL);
                // TODO including a type breaks server parsing of the metadata - 4.0.0
//                atts.addAttribute(null, null, MetadataXML.TYPE, "CDATA", MetadataItemType.CREATION_DATE.getType());
                handler.startElement(null, null, ATOM_WRITE_ELEMENT, atts);
                final var now = ZonedDateTime.now();
                final String date = now.getDayOfMonth() + "/" + now.getMonthValue() + "/" + now.getYear();
                final char[] dateChars = date.toCharArray();
                handler.characters(dateChars, 0, dateChars.length);
                handler.endElement(null, null, ATOM_WRITE_ELEMENT);

                endMetadataDocument(handler);
            }
        } catch (final Exception e) {
            LOGGER.error("Error while trying to create default meta info file for " + meta.getParentFile().getName(),
                e);
        }
    }

    private MetaInfoFile() { }
}
