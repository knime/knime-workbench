/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * Created: Sep 5, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.filesystem;

import java.io.InputStream;
import java.util.List;

/**
 * Used to download workflows from a remote store location. The stream provides
 * a zipped KNIME workflow.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public abstract class RemoteFlowDownloadStream extends InputStream {

    /**
     * @return true, if the server finished packing the flow and is ready to
     *         provide data for downlaod. If false is returned, other methods
     *         block until the server is ready.
     */
    public abstract boolean readyForDownload();

    /**
     * @return the number of bytes in the stream (if known), or -1 (if not
     *         known).
     */
    public abstract long length();

    /**
     * @return messages that were created during download if available, or an
     *      empty list otherwise
     *
     * @since 3.0
     */
    public abstract List<String> getMessages();

}
