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
 *   Apr 26, 2019 (moritz): created
 */
package org.knime.workbench.explorer.view;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.knime.workbench.explorer.view.actions.ExplorerAction;

/**
 * Menu manager that allows to exclude actions derived by other plugins. This is usually the case when the menu is
 * opened on a selection of the KNIME Hub.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 * @since 8.4
 */
public class KNIMEMenuManager extends MenuManager {

    private boolean m_allowExternalActions;

    /**
     * Creates a new {@link MenuManager}.
     *
     * @see MenuManager#MenuManager(String)
     */
    public KNIMEMenuManager(final String name) {
        super(name);
    }

    /**
     * Sets if external actions are allowed.
     *
     * @param allow <code>true</code> if adding actions shall be allowed, <code>false</code> otherwise.
     */
    public void setAllowAddingActions(final boolean allow) {
        m_allowExternalActions = allow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final IAction action) {
        if (m_allowExternalActions) {
            super.add(action);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final IContributionItem item) {
        if (m_allowExternalActions) {
            super.add(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertAfter(final String ID, final IAction action) {
        if (m_allowExternalActions) {
            super.insertAfter(ID, action);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertAfter(final String ID, final IContributionItem item) {
        if (m_allowExternalActions) {
            super.insertAfter(ID, item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertBefore(final String ID, final IAction action) {
        if (m_allowExternalActions || action instanceof ExplorerAction) {
            super.insertBefore(ID, action);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertBefore(final String ID, final IContributionItem item) {
        if (m_allowExternalActions) {
            super.insertBefore(ID, item);
        }
    }
}
