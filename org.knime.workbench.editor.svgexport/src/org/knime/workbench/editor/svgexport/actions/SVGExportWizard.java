/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.CachedImageHandlerBase64Encoder;
import org.apache.batik.svggen.CachedImageHandlerPNGEncoder;
import org.apache.batik.svggen.GenericImageHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.dialogs.ExportWizard;
import org.knime.workbench.editor.svgexport.BatikGraphics2DWrapper;
import org.knime.workbench.editor.svgexport.figures.AnnotationSVGFigure;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.AbstractWorkflowEditPart;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.SubworkflowEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * This wizard exports KNIME workflows and workflow groups if workflows are
 * selected which are in different workflow groups.
 *
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 * @author Andreas Burger
 */
@SuppressWarnings("restriction")
public class SVGExportWizard extends ExportWizard implements IExportWizard {

    private List<ConnectionContainerEditPart> drawnConnections =
            new LinkedList<ConnectionContainerEditPart>();

    // private static final NodeLogger LOGGER =
    // NodeLogger.getLogger(WorkflowExportWizard.class);

    private ExportToSVGPage m_page;

    private ISelection m_selection;

    /**
     * Constructor.
     */
    public SVGExportWizard() {
        super();
        setWindowTitle("Export SVG-Image");
        // setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        m_page = new ExportToSVGPage(m_selection);
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
     * @return If finished successfully
     */
    @Override
    public boolean performFinish() {

        String fileDestination = m_page.getFileDestination();

        if (fileDestination.isEmpty()) {
            m_page.setErrorMessage("No file specified!");
            return false;
        }

        if (fileDestination != null && fileDestination.trim().length() > 0) {
            if (fileDestination.length() < 5
                    || fileDestination.lastIndexOf('.') < fileDestination
                            .length() - 4) {
                fileDestination += ".svg";
            }

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

        DOMImplementation domImpl =
                GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        Document document = domImpl.createDocument(svgNS, "svg", null);
        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);

        ctx.setComment("SVG-export of a KNIME-workflow generated by Batik SVG-Generator");
        GenericImageHandler iHandler;

        try {
            if (m_page.embedData()) {
                iHandler = new CachedImageHandlerBase64Encoder();
            } else {
                iHandler =
                        new CachedImageHandlerPNGEncoder(outFile.getParent(),
                                outFile.getParent());
            }
            ctx.setGenericImageHandler(iHandler);

        } catch (Exception e1) {
            e1.printStackTrace();
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

        // Obtain the bounds of the workflow by searching for the biggest
        // existing x and y values of the painted Figures. Add nodewidth + 10px
        // for good measure.
        // Also obtain the minimum x and y values used in the Workflow and pass
        // them on to the wrapper, since Rectangles with negative x and y can't
        // be created.

        int xCoords = 0, xMin = 0, xMax = 0, minWidth = 0, maxWidth = 0;
        int yCoords = 0, yMin = 0, yMax = 0, minHeight = 0, maxHeight = 0;

        for (Object node : part.getChildren()) {
            IFigure figure = ((AbstractWorkflowEditPart)node).getFigure();
            xCoords = figure.getBounds().x;
            yCoords = figure.getBounds().y;
            if (xCoords >= xMax) {
                xMax = xCoords;
                maxWidth = figure.getBounds().width;
            }
            if (xCoords <= xMin) {
                xMin = xCoords;
                minWidth = figure.getBounds().width;
            }
            if (yCoords >= yMax) {
                yMax = yCoords;
                maxHeight = figure.getBounds().height;
            }
            if (yCoords <= yMin) {
                yMin = yCoords;
                minHeight = figure.getBounds().height;
            }
        }

        int xOffset = Math.abs(xMin) + minWidth;
        int yOffset = Math.abs(yMin) + minHeight;
        int xBounds = xMax + xOffset + maxWidth;
        int yBounds = yMax + yOffset + maxHeight;

        Rectangle bounds = new Rectangle(0, 0, xBounds, yBounds);

        BatikGraphics2DWrapper graphics =
                new BatikGraphics2DWrapper(PlatformUI.getWorkbench()
                        .getDisplay(), ctx, false, xOffset, yOffset, bounds);

        // First, obtain the figure of the RootEditPart, its children are the
        // figures of the nodes. Now each child paints itself to the wrapped
        // generator and resets the foreground colour afterwards. This approach
        // is necessary to prevent drawing each node twice and colour-errors.

        for (Object node : part.getChildren()) {
            if (node instanceof AnnotationEditPart) {
                if (m_page.includeAnnotations()) {
                    AnnotationSVGFigure figure =
                            new AnnotationSVGFigure(PlatformUI.getWorkbench()
                                    .getDisplay(), (AnnotationEditPart)node);
                    figure.paint(graphics);
                    graphics.resetColors();
                }
            } else {
                ((AbstractWorkflowEditPart)node).getFigure().paint(graphics);
                graphics.resetColors();
            }
        }

        // Now we need to paint the connections. In order to do this we get all
        // nodes to which the user may add connections and draw these
        // connections while keeping track of those already drawn.
        for (Object child : part.getChildren()) {
            if (child instanceof NodeContainerEditPart) {
                for (ConnectionContainerEditPart connection : ((NodeContainerEditPart)child)
                        .getAllConnections()) {
                    if (!drawnConnections.contains(connection)) {
                        connection.getFigure().paint(graphics);
                        graphics.resetColors();
                        drawnConnections.add(connection);
                    }

                }
            }
            if (child instanceof SubworkflowEditPart) {
                for (ConnectionContainerEditPart connection : ((SubworkflowEditPart)child)
                        .getAllConnections()) {
                    if (!drawnConnections.contains(connection)) {
                        connection.getFigure().paint(graphics);
                        graphics.resetColors();
                        drawnConnections.add(connection);
                    }
                }
            }
        }

        // Use CSS-Style attributes. Currently disabled (= always false)
        boolean useCSS = m_page.useCSS();

        // Finally, create a FileWriter and stream the SVG into it.
        FileWriter out;
        try {
            out = new FileWriter(outFile);
            graphics.stream(out, useCSS);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

}
