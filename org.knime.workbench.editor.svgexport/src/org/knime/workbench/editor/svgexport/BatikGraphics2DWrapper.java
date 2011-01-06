package org.knime.workbench.editor.svgexport;

import java.awt.BasicStroke;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.io.Writer;
import java.util.Stack;

import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Document;

/**
 * Wrapper for an Apache Batik SVGGraphics2D instance. Provides substitutes for most AWT Methods.
 *
 * @author Andreas Burger
 *
 */
public class BatikGraphics2DWrapper extends Graphics {


	private SVGGraphics2D					batikGraphicsSVG;

	private Display 						display;

	private float 							_SWTLineWidth = 10;

	private int 							_SWTLineStyle = org.eclipse.draw2d.Graphics.LINE_SOLID;

	private Font 							_SWTFont;

	private org.eclipse.swt.graphics.Color 	_SWTForegroundColor = new Color(display, 0, 0, 0) ;

	private org.eclipse.swt.graphics.Color 	_SWTBackgroundColor = new Color(display, 0, 0, 0);

	private boolean 						_XORMode;

	private Rectangle						visibleArea;
	
	private Stack<State>					_stack = new Stack<State>();
	
	private class State{
		
		float _lineWidth;
		int _lineStyle;
		Font _font;
		Color _bg;
		Color _fg;
		boolean _XOR;
		Rectangle _visibleArea;
		
		public State(){
			_lineWidth = _SWTLineWidth;
			_lineStyle = _SWTLineStyle;
			_font = _SWTFont;
			_bg = _SWTBackgroundColor;
			_fg = _SWTForegroundColor;
			_XOR = _XORMode;
			_visibleArea = visibleArea;
		}
		
		public void restore(){
			_SWTLineWidth  = _lineWidth;
			_SWTLineStyle  = _lineStyle;
			_SWTFont  = _font;
			_SWTBackgroundColor  = _bg;
			_SWTForegroundColor  = _fg;
			_XORMode  = _XOR;
			visibleArea = _visibleArea;
		}
		
	}

	/**
	 *
	 * @param display display-instance required for several calculations. Must not be null
	 * @param document required to instantiate the SVGGraphics2D
	 */

	public BatikGraphics2DWrapper(final Display display, final Document document){
		this.display = display;
		this.batikGraphicsSVG = new SVGGraphics2D(document);
	}

	/**
	 *
	 * @param display display-instance required for several calculations.
	 * @param ctx SVG Generator Context
	 * @param textAsShapes Whether or not to render text as Shapes
	 * @param xOffset x-value + width of the Node with the smallest x-coordinate
	 * @param yOffset y-value + width of the Node with the smallest y-coordinate
	 * @param bounds visible Area. Note: Nodes with negative (x,y) are handled by Offset!
	 */

	public BatikGraphics2DWrapper(final Display display, final SVGGeneratorContext ctx, final boolean textAsShapes, final int xOffset, final int yOffset, final Rectangle bounds) {
		this.display = display;
		this.batikGraphicsSVG = new SVGGraphics2D(ctx, textAsShapes);
		bounds.x = bounds.x - xOffset;
		bounds.y = bounds.y - yOffset;
		visibleArea = bounds;
	}

	@Override
	public void clipRect(final Rectangle r) {
	}

	@Override
	public void dispose() {

		batikGraphicsSVG = null;
		display = null;
		_SWTFont = null;
		_SWTForegroundColor = null;
		_SWTBackgroundColor = null;
		visibleArea = null;

	}


	@Override
	public void drawArc(final int x, final int y, final int w, final int h, final int offset, final int length) {
		batikGraphicsSVG.drawArc(x, y, w, h, offset, length);

	}

	@Override
	public void drawFocus(final int x, final int y, final int w, final int h) {
		batikGraphicsSVG.drawRect(x, y, w, h); // !

	}

	/**
	 * Converts SWT Image to AWT BufferedImage.
	 * @param src Image to convert
	 * @return BufferedImage equivalent to the source image
	 */

