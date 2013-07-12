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

import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.osgi.framework.FrameworkUtil;

/**
 * Action for copying the mount-point relative URL of an item in the explorer tree to the clipboard.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class CopyURLAction extends ExplorerAction {
    /** ID of the global rename action in the explorer menu. */
    public static final String URLCOPY_ACTION_ID =
            "org.knime.workbench.explorer.action.copy-url";

    private final Clipboard m_cb;

    /**
     * Creates a new action.
     *
     * @param viewer the associated tree viewer
     * @param cb the clipboard into which the URL is copied
     */
    public CopyURLAction(final ExplorerView viewer, final Clipboard cb) {
        super(viewer, "Absolute URL");
        m_cb = cb;
        setToolTipText("Copy absolute URL to clipboard");
        setImageDescriptor(ImageRepository.getImageDescriptor(FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                "/icons/url.png"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return URLCOPY_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        IStructuredSelection sel = getSelection();
        Iterator<?> i = sel.iterator();
        StringBuilder url = new StringBuilder();
        while (i.hasNext()) {
            AbstractExplorerFileStore fs = DragAndDropUtils.getFileStore(i.next());
            if (url.length() > 0) {
                url.append("\n");
            }
            if (fs != null) {
                url.append(fs.toURI().toString());
            } else {
                url.append("<null>");
            }
        }
        TextTransfer textTransfer = TextTransfer.getInstance();
        m_cb.setContents(new Object[]{url.toString()},
                new Transfer[]{textTransfer});
    }
}
