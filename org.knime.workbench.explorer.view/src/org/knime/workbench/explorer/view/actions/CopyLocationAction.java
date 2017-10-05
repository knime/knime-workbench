/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
