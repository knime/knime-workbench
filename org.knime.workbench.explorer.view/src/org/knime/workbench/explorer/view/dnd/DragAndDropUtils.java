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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;

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
                AbstractExplorerFileStore fs = provider.getRootStore();
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
                        .getRootStore());
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
            return ((AbstractContentProvider)selection).getRootStore();
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
}
