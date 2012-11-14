/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
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
 * Author: Peter Ohl
 */
package org.knime.workbench.explorer.filesystem;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Used to upload items from a remote store location. The data sent must be
 * in the format expected by the server for that item.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public abstract class RemoteUploadStream extends OutputStream {

    public abstract void cancel() throws IOException;
}
