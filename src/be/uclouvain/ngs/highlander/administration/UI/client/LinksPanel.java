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

package be.uclouvain.ngs.highlander.administration.UI.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.datatype.ExternalLink;
import be.uclouvain.ngs.highlander.datatype.Reference;

/**
* @author Raphael Helaers
*/

public class LinksPanel extends ManagerPanel {
	
	private List<ExternalLink> availableLinks = new ArrayList<>();
	private ExternalLink selectedLink = null;
	private int selectedIndex = -1;
	private JScrollPane scrollButtons = new JScrollPane();
	private JScrollPane scrollData = new JScrollPane();

	private final String tooltipImage = "<html>The image shown on the button in Highlander.<br>"
			+ "Click to select a new image from a file.<br>"
			+ "Image size doesn't matter, it will be rescaled.<br>"
			+ "</html>";
	private final String tooltipName = "<html>The name of the external resource.<br>"
			+ "If you don't provide an image, the name is displayed on the button in Highlander.<br>"
			+ "</html>";
	private final String tooltipDescription = "<html>A more detailled description of the external resource.<br>"
			+ "It will be displayed on the tooltip of the button in Highlander.<br>"
			+ "</html>";
	private final String tooltipURL = "<html>The URL to access the external resource.<br>"
			+ "If you URL has parameters that must be encoded (<i>e.g.</i> ':' -> '%3A'), use the URL parameters field.<br>"
			+ "You can use Highlander fields (column exact name) in brackets within URL and URL parameters, they will be replaced when a variant is selected.<br>"
			+ "Some examples:<br>"
			+ "<ul>"
			+ "<li><code>http://marrvel.org/search/variant/[chr]:[pos]%20[reference]%3E[alternative]</code><br>"
			+ "<code>[chr]</code>, <code>[pos]</code>, <code>[reference]</code> and <code>[alternative]</code> will be replaced by the value of the variant selected in Highlander.</li>"
			+ "<li><code>http://www.ncbi.nlm.nih.gov/omim/?term=[gene_symbol]</code><br>"
			+ "<code>[gene_symbol]</code> will be replaced by the gene spanning the variant selected in Highlander.</li>"
			+ "<li><code>http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=[dbsnp_id:2]</code><br>"
			+ "<code>[dbsnp_id:2]</code> will be replaced by the dbSNP identifier <i>minus the 2 first characters</i>.<br>"
			+ "Indeed in Highlander dbSNP ids are in the form 'rs452678', and the URL only needs '452678'.</li>"
			+ "<li>You can use the special fields <code>[chr_grch37]</code> and <code>[pos_grch37]</code>, <code>[chr_grch38]</code> and <code>[pos_grch38]</code> when a position needs to be converted to a specific genome version.<br>"
			+ "<i>e.g.</i> MutationTaster is only available for GRCh37, so using <code>[chr_grch37]</code> and <code>[pos_grch37]</code> instead of <code>[chr]</code> and <code>[pos]</code> will make sure that is always the case (converting e.g. from GRCh38 if necessary).</li>"
			+ "<li>You can use the special field <code>[protein_change]</code> if the amino acid change is needed.<br>"
			+ "<i>e.g.</i> the HGVS notation of a variant <code>p.Arg292Leu</code> will give <code>R292L</code> with <code>[protein_change]</code>.</li>"
			+ "<li>You can use the special field <code>[genome]</code> to get the reference linked to the analysis.<br>"
			+ "<i>e.g.</i> <code>http://genome.ucsc.edu/cgi-bin/hgTracks?org=human&db=[genome]&position=chr[chr]:[pos]</code><br>"
			+ "<code>[genome]</code> must be replaced by hg19 (GRCh37-like refs) or hg38 (GRCh38-like refs) or mm10 (GRCm38-like refs).<br>"
			+ "You will have to define a replacement for each of your references in the interface.</li>"
			+ "<li>You can use the special field <code>[analysis]</code> to get the Highlander analysis where the variant comes from.<br>"
			+ "</ul>"
			+ "</html>";
	private final String tooltipURLParameters = "<html>Part of the URL that needs encoding (<i>e.g.</i> ':' -> '%3A').<br>"
			+ "Part of the URL needing encoding depends on the web site, see other existing URLs for examples.<br>"
			+ "You can use Highlander fields (column exact name) in brackets within URL and URL parameters, they will be replaced when a variant is selected.<br>"
			+ "Some examples:<br>"
			+ "<ul>"
			+ "<li><code>http://marrvel.org/search/variant/[chr]:[pos]%20[reference]%3E[alternative]</code><br>"
			+ "<code>[chr]</code>, <code>[pos]</code>, <code>[reference]</code> and <code>[alternative]</code> will be replaced by the value of the variant selected in Highlander.</li>"
			+ "<li><code>http://www.ncbi.nlm.nih.gov/omim/?term=[gene_symbol]</code><br>"
			+ "<code>[gene_symbol]</code> will be replaced by the gene spanning the variant selected in Highlander.</li>"
			+ "<li><code>http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=[dbsnp_id:2]</code><br>"
			+ "<code>[dbsnp_id:2]</code> will be replaced by the dbSNP identifier <i>minus the 2 first characters</i>.<br>"
			+ "Indeed in Highlander dbSNP ids are in the form 'rs452678', and the URL only needs '452678'.</li>"
			+ "<li>You can use the special fields <code>[chr_grch37]</code> and <code>[pos_grch37]</code>, <code>[chr_grch38]</code> and <code>[pos_grch38]</code> when a position needs to be converted to a specific genome version.<br>"
			+ "<i>e.g.</i> MutationTaster is only available for GRCh37, so using <code>[chr_grch37]</code> and <code>[pos_grch37]</code> instead of <code>[chr]</code> and <code>[pos]</code> will make sure that is always the case (converting e.g. from GRCh38 if necessary).</li>"
			+ "<li>You can use the special field <code>[protein_change]</code> if the amino acid change is needed.<br>"
			+ "<i>e.g.</i> the HGVS notation of a variant <code>p.Arg292Leu</code> will give <code>R292L</code> with <code>[protein_change]</code>.</li>"
			+ "<li>You can use the special field <code>[genome]</code> to get the reference linked to the analysis.<br>"
			+ "<i>e.g.</i> <code>http://genome.ucsc.edu/cgi-bin/hgTracks?org=human&db=[genome]&position=chr[chr]:[pos]</code><br>"
			+ "<code>[genome]</code> must be replaced by hg19 (GRCh37-like refs) or hg38 (GRCh38-like refs) or mm10 (GRCm38-like refs).<br>"
			+ "You will have to define a replacement for each of your references in the interface.</li>"
			+ "<li>You can use the special field <code>[analysis]</code> to get the Highlander analysis where the variant comes from.<br>"
			+ "</ul>"
			+ "</html>";
	private final String tooltipGenome = "<html>If you use the [genome] special field in URL, it will be replaced by this value for variants aligned on this reference.<br>"
			+ "Leave it blank if this reference isn't supported by the web ressource you link.<br>"
			+ "</html>";
	
