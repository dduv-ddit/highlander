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
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
 * Allele frequencies from Highlander, split by pathology, in the form of a summary table.
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxAlleleFrequencyTable extends DetailsBox {

	public enum Filter {All, Germline, Somatic};
	private static Filter selectedFilter = Filter.All;
	private static boolean showAllPathologies = false;

	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;

	private Map<Boolean, Map<Filter, Map<String, List<String>>>> map;
	private Map<Filter, String[]> totals;
	private JPanel tablePanel;

	private Map<String,String> pathologyDescriptons = new TreeMap<>();
	private Map<Filter, Map<String,Integer>> pathologyAlleleNumber = new TreeMap<>();

	public DetailsBoxAlleleFrequencyTable(int variantId, DetailsPanel mainPanel){
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
		return "Allele Frequency per pathology (table)";
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
			totals = new EnumMap<>(Filter.class);
			map = new HashMap<>();
			JPanel buttonPanel = new JPanel(new BorderLayout());
			JPanel filterPanel = new JPanel(new GridLayout(1, 3));
			ButtonGroup group = new ButtonGroup();
			map.put(true, new EnumMap<>(Filter.class));
			map.put(false, new EnumMap<>(Filter.class));
			pathologyAlleleNumber = new EnumMap<>(Filter.class);
			for (Filter filter : Filter.values()) {
				pathologyAlleleNumber.put(filter, new HashMap<>());
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT individual, count(*) as ct, MIN(pathology) as pathology "
						+ "FROM projects JOIN projects_analyses USING (project_id) JOIN pathologies USING (pathology_id) "
						+ "WHERE analysis = '"+analysis+"' "
						+ ((filter == Filter.All) ? "" : "AND sample_type = '"+filter+"' ")
						+ "GROUP BY individual"
						)) {
					while (res.next()){
						String pathology = res.getString("pathology");
						if (!pathologyAlleleNumber.get(filter).containsKey(pathology)) {
							pathologyAlleleNumber.get(filter).put(pathology, 0);
						}
						pathologyAlleleNumber.get(filter).put(pathology, pathologyAlleleNumber.get(filter).get(pathology)+2);
					}
				}
				for (Boolean all : new Boolean[] {true, false}) {
					map.get(all).put(filter, new TreeMap<>());
					String prefix = (filter == Filter.All) ? "local" : filter.toString().toLowerCase();
					String query = 
							(all) ?
									"SELECT "
									+ Field.pathology.getQuerySelectName(analysis, false)+", "+Field.pathology_description.getQuerySelectName(analysis, false)+", "
									+ prefix+"_ac, "+prefix+"_an, "+prefix+"_af, "+prefix+"_het, "+prefix+"_hom "
									+	"FROM " + analysis.getFromSampleAnnotations()
									+ "FULL JOIN `pathologies` "
									+ "LEFT JOIN `"+analysis.getTableAlleleFrequenciesPerPathology()+"` USING (`pos`,`alternative`,`reference`,`chr`,`length`,`pathology_id`) "
									+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + variantSampleId
									:
										"SELECT "
										+ Field.pathology.getQuerySelectName(analysis, false)+", "+Field.pathology_description.getQuerySelectName(analysis, false)+", "
										+ prefix+"_ac, "+prefix+"_an, "+prefix+"_af, "+prefix+"_het, "+prefix+"_hom "
										+	"FROM " + analysis.getFromSampleAnnotations()
										+ analysis.getJoinAlleleFrequenciesPerPathology(false)
										+ analysis.getJoinPathologies()
										+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + variantSampleId;
					try (Results res = DB.select(Schema.HIGHLANDER,	query)) {
						while (res.next()){
							String val_pathology = res.getString(Field.pathology.getName());
							pathologyDescriptons.put(val_pathology, res.getString(Field.pathology_description.getName()));
							int ac = res.getInt(prefix+"_ac");
							int an = res.getInt(prefix+"_an");
							double af = res.getDouble(prefix+"_af");
							int het = res.getInt(prefix+"_het");
							int hom = res.getInt(prefix+"_hom");
							if (!all || pathologyAlleleNumber.get(filter).containsKey(val_pathology)) {
								if (!map.get(all).get(filter).containsKey(val_pathology)){
									map.get(all).get(filter).put(val_pathology, new ArrayList<String>());
								}
								map.get(all).get(filter).get(val_pathology).add(Tools.doubleToPercent(af, 2));
								map.get(all).get(filter).get(val_pathology).add(""+ac);
								if (all) {
									map.get(all).get(filter).get(val_pathology).add(""+pathologyAlleleNumber.get(filter).get(val_pathology));
								}else {
									map.get(all).get(filter).get(val_pathology).add(""+an);								
								}
								map.get(all).get(filter).get(val_pathology).add(""+het);
								map.get(all).get(filter).get(val_pathology).add(""+hom);
							}
						}
					}
					try (Results res = DB.select(Schema.HIGHLANDER, 
							"SELECT "
									+ prefix+"_ac, "+prefix+"_an, "+prefix+"_af, "+prefix+"_het, "+prefix+"_hom "
									+	"FROM " + analysis.getFromSampleAnnotations()
									+ analysis.getJoinAlleleFrequencies()
									+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + variantSampleId
							)) {
						String[] total = new String[6];
						if (res.next()){
							total[0] = "Total";
							total[1] = Tools.doubleToPercent(res.getDouble(prefix+"_af"), 2);
							total[2] = res.getString(prefix+"_ac");
							total[3] = res.getString(prefix+"_an");
							total[4] = res.getString(prefix+"_het");
							total[5] = res.getString(prefix+"_hom");
						}
						totals.put(filter, total);
					}
				}
				JToggleButton button = new JToggleButton(filter.toString());
				if (filter == selectedFilter) button.setSelected(true);
				button.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							selectedFilter = filter;
							JTable table = getTable(map.get(showAllPathologies).get(selectedFilter), totals.get(selectedFilter));
							tablePanel.removeAll();
							tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
							tablePanel.add(table, BorderLayout.CENTER);
							tablePanel.revalidate();
						}
					}
				});
				group.add(button);
				filterPanel.add(button);
			}
			buttonPanel.add(filterPanel, BorderLayout.NORTH);
			JToggleButton button = new JToggleButton("Show all pathologies");
			button.setSelected(showAllPathologies);
			button.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					showAllPathologies = (e.getStateChange() == ItemEvent.SELECTED);
					JTable table = getTable(map.get(showAllPathologies).get(selectedFilter), totals.get(selectedFilter));
					tablePanel.removeAll();
					tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
					tablePanel.add(table, BorderLayout.CENTER);
					tablePanel.revalidate();
				}
			});
			buttonPanel.add(button, BorderLayout.SOUTH);
			detailsPanel.removeAll();
			detailsPanel.add(buttonPanel, BorderLayout.NORTH);
			tablePanel = new JPanel(new BorderLayout());
			JTable table = getTable(map.get(showAllPathologies).get(selectedFilter), totals.get(selectedFilter));
			tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
			tablePanel.add(table, BorderLayout.CENTER);
			detailsPanel.add(tablePanel, BorderLayout.CENTER);
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

	private JTable getTable(Map<String, List<String>> map, String[] total){
		String[][] data;
		if (map.isEmpty()){
			data = new String[1][1];
			data[0][0] = "No sample found";
		}else{
			data = new String[map.size()+1][6];
			int r=0;
			for (String pathology : map.keySet()){
				try{
					data[r][0] = pathology;
				}catch(Exception ex){
					Tools.exception(ex);
					data[r][0] = "ERROR";
				}
				int s=1;
				for (String st : map.get(pathology)) {
					data[r][s++] = st;
				}
				r++;
			}
			data[r] = total;
		}
		final DetailsTableModel model = new DetailsTableModel(data);
		JTable table = new JTable(model){
			@Override
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					@Override
					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						if (index >= 0){
							int realIndex = columnModel.getColumn(index).getModelIndex();
							String colname = (table.getModel()).getColumnName(realIndex);
							switch(colname) {
							case "AF":
								return "Alternative allele frequency in this analysis (per pathology) at this position.";
							case "AC":
								return "Alternative allele count in this analysis (per pathology) at this position.";
							case "AN":
								return "Total number of alleles in this analysis (per pathology) at this position.";
							case "Het":
								return "Count of heterozygous individuals in this analysis (per pathology) at this position.";
							case "Hom":
								return "Count of homozygous individuals in this analysis (per pathology) at this position.";
							default:
								return colname;
							}
						}else{
							return null;
						}
					}
				};
			}
			@Override
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				int colIndex = columnAtPoint(p);
				Object val = "";
				val = getValueAt(rowIndex, colIndex);
				if (colIndex == 0) val += " ("+pathologyDescriptons.get(val)+")"; 
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
				JLabel label = (JLabel)(table.getCellRenderer(row, 0).getTableCellRendererComponent(table, table.getValueAt(row, 0),false, false, row, 0));
				maxWidth = Math.max(maxWidth, new JLabel(label.getText()).getPreferredSize().width + 5);
			}
			TableColumn tc = table.getColumnModel().getColumn(0);
			tc.setPreferredWidth(maxWidth);
			tc.setMaxWidth(maxWidth);
		}

		return table;
	}

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			if (row%2 == 0) label.setBackground(Resources.getTableEvenRowBackgroundColor(getColor()));
			else label.setBackground(Color.WHITE);
			label.setForeground(Color.black);
			label.setBorder(new LineBorder(Color.WHITE));
			if (isSelected) {
				label.setBackground(new Color(51,153,255));
			}
			if (column == 0)
				label.setHorizontalAlignment(SwingConstants.LEFT);
			else
				label.setHorizontalAlignment(SwingConstants.CENTER);
			if (row == table.getRowCount()-1)
				label.setFont(label.getFont().deriveFont(Font.BOLD));
			return label;
		}
	}

	public static class DetailsTableModel	extends AbstractTableModel {
		String[] headers = new String[] {
				"Pathology",
				"AF",
				"AC",
				"AN",
				"Het",
				"Hom",
		};
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
			return headers[col];
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
