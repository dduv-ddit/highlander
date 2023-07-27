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
import java.util.Random;

import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;

public class BarChart extends JPanel {

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
	private String labelX; 
	private String labelY; 
	private String[] categories; 
	private double[] values;
	private Color[] colors;
	private boolean showMeanAndSD;
	private boolean coloredCategories;
	private double mean;
	private double sd;
	
	public BarChart(String title, String labelX, String labelY, String[] categories, double[] values, boolean showMeanAndSD, boolean coloredCategories){
		this.title = title; 
		this.labelX = labelX; 
		this.labelY = labelY; 
		this.categories = categories; 
		this.values = values;
		this.showMeanAndSD = showMeanAndSD;
		this.coloredCategories = coloredCategories;
		colors = new Color[categories.length];
		for (double value : values){
			mean += value;
		}
		mean /= values.length;
		for (double value : values){
			sd += Math.pow(value-mean, 2);
		}
		sd /= values.length;
		sd = Math.sqrt(sd);
		for (int i=0 ; i < categories.length ; i++) {
			if (i < presetcolors.length) {
				colors[i] = presetcolors[i];
			}else {
				colors[i] = new Color(new Random().nextInt(0xFFFFFF));
			}
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
		Stroke normalStroke = g.getStroke();
		Stroke thickStroke = new BasicStroke(3);
		Stroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);

		g.setFont(smallFont);
		Dimension dim = this.getSize();

		//Compute space needed for X axis labels, then chart height
		int longestX = 0;
		for (String value : categories){
			int length = (int)(g.getFontMetrics().getStringBounds(value,g).getWidth() / 1.8);
			if (length > longestX) longestX = length;
		}
		int chartHeight = (int)dim.getHeight() - longestX - 10;
		
		//Get the biggest given value
		double maxValue = 0.0;
		for (double value : values){
			if (value > maxValue) maxValue = value;					
		}
		
		//Compute Y axis division height and number, depending on the max value power
		long trunc = (long)(maxValue*100);
		int nDig = (""+trunc).length();
		if (trunc == Math.pow(10, nDig-1)){
			nDig--;
		}
		double valDivisionY = Math.pow(10, nDig-3);
		int numDivisionY = (int)Math.ceil(maxValue/valDivisionY);
		while (numDivisionY < 5 && nDig > 3) {
			nDig--;
			valDivisionY = Math.pow(10, nDig-3);
			numDivisionY = (int)Math.ceil(maxValue/valDivisionY);
		}
		int heightDivisionY = (chartHeight / (numDivisionY+1));
		String[] labelsY = new String[numDivisionY];
		for (int i=0 ; i < labelsY.length ; i++){
			labelsY[i] = "" + Tools.doubleToString((valDivisionY * (i+1)), 2, false);
		}		
		
		//Compute space needed for Y axis labels
		int longestY = 0;
		for (String value : labelsY){
			int length = (int)g.getFontMetrics().getStringBounds(value,g).getWidth();
			if (length > longestY) longestY = length;
		}
		int length = (int)g.getFontMetrics().getStringBounds(labelY,g).getWidth();
		if (length > longestY) longestY = length;
		
		//Compute chart width and then X axis division width and number
		int chartWidth = (int)dim.getWidth() - longestY - 30;
		int numDivisionX = categories.length;
		int widthGapX = 20;
		int widthDivisionX = (chartWidth-widthGapX) / Math.max(numDivisionX, 1);

		//If bar width is too small, enlarge the chart size (it will be larger than window so a scroll bar will be needed)
		if (widthDivisionX < 50) {
			widthDivisionX = 50;	
			chartWidth = widthGapX + (widthDivisionX * Math.max(numDivisionX, 1));
			dim.width = longestY + 30 + chartWidth;
			setPreferredSize(dim);
		}

		//Starting coordinates : where the Y axis starts drawing
		int chartStartX = longestY + 15; 
		int chartStartY = 5;

		//Set chart background
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, (int) dim.getWidth(), (int) dim.getHeight());

		//Draw Y axis labels
		g.setColor(Color.BLACK);
		for (int i=0 ; i < labelsY.length ; i++){
			int x = chartStartX - 8 - (int)g.getFontMetrics().getStringBounds(labelsY[i],g).getWidth();
			int y = chartStartY + chartHeight - ((i+1) * heightDivisionY);
			g.drawString(labelsY[i], x, y);
			x = chartStartX;
			y = y - ((int)(g.getFontMetrics().getStringBounds(labelsY[i],g).getHeight())-3)/2;
			g.drawLine(x-2, y, x+2, y);
		}
		
