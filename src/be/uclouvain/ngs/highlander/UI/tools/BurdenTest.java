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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.time.OffsetDateTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
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
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable.VariantsTableModel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.JSon;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter;
import be.uclouvain.ngs.highlander.datatype.filter.ListOfVariants;
import cern.jet.stat.Probability;

public class BurdenTest extends JFrame {

	public enum Source {HIGHLANDER,GONL,EXAC}

	private HighlanderObserver obs = new HighlanderObserver();

	private int scale = 1; // 1 = zoom max / full size

	private final Source source;
	private final Schema sourceSchema;
	private final String acExtCol;
	private final String anExtCol;

	private List<Field> displayedColumns;
	private Set<String> extAvailableColumns;
	private CustomFilter filter;
	private Set<String> hlSamples;

	private Map<String, Gene> genes = new TreeMap<String, Gene>();

	private Map<String,Map<Variant,Integer>> extVariants = new HashMap<String, Map<Variant,Integer>>(); // gene -> [variant -> ac]
	private int extTotal;
	private Map<String,Map<Variant,Integer>> hlVariants = new HashMap<String, Map<Variant,Integer>>(); // gene -> [variant -> ac]
	private int hlTotal;

	private Map<Integer,Set<Variant>> pixExt = new TreeMap<Integer, Set<Variant>>(); //pix -> [variants]
	private Map<Integer,Set<Variant>> pixHl = new TreeMap<Integer, Set<Variant>>(); //pix -> [variants]

	private JTabbedPane tabbedPane;
	private Map<String, JTable> tables = new TreeMap<String, JTable>();
	private Map<String, TableRowSorter<BurdenTestTableModel>> sorters = new TreeMap<String, TableRowSorter<BurdenTestTableModel>>();
	private Map<String, BurdenChart> charts = new TreeMap<String, BurdenChart>();
	private Map<String, JSlider> zoomSliders = new TreeMap<String, JSlider>();

	static private WaitingPanel waitingPanel;

