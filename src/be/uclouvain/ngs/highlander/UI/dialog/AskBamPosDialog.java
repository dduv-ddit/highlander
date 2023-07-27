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

package be.uclouvain.ngs.highlander.UI.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.border.Border;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;

public class AskBamPosDialog extends JDialog {

	private static String ALL_BAM = "All BAM available";
	private static String SELECTION = "User selection";
	private static String PROFILE = "List from your profile";

	private Reference reference;
	private Map<AnalysisFull, Boolean> selectedAnalysis = new TreeMap<AnalysisFull, Boolean>();
	private Map<AnalysisFull, Set<String>> selectedBAM = new TreeMap<AnalysisFull, Set<String>>();
	private Set<Interval> selectedPositions = new TreeSet<Interval>();

	private final JTextArea text_intervals_intervals = new JTextArea();

	public AskBamPosDialog(Highlander mainFrame, Reference reference){
		this.reference = reference;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		try {
			selectedPositions = Interval.fetchIntervals(mainFrame.getVariantTable().getSelectedVariantsId());
			for (Interval pos : selectedPositions){
				text_intervals_intervals.append(((text_intervals_intervals.getText().length() > 0)?";":"")+pos.toString());	
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive position for selected variant", ex), "BAM and positions selection",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		pack();
	}

	private void initUI(){
		setModal(true);
		setTitle("BAM and positions selection");
		setIconImage(Resources.getScaledIcon(Resources.iBamViewer, 64).getImage());

		getContentPane().setLayout(new BorderLayout());

		JPanel bamPanel = new JPanel(new GridLayout());
		for (AnalysisFull analysis : Highlander.getAvailableAnalyses()){
			String ensAn = analysis.getReference().getName();
			String ensIn = reference.getName();
			try {
				ensAn = analysis.getReference().getSchemaName(Schema.ENSEMBL);
				ensIn = reference.getSchemaName(Schema.ENSEMBL);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			if (ensAn.equals(ensIn)) {
				final AnalysisFull fanalysis = analysis;
				selectedAnalysis.put(analysis, false);
				selectBAM(ALL_BAM, fanalysis);
				JPanel analysisPanel = new JPanel(new GridBagLayout());
				final JLabel label = new JLabel();
				final JCheckBox analCheck = new JCheckBox(analysis.toString());
				analCheck.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent arg0) {
						selectedAnalysis.put(fanalysis, analCheck.isSelected());
						label.setText(((selectedBAM.containsKey(fanalysis) && analCheck.isSelected())?selectedBAM.get(fanalysis).size():0) + " BAM selected");
					}
				});
				analysisPanel.add(analCheck, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
				if (analysis.equals(Highlander.getCurrentAnalysis())) {
					analCheck.setSelected(true);
					selectedAnalysis.put(analysis, true);
				}
				JComboBox<String> selectionBox = new JComboBox<>(new String[]{ALL_BAM,SELECTION,PROFILE});
				label.setText(((selectedBAM.containsKey(fanalysis) && analCheck.isSelected())?selectedBAM.get(fanalysis).size():0) + " BAM selected");
				selectionBox.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED){
							selectBAM(((JComboBox<?>)e.getSource()).getSelectedItem().toString(), fanalysis);
							if (!analCheck.isSelected()) analCheck.setSelected(true);
							label.setText(((selectedBAM.containsKey(fanalysis) && analCheck.isSelected())?selectedBAM.get(fanalysis).size():0) + " BAM selected");
							AskBamPosDialog.this.validate();
							AskBamPosDialog.this.repaint();
						}
					}
				});
				analysisPanel.add(selectionBox, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
				analysisPanel.add(label, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
				bamPanel.add(analysisPanel);
			}
		}
		getContentPane().add(bamPanel, BorderLayout.NORTH);

		JPanel panelIntervals = new JPanel(new BorderLayout(5,5));

