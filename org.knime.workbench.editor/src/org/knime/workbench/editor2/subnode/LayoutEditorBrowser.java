/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
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
 * History
 *   Created on Jun 26, 2021 by hornm
 */
package org.knime.workbench.editor2.subnode;

import java.util.function.Function;

import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.GridData;

/**
 * A browser abstraction for the layout editor which allows one to provide different browser implementations depending
 * on, e.g., a system property.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public interface LayoutEditorBrowser { // NOSONAR

    void setUrl(String string);

    void setLayoutData(GridData gridData);

    void addProgressListener(ProgressListener progressListener);

    void evaluate(String string);

    boolean isDisposed();

    void dispose();

    LayoutEditorBrowserFunction registerBrowserFunction(String name, Function<Object[], Object> function);

    interface LayoutEditorBrowserFunction {

        void dispose();

        boolean isDisposed();

    }

}
