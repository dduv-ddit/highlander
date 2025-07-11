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
import java.awt.Font;
import java.awt.Frame;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel.ColorBy;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.datatype.Variant;

/**
* @author Raphael Helaers
*/

public class M6AScanner extends JFrame {

	public record Region(String chr, int start, int end, int length, String ensg, String enst, String symbol) implements Comparable<Region> {
		@Override
		public int compareTo(Region o) {
			int cmp = chr.compareTo(o.chr);
			if (cmp == 0) {
				cmp = Integer.compare(start, o.start);
				if (cmp == 0) {
					cmp = Integer.compare(end, o.end);
					if (cmp == 0) {
						cmp = ensg.compareTo(o.ensg);
						if (cmp == 0) {
							cmp = enst.compareTo(o.enst);
						}
					}
				}
			}
			return cmp;
		}
	}

	final private String pattern = "[AGT][AG]AC[ACT]";
	final private int patternLength = 5;
	
	private final Set<String> samples;
	//Keys are sample -> chromosome -> ensembl gene -> set of stuff
	private Map<String, Map<String, Map<String, Set<Variant>>>> variants = new TreeMap<>();
	private Map<String, Map<String, Map<String, Set<Region>>>> deletedRegions = new TreeMap<>();
	private Map<String, Map<String, Map<String ,Set<Region>>>> insertedRegions = new TreeMap<>();

