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
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
 * Allele frequencies from Highlander, split by pathology, with complete list of samples bearing the variant.
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxAlleleFrequencyDetailled extends DetailsBox {

	public enum Filter {All, Germline, Somatic};
	private static Filter selectedFilter = Filter.All;
	
	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;
	
	Map<Filter, Map<String, Set<String>>> map;
	private JTable table;
	
	
	public DetailsBoxAlleleFrequencyDetailled(int variantId, DetailsPanel mainPanel){
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
		return "Allele Frequency per pathology (details)";
	}

	@Override
	public Palette getColor() {
		return Field.gnomad_wgs_af.getCategory().getColor();
	}

	@Override
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	@Override
	protected void loadDetails(){
		try {
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			String chr = "";
			int pos = 0;
			int length = 0;
			String reference = "";
			String alternative = "";
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT chr, pos, length, reference, alternative "
					+ "FROM " + analysis.getFromSampleAnnotations() 
					+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + variantSampleId)){
				if (res.next()){
					chr = res.getString("chr");
					pos = res.getInt("pos");
					length = res.getInt("length");
					reference = res.getString("reference");
					alternative = res.getString("alternative");
				}
			}
			if (pos >= 0){
				map = new EnumMap<>(Filter.class);
				JPanel filterPanel = new JPanel(new GridLayout(1, 3));
				ButtonGroup group = new ButtonGroup();
				for (Filter filter : Filter.values()) {
					map.put(filter, new TreeMap<>());
					String prefix = (filter == Filter.All) ? "local" : filter.toString().toLowerCase();
					
					try (Results res = DB.select(Schema.HIGHLANDER, 
							"SELECT "
									+ Field.pathology.getQuerySelectName(analysis, false)+", "+Field.individual.getQuerySelectName(analysis, false)+", "
									+ "group_concat("+Field.sample.getQuerySelectName(analysis, false)+",' [',SUBSTRING("+Field.zygosity.getQuerySelectName(analysis, false)+",1,3),']' SEPARATOR ', ') as samples, "
									+ ""+prefix+"_ac, "+prefix+"_an "
									+	"FROM " + analysis.getFromSampleAnnotations()
									+ analysis.getJoinProjects()
									+ analysis.getJoinPathologies()
									+ analysis.getJoinAlleleFrequenciesPerPathology(true)
									+ "WHERE `chr` = '"+chr+"' AND `pos` = "+pos+" AND `length` = "+length+" AND "
									+ "`reference` = '"+reference+"' AND `alternative` = '"+alternative+"' "
									+ ((filter == Filter.All) ? "" : "AND "+Field.sample_type.getQueryWhereName(analysis, false)+" = '"+filter+"' ")
									+ "GROUP BY pathology, "+prefix+"_ac, "+prefix+"_an, individual"
							)) {
						while (res.next()){
							String val_individual = res.getString(Field.individual.getName());
							String val_samples = res.getString("samples");
							String val_pathology = res.getString(prefix+"_ac") + "/" + res.getString(prefix+"_an") + " " + res.getString(Field.pathology.getName()) + " : ";
							if (!map.get(filter).containsKey(val_pathology)){
								map.get(filter).put(val_pathology, new TreeSet<String>());
							}
							map.get(filter).get(val_pathology).add(val_individual+" ("+val_samples+")");
						}
					}
					JToggleButton button = new JToggleButton(filter.toString());
					if (filter == selectedFilter) button.setSelected(true);
					button.addItemListener(new ItemListener() {
						@Override
						public void itemStateChanged(ItemEvent e) {
							detailsPanel.remove(table);
							if (e.getStateChange() == ItemEvent.SELECTED) {
								selectedFilter = filter;
								table = getTable(map.get(filter));
							}
							detailsPanel.add(table, BorderLayout.CENTER);
							detailsPanel.revalidate();
						}
					});
					group.add(button);
					filterPanel.add(button);
				}
				table = getTable(map.get(selectedFilter));					
				detailsPanel.removeAll();
				detailsPanel.add(filterPanel, BorderLayout.NORTH);
				detailsPanel.add(table, BorderLayout.CENTER);
			}
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

	private JTable getTable(Map<String, Set<String>> map){
		String[][] data;
		if (map.isEmpty()){
			data = new String[1][1];
			data[0][0] = "No sample found";
		}else{
			data = new String[map.size()][2];
			int r=0;
			for (String pathology : map.keySet()){
				try{
					data[r][0] = pathology;
				}catch(Exception ex){
					Tools.exception(ex);
					data[r][0] = "ERROR";
				}
				StringBuilder sb = new StringBuilder();
				for (String sample : map.get(pathology)){
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
				maxWidth = Math.max(maxWidth, new JLabel(textArea.getText()).getPreferredSize().width + 5);
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
