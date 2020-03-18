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
 *   May 9, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.workflow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.descriptionview.metadata.AbstractMetadataModelFacilitator;
import org.knime.workbench.descriptionview.metadata.atoms.ComboBoxMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.DateMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.LinkMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.MetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TagMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextAreaMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextFieldMetaInfoAtom;
import org.knime.workbench.ui.workflow.metadata.MetaInfoFile;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class provides a UI supported form of the metadata representation, as a wrapper with augmented functionality.
 *
 * As part of https://knime-com.atlassian.net/browse/AP-12082 is was decided that the license field not be written in
 *  the save metadata; i've gated this condition with a static boolean below (search 'AP-12082') so that future
 *  generations can turn the license stuff back on when we support it more widely.
 *
 * @author loki der quaeler
 */
class MetadataModelFacilitator extends AbstractMetadataModelFacilitator {
    // I've seen both "TAG:" and "TAGS:" - we write out the latter
    private static final String LEGACY_METADATA_TAG_KEYWORD = "TAG";
    private static final String LEGACY_METADATA_TAGS_KEYWORD = "TAGS";

    private static final String LEGACY_METADATA_LICENSE_KEYWORD = "LICENSE";

    // AP-12082
    private static final boolean WRITE_LICENSE_IN_OLD_STYLE_METADATA = false;

    private static final HashSet<String> LEGACY_KEYWORDS;
    static {
        LEGACY_KEYWORDS = new HashSet<>();
        LEGACY_KEYWORDS.addAll(LinkMetaInfoAtom.LEGACY_VALID_INCOMING_TYPE_STRINGS);
        LEGACY_KEYWORDS.add(LEGACY_METADATA_TAG_KEYWORD);
        LEGACY_KEYWORDS.add(LEGACY_METADATA_TAGS_KEYWORD);
        LEGACY_KEYWORDS.add(LEGACY_METADATA_LICENSE_KEYWORD);
    }


    private int m_metadataVersion;

    MetadataModelFacilitator() {
        this(null, null, null);
    }

    MetadataModelFacilitator(final String author, final String legacyDescription, final Calendar creationDate) {
        super();

        if (author != null) {
            m_authorAtom = new TextFieldMetaInfoAtom(MetadataItemType.AUTHOR, MetadataXML.AUTHOR_LABEL, author, false);
            m_authorAtom.addChangeListener(this);
        }

        if (legacyDescription != null) {
            final String description = potentiallyParseOldStyleDescription(legacyDescription);

            m_descriptionAtom = new TextAreaMetaInfoAtom(MetadataXML.DESCRIPTION_LABEL,
                ((description.trim().length() == 0) ? null : description), false);
            m_descriptionAtom.addChangeListener(this);
        }

        if (creationDate != null) {
            m_creationDateAtom = new DateMetaInfoAtom(MetadataXML.CREATION_DATE_LABEL, creationDate, false);
            m_creationDateAtom.addChangeListener(this);
        }
    }

    /**
     * This will be invoked prior to any invocations of {@link #processElement(String, String, String, boolean, Map)}.
     *
     * @param version a version which can be compared against the version constants defined in MetaInfoFile
     */
    void setMetadataVersion(final int version) {
        m_metadataVersion = version;
    }

