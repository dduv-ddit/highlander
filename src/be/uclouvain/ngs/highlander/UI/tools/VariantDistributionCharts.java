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
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.apache.commons.lang.WordUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.charts.BarChart;
import be.uclouvain.ngs.highlander.UI.charts.PieChart;
import be.uclouvain.ngs.highlander.UI.dialog.IndependentFilteringPanel;
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;
import be.uclouvain.ngs.highlander.database.Field.Tag;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class VariantDistributionCharts extends JFrame {

	private HighlanderObserver obs = new HighlanderObserver();
	protected HighlanderDatabase DB = Highlander.getDB();

	private JScrollPane panel_center = new JScrollPane();
	private IndependentFilteringPanel filter = new IndependentFilteringPanel(null, obs);
	private JComboBox<Field> boxField;
	private EventList<Field> fields;
	private AutoCompleteSupport<Field> support;
	private JCheckBox aggregate_variants = new JCheckBox("Aggregate by variant");
	private JCheckBox include_NA = new JCheckBox("Include NA");
	private JCheckBox show_mean = new JCheckBox("Show mean and SD");
	private JToggleButton chartType;

	private Map<Object,Integer> data;
	private Map<Object,Integer> aggregate;

	static private WaitingPanel waitingPanel;

	public VariantDistributionCharts(){
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
				obs.setControlName("RESIZE_TOOLBAR");
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
		setTitle("Variants distribution charts");
		setIconImage(Resources.getScaledIcon(Resources.iChartDouble, 64).getImage());

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
				}, "VariantDistributionCharts.export").start();
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

		JPanel panel_north = new JPanel(new BorderLayout());
		panel_north.add(getPanelPreFiltering(filter), BorderLayout.NORTH);

		JPanel panel_toolbar = new JPanel(new WrapLayout());
		panel_north.add(panel_toolbar, BorderLayout.SOUTH);

		getContentPane().add(panel_north, BorderLayout.NORTH);

		List<Field> avfields = new ArrayList<>();
		avfields.addAll(Field.getAvailableFields(Highlander.getCurrentAnalysis(), true));
		Field[] fieldsArr = avfields.toArray(new Field[0]);
		fields = GlazedLists.eventListOf(fieldsArr);
		boxField = new JComboBox<>(fieldsArr);
		boxField.setMaximumRowCount(20);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				support = AutoCompleteSupport.install(boxField, fields);
				support.setCorrectsCase(true);
				support.setFilterMode(TextMatcherEditor.CONTAINS);
				support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				support.setStrict(false);
			}
		});		
		boxField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("comboBoxEdited")){
					if (boxField.getSelectedIndex() < 0) boxField.setSelectedItem(null);
				}
				ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
				renderer.setTooltips(support.getItemList());
				boxField.setRenderer(renderer);
			}
		});
		ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
		renderer.setTooltips(avfields);
		boxField.setRenderer(renderer);
		panel_toolbar.add(boxField);

		aggregate_variants.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (boxField.getSelectedIndex() >= 0 && !data.isEmpty()){
					new Thread(new Runnable(){
						public void run(){
							showChart((Field)boxField.getSelectedItem());;
						}
					}, "VariantDistributionCharts.showChart").start();
				}
			}
		});
		panel_toolbar.add(aggregate_variants);

		include_NA.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (boxField.getSelectedIndex() >= 0 && !data.isEmpty()){
					new Thread(new Runnable(){
						public void run(){
							showChart((Field)boxField.getSelectedItem());;
						}
					}, "VariantDistributionCharts.showChart").start();
				}
			}
		});
		panel_toolbar.add(include_NA);

		show_mean.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (boxField.getSelectedIndex() >= 0 && !data.isEmpty()){
					new Thread(new Runnable(){
						public void run(){
							showChart((Field)boxField.getSelectedItem());;
						}
					}, "VariantDistributionCharts.showChart").start();
				}
			}
		});
		panel_toolbar.add(show_mean);

		chartType = new JToggleButton(Resources.getScaledIcon(Resources.iChartBar, 24));
		chartType.setSelectedIcon(Resources.getScaledIcon(Resources.iChartPie, 24));
		chartType.setHorizontalAlignment(SwingConstants.LEADING);
		chartType.setRolloverEnabled(false);
		chartType.setToolTipText("Show bar or pie chart");
		chartType.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (chartType.isSelected()) {
					show_mean.setText("Show percents");
				}else {
					show_mean.setText("Show mean and SD");
				}
				show_mean.repaint();
				if (boxField.getSelectedIndex() >= 0 && !data.isEmpty()){
					new Thread(new Runnable(){
						public void run(){
							showChart((Field)boxField.getSelectedItem());;
						}
					}, "VariantDistributionCharts.showChart").start();
				}
			}
		});

		panel_toolbar.add(chartType);

		JButton btnShow = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnShow.setToolTipText("Display field distribution chart");
		btnShow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (boxField.getSelectedIndex() >= 0) { 
					new Thread(new Runnable(){
						public void run(){
							fetchChartData(Highlander.getCurrentAnalysis(), (Field)boxField.getSelectedItem());
						}
					}, "VariantDistributionCharts.fetchChartData").start();
				}else if (boxField.getSelectedIndex() < 0) {
					JOptionPane.showMessageDialog(VariantDistributionCharts.this, "Please select a field", "Display field distribution chart", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		});
		panel_toolbar.add(btnShow);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);

	}

	private JPanel getPanelPreFiltering(IndependentFilteringPanel filter){
		JPanel panel_prefilter = new JPanel(new BorderLayout());
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "PreFiltering");
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		panel_prefilter.setToolTipText("<html><b>Only variants obtained from this pre-filter will be taken into account in the magic filter.</b><br>"
				+ "If possible, you should include all the samples from the magic filter for better performances.<br>"
				+ "If your pre-filter is larger than the window and blue arrows don't appear to scroll, resize a little bit the window !</html>");
		panel_prefilter.setBorder(compoundBorder);
		panel_prefilter.add(filter, BorderLayout.CENTER);
		return panel_prefilter;
	}

	public class ComboboxToolTipRenderer extends DefaultListCellRenderer {
		List<Field> tooltips;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			JComponent comp = (JComponent) super.getListCellRendererComponent(list,
					value, index, isSelected, cellHasFocus);

			if (-1 < index && null != value && null != tooltips) {
				list.setToolTipText(tooltips.get(index).getHtmlTooltip());
			}
			return comp;
		}

		public void setTooltips(List<Field> tooltips) {
			this.tooltips = tooltips;
		}
	}

	public void fetchChartData(Analysis analysis, Field field) {
		try{
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});

			List<Field> neededFields = new ArrayList<Field>();
			neededFields.add(Field.chr);
			neededFields.add(Field.pos);
			neededFields.add(Field.length);
			neededFields.add(Field.reference);
			neededFields.add(Field.alternative);
			neededFields.add(field);
			VariantResults variantResults = filter.getFilter().retreiveData(neededFields, filter.getFilter().getAllSamples(), "Retrieving variants");
			int i_field=-1;
			for (int i=0 ; i < variantResults.headers.length ; i++){
				if (variantResults.headers[i].getName().equalsIgnoreCase(field.getName())) i_field = i;
			}

			Set<String> variants = new HashSet<>();

			if (field.hasTag(Tag.FORMAT_PERCENT_2) || field.hasTag(Tag.FORMAT_PERCENT_0)) {
				data = new LinkedHashMap<>();
				data.put("#NA", 0);
				double min = 0.0;
				double[] bins = new double[] {
						0.0001,
						0.001,
						0.01,
						0.1,
						0.2,
						0.3,
						0.4,
						0.5,
						0.6,
						0.7,
						0.8,
						0.9,
						1,
				};
				for (int i=0 ; i < bins.length ; i++) {
					double left = (i > 0) ? bins[i-1] : +min;
					double right = bins[i];
					data.put(((i > 0) ? "]"+((left<0.01)?Tools.doubleToPercent(left, 2):Tools.doubleToPercent(left, 0)) : "[0%") + "-" + ((left<0.01)?Tools.doubleToPercent(right, 2):Tools.doubleToPercent(right, 0))+"]", 0);
				}
				aggregate = new LinkedHashMap<>(data);

				for (int k=0 ; k < variantResults.data.length ; k++){
					String key = "#NA";
					if (variantResults.data[k][i_field] != null) {
						double val = (double)variantResults.data[k][i_field];
						for (int i=0 ; i < bins.length ; i++) {
							if (val <= bins[i]) {
								double left = (i > 0) ? bins[i-1] : +min;
								double right = bins[i];
								key = ((i > 0) ? "]"+((left<0.01)?Tools.doubleToPercent(left, 2):Tools.doubleToPercent(left, 0)) : "[0%") + "-" + ((left<0.01)?Tools.doubleToPercent(right, 2):Tools.doubleToPercent(right, 0))+"]";
								break;
							}
						}
					}
					if (!data.containsKey(key)) {
						data.put(key, 0);
						aggregate.put(key, 0);
					}
					data.put(key, data.get(key)+1);
					if (!variants.contains(variantResults.variant[k])) {
						variants.add(variantResults.variant[k]);
						aggregate.put(key, aggregate.get(key)+1);
					}
				}
			}else if (field.getName().equalsIgnoreCase(Field.consensus_prediction.getName())){
				data = new LinkedHashMap<>();
				data.put("#NA", 0);
				int[] bins = new int[] {
						1,
						2,
						3,
						4,
						5,
						6,
						7,
						8,
						9,
						10,
						11,
						12,
						13,
						14,
						15,
						16,
						17,
						18,
						19,
						20,
						100,
						200,
						300,
						400,
				};
				for (int i=0 ; i < bins.length ; i++) {
					data.put((bins[i] < 100) ? bins[i]+"" : bins[i]+"+", 0);
				}
				aggregate = new LinkedHashMap<>(data);

				for (int k=0 ; k < variantResults.data.length ; k++){
					String key = "#NA";
					if (variantResults.data[k][i_field] != null) {
						int val = (int)variantResults.data[k][i_field];
						for (int i=bins.length-1 ; i >= 0 ; i--) {
							if (val >= bins[i]) {
								key = (bins[i] < 100) ? bins[i]+"" : bins[i]+"+";
								break;
							}
						}
					}
					if (!data.containsKey(key)) {
						data.put(key, 0);
						aggregate.put(key, 0);
					}
					data.put(key, data.get(key)+1);
					if (!variants.contains(variantResults.variant[k])) {
						variants.add(variantResults.variant[k]);
						aggregate.put(key, aggregate.get(key)+1);
					}
				}
			}else if (field.getName().equalsIgnoreCase(Field.chr.getName())){
				data = new LinkedHashMap<>();
				data.put("1", 0);
				data.put("2", 0);
				data.put("3", 0);
				data.put("4", 0);
				data.put("5", 0);
				data.put("6", 0);
				data.put("7", 0);
				data.put("8", 0);
				data.put("9", 0);
				data.put("10", 0);
				data.put("11", 0);
				data.put("12", 0);
				data.put("13", 0);
				data.put("14", 0);
				data.put("15", 0);
				data.put("16", 0);
				data.put("17", 0);
				data.put("18", 0);
				data.put("19", 0);
				data.put("20", 0);
				data.put("21", 0);
				data.put("22", 0);
				data.put("X", 0);
				data.put("Y", 0);
				aggregate = new LinkedHashMap<>(data);
				for (int k=0 ; k < variantResults.data.length ; k++){
					String key;
					if (variantResults.data[k][i_field] == null) {
						key = "#NA";
					}else {
						key = variantResults.data[k][i_field].toString();
					}
					if (!data.containsKey(key)) {
						data.put(key, 0);
						aggregate.put(key, 0);
					}
					data.put(key, data.get(key)+1);
					if (!variants.contains(variantResults.variant[k])) {
						variants.add(variantResults.variant[k]);
						aggregate.put(key, aggregate.get(key)+1);
					}
				}
			}else if (field.getFieldClass() == Double.class) {
				double min = 0.0;
				double max = 0.0;
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT MIN(`"+field+"`), MAX(`"+field+"`) "
								+ "FROM "+analysis.getFromSampleAnnotations()
								+ analysis.getJoinProjects()
								+ "WHERE sample IN ("+HighlanderDatabase.makeSqlList(filter.getFilter().getSamples(), String.class)+")"
						)) {
					if (res.next()){
						min = res.getDouble(1);
						max = res.getDouble(2);
					}
				}

				int nDig = (""+Math.round(max)).length() - 1;							
				max = (((max > 0)?Math.ceil(max / Math.pow(10, nDig)):Math.floor(max / Math.pow(10, nDig))) *  Math.pow(10, nDig));
				int ncat = (int)(max / Math.pow(10, nDig));
				if (nDig > 0 && ncat < 5) ncat *= 10;
				nDig = (""+Math.round(max)).length() - 1;							
				min = (((min < 0)?Math.ceil(min / Math.pow(10, nDig)):Math.floor(min / Math.pow(10, nDig))) *  Math.pow(10, nDig));
				if (min < 10) min = 0;

				data = new LinkedHashMap<>();
				data.put("#NA", 0);
				double interval = (max-min) / 10.0;
				double[] bins = new double[10];
				for (int i=0 ; i < bins.length ; i++) {
					bins[i] = (i < bins.length -1) ? min + ((i+1) * interval) : max;
					data.put(((i > 0) ? "]"+Tools.doubleToString(bins[i-1], 2, true) : "["+Tools.doubleToString(min, 2, true)) + ";" + Tools.doubleToString(bins[i], 2, true)+"]", 0);
				}
				aggregate = new LinkedHashMap<>(data);
				for (int k=0 ; k < variantResults.data.length ; k++){
					String key = "#NA";
					if (variantResults.data[k][i_field] != null) {
						double val = (double)variantResults.data[k][i_field];
						for (int i=0 ; i < bins.length ; i++) {
							if (val <= bins[i]) {
								key = ((i > 0) ? "]"+Tools.doubleToString(bins[i-1], 2, true) : "["+Tools.doubleToString(min, 2, true)) + ";" + Tools.doubleToString(bins[i], 2, true)+"]";
								break;
							}
						}
					}
					if (!data.containsKey(key)) {
						data.put(key, 0);
						aggregate.put(key, 0);
					}
					data.put(key, data.get(key)+1);
					if (!variants.contains(variantResults.variant[k])) {
						variants.add(variantResults.variant[k]);
						aggregate.put(key, aggregate.get(key)+1);
					}
				}
			}else if (field.getFieldClass() == Integer.class) {
				int min = 0;
				int max = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT MIN(`"+field+"`), MAX(`"+field+"`) "
								+ "FROM "+analysis.getFromSampleAnnotations()
								+ analysis.getJoinProjects()
								+ "WHERE sample IN ("+HighlanderDatabase.makeSqlList(filter.getFilter().getSamples(), String.class)+")"
						)) {
					if (res.next()){
						min = res.getInt(1);
						max = res.getInt(2);
					}
				}

				int nDig = (""+max).length() - 1;							
				max = (int)(((max > 0)?Math.ceil(max / Math.pow(10, nDig)):Math.floor(max / Math.pow(10, nDig))) *  Math.pow(10, nDig));
				int ncat = (int)(max / Math.pow(10, nDig));
				if (nDig > 0 && ncat < 5) ncat *= 10;
				nDig = (""+min).length() - 1;							
				min = (int)(((min < 0)?Math.ceil(min / Math.pow(10, nDig)):Math.floor(min / Math.pow(10, nDig))) *  Math.pow(10, nDig));
				if (min < 10) min = 0;

				data = new LinkedHashMap<>();
				data.put("#NA", 0);
				int interval = (max-min) / ncat;
				int[] bins = new int[ncat];
				for (int i=0 ; i < bins.length ; i++) {
					bins[i] = (i < bins.length -1) ? min + ((i+1) * interval) : max;
					data.put(((i > 0) ? "]"+bins[i-1] : "["+min) + ";" + bins[i]+"]", 0);
				}
				aggregate = new LinkedHashMap<>(data);
				for (int k=0 ; k < variantResults.data.length ; k++){
					String key = "#NA";
					if (variantResults.data[k][i_field] != null) {
						int val = (int)variantResults.data[k][i_field];
						for (int i=0 ; i < bins.length ; i++) {
							if (val <= bins[i]) {
								key = ((i > 0) ? "]"+bins[i-1] : "["+min) + ";" + bins[i]+"]";
								break;
							}
						}
					}
					if (!data.containsKey(key)) {
						data.put(key, 0);
						aggregate.put(key, 0);
					}
					data.put(key, data.get(key)+1);
					if (!variants.contains(variantResults.variant[k])) {
						variants.add(variantResults.variant[k]);
						aggregate.put(key, aggregate.get(key)+1);
					}
				}
			}else if (field.getFieldClass() == Long.class) {
				long min = 0;
				long max = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT MIN(`"+field+"`), MAX(`"+field+"`) "
								+ "FROM "+analysis.getFromSampleAnnotations()
								+ analysis.getJoinProjects()
								+ "WHERE sample IN ("+HighlanderDatabase.makeSqlList(filter.getFilter().getSamples(), String.class)+")"
						)) {
					if (res.next()){
						min = res.getLong(1);
						max = res.getLong(2);
					}
				}

				int nDig = (""+max).length() - 1;							
				max = (long)(((max > 0)?Math.ceil(max / Math.pow(10, nDig)):Math.floor(max / Math.pow(10, nDig))) *  Math.pow(10, nDig));
				int ncat = (int)(max / Math.pow(10, nDig));
				if (nDig > 0 && ncat < 5) ncat *= 10;
				nDig = (""+min).length() - 1;							
				min = (long)(((min < 0)?Math.ceil(min / Math.pow(10, nDig)):Math.floor(min / Math.pow(10, nDig))) *  Math.pow(10, nDig));
				if (min < 10) min = 0;

				data = new LinkedHashMap<>();
				data.put("#NA", 0);
				long interval = (max-min) / ncat;
				long[] bins = new long[ncat];
				for (int i=0 ; i < bins.length ; i++) {
					bins[i] = (i < bins.length -1) ? min + ((i+1) * interval) : max;
					data.put(((i > 0) ? "]"+bins[i-1] : "["+min) + ";" + bins[i]+"]", 0);
				}
				aggregate = new LinkedHashMap<>(data);
				for (int k=0 ; k < variantResults.data.length ; k++){
					String key = "#NA";
					if (variantResults.data[k][i_field] != null) {
						long val = (long)variantResults.data[k][i_field];
						for (int i=0 ; i < bins.length ; i++) {
							if (val <= bins[i]) {
								key = ((i > 0) ? "]"+bins[i-1] : "["+min) + ";" + bins[i]+"]";
								break;
							}
						}
					}
					if (!data.containsKey(key)) {
						data.put(key, 0);
						aggregate.put(key, 0);
					}
					data.put(key, data.get(key)+1);
					if (!variants.contains(variantResults.variant[k])) {
						variants.add(variantResults.variant[k]);
						aggregate.put(key, aggregate.get(key)+1);
					}
				}
			}else {
				data = new TreeMap<>();
				aggregate = new TreeMap<>();
				for (int k=0 ; k < variantResults.data.length ; k++){
					String key;
					if (variantResults.data[k][i_field] == null) {
						key = "#NA";
					}else {
						key = variantResults.data[k][i_field].toString();
					}
					if (!data.containsKey(key)) {
						data.put(key, 0);
						aggregate.put(key, 0);
					}
					data.put(key, data.get(key)+1);
					if (!variants.contains(variantResults.variant[k])) {
						variants.add(variantResults.variant[k]);
						aggregate.put(key, aggregate.get(key)+1);
					}
				}
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			showChart(field);
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Error when retreiving values", ex), "Retreiving " + field+ " values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void showChart(Field field) {
		Map<Object, Integer> map = (aggregate_variants.isSelected()) ? new LinkedHashMap<>(aggregate) : new LinkedHashMap<>(data);
		if (!include_NA.isSelected()) map.remove("#NA");
		String[] categories = map.keySet().toArray(new String[0]);
		double[] values = new double[map.size()];
		for (int i=0 ; i < values.length ; i++){
			values[i] = map.get(categories[i]);
		}

		if (!chartType.isSelected()) {
			BarChart chart = new BarChart(WordUtils.capitalize(field.toString()).replace('_', ' ') + " distribution for selected samples" + ((aggregate_variants.isSelected())?" (aggregated by variant)":""), WordUtils.capitalize(field.toString()).replace('_', ' ') + " values", "Number of variants", categories, values, show_mean.isSelected(), true);
			panel_center.setViewportView(chart);
		}else {
			PieChart chart = new PieChart(WordUtils.capitalize(field.toString()).replace('_', ' ') + " distribution for selected samples" + ((aggregate_variants.isSelected())?" (aggregated by variant)":""), categories, values, show_mean.isSelected());
			panel_center.setViewportView(chart);			
		}
		validate();
		repaint();
	}

	public void export(){
		if (panel_center.getViewport().getComponents().length > 0){
			JPanel chart = (JPanel)panel_center.getViewport().getComponents()[0];
			String title = (chartType.isSelected()) ? ((PieChart)chart).getTitle() : ((BarChart)chart).getTitle(); 
			Object format = JOptionPane.showInputDialog(this, "Choose an image format: ", "Export chart to image file", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iExportJpeg, 64), ImageIO.getWriterFileSuffixes(), "png");
			if (format != null){
				FileDialog chooser = new FileDialog(this, "Export chart to image", FileDialog.SAVE) ;
				chooser.setFile(Tools.formatFilename(title + "." + format));
				Tools.centerWindow(chooser, false);
				chooser.setVisible(true) ;
				if (chooser.getFile() != null) {
					try {
						String filename = chooser.getDirectory() + chooser.getFile();
						if (!filename.toLowerCase().endsWith("."+format.toString())) filename += "."+format.toString();      
						BufferedImage image = new BufferedImage(chart.getWidth(), chart.getHeight(), BufferedImage.TYPE_INT_RGB);
						Graphics2D g = image.createGraphics();
						if (chartType.isSelected()) {
							((PieChart)chart).paintComponent(g); 
						}else {
							((BarChart)chart).paintComponent(g);
						}
						ImageIO.write(image, format.toString(), new File(filename));
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(this, Tools.getMessage("Error when exporting chart", ex), "Export chart to image file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));  			}
				}   		
			}
		}
	}

}
