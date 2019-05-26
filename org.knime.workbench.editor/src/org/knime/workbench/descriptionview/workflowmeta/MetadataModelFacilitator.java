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
package org.knime.workbench.descriptionview.workflowmeta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.node.NodeLogger;
import org.knime.workbench.descriptionview.workflowmeta.atoms.ComboBoxMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.DateMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.LinkMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.MetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.TagMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.TextAreaMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.TextFieldMetaInfoAtom;
import org.knime.workbench.ui.workflow.metadata.MetaInfoFile;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.knime.workbench.ui.workflow.metadata.MetadataXML;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class provides a UI supported form of the metadata representation, as a wrapper with augmented functionality.
 *
 * @author loki der quaeler
 */
public class MetadataModelFacilitator implements MetaInfoAtom.MutationListener {
    /** This is the label which has been historically used to denote the author **/
    public static final String AUTHOR_LABEL = "Author";
    /** This is the label which has been historically used to denote the description **/
    public static final String DESCRIPTION_LABEL = "Comments";
    /** This is the label which has been historically used to denote the creation dat **/
    public static final String CREATION_DATE_LABEL = "Creation Date";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MetadataModelFacilitator.class);

    private static final String PRE_38_METADATA_LINK_BLOG_KEYWORD = "BLOG";
    private static final String PRE_38_METADATA_LINK_URL_KEYWORD = "URL";
    private static final String PRE_38_METADATA_LINK_VIDEO_KEYWORD = "VIDEO";
    private static final String PRE_38_METADATA_TAG_KEYWORD = "TAG";
    private static final String PRE_38_METADATA_TAGS_KEYWORD = "TAGS";
    private static final String PRE_38_METADATA_LICENSE_KEYWORD = "LICENSE";

    private static final String URL_LEGACY_KEYWORD_TYPE_NAME = "Website";

    private static final HashSet<String> PRE_38_KEYWORDS;
    static {
        PRE_38_KEYWORDS = new HashSet<>();
        PRE_38_KEYWORDS.add(PRE_38_METADATA_LINK_BLOG_KEYWORD);
        PRE_38_KEYWORDS.add(PRE_38_METADATA_LINK_URL_KEYWORD);
        PRE_38_KEYWORDS.add(PRE_38_METADATA_LINK_VIDEO_KEYWORD);
        PRE_38_KEYWORDS.add(PRE_38_METADATA_TAG_KEYWORD);
        PRE_38_KEYWORDS.add(PRE_38_METADATA_TAGS_KEYWORD);
        PRE_38_KEYWORDS.add(PRE_38_METADATA_LICENSE_KEYWORD);
    }

    /**
     * Classes which want to know when the number of atoms have changed, or an edit-time dirty state has changed,
     *  should implement this.
     */
    public interface ModelObserver {
        /**
         * Invoked when the number of atoms have changed
         *
         * @param increased true if atoms were added, false if removed
         */
        void modelCardinalityChanged(final boolean increased);

        /**
         * Invoked when the dirty state changes; when the implementor is invoked, calling
         * {@link MetadataModelFacilitator#modelIsDirty()} will return an accurate value.
         */
        void modelDirtyStateChanged();
    }


    private TextFieldMetaInfoAtom m_authorAtom;              // 1
    private DateMetaInfoAtom m_creationDateAtom;             // 1
    private TextAreaMetaInfoAtom m_descriptionAtom;          // 1
    private final ArrayList<TagMetaInfoAtom> m_tagAtoms;     // 1-N
    private final ArrayList<LinkMetaInfoAtom> m_linkAtoms;   // 1-N
    private ComboBoxMetaInfoAtom m_licenseAtom;              // 1
    private TextFieldMetaInfoAtom m_titleAtom;               // 1

    private int m_metadataVersion;

    private ModelObserver m_modelObserver;

    // for edit state store
    private final ArrayList<TagMetaInfoAtom> m_savedTagAtoms;
    private final AtomicBoolean m_tagWasDeletedDuringEdit;
    private final ArrayList<LinkMetaInfoAtom> m_savedLinkAtoms;
    private final AtomicBoolean m_linkWasDeletedDuringEdit;
    private final AtomicBoolean m_editStateIsDirty;

    MetadataModelFacilitator() {
        m_tagAtoms = new ArrayList<>();
        m_linkAtoms = new ArrayList<>();

        m_savedTagAtoms = new ArrayList<>();
        m_savedLinkAtoms = new ArrayList<>();

        m_tagWasDeletedDuringEdit = new AtomicBoolean(false);
        m_linkWasDeletedDuringEdit = new AtomicBoolean(false);
        m_editStateIsDirty = new AtomicBoolean(false);
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
            if (AUTHOR_LABEL.equals(label)) {
                typeToUse = MetadataItemType.AUTHOR;
            } else if (DESCRIPTION_LABEL.equals(label)) {
                typeToUse = MetadataItemType.DESCRIPTION;
            } else if (CREATION_DATE_LABEL.equals(label)) {
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
                    mia = new LinkMetaInfoAtom(label, value, isReadOnly, otherAttributes);
                    m_linkAtoms.add((LinkMetaInfoAtom)mia);
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

    void parsingHasFinished() {
        if (m_authorAtom == null) {
            m_authorAtom = new TextFieldMetaInfoAtom(MetadataItemType.AUTHOR, "legacy-author", null, false);
            m_authorAtom.addChangeListener(this);
        }

        if (m_titleAtom == null) {
            m_titleAtom = new TextFieldMetaInfoAtom(MetadataItemType.TITLE, "legacy-title", null, false);
            m_titleAtom.addChangeListener(this);
        } else if (MetaInfoFile.NO_TITLE_PLACEHOLDER_TEXT.equals(m_titleAtom.getValue())) {
            m_titleAtom.setValue(null);
        }

        if (m_descriptionAtom == null) {
            m_descriptionAtom = new TextAreaMetaInfoAtom("legacy-comments", null, false);
            m_descriptionAtom.addChangeListener(this);
        } else if (MetaInfoFile.NO_DESCRIPTION_PLACEHOLDER_TEXT.equals(m_descriptionAtom.getValue())) {
            m_descriptionAtom.setValue(null);
        }

        if (m_licenseAtom == null) {
            m_licenseAtom =
                new ComboBoxMetaInfoAtom("legacy", LicenseType.DEFAULT_LICENSE_NAME, false);
            m_licenseAtom.addChangeListener(this);
        }

        if (m_creationDateAtom == null) {
            m_creationDateAtom = new DateMetaInfoAtom("legacy-creation-date", null, false);
            m_creationDateAtom.addChangeListener(this);
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

    // this writes the original description-holds-everything-glommed-together metadata file.
    void writeOldStyleMetadata(final File metadataFile) throws IOException {
        try (final OutputStream os = new FileOutputStream(metadataFile)) {
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
            attributes.addAttribute(null, null, MetadataXML.NAME, "CDATA", DESCRIPTION_LABEL);
            attributes.addAttribute(null, null, MetadataXML.TYPE, "CDATA", m_descriptionAtom.getType().getType());
            handler.startElement(null, null, MetadataXML.ATOM_WRITE_ELEMENT, attributes);
            final char[] value = createOldStyleDescriptionBlock().toCharArray();
            handler.characters(value, 0, value.length);
            handler.endElement(null, null, MetadataXML.ATOM_WRITE_ELEMENT);

            MetaInfoFile.endMetadataDocument(handler);
        } catch (final SAXException | TransformerConfigurationException e) {
            throw new IOException("Caught exception while writing metadata.", e);
        }
    }

    /**
     * @param observer an implementor which wishes to know when the model changes
     */
    public void setModelObserver(final ModelObserver observer) {
        m_modelObserver = observer;
    }

    /**
     * Invoking this allows this instance to store a copy of its state, and alert all atoms to store theirs.
     */
    public void storeStateForEdit() {
        m_savedTagAtoms.addAll(m_tagAtoms);
        m_savedLinkAtoms.addAll(m_linkAtoms);

        m_titleAtom.storeStateForEdit();
        m_descriptionAtom.storeStateForEdit();
        m_authorAtom.storeStateForEdit();
        m_licenseAtom.storeStateForEdit();
        m_tagAtoms.stream().forEach((tag) -> {
            tag.storeStateForEdit();
        });
        m_linkAtoms.stream().forEach((link) -> {
            link.storeStateForEdit();
        });
        m_creationDateAtom.storeStateForEdit();

        m_tagWasDeletedDuringEdit.set(false);
        m_linkWasDeletedDuringEdit.set(false);
        m_editStateIsDirty.set(false);
    }

    /**
     * Invoking this restores this instance's state to the one stored during the call to {@link #storeStateForEdit()}
     * and alerts all atoms to do the same - the implementation of this action being subjective to the type of atom.
     */
    public void restoreState() {
        m_tagAtoms.clear();
        m_linkAtoms.clear();
        m_tagAtoms.addAll(m_savedTagAtoms);
        m_linkAtoms.addAll(m_savedLinkAtoms);
        m_savedTagAtoms.clear();
        m_savedLinkAtoms.clear();

        m_titleAtom.restoreState();
        m_descriptionAtom.restoreState();
        m_authorAtom.restoreState();
        m_licenseAtom.restoreState();
        m_tagAtoms.stream().forEach((tag) -> {
            tag.restoreState();
        });
        m_linkAtoms.stream().forEach((link) -> {
            link.restoreState();
        });
        m_creationDateAtom.restoreState();
    }

    /**
     * Invoking this releases this instance's copied state to maintain the modified-during-edit changes; it also alerts
     * all atoms to also commit their edits - the implementation of this action being subjective to the type of atom.
     */
    public void commitEdit() {
        m_savedTagAtoms.clear();
        m_savedLinkAtoms.clear();

        m_titleAtom.commitEdit();
        m_descriptionAtom.commitEdit();
        m_authorAtom.commitEdit();
        m_licenseAtom.commitEdit();
        m_tagAtoms.stream().forEach((tag) -> {
            tag.commitEdit();
        });
        m_linkAtoms.stream().forEach((link) -> {
            link.commitEdit();
        });
        m_creationDateAtom.commitEdit();
    }

    /**
     * @return true if we're in edit mode and there has been a dirty-ing action.
     */
    public boolean modelIsDirty() {
        return m_editStateIsDirty.get();
    }

    /**
     * @return the atom representing the author
     */
    public MetaInfoAtom getAuthor() {
        return m_authorAtom;
    }

    /**
     * @return the atom representing the creation date
     */
    public MetaInfoAtom getCreationDate() {
        return m_creationDateAtom;
    }

    /**
     * @return the atom representing the description
     */
    public MetaInfoAtom getDescription() {
        return m_descriptionAtom;
    }

    /**
     * @return a mutable list of tags
     */
    public List<? extends MetaInfoAtom> getTags() {
        return m_tagAtoms;
    }

    /**
     * @param tagText the text of the tag
     * @return the created instance which was added to the internal store
     */
    public MetaInfoAtom addTag(final String tagText) {
        final TagMetaInfoAtom mia = new TagMetaInfoAtom("legacy", tagText, false);

        mia.addChangeListener(this);
        m_tagAtoms.add(mia);

        metaInfoAtomBecameDirty(null);

        return mia;
    }

    /**
     * @return a mutable list of links
     */
    public List<? extends MetaInfoAtom> getLinks() {
        return m_linkAtoms;
    }

    /**
     * @param url the fully formed URL for the link
     * @param title the display text for the link
     * @param type the type for the link
     * @return the created instance which was added to the internal store
     */
    public MetaInfoAtom addLink(final String url, final String title, final String type) {
        final LinkMetaInfoAtom mia = new LinkMetaInfoAtom("legacy", title, type, url, false);

        mia.addChangeListener(this);
        m_linkAtoms.add(mia);

        metaInfoAtomBecameDirty(null);

        return mia;
    }

    /**
     * @return the atom representing the license type
     */
    public MetaInfoAtom getLicense() {
        return m_licenseAtom;
    }

    /**
     * @return the atom representing the title
     */
    public MetaInfoAtom getTitle() {
        return m_titleAtom;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void metaInfoAtomDeleted(final MetaInfoAtom deletedAtom) {
        boolean itemRemoved = false;

        switch (deletedAtom.getType()) {
            case TAG:
                if (m_tagAtoms.remove(deletedAtom)) {
                    m_editStateIsDirty.set(true);
                    m_tagWasDeletedDuringEdit.set(true);
                    itemRemoved = true;
                } else {
                    LOGGER.warn("Could not find tag [" + deletedAtom.getValue() + "] for removal.");
                }
                break;
            case LINK:
                if (m_linkAtoms.remove(deletedAtom)) {
                    m_editStateIsDirty.set(true);
                    m_linkWasDeletedDuringEdit.set(true);
                    itemRemoved = true;
                } else {
                    LOGGER.warn("Could not find link [" + deletedAtom.getValue() + "] for removal.");
                }
                break;
            default:
                LOGGER.error("Info atom of type " + deletedAtom.getType()
                    + " reports itself as deleted which should not be possible.");
                break;
        }

        if (itemRemoved) {
            metaInfoAtomBecameClean(null);

            if (m_modelObserver != null) {
                m_modelObserver.modelCardinalityChanged(false);
                m_modelObserver.modelDirtyStateChanged();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void metaInfoAtomBecameClean(final MetaInfoAtom cleanAtom) {
        if (m_titleAtom.isDirty()) {
            return;
        }
        if (m_creationDateAtom.isDirty()) {
            return;
        }
        if (m_descriptionAtom.isDirty()) {
            return;
        }
        if (m_authorAtom.isDirty()) {
            return;
        }
        if (m_licenseAtom.isDirty()) {
            return;
        }
        // I'm not going to both expending the exact array element matching and instead say if there has been
        //      any addition or deletion from these sets, then they are technically dirty.
        if (m_savedTagAtoms.size() != m_tagAtoms.size()) {
            return;
        }
        if (m_savedLinkAtoms.size() != m_linkAtoms.size()) {
            return;
        }
        // The special cheap delete situation we accept is if there was a deletion but we started off with 0
        //      elements (and, by this point of checking, still have 0 elements)
        if (m_tagWasDeletedDuringEdit.get() && (m_savedTagAtoms.size() > 0)) {
            return;
        }
        if (m_linkWasDeletedDuringEdit.get() && (m_savedLinkAtoms.size() > 0)) {
            return;
        }

        if (m_editStateIsDirty.getAndSet(false) && (m_modelObserver != null)) {
            m_modelObserver.modelDirtyStateChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void metaInfoAtomBecameDirty(final MetaInfoAtom dirtyAtom) {
        if (!m_editStateIsDirty.getAndSet(true) && (m_modelObserver != null)) {
            m_modelObserver.modelDirtyStateChanged();
        }
    }

    // Using "Specifications for workflows on EXAMPLES Server.pdf" as guidance, check whether this description
    //      is smuggling in tags and links and if so, tease them out. This should return only the description
    //      itself.
    private String potentiallyParseOldStyleDescription(final String description) {
        final String[] lines = description.split("\r?\n");

        if ((lines.length > 2) && (lines[1].trim().length() == 0)) {
            final StringBuilder actualDescription = new StringBuilder();

            final String titleLine;
            if (MetaInfoFile.NO_TITLE_PLACEHOLDER_TEXT.equals(lines[0])) {
                titleLine = null;
            } else {
                titleLine = lines[0];
            }
            m_titleAtom = new TextFieldMetaInfoAtom(MetadataItemType.TITLE, "legacy-title", titleLine, false);
            m_titleAtom.addChangeListener(this);

            final String descriptionLine;
            if (MetaInfoFile.NO_DESCRIPTION_PLACEHOLDER_TEXT.equals(lines[2])) {
                descriptionLine = "";
            } else {
                descriptionLine = lines[2];
            }
            actualDescription.append(descriptionLine);
            int counter = 3;
            boolean haveFinishedDescription = false;
            while (counter < lines.length) {
                final String line = lines[counter];

                if (line.trim().length() == 0) {
                    haveFinishedDescription = true;
                } else {
                    final int index = line.indexOf(':');
                    boolean consumedLine = false;

                    if ((index != -1) && (index < (line.length() - 2))) {
                        final String initialText = line.substring(0, index);

                        if (PRE_38_KEYWORDS.contains(initialText)) {
                            if (initialText.equals(PRE_38_METADATA_TAG_KEYWORD)
                                        || initialText.equals(PRE_38_METADATA_TAGS_KEYWORD)) {
                                final String tagsConcatenated = line.substring(index + 1).trim();
                                final String[] tags = tagsConcatenated.split(",");

                                for (final String tag : tags) {
                                    addTag(tag.trim());
                                }
                            } else if (initialText.equals(PRE_38_METADATA_LICENSE_KEYWORD)) {
                                m_licenseAtom = new ComboBoxMetaInfoAtom("legacy-license",
                                    line.substring(index + 1).trim(), false);
                                m_licenseAtom.addChangeListener(this);
                            } else {
                                final String type;
                                if (initialText.equals(PRE_38_METADATA_LINK_BLOG_KEYWORD)) {
                                    type = "Blog";
                                } else if (initialText.equals(PRE_38_METADATA_LINK_URL_KEYWORD)) {
                                    type = URL_LEGACY_KEYWORD_TYPE_NAME;
                                } else {
                                    type = "Video";
                                }

                                final String lowercaseLine = line.toLowerCase(Locale.ROOT);
                                int urlStart = lowercaseLine.indexOf("http:");
                                if (urlStart == -1) {
                                    urlStart = lowercaseLine.indexOf("https:");
                                }

                                if (urlStart == -1) {
                                    LOGGER.warn("Could not find URL in old-style metadata link citation [" + line
                                                        + "]");
                                } else {
                                    final String url = line.substring(urlStart);
                                    final String title = line.substring((index + 1), urlStart).trim();

                                    addLink(url, title, type);
                                }
                            }

                            consumedLine = true;
                            haveFinishedDescription = true;
                        }
                    }

                    if (!consumedLine && !haveFinishedDescription) {
                        actualDescription.append('\n').append(line);
                    }
                }

                counter++;
            }

            return actualDescription.toString();
        }

        return description;
    }

    // Using "Specifications for workflows on EXAMPLES Server.pdf" as guidance, create an old style description
    //      block which wraps up title, description, tags, and links (and we've added in license now.)
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
            sb.append(mia.getOldStyleDescriptionRepresentation(URL_LEGACY_KEYWORD_TYPE_NAME)).append('\n');
        }

        if (m_tagAtoms.size() > 0) {
            sb.append(PRE_38_METADATA_TAGS_KEYWORD).append(": ");

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

        if (m_licenseAtom.hasContent()) { // is currently always true but who knows about the future
            sb.append(PRE_38_METADATA_LICENSE_KEYWORD).append(": ").append(m_licenseAtom.getValue()).append('\n');
        }

        // old style does not include a trailing \n
        return sb.toString().trim();
    }
}