	private BufferedImage convertImage(final Image src){
		ImageData data = src.getImageData();	// Obtain ImageData

		if (data.getTransparencyType() == SWT.TRANSPARENCY_NONE){			// No Transparency, so this should be easy
			DirectColorModel cm = new DirectColorModel(data.depth, 0xFF0000, 0x00FF00, 0x0000FF);
			PaletteData palette = data.palette;
			WritableRaster wr = cm.createCompatibleWritableRaster(data.width, data.height);
//			if (palette.isDirect){		// Note: Currently all opaque images have a direct palette
				int[] rgbArray = new int[3];
				int pixel;
				RGB rgb;
				for (int x=0; x < data.width; x++) {
					for (int y=0; y < data.height; y++) {
						pixel = data.getPixel(x, y);
						rgb = palette.getRGB(pixel);
						rgbArray[0] = rgb.red;
						rgbArray[1] = rgb.green;
						rgbArray[2] = rgb.blue;
						wr.setPixel(x, y, rgbArray);
					}
				}

//			}
			BufferedImage result = new BufferedImage(cm, wr, false, null);
			return result;
		}

		if (data.getTransparencyType() == SWT.TRANSPARENCY_ALPHA){		// Uses Alpha-Values for each pixel
			DirectColorModel cm = new DirectColorModel(32, 0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);  // Creates a 32-bit ColorModel with 8 Bits each for red, green, blue and alpha
			PaletteData palette = data.palette;
			WritableRaster wr = cm.createCompatibleWritableRaster(data.width, data.height);
//			if (palette.isDirect){		// Note: Currently all images with an alpha-layer have a direct palette
				int[] rgbArray = new int[4];
				int pixel;
				RGB rgb;
				for (int x=0; x < data.width; x++) {
					for (int y=0; y < data.height; y++) {
						pixel = data.getPixel(x, y);
						rgb = palette.getRGB(pixel);
						rgbArray[0] = rgb.red;
						rgbArray[1] = rgb.green;
						rgbArray[2] = rgb.blue;
						rgbArray[3] = data.getAlpha(x, y);
						wr.setPixel(x, y, rgbArray);
					}
				}

//			}
			BufferedImage result = new BufferedImage(cm, wr, false, null);
			return result;
		}

		if (data.getTransparencyType() == SWT.TRANSPARENCY_PIXEL){		// A certain pixel-value is set for transparent pixels
			DirectColorModel cm = new DirectColorModel(32, 0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);
			PaletteData palette = data.palette;
			WritableRaster wr = cm.createCompatibleWritableRaster(data.width, data.height);
				int[] rgbArray = new int[4];
				int pixel;
				RGB rgb;
				for (int x=0; x < data.width; x++) {
					for (int y=0; y < data.height; y++) {
						pixel = data.getPixel(x, y);
						rgb = palette.getRGB(pixel);
						rgbArray[0] = rgb.red;
						rgbArray[1] = rgb.green;
						rgbArray[2] = rgb.blue;
						if (data.getPixel(x, y) == data.transparentPixel) {
                            rgbArray[3] = 0 ;
                        } else {
                            rgbArray[3] = 255;
                        }
						wr.setPixel(x, y, rgbArray);
					}
				}
			BufferedImage result = new BufferedImage(cm, wr, false, null);
			return result;
		}

		if (data.getTransparencyType() == SWT.TRANSPARENCY_MASK){  // Image contains additional ImageData for the alphaMask
			DirectColorModel cm = new DirectColorModel(32, 0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);
			PaletteData palette = data.palette;
			ImageData alphaMask = data.getTransparencyMask();
			WritableRaster wr = cm.createCompatibleWritableRaster(data.width, data.height);
				int[] rgbArray = new int[4];
				int pixel;
				RGB rgb;
				for (int x=0; x < data.width; x++) {
					for (int y=0; y < data.height; y++) {
						pixel = data.getPixel(x, y);
						rgb = palette.getRGB(pixel);
						rgbArray[0] = rgb.red;
						rgbArray[1] = rgb.green;
						rgbArray[2] = rgb.blue;
						if (alphaMask.getPixel(x, y) == 0) {
                            rgbArray[3] = 0 ;
                        } else {
                            rgbArray[3] = 255;
                        }
						wr.setPixel(x, y, rgbArray);
					}
				}
			BufferedImage result = new BufferedImage(cm, wr, false, null);
			return result;
		}
		 BufferedImage result = new BufferedImage(data.width, data.height, Transparency.OPAQUE);
		 return result;

	}


	@Override
	public void drawImage(final Image srcImage, final int x, final int y) {
		batikGraphicsSVG.drawImage(convertImage(srcImage), x, y, null);

	}

	@Override
	public void drawImage(final Image srcImage, final int x1, final int y1, final int w1, final int h1,
			final int x2, final int y2, final int w2, final int h2) {
		batikGraphicsSVG.drawImage(convertImage(srcImage), x2, y2, x2+w2, y2+h2, x1, y1, x1+w1, y1+h1, null);

	}

	@Override
	public void drawLine(final int x1, final int y1, final int x2, final int y2) {
		batikGraphicsSVG.drawLine(x1, y1, x2, y2);
	}

	@Override
	public void drawOval(final int x, final int y, final int w, final int h) {
		batikGraphicsSVG.drawOval(x, y, w, h);

	}

	@Override
	public void drawPolygon(final PointList points) {
		int[] xPoints = new int[points.size()];
		int[] yPoints = new int[points.size()];
		for (int i = 0; i<points.size(); i++) {
			xPoints[i]= points.getPoint(i).x;
			yPoints[i]= points.getPoint(i).y;
		}
		batikGraphicsSVG.drawPolygon(xPoints, yPoints, points.size());

	}

