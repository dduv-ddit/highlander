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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.dialog.AskListOfFreeValuesDialog;
import be.uclouvain.ngs.highlander.UI.dialog.AskListOfPossibleValuesDialog;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.table.HeatMap;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ColorRange;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ConversionMethod;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

import java.util.Arrays;
import java.awt.Insets;

import javax.swing.JTextArea;
import javax.swing.JRadioButton;

import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

public class CoverageInfo extends JFrame {

	public enum Grouping {GENE, EXON, SAMPLE_GENE, SAMPLE_EXON}

	private static final List<String> leftAligned = new ArrayList<String>(Arrays.asList(new String[]{
			"",
	}));
	private static final List<String> percent = new ArrayList<String>(Arrays.asList(new String[]{
			"",
	}));
	private static final List<String> hasHeatMap = new ArrayList<String>(Arrays.asList(new String[]{
			"mean_depth",
	}));

	private static final List<String> coverages = new ArrayList<String>();

	private Map<Grouping, CoverageTableModel> tableModels = new EnumMap<Grouping, CoverageInfo.CoverageTableModel>(Grouping.class);
	private Map<Grouping, JTable> tables = new EnumMap<Grouping, JTable>(Grouping.class);
	private Map<Grouping, TableRowSorter<CoverageTableModel>> sorters = new EnumMap<Grouping, TableRowSorter<CoverageTableModel>>(Grouping.class);
	private Map<JTable, HeatMap> heatMaps = new HashMap<JTable, HeatMap>();

	static private WaitingPanel waitingPanel;
	private JTabbedPane tabs;
	private final ButtonGroup targertsButtonGroup = new ButtonGroup();
	private final ButtonGroup samplesButtonGroup = new ButtonGroup();
	private JRadioButton rdbtnGeneList;
	private JRadioButton rdbtnRegionsOfInterest;
	private JCheckBox boxShowNA;
	private JSpinner depthMinSpinner;
	private JSpinner depthMaxSpinner;
	private JTextArea textAreaTargets;
	private JTextArea textAreaSamples;
	private JRadioButton rdbtnSamples;
	private JRadioButton rdbtnGroup;
	private JButton btnLoad;
	private JButton btnPossibleValues;

	private boolean hasTable = false;
	private List<String> targets = new ArrayList<String>();
	private List<String> samples = new ArrayList<String>();
	private Map<String,Map<String,Map<Integer,Double[]>>> results = new TreeMap<>(new Tools.NaturalOrderComparator(true));
	
	public CoverageInfo(){
		try{
			coverages.clear();
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SHOW COLUMNS FROM " + Highlander.getCurrentAnalysis().getFromCoverage())) {
				while(res.next()){
					String col = Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res);
					if (col.startsWith("num_pos_")){
						coverages.add(col.substring("num_pos_".length()));
					}
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive coverage columns", ex), "Retreive coverage columns",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		percent.clear();
		hasHeatMap.clear();
		hasHeatMap.add("mean_depth");
		for (String cov : coverages) {
			percent.add("coverage_above_"+cov);
			hasHeatMap.add("coverage_above_"+cov);			
		}
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/5);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
	}

