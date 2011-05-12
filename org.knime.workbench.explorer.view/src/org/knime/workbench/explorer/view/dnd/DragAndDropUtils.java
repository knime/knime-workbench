/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 29, 2011 (morent): created
 */

package org.knime.workbench.explorer.view.dnd;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * Utility class for drag and drop support.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public final class DragAndDropUtils {
    private static final  NodeLogger LOGGER = NodeLogger.getLogger(
            DragAndDropUtils.class);

    private DragAndDropUtils() {
        super();
    }

    /**
     * Builds a map containing the selected explorer file stores grouped by
     * content provider.
     * @param selection the structured collection to group by provider
     * @return a map of content providers to explorer file stores or null if
     *      the selection includes {@link ContentObject}s not containing an
     *      {@link ExplorerFileStore}.
     */
    public static Map<AbstractContentProvider, List<ExplorerFileStore>>
            getProviderMap(final IStructuredSelection selection) {
        Map<AbstractContentProvider, List<ExplorerFileStore>> providers =
                new TreeMap<AbstractContentProvider, List<ExplorerFileStore>>();
        @SuppressWarnings("rawtypes")
        Iterator iter = selection.iterator();
        while (iter.hasNext()) {
            Object nextObject = iter.next();
            if (nextObject instanceof ContentObject) {
                ContentObject content = (ContentObject)nextObject;
                AbstractContentProvider provider = content.getProvider();
                ExplorerFileStore fs = content.getObject();
                if (!providers.containsKey(provider)) {
                    providers.put(provider, new ArrayList<ExplorerFileStore>());
                }
                providers.get(provider).add(fs);
            } else {
                return null;
            }
        }
        return providers;
    }

    /**
     * Builds a list containing all selected explorer file stores.
     * @param selection the structured collection to process
     * @return a map of content providers to explorer file stores or null if
     *      the selection includes {@link ContentObject}s not containing an
     *      {@link ExplorerFileStore}.
     */
    public static List<ExplorerFileStore> getExplorerFileStores(
            final IStructuredSelection selection) {
        List<ExplorerFileStore> fileStores = new ArrayList<ExplorerFileStore>();
        @SuppressWarnings("rawtypes")
        Iterator iter = selection.iterator();
        while (iter.hasNext()) {
            Object nextObject = iter.next();
            if (nextObject instanceof ContentObject) {
                fileStores.add(((ContentObject)nextObject).getObject());
            } else {
                return null;
            }
        }
        return fileStores;
    }

    /**
     * @param selection a selection in the explorer tree view
     * @return the explorer file store corresponding to the selection
     */
    public static ExplorerFileStore getFileStore(final Object selection) {
        if (selection instanceof ContentObject) {
            return ((ContentObject)selection).getObject();
        } else if (selection instanceof AbstractContentProvider) {
            return ((AbstractContentProvider)selection).getFileStore("/");
        } else if (selection instanceof ExplorerFileStore) {
            return (ExplorerFileStore)selection;
        } else {
            return null;
        }
    }


    /**
     * @param selection a selection in the explorer tree view
     * @return the abstract content provider responsible for the selection
     */
    public static AbstractContentProvider getContentProvider(
            final Object selection) {
        if (selection instanceof ContentObject) {
            ContentObject contentObject = (ContentObject)selection;
            return contentObject.getProvider();
        } else if (selection instanceof AbstractContentProvider) {
            return (AbstractContentProvider)selection;
        } else {
            return null;
        }
    }

    /**
     * @param selection a selection in the explorer tree view
     */
    public static void refreshResource(final Object selection) {
        IResource r = null;
        String pName = "<unknown>";
        try {
            ExplorerFileStore fs = DragAndDropUtils.getFileStore(selection);
            File localFile = fs.toLocalFile(EFS.NONE, null);
            if (fs == null || localFile == null) {
                return;
            }
            r = KnimeResourceUtil.getResourceForURI(localFile.toURI());
            if (r != null) {
                if (r instanceof IWorkspaceRoot) {
                    // we have the workspace root and therefore no project
                    pName = "workspace root";
                    r.refreshLocal(IResource.DEPTH_INFINITE, null);
                } else {
                    pName = r.getParent().getName();
                    r.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
                }
            }
        } catch (CoreException e) {
            LOGGER.error("Could not refresh resources for project "
                    + pName + ".");
        }
    }

}
