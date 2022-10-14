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

package be.uclouvain.ngs.highlander.UI.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.dialog.CreateRunSelection;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.table.HeatMap;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ColorRange;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ConversionMethod;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.RunNGS;

import java.util.Arrays;

public class RunStatistics extends JFrame {

	private static final List<String> leftAligned = Arrays.asList(new String[]{
			"run_name",
			"run_path",
			"comments",
	});
	private static final List<String> percent = Arrays.asList(new String[]{
			"sequence_duplication_prop",
			"percent_bad_beads",
			"percent_unassigned_reads",
			"percent_total_mapped",
			"percent_low_mapqv",
			"percent_on",
			"percent_off",
			"percent_of_target_bases_not_covered",
			"percent_of_target_covered_meq_1X",
			"percent_of_target_covered_meq_5X",
			"percent_of_target_covered_meq_10X",
			"percent_of_target_covered_meq_20X",
			"percent_of_target_covered_meq_1X_wo_dup",
			"percent_of_target_covered_meq_5X_wo_dup",
			"percent_of_target_covered_meq_10X_wo_dup",
			"percent_of_target_covered_meq_20X_wo_dup",
			"percent_of_target_covered_meq_30X_wo_dup",
			"percent_of_exome_covered_meq_1X_wo_dup",
			"percent_of_exome_covered_meq_5X_wo_dup",
			"percent_of_exome_covered_meq_10X_wo_dup",
			"percent_of_exome_covered_meq_20X_wo_dup",
			"percent_of_exome_covered_meq_30X_wo_dup",
			"percent_duplicates_picard",
			"percent_not_totally_on_target",
	});
	private static final List<String> fastQC = Arrays.asList(new String[]{
			"per_base_sequence_quality",
			"per_sequence_quality_scores",
			"per_base_sequence_content",
			"per_base_GC_content",
			"per_sequence_GC_content",
			"per_base_N_content",
			"sequence_length_distribution",
			"sequence_duplication_levels",
			"over-represented_sequences",
			"kmer_content",
			"adapter_content",
			"per_tile_sequence_quality",
	});
	private static final List<String> hasHeatMap = Arrays.asList(new String[]{
			"percent_bad_beads",
			"percent_unassigned_reads",
			"percent_low_mapqv",
			"percent_duplicates_picard",
			"percent_not_totally_on_target",
			"percent_total_mapped",
			"percent_on",
			"average_depth_of_target_coverage",
			"coverage_wo_dup",
			"coverage_exome_wo_dup",
	});

	private StatisticsTableModel tableModel;
	private JTable table;
	private TableRowSorter<StatisticsTableModel> sorter;
	private HeatMap heatMap;

	private JLabel label_run_selection = new JLabel("All runs are selected");
	private Set<RunNGS> selectedRuns = new TreeSet<RunNGS>();

