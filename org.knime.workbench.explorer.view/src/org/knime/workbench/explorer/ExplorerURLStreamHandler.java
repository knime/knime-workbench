/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import com.knime.licenses.License;
import com.knime.licenses.LicenseStore;
import com.knime.licenses.LicenseTypes;


/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerURLStreamHandler extends AbstractURLStreamHandlerService {
    /* Check the license. */
    private static final boolean VALID_LICENSE;
    private static final String LICENSE_MESSAGE;
    static {
        License license = LicenseStore.getDefaultStore().getLicense(
                LicenseTypes.TeamSpace);
        String message = "";
        boolean valid = false;
        try {
            if (license != null) {
                valid = license.validate();
            }
        } catch (Exception e) {
            valid = false;
        }
        VALID_LICENSE = valid;
        if (!valid) {
            message = "No valid license for Team Space feature found.";
            boolean expired = license != null && license.checkExpiry();
            if (expired) {
                Date expirationDate = license.getExpirationDate();
                message += " Your license has expired on " + expirationDate
                        + ".";
            }
        }
        LICENSE_MESSAGE = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URLConnection openConnection(final URL url) throws IOException {
        if (!ExplorerFileSystem.SCHEME.equalsIgnoreCase(url
                .getProtocol())) {
            throw new IOException("Unexpected protocol: " + url.getProtocol()
                    + ". Only " + ExplorerFileSystem.SCHEME
                    + " is supported by this handler.");
        } else if (!VALID_LICENSE) {
            throw new IOException("Protocol: " + url.getProtocol()
                    + " requires a license." + LICENSE_MESSAGE);
        }
        AbstractExplorerFileStore efs;
        try {
            efs = ExplorerMountTable.getFileSystem().getStore(url.toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
        return new ExplorerURLConnection(url, efs);
    }

    /**
     * Allows the communication with a "knime" URL.
     * @author ohl, University of Konstanz
     *
     */
    class ExplorerURLConnection extends URLConnection {
        private final AbstractExplorerFileStore m_file;

        /**
         * @param url the specified url
         * @param file the specified file
         */
        public ExplorerURLConnection(final URL url,
                final AbstractExplorerFileStore file) {
            super(url);
            m_file = file;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connect() throws IOException {
            // ...
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream() throws IOException {
            try {
                return m_file.openInputStream(EFS.NONE, null);
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }
}
