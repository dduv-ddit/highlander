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

import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;


public class IndependentFilteringPanel extends FilteringPanel {
	
	public IndependentFilteringPanel(Highlander mainFrame, HighlanderObserver obs) {
		super(mainFrame, obs);
	}
	
	@Override
	protected void initUI(){
		setLayout(new BorderLayout(0, 0));
		JPanel panel = new JPanel(new BorderLayout(0, 0));
		
		JPanel panel_w = new JPanel();
		panel.add(panel_w, BorderLayout.WEST);
		initWestPanel(panel_w);
		
		initCenterPanel(panel);
		
		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, obs, 40);
	  add(scrollablePanel, BorderLayout.CENTER);

	}
	


}
