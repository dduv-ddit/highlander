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

package be.uclouvain.ngs.highlander.UI.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import java.awt.FlowLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.dialog.FilteringTree;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.tools.BamViewer;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.administration.users.User.TargetColor;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.JSon;
import be.uclouvain.ngs.highlander.database.Field.SampleType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.SortingCriterion;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;


public class VariantsTable extends JPanel {

	private Highlander mainFrame;
	private JScrollPane scrollPane = new JScrollPane();
	private WelcomePage startTxt = new WelcomePage(true);
	private JTable table;
	private Palette evenRowsColor = Palette.Orange;
	private Palette sameVariantColor = Palette.Purple;
	private XTableColumnModel columnModel;
	private TableRowSorter<VariantsTableModel> sorter;
	private List<SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
	private RowFilter<VariantsTableModel, Object> textFilter = null;
	private RowFilter<VariantsTableModel, Object> sampleFilter = null;
	private RowFilter<VariantsTableModel, Object> interestFilter = null;
	private RowFilter<VariantsTableModel, Object> evaluationFilter = null;
	private String rowFilterExp = null;

	protected List<String> columnToolTips = new ArrayList<String>();
	private JLabel messageLabel;
	private JProgressBar memoryBar = new JProgressBar();
	private JProgressBar countBar = new JProgressBar();
	private JLabel databaseLoadLabel;
	private String nbUniques = "?";

