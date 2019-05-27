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
 *   May 26, 2019 (loki): created
 */
package org.knime.workbench.searchhubview;

import java.net.URLEncoder;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.NodeLogger;

/**
 * The genesis of this view is https://knime-com.atlassian.net/browse/AP-11738
 *
 * @author loki der quaeler
 */
public final class SearchHubView extends ViewPart {

    // oh SWT... you're lucky there's no class action suits possible against a technology
    private static final boolean IS_MAC = Platform.OS_MACOSX.equals(Platform.getOS());

    private static final boolean IS_LINUX = Platform.OS_LINUX.equals(Platform.getOS());

    private static final String URL_PREFIX = "https://hub.knime.com/search?q=";

    private static final String LINUX_LABEL_TEXT = "Search:";

    private static final String HINT_TEXT = "Search workflows, nodes, and more...";

    private static final Font HINT_FONT = JFaceResources.getFont(JFaceResources.DIALOG_FONT);

    private static final Color HINT_TEXT_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 110, 110, 110);

    private static final Color FILL_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 239, 240, 241);

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SearchHubView.class);

    private Text m_searchTextField;

    @Override
    public void createPartControl(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);
        final GridLayout gl = IS_LINUX ? new GridLayout(2, false) : new GridLayout(1, false);
        gl.marginHeight = 6;
        gl.marginWidth = 3;
        container.setLayout(gl);

        if (IS_LINUX) {
            final Label l = new Label(container, SWT.LEFT);
            l.setFont(HINT_FONT);
            l.setLayoutData(new GridData());
            l.setText(LINUX_LABEL_TEXT);
        }

        m_searchTextField = new Text(container, SWT.BORDER);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.CENTER; // to be pedantic
        gd.heightHint = 22;
        gd.horizontalIndent = IS_LINUX ? 6 : 0;
        gd.grabExcessHorizontalSpace = true;
        m_searchTextField.setLayoutData(gd);
        setBackground(m_searchTextField, FILL_COLOR);

        m_searchTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                if (ke.character == SWT.CR) {
                    final String searchTerms = m_searchTextField.getText().trim();

                    if (searchTerms.length() > 0) {
                        try {
                            Program.launch(URL_PREFIX + URLEncoder.encode(searchTerms, "UTF-8"));
                            m_searchTextField.setText("");
                        } catch (final Exception e) {
                            LOGGER.error("Problem attempting to invoke hub search: " + e);
                        }
                    }
                }
            }
        });

        /*
         * Problems abound with paint listeners and native text field widgets on some platforms; ie. this is not
         *  called, or at least called in the wrong order, when the widget has focus (so that the rendered text
         *  below is not seen.)
         */
        if (!IS_LINUX) { // it actually doesn't matter if we do this on linux as it just doesn't do anything visible
            m_searchTextField.addPaintListener((event) -> {
                if (m_searchTextField.getCharCount() == 0) {
                    final GC gc = event.gc;
                    final Rectangle size = m_searchTextField.getClientArea();

                    gc.setAdvanced(true);
                    gc.setTextAntialias(SWT.ON);
                    gc.setFont(HINT_FONT);
                    gc.setForeground(HINT_TEXT_COLOR);
                    gc.drawString(HINT_TEXT, size.x + 3, size.y + 4, true);
                }
            });
        }
    }

    @Override
    public void setFocus() {
        if ((m_searchTextField != null) && !m_searchTextField.isDisposed()) {
            m_searchTextField.setFocus();
        }
    }

    // BUG OSX will not set / repaint background if it has focus - this should go in SWTUtilities should
    //      there be use in other code for it
    private static void setBackground(final Text text, final Color color) {
        if (IS_MAC && text.isFocusControl()) {
            text.setEnabled(false);
            text.setBackground(color);
            text.setEnabled(true);
        } else {
            text.setBackground(color);
        }
    }
}
