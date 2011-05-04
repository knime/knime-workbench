/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
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
 */
package org.knime.workbench.explorer.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.ui.KNIMEUIPlugin;


/**
 * Content and Label provider for the explorer view. Delegates the corresponding
 * calls to different content providers providing a view to different sources. <br />
 * The objects returned by the different providers are wrapped into a
 * {@link ContentObject} (associating the creating provider with it) and the
 * wrapper is placed in the tree view. <br />
 * An instance of this should be set as input of the tree view.
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class ContentDelegator extends LabelProvider implements
        IStructuredContentProvider, ITreeContentProvider {

    private static NodeLogger LOGGER = NodeLogger
            .getLogger(ContentDelegator.class);

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
     *
     */
    public ContentDelegator() {
        m_provider = new HashSet<MountPoint>();
    }

    /**
     * Adds the specified content provider to the explorer.
     *
     * @param mountPoint the mount id and the contentprovider
     * @throws IOException
     */
    public void addMountPoint(final MountPoint mountPoint) {
        if (mountPoint == null) {
            throw new NullPointerException("Mount point can't be null");
        }
        // TODO: register preference listener
        m_provider.add(mountPoint);
    }

    /**
     * Clears the view content.
     */
    public void removeAllMountPoints() {
        // TODO: unregister listeners
        m_provider.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        m_provider.clear();
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
            final ExplorerFileStore[] fileStores) {
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
                    + " of type "
                    + parentElement.getClass().getCanonicalName());
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
        ExplorerFileStore parent = provider.getParent(c.getObject());
        if (provider.getFileStore("/").equals(parent)) {
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
     * {@inheritDoc}
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        // we are the root element - providers are the first level children
        if (inputElement == this) {
            return getVisibleContentProvider().toArray();
        }
        if (inputElement instanceof AbstractContentProvider) {
            AbstractContentProvider prov =
                    (AbstractContentProvider)inputElement;
            // get the children of the provider's root.
            return prov.getElements(prov.getFileStore("/"));
        }
        if (!(inputElement instanceof ContentObject)) {
            // all children should be of that type!
            LOGGER.coding("Unexpected object in tree view! (" + inputElement
                    + " of type " + inputElement.getClass().getCanonicalName());
            return NO_CHILDREN;
        }
        ContentObject c = (ContentObject)inputElement;
        return c.getProvider().getElements(c.getObject());
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

    public void saveState(final IMemento storage) {
        // for all conentprovider instances:
        // child = memento.getChild(contentprovider.getFactory().getID())
        // contentprovider.saveState(child)
        // TODO: Traverse the extensions and let them save their stuff
        // Also store preserved sub-mementos from currently not present plugins
    }

    public void restoreState(final IMemento storage) {
        // for all registered contentprovider factories:
        // instances = memento.getChildren(factory.getID())
        // for all instances:
        // factory.getInstance(inst (=memento))

        // !!! Preserve sub-mementos of provider from plugins that are not
        // present (and save them in the saveState method). In case the
        // workspace gets opened with another installation that contains
        // those plugins.
    }
}
