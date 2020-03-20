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
 *   Mar 9, 2020 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowInPortEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortBarEditPart;
import org.knime.workbench.editor2.editparts.WorkflowOutPortEditPart;

/**
 * The genesis for this class is https://knime-com.atlassian.net/browse/AP-13833
 *
 * @author loki der quaeler
 */
class ConnectionHighlighter implements ISelectionListener {
    private final HashSet<ConnectionContainerEditPart> m_currentlyHighlightedConnections;
    private final WorkflowEditor m_workflowEditor;

    ConnectionHighlighter(final WorkflowEditor we) {
        m_currentlyHighlightedConnections = new HashSet<>();
        m_workflowEditor = we;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        if (m_workflowEditor.equals(part)) {
            final HashSet<ConnectionContainerEditPart> connections = new HashSet<>();

            m_currentlyHighlightedConnections.stream().forEach(connection -> {
                connection.setHighlighted(false);
            });
            m_currentlyHighlightedConnections.clear();

            if (!vetoSelection(selection)) {
                final Iterator<?> it = ((IStructuredSelection)selection).iterator();
                while (it.hasNext()) {
                    final Object o = it.next();
                    if (o instanceof NodeContainerEditPart) {
                        final NodeContainerEditPart ncep = (NodeContainerEditPart)o;
                        final ConnectionContainerEditPart[] outbounds = ncep.getOutgoingConnections();
                        Arrays.stream(outbounds).forEach(outbound -> {
                            outbound.setHighlighted(true);
                            connections.add(outbound);
                        });
                        final ConnectionContainerEditPart[] inbounds = ncep.getIncomingConnections();
                        Arrays.stream(inbounds).forEach(inbound -> {
                            inbound.setHighlighted(true);
                            connections.add(inbound);
                        });
                    } else if (o instanceof WorkflowInPortBarEditPart) {
                        final WorkflowInPortBarEditPart barEP = (WorkflowInPortBarEditPart)o;
                        for (final Object child : barEP.getChildren()) {
                            if (child instanceof WorkflowInPortEditPart) {
                                final WorkflowInPortEditPart portEP = (WorkflowInPortEditPart)child;
                                for (final Object portConnection : portEP.getSourceConnections()) {
                                    if (portConnection instanceof ConnectionContainerEditPart) {
                                        final ConnectionContainerEditPart ccep
                                                = (ConnectionContainerEditPart)portConnection;
                                        ccep.setHighlighted(true);
                                        connections.add(ccep);
                                    }
                                }
                            }
                        }
                    } else if (o instanceof WorkflowOutPortBarEditPart) {
                        final WorkflowOutPortBarEditPart barEP = (WorkflowOutPortBarEditPart)o;
                        for (final Object child : barEP.getChildren()) {
                            if (child instanceof WorkflowOutPortEditPart) {
                                final WorkflowOutPortEditPart portEP = (WorkflowOutPortEditPart)child;
                                for (final Object portConnection : portEP.getTargetConnections()) {
                                    if (portConnection instanceof ConnectionContainerEditPart) {
                                        final ConnectionContainerEditPart ccep
                                                                = (ConnectionContainerEditPart)portConnection;
                                        ccep.setHighlighted(true);
                                        connections.add(ccep);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            m_currentlyHighlightedConnections.addAll(connections);
        }
    }

    // Per request, we highlight connections iff one node is selected (though we can't check for selection size == 1
    //      since the selection may also contain connection lines and other future non-node stuff.)
    private static boolean vetoSelection(final ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection iss = (IStructuredSelection)selection;
            int connnectionPossessorCount = 0;
            final Iterator<?> it = iss.iterator();
            while (it.hasNext()) {
                final Object o = it.next();
                if ((o instanceof NodeContainerEditPart)
                        || (o instanceof WorkflowInPortBarEditPart)
                        || (o instanceof WorkflowOutPortBarEditPart)) {
                    if (connnectionPossessorCount > 0) {
                        return true;
                    }
                    connnectionPossessorCount++;
                }
            }

            return false;
        }

        return true;
    }
}
