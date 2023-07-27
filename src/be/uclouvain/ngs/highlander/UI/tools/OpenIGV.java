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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
//import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Dimension;
//import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.ws.http.HTTPException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
//import org.broad.igv.ui.IGVAccess;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.AskSamplesDialog;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

public class OpenIGV extends JDialog {

	Set<String> variants = new HashSet<String>();
	Map<String, AnalysisFull> samples = new HashMap<String, AnalysisFull>();
	Map<String,String> dbsnpFiles = new TreeMap<String,String>(); 
	String lastDbsnpVersion = "";

	private Map<JCheckBox, AnalysisFull> analysesBam = new HashMap<JCheckBox, AnalysisFull>();
	private Map<JCheckBox, AnalysisFull> analysesVcf = new HashMap<JCheckBox, AnalysisFull>();

	private JPanel dbsnpPanel;
	private JPanel variantsPanel;
	private JPanel bamPanel;
	private JPanel vcfPanel;
	private JSlider memorySlider;
	private JLabel lblAmount;
	private int ypat = 1;

	//For new process

	private final JFrame igvFrame = new JFrame();
	private final JTextArea console = new JTextArea();

	//For new frame
	/*
	private final JFrame igvFrame = new JFrame(){
	  //Overridden so we can exit when window is closed
	  protected void processWindowEvent(WindowEvent e) {
	    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	    	igvFrame.setVisible(false);
	    }
	  }
	};

	private static boolean alreadyOpen = false;
	 */