	private JPanel north;
	private JPanel east;
	private JProgressBar bar;
	private JTable table;
	private M6AScannerTableModel model;
	private TableRowSorter<M6AScannerTableModel> sorter;
	private SearchField	searchField = new SearchField(20){
		@Override
		public void applyFilter(){
			RowFilter<M6AScannerTableModel, Object> rf = null;
	    //If current expression doesn't parse, don't update.
	    try {
	        rf = RowFilter.regexFilter("(?i)"+getText());
	    } catch (java.util.regex.PatternSyntaxException e) {
	        return;
	    }
	    final RowFilter<M6AScannerTableModel, Object> rff = rf;
			//waitingPanel.start();
			new Thread(new Runnable(){
				@Override
				public void run(){
					try{
						textFilter = rff;
						rowFilterExp = getTyppedText();
						applyFilters();
						/*
						List<RowFilter<M6AScannerTableModel,Object>> filters = new ArrayList<RowFilter<M6AScannerTableModel,Object>>();
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
	private RowFilter<M6AScannerTableModel, Object> textFilter = null;
	private Map<String, RowFilter<M6AScannerTableModel, Object>> columnFilters = new LinkedHashMap<>();
	private String rowFilterExp = null;
	static private WaitingPanel waitingPanel;
	
	public M6AScanner(Set<String> samples) {
		super();
		this.samples = samples;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (int)(screenSize.width*0.05);
		int height = screenSize.height - (int)(screenSize.height*0.05);
		setSize(new Dimension(width,height));
		setExtendedState(Frame.MAXIMIZED_BOTH);
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						search();	
						showTable();
					}
				}, "M6AScanner.search").start();
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
		setTitle("M6A Scanner");
		setIconImage(Resources.getScaledIcon(Resources.iM6A, 64).getImage());

		setLayout(new BorderLayout());

		JPanel south = new JPanel();	
		getContentPane().add(south, BorderLayout.SOUTH);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 40));
		export.setPreferredSize(new Dimension(54,54));
		export.setToolTipText("Export to an Excel file");
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
						chooser.setFile("M6A scan.xlsx");
						Tools.centerWindow(chooser, false);
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							String filename = chooser.getDirectory() + chooser.getFile();
							if (!filename.endsWith(".xlsx")) filename += ".xlsx";
							File xls = new File(filename);
							exportToExcel(xls);
						}
					}
				}, "M6AScanner.export").start();
			}
		});
		south.add(export);

		north = new JPanel();
		getContentPane().add(north, BorderLayout.NORTH);

		JPanel searchPanel = new JPanel(new GridBagLayout());

		JLabel title = new JLabel("Search");
		searchPanel.add(title, new GridBagConstraints(0,0,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,30,0,10), 0, 0));
		searchPanel.add(searchField, new GridBagConstraints(0,1,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,30,0,10), 0, 0));
		north.add(searchPanel);

		columnFilters.put("sample", null);
		columnFilters.put("type", null);
		columnFilters.put("caused by", null);
		columnFilters.put("gene symbol", null);
		
		east = new JPanel(new BorderLayout());
		getContentPane().add(east, BorderLayout.EAST);

		JScrollPane scrollAlignment = new JScrollPane();
		scrollAlignment.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		east.add(scrollAlignment, BorderLayout.CENTER);
		
		bar = new JProgressBar();
		east.add(bar, BorderLayout.SOUTH);
		
		JPanel center = new JPanel(new BorderLayout());
		getContentPane().add(center, BorderLayout.CENTER);

		JScrollPane scrollPane = new JScrollPane();
		table = new JTable();
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(true);			
		table.setCellSelectionEnabled(true);
		table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Integer.class, new ColoredTableCellRenderer());

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting())
					return;
				scrollAlignment.setViewportView(null);
				scrollAlignment.setViewportView(getAlignment());
			}
		});
		
		scrollPane.setViewportView(table);
		center.add(scrollPane, BorderLayout.CENTER);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);

	}
	
	private void showTable() {
		model = new M6AScannerTableModel();
		sorter = new TableRowSorter<M6AScannerTableModel>(model);
		table.setModel(model);
		table.setRowSorter(sorter);
		for (String field : columnFilters.keySet()) {
			JPanel filterPanel = new JPanel(new GridBagLayout());		
			JComboBox<String> boxFilter = new JComboBox<String>(model.getPossibleValues(field));
			//boxFilter.setPrototypeDisplayValue("AZERTYUIOPQSDFGHJKLM"); //to limit combobox size to this text, and not the longest item
			boxFilter.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						if (boxFilter.getSelectedItem().toString().equals("All")) {
							columnFilters.put(field, null);											
						}else {
							RowFilter<M6AScannerTableModel, Object> rowFilter = new RowFilter<M6AScannerTableModel, Object>() {
								@Override
								public boolean include(javax.swing.RowFilter.Entry<? extends M6AScannerTableModel, ? extends Object> entry) {
									M6AScannerTableModel model = entry.getModel();
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
			north.add(filterPanel);		
		}
	}

	public void applyFilters(){
		List<RowFilter<M6AScannerTableModel,Object>> filters = new ArrayList<RowFilter<M6AScannerTableModel,Object>>();
		if (textFilter != null) filters.add(textFilter);
		for (RowFilter<M6AScannerTableModel,Object> columnFiler : columnFilters.values()) {
			if (columnFiler != null) filters.add(columnFiler);			
		}
		if (!filters.isEmpty()){
			sorter.setRowFilter(RowFilter.andFilter(filters));
		}else{
			sorter.setRowFilter(null);
		}
	}
	
	private JPanel getAlignment() {
		if (table.getSelectedRow() >= 0) {
			int row = table.getSelectedRow();
			String sample = (String)table.getValueAt(row, model.getColumnIndex("sample"));
			String chr = (String)table.getValueAt(row, model.getColumnIndex("chr"));
			int start = (int)table.getValueAt(row, model.getColumnIndex("start"));
			int end = (int)table.getValueAt(row, model.getColumnIndex("end"));
			Interval interval = new Interval(Highlander.getCurrentAnalysis().getReference(), chr, start, end);
			try {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						bar.setValue(0);
						bar.setString("Loading Alignment");
						bar.setStringPainted(true);
					}
				});
				int width = east.getWidth()-30;
				if (width / interval.getSize() < 4) {
					width = interval.getSize() * 4;
				}
				final AlignmentPanel alignment = BamViewer.getAlignmentPanel(Highlander.getCurrentAnalysis(), sample, interval, null, false, false, false, ColorBy.STRAND, true, width, bar);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						bar.setString("Alignment loaded");
						bar.setStringPainted(true);
					}
				});				
				return alignment;
			}catch(Exception ex) {
				return Tools.getMessage("Cannot get alignment panel", ex);
			}
		}else {
			return new JPanel();
		}
	}

	public void search() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					waitingPanel.setProgressString("Checking M6A pattern for "+samples.size()+" samples", false);
					waitingPanel.setProgressMaximum(samples.size()*26);
				}
			});
			int count = 0;
			for (String sample : samples) {
				waitingPanel.setProgressValue(++count);
				waitingPanel.setProgressString("Fetching 3' UTR variants of " + sample, false);
				//System.out.println("# Fetching 3' UTR variants of " + sample);
				variants.put(sample, fetchVariants(sample));
				deletedRegions.put(sample, new TreeMap<>(new Tools.NaturalOrderComparator(true)));
				insertedRegions.put(sample, new TreeMap<>(new Tools.NaturalOrderComparator(true)));
				for (String chr : variants.get(sample).keySet()) {
					waitingPanel.setProgressValue(++count);
					waitingPanel.setProgressString("Looking for pattern in chr " + chr + " of " + sample, false);
					//System.out.println("# Looking for pattern "+ pattern +" in chromosome " + chr);
					deletedRegions.get(sample).put(chr, new TreeMap<>());
					insertedRegions.get(sample).put(chr, new TreeMap<>());
					for (String ensg : variants.get(sample).get(chr).keySet()) {
						deletedRegions.get(sample).get(chr).put(ensg, new TreeSet<>());
						insertedRegions.get(sample).get(chr).put(ensg, new TreeSet<>());
						//System.out.println("## " + ensg + " (" + DBUtils.getGeneSymbol(referenceGenome, ensg) + ")");
						Set<Region> wildTypeRegions = findRegionsInGene(Highlander.getCurrentAnalysis().getReference(), chr, ensg, pattern, new TreeSet<Variant>());
						/*
					if (!wildTypeRegions.isEmpty()) {
						System.out.println("### Wildtype regions");
						for (Region r : wildTypeRegions) {
							System.out.println("\t" + r.start + "-" + r.end);
						}
					}
						 */
						Set<Region> mutatedRegions = findRegionsInGene(Highlander.getCurrentAnalysis().getReference(), chr, ensg, pattern, variants.get(sample).get(chr).get(ensg));
						/*
					if (!mutatedRegions.isEmpty()) {
						System.out.println("### Mutated regions");
						for (Region r : mutatedRegions) {
							System.out.println("\t" + r.start + "-" + r.end);
						}
					}
						 */
						//List regions that disappeared because of variants
						Set<Region> deleted = new TreeSet<Region>(wildTypeRegions);
						deleted.removeAll(mutatedRegions);
						if (!deleted.isEmpty()) {
							/*
						System.out.println("### Deleted regions");
						for (Region r : deleted) {
							String cause = (r.length < 5) ? "INS" : (r.length > 5) ? "DEL" : "SNV";
							System.out.println("\t" + r.start + "-" + r.end + "\t" + cause);
						}
							 */
							deletedRegions.get(sample).get(chr).put(ensg, deleted);
						}
						//List regions that appeared because of variants
						Set<Region> inserted = new TreeSet<Region>(mutatedRegions);
						inserted.removeAll(wildTypeRegions);
						if (!inserted.isEmpty()) {
							/*
						System.out.println("### Inserted regions");
						for (Region r : inserted) {
							String cause = (r.length < 5) ? "INS" : (r.length > 5) ? "DEL" : "SNV";
							System.out.println("\t" + r.start + "-" + r.end + "\t" + cause);
						}
							 */
							insertedRegions.get(sample).get(chr).put(ensg, inserted);						
						}
					}
				}
				waitingPanel.setProgressValue(++count);
				waitingPanel.setProgressString("Clean artefacts due to INDELS in " + sample, false);
				//Clean artefacts due to INDELs that delete a region but where the pattern is still conserved
				for (String chr : deletedRegions.get(sample).keySet()) {
					for (String ensg : deletedRegions.get(sample).get(chr).keySet()) {
						for (Iterator<Region> itd = deletedRegions.get(sample).get(chr).get(ensg).iterator() ; itd.hasNext() ;) {
							Region del = itd.next();
							for (Iterator<Region> iti = insertedRegions.get(sample).get(chr).get(ensg).iterator() ; iti.hasNext() ;) {
								Region ins = iti.next();
								if (del.start() == ins.start() || del.end() == ins.end()) {
									itd.remove();
									iti.remove();
								}
							}
						}
					}
				}
			}
			waitingPanel.setProgressDone();
		}catch(Exception ex) {
			waitingPanel.forceStop();
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "M6A Scanner",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}
	
	private Map<String, Map<String, Set<Variant>>> fetchVariants(String sample) throws Exception {
		Map<String, Map<String, Set<Variant>>> variants = new TreeMap<>(new Tools.NaturalOrderComparator(true));
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT "+Field.chr+", "+Field.pos+", "+Field.length+", "+Field.reference+", "+Field.alternative+", "+Field.variant_type+", "+Field.gene_ensembl+" "
				+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
				+ Highlander.getCurrentAnalysis().getJoinStaticAnnotations()
				+ Highlander.getCurrentAnalysis().getJoinGeneAnnotations()
				+ Highlander.getCurrentAnalysis().getJoinProjects()
				+ "WHERE "+Field.sample.getQueryWhereName(Highlander.getCurrentAnalysis(), false)+" = '" + sample + "' "
				+ "AND "+Field.snpeff_effect.getQueryWhereName(Highlander.getCurrentAnalysis(), false)+" IN ('UTR_3_DELETED','UTR_3_PRIME') "
				+ "AND "+Field.filters.getQueryWhereName(Highlander.getCurrentAnalysis(), false)+" = 'PASS' "
				)) {
			while (res.next()) {
				String chr = res.getString(Field.chr.getName());
				String ensg = res.getString(Field.gene_ensembl.getName());				
				variants
				.computeIfAbsent(chr, k -> new TreeMap<String, Set<Variant>>())
				.computeIfAbsent(ensg, k -> new TreeSet<Variant>())
				.add(new Variant(chr, res.getInt(Field.pos.getName()), res.getInt(Field.length.getName()),  res.getString(Field.reference.getName()), res.getString(Field.alternative.getName()), VariantType.valueOf(res.getString(Field.variant_type.getName()))));
			}
		}
		return variants;
	}
	
