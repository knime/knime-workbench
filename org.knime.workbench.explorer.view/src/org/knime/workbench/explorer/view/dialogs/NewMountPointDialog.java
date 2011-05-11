/* ------------------------------------------------------------------
# * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Mar 22, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.dialogs;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.ui.KNIMEUIPlugin;


/**
 * Dialog for selecting a new resource/item to be displayed in the KNIME
 * Explorer view.
 *
 * @author ohl, University of Konstanz
 */

public class NewMountPointDialog extends ListDialog {

    private static final ImageDescriptor IMG_NEWITEM = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/new_knime55.png");

    private static final ImageDescriptor IMG_ERR = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/error.png");

    private static final Set<Character> INVALID_CHARS;
    private static final String INVALID_MSG;

    static {
        INVALID_CHARS = new LinkedHashSet<Character>();
        INVALID_CHARS.add(':');
        INVALID_CHARS.add('\\');
        INVALID_CHARS.add('/');
        StringBuffer sb = new StringBuffer();
        sb.append("The following characters are invalid as mount id: ");
        for (Character character : INVALID_CHARS) {
            sb.append(character);
            sb.append(" ");
        }
        INVALID_MSG = sb.toString();
    }

    private String m_mountIDval;

    private Button m_ok;

    private Label m_errIcon;

    private Label m_errText;

    private final Set<String> m_invalidIDs;

    /* --- the result (after ok) ---- */

    private Text m_mountID;

    private AbstractContentProviderFactory m_factory;


    /**
     * @param parentShell the parent shell
     * @param input list of selectable items
     * @param invalidIDs list of invalid ids - rejected in the mountID field.
     */
    public NewMountPointDialog(final Shell parentShell,
            final List<AbstractContentProviderFactory> input,
            final Collection<String> invalidIDs) {
        super(parentShell);
        m_invalidIDs = new HashSet<String>(invalidIDs);
        setAddCancelButton(true);
        setContentProvider(new ContentFactoryProvider(input));
        setLabelProvider(new ContentFactoryLabelProvider());
        setInput(input);
        setTitle("Select New Content");
        m_mountIDval = "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Button createButton(final Composite parent, final int id,
            final String label, final boolean defaultButton) {
        Button b = super.createButton(parent, id, label, defaultButton);
        if (id == IDialogConstants.OK_ID) {
            // disabled by default until validated
            b.setEnabled(false);
            m_ok = b;
        }
        return b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        // this method gets called through a double click (if cancel button is
        // added)
        if (!validate(null)) {
            return;
        }

        m_mountIDval = m_mountID.getText().trim();
        super.okPressed();
        m_factory = (AbstractContentProviderFactory)getResult()[0];
    }

    /**
     * @return the ID entered by the user (only valid after dialog is OKed.)
     */
    public String getMountID() {
        return m_mountIDval;
    }

    /**
     * @return the selected factory (only valid after dialog is OKed)
     */
    public AbstractContentProviderFactory getFactory() {
        return m_factory;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {

        createHeader(parent);

        Composite mountHdr = new Composite(parent, SWT.FILL);
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight =
                convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gl.marginWidth = convertHorizontalDLUsToPixels(
                IDialogConstants.HORIZONTAL_MARGIN);
        gl.verticalSpacing =
                convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gl.horizontalSpacing = convertHorizontalDLUsToPixels(
                IDialogConstants.HORIZONTAL_SPACING);
        mountHdr.setLayout(gl);
        mountHdr.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label txt = new Label(mountHdr, SWT.NONE);
        txt.setText(
                "Enter the name that is used to reference the new content.");

        Composite mountInput = new Composite(mountHdr, SWT.FILL | SWT.BORDER);
        gl = new GridLayout(2, false);
        gl.marginHeight =
                convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gl.marginWidth = convertHorizontalDLUsToPixels(
                IDialogConstants.HORIZONTAL_MARGIN);
        // gl.verticalSpacing =
        // convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gl.horizontalSpacing = convertHorizontalDLUsToPixels(
                IDialogConstants.HORIZONTAL_SPACING);
        mountInput.setLayout(gl);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        mountInput.setLayoutData(gd);

        Label l = new Label(mountInput, SWT.NONE);
        l.setText("Mount ID:");
        m_mountID = new Text(mountInput, SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        m_mountID.setLayoutData(gridData);
        m_mountID.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(final KeyEvent e) {
                validate(e);
            }

            @Override
            public void keyPressed(final KeyEvent e) {
                validate(e);
            }
        });

