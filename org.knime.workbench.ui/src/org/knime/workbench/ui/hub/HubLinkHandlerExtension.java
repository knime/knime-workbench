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
 *   Nov 5, 2020 (hornm): created
 */
package org.knime.workbench.ui.hub;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Utility class to work with {@link HubLinkHandler}-extensions.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class HubLinkHandlerExtension {

    private static final String EXT_POINT_ID = "org.knime.workbench.ui.HubLinkHandler";

    private static List<HubLinkHandler> handlers;

    private HubLinkHandlerExtension() {
        // utility class
    }

    /**
     * Special handling of hub links (e.g. to be opened in the Hub View instead of the system browser).
     *
     * @param url the link url to be handled
     * @return <code>true</code> if the link has been handled, otherwise <code>false</code>
     */
    public static boolean handleLink(final String url) {
        if (handlers == null) {
            handlers = collectHubLinkHandlers();
        }
        return handlers.stream().anyMatch(h -> h.handleLink(url));
    }

    private static List<HubLinkHandler> collectHubLinkHandlers() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        return Stream.of(point.getExtensions()).flatMap(ext -> Stream.of(ext.getConfigurationElements()))
            .map(HubLinkHandlerExtension::getHubLinkHandler).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Gets the {@link HubLinkHandler} from an eclipse extension.
     *
     * @param cfe an eclipse extension
     * @return null, if the extension does not implement the {@link HubLinkHandler} interface
     */
    private static HubLinkHandler getHubLinkHandler(final IConfigurationElement cfe) {
        try {
            HubLinkHandler ext = (HubLinkHandler)cfe.createExecutableExtension("impl");
            NodeLogger.getLogger(HubLinkHandlerExtension.class).debugWithFormat("Added HubLinkHandler '%s' from '%s'",
                ext.getClass().getName(), cfe.getContributor().getName());
            return ext;
        } catch (CoreException ex) {
            NodeLogger.getLogger(HubLinkHandlerExtension.class)
                .error(String.format("Looking for an implementation of the HubLinkHandler extension point,\n"
                    + "but could not process extension %s: %s", cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

}
