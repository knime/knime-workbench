/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
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
 * History:
 *    March 2011: created
 */
package org.knime.workbench.explorer;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.workbench.explorer.pathresolve.URIToFileResolveImpl;
import org.knime.workbench.explorer.view.preferences.ExplorerPrefsSyncer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerActivator extends AbstractUIPlugin {
    /**
     * the id of the plug-in.
     */
    public static final String PLUGIN_ID = "org.knime.workbench.explorer.view";

    private ServiceRegistration<?> m_uriToFileServiceRegistration;

    private AtomicBoolean m_prefSyncerAdded = new AtomicBoolean();

    private static ExplorerActivator plugin;

    /**
     * Creates a new activator for the explorer plugin.
     */
    public ExplorerActivator() {
        plugin = this;
    }

    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Explorer Plugin
     */
    public static ExplorerActivator getDefault() {
        return plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        super.start(bundleContext);
        m_uriToFileServiceRegistration = bundleContext.registerService(
                URIToFileResolve.class.getName(),
                new URIToFileResolveImpl(), new Hashtable<String, String>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        bundleContext.ungetService(
                m_uriToFileServiceRegistration.getReference());
        super.stop(bundleContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPreferenceStore getPreferenceStore() {
        IPreferenceStore prefStore = super.getPreferenceStore();
        if (!m_prefSyncerAdded.getAndSet(true)) {
            prefStore.addPropertyChangeListener(new ExplorerPrefsSyncer());
        }
        return prefStore;
    }
}
