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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.GUIDeadlockDetector;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.preferences.ExplorerPreferenceInitializer;
import org.knime.workbench.explorer.view.preferences.MountSettings;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Content and Label provider for the explorer view. Delegates the corresponding
 * calls to different content providers providing a view to different sources. <br>
 * The objects returned by the different providers are wrapped into a
 * {@link ContentObject} (associating the creating provider with it) and the
 * wrapper is placed in the tree view. <br>
 * An instance of this should be set as input of the tree view.
 *
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class ContentDelegator extends LabelProvider
    implements ITreeContentProvider, IColorProvider, IPropertyChangeListener, ILabelProviderListener {
    /**
     * The property for changes in the content IPropertyChangeListener can
     * register for.
     */
    public static final String CONTENT_CHANGED = "CONTENT_CHANGED";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ContentDelegator.class);

    private final List<IPropertyChangeListener> m_changeListener;

    private static final Image USER_SPACE_IMG = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/workflow_projects.png").createImage();

    /**
     * constant empty array. Yup.
     */
    private static final ContentObject[] NO_CHILDREN = new ContentObject[0];

    /**
     * All currently visible providers.
     */
    private final HashSet<MountPoint> m_provider;

    /**
     * Creates a new content delegator and registers it for property changes of
     * the explorer mount table. None of the mounted content is visible through
     * this constructor.
     */
    public ContentDelegator() {
        m_provider = new LinkedHashSet<MountPoint>();
        m_changeListener = new CopyOnWriteArrayList<IPropertyChangeListener>();
        ExplorerMountTable.addPropertyChangeListener(this);
    }

    /**
     * Adds the specified content provider to the explorer.
     *
     * @param mountPoint the mount point to add
     */
    public void addMountPoint(final MountPoint mountPoint) {
        if (mountPoint == null) {
            throw new NullPointerException("Mount point can't be null");
        }
        m_provider.add(mountPoint);
        mountPoint.getProvider().addListener(this);
        notifyListeners(new PropertyChangeEvent(mountPoint, CONTENT_CHANGED,
                null, mountPoint.getMountID()));
    }

    /**
     * @return a set with the ids of the currently shown mount points
     */
    public Set<String> getMountedIds() {
        Set<String> mounted = new LinkedHashSet<String>();
        for (MountPoint mountPoint : m_provider) {
            mounted.add(mountPoint.getMountID());
        }
        return mounted;
    }

    /**
     * Clears the view content.
     */
    public void removeAllMountPoints() {
        for (MountPoint mountPoint : m_provider) {
            final AbstractContentProvider provider = mountPoint.getProvider();
            provider.removeListener(this);
            // don't dispose provider (owned by the mount table)
        }
        m_provider.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        ExplorerMountTable.updateProviderSettings();
        removeAllMountPoints();
        ExplorerMountTable.removePropertyChangeListener(this);
        super.dispose();
    }

    /**
     * @return a list of providers currently visible in this view
     */
    public Collection<AbstractContentProvider> getVisibleContentProvider() {
        ArrayList<AbstractContentProvider> result =
                new ArrayList<AbstractContentProvider>();
        for (MountPoint mp : m_provider) {
            result.add(mp.getProvider());
        }
        return result;
    }

    /**
     * Converts an array of objects into an array of wrapped objects.
     *
     * @param provider the creator of the passed objects.
     * @param fileStores the explorer file stores to wrap
     * @return a new array with wrapped objects.
     */
    private ContentObject[] wrapObjects(final AbstractContentProvider provider,
            final AbstractExplorerFileStore[] fileStores) {
        ContentObject[] result = new ContentObject[fileStores.length];
        for (int i = 0; i < fileStores.length; i++) {
            result[i] = new ContentObject(provider, fileStores[i]);
        }
        return result;
    }

    /*
     * ------------ Content Provider Methods --------------------
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getChildren(final Object parentElement) {
        // we are the root element - providers are the first level children
        if (parentElement == this) {
            return getVisibleContentProvider().toArray();
        }
        if (parentElement instanceof AbstractContentProvider) {
            AbstractContentProvider prov =
                    (AbstractContentProvider)parentElement;
            // get the children of the provider's root.
            return wrapObjects(prov, prov.getChildren(prov.getFileStore("/")));

        }
        if (!(parentElement instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + parentElement
                    + " of type " + parentElement.getClass().getCanonicalName());
            return NO_CHILDREN;
        }
        ContentObject c = ((ContentObject)parentElement);
        AbstractContentProvider prov = c.getProvider();
        return wrapObjects(prov, prov.getChildren(c.getObject()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "KNIME Explorer #" + System.identityHashCode(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
        // thanks for letting me know.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParent(final Object element) {
        if (element == this) {
            // we are the root of the tree
            return null;
        }
        if (element instanceof AbstractContentProvider) {
            // content providers are the first level children
            return this;
        }
        if (!(element instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + element
                    + " of type " + element.getClass().getCanonicalName());
            return null;
        }

        ContentObject c = (ContentObject)element;
        // we must ask the corresponding content provider
        AbstractContentProvider provider = c.getProvider();
        AbstractExplorerFileStore parent = provider.getParent(c.getObject());
        if (parent == null || provider.getFileStore("/").equals(parent)) {
            // the root of each subtree is the provider itself
            return provider;
        } else {
            return new ContentObject(provider, parent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        if (element == this) {
            // content providers are the first level children
            return !m_provider.isEmpty();
        }
        if (element instanceof AbstractContentProvider) {
            // content providers are the first level children
            AbstractContentProvider prov = (AbstractContentProvider)element;
            return prov.hasChildren(prov.getFileStore("/"));
        }
        if (!(element instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + element
                    + " of type " + element.getClass().getCanonicalName());
            return false;
        }
        ContentObject c = (ContentObject)element;
        return c.getProvider().hasChildren(c.getObject());
    }

    /**
     * Creates the (new but) same object that is stored in the view tree for the
     * passed file.
     *
     * @param file to return the tree object for
     * @return the (new but) same object that is stored in the view tree for the
     *         passed file.
     */
    public static Object getTreeObjectFor(final AbstractExplorerFileStore file) {
        if (file == null) {
            return null;
        }
        String id = file.getMountID();
        MountPoint mp = ExplorerMountTable.getMountPoint(id);
        if (mp == null) {
            return null;
        }
        if (file.getFullName().equals("/") || file.getParent() == null) {
            return mp.getProvider();
        } else {
            return new ContentObject(mp.getProvider(), file);
        }
    }

    /**
     * Creates the (new but) same object that is stored in the view tree for the
     * mount point with the passed mount id.
     *
     * @param mountID mount id of the mount point
     * @return the (new but) same object that is stored in the view tree for the
     *         passed mount id.
     */
    public static AbstractContentProvider getTreeObjectFor(final String mountID) {
        MountPoint mp = ExplorerMountTable.getMountPoint(mountID);
        return mp.getProvider();
    }

    /**
     * Same as {@link #getTreeObjectFor(AbstractExplorerFileStore)}, just for
     * lists. The result list may be shorter than the argument list as null
     * elements are not added (in case objects in the argument collection are of
     * unexpected type).
     *
     * @param files the files to get tree objects for
     * @return a list of tree objects for the files.
     */
    public static List<Object> getTreeObjectList(
            final Collection<? extends AbstractExplorerFileStore> files) {
        if (files == null) {
            return null;
        }
        ArrayList<Object> result = new ArrayList<Object>();
        for (AbstractExplorerFileStore f : files) {
            Object o = getTreeObjectFor(f);
            if (o != null) {
                result.add(o);
            }
        }
        return result;
    }

    /**
     * Returns the file store corresponding to the passed treeviewer object.
     *
     * @param treeObject either a content object or a content provider
     * @return the file store corresponding to the passed treeviewer object or
     *         null, if the object passed is of unexpected type
     */
    public static AbstractExplorerFileStore getFileStore(final Object treeObject) {
        if (treeObject instanceof ContentObject) {
            return ((ContentObject)treeObject).getObject();
        } else if (treeObject instanceof AbstractContentProvider) {
            // content provider represent the root object of their content
            return ((AbstractContentProvider)treeObject).getFileStore("/");
        } else {
            return null;
        }
    }

    /**
     * Same as {{@link #getFileStore(Object)}. Just for collections. The result
     * list may be shorter than the argument list as null elements are not added
     * (in case objects in the argument collection are of unexpected type).
     *
     * @param treeObjects the tree objects to convert
     * @return a list of file stores
     */
    public static List<AbstractExplorerFileStore> getFileStoreList(
            final Collection<? extends Object> treeObjects) {
        if (treeObjects == null) {
            return null;
        }
        ArrayList<AbstractExplorerFileStore> result =
                new ArrayList<AbstractExplorerFileStore>();
        for (Object o : treeObjects) {
            AbstractExplorerFileStore f = getFileStore(o);
            if (f != null) {
                result.add(f);
            }
        }
        return result;
    }

    /*
     * ------------ Label Provider Methods --------------------
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object obj) {
        if (obj == this) {
            return USER_SPACE_IMG;
        }
        if (obj instanceof AbstractContentProvider) {
            return ((AbstractContentProvider)obj).getImage();
        }
        if (!(obj instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + obj
                    + " of type " + obj.getClass().getCanonicalName());
            return null;
        }
        ContentObject c = (ContentObject)obj;
        return c.getProvider().getImage(c.getObject());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {
        if (element == this) {
            return toString();
        }
        if (element instanceof AbstractContentProvider) {
            AbstractContentProvider acp = (AbstractContentProvider)element;
            return getMountID(acp) + " (" + acp.toString() + ")";
        }
        if (!(element instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + element
                    + " of type " + element.getClass().getCanonicalName());
            return null;
        }
        ContentObject c = (ContentObject)element;
        return c.getProvider().getText(c.getObject());
    }

    private String getMountID(final AbstractContentProvider p) {
        for (MountPoint mp : m_provider) {
            if (mp.getProvider() == p) {
                return mp.getMountID();
            }
        }
        return "<?>";
    }

    /*
     * --------------------------------------------------------
     */

    // colon is very unlikely (up to not allowed) to appear in a mount id
    private static final char ID_SEP = ':';

    private static final String KEY = "DisplayedMountIDs";
    private static final String IGNORE_MEMENTO = "IgnoreMementoFor29Plus";

    private String m_pre29Storage;

    /**
     * @param storage store information in order to restore state
     */
    public void saveState(final IMemento storage) {
        // save state for pre 2.9 workspaces
        if (m_pre29Storage != null) {
            storage.putString(KEY, m_pre29Storage);
            storage.putBoolean(IGNORE_MEMENTO, true);
        }
    }

    /**
     *
     * Restore previously displayed mount points.
     *
     * @param storage to restore display from
     *
     */
    public void restoreState(final IMemento storage) {
        boolean ignoreMemento = false;
        if (storage != null) {
            m_pre29Storage = storage.getString(KEY);
            ignoreMemento = Boolean.TRUE.equals(storage.getBoolean(IGNORE_MEMENTO));
        }
        if (ignoreMemento || ExplorerPreferenceInitializer.existMountPointPreferenceNodes()
                || ExplorerPreferenceInitializer.existsMountPreferencesXML()) {
            restoreStateFromPreferences();
        } else {
            createMountPointXMLPreferences();
            if (storage != null) {
                String displayed = storage.getString(KEY);
                if (displayed != null && !displayed.isEmpty()) {
                    restoreStateFromStorage(displayed);
                    saveStateToPreferences();
                } else {
                    restoreStateFromPreferences();
                }
            } else {
                restoreStateFromPreferences();
            }
        }
    }

    private void createMountPointXMLPreferences() {
        String prefKey = PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML;
        IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        String pre29PrefString = prefStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT);
        if (pre29PrefString != null && !pre29PrefString.isEmpty()) {
            prefStore.setValue(prefKey, pre29PrefString);
        } else {
            prefStore.setValue(prefKey, prefStore.getDefaultString(prefKey));
        }
    }

    private void restoreStateFromStorage(final String storageString) {
        removeAllMountPoints();
        if (storageString != null) {
            String[] ids = storageString.split(String.valueOf(ID_SEP));
            for (String id : ids) {
                tryAddMountPoint(id);
            }
        }
    }

    private void saveStateToPreferences() {
        List<MountSettings> mountSettings = getMountSettingsFromPreferences();
        Set<String> activeIDs = getMountedIds();
        for (MountSettings settings : mountSettings) {
            settings.setActive(activeIDs.contains(settings.getMountID()));
        }
        writeToPreferences(mountSettings);
    }

    private void restoreStateFromPreferences() {
        List<MountSettings> settingsList = getMountSettingsFromPreferences();
        for (MountSettings settings : settingsList) {
            if (settings.isActive() && MountSettings.isMountSettingsAddable(settings)) {
                tryAddMountPoint(settings.getMountID());
            }
        }
    }

    private List<MountSettings> getMountSettingsFromPreferences() {
        List<MountSettings> mountSettings = null;

        try {
            mountSettings = MountSettings.loadSortedMountSettingsFromPreferences();
        } catch (BackingStoreException e) {
            LOGGER.error("Could not load mount point settings:" + e.getMessage(), e);
        }

        return mountSettings;
    }

    private void writeToPreferences(final List<MountSettings> mountSettings) {
        if (!CollectionUtils.isEmpty(mountSettings)) {
            MountSettings.saveMountSettings(mountSettings);
        }
    }

    private void tryAddMountPoint(final String mountID) {
        MountPoint mp = ExplorerMountTable.getMountPoint(mountID);
        if (mp != null) {
            addMountPoint(mp);
        } else {
            LOGGER.info("Can't restore mount point to display: " + mountID);
            return;
        }
    }

    private static boolean hasLoggedBugSRV715;
    private static final String LOG_MOUNT_REMOVE_SRV_715 = System.getProperty("knime.log.srv-715.path");

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (ExplorerMountTable.MOUNT_POINT_PROPERTY.equals(event.getProperty())) {
            MountPoint mp = (MountPoint)event.getSource();
            if (event.getNewValue() == null) {
                // mount point was removed
                mp.getProvider().removeListener(this);
                boolean removed = m_provider.remove(mp);
                if (removed) {
                    notifyListeners(new PropertyChangeEvent(mp,
                            CONTENT_CHANGED, mp.getMountID(), null));
                    if (StringUtils.isNotBlank(LOG_MOUNT_REMOVE_SRV_715) && !hasLoggedBugSRV715) {
                        hasLoggedBugSRV715 = true;
                        LOGGER.infoWithFormat("Logging mount point preference change to '%s'", LOG_MOUNT_REMOVE_SRV_715);
                        try (BufferedWriter s = Files.newBufferedWriter(Paths.get(LOG_MOUNT_REMOVE_SRV_715), StandardOpenOption.CREATE)) {
                            s.write(GUIDeadlockDetector.createStacktrace());
                        } catch (IOException e) {
                            LOGGER.error("Couldn't log details for SRV-715", e);
                        }
                    }
                    LOGGER.debug("Removed mount point with id \""
                            + mp.getMountID()
                            + "\" from view because it was deleted in the "
                            + "preferences.");
                }
            } else {
                tryAddMountPoint(((MountPoint)event.getSource()).getMountID());
                LOGGER.debug("Added mount point with id \"" + mp.getMountID() + ".");
            }
            return;
        }
    }

    /**
     * {@inheritDoc}
     * <p />
     * Mounted content providers notify listeners of new content with this. They
     * should set themselves as source and a filestore as element (if that is
     * null a global refresh is triggered)
     */
    @Override
    public void labelProviderChanged(final LabelProviderChangedEvent event) {
        if (event != null
                && (event.getSource() instanceof AbstractContentProvider)) {
            AbstractContentProvider source =
                    (AbstractContentProvider)event.getSource();
            Object refresh = event.getElement();
            if (refresh instanceof AbstractExplorerFileStore) {
                notifyListeners(new PropertyChangeEvent(source,
                        CONTENT_CHANGED, null, refresh));
            } else {
                notifyListeners(new PropertyChangeEvent(source,
                        CONTENT_CHANGED, null, null));
            }
        }

    }

    /*---------------------------------------------------------------*/
    /**
     * Adds a property change listener for mount changes.
     *
     * @param listener the property change listener to add
     */
    public void addPropertyChangeListener(final IPropertyChangeListener listener) {
        m_changeListener.add(listener);
    }

    /**
     * Removes the given listener. Calling this method has no affect if the
     * listener is not registered.
     *
     * @param listener a property change listener
     */
    public void removePropertyChangeListener(
            final IPropertyChangeListener listener) {
        m_changeListener.remove(listener);
    }

    private void notifyListeners(final PropertyChangeEvent event) {
        for (IPropertyChangeListener listener : m_changeListener) {
            listener.propertyChange(event);
        }
    }

    /**
     * {@inheritDoc}
     * @since 7.2
     */
    @Override
    public Color getForeground(final Object element) {
        if (element instanceof AbstractContentProvider) {
            return ((AbstractContentProvider)element).getForeground(element);
        } else if (element instanceof ContentObject) {
            ContentObject co = (ContentObject) element;
            return co.getProvider().getForeground(co.getObject());
        } else {
            return Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        }
    }

    /**
     * {@inheritDoc}
     * @since 7.2
     */
    @Override
    public Color getBackground(final Object element) {
        if (element instanceof AbstractContentProvider) {
            return ((AbstractContentProvider)element).getBackground(element);
        } else if (element instanceof ContentObject) {
            ContentObject co = (ContentObject) element;
            return co.getProvider().getBackground(co.getObject());
        } else {
            return Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        }
    }
}
