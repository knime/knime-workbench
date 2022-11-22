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
 *   Mar 14, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.ui;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.knime.core.ui.workflowcoach.NodeRecommendationManager.NodeRecommendation;

/**
 * Label provider for the table of recommended nodes in the {@link WorkflowCoachView}. It provides the node's icon, the
 * node name and the frequency of occurrence in the node recommendation statistics.
 *
 * @author Martin Horn, University of Konstanz
 */
public class WorkflowCoachLabelProvider extends LabelProvider implements ITableLabelProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        if (element instanceof NodeRecommendation[] && columnIndex == 0) {
            NodeRecommendation[] nps = (NodeRecommendation[])element;
            return WorkflowCoachView.getNodeTemplateFromNodeRecommendations(nps).getIcon();
        }
        return super.getImage(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        if (element instanceof NodeRecommendation[]) {
            NodeRecommendation[] nrs = (NodeRecommendation[])element;
            if (columnIndex == 0) {
                return WorkflowCoachView.getNodeTemplateFromNodeRecommendations(nrs).getName();
            } else {
                int idx = columnIndex - 1;
                if (idx >= nrs.length) {
                    return "<missing>";
                } else if (nrs[idx] != null) {
                    double perc = (nrs[idx].getFrequency() / (double)nrs[idx].getTotalFrequency() * 100);
                    if (perc < 1.0) {
                        return "<1%";
                    } else {
                        return ((int)Math.round(perc)) + "%";
                    }
                } else {
                    return "";
                }
            }
        } else if (element instanceof String && columnIndex == 0) {
            return (String)element;
        }
        return null;
    }

}
