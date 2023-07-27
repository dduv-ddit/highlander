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

import java.sql.ResultSetMetaData;

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
 * Run information
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxRun extends DetailsBox {

	private DetailsPanel mainPanel;
	private JTable table;
	private boolean detailsLoaded = false;

	public DetailsBoxRun(int variantId, DetailsPanel mainPanel){
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
		return "Run statistics";
	}

	@Override
	public Palette getColor() {
		return Field.run_label.getCategory().getColor();
	}

	@Override
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	@Override
	protected void loadDetails(){
		try{
			int project_id = -1;			
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT project_id "
					+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations() 
					+	"WHERE variant_sample_id = " + variantSampleId
					)) {
				if (res.next()){
					project_id = res.getInt("project_id");
				}
			}
			if (project_id >= 0){
				String[][] data = null;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM projects WHERE project_id = " + project_id)) {
					ResultSetMetaData meta = res.getMetaData();
					int rows = meta.getColumnCount();
					data = new String[rows][2];
					if (res.next()){
						for (int row = 0 ; row < rows ; row++){
							data[row][0] = meta.getColumnLabel(row+1);
							data[row][1] = res.getString(row+1);
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
			String rowname = (column == 1) ? ((table.getValueAt(row,0) != null) ? table.getValueAt(row,0).toString() : "") : "field";
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
