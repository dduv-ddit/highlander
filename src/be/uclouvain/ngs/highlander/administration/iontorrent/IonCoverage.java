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

package be.uclouvain.ngs.highlander.administration.iontorrent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.sql.ResultSetMetaData;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
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
import be.uclouvain.ngs.highlander.UI.dialog.AskSamplesDialog;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.table.HeatMap;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ColorRange;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ConversionMethod;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Reference;

import java.util.Arrays;

public class IonCoverage extends JFrame {

	public enum Bed {DESIGNED_AMPLICON, FULL_EXON}
	public enum Grouping {INTERVAL, SAMPLE}

	private static final List<String> leftAligned = new ArrayList<String>(Arrays.asList(new String[]{
			"",
	}));
	private static final List<String> percent = new ArrayList<String>(Arrays.asList(new String[]{
			"",
	}));
	private static final List<String> hasHeatMap = new ArrayList<String>(Arrays.asList(new String[]{
			"mean_depth",
	}));

	private static final List<String> coverages = new ArrayList<String>();

	private Reference reference;
	private String panelCode;
	private Set<String> samples = new HashSet<String>();

	private Map<Bed,Map<Grouping, CoverageTableModel>> tableModels = new EnumMap<Bed, Map<Grouping, CoverageTableModel>>(Bed.class);
	private Map<Bed,Map<Grouping, JTable>> tables = new EnumMap<Bed, Map<Grouping, JTable>>(Bed.class);
	private Map<Bed,Map<Grouping, TableRowSorter<CoverageTableModel>>> sorters = new EnumMap<Bed, Map<Grouping, TableRowSorter<CoverageTableModel>>>(Bed.class);
	private Map<JTable, HeatMap> heatMaps = new HashMap<JTable, HeatMap>();

	static private WaitingPanel waitingPanel;
	private JTabbedPane tabs;
	private boolean germinal = true;

	/* Assumes that all ion torrent data goes to panels_torrent_caller
	 * If at some point, different torrent analyses are used (e.g. hg19 & hg38),
	 * I would need to add support in the GUI
	 */
	private AnalysisFull analysis = null;
	
