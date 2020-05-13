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
 *   Apr 14, 2020 (loki): created
 */
package org.knime.chromium.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * As part of https://knime-com.atlassian.net/browse/AP-13382 we have this view as sanity check to make sure
 *  the Equo / Make Technology Chromium CEF bundles are working correctly; this requires the same-issue-branch
 *  of knime-sdk-setup (which cites the appropriate p2 sites in the KNIME-AP-complete-internal target.
 *
 * @author loki der quaeler
 */
public class HubView extends ViewPart {
    private Browser m_browser;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        m_browser = new Browser(parent, SWT.NONE);
        m_browser.setUrl("https://hub.knime.com/");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        if (m_browser != null) {
            m_browser.dispose();
        }

        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() { }
}
