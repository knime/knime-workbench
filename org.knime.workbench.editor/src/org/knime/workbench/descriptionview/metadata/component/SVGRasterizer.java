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

import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.XMLAbstractTranscoder;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.commons.io.FileUtils;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/**
 * The class wraps the functionality of digesting an SVG file and producting a rasterized form (for icons, and
 *  such.)
 *
 * @author loki der quaeler
 */
class SVGRasterizer {
    private static final String SVG_CSS = "svg {"
                                                + "shape-rendering: geometricPrecision;"
                                                + "text-rendering:  geometricPrecision;"
                                                + "color-rendering: optimizeQuality;"
                                                + "image-rendering: optimizeQuality;"
                                            + "}";

    /**
     * Given the SVG file at the specified path, convert it into a rasterized image and return an instance of
     * {@code ImageData} wrapping it.
     *
     * @param filename the full path to the SVG file
     * @return the {@link ImageData} of the rasterized content of the SVG file
     * @throws IOException
     * @throws TranscoderException
     */
    public static ImageData rasterizeImageFromSVGFile(final String filename)
                throws IOException, TranscoderException {
        final File cssFile = File.createTempFile("batik-default-override-", ".css");
        FileUtils.writeStringToFile(cssFile, SVG_CSS, Charset.forName("UTF-8"));

        final TranscodingHints transcoderHints = new TranscodingHints();
        transcoderHints.put(XMLAbstractTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
        transcoderHints.put(XMLAbstractTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
        transcoderHints.put(XMLAbstractTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
        transcoderHints.put(XMLAbstractTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
        transcoderHints.put(SVGAbstractTranscoder.KEY_USER_STYLESHEET_URI, cssFile.toURI().toString());

        try (final FileInputStream fis = new FileInputStream(filename)) {
            final TranscoderInput input = new TranscoderInput(fis);
            final ArrayList<BufferedImage> renderedImages = new ArrayList<>();
            final ImageTranscoder imageTranscoder = new ImageTranscoder() {
                @Override
                public BufferedImage createImage(final int w, final int h) {
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }

                @Override
                public void writeImage(final BufferedImage image, final TranscoderOutput out)
                    throws TranscoderException {
                    renderedImages.add(image);
                }

                @Override
                protected BridgeContext createBridgeContext() {
                    return new LinkerCajolingBridgeContext(userAgent);
                }
            };

            imageTranscoder.setTranscodingHints(transcoderHints);
            imageTranscoder.transcode(input, null);

            return convertBufferedImageToImageData(renderedImages.get(0));
        }
    }

    private static ImageData convertBufferedImageToImageData(final BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            final DirectColorModel colorModel = (DirectColorModel)bufferedImage.getColorModel();
            final PaletteData palette =
                new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
            final ImageData data =
                new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    final int rgb = bufferedImage.getRGB(x, y);
                    final int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
                    data.setPixel(x, y, pixel);
                    if (colorModel.hasAlpha()) {
                        data.setAlpha(x, y, (rgb >> 24) & 0xFF);
                    }
                }
            }
            return data;
        } else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            final IndexColorModel colorModel = (IndexColorModel)bufferedImage.getColorModel();
            final int size = colorModel.getMapSize();
            final byte[] reds = new byte[size];
            final byte[] greens = new byte[size];
            final byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            final RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
            }
            final PaletteData palette = new PaletteData(rgbs);
            final ImageData data =
                new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            final WritableRaster raster = bufferedImage.getRaster();
            final int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        } else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
            final ComponentColorModel colorModel = (ComponentColorModel)bufferedImage.getColorModel();
            //ASSUMES: 3 BYTE BGR IMAGE TYPE
            final PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
            final ImageData data =
                new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
            //This is valid because we are using a 3-byte Data model with no transparent pixels
            data.transparentPixel = -1;
            final WritableRaster raster = bufferedImage.getRaster();
            final int[] pixelArray = new int[3];
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
}
