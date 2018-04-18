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
package org.knime.workbench.explorer;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.workbench.explorer.pathresolve.URIToFileResolveImpl;
import org.knime.workbench.explorer.view.preferences.ExplorerPrefsSyncer;
import org.knime.workbench.explorer.view.preferences.MountSettings;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;

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
        addPrefSyncer();
        return prefStore;
    }

    private void addPrefSyncer() {
        if (!m_prefSyncerAdded.getAndSet(true)) {
            // AP-8989 switching to IEclipsePreferences
            ExplorerPrefsSyncer prefsSyncer = new ExplorerPrefsSyncer();
            IEclipsePreferences defaultPrefs = DefaultScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation());
            defaultPrefs.addPreferenceChangeListener(prefsSyncer);
            IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(MountSettings.getMountpointPreferenceLocation());
            preferences.addNodeChangeListener(prefsSyncer);
            preferences.addPreferenceChangeListener(prefsSyncer);
            try {

                for (String childName : preferences.childrenNames()) {
                    IEclipsePreferences childPreference = (IEclipsePreferences)preferences.node(childName);
                    childPreference.addNodeChangeListener(prefsSyncer);
                    childPreference.addPreferenceChangeListener(prefsSyncer);
                }
            } catch (BackingStoreException e) {

            }
        }
    }


}
