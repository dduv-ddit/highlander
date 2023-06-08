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

package be.uclouvain.ngs.highlander.administration.iontorrent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

import com.install4j.api.launcher.ApplicationLauncher;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Parameters.Platform;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.administration.users.LoginBox;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.SampleType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;
import be.uclouvain.ngs.highlander.tools.ExomeBed;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;


public class IonImporter extends JFrame {

	static final public String version = "17.11";

	private static final String VALID = "Valid";	
	private static final String RUN_ID = "Run id";
	private static final String RUN_DATE = "Run date";
	private static final String CALLER = "Caller parameters";
	private static final String PANEL_NAME = "Panel name";
	private static final String BAM = "BAM";
	private static final String BARCODE = "Barcode";
	private static final String SAMPLE = "Sample";
	private static final String INDIVIDUAL = "Individual";
	private static final String FAMILY = "Family";
	private static final String INDEX = "Index case";
	private static final String PATHOLOGY = "Pathology";
	private static final String TYPE = "Type";
	private static final String POPULATION = "Population";
	private static final String PERSON_IN_CHARGE = "Persons in charge";
	private static final String COMMENTS = "Comments";

	private static Parameters parameters;
	private static User user;
	private static HighlanderDatabase DB;

	//TODO JSch cannot connect on new cluster - protocol too old
	private JSch hlJsch;
	private Map<Platform, JSch> sequencerJsch = new HashMap<Platform, JSch>();
	private Session hlSession;
	private Map<Platform, Session> sequencerSession = new HashMap<Platform, Session>();
	private ChannelSftp hlChannel;
	private Map<Platform, ChannelSftp> sequencerChannels = new HashMap<Platform, ChannelSftp>();

	private Map<Platform, String> sequencerResults = new HashMap<Platform, String>();
	private Map<Platform, String> sequencerReferences = new HashMap<Platform, String>();
	private final String hlWorking = "/storage/ngs/highlander/iontorrent";
	private final String hlScript = "/storage/ngs/highlander";
	private final String hlReferences = "/storage/ngs/highlander/reference";

	private final Set<Platform> availablePlatforms = new TreeSet<>();
	
	private String[] users;
	private Map<Platform, String[]> projects = new HashMap<Platform, String[]>();
	private JComboBox<Platform> platformBox;
	private EventList<String> projectsList ;
	private JComboBox<String> projectBox;
	AutoCompleteSupport<String> support;
	private JLabel barcodeNumLabel = new JLabel();
	//private String[] references;
	//private JComboBox<String> referenceBox;
	private Map<Platform, AnalysisFull[]> analyses = new HashMap<Platform, AnalysisFull[]>();
	private JComboBox<AnalysisFull> analysisBox;
	private JTextField kitField;
	private JTextField readLengthField;
	private JCheckBox pairEndCheckBox;
	private Map<String, Integer> pathologies;
	private Map<String, Integer> populations;
	private String[] callers = new String[]{
			"PGM_GERMLINE_LOW_STRINGENCY","PGM_SOMATIC_LOW_STRINGENCY","PROTON_GERMLINE_LOW_STRINGENCY","PROTON_SOMATIC_LOW_STRINGENCY",
	};
	private JScrollPane scroll;
	private ProjectTableModel model;
	private JTable table;
	private JButton importAllButton;
	private JButton importSelectedButton;
	private JComboBox<String> panelBox1 = new JComboBox<>();
	private JTextArea panelDesc;
	DefaultListModel<Reference> panelReferenceListModel = new DefaultListModel<>();
	JList<Reference> panelReferenceList = new JList<>(panelReferenceListModel);
	DefaultListModel<String> panelPanelListModel = new DefaultListModel<>();
	JList<String> panelPanelList = new JList<>(panelPanelListModel);
	DefaultListModel<String> panelGeneListModel = new DefaultListModel<>();
	JList<String> panelGeneList = new JList<>(panelGeneListModel);
	DefaultListModel<String> panelPositionListModel = new DefaultListModel<>();
	JList<String> panelPositionList = new JList<>(panelPositionListModel);

	static private WaitingPanel waitingPanel;
	static private int transferCount = 0;

