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
 * ------------------------------------------------------------------------
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
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
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
        setImageDescriptor(ImageRepository.getIconDescriptor(FrameworkUtil.getBundle(getClass()).getSymbolicName(), "/icons/url.png"));
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
