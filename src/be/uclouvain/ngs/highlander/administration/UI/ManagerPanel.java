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

package be.uclouvain.ngs.highlander.administration.UI;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;

/**
* @author Raphael Helaers
*/

public abstract class ManagerPanel extends JPanel {
	
	protected ProjectManager manager;
	static protected HighlanderDatabase DB = ProjectManager.getDB();
	static protected WaitingPanel waitingPanel = ProjectManager.getWaitingPanel();

	public ManagerPanel(ProjectManager manager){
		this.manager = manager;
		setLayout(new BorderLayout(10,10));
	}
	

}
