package org.knime.workbench.editor.svgexport.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.knime.core.node.workflow.WorkflowAnnotation.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;

/**
 * Currently used figure for annotations in the SVG-export. Work in Progress.
 * @author Andreas
 *
 */
public class AnnotationSVGFigure {
	
	private Display display;
	private AnnotationEditPart part;
	private String text;
	private StyleRange[] styleRanges;
	
	public AnnotationSVGFigure(Display display, AnnotationEditPart part){
		this.part = part;
		this.display = display;
	}
	
	public void paint(Graphics g){
		WorkflowAnnotation anno = part.getModel();
		Color bg = part.getFigure().getBackgroundColor();
		g.setBackgroundColor(bg);
		Color fg = part.getFigure().getForegroundColor();
		g.setForegroundColor(fg);
		g.fillRectangle(new Rectangle(anno.getX(), anno.getY(), anno.getWidth(), anno.getHeight()));
		g.setForegroundColor(ColorConstants.black);
		g.setFont(part.getFigure().getFont());
		text = anno.getText();
		String[] lines = text.split("\n");
		int height = 0; int i = 0; int x = anno.getX(), y = anno.getY();
		GC gc = new GC(display);
		gc.setFont(part.getFigure().getFont());
		if (lines.length!=0){
			while ((i<lines.length)&&((height+gc.getFontMetrics().getHeight()) < anno.getHeight())){
				if (anno.getWidth()> (lines[i].length() * gc.getFontMetrics().getAverageCharWidth())){
					g.drawString(lines[i],x,y);
					
				}
				i++;
				height = height + gc.getFontMetrics().getHeight();
				y =y + gc.getFontMetrics().getHeight();
			}
		}
		gc.dispose();
		
		
		
		

	}
}
