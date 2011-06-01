/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.preferences.MountSettings;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author ohl, University of Konstanz
 */
public final class ExplorerMountTable {
    /** The property for changes on mount points IPropertyChangeListener
     * can register for. */
    public static final String MOUNT_POINT_PROPERTY = "MOUNT_POINTS";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerMountTable.class);

    private static final List<IPropertyChangeListener> CHANGE_LISTENER
            = new CopyOnWriteArrayList<IPropertyChangeListener>();

    private ExplorerMountTable() {
        // hiding constructor of utility class
    }


    /**
     * Keeps all currently mounted content with the mountID (provided by the
     * user).
     */
    private static final HashMap<String, MountPoint> MOUNTED =
            new LinkedHashMap<String, MountPoint>();

    /**
     * Keeps all content that is prepared for mounting with the mountID
     * (provided by the user).
     */
    private static final HashMap<String, MountPoint> PREPARED =
            new LinkedHashMap<String, MountPoint>();

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
        return mountOrRestore(mountID, providerID, (String)null, false);
    }

    /**
     * Sorts the mount points in the same way as in the passed list. It makes 
     * sure that if two entries A and B of the list are mounted and A comes 
     * before B, the mount point A will appear before mount point B in the 
     * mount table.
     * @param mountIDs a list of mount ids
     */
    public static void setMountOrder(final List<String> mountIDs) {
        synchronized (MOUNTED) {
            for (String mountID : mountIDs) {
                MountPoint mountPoint = MOUNTED.get(mountID);
                if (mountPoint != null) {
                    /* Remove the mount point and insert it again immediately to
                     * get the same order as in the mount id list. */
                    MOUNTED.remove(mountID);
                    MOUNTED.put(mountID, mountPoint);
                }
            }
        }
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
    public static AbstractContentProvider mount(final String mountID,
            final String providerID, final String storage) throws IOException {
        return mountOrRestore(mountID, providerID, storage, false);
    }

    /**
     * Creates a new instance of the specified content provider and prepares the
     * mount point for commitment. May open a user
     * dialog to get parameters needed by the provider factory. Returns null, if
     * the user canceled.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException if the preparation of the mount fails
     */
    public static AbstractContentProvider prepareMount(final String mountID,
            final String providerID) throws IOException {
        return mountOrRestore(mountID, providerID, (String)null, true);
    }

    /**
     * Activates the mount point.
     * @param mountID name under which the content is mounted
     * @return a new content provider instance
     */
    public static AbstractContentProvider commitMount(final String mountID) {
        MountPoint mountPoint = PREPARED.remove(mountID);
        if (mountPoint == null) {
            throw new IllegalArgumentException("MountID \"" + mountID
                    + "\" has not been prepared for mounting.");
        }
        MOUNTED.put(mountID, mountPoint);
        return mountPoint.getProvider();
    }

    /**
     * Clears all prepared mount points.
     */
    public static void clearPreparedMounts() {
        PREPARED.clear();
    }

    /**
     * Mounts a new content with the specified mountID and from the specified
     * provider factory - and initializes it from the specified storage, if that
     * is not null.
     *
     * @param mountID
     * @param providerID
     * @param storage
     * @param prepare
     * @return
     */
    private static AbstractContentProvider mountOrRestore(final String mountID,
            final String providerID, final String storage,
            final boolean prepare) throws IOException {
        synchronized (MOUNTED) {
            // can't mount different providers with the same ID
            MountPoint existMp = MOUNTED.get(mountID);
            if (existMp == null) {
               existMp = PREPARED.get(mountID);
            }
            if (existMp != null) {
                if (existMp.getProviderFactory().getID().equals(providerID)) {
                    // re-use the provider
                    LOGGER.debug("The content provider with the "
                            + "specified id (" + providerID
                            + ") is already mounted with requested ID ("
                            + mountID + ").");
                    existMp.incrRefCount();
                    return existMp.getProvider();
                }
                throw new IOException(
                        "There is a different content mounted with"
                                + " the same mountID. Unmount that first ("
                                + existMp.getProvider().toString() + ").");
            }

            AbstractContentProviderFactory fac =
                    CONTENT_FACTORIES.get(providerID);
            if (fac == null) {
                LOGGER.coding("Internal error: The content provider with the "
                        + "specified id (" + providerID
                        + ") is not available.");
                throw new IOException(
                        "Internal error: The content provider with the "
                                + "specified id (" + providerID
                                + ") is not available.");
            }
            if (!fac.multipleInstances() && isMounted(providerID)) {
                throw new IllegalStateException("Cannot mount "
                        + fac.toString() + " multiple times.");
            }
            AbstractContentProvider newProvider = null;
            if (storage == null) {
                // may open a dialog for the user to provide parameters
                newProvider = fac.getContentProvider(mountID);
                if (newProvider == null) {
                    // user probably canceled.
                    return null;
                }
            } else {
                newProvider = fac.getContentProvider(mountID, storage);
                if (newProvider == null) {
                    // something went wrong
                    return null;
                }
            }

            MountPoint mp = new MountPoint(mountID, newProvider, fac);
            synchronized (MOUNTED) {
                if (prepare) {
                    PREPARED.put(mountID, mp);
                } else {
                    MOUNTED.put(mountID, mp);
                    notifyListeners(
                            new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY,
                            null, mp.getMountID()));
                }
            }
            return newProvider;
        }
    }

    /**
     * @param mountID the id to unmount
     * @return true if unmounting was successful, false otherwise
     */
    public static synchronized boolean unmount(final String mountID) {
        synchronized (MOUNTED) {
            MountPoint mp = MOUNTED.remove(mountID);
            if (mp == null) {
                return false;
            }
            mp.dispose();
            notifyListeners(new PropertyChangeEvent(mp, MOUNT_POINT_PROPERTY,
                    mp.getMountID(), null));
            return true;
        }
    }

    /**
     * Unmounts all MountPoints.
     */
    public static void unmountAll() {
        synchronized (MOUNTED) {
            List<String> ids = getAllMountIDs();
            for (String id : ids) {
                unmount(id);
            }
        }
    }

    /**
     * Returns a list of content providers that could be added (that is that
     * allow multiple instances or are not yet mounted). The list contains the
     * factory objects. Their toString method should return a useful name.
     *
     * @return a map of available content providers (key = name, value = ID).
     */
    public static List<AbstractContentProviderFactory>
            getAddableContentProviders() {
        LinkedList<AbstractContentProviderFactory> result =
                new LinkedList<AbstractContentProviderFactory>();
        // nobody should add a new provider while we are working
        synchronized (MOUNTED) {
            for (Map.Entry<String, AbstractContentProviderFactory> e
                    : CONTENT_FACTORIES.entrySet()) {
                String facID = e.getKey();
                AbstractContentProviderFactory fac = e.getValue();
                if (fac.multipleInstances()
                        || !(isMounted(facID) || isPrepared(facID))) {
                    result.add(fac);
                }
            }
        }
        return result;
    }

    /**
     * Returns the content provider factory for the provided id.
     * @param factoryID the id of the factory
     *
     * @return a map of available content providers (key = name, value = ID).
     */
    public static AbstractContentProviderFactory getContentProviderFactory(
            final String factoryID) {
        return CONTENT_FACTORIES.get(factoryID);
    }

    /**
     * @return a list of all mount IDs currently in use.
     */
    public static List<String> getAllMountIDs() {
        synchronized (MOUNTED) {
            return new ArrayList<String>(MOUNTED.keySet());
        }
    }

    /**
     * @return a map with all currently mounted content providers with their
     *         mount ID.
     */
    public static Map<String, AbstractContentProvider> getMountedContent() {
        HashMap<String, AbstractContentProvider> result =
                new LinkedHashMap<String, AbstractContentProvider>();
        synchronized (MOUNTED) {
            for (Map.Entry<String, MountPoint> e : MOUNTED.entrySet()) {
                result.put(e.getKey(), e.getValue().getProvider());
            }
        }
        return result;
    }

    /**
     * The MountPoint (containing the content provider) currently mounted with
     * the specified ID - or null if the ID is not used as mount point.
     *
     * @param mountID the mount point
     * @return null, if no content is mounted with the specified ID
     */
    public static MountPoint getMountPoint(final String mountID) {
        synchronized (MOUNTED) {
            return MOUNTED.get(mountID);
        }
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
        return !getMountIDs(providerID).isEmpty();
    }

    /**
     * Checks whether an instance created by the specified factory is already
     * prepared for mounting.
     *
     *
     * @param providerID the id of the provider factory
     * @return true, if an instance created by the specified factory exists
     *         already, false, if not.
     */
    public static boolean isPrepared(final String providerID) {
        return !getPreparedMountIDs(providerID).isEmpty();
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
            throw new IllegalArgumentException(
                    "Internal error: provider ID can't be null");
        }
        LinkedList<String> mountIDs = new LinkedList<String>();

        synchronized (MOUNTED) {
            for (Map.Entry<String, MountPoint> e : MOUNTED.entrySet()) {
                MountPoint mp = e.getValue();
                if (providerID.equals(mp.getProviderFactory().getID())) {
                    mountIDs.add(e.getKey());
                }
            }
        }
        return mountIDs;
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
    private static List<String> getPreparedMountIDs(final String providerID) {
        if (providerID == null || providerID.isEmpty()) {
            throw new IllegalArgumentException(
                    "Internal error: provider ID can't be null");
        }
        LinkedList<String> mountIDs = new LinkedList<String>();

        synchronized (PREPARED) {
            for (Map.Entry<String, MountPoint> e : PREPARED.entrySet()) {
                MountPoint mp = e.getValue();
                if (providerID.equals(mp.getProviderFactory().getID())) {
                    mountIDs.add(e.getKey());
                }
            }
        }
        return mountIDs;
    }

    /**
     *
     * @return the file system representing the content of the mount table.
     */
    public static ExplorerFileSystem getFileSystem() {
        return new ExplorerFileSystem();
    }

    /* ------------- read the extension point ------------------------------ */

    /**
     * Stores all content provider factories (registered with the extension
     * point) mapped to their ID.
     */
    private static final TreeMap<String, AbstractContentProviderFactory>
            CONTENT_FACTORIES = new TreeMap<String,
            AbstractContentProviderFactory>();

    private static final TreeMap<String, String> FACTORY_NAMES =
            new TreeMap<String, String>();

    static {
        // read out the extenstion point now
        collectContentProviderFactories();
        init();
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
     * core plugin's preference store.
     */
    public static void init() {
        unmountAll();
        synchronized (MOUNTED) {
            for (MountSettings ms : getMountSettings()) {
                String mountID = ms.getMountID();
                String storage = ms.getContent();
                if (storage == null) {
                    LOGGER.error("Corrupted mount table state storage. "
                            + "Can't restore mount point '" + mountID + "'.");
                    continue;
                }
                String factID = ms.getFactoryID();
                if (factID == null) {
                    LOGGER.error("Corrupted mount table state storage. "
                            + "Can't restore mount point '" + mountID + "'.");
                    continue;
                }

                try {
                    if (mountOrRestore(mountID, factID, storage, false)
                            == null) {
                        LOGGER.error("Unable to restore mount point '"
                                + mountID + "' (from " + factID
                                + ": returned null).");
                    }
                } catch (Throwable t) {
                    String msg = t.getMessage();
                    if (msg == null || msg.isEmpty()) {
                        msg = "<no details>";
                    }
                    LOGGER.error("Unable to restore mount point '" + mountID
                            + "' (from " + factID + "): " + msg, t);
                }
            }
        }

    }

    private static List<MountSettings> getMountSettings() {
        String mpSettings = KNIMECorePlugin.getDefault()
                .getPreferenceStore().getString(
                                PreferenceConstants.P_EXPLORER_MOUNT_POINT);
       return MountSettings.parseSettings(mpSettings);
    }

    /*---------------------------------------------------------------*/
    /**
     * Adds a property change listener for mount changes.
     * @param listener the property change listener to add
     */
    public static void addPropertyChangeListener(
            final IPropertyChangeListener listener) {
        CHANGE_LISTENER.add(listener);
    }


    /**
     * Removes the given listener. Calling this method has no affect if the
     * listener is not registered.
     *
     * @param listener a property change listener
     */
    public static void removePropertyChangeListener(
            final IPropertyChangeListener listener) {
        CHANGE_LISTENER.remove(listener);
    }

    private static void notifyListeners(final PropertyChangeEvent event) {
        for (IPropertyChangeListener listener : CHANGE_LISTENER) {
            listener.propertyChange(event);
        }
    }

}
