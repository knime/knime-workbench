/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 21, 2013 (Patrick Winter): created
 */
package org.knime.workbench.editor.svgexport.actions;

import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gmf.runtime.common.ui.util.DisplayUtils;
import org.eclipse.gmf.runtime.draw2d.ui.render.awt.internal.svg.export.GraphicsSVG;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.svgexport.SVGExportException;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Exports a workflow as SVG.
 *
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
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
     * @param wfm The workflow manager of the workflow that should be exported
     * @param file The file to save to
     * @throws SVGExportException If the export failed (e.g. IO or XML problems)
     */
    public static void exportDuringSave(final WorkflowManager wfm, final File file) throws SVGExportException {
        // Exception reference in case an exception occurs
        final SVGExportException[] exception = new SVGExportException[1];
        // UI job that executes the actual export
        Job job = new UIJob("SVG export") {
            /**
             * {@inheritDoc}
             */
            @Override
            public IStatus runInUIThread(final IProgressMonitor monitor) {
                try {
                    // Remember the correct editor in this variable if found
                    WorkflowEditor correctEditor = null;
                    // Get reference for all open editors
                    IEditorReference[] editorRefs =
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
                    for (int i = 0; i < editorRefs.length; i++) {
                        // Only look at loaded editors, do not load others
                        IEditorPart editor = editorRefs[i].getEditor(false);
                        if (editor != null) {
                            WorkflowEditor workflowEditor = (WorkflowEditor)editor;
                            // Check if the workflow manager is the same as the one given
                            if (workflowEditor.getWorkflowManager() == wfm) {
                                // Remember editor and stop searching
                                correctEditor = workflowEditor;
                                break;
                            }
                        }
                    }
                    if (correctEditor != null) {
                        // Create SVG
                        export(correctEditor, file);
                    } else {
                        // No editor to the workflow was found
                        exception[0] = new SVGExportException(SVGExportException.NO_EDITOR_OPEN_MSG);
                    }
                } catch (Exception e) {
                    exception[0] = new SVGExportException(e);
                }
                return Status.OK_STATUS;
            }
        };
        // Execute job and wait for its execution
        job.schedule();
        try {
            job.join();
        } catch (InterruptedException e) {
            // If interrupted continue execution
        }
        // Throw exception if one occured
        if (exception[0] != null) {
            throw exception[0];
        }
    }

    /**
     * Export the content of the workflow editor as SVG image.
     *
     * <b>Must be run in the UI thread.</b>
     *
     * @param editor The workflow editor that will be exported
     * @param outFile The SVG file to save to
     * @throws IOException If an I/O error occurs
     * @throws TranscoderException If a transcoding error occurs
     */
    @SuppressWarnings("restriction")
    static void export(final WorkflowEditor editor, final File outFile) throws IOException, TranscoderException {
        // Obtain WorkflowRootEditPart, which holds all the nodes
        WorkflowRootEditPart part = (WorkflowRootEditPart)editor.getViewer().getRootEditPart().getChildren().get(0);
        // export workflow (unfortunately without connections)
        IFigure figure = part.getFigure();
        Rectangle bounds = new Rectangle(figure.getBounds());

        int minX = bounds.x + bounds.width; // default minimum (right border of the figure)
        int minY = bounds.y + bounds.height;

        // clip the bounds and remove unoccupied space at the borders
        @SuppressWarnings("unchecked")
        List<EditPart> children = part.getChildren();
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
        SVGTranscoder transcoder = new SVGTranscoder();
        Writer fileOut = new BufferedWriter(new FileWriter(outFile));
        TranscoderOutput out = new TranscoderOutput(fileOut);
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
        TranscoderInput in = new TranscoderInput(doc);
        transcoder.transcode(in, out);
        fileOut.close();
    }

}