	private void initUI(){
		setTitle("Coverage information");
		setIconImage(Resources.getScaledIcon(Resources.iCoverage, 64).getImage());

		getContentPane().setLayout(new BorderLayout());

		JPanel panel_south = new JPanel();	
		getContentPane().add(panel_south, BorderLayout.SOUTH);

		JButton btnClose = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel_south.add(btnClose);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExcel, 24));
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						export(Grouping.values()[tabs.getSelectedIndex()]);
					}
				}, "CoverageInfo.export").start();
			}
		});
		panel_south.add(export);

		GridBagLayout gbl_northPanel = new GridBagLayout();
		gbl_northPanel.rowWeights = new double[]{1.0};
		gbl_northPanel.columnWeights = new double[]{1.0, 0.0, 0.0};
		JPanel northPanel = new JPanel(gbl_northPanel);
		getContentPane().add(northPanel, BorderLayout.NORTH);

		JPanel targetPanel = new JPanel();
		targetPanel.setBorder(new TitledBorder(null, "Targets", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_targetPanel = new GridBagConstraints();
		gbc_targetPanel.weightx = 1.0;
		gbc_targetPanel.insets = new Insets(5, 10, 5, 10);
		gbc_targetPanel.fill = GridBagConstraints.BOTH;
		gbc_targetPanel.gridx = 0;
		gbc_targetPanel.gridy = 0;
		northPanel.add(targetPanel, gbc_targetPanel);
		GridBagLayout gbl_targetPanel = new GridBagLayout();
		gbl_targetPanel.columnWidths = new int[]{0, 0};
		gbl_targetPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_targetPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_targetPanel.rowWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		targetPanel.setLayout(gbl_targetPanel);

		JPanel panel_1 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_1.getLayout();
		flowLayout.setVgap(0);
		flowLayout.setAlignment(FlowLayout.LEFT);
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 0;
		targetPanel.add(panel_1, gbc_panel_1);

		rdbtnGeneList = new JRadioButton("Gene list");
		rdbtnGeneList.setSelected(true);
		rdbtnGeneList.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				textAreaTargets.setText("");
			}
		});
		targertsButtonGroup.add(rdbtnGeneList);
		panel_1.add(rdbtnGeneList);

		rdbtnRegionsOfInterest = new JRadioButton("Regions of interest");
		rdbtnRegionsOfInterest.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				textAreaTargets.setText("");
			}
		});
		rdbtnRegionsOfInterest.setToolTipText("Region format is 'chr-start-stop', like '14-2100000-2200000'. Don't put things like 'chr14-2,100,000-2,200,000'.");
		targertsButtonGroup.add(rdbtnRegionsOfInterest);
		panel_1.add(rdbtnRegionsOfInterest);
		//Position data is no longer available in the gene_coverage db, and this specific usage was never used -- 11/2020 could be back again
		rdbtnRegionsOfInterest.setVisible(false);

		JPanel panel_2 = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel_2.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		flowLayout_1.setVgap(0);
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.insets = new Insets(0, 0, 5, 0);
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 0;
		gbc_panel_2.gridy = 1;
		targetPanel.add(panel_2, gbc_panel_2);

		JButton btnNewButton_1 = new JButton("Load from profile",Resources.getScaledIcon(Resources.iUserList, 16));
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String name = ProfileTree.showProfileDialog(CoverageInfo.this, Action.LOAD, UserData.VALUES, "gene_symbol");
				if (name != null){
					try{
						textAreaTargets.setText("");
						for (String val : Highlander.getLoggedUser().loadValues(Field.gene_symbol, name)){
							textAreaTargets.append(val.toUpperCase()+";");
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive profile values", ex), "Retreive profile values",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		});
		panel_2.add(btnNewButton_1);

		JButton btnNewButton_2 = new JButton("Create gene list",Resources.getScaledIcon(Resources.iDbStatus, 16));
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<String> existingValues = new ArrayList<String>();
				try{
					if(textAreaTargets.getText().length() > 0){
						existingValues = Arrays.asList(textAreaTargets.getText().split(";"));
					}
					AskListOfPossibleValuesDialog ask = new AskListOfPossibleValuesDialog(Field.gene_symbol, null, false, existingValues);
					Tools.centerWindow(ask, false);
					ask.setVisible(true);
					if (!ask.getSelection().isEmpty()){
						textAreaTargets.setText("");
						for (String value : ask.getSelection()){
							textAreaTargets.append(value.replace('\t', '-')+";");
						}
					}
				}catch(Exception ex){
					Tools.exception(ex);
					Field field = null;
					try {
						field = Field.gene_symbol;
					}catch(Exception ex2) {					
					}
					AskListOfFreeValuesDialog ask = new AskListOfFreeValuesDialog(field, existingValues);
					Tools.centerWindow(ask, false);
					ask.setVisible(true);
					if (!ask.getSelection().isEmpty()){
						for (String value : ask.getSelection()){
							textAreaTargets.append(value.replace('\t', '-')+";");
						}
					}
				}
			}
		});
		panel_2.add(btnNewButton_2);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.insets = new Insets(0, 5, 5, 5);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 2;
		targetPanel.add(scrollPane_1, gbc_scrollPane_1);

		textAreaTargets = new JTextArea();
		textAreaTargets.setToolTipText(/*"Region format is 'chr-start-stop', like '14-2100000-2200000'. "+*/"Case insensitive, values must be separated by ';'. If you want to search for a ';' in the database you must write preceded by a backslash '\\;'.");
		textAreaTargets.setRows(3);
		scrollPane_1.setViewportView(textAreaTargets);

		JPanel samplesPanel = new JPanel();
		samplesPanel.setBorder(new TitledBorder(null, "Samples", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_samplesPanel = new GridBagConstraints();
		gbc_samplesPanel.weightx = 1.0;
		gbc_samplesPanel.insets = new Insets(5, 10, 5, 10);
		gbc_samplesPanel.fill = GridBagConstraints.BOTH;
		gbc_samplesPanel.gridx = 1;
		gbc_samplesPanel.gridy = 0;
		northPanel.add(samplesPanel, gbc_samplesPanel);
		GridBagLayout gbl_samplesPanel = new GridBagLayout();
		gbl_samplesPanel.columnWidths = new int[]{0, 0};
		gbl_samplesPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_samplesPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_samplesPanel.rowWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		samplesPanel.setLayout(gbl_samplesPanel);

		JPanel panel_3 = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) panel_3.getLayout();
		flowLayout_2.setVgap(0);
		flowLayout_2.setAlignment(FlowLayout.LEFT);
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.insets = new Insets(0, 0, 5, 0);
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 0;
		gbc_panel_3.gridy = 0;
		samplesPanel.add(panel_3, gbc_panel_3);

		rdbtnSamples = new JRadioButton("Samples");
		rdbtnSamples.setSelected(true);
		rdbtnSamples.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				textAreaSamples.setText("");
			}
		});
		samplesButtonGroup.add(rdbtnSamples);
		panel_3.add(rdbtnSamples);

		rdbtnGroup = new JRadioButton("Pathology");
		rdbtnRegionsOfInterest.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				textAreaSamples.setText("");
			}
		});
		samplesButtonGroup.add(rdbtnGroup);
		panel_3.add(rdbtnGroup);

		JPanel panel_4 = new JPanel();
		FlowLayout flowLayout_3 = (FlowLayout) panel_4.getLayout();
		flowLayout_3.setAlignment(FlowLayout.LEFT);
		flowLayout_3.setVgap(0);
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.insets = new Insets(0, 0, 5, 0);
		gbc_panel_4.fill = GridBagConstraints.BOTH;
		gbc_panel_4.gridx = 0;
		gbc_panel_4.gridy = 1;
		samplesPanel.add(panel_4, gbc_panel_4);

		btnLoad = new JButton("Load from profile",Resources.getScaledIcon(Resources.iUserList, 16));
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = ProfileTree.showProfileDialog(CoverageInfo.this, Action.LOAD, UserData.VALUES, "sample");
				if (name != null){
					try{
						textAreaSamples.setText("");
						for (String val : Highlander.getLoggedUser().loadValues(Field.sample, name)){
							textAreaSamples.append(val.toUpperCase()+";");
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive profile values", ex), "Retreive profile values",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		});
		panel_4.add(btnLoad);

		btnPossibleValues = new JButton("Create list",Resources.getScaledIcon(Resources.iDbStatus, 16));
		btnPossibleValues.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<String> existingValues = new ArrayList<String>();
				try{
					if(textAreaSamples.getText().length() > 0){
						existingValues = Arrays.asList(textAreaSamples.getText().split(";"));
					}
					AskListOfPossibleValuesDialog ask = new AskListOfPossibleValuesDialog((rdbtnSamples.isSelected())?Field.sample:Field.pathology, null, false, existingValues);
					Tools.centerWindow(ask, false);
					ask.setVisible(true);
					if (!ask.getSelection().isEmpty()){
						textAreaSamples.setText("");
						for (String value : ask.getSelection()){
							textAreaSamples.append(value.replace('\t', '-')+";");
						}
					}
				}catch(Exception ex){
					Tools.exception(ex);
					Field field = null;
					try {
						field = (rdbtnSamples.isSelected())?Field.sample:Field.pathology;
					}catch(Exception ex2) {					
					}
					AskListOfFreeValuesDialog ask = new AskListOfFreeValuesDialog(field, existingValues);
					Tools.centerWindow(ask, false);
					ask.setVisible(true);
					if (!ask.getSelection().isEmpty()){
						for (String value : ask.getSelection()){
							textAreaSamples.append(value.replace('\t', '-')+";");
						}
					}
				}				
			}
		});
		panel_4.add(btnPossibleValues);

		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.insets = new Insets(0, 5, 5, 5);
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 0;
		gbc_scrollPane_2.gridy = 2;
		samplesPanel.add(scrollPane_2, gbc_scrollPane_2);

		textAreaSamples = new JTextArea();
		textAreaSamples.setRows(3);
		scrollPane_2.setViewportView(textAreaSamples);

		JPanel validationPanel = new JPanel();
		GridBagConstraints gbc_validationPanel = new GridBagConstraints();
		gbc_validationPanel.insets = new Insets(5, 10, 5, 20);
		gbc_validationPanel.fill = GridBagConstraints.BOTH;
		gbc_validationPanel.gridx = 2;
		gbc_validationPanel.gridy = 0;
		northPanel.add(validationPanel, gbc_validationPanel);
		GridBagLayout gbl_validationPanel = new GridBagLayout();
		gbl_validationPanel.columnWidths = new int[]{147, 0};
		gbl_validationPanel.rowHeights = new int[]{33, 0};
		gbl_validationPanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_validationPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		validationPanel.setLayout(gbl_validationPanel);

		JButton btnNewButton = new JButton("Get coverage info", Resources.getScaledIcon(Resources.iCoverage, 24));
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (textAreaTargets.getText().length() == 0){
							JOptionPane.showMessageDialog(CoverageInfo.this, "You must give at least one gene !", "Cannot get coverage info", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCoverage, 64));
							return;
						}
						if (textAreaSamples.getText().length() == 0){
							JOptionPane.showMessageDialog(CoverageInfo.this, "You must give at least one sample !", "Cannot get coverage info", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCoverage, 64));
							return;
						}
						for (Grouping grouping : Grouping.values())	fetchData(grouping);
					}
				}, "CoverageInfo.getCoverageInfo").start();
			}
		});
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnNewButton.weighty = 1.0;
		gbc_btnNewButton.weightx = 1.0;
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 0;
		validationPanel.add(btnNewButton, gbc_btnNewButton);

		JPanel panel_center = new JPanel(new BorderLayout());
		getContentPane().add(panel_center, BorderLayout.CENTER);
		
		JPanel panel_other_options = new JPanel(new FlowLayout(FlowLayout.LEADING, 40, 2));
		panel_center.add(panel_other_options, BorderLayout.NORTH);
		
		JPanel panel_other_options_NA = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_other_options.add(panel_other_options_NA);
		
		boxShowNA = new JCheckBox("Show genes and samples not present in coverage database as N/A");
		boxShowNA.setSelected(true);
		boxShowNA.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				for (Grouping grouping : Grouping.values())	fillTable(grouping);
			}
		});
		panel_other_options_NA.add(boxShowNA);
		
		JPanel panel_other_options_depth = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_other_options.add(panel_other_options_depth);
		
		JLabel depthLabel = new JLabel("Range for coloring mean depth");
		panel_other_options_depth.add(depthLabel);
		
		depthMinSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 10000, 5));
		depthMinSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				for (Grouping grouping : Grouping.values())	fillTable(grouping);
			}
		});
		panel_other_options_depth.add(depthMinSpinner);

		JLabel depthToLabel = new JLabel(" to ");
		panel_other_options_depth.add(depthToLabel);
		
		depthMaxSpinner = new JSpinner(new SpinnerNumberModel(65, 0, 10000, 5));
		depthMaxSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				for (Grouping grouping : Grouping.values())	fillTable(grouping);
			}
		});
		panel_other_options_depth.add(depthMaxSpinner);

		tabs = new JTabbedPane();
		panel_center.add(tabs, BorderLayout.CENTER);

		for (Grouping grouping : Grouping.values()){
			JScrollPane scrollPane = new JScrollPane();
			String tabName = grouping.toString();
			switch (grouping) {
			case GENE:
				tabName = "Grouped by gene";
				break;
			case SAMPLE_GENE:
				tabName = "Grouped by sample and gene";
				break;
			case EXON:
				tabName = "Grouped by exon";
				break;
			case SAMPLE_EXON:
				tabName = "Grouped by sample and exon";
				break;
			}
			tabs.addTab(tabName, scrollPane);

			JTable table = new JTable(){
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
			tables.put(grouping, table);
			heatMaps.put(table, new HeatMap(table));
			//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());
			table.setDefaultRenderer(Integer.class, new ColoredTableCellRenderer());
			table.setDefaultRenderer(Double.class, new ColoredTableCellRenderer());
			table.setDefaultRenderer(Boolean.class, new ColoredTableCellRenderer());
			table.setDefaultRenderer(Date.class, new ColoredTableCellRenderer());
			table.setDefaultRenderer(Long.class, new ColoredTableCellRenderer());
			scrollPane.setViewportView(table);
		}

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}	

	private class ColoredTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JLabel label = (JLabel) comp;
			String colname = table.getColumnName(column);

			if (leftAligned.contains(colname)){
				label.setHorizontalAlignment(JLabel.LEFT);
			}else{
				label.setHorizontalAlignment(JLabel.CENTER);
			}

			if (value != null) {
				if (percent.contains(colname)){
					value = Tools.doubleToString(((Double)value)*100.0, 0, false) + "%";
				}else if (table.getColumnClass(column) == Double.class){
					value = Tools.doubleToString((Double)value, 0, false);
				}else if (table.getColumnClass(column) == Long.class){
					value = Tools.longToString((Long)value);
				}else if (table.getColumnClass(column) == Integer.class){
					value = Tools.intToString((Integer)value);
				}
			}

			if (row%2 == 0) label.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Purple));
			else label.setBackground(Color.white);
			label.setForeground(Color.black);
			label.setBorder(new LineBorder(Color.WHITE));
			if (value != null){
				if (hasHeatMap.contains(colname)){
					label.setBackground(heatMaps.get(table).getColor(row, column));
				}
				label.setText(value.toString());
			}else {
				label.setText("N/A");
			}
			if (isSelected) {
				label.setBackground(new Color(51,153,255));
			}
			return label;
		}
	}

	public static class CoverageTableModel	extends AbstractTableModel {
		final private Object[][] data;
		final private String[] headers;
		final private Class<?>[] classes;

		public CoverageTableModel(Object[][] data, String[] headers, Class<?>[] classes) {    	
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

	private void fetchData(Grouping grouping){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {			
			targets.clear();
			for (String val : textAreaTargets.getText().replace("\\\\;", "|*?").split(";")){
				if (val.trim().length() > 0) {
					targets.add(val.replace("|*?", ";").toUpperCase().trim());					
				}
			}
			if (targets.isEmpty()) return;
			samples.clear();
			for (String val : textAreaSamples.getText().replace("\\\\;", "|*?").split(";")){
				if (val.trim().length() > 0) {
					samples.add(val.replace("|*?", ";").toUpperCase().trim());					
				}
			}
			if (rdbtnGroup.isSelected()){
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT sample FROM projects JOIN pathologies USING (pathology_id) WHERE pathology IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+")"
						)) {
					samples.clear();
					while(res.next()) {
						samples.add(res.getString("sample").toUpperCase());
					}
				}
			}
			StringBuilder query = new StringBuilder();
			query.append("SELECT sample, gene_symbol, exon, "
					+ "`end`-`start` as length, "
					+ "`mean_depth`"
					);
			for (String cov : coverages) {
				query.append(", (`num_pos_"+cov+"`/(`end`-`start`)) as "+cov);
			}
			query.append(" FROM " + Highlander.getCurrentAnalysis().getFromCoverageRegions()
						+ Highlander.getCurrentAnalysis().getJoinCoverage()
						+ "JOIN `projects` USING (`project_id`) "
						+ "JOIN `projects_analyses` USING (project_id) "
						+ "JOIN `pathologies` USING (pathology_id) "
						);
			/*
			 * To fetch genes present in coverage_regions but not in coverage
			 * Currently, I just check the list of given genes and the missing as N/A 
			 * (even genes absent from coverage_regions, like wrong identifier)
			 * It's easier and cover more cases, but if one day e.g. no gene list is given,
			 * but all regions should be fetch, this query will be useful again.
			 * Note that the rest of fetchData() an fillTable() already take care of everything (NULL values)
			 * 
			query.append(" FROM " + Highlander.getCurrentAnalysis().getFromCoverageRegions()
					+ Highlander.getCurrentAnalysis().getJoinCoverage()
					+ "LEFT JOIN `projects` USING (`project_id`) "
					+ "LEFT JOIN `projects_analyses` USING (project_id) "
					+ "LEFT JOIN `pathologies` USING (pathology_id) "
					);
			*/
			query.append("WHERE");
			if (rdbtnGeneList.isSelected()){
				query.append(" gene_symbol IN ("+HighlanderDatabase.makeSqlList(targets, String.class)+")");
				query.append(" AND (analysis = '"+Highlander.getCurrentAnalysis()+"' OR analysis IS NULL)");
			}else{
				
				/*
				 * To rewrite if region functionality is reintroduced
				int maxid = 0;
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT MAX(project_id) FROM gene_coverage;")) {
				if (res.next()){
					maxid = res.getInt(1);
				}
				}
				Set<String> genes = new HashSet<String>();
				for (String target : targets){
					String[] region = target.split("-");
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
							"SELECT gene_symbol FROM gene_coverage WHERE project_id = "+maxid+" AND chr = '"+region[0]+"' AND ((start >= "+region[1]+" AND start <= "+region[2]+") OR (end <= "+region[2]+" AND end >= "+region[1]+"))")){
					while (res.next()){
						genes.add(res.getString("gene_symbol"));
					}
					}
				}
				query.append(" gene_symbol IN ("+HighlanderDatabase.makeSqlList(genes)+")");
				 */
			}
			query.append(" AND (sample IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+") OR sample IS NULL)");
			results.clear();
			int row = 0;
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString())) {
				final int max = res.getResultSetSize();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setProgressString(null, (max == -1));
						if (max != -1) waitingPanel.setProgressMaximum(max);
					}
				});
				while (res.next()){
					waitingPanel.setProgressValue(row+1);
					String gene_symbol = res.getString("gene_symbol");
					if (!results.containsKey(gene_symbol)) {
						results.put(gene_symbol, new TreeMap<>());
					}
					String sample = res.getString("sample").toUpperCase(); //uppercase because I set input to uppercase too
					if (sample == null) sample = "?";
					if (!results.get(gene_symbol).containsKey(sample)) {
						results.get(gene_symbol).put(sample, new TreeMap<>());
					}
					int exon = (res.getObject("exon") != null) ? res.getInt("exon") : 0;
					Double[] values =  new Double[2+coverages.size()];
					values[0] = (res.getObject("length") != null) ? res.getDouble("length") : null;
					values[1] = (res.getObject("mean_depth") != null) ? res.getDouble("mean_depth") : null;
					for (int i=0 ; i < coverages.size() ; i++) {
						values[2+i] = (res.getObject(coverages.get(i)) != null) ? res.getDouble(coverages.get(i)) : null;
					}
					results.get(gene_symbol).get(sample).put(exon,values);
					row++;
				}
			}
			hasTable = true;
			waitingPanel.setProgressDone();
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Coverage information",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		fillTable(grouping);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	private void fillTable(Grouping grouping){
		if (hasTable) {
			try {			
				String[] headers = null;
				Class<?>[] classes = null;
				int offset = 0;
				switch(grouping) {
				case GENE:
					offset = 1;
					break;
				case SAMPLE_GENE:
					offset = 2;
					break;
				case EXON:
					offset = 2;
					break;
				case SAMPLE_EXON:
					offset = 3;
					break;
				}
				headers = new String[offset+1+coverages.size()];
				classes = new Class<?>[offset+1+coverages.size()];
				switch(grouping) {
				case GENE:
					headers[0] = "gene_symbol";
					classes[0] = String.class;
					break;
				case SAMPLE_GENE:
					headers[0] = "sample";
					classes[0] = String.class;
					headers[1] = "gene_symbol";
					classes[1] = String.class;
					break;
				case EXON:
					headers[0] = "gene_symbol";
					classes[0] = String.class;
					headers[1] = "exon";
					classes[1] = Integer.class;
					break;
				case SAMPLE_EXON:
					headers[0] = "sample";
					classes[0] = String.class;
					headers[1] = "gene_symbol";
					classes[1] = String.class;
					headers[2] = "exon";
					classes[2] = Integer.class;
					break;
				}
				headers[offset] = "mean_depth";
				for (int i=0 ; i < coverages.size() ; i++) {
					headers[i+1+offset] = "coverage_above_" + coverages.get(i);
				}
				classes[offset] = Double.class;
				for (int i=0 ; i < coverages.size() ; i++) {
					classes[i+1+offset] = Double.class;
				}

				int rowCount = 0;
				switch(grouping) {
				case GENE:
					rowCount = results.size()+1;
					break;
				case SAMPLE_GENE:
					for (String gene_symbol : results.keySet()) {
						rowCount += results.get(gene_symbol).size();
					}
					break;
				case EXON:
					for (String gene_symbol : results.keySet()) {
						rowCount += results.get(gene_symbol).values().iterator().next().size();
					}
					break;
				case SAMPLE_EXON:
					for (String gene_symbol : results.keySet()) {
						for (String sample : results.get(gene_symbol).keySet()) {
							rowCount += results.get(gene_symbol).get(sample).size();
						}
					}
					break;
				}

				Map<String, Set<String>> missingSamples = new TreeMap<>(new Tools.NaturalOrderComparator(true));
				Set<String> missingGenes = new TreeSet<>(new Tools.NaturalOrderComparator(true)); 
				if (boxShowNA.isSelected()) {
					for (String gene_symbol : targets) {
						if (!results.containsKey(gene_symbol)) {
							missingSamples.put(gene_symbol, new TreeSet<>(samples));
						}else {
							for (String sample : samples) {
								if (!results.get(gene_symbol).containsKey(sample)) {
									if (!missingSamples.containsKey(gene_symbol)) {
										missingSamples.put(gene_symbol, new TreeSet<>());
									}
									missingSamples.get(gene_symbol).add(sample);
								}
							}
						}
					}
					missingGenes.addAll(missingSamples.keySet());
					missingGenes.removeAll(results.keySet());
					switch(grouping) {
					case GENE:
						rowCount += missingGenes.size();
						break;
					case SAMPLE_GENE:
						for (String gene_symbol : missingSamples.keySet()) {
							rowCount  += missingSamples.get(gene_symbol).size();
						}
						break;
					case EXON:
						rowCount += missingGenes.size();
						break;
					case SAMPLE_EXON:
						for (String gene_symbol : missingSamples.keySet()) {
							rowCount  += missingSamples.get(gene_symbol).size();
						}
						break;
					}
				}
				
				Object[][] data = new Object[rowCount][headers.length];
				int row = 0;
				switch(grouping) {
				case GENE:
					row = 1;
					for (String gene_symbol : results.keySet()) {
						Object[] array = new Object[headers.length];
						array[0] = gene_symbol;
						array[1] = 0.0;
						for (int i=0 ; i < coverages.size() ; i++) {
							array[2+i] = 0.0;
						}
						boolean[] notnull = new boolean[headers.length];
						double totalLength = 0.0;
						for (String sample : results.get(gene_symbol).keySet()) {							
							for (int exon : results.get(gene_symbol).get(sample).keySet()) {
								double length = 0;
								if(results.get(gene_symbol).get(sample).get(exon)[0] != null) {
									length = results.get(gene_symbol).get(sample).get(exon)[0];
									totalLength += length;
								}
								if(results.get(gene_symbol).get(sample).get(exon)[1] != null && length > 0) {
									array[1] =  (double)array[1] + (results.get(gene_symbol).get(sample).get(exon)[1] * length);
									notnull[1] = true;
								}
								for (int i=0 ; i < coverages.size() ; i++) {
									if (results.get(gene_symbol).get(sample).get(exon)[2+i] != null && length > 0) {
										array[2+i] = (double)array[2+i] + (results.get(gene_symbol).get(sample).get(exon)[2+i] * length);
										notnull[2+i] = true;
									}
								}
							}
						}
						if (notnull[1]) {
							array[1] = (double)array[1] / totalLength;
						}else {
							array[1] = null;
						}
						for (int i=0 ; i < coverages.size() ; i++) {
							if (notnull[2+i]) {
								array[2+i] = (double)array[2+i] / totalLength;
							}else {
								array[2+i] = null;
							}
						}
						data[row++] = array;
					}
					break;
				case SAMPLE_GENE:
					row = 0;
					for (String gene_symbol : results.keySet()) {
						for (String sample : results.get(gene_symbol).keySet()) {							
							Object[] array = new Object[headers.length];
							array[0] = sample;
							array[1] = gene_symbol;
							array[2] = 0.0;
							for (int i=0 ; i < coverages.size() ; i++) {
								array[3+i] = 0.0;
							}
							boolean[] notnull = new boolean[headers.length];
							double totalLength = 0.0;
							for (int exon : results.get(gene_symbol).get(sample).keySet()) {
								double length = 0;
								if(results.get(gene_symbol).get(sample).get(exon)[0] != null) {
									length = results.get(gene_symbol).get(sample).get(exon)[0];
									totalLength += length;
								}
								if(results.get(gene_symbol).get(sample).get(exon)[1] != null && length > 0) {
									array[2] =  (double)array[2] + (results.get(gene_symbol).get(sample).get(exon)[1] * length);
									notnull[2] = true;
								}
								for (int i=0 ; i < coverages.size() ; i++) {
									if (results.get(gene_symbol).get(sample).get(exon)[2+i] != null && length > 0) {
										array[3+i] = (double)array[3+i] + (results.get(gene_symbol).get(sample).get(exon)[2+i] * length);
										notnull[3+i] = true;
									}
								}
							}
							if (notnull[2]) {
								array[2] = (double)array[2] / totalLength;
							}else {
								array[2] = null;
							}
							for (int i=0 ; i < coverages.size() ; i++) {
								if (notnull[3+i]) {
									array[3+i] = (double)array[3+i] / totalLength;
								}else {
									array[3+i] = null;
								}
							}
							data[row++] = array;
						}
					}
					break;
				case EXON:
					row = 0;
					for (String gene_symbol : results.keySet()) {
						for (int exon : results.get(gene_symbol).values().iterator().next().keySet()) {
							Object[] array = new Object[headers.length];
							array[0] = gene_symbol;
							array[1] = exon;
							array[2] = 0.0;
							for (int i=0 ; i < coverages.size() ; i++) {
								array[3+i] = 0.0;
							}
							boolean[] notnull = new boolean[headers.length];
							double totalLength = 0.0;
							for (String sample : results.get(gene_symbol).keySet()) {							
								double length = 0;
								if(results.get(gene_symbol).get(sample).get(exon)[0] != null) {
									length = results.get(gene_symbol).get(sample).get(exon)[0];
									totalLength += length;
								}
								if(results.get(gene_symbol).get(sample).get(exon)[1] != null && length > 0) {
									array[2] =  (double)array[2] + (results.get(gene_symbol).get(sample).get(exon)[1] * length);
									notnull[2] = true;
								}
								for (int i=0 ; i < coverages.size() ; i++) {
									if (results.get(gene_symbol).get(sample).get(exon)[2+i] != null && length > 0) {
										array[3+i] = (double)array[3+i] + (results.get(gene_symbol).get(sample).get(exon)[2+i] * length);
										notnull[3+i] = true;
									}
								}
							}
							if (notnull[2]) {
								array[2] = (double)array[2] / totalLength;
							}else {
								array[2] = null;
							}
							for (int i=0 ; i < coverages.size() ; i++) {
								if (notnull[3+i]) {
									array[3+i] = (double)array[3+i] / totalLength;
								}else {
									array[3+i] = null;
								}
							}
							data[row++] = array;
						}
					}
					break;
				case SAMPLE_EXON:
					row = 0;
					for (String gene_symbol : results.keySet()) {
						for (String sample : results.get(gene_symbol).keySet()) {
							for (int exon : results.get(gene_symbol).get(sample).keySet()) {
								Object[] array = new Object[headers.length];
								array[0] = sample;
								array[1] = gene_symbol;
								array[2] = exon;
								array[3] = results.get(gene_symbol).get(sample).get(exon)[1];
								for (int i=0 ; i < coverages.size() ; i++) {
									array[4+i] = results.get(gene_symbol).get(sample).get(exon)[2+i];
								}
								data[row++] = array;
							}
						}
					}
					break;
				}

				if (grouping == Grouping.GENE){
					data[0][0] = "[*] All selected genes";
					for (int c=1 ; c < headers.length ; c++){
						data[0][c] = 0.0;					
					}
					for (int r=1 ; r < rowCount ; r++){
						for (int c=1 ; c < headers.length ; c++){
							if (data[r][c] != null) {
								data[0][c] = Double.parseDouble(data[0][c].toString()) + (Double.parseDouble(data[r][c].toString())/(double)(rowCount-1));
							}
						}
					}
				}

				if (boxShowNA.isSelected()) {
					switch(grouping) {
					case GENE:
						for (String gene_symbol : missingGenes) {
							data[row][0] = gene_symbol;
							row++;
						}
						break;
					case SAMPLE_GENE:
						for (String gene_symbol : missingSamples.keySet()) {
							for (String sample : missingSamples.get(gene_symbol)) {
								data[row][0] = sample;
								data[row][1] = gene_symbol;
								row++;
							}
						}
						break;
					case EXON:
						for (String gene_symbol : missingGenes) {
							data[row][0] = gene_symbol;
							data[row][1] = 0;
							row++;
						}
						break;
					case SAMPLE_EXON:
						for (String gene_symbol : missingSamples.keySet()) {
							for (String sample : missingSamples.get(gene_symbol)) {
								data[row][0] = sample;
								data[row][1] = gene_symbol;
								data[row][2] = 0;
								row++;
							}
						}
						break;
					}
				}

				CoverageTableModel tableModel = new CoverageTableModel(data, headers, classes);
				TableRowSorter<CoverageTableModel> sorter = new TableRowSorter<CoverageTableModel>(tableModel);
				JTable table = tables.get(grouping); 
				table.setModel(tableModel);		
				table.setRowSorter(sorter);
				HeatMap heatMap = heatMaps.get(table);
				for (int i=0 ; i < table.getColumnCount() ; i++){
					if (table.getColumnName(i).startsWith("coverage_above_")){
						heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, "0.5", "1.0");
					}else if (table.getColumnName(i).equalsIgnoreCase("mean_depth")){
						heatMap.setHeatMap(i, ColorRange.RGB_RED_TO_GREEN, ConversionMethod.RANGE_GIVEN, depthMinSpinner.getValue().toString(), depthMaxSpinner.getValue().toString());
					}
					int width = 0;
					for (row = 0; row < table.getRowCount(); row++) {
						TableCellRenderer renderer = table.getCellRenderer(row, i);
						Component comp = table.prepareRenderer(renderer, row, i);
						width = Math.max (comp.getPreferredSize().width, width);
					}
					table.getColumnModel().getColumn(i).setPreferredWidth(width+20);
				}
				tableModels.put(grouping, tableModel);
				sorters.put(grouping, sorter);
			} catch (Exception ex) {
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Coverage information",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public void export(Grouping grouping){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile(Tools.formatFilename("Coverage by "+grouping+".xlsx"));
		Tools.centerWindow(chooser, false);
		chooser.setVisible(true) ;
		if (chooser.getFile() != null) {
			String filename = chooser.getDirectory() + chooser.getFile();
			if (!filename.endsWith(".xlsx")) filename += ".xlsx";
			File xls = new File(filename);
			try{
				waitingPanel.start();
				JTable table = tables.get(grouping);
				HeatMap heatMap = heatMaps.get(table);
				try{
					Workbook wb = new SXSSFWorkbook(100);  		
					Sheet sheet = wb.createSheet("coverage info");
					sheet.createFreezePane(0, 1);		
					int r = 0;
					Row row = sheet.createRow(r++);
					row.setHeightInPoints(50);
					for (int c = 0 ; c < table.getColumnCount() ; c++){
						row.createCell(c).setCellValue(table.getColumnName(c));
					}
					sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
					int nrow = table.getRowCount();
					waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" rows", false);
					waitingPanel.setProgressMaximum(nrow);

					Map<String, XSSFCellStyle> styles = new HashMap<String, XSSFCellStyle>();		  	
					for (int i=0 ; i < nrow ; i++ ){
						waitingPanel.setProgressValue(r);
						row = sheet.createRow(r++);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							Cell cell = row.createCell(c);
							if (table.getValueAt(i, c) != null){
								String colname = table.getColumnName(c);
								Object value = table.getValueAt(i, c);
								Color color = null;
								if (value != null){
									if (hasHeatMap.contains(colname)){
										color = heatMap.getColor(i, c);
									}
								}  		
								String styleKey  = generateCellStyleKey(color, !leftAligned.contains(colname), percent.contains(colname), (table.getColumnClass(c) == Long.class || table.getColumnClass(c) == Integer.class));
								if (!styles.containsKey(styleKey)){
									styles.put(styleKey, createCellStyle(sheet, cell, color, !leftAligned.contains(colname), percent.contains(colname), (table.getColumnClass(c) == Long.class || table.getColumnClass(c) == Integer.class)));
								}
								cell.setCellStyle(styles.get(styleKey));
								if (table.getColumnClass(c) == Timestamp.class)
									cell.setCellValue((Timestamp)value);
								else if (table.getColumnClass(c) == Integer.class)
									cell.setCellValue(Integer.parseInt(value.toString()));
								else if (table.getColumnClass(c) == Long.class)
									cell.setCellValue(Long.parseLong(value.toString()));
								else if (table.getColumnClass(c) == Double.class)
									cell.setCellValue(Double.parseDouble(value.toString()));
								else if (table.getColumnClass(c) == Boolean.class)
									cell.setCellValue(Boolean.parseBoolean(value.toString()));
								else 
									cell.setCellValue(value.toString());
							}
						}
					}
					for (int c = 0 ; c < table.getColumnCount() ; c++){
						//sheet.autoSizeColumn(c);
						//Don't work with Java 7, Windows 7 and fonts not installed in the JVM. Here by default it would be Calibri, and then size is evaluated to zero for strings
						//http://stackoverflow.com/questions/16943493/apache-poi-autosizecolumn-resizes-incorrectly
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

	private XSSFCellStyle createCellStyle(Sheet sheet, Cell cell, Color color, boolean centered, boolean percent, boolean number){
		XSSFCellStyle cs = (XSSFCellStyle)sheet.getWorkbook().createCellStyle();
		if (color != null){
			cs.setFillPattern(CellStyle.SOLID_FOREGROUND);
			cs.setFillForegroundColor(new XSSFColor(color));  		
		}
		if (percent){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("0%"));
		}
		if (number){
			cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
		}
		if (centered){
			cs.setAlignment(CellStyle.ALIGN_CENTER);
		}
		cs.setBorderBottom(CellStyle.BORDER_DASHED);
		cs.setBorderTop(CellStyle.BORDER_DASHED);
		cs.setBorderLeft(CellStyle.BORDER_DASHED);
		cs.setBorderRight(CellStyle.BORDER_DASHED);
		return cs;
	}

	private String generateCellStyleKey(Color color, boolean centered, boolean percent, boolean number){
		StringBuilder sb = new StringBuilder();
		if (color != null) sb.append(color.getRGB() + "+");
		if (percent) sb.append("P+");
		if (number) sb.append("N+");
		if (centered) sb.append("C+");
		sb.append("dashed");
		return sb.toString();
	}

}
