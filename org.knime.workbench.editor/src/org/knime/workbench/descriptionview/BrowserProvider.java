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
 *   Dec 7, 2019 (loki): created
 */
package org.knime.workbench.descriptionview;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.knime.core.node.NodeLogger;

/**
 * The genesis for this class is https://knime-com.atlassian.net/browse/AP-13283 - this class takes care of constructing
 * a {@link Browser} instance or a fallback version in certain situations and updating that view with provided content.
 *
 * @author loki der quaeler
 */
public class BrowserProvider implements LocationListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(BrowserProvider.class);

    private static AtomicBoolean IS_GTK_POISON_COMBINATION = null;


    private final Browser m_browser;
    private final FallbackBrowser m_fallbackBrowser;

    /**
     * This must be called on the SWT thread.
     *
     * @param parent if the layout set on this instance is {@link GridLayout} then an appropriate filling and grabbing
     *            {@link GridData} will be set as layout data on the created browser
     * @param beConcernedAboutGTKWeirdness if true, known poison-combinations of GTK and WebkitGTK versions will trigger
     *            the usage of the fallback browser. <b>If you set this to true, please read the code comments in this
     *            constructor.</b>
     */
    public BrowserProvider(final Composite parent, final boolean beConcernedAboutGTKWeirdness) {
        Composite compositeToLayout;
        Browser b;
        FallbackBrowser fb;

        try {
            if ((IS_GTK_POISON_COMBINATION != null) && IS_GTK_POISON_COMBINATION.get() && beConcernedAboutGTKWeirdness) {
                b = null;
                fb = new FallbackBrowser(parent, false, (SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL));
                compositeToLayout = fb.getStyledText();
            } else {
                fb = null;
                b = new Browser(parent, SWT.NONE);
                b.addLocationListener(this);
                b.setText("");
                compositeToLayout = b;
                if (IS_GTK_POISON_COMBINATION == null) {
                    b.addPaintListener((event) -> {
                        /*
                         * There is some pretty underhanded things going on here:
                         *
                         * Firstly, we know that (currently) the only case  in which we are concerned about the weirdness is for the
                         *  Component Meta View, and we know that (currently) that view can never be seen during startup.
                         *
                         * Secondly, we know that prior to having an SWT Browser instance display for the first time, the native
                         *  libraries do not yet load and, as a result, the webkitgtk version system property is not set - so
                         *  we must actually go through the displaying of the SWT Browser in the UI before we can see whether
                         *  it is poisonous and will crash the app in the Component Meta View. We'd be screwed if the SWT Browser
                         *  in a poison combination *always* crashed the app (not totally screwed, we'd just need to get even
                         *  more hacky.)
                         *
                         * So, we rely upon the fact that by the time beConcernedAboutGTKWeirdness==true, we've already displayed
                         *  the node description view, and thereby the SWT Browser.
                         */
                        if (IS_GTK_POISON_COMBINATION == null) {
                            final String gtkVersion = System.getProperty("org.eclipse.swt.internal.gtk.version");
                            if (gtkVersion != null) {
                                final String webkitGTKVersion =
                                    System.getProperty("org.eclipse.swt.internal.webkitgtk.version");
                                final boolean mortizMint18x =
                                        ("3.18.9".equals(gtkVersion) && "2.4.11".equals(webkitGTKVersion));

                                IS_GTK_POISON_COMBINATION = new AtomicBoolean(mortizMint18x);
                                if (IS_GTK_POISON_COMBINATION.get()) {
                                    LOGGER.warn("Detected poison GTK/WebkitGTK combination: " + gtkVersion + "/"
                                        + webkitGTKVersion);
                                }
                            } else {
                                IS_GTK_POISON_COMBINATION = new AtomicBoolean(false);
                            }
                        }
                    });
                }
            }
        } catch (SWTError e) {
            LOGGER.warn("No html browser for node description available.", e);
            b = null;
            fb = new FallbackBrowser(parent, true, (SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL));
            compositeToLayout = fb.getStyledText();
            IS_GTK_POISON_COMBINATION = new AtomicBoolean(true);
        }

        if (parent.getLayout() instanceof GridLayout) {
            final GridData gd = new GridData();
            gd.horizontalAlignment = SWT.FILL;
            gd.grabExcessHorizontalSpace = true;
            gd.verticalAlignment = SWT.FILL;
            gd.heightHint = 296;
            compositeToLayout.setLayoutData(gd);
        }

        m_browser = b;
        m_fallbackBrowser = fb;
    }

    /**
     * @return the result of invoking {@code setFocus()} on the browser that is in view.
     */
    public boolean setFocus() {
        if (m_browser != null) {
            return m_browser.setFocus();
        } else {
            return m_fallbackBrowser.setFocus();
        }
    }

    /**
     * This need not be called on the SWT thread.
     *
     * @param html an HTML document to be rendered; if we are wrapping a fallback browser, an XSLT will be applied to
     *            the content and the resulting text will be displayed in the view.
     */
    public void updateBrowserContent(final String html) {
        if (m_browser != null) {
            m_browser.getDisplay().asyncExec(() -> {
                if (!m_browser.isDisposed()) {
                    m_browser.setText(html);
                }
            });
        } else {
            m_fallbackBrowser.getDisplay().asyncExec(() -> {
                m_fallbackBrowser.setText(html);
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changed(final LocationEvent event) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changing(final LocationEvent event) {
        if (!event.location.startsWith("about:")) {
            final IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            try {
                final IWebBrowser browser = browserSupport.getExternalBrowser();
                browser.openURL(new URL(event.location));
                event.doit = false;
            } catch (PartInitException ex) {
                LOGGER.error(ex.getMessage(), ex);
            } catch (MalformedURLException ex) {
                LOGGER.warn("Cannot open URL '" + event.location + "'", ex);
                // just ignore it and let the default handle this case
            }
        }
    }
}
