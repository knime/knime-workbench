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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;

/**
 * Utility class for drag and drop support.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class DragAndDropUtils {
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
            if (nextObject instanceof ContentObject
                    && ((ContentObject)nextObject).getObject()
                    instanceof ExplorerFileStore) {
                ContentObject content = (ContentObject)nextObject;
                AbstractContentProvider provider = content.getProvider();
                ExplorerFileStore fs = (ExplorerFileStore)content.getObject();
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

}
