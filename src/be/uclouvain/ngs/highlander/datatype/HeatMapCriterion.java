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

package be.uclouvain.ngs.highlander.datatype;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateHeatMapCriterion;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ColorRange;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ConversionMethod;
import be.uclouvain.ngs.highlander.UI.toolbar.HighlightingPanel;
import be.uclouvain.ngs.highlander.database.Field;

public class HeatMapCriterion extends HighlightingRule {

	private HighlightingPanel highlightingPanel;

	private ColorRange colorRange;
	private ConversionMethod conversionMethod;
	private boolean expandTable;
	private String minimum;
	private String maximum;
	private JLabel label;
	private JLabel iconLab;

	public HeatMapCriterion(HighlightingPanel highlightingPanel, Field field, ColorRange colorRange, ConversionMethod conversionMethod, String minimum, String maximum, boolean expandTable){
		this.highlightingPanel = highlightingPanel;
		this.field = field;
		this.colorRange = colorRange;
		this.conversionMethod = conversionMethod;
		this.minimum = minimum;
		this.maximum = maximum;
		this.expandTable = expandTable;
		displayCriterionPanel();
	}

	public HeatMapCriterion(){

	}

	@Override
	public RuleType getRuleType(){
		return RuleType.HEATMAP;
	}

	public ColorRange getColorRange() {
		return colorRange;
	}

	public ConversionMethod getConversionMethod() {
		return conversionMethod;
	}

	public String getMinimum() {
		return minimum;
	}

	public String getMaximum() {
		return maximum;
	}

	public boolean expandTable() {
		return expandTable;
	}

	@Override
	public String getSaveString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getRuleType()+"|");
		sb.append(field.getName());
		sb.append("!");
		sb.append(colorRange.toString());
		sb.append("!");
		sb.append(conversionMethod.toString());
		sb.append("!");
		sb.append(""+(minimum.length() > 0 ? minimum : "-"));
		sb.append("!");
		sb.append(""+(maximum.length() > 0 ? minimum : "-"));
		sb.append("!");
		sb.append((expandTable)?"1":"0");
		return sb.toString();
	}

	@Override
	public JLabel parseSaveString(String saveString){
		String[] main = saveString.split("\\|");
		if (!main[0].equals(getRuleType().toString())){
			System.err.println("The following string is not an "+ getRuleType() +" : " + saveString);
			return new JLabel("Error !");
		}
		String[] parts = main[1].split("\\!");
		String f = parts[0];
		ColorRange col = ColorRange.valueOf(parts[1]);
		ConversionMethod con = ConversionMethod.valueOf(parts[2]);
		String min = parts[3];
		String max = parts[4];
		String method = "unknown";
		switch(con){
		case RANGE_DATA:
			method = "estimated range";
			break;
		case RANGE_GIVEN:
			method = "range ["+min+","+max+"]";
			break;
		case SORTING:
			method = "position sorting";
			break;
		}
		JLabel label = new JLabel(f + " ("+method+")", Resources.getScaledIcon(col.getIcon(), 16), SwingConstants.CENTER);
		return label;
	}

	@Override
	public HeatMapCriterion loadCriterion(HighlightingPanel highlightingPanel, String saveString) throws Exception {		
		String[] main = saveString.split("\\|");
		if (!main[0].equals(getRuleType().toString())){
			throw new Exception("The following string is not an "+ getRuleType() +" : " + saveString);
		}
		String[] parts = main[1].split("\\!");
		Field f = Field.getField(parts[0]);
		ColorRange col = ColorRange.valueOf(parts[1]);
		ConversionMethod con = ConversionMethod.valueOf(parts[2]);
		String min = parts[3];
		String max = parts[4];
		boolean e = (parts.length >= 6) ? (parts[5].equals("1")) : false;
		return new HeatMapCriterion(highlightingPanel, f, col, con, min, max, e);
	}

	private void displayCriterionPanel(){
		setLayout(new BorderLayout(6,1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
		String labtxt = ((field != null)?field.getName():"");
		label = new JLabel(labtxt);
		label.setOpaque(true);
		label.setToolTipText(toHtmlString());		
		add(label,BorderLayout.CENTER);
		JPanel west = new JPanel(new GridBagLayout());
		iconLab = new JLabel(Resources.getScaledIcon(colorRange.getIcon(), 16));		
		iconLab.setOpaque(true);
		west.add(iconLab, new GridBagConstraints(0, 0, 0, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
		add(west,BorderLayout.WEST);
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridBagLayout());
		JButton removeButton = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						delete();
					}
				}, "HeatMapCriterion.delete").start();
			}
		});
		removeButton.setToolTipText("Delete criterion");	
		removeButton.setBorder(null);
		removeButton.setBorderPainted(false);
		removeButton.setContentAreaFilled(false);
		removeButton.setMargin(new Insets(0, 0, 0, 0));
		buttonsPanel.add(removeButton,new GridBagConstraints(0, 0, 0, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
		add(buttonsPanel,BorderLayout.EAST);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editCriterion();
				}
			}
		});
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editCriterion();
				}
			}
		});
	}

	@Override
	public String toString(){
		StringBuilder details = new StringBuilder();
		details.append(field.getName());
		details.append(" (");
		String method = "unknown";
		switch(conversionMethod){
		case RANGE_DATA:
			method = "estimated range";
			break;
		case RANGE_GIVEN:
			method = "range ["+minimum+","+maximum+"]";
			break;
		case SORTING:
			method = "position sorting";
			break;
		}
		details.append(method);
		details.append(", " + colorRange + ")");
		if (expandTable) details.append(" on whole table"); 
		return details.toString();
	}

	@Override
	public String toHtmlString(){
		StringBuilder details = new StringBuilder();
		details.append("<html>");
		details.append(field.getName());
		details.append("<br>");
		String method = "unknown";
		switch(conversionMethod){
		case RANGE_DATA:
			method = "estimated range";
			break;
		case RANGE_GIVEN:
			method = "range ["+minimum+","+maximum+"]";
			break;
		case SORTING:
			method = "position sorting";
			break;
		}
		details.append(method);
		details.append("<br>");
		details.append(colorRange);
		details.append("<br>");
		if (expandTable) details.append("Affects whole table"); 
		else details.append("Affects field's column only"); 
		details.append("</html>");
		return details.toString();
	}

	@Override
	public void editCriterion(){
		CreateHeatMapCriterion cfc = new CreateHeatMapCriterion(Highlander.getCurrentAnalysis(), highlightingPanel, this);
		Tools.centerWindow(cfc, false);
		cfc.setVisible(true);
		HeatMapCriterion edited = cfc.getCriterion();
		Highlander.getCellRenderer().removeHeatMap(this);
		if(edited != null){			
			field = edited.getField();
			colorRange = edited.getColorRange();
			conversionMethod = edited.getConversionMethod();
			minimum = edited.getMinimum();
			maximum = edited.getMaximum();
			expandTable = edited.expandTable();
			String labtxt = ((field != null)?field.getName():"");
			label.setText(labtxt);		
			label.setToolTipText(toHtmlString());
			iconLab.setIcon(Resources.getScaledIcon(colorRange.getIcon(), 16));
		}
		Highlander.getCellRenderer().addHeatMap(this);
		highlightingPanel.refresh();		
	}

	@Override
	public void delete(){
		highlightingPanel.getCriteriaPanel().remove(HeatMapCriterion.this);
		Highlander.getCellRenderer().removeHeatMap(this);
		highlightingPanel.refresh();		
	}

}
