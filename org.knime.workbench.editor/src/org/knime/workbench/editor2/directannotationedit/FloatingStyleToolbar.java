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
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
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

    private final AtomicBoolean m_mainWindowActiveState;
    private final AtomicBoolean m_toolbarActiveState;

    private final AnnotationEditFloatingToolbar m_toolbar;

    private final ArrayList<Region> m_regionsToDispose;

    // presently there will never be more than one
    private final ArrayList<Control> m_controlsInView = new ArrayList<>();

    private final Listener m_showListener;
    private final Listener m_hideListener;

    private final MainWindowListener m_shellListener;

    private final ArrayList<VendedShellListener> m_vendedShellListeners;

    /**
     * @param editor the editor which owns this toolbar
     */
    public FloatingStyleToolbar(final StyledTextEditor editor) {
        final Display display = PlatformUI.getWorkbench().getDisplay();
        m_mainApplicationWindow = display.getActiveShell();

        m_vendedShellListeners = new ArrayList<>();

        m_shellListener = new MainWindowListener();
        m_mainApplicationWindow.addShellListener(m_shellListener);
        m_mainWindowActiveState = new AtomicBoolean(true);

        // AP-14496: On Linux/GTK, since at least SWT 4.16, SWT.ON_TOP behaves
        // differently than before w.r.t. manually transferring focus between Shells.
        // SWT.TOOL is an alternative flag that provides similar behaviour.
        int toolbarWindowFlags = Platform.OS_LINUX.equals(Platform.getOS()) ?
              SWT.NO_TRIM | SWT.TOOL
            : SWT.NO_TRIM | SWT.ON_TOP;
        m_toolbarWindow = new Shell(display, toolbarWindowFlags);

        final GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        m_toolbarWindow.setLayout(gl);
        m_toolbar = new AnnotationEditFloatingToolbar(m_toolbarWindow, editor, (source) -> {
            m_mainApplicationWindow.forceActive();
        });

        m_toolbarWindow.addShellListener(new ToolbarListener());
        m_toolbarActiveState = new AtomicBoolean(false);

        m_showListener = (event) -> {
            m_controlsInView.add((Control)event.widget);

            recomputeRegion();
        };
        m_hideListener = (event) -> {
            m_controlsInView.remove(event.widget);
            recomputeRegion();
        };
        m_toolbar.addShowHideListeners(m_showListener, m_hideListener);

        m_regionsToDispose = new ArrayList<>();
        recomputeRegion();
    }

    ShellListener vendShellListener() {
        final VendedShellListener shellListener = new VendedShellListener();

        m_vendedShellListeners.add(shellListener);

        return shellListener;
    }

    Shell getParentShell() {
        return m_toolbarWindow;
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

        m_mainApplicationWindow.removeShellListener(m_shellListener);

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

    void pruneToolbarForNodeAnnotation() {
        if (m_toolbar.pruneToolbarForNodeAnnotation()) {
            recomputeRegion();
        }
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


    private class MainWindowListener implements ShellListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void shellActivated(final ShellEvent se) {
            m_toolbarWindow.setVisible(true);
            m_mainWindowActiveState.set(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellClosed(final ShellEvent se) { }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellDeactivated(final ShellEvent se) {
            m_mainWindowActiveState.set(false);

            // as this will be an infrequently occuring case, i have no qualm in not take a thread from a pool
            (new Thread(new ToolbarHideDecider())).start();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellDeiconified(final ShellEvent se) {
            m_toolbarWindow.setVisible(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellIconified(final ShellEvent se) {
            m_toolbarWindow.setVisible(false);
            m_toolbarActiveState.set(false);
        }
    }


    private class ToolbarListener implements ShellListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void shellActivated(final ShellEvent se) {
            m_toolbarActiveState.set(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellClosed(final ShellEvent se) { }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellDeactivated(final ShellEvent se) {
            m_toolbarActiveState.set(false);

            // as this will be an infrequently occuring case, i have no qualm in not take a thread from a pool
            (new Thread(new ToolbarHideDecider())).start();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellDeiconified(final ShellEvent se) { }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellIconified(final ShellEvent se) { }
    }


    // we are not guaranteed an ordering of notifications, so we wait briefly and then determine whether
    //  the multi-varitate state is met requiring the toolbar to be hidden.
    private class ToolbarHideDecider implements Runnable {
        @Override
        public void run () {
            // The XYActiveState fields below are being set by ShellListeners
            // i.e. on activation/deactivation of a Shell.
            // These events are processed on the UI thread, here we are working on
            // a different thread. Hence, we need to ensure that there are no unprocessed
            // events before we determine and use the state of these fields.
            // `syncExec` blocks the current thread until the UI thread ran the given runnable.
            // `display.readAndDispatch()` processes all unprocessed events.
            Display display = m_toolbarWindow.getDisplay();
            display.syncExec(() -> {
                long start = System.currentTimeMillis();
                while (start + 2000 > System.currentTimeMillis()) {
                    while (display.readAndDispatch()) {
                    }
                }
            });

            for (final VendedShellListener listener : m_vendedShellListeners) {
                if (listener.getActiveState()) {
                    return;
                }
            }
            if (!m_toolbarActiveState.get() && !m_mainWindowActiveState.get() && !m_toolbarWindow.isDisposed()) {
                m_toolbarWindow.getDisplay().asyncExec(() -> {
                    if (!m_toolbarWindow.isDisposed()) {
                        m_toolbarWindow.setVisible(false);
                    }
                });
            }
        }
    }


    private static class VendedShellListener implements ShellListener {
        private AtomicBoolean m_activeState;

        VendedShellListener() {
            m_activeState = new AtomicBoolean(false);
        }

        private boolean getActiveState() {
            return m_activeState.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellActivated(final ShellEvent se) {
            m_activeState.set(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellClosed(final ShellEvent se) {
            m_activeState.set(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellDeactivated(final ShellEvent se) {
            m_activeState.set(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellDeiconified(final ShellEvent se) {
            m_activeState.set(true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shellIconified(final ShellEvent se) {
            m_activeState.set(false);
        }
    }
}
