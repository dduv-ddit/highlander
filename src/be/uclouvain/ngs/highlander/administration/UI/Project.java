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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
* @author Raphael Helaers
*/

public class Project {

	public String run_label;
	public boolean pair_end;
	public String run_path_full;
	public String run_path;
	public String comments;
	public Map<String,String> kits = new HashMap<String, String>();
	public Map<String,String> references = new HashMap<String, String>(); //TODO SCRIPTS 17 - à virer, mais revoir le relauncher après avoir revu les scripts
	public String[] analyses;

	public void setPathAndKit(String path, String kit, String reference){
		run_path_full = path;
		run_path = path;
		if (run_path.startsWith("/")) run_path = run_path.substring(1);
		if (run_path.endsWith("/")) run_path = run_path.substring(0,run_path.length()-1);
		run_path = run_path.substring(0,run_path.lastIndexOf("/"));
		String[] partsRunPath = run_path.split("\\/");
		kits.put(partsRunPath[partsRunPath.length-1], kit);
		references.put(partsRunPath[partsRunPath.length-1], reference);			
	}

	public String toString(){
		return run_label;
	}

	public String getAllAnalyses(){
		String res = "";
		for (int i=0 ; i < analyses.length ; i++){
			res += analyses[i];
			if (i+1 < analyses.length) res += "+";
		}
		return res;
	}

	public String getAllAnalysesCompatibleWith(Set<String> compatible){
		String res = "";
		for (int i=0 ; i < analyses.length ; i++){
			if (compatible.contains(analyses[i])){
				res += analyses[i] + "+";
			}
		}
		if (res.length() > 0) res = res.substring(0, res.length()-1);
		return res;
	}
}
