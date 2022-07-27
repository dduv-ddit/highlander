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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.broad.igv.DirectoryManager;
import org.broad.igv.exceptions.DataLoadException;
//import org.broad.igv.ui.IGVAccess;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.HttpUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel.ColorBy;
import be.uclouvain.ngs.highlander.UI.table.MultiLineTableCellRenderer;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.tools.ViewBam;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.seekablestream.SeekableBufferedStream;
import net.sf.samtools.seekablestream.SeekableFTPStream;
import net.sf.samtools.SAMFileReader.ValidationStringency;

public class BamViewer extends JFrame {

	public final static int A = 0;
	public final static int C = 1;
	public final static int G = 2;
	public final static int T = 3;
	public final static int N = 4;

	public static final long oneDay = 24 * 60 * 60 * 1000;

	private Reference reference;
	private Map<AnalysisFull, Set<String>> bams;
	private Set<Interval> positions;
	private Map<Interval, List<String>> allHeaders;
	private Map<Interval, Object[][]> allData;

	private JTabbedPane tabbedPane;
	private BamViewerTableModel tableModel;
	private Map<Interval, JTable> tables = new TreeMap<Interval, JTable>();
	private Map<Interval, TableRowSorter<BamViewerTableModel>> sorters = new TreeMap<Interval, TableRowSorter<BamViewerTableModel>>();

	static Hashtable<String, File> indexFileCache = new Hashtable<String, File>();
	static private WaitingPanel waitingPanel;

