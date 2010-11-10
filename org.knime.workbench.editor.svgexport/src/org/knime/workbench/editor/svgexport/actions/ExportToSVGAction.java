package org.knime.workbench.editor.svgexport.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 *  Action which initiates the SVG-export wizard.
 * 
 * @author Andreas Burger
 *
 */

public class ExportToSVGAction implements IWorkbenchWindowActionDelegate {
	private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;

    public ExportToSVGAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		IWorkbenchWindow workbenchWindow = 
            PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            // action has been disposed
            return;
        }

        SVGExportWizard wizard = new SVGExportWizard();

        wizard.init(workbenchWindow.getWorkbench(), null);

        Shell parent = workbenchWindow.getShell();
        WizardDialog dialog = new WizardDialog(parent, wizard);
        dialog.create();
        dialog.getShell().setSize(
                Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
                SIZING_WIZARD_HEIGHT);
        dialog.open();
    }


	public void selectionChanged(IAction action, ISelection selection) {
	}


	public void dispose() {
	}


	public void init(IWorkbenchWindow window) {
	}
}