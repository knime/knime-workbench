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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.ThreadUtils;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPoint;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointSettings;
import org.knime.core.workbench.preferences.MountPointsPreferencesUtil;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.ui.KNIMEUIPlugin;

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
    static final String CONTENT_CHANGED = "CONTENT_CHANGED";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ContentDelegator.class);

    private final List<IPropertyChangeListener> m_changeListener;

    private static final Image USER_SPACE_IMG = AbstractUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
        "icons/workflow_projects.png").createImage();

    /**
     * constant empty array. Yup.
     */
    private static final ContentObject[] NO_CHILDREN = new ContentObject[0];

    /**
     * All currently visible providers.
     */
    private final HashMap<String, AbstractContentProvider> m_providerMap;

    private final boolean m_updateProvSettings;

    /**
     * Creates a new content delegator and registers it for property changes of
     * the explorer mount table. None of the mounted content is visible through
     * this constructor.
     */
    public ContentDelegator() {
        this(true);
    }

    /**
     * Creates a new content delegator and registers it for property changes of the explorer mount table. None of the
     * mounted content is visible through this constructor.
     *
     * @param updateProvSettings {@code true} if the settings of all content providers in the preferences should be
     *            updated, {@code false} otherwise. Default is {@code true}.
     * @since 8.5
     */
    public ContentDelegator(final boolean updateProvSettings) {
        m_providerMap = new LinkedHashMap<>();
        m_changeListener = new CopyOnWriteArrayList<>();
        m_updateProvSettings = updateProvSettings;
        ExplorerMountTable.addPropertyChangeListener(this);
    }

    /**
     * Adds the specified content provider to the explorer.
     *
     * @param provider the mount point to add
     * @since 9.0
     */
    public void addMountPoint(final AbstractContentProvider provider) {
        CheckUtils.checkArgumentNotNull(provider, "Mount point can't be null");
        m_providerMap.put(provider.getMountID(), provider);
        provider.addListener(this);
        notifyListeners(new PropertyChangeEvent(provider, CONTENT_CHANGED, null, provider.getMountID()));
    }

    /**
     * @return a set with the ids of the currently shown mount points
     */
    public Set<String> getMountedIds() {
        return new LinkedHashSet<>(m_providerMap.keySet());
    }

    /**
     * Clears the view content.
     */
    private void removeAllMountPoints() {
        m_providerMap.values().forEach(provider -> provider.removeListener(ContentDelegator.this));
        m_providerMap.clear();
    }

    @Override
    public void dispose() {
        if (m_updateProvSettings) {
            ExplorerMountTable.updateProviderSettings();
        }
        removeAllMountPoints();
        ExplorerMountTable.removePropertyChangeListener(this);
        super.dispose();
    }

    /**
     * @return a list of providers currently visible in this view
     */
    public Collection<AbstractContentProvider> getVisibleContentProvider() {
        return new ArrayList<>(m_providerMap.values());
    }

    /**
     * Converts an array of objects into an array of wrapped objects.
     *
     * @param provider the creator of the passed objects.
     * @param fileStores the explorer file stores to wrap
     * @return a new array with wrapped objects.
     */
    private static ContentObject[] wrapObjects(final AbstractContentProvider provider,
            final AbstractExplorerFileStore[] fileStores) {
        return Arrays.stream(fileStores) //
                .map(store -> new ContentObject(provider, store)) //
                .toArray(ContentObject[]::new);
    }

    /*
     * ------------ Content Provider Methods --------------------
     */

    @Override
    public Object[] getChildren(final Object parentElement) {
        // we are the root element - providers are the first level children
        if (parentElement == this) {
            return getVisibleContentProvider().toArray();
        }
        if (parentElement instanceof AbstractContentProvider prov) {
            // get the children of the provider's root.
            return wrapObjects(prov, prov.getChildren(prov.getRootStore()));

        }
        if (parentElement instanceof ContentObject c) {
            AbstractContentProvider prov = c.getProvider();
            return wrapObjects(prov, prov.getChildren(c.getObject()));
        }
        // all children should be of that type!
        logUnexpectedObject(parentElement);
        return NO_CHILDREN;
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public String toString() {
        return "KNIME Explorer #" + System.identityHashCode(this);
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        // thanks for letting me know.
    }

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
        if (element instanceof ContentObject c) {
            // we must ask the corresponding content provider
            AbstractContentProvider provider = c.getProvider();
            AbstractExplorerFileStore parent = provider.getParent(c.getObject());
            if (parent == null || provider.getRootStore().equals(parent)) {
                // the root of each subtree is the provider itself
                return provider;
            } else {
                return new ContentObject(provider, parent);
            }
        }
        // all children should be of that type!
        logUnexpectedObject(element);
        return null;
    }

    @Override
    public boolean hasChildren(final Object element) {
        if (element == this) {
            // content providers are the first level children
            return !m_providerMap.isEmpty();
        }
        if (element instanceof AbstractContentProvider prov) {
            // content providers are the first level children
            return prov.hasChildren(prov.getRootStore());
        }
        if (element instanceof ContentObject c) {
            return c.getProvider().hasChildren(c.getObject());
        }
        // all children should be of that type!
        logUnexpectedObject(element);
        return false;
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
        Optional<AbstractContentProvider> providerOptional = ExplorerMountTable.getContentProvider(id);
        if (providerOptional.isEmpty()) {
            return null;
        }
        final AbstractContentProvider provider = providerOptional.get();
        if (file.getFullName().equals("/") || file.getParent() == null) {
            return provider;
        } else {
            return new ContentObject(provider, file);
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
        return ExplorerMountTable.getContentProvider(mountID)
            .orElseThrow(() -> new IllegalArgumentException("Mount point with id " + mountID + " not found"));
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
    public static List<Object> getTreeObjectList(final Collection<? extends AbstractExplorerFileStore> files) {
        if (files == null) {
            return null;
        }
        ArrayList<Object> result = new ArrayList<>();
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
        if (treeObject instanceof ContentObject content) {
            return content.getObject();
        } else if (treeObject instanceof AbstractContentProvider provider) {
            // content provider represent the root object of their content
            return provider.getRootStore();
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
    public static List<AbstractExplorerFileStore> getFileStoreList(final Collection<?> treeObjects) {
        if (treeObjects == null) {
            return null;
        }
        ArrayList<AbstractExplorerFileStore> result = new ArrayList<>();
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

    @Override
    public Image getImage(final Object obj) {
        if (obj == this) {
            return USER_SPACE_IMG;
        }
        if (obj instanceof AbstractContentProvider provider) {
            return provider.getImage();
        }
        if (obj instanceof ContentObject content) {
            return content.getProvider().getImage(content.getObject());
        }
        // all children should be of that type!
        logUnexpectedObject(obj);
        return null;
    }

    @Override
    public String getText(final Object obj) {
        if (obj == this) {
            return toString();
        }
        if (obj instanceof AbstractContentProvider acp) {
            return acp.getMountID() + " (" + acp.toString() + ")";
        }
        if (obj instanceof ContentObject content) {
            return content.getProvider().getText(content.getObject());
        }
        // all children should be of that type!
        logUnexpectedObject(obj);
        return null;
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
        if (ignoreMemento || MountPointsPreferencesUtil.existMountPointPreferenceNodes()) {
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
    /**
     * Preference constant for mount points for the Explorer.
     */
    private static final String P_EXPLORER_MOUNT_POINT = "knime.explorer.mountpoint"; // NOSONAR (deprecation)

    private static void createMountPointXMLPreferences() {
        String prefKey = WorkbenchConstants.P_EXPLORER_MOUNT_POINT_XML;
        IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        String pre29PrefString = prefStore.getString(P_EXPLORER_MOUNT_POINT);
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
        final Set<String> activeIDs = getMountedIds();
        final List<WorkbenchMountPointSettings> mountSettings = getMountSettingsFromPreferences().stream() //
                .map(mps -> mps.withActive(activeIDs.contains(mps.mountID())))
                .toList();
        writeToPreferences(mountSettings);
    }

    private void restoreStateFromPreferences() {
        List<WorkbenchMountPointSettings> settingsList = getMountSettingsFromPreferences();
        for (WorkbenchMountPointSettings settings : settingsList) {
            if (settings.isActive() && ExplorerMountTable.getContentProviderFactory(settings.factoryID()) != null) {
                tryAddMountPoint(settings.mountID());
            }
        }
    }

    private static List<WorkbenchMountPointSettings> getMountSettingsFromPreferences() {
        return MountPointsPreferencesUtil.loadSortedMountSettingsFromPreferences(true);
    }

    private static void writeToPreferences(final List<WorkbenchMountPointSettings> mountSettings) {
        if (!CollectionUtils.isEmpty(mountSettings)) {
            MountPointsPreferencesUtil.saveMountSettings(mountSettings);
        }
    }

    private boolean tryAddMountPoint(final String mountID) {
        final Optional<AbstractContentProvider> contentProviderOptional = ExplorerMountTable.getContentProvider(mountID);
        if (contentProviderOptional.isEmpty()) {
            LOGGER.info("Can't restore mount point to display: " + mountID);
            return false;
        }
        addMountPoint(contentProviderOptional.get());
        return true;
    }

    private static boolean hasLoggedBugSRV715;
    private static final String LOG_MOUNT_REMOVE_SRV_715 = System.getProperty("knime.log.srv-715.path");

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (ExplorerMountTable.MOUNT_POINT_PROPERTY.equals(event.getProperty())) {
            WorkbenchMountPoint mp = (WorkbenchMountPoint)event.getSource();
            if (event.getNewValue() == null) {
                // mount point was removed
                ExplorerMountTable.toAbstractContentProviderIfKnown(mp).ifPresent(c -> c.removeListener(this));
                boolean removed = m_providerMap.remove(mp.getMountID()) != null;
                if (removed) {
                    notifyListeners(new PropertyChangeEvent(mp,
                            CONTENT_CHANGED, mp.getMountID(), null));
                    if (StringUtils.isNotBlank(LOG_MOUNT_REMOVE_SRV_715) && !hasLoggedBugSRV715) {
                        hasLoggedBugSRV715 = true;
                        LOGGER.infoWithFormat("Logging mount point preference change to '%s'", LOG_MOUNT_REMOVE_SRV_715);
                        try (BufferedWriter s = Files.newBufferedWriter(Paths.get(LOG_MOUNT_REMOVE_SRV_715), StandardOpenOption.CREATE)) {
                            s.write(ThreadUtils.getJVMStacktraces());
                        } catch (IOException e) {
                            LOGGER.error("Couldn't log details for SRV-715", e);
                        }
                    }
                    LOGGER.debug("Removed mount point with id \"" + mp.getMountID()
                        + "\" from view because it was deleted in the " + "preferences.");
                }
            } else {
                if (tryAddMountPoint(((WorkbenchMountPoint)event.getSource()).getMountID())) {
                    LOGGER.debug("Added mount point with id \"" + mp.getMountID() + ".");
                }
            }
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
        if (event != null && event.getSource() instanceof AbstractContentProvider source) {
            final var newValue = event.getElement() instanceof AbstractExplorerFileStore store ? store : null;
            notifyListeners(new PropertyChangeEvent(source, CONTENT_CHANGED, null, newValue));
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
        if (element instanceof AbstractContentProvider provider) {
            return provider.getForeground(element);
        } else if (element instanceof ContentObject content) {
            return content.getProvider().getForeground(content.getObject());
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
        if (element instanceof AbstractContentProvider provider) {
            return provider.getBackground(element);
        } else if (element instanceof ContentObject content) {
            return content.getProvider().getBackground(content.getObject());
        } else {
            return Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        }
    }

    private static void logUnexpectedObject(final Object obj) {
        LOGGER.coding(() -> "Unexpected object in tree view! (%s of type %s)" //
            .formatted(obj, obj.getClass().getCanonicalName()));
    }
}
