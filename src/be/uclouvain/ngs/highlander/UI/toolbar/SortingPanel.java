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

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateColumnSelection;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.SortingCriterion;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JButton;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SortingPanel extends JPanel implements DropTargetListener {
	
	private DropTarget target;
	
	private JPanel sortingCriteria;
	private final VariantsTable table;
	
	private String currentSortingName = null;
	
	public SortingPanel(final Highlander mainframe) {
		this.table = mainframe.getVariantTable();
		
		setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JButton btnSave = new JButton(Resources.getScaledIcon(Resources.iDbSave, 40));
		btnSave.setToolTipText("Save current criteria list in your profile");
		btnSave.setPreferredSize(new Dimension(54,54));
		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					String name = ProfileTree.showProfileDialog(mainframe, Action.SAVE, UserData.SORTING, Highlander.getCurrentAnalysis().toString(), "Save "+UserData.SORTING.getName()+" to your profile", currentSortingName);
					if (name == null) return;
					if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.SORTING, Highlander.getCurrentAnalysis().toString(), name)){
						int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
								"You already have a sorting criteria list named '"+name.replace("~", " -> ")+"', do you want to overwrite it ?", 
								"Overwriting sorting criteria list in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
						if (yesno == JOptionPane.NO_OPTION)	return;
					}
					String sortingName = name;
					List<SortingCriterion> list = getSortingCriteria();
					Highlander.getLoggedUser().saveSorting(sortingName, Highlander.getCurrentAnalysis(), list);
					currentSortingName = sortingName;
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Save current criteria list in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		});
		panel.add(btnSave);
		
		JButton btnLoad = new JButton(Resources.getScaledIcon(Resources.iDbLoad, 40));
		btnLoad.setToolTipText("Load a criteria list from your profile");
		btnLoad.setPreferredSize(new Dimension(54,54));
		btnLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String name = ProfileTree.showProfileDialog(mainframe, Action.LOAD, UserData.SORTING, Highlander.getCurrentAnalysis().toString());
				if (name != null){					
					try {
						setSorting(Highlander.getLoggedUser().loadSorting(SortingPanel.this, Highlander.getCurrentAnalysis(), name), name);
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Load criteria list from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		});
		panel.add(btnLoad);
		
		JButton button_add = new JButton(Resources.getScaledIcon(Resources.i3dPlus, 40));
		button_add.setToolTipText("Add sorting criterion");
		button_add.setPreferredSize(new Dimension(54,54));
		button_add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (table == null) {
					JOptionPane.showMessageDialog(new JFrame(), "Table is empty, you must first generate a filter.", "No table", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}else{
					List<Field> availableColumns = table.getAvailableColumns();
					for (Object o : sortingCriteria.getComponents()){
						String colName = ((SortingCriterion)o).getField().getName();
						for (java.util.Iterator<Field> it = availableColumns.iterator() ; it.hasNext() ; ){
							Field f = it.next();
							if (f.getName().equals(colName)){
								it.remove();
								break;
							}
						}
					}
					CreateColumnSelection askCol = new CreateColumnSelection(Highlander.getCurrentAnalysis(), UserData.SORTING, availableColumns);
					Tools.centerWindow(askCol, false);
					askCol.setVisible(true);
					for (Field field : askCol.getSelection()){
						SortingCriterion item = new SortingCriterion(SortingPanel.this, field, SortOrder.ASCENDING);
						sortingCriteria.add(item);
					}
					refresh();
				}
			}
		});
		panel.add(button_add);
		
		sortingCriteria = new JPanel();
		FlowLayout flowLayout = (FlowLayout) sortingCriteria.getLayout();
		flowLayout.setVgap(10);
		flowLayout.setHgap(10);
		flowLayout.setAlignment(FlowLayout.LEADING);
		panel.add(sortingCriteria);
		
		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
	  add(scrollablePanel, BorderLayout.CENTER);
		
		//mark this a DropTarget
		if(target==null) target = new DropTarget(this,this);

		//have it utilize a custom transfer handler
		setTransferHandler(new SortingItemTransferHandler());
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {}
	@Override
	public void dragOver(DropTargetDragEvent dtde) {}
	public void dropActionchanged(DropTargetDragEvent dtde) {}
	@Override
	public void dragExit(DropTargetEvent dte) {}
	@Override
	public void dropActionChanged(DropTargetDragEvent arg0) {}

	//This is what happens when a Drop occurs
	@Override
	public void drop(DropTargetDropEvent dtde) {
		try {
			//get the Point where the drop occurred
			Point loc = dtde.getLocation(); 

			//get Transfer data
			Transferable t = dtde.getTransferable();

			//get the Data flavors transferred with the Transferable
			DataFlavor[] d = t.getTransferDataFlavors();

			//and if the DataFlavors match for the SortingPanel 
			//(ie., we don't want an ImageFlavor marking an image transfer)
			if(getTransferHandler().canImport(this, d)){
				//then import the Draggable JComponent and repaint() the JTable
				((SortingItemTransferHandler)getTransferHandler()).importData(this, (SortingCriterion)t.getTransferData(d[0]), loc);
			}
			else return;

		} catch (UnsupportedFlavorException ex) {
			Tools.exception(ex);
		} catch (IOException ex) {
			Tools.exception(ex);
		}
		finally{ dtde.dropComplete(true); }
	}

	class SortingItemTransferHandler extends TransferHandler {

		//tests for a valid SortingItem DataFlavor
		@Override
		public boolean canImport(JComponent c, DataFlavor[] f){
			DataFlavor temp = new DataFlavor(SortingCriterion.class, "SortingItem");
			for(DataFlavor d:f){
				if(d.equals(temp))
					return true;

			}
			return false;
		}

		//add the data into the JTable
		public boolean importData(JComponent comp, Transferable t, Point p){
			try {
				SortingCriterion tempItem = (SortingCriterion)t.getTransferData(new DataFlavor(SortingCriterion.class, "SortingItem"));
				int oldPos;
				for (oldPos = 0 ; oldPos < sortingCriteria.getComponentCount() ; oldPos++){
					SortingCriterion si = (SortingCriterion)(sortingCriteria.getComponent(oldPos));
					if (si.getFieldName().equals(tempItem.getFieldName())) break;
				}
				int newPos;
				SwingUtilities.convertPointToScreen(p, comp);
				for (newPos = 0 ; newPos < sortingCriteria.getComponentCount() ; newPos++){
					Component c = sortingCriteria.getComponent(newPos);
					if (p.getX() <= c.getLocationOnScreen().getX()) break;
				}
				if (oldPos == newPos) return false;
				if (oldPos < newPos)	newPos--;			
				sortingCriteria.remove(oldPos);
				sortingCriteria.add(tempItem, newPos);
				tempItem.addListeners(SortingPanel.this);
				tempItem.revalidate();
				refresh();
			} catch (UnsupportedFlavorException ex) {
				Tools.exception(ex);
			} catch (IOException ex) {
				Tools.exception(ex);
			}
			return true;
		}

	}

	public JPanel getCriteriaPanel(){
		return sortingCriteria;
	}
	
	public List<SortingCriterion> getSortingCriteria(){
		List<SortingCriterion> list = new ArrayList<SortingCriterion>();
		for (Object o : sortingCriteria.getComponents()){
			list.add((SortingCriterion)o);
		}
		return list;
	}
	
	public void removeMaskedColumns(){
		for (Object o : sortingCriteria.getComponents()){
			SortingCriterion crit = (SortingCriterion)o;
			if (!table.hasColumn(crit.getField())){
				sortingCriteria.remove(crit);
			}
		}
		refresh();
	}
	
	public void setSorting(List<SortingCriterion> list, String sortingName) throws Exception {
		sortingCriteria.removeAll();
		for (SortingCriterion item : list){
			if (table.hasColumn(item.getField())) sortingCriteria.add(item);
		}
		currentSortingName = sortingName;
		refresh();				
	}
	
	public void refresh(){
		validate();
		repaint();
		Highlander.getHighlanderObserver().setControlName("RESIZE_TOOLBAR");
		Highlander.waitingPanel.start();
		new Thread(new Runnable(){
  		@Override
			public void run(){
  			try{
	  			if (table != null && !table.isEmpty()){
	  				table.setSorting(getSortingCriteria());
	  			}
  			}catch(Exception ex){
  				Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Sorting table", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
  			}
  			Highlander.waitingPanel.stop();
  		}
  	}, "SortingPanel.refresh").start();
	}

}
