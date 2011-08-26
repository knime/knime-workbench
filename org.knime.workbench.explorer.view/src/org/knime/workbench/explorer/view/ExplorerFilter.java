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
  *   Jun 1, 2011 (morent): created
  */

package org.knime.workbench.explorer.view;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.repository.view.TextualViewFilter;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerFilter extends TextualViewFilter {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExplorerFilter.class);

    private final Map<String, Boolean> m_cache = new HashMap<String, Boolean>();

    /**
     *  An element is selected if itself, a parent or a
     * child contains the query string in its name.
     * {@inheritDoc}
     */
    @Override
    protected boolean doSelect(final Object parentElement,
            final Object element, final boolean direct) {
        boolean selectThis = false;
        // Content delegators are always shown
        if (element instanceof AbstractContentProvider) {
            return true;
        } else if (element instanceof ContentObject
                || element instanceof AbstractExplorerFileStore) {
            AbstractExplorerFileStore fileStore = null;
            AbstractContentProvider contentProvider = null;
            if (element instanceof ContentObject) {
                ContentObject contentObject = (ContentObject)element;
                fileStore = contentObject.getObject();
                contentProvider = contentObject.getProvider();
            } else {
                fileStore = (AbstractExplorerFileStore)element;
                contentProvider = fileStore.getContentProvider();
            }
            String fullName = fileStore.getFullName();

            Boolean selected = m_cache.get(fullName);
            if (selected != null) {
//                LOGGER.debug("Retrieved " + fullName
//                        + " from cache with value " + selected);
                if (direct) {
//                    LOGGER.debug("Removing " + fullName + " from cache.");
                    m_cache.remove(fullName);
                }
                return selected;
            }

            selectThis = match(fullName);
            /* Files are shown if their name matches or if the name of any
             * parent folder matches. */
            if (AbstractExplorerFileStore.isDirOrWorkflowGroup(fileStore)) {
                /* Directories are shown if their name matches or if the name
                 * of any child matches. */
                if (!selectThis) {
                    for (AbstractExplorerFileStore child
                            : contentProvider.getChildren(fileStore)) {
                        // parent is not necessary (->null)
                        selectThis = doSelect(null, child, false);
                        if (selectThis) {
                            break;
                        }
                    }
                }
            } else {
                if (!direct) {
                    IPath path = new Path(fullName);
                    while (path.segmentCount() > 1) {
                        path = path.removeLastSegments(1);
                        String pathString = path.toString();
                        Boolean existsAndTrue = m_cache.get(pathString) != null
                                ? m_cache.get(pathString) : false;
//                        LOGGER.debug("Caching " + pathString + ": " + (
//                                selectThis || existsAndTrue));
                        m_cache.put(pathString, selectThis || existsAndTrue);
                    }
                }
            }

        }
        return selectThis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQueryString(final String query) {
        super.setQueryString(query);
        m_cache.clear();
//        LOGGER.debug("Clearing cache...");
    }
}
