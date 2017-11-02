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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.net.URI;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerFileStoreTransfer;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;

public class CutCopyToClipboardAction extends SelectionListenerAction {
//    private final static NodeLogger LOGGER = NodeLogger.getLogger(
//            CutCopyToClipboardAction.class);
    /** The id of this action. */
    public final String ID;

    private final boolean m_performCut;
    private final ExplorerView m_view;
    private PasteFromClipboardAction m_pasteAction;




    /**
     * @param view the associated explorer view
     * @param menuText the menu text of the action
     * @param performCut set to true if a cut operation shall be performed, or
     *      to false for a copy operation
     */
    public CutCopyToClipboardAction(final ExplorerView view,
            final String menuText, final boolean performCut) {
        super(menuText);
        m_view = view;
        m_performCut = performCut;
        ID = "org.knime.workbench.explorer.view.actions."
            + (performCut ? "CutToClipboard" : "CopyToClipboard");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * @param pasteAction the paste action to notify of changes
     */
    public void setPasteAction(final PasteFromClipboardAction pasteAction) {
        m_pasteAction = pasteAction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<AbstractExplorerFileStore> selection
                = ExplorerAction.removeSelectedChildren(
                      DragAndDropUtils.getExplorerFileStores(
                      (IStructuredSelection)m_view.getViewer().getSelection()));
        URI[] fileURI = new URI[selection.size()];
        int n = 0;
        StringBuffer sb = new StringBuffer();
        for (AbstractExplorerFileStore fs : selection) {
            URI uri = fs.toURI();
            fileURI[n++] = uri;
            sb.append(uri.toString());
            sb.append("\n");
        }

        ExplorerFileStoreTransfer transfer
                = ExplorerFileStoreTransfer.getInstance();
        transfer.setCut(m_performCut);
        m_view.getClipboard().setContents(new Object[] {fileURI, sb.toString()},
                new Transfer[] {transfer, TextTransfer.getInstance()});
        if (m_pasteAction != null) {
            m_pasteAction.setEnabled(m_pasteAction.isEnabled());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateSelection(final IStructuredSelection selection) {
        boolean enabled = AbstractCopyMoveAction.isCopyOrMovePossible(
                DragAndDropUtils.getProviderMap(selection), m_performCut);
        setEnabled(enabled);
        return enabled;
    }
}
