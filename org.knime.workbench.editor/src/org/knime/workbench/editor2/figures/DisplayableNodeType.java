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
 *   Nov 13, 2019 (loki): created
 */
package org.knime.workbench.editor2.figures;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.util.Pair;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * A class which wraps the {@code NodeFactory.NodeType} enum, adding (and caching) background {@code Image} instances
 * for rendering.
 *
 * @author loki der quaeler
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class DisplayableNodeType {
    private static final Map<NodeType, String> NODE_IMAGE_MAP = initMap();

    private static Map<NodeType, String> initMap() {
        Map<NodeType, String> m = new HashMap<>();
        m.put(NodeType.Learner, "learner");
        m.put(NodeType.LoopEnd, "looper_end");
        m.put(NodeType.LoopStart, "looper_start");
        m.put(NodeType.Manipulator, "manipulator");
        m.put(NodeType.Meta, "meta");
        m.put(NodeType.Missing, "missing");
        m.put(NodeType.Other, "other");
        m.put(NodeType.Predictor, "predictor");
        m.put(NodeType.QuickForm, "quickform");
        m.put(NodeType.ScopeEnd, "scope_end");
        m.put(NodeType.ScopeStart, "scope_start");
        m.put(NodeType.Configuration, "quickform");
        m.put(NodeType.Source, "source");
        m.put(NodeType.Sink, "sink");
        m.put(NodeType.Subnode, "subnode");
        m.put(NodeType.Unknown, "unknown");
        m.put(NodeType.VirtualIn, "virtual_in");
        m.put(NodeType.VirtualOut, "virtual_out");
        m.put(NodeType.Visualizer, "viewer");
        m.put(NodeType.Widget, "viewer");
        return m;
    }

    private static final Map<Pair<NodeFactory.NodeType, Boolean>, DisplayableNodeType> CACHE = new HashMap<>();

    /**
     * Given the {@code NodeFactory.NodeType}, return the mapped instance of this enum.
     *
     * @param nodeType
     * @param isComponent if the node type is requested for a component
     * @return the instance of this enum which wraps the parameter value enum
     */
    public synchronized static DisplayableNodeType getTypeForNodeType(final NodeFactory.NodeType nodeType,
        final boolean isComponent) {
        return CACHE.computeIfAbsent(Pair.create(nodeType, isComponent), k -> {
            Image image = ImageRepository.getUnscaledImage(KNIMEEditorPlugin.PLUGIN_ID,
                "icons/node/background_" + NODE_IMAGE_MAP.get(nodeType) + (isComponent ? "_component" : "") + ".png");
            return new DisplayableNodeType(nodeType.name(), nodeType, image);
        });
    }

    private final String m_displayText;
    private final NodeFactory.NodeType m_nodeType;
    private final Image m_nodeBackgroundImage;

    private DisplayableNodeType(final String displayText, final NodeFactory.NodeType nodeType,
        final Image backgroundImage) {
        m_displayText = displayText;
        m_nodeType = nodeType;
        m_nodeBackgroundImage = backgroundImage;
    }

    /**
     * @return the human readable text representing this enum
     */
    public String getDisplayText() {
        return m_displayText;
    }

    /**
     * @return the {@code NodeFactory.NodeType} which this enum instance wraps.
     */
    public NodeFactory.NodeType getNodeType() {
        return m_nodeType;
    }

    /**
     * @return the background image associated to this type. <b>DO NOT DISPOSE THIS IMAGE.</b>
     */
    public Image getImage() {
        return m_nodeBackgroundImage;
    }
}
