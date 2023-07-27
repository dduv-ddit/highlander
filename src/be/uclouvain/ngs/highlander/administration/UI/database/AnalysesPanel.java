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

package be.uclouvain.ngs.highlander.administration.UI.database;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;
import be.uclouvain.ngs.highlander.tools.ExomeBed;

/**
* @author Raphael Helaers
*/

public class AnalysesPanel extends ManagerPanel {
	
	private DefaultListModel<AnalysisFull> listAnalysesModel = new DefaultListModel<>();
	private JList<AnalysisFull> listAnalyses = new JList<>(listAnalysesModel);
	private JScrollPane scrollData = new JScrollPane();

	private JCheckBox generateBed;
	private JCheckBox includeCDS;
	private JCheckBox includeUTR;
	private Map<String,Boolean> biotypes = new TreeMap<>();

	public AnalysesPanel(ProjectManager manager){
		super(manager);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		add(splitPane, BorderLayout.CENTER);

		JPanel panel_left = new JPanel(new BorderLayout(5,5));
		panel_left.setBorder(BorderFactory.createTitledBorder("Analyses"));
		splitPane.setLeftComponent(panel_left);
		
		JScrollPane scrollPane_left = new JScrollPane();
		panel_left.add(scrollPane_left, BorderLayout.CENTER);

		for (AnalysisFull analysis : manager.getAvailableAnalysesAsArray()) {
			listAnalysesModel.addElement(analysis);
		}
		listAnalyses.setFixedCellHeight(20); //To avoid a 'random bug' to sometimes (super) oversize one cell
		listAnalyses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listAnalyses.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					fill(listAnalyses.getSelectedValue());
				}
			}
		});
		scrollPane_left.setViewportView(listAnalyses);
		
		JPanel panel_middle = new JPanel();
		panel_left.add(panel_middle, BorderLayout.EAST);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_middle.setLayout(gbl_panel_2);

		JButton button_up = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleUp, 24));
		button_up.setToolTipText("Put selected analysis before in order of appearance in Highlander toolbar");
		button_up.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				reorderAnalysis(listAnalyses.getSelectedIndex(), true);
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 0;
		panel_middle.add(button_up, gbc_button);

		JButton button_down = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleDown, 24));
		button_down.setToolTipText("Put selected analysis after in order of appearance in Highlander toolbar");
		button_down.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				reorderAnalysis(listAnalyses.getSelectedIndex(), false);
			}
		});
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 1;
		panel_middle.add(button_down, gbc_button_1);
		
		JPanel panel_right = new JPanel(new BorderLayout());
		panel_right.setBorder(new EmptyBorder(10, 5, 0, 5));
		splitPane.setRightComponent(panel_right);
		
		panel_right.add(scrollData, BorderLayout.CENTER);
		
		JPanel southPanel = new JPanel(new WrapLayout(FlowLayout.CENTER));
		add(southPanel, BorderLayout.SOUTH);

		JButton createNewButton = new JButton("Create new analysis", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createAnalysis();
					}
				}, "AnalysesPanel.createAnalysis").start();

			}
		});
		southPanel.add(createNewButton);

		JButton setCoverageRegionsButton = new JButton("Set coverage regions", Resources.getScaledIcon(Resources.iCoverage, 16));
		setCoverageRegionsButton.setToolTipText("Coverage regions must match the bed file you use with MosDepth when importing coverage into Highlander");
		setCoverageRegionsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						setCoverageRegions(listAnalyses.getSelectedValue());
					}
				}, "AnalysesPanel.setCoverageRegions").start();
				
			}
		});
		southPanel.add(setCoverageRegionsButton);
		
		JButton deleteButton = new JButton("Delete analysis", Resources.getScaledIcon(Resources.i3dMinus, 16));
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteAnalysis(listAnalyses.getSelectedValue());
					}
				}, "AnalysesPanel.deleteAnalysis").start();

			}
		});
		southPanel.add(deleteButton);

		listAnalyses.setSelectedIndex(0);
	}
	
	public void fill(){
		fill(listAnalyses.getSelectedValue());
	}

	private void fill(AnalysisFull analysis){
		scrollData.setViewportView(null);
		if (analysis != null) {
			JPanel panel = new JPanel(new GridBagLayout());

			int row = 0;
			
			JButton buttonIcon = new JButton(Resources.getScaledIcon(analysis.getIcon(), 40));
			buttonIcon.setToolTipText("<html>The image representing this analysis, shown on buttons in Highlander.<br>"
					+ "Click to select a new image from a file. Image size doesn't matter, it will be rescaled.</html>");
			buttonIcon.setPreferredSize(new Dimension(54,54));
			panel.add(buttonIcon, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			buttonIcon.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							FileDialog d = new FileDialog(manager, "Select an image file", FileDialog.LOAD);
							Tools.centerWindow(d, false);
							d.setVisible(true);
							if (d.getFile() != null){
								String filename = d.getDirectory() + d.getFile();
								File file = new File(filename);
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										waitingPanel.setVisible(true);
										waitingPanel.start();
									}
								});
								try{
									analysis.updateIcon(file);
									buttonIcon.setIcon(Resources.getScaledIcon(analysis.getIcon(), 40));
								}catch(Exception ex){
									ProjectManager.toConsole(ex);			
								}
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										waitingPanel.setVisible(false);
										waitingPanel.stop();
									}
								});
							}
						}
					}, "AnalysesPanel.setIcon").start();
				}
			});
			row++;
			
			String tooltipReference = "<html>The reference genome used in this analysis.<br>"
					+ "<b>All VCFs imported in the analysis will be considered aligned to this reference</b> (reference set in the VCF is ignored).<br>"
					+ "Practically, it means that annotations and reference sequence of an analysis are fetched from databases linked to its reference.<br>"
					+ "You can modify or add references in the 'References' tab.<br>"
					+ "Note that you cannot mix 2 references in the same analysis, even if they are virtually the same (as GRCh37 and hg19).<br>"
					+ "However, in the case of such similar references, importing both in the same analysis shouldn't cause any problem."
					+ "</html>";
			JLabel labelReference = new JLabel("Reference");
			labelReference.setToolTipText(tooltipReference);
			panel.add(labelReference, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<Reference> boxReference = new JComboBox<>(Reference.getAvailableReferences().toArray(new Reference[0]));
			boxReference.setToolTipText(tooltipReference);
			panel.add(boxReference, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			boxReference.setSelectedItem(analysis.getReference());				
			boxReference.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<Reference> source = boxReference;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									Reference reference = (Reference)source.getSelectedItem();
									if (!reference.equals(analysis.getReference())) {
										analysis.setReference(reference);
										try {
											analysis.update();
										}catch(Exception ex) {
											ProjectManager.toConsole(ex);
										}
									}
								}
							});
						}
					}
				}
			});
			row++;
			
			String tooltipTarget = "<html>An analysis can only hold samples of the same sequencing target (<i>e.g.</i> you cannot mix WES and WGS in the same analysis).<br>"
					+ "It means that each sequencing target should have its own analysis.<br>"
					+ "</html>";
			JLabel labelTarget = new JLabel("Sequencing target");
			labelTarget.setToolTipText(tooltipTarget);
			panel.add(labelTarget, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<String> boxTarget = new JComboBox<>(manager.listSequencingTargets());
			boxTarget.setToolTipText(tooltipTarget);
			panel.add(boxTarget, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			boxTarget.setSelectedItem(analysis.getSequencingTarget());				
			boxTarget.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<String> source = boxTarget;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									String target = source.getSelectedItem().toString();
									if (!target.equals(analysis.getSequencingTarget())) {
										if (target.equals("Add new sequencing_target")) {
											Object res = JOptionPane.showInputDialog(manager,  "Set a name for this new sequencing target", "New sequencing target",
													JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
											if (res != null) {
												target = res.toString().trim();
												boxTarget.addItem(target);
											}else {
												boxTarget.setSelectedItem(analysis.getSequencingTarget());
												return;
											}
										}
										analysis.setSequencingTarget(target);
										try {
											analysis.update();
										}catch(Exception ex) {
											ProjectManager.toConsole(ex);
										}
									}
								}
							});
						}
					}
				}
			});
			row++;

			String tooltipCaller = "<html>The variant caller is used when importing VCF files, as they have sometimes specific fields.<br>"
					+ "If your variant caller is not in the list, set it to OTHER or to the most close considering the VCF format.<br>"
					+ "<ul>"
					+ "<li><b>MUTECT</b> is used for somatic callers (when you call using a paired samples, <i>e.g.</i> normal/tumor).</li>"
					+ "<li><b>TORRENT</b> is used with Ion Torrent technology (PGM, Proton, S5, ...).</li>"
					+ "<li><b>LIFESCOPE</b> is used with SOLID technology.</li>"
					+ "<li><b>GATK</b> and <b>OTHER</b> have no special parsing rules, it's standard VCF format. <br>The only difference is that Highlander will propose the BamOut tool for <b>GATK</b> (and <b>MUTECT</b>) analyses.</li>"
					+ "</ul>"
					+ "</html>";
			JLabel labelCaller = new JLabel("Variant caller");
			labelCaller.setToolTipText(tooltipCaller);
			panel.add(labelCaller, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<VariantCaller> boxCaller = new JComboBox<>(VariantCaller.values());
			boxCaller.setToolTipText(tooltipCaller);
			panel.add(boxCaller, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			boxCaller.setSelectedItem(analysis.getVariantCaller());				
			boxCaller.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<VariantCaller> source = boxCaller;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									VariantCaller caller = (VariantCaller)source.getSelectedItem();
									if (!caller.equals(analysis.getVariantCaller())) {
										analysis.setVariantCaller(caller);
										try {
											analysis.update();
										}catch(Exception ex) {
											ProjectManager.toConsole(ex);
										}
									}
								}
							});
						}
					}
				}
			});
			row++;

			String tooltipBamUrl = "<html>Highlander will read BAM files using an URL (<i>e.g.</i> to display the alignement).<br>"
					+ "The complete URL to access a BAM must be <code>[baseURL]/[sample].bam</code><br>"
					+ "<ul>"
					+ "<li><code>[baseURL]</code> is entered in this field (<i>URL of BAM repository</i>).</li>"
					+ "<li><code>[sample]</code> is the exact sample name as in Highlander.</li>"
					+ "</ul>"
					+ "Note that you can use special characters for [baseURL]:<br>"
					+ "<ul>"
					+ "<li><b>@</b> will be replaced by the sample name</li>"
					+ "<li><b>$</b> will be replaced by the pathology of the sample</li>"
					+ "<li><b>#</b> will be replaced by the population of the sample</li>"
					+ "</ul>"
					+ "Example:<br>"
					+ "<ul>"
					+ "<li>You set this field to 'http://192.168.1.1/bam/exomes_haplotype_caller/$/@'</li>"
					+ "<li>You have a sample 'SMP-00452' with pathology 'CANCER'</li>"
					+ "<li>The URL to access its BAM will be <code>http://192.168.1.1/bam/exomes_haplotype_caller/CANCER/SMP-00452/SMP-00452.bam</code></li>"
					+ "</ul>"				
					+ "If you don't have/want a web server for accessing BAM files, you can leave this field blank (but some tools in Highlander won't work, like alignment visualization or BamCheck).<br>"
					+ "</html>";
			JLabel labelBamUrl = new JLabel("URL of BAM repository");
			labelBamUrl.setToolTipText(tooltipBamUrl);
			panel.add(labelBamUrl, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtBamUrl = new JTextField(analysis.getBamRepository());
			txtBamUrl.setToolTipText(tooltipBamUrl);
			panel.add(txtBamUrl, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtBamUrl.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					AnalysisFull analysis = listAnalyses.getSelectedValue();
					analysis.setBamURL(inputTxtField.getText());
					try {
						analysis.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					return true;
				}
			});
			row++;
			
			String tooltipVcfUrl = "<html>Highlander will read VCF files using an URL.<br>"
					+ "The complete URL to access a VCF must be <code>[baseURL]/[sample].[extension]</code><br>"
					+ "<ul>"
					+ "<li><code>[baseURL]</code> is entered in this field (<i>URL of VCF repository</i>).</li>"
					+ "<li><code>[sample]</code> is the exact sample name as in Highlander.</li>"
					+ "<li><code>[extension]</code> is defined in the fields <i>File extension for VCF ...</i> (it can be different for SNVs and INDELs).</li>"
					+ "</ul>"
					+ "Note that you can use special characters for [baseURL]:<br>"
					+ "<ul>"
					+ "<li><b>@</b> will be replaced by the sample name</li>"
					+ "<li><b>$</b> will be replaced by the pathology of the sample</li>"
					+ "<li><b>#</b> will be replaced by the population of the sample</li>"
					+ "</ul>"
					+ "Example:<br>"
					+ "<ul>"
					+ "<li>You set the field <i>URL of VCF repository</i> to 'http://192.168.1.1/vcf/exomes_haplotype_caller'</li>"
					+ "<li>You set the field to <i>File extension for VCF</i> to '.gatk.snpEff.vcf'</li>"
					+ "<li>You have a sample 'SMP-00452'</li>"
					+ "<li>The URL to access its VCF (INDEL, SNV or both) will be <code>http://192.168.1.1/vcf/exomes_haplotype_caller/SMP-00452.gatk.snpEff.vcf</code></li>"
					+ "</ul>"				
					+ "Note that this URL is only used in Highlander when a user open IGV with tracks of the VCFs, and when a user want to download the original VCF.<br>"
					+ "If you don't have/want a web server for accessing VCF files, you can leave this field blank (but features described before will not work).<br>"
					+ "</html>";
			JLabel labelVcfUrl = new JLabel("URL of VCF repository");
			labelVcfUrl.setToolTipText(tooltipVcfUrl);
			panel.add(labelVcfUrl, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtVcfUrl = new JTextField(analysis.getVcfRepository());
			txtVcfUrl.setToolTipText(tooltipVcfUrl);
			panel.add(txtVcfUrl, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtVcfUrl.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					AnalysisFull analysis = listAnalyses.getSelectedValue();
					analysis.setVcfURL(inputTxtField.getText());
					try {
						analysis.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					return true;
				}
			});
			row++;
			
			String tooltipVcf = "<html>Highlander will read VCF files using an URL.<br>"
					+ "The complete URL to access a VCF must be <code>[baseURL]/[sample].[extension]</code><br>"
					+ "<ul>"
					+ "<li><code>[baseURL]</code> is entered in the field <i>URL of VCF repository</i>.</li>"
					+ "<li><code>[sample]</code> is the exact sample name as in Highlander.</li>"
					+ "<li><code>[extension]</code> is defined in the fields <i>File extension for VCF ...</i> (it can be different for SNVs and INDELs).</li>"
					+ "</ul>"
					+ "Note that you can use special characters for [baseURL]:<br>"
					+ "<ul>"
					+ "<li><b>@</b> will be replaced by the sample name</li>"
					+ "<li><b>$</b> will be replaced by the pathology of the sample</li>"
					+ "<li><b>#</b> will be replaced by the population of the sample</li>"
					+ "</ul>"
					+ "Example:<br>"
					+ "<ul>"
					+ "<li>You set the field <i>URL of VCF repository</i> to 'http://192.168.1.1/vcf/exomes_haplotype_caller'</li>"
					+ "<li>You set the field to <i>File extension for VCF</i> to '.gatk.snpEff.vcf'</li>"
					+ "<li>You have a sample 'SMP-00452'</li>"
					+ "<li>The URL to access its VCF will be <code>http://192.168.1.1/vcf/exomes_haplotype_caller/SMP-00452.gatk.snpEff.vcf</code></li>"
					+ "</ul>"				
					+ "Note that this URL is only used in Highlander when a user open IGV with tracks of the VCFs, and when a user want to download the original VCF.<br>"
					+ "If you don't have/want a web server for accessing VCF files, you can leave this field blank (but features described before will not work).<br>"
					+ "</html>";
			JLabel labelVcf = new JLabel("File extension for VCF");
			labelVcf.setToolTipText(tooltipVcf);
			panel.add(labelVcf, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtVcf = new JTextField(analysis.getVcfExtension());
			txtVcf.setToolTipText(tooltipVcf);
			panel.add(txtVcf, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtVcf.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					AnalysisFull analysis = listAnalyses.getSelectedValue();
					analysis.setVcfExtension(inputTxtField.getText());
					try {
						analysis.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					return true;
				}
			});
			row++;
						
			String tooltipBamDir = "<html>Full path of the directory containing BAM files of this analysis on the server.<br>"
					+ "The directory must contain all BAM with the exact name <code>[sample].bam</code>.<br>"
					+ "<ul>"
					+ "<li><code>[sample]</code> is the exact sample name as in Highlander.</li>"
					+ "</ul>"
					+ "Note that it can be symbolic links to the files.<br>"
					+ "This information is only used by some scripts server side, so this field can be left blank if you don't run them."
					+ "</html>";
			JLabel labelBamDir = new JLabel("Directory containing BAM on server");
			labelBamDir.setToolTipText(tooltipBamDir);
			panel.add(labelBamDir, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtBamDir = new JTextField(analysis.getBamDirectory());
			txtBamDir.setToolTipText(tooltipBamDir);
			panel.add(txtBamDir, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtBamDir.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					AnalysisFull analysis = listAnalyses.getSelectedValue();
					analysis.setBamDIR(inputTxtField.getText());
					try {
						analysis.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					return true;
				}
			});
			row++;
			
			String tooltipVcfDir = "<html>Full path of the directory containing VCF files of this analysis on the server.<br>"
					+ "The directory must contain all VCF with the exact name <code>[sample].[extension]</code>.<br>"
					+ "<ul>"
					+ "<li><code>[sample]</code> is the exact sample name as in Highlander.</li>"
					+ "<li><code>[extension]</code> is defined in the fields <i>File extension for VCF ...</i> (it can be different for SNVs and INDELs).</li>"
					+ "</ul>"
					+ "Note that it can be symbolic links to the files.<br>"
					+ "This information is only used by some scripts server side, so this field can be left blank if you don't run them."
					+ "</html>";
			JLabel labelVcfDir = new JLabel("Directory containing VCF on server");
			labelVcfDir.setToolTipText(tooltipVcfDir);
			panel.add(labelVcfDir, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtVcfDir = new JTextField(analysis.getVcfDirectory());
			txtVcfDir.setToolTipText(tooltipVcfDir);
			panel.add(txtVcfDir, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtVcfDir.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					AnalysisFull analysis = listAnalyses.getSelectedValue();
					analysis.setVcfDIR(inputTxtField.getText());
					try {
						analysis.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					return true;
				}
			});
			row++;
			
			panel.add(new JPanel(), new GridBagConstraints(0, row, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			
			scrollData.setViewportView(panel);
		}
	}

	public void createAnalysis(){
		Object resu = JOptionPane.showInputDialog(this, "Analysis name (alphanumeric caracters only and '_').\nNote that analysis name CANNOT be changed afterwards.", "Creating analysis", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String analysisStr = resu.toString();
			analysisStr = analysisStr.trim().replace(' ', '_').toLowerCase();
			Pattern pat = Pattern.compile("[^a-zA-Z0-9_]");
			if (pat.matcher(analysisStr).find()){
				JOptionPane.showMessageDialog(this, "Analysis name can only contain alphanumeric caracters and '_'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else if (manager.getAvailableAnalysesAsList().contains(new Analysis(analysisStr))){
				JOptionPane.showMessageDialog(this, "Analysis '"+analysisStr+"' already exists", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else{
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					Reference reference = (Reference)JOptionPane.showInputDialog(this, "Reference genome", "Creating analysis", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), Reference.getAvailableReferences().toArray(new Reference[0]), null);
					if (reference != null){
						Object target = JOptionPane.showInputDialog(this, "Sequencing target", "Creating analysis", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), manager.listSequencingTargets(), null);
						if (target != null){
							if (target.equals("Add new sequencing_target")){
								Object res = JOptionPane.showInputDialog(manager,  "Set a name for this new sequencing target", "New sequencing target",
										JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
								if (res != null) {
									target = res.toString().trim();
								}else {
									target = null;
								}
							}
							if (target != null){
								Object caller = JOptionPane.showInputDialog(this, "Variant caller type", "Creating analysis", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), VariantCaller.values(), VariantCaller.GATK);
								if (caller != null){
									FileDialog d = new FileDialog(manager, "Select an image file", FileDialog.LOAD);
									Tools.centerWindow(d, false);
									d.setVisible(true);
									if (d.getFile() != null){
										String filename = d.getDirectory() + d.getFile();
										File iconFile = new File(filename);
										ProjectManager.setHardUpdate(true);
										AnalysisFull analysis = new AnalysisFull(analysisStr, VariantCaller.valueOf(caller.toString()), reference, target.toString(),
												"http://url/bam/"+analysisStr,"http://url/vcf/"+analysisStr,"local_path/bam/"+analysisStr,"local_path/vcf/"+analysisStr,".vcf",".vcf");
										ProjectManager.toConsole("-----------------------------------------------------");
										ProjectManager.toConsole("Creating analysis and all supporting tables for " + analysis);
										analysis.insert(iconFile);
										manager.addAvailableAnalysis(analysis);
										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
												listAnalysesModel.addElement(analysis);
												listAnalyses.setSelectedValue(analysis, true);
												manager.refreshPanels();
											}
										});
									}
								}
							}
						}
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
				ProjectManager.setHardUpdate(false);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						waitingPanel.setVisible(false);
						waitingPanel.stop();
					}
				});
			}
		}		
	}

	public void setCoverageRegions(AnalysisFull analysis) {
		SetCoverageRegionsDialog dialog = new SetCoverageRegionsDialog();
		Tools.centerWindow(dialog, false);
		dialog.setVisible(true);
		if (dialog.validate){
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try {
				ProjectManager.toConsole("-----------------------------------------------------");
				ProjectManager.toConsole("Setting coverage regions for '"+analysis+"'");
				DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromCoverageRegions());
				ProjectManager.toConsole("Biotypes included: ");
				for (String biotype : biotypes.keySet()) {
					ProjectManager.toConsole("- " + biotype + "("+(biotypes.get(biotype)?"with":"without")+" UTR)");					
				}
				boolean cds = includeCDS.isSelected();
				ProjectManager.toConsole(((cds)?"Including":"Excluding")+" all CDS");
				boolean utr = includeUTR.isSelected();
				ProjectManager.toConsole(((utr)?"Including":"Excluding")+" all UTR");
				File output = null;
				if (generateBed.isSelected()) {
					String name = analysis.getReference().getName();
					name += (cds) ? ".coding" : ".noncoding";
					name += (utr) ? ".withUTR" : ".withoutUTR";
					name += ".nochr.bed";
					output = new File(name);
					ProjectManager.toConsole("Generating corresponding bed file " + output);
				}
				manager.startRedirectSystemOut();
				ExomeBed.generateBed(analysis.getReference(), false, biotypes, cds, utr, false, false, false, output, analysis);
				manager.stopRedirectSystemOut();
				ProjectManager.toConsole("Successfully done");
			}catch(Exception ex){
				ProjectManager.toConsole(ex);
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}
	}
	
	public void deleteAnalysis(AnalysisFull analysis){
		ProjectManager.setHardUpdate(true);
		int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to DEFINITIVELY delete analysis:\n"+analysis+" and ALL samples in it ?", "Delete analysis from Highlander", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dMinus,64));
		if (res == JOptionPane.CANCEL_OPTION){
			return;
		}else if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try {
				ProjectManager.toConsole("-----------------------------------------------------");
				ProjectManager.toConsole("Deleting analysis '"+analysis+"'");
				analysis.delete();
				manager.removeAvailableAnalysis(analysis);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						listAnalyses.clearSelection();
						listAnalyses.setSelectedIndex(0);
						listAnalysesModel.removeElement(analysis);
						manager.refreshPanels();
					}
				});
			}catch(Exception ex){
				ProjectManager.toConsole(ex);
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}
		ProjectManager.setHardUpdate(false);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				manager.refreshPanels();
			}
		});
	}
	
	private void reorderAnalysis(int index, boolean up) {
		if (up && index == 0) return;
		if (!up && index == listAnalysesModel.getSize()-1)  return;
		AnalysisFull analysis = listAnalysesModel.get(index);
		AnalysisFull other = listAnalysesModel.get((up)?index-1:index+1);
		if (up) {
			int newOrder = other.getOrdering();	
			other.setOrdering(analysis.getOrdering());
			analysis.setOrdering(newOrder);
		}else {
			int newOrder = analysis.getOrdering();
			analysis.setOrdering(other.getOrdering());
			other.setOrdering(newOrder);
		}
		try {
			analysis.update();
			other.update();
			listAnalysesModel.set(index, other);
			listAnalysesModel.set((up)?index-1:index+1, analysis);
			listAnalyses.setSelectedValue(analysis, true);
		}catch(Exception ex) {
			ProjectManager.toConsole(ex);
		}
	}
	
	public class SetCoverageRegionsDialog extends JDialog {
		public boolean validate = false;

		public SetCoverageRegionsDialog(){
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(getMainPanel(), BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int width = screenSize.width - (screenSize.width/3*2);
			int height = screenSize.height - (screenSize.height/3);
			setSize(new Dimension(width,height));
			setModal(true);
			setTitle("Set coverage regions for " + listAnalyses.getSelectedValue());
			setIconImage(Resources.getScaledIcon(Resources.iCoverage, 64).getImage());
			pack();
		}

		public JPanel getMainPanel(){
			JPanel panel = new JPanel(new BorderLayout());

			JPanel checkBoxesPanel = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
			generateBed = new JCheckBox("Generate a corresponding bed file");
			checkBoxesPanel.add(generateBed);
			includeCDS = new JCheckBox("Include coding sequences");
			includeCDS.setSelected(true);
			checkBoxesPanel.add(includeCDS);
			includeUTR = new JCheckBox("Include untranslated regions");
			checkBoxesPanel.add(includeUTR);
			panel.add(checkBoxesPanel, BorderLayout.NORTH);

			JPanel biotypesPanel = new JPanel(new GridBagLayout());
			int r=0;
			biotypes.clear();
			for (String biotype : Gene.biotypePriorities){
				final JCheckBox boxBiotype = new JCheckBox(biotype);
				final JCheckBox boxBiotypeUTR = new JCheckBox("include UTR");
				boxBiotypeUTR.setEnabled(false);
				boxBiotype.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (boxBiotype.isSelected()) {
							boxBiotypeUTR.setEnabled(true);
							biotypes.put(boxBiotype.getText(), boxBiotypeUTR.isSelected());
						}else {
							boxBiotypeUTR.setEnabled(false);
							biotypes.remove(boxBiotype.getText());							
						}
					}
				});
				boxBiotypeUTR.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						biotypes.put(boxBiotype.getText(), boxBiotypeUTR.isSelected());
					}
				});
				if (biotype.equals("protein_coding")) boxBiotype.setSelected(true);
				biotypesPanel.add(boxBiotype, new GridBagConstraints(0, r, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
				biotypesPanel.add(boxBiotypeUTR, new GridBagConstraints(1, r, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
				r++;
			}
			JScrollPane biotypesScrollPane = new JScrollPane(biotypesPanel);
			biotypesScrollPane.setBorder(BorderFactory.createTitledBorder("Select which Ensembl biotypes to include"));
			panel.add(biotypesScrollPane, BorderLayout.CENTER);

			JPanel validationPanel = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
			JButton importButton = new JButton(" Generate coverage regions ");
			importButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					validate = true;
					dispose();
				}
			});
			validationPanel.add(importButton);
			panel.add(validationPanel, BorderLayout.SOUTH);

			return panel;
		}

	}

}
