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
 *   Mar 4, 2019 (loki): created
 */
package org.knime.workbench.outportview;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.OutPortView;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodeOutPortUI;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * The genesis for this view is AP-5078; this is an Eclipse view which allows the user to display the outport content of
 * the currently selected node.
 *
 * TODO detection of part closing - then getViewSite().getPage().removeSelectionListener(this);
 *
 * @author loki der quaeler
 */
public class OutPortViewPart extends ViewPart implements ISelectionListener {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(OutPortViewPart.class);

    private static final int ROUNDED_RADIUS = 2;

    private static Color BORDER_COLOR = null;

    private static synchronized void initializeSWTAssetsIfNecessary() {
        if (BORDER_COLOR == null) {
            final Display d = Display.getDefault();

            BORDER_COLOR = FlatButton.createColorFromHexString(d, "#D7D7D7");
        }
    }


    private JPanel m_outPortContentPanel;
    private Composite m_outPortContentPane;

    private Composite m_buttonPane;
    private final ArrayList<FlatButton> m_currentPortButtons = new ArrayList<>();
    private final FlatButtonRadioGroup m_buttonRadioGroup = new FlatButtonRadioGroup();
    private final OutPortButtonClickListener m_clickListener = new OutPortButtonClickListener();

    private NodeContainerUI m_currentNodeContainerUI;
    private OutPortView m_currentOutPortView;

    private WeakReference<IStructuredSelection> m_lastSelectionReference;

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        initializeSWTAssetsIfNecessary();

        getViewSite().getPage().addSelectionListener(this);

        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = 3;
        layout.verticalSpacing = 9;
        parent.setLayout(layout);

        m_buttonPane = new Composite(parent, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.minimumHeight = 22;
        m_buttonPane.setLayoutData(gd);

        m_outPortContentPane = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND | SWT.BORDER);
        m_outPortContentPane.setLayout(new FillLayout());
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        m_outPortContentPane.setLayoutData(gd);

        final Frame frame = SWT_AWT.new_Frame(m_outPortContentPane);
        m_outPortContentPanel = new JPanel(new BorderLayout());
        frame.add(m_outPortContentPanel);
        final Color c = parent.getBackground();
        m_outPortContentPanel.setBackground(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue()));
        m_outPortContentPanel.setOpaque(true);

        parent.requestLayout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection is) {
        if (is instanceof IStructuredSelection) {
            final IStructuredSelection selection = (IStructuredSelection)is;
            final IStructuredSelection lastSelection =
                (m_lastSelectionReference == null) ? null : m_lastSelectionReference.get();

            if (selection.size() != 1) {
                populateView(null);

                return;
            }

            if (selection.equals(lastSelection)) {
                return;
            }

            m_lastSelectionReference = new WeakReference<>(selection);

            NodeContainerEditPart ncep = null;
            for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
                final Object item = iterator.next();

                if (item instanceof NodeContainerEditPart) {
                    ncep = (NodeContainerEditPart)item;
                }
            }
            populateView(ncep);
        }
    }

    private void populateViewForPort(final int portNumber) {
        if (m_currentNodeContainerUI != null) {
            final int outPortCount = m_currentNodeContainerUI.getNrOutPorts();

            if (outPortCount > portNumber) {
                final NodeOutPortUI ui = m_currentNodeContainerUI.getOutPort(portNumber);
                final Consumer<OutPortView> consumer = (view) -> {
                    m_currentOutPortView = view;

                    PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                        finishViewPortDisplay();
                    });
                };

                ui.createPortView(consumer);
            }
        }
    }

    private void finishViewPortDisplay() {
        if (m_currentOutPortView != null) {
            final Consumer<JTabbedPane> consumer = (jtp) -> {
                PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                    m_outPortContentPanel.add(jtp, BorderLayout.CENTER);
                    m_outPortContentPanel.invalidate();
                    m_outPortContentPanel.validate();

                    m_outPortContentPane.pack();
                    m_outPortContentPane.setSize(jtp.getBounds().width, jtp.getBounds().height);
                });
            };

            m_currentOutPortView.createDisplayAndReturnTabbedPane(consumer);
        }
    }

    private void clearOutPortContentPanel() {
        m_outPortContentPanel.removeAll();
        ViewUtils.invokeLaterInEDT(() -> {
            m_outPortContentPanel.repaint();
        });
    }

    private void populateView(final NodeContainerEditPart editPart) {
        for (final FlatButton button : m_currentPortButtons) {
            m_buttonRadioGroup.removeButton(button);

            button.dispose();
        }
        m_currentPortButtons.clear();

        clearOutPortContentPanel();

        if (editPart != null) {
            m_currentNodeContainerUI = editPart.getNodeContainer();

            final int outPortCount = m_currentNodeContainerUI.getNrOutPorts();
            if (outPortCount > 0) {
                final GridLayout layout = new GridLayout(outPortCount, false);
                layout.horizontalSpacing = 6;
                m_buttonPane.setLayout(layout);

                for (int i = 0; i < outPortCount; i++) {
                    final FlatButton button = createFlatButtonForOutPort(i);

                    m_currentPortButtons.add(button);
                    m_buttonRadioGroup.addButton(button);
                }

                m_buttonPane.pack();

                final int portToShow = getFirstNonFlowVariableOutPortOrZero();
                populateViewForPort(portToShow);
                m_currentPortButtons.get(portToShow).setSelected(true);
            }
        } else {
            m_currentNodeContainerUI = null;
            m_currentOutPortView = null;
        }
    }

    private FlatButton createFlatButtonForOutPort(final int index) {
        final NodeOutPortUI portUI = m_currentNodeContainerUI.getOutPort(index);
        final String buttonLabel;

        if (portUI.getPortObjectSpec() instanceof FlowVariablePortObjectSpec) {
            buttonLabel = "Flow Variables";
        } else {
            buttonLabel = portUI.getPortName();
        }

        final FlatButton button = new LabelFlatButton(m_buttonPane, buttonLabel, buttonLabel, true);
        final PaintListener painter = (pe) -> {
            final Point size = button.getSize();
            final GC gc = pe.gc;

            gc.setForeground(BORDER_COLOR);
            gc.drawRoundRectangle(1, 1, (size.x - 2), (size.y - 2), ROUNDED_RADIUS, ROUNDED_RADIUS);
        };

        button.setPostRenderer(painter);
        button.addClickListener(m_clickListener);

        return button;
    }

    private int getFirstNonFlowVariableOutPortOrZero() {
        final int outPortCount = m_currentNodeContainerUI.getNrOutPorts();

        for (int i = 0; i < outPortCount; i++) {
            final NodeOutPortUI portUI = m_currentNodeContainerUI.getOutPort(i);
            final PortObjectSpec spec = portUI.getPortObjectSpec();

            if (!(spec instanceof FlowVariablePortObjectSpec)) {
                return i;
            }
        }

        return 0;
    }


    private class OutPortButtonClickListener implements FlatButton.ClickListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void clickOccurred(final FlatButton source) {
            final int index = m_currentPortButtons.indexOf(source);

            clearOutPortContentPanel();

            populateViewForPort(index);
        }
    }
}
