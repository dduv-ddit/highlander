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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter;
import be.uclouvain.ngs.highlander.datatype.filter.VariantsCommonToSamples;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter.ComparisonOperator;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.LogicalOperator;
import be.uclouvain.ngs.highlander.datatype.filter.VariantsCommonToSamples.VCSCriterion;
import cern.jet.stat.Probability;

public class CtdnaEstimation extends JFrame {

	private JTable table ;
	private Analysis analysis;
	private Map<String, String> samples;
	static private WaitingPanel waitingPanel;

	public CtdnaEstimation(Analysis analysis, Map<String, String> samples) {
		super();
		this.analysis = analysis;
		this.samples = samples;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (int)(screenSize.width*0.05);
		int height = screenSize.height - (int)(screenSize.height*0.05);
		setSize(new Dimension(width,height));
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						compute();				
					}
				}, "CtdnaEstimation.compute").start();
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
		setTitle("ctDNA estimation");
		setIconImage(Resources.getScaledIcon(Resources.iCTDNA, 64).getImage());

		setLayout(new BorderLayout());

		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		table = new JTable();
		table.createDefaultColumnsFromModel();
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Integer.class, new ColoredTableCellRenderer());
		table.setDefaultRenderer(Double.class, new ColoredTableCellRenderer());
		scrollPane.setViewportView(table);

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 40));
		export.setPreferredSize(new Dimension(54,54));
		export.setToolTipText("Export to an Excel file");
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						export();
					}
				}, "CtdnaEstimation.export").start();
			}
		});
		panel.add(export);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			int alignment = (column == 0 || column == table.getColumnCount()-1) ? SwingConstants.LEFT : SwingConstants.CENTER;
			if (table.getModel().getColumnClass(column) == Double.class) {
				value = Tools.doubleToPercent(Double.parseDouble(value.toString()), 0);
			}
			return Highlander.getCellRenderer().renderCell(label, value, new Field("field"), alignment, row, isSelected, Resources.getTableEvenRowBackgroundColor(Palette.Red), Color.WHITE, false);
		}
	}

	public static class CtdnaTableModel	extends AbstractTableModel {
		final private Object[][] data;
		final private String[] headers;
		final private Class<?>[] classes;

		public CtdnaTableModel(Object[][] data, String[] headers, Class<?>[] classes) {    	
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

	public void compute() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});

		HighlanderDatabase DB = Highlander.getDB();

		String[] genomics = new String[samples.size()];
		String[] plasmas = new String[samples.size()];
		String[] projects = new String[samples.size()];

		int index=0;
		for (String plasma : samples.keySet()) {
			plasmas[index] = plasma;
			genomics[index] = samples.get(plasma);
			try {
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT run_label FROM projects WHERE sample = '"+plasma+"'")) {
					if (res.next()){
						projects[index] = res.getString("run_label");
					}else {
						projects[index] = "unknown_project";
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				projects[index] = "unknown_project";
			}
			index++;
		}

		double alpha = 0.05; //Benjamini-Hochberg correction alpha

		Object[][] data = new Object[genomics.length][9];
		String[] headers = new String[] {
				"Plasma",
				"Purity range from FACETS",
				"ctDNA proportion range",
				"Median",
				"Mean",
				"SD",
				"Num snv passing BH correction",
				"Num snv failing BH correction",
				"List of snv used",
		};
		Class<?>[] classes = new Class<?>[] {
			String.class,
			String.class,
			String.class,
			Double.class,
			Double.class,
			Double.class,
			Integer.class,
			Integer.class,
			String.class,
		};

		for (int row=0 ; row < genomics.length ; row++) {
			try {

				String genomic = genomics[row];
				String plasma = plasmas[row];

				VariantsCommonToSamples vcs = new VariantsCommonToSamples();			
				List<VCSCriterion> VCSCriteria = new ArrayList<>();
				VCSCriterion criterionNormal = vcs.new VCSCriterion();
				criterionNormal.sample = genomic;
				criterionNormal.useZigosity = true;
				criterionNormal.zygosity = "Heterozygous";
				VCSCriteria.add(criterionNormal);
				VCSCriterion criterionTumor = vcs.new VCSCriterion();
				criterionTumor.sample = plasma;
				criterionTumor.useZigosity = true;
				criterionTumor.zygosity = "Any";
				VCSCriteria.add(criterionTumor);

				List<String> f1vals = new ArrayList<>();
				f1vals.add("100");
				Field f1field = null;
				for (Field f : Field.getAvailableFields(analysis, false)) {
					if (f.getName().equals("read_depth")) {
						f1field = f;
					}
				}
				CustomFilter f1 = new CustomFilter(null, f1field, ComparisonOperator.GREATEROREQUAL, false, f1vals, new ArrayList<String>(), true);

				List<String> f2vals = new ArrayList<>();
				f2vals.add("PASS");
				Field f2field = null;
				for (Field f : Field.getAvailableFields(analysis, false)) {
					if (f.getName().equals("filters")) {
						f2field = f;
					}
				}
				CustomFilter f2 = new CustomFilter(null, f2field, ComparisonOperator.EQUAL, false, f2vals, new ArrayList<String>(), true);

				List<String> f3vals = new ArrayList<>();
				f3vals.add("3");
				Field f3field = null;
				for (Field f : Field.getAvailableFields(analysis, false)) {
					if (f.getName().equals("exac_ac")) {
						f3field = f;
					}
				}
				CustomFilter f3 = new CustomFilter(null, f3field, ComparisonOperator.GREATEROREQUAL, false, f3vals, new ArrayList<String>(), false);

				List<String> f4vals = new ArrayList<>();
				f4vals.add("3");
				Field f4field = null;
				for (Field f : Field.getAvailableFields(analysis, false)) {
					if (f.getName().equals("gonl_ac")) {
						f4field = f;
					}
				}
				CustomFilter f4 = new CustomFilter(null, f4field, ComparisonOperator.GREATEROREQUAL, false, f4vals, new ArrayList<String>(), false);

				List<String> f5vals = new ArrayList<>();
				f5vals.add("3");
				Field f5field = null;
				for (Field f : Field.getAvailableFields(analysis, false)) {
					if (f.getName().equals("gnomad_wgs_af")) {
						f5field = f;
					}
				}
				CustomFilter f5 = new CustomFilter(null, f5field, ComparisonOperator.GREATEROREQUAL, false, f5vals, new ArrayList<String>(), false);

				List<String> f6vals = new ArrayList<>();
				f6vals.add(VariantType.SNV.toString());
				Field f6field = null;
				for (Field f : Field.getAvailableFields(analysis, false)) {
					if (f.getName().equals("variant_type")) {
						f6field = f;
					}
				}
				CustomFilter f6 = new CustomFilter(null, f6field, ComparisonOperator.EQUAL, false, f6vals, new ArrayList<String>(), true);

				List<String> f7vals = new ArrayList<>();
				f7vals.add("2");
				Field f7field = null;
				for (Field f : Field.getAvailableFields(analysis, false)) {
					if (f.getName().equals("allele_num")) {
						f7field = f;
					}
				}
				CustomFilter f7 = new CustomFilter(null, f7field, ComparisonOperator.EQUAL, false, f7vals, new ArrayList<String>(), true);

				List<CustomFilter> orFilters = new ArrayList<>();
				orFilters.add(f3);
				orFilters.add(f4);
				orFilters.add(f5);
				CustomFilter orFilter = new CustomFilter(null, LogicalOperator.OR, orFilters);

				List<CustomFilter> andFilters = new ArrayList<>();
				andFilters.add(f1);
				andFilters.add(f2);
				andFilters.add(f6);
				andFilters.add(f7);
				andFilters.add(orFilter);
				CustomFilter andFilter = new CustomFilter(null, LogicalOperator.AND, andFilters);

				ComboFilter prefilter = new ComboFilter(null, andFilter);

				VariantsCommonToSamples filter = new VariantsCommonToSamples(null, VCSCriteria, prefilter);

				Map<Integer, String> results = filter.getResultIds(null);

				Map<Integer, Integer> genomicIdToPasmaId = new HashMap<>();
				Map<Integer, Integer> plasmaIdToGenomicId = new HashMap<>();
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT variant_sample_id "
						+ "FROM " + analysis.getFromSampleAnnotations()
						+ analysis.getJoinProjects()
						+ "WHERE variant_sample_id IN ("+HighlanderDatabase.makeSqlList(results.keySet(), Integer.class)+") "
						+ "AND sample = '"+genomic+"'"
						)) {
					while (res.next()) {
						int idGenomic = res.getInt("variant_sample_id");
						int idPlasma = -1;
						for (int id : results.keySet()) {
							if (id != idGenomic && results.get(id).equals(results.get(idGenomic))) {
								idPlasma = id;
								break;
							}
						}
						if (idPlasma == -1) {
							System.err.println(idGenomic + " - " + results.get(idGenomic) + " was not found in tumor !!! Problem with filter ...");
						}else {
							genomicIdToPasmaId.put(idGenomic,	idPlasma);
							plasmaIdToGenomicId.put(idPlasma, idGenomic);
						}
					}
				}

				Map<Integer, Integer> altNum = new HashMap<>();
				Map<Integer, Integer> refNum = new HashMap<>();
				//System.out.println("sample" + "\t" + "chr" + "\t" + "pos" + "\t" + "reference" + "\t" + "alternative" + "\t" + "filters" + "\t" + "read_depth" + "\t"+ "allelic_depth_ref" + "\t"+ "allelic_depth_alt" + "\t" + "zygosity" + "\t" + "exac_ac" + "\t" + "gonl_ac" + "\t" + "the1000G_AC");
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT variant_sample_id, allelic_depth_alt, allelic_depth_ref "
						+ "FROM " + analysis.getFromSampleAnnotations()
						+ "WHERE variant_sample_id IN ("+HighlanderDatabase.makeSqlList(results.keySet(), Integer.class)+")"
						)) {
					while (res.next()) {
						//System.out.println(res.getString("sample") + "\t" + res.getString("chr") + "\t" + res.getString("pos") + "\t" + res.getString("reference") + "\t" + res.getString("alternative") + "\t" + res.getString("filters") + "\t" + res.getString("read_depth") + "\t" + res.getString("allelic_depth_ref") + "\t" + res.getString("allelic_depth_alt") + "\t" + res.getString("zygosity") + "\t" + res.getString("exac_ac") + "\t" + res.getString("gonl_ac") + "\t" + res.getString("the1000G_AC"));
						int id = res.getInt("variant_sample_id");
						altNum.put(id, res.getInt("allelic_depth_alt"));
						refNum.put(id, res.getInt("allelic_depth_ref"));
					}
				}

				//System.out.println("snv" + "\t" + "alt" + "\t" + "ref" + "\t" + "Min binomial");
				Map<Integer, Double> binomialGenomic = new LinkedHashMap<>();
				for (int id : genomicIdToPasmaId.keySet()) {
					int ref = refNum.get(id);
					int alt = altNum.get(id);
					binomialGenomic.put(id, Math.min(Probability.binomial(alt, alt+ref, 0.5), Probability.binomialComplemented(alt, alt+ref, 0.5)));
					//System.out.println(results.get(id) + "\t" + altNum.get(id) + "\t" + refNum.get(id) + "\t" + binomialNormal.get(id));
				}			
				List<Entry<Integer, Double>> sortedList = new ArrayList<>();
				for (Entry<Integer,Double> e : binomialGenomic.entrySet()) {				
					int pos=0;
					while (pos < sortedList.size() && sortedList.get(pos).getValue() < e.getValue()) pos++;
					sortedList.add(pos, e);
				}
				/*
				for (Entry<Integer,Double> e : sortedList) {
					System.out.println(e.getKey() + "\t" + results.get(e.getKey()) + "\t" + e.getValue());
				}
				 */
				Map<Integer, Double> binomialPlasma = new LinkedHashMap<>();
				//System.out.println("id normal" + "\t" + "snv normal" + "\t" + "alt normal" + "\t" + "ref normal" + "\t" + "id tumor" + "\t" + "snv tumor" + "\t" + "alt tumor" + "\t" + "ref tumor" + "\t" + "binomial tumor");
				//Remove the 10% variants with lowest p-value
				for (int i=sortedList.size()/10 ; i < sortedList.size() ; i++) {
					//System.out.println(sortedList.get(i).getKey() + "\t" + results.get(sortedList.get(i).getKey()) + "\t" + sortedList.get(i).getValue());
					int idGenomic = sortedList.get(i).getKey();
					int idPlasma = genomicIdToPasmaId.get(idGenomic);
					int alt = altNum.get(idPlasma);
					int ref = refNum.get(idPlasma);
					binomialPlasma.put(idPlasma, Math.min(Probability.binomial(alt, alt+ref, 0.5), Probability.binomialComplemented(alt, alt+ref, 0.5)));
					//System.out.println(idNormal + "\t" + results.get(idNormal) + "\t" + altNum.get(idNormal) + "\t" + refNum.get(idNormal) + "\t" + idTumor + "\t" + results.get(idTumor) + "\t" + altNum.get(idTumor) + "\t" + refNum.get(idTumor) + "\t" + binomialTumor.get(idTumor));					
				}

				//Benjamini-Hochberg correction
				sortedList.clear();
				for (Entry<Integer,Double> e : binomialPlasma.entrySet()) {				
					int pos=0;
					while (pos < sortedList.size() && sortedList.get(pos).getValue() < e.getValue()) pos++;
					sortedList.add(pos, e);
				}
				Map<Integer, Double> critical = new LinkedHashMap<>();
				for (int i=0 ; i < sortedList.size() ; i++) {
					int id = sortedList.get(i).getKey();
					double crit = (((i+1) * alpha ) / sortedList.size()); 
					critical.put(id, crit);
				}
				int thresh=0;
				for (double crit : critical.values()) {
					double pval = sortedList.get(thresh).getValue();
					if (pval >= crit) {
						break;
					}
					thresh++;
				}
				Map<Integer,Double> idsHemi = new LinkedHashMap<>();
				Map<Integer,Double> idsDup = new LinkedHashMap<>();
				for (int i=0 ; i < sortedList.size() && i < thresh ; i++) {
					//double pval = sortedList.get(i).getValue(); 
					int id = sortedList.get(i).getKey();
					int alt = altNum.get(id);
					int ref = refNum.get(id);
					//Take major allele frequency
					double prop = (alt > ref) ? (double)alt / (double)(alt + ref) : (double)ref / (double)(alt + ref);
					/*
					String chr = results.get(id).split("-")[0];
					if (chr.equals("X") || chr.equals("Y")) chr = "23";
					int pos = Integer.parseInt(results.get(id).split("-")[1]);
					//Retreive Facet data
					for (String line : Tools.httpGet("http://130.104.74.150/reports/"+projects[p]+"/facets/"+plasma+"/"+plasma+".facets.tsv").split("\n")) {
						if (line.startsWith(chr+"\t")) {
							String[] cols = line.split("\t");
							int rangeStart = Integer.parseInt(cols[9]);
							int rangeEnd = Integer.parseInt(cols[10]);
							if (pos >= rangeStart && pos <= rangeEnd) {
								double totalCN = Double.parseDouble(cols[12]);
								double minorCN = (cols[13].equals("NA")) ? -1.0 : Double.parseDouble(cols[13]);
								double majorCN = totalCN-minorCN;
								break;
							}
						}
					}											
					 */
					double minorCN = -1.0;
					double majorCN = -1.0;
					minorCN = 0.0;
					majorCN = 1.0;
					double magicValue = (1.0 - (2.0 * prop)) / (1.0 - majorCN - (2.0 * prop) + (prop * minorCN) + (prop * majorCN));
					idsHemi.put(id,magicValue);
					//Impossible to have AF > 66% and a duplication
					if (prop < 0.66) {
						minorCN = 1.0;
						majorCN = 2.0;
						magicValue = (1.0 - (2.0 * prop)) / (1.0 - majorCN - (2.0 * prop) + (prop * minorCN) + (prop * majorCN));
						idsDup.put(id,magicValue);
					}
					//sb.append(id + "\t" + results.get(id) + "\t" + refNum.get(plasmaIdToGenomicId.get(id)) + "\t" + altNum.get(plasmaIdToGenomicId.get(id)) + "\t" + ref + "\t" + alt + "\t" + Tools.doubleToPercent(prop, 0) + "\t" + majorCN + "\t" + minorCN + "\t" + Tools.doubleToString(pval, 2, true) + "\t" + (i < thresh) + "\t" + Tools.doubleToPercent(magicValue, 2) + "\n");
				}
				//Retreive Facet data
				double minPurity = 1.0;
				double maxPurity = 0.0;
				boolean facetFound = false;
				for (String line : Tools.httpGet("http://192.168.50.22/reports/"+projects[row]+"/facets/"+plasma+"/"+plasma+".facets.tsv").split("\n")) {
					if (line.startsWith("\"chrom\"")) {
						facetFound = true;
					}else if (facetFound) {
						String purity = line.split("\t")[11];
						if (!purity.equals("NA")) {
							double pur = Double.parseDouble(purity);
							if (pur < minPurity) minPurity = pur;
							if (pur > maxPurity) maxPurity = pur;
						}
					}
				}											

				List<Double> vals = new ArrayList<>(idsHemi.values());
				vals.addAll(idsDup.values());
				Collections.sort(vals);
				double median = 0.0;
				double mean = 0.0;
				double sd = 0.0;
				for (double x : vals) {
					mean += x;
				}
				if (!vals.isEmpty()) mean /= vals.size(); 

				if (!vals.isEmpty()) {
					if (vals.size() %2 == 0) {
						median = (vals.get((vals.size()/2)-1) + vals.get(vals.size()/2)) / 2.0;
					}else {
						median = vals.get(vals.size()/2);
					}
				}
				if (vals.size() > 1) {
					for (double x : vals) {
						sd += Math.pow(x - mean, 2);
					}
					sd /= vals.size()-1;
					sd = Math.sqrt(sd);
				}

				int col=0;
				data[row][col++] = plasma;
				if (facetFound) {
					if (minPurity == maxPurity) data[row][col++] = Tools.doubleToPercent(minPurity, 0);
					else data[row][col++] = Tools.doubleToPercent(minPurity, 0) + "-" + Tools.doubleToPercent(maxPurity, 0);
				}else {
					data[row][col++] = "FACET file not found";
				}
				if (vals.size() > 1) data[row][col++] = Tools.doubleToPercent(Collections.min(vals), 0) + "-" + Tools.doubleToPercent(Collections.max(vals), 0);
				else if (vals.size() == 1) data[row][col++] = Tools.doubleToPercent(Collections.min(vals), 0);
				else data[row][col++] = "0 %";
				data[row][col++] = median;
				data[row][col++] = mean;
				data[row][col++] = sd;
				data[row][col++] = thresh;
				data[row][col++] = (sortedList.size() - thresh);
				StringBuilder sb = new StringBuilder();
				for (int id : idsHemi.keySet()) {
					sb.append(results.get(id) + ", ");
				}
				if (idsHemi.size() > 0) sb.delete(sb.length()-2, sb.length());
				data[row][col++] = sb.toString();

			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}		

		CtdnaTableModel model = new CtdnaTableModel(data, headers, classes);
		TableRowSorter<CtdnaTableModel> sorter = new TableRowSorter<CtdnaTableModel>(model);
		table.setModel(model);
		table.setRowSorter(sorter);
		//Set the column width to the max between every cell content and header
		for (int i=0 ; i < table.getColumnCount() ; i++){
			int width = 0;
			for (int r = 0; r < table.getRowCount(); r++) {
				TableCellRenderer renderer = table.getCellRenderer(r, i);
				Component comp = table.prepareRenderer(renderer, r, i);
				width = Math.max (comp.getPreferredSize().width, width);
			}
			TableColumn column = table.getColumnModel().getColumn(i);
			TableCellRenderer headerRenderer = column.getHeaderRenderer();
			if (headerRenderer == null) {
				headerRenderer = table.getTableHeader().getDefaultRenderer();
			}
			Object headerValue = column.getHeaderValue();
			Component headerComp = headerRenderer.getTableCellRendererComponent(table, headerValue, false, false, 0, i);
			width = Math.max(width, headerComp.getPreferredSize().width);
			column.setPreferredWidth(width + 20);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	public void export(){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile("ctDNA Estimation.xlsx");
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
						Sheet sheet = wb.createSheet("ctDNA estimation");
						sheet.createFreezePane(1, 1);		
						int r = 0;
						Row row = sheet.createRow(r++);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							row.createCell(c).setCellValue(table.getColumnName(c));
						}
						int nrow = table.getRowCount();
						waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" samples", false);
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
							waitingPanel.setProgressValue(i);						
						}	
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							sheet.autoSizeColumn(c);					
						}
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
}
