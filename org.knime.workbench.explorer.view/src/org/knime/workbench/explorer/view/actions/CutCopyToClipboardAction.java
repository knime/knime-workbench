/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright by 
  * KNIME.com, Zurich, Switzerland
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
  * History
  *   Jan 20, 2012 (morent): created
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

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
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
