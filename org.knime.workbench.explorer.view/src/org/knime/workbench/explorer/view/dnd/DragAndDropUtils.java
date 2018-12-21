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

package org.knime.workbench.explorer.view.dnd;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * Utility class for drag and drop support.
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
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
     *      the selection includes any object other than
     *      {@link ContentObject} or {@link AbstractContentProvider}.
     */
    public static Map<AbstractContentProvider, List<AbstractExplorerFileStore>>
            getProviderMap(final IStructuredSelection selection) {
        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> providers
                = new TreeMap<AbstractContentProvider,
                List<AbstractExplorerFileStore>>();
        if (selection == null) {
            return providers;
        }
        @SuppressWarnings("rawtypes")
        Iterator iter = selection.iterator();
        while (iter.hasNext()) {
            Object nextObject = iter.next();
            if (nextObject instanceof ContentObject) {
                ContentObject content = (ContentObject)nextObject;
                AbstractContentProvider provider = content.getProvider();
                AbstractExplorerFileStore fs = content.getObject();
                if (!providers.containsKey(provider)) {
                    providers.put(provider,
                            new ArrayList<AbstractExplorerFileStore>());
                }
                providers.get(provider).add(fs);
            }else if (nextObject instanceof AbstractContentProvider) {
                AbstractContentProvider provider =
                        (AbstractContentProvider)nextObject;
                AbstractExplorerFileStore fs = provider.getFileStore("/");
                if (!providers.containsKey(provider)) {
                    providers.put(provider,
                            new ArrayList<AbstractExplorerFileStore>());
                }
                providers.get(provider).add(fs);
            } else {
                return null;
            }
        }
        return providers;
    }

    /**
     * Builds a list containing all selected explorer file stores. Selected
     * content providers (mount points) are represented by a root file store
     * ("/").
     * @param selection the structured collection to process
     * @return a map of content providers to explorer file stores or null if
     *      the selection includes {@link ContentObject}s not containing an
     *      {@link AbstractExplorerFileStore}.
     */
    public static List<AbstractExplorerFileStore> getExplorerFileStores(
            final IStructuredSelection selection) {
        List<AbstractExplorerFileStore> fileStores
                = new ArrayList<AbstractExplorerFileStore>();
        @SuppressWarnings("rawtypes")
        Iterator iter = selection.iterator();
        while (iter.hasNext()) {
            Object nextObject = iter.next();
            if (nextObject instanceof ContentObject) {
                fileStores.add(((ContentObject)nextObject).getObject());
            } else if (nextObject instanceof AbstractContentProvider) {
                fileStores.add(((AbstractContentProvider)nextObject)
                        .getFileStore("/"));
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
    public static AbstractExplorerFileStore getFileStore(
            final Object selection) {
        if (selection instanceof ContentObject) {
            return ((ContentObject)selection).getObject();
        } else if (selection instanceof AbstractContentProvider) {
            return ((AbstractContentProvider)selection).getFileStore("/");
        } else if (selection instanceof AbstractExplorerFileStore) {
            return (AbstractExplorerFileStore)selection;
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
            AbstractExplorerFileStore fs
                    = DragAndDropUtils.getFileStore(selection);
            if (fs == null) {
                return;
            }
            File localFile = fs.toLocalFile(EFS.NONE, null);
            if (localFile == null) {
                return;
            }
            r = KnimeResourceUtil.getResourceForURI(localFile.toURI());
            if (r != null) {
                if (r instanceof IWorkspaceRoot) {
                    // we have the workspace root and therefore no project
                    pName = "workspace root";
                    r.refreshLocal(1, null);
                } else {
                    pName = r.getParent().getName();
                    r.getParent().refreshLocal(1, null);
                }
            }
        } catch (CoreException e) {
            LOGGER.error("Could not refresh resources for project "
                    + pName + ".");
        }
    }

    /**
     * Evaluates if a project is linked into the workspace or if it has been
     * copied/created there.
     * @param selection a selection in the explorer tree view
     * @return true if it is a KNIME project linked into the workspace, false
     *          otherwise
     */
    public static boolean isLinkedProject(final Object selection) {
        AbstractExplorerFileStore fs = getFileStore(selection);
        File localFile;
        IResource source;
        try {
            localFile = fs.toLocalFile(EFS.NONE, null);
            source = KnimeResourceUtil.getResourceForURI(
                    localFile.toURI());
        } catch (CoreException e) {
            return false;
        }
        if (source != null && source.getProject() != null) {
            IProject project = source.getProject();
            IPath loc = project.getLocation();
            return !ResourcesPlugin.getWorkspace().getRoot().getLocation()
                    .isPrefixOf(loc);
         } else {
             return false;
         }
    }

}
