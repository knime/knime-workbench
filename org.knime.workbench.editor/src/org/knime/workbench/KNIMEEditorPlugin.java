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
package org.knime.workbench;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ThreadsafeImageRegistry;
import org.knime.workbench.editor2.svgexport.WorkflowSVGExport;
import org.knime.workbench.editor2.svgexport.WorkflowSVGExportAction;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class for the editor.
 *
 * @author Florian Georg, University of Konstanz
 */
public class KNIMEEditorPlugin extends AbstractUIPlugin {
    // Make sure that this *always* matches the ID in plugin.xml
    /** The Plugin ID. */
    public static final String PLUGIN_ID = "org.knime.workbench.editor";

    // The shared instance.
    private static KNIMEEditorPlugin plugin;

    /** SVG service provided by *.editor.svgexport fragment, see #start method. */
    private WorkflowSVGExport m_svgExport;

    private WorkflowSVGExportAction m_svgExportAction;

    /**
     * The constructor.
     */
    public KNIMEEditorPlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     *
     * @param context The bundle context
     * @throws Exception If failed
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        // TODO: temporary hug for preference page, to ensure that the
        // MasterKeySupplier is set correctly before the editor is started
        KNIMEUIPlugin.getDefault().getPreferenceStore();

        // the svg export is provided by the *.editor.svgexport fragment. Fragments can't have bundle activators
        // so this host plugin does it (not sure what the eclipse standard way is)
        Class<?> svgExportClass = null;
        final String className = "org.knime.workbench.editor.svgexport.exportservice.WorkflowSVGExportImpl";
        try {
            svgExportClass = Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            NodeLogger.getLogger(getClass()).debug("Workflow SVG export not available, unable to instantiate \""
                + className + "\"");
        }
        if (svgExportClass != null) {
            try {
                Object instance = svgExportClass.newInstance();
                m_svgExport = (WorkflowSVGExport)instance;
            } catch (Exception e) {
                NodeLogger.getLogger(getClass()).error(
                    "Unable to instantiate" + WorkflowSVGExport.class.getName() + " implementation", e);
            }
        }

        // the svg export action is provided by the *.editor.svgexport fragment. Fragments can't have bundle activators
        // so this host plugin does it (not sure what the eclipse standard way is)
        Class<?> svgExportActionClass = null;
        final String classNameAction = "org.knime.workbench.editor.svgexport.exportservice.WorkflowSVGExportActionImpl";
        try {
            svgExportActionClass = Class.forName(classNameAction);
        } catch (ClassNotFoundException cnfe) {
            NodeLogger.getLogger(getClass()).debug("Workflow SVG export not available, unable to instantiate \""
                + classNameAction + "\"");
        }
        if (svgExportActionClass != null) {
            try {
                Object instance = svgExportActionClass.newInstance();
                m_svgExportAction = (WorkflowSVGExportAction)instance;
            } catch (Exception e) {
                NodeLogger.getLogger(getClass()).error(
                    "Unable to instantiate" + WorkflowSVGExportAction.class.getName() + " implementation", e);
            }
        }
        initChromiumSWT();
    }

    /** (2020-06-28) Temporary workaround added as part of AP-14231 -- If Chromium Embedded Framwork / Chromium.SWT is
     * installed it must be loaded prior the SWT browser instance. The maintainers provide a (again, temporary)
     * workaround to ensure it. {@link org.eclipse.ui.IStartup IStartUp} did not suffice / was called too late.
     */
    private static void initChromiumSWT()
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final long start = System.currentTimeMillis();
        Bundle chromiumStartupBundle = Platform.getBundle("com.make.chromium.startup");
        if (chromiumStartupBundle != null) {
            NodeLogger logger = NodeLogger.getLogger(KNIMEEditorPlugin.class);
            try {
                Class<?> startupCl = chromiumStartupBundle.loadClass("com.make.chromium.startup.ChromiumStartup");
                Method initMethod = startupCl.getMethod("init");
                initMethod.invoke(null);
                logger.debugWithFormat("Succesfully invoked ChromiumStartup.init() (%d ms)",
                    System.currentTimeMillis() - start);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                    | InvocationTargetException e) {
                logger.error("Failed to invoke ChromiumStartup.init()", e);
            }
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     *
     * @param context The bundle context
     * @throws Exception If failed
     *
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     *
     * @return The shared instance of this plugin
     */
    public static KNIMEEditorPlugin getDefault() {
        return plugin;
    }

    /**
     * @return the svgExport used to put SVG thumbnail in workflow folder when flow is saved. Might be null if
     * *.editor.svgexport fragment is not available.
     */
    public WorkflowSVGExport getSvgExport() {
        return m_svgExport;
    }

    public WorkflowSVGExportAction getSvgExportAction() {
        return m_svgExportAction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageRegistry createImageRegistry() {
        //If we are in the UI Thread use that
        if (Display.getCurrent() != null) {
            return new ThreadsafeImageRegistry(Display.getCurrent());
        }

        if (PlatformUI.isWorkbenchRunning()) {
            return new ThreadsafeImageRegistry(PlatformUI.getWorkbench().getDisplay());
        }

        //Invalid thread access if it is not the UI Thread
        //and the workbench is not created.
        throw new SWTError(SWT.ERROR_THREAD_INVALID_ACCESS);
    }

}