	public BamViewer(Reference reference, Map<AnalysisFull, Set<String>> selectedBAM, Set<Interval> positions){
		super();
		this.reference = reference;
		this.bams = selectedBAM;
		this.positions = positions;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						fillTables();				
					}
				}, "BamViewer.fillTables").start();
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
		setTitle("BAM Viewer");
		setIconImage(Resources.getScaledIcon(Resources.iBamViewer, 64).getImage());

		setLayout(new BorderLayout());

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton checkBam = new JButton(Resources.getScaledIcon(Resources.iBamChecker, 40));
		checkBam.setPreferredSize(new Dimension(54,54));
		checkBam.setToolTipText("Check ALL selected variants in ALL selected BAM files");
		checkBam.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						bamCheck();
					}
				}, "BamViewer.bamCheck").start();
			}
		});
		panel.add(checkBam);

		JButton showInIGV = new JButton(Resources.getScaledIcon(Resources.iIGV, 40));
		showInIGV.setPreferredSize(new Dimension(54,54));
		showInIGV.setToolTipText("View selected variant in IGV");
		showInIGV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewInIGV();
					}
				}, "BamViewer.viewInIGV").start();
			}
		});
		panel.add(showInIGV);
		/*
		 * Only work with IGV in new Frame
		 * 
	  JButton posInIGV = new JButton(Resources.getScaledIcon(Resources.iIGVpos, 40));
	  posInIGV.setPreferredSize(new Dimension(54,54));
	  posInIGV.setToolTipText("View selected position in IGV");
	  posInIGV.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				if (!IGVAccess.isPresent()){
	  					viewInIGV();
	  				}else{
	  					try {
	  						String variant = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
	  						IGVAccess.setPosition(variant.split(":")[0], Integer.parseInt(variant.split(":")[1]));
	  					} catch (Exception ex) {
	  						Tools.exception(ex);
	  						JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive position for selected variant", ex), "View selected position in IGV",
	  								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
	  					}
	  				}
	  			}
	  		}, "BamViewer.posInIGV").start();
	  	}
	  });
	  panel.add(posInIGV);
		 */
		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 40));
		export.setPreferredSize(new Dimension(54,54));
		export.setToolTipText("Export all tabs in one Excel file (1 sheet per tab)");
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						export();
					}
				}, "BamViewer.export").start();
			}
		});
		panel.add(export);

		tabbedPane = new JTabbedPane();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		for (Interval pos : positions){
			JScrollPane scrollPane = new JScrollPane();
			tabbedPane.addTab(pos.toString(), scrollPane);

			JTable table = new JTable() {
				protected JTableHeader createDefaultTableHeader() {
					return new JTableHeader(columnModel) {
						public String getToolTipText(MouseEvent e) {
							java.awt.Point p = e.getPoint();
							int index = columnModel.getColumnIndexAtX(p.x);
							if (index >= 0){
								int realIndex = columnModel.getColumn(index).getModelIndex();
								TableModel model = table.getModel();
								if (model instanceof BamViewerTableModel) {
									StringBuilder sb = new StringBuilder();
									sb.append("<html>");
									sb.append("<b>"+((BamViewerTableModel)table.getModel()).getColumnName(realIndex) + "</b><br>");
									double mean = ((BamViewerTableModel)table.getModel()).getMean(realIndex);
									double sd = ((BamViewerTableModel)table.getModel()).getSD(realIndex);
									if (mean != -1.0 && sd != -1.0) {
										sb.append("Mean of alt proportion = " + Tools.doubleToPercent(mean, 0) + "<br>");
										sb.append("SD of alt proportion = " + Tools.doubleToPercent(sd, 0));
									}
									return sb.toString();
								}else {
									return "";
								}
							}else{
								return null;
							}
						}
					};
				}
			};
			table.createDefaultColumnsFromModel();
			table.getTableHeader().setReorderingAllowed(false);
			table.getTableHeader().setResizingAllowed(true);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

			table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());
			table.setDefaultRenderer(Integer.class, new ColoredTableCellRenderer());
			table.setDefaultRenderer(Double.class, new ColoredTableCellRenderer());
			scrollPane.setViewportView(table);
			tables.put(pos, table);
		}

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}	

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			String rowname = (column == 1) ? table.getValueAt(row,0).toString() : "field";
			int alignment = (column == 0) ? JLabel.LEFT : JLabel.CENTER;
			if (table.getModel().getColumnClass(column) == Double.class) {
				value = Tools.doubleToString(Double.parseDouble(value.toString()), 2, false);
			}
			Field field = Field.getField(rowname);
			return Highlander.getCellRenderer().renderCell(label, value, field, alignment, row, isSelected, Resources.getTableEvenRowBackgroundColor(Palette.Red), Color.WHITE, false);
		}
	}

	private class MultilineColoredTableCellRenderer extends MultiLineTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JTextArea textArea = (JTextArea) comp;
			if (row%2 == 0) textArea.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Blue));
			else textArea.setBackground(Color.WHITE);
			textArea.setForeground(Color.black);
			textArea.setBorder(new LineBorder(Color.WHITE));
			if (isSelected) {
				textArea.setBackground(new Color(51,153,255));
			}
			return textArea;
		}
	}

	//Include computation of z-scores for each alternative allele
	public static class BamViewerTableModel	extends AbstractTableModel {
		private Object[][] data;
		private String[] headers;
		private double[] means;
		private double[] sds;
		private final int nh;
		private final int iTotal;
		private final int iRef;

		public BamViewerTableModel(Object[][] input, String[] inputHeaders, int numHeaderWithoutZScore, int indexColTotalReads, int indexColRef) {
			nh = numHeaderWithoutZScore;
			iRef = indexColRef;
			iTotal = indexColTotalReads;
			headers = new String[(inputHeaders.length*2)-nh];
			int j=0;
			for (int i=0 ; i < inputHeaders.length ; i++) {
				headers[j++] = inputHeaders[i];
				if (i >= nh) {
					headers[j++] = "z_score_" + inputHeaders[i];
				}
			}
			data = new Object[input.length][headers.length];

			means = new double[headers.length];
			sds = new double[headers.length];
			java.util.Arrays.fill(means, -1.0);
			java.util.Arrays.fill(sds, -1.0);

			j=0;
			for (int col=0 ; col < inputHeaders.length ; col++) {
				for (int row=0 ; row < input.length ; row++) {
					data[row][j] = input[row][col];
					if (col >= nh) {
						if ((int)input[row][iTotal] > 0 && (int)input[row][iRef]+(int)input[row][col] > 0)
							data[row][j+1] = ((Integer)input[row][col]).doubleValue() / (((Integer)input[row][iRef]).doubleValue() + ((Integer)input[row][col]).doubleValue()); // Changing #reads to proportion
						else
							data[row][j+1] = 0.0;
					}
				}
				if (col >= nh) {
					//Computation of the mean
					double mean = 0;
					for (int r=0 ; r < input.length ; r++) {
						mean += (double)data[r][j+1];
					}
					mean /= input.length;
					means[j] = mean;
					//Computation of standard deviation
					double sd =0;
					for (int r=0 ; r < input.length ; r++) {
						sd += Math.pow((double)data[r][j+1] - mean, 2);
					}
					sd /= input.length;
					sd = Math.sqrt(sd);
					sds[j] = sd;
					//Computation of z-score					
					for (int r=0 ; r < input.length ; r++) {
						if (sd > 0)
							data[r][j+1] = ((double)data[r][j+1] - mean) / sd;
						else 
							data[r][j+1] = 0.0;
					}
				}
				//increasing local index
				j++;
				if (col >= nh) j++;
			}
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

		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == iTotal) return Integer.class;
			if (columnIndex == iRef) return Integer.class;
			if (columnIndex < nh) return String.class;
			if ((columnIndex-nh) % 2 == 0) return Integer.class;
			else return Double.class;
		}

		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		public double getMean(int col) {
			return means[col];
		}

		public double getSD(int col) {
			return sds[col];
		}

		public void setValueAt(Object value, int row, int col) {
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}

	private void fillTables(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			int numBam = 0;
			for (Set<String> set : bams.values()){
				numBam += set.size();
			}
			final int maxBam = numBam;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setProgressString("Checking "+maxBam+" BAM files", false);
					waitingPanel.setProgressMaximum(maxBam);
				}
			});

			if (!launchDistantBamView()) launchLocalBamView();

			Set<Interval> errors = new TreeSet<>();
			for (Iterator<Interval> it = positions.iterator() ; it.hasNext() ; ){
				Interval pos = it.next();
				List<String> headers = new ArrayList<>(allHeaders.get(pos));
				String ref = pos.getReferenceSequence();
				Object[][] data = allData.get(pos);
				if (headers.remove(ref)) {
					headers.add(4, ref +" (ref)");
					tableModel = new BamViewerTableModel(data, headers.toArray(new String[0]), 5, 3, 4);
					TableRowSorter<BamViewerTableModel> sorter = new TableRowSorter<BamViewerTableModel>(tableModel);
					sorters.put(pos, sorter);
					tables.get(pos).setModel(tableModel);		
					tables.get(pos).setRowSorter(sorter);

					//Set the column width to the max between every cell content and header
					for (int i=0 ; i < tables.get(pos).getColumnCount() ; i++){
						int width = 0;
						for (int r = 0; r < tables.get(pos).getRowCount(); r++) {
							TableCellRenderer renderer = tables.get(pos).getCellRenderer(r, i);
							Component comp = tables.get(pos).prepareRenderer(renderer, r, i);
							width = Math.max (comp.getPreferredSize().width, width);
						}
						TableColumn column = tables.get(pos).getColumnModel().getColumn(i);
						TableCellRenderer headerRenderer = column.getHeaderRenderer();
						if (headerRenderer == null) {
							headerRenderer = tables.get(pos).getTableHeader().getDefaultRenderer();
						}
						Object headerValue = column.getHeaderValue();
						Component headerComp = headerRenderer.getTableCellRendererComponent(tables.get(pos), headerValue, false, false, 0, i);
						width = Math.max(width, headerComp.getPreferredSize().width);
						column.setPreferredWidth(width + 20);
					}
				}else {
					tables.remove(pos);
					it.remove();
					errors.add(pos);
				}
			}
			if (!errors.isEmpty()) {
				System.err.println("Cannot retreive reference for positions:\n"+errors+"\nThose positions have been removed from the tables.\nCheck if positions really exists on the chromosome.");
				JOptionPane.showMessageDialog(new JFrame(),  "Cannot retreive reference for positions:\n"+errors+"\nThose positions have been removed from the tables.\nCheck if positions really exists on the chromosome.", "BamViewer",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive position for selected variant", ex), "BamViewer",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	private void launchLocalBamView() throws Exception {
		allHeaders = new TreeMap<>();
		allData = new TreeMap<>();
		Map<Interval,Map<Analysis,Map<String,Map<String,Integer>>>> intervals = new TreeMap<Interval,Map<Analysis,Map<String,Map<String,Integer>>>>();
		String NREADS="#reads";
		int count = 0;
		for (AnalysisFull analysis : bams.keySet()){
			for (String sample : bams.get(analysis)){
				waitingPanel.setProgressValue(++count);
				try{
					URL url = new URL(analysis.getBamURL(sample));
					if (Tools.exists(url.toString())){
						SAMFileReader samfr = (url.toString().startsWith("ftp")) ? new SAMFileReader(new SeekableBufferedStream(new SeekableFTPStream(url)), getIndexFile(url, null), false) : new SAMFileReader(url, getIndexFile(url, null), false);
						samfr.setValidationStringency(ValidationStringency.SILENT);
						for (Interval pos : positions){
							if (!intervals.containsKey(pos)){
								intervals.put(pos, new TreeMap<Analysis, Map<String,Map<String,Integer>>>());
							}
							Map<Analysis, Map<String,Map<String,Integer>>> analyses = intervals.get(pos);
							if (!analyses.containsKey(analysis)){
								analyses.put(analysis, new TreeMap<String,Map<String,Integer>>());
							}
							Map<String,Map<String,Integer>> samples = analyses.get(analysis); 
							if (!samples.containsKey(sample)){
								Map<String, Integer> patterns = new HashMap<String, Integer>();
								if(pos.getSize() == 1){
									patterns.put("A", 0);
									patterns.put("C", 0);
									patterns.put("G", 0);
									patterns.put("T", 0);
								}
								samples.put(sample, patterns);
							}
							Map<String,Integer> patterns = samples.get(sample);								
							SAMRecordIterator it =  samfr.query(pos.getChromosome(samfr.getFileHeader()), pos.getStart(), pos.getEnd(), false);
							int total = 0;
							while(it.hasNext()){									
								//Check if the interval falls completely inside the read, drop it if not									
								Optional<String> optionalPattern = ViewBam.getPattern(it.next(), pos.getStart(), pos.getEnd());
								if (optionalPattern.isPresent()){
									String pattern = optionalPattern.get();
									if (!patterns.containsKey(pattern)){
										patterns.put(pattern, 0);
									}
									patterns.put(pattern, patterns.get(pattern)+1);
									total++;
								}
							}
							it.close();
							patterns.put(NREADS, total);
						}
						samfr.close();
					}
				}catch(Exception ex){
					System.err.println("BamViewer: problem with sample " + sample + " in analysis " + analysis);
					ex.printStackTrace();
				}
			}
		}
		waitingPanel.setProgressDone();

		for (Interval pos : positions){
			Map<String,Integer> totalPatterns = new HashMap<String,Integer>();
			Map<Analysis, Map<String,Map<String,Integer>>> analyses = intervals.get(pos);
			int totalSamples = 0;
			for (Analysis analysis : analyses.keySet()) {
				Map<String,Map<String,Integer>> samples = analyses.get(analysis);
				totalSamples += samples.size();
				for (Map<String,Integer> patterns : samples.values()){
					for (String pattern : patterns.keySet()){
						if (!totalPatterns.containsKey(pattern)){
							totalPatterns.put(pattern, 0);
						}
						totalPatterns.put(pattern, totalPatterns.get(pattern)+patterns.get(pattern));						
					}
				}
			}
			List<String> headers = new ArrayList<String>();
			for (String pattern : totalPatterns.keySet()){
				int i = 0;
				while (!pattern.equals(NREADS) && i < headers.size() && totalPatterns.get(headers.get(i)) > totalPatterns.get(pattern)){
					i++;
				}
				headers.add(i, pattern);
			}				
			String ref = pos.getReferenceSequence();
			if (headers.remove(ref)) {
				headers.add(1, ref);
			}
			headers.add(0, "Analysis");
			headers.add(1, "BAM");
			headers.add(2, "Pathology");
			Object[][] data = new Object[totalSamples][headers.size()];
			int row = 0;
			for (Analysis analysis : analyses.keySet()) {
				Map<String,Map<String,Integer>> samples = analyses.get(analysis);
				for (String sample : samples.keySet()){
					int col = 0;
					for (String pattern : headers){
						if (pattern.equals("BAM")){
							data[row][col] = sample;
						}else if (pattern.equals("Analysis")) {
							data[row][col] = analysis;
						}else if (pattern.equals("Pathology")) {
							String pathology = "UNKNOWN";
							try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT pathology FROM projects JOIN pathologies USING (pathology_id) WHERE sample = '"+sample+"'")) {
								if (res.next()) {
									pathology = res.getString(1);
								}
							}
							data[row][col] = pathology;
						}else if (samples.get(sample).containsKey(pattern)){
							data[row][col] = samples.get(sample).get(pattern);
						}else{
							data[row][col] = 0;
						}
						col++;
					}
					row++;
				}
			}
			allHeaders.put(pos, headers);
			allData.put(pos, data);
		}
	}

	private boolean launchDistantBamView() {
		allHeaders = new TreeMap<>();
		allData = new TreeMap<>();
		StringBuilder posString = new StringBuilder();
		for (Interval pos : positions){
			posString.append(pos.toString());
			posString.append(";");
		}
		if (posString.length() > 0) {
			posString.deleteCharAt(posString.length()-1);
		}else {
			return false;
		}
		StringBuilder sampleString = new StringBuilder();
		AnalysisFull lastAnalysis = null;
		String lastSample = null;
		for (AnalysisFull analysis : bams.keySet()){
			lastAnalysis = analysis;
			for (String sample : bams.get(analysis)){
				lastSample = sample;
				sampleString.append(analysis.toString());
				sampleString.append("|");
				sampleString.append(sample);
				sampleString.append(";");
			}
		}
		if (sampleString.length() > 0) {
			sampleString.deleteCharAt(sampleString.length()-1);
		}else {
			return false;
		}
		String filename = posString.toString() + "@" + sampleString.toString();
		filename = Integer.toString(Math.abs(filename.hashCode()));
		try{
			URL url = new URL(lastAnalysis.getBamURL(lastSample).replaceAll(lastAnalysis.toString(), "bamout").replace(lastSample+".bam", filename));
			if (!Tools.exists(url.toString())){
				waitingPanel.setProgressString("Launching BamCheck on server", true);
				if (Highlander.getParameters().getUrlForPhpScripts() == null) {
					System.err.println("Distant BamView impossible: you should configure 'server > php' parameter in settings.xml");
					return false;
				}
				int max = sampleString.toString().split(";").length;
				int count = 0;
				//launch script server side
				HttpClient httpClient = new HttpClient();
				boolean bypass = false;
				if (System.getProperty("http.nonProxyHosts") != null) {
					for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
						if ((Highlander.getParameters().getUrlForPhpScripts()+"/bamcheck.php").toLowerCase().contains(host.toLowerCase())) bypass = true;
					}
				}
				if (!bypass && System.getProperty("http.proxyHost") != null) {
					try {
						HostConfiguration hostConfiguration = httpClient.getHostConfiguration();
						hostConfiguration.setProxy(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")));
						httpClient.setHostConfiguration(hostConfiguration);
						if (System.getProperty("http.proxyUser") != null && System.getProperty("http.proxyPassword") != null) {
							// Credentials credentials = new UsernamePasswordCredentials(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword"));
							// Windows proxy needs specific credentials with domain ... if proxy user is in the form of domain\\user, consider it's windows
							String user = System.getProperty("http.proxyUser");
							Credentials credentials;
							if (user.contains("\\")) {
								credentials = new NTCredentials(user.split("\\\\")[1], System.getProperty("http.proxyPassword"), System.getProperty("http.proxyHost"), user.split("\\\\")[0]);
							}else {
								credentials = new UsernamePasswordCredentials(user, System.getProperty("http.proxyPassword"));
							}
							httpClient.getState().setProxyCredentials(null, System.getProperty("http.proxyHost"), credentials);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				PostMethod post = new PostMethod(Highlander.getParameters().getUrlForPhpScripts()+"/bamcheck.php");
				NameValuePair[] data = {
						new NameValuePair("filename", filename),
						new NameValuePair("patients", "\""+sampleString.toString()+"\""),
						new NameValuePair("positions", "\""+posString.toString()+"\"")
				};
				post.addParameters(data);
				if (httpClient.executeMethod(post) == 200) {			
					//try (BufferedReader br = new BufferedReader(new InputStreamReader(post.getResponseBodyAsStream()))){
					InputStreamReader isr = new InputStreamReader(post.getResponseBodyAsStream());
					boolean firstPoint = true;
					boolean firstPlus = true;
					int value = 0;
					StringBuilder sb = new StringBuilder();
					while(((value = isr.read()) != -1)) {
						char c = (char)value;
						if (c != '#') { //workaround for PHP buffer filling
							//System.out.print(c);
							if (c == '.') {
								if (firstPoint) {
									waitingPanel.setProgressMaximum(max);
									waitingPanel.setProgressValue(0);
									waitingPanel.setProgressString("Accessing "+max+" BAM files", false);
									count = 0;
									firstPoint = false;
								}
								waitingPanel.setProgressValue(++count);
							}else if (c == '!') {
								waitingPanel.setProgressString("BAM available, sending positions", true);
							}else if (c == '+') {
								if (firstPlus) {
									waitingPanel.setProgressMaximum(max);
									waitingPanel.setProgressValue(0);
									waitingPanel.setProgressString("Reading "+positions.size()+" positions in "+max+" BAM", false);
									count = 0;
									firstPlus = false;
								}
								waitingPanel.setProgressValue(++count);
							}else if (c == '\n') {
								waitingPanel.setProgressString(sb.toString(), true);
								sb.setLength(0);
							}else if (c == '*') {
								sb.setLength(0);
								while(((value = isr.read()) != -1)) {
									c = (char)value;
									if (c == '*') {
										String[] output = sb.toString().split("\\^");
										if (output[0].equals("cmd")) {
											System.out.println(output[1]);
										}else if (output[0].equals("exitcode")) {
											int exitStatus = Integer.parseInt(output[1]);
											System.out.println("exit-status: "+exitStatus);
											if (exitStatus != 0) return false;
										}
										sb.setLength(0);
										break;
									}else {
										sb.append(c);									
									}
								}
							}else{
								sb.append(c);
							}
						}
					}
				}else {
					return false;
				}
			}
			while (!Tools.exists(url.toString())){
				try{
					Thread.sleep(1000);
				}catch (InterruptedException ex){
					Tools.exception(ex);
				}
			}
			String results = Tools.httpGet(url.toString());
			String[] parsePositions = results.trim().split("####");
			for (String position : parsePositions) {
				String[] parse = position.split("##");
				Interval pos = new Interval(reference, parse[0].trim());
				List<String> headers = Arrays.asList(parse[1].trim().split("\t"));
				String[] rows = parse[2].trim().split("\n");
				Object[][] data = new Object[rows.length][headers.size()];
				for (int i=0 ; i < rows.length ; i++) {
					String[] cols = rows[i].split("\t");
					for (int j=0 ; j < cols.length ; j++) {
						if (j < 3) data[i][j] = cols[j];
						else data[i][j] = Integer.parseInt(cols[j]);
					}
				}
				allHeaders.put(pos, headers);
				allData.put(pos, data);
			}
			return true;
		}catch(Exception ex) {
			ex.printStackTrace();
			return false;			
		}
	}

	static public int getReadCount(AnalysisFull analysis, String sample, Interval pos) {
		String NREADS="#reads";
		Map<String, Integer> patterns = new HashMap<String, Integer>();
		if(pos.getSize() == 1){
			patterns.put("A", 0);
			patterns.put("C", 0);
			patterns.put("G", 0);
			patterns.put("T", 0);
		}
		patterns.put(NREADS, 0);
		try{
			URL url = new URL(analysis.getBamURL(sample));
			if (Tools.exists(url.toString())){
				SAMFileReader samfr = (url.toString().startsWith("ftp")) ? new SAMFileReader(new SeekableBufferedStream(new SeekableFTPStream(url)), getIndexFile(url, null), false) : new SAMFileReader(url, getIndexFile(url, null), false);
				samfr.setValidationStringency(ValidationStringency.SILENT);
				SAMRecordIterator it =  samfr.query(pos.getChromosome(samfr.getFileHeader()), pos.getStart(), pos.getEnd(), false);
				int total = 0;
				while(it.hasNext()){									
					//Check if the interval falls completely inside the read, drop it if not									
					Optional<String> optionalPattern = ViewBam.getPattern(it.next(), pos.getStart(), pos.getEnd());
					if (optionalPattern.isPresent()){
						String pattern = optionalPattern.get();
						if (!patterns.containsKey(pattern)){
							patterns.put(pattern, 0);
						}
						patterns.put(pattern, patterns.get(pattern)+1);
						total++;
					}
				}
				it.close();
				patterns.put(NREADS, total);
				samfr.close();
			}
		}catch(Exception ex){
			System.err.println("BamViewer: problem with sample " + sample + " in analysis " + analysis);
			ex.printStackTrace();
		}
		return patterns.get(NREADS);
	}

	static File getIndexFile(URL url, String indexPath) throws IOException {

		String urlString = url.toString();
		File indexFile = getTmpIndexFile(urlString);

		// Crude staleness check -- if more than a day old discard
		long age = System.currentTimeMillis() - indexFile.lastModified();
		if (age > oneDay) {
			indexFile.delete();
		}

		if (!indexFile.exists() || indexFile.length() < 1) {
			loadIndexFile(urlString, indexPath, indexFile);
			indexFile.deleteOnExit();
		}

		return indexFile;

	}

	private static File getTmpIndexFile(String bamURL) throws IOException {
		File indexFile = indexFileCache.get(bamURL);
		if (indexFile == null) {
			indexFile = File.createTempFile("index_", ".bai", DirectoryManager.getCacheDirectory());
			indexFile.deleteOnExit();
			indexFileCache.put(bamURL, indexFile);
		}
		return indexFile;
	}

	private static void loadIndexFile(String path, String indexPath, File indexFile) throws IOException {
		InputStream is = null;
		OutputStream os = null;

		try {
			String idx = (indexPath != null && indexPath.length() > 0) ? indexPath : path + ".bai";
			URL indexURL = new URL(idx);
			os = new FileOutputStream(indexFile);
			try {
				is = HttpUtils.getInstance().openConnectionStream(indexURL);
			} catch (FileNotFoundException e) {
				// Try other index convention
				String baseName = path.substring(0, path.length() - 4);
				indexURL = new URL(baseName + ".bai");

				try {
					is = org.broad.igv.util.HttpUtils.getInstance().openConnectionStream(indexURL);
				} catch (FileNotFoundException e1) {
					MessageUtils.showMessage("Index file not found for file: " + path);
					throw new DataLoadException("Index file not found for file: " + path, path);
				}
			}
			byte[] buf = new byte[512000];
			int bytesRead;
			while ((bytesRead = is.read(buf)) != -1) {
				os.write(buf, 0, bytesRead);
			}

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace(); 
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace(); 
				}
			}

		}
	}

	public void bamCheck(){
		Object res = JOptionPane.showInputDialog(new JFrame(),  "Minimum number of reads under the position", "BAM Checker",
				JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iBamViewer,64), null, null);
		if (res != null){
			int minReads = Integer.parseInt(res.toString());
			res = JOptionPane.showInputDialog(new JFrame(),  "Minimum number of pattern (nucleotides) of interest", "BAM Checker",
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iBamViewer,64), null, null);
			if (res != null){
				int minAlt = Integer.parseInt(res.toString());
				int nrows = 0;
				for (Interval pos : positions){
					JTable table = tables.get(pos);
					nrows += 1+(table.getColumnCount()-((BamViewerTableModel)table.getModel()).nh)/2;
				}
				Object[][] data = new Object[nrows][4];
				String[] headers = new String[]{"Position","Pattern","#bams","bams"};
				int row = 0;
				for (Interval pos : positions){
					JTable table = tables.get(pos);
					int iRef = ((BamViewerTableModel)table.getModel()).iRef;
					int iTotal = ((BamViewerTableModel)table.getModel()).iTotal;
					for (int tcol=iRef ; tcol < table.getColumnCount() ; tcol++){
						data[row][0] = pos.toString();
						data[row][1] = table.getColumnName(tcol);
						int num = 0;
						StringBuilder bams = new StringBuilder();
						for (int i=0 ; i < table.getRowCount() ; i++){
							if (Integer.parseInt(table.getValueAt(i, iTotal).toString()) >= minReads){
								if (Integer.parseInt(table.getValueAt(i, tcol).toString()) >= minAlt){
									num++;
									bams.append(table.getValueAt(i, 0) + "." + table.getValueAt(i, 1) + " ["+table.getValueAt(i, tcol)+"/"+table.getValueAt(i, iTotal)+"], ");
								}
							}
						}
						if (bams.length() > 0) bams.delete(bams.length()-2, bams.length());
						data[row][2] = num;
						data[row][3] = bams.toString();
						row++;					
						if (tcol > iRef) tcol++;
					}
				}	
				final String title = "BAM Checker ("+minReads+" reads & "+minAlt+" pattern of interest minimum)";
				final JTable table = new JTable(data, headers);
				table.setDefaultRenderer(Object.class, new MultilineColoredTableCellRenderer());
				table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
				for (int i=0 ; i < table.getColumnCount()-1 ; i++){
					if (table.getRowCount() > 0){
						TableColumn tc = table.getColumnModel().getColumn(i);
						TableCellRenderer renderer = tc.getHeaderRenderer();
						if (renderer == null) {
							renderer = table.getTableHeader().getDefaultRenderer();
						}
						int maxWidth = renderer.getTableCellRendererComponent(table, tc.getHeaderValue(),false, false, 0, 0).getPreferredSize().width+5;
						for (int r = 0 ; r < table.getRowCount() ; r++){
							JTextArea textArea = (JTextArea)(table.getCellRenderer(r, i).getTableCellRendererComponent(table, table.getValueAt(r, i),false, false, r, i));
							maxWidth = Math.max(maxWidth, new JLabel(textArea.getText()).getPreferredSize().width + 15);
						}
						tc.setPreferredWidth(maxWidth);
						tc.setMaxWidth(maxWidth);
					}
				}
				/*
				for (int i=0 ; i < table.getColumnCount() ; i++){
					int width = 0;
					for (int r = 0; r < table.getRowCount(); r++) {
						TableCellRenderer renderer = table.getCellRenderer(r, i);
						Component comp = table.prepareRenderer(renderer, r, i);
						width = Math.max (comp.getPreferredSize().width, width);
					}
					table.getColumnModel().getColumn(i).setPreferredWidth(Math.max(width+20, (i>0)?80:150));
				}
				 */
				JScrollPane scroll = new JScrollPane(table);
				JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 40));
				export.setPreferredSize(new Dimension(54,54));
				export.setToolTipText("Export this BamCheck and all BamViewer tabs in one Excel file (1 sheet per tab)");
				export.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						new Thread(new Runnable(){
							public void run(){
								export(title, table);
							}
						}, "BamViewer.export").start();
					}
				});
				JFrame frame = new JFrame(title);
				frame.setIconImage(Resources.getScaledIcon(Resources.iBamChecker, 16).getImage());
				frame.getContentPane().add(scroll, BorderLayout.CENTER);				
				frame.getContentPane().add(export, BorderLayout.SOUTH);				
				frame.pack();
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				frame.setSize(new Dimension(screenSize.width/2, screenSize.height/3*2));
				Tools.centerWindow(frame, false);
				frame.setVisible(true);
			}
		}
	}

	public void viewInIGV(){
		List<String> variants = new ArrayList<String>();
		String variant = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
		variants.add(variant);
		Map<String, AnalysisFull> samples = new HashMap<String, AnalysisFull>();
		for (Interval key : tables.keySet()){
			if (key.toString().equals(variant)){
				JTable table = tables.get(key);
				for (int row : table.getSelectedRows()){
					samples.put(table.getValueAt(row, 1).toString(), Highlander.getAnalysis(table.getValueAt(row, 0).toString()));
				}
			}
		}
		OpenIGV oigv = new OpenIGV(variants, samples);
		Tools.centerWindow(oigv, false);
		oigv.setVisible(true);
	}

	public void export(){
		export(null,null);
	}

	public void export(String checkParam, JTable checkTable){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile(Tools.formatFilename("BAM viewer " + positions.size() + " positions.xlsx"));
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
					int totalRows = 0;
					if (checkParam != null){
						JTable table = checkTable;
						Sheet sheet = wb.createSheet(checkParam);
						sheet.createFreezePane(0, 1);		
						int r = 0;
						Row row = sheet.createRow(r++);
						row.setHeightInPoints(50);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							row.createCell(c).setCellValue(table.getColumnName(c));
						}
						sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
						int nrow = table.getRowCount();
						waitingPanel.setProgressString("Exporting "+checkParam, false);
						waitingPanel.setProgressMaximum(nrow);
						for (int i=0 ; i < nrow ; i++ ){
							waitingPanel.setProgressValue(r);
							row = sheet.createRow(r++);
							for (int c = 0 ; c < table.getColumnCount() ; c++){
								if (table.getValueAt(i, c) == null)
									row.createCell(c);
								else if (table.getColumnClass(c) == Timestamp.class)
									row.createCell(c).setCellValue((Timestamp)table.getValueAt(i, c));
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
					}
					for (Interval pos : positions){
						JTable table = tables.get(pos);
						Sheet sheet = wb.createSheet(pos.toString().replace(':', '-'));
						sheet.createFreezePane(0, 1);		
						int r = 0;
						Row row = sheet.createRow(r++);
						row.setHeightInPoints(50);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							row.createCell(c).setCellValue(table.getColumnName(c));
						}
						int nrow = table.getRowCount();
						if (nrow > 0) {
							sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
							waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" variants for " + pos, false);
							waitingPanel.setProgressMaximum(nrow);
							for (int i=0 ; i < nrow ; i++ ){
								waitingPanel.setProgressValue(r);
								row = sheet.createRow(r++);
								for (int c = 0 ; c < table.getColumnCount() ; c++){
									if (table.getValueAt(i, c) == null)
										row.createCell(c);
									else if (table.getColumnClass(c) == Timestamp.class)
										row.createCell(c).setCellValue((Timestamp)table.getValueAt(i, c));
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
							row = sheet.createRow(r++);
							row = sheet.createRow(r++);
							for (int c = 0 ; c < table.getColumnCount() ; c++){
								if (c == 0) {
									row.createCell(c).setCellValue("Mean of alt proportion");
								}else {
									double mean = ((BamViewerTableModel)table.getModel()).getMean(c);
									if (mean != -1.0) {
										row.createCell(c).setCellValue(mean);
									}
								}
							}
							row = sheet.createRow(r++);
							for (int c = 0 ; c < table.getColumnCount() ; c++){
								if (c == 0) {
									row.createCell(c).setCellValue("SD of alt proportion");
								}else {
									double sd = ((BamViewerTableModel)table.getModel()).getSD(c);
									if (sd != -1.0) {
										row.createCell(c).setCellValue(sd);
									}
								}
							}
							totalRows += 3;
						}
					}
					waitingPanel.setProgressValue(totalRows);
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

	public static AlignmentPanel getAlignmentPanel(AnalysisFull analysis, String sample, Interval interval, int width, JProgressBar progress) throws Exception {
		return getAlignmentPanel(analysis, sample, interval, null, false, false, false, ColorBy.STRAND, true, width, progress);
	}

	public static AlignmentPanel getAlignmentPanel(AnalysisFull analysis, String sample, Interval interval, Variant highlightedVariant, boolean showSoftClippedBases, boolean squished, boolean frameShift, ColorBy colorBy, boolean drawReference, int width, JProgressBar progress) throws Exception {
		URL url = new URL(analysis.getBamURL(sample));
		return getAlignmentPanel(url, interval, highlightedVariant, showSoftClippedBases, squished, frameShift, colorBy, drawReference, width, progress);
	}

	public static AlignmentPanel getAlignmentPanel(URL url, Interval interval, Variant highlightedVariant, boolean showSoftClippedBases, boolean squished, boolean frameShift, ColorBy colorBy, boolean drawReference, int width, JProgressBar progress) throws Exception {
		if (Tools.exists(url.toString())){
			SAMFileReader samfr = (url.toString().startsWith("ftp")) ? new SAMFileReader(new SeekableBufferedStream(new SeekableFTPStream(url)), getIndexFile(url, null), false) : new SAMFileReader(url, getIndexFile(url, null), false);
			samfr.setValidationStringency(ValidationStringency.SILENT);
			progress.setIndeterminate(true);
			progress.setString("Retrieving reads in interval");
			progress.setStringPainted(true);
			SAMRecordIterator it =  samfr.query(interval.getChromosome(samfr.getFileHeader()), interval.getStart(), interval.getEnd(), false);
			AlignmentPanel alignmentPanel = new AlignmentPanel(it, interval, highlightedVariant, showSoftClippedBases, squished, frameShift, colorBy, drawReference, width, progress);
			it.close();
			samfr.close();		
			return alignmentPanel;
		}else{
			throw new Exception("URL '"+url.toString()+"' does not exist or is inaccessible");
		}
	}

}