	public IonImporter(){
		setIconImage(Resources.getScaledIcon(Resources.iIonImporter, 32).getImage());
		setTitle("Ion Torrent and Proton projects manager " + version);
		sequencerResults.put(Platform.ION_TORRENT, "/results/analysis/output/Home");
		sequencerResults.put(Platform.PROTON, "/results/analysis/output/Home");
		sequencerReferences.put(Platform.ION_TORRENT, "/results/referenceLibrary/tmap-f3/");
		sequencerReferences.put(Platform.PROTON, "/results/referenceLibrary/tmap-f3/");
		try {
			for (Platform platform : parameters.getAvailablePlatforms()){
				availablePlatforms.add(platform);
			}
			if (availablePlatforms.isEmpty()) {
				JOptionPane.showMessageDialog(IonImporter.this, "No platform is inaccessible, exiting", "Launching Ion Importer", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				System.exit(-1);
			}
			platformBox = new JComboBox<Platform>(availablePlatforms.toArray(new Platform[0]));
			users = listUsers();
			pathologies = listPathologies();			
			populations = listPopulations();			
			initUI();		
			displayReferences();
			this.addComponentListener(new ComponentListener() {
				@Override
				public void componentShown(ComponentEvent arg0) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							platformBox.setSelectedItem(Platform.PROTON);
						}
					}, "Connection to PROTON").start();
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
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(IonImporter.this, Tools.getMessage("Error", ex), "Launching Ion Importer", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	private void initUI(){
		getContentPane().setLayout(new BorderLayout());
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		JPanel panel = new JPanel(new BorderLayout());
		tabbedPane.addTab("Import samples from Ion/Proton Server to Highlander", null, panel, null);

		JPanel northPanel = new JPanel(new BorderLayout());
		panel.add(northPanel, BorderLayout.NORTH);

		JPanel projectPanel = new JPanel(new BorderLayout());
		northPanel.add(projectPanel, BorderLayout.NORTH);

		platformBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					new Thread(new Runnable() {
						@Override
						public void run() {
							if (!projects.containsKey(platformBox.getSelectedItem())) {
								if (!getPlatformData((Platform)platformBox.getSelectedItem())) {
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											projectsList.clear();
											projectBox.validate();
											projectBox.repaint();
											analysisBox.removeAllItems();
											analysisBox.validate();
											analysisBox.repaint();									
										}
									});
									return;
								}
							}
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(true);
									waitingPanel.setProgressString("Retrieving projects", true);
									waitingPanel.start();
								}
							});
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									projectsList.clear();
									for (String pr : projects.get(platformBox.getSelectedItem())){
										projectsList.add(pr);
									}
									projectBox.validate();
									projectBox.repaint();
									analysisBox.removeAllItems();
									for (AnalysisFull a : analyses.get(platformBox.getSelectedItem())) {
										analysisBox.addItem(a);
									}
									analysisBox.validate();
									analysisBox.repaint();									
								}
							});
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(false);
									waitingPanel.stop();
								}
							});
						}
					}).start();
				}
			}
		});
		projectPanel.add(platformBox, BorderLayout.WEST);

		String[] values = new String[] {"Retrieving project list ..."};
		projectBox = new JComboBox<>(values);
		projectsList = GlazedLists.eventListOf(values);
		projectBox.setMaximumRowCount(20);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				support = AutoCompleteSupport.install(projectBox, projectsList);
				support.setCorrectsCase(true);
				support.setFilterMode(TextMatcherEditor.CONTAINS);
				support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				support.setStrict(false);
			}
		});
		projectBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("comboBoxEdited")){
					if (projectBox.getSelectedIndex() < 0) projectBox.setSelectedItem(null);
				}
			}
		});
		projectBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (projectBox.getSelectedIndex() >= 0) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										waitingPanel.setVisible(true);
										waitingPanel.setProgressString("Retrieving BAM list", true);
										waitingPanel.start();
									}
								});
								fillTable();
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										waitingPanel.setVisible(false);
										waitingPanel.stop();
									}
								});
							}
						}).start();
					}
				}
			}
		});
		projectPanel.add(projectBox, BorderLayout.CENTER);

		JPanel referencePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		northPanel.add(referencePanel, BorderLayout.CENTER);

		JLabel analysisLabel = new JLabel("Highlander analysis");
		referencePanel.add(analysisLabel);
		analysisBox = new JComboBox<>();
		analysisBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					panelBox1.removeAllItems();
					panelBox1.addItem("Select the associated panel");
					for (String panel : listPanels(((AnalysisFull)analysisBox.getSelectedItem()).getReference())){
						panelBox1.addItem(panel);	
					}
					panelBox1.validate();
					panelBox1.repaint();									
				}				
			}
		});
		referencePanel.add(analysisBox);
		
		JLabel panelLabel = new JLabel("Panel");
		referencePanel.add(panelLabel);
		panelBox1 = new JComboBox<>();
		panelBox1.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED && panelBox1.getSelectedIndex() > 0){
					String item = panelBox1.getSelectedItem().toString();
					for (int row=0 ; row < table.getRowCount() ; row++){
						table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(PANEL_NAME)));
					}
					refreshTable();
				}				
			}
		});
		referencePanel.add(panelBox1);
		/*
		JLabel refLabel = new JLabel("Reference");
		referencePanel.add(refLabel);
		referenceBox = new JComboBox<>(references);
		referenceBox.setSelectedItem("default");
		referencePanel.add(referenceBox);
		 */

		referencePanel.add(new JLabel("Kit"));
		kitField = new JTextField("AmpliSeq");
		kitField.setColumns(10);
		referencePanel.add(kitField);

		referencePanel.add(new JLabel("Read length"));
		readLengthField = new JTextField("2x200bp");
		readLengthField.setColumns(10);
		referencePanel.add(readLengthField);
		
		pairEndCheckBox = new JCheckBox("Pair-end");
		pairEndCheckBox.setSelected(true);
		referencePanel.add(pairEndCheckBox);
		
		referencePanel.add(barcodeNumLabel);

		JPanel fillToolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		northPanel.add(fillToolsPanel, BorderLayout.SOUTH);

		JButton setRunIdButton = new JButton("Set run id");
		setRunIdButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Set the run id: it must be the same number than on the Ion Server", "Run id",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
				if (res != null){
					try{
						int item = Integer.parseInt(res.toString());
						for (int row=0 ; row < table.getRowCount() ; row++){
							table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(RUN_ID)));
						}
						refreshTable();
					}catch(NumberFormatException ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(IonImporter.this, "You must enter a valid number", "Run id", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		});
		fillToolsPanel.add(setRunIdButton);

		JButton setRunDateButton = new JButton("Set run date");
		setRunDateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Set the run date in this format: YYYY_MM_DD", "Run date",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
				if (res != null){
					String item = res.toString();
					if (item.length() != 10 || 
							item.split("_").length != 3 || 
							Integer.parseInt(item.split("_")[0]) < 2000 || 
							Integer.parseInt(item.split("_")[0]) > 2500 || 
							Integer.parseInt(item.split("_")[1]) < 1 || 
							Integer.parseInt(item.split("_")[1]) > 12 || 
							Integer.parseInt(item.split("_")[2]) < 1 || 
							Integer.parseInt(item.split("_")[2]) > 31 
							){
						JOptionPane.showMessageDialog(IonImporter.this, "You must enter a valid date in the format YYYY_MM_DD", "Run date", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));						
					}else{
						for (int row=0 ; row < table.getRowCount() ; row++){
							table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(RUN_DATE)));
						}
						refreshTable();
					}
				}
			}
		});
		fillToolsPanel.add(setRunDateButton);

		JButton setPathologyButton = new JButton("Set pathology");
		setPathologyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] pathos = pathologies.keySet().toArray(new String[0]);
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Set the pathology of selected samples", "Pathology",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), pathos, null);
				if (res != null){
					String item = res.toString();
					for (int row : table.getSelectedRows()){
						table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(PATHOLOGY)));
					}
					refreshTable();
				}
			}
		});
		fillToolsPanel.add(setPathologyButton);

		JButton setIndexButton = new JButton("Set index case");
		setIndexButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Set YES if the individual is the index case of the family", "Index case",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), new String[] {"YES","NO"}, null);
				if (res != null){
					String item = res.toString();
					for (int row : table.getSelectedRows()){
						table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(INDEX)));
					}
					refreshTable();
				}
			}
		});
		fillToolsPanel.add(setIndexButton);
		
		JButton setPopulationButton = new JButton("Set population");
		setPopulationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] pathos = populations.keySet().toArray(new String[0]);
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Set the population of selected patients", "Population",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), pathos, null);
				if (res != null){
					String item = res.toString();
					for (int row : table.getSelectedRows()){
						table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(POPULATION)));
					}
					refreshTable();
				}
			}
		});
		fillToolsPanel.add(setPopulationButton);
		
		JButton setCallerButton = new JButton("Set caller parameters");
		setCallerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Set the caller parameters of selected samples", "Caller parameters",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), callers, null);
				if (res != null){
					String item = res.toString();
					for (int row : table.getSelectedRows()){
						table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(CALLER)));
					}
					refreshTable();
				}
			}
		});
		fillToolsPanel.add(setCallerButton);

		JButton setTypeButton = new JButton("Set sample type");
		setTypeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Set the type of selected samples", "Sample type",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), SampleType.values(), null);
				if (res != null){
					String item = res.toString();
					for (int row : table.getSelectedRows()){
						table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(TYPE)));
					}
					refreshTable();
				}
			}
		});
		fillToolsPanel.add(setTypeButton);

		JButton setPersonInChargeButton = new JButton("Add person in charge");
		setPersonInChargeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(IonImporter.this,  "Add a person in charge for selected samples", "Person in charge",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), users, null);
				if (res != null){
					String item = res.toString();
					for (int row : table.getSelectedRows()){
						Object val = table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PERSON_IN_CHARGE)));
						if (val != null && val.toString().length() > 0){
							if (!val.toString().contains(item)){
								table.setValueAt(val + "," + item, row, table.convertColumnIndexToView(model.getColumn(PERSON_IN_CHARGE)));
							}
						}else{
							table.setValueAt(item, row, table.convertColumnIndexToView(model.getColumn(PERSON_IN_CHARGE)));
						}
					}
					refreshTable();
				}
			}
		});
		fillToolsPanel.add(setPersonInChargeButton);

		table = new JTable();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		new ExcelAdapter(table);		
		table.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent arg0) {
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER ||
						arg0.getKeyCode() == KeyEvent.VK_LEFT ||
						arg0.getKeyCode() == KeyEvent.VK_RIGHT ||
						arg0.getKeyCode() == KeyEvent.VK_UP ||
						arg0.getKeyCode() == KeyEvent.VK_DOWN){
					refreshTable();
				}				
			}
		});
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				importSelectedButton.setText("Import "+table.getSelectedRowCount()+" SELECTED samples in Highlander");

			}
		});
		scroll = new JScrollPane(table);
		panel.add(scroll, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		panel.add(southPanel, BorderLayout.SOUTH);

		importAllButton = new JButton("Import ALL samples in Highlander", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importSamples(true);
					}
				}, "IonImporter.importSamples").start();
				
			}
		});
		southPanel.add(importAllButton);
		
		importSelectedButton = new JButton("Import SELECTED samples in Highlander", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importSelectedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importSamples(false);
					}
				}, "IonImporter.importSamples").start();

			}
		});
		southPanel.add(importSelectedButton);

		JPanel panel2 = new JPanel(new BorderLayout());
		tabbedPane.addTab("Panel management", null, panel2, null);

		panelDesc = new JTextArea();
		panelDesc.setEditable(false);
		panelDesc.setLineWrap(true);
		panelDesc.setWrapStyleWord(true);
		panel2.add(panelDesc, BorderLayout.NORTH);

		JPanel center2 = new JPanel();
		GridBagLayout gbl_panel_center2 = new GridBagLayout();
		gbl_panel_center2.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_panel_center2.rowHeights = new int[]{0, 0};
		gbl_panel_center2.columnWeights = new double[]{1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
		gbl_panel_center2.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		center2.setLayout(gbl_panel_center2);
		panel2.add(center2, BorderLayout.CENTER);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.insets = new Insets(5, 10, 5, 10);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 0;
		center2.add(scrollPane_1, gbc_scrollPane_1);
		panelReferenceList.setBorder(new TitledBorder(null, "References", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelReferenceList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					displayPanels();
				}
			}
		});
		scrollPane_1.setViewportView(panelReferenceList);

		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.insets = new Insets(5, 10, 5, 10);
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 1;
		gbc_scrollPane_2.gridy = 0;
		center2.add(scrollPane_2, gbc_scrollPane_2);		
		panelPanelList.setBorder(new TitledBorder(null, "Panels", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelPanelList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					displayPanelDetails();
				}
			}
		});
		scrollPane_2.setViewportView(panelPanelList);

		JScrollPane scrollPane_3 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_3 = new GridBagConstraints();
		gbc_scrollPane_3.insets = new Insets(5, 10, 5, 10);
		gbc_scrollPane_3.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_3.gridx = 2;
		gbc_scrollPane_3.gridy = 0;
		center2.add(scrollPane_3, gbc_scrollPane_3);		
		panelGeneList.setBorder(new TitledBorder(null, "Genes", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		scrollPane_3.setViewportView(panelGeneList);
		
		JScrollPane scrollPane_4 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_4 = new GridBagConstraints();
		gbc_scrollPane_4.insets = new Insets(5, 10, 5, 10);
		gbc_scrollPane_4.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_4.gridx = 3;
		gbc_scrollPane_4.gridy = 0;
		center2.add(scrollPane_4, gbc_scrollPane_4);
		panelPositionList.setBorder(new TitledBorder(null, "Positions", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		scrollPane_4.setViewportView(panelPositionList);
		
		JPanel south2 = new JPanel(new FlowLayout());
		panel2.add(south2, BorderLayout.SOUTH);

		JButton newPanelButton = new JButton("Create new panel", Resources.getScaledIcon(Resources.iDbAdd, 16));
		newPanelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createNewPanel();
					}
				}, "IonImporter.createNewPanel").start();

			}
		});
		south2.add(newPanelButton);

		JButton coverageButton = new JButton("View panel amplicons coverage", Resources.getScaledIcon(Resources.iCoverage, 16));
		coverageButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {				
						if (panelReferenceList.getSelectedValue().getName().equals("hg19_lifescope")) {
							IonCoverage viewer = new IonCoverage(panelReferenceList.getSelectedValue(), panelPanelList.getSelectedValue());
							Tools.centerWindow(viewer, false);
							//viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
							viewer.setVisible(true);							
						}else {
							/* Assumes that all ion torrent data goes to panels_torrent_caller
							 * If at some point, different torrent analyses are used (e.g. hg19 & hg38),
							 * I would need to add support in the GUI
							 */
							JOptionPane.showMessageDialog(IonImporter.this, "Only Ion Torrent and Proton panels with reference hg19_lifescope are supported by this tool.\nCoverage of other platforms/references is accessible from Highlander.", "Coverage information on panel " + panelPanelList.getSelectedValue(), JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iCoverage, 64));
						}
					}
				}, "IonImporter.coverageButton").start();

			}
		});
		south2.add(coverageButton);

		JButton exportBedButton = new JButton("Export bed positions to Highlander", Resources.getScaledIcon(Resources.iUserIntervalsNew, 16));
		exportBedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {										
						try {
							List<Interval> intervals = new ArrayList<Interval>();
							Enumeration<String> e = panelPositionListModel.elements();
							while (e.hasMoreElements()){
								intervals.add(new Interval(panelReferenceList.getSelectedValue(), e.nextElement()));
							}
							String listName = ProfileTree.showProfileDialog(IonImporter.this, Action.SAVE, UserData.INTERVALS, panelReferenceList.getSelectedValue().getName(), "Export "+UserData.INTERVALS+" to Highlander", panelPanelList.getSelectedValue());
							if (listName == null) return;
							if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.INTERVALS, panelReferenceList.getSelectedValue().getName(), listName)){
								int yesno = JOptionPane.showConfirmDialog(IonImporter.this, 
										"You already have a "+UserData.INTERVALS.getName()+" named '"+listName.replace("~", " -> ")+"', do you want to overwrite it ?", 
										"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
								if (yesno == JOptionPane.NO_OPTION)	return;
							}
							user.saveIntervals(listName, panelReferenceList.getSelectedValue(), intervals);
							JOptionPane.showMessageDialog(IonImporter.this, "A new list of intervals named '"+listName+"' has been created in your Highlander profile.", 
									"Export bed positions to Highlander", JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iUserIntervalsNew,64));
						} catch (Exception ex) {
							Tools.exception(ex);
						}
					}
				}, "IonImporter.exportBedButton").start();

			}
		});
		south2.add(exportBedButton);

		JButton exportGeneListButton = new JButton("Export gene list to Highlander", Resources.getScaledIcon(Resources.iUserListNew, 16));
		exportGeneListButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							List<String> genes = new ArrayList<String>();
							Enumeration<?> e = panelGeneListModel.elements();
							while (e.hasMoreElements()){
								genes.add((String) e.nextElement());
							}
							String listName = ProfileTree.showProfileDialog(IonImporter.this, Action.SAVE, UserData.VALUES, "gene_symbol", "Export gene list to Highlander", panelPanelList.getSelectedValue() + " gene list");
							if (listName == null) return;
							if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.VALUES, "gene_symbol", listName)){
								int yesno = JOptionPane.showConfirmDialog(IonImporter.this, 
										"You already have a "+UserData.VALUES.getName()+" named '"+listName.replace("~", " -> ")+"', do you want to overwrite it ?", 
										"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
								if (yesno == JOptionPane.NO_OPTION)	return;
							}
							user.saveValues(listName, Field.gene_symbol, genes);
							JOptionPane.showMessageDialog(IonImporter.this, "A new list of values named '"+listName+"' has been created in your Highlander profile.", 
									"Export gene list to Highlander", JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iUserListNew,64));
						} catch (Exception ex) {
							Tools.exception(ex);
						}
					}
				}, "IonImporter.exportGeneListButton").start();

			}
		});
		south2.add(exportGeneListButton);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	private void refreshTable(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					validateSamples();
					model.fireTableRowsUpdated(0,model.getRowCount()-1);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		});	
	}

	private boolean getPlatformData(Platform platform) {
		if (connectToSequencer(platform)) {
			try {
				projects.put(platform, listProjects(platform));
			}catch(SftpException ex) {
				JOptionPane.showMessageDialog(IonImporter.this, "Problem when trying to list project directories on "+platform+".", "Retrieving projects from " + platform, JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;
			}
			try {
				analyses.put(platform, listAnalyses(platform));
			}catch(Exception ex) {
				JOptionPane.showMessageDialog(IonImporter.this, "Problem when trying to list Highlander analyses for "+platform+".", "Retrieving analyses from " + platform, JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;
			}
			/*
			try {			
				references = listReferences(platform);
			}catch(Exception ex) {
				JOptionPane.showMessageDialog(IonImporter.this, "Problem when trying to list references for "+platform+".", "Retrieving references from " + platform, JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;
			}
			*/
			disconnectFromSequencer(platform);
			return true;
		}
		return false;
	}
	
	private void fillTable(){
		try{
			String[] bams = listBam(platformBox.getItemAt(platformBox.getSelectedIndex()));
			Object[][] data = new Object[bams.length][16];
			String[] headers = new String[]{
					VALID,
					RUN_ID,
					RUN_DATE,
					PANEL_NAME,
					CALLER,
					BAM,
					BARCODE,
					SAMPLE,
					INDIVIDUAL,
					FAMILY,
					INDEX,
					PATHOLOGY,
					TYPE,
					POPULATION,
					PERSON_IN_CHARGE,
					COMMENTS,
			};			
			model = new ProjectTableModel(data, headers, bams);
			table.setModel(model);
			barcodeNumLabel.setText("Barcodes : " + bams.length+" barcodes detected");
			importAllButton.setText("Import ALL "+bams.length+" samples in Highlander");
			refreshTable();
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	public class ProjectTableModel	extends AbstractTableModel {
		private Object[][] data;
		private String[] headers;

		public ProjectTableModel(Object[][] data, String[] headers, String[] bams) {    	
			this.data = data;
			this.headers = headers;
			String panel = "";
			for (int i=0 ; i < bams.length ; i++){
				data[i][getColumn(BAM)] = bams[i];
				data[i][getColumn(BARCODE)] = bams[i].split("_")[1]; 
				try{
					try (Results res = DB.select(Schema.HIGHLANDER, 
							"SELECT * "
							+ "FROM ion_importer "
							+ "JOIN pathologies USING (pathology_id) "
							+ "LEFT JOIN populations USING (population_id) "
							+ "WHERE ANALYSIS = '"+projectBox.getSelectedItem().toString()+"' AND BAM = '"+bams[i]+"'")) {
						if (res.next()){
							data[i][getColumn(RUN_ID)] = res.getInt("RUN_ID");
							data[i][getColumn(RUN_DATE)] = res.getString("RUN_DATE");
							data[i][getColumn(PANEL_NAME)] = res.getString("PANEL_NAME");
							if (res.getString("PANEL_NAME").length() > panel.length()) panel = res.getString("PANEL_NAME"); 
							data[i][getColumn(CALLER)] = res.getString("CALLER");
							data[i][getColumn(SAMPLE)] = res.getString("SAMPLE");
							data[i][getColumn(INDIVIDUAL)] = res.getString("INDIVIDUAL");
							data[i][getColumn(FAMILY)] = res.getString("FAMILY");
							data[i][getColumn(INDEX)] = (res.getBoolean("INDEX")) ? "YES" : "NO";
							data[i][getColumn(PATHOLOGY)] = res.getString("pathology");
							data[i][getColumn(TYPE)] = res.getString("TYPE");
							data[i][getColumn(POPULATION)] = res.getString("population");
							data[i][getColumn(PERSON_IN_CHARGE)] = res.getString("PERSON_IN_CHARGE");
							data[i][getColumn(COMMENTS)] = res.getString("COMMENTS");
						}
					}
					if (panel.length() > 0) panelBox1.setSelectedItem(panel);
					else panelBox1.setSelectedIndex(0);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
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

		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) return ImageIcon.class;
			return String.class;
		}

		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		public void setValueAt(Object value, int row, int col) {
			data[row][col] = value;
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == getColumn(SAMPLE) || columnIndex == getColumn(INDIVIDUAL) || columnIndex == getColumn(FAMILY) || columnIndex == getColumn(COMMENTS);
		}

	}

	public class ExcelAdapter implements ActionListener {
		private String rowstring,value;
		private Clipboard system;
		private JTable table ;
		/**
		 * The Excel Adapter is constructed with a
		 * JTable on which it enables Copy-Paste and acts
		 * as a Clipboard listener.
		 */
		public ExcelAdapter(JTable myJTable){
			table = myJTable;
			KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(),false);
			table.registerKeyboardAction(this,"Paste",paste,JComponent.WHEN_FOCUSED);
			KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0,false);
			table.registerKeyboardAction(this,"Delete",delete,JComponent.WHEN_FOCUSED);
			system = Toolkit.getDefaultToolkit().getSystemClipboard();
		}
		/**
		 * Public Accessor methods for the Table on which this adapter acts.
		 */
		public JTable getJTable() {return table;}
		public void setJTable(JTable jTable1) {this.table=jTable1;}
		/**
		 * This method is activated on the Keystrokes we are listening to
		 * in this implementation. Here it listens for Copy and Paste ActionCommands.
		 * Selections comprising non-adjacent cells result in invalid selection and
		 * then copy action cannot be performed.
		 * Paste is done by aligning the upper left corner of the selection with the
		 * 1st element in the current selection of the JTable.
		 */
		public void actionPerformed(ActionEvent e){
			if (e.getActionCommand().compareTo("Paste")==0){
				int startRow=(table.getSelectedRows())[0];
				int startCol=(table.getSelectedColumns())[0];
				try	{
					String trstring= ((String)(system.getContents(this).getTransferData(DataFlavor.stringFlavor))).replace("\r", "\n");
					String[] st1= trstring.split("\n");
					for(int i=0; i < st1.length ;i++)	{
						rowstring=st1[i];
						String[] st2= rowstring.split("\t");
						for(int j=0; j < st2.length ;j++)	{
							value= st2[j];
							if (startRow+i< table.getRowCount()  &&
									startCol+j< table.getColumnCount())
								table.setValueAt(value,startRow+i,startCol+j);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				refreshTable();
			}else if (e.getActionCommand().compareTo("Delete")==0){
				try	{
					for (int row : table.getSelectedRows()){
						for (int col : table.getSelectedColumns()){
							table.setValueAt(null,row,col);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				refreshTable();
			}
		}
	}

	private void validateSamples(){
		for (int row = 0 ; row < model.getRowCount() ; row++){
			int check = checkSample(row);
			if (check == 2){
				model.setValueAt(Resources.getScaledIcon(Resources.iQuestion, 12), row, 0);
			}else if (check == 1){
				model.setValueAt(Resources.getScaledIcon(Resources.iButtonApply, 12), row, 0);
			}else{
				model.setValueAt(Resources.getScaledIcon(Resources.iCross, 12), row, 0);				
			}
		}
	}

	private int checkSample(int row) {
		String panel = ".";
		boolean valid = true;
		boolean exists = false;
		Object o;
		o = model.getValueAt(row, model.getColumn(RUN_ID));
		if (o == null || o.toString().length() == 0 ) return 0;
		o = model.getValueAt(row, model.getColumn(RUN_DATE));
		if (o == null || o.toString().length() == 0 ) return 0;
		o = model.getValueAt(row, model.getColumn(PANEL_NAME));
		if (o == null || o.toString().length() == 0 ) return 0;
		else panel = "."+o.toString();
		o = model.getValueAt(row, model.getColumn(CALLER));
		if (o == null || o.toString().length() == 0 ) return 0;
		o = model.getValueAt(row, model.getColumn(INDIVIDUAL));
		if (o == null || o.toString().length() == 0) return 0;
		o = model.getValueAt(row, model.getColumn(FAMILY));
		if (o == null || o.toString().length() == 0 ) return 0;
		o = model.getValueAt(row, model.getColumn(INDEX));
		if (o == null || o.toString().length() == 0 ) return 0;
		o = model.getValueAt(row, model.getColumn(PATHOLOGY));
		if (o == null || o.toString().length() == 0 ) return 0;
		if (!pathologies.containsKey(o.toString())) return 0;
		o = model.getValueAt(row, model.getColumn(POPULATION));
		if (o != null && o.toString().length() > 0 && !populations.containsKey(o.toString())) return 0;
		o = model.getValueAt(row, model.getColumn(TYPE));
		if (o == null || o.toString().length() == 0 ) return 0;
		o = model.getValueAt(row, model.getColumn(PERSON_IN_CHARGE));
		if (o == null || o.toString().length() == 0 )	return 0;
		o = model.getValueAt(row, model.getColumn(SAMPLE));
		if (o == null || o.toString().length() == 0) {
			return 0;
		}else{	
			Pattern p = Pattern.compile("[^a-z0-9\\-]", Pattern.CASE_INSENSITIVE);
			if(p.matcher(o.toString()).find()){
				return 0;
			}else{
				o = o.toString().replace('_', '-').trim().replace(' ', '-');
				try{
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM projects WHERE platform IN ('ION_TORRENT','PROTON') AND sample = '"+DB.format(Schema.HIGHLANDER, o.toString().trim()+panel)+"'")) {
						if (res.next()){
							int count = res.getInt(1);
							if (count > 0) exists = true;
						}
					}
				}catch(Exception ex){
					Tools.exception(ex);
				}				
			}
		}
		if (exists){
			return 2;
		}else if (valid){
			return 1;
		}else{
			return 0;				
		}
	}

	private void importSamples(boolean all){
		if (panelBox1.getSelectedIndex() == 0){
			JOptionPane.showMessageDialog(IonImporter.this, "You MUST select a panel before importing into Highlander !", "Importation in Highlander", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		int[] selection;
		if (all){
			selection = new int[table.getRowCount()];
			for (int i=0 ; i < table.getRowCount() ; i++){
				selection[i] = i;
			}
		}else{
			selection = table.getSelectedRows();
		}
		int finalCheck = 1;
		for (int row : selection){
			int check = checkSample(row);
			if (check == 2){
				finalCheck = 2;
			}else if (check == 0){
				finalCheck = 0;
				break;
			}
		}
		int res = JOptionPane.YES_OPTION;
		if (finalCheck == 0){
			JOptionPane.showMessageDialog(IonImporter.this, "Some information is missing (or you use a special character other than '-' in your sample id), please fill all information for samples you want to import", "Importation in Highlander", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}else{ 
			if (finalCheck == 2){
				res = JOptionPane.showConfirmDialog(IonImporter.this, "Some samples are already present in the database, do you want to replace them?", "Importation in Highlander", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iQuestion,64));
			}
			if (res == JOptionPane.YES_OPTION){
				try{
					System.out.println("Creating project in Highlander");
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							waitingPanel.setProgressDone();
							waitingPanel.setProgressString("Creating project in Highlander", true);
						}
					});					for (int row : selection){
						String panel = table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PANEL_NAME))).toString();
						String sample = DB.format(Schema.HIGHLANDER, table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel);
						if (checkSample(row) == 2){
							int id = -1;
							try (Results rs = DB.select(Schema.HIGHLANDER, "SELECT project_id FROM projects WHERE platform IN ('ION_TORRENT','PROTON') AND sample = '"+sample+"'")){
								if (rs.next()){
									id = rs.getInt(1);
								}
							}
							if (id >= 0){
								DB.update(Schema.HIGHLANDER, "DELETE FROM projects WHERE project_id = " + id);
								DB.update(Schema.HIGHLANDER, "DELETE FROM `projects_users` WHERE `project_id` = "+id);
								DB.update(Schema.HIGHLANDER, "DELETE FROM `projects_analyses` WHERE `project_id` = "+id);
							}
							DB.update(Schema.HIGHLANDER, "DELETE FROM ion_importer WHERE BAM = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(BAM))).toString()+"' AND ANALYSIS = '"+projectBox.getSelectedItem().toString()+"'");
						}
						DB.insert(Schema.HIGHLANDER, "REPLACE INTO `ion_importer` SET" + 
								" `RUN_ID` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(RUN_ID)))+"'," +
								" `RUN_DATE` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(RUN_DATE)))+"'," +
								" `PANEL_NAME` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PANEL_NAME)))+"'," +
								" `CALLER` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(CALLER)))+"'," +
								" `BAM` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(BAM)))+"'," +
								" `BARCODE` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(BARCODE)))+"'," +
								" `SAMPLE` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-') +"'," +
								" `INDIVIDUAL` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(INDIVIDUAL))).toString().replace('_', '-').trim().replace(' ', '-') +"'," +
								" `FAMILY` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(FAMILY))).toString().replace('_', '-').trim().replace(' ', '-') +"'," +
								" `INDEX` = "+((table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(INDEX))).toString().equals("YES")) ? 1 : 0)+"," +
								" `pathology_id` = "+pathologies.get(table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PATHOLOGY))).toString())+", "+
								" `TYPE` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(TYPE)))+"'," +
								" `population_id` = "+((table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(POPULATION))) != null && table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(POPULATION))).toString().length() > 0)?populations.get(table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(POPULATION))).toString()):"NULL")+", "+
								" `PERSON_IN_CHARGE` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PERSON_IN_CHARGE)))+"'," +
								((table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(COMMENTS))) != null)?"`COMMENTS` = '"+DB.format(Schema.HIGHLANDER, table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(COMMENTS))).toString())+"', ":"`COMMENTS` = '', ") + 
								" `ANALYSIS` = '"+projectBox.getSelectedItem().toString()+"'");
						int id = DB.insertAndGetAutoId(Schema.HIGHLANDER, "INSERT INTO `projects` SET" +
								" `sequencing_target` = 'Panel', " +
								" `platform` = '"+((Platform)platformBox.getSelectedItem()).toString()+"', " +
								" `outsourcing` = 'PGEN', " +
								" `family` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(FAMILY))).toString().replace('_', '-').trim().replace(' ', '-')+"', " +
								" `individual` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(INDIVIDUAL))).toString().replace('_', '-').trim().replace(' ', '-')+"', " +
								" `sample` = '"+sample+"', " +
								" `index_case` = "+((table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(INDEX))).toString().equals("YES")) ? 1 : 0)+", " +
								" `pathology_id` = "+pathologies.get(table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PATHOLOGY))).toString())+", " +
								" `population_id` = "+((table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(POPULATION))) != null && table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(POPULATION))).toString().length() > 0)?populations.get(table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(POPULATION))).toString()):"NULL")+", "+
								" `sample_type` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(TYPE)))+"', " +
								" `barcode` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(BARCODE)))+"', " +
								" `kit` = '"+kitField.getText()+"', " +
								" `read_length` = '"+readLengthField.getText()+"', " +
								" `pair_end` = "+(pairEndCheckBox.isSelected() ? "TRUE" : "FALSE")+", " +
								" `trim` = FALSE, " +
								" `remove_duplicates` = FALSE, " +
								" `run_id` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(RUN_ID)))+"', " +
								" `run_date` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(RUN_DATE)))+"', " +
								" `run_name` = '"+DB.format(Schema.HIGHLANDER, table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PANEL_NAME))).toString().substring(1))+"', " +
								" `run_label` = '"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(RUN_ID)))+"_"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(RUN_DATE))).toString().replace("-", "_")+"_"+DB.format(Schema.HIGHLANDER, table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PANEL_NAME))).toString().substring(1))+"', " +
								((table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(COMMENTS))) != null)?"`comments` = '"+DB.format(Schema.HIGHLANDER, table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(COMMENTS))).toString())+"'":"`comments` = ''"));
						for (String user : table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(PERSON_IN_CHARGE))).toString().split(",")){
							DB.insert(Schema.HIGHLANDER, "INSERT INTO `projects_users` SET `project_id` = "+id+", `username` = '"+user+"'");
						}
					}		
					String project = ""+table.getValueAt(selection[0], table.convertColumnIndexToView(model.getColumn(RUN_ID)));
					project += "_" + table.getValueAt(selection[0], table.convertColumnIndexToView(model.getColumn(RUN_DATE))).toString();
					String panel = table.getValueAt(selection[0], table.convertColumnIndexToView(model.getColumn(PANEL_NAME))).toString();
					project += "_" + panel.substring(1);
					Platform platform = platformBox.getItemAt(platformBox.getSelectedIndex());
					Reference reference = ((AnalysisFull)analysisBox.getSelectedItem()).getReference();
					String sequencerPath = projectBox.getSelectedItem().toString();
					//sequencerChannels.get(platform).cd(sequencerResults.get(platform)+"/"+projectBox.getSelectedItem().toString());
					ExecutorService executor = Executors.newFixedThreadPool(10);
					transferCount = 0;
					System.out.println("Transferring BAM from " + platform + " to Highlander Server");
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							waitingPanel.setProgressMaximum(selection.length);
							waitingPanel.setProgressValue(transferCount);
							waitingPanel.setProgressString("Transferring BAM from " + platform + " to Highlander Server", false);
						}
					});
					for (int row : selection){
						/*
						 * Now using rsync
						 * 
						File targetBam = new File(project+"/"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel+".bam");
						if (!targetBam.exists()){
							sequencerChannels.get(platform).get(table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(BAM))).toString(),targetBam.toString(), monitor, mode);
						}
						File targetBai = new File(project+"/"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel+".bam.bai");
						if (!targetBai.exists()){
							sequencerChannels.get(platform).get(table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(BAM))).toString()+".bai",targetBai.toString(), monitor, mode);
						}
						*/
						String sequencerLibrary = table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(BAM))).toString().replace(".bam", "");
						String highlanderSample = table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel;
						executor.execute(new RSync(platform, sequencerPath, sequencerLibrary, project, highlanderSample));
					}
					executor.shutdown();
					executor.awaitTermination(5, TimeUnit.HOURS);
					if (connectToHighlander()) {
						SftpProgressMonitor monitor = new MyProgressMonitor();
						int mode=ChannelSftp.OVERWRITE;
						File localDir = new File(project);
						localDir.mkdir();
						System.out.println("Downloading targeted BED file");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Downloading targeted BED file", true);
							}
						});
						hlChannel.cd(hlReferences+"/"+reference.getName()+"/panels");
						File bedFile = new File(project+"/"+project+".bed");
						if (!bedFile.exists()){
							hlChannel.get(panelBox1.getSelectedItem().toString()+".bed",bedFile.toString(), monitor, mode);					
						}
						System.out.println("Downloading full genes BED file");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Downloading full genes BED file", true);
							}
						});
						hlChannel.cd(hlReferences+"/"+reference.getName()+"/panels");
						File fullBedFile = new File(project+"/"+project+".fullgenes.bed");
						if (!fullBedFile.exists()){
							hlChannel.get(panelBox1.getSelectedItem().toString()+".fullgenes.bed",fullBedFile.toString(), monitor, mode);					
						}
						System.out.println("Downloading ensembl genes priority file");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Downloading ensembl genes priority file", true);
							}
						});
						hlChannel.cd(hlReferences+"/"+reference.getName()+"/panels");
						File priorityFile = new File(project+"/"+project+".genes");
						if (!priorityFile.exists()){
							hlChannel.get(panelBox1.getSelectedItem().toString()+".genes",priorityFile.toString(), monitor, mode);					
						}
						System.out.println("Downloading JSON parameters file");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Downloading JSON parameters file", true);
							}
						});
						hlChannel.cd(hlReferences+"/"+reference.getName());
						for (int row : selection){
							File jsonFile = new File(project+"/"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel+".json");
							String caller = table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(CALLER))).toString();
							String jsonfilename = "";
							if (caller.equals("PGM_GERMLINE_LOW_STRINGENCY")){
								jsonfilename = "pgm_germline_low_stringency.json";
							}else if (caller.equals("PGM_SOMATIC_LOW_STRINGENCY")){
								jsonfilename = "pgm_somatic_low_stringency.json";
							}else if (caller.equals("PROTON_GERMLINE_LOW_STRINGENCY")){
								jsonfilename = "proton_germline_low_stringency.json";
							}else if (caller.equals("PROTON_SOMATIC_LOW_STRINGENCY")){
								jsonfilename = "proton_somatic_low_stringency.json";
							}
							hlChannel.get(jsonfilename,jsonFile.toString(), monitor, mode);					
						}
						System.out.println("Uploading JSON files to Highlander Server");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Uploading JSON files to Highlander Server", true);
							}
						});
						hlChannel.cd(hlWorking);
						try {
							hlChannel.cd(project);
						}
						catch (SftpException e ) {
							hlChannel.mkdir(project);
							hlChannel.cd(project);
						}
						for (int row : selection){
							/*
							 * Now using rsync
							 * 
						File targetBam = new File(project+"/"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel+".bam");
						hlChannel.put(targetBam.toString(), ".", monitor, mode);
						File targetBai = new File(project+"/"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel+".bam.bai");
						hlChannel.put(targetBai.toString(), ".", monitor, mode);
							 */
							File targetJson = new File(project+"/"+table.getValueAt(row, table.convertColumnIndexToView(model.getColumn(SAMPLE))).toString().replace('_', '-').trim().replace(' ', '-')+"."+panel+".json");
							hlChannel.put(targetJson.toString(), ".", monitor, mode);
						}
						System.out.println("Uploading BED file to Highlander Server");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Uploading BED file to Highlander Server", true);
							}
						});
						hlChannel.put(project+"/"+project+".bed", ".", monitor, mode);
						System.out.println("Uploading full genes BED file to Highlander Server");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Uploading full genes BED file to Highlander Server", true);
							}
						});
						hlChannel.put(project+"/"+project+".fullgenes.bed", ".", monitor, mode);
						System.out.println("Uploading ensembl genes priority file to Highlander Server");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Uploading ensembl genes priority file to Highlander Server", true);
							}
						});
						hlChannel.put(project+"/"+project+".genes", ".", monitor, mode);
						/*
						 * Now that Highlander analyses are linked to a unique reference, it doesn't make sense to download the reference to the cluster
						 * Analysis should first be created "by hand"
						 * 
					if (!referenceBox.getSelectedItem().toString().equals("default")){
						//Check if reference is present on Highlander server
						boolean exist = false;
						for (Object dir : hlChannel.ls(hlReference)){
							LsEntry entry = (LsEntry)dir;
							if (entry.getFilename().equals(referenceBox.getSelectedItem().toString()+".fasta")){
								exist = true;
								break;
							}
						}	
						if (!exist){
							System.out.println("Downloading reference from Ion Server");
							File localRef = new File("references");
							localRef.mkdir();
							sequencerChannels.get(platform).cd(sequencerReferences.get(platform));
							sequencerChannels.get(platform).cd(referenceBox.getSelectedItem().toString());
							sequencerChannels.get(platform).lcd(localRef.getName());
							sequencerChannels.get(platform).get(referenceBox.getSelectedItem().toString()+".fasta",".", monitor, mode);
							sequencerChannels.get(platform).get(referenceBox.getSelectedItem().toString()+".fasta.fai",".", monitor, mode);
							sequencerChannels.get(platform).get(referenceBox.getSelectedItem().toString()+".dict",".", monitor, mode);
							System.out.println("Uploading reference to Highlander Server");
							hlChannel.lcd(localRef.getName());
							hlChannel.cd(hlReference);
							hlChannel.put(referenceBox.getSelectedItem().toString()+".fasta",".", monitor, mode);
							hlChannel.put(referenceBox.getSelectedItem().toString()+".fasta.fai",".", monitor, mode);
							hlChannel.put(referenceBox.getSelectedItem().toString()+".dict",".", monitor, mode);
						}
					}
						 */
						System.out.println("Launching importation pipeline");
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setProgressDone();
								waitingPanel.setProgressString("Launching importation pipeline", true);
							}
						});
						ChannelExec channelExec = (ChannelExec)hlSession.openChannel("exec");
						String command = hlScript+"/submit_iontorrent.sh ";
						//if (!referenceBox.getSelectedItem().toString().equals("default")) command += "-R "+referenceBox.getSelectedItem().toString()+".fasta ";
						if (panel.contains("EXOME")) command += "-E ";
						command += "-R " + reference.getName() + " ";
						command += hlWorking + "/" + project;					
						channelExec.setCommand(command);
						channelExec.connect();
						channelExec.setInputStream(null);
						channelExec.setOutputStream(System.out);
						channelExec.setErrStream(System.err);
						InputStream in=channelExec.getInputStream();
						channelExec.connect();
						byte[] tmp=new byte[1024];
						while(true){
							while(in.available()>0){
								int i=in.read(tmp, 0, 1024);
								if(i<0)break;
								System.out.print(new String(tmp, 0, i));
							}
							if(channelExec.isClosed()){
								if(in.available()>0) continue;
								System.out.println("exit-status: "+channelExec.getExitStatus());
								break;
							}
							try{Thread.sleep(1000);}catch(Exception ee){}
						}
						channelExec.disconnect();
						JOptionPane.showMessageDialog(IonImporter.this, "Your samples are being imported in Highlander, you'll receive an email when they are ready.", "Ion Importer",
								JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iDbAdd,64));		 
					}
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(IonImporter.this, Tools.getMessage("Importation", ex), "Ion Importer",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	public static class MyProgressMonitor implements SftpProgressMonitor {
		ProgressMonitor monitor;
		long count=0;
		long max=0;
		public void init(int op, String src, String dest, long max){
			this.max=max;
			monitor=new ProgressMonitor(null, 
					((op==SftpProgressMonitor.PUT)? 
							"put" : "get")+": "+src, 
					"",  0, 100);
			count=0;
			percent=-1;
			monitor.setProgress((int)this.count);
			monitor.setMillisToDecideToPopup(1000);
		}
		private long percent=-1;
		public boolean count(long count){
			this.count+=count;

			if(percent>=this.count*100/max){ return true; }
			percent=this.count*100/max;

			monitor.setNote("Completed "+this.count+" ("+percent+"%) out of "+max+".");     
			monitor.setProgress((int)this.percent);

			return !(monitor.isCanceled());
		}
		public void end(){
			monitor.close();
		}
	}

	private boolean connectToSequencer(Platform platform) {
		boolean connected = false;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.setProgressString("Connecting to " + platform, true);
				waitingPanel.start();
			}
		});
		try {
			sequencerJsch.put(platform, new JSch());
			sequencerJsch.get(platform).addIdentity(parameters.getConfigPath()+"/"+parameters.getServerSequencerPrivateKey().get(platform));
			sequencerSession.put(platform, sequencerJsch.get(platform).getSession(parameters.getServerSequencerUsername().get(platform), parameters.getServerSequencerHost().get(platform), 22));
			sequencerSession.get(platform).setConfig("StrictHostKeyChecking", "no");
			sequencerSession.get(platform).setTimeout(3000);
			sequencerSession.get(platform).connect();
			sequencerChannels.put(platform, (ChannelSftp) sequencerSession.get(platform).openChannel("sftp"));
			sequencerChannels.get(platform).connect();
			connected = true;
		}catch(JSchException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(IonImporter.this, "Platform " + platform + " is currently inaccessible, please set the server online.", "Connecting to " + platform, JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
		return connected;
	}

	private void disconnectFromSequencer(Platform platform) {
		try {
			if (sequencerChannels.get(platform) != null) sequencerChannels.get(platform).quit();
			if (sequencerSession.get(platform) != null) sequencerSession.get(platform).disconnect();
			if (sequencerJsch.get(platform) != null) sequencerJsch.get(platform).removeAllIdentity();
		}catch(JSchException ex) {
			ex.printStackTrace();
		}
	}

	private boolean connectToHighlander() throws JSchException {
		boolean connected = false;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.setProgressString("Connecting to Highlander server", true);
				waitingPanel.start();
			}
		});
		try {
			hlJsch = new JSch();
			hlJsch.addIdentity(parameters.getConfigPath()+"/"+parameters.getServerPipelinePrivateKey());			
			hlSession = hlJsch.getSession(parameters.getServerPipelineUsername(), parameters.getServerPipelineHost(), 22);
			hlSession.setConfig("StrictHostKeyChecking", "no");
			hlSession.setTimeout(3000);
			hlSession.connect();
			hlChannel = (ChannelSftp) hlSession.openChannel("sftp");
			hlChannel.connect();
			connected = true;
		}catch(JSchException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(IonImporter.this, "Highlander server is currently inaccessible.", "Connecting to Highlander server", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
		return connected;
	}

	private void disconnectFromHighlander() {
		try {
			if (hlChannel != null) hlChannel.quit();
			if (hlSession != null) hlSession.disconnect();
			if (hlJsch != null) hlJsch.removeAllIdentity();
		}catch(JSchException ex) {
			ex.printStackTrace();
		}
	}

	public boolean upload(File file, String destination, Platform platform) throws Exception {		
		if (!file.exists()) throw new Exception("The file '"+file+"' does not exist.");
		if (connectToSequencer(platform)) {
			sequencerChannels.get(platform).put(file.getPath(),destination);
			return true;
		}
		return false;
	}

	public void download(File file, String source, Platform platform) throws Exception {
		FileDialog fd = new FileDialog(IonImporter.this, "Download file from repository", FileDialog.SAVE);
		fd.setFile(file.getName());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
		Dimension windowSize = fd.getSize() ;
		fd.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
				Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
		fd.setVisible(true);
		if (fd.getFile() != null) {
			String filename = fd.getDirectory() + fd.getFile();
			if (connectToSequencer(platform)) {
				sequencerChannels.get(platform).get(source,filename);
			}
		}
	}

	public class RSync implements Runnable {
		private Platform platform;
		private String sequencerPath;
		private String sequencerLibrary;
		private String highlanderProject;
		private String highlanderSample;
		
		public RSync(Platform platform, String sequencerPath, String sequencerLibrary, String highlanderProject, String highlanderSample) {
			this.platform = platform;
			this.sequencerPath = sequencerPath;
			this.sequencerLibrary = sequencerLibrary;
			this.highlanderProject = highlanderProject;
			this.highlanderSample = highlanderSample;
		}
		
		public void run(){
			try{
				HttpClient httpClient = new HttpClient();
				boolean bypass = false;
				if (System.getProperty("http.nonProxyHosts") != null) {
					for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
						if ((Highlander.getParameters().getUrlForPhpScripts()+"/retreive_panel_data.php").toLowerCase().contains(host.toLowerCase())) bypass = true;
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
				PostMethod post = new PostMethod(Highlander.getParameters().getUrlForPhpScripts()+"/retreive_panel_data.php");
				NameValuePair[] data = {
						new NameValuePair("sequencer", platform.toString()),
						new NameValuePair("run", sequencerPath),
						new NameValuePair("library", sequencerLibrary),
						new NameValuePair("project", highlanderProject),
						new NameValuePair("sample", highlanderSample),
				};
				post.addParameters(data);
				int httpRes = httpClient.executeMethod(post); 
				if (httpRes == 200) {			
					try (InputStreamReader isr = new InputStreamReader(post.getResponseBodyAsStream())){
						try (BufferedReader br = new BufferedReader(isr)){
							String line = null;
							while(((line = br.readLine()) != null)) {
								System.out.println(line);
							}
						}
					}
				}else {
					//TODO error 503 plusieurs fois avec Audrey ... mettre ça dans un while et recommencer max 10x ?
					throw new Exception("PHP script return error code " + httpRes + ".\n");
				}
			}catch(IOException iex){
				iex.printStackTrace();
				//Get sometimes 'chunked stream ended unexpectedly' with 'CRLF expected at end of chunk: -1/-1'
				//Probably linked to the php inputstream, bash script with rsync probably not affected
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(IonImporter.this, Tools.getMessage(highlanderSample + " could had a transfer problem, please contact Raphael with the following info:", ex), "Transfer of " + highlanderSample, JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setProgressValue(++transferCount);
				}
			});
		}
	}
	
	public String[] listProjects(Platform platform) throws SftpException {
		Set<String> projects = new TreeSet<String>();
		for (Object dir : sequencerChannels.get(platform).ls(sequencerResults.get(platform))){
			LsEntry entry = (LsEntry)dir;
			if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..") && !entry.getFilename().contains("_tn_"))
				projects.add(entry.getFilename());
		}
		return projects.toArray(new String[0]);
	}

	public AnalysisFull[] listAnalyses(Platform platform) throws Exception {
		Set<AnalysisFull> analyses = new TreeSet<AnalysisFull>();
		for (AnalysisFull analysis : AnalysisFull.getAvailableAnalyses()) {
			if (analysis.getSequencingTarget().equalsIgnoreCase("Panel")) {
				if (platform == Platform.ION_TORRENT || platform == Platform.PROTON) {
					if (analysis.getVariantCaller() == VariantCaller.TORRENT) {
						analyses.add(analysis);
					}
				}else if (platform == Platform.MINISEQ || platform == Platform.MISEQ) {
					if (analysis.getVariantCaller() == VariantCaller.GATK) {
						analyses.add(analysis);
					}
				}				
			}
		}
		return analyses.toArray(new AnalysisFull[0]);
	}
	
	public String[] listReferences(Platform platform) throws SftpException {
		Set<String> references = new TreeSet<String>();
		references.add("default");
		for (Object dir : sequencerChannels.get(platform).ls(sequencerReferences.get(platform))){
			LsEntry entry = (LsEntry)dir;
			if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
				references.add(entry.getFilename());
		}		
		return references.toArray(new String[0]);
	}

	public String[] listBam(Platform platform) throws SftpException {
		Set<String> bams = new TreeSet<String>();
		if (connectToSequencer(platform)) {
			for (Object dir : sequencerChannels.get(platform).ls(sequencerResults.get(platform)+"/"+projectBox.getSelectedItem().toString())){
				LsEntry entry = (LsEntry)dir;
				if (entry.getFilename().startsWith("IonXpress_") && entry.getFilename().contains("rawlib") && entry.getFilename().endsWith(".bam"))
					bams.add(entry.getFilename());
			}
			disconnectFromSequencer(platform);
		}
		return bams.toArray(new String[0]);
	}

	public String[] listUsers() throws Exception {
		Set<String> users = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT username FROM users")) {
			while(res.next()){
				users.add(res.getString(1));
			}
		}
		return users.toArray(new String[0]);
	}

	public String[] listPanels(Reference reference) {
		Set<String> codes = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `code` FROM ion_panels WHERE `reference` = '"+reference.getName()+"'")) {
			while(res.next()){
				codes.add(res.getString(1));
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return codes.toArray(new String[0]);
	}
	
	public Map<String, Integer> listPathologies() throws Exception {
		Map<String, Integer> pathologies = new TreeMap<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM pathologies")) {
			while(res.next()){
				pathologies.put(res.getString("pathology"), res.getInt("pathology_id"));
			}
		}
		return pathologies;
	}

	public Map<String, Integer> listPopulations() throws Exception {
		Map<String, Integer> populations = new TreeMap<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM populations")) {
			while(res.next()){
				populations.put(res.getString("population"), res.getInt("population_id"));
			}
		}
		return populations;
	}
	
	public void createNewPanel(){
		try{
			Reference reference;
			String code;
			String description;
			String designer;
			Set<String> genes = new LinkedHashSet<String>();
			Set<String> positions = new LinkedHashSet<String>();
			Object res = JOptionPane.showInputDialog(IonImporter.this,  "Select the reference genome of your panel", "Reference genome",
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), Reference.getAvailableReferences().toArray(new Reference[0]), Reference.getReference("hg19_lifescope"));
			if (res != null){
				reference = (Reference)res;
				res = JOptionPane.showInputDialog(IonImporter.this,  "Enter the unique code of your "+reference+" panel (max 19 characters).\n" +
						"Please only use capital LETTERS (no space, point, coma, hyphen, underscore, etc).\n" +
						"Don't enter the starting \"p\", it will be automatically added.\n" +
						"e.g. KPT, LELM, LEGAPS, PTHOT, CMAVM, ...", "Panel code",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
				if (res != null){
					code = res.toString().toUpperCase().trim();
					Pattern p = Pattern.compile("[^A-Z0-9]");
					Matcher m = p.matcher(code);
					if(m.find()){
						JOptionPane.showMessageDialog(IonImporter.this, "Please only use letters and numbers", "Panel code", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}else if(code.length() == 0){
						JOptionPane.showMessageDialog(IonImporter.this, "Panel code is mandatory", "Panel code", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}else if(code.length() > 19){
						JOptionPane.showMessageDialog(IonImporter.this, "Panel code is limited to 19 characters", "Panel code", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}else{
						code = "p"+code;
						boolean unique = true;
						for (String panel : listPanels(reference)){
							if (panel.equals(code)) unique = false;
						}
						if (!unique){
							JOptionPane.showMessageDialog(IonImporter.this, "Panel code already exists for "+reference+" (it must be unique)", "Panel code", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}else{
							res = JOptionPane.showInputDialog(IonImporter.this,  "Enter a short description of your panel (max 200 characters)", "Panel description",
									JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
							if (res != null){
								description = res.toString().trim();
								if(description.length() > 200){
									JOptionPane.showMessageDialog(IonImporter.this, "Panel description is limited to 200 characters", "Panel description", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
								}else if(description.length() == 0){
									JOptionPane.showMessageDialog(IonImporter.this, "Panel description is mandatory", "Panel description", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
								}else{
									res = JOptionPane.showInputDialog(IonImporter.this,  "Enter the name of the panel desginer", "Panel designer",
											JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
									if (res != null){
										designer = res.toString().trim();
										if(designer.length() > 45){
											JOptionPane.showMessageDialog(IonImporter.this, "Panel designer is limited to 45 characters", "Panel designer", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
										}else if(description.length() == 0){
											JOptionPane.showMessageDialog(IonImporter.this, "Panel designer is mandatory", "Panel designer", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
										}else{
											JOptionPane.showMessageDialog(IonImporter.this, "Please select the bed file defining the SEQUENCED TARGET of your panel. \nThis is the bed file named 'designed' in Ampliseq.", "Targeted BED file", JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iInterval,64));
											FileDialog chooser = new FileDialog(IonImporter.this, "Select the targeted bed file", FileDialog.LOAD);
											chooser.setVisible(true);
											if (chooser.getFile() != null) {
												boolean addchr = false;
												File bed = new File(chooser.getDirectory() + chooser.getFile());
												try (FileReader fr = new FileReader(bed)){
													try (BufferedReader br = new BufferedReader(fr)){
														String line;
														while ((line = br.readLine()) != null){
															if (!line.startsWith("track")){
																String[] array = line.split("\t");
																String chr = array[0];
																if (chr.startsWith("chr")) {
																	chr = chr.substring(3);
																	addchr = true;
																}
																positions.add(chr+":"+array[1]+"-"+array[2]); 
																if (array.length > 3){
																	genes.add(array[array.length-1]);
																}
															}
														}
													}
												}
												if (positions.isEmpty()){
													JOptionPane.showMessageDialog(IonImporter.this, "No position found in the bed file, perhaps it's not well formated (tab separated, each line starting by chr+start+stop and possibly other information following).", "Panel designer", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));										
												}else{
													/* Problematic, because in the submitted AmpliSeq file, exons can be divided in multiple interval, which cause problems with convading CNV calling
													JOptionPane.showMessageDialog(IonImporter.this, "Please select the bed file defining the FULL GENES of your panel. \nThis is the bed file named 'submitted' in Ampliseq IF you wished to cover the full genes.\nIf you only covered a few exons, please create yourself a full genes bed file !", "Full gene BED file", JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iInterval,64));
													chooser = new FileDialog(IonImporter.this, "Select the full genes bed file", FileDialog.LOAD);
													chooser.setVisible(true);
													if (chooser.getFile() != null) {
														File fullbed = new File(chooser.getDirectory() + chooser.getFile());
														try (FileReader fr = new FileReader(fullbed)){
														try (BufferedReader br = new BufferedReader(fr)){
														while ((line = br.readLine()) != null){
															if (!line.startsWith("track")){
																String[] array = line.split("\t");
																String chr = array[0];
																if (chr.startsWith("chr")) chr = chr.substring(3);
																if (array.length > 3){
																	genes.add(array[array.length-1]);
																}
															}
														}
														}
														}
													 */
													AskGeneList ask = new AskGeneList(genes);
													ask.sort();
													Tools.centerWindow(ask, false);
													ask.setVisible(true);
													genes = new TreeSet<String>(ask.getSelection());
													//Validation panel
													JPanel panel = new JPanel();
													panel.setLayout(new BorderLayout());
													JTextArea label = new JTextArea("Please check that all information is correct:");
													Color bg = panel.getBackground();
													label.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue()));
													label.setBorder(null);
													label.setEditable(false);
													panel.add(label, BorderLayout.NORTH);
													final StringBuilder sb = new StringBuilder();
													sb.append("Reference genome: " + reference);
													sb.append("\n");
													sb.append("Code: " + code);
													sb.append("\n");
													sb.append("Short description: " + description);
													sb.append("\n");
													sb.append("Designer: " + designer);
													sb.append("\n");
													sb.append("\n");
													sb.append("Gene list:");
													sb.append("\n");
													for (String gene : genes){
														sb.append(gene + "\n");
													}
													sb.append("\n");
													sb.append("Positions:");
													sb.append("\n");
													for (String pos : positions){
														sb.append(pos + "\n");
													}
													JTextArea textArea = new JTextArea(sb.toString());
													textArea.setCaretPosition(0);
													textArea.setEditable(true);
													JScrollPane scrollPane = new JScrollPane(textArea);
													panel.add(scrollPane, BorderLayout.CENTER);										
													panel.setPreferredSize(new Dimension(500,300));
													int yesno = JOptionPane.showConfirmDialog(IonImporter.this, panel, "Panel creation", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iButtonApply,64));									
													if (yesno == JOptionPane.YES_OPTION){
														StringBuilder genelist = new StringBuilder();
														for (String gene : genes) genelist.append(gene+";");
														if (genelist.length() > 0) genelist.deleteCharAt(genelist.length()-1);
														StringBuilder poslist = new StringBuilder();
														for (String pos : positions) poslist.append(pos+";");
														if (poslist.length() > 0) poslist.deleteCharAt(poslist.length()-1);
														DB.insert(Schema.HIGHLANDER, "INSERT INTO `ion_panels` SET" + 
																" `reference` = '"+DB.format(Schema.HIGHLANDER, reference.getName())+"'," +
																" `code` = '"+DB.format(Schema.HIGHLANDER, code)+"'," +
																" `description` = '"+DB.format(Schema.HIGHLANDER, description)+"'," +
																" `designer` = '"+DB.format(Schema.HIGHLANDER, designer)+"'," +
																" `genes` = '"+DB.format(Schema.HIGHLANDER, genelist.toString())+"'," +
																" `positions` = '"+DB.format(Schema.HIGHLANDER, poslist.toString())+"'");
														System.out.println("Uploading BED file to Highlander Server");
														//Torrent Variant Caller doesn't like first commented line, so remove it
														//TODO sort the bed file by chr:pos, it's not always the case
														File tmp = File.createTempFile(code+".bed", "");
														try (BufferedReader br = new BufferedReader(new FileReader(bed))){
															try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmp))){
																String line;
																while ((line = br.readLine()) != null){
																	if (!line.startsWith("track")){
																		bw.write(String.format("%s%n", line));
																	}
																}
															}
														}
														File tmpfull = File.createTempFile(code+".fullgenes.bed", "");
														//Create the full genes bed using Ensembl													
														ExomeBed.generateBed(reference, genes, new HashMap<String,Boolean>(), true, false, addchr, false, false, tmpfull, null);
														/*
														try (BufferedReader br = new BufferedReader(new FileReader(fullbed))){
												    try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpfull))){
												    while ((line = br.readLine()) != null){
												    	if (!line.startsWith("track")){
												    		bw.write(String.format("%s%n", line));
												    	}
												    }
												    }
												    }
														 */
														File tmpgenes = File.createTempFile(code+".genes", "");
														try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpgenes))){
															for (String geneSymbol : genes) {
																String ensg = DBUtils.getEnsemblGene(reference, geneSymbol);
																if (ensg != null) {
																	bw.write(ensg + "\n");
																}
															}
														}
														if (connectToHighlander()) {
															SftpProgressMonitor monitor = new MyProgressMonitor();
															int mode=ChannelSftp.OVERWRITE;
															hlChannel.cd(hlReferences+"/"+reference.getName()+"/panels");
															hlChannel.put(tmp.toString(), code+".bed", monitor, mode);
															hlChannel.put(tmpfull.toString(), code+".fullgenes.bed", monitor, mode);
															hlChannel.put(tmpgenes.toString(), code+".genes", monitor, mode);
															//Launch convader script
															System.out.println("Launching Convader create panel script");
															ChannelExec channelExec = (ChannelExec)hlSession.openChannel("exec");
															String command = hlScript+"/software/convader/scripts/create_panel.sh " + reference.getName() + " " + code;
															channelExec.setCommand(command);
															channelExec.connect();
															channelExec.setInputStream(null);
															channelExec.setOutputStream(System.out);
															channelExec.setErrStream(System.err);
															InputStream in=channelExec.getInputStream();
															channelExec.connect();
															byte[] tmpBytes=new byte[1024];
															while(true){
																while(in.available()>0){
																	int i=in.read(tmpBytes, 0, 1024);
																	if(i<0)break;
																	System.out.print(new String(tmpBytes, 0, i));
																}
																if(channelExec.isClosed()){
																	if(in.available()>0) continue;
																	System.out.println("exit-status: "+channelExec.getExitStatus());
																	break;
																}
																try{Thread.sleep(1000);}catch(Exception ee){}
															}
															channelExec.disconnect();
															JOptionPane.showMessageDialog(IonImporter.this, "Panel "+code+" successfuly created", "Panel creation", JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iButtonApply,64));
															displayReferences();
															panelReferenceList.setSelectedValue(reference, true);
															panelPanelList.setSelectedValue(code, true);
															if (analysisBox.getSelectedItem() != null && ((AnalysisFull)analysisBox.getSelectedItem()).getReference().equals(reference)) panelBox1.addItem(code);
														}else{
															FileUtils.copyFile(tmp, new File(Tools.getHomeDirectory().toString()+"/Downloads/"+code+".bed"));
															FileUtils.copyFile(tmpfull, new File(Tools.getHomeDirectory().toString()+"/Downloads/"+code+".fullgenes.bed"));
															FileUtils.copyFile(tmpgenes, new File(Tools.getHomeDirectory().toString()+"/Downloads/"+code+".genes"));
															System.err.println("No connexion to Highlander server");
															System.err.println("Files in " + Tools.getHomeDirectory().toString()+"/Downloads/" + " must be copied to '"+hlReferences+"/"+reference.getName()+"/panels'");
															System.err.println("You must launching Convader create panel script: ");
															System.err.println(hlScript+"/software/convader/scripts/create_panel.sh " + reference.getName() + " " + code);
														}
													}
													//}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}			
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(IonImporter.this, Tools.getMessage("Error", ex), "Panel creation", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	private void displayReferences() {
		panelReferenceListModel.clear();
		try {
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(`reference`) FROM ion_panels")) {
				while(res.next()){
					panelReferenceListModel.addElement(Reference.getReference(res.getString(1)));
				}
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(IonImporter.this, ex.getMessage(), "Can't display references", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		panelReferenceList.setBorder(new TitledBorder(null, "References ("+panelReferenceListModel.getSize()+")", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	}
	
	private void displayPanels() {
		Reference ref = panelReferenceList.getSelectedValue();
		panelPanelListModel.clear();
		try {
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `code` FROM ion_panels WHERE `reference` = '"+DB.format(Schema.HIGHLANDER, ref.getName())+"'")) {
				while(res.next()){
					panelPanelListModel.addElement(res.getString(1));
				}
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(IonImporter.this, ex.getMessage(), "Can't display panels for reference " + ref, JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		panelPanelList.setBorder(new TitledBorder(null, "Panels ("+panelPanelListModel.getSize()+")", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	}
	
	private void displayPanelDetails(){
		try {
			Reference ref = panelReferenceList.getSelectedValue();
			String code = panelPanelList.getSelectedValue();
			if (ref != null && code != null) {
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM ion_panels WHERE `code` = '"+DB.format(Schema.HIGHLANDER, code)+"' AND `reference` = '"+DB.format(Schema.HIGHLANDER, ref.getName())+"'")) {
					if(res.next()){
						panelDesc.setText("Description: " + res.getString("description") + "\nDesigner: " + res.getString("designer"));
						panelDesc.setCaretPosition(0);
						panelDesc.validate();
						panelGeneListModel.clear();
						for (String gene : res.getString("genes").split(";")){
							panelGeneListModel.addElement(gene);
						}
						panelGeneList.setBorder(new TitledBorder(null, "Genes ("+panelGeneListModel.getSize()+")", TitledBorder.LEADING, TitledBorder.TOP, null, null));
						panelPositionListModel.clear();
						for (String pos : res.getString("positions").split(";")){
							panelPositionListModel.addElement(pos);
						}
						panelPositionList.setBorder(new TitledBorder(null, "Positions ("+panelPositionListModel.getSize()+")", TitledBorder.LEADING, TitledBorder.TOP, null, null));
					}else{
						JOptionPane.showMessageDialog(IonImporter.this, "Panel "+code+" not found in the database for reference "+ref+" !", "Display selected panel", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}else {
				panelGeneList.setBorder(new TitledBorder(null, "Genes", TitledBorder.LEADING, TitledBorder.TOP, null, null));
				panelPositionList.setBorder(new TitledBorder(null, "Positions", TitledBorder.LEADING, TitledBorder.TOP, null, null));				
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(IonImporter.this, ex.getMessage(), "Can't display selected panel", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			exit();
		}
	}

	public void exit(){
		try{
			for (Platform platform : availablePlatforms){
				disconnectFromSequencer(platform);
			}
			disconnectFromHighlander();
		}catch(Exception ex){
			Tools.exception(ex);
		}
		System.exit(0);
	}

	public static User login(){
		LoginBox loginBox = new LoginBox(new JFrame(), "Ion Importer "+version+" login at "+parameters.getDbMainHost(), true) ;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
		Dimension windowSize = loginBox.getSize() ;
		loginBox.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
				Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
		loginBox.setVisible(true) ;
		if (loginBox.OKCancel) {
			try {
				return new User(loginBox.getUsername(), loginBox.getEncryptedPassword()) ;
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Can't login", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return null;
			}
		} else {
			System.exit(0);
			return null;
		}
	}

	/**
	 * 
	 * Program arguments
	 * -u [username] : give username as parameter 
	 * -p [password] : give password as parameter
	 * -c [config file] : give config file to use as parameter
	 * -upoff				 : turn auto update off
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String argUser = null;
		String argPass = null;
		String argConfig = null;
		boolean updateCheck = true;
		for(int i=0 ; i < args.length ; i++){
			if (args[i].equalsIgnoreCase("-u")){
				argUser = args[++i];
			}else if (args[i].equalsIgnoreCase("-p")){
				argPass = args[++i];
			}else if (args[i].equalsIgnoreCase("-c")){
				argConfig = args[++i];
			}else if (args[i].equalsIgnoreCase("-upoff")){
				updateCheck = false;
			}
		}
		try {
			if (updateCheck) ApplicationLauncher.launchApplication("170", null, true, null);
		} catch (Exception ex) {
			Tools.exception(ex);
		}
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					InputMap im = (InputMap)UIManager.get("Button.focusInputMap");
					im.put( KeyStroke.getKeyStroke( "ENTER" ), "pressed" );
					im.put( KeyStroke.getKeyStroke( "released ENTER" ), "released" );
					break;
				}
			}
		} catch (Exception e) {
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}catch (Exception ex) {
				Tools.exception(ex);
			}
		}
		parameters = (argConfig == null) ? new Parameters(true) : new Parameters(true, new File(argConfig));
		try{
			Highlander.initialize(parameters, 5);
			DB = Highlander.getDB();
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem when connecting the database", ex), "Connecting to Highlander database",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		while(user == null){
			if (argUser == null || argPass == null){
				user = login();
			}else{
				try {
					user = new User(argUser, Tools.md5Encryption(argPass));
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Can't login", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					user = login();
				}
			}
		}
		Highlander.setLoggedUser(user);
		final IonImporter ion = new IonImporter();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ion.validate();
				//Center the window
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				ion.setSize(new Dimension(screenSize.width/2, screenSize.height/3*2));
				Dimension frameSize = ion.getSize();
				if (frameSize.height > screenSize.height) {
					frameSize.height = screenSize.height;
				}
				if (frameSize.width > screenSize.width) {
					frameSize.width = screenSize.width;
				}
				ion.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
				//pm.setExtendedState(ProjectManager.MAXIMIZED_BOTH);
				ion.setVisible(true);
			}
		});
	}

}
