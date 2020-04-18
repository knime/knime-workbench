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
 *   Apr 18, 2020 (loki): created
 */
package org.knime.workbench.editor2.actions.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.ui.SyncExecQueueDispatcher;

/**
 * This embodies the results of the user's find operation and provides additional functionality to keep track of the
 * last displayed result (and, so therefore, be able to fetch the next result in a ring fashion.) It also keeps the
 * search criteria and applies it to new nodes added to the workflow for possible inclusion in the results.
 *
 * See notes in {@link FindNodePopOver} class javadocs concerning future use of an alternate data structure for
 * searches.
 *
 * @author loki der quaeler
 */
public class FindResults implements WorkflowListener {


    private final ArrayList<NodeContainer> m_resultsList;
    // kept around for faster determination as to whether we should invoke remove() on the List... one might argue
    //      that condensing these two as a LinkedHashSet, and then keeping around an iterator that gets re-created
    //      on adds and removals (as well as traversed to the correct position on recreation) would be a better
    //      approach. The somewhat more convoluted logic in that approach probably is not worth the space savings
    //      as there will only ever be at most one instance of this class per workflow editor.
    private final HashSet<NodeContainer> m_resultsHash;
    private int m_currentResultIndex;
    private final String m_searchText;
    private final boolean m_searchById;

    private WorkflowEditor m_owningEditor;

    FindResults(final List<NodeContainer> results, final int currentIndex, final String searchText) {
        m_resultsList = new ArrayList<>(results);
        m_resultsHash = new HashSet<>(results);
        m_currentResultIndex = currentIndex;

        m_searchText = searchText.toLowerCase();
        m_searchById = FindNodePopOver.NODE_ID_PATTERN.matcher(m_searchText).find();
    }

    /**
     * @return the next search result, where 'next' is the following item in the results list, or the first one if the
     *         previously viewed result was the last in the results list
     */
    public NodeContainer getNextResult() {
        synchronized(m_resultsList) {
            m_currentResultIndex++;
            if (m_currentResultIndex >= m_resultsList.size()) {
                m_currentResultIndex = 0;
            }

            return m_resultsList.get(m_currentResultIndex);
        }
    }

    /**
     * It is expected that this method will be invoked once per the lifetime of an instance.
     *
     * @param editor
     */
    public void setWorkflowEditor(final WorkflowEditor editor) {
        m_owningEditor = editor;
        m_owningEditor.getWorkflowManager().get().addListener(this);
    }

    /**
     * This method should be invoked when the results are being thrown away in order to ensure the instance deregisters
     * itself as a workflow listener.
     */
    public void dispose() {
        m_owningEditor.getWorkflowManager().get().removeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        SyncExecQueueDispatcher.asyncExec(() -> {
            if (m_owningEditor.isClosed()) {
                return;
            }

            switch (event.getType()) {
                case NODE_REMOVED:
                    final NodeContainer removed = (NodeContainer)event.getOldValue();

                    synchronized(m_resultsList) {
                        if (m_resultsHash.remove(removed)) {
                            m_resultsList.remove(removed);
                        }
                    }

                    break;
                case NODE_ADDED:
                    final NodeContainer added = (NodeContainer)event.getNewValue();
                    final boolean shouldAdd;
                    if (m_searchById) {
                        shouldAdd = added.getID().toString().contains(m_searchText);
                    } else {
                        final FindNodePopOver.ProcessedNodeAttributes processed
                                                        = new FindNodePopOver.ProcessedNodeAttributes(added);
                        shouldAdd = processed.getSearchText().contains(m_searchText);
                    }

                    if (shouldAdd) {
                        synchronized(m_resultsList) {
                            m_resultsHash.add(added);
                            m_resultsList.add(added);
                        }
                    }

                    break;
                default:
                    // NOOP
            }
        });
    }
}
