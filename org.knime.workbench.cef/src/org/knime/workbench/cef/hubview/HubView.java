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
 *   Apr 14, 2020 (loki): created
 */
package org.knime.workbench.cef.hubview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.knime.workbench.core.util.ImageRepository;

/**
 * As part of https://knime-com.atlassian.net/browse/AP-13382 we have this view as sanity check to make sure
 *  the Equo / Make Technology Chromium CEF bundles are working correctly; this requires the same-issue-branch
 *  of knime-sdk-setup (which cites the appropriate p2 sites in the KNIME-AP-complete-internal target.
 *
 * @author loki der quaeler
 * @author Emiliano Recofsky (Equo)
 */
public class HubView extends ViewPart {

    private static final String PLUGIN_ID = "org.knime.workbench.cef";

    private static final String HOME_URL = "https://hub.knime.com/";

    /**
     * The fill color for the header bar
     */
    public static final Color GENERAL_FILL_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 240, 240, 242);

    private Browser m_browser;
    private ToolItem m_backButton;
    private ToolItem m_forwardButton;

    final ProgressAdapter browserProgressListener = new ProgressAdapter () {
        @Override
        public void completed(final ProgressEvent event) {
            m_backButton.setEnabled(m_browser.isBackEnabled());
            m_forwardButton.setEnabled(m_browser.isForwardEnabled());
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        createButtons(parent);
        m_browser = new Browser(parent, SWT.NONE);
        m_browser.setUrl(HOME_URL);
        m_browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        m_browser.addProgressListener(browserProgressListener);
    }

    /**
     *
     * @param parent
     */
    public void createButtons(final Composite parent) {
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        parent.setLayout(layout);
        Composite compositeNavBar = new Composite(parent, SWT.NONE);
        compositeNavBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = true;
        compositeNavBar.setLayout(layout);
        compositeNavBar.setBackground(GENERAL_FILL_COLOR);
        ToolBar toolbarBegin = new ToolBar(compositeNavBar,SWT.HORIZONTAL);
        toolbarBegin.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
        toolbarBegin.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
        Image homeIcon = ImageRepository.getImage(PLUGIN_ID, "/icons/ap-hub-home.png");
        ToolItem homeButton = new ToolItem(toolbarBegin, SWT.PUSH);
        homeButton.setImage(homeIcon);
        homeButton.setToolTipText("Home");
        Image backIcon = ImageRepository.getImage(PLUGIN_ID, "/icons/ap-hub-back.png");
        m_backButton = new ToolItem(toolbarBegin, SWT.PUSH);
        m_backButton.setImage(backIcon);
        m_backButton.setToolTipText("Back");
        m_backButton.setEnabled(false);
        Image forwardIcon = ImageRepository.getImage(PLUGIN_ID, "/icons/ap-hub-forward.png");
        m_forwardButton = new ToolItem(toolbarBegin, SWT.PUSH);
        m_forwardButton.setImage(forwardIcon);
        m_forwardButton.setToolTipText("Forward");
        m_forwardButton.setEnabled(false);
        Image refreshIcon = ImageRepository.getImage(PLUGIN_ID, "/icons/ap-hub-reload.png");
        ToolItem reloadButton = new ToolItem(toolbarBegin, SWT.PUSH);
        reloadButton.setImage(refreshIcon);
        reloadButton.setToolTipText("Reload");
        ToolBar toolbarEnd = new ToolBar(compositeNavBar, SWT.HORIZONTAL);
        toolbarEnd.setCursor(Display.getDefault().getSystemCursor(SWT.CURSOR_HAND));
        toolbarEnd.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        Image openIcon = ImageRepository.getImage(PLUGIN_ID, "/icons/ap-hub-open-in-browser.png");
        ToolItem openButton = new ToolItem(toolbarEnd, SWT.PUSH);
        openButton.setImage(openIcon);
        openButton.setToolTipText("Open in browser");

        homeButton.addListener(SWT.Selection, event -> {
            m_browser.setUrl(HOME_URL);
        });
        m_backButton.addListener(SWT.Selection, event -> {
            m_browser.back();
        });
        m_forwardButton.addListener(SWT.Selection, event -> {
            m_browser.forward();
        });
        reloadButton.addListener(SWT.Selection, event -> {
            m_browser.refresh();
        });
        openButton.addListener(SWT.Selection, event -> {
            Program.launch(m_browser.getUrl());
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        if (m_browser != null) {
            m_browser.dispose();
        }

        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() { }
}
