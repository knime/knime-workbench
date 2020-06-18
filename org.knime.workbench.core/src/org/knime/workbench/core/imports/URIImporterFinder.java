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
 *   Jun 27, 2019 (hornm): created
 */
package org.knime.workbench.core.imports;

import static org.knime.workbench.core.imports.URIImporter.createEntityImport;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Helper to find {@link URIImporter}s registered at the respective extension point.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class URIImporterFinder {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(URIImporterFinder.class);

    private static final String URI_IMPORTER_EXTENSION_POINT_ID = "org.knime.workbench.core.URIImporter";

    private static URIImporterFinder INSTANCE;

    private final List<URIImporter> m_availableURIImporter;

    private URIImporterFinder() {
        List<URIImporter> list = collectURIImporter();
        Collections.sort(list, (i1, i2) -> Integer.compare(i1.getPriority(), i2.getPriority()));
        m_availableURIImporter = list;
    }

    /**
     * @return the singleton instance
     */
    public static URIImporterFinder getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new URIImporterFinder();
        }
        return INSTANCE;
    }

    /**
     * Find a {@link URIImporter} from the registered extension points that can handle the given URI.
     *
     * @param uri the uri to find an importer for
     * @return the importer or an empty optional if no importer couldn't be found
     */
    public Optional<URIImporter> findURIImporterFor(final URI uri) {
        return m_availableURIImporter.stream().filter(i -> i.canHandle(uri)).findFirst();
    }

    /**
     * Shortcut to find an importer and directly create the {@link EntityImport}.
     *
     * @param uri the uri to create the entity import from
     * @return the entity import or an empty optional if no importer couldn't be found or entity import couldn't be
     *         created
     * @throws ImportForbiddenException in case the uri couldn't be imported because user is not logged in
     */
    public Optional<EntityImport> createEntityImportFor(final URI uri) throws ImportForbiddenException {
        Optional<URIImporter> uriImporter = findURIImporterFor(uri);
        if (uriImporter.isPresent()) {
            return createEntityImport(uri, uriImporter.get());
        } else {
            return Optional.empty();
        }
    }

    private static List<URIImporter> collectURIImporter() {
        List<URIImporter> l = new ArrayList<URIImporter>(3);

        //get node triple providers from extension points
        IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(URI_IMPORTER_EXTENSION_POINT_ID);
        assert (extPoint != null) : "Invalid extension point: " + URI_IMPORTER_EXTENSION_POINT_ID;

        IExtension[] extensions = extPoint.getExtensions();
        for (IExtension ext : extensions) {
            for (IConfigurationElement conf : ext.getConfigurationElements()) {
                try {
                    URIImporter factory = (URIImporter)conf.createExecutableExtension("URIImporter");
                    l.add(factory);
                } catch (CoreException e) {
                    LOGGER.warn("Could not create URIImporter from " + conf.getAttribute("URIImporter"));
                }
            }
        }
        return l;
    }
}