    /**
     * @param label the display label preferencing the UI widget
     * @param type this will be null for historical (pre-3.8.0) metadata in which case it we consult the label; if the
     *            label is Author or Comments, then we keep, however we discard anything else (which consists of only
     *            creation date.)
     * @param value the content of the metadata (e.g type == AUTHOR_TYPE, value == "Albert Camus")
     * @param isReadOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     * @param otherAttributes key-value pairs of non-universal attributes of the metadata element
     * @throws SAXException if something gets throw in an anticipatable location, we'll wrap it in a SAXException and
     *             re-throw it.
     */
    void processElement(final String label, final String type, final String value, final boolean isReadOnly,
        final Map<String, String> otherAttributes) throws SAXException {
        final MetadataItemType typeToUse;
        if (type == null) {
            // we've read in metadata created in a version prior to 3.8.0 (and which has not been resaved since.)
            if (MetadataXML.AUTHOR_LABEL.equals(label)) {
                typeToUse = MetadataItemType.AUTHOR;
            } else if (MetadataXML.DESCRIPTION_LABEL.equals(label)) {
                typeToUse = MetadataItemType.DESCRIPTION;
            } else if (MetadataXML.CREATION_DATE_LABEL.equals(label)) {
                typeToUse = MetadataItemType.CREATION_DATE;
            } else {
                return;
            }
        } else {
            typeToUse = MetadataItemType.getInfoTypeForType(type);
        }

        if (typeToUse != null) {
            MetaInfoAtom mia = null;

            switch (typeToUse) {
                case TAG:
                    mia = new TagMetaInfoAtom(label, value, isReadOnly);
                    m_tagAtoms.add((TagMetaInfoAtom)mia);
                    break;
                case LINK:
                    try {
                        mia = new LinkMetaInfoAtom(label, value, isReadOnly, otherAttributes);
                        m_linkAtoms.add((LinkMetaInfoAtom)mia);
                    } catch (final MalformedURLException e) {
                        final String url =
                            ((otherAttributes != null) ? otherAttributes.get(MetadataXML.URL_URL_ATTRIBUTE) : null);
                        m_logger.error("Could not parse incoming URL [" + url + "]", e);
                    }
                    break;
                case AUTHOR:
                    m_authorAtom = new TextFieldMetaInfoAtom(MetadataItemType.AUTHOR, label, value, isReadOnly);
                    mia = m_authorAtom;
                    break;
                case CREATION_DATE:
                    m_creationDateAtom = new DateMetaInfoAtom(label, value, isReadOnly);
                    mia = m_creationDateAtom;
                    break;
                case TITLE:
                    m_titleAtom = new TextFieldMetaInfoAtom(MetadataItemType.TITLE, label, value, isReadOnly);
                    mia = m_titleAtom;
                    break;
                case DESCRIPTION:
                    final String description;
                    if (m_metadataVersion < MetaInfoFile.METADATA_VERSION_NG_STORAGE) {
                        description = potentiallyParseOldStyleDescription(value);
                    } else {
                        description = value;
                    }
                    m_descriptionAtom =
                        new TextAreaMetaInfoAtom(label, ((description.trim().length() == 0) ? null : description), isReadOnly);
                    mia = m_descriptionAtom;
                    break;
                case LICENSE:
                    m_licenseAtom = new ComboBoxMetaInfoAtom(label, value, isReadOnly);
                    mia = m_licenseAtom;
                    break;
            }

            if (mia != null) {
                mia.addChangeListener(this);
            }
        }
    }

    /**
     * This is preserved so that when me move to the new style XML format file, we can switch to invoking this
     * from WorkflowMetaView.performSave().
     */
    void writeMetadata(final File metadataFile) throws IOException {
        try (final OutputStream os = new FileOutputStream(metadataFile)) {
            final SAXTransformerFactory fac = (SAXTransformerFactory)TransformerFactory.newInstance();
            final TransformerHandler handler = fac.newTransformerHandler();
            final Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            handler.setResult(new StreamResult(os));

            MetaInfoFile.startMetadataDocument(handler);

            m_authorAtom.save(handler);
            m_descriptionAtom.save(handler);
            m_licenseAtom.save(handler);
            m_titleAtom.save(handler);
            m_creationDateAtom.save(handler);
            for (final MetaInfoAtom mia : m_tagAtoms) {
                mia.save(handler);
            }
            for (final MetaInfoAtom mia : m_linkAtoms) {
                mia.save(handler);
            }

            MetaInfoFile.endMetadataDocument(handler);
        } catch (final SAXException | TransformerConfigurationException e) {
            throw new IOException("Caught exception while writing metadata.", e);
        }
    }

