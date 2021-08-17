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

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * Chromium Embedded Framework Browser (CEF)
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class CefBrowser implements Browser {

    private final com.equo.chromium.swt.Browser m_b;

    CefBrowser(final Composite parent) {
        m_b = new com.equo.chromium.swt.Browser(parent, SWT.NONE);
    }

    @Override
    public void setUrl(final String string) {
        m_b.setUrl(string);
    }

    @Override
    public void setLayoutData(final GridData gridData) {
        m_b.setLayoutData(gridData);
    }

    @Override
    public void addProgressListener(final ProgressListener progressListener) {
        m_b.addProgressListener(progressListener);
    }

    @Override
    public void evaluate(final String string) {
        m_b.evaluate(string);
    }

    @Override
    public boolean isDisposed() {
        return m_b.isDisposed();
    }

    @Override
    public void dispose() {
        m_b.dispose();
    }

    @Override
    public Object getBrowser() {
        return m_b;
    }

}
