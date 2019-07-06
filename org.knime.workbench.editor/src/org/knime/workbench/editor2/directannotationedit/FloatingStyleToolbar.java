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
 *   Jul 5, 2019 (loki): created
 */
package org.knime.workbench.editor2.directannotationedit;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * The genesis for this is AP-11990 - and really, we should have done this path to being with.
 *
 * @author loki der quaeler
 */
public class FloatingStyleToolbar {
    private final Shell m_mainApplicationWindow;
    private final Shell m_toolbarWindow;

    private final AnnotationEditFloatingToolbar m_toolbar;

    private final ArrayList<Region> m_regionsToDispose;

    // presently there will never be more than one
    private final ArrayList<Control> m_controlsInView = new ArrayList<>();

    private final Listener m_showListener;
    private final Listener m_hideListener;

    /**
     * @param editor the editor which owns this toolbar
     */
    public FloatingStyleToolbar(final StyledTextEditor editor) {
        final Display display = PlatformUI.getWorkbench().getDisplay();
        m_mainApplicationWindow = display.getActiveShell();

        m_toolbarWindow = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
        final GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        m_toolbarWindow.setLayout(gl);
        m_toolbar = new AnnotationEditFloatingToolbar(m_toolbarWindow, editor, (source) -> {
            m_mainApplicationWindow.forceActive();
        });

        m_showListener = (event) -> {
            m_controlsInView.add((Control)event.widget);

            recomputeRegion();
        };
        m_hideListener = (event) -> {
            m_controlsInView.remove(event.widget);

            m_mainApplicationWindow.forceActive();

            recomputeRegion();
        };
        m_toolbar.addShowHideListeners(m_showListener, m_hideListener);

        m_regionsToDispose = new ArrayList<>();
        recomputeRegion();
    }

    void registerListenersWithColorDropDown(final ColorDropDown dropDown) {
        dropDown.addListener(SWT.Show, m_showListener);
        dropDown.addListener(SWT.Hide, m_hideListener);
    }

    boolean isDisposed() {
        return m_toolbarWindow.isDisposed();
    }

    void setLocationFromAnnotationDisplayLocation(final Point location) {
        final int y = location.y - 15 - m_toolbar.getSize().y;
        m_toolbarWindow.setLocation(location.x, y);
    }

    void dispose() {
        for (final Region region : m_regionsToDispose) {
            region.dispose();
        }
        m_regionsToDispose.clear();

        m_toolbar.dispose();
        m_toolbarWindow.dispose();
    }

    // If the window is not already open, then open it.
    void open() {
        if (!m_toolbarWindow.isVisible()) {
            m_toolbarWindow.open();
        }
    }

    void close() {
        dispose();
    }

    Shell getShell() {
        return m_toolbarWindow;
    }

    AnnotationEditFloatingToolbar getToolbar() {
        return m_toolbar;
    }

    private void recomputeRegion() {
        final Region region = new Region();
        final Point size = m_toolbar.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        region.add(new int[] {0, 0, 0, size.y, size.x, size.y, size.x, 0});
        for (final Control control : m_controlsInView) {
            final Rectangle bounds = control.getBounds();
            final int x2 = bounds.x + bounds.width;
            final int y2 = bounds.y + bounds.height;

            region.add(new int[] {bounds.x, bounds.y, bounds.x, y2, x2, y2, x2, bounds.y});
        }

        m_toolbarWindow.setRegion(region);
        m_toolbarWindow.setSize(region.getBounds().width, region.getBounds().height);
        m_regionsToDispose.add(region);
    }
}