	public Set<Region> findRegionsInGene(Reference referenceGenome, String chr, String ensg, String pattern, Set<Variant> variants) throws Exception {
		Set<Region> regions = new TreeSet<Region>();
		String enst = DBUtils.getEnsemblCanonicalTranscript(referenceGenome, ensg);
		String geneSymbol = DBUtils.getGeneSymbol(referenceGenome, ensg);
		Gene gene = new Gene(enst, referenceGenome, chr, true);
		int start3UTR = 0;
		int end3UTR = 0;
		if (gene.isStrandPositive()) {
			end3UTR = gene.getTranscriptionEnd();
			start3UTR = gene.getTranslationEnd();
		}else {
			start3UTR = gene.getTranscriptionStart();
			end3UTR = gene.getTranslationStart();
		}
		//Array to match relative positions in the 3' UTR, 
		List<Integer> posList = new ArrayList<Integer>();
		for (int i = 0 ; i < end3UTR-start3UTR+1 ; i++) {
			posList.add(start3UTR + i);
    }
		String seq = DBUtils.getSequence(referenceGenome, chr, start3UTR, end3UTR);
		//System.out.println("## Sequence for 3' UTR of " + ensg + ":\n\t" + seq);
		if (!variants.isEmpty()) {
			for (Variant v : variants) {
				//Don't check variant that starts before the 3' UTR, because they probably break the STOP codon
				if (v.getChromosome().equals(chr) && v.getPosition() <= end3UTR && v.getPosition()/*+v.getAffectedReferenceLength()*/ >= start3UTR) {
					String mutSeq = "";
					List<Integer> mutPos = new ArrayList<Integer>();
					for (int i=0 ; i < seq.length() ; ) {
						int pos = posList.get(i);
						if (pos == v.getAlternativePosition()) {
							if (v.getVariantType() != VariantType.DEL) {
								if (v.getVariantType() == VariantType.INS) {
									mutSeq += seq.charAt(i);
									mutPos.add(pos);
								}
								mutSeq += v.getAlternativeChangedNucleotides();	
								for (int j = 0; j <v.getAlternativeChangedNucleotides().length() ; j++) {
									mutPos.add(pos);
								}
							}
							i+= v.getAffectedReferenceLength()+1;
						}else {
							mutSeq += seq.charAt(i);
							mutPos.add(pos);
							i++;
						}
					}					
					//System.out.println("\t-> Applying variant " + v + " ("+v.getVariantType() + "|"+v.getReference()+" > "+v.getAlternative()+")");
					seq = mutSeq;
					posList = mutPos;
				}
			}
			//System.out.println("\t"+seq);
		}
		if (!gene.isStrandPositive()) {
			seq = Tools.reverseComplement(seq);
		}
		int[] positions = new int[posList.size()];
		for (int i = 0; i < posList.size(); i++) {
			positions[i] = (gene.isStrandPositive()) ? posList.get(i) : posList.get(posList.size() - 1 - i);
		}
		Pattern p = Pattern.compile(pattern);
		Matcher matcher = p.matcher(seq);
		while (matcher.find()){
			int startPos = (gene.isStrandPositive()) ? positions[matcher.start()] : positions[matcher.end()-1];
			int endPos = (gene.isStrandPositive()) ? positions[matcher.end()-1] : positions[matcher.start()];
			regions.add(new Region(chr, startPos, endPos, endPos-startPos+1, ensg, enst, geneSymbol));
		}
		return regions;
	}
	
