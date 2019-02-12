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
package org.knime.workbench.explorer.view;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.MessageFileStore;
import org.knime.workbench.repository.view.TextualViewFilter;


public class ExplorerFilter extends TextualViewFilter {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExplorerFilter.class);

    private final Map<String, Boolean> m_cache = new HashMap<String, Boolean>();

    private boolean m_usingKnimeProtocol;

    private String m_queryString;

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

            final String cachekey = m_usingKnimeProtocol ? contentProvider.getMountID() + fullName : fullName;

            Boolean selected = m_cache.get(cachekey);
            if (selected != null) {
//                LOGGER.debug("Retrieved " + fullName
//                        + " from cache with value " + selected);
                if (direct) {
//                    LOGGER.debug("Removing " + fullName + " from cache.");
                    m_cache.remove(cachekey);
                }
                return selected;
            }

            selectThis = match(fileStore);
            /* Files are shown if their name matches or if the name of any
             * parent folder matches. */
            if (AbstractExplorerFileStore.isWorkflowGroup(fileStore)) {
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
                        final String key =
                            m_usingKnimeProtocol ? contentProvider.getMountID() + pathString : pathString;
                        m_cache.put(key, selectThis || existsAndTrue);
                    }
                }
            }

        }
        return selectThis;
    }

    private boolean match(final AbstractExplorerFileStore fileStore) {
        if (m_usingKnimeProtocol) {
            // searching using the knime protocol, check url of the store.
            return !(fileStore instanceof MessageFileStore)
                && fileStore.toURI().toString().toUpperCase().startsWith(m_queryString);
        }
        return match(fileStore.getFullName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQueryString(final String query) {
        super.setQueryString(query);
        m_cache.clear();
        m_queryString = query.toUpperCase();
        m_usingKnimeProtocol = m_queryString.startsWith("KNIME://");
//        LOGGER.debug("Clearing cache...");
    }
}
