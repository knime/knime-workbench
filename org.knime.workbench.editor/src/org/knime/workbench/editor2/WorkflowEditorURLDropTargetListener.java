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
 *   May 20, 2019 (hornm): created
 */
package org.knime.workbench.editor2;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.knime.core.node.NodeLogger;

/**
 * Listens to drops of URLs to the workflow editor workbench. The URLs can, e.g., be dragged and dropped from browsers.
 * From the dropped URL, e.g., a metanode template is added to the workflow (depending on what the url is refering to).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class WorkflowEditorURLDropTargetListener extends WorkflowEditorDropTargetListener<URLCreationFactory>{

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowEditorURLDropTargetListener.class);

    /**
     * @param viewer
     */
    public WorkflowEditorURLDropTargetListener(final EditPartViewer viewer) {
        super(viewer, new URLCreationFactory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handleDrop() {
        URL url = getURL(getCurrentEvent());
        if (url != null) {
            getFactory().setURL(url);
            super.handleDrop();
        }
    }

    private static URL getURL(final DropTargetEvent event) {
        if (event == null) {
            return null;
        }
        if (URLTransfer.getInstance().isSupportedType(event.currentDataType)) {
            if (event.data != null && event.data instanceof String) {
                try {
                    String[] dataLines = ((String)event.data).split(System.getProperty("line.separator")); //$NON-NLS-1$
                    return new URL(dataLines[0]);
                } catch (MalformedURLException e) {
                    LOGGER.warn("URL dropped on workbench can not be parsed", e);
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transfer getTransfer() {
        return URLTransfer.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled(final DropTargetEvent event) {
        return dropTargetIsValid(event);
    }

    /**
     * Determine whether the drop target is valid.  Subclasses may override.
     * @param event the drop target event
     * @return <code>true</code> if drop should proceed, <code>false</code> if it should not.
     */
    private static boolean dropTargetIsValid(final DropTargetEvent event) {
        if (URLTransfer.getInstance().isSupportedType(event.currentDataType) && dropTargetDataIsValid(event)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determine whether the drop target data is valid.  On some platforms this cannot be detected,
     * in which which case we return true.
     * @param event the drop target event
     * @return <code>true</code> if data is valid, (or can not be determined), <code>false</code> otherwise.
     */
    private static boolean dropTargetDataIsValid(final DropTargetEvent event) {
        if (Util.isWindows()) {
            return URLTransfer.getInstance().nativeToJava(event.currentDataType) != null;
        }
        return true;
    }
}
