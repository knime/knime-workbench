package org.knime.workbench.editor.svgexport.figures;

import java.util.LinkedList;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowAnnotation.StyleRange;
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
	private Font defaultFont;
	private LinkedList<String> preparedText = new LinkedList<String>();
	private LinkedList<Font> preparedFonts = new LinkedList<Font>();
	private GC gc;

	public AnnotationSVGFigure(final Display display, final AnnotationEditPart part){
		this.part = part;
		this.display = display;
	}

	public void paint(final Graphics g){
		WorkflowAnnotation anno = part.getModel();

		Color bg = part.getFigure().getBackgroundColor();
		Color fg = part.getFigure().getForegroundColor();
		g.setBackgroundColor(bg);
		g.setForegroundColor(fg);
		defaultFont = part.getFigure().getFont();
		g.fillRectangle(new Rectangle(anno.getX(), anno.getY(), anno.getWidth(), anno.getHeight()));

		g.setForegroundColor(ColorConstants.black);
		g.setFont(part.getFigure().getFont());
		text = anno.getText();
		int x = anno.getX(), y = anno.getY();
//		String[] lines = text.split("\n");
		gc = new GC(display);

		boolean qw = prepareText(anno);
		gc.setFont(part.getFigure().getFont());
//		if (lines.length!=0){
//			while ((i<lines.length)&&((height+gc.getFontMetrics().getHeight()) < anno.getHeight())){
//				if (anno.getWidth() < (lines[i].length() * gc.getFontMetrics().getAverageCharWidth())){
//					lines = cullLines(lines, lines[i], i);
//
//				}
//				g.drawString(lines[i],x,y);
//				i++;
//				height = height + gc.getFontMetrics().getHeight();
//				y =y + gc.getFontMetrics().getHeight();
//			}
//		}
		String temp = "";
		String[] lines = null;
		while (preparedText.iterator().hasNext()){
			temp = preparedText.remove();
			lines = temp.split("/n");
			System.out.println("-");
			for (String asdf:lines){
				System.out.println(asdf + " " + lines.length);
			}
			System.out.println("-");
//			if (lines.length == 1){
//				lines = cullLines(lines, 1);
//				g.setFont(preparedFonts.remove());
//				g.drawString(temp, x, y);
//				x = x + gc.getFontMetrics().getAverageCharWidth() * temp.length();
//			}
//			else{
				g.setFont(defaultFont = preparedFonts.remove());
				gc.setFont(defaultFont);
				for (int j = 0; j<lines.length; j++){
					if (anno.getWidth() < (lines[j].length() * gc.getFontMetrics().getAverageCharWidth())){
						g.drawString(lines[j], x, y);
						x = x + lines[j].length() * gc.getFontMetrics().getAverageCharWidth();
					}
					else{
						g.drawString(lines[j], x, y);
						x = x + lines[j].length() * gc.getFontMetrics().getAverageCharWidth();
					}
					if ((x - anno.getX()) > anno.getWidth()){
						x = anno.getX();
						y = y+ gc.getFontMetrics().getHeight();
					}
					if (lines.length > j+1){
						x = anno.getX();
						y = y+ gc.getFontMetrics().getHeight();
					}
//				}
			}
		}

		gc.dispose();
	}

	private boolean prepareText(final WorkflowAnnotation annotation){
		StyleRange[] ranges = annotation.getStyleRanges();
		String text = annotation.getText();
		if (ranges[0].getStart() != 0) {
            preparedText.add(text.substring(0, ranges[0].getStart()));
        }
				preparedFonts.add(defaultFont);
		StyleRange lastrange = ranges[0];
		preparedText.add(text.substring(ranges[0].getStart(), ranges[0].getStart()+ranges[0].getLength()));
		preparedFonts.add(new Font(display, ranges[0].getFontName(), ranges[0].getFontSize(), ranges[0].getFontStyle()));
		for (int i = 1; i<ranges.length; i++){
			if(ranges[i].getStart()>lastrange.getStart()+lastrange.getLength()){
				preparedText.add(text.substring(lastrange.getStart()+lastrange.getLength(), ranges[i].getStart()));
				preparedFonts.add(defaultFont);
				preparedText.add(text.substring(ranges[i].getStart(), ranges[i].getStart()+ranges[i].getLength()));
				preparedFonts.add(new Font(display, ranges[i].getFontName(), ranges[i].getFontSize(), ranges[i].getFontStyle()));

			}
			else{
				preparedText.add(text.substring(ranges[i].getStart(), ranges[i].getStart()+ranges[i].getLength()));
				preparedFonts.add(new Font(display, ranges[i].getFontName(), ranges[i].getFontSize(), ranges[i].getFontStyle()));
			}
			lastrange = ranges[i];
		}
		return true;
	}

	private String[] cullLines(final String[] strings, final int i){
		String[] result = new  String[strings.length+1];
		String temp = strings[i];
		String temp2 = temp;
		int k = (int) (part.getModel().getWidth()* 1.2 / gc.getFontMetrics().getAverageCharWidth());
		k = temp2.lastIndexOf(' ', k);
		temp = temp2.substring(0, k) + "\n" + temp2.substring(k);
		String[] temp3 = temp.split("\n ");
		temp3[1].trim();
		for (int l = 0; l< i; l++){
			result[l] = strings[l];
		}
		result[i] = temp3[0];
		result[i+1] = temp3[1];
		for (int l = i+1; l< strings.length; l++){
			result[l+1] = strings[l];
		}
		for (int m = 0; m<result.length; m++ ){

		}
		return result;
	}
}