	@Override
	public void drawPolyline(final PointList points) {
		int[] xPoints = new int[points.size()];
		int[] yPoints = new int[points.size()];
		for (int i = 0; i<points.size(); i++) {
			xPoints[i]= points.getPoint(i).x;
			yPoints[i]= points.getPoint(i).y;
		}
		batikGraphicsSVG.drawPolyline(xPoints, yPoints, points.size());

	}

	@Override
	public void drawRectangle(final int x, final int y, final int width, final int height) {
		batikGraphicsSVG.drawRect(x, y, width, height);

	}

	@Override
	public void drawRoundRectangle(final Rectangle r, final int arcWidth, final int arcHeight) {
		batikGraphicsSVG.drawRoundRect(r.x, r.y, r.width, r.height, arcWidth, arcHeight);

	}

	@Override
	public void drawString(final String s, final int x, final int y) {
		GC gc = new GC(display);
		gc.setFont(_SWTFont);
		int height = gc.getFontMetrics().getHeight();
		gc.dispose();
		batikGraphicsSVG.drawString(s, x, y + height);

	}

	@Override
	public void drawText(final String s, final int x, int y) {
		String[] text = s.split("\n");
		GC gc = new GC(display);
		gc.setFont(_SWTFont);
		int height = gc.getFontMetrics().getHeight();
		gc.dispose();
		for (String string : text){
			y = y+height;
			batikGraphicsSVG.drawString(string,x, y);
		}


	}

	@Override
	public void fillArc(final int x, final int y, final int w, final int h, final int offset, final int length) {
		swapColors();
		batikGraphicsSVG.fillArc(x, y-1, w, h, offset, length);
		swapColors();

	}

	@Override
	public void fillGradient(final int x, final int y, final int w, final int h, final boolean vertical) {

	}

	@Override
	public void fillOval(final int x, final int y, final int w, final int h) {
		swapColors();
		batikGraphicsSVG.fillOval(x, y-1, w, h);
		swapColors();

	}

	@Override
	public void fillPolygon(final PointList points) {
		int[] xPoints = new int[points.size()];
		int[] yPoints = new int[points.size()];
		for (int i = 0; i<points.size(); i++) {
			xPoints[i]= points.getPoint(i).x;
			yPoints[i]= points.getPoint(i).y;
		}
		swapColors();
		batikGraphicsSVG.fillPolygon(xPoints, yPoints, points.size());
		swapColors();

	}

	@Override
	public void fillRectangle(final int x, final int y, final int width, final int height) {
		swapColors();
		batikGraphicsSVG.fillRect(x, y-1, width, height);
		swapColors();

	}

	@Override
	public void fillRoundRectangle(final Rectangle r, final int arcWidth, final int arcHeight) {
		swapColors();
		batikGraphicsSVG.fillRoundRect(r.x, r.y-1, r.width, r.height, arcWidth, arcHeight);
		swapColors();
	}

	@Override
    public void fillString(final String s, final int x, final int y) {
		swapColors();
		batikGraphicsSVG.drawString(s, x, y);
		swapColors();
	}

	@Override
	public void fillText(final String s, final int x, final int y) {
		swapColors();
		batikGraphicsSVG.drawString(s, x, y);
		swapColors();

	}

	@Override
	public Color getBackgroundColor() {

		return _SWTBackgroundColor;
	}

	@Override
	public Rectangle getClip(final Rectangle rect) { 				// Determines visible area in the generated SVG image.
//		Rectangle rect2 = new Rectangle(0, 0, 7680, 4320);  // Super Hi-Vision resolution
		return visibleArea;
	}

	@Override
	public Font getFont() {

		return _SWTFont;
	}

	@Override
	public FontMetrics getFontMetrics() {
		GC gc = new GC(display);
		gc.setFont(_SWTFont);
		FontMetrics metrik = gc.getFontMetrics();
		gc.dispose();
		return metrik;
	}

	@Override
	public Color getForegroundColor() {

		return _SWTForegroundColor;
	}

	@Override
	public int getLineStyle() {

		return _SWTLineStyle;
	}

	@Override
	public int getLineWidth() {

		return (int) _SWTLineWidth;
	}

	@Override
	public float getLineWidthFloat() {

		return _SWTLineWidth;
	}

	@Override
	public boolean getXORMode() {

		return _XORMode;
	}

	@Override
	public void popState() {
		if (!(_stack.isEmpty()))
		_stack.pop().restore();

	}

	@Override
	public void pushState() {
		_stack.push(new State());

	}

	@Override
	public void restoreState() {
		_stack.peek().restore();

	}

	@Override
	public void scale(final double amount) {
		batikGraphicsSVG.scale(amount, amount);

	}

