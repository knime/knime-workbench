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
 *   ${date} (${user}): created
 */
package org.knime.workbench.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.extension.CategoryExtension;
import org.knime.core.node.extension.InvalidNodeFactoryExtensionException;
import org.knime.core.node.extension.NodeFactoryExtension;
import org.knime.core.node.extension.NodeFactoryExtensionManager;
import org.knime.core.node.extension.NodeSetFactoryExtension;
import org.knime.core.node.workflow.FileWorkflowPersistor;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.core.util.Pair;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.DefaultNodeTemplate;
import org.knime.workbench.repository.model.DynamicNodeTemplate;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.osgi.framework.Bundle;

/**
 * Factory for creation of repository objects from
 * <code>IConfigurationElement</code> s from the Plugin registry.
 *
 * @author Florian Georg, University of Konstanz
 */
public final class RepositoryFactory {
    private RepositoryFactory() {
        // hidden constructor (utility class)
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RepositoryFactory.class);


    /**
     * Creates a new node repository object. Throws an exception, if this fails
     *
     * @param nodeFactoryExtension from {@link NodeFactoryExtensionManager}.
     * @return NodeTemplate object to be used within the repository.
     * @throws InvalidNodeFactoryExtensionException if the element is not compatible (e.g.
     *             wrong attributes, or factory class not found)
     */
    @SuppressWarnings("unchecked")
    static Pair<DefaultNodeTemplate, Boolean> createNode(final NodeFactoryExtension nodeFactoryExtension)
        throws InvalidNodeFactoryExtensionException {
        // Try to load the node factory class...
        NodeFactory<? extends NodeModel> factory = nodeFactoryExtension.getFactory();
        boolean isDeprecated = factory.isDeprecated();

        String pluginID = nodeFactoryExtension.getPlugInSymbolicName();
        String categoryPath = nodeFactoryExtension.getCategoryPath();
        DefaultNodeTemplate node = new DefaultNodeTemplate((Class<NodeFactory<? extends NodeModel>>)factory.getClass(),
            factory.getNodeName(), pluginID, categoryPath, factory.getType());
        node.setAfterID(nodeFactoryExtension.getAfterID());

        if (!Boolean.getBoolean("java.awt.headless")) {
            // Load images from declaring plugin
            Image icon = ImageRepository.getIconImage(factory);
            node.setIcon(icon);
        }

        return Pair.create(node, Boolean.valueOf(isDeprecated));
    }

    /**
     *
     * @param configuration content of the extension
     * @return a meta node template
     */
    public static MetaNodeTemplate createMetaNode(
            final IConfigurationElement configuration) {
        String id = configuration.getAttribute("id");
        String name = configuration.getAttribute("name");
        String workflowDir = configuration.getAttribute("workflowDir");
        String after = configuration.getAttribute("after");
        String iconPath = configuration.getAttribute("icon");
        String categoryPath = configuration.getAttribute("category-path");
        String pluginId =
                configuration.getDeclaringExtension().getNamespaceIdentifier();
        String description = configuration.getAttribute("description");

        WorkflowManagerUI manager = loadMetaNode(pluginId, workflowDir);
        if (manager == null) {
            LOGGER.error("MetaNode  " + name + " could not be loaded. "
                    + "Skipped.");
            return null;
        }
        MetaNodeTemplate template =
                new MetaNodeTemplate(id, name, categoryPath, configuration.getContributor().getName(), manager);
        if (after != null && !after.isEmpty()) {
            template.setAfterID(after);
        }
        if (description != null) {
            template.setDescription(description);
        }
        if (!Boolean.getBoolean("java.awt.headless")) {
            // Load images from declaring plugin
            Image icon = null;
            if (iconPath != null) {
                icon = ImageRepository.getIconImage(pluginId, iconPath);
            }
            if (icon == null) {
                LOGGER.coding("Icon '" + iconPath + "' for metanode "
                        + categoryPath + "/" + name + " does not exist");
                icon = ImageRepository.getIconImage(SharedImages.DefaultMetaNodeIcon);
            }
            template.setIcon(icon);
        }
        return template;
    }

