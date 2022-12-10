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

package be.uclouvain.ngs.highlander.UI.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Report;

/**
 * @author Raphael Helaers
 */

public class Kraken extends JFrame {

	private final Map<AnalysisFull, Set<String>> samples;
	private Report krakenReport;

	private JTextField txtFieldOutputDir;
	private List<JButton> missingResults = new ArrayList<JButton>();
	private List<JButton> runningJobs = new ArrayList<JButton>();

	static private WaitingPanel waitingPanel;

	private boolean frameVisible = true;
	
	public Kraken(Report krakenReport, Map<AnalysisFull, Set<String>> samples) {
		super();
		this.samples = new TreeMap<AnalysisFull, Set<String>>(samples);
		this.krakenReport = krakenReport;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height - (int)(screenSize.height*0.06);
		int width = height;
		setSize(new Dimension(width,height));		
		initUI();
		pack();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (frameVisible){
					//copy the list of buttons in a new one to avoid concurrent modification when a button is removed from the list
					List<JButton> list = new ArrayList<JButton>(runningJobs);
					for (JButton button : list) {
						long when  = System.currentTimeMillis();
						ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Check sample status", when, 0);
						for (ActionListener listener : button.getActionListeners()) {
							listener.actionPerformed(event);
						}
					}
					try{
						Thread.sleep(30_000);
					}catch (InterruptedException ex){
						Tools.exception(ex);
					}
				}
			}
		}, "Kraken.checkForSampleStatus").start();
	}

	private void initUI(){
		setTitle("Kraken");
		setIconImage(Resources.getScaledIcon(Resources.iKraken, 64).getImage());

		setLayout(new BorderLayout());

		JPanel south = new JPanel();
		getContentPane().add(south, BorderLayout.SOUTH);

		JButton downloadReportButton = new JButton("Download all available reports", Resources.getScaledIcon(Resources.iDownload, 24));
		downloadReportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (String file : krakenReport.getFiles()) {
					if (file.contains(".txt")) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										waitingPanel.setVisible(true);
										waitingPanel.start();
									}
								});
								try {
									File localDir = new File(txtFieldOutputDir.getText());
									for (Analysis analysis : samples.keySet()) {
										for (String sample : samples.get(analysis)) {
											if (isResultsAvailable(analysis, sample)) {
												String output = localDir + "/" + sample + file;
												try {
													Tools.httpDownload(krakenReport.getUrlForFile(file, getProject(analysis, sample), sample), new File(output));
												}catch(IOException ex) {
													Tools.exception(ex);
													JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Kraken",
															JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
												}																							
											}
										}
									}
									JOptionPane.showMessageDialog(new JFrame(), "All reports downloaded in " + txtFieldOutputDir.getText(), "Download all available reports",	JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iDownload,64));
								}catch (Exception ex) {
									Tools.exception(ex);
									JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Kraken",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
								}
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
		south.add(downloadReportButton);
		
		JButton openPavianButton = new JButton("Open Pavian", Resources.getScaledIcon(Resources.iPavian, 24));
		openPavianButton.setToolTipText("<html>Pavian is a interactive browser application for analyzing and visualization metagenomics classification results from classifiers such as Kraken 2.<br>"
				+ "Pavian also provides an alignment viewer for validation of matches to a particular genome.<br>"
				+ "Download reports from your samples of interest, then upload them together in Pavian.</html>");
		openPavianButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (Highlander.getParameters().getPavian() != null && Tools.exists(Highlander.getParameters().getPavian())) {
							Tools.openURL(Highlander.getParameters().getPavian());							
						}else {
							Tools.openURL("https://ccb.jhu.edu/software/pavian/");
						}
					}
				}).start();
			}
		});
		south.add(openPavianButton);

		JButton runAllMissingButton = new JButton("Run Kraken on all missing", Resources.getScaledIcon(Resources.iRun, 24));
		runAllMissingButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (JButton button : missingResults) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							long when  = System.currentTimeMillis();
							ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Launch Kraken", when, 0);
							for (ActionListener listener : button.getActionListeners()) {
								listener.actionPerformed(event);
							}
						}
					}).start();
				}
			}
		});
		south.add(runAllMissingButton);
		
		JButton btnClose = new JButton("Close", Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frameVisible = false;
				dispose();
			}
		});
		south.add(btnClose);

		JPanel center = new JPanel(new GridBagLayout());
		getContentPane().add(new JScrollPane(center), BorderLayout.CENTER);

		int a=0;

		JPanel outputDirPanel = new JPanel(new GridBagLayout());
		center.add(outputDirPanel, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 0, 20), 0, 0));
		outputDirPanel.add(new JLabel("Download directory"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));
		txtFieldOutputDir = new JTextField(Tools.getHomeDirectory().toString());
		outputDirPanel.add(txtFieldOutputDir, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 0, 5), 0, 0));
		JButton browseDir = new JButton(Resources.getScaledIcon(Resources.iFolder, 24));
		browseDir.setPreferredSize(new Dimension(32,32));
		browseDir.setToolTipText("Browse");
		browseDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						JFileChooser chooser = new JFileChooser(Tools.getHomeDirectory().toString());
						chooser.setDialogTitle("Select the download directory");
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						chooser.setMultiSelectionEnabled(false);
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						int dirChooserRes = chooser.showOpenDialog(Kraken.this) ;
						if (dirChooserRes == JFileChooser.APPROVE_OPTION) {
							txtFieldOutputDir.setText(chooser.getSelectedFile().getAbsolutePath());
						}
					}
				}, "Kraken.browseDir").start();
			}
		});
		outputDirPanel.add(browseDir, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));

		for (Analysis analysis : samples.keySet()) {
			JPanel panel_analysis = new JPanel(new GridBagLayout());
			panel_analysis.setBorder(new TitledBorder(null, analysis.toString(), TitledBorder.LEADING, TitledBorder.TOP, null, null));
			center.add(panel_analysis, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 0, 20), 0, 0));
			int y=0;
			for (String sample : samples.get(analysis)) {
				panel_analysis.add(getPanel(analysis, sample, y), new GridBagConstraints(0, y, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
				y++;
			}
		}
		center.add(new JPanel(), new GridBagConstraints(0, a++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			frameVisible = false;
			dispose();
		}
	}

	private JPanel getPanel(Analysis analysis, String sample, int pos) {
		JPanel panel = new JPanel(new GridBagLayout());
		if (pos%2 == 0) panel.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Indigo));
		else panel.setBackground(Resources.getTableOddRowBackgroundColor(Palette.Indigo));
		boolean resultsAvailable = isResultsAvailable(analysis, sample);
		JLabel label = new JLabel(sample, (resultsAvailable ? Resources.getScaledIcon(Resources.iShinyBallGreen, 24) : Resources.getScaledIcon(Resources.iShinyBallRed, 24)), SwingConstants.LEADING);
		panel.add(label, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		if (resultsAvailable) {
			setViewButtons(panel, analysis, sample);
		}else {
			JButton launchKrakenButton = new JButton("Run Kraken", Resources.getScaledIcon(Resources.iRun, 24));
			launchKrakenButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							if (runKraken(analysis, sample)) {
								label.setIcon(Resources.getScaledIcon(Resources.iShinyBallPink, 24));
								panel.remove(launchKrakenButton);
								missingResults.remove(launchKrakenButton);
								JButton checkKrakenStatusButton = new JButton("Check status", Resources.getScaledIcon(Resources.iUpdater, 24));
								runningJobs.add(checkKrakenStatusButton);
								checkKrakenStatusButton.setToolTipText("<html>You'll be warn by email when results are available<br>You can use this button to check if results are available.<br>If they are, the ball will become green and new buttons will replace this one.<br>Kraken can take between 20 minutes and 1 hour to finish depending on the sample.</html>");
								checkKrakenStatusButton.addActionListener(new ActionListener() {
									@Override
									public void actionPerformed(ActionEvent e) {
										if (isResultsAvailable(analysis, sample)) {
											label.setIcon(Resources.getScaledIcon(Resources.iShinyBallGreen, 24));
											panel.remove(checkKrakenStatusButton);
											runningJobs.remove(checkKrakenStatusButton);
											setViewButtons(panel, analysis, sample);
											panel.validate();								
										}
									}
								});
								panel.add(checkKrakenStatusButton, new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
								panel.validate();								
							}
						}
					}).start();
				}
			});
			missingResults.add(launchKrakenButton);
			panel.add(launchKrakenButton, new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		}
		return panel;
	}

	private void setViewButtons(JPanel panel, Analysis analysis, String sample) {
		JButton viewHTMLButton = new JButton("View results", Resources.getScaledIcon(Resources.iKraken, 24));
		viewHTMLButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (String file : krakenReport.getFiles()) {
					if (file.contains("html")) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									Tools.openURL(krakenReport.getUrlForFile(file, getProject(analysis, sample), sample).toURI());
								}catch (Exception ex) {
									Tools.exception(ex);
									JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Kraken",
											JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
								}
							}
						}).start();
					}
				}
			}
		});
		panel.add(viewHTMLButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		JButton downloadReportButton = new JButton("Download report", Resources.getScaledIcon(Resources.iDownload, 24));
		downloadReportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (String file : krakenReport.getFiles()) {
					if (file.contains(".txt")) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									File localDir = new File(txtFieldOutputDir.getText());
									String output = localDir + "/" + sample + file;
									try {
										Tools.httpDownload(krakenReport.getUrlForFile(file, getProject(analysis, sample), sample), new File(output));
									}catch(IOException ex) {
										Tools.exception(ex);
										JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Kraken",
												JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
									}
								}catch (Exception ex) {
									Tools.exception(ex);
									JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Kraken",
											JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
								}
							}
						}).start();
					}
				}
			}
		});
		panel.add(downloadReportButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
	}
	
	private boolean isResultsAvailable(Analysis analysis, String sample) {
		for (String file : krakenReport.getFiles()) {
			try {
				URL url = krakenReport.getUrlForFile(file, getProject(analysis, sample), sample);
				if (!Tools.exists(url.toString())) {
					return false;
				}
			}catch (MalformedURLException ex) {
				Tools.exception(ex);
				return false;
			}
		}
		return true;
	}

	private String getProject(Analysis analysis, String sample) {
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
			Tools.exception(ex);
		}
		return project;
	}

	private boolean runKraken(Analysis analysis, String sample) {
		if (Highlander.getParameters().getUrlForPhpScripts() == null) {
			JOptionPane.showMessageDialog(new JFrame(),  "Launching Kraken impossible: you should configure 'server > php' parameter in settings.xml", "Kraken",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return false;
		}
		try {
			HttpClient httpClient = new HttpClient();
			boolean bypass = false;
			if (System.getProperty("http.nonProxyHosts") != null) {
				for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
					if ((Highlander.getParameters().getUrlForPhpScripts()+"/kraken.php").toLowerCase().contains(host.toLowerCase())) bypass = true;
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
			PostMethod post = new PostMethod(Highlander.getParameters().getUrlForPhpScripts()+"/kraken.php");
			NameValuePair[] data = {
					new NameValuePair("analysis", analysis.toString()),
					new NameValuePair("sample", sample),
					new NameValuePair("email", Highlander.getLoggedUser().getEmail()),
			};
			post.addParameters(data);
			int httpRes = httpClient.executeMethod(post); 
			if (httpRes == 200) {
				StringBuilder sb = new StringBuilder();
				try (InputStreamReader isr = new InputStreamReader(post.getResponseBodyAsStream())){
					try (BufferedReader br = new BufferedReader(isr)){
						String line = null;
						while(((line = br.readLine()) != null)) {
							System.out.println(line);				
							sb.append(line+"\n");
							if (line.contains("*exitcode^1*")) {
								throw new Exception(sb.toString());
							}
						}
					}
				}
				return true;
			}else {
				JOptionPane.showMessageDialog(new JFrame(),  "Cannot launch Kraken on the server, HTTP error " + httpRes, "Kraken",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Kraken",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return false;
		}		
	}
}
