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
 *   Apr 30, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.workflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.ui.workflow.metadata.MetaInfoFile;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A handler expecting XML conforming to the DTD(?) associated with the namespace http://www.knime.org/2.9/metainfo
 *
 * This DTD (which i haven't been able to find,) needs be augmented for new requirements of UI introduced
 *  in the requiements of AP-11628.
 *
 * @author loki der quaeler
 */
public class SAXInputHandler extends DefaultHandler {

    private static final Set<String> UNIVERSAL_ATTRIBUTES;

    static {
        UNIVERSAL_ATTRIBUTES = new HashSet<>();
        UNIVERSAL_ATTRIBUTES.add(MetadataXML.FORM);
        UNIVERSAL_ATTRIBUTES.add(MetadataXML.NAME);
        UNIVERSAL_ATTRIBUTES.add(MetadataXML.READ_ONLY);
        UNIVERSAL_ATTRIBUTES.add(MetadataXML.TYPE);
    }


    private final StringBuilder m_elementContent = new StringBuilder();

    private final MetadataModelFacilitator m_modelFacilitator = new MetadataModelFacilitator();

    @SuppressWarnings("unused")
    private String m_currentFormValue;
    private String m_currentLabelValue;
    private String m_currentTypeValue;
    private Map<String, String> m_currentOtherAttributes;
    private boolean m_currentElementIsReadOnly;

    private boolean m_currentlyParsingElement = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        if (m_currentlyParsingElement) {
            m_elementContent.append(ch, start, length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName, final String name,
            final Attributes attributes) throws SAXException {
        if (localName.equals(MetadataXML.ATOM_ELEMENT)) {
            m_currentFormValue = attributes.getValue(MetadataXML.FORM);
            m_currentLabelValue = attributes.getValue(MetadataXML.NAME);
            m_currentTypeValue = attributes.getValue(MetadataXML.TYPE);
            m_currentElementIsReadOnly = Boolean.valueOf(attributes.getValue(MetadataXML.READ_ONLY));

            populateOtherAttributeMap(attributes);

            m_currentlyParsingElement = true;
        } else if (localName.equals(MetadataXML.METADATA_ELEMENT)) {
            final String versionString = attributes.getValue(MetadataXML.METADATA_VERSION);
            final int version;
            if (versionString != null) {
                version = Integer.parseInt(versionString);
            } else {
                version = MetaInfoFile.METADATA_NO_VERSION;
            }

            m_modelFacilitator.setMetadataVersion(version);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName, final String name)
            throws SAXException {
        if (localName.equals(MetadataXML.ATOM_ELEMENT)) {
            m_modelFacilitator.processElement(m_currentLabelValue, m_currentTypeValue, m_elementContent.toString(),
                m_currentElementIsReadOnly, m_currentOtherAttributes);

            m_currentFormValue = null;
            m_currentLabelValue = null;
            m_currentTypeValue = null;
            m_currentOtherAttributes = null;

            m_currentlyParsingElement = false;

            m_elementContent.delete(0, m_elementContent.length());
        }
    }

    /**
     * @return the model facilitator - if this is invoked mid-parse, it will only represent the elements which had their
     *         closing tags consumed; if parsing has finished, the consumer should invoke
     *         {@link MetadataModelFacilitator#parsingHasFinishedForWorkflowWithFilename(String)} appropriately.
     */
    MetadataModelFacilitator getModelFacilitator() {
        return m_modelFacilitator;
    }

    private void populateOtherAttributeMap(final Attributes attributes) {
        m_currentOtherAttributes = new HashMap<>();

        final int count = attributes.getLength();
        for (int i = 0; i < count; i++) {
            final String attribute = attributes.getQName(i);
            if (!UNIVERSAL_ATTRIBUTES.contains(attribute)) {
                m_currentOtherAttributes.put(attribute, attributes.getValue(attribute));
            }
        }
    }
}
