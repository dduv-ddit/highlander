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
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.table.HeatMap;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ColorRange;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ConversionMethod;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public class PedigreeChecker extends JFrame {

	private final String[] dbsnpVersions = new String[]{"dbSNP with gnomAD (exomes) af > 5%"};

	private Map<String,Map<String, Set<String>>> dbsnp = new TreeMap<String, Map<String,Set<String>>>();
	private Map<String,Map<String, ImageIcon>> sex = new TreeMap<String, Map<String,ImageIcon>>();
	private Map<String,Map<String, Double>> ratio = new TreeMap<String, Map<String,Double>>();
	private Map<String,int[][]> totalsAdj = new TreeMap<String, int[][]>();
	private Map<String,int[][]> totals = new TreeMap<String, int[][]>();
	private final Set<String> samples;
	private Map<String, PedigreeTableModel> models = new TreeMap<String, PedigreeTableModel>();
	private Map<String, JTable> tables = new TreeMap<String, JTable>();
	private Map<String, PedigreeTableModel> modelsAdj = new TreeMap<String, PedigreeTableModel>();
	private Map<String, JTable> tablesAdj = new TreeMap<String, JTable>();
	private Map<String, HeatMap> heatMapsAdj = new TreeMap<String, HeatMap>();
	private JTabbedPane tabbedPane;

	private ImageIcon iUnknown = Resources.getScaledIcon(Resources.iHelp, 16);
	private ImageIcon iFemale = Resources.getScaledIcon(Resources.iPedigreeFemale, 16);
	private ImageIcon iMale = Resources.getScaledIcon(Resources.iPedigreeMale, 16);

	static private WaitingPanel waitingPanel;

	private Analysis analysis = Highlander.getCurrentAnalysis();

	public PedigreeChecker(Set<String> samples){
		super();
		this.samples = new TreeSet<String>(samples);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (int)(screenSize.width*0.05);
		int height = screenSize.height - (int)(screenSize.height*0.05);
		setSize(new Dimension(width,height));
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						checkSamples();				
					}
				}, "PedigreeChecker.checkSamples").start();
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
		setTitle("Pedigree checker");
		setIconImage(Resources.getScaledIcon(Resources.iPedigreeChecker, 64).getImage());

		setLayout(new BorderLayout());

		JPanel panelTop = new JPanel(new BorderLayout());
		int height = 150;
		int width = (int)(2500.0 / (1250.0/(double)height));
		panelTop.add(new JLabel(new ImageIcon(Resources.iPedigreeCheckerCommon.getImage().getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH))), BorderLayout.WEST);
		JTextArea explanation = new JTextArea();
		explanation.append("This tool will give you some insight about the snp shared between 2 samples.\n");
		explanation.append("It allows you to guage the degree of relatedness between pairs of sampled individuals and to determine sex.\n");
		explanation.append("Its goal is to help you check for sample mix-ups, it is not meant to accurately determine relatedness.\n");
		explanation.append("\n");
		explanation.append("Note that gender is determined using heterozygous/homozygous variants ratio on chromosome X.\n");
		explanation.append("It means that a 'wrong' gender could be infered if the sample has an het/hom imbalance, due for example if 2 dna's were mixed together.\n");
		explanation.append("\n");
		explanation.append("For the 'Common' tab, when comparing 2 samples S1 and S2, we first take all variants from S1 and S2 having a dbSNP id, an allele frequency in gnomAD (exomes) > 5% and a read depth > 10.\n");
		explanation.append("The total (number between parenthesis in the table) is the sum of snps unique to S1, unique to S2 and common between S1 and S2 (so the snps common are only counted ONCE).\n");
		explanation.append("The percentage reflects then the number of snps common to S1 and S2. ");
		explanation.append("See figure on the LEFT.\n");
		explanation.append("\n");
		explanation.append("For the 'Adjusted' tabs, when comparing 2 samples S1 and S2, we first take all variants from S1 and S2 having a dbSNP id, an allele frequency in gnomAD (exomes) > 5% and a read depth > 10.\n");
		explanation.append("The total (number between parenthesis in the table) is the sum of snps found in S1 and the snps found in S2 (so the snps common are counted TWICE).\n");
		explanation.append("The percentage reflects then TWICE the number of snps common to S1 and S2. ");
		explanation.append("See figure on the RIGHT.\n");
		explanation.append("\n");
		explanation.append("'Common' gives you real numbers: percentage of snps common between 2 samples, from a real total number of snps.\n");
		explanation.append("The problem is that this percentage is generally low, mainly because 'homozygous reference' genotypes are not taken into account and no IBD model is used.\n");
		explanation.append("'Adjusted dbSNP' gives you an abstract % and total (intersection counted twice), but it mitigates errors and gives better estimation of similarity between 2 samples.\n");
		explanation.setEditable(false);
		explanation.setLineWrap(true);
		JScrollPane explScroll = new JScrollPane(explanation);
		explScroll.setPreferredSize(new Dimension(100, 170));
		panelTop.add(explScroll, BorderLayout.CENTER);
		panelTop.add(new JLabel(new ImageIcon(Resources.iPedigreeCheckerAdjusted.getImage().getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH))), BorderLayout.EAST);
		getContentPane().add(panelTop, BorderLayout.NORTH);

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 40));
		export.setPreferredSize(new Dimension(54,54));
		export.setToolTipText("Export to an Excel file");
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						export();
					}
				}, "PedigreeChecker.export").start();
			}
		});
		panel.add(export);

		tabbedPane = new JTabbedPane();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		for (final String version : dbsnpVersions){
			dbsnp.put(version, new TreeMap<String, Set<String>>());
			sex.put(version, new TreeMap<String, ImageIcon>());
			ratio.put(version, new TreeMap<String, Double>());

			JScrollPane scrollPaneAdj = new JScrollPane();
			JTable tableAdj = new JTable(){
				@Override
				public String getToolTipText(MouseEvent e) {
					java.awt.Point p = e.getPoint();
					int rowIndex = rowAtPoint(p);
					int colIndex = columnAtPoint(p);
					Object val = getValueAt(rowIndex, colIndex);
					if (val == null) {
						return null;
					}else if (val instanceof ImageIcon) {
						Double d = ratio.get(version).get(getValueAt(rowIndex,0));
						String gender = (d > 2.5) ? "male" : ((d < 2) ? "female" : "unknown - e.g. XXY");
						return "Chr X hom/het ratio = " + Tools.doubleToString(d, 2, false) + " ("+gender+")";
					}else if (val instanceof Double) {
						return getValueAt(rowIndex,0) + " + " + getColumnName(colIndex) + ": " + Tools.doubleToPercent((Double)val, 2);
					}else {
						return val.toString();
					}
				}
			};
			tableAdj.setDefaultRenderer(String.class, new ColoredTableCellRenderer(true, version));
			tableAdj.setDefaultRenderer(Double.class, new ColoredTableCellRenderer(true, version));
			scrollPaneAdj.setViewportView(tableAdj);
			tablesAdj.put(version, tableAdj);
			tabbedPane.addTab("Adjusted " + version, scrollPaneAdj);

			JScrollPane scrollPane = new JScrollPane();
			JTable table = new JTable(){
				@Override
				public String getToolTipText(MouseEvent e) {
					java.awt.Point p = e.getPoint();
					int rowIndex = rowAtPoint(p);
					int colIndex = columnAtPoint(p);
					Object val = getValueAt(rowIndex, colIndex);
					if (val == null) {
						return null;
					}else if (val instanceof ImageIcon) {
						Double d = ratio.get(version).get(getValueAt(rowIndex,0));
						String gender = (d > 2.5) ? "male" : ((d < 2) ? "female" : "unknown - e.g. XXY");
						return "Chr X hom/het ratio = " + Tools.doubleToString(d, 2, false) + " ("+gender+")";
					}else if (val instanceof Double) {
						return getValueAt(rowIndex,0) + " + " + getColumnName(colIndex) + ": " + Tools.doubleToPercent((Double)val, 2);
					}else {
						return val.toString();
					}
				}
			};
			table.setDefaultRenderer(String.class, new ColoredTableCellRenderer(false, version));
			table.setDefaultRenderer(Double.class, new ColoredTableCellRenderer(false, version));
			scrollPane.setViewportView(table);
			tables.put(version, table);
			tabbedPane.addTab("Common " + version, scrollPane);			
		}

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	private void checkSamples(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setProgressString("Checking SNP for "+samples.size()+" samples", false);
					waitingPanel.setProgressMaximum(samples.size()*2*dbsnpVersions.length);
				}
			});

			int count = 0;
			for (String version : dbsnpVersions){
				for (String sample : samples){
					waitingPanel.setProgressValue(++count);
					String query = 
							"SELECT MIN(`"+Field.dbsnp_id.getName()+"`) as "+Field.dbsnp_id.getName()+" "
									+ "FROM "+analysis.getFromSampleAnnotations()
									+ analysis.getJoinStaticAnnotations()
									+ analysis.getJoinProjects()
									+	"WHERE "+Field.sample.getQueryWhereName(analysis, false)+" = '"+sample+"' "
									+ "AND "+Field.dbsnp_id.getQueryWhereName(analysis, false)+" IS NOT NULL "
									+ "AND "+Field.gnomad_wes_af.getQueryWhereName(analysis, false)+" >= 0.05 "
									+ "AND "+Field.read_depth.getQueryWhereName(analysis, false)+" > 10 " 
									+ "GROUP BY `pos`,`chr`,`alternative`,`reference`,`length`"
									;
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query)) {
						Set<String> set = new HashSet<String>();
						while (res.next()){
							set.add(res.getString(1));
						}
						dbsnp.get(version).put(sample, set);
					}
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
							"SELECT "+Field.zygosity.getQuerySelectName(analysis, false)+", COUNT(*) "
									+ "FROM "+analysis.getFromSampleAnnotations()
									+ analysis.getJoinStaticAnnotations()
									+ analysis.getJoinProjects()
									+ "WHERE "+Field.sample.getQueryWhereName(analysis, false)+" = '"+sample+"' "
									+ "AND "+Field.chr.getQueryWhereName(analysis, false)+" = 'X' "
									+ "AND "+Field.gnomad_wes_af.getQueryWhereName(analysis, false)+" > 0.2 "
									+ "AND "+Field.read_depth.getQueryWhereName(analysis, false)+" > 10 "
									+ "GROUP BY `pos`,`chr`,`alternative`,`reference`,`length`,`zygosity`"
							)){
						int x=1;
						int y=0;
						while (res.next()){
							if (res.getString(1).equalsIgnoreCase("Heterozygous")){
								x += res.getInt(2);
							}else if (res.getString(1).equalsIgnoreCase("Homozygous")){
								y += res.getInt(2);
							}
						}
						if (x == 0){
							//System.out.println(sample + " : zero heterozygous !!");
							sex.get(version).put(sample, iUnknown);	
							ratio.get(version).put(sample, Double.NaN);	
						}else{
							double r = (double)y/(double)x;
							ratio.get(version).put(sample, r);
							//System.out.println(sample + " : " + ratio);
							if (r < 2){
								//female
								//note that some ethnicities with a lot of heterozygous polymorphisms compared to the reference can pose problem, female having a ratio around 3
								sex.get(version).put(sample, iFemale);
							}else if (r > 2.5){
								//male
								sex.get(version).put(sample, iMale);
							}else{
								//grey zone (like XXY)

								sex.get(version).put(sample, iUnknown);
							}
						}
					}
				}

				Object[][] data = new Object[samples.size()][samples.size()+2];
				Object[][] dataAdj = new Object[samples.size()][samples.size()+2];
				int[][] totalAdj = new int[samples.size()][samples.size()+2];
				totalsAdj.put(version, totalAdj);
				int[][] total = new int[samples.size()][samples.size()+2];
				totals.put(version, total);
				int i=0;

				for (String sample : dbsnp.get(version).keySet()){
					waitingPanel.setProgressValue(++count);
					data[i][0] = sample;
					dataAdj[i][0] = sample;
					data[i][1] = sex.get(version).get(sample);
					dataAdj[i][1] = sex.get(version).get(sample);
					int j=0;
					Set<String> snps = dbsnp.get(version).get(sample);
					for (String p : samples){
						Set<String> set = new HashSet<String>(snps);
						set.retainAll(dbsnp.get(version).get(p));
						int t = snps.size() + dbsnp.get(version).get(p).size() - set.size();
						int tadj = snps.size() + dbsnp.get(version).get(p).size();
						double percent = (double)set.size() / (double)t;
						double percentAdj = (double)set.size()*2.0 / (double)tadj;
						data[j][i+2] = percent;
						dataAdj[j][i+2] = percentAdj;
						total[j][i+2] = t;
						totalAdj[j][i+2] = tadj;
						j++;
					}
					i++;
				}					

				String[] cols = new String[samples.size()+2];
				Class<?>[] classes = new Class<?>[samples.size()+2];
				cols[0] = "";
				classes[0] = String.class;
				cols[1] = "Gender";
				classes[1] = ImageIcon.class;
				int c=2;
				for (String p : dbsnp.get(version).keySet()){
					cols[c] = p;
					classes[c] = Double.class;
					c++;
				}
				PedigreeTableModel model = new PedigreeTableModel(data, cols, classes);
				models.put(version, model);
				PedigreeTableModel modelAdj = new PedigreeTableModel(dataAdj, cols, classes);
				modelsAdj.put(version, modelAdj);
				tables.get(version).setModel(model);
				tablesAdj.get(version).setModel(modelAdj);
				HeatMap heatMapAdj = new HeatMap(tablesAdj.get(version));
				heatMapsAdj.put(version, heatMapAdj);
				for (int k=2 ; k < cols.length ; k++){
					heatMapAdj.setHeatMap(k, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "0.60", "0.95");
				}
			}
			waitingPanel.setProgressDone();
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Pedigree checker",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		private boolean adj;
		private String version;

		public ColoredTableCellRenderer(boolean adjusted, String version){
			super();
			this.adj = adjusted;
			this.version = version;
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			JLabel label = (JLabel) comp;

			label.setHorizontalAlignment(JLabel.LEFT);

			if (value == null) {
				value = "";
			}else if (table.getColumnClass(column) == Double.class){
				if (adj){
					value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 0) + " ("+Tools.intToString(totalsAdj.get(version)[table.convertRowIndexToModel(row)][table.convertColumnIndexToModel(column)])+")";
				}else{
					value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 0) + " ("+Tools.intToString(totals.get(version)[table.convertRowIndexToModel(row)][table.convertColumnIndexToModel(column)])+" snps)";
				}
			}

			if (row%2 != 0) label.setBackground(new Color(240,240,240));
			else label.setBackground(Color.white);
			label.setForeground(Color.black);
			label.setBorder(new LineBorder(Color.WHITE));
			if (value != null){
				if (adj && table.convertColumnIndexToModel(column) > 1){
					label.setBackground(heatMapsAdj.get(version).getColor(row, column));
				}
				label.setText(value.toString());
			}      
			if (isSelected) {
				label.setBackground(new Color(51,153,255));
			}
			return label;
		}
	}

	public static class PedigreeTableModel	extends AbstractTableModel {
		final private Object[][] data;
		final private String[] headers;
		final private Class<?>[] classes;

		public PedigreeTableModel(Object[][] data, String[] headers, Class<?>[] classes) {    	
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

	}

	public void export(){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile("Pedrigree checker.xlsx");
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
					for (String version : dbsnpVersions){
						for (int a=0 ; a <= 1 ; a++){
							JTable table = (a==0) ? tablesAdj.get(version) : tables.get(version);
							int[][] tot = (a==0) ? totalsAdj.get(version) : totals.get(version);
							Sheet sheet = wb.createSheet((a==0) ? ("Adjusted dbSNP "+version) : ("Common dbSNP " + version));
							sheet.createFreezePane(1, 1);		
							int r = 0;
							Row row = sheet.createRow(r++);
							for (int c = 0 ; c < table.getColumnCount() ; c++){
								row.createCell(c).setCellValue(table.getColumnName(c));
							}
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
										Object value = table.getValueAt(i, c);
										Color color = null;
										if (value != null){
											if (c > 1 && a==0){
												color = heatMapsAdj.get(version).getColor(i, c);
											}
										}  		
										String styleKey  = generateCellStyleKey(color);
										if (!styles.containsKey(styleKey)){
											styles.put(styleKey, createCellStyle(sheet, cell, color));
										}
										cell.setCellStyle(styles.get(styleKey));
										if (table.getColumnClass(c) == Double.class && r > 1){
											value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 0) + " ("+Tools.intToString(tot[r-2][c])+")";
										}else if (table.getColumnClass(c) == ImageIcon.class && r > 1){
											if (value == iUnknown) value = "?";
											else if (value == iMale) value = "M";
											else if (value == iFemale) value = "F";
										}
										cell.setCellValue(value.toString());
									}
								}
							}
							for (int c = 0 ; c < table.getColumnCount() ; c++){
								sheet.autoSizeColumn(c);					
							}
							waitingPanel.setProgressValue(nrow);						
						}
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

	private XSSFCellStyle createCellStyle(Sheet sheet, Cell cell, Color color){
		XSSFCellStyle cs = (XSSFCellStyle)sheet.getWorkbook().createCellStyle();
		if (color != null){
			cs.setFillPattern(CellStyle.SOLID_FOREGROUND);
			cs.setFillForegroundColor(new XSSFColor(color));  		
		}
		return cs;
	}

	private String generateCellStyleKey(Color color){
		StringBuilder sb = new StringBuilder();
		if (color != null) sb.append(color.getRGB());
		return sb.toString();
	}

	/**
	 * Check if this analysis contains samples belonging to the same individual (using common snps > 90%) but with a different individual id, so they can be corrected.
	 * Similarly, reports samples marked as same individual but with less than 90% common snps.
	 * 
	 * @param analysis
	 * @throws Exception
	 */
	public static void checkDuplicateIndividuals(Analysis analysis) throws Exception {
		System.out.println("Getting samples information");
		Map<Integer, Set<String>> variants = new TreeMap<Integer,Set<String>>();
		Map<Integer, String> samples = new TreeMap<>();
		Map<Integer, String> individuals = new TreeMap<>();
		Map<Integer, String> comments = new TreeMap<>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT project_id, sample, individual, comments FROM projects JOIN projects_analyses USING (project_id) WHERE analysis = '"+analysis+"'")) {
			while (res.next()){
				int project_id = res.getInt(1);
				variants.put(project_id, new HashSet<String>());					
				samples.put(project_id, res.getString("sample"));
				individuals.put(project_id, res.getString("individual"));
				comments.put(project_id, res.getString("comments"));
			}
		}
		System.out.println("Getting snps");
		int count = 0;
		for (int project_id : variants.keySet()){
			System.out.println("sample " + (++count) + "/" + variants.size());
			String query = 
					"SELECT MIN(`"+Field.dbsnp_id.getName()+"`) as "+Field.dbsnp_id.getName()+" "
							+ "FROM "+analysis.getFromSampleAnnotations()
							+ analysis.getJoinStaticAnnotations()
							+	"WHERE "+Field.project_id.getQueryWhereName(analysis, false)+" = "+project_id+" "
							+ "AND "+Field.dbsnp_id.getQueryWhereName(analysis, false)+" IS NOT NULL "
							+ "AND "+Field.gnomad_wes_af.getQueryWhereName(analysis, false)+" >= 0.05 "
							+ "AND "+Field.read_depth.getQueryWhereName(analysis, false)+" > 10 " 
							+ "GROUP BY `pos`,`chr`,`alternative`,`reference`,`length`"
							;
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query, true)) {
				while (res.next()){
					variants.get(project_id).add(res.getString(1));
				}
			}
		}
		System.out.println("Computing similarity");
		Map<Integer, Map<Integer, Double>> common = new TreeMap<>();
		System.out.println("SAMPLE 1\tINDIVIDUAL 1\tSAMPLE 2\tINDIVIDUAL 2\tCOMMON SNPs\tHIGHLANDER INDIVIDUAL\tCOMMENTS 1\tCOMMENTS 2");
		for (int project_id_from : variants.keySet()){
			common.put(project_id_from, new TreeMap<>());
			for (int project_id_to : variants.keySet()){
				Set<String> set = new HashSet<String>(variants.get(project_id_from));
				set.retainAll(variants.get(project_id_to));
				int tadj = variants.get(project_id_from).size() + variants.get(project_id_to).size();
				double percentAdj = (double)set.size()*2.0 / (double)tadj;
				common.get(project_id_from).put(project_id_to, percentAdj);
				if (percentAdj > 0.9) {
					if (!individuals.get(project_id_from).equals(individuals.get(project_id_to))) {
						System.out.println(samples.get(project_id_from) + "\t" + individuals.get(project_id_from) + "\t" + samples.get(project_id_to) + "\t" + individuals.get(project_id_to) + "\t" + Tools.doubleToPercent(percentAdj, 0) + "\t" + "DIFFERENT" + "\t" + comments.get(project_id_from) + "\t" + comments.get(project_id_to));
					}
				}else {
					if (individuals.get(project_id_from).equals(individuals.get(project_id_to))) {
						System.out.println(samples.get(project_id_from) + "\t" + individuals.get(project_id_from) + "\t" + samples.get(project_id_to) + "\t" + individuals.get(project_id_to) + "\t" + Tools.doubleToPercent(percentAdj, 0) + "\t" + "SAME" + "\t" + comments.get(project_id_from) + "\t" + comments.get(project_id_to));
					}					
				}
			}
		}
		System.out.println("DONE");
	}
	
	public static void main(String[] args) {
		try {
			Highlander.initialize(new Parameters(false, new File("config\\GEHU admin\\settings.xml")), 5);
			//checkDuplicateIndividuals(new Analysis("panels_hg38"));
			checkDuplicateIndividuals(new Analysis("exomes_hg38"));
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
