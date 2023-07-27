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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.AskSamplesDialog;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Report;

public class FileDownloader extends JFrame {

	private JCheckBox boxFileBam = new JCheckBox("Alignment (BAM)");
	private JCheckBox boxFileVcf = new JCheckBox("Variants (VCF)");
	private Map<Report, JCheckBox> reports = new TreeMap<>();
	private JTextField txtFieldOutputDir;
	private JCheckBox boxSubDir;
	private JSpinner spinnerConcurrent = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
	private Map<AnalysisFull, Set<String>> samples = new TreeMap<>();
	private JPanel panelDownload;
	private int ypat = 0;
	private Map<AnalysisFull, Map<String, JProgressBar>> progressBars = new TreeMap<>();
	private Set<String> errors = new TreeSet<>();		

	public FileDownloader() {
		try {
			for (Report report : Report.getAvailableReports()) {
				reports.put(report, new JCheckBox(report.getDescription() + " ("+report.getSoftware()+")"));
			}
		}catch(Exception ex) {
			Tools.exception(ex);
			ex.printStackTrace();
		}
		initUI();
		pack();
		setSize(getWidth(), (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()*0.8));
		setPreferredSize(new Dimension(getWidth(), (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()*0.8)));
	}

	private void initUI(){
		setTitle("File downloader");
		setIconImage(Resources.getScaledIcon(Resources.iDownload, 64).getImage());

		setLayout(new BorderLayout());

		JPanel south = new JPanel();	
		getContentPane().add(south, BorderLayout.SOUTH);
		JButton downloadButton = new JButton("Start download", Resources.getScaledIcon(Resources.iDownload, 40));
		downloadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						start();
					}
				}, "FileDownloader.download").start();
			}
		});
		south.add(downloadButton);

		JPanel north = new JPanel(new BorderLayout(5,5));
		getContentPane().add(north, BorderLayout.NORTH);
		int numRows = (reports.size()+2) / 4;
		if ((reports.size()+2)%4 > 0) numRows++;
		JPanel filesPanel = new JPanel(new GridLayout(numRows, 4, 5, 5));
		filesPanel.setBorder(new TitledBorder(null, "Files", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		filesPanel.add(boxFileBam);
		filesPanel.add(boxFileVcf);
		for (Report report : reports.keySet()) {
			JCheckBox box = reports.get(report);
			filesPanel.add(box);
			String tooltip = "<html>";
			tooltip += report.getSoftware() + " files are available for samples selected in the following analyses:<br><ul>";
			for (Analysis analysis : report.getAnalyses()) {
				tooltip += "<li>" + analysis + "</li>";
			}
			tooltip += "</ul>";
			tooltip += "Available files are:<br><ul>";
			for (String file : report.getFiles()) {
				tooltip += "<li>" + file;
				if (report.getFileDescription(file).length() > 0) tooltip += " ("+report.getFileDescription(file)+")";
				tooltip += "</li>";
			}
			tooltip += "</ul></html>";
			box.setToolTipText(tooltip);
		}
		north.add(filesPanel, BorderLayout.NORTH);
		JPanel outputPanel = new JPanel(new BorderLayout());
		outputPanel.setBorder(new TitledBorder(null, "Output", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		north.add(outputPanel, BorderLayout.SOUTH);
		JPanel outputDirPanel = new JPanel(new GridBagLayout());
		outputPanel.add(outputDirPanel, BorderLayout.NORTH);
		outputDirPanel.add(new JLabel("Local directory"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));
		txtFieldOutputDir = new JTextField(Tools.getHomeDirectory().toString());
		outputDirPanel.add(txtFieldOutputDir, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 0, 5), 0, 0));
		JButton browseDir = new JButton(Resources.getScaledIcon(Resources.iFolder, 24));
		browseDir.setPreferredSize(new Dimension(32,32));
		browseDir.setToolTipText("Browse");
		browseDir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						JFileChooser chooser = new JFileChooser(Tools.getHomeDirectory().toString()	);
						chooser.setDialogTitle("Select the download directory");
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						chooser.setMultiSelectionEnabled(false);
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						int dirChooserRes = chooser.showOpenDialog(FileDownloader.this) ;
						if (dirChooserRes == JFileChooser.APPROVE_OPTION) {
							txtFieldOutputDir.setText(chooser.getSelectedFile().getAbsolutePath());
						}
					}
				}, "FileDownloader.browseDir").start();
			}
		});
		outputDirPanel.add(browseDir, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));
		JPanel outputOtherOpPanel = new JPanel(new BorderLayout());
		outputPanel.add(outputOtherOpPanel, BorderLayout.SOUTH);
		boxSubDir = new JCheckBox("Create a directory for each sample");
		outputOtherOpPanel.add(boxSubDir, BorderLayout.NORTH);
		JPanel concurrentPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 2));
		concurrentPanel.add(new JLabel("Number of concurrent downloads "));
		concurrentPanel.add(spinnerConcurrent);
		outputOtherOpPanel.add(concurrentPanel, BorderLayout.SOUTH);

		JPanel center = new JPanel(new BorderLayout());
		getContentPane().add(center, BorderLayout.CENTER);
		center.setBorder(new TitledBorder(null, "Samples", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JButton selectSamples = new JButton("Select samples", Resources.getScaledIcon(Resources.iPatients, 24));
		selectSamples.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						AskSamplesDialog ask = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), null, samples, true);
						Tools.centerWindow(ask, false);
						ask.setVisible(true);
						for (Entry<AnalysisFull,Set<String>> e : ask.getMultiAnalysisSelection().entrySet()){
							for (String sample : e.getValue()){
								if (!samples.containsKey(e.getKey())){
									samples.put(e.getKey(), new TreeSet<String>());
								}
								if (!samples.get(e.getKey()).contains(sample)){
									addSample(sample, e.getKey());
									pack();
								}
							}
						}
					}
				}, "FileDownloader.selectSamples").start();
			}
		});
		center.add(selectSamples, BorderLayout.NORTH);
		panelDownload = new JPanel(new GridBagLayout());
		JScrollPane scroll = new JScrollPane(panelDownload);
		center.add(scroll, BorderLayout.CENTER);
	}

	private void addSample(String sample, AnalysisFull analysis){
		samples.get(analysis).add(sample);
		JProgressBar progress = new JProgressBar();
		if (!progressBars.containsKey(analysis)) {
			progressBars.put(analysis, new TreeMap<String, JProgressBar>());
		}
		progressBars.get(analysis).put(sample, progress);
		progress.setString(sample);
		progress.setStringPainted(true);

		GridBagConstraints gbc_rdbtnBamSelection = new GridBagConstraints();
		gbc_rdbtnBamSelection.fill = GridBagConstraints.HORIZONTAL;
		gbc_rdbtnBamSelection.insets = new Insets(2, 5, 2, 5);
		gbc_rdbtnBamSelection.gridx = 0;
		gbc_rdbtnBamSelection.gridy = ypat++;
		gbc_rdbtnBamSelection.weightx = 1.0;
		gbc_rdbtnBamSelection.fill = GridBagConstraints.HORIZONTAL;
		panelDownload.add(progress, gbc_rdbtnBamSelection);
	}

	private void start() {
		ExecutorService executor = Executors.newFixedThreadPool((Integer)spinnerConcurrent.getValue());
		for (final AnalysisFull analysis : progressBars.keySet()) {
			for (final String sample : progressBars.get(analysis).keySet()){
				final JProgressBar p = progressBars.get(analysis).get(sample);
				executor.execute(new Runnable() {
					@Override
					public void run() {
						download(analysis, sample, p);
					}
				});
			}
		}
		executor.shutdown();
		try {
			executor.awaitTermination(100, TimeUnit.HOURS);
		}catch(InterruptedException ex) {
			ex.printStackTrace();
		}
		dispose();
		int i=0;
		JPanel message = new JPanel(new GridBagLayout());
		message.add(new JLabel("Files available in "+txtFieldOutputDir.getText()), new GridBagConstraints(0, i++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		if (!errors.isEmpty()) {
			message.add(new JLabel("The following downloads have encountered a problem: "), new GridBagConstraints(0, i++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
			for (String error : errors) {
				message.add(new JTextField(error), new GridBagConstraints(0, i++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
			}
		}
		JOptionPane.showMessageDialog(new JFrame(), message, "Download original files",	JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iDownload,64));
	}

	private void download(AnalysisFull analysis, String sample, JProgressBar p) {
		File localDir = (boxSubDir.isSelected()) ? new File(txtFieldOutputDir.getText()+"/"+sample) : new File(txtFieldOutputDir.getText());
		localDir.mkdirs();
		p.setIndeterminate(true);
		for (Report report : reports.keySet()) {
			JCheckBox box = reports.get(report);
			if (box.isSelected()) {
				if (report.getAnalyses().contains(analysis)) {
					p.setString("Downloading " + sample + " "+report.getDescription()+" ("+report.getSoftware()+")");
					p.setStringPainted(true);
					String project = "unknown_run";
					try {
						try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT run_label "
								+ "FROM projects JOIN projects_analyses USING (project_id) "
								+ "WHERE sample = '"+sample+"' AND analysis = '"+analysis+"'")) {
							if (res.next()){
								project = res.getString(1);
							}
						}
					}catch(Exception ex) {
						errors.add(sample + " "+report.getSoftware()+" [" +ex.getMessage()+ "]");
						ex.printStackTrace();
					}
					for (String file :report.getFiles()) {
						String output = localDir + "/" + sample + file;
						try {
							Tools.httpDownload(report.getUrlForFile(file, project, sample), new File(output));
						}catch(IOException ex) {
							errors.add(output + " [ " +ex.getMessage()+ " ]");
							ex.printStackTrace();
						}
					}
				}
			}
		}
		if (boxFileVcf.isSelected()) {
			p.setString("Downloading " + sample + " variants (VCF)");
			p.setStringPainted(true);
			String output = localDir + "/" + sample + ".vcf";
			try {
				Tools.httpDownload(new URL(analysis.getVcfURL(sample)), new File(output));
			}catch(IOException ex) {
				errors.add(output + " [ " +ex.getMessage()+ " ]");
				ex.printStackTrace();
			}
		}
		if (boxFileBam.isSelected()) {
			p.setString("Downloading " + sample + " alignment (BAM)");
			p.setStringPainted(true);
			String bam = analysis.getBamURL(sample);
			String output = localDir + "/" + sample + ".bam";
			try {
				Tools.httpDownload(new URL(bam), new File(output));
			}catch(IOException ex) {
				errors.add(output + " [ " +ex.getMessage()+ " ]");
				ex.printStackTrace();
			}
			p.setString("Downloading " + sample + " alignment index (BAI)");
			p.setStringPainted(true);
			try {
				Tools.httpDownload(new URL(bam.replace(".bam", ".bai")), new File(output.replace(".bam", ".bai")));
			}catch(IOException ex) {
				errors.add(output.replace(".bam", ".bai") + " [ " +ex.getMessage()+ " ]");
				ex.printStackTrace();
			}
		}
		p.setIndeterminate(false);
		p.setMaximum(1);
		p.setValue(1);
		p.setString(sample + " downloaded");
		p.setStringPainted(true);
	}
}
