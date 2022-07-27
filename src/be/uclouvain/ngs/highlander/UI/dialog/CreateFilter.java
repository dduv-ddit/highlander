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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JButton;
import javax.swing.JDialog;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;

public class CreateFilter extends JDialog {

	private HighlanderObserver obs = new HighlanderObserver();
	private IndependentFilteringPanel filteringPanel;
	
	public CreateFilter(Highlander mainFrame){
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI(mainFrame);
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
				obs.setControlName("RESIZE_TOOLBAR");
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});
	}
	
	private void initUI(Highlander mainFrame){
		filteringPanel = new IndependentFilteringPanel(mainFrame, obs);
		getContentPane().add(filteringPanel, BorderLayout.CENTER);
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		getContentPane().add(okButton, BorderLayout.SOUTH);
	}
	
	public FilteringPanel getFilteringPanel(){
		return filteringPanel;
	}
}
