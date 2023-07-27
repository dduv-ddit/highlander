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

package be.uclouvain.ngs.highlander.administration.UI.projects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.administration.UI.Sample;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.SampleType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public class SamplesPanel extends ManagerPanel {

	private SamplesTableModel samplesTableModel;
	private JTable samplesTable;
	private TableRowSorter<SamplesTableModel> sorterIci;
	private SearchField	searchField = new SearchField(10){
		@Override
		public void applyFilter(){
			RowFilter<SamplesTableModel, Object> rf = null;
	    //If current expression doesn't parse, don't update.
	    try {
	        rf = RowFilter.regexFilter("(?i)"+getText());
	    } catch (java.util.regex.PatternSyntaxException e) {
	        return;
	    }
	    sorterIci.setRowFilter(rf);   
		}
	};
	
	public SamplesPanel(ProjectManager manager){
		super(manager);

		JPanel northPanel = new JPanel(new BorderLayout());
		add(northPanel, BorderLayout.NORTH);

		JPanel panel_filters = new JPanel(new BorderLayout());
		northPanel.add(panel_filters, BorderLayout.NORTH);
		panel_filters.add(searchField, BorderLayout.CENTER);

		JPanel fillToolsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT));
		northPanel.add(fillToolsPanel, BorderLayout.SOUTH);

		JButton setIndexCaseButton = new JButton("Set index case");
		setIndexCaseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set if selected samples are from an index case ('false' by default)", "Index case",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), new String[]{"true","false"}, null);
				if (res != null){
					String item = res.toString();
					for (int row : samplesTable.getSelectedRows()){
						samplesTable.setValueAt(item, row, samplesTable.convertColumnIndexToView(samplesTableModel.getColumn("index_case")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setIndexCaseButton);

		JButton setPathologyButton = new JButton("Set pathology");
		setPathologyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the pathology of selected samples (mandatory)", "Pathology",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listPathologies(), null);
				if (res != null){
					String item = res.toString();
					for (int row : samplesTable.getSelectedRows()){
						samplesTable.setValueAt(item, row, samplesTable.convertColumnIndexToView(samplesTableModel.getColumn("pathology")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setPathologyButton);

		JButton setPopulationButton = new JButton("Set population");
		setPopulationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the population to which individuals of selected samples belong to", "Population",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listPopulations(), null);
				if (res != null){
					String item = res.toString();
					for (int row : samplesTable.getSelectedRows()){
						samplesTable.setValueAt(item, row, samplesTable.convertColumnIndexToView(samplesTableModel.getColumn("population")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setPopulationButton);
		
		JButton setTypeButton = new JButton("Set sample type");
		setTypeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the type of selected samples (mandatory)", "Sample type",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), SampleType.values(), null);
				if (res != null){
					String item = res.toString();
					for (int row : samplesTable.getSelectedRows()){
						samplesTable.setValueAt(item, row, samplesTable.convertColumnIndexToView(samplesTableModel.getColumn("sample_type")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setTypeButton);
		
		JButton setNormalButton = new JButton("Set normal sample");
		setNormalButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AskNormalSampleDialog ask = new AskNormalSampleDialog();
				Tools.centerWindow(ask, false);
				ask.setVisible(true);
				if (ask.isSampleSelected()){
					int id = ask.getSelectedId();
					String sample = ask.getSelectedSample();
					for (int row : samplesTable.getSelectedRows()){
						samplesTable.setValueAt(id, row, samplesTable.convertColumnIndexToView(samplesTableModel.getColumn("normal_id")));
						samplesTable.setValueAt(sample, row, samplesTable.convertColumnIndexToView(samplesTableModel.getColumn("normal_sample")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setNormalButton);

		samplesTable = new JTable(){
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
		samplesTable.setRowSelectionAllowed(true);
		samplesTable.setCellSelectionEnabled(true);
		samplesTable.setColumnSelectionAllowed(false);
		samplesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		samplesTable.createDefaultColumnsFromModel();
		samplesTable.getTableHeader().setReorderingAllowed(false);
		samplesTable.getTableHeader().setResizingAllowed(true);
		samplesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		new ExcelAdapter(samplesTable);		
		samplesTable.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent arg0) {
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER ||
						arg0.getKeyCode() == KeyEvent.VK_LEFT ||
						arg0.getKeyCode() == KeyEvent.VK_RIGHT ||
						arg0.getKeyCode() == KeyEvent.VK_UP ||
						arg0.getKeyCode() == KeyEvent.VK_DOWN){
					refresh();
				}				
			}
		});
		JScrollPane scroll = new JScrollPane(samplesTable);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		add(scroll, BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new FlowLayout());
		add(southPanel, BorderLayout.SOUTH);

		JButton excelButton = new JButton("Export table to Excel", Resources.getScaledIcon(Resources.iExcel, 16));
		excelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						toXlsx();
					}
				}, "ProjectsPanel.toXlsx").start();

			}
		});
		southPanel.add(excelButton);

		new Thread(new Runnable() {				
			@Override
			public void run() {
				fill();
				sorterIci = new TableRowSorter<SamplesTableModel>(samplesTableModel);
				samplesTable.setRowSorter(sorterIci);
				searchField.applyFilter();		
			}
		}, "SamplesPanel.fill").start();
	}

	private void refresh(){
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				try{
					samplesTableModel.fireTableRowsUpdated(0,samplesTableModel.getRowCount()-1);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		});	
	}

	public void fill(){
		try{
			String[] headers = new String[]{
					"project_id", //Must stay at index 0
					"family",
					"individual",
					"sample",
					"index_case",
					"pathology",
					"population",
					"sample_type",
					"normal_id",
					"normal_sample",
					"comments",
					"run_label",
					"analyses",								
			};
			Object[][] data;
				List<Object[]> arrayList = new ArrayList<Object[]>();
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT GROUP_CONCAT(DISTINCT a.analysis) as analyses, p.*"+
						", pathology, population" + 
						", p2.sample as normal_sample "+
						"FROM projects as p " +
						"JOIN pathologies USING (pathology_id) " +
						"LEFT JOIN populations USING (population_id) " +
						"LEFT JOIN projects_analyses as a USING (project_id) " +
						"LEFT JOIN projects as p2 ON p.normal_id = p2.project_id " +
						"GROUP BY p.project_id")){
					while(res.next()){
						Object[] array = new Object[headers.length];
						for (int col=0 ; col < headers.length ; col++){
							array[col] = res.getObject(headers[col]);
						}
						arrayList.add(array);
					}
				}
				data = new Object[arrayList.size()][headers.length];
				int row = 0;
				for (Object[] array : arrayList){
					data[row] = array;
					row++;
				}
			samplesTableModel = new SamplesTableModel(data, headers);
			samplesTable.setModel(samplesTableModel);
			for (int i=0 ; i < samplesTable.getColumnCount() ; i++){
				int width = 0;
				for (int r = 0; r < samplesTable.getRowCount(); r++) {
					TableCellRenderer renderer = samplesTable.getCellRenderer(r, i);
					Component comp = samplesTable.prepareRenderer(renderer, r, i);
					width = Math.max (comp.getPreferredSize().width, width);
				}
				samplesTable.getColumnModel().getColumn(i).setPreferredWidth(width+20);
			}
			refresh();
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	public class ExcelAdapter implements ActionListener {
		private String rowstring,value;
		private Clipboard system;
		private JTable table ;
		/**
		 * The Excel Adapter is constructed with a
		 * JTable on which it enables Copy-Paste and acts
		 * as a Clipboard listener.
		 */
		public ExcelAdapter(JTable myJTable){
			table = myJTable;
			KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(),false);
			table.registerKeyboardAction(this,"Paste",paste,JComponent.WHEN_FOCUSED);
			KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0,false);
			table.registerKeyboardAction(this,"Delete",delete,JComponent.WHEN_FOCUSED);
			system = Toolkit.getDefaultToolkit().getSystemClipboard();
		}
		/**
		 * Public Accessor methods for the Table on which this adapter acts.
		 */
		public JTable getJTable() {return table;}
		public void setJTable(JTable jTable1) {this.table=jTable1;}
		/**
		 * This method is activated on the Keystrokes we are listening to
		 * in this implementation. Here it listens for Copy and Paste ActionCommands.
		 * Selections comprising non-adjacent cells result in invalid selection and
		 * then copy action cannot be performed.
		 * Paste is done by aligning the upper left corner of the selection with the
		 * 1st element in the current selection of the JTable.
		 */
		@Override
		public void actionPerformed(ActionEvent e){
			if (e.getActionCommand().compareTo("Paste")==0){
				int startRow=(table.getSelectedRows())[0];
				int startCol=(table.getSelectedColumns())[0];
				try	{
					String trstring= ((String)(system.getContents(this).getTransferData(DataFlavor.stringFlavor))).replace("\r", "\n");
					String[] st1= trstring.split("\n");
					for(int i=0; i < st1.length ;i++)	{
						rowstring=st1[i];
						String[] st2= rowstring.split("\t");
						for(int j=0; j < st2.length ;j++)	{
							value= st2[j];
							if (startRow+i< table.getRowCount()  &&
									startCol+j< table.getColumnCount())
								if (table.isCellEditable(startRow+i,startCol+j)) table.setValueAt(value,startRow+i,startCol+j);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				refresh();
			}else if (e.getActionCommand().compareTo("Delete")==0){
				try	{
					for (int row : table.getSelectedRows()){
						for (int col : table.getSelectedColumns()){
							if (col != ((SamplesTableModel)(table.getModel())).getColumn("project_id") &&
									col != ((SamplesTableModel)(table.getModel())).getColumn("run_label") &&
									col != ((SamplesTableModel)(table.getModel())).getColumn("analyses"))
								table.setValueAt(null,row,col);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				refresh();
			}
		}
	}

	public void toXlsx(){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		Tools.centerWindow(chooser, false);
		chooser.setVisible(true) ;
		if (chooser.getFile() != null) {
			String filename = chooser.getDirectory() + chooser.getFile();
			if (!filename.endsWith(".xlsx")) filename += ".xlsx";
			File xls = new File(filename);
			try{
				waitingPanel.start();
				try{
					try(Workbook wb = new SXSSFWorkbook(100)){ 
						int totalRows = 0;					
						JTable table = samplesTable;
						Sheet sheet = wb.createSheet("Highlander samples");
						sheet.createFreezePane(0, 1);		
						int r = 0;
						Row row = sheet.createRow(r++);
						row.setHeightInPoints(50);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							row.createCell(c).setCellValue(table.getColumnName(c));
						}
						sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
						int nrow = table.getRowCount();
						waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" lines", false);
						waitingPanel.setProgressMaximum(nrow);
						for (int i=0 ; i < nrow ; i++ ){
							waitingPanel.setProgressValue(r);
							row = sheet.createRow(r++);
							for (int c = 0 ; c < table.getColumnCount() ; c++){
								if (table.getValueAt(i, c) == null)
									row.createCell(c);
								else if (table.getColumnClass(c) == OffsetDateTime.class)
									row.createCell(c).setCellValue(((OffsetDateTime)table.getValueAt(i, c)).toLocalDateTime());
								else if (table.getColumnClass(c) == Integer.class)
									row.createCell(c).setCellValue(Integer.parseInt(table.getValueAt(i, c).toString()));
								else if (table.getColumnClass(c) == Long.class)
									row.createCell(c).setCellValue(Long.parseLong(table.getValueAt(i, c).toString()));
								else if (table.getColumnClass(c) == Double.class)
									row.createCell(c).setCellValue(Double.parseDouble(table.getValueAt(i, c).toString()));
								else if (table.getColumnClass(c) == Boolean.class)
									row.createCell(c).setCellValue(Boolean.parseBoolean(table.getValueAt(i, c).toString()));
								else 
									row.createCell(c).setCellValue(table.getValueAt(i, c).toString());
							}
						}		
						totalRows += nrow;
						waitingPanel.setProgressValue(totalRows);
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

	public class SamplesTableModel	extends AbstractTableModel {
		private Object[][] data;
		private String[] headers;

		public SamplesTableModel(Object[][] data, String[] headers) {    	
			this.data = data;
			this.headers = headers;
		}

		@Override
		public int getColumnCount() {
			return headers.length;
		}

		@Override
		public String getColumnName(int col) {
			return headers[col];
		}

		public int getColumn(String header){
			for (int i = 0 ; i < headers.length ; i++){
				if (headers[i].equals(header)){
					return i;
				}
			}
			return -1;
		}

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			try {
				String colName = getColumnName(col);
				int project_id = Integer.parseInt(data[row][0].toString());
				switch(colName) {
				case "family":
					if (value == null) {
						JOptionPane.showMessageDialog(new JFrame(), "Family is mandatory and cannot be deleted.", "Validate sample", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64));
					}else {
						DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
								" `family` = '"+DB.format(Schema.HIGHLANDER, value.toString().trim())+"'" +
								" WHERE project_id = " + project_id);
						data[row][col] = value;
					}
					break;
				case "individual":
					if (value == null) {
						JOptionPane.showMessageDialog(new JFrame(), "Individual is mandatory and cannot be deleted.", "Validate sample", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64));
					}else {
						DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
								" `individual` = '"+DB.format(Schema.HIGHLANDER, value.toString().trim())+"'" +
								" WHERE project_id = " + project_id);
						data[row][col] = value;
					}
					break;
				case "sample":
					if (value == null) {
						JOptionPane.showMessageDialog(new JFrame(), "Sample is mandatory and cannot be deleted.", "Validate sample", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64));
					}else {
						String oldName = data[row][col].toString();
						if (!oldName.equals(value.toString().trim())) {
							int res = JOptionPane.showConfirmDialog(SamplesPanel.this, "Are you sure you want to rename sample " + oldName +" to " + value.toString().trim() + " ? \nFiles and directories on server will be renamed accordingly !", "Rename sample", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iQuestion,64));
							if (res == JOptionPane.YES_OPTION){
								DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
										" `sample` = '"+DB.format(Schema.HIGHLANDER, value.toString().trim())+"'" +
										" WHERE project_id = " + project_id);
								data[row][col] = value;
								if (data[row][getColumn("analyses")] != null) {
									String[] analyses = data[row][getColumn("analyses")].toString().split(",");
									for (String analysis : analyses) {
										DB.update(Schema.HIGHLANDER, "UPDATE `"+analysis+"_possible_values` SET `value` = '"+DB.format(Schema.HIGHLANDER, value.toString().trim())+"' WHERE `field` = 'sample' AND `value` = '"+oldName+"'");
									}
									Sample.renameOnServer(manager, project_id, oldName, analyses);
								}
							}
						}
					}
					break;
				case "index_case":
					if (value == null) {
						JOptionPane.showMessageDialog(new JFrame(), "Index case is mandatory and cannot be deleted.", "Validate sample", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64));
					}else {
						DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
								" `index_case` = "+Boolean.parseBoolean(value.toString().trim())+
								" WHERE project_id = " + project_id);
						data[row][col] = value;
					}
					break;
				case "pathology":
					if (value == null) {
						JOptionPane.showMessageDialog(new JFrame(), "Pathology is mandatory and cannot be deleted.", "Validate sample", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64));
					}else {
						int pathology_id = -1;
						try (Results res = DB.select(Schema.HIGHLANDER, "SELECT pathology_id FROM pathologies WHERE pathology = '"+value.toString().trim()+"'")){
							if (res.next()) {
								pathology_id = res.getInt("pathology_id");
							}else {
								pathology_id = -1;
							}
						}catch(Exception ex){
							pathology_id = -1;
						}
						if (pathology_id != -1) {
							DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
									" `pathology_id` = "+pathology_id+
									" WHERE project_id = " + project_id);
							data[row][col] = value;
						}
					}
					break;
				case "population":
					int population_id = -1;
					if (value != null) {
						try (Results res = DB.select(Schema.HIGHLANDER, "SELECT population_id FROM populations WHERE population = '"+value.toString().trim()+"'")){
							if (res.next()) {
								population_id = res.getInt("population_id");
							}else {
								population_id = -1;
							}
						}catch(Exception ex){
							population_id = -1;
						}					
					}
					DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
							" `population_id` = "+((population_id != -1)?population_id:"NULL") +
							" WHERE project_id = " + project_id);
					data[row][col] = value;
					break;
				case "sample_type":
					if (value == null) {
						JOptionPane.showMessageDialog(new JFrame(), "Sample type is mandatory and cannot be deleted.", "Validate sample", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64));
					}else {
						DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
								" `sample_type` = '"+value.toString().trim()+"'" +
								" WHERE project_id = " + project_id);
						data[row][col] = value;
					}
					break;
				case "normal_id":
					int normal_id = (value == null) ? -1 : Integer.parseInt(value.toString().trim());
					DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
							" `normal_id` = "+((normal_id != -1)?normal_id:"NULL") +
							" WHERE project_id = " + project_id);
					data[row][col] = value;
					break;
				case "comments":
					DB.update(Schema.HIGHLANDER, "UPDATE projects SET" +
							" `comments` = '"+((value == null)?"":DB.format(Schema.HIGHLANDER, value.toString()))+"'" +
							" WHERE project_id = " + project_id);
					data[row][col] = value;
					break;
				case "project_id":
				case "normal_sample":
				case "run_label":
				case "analyses":								
				default:
					data[row][col] = value;
					break;
				}
			}catch(Exception ex) {
				ProjectManager.toConsole(ex);
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			boolean edit = false;
			if (columnIndex == getColumn("family")) edit = true;
			if (columnIndex == getColumn("individual")) edit = true;
			if (columnIndex == getColumn("sample")) edit = true;
			if (columnIndex == getColumn("comments")) edit = true;
			return edit;
		}

	}

}
