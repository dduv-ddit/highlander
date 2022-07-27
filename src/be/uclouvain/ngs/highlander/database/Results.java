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

package be.uclouvain.ngs.highlander.database;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Reference;

public class Results implements AutoCloseable {
	private Connection con;
	private Statement stm;
	private ResultSet res;

	private String sqlStatement;
	
	public Results(HighlanderDatabase DB, Reference reference, Schema schema, String sqlStatement, boolean hugeResultSetExpected) throws Exception {
		this.sqlStatement = sqlStatement;
		try {
			con = DB.getConnection(reference, schema);
			if (hugeResultSetExpected && DB.getDBMS(schema) == DBMS.mysql){
				//Necessary to fetch row one by one and avoid storing the whole ResultSet in memory
				stm = con.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
				stm.setFetchSize(Integer.MIN_VALUE);
			}else{
				stm = con.createStatement();
			}
		}catch (Exception ex) {
			close();
			throw ex;
		}
	}

	public void executeStatement() throws Exception {
		try {
			res = stm.executeQuery(sqlStatement);
		}catch (Exception ex) {
			close();
			throw ex;
		}
	}
	
	public Connection getConnection() {
		return con;
	}
	
	public Statement getStatement() {
		return stm;
	}
	
	public ResultSet getResultSet() {
		return res;
	}

	@Override
	public void close() {
		try {
			if (res != null) res.close();
			if (stm != null) stm.close();
			if (con != null) con.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public boolean next() throws SQLException {
		return res.next();
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return res.getMetaData();
	}
  /**
   * Determines the number of rows in a <code>ResultSet</code>. Upon exit, if the cursor was not
   * currently on a row, it is just before the first row in the result set (a call to
   * {@link ResultSet#next()} will go to the first row).
   * @param set The <code>ResultSet</code> to check (must be scrollable).
   * @return The number of rows or -1 if the <code>ResultSet</code> is not scrollable.
   */
  public int getResultSetSize() {
  	try{
  		int rowCount;
  		int currentRow = res.getRow();            // Get current row
  		rowCount = res.last() ? res.getRow() : 0; // Determine number of rows
  		if (currentRow == 0)                      // If there was no current row
  			res.beforeFirst();                     // We want next() to go to first row
  		else                                      // If there WAS a current row
  			res.absolute(currentRow);              // Restore it
  		return rowCount;
  	}catch(SQLException ex){
  		return -1;
  	}
  }

	public boolean wasNull() throws SQLException {
		return res.wasNull();
	}

	public String getString(int columnIndex) throws SQLException {
		return res.getString(columnIndex);
	}

	public String getString(String columnLabel) throws SQLException {
		return res.getString(columnLabel);
	}

	public boolean getBoolean(int columnIndex) throws SQLException {
		return res.getBoolean(columnIndex);
	}

	public boolean getBoolean(String columnLabel) throws SQLException {
		return res.getBoolean(columnLabel);
	}

	public Date getDate(int columnIndex) throws SQLException {
		return res.getDate(columnIndex);
	}

	public Date getDate(String columnLabel) throws SQLException {
		return res.getDate(columnLabel);
	}

	public double getDouble(int columnIndex) throws SQLException {
		return res.getDouble(columnIndex);
	}

	public double getDouble(String columnLabel) throws SQLException {
		return res.getDouble(columnLabel);
	}

	public int getInt(int columnIndex) throws SQLException {
		return res.getInt(columnIndex);
	}

	public int getInt(String columnLabel) throws SQLException {
		return res.getInt(columnLabel);
	}

	public long getLong(int columnIndex) throws SQLException {
		return res.getLong(columnIndex);
	}
	
	public long getLong(String columnLabel) throws SQLException {
		return res.getLong(columnLabel);
	}
	
	public Blob getBlob(int columnIndex) throws SQLException {
		return res.getBlob(columnIndex);
	}

	public Blob getBlob(String columnLabel) throws SQLException {
		return res.getBlob(columnLabel);
	}

	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return res.getTimestamp(columnIndex);
	}
	
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return res.getTimestamp(columnLabel);
	}
	
	public Object getObject(int columnIndex) throws SQLException {
		return res.getObject(columnIndex);
	}
	
	public Object getObject(String columnLabel) throws SQLException {
		return res.getObject(columnLabel);
	}
	
}

