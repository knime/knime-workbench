/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
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
 * Created: Mar 23, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.ui.KNIMEUIPlugin;


/**
 *
 * @author ohl, University of Konstanz
 */
public class SelectMountPointDialog extends Dialog {

    private static final ImageDescriptor IMG_NEWMOUNT = AbstractUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/new_mountpoint55.png");

    private static final ImageDescriptor IMG_ERR = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/error.png");

    private Table m_table;
    private Button m_umountButton;

    private List<MountPoint> m_result;

    public SelectMountPointDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Select Mountpoint");
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight =
                convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gl.marginWidth =
                convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        gl.verticalSpacing =
                convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gl.horizontalSpacing =
                convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

        Composite overall = new Composite(parent, SWT.FILL);
        overall.setLayoutData(new GridData(GridData.FILL_BOTH));
        overall.setLayout(gl);

        createHeader(overall);
        createAddMountPointPanel(overall);
        createTableSelectionPanel(overall);
        return overall;
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
        exec.setText("Change Resources");
        FontData[] fd = parent.getFont().getFontData();
        for (FontData f : fd) {
            f.setStyle(SWT.BOLD);
            f.setHeight(f.getHeight() + 2);
        }
        exec.setFont(new Font(parent.getDisplay(), fd));
        exec.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        Label execIcon = new Label(header, SWT.NONE);
        execIcon.setBackground(white);
        execIcon.setImage(IMG_NEWMOUNT.createImage());
        execIcon.setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, true));
        // second row
        new Label(header, SWT.None);
        Label txt = new Label(header, SWT.NONE);
        txt.setBackground(white);
        txt.setText("Please select the mount point(s) that should "
                + "be displayed in the KNIME explorer.");
        txt.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        new Label(header, SWT.None);
        // // third row
        // m_errIcon = new Label(header, SWT.NONE);
        // m_errIcon.setVisible(true);
        // m_errIcon.setImage(IMG_ERR.createImage());
        // m_errIcon.setLayoutData(new GridData(
        // GridData.HORIZONTAL_ALIGN_BEGINNING));
        // m_errIcon.setBackground(white);
        // m_errText = new Label(header, SWT.None);
        // m_errText.setText("Please select a mount point to add.");
        // m_errText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // m_errText.setBackground(white);
        // new Label(header, SWT.None);
    }

    protected void createTableSelectionPanel(final Composite panel) {

        m_table =
                new Table(panel, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL
                        | SWT.V_SCROLL | SWT.CHECK);
        m_table.setLinesVisible(true);
        m_table.setHeaderVisible(true);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        m_table.setLayoutData(gd);
        TableColumn col0 = new TableColumn(m_table, SWT.NONE);
        TableColumn col1 = new TableColumn(m_table, SWT.NONE);
        TableColumn col2 = new TableColumn(m_table, SWT.NONE);
        col0.setText("MountID");
        col1.setText("Mounted Content");
        col2.setText("Mounted Type");
        m_table.addListener(SWT.Selection, new Listener () {
            @Override
            public void handleEvent (final Event event) {
                if (event.detail != SWT.CHECK) {
                    onTableSelectionChanged();
                }
            }
        });
        onTableSelectionChanged();
        synchronizeTable();
    }

    protected void synchronizeTable() {
        m_table.removeAll();
        Map<String, AbstractContentProvider> mountedContent =
                ExplorerMountTable.getMountedContent();
        for (Map.Entry<String, AbstractContentProvider> e : mountedContent
                .entrySet()) {
            TableItem item = new TableItem(m_table, SWT.NONE);
            // Column0: MountID
            item.setText(0, e.getKey());
            // Column 1: icon and name of provider instance
            AbstractContentProvider acp = e.getValue();
            item.setImage(1, acp.getImage());
            item.setText(1, acp.toString());
            // Column 2: icon and name of factory
            AbstractContentProviderFactory acpf = acp.getFactory();
            item.setImage(2, acpf.getImage());
            item.setText(2, acpf.toString());
        }
        for (TableColumn c : m_table.getColumns()) {
            c.pack();
        }
    }

    protected void createAddMountPointPanel(final Composite parent) {
        Composite addPanel = new Composite(parent, SWT.FILL);
        GridLayout gl = new GridLayout(3, false);
        addPanel.setLayout(gl);
        addPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label txt = new Label(addPanel, SWT.NONE);
        txt.setText("Change available resources ...");
        txt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        Button mnt = new Button(addPanel, SWT.PUSH);
        mnt.setText("Add New...");
        mnt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        mnt.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addNewMountPoint();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                addNewMountPoint();
            }
        });
        m_umountButton = new Button(addPanel, SWT.PUSH);
        m_umountButton.setText("Remove Selected");
        m_umountButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        m_umountButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                removeSelectedMountPoints();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                removeSelectedMountPoints();
            }
        });
    }

    private void onTableSelectionChanged() {
        m_umountButton.setEnabled(m_table.getSelectionCount() > 0);
    }

    protected void addNewMountPoint() {
        NewMountPointDialog dlg =
                new NewMountPointDialog(getShell(),
                        ExplorerMountTable.getAddableContentProviders(),
                        ExplorerMountTable.getAllMountIDs());
        if (dlg.open() != Dialog.OK) {
            return;
        }
        AbstractContentProvider newCP = null;
        try {
            newCP =
                    ExplorerMountTable.mount(dlg.getMountID(), dlg.getFactory()
                            .getID());
        } catch (IOException e) {
            MessageBox mb =
                    new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            String msg = e.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = "<no details>";
            }
            mb.setMessage("I/O error while creating new content: " + msg);
            mb.open();
        }
        if (newCP != null) {
            synchronizeTable();
        }
    }

    protected void removeSelectedMountPoints() {
        int[] selectionIndices = m_table.getSelectionIndices();
        for (int index : selectionIndices) {
            String key = m_table.getItem(index).getText();
            ExplorerMountTable.unmount(key);
        }
        synchronizeTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {

        m_result = new ArrayList<MountPoint>();
        // mount id in column 0
        TableItem[] items = m_table.getItems();
        for (TableItem item : items) {
            if (item.getChecked()) {
                m_result.add(ExplorerMountTable.getMountPoint(item.getText(0)));
            }
        }

        m_table.getItem(0);

        super.okPressed();
    }

    public List<MountPoint> getResult() {
        return m_result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    public static void main2(final String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());
        Table table = new Table(shell, SWT.BORDER | SWT.MULTI);
        table.setLinesVisible(true);
        for (int i = 0; i < 3; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setWidth(100);
        }
        for (int i = 0; i < 12; i++) {
            new TableItem(table, SWT.NONE);
        }
        TableItem[] items = table.getItems();
        for (int i = 0; i < items.length; i++) {
            TableEditor editor = new TableEditor(table);
            CCombo combo = new CCombo(table, SWT.NONE);
            combo.setText("CCombo");
            combo.add("item 1");
            combo.add("item 2");
            editor.grabHorizontal = true;
            editor.setEditor(combo, items[i], 0);
            editor = new TableEditor(table);
            Text text = new Text(table, SWT.NONE);
            text.setText("Text");
            editor.grabHorizontal = true;
            editor.setEditor(text, items[i], 1);
            editor = new TableEditor(table);
            Button button = new Button(table, SWT.CHECK);
            button.pack();
            editor.minimumWidth = button.getSize().x;
            editor.horizontalAlignment = SWT.LEFT;
            editor.setEditor(button, items[i], 2);
        }
        shell.pack();
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }

    public static void main(final String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        final Table table =
                new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL
                        | SWT.H_SCROLL | SWT.FULL_SELECTION);
        for (int i = 0; i < 12; i++) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText("Item " + i);
        }
        Rectangle clientArea = shell.getClientArea();
        table.setBounds(clientArea.x, clientArea.y, 100, 100);
        table.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                TableItem[] sel = table.getItems();
                for (TableItem ti : sel) {
                    if (ti.getChecked()) {
                        System.out.println("Checked: " + ti.getText());
                    }
                }
            }
        });
        shell.setSize(200, 200);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }

}
