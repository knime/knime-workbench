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
 *   Nov 24, 2024 (magnus): created
 */
package org.knime.workbench.editor.svgexport.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.editor2.WorkflowEditor;

/**
 * Workflow export SVG action.
 *
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
*/
public class ExportToSVGAction extends Action {

   private static final int SIZING_WIZARD_WIDTH = 470;

   private static final int SIZING_WIZARD_HEIGHT = 400;

   /**
    * The ID for this action.
    */
   public static final String ID = "org.knime.workbench.editor.svgexport.actions.ExportToSVGAction";

   /**
    * The constructor.
    */
   public ExportToSVGAction() {
       super("Image (SVG)...");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getId() {
       return ID;
   }

/**
    * {@inheritDoc}
    */
   @SuppressWarnings("restriction")
   @Override
   public void run() {
       IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
       if (workbenchWindow == null) {
           // action has been disposed
           return;
       }
       SVGExportWizard wizard = new SVGExportWizard();
       wizard.init(workbenchWindow.getWorkbench(), null);
       Shell parent = workbenchWindow.getShell();
       WizardDialog dialog = new WizardDialog(parent, wizard);
       dialog.create();
       dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x), SIZING_WIZARD_HEIGHT);
       dialog.open();
       return;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isEnabled() {
       IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
       return workbenchWindow != null && workbenchWindow.getActivePage().getActiveEditor() instanceof WorkflowEditor;
   }

}
