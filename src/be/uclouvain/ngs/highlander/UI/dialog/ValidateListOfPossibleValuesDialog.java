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

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;

import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;



public class ValidateListOfPossibleValuesDialog extends JDialog {
	private List<String> finalSelection = new ArrayList<String>();
	private List<String> finalSelectionComments = new ArrayList<String>();
	private JButton btnOk;
	private final AnalysisFull analysis;
	private final Field field;
	private final String[] input;
	private final String[] inputComments;
	private final boolean[] valid;
	private String[][] output;
	private JLabel[] selection;

	private JScrollPane scrollPane;
	static private WaitingPanel waitingPanel;

	public ValidateListOfPossibleValuesDialog(final List<String> values, final List<String> comments, AnalysisFull analysis, Field field) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		this.analysis = analysis;
		this.field = field;
		input = values.toArray(new String[0]);
		inputComments = comments.toArray(new String[0]);
		valid = new boolean[values.size()];
		output = new String[values.size()][];
		selection = new JLabel[values.size()];
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						validateInput(values);
					}
				}, "ValidateListOfPossibleValuesDialog.validateInput").start();
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
	}

	private void initUI(){
		setModal(true);
		setTitle("Validate value list");
		setIconImage(Resources.getScaledIcon(Resources.iList, 64).getImage());

		JPanel panel_2 = new JPanel();
		getContentPane().add(panel_2, BorderLayout.NORTH);

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.setText("0 values");
		btnOk.setToolTipText("Replace your input by the corrected list");
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//Set<String> set = new LinkedHashSet<>();
				//Set<String> comments = new LinkedHashSet<>();
				finalSelection.clear();
				finalSelectionComments.clear();
				for (int i=0 ; i < selection.length ; i++) {
					JLabel l = selection[i];
					if (l.getText().length() > 0) {
						finalSelection.add(l.getText());
						finalSelectionComments.add(inputComments[i]);
					}
				}
				//finalSelection.addAll(set);
				//finalSelectionComments.addAll(comments);
				dispose();
			}
		});
		panel.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.setToolTipText("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				cancelClose();
			}
		});
		panel.add(btnCancel);

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);

		GridBagLayout gbl_panel_1 = new GridBagLayout();
		panel_1.setLayout(gbl_panel_1);

		scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.weighty = 1.0;
		gbc_scrollPane.weightx = 1.0;
		gbc_scrollPane.insets = new Insets(5, 5, 0, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		panel_1.add(scrollPane, gbc_scrollPane);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	private JPanel getValidationPanel() {
		GridBagLayout gbl = new GridBagLayout();
		JPanel panel = new JPanel(gbl);
		panel.add(new JLabel(""), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 20, 2, 15), 0, 0));
		JLabel labelInput = new JLabel("Input");
		labelInput.setFont(new JLabel("").getFont().deriveFont(Font.BOLD));
		panel.add(labelInput, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 15), 0, 0));
		JLabel labelOutput = new JLabel("Corrected list");
		labelOutput.setFont(new JLabel("").getFont().deriveFont(Font.BOLD));
		panel.add(labelOutput, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 15), 0, 0));
		JLabel labelSuggestions = new JLabel("Suggestions");
		labelSuggestions.setFont(new JLabel("").getFont().deriveFont(Font.BOLD));
		panel.add(labelSuggestions, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 15), 0, 0));
		JLabel labelModifications = new JLabel("Modifications");
		labelModifications.setFont(new JLabel("").getFont().deriveFont(Font.BOLD));
		panel.add(labelModifications, new GridBagConstraints(4, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 15), 0, 0));
		for (int y=0 ; y < input.length ; y++) {
			JLabel labelValidation = new JLabel();
			if (valid[y]) {
				labelValidation.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			}else{
				labelValidation.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
				labelValidation.setToolTipText("'" + input[y] + "' was not found in the database as a value for the field '"+field+"'");
			}

			panel.add(labelValidation, new GridBagConstraints(0, y+1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 20, 2, 5), 0, 0));
			panel.add(new JLabel(input[y]), new GridBagConstraints(1, y+1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 15), 0, 0));
			selection[y] = new JLabel("");
			if (output[y].length > 0 && output[y][0] != null) selection[y].setText(output[y][0]);
			panel.add(selection[y], new GridBagConstraints(2, y+1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 15), 0, 0));
			if (!valid[y]) {
				final int currentY = y; 
				final JComboBox<String> box = new JComboBox<>(output[y]);
				box.setToolTipText("Closest suggestions from the database");
				box.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									selection[currentY].setText(box.getSelectedItem().toString());		
									btnOk.setText(getValuesCount()+" values");
								}
							});
						}
					}
				});
				panel.add(box, new GridBagConstraints(3, y+1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 15), 0, 0));
				JButton buttonSearch = new JButton(Resources.getScaledIcon(Resources.iDbSearch, 18));
				buttonSearch.setToolTipText("Select the value in the database");
				buttonSearch.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						AskListOfPossibleValuesDialog pos = new AskListOfPossibleValuesDialog(field, null, true);
						Tools.centerWindow(pos, false);
						pos.setVisible(true);
						if (!pos.getSelection().isEmpty()) {
							selection[currentY].setText(pos.getSelection().iterator().next());
							btnOk.setText(getValuesCount()+" values");
						}
					}
				});
				panel.add(buttonSearch, new GridBagConstraints(4, y+1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 2, 5), 0, 0));
				JButton buttonDelete = new JButton(Resources.getScaledIcon(Resources.i3dMinus, 18));
				buttonDelete.setToolTipText("Remove this value from the list");
				buttonDelete.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						selection[currentY].setText("");
						btnOk.setText(getValuesCount()+" values");
					}
				});
				panel.add(buttonDelete, new GridBagConstraints(5, y+1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 2, 5), 0, 0));
				JButton buttonKeepOriginal = new JButton(Resources.getScaledIcon(Resources.iButtonAutoApply, 18));
				buttonKeepOriginal.setToolTipText("Keep the original value");
				buttonKeepOriginal.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						selection[currentY].setText(input[currentY]);
						btnOk.setText(getValuesCount()+" values");
					}
				});
				panel.add(buttonKeepOriginal, new GridBagConstraints(6, y+1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 2, 5), 0, 0));
			}else {
				panel.add(new JPanel(), new GridBagConstraints(3, y+1, 1, 4, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
			}
			panel.add(new JPanel(), new GridBagConstraints(7, y+1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 10, 2, 5), 0, 0));
		}
		btnOk.setText(getValuesCount()+" values");
		panel.add(new JPanel(), new GridBagConstraints(0, input.length+1, 7, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 10, 2, 5), 0, 0));
		return panel;
	}

	private void validateInput(List<String> values) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			Set<String> found = new HashSet<>();
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `value` FROM " + analysis.getFromPossibleValues() + " WHERE `field` = '" + field.getName() + "' AND `value` IN ("+HighlanderDatabase.makeSqlList(values, String.class)+")")) {
				while (res.next()) {
					found.add(res.getString(1).toUpperCase());
				}
			}
			for (int i=0 ; i < input.length ; i++) {
				if (found.contains(input[i].toUpperCase())) {
					valid[i] = true;
					output[i] = new String[] {input[i]};
				}else {
					valid[i] = false;
					output[i] = findAlternative(input[i]);
				}
			}
		}catch(Exception ex) {
			Tools.exception(ex);
		}
		scrollPane.setViewportView(getValidationPanel());
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	public String[] findAlternative(String value) throws Exception {
		if (field.getName().equals("gene_symbol")) {
			return new String[] {DBUtils.getGeneSymbol(analysis.getReference(), DBUtils.getEnsemblGene(analysis.getReference(), value))};
		}else {
			return DBUtils.getSimilarPossibleValues(analysis, field, value, 8);
		}
	}

	public List<String> getSelection(){
		return finalSelection;
	}

	public List<String> getSelectionComments(){
		return finalSelectionComments;
	}
	
	private int getValuesCount(){
		Set<String> set = new LinkedHashSet<>();
		for (JLabel l : selection) {
			if (l.getText().length() > 0) {
				set.add(l.getText());
			}
		}
		return set.size();
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
		finalSelection.clear();
		finalSelectionComments.clear();
		dispose();
	}

}