	public IonCoverage(Reference reference, String panelCode){
		this.reference = reference;
		this.panelCode = panelCode;
		for (Bed bed : Bed.values()){
			tableModels.put(bed, new EnumMap<Grouping, IonCoverage.CoverageTableModel>(Grouping.class));
			tables.put(bed, new EnumMap<Grouping, JTable>(Grouping.class));
			sorters.put(bed, new EnumMap<Grouping, TableRowSorter<CoverageTableModel>>(Grouping.class));
		}
		try{
			analysis = new AnalysisFull(new Analysis("panels_torrent_caller"));
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive analysis panels_torrent_caller", ex), "Retreive panels_torrent_caller analysis",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		try{
			coverages.clear();
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SHOW COLUMNS FROM ion_amplicon_coverage")) {
				while(res.next()){
					String col = Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res);
					if (col.startsWith("coverage_above_")){
						coverages.add(col);
					}
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive coverage columns", ex), "Retreive coverage columns",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		percent.addAll(coverages);
		hasHeatMap.addAll(coverages);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/5);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				AskSamplesDialog ask = new AskSamplesDialog(false, analysis, IonCoverage.this.panelCode);
				Tools.centerWindow(ask, false);
				ask.setVisible(true);
				if (ask.getSelection() == null) {
					dispose();
					return;
				}
				samples.addAll(ask.getSelection());
				new Thread(new Runnable() {
					@Override
					public void run() {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
								waitingPanel.setProgressString("Retreiving coverage information",true);
							}
						});
						for (Bed bed : Bed.values()){
							for (Grouping grouping : Grouping.values()){	
								fillTable(bed, grouping);				
							}
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "IonCoverage.fillTable").start();
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
		setTitle("Coverage information on panel " + panelCode);
		setIconImage(Resources.getScaledIcon(Resources.iCoverage, 64).getImage());

		getContentPane().setLayout(new BorderLayout());

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnClose = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel.add(btnClose);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 24));
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						export(Bed.values()[tabs.getSelectedIndex()/Bed.values().length], Grouping.values()[tabs.getSelectedIndex()%Bed.values().length]);
					}
				}, "IonCoverage.export").start();
			}
		});
		panel.add(export);

		final JToggleButton colors = new JToggleButton("Germinal", Resources.getScaledIcon(Resources.iHighlighting, 24), germinal);
		colors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (germinal){
							germinal = false;
							colors.setText("Somatic");
						}else{
							germinal = true;
							colors.setText("Germinal");
						}
						for (Bed bed : Bed.values()){
							for (Grouping grouping : tables.get(bed).keySet()){
								JTable table = tables.get(bed).get(grouping); 
								HeatMap heatMap = heatMaps.get(table);
								for (int i=0 ; i < table.getColumnCount() ; i++){
									if (table.getColumnName(i).equalsIgnoreCase("mean_depth")){
										if (germinal){
											heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "20", "100");
										}else{
											heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "500", "1000");
										}
									}
								}
								table.validate();
								table.repaint();
							}
						}
					}
				});
			}
		});
		panel.add(colors);

		tabs = new JTabbedPane();
		getContentPane().add(tabs, BorderLayout.CENTER);

		for (Bed bed : Bed.values()){
			for (Grouping grouping : Grouping.values()){
				JScrollPane scrollPane = new JScrollPane();
				String tabName = bed + "-" + grouping;
				switch (bed) {
				case DESIGNED_AMPLICON:
					switch (grouping) {
					case INTERVAL:
						tabName = "Designed amplicons grouped by amplicon";
						break;
					case SAMPLE:
						tabName = "Designed amplicons grouped by sample";
						break;
					}
					break;
				case FULL_EXON:
					switch (grouping) {
					case INTERVAL:
						tabName = "Full exons grouped by exon";
						break;
					case SAMPLE:
						tabName = "Full exons grouped by sample";
						break;
					}
					break;
				}
				tabs.addTab(tabName, scrollPane);

				JTable table = new JTable(){
					@Override
					protected JTableHeader createDefaultTableHeader() {
						return new JTableHeader(columnModel) {
							@Override
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
				};
				tables.get(bed).put(grouping, table);
				heatMaps.put(table, new HeatMap(table));
				//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());
				table.setDefaultRenderer(Integer.class, new ColoredTableCellRenderer());
				table.setDefaultRenderer(Double.class, new ColoredTableCellRenderer());
				table.setDefaultRenderer(Boolean.class, new ColoredTableCellRenderer());
				table.setDefaultRenderer(Date.class, new ColoredTableCellRenderer());
				table.setDefaultRenderer(Long.class, new ColoredTableCellRenderer());
				scrollPane.setViewportView(table);
			}
		}

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}	

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			String colname = table.getColumnName(column);

			if (leftAligned.contains(colname)){
				label.setHorizontalAlignment(SwingConstants.LEFT);
			}else{
				label.setHorizontalAlignment(SwingConstants.CENTER);
			}

			if (value == null) {
				value = "";
			}else{      	
				if (percent.contains(colname)){
					value = Tools.doubleToString(((Double)value)*100.0, 0, false) + "%";
				}else if (table.getColumnClass(column) == Double.class){
					value = Tools.doubleToString((Double)value, 0, false);
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
				if (hasHeatMap.contains(colname)){
					label.setBackground(heatMaps.get(table).getColor(row, column));
				}
				label.setText(value.toString());
			}      
			if (isSelected) {
				label.setBackground(new Color(51,153,255));
			}
			return label;
		}
	}

	public static class CoverageTableModel	extends AbstractTableModel {
		final private Object[][] data;
		final private String[] headers;
		final private Class<?>[] classes;

		public CoverageTableModel(Object[][] data, String[] headers, Class<?>[] classes) {    	
			this.data = data;
			this.headers = headers;
			this.classes = classes;
		}

		@Override
		public int getColumnCount() {
			return headers.length;
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
		public Class<?> getColumnClass(int col) {
			return classes[col];
		}

		@Override
		public Object getValueAt(int row, int col) {
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


	private void fillTable(Bed bed, Grouping grouping){
		try {			
			StringBuilder query = new StringBuilder();
			String sqlTable = "";
			switch (bed) {
			case DESIGNED_AMPLICON:
				sqlTable = "ion_amplicon_coverage";
				break;
			case FULL_EXON:
				sqlTable = "ion_exon_coverage";
				break;
			}
			switch(grouping){
			case INTERVAL:				
				query.append("SELECT amplicon as exon, gene_symbol, avg(total_depth) as total_depth, avg(mean_depth) as mean_depth");
				for (String cov : coverages){
					query.append(", avg("+cov+") as "+cov);
				}
				query.append(" FROM "+sqlTable+" cov JOIN projects as p USING (project_id) ");
				break;
			case SAMPLE:
				query.append("SELECT sample, amplicon as exon, gene_symbol, total_depth, mean_depth");
				for (String cov : coverages){
					query.append(", "+cov);
				}
				query.append(" FROM "+sqlTable+" as cov JOIN projects as p USING (project_id) ");
				break;
			}
			query.append("WHERE reference = '"+reference+"' AND panel_code = '"+panelCode+"'");			
			switch(grouping){
			case INTERVAL:
				query.append(" AND sample IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+") GROUP BY amplicon, gene_symbol ORDER BY amplicon");
				break;
			case SAMPLE:
				query.append(" AND sample IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+") ORDER BY sample, amplicon");
				break;
			}

			List<Object[]> arrayList = new ArrayList<Object[]>();
			int row = 0;
			int colCount = 0;
			String[] headers = null;
			Class<?>[] classes = null;
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString())) {
				ResultSetMetaData meta = res.getMetaData();
				colCount = meta.getColumnCount();
				headers = new String[colCount];
				classes = new Class<?>[colCount];
				for (int c = 1 ; c <= meta.getColumnCount() ; c++){
					headers[c-1] = meta.getColumnName(c);
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
			}

			int rowCount = arrayList.size();
			if (grouping == Grouping.INTERVAL) rowCount++;
			Object[][] data = new Object[rowCount][colCount];
			row = (grouping == Grouping.INTERVAL) ? 1 : 0;
			for (Object[] array : arrayList){
				data[row] = array;
				row++;
			}

			String bthing = "";
			switch (bed) {
			case DESIGNED_AMPLICON:
				bthing = "amplicons";
				break;
			case FULL_EXON:
				bthing = "exons";
				break;
			}
			if (grouping == Grouping.INTERVAL){
				data[0][0] = "[*] All selected " + bthing;
				for (int c=2 ; c < colCount ; c++){
					data[0][c] = 0.0;					
				}
				for (int r=1 ; r < rowCount ; r++){
					for (int c=2 ; c < colCount ; c++){
						data[0][c] = Double.parseDouble(data[0][c].toString()) + (Double.parseDouble(data[r][c].toString())/(rowCount-1));
					}
				}
			}

			CoverageTableModel tableModel = new CoverageTableModel(data, headers, classes);
			TableRowSorter<CoverageTableModel> sorter = new TableRowSorter<CoverageTableModel>(tableModel);
			JTable table = tables.get(bed).get(grouping); 
			table.setModel(tableModel);		
			table.setRowSorter(sorter);
			HeatMap heatMap = heatMaps.get(table);
			for (int i=0 ; i < table.getColumnCount() ; i++){
				if (table.getColumnName(i).startsWith("coverage_above_")){
					heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "0.5", "1.0");
				}else if (table.getColumnName(i).equalsIgnoreCase("mean_depth")){
					if (germinal){
						heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "20", "100");
					}else{
						heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "500", "1000");
					}
				}
				int width = 0;
				for (row = 0; row < table.getRowCount(); row++) {
					TableCellRenderer renderer = table.getCellRenderer(row, i);
					Component comp = table.prepareRenderer(renderer, row, i);
					width = Math.max (comp.getPreferredSize().width, width);
				}
				table.getColumnModel().getColumn(i).setPreferredWidth(width+20);
			}
			tableModels.get(bed).put(grouping, tableModel);
			sorters.get(bed).put(grouping, sorter);
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Coverage information",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void export(Bed bed, Grouping grouping){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile(Tools.formatFilename(panelCode + " coverage by "+grouping+".xlsx"));
		Tools.centerWindow(chooser, false);
		chooser.setVisible(true) ;
		if (chooser.getFile() != null) {
			String filename = chooser.getDirectory() + chooser.getFile();
			if (!filename.endsWith(".xlsx")) filename += ".xlsx";
			File xls = new File(filename);
			try{
				waitingPanel.start();
				JTable table = tables.get(bed).get(grouping);
				HeatMap heatMap = heatMaps.get(table);
				try{
					try(Workbook wb = new SXSSFWorkbook(100)){  		
						Sheet sheet = wb.createSheet("coverage info");
						sheet.createFreezePane(0, 1);		
						int r = 0;
						Row row = sheet.createRow(r++);
						row.setHeightInPoints(50);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							row.createCell(c).setCellValue(table.getColumnName(c));
						}
						sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
						int nrow = table.getRowCount();
						waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" rows", false);
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
										if (hasHeatMap.contains(colname)){
											color = heatMap.getColor(i, c);
										}
									}  		
									String styleKey  = generateCellStyleKey(color, !leftAligned.contains(colname), percent.contains(colname), (table.getColumnClass(c) == Long.class || table.getColumnClass(c) == Integer.class));
									if (!styles.containsKey(styleKey)){
										styles.put(styleKey, createCellStyle(sheet, cell, color, !leftAligned.contains(colname), percent.contains(colname), (table.getColumnClass(c) == Long.class || table.getColumnClass(c) == Integer.class)));
									}
									cell.setCellStyle(styles.get(styleKey));
									if (table.getColumnClass(c) == OffsetDateTime.class)
										cell.setCellValue(((OffsetDateTime)value).toLocalDateTime());
									else if (table.getColumnClass(c) == Integer.class)
										cell.setCellValue(Integer.parseInt(value.toString()));
									else if (table.getColumnClass(c) == Long.class)
										cell.setCellValue(Long.parseLong(value.toString()));
									else if (table.getColumnClass(c) == Double.class)
										cell.setCellValue(Double.parseDouble(value.toString()));
									else if (table.getColumnClass(c) == Boolean.class)
										cell.setCellValue(Boolean.parseBoolean(value.toString()));
									else 
										cell.setCellValue(StringUtils.left(value.toString(), 32765)); //The maximum length of cell contents (text) is 32767 characters
								}
							}
						}
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							//sheet.autoSizeColumn(c);
							//Don't work with Java 7, Windows 7 and fonts not installed in the JVM. Here by default it would be Calibri, and then size is evaluated to zero for strings
							//http://stackoverflow.com/questions/16943493/apache-poi-autosizecolumn-resizes-incorrectly
						}
						waitingPanel.setProgressValue(nrow);
						waitingPanel.setProgressString("Writing file ...",true);		
						try (FileOutputStream fileOut = new FileOutputStream(xls)){
							wb.write(fileOut);
						}
						waitingPanel.setProgressDone();
					}
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
			cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			cs.setFillForegroundColor(new XSSFColor(color, null));  		
		}
		if (percent){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("0%"));
		}
		if (number){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
		}
		if (centered){
			cs.setAlignment(HorizontalAlignment.CENTER);
		}
		cs.setBorderBottom(BorderStyle.DASHED);
		cs.setBorderTop(BorderStyle.DASHED);
		cs.setBorderLeft(BorderStyle.DASHED);
		cs.setBorderRight(BorderStyle.DASHED);
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