	public BurdenTest(Highlander mainFrame, Source source, CustomFilter filtering){
		super();
		this.source = source;
		switch(source){
		case EXAC:
			sourceSchema = Schema.EXAC;
			acExtCol = "AC_Adj";
			anExtCol = "AN_Adj";
			break;
		case GONL:
			sourceSchema = Schema.GONL;
			acExtCol = "ac";
			anExtCol = "an";
			break;
		default:
			//Not supported
			sourceSchema = Schema.HIGHLANDER;
			acExtCol = "?";
			anExtCol = "?";
			break;
		}
		this.filter = filtering;
		this.displayedColumns = new ArrayList<Field>(mainFrame.getColumnSelection());
		setExtendedState(Frame.MAXIMIZED_BOTH);
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						fillTables();				
					}
				}, "BurdenTest.fillTables").start();
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
				obs.setControlName("RESIZE_TOOLBAR");
				for (String geneSymbol : genes.keySet()){
					int max = Math.max(1, charts.get(geneSymbol).getMaxPosPerPix());
					JSlider slider = zoomSliders.get(geneSymbol);
					if (slider.getValue() > max) slider.setValue(max);
					slider.setMaximum(max);
				}
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
		setTitle("Burden Test");
		setIconImage(Resources.getScaledIcon(Resources.iBurdenTest, 64).getImage());

		setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel();	
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		JButton exportTable = new JButton(Resources.getScaledIcon(Resources.iExcel, 40));
		exportTable.setPreferredSize(new Dimension(54,54));
		exportTable.setToolTipText("Export all tabs in one Excel file (1 sheet per tab)");
		exportTable.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						export();
					}
				}, "BurdenTest.export").start();
			}
		});
		buttonPanel.add(exportTable);

		JButton exportChart = new JButton(Resources.getScaledIcon(Resources.iExportJpeg, 40));
		exportChart.setPreferredSize(new Dimension(54,54));
		exportChart.setToolTipText("Export current chart to image file");
		exportChart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						exportChart();
					}
				}, "BurdenTest.exportChart").start();
			}
		});
		buttonPanel.add(exportChart);

		tabbedPane = new JTabbedPane();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}	

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			int realRow = table.convertRowIndexToModel(row);
			int realCol = table.convertColumnIndexToModel(column);
			Field field = ((VariantsTableModel)table.getModel()).getColumnField(realCol);
			int alignment = ((BurdenTestTableModel)table.getModel()).getColumnAlignment(realCol);
			Highlander.getCellRenderer().renderCell(label, value, field, alignment, row, isSelected, Resources.getTableEvenRowBackgroundColor(Palette.Orange), Color.WHITE, true);
			if (((BurdenTestTableModel)table.getModel()).getSource(realRow) == Source.HIGHLANDER) label.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Blue));
			else if (((BurdenTestTableModel)table.getModel()).getSource(realRow) == source) label.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Green));
			else label.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Pink));
			if (isSelected) {
				label.setBackground(new Color(51,153,255));
			}
			return label;
		}
	}

	public static class BurdenTestTableModel	extends AbstractTableModel {
		private Object[][] data;
		private Field[] headers;

		public BurdenTestTableModel(Object[][] highlanderData, Object[][] externalData, Field[] headers) {
			data = new Object[highlanderData.length+externalData.length][headers.length];
			int row = 0;
			for (int i=0 ; i < highlanderData.length ; i++){
				data [row] = highlanderData[i];
				row++;
			}
			for (int i=0 ; i < externalData.length ; i++){
				data [row] = externalData[i];
				row++;
			}
			this.headers = headers;
		}

		public Source getSource(int row){
			return (Source)data[row][0];
		}

		public int getPosition(int row){
			return Integer.parseInt(data[row][4].toString());
		}

		public String getReference(int row){
			return data[row][5].toString();
		}

		public String getAlternative(int row){
			return data[row][6].toString();
		}

		@Override
		public int getColumnCount() {
			return headers.length;
		}

		@Override
		public String getColumnName(int col) {
			return headers[col].getName();
		}

		public String getColumnDescription(int col) {
			return headers[col].getHtmlTooltip();
		}

		public int getColumnAlignment(int col){
			return headers[col].getAlignment();
		}

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return headers[columnIndex].getFieldClass();
		}

		public Field getColumnField(int columnIndex) {
			return headers[columnIndex];
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

	private void fillTables(){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			AnalysisFull analysis = Highlander.getCurrentAnalysis();

			waitingPanel.setProgressString("Submitting your query",true);
			Map<String, Map<Integer,Map<Zygosity,Set<Integer>>>> variantPosAndIds = new HashMap<String, Map<Integer,Map<Zygosity,Set<Integer>>>>();
			List<Field> neededFields = new ArrayList<Field>();
			neededFields.add(Field.variant_sample_id);
			neededFields.add(Field.pos);
			neededFields.add(Field.gene_symbol);
			neededFields.add(Field.chr);
			neededFields.add(Field.gene_ensembl);
			neededFields.add(Field.transcript_ensembl);
			neededFields.add(Field.transcript_refseq_mrna);
			neededFields.add(Field.biotype);
			neededFields.add(Field.zygosity);

			VariantResults variantResults = filter.retreiveData(neededFields, filter.getAllSamples(), "Retrieving involved genes");
			int i_gene_symbol=-1, i_chr=-1, i_gene_ensembl=-1, i_transcript_ensembl=-1, i_transcript_refseq_mrna=-1, i_pos=-1, i_zygosity=-1, i_id=-1, i_biotype=-1;
			for (int i=0 ; i < variantResults.headers.length ; i++){
				if (variantResults.headers[i].getName().equalsIgnoreCase("variant_sample_id")) i_id = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("pos")) i_pos = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("gene_symbol")) i_gene_symbol = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("chr")) i_chr = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("gene_ensembl")) i_gene_ensembl = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("transcript_ensembl")) i_transcript_ensembl = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("transcript_refseq_mrna")) i_transcript_refseq_mrna = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("biotype")) i_biotype = i;
				else if (variantResults.headers[i].getName().equalsIgnoreCase("zygosity")) i_zygosity = i;
			}
			for (int i=0 ; i < variantResults.data.length ; i++){
				String geneSymbol = variantResults.data[i][i_gene_symbol] != null ? variantResults.data[i][i_gene_symbol].toString() : null;
				try{
					Integer.parseInt(variantResults.data[i][i_chr].toString());
				}catch(NumberFormatException nex){
					geneSymbol = null;
				}
				if (geneSymbol != null && geneSymbol.length() > 0){
					if (!genes.containsKey(geneSymbol)){
						genes.put(geneSymbol, new Gene(analysis.getReference(), variantResults.data[i][i_chr].toString(), geneSymbol, 
								(variantResults.data[i][i_gene_ensembl] != null ? variantResults.data[i][i_gene_ensembl].toString() : null), 
								(variantResults.data[i][i_transcript_ensembl] != null ? variantResults.data[i][i_transcript_ensembl].toString() : null), 
								(variantResults.data[i][i_transcript_refseq_mrna] != null ? variantResults.data[i][i_transcript_refseq_mrna].toString() : null),
								(variantResults.data[i][i_biotype] != null ? variantResults.data[i][i_biotype].toString() : "?")
								));
						variantPosAndIds.put(geneSymbol, new TreeMap<Integer, Map<Zygosity,Set<Integer>>>());
					}
					int pos = Integer.parseInt(variantResults.data[i][i_pos].toString());
					if (!variantPosAndIds.get(geneSymbol).containsKey(pos)){
						variantPosAndIds.get(geneSymbol).put(pos, new EnumMap<Zygosity,Set<Integer>>(Zygosity.class));
						for (Zygosity zygosity : Zygosity.values()){
							variantPosAndIds.get(geneSymbol).get(pos).put(zygosity, new HashSet<Integer>());
						}
					}
					variantPosAndIds.get(geneSymbol).get(pos).get(Zygosity.valueOf(variantResults.data[i][i_zygosity].toString())).add(Integer.parseInt(variantResults.data[i][i_id].toString()));
				}
			}
			waitingPanel.setProgressString("Retreiving number of sample involved in your query",true);
			hlSamples = filter.getSamples();
			hlTotal = hlSamples.size()*2;

			waitingPanel.setProgressString("Retreiving fields available in "+source,true);
			extAvailableColumns = new HashSet<String>();
			try (Results res = Highlander.getDB().select(sourceSchema, "SHOW COLUMNS FROM chromosome_1")) {
				while (res.next()){
					extAvailableColumns.add(Highlander.getDB().getDescribeColumnName(sourceSchema, res));
				}
			}

			for (Iterator<Field> it = displayedColumns.iterator() ; it.hasNext() ; ){
				Field field = it.next();
				if (field.equals(Field.chr)) it.remove();
				else if (field.equals(Field.pos)) it.remove();
				else if (field.equals(Field.reference)) it.remove();
				else if (field.equals(Field.alternative)) it.remove();
			}
			displayedColumns.add(0, Field.alternative);
			displayedColumns.add(0, Field.reference);
			displayedColumns.add(0, Field.pos);
			displayedColumns.add(0, Field.chr);
			displayedColumns.add(0, new Field("total_an", sourceSchema, analysis.toString(), "int(5)", JSon.INFO, "Total number of alleles in all called genotypes", Annotation.HIGHLANDER, "", "Highlander/"+source, Integer.MAX_VALUE, null, 100, SwingConstants.CENTER));
			displayedColumns.add(0, new Field("total_ac", sourceSchema, analysis.toString(), "int(5)", JSon.INFO, "Allele count in all called genotypes", Annotation.HIGHLANDER, "", "Highlander/"+source, Integer.MAX_VALUE, null, 100, SwingConstants.CENTER));
			displayedColumns.add(0, new Field("source", sourceSchema, analysis.toString(), "enum('HIGHLANDER','"+source+"')", JSon.INFO, "Database from which the variant is related", Annotation.HIGHLANDER, "", "Highlander/"+source, Integer.MAX_VALUE, null, 100, SwingConstants.CENTER));

			for (final String geneSymbol : genes.keySet()){
				createGeneTab(geneSymbol, variantPosAndIds);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int max = charts.get(geneSymbol).getMaxPosPerPix();
						JSlider slider = zoomSliders.get(geneSymbol);
						slider.setMaximum(max);
					}
				});
			}

			waitingPanel.setProgressDone();

		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem: ", ex), "Burden test",
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

	private void createGeneTab(final String geneSymbol, Map<String, Map<Integer,Map<Zygosity,Set<Integer>>>> variantPosAndIds) throws Exception {
		final Gene gene = genes.get(geneSymbol);
		Field[] headers = displayedColumns.toArray(new Field[0]);

		JPanel panel = new JPanel(new BorderLayout(0,0));
		tabbedPane.addTab(geneSymbol, panel);

		JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

		topBar.add(new ColorLegend());

		JPanel numPanel = new JPanel(new BorderLayout(0,0));
		final JLabel extNumSelectedLabel = new JLabel("0 variants selected in "+source);
		numPanel.add(extNumSelectedLabel, BorderLayout.NORTH);
		final JLabel hlNumSelectedLabel = new JLabel("0 variants selected in Highlander");
		numPanel.add(hlNumSelectedLabel, BorderLayout.SOUTH);
		topBar.add(numPanel);

		JPanel sep = new JPanel();
		sep.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		sep.setPreferredSize(new Dimension(2, 50));
		topBar.add(sep);

		JPanel transcriptPanel = new JPanel(new BorderLayout(0,0));
		JLabel ensemblTransLabel = new JLabel("Ensembl transcript : " + gene.getEnsemblTranscript());
		transcriptPanel.add(ensemblTransLabel, BorderLayout.NORTH);
		JLabel refseqTransLabel = new JLabel("RefSeq transcript : " + gene.getRefSeqTranscript());
		transcriptPanel.add(refseqTransLabel, BorderLayout.SOUTH);
		topBar.add(transcriptPanel);

		JButton zoomOriginal = new JButton(Resources.getScaledIcon(Resources.iZoomIn, 40));
		zoomOriginal.setPreferredSize(new Dimension(54,54));
		zoomOriginal.setToolTipText("Zoom in");
		zoomOriginal.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						scale--;
						if (scale < 1) scale = 1;
						zoomSliders.get(geneSymbol).setValue(scale);
					}
				}, "BurdenTest.zoomOriginal").start();
			}
		});
		topBar.add(zoomOriginal);

		JSlider zoomSlider = new JSlider(1, 1);
		zoomSliders.put(geneSymbol,zoomSlider);
		zoomSlider.setToolTipText("Set zoom level");
		zoomSlider.setMajorTickSpacing(5);
		zoomSlider.setMinorTickSpacing(1);
		zoomSlider.setPaintLabels(false);
		zoomSlider.setPaintTicks(true);
		zoomSlider.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				if (!source.getValueIsAdjusting()){
					scale = source.getValue();
					if (scale < 1) scale = 1;
					validate();
					repaint();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							charts.get(geneSymbol).scrollToSelection();
						}
					});				
				}
			}
		});
		topBar.add(zoomSlider);

		JButton zoomBestFit = new JButton(Resources.getScaledIcon(Resources.iZoomOut, 40));
		zoomBestFit.setPreferredSize(new Dimension(54,54));
		zoomBestFit.setToolTipText("Zoom out");
		zoomBestFit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						scale++;
						int max = charts.get(geneSymbol).getMaxPosPerPix();
						if (scale > max) scale = max;
						if (scale < 1) scale = 1;
						zoomSliders.get(geneSymbol).setValue(scale);						
					}
				}, "BurdenTest.zoomBestFit").start();
			}
		});
		topBar.add(zoomBestFit);

		JButton chiSquare = new JButton(Resources.getScaledIcon(Resources.iChiSquare, 40));
		chiSquare.setPreferredSize(new Dimension(54,54));
		chiSquare.setToolTipText("Compute a Chi² p-value");
		chiSquare.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						final Set<VariantType> variantTypes = new HashSet<VariantType>();
						JPanel panel = new JPanel(new GridBagLayout());
						panel.add(new JLabel("Which kind of changes to you want to consider ?"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
						int y=1;
						for (final VariantType type : VariantType.values()){
							variantTypes.add(type);
							JCheckBox check = new JCheckBox(type.toString());
							check.setSelected(true);
							check.addItemListener(new ItemListener() {
								@Override
								public void itemStateChanged(ItemEvent e) {
									if (e.getStateChange() == ItemEvent.SELECTED){
										variantTypes.add(type);
									}else if (e.getStateChange() == ItemEvent.DESELECTED){
										variantTypes.remove(type);
									}
								}
							});
							panel.add(check, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
							y++;
						}
						panel.add(new JLabel("Do NOT remove types included in your filtering criteria !"), new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
						int res = JOptionPane.showConfirmDialog(BurdenTest.this, panel, "Chi square", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iChiSquare,64));
						if (res == JOptionPane.YES_OPTION) chiSquare(gene, variantTypes);
					}
				}, "BurdenTest.chiSquare").start();
			}
		});
		topBar.add(chiSquare);

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPanel.setResizeWeight(0.5);
		panel.add(splitPanel, BorderLayout.CENTER);

		JScrollPane scrollPaneBottom = new JScrollPane();
		splitPanel.add(scrollPaneBottom, JSplitPane.BOTTOM);

		waitingPanel.setProgressString("Retreiving information from RefSeq for " + geneSymbol,true);
		/*
		if (geneData.getRefSeqTranscript() == null){
			try (Results res = Highlander.getDB().select(Schema.UCSC, "SELECT * " +
					"FROM (SELECT * FROM refGene WHERE name2 = '"+geneSymbol+"' AND exonCount = (SELECT MAX(exonCount) FROM refGene WHERE name2 = '"+geneSymbol+"')) as t1 " +
					"WHERE txStart = (SELECT MIN(txStart) FROM (SELECT * FROM refGene WHERE name2 = '"+geneSymbol+"' AND chrom = 'chr"+geneData.getChromosome()+"' AND exonCount = (SELECT MAX(exonCount) FROM refGene WHERE name2 = '"+geneSymbol+"')) as t2)")){
			if (res.next()){
				geneData.setRefSeqTranscript(res.getString("name"));
				geneData.setEnsemblTranscript("?");
				ensemblTransLabel.setText("No Ensembl/RefSeq transcript correspondance found !");
				ensemblTransLabel.setForeground(Color.RED);
				refseqTransLabel.setText("RefSeq transcript : " + geneData.getRefSeqTranscript());
			}else{
				//Gene symbol not found in RefSeq !
				geneData.setRefSeqTranscript(null);
				refseqTransLabel.setText("Gene symbol NOT FOUND in RefSeq");
				refseqTransLabel.setForeground(Color.RED);
			}
			}
		}
		geneData.setExonDataFromRefSeq();
		 */
		gene.setExonDataFromEnsembl();
		for (Entry<Integer, Map<Zygosity,Set<Integer>>> e : variantPosAndIds.get(geneSymbol).entrySet()){
			if (gene.isExonic(e.getKey(), true)){
				for (Zygosity zigosity : e.getValue().keySet()){
					for (int id : e.getValue().get(zigosity)){
						gene.addVariantId(id, zigosity);
					}
				}
			}
		}

		waitingPanel.setProgressString("Retreiving information from "+source+" for " + geneSymbol,true);
		extVariants.put(geneSymbol, new HashMap<Variant,Integer>());
		StringBuilder query = new StringBuilder();
		query.append("SELECT * FROM chromosome_"+gene.getChromosome());
		query.append(" WHERE gene_symbol = '"+geneSymbol+"'");
		String exonwhere = gene.getExonicWhereClause("chromosome_"+gene.getChromosome());
		if (exonwhere.length() > 0) query.append(" AND " + exonwhere);
		String extwhere = filter.getExternalSourceQueryWhereClause(gene.getChromosome(), extAvailableColumns, false);
		if (extwhere.length() > 0) query.append(" AND (" + extwhere + ")");
		query.append(" ORDER BY pos");
		List<Object[]> arrayList = new ArrayList<Object[]>();
		try (Results res = Highlander.getDB().select(sourceSchema, query.toString())) {
			while (res.next()){
				Object[] array = new Object[headers.length];
				array[0] = source;
				array[1] = res.getInt(acExtCol);
				array[2] = res.getInt(anExtCol);
				extTotal = res.getInt(anExtCol);
				for (int col = 3 ; col < headers.length ; col++){
					if (extAvailableColumns.contains(headers[col].getName())){
						array[col] = res.getObject(headers[col].getName());
					}
				}
				Variant var = new Variant(res.getString("chr"),res.getInt("pos"),res.getString("reference"),res.getString("alternative"));
				extVariants.get(geneSymbol).put(var, res.getInt(acExtCol));
				arrayList.add(array);
			}
		}
		Object[][] extData = new Object[arrayList.size()][headers.length];
		int row = 0;
		for (Object[] array : arrayList){
			extData[row] = array;
			row++;
		}

		waitingPanel.setProgressString("Retreiving information from Highlander for " + geneSymbol,true);
		hlVariants.put(geneSymbol, new HashMap<Variant,Integer>());
		ListOfVariants pseudoFilter = new ListOfVariants(gene.getAllVariantsIds());
		List<Field> pseudoHeaders = new ArrayList<Field>(displayedColumns);
		pseudoHeaders.remove(0);
		pseudoHeaders.remove(0);
		pseudoHeaders.remove(0);
		VariantResults variantResults =	pseudoFilter.retreiveData(pseudoHeaders, pseudoFilter.getAllSamples(), "Highlander info " + geneSymbol);
		Map<Integer,List<Integer>> posSorting = new TreeMap<Integer, List<Integer>>();
		row = 0;
		while (row < variantResults.data.length){
			int pos = Integer.parseInt(variantResults.data[row][1].toString());
			if (!posSorting.containsKey(pos)){
				posSorting.put(pos, new ArrayList<Integer>());
			}
			posSorting.get(pos).add(row);
			row++;
		}
		Object[][] highlanderData = new Object[variantResults.data.length][headers.length];
		row = 0;
		for (List<Integer> posList : posSorting.values()){
			for (int r : posList){
				highlanderData[row][0] = Source.HIGHLANDER;
				highlanderData[row][1] = 0;
				highlanderData[row][2] = hlTotal;					
				for (int col = 3 ; col < headers.length ; col++){
					highlanderData[row][col] = variantResults.data[r][col-3];
				}
				Variant var = new Variant(highlanderData[row][3].toString(),Integer.parseInt(highlanderData[row][4].toString()),highlanderData[row][5].toString(),highlanderData[row][6].toString());
				if (!hlVariants.get(geneSymbol).containsKey(var)){
					hlVariants.get(geneSymbol).put(var, 0);
				}
				int allNum = (gene.getVariantsIds(Zygosity.Heterozygous).contains(variantResults.id[r])) ? 1 : 2;
				hlVariants.get(geneSymbol).put(var, hlVariants.get(geneSymbol).get(var)+allNum);
				row++;
			}
		}
		for (int i=0 ; i < highlanderData.length ; i++){
			Variant var = new Variant(highlanderData[i][3].toString(),Integer.parseInt(highlanderData[i][4].toString()),highlanderData[i][5].toString(),highlanderData[i][6].toString());
			highlanderData[i][1] = hlVariants.get(geneSymbol).get(var);
		}

		final BurdenTestTableModel model = new BurdenTestTableModel(highlanderData, extData, headers);
		TableRowSorter<BurdenTestTableModel> sorter = new TableRowSorter<BurdenTestTableModel>(model);
		final JTable table = new JTable(model){

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
				tip = (val != null) ? val.toString() : "";
				return tip;
			}

			@Override
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					@Override
					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						if (index >= 0){
							int realIndex = columnModel.getColumn(index).getModelIndex();
							return ((BurdenTestTableModel)table.getModel()).getColumnDescription(realIndex);
						}else{
							return null;
						}
					}
				};
			}

		};
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()){
					int extCount = 0;
					Set<Integer> hlpos = new HashSet<Integer>();
					for (int row : table.getSelectedRows()){
						if (model.getSource(table.convertRowIndexToModel(row)) == source){
							extCount++;
						}else{
							hlpos.add(model.getPosition(table.convertRowIndexToModel(row)));
						}
					}
					extNumSelectedLabel.setText(extCount+" variants selected in "+source);
					hlNumSelectedLabel.setText(hlpos.size()+" variants selected in Highlander");
					charts.get(geneSymbol).scrollToSelection();
					validate();
					repaint();
				}
			}
		});
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		scrollPaneBottom.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scrollPaneBottom.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);	
		for (Class<?> cl : Field.AVAILABLE_CLASSES){
			table.setDefaultRenderer(cl, new ColoredTableCellRenderer());			
		}
		for (int i=0 ; i < headers.length ; i++){
			table.getColumnModel().getColumn(i).setPreferredWidth(headers[i].getSize());
		}
		table.setRowSorter(sorter);
		scrollPaneBottom.setViewportView(table);
		tables.put(geneSymbol, table);
		sorters.put(geneSymbol, sorter);

		JScrollPane scrollPaneTop = new JScrollPane();
		scrollPaneTop.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scrollPaneTop.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);	
		splitPanel.add(scrollPaneTop, JSplitPane.TOP);

		waitingPanel.setProgressString("Retreiving " + geneSymbol +" exons from RefSeq",true);
		BurdenChart graphicView = new BurdenChart(gene);

		scrollPaneTop.setViewportView(graphicView);
		charts.put(geneSymbol, graphicView);

		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(topBar, obs, 40);
		panel.add(scrollablePanel, BorderLayout.NORTH);
	}

	public Color getVariantColor(double value){
		if (value <= 0.1){
			value *= 200;
		}else if (value <= 1.0){
			value = ((value-0.1)*20.0/0.9)+20.0;
		}else if (value <= 10.0){
			value = ((value-1.0)*30.0/9.0)+40.0;
		}else if (value <= 50.0){
			value = ((value-10.0)*0.5)+70.0;
		}else{
			value = ((value-50.0)*0.2)+90.0;
		}
		double H = (100.0 - value) * 0.007 ; // Hue (note 0.7 = Blue)
		double S = 0.9; // Saturation
		double B = 0.9; // Brightness
		return Color.getHSBColor((float)H, (float)S, (float)B);
	}

	public class ColorLegend extends JPanel {

		@Override
		public void paintComponent(Graphics g1) {

			Graphics2D g = (Graphics2D)g1;

			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			g.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);

			Font baseFont = g.getFont();
			baseFont = baseFont.deriveFont(Font.PLAIN, baseFont.getSize()+2);
			Font smallFont = baseFont.deriveFont(Font.PLAIN, baseFont.getSize()-2);

			Dimension dim = this.getSize();
			int width = dim.width;
			int height = dim.height;

			int w = 4;
			int x = 20;
			int y = 20;

			width = (w*100)+40;
			height = 60;
			setSize(new Dimension(width, height));
			setPreferredSize(new Dimension(width, height));

			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);						

			g.setColor(Color.black);
			String label = "Allele frequency";
			int swidth = (int)g.getFontMetrics().getStringBounds(label,g).getWidth();
			g.drawString(label, (width/2)-swidth/2, 10);

			for (int f = 0 ; f <= 100 ; f++){
				double H = (100.0 - f) * 0.007 ; // Hue (note 0.7 = Blue)
				double S = 0.9; // Saturation
				double B = 0.9; // Brightness
				g.setColor(Color.getHSBColor((float)H, (float)S, (float)B));
				g.fillRect(x+(f*w), y, w, 20);
				if (f%10 == 0){
					String s = "";
					if (f == 0){
						s = "0%";
					}else if (f == 10){
						s = "0.05%";
					}else if (f == 20){
						s = "0.1%";
					}else if (f == 30){
						s = "0.5%";
					}else if (f == 40){
						s = "1%";
					}else if (f == 50){
						s = "4%";
					}else if (f == 60){
						s = "7%";
					}else if (f == 70){
						s = "10%";
					}else if (f == 80){
						s = "30%";
					}else if (f == 90){
						s = "50%";
					}else if (f == 100){
						s = "100%";
					}
					g.setColor(Color.black);
					g.drawLine(x+(f*w)+w/2, y, x+(f*w)+w/2, y+25);
					g.setFont(smallFont);
					swidth = (int)g.getFontMetrics().getStringBounds(s,g).getWidth();
					g.drawString(s, x+(f*w)+w/2-swidth/2, y+35);
				}
			}
		}
	}

	public class BurdenChart extends JPanel {

		private int variantHeight;
		private int variantWidth = 5;
		private int pix = variantWidth+2;
		private int posPerPix;
		private int exonHeight = 30;
		private int minExonWidth = 30;
		private int utrHeight = 15;
		private Color exonColor = new Color(113,80,152);
		private int intronWidth;
		private int intronHeight = 6;
		private Color selectionColor = new Color(248,127,255);

		private Gene gene;

		private Font baseFont;
		private Font bigFont;
		private Stroke basicStroke = new BasicStroke();
		private Stroke boldStroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);

		private Point startDragSelection = null;
		private Point endDragSelection = null;

		public BurdenChart(){
			super();
			ToolTipManager.sharedInstance().registerComponent(this);
		}

		public BurdenChart(Gene g){
			super();
			ToolTipManager.sharedInstance().registerComponent(this);
			this.gene = g;
			addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {
					if (startDragSelection != null && endDragSelection != null){
						Set<Variant> extSelection = getExtVariationsBetween(startDragSelection,endDragSelection);
						Set<Variant> hlSelection = getHighlanderVariationsBetween(startDragSelection,endDragSelection);
						JTable table = tables.get(gene.getGeneSymbol());
						BurdenTestTableModel model = (BurdenTestTableModel)table.getModel();
						tables.get(gene.getGeneSymbol()).getSelectionModel().clearSelection();
						for (Variant var : extSelection){
							int selectedRow = -1;
							for (int row=0 ; row < table.getRowCount() ; row++){
								if (model.getSource(table.convertRowIndexToModel(row)) == source && model.getPosition(table.convertRowIndexToModel(row)) == var.getPosition()){
									selectedRow = row;
									break;
								}
							}
							tables.get(gene.getGeneSymbol()).getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
						}
						for (Variant var : hlSelection){
							List<Integer> selectedRows = new ArrayList<Integer>();
							for (int row=0 ; row < table.getRowCount() ; row++){
								if (model.getSource(table.convertRowIndexToModel(row)) == Source.HIGHLANDER && model.getPosition(table.convertRowIndexToModel(row)) == var.getPosition()){
									selectedRows.add(row);
								}
							}
							for (int selectedRow : selectedRows){
								tables.get(gene.getGeneSymbol()).getSelectionModel().addSelectionInterval(selectedRow,selectedRow);
							}
						}
					}
					startDragSelection = null;
					endDragSelection = null;
					repaint();
					Rectangle r = tables.get(gene.getGeneSymbol()).getCellRect(tables.get(gene.getGeneSymbol()).getSelectedRow(), 0, true);
					tables.get(gene.getGeneSymbol()).scrollRectToVisible(r);
				}
				@Override
				public void mousePressed(MouseEvent e) {
					startDragSelection = e.getPoint();
				}
				@Override
				public void mouseExited(MouseEvent e) {
				}
				@Override
				public void mouseEntered(MouseEvent e) {
				}
				@Override
				public void mouseClicked(MouseEvent e) {
					Point p = e.getPoint();
					Optional<Variant> optionalVar = getVariationAt(p);
					if (optionalVar.isPresent()){
						Variant var = optionalVar.get();
						Source thisSource = ((p.y < getHeight()/2)?source:Source.HIGHLANDER);
						JTable table = tables.get(gene.getGeneSymbol());
						BurdenTestTableModel model = (BurdenTestTableModel)table.getModel();
						List<Integer> selectedRows = new ArrayList<Integer>();
						for (int row=0 ; row < table.getRowCount() ; row++){
							if (model.getSource(table.convertRowIndexToModel(row)) == thisSource && model.getPosition(table.convertRowIndexToModel(row)) == var.getPosition()){
								selectedRows.add(row);
							}
						}
						if (e.isControlDown()){
							boolean selected = false;
							for (int selectedRow : selectedRows){
								if (tables.get(gene.getGeneSymbol()).getSelectionModel().isSelectedIndex(selectedRow)){
									tables.get(gene.getGeneSymbol()).getSelectionModel().removeSelectionInterval(selectedRow, selectedRow);
									selected = true;
								}
							}
							if (!selected){
								for (int selectedRow : selectedRows){
									tables.get(gene.getGeneSymbol()).getSelectionModel().addSelectionInterval(selectedRow,selectedRow);
									Rectangle r = tables.get(gene.getGeneSymbol()).getCellRect(selectedRow, 0, true);
									tables.get(gene.getGeneSymbol()).scrollRectToVisible(r);
								}
							}
						}else{
							tables.get(gene.getGeneSymbol()).getSelectionModel().clearSelection();
							for (int selectedRow : selectedRows){
								tables.get(gene.getGeneSymbol()).getSelectionModel().addSelectionInterval(selectedRow,selectedRow);
								Rectangle r = tables.get(gene.getGeneSymbol()).getCellRect(selectedRow, 0, true);
								tables.get(gene.getGeneSymbol()).scrollRectToVisible(r);
							}
						}						
					}
				}
			});
			addMouseMotionListener(new MouseMotionListener() {
				@Override
				public void mouseMoved(MouseEvent e) {
				}
				@Override
				public void mouseDragged(MouseEvent e) {
					endDragSelection = e.getPoint();
					repaint();
				}
			});
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			Point p = e.getPoint();
			Optional<Integer> optionalExon = getExonAt(p);
			if (optionalExon.isPresent()){
				int exon = optionalExon.get();
				return "<html><b>Exon "+((gene.isStrandPositive()) ? (exon+1) : (gene.getExonCount()-exon))+"</b><br>"+
						"Start : "+gene.getExonStart(exon)+"<br>"+
						"End : "+gene.getExonEnd(exon)+"<br>"+
						"Length : "+gene.getExonLength(exon)+"<br>"+
						"</html>";
			}
			Optional<Variant> optionalVar = getVariationAt(p);
			if (optionalVar.isPresent()){
				Variant var = optionalVar.get();
				int ac = ((p.y < getHeight()/2)?extVariants.get(gene.getGeneSymbol()).get(var):hlVariants.get(gene.getGeneSymbol()).get(var));
				double freq = ((p.y < getHeight()/2)?(double)ac / (double)extTotal:(double)ac / (double)hlTotal);
				return "<html><b>["+"chr"+var.getChromosome()+":"+var.getPosition()+"]</b><br>"+
				"Change type : "+var.getVariantType()+"<br>"+
				"Reference : "+var.getReference()+"<br>"+
				"Alternative : "+var.getAlternative()+"<br>"+
				"Allele count : "+ac+"<br>"+
				"Frequency : "+Tools.doubleToPercent(freq, 2)+"<br>"+
				"Source : "+((p.y < getHeight()/2)?source:"Highlander")+"<br>"+
				"</html>";				
			}
			return null;
		}

		public void scrollToSelection(){
			JTable table = tables.get(gene.getGeneSymbol());
			BurdenTestTableModel model = (BurdenTestTableModel)table.getModel();
			int row = table.getSelectedRow();
			if (row >=0){
				int pos = model.getPosition(table.convertRowIndexToModel(row));
				int x = 20 + intronWidth;
				for (int i=0 ; i < gene.getExonCount() ; i++){
					if (pos >= gene.getExonStart(i)-Gene.SPLICE_SITE_EXTENDS && pos <= gene.getExonEnd(i)+Gene.SPLICE_SITE_EXTENDS){
						int xv = x + (pos - gene.getExonStart(i))/posPerPix*pix + 1;
						Rectangle rect = new Rectangle(new Point(xv-(getParent().getWidth()/2), 0), new Dimension(getParent().getWidth(), getHeight()));
						scrollRectToVisible(rect);
						return;
					}
					int w = (gene.getExonLength(i)/posPerPix) * pix;
					x += w + intronWidth;
				}
			}
		}

		private Optional<Integer> getExonAt(Point p){
			if (p.y >= ((getHeight()/2)-(exonHeight/2)) && p.y <= ((getHeight()/2)+(exonHeight/2))){
				int x = 20 + intronWidth;
				for (int i=0 ; i < gene.getExonCount() ; i++){
					int w = (gene.getExonLength(i)/posPerPix) * pix;
					if (p.x >= x && p.x <= x+w){
						return Optional.of(i);
					}
					x += w + intronWidth;
				}							
			}
			return Optional.empty();
		}

		private Optional<Variant> getVariationAt(Point p){
			Map<Integer,Set<Variant>> pixes = (p.y < getHeight()/2) ? pixExt : pixHl;
			for (int xv : pixes.keySet()){
				if (p.x >= xv && p.x <= xv+pix){
					int hv = variantHeight / pixes.get(xv).size();
					int count=0;
					for (Variant var : pixes.get(xv)){
						int yv = (p.y < getHeight()/2) ? (getHeight()/2 - (exonHeight/2) - 10 - (hv*(count+1))) : (getHeight()/2 + (exonHeight/2) + 10 + (hv*count));
						if (p.y >= yv && p.y <= yv+hv-2){
							return Optional.of(var);
						}
						count++;
					}
				}
			}
			return Optional.empty();
		}

		private Set<Variant> getExtVariationsBetween(Point a, Point b){
			Set<Variant> results = new TreeSet<Variant>();
			int leftX = Math.min(a.x, b.x);
			int rightX = Math.max(a.x, b.x);
			int topY = Math.min(a.y, b.y);
			int bottomY = Math.max(a.y, b.y);
			if (topY < getHeight()/2){
				for (int xv : pixExt.keySet()){
					if (xv >= leftX && xv+pix <= rightX){
						int hv = variantHeight / pixExt.get(xv).size();
						int count=0;
						for (Variant var : pixExt.get(xv)){
							int yv = getHeight()/2 - (exonHeight/2) - 10 - (hv*(count+1));
							if (yv >= topY && yv+hv-2 <= bottomY){
								results.add(var);
							}
							count++;
						}
					}
				}
			}
			return results;
		}

		private Set<Variant> getHighlanderVariationsBetween(Point a, Point b){
			Set<Variant> results = new TreeSet<Variant>();
			int leftX = Math.min(a.x, b.x);
			int rightX = Math.max(a.x, b.x);
			int topY = Math.min(a.y, b.y);
			int bottomY = Math.max(a.y, b.y);
			if (bottomY > getHeight()/2){
				for (int xv : pixHl.keySet()){
					if (xv >= leftX && xv+pix <= rightX){
						int hv = variantHeight / pixHl.get(xv).size();
						int count=0;
						for (Variant var : pixHl.get(xv)){
							int yv = getHeight()/2 + (exonHeight/2) + 10 + (hv*count);
							if (yv >= topY && yv+hv-2 <= bottomY){
								results.add(var);
							}
							count++;
						}
					}
				}
			}
			return results;
		}

		private void drawExonNumber(Graphics2D g, int i, int x, int y, int length){
			String exonString = ((int)g.getFontMetrics().getStringBounds("EXON XX",g).getWidth()+10 < (gene.getCodingLength(i)/posPerPix) * pix) ? "EXON " : "";
			String label = (gene.isStrandPositive()) ? exonString + (i+1) : exonString + (gene.getExonCount()-i);
			int swidth = (int)g.getFontMetrics().getStringBounds(label,g).getWidth();
			int sheight = (int)g.getFontMetrics().getStringBounds(label,g).getHeight();
			g.setColor(Color.WHITE);
			g.setFont(bigFont);
			g.drawString(label, x+(length/2)-(swidth/2), y+(sheight/2));
			g.setFont(baseFont);
			g.setColor(exonColor);
			//g.drawString(""+gene.getExonStart(i), x, y-60);
			//g.drawString(""+gene.getExonEnd(i), x+v+w, y+60);
		}

		private void drawStrandOrientation(Graphics2D g, int x){
			int numTriangles = 3;
			while (intronHeight*numTriangles > intronWidth/2 && numTriangles > 1){
				numTriangles--;
			}
			int ax = x+(intronWidth-intronHeight*numTriangles)/2;
			int bx = ax+intronHeight;
			int cx = bx;
			if( gene.isStrandPositive()){
				ax += intronHeight;
				bx -= intronHeight;
				cx -= intronHeight;
			}
			int ay = getHeight()/2;
			int by = ay-(intronHeight/2);
			int cy = ay+(intronHeight/2);
			g.setColor(Color.WHITE);
			for (int i=0 ; i < numTriangles ; i++){
				g.fillPolygon(new int[]{ax,bx,cx}, new int[]{ay,by,cy}, 3);
				ax +=intronHeight;
				bx +=intronHeight;
				cx +=intronHeight;
			}
		}

		public int getMaxPosPerPix(){
			int width = getParent().getWidth();
			if (width < 10) {
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				width = screenSize.width;
			}
			intronWidth = 20+pix;
			int widthAvForExons = width - ((gene.getExonCount()+1)*intronWidth) - 40;
			int totalExonWidth = 0;
			for (int i=0 ; i < gene.getExonCount() ; i++){
				totalExonWidth += (gene.getExonLength(i))*pix;
			}
			return (int)Math.ceil((double)totalExonWidth / (double)widthAvForExons);
		}

		@Override
		public void paintComponent(Graphics g1) {

			if (scale < 1) scale = 1;

			Graphics2D g = (Graphics2D)g1;

			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			g.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);

			Dimension dim = this.getSize();
			int width = dim.width;
			int height = dim.height;

			if(gene.getEnsemblTranscript() == null){
				//NO gene info has been retreived
				return;
			}

			intronWidth = 20+(Math.max(1, 20-scale)*pix);
			int totalExonWidth = 0;
			for (int i=0 ; i < gene.getExonCount() ; i++){
				totalExonWidth += (gene.getExonLength(i))*pix;
			}
			posPerPix = scale;
			int widthAvForExons = (int)Math.ceil((double)totalExonWidth / (double)posPerPix);
			width = widthAvForExons + ((gene.getExonCount()+1)*intronWidth) + 40;
			int tooSmallExonAddition = 0;
			for (int i=0 ; i < gene.getExonCount() ; i++){
				if (gene.isExonInLeftUTR(i) || gene.isExonInRightUTR(i)){
					if (gene.getCodingLength(i) > 0 && ((gene.getCodingLength(i)/posPerPix) * pix) < minExonWidth){
						tooSmallExonAddition += minExonWidth - ((gene.getCodingLength(i)/posPerPix) * pix);
					}
				}else{
					if (((gene.getExonLength(i)/posPerPix) * pix) < minExonWidth){
						tooSmallExonAddition += minExonWidth - ((gene.getExonLength(i)/posPerPix) * pix);
					}
				}
			}
			width +=  tooSmallExonAddition;
			if (width < getParent().getWidth()){
				width = getParent().getWidth();
				intronWidth = 20+pix;
				widthAvForExons = width - ((gene.getExonCount()+1)*intronWidth) - 40;
				widthAvForExons -= tooSmallExonAddition;
				totalExonWidth = 0;
				for (int i=0 ; i < gene.getExonCount() ; i++){
					totalExonWidth += (gene.getExonLength(i))*pix;
				}
				posPerPix = (int)Math.ceil((double)totalExonWidth / (double)widthAvForExons);				
			}

			variantHeight = (getParent().getHeight() - exonHeight)/2 - (15*2);

			setSize(new Dimension(width, height));
			setPreferredSize(new Dimension(width, 0));

			baseFont = g.getFont();
			baseFont = baseFont.deriveFont(Font.PLAIN, baseFont.getSize()+2);
			Font baseBoldFont = baseFont.deriveFont(Font.BOLD, baseFont.getSize());;
			bigFont = baseFont.deriveFont(Font.BOLD, baseFont.getSize()+2);
			g.setFont(baseFont);

			pixExt.clear();
			pixHl.clear();

			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);

			int x = 20;
			int y = height/2;

			//Draw first intron
			g.setColor(new Color(exonColor.getRed()+40, exonColor.getGreen()+40, exonColor.getBlue()+40));
			g.fillRect(x, y-(intronHeight/2), intronWidth, intronHeight);
			drawStrandOrientation(g, x);
			x += intronWidth;

			JTable table = tables.get(gene.getGeneSymbol());
			BurdenTestTableModel model = (BurdenTestTableModel)table.getModel();

			for (int i=0 ; i < gene.getExonCount() ; i++){
				int v,w,u;
				if (gene.isExonInLeftUTR(i) && gene.isExonInRightUTR(i)){
					v = Math.max((gene.getLeftUTRNonCodingLength(i)/posPerPix) * pix, minExonWidth);
					w = (gene.getCodingLength(i) > 0) ? Math.max((gene.getCodingLength(i)/posPerPix) * pix, minExonWidth) : 0;
					u = Math.max((gene.getRightUTRNonCodingLength(i)/posPerPix) * pix, minExonWidth);					
				}else if (gene.isExonInLeftUTR(i)){
					v = Math.max((gene.getLeftUTRNonCodingLength(i)/posPerPix) * pix, minExonWidth);
					w = (gene.getCodingLength(i) > 0) ? Math.max((gene.getCodingLength(i)/posPerPix) * pix, minExonWidth) : 0;
					u = 0;
				}else if (gene.isExonInRightUTR(i)) {
					v = Math.max((gene.getRightUTRNonCodingLength(i)/posPerPix) * pix, minExonWidth);
					w = (gene.getCodingLength(i) > 0) ? Math.max((gene.getCodingLength(i)/posPerPix) * pix, minExonWidth) : 0;					
					u = 0;
				}else{
					v = 0;
					w = Math.max((gene.getExonLength(i)/posPerPix) * pix, minExonWidth);
					u = 0;
				}
				if (gene.isExonInLeftUTR(i) && gene.isExonInRightUTR(i)){
					g.setColor(new Color(exonColor.getRed()+20, exonColor.getGreen()+20, exonColor.getBlue()+20));
					g.fillRect(x, y-(utrHeight/2), v+1, utrHeight);
					g.setColor(exonColor);
					g.fillRoundRect(x+v, y-(exonHeight/2), w, exonHeight, 20 , 20);
					if (w > 0) drawExonNumber(g, i, x+v, y, w);
					else drawExonNumber(g, i, x, y, v+1);
					g.setColor(new Color(exonColor.getRed()+20, exonColor.getGreen()+20, exonColor.getBlue()+20));
					g.fillRect(x+w+v-1, y-(utrHeight/2), u+1, utrHeight);										
				}else if (gene.isExonInLeftUTR(i)){
					g.setColor(new Color(exonColor.getRed()+20, exonColor.getGreen()+20, exonColor.getBlue()+20));
					g.fillRect(x, y-(utrHeight/2), v+1, utrHeight);
					g.setColor(exonColor);
					g.fillRoundRect(x+v, y-(exonHeight/2), w, exonHeight, 20 , 20);
					if (w > 0) drawExonNumber(g, i, x+v, y, w);
					else drawExonNumber(g, i, x, y, v+1);
				}else if (gene.isExonInRightUTR(i)){
					g.setColor(new Color(exonColor.getRed()+20, exonColor.getGreen()+20, exonColor.getBlue()+20));
					g.fillRect(x+w-1, y-(utrHeight/2), v+1, utrHeight);					
					g.setColor(exonColor);
					g.fillRoundRect(x, y-(exonHeight/2), w, exonHeight, 20 , 20);
					if (w > 0) drawExonNumber(g, i, x, y, w);
					else drawExonNumber(g, i, x+w-1, y, v+1);
				}else{
					g.setColor(exonColor);
					g.fillRoundRect(x, y-(exonHeight/2), w, exonHeight, 20 , 20);
					drawExonNumber(g, i, x, y, w);
				}

				//Save pix for External variants included in this exon
				for (Variant var : extVariants.get(gene.getGeneSymbol()).keySet()){
					int pos = var.getPosition();
					if (pos >= gene.getExonStart(i)-Gene.SPLICE_SITE_EXTENDS && pos <= gene.getExonEnd(i)+Gene.SPLICE_SITE_EXTENDS){
						int xv = x + (pos - gene.getExonStart(i))/posPerPix*pix + 1;		
						if (!pixExt.containsKey(xv)){
							pixExt.put(xv, new TreeSet<Variant>());
						}
						pixExt.get(xv).add(var);
					}
				}
				g.setColor(exonColor);

				//Save pix for Highlander variants included in this exon
				for (Variant var : hlVariants.get(gene.getGeneSymbol()).keySet()){
					int pos = var.getPosition();
					if (pos >= gene.getExonStart(i)-Gene.SPLICE_SITE_EXTENDS && pos <= gene.getExonEnd(i)+Gene.SPLICE_SITE_EXTENDS){
						int xv = x + (pos-gene.getExonStart(i))/posPerPix*pix + 1;
						if (!pixHl.containsKey(xv)){
							pixHl.put(xv, new TreeSet<Variant>());
						}
						pixHl.get(xv).add(var);
					}
				}
				g.setColor(exonColor);

				//Draw following intron
				x += v+w+u;
				g.setColor(new Color(exonColor.getRed()+40, exonColor.getGreen()+40, exonColor.getBlue()+40));
				g.fillRect(x, y-(intronHeight/2), intronWidth, intronHeight);
				drawStrandOrientation(g, x);
				x += intronWidth;
			}			

			//Draw external variants included in this exon
			for (int xv : pixExt.keySet()){
				int hv = variantHeight / pixExt.get(xv).size();
				int count=0;
				for (Variant var : pixExt.get(xv)){
					int yv = height/2 - (exonHeight/2) - 10 - (hv*(count+1));
					double freq = (double)extVariants.get(gene.getGeneSymbol()).get(var) / (double)extTotal;
					g.setColor(getVariantColor(freq*100.0));
					g.fillRoundRect(xv, yv, variantWidth, hv-2, 10 , 10);
					for (int row : table.getSelectedRows()){
						if (model.getSource(table.convertRowIndexToModel(row)) == source && model.getPosition(table.convertRowIndexToModel(row)) == var.getPosition()){
							g.setColor(selectionColor);
							g.setStroke(boldStroke);
							g.drawRoundRect(xv-2, yv-2, variantWidth+3, hv-2+3, 10 , 10);
							g.setStroke(basicStroke);
						}
					}
					count++;
				}
			}

			//Draw Highlander variants included in this exon
			for (int xv : pixHl.keySet()){
				int hv = variantHeight / pixHl.get(xv).size();
				int count=0;
				for (Variant var : pixHl.get(xv)){
					int yv = height/2 + (exonHeight/2) + 10 + (hv*count);
					double freq = (double)hlVariants.get(gene.getGeneSymbol()).get(var) / (double)hlTotal;
					g.setColor(getVariantColor(freq*100.0));
					g.fillRoundRect(xv, yv, variantWidth, hv-2, 10 , 10);
					for (int row : table.getSelectedRows()){
						if (model.getSource(table.convertRowIndexToModel(row)) == Source.HIGHLANDER && model.getPosition(table.convertRowIndexToModel(row)) == var.getPosition()){
							g.setColor(selectionColor);
							g.setStroke(boldStroke);
							g.drawRoundRect(xv-2, yv-2, variantWidth+3, hv-2+3, 10 , 10);
							g.setStroke(basicStroke);
						}
					}
					count++;
				}
			}

			//Draw selection rectangle
			if (startDragSelection != null && endDragSelection != null){
				int startX = Math.min(startDragSelection.x,endDragSelection.x);
				int startY = Math.min(startDragSelection.y,endDragSelection.y);
				int selectionWidth = Math.abs(startDragSelection.x-endDragSelection.x);
				int selectionHeight = Math.abs(startDragSelection.y-endDragSelection.y);
				g.setColor(new Color(selectionColor.getRed(), selectionColor.getGreen(), selectionColor.getBlue(), 100));
				g.fillRect(startX, startY, selectionWidth, selectionHeight);
				g.setColor(selectionColor);				
				g.drawRect(startX, startY, selectionWidth, selectionHeight);
				String label = getExtVariationsBetween(startDragSelection, endDragSelection).size() + " "+source+" variants";
				int sheight = (int)g.getFontMetrics().getStringBounds(label,g).getHeight();
				g.setFont(baseBoldFont);
				g.setColor(Color.DARK_GRAY);
				g.drawString(label, startX+4, startY+sheight);
				label = getHighlanderVariationsBetween(startDragSelection, endDragSelection).size() + " Highlander variants";
				g.drawString(label, startX+4, Math.max(startDragSelection.y,endDragSelection.y)-3);
			}
		}
	}

	public void chiSquare(Gene gene, Set<VariantType> variantTypes){
		try{
			int obsHlMut = 0;
			int obsHlNonMut = 0;			
			int obsExtMut = 0;
			int obsExtNonMut = 0;
			for (int ac : hlVariants.get(gene.getGeneSymbol()).values()){
				obsHlMut += ac;
			}
			for (int ac : extVariants.get(gene.getGeneSymbol()).values()){
				obsExtMut += ac;
			}
			//All mutations in External for selected area
			try (Results res = Highlander.getDB().select(sourceSchema, "SELECT SUM(ac) FROM chromosome_"+gene.getChromosome()+
					" WHERE gene_symbol = '"+gene.getGeneSymbol()+"' AND variant_type IN ("+HighlanderDatabase.makeSqlList(variantTypes, VariantType.class)+") AND "+gene.getExonicWhereClause("chromosome_"+gene.getChromosome()))){
				if (res.next()){
					obsExtNonMut = res.getInt(1) - obsExtMut;
				}
			}
			//All mutations in Highlander for selected area
			Set<String> samples = filter.getSamples();
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			StringBuilder query = new StringBuilder();
			//TODO BURDEN - peut-être utiliser la nouvelle table allele frequencies ?
			query.append("SELECT zygosity, COUNT(*) FROM "+analysis);
			query.append(" WHERE gene_symbol = '"+gene.getGeneSymbol()+"' AND "+analysis+".variant_type IN ("+HighlanderDatabase.makeSqlList(variantTypes, VariantType.class)+") " +
					"AND "+gene.getExonicWhereClause(analysis.toString())+" AND sample IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+")");
			query.append(" GROUP BY zygosity");
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString())) {
				while (res.next()){
					obsHlNonMut += res.getInt(2);
					if (Zygosity.valueOf(res.getString(1)) == Zygosity.Homozygous){
						obsHlNonMut += res.getInt(2);
					}
				}
			}
			obsHlNonMut -= obsHlMut;

			//Chi square computation
			int[][] observed = new int[2][2];
			observed[0][0] = obsHlMut;
			observed[0][1] = obsHlNonMut;
			observed[1][0] = obsExtMut;
			observed[1][1] = obsExtNonMut;
			int[] rowN = new int[observed.length];
			int[] colN = new int[observed[0].length];
			int N = 0;
			for (int i=0 ; i < rowN.length ; i++){
				for (int j=0 ; j < observed[i].length ; j++){
					rowN[i] += observed[i][j];
					colN[j] += observed[i][j];
					N += observed[i][j];
				}
			}
			double[][] expected = new double[2][2];
			for (int i=0 ; i < expected.length ; i++){
				for (int j=0 ; j < expected[i].length ; j++){
					expected[i][j] = rowN[i] * colN[j] / (double)N;
				}
			}
			double chi = 0.0;
			for (int i=0 ; i < observed.length ; i++){
				for (int j=0 ; j < observed[i].length ; j++){
					chi += Math.pow(observed[i][j]-expected[i][j], 2) / expected[i][j];
				}
			}		
			int freedom = (rowN.length-1) * (colN.length-1);
			double pval = Probability.chiSquareComplemented(freedom, chi);

			Object[][] output = new Object[7][4];
			output[0][1] = "Pass filters";
			output[0][2] = "Don't pass";
			output[0][3] = "Total";
			output[1][0] = "Highlander";
			output[1][1] = obsHlMut;
			output[1][2] = obsHlNonMut;
			output[1][3] = obsHlMut+obsHlNonMut;
			output[2][0] = source;
			output[2][1] = obsExtMut;
			output[2][2] = obsExtNonMut;
			output[2][3] = obsExtMut+obsExtNonMut;
			output[3][0] = "Total";
			output[3][1] = obsHlMut+obsExtMut;
			output[3][2] = obsHlNonMut+obsExtNonMut;
			output[3][3] = obsHlMut+obsExtMut+obsHlNonMut+obsExtNonMut;
			output[5][0] = "Chi-square";
			output[5][1] = Tools.doubleToString(chi, 3, true);
			output[6][0] = "p-value";
			output[6][1] = Tools.doubleToString(pval, 3, true);
			JTable jtable = new JTable(output, new String[]{"","","",""});
			jtable.setTableHeader(null);
			JOptionPane.showMessageDialog(this, jtable, "Chi Square", JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iChiSquare,64));
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Error computing Chi²", ex), "Chi Square", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));			
		}
	}

	public void exportChart(){
		String gene = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
		BurdenChart chart = charts.get(gene);
		Object format = JOptionPane.showInputDialog(this, "Choose an image format: ", "Export chart to image file", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iExportJpeg, 64), ImageIO.getWriterFileSuffixes(), "png");
		if (format != null){
			FileDialog chooser = new FileDialog(this, "Export chart to image", FileDialog.SAVE) ;
			chooser.setFile(Tools.formatFilename(gene + "." + format));
			Tools.centerWindow(chooser, false);
			chooser.setVisible(true) ;
			if (chooser.getFile() != null) {
				try {
					String filename = chooser.getDirectory() + chooser.getFile();
					if (!filename.toLowerCase().endsWith("."+format.toString())) filename += "."+format.toString();      
					BufferedImage image = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics2D g = image.createGraphics();
					chart.paintComponent(g);
					ImageIO.write(image, format.toString(), new File(filename));
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(this, Tools.getMessage("Error when exporting chart", ex), "Export chart to image file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));  			}
			}   		
		}
	}

	public void export(){
		try{
			FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
			Tools.centerWindow(chooser, false);
			chooser.setVisible(true) ;
			if (chooser.getFile() != null) {
				String filename = chooser.getDirectory() + chooser.getFile();
				if (!filename.endsWith(".xlsx")) filename += ".xlsx";
				File xls = new File(filename);
				waitingPanel.start();
				try{
					try(Workbook wb = new SXSSFWorkbook(100)){
						DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
						for (String gene : genes.keySet()){
							Sheet sheet = wb.createSheet(gene);
							sheet.createFreezePane(0, 1);		
							int r = 0;
							Row row = sheet.createRow(r++);
							row.setHeightInPoints(50);
							JTable table = tables.get(gene);
							BurdenTestTableModel model = (BurdenTestTableModel)table.getModel();
							CellStyle cs = sheet.getWorkbook().createCellStyle();
							cs.setWrapText(true);
							int[] alignments = new int[table.getColumnCount()];
							for (int c = 0 ; c < table.getColumnCount() ; c++){
								Cell cell = row.createCell(c);
								cell.setCellValue(table.getColumnName(c));
								setCellComment(row.getCell(c), model.headers[table.convertColumnIndexToModel(c)].getDescriptionAndSource());
								sheet.setColumnWidth(c, Math.min(model.headers[table.convertColumnIndexToModel(c)].getSize()*32, 250*128));
								alignments[c] = model.getColumnAlignment(c);
								cell.setCellStyle(cs);
							}
							sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
							int nrow = table.getRowCount();
							waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" variants", false);
							waitingPanel.setProgressMaximum(nrow);
							Highlander.getCellRenderer().clearCellStyles();
							for (int i=0 ; i < nrow ; i++ ){
								waitingPanel.setProgressValue(r);
								row = sheet.createRow(r++);
								for (int c = 0 ; c < table.getColumnCount() ; c++){
									Cell cell = row.createCell(c);
									if (table.getValueAt(i, c) != null){
										Field field = ((VariantsTableModel)table.getModel()).getColumnField(c);
										Highlander.getCellRenderer().formatXlsCell(table.getValueAt(i, c), field, alignments[c], sheet, cell, i);
										if (table.getColumnClass(c) == OffsetDateTime.class){
											cell.setCellValue(((OffsetDateTime)table.getValueAt(i, c)).toLocalDateTime());
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
							}
							waitingPanel.setProgressValue(nrow);		
						}

						Sheet sheetFilt = wb.createSheet("Filters details");
						int r = 0;
						Row	row = sheetFilt.createRow(r++);
						Cell cell = row.createCell(0);
						cell = row.createCell(0);
						cell.setCellValue(filter.toString());

						row = sheetFilt.createRow(r++);
						row = sheetFilt.createRow(r++);
						cell = row.createCell(0);

						cell.setCellValue("Generated with Burden test tool of Highlander version " + Highlander.version + " by " + Highlander.getLoggedUser() + " ("+df.format(System.currentTimeMillis())+")");

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
			}
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

	private static void setCellComment(Cell cell, String cellComment){
		CreationHelper factory = cell.getSheet().getWorkbook().getCreationHelper();
		Drawing<?> drawing = cell.getSheet().createDrawingPatriarch();
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

}
