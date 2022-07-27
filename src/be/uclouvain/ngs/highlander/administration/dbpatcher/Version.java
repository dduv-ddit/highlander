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

package be.uclouvain.ngs.highlander.administration.dbpatcher;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public abstract class Version implements Comparable<Version> {

	protected final String version;
	protected static HighlanderDatabase DB;
	
	public Version(String version) {
		this.version = version;
		DB = Highlander.getDB();
	}
	
	public static void setDatabase(HighlanderDatabase database) {
		DB = database;
	}
	
	public void update() throws Exception {
		toConsole("Updating to "+version+" ...");
		makeUpdate();
		DbPatcher.currentVersion = version;
		updateAndPrint(Schema.HIGHLANDER, "UPDATE main SET version = '"+version+"'");
	}
	
	protected abstract void makeUpdate() throws Exception;
	
	public String getVersion() {
		return version;
	}
	
	@Override
	public String toString(){
		return version;
	}

	@Override
	public int hashCode() {
		return version.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return version.equals(obj.toString());
	}

	@Override
	public int compareTo (Version v) {
		return version.compareTo(v.version);
	}
	
	protected void toConsole(String line) {
		DbPatcher.toConsole(line);
	}
	
	protected void updateAndPrint(Schema schema, String query) throws Exception {
		DbPatcher.updateAndPrint(schema, query);
	}
	
}
