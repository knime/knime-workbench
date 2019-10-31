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
 *   May 9, 2019 (loki): created
 */
package org.knime.workbench.descriptionview;

import java.lang.ref.WeakReference;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.knime.workbench.descriptionview.metadata.workflow.WorkflowMetaView;
import org.knime.workbench.descriptionview.node.HelpView;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.view.ContentObject;

/**
 * The genesis for this view is https://knime-com.atlassian.net/browse/AP-11628
 *
 * @author loki der quaeler
 */
public class DescriptionView extends ViewPart implements ISelectionListener {
    /** The id string registered in the plugin.xml **/
    public static final String ID = "org.knime.workbench.helpview";

    private static final String MIDST_EDIT_WARNING_TEXT =
        "You are still editing your workflow's metadata; do you want to save the metadata or discard any changes?";
    private static final String[] DIALOG_BUTTON_LABELS = {"Save", "Discard"};


    private StackLayout m_stackLayout;

    private Composite m_emptyView;
    private HelpView m_nodeDescriptionView;
    private WorkflowMetaView m_workflowMetaView;
    private Control m_previousNonEmtpyView;

    private WeakReference<IStructuredSelection> m_lastSelection;

    private Composite m_control;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        final IWorkbenchPage iwp = getViewSite().getPage();
        iwp.addSelectionListener(this);

        parent.setLayout(new GridLayout());

        m_control = new Composite(parent, SWT.NONE);

        m_stackLayout = new StackLayout();
        m_control.setLayout(m_stackLayout);

        final GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        m_control.setLayoutData(gd);

        m_emptyView = new Composite(m_control, SWT.NONE);
        m_workflowMetaView = new WorkflowMetaView(m_control);
        m_nodeDescriptionView = new HelpView(m_control);

        m_stackLayout.topControl = m_nodeDescriptionView;
        m_previousNonEmtpyView = m_nodeDescriptionView;

        parent.getDisplay().asyncExec(() -> {
            attemptToDisplayCurrentSelectionOrWorkflowMetadata();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose () {
        final IWorkbenchPage iwp = getViewSite().getPage();
        iwp.removeSelectionListener(this);
    }

    /**
     * This provides a sew-in point for the display of remotely fetched metadata as requested in AP-12082; if
     *  the author, description, and creation date are all null, it will be interpretted as a failure to
     *  fetch remote metadata.
     *
     * @param author the author, or null
     * @param legacyDescription the legacy-style description, or null
     * @param creationDate the creation date, or null
     * @param shouldShowCCBY40License if true, the CC-BY-4.0 license will be shown in the UI
     */
    public void displayRemoteMetadata(final String author, final String legacyDescription,
        final ZonedDateTime creationDate, final boolean shouldShowCCBY40License) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            final GregorianCalendar c = (creationDate != null) ? GregorianCalendar.from(creationDate) : null;
            m_workflowMetaView.handleAsynchronousRemoteMetadataPopulation(author, legacyDescription, c,
                shouldShowCCBY40License);
        });
    }

    /**
     * This gets messaged from {@code WorkflowEditPartFactory#partActivated}.
     *
     * @param isWelcomePage true if the part activated was the welcome page
     */
    public void changeViewDueToPartActivation(final boolean isWelcomePage) {
        moveControlToTop(isWelcomePage ? m_emptyView : m_previousNonEmtpyView);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        m_stackLayout.topControl.setFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        // TODO AP-12738 if the selection is a worfklow, and we're in a component's workflow, then fetch
        //              the SubNodeContainer and make a selection of it
        // TODO AP-12738 if the selection is a worfklow, and we're in a component's workflow, then fetch
        //              the SubNodeContainer and make a selection of it
        // TODO AP-12738 if the selection is a worfklow, and we're in a component's workflow, then fetch
        //              the SubNodeContainer and make a selection of it
        // TODO AP-12738 if the selection is a worfklow, and we're in a component's workflow, then fetch
        //              the SubNodeContainer and make a selection of it
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection)selection;
            final IStructuredSelection lastSelection = (m_lastSelection == null) ? null : m_lastSelection.get();

            // we do not clear our content if nothing is selected.
            if ((structuredSelection.size() < 1) || structuredSelection.equals(lastSelection)) {
                return;
            }

            // in some weird world where we have an edit mode for m_metanodeMetaView, that need be checked here too
            if ((lastSelection != null)
                    && ((lastSelection.getFirstElement() instanceof WorkflowRootEditPart)
                            || (lastSelection.getFirstElement() instanceof ContentObject))
                && m_workflowMetaView.inEditMode()) {

                if (m_workflowMetaView.modelIsDirty()) {
                    displayDirtyWarning(() -> {
                        finishSelectionChange(structuredSelection);
                    });
                } else {
                    m_workflowMetaView.endEditMode(false);
                    finishSelectionChange(structuredSelection);
                }
            } else {
                finishSelectionChange(structuredSelection);
            }
        }
    }

    private void moveControlToTop(final Control c) {
        if (m_stackLayout.topControl != c) {
            if (m_emptyView == c) {
                m_previousNonEmtpyView = m_stackLayout.topControl;
            }

            m_stackLayout.topControl = c;

            m_control.layout();
        }
    }

    private void displayDirtyWarning(final Runnable postDisplayAction) {
        final Display d = PlatformUI.getWorkbench().getDisplay();

        d.syncExec(() -> {
            final Shell s = d.getActiveShell();
            final int response = MessageDialog.open(MessageDialog.QUESTION, s, "Wait...",
                MIDST_EDIT_WARNING_TEXT, SWT.NONE, DIALOG_BUTTON_LABELS);
            final boolean save = (response == 0);

            m_workflowMetaView.endEditMode(save);

            if (postDisplayAction != null) {
                postDisplayAction.run();
            }
        });
    }

    private void finishSelectionChange(final IStructuredSelection structuredSelection) {
        m_lastSelection = new WeakReference<>(structuredSelection);

        final Object o = structuredSelection.getFirstElement();
        if ((o instanceof ContentObject) || (o instanceof WorkflowRootEditPart)) {
            moveControlToTop(m_workflowMetaView);
            m_workflowMetaView.selectionChanged(structuredSelection);
        } else {
            moveControlToTop(m_nodeDescriptionView);
            m_nodeDescriptionView.selectionChanged(structuredSelection);
        }
    }

    private void attemptToDisplayCurrentSelectionOrWorkflowMetadata() {
        final IWorkbenchWindow iww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (iww != null) {
            final IWorkbenchPage iwp = iww.getActivePage();

            if (iwp != null) {
                final IEditorPart iep = iwp.getActiveEditor();

                if (iep instanceof WorkflowEditor) {
                    final WorkflowEditor we = (WorkflowEditor)iep;
                    final ISelectionProvider provider = we.getEditorSite().getSelectionProvider();

                    if (provider != null) {
                        final ISelection selection = provider.getSelection();

                        if ((selection instanceof IStructuredSelection) && !selection.isEmpty()) {
                            finishSelectionChange((IStructuredSelection)selection);
                            return;
                        }
                    }

                    final WorkflowRootEditPart wrep =
                        (WorkflowRootEditPart)we.getViewer().getRootEditPart().getContents();

                    finishSelectionChange(new StructuredSelection(wrep));
                }
            }
        }
    }
}
