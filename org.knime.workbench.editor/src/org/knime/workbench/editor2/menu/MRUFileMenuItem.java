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
 *   Oct 24, 2019 (loki): created
 */
package org.knime.workbench.editor2.menu;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.explorer.ExplorerURLStreamHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The genesis of this class is https://knime-com.atlassian.net/browse/AP-12953 - this dynamic menu item, referenced
 *  from this plugin's plugin.xml file, populates a File submenu of 'Recent Workflows'
 *
 * @author loki der quaeler
 */
public class MRUFileMenuItem extends ContributionItem {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MRUFileMenuItem.class);


    /**
     * Default constructor.
     */
    public MRUFileMenuItem() { }

    /**
     * Constructor taking an item id.
     * @param id
     */
    public MRUFileMenuItem(final String id) {
        super(id);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public void fill(final Menu menu, final int index) {
        try {
            final Bundle myself = FrameworkUtil.getBundle(MRUFileMenuItem.class);
            final IPath statePath = Platform.getStateLocation(myself);
            final Path parentPath = statePath.toFile().toPath().getParent();
            if (parentPath == null) {
                return;
            }

            final DocumentBuilder parser;
            Document doc;
            try {
                final Path workbenchPath = parentPath.resolve("org.eclipse.e4.workbench").resolve("workbench.xmi");
                parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = parser.parse(workbenchPath.toFile());
            } catch (final InvalidPathException | SAXException | UnsupportedOperationException
                    | IOException exception) {
                LOGGER.info("Could not find the workbench.xmi file - this is expected in new workspaces.", exception);
                return;
            }

            final XPath xpath = XPathFactory.newInstance().newXPath();
            final String mruMemento = (String)xpath.evaluate("persistedState[@key = 'memento']/@value",
                doc.getDocumentElement(), XPathConstants.STRING);

            doc = parser.parse(new InputSource(new StringReader(mruMemento)));

            final NodeList workflowList =
                (NodeList)xpath.evaluate("//mruList/file[@id = '" + WorkflowEditor.ID + "']/persistable",
                    doc.getDocumentElement(), XPathConstants.NODESET);
            if (workflowList.getLength() > 0) {
                for (int i = 0; i < workflowList.getLength(); i++) {
                    final Element e = (Element)workflowList.item(i);
                    final String uri = e.getAttribute("uri"); // knime://MP/.../WorkflowName/workflow.knime
                    final String[] parts = uri.split("/");
                    if (parts.length > 2) {
                        final URL u = new URL(uri);
                        final MenuItem mi = new MenuItem(menu, SWT.PUSH);
                        mi.setText(URLDecoder.decode(parts[parts.length - 2], "UTF-8"));
                        mi.setToolTipText(URLDecoder.decode(uri, "UTF-8").replaceAll("/workflow\\.knime$", ""));
                        mi.setEnabled(false);
                        mi.addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(final SelectionEvent se) {
                                try {
                                    final URI workflowURI = new URI(u.getProtocol(), u.getHost(),
                                        URLDecoder.decode(u.getPath(), "UTF-8"), u.getQuery());
                                    IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
                                        workflowURI, WorkflowEditor.ID, true);
                                } catch (final Exception exception) {
                                    LOGGER.error("Exception encountered attempting to open the workflow at "
                                        + u.toExternalForm(), exception);
                                }
                            }
                        });

                        KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(new AssetExistenceResolver(mi, u));
                    }
                }
            }
        }
        catch (final Exception e) {
            LOGGER.error("Exception encountered while updating the Recent Workflows submenu.", e);
        }
    }


    private static class AssetExistenceResolver implements Runnable {
        private final MenuItem m_menuItem;
        private final URL m_assetURL;

        private AssetExistenceResolver(final MenuItem menuItem, final URL url) {
            m_menuItem = menuItem;
            m_assetURL = url;
        }

        @Override
        public void run() {
            final ExplorerURLStreamHandler handler = new ExplorerURLStreamHandler();

            try {
                final URLConnection connection = handler.openConnection(m_assetURL);
                try (final InputStream is = connection.getInputStream()) {
                    if (connection.getContentLength() > 0) {
                        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                            m_menuItem.setEnabled(true);
                        });
                    } else {
                        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                            m_menuItem.dispose();
                        });
                    }
                }
            } catch (IOException ioe) {
                LOGGER.debug("Unable to open a connection to a recent workflow [" + m_assetURL.toExternalForm() + "].",
                    ioe);

                PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                    m_menuItem.dispose();
                });
            }
        }
    }
}
