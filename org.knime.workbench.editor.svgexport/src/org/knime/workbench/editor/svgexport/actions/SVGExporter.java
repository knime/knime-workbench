/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 21, 2013 (Patrick Winter): created
 */
package org.knime.workbench.editor.svgexport.actions;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gmf.runtime.common.ui.util.DisplayUtils;
import org.eclipse.gmf.runtime.draw2d.ui.render.awt.internal.svg.export.GraphicsSVG;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.svgexport.SVGExportException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Exports a workflow as SVG.
 *
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public final class SVGExporter {

    private SVGExporter() {
        // Disable public constructor
    }

    /**
     * Export the given workflow to the given file.
     *
     * This method will search for the corresponding workflow editor to the given workflow manager. It will also
     * schedule the execution of the export method in the UI thread.
     *
     * @param editor The editor of the workflow being exported as SVG.
     * @param file The file to save to
     * @throws SVGExportException Wraps potential I/O or batik exceptions.
     */
    public static void export(final WorkflowEditor editor, final File file) throws SVGExportException {
        try {
            exportInternal(editor, file);
        } catch (IOException | TranscoderException | TransformerException e) {
            throw new SVGExportException(e);
        }
    }

    private static void exportInternal(final WorkflowEditor editor, final File file)
            throws IOException, TranscoderException, TransformerException {
        // Obtain WorkflowRootEditPart, which holds all the nodes
        final GraphicalViewer viewer = editor.getViewer();
        if (viewer == null) {
            NodeLogger.getLogger(SVGExporter.class).debug("Not saving SVG to workflow (viewer is null)");
            return;
        }
        WorkflowRootEditPart part = (WorkflowRootEditPart)viewer.getRootEditPart().getChildren().get(0);
        // export workflow (unfortunately without connections)
        IFigure figure = part.getFigure();
        Rectangle bounds = new Rectangle(figure.getBounds());

        int minX = bounds.x + bounds.width; // default minimum (right border of the figure)
        int minY = bounds.y + bounds.height;

        // clip the bounds and remove unoccupied space at the borders
        @SuppressWarnings("unchecked")
        var children = part.getChildren();
        for (EditPart ep : children) {
            if (ep instanceof AbstractGraphicalEditPart) {
                Rectangle epBounds = ((AbstractGraphicalEditPart)ep).getFigure().getBounds();
                if (minX > epBounds.x) {
                    minX = epBounds.x;
                }
                if (minY > epBounds.y) {
                    minY = epBounds.y;
                }
            }
        }
        bounds.x = minX;
        bounds.y = minY;
        final org.eclipse.draw2d.geometry.Dimension minimumSize =
                new org.eclipse.draw2d.geometry.Dimension(figure.getMinimumSize());
        if (bounds.x > 0) {
            minimumSize.width -= bounds.x;
        }
        if (bounds.y > 0) {
            minimumSize.height -= bounds.y;
        }
        // shift bounds and translate the image so that off-screen parts of the workflow are
        // within the bounding box of the SVG
        GraphicsSVG svgExporter =
            GraphicsSVG.getInstance(new Rectangle(0, 0, bounds.width - bounds.x, bounds.height - bounds.y));
        svgExporter.translate(-bounds.x, -bounds.y);
        svgExporter.getSVGGraphics2D().setSVGCanvasSize(new Dimension(minimumSize.width, minimumSize.height));
        svgExporter.pushState();
        figure.paint(svgExporter);
        // export all connections
        Set<ConnectionContainerEditPart> connections = new HashSet<ConnectionContainerEditPart>();
        //@SuppressWarnings("unchecked")
        //List<EditPart> children = part.getChildren();
        for (EditPart ep : children) {
            if (ep instanceof NodeContainerEditPart) {
                for (ConnectionContainerEditPart c : ((NodeContainerEditPart)ep).getAllConnections()) {
                    connections.add(c);
                }
            }
        }
        for (ConnectionContainerEditPart ep : connections) {
            ep.getFigure().paint(svgExporter);
        }
        Document doc = svgExporter.getDocument();
        doc.replaceChild(svgExporter.getRoot(), doc.getDocumentElement());
        // fix font sizes
        int dpix = DisplayUtils.getDisplay().getDPI().x;
        if (dpix > 72) {
            try {
                XPathExpression xpath = XPathFactory.newInstance().newXPath().compile("//@font-size");
                NodeList fontSizes = (NodeList)xpath.evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < fontSizes.getLength(); i++) {
                    Attr attribute = (Attr)fontSizes.item(i);
                    String value = attribute.getNodeValue();
                    try {
                        double size = Double.parseDouble(value);
                        size = Math.floor(size / dpix * 72.0) * dpix / 72.0;
                        attribute.setNodeValue(Integer.toString((int)size));
                    } catch (NumberFormatException ex) {
                        // ignore it
                    }
                }
            } catch (XPathExpressionException ex) {
                // ignore
            }
        }

        // Serialize to String first, otherwise special characters don't get escaped (see AP-22522).
        // The `SVGTranscoder` serializes internally if it gets a DOM `Document` instead of a `Reader`.
        final var stringWriter = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(stringWriter));

        final var transcoder = new SVGTranscoder();
        transcoder.addTranscodingHint(SVGTranscoder.KEY_XML_DECLARATION, "<?xml version=\"1.1\" encoding=\"UTF-8\"?>");
        try (final Writer fileOut =  Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            transcoder.transcode(new TranscoderInput(stringWriter.toString()), new TranscoderOutput(fileOut));
        }
    }
}
