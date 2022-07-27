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

package be.uclouvain.ngs.highlander.tools;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* Check variants in all analysis, and display orphan samples not present in projects
* @author Raphael Helaers
*/

public class AnalysisCleaner {

	public static void main(String[] args) {
		try {
			Highlander.initialize(new Parameters(false),5);
			Set<Integer> ids = new HashSet<>();
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT project_id FROM projects", true)){
				while(res.next()) {
					ids.add(res.getInt(1));
				}
			}
			Map<AnalysisFull, Set<Integer>> orphans = new TreeMap<AnalysisFull, Set<Integer>>();
			for (AnalysisFull analysis : AnalysisFull.getAvailableAnalyses()) {
				System.out.println("Checking " + analysis + " ...");
				Set<Integer> notFound = new HashSet<>();
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT project_id FROM " + analysis.getFromSampleAnnotations(), true)){
					while(res.next()) {
						int id = res.getInt(1);
						if (!ids.contains(id)) {
							notFound.add(id);
						}
					}
				}
				orphans.put(analysis, notFound);
			}
			for (AnalysisFull analysis : orphans.keySet()) {
				System.out.println(analysis + ": ");
				for (int id : orphans.get(analysis)) {
					System.out.println("\t" + id);
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
