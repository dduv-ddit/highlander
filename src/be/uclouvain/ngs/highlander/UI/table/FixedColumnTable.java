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

package be.uclouvain.ngs.highlander.UI.table;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * @author Raphael Helaers
 */

/**
 *  Prevent the specified number of columns from scrolling horizontally in
 *  the scroll pane. The table must already exist in the scroll pane.
 *
 *  The functionality is accomplished by creating a second JTable (fixed)
 *  that will share the TableModel and SelectionModel of the main table.
 *  This table will be used as the row header of the scroll pane.
 *
 *  The fixed table created can be accessed by using the getFixedTable()
 *  method. will be returned from this method. It will allow you to:
 *
 *  You can change the model of the main table and the change will be
 *  reflected in the fixed model. However, you cannot change the structure
 *  of the model.
 */
public class FixedColumnTable implements ChangeListener, PropertyChangeListener
{
	private JTable main;
	private JTable fixed;
	private JScrollPane scrollPane;

	/**
	 * 
	 * @param fixedColumns the number of columns to be fixed
	 * @param scrollPane that contain the table
	 */
	public FixedColumnTable(int fixedColumns, JScrollPane scrollPane)
	{
		this.scrollPane = scrollPane;

		main = ((JTable)scrollPane.getViewport().getView());
		main.setAutoCreateColumnsFromModel( false );
		main.addPropertyChangeListener( this );

		//  Use the existing table to create a new table sharing
		//  the DataModel and ListSelectionModel

		fixed = new JTable();
		fixed.setAutoCreateColumnsFromModel( false );
		fixed.setModel( main.getModel() );
		fixed.setSelectionModel( main.getSelectionModel() );
		fixed.setFocusable( false );

		//  Remove the fixed columns from the main table
		//  and add them to the fixed table

		for (int i = 0; i < fixedColumns; i++)
		{
			TableColumnModel columnModel = main.getColumnModel();
			TableColumn column = columnModel.getColumn( 0 );
			columnModel.removeColumn( column );
			fixed.getColumnModel().addColumn( column );
		}

		//  Add the fixed table to the scroll pane

		fixed.setPreferredScrollableViewportSize(fixed.getPreferredSize());
		scrollPane.setRowHeaderView( fixed );
		scrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, fixed.getTableHeader());

		// Synchronize scrolling of the row header with the main table

		scrollPane.getRowHeader().addChangeListener( this );
	}

	/**
	 * 
	 * @return the table being used in the row header
	 */
	public JTable getFixedTable()
	{
		return fixed;
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		//  Sync the scroll pane scrollbar with the row header
		JViewport viewport = (JViewport) e.getSource();
		scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e)
	{
		//  Keep the fixed table in sync with the main table
		if ("selectionModel".equals(e.getPropertyName()))
		{
			fixed.setSelectionModel( main.getSelectionModel() );
		}

		if ("model".equals(e.getPropertyName()))
		{
			fixed.setModel( main.getModel() );
		}
	}
}