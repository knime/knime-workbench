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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.workbench.mounts.WorkbenchMountPoint;
import org.knime.core.workbench.mounts.WorkbenchMountPointDefinition;
import org.knime.core.workbench.mounts.WorkbenchMountTable;
import org.knime.core.workbench.mounts.events.MountPointEvent;
import org.knime.core.workbench.mounts.events.MountPointListener;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.preferences.ExplorerPreferenceInitializer;
import org.knime.workbench.explorer.view.preferences.MountSettings;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

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

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerMountTable.class);

    private static final String PLUGIN_ID = FrameworkUtil.getBundle(ExplorerMountTable.class).getSymbolicName();

    private static final Map<IPropertyChangeListener, MountPointListener> CHANGE_LISTENERS = new ConcurrentHashMap<>();

    private ExplorerMountTable() {
        // hiding constructor of utility class
    }

    /**
     * Creates a new instance of the specified content provider. May open a user
     * dialog to get parameters needed by the provider factory. Returns null, if
     * the user canceled.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException if the mounting fails
     */
    public static AbstractContentProvider mount(final String mountID,
        final String providerID) throws IOException {
        return mount(mountID, providerID, null);
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
        return WorkbenchMountTable.isValidMountID(id);
    }

    /**
     * Throws an exception, if the specified id is not a valid mount id (in
     * terms of contained characters). (@see {@link #isValidMountID(String)})
     *
     * @param id to test
     * @throws IllegalArgumentException if the specified id is not a valid mount
     *             id (in terms of contained characters). (@see
     *             {@link #isValidMountID(String)})
     */
    public static void checkMountID(final String id) throws IllegalArgumentException {
        WorkbenchMountTable.checkMountID(id);
    }

    /**
     * Sorts the mount points in the same way as in the passed list. It makes
     * sure that if two entries A and B of the list are mounted and A comes
     * before B, the mount point A will appear before mount point B in the mount
     * table.
     *
     * @param mountIDs a list of mount ids
     */
    public static void setMountOrder(final List<String> mountIDs) {
        WorkbenchMountTable.setMountOrder(mountIDs);
    }

    /**
     * Creates a new instance of the specified content provider.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @param storage the stored data of the content provider
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException if the mounting fails
     */
    public static AbstractContentProvider mount(final String mountID, final String providerID, final String storage)
            throws IOException {
        // can be null, e.g. when we did need to mount but the user canceled
        return WorkbenchMountTable.withMounted(mounted -> getOrMount(mounted, mountID, providerID, storage));
    }

    private static AbstractContentProvider getOrMount(final Collection<WorkbenchMountPoint> mounted,
            final String mountID, final String providerID, final String storage) throws IOException {
        // check if it is already mounted under the ID and of this providerID
        final var wmpOpt = mounted.stream() //
                .filter(mp -> mp.getMountID().equals(mountID)) //
                .findFirst();
        final WorkbenchMountPoint wmp;
        if (wmpOpt.isEmpty()) {
            wmp = WorkbenchMountTable.mount(mountID, providerID, storage);
        } else {
            wmp = wmpOpt.get();
        }
        final var mountedTypeIdentifier = wmp.getDefinition().getTypeIdentifier();
        if (mountedTypeIdentifier.equals(providerID)) {
            // mountID fits and providerID, too
            return getContentProvider(wmp, storage);
        }

        // something is already mounted under the mount ID but from a different provider
        throw new IOException(
            "Mount ID \"%s\" is already in use by mounted content provided via \"%s\", but requested provider is \"%s\"." // NOSONAR don't break string formatting
                .formatted(mountID, mountedTypeIdentifier, providerID));
    }

    private static AbstractContentProvider getContentProvider(final WorkbenchMountPoint mp, final String storage) {
        // resolve content factory and mount ID at this point, but defer creating the provider until it is needed
        final var fac = CONTENT_FACTORIES.get(mp.getDefinition().getTypeIdentifier());
        final var mountID = mp.getMountID();
        // this call will also register the legacy content provider
        // AbstractContentProvider.class here (or SpaceProviders.class on the modern side)
        return mp.getProvider(AbstractContentProvider.class, content -> {
            final var optProvider = storage == null ? fac.tryCreateContentProvider(mountID)
                : fac.tryCreateContentProvider(mountID, storage);
            return optProvider.orElse(null);
        });
    }

    // TODO figure out when/where to call this method
    //      We want to sever link to fetchers backing content providers as soon as possible:
    //      - when we switch perspective from classic to modern, this should be called on all mounted mountpoints
    //      - when a filesystem is closed that uses the content provider, we can dispose it
    // Do we need a refcount for this?
    /**
     * Disposes the content provider for the specified mount point if it is currently mounted. This will leave the
     * mountpoint mounted, but the content provider will be disposed of and must be created by mounting again
     * (using any of the {@link #mount(String, String, String)} methods).
     *
     * @since 8.14
     */
    public static void dispose(final String mountID) {
        final var mp = WorkbenchMountTable.getMountPoint(mountID);
        if (mp != null) {
            mp.dispose(AbstractContentProvider.class);
        }
    }

    /**
     * @param mountID the id to unmount
     * @return true if unmounting was successful, false otherwise
     */
    public static boolean unmount(final String mountID) {
        return WorkbenchMountTable.unmount(mountID);
    }

    /**
     * Unmounts all MountPoints.
     */
    public static synchronized void unmountAll() {
        WorkbenchMountTable.unmountAll();
    }

    private static final Comparator<AbstractContentProviderFactory> DESC_PRIO =
            Comparator.comparingInt(AbstractContentProviderFactory::getSortPriority).reversed();

    /**
     * Returns a list of content providers that could be added (that is that
     * allow multiple instances or are not yet mounted). The list contains the
     * factory objects. Their toString method should return a useful name.
     *
     * @return a map of available content providers (key = name, value = ID).
     */
    public static List<AbstractContentProviderFactory> getAddableContentProviders() {
        return WorkbenchMountTable.getAddableContentProviders() //
            .stream() //
            .map(WorkbenchMountPointDefinition::getTypeIdentifier) //
            // ask legacy provider factory for prio
            .map(ExplorerMountTable::getContentProviderFactory) //
            .sorted(DESC_PRIO) //
            .toList();
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
        return WorkbenchMountTable.getAddableContentProviders() //
            .stream() //
            .map(WorkbenchMountPointDefinition::getTypeIdentifier) //
            .filter(typeId -> !existingProviderIDs.contains(typeId)) //
            // ask legacy provider factory for temp space and prio
            .map(ExplorerMountTable::getContentProviderFactory) //
            .filter(AbstractContentProviderFactory::isTempSpace) //
            .sorted(DESC_PRIO) //
            .toList();
    }

    /**
     * Returns the content provider factory for the provided id.
     *
     * @param factoryID the id of the factory
     *
     * @return The content provider factory for the provided id, null if id does not exist.
     */
    public static AbstractContentProviderFactory getContentProviderFactory(
            final String factoryID) {
        return CONTENT_FACTORIES.get(factoryID);
    }

    /**
     * Retrieves the legacy content provider for the corresponding workbench mount point.
     * @param wmp workbench mount point
     * @return abstract content provider
     */
    private static AbstractContentProvider toAbstractContentProvider(final WorkbenchMountPoint wmp) {
        return getContentProvider(wmp, /* not restored */null);
    }

    private static final Predicate<AbstractContentProvider> IS_NOT_REMOTE = provider -> !provider.isRemote();

    private static final Predicate<AbstractContentProvider> IS_NOT_TEMP_SPACE =
        provider -> !provider.getFactory().isTempSpace();


    /**
     * @return a list of all mount IDs currently in use and not hidden.
     * @since 6.4
     */
    public static List<String> getAllVisibleMountIDs() {
        return WorkbenchMountTable.withMounted(mounted ->
            mounted.stream() //
                .map(ExplorerMountTable::toAbstractContentProvider) //
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
                .filter(IS_NOT_REMOTE.and(IS_NOT_TEMP_SPACE)) //
                .map(AbstractContentProvider::getMountID) //
                .toList());
    }

    /**
     * @return a list of all mount IDs currently in use.
     * @since 6.4
     */
    public static List<String> getAllMountedIDs() {
        return WorkbenchMountTable.getAllMountedIDs();
    }

    /**
     * @return a map with all currently mounted content providers with their mount ID (including the temp space that
     * should never be shown to the user).
     * @since 6.4
     */
    public static Map<String, AbstractContentProvider> getMountedContentInclTempSpace() {
        return WorkbenchMountTable.withMounted(mounted ->
            mounted.stream() //
                    .map(ExplorerMountTable::toAbstractContentProvider) //
                    .collect(Collectors.toMap(AbstractContentProvider::getMountID, Function.identity()))
            );
    }

    /**
     * @return a map with the currently mounted content providers with their mount ID (temp space is not included).
     */
    public static Map<String, AbstractContentProvider> getMountedContent() {
        return WorkbenchMountTable.withMounted(mounted ->
            mounted.stream() //
                    .map(ExplorerMountTable::toAbstractContentProvider) //
                    .filter(IS_NOT_TEMP_SPACE)
                    .collect(Collectors.toMap(AbstractContentProvider::getMountID, Function.identity()))
            );
    }

    /**
     * The MountPoint (containing the content provider) currently mounted with
     * the specified ID - or null if the ID is not used as mount point.
     *
     * @param mountID the mount point
     * @return null, if no content is mounted with the specified ID
     */
    public static MountPoint getMountPoint(final String mountID) {
        final var mp = WorkbenchMountTable.getMountPoint(mountID); // TODO adapter
        return null;
    }

    /**
     * Checks whether an instance created by the specified factory is already
     * mounted.
     *
     *
     * @param providerID the id of the provider factory
     * @return true, if an instance created by the specified factory exists
     *         already, false, if not.
     */
    public static boolean isMounted(final String providerID) {
        return WorkbenchMountTable.withMounted(mounted ->
            mounted.stream().anyMatch(mp -> mp.getDefinition().getTypeIdentifier().equals(providerID))
        );
    }

    /**
     * Checks whether an instance created by the specified factory is already
     * mounted and returns the mount IDs for it.
     *
     *
     * @param providerID the id of the provider factory
     * @return a list with mount IDs (or an empty list if provider is not
     *         mounted).
     */
    public static List<String> getMountIDs(final String providerID) {
        if (providerID == null || providerID.isEmpty()) {
            throw new IllegalArgumentException("Internal error: provider ID can't be null");
        }
        return WorkbenchMountTable.withMounted(mounted ->
            mounted.stream() //
                .filter(mp -> mp.getDefinition().getTypeIdentifier().equals(providerID)) //
                .map(WorkbenchMountPoint::getMountID)
                .toList()
        );
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
            if (e.getValue().getFactory().isTempSpace()) {
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
            LocalWorkspaceContentProviderFactory lwcpf =
                    new LocalWorkspaceContentProviderFactory();
            CONTENT_FACTORIES.put(lwcpf.getID(), lwcpf);
            FACTORY_NAMES.put(lwcpf.toString(), lwcpf.getID());
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
                instance =
                        (AbstractContentProviderFactory)elem
                                .createExecutableExtension(ATTR_CONT_PROV_FACT);
            } catch (Throwable t) {
                LOGGER.error("Problems during initialization of "
                        + "content provider factory (with id '" + contProvFact
                        + "'.)", t);
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.");
                }
            }

            if (instance != null) {
                CONTENT_FACTORIES.put(instance.getID(), instance);
                FACTORY_NAMES.put(instance.toString(), instance.getID());
            }
        }

    }

    /*---------------------------------------------------------------*/

    /**
     * Initializes the explorer mount table based on the preferences of the
     * plugin's preference store.
     *
     * @deprecated This method is deprecated and should not be used anymore. {@link WorkbenchMountTable} is responsible
     *            for initialization and this method has no effect.
     * @see WorkbenchMountTable single-source of truth for mountpoint information
     */
    @Deprecated(since = "5.4", forRemoval = true)
    public static void init() {
        // no-op: WorkbenchMountTable is responsible for initialization
    }

    private static List<MountSettings> getMountSettings() {
        // AP-8989 switching to IEclipsePreferences
        List<MountSettings> mountSettings = new ArrayList<>();

        IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation());
        String[] childrenNames = null;
        try {
            childrenNames = mountPointNode.childrenNames();
        } catch (BackingStoreException e) {
            LOGGER.error("Unabled to read mount point preferences: " + e.getMessage(), e);
        }

        if (!ArrayUtils.isEmpty(childrenNames)) {
            mountSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();
        } else {
            IPreferenceStore pStore = ExplorerActivator.getDefault().getPreferenceStore();
            String mpSettings;
            if (ExplorerPreferenceInitializer.existsMountPreferencesXML()) {
                mpSettings = pStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);
            } else {
                mpSettings = pStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT);
            }
            if (StringUtils.isEmpty(mpSettings)) {
                ExplorerPreferenceInitializer.loadDefaultMountPoints();
                mpSettings = pStore.getDefaultString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);
            }
             mountSettings = MountSettings.parseSettings(mpSettings, true);
             mountSettings.addAll(MountSettings.loadSortedMountSettingsFromDefaultPreferenceNode());
        }

        return mountSettings;
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
                // TODO adapt to legacy mountpoint
                final var mountID = event.getMountPointID();
                final MountPoint mpAdapter = null;
                listener.propertyChange(new PropertyChangeEvent(mpAdapter, MOUNT_POINT_PROPERTY, mountID, null));
            }

            @Override
            public void mountPointAdded(final MountPointEvent event) {
                // TODO adapt to legacy mountpoint
                final var mountID = event.getMountPointID();
                final MountPoint mpAdapter = null;
                listener.propertyChange(new PropertyChangeEvent(mpAdapter, MOUNT_POINT_PROPERTY, null, mountID));
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
        // AP-8989 switching to IEclipsePreferences
        Map<String, AbstractContentProvider> mountedContent = getMountedContent();
        List<MountSettings> mountSettingsToSave = new ArrayList<>();

        for (MountSettings ms : getMountSettings()) {
            if (mountedContent.containsKey(ms.getMountID())) {
                mountSettingsToSave.add(new MountSettings(mountedContent.get(ms.getMountID())));
            } else {
                mountSettingsToSave.add(ms);
            }
        }

        MountSettings.saveMountSettings(mountSettingsToSave);

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
