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
 */
package org.knime.workbench.editor2.actions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.SelectionManager;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectableEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Abstract base class for actions that do something with a
 * <code>NodeContainer</code> inside the <code>WorkflowEditor</code>. Note
 * that this hooks as a workflow listener as soon as the
 * <code>WorkflowManager</code> is available. This is needed, because
 * enablement of an action may change not only on selection changes but also on
 * workflow changes.
 *
 * @author Florian Georg, University of Konstanz
 */
public abstract class AbstractNodeAction extends SelectionAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public void update() {
        super.update();
        // update hotkey text after the hotkey was changed in the
        // preferences
        setText(getText());
    }

    private final WorkflowEditor m_editor;

    /**
     *
     * @param editor The editor that is associated with this action
     */
    public AbstractNodeAction(final WorkflowEditor editor) {
        super(editor);
        setLazyEnablementCalculation(true);
        m_editor = editor;

    }

    /**
     * @param commandID from the org.knime.workbench.editor/plugin.xml which links the action to the the label/shortcut sequence
     * @return shortcut sequence or empty string if no shortcut sequence is available
     */
    public String getHotkey(final String commandID) {
        IBindingService bindingService = PlatformUI.getWorkbench().getAdapter(IBindingService.class);
        if (bindingService == null) {
            return "";
        }

        String hotkey = bindingService.getBestActiveBindingFormattedFor(commandID);
        return (hotkey == null) ? "" : hotkey;
    }

    /**
     * Note that this return may be <code>null</code> if the editor has not already been created completely!
     *
     * @return The manager that is edited by the current editor. Subclasses may want to have a reference to this.
     * @throws IllegalStateException if the subclass' implementation of <code>canHandleWorkflowManagerUI()</code>
     *             returns true
     * @see #canHandleWorkflowManagerUI()
     */
    protected final WorkflowManager getManager() {
        if (canHandleWorkflowManagerUI()) {
            throw new IllegalStateException(
                "This action can supposedly handle the WorkflowManagerUI but tries to retrieve the WorkflowManager.");
        }
        return m_editor.getWorkflowManager().get();
    }

    /**
     * @return The manager that is edited by the current editor. Subclasses that can handle a {@link WorkflowManagerUI}
     *         (see {@link #canHandleWorkflowManagerUI()}) may want to have a reference to this.
     *
     * Note that this value may be <code>null</code> if the editor has not
     * already been created completely !
     */
    protected final WorkflowManagerUI getManagerUI() {
        return m_editor.getWorkflowManagerUI();
    }

    /**
     * Calls <code>runOnNodes</code> with the current selected
     * <code>NodeContainerEditPart</code>s.
     *
     * @see org.eclipse.jface.action.IAction#run()
     */
    @Override
    public final void run() {
        // call implementation of this action in the SWT UI thread
        Display.getCurrent().syncExec(this::runInSWT);
    }

    /**
     * Calls {@link #runOnNodes(NodeContainerEditPart[])}
     * with the current selected <code>NodeContainerEditPart</code>s.
     */
    public void runInSWT() {
        // get selected parts...
        final NodeContainerEditPart[] parts = getSelectedParts(NodeContainerEditPart.class);
        runOnNodes(parts);

    }

    /**
     * Get selected edit parts. <b>NOTE:</b> The parts returned by this may no longer exist in the associated
     * <code>WorkflowManager</code> instance.
     *
     * @param editPartClass The class of interest
     * @param <T> The class to the argument
     * @return The selected <code>EditParts</code> of the given part.
     */
    protected <T extends EditPart> T[] getSelectedParts(final Class<T> editPartClass) {
        return filterObjects(editPartClass, getSelectedObjects());
    }

    /**
     * Get selected connectable edit parts. Honestly i have no idea why <code>getSelectedParts</code> clamps down on T -
     * if the consumer want to look for all selected objects that subclass AtomicBoolean, so be it - they'll get an
     * empty list - their problem. Until we come to consensus on that, i've implemented this variant. TODO
     *
     * <b>NOTE:</b> The parts returned by this may no longer exist in the associated <code>WorkflowManager</code>
     * instance.
     *
     * @param connectableClass The class of interest
     * @param <T> The class to the argument
     * @return The selected <code>ConnectableEditPart</code> of the given part.
     */
    protected <T extends ConnectableEditPart> T[] getSelectedConnectables(final Class<T> connectableClass) {
        return filterObjectsForConnectables(connectableClass, getSelectedObjects());
    }

    /**
     * Get all edit parts.
     * @param editPartClass The class of interest
     * @param <T> The class to the argument
     * @return The <code>EditParts</code> of the given part.
     */
    protected <T extends EditPart> T[] getAllParts(final Class<T> editPartClass) {
        return filterObjects(editPartClass, getAllObjects());
    }

    /**
     * @param editPartClass The class of interest
     * @param list To filter from
     * @param <T> The class to the argument
     * @return The selected <code>EditParts</code> of the given part. */
    public static final <T extends EditPart> T[] filterObjects(final Class<T> editPartClass, final List<?> list) {
        final ArrayList<T> objects = new ArrayList<T>();

        // clean list, that is, remove all objects that are not edit
        // parts for a NodeContainer
        for (Object e : list) {
            if (editPartClass.isInstance(e)) {
                objects.add(editPartClass.cast(e));
            }
        }
        @SuppressWarnings("unchecked")
        final T[] array = (T[])Array.newInstance(editPartClass, objects.size());
        return objects.toArray(array);
    }

    /**
     * See comments in <code>getSelectedConnectables</code>.
     *
     * @param connectableClass The class of interest
     * @param list To filter from
     * @param <T> The class to the argument
     * @return The selected <code>EditParts</code> of the given part.
     * @see #getSelectedConnectables(Class)
     */
    public static final <T extends ConnectableEditPart> T[]
        filterObjectsForConnectables(final Class<T> connectableClass, final List<?> list) {
        final ArrayList<T> objects = new ArrayList<T>();

        // clean list, that is, remove all objects that are not edit parts for a NodeContainer
        for (Object e : list) {
            if (connectableClass.isInstance(e)) {
                objects.add(connectableClass.cast(e));
            }
        }
        @SuppressWarnings("unchecked")
        final T[] array = (T[])Array.newInstance(connectableClass, objects.size());
        return objects.toArray(array);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // Cannot generic type to List<?> due to AbstractClipboardAction casting to ArrayList<ConnectionContainerEditPart>
    // (which is arguably code that need be tweaked...)
    @SuppressWarnings("rawtypes")
    protected List getSelectedObjects() {
        final ISelectionProvider provider = m_editor.getEditorSite().getSelectionProvider();
        if (provider == null) {
            return Collections.EMPTY_LIST;
        }
        final ISelection sel = provider.getSelection();
        if (!(sel instanceof IStructuredSelection)) {
            return Collections.EMPTY_LIST;
        }

        return ((IStructuredSelection)sel).toList();
    }

    /**
     * @return the selection manager associated to the editor's selection provider's root edit part
     */
    protected SelectionManager getSelectionManager() {
        final ScrollingGraphicalViewer provider =
            (ScrollingGraphicalViewer)m_editor.getEditorSite().getSelectionProvider();

        if (provider == null) {
            return null;
        }

        // get parent of the node parts
        final EditPart editorPart = (EditPart)provider.getRootEditPart().getChildren().get(0);

        return editorPart.getViewer().getSelectionManager();
    }

    /**
     * @return all objects of the selected editor site.
     */
    protected List<?> getAllObjects() {
        final ScrollingGraphicalViewer provider =
            (ScrollingGraphicalViewer)m_editor.getEditorSite().getSelectionProvider();
        if (provider == null) {
            return Collections.EMPTY_LIST;
        }

        // get parent of the node parts
        final EditPart editorPart = (EditPart)provider.getRootEditPart().getChildren().get(0);

        return editorPart.getChildren();
    }

    /** {@inheritDoc} */
    @Override
    public abstract String getId();

    /** Clients can implement action code here (or overwrite
     * {@link #runInSWT()}).
     *
     * @param nodeParts The parts that the action should be executed on.
     */
    public abstract void runOnNodes(final NodeContainerEditPart[] nodeParts);

    /**
     * @return the underlying editor for this action
     */
    protected WorkflowEditor getEditor() {
        return m_editor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final boolean calculateEnabled() {
        if (getManagerUI() != null && !Wrapper.wraps(getManagerUI(), WorkflowManager.class)
            && !canHandleWorkflowManagerUI()) {
            //if the WorkflowManagerUI is NOT just a wrapper the WorkflowManager
            //and the action cannot deal with the WorkflowManagerUI-interface itself, it is disabled
            return false;
        }
        return getManagerUI() != null && internalCalculateEnabled();
    }

    /**
     * Subclasses override this method and return <code>true</code> if they can handle, i.e., work on the
     * {@link WorkflowManagerUI} interface. If the current {@link WorkflowManagerUI} is NOT just a wrapper of {@link WorkflowManager}
     * and the derived action cannot deal with the {@link WorkflowManagerUI}-interface only, the respective action will be
     * disabled!
     *
     * If the methods returns <code>true</code> the action MUST only work on the {@link WorkflowManagerUI}, returned by
     * the {@link #getManagerUI()}-method.
     *
     * @return whether the action can handle the {@link WorkflowManagerUI}.
     *
     */
    protected boolean canHandleWorkflowManagerUI() {
        return false;
    }

    /**
     * @return If this action is enabled
     */
    protected abstract boolean internalCalculateEnabled();

}
