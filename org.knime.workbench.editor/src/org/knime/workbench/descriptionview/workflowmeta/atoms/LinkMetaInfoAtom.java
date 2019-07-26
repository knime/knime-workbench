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
package org.knime.workbench.descriptionview.workflowmeta.atoms;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.descriptionview.workflowmeta.PlatformSpecificUIisms;
import org.knime.workbench.descriptionview.workflowmeta.WorkflowMetaView;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.knime.workbench.ui.workflow.metadata.MetadataXML;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * The atom representing (hyper)links.
 *
 * @author loki der quaeler
 */
public class LinkMetaInfoAtom extends MetaInfoAtom {
    private static final String BLACK_CIRCLE;
    private static final Color BULLET_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 68, 61, 65);

    private static final String HTTP_PREFIX = "http://";

    static {
        final Optional<Object> o = PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.BLACK_CIRCLE_UNICODE_DETAIL);

        if (o.isPresent()) {
            BLACK_CIRCLE = (String)o.get();
        } else {
            BLACK_CIRCLE = "\u2022";
        }
    }


    private String m_linkType;
    private String m_linkURL;

    private String m_displayText;

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     * @param otherAttributes a map containing attributes we associate with links (type, url, ...)
     * @throws MalformedURLException if the URL is null or is invalid
     */
    public LinkMetaInfoAtom(final String label, final String value, final boolean readOnly,
        final Map<String, String> otherAttributes)
            throws MalformedURLException {
        this(label, value, ((otherAttributes != null) ? otherAttributes.get(MetadataXML.URL_TYPE_ATTRIBUTE) : null),
            ((otherAttributes != null) ? otherAttributes.get(MetadataXML.URL_URL_ATTRIBUTE) : null), readOnly);
    }

    /**
     * @param label the label displayed with the value of this atom in some UI widget; this is historical and unused.
     * @param value the displayed value of this atom; if this is null or empty it will attempt to be derived from the
     *            url if that is non-null, non-empty
     * @param type the type of the link of this atom.
     * @param url the url the link of this atom; if this has no scheme prefix, it will be prefixed with the class static
     *            variable <code>HTTP_PREFIX</code>
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     * @throws MalformedURLException if the URL is null or is invalid
     */
    public LinkMetaInfoAtom(final String label, final String value, final String type, final String url,
        final boolean readOnly)
                throws MalformedURLException {
        super(MetadataItemType.LINK, label, value, readOnly);

        String urlToUse = url;
        if (url == null) {
            throw new MalformedURLException("URL cannot be null.");
        }

        int index = url.indexOf("//");
        if (index == -1) {
            urlToUse = HTTP_PREFIX + url;
        }

        @SuppressWarnings("unused") // only used to validate the URL
        final URL u = new URL(urlToUse);

        m_linkType = type;
        m_linkURL = urlToUse;

        if (((m_value == null) || (m_value.trim().length() == 0)) && (m_linkURL != null)) {
            index = m_linkURL.indexOf("//");

            if (index != -1) {
                m_value = m_linkURL.substring(index + 2);
            }
        }

        updateDisplayText();
    }

    private void updateDisplayText() {
        m_displayText = m_value + (((m_linkType != null) && !m_linkType.isEmpty()) ? " (" + m_linkType + ")" : "");
    }

    /**
     * This kludge generates the link-line that gets crammed into description blocks for the older version of
     *  storing metadata. This will become obsolete when we move over to newer XML storage.
     *
     * @param urlKeywordLinkTypeName
     * @return a string to be stuck in the description block
     */
    public String getOldStyleDescriptionRepresentation(final String urlKeywordLinkTypeName) {
        final StringBuilder sb = new StringBuilder();
        final boolean urlMissing = (m_linkURL == null) || (m_linkURL.trim().length() == 0);
        final boolean titleMissing = (m_value == null) || (m_value.trim().length() == 0);

        if (urlKeywordLinkTypeName.equals(m_linkType) || (m_linkType == null) || (m_linkType.trim().length() == 0)) {
            sb.append("URL");
        } else {
            sb.append(m_linkType.toUpperCase());
        }
        sb.append(": ");

        if (titleMissing) {
            sb.append("A link");
        } else {
            sb.append(m_value);
        }
        sb.append(' ');

        if (urlMissing) {
            sb.append("None");
        } else {
            sb.append(m_linkURL);
        }

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForDisplay(final Composite parent) {
        populateContainer(parent, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void populateContainerForEdit(final Composite parent) {
        populateContainer(parent, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final TransformerHandler parentElement) throws SAXException {
        if (hasContent()) {
            save(parentElement, MetadataXML.URL);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAdditionalSaveTimeAttributes(final AttributesImpl attributes) {
        attributes.addAttribute(null, null, MetadataXML.URL_TYPE_ATTRIBUTE, "CDATA", m_linkType);
        attributes.addAttribute(null, null, MetadataXML.URL_URL_ATTRIBUTE, "CDATA", m_linkURL);
    }

    private void populateContainer(final Composite parent, final boolean isEdit) {
        final Composite container = new Composite(parent, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 22;
        container.setLayoutData(gd);
        final GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        gl.horizontalSpacing = 3;
        container.setLayout(gl);

        final Label bullet = new Label(container, SWT.LEFT);
        bullet.setText(BLACK_CIRCLE);
        bullet.setFont(WorkflowMetaView.BOLD_CONTENT_FONT);
        bullet.setForeground(BULLET_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.TOP;
        gd.heightHint = 18;
        bullet.setLayoutData(gd);

        final HyperLinkLabel hll = new HyperLinkLabel(container, isEdit, m_displayText, m_linkURL);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        hll.setLayoutData(gd);
        hll.addDisposeListener((event) -> {
            container.dispose();
        });
    }
}