	private SearchField searchField = new SearchField(20){
		@Override
		protected void keyListener(KeyEvent key){
			if (table == null) {
				JOptionPane.showMessageDialog(RunStatistics.this, "Table is empty, you must first generate a filter.", "No table", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}else{
				applyFilter();
			}
		}
		@Override
		public void applyFilter(){
			RowFilter<StatisticsTableModel, Object> rf = null;
			//If current expression doesn't parse, don't update.
			try {
				rf = RowFilter.regexFilter("(?i)"+getText());
			} catch (java.util.regex.PatternSyntaxException e) {
				return;
			}
			final RowFilter<StatisticsTableModel, Object> rff = rf;
			//waitingPanel.start();
			new Thread(new Runnable(){
				public void run(){
					try{
						textFilter = rff;
						rowFilterExp = getTyppedText();
						applyFilters();
						/*
						List<RowFilter<StatisticsTableModel,Object>> filters = new ArrayList<RowFilter<StatisticsTableModel,Object>>();
						if (textFilter != null) filters.add(textFilter);
						if (!filters.isEmpty()){
							sorter.setRowFilter(RowFilter.andFilter(filters));
						}
						*/
					}catch(Exception ex){
						Tools.exception(ex);
					}
					//waitingPanel.stop();
				}
			}, "RunStatistics.applyFilter").start();    

		}
	};
	private RowFilter<StatisticsTableModel, Object> textFilter = null;
	private Map<String, RowFilter<StatisticsTableModel, Object>> columnFilters = new LinkedHashMap<>();
	private String rowFilterExp = null;

	static private WaitingPanel waitingPanel;

	public RunStatistics(){
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/5);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						fillTable();				
					}
				}, "RunStatistics.fillTable").start();
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});
	}

	private void initUI(){
		setTitle("Run statistics details");
		setIconImage(Resources.getScaledIcon(Resources.iRunStatisticsDetails, 64).getImage());

		setLayout(new BorderLayout());

		JPanel panel_north = new JPanel();
		getContentPane().add(panel_north, BorderLayout.NORTH);

		JButton btnSelect = new JButton("Select NGS runs to include in the chart",Resources.getScaledIcon(Resources.i3dPlus, 32));
		btnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CreateRunSelection select = new CreateRunSelection(selectedRuns);
				Tools.centerWindow(select, false);
				select.setVisible(true);
				if (!select.getSelection().isEmpty()){
					selectedRuns = select.getSelection();
					if (selectedRuns.size() > 0){
						label_run_selection.setText(selectedRuns.size() + " runs selected");
					}else{
						label_run_selection.setText("No run selected !");						
					}
					label_run_selection.repaint();
				}
				new Thread(new Runnable(){
					public void run(){
						fillTable();
					}
				}, "RunStatistics.fillTable").start();
			}
		});
		panel_north.add(btnSelect);

		panel_north.add(label_run_selection);

		JPanel searchPanel = new JPanel(new GridBagLayout());

		JLabel title = new JLabel("Search");
		searchPanel.add(title, new GridBagConstraints(0,0,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,30,0,10), 0, 0));
		searchPanel.add(searchField, new GridBagConstraints(0,1,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,30,0,10), 0, 0));
		panel_north.add(searchPanel);

		columnFilters.put("sequencing_target", null);
		columnFilters.put("pathology", null);
		columnFilters.put("population", null);
		columnFilters.put("index_case", null);
		columnFilters.put("sample_type", null);
		
		for (String field : columnFilters.keySet()) {
			JPanel filterPanel = new JPanel(new GridBagLayout());		
			JComboBox<String> boxFilter = new JComboBox<String>(getPossibleValues(field));
			//boxFilter.setPrototypeDisplayValue("AZERTYUIOPQSDFGHJKLM"); //to limit combobox size to this text, and not the longest item
			boxFilter.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						if (boxFilter.getSelectedItem().toString().equals("All")) {
							columnFilters.put(field, null);											
						}else {
							RowFilter<StatisticsTableModel, Object> rowFilter = new RowFilter<StatisticsTableModel, Object>() {
								@Override
								public boolean include(javax.swing.RowFilter.Entry<? extends StatisticsTableModel, ? extends Object> entry) {
									StatisticsTableModel model = entry.getModel();
									int colIndex = model.getColumnIndex(field);
									if (colIndex != -1) {
											Object value = model.getValueAt((Integer)entry.getIdentifier(), colIndex);
											if (value != null && value.toString().equals(boxFilter.getSelectedItem().toString())){
												return true;
											}				
									}
									return false;
								}
							};
							columnFilters.put(field, rowFilter);				
						}
						applyFilters();
					}
				}
			});
			filterPanel.add(new JLabel(field), new GridBagConstraints(0,0,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,30,0,10), 0, 0));
			filterPanel.add(boxFilter, new GridBagConstraints(0,1,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,30,0,10), 0, 0));
			panel_north.add(filterPanel);		
		}
			
		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnClose = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel.add(btnClose);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 24));
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						export();
					}
				}, "RunStatistics.export").start();
			}
		});
		panel.add(export);

		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		table = new JTable(){
			@Override
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						if (index >= 0){
							int realIndex = columnModel.getColumn(index).getModelIndex();
							return (table.getModel()).getColumnName(realIndex);
						}else{
							return null;
						}
					}
				};
			}
			@Override
			//Auto-resize columns to fit content
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component component = super.prepareRenderer(renderer, row, column);
				int rendererWidth = component.getPreferredSize().width;
				TableColumn tableColumn = getColumnModel().getColumn(column);
				tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
				return component;
			}
		};
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Integer.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Double.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Boolean.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Date.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Long.class, new ColoredTableCellRenderer());
		heatMap = new HeatMap(table);
		scrollPane.setViewportView(table);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}	

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			String colname = table.getColumnName(column);

			if (leftAligned.contains(colname)){
				label.setHorizontalAlignment(JLabel.LEFT);
			}else{
				label.setHorizontalAlignment(JLabel.CENTER);
			}

			if (value == null) {
				value = "";
			}else{      	
				if (percent.contains(colname)){
					value = Tools.doubleToString((Double)value, 0, false) + "%";
				}else if (table.getColumnClass(column) == Long.class){
					value = Tools.longToString((Long)value);
				}else if (table.getColumnClass(column) == Integer.class){
					value = Tools.intToString((Integer)value);
				}
			}

			if (row%2 == 0) label.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Purple));
			else label.setBackground(Color.white);
			label.setForeground(Color.black);
			label.setBorder(new LineBorder(Color.WHITE));
			if (value != null){
				if (fastQC.contains(colname)){
					if (value.toString().equalsIgnoreCase("pass")) label.setBackground(Color.green);
					else if (value.toString().equalsIgnoreCase("warn")) label.setBackground(Color.orange);
					else if (value.toString().equalsIgnoreCase("fail")) label.setBackground(Color.red);
				}else if (hasHeatMap.contains(colname)){
					label.setBackground(heatMap.getColor(row, column));
				}
				label.setText(value.toString());
			}      
			if (isSelected) {
				label.setBackground(new Color(51,153,255));
			}
			if (rowFilterExp != null && value != null && rowFilterExp.length() > 0 && value.toString().toLowerCase().contains(rowFilterExp.toLowerCase())){
				Font font = label.getFont();
				label.setFont(font.deriveFont(Font.BOLD));
				label.setForeground(Color.green);
			}
			return label;
		}
	}

	public static class StatisticsTableModel	extends AbstractTableModel {
		final private Object[][] data;
		final private String[] headers;
		final private Class<?>[] classes;

		public StatisticsTableModel(Object[][] data, String[] headers, Class<?>[] classes) {    	
			this.data = data;
			this.headers = headers;
			this.classes = classes;
		}

		public int getColumnCount() {
			return headers.length;
		}

		public String getColumnName(int col) {
			return headers[col];
		}

		public int getRowCount() {
			return data.length;
		}

		public Class<?> getColumnClass(int col) {
			return classes[col];
		}

		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		public void setValueAt(Object value, int row, int col) {
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		public int getColumnIndex(String columnName) {
			for (int i=0 ; i < headers.length ; i++) {
				if (headers[i].equals(columnName)) return i;
			}
			return -1;
		}

	}

	private String[] getPossibleValues(String field) {
		Set<String> values = new TreeSet<String>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT DISTINCT(`"+field+"`) FROM projects JOIN pathologies USING (pathology_id) LEFT JOIN populations USING (population_id)")) {
			while (res.next()){
				if (res.getString(1) != null){
					values.add(res.getString(1));
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Can't retrieve field values from the database", ex), "Fill available values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		String[] res = new String[values.size()+1];
		res[0] = "All";
		int i=1;
		for (String value : values) {
			res[i++] = value;
		}
		return res;
	}
	
	private void fillTable(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			StringBuilder selection = new StringBuilder();
			selection.append("(");
			for (RunNGS run : selectedRuns){
				selection.append("(run_id = " + run.getRun_id() + " AND run_date = '"+run.getRun_date()+"' AND run_name = '"+run.getRun_name()+"') OR");
			}
			if (selection.length() > 1) selection.delete(selection.length()-3, selection.length());
			else selection.append("run_path IS NOT NULL");
			selection.append(")");
			List<Object[]> arrayList = new ArrayList<Object[]>();
			String[] headers = null;
			Class<?>[] classes = null;
			int row = 0;
			int colCount = 0;
			Object[][] data = null;
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT "
					+ "p.`project_id`, "
					+ "p.`sequencing_target`, "
					+ "p.`platform`, "
					+ "p.`outsourcing`, "
					+ "p.`family`, "
					+ "p.`individual`, "
					+ "p.`sample`, "
					+ "MIN(a.gene_coverage_ratio_chr_xy) as ratio_xy, "
					+ "(CASE WHEN MIN(a.gene_coverage_ratio_chr_xy) IS NULL THEN '?' WHEN MIN(a.gene_coverage_ratio_chr_xy) = 0 THEN 'F' WHEN MIN(a.gene_coverage_ratio_chr_xy) < 65 THEN 'M' ELSE 'F' END) as gender_xy, "
					+ "p.`index_case`, "
					+ "pathology, "
					+ "pathology_description, "
					+ "population, "
					+ "population_description, "
					+ "p.`sample_type`, "
					+ "p2.`sample` as normal, "
					+ "GROUP_CONCAT(DISTINCT a.analysis) as analyses, "
					+ "GROUP_CONCAT(DISTINCT u.username) as users, "
					+ "p.`barcode`, "
					+ "p.`kit`, "
					+ "p.`read_length`, "
					+ "p.`pair_end`, "
					+ "p.`trim`, "
					+ "p.`remove_duplicates`, "
					+ "p.`run_id`, "
					+ "p.`run_date`, "
					+ "p.`run_name`, "
					+ "p.`run_label`, "
					+ "p.`per_base_sequence_quality`, "
					+ "p.`per_tile_sequence_quality`, "
					+ "p.`per_sequence_quality_scores`, "
					+ "p.`per_sequence_GC_content`, "
					+ "p.`per_base_N_content`, "
					+ "p.`sequence_length_distribution`, "
					+ "p.`sequence_duplication_levels`, "
					+ "p.`sequence_duplication_prop`, "
					+ "p.`over-represented_sequences`, "
					+ "p.`adapter_content`, "
					+ "p.`percent_of_target_covered_meq_1X`, "
					+ "p.`percent_of_target_covered_meq_5X`, "
					+ "p.`percent_of_target_covered_meq_10X`, "
					+ "p.`percent_of_target_covered_meq_20X`, "
					+ "p.`average_depth_of_target_coverage`, "
					+ "p.`coverage_wo_dup`, "
					+ "p.`percent_of_target_covered_meq_1X_wo_dup`, "
					+ "p.`percent_of_target_covered_meq_5X_wo_dup`, "
					+ "p.`percent_of_target_covered_meq_10X_wo_dup`, "
					+ "p.`percent_of_target_covered_meq_20X_wo_dup`, "
					+ "p.`percent_of_target_covered_meq_30X_wo_dup`, "
					+ "p.`coverage_exome_wo_dup`, "
					+ "p.`percent_of_exome_covered_meq_1X_wo_dup`, "
					+ "p.`percent_of_exome_covered_meq_5X_wo_dup`, "
					+ "p.`percent_of_exome_covered_meq_10X_wo_dup`, "
					+ "p.`percent_of_exome_covered_meq_20X_wo_dup`, "
					+ "p.`percent_of_exome_covered_meq_30X_wo_dup`, "
					+ "p.`percent_duplicates_picard`, "
					+ "p.`comments` "
					+ "FROM projects as p "
					+ "JOIN pathologies USING (pathology_id) "
					+ "LEFT JOIN populations USING (population_id) "
					+ "LEFT JOIN projects_users as u USING (project_id) "
					+ "LEFT JOIN projects_analyses as a USING (project_id) "
					+ "LEFT JOIN projects as p2 ON p.normal_id = p2.project_id "
					+ "WHERE " + selection.toString() + " AND run_path IS NOT NULL GROUP BY p.project_id")){
				ResultSetMetaData meta = res.getMetaData();
				colCount = meta.getColumnCount();
				final int max = res.getResultSetSize();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setProgressString(null, (max == -1));
						if (max != -1) waitingPanel.setProgressMaximum(max);
					}
				});
				headers = new String[colCount];
				classes = new Class<?>[colCount];
				for (int c = 1 ; c <= meta.getColumnCount() ; c++){
					headers[c-1] = meta.getColumnLabel(c);
					switch (meta.getColumnType(c)){
					case java.sql.Types.CHAR:
					case java.sql.Types.VARCHAR:
						classes[c-1] = String.class;
						break;
					case java.sql.Types.DATE:
					case java.sql.Types.TIMESTAMP:
						classes[c-1] = Date.class;
						break;
					case java.sql.Types.DECIMAL:
					case java.sql.Types.DOUBLE:
					case java.sql.Types.FLOAT:
						classes[c-1] = Double.class;
						break;
					case java.sql.Types.BOOLEAN:
						classes[c-1] = Boolean.class;
						break;
					case java.sql.Types.BIGINT:
						classes[c-1] = Long.class;
						break;
					case java.sql.Types.INTEGER:
					case java.sql.Types.SMALLINT:
					case java.sql.Types.TINYINT:
						classes[c-1] = Integer.class;
						break;
					default:
						if (meta.getColumnTypeName(c).equalsIgnoreCase("TINYINT")){
							classes[c-1] = Integer.class;
						}else if (meta.getColumnTypeName(c).equalsIgnoreCase("BOOLEAN")){
							classes[c-1] = Boolean.class;
						}else if (meta.getColumnTypeName(c).equalsIgnoreCase("VARCHAR")){
							classes[c-1] = String.class;
						}else{
							classes[c-1] = String.class;
						}
						break;
					}
				}
				while (res.next()){
					waitingPanel.setProgressValue(row+1);
					Object[] array = new Object[colCount];
					for (int c = 1 ; c <= meta.getColumnCount() ; c++){
						switch (meta.getColumnType(c)){
						case java.sql.Types.CHAR:
						case java.sql.Types.VARCHAR:
							array[c-1] = res.getString(c);
							break;
						case java.sql.Types.DATE:
						case java.sql.Types.TIMESTAMP:
							array[c-1] = res.getDate(c);
							break;
						case java.sql.Types.DECIMAL:
						case java.sql.Types.DOUBLE:
						case java.sql.Types.FLOAT:
							array[c-1] = res.getDouble(c);
							break;
						case java.sql.Types.BOOLEAN:
							array[c-1] = res.getBoolean(c);
							break;
						case java.sql.Types.BIGINT:
							array[c-1] = res.getLong(c);
							break;
						case java.sql.Types.INTEGER:
						case java.sql.Types.SMALLINT:
						case java.sql.Types.TINYINT:
							array[c-1] = res.getInt(c);
							break;
						default:
							if (meta.getColumnTypeName(c).equalsIgnoreCase("TINYINT")){
								array[c-1] = res.getInt(c);
							}else if (meta.getColumnTypeName(c).equalsIgnoreCase("BOOLEAN")){
								array[c-1] = res.getBoolean(c);
							}else if (meta.getColumnTypeName(c).equalsIgnoreCase("VARCHAR")){
								array[c-1] = res.getString(c);
							}else{
								array[c-1] = res.getString(c);
							}
							break;
						}
					}
					arrayList.add(array);
					row++;
				}
				int rowCount = arrayList.size();
				data = new Object[rowCount][colCount];
				row = 0;
				for (Object[] array : arrayList){
					data[row] = array;
					row++;
				}

			}
			waitingPanel.setProgressDone();
			tableModel = new StatisticsTableModel(data, headers, classes);
			sorter = new TableRowSorter<StatisticsTableModel>(tableModel);
			table.setModel(tableModel);		
			table.setRowSorter(sorter);
			for (int i=0 ; i < table.getColumnCount() ; i++){
				if (table.getColumnName(i).equalsIgnoreCase("percent_bad_beads")){
					heatMap.setHeatMap(i, ColorRange.RGB_GREEN_TO_RED, ConversionMethod.RANGE_GIVEN, "5.0", "40.0");
				}else if (table.getColumnName(i).equalsIgnoreCase("percent_unassigned_reads")){
					heatMap.setHeatMap(i, ColorRange.RGB_GREEN_TO_RED, ConversionMethod.RANGE_GIVEN, "0.0", "40.0");				
				}else if (table.getColumnName(i).equalsIgnoreCase("percent_low_mapqv")){
					heatMap.setHeatMap(i, ColorRange.RGB_GREEN_TO_RED, ConversionMethod.RANGE_GIVEN, "5.0", "30.0");
				}else if (table.getColumnName(i).equalsIgnoreCase("percent_duplicates_picard")){
					heatMap.setHeatMap(i, ColorRange.RGB_GREEN_TO_RED, ConversionMethod.RANGE_GIVEN, "10.0", "50.0");
				}else if (table.getColumnName(i).equalsIgnoreCase("percent_not_totally_on_target")){
					heatMap.setHeatMap(i, ColorRange.RGB_GREEN_TO_RED, ConversionMethod.RANGE_GIVEN, "10.0", "40.0");
				}else if (table.getColumnName(i).equalsIgnoreCase("percent_total_mapped")){
					heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "70.0", "100.0");
				}else if (table.getColumnName(i).equalsIgnoreCase("percent_on")){
					heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "40.0", "90.0");
				}else if (table.getColumnName(i).equalsIgnoreCase("average_depth_of_target_coverage")){
					heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "8", "90");
				}else if (table.getColumnName(i).equalsIgnoreCase("coverage_wo_dup")){
					heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "5", "65");
				}else if (table.getColumnName(i).equalsIgnoreCase("coverage_exome_wo_dup")){
					heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "5", "65");
				}
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Run statistics",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	public void applyFilters(){
		List<RowFilter<StatisticsTableModel,Object>> filters = new ArrayList<RowFilter<StatisticsTableModel,Object>>();
		if (textFilter != null) filters.add(textFilter);
		for (RowFilter<StatisticsTableModel,Object> columnFiler : columnFilters.values()) {
			if (columnFiler != null) filters.add(columnFiler);			
		}
		if (!filters.isEmpty()){
			sorter.setRowFilter(RowFilter.andFilter(filters));
		}else{
			sorter.setRowFilter(null);
		}
	}

	public void export(){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile("Run statistics.xlsx");
		Tools.centerWindow(chooser, false);
		chooser.setVisible(true) ;
		if (chooser.getFile() != null) {
			String filename = chooser.getDirectory() + chooser.getFile();
			if (!filename.endsWith(".xlsx")) filename += ".xlsx";
			File xls = new File(filename);
			try{
				waitingPanel.start();
				try{
					Workbook wb = new SXSSFWorkbook(100);  		
					Sheet sheet = wb.createSheet("run statistics");
					sheet.createFreezePane(0, 1);		
					int r = 0;
					Row row = sheet.createRow(r++);
					row.setHeightInPoints(50);
					for (int c = 0 ; c < table.getColumnCount() ; c++){
						row.createCell(c).setCellValue(table.getColumnName(c));
					}
					sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
					int nrow = table.getRowCount();
					waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" samples", false);
					waitingPanel.setProgressMaximum(nrow);

					Map<String, XSSFCellStyle> styles = new HashMap<String, XSSFCellStyle>();		  	
					for (int i=0 ; i < nrow ; i++ ){
						waitingPanel.setProgressValue(r);
						row = sheet.createRow(r++);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							Cell cell = row.createCell(c);
							if (table.getValueAt(i, c) != null){
								String colname = table.getColumnName(c);
								Object value = table.getValueAt(i, c);
								Color color = null;
								if (value != null){
									if (fastQC.contains(colname)){
										if (value.toString().equalsIgnoreCase("pass")) color = Color.green;
										else if (value.toString().equalsIgnoreCase("warn")) color = Color.orange;
										else if (value.toString().equalsIgnoreCase("fail")) color = Color.red;
									}else if (hasHeatMap.contains(colname)){
										color = heatMap.getColor(i, c);
									}
								}  		
								String styleKey  = generateCellStyleKey(color, !leftAligned.contains(colname), percent.contains(colname), (table.getColumnClass(c) == Long.class || table.getColumnClass(c) == Integer.class));
								if (!styles.containsKey(styleKey)){
									styles.put(styleKey, createCellStyle(sheet, cell, color, !leftAligned.contains(colname), percent.contains(colname), (table.getColumnClass(c) == Long.class || table.getColumnClass(c) == Integer.class)));
								}
								cell.setCellStyle(styles.get(styleKey));
								if (table.getColumnClass(c) == Timestamp.class)
									cell.setCellValue((Timestamp)value);
								else if (table.getColumnClass(c) == Integer.class)
									cell.setCellValue(Integer.parseInt(value.toString()));
								else if (table.getColumnClass(c) == Long.class)
									cell.setCellValue(Long.parseLong(value.toString()));
								else if (table.getColumnClass(c) == Double.class && !percent.contains(colname))
									cell.setCellValue(Double.parseDouble(value.toString()));
								else if (table.getColumnClass(c) == Double.class && percent.contains(colname))
									cell.setCellValue(Double.parseDouble(value.toString()) / 100.0);								
								else if (table.getColumnClass(c) == Boolean.class)
									cell.setCellValue(Boolean.parseBoolean(value.toString()));
								else 
									cell.setCellValue(value.toString());
							}
						}
					}
					for (int c = 0 ; c < table.getColumnCount() ; c++){
						sheet.autoSizeColumn(c);					
					}
					waitingPanel.setProgressValue(nrow);
					waitingPanel.setProgressString("Writing file ...",true);		
					try (FileOutputStream fileOut = new FileOutputStream(xls)){
						wb.write(fileOut);
					}
					waitingPanel.setProgressDone();
				}catch(Exception ex){
					waitingPanel.forceStop();
					throw ex;
				}
				waitingPanel.stop();
			}catch (IOException ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Exporting to Excel",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}catch (Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Exporting to Excel",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	private XSSFCellStyle createCellStyle(Sheet sheet, Cell cell, Color color, boolean centered, boolean percent, boolean number){
		XSSFCellStyle cs = (XSSFCellStyle)sheet.getWorkbook().createCellStyle();
		if (color != null){
			cs.setFillPattern(CellStyle.SOLID_FOREGROUND);
			cs.setFillForegroundColor(new XSSFColor(color));  		
		}
		if (percent){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("0%"));
		}
		if (number){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
		}
		if (centered){
			cs.setAlignment(CellStyle.ALIGN_CENTER);
		}
		cs.setBorderBottom(CellStyle.BORDER_DASHED);
		cs.setBorderTop(CellStyle.BORDER_DASHED);
		cs.setBorderLeft(CellStyle.BORDER_DASHED);
		cs.setBorderRight(CellStyle.BORDER_DASHED);
		return cs;
	}

	private String generateCellStyleKey(Color color, boolean centered, boolean percent, boolean number){
		StringBuilder sb = new StringBuilder();
		if (color != null) sb.append(color.getRGB() + "+");
		if (percent) sb.append("P+");
		if (number) sb.append("N+");
		if (centered) sb.append("C+");
		sb.append("dashed");
		return sb.toString();
	}

}
