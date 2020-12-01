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

import java.util.Calendar;
import java.util.EnumSet;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;

/**
 * This is the view that supports component metadata viewing and editing when the component is open in its own
 * workflow editor.
 *
 * @author loki der quaeler
 */
public class ComponentMetaView extends AbstractMetaView {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ComponentMetaView.class);


    private SubNodeContainer m_currentSubNodeContainer;
    private boolean m_currentWriteProtectionState;

    private Composite m_editUpperComposite;
    private Composite m_displayUpperComposite;

    private Composite m_editLowerComposite;
    private Composite m_displayLowerComposite;

    /**
     * @param parent
     */
    public ComponentMetaView(final Composite parent) {
        super(parent, EnumSet.of(HiddenSection.TITLE, HiddenSection.TAGS, HiddenSection.LINKS, HiddenSection.LICENSE,
            HiddenSection.CREATION_DATE, HiddenSection.AUTHOR));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateUpperSection(final Composite upperComposite) {
        GridLayout gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 4;
        gl.marginTop = 5;
        gl.marginBottom = 8;
        gl.marginWidth = 0;
        upperComposite.setLayout(gl);


        final Label l = new Label(upperComposite, SWT.NONE);
        l.setText("Component Icon");
        l.setFont(BOLD_CONTENT_FONT);
        l.setForeground(SECTION_LABEL_TEXT_COLOR);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);


        m_editUpperComposite = new Composite(upperComposite, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 3;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        m_editUpperComposite.setLayout(gl);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        m_editUpperComposite.setLayoutData(gd);


        m_displayUpperComposite = new Composite(upperComposite, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 3;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        m_displayUpperComposite.setLayout(gl);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        m_displayUpperComposite.setLayoutData(gd);


        SWTUtilities.spaceReclaimingSetVisible(m_editUpperComposite, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateLowerSection(final Composite lowerComposite) {
        GridLayout gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 8;
        gl.marginWidth = 0;
        lowerComposite.setLayout(gl);

        final Label l = new Label(lowerComposite, SWT.NONE);
        l.setText("Ports");
        l.setFont(BOLD_CONTENT_FONT);
        l.setForeground(SECTION_LABEL_TEXT_COLOR);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);


        m_editLowerComposite = new Composite(lowerComposite, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 3;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        m_editLowerComposite.setLayout(gl);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        m_editLowerComposite.setLayoutData(gd);


        m_displayLowerComposite = new Composite(lowerComposite, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 3;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        m_displayLowerComposite.setLayout(gl);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        m_displayLowerComposite.setLayoutData(gd);
    }

    @Override
    protected void updateLocalDisplay() {
        if (inEditMode()) {
            SWTUtilities.spaceReclaimingSetVisible(m_displayUpperComposite, false);
            SWTUtilities.spaceReclaimingSetVisible(m_editUpperComposite, true);

            SWTUtilities.spaceReclaimingSetVisible(m_displayLowerComposite, false);
            SWTUtilities.spaceReclaimingSetVisible(m_editLowerComposite, true);
        } else {
            SWTUtilities.spaceReclaimingSetVisible(m_editUpperComposite, false);
            SWTUtilities.spaceReclaimingSetVisible(m_displayUpperComposite, true);

            SWTUtilities.spaceReclaimingSetVisible(m_editLowerComposite, false);
            SWTUtilities.spaceReclaimingSetVisible(m_displayLowerComposite, true);
        }
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
        if (!hasComponentChanged(subNodeContainer)) {
            // We should still regenerate the port display since the name and description may have changed behind
            //  our backs by the user editing the virtual in and output nodes' configurations directly.
            ((ComponentMetadataModelFacilitator)m_modelFacilitator).populatePortDisplay();
            return;
        }

        m_modelFacilitator = new ComponentMetadataModelFacilitator(subNodeContainer);

        SWTUtilities.removeAllChildren(m_editUpperComposite);
        SWTUtilities.removeAllChildren(m_displayUpperComposite);
        SWTUtilities.removeAllChildren(m_editLowerComposite);
        SWTUtilities.removeAllChildren(m_displayLowerComposite);
        final ComponentMetadataModelFacilitator cmmf = (ComponentMetadataModelFacilitator)m_modelFacilitator;
        cmmf.createUIAtomsForEdit(m_editUpperComposite, m_editLowerComposite);
        cmmf.createUIAtomsForDisplay(m_displayUpperComposite, m_displayLowerComposite);

        m_currentSubNodeContainer = subNodeContainer;

        // TODO waiting on API to fetch metadata (AP-12986)
        m_currentAssetName = subNodeContainer.getName();    // TODO getMetadataTitle or similar...
        currentAssetNameHasChanged();

        m_modelFacilitator.parsingHasFinishedWithDefaultTitleName(m_currentAssetName, Calendar::getInstance);
        m_modelFacilitator.setModelObserver(this);

        m_currentWriteProtectionState = m_currentSubNodeContainer.isWriteProtected();
        m_metadataCanBeEdited.set(!m_currentWriteProtectionState);
        configureFloatingHeaderBarButtons();

        getDisplay().asyncExec(() -> {
            if (!isDisposed()) {
                updateDisplay();
            }
        });
    }

    private boolean hasComponentChanged(final SubNodeContainer newSnc) {
        if (m_currentSubNodeContainer != newSnc) {
            return true;
        } else if (m_currentWriteProtectionState != newSnc.isWriteProtected()) {
            return true;
        } else if (!m_currentAssetName.equals(newSnc.getName())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void completeSave() { }
}
