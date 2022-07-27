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
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class AskTumorNormalAssociation extends JDialog {

	private Analysis analysis;
	private Set<String> tumorSamples;
	private Map<String, String> tumorToNormal = new TreeMap<>();
	private String[] availableSamples;
	private Map<String, JComboBox<String>> boxes = new HashMap<>();

	public AskTumorNormalAssociation(Analysis analysis, Set<String> tumorSamples) {
		this.tumorSamples = tumorSamples;
		this.analysis = analysis;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		try{
			List<String> listap = new ArrayList<String>(); 
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `value` FROM "+analysis.getFromPossibleValues()+" WHERE `field` = 'sample' ORDER BY `value`")) {
				while (res.next()){
					listap.add(res.getString(1));
				}
			}
			availableSamples = listap.toArray(new String[0]);
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Can't retreive sample list", ex), "Fetching sample list",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		initUI();
	}

	private void initUI() {
		setModal(true);
		setTitle("Associate 'tumor' to 'normal' samples");
		setIconImage(Resources.getScaledIcon(Resources.iPatients, 64).getImage());

		JPanel south = new JPanel();	
		getContentPane().add(south, BorderLayout.SOUTH);

		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				tumorToNormal.clear();
				for (String tumor : boxes.keySet()) {
					if (boxes.get(tumor).getSelectedIndex() > -1) {
						tumorToNormal.put(tumor, boxes.get(tumor).getSelectedItem().toString());
					}
				}
				if (tumorToNormal.size() == tumorSamples.size()) {
					dispose();
				}else {
					JOptionPane.showMessageDialog(new JFrame(), "You must associate each sample", "Associate 'tumor' to 'normal' samples", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iPatients, 64));
				}
			}
		});
		south.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cancelClose();
			}
		});
		south.add(btnCancel);

		JPanel center = new JPanel(new GridBagLayout());
		JScrollPane scroll = new JScrollPane(center);
		getContentPane().add(scroll, BorderLayout.CENTER);

		int y=0;
		for (final String sample : tumorSamples) {
			center.add(new JLabel(sample), new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,10,5,5), 0, 0));
			final JComboBox<String> box = new JComboBox<String>(availableSamples);
			boxes.put(sample, box);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {	
					AutoCompleteSupport<String> support = AutoCompleteSupport.install(box, GlazedLists.eventListOf(availableSamples));
					support.setCorrectsCase(true);
					support.setFilterMode(TextMatcherEditor.CONTAINS);
					support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
					support.setStrict(false);
					box.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if (e.getActionCommand().equals("comboBoxEdited")){
								JComboBox<?> sampleBox = (JComboBox<?>)e.getSource();
								if (sampleBox.getSelectedIndex() < 0) {
									sampleBox.setSelectedItem(null);
								}
							}
						}
					});
				}});
			center.add(box, new GridBagConstraints(1, y, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5,5,5,10), 0, 0));
			y++;
		}
		center.add(new JPanel(), new GridBagConstraints(0, y, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));

		for (final String tumor : tumorSamples) {
			try {
				String fromdb = null;
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT p2.sample "
						+ "FROM projects as p "
						+ "LEFT JOIN projects as p2 ON p.normal_id = p2.project_id "
						+ "JOIN projects_analyses as pa ON p.project_id = pa.project_id "
						+ "WHERE pa.analysis = '"+analysis+"' AND p.sample = '"+tumor+"'")) {
					if (res.next()){
						fromdb = res.getString("p2.sample");
					}
				}	
				if (fromdb != null) {
					final String normal = fromdb;
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {	
							boxes.get(tumor).setSelectedItem(normal);
						}});
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public Map<String, String> getAssociation() {
		return tumorToNormal;
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancelClose();
		}
	}

	private void cancelClose(){
		tumorToNormal.clear();
		dispose();
	}
}