	public LinksPanel(ProjectManager manager){
		super(manager);

		try {
			availableLinks = ExternalLink.getAvailableExternalLinks();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		fill(availableLinks.get(0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		add(splitPane, BorderLayout.CENTER);

		JPanel panel_left = new JPanel(new BorderLayout(5,5));
		panel_left.setBorder(BorderFactory.createTitledBorder("External links"));
		splitPane.setLeftComponent(panel_left);

		scrollButtons = new JScrollPane();
		panel_left.add(scrollButtons, BorderLayout.CENTER);

		fillButtons();
		
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
			public void actionPerformed(ActionEvent arg0) {
				reorderLinks(selectedIndex, true);
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
			public void actionPerformed(ActionEvent arg0) {
				reorderLinks(selectedIndex, false);
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
		scrollData.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel panel_south = new JPanel();
		add(panel_south, BorderLayout.SOUTH);
		
		JButton createNewButton = new JButton("Add new link", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createExternalLink();
					}
				}, "LinksPanel.createExternalLink").start();

			}
		});
		panel_south.add(createNewButton);

		JButton deleteButton = new JButton("Delete link", Resources.getScaledIcon(Resources.iCross, 16));
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteExternalLink(selectedLink);
					}
				}, "LinksPanel.deleteExternalLink").start();

			}
		});
		panel_south.add(deleteButton);

	}
	
	private void fillButtons() {
		scrollButtons.setViewportView(null);
		JPanel buttonPanel = new JPanel(new WrapLayout(WrapLayout.LEADING));
		for (ExternalLink link : availableLinks) {
			JButton button = link.getButton();
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					fill(link);
				}
			});
			buttonPanel.add(button);
		}
		scrollButtons.setViewportView(buttonPanel);
	}
	
	private void fill(ExternalLink link) {
		scrollData.setViewportView(null);
		if (link != null) {
			selectedLink = link;
			for (int i=0 ; i < availableLinks.size() ; i++) {
				if (availableLinks.get(i).equals(link)) {
					selectedIndex = i;					
				}
			}
			JPanel panel = new JPanel(new GridBagLayout());
			int row = 0;
			
			JButton buttonIcon = new JButton();
			if (link.getIcon() != null) {
				buttonIcon.setIcon(link.getScaledIcon());
			}else {
				buttonIcon.setIcon(Resources.getScaledIcon(Resources.i2dPlus, 40));				
			}
			buttonIcon.setToolTipText(tooltipImage);
			panel.add(buttonIcon, new GridBagConstraints(0, row, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			buttonIcon.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							FileDialog d = new FileDialog(manager, tooltipImage, FileDialog.LOAD);
							Tools.centerWindow(d, false);
							d.setVisible(true);
							if (d.getFile() != null){
								String filename = d.getDirectory() + d.getFile();
								File file = new File(filename);
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										waitingPanel.setVisible(true);
										waitingPanel.start();
									}
								});
								try{
									link.updateIcon(file);
									buttonIcon.setIcon(link.getScaledIcon());
									fillButtons();
								}catch(Exception ex){
									ProjectManager.toConsole(ex);			
								}
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										waitingPanel.setVisible(false);
										waitingPanel.stop();
									}
								});
							}
						}
					}, "LinksPanel.setIcon").start();
				}
			});
			row++;
			
			JButton buttonRemoveIcon = new JButton("Remove image");
			buttonRemoveIcon.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(true);
									waitingPanel.start();
								}
							});
							try{
								link.removeIcon();
								buttonIcon.setIcon(Resources.getScaledIcon(Resources.i2dPlus, 40));
								fillButtons();
							}catch(Exception ex){
								ProjectManager.toConsole(ex);			
							}
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(false);
									waitingPanel.stop();
								}
							});
						}
					}, "LinksPanel.removeIcon").start();
				}
			});
			panel.add(buttonRemoveIcon, new GridBagConstraints(0, row, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			row++;
			
			JLabel labelName = new JLabel("Name");
			labelName.setToolTipText(tooltipName);
			panel.add(labelName, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtName = new JTextField(link.getName());
			txtName.setToolTipText(tooltipName);
			panel.add(txtName, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtName.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					link.setName(inputTxtField.getText());
					try {
						link.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					fillButtons();
					return true;
				}
			});
			row++;
			
			JLabel labelDescription = new JLabel("Description");
			labelDescription.setToolTipText(tooltipDescription);
			panel.add(labelDescription, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtDescription = new JTextField(link.getDescription());
			txtDescription.setToolTipText(tooltipDescription);
			panel.add(txtDescription, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtDescription.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					link.setDescription(inputTxtField.getText());
					try {
						link.update();
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
			
			JLabel labelURL = new JLabel("URL");
			labelURL.setToolTipText(tooltipURL);
			panel.add(labelURL, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtURL = new JTextField(link.getURL());
			txtURL.setToolTipText(tooltipURL);
			panel.add(txtURL, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtURL.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					link.setURL(inputTxtField.getText());
					try {
						link.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					if (link.getURL().contains("[genome]")) {
						fill(link);
					}
					return true;
				}
			});
			row++;
			
			JLabel labelURLParameters = new JLabel("URL parameters");
			labelURLParameters.setToolTipText(tooltipURLParameters);
			panel.add(labelURLParameters, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtURLParameters = new JTextField(link.getURLParameters());
			txtURLParameters.setToolTipText(tooltipURLParameters);
			panel.add(txtURLParameters, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtURLParameters.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					link.setURLParameters(inputTxtField.getText());
					try {
						link.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
						inputTxtField.setForeground(Color.RED);
						return false;
					}
					inputTxtField.setForeground(Color.BLACK);
					if (link.getURLParameters().contains("[genome]")) {
						fill(link);
					}
					return true;
				}
			});
			row++;
			
			if (link.getURL().contains("[genome]") || link.getURLParameters().contains("[genome]")) {
				JPanel panel_genomes = new JPanel(new GridBagLayout());
				panel_genomes.setBorder(BorderFactory.createTitledBorder("[genome] replacement per reference"));
				int r=0;
				for (Reference reference : Reference.getAvailableReferences()) {
					JLabel labelReference = new JLabel(reference.getName());
					labelReference.setToolTipText(tooltipGenome);
					panel_genomes.add(labelReference, new GridBagConstraints(0, r, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
					JTextField txtGenome = new JTextField(link.getReferenceNameInURL(reference));
					txtGenome.setToolTipText(tooltipGenome);
					panel_genomes.add(txtGenome, new GridBagConstraints(1, r, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
					txtGenome.setInputVerifier(new InputVerifier() {
						@Override
						public boolean verify(JComponent input) {
							JTextField inputTxtField = (JTextField)input;
							link.addReference(reference, inputTxtField.getText());
							try {
								link.update();
							}catch(Exception ex) {
								ProjectManager.toConsole(ex);
								inputTxtField.setForeground(Color.RED);
								return false;
							}
							inputTxtField.setForeground(Color.BLACK);
							return true;
						}
					});
					r++;
				}
				panel.add(panel_genomes, new GridBagConstraints(0, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
				row++;
			}
			
			JCheckBox cbox_enable = new JCheckBox("Display in Highlander");
			cbox_enable.setSelected(link.isEnable());
			panel.add(cbox_enable, new GridBagConstraints(0, row, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			cbox_enable.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					link.setEnable(cbox_enable.isSelected());
					try {
						link.update();
						fillButtons();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
					}
				}
			});
			row++;
			
			panel.add(new JPanel(), new GridBagConstraints(0, row, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			
			scrollData.setViewportView(panel);

		}
	}

	private void reorderLinks(int index, boolean up) {
		if (up && index == 0) return;
		if (!up && index == availableLinks.size()-1)  return;
		ExternalLink link = availableLinks.get(index);
		ExternalLink other = availableLinks.get((up)?index-1:index+1);
		if (up) {
			int newOrder = other.getOrdering();	
			other.setOrdering(link.getOrdering());
			link.setOrdering(newOrder);
		}else {
			int newOrder = link.getOrdering();
			link.setOrdering(other.getOrdering());
			other.setOrdering(newOrder);
		}
		try {
			link.update();
			other.update();
			availableLinks.set(index, other);
			availableLinks.set((up)?index-1:index+1, link);
			selectedIndex = (up)?index-1:index+1;
			fillButtons();
		}catch(Exception ex) {
			ProjectManager.toConsole(ex);
		}
	}

	public void createExternalLink(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		Object resu = JOptionPane.showInputDialog(this, tooltipName, "Add external link", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String name = resu.toString();
			if (name.length() > 255){
				JOptionPane.showMessageDialog(this, "Link name is limited to 255 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else{
				resu = JOptionPane.showInputDialog(this, tooltipDescription, "Add external link", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
				if (resu != null){
					String description = resu.toString();
					resu = JOptionPane.showInputDialog(this, tooltipURL, "Add external link", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
					if (resu != null){
						String url = resu.toString();
						if (url.length() > 1500){
							JOptionPane.showMessageDialog(this, "Link URL is limited to 1500 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
						}else{
							resu = JOptionPane.showInputDialog(this, tooltipURLParameters, "Add external link", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
							if (resu != null){
								String url_parameters = resu.toString();
								if (url_parameters.length() > 1500){
									JOptionPane.showMessageDialog(this, "Link URL parameter is limited to 1500 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
								}else{
									try{
										ProjectManager.toConsole("-----------------------------------------------------");
										ProjectManager.toConsole("Creating external link for " + name);
										ExternalLink link = new ExternalLink(name, description, url, url_parameters);
										link.insert();
										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
												availableLinks.add(link);
												fillButtons();
												fill(link);
											}
										});
									}catch(Exception ex){
										ProjectManager.toConsole(ex);
									}
								}
							}
						}
					}
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

	public void deleteExternalLink(ExternalLink link){
		if (link != null) {
			try{
				int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to delete the link to '"+link.getName()+"' ?", "Delete software links", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				if (res == JOptionPane.YES_OPTION){
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Deleting external link of " + link.getName());
					link.delete();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							availableLinks.remove(link);
							fillButtons();
							fill(availableLinks.get(0));
						}
					});
				}
			}catch(Exception ex){
				ProjectManager.toConsole(ex);
			}
		}
	}

}
