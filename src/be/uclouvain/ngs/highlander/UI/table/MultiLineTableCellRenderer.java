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

package be.uclouvain.ngs.highlander.UI.table;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

public class MultiLineTableCellRenderer extends JTextArea implements TableCellRenderer {
  private List<List<Integer>> rowColHeight = new ArrayList<List<Integer>>();
  
  public MultiLineTableCellRenderer() {
    setLineWrap(true);
    setWrapStyleWord(true);
    setOpaque(true);
  }
  
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus,
      int row, int column) {
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      setBackground(table.getSelectionBackground());
    } else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
    }
    setFont(table.getFont());
    if (hasFocus) {
      setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
      if (table.isCellEditable(row, column)) {
        setForeground(UIManager.getColor("Table.focusCellForeground"));
        setBackground(UIManager.getColor("Table.focusCellBackground"));
      }
    } else {
      setBorder(new EmptyBorder(1, 2, 1, 2));
    }
    if (value != null) {
      setText(value.toString());
    } else {
      setText("");
    }
    adjustRowHeight(table, row, column);
    return this;
  }
  
  /**
   * Calculate the new preferred height for a given row, and sets the height on the table.
   */
  private void adjustRowHeight(JTable table, int row, int column) {
    //The trick to get this to work properly is to set the width of the column to the 
    //textarea. The reason for this is that getPreferredSize(), without a width tries 
    //to place all the text in one line. By setting the size with the with of the column, 
    //getPreferredSize() returnes the proper height which the row should have in
    //order to make room for the text.
    int cWidth = table.getTableHeader().getColumnModel().getColumn(column).getWidth();
    setSize(new Dimension(cWidth, 1000));
    int prefH = getPreferredSize().height;
    while (rowColHeight.size() <= row) {
      rowColHeight.add(new ArrayList<Integer>(column));
    }
    List<Integer> colHeights = rowColHeight.get(row);
    while (colHeights.size() <= column) {
      colHeights.add(0);
    }
    colHeights.set(column, prefH);
    int maxH = prefH;
    for (Integer colHeight : colHeights) {
      if (colHeight > maxH) {
        maxH = colHeight;
      }
    }
    if (table.getRowHeight(row) != maxH) {
      table.setRowHeight(row, maxH);
    }
  }
}
