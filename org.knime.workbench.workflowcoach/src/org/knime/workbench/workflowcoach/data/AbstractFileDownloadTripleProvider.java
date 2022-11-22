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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
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
import java.util.stream.Stream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeFrequencies;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeTriple;
import org.knime.core.ui.workflowcoach.data.UpdatableNodeTripleProvider;

/**
 * A node triple provider that downloads the nodes triples from a url and stores it to a file.
 *
 * @author Martin Horn, University of Konstanz
 */
public abstract class AbstractFileDownloadTripleProvider implements UpdatableNodeTripleProvider {

    private static final int TIMEOUT = 10_000; //10 seconds

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
    public void update() throws Exception {

        // default of 3 retries is what we want
        final var request = new HttpGet(m_url);
        if (Files.exists(m_file)) {
            String lastModified = getHttpDateFormat().format(Date.from(Files.getLastModifiedTime(m_file).toInstant()));
            request.setHeader("If-Modified-Since", lastModified);
        }
        request.setHeader("Accept-Encoding", "gzip");

        final var configBuilder = RequestConfig.custom().setConnectTimeout(TIMEOUT);
        ProxySelector.getDefault().select(URI.create(m_url)).stream()
                .map(Proxy::address)
                .filter(InetSocketAddress.class::isInstance) // also takes care of `null`
                .map(InetSocketAddress.class::cast)
                .findFirst()
                .ifPresent(addr -> configBuilder.setProxy(new HttpHost(addr.getHostString(), addr.getPort())));

        try (final var client = HttpClientBuilder.create().setDefaultRequestConfig(configBuilder.build()).build();
                final var response = client.execute(request)) {
            final var statusLine = response.getStatusLine();
            final var statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                return;
            }

            if (statusCode != HttpStatus.SC_OK) {
                throw new HttpException("Cannot access server node recommendation file: " + statusLine);
            }

            //download and store the file
            final HttpEntity entity = response.getEntity();
            try (final var in = entity.getContent()) {
                Files.copy(in, m_tmpFile);
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
        try (final var in = new BufferedInputStream(Files.newInputStream(file))) {
            NodeFrequencies.from(in);
        } catch (IOException e) {
            throw new IOException("Downloaded file doesn't contain node recommendation data.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
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

    private static SimpleDateFormat getHttpDateFormat() {
        final var dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        final var tZone = TimeZone.getTimeZone("GMT");
        dateFormat.setTimeZone(tZone);
        return dateFormat;
    }
}