        // insert the selection list
        Control c = super.createDialogArea(parent);

        getTableViewer().addSelectionChangedListener(
                new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(
                            final SelectionChangedEvent event) {
                        validate(null);
                    }
                });
        return c;

    }

    /**
     * Enables the ok button and sets the error icon/message.
     * @param keyEvent the key event or null
     *
     * @return true, if the selection/input is okay.
     */
    protected boolean validate(final KeyEvent keyEvent) {
        boolean valid = true;
        String errMsg = "";
        if (getTableViewer().getSelection().isEmpty()) {
            valid = false;
            errMsg = "Please select a resource to add.";
        }

        String id = m_mountID.getText().trim();
        if (id == null || id.isEmpty()) {
            valid = false;
            errMsg = "Please enter a valid mount ID.";
        } else {
            if (m_invalidIDs.contains(id)) {
                valid = false;
                errMsg =
                        "Mount ID already in use. Please enter "
                                + "a different ID.";
            }
        }
        if (keyEvent != null && INVALID_CHARS.contains(keyEvent.character)) {
            valid = false;
            errMsg = INVALID_MSG;
        } else {
            for (char c : id.toCharArray()) {
                if (INVALID_CHARS.contains(c)) {
                    valid = false;
                    errMsg = INVALID_MSG;
                    break;
                }
            }
        }

        m_errText.setText(errMsg);
        m_errIcon.setVisible(!valid);
        m_ok.setEnabled(valid);
        return valid;
    }

    /**
     * Adds the white header to the dialog.
     *
     * @param parent
     */
    protected void createHeader(final Composite parent) {
        Composite header = new Composite(parent, SWT.FILL);
        Color white = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
        header.setBackground(white);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        header.setLayoutData(gridData);
        header.setLayout(new GridLayout(3, false));
        // first row
        new Label(header, SWT.NONE);
        Label exec = new Label(header, SWT.NONE);
        exec.setBackground(white);
        exec.setText("Mounting a new Resource for Display in the KNIME Explorer");
        FontData[] fd = parent.getFont().getFontData();
        for (FontData f : fd) {
            f.setStyle(SWT.BOLD);
            f.setHeight(f.getHeight() + 2);
        }
        exec.setFont(new Font(parent.getDisplay(), fd));
        exec.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Label execIcon = new Label(header, SWT.NONE);
        execIcon.setBackground(white);
        execIcon.setImage(IMG_NEWITEM.createImage());
        execIcon.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, true));
        // second row
        new Label(header, SWT.None);
        Label txt = new Label(header, SWT.NONE);
        txt.setBackground(white);
        txt.setText("Please select the type of resource that should "
                + "be mounted.");
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        new Label(header, SWT.None);
        // third row
        m_errIcon = new Label(header, SWT.NONE);
        m_errIcon.setVisible(true);
        m_errIcon.setImage(IMG_ERR.createImage());
        m_errIcon.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_BEGINNING));
        m_errIcon.setBackground(white);
        m_errText = new Label(header, SWT.None);
        m_errText.setText("Please enter a mount id.");
        m_errText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_errText.setBackground(white);
        new Label(header, SWT.None);
    }

    private static class ContentFactoryLabelProvider implements ILabelProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public void addListener(final ILabelProviderListener listener) {
            // not supported
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            // nothing
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLabelProperty(final Object element,
                final String property) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeListener(final ILabelProviderListener listener) {
            // not supported
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Image getImage(final Object element) {
            if (element instanceof AbstractContentProviderFactory) {
                return ((AbstractContentProviderFactory)element).getImage();
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(final Object element) {
            if (element instanceof AbstractContentProviderFactory) {
                return ((AbstractContentProviderFactory)element).toString();
            } else {
                return null;
            }
        }

    }

    private final class ContentFactoryProvider implements
            IStructuredContentProvider {
        private final List<AbstractContentProviderFactory> m_elements;

        private ContentFactoryProvider(
                final List<AbstractContentProviderFactory> elements) {
            m_elements = elements;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object[] getElements(final Object inputElement) {
            if (inputElement == m_elements) {
                return ((List<AbstractContentProviderFactory>)inputElement)
                        .toArray();
            }
            return new Object[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            // nothing here.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput,
                final Object newInput) {
            // empty.
        }
    }
}
