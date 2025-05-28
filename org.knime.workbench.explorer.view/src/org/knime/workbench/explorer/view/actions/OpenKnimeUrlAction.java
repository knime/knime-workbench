/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.CoreConstants;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.ExplorerURLStreamHandler;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.explorer.view.ExplorerView;
import org.osgi.framework.FrameworkUtil;

/**
 * Action that downloads remote items to a temp location and opens them in an editor.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 7.3
 */
public class OpenKnimeUrlAction extends Action {

    private static final String PLUGIN_ID = FrameworkUtil.getBundle(OpenKnimeUrlAction.class).getSymbolicName();

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenKnimeUrlAction.class);

    private static final String EXAMPLES = "knime://EXAMPLES/";

    private static final String PATH_START = "/Users/knime/Examples/";

    private static boolean eventHandlingActive;

    /**
     * @param active whether the URLs and KNWF/KNAR from OS events should be processed
     * @since 8.9
     */
    public static void setEventHandlingActive(final boolean active) {
        eventHandlingActive = active;
    }

    /**
     * @return whether the URLs and KNWF/KNAR from OS events should be processed
     * @since 8.9
     */
    public static boolean isEventHandlingActive() {
        return eventHandlingActive;
    }

    /*--------- inner job class -------------------------------------------------------------------------*/
    private static class OpenURLJob extends ExplorerJob {

        private final IWorkbenchPage m_page;

        private URI m_url;

        OpenURLJob(final IWorkbenchPage page, final URI url) {
            super("Open KNIME URL");
            m_page = page;
            try {
                m_url = parseURL(url);
            } catch (URISyntaxException e) {
                LOGGER.warn("Could not process URL " + url + ": " + e.getMessage(), e);
                m_url = url;
            }
        }

        /**
         * Adjusts KNIME URIs that point to example server so that they work with KNIME Hub.
         *
         * @param url The original URI.
         * @return The adjusted URI.
         * @throws URISyntaxException If an error occurs.
         */
        private static URI parseURL(final URI url) throws URISyntaxException {
            final String urlString = url.toString();

            if (!urlString.startsWith(EXAMPLES)) {
                return url;
            }

            return new URI(StringUtils.replace(urlString, EXAMPLES,
                "knime://" + CoreConstants.KNIME_EXAMPLES_MOUNT_ID + PATH_START, 1));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            final AtomicReference<IStatus> returnStatus = new AtomicReference<IStatus>(Status.OK_STATUS);
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        String host = m_url.getHost();
                        if (host.equals(ExplorerURLStreamHandler.NODE_RELATIVE)
                            || host.equals(ExplorerURLStreamHandler.WORKFLOW_RELATIVE)
                            || host.equals(ExplorerURLStreamHandler.MOUNTPOINT_RELATIVE)) {
                            LOGGER.error("Opening of relative KNIME URLs is not supported!");
                            returnStatus.set(new Status(IStatus.ERROR, PLUGIN_ID, 1,
                                "Opening of relative KNIME URLs is not supported!", null));
                            return;
                        }
                        final AbstractContentProvider provider = ExplorerMountTable.getContentProvider(host).orElse(null);

                        if (provider == null) {
                            LOGGER.error("Failed to open URL. Mount point does not exist: " + host);
                            returnStatus.set(new Status(IStatus.ERROR, PLUGIN_ID, 1,
                                "Failed to open URL. Mount point does not exist: " + host, null));
                            return;
                        }
                        AbstractExplorerFileStore fileStore = ExplorerMountTable.getFileSystem().getStore(m_url);
                        if (fileStore == null) {
                            LOGGER.error("Failed to create file store for URL.");
                            returnStatus.set(
                                new Status(IStatus.ERROR, PLUGIN_ID, 1, "Failed to create file store for URL.", null));
                            return;
                        }

                        IViewPart part = m_page.findView("org.knime.workbench.explorer.view");
                        if (part instanceof ExplorerView) {
                            ExplorerView view = (ExplorerView)part;
                            view.setNextSelection(fileStore);
                            if (provider.isRemote()) {
                                try {
                                    provider.connectAndWaitForRepository(30 * 1000);
                                } catch (TimeoutException e) {
                                    LOGGER.error("Failed to open item denoted by '" + m_url + "': " + e.getMessage());
                                    returnStatus.set(new Status(IStatus.ERROR, PLUGIN_ID,
                                        "Failed to open item denoted by '" + m_url + "': " + e.getMessage()));
                                    return;
                                }
                            }
                            if (!fileStore.fetchInfo().exists()) {
                                returnStatus.set(new Status(IStatus.ERROR, PLUGIN_ID,
                                    "The item denoted by " + m_url + " does not exist."));
                                return;
                            }
                            Object object = ContentDelegator.getTreeObjectFor(fileStore);
                            if (object != null) {
                                view.getViewer().reveal(object);
                                view.getViewer().refresh(object);
                                view.getViewer().setSelection(new StructuredSelection(object));
                            }
                            if (AbstractExplorerFileStore.isWorkflow(fileStore)) {
                                if (provider.isRemote()) {
                                    if (fileStore instanceof RemoteExplorerFileStore) {
                                        List<RemoteExplorerFileStore> downloadList =
                                            new ArrayList<RemoteExplorerFileStore>(1);
                                        downloadList.add((RemoteExplorerFileStore)fileStore);
                                        DownloadAndOpenWorkflowAction action =
                                            new DownloadAndOpenWorkflowAction(m_page, downloadList);
                                        action.run();
                                    } else {
                                        //should not happen
                                        returnStatus.set(new Status(IStatus.WARNING, PLUGIN_ID,
                                            "FileStoreProvider is remote but FileStore is not a RemoteExplorerFileStore"));
                                    }
                                } else {
                                    URI urlToOpen = new URI(m_url + "/workflow.knime");
                                    IDE.openEditor(m_page, urlToOpen, "org.knime.workbench.editor.WorkflowEditor", true);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.info("Cannot open editor for the URL " + m_url);
                        returnStatus.set(
                            new Status(IStatus.ERROR, PLUGIN_ID, 1, "Cannot open the editor for URL " + m_url, null));
                    }

                }
            });
            return returnStatus.get();
        }
    }

    /* --- end of inner job class -----------------------------------------------------------------------------------*/

    private final List<String> m_urls;

    private final IWorkbenchPage m_page;

    /**
     * Opens a resource denoted by KNIME URL in an editor.
     *
     * @param page the current workbench page
     * @param urls things to open
     */
    public OpenKnimeUrlAction(final IWorkbenchPage page, final List<String> urls) {
        setDescription("Open KNIME URL");
        setToolTipText(getDescription());
        setImageDescriptor(ImageRepository.getIconDescriptor(SharedImages.Workflow));
        m_page = page;
        m_urls = new LinkedList<>(urls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        for (String url : m_urls) {
            if (url == null || url.isEmpty()) {
                continue;
            }
            URI uri;
            try {
                uri = new URI(url.replaceAll(" ", "%20"));
                LOGGER.info("Opening location denoted by KNIME URL: " + url);
                OpenURLJob job = new OpenURLJob(m_page, uri);
                job.schedule();
            } catch (NullPointerException | URISyntaxException e) {
                LOGGER.error(url + " is no valid URL");
            }
        }
    }
}