	@Override
	public void setBackgroundColor(final Color rgb) {
		_SWTBackgroundColor = rgb;
		batikGraphicsSVG.setBackground(SWTtoAWTColor(rgb));

	}

	@Override
	public void setClip(final Rectangle r) {
	}

	@Override
	public void setFont(final Font f) {
		_SWTFont = f;
		updateFont();
	}

	@Override
	public void setForegroundColor(final Color rgb) {
		_SWTForegroundColor = rgb;
		batikGraphicsSVG.setColor(SWTtoAWTColor(rgb));

	}

	@Override
	public void setLineStyle(final int style) {
		_SWTLineStyle = style;		// Currently, no case other than default is used.
//		switch(style){
//		case LINE_DOT :
//			wrapped.setStroke(new BasicStroke(_SWTLineWidth, BasicStroke.CAP_SQUARE,
//				BasicStroke.JOIN_MITER, 10, null, 0));
//			break;
//		case LINE_DASH :
//			wrapped.setStroke(new BasicStroke(_SWTLineWidth, BasicStroke.CAP_SQUARE,
//				BasicStroke.JOIN_MITER, 10, null, 0));
//			break;
//		case LINE_DASHDOT :
//			wrapped.setStroke(new BasicStroke(_SWTLineWidth, BasicStroke.CAP_SQUARE,
//				BasicStroke.JOIN_MITER, 10, null, 0));
//			break;
//		case LINE_DASHDOTDOT :
//			wrapped.setStroke(new BasicStroke(_SWTLineWidth, BasicStroke.CAP_SQUARE,
//				BasicStroke.JOIN_MITER, 10, null, 0));
//			break;
//		default :
			batikGraphicsSVG.setStroke(new BasicStroke(_SWTLineWidth, BasicStroke.CAP_SQUARE,
				BasicStroke.JOIN_MITER, 10, null, 0));
//		}

	}

	@Override
	public void setLineWidth(final int width) {
		_SWTLineWidth = width;
	}

	@Override
	public void setLineWidthFloat(final float width) {
		_SWTLineWidth = width;
	}

	@Override
	public void setLineMiterLimit(final float miterLimit) {
	}

	@Override
	public void setXORMode(final boolean b) {
		_XORMode = b;	//Note: No effect

	}

	/**
	 *
	 * @param writer Writer in which to stream the generated SVG
	 * @param useCSS Whether to use CSS-Style attributes or not
	 * @throws SVGGraphics2DIOException
	 */

	public void stream(final Writer writer, final Boolean useCSS) throws SVGGraphics2DIOException{
		batikGraphicsSVG.stream(writer, useCSS);
	}

	@Override
	public void setLineAttributes(final LineAttributes attributes) {
		_SWTLineWidth = attributes.width;
		_SWTLineStyle = attributes.style;
		setLineStyle(_SWTLineStyle);
	}

	/**
	 * Updates the font currently used by the wrapped SVGGraphics2D
	 */

	private void updateFont() {
		FontData fd = _SWTFont.getFontData()[0];
		int fontStyle = fd.getStyle();
		int height = fd.getHeight();
		int AWTStyle;
		switch (fontStyle) {
		case SWT.BOLD :
			AWTStyle = java.awt.Font.BOLD;
			break;
		case SWT.ITALIC :
			AWTStyle = java.awt.Font.ITALIC;
			break;
		default :
			AWTStyle = java.awt.Font.PLAIN;
		}
		java.awt.Font AWTFont = new java.awt.Font(fd.getName(), AWTStyle, height);
		batikGraphicsSVG.setFont(AWTFont);
	}
	/**
	 * Swaps the colors currently used by the wrapped SVGGraphics2D
	 */
	private void swapColors(){

			java.awt.Color tempColour = batikGraphicsSVG.getBackground();
			batikGraphicsSVG.setBackground(batikGraphicsSVG.getColor());
			batikGraphicsSVG.setColor(tempColour);
	}

	/**
	 * Transforms a org.eclipse.swt.graphics Color into a java.awt.Color. All transparency information is lost.
	 *
	 * @param color SWT Color to transform
	 * @return transformed AWT Color
	 */
	private java.awt.Color SWTtoAWTColor(final org.eclipse.swt.graphics.Color color){
		if (color == null) {
            return new java.awt.Color(0,0,0);
        }
		java.awt.Color result = new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue());
		return result;
	}

	@Override
	public void translate(final int dx, final int dy) {
		batikGraphicsSVG.translate(dx, dy);

	}
	/**
	 * Resets foreground- and background-color to black.
	 */
	public void resetColors(){
		batikGraphicsSVG.setBackground(new java.awt.Color(0,0,0));
		batikGraphicsSVG.setColor(new java.awt.Color(0,0,0));
	}

}
