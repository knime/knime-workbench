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
 *   Mar 13, 2018 (Tobias Urhaug): created
 */
package org.knime.workbench.explorer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.http.client.utils.URIBuilder;

/**
 * Utility class for encoding the path segments of URIs/URLs.
 *
 * @author Tobias Urhaug
 */
final class URIPathEncoder {

    private final static String UNC_PREFIX = "//";

    private final Charset m_encoding;

    public URIPathEncoder(final Charset encoding) {
        m_encoding = encoding;
    }

    /**
     * Encodes the path segments of a URL.
     *
     * @param url URL to be encoded
     * @return an equivalent URL with encoded path or the input URL itself if it has
     * a syntax error.
     */
    public URL encodePathSegments(final URL url) {
        try {
            return encodePathSegments(url.toURI()).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            return url;
        }
    }

    /**
     * Encodes the path segments of a URI.
     *
     * @param uri URI to be encoded
     * @return an equivalent URI with all path segment encoded
     */
    public URI encodePathSegments(final URI uri) {
        return hasUNCPath(uri) ? encodeUNC_URI(uri) : encodeURI(uri);
    }

    private static boolean hasUNCPath(final URI uri) {
        String path = uri.getPath();
        return path != null && path.startsWith(UNC_PREFIX);
    }

    /**
     * A UNC URI has no host and its scheme specific part must start with four
     * leading slashes.
     *
     * @param uri URI to be encoded
     * @return an equivalent URI with encoded path or the input URI itself if it has
     * a syntax error.
     */
    private URI encodeUNC_URI(final URI uri) {
        try {
            return new URI(uri.getScheme(), encodeUNCPath(uri), uri.getFragment());
        } catch (URISyntaxException e) {
            return uri;
        }
    }

    /**
     * UNC paths must start with four leading slashes in order to be interpreted correctly
     * downstream in our system. The encoded URI retrieved from the encodeURI method will
     * return a path which is normalized and only contains one leading slash. For this
     * reason it is safe to append three slashes to the encoded path to ensure it is a UNC path.
     *
     * @param uri URI to be encoded
     * @return a string representation of the encoded UNC path
     */
    private String encodeUNCPath(final URI uri) {
        final var uriEncoded = encodeURI(uri);
        return "////" + uriEncoded.getHost() + uriEncoded.getPath();
    }

    /**
     * Encodes the path segments without encoding reserved characters of a URI.
     * The setPath method must be called as it is where the actual encoding happens.
     * The Apache URIBuilder does not encode the URI in the builder constructor.
     *
     * @param uri URI to be encoded
     * @return an equivalent URI with encoded path or the input URI itself if it has
     * a syntax error.
     */
    private URI encodeURI(final URI uri) {
        try {
            return
                new URIBuilder(uri)
                    .setCharset(m_encoding)
                    .setPath(uri.getPath())
                    .build();
        } catch (URISyntaxException e) {
            return uri;
        }
    }

}
