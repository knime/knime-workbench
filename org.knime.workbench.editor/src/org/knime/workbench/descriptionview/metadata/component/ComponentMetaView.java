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
 *   Oct 31, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.XMLAbstractTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FileUtils;
import org.eclipse.gmf.runtime.draw2d.ui.render.RenderInfo;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.descriptionview.FallbackBrowser;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.repository.util.DynamicNodeDescriptionCreator;
import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;
import org.w3c.dom.Element;

/**
 * This is the view that supports component metadata viewing and editing when the component is open in its own
 * workflow editor.
 *
 * @author loki der quaeler
 */
public class ComponentMetaView extends AbstractMetaView {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ComponentMetaView.class);

    private static final int[] DROP_ZONE_DASH = { 5, 4 };

    private static final String NO_SELECTION_COLOR_COMBO_TEXT = "Select color";


    private Browser m_browser;

    private FallbackBrowser m_text;

    private boolean m_isFallback;

    private SubNodeContainer m_currentSubNodeContainer;

    private Composite m_editUpperComposite;
    private ImageSwatch m_editImageSwatch;
    private Canvas m_iconDropZone;
    private Label m_dropZoneLabel1;
    private Label m_dropZoneLabel2;
    private ColorSwatch m_editColorSwatch;
    private ComboViewer m_colorComboViewer;

    private Composite m_displayUpperComposite;
    private ImageSwatch m_displayImageSwatch;
    private ColorSwatch m_displayColorSwatch;

    /**
     * @param parent
     */
    public ComponentMetaView(final Composite parent) {
        super(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean populateUpperSection(final Composite upperComposite) {
        GridLayout gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 4;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        upperComposite.setLayout(gl);


        final Label l = new Label(upperComposite, SWT.NONE);
        l.setText("Component Icon");
        l.setFont(BOLD_CONTENT_FONT);
        l.setForeground(SECTION_LABEL_TEXT_COLOR);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);


        m_editUpperComposite = new Composite(upperComposite, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 3;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        m_editUpperComposite.setLayout(gl);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        m_editUpperComposite.setLayoutData(gd);

        Composite c = new Composite(m_editUpperComposite, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        c.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        gl.horizontalSpacing = 9;
        c.setLayout(gl);

        m_editImageSwatch = new ImageSwatch(c, (e) -> {
            m_editImageSwatch.setImage((Image)null, true);
            upperComposite.getParent().layout(true, true);
            // TODO communicate state to meta model facilitator
        });
        m_editImageSwatch.setEditMode(true);

        m_iconDropZone = new Canvas(c, SWT.NONE);
        // SWT.BORDER_DASH does nothing... SWT!
        m_iconDropZone.addPaintListener((e) -> {
            final GC gc = e.gc;

            gc.setAntialias(SWT.ON);
            gc.setLineDash(DROP_ZONE_DASH);
            gc.setForeground(SECTION_LABEL_TEXT_COLOR);
            final Point size = m_iconDropZone.getSize();
            gc.drawRectangle(0, 0, (size.x - 1), (size.y - 1));
        });
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.heightHint = 48;
        m_iconDropZone.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginLeft = 6;
        gl.verticalSpacing = 6;
        m_iconDropZone.setLayout(gl);
        // -----
        m_dropZoneLabel1 = new Label(m_iconDropZone, SWT.NONE);
        m_dropZoneLabel1.setText("Drag and Drop a square image file");
        FontData[] baseFD = BOLD_CONTENT_FONT.getFontData();
        FontData smallerFD = new FontData(baseFD[0].getName(), (baseFD[0].getHeight() - 1), baseFD[0].getStyle());
        Font smallerFont = new Font(PlatformUI.getWorkbench().getDisplay(), smallerFD);
        m_dropZoneLabel1.setFont(smallerFont);
        m_dropZoneLabel1.setForeground(SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        m_dropZoneLabel1.setLayoutData(gd);
        // -----
        m_dropZoneLabel2 = new Label(m_iconDropZone, SWT.NONE);
        m_dropZoneLabel2.setText("Can be a SVG, or a PNG 32x32 or larger");
        baseFD = VALUE_DISPLAY_FONT.getFontData();
        smallerFD = new FontData(baseFD[0].getName(), (baseFD[0].getHeight() - 3), baseFD[0].getStyle());
        smallerFont = new Font(PlatformUI.getWorkbench().getDisplay(), smallerFD);
        m_dropZoneLabel2.setFont(smallerFont);
        m_dropZoneLabel2.setForeground(TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        m_dropZoneLabel2.setLayoutData(gd);

        c = new Composite(m_editUpperComposite, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        c.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        gl.horizontalSpacing = 9;
        c.setLayout(gl);

        m_editColorSwatch = new ColorSwatch(c, (e) -> {
            m_editColorSwatch.setColor(null);
            upperComposite.getParent().layout(true, true);
            m_colorComboViewer.setInput(Arrays.asList(NO_SELECTION_COLOR_COMBO_TEXT, "Blue", "Green", "Red")/**TODO**/);
            m_colorComboViewer.setSelection(new StructuredSelection(m_colorComboViewer.getElementAt(0)), true);
            m_colorComboViewer.getCombo().getParent().layout(true, true);
            // TODO communicate state to meta model facilitator
        });

        m_colorComboViewer = new ComboViewer(c, SWT.READ_ONLY);
        m_colorComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_colorComboViewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(final Object element) {
                // TODO maybe generate a color swatch?
                return null;
            }

            @Override
            public String getText(final Object o) {
                // TODO if we use some sort of object containing swatch color and text
                return (String)o;
            }
        });
        m_colorComboViewer.setInput(Arrays.asList(NO_SELECTION_COLOR_COMBO_TEXT, "Blue", "Green", "Red")/**TODO**/);
        m_colorComboViewer.setSelection(new StructuredSelection(m_colorComboViewer.getElementAt(0)), true);

        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.CENTER;
        m_colorComboViewer.getCombo().setLayoutData(gd);
        m_colorComboViewer.addPostSelectionChangedListener((event) -> {
            if (!m_colorComboViewer.getStructuredSelection().getFirstElement().equals(NO_SELECTION_COLOR_COMBO_TEXT)) {
                // TODO update swatch color component once we have the object sorted out
                m_colorComboViewer.remove(NO_SELECTION_COLOR_COMBO_TEXT);
                final int r = (int)(Math.random() * 255.0);
                final int g = (int)(Math.random() * 255.0);
                final int b = (int)(Math.random() * 255.0);
                m_editColorSwatch.setColor(new RGB(r, g, b));

                m_editColorSwatch.setEditMode(m_colorComboViewer.getCombo().getSelectionIndex() == 0);

                upperComposite.getParent().layout(true, true);
            }
        });


        m_displayUpperComposite = new Composite(upperComposite, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 3;
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        m_displayUpperComposite.setLayout(gl);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        m_displayUpperComposite.setLayoutData(gd);

        m_displayImageSwatch = new ImageSwatch(m_displayUpperComposite, null);
        m_displayColorSwatch = new ColorSwatch(m_displayUpperComposite, null);


        // TODO m_edit is the one hidden
        SWTUtilities.spaceReclaimingSetVisible(m_displayUpperComposite, false);


        final DropTarget dropTarget = new DropTarget(m_iconDropZone, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
        dropTarget.setTransfer(FileTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(final DropTargetEvent event) {
                event.detail = DND.DROP_COPY;
            }

            @Override
            public void dragLeave(final DropTargetEvent event) { }

            @Override
            public void dragOperationChanged(final DropTargetEvent event) { }

            @Override
            public void dragOver(final DropTargetEvent event) { }

            @Override
            public void drop(final DropTargetEvent event) {
                final String[] files = (String[])event.data;

                // only allow single file drop
                if (files.length == 1) {
                    final String lcFilename = files[0].toLowerCase();

                    if (lcFilename.endsWith("png")) {
                        pngFileWasDropped(files[0]);
                    } else if (lcFilename.endsWith("svg")) {
                        svgFileWasDropped(files[0]);
                    }
                }
            }

            @Override
            public void dropAccept(final DropTargetEvent event) { }
        });

        return true;
    }

    private void pngFileWasDropped(final String filename) {
        final String urlString = "file:" + filename;

        try {
            final URL url = new URL(urlString);
            final ImageDescriptor id = ImageDescriptor.createFromURL(url);

            m_displayImageSwatch.setImage(id);
            m_editImageSwatch.setImage(id);

            if (inEditMode()) {
                m_editImageSwatch.getParent().getParent().layout(true, true);
            } else {
                m_displayImageSwatch.getParent().getParent().layout(true, true);
            }
            // TODO update metadate when sorted out
        } catch (final Exception e) {
            LOGGER.error("Unable to create and load from url [" + urlString + "].", e);
        }
    }

//    @SuppressWarnings("restriction")        // GMF internal transcoder usage
    private void svgFileWasDropped(final String filename) {
        try {
            // doesn't produce an exception, but produces an 8x8 BI of nothing-ness, no matter what
//            try {
//                FileInputStream fis = new FileInputStream(filename);
//                final byte[] bytes = IOUtils.toByteArray(fis);
//                final SVGImage svgImage = new SVGImage(bytes, new RenderedImageKey(new SVGRenderInfo()));
//                final BufferedImage bi = svgImage.getBufferedImage();
//
//                final ImageData id = convertToSWT(bi);
//                final Image i = new Image(getDisplay(), id);
//
//                m_displayImageSwatch.setImage(i, false);
//                m_editImageSwatch.setImage(i, true);
//                fis.close();
//                return;
//
//            } catch (Exception bad) {
//                LOGGER.error("bad ", bad);
//            }
            String css = "svg {" +
                    "shape-rendering: geometricPrecision;" +
                    "text-rendering:  geometricPrecision;" +
                    "color-rendering: optimizeQuality;" +
                    "image-rendering: optimizeQuality;" +
                    "}";
            File cssFile = File.createTempFile("batik-default-override-", ".css");
            FileUtils.writeStringToFile(cssFile, css);

            final TranscodingHints transcoderHints = new TranscodingHints();
            transcoderHints.put(XMLAbstractTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
            transcoderHints.put(XMLAbstractTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
            transcoderHints.put(XMLAbstractTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
            transcoderHints.put(XMLAbstractTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
            transcoderHints.put(SVGAbstractTranscoder.KEY_USER_STYLESHEET_URI, cssFile.toURI().toString());

            try (final FileInputStream fis = new FileInputStream(filename)) {
//                final SWTImageTranscoder imageTranscoder = new SWTImageTranscoder();
                final TranscoderInput input = new TranscoderInput(fis);
                final ArrayList<BufferedImage> renderedImages = new ArrayList<>();
                final PNGTranscoder imageTranscoder = new PNGTranscoder() {
//                    @Override
//                    public BufferedImage createImage(final int w, final int h) {
//                        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
//                    }
//                    @Override
//                    public void writeImage(final BufferedImage image, final TranscoderOutput out)
//                            throws TranscoderException {
//                        renderedImages.add(image);
//                    }
                };
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final TranscoderOutput output = new TranscoderOutput(baos);

                imageTranscoder.setTranscodingHints(transcoderHints);
// This line ends up throwing a class cast exception from the bowels of Batik - in BridgeContext.dispose()
//      which claims:
// java.lang.ClassCastException: org.apache.batik.dom.svg.SVGOMDocument cannot be cast to org.w3c.dom.events.EventTarget
//      which is nonsense as the API doc show:
//              https://xmlgraphics.apache.org/batik/javadoc/org/apache/batik/anim/dom/SVGOMDocument.html
                imageTranscoder.transcode(input, output);

                final ImageData id = convertToSWT(renderedImages.get(0));
                final Image i = new Image(getDisplay(), id);

              m_displayImageSwatch.setImage(i, false);
              m_editImageSwatch.setImage(i, true);

//                m_displayImageSwatch.setImage(imageTranscoder.getSWTImage());
//                m_editImageSwatch.setImage(imageTranscoder.getSWTImage());

                if (inEditMode()) {
                    m_editImageSwatch.getParent().getParent().layout(true, true);
                } else {
                    m_displayImageSwatch.getParent().getParent().layout(true, true);
                }
                // TODO update metadate when sorted out
            }
        } catch (final Exception e) {
            LOGGER.error("Caught exception attempting to read in SVG file.", e);
        }
    }

    private static ImageData convertToSWT(final BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            DirectColorModel colorModel = (DirectColorModel)bufferedImage.getColorModel();
            PaletteData palette = new PaletteData(
                    colorModel.getRedMask(),
                    colorModel.getGreenMask(),
                    colorModel.getBlueMask());
            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
                    colorModel.getPixelSize(), palette);
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                    data.setPixel(x, y, pixel);
                    if (colorModel.hasAlpha()) {
                        data.setAlpha(x, y, (rgb >> 24) & 0xFF);
                    }
                }
            }
            return data;
        }
        else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel)bufferedImage.getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
                    colorModel.getPixelSize(), palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        }
        else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
            ComponentColorModel colorModel = (ComponentColorModel)bufferedImage.getColorModel();
            //ASSUMES: 3 BYTE BGR IMAGE TYPE
            PaletteData palette = new PaletteData(0x0000FF, 0x00FF00,0xFF0000);
            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
                    colorModel.getPixelSize(), palette);
            //This is valid because we are using a 3-byte Data model with no transparent pixels
            data.transparentPixel = -1;
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
                    data.setPixel(x, y, pixel);
                }
            }
            return data;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean populateLowerSection(final Composite lowerComposite) {
        if (m_browser == null) {
            lowerComposite.setLayout(new FillLayout());

            try {
                m_text = null;
                m_browser = new Browser(lowerComposite, SWT.NONE);
                m_browser.setText("");
                m_isFallback = false;
            } catch (final SWTError e) {
                LOGGER.warn("No html browser for node description available.", e);
                m_browser = null;
                m_text = new FallbackBrowser(lowerComposite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
                m_isFallback = true;
            }
        }

        return true;
    }

    @Override
    protected void updateDisplay() {
        super.updateDisplay();

        if (inEditMode()) {
            // TODO m_edit visible, m_display hide

            if (m_browser != null) {
                m_browser.setVisible(false);
            } else if (m_isFallback) {
                m_text.setVisible(false);
            }
        } else {
            // TODO m_edit hide, m_display visible

            final StringBuilder content = new StringBuilder(DynamicNodeDescriptionCreator.instance().getHeader());
            final Element portDOM = m_currentSubNodeContainer.getXMLDescriptionForPorts();

            try {
                content.append(NodeFactoryHTMLCreator.instance.readFullDescription(portDOM));
                content.append("</body></html>");

                if (m_browser != null) {
                    m_browser.getDisplay().asyncExec(() -> {
                        if (!m_browser.isDisposed()) {
                            m_browser.setText(content.toString());
                            m_browser.setVisible(true);
                        }
                    });
                } else if (m_isFallback) {
                    m_text.getDisplay().asyncExec(() -> {
                        m_text.setText(content.toString());
                        m_text.setVisible(true);
                    });
                }
            } catch (final Exception e) {
                LOGGER.error("Exception attempting to generate components port description display.", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IStructuredSelection selection) {
        final Object o = selection.getFirstElement();

        if (!(o instanceof SubNodeContainer)) {
            LOGGER.error("We were expecting an instance of SubNodeContainer but instead got " + o);

            return;
        }

        final SubNodeContainer subNodeContainer = (SubNodeContainer)o;
        if (Objects.equals(m_currentSubNodeContainer, subNodeContainer)) {
            return;
        }

        m_modelFacilitator = new ComponentMetadataModelFacilitator(subNodeContainer);
        m_currentSubNodeContainer = subNodeContainer;

        // TODO waiting on API to fetch metadata (AP-12986)
        m_currentAssetName = subNodeContainer.getName();    // TODO getMetadataTitle or similar...
        currentAssetNameHasChanged();

        // Is there ever a case where it cannot be?
        m_metadataCanBeEdited.set(true);
        configureFloatingHeaderBarButtons();

        getDisplay().asyncExec(() -> {
            if (!isDisposed()) {
                updateDisplay();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void completeSave() {
        ((ComponentMetadataModelFacilitator)m_modelFacilitator).storeMetadataInComponent();
    }


    private class SVGRenderInfo implements RenderInfo {

        /**
         * {@inheritDoc}
         */
        @Override
        public int getWidth() {
            return 48;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getHeight() {
            return 48;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RGB getBackgroundColor() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RGB getForegroundColor() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean shouldMaintainAspectRatio() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean shouldAntiAlias() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValues(final int width, final int height, final boolean maintainAspectRatio, final boolean antialias, final RGB background,
            final RGB foreground) {
            // NOOP
        }

    }
}
