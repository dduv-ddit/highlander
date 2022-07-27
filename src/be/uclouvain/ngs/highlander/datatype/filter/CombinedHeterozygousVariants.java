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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateMagicFilter;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public class CombinedHeterozygousVariants extends MagicFilter {

	private String child;
	private String father;
	private String mother;

	private int nVariants = -1;

	public CombinedHeterozygousVariants(FilteringPanel filteringPanel, String child, String father, String mother, ComboFilter preFiltering){
		this.filteringPanel = filteringPanel;
		this.child = child;
		this.father = father;
		this.mother = mother;
		this.preFiltering = preFiltering;

		displayCriterionPanel();
	}

	public CombinedHeterozygousVariants(){
		child = null;
		father = null;
		mother = null;
	}

	public FilterType getFilterType(){
		return FilterType.COMBINED_HETEROZYGOUS_VARIANTS;
	}

	public Set<String> getIncludedSamples(){
		Set<String> set = new HashSet<String>();
		set.add(child);
		set.add(father);
		set.add(mother);
		return set;
	}

	public Set<String> getExcludedSamples(){
		return new TreeSet<String>();
	}

	public String getChild(){
		return child;
	}

	public String getFather(){
		return father;
	}

	public String getMother(){
		return mother;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getFilterType().getName() + ": ");
		sb.append("child = " + child + ", father = " + father + ", mother = " + mother);
		if (preFiltering != null){			
			sb.append(", with prefiltering: " + preFiltering.toString());
		}
		return sb.toString();
	}

	public String toHtmlString(){
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(getFilterType().getDescription().replace("\n", "<br>"));
		sb.append("<br>Child = "+child);
		sb.append("<br>Father = "+father);
		sb.append("<br>Mother = "+mother);
		if (preFiltering != null){
			sb.append("<br>Prefiltering: ");
			sb.append("<br>");
			sb.append(preFiltering.toHtmlString());
		}
		sb.append("</html>");
		return sb.toString();
	}

	public boolean isFilterValid(){
		if (preFiltering != null){
			return preFiltering.isFilterValid();
		}else{
			return true;
		}
	}

	public List<String> getValidationProblems(){
		List<String> problems = new ArrayList<String>();
		if (preFiltering != null){
			problems.addAll(preFiltering.getValidationProblems());
		}
		return problems;
	}

	public String getSaveString(){
		StringBuilder sb = new StringBuilder();
		sb.append(child);
		sb.append("#");
		sb.append(father);
		sb.append("#");
		sb.append(mother);
		sb.append("#");
		if (preFiltering != null){
			sb.append(preFiltering.getSaveString());
		}else{
			sb.append("NO_PREFILTERING");
		}
		return sb.toString();
	}

	public String parseSaveString(String saveString){
		StringBuilder sb = new StringBuilder();
		sb.append(getFilterType().getName() + ": ");
		int beginIndex = 0;
		int endIndex = saveString.indexOf("#");
		sb.append("child = " + saveString.substring(beginIndex, endIndex));
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		sb.append(", father = " + saveString.substring(beginIndex, endIndex));
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		sb.append(", mother = " + saveString.substring(beginIndex, endIndex));
		String prefilter = saveString.substring(endIndex+1);
		if (!prefilter.equals("NO_PREFILTERING")){			
			sb.append(", with prefiltering: ");
			sb.append((new ComboFilter()).parseSaveString(prefilter));
		}
		return sb.toString();
	}

	public Filter loadCriterion(FilteringPanel filteringPanel, String saveString) throws Exception {
		int beginIndex = 0;
		int endIndex = saveString.indexOf("#");
		String loadChild = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		String loadFather = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		String loadMother = saveString.substring(beginIndex, endIndex);
		String prefilter = saveString.substring(endIndex+1);
		ComboFilter loadPreFiltering = null;
		if (!prefilter.equals("NO_PREFILTERING")){
			loadPreFiltering = (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, prefilter);
		}
		return new CombinedHeterozygousVariants(filteringPanel, loadChild, loadFather, loadMother, loadPreFiltering);
	}

	public boolean checkFieldCompatibility(Analysis analysis){
		if (!Field.variant_sample_id.hasAnalysis(analysis)) return false;
		if (!Field.chr.hasAnalysis(analysis)) return false;
		if (!Field.pos.hasAnalysis(analysis)) return false;
		if (!Field.length.hasAnalysis(analysis)) return false;
		if (!Field.reference.hasAnalysis(analysis)) return false;
		if (!Field.alternative.hasAnalysis(analysis)) return false;
		if (!Field.gene_symbol.hasAnalysis(analysis)) return false;
		if (!Field.sample.hasAnalysis(analysis)) return false;
		if (preFiltering != null && !preFiltering.checkFieldCompatibility(analysis)) return false;
		return true;
	}

	public boolean changeAnalysis(Analysis analysis){
		if (preFiltering != null) return preFiltering.changeAnalysis(analysis);
		return (checkFieldCompatibility(analysis));
	}

	public void editFilter(){
		CreateMagicFilter cmf = new CreateMagicFilter(filteringPanel, this);
		Tools.centerWindow(cmf, false);
		cmf.setVisible(true);
		CombinedHeterozygousVariants edited = (CombinedHeterozygousVariants)cmf.getCriterion();
		if(edited != null){			
			child = edited.getChild();		
			father = edited.getFather();		
			mother = edited.getMother();
			preFiltering = edited.getPreFiltering();		
			label.setToolTipText(toHtmlString());
		}
	}

	public int getNumberOfVariants() {
		return nVariants;
	}

	public Map<Integer,String> getResultIds(Set<String> autoSamples) throws Exception {
		nVariants = -1;
		Highlander.waitingPanel.start(true);
		Analysis analysis = Highlander.getCurrentAnalysis();

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [1/4]", true);
		Set<Integer> idsPreFilter = new HashSet<Integer>();
		if (preFiltering != null){
			idsPreFilter = preFiltering.getResultIds(getAllSamples()).keySet();
			if (idsPreFilter.isEmpty()){
				Highlander.waitingPanel.setProgressDone();
				Highlander.waitingPanel.stop();
				nVariants = 0;
				return new HashMap<Integer, String>();
			}
		}

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [2/4]", true);
		Map<String, Map<String, Map<String, Integer>>> map = new HashMap<String, Map<String,Map<String,Integer>>>();
		StringBuilder query = new StringBuilder();
		query.append(
				"SELECT "
						+ Field.variant_sample_id.getQuerySelectName(analysis, false)
						+ ", " + Field.chr.getQuerySelectName(analysis, false)
						+ ", " + Field.pos.getQuerySelectName(analysis, false)
						+ ", " + Field.length.getQuerySelectName(analysis, false)
						+ ", " + Field.reference.getQuerySelectName(analysis, false)
						+ ", " + Field.alternative.getQuerySelectName(analysis, false)
						+ ", " + Field.gene_symbol.getQuerySelectName(analysis, false)
						+ ", " + Field.sample.getQuerySelectName(analysis, false)
						+ " FROM " + analysis.getFromSampleAnnotations()
						+ analysis.getJoinStaticAnnotations()
						+ analysis.getJoinProjects()
						+ analysis.getJoinGeneAnnotations()
						+ "WHERE "
						+ Field.sample.getQueryWhereName(analysis, false) + " IN ('"+child+"','"+father+"','"+mother+"')");
		if (!idsPreFilter.isEmpty()) 
			query.append(" AND "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN ("+HighlanderDatabase.makeSqlList(idsPreFilter, Integer.class)+")");

		Tools.print("Submitting query to the database ("+getFilterType().getName()+" filter): ");
		System.out.println(query.toString());
		int progress = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString(), true)) {
			int count = res.getResultSetSize();
			Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [3/4]", (count == -1));
			if (count != -1) Highlander.waitingPanel.setProgressMaximum(count);
			while (res.next()){
				Highlander.waitingPanel.setProgressValue(progress++);
				String gene = res.getString(Field.gene_symbol.getName());
				String sample = res.getString(Field.sample.getName()).toUpperCase();
				int id = res.getInt(Field.variant_sample_id.getName());
				String uniqueVariant = res.getString(Field.chr.getName()) + "-" + res.getInt(Field.pos.getName()) + "-" + res.getInt(Field.length.getName()) + "-" + res.getString(Field.reference.getName()) + "-" + res.getString(Field.alternative.getName());
				if (!map.containsKey(gene)){
					map.put(gene, new HashMap<String, Map<String,Integer>>());
				}
				Map<String, Map<String,Integer>> geneMap = map.get(gene);
				if (!geneMap.containsKey(sample)){
					geneMap.put(sample, new HashMap<String, Integer>());
				}
				geneMap.get(sample).put(uniqueVariant, id);
			}
		}

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [4/4]", false);
		Highlander.waitingPanel.setProgressMaximum(map.size());
		progress = 0;
		Map<Integer,String> ids = new HashMap<Integer, String>();
		for (Map<String, Map<String,Integer>> geneMap : map.values()){
			Highlander.waitingPanel.setProgressValue(progress++);
			if (geneMap.size() == 3){
				Map<String, Integer> childMap = geneMap.get(child.toUpperCase());
				Map<String, Integer> fatherMap = geneMap.get(father.toUpperCase());
				Map<String, Integer> motherMap = geneMap.get(mother.toUpperCase());
				//Intersection of father/mother with the child (remove variants not found in the child)
				Set<String> fatherIntersectChild = new HashSet<String>(fatherMap.keySet());
				fatherIntersectChild.retainAll(childMap.keySet());
				fatherMap.keySet().retainAll(childMap.keySet());
				Set<String> motherIntersectChild = new HashSet<String>(motherMap.keySet());
				motherIntersectChild.retainAll(childMap.keySet());
				motherMap.keySet().retainAll(childMap.keySet());
				//Complement of father/mother intersections (remove variants found in the other parent)
				fatherMap.keySet().removeAll(motherIntersectChild);
				motherMap.keySet().removeAll(fatherIntersectChild);
				if (fatherMap.size() > 0 && motherMap.size() > 0){
					for (String key : fatherMap.keySet()){
						ids.put(fatherMap.get(key), key);
						ids.put(childMap.get(key), key);
					}
					for (String key : motherMap.keySet()){
						ids.put(motherMap.get(key), key);
						ids.put(childMap.get(key), key);
					}
				}
			}
		}

		Highlander.waitingPanel.setProgressDone();
		Highlander.waitingPanel.stop();
		nVariants = ids.size();
		return ids;
	}
}
