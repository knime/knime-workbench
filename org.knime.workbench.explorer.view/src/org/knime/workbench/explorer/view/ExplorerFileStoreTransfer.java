/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * Created: Jan 23, 2012
 * Author: morent
 */

package org.knime.workbench.explorer.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;

/**
 * Transfer for arrays of AbstractExplorerFileStore URIs. Only URIs with
 * {@link ExplorerFileSystem}#SCHEME are supported.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerFileStoreTransfer extends ByteArrayTransfer {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            ExplorerFileStoreTransfer.class);

    private static final String TYPE_NAME = "knime-explorerfs-transfer";
    private static final int TYPE_ID = registerType(TYPE_NAME);
    private static final ExplorerFileStoreTransfer INSTANCE
            = new ExplorerFileStoreTransfer();

    private boolean m_isCut = false;

    /**
     * {@inheritDoc}
     */
    @Override
    protected int[] getTypeIds() {
        return new int[]{TYPE_ID};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getTypeNames() {
        return new String[]{TYPE_NAME};
    }


    /**
     * @return the single instance
     */
    public static ExplorerFileStoreTransfer getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void javaToNative(final Object object,
            final TransferData transferData) {
        if (object == null || !(object instanceof URI[])) {
            return;
        }

        if (isSupportedType(transferData)) {
            URI[] fileStores = (URI[])object;
            try {
                // write data to a byte array,
                // then ask super to convert to pMedium
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream writeOut = new DataOutputStream(out);
                for (int i = 0; i < fileStores.length; i++) {
                    String uriStr = fileStores[i].toString();
                    byte[] name = uriStr.getBytes();
                    writeOut.writeInt(name.length);
                    writeOut.write(name);
                }
                byte[] buffer = out.toByteArray();
                writeOut.close();
                super.javaToNative(buffer, transferData);
            } catch (IOException e) {
                LOGGER.error("IO Exception", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nativeToJava(final TransferData transferData) {
        if (isSupportedType(transferData)) {
            byte[] buffer = (byte[])super.nativeToJava(transferData);
            if (buffer == null) {
                return null;
            }
            ArrayList<URI> fileStores = new ArrayList<URI>();
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(buffer);
                DataInputStream readIn = new DataInputStream(in);
                while (readIn.available() > 1) {
                    int size = readIn.readInt();
                    byte[] name = new byte[size];
                    readIn.read(name);
                    String uriStr = new String(name);
                    fileStores.add(new URI(uriStr));
                }
                readIn.close();
            } catch (IOException ex) {
                LOGGER.error("IO Exception", ex);
                return null;
            } catch (URISyntaxException use) {
                LOGGER.error("URI Syntax Exception", use);
                return null;
            }
            return fileStores.toArray(new URI[fileStores.size()]);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean validate(final Object object) {
        if ((!(object instanceof URI[])) || (((URI[])object).length == 0)) {
            return false;
        }
        for (URI s : (URI[])object) {
            if (s == null) {
                return false;
            } else {
                // make sure that we have only "knime" URIs
                return ExplorerFileSystem.SCHEME.equals(s.getScheme());
            }
        }
        return true;
    }

    /**
     * @return true, if the transfer operation represents a "cut", false if it
     *      is a "copy"
     */
    public boolean isCut() {
        return m_isCut;
    }

    /**
     * @param isCut set to true, if the transfer shall represent a "cut", false
     *      for a "copy"
     */
    public void setCut(final boolean isCut) {
        m_isCut = isCut;
    }

}