		for (int i=0 ; i < categories.length ; i++){
			//Draw bars
			int x = chartStartX + widthGapX + (i*widthDivisionX);
			int height = (int)((values[i] / valDivisionY) * heightDivisionY);
			int y = chartStartY + chartHeight - height;
			if (showMeanAndSD || !coloredCategories) {
				if (showMeanAndSD && values[i] < mean - sd) g.setColor(Resources.getTableEvenRowBackgroundColor(Palette.Red));
				else if (showMeanAndSD && values[i] > mean + sd) g.setColor(Resources.getTableEvenRowBackgroundColor(Palette.Green));
				else g.setColor(Resources.getTableEvenRowBackgroundColor(Palette.Blue));		
			}else {
				g.setColor(colors[i]);
			}
			g.fillRect(x, y, widthDivisionX-widthGapX, height);
			g.setColor(Color.BLACK);		
			g.drawRect(x, y, widthDivisionX-widthGapX, height);
			
			//Draw bars values
			int midBarX = x+(widthDivisionX-widthGapX)/2;
			g.setColor(Color.BLACK);
			g.setFont(bigFont);
			int l = (int) g.getFontMetrics().getStringBounds(Tools.doubleToString(values[i],2,false), g).getWidth() + 8 ;
			if (l > (widthDivisionX-widthGapX)) {
				//need to rotate the text, bar not wide enough
				int yTxt = y;
				if (y + l >= chartStartY + chartHeight){
					yTxt = y - l - 8;
				}
				g.rotate(Math.toRadians(-90),midBarX+2,yTxt);
				g.setColor(Color.BLACK);
				//for outilined text
				g.drawString(Tools.doubleToString(values[i],2,false), midBarX+2-l+1, yTxt+1);
				g.drawString(Tools.doubleToString(values[i],2,false), midBarX+2-l-1, yTxt+1);
				g.drawString(Tools.doubleToString(values[i],2,false), midBarX+2-l+1, yTxt-1);
				g.drawString(Tools.doubleToString(values[i],2,false), midBarX+2-l-1, yTxt-1);
				g.setColor(Color.WHITE);
				g.drawString(Tools.doubleToString(values[i],2,false), midBarX+2-l, yTxt);
				g.rotate(Math.toRadians(90),midBarX+2,yTxt);
			}else {
				int h = (int) g.getFontMetrics().getStringBounds(Tools.doubleToString(values[i],2,false), g).getHeight();
				int xTxt = x+((widthDivisionX-widthGapX)-l)/2;
				int yTxt = y + h;
				if (yTxt + 4 >= chartStartY + chartHeight){
					yTxt = y - 8;
				}
				g.setColor(Color.BLACK);
				//for outilined text
				g.drawString(Tools.doubleToString(values[i],2,false), xTxt+1, yTxt+1);
				g.drawString(Tools.doubleToString(values[i],2,false), xTxt-1, yTxt+1);
				g.drawString(Tools.doubleToString(values[i],2,false), xTxt+1, yTxt-1);
				g.drawString(Tools.doubleToString(values[i],2,false), xTxt-1, yTxt-1);
				g.setColor(Color.WHITE);
				g.drawString(Tools.doubleToString(values[i],2,false), xTxt, yTxt);
			}
			
			//Draw X axis labels
			g.setFont(smallFont);
			g.setColor(Color.black);
      g.drawLine(midBarX, y+height-2, midBarX, y+height+2);
			x += widthDivisionX/2; 
      y += height + 8;
			l = (int) g.getFontMetrics().getStringBounds(categories[i], g).getWidth() + 8 ;
      g.rotate(Math.toRadians(-25),x,y);
      g.drawString(categories[i], x-l, y);
      g.rotate(Math.toRadians(25),x,y);
		}
		
		//Draw mean
		if(showMeanAndSD) {
			g.setColor(Color.RED);
			int heightMean = chartStartY + chartHeight - ((int)((mean / valDivisionY) * heightDivisionY));
			g.setStroke(thickStroke);
			g.drawLine(chartStartX, heightMean, chartStartX + chartWidth, heightMean);
			int heightSD = (int)((sd / valDivisionY) * heightDivisionY);
			g.setStroke(dashedStroke);
			g.drawLine(chartStartX, heightMean+heightSD, chartStartX + chartWidth, heightMean+heightSD);
			g.drawLine(chartStartX, heightMean-heightSD, chartStartX + chartWidth, heightMean-heightSD);
			g.setStroke(normalStroke);
		}
		
		//Draw axis
		g.setColor(Color.BLACK);
		g.drawLine(chartStartX, chartStartY, chartStartX, chartStartY + chartHeight);
		g.drawLine(chartStartX, chartStartY + chartHeight, chartStartX + chartWidth, chartStartY + chartHeight);
		
		//Draw axis labels
		g.setColor(Color.BLACK);
		g.drawString(labelX, (int)dim.getWidth() - 20 - (int)g.getFontMetrics().getStringBounds(labelX,g).getWidth(), chartStartY + chartHeight + 5 + (int)g.getFontMetrics().getStringBounds(labelX,g).getHeight());
		g.drawString(labelY, chartStartX - 3 - (int)g.getFontMetrics().getStringBounds(labelY,g).getWidth(), 10 + (int)g.getFontMetrics().getStringBounds(labelY,g).getHeight());
		
		//Draw title
		g.setFont(bigFont);
		g.drawString(title, chartStartX + (chartWidth-(int)g.getFontMetrics().getStringBounds(title,g).getWidth())/2, 20);
	}

}
