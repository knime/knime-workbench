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

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.pathresolve.URIToFileResolveImpl;
import org.knime.workbench.explorer.view.preferences.ExplorerPrefsSyncer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerActivator extends AbstractUIPlugin {

    /**
     * the id of the plug-in.
     */
    public static final String PLUGIN_ID = "org.knime.workbench.explorer.view";

    private static BundleContext context;

    private ServiceRegistration m_uriToFileServiceRegistration;

    /**
     * @return the context.
     */
    static BundleContext getContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        ExplorerActivator.context = bundleContext;
        // register our handler for the "explorer" protocol with the framework
        Hashtable<String, String[]> properties =
                new Hashtable<String, String[]>();
        properties.put(URLConstants.URL_HANDLER_PROTOCOL,
                new String[]{ExplorerFileSystem.SCHEME});
        context.registerService(URLStreamHandlerService.class.getName(),
                new ExplorerURLStreamHandler(), properties);
        KNIMECorePlugin.getDefault().getPreferenceStore()
                .addPropertyChangeListener(new ExplorerPrefsSyncer());

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
        ExplorerActivator.context = null;
    }

}
