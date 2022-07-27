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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import be.uclouvain.ngs.highlander.Parameters.PasswordPolicy;
import be.uclouvain.ngs.highlander.Parameters.Protocol;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public class GlobalSettingsPanel extends ManagerPanel {
	
	private DefaultMutableTreeNode root;
	private TreeModel treeModel;
	private JTree tree;
	private JScrollPane scrollData = new JScrollPane();

	public GlobalSettingsPanel(ProjectManager manager){
		super(manager);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		add(splitPane, BorderLayout.CENTER);

		JPanel panel_left = new JPanel(new BorderLayout(5,5));
		panel_left.setBorder(BorderFactory.createTitledBorder("Sections"));
		splitPane.setLeftComponent(panel_left);
		
		JScrollPane scrollPane_left = new JScrollPane();
		panel_left.add(scrollPane_left, BorderLayout.CENTER);

		root = new DefaultMutableTreeNode("highlander", true);
		treeModel = new DefaultTreeModel(root);
		tree = new JTree(treeModel);
		scrollPane_left.setViewportView(tree);
		buildTree(root);
		for (int i = 0; i < tree.getRowCount(); i++) {
	    tree.expandRow(i);
		}
		tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
      	DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
      	StringBuilder sb = new StringBuilder();
      	TreeNode[] path = selectedNode.getPath();
      	for (int i=1 ; i < path.length ; i++) {
      		sb.append(((DefaultMutableTreeNode)path[i]).getUserObject().toString());
      		if (i < path.length-1) sb.append("|");
      	}
      	fill(sb.toString());
      }
    }) ;
		
		JPanel panel_right = new JPanel(new BorderLayout());
		panel_right.setBorder(new EmptyBorder(10, 5, 0, 5));
		splitPane.setRightComponent(panel_right);

		panel_right.add(scrollData, BorderLayout.CENTER);
		scrollData.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

	}
	
	private void buildTree(DefaultMutableTreeNode root) {
		try(Results res = DB.select(Schema.HIGHLANDER, "SELECT section FROM `settings` ORDER BY ordering")){
			while (res.next()) {
				String section = res.getString("section");
				String[] sections = section.split("\\|");
				DefaultMutableTreeNode parent = root;
				for (int i=0 ; i < sections.length ; i++) {
					boolean exists = false;
					for (int j=0 ; j < parent.getChildCount() ; j++) {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode)parent.getChildAt(j);
						if (node.getUserObject().toString().equals(sections[i])) {
							parent = node;
							exists = true;
							break;
						}
					}
					if (!exists) {
						DefaultMutableTreeNode node = new DefaultMutableTreeNode(sections[i]);
						parent.add(node);
						parent = node;
					}
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void fill(String section) {
		scrollData.setViewportView(null);
		if (section != null) {
			JPanel panel = new JPanel(new GridBagLayout());
			int row = 0;
			try(Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM `settings` WHERE section = '"+section+"' ORDER BY ordering")){
				while (res.next()) {
					String setting = res.getString("setting");
					String value = res.getString("value");
					String description = res.getString("description");
					
					JLabel label_setting = new JLabel(setting);
					label_setting.setToolTipText(description);
					JComponent txt_value;
					if (setting.equals("passwordpolicy")) {
						txt_value = new JComboBox<PasswordPolicy>(PasswordPolicy.values());
						txt_value.setToolTipText(description);
						((JComboBox<?>)txt_value).setSelectedItem(PasswordPolicy.valueOf(value));
						((JComboBox<?>)txt_value).addItemListener(new ItemListener() {
							@Override
							public void itemStateChanged(ItemEvent e) {
								JComboBox<?> source = (JComboBox<?>)txt_value;
								if (e.getStateChange() == ItemEvent.SELECTED){
									if (source.getSelectedIndex() >= 0) {
										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
												PasswordPolicy val = (PasswordPolicy)source.getSelectedItem();
													try {
														DB.update(Schema.HIGHLANDER, "UPDATE `settings` SET `value` = '"+DB.format(Schema.HIGHLANDER, val.toString())+"' WHERE `section` = '"+DB.format(Schema.HIGHLANDER, section)+"' AND `setting` = '"+DB.format(Schema.HIGHLANDER, setting)+"'");
													}catch(Exception ex) {
														ProjectManager.toConsole(ex);
													}
											}
										});
									}
								}
							}
						});
					}else if (setting.equals("protocol")) {
							txt_value = new JComboBox<Protocol>(Protocol.values());
							txt_value.setToolTipText(description);
							((JComboBox<?>)txt_value).setSelectedItem(Protocol.valueOf(value));
							((JComboBox<?>)txt_value).addItemListener(new ItemListener() {
								@Override
								public void itemStateChanged(ItemEvent e) {
									JComboBox<?> source = (JComboBox<?>)txt_value;
									if (e.getStateChange() == ItemEvent.SELECTED){
										if (source.getSelectedIndex() >= 0) {
											SwingUtilities.invokeLater(new Runnable() {
												@Override
												public void run() {
													Protocol val = (Protocol)source.getSelectedItem();
													try {
														DB.update(Schema.HIGHLANDER, "UPDATE `settings` SET `value` = '"+DB.format(Schema.HIGHLANDER, val.toString())+"' WHERE `section` = '"+DB.format(Schema.HIGHLANDER, section)+"' AND `setting` = '"+DB.format(Schema.HIGHLANDER, setting)+"'");
													}catch(Exception ex) {
														ProjectManager.toConsole(ex);
													}
												}
											});
										}
									}
								}
							});
					}else {
						txt_value = new JTextField(value);
						txt_value.setToolTipText(description);
						((JTextField)txt_value).setColumns(15);
						txt_value.setInputVerifier(new InputVerifier() {
							@Override
							public boolean verify(JComponent input) {
								JTextField inputTxtField = (JTextField)input;
								try {
									DB.update(Schema.HIGHLANDER, "UPDATE `settings` SET `value` = '"+DB.format(Schema.HIGHLANDER, inputTxtField.getText())+"' WHERE `section` = '"+DB.format(Schema.HIGHLANDER, section)+"' AND `setting` = '"+DB.format(Schema.HIGHLANDER, setting)+"'");
								}catch(Exception ex) {
									ProjectManager.toConsole(ex);
									inputTxtField.setForeground(Color.RED);
									return false;
								}
								inputTxtField.setForeground(Color.BLACK);
								return true;
							}
						});
					}

					panel.add(label_setting, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
					panel.add(txt_value, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
					
					row++;
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}			
			panel.add(new JPanel(), new GridBagConstraints(0, row, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			scrollData.setViewportView(panel);

		}
	}
	
}