    /**
     * This returns the original description-holds-everything-glommed-together format of this instance's metadata
     *  state.
     */
    String metadataSavedInLegacyFormat() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeLegacyStyleMetadataToStream(baos);
        return baos.toString("UTF-8");
    }

    private void writeLegacyStyleMetadataToStream(final OutputStream os) throws IOException {
        try {
            final SAXTransformerFactory fac = (SAXTransformerFactory)TransformerFactory.newInstance();
            final TransformerHandler handler = fac.newTransformerHandler();
            final Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            handler.setResult(new StreamResult(os));

            MetaInfoFile.startMetadataDocument(handler);

            m_authorAtom.save(handler);
            m_creationDateAtom.save(handler);

            final AttributesImpl attributes = new AttributesImpl();
            attributes.addAttribute(null, null, MetadataXML.FORM, "CDATA", MetadataXML.MULTILINE);
            attributes.addAttribute(null, null, MetadataXML.READ_ONLY, "CDATA", Boolean.toString(false));
            attributes.addAttribute(null, null, MetadataXML.NAME, "CDATA", MetadataXML.DESCRIPTION_LABEL);

            // TODO including a type will break the server parsing of the metadata - 4.0.0
//            attributes.addAttribute(null, null, MetadataXML.TYPE, "CDATA", m_descriptionAtom.getType().getType());

            handler.startElement(null, null, MetadataXML.ATOM_WRITE_ELEMENT, attributes);
            final char[] value = createOldStyleDescriptionBlock().toCharArray();
            handler.characters(value, 0, value.length);
            handler.endElement(null, null, MetadataXML.ATOM_WRITE_ELEMENT);

            MetaInfoFile.endMetadataDocument(handler);
        } catch (final SAXException | TransformerConfigurationException e) {
            throw new IOException("Caught exception while writing metadata.", e);
        }
    }

    // Using "Specifications for workflows on EXAMPLES Server.pdf" as guidance, check whether this description
    //      is smuggling in tags and links and if so, tease them out. This should return only the description
    //      itself.
    private String potentiallyParseOldStyleDescription(final String description) {
        final String[] lines = description.split("\r?\n");
        boolean isOldStyle = (lines.length > 2);

        if (isOldStyle) {
            // is the at least one blank line sandwiched between two non-blank lines? TODO - this will be an
            //          insufficient check in the future since descriptions can have blank lines; for the moment
            //          (aka 4.0) we know we have only 'legacy' format.
            isOldStyle = false;
            for (int i = 1; i < (lines.length - 1); i++) {
                if ((lines[i].trim().length() == 0) && (lines[i + 1].trim().length() > 0)) {
                    isOldStyle = true;
                    break;
                }
            }
        }

        if (isOldStyle) {
            final StringBuilder actualDescription = new StringBuilder();

            final String titleLine;
            int lineIndex = 0;
            if (MetaInfoFile.NO_TITLE_PLACEHOLDER_TEXT.equals(lines[lineIndex])) {
                titleLine = null;
                lineIndex++;
            } else {
                final StringBuilder title = new StringBuilder(lines[lineIndex]);

                lineIndex++;
                while (lines[lineIndex].trim().length() > 0) {
                    title.append('\n').append(lines[lineIndex]);
                    lineIndex++;
                }

                titleLine = title.toString();
            }
            lineIndex++;

            m_titleAtom = new TextFieldMetaInfoAtom(MetadataItemType.TITLE, "legacy-title", titleLine, false);
            m_titleAtom.addChangeListener(this);

            if (!MetaInfoFile.NO_DESCRIPTION_PLACEHOLDER_TEXT.equals(lines[lineIndex])) {
                actualDescription.append(lines[lineIndex]);
            }
            lineIndex++;

            boolean haveFinishedDescription = false;
            while (lineIndex < lines.length) {
                final String line = lines[lineIndex];

                final int index = line.indexOf(':');
                boolean consumedLine = false;

                if ((index != -1) && (index < (line.length() - 2))) {
                    final String initialText = line.substring(0, index).toUpperCase();

                    if (LEGACY_KEYWORDS.contains(initialText)) {
                        if (initialText.equals(LEGACY_METADATA_TAG_KEYWORD)
                                    || initialText.equals(LEGACY_METADATA_TAGS_KEYWORD)) {
                            final String tagsConcatenated = line.substring(index + 1).trim();
                            final String[] tags = tagsConcatenated.split(",");

                            for (final String tag : tags) {
                                addTag(tag.trim());
                            }
                        } else if (initialText.equals(LEGACY_METADATA_LICENSE_KEYWORD)) {
                            m_licenseAtom = new ComboBoxMetaInfoAtom("legacy-license",
                                line.substring(index + 1).trim(), false);
                            m_licenseAtom.addChangeListener(this);
                        } else {
                            final String type = LinkMetaInfoAtom.getDisplayLinkTypeForKeyword(initialText);
                            final String lowercaseLine = line.toLowerCase(Locale.ROOT);
                            int urlStart = lowercaseLine.indexOf("http:");
                            if (urlStart == -1) {
                                urlStart = lowercaseLine.indexOf("https:");
                            }

                            if (urlStart == -1) {
                                m_logger.warn("Could not find URL in legacy metadata link citation [" + line
                                                    + "]");
                            } else {
                                final String url = line.substring(urlStart);
                                final String title = line.substring((index + 1), urlStart).trim();

                                try {
                                    addLink(url, title, type);
                                } catch (final MalformedURLException e) {
                                    m_logger.error("Could not parse incoming URL [" + url + "]", e);
                                }
                            }
                        }

                        consumedLine = true;
                        haveFinishedDescription = true;
                    }
                }

                if (!consumedLine && !haveFinishedDescription) {
                    actualDescription.append('\n').append(line);
                }

                lineIndex++;
            }

            return actualDescription.toString();
        }

        return description;
    }

    // Using "Specifications for workflows on EXAMPLES Server.pdf" as guidance, create an old style description
    //      block which wraps up title, description, tags, and links (and we've added in license now.)
    @SuppressWarnings("unused")  // because we currently have a dead code block due to WRITE_LICENSE_IN_OLD_STYLE_METADATA = false
    private String createOldStyleDescriptionBlock() {
        final StringBuilder sb = new StringBuilder();

        if (m_titleAtom.hasContent()) {
            sb.append(m_titleAtom.getValue());
        } else {
            sb.append(MetaInfoFile.NO_TITLE_PLACEHOLDER_TEXT);
        }
        sb.append("\n\n");

        if (m_descriptionAtom.hasContent()) {
            sb.append(m_descriptionAtom.getValue());
        } else {
            sb.append(MetaInfoFile.NO_DESCRIPTION_PLACEHOLDER_TEXT);
        }
        sb.append("\n\n");

        for (final LinkMetaInfoAtom mia : m_linkAtoms) {
            sb.append(mia.getLegacyDescriptionRepresentation()).append('\n');
        }

        if (m_tagAtoms.size() > 0) {
            sb.append(LEGACY_METADATA_TAGS_KEYWORD).append(": ");

            boolean appendDelimiter = false;
            for (final MetaInfoAtom mia : m_tagAtoms) {
                if (appendDelimiter) {
                    sb.append(',');
                }

                sb.append(mia.getValue());
                appendDelimiter = true;
            }
            sb.append('\n');
        }

        if (m_licenseAtom.hasContent() && WRITE_LICENSE_IN_OLD_STYLE_METADATA) {
            sb.append(LEGACY_METADATA_LICENSE_KEYWORD).append(": ").append(m_licenseAtom.getValue()).append('\n');
        }

        // old style does not include a trailing \n
        return sb.toString().trim();
    }
}
