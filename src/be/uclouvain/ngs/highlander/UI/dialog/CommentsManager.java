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

package be.uclouvain.ngs.highlander.UI.dialog;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.table.MultiLineTableCellRenderer;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Variant;

/**
* @author Raphael Helaers
*/

public class CommentsManager extends JFrame {

	public enum TargetField {Genes, Samples, Variants}
	
	private CommentsTableModel commentsTableModel;
	private JTable commentsTable;
	private JComboBox<AnalysisFull> boxAnalyses;
	private JComboBox<TargetField> boxCategories;

	static private WaitingPanel waitingPanel;
	
	private Map<Integer, Integer> publicIds = new TreeMap<>();
	private Map<Integer, Integer> privateIds = new TreeMap<>();

	private Map<Integer, Variant> variants = new HashMap<>();
	private Map<Integer, String> genes = new HashMap<>();
	
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
	public CommentsManager() {
		super();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						fill();
					}
				}, "CommentsManager.shown").start();
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
		setTitle("Comments manager");
		setIconImage(Resources.getScaledIcon(Resources.iComments, 64).getImage());

		JPanel panel_north = new JPanel(new GridLayout(1, 2));
		getContentPane().add(panel_north, BorderLayout.NORTH);
		
		boxAnalyses = new JComboBox<AnalysisFull>(Highlander.getAvailableAnalyses().toArray(new AnalysisFull[0]));
		boxAnalyses.setToolTipText("Analysis");
		boxAnalyses.setSelectedItem(Highlander.getCurrentAnalysis());
		boxAnalyses.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED){
					fill();
				}
			}
		});
		panel_north.add(boxAnalyses);

		boxCategories = new JComboBox<TargetField>(TargetField.values());
		boxCategories.setToolTipText("Annotation target");
		boxCategories.setSelectedIndex(0);
		boxCategories.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED){
					fill();
				}
			}
		});
		panel_north.add(boxCategories);
		
		commentsTable = new JTable(commentsTableModel){
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
		};
		commentsTable.setDefaultRenderer(Object.class, new ColoredTableCellRenderer());
		commentsTable.setDefaultRenderer(String.class, new ColoredTableCellRenderer());
		commentsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		commentsTable.setCellSelectionEnabled(false);
		commentsTable.setRowSelectionAllowed(true);
		commentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroll = new JScrollPane(commentsTable);
		getContentPane().add(scroll, BorderLayout.CENTER);
		
		JPanel panel_south = new JPanel(new FlowLayout());
		getContentPane().add(panel_south, BorderLayout.SOUTH);
		
		JButton btnAdd = new JButton("Add element", Resources.getScaledIcon(Resources.i3dPlus, 24));
		btnAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				addElementsDialog();
			}
		});
		panel_south.add(btnAdd);

		JButton btnModifyPrivate = new JButton("Modify private comment", Resources.getScaledIcon(Resources.iUpdater, 24));
		btnModifyPrivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				modifyPrivate();
			}
		});
		panel_south.add(btnModifyPrivate);
		
		JButton btnModifyPublic = new JButton("Modify public comment", Resources.getScaledIcon(Resources.iUpdater, 24));
		btnModifyPublic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				modifyPublic();
			}
		});
		panel_south.add(btnModifyPublic);
		
		JButton btnExport = new JButton("Export", Resources.getScaledIcon(Resources.iExcel, 24));
		btnExport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				export();
			}
		});
		panel_south.add(btnExport);
		
		JButton btnOk = new JButton("close", Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				dispose();
			}
		});
		panel_south.add(btnOk);
		
		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	private void fill() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			AnalysisFull analysis = (AnalysisFull)boxAnalyses.getSelectedItem();
			TargetField selection = (TargetField)boxCategories.getSelectedItem();
			String[] headers = new String[] {selection.toString(), "Private comments", "Public comments", "Last modification (public)"};
			String fields = "";
			String table = "";
			String order = "";
			switch(selection) {
			case Genes:
				fields = "gene_symbol, gene_comments as comments, username";
				table = analysis.getFromUserAnnotationsGenes();
				order = "gene_symbol";
				break;
			case Samples:
				fields = "sample, sample_comments as comments, username";
				table = analysis.getFromUserAnnotationsSamples() + analysis.getJoinProjects();
				order = "sample";
				break;
			case Variants:
				fields = "chr, pos, length, reference, alternative, gene_symbol, variant_comments as comments, username";
				table = analysis.getFromUserAnnotationsVariants();
				order = "gene_symbol";
				break;
			}
			Map<String, Object[]> elements = new TreeMap<>(new Tools.NaturalOrderComparator(true));
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT id, " + fields + " "
					+ "FROM " + table
					+ "WHERE `username` = '"+Highlander.getLoggedUser().getUsername()+"' OR `username` = 'PUBLIC' "
					+ "ORDER BY " + order
					)) {
				while(res.next()){
					boolean publicComment = res.getString("username").equalsIgnoreCase("PUBLIC");
					int id = res.getInt("id");
					String txt = (res.getString("comments") != null) ? res.getString("comments") : "";
					String comment = txt;
					String lastModif = "";
					int i = txt.indexOf("\nLast modified by "); 
					if (i >= 0){
						comment = txt.substring(0, i);
						int j = txt.indexOf(".", i);
						if (j >= 0 && j+1 < txt.length()){
							comment += txt.substring(j+1);
						}
						lastModif = txt.substring(i+"\nLast modified by ".length());
					}
					switch(selection) {
					case Genes:
						String gene_symbol = res.getString("gene_symbol");
						if (!elements.containsKey(gene_symbol)) {
							elements.put(gene_symbol, new Object[6]);
						}
						elements.get(gene_symbol)[0] = gene_symbol;
						elements.get(gene_symbol)[publicComment?2:1] = comment;
						elements.get(gene_symbol)[3] = lastModif;
						elements.get(gene_symbol)[publicComment?4:5] = id;
						break;
					case Samples:
						String sample = res.getString("sample");
						if (!elements.containsKey(sample)) {
							elements.put(sample, new Object[6]);
						}
						elements.get(sample)[0] = sample;
						elements.get(sample)[publicComment?2:1] = comment;
						elements.get(sample)[3] = lastModif;
						elements.get(sample)[publicComment?4:5] = id;
						break;
					case Variants:
						String chr = res.getString("chr");
						int pos = res.getInt("pos");
						int length = res.getInt("length");
						String reference = res.getString("reference");
						String alternative = res.getString("alternative");
						String gene = res.getString("gene_symbol");
						Variant variant = new Variant(chr, pos, length, reference, alternative);
						String variantTxt = variant.getVariantType() + " at " + variant.getChromosome() + ":" + variant.getPosition() + " - " + variant.getReference() + " > " + variant.getAlternative() + " (length " + variant.getLength() + ")";
						if (gene.length() > 0) variantTxt += " for gene " + gene;
						if (!elements.containsKey(variantTxt)) {
							elements.put(variantTxt, new Object[6]);
						}
						elements.get(variantTxt)[0] = variantTxt;
						elements.get(variantTxt)[publicComment?2:1] = comment;
						elements.get(variantTxt)[3] = lastModif;
						elements.get(variantTxt)[publicComment?4:5] = id;
						variants.put(id, variant);
						genes.put(id, gene);
						break;
					}
				}
			}		
			Object[][] data = new Object[elements.size()][headers.length];
			int row = 0;
			for (Object[] array : elements.values()){
				data[row][0] = array[0];
				data[row][1] = array[1];
				data[row][2] = array[2];
				data[row][3] = array[3];
				if (array[4] != null) publicIds.put(row, (Integer)array[4]);
				if (array[5] != null) privateIds.put(row, (Integer)array[5]);
				row++;
			}
			commentsTableModel = new CommentsTableModel(data, headers);
			commentsTable.setModel(commentsTableModel);
		}catch(Exception ex){
			Tools.exception(ex);
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	public void addElementsDialog() {
		TargetField selection = (TargetField)boxCategories.getSelectedItem();
		try {
			switch(selection) {
			case Genes:
				AskListOfPossibleValuesDialog askGene = new AskListOfPossibleValuesDialog(Field.gene_symbol, null, false);
				Tools.centerWindow(askGene, false);
				askGene.setVisible(true);
				if (!askGene.getSelection().isEmpty()){
					for(String gene : askGene.getSelection()) {
						try {
							addElement(gene);
						} catch (Exception ex) {
							Tools.exception(ex);
							JOptionPane.showMessageDialog(this, Tools.getMessage("Cannot add element" + gene, ex), "Add elements", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				}
				fill();
				break;
			case Samples:
				AskListOfPossibleValuesDialog askSample = new AskListOfPossibleValuesDialog(Field.sample, null, false);
				Tools.centerWindow(askSample, false);
				askSample.setVisible(true);
				if (!askSample.getSelection().isEmpty()){
					for(String sample : askSample.getSelection()) {
						try {
							addElement(sample);
						} catch (Exception ex) {
							Tools.exception(ex);
							JOptionPane.showMessageDialog(this, Tools.getMessage("Cannot add element" + sample, ex), "Add elements", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				}
				fill();
				break;
			case Variants:
				JOptionPane.showMessageDialog(this, "Variants cannot be added with this tool.", "Add elements", JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iComments,64));				
				break;
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Error retrieving selected database field", ex), "Add elements", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void addElement(String element) throws Exception {
		AnalysisFull analysis = (AnalysisFull)boxAnalyses.getSelectedItem();
		TargetField selection = (TargetField)boxCategories.getSelectedItem();
		boolean privateAlreadyInDB = false;
		boolean publicAlreadyInDB = false;
		int foundInRow = -1;
		for (int row = 0 ; row < commentsTable.getRowCount() ; row++) {
			if (commentsTable.getValueAt(row, 0).equals(element)) {
				foundInRow = row;
				if (publicIds.containsKey(row) && publicIds.get(row) != null) publicAlreadyInDB = true;
				if (privateIds.containsKey(row) && privateIds.get(row) != null) privateAlreadyInDB = true;
				break;
			}
		}
		if (!privateAlreadyInDB) {
			int id = -1;
			switch(selection) {
			case Genes:
				id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
						"INSERT INTO " + Highlander.getCurrentAnalysis().getFromUserAnnotationsGenes()
						+ "(`gene_symbol`, `username`, `gene_comments`) " +
						"VALUES ('"+element+"', '"+Highlander.getLoggedUser().getUsername()+"', '')");
				break;
			case Samples:
				int project_id = -1;
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT project_id "
								+ "FROM projects JOIN projects_analyses USING (project_id)"
								+ "WHERE analysis = '"+analysis+"' AND sample = '"+element+"'"
						)) {
					if(res.next()){
						project_id = res.getInt("project_id");
					}
				}
				if (project_id != -1) {
					id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT INTO " + Highlander.getCurrentAnalysis().getFromUserAnnotationsSamples()
							+ "(`project_id`, `username`, `sample_comments`) " +
							"VALUES ("+project_id+", '"+Highlander.getLoggedUser().getUsername()+"', '')");
				}else {
					throw new Exception("project_id not found for sample " + element + " in analysis " + analysis);
				}
				break;
			case Variants:
				if (foundInRow != -1) {
					Variant variant = null;
					String gene = null;
					variant = variants.get(publicIds.get(foundInRow));
					gene = genes.get(publicIds.get(foundInRow));
					id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT INTO " + Highlander.getCurrentAnalysis().getFromUserAnnotationsVariants()
							+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_comments`) " +
							"VALUES ('"+variant.getChromosome()+"', "+variant.getPosition()+", "+variant.getLength()+", '"+variant.getReference()+"','"+variant.getAlternative()+"', '"+gene+"', '"+Highlander.getLoggedUser().getUsername()+"', '')");
				}else {
					throw new Exception("variant not found for variant " + element);
				}
				break;
			}	
			if (id != -1 && foundInRow != -1) {
				privateIds.put(foundInRow, id);
			}
		}
		if (!publicAlreadyInDB) {
			int id = -1;
			switch(selection) {
			case Genes:
				id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
						"INSERT INTO " + analysis.getFromUserAnnotationsGenes()
						+ "(`gene_symbol`, `username`, `gene_comments`) " +
						"VALUES ('"+element+"', 'PUBLIC', '')");
				break;
			case Samples:
				int project_id = -1;
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT project_id "
								+ "FROM projects JOIN projects_analyses USING (project_id)"
								+ "WHERE analysis = '"+analysis+"' AND sample = '"+element+"'"
						)) {
					if(res.next()){
						project_id = res.getInt("project_id");
					}
				}
				if (project_id != -1) {
					id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT INTO " + analysis.getFromUserAnnotationsSamples()
							+ "(`project_id`, `username`, `sample_comments`) " +
							"VALUES ("+project_id+", 'PUBLIC', '')");
				}else {
					throw new Exception("project_id not found for sample " + element + " in analysis " + analysis);
				}
				break;
			case Variants:
				if (foundInRow != -1) {
					Variant variant = null;
					String gene = null;
					variant = variants.get(privateIds.get(foundInRow));
					gene = genes.get(privateIds.get(foundInRow));
					id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT INTO " + analysis.getFromUserAnnotationsVariants()
							+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_comments`) " +
							"VALUES ('"+variant.getChromosome()+"', "+variant.getPosition()+", "+variant.getLength()+", '"+variant.getReference()+"','"+variant.getAlternative()+"', '"+gene+"', 'PUBLIC', '')");
				}else {
					throw new Exception("variant not found for variant " + element);
				}
				break;
			}
			if (id != -1 && foundInRow != -1) {
				publicIds.put(foundInRow, id);
			}

		}
	}
	
	public void modifyPrivate() {
		TargetField selection = (TargetField)boxCategories.getSelectedItem();
		int row = commentsTable.getSelectedRow();
		String target = (commentsTable.getValueAt(row, 0) != null) ? commentsTable.getValueAt(row, 0).toString() : "";
		String comment = (commentsTable.getValueAt(row, 1) != null) ? commentsTable.getValueAt(row, 1).toString() : "";
		JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setColumns(50);
		textArea.setRows(3);
		textArea.setText(comment);
		switch (JOptionPane.showConfirmDialog(this, new JScrollPane(textArea), "Modify private comment for " + target, 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iComments, 64))) {
		    case JOptionPane.OK_OPTION:
					try{
						String newTxt = textArea.getText();
						AnalysisFull analysis = (AnalysisFull)boxAnalyses.getSelectedItem();
						if (!publicIds.containsKey(row) || privateIds.get(row) == null) {
							addElement(target);
						}
						int variantAnnotationId = privateIds.get(row);
						String table = "";
						Field field = null;
						switch(selection) {
						case Genes:
							table = analysis.getFromUserAnnotationsGenesPrivate();
							field = Field.gene_comments_private;
							break;
						case Samples:
							table = analysis.getFromUserAnnotationsSamplesPrivate();
							field = Field.sample_comments_private;
							break;
						case Variants:
							table = analysis.getFromUserAnnotationsVariantsPrivate();
							field = Field.variant_comments_private;
							break;
						}
						Highlander.getDB().update(Schema.HIGHLANDER, 
									"UPDATE " + table
									+ "SET "+field.getQueryWhereName(analysis, false)+" = '"+ Highlander.getDB().format(Schema.HIGHLANDER, newTxt) + "' "
									+ "WHERE id = " + variantAnnotationId);		
						commentsTable.setValueAt(newTxt, row, 1);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
		      break;
		}
	}

	public void modifyPublic() {
		TargetField selection = (TargetField)boxCategories.getSelectedItem();
		int row = commentsTable.getSelectedRow();
		String target = (commentsTable.getValueAt(row, 0) != null) ? commentsTable.getValueAt(row, 0).toString() : "";
		String comment = (commentsTable.getValueAt(row, 2) != null) ? commentsTable.getValueAt(row, 2).toString() : "";
		JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setColumns(50);
		textArea.setRows(3);
		textArea.setText(comment);
		switch (JOptionPane.showConfirmDialog(this, new JScrollPane(textArea), "Modify public comment for " + target, 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iComments, 64))) {
		    case JOptionPane.OK_OPTION:
					try{
						String newTxt = textArea.getText();
						String modif = Highlander.getLoggedUser().getUsername() + " on "+df.format(System.currentTimeMillis())+"."; 
						AnalysisFull analysis = (AnalysisFull)boxAnalyses.getSelectedItem();
						if (!publicIds.containsKey(row) || publicIds.get(row) == null) {
							addElement(target);
						}
						int variantAnnotationId = publicIds.get(row);
						String table = "";
						Field field = null;
						switch(selection) {
						case Genes:
							table = analysis.getFromUserAnnotationsGenesPublic();
							field = Field.gene_comments_public;
							break;
						case Samples:
							table = analysis.getFromUserAnnotationsSamplesPublic();
							field = Field.sample_comments_public;
							break;
						case Variants:
							table = analysis.getFromUserAnnotationsVariantsPublic();
							field = Field.variant_comments_public;
							break;
						}
						Highlander.getDB().update(Schema.HIGHLANDER, 
									"UPDATE " + table
									+ "SET "+field.getQueryWhereName(analysis, false)+" = '"+ Highlander.getDB().format(Schema.HIGHLANDER, newTxt+"\nLast modified by "+modif) + "' "
									+ "WHERE id = " + variantAnnotationId);		
						commentsTable.setValueAt(newTxt, row, 2);
						commentsTable.setValueAt(modif, row, 3);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
		      break;
		}
	}

	public void export() {
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile("Highlander "+boxCategories.getSelectedItem()+" comments in "+boxAnalyses.getSelectedItem()+".xlsx");
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
					Sheet sheet = wb.createSheet(boxCategories.getSelectedItem()+" comments in "+boxAnalyses.getSelectedItem());
					sheet.createFreezePane(1, 1);		
					int r = 0;
					Row row = sheet.createRow(r++);
					for (int c = 0 ; c < commentsTable.getColumnCount() ; c++){
						row.createCell(c).setCellValue(commentsTable.getColumnName(c));
					}
					int nrow = commentsTable.getRowCount();
					waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" lines", false);
					waitingPanel.setProgressMaximum(nrow);

					for (int i=0 ; i < nrow ; i++ ){
						waitingPanel.setProgressValue(r);
						row = sheet.createRow(r++);
						for (int c = 0 ; c < commentsTable.getColumnCount() ; c++){
							if (commentsTable.getValueAt(i, c) == null)
								row.createCell(c);
							else if (commentsTable.getColumnClass(c) == Timestamp.class)
								row.createCell(c).setCellValue((Timestamp)commentsTable.getValueAt(i, c));
							else if (commentsTable.getColumnClass(c) == Integer.class)
								row.createCell(c).setCellValue(Integer.parseInt(commentsTable.getValueAt(i, c).toString()));
							else if (commentsTable.getColumnClass(c) == Long.class)
								row.createCell(c).setCellValue(Long.parseLong(commentsTable.getValueAt(i, c).toString()));
							else if (commentsTable.getColumnClass(c) == Double.class)
								row.createCell(c).setCellValue(Double.parseDouble(commentsTable.getValueAt(i, c).toString()));
							else if (commentsTable.getColumnClass(c) == Boolean.class)
								row.createCell(c).setCellValue(Boolean.parseBoolean(commentsTable.getValueAt(i, c).toString()));
							else 
								row.createCell(c).setCellValue(commentsTable.getValueAt(i, c).toString());
						}
						waitingPanel.setProgressValue(i);						
					}	
					for (int c = 0 ; c < commentsTable.getColumnCount() ; c++){
						sheet.autoSizeColumn(c);					
					}
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

	private class ColoredTableCellRenderer extends MultiLineTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JTextArea textArea = (JTextArea) comp;
			Palette palette = Palette.Indigo;
			Field field = new Field("field");
			switch((TargetField)boxCategories.getSelectedItem()) {
			case Genes:
				if (column == 0) field = Field.gene_symbol;	
				if (column == 1) field = Field.gene_comments_private;	
				if (column == 2) field = Field.gene_comments_public;
				palette = Field.gene_comments_public.getCategory().getColor();
				break;
			case Samples:
				if (column == 0) field = Field.sample;	
				if (column == 1) field = Field.sample_comments_private;	
				if (column == 2) field = Field.sample_comments_public;
				palette = Field.sample_comments_public.getCategory().getColor();
				break;
			case Variants:
				if (column == 1) field = Field.variant_comments_private;	
				if (column == 2) field = Field.variant_comments_public;
				palette = Field.variant_comments_public.getCategory().getColor();
				break;
			}
			
			if (row%2 == 0) textArea.setBackground(Resources.getTableEvenRowBackgroundColor(palette));
			else textArea.setBackground(Color.WHITE);
			textArea.setForeground(Color.black);
			textArea.setBorder(new LineBorder(Color.WHITE));
			if (isSelected) {
				textArea.setBackground(new Color(51,153,255));
			}
		return Highlander.getCellRenderer().renderCell(textArea, value, field, JLabel.LEFT, row, isSelected, Resources.getTableEvenRowBackgroundColor(palette), Color.WHITE, false);
		}
	}

	public class CommentsTableModel	extends AbstractTableModel {
		private Object[][] data;
		private String[] headers;

		public CommentsTableModel(Object[][] data, String[] headers) {    	
			this.data = data;
			this.headers = headers;
		}

		public int getColumnCount() {
			return headers.length;
		}

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

		public int getRowCount() {
			return data.length;
		}

		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		public void setValueAt(Object value, int row, int col) {
			data[row][col] = value;
			fireTableCellUpdated(row, col);
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}
}
