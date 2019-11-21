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
 *   Nov 26, 2019 (loki): created
 */
package org.knime.workbench.editor2.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.DisplayableNodeType;

/**
 * The genesis for this dialog is https://knime-com.atlassian.net/browse/AP-6904
 *
 * Note, for 4.1.0 we will only search the currently open (currently focused editor) worfklow - no children
 *  nor parents. (This is not completely true, technically - we have provided an easter egg for Iris.)
 *
 * Future optimizations:
 *          - move to using multiple Tries for faster search results
 *          - potentially cache a "global" list and listen for node additions and deletions to update the cache
 *
 * @author loki der quaeler
 */
public class FindNodePopOver extends PopupDialog {
    private static final int MAX_CHARACTER_LENGTH_FOR_NODE_ANNOTATION_IN_LABEL = 33;
    private static final Point POP_OVER_SIZE = new Point(500, 375);

    private static final Pattern NODE_ID_PATTERN = Pattern.compile("^[\\d]{1,2}:?");
    private static final Pattern EASTER_EGG_NODE_ID_PATTERN = Pattern.compile("^[\\d]{1,2}:[\\d]{1,8}");

    private static final Color TREE_CELL_BACKGROUND = new Color(PlatformUI.getWorkbench().getDisplay(), 246, 250, 252);

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FindNodePopOver.class);


    private Text m_searchField;
    private TreeViewer m_nodeTreeViewer;

    private final WorkflowManager m_workflowManager;
    private final WorkflowEditor m_workflowEditor;

    private final ArrayList<NodeContainerDisplayWrapper> m_fullContainerList;

    /**
     * @param workflowEditor
     */
    public FindNodePopOver(final WorkflowEditor workflowEditor) {
        super(PlatformUI.getWorkbench().getDisplay().getActiveShell(), SWT.NO_TRIM, true, false, false, false, false, null, null);

        m_workflowEditor = workflowEditor;
        final Optional<WorkflowManager> workflowManager = workflowEditor.getWorkflowManager();
        if (!workflowManager.isPresent()) {
            throw new IllegalStateException("Could not get the workflow manager from the supplied editor.");
        }
        m_workflowManager = workflowManager.get();

        m_fullContainerList = produceSortedNodeList();
    }

    @Override
    public boolean close() {
        final boolean result = super.close();

        m_fullContainerList.stream().forEach(wrapper -> wrapper.getDisplayImage().dispose());

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point getDefaultSize() {
        return POP_OVER_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getBackground() {
        return TREE_CELL_BACKGROUND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control getFocusControl() {
        return m_searchField;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite content = (Composite)super.createDialogArea(parent);

        m_searchField = new Text(content, (SWT.SINGLE | SWT.BORDER | SWT.BORDER_SOLID));
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_searchField.setLayoutData(gd);
        m_searchField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent e) { }

            @Override
            public void keyReleased(final KeyEvent e) {
                if (e.keyCode == SWT.ESC) {
                    close();

                    return;
                } else if (e.character == '\n' || e.character == '\r') {
                    if (m_nodeTreeViewer.getTree().getSelectionCount() == 0) {
                        final String text = m_searchField.getText();
                        final Matcher m = EASTER_EGG_NODE_ID_PATTERN.matcher(text);
                        if (m.find()) {
                            // Iris' easter egg
                            searchRobustlyForNode(text);
                        }
                    } else {
                        final IStructuredSelection iss = (IStructuredSelection)m_nodeTreeViewer.getSelection();

                        handleNodeSelection((NodeContainerDisplayWrapper)iss.getFirstElement());
                    }

                    return;
                } else if (e.keyCode == SWT.ARROW_DOWN) {
                    if (m_nodeTreeViewer.getTree().getSelectionCount() == 0) {
                        return;
                    }

                    adjustTreeSelectionBy(1);

                    return;
                } else if (e.keyCode == SWT.ARROW_UP) {
                    if (m_nodeTreeViewer.getTree().getSelectionCount() == 0) {
                        return;
                    }

                    adjustTreeSelectionBy(-1);

                    return;
                }

                final String text = m_searchField.getText();

                final ArrayList<NodeContainerDisplayWrapper> searchResults;
                if (text.trim().length() == 0) {
                    searchResults = m_fullContainerList;
                } else {
                    searchResults = new ArrayList<>();

                    // see class javadoc to-do notes concerning Tries
                    final Matcher m = NODE_ID_PATTERN.matcher(text);
                    final boolean searchNodeIds = m.find();
                    if (searchNodeIds) {
                        for (final NodeContainerDisplayWrapper displayWrapper : m_fullContainerList) {
                            if (displayWrapper.getNodeContainer().getID().toString().contains(text)) {
                                searchResults.add(displayWrapper);
                            }
                        }
                    } else {
                        final String lcText = text.toLowerCase();
                        for (final NodeContainerDisplayWrapper displayWrapper : m_fullContainerList) {
                            if (displayWrapper.getSearchableText().contains(lcText)) {
                                searchResults.add(displayWrapper);
                            }
                        }
                    }
                }

                m_nodeTreeViewer.setInput(searchResults);
                if (searchResults.size() > 0) {
                    m_nodeTreeViewer.getTree().select(m_nodeTreeViewer.getTree().getItem(0));
                }
            }
        });

        m_nodeTreeViewer = new TreeViewer(content, (SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER));
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        final Tree tree = m_nodeTreeViewer.getTree();
        tree.setLayoutData(gd);
        tree.addMouseListener(new MouseListener() {
            @Override
            public void mouseDoubleClick(final MouseEvent e) {
                final IStructuredSelection iss = (IStructuredSelection)m_nodeTreeViewer.getSelection();

                handleNodeSelection((NodeContainerDisplayWrapper)iss.getFirstElement());
            }

            @Override
            public void mouseDown(final MouseEvent e) { }

            @Override
            public void mouseUp(final MouseEvent e) { }
        });
        m_nodeTreeViewer.setLabelProvider(new NodeLabelProvider());
        m_nodeTreeViewer.setContentProvider(new NodeTreeContentProvider());
        m_nodeTreeViewer.setInput(m_fullContainerList);
        tree.select(tree.getItem(0));
        tree.setBackground(TREE_CELL_BACKGROUND);

        return content;
    }

    private void adjustTreeSelectionBy(final int delta) {
        final Tree tree = m_nodeTreeViewer.getTree();
        final int currentIndex = tree.indexOf(tree.getSelection()[0]);
        final int finalIndex = (currentIndex + delta);

        if ((finalIndex >= tree.getItemCount()) || (finalIndex < 0)) {
            return;
        }

        tree.setSelection(tree.getItem(finalIndex));
    }

    private final ArrayList<NodeContainerDisplayWrapper> produceSortedNodeList() {
        final ArrayList<NodeContainerDisplayWrapper> nodeList = new ArrayList<>();
        final WorkflowManagerUI wmUI = m_workflowEditor.getWorkflowManagerUI();
        final Display d = PlatformUI.getWorkbench().getDisplay();

        wmUI.getNodeContainers().stream().forEach(ncUI -> nodeList.add(new NodeContainerDisplayWrapper(ncUI, d)));

        Collections.sort(nodeList, new Comparator<NodeContainerDisplayWrapper>() {
            @Override
            public int compare(final NodeContainerDisplayWrapper dw1, final NodeContainerDisplayWrapper dw2) {
                final String s1 = dw1.getNodeContainer().getNameWithID();
                final String s2 = dw2.getNodeContainer().getNameWithID();
                return s1.compareTo(s2);
            }
        });

        return nodeList;
    }

    // This method is incredibly brief currently - but will not be so in the future where we support more
    //      robust search scenarios.
    private void handleNodeSelection(final NodeContainerDisplayWrapper displayWrapper) {
        m_workflowEditor.setNodeSelection(displayWrapper.getNodeContainer());

        close();
    }

    // Iris' easter egg
    private void searchRobustlyForNode(final String id) {
        final WorkflowManager projectWM = m_workflowManager.getProjectWFM();

        NodeContainer nc;
        try {
            nc = projectWM.findNodeContainer(NodeID.fromString(id));
        } catch (final Exception e) {
            LOGGER.debug("Unable to find node for id " + id, e);
            nc = null;
        }

        if (nc == null) {
            MessageDialog.openWarning(SWTUtilities.getKNIMEWorkbenchShell(), "Not Found",
                "We could find no node with the id, " + id);
        } else {
            final WorkflowManager parent = nc.getParent();
            final IWorkbenchPage iwp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

            WorkflowEditor we = null;
            if (parent.equals(projectWM)) {
                final IEditorReference[] editorReferences = iwp.getEditorReferences();

                for (final IEditorReference reference : editorReferences) {
                    final IEditorPart iep = reference.getEditor(false);

                    if (iep instanceof WorkflowEditor) {
                        final Optional<WorkflowManager> wmOpt = ((WorkflowEditor)iep).getWorkflowManager();

                        if (wmOpt.isPresent() && wmOpt.get().equals(parent)) {
                            we = ((WorkflowEditor)iep);
                            break;
                        }
                    }
                }

                if (we != null) {
                    iwp.activate(we);
                } else {
                    MessageDialog.openInformation(SWTUtilities.getKNIMEWorkbenchShell(), "Search Problem...",
                        "We were able to find the node, but not its editor which should already be open.");
                }
            } else {
                final WorkflowManagerUI wmUI = WorkflowManagerWrapper.wrap(parent);
                try {
                    // TODO this may be an incorrect assignation of the 'parent'
                    final WorkflowEditor parentEditor = (WorkflowEditor)iwp.getActiveEditor();
                    final WorkflowManagerInput input = new WorkflowManagerInput(wmUI, parentEditor);

                    we = (WorkflowEditor)iwp.openEditor(input, WorkflowEditor.ID);
                } catch (PartInitException e) {
                    LOGGER.error("Error while opening new editor", e);
                }
            }

            if (we != null) {
                final NodeContainer nodeContainer = nc;
                final WorkflowEditor workflowEditor = we;
                final Runnable r = () -> {
                    try {
                        Thread.sleep(300);
                    } catch (final Exception e) {
                    }

                    workflowEditor.setNodeSelection(nodeContainer);
                };

                KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(r);
            }
        }
    }


    private static class NodeLabelProvider extends StyledCellLabelProvider {
        private static final Color NAME_FOREGROUND;
        private static final Font NAME_FONT;
        private static final Color LOWER_TEXT_FOREGROUND;
        private static final Font LOWER_TEXT_FONT;
        private static final Color ID_FOREGROUND;
        private static final Font ID_FONT;

        static {
            final Display d = PlatformUI.getWorkbench().getDisplay();
            final Font boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
            final FontData[] boldFD = boldFont.getFontData();
            final Font nonBoldFont = JFaceResources.getFont(JFaceResources.DIALOG_FONT);
            final FontData[] normalFD = nonBoldFont.getFontData();

            NAME_FOREGROUND = new Color(d, 25, 44, 65);
            NAME_FONT = boldFont;

            LOWER_TEXT_FOREGROUND = new Color(d, 31, 46, 56);
            LOWER_TEXT_FONT = new Font(d, normalFD[0].getName(), normalFD[0].getHeight() - 2, normalFD[0].getStyle());

            ID_FOREGROUND = new Color(d, 70, 55, 23);
            ID_FONT = new Font(d, boldFD[0].getName(), boldFD[0].getHeight() - 3, boldFD[0].getStyle());
        }


        @Override
        public void update(final ViewerCell cell) {
            final NodeContainerDisplayWrapper displayWrapper = (NodeContainerDisplayWrapper)cell.getElement();

            cell.setText(displayWrapper.getDisplayText());
            cell.setImage(displayWrapper.getDisplayImage());
            cell.setBackground(TREE_CELL_BACKGROUND);

            final StyleRange[] sr = new StyleRange[displayWrapper.shouldRenderLowerText() ? 3 : 2];
            final int idIndex = displayWrapper.shouldRenderLowerText() ? 2 : 1;
            int start = 0;
            sr[0] = new StyleRange(start, displayWrapper.getRangeIndices()[0], NAME_FOREGROUND, TREE_CELL_BACKGROUND);
            sr[0].font = NAME_FONT;
            start += displayWrapper.getRangeIndices()[0];
            if (displayWrapper.shouldRenderLowerText()) {
                sr[1] = new StyleRange(start, displayWrapper.getRangeIndices()[1], LOWER_TEXT_FOREGROUND,
                    TREE_CELL_BACKGROUND);
                sr[1].font = LOWER_TEXT_FONT;
                start += displayWrapper.getRangeIndices()[1];
            }
            sr[idIndex] =
                new StyleRange(start, displayWrapper.getRangeIndices()[idIndex], ID_FOREGROUND, TREE_CELL_BACKGROUND);
            sr[idIndex].font = ID_FONT;
            cell.setStyleRanges(sr);

            // calls 'repaint' to trigger the paint listener
            super.update(cell);
        }
    }


    private static class NodeTreeContentProvider implements ITreeContentProvider {
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")  // generics casting
        @Override
        public Object[] getElements(final Object inputElement) {
            final ArrayList<NodeContainerDisplayWrapper> list = (ArrayList<NodeContainerDisplayWrapper>)inputElement;

            return list.toArray(new NodeContainerDisplayWrapper[list.size()]);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object[] getChildren(final Object parentElement) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getParent(final Object element) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasChildren(final Object element) {
            return false;
        }
    }


    private static class NodeContainerDisplayWrapper {
        private final NodeContainer m_nodeContainer;

        private final String m_displayText;
        private final String m_searchText;

        private final int[] m_rangeIndices;

        private final boolean m_renderLowerText;

        private final Image m_displayImage;

        private NodeContainerDisplayWrapper(final NodeContainerUI ncUI, final Display display) {
            m_nodeContainer = Wrapper.unwrapNC(ncUI);

            final NodeAnnotation na = m_nodeContainer.getNodeAnnotation();
            final String name = m_nodeContainer.getName();
            final String nid = m_nodeContainer.getID().toString();
            final String fullAnnotationText
                = na.getText().trim().replace("\n\r", " ").replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');


            m_renderLowerText = (fullAnnotationText.length() != 0);
            m_rangeIndices = new int[m_renderLowerText ? 3 : 2];
            m_rangeIndices[0] = name.length();
            m_rangeIndices[m_renderLowerText ? 2 : 1] = nid.length() + 3;
            if (!m_renderLowerText) {
                m_displayText = name + " (" + nid + ")";
                m_searchText = m_displayText.toLowerCase();
            } else {
                final String displayAnnotationText;
                if (fullAnnotationText.length() > MAX_CHARACTER_LENGTH_FOR_NODE_ANNOTATION_IN_LABEL) {
                    displayAnnotationText =
                        fullAnnotationText.substring(0, MAX_CHARACTER_LENGTH_FOR_NODE_ANNOTATION_IN_LABEL) + "...";
                } else {
                    displayAnnotationText = fullAnnotationText;
                }
                m_displayText = name + " " + displayAnnotationText + " (" + nid + ")";
                final String searchText = name + " " + fullAnnotationText + " (" + nid + ")";
                m_searchText = searchText.toLowerCase();
                m_rangeIndices[1] = displayAnnotationText.length() + 1;
            }

            final DisplayableNodeType dnt =
                    DisplayableNodeType.getTypeForNodeType(m_nodeContainer.getType(), (ncUI instanceof SubNodeContainerUI));
            final Image backgroundImage = dnt.getImage();
            final Rectangle backgroundBounds = backgroundImage.getBounds();

            m_displayImage = new Image(display, backgroundBounds.width, backgroundBounds.height);
            final GC gc = new GC(m_displayImage);

            gc.setAntialias(SWT.ON);
            gc.setInterpolation(SWT.HIGH);
            gc.drawImage(backgroundImage, 0, 0);

            final Image nodeIcon = NodeContainerEditPart.getIconImageForNodeContainer(ncUI);
            if (nodeIcon != null) {
                final Rectangle iconBounds = nodeIcon.getBounds();
                final int x = (backgroundBounds.width - iconBounds.width) / 2;
                final int y = (backgroundBounds.height - iconBounds.height) / 2;
                gc.drawImage(nodeIcon, x, y);
            }

            gc.dispose();
        }

        String getDisplayText() {
            return m_displayText;
        }

        String getSearchableText() {
            return m_searchText;
        }

        int[] getRangeIndices() {
            return m_rangeIndices;
        }

        boolean shouldRenderLowerText() {
            return m_renderLowerText;
        }

        NodeContainer getNodeContainer() {
            return m_nodeContainer;
        }

        Image getDisplayImage() {
            return m_displayImage;
        }
    }
}
