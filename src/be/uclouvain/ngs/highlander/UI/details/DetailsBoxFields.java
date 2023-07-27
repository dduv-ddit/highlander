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
import java.awt.Font;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.apache.commons.lang.WordUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.table.MultiLineTableCellRenderer;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

public class DetailsBoxFields extends DetailsBox {

	private DetailsPanel mainPanel;
	private final Category category;
	private JTable table;
	private boolean detailsLoaded = false;
	
	private Field highlightedField = null;

	/**
	 * Table with all the field names and values for a given category
	 * 
	 * @param variantId
	 * @param mainPanel
	 * @param category
	 */
	public DetailsBoxFields(int variantId, DetailsPanel mainPanel, Category category){
		this.variantSampleId = variantId;
		this.mainPanel = mainPanel;
		this.category = category;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	@Override
	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	@Override
	public String getTitle(){
		return WordUtils.capitalize(category.getName());
	}


	@Override
	public Palette getColor() {
		return category.getColor();
	}

	@Override
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	@Override
	protected void loadDetails(){
		try{
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			final List<Field> fields = new ArrayList<Field>();
			List<Object> values = new ArrayList<Object>();
			for (Field field : Field.getAvailableFields(analysis, false)){
				if (field.getCategory() == category) {
					fields.add(field);
				}
			}
			if (fields.isEmpty()){
				detailsPanel.removeAll();
				detailsPanel.add(new JLabel("This category is empty (no field visible in this analysis has been assigned to it)"), BorderLayout.CENTER);
			}else if (!category.hasGenericDetailBox()){
				//Use DetailsBoxPublicAnnotations and DetailsBoxPrivateAnnotations instead
				detailsPanel.removeAll();
				detailsPanel.add(new JLabel("Please use specialized detail box"), BorderLayout.CENTER);
			}else{
				boolean includeStatic = false; 
				boolean includeCustom = false; 
				boolean includeGene = false; 
				boolean includeProjects = false; 
				boolean includePathologies = false; 
				boolean includePopulations = false; 
				boolean includeAlleleFrequencies = false;
				boolean includeUserEvaluations = false;
				boolean includeUserNumEvaluations = false;
				boolean includeUserVariantsPublic = false;
				boolean includeUserVariantsPrivate = false;
				boolean includeUserGenesPublic = false;
				boolean includeUserGenesPrivate = false;
				boolean includeUserSamplesPublic = false;
				boolean includeUserSamplesPrivate = false;
				
				boolean includeTableWithJoinON = false;
				
				for (Field field : fields){
					if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableStaticAnnotations())){
						includeStatic = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableCustomAnnotations())){
						includeCustom = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableGeneAnnotations())){
						includeGene = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableProjects())){
						includeProjects = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTablePathologies())){
						includeProjects = true;
						includePathologies = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTablePopulations())){
						includeProjects = true;
						includePopulations = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableAlleleFrequencies())){
						includeAlleleFrequencies = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsEvaluations())){
						includeUserEvaluations = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsNumEvaluations())){
						includeUserNumEvaluations = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsVariants())){
						if (field.toString().equalsIgnoreCase("variant_comments_public")) {
							includeUserVariantsPublic = true;
						}else {
							includeUserVariantsPrivate = true;
						}
						includeTableWithJoinON = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsGenes())){
						if (field.toString().equalsIgnoreCase("gene_comments_public")) {
							includeUserGenesPublic = true;
						}else {
							includeUserGenesPrivate = true;
						}
						includeTableWithJoinON = true;
					}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsSamples())){
						if (field.toString().equalsIgnoreCase("sample_comments_public")) {
							includeUserSamplesPublic = true;
						}else {
							includeUserSamplesPrivate = true;
						}
						includeTableWithJoinON = true;
					}	
				}

				StringBuilder query = new StringBuilder();
				query.append("SELECT ");
				for (Field field : fields){
					query.append(field.getQuerySelectName(analysis, includeTableWithJoinON) + ", ");
				}
				query.delete(query.length()-2, query.length());
				query.append(" FROM "+analysis.getFromSampleAnnotations());		
				if (includeStatic) query.append(analysis.getJoinStaticAnnotations());		
				if (includeCustom) query.append(analysis.getJoinCustomAnnotations());		
				if (includeGene) query.append(analysis.getJoinGeneAnnotations());		
				if (includeProjects) query.append(analysis.getJoinProjects());		
				if (includePathologies) query.append(analysis.getJoinPathologies());		
				if (includePopulations) query.append(analysis.getJoinPopulations());		
				if (includeAlleleFrequencies) query.append(analysis.getJoinAlleleFrequencies());		
				if (includeUserEvaluations) query.append(analysis.getJoinUserAnnotationsEvaluations());		
				if (includeUserNumEvaluations) query.append(analysis.getJoinUserAnnotationsNumEvaluations());		
				if (includeUserVariantsPrivate) query.append(analysis.getJoinUserAnnotationsVariantsPrivate());		
				if (includeUserVariantsPublic) query.append(analysis.getJoinUserAnnotationsVariantsPublic());		
				if (includeUserGenesPrivate) query.append(analysis.getJoinUserAnnotationsGenesPrivate());		
				if (includeUserGenesPublic) query.append(analysis.getJoinUserAnnotationsGenesPublic());		
				if (includeUserSamplesPrivate) query.append(analysis.getJoinUserAnnotationsSamplesPrivate());		
				if (includeUserSamplesPublic) query.append(analysis.getJoinUserAnnotationsSamplesPublic());		
				query.append("WHERE variant_sample_id = " + variantSampleId);
				try (Results res = DB.select(Schema.HIGHLANDER, query.toString())) {					
					if (res.next()){
						for (int i=0 ; i < fields.size() ; i++){
							values.add(res.getObject(i+1));
						}
						final DetailsTableModel model = new DetailsTableModel(fields, values);
						table = new JTable(model){
							@Override
							public String getToolTipText(MouseEvent e) {
								String tip = null;
								java.awt.Point p = e.getPoint();
								int rowIndex = rowAtPoint(p);
								int colIndex = columnAtPoint(p);
								Object val = "";
								if (colIndex == 0){
									val = model.getRowDescription(rowIndex);
								}else{
									val = getValueAt(rowIndex, colIndex);
									if (val != null) {
										if (model.getRowField(rowIndex).getFieldClass() == Integer.class) val = Tools.intToString(Integer.parseInt(val.toString()));
										else if (model.getRowField(rowIndex).getFieldClass() == Long.class) val = Tools.longToString(Long.parseLong(val.toString()));
									}
								}
								tip = (val != null) ? val.toString() : "";
								return tip;
							}
						};
						Highlander.getCellRenderer().registerTableForHighlighting(getTitle(), table);
						table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());	
						table.setRowSelectionAllowed(true);
						table.setColumnSelectionAllowed(true);			
						table.setCellSelectionEnabled(true);
						table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

						//Fit the first (field) column size to the biggest cell, letting all remaining space to the second (data) column
						if (table.getRowCount() > 0){
							int maxWidth = 0;
							for (int r = 0 ; r < table.getRowCount() ; r++){
								JTextArea textArea = (JTextArea)(table.getCellRenderer(r, 0).getTableCellRendererComponent(table, table.getValueAt(r, 0),false, false, r, 0));
								maxWidth = Math.max(maxWidth, new JLabel(textArea.getText()).getPreferredSize().width + 5);
							}
							TableColumn tc = table.getColumnModel().getColumn(0);
							tc.setPreferredWidth(maxWidth);
							tc.setMaxWidth(maxWidth);
						}

						detailsPanel.removeAll();
						detailsPanel.add(table, BorderLayout.CENTER);
					}else{
						System.err.println("Variant '"+variantSampleId+"' not found in the database, can't show detailled information");
					}
				}
			}
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

	
	public Category getCategory() {
		return category;
	}
	
	public void highlight(Field field) {
		highlightedField = field;
		repaint();
		validate();
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
			if (isSelected) {
				textArea.setBackground(new Color(51,153,255));
			}
			Color even = Resources.getTableEvenRowBackgroundColor(getColor());
			Color odd = Color.WHITE;
			if (highlightedField != null && ((DetailsTableModel)table.getModel()).getRowField(row).equals(highlightedField)) {
				textArea.setFont(textArea.getFont().deriveFont(Font.BOLD));
				even = Resources.getColor(Palette.Green, 600, true);
				odd = Resources.getColor(Palette.Green, 600, true);
			}
			Field field = (column == 0) ? new Field("field") : ((DetailsTableModel)table.getModel()).getRowField(row);
			return Highlander.getCellRenderer().renderCell(textArea, value, field, SwingConstants.LEFT, row, isSelected, even, odd , false);
		}
	}

	public static class DetailsTableModel	extends AbstractTableModel {
		private List<Field> fields;
		private List<Object> values;

		public DetailsTableModel(List<Field> fields, List<Object> values) {    	
			this.fields = fields;
			this.values = values;
		}

		@Override
		public int getColumnCount() {
			return 2;
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

		public int getRowIndex(Field field){
			for (int i=0 ; i < fields.size() ; i++){
				if (fields.get(i).getName().equals(field.getName())) return i;
			}
			return -1;
		}

		public Field getRowField(int row) {
			return fields.get(row);
		}

		public String getRowDescription(int row) {
			return fields.get(row).getHtmlTooltip();
		}
		
		@Override
		public int getRowCount() {
			return fields.size();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			switch(col){
			case 0:
				return fields.get(row);
			case 1:
				return values.get(row);
			default:
				return null;
			}
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
