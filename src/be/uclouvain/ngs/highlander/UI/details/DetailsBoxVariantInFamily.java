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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;

/**
 * Displays all samples from each individual of the whole family, and indicates if the variant is found in other samples, whichever the analysis.
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxVariantInFamily extends DetailsBox {

	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;

	private JPanel tablePanel;

	public DetailsBoxVariantInFamily(int variantId, DetailsPanel mainPanel){
		this.variantSampleId = variantId;
		this.mainPanel = mainPanel;
		boolean visible = mainPanel.isBoxVisible(getTitle());				
		initCommonUI(visible);
	}

	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	public String getTitle(){
		return "Variant in family";
	}

	public Palette getColor() {
		return Field.individual.getCategory().getColor();
	}

	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	protected void loadDetails(){
		try {
			Map<String, Map<String, Map<Analysis, Zygosity>>> map = new TreeMap<>();
			Set<Analysis> analyses = new HashSet<>();
			Analysis current = Highlander.getCurrentAnalysis();	
			String family = "", chr = "", pos = "", length = "", ref = "", alt = "", gene_symbol = "";
			try (Results res = DB.select(Schema.HIGHLANDER,
					"SELECT `family`, "
					+ "`" + Field.chr.getName() + "`, "
					+ "`" + Field.pos.getName() + "`, "
					+ "`" + Field.length.getName() + "`, "
					+ "`" + Field.reference.getName() + "`, "
					+ "`" + Field.alternative.getName() + "`, "
					+ "`" + Field.gene_symbol.getName() + "` "
					+ "FROM " + current.getFromSampleAnnotations()
					+ current.getJoinProjects()
					+ "WHERE "+Field.variant_sample_id.getQueryWhereName(current, false)+" = " + variantSampleId
					)){
				if (res.next()){
					family = res.getString("family");
					chr = res.getString(Field.chr.getName());
					pos = res.getString(Field.pos.getName());
					length = res.getString(Field.length.getName());
					ref = res.getString(Field.reference.getName());
					alt = res.getString(Field.alternative.getName());
					gene_symbol = res.getString(Field.gene_symbol.getName());
				}
			}
			try (Results res = DB.select(Schema.HIGHLANDER,
					"SELECT `individual`, `sample`, `analysis` "
					+ "FROM `projects` "
					+ "JOIN `projects_analyses` USING (`project_id`) "
					+ "WHERE `family` = '"+family+"'"
					)){
				while (res.next()){
					String individual = res.getString("individual");
					String sample = res.getString("sample");
					Analysis analysis = new Analysis(res.getString("analysis"));
					if (!map.containsKey(individual)) {
						map.put(individual, new TreeMap<>());
					}
					if (!map.get(individual).containsKey(sample)) {
						map.get(individual).put(sample, new TreeMap<>());
					}
					map.get(individual).get(sample).put(analysis, Zygosity.Reference);
					analyses.add(analysis);
				}
			}
			for (Analysis analysis : analyses) {
				try (Results res = DB.select(Schema.HIGHLANDER,
						"SELECT `individual`, `sample`, " + Field.zygosity.getQuerySelectName(analysis, false)
						+ " FROM " + analysis.getFromSampleAnnotations()
						+ analysis.getJoinProjects()
						+ "WHERE "+Field.individual.getQueryWhereName(analysis, false)+" IN (" + HighlanderDatabase.makeSqlList(map.keySet(), String.class) + ") "
						+ "AND `" + Field.chr.getName() + "` = '"+chr+"' "
						+ "AND `" + Field.pos.getName() + "` = "+pos+ " "
						+ "AND `" + Field.length.getName() + "` = "+length+ " "
						+ "AND `" + Field.reference.getName() + "` = '"+ref+"' "
						+ "AND `" + Field.alternative.getName() + "` = '"+alt+"' "
						+ "AND `" + Field.gene_symbol.getName() + "` = '"+gene_symbol+"'"
						)){
					while (res.next()){
						String individual = res.getString("individual");
						String sample = res.getString("sample");
						Zygosity zygosity = Zygosity.valueOf(res.getString(Field.zygosity.getName()));
						map.get(individual).get(sample).put(analysis, zygosity);
					}
				}
			}
			detailsPanel.removeAll();
			tablePanel = new JPanel(new BorderLayout());
			JTable table = getTable(map);					
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

	private JTable getTable(Map<String, Map<String, Map<Analysis, Zygosity>>> map){
		String[][] data;
		if (map.isEmpty()){
			data = new String[1][1];
			data[0][0] = "No sample found";
		}else{
			int rows = 0;
			for (String individual : map.keySet()) {
				for (String sample : map.get(individual).keySet()) {
					rows +=  map.get(individual).get(sample).size();
				}
			}
			data = new String[rows][4];
			int r=0;
			for (String individual : map.keySet()) {
				for (String sample : map.get(individual).keySet()) {
					for (Analysis analysis : map.get(individual).get(sample).keySet()) {
						data[r][0] = individual;
						data[r][1] = sample;
						data[r][2] = analysis.toString();
						switch (map.get(individual).get(sample).get(analysis)) {
						case Reference:
							data[r][3] = "-";
							break;							
						case Heterozygous:
							data[r][3] = "het";
							break;							
						case Homozygous:
							data[r][3] = "hom";
							break;							
						}
						r++;
					}
				}
			}
		}
		final DetailsTableModel model = new DetailsTableModel(data);
		JTable table = new JTable(model){
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

		return table;
	}

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
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
			if (column != 3)
				label.setHorizontalAlignment(SwingConstants.LEFT);
			else
				label.setHorizontalAlignment(SwingConstants.CENTER);
			return label;
		}
	}

	public static class DetailsTableModel	extends AbstractTableModel {
		String[] headers = new String[] {
				"Individual",
				"Sample",
				"Analysis",
				"Variant",
		};
		private String[][] data;

		public DetailsTableModel(String[][] data) {    	
			this.data = data;
		}

		public int getColumnCount() {
			if (data.length == 0) return 0;
			return data[0].length;
		}

		public String getColumnName(int col) {
			return headers[col];
		}

		public int getRowCount() {
			return data.length;
		}

		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		public Object getValueAt(int row, int col) {
			if (row >= data.length || col >= data[row].length) return null;
			if (row < 0 || col < 0) return null;
			return data[row][col];
		}

		public void setValueAt(Object value, int row, int col) {
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}

}
