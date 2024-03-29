/*
 * ------------------------------------------------------------------------
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
 * Created: Jun 7, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 *
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class ExplorerViewComparator extends ViewerComparator {
    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
        if ((e1 instanceof ContentObject)
                && (e2 instanceof ContentObject)) {
            AbstractExplorerFileStore efs1 = ((ContentObject)e1).getObject();
            AbstractExplorerFileStore efs2 = ((ContentObject)e2).getObject();
            int cmp = rank(efs2) - rank(efs1);
            if (cmp == 0) {
                return efs1.getName().toLowerCase().compareTo(
                        efs2.getName().toLowerCase());
            }
            return cmp;
        }
        // don't sort AbstractContentProviders. They have their own order
        return 0;
    }

    private int rank(final AbstractExplorerFileStore f) {
        // we want to see message at the top
        if (AbstractExplorerFileStore.isMessage(f)) {
            return 8;
        }
        if (f instanceof RemoteExplorerFileStore) {
            // For Hub we want to show teams first then users.
            final var info = ((RemoteExplorerFileStore)f).fetchInfo();
            if (info.isTeamRoot()) {
                return 7;
            } else if (info.isUserRoot()) {
                return 6;
            }
        }
        // then workflow groups
        if (AbstractExplorerFileStore.isWorkflowGroup(f)) {
            return 5;
        }
        // then flows
        if (AbstractExplorerFileStore.isWorkflow(f)) {
            return 4;
        }
        // then templates
        if (AbstractExplorerFileStore.isWorkflowTemplate(f)) {
            return 3;
        }
        // down there directories
        if (f.fetchInfo().isDirectory()) {
            return 2;
        }
        // last: the files
        return 1;
    }
}