    private static WorkflowManagerUI loadMetaNode(final String pluginId,
            final String workflowDir) {
        LOGGER.debug("found pre-installed template " + workflowDir);

        Bundle bundle = Platform.getBundle(pluginId);
        URL url = FileLocator.find(bundle, new Path(workflowDir), null);

        if (url != null) {
            try {
                File f = new File(FileLocator.toFileURL(url).getFile());
                LOGGER.debug("meta node template name: " + f.getName());
                WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true) {
                    /** {@inheritDoc} */
                    @Override
                    public String getDotKNIMEFileName() {
                        return WorkflowPersistor.WORKFLOW_FILE;
                    }
                };
                // don't lock workflow dir
                FileWorkflowPersistor persistor =
                        WorkflowManager.createLoadPersistor(f, loadHelper);

                WorkflowManager metaNode = WorkflowManager.META_NODE_ROOT.load(persistor, new ExecutionMonitor(),
                                false).getWorkflowManager();
                return WorkflowManagerWrapper.wrap(metaNode);
            } catch (CanceledExecutionException cee) {
                LOGGER.error("Unexpected canceled execution exception", cee);
            } catch (Exception e) {
                LOGGER.error("Failed to load meta workflow repository", e);
            }
        }
        return null;
    }

    /**
     * Creates a new category object. Throws an exception, if this fails
     *
     * @param root The root to insert the category in
     * @param element Configuration element from the contributing plugin
     * @return Category object to be used within the repository.
     * @throws IllegalArgumentException If the element is not compatible (e.g. wrong attributes)
     */
    static Category createCategory(final Root root, final CategoryExtension catExtension) {
        String id = catExtension.getLevelId();

        // get the id of the contributing plugin
        String pluginID = catExtension.getContributingPlugin();
        boolean locked = catExtension.isLocked();

        Category cat = new Category(id, catExtension.getName(), pluginID, locked);
        cat.setDescription(catExtension.getDescription());
        cat.setAfterID(catExtension.getAfterID());
        var path = catExtension.getPath();
        cat.setPath(path);
        if (!Boolean.getBoolean("java.awt.headless")) {
            cat.setIcon(findCategoryIcon(catExtension.getIcon(), pluginID, catExtension.getCompletePath()));
        }

        //
        // Insert in proper location
        //
        IContainerObject container = findParentContainer(root, path);

        if (canAdd(pluginID, container)) {
            container.addChild(cat);
        } else {
            LOGGER.errorWithFormat("Locked parent category for category %s: %s. Category will NOT be added!",
                cat.getID(), cat.getPath());
        }

        return cat;
    }

    private static Image findCategoryIcon(final String icon, final String pluginID, final String completeCategoryPath) {
        if (icon == null) {
            return defaultCategoryIcon();
        }

        // Find the URL to the icon
        final Path iconPath = new Path(icon);
        final URL iconUrl;
        if (iconPath.isAbsolute()) {
            try {
                iconUrl = iconPath.toFile().toURI().toURL();
            } catch (final MalformedURLException e) {
                LOGGER.error(
                    String.format("Icon '%s' for category %s could not be resolved.", icon, completeCategoryPath), e);
                return defaultCategoryIcon();
            }
        } else {
            // Relative path from the plugin root
            iconUrl = FileLocator.find(Platform.getBundle(pluginID), iconPath, null);
        }

        // Get the image from the ImageRepository
        Image img = null;
        if (iconUrl != null) {
            img = ImageRepository.getIconImage(iconUrl);
        }
        if (img != null) {
            return img;
        }

        // Image was null -> Log a coding error and return the default icon
        LOGGER.codingWithFormat("Icon '%s' for category %s does not exist", icon, completeCategoryPath);
        return defaultCategoryIcon();
    }

    private static Image defaultCategoryIcon() {
        return ImageRepository.getIconImage(SharedImages.DefaultCategoryIcon);
    }

    static boolean canAdd(final String pluginID, final IContainerObject container) {
        String parentPluginId = container.getContributingPlugin();
        return !container.isLocked() || // it's generally allowed to add to the category
            pluginID.equals(parentPluginId) || // the child is from the same plugin as the category
            pluginID.startsWith("org.knime.") || // the child is contributed by KNIME
            pluginID.startsWith("com.knime.") || // the child is contributed by KNIME
            RepositoryManager.isPluginIdFromSameVendor(parentPluginId, pluginID); // the child is from the same vendor
    }

    private static IContainerObject findParentContainer(final Root root, String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // split the path
        String[] segments = path.split("/");
        // start at root
        IContainerObject container = root;

        for (int i = 0; i < segments.length; i++) {
            IRepositoryObject obj = container.getChildByID(segments[i], false);
            if (obj == null) {
                throw new IllegalArgumentException(
                    "The segment '" + segments[i] + "' in path '" + path + "' does not exist!");
            }
            // continue at this level
            container = (IContainerObject)obj;
        }
        return container;
    }

    //
    // little helper, returns a default if s==null
    private static String str(final String s, final String defaultString) {
        return s == null ? defaultString : s;
    }

    /**
     * Creates the set of dynamic node templates.
     *
     * @param set the node set factory extension to load the nodes from
     * @param root the root to add the missing categories in
     * @param isIncludeDeprecated if deprecated nodes are to be included in the result collection
     * @return the created dynamic node templates
     */
    public static Collection<DynamicNodeTemplate> createNodeSet(
            final NodeSetFactoryExtension set, final Root root, final boolean isIncludeDeprecated) {
        String iconPath = set.getDefaultCategoryIconPath().orElse(null);

        // Try to load the node set factory class...
        NodeSetFactory nodeSet = set.getNodeSetFactory();
        if (nodeSet.isHidden()) {
            return Collections.emptyList();
        }

        Collection<DynamicNodeTemplate> dynamicNodeTemplates = new ArrayList<>();

        // for all nodes in the node set
        for (String factoryId : set.getNodeFactoryIds()) {
            // Try to load the node factory class...
            Optional<NodeFactory<? extends NodeModel>> factoryOptional = set.getNodeFactory(factoryId);
            if (!factoryOptional.isPresent()) {
                continue; // error handling done elsewhere
            }

            NodeFactory<? extends NodeModel> factory = factoryOptional.get();

            // DynamicNodeFactory implementations can set deprecation independently from extension
            if ((set.isDeprecated() || factory.isDeprecated()) && !isIncludeDeprecated ) {
                continue;
            }

            String categoryPath = nodeSet.getCategoryPath(factoryId);
            NodeType nodeType = factory.getType();

            @SuppressWarnings("unchecked")
            DynamicNodeTemplate node = new DynamicNodeTemplate(set,
                (Class<? extends NodeFactory<? extends NodeModel>>)factory.getClass(), factoryId,
                factory.getNodeName(), categoryPath, nodeType);

            node.setAfterID(nodeSet.getAfterID(factoryId));

            if (!Boolean.getBoolean("java.awt.headless")) {
                Image icon = ImageRepository.getIconImage(factory);
                node.setIcon(icon);
            }

            dynamicNodeTemplates.add(node);

            String pluginID = set.getPlugInSymbolicName();

            //
            // Insert in proper location, create all categories on
            // the path
            // if not already there
            //
            String path = node.getCategoryPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            // split the path
            String[] segments = path.split("/");
            // start at root
            IContainerObject container = root;
            String currentPath = "";
            for (int i = 0; i < segments.length; i++) {
                IRepositoryObject obj = container.getChildByID(segments[i], false);
                currentPath += segments[i];
                if (obj == null) {
                    if (canAdd(pluginID, container)) {
                        LOGGER.warnWithFormat("Category %s is not registered properly. "
                            + "Use the categorysets extension point to register categories dynamically "
                            + "(node factory %s, node set factory: %s).", currentPath, factoryId, set);
                        Category cat =
                            createCategory(pluginID, segments[i], "", segments[i], "", iconPath, currentPath);
                        // append the newly created category to the container
                        container.addChild(cat);
                        obj = cat;
                    } else {
                        LOGGER.errorWithFormat("Locked parent category for category %s. Category will NOT be added!",
                            currentPath);
                        break;
                    }
                }
                currentPath += "/";
                // continue at this level
                container = (IContainerObject)obj;
            }

        } // for node sets

        return dynamicNodeTemplates;

    }

    /* Little helper to create a category */
    private static Category createCategory(final String pluginID,
            final String categoryID, final String description,
            final String name, final String afterID, final String icon,
            final String categoryPath) {

        Category cat = new Category(categoryID, str(name, "!name is missing!"), pluginID);
        cat.setDescription(str(description, ""));
        cat.setAfterID(str(afterID, ""));
        String path = str(categoryPath, "/");
        cat.setPath(path);
        if (!Boolean.getBoolean("java.awt.headless")) {
            Image img;
            if (icon == null) {
                img = ImageRepository.getIconImage(SharedImages.DefaultCategoryIcon);
            } else {
                img = ImageRepository.getIconImage(pluginID, icon);
                if (img == null) {
                    LOGGER.coding(
                        "Icon '" + icon + "' for category " + cat.getPath() + "/" + cat.getName() + " does not exist");
                    img = ImageRepository.getIconImage(SharedImages.DefaultCategoryIcon);
                }
            }
            cat.setIcon(img);
        }

        return cat;
    }
}
