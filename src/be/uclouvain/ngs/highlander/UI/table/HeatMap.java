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

package be.uclouvain.ngs.highlander.UI.table;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTable;

import be.uclouvain.ngs.highlander.Resources;

public class HeatMap {

	public enum ColorRange {
		RGB_GREEN_TO_RED(Resources.iHeatMapRgbGR),
		RGB_RED_TO_GREEN(Resources.iHeatMapRgbRG),
		HSV_BLUE_TO_RED(Resources.iHeatMapHsvBR),
		HSV_RED_TO_BLUE(Resources.iHeatMapHsvRB),
		;
		private ImageIcon icon;
		private ColorRange(ImageIcon icon){this.icon = icon;}
		public JPanel getExampleRange(){
			JPanel panel = new JPanel(){
				@Override
				public void paintComponent(Graphics g) {
					Dimension dim = this.getSize();
					int min = 0;
					int max = dim.width;
					for (int x=0 ; x < max ; x++){
						g.setColor(getColor(ColorRange.this, new Integer(x), new Integer(min), new Integer(max), Integer.class));
						g.fillRect(x, 0, 1, dim.height);
					}
				}
			};
			return panel;
		}
		public ImageIcon getIcon(){
			return icon;
		}
	}

	public enum ConversionMethod {
		SORTING("Distinct values are sorted and color is relative to the value position in this sorted list"),
		RANGE_DATA("Minimum and maximum values are extracted from the data to create a range and color is relative to the value position in this range"),
		RANGE_GIVEN("Minimum and maximum values given in arguments are used to create a range and color is relative to the value position in this range"),
		;
		private final String definition;
	  ConversionMethod(String definition){this.definition = definition;}
	  public String getDefinition(){return definition;}
	}
	
	private final JTable table;	
	private final Map<Integer, ColorRange> colorRanges = new HashMap<Integer, ColorRange>();
	private final Map<Integer, ConversionMethod> conversionMethods = new HashMap<Integer, ConversionMethod>();
	private final Map<Integer, Object> minima = new HashMap<Integer, Object>();
	private final Map<Integer, Object> maxima = new HashMap<Integer, Object>();
	private final Map<Integer, List<Object>> sortedVals = new HashMap<Integer, List<Object>>();
	
	public HeatMap(JTable table){
		this.table = table;
	}
		
	public void setHeatMap(int column, ColorRange range, ConversionMethod method, String mininum, String maximum){
		colorRanges.put(column, range);
		conversionMethods.put(column, method);
		if (method == ConversionMethod.RANGE_DATA || method == ConversionMethod.RANGE_GIVEN){
			Object min = 0, max = 0;
			if (method == ConversionMethod.RANGE_DATA){
				int firstNonNullRow = 0;
				do{
					min = table.getValueAt(firstNonNullRow, column);
					max = table.getValueAt(firstNonNullRow, column);
					firstNonNullRow++;
				}while (table.getRowCount() > firstNonNullRow && min == null);
				for (int row = firstNonNullRow ; row < table.getRowCount() ; row++){
					Object val = table.getValueAt(row, column);
					Class<?> colClass = table.getColumnClass(column);
					if (val != null){
						if (colClass == OffsetDateTime.class){
							if (((OffsetDateTime)val).isBefore((OffsetDateTime)min)) min = val;
							if (((OffsetDateTime)val).isAfter((OffsetDateTime)max)) max = val;
						}else if (colClass == Integer.class){
							if (((Integer)val).intValue() < ((Integer)min).intValue()) min = val;
							if (((Integer)val).intValue() > ((Integer)max).intValue()) max = val;
						}else if (colClass == Long.class){
							if (((Long)val).longValue() < ((Long)min).longValue()) min = val;
							if (((Long)val).longValue() > ((Long)max).longValue()) max = val;
						}else if (colClass == Double.class){
							if (((Double)val).doubleValue() < ((Double)min).doubleValue()) min = val;
							if (((Double)val).doubleValue() > ((Double)max).doubleValue()) max = val;
						}else {
							if (val.toString().compareTo(min.toString()) < 0) min = val;
							if (val.toString().compareTo(max.toString()) > 0) max = val;
						}
					}
				}
			}else if (method == ConversionMethod.RANGE_GIVEN){
				Class<?> colClass = table.getColumnClass(column);
				if (colClass == OffsetDateTime.class){
					min = OffsetDateTime.parse(mininum);
					max = OffsetDateTime.parse(maximum);
				}else if (colClass == Integer.class){
					min = Integer.parseInt(mininum);
					max = Integer.parseInt(maximum);
				}else if (colClass == Long.class){
					min = Long.parseLong(mininum);
					max = Long.parseLong(maximum);
				}else if (colClass == Double.class){
					min = Double.parseDouble(mininum);
					max = Double.parseDouble(maximum);
				}else {
					min = mininum;
					max = maximum;
				}
			}
			minima.put(column, min);
			maxima.put(column, max);
		}else if (method == ConversionMethod.SORTING){
			Set<Object> set = new TreeSet<Object>();
			for (int row = 0 ; row < table.getRowCount() ; row++){
				if (table.getValueAt(row, column) != null) set.add(table.getValueAt(row, column));
			}
			sortedVals.put(column, new ArrayList<Object>(set));
		}
	}
	
	public void setHeatMap(int column, ColorRange range, ConversionMethod method){
		setHeatMap(column, range, method, "0", "0");
	}
		
