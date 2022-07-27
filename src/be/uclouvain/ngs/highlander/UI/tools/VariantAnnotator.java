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

package be.uclouvain.ngs.highlander.UI.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.JTabbedPaneCloseButton;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.table.MultiLineTableCellRenderer;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.AnnotatedVariant;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;

/**
* @author Raphael Helaers
*/

public class VariantAnnotator extends JFrame {

	private JComboBox<AnalysisFull> box_analysis;
	private JComboBox<String> box_chr;
	private JTextField field_pos;
	private JComboBox<String> box_alt;
	
	private JTabbedPaneCloseButton tabbedPane;
	
	static private WaitingPanel waitingPanel;

	public VariantAnnotator() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/5);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
	}

	private void initUI(){
		setTitle("Variant Annotator");
		setIconImage(Resources.getScaledIcon(Resources.iDbSearch, 64).getImage());

		getContentPane().setLayout(new BorderLayout());
		
		JPanel panel_north = new JPanel(new GridBagLayout());	
		getContentPane().add(panel_north, BorderLayout.NORTH);
		
		int x = 0;
		
		JLabel label_analysis = new JLabel("Analysis");
		panel_north.add(label_analysis, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		box_analysis = new JComboBox<AnalysisFull>(Highlander.getAvailableAnalyses().toArray(new AnalysisFull[0]));
		box_analysis.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					AnalysisFull analysis = (AnalysisFull)box_analysis.getSelectedItem();
					fillChromosomes(analysis);
				}
			}
		});
		panel_north.add(box_analysis, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		JLabel label_chr = new JLabel("Chromosome");
		panel_north.add(label_chr, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		box_chr = new JComboBox<String>();
		fillChromosomes(Highlander.getAvailableAnalyses().get(0));
		panel_north.add(box_chr, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		JLabel label_pos = new JLabel("Position");
		panel_north.add(label_pos, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		field_pos = new JTextField();
		field_pos.setColumns(10);
		panel_north.add(field_pos, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		JLabel label_alt = new JLabel("Alternative allele");
		panel_north.add(label_alt, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		box_alt = new JComboBox<String>(new String[] {"A","C","G","T"});
		panel_north.add(box_alt, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		JButton button_annotate = new JButton("Annotate variant");
		button_annotate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int pos = -1;
				try {
					pos = Integer.parseInt(field_pos.getText().trim());
				}catch(NumberFormatException nfe) {
					JOptionPane.showMessageDialog(new JFrame(), "Position is not valid", "Annotate variant",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
				if (pos != -1) {
					annotateVariant((AnalysisFull)box_analysis.getSelectedItem(), box_chr.getSelectedItem().toString(), pos, box_alt.getSelectedItem().toString());
				}
			}
		});
		panel_north.add(button_annotate, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		JPanel panel_south = new JPanel();	
		getContentPane().add(panel_south, BorderLayout.SOUTH);

		JButton btnClose = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.setToolTipText("Close tool");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel_south.add(btnClose);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 24));
		btnClose.setToolTipText("Export tabs to Excel sheets");
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						export();
					}
				}, "VariantAnnotator.export").start();
			}
		});
		panel_south.add(export);

		tabbedPane = new JTabbedPaneCloseButton();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);

	}
	
	private void fillChromosomes(AnalysisFull analysis) {
		box_chr.removeAllItems();
		for (String chr : analysis.getReference().getChromosomes()) {
			box_chr.addItem(chr);
		}
	}
	
	private void annotateVariant(AnalysisFull analysis, String chr, int pos, String alt) {
		List<AnnotatedVariant> list = new ArrayList<>();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setProgressString("Annotating variant", true);
				}
			});
			AnnotatedVariant va = new AnnotatedVariant(analysis);
			va.setFieldValue(Field.chr, chr);
			va.setFieldValue(Field.pos, ""+pos);
			va.setFieldValue(Field.reference, DBUtils.getSequence(analysis.getReference(), chr, pos, pos));
			va.setFieldValue(Field.alternative, alt);
			va.setFieldValue(Field.length, ""+1);
			va.setFieldValue(Field.variant_type, VariantType.SNV.toString());
			va.setAnnotation(Annotation.GONL);
			va.setAnnotation(Annotation.COSMIC);
			Variant variant = new Variant(
					(String)va.getValue(Field.chr), 
					(int)va.getValue(Field.pos), 
					(int)va.getValue(Field.length), 
					(String)va.getValue(Field.reference), 
					(String)va.getValue(Field.alternative), 
					(VariantType)va.getValue(Field.variant_type));
			Set<Gene> genesAtThisPosition = DBUtils.getGenesWithCanonicalTranscriptIntersect(analysis.getReference(), variant);
			va.setFieldValue(Field.num_genes, ""+genesAtThisPosition.size());
			if (genesAtThisPosition.isEmpty()) genesAtThisPosition.add(new Gene(analysis.getReference(), variant.getChromosome(), "", "", "", "", ""));
			for (Gene gene : genesAtThisPosition) {
				AnnotatedVariant vg = new AnnotatedVariant(va);
				if (vg.exist()){
					if (gene.getGeneSymbol().length() > 0) {
						vg.setEnsembl(gene, true);
						vg.setValue(Field.biotype, gene.getBiotype());
						vg.setSNPEffect(gene, true);
					}
					vg.setDBNSFP(true, true, true, true);
					vg.setConsensusPrediction(gene);
				}
				list.add(vg);
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
		for (AnnotatedVariant v : list) {
			showVariant(v);
		}
	}
	
	private void showVariant(AnnotatedVariant variant) {
		JPanel panel = new JPanel(new BorderLayout());
		String id = variant.getValue(Field.chr)+":"+variant.getValue(Field.pos)+" "+variant.getValue(Field.reference)+">"+variant.getValue(Field.alternative);
		if (variant.getValue(Field.gene_symbol) != null && variant.getValue(Field.gene_symbol).toString().length() > 0) id += " ("+variant.getValue(Field.gene_symbol)+")";
		tabbedPane.addTab(id, panel);

		JScrollPane scrollPane = new JScrollPane();
		final AnnotatedVariantTableModel model = new AnnotatedVariantTableModel(variant);
		JTable table = new JTable(model){
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				int colIndex = columnAtPoint(p);
				Object val = "";
				int realRowIndex = convertRowIndexToModel(rowIndex);
				if (colIndex == 0){
					val = model.getRowDescription(realRowIndex);
				}else{
					val = getValueAt(rowIndex, colIndex);
					if (val != null) {
						if (model.getRowField(realRowIndex).getFieldClass() == Integer.class) val = Tools.intToString(Integer.parseInt(val.toString()));
						else if (model.getRowField(realRowIndex).getFieldClass() == Long.class) val = Tools.longToString(Long.parseLong(val.toString()));
					}
				}
				tip = (val != null) ? val.toString() : "";
				return tip;
			}
		};
		Highlander.getCellRenderer().registerTableForHighlighting(getTitle(), table);
		table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());	
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(true);			
		table.setCellSelectionEnabled(true);
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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
		
		final TableRowSorter<AnnotatedVariantTableModel> sorter = new TableRowSorter<AnnotatedVariantTableModel>(model);
		SearchField	searchField = new SearchField(10){
			@Override
			public void applyFilter(){
				RowFilter<AnnotatedVariantTableModel, Object> rf = null;
		    //If current expression doesn't parse, don't update.
		    try {
		        rf = RowFilter.regexFilter("(?i)"+getText());
		    } catch (java.util.regex.PatternSyntaxException e) {
		        return;
		    }
		    sorter.setRowFilter(rf);   
			}
		};
		panel.add(searchField, BorderLayout.NORTH);
		table.setRowSorter(sorter);
		searchField.applyFilter();		

		scrollPane.setViewportView(table);
		panel.add(scrollPane, BorderLayout.CENTER);
		
		tabbedPane.setSelectedComponent(panel);
	}
	
	public void export() {
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile(Tools.formatFilename("annotated_variants.xlsx"));
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
					int nrow = tabbedPane.getComponents().length;
					waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" variants", false);
					waitingPanel.setProgressMaximum(nrow);
					int tab = 0;
					for (Component panel : tabbedPane.getComponents()) {
						for (Component comp : ((JPanel)panel).getComponents()) {
							if (comp instanceof JScrollPane) {
								waitingPanel.setProgressValue(tab);
								JTable table = (JTable)((JScrollPane)comp).getViewport().getComponent(0);
								Sheet sheet = wb.createSheet(tabbedPane.getTitleAt(tab).replace(':', '-'));
								tab++;
								int r = 0;
								Row row = sheet.createRow(r++);
								row.setHeightInPoints(50);
								for (int c = 0 ; c < table.getColumnCount() ; c++){
									row.createCell(c).setCellValue(table.getColumnName(c));
									sheet.setColumnWidth(c, 10000);
								}
								sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
								Highlander.getCellRenderer().clearCellStyles();
								AnnotatedVariantTableModel model = ((AnnotatedVariantTableModel)table.getModel());
								for (int i=0 ; i < model.getRowCount() ; i++ ){
									row = sheet.createRow(r++);
									for (int c = 0 ; c < model.getColumnCount() ; c++){
										Cell cell = row.createCell(c);
										if (model.getValueAt(i, c) != null){
											Field field = (c == 0) ? new Field("field") : model.getRowField(i);
											Highlander.getCellRenderer().formatXlsCell(model.getValueAt(i, c), field, JLabel.LEFT, sheet, cell, i);
											if (field.getFieldClass() == Timestamp.class){
												cell.setCellValue((Timestamp)model.getValueAt(i, c));
											}else if (field.getFieldClass() == Integer.class){
												cell.setCellValue(Integer.parseInt(model.getValueAt(i, c).toString()));
											}else if (field.getFieldClass() == Long.class){
												cell.setCellValue(Long.parseLong(model.getValueAt(i, c).toString()));
											}else if (field.getFieldClass() == Double.class){
												cell.setCellValue(Double.parseDouble(model.getValueAt(i, c).toString()));
											}else if (field.getFieldClass() == Boolean.class){
												cell.setCellValue(Boolean.parseBoolean(model.getValueAt(i, c).toString()));
											}else {
												cell.setCellValue(model.getValueAt(i, c).toString());
											}
										}
									}
								}
							}
						}
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
	
	private class ColoredTableCellRenderer extends MultiLineTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			int realRowIndex = table.convertRowIndexToModel(row);
			Field rowField = ((AnnotatedVariantTableModel)table.getModel()).getRowField(realRowIndex);
			Palette palette = (rowField.getCategory() != null) ? rowField.getCategory().getColor() : Palette.Gray;
			JTextArea textArea = (JTextArea) comp;
			if (realRowIndex%2 == 0) textArea.setBackground(Resources.getTableEvenRowBackgroundColor(palette));
			else textArea.setBackground(Resources.getTableOddRowBackgroundColor(palette));
			textArea.setForeground(Color.black);
			textArea.setBorder(new LineBorder(Color.WHITE));
			if (isSelected) {
				textArea.setBackground(new Color(51,153,255));
			}
			Field field = (column == 0) ? new Field("field") : ((AnnotatedVariantTableModel)table.getModel()).getRowField(realRowIndex);
			return Highlander.getCellRenderer().renderCell(textArea, value, field, JLabel.LEFT, row, isSelected, Resources.getTableEvenRowBackgroundColor(palette), Resources.getTableOddRowBackgroundColor(palette), false);
		}
	}

	public static class AnnotatedVariantTableModel	extends AbstractTableModel {
		private List<Field> fields;
		private List<Object> values;

		public AnnotatedVariantTableModel(AnnotatedVariant variant) {    	
			fields = new ArrayList<>();
			values = new ArrayList<>();
			for (Field field : Field.getAvailableFields(variant.getAnalysis(), false)) {
				switch(field.getAnnotationCode()) {
				case CONSENSUS:
				case COSMIC:
				case DBNSFP:
				case ENSEMBL:
				case EXAC:
				case GONL:
					fields.add(field);
					values.add(variant.getValue(field));
					break;
				case COMPUTED:
					if (!field.equals(Field.allele_num)
							&& !field.equals(Field.allelic_depth_proportion_ref)
							&& !field.equals(Field.allelic_depth_proportion_alt)
							) {
						fields.add(field);
						values.add(variant.getValue(field));						
					}
					break;
				case VCF:
					if (field.equals(Field.chr)
					|| field.equals(Field.pos)
					|| field.equals(Field.reference)
					|| field.equals(Field.alternative)
					|| field.equals(Field.gene_symbol)
					|| field.equals(Field.biotype)
					|| field.equals(Field.gene_ensembl)
					) {
						fields.add(field);
						values.add(variant.getValue(field));
					}
					break;
				case ALAMUT:
				case ANNOTSV:
				case FALSEPOSITIVEEXAMINER:
				case HIGHLANDER:
				default:
					break;
				}
			}
		}

		public int getColumnCount() {
			return 2;
		}

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

		public int getRowIndex(Field field){
			for (int i=0 ; i < fields.size() ; i++){
				if (fields.get(i).getName().equals(field.getName())) return i;
			}
			return -1;
		}

		public Field getRowField(int row) {
			return fields.get(row);
		}

		public String getRowDescription(int row) {
			return fields.get(row).getHtmlTooltip();
		}
		
		public int getRowCount() {
			return fields.size();
		}

		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		public Object getValueAt(int row, int col) {
			switch(col){
			case 0:
				return fields.get(row);
			case 1:
				return values.get(row);
			default:
				return null;
			}
		}

		public void setValueAt(Object value, int row, int col) {
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}


}
