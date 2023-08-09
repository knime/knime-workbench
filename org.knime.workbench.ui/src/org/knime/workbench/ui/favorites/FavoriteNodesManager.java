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
 * ---------------------------------------------------------------------
 *
 * History
 *   17.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.ui.favorites;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.repository.NodeUsageRegistry;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public final class FavoriteNodesManager {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FavoriteNodesManager.class);

    private static FavoriteNodesManager instance;

    private Root m_root;

    private Category m_favNodes;

    private Category m_freqNodes;

    private Category m_lastNodes;

    // loading and saving
    private static final String TAG_FAVORITES = "favoritenodes";

    private static final String TAG_PERSONAL_FAVS = "personals";

    private static final String TAG_MOST_FREQUENT = "frequents";

    private static final String TAG_LAST_USED = "lastused";

    private static final String TAG_FAVORITE = "favorite";

    private static final String TAG_NODE_ID = "nodeid";

    /** ID of the personal favorites category. */
    public static final String FAV_CAT_ID = "fav";

    /**
     * Title of the personal favorites category (used by {@link FavoriteNodesDropTarget}).
     */
    static final String FAV_TITLE = "Personal favorite nodes";

    /**
     *
     * @return singleton instance
     */
    public static synchronized FavoriteNodesManager getInstance() {
        if (instance == null) {
            instance = new FavoriteNodesManager();
        }
        return instance;
    }

    private FavoriteNodesManager() {
        createTreeModel();
    }

    /**
     *
     * @return true if it was initialized, false otherwise
     */
    public static boolean wasInitialized() {
        return instance != null;
    }

    /**
     *
     * @return the tree model with three categories: favorites, most frequent and last used
     */
    public Root getRoot() {
        return m_root;
    }

    /**
     *
     */
    private void createTreeModel() {
        String pluginID = FrameworkUtil.getBundle(getClass()).getSymbolicName();

        m_root = new Root();
        m_root.setSortChildren(false);
        m_favNodes = new CopyingCategory(FAV_CAT_ID, FAV_TITLE, pluginID);
        m_favNodes.setIcon(ImageRepository.getIconImage(SharedImages.FavoriteNodesFolder));
        m_favNodes.setAfterID("");
        m_favNodes.setSortChildren(true);
        m_root.addChild(m_favNodes);

        m_freqNodes = new CopyingCategory("freq", "Most frequently used nodes", pluginID);
        m_freqNodes.setIcon(ImageRepository.getIconImage(SharedImages.FavoriteNodesFrequentlyUsed));
        m_freqNodes.setAfterID("fav");
        m_freqNodes.setSortChildren(false);
        m_root.addChild(m_freqNodes);

        m_lastNodes = new CopyingCategory("last", "Last used nodes", pluginID);
        m_lastNodes.setIcon(ImageRepository.getIconImage(SharedImages.FavoriteNodesLastUsed));
        m_lastNodes.setAfterID("freq");
        m_lastNodes.setSortChildren(false);
        m_root.addChild(m_lastNodes);

        loadFavorites();
    }

    /**
     *
     * @param node adds this node to the favorite nodes category
     */
    public void addFavoriteNode(final NodeTemplate node) {
        m_favNodes.addChild((NodeTemplate)node.deepCopy());
    }

    /**
     *
     * @param node removes this node from the favorites
     */
    public void removeFavoriteNode(final NodeTemplate node) {
        m_favNodes.removeChild(node);
    }

    /**
     * Updates the categories most frequent and last used with the information from the {@link NodeUsageRegistry}.
     */
    public void updateNodes() {
        updateLastUsedNodes();
        updateFrequentUsedNodes();
    }

    /**
     * Updates last used nodes.
     */
    public void updateLastUsedNodes() {
        // update last used
        m_lastNodes.removeAllChildren();
        m_lastNodes.addAllChildren(NodeUsageRegistry.getLastUsedNodes());
    }

    /**
     * Updates most frequently used nodes.
     */
    public void updateFrequentUsedNodes() {
        // update most frequent
        m_freqNodes.removeAllChildren();
        m_freqNodes.addAllChildren(NodeUsageRegistry.getMostFrequentNodes());
    }

    /**
     * Saves the ids of the favorite nodes to the state location of the plugin.
     */
    public void saveFavoriteNodes() {
        XMLMemento memento = XMLMemento.createWriteRoot(TAG_FAVORITES);
        saveFavoriteNodes(memento);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(getFavoriteNodesFile()), Charset.forName("UTF-8"));
            memento.save(writer);
        } catch (IOException ioe) {
            LOGGER.error("Problems writing file for FavoriteNodes: ", ioe);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ioe) {
                LOGGER.error("Error closing input stream for FavoriteNodes ", ioe);
            }
        }
    }

    private void saveFavoriteNodes(final XMLMemento memento) {
        // personal favorites
        IMemento favNodes = memento.createChild(TAG_PERSONAL_FAVS);
        for (IRepositoryObject reposObj : m_favNodes.getChildren()) {
            IMemento item = favNodes.createChild(TAG_FAVORITE);
            item.putString(TAG_NODE_ID, ((NodeTemplate)reposObj).getID());
        }
        // most frequent
        IMemento freqNodes = memento.createChild(TAG_MOST_FREQUENT);
        NodeUsageRegistry.saveFrequentNodes(freqNodes);
        // last used
        IMemento lastUsedNodes = memento.createChild(TAG_LAST_USED);
        NodeUsageRegistry.saveLastUsedNodes(lastUsedNodes);
    }

    private File getFavoriteNodesFile() {
        return KNIMEUIPlugin.getDefault().getStateLocation().append("favoriteNodes.xml").toFile();
    }

    private void loadFavorites() {
        // load the personal favorites
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(getFavoriteNodesFile()), Charset.forName("UTF-8"));
            loadFavoriteNodes(XMLMemento.createReadRoot(reader));
        } catch (FileNotFoundException fnf) {
            // no favorites saved
        } catch (Exception e) {
            LOGGER.error("Failed to load favorite nodes file", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                LOGGER.error("Failed to close favorite nodes file", ioe);
            }
        }
    }

    private void loadFavoriteNodes(final XMLMemento favoriteNodes) {
        IMemento favNodes = favoriteNodes.getChild(TAG_PERSONAL_FAVS);
        for (IMemento favNode : favNodes.getChildren(TAG_FAVORITE)) {
            String id = NodeUsageRegistry.fixNodeTemplateId(favNode.getString(TAG_NODE_ID));
            NodeTemplate node = RepositoryManager.INSTANCE.getNodeTemplate(id);
            if (node != null) {
                addFavoriteNode(node);
            }
        }
        IMemento freqNodes = favoriteNodes.getChild(TAG_MOST_FREQUENT);
        NodeUsageRegistry.loadFrequentNodes(freqNodes);
        IMemento lastNodes = favoriteNodes.getChild(TAG_LAST_USED);
        NodeUsageRegistry.loadLastUsedNodes(lastNodes);
        updateNodes();
    }

    /**
     * The category class for the favorite categories. It copies the given {@link AbstractRepositoryObject} before
     * adding them to itself, this is done to preserve the parent of the added child. Since #hashCode and
     * #equals(Object) are implemented by the subclasses of {@link AbstractRepositoryObject} there should be no side
     * effects.
     *
     * @author Marcel Hanser
     */
    private static class CopyingCategory extends Category {

        public CopyingCategory(final String id, final String name, final String contributingPlugin) {
            super(id, name, contributingPlugin);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean addChild(final AbstractRepositoryObject child) {
            return super.addChild((AbstractRepositoryObject)child.deepCopy());
        }
    }
}
