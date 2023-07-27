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

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.table.MultiLineTableCellRenderer;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
 * Coverage data for the gene and sample
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxCoverage extends DetailsBox {

	private DetailsPanel mainPanel;
	private JTable table;
	private boolean detailsLoaded = false;

	public DetailsBoxCoverage(int variantId, DetailsPanel mainPanel){
		this.variantSampleId = variantId;
		this.mainPanel = mainPanel;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	@Override
	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	@Override
	public String getTitle(){
		return "Gene and sample coverage";
	}

	@Override
	public Palette getColor() {
		return Field.read_depth.getCategory().getColor();
	}

	@Override
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	@Override
	protected void loadDetails(){
		try{
			int project_id = -1;			
			String pathology = "", gene_symbol = "", sample = "";
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT project_id, gene_symbol, pathology, sample "
					+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
					+ Highlander.getCurrentAnalysis().getJoinProjects()
					+ Highlander.getCurrentAnalysis().getJoinPathologies()
					+	" WHERE variant_sample_id = " + variantSampleId
					)){
				if (res.next()){
					project_id = res.getInt("project_id");
					pathology = res.getString("pathology");
					gene_symbol = res.getString("gene_symbol");
					sample = res.getString("sample");
				}
			}
			if (project_id >= 0){
				List<String> coverages = new ArrayList<String>();
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SHOW COLUMNS FROM " + Highlander.getCurrentAnalysis().getFromCoverage())) {
					while(res.next()){
						String col = Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res);
						if (col.startsWith("num_pos_")){
							coverages.add(col.substring("num_pos_".length()));
						}
					}
				}
				int rows = (gene_symbol != null) ? 4+(3*coverages.size()) : 1;
				String[][] data = new String[rows][2];
				int row = 0;
				data[row++][0] = sample+" mean coverage";
				if (gene_symbol != null){
					data[row++][0] = gene_symbol + " exons depth ("+sample+" only)";
					for (String cov : coverages) {
						data[row++][0] = gene_symbol + " exons coverage "+cov+" ("+sample+" only)";						
					}
					data[row++][0] = gene_symbol + " exons depth ("+pathology+" samples)";
					for (String cov : coverages) {
						data[row++][0] = gene_symbol + " exons coverage "+cov+" ("+pathology+" samples)";
					}
					data[row++][0] = gene_symbol + " exons depth (all samples)";
					for (String cov : coverages) {
						data[row++][0] = gene_symbol + " exons coverage "+cov+" (all samples)";
					}
				}
				row = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT coverage_wo_dup FROM projects " +
						"WHERE project_id = " + project_id)){
					if (res.next()){
						data[row++][1] = ""+res.getInt("coverage_wo_dup");
					}
				}
				if (gene_symbol != null){
					String query = "SELECT ";
					for (String cov : coverages) {
						query += "SUM(num_pos_"+cov+") as "+cov+", ";
					}
					query += "SUM(`end`-`start`) as length, "
								+ "SUM(mean_depth*(`end`-`start`)) as depth "
								+ "FROM " + Highlander.getCurrentAnalysis().getFromCoverageRegions()
								+ Highlander.getCurrentAnalysis().getJoinCoverage()
								+	"WHERE project_id = " + project_id + " AND gene_symbol = '"+gene_symbol+"' "
								+ "GROUP BY gene_symbol";
					try (Results res = DB.select(Schema.HIGHLANDER, query)){
						if (res.next()){
							double length = res.getDouble("length");
							data[row++][1] = Tools.doubleToString(res.getDouble("depth")/length, 2, false);
							for (String cov : coverages) {
								data[row++][1] = Tools.doubleToPercent(res.getDouble(cov)/length, 0);								
							}
						}else {
							data[row++][1] = gene_symbol + " coverage not imported for this sample";
							for (int i=0 ; i < coverages.size() ; i++) {
								data[row++][1] = gene_symbol + " coverage not imported for this sample";
							}
						}
					}
					int count = 0;
					double[] values = new double[1+coverages.size()];
					query = "SELECT ";
					for (String cov : coverages) {
						query += "SUM(num_pos_"+cov+") as "+cov+", ";
					}
					query +=  "project_id, "
							+ "SUM(`end`-`start`) as length, "
							+ "SUM(mean_depth*(`end`-`start`)) as depth "
							+ "FROM " + Highlander.getCurrentAnalysis().getFromCoverageRegions()
							+ Highlander.getCurrentAnalysis().getJoinCoverage()
							+ Highlander.getCurrentAnalysis().getJoinProjects()
							+ Highlander.getCurrentAnalysis().getJoinPathologies()
							+	"WHERE pathology = '" + pathology + "' AND gene_symbol = '"+gene_symbol+"' "
							+ "GROUP BY gene_symbol, project_id";
					try (Results res = DB.select(Schema.HIGHLANDER, query)){
						while (res.next()){
							double length = res.getDouble("length");
							values[0] += res.getDouble("depth")/length;
							for (int i=0 ; i < coverages.size() ; i++) {
								values[i+1] += res.getDouble(coverages.get(i))/length;
							}
							count++;
						}
					}
					if (count > 0) {
						data[row++][1] = Tools.doubleToString(values[0]/count, 2, false);
						for (int i=1 ; i < values.length ; i++) {
							data[row++][1] = Tools.doubleToPercent(values[i]/count, 0);							
						}
					}else {
						data[row++][1] = gene_symbol + " coverage not imported for this pathology";
						for (int i=0 ; i < coverages.size() ; i++) {
							data[row++][1] = gene_symbol + " coverage not imported for this pathology";
						}	
					}
					count = 0;
					values = new double[1+coverages.size()];
					query = "SELECT ";
					for (String cov : coverages) {
						query += "SUM(num_pos_"+cov+") as "+cov+", ";
					}
					query +=  "project_id, "
							+ "SUM(`end`-`start`) as length, "
							+ "SUM(mean_depth*(`end`-`start`)) as depth "
							+ "FROM " + Highlander.getCurrentAnalysis().getFromCoverageRegions()
							+ Highlander.getCurrentAnalysis().getJoinCoverage()
							+	"WHERE gene_symbol = '"+gene_symbol+"' "
							+ "GROUP BY gene_symbol, project_id";
					try (Results res = DB.select(Schema.HIGHLANDER, query)){
						while (res.next()){
							double length = res.getDouble("length");
							values[0] += res.getDouble("depth")/length;
							for (int i=0 ; i < coverages.size() ; i++) {
								values[i+1] += res.getDouble(coverages.get(i))/length;
							}
							count++;
						}
					}
					if (count > 0) {
						data[row++][1] = Tools.doubleToString(values[0]/count, 2, false);
						for (int i=1 ; i < values.length ; i++) {
							data[row++][1] = Tools.doubleToPercent(values[i]/count, 0);							
						}
					}else {
						data[row++][1] = gene_symbol + " coverage never imported";
						for (int i=0 ; i < coverages.size() ; i++) {
							data[row++][1] = gene_symbol + " coverage never imported";
						}
					}
				}				

				final DetailsTableModel model = new DetailsTableModel(data);
				table = new JTable(model){
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
					for (int r = 0 ; r < table.getRowCount() ; r++){
						JTextArea textArea = (JTextArea)(table.getCellRenderer(r, 0).getTableCellRendererComponent(table, table.getValueAt(r, 0),false, false, r, 0));
						maxWidth = Math.max(maxWidth, new JLabel(textArea.getText()).getPreferredSize().width + 5);
					}
					TableColumn tc = table.getColumnModel().getColumn(0);
					tc.setPreferredWidth(maxWidth);
					tc.setMaxWidth(maxWidth);
				}

				table.getColumnModel().getColumn(1).setPreferredWidth(20);
				detailsPanel.removeAll();
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
			String rowname = (column == 1) ? table.getValueAt(row,0).toString() : "field";
			Field field = Field.getField(rowname);
			return Highlander.getCellRenderer().renderCell(textArea, value, field, SwingConstants.LEFT, row, isSelected, Resources.getTableEvenRowBackgroundColor(getColor()), Color.WHITE, false);
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
