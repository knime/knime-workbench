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
 *   Nov 6, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.descriptionview.metadata.PlatformSpecificUIisms;

/**
 * A widget to display an image swatch, potentially with a 'delete' icon and listener functionality for that delete
 * click.
 *
 *  TODO abstract swatch commonality to abstract super class
 *
 * @author loki der quaeler
 */
class ImageSwatch extends Canvas {
    private static final String N_ARY_TIMES = "\u2A09";

    private static final Cursor HAND_CURSOR = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_HAND);
    private static final Cursor DEFAULT_CURSOR = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_ARROW);

    private static final int IMAGE_SIZE = 48;


    private final AtomicBoolean m_editMode;
    private Image m_image;
    private Rectangle m_nAryBounds;

    ImageSwatch(final Composite parent, final Listener deleteListener) {
        super(parent, SWT.NONE);

        m_editMode = new AtomicBoolean(false);

        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.CENTER;
        gd.heightHint = IMAGE_SIZE;
        gd.widthHint = IMAGE_SIZE;
        gd.exclude = true;
        setLayoutData(gd);

        addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(final MouseEvent me) {
                if ((m_nAryBounds != null) && m_editMode.get() && m_nAryBounds.contains(me.x, me.y)) {
                    setCursor(HAND_CURSOR);
                } else {
                    setCursor(DEFAULT_CURSOR);
                }
            }
        });
        addMouseListener(new MouseListener() {
            @Override
            public void mouseDoubleClick(final MouseEvent me) { }

            @Override
            public void mouseDown(final MouseEvent me) { }

            @Override
            public void mouseUp(final MouseEvent me) {
                if ((m_nAryBounds != null) && (deleteListener != null) && m_editMode.get()
                                           && m_nAryBounds.contains(me.x, me.y)) {
                    final Event e = new Event();
                    e.widget = ImageSwatch.this;
                    deleteListener.handleEvent(e);
                }
            }
        });
        addPaintListener((e) -> {
            if (m_image == null) {
                return;
            }

            final GC gc = e.gc;

            gc.setAntialias(SWT.ON);

            gc.drawImage(m_image, 0, 0);

            if (m_editMode.get()) {
                gc.setTextAntialias(SWT.ON);
                gc.setFont(AbstractMetaView.BOLD_CONTENT_FONT);

                if (m_nAryBounds == null) {
                    final Point nArySize = gc.textExtent(N_ARY_TIMES);
                    if (PlatformSpecificUIisms.OS_IS_MAC) {
                        nArySize.y = nArySize.x;    // it's an equi-sided X, but some platform fonts give it a bottom inset
                    }

                    final Point size = ImageSwatch.this.getSize();
                    m_nAryBounds =
                        new Rectangle((size.x - 4 - nArySize.x), ((size.y - nArySize.y) / 2), nArySize.x, nArySize.y);
                }

                // TODO what to do about color conflicts? put this in its own rendered circle background?
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
                gc.drawString(N_ARY_TIMES, m_nAryBounds.x, m_nAryBounds.y, true);
            }
        });

        setVisible(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        disposeOfImageIfPresent();
    }

    void setImage(final ImageDescriptor newImage) {
        disposeOfImageIfPresent();

        final GridData gd = (GridData)getLayoutData();
        if (newImage != null) {
            final Image i = newImage.createImage(getDisplay());
            final Rectangle bounds = i.getBounds();
            final int maxDimension = Math.max(bounds.width, bounds.height);
            final float scale = (float)IMAGE_SIZE / (float)maxDimension;
            m_image = resize(i, scale);
            i.dispose();

            gd.exclude = false;
            setVisible(true);
        } else {
            gd.exclude = true;
            setVisible(false);
        }
        setLayoutData(gd);

        redraw();
    }

    void setEditMode(final boolean editMode) {
        if (m_editMode.getAndSet(editMode) != editMode) {
            redraw();
        }
    }

    private void disposeOfImageIfPresent() {
        if (m_image != null) {
            m_image.dispose();
            m_image = null;
        }
    }


    // TODO these three should go in core utilities somewhere
    private static Image resize(final Image image, final float scale) {
        final int w = image.getBounds().width;
        final int h = image.getBounds().height;

        // convert to buffered image
        BufferedImage img = convertToAWT(image.getImageData());

        // resize buffered image
        final int newWidth = Math.round(scale * w);
        final int newHeight = Math.round(scale * h);

        // determine scaling mode for best result: if downsizing, use area averaging, if upsizing, use smooth scaling
        // (usually bilinear).
        final int mode = (scale < 1) ? java.awt.Image.SCALE_AREA_AVERAGING : java.awt.Image.SCALE_SMOOTH;
        final java.awt.Image scaledImage = img.getScaledInstance(newWidth, newHeight, mode);

        // convert the scaled image back to a buffered image
        img = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        img.getGraphics().drawImage(scaledImage, 0, 0, null);

        // reconstruct swt image
        final ImageData imageData = convertToSWT(img);
        return new org.eclipse.swt.graphics.Image(Display.getDefault(), imageData);
    }

    private static BufferedImage convertToAWT(final ImageData data) {
        ColorModel colorModel = null;
        final PaletteData palette = data.palette;
        if (palette.isDirect) {
            colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
            final BufferedImage bufferedImage = new BufferedImage(colorModel,
                colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
            final WritableRaster raster = bufferedImage.getRaster();
            final int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    final int pixel = data.getPixel(x, y);
                    final RGB rgb = palette.getRGB(pixel);
                    pixelArray[0] = rgb.red;
                    pixelArray[1] = rgb.green;
                    pixelArray[2] = rgb.blue;
                    raster.setPixels(x, y, 1, 1, pixelArray);
                }
            }
            return bufferedImage;
        } else {
            final RGB[] rgbs = palette.getRGBs();
            final byte[] red = new byte[rgbs.length];
            final byte[] green = new byte[rgbs.length];
            final byte[] blue = new byte[rgbs.length];
            for (int i = 0; i < rgbs.length; i++) {
                final RGB rgb = rgbs[i];
                red[i] = (byte)rgb.red;
                green[i] = (byte)rgb.green;
                blue[i] = (byte)rgb.blue;
            }
            if (data.transparentPixel != -1) {
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
            } else {
                colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
            }
            final BufferedImage bufferedImage = new BufferedImage(colorModel,
                colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
            final WritableRaster raster = bufferedImage.getRaster();
            final int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    final int pixel = data.getPixel(x, y);
                    pixelArray[0] = pixel;
                    raster.setPixel(x, y, pixelArray);
                }
            }
            return bufferedImage;
        }
    }

    private static ImageData convertToSWT(final BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            final DirectColorModel colorModel = (DirectColorModel)bufferedImage.getColorModel();
            final PaletteData palette =
                new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
            final ImageData data =
                new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
            final WritableRaster raster = bufferedImage.getRaster();
            final int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    final int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
                    data.setPixel(x, y, pixel);
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
        }
        return null;
    }
}