		text_intervals_intervals.setRows(5);
		text_intervals_intervals.setToolTipText("Enter intervals in the format chr:pos (SNV) or chr:start-stop (INDEL), values must be separated by ';' or be on different lines. e.g. 15:1562448;X:2450122-2450127");
		text_intervals_intervals.setLineWrap(true);
		text_intervals_intervals.setWrapStyleWord(true);

		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Positions to view");
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);

		JScrollPane scrollPane = new JScrollPane(text_intervals_intervals);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setViewportView(text_intervals_intervals);
		scrollPane.setBorder(compoundBorder);
		panelIntervals.add(scrollPane, BorderLayout.CENTER);

		JPanel panel_add = new JPanel(new GridBagLayout());
		panelIntervals.add(panel_add, BorderLayout.SOUTH);

		JButton button_import_INDEL = new JButton("Import intervals from profile or file", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_import_INDEL.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				AskListOfIntervalsDialog apd = new AskListOfIntervalsDialog(reference);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				for (Interval interval : apd.getSelection()){
					text_intervals_intervals.append(((text_intervals_intervals.getText().length() > 0)?";":"")+interval);					
				}
				pack();
			}
		});
		GridBagConstraints gbc_button_import_INDEL = new GridBagConstraints();
		gbc_button_import_INDEL.fill = GridBagConstraints.HORIZONTAL;
		gbc_button_import_INDEL.weighty = 0.0;
		gbc_button_import_INDEL.weightx = 1.0;
		gbc_button_import_INDEL.insets = new Insets(5, 5, 5, 5);
		gbc_button_import_INDEL.gridx = 1;
		gbc_button_import_INDEL.gridy = 0;
		gbc_button_import_INDEL.anchor = GridBagConstraints.NORTHWEST;
		panel_add.add(button_import_INDEL, gbc_button_import_INDEL);

		JButton button_import_SNP = new JButton("Import intervals from profile or file as single positions", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_import_SNP.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				AskListOfIntervalsDialog apd = new AskListOfIntervalsDialog(reference);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				for (Interval interval : apd.getSelection()){
					for (int pos = interval.getStart() ; pos <= interval.getEnd() ; pos++){
						text_intervals_intervals.append(((text_intervals_intervals.getText().length() > 0)?";":"")+interval.getChromosome()+":"+pos);
					}
				}
				pack();
			}
		});
		GridBagConstraints gbc_button_import_SNP = new GridBagConstraints();
		gbc_button_import_SNP.fill = GridBagConstraints.HORIZONTAL;
		gbc_button_import_SNP.weighty = 0.0;
		gbc_button_import_SNP.weightx = 1.0;
		gbc_button_import_SNP.insets = new Insets(5, 5, 5, 5);
		gbc_button_import_SNP.gridx = 0;
		gbc_button_import_SNP.gridy = 0;
		gbc_button_import_SNP.anchor = GridBagConstraints.NORTHWEST;
		panel_add.add(button_import_SNP, gbc_button_import_SNP);

		getContentPane().add(panelIntervals, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				dispose();
			}
		});
		buttonPanel.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				cancelClose();
			}
		});
		buttonPanel.add(btnCancel);


		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
	}

	private void selectBAM(String option, AnalysisFull analysis){
		if (option.equals(ALL_BAM)){
			try{
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `value` FROM "+analysis.getFromPossibleValues()+" WHERE `field` = 'sample'")) {
					Set<String> set = new TreeSet<String>();
					while (res.next()){
						set.add(res.getString(1));
					}
					selectedBAM.put(analysis, set);
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}else if (option.equals(PROFILE)){
			String listname = ProfileTree.showProfileDialog(this, Action.LOAD, UserData.VALUES, "sample");
			if (listname != null){
				try{
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
							"SELECT `value` FROM "+analysis.getFromPossibleValues()+" " +
									"WHERE `field` = 'sample' AND `value` IN ("+HighlanderDatabase.makeSqlList(Highlander.getLoggedUser().loadValues(Field.sample, listname), String.class)+")")){
						Set<String> set = new TreeSet<String>();
						while (res.next()){
							set.add(res.getString(1));
						}
						selectedBAM.put(analysis, set);
					}
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive all BAM from your list", ex), "BAM selection",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		}else{
			AskSamplesDialog ask = new AskSamplesDialog(false, analysis);
			Tools.centerWindow(ask, false);
			ask.setVisible(true);
			if (ask.getSelection() != null) selectedBAM.put(analysis, ask.getSelection());
		}
	}

	public Set<Interval> getSelectedPositions(){
		String[] input = text_intervals_intervals.getText().replace('\n', ';').split(";");
		for (String string : input){
			if (string.length() > 0){
				try{
					selectedPositions.add(new Interval(reference, string));
				}catch(NumberFormatException ex){
					System.err.println("Problem with input "+string+" (you must give a numeric positive value for start and end positions)");
				}
			}
		}
		return selectedPositions;
	}

	public Map<AnalysisFull, Set<String>> getSelectedBAMs(){
		Map<AnalysisFull, Set<String>> result = new TreeMap<AnalysisFull, Set<String>>();
		for (AnalysisFull analysis : selectedBAM.keySet()){
			if (selectedAnalysis.get(analysis)){
				result.put(analysis, selectedBAM.get(analysis));
			}else{
				result.put(analysis, new TreeSet<String>());
			}
		}
		return result;
	}

	//Overridden so we can exit when window is closed
	@Override
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancelClose();
		}
	}

	private void cancelClose(){
		selectedPositions.clear();
		selectedBAM.clear();
		dispose();
	}

}
