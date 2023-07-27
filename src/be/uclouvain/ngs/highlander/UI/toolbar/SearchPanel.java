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

package be.uclouvain.ngs.highlander.UI.toolbar;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable.VariantsTableModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.KeyEvent;

public class SearchPanel extends JPanel {
	
	private final Highlander mainframe;
	private final VariantsTable table;
	private JToggleButton pressEnterButton;
	private SearchField searchField = new SearchField(20){
		@Override
		protected void keyListener(KeyEvent key){
			if (table == null) {
				JOptionPane.showMessageDialog(SearchPanel.this, "Table is empty, you must first generate a filter.", "No table", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}else{
				if (!pressEnterButton.isSelected() || key.getKeyCode() == KeyEvent.VK_ENTER){			
					applyFilter();
				}
			}
		}
		@Override
		public void applyFilter(){
			setIcon();
	    RowFilter<VariantsTableModel, Object> rf = null;
	    //If current expression doesn't parse, don't update.
	    try {
	        rf = RowFilter.regexFilter("(?i)"+getText());
	    } catch (java.util.regex.PatternSyntaxException e) {
	        return;
	    }
	    final RowFilter<VariantsTableModel, Object> rff = rf;
			Highlander.waitingPanel.start();
			new Thread(new Runnable(){
	  		@Override
				public void run(){
	  			try{
	  				table.setTextFilter(rff,getTyppedText());
	  			}catch(Exception ex){
	  				Tools.exception(ex);
	  			}
	  			Highlander.waitingPanel.stop();
	  		}
	  	}, "SearchPanel.applyFilter").start();   
		}
	};
	
	public void setIcon(){
		if (searchField.getText().length() > 0){
			mainframe.tabbedPane.setIconAt(5, Resources.getScaledIcon(Resources.iSearchGlow, 32));
		}else{
			mainframe.tabbedPane.setIconAt(5, Resources.getScaledIcon(Resources.iSearch, 32));
		}
	}
	
	public SearchPanel(Highlander mainframe) {
		this.mainframe = mainframe;
		this.table = mainframe.getVariantTable();
		
		setLayout(new BorderLayout(0, 0));

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		add(mainPanel, BorderLayout.CENTER);
		
		pressEnterButton = new JToggleButton(Resources.getScaledIcon(Resources.iPressKey, 40));
		pressEnterButton.setSelectedIcon(Resources.getScaledIcon(Resources.iPressEnter, 40));
		pressEnterButton.setRolloverIcon(Resources.getScaledIcon(Resources.iPressEnter, 40));
		pressEnterButton.setRolloverSelectedIcon(Resources.getScaledIcon(Resources.iPressKey, 40));
		pressEnterButton.setRolloverEnabled(true);
		pressEnterButton.setToolTipText("Row are masked immediately as you type (can be slow with a lot of variants), or you have to press enter to apply");
		pressEnterButton.setPreferredSize(new Dimension(54,54));		
		mainPanel.add(pressEnterButton, new GridBagConstraints(0,0,1,2,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,10,0,10), 0, 0));
		
		JLabel title = new JLabel("Mask rows that do not contain typed text, and highlight it in bold green");
		mainPanel.add(title, new GridBagConstraints(1,0,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,5,0,10), 0, 0));
		mainPanel.add(searchField, new GridBagConstraints(1,1,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,5,0,10), 0, 0));
		
	}
	
}
