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
 * Created: Jun 7, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class ExplorerViewComparator extends ViewerComparator {
    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
        if ((e1 instanceof ContentObject)
                && (e2 instanceof ContentObject)) {
            ExplorerFileStore efs1 = ((ContentObject)e1).getObject();
            ExplorerFileStore efs2 = ((ContentObject)e2).getObject();
            int cmp = rank(efs2) - rank(efs1);
            if (cmp == 0) {
                return efs1.getName().compareTo(efs2.getName());
            }
            return cmp;
        }
        // don't sort AbstractContentProviders. They have their own order
        return 0;
    }

    private int rank(final ExplorerFileStore f) {
        // we want to see workflow groups at the top
        if (ExplorerFileStore.isWorkflowGroup(f)) {
            return 4;
        }
        // then flows
        if (ExplorerFileStore.isWorkflow(f)) {
            return 3;
        }
        // then templates
        if (ExplorerFileStore.isWorkflowTemplate(f)) {
            return 2;
        }
        // down there directories
        if (f.fetchInfo().isDirectory()) {
            return 1;
        }
        // last: the files
        return 0;
    }
}
