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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.administration.dbpatcher.DbPatcher;
import be.uclouvain.ngs.highlander.administration.dbpatcher.Version;

/**
* @author Raphael Helaers
*/

public class UpgradePanel extends ManagerPanel {
	
	public UpgradePanel(ProjectManager manager){
		super(manager);
		setLayout(new BorderLayout(10,10));
		JPanel centerPanel = new JPanel(new GridBagLayout());
		JLabel labelCurrent = new JLabel("Current Highlander database version");
		ProjectManager.getDbPatcher().labelCurrentVersion = new JLabel(DbPatcher.currentVersion);		
		JLabel labelUpdate = new JLabel("Update database to version");
		ProjectManager.getDbPatcher().box = new JComboBox<Version>(DbPatcher.availableVersions);
		ProjectManager.getDbPatcher().box.setSelectedIndex(DbPatcher.availableVersions.length-1);
		centerPanel.add(labelCurrent, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(ProjectManager.getDbPatcher().labelCurrentVersion, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(labelUpdate, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(ProjectManager.getDbPatcher().box, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(new JPanel(), new GridBagConstraints(2, 0, 0, 2, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
		add(centerPanel, BorderLayout.NORTH);

		JPanel southPanel = new JPanel();
		JButton updateButton = new JButton("Update", Resources.getScaledIcon(Resources.iUpdater, 16));
		updateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						ProjectManager.getDbPatcher().update();
					}
				}, "UpgradePanel.update").start();
			}
		});
		southPanel.add(updateButton);		
		add(southPanel, BorderLayout.SOUTH);

	}

}
