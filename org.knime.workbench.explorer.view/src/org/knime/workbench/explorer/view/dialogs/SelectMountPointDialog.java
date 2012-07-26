/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * ---------------------------------------------------------------------
 *
 * Created: Mar 23, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.preferences.ExplorerPreferencePage;

/**
 *
 * @author ohl, University of Konstanz
 */
public class SelectMountPointDialog extends Dialog {
    private static final ImageDescriptor IMG_NEWMOUNT = AbstractUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/new_mountpoint55.png");

    private Table m_table;

    private List<MountPoint> m_result;

    private final Set<String> m_mountedIds;

    /**
     * @param parentShell the parent shell
     * @param mountedIds the ids of the mount points currently shown
     */
    public SelectMountPointDialog(final Shell parentShell,
            final Set<String> mountedIds) {
        super(parentShell);
        m_mountedIds = mountedIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        Point p = super.getInitialSize();
        p.y = (int)(p.y * 1.4);
        return p;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Select Mountpoints");
    }

    /**
     * {@inheritDoc}
     */
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
        createManageMountPointPanel(overall);
        createTableSelectionPanel(overall);
        return overall;
    }

    /**
     * Adds the white header to the dialog.
     *
     * @param parent the parent composite
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

    /**
     * Creates a table and adds it to the panel.
     *
     * @param panel the panel to add the table to
     */
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
        updateTableContent();
    }

    /**
     * Fills the table with the current content of the explorer mount table.
     */
    protected void updateTableContent() {
        final int mountIDidx = 0;
        // make sure unchecked content stays unchecked after update
        HashMap<String, String> m_unchecked = new HashMap<String, String>();
        // hashmap of <mountID, ACP.toString()>
        Map<String, AbstractContentProvider> mounts =
                ExplorerMountTable.getMountedContent();
        if (m_table.getItemCount() == 0) {
            // table created for the first time: check all IDs visible in view
            for (AbstractContentProvider acp : mounts.values()) {
                if (!m_mountedIds.contains(acp.getMountID())) {
                    m_unchecked.put(acp.getMountID(), acp.toString());
                }
            }
        } else {
            for (TableItem i : m_table.getItems()) {
                if (!i.getChecked()) {
                    // keep the mountID and toString() of unchecked items
                    AbstractContentProvider acp =
                            mounts.get(i.getText(mountIDidx));
                    if (acp != null) {
                        m_unchecked.put(acp.getMountID(), acp.toString());
                    }
                }
            }
        }
        m_table.removeAll();
        Map<String, AbstractContentProvider> mountedContent =
                ExplorerMountTable.getMountedContent();
        for (Map.Entry<String, AbstractContentProvider> e : mountedContent
                .entrySet()) {
            TableItem item = new TableItem(m_table, SWT.NONE);
            // Column0: MountID
            item.setText(mountIDidx, e.getKey());
            // Column 1: icon and name of provider instance
            AbstractContentProvider acp = e.getValue();
            item.setImage(1, acp.getImage());
            item.setText(1, acp.toString());
            // Column 2: icon and name of factory
            AbstractContentProviderFactory acpf = acp.getFactory();
            item.setImage(2, acpf.getImage());
            item.setText(2, acpf.toString());
            boolean check = true;
            String toString = m_unchecked.get(e.getKey());
            if (acp.toString().equals(toString)) {
                // if old mount point is still the same - keep it unchecked
                check = false;
            }
            item.setChecked(check);
        }
        for (TableColumn c : m_table.getColumns()) {
            c.pack();
        }
    }

    /**
     * Creates a panel containing a link to the Explorer preference page.
     *
     * @param parent the parent composite
     */
    protected void createManageMountPointPanel(final Composite parent) {
        Composite addPanel = new Composite(parent, SWT.FILL);
        GridLayout gl = new GridLayout(1, false);
        addPanel.setLayout(gl);
        addPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Link link = new Link(addPanel, SWT.NONE);
        link.setText("<A>Configure Explorer Settings...</A>");
        link.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        link.setToolTipText("Add or remove mount points etc.");
        link.addSelectionListener(new SelectionListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                PreferenceDialog dialog =
                        PreferencesUtil.createPreferenceDialogOn(getShell(),
                                ExplorerPreferencePage.ID,
                                new String[]{ExplorerPreferencePage.ID}, null);
                dialog.open();
                updateTableContent();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });

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
        super.okPressed();
    }

    /**
     * Returns the mount points that are selected for display in the view.
     *
     * @return the selected mount points
     */
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

    // public static void main2(final String[] args) {
    // Display display = new Display();
    // Shell shell = new Shell(display);
    // shell.setLayout(new FillLayout());
    // Table table = new Table(shell, SWT.BORDER | SWT.MULTI);
    // table.setLinesVisible(true);
    // for (int i = 0; i < 3; i++) {
    // TableColumn column = new TableColumn(table, SWT.NONE);
    // column.setWidth(100);
    // }
    // for (int i = 0; i < 12; i++) {
    // new TableItem(table, SWT.NONE);
    // }
    // TableItem[] items = table.getItems();
    // for (int i = 0; i < items.length; i++) {
    // TableEditor editor = new TableEditor(table);
    // CCombo combo = new CCombo(table, SWT.NONE);
    // combo.setText("CCombo");
    // combo.add("item 1");
    // combo.add("item 2");
    // editor.grabHorizontal = true;
    // editor.setEditor(combo, items[i], 0);
    // editor = new TableEditor(table);
    // Text text = new Text(table, SWT.NONE);
    // text.setText("Text");
    // editor.grabHorizontal = true;
    // editor.setEditor(text, items[i], 1);
    // editor = new TableEditor(table);
    // Button button = new Button(table, SWT.CHECK);
    // button.pack();
    // editor.minimumWidth = button.getSize().x;
    // editor.horizontalAlignment = SWT.LEFT;
    // editor.setEditor(button, items[i], 2);
    // }
    // shell.pack();
    // shell.open();
    // while (!shell.isDisposed()) {
    // if (!display.readAndDispatch())
    // display.sleep();
    // }
    // display.dispose();
    // }
    //
    // public static void main(final String[] args) {
    // Display display = new Display();
    // Shell shell = new Shell(display);
    // final Table table =
    // new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL
    // | SWT.H_SCROLL | SWT.FULL_SELECTION);
    // for (int i = 0; i < 12; i++) {
    // TableItem item = new TableItem(table, SWT.NONE);
    // item.setText("Item " + i);
    // }
    // Rectangle clientArea = shell.getClientArea();
    // table.setBounds(clientArea.x, clientArea.y, 100, 100);
    // table.addListener(SWT.Selection, new Listener() {
    // @Override
    // public void handleEvent(final Event event) {
    // TableItem[] sel = table.getItems();
    // for (TableItem ti : sel) {
    // if (ti.getChecked()) {
    // System.out.println("Checked: " + ti.getText());
    // }
    // }
    // }
    // });
    // shell.setSize(200, 200);
    // shell.open();
    // while (!shell.isDisposed()) {
    // if (!display.readAndDispatch())
    // display.sleep();
    // }
    // display.dispose();
    // }

}
