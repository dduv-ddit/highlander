/*****************************************************************************************
*
* Highlander - Copyright (C) <2012-2020> <Université catholique de Louvain (UCLouvain)>
* 	
* List of the contributors to the development of Highlander: see LICENSE file.
* Description and complete License: see LICENSE file.
* 	
* This program (Highlander) is free software: 
* you can redistribute it and/or modify it under the terms of the 
* GNU General Public License as published by the Free Software Foundation, 
* either version 3 of the License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program (see COPYING file).  If not, 
* see <http://www.gnu.org/licenses/>.
* 
*****************************************************************************************/

/**
*
* @author Raphael Helaers
*
*/

package be.uclouvain.ngs.highlander.UI.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JPanel;

public class SpiderChart extends JPanel {

	public Color[] presetcolors = {
      new Color(0,191,255) /*DeepSkyBlue*/,
      new Color(255,127,0) /*coral*/,
      new Color(125,38,205) /*purple3*/,
      new Color(255,215,0) /*gold*/,
      new Color(255,20,147) /*DeepPink*/,
      new Color(0,206,209) /*dark turquoise*/,
      new Color(133,99,99) /*Light Wood*/,
      new Color(224,102,255) /*MediumOrchid1*/,
      new Color(255,48,48) /*firebrick1*/,
      new Color(0,250,154) /*MediumSpringGreen*/,
      new Color(16,78,139) /*DodgerBlue4*/,
      new Color(139,139,0) /*yellow4*/,
      Color.pink,
      Color.orange,
      Color.lightGray,
      new Color(46,139,87) /*SeaGreen*/,
      new Color(255,127,36) /*chocolate1*/,
      new Color(127,255,0) /*chartreuse*/,
      Color.yellow,
      new Color(152,245,255) /*CadetBlue1*/,
      Color.blue,
      new Color(139,69,19) /*SaddleBrown*/,
      Color.magenta,
      new Color(202,255,112) /*DarkOliveGreen1*/,
      new Color(138,43,226) /*BlueViolet*/,
      new Color(127,255,212) /*aquamarine*/,
      Color.red,
      new Color(221,160,221) /*plum*/,
      new Color(187,255,255) /*PaleTurquoise1*/,
      new Color(255,62,150) /*VioletRed1*/,
      new Color(255,160,122) /*light salmon*/,
      Color.green,
      new Color(205,201,165) /*LemonChiffon3*/,
      Color.cyan,
      new Color(255,36,0) /*Orange Red*/,
	};
	
	private String title; 
	private List<String> categories;
	private double min;
	private double max;
	private int numLines;
	private Map<String,Map<String, Double>> dataPoints;
	private Map<String,Color> colors;
	private double angle;

	public SpiderChart(String title, List<String> categories, double min, double max, int numLines, Map<String,Map<String, Double>> dataPoints){
		this.title = title; 
		this.categories = categories; 
		this.min = min;
		this.max = max;
		this.numLines = numLines;
		this.dataPoints = dataPoints;
		colors = new HashMap<>();
		int i=0;
		for (String label : dataPoints.keySet()) {
			if (i < presetcolors.length) {
				colors.put(label, presetcolors[i++]);
			}else {
				colors.put(label, new Color(new Random().nextInt(0xFFFFFF)));
			}
		}
		angle = 360.0 / categories.size();
	}

	public String getTitle(){
		return title;
	}
	
