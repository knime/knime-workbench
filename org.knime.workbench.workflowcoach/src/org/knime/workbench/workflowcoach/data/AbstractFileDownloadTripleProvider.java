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
 *   Apr 14, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeFrequencies;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeTriple;
import org.knime.core.ui.workflowcoach.data.UpdatableNodeTripleProvider;
import org.knime.core.util.auth.SuppressingAuthenticator;
import org.knime.core.util.exception.HttpExceptionUtils;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * A node triple provider that downloads the nodes triples from a url and stores it to a file.
 *
 * @author Martin Horn, University of Konstanz
 */
public abstract class AbstractFileDownloadTripleProvider implements UpdatableNodeTripleProvider {

    private static final int TIMEOUT = 10000; //10 seconds

    /**
     * Name of the temporary file the download file is stored into before it is checked and renamed to the desired file
     * name.
     */
    private static final String TMP_FILE_NAME = "file_download.temp";

    private final String m_url;

    private final Path m_file;

    private final Path m_tmpFile;

    /**
     * Creates a new triple provider.
     *
     * @param url the URL to download the file from
     * @param fileName the file name to store the downloaded nodes triples to - file name only, not a path!
     *
     */
    protected AbstractFileDownloadTripleProvider(final String url, final String fileName) {
        m_url = url;
        m_file = Paths.get(KNIMEConstants.getKNIMEHomeDir(), fileName);
        m_tmpFile = Paths.get(KNIMEConstants.getKNIMEHomeDir(), TMP_FILE_NAME);
    }

    @Override
    public Stream<NodeTriple> getNodeTriples() throws IOException {
        try (final var inStream = Files.newInputStream(m_file)) {
            return NodeFrequencies.from(inStream).getFrequencies().stream();
        }
    }

    @Override
    public boolean updateRequired() {
        return !Files.exists(m_file);
    }

    @Override
    public void update() throws IOException {
        final var clientBuilder = ClientBuilder.newBuilder() //
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS) //
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        try (final var client = clientBuilder.build()) {
            final var requestBuilder = client.target(m_url).request() //
                    .acceptEncoding("gzip");

            if (Files.exists(m_file)) {
                // date is automatically converted to the correct format
                final var lastModified = Date.from(Files.getLastModifiedTime(m_file).toInstant());
                requestBuilder.header("If-Modified-Since", getHttpDateFormat().format(lastModified));
            }

            try (final var supp = SuppressingAuthenticator.suppressDelegate();
                    final var response = requestBuilder.get()) {
                final var statusInfo = response.getStatusInfo();
                if (statusInfo.toEnum() == Status.NOT_MODIFIED) {
                    return;
                }

                if (statusInfo.getFamily() != Status.Family.SUCCESSFUL) {
                    throw HttpExceptionUtils.wrapException(statusInfo.getStatusCode(),
                        "Cannot access server node recommendation file: " + statusInfo.getReasonPhrase());
                }

                //download and store the file
                try (final var in = getInputStream(response)) {
                    Files.copy(in, m_tmpFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        //check the download and rename the file
        try {
            checkDownloadedFile(m_tmpFile);
            Files.move(m_tmpFile, m_file, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            //delete temporary file
            Files.deleteIfExists(m_tmpFile);
        }
    }

    /**
     * Attempts to parse the temporary file containing the downloaded recommendation data. If the file does not contain
     * node triples an {@code IOException} is thrown. Necessary to detect e.g. login-webpages in hotels.
     * @see NodeFrequencies#from(InputStream)
     *
     * @param file the temporary file containing the downloaded data
     * @throws IOException throws an exception with an explaining error message if something is wrong with the
     *             downloaded file
     */
    protected void checkDownloadedFile(final Path file) throws IOException {
        try (final var in = Files.newInputStream(file)) {
            NodeFrequencies.from(in);
        } catch (IOException e) {
            throw new IOException("Downloaded file doesn't contain node recommendation data.", e);
        }
    }

    @Override
    public Optional<LocalDateTime> getLastUpdate() {
        try {
            if (Files.exists(m_file)) {
                return Optional
                    .of(LocalDateTime.ofInstant(Files.getLastModifiedTime(m_file).toInstant(), ZoneId.systemDefault()));
            } else {
                return Optional.empty();
            }
        } catch (IOException ex) {
            NodeLogger.getLogger(getClass())
                .warn("Could not determine last update of '" + m_file + "': " + ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private static InputStream getInputStream(final Response response) throws IOException {
        final var stream = response.readEntity(InputStream.class);
        return "gzip".equals(response.getHeaderString("Content-Encoding")) ? new GZIPInputStream(stream) : stream;
    }

    private static SimpleDateFormat getHttpDateFormat() {
        final var dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        final var tZone = TimeZone.getTimeZone("GMT");
        dateFormat.setTimeZone(tZone);
        return dateFormat;
    }
}