	public void exportToExcel(File xls) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try{
			waitingPanel.start();
			waitingPanel.setProgressString("Export to Excel",true);
			try(Workbook wb = new SXSSFWorkbook(100)){  		
				Sheet sheet = wb.createSheet(Highlander.getCurrentAnalysis() + " " + df.format(System.currentTimeMillis()));
				sheet.createFreezePane(0, 1);		
				int r = 0;
				Row row = sheet.createRow(r++);
				String[] headers = new String[] {
						"sample",
						"chr",
						"start",
						"end",
						"type",
						"caused by",
						"gene symbol",
						"ensembl gene",
						"ensembl transcript",
				};
				for (int c = 0 ; c < headers.length ; c++){
					Cell cell = row.createCell(c);
					cell.setCellValue(headers[c]);
				}
				sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length));
				for (String sample : samples) {
					for (String chr : deletedRegions.get(sample).keySet()) {
						for (String ensg : deletedRegions.get(sample).get(chr).keySet()) {
							for (Region region : deletedRegions.get(sample).get(chr).get(ensg)) {
								String cause = (region.length < patternLength) ? "INS" : (region.length > patternLength) ? "DEL" : "SNV";
								row = sheet.createRow(r++);								
								int c=0;
								Cell cell = row.createCell(c++);
								cell.setCellValue(sample);
								cell = row.createCell(c++);
								cell.setCellValue(chr);
								cell = row.createCell(c++);
								cell.setCellValue(region.start());
								cell = row.createCell(c++);
								cell.setCellValue(region.end());
								cell = row.createCell(c++);
								cell.setCellValue("DELETED");
								cell = row.createCell(c++);
								cell.setCellValue(cause);
								cell = row.createCell(c++);
								cell.setCellValue(region.symbol());
								cell = row.createCell(c++);
								cell.setCellValue(region.ensg());
								cell = row.createCell(c++);
								cell.setCellValue(region.enst());
							}
							for (Region region : insertedRegions.get(sample).get(chr).get(ensg)) {
								String cause = (region.length < patternLength) ? "INS" : (region.length > patternLength) ? "DEL" : "SNV";
								row = sheet.createRow(r++);								
								int c=0;
								Cell cell = row.createCell(c++);
								cell.setCellValue(sample);
								cell = row.createCell(c++);
								cell.setCellValue(chr);
								cell = row.createCell(c++);
								cell.setCellValue(region.start());
								cell = row.createCell(c++);
								cell.setCellValue(region.end());
								cell = row.createCell(c++);
								cell.setCellValue("INSERTED");
								cell = row.createCell(c++);
								cell.setCellValue(cause);
								cell = row.createCell(c++);
								cell.setCellValue(region.symbol());
								cell = row.createCell(c++);
								cell.setCellValue(region.ensg());
								cell = row.createCell(c++);
								cell.setCellValue(region.enst());
							}
						}
					}
				}
				waitingPanel.setProgressString("Writing file ...",true);
				try (FileOutputStream fileOut = new FileOutputStream(xls)){
					wb.write(fileOut);
				}
				waitingPanel.setProgressDone();
			}
			waitingPanel.stop();
		}catch (IOException ex){
			waitingPanel.forceStop();
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Export M6A scanner results",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}catch (Exception ex){
			waitingPanel.forceStop();
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Export M6A scanner results",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}
	
	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			String colname = table.getColumnName(column);

			switch(colname) {
			case "chr", "start", "end", "type", "caused by" -> label.setHorizontalAlignment(SwingConstants.CENTER);
			default -> label.setHorizontalAlignment(SwingConstants.LEFT);
			}

			if (value == null) {
				value = "";
			}else{      	
				if (table.getColumnClass(column) == Integer.class){
					value = Tools.intToString((Integer)value);
				}
			}

			if (row%2 == 0) label.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Purple));
			else label.setBackground(Color.white);
			label.setForeground(Color.black);
			label.setBorder(new LineBorder(Color.WHITE));
			if (value != null){
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

	
	private class M6AScannerTableModel extends AbstractTableModel {
		final private List<Object[]> data;
		final private String[] headers;
		final private Class<?>[] classes;
		
		public M6AScannerTableModel() {
			headers = new String[] {
					"sample",
					"chr",
					"start",
					"end",
					"type",
					"caused by",
					"gene symbol",
					"ensembl gene",
					"ensembl transcript",
			};
			classes = new Class<?>[] {
					String.class,
					String.class,
					Integer.class,
					Integer.class,
					String.class,
					String.class,
					String.class,
					String.class,
					String.class,
			};
			data = new ArrayList<>();
			for (String sample : samples) {
				for (String chr : deletedRegions.get(sample).keySet()) {
					for (String ensg : deletedRegions.get(sample).get(chr).keySet()) {
						for (Region region : deletedRegions.get(sample).get(chr).get(ensg)) {
							String cause = (region.length < patternLength) ? "INS" : (region.length > patternLength) ? "DEL" : "SNV";
							Object[] row = new Object[headers.length];
							int c=0;
							row[c++] = sample;
							row[c++] = chr;
							row[c++] = region.start();
							row[c++] = region.end();
							row[c++] = "DELETED";
							row[c++] = cause;
							row[c++] = region.symbol();
							row[c++] = region.ensg();
							row[c++] = region.enst();
							data.add(row);
						}
						for (Region region : insertedRegions.get(sample).get(chr).get(ensg)) {
							String cause = (region.length < patternLength) ? "INS" : (region.length > patternLength) ? "DEL" : "SNV";
							Object[] row = new Object[headers.length];
							int c=0;
							row[c++] = sample;
							row[c++] = chr;
							row[c++] = region.start();
							row[c++] = region.end();
							row[c++] = "INSERTED";
							row[c++] = cause;
							row[c++] = region.symbol();
							row[c++] = region.ensg();
							row[c++] = region.enst();
							data.add(row);
						}
					}
				}
			}			
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
			return data.size();
		}

		@Override
		public Class<?> getColumnClass(int col) {
			return classes[col];
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data.get(row)[col];
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		public int getColumnIndex(String columnName) {
			for (int i=0 ; i < headers.length ; i++) {
				if (headers[i].equals(columnName)) return i;
			}
			return -1;
		}
		
		public String[] getPossibleValues(String columnName) {
			int col = getColumnIndex(columnName);
			Set<String> set = new TreeSet<>(new Tools.NaturalOrderComparator(true));
			for (Object[] row : data) {
				if (row[col] != null) set.add(row[col].toString());
			}
			List<String> list = new ArrayList<>();
			list.add("All");
			list.addAll(set);
			return list.toArray(new String[0]);
		}

	}
	
	public static void main(String[] args) {
		try {
			Highlander.initialize(new Parameters(false, new File("D:\\Dropbox\\Projets\\Highlander\\config\\GEHU\\settings.xml")), 5);
			Highlander.setCurrentAnalysis(new AnalysisFull(new Analysis("exomes_hg38")));
			Set<String> samples = new TreeSet<>();
			samples.add("JOYCE-HEM8-B");
			M6AScanner scanner = new M6AScanner(samples);
			scanner.search();
			scanner.exportToExcel(new File("C:\\Users\\Raphaël\\Downloads\\M6A-Joyce.xlsx"));
		} catch (Exception ex) {
			Tools.exception(ex);
		}	
		System.exit(0);
	}

}
