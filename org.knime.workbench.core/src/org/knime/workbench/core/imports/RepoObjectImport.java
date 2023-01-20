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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import org.knime.core.node.workflow.contextv2.LocationInfo;

/**
 * Holds the information required to import a repository object (workflow, component, file etc.) in to the AP (e.g., a
 * workflow put into the explorer or a component added to a workflow).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface RepoObjectImport extends EntityImport {

    /**
     * Repository object types.
     */
    static enum RepoObjectType {
            /**
             * Aka component.
             */
            WorkflowTemplate,
            /**
             * A workflowgroup.
             */
            WorkflowGroup,
            /**
             * A workflow.
             */
            Workflow,
            /**
             * A data file.
             */
            Data,
            /**
             * Any other type of repository object.
             */
            Other;
    }

    /**
     * @return the type of the repository object
     */
    RepoObjectType getType();

    /**
     * @return the object name
     */
    String getName();

    /**
     * The knime-URI (i.e. https://knime/...) that references the repository object.
     *
     * @return a knime-URI
     */
    URI getKnimeURI();

    /**
     * @return an URI to download/get the object
     */
    URI getDataURI();

    /**
     * The ETag of the repository object.
     *
     * @return ETag of the repo item if available, {@link Optional#empty()} otherwise
     * @since 4.7.1
     */
    Optional<String> getRepositoryETag();

    /**
     * Returns the location info for the workflow being downloaded.
     *
     * @return location info
     */
    Optional<? extends LocationInfo> locationInfo();

    /**
     * @return the connection to download the object
     * @throws IOException if something went wrong while establishing the connection
     */
    default HttpURLConnection getData() throws IOException {
        return (HttpURLConnection)getDataURI().toURL().openConnection();
    }
}
