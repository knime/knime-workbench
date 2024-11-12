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

import java.net.URI;
import java.util.Optional;

/**
 * Imports entities, such as workflows, components, nodes or extensions into the AP (e.g., by drag'n'drop or
 * copy&paste).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface URIImporter {

    /**
     * Normal priority value.
     */
    static final int NORMAL_PRIORITY = 0;

    /**
     * Helper to create the correct entity import instance.
     *
     * @param uri the URI to create the import instance from
     * @param importer the importer to use to create the import instance
     * @param importClass the class of the instance to create
     * @return the import instance or an empty optional, if instance couldn't be created (e.g., because the URI doesn't
     *         represent an entity of the given type)
     * @throws ImportForbiddenException in case the uri couldn't be imported because user is not logged in
     */
    @SuppressWarnings("unchecked")
    public static <I extends EntityImport> Optional<I> createEntityImport(final URI uri, final URIImporter importer,
        final Class<I> importClass) throws ImportForbiddenException {
        if (RepoObjectImport.class.isAssignableFrom(importClass)) {
            return (Optional<I>)importer.createRepoObjectImport(uri);
        } else if (NodeImport.class.isAssignableFrom(importClass)) {
            return (Optional<I>)importer.createNodeImport(uri);
        } else if (ExtensionImport.class.isAssignableFrom(importClass)) {
            return (Optional<I>)importer.createExtensionImport(uri);
        } else if (SecretImport.class.isAssignableFrom(importClass)) {
            return (Optional<I>)importer.createSecretImport(uri);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Helper to create the correct entity import instance.
     *
     * @param uri the URI to create the import instance from
     * @param importer the importer to use to create the import instance
     * @return the import instance or an empty optional, if instance couldn't be created (e.g., because the URI doesn't
     *         represent an entity of the given type)
     * @throws ImportForbiddenException in case the uri couldn't be imported because user is not logged in
     */
    @SuppressWarnings("unchecked")
    public static <I extends EntityImport> Optional<I> createEntityImport(final URI uri, final URIImporter importer)
        throws ImportForbiddenException {
        Optional<Class<? extends EntityImport>> entityImportClass = importer.getEntityImportClass(uri);
        if (entityImportClass.isPresent()) {
            return (Optional<I>)createEntityImport(uri, importer, entityImportClass.get());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gives a priority of this importer to decide what importer to prefer if multiple can handle the same URI-type
     * (i.e. see {@link #canHandle(URI)}).
     *
     * @return the priority
     */
    default int getPriority() {
        return NORMAL_PRIORITY;
    }

    /**
     * Determines whether the URI importer can handle use the given URI to import entities. I.e. it returns
     * <code>true</code> {@link #getEntityImportClass(URI)} is very likely to return a non-empty optional!
     *
     * @param uri
     * @return <code>true</code> if the URI can be handled by this importer
     */
    boolean canHandle(URI uri);

    /**
     * Determines the actual import class from the given URI.
     *
     * @param uri the URI to determine the import class from
     * @return the import class or and empty optional if it couldn't be determined from the given URI
     */
    Optional<Class<? extends EntityImport>> getEntityImportClass(URI uri);

    /**
     * Creates the actual {@link NodeImport}.
     *
     * @param uri the URI to create the import from
     * @return the import or an empty optional if it couldn't be created from the given URI
     * @throws ImportForbiddenException in case the uri couldn't be imported because user is not logged in
     */
    Optional<NodeImport> createNodeImport(URI uri) throws ImportForbiddenException;

    /**
     * Creates the actual {@link ExtensionImport}.
     *
     * @param uri the URI to create the import from
     * @return the import or an empty optional if it couldn't be created from the given URI
     * @throws ImportForbiddenException in case the uri couldn't be imported because user is not logged in
     */
    Optional<ExtensionImport> createExtensionImport(URI uri) throws ImportForbiddenException;

    /**
     * Creates the actual {@link SecretImport}.
     *
     * @param uri the URI to create the import from
     * @return the import or an empty optional if it couldn't be created from the given URI
     * @throws ImportForbiddenException in case the uri couldn't be imported because user is not logged in
     */
    Optional<SecretImport> createSecretImport(URI uri) throws ImportForbiddenException;

    /**
     * Creates the actual {@link RepoObjectImport}.
     *
     * @param uri the URI to create the import from
     * @return the import or an empty optional if it couldn't be created from the given URI
     * @throws ImportForbiddenException in case the uri couldn't be imported because user is not logged in
     */
    Optional<RepoObjectImport> createRepoObjectImport(URI uri) throws ImportForbiddenException;
}