	public boolean hasHeatMap(int col){
		return (conversionMethods.containsKey(col));
	}
	
	public Color getColor(int viewRow, int viewColumn){
    int col = table.convertColumnIndexToModel(viewColumn);
		if (conversionMethods.containsKey(col)){
			if (conversionMethods.get(col) == ConversionMethod.SORTING){
				return getColor(colorRanges.get(col), table.getValueAt(viewRow, viewColumn), sortedVals.get(col), table.getColumnClass(viewColumn));
			}else{
				return getColor(colorRanges.get(col), table.getValueAt(viewRow, viewColumn), minima.get(col), maxima.get(col), table.getColumnClass(viewColumn));
			}
		}else{
			return Color.WHITE;
		}
	}

	public Color getColor(Object value, int modelColumn){
		if (conversionMethods.containsKey(modelColumn)){
			if (conversionMethods.get(modelColumn) == ConversionMethod.SORTING){
				return getColor(colorRanges.get(modelColumn), value, sortedVals.get(modelColumn), table.getModel().getColumnClass(modelColumn));
			}else{
				return getColor(colorRanges.get(modelColumn), value, minima.get(modelColumn), maxima.get(modelColumn), table.getModel().getColumnClass(modelColumn));
			}
		}else{
			return Color.WHITE;
		}
	}
	
	public static Color getColor(ColorRange colorRange, Object value, List<Object> sortedValues, Class<?> dataClass){
		int min = 0;
		int max = sortedValues.size()-1;
		int val = sortedValues.indexOf(value);
		return getColor(colorRange, val, min, max, Integer.class);
	}
	
	public static Color getColor(ColorRange colorRange, Object value, Object minVal, Object maxVal, Class<?> dataClass){
		double rangeLength = 0;
		double bestVal = 0;
		switch(colorRange){
		case HSV_BLUE_TO_RED:
			rangeLength = 100;
			bestVal = 0;
			break;
		case HSV_RED_TO_BLUE:
			rangeLength = 100;
			bestVal = rangeLength;
			break;
		case RGB_GREEN_TO_RED:
			rangeLength = 510;
			bestVal = 0;
			break;
		case RGB_RED_TO_GREEN:
			rangeLength = 510;
			bestVal = rangeLength;
			break;
		}		
		double totalColor;
		if (dataClass == OffsetDateTime.class){
			OffsetDateTime min = (OffsetDateTime)minVal;
			OffsetDateTime max = (OffsetDateTime)maxVal;
			OffsetDateTime val = (OffsetDateTime)value;
			double range = (max.compareTo(min));
			if (range > 0){
				double ratio = rangeLength/range;
				totalColor = max.compareTo(val) * ratio;
			}else{
				totalColor = bestVal;
			}
		}else if (dataClass == Integer.class){
			int min = (Integer)minVal;
			int max = (Integer)maxVal;
			int val = (Integer)value;
			double range = max-min;
			if (range > 0){
				double ratio = rangeLength/range;
				totalColor = (val-min) * ratio;
			}else{
				totalColor = bestVal;
			}
		}else if (dataClass == Long.class){
			long min = (Long)minVal;
			long max = (Long)maxVal;
			long val = (Long)value;
			double range = max-min;
			if (range > 0){
				double ratio = rangeLength/range;
				totalColor = (val-min) * ratio;
			}else{
				totalColor = bestVal;
			}
		}else if (dataClass == Double.class){
			double min = (Double)minVal;
			double max = (Double)maxVal;
			double val = (Double)value;
			double range = max-min;
			if (range > 0){
				double ratio = rangeLength/range;
				totalColor = (val-min) * ratio;
			}else{
				totalColor = bestVal;
			}
		}else{
			String min = minVal.toString();
			String max = maxVal.toString();
			String val = value.toString();
			double range = (max.compareTo(min));
			if (range > 0){
				double ratio = rangeLength/range;
				totalColor = val.compareTo(min) * ratio;
			}else{
				totalColor = bestVal;
			}
		}
  	if (colorRange == ColorRange.HSV_BLUE_TO_RED || colorRange == ColorRange.HSV_RED_TO_BLUE){
  		if (colorRange == ColorRange.HSV_BLUE_TO_RED){
  			totalColor = 100.0 - totalColor;			
  		}
  		if (totalColor > 100.0) totalColor = 100.0;
			if (totalColor < 0.0) totalColor = 0.0;
  		double H = totalColor * 0.007 ; // Hue (note 0.7 = Blue)
  		double S = 0.9; // Saturation
  		double B = 0.9; // Brightness
  		return Color.getHSBColor((float)H, (float)S, (float)B);
  	}else if (colorRange == ColorRange.RGB_GREEN_TO_RED || colorRange == ColorRange.RGB_RED_TO_GREEN){
  		if (totalColor > 510.0) totalColor = 510.0;
			if (totalColor < 0.0) totalColor = 0.0;
			int red = (totalColor <= 255) ? 255 : 255-((int)totalColor-255);
			int green = (totalColor >= 255) ? 255 : (int)totalColor;
  		if (red < 0) red = 0;
  		if (red > 255) red = 255;
  		if (green < 0) green = 0;
  		if (green > 255) green = 255;
  		if (colorRange == ColorRange.RGB_GREEN_TO_RED){
  			int swap = red;
  			red = green;
  			green = swap;
  		}
			return new Color(red,green,80);
  	}else{
  		return Color.WHITE;
  	}
	}
	
}
