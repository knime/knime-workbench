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
 * -------------------------------------------------------------------
 *
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.knime.core.customization.APCustomization;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.extension.CategoryExtension;
import org.knime.core.node.extension.CategorySetFactory;
import org.knime.core.node.extension.InvalidNodeFactoryExtensionException;
import org.knime.core.node.extension.NodeFactoryExtension;
import org.knime.core.node.extension.NodeFactoryExtensionManager;
import org.knime.core.node.extension.NodeSetFactoryExtension;
import org.knime.core.node.workflow.FileNativeNodeContainerPersistor;
import org.knime.core.util.Pair;
import org.knime.workbench.repository.model.AbstractContainerObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.DefaultNodeTemplate;
import org.knime.workbench.repository.model.DynamicNodeTemplate;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.osgi.framework.Bundle;

/**
 * Manages the (global) KNIME Repository. This class collects all the
 * contributed extensions from the extension points and creates an arbitrary
 * model. The repository is created on-demand as soon as one of the three public
 * methods is called. Thus the first call can take some time to return.
 * Subsequent calls will return immediately with the full repository tree.
 *
 * @author Florian Georg, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 * @author Kai Franze, KNIME GmbH
 */
public final class RepositoryManager {
    /**
     * Listener interface for acting on events while the repository is read.
     *
     * @author Thorsten Meinl, University of Konstanz
     * @since 2.4
     */
    public interface Listener {
        /**
         * Called when a new category has been created.
         *
         * @param root the repository root
         * @param category the new category
         */
        public void newCategory(Root root, Category category);

        /**
         * Called when a new node has been created.
         *
         * @param root the repository root
         * @param node the new node
         */
        public void newNode(Root root, NodeTemplate node);

