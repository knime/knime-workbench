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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerMountTable {

    private static NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerMountTable.class);

    /**
     * Keeps all currently mounted content with the mountID (provided by the
     * user).
     */
    private static final HashMap<String, MountPoint> m_mounted =
            new HashMap<String, MountPoint>();

    /**
     * Creates a new instance of the specified content provider. May open a user
     * dialog to get parameters needed by the provider factory. Returns null, if
     * the user canceled.
     *
     * @param mountID name under which the content is mounted
     * @param providerID the provider factory id
     * @return a new content provider instance - or null if user canceled.
     * @throws IOException
     */
    public static AbstractContentProvider mount(final String mountID,
            final String providerID) throws IOException {
        return mountOrRestore(mountID, providerID, null);
    }

    /**
     * Mounts a new content with the specified mountID and from the specified
     * provider factory - and initializes it from the specified storage, if that
     * is not null.
     *
     * @param mountID
     * @param providerID
     * @param storage
     * @return
     */
    private static AbstractContentProvider mountOrRestore(final String mountID,
            final String providerID, final IMemento storage) throws IOException {
        synchronized (m_mounted) {
            // can't mount different providers with the same ID
            MountPoint existMp = m_mounted.get(mountID);
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
                        + "specified id (" + providerID + ") is not available.");
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
            synchronized (m_mounted) {
                m_mounted.put(mountID, mp);
            }
            return newProvider;
        }
    }

    /**
     * @param mountID
     * @return
     */
    public synchronized static boolean unmount(final String mountID) {
        synchronized (m_mounted) {
            MountPoint mp = m_mounted.remove(mountID);
            if (mp == null) {
                return false;
            }
            mp.dispose();
            return true;
        }
    }

    public static void unmountAll() {
        synchronized (m_mounted) {
            List<String> IDs = getAllMountIDs();
            for (String id : IDs) {
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
    public static List<AbstractContentProviderFactory> getAddableContentProviders() {
        LinkedList<AbstractContentProviderFactory> result =
                new LinkedList<AbstractContentProviderFactory>();
        // nobody should add a new provider while we are working
        synchronized (m_mounted) {
            for (Map.Entry<String, AbstractContentProviderFactory> e : CONTENT_FACTORIES
                    .entrySet()) {
                String facID = e.getKey();
                AbstractContentProviderFactory fac = e.getValue();
                if (fac.multipleInstances() || !isMounted(facID)) {
                    result.add(fac);
                }
            }
        }
        return result;
    }

    /**
     * @return a list of all mount IDs currently in use.
     */
    public static List<String> getAllMountIDs() {
        synchronized (m_mounted) {
            return new ArrayList<String>(m_mounted.keySet());
        }
    }

    /**
     * @return a map with all currently mounted content providers with their
     *         mount ID.
     */
    public static Map<String, AbstractContentProvider> getMountedContent() {
        HashMap<String, AbstractContentProvider> result =
                new HashMap<String, AbstractContentProvider>();
        synchronized (m_mounted) {
            for (Map.Entry<String, MountPoint> e : m_mounted.entrySet()) {
                result.put(e.getKey(), e.getValue().getProvider());
            }
        }
        return result;
    }

    /**
     * The MountPoint (containing the content provider) currently mounted with
     * the specified ID - or null if the ID is not used as mount point.
     *
     * @param ID the mount point
     * @return null, if no content is mounted with the specified ID
     */
    public static MountPoint getMountPoint(final String ID) {
        synchronized (m_mounted) {
            return m_mounted.get(ID);
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

        synchronized (m_mounted) {
            for (Map.Entry<String, MountPoint> e : m_mounted.entrySet()) {
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
    private static final TreeMap<String, AbstractContentProviderFactory> CONTENT_FACTORIES =
            new TreeMap<String, AbstractContentProviderFactory>();

    private static final TreeMap<String, String> FACTORY_NAMES =
            new TreeMap<String, String>();

    static {
        // read out the extenstion point now
        collectContentProviderFactories();
        // always mount the local workspace
        try {
            mount("LOCAL", LocalWorkspaceContentProviderFactory.ID);
        } catch (IOException e) {
            LOGGER.error("Unable to mount local workspace. "
                    + "Won't be available in explorer view");
        }
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

    private static final char SEP = ':';

    private static final String KEY = "IDs";

    public static void saveState(final IMemento memento) {
        synchronized (m_mounted) {
            StringBuilder sb = new StringBuilder();
            for (MountPoint mp : m_mounted.values()) {
                if (sb.length() > 0) {
                    sb.append(SEP);
                }
                sb.append(mp.getMountID());
                IMemento sub = memento.createChild(mp.getMountID());
                sub.putString("Factory", mp.getProviderFactory().getID());
                mp.getProvider().saveState(sub.createChild("Provider"));
            }
            memento.putString(KEY, sb.toString());
        }
    }

    public static void restore(final IMemento memento) {

        unmountAll();

        synchronized (m_mounted) {
            String[] mountIDs =
                    memento.getString(KEY).split(String.valueOf(SEP));
            for (String mountID : mountIDs) {
                IMemento storage = memento.getChild(mountID);
                if (storage == null) {
                    LOGGER.error("Corrupted mount table state storage. "
                            + "Can't restore mount point '" + mountID + "'.");
                    continue;
                }
                String factID = storage.getString("Factory");
                IMemento sub = storage.getChild("Provider");
                if (factID == null || sub == null) {
                    LOGGER.error("Corrupted mount table state storage. "
                            + "Can't restore mount point '" + mountID + "'.");
                    continue;
                }

                try {
                    if (mountOrRestore(mountID, factID, sub) == null) {
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

}
