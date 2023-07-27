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

package be.uclouvain.ngs.highlander.UI.details;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.table.MultiLineTableCellRenderer;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.Insilico;
import be.uclouvain.ngs.highlander.database.Field.Mosaicism;
import be.uclouvain.ngs.highlander.database.Field.Reporting;
import be.uclouvain.ngs.highlander.database.Field.Segregation;
import be.uclouvain.ngs.highlander.database.Field.Validation;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
 * Evaluations given to selected variant in all samples, so user can compare with existing data.
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxOtherEvaluations extends DetailsBox {

	private static boolean showNonEvaluated = false;
	
	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;
	private boolean detailled;
	Map<String, Set<String>> map;
	private JTable table;
	
	public DetailsBoxOtherEvaluations(int variantId, DetailsPanel mainPanel, boolean detailled){
		this.variantSampleId = variantId;
		this.mainPanel = mainPanel;
		this.detailled = detailled;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	@Override
	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	@Override
	public String getTitle(){
		return "Samples with same evaluated variant" + (detailled?" (details)":" (count)");
	}

	@Override
	public Palette getColor() {
		return Field.evaluation.getCategory().getColor();
	}

	@Override
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	@Override
	protected void loadDetails(){
		try{
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			String chr = "";
			int pos = 0;
			int length = 0;
			String reference = "";
			String alternative = "";
			String gene_symbol = "";
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT chr, pos, length, reference, alternative, gene_symbol "
					+ "FROM " + analysis.getFromSampleAnnotations() 
					+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + variantSampleId)){
				if (res.next()){
					chr = res.getString("chr");
					pos = res.getInt("pos");
					length = res.getInt("length");
					reference = res.getString("reference");
					alternative = res.getString("alternative");
					gene_symbol = res.getString("gene_symbol");
				}
			}
			if (pos >= 0){
				map = new TreeMap<>();
				Map<String, Map<String, Integer>> count = new TreeMap<>();
				Map<Field,String> values = new HashMap<Field, String>();
				int total = 0;
				values.put(Field.evaluation, null);
				values.put(Field.check_insilico, null);
				values.put(Field.reporting, null);
				values.put(Field.check_validated_variant, null);
				values.put(Field.check_somatic_variant, null);
				values.put(Field.check_segregation, null);
				values.put(Field.evaluation_comments, null);
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT "
								+ Field.evaluation.getQuerySelectName(analysis, false)+", "+Field.evaluation_username.getQuerySelectName(analysis, false)+", "+Field.evaluation_date.getQuerySelectName(analysis, false)+", "
								+ Field.check_insilico.getQuerySelectName(analysis, false)+", "+Field.check_insilico_username.getQuerySelectName(analysis, false)+", "+Field.check_insilico_date.getQuerySelectName(analysis, false)+", "
								+ Field.reporting.getQuerySelectName(analysis, false)+", "+Field.reporting_username.getQuerySelectName(analysis, false)+", "+Field.reporting_date.getQuerySelectName(analysis, false)+", "
								+ Field.check_validated_variant.getQuerySelectName(analysis, false)+", "+Field.check_validated_variant_username.getQuerySelectName(analysis, false)+", "+Field.check_validated_variant_date.getQuerySelectName(analysis, false)+", "
								+ Field.check_somatic_variant.getQuerySelectName(analysis, false)+", "+Field.check_somatic_variant_username.getQuerySelectName(analysis, false)+", "+Field.check_somatic_variant_date.getQuerySelectName(analysis, false)+", "
								+ Field.check_segregation.getQuerySelectName(analysis, false)+", "+Field.check_segregation_username.getQuerySelectName(analysis, false)+", "+Field.check_segregation_date.getQuerySelectName(analysis, false)+", "
								+ Field.evaluation_comments.getQuerySelectName(analysis, false)+", "+Field.evaluation_comments_username.getQuerySelectName(analysis, false)+", "+Field.evaluation_comments_date.getQuerySelectName(analysis, false)+", "
								+ Field.sample.getQuerySelectName(analysis, false)+" "
								+	"FROM " + analysis.getFromSampleAnnotations()
								+ analysis.getJoinProjects()
								+ analysis.getJoinUserAnnotationsEvaluations()
								+ "WHERE `chr` = '"+chr+"' AND `pos` = "+pos+" AND `length` = "+length+" AND "
								+ "`reference` = '"+reference+"' AND `alternative` = '"+alternative+"' AND `gene_symbol` = '"+gene_symbol+"'"
						)){
					while (res.next()){
						total++;
						String val_sample = res.getString(Field.sample.getName());
						for (Field field : values.keySet()) {
							if (field == Field.evaluation) {
								String val_evaluation = res.getString(Field.evaluation.getName());
								if (val_evaluation.equals("0")) val_evaluation = "Evaluation - Not evaluated";
								if (val_evaluation.equals("1")) val_evaluation = "Evaluation - Type I";
								if (val_evaluation.equals("2")) val_evaluation = "Evaluation - Type II";
								if (val_evaluation.equals("3")) val_evaluation = "Evaluation - Type III";
								if (val_evaluation.equals("4")) val_evaluation = "Evaluation - Type IV";
								if (val_evaluation.equals("5")) val_evaluation = "Evaluation - Type V";
								values.put(field, val_evaluation);
							}else if (field == Field.check_insilico) {
								values.put(field, "Check insilico - " + res.getString(Field.check_insilico.getName()));
							}else if (field == Field.reporting) {
								values.put(field, "Reporting - " + res.getString(Field.reporting.getName()));
							}else if (field == Field.check_validated_variant) {
								values.put(field, "Validation - " + res.getString(Field.check_validated_variant.getName()));
							}else if (field == Field.check_somatic_variant) {
								values.put(field, "Mosaicism - " + res.getString(Field.check_somatic_variant.getName()));
							}else if (field == Field.check_segregation) {
								values.put(field, "Segregation - " + res.getString(Field.check_segregation.getName()));
							}else if (field == Field.evaluation_comments) {
								String val_evaluation_comments = "Comments - " + res.getString(Field.evaluation_comments.getName());
								if (val_evaluation_comments.length() <= "Comments - ".length()){
									val_evaluation_comments = "Comments - Not commented";
								}
								values.put(field, val_evaluation_comments);
							}
							if (!map.containsKey(values.get(field))){
								map.put(values.get(field), new TreeSet<String>());
								count.put(values.get(field), new TreeMap<>());
							}
						}
						for (Field field : values.keySet()) {
							if (detailled) {
								map.get(values.get(field)).add(val_sample 
										+ ((res.getString(field.getName()+"_username")!=null)?
												" ("+res.getString(field.getName()+"_username")+" on "+res.getString(field.getName()+"_date")+")":""));								
							}else {
								String username = (res.getString(field.getName()+"_username")!=null) ? res.getString(field.getName()+"_username") : "nobody";
								if (!count.get(values.get(field)).containsKey(username)) {
									count.get(values.get(field)).put(username, 0);
								}
								count.get(values.get(field)).put(username, count.get(values.get(field)).get(username)+1);
							}
						}
					}
				}
				detailsPanel.removeAll();
				if (detailled) {
					table = (showNonEvaluated) ? getTable(map) : getLightTable(map);
					JToggleButton button = new JToggleButton("Show non-evaluated");
					button.setSelected(showNonEvaluated);
					button.addItemListener(new ItemListener() {
						@Override
						public void itemStateChanged(ItemEvent e) {
							detailsPanel.remove(table);
							if (e.getStateChange() == ItemEvent.SELECTED) {
								showNonEvaluated = true;
								table = getTable(map);
							}else {
								showNonEvaluated = false;
								table = getLightTable(map);
							}
							detailsPanel.add(table, BorderLayout.CENTER);
							detailsPanel.revalidate();
						}
					});
					detailsPanel.add(button, BorderLayout.NORTH);
				}else {
					for (String value : count.keySet()) {
						for (String username : count.get(value).keySet()) {
							map.get(value).add(count.get(value).get(username) + "/"+total+" ("+username+")");
						}
					}					
					table = getTable(map);
				}
				detailsPanel.add(table, BorderLayout.CENTER);					
			}else{
				detailsPanel.removeAll();
				detailsPanel.add(new JLabel("Variant was not found in the database"), BorderLayout.CENTER);			
			}
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

	private JTable getLightTable(Map<String, Set<String>> map) {
		Map<String, Set<String>> light = new HashMap<>(map);
		light.remove("Evaluation - Not evaluated");
		light.remove("Check insilico - NOT_CHECKED");
		light.remove("Reporting - NOT_CHECKED");
		light.remove("Validation - NOT_CHECKED");
		light.remove("Mosaicism - NOT_CHECKED");
		light.remove("Segregation - NOT_CHECKED");
		light.remove("Comments - Not commented");
		return getTable(light);
	}
	
	private JTable getTable(Map<String, Set<String>> map){
		String[][] data;
		if (map.isEmpty()){
			data = new String[1][1];
			data[0][0] = "No evaluation found";
		}else{
			data = new String[map.size()][2];
			int r=0;
			for (String evaluation : map.keySet()){
				try{
					data[r][0] = evaluation;
				}catch(Exception ex){
					Tools.exception(ex);
					data[r][0] = "ERROR";
				}
				StringBuilder sb = new StringBuilder();
				for (String sample : map.get(evaluation)){
					sb.append(sample + ", ");					
				}
				sb.delete(sb.length()-2, sb.length());
				data[r][1] = sb.toString();
				r++;
			}
		}

		final DetailsTableModel model = new DetailsTableModel(data);
		JTable table = new JTable(model){
			@Override
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				int colIndex = columnAtPoint(p);
				Object val = "";
				val = getValueAt(rowIndex, colIndex);
				tip = (val != null) ? val.toString() : "";
				return tip;
			}
		};
		table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());	
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(true);			
		table.setCellSelectionEnabled(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		//Fit the first (field) column size to the biggest cell, letting all remaining space to the second (data) column
		if (table.getRowCount() > 0){
			int maxWidth = 0;
			for (int row = 0 ; row < table.getRowCount() ; row++){
				JTextArea textArea = (JTextArea)(table.getCellRenderer(row, 0).getTableCellRendererComponent(table, table.getValueAt(row, 0),false, false, row, 0));
				if (!textArea.getText().startsWith("Comments")){
					maxWidth = Math.max(maxWidth, new JLabel(textArea.getText()).getPreferredSize().width + 5);
				}
			}
			TableColumn tc = table.getColumnModel().getColumn(0);
			tc.setPreferredWidth(maxWidth);
			tc.setMaxWidth(maxWidth);
		}

		return table;
	}

	private class ColoredTableCellRenderer extends MultiLineTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JTextArea textArea = (JTextArea) comp;
			if (row%2 == 0) textArea.setBackground(Resources.getTableEvenRowBackgroundColor(getColor()));
			else textArea.setBackground(Color.WHITE);
			textArea.setForeground(Color.black);
			textArea.setBorder(new LineBorder(Color.WHITE));

			if (column == 0){
				String evaluation = value.toString();
				if (evaluation.equals("Check insilico - " + Insilico.OK)) textArea.setBackground(Color.green);
				else if (evaluation.equals("Check insilico - " + Insilico.SUSPECT)) textArea.setBackground(Color.orange);
				else if (evaluation.equals("Check insilico - " + Insilico.NOT_OK)) textArea.setBackground(Color.red);
				else if (evaluation.equals("Validation - " + Validation.VALIDATED)) textArea.setBackground(Color.green);
				else if (evaluation.equals("Validation - " + Validation.SUSPECT)) textArea.setBackground(Color.orange);
				else if (evaluation.equals("Validation - " + Validation.INVALIDATED)) textArea.setBackground(Color.red);
				else if (evaluation.equals("Mosaicism - " + Mosaicism.SOMATIC)) textArea.setBackground(Color.green);
				else if (evaluation.equals("Mosaicism - " + Mosaicism.DUBIOUS)) textArea.setBackground(Color.orange);
				else if (evaluation.equals("Mosaicism - " + Mosaicism.GERMLINE)) textArea.setBackground(Color.red);
				else if (evaluation.equals("Segregation - " + Segregation.COSEG)) textArea.setBackground(Color.green);
				else if (evaluation.equals("Segregation - " + Segregation.CARRIERS)) textArea.setBackground(Color.green);
				else if (evaluation.equals("Segregation - " + Segregation.NO_COSEG)) textArea.setBackground(Color.red);
				else if (evaluation.equals("Segregation - " + Segregation.NO_COSEG_OTHER)) textArea.setBackground(Color.red);
				else if (evaluation.equals("Evaluation - Type V")) textArea.setBackground(new Color(192,0,0));
				else if (evaluation.equals("Evaluation - Type IV")) textArea.setBackground(new Color(228,108,10));
				else if (evaluation.equals("Evaluation - Type III")) textArea.setBackground(new Color(112,48,160));
				else if (evaluation.equals("Evaluation - Type II")) textArea.setBackground(new Color(79,129,189));
				else if (evaluation.equals("Evaluation - Type I")) textArea.setBackground(new Color(0,176,80));				
				else if (evaluation.equals("Reporting - " + Reporting.YES)) textArea.setBackground(Color.green);
				else if (evaluation.equals("Reporting - " + Reporting.NO)) textArea.setBackground(Color.red);
			}

			if (isSelected) {
				textArea.setBackground(new Color(51,153,255));
			}
			return textArea;
		}
	}

	public static class DetailsTableModel	extends AbstractTableModel {
		private String[][] data;

		public DetailsTableModel(String[][] data) {    	
			this.data = data;
		}

		@Override
		public int getColumnCount() {
			if (data.length == 0) return 0;
			return data[0].length;
		}

		@Override
		public String getColumnName(int col) {
			switch(col){
			case 0:
				return "Field";
			case 1:
				return "Value";
			default:
				return "Out of range columns";
			}
		}

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			if (row >= data.length || col >= data[row].length) return null;
			if (row < 0 || col < 0) return null;
			return data[row][col];
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}

}
