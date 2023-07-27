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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;


public class FiltersTemplate {

	private String templateName;
	private Analysis analysis;
	private Map<String,String> filtersSaveStrings = new LinkedHashMap<>();

	public FiltersTemplate(Analysis analysis, Map<String,String> filtersSaveStrings, String templateName){
		this.analysis = analysis;
		this.filtersSaveStrings = filtersSaveStrings;
		this.templateName = templateName;
	}

	public FiltersTemplate(Analysis analysis, String templateName){
		this.analysis = analysis;
		this.templateName = templateName;		
	}

	@Override
	public String toString(){
		return templateName;
	}

	public Analysis getAnalysis(){
		return analysis;
	}

	public boolean changeAnalysis(Analysis newAnalysis) throws Exception {
		for (String filterSaveString : filtersSaveStrings.values()) {
			Filter filter = new ComboFilter().loadCriterion(null, filterSaveString);
			if (!filter.checkFieldCompatibility(newAnalysis)) return false;
		}
		for (String f : filtersSaveStrings.keySet()) {
			filtersSaveStrings.put(f, filtersSaveStrings.get(f).replaceAll("found_in_"+newAnalysis, "found_in_"+analysis));
		}
		analysis = newAnalysis;
		return true;
	}

	public Map<String,String> getFiltersSaveStrings() {
		return filtersSaveStrings;
	}

	public Set<String> getPlaceHolders() {
		Set<String> placeholders = new TreeSet<>();
		for (String filter : filtersSaveStrings.values()) {
			int start = filter.indexOf('@');
			int end = filter.indexOf('@', start+1);
			while (start > -1 && end > -1) {
				placeholders.add(filter.substring(start+1, end));
				start = filter.indexOf('@', end+1);
				end = filter.indexOf('@', start+1);
			}
		}
		return placeholders;
	}

	public List<String> getSaveStrings(){
		List<String> saveStrings = new ArrayList<String>();

		for (String filterName : filtersSaveStrings.keySet()) {
			String filterSaveString = UserData.FILTER + "°" + filterName + "°" + filtersSaveStrings.get(filterName);
			saveStrings.add(filterSaveString);
		}

		return saveStrings;
	}

	public void parseSaveString(String saveString) throws Exception {
		int cut = saveString.indexOf('°');
		UserData data = UserData.valueOf(saveString.substring(0, cut));
		String[] vals = saveString.substring(cut+1, saveString.length()).split("°");
		switch(data){
		case FILTER:
			filtersSaveStrings.put(vals[0], vals[1]);			
			break;
		default:
			throw new Exception(data + " is not supported for filters templates");
		}
	}

	public void generateFilters(String path, Map<String,String> placeholders) throws Exception {
		for (String filterName : filtersSaveStrings.keySet()) {
			String filter = filtersSaveStrings.get(filterName);
			for (String placeholder : placeholders.keySet()) {
				filter = filter.replace("@" + placeholder + "@", placeholders.get(placeholder));
			}
			Highlander.getLoggedUser().saveFilter(path + "~" + filterName, analysis, filter);
		}
	}

}
