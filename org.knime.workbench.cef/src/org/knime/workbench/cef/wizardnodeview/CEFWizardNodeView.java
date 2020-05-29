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
 *   May 29, 2020 (hornm): created
 */
package org.knime.workbench.cef.wizardnodeview;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.chromium.BrowserFunction;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.web.WebViewContent;
import org.knime.core.node.wizard.WizardNode;
import org.knime.workbench.editor2.WizardNodeView;

/**
 * Wizard node view implementation using the Chromium Embedded Framework (CEF) as browser.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <T>
 * @param <REP>
 * @param <VAL>
 */
public class CEFWizardNodeView<T extends ViewableModel & WizardNode<REP, VAL>, REP extends WebViewContent, VAL extends WebViewContent>
    extends WizardNodeView<T, REP, VAL> {

    /**
     * @param nodeModel
     */
    public CEFWizardNodeView(final T nodeModel) {
        super(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BrowserWrapper createBrowserWrapper(final Shell shell) {
        final Browser browser = new Browser(shell, SWT.NONE);
        return new BrowserWrapper() {

            @Override
            public void execute(final String call) {
                browser.execute(call);
            }

            @Override
            public Display getDisplay() {
                return browser.getDisplay();
            }

            @Override
            public void addProgressListener(final ProgressListener progressListener) {
                browser.addProgressListener(progressListener);
            }

            @Override
            public void setUrl(final String absolutePath) {
                browser.setUrl(absolutePath);
            }

            @Override
            public void setText(final String html) {
                browser.setText(html);
            }

            @Override
            public Shell getShell() {
                return browser.getShell();
            }

            @Override
            public String evaluate(final String evalCode) {
                return (String)browser.evaluate(evalCode);
            }

            @Override
            public boolean isDisposed() {
                return browser.isDisposed();
            }

            @Override
            public void setText(final String html, final boolean trusted) {
                browser.setText(html, trusted);
            }

            @Override
            public void setLayoutData(final GridData gridData) {
                browser.setLayoutData(gridData);
            }

            @Override
            public BrowserFunctionWrapper registerBrowserFunction(final String name,
                final Function<Object[], Object> func) {
                final BrowserFunction fct = new BrowserFunction(browser, name) {
                    @Override
                    public Object function(final Object[] args) {
                        return func.apply(args);
                    }
                };
                return new BrowserFunctionWrapper() {

                    @Override
                    public boolean isDisposed() {
                        return fct.isDisposed();
                    }

                    @Override
                    public void dispose() {
                        fct.dispose();
                    }
                };
            }

        };
    }

}
