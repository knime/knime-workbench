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
 *   Nov 16, 2015 (albrecht): created
 */
package org.knime.workbench.editor2.subnode;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JRootPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.dialog.util.ConfigurationLayoutUtil;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.ViewHideable;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.wizard.util.LayoutUtil;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.SubnodeContainerConfigurationStringProvider;
import org.knime.core.node.workflow.SubnodeContainerLayoutStringProvider;
import org.knime.core.node.workflow.WorkflowLock;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.JavaScriptViewCreator;
import org.knime.js.core.layout.DefaultConfigurationCreatorImpl;
import org.knime.js.core.layout.DefaultLayoutCreatorImpl;
import org.knime.js.core.layout.bs.JSONLayoutColumn;
import org.knime.js.core.layout.bs.JSONLayoutConfigurationContent;
import org.knime.js.core.layout.bs.JSONLayoutContent;
import org.knime.js.core.layout.bs.JSONLayoutPage;
import org.knime.js.core.layout.bs.JSONLayoutRow;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;
import org.knime.js.core.layout.bs.JSONNestedLayout;
import org.knime.js.core.webtemplate.WebTemplateUtil;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public final class SubnodeLayoutJSONEditorPage extends WizardPage {

    private static NodeLogger LOGGER = NodeLogger.getLogger(SubnodeLayoutJSONEditorPage.class);

    private SubNodeContainer m_subNodeContainer;
    private WorkflowManager m_wfManager;
    private Map<NodeIDSuffix, SingleNodeContainer> m_viewNodes;
    @SuppressWarnings("rawtypes")
    private Map<NodeIDSuffix, DialogNode> m_dialogNodes;
    private SubnodeContainerLayoutStringProvider m_subnodeLayoutStringProvider;
    private SubnodeContainerConfigurationStringProvider m_subnodeConfigurationStringProvider;
    private Label m_statusLine;
    private RSyntaxTextArea m_textArea;
    private int m_caretPosition = 5;
    private Text m_text;
    private List<Integer> m_documentNodeIDs = new ArrayList<Integer>();
    private NodeUsageComposite m_nodeUsageComposite;
    private Browser m_browser;
    private Browser m_configurationBrowser;
    private BrowserFunction m_visualLayoutUpdate;
    private BrowserFunction m_configurationLayoutUpdate;

    /**
     * Crates a new page instance with a given page name
     *
     * @param pageName the page name
     */
    protected SubnodeLayoutJSONEditorPage(final String pageName) {
        super(pageName);
        setDescription("Define a layout for the KNIME WebPortal and the composite view.\n"
            + "Specify the order of the contained configuration nodes for the configuration dialog of the component.");
        m_subnodeLayoutStringProvider = new SubnodeContainerLayoutStringProvider("");
        m_subnodeConfigurationStringProvider = new SubnodeContainerConfigurationStringProvider("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        TabFolder tabs = new TabFolder(parent, SWT.BORDER);

        TabItem usageTab = new TabItem(tabs, SWT.NONE);
        usageTab.setText("Node Usage");
        m_nodeUsageComposite = new NodeUsageComposite(tabs, m_viewNodes, m_subNodeContainer);
        usageTab.setControl(m_nodeUsageComposite);

        TabItem visualTab = new TabItem(tabs, SWT.NONE);
        visualTab.setText("Composite View Layout");

        TabItem jsonTab = new TabItem(tabs, SWT.NONE);
        jsonTab.setText("Advanced Composite View Layout");
        jsonTab.setControl(createJSONEditorComposite(tabs));

        TabItem configurationTab = new TabItem(tabs, SWT.NONE);
        configurationTab.setText("Configuration Dialog Layout");
        configurationTab.setControl(createConfigurationLayoutComposite(tabs));

        // The visual layout tab should be the second tab, but its control should be made
        // after the Advanced tab. This ensures that the JSON document is created before
        // it is used in the creation of the visual layout tab
        visualTab.setControl(createVisualLayoutComposite(tabs));

        tabs.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                final String tabText = tabs.getSelection()[0].getText();
                applyUsageChanges();

                // clean JSON
                final ObjectMapper mapper = createObjectMapperForUpdating();
                final ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
                final ObjectReader configurationReader = mapper.readerForUpdating(new JSONLayoutPage());
                try {
                    JSONLayoutPage page = reader.readValue(m_subnodeLayoutStringProvider.getLayoutString());
                    cleanJSONPage(page);
                    m_subnodeLayoutStringProvider
                        .setLayoutString(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page));
                    JSONLayoutPage configurationPage =
                        configurationReader.readValue(m_subnodeConfigurationStringProvider.getConfigurationLayoutString());
                    m_subnodeConfigurationStringProvider.setConfigurationLayoutString(
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(configurationPage));
                } catch (IOException ex) {
                    LOGGER.error("Failed to retrieve JSON string from layout:" + ex.getMessage(), ex);
                }

                switch (tabText) {
                    case "Composite View Layout":
                        updateVisualLayout();
                        break;
                    case "Advanced Composite View Layout":
                        updateJsonTextArea();
                        break;
                    case "Configuration Dialog Layout":
                        updateConfigurationLayout();
                        break;
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                // Do nothing
            }
        });

        tabs.setSelection(1);
        setControl(tabs);
    }

    boolean applyUsageChanges() {
        try (WorkflowLock lock = m_subNodeContainer.lock()) { // each node will cause lock acquisition, do it as bulk
            for (Entry<NodeID, Button> wUsage : m_nodeUsageComposite.getWizardUsageMap().entrySet()) {
                NodeID id = wUsage.getKey();
                boolean hide = !wUsage.getValue().getSelection();
                try {
                    m_subNodeContainer.setHideNodeFromWizard(id, hide);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unable to set hide in wizard flag on node: " + e.getMessage(), e);
                    return false;
                }
            }

            for (Entry<NodeID, Button> dUsage : m_nodeUsageComposite.getDialogUsageMap().entrySet()) {
                NodeID id = dUsage.getKey();
                boolean hide = !dUsage.getValue().getSelection();
                try {
                    m_subNodeContainer.setHideNodeFromDialog(id, hide);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unable to set hide in dialog flag on node: " + e.getMessage(), e);
                    return false;
                }
            }
        }

        return true;
    }

    private Composite createVisualLayoutComposite(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Web resources
        final WebTemplate template = WebTemplateUtil.getWebTemplateFromBundleID("knimeLayoutEditor_1.0.0");
        final WebTemplate dT = WebTemplateUtil.getWebTemplateFromBundleID("knimeLayoutEditor_1.0.0_Debug");
        VisualLayoutViewCreator creator = new VisualLayoutViewCreator(template, dT);
        String html = "";
        try {
            html = creator.createWebResources("visual layout editor", null, null, "");
        } catch (final IOException e) {
            LOGGER.error("Cannot create visual layout editor", e);
        }

        // Create browser
        m_browser = new Browser(composite, SWT.NONE);

        try {
            m_browser.setUrl(new File(html).toURI().toURL().toString());
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
        }
        m_browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Create JSON Objects
        final List<VisualLayoutEditorJSONNode> nodes = createJSONNodeList();
        // ensure node layout is written the same as the metanode layout
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        String jsonNodes = "";
        try {
            jsonNodes = mapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot write JSON: " + e.getMessage(), e);
        }
        // variables in progress listener must be final
        final String jsonLayoutConst = getJsonDocument();
        final String jsonNodesConst = jsonNodes;
        m_browser.addProgressListener(new ProgressListener() {

            @Override
            public void changed(final ProgressEvent event) {
                // do nothing
            }

            @Override
            public void completed(final ProgressEvent event) {
                m_browser.evaluate("setNodes(\'" + jsonNodesConst + "\');");
                m_browser.evaluate("setLayout(\'" + jsonLayoutConst + "\');");
            }
        });
        m_visualLayoutUpdate = new UpdateLayoutFunction(m_browser, "pushLayout");
        return composite;
    }

    private Composite createConfigurationLayoutComposite(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Web resources
        final WebTemplate template =
            WebTemplateUtil.getWebTemplateFromBundleID("knimeConfigurationLayoutEditor_1.0.0");
        final WebTemplate dT =
            WebTemplateUtil.getWebTemplateFromBundleID("knimeConfigurationLayoutEditor_1.0.0_Debug");
        VisualLayoutViewCreator creator = new VisualLayoutViewCreator(template, dT);
        String html = "";
        try {
            html = creator.createWebResources("configuration layout editor", null, null, "");
        } catch (final IOException e) {
            LOGGER.error("Cannot create configuration layout editor", e);
        }

        // Create browser
        m_configurationBrowser = new Browser(composite, SWT.NONE);

        try {
            m_configurationBrowser.setUrl(new File(html).toURI().toURL().toString());
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
        }
        m_configurationBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Create JSON Objects
        final List<ConfigurationLayoutEditorJSONNode> nodes = createJSONConfigurationNodeList();
        // ensure node layout is written the same as the metanode layout
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        String jsonNodes = "";
        try {
            jsonNodes = mapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot write JSON: " + e.getMessage(), e);
        }
        // variables in progress listener must be final
        final String jsonLayoutConst = getJsonConfigurationDocument();
        final String jsonNodesConst = jsonNodes;
        m_configurationBrowser.addProgressListener(new ProgressListener() {

            @Override
            public void changed(final ProgressEvent event) {
                // do nothing
            }

            @Override
            public void completed(final ProgressEvent event) {
                m_configurationBrowser.evaluate("setNodes(\'" + jsonNodesConst + "\');");
                m_configurationBrowser.evaluate("setLayout(\'" + jsonLayoutConst + "\');");
            }
        });
        m_configurationLayoutUpdate = new UpdateConfigurationFunction(m_configurationBrowser, "pushLayout");
        return composite;
    }

    private Composite createJSONEditorComposite(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));
        composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

        if (isWindows()) {

            Composite embedComposite = new Composite(composite, SWT.EMBEDDED | SWT.NO_BACKGROUND);
            final GridLayout gridLayout = new GridLayout();
            gridLayout.verticalSpacing = 0;
            gridLayout.marginWidth = 0;
            gridLayout.marginHeight = 0;
            gridLayout.horizontalSpacing = 0;
            embedComposite.setLayout(gridLayout);
            embedComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            Frame frame = SWT_AWT.new_Frame(embedComposite);
            Panel heavyWeightPanel = new Panel();
            heavyWeightPanel.setLayout(new BoxLayout(heavyWeightPanel, BoxLayout.Y_AXIS));
            frame.add(heavyWeightPanel);
            frame.setFocusTraversalKeysEnabled(false);

            // Use JApplet with JRootPane as layer in between heavyweightPanel and RTextScrollPane
            // This reduces flicker on resize in RSyntaxTextArea
            JApplet applet = new JApplet();
            JRootPane root = applet.getRootPane();
            Container contentPane = root.getContentPane();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            heavyWeightPanel.add(applet);

            m_textArea = new RSyntaxTextArea(10, 60);
            m_textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            m_textArea.setCodeFoldingEnabled(true);
            m_textArea.setAntiAliasingEnabled(true);
            RTextScrollPane sp = new RTextScrollPane(m_textArea);
            sp.setDoubleBuffered(true);
            m_textArea.setText(m_subnodeLayoutStringProvider.getLayoutString());
            m_textArea.setEditable(true);
            m_textArea.setEnabled(true);
            contentPane.add(sp);

            Dimension size = sp.getPreferredSize();
            embedComposite.setSize(size.width, size.height);

            // forward focus to RSyntaxTextArea
            embedComposite.addFocusListener(new FocusAdapter() {

                @Override
                public void focusGained(final FocusEvent e) {
                    ViewUtils.runOrInvokeLaterInEDT(new Runnable() {
                        @Override
                        public void run() {
                            m_textArea.requestFocus();
                            m_textArea.setCaretPosition(m_caretPosition);
                        }
                    });
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    // do nothing
                }
            });

            // delete content of status line, when something is inserted or deleted
            m_textArea.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void changedUpdate(final DocumentEvent arg0) {
                    if (!composite.isDisposed()) {
                        composite.getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                m_subnodeLayoutStringProvider.setLayoutString(m_textArea.getText());
                                if (m_statusLine != null && !m_statusLine.isDisposed()) {
                                    m_statusLine.setText("");
                                    isJSONValid();
                                }
                            }
                        });
                    }
                }

                @Override
                public void insertUpdate(final DocumentEvent arg0) {
                    /* do nothing */ }

                @Override
                public void removeUpdate(final DocumentEvent arg0) {
                    /* do nothing */ }

            });

            // remember caret position
            m_textArea.addCaretListener(new CaretListener() {
                @Override
                public void caretUpdate(final CaretEvent arg0) {
                    m_caretPosition = arg0.getDot();
                }
            });

        } else {
            m_text = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
            GridData layoutData = new GridData(GridData.FILL_BOTH);
            layoutData.widthHint = 600;
            layoutData.heightHint = 400;
            m_text.setLayoutData(layoutData);
            m_text.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent e) {
                    m_subnodeLayoutStringProvider.setLayoutString(m_text.getText());
                    if (m_statusLine != null && !m_statusLine.isDisposed()) {
                        m_statusLine.setText("");
                        isJSONValid();
                    }
                }
            });
            m_text.setText(m_subnodeLayoutStringProvider.getLayoutString());
        }

        // add status line
        m_statusLine = new Label(composite, SWT.SHADOW_NONE | SWT.WRAP);
        GridData statusGridData = new GridData(SWT.LEFT | SWT.FILL, SWT.BOTTOM, true, false);
        int maxHeight = new PixelConverter(m_statusLine).convertHeightInCharsToPixels(3);
        statusGridData.heightHint = maxHeight + 5;
        // seems to have no impact on the layout. The height will still be 3 rows (at least on Windows 8)
        statusGridData.minimumHeight = new PixelConverter(m_statusLine).convertHeightInCharsToPixels(1);
        m_statusLine.setLayoutData(statusGridData);
        compareNodeIDs();

        return composite;
    }

    /**
     * Sets all currently available view nodes on this editor page.
     *
     * @param manager the workflow manager
     * @param subnodeContainer the component container
     * @param viewNodes a map of all available view nodes
     */
    public void setNodes(final WorkflowManager manager, final SubNodeContainer subnodeContainer,
        final Map<NodeIDSuffix, SingleNodeContainer> viewNodes) {
        m_wfManager = manager;
        m_subNodeContainer = subnodeContainer;
        m_viewNodes = viewNodes;
        JSONLayoutPage page = null;
        m_subnodeLayoutStringProvider = subnodeContainer.getSubnodeLayoutStringProvider();
        if (!m_subnodeLayoutStringProvider.isEmptyLayout() && !m_subnodeLayoutStringProvider.isPlaceholderLayout()) {
            try {
                ObjectMapper mapper = createObjectMapperForUpdating();
                page = mapper.readValue(m_subnodeLayoutStringProvider.getLayoutString(), JSONLayoutPage.class);
                if (page.getRows() == null) {
                    page = null;
                } else {
                    cleanJSONPage(page);
                    m_subnodeLayoutStringProvider.setLayoutString(mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(page));
                }
            } catch (IOException e) {
                LOGGER.error("Error parsing layout. Pretty printing not possible: " + e.getMessage(), e);
            }
        }
        if (page == null) {
            page = resetLayout();
        }
        try {
            ObjectMapper mapper = createObjectMapperForUpdating();
            m_subnodeLayoutStringProvider.setLayoutString(mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(page));
            LayoutUtil.updateLayout(m_subnodeLayoutStringProvider);
            page = mapper.readValue(m_subnodeLayoutStringProvider.getLayoutString(), JSONLayoutPage.class);
        } catch (IOException e) {
            LOGGER.error("Error updating JSON layout. Pretty printing not possible: " + e.getMessage(), e);
        }
        List<JSONLayoutRow> rows = page.getRows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            JSONLayoutRow row = rows.get(rowIndex);
            populateDocumentNodeIDs(row);
        }
    }

    /**
     * Sets all currently available dialog nodes on the node sorter page.
     *
     * @param manager the workflow manager
     * @param subnodeContainer the component container
     * @param dialogNodes a map of all available view nodes
     */
    public void setConfigurationNodes(final WorkflowManager manager, final SubNodeContainer subnodeContainer,
        @SuppressWarnings("rawtypes") final Map<NodeIDSuffix, DialogNode> dialogNodes) {
        m_wfManager = manager;
        m_subNodeContainer = subnodeContainer;
        m_dialogNodes = dialogNodes;
        JSONLayoutPage page = null;
        m_subnodeConfigurationStringProvider = subnodeContainer.getSubnodeConfigurationLayoutStringProvider();
        if (!m_subnodeConfigurationStringProvider.isEmptyLayout()) {
            try {
                ConfigurationLayoutUtil.addUnreferencedDialogNodes(m_subnodeConfigurationStringProvider, dialogNodes);
                ObjectMapper mapper = createObjectMapperForUpdating();
                page = mapper.readValue(m_subnodeConfigurationStringProvider.getConfigurationLayoutString(),
                    JSONLayoutPage.class);
                if (page.getRows() == null) {
                    page = null;
                } else {
                    cleanJSONPage(page);
                    removeMissingRows(page, dialogNodes);
                    m_subnodeConfigurationStringProvider
                        .setConfigurationLayoutString(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page));
                }
            } catch (IOException e) {
                LOGGER.error("Error parsing layout. Pretty printing not possible: " + e.getMessage(), e);
            }
        }
        if (page == null) {
            page = resetConfigurationLayout();
        }
        try {
            ObjectMapper mapper = createObjectMapperForUpdating();
            m_subnodeConfigurationStringProvider.setConfigurationLayoutString(mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(page));
            ConfigurationLayoutUtil.updateLayout(m_subnodeConfigurationStringProvider);
        } catch (IOException e) {
            LOGGER.error("Error updating JSON layout. Pretty printing not possible: " + e.getMessage(), e);
        }
    }

    /**
     * @param page
     */
    private JSONLayoutPage resetLayout() {
        m_documentNodeIDs.clear();
        return generateInitialJson();
    }

    private JSONLayoutPage generateInitialJson() {
        JSONLayoutPage page = DefaultLayoutCreatorImpl.createDefaultLayoutStructure(m_viewNodes);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        try {
            String initialJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
            m_subnodeLayoutStringProvider.setLayoutString(initialJson);
            return page;
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not create initial layout: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param page
     */
    private JSONLayoutPage resetConfigurationLayout() {
        return generateInitialConfigurationJson();
    }

    private JSONLayoutPage generateInitialConfigurationJson() {
        JSONLayoutPage page = DefaultConfigurationCreatorImpl.createDefaultConfigurationLayoutStructure(m_dialogNodes);
        ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        try {
            String initialJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page);
            m_subnodeConfigurationStringProvider.setConfigurationLayoutString(initialJson);
            return page;
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not create initial layout: " + e.getMessage(), e);
            return null;
        }
    }

    private void populateDocumentNodeIDs(final JSONLayoutContent content) {
        if (content instanceof JSONLayoutRow) {
            JSONLayoutRow row = (JSONLayoutRow)content;
            if (row.getColumns() != null && row.getColumns().size() > 0) {
                for (JSONLayoutColumn col : row.getColumns()) {
                    if (col.getContent() != null && col.getContent().size() > 0) {
                        for (JSONLayoutContent c : col.getContent()) {
                            populateDocumentNodeIDs(c);
                        }
                    }
                }
            }
        } else if (content instanceof JSONLayoutViewContent) {
            String id = ((JSONLayoutViewContent)content).getNodeID();
            if (id != null && !id.isEmpty()) {
                m_documentNodeIDs.add(Integer.parseInt(id));
            }
        } else if (content instanceof JSONNestedLayout) {
            String id = ((JSONNestedLayout)content).getNodeID();
            if (id != null && !id.isEmpty()) {
                m_documentNodeIDs.add(Integer.parseInt(id));
            }
        }
    }

    private static String cleanJSONPage(final JSONLayoutPage page) {
        if (page.getRows() == null) {
            return "";
        }
        String errorMsg = "";
        final List<JSONLayoutRow> cleanedRows = new ArrayList<>();
        boolean emptyRows = false;
        boolean emptyCols = false;
        for(final JSONLayoutRow row : page.getRows()) {
            if (!row.getColumns().isEmpty()) {
                final List<JSONLayoutColumn> cleanedColumns = new ArrayList<>();
                for (final JSONLayoutColumn col : row.getColumns()) {
                    if (col.getContent() != null) {
                        cleanedColumns.add(col);
                    } else {
                        emptyCols = true;
                    }
                }
                if (!cleanedColumns.isEmpty()) {
                    row.setColumns(cleanedColumns);
                    cleanedRows.add(row);
                } else {
                    emptyRows = true;
                }
            } else {
                emptyRows = true;
            }
        }
        if (emptyRows && !cleanedRows.isEmpty()) {
            errorMsg += "Empty row(s) detected, these will be ignored in the layout";
        }
        if (emptyCols && !cleanedRows.isEmpty()) {
            if (!errorMsg.isEmpty()) {
                errorMsg += "\n";
            }
            errorMsg += "Empty column(s) detected, these will be ignored in the layout";
        }
        page.setRows(cleanedRows);
        return errorMsg;
    }

    private static void removeMissingRows(final JSONLayoutPage page,
        @SuppressWarnings("rawtypes") final Map<NodeIDSuffix, DialogNode> allNodes) {
        if (page.getRows() == null) {
            return;
        }
        final List<JSONLayoutRow> cleanedRows = new ArrayList<>();
        for (final JSONLayoutRow row : page.getRows()) {
            if (!row.getColumns().isEmpty()) {
                final List<JSONLayoutColumn> cleanedColumns = new ArrayList<>();
                for (final JSONLayoutColumn col : row.getColumns()) {
                    if (col.getContent() != null) {
                        col.getContent().stream().forEach(content -> {
                            if (content instanceof JSONLayoutConfigurationContent) {
                                if (allNodes.containsKey(
                                    NodeIDSuffix.fromString(((JSONLayoutConfigurationContent)content).getNodeID()))) {
                                    cleanedColumns.add(col);
                                }
                            } else if (content instanceof JSONNestedLayout) {
                                if (allNodes
                                    .containsKey(NodeIDSuffix.fromString(((JSONNestedLayout)content).getNodeID()))) {
                                    cleanedColumns.add(col);
                                }
                            }
                        });
                    }
                }
                if (!cleanedColumns.isEmpty()) {
                    row.setColumns(cleanedColumns);
                    cleanedRows.add(row);
                }
            }
        }
        page.setRows(cleanedRows);
    }

    private void compareNodeIDs() {
        Set<Integer> missingIDs = new HashSet<Integer>();
        Set<Integer> notExistingIDs = new HashSet<Integer>(m_documentNodeIDs);
        Set<Integer> duplicateIDCheck = new HashSet<Integer>(m_documentNodeIDs);
        Set<Integer> duplicateIDs = new HashSet<Integer>();
        for (NodeIDSuffix id : m_viewNodes.keySet()) {
            int i = NodeID.fromString(id.toString()).getIndex();
            if (m_documentNodeIDs.contains(i)) {
                notExistingIDs.remove(i);
            } else {
                missingIDs.add(i);
            }
        }
        for (int id : m_documentNodeIDs) {
            if (!duplicateIDCheck.remove(id)) {
                duplicateIDs.add(id);
            }
        }
        StringBuilder error = new StringBuilder();
        if (notExistingIDs.size() > 0) {
            error.append("Node IDs referenced in layout, but do not exist in node: ");
            for (int id : notExistingIDs) {
                error.append(id);
                error.append(", ");
            }
            error.setLength(error.length() - 2);
            if (missingIDs.size() > 0 || duplicateIDs.size() > 0) {
                error.append("\n");
            }
        }
        if (missingIDs.size() > 0) {
            error.append("Node IDs missing in layout: ");
            for (int id : missingIDs) {
                error.append(id);
                error.append(", ");
            }
            error.setLength(error.length() - 2);
            if (duplicateIDs.size() > 0) {
                error.append("\n");
            }
        }
        if (duplicateIDs.size() > 0) {
            error.append("Multiple references to node IDs: ");
            for (int id : duplicateIDs) {
                error.append(id);
                error.append(", ");
            }
            error.setLength(error.length() - 2);
        }
        if (error.length() > 0 && m_statusLine != null && !m_statusLine.isDisposed()) {
            int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
            Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
            m_statusLine.setSize(newSize);
            m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 140, 0));
            m_statusLine.setText(error.toString());
        }
    }

    private void updateVisualLayout() {
        final List<VisualLayoutEditorJSONNode> nodes = createJSONNodeList();
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        String JSONNodes = "";
        try {
            JSONNodes = mapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot write JSON: " + e.getMessage(), e);
        }
        m_browser.evaluate("setNodes(\'" + JSONNodes + "\');");
        m_browser.evaluate("setLayout(\'" + getJsonDocument() + "\');");
    }

    private void updateConfigurationLayout() {
        final List<ConfigurationLayoutEditorJSONNode> nodes = createJSONConfigurationNodeList();
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        String JSONNodes = "";
        try {
            JSONNodes = mapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            LOGGER.error("Cannot write JSON: " + e.getMessage(), e);
        }
        m_configurationBrowser.evaluate("setNodes(\'" + JSONNodes + "\');");
        m_configurationBrowser.evaluate("setLayout(\'" + getJsonConfigurationDocument() + "\');");
    }

    /**
     * @return true, if current JSON layout structure is valid
     */
    protected boolean isJSONValid() {
        ObjectMapper mapper = createObjectMapperForUpdating();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            String json = isWindows() ? m_textArea.getText() : m_subnodeLayoutStringProvider.getLayoutString();
            JSONLayoutPage page = reader.readValue(json);
            m_documentNodeIDs.clear();
            if (page.getRows() != null) {
                for (JSONLayoutRow row : page.getRows()) {
                    populateDocumentNodeIDs(row);
                }
                compareNodeIDs();
                final String msg = cleanJSONPage(page);
                if (msg != null && !msg.isEmpty() && m_statusLine != null && !m_statusLine.isDisposed()) {
                    int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
                    Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
                    m_statusLine.setSize(newSize);
                    m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 140, 0));
                    m_statusLine.setText(msg);
                }
            }
            return true;
        } catch (IOException | NumberFormatException e) {
            String errorMessage;
            if (e instanceof JsonProcessingException) {
                JsonProcessingException jsonException = (JsonProcessingException)e;
                Throwable cause = null;
                Throwable newCause = jsonException.getCause();
                while (newCause instanceof JsonProcessingException) {
                    if (cause == newCause) {
                        break;
                    }
                    cause = newCause;
                    newCause = cause.getCause();
                }
                if (cause instanceof JsonProcessingException) {
                    jsonException = (JsonProcessingException)cause;
                }
                errorMessage = jsonException.getOriginalMessage().split("\n")[0];
                JsonLocation location = jsonException.getLocation();
                if (location != null) {
                    errorMessage += " at line: " + (location.getLineNr() + 1) + " column: " + location.getColumnNr();
                }
            } else {
                String message = e.getMessage();
                errorMessage = message;
            }
            if (m_statusLine != null && !m_statusLine.isDisposed()) {
                m_statusLine.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
                m_statusLine.setText(errorMessage);
                int textWidth = isWindows() ? m_textArea.getSize().width : m_text.getSize().x;
                Point newSize = m_statusLine.computeSize(textWidth, m_statusLine.getSize().y, true);
                m_statusLine.setSize(newSize);
            }
        }
        return false;
    }

    /**
     * @return the jsonDocument
     */
    public String getJsonDocument() {
        // keep empty fields
        ObjectMapper mapper = createObjectMapperForUpdating();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            return mapper.writeValueAsString(reader.readValue(m_subnodeLayoutStringProvider.getLayoutString()));
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve JSON string from layout:" + e.getMessage(), e);
        }

        return "";
    }

    /**
     * @return the jsonDocument
     */
    public String getJsonConfigurationDocument() {
        // keep empty fields
        ObjectMapper mapper = createObjectMapperForUpdating();
        ObjectReader reader = mapper.readerForUpdating(new JSONLayoutPage());
        try {
            return mapper.writeValueAsString(
                reader.readValue(m_subnodeConfigurationStringProvider.getConfigurationLayoutString()));
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve JSON string from layout:" + e.getMessage(), e);
        }

        return "";
    }

    private static boolean isWindows() {
        return Platform.OS_WIN32.equals(Platform.getOS());
    }

    private void updateJsonTextArea() {
        if (isWindows()) {
            m_textArea.setText(m_subnodeLayoutStringProvider.getLayoutString());
        } else {
            m_text.setText(m_subnodeLayoutStringProvider.getLayoutString());
        }
    }

    @Override
    public void dispose() {
        if (m_browser != null && !m_browser.isDisposed()) {
            m_browser.dispose();
        }
        if (m_configurationBrowser != null && !m_configurationBrowser.isDisposed()) {
            m_configurationBrowser.dispose();
        }
        if (m_visualLayoutUpdate != null && !m_visualLayoutUpdate.isDisposed()) {
            m_visualLayoutUpdate.dispose();
        }
        if (m_configurationLayoutUpdate != null && !m_configurationLayoutUpdate.isDisposed()) {
            m_configurationLayoutUpdate.dispose();
        }
        m_browser = null;
        m_visualLayoutUpdate = null;
        m_configurationBrowser = null;
        m_configurationLayoutUpdate = null;
        super.dispose();
    }

    /**
     * @return an {@link ObjectMapper} configured to skip non-empty fields, with the exception of empty content fields
     */
    private static ObjectMapper createObjectMapperForUpdating() {
        final ObjectMapper mapper = JSONLayoutPage.getConfiguredObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addSerializer(new JSONLayoutColumnSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    private List<VisualLayoutEditorJSONNode> createJSONNodeList() {
        final List<VisualLayoutEditorJSONNode> nodes = new ArrayList<>();
        for (final Entry<NodeIDSuffix, SingleNodeContainer> viewNode : m_viewNodes.entrySet()) {
            final SingleNodeContainer node = viewNode.getValue();
            final VisualLayoutEditorJSONNode jsonNode =
                new VisualLayoutEditorJSONNode(node.getID().getIndex(), node.getName(),
                    node.getNodeAnnotation().getText(), getLayout(viewNode.getValue(), viewNode.getKey()),
                    getIcon(node), !isHideInWizard(node), getType(node));
            if (node instanceof SubNodeContainer) {
                // set to provide additional info in the Visual Layout Editor
                boolean isSubNodeContainerUsingLegacyMode = !((SubNodeContainer)node).getSubnodeLayoutStringProvider()
                        .getLayoutString().contains("\"parentLayoutLegacyMode\":false");
                jsonNode.setContainerLegacyModeEnabled(isSubNodeContainerUsingLegacyMode);
            }
            nodes.add(jsonNode);
        }
        return nodes;
    }

    private static boolean isHideInWizard(final SingleNodeContainer nc) {
        if (nc instanceof NativeNodeContainer) {
            NodeModel model = ((NativeNodeContainer)nc).getNodeModel();
            if (model instanceof ViewHideable) {
                return ((ViewHideable)model).isHideInWizard();
            }
        }
        return false;
    }

    private List<ConfigurationLayoutEditorJSONNode> createJSONConfigurationNodeList() {
        final List<ConfigurationLayoutEditorJSONNode> nodes = new ArrayList<>();
        for (@SuppressWarnings("rawtypes") final Entry<NodeIDSuffix, DialogNode> viewNode : m_dialogNodes.entrySet()) {
            @SuppressWarnings("rawtypes")
            final DialogNode node = viewNode.getValue();
            final NodeID nodeID = viewNode.getKey().prependParent(m_subNodeContainer.getWorkflowManager().getID());
            final NodeContainer nodeContainer = m_wfManager.getNodeContainer(nodeID);
            final ConfigurationLayoutEditorJSONNode jsonNode = new ConfigurationLayoutEditorJSONNode(
                nodeContainer.getID().getIndex(), nodeContainer.getName(), nodeContainer.getNodeAnnotation().getText(),
                getConfigurationLayout(viewNode.getValue(), viewNode.getKey()), getIcon(nodeContainer),
                !node.isHideInDialog(), "configuration");
            nodes.add(jsonNode);
        }
        return nodes;
    }

    private static JSONLayoutContent getLayout(final SingleNodeContainer node, final NodeIDSuffix id) {
        if (node instanceof SubNodeContainer) {
            final JSONNestedLayout layout = new JSONNestedLayout();
            layout.setNodeID(id.toString());
            return layout;
        }
        return DefaultLayoutCreatorImpl.getDefaultViewContentForNode(id, (NativeNodeContainer)node);
    }

    private static JSONLayoutConfigurationContent
        getConfigurationLayout(@SuppressWarnings("rawtypes") final DialogNode node, final NodeIDSuffix id) {
        return DefaultConfigurationCreatorImpl.getDefaultConfigurationContentForNode(id, node);
    }

    private static String getType(final SingleNodeContainer node) {
        NodeModel model = node instanceof NativeNodeContainer ? ((NativeNodeContainer)node).getNodeModel() : null;
        final boolean isWizardNode = model instanceof WizardNode;
        if (isWizardNode) {
            if (model instanceof DialogNode) {
                return "quickform";
            }
            return "view";
        }
        if (model instanceof DialogNode) {
            return "configuration";
        }
        if (node instanceof SubNodeContainer) {
            return "nestedLayout";
        }
        throw new IllegalArgumentException(
            "Node is not view, subnode, configuration or quickform: " + node.getNameWithID());
    }

    private static String getIcon(final NodeContainer nodeContainer) {
        if (nodeContainer == null) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png"));
        }
        String iconBase64 = "";
        if (nodeContainer instanceof SubNodeContainer) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout_16.png"));
        }
        try {
            final URL url = FileLocator.resolve(nodeContainer.getIcon());
            final String mimeType = URLConnection.guessContentTypeFromName(url.getFile());
            byte[] imageBytes = null;
            try (InputStream s = url.openStream()) {
                imageBytes = IOUtils.toByteArray(s);
            }
            iconBase64 = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (final IOException e) {
            // Do nothing
        }

        if (iconBase64.isEmpty()) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png"));
        }
        return iconBase64;
    }

    private static String createIcon(final Image i) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[]{i.getImageData()};
        loader.save(out, SWT.IMAGE_PNG);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static final class VisualLayoutViewCreator extends JavaScriptViewCreator<WebViewContent, WebViewContent> {
        VisualLayoutViewCreator(final WebTemplate template, final WebTemplate debugTemplate) {
            super(null);
            setWebTemplate(isDebug() ? debugTemplate : template);
        }
    }

    private class UpdateLayoutFunction extends BrowserFunction {

        /**
         * @param browser
         * @param name
         */
        public UpdateLayoutFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object function(final Object[] arguments) {
            if (arguments == null || arguments.length < 1) {
                return false;
            }
            final String layout = arguments[0].toString();
            final ObjectMapper mapper = createObjectMapperForUpdating();
            JSONLayoutPage page = new JSONLayoutPage();
            final ObjectReader reader = mapper.readerForUpdating(page);
            try {
                page = reader.readValue(layout);
            } catch (Exception e) {
                LOGGER.error("Cannot read layout from visual editor. " + e.getMessage(), e);
                return false;
            }

            try {
                m_subnodeLayoutStringProvider.setLayoutString(mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(page));
            } catch (Exception e) {
                LOGGER.error("Cannot write layout from visual editor. " + e.getMessage(), e);
                return false;
            }

            return true;
        }

    }

    private class UpdateConfigurationFunction extends BrowserFunction {

        /**
         * @param browser
         * @param name
         */
        public UpdateConfigurationFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object function(final Object[] arguments) {
            if (arguments == null || arguments.length < 1) {
                return false;
            }
            final String layout = arguments[0].toString();
            final ObjectMapper mapper = createObjectMapperForUpdating();
            JSONLayoutPage page = new JSONLayoutPage();
            final ObjectReader reader = mapper.readerForUpdating(page);
            try {
                page = reader.readValue(layout);
            } catch (Exception e) {
                LOGGER.error("Cannot read layout from visual editor. " + e.getMessage(), e);
                return false;
            }

            try {
                m_subnodeConfigurationStringProvider
                    .setConfigurationLayoutString(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page));
            } catch (Exception e) {
                LOGGER.error("Cannot write layout from visual editor. " + e.getMessage(), e);
                return false;
            }

            return true;
        }

    }

    /**
     * Custom serializer for {@link JSONLayoutColumn}. This will only serialize non-empty fields with the exception of
     * "content" which can be empty but not null. This was needed because there's no way to override Jackson's
     * serialization inclusion rule.
     */
    private static final class JSONLayoutColumnSerializer extends StdSerializer<JSONLayoutColumn> {

        private static final long serialVersionUID = 1L;

        protected JSONLayoutColumnSerializer() {
            super(JSONLayoutColumn.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final JSONLayoutColumn value, final JsonGenerator gen,
            final SerializerProvider serializers) throws IOException, JsonProcessingException {
            final List<String> additionalClasses = value.getAdditionalClasses();
            final List<String> additionalStyles = value.getAdditionalStyles();
            final List<JSONLayoutContent> content = value.getContent();
            final Integer widthLG = value.getWidthLG();
            final Integer widthMD = value.getWidthMD();
            final Integer widthSM = value.getWidthSM();
            final Integer widthXL = value.getWidthXL();
            final Integer widthXS = value.getWidthXS();
            gen.writeStartObject();
            if (additionalClasses != null && !additionalClasses.isEmpty()) {
                gen.writeArrayFieldStart("additionalClasses");
                for (final String s : additionalClasses) {
                    gen.writeString(s);
                }
                gen.writeEndArray();
            }
            if (additionalStyles != null && !additionalStyles.isEmpty()) {
                gen.writeArrayFieldStart("additionalStyles");
                for (final String s : additionalStyles) {
                    gen.writeString(s);
                }
                gen.writeEndArray();
            }
            if (content != null) {
                gen.writeArrayFieldStart("content");
                for (final JSONLayoutContent c : content) {
                    gen.writeObject(c);
                }
                gen.writeEndArray();
            }
            if (widthLG != null) {
                gen.writeNumberField("widthLG", widthLG);
            }
            if (widthMD != null) {
                gen.writeNumberField("widthMD", widthMD);
            }
            if (widthSM != null) {
                gen.writeNumberField("widthSM", widthSM);
            }
            if (widthXL != null) {
                gen.writeNumberField("widthXL", widthXL);
            }
            if (widthXS != null) {
                gen.writeNumberField("widthXS", widthXS);
            }
            gen.writeEndObject();
        }
    }
}
