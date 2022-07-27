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
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.users.User.TargetColor;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Field.Insilico;
import be.uclouvain.ngs.highlander.database.Field.Mosaicism;
import be.uclouvain.ngs.highlander.database.Field.Reporting;
import be.uclouvain.ngs.highlander.database.Field.Segregation;
import be.uclouvain.ngs.highlander.database.Field.Tag;
import be.uclouvain.ngs.highlander.database.Field.Validation;
import be.uclouvain.ngs.highlander.datatype.HeatMapCriterion;
import be.uclouvain.ngs.highlander.datatype.HighlightCriterion;

public class CellRenderer {

	private Map<String, List<HighlightCriterion>> cellHighlighting = new HashMap<String, List<HighlightCriterion>>();
	private List<HighlightCriterion> rowHighlighting = new ArrayList<HighlightCriterion>();
	private HeatMap heatMap;
	VariantsTable table;
	private Map<String, HeatMapCriterion> heatMapCrits = new HashMap<String, HeatMapCriterion>();
	private Map<String,JTable> tables = new HashMap<String,JTable>();
	
	private Map<String,XSSFCellStyle> cellStyles = new HashMap<String, XSSFCellStyle>();
	
	public void registerTableForHeatMap(VariantsTable table){
		this.table = table;
		heatMap = new HeatMap(table.getTable());
		for (HeatMapCriterion crit : heatMapCrits.values()){
			addHeatMap(crit);
		}
	}
		
	public void addHeatMap(HeatMapCriterion crit){
		heatMapCrits.put(crit.getFieldName(), crit);
		heatMap.setHeatMap(table.getColumnIndex(crit.getField()), crit.getColorRange(), crit.getConversionMethod(), crit.getMinimum(), crit.getMaximum());
		table.getTable().repaint();
		table.getTable().revalidate();
	}
	
	public void removeHeatMap(HeatMapCriterion crit){
		heatMapCrits.remove(crit.getFieldName());
		table.getTable().repaint();
		table.getTable().revalidate();
	}
	
	public void clearHeatMaps(){
		heatMapCrits.clear();
		if (table != null){
			table.getTable().repaint();
			table.getTable().revalidate();
		}
	}
	
	public void registerTableForHighlighting(String key, JTable table){
		tables.put(key, table);
	}
	
	public void unregisterTableForHighlighting(String key){
		tables.remove(key);
	}
	
	public void addHighlighting(HighlightCriterion crit){
		if (crit.expandRow()) {
			rowHighlighting.add(crit);
		}else {
			if (!cellHighlighting.containsKey(crit.getFieldName())){
				cellHighlighting.put(crit.getFieldName(), new ArrayList<HighlightCriterion>());
			}
			cellHighlighting.get(crit.getFieldName()).add(crit);
		}
		for (JTable table : tables.values()){
			table.repaint();
			table.revalidate();
		}
	}
	
	public boolean removeHighlighting(HighlightCriterion crit){
		if (crit.expandRow()) {
			if(rowHighlighting.remove(crit)){
				for (JTable table : tables.values()){
					table.repaint();
					table.revalidate();
				}
				return true;
			}
		}else {
			if (cellHighlighting.containsKey(crit.getFieldName())){
				if(cellHighlighting.get(crit.getFieldName()).remove(crit)){
					if (cellHighlighting.get(crit.getFieldName()).isEmpty()){
						cellHighlighting.remove(crit.getFieldName());
					}
					for (JTable table : tables.values()){
						table.repaint();
						table.revalidate();
					}
					return true;
				}			
			}			
		}
		return false;
	}
	
	public void clearHighlighting(){
		rowHighlighting.clear();
		cellHighlighting.clear();
		for (JTable table : tables.values()){
			table.repaint();
			table.revalidate();
		}
	}
	
