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
 *   Nov 1, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import java.net.MalformedURLException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.workbench.descriptionview.metadata.AbstractMetadataModelFacilitator;
import org.knime.workbench.descriptionview.metadata.LicenseType;
import org.knime.workbench.descriptionview.metadata.atoms.ComboBoxMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.DateMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.LinkMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TagMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextAreaMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextFieldMetaInfoAtom;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;

/**
 * This is the concrete subclass of {@code AbstractMetadataModelFacilitator} which knows how to consume metadata
 * from a {@link SubNodeContainer}.
 *
 * @author loki der quaeler
 */
class ComponentMetadataModelFacilitator extends AbstractMetadataModelFacilitator {
    private final SubNodeContainer m_subNodeContainer;

    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class

    // for development only
    private final MetadataMockProvider m_mockProvider;

    ComponentMetadataModelFacilitator(final SubNodeContainer snc) {
        super();

        m_subNodeContainer = snc;

        m_mockProvider = new MetadataMockProvider(m_subNodeContainer);

        for (final String tag : m_mockProvider.getTags()) {
            addTag(tag);
        }

        for (final String[] link : m_mockProvider.getLinkObjects()) {
            try {
                addLink(link[2], link[0], link[1]);
            } catch (final MalformedURLException e) {
                m_logger.error("Could not parse incoming URL [" + link[2] + "]", e);
            }
        }

        m_titleAtom =
            new TextFieldMetaInfoAtom(MetadataItemType.TITLE, "legacy-title", m_mockProvider.getTitle(), false);
        m_titleAtom.addChangeListener(this);

        m_authorAtom =
            new TextFieldMetaInfoAtom(MetadataItemType.AUTHOR, "legacy-author", m_mockProvider.getAuthor(), false);
        m_authorAtom.addChangeListener(this);

        m_descriptionAtom = new TextAreaMetaInfoAtom("legacy-description", m_mockProvider.getDescription(), false);
        m_descriptionAtom.addChangeListener(this);

        m_creationDateAtom = new DateMetaInfoAtom("legacy-creation-date", m_mockProvider.getDate(), false);
        m_creationDateAtom.addChangeListener(this);

        m_licenseAtom = new ComboBoxMetaInfoAtom("legacy-license", m_mockProvider.getLicense(), false);
        m_licenseAtom.addChangeListener(this);

        // TODO color and icon - AP-12984
    }

    // TODO waiting on API to set metadata (AP-12986)
    void storeMetadataInComponent() {
        final String[] tags = new String[m_tagAtoms.size()];
        int index = 0;
        for (final TagMetaInfoAtom tag : m_tagAtoms) {
            tags[index++] = tag.getValue();
        }
        m_mockProvider.setTags(tags);

        index = 0;
        final String[][] links = new String[m_linkAtoms.size()][];
        for (final LinkMetaInfoAtom link : m_linkAtoms) {
            links[index] = new String[3];
            links[index][0] = link.getValue();
            links[index][1] = link.getLinkType();
            links[index++][2] = link.getURL();
        }
        m_mockProvider.setLinkObjects(links);

        m_mockProvider.setTitle(m_titleAtom.getValue());
        m_mockProvider.setAuthor(m_authorAtom.getValue());
        m_mockProvider.setDescription(m_descriptionAtom.getValue());
        m_mockProvider.setDate(m_creationDateAtom.getValue());
        m_mockProvider.setLicense(m_licenseAtom.getValue());

        // TODO color and icon - AP-12984
    }


    /*
     * Just for development... prior to AP-12986, this will return made up data; after AP-12986, during development
     *  this will be a proxy to SubNodeContainer and prior to final PR this class will go away.
     */
    @SuppressWarnings("static-method")
    private static class MetadataMockProvider {
        @SuppressWarnings("unused")
        private final SubNodeContainer m_subNodeContainer;

        private MetadataMockProvider(final SubNodeContainer snc) {
            m_subNodeContainer = snc;
        }

        private String[] getTags() {
            return new String[] {"cool component", "db", "complex", "deep learning"};
        }
        private void setTags(final String[] tags) {
            // TODO
        }

        // unclear what, if any object, SNC will hand over for a "link object".. this will be mocked up to
        //      return a triplet {link text, link type, url}
        private String[][] getLinkObjects() {
            return new String[][] {
              { "KNIME Homepage", "Website", "http://www.knime.org" },
              { "Moe Flanders", "Video", "https://www.youtube.com/watch?v=AWbElkaeqVA" }
            };
        }
        private void setLinkObjects(final String[][] linkObjects) {
            // TODO
        }

        private String getAuthor() {
            return "acamus";
        }
        private void setAuthor(final String author) {
            // TODO
        }

        // this assumes the format will be the long standing metadata precedent for time-dates..
        //      see MetaInfoFile#calendarFromDateString(String)
        private String getDate() {
            return "13/5/2018/10:28:12 +02:00";
        }
        private void setDate(final String date) {
            // TODO
        }

        private String getTitle() {
            return "Tomorrow's Component, Today";
        }
        private void setTitle(final String title) {
            // TODO
        }

        private String getDescription() {
            return "This component does some stuff, and then emits further operations on that stuff.\n\n"
                        + "That being said, at the moment, this is just mocked up metadata.";
        }
        private void setDescription(final String description) {
            // TODO
        }

        private String getLicense() {
            return LicenseType.DEFAULT_LICENSE_NAME;
        }
        private void setLicense(final String license) {
            // TODO
        }

        @SuppressWarnings("unused")
        private RGB getComponentColor() {
            // TODO
            return null;
        }
        @SuppressWarnings("unused")
        private void setComponentColor(final RGB color) {
            // TODO
        }

        @SuppressWarnings("unused")
        private ImageDescriptor getComponentIcon() {
            // TODO
            return null;
        }
        @SuppressWarnings("unused")
        private void setComponentIcon(final ImageDescriptor icon) {
            // TODO
        }
    }
}
