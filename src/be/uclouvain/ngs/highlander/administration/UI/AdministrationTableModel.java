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

import javax.swing.table.AbstractTableModel;

/**
* @author Raphael Helaers
*/

public class AdministrationTableModel	extends AbstractTableModel {
	private Object[][] data;
	private String[] headers;

	public AdministrationTableModel(Object[][] data, String[] headers) {    	
		this.data = data;
		this.headers = headers;
	}

	@Override
	public int getColumnCount() {
		return headers.length;
	}

	@Override
	public String getColumnName(int col) {
		return headers[col];
	}

	public int getColumn(String header){
		for (int i = 0 ; i < headers.length ; i++){
			if (headers[i].equals(header)){
				return i;
			}
		}
		return -1;
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		data[row][col] = value;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		boolean edit = false;
		if (columnIndex == getColumn("family")) edit = true;
		if (columnIndex == getColumn("individual")) edit = true;
		if (columnIndex == getColumn("sample")) edit = true;
		if (columnIndex == getColumn("barcode")) edit = true;
		if (columnIndex == getColumn("comments")) edit = true;
		return edit;
	}

}

