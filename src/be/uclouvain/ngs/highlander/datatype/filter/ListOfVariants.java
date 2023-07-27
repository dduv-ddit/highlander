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

package be.uclouvain.ngs.highlander.datatype.filter;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public class ListOfVariants extends MagicFilter {

	private Set<Integer> ids;

	public ListOfVariants(Collection<Integer> ids){
		this.ids = new TreeSet<Integer>(ids);
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.LIST_OF_VARIANTS;
	}

	@Override
	public Set<String> getIncludedSamples(){
		return new TreeSet<String>();
	}

	@Override
	public Set<String> getExcludedSamples(){
		return new TreeSet<String>();
	}

	@Override
	public Set<String> getUserDefinedSamples(boolean includeProfileList){
		Set<String> set = getIncludedSamples();
		set.addAll(getExcludedSamples());
		return set;
	}

	@Override
	public String toString(){
		return "List of variants ("+ids.size()+" id's)";
	}

	@Override
	public boolean isFilterValid(){
		return true;
	}

	@Override
	public List<String> getValidationProblems(){
		return new ArrayList<String>();
	}

	@Override
	public String toHtmlString(){
		return toString();
	}

	@Override
	public String getSaveString(){
		return null;
	}

	@Override
	public String parseSaveString(String saveString){
		return null;
	}

	@Override
	public Filter loadCriterion(FilteringPanel filteringPanel, String saveString) throws Exception {
		return null;
	}

	@Override
	public boolean checkFieldCompatibility(Analysis analysis){
		return true;
	}

	@Override
	public boolean changeAnalysis(Analysis analysis){
		return false;
	}

	@Override
	public void editFilter(){

	}

	@Override
	public int getNumberOfVariants() {
		return ids.size();
	}

	@Override
	public Map<Integer,String> getResultIds(Set<String> autoSamples) throws Exception {	
		Map<Integer,String> results = new HashMap<Integer, String>();
		if (!ids.isEmpty()){
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT variant_sample_id, chr, pos, length, reference, alternative "
					+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations() 
					+ "WHERE variant_sample_id IN (" + HighlanderDatabase.makeSqlList(ids, Integer.class) + ")"
					, true)) {
				while (res.next()){
					int id = res.getInt("variant_sample_id");
					String variant = res.getString("chr") + "-" + res.getInt("pos") + "-" + + res.getInt("length") + "-" + res.getString("reference") + "-" + res.getString("alternative");
					results.put(id, variant);
				}
			}
		}
		return results;
	}
}
