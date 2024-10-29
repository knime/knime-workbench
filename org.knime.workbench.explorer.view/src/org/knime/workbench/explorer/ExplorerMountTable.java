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
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPoint;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.knime.core.workbench.mountpoint.api.events.MountPointEvent;
import org.knime.core.workbench.mountpoint.api.events.MountPointListener;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author ohl, University of Konstanz
 */
public final class ExplorerMountTable {
    /**
     * The property for changes on mount points IPropertyChangeListener can
     * register for.
     */
    public static final String MOUNT_POINT_PROPERTY = "MOUNT_POINTS";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExplorerMountTable.class);

    private static final String PLUGIN_ID = FrameworkUtil.getBundle(ExplorerMountTable.class).getSymbolicName();

    private static final Map<IPropertyChangeListener, MountPointListener> CHANGE_LISTENERS = new ConcurrentHashMap<>();

    private ExplorerMountTable() {
        // hiding constructor of utility class
    }

    /**
     * Valid mount IDs must comply with the hostname restrictions. That is, they
     * must only contain a-z, A-Z, 0-9 and '.' or '-'. They must not start with
     * a number, dot or a hyphen and must not end with a dot or hyphen.
     *
     * @param id the id to test
     * @return true if the id is valid (in terms of contained characters)
     *         independent of it may already be in use.
     */
	public static boolean isValidMountID(final String id) {
        return WorkbenchConstants.isValidMountID(id);
    }

    /**
     * Returns a list of content providers that could be added (that is that
     * allow multiple instances or are not contained in a provided list). The list contains the
     * factory objects. Their toString method should return a useful name.
     * Use this method for intermediate determination of addable content providers (e.g. in preferences)
     * @param existingProviderIDs a list with given content provider IDs
     * @return a list of available content providers
     * @since 6.0
     */
    public static List<AbstractContentProviderFactory>
        getAddableContentProviders(final List<String> existingProviderIDs) {
        return WorkbenchMountTable.getAddableMountPointDefinitions() //
            .stream() //
            .filter(def -> def.supportsMultipleInstances() || !existingProviderIDs.contains(def.getTypeIdentifier())) //
            .filter(def -> !def.isTemporaryMountPoint()) //
            .map(def -> ExplorerMountTable.getContentProviderFactory(def.getTypeIdentifier())) //
            .filter(fac -> fac != null) // some "new" types don't have a representation in the legacy explorer
            .toList();
    }

    /**
     * Returns the content provider factory for the provided id.
     *
     * @param factoryID the id of the factory
     *
     * @return The content provider factory for the provided id, null if id does not exist.
     */
    public static AbstractContentProviderFactory getContentProviderFactory(final String factoryID) {
        return CONTENT_FACTORIES.get(factoryID);
    }

    /**
     * Retrieves the legacy content provider for the given mount point only if it is known and has been previously
     * registered via {@link #toAbstractContentProvider(WorkbenchMountPoint)}.
     *
     * @param mp workbench mount point, not null.
     * @return abstract content provider if available, an empty Optional otherwise
     * @since 9.0
     */
    public static Optional<AbstractContentProvider> toAbstractContentProviderIfKnown(final WorkbenchMountPoint mp) {
        final var fac = CONTENT_FACTORIES.get(mp.getType().getTypeIdentifier());
        return Optional.ofNullable(fac).map(f -> mp.getProvider(AbstractContentProvider.class, () -> null));
    }

    /**
     * Retrieves the legacy content provider for the given mount point. If there is a corresponding factory registered,
     * it will also register the legacy content provider. It only returns an empty Optional if the factory is not
     * registered.
     *
     * @param mp workbench mount point, not null.
     * @return abstract content provider if available, an empty Optional otherwise
     * @since 9.0
     */
    public static Optional<AbstractContentProvider> toAbstractContentProvider(final WorkbenchMountPoint mp) {
        // resolve content factory and mount ID at this point, but defer creating the provider until it is needed
        final var fac = CONTENT_FACTORIES.get(mp.getType().getTypeIdentifier());
        // this call will also register the legacy content provider
        // AbstractContentProvider.class here (or SpaceProviders.class on the modern side)
        return Optional.ofNullable(fac)
            .map(f -> mp.getProvider(AbstractContentProvider.class, () -> f.createContentProvider(mp)));
    }

    private static final Predicate<AbstractContentProvider> IS_NOT_REMOTE = provider -> !provider.isRemote();

    private static final Predicate<AbstractContentProvider> IS_NOT_TEMP_SPACE =
        provider -> !provider.getFactory().getMountPointType().isTemporaryMountPoint();


    /**
     * @return a list of all mount IDs currently in use and not hidden.
     * @since 6.4
     */
    public static List<String> getAllVisibleMountIDs() {
        return WorkbenchMountTable.withMounted(mounted ->
            mounted.stream() //
                .map(ExplorerMountTable::toAbstractContentProvider) //
                .flatMap(Optional::stream) //
                .filter(IS_NOT_TEMP_SPACE) //
                .map(AbstractContentProvider::getMountID) //
                .toList());
    }

    /**
     * @return a list of all local mount IDs currently in use and not hidden
     * @since 8.4
     */
    public static List<String> getAllVisibleLocalMountIDs() {
        return WorkbenchMountTable.withMounted(mounted ->
            mounted.stream() //
                .map(ExplorerMountTable::toAbstractContentProvider) //
                .flatMap(Optional::stream) //
                .filter(IS_NOT_REMOTE.and(IS_NOT_TEMP_SPACE)) //
                .map(AbstractContentProvider::getMountID) //
                .toList());
    }

    /**
     * @return a map with all currently mounted content providers with their mount ID (including the temp space that
     * should never be shown to the user).
     * @since 6.4
     */
    public static Map<String, AbstractContentProvider> getMountedContentInclTempSpace() {
        return WorkbenchMountTable.withMounted(mounted -> mounted.stream() //
            .map(ExplorerMountTable::toAbstractContentProvider) //
            .flatMap(Optional::stream) //
            .collect(Collectors.toMap(AbstractContentProvider::getMountID, Function.identity(), (a, b) -> a,
                LinkedHashMap::new)));
    }

    /**
     * @return a map with the currently mounted content providers with their mount ID (temp space is not included).
     */
    public static Map<String, AbstractContentProvider> getMountedContent() {
        return WorkbenchMountTable.withMounted(mounted -> mounted.stream() //
            .map(ExplorerMountTable::toAbstractContentProvider) //
            .flatMap(Optional::stream) //
            .filter(IS_NOT_TEMP_SPACE) //
            .collect(Collectors.toMap(AbstractContentProvider::getMountID, Function.identity(), (a, b) -> a,
                LinkedHashMap::new)));
    }

    /**
     * The content provider currently mounted with the specified ID - or an empty optional
     * if the ID is not used as mount point or no such representation as AbstractContentProvider exists.
     *
     * @param mountID the mount point
     * @return that content provider or an empty optional if no such mount point exists
     * @since 9.0
     */
    public static Optional<AbstractContentProvider> getContentProvider(final String mountID) {
        return WorkbenchMountTable.getMountPoint(mountID).flatMap(ExplorerMountTable::toAbstractContentProvider);
    }

    /**
     *
     * @return the file system representing the content of the mount table.
     */
    public static ExplorerFileSystem getFileSystem() {
        return ExplorerFileSystem.INSTANCE;
    }

    /**
     * Creates a new dir in the explorer temp provider. The dir is deleted on exit. The returned file store has a valid
     * mount id - which is not shown in the explorer view.
     *
     * @param prefix to the name of the returned file store
     * @return a newly created temporary directory. Deleted on VM exit.
     * @throws CoreException if it couldn't create the temp dir
     * @since 6.4
     */
    public static LocalExplorerFileStore createExplorerTempDir(final String prefix) throws CoreException {
        Map<String, AbstractContentProvider> allMounts = getMountedContentInclTempSpace();
        AbstractContentProvider tempProvider = null;
        for (Entry<String, AbstractContentProvider> e : allMounts.entrySet()) {
            if (e.getValue().getFactory().getMountPointType().isTemporaryMountPoint()) {
                tempProvider = e.getValue();
                break;
            }
        }
        if (tempProvider == null) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                "No temporary space mounted. Temp dir can't be created."));
        }
        AbstractExplorerFileStore tmpRoot = tempProvider.getRootStore();
        File localTmp;
        try {
            localTmp = tmpRoot.toLocalFile();
        } catch (final CoreException e1) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                "Could not get the local file representation for " + tmpRoot, e1));
        }
        try {
            final var tmpDir = FileUtil.createTempDir(prefix, localTmp,
                false /* entire temp mount point is deleted on exit */);
            AbstractExplorerFileStore tmpFileStore = tmpRoot.getChild(tmpDir.getName());
            if (!(tmpFileStore instanceof LocalExplorerFileStore)) {
                throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                    "Created temp dir is of incorrect type (internal implementation error!)"
                            + tmpFileStore.getClass().getCanonicalName()));
            }
            return (LocalExplorerFileStore)tmpFileStore;
        } catch (IOException e1) {
            throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID,
                "Could not create temporary directory in " + tmpRoot, e1));
        }
    }

    /* ------------- read the extension point ------------------------------ */

    /**
     * Stores all content provider factories (registered with the extension
     * point) mapped to their ID.
     */
    private static final TreeMap<String, AbstractContentProviderFactory> CONTENT_FACTORIES = new TreeMap<>();

    private static final TreeMap<String, String> FACTORY_NAMES = new TreeMap<>();

    static {
        // read out the extension point now
        collectContentProviderFactories();
    }

    /**
     * Some extension point parameters.
     */
    private static final String EXT_POINT_ID =
            "org.knime.workbench.explorer.contentprovider";

    private static final String ATTR_CONT_PROV_FACT = "ContentProviderFactory";

    private static void collectContentProviderFactories() {

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            LOGGER.error("Corrupt installation (plugin.xml) - no view content "
                    + "provider is registered. Using the local workspace"
                    + " content provider.");
            // let's throw in the local workspace content provider
            LocalWorkspaceContentProviderFactory lwcpf = new LocalWorkspaceContentProviderFactory();
            final var id = lwcpf.getMountPointType().getTypeIdentifier();
            CONTENT_FACTORIES.put(id, lwcpf);
            FACTORY_NAMES.put(lwcpf.toString(), id);
            return;
        }

        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String contProvFact = elem.getAttribute(ATTR_CONT_PROV_FACT);
            String decl = elem.getDeclaringExtension().getUniqueIdentifier();

            if (contProvFact == null || contProvFact.isEmpty()) {
                LOGGER.error("The extension '" + decl
                        + "' doesn't provide the required attribute '"
                        + ATTR_CONT_PROV_FACT + "'");
                LOGGER.error("Extension " + decl + " ignored.");
                continue;
            }

            // try instantiating the content provider factory.
            AbstractContentProviderFactory instance = null;
            try {
                instance = (AbstractContentProviderFactory)elem.createExecutableExtension(ATTR_CONT_PROV_FACT);
            } catch (Throwable t) {
                if (t instanceof OutOfMemoryError oome) {
                    throw oome;
                }

                LOGGER.error(
                    "Problems during initialization of content provider factory (with id '" + contProvFact + "'.)", t);
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.");
                }
            }

            if (instance != null) {
                final var mountPointType = instance.getMountPointType();
                final var id = mountPointType.getTypeIdentifier();
                CONTENT_FACTORIES.put(id, instance);
                FACTORY_NAMES.put(instance.toString(), id);
            }
        }

    }

    /*---------------------------------------------------------------*/
    /**
     * Adds a property change listener for mount changes.
     *
     * @param listener the property change listener to add
     */
    public static void addPropertyChangeListener(
            final IPropertyChangeListener listener) {
        final var listenerAdapter = new MountPointListener() {

            @Override
            public void mountPointRemoved(final MountPointEvent event) {
                final var mp = event.getMountPoint();
                listener.propertyChange(new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY, mp.getMountID(), null));
            }

            @Override
            public void mountPointAdded(final MountPointEvent event) {
                final var mp = event.getMountPoint();
                listener.propertyChange(new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY, null, mp.getMountID()));
            }
        };
        CHANGE_LISTENERS.put(listener, listenerAdapter);
        WorkbenchMountTable.addListener(listenerAdapter);
    }

    /**
     * Removes the given listener. Calling this method has no affect if the
     * listener is not registered.
     *
     * @param listener a property change listener
     */
    public static void removePropertyChangeListener(
            final IPropertyChangeListener listener) {
        final var listenerAdapter = CHANGE_LISTENERS.remove(listener);
        if (listenerAdapter != null) {
            WorkbenchMountTable.removeListener(listenerAdapter);
        }
    }

    /**
     * Updates the settings of all providers in the preferences. Some providers may get additional attributes once they
     * are used (e.g. the REST address for server mount points) which should be persisted.
     * @since 7.3
     */
    public static void updateProviderSettings() {
        WorkbenchMountTable.updateProviderSettings();
    }

    /**
     * Returns the collected ContentProviderFactories.
     *
     * @return the collected ContentProviderFactories
     * @since 8.2
     */
    public static TreeMap<String, AbstractContentProviderFactory> getContentProviderFactories() {
        return CONTENT_FACTORIES;
    }
}