        /**
         * Called when a new meta node has been created.
         *
         * @param root the repository root
         * @param metanode the new category
         */
        public void newMetanode(Root root, MetaNodeTemplate metanode);
    }

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RepositoryManager.class);

    /** The singleton instance. */
    public static final RepositoryManager INSTANCE = new RepositoryManager();

    // ID of "category" extension point
    private static final String ID_CATEGORY
            = "org.knime.workbench.repository.categories";

    private static final String ID_META_NODE
            = "org.knime.workbench.repository.metanode";

    private static final String ID_CATEGORY_SET  = "org.knime.workbench.repository.categorysets";

    private final List<Listener> m_loadListeners = new CopyOnWriteArrayList<>();

    private final Root m_root = new Root();

    private final Map<String, NodeTemplate> m_nodesById = new HashMap<>();

    private final Root m_completeRoot = new Root();

    /**
     * Creates the repository model. This instantiates all contributed
     * category/node extensions found in the global Eclipse PluginRegistry, and
     * attaches them to the repository tree.
     */
    private RepositoryManager() {
    }

    private void readRepository(final IProgressMonitor monitor) {
        // Read in all node templates available
        assert !m_root.hasChildren();
        readCategories(monitor, m_root);
        if (monitor.isCanceled()) {
            return;
        }
        var isIncludeDeprecated = Boolean.getBoolean("knime.repository.deprecated.show"); // false in production code
        readNodes(monitor, m_root, isIncludeDeprecated);
        if (monitor.isCanceled()) {
            return;
        }
        readNodeSets(monitor, m_root, isIncludeDeprecated);
        if (monitor.isCanceled()) {
            return;
        }
        readMetanodes(monitor, m_root);
        if (monitor.isCanceled()) {
            return;
        }
        removeEmptyCategories(m_root);
        m_loadListeners.clear();
    }

    private void readCompleteRepository(final IProgressMonitor monitor) {
        assert !m_completeRoot.hasChildren();
        // Read in all node templates available
        readCategories(monitor, m_completeRoot);
        if (monitor.isCanceled()) {
            return;
        }
        readNodes(monitor, m_completeRoot, true);
        if (monitor.isCanceled()) {
            return;
        }
        readNodeSets(monitor, m_completeRoot, true);
        if (monitor.isCanceled()) {
            return;
        }
        readMetanodes(monitor, m_completeRoot);
        if (monitor.isCanceled()) {
            return;
        }
        removeEmptyCategories(m_completeRoot);
        m_loadListeners.clear();
    }

    private void readMetanodes(final IProgressMonitor monitor, final Root root) {
        // iterate over the meta node config elements
        // and create meta node templates
        IExtension[] metanodeExtensions = getExtensions(ID_META_NODE);
        for (IExtension mnExt : metanodeExtensions) {
            IConfigurationElement[] mnConfigElems = mnExt.getConfigurationElements();
            for (IConfigurationElement mnConfig : mnConfigElems) {
                if (monitor.isCanceled()) {
                    return;
                }
                try {
                    processConfigurationElements(mnConfig, root);
                } catch (Throwable t) {
                    String message = "MetaNode " + mnConfig.getAttribute("id") + "' from plugin '"
                        + mnConfig.getNamespaceIdentifier() + "' could not be created: " + t.getMessage();
                    var bundle = Platform.getBundle(mnConfig.getNamespaceIdentifier());
                    if ((bundle == null) || (bundle.getState() != Bundle.ACTIVE)) {
                        // if the plugin is null, the plugin could not
                        // be activated maybe due to a not
                        // activateable plugin
                        // (plugin class cannot be found)
                        message = message + " The corresponding plugin bundle could not be activated!";
                    }
                    LOGGER.error(message, t);
                }
            }
        }
    }

    private void processConfigurationElements(final IConfigurationElement mnConfig, final Root root) throws Throwable {
        MetaNodeTemplate metaNode = RepositoryFactory.createMetaNode(mnConfig);
        LOGGER.debug("Found meta node definition '" + metaNode.getID() + "': " + metaNode.getName());
        m_loadListeners.forEach(l -> l.newMetanode(root, metaNode));
        IContainerObject parentContainer = root.findContainer(metaNode.getCategoryPath());

        // If parent category is illegal, log an error and
        // append the node to the repository root.
        if (parentContainer == null) {
            LOGGER.warn("Invalid category-path for node " + "contribution: '" + metaNode.getCategoryPath()
                + "' - adding to root instead");
            root.addChild(metaNode);
        } else {
            // everything is fine, add the node to its parent
            // category
            parentContainer.addChild(metaNode);
        }
    }

    private Stream<CategoryExtension> extractDeclarationsFromSet(final IExtension categorySetExtension) { // NOSONAR: Method can't be static
        var pluginId = categorySetExtension.getNamespaceIdentifier();
        return Stream.of(categorySetExtension.getConfigurationElements())//
            .map(RepositoryManager::createFactory)//
            .filter(Objects::nonNull)//
            .map(CategorySetFactory::getCategories)//
            .flatMap(Collection::stream)//
            .filter(c -> filterCategoryExtensionForForeignPlugins(c, pluginId));
    }

    /** Filter out category declarations that claim to come from another group than the category set */
    private static boolean filterCategoryExtensionForForeignPlugins(final CategoryExtension category,
        final String pluginIdOfCategorySet) {
        if (pluginIdOfCategorySet.startsWith("org.knime.")) {
            // If the category set comes from KNIME, we know that the plugin ids
            // of the CategoryExtension elements are set correctly
            // => We allow all pluginIds in the category
            return true;
        }
        if (!isPluginIdFromSameVendor(pluginIdOfCategorySet, category.getContributingPlugin())) {
            LOGGER.errorWithFormat("The category %s of the category set from plugin %s has the pluginId %s."
                + "This is not allowed because category sets can only define categories of plugins from the same group.",
                category.getCompletePath(), pluginIdOfCategorySet, category.getContributingPlugin());
            return false;
        }
        return true;
    }

    private static CategorySetFactory createFactory(final IConfigurationElement factoryElement) {
        try {
            return (CategorySetFactory)factoryElement.createExecutableExtension("factory-class");
        } catch (CoreException e) {
            LOGGER.error(String.format("Failed to create CategorySetFactory from plugin '%s'.",
                factoryElement.getDeclaringExtension().getNamespaceIdentifier()), e);
            return null;
        }
    }

    private void readCategories(final IProgressMonitor monitor, final Root root) {
        var categories = new ArrayList<>(getCategoryEntriesFromExtPoint());

        // sort first by path-depth, so that everything is there in the
        // right order
        Collections.sort(categories, RepositoryManager::compareByPathDepth);

        for (var cat : categories) {
            if (monitor.isCanceled()) {
                return;
            }
            try {
                var category = RepositoryFactory.createCategory(root, cat);
                LOGGER.debugWithFormat("Found category extension '%s' on path '%s'", category.getID(),
                    category.getPath());
                for (Listener l : m_loadListeners) {
                    l.newCategory(root, category);
                }
            } catch (Exception ex) {
                LOGGER.error(String.format("Category '%s' from plugin '%s' could not be created in parent path '%s'.",
                    cat.getLevelId(), cat.getContributingPlugin(), cat.getPath()), ex);
            }
        }
    }

    private Collection<CategoryExtension> getCategoryEntriesFromExtPoint() {
        // categories extension point
        IExtension[] categoryExtensions = getExtensions(ID_CATEGORY);
        Stream<CategoryExtension> categoryExtPointEntries = Stream.of(categoryExtensions)//
            .flatMap(e -> Stream.of(e.getConfigurationElements()))//
            .map(CategoryExtension::fromConfigurationElement);

        // category sets extension point
        IExtension[] categorySetExtensions = getExtensions(ID_CATEGORY_SET);
        Stream<CategoryExtension> categorySetFactoryExtPointEntries = Stream.of(categorySetExtensions)//
            .flatMap(this::extractDeclarationsFromSet);

        return Stream.concat(categoryExtPointEntries, categorySetFactoryExtPointEntries)//
            .sequential()//
            .collect(Collectors.toMap(CategoryExtension::getCompletePath, Function.identity(),
                RepositoryManager::takeLeftAndReportDuplicateIfNameDiffers))
            .values();
    }

    private static int compareByPathDepth(final CategoryExtension o1, final CategoryExtension o2) {
        String element1 = o1.getPath();
        String element2 = o2.getPath();
        if (Objects.equals(element1, element2)) {
            return 0;
        } else if (element1 == null) {
            return -1;
        } else if (element2 == null) {
            return 1;
        } else {
            var countSlashes1 = 0;
            for (var i = 0; i < element1.length(); i++) {
                if (element1.charAt(i) == '/') {
                    countSlashes1++;
                }
            }

            var countSlashes2 = 0;
            for (var i = 0; i < element2.length(); i++) {
                if (element2.charAt(i) == '/') {
                    countSlashes2++;
                }
            }
            return countSlashes1 - countSlashes2;
        }
    }

    private static CategoryExtension takeLeftAndReportDuplicateIfNameDiffers(final CategoryExtension left,
        final CategoryExtension right) {
        assert left.getCompletePath().equals(right.getCompletePath()) : "Should only be called in case of a collision.";
        // only report removal if the names differ (otherwise users will not notice any difference
        // except for icon and description
        if (!left.getName().equals(right.getName())) {
            LOGGER.warnWithFormat(
                "Category '%s' was found twice. Names are '%s'(Plugin: %s) and '%s'(Plugin: %s). "
                    + "The category with name '%s' is ignored.",
                left.getCompletePath(), left.getName(), left.getContributingPlugin(), right.getName(),
                right.getContributingPlugin(), right.getName());
        }
        return left;
    }

    private void readNodes(final IProgressMonitor monitor, final Root root, final boolean isIncludeDeprecated) {
        IContainerObject uncategorized = findUncategorizedCategory(root);
        final APCustomization.Nodes nodesCustomization = KNIMERepositoryPlugin.getDefault().getCustomization().nodes();

        for (NodeFactoryExtension nodeFactoryExtension : NodeFactoryExtensionManager.getInstance()
            .getNodeFactoryExtensions()) {
            if (monitor.isCanceled()) {
                return;
            }

            try {
                if ((nodeFactoryExtension.isDeprecated() && !isIncludeDeprecated) || nodeFactoryExtension.isHidden()) {
                    // deprecate nodes are hidden, hidden ones are ignored too
                    continue;
                }

                Pair<DefaultNodeTemplate, Boolean> nodePair = RepositoryFactory.createNode(nodeFactoryExtension);
                DefaultNodeTemplate node = nodePair.getFirst();
                if (!nodesCustomization.isViewAllowed(node.getID())) {
                    continue; // hidden by AP customization
                }

                node.setDeprecated(nodeFactoryExtension.isDeprecated());
                Boolean isDeprecatedInNode = nodePair.getSecond();

                // nodeFactoryExtension.isDeprecated() - reads the flag from the plugin.xml
                // isDeprecatedInNode -- reads FooNodeFactory.xml header _AND_ plugin.xml
                //                                                             (injected via NodeFactoryExtension)
                // if they are different then the node is deprecated via the FooFactory.xml but not in the plugin.xml...
                if (nodeFactoryExtension.isDeprecated() != isDeprecatedInNode) {
                    LOGGER.codingWithFormat(
                        "%s \"%s\" is declared 'deprecated' in its node description but not in "
                            + "the extension point contribution (plug-in \"%s\")",
                        NodeFactory.class.getSimpleName(), nodeFactoryExtension.getFactoryClassName(),
                        nodeFactoryExtension.getPlugInSymbolicName());
                    if (!isIncludeDeprecated) {
                        continue;
                    }
                }

                LOGGER.debugWithFormat("Found node extension '%s': %s", node.getID(), node.getName());
                for (Listener l : m_loadListeners) {
                    l.newNode(root, node);
                }
                m_nodesById.put(node.getID(), node);

                // Ask the root to lookup the category-container located at
                // the given path
                IContainerObject parentContainer =
                        root.findContainer(node.getCategoryPath());

                // If parent category is illegal, log an error and append
                // the node to the repository root.
                if (parentContainer == null) {
                    LOGGER.coding("Unknown category for node " + node.getID() + " (plugin: "
                            + node.getContributingPlugin() + "): " + node.getCategoryPath()
                            + ". Node will be added to 'Uncategorized' instead");
                    uncategorized.addChild(node);
                } else {
                    String nodePluginId = nodeFactoryExtension.getPlugInSymbolicName();

                    if (RepositoryFactory.canAdd(nodePluginId, parentContainer)) {
                        // container not locked, or node and category from same plug-in
                        // or the vendor is the same (comparing the first two parts of the plug-in ids)
                        parentContainer.addChild(node);
                    } else {
                        LOGGER.coding("Locked category for node " + node.getID() + ": " + node.getCategoryPath()
                                    + ". Node will be added to 'Uncategorized' instead");
                        uncategorized.addChild(node);
                    }
                }

            } catch (InvalidNodeFactoryExtensionException t) {
                LOGGER.error(t.getMessage(), t);
            }

        } // for configuration elements
    }

    private static IContainerObject findUncategorizedCategory(final Root root) {
        IContainerObject uncategorized = root.findContainer("/uncategorized");
        if (uncategorized == null) {
            // this should never happen, but who knows...
            uncategorized = root;
        }
        return uncategorized;
    }

    static boolean isPluginIdFromSameVendor(final String pluginIdA, final String pluginIdB) {
        if (pluginIdA == null || pluginIdB == null) {
            return false;
        }

        int secondDotIndex = pluginIdA.indexOf('.', pluginIdA.indexOf('.') + 1);
        if (secondDotIndex == -1) {
            secondDotIndex = 0;
        }
        return pluginIdA.regionMatches(0, pluginIdB, 0, secondDotIndex);
    }


    private void readNodeSets(final IProgressMonitor monitor, final Root root, final boolean isIncludeDeprecated) {
        for (NodeSetFactoryExtension set : NodeFactoryExtensionManager.getInstance().getNodeSetFactoryExtensions()) {
            Collection<DynamicNodeTemplate> dynamicNodeTemplates =
                    RepositoryFactory.createNodeSet(set, root, isIncludeDeprecated);

            for (DynamicNodeTemplate node : dynamicNodeTemplates) {
                if (monitor.isCanceled()) {
                    return;
                }
                for (Listener l : m_loadListeners) {
                    l.newNode(root, node);
                }
                m_nodesById.put(node.getID(), node);

                // Ask the root to lookup the category-container located at the given path
                IContainerObject parentContainer = root.findContainer(node.getCategoryPath());

                // If parent category is illegal, log an error and append the node to the repository root.
                if (parentContainer != null
                    && RepositoryFactory.canAdd(node.getContributingPlugin(), parentContainer)) {
                    // everything is fine, add the node to its parent
                    // category
                    parentContainer.addChild(node);
                } else {
                    LOGGER.coding("Locked category for node " + node.getID() + ": " + node.getCategoryPath()
                        + ". Node will be added to 'Uncategorized' instead");
                    findUncategorizedCategory(root).addChild(node);
                }
            }
        }
    }

    /**
     * Returns the extensions for a given extension point.
     *
     * @param pointID The extension point ID
     *
     * @return The extensions
     */
    private static IExtension[] getExtensions(final String pointID) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(pointID);
        if (point == null) {
            throw new IllegalStateException("Invalid extension point : "
                    + pointID);

        }
        return point.getExtensions();
    }

    private static void removeEmptyCategories(
            final AbstractContainerObject treeNode) {
        for (IRepositoryObject object : treeNode.getChildren()) {
            if (object instanceof AbstractContainerObject) {
                AbstractContainerObject cat = (AbstractContainerObject)object;
                removeEmptyCategories(cat);
                if (!cat.hasChildren() && (cat.getParent() != null)) {
                    cat.getParent()
                            .removeChild((AbstractContainerObject)object);
                }
            }
        }
    }

    /**
     * Returns the repository root. If the repository has not yet read, it will
     * be created during the call. Thus the first call to this method can take
     * some time.
     *
     * @param monitor a progress monitor, mainly use for canceling; must not be
     *            <code>null</code>
     *
     * @return the root object
     */
    public synchronized Root getRoot(final IProgressMonitor monitor) {
        if (!m_root.hasChildren()) {
            readRepository(monitor);
        }
        return m_root;
    }

    /**
     * Returns the repository root. If the repository has not yet read, it will
     * be created during the call. Thus the first call to this method can take
     * some time.
     *
     * @return the root object
     */
    public synchronized Root getRoot() {
        return getRoot(new NullProgressMonitor());
    }

    /**
     * Returns the complete repository root. If the repository has not yet read, it will be created during the call.
     * Thus the first call to this method can take some time.
     *
     * <p>
     * Unlike {@link RepositoryManager#getRoot(IProgressMonitor)}, the {@code Root} returned by this method will contain
     * deprecated nodes.
     * </p>
     *
     * @param monitor a progress monitor, mainly use for canceling; must not be <code>null</code>
     *
     * @return the root object
     */
    public synchronized Root getCompleteRoot(final IProgressMonitor monitor) {
        if (!m_completeRoot.hasChildren()) {
            readCompleteRepository(monitor);
        }
        return m_completeRoot;
    }

    /**
     * Returns the complete repository root. If the repository has not yet read, it will be created during the call.
     * Thus the first call to this method can take some time.
     *
     * <p>
     * Unlike {@link RepositoryManager#getRoot()}, the {@code Root} returned by this method will contain deprecated
     * nodes.
     * </p>
     *
     * @return the root object
     */
    public synchronized Root getCompleteRoot() {
        return getCompleteRoot(new NullProgressMonitor());
    }

    /**
     * Adds a listener which is notified while the node repository is loaded.
     * The listener is automatically removed from the list once the node
     * repository is fully loaded.
     *
     * @param listener a listener
     */
    public void addLoadListener(final Listener listener) {
        m_loadListeners.add(listener); // NOSONAR: This collection will never be large
    }

    /**
     * Returns the node template with the given id, or <code>null</code> if no such node exists.
     *
     * @param id the node's id consisting of the factory's class name in case of the {@link NodeTemplate}, or in
     *            combination with the node's name ( <code>&#60;node-factory class name&#62;#&#60;node name&#62;</code>)
     *            in case of the {@link DynamicNodeTemplate}.
     * @return a node template or <code>null</code>
     * @since 2.4
     */
    public synchronized NodeTemplate getNodeTemplate(final String id) {
        if (!m_root.hasChildren() && !m_completeRoot.hasChildren()) {
            readRepository(new NullProgressMonitor());
        }
        return m_nodesById.get(id);
    }

    /**
     * Creates the node factory instance for the given fully-qualified factory class name.
     * Otherwise a respective exception will be thrown.
     * @param factoryClassName
     * @return a new node factory instance
     * @throws InvalidSettingsException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvalidNodeFactoryExtensionException
     *
     * @since 3.5
     */
    public static final synchronized NodeFactory<NodeModel> loadNodeFactory(final String factoryClassName)
        throws InvalidSettingsException, InstantiationException, IllegalAccessException,
        InvalidNodeFactoryExtensionException {
        return FileNativeNodeContainerPersistor.loadNodeFactory(factoryClassName);
    }
}
