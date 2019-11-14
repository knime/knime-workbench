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
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * An enum which wraps the {@code NodeFactory.NodeType} enum, adding in display text and background {@code Image}
 * instances for rendering.
 *
 * @author loki der quaeler
 */
public enum DisplayableNodeType {
    /** A learning node. */
    LEARNER("Learner", NodeFactory.NodeType.Learner, "icons/node/background_learner.png"),
    /** End node of a loop. */
    LOOP_END("Loop End", NodeFactory.NodeType.LoopEnd, "icons/node/background_looper_end.png"),
    /** Start node of a loop. */
    LOOP_START("Loop Start", NodeFactory.NodeType.LoopStart, "icons/node/background_looper_start.png"),
    /** A data manipulating node. */
    MANIPULATOR("Manipulator", NodeFactory.NodeType.Manipulator, "icons/node/background_manipulator.png"),
    /** A metanode. */
    META("Metanode", NodeFactory.NodeType.Meta, "icons/node/background_meta.png"),
    /**
     * A missing node (framework use only).
     * @since 2.7
     */
    MISSING("Missing Type", NodeFactory.NodeType.Missing, "icons/node/background_missing.png"),
    /** All other nodes. */
    OTHER("Other", NodeFactory.NodeType.Other, "icons/node/background_other.png"),
    /** A predicting node. */
    PREDICTOR("Predictor", NodeFactory.NodeType.Predictor, "icons/node/background_predictor.png"),
    /** A node contributing to quick/web form. */
    QUICK_FORM("Quick Form", NodeFactory.NodeType.QuickForm, "icons/node/background_quickform.png"),
    /**
     * End node of a scope.
     * @since 2.8
     */
    SCOPE_END("Scope End", NodeFactory.NodeType.ScopeEnd, "icons/node/background_scope_end.png"),
    /**
     * Start node of a scope.
     * @since 2.8
     */
    SCOPE_START("Scope Start", NodeFactory.NodeType.ScopeStart, "icons/node/background_scope_start.png"),
    /** A data consuming node. */
    SINK("Sink", NodeFactory.NodeType.Sink, "icons/node/background_sink.png"),
    /** A data producing node. */
    SOURCE("Source", NodeFactory.NodeType.Source, "icons/node/background_source.png"),
    /** @since 2.10 */
    SUBNODE("Component", NodeFactory.NodeType.Subnode, "icons/node/background_subnode.png"),
    /** If not specified. */
    UNKNOWN("Unknown", NodeFactory.NodeType.Unknown, "icons/node/background_unknown.png"),
    /** @since 2.10 */
    VIRTUAL_IN("Virtual In Port", NodeFactory.NodeType.VirtualIn, "icons/node/background_virtual_in.png"),
    /** @since 2.10 */
    VIRTUAL_OUT("Virtual Out Port", NodeFactory.NodeType.VirtualOut, "icons/node/background_virtual_out.png"),
    /** A visualizing node. */
    VISUALIZER("Visualizer", NodeFactory.NodeType.Visualizer, "icons/node/background_viewer.png");


    private static final Map<NodeFactory.NodeType, DisplayableNodeType> NODE_TYPE_DISPLAYABLE_MAP = new HashMap<>();

    /**
     * Given the {@code NodeFactory.NodeType}, return the mapped instance of this enum.
     *
     * @param nodeType
     * @return the instance of this enum which wraps the parameter value enum
     */
    public synchronized static DisplayableNodeType getTypeForNodeType(final NodeFactory.NodeType nodeType) {
        if (NODE_TYPE_DISPLAYABLE_MAP.size() == 0) {
            for (final DisplayableNodeType dnt : DisplayableNodeType.values()) {
                NODE_TYPE_DISPLAYABLE_MAP.put(dnt.getNodeType(), dnt);
            }
        }
        return NODE_TYPE_DISPLAYABLE_MAP.get(nodeType);
    }


    private final String m_displayText;
    private final NodeFactory.NodeType m_nodeType;
    private final Image m_nodeBackgroundImage;

    private DisplayableNodeType(final String name, final NodeFactory.NodeType nodeType, final String imagePath) {
        m_displayText = name;
        m_nodeType = nodeType;
        m_nodeBackgroundImage = ImageRepository.getUnscaledImage(KNIMEEditorPlugin.PLUGIN_ID, imagePath);
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
