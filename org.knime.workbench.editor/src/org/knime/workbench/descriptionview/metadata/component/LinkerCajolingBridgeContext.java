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
 *   Nov 7, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import java.util.Iterator;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.css.engine.CSSEngine;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.batik.script.Interpreter;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

/**
 * We provide this no-op subclass to keep the JVM happy with versions of {@code EventTarget}.
 *
 * @author loki der quaeler
 */
class LinkerCajolingBridgeContext extends BridgeContext {

    /**
     * @param userAgent
     */
    LinkerCajolingBridgeContext(final UserAgent ua) {
        super(ua);
    }

    /**
     * Disposes this BridgeContext - this method is exactly the same as its superclass, but as this plugin
     *  is forcing linkage against the same version of EventTarget that SVGOMDocument was built against,
     *  this dispose works whereas the one in the parent class would throw a:
     *      ClassCastException: org.apache.batik.dom.svg.SVGOMDocument cannot be cast to org.w3c.dom.events.EventTarget
     */
    @Override
    public void dispose() {
        synchronized (eventListenerSet) {
            // remove all listeners added by Bridges
            final Iterator<?> iter = eventListenerSet.iterator();
            while (iter.hasNext()) {
                final EventListenerMememto m = (EventListenerMememto)iter.next();
                final EventTarget et = m.getTarget();
                final EventListener el = m.getListener();
                final boolean uc = m.getUseCapture();
                final String t = m.getEventType();
                if ((et == null) || (el == null) || (t == null)) {
                    continue;
                }
                et.removeEventListener(t, el, uc);
            }
        }

        if (document != null) {
            final EventTarget evtTarget = (EventTarget)document;

            evtTarget.removeEventListener("DOMAttrModified", domAttrModifiedEventListener, true);
            evtTarget.removeEventListener("DOMNodeInserted", domNodeInsertedEventListener, true);
            evtTarget.removeEventListener("DOMNodeRemoved", domNodeRemovedEventListener, true);
            evtTarget.removeEventListener("DOMCharacterDataModified", domCharacterDataModifiedListener, true);

            final SVGOMDocument svgDocument = (SVGOMDocument)document;
            final CSSEngine cssEngine = svgDocument.getCSSEngine();
            if (cssEngine != null) {
                cssEngine.removeCSSEngineListener(cssPropertiesChangedListener);
                cssEngine.dispose();
                svgDocument.setCSSEngine(null);
            }
        }
        final Iterator<?> iter = interpreterMap.values().iterator();
        while (iter.hasNext()) {
            final Interpreter interpreter = (Interpreter)iter.next();
            if (interpreter != null) {
                interpreter.dispose();
            }
        }
        interpreterMap.clear();

        if (focusManager != null) {
            focusManager.dispose();
        }
    }
}
