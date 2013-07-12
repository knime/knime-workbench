/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com AG, Zurich, Switzerland
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
 * Created: Sep 6, 2011
 * Author: Peter Ohl
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class CopyLocationAction extends ExplorerAction {

    private static final ImageDescriptor IMG = KNIMEUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "/icons/path.png");

    /** ID of the global rename action in the explorer menu. */
    public static final String LOCCOPY_ACTION_ID =
            "org.knime.workbench.explorer.action.loc-url";

    private final Clipboard m_cb;

    /**
     * @param viewer the associated  view
     * @param cb clipboard to copy the path in
     */
    public CopyLocationAction(final ExplorerView viewer, final Clipboard cb) {
        super(viewer, "Local path");
        m_cb = cb;
        setToolTipText("Copy local abstract path to clipboard");
        setImageDescriptor(IMG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return LOCCOPY_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        IStructuredSelection sel = getSelection();
        Iterator i = sel.iterator();
        while (i.hasNext()) {
            AbstractExplorerFileStore fs =
                    DragAndDropUtils.getFileStore(i.next());
            if (!(fs instanceof LocalExplorerFileStore)) {
                return false;
            }
        }
        return true;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        IStructuredSelection sel = getSelection();
        Iterator i = sel.iterator();
        StringBuilder url = new StringBuilder();
        while (i.hasNext()) {
            AbstractExplorerFileStore fs =
                    DragAndDropUtils.getFileStore(i.next());
            if (!(fs instanceof LocalExplorerFileStore)) {
                continue;
            }
            File loc;
            try {
                loc = fs.toLocalFile();
                if (loc != null) {
                    if (url.length() > 0) {
                        url.append("\n");
                    }
                    url.append(loc.getAbsolutePath());
                }
            } catch (CoreException e) {
                // don't add it then
            }

        }
        TextTransfer textTransfer = TextTransfer.getInstance();
        m_cb.setContents(new Object[]{url.toString()},
                new Transfer[]{textTransfer});
    }

}
