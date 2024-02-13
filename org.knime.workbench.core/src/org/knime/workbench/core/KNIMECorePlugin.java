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
 * -------------------------------------------------------------------
 *
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.NodeLoggerConfig;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.core.util.KnimeEncryption;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
import org.knime.workbench.core.util.ThreadsafeImageRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;


/**
 * The core plugin, basically a holder for the framework's jar and some minor
 * workbench componentes that are needed everywhere (ErrorDialog,...).
 *
 * NOTE: Plugins need to depend upon this, as this plugin exports the underlying
 * framework API !!
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMECorePlugin extends AbstractUIPlugin {
    /** Make sure that this *always* matches the ID in plugin.xml. */
    public static final String PLUGIN_ID = FrameworkUtil.getBundle(
            KNIMECorePlugin.class).getSymbolicName();

    // The shared instance.
    private static KNIMECorePlugin plugin;

    // Resource bundle.
    private ResourceBundle m_resourceBundle;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            KNIMECorePlugin.class);

    /**
     * Keeps list of <code>ConsoleViewAppender</code>. TODO FIXME remove
     * static if you want to have a console for each Workbench
     */
    private static final List<ConsoleViewAppender> APPENDERS =
            new ArrayList<ConsoleViewAppender>();

    /**
     * The constructor.
     */
    public KNIMECorePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        if (!Boolean.getBoolean("java.awt.headless") && (Display.getCurrent() != null)) {
            getImageRegistry();
        }

        try {
            // get the preference store
            // with the preferences for nr threads and tempDir
            final var pStore = KNIMECorePlugin.getDefault().getPreferenceStore();
            initMaxThreadCountProperty();
            initTmpDirProperty();
            // set log file level to stored
            setLogLevelOnNodeLogger(pStore.getString(HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE),
                HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE);
            // set stdout level to stored
            setLogLevelOnNodeLogger(pStore.getString(HeadlessPreferencesConstants.P_LOGLEVEL_STDOUT),
                HeadlessPreferencesConstants.P_LOGLEVEL_STDOUT);

            final boolean enableWorkflowRelativeLogging =
                    pStore.getBoolean(HeadlessPreferencesConstants.P_LOG_FILE_LOCATION);
            NodeLogger.logInWorkflowDir(enableWorkflowRelativeLogging);
            final boolean enableGlobalInWfLogging =
                    pStore.getBoolean(HeadlessPreferencesConstants.P_LOG_GLOBAL_IN_WF_DIR);
            NodeLogger.logGlobalMsgsInWfDir(enableGlobalInWfLogging);
            pStore.addPropertyChangeListener(new IPropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent event) {
                    final String propertyName = event.getProperty();
                    if (HeadlessPreferencesConstants.P_MAXIMUM_THREADS.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof Integer)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        int count;
                        try {
                            count = (Integer)event.getNewValue();
                            KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(count);
                        } catch (Exception e) {
                            LOGGER.error("Unable to get maximum thread count " + " from preference page.", e);
                        }
                    } else if (HeadlessPreferencesConstants.P_TEMP_DIR.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof String)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        String dirName = (String)event.getNewValue();
                        if (dirName.isEmpty()) {
                            return;
                        }
                        File f = new File(dirName);
                        LOGGER.debug("Setting temp dir to " + f.getAbsolutePath());
                        try {
                            KNIMEConstants.setKNIMETempDir(f);
                        } catch (Exception e) {
                            LOGGER.error("Setting temp dir failed: " + e.getMessage(), e);
                        }
                    } else if (HeadlessPreferencesConstants.P_LOG_FILE_LOCATION.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof Boolean)) {
                            // when preferences are imported and this value is not set, they send an empty string
                            return;
                        }
                        Boolean enable = (Boolean)event.getNewValue();
                        NodeLogger.logInWorkflowDir(enable);
                    } else if (HeadlessPreferencesConstants.P_LOG_GLOBAL_IN_WF_DIR.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof Boolean)) {
                            // when preferences are imported and this value is not set, they send an empty string
                            return;
                        }
                        Boolean enable = (Boolean)event.getNewValue();
                        NodeLogger.logGlobalMsgsInWfDir(enable);

                    } else if (HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE.equals(propertyName) ||
                            HeadlessPreferencesConstants.P_LOGLEVEL_STDOUT.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof String)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        String newName = (String)event.getNewValue();
                        if (newName.isEmpty()) {
                            return;
                        }
                        setLogLevelOnNodeLogger(newName, propertyName);
                    } else if (HeadlessPreferencesConstants.P_LOGLEVEL_CONSOLE.equals(propertyName)) {
                        if (!(event.getNewValue() instanceof String)) {
                            // when preferences are imported and this value is
                            // not set, they send an empty string
                            return;
                        }
                        String newName = (String)event.getNewValue();
                        if (newName.isEmpty()) {
                            return;
                        }
                        setLogLevelOnConsoleView(newName);
                    } else if (HeadlessPreferencesConstants.P_DATABASE_TIMEOUT.equals(propertyName)) {
                        //setting is still exposed in the new db preference page and stored in this preference store!!!
                        DatabaseConnectionSettings.setDatabaseTimeout(Integer.parseInt(event.getNewValue().toString()));
                    } else if (WorkflowMigrationSettings.P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED.contentEquals(propertyName)) {
                        //setting is still exposed in the new db preference page and stored in this preference store!!!
                        final Object newValue = event.getNewValue();
                        if (newValue instanceof Boolean) {
                            WorkflowMigrationSettings.setNotificationEnabled((Boolean)newValue);
                        }
                    }
                }
            });
            // end property listener

            final var logLevelConsole = pStore.getString(HeadlessPreferencesConstants.P_LOGLEVEL_CONSOLE);
            if (!Boolean.getBoolean("java.awt.headless") && PlatformUI.isWorkbenchRunning()) {
                //async exec should fix AP-13234 (deadlock):
                Display.getDefault().asyncExec(() -> {
                    try {
                        ConsoleViewAppender.FORCED_APPENDER.write(
                                KNIMEConstants.WELCOME_MESSAGE);
                        ConsoleViewAppender.INFO_APPENDER.write(
                        "Log file is located at: "
                        + KNIMEConstants.getKNIMEHomeDir() + File.separator
                        + NodeLogger.LOG_FILE + "\n");
                    } catch (IOException ioe) {
                        LOGGER.error("Could not print welcome message: ", ioe);
                    }
                    setLogLevelOnConsoleView(logLevelConsole);
                });
            }
            // encryption key supplier registered with the eclipse framework
            // and serves as a master key provider
            KnimeEncryption.setEncryptionKeySupplier(
                    new EclipseEncryptionKeySupplier());

            // continue to load deprecated database driver files from this preferences store even though they are no
            //longer exposed anywhere in the UI
            String dbDrivers = pStore.getString(
                    HeadlessPreferencesConstants.P_DATABASE_DRIVERS);
            initDatabaseDriver(dbDrivers);
            //setting is still exposed in the new db preference page and handled here!!!
            DatabaseConnectionSettings.setDatabaseTimeout(pStore
                .getInt(HeadlessPreferencesConstants.P_DATABASE_TIMEOUT));
            //setting is still exposed in the new db preference page and handled here!!!
            WorkflowMigrationSettings.setNotificationEnabled(pStore
                .getBoolean(WorkflowMigrationSettings.P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED));
        } catch (Throwable e) {
            LOGGER.error(
                "Error while starting workbench, some setting may not have been applied properly: " + e.getMessage(),
                e);
        }
    }

    private static void initDatabaseDriver(final String dbDrivers) {
        if (dbDrivers != null && !dbDrivers.trim().isEmpty()) {
            for (String d : dbDrivers.split(";")) {
                try {
                    DatabaseDriverLoader.loadDriver(new File(d));
                } catch (IOException ioe) {
                    LOGGER.warn("Can't load driver file \"" + d + "\""
                        + (ioe.getMessage() != null
                            ? ", reason: " + ioe.getMessage() : "."));
                }
            }
        }
    }

    private void initMaxThreadCountProperty() {
        IPreferenceStore pStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        int maxThreads = pStore.getInt(
                HeadlessPreferencesConstants.P_MAXIMUM_THREADS);
        var useEnv = !StringUtils.isEmpty(System.getenv(KNIMEConstants.ENV_MAX_THREAD_COUNT));
        String maxTString = useEnv ? System.getenv(KNIMEConstants.ENV_MAX_THREAD_COUNT)
            : System.getProperty(KNIMEConstants.PROPERTY_MAX_THREAD_COUNT);
        if (maxTString == null) {
            if (maxThreads <= 0) {
                LOGGER.warn("Can't set " + maxThreads
                        + " as number of threads to use");
            } else {
                KNIMEConstants.GLOBAL_THREAD_POOL.setMaxThreads(maxThreads);
                LOGGER.debug("Setting KNIME max thread count to "
                    + maxThreads);
            }
        } else {
            LOGGER.debug(String.format(
                "Ignoring thread count from preference page (%d), since it has been set by %s \"%s\" (%s)", maxThreads,
                useEnv ? "java property" : "environment variable",
                useEnv ? KNIMEConstants.ENV_MAX_THREAD_COUNT : KNIMEConstants.PROPERTY_MAX_THREAD_COUNT, maxTString));
        }
    }

    private void initTmpDirProperty() {
        IPreferenceStore pStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        String tmpDirPref = pStore.getString(
                HeadlessPreferencesConstants.P_TEMP_DIR);
        String tmpDirSystem = System.getProperty(
                KNIMEConstants.PROPERTY_TEMP_DIR);
        File tmpDir = null;
        if (tmpDirSystem == null) {
            if (tmpDirPref != null) {
                tmpDir = new File(tmpDirPref);
                if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                    LOGGER.warn("Can't set " + tmpDirPref + " as temp dir");
                    tmpDir = null;
                }
            }
        } else {
            tmpDir = new File(tmpDirSystem);
            if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                LOGGER.warn("Can't set " + tmpDirSystem + " as temp dir");
                // try to set path from preference page as fallback
                tmpDir = new File(tmpDirPref);
                if (!(tmpDir.isDirectory() && tmpDir.canWrite())) {
                    LOGGER.warn("Can't set " + tmpDirPref + " as temp dir");
                    tmpDir = null;
                }
            }
        }
        if (tmpDir != null) {
            LOGGER.debug("Setting KNIME temp dir to \""
                    + tmpDir.getAbsolutePath() + "\"");
            KNIMEConstants.setKNIMETempDir(tmpDir);
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        // remove appender listener from "our" NodeLogger
        for (int i = 0; i < APPENDERS.size(); i++) {
            removeAppender(APPENDERS.get(i));
        }
        super.stop(context);
        plugin = null;
        m_resourceBundle = null;
    }

    /**
     * Sets a given minimum log level to the given logger property. Uses the default maximum log level value of FATAL.
     *
     * @param logLevel Log level to set.
     * @param logerProperty Name of the property corresponding to NodeLoggers (found in
     *            HeadlessPreferencesConstants.P_LOGLEVEL_*).
     */
    private static void setLogLevelOnNodeLogger(final String logLevel, final String loggerProperty) {
        var nodeLoggerLevel = LEVEL.WARN;
        try {
            nodeLoggerLevel = LEVEL.valueOf(logLevel);
        } catch (IllegalArgumentException ignored) {
            LOGGER.error("Invalid log level \"" + logLevel + "\", using " + nodeLoggerLevel.name());
        }

        switch (loggerProperty) {
            case HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE -> {
                final var existing = NodeLoggerConfig.getAppenderLevelRange(NodeLogger.LOGFILE_APPENDER);
                final var max = existing != null ? existing.getSecond() : LEVEL.FATAL;
                NodeLoggerConfig.setAppenderLevelRange(NodeLogger.LOGFILE_APPENDER, nodeLoggerLevel, max);
            }
            case HeadlessPreferencesConstants.P_LOGLEVEL_STDOUT -> {
                final var existing = NodeLoggerConfig.getAppenderLevelRange(NodeLogger.STDOUT_APPENDER);
                final var max = existing != null ? existing.getSecond() : LEVEL.FATAL;
                NodeLoggerConfig.setAppenderLevelRange(NodeLogger.STDOUT_APPENDER, nodeLoggerLevel, max);
            }
            default -> LOGGER.error("Unsupported NodeLogger property, log level cannot be set: " + loggerProperty);
        }
    }

    /**
     * Register the appenders according to logLevel for the console view, i.e.
     * PreferenceConstants.P_LOGLEVEL_DEBUG, PreferenceConstants.P_LOGLEVEL_INFO, etc.
     *
     * @param logLevel The new log level.
     */
    private static void setLogLevelOnConsoleView(final String logLevel) {
        // check if can create a console view
        // only possible if we are not "headless"
        if (Boolean.valueOf(System.getProperty("java.awt.headless", "false"))) {
            return;
        }
        boolean changed = false;
        if (logLevel.equals(LEVEL.DEBUG.name())) {
            changed |= addAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.INFO.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= addAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.WARN.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= addAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else if (logLevel.equals(LEVEL.ERROR.name())) {
            changed |= removeAppender(ConsoleViewAppender.DEBUG_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.INFO_APPENDER);
            changed |= removeAppender(ConsoleViewAppender.WARN_APPENDER);
            changed |= addAppender(ConsoleViewAppender.ERROR_APPENDER);
            changed |= addAppender(ConsoleViewAppender.FATAL_ERROR_APPENDER);
        } else {
            LOGGER.warn("Invalid log level " + logLevel + "; setting to "
                    + LEVEL.WARN.name());
            setLogLevelOnConsoleView(LEVEL.WARN.name());
        }
        if (changed) {
            LOGGER.info("Setting console view log level to " + logLevel);
        }
    }

    /**
     * Add the given Appender to the NodeLogger.
     *
     * @param app Appender to add.
     * @return If the given appender was not previously registered.
     */
    static boolean addAppender(final ConsoleViewAppender app) {
        if (!APPENDERS.contains(app)) {
            NodeLogger.addKNIMEConsoleWriter(app, app.getLevel(), app.getLevel());
            APPENDERS.add(app);
            return true;
        }
        return false;
    }

    /**
     * Removes the given Appender from the NodeLogger.
     *
     * @param app Appender to remove.
     * @return If the given appended was previously registered.
     */
    static boolean removeAppender(final ConsoleViewAppender app) {
        if (APPENDERS.contains(app)) {
            NodeLogger.removeWriter(app);
            APPENDERS.remove(app);
            return true;
        }
        return false;
    }


    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Core Plugin
     */
    public static KNIMECorePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     *
     * @param key The resource key
     * @return The resource value, or the key if not found in the resource
     *         bundle
     */
    public static String getResourceString(final String key) {
        ResourceBundle bundle = KNIMECorePlugin.getDefault()
                .getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle.
     *
     * @return The resource bundle, or <code>null</code>
     */
    public ResourceBundle getResourceBundle() {
        try {
            if (m_resourceBundle == null) {
                m_resourceBundle = ResourceBundle.getBundle(plugin
                        .getClass().getName());
            }
        } catch (MissingResourceException x) {
            m_resourceBundle = null;
            WorkbenchErrorLogger
                    .warning("Could not locate resource bundle for "
                            + plugin.getClass().getName());
        }
        return m_resourceBundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageRegistry createImageRegistry() {
        //If we are in the UI Thread use that
        if (Display.getCurrent() != null) {
            return new ThreadsafeImageRegistry(Display.getCurrent());
        } else {
            Display display;
            if (PlatformUI.isWorkbenchRunning()) {
                display = PlatformUI.getWorkbench().getDisplay();
            } else {
                display = Display.getDefault();
            }
            final AtomicReference<ImageRegistry> ref = new AtomicReference<>();
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    ref.set(new ThreadsafeImageRegistry(Display.getCurrent()));
                }
            });
            return ref.get();
        }
    }
}
