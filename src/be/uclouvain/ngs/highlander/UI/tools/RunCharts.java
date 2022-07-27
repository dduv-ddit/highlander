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
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.WordUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.charts.BarChart;
import be.uclouvain.ngs.highlander.UI.dialog.CreateRunSelection;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.RunNGS;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class RunCharts extends JFrame {

	private JScrollPane panel_center = new JScrollPane();
	private JLabel label_run_selection = new JLabel("No run selected !");
	private JCheckBox sample_split = new JCheckBox("Split runs in samples");
	private JCheckBox show_mean = new JCheckBox("Show mean and SD");
	private JComboBox<String> order = new JComboBox<>(new String[]{
			"run_date",
			"run_id",
			"sample",
			"value",
	});
	private AutoCompleteSupport<String> fieldBoxSupport;
	private EventList<String> fieldList;
	private JComboBox<String> boxField;
	private String[] fields = new String[]{
			"average_depth_of_target_coverage",
			"percent_of_target_covered_meq_1X",
			"percent_of_target_covered_meq_5X",
			"percent_of_target_covered_meq_10X",
			"percent_of_target_covered_meq_20X",
			"coverage_wo_dup",
			"percent_of_target_covered_meq_1X_wo_dup",
			"percent_of_target_covered_meq_5X_wo_dup",
			"percent_of_target_covered_meq_10X_wo_dup",
			"percent_of_target_covered_meq_20X_wo_dup",
			"percent_of_target_covered_meq_30X_wo_dup",
			"coverage_exome_wo_dup",
			"percent_of_exome_covered_meq_1X_wo_dup",
			"percent_of_exome_covered_meq_5X_wo_dup",
			"percent_of_exome_covered_meq_10X_wo_dup",
			"percent_of_exome_covered_meq_20X_wo_dup",
			"percent_of_exome_covered_meq_30X_wo_dup",
			"percent_duplicates_picard",
			"sequence_duplication_prop",
	};

	private Set<RunNGS> selectedRuns = new TreeSet<RunNGS>();

	static private WaitingPanel waitingPanel;

	public RunCharts(){
		List<String> allFields = new ArrayList<>(Arrays.asList(fields));
		for (Analysis analysis : Highlander.getAvailableAnalyses()){
			allFields.add(analysis+"_gene_coverage_ratio_chr_xy");
			allFields.add(analysis+"_ti_tv_ratio_all");
			allFields.add(analysis+"_ti_tv_ratio_known");
			allFields.add(analysis+"_ti_tv_ratio_novel");
			allFields.add(analysis+"_het_hom_ratio_all");
			allFields.add(analysis+"_het_hom_ratio_known");
			allFields.add(analysis+"_het_hom_ratio_novel");
			allFields.add(analysis+"_variant_count_all");
			allFields.add(analysis+"_variant_count_known");
			allFields.add(analysis+"_variant_count_novel");
			allFields.add(analysis+"_variant_count_pass_filters_all");
			allFields.add(analysis+"_variant_count_pass_filters_known");
			allFields.add(analysis+"_variant_count_pass_filters_novel");
			allFields.add(analysis+"_SNV_count_all");
			allFields.add(analysis+"_SNV_count_known");
			allFields.add(analysis+"_SNV_count_novel");
			allFields.add(analysis+"_SNV_count_pass_filters_all");
			allFields.add(analysis+"_SNV_count_pass_filters_known");
			allFields.add(analysis+"_SNV_count_pass_filters_novel");
			allFields.add(analysis+"_INDEL_count_all");
			allFields.add(analysis+"_INDEL_count_known");
			allFields.add(analysis+"_INDEL_count_novel");
			allFields.add(analysis+"_INDEL_count_pass_filters_all");
			allFields.add(analysis+"_INDEL_count_pass_filters_known");
			allFields.add(analysis+"_INDEL_count_pass_filters_novel");
		}
		fieldList = GlazedLists.eventListOf(allFields.toArray(new String[0]));
		boxField = new JComboBox<>(allFields.toArray(new String[0]));
		initUI();
	}

	private void initUI(){
		setTitle("Run statistics charts");
		setIconImage(Resources.getScaledIcon(Resources.iRunStatisticsCharts, 64).getImage());

		setLayout(new BorderLayout());

		panel_center.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		panel_center.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		getContentPane().add(panel_center, BorderLayout.CENTER);

		JPanel panel_south = new JPanel();	
		getContentPane().add(panel_south, BorderLayout.SOUTH);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExportJpeg, 24));
		export.setToolTipText("Export current chart to image file");
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						export();
					}
				}, "RunCharts.export").start();
			}
		});
		panel_south.add(export);

		JButton btnClose = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel_south.add(btnClose);

		JPanel panel_north = new JPanel();
		getContentPane().add(panel_north, BorderLayout.NORTH);

		JButton btnSelect = new JButton("Select NGS runs to include in the chart",Resources.getScaledIcon(Resources.i3dPlus, 24));
		btnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CreateRunSelection select = new CreateRunSelection(selectedRuns);
				Tools.centerWindow(select, false);
				select.setVisible(true);
				if (!select.getSelection().isEmpty()){
					selectedRuns = select.getSelection();
					if (selectedRuns.size() > 0){
						label_run_selection.setText(selectedRuns.size() + " runs selected");
					}else{
						label_run_selection.setText("No run selected !");						
					}
					label_run_selection.repaint();
				}
				if (boxField.getSelectedIndex() >= 0 && !selectedRuns.isEmpty()) { 
					new Thread(new Runnable(){
						public void run(){
							showChart(boxField.getSelectedItem().toString(), order.getSelectedItem().toString());
						}
					}, "RunCharts.showChart").start();
				}
			}
		});
		panel_north.add(btnSelect);

		panel_north.add(label_run_selection);

		boxField.setMaximumRowCount(30);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				fieldBoxSupport = AutoCompleteSupport.install(boxField, fieldList);
				fieldBoxSupport.setCorrectsCase(true);
				fieldBoxSupport.setFilterMode(TextMatcherEditor.CONTAINS);
				fieldBoxSupport.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				fieldBoxSupport.setStrict(false);
			}
		});
		boxField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("comboBoxEdited")){
					if (boxField.getSelectedIndex() < 0) boxField.setSelectedItem(null);
				}
			}
		});
		boxField.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (boxField.getSelectedIndex() >= 0 && !selectedRuns.isEmpty()) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run(){
								showChart(boxField.getSelectedItem().toString(), order.getSelectedItem().toString());
							}
						});
					}
				}
			}
		});
		panel_north.add(boxField);

		panel_north.add(new JLabel("Order by "));		
		order.setMaximumRowCount(30);
		order.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (boxField.getSelectedIndex() >= 0 && !selectedRuns.isEmpty()){
						new Thread(new Runnable(){
							public void run(){
								showChart(boxField.getSelectedItem().toString(), order.getSelectedItem().toString());
							}
						}, "RunCharts.showChart").start();
					}
				}
			}
		});
		panel_north.add(order);

		sample_split.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (boxField.getSelectedIndex() >= 0 && !selectedRuns.isEmpty()){
					new Thread(new Runnable(){
						public void run(){
							showChart(boxField.getSelectedItem().toString(), order.getSelectedItem().toString());
						}
					}, "RunCharts.showChart").start();
				}
			}
		});
		panel_north.add(sample_split);

		show_mean.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (boxField.getSelectedIndex() >= 0 && !selectedRuns.isEmpty()){
					new Thread(new Runnable(){
						public void run(){
							showChart(boxField.getSelectedItem().toString(), order.getSelectedItem().toString());
						}
					}, "RunCharts.showChart").start();
				}
			}
		});
		panel_north.add(show_mean);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	public void showChart(String field, String order){
		try{
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			String[] categories;
			double[] values;
			StringBuilder selection = new StringBuilder();
			selection.append("(");
			for (RunNGS run : selectedRuns){
				selection.append("(run_id = " + run.getRun_id() + " AND run_date = '"+run.getRun_date()+"' AND run_name = '"+run.getRun_name()+"') OR");
			}
			if (selection.length() > 1) selection.delete(selection.length()-3, selection.length());
			else selection.append("run_id < 0");
			selection.append(")");

			String db = null;
			for (Analysis analysis : Highlander.getAvailableAnalyses()){
				if (field.contains(analysis.toString())) db = analysis.toString();
			}

			if (sample_split.isSelected()){
				String dbField = "";
				String query = "";
				if (db != null){
					dbField = field.replace(db+"_", "");
					if (order.equals("value")) order = dbField;
					query = "SELECT * "
							+ "FROM projects "
							+ "JOIN pathologies USING (pathology_id) "
							+ "LEFT JOIN populations USING (population_id) "
							+ "JOIN projects_analyses USING (project_id) "
							+ "WHERE analysis = '"+db+"' AND " + selection.toString() + " ORDER BY " + order;
				}else{
					dbField = field;
					if (order.equals("value")) order = dbField;
					query = "SELECT * "
							+ "FROM projects "
							+ "JOIN pathologies USING (pathology_id) "
							+ "LEFT JOIN populations USING (population_id) "
							+ "WHERE " + selection.toString() + " ORDER BY " + order;
				}
				List<String> runs = new ArrayList<String>();
				List<Double> avgs = new ArrayList<Double>();
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query)) {
					while (res.next()){
						RunNGS run = new RunNGS(res);
						runs.add(run.toString() + ":" + res.getString("sample"));
						avgs.add(res.getDouble(dbField));					
					}
				}
				categories = runs.toArray(new String[0]);
				values = new double[avgs.size()];
				for (int i=0 ; i < values.length ; i++){
					values[i] = avgs.get(i);
				}						
			}else{
				String query = "";
				if (order.equals("value")) order = "avg";
				else if (order.equals("sample")) order = "run_date"; //runs are not split by samples, so we can't order on it, and MySQL 5.7 complains about it
				if (db != null){
					String dbField = field.replace(db+"_", "");
					query = "SELECT run_id, run_date, run_name, run_label, "
							+ "MIN(sequencing_target) as sequencing_target, MIN(platform) as platform, MIN(outsourcing) as outsourcing, MIN(pathology) as pathology, MIN(sample_type) as sample_type, "
							+ "MIN(kit) as kit, MIN(read_length) as read_length, MIN(pair_end) as pair_end, AVG("+dbField+") as avg "
							+ "FROM projects as p "
							+ "JOIN pathologies USING (pathology_id) "
							+ "JOIN projects_analyses as a USING (project_id) "
							+ "WHERE analysis = '"+db+"' AND " + selection.toString() + " "
							+ "GROUP BY run_id, run_date, run_name, run_label ORDER BY " + order;
				}else{
					query = "SELECT run_id, run_date, run_name, run_label, "
							+ "MIN(sequencing_target) as sequencing_target, MIN(platform) as platform, MIN(outsourcing) as outsourcing, MIN(pathology) as pathology, MIN(sample_type) as sample_type, "
							+ "MIN(kit) as kit, MIN(read_length) as read_length, MIN(pair_end) as pair_end, AVG("+field+") as avg "
							+ "FROM projects "
							+ "JOIN pathologies USING (pathology_id) "
							+ "WHERE " + selection.toString() + " "
							+ "GROUP BY run_id, run_date, run_name, run_label ORDER BY " + order;
				}
				List<String> runs = new ArrayList<String>();
				List<Double> avgs = new ArrayList<Double>();
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query)) {
					while (res.next()){
						RunNGS run = new RunNGS(res);
						runs.add(run.toString());
						avgs.add(res.getDouble("avg"));					
					}
				}
				categories = runs.toArray(new String[0]);
				values = new double[avgs.size()];
				for (int i=0 ; i < values.length ; i++){
					values[i] = avgs.get(i);
				}	
			}

			BarChart chart = new BarChart(WordUtils.capitalize(field).replace('_', ' ') + " for selected runs", "Runs", WordUtils.capitalize(field).replace('_', ' '), categories, values, show_mean.isSelected(), false);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			panel_center.setViewportView(chart);
			validate();
			repaint();
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Error when retreiving statistics", ex), "Retreiving " + field+ " statistics", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void export(){
		if (panel_center.getViewport().getComponents().length > 0){
			BarChart chart = (BarChart)panel_center.getViewport().getComponents()[0];
			Object format = JOptionPane.showInputDialog(this, "Choose an image format: ", "Export chart to image file", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iExportJpeg, 64), ImageIO.getWriterFileSuffixes(), "png");
			if (format != null){
				FileDialog chooser = new FileDialog(this, "Export chart to image", FileDialog.SAVE) ;
				chooser.setFile(Tools.formatFilename(chart.getTitle() + "." + format));
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
	}
}
