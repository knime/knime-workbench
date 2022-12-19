/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 19, 2022 (hornm): created
 */
package org.knime.workbench.editor2.subnode;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * Creates {@link LayoutEditorBrowser}-instances.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
final class LayoutEditorBrowserFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LayoutEditorBrowserFactory.class);

    private static final String LAYOUT_EDITOR_BROWSER_SYSTEM_PROPERTY_KEY = "knime.layout_editor.browser";

    private static final String LAYOUT_EDITOR_BROWSER_EXTENSION_ID = "org.knime.workbench.editor.LayoutEditorBrowser";

    /**
     * @param parent
     * @return a new browser instance
     */
    static LayoutEditorBrowser createBrowser(final Composite parent) {
        String browserProp = System.getProperty(LAYOUT_EDITOR_BROWSER_SYSTEM_PROPERTY_KEY);
        var layoutEditorBrowserClass = getLayoutEditorBrowserFromExtensionPoint();
        if (browserProp != null && !"swt".equals(browserProp) && layoutEditorBrowserClass != null) {
            var layoutEditorBrowser = createLayoutEditorBrowserInstance(layoutEditorBrowserClass, parent);
            if (layoutEditorBrowser != null) {
                LOGGER.info("Browser used for the layout editor: " + layoutEditorBrowser);
                return layoutEditorBrowser;
            }
        }
        LOGGER.info("Browser used for the layout editor: SWT Browser");
        return new SwtBrowser(parent);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends LayoutEditorBrowser> getLayoutEditorBrowserFromExtensionPoint() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(LAYOUT_EDITOR_BROWSER_EXTENSION_ID);
        CheckUtils.checkState(point != null, "Invalid extension point: %s", LAYOUT_EDITOR_BROWSER_EXTENSION_ID);
        var iterator = Arrays.stream(point.getExtensions())//
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())).iterator();
        if (!iterator.hasNext()) {
            // no layout editor browser extension registered
            return null;
        }
        var el = iterator.next();
        try {
            return (Class<? extends LayoutEditorBrowser>)Platform
                .getBundle(el.getDeclaringExtension().getContributor().getName()).loadClass(el.getAttribute("class"));
        } catch (ClassNotFoundException | InvalidRegistryObjectException | IllegalArgumentException
                | SecurityException e) {
            LOGGER.error("Failed to create layout editor browser from extension point.", e);
            return null;
        }
    }

    private static LayoutEditorBrowser createLayoutEditorBrowserInstance(
        final Class<? extends LayoutEditorBrowser> layoutEditorBrowserClass, final Composite parent) {
        try {
            return layoutEditorBrowserClass.getDeclaredConstructor(Composite.class).newInstance(parent);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            LOGGER.error("Layout editor browser of class '" + layoutEditorBrowserClass.getName()
                + "' could not be instantiated.", e);
            return null;
        }
    }

    private LayoutEditorBrowserFactory() {
        //
    }

}