	public OpenIGV(Highlander mainFrame){		
		List<Integer> selection = mainFrame.getVariantTable().getSelectedVariantsId();
		try {
			if (!selection.isEmpty()){
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT sample, chr, pos "
						+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
						+ Highlander.getCurrentAnalysis().getJoinProjects()
						+ "WHERE variant_sample_id IN (" + HighlanderDatabase.makeSqlFieldList(selection) + ")"
						)) {
					while (res.next()){
						variants.add(res.getString("chr") + ":" + res.getInt("pos"));
						samples.put(res.getString("sample"), Highlander.getCurrentAnalysis());
					}
				}
			}
			fetchAvailableDbsnpVersion();
		} catch (Exception ex) {
			Tools.exception(ex);
		}
		initUI();
		pack();
	}

	public OpenIGV(List<String> variants, Map<String, AnalysisFull> samples){
		this.variants.addAll(variants);
		this.samples.putAll(samples);
		initUI();
		pack();
	}

	private void fetchAvailableDbsnpVersion() {
		try {
			String[] versions = get(Highlander.getParameters().getUrlForDbsnpVcfs()+"/versions").split("\n");
			for (String line : versions){
				String version = line.split("\t")[0];
				String file = line.split("\t")[1];
				if (version.compareTo(lastDbsnpVersion) > 0) lastDbsnpVersion = version;
				dbsnpFiles.put(version, file);
			}
		}catch(HTTPException ex) {
			System.err.println("Fetch DBSNP versions file from server sent HTTP error " + ex.getStatusCode() + ".\nCreate a 2 columns tab separated file named 'versions' with dbsnp version number and filename within URL (e.g. 138 dbsnp_138.vcf).\nNow Trying to browse directory struture for dbsnp vcf.");
			try {
				String[] lines = get(Highlander.getParameters().getUrlForDbsnpVcfs()).split("\n");
				for (String line : lines){
					if (line.contains("dbsnp") && line.contains(".vcf")){
						int start = line.indexOf("dbsnp");
						int end = line.indexOf(".vcf")+4;
						String file = line.substring(start, end);
						String version = "dbSNP ";
						if (file.split("\\.")[0].contains("_")){
							version += file.split("\\.")[0].split("_")[1];
						}else if (file.split("\\.").length > 2){
							version += file.split("\\.")[1];
						}else{
							version = file.split("\\.")[0];
						}
						if (version.compareTo(lastDbsnpVersion) > 0) lastDbsnpVersion = version;
						dbsnpFiles.put(version, file);
					}
				}			
			}catch(HTTPException e) {
				System.err.println("Browse DBSNP directory from server sent HTTP error " + ex.getStatusCode() + ".\nThe URL '"+Highlander.getParameters().getUrlForDbsnpVcfs()+"' is not accessible or directory browsing is disabled on server (and no file named 'versions' has been found)");
			}
		}
	}

	private String get(String url) throws HTTPException {
		String response = "";
		int res = 0;
		int attempts = 0;
		while (res != 200) {
			try {
				GetMethod getMethod = new GetMethod(url);
				HttpClient httpClient = new HttpClient();
				boolean bypass = false;
				if (System.getProperty("http.nonProxyHosts") != null) {
					for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
						if (url.toLowerCase().contains(host.toLowerCase())) bypass = true;
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
						System.out.println("USING PROXY: "+httpClient.getHostConfiguration().getProxyHost());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				res = httpClient.executeMethod(getMethod);
				response = getMethod.getResponseBodyAsString();
				getMethod.releaseConnection();
				if (res >= 400 || attempts == 20) {
					throw new HTTPException(res);
				}
			}
			catch (IOException ex) {
				Tools.exception(ex);
			}
			attempts++;
		}
		return response;
	}

	private void initUI(){
		setModal(true);
		setTitle("View variant in IGV");
		setIconImage(Resources.getScaledIcon(Resources.iIGV, 64).getImage());

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnApply = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnApply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				new Thread(new Runnable(){
					@Override
					public void run(){
						viewInIGV();
					}
				}, "OpenIGV.viewInIGV").start();
			}
		});
		panel.add(btnApply);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel.add(btnCancel);

		JPanel panel_1 = new JPanel();
		JScrollPane mainScroll = new JScrollPane(panel_1);
		mainScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		mainScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(mainScroll, BorderLayout.CENTER);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0, 0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);

		dbsnpPanel = new JPanel();
		dbsnpPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc_dbsnpPanel = new GridBagConstraints();
		gbc_dbsnpPanel.weightx = 1.0;
		gbc_dbsnpPanel.insets = new Insets(0, 0, 5, 0);
		gbc_dbsnpPanel.fill = GridBagConstraints.BOTH;
		gbc_dbsnpPanel.gridx = 0;
		gbc_dbsnpPanel.gridy = 0;
		panel_1.add(dbsnpPanel, gbc_dbsnpPanel);


		JLabel lbldbsnp = new JLabel("Which version(s) of dbsnp do you want to include ?");
		GridBagConstraints gbc_lbldbsnp = new GridBagConstraints();
		gbc_lbldbsnp.weightx = 1.0;
		gbc_lbldbsnp.insets = new Insets(10, 10, 5, 20);
		gbc_lbldbsnp.anchor = GridBagConstraints.NORTH;
		gbc_lbldbsnp.fill = GridBagConstraints.HORIZONTAL;
		gbc_lbldbsnp.gridx = 0;
		gbc_lbldbsnp.gridy = 0;
		dbsnpPanel.add(lbldbsnp, gbc_lbldbsnp);

		int y=1;
		for (String dbsnpVersion : dbsnpFiles.keySet()){
			JCheckBox rdbtndbsnp = new JCheckBox(dbsnpVersion);
			rdbtndbsnp.setSelected(dbsnpVersion.equals(lastDbsnpVersion));
			GridBagConstraints gbc_rdbtndbsnp = new GridBagConstraints();
			gbc_rdbtndbsnp.fill = GridBagConstraints.HORIZONTAL;
			gbc_rdbtndbsnp.insets = new Insets(5, 20, 5, 10);
			gbc_rdbtndbsnp.gridx = 0;
			gbc_rdbtndbsnp.gridy = y++;
			dbsnpPanel.add(rdbtndbsnp, gbc_rdbtndbsnp);			
		}

		variantsPanel = new JPanel();
		variantsPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc_variantsPanel = new GridBagConstraints();
		gbc_variantsPanel.weightx = 1.0;
		gbc_variantsPanel.insets = new Insets(0, 0, 5, 0);
		gbc_variantsPanel.fill = GridBagConstraints.BOTH;
		gbc_variantsPanel.gridx = 0;
		gbc_variantsPanel.gridy = 1;
		panel_1.add(variantsPanel, gbc_variantsPanel);


		JLabel lblOnWhichVariant = new JLabel("On which variant to you want to zoom in ?");
		GridBagConstraints gbc_lblOnWhichVariant = new GridBagConstraints();
		gbc_lblOnWhichVariant.weightx = 1.0;
		gbc_lblOnWhichVariant.insets = new Insets(10, 10, 5, 20);
		gbc_lblOnWhichVariant.anchor = GridBagConstraints.NORTH;
		gbc_lblOnWhichVariant.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblOnWhichVariant.gridx = 0;
		gbc_lblOnWhichVariant.gridy = 0;
		variantsPanel.add(lblOnWhichVariant, gbc_lblOnWhichVariant);

		ButtonGroup variantGroup = new ButtonGroup();

		int yvar = 1;
		for (String variant : variants){
			JRadioButton rdbtnVariantSelection = new JRadioButton(variant);
			if (yvar == 1) rdbtnVariantSelection.setSelected(true);
			GridBagConstraints gbc_rdbtnVariantSelection = new GridBagConstraints();
			gbc_rdbtnVariantSelection.fill = GridBagConstraints.HORIZONTAL;
			gbc_rdbtnVariantSelection.insets = new Insets(5, 20, 5, 10);
			gbc_rdbtnVariantSelection.gridx = 0;
			gbc_rdbtnVariantSelection.gridy = yvar++;
			variantsPanel.add(rdbtnVariantSelection, gbc_rdbtnVariantSelection);
			variantGroup.add(rdbtnVariantSelection);
		}

		JPanel samplesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc_samplesPanel = new GridBagConstraints();
		gbc_samplesPanel.weightx = 1.0;
		gbc_samplesPanel.insets = new Insets(0, 0, 5, 0);
		gbc_samplesPanel.fill = GridBagConstraints.BOTH;
		gbc_samplesPanel.gridx = 0;
		gbc_samplesPanel.gridy = 2;
		panel_1.add(samplesPanel, gbc_samplesPanel);

		JLabel lblSample = new JLabel("Which sample(s) do you want to include ?");
		GridBagConstraints gbc_lblSample = new GridBagConstraints();
		gbc_lblSample.weightx = 1.0;
		gbc_lblSample.insets = new Insets(10, 10, 5, 20);
		gbc_lblSample.anchor = GridBagConstraints.NORTH;
		gbc_lblSample.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblSample.gridwidth = 2;
		gbc_lblSample.gridx = 0;
		gbc_lblSample.gridy = 0;
		samplesPanel.add(lblSample, gbc_lblSample);

		bamPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc_bamPanel = new GridBagConstraints();
		gbc_bamPanel.weightx = 1.0;
		gbc_bamPanel.insets = new Insets(0, 0, 0, 0);
		gbc_bamPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_bamPanel.gridx = 0;
		gbc_bamPanel.gridy = 1;
		samplesPanel.add(bamPanel, gbc_bamPanel);

		JCheckBox checkBoxAllBams = new JCheckBox("Alignment (BAM)");
		checkBoxAllBams.setSelected(true);
		checkBoxAllBams.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						for (Component c : bamPanel.getComponents()){
							if (c instanceof JCheckBox){
								((JCheckBox)c).setSelected(e.getStateChange() == ItemEvent.SELECTED);
							}
						}
					}
				});
			}
		});
		GridBagConstraints gbc_lblBam = new GridBagConstraints();
		gbc_lblBam.weightx = 1.0;
		gbc_lblBam.insets = new Insets(5, 10, 5, 20);
		gbc_lblBam.anchor = GridBagConstraints.NORTH;
		gbc_lblBam.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblBam.gridx = 0;
		gbc_lblBam.gridy = 0;
		bamPanel.add(checkBoxAllBams, gbc_lblBam);

		vcfPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc_vcfPanel = new GridBagConstraints();
		gbc_vcfPanel.weightx = 1.0;
		gbc_vcfPanel.insets = new Insets(0, 0, 0, 0);
		gbc_vcfPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_vcfPanel.gridx = 1;
		gbc_vcfPanel.gridy = 1;
		samplesPanel.add(vcfPanel, gbc_vcfPanel);

		JCheckBox checkBoxAllVcfs = new JCheckBox("Variants (VCF)");
		checkBoxAllVcfs.setSelected(true);
		checkBoxAllVcfs.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						for (Component c : vcfPanel.getComponents()){
							if (c instanceof JCheckBox){
								((JCheckBox)c).setSelected(e.getStateChange() == ItemEvent.SELECTED);
							}
						}
					}
				});
			}
		});
		GridBagConstraints gbc_lblvcf = new GridBagConstraints();
		gbc_lblvcf.weightx = 1.0;
		gbc_lblvcf.insets = new Insets(5, 10, 5, 20);
		gbc_lblvcf.anchor = GridBagConstraints.NORTH;
		gbc_lblvcf.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblvcf.gridx = 0;
		gbc_lblvcf.gridy = 0;
		vcfPanel.add(checkBoxAllVcfs, gbc_lblvcf);

		JButton btnAddSample = new JButton("Add sample",Resources.getScaledIcon(Resources.i3dPlus, 24));
		btnAddSample.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				searchSample();
			}
		});
		GridBagConstraints gbc_btnAddSample = new GridBagConstraints();
		gbc_btnAddSample.insets = new Insets(5, 20, 5, 0);
		gbc_btnAddSample.anchor = GridBagConstraints.WEST;
		gbc_btnAddSample.gridx = 0;
		gbc_btnAddSample.gridy = 3;
		panel_1.add(btnAddSample, gbc_btnAddSample);

		JPanel memoryPanel = new JPanel();
		memoryPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc_memoryPanel = new GridBagConstraints();
		gbc_memoryPanel.weightx = 1.0;
		gbc_memoryPanel.insets = new Insets(0, 0, 5, 0);
		gbc_memoryPanel.fill = GridBagConstraints.BOTH;
		gbc_memoryPanel.gridx = 0;
		gbc_memoryPanel.gridy = 4;
		panel_1.add(memoryPanel, gbc_memoryPanel);

		JLabel lblMemory = new JLabel("How much memory do you want to allow to IGV ?");
		GridBagConstraints gbc_lblMemory = new GridBagConstraints();
		gbc_lblMemory.weightx = 1.0;
		gbc_lblMemory.insets = new Insets(10, 10, 5, 20);
		gbc_lblMemory.anchor = GridBagConstraints.NORTH;
		gbc_lblMemory.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblMemory.gridx = 0;
		gbc_lblMemory.gridy = 0;
		memoryPanel.add(lblMemory, gbc_lblMemory);

		memorySlider = new JSlider(0, (int)Tools.getMaxPhysicalMemory(), 1024);
		memorySlider.setMajorTickSpacing(1024);
		memorySlider.setMinorTickSpacing(256);
		memorySlider.setPaintTicks(true);
		memorySlider.setPaintLabels(false);
		memorySlider.setSnapToTicks(true);
		memorySlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				int memory = source.getValue();
				if (memory == 0) memory = 256;
				lblAmount.setText("Memory allowed : " +((memory < 1024)?memory+" Mb":Tools.doubleToString(memory/1024.0, 1, false)+" Gb"));
			}
		});
		GridBagConstraints gbc_rdbtndbsnp = new GridBagConstraints();
		gbc_lblMemory.weightx = 1.0;
		gbc_rdbtndbsnp.fill = GridBagConstraints.HORIZONTAL;
		gbc_rdbtndbsnp.insets = new Insets(5, 20, 5, 10);
		gbc_rdbtndbsnp.gridx = 0;
		gbc_rdbtndbsnp.gridy = 1;
		memoryPanel.add(memorySlider, gbc_rdbtndbsnp);			

		lblAmount = new JLabel("Memory allowed : 1 Gb");
		GridBagConstraints gbc_lblAmount = new GridBagConstraints();
		gbc_lblAmount.weightx = 1.0;
		gbc_lblAmount.insets = new Insets(10, 10, 5, 20);
		gbc_lblAmount.anchor = GridBagConstraints.NORTH;
		gbc_lblAmount.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblAmount.gridx = 0;
		gbc_lblAmount.gridy = 2;
		memoryPanel.add(lblAmount, gbc_lblAmount);

		for (String sample : samples.keySet()){
			addSample(sample, samples.get(sample));
		}

	}

	private void searchSample(){
		Map<AnalysisFull, Set<String>> bams = new TreeMap<AnalysisFull, Set<String>>();
		for (Component c : bamPanel.getComponents()){
			if (c instanceof JCheckBox){
				if (!((JCheckBox)c).getText().equals("Alignment (BAM)")) {
					AnalysisFull a = analysesBam.get(c);
					if (!bams.containsKey(a)){
						bams.put(a, new TreeSet<String>());
					}
					bams.get(a).add(((JCheckBox)c).getText());
				}
			}
		}
		AskSamplesDialog ask = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), null, bams, true);
		Tools.centerWindow(ask, false);
		ask.setVisible(true);
		for (Entry<AnalysisFull,Set<String>> e : ask.getMultiAnalysisSelection().entrySet()){
			for (String sample : e.getValue()){
				if (!bams.containsKey(e.getKey())){
					bams.put(e.getKey(), new TreeSet<String>());
				}
				if (!bams.get(e.getKey()).contains(sample)){
					addSample(sample, e.getKey());
					pack();
				}
			}
		}
	}

	private void addSample(String sample, AnalysisFull analysis){
		JCheckBox rdbtnBamSelection = new JCheckBox(sample);
		rdbtnBamSelection.setSelected(true);
		GridBagConstraints gbc_rdbtnBamSelection = new GridBagConstraints();
		gbc_rdbtnBamSelection.fill = GridBagConstraints.HORIZONTAL;
		gbc_rdbtnBamSelection.insets = new Insets(5, 20, 5, 10);
		gbc_rdbtnBamSelection.gridx = 0;
		gbc_rdbtnBamSelection.gridy = ypat;
		bamPanel.add(rdbtnBamSelection, gbc_rdbtnBamSelection);
		analysesBam.put(rdbtnBamSelection, analysis);

		JCheckBox rdbtnVcfSelection = new JCheckBox(sample);
		rdbtnVcfSelection.setSelected(true);
		GridBagConstraints gbc_rdbtnVcfSelection = new GridBagConstraints();
		gbc_rdbtnVcfSelection.fill = GridBagConstraints.HORIZONTAL;
		gbc_rdbtnVcfSelection.insets = new Insets(5, 20, 5, 10);
		gbc_rdbtnVcfSelection.gridx = 0;
		gbc_rdbtnVcfSelection.gridy = ypat++;
		vcfPanel.add(rdbtnVcfSelection, gbc_rdbtnVcfSelection);
		analysesVcf.put(rdbtnVcfSelection, analysis);
	}

	public void viewInIGV(){
		String selectedVariant = ""; 
		Map<AnalysisFull, Set<String>> selectedBams = new TreeMap<AnalysisFull, Set<String>>();
		Map<AnalysisFull, Set<String>> selectedVcfs = new TreeMap<AnalysisFull, Set<String>>();
		Set<String> selectedDbsnp = new TreeSet<String>();
		for (Component c : variantsPanel.getComponents()){
			if (c instanceof JRadioButton){
				if (((JRadioButton)c).isSelected()) {
					selectedVariant = ((JRadioButton)c).getText();
				}
			}
		}
		for (Component c : bamPanel.getComponents()){
			if (c instanceof JCheckBox){
				if (!((JCheckBox)c).getText().equals("Alignment (BAM)") && ((JCheckBox)c).isSelected()) {
					AnalysisFull a = analysesBam.get(c);
					if (!selectedBams.containsKey(a)){
						selectedBams.put(a, new TreeSet<String>());
					}
					selectedBams.get(a).add(((JCheckBox)c).getText());
				}
			}
		}
		for (Component c : vcfPanel.getComponents()){
			if (c instanceof JCheckBox){
				if (!((JCheckBox)c).getText().equals("Variants (VCF)") && ((JCheckBox)c).isSelected()) {
					AnalysisFull a = analysesVcf.get(c);
					if (!selectedVcfs.containsKey(a)){
						selectedVcfs.put(a, new TreeSet<String>());
					}
					selectedVcfs.get(a).add(((JCheckBox)c).getText());
				}
			}
		}
		for (Component c : dbsnpPanel.getComponents()){
			if (c instanceof JCheckBox){
				if (((JCheckBox)c).isSelected()) {
					selectedDbsnp.add(((JCheckBox)c).getText());
				}
			}
		}
		if (selectedBams.isEmpty()){
			JOptionPane.showMessageDialog(new JFrame(), "You must select at least 1 sample", "Show variant in IGV",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iIGV,64));
			return;
		}
		Process p = null;
		try{	

			//For New process
			String igvPath = Highlander.getParameters().getIGV();
			String heapSize = memorySlider.getValue()+"m";
			if (heapSize.equals("0m")) heapSize = "256m";
			String[] exec;
			if (Tools.isMac()) {
				exec = new String[]{"java","-Xmx"+heapSize,"-Dproduction=true","-Xdock:name=\"IGV\"","-Dapple.laf.useScreenMenuBar=true","-Djava.net.preferIPv4Stack=true","-jar","igv.jar","",""};
			}else if (Tools.isWindows()){
				exec = new String[]{"java","-Xmx"+heapSize,"-Dproduction=true","-Djava.net.preferIPv4Stack=true","-Dsun.java2d.noddraw=true","-jar","igv.jar","",""};
			}else{
				exec = new String[]{"java","-Xmx"+heapSize,"-Dproduction=true","-Dapple.laf.useScreenMenuBar=true","-Djava.net.preferIPv4Stack=true","-jar","igv.jar","",""};
			}

			String refURL = Highlander.getParameters().getUrlForDbsnpVcfs();
			String dbsnp = "";
			for (String ver : selectedDbsnp){
				String url = refURL+"/"+dbsnpFiles.get(ver);
				if (Tools.exists(url)){
					dbsnp += url+",";			
				}else{
					JOptionPane.showMessageDialog(new JFrame(),  url + " was not found on the server, you should warn the administrator", "Show variant in IGV",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}			
			String vcfs = "";
			for (AnalysisFull a : selectedVcfs.keySet()){
				for (String sample : selectedVcfs.get(a)){
					if (Tools.exists(a.getVcfURL(sample))){
						vcfs += a.getVcfURL(sample)+",";
					}else{
						JOptionPane.showMessageDialog(new JFrame(),  a.getVcfURL(sample) + " was not found on the server, you should warn the administrator", "Show variant in IGV",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}			
			}
			String bams = "";
			for (AnalysisFull a : selectedBams.keySet()){
				for (String sample : selectedBams.get(a)){
					String url = a.getBamURL(sample); 
					if (Tools.exists(url)){
						bams += url + ",";
					}else{
						JOptionPane.showMessageDialog(new JFrame(),  url + " was not found on the server, you should warn the administrator", "Show variant in IGV",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
			String urls = dbsnp+vcfs+bams;
			if (urls.length() > 0) urls = urls.substring(0,urls.length()-1);

			//New process

			exec[exec.length-2] = urls;
			exec[exec.length-1] = selectedVariant;
			p = Runtime.getRuntime().exec(exec, null, new File(igvPath));
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
			errorGobbler.start();
			outputGobbler.start();

			//New Frame
			/*
			final String params = urls+" "+selectedVariant;
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					igvFrame.setIconImage(Resources.getScaledIcon(Resources.iIGV,64).getImage());
					IGVAccess.openIGV(igvFrame, (params).split(" "), alreadyOpen);
					alreadyOpen = true;
				}});
			 */
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem when retreiving variant data", ex), "Show variant in IGV",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		dispose();
		//New process
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				igvFrame.setLayout(new BorderLayout());
				console.setEditable(false);
				console.setLineWrap(true);
				console.setWrapStyleWord(true);
				igvFrame.add(new JScrollPane(console), BorderLayout.CENTER);
				igvFrame.setIconImage(Resources.getScaledIcon(Resources.iIGV,64).getImage());
				igvFrame.setTitle("IGV Console");
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				int width = screenSize.width - (screenSize.width/3);
				int height = screenSize.height - (screenSize.height/3*2);
				igvFrame.setSize(new Dimension(width,height));
				Tools.centerWindow(igvFrame, false);
				igvFrame.setVisible(true);
			}
		});
		final Process igvProcess = p;
		new Thread(new Runnable() {

			@Override
			public void run() {				
				if (igvProcess != null){
					try{
						igvProcess.waitFor();
					}catch(InterruptedException ex){
						ex.printStackTrace();
					}
				}
				igvFrame.dispose();
			}
		}, "OpenIGV.viewInIGV").start();
	}

	class StreamGobbler extends Thread {
		InputStream is;

		// reads everything from is until empty. 
		StreamGobbler(InputStream is) {
			this.is = is;
		}

		@Override
		public void run() {
			try (InputStreamReader isr = new InputStreamReader(is)){
				try (BufferedReader br = new BufferedReader(isr)){
					String line=null;
					while ( (line = br.readLine()) != null){
						console.append(line+"\n");
						console.setCaretPosition(console.getText().length());
					}
				}
			} catch (IOException ex) {
				Tools.exception(ex);  
			}
		}
	}

}
