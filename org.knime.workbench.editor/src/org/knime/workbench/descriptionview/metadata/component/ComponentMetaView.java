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
package org.knime.workbench.descriptionview.metadata.component;

import java.util.Objects;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.workbench.descriptionview.FallbackBrowser;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;

/**
 * This is the view that supports component metadata viewing and editing when the component is open in its own
 * workflow editor.
 *
 * @author loki der quaeler
 */
public class ComponentMetaView extends AbstractMetaView {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ComponentMetaView.class);


    private Browser m_browser;

    private FallbackBrowser m_text;

    private boolean m_isFallback;

    private SubNodeContainer m_currentSubNodeContainer;

    /**
     * @param parent
     */
    public ComponentMetaView(final Composite parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean populateUpperSection(final Composite upperComposite) {
        // TODO this is AP-12984 - providing color chooser and icon drop
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean populateLowerSection(final Composite lowerComposite) {
        if (m_browser == null) {
            lowerComposite.setLayout(new FillLayout());

            try {
                m_text = null;
                m_browser = new Browser(lowerComposite, SWT.NONE);
                m_browser.setText("");
                m_isFallback = false;
            } catch (final SWTError e) {
                LOGGER.warn("No html browser for node description available.", e);
                m_browser = null;
                m_text = new FallbackBrowser(lowerComposite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
                m_isFallback = true;
            }
        }

        return true;
    }

    @Override
    protected void updateDisplay() {
        super.updateDisplay();

        /*
         * TODO
         *   need to populate the browser, for that we need:
         *      . add something to SNC to generate XML for just the port descriptions
         *      . created an HTML document like is done in DynamicNodeDescriptionCreator
         *      . will NodeFactoryHTMLCreator.instance.readFullDescription do the right thing with the port-only XML?
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IStructuredSelection selection) {
        final Object o = selection.getFirstElement();

        if (!(o instanceof SubNodeContainer)) {
            LOGGER.error("We were expecting an instance of SubNodeContainer but instead got " + o);

            return;
        }

        final SubNodeContainer subNodeContainer = (SubNodeContainer)o;
        if (Objects.equals(m_currentSubNodeContainer, subNodeContainer)) {
            return;
        }

        m_modelFacilitator = new ComponentMetadataModelFacilitator(subNodeContainer);
        m_currentSubNodeContainer = subNodeContainer;

        // TODO waiting on API to fetch metadata (AP-12986)
        m_currentAssetName = subNodeContainer.getName();    // TODO getMetadataTitle or similar...
        currentAssetNameHasChanged();

        // Is there ever a case where it cannot be?
        m_metadataCanBeEdited.set(true);

        getDisplay().asyncExec(() -> {
            if (!isDisposed()) {
                updateDisplay();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void completeSave() {
        ((ComponentMetadataModelFacilitator)m_modelFacilitator).storeMetadataInComponent();
    }
}
