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
 * -------------------------------------------------------------------
 *
 * History
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import java.util.Optional;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.ui.node.workflow.WorkflowCopyUI;
import org.knime.core.ui.wrapper.WorkflowDefWrapper;
import org.knime.shared.workflow.storage.clipboard.SystemClipboardFormat;
import org.knime.shared.workflow.storage.clipboard.SystemClipboardFormat.ObfuscatorException;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.async.AsyncUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Implements the clipboard copy action to copy nodes and connections into the
 * clipboard.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class CopyAction extends AbstractClipboardAction {

    private NodeContainerEditPart[] m_nodeParts;

    private AnnotationEditPart[] m_annotationParts;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CopyAction.class);

    /**
     * Constructs a new clipboard copy action.
     *
     * @param editor the workflow editor this action is intended for
     */
    public CopyAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ActionFactory.COPY.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        ISharedImages sharedImages =
            PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Copy";
    }

    /**
     * At least one node must be selected.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] nodeParts = getSelectedParts(NodeContainerEditPart.class);
        AnnotationEditPart[] annoParts = getSelectedParts(AnnotationEditPart.class);
        WorkflowAnnotationID[] annos = AnnotationUtilities.extractWorkflowAnnotationIDs(annoParts);
        return (nodeParts.length > 0) || (annos.length > 0);
    }

    /** {@inheritDoc} */
    @Override
    public void runInSWT() {
        m_nodeParts = getSelectedParts(NodeContainerEditPart.class);
        m_annotationParts = getSelectedParts(AnnotationEditPart.class);

        NodeID[] ids = new NodeID[m_nodeParts.length];
        for (int i = 0; i < m_nodeParts.length; i++) {
            NodeContainerEditPart nodeEP = m_nodeParts[i];
            ids[i] = nodeEP.getNodeContainer().getID();
        }
        WorkflowAnnotationID[] annotationIDs = AnnotationUtilities.extractWorkflowAnnotationIDs(m_annotationParts);

        WorkflowCopyContent.Builder content = WorkflowCopyContent.builder();
        content.setNodeIDs(ids);
        content.setAnnotationIDs(annotationIDs);
        WorkflowCopyUI wfCopy = AsyncUtil.wfmAsyncSwitch(//
            syncWfmUI -> syncWfmUI.copy(content.build()),// calls copyToDef
            asyncWfmUI -> asyncWfmUI.copyAsync(content.build()), //
            super.getManagerUI(), "Copying workflow parts ...");

        if (wfCopy instanceof WorkflowDefWrapper) {
            var defClipboardContent = ((WorkflowDefWrapper)wfCopy).unwrap();
            try {
                // obfuscated string that protects for instance locked metanode/component contents
                var systemClipboardContent = SystemClipboardFormat.serialize(defClipboardContent);
                copyToSystemClipboard(systemClipboardContent);
                // null legacy clipboard in order for it not to take precedence with now outdated content
                getEditor().setClipboardContent(null);
            } catch (JsonProcessingException | ObfuscatorException e) {
                LOGGER.error("Cannot copy to system clipboard: ", e);
            }
        } else {
            // TODO use eclipse clipboard for copy & paste from remote to remote
            // the information about the nodes is stored in the config XML format
            // also used to store workflow information in the kflow files
            getEditor().setClipboardContent(new ClipboardObject(wfCopy));
        }

        // update the actions
        getEditor().updateActions();

        // Give focus to the editor again. Otherwise the actions (selection)
        // is not updated correctly.
        getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
    }

    /** Write (non-null) argument string to system clipboard. */
    private static void copyToSystemClipboard(final String s) {
        Display display = PlatformUI.getWorkbench().getDisplay();
        Clipboard cb = new Clipboard(display);
        try {
            cb.setContents(new Object[] {s}, new Transfer[] {TextTransfer.getInstance()});
        } finally {
            cb.dispose();
        }
    }

    /** Read text from system clipboard. */
    static Optional<String> readFromSystemClipboard() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        Clipboard cb = new Clipboard(display);
        try {
            Object contents = cb.getContents(TextTransfer.getInstance());
            if (contents instanceof String) {
                return Optional.of((String)contents);
            } else {
                return Optional.empty();
            }
        } finally {
            cb.dispose();
        }
    }

    /** @return the annotationParts */
    public AnnotationEditPart[] getAnnotationParts() {
        return m_annotationParts;
    }

    /** @return the nodeParts */
    public NodeContainerEditPart[] getNodeParts() {
        return m_nodeParts;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new IllegalStateException(
                "Not to be called as runInSWT is overwritten.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleWorkflowManagerUI() {
        return true;
    }

}