	@Override
	public void paintComponent(Graphics graphic) {
		super.paintComponent(graphic);
    Graphics2D g = (Graphics2D) graphic;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		Font smallFont = new Font("SansSerif", Font.PLAIN, 10);
    Font bigFont = new Font("SansSerif", Font.BOLD, 16);
		//Stroke normalStroke = g.getStroke();
		Stroke thickStroke = new BasicStroke(2);
		//Stroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
		Stroke dottedStroke = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2}, 0);

		g.setFont(smallFont);
		Dimension dim = this.getSize();

    int width = (int)dim.getWidth();
    int height = width;
    setSize(width, height);
    setPreferredSize(new Dimension(width, height));
		g.clipRect(0, 0, width, height);
		
		int centerX = width/2;
    int centerY = height/2;
    int leftX = centerX-width/2;
    //int rightX = centerX+width/2;
    int topY = centerY-height/2;
    //int bottomY = centerY+height/2;

    //Set chart background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, width, height);

    // Color legend
    int hLegend = (int)g.getFontMetrics().getStringBounds("AZERTYUIOPMLKJHGFDSQWXCVBN",g).getHeight();
    int y = topY;
    for (String label : dataPoints.keySet()) {
    	y+=hLegend+3;
    	g.setColor(colors.get(label));
    	g.fillRect(leftX+5, y, hLegend, hLegend);
    	g.setColor(Color.BLACK);
    	g.drawString(label, leftX+5 + hLegend + 10, y+(hLegend-3));
    }
        
		//Draw title
    g.setColor(Color.BLACK);
		g.setFont(bigFont);
		g.drawString(title, leftX + (width-(int)g.getFontMetrics().getStringBounds(title,g).getWidth())/2, 20);
		
		
		//Draw background chart
		int radius = width / 2 - 60;
		int sweepAngle=0;
  	int incSweepAngle=0;
  	int[] centerAngles = new int[categories.size()];
		for (int i=0 ; i < categories.size() ; i++) {
			sweepAngle = (int) Math.round(angle);
  		if (i == categories.size()-1) {
  			// Ensure that rounding errors do not leave a gap between the first and last slice
  			sweepAngle = 360 - incSweepAngle;
  		}
			centerAngles[i] = incSweepAngle + sweepAngle / 2;
  		int cx = (int) (centerX + (Math.cos((centerAngles[i] * 3.14f/180) - 3.14f/2)));
  		int cy = (int) (centerY + (Math.sin((centerAngles[i] * 3.14f/180) - 3.14f/2)));
  		int ax = (int) (cx + ((radius) * Math.cos((incSweepAngle * 3.14f/180) - 3.14f/2)));
  		int ay = (int) (cy + ((radius) * Math.sin((incSweepAngle * 3.14f/180) - 3.14f/2)));
  		int bx = (int) (cx + ((radius) * Math.cos(((incSweepAngle + sweepAngle) * 3.14f/180) - 3.14f/2)));
  		int by = (int) (cy + ((radius) * Math.sin(((incSweepAngle + sweepAngle) * 3.14f/180) - 3.14f/2)));
			int px = (int) (centerX + ((radius*1.05) * Math.cos((incSweepAngle * 3.14f/180) - 3.14f/2)));
			int py = (int) (centerY + ((radius*1.05) * Math.sin((incSweepAngle * 3.14f/180) - 3.14f/2)));
  		g.setColor(Color.LIGHT_GRAY);
  		g.setStroke(dottedStroke);
  		g.drawLine(cx,cy,ax,ay);
  		g.drawLine(ax,ay,bx,by);
  		double offset = 1.0 / numLines;
  		for (int j=1 ; j < numLines ; j++) {  		
  			double r = radius*(j*offset);
    		int nx = (int) (cx + ((r) * Math.cos((incSweepAngle * 3.14f/180) - 3.14f/2)));
    		int ny = (int) (cy + ((r) * Math.sin((incSweepAngle * 3.14f/180) - 3.14f/2)));
    		int mx = (int) (cx + ((r) * Math.cos(((incSweepAngle + sweepAngle) * 3.14f/180) - 3.14f/2)));
    		int my = (int) (cy + ((r) * Math.sin(((incSweepAngle + sweepAngle) * 3.14f/180) - 3.14f/2)));
  			g.drawLine(nx,ny,mx,my);
  		}
  		int labelWidth = (int)g.getFontMetrics().getStringBounds(categories.get(i),g).getWidth();
  		int labelHeight = (int)g.getFontMetrics().getStringBounds(categories.get(i),g).getHeight();
  		g.setColor(Color.BLACK);
  		g.drawString(categories.get(i), px - (labelWidth/2) , py + (labelHeight/2) - 3);
  		
  		incSweepAngle +=sweepAngle;
  		if (i == categories.size()-1) {
  			// Ensure that rounding errors do not leave a gap between the first and last slice
  			incSweepAngle = 0;
  		}
  		
  		if (i < categories.size()-1) centerAngles[i+1]= centerAngles[i] + (int)angle;
		}
		
		//Draw data points
		for (String label : dataPoints.keySet()) {
			int[] xPoints = new int[categories.size()];
			int[] yPoints = new int[categories.size()];
			sweepAngle=0;
	  	incSweepAngle=0;
			for (int i=0 ; i < categories.size() ; i++) {
				sweepAngle = (int) Math.round(angle);
	  		if (i == categories.size()-1) {
	  			// Ensure that rounding errors do not leave a gap between the first and last slice
	  			sweepAngle = 360 - incSweepAngle;
	  		}
	  		double r = 0.0;
	  		if (dataPoints.get(label).containsKey(categories.get(i))) {
	  			r = radius * ( (dataPoints.get(label).get(categories.get(i)) - min) / (max-min) );
	  		}else {
	  			//System.out.println(label + " doesn't have data point for " + categories.get(i));
	  		}
  			int cx = (int) (centerX + (Math.cos((centerAngles[i] * 3.14f/180) - 3.14f/2)));
  			int cy = (int) (centerY + (Math.sin((centerAngles[i] * 3.14f/180) - 3.14f/2)));
  			xPoints[i] = (int) (cx + ((r) * Math.cos((incSweepAngle * 3.14f/180) - 3.14f/2)));
  			yPoints[i] = (int) (cy + ((r) * Math.sin((incSweepAngle * 3.14f/180) - 3.14f/2)));
	  		incSweepAngle +=sweepAngle;
	  		if (i == categories.size()-1) {
	  			// Ensure that rounding errors do not leave a gap between the first and last slice
	  			incSweepAngle = 0;
	  		}

	  		if (i < categories.size()-1) centerAngles[i+1]= centerAngles[i] + (int)angle;
			}
			
			g.setColor(new Color(colors.get(label).getRed(), colors.get(label).getGreen(), colors.get(label).getBlue(), 50));
  		g.fillPolygon(xPoints, yPoints, categories.size());
			g.setColor(colors.get(label));
  		g.setStroke(thickStroke);
  		g.drawPolygon(xPoints, yPoints, categories.size());
  		for (int i=0 ; i < categories.size() ; i++) {
  			g.fillArc(xPoints[i]-5, yPoints[i]-5, 10, 10, 0, 360);
  		}
		}
	}

}
