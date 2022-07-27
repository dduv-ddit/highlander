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

package be.uclouvain.ngs.highlander.UI.details;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;

public abstract class DetailsBox extends JPanel implements Transferable, DragSourceListener, DragGestureListener, DropTargetListener, Comparable<DetailsBox> {

	protected HighlanderDatabase DB = Highlander.getDB();
	protected int variantSampleId;
	
	private DragSource source;
  private TransferHandler transfertHandler;
  private DropTarget target;
  
	protected JPanel detailsPanel = new JPanel();
	private JToggleButton showButton;

	protected void initCommonUI(boolean visible){		
		detailsPanel.setBackground(Resources.getColor(getColor(), 200, false));
		transfertHandler = new TransferHandler(){
			public Transferable createTransferable(JComponent c){
				return DetailsBox.this;
			}
		};
		setTransferHandler(transfertHandler);
		source = new DragSource();
		source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
		if(target==null) target = new DropTarget(this,this);		
		setTransferHandler(new DetailsBoxTransferHandler());

		setLayout(new BorderLayout(0,0));
		JPanel north = new JPanel();
		north.setBackground(Resources.getColor(getColor(), 200, false));
		north.setLayout(new BorderLayout(0,0));
		showButton = new JToggleButton(Resources.getScaledIcon(Resources.i2dPlus, 24));
		showButton.setPreferredSize(new Dimension(30,30));
		showButton.setSelectedIcon(Resources.getScaledIcon(Resources.i2dMinus, 24));
		showButton.setToolTipText("Show/Hide section");
		showButton.setRolloverEnabled(false);
		showButton.setSelected(visible);		
		detailsPanel.setVisible(visible);
		showButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					expand();
				}else if (arg0.getStateChange() == ItemEvent.DESELECTED){
					collapse();
				}
			}
		});
		north.add(showButton, BorderLayout.WEST);
		JLabel sectionName = new JLabel(getTitle(), JLabel.LEFT);
		north.add(sectionName, BorderLayout.CENTER);
		add(north, BorderLayout.NORTH);
		detailsPanel.setLayout(new BorderLayout(0,0));
		JLabel loadingLabel = new JLabel(Resources.getScaledIcon(Resources.iLoading, 200));
		detailsPanel.add(loadingLabel, BorderLayout.WEST);
		add(detailsPanel, BorderLayout.CENTER);
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		if (visible){
			new Thread(new Runnable(){
				public void run(){
					try{
						loadDetails();
					}catch(Exception ex){
						Tools.exception(ex);
						detailsPanel.removeAll();
						detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
					}
				}
			}, "DetailsBox.loadDetails").start();
		}
	}
 
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{new DataFlavor(String.class, "DetailsBox")};
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return true;
	}

	public Object getTransferData(DataFlavor flavor) {
		return getTitle();
	}

	public void dragEnter(DragSourceDragEvent dsde) {}
	public void dragOver(DragSourceDragEvent dsde) {}
	public void dropActionchanged(DragSourceDragEvent dsde) {}
	public void dragExit(DragSourceEvent dse) {}
	public void dropActionChanged(DragSourceDragEvent arg0) {}

	public void dragDropEnd(DragSourceDropEvent dsde) {
		repaint();
	}

	public void dragGestureRecognized(DragGestureEvent dge) {
		source.startDrag(dge, DragSource.DefaultMoveDrop, DetailsBox.this, this);       
	}

	public void dragEnter(DropTargetDragEvent dtde) {}
	public void dragOver(DropTargetDragEvent dtde) {}
	public void dropActionchanged(DropTargetDragEvent dtde) {}
	public void dragExit(DropTargetEvent dte) {}
	public void dropActionChanged(DropTargetDragEvent arg0) {}

	public void drop(DropTargetDropEvent dtde) {
		try {
			Point loc = dtde.getLocation(); 
			Transferable t = dtde.getTransferable();
			DataFlavor[] d = t.getTransferDataFlavors();
			if(getTransferHandler().canImport(this, d)){
				((DetailsBoxTransferHandler)getTransferHandler()).importData(this, t, loc);
			}
			else return;
		} catch (Exception ex) {
			Tools.exception(ex);
		}
		finally{ dtde.dropComplete(true); }
	}

	class DetailsBoxTransferHandler extends TransferHandler {

		public boolean canImport(JComponent c, DataFlavor[] f){
			DataFlavor temp = new DataFlavor(String.class, "DetailsBox");
			for(DataFlavor d:f){
				if(d.equals(temp))
					return true;

			}
			return false;
		}

		public boolean importData(JComponent comp, Transferable t, Point p){
			try {
				String boxTitle = (String)t.getTransferData(new DataFlavor(String.class, "DetailsBox"));
				SwingUtilities.convertPointToScreen(p, comp);
				return getDetailsPanel().moveDetailsBox(boxTitle, p);
			} catch (UnsupportedFlavorException ex) {
				Tools.exception(ex);
			} catch (IOException ex) {
				Tools.exception(ex);
			}
			return false;
		}

	}

	public int getVariantId(){
		return variantSampleId;
	}
	
	public abstract DetailsPanel getDetailsPanel();
	
	public abstract String getTitle();
	
	public abstract Palette getColor();
	
	@Override
	public String toString(){
		return getTitle();
	}
	
	@Override
	public int hashCode() {
		return getTitle().hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return getTitle().equals(obj.toString());
	}
	
	@Override
	public int compareTo (DetailsBox a) {
		return getTitle().compareTo(a.getTitle());
	} 

	public void showDetails(boolean visible){
		showButton.setSelected(visible);
	}
	
	protected abstract boolean isDetailsLoaded();
	
	protected abstract void loadDetails();
	
	protected void expand(){
		try{
			Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.VISIBLE, getTitle(), "1");
		}catch (Exception ex){
			Tools.exception(ex);
		}
		showButton.setSelected(true);
		detailsPanel.setVisible(true);
		if (!isDetailsLoaded()){
			new Thread(new Runnable(){
  			public void run(){
					try{
						loadDetails();
					}catch(Exception ex){
						Tools.exception(ex);
						detailsPanel.removeAll();
						detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
					}
  			}
  		}, "DetailsBox.loadDetails").start();
		}
	}

	protected void collapse(){
		try{
			Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.VISIBLE, getTitle(), "0");
		}catch (Exception ex){
			Tools.exception(ex);
		}
		showButton.setSelected(false);
		detailsPanel.setVisible(false);
	}
}
