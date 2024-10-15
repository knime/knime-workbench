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
 *   Feb 27, 2018 (loki): created
 */
package org.knime.workbench.ui.wrapper;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.eclipse.gef.Disposable;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * This class provides a utility method to walk SWT and AWT/Swing component graphs instrumenting them with key listeners
 * of their respective ilks. This enters existence due to AP-5670 (and again as AP-9477).
 *
 * Along the way, i noticed that if i paused the SWT thread's execution during the construction of the
 * WrappedNodeDialog's components, then the key listeners would work on Mac until something else in the dialog received
 * focus, then never again. If i did not pause the thread's execution, the key listeners under Mac would never receive
 * key events - at a very base level (like NSResponder never does an objc_msgSend for an OS.sel_flagsChanged_ event.)
 *
 * I am working around this unsavory situation by attaching key listeners throughout the SWT and AWT component trees for
 * dialogs under Mac; pretty awful, but it gets us around some of the SWT_AWT issues here.
 *
 * @author loki der quaeler
 */
final class AWTKeyTracker {
    private static AWTKeyTracker INSTANCE = new AWTKeyTracker();

    static AWTKeyTracker getInstance () {
        return INSTANCE;
    }


    private AWTKeyTracker() {
    }

    /**
     * This must be called on the SWT thread.
     */
    Disposable instrumentTree(final Container awtParentContainer, final java.awt.event.KeyListener awtKeyListener,
        final Composite swtParentComposite, final KeyListener swtKeyListener) {
        if (Thread.currentThread() != Display.getCurrent().getThread()) {
            throw new IllegalStateException("This must be called on the SWT thread.");
        }

        // no need to synchronize - map is only modified/traversed in EDT
        final List<Disposable> awtDisposables = new ArrayList<>();
        SwingUtilities.invokeLater(() -> {
            Consumer<Component> awtAffector = component -> {
                final var keyListener = new AWTKeyListener(component, awtKeyListener);
                component.addKeyListener(keyListener);
                awtDisposables.add(keyListener);
            };

            walkAWTTree(awtParentContainer, awtAffector);
        });

        final List<Disposable> swtDisposables = new ArrayList<>();
        // Since this can only ever be called on the SWT thread, we needn't worry about concurrency issues with the map.
        Consumer<Control> swtAffector = control -> {
            final var keyListener = new SWTKeyListener(control, swtKeyListener);
            control.addKeyListener(keyListener);
            swtDisposables.add(keyListener);
        };

        walkSWTTree(swtParentComposite, swtAffector);

        // if this lambda gets called before AWT is done,... bad luck
        return () -> {
            SwingUtilities.invokeLater(() -> awtDisposables.forEach(Disposable::dispose));
            swtDisposables.forEach(Disposable::dispose);
        };
    }

    private void walkAWTTree(final Container c, final Consumer<Component> componentAffector) {
        if (c != null) {
            final Component[] children;

            componentAffector.accept(c);

            children = c.getComponents();
            for (Component child : children) {
                if (child instanceof Container) {
                    walkAWTTree((Container)child, componentAffector);
                } else {
                    componentAffector.accept(child);
                }
            }
        }
    }

    private void walkSWTTree(final Composite c, final Consumer<Control> controlAffector) {
        if (c != null) {
            final Control[] children;

            controlAffector.accept(c);

            children = c.getChildren();
            for (Control child : children) {
                if (child instanceof Composite) {
                    walkSWTTree((Composite)child, controlAffector);
                } else {
                    controlAffector.accept(child);
                }
            }
        }
    }


    // We wrap this for instanceof comparisons when deregistering listeners
    private static class AWTKeyListener extends KeyAdapter implements Disposable {
        private java.awt.event.KeyListener m_wrappedListener;
        private final Component m_component;

        private AWTKeyListener(final Component component, final java.awt.event.KeyListener kl) {
            m_component = component;
            m_wrappedListener = kl;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyPressed(final java.awt.event.KeyEvent ke) {
            m_wrappedListener.keyPressed(ke);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyReleased(final java.awt.event.KeyEvent ke) {
            m_wrappedListener.keyReleased(ke);
        }

        @Override
        public void dispose() {
            m_component.removeKeyListener(this);
        }
    }


    // We don't *need* to wrap the SWT one, but i prefer the symmetry for readability and it has low expense.
    private static class SWTKeyListener implements KeyListener, Disposable {
        private KeyListener m_wrappedListener;
        private final Control m_control;

        private SWTKeyListener(final Control control, final KeyListener kl) {
            m_control = control;
            m_wrappedListener = kl;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyPressed(final KeyEvent ke) {
            m_wrappedListener.keyPressed(ke);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyReleased(final KeyEvent ke) {
            m_wrappedListener.keyReleased(ke);
        }

        @Override
        public void dispose() {
            m_control.removeKeyListener(this);
        }
    }
}