	public VariantsTable(Highlander mainFrame, boolean bottomBarVisible) {
		this.mainFrame = mainFrame;
		try{
			String color = Highlander.getLoggedUser().loadSetting(Settings.COLOR, TargetColor.VARIANT_TABLE.toString());
			if (color != null) evenRowsColor = Palette.valueOf(color);
			color = Highlander.getLoggedUser().loadSetting(Settings.COLOR, TargetColor.SAME_VARIANT.toString());
			if (color != null) sameVariantColor = Palette.valueOf(color);
			initUI(bottomBarVisible);
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	public VariantsTable(Highlander mainFrame) {
		this(mainFrame, true);
	}

	private void initUI(boolean bottomBarVisible) throws Exception {
		setLayout(new BorderLayout());
		columnModel = new XTableColumnModel();
		VariantsTableModel model = new VariantsTableModel(new Field[0],new Object[0][0],new int[0],new String[0]);
		sorter = new TableRowSorter<VariantsTableModel>(model);
		table = new JTable(model){
			@Override
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				int colIndex = columnAtPoint(p);
				//Seems we don't need to convert anything at this stage, we are in the model ??
				//int realRowIndex = convertRowIndexToModel(rowIndex);
				//int realColumnIndex = convertColumnIndexToModel(colIndex);
				Object val = getValueAt(rowIndex, colIndex);
				if (val != null) {
					if (getColumnClass(colIndex) == Integer.class) val = Tools.intToString(Integer.parseInt(val.toString()));
					else if (getColumnClass(colIndex) == Long.class) val = Tools.longToString(Long.parseLong(val.toString()));
				}
				tip = (val != null) ? val.toString() : "";
				return tip;
			}

			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						if (index >= 0){
							int realIndex = columnModel.getColumn(index).getModelIndex();
							return ((VariantsTableModel)table.getModel()).getColumnDescription(realIndex);
						}else{
							return null;
						}
					}
				};
			}

		};
		table.setColumnModel(columnModel);
		table.setCellSelectionEnabled(false);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.createDefaultColumnsFromModel();
		table.setRowSorter(sorter);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(true);
		for (Class<?> cl : Field.AVAILABLE_CLASSES){
			table.setDefaultRenderer(cl, new ColoredTableCellRenderer());			
		}
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		for (int col = 0 ; col < model.getColumnCount() ; col++){
			sorter.setSortable(col, false);
		}
		Highlander.getCellRenderer().registerTableForHighlighting("Variants", table);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting())
					return;
				List<Integer> selection = getSelectedVariantsId();
				if (!selection.isEmpty()){
					mainFrame.getDetailsPanel().setSelection(selection.get(0), VariantsTable.this);
				}
				table.validate();
				table.repaint();
			}
		}); 
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);	
		scrollPane.setViewportView(startTxt);	
		add(scrollPane, BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new BorderLayout(0,0));

		JPanel panel = new JPanel();
		((FlowLayout) panel.getLayout()).setAlignment(FlowLayout.LEADING);
		southPanel.add(panel, BorderLayout.WEST);

		databaseLoadLabel = new JLabel();
		panel.add(databaseLoadLabel);
		checkDatabaseLoad();

		JLabel memoryIcon = new JLabel(Resources.getScaledIcon(Resources.iMemory, 32));
		panel.add(memoryIcon);

		memoryBar.setPreferredSize(new Dimension(150, 24));
		memoryBar.setMaximum((int)(Runtime.getRuntime().maxMemory() / 1024 /1024));
		memoryBar.setValue(Tools.getUsedMemoryInMb());
		memoryBar.setString(Tools.doubleToString(((double)Tools.getUsedMemoryInMb() / 1024.0), 1, false) + " Gb / " 
				+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024) / 1024.0), 1, false)) + " Gb");
		memoryBar.setStringPainted(true);
		memoryBar.setToolTipText("Memory currently used by Highlander with bar length equal to allowed memory");
		panel.add(memoryBar);
		runMemoryBarUpdater();

		countBar.setPreferredSize(new Dimension(300, 24));
		countBar.setMaximum(1000);
		countBar.setValue(0);
		countBar.setString("-");
		countBar.setStringPainted(true);
		countBar.setToolTipText("Number of displayed variants");
		panel.add(countBar);

		messageLabel = new JLabel();
		setMessageToCurrentAnalysis();
		panel.add(messageLabel);

		JPanel variantListPanel = new JPanel();
		((FlowLayout)panel.getLayout()).setAlignment(FlowLayout.TRAILING);
		southPanel.add(variantListPanel, BorderLayout.EAST);

		JButton btnSave = new JButton(Resources.getScaledIcon(Resources.iVariantListSave, 30));
		btnSave.setToolTipText("Save current variant list in your profile");
		btnSave.setPreferredSize(new Dimension(32,32));
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						mainFrame.saveVariantList();
					}
				}, "VariantsTable.saveVariantList").start();
			}
		});
		variantListPanel.add(btnSave);

		JButton btnLoad = new JButton(Resources.getScaledIcon(Resources.iVariantListLoad, 30));
		btnLoad.setToolTipText("Load a variant list from your profile");
		btnLoad.setPreferredSize(new Dimension(32,32));
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						mainFrame.loadVariantList();
					}
				}, "VariantsTable.loadVariantList").start();
			}
		});
		variantListPanel.add(btnLoad);

		southPanel.setVisible(bottomBarVisible);

		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(southPanel, Highlander.getHighlanderObserver(), 32);
		add(scrollablePanel, BorderLayout.SOUTH);

	}

	public void showWelcome(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				scrollPane.setViewportView(startTxt);
			}
		});
	}

	public void checkDatabaseLoad(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				HighlanderDatabase DB = Highlander.getDB();
				try{
					while (!DB.getDataSource(Schema.HIGHLANDER).isClosed()){
						boolean soft = false; 
						boolean hard = false; 
						try (Results res = DB.select(Schema.HIGHLANDER, "SELECT update_soft, update_hard FROM main", false)) {
							if (res.next()){
								soft = res.getBoolean("update_soft");
								hard = res.getBoolean("update_hard");
							}
						}
						if (hard){
							databaseLoadLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallRed, 24));
							databaseLoadLabel.setText("Database is being updated, launching queries is strongly discouraged !");
						}else if (soft){
							databaseLoadLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallOrange, 24));
							databaseLoadLabel.setText("New samples are being processed by the pipeline");
						}else{
							databaseLoadLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallGreen, 24));
							databaseLoadLabel.setText("Database is ready");
						}
						Thread.sleep(60_000);
					}
				}catch(Exception ex){
					Tools.exception(ex);
				}				
			}
		}, "VariantsTable.checkDatabaseLoad").start();
	}

	public void setMessageToCurrentAnalysis(){
		messageLabel.setIcon(Resources.getScaledIcon(Highlander.getCurrentAnalysis().getIcon(), 32));
		String numSample = "";
		try{
			Map<SampleType, Integer> map = new TreeMap<>();
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT sample_type, COUNT(*) as ct "
							+ "FROM projects JOIN projects_analyses USING (project_id) "
							+ "JOIN pathologies USING (pathology_id) "
							+ "WHERE analysis = '"+Highlander.getCurrentAnalysis()+"' "
							+ "GROUP BY sample_type")) {
				while (res.next()){
					SampleType type = SampleType.valueOf(res.getString("sample_type"));
					int num = res.getInt("ct");
					map.put(type, num);
				}
			}
			int total = 0;
			for (int num : map.values()) total += num;
			numSample += " holding "+total+" samples (";
			boolean first = true;
			for (SampleType type : map.keySet()) {
				if (first) first = false;
				else numSample += ", ";
				numSample += map.get(type) + " " + type;
			}
			numSample += ")";
		}catch(Exception ex){
			Tools.exception(ex);
		}
		messageLabel.setText("Connected to " + Highlander.getCurrentAnalysis() + " database" + numSample);
	}

	public void newFilter(String filteringExpression) {
		RowFilter<VariantsTableModel, Object> rf = null;
		//If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)"+filteringExpression);
		} catch (java.util.regex.PatternSyntaxException e) {
			return;
		}
		sorter.setRowFilter(rf);
	}

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			//int realRow = table.convertRowIndexToModel(row);
			int realCol = table.convertColumnIndexToModel(column);
			Field field = ((VariantsTableModel)table.getModel()).getColumnField(realCol);
			int alignment = ((VariantsTableModel)table.getModel()).getColumnAlignment(realCol);
			Highlander.getCellRenderer().renderCell(label, value, field, alignment, row, isSelected, Resources.getTableEvenRowBackgroundColor(evenRowsColor), Color.WHITE, true);
			if (rowFilterExp != null && value != null && rowFilterExp.length() > 0 && value.toString().toLowerCase().contains(rowFilterExp.toLowerCase())){
				Font font = label.getFont();
				label.setFont(font.deriveFont(Font.BOLD));
				label.setForeground(Color.green);
			}
			return label;
		}
	}

	public static class VariantsTableModel	extends AbstractTableModel {
		private Object[][] data;
		private int[] id;
		private String[] uniqueVariant;
		private Field[] headers;

		public VariantsTableModel(Field[] headers, Object[][] data, int[] id, String[] uniqueVariant) throws Exception {    	
			this.data = data;
			this.headers = headers;
			this.id = id;
			this.uniqueVariant = uniqueVariant;
		}

		public int getColumnCount() {
			return headers.length;
		}

		public String getColumnName(int col) {
			return headers[col].getName();
		}

		public int getColumnIndex(Field field){
			for (int i=0 ; i < headers.length ; i++){
				if (headers[i].getName().equals(field.getName())) return i;
			}
			return -1;
		}

		public int getColumnAlignment(int col){
			return headers[col].getAlignment();
		}

		public String getColumnDescription(int col) {
			return headers[col].getHtmlTooltip();
		}

		public int getRowCount() {
			return data.length;
		}

		public Class<?> getColumnClass(int columnIndex) {
			return headers[columnIndex].getFieldClass();
		}

		public Field getColumnField(int columnIndex) {
			return headers[columnIndex];
		}
		
		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		public List<Integer> getVariantIds(){
			List<Integer> list = new ArrayList<Integer>(id.length);
			for (int i : id) list.add(i);
			return list;
		}

		public int getVariantId(int row){
			return id[row];
		}

		public String getVariantUniqueLabel(int row){
			return uniqueVariant[row];
		}
		
		public void setAnnotation(int variantSampleId, Field fieldToModify, Object valueToModify){
			for (int i=0 ; i < id.length ; i++){
				if (id[i] == variantSampleId) {
					int j = getColumnIndex(fieldToModify);
					if (j >= 0){
						data[i][j] = valueToModify;
						final int row = i;
						SwingUtilities.invokeLater(new Runnable(){
							public void run(){
								try{
									fireTableRowsUpdated(row,row);
								}catch(Exception ex){
									Tools.exception(ex);
								}
							}
						});
					}
					break;
				}
			}
		}

		public void setAnnotation(String chr, int pos, int length, String reference, String alternative, String geneSymbol, Field fieldToModify, Object valueToModify){
			try{
				int k = getColumnIndex(Field.gene_symbol);
				if (k >= 0){
					for (int i=0 ; i < id.length ; i++){
						if (data[i][k] != null && data[i][k].equals(geneSymbol) && uniqueVariant[i].equals(chr+"-"+pos+"-"+length+"-"+reference+"-"+alternative)) {
							int j = getColumnIndex(fieldToModify);
							if (j >= 0){
								data[i][j] = valueToModify;
								final int row = i;
								SwingUtilities.invokeLater(new Runnable(){
									public void run(){
										try{
											fireTableRowsUpdated(row,row);
										}catch(Exception ex){
											Tools.exception(ex);
										}
									}
								});
							}
						}
					}
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
		
		public void setAnnotation(Field conditionField, Object conditionValue, Field fieldToModify, Object valueToModify){
			try{
				int k = getColumnIndex(conditionField);
				if (k >= 0){
					for (int i=0 ; i < id.length ; i++){
						if (data[i][k] != null && data[i][k].equals(conditionValue)) {
							int j = getColumnIndex(fieldToModify);
							if (j >= 0){
								data[i][j] = valueToModify;
								final int row = i;
								SwingUtilities.invokeLater(new Runnable(){
									public void run(){
										try{
											fireTableRowsUpdated(row,row);
										}catch(Exception ex){
											Tools.exception(ex);
										}
									}
								});
							}
						}
					}
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}

		public Object[] getRow(int variantId){
			for (int i=0 ; i < id.length ; i++){
				if (id[i] == variantId) return data[i];
			}
			return null;
		}

		public void setValueAt(Object value, int row, int col) {
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}

	public JTable getTable(){
		return table;
	}

	public boolean isEmpty(){
		return (table.getRowCount() == 0);
	}

	public void updateAnnotation(int variantId, Field fieldToModify, Object valueToModify){
		((VariantsTableModel)table.getModel()).setAnnotation(variantId, fieldToModify, valueToModify);
	}

	public void updateAnnotation(String chr, int pos, int length, String reference, String alternative, String geneSymbol, Field fieldToModify, Object valueToModify){
		((VariantsTableModel)table.getModel()).setAnnotation(chr, pos, length, reference, alternative, geneSymbol, fieldToModify, valueToModify);
	}
	
	public void updateAnnotation(Field conditionField, Object conditionValue, Field fieldToModify, Object valueToModify){
		((VariantsTableModel)table.getModel()).setAnnotation(conditionField, conditionValue, fieldToModify, valueToModify);
	}

	public List<Field> getAvailableColumns(){
		List<Field> list = new ArrayList<Field>();
		for (Field field : ((VariantsTableModel)table.getModel()).headers){
			if (hasColumn(field)) list.add(field);
		}
		return list;
	}

	public boolean hasColumn(Field field){
		for(Enumeration<TableColumn> it = columnModel.getColumns(true) ; it.hasMoreElements() ; ){
			TableColumn col = it.nextElement();
			if (col.getHeaderValue().toString().equals(field.getName())) return true;
		}
		return false;
	}

	public int getColumnIndex(Field field) {
		return ((VariantsTableModel)table.getModel()).getColumnIndex(field);
	}

	public Object[] getRow(int id){
		return ((VariantsTableModel)table.getModel()).getRow(id);
	}

	public void fillTable(final VariantResults variantResults){
		scrollPane.setViewportView(table);
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					VariantsTableModel model = new VariantsTableModel(variantResults.headers, variantResults.data, variantResults.id, variantResults.variant);
					sorter = new TableRowSorter<VariantsTableModel>(model);
					table.setModel(model);					
					table.setRowSorter(sorter);
					sorter.setSortKeys(sortKeys);
					for (int i=0 ; i < variantResults.headers.length ; i++){
						table.getColumnModel().getColumn(i).setPreferredWidth(variantResults.headers[i].getSize());
						sorter.setSortable(i, false);
					}
					countBar.setMaximum(variantResults.data.length);
					countBar.setValue(variantResults.data.length);
					nbUniques = variantResults.getNumberUniqueVariants();
					if (nbUniques.equals("?")){
						countBar.setString(Tools.doubleToString(variantResults.data.length, 0, false) + " / " + Tools.doubleToString(variantResults.data.length, 0, false) + " variants");
					}else{
						countBar.setString(Tools.doubleToString(variantResults.data.length, 0, false) + " / " + Tools.doubleToString(variantResults.data.length, 0, false) + " variants ("+nbUniques+" uniques)");						
					}
					countBar.setStringPainted(true);
					Highlander.getCellRenderer().registerTableForHeatMap(VariantsTable.this);
					mainFrame.refreshTableView();
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		});		

	}

	public Palette getColor(TargetColor target) {
		switch(target) {
		case VARIANT_TABLE:
			return evenRowsColor;
		case SAME_VARIANT:
			return sameVariantColor;
		default:
			return null;
		}
	}
	
	public void setColor(TargetColor target, Palette color) {
		switch(target) {
		case VARIANT_TABLE:
			evenRowsColor = color;
			break;
		case SAME_VARIANT:
			sameVariantColor = color;
			break;
		}
		table.validate();
		table.repaint();
	}

	public void setSorting(List<SortingCriterion> sortItems){
		sortKeys.clear();
		VariantsTableModel model = (VariantsTableModel)table.getModel();
		for (SortingCriterion item  : sortItems){	
			if (model.getColumnIndex(item.getField()) != -1) {
				sortKeys.add(new SortKey(table.convertColumnIndexToModel(table.getColumnModel().getColumnIndex(item.getFieldName())), item.getSortOrder()));
			}
		}
		sorter.setSortKeys(sortKeys);
	}

	public void setTextFilter(RowFilter<VariantsTableModel, Object> rf, String filteringExpression){
		textFilter = rf;
		rowFilterExp = filteringExpression;
		applyFilters();
	}

	public void setSampleFilter(Set<String> samples){
		if (samples.isEmpty()){
			sampleFilter = null;
		}else{
			ArrayList<RowFilter<Object,Object>> sampleFilters = new ArrayList<RowFilter<Object,Object>>();
			for (String sample : samples){
				sampleFilters.add(RowFilter.regexFilter(sample, table.getColumnModel().getColumnIndex("sample")));
			}
			sampleFilter = RowFilter.orFilter(sampleFilters);
		}
		applyFilters();
	}

	public void setInterestFilter(final boolean yes, final boolean maybe, final boolean no){		
		RowFilter<VariantsTableModel, Object> filter = new RowFilter<VariantsTableModel, Object>() {
			@Override
			public boolean include(javax.swing.RowFilter.Entry<? extends VariantsTableModel, ? extends Object> entry) {
				VariantsTableModel model = entry.getModel();
				if (model.getColumnIndex(Field.variant_of_interest) == -1) return true; //column is absent from the table
				Boolean interest = (Boolean)model.getValueAt((Integer)entry.getIdentifier(), model.getColumnIndex(Field.variant_of_interest));
				if (interest == null && maybe) {
					return true;
				}
				if (interest != null && interest == false && no) {
					return true;
				}
				if (interest != null && interest == true && yes) {
					return true;
				}
				return false;
			}
		};
		interestFilter = filter;
		applyFilters();
	}

	public void setEvaluationFilter(final boolean unclassified, final boolean I, final boolean II, final boolean III, final boolean IV, final boolean V){		
		RowFilter<VariantsTableModel, Object> filter = new RowFilter<VariantsTableModel, Object>() {
			@Override
			public boolean include(javax.swing.RowFilter.Entry<? extends VariantsTableModel, ? extends Object> entry) {
				VariantsTableModel model = entry.getModel();
				if (model.getColumnIndex(Field.evaluation) == -1) return true; //column is absent from the table
				int interest = (Integer)model.getValueAt((Integer)entry.getIdentifier(), model.getColumnIndex(Field.evaluation));
				if (interest == 0 && unclassified) {
					return true;
				}
				if (interest == 1 && I) {
					return true;
				}
				if (interest == 2 && II) {
					return true;
				}
				if (interest == 3 && III) {
					return true;
				}
				if (interest == 4 && IV) {
					return true;
				}
				if (interest == 5 && V) {
					return true;
				}
				if (interest < 1 && interest > 5 && unclassified) {
					return true;
				}
				return false;
			}
		};
		evaluationFilter = filter;
		applyFilters();
	}

	public void applyFilters(){
		List<RowFilter<VariantsTableModel,Object>> filters = new ArrayList<RowFilter<VariantsTableModel,Object>>();
		if (textFilter != null) filters.add(textFilter);
		if (sampleFilter != null) filters.add(sampleFilter);
		if (interestFilter != null) filters.add(interestFilter);
		if (evaluationFilter != null) filters.add(evaluationFilter);
		if (!filters.isEmpty()){
			sorter.setRowFilter(RowFilter.andFilter(filters));
		}
		countBar.setValue(sorter.getViewRowCount());
		if (nbUniques.equals("?")){
			countBar.setString(Tools.doubleToString(sorter.getViewRowCount(), 0, false) + " / " + Tools.doubleToString(sorter.getModelRowCount(), 0, false) + " variants");
		}else{
			countBar.setString(Tools.doubleToString(sorter.getViewRowCount(), 0, false) + " / " + Tools.doubleToString(sorter.getModelRowCount(), 0, false) + " variants ("+nbUniques+" uniques)");
		}
		countBar.setStringPainted(true);				     
	}

	public void setHiddenColumns(List<Field> mask){
		columnModel.setAllColumnsVisible();
		for(Enumeration<TableColumn> it = columnModel.getColumns(false) ; it.hasMoreElements() ; ){
			TableColumn col = it.nextElement();
			Field field = Field.getField(col.getHeaderValue().toString());
			if (mask.contains(field)) columnModel.setColumnVisible(col, false);
		}
		table.validate();
	}

	public void scrollToColumn(Field field) {
		VariantsTableModel model = (VariantsTableModel)table.getModel();
		if (model.getColumnIndex(field) >= 0) {
			table.scrollRectToVisible(table.getCellRect(table.getSelectedRow(), table.convertColumnIndexToView(model.getColumnIndex(field)), true));
		}
	}
	
	public Set<String> getDistinctValues(Field field){
		Set<String> set = new TreeSet<String>();
		if (hasColumn(field)){
			int col = table.getColumnModel().getColumnIndex(field.getName());
			if (col < 0) return set;
			for (int row=0 ; row < table.getRowCount() ; row++){
				Object o = table.getValueAt(row, col); 
				if (o != null) set.add(o.toString());
			}
		}
		return set;	
	}

	public List<Integer> getAllVariantsIds(){
		VariantsTableModel model = (VariantsTableModel)table.getModel();
		return model.getVariantIds();
	}

	public List<Integer> getSelectedVariantsId(){
		List<Integer> selection = new ArrayList<Integer>();
		try{
			VariantsTableModel model = (VariantsTableModel)table.getModel();
			for (int row : table.getSelectedRows()){
				selection.add(model.getVariantId(table.convertRowIndexToModel(row)));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		return selection;
	}

	public List<String> getSelectedUniqueVariantId() {
		List<String> selection = new ArrayList<String>();
		try{
			VariantsTableModel model = (VariantsTableModel)table.getModel();
			for (int row : table.getSelectedRows()){
				selection.add(model.getVariantUniqueLabel(table.convertRowIndexToModel(row)));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		return selection;
	}
	
	public String getUniqueVariantId(int row) {
		return ((VariantsTableModel)table.getModel()).getVariantUniqueLabel(table.convertRowIndexToModel(row));
	}
	
	public void setRowSelectionAllowed(boolean allow){
		if (allow){
			table.setCellSelectionEnabled(false);
			table.setRowSelectionAllowed(true);
			table.setColumnSelectionAllowed(false);
		}else{
			table.setRowSelectionAllowed(true);
			table.setColumnSelectionAllowed(true);			
			table.setCellSelectionEnabled(true);
		}
	}

	public void saveUserColumnWidths() {
		try{
			for (int i=0 ; i < table.getColumnModel().getColumnCount() ; i++){
				((VariantsTableModel)table.getModel()).getColumnField(table.convertColumnIndexToModel(i)).saveUserSize(table.getColumnModel().getColumn(i).getPreferredWidth());
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	public void toTSV(File file) throws Exception {
		Highlander.waitingPanel.start();
		char delimiter = '\t';
		char endline = '\n';
		try(FileWriter fw = new FileWriter(file)){
			for (int c = 0 ; c < table.getColumnCount() ; c++){
				fw.write(table.getColumnName(c));
				if (c < table.getColumnCount()-1) fw.write(delimiter);
				else fw.write(endline);
			}
			int nrow = table.getRowCount();
			Highlander.waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" variants", false);
			Highlander.waitingPanel.setProgressMaximum(nrow);
			for (int i=0 ; i < nrow ; i++ ){
				Highlander.waitingPanel.setProgressValue(i);
				for (int c = 0 ; c < table.getColumnCount() ; c++){
					if (table.getValueAt(i, c) != null){
						fw.write(table.getValueAt(i, c).toString());
					}
					if (c < table.getColumnCount()-1) fw.write(delimiter);
					else fw.write(endline);
				}
			}
			Highlander.waitingPanel.setProgressDone();
		}catch(Exception ex){
			Highlander.waitingPanel.forceStop();
			throw ex;
		}
		Highlander.waitingPanel.stop();

	}

	public void toVCF(File file) throws Exception {
		toVCF(file, null);
	}

	/**
	 * Export this table to a VCF file.
	 * Important: if a variant span multiple genes, only one of them will we outputed in the VCF.
	 * 
	 * @param file
	 * @param sampleFilter
	 * @throws Exception
	 */
	public void toVCF(File file, String sampleFilter) throws Exception {
		Highlander.waitingPanel.start();
		AnalysisFull analysis = Highlander.getCurrentAnalysis();
		String columnDelimiter = "\t";
		String idDelimiter = ";";
		String altDelimiter = ",";
		String filterDelimiter = ";";
		String infoDelimiter = ";";
		String formatDelimiter = ":";
		String endline = "\n";
		String missing = ".";
		int cCHR = -1;
		int cPOS = -1;
		int cREF = -1;
		int cALT = -1;
		int cID = -1;
		int cQUAL = -1;
		int cFILTER = -1;
		int cSAMPLE = -1;
		int cGT = -1;
		int cADref = -1;
		int cADalt = -1;
		int cDP = -1;
		List<Integer> cINFO = new ArrayList<>();
		List<Integer> cFORMAT = new ArrayList<>();
		List<Integer> cNOTEXPORTED = new ArrayList<>();
		int nrow = table.getRowCount();
		int ncol = table.getColumnCount();
		VariantsTableModel model = (VariantsTableModel)table.getModel();
		for (int c = 0 ; c < ncol ; c++){
			Field f = model.getColumnField(c);
			if (f.getAnnotationCode() == Annotation.VCF) {
				if (f.getAnnotationHeaders()[0].equals("CHROM")) cCHR = c;	
				else if (f.getAnnotationHeaders()[0].equals("POS")) cPOS = c;	
				else if (f.getAnnotationHeaders()[0].equals("REF")) cREF = c;	
				else if (f.getAnnotationHeaders()[0].startsWith("ALT")) cALT = c;	
				else if (f.getAnnotationHeaders()[0].equals("ID")) cID = c;	
				else if (f.getAnnotationHeaders()[0].equals("QUAL")) cQUAL = c;	
				else if (f.getAnnotationHeaders()[0].equals("FILTER")) cFILTER = c;	
				else if (f.getName().equals(Field.sample.getName())) cSAMPLE = c;	
				else if (f.getAnnotationHeaders()[0].equals("FORMAT") && f.getAnnotationHeaders()[1].equals("GT")) cGT = c;	
				else if (f.getName().equals(Field.allelic_depth_ref.getName())) cADref = c;	
				else if (f.getName().equals(Field.allelic_depth_alt.getName())) cADalt = c;	
				else if (f.getName().equals(Field.read_depth.getName())) cDP = c;	
				else if (f.getAnnotationHeaders()[0].equals("FORMAT")) cFORMAT.add(c);
				else if (f.getAnnotationHeaders()[0].equals("INFO")) cINFO.add(c);
			}else {
				if (f.getJSonPath() == JSon.CALLS) cFORMAT.add(c);
				else if (f.getJSonPath() == JSon.INFO) cINFO.add(c);
				else cNOTEXPORTED.add(c);
			}
		}
		Map<String,Set<Integer>> samples = new TreeMap<>(); // sample -> [set of variant_ids] 
		Map<String, Map<Integer,Map<String,Map<String,Set<Integer>>>>> variantsIds = new HashMap<>(); // chromosome (need to be sorted by hand !) -> position -> reference -> alternative -> [set of variant_ids]
		Map<Integer, String> dbsnps = new HashMap<>(); // variant_id -> ID
		Map<Integer, String> quals = new HashMap<>(); // variant_id -> QUAL
		Map<Integer, String> filters = new HashMap<>(); // variant_id -> FILTER
		Map<Integer, String> gts = new HashMap<>(); // variant_id -> GT
		Map<Integer, String> adrefs = new HashMap<>(); // variant_id -> AD (ref)
		Map<Integer, String> adalts = new HashMap<>(); // variant_id -> AD (alt)
		Map<Integer, String> dps = new HashMap<>(); // variant_id -> DP
		Map<Integer, Integer> rows = new HashMap<>(); // variant_id -> row id
		if (cCHR > -1 && 
				cPOS > -1 && 
				cREF > -1 && 
				cALT > -1 && 
				cID > -1 && 
				cQUAL > -1 && 
				cFILTER > -1 && 
				cSAMPLE > -1 && 
				cGT > -1 && 
				cADref > -1 && 
				cADalt > -1 && 
				cDP > -1 ) {
			//All mandatory fields are present in the table
			for (int i=0 ; i < nrow ; i++ ){
				String sample = table.getValueAt(i, cSAMPLE).toString(); 
				if (sampleFilter == null || sample.equalsIgnoreCase(sampleFilter)) {
					String chromosome = table.getValueAt(i, cCHR).toString();
					int position = Integer.parseInt(table.getValueAt(i, cPOS).toString());
					String reference = table.getValueAt(i, cREF).toString();
					String alternative = table.getValueAt(i, cALT).toString();
					int variantId = model.getVariantId(i);
					if (!samples.containsKey(sample)) {
						samples.put(sample, new HashSet<Integer>());
					}
					samples.get(sample).add(variantId);
					if (!variantsIds.containsKey(chromosome)) {
						variantsIds.put(chromosome, new TreeMap<Integer, Map<String,Map<String,Set<Integer>>>>());
					}
					if (!variantsIds.get(chromosome).containsKey(position)) {
						variantsIds.get(chromosome).put(position, new TreeMap<String,Map<String,Set<Integer>>>());
					}
					if (!variantsIds.get(chromosome).get(position).containsKey(reference)) {
						variantsIds.get(chromosome).get(position).put(reference, new TreeMap<String,Set<Integer>>());
					}
					if (!variantsIds.get(chromosome).get(position).get(reference).containsKey(alternative)) {
						variantsIds.get(chromosome).get(position).get(reference).put(alternative, new TreeSet<Integer>());
					}
					variantsIds.get(chromosome).get(position).get(reference).get(alternative).add(variantId);
					dbsnps.put(variantId, ((table.getValueAt(i, cID) != null) ? table.getValueAt(i, cID).toString() : missing));
					quals.put(variantId, ((table.getValueAt(i, cQUAL) != null) ? table.getValueAt(i, cQUAL).toString() : missing));
					filters.put(variantId, ((table.getValueAt(i, cFILTER) != null) ? table.getValueAt(i, cFILTER).toString() : missing));
					gts.put(variantId, ((table.getValueAt(i, cGT) != null) ? table.getValueAt(i, cGT).toString() : missing));
					adrefs.put(variantId, ((table.getValueAt(i, cADref) != null) ? table.getValueAt(i, cADref).toString() : missing));
					adalts.put(variantId, ((table.getValueAt(i, cADalt) != null) ? table.getValueAt(i, cADalt).toString() : missing));
					dps.put(variantId, ((table.getValueAt(i, cDP) != null) ? table.getValueAt(i, cDP).toString() : missing));
					rows.put(variantId, i);
				}
			}
		}else {
			//Need to fetch missing mandatory fields from the database
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT "
							+Field.variant_sample_id.getQuerySelectName(analysis, false)+", "
							+Field.chr.getQuerySelectName(analysis, false)+", "
							+Field.pos.getQuerySelectName(analysis, false)+", "
							+Field.reference.getQuerySelectName(analysis, false)+", "
							+Field.alternative.getQuerySelectName(analysis, false)+", "
							+Field.dbsnp_id.getQuerySelectName(analysis, false)+", "
							+Field.confidence.getQuerySelectName(analysis, false)+", "
							+Field.filters.getQuerySelectName(analysis, false)+", "
							+Field.sample.getQuerySelectName(analysis, false)+", "
							+Field.zygosity.getQuerySelectName(analysis, false)+", "
							+Field.allelic_depth_ref.getQuerySelectName(analysis, false)+", "
							+Field.allelic_depth_alt.getQuerySelectName(analysis, false)+", "
							+Field.read_depth.getQuerySelectName(analysis, false)
							+" FROM "+analysis.getFromSampleAnnotations()
							+ analysis.getJoinProjects()
							+ analysis.getJoinStaticAnnotations()
							+" WHERE variant_sample_id in ("+HighlanderDatabase.makeSqlList(model.getVariantIds(), Integer.class)+")"
					)){
				while (res.next()) {
					String sample = res.getString(Field.sample.getName()); 
					if (sampleFilter == null || sample.equalsIgnoreCase(sampleFilter)) {
						String chromosome = res.getString(Field.chr.getName());
						int position = res.getInt(Field.pos.getName());
						String reference = res.getString(Field.reference.getName());
						String alternative = res.getString(Field.alternative.getName());
						int variantId = res.getInt("variant_sample_id");
						if (!samples.containsKey(sample)) {
							samples.put(sample, new HashSet<Integer>());
						}
						samples.get(sample).add(variantId);
						if (!variantsIds.containsKey(chromosome)) {
							variantsIds.put(chromosome, new TreeMap<Integer, Map<String,Map<String,Set<Integer>>>>());
						}
						if (!variantsIds.get(chromosome).containsKey(position)) {
							variantsIds.get(chromosome).put(position, new TreeMap<String,Map<String,Set<Integer>>>());
						}
						if (!variantsIds.get(chromosome).get(position).containsKey(reference)) {
							variantsIds.get(chromosome).get(position).put(reference, new TreeMap<String,Set<Integer>>());
						}
						if (!variantsIds.get(chromosome).get(position).get(reference).containsKey(alternative)) {
							variantsIds.get(chromosome).get(position).get(reference).put(alternative, new TreeSet<Integer>());
						}
						variantsIds.get(chromosome).get(position).get(reference).get(alternative).add(variantId);
						dbsnps.put(variantId, ((res.getString(Field.dbsnp_id.getName()) != null) ? res.getString(Field.dbsnp_id.getName()) : missing));
						quals.put(variantId, ((res.getString(Field.confidence.getName()) != null) ? res.getString(Field.confidence.getName()) : missing));
						filters.put(variantId, ((res.getString(Field.filters.getName()) != null) ? res.getString(Field.filters.getName()) : missing));
						gts.put(variantId, ((res.getString(Field.zygosity.getName()) != null) ? res.getString(Field.zygosity.getName()) : missing));
						adrefs.put(variantId, ((res.getString(Field.allelic_depth_ref.getName()) != null) ? res.getString(Field.allelic_depth_ref.getName()) : missing));
						adalts.put(variantId, ((res.getString(Field.allelic_depth_alt.getName()) != null) ? res.getString(Field.allelic_depth_alt.getName()) : missing));
						dps.put(variantId, ((res.getString(Field.read_depth.getName()) != null) ? res.getString(Field.read_depth.getName()) : missing));
					}
				}
			}catch(Exception ex){
				Highlander.waitingPanel.forceStop();
				throw ex;
			}
			for (int i=0 ; i < nrow ; i++ ){
				rows.put(model.getVariantId(i), i);
			}
		}
		List<String> chromosomes = new ArrayList<>(variantsIds.keySet());
		Collections.sort(chromosomes, new Tools.NaturalOrderComparator(true));

		try(FileWriter fw = new FileWriter(file)){
			//meta informations
			fw.write("##fileformat=VCFv4.3" + endline);
			//FILTER
			Set<String> possibleFilters = new TreeSet<>();
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `value` FROM "+analysis.getFromPossibleValues()+" WHERE `field` = 'filters'")) {
				while (res.next()) {
					for (String f : res.getString(1).split(";")) {
						possibleFilters.add(f);
					}
				}
			}
			possibleFilters.remove("PASS");
			for (String f : possibleFilters) {
				fw.write("##FILTER=<ID="+f+",Description=\"\">" + endline);
			}
			//FORMAT
			fw.write("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">" + endline);
			fw.write("##FORMAT=<ID=AD,Number=R,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">" + endline);
			fw.write("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">" + endline);
			for (int format : cFORMAT) {
				Field f = Field.getField(table.getColumnName(format));
				fw.write("##FORMAT=<ID="+f+",Number=G,Type="+f.getVcfClass()+",Description=\""+f.getDescription()+"\">" + endline);
			}
			//INFO
			for (int info : cINFO) {
				Field f = Field.getField(table.getColumnName(info));
				fw.write("##INFO=<ID="+f+",Number=A,Type="+f.getVcfClass()+",Description=\""+f.getDescription()+"\">" + endline);
			}
			fw.write("##HighlanderVersion=\""+Highlander.version+", by Raphael Helaers\"" + endline);
			//Headers
			fw.write("#CHROM"+columnDelimiter+"POS"+columnDelimiter+"ID"+columnDelimiter+"REF"+columnDelimiter+"ALT"+columnDelimiter+"QUAL"+columnDelimiter+"FILTER"+columnDelimiter+"INFO"+columnDelimiter+"FORMAT");
			if (sampleFilter == null) {
				for (String sample : samples.keySet()) {
					fw.write(columnDelimiter + sample);
				}
			}else {
				fw.write(columnDelimiter + sampleFilter);				
			}
			fw.write(endline);
			//Records
			Highlander.waitingPanel.setProgressString("Exporting "+Tools.doubleToString(variantsIds.size(), 0, false)+" chromosomes", false);
			Highlander.waitingPanel.setProgressMaximum(variantsIds.size());
			int progress = 0;
			for (String chr : chromosomes){
				Highlander.waitingPanel.setProgressValue(progress++);
				for (int pos : variantsIds.get(chr).keySet()){
					for (String ref : variantsIds.get(chr).get(pos).keySet()){
						//CHR
						fw.write(chr + columnDelimiter);
						//POS
						fw.write(pos + columnDelimiter);
						//ID
						Set<String> distinctIds = new LinkedHashSet<String>();
						for (Set<Integer> ids : variantsIds.get(chr).get(pos).get(ref).values()) {
							int id = ids.iterator().next();
							if (!dbsnps.get(id).equals(missing) && dbsnps.get(id).length() > 0) {
								distinctIds.add(dbsnps.get(id));
							}
						}
						if (distinctIds.isEmpty()) {
							fw.write(missing);
						}else {
							String[] array = distinctIds.toArray(new String[0]);
							for (int i=0 ; i < array.length ; i++) {
								fw.write(array[i]);
								if (i < array.length-1) fw.write(idDelimiter);
							}
						}
						fw.write(columnDelimiter);
						//REF
						fw.write(ref + columnDelimiter);
						//ALT
						String[] array = variantsIds.get(chr).get(pos).get(ref).keySet().toArray(new String[0]);
						for (int i=0 ; i < array.length ; i++) {
							fw.write(array[i]);
							if (i < array.length-1) fw.write(altDelimiter);
						}
						fw.write(columnDelimiter);
						//QUAL
						double sumQuals = 0;
						int numQuals = 0;
						for (Set<Integer> ids : variantsIds.get(chr).get(pos).get(ref).values()) {
							for (int id : ids) {
								if (!quals.get(id).equals(missing) && quals.get(id).length() > 0) {
									numQuals++;
									sumQuals += Double.parseDouble(quals.get(id));
								}
							}
						}
						if (numQuals == 0) {
							fw.write(missing);
						}else {
							fw.write(Tools.doubleToString(sumQuals/(double)numQuals, 2, false, false));
						}
						fw.write(columnDelimiter);
						//FILTER
						Set<String> distinctFilters = new TreeSet<String>();
						for (Set<Integer> ids : variantsIds.get(chr).get(pos).get(ref).values()) {
							for (int id : ids) {
								if (!filters.get(id).equals(missing) && filters.get(id).length() > 0) {
									for (String f : filters.get(id).split(";"))
										if (!f.equalsIgnoreCase("PASS")) distinctFilters.add(f);
								}
							}
						}
						if (distinctFilters.isEmpty()) {
							fw.write("PASS");
						}else {
							array = distinctFilters.toArray(new String[0]);
							for (int i=0 ; i < array.length ; i++) {
								fw.write(array[i]);
								if (i < array.length-1) fw.write(filterDelimiter);
							}
						}
						fw.write(columnDelimiter);
						//INFO
						for (int j=0 ; j < cINFO.size() ; j++) {
							int info = cINFO.get(j);
							fw.write(table.getColumnName(info) + "=");
							array = new String[variantsIds.get(chr).get(pos).get(ref).size()];
							int k=0;
							for (Set<Integer> ids : variantsIds.get(chr).get(pos).get(ref).values()) {
								int id = ids.iterator().next();
								Object o = table.getValueAt(rows.get(id), info);
								if (o == null || o.toString().length() == 0) {
									array[k++] = missing;
								}else {
									array[k++] = o.toString().replace(';', '|');
								}
							}
							for (int i=0 ; i < array.length ; i++) {
								fw.write(array[i]);
								if (i < array.length-1) fw.write(",");
							}
							if (j < cINFO.size()-1) fw.write(infoDelimiter);
						}
						if (cINFO.size() == 0) fw.write(missing); 
						fw.write(columnDelimiter);
						//FORMAT
						fw.write("GT"+formatDelimiter+"AD"+formatDelimiter+"DP");
						for (int format : cFORMAT) {
							fw.write(formatDelimiter+table.getColumnName(format));
						}
						for (String sample : samples.keySet()) {
							fw.write(columnDelimiter);
							//GT:AD:DP
							//Gather all alternatives of current sample with their variant_id
							Map<Integer,String> altVarid = new TreeMap<>(); //  variant_id -> ALT
							Map<String,Integer> altNum = new TreeMap<>(); // ALT -> alt pos
							int l = 1;
							for (String alt : variantsIds.get(chr).get(pos).get(ref).keySet()) {
								altNum.put(alt, l++);
								Set<Integer> sampleId = new HashSet<>(variantsIds.get(chr).get(pos).get(ref).get(alt));
								sampleId.retainAll(samples.get(sample));
								if (!sampleId.isEmpty()) {
									//Happens each time a variant span multiple genes
									//if (sampleId.size() > 1) System.err.println("Problem with format field of sample " + sample + " variant " + chr +":"+pos+", multiple variant ids for same variant");
									altVarid.put(sampleId.iterator().next(), alt);
								}
							}
							//Determine the genotype
							if (altVarid.isEmpty()) {
								//GT
								fw.write("./.");
								fw.write(formatDelimiter);
								//AD
								fw.write(".");
								for (int i=0 ; i < altNum.size() ; i++) {
									fw.write(",.");
								}
								fw.write(formatDelimiter);
								//DP
								fw.write(".");
							}else if (altVarid.size() == 1) {
								int id = altVarid.keySet().iterator().next();
								Zygosity zig = Zygosity.valueOf(gts.get(id));
								switch(zig) {
								case Heterozygous:
									fw.write("0/"+altNum.get(altVarid.get(id)));
									break;
								case Homozygous:
									fw.write(altNum.get(altVarid.get(id))+"/"+altNum.get(altVarid.get(id)));
									break;
								case Reference:
									fw.write("0/0");
									break;
								default:
									fw.write("./.");
									break;
								}
								fw.write(formatDelimiter);
								//AD
								array = new String[altNum.size()+1];
								for (int i=0 ; i< array.length ; i++) array[i] = "0";
								array[0] = adrefs.get(id);
								array[altNum.get(altVarid.get(id))] = adalts.get(id);	
								for (int i=0 ; i < array.length ; i++) {
									fw.write(array[i]);
									if (i < array.length-1) fw.write(",");
								}
								fw.write(formatDelimiter);
								//DP
								fw.write(dps.get(id));
							}else {
								Set<Integer> orderedAlts = new TreeSet<>();
								for (String alt : altVarid.values()) {
									orderedAlts.add(altNum.get(alt));
								}
								String genotype = "";
								for (int idx : orderedAlts){
									genotype += "/" + idx;
								}
								fw.write(genotype.substring(1));
								fw.write(formatDelimiter);
								//AD
								array = new String[altNum.size()+1];
								for (int i=0 ; i< array.length ; i++) array[i] = "0";
								array[0] = adrefs.get(altVarid.keySet().iterator().next());
								for (int id : altVarid.keySet()) {
									array[altNum.get(altVarid.get(id))] = adalts.get(id);
								}
								for (int i=0 ; i < array.length ; i++) {
									fw.write(array[i]);
									if (i < array.length-1) fw.write(",");
								}
								fw.write(formatDelimiter);
								//DP
								fw.write(dps.get(altVarid.keySet().iterator().next()));
							}
							//Other format fields
							for (int j=0 ; j < cFORMAT.size() ; j++) {
								fw.write(formatDelimiter);
								int format = (j < 0) ? j : cFORMAT.get(j);
								array = new String[variantsIds.get(chr).get(pos).get(ref).size()];
								int k=0;
								for (Set<Integer> ids : variantsIds.get(chr).get(pos).get(ref).values()) {
									Set<Integer> sampleId = new HashSet<>(ids);
									sampleId.retainAll(samples.get(sample));
									if (sampleId.isEmpty()) {
										array[k++] = missing;
									}else{
										//Happens each time a variant span multiple genes
										//if (sampleId.size() > 1) System.err.println("Problem with format field of sample " + sample + " variant " + chr +":"+pos+", multiple variant ids for same variant");
										int id = sampleId.iterator().next();
										Object o = table.getValueAt(rows.get(id), format);
										if (o == null || o.toString().length() == 0) {
											array[k++] = missing;
										}else {
											array[k++] = o.toString().replace(':', '|');
										}
									}
								}
								for (int i=0 ; i < array.length ; i++) {
									fw.write(array[i]);
									if (i < array.length-1) fw.write(",");
								}
							}
						}
						fw.write(endline);
					}
				}
			}
			Highlander.waitingPanel.setProgressDone();
		}catch(Exception ex){
			Highlander.waitingPanel.forceStop();
			throw ex;
		}
		Highlander.waitingPanel.stop();

	}

	public void toXlsx(File file) throws Exception {
		toXlsx(file, false);
	}

	public void toXlsx(File file, boolean addNormalReadCount) throws Exception {
		Highlander.waitingPanel.start();
		try{
			int columnCount = table.getColumnCount();
			if (addNormalReadCount) columnCount += 2;
			Workbook wb = new SXSSFWorkbook(100);  		
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			Sheet sheet = wb.createSheet(Highlander.getCurrentAnalysis() + " " + df.format(System.currentTimeMillis()));
			sheet.createFreezePane(0, 1);		
			int r = 0;
			Row row = sheet.createRow(r++);
			row.setHeightInPoints(50);
			VariantsTableModel model = (VariantsTableModel)table.getModel();
			CellStyle cs = sheet.getWorkbook().createCellStyle();
			cs.setWrapText(true);
			int[] alignments = new int[columnCount];
			for (int c = 0 ; c < table.getColumnCount() ; c++){
				Cell cell = row.createCell(c);
				cell.setCellValue(table.getColumnName(c));
				setCellComment(row.getCell(c), model.headers[table.convertColumnIndexToModel(c)].getDescriptionAndSource());
				sheet.setColumnWidth(c, Math.min(model.headers[table.convertColumnIndexToModel(c)].getSize()*32, 250*128));
				alignments[c] = model.getColumnAlignment(c);
				cell.setCellStyle(cs);
			}
			if (addNormalReadCount) {
				int c = table.getColumnCount();
				Cell cell = row.createCell(c);
				cell.setCellValue("Normal sample");
				setCellComment(row.getCell(c), "Normal sample");
				sheet.setColumnWidth(c, 85*32);
				alignments[c] = JLabel.LEFT;
				cell.setCellStyle(cs);
				c++;
				cell = row.createCell(c);
				cell.setCellValue("Normal read count");
				setCellComment(row.getCell(c), "Read count in the normal sample");
				sheet.setColumnWidth(c, 70*32);
				alignments[c] = JLabel.CENTER;
				cell.setCellStyle(cs);
			}
			sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columnCount-1));
			int nrow = table.getRowCount();
			Highlander.waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" variants", false);
			Highlander.waitingPanel.setProgressMaximum(nrow);
			Highlander.getCellRenderer().clearCellStyles();
			for (int i=0 ; i < nrow ; i++ ){
				Highlander.waitingPanel.setProgressValue(r);
				row = sheet.createRow(r++);
				for (int c = 0 ; c < table.getColumnCount() ; c++){
					Cell cell = row.createCell(c);
					if (table.getValueAt(i, c) != null){
						Field field = ((VariantsTableModel)table.getModel()).getColumnField(c);
						Highlander.getCellRenderer().formatXlsCell(table.getValueAt(i, c), field, alignments[c], sheet, cell, i);
						if (table.getColumnClass(c) == Timestamp.class){
							cell.setCellValue((Timestamp)table.getValueAt(i, c));
						}else if (table.getColumnClass(c) == Integer.class){
							cell.setCellValue(Integer.parseInt(table.getValueAt(i, c).toString()));
						}else if (table.getColumnClass(c) == Long.class){
							cell.setCellValue(Long.parseLong(table.getValueAt(i, c).toString()));
						}else if (table.getColumnClass(c) == Double.class){
							cell.setCellValue(Double.parseDouble(table.getValueAt(i, c).toString()));
						}else if (table.getColumnClass(c) == Boolean.class){
							cell.setCellValue(Boolean.parseBoolean(table.getValueAt(i, c).toString()));
						}else {
							cell.setCellValue(table.getValueAt(i, c).toString());
						}
					}
				}
				if (addNormalReadCount) {
					int idxSample = model.getColumnIndex(Field.sample);
					int idxChr = model.getColumnIndex(Field.chr);
					int idxPos = model.getColumnIndex(Field.pos);
					int idxRef = model.getColumnIndex(Field.reference);
					String sample = "";
					String chr = "";
					int pos = -1;
					String ref = "";
					if (idxSample == -1 || idxChr == -1 || idxPos == -1 || idxRef == -1) {
						try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
								"SELECT sample, chr, pos, reference "
								+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
								+ Highlander.getCurrentAnalysis().getJoinProjects()
								+ "WHERE variant_sample_id = " + model.getVariantId(i))) {
							if (res.next()){
								sample = res.getString("sample");
								chr = res.getString("chr");
								pos = res.getInt("pos");
								ref = res.getString("reference");
							}
						}
					}else{
						sample = model.getValueAt(i, idxSample).toString();
						chr = model.getValueAt(i, idxChr).toString();
						pos = Integer.parseInt(model.getValueAt(i, idxPos).toString());
						ref = model.getValueAt(i, idxRef).toString();
					}
					String normal = null;
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
							"SELECT p2.sample "
							+ "FROM projects as p "
							+ "LEFT JOIN projects as p2 ON p.normal_id = p2.project_id "
							+ "JOIN projects_analyses as pa ON p.project_id = pa.project_id "
							+ "WHERE pa.analysis = '"+Highlander.getCurrentAnalysis()+"' AND p.sample = '"+sample+"'")) {
						if (res.next()){
							normal = res.getString("p2.sample");
						}
					}	
					int c = table.getColumnCount();
					Cell cell = row.createCell(c);
					if (normal != null) {
						Highlander.getCellRenderer().formatXlsCell(normal, new Field("Normal sample"), alignments[c], sheet, cell, i);
						cell.setCellValue(normal);
						c++;
						cell = row.createCell(c);
						int val = BamViewer.getReadCount(Highlander.getCurrentAnalysis(), sample, new Interval(Highlander.getCurrentAnalysis().getReference(), chr, pos, pos+ref.length()-1));
						Highlander.getCellRenderer().formatXlsCell(val, new Field("Read count in the normal sample"), alignments[c], sheet, cell, i);
						cell.setCellValue(val);
					}
				}
			}
			Highlander.waitingPanel.setProgressValue(nrow);

			Sheet sheetFilt = wb.createSheet("Filters details");
			r = 0;
			row = sheetFilt.createRow(r++);
			Cell cell = row.createCell(0);

			if (mainFrame.getCurrentFilterName() != null){
				cell = row.createCell(0);
				cell.setCellValue("Filter " + mainFrame.getCurrentFilterName());
				row = sheetFilt.createRow(r++);
			}

			if (mainFrame.getCurrentFilter() != null) {
				cell = row.createCell(0);
				cell.setCellValue(mainFrame.getCurrentFilter().toString());
				if (rowFilterExp != null && rowFilterExp.length() > 0){
					row = sheetFilt.createRow(r++);
					row = sheetFilt.createRow(r++);
					cell = row.createCell(0);
					cell.setCellValue("Search criterion applied:");
					row = sheetFilt.createRow(r++);
					cell = row.createCell(0);
					cell.setCellValue(rowFilterExp);
				}
			}

			row = sheetFilt.createRow(r++);
			row = sheetFilt.createRow(r++);
			cell = row.createCell(0);
			cell.setCellValue("Analysis");
			cell = row.createCell(1);
			cell.setCellValue(""+Highlander.getCurrentAnalysis());

			row = sheetFilt.createRow(r++);
			row = sheetFilt.createRow(r++);
			cell = row.createCell(0);

			cell.setCellValue("Generated with Highlander version " + Highlander.version + " by " + Highlander.getLoggedUser() + " ("+df.format(System.currentTimeMillis())+")");

			if (mainFrame.getCurrentFilter() != null) {
				final CreationHelper helper = wb.getCreationHelper();
				final Drawing drawing = sheetFilt.createDrawingPatriarch();
				final ClientAnchor anchor = helper.createClientAnchor();
				anchor.setAnchorType( ClientAnchor.MOVE_AND_RESIZE );
				BufferedImage filterImage = FilteringTree.getFilterImage(mainFrame.getCurrentFilter());
				byte[] imageInByte;
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
					ImageIO.write( filterImage, "png", baos );
					baos.flush();
					imageInByte = baos.toByteArray();
				}
				final int pictureIndex = wb.addPicture( imageInByte, Workbook.PICTURE_TYPE_PNG );
				r++;
				anchor.setCol1( 0 );
				anchor.setRow1( r++ ); // same row is okay
				anchor.setRow2( r++ );
				anchor.setCol2( 1 );
				final Picture pict = drawing.createPicture( anchor, pictureIndex );
				pict.resize();
			}
			
			Highlander.waitingPanel.setProgressString("Writing file ...",true);		
			try (FileOutputStream fileOut = new FileOutputStream(file)){
				wb.write(fileOut);
			}
			Highlander.waitingPanel.setProgressDone();
		}catch(Exception ex){
			Highlander.waitingPanel.forceStop();
			throw ex;
		}
		Highlander.waitingPanel.stop();
	}

	private static void setCellComment(Cell cell, String cellComment){
		CreationHelper factory = cell.getSheet().getWorkbook().getCreationHelper();
		Drawing drawing = cell.getSheet().createDrawingPatriarch();
		ClientAnchor anchor = factory.createClientAnchor();
		anchor.setCol1(cell.getColumnIndex());
		anchor.setCol2(cell.getColumnIndex()+6);
		anchor.setRow1(cell.getRowIndex());
		anchor.setRow2(cell.getRowIndex()+1);
		Comment comment = drawing.createCellComment(anchor);
		RichTextString str = factory.createRichTextString(cellComment);
		comment.setString(str);
		cell.setCellComment(comment);
	}

	private void runMemoryBarUpdater(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true){
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							memoryBar.setValue(Tools.getUsedMemoryInMb());
							memoryBar.setString(Tools.doubleToString(((double)Tools.getUsedMemoryInMb() / 1024.0), 1, false) + " Gb / " 
									+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024) / 1024.0), 1, false)) + " Gb");
							memoryBar.setStringPainted(true);				     
						}
					});
					try{
						Thread.sleep(500);
					}catch (InterruptedException ex){
						Tools.exception(ex);
					}
				}
			}
		}, "VariantsTable.runMemoryBarUpdater").start();
	}
}
