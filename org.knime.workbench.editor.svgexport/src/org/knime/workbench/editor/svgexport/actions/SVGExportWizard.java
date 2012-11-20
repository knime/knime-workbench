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
 * -------------------------------------------------------------------
 *
 * History
 *   02.07.2006 (sieb): created
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
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gmf.runtime.common.ui.util.DisplayUtils;
import org.eclipse.gmf.runtime.draw2d.ui.render.awt.internal.svg.export.GraphicsSVG;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.ExportWizard;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ExportToFilePage;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This wizard exports KNIME workflows as SVG images.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com AG, Zurich, Switzerland
 * @author Andreas Burger
 */
@SuppressWarnings("restriction")
public class SVGExportWizard extends ExportWizard implements IExportWizard {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SVGExportWizard.class);

    private final ExportToFilePage m_page =
            new ExportToFilePage("Export KNIME workflows",
                    "Exports a KNIME workflow as SVG.");

    /**
     * Creates a new export wizard.
     */
    public SVGExportWizard() {
        super();
        setWindowTitle("Export workflow as SVG");
        m_page.addFileExtensionFilter("*.svg", "Scalable vector graphics");
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        addPage(m_page);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_page.isPageComplete();
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     *
     * @return <code>true</code> if finished successfully, <code>false</code>
     *         otherwise
     */
    @Override
    public boolean performFinish() {
        if (!(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .getActiveEditor() instanceof WorkflowEditor)) {
            MessageBox mb = new MessageBox(Display.getDefault().getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Error during export");
            mb.setMessage("Current editor is not a KNIME workflow.");
            mb.open();
            return false;
        }

        try {
            return export();
        } catch (Exception ex) {
            LOGGER.error("Error during SVG export: " + ex.getMessage(), ex);
            MessageBox mb =
                    new MessageBox(Display.getDefault().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setText("Error during export");
            mb.setMessage("Could not export workflow as SVG: "
                    + ex.getMessage());
            mb.open();
            return false;
        }
    }

    private boolean export() throws IOException, TranscoderException {
        String fileDestination = m_page.getFile();

        if (fileDestination.isEmpty()) {
            m_page.setErrorMessage("No file specified!");
            return false;
        }

        if ((fileDestination.trim().length() > 0)
                && ((fileDestination.length() < 5) || (fileDestination
                        .lastIndexOf('.') < fileDestination.length() - 4))) {
            fileDestination += ".svg";
        }

        File outFile = new File(fileDestination);

        if (outFile.exists()) {
            // if it exists we have to check if we can write to:
            if (!outFile.canWrite() || outFile.isDirectory()) {
                // display error
                m_page.setErrorMessage("Cannot write to specified file");
                return false;
            }
            boolean overwrite =
                    MessageDialog.openQuestion(getShell(),
                            "File already exists...",
                            "File already exists.\nDo you want to overwrite the "
                                    + "specified file ?");
            if (!overwrite) {
                return false;
            }
        }

        // Obtain currently active workflow editor
        WorkflowEditor editor =
                (WorkflowEditor)PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage()
                        .getActiveEditor();

        // Obtain WorkflowRootEditPart, which holds all the nodes
        WorkflowRootEditPart part =
                (WorkflowRootEditPart)editor.getViewer().getRootEditPart()
                        .getChildren().get(0);

        // export workflow (unfortunately without connections)
        IFigure figure = part.getFigure();
        Rectangle bounds = figure.getBounds();
        // shift bounds and translate the image so that off-screen parts of the workflow
        // are within the bounding box of the SVG
        GraphicsSVG svgExporter = GraphicsSVG.getInstance(new Rectangle(0, 0, bounds.width - bounds.x, bounds.height - bounds.y));
        svgExporter.translate(-bounds.x, -bounds.y);
        svgExporter.getSVGGraphics2D().setSVGCanvasSize(new Dimension(bounds.width, bounds.height));
        svgExporter.pushState();
        figure.paint(svgExporter);

        // export all connections
        Set<ConnectionContainerEditPart> connections =
                new HashSet<ConnectionContainerEditPart>();
        @SuppressWarnings("unchecked")
        List<EditPart> children = part.getChildren();
        for (EditPart ep : children) {
            if (ep instanceof NodeContainerEditPart) {
                for (ConnectionContainerEditPart c : ((NodeContainerEditPart)ep)
                        .getAllConnections()) {
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
                XPathExpression xpath =
                        XPathFactory.newInstance().newXPath()
                                .compile("//@font-size");
                NodeList fontSizes =
                        (NodeList)xpath.evaluate(doc, XPathConstants.NODESET);
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
                LOGGER.error("XPath error while exporting SVG", ex);
            }
        }

        TranscoderInput in = new TranscoderInput(doc);
        transcoder.transcode(in, out);
        fileOut.close();

        return true;
    }
}
