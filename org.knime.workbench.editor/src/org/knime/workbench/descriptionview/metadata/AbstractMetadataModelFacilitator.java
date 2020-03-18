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
 *   Oct 31, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.descriptionview.metadata.atoms.ComboBoxMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.DateMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.LinkMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.MetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TagMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextAreaMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextFieldMetaInfoAtom;
import org.knime.workbench.ui.workflow.metadata.MetaInfoFile;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;

/**
 * This is the abstract superclass of metadata model facilitators; there is a concrete subclass which supports an origin
 * of XML and another which supports an origin of a {@code SubNodeContainer} instance.
 *
 * @author loki der quaeler
 */
public abstract class AbstractMetadataModelFacilitator implements MetaInfoAtom.MutationListener {
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
         * {@link AbstractMetadataModelFacilitator#modelIsDirty()} will return an accurate value.
         */
        void modelDirtyStateChanged();
    }


    /** the author atom **/
    protected TextFieldMetaInfoAtom m_authorAtom;              // 1
    /** the creation date atom **/
    protected DateMetaInfoAtom m_creationDateAtom;             // 1
    /** the description atom **/
    protected TextAreaMetaInfoAtom m_descriptionAtom;          // 1
    /** the tag atoms **/
    protected final ArrayList<TagMetaInfoAtom> m_tagAtoms;     // 1-N
    /** the link atoms **/
    protected final ArrayList<LinkMetaInfoAtom> m_linkAtoms;   // 1-N
    /** the license atom **/
    protected ComboBoxMetaInfoAtom m_licenseAtom;              // 1
    /** the title atom **/
    protected TextFieldMetaInfoAtom m_titleAtom;               // 1

    /** a NodeLogger available for subclasses **/
    protected final NodeLogger m_logger;

    private ModelObserver m_modelObserver;

                    // for edit state store
    private final ArrayList<TagMetaInfoAtom> m_savedTagAtoms;
    private final ArrayList<LinkMetaInfoAtom> m_savedLinkAtoms;
    private final AtomicBoolean m_tagWasDeletedDuringEdit;
    private final AtomicBoolean m_linkWasDeletedDuringEdit;
    private final AtomicBoolean m_editStateIsDirty;

    /**
     * All subclasses should invoke this in their constructors.
     */
    protected AbstractMetadataModelFacilitator() {
        m_tagAtoms = new ArrayList<>();
        m_linkAtoms = new ArrayList<>();

        m_savedTagAtoms = new ArrayList<>();
        m_savedLinkAtoms = new ArrayList<>();

        m_tagWasDeletedDuringEdit = new AtomicBoolean(false);
        m_linkWasDeletedDuringEdit = new AtomicBoolean(false);
        m_editStateIsDirty = new AtomicBoolean(false);

        m_logger = NodeLogger.getLogger(getClass());
    }

    /**
     * This will be called when the consumption of the metadata has finished.
     *
     * @param defaultTitleName the default name for the title field if none has been defined in the source metadata
     */
    public void parsingHasFinishedWithDefaultTitleName(final String defaultTitleName) {
        if (m_authorAtom == null) {
            m_authorAtom = new TextFieldMetaInfoAtom(MetadataItemType.AUTHOR, MetadataXML.AUTHOR_LABEL,
                System.getProperty("user.name"), false);
            m_authorAtom.addChangeListener(this);
        }

        if (m_titleAtom == null) {
            m_titleAtom = new TextFieldMetaInfoAtom(MetadataItemType.TITLE, "legacy-title", defaultTitleName, false);
            m_titleAtom.addChangeListener(this);
        } else if (MetaInfoFile.NO_TITLE_PLACEHOLDER_TEXT.equals(m_titleAtom.getValue())) {
            m_titleAtom.setValue(null);
        }

        if (m_descriptionAtom == null) {
            m_descriptionAtom = new TextAreaMetaInfoAtom(MetadataXML.DESCRIPTION_LABEL, null, false);
            m_descriptionAtom.addChangeListener(this);
        } else if (MetaInfoFile.NO_DESCRIPTION_PLACEHOLDER_TEXT.equals(m_descriptionAtom.getValue())) {
            m_descriptionAtom.setValue(null);
        }

        if (m_licenseAtom == null) {
            m_licenseAtom =
                new ComboBoxMetaInfoAtom("legacy-license", LicenseType.DEFAULT_LICENSE_NAME, false);
            m_licenseAtom.addChangeListener(this);
        }

        if (m_creationDateAtom == null) {
            m_creationDateAtom = new DateMetaInfoAtom(MetadataXML.CREATION_DATE_LABEL, Calendar.getInstance(), false);
            m_creationDateAtom.addChangeListener(this);
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
        return m_editStateIsDirty.get() || containedMetadataIsDirty();
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
     * @throws MalformedURLException if the URL is null or is invalid
     */
    public MetaInfoAtom addLink(final String url, final String title, final String type)
            throws MalformedURLException {
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
     * Subclasses can override this to affect dirty state decisions.
     *
     * @return true if the metadata specific to the subclass is dirty - not the metadata represented in this
     *              class.
     */
    protected boolean containedMetadataIsDirty() {
        return false;
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
                    m_logger.warn("Could not find tag [" + deletedAtom.getValue() + "] for removal.");
                }
                break;
            case LINK:
                if (m_linkAtoms.remove(deletedAtom)) {
                    m_editStateIsDirty.set(true);
                    m_linkWasDeletedDuringEdit.set(true);
                    itemRemoved = true;
                } else {
                    m_logger.warn("Could not find link [" + deletedAtom.getValue() + "] for removal.");
                }
                break;
            default:
                m_logger.error("Info atom of type " + deletedAtom.getType()
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

        if (containedMetadataIsDirty()) {
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
}
