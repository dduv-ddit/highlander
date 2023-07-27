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
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Random;

import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Tools;

public class PieChart extends JPanel {

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
	private String[] categories; 
	//private double[] values;
	private double[] percent;
	private double[] angle;
	private Color[] colors;
	private double totalValues = 0.0D;
	private boolean showPercent;
	
	public PieChart(String title, String[] categories, double[] values, boolean showPercent){
		this.title = title; 
		this.categories = categories; 
		//this.values = values;
		this.showPercent = showPercent;
		percent = new double[values.length];
		angle = new double[values.length];
		colors = new Color[categories.length];
    for (int i = 0; i < values.length; i++) {
    	totalValues += values[i];
    }
		for (int i=0 ; i < categories.length ; i++) {
			if (i < presetcolors.length) {
				colors[i] = presetcolors[i];
			}else {
				colors[i] = new Color(new Random().nextInt(0xFFFFFF));
			}
  		percent[i] = values[i] / totalValues;
			angle[i] = values[i] * 360.0 / totalValues; 
		}
	}

	public String getTitle(){
		return title;
	}
	
	@Override
	public void paintComponent(Graphics graphic) {
    Graphics2D g = (Graphics2D) graphic;
    Font smallFont = new Font("SansSerif", Font.PLAIN, 12);
    Font bigFont = new Font("SansSerif", Font.BOLD, 16);
		//Stroke normalStroke = g.getStroke();
		Stroke thickStroke = new BasicStroke(2);
		//Stroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);

		g.setFont(smallFont);
		Dimension dim = this.getSize();

    int height = (int)dim.getHeight();
    int width = (int)dim.getWidth();
		int centerX = width/2;
    int centerY = height/2;
    if (width > height) {
    	width = height -= 100;
    }else {
    	height = width -= 100;
    }
    int leftX = centerX-width/2;
    int rightX = centerX+width/2;
    int topY = centerY-height/2;
    //int bottomY = centerY+height/2;

    //Set chart background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, (int) dim.getWidth(), (int) dim.getHeight());

    // Color legend
    int hLegend = (int)g.getFontMetrics().getStringBounds("AZERTYUIOPMLKJHGFDSQWXCVBN",g).getHeight();
    int y = topY;
    for (int i=0 ; i < categories.length ; i++) {
    	y+=hLegend+10;
    	g.setColor(colors[i]);
    	g.fillRect(rightX+40, y, hLegend, hLegend);
    	g.setColor(Color.BLACK);
    	g.drawString(categories[i], rightX+40 + hLegend + 10, y+(hLegend-3));
    }
    
    if (totalValues != 0){
    	// Draw each pie slice
    	int radius = width /2;
    	int initAngle=90;
    	int sweepAngle=0;
    	int incSweepAngle=0;
    	int[] centerAngles = new int[categories.length];
    	for (int i=0 ; i < categories.length ; i++) {
    		sweepAngle = (int) Math.round(angle[i]);
    		if (i == categories.length-1) {
    			// Ensure that rounding errors do not leave a gap between the first and last slice
    			sweepAngle = 360 - incSweepAngle;
    		}
    		centerAngles[i] = incSweepAngle + sweepAngle / 2;
    		g.setColor(colors[i]);
    		int tx = (int) (leftX + (20 * Math.cos((centerAngles[i] * 3.14f/180) - 3.14f/2)));
    		int ty = (int) (topY + (20 * Math.sin((centerAngles[i] * 3.14f/180) - 3.14f/2)));
    		g.fillArc(tx, ty, width-20, height-20, initAngle, -sweepAngle);
      	
    		//Black outline of parts
    		g.setColor(Color.BLACK);
    		g.setStroke(thickStroke);
    		g.drawArc(tx, ty, width-20, height-20, initAngle, -sweepAngle);
    		int cx = (int) (centerX-10 + (20 * Math.cos((centerAngles[i] * 3.14f/180) - 3.14f/2)));
    		int cy = (int) (centerY-10 + (20 * Math.sin((centerAngles[i] * 3.14f/180) - 3.14f/2)));
    		int ax = (int) (cx + ((radius-10) * Math.cos((incSweepAngle * 3.14f/180) - 3.14f/2)));
    		int ay = (int) (cy + ((radius-10) * Math.sin((incSweepAngle * 3.14f/180) - 3.14f/2)));
    		int bx = (int) (cx + ((radius-10) * Math.cos(((incSweepAngle + sweepAngle) * 3.14f/180) - 3.14f/2)));
    		int by = (int) (cy + ((radius-10) * Math.sin(((incSweepAngle + sweepAngle) * 3.14f/180) - 3.14f/2)));
    		g.drawLine(cx,cy,ax,ay);
    		g.drawLine(cx,cy,bx,by);
    		
    		
    		incSweepAngle +=sweepAngle;
    		if (i == categories.length-1) {
    			// Ensure that rounding errors do not leave a gap between the first and last slice
    			incSweepAngle = 0;
    		}
    		
    		if (i < categories.length-1) centerAngles[i+1]= centerAngles[i] + (int) (angle[i]/2 + angle[i+1]/2);
    		initAngle += (-sweepAngle);
    	}

    	if (showPercent) {
    		for (int i=0 ; i < categories.length ; i++) {
    			int px = (int) (centerX + ((radius*0.9) * Math.cos((centerAngles[i] * 3.14f/180) - 3.14f/2)));
    			int py = (int) (centerY + ((radius*0.9) * Math.sin((centerAngles[i] * 3.14f/180) - 3.14f/2)));
    			g.setFont(bigFont);
    			int strWidth = g.getFontMetrics().stringWidth(Tools.doubleToPercent(percent[i], 0));
    			int sh = g.getFontMetrics().getHeight()/2;
    			g.setColor(Color.BLACK);
    			AffineTransform origin = g.getTransform();
    			g.translate((px - strWidth/2), py);
    			double angle = (centerAngles[i] <= 180) ? Math.toRadians(90) : Math.toRadians(270);
    			g.rotate(Math.toRadians(centerAngles[i]) - angle);
    			//for outilined text    			
    			g.drawString(Tools.doubleToPercent(percent[i], 0),1,sh+1);
    			g.drawString(Tools.doubleToPercent(percent[i], 0),1,sh-1);
    			g.drawString(Tools.doubleToPercent(percent[i], 0),-1,sh+1);
    			g.drawString(Tools.doubleToPercent(percent[i], 0),-1,sh-1);
    			g.setColor(Color.WHITE);
    			g.drawString(Tools.doubleToPercent(percent[i], 0),0,sh);
    			g.setTransform(origin);
    		}
    	}
    }
    
		//Draw title
    g.setColor(Color.BLACK);
		g.setFont(bigFont);
		g.drawString(title, leftX + (width-(int)g.getFontMetrics().getStringBounds(title,g).getWidth())/2, 20);
	}

}