	public JLabel renderCell(JLabel label, Object value, Field field, int alignment, int row, boolean isSelected, Color evenColor, Color oddColor, boolean isVariantsTable){
		label.setHorizontalAlignment(alignment);
		Object unformattedValue = value;
    if (value == null) {
    	value = "";
    }else{
			if (field.hasTag(Tag.FORMAT_PERCENT_0)){
				value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 0);
			}else if (field.hasTag(Tag.FORMAT_PERCENT_2)){
				value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 2);
			}
    }
    
    if (row%2 == 0) label.setBackground(evenColor);
    else label.setBackground(oddColor);
    if (!isSelected) {
    	if (isVariantsTable) {
    		String uniqueVariantId = table.getUniqueVariantId(row);
    		for (String unique : table.getSelectedUniqueVariantId()) {
    			if (unique.equals(uniqueVariantId)) {
    				if (row%2 == 0) label.setBackground(Resources.getTableEvenRowBackgroundColor(((VariantsTable)table).getColor(TargetColor.SAME_VARIANT)));
    		    else label.setBackground(Resources.getTableOddRowBackgroundColor(((VariantsTable)table).getColor(TargetColor.SAME_VARIANT)));
    			}
    		}
    	}
    }
		label.setForeground(Color.black);
    label.setBorder(new LineBorder(Color.WHITE));
    if (value != null){
    	if (field.equals(Field.evaluation)
    			|| field.equals(Field.check_insilico)
    			|| field.equals(Field.reporting)
    			|| field.equals(Field.check_validated_variant)
    			|| field.equals(Field.check_somatic_variant)
    			|| field.equals(Field.check_segregation)
    			|| field.equals(Field.variant_of_interest)
    			|| field.equals(Field.gene_of_interest)
    			|| field.equals(Field.sample_of_interest)
    	){
    		if (value.toString().equalsIgnoreCase("true")) label.setBackground(Color.green);
    		else if (value.toString().equalsIgnoreCase("false")) label.setBackground(Color.red);
    		else if (value.toString().equalsIgnoreCase(Insilico.OK.toString())) label.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Insilico.SUSPECT.toString())) label.setBackground(Color.ORANGE);
    		else if (value.toString().equalsIgnoreCase(Insilico.NOT_OK.toString())) label.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Reporting.YES.toString())) label.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Reporting.NO.toString())) label.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Validation.VALIDATED.toString())) label.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Validation.SUSPECT.toString())) label.setBackground(Color.ORANGE);
    		else if (value.toString().equalsIgnoreCase(Validation.INVALIDATED.toString())) label.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Mosaicism.SOMATIC.toString())) label.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Mosaicism.DUBIOUS.toString())) label.setBackground(Color.ORANGE);
    		else if (value.toString().equalsIgnoreCase(Mosaicism.GERMLINE.toString())) label.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Segregation.COSEG.toString())) label.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Segregation.CARRIERS.toString())) label.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Segregation.NO_COSEG.toString())) label.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Segregation.NO_COSEG_OTHER.toString())) label.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase("5")) label.setBackground(new Color(192,0,0));
    		else if (value.toString().equalsIgnoreCase("4")) label.setBackground(new Color(228,108,10));
    		else if (value.toString().equalsIgnoreCase("3")) label.setBackground(new Color(112,48,160));
    		else if (value.toString().equalsIgnoreCase("2")) label.setBackground(new Color(79,129,189));
    		else if (value.toString().equalsIgnoreCase("1")) label.setBackground(new Color(0,176,80));
    	} 
    	if (field.equals(Field.allele_num)){
    		try{
    			if(value.toString().length() > 0 && Integer.parseInt(value.toString()) > 2){
    				label.setBackground(Color.red);
    			}
    		}catch(Exception ex){
    			Tools.exception(ex);
    		}
    	}

    	for (HighlightCriterion crit : rowHighlighting){
    		if (isVariantsTable || crit.getField().equals(field)) {
    			try{
    				Object valCrit = unformattedValue;
    				if (isVariantsTable) {
    					int colCrit = table.getColumnIndex(crit.getField());
    					valCrit = table.getTable().getValueAt(row, colCrit);
    				}
    				if (crit.isHighlighted(valCrit)){
    					if (crit.getBackground() != null) label.setBackground(crit.getBackground());
    					if (crit.getForeground() != null) label.setForeground(crit.getForeground());
    					if (crit.isBold()) label.setFont(label.getFont().deriveFont(Font.BOLD));
    					if (crit.isItalic()) label.setFont(label.getFont().deriveFont(Font.ITALIC));
    					if (crit.isBold() && crit.isItalic()) label.setFont(label.getFont().deriveFont(Font.BOLD | Font.ITALIC));
    				}
    			}catch(Exception ex){
    				Tools.exception(ex);
    			}
    		}
    	}

    	if (cellHighlighting.containsKey(field.getName())){
    		for (HighlightCriterion crit : cellHighlighting.get(field.getName())){
    			try{
    				if (crit.isHighlighted(unformattedValue)){
    					if (crit.getBackground() != null) label.setBackground(crit.getBackground());
    					if (crit.getForeground() != null) label.setForeground(crit.getForeground());
    					if (crit.isBold()) label.setFont(label.getFont().deriveFont(Font.BOLD));
    					if (crit.isItalic()) label.setFont(label.getFont().deriveFont(Font.ITALIC));
    					if (crit.isBold() && crit.isItalic()) label.setFont(label.getFont().deriveFont(Font.BOLD | Font.ITALIC));
    				}
    			}catch(Exception ex){
    				Tools.exception(ex);
    			}
    		}
    	}
    	
    	for (HeatMapCriterion crit : heatMapCrits.values()) {
    		if(crit.expandTable() && isVariantsTable) {
    			int colCrit = table.getColumnIndex(crit.getField());
    			Object valCrit = table.getTable().getValueAt(row, colCrit);
    			if (valCrit != null){
    				label.setBackground(heatMap.getColor(valCrit, table.getColumnIndex(crit.getField())));
    			}
    		}
    	}
    	
    	if (unformattedValue != null){
    		if (heatMapCrits.containsKey(field.getName())){
    			HeatMapCriterion crit = heatMapCrits.get(field.getName());
    			label.setBackground(heatMap.getColor(unformattedValue, table.getColumnIndex(crit.getField())));
    		}
    	}
    	
    	label.setText(value.toString());
    }      
    if (isSelected) {
      label.setBackground(new Color(51,153,255));
    }
    return label;
	}
	
	public JTextArea renderCell(JTextArea txtArea, Object value, Field field, int alignment, int row, boolean isSelected, Color evenColor, Color oddColor, boolean isVariantsTable){
		Object unformattedValue = value;
		if (value == null) {
			value = "";
		}else{
			if (field.hasTag(Tag.FORMAT_PERCENT_0)){
				value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 0);
			}else if (field.hasTag(Tag.FORMAT_PERCENT_2)){
				value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 2);
			}
		}
		
		if (row%2 == 0) txtArea.setBackground(evenColor);
		else txtArea.setBackground(oddColor);
		txtArea.setForeground(Color.black);
		txtArea.setBorder(new LineBorder(Color.WHITE));
		if (value != null){
			if (field.equals(Field.evaluation)
					|| field.equals(Field.check_insilico)
    			|| field.equals(Field.reporting)
    			|| field.equals(Field.check_validated_variant)
    			|| field.equals(Field.check_somatic_variant)
    			|| field.equals(Field.check_segregation)
    			|| field.equals(Field.variant_of_interest)
    			|| field.equals(Field.gene_of_interest)
    			|| field.equals(Field.sample_of_interest)
					){
				if (value.toString().equalsIgnoreCase("true")) txtArea.setBackground(Color.green);
				else if (value.toString().equalsIgnoreCase("false")) txtArea.setBackground(Color.red);
    		else if (value.toString().equalsIgnoreCase(Insilico.OK.toString())) txtArea.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Insilico.SUSPECT.toString())) txtArea.setBackground(Color.ORANGE);
    		else if (value.toString().equalsIgnoreCase(Insilico.NOT_OK.toString())) txtArea.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Reporting.YES.toString())) txtArea.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Reporting.NO.toString())) txtArea.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Validation.VALIDATED.toString())) txtArea.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Validation.SUSPECT.toString())) txtArea.setBackground(Color.ORANGE);
    		else if (value.toString().equalsIgnoreCase(Validation.INVALIDATED.toString())) txtArea.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Mosaicism.SOMATIC.toString())) txtArea.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Mosaicism.DUBIOUS.toString())) txtArea.setBackground(Color.ORANGE);
    		else if (value.toString().equalsIgnoreCase(Mosaicism.GERMLINE.toString())) txtArea.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Segregation.COSEG.toString())) txtArea.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Segregation.CARRIERS.toString())) txtArea.setBackground(Color.GREEN);
    		else if (value.toString().equalsIgnoreCase(Segregation.NO_COSEG.toString())) txtArea.setBackground(Color.RED);
    		else if (value.toString().equalsIgnoreCase(Segregation.NO_COSEG_OTHER.toString())) txtArea.setBackground(Color.RED);
				else if (value.toString().equalsIgnoreCase("5")) txtArea.setBackground(new Color(192,0,0));
				else if (value.toString().equalsIgnoreCase("4")) txtArea.setBackground(new Color(228,108,10));
				else if (value.toString().equalsIgnoreCase("3")) txtArea.setBackground(new Color(112,48,160));
				else if (value.toString().equalsIgnoreCase("2")) txtArea.setBackground(new Color(79,129,189));
				else if (value.toString().equalsIgnoreCase("1")) txtArea.setBackground(new Color(0,176,80));
			} 
			if (field.equals(Field.allele_num)){
				try{
					if(value.toString().length() > 0 && Integer.parseInt(value.toString()) > 2){
						txtArea.setBackground(Color.red);
					}
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
			
			for (HighlightCriterion crit : rowHighlighting){
				if (isVariantsTable || crit.getField().equals(field)) {
					try{
						Object valCrit = unformattedValue;
						if (isVariantsTable) {
							int colCrit = table.getColumnIndex(crit.getField());
							valCrit = table.getTable().getValueAt(row, colCrit);
						}
						if (crit.isHighlighted(valCrit)){
							if (crit.getBackground() != null) txtArea.setBackground(crit.getBackground());
							if (crit.getForeground() != null) txtArea.setForeground(crit.getForeground());
							if (crit.isBold()) txtArea.setFont(txtArea.getFont().deriveFont(Font.BOLD));
							if (crit.isItalic()) txtArea.setFont(txtArea.getFont().deriveFont(Font.ITALIC));
							if (crit.isBold() && crit.isItalic()) txtArea.setFont(txtArea.getFont().deriveFont(Font.BOLD | Font.ITALIC));
						}
					}catch(Exception ex){
						Tools.exception(ex);
					}
				}
			}
    	
			if (cellHighlighting.containsKey(field.getName())){
				for (HighlightCriterion crit : cellHighlighting.get(field.getName())){
					try{
						if (crit.isHighlighted(unformattedValue)){
							if (crit.getBackground() != null) txtArea.setBackground(crit.getBackground());
							if (crit.getForeground() != null) txtArea.setForeground(crit.getForeground());
							if (crit.isBold()) txtArea.setFont(txtArea.getFont().deriveFont(Font.BOLD));
							if (crit.isItalic()) txtArea.setFont(txtArea.getFont().deriveFont(Font.ITALIC));
							if (crit.isBold() && crit.isItalic()) txtArea.setFont(txtArea.getFont().deriveFont(Font.BOLD | Font.ITALIC));
						}
					}catch(Exception ex){
						Tools.exception(ex);
					}
				}
			}
			
			for (HeatMapCriterion crit : heatMapCrits.values()) {
    		if(crit.expandTable() && isVariantsTable) {
    			int colCrit = table.getColumnIndex(crit.getField());
    			Object valCrit = table.getTable().getValueAt(row, colCrit);
    			if (valCrit != null){
    				txtArea.setBackground(heatMap.getColor(valCrit, table.getColumnIndex(crit.getField())));
    			}
    		}
    	}
    	
    	if (unformattedValue != null){
    		if (heatMapCrits.containsKey(field.getName())){
    			HeatMapCriterion crit = heatMapCrits.get(field.getName());
    			txtArea.setBackground(heatMap.getColor(unformattedValue, table.getColumnIndex(crit.getField())));
    		}
    	}
			
			txtArea.setText(value.toString());
		}      
		if (isSelected) {
			txtArea.setBackground(new Color(51,153,255));
		}
		return txtArea;
	}
	
	public void clearCellStyles(){
		cellStyles.clear();
	}
	
	public void formatXlsCell(Object value, Field field, int alignment, Sheet sheet, Cell cell, int variantTableRow){
		if (field.hasTag(Tag.FORMAT_PERCENT_0)){
			value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 0);
		}else if (field.hasTag(Tag.FORMAT_PERCENT_2)){
			value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 2);
		}

		boolean formatPercent = field.hasTag(Tag.FORMAT_PERCENT_0) || field.hasTag(Tag.FORMAT_PERCENT_2);
		boolean bigNumber = (field.getFieldClass() == Long.class);
		Color backColor = null;
		Color foreColor = null;
		boolean bold = false;
		boolean italic = false;
		if (field.equals(Field.evaluation)
  			|| field.equals(Field.check_insilico)
  			|| field.equals(Field.reporting)
  			|| field.equals(Field.check_validated_variant)
  			|| field.equals(Field.check_somatic_variant)
  			|| field.equals(Field.check_segregation)
  			|| field.equals(Field.variant_of_interest)
  			|| field.equals(Field.gene_of_interest)
  			|| field.equals(Field.sample_of_interest)
  	){
  		if (value.toString().equalsIgnoreCase("true")) backColor = Color.green;
  		else if (value.toString().equalsIgnoreCase("false")) backColor = Color.red;
  		else if (value.toString().equalsIgnoreCase(Insilico.OK.toString())) backColor = Color.GREEN;
  		else if (value.toString().equalsIgnoreCase(Insilico.SUSPECT.toString())) backColor = Color.ORANGE;
  		else if (value.toString().equalsIgnoreCase(Insilico.NOT_OK.toString())) backColor = Color.RED;
  		else if (value.toString().equalsIgnoreCase(Reporting.YES.toString())) backColor = Color.GREEN;
  		else if (value.toString().equalsIgnoreCase(Reporting.NO.toString())) backColor = Color.RED;
  		else if (value.toString().equalsIgnoreCase(Validation.VALIDATED.toString())) backColor = Color.GREEN;
  		else if (value.toString().equalsIgnoreCase(Validation.SUSPECT.toString())) backColor = Color.ORANGE;
  		else if (value.toString().equalsIgnoreCase(Validation.INVALIDATED.toString())) backColor = Color.RED;
  		else if (value.toString().equalsIgnoreCase(Mosaicism.SOMATIC.toString())) backColor = Color.GREEN;
  		else if (value.toString().equalsIgnoreCase(Mosaicism.DUBIOUS.toString())) backColor = Color.ORANGE;
  		else if (value.toString().equalsIgnoreCase(Mosaicism.GERMLINE.toString())) backColor = Color.RED;
  		else if (value.toString().equalsIgnoreCase(Segregation.COSEG.toString())) backColor = Color.GREEN;
  		else if (value.toString().equalsIgnoreCase(Segregation.CARRIERS.toString())) backColor = Color.GREEN;
  		else if (value.toString().equalsIgnoreCase(Segregation.NO_COSEG.toString())) backColor = Color.RED;
  		else if (value.toString().equalsIgnoreCase(Segregation.NO_COSEG_OTHER.toString())) backColor = Color.RED;
  		else if (value.toString().equalsIgnoreCase("5")) backColor = new Color(192,0,0);
  		else if (value.toString().equalsIgnoreCase("4")) backColor = new Color(228,108,10);
  		else if (value.toString().equalsIgnoreCase("3")) backColor = new Color(112,48,160);
  		else if (value.toString().equalsIgnoreCase("2")) backColor = new Color(79,129,189);
  		else if (value.toString().equalsIgnoreCase("1")) backColor = new Color(0,176,80);
  	} 
		if (field.equals(Field.allele_num)){
			try{
				if(value.toString().length() > 0 && Integer.parseInt(value.toString()) > 2){
					backColor = Color.red;
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
  	for (HighlightCriterion crit : rowHighlighting){
			try{
				int colCrit = table.getColumnIndex(crit.getField());
				Object valCrit = table.getTable().getValueAt(variantTableRow, colCrit);
				if (crit.isHighlighted(valCrit)){
					if (crit.getBackground() != null) backColor = crit.getBackground();
					if (crit.getForeground() != null) foreColor = crit.getForeground();
					if (crit.isBold()) bold = true;
					if (crit.isItalic()) italic = true;
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
		if (cellHighlighting.containsKey(field.getName())){
			for (HighlightCriterion crit : cellHighlighting.get(field.getName())){
				try{
					if (crit.isHighlighted(value)){
						if (crit.getBackground() != null) backColor = crit.getBackground();
						if (crit.getForeground() != null) foreColor = crit.getForeground();
						if (crit.isBold()) bold = true;
						if (crit.isItalic()) italic = true;
					}
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		}
		if (heatMapCrits.containsKey(field.getName())){
			HeatMapCriterion crit = heatMapCrits.get(field.getName());
			backColor = (heatMap.getColor(value, table.getColumnIndex(crit.getField())));
		}
		String key = generateCellStyleKey(backColor, foreColor, bold, italic, formatPercent, bigNumber, alignment);		
		if (!cellStyles.containsKey(key)){
			cellStyles.put(key, createCellStyle(sheet, cell, backColor, foreColor, bold, italic, formatPercent, bigNumber, alignment));
		}
		cell.setCellStyle(cellStyles.get(key));
	}
	
	private XSSFCellStyle createCellStyle(Sheet sheet, Cell cell, Color back, Color fore, boolean bold, boolean italic, boolean percent, boolean bigNumber, int alignment){
		XSSFCellStyle cs = (XSSFCellStyle)sheet.getWorkbook().createCellStyle();
		if (back != null){
			cs.setFillPattern(CellStyle.SOLID_FOREGROUND);
			cs.setFillForegroundColor(new XSSFColor(back));  		
		}
		if (fore != null || bold || italic){
			XSSFFont font = (XSSFFont)sheet.getWorkbook().createFont();
			if (fore != null) font.setColor(new XSSFColor(fore));
			if (bold) font.setBold(true);
			if (italic) font.setItalic(true);
			cs.setFont(font);
		}
		if (percent){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("0%"));
		}
		if (bigNumber){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
		}
		if (alignment == JLabel.CENTER){
			cs.setAlignment(CellStyle.ALIGN_CENTER);
		}else if (alignment == JLabel.RIGHT){
			cs.setAlignment(CellStyle.ALIGN_RIGHT);
		}else{
			cs.setAlignment(CellStyle.ALIGN_LEFT);
		}
		return cs;
	}
	
	private String generateCellStyleKey(Color back, Color fore, boolean bold, boolean italic, boolean percent, boolean bigNumber, int alignment){
		StringBuilder sb = new StringBuilder();
		if (back != null) sb.append(back.getRGB() + "+");
		if (fore != null) sb.append(fore.getRGB() + "+");
		if (bold) sb.append("B+");
		if (italic) sb.append("I+");
		if (percent) sb.append("P+");
		if (bigNumber) sb.append("N+");
		sb.append(alignment+"");
		return sb.toString();
	}
}
