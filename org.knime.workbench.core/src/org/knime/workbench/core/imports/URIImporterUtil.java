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
 *   Nov 18, 2020 (hornm): created
 */
package org.knime.workbench.core.imports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.URIUtil;
import org.knime.workbench.core.imports.RepoObjectImport.RepoObjectType;

/**
 * Utility methods to import stuff from URIs.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class URIImporterUtil {

    private URIImporterUtil() {
        // utility class
    }

    /**
     * Downloads a file from the provided obj-import instance into a given tmp directory.
     *
     * @param objImport the import representing where to download the file from
     * @param destDir the destination where to download the file into
     * @return the downloaded file
     * @throws IOException
     */
    public static File fetchFile(final RepoObjectImport objImport, final File destDir) throws IOException {
        HttpURLConnection dataConnection = objImport.getData();
        File tmpFile = download(dataConnection);

        //change file extension according to the data object to import
        RepoObjectType objType = objImport.getType();
        String fileExt = "";
        if (objType == RepoObjectType.Workflow) {
            fileExt = "." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION;
        } else if (objType == RepoObjectType.WorkflowGroup) {
            fileExt = "." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION;
        } else {
            //
        }

        File file = new File(destDir, objImport.getName() + fileExt);
        if (!tmpFile.renameTo(file)) {
            throw new IOException(
                "Import failed. The temporary file '" + tmpFile + "' couldn't be renamed to '" + file + "'");
        } else {
            return file;
        }
    }

    /**
     * Retrieves the import details (represented as {@link RepoObjectImport}) from an URI.
     *
     * @param uri the uri to retreive the details from
     * @return the import details or an empty optional if none available
     */
    public static Optional<RepoObjectImport> getRepoObjectImportFromURI(final String uri) {
        URI knimeURI = URIUtil.createEncodedURI(uri.split("\n")[0]).orElse(null);
        if (knimeURI == null) {
            return Optional.empty();
        }

        Optional<EntityImport> entityImport;
        try {
            entityImport = URIImporterFinder.getInstance().createEntityImportFor(knimeURI);
        } catch (ImportForbiddenException e) {
            NodeLogger.getLogger(URIImporterUtil.class).warn(e.getMessage(), e);
            return Optional.empty();
        }
        if (!entityImport.isPresent()) {
            NodeLogger.getLogger(URIImporterUtil.class).warn("Object at URI '" + knimeURI + "' not found");
            return Optional.empty();
        }
        return entityImport.map(i -> (RepoObjectImport)i);
    }

    private static File download(final HttpURLConnection connection) throws IOException {
        File f = FileUtil.createTempFile("download", ".bin");
        try (InputStream is = connection.getInputStream(); OutputStream os = new FileOutputStream(f)) {
            IOUtils.copy(is, os);
        }
        return f;
    }

}
