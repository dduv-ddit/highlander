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

package be.uclouvain.ngs.highlander.datatype;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.UI.toolbar.SortingPanel;
import be.uclouvain.ngs.highlander.database.Field;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.Serializable;

public class SortingCriterion extends JPanel implements Transferable, DragSourceListener, DragGestureListener, Serializable {

	private static final long serialVersionUID = 1L;

	private Field field;

	//marks this SortingItem as the source of the Drag
	private DragSource source;
	private TransferHandler transfertHandler;

	private JToggleButton buttonColumn;
	private JButton buttonRemove;

	public SortingCriterion(SortingPanel sortingPanel, Field field, SortOrder sortOrder) {
		setBorder(BorderFactory.createRaisedBevelBorder());
		this.field = field;

		buttonColumn = new JToggleButton(field.getName());
		buttonColumn.setHorizontalAlignment(SwingConstants.LEADING);
		buttonColumn.setIcon(Resources.getScaledIcon(Resources.iSortAsc, 16));
		buttonColumn.setSelectedIcon(Resources.getScaledIcon(Resources.iSortDesc, 16));
		buttonColumn.setRolloverEnabled(false);
		buttonColumn.setToolTipText(field.getHtmlTooltip());
		add(buttonColumn);
		buttonColumn.setSelected(sortOrder == SortOrder.DESCENDING);

		buttonRemove = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
		add(buttonRemove);		

		addListeners(sortingPanel);

		//to be transferred in the Drag
		transfertHandler = new TransferHandler(){
			public Transferable createTransferable(JComponent c){
				return SortingCriterion.this;
			}
		};
		setTransferHandler(transfertHandler);

		//The Drag will move the SortingItem
		source = new DragSource();
		source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
	}

	public void addListeners(final SortingPanel sortingPanel){
		if(buttonColumn.getListeners(ActionListener.class).length == 0){
			buttonColumn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					sortingPanel.refresh();
				}
			});
		}
		if(buttonRemove.getListeners(ActionListener.class).length == 0){
			buttonRemove.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					sortingPanel.getCriteriaPanel().remove(SortingCriterion.this);
					sortingPanel.refresh();
				}
			});
		}
	}

	public Field getField(){
		return field;
	}

	public String getFieldName(){
		return field.getName();
	}

	public SortOrder getSortOrder(){
		return (buttonColumn.isSelected()) ? SortOrder.DESCENDING : SortOrder.ASCENDING;
	}

	//The DataFlavor is a marker to let the DropTarget know how to handle the Transferable
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{new DataFlavor(SortingCriterion.class, "SortingItem")};
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return true;
	}

	public Object getTransferData(DataFlavor flavor) {
		return this;
	}

	public void dragEnter(DragSourceDragEvent dsde) {}
	public void dragOver(DragSourceDragEvent dsde) {}
	public void dropActionchanged(DragSourceDragEvent dsde) {}
	public void dragExit(DragSourceEvent dse) {}
	public void dropActionChanged(DragSourceDragEvent arg0) {}

	//When the drag finishes, refresh (boutton still pressed etc)
	public void dragDropEnd(DragSourceDropEvent dsde) {
		repaint();
	}

	//when a DragGesture is recognized, initiate the Drag
	public void dragGestureRecognized(DragGestureEvent dge) {
		source.startDrag(dge, DragSource.DefaultMoveDrop, SortingCriterion.this, this);       
	}


}
