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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateMagicFilter;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

import java.util.Set;

public class SampleSpecificVariants extends MagicFilter {

	private Map<List<String>, List<String>> samples;
	private Map<List<String>, LogicalOperator> toKeepOperator;
	private int nVariants = -1;

	public SampleSpecificVariants(FilteringPanel filteringPanel, Map<List<String>, List<String>> samples, Map<List<String>, LogicalOperator> toKeepOperator, ComboFilter preFiltering){
		this.filteringPanel = filteringPanel;
		this.samples = samples;
		this.toKeepOperator = toKeepOperator;
		this.preFiltering = preFiltering;
		displayCriterionPanel();
	}

	public SampleSpecificVariants(){
		samples = null;
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.SAMPLE_SPECIFIC_VARIANTS;
	}

	public Map<List<String>, List<String>> getFilterSamples(){
		return samples;
	}

	public Map<List<String>, LogicalOperator> getToKeepOperator(){
		return toKeepOperator;
	}

	@Override
	public Set<String> getIncludedSamples(){
		Set<String> set = new HashSet<String>();
		for (List<String> toKeep : samples.keySet()){
			set.addAll(toKeep);
		}
		return set;
	}

	@Override
	public Set<String> getExcludedSamples(){
		Set<String> set = new HashSet<String>();
		for (List<String> toExclude : samples.values()){
			set.addAll(toExclude);
		}
		return set;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getFilterType().getName() +": ");
		for (Entry<List<String>, List<String>> e : samples.entrySet()){
			sb.append("Exclude "+e.getValue()+" variants from "+((toKeepOperator.get(e.getKey())==LogicalOperator.OR)?"UNION of ":"INTERSECTION of ")+e.getKey() + " OR ");
		}
		if (!samples.isEmpty()) sb.delete(sb.length()-4, sb.length());
		if (preFiltering != null){			
			sb.append(", with prefiltering: " + preFiltering.toString());
		}
		return sb.toString();
	}

	@Override
	public String toHtmlString(){
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(getFilterType().getDescription().replace("\n", "<br>"));
		for (Entry<List<String>, List<String>> e : samples.entrySet()){
			sb.append("<br> * Exclude "+e.getValue()+" variants from "+((toKeepOperator.get(e.getKey())==LogicalOperator.OR)?"UNION of ":"INTERSECTION of ")+e.getKey());
		}
		if (preFiltering != null){
			sb.append("<br>Prefiltering: ");
			sb.append("<br>");
			sb.append(preFiltering.toHtmlString());
		}
		sb.append("</html>");
		return sb.toString();
	}

	@Override
	public boolean isFilterValid(){
		if (samples.isEmpty()){
			return false;
		}else if (preFiltering != null){
			return preFiltering.isFilterValid();
		}else{
			return true;
		}
	}

	@Override
	public List<String> getValidationProblems(){
		List<String> problems = new ArrayList<String>();
		if (preFiltering != null){
			problems.addAll(preFiltering.getValidationProblems());
		}
		if (samples.isEmpty()){
			problems.add("No sample has been selected for a Magic Filter of type " + getFilterType());
		}
		return problems;
	}

	@Override
	public String getSaveString(){
		StringBuilder sb = new StringBuilder();
		for (Entry<List<String>, List<String>> e : samples.entrySet()){
			for (String s : e.getKey()){
				sb.append(s);
				if (toKeepOperator.get(e.getKey()) == LogicalOperator.AND) sb.append("!");
				else sb.append("&");
			}
			if (sb.length() > 0) sb.delete(sb.length()-1, sb.length());
			sb.append("|");
			for (String s : e.getValue()){
				sb.append(s);
				sb.append("&");
			}
			if (sb.length() > 0) sb.delete(sb.length()-1, sb.length());
			sb.append("#");
		}
		if (sb.length() > 0) sb.delete(sb.length()-1, sb.length());
		sb.append("µ");
		if (preFiltering != null){
			sb.append(preFiltering.getSaveString());
		}else{
			sb.append("NO_PREFILTERING");
		}
		return sb.toString();
	}

	@Override
	public String parseSaveString(String saveString){
		StringBuilder sb = new StringBuilder();
		boolean hasPrefilter = saveString.indexOf('µ') > 0;
		String prefilter = (hasPrefilter) ? saveString.substring(saveString.indexOf('µ')+1) : "NO_PREFILTERING";		
		String filter = (hasPrefilter) ? saveString.substring(0,saveString.indexOf('µ')) : saveString;
		String[] entries = filter.split("#");
		sb.append(getFilterType().getName() +": ");
		for (String entry : entries){
			StringBuilder toKeepSb = new StringBuilder();
			StringBuilder toExcludeSb = new StringBuilder();
			String[] elems = entry.split("\\|");
			if (elems[0].contains("!")){
				for (String elem : elems[0].split("!")){
					toKeepSb.append(elem + " AND ");
				}
				toKeepSb.delete(sb.length()-5, sb.length());
			}else{
				for (String elem : elems[0].split("\\&")){
					toKeepSb.append(elem + " OR ");
				}
				toKeepSb.delete(sb.length()-4, sb.length());
			}
			for (String elem : elems[1].split("\\&")){
				toExcludeSb.append(elem + ", ");
			}
			toExcludeSb.delete(sb.length()-2, sb.length());
			sb.append("Exclude (");
			sb.append(toExcludeSb.toString());
			sb.append(") from (");
			sb.append(toKeepSb.toString());
			sb.append(") OR ");
		}		
		if (entries.length > 0) sb.delete(sb.length()-4, sb.length());
		if (!prefilter.equals("NO_PREFILTERING")){			
			sb.append(", with prefiltering: ");
			sb.append((new ComboFilter()).parseSaveString(prefilter));
		}
		return sb.toString();
	}

	@Override
	public Filter loadCriterion(FilteringPanel filteringPanel, String saveString) throws Exception {
		boolean hasPrefilter = saveString.indexOf('µ') > 0;
		String prefilter = (hasPrefilter) ? saveString.substring(saveString.indexOf('µ')+1) : "NO_PREFILTERING";		
		String filter = (hasPrefilter) ? saveString.substring(0,saveString.indexOf('µ')) : saveString;
		Map<List<String>, List<String>> map = new LinkedHashMap<List<String>, List<String>>();
		Map<List<String>, LogicalOperator> log = new LinkedHashMap<List<String>, LogicalOperator>();
		String[] entries = filter.split("#");
		for (String entry : entries){
			String[] elems = entry.split("\\|");
			List<String> a = new ArrayList<String>();
			if (elems[0].contains("!")){
				for (String elem : elems[0].split("!")){
					a.add(elem);
				}
			}else{
				for (String elem : elems[0].split("\\&")){
					a.add(elem);
				}				
			}
			if (elems[0].contains("!")){
				log.put(a, LogicalOperator.AND);
			}else{
				log.put(a, LogicalOperator.OR);
			}
			List<String> b = new ArrayList<String>();
			for (String elem : elems[1].split("\\&")){
				b.add(elem);
			}
			map.put(a, b);
		}		
		ComboFilter loadPreFiltering = null;
		if (!prefilter.equals("NO_PREFILTERING")){
			loadPreFiltering = (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, prefilter);
		}
		return new SampleSpecificVariants(filteringPanel, map, log, loadPreFiltering);
	}

	@Override
	public boolean checkFieldCompatibility(Analysis analysis){
		if (!Field.variant_sample_id.hasAnalysis(analysis)) return false;
		if (!Field.chr.hasAnalysis(analysis)) return false;
		if (!Field.pos.hasAnalysis(analysis)) return false;
		if (!Field.length.hasAnalysis(analysis)) return false;
		if (!Field.reference.hasAnalysis(analysis)) return false;
		if (!Field.alternative.hasAnalysis(analysis)) return false;
		if (!Field.sample.hasAnalysis(analysis)) return false;
		if (preFiltering != null && !preFiltering.checkFieldCompatibility(analysis)) return false;
		return true;
	}

	@Override
	public boolean changeAnalysis(Analysis analysis){
		if (preFiltering != null) return preFiltering.changeAnalysis(analysis);
		return (checkFieldCompatibility(analysis));
	}

	@Override
	public void editFilter(){
		CreateMagicFilter cmf = new CreateMagicFilter(filteringPanel, this);
		Tools.centerWindow(cmf, false);
		cmf.setVisible(true);
		SampleSpecificVariants edited = (SampleSpecificVariants)cmf.getCriterion();
		if(edited != null){			
			samples = edited.getFilterSamples();
			toKeepOperator = edited.getToKeepOperator();
			preFiltering = edited.getPreFiltering();		
			label.setToolTipText(toHtmlString());
		}
	}

	@Override
	public int getNumberOfVariants() {
		return nVariants;
	}

	@Override
	public Map<Integer,String> getResultIds(Set<String> autoSamples) throws Exception {
		nVariants = -1;
		Highlander.waitingPanel.start(true);
		Analysis analysis = Highlander.getCurrentAnalysis();

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [1/2]", true);
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

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [2/2]", false);
		Highlander.waitingPanel.setProgressMaximum(samples.size()*4);
		int progress = 0;
		Map<Integer,String> ids = new HashMap<Integer, String>();
		for (Entry<List<String>, List<String>> e : samples.entrySet()){
			Highlander.waitingPanel.setProgressValue(progress++);
			/**
			 * Done as one request : join of 2 tables, seems to be reaaaally slow 
			ResultSet res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT KEEP.id FROM "+analysis+" as EXCL JOIN "+analysis+" as KEEP " +
					"on (EXCL.chr = KEEP.chr AND EXCL.pos = KEEP.pos AND EXCL.reference = KEEP.reference AND EXCL.alternative = KEEP.alternative AND EXCL.variant_type = KEEP.variant_type) " +
					"WHERE EXCL.sample IN ("+HighlanderDatabase.makeSqlList(e.getValue())+") AND KEEP.sample IN ("+HighlanderDatabase.makeSqlList(e.getKey())+")");
			Set<Integer> commons = new HashSet<Integer>();
			while (res.next()){
				commons.add(res.getInt(1));
			}
			}
			 */
			if (!e.getKey().isEmpty()){
				StringBuilder query = new StringBuilder();
				query.append(
						"SELECT "
								+ Field.variant_sample_id.getQuerySelectName(analysis, false)
								+ ", " + Field.chr.getQuerySelectName(analysis, false)
								+ ", " + Field.pos.getQuerySelectName(analysis, false)
								+ ", " + Field.length.getQuerySelectName(analysis, false)
								+ ", " + Field.reference.getQuerySelectName(analysis, false)
								+ ", " + Field.alternative.getQuerySelectName(analysis, false)
								+ " FROM " + analysis.getFromSampleAnnotations()
								+ analysis.getJoinStaticAnnotations()
								+ analysis.getJoinProjects()
								+ "WHERE "
								+ Field.sample.getQueryWhereName(analysis, false) + " IN ("+HighlanderDatabase.makeSqlList(e.getKey(), String.class)+")");
				if (!idsPreFilter.isEmpty()) 
						query.append(" AND "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN ("+HighlanderDatabase.makeSqlList(idsPreFilter, Integer.class)+")");

				Tools.print("Submitting query to the database ("+getFilterType().getName()+" filter - retreiving to keep variants): ");
				System.out.println(query.toString());
				Map<String,List<Integer>> toKeep = new HashMap<String, List<Integer>>();
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString(), true)) {
					while (res.next()){
						String uniqueVariant = res.getString(Field.chr.getName()) + "-" + res.getInt(Field.pos.getName()) + "-" + res.getInt(Field.length.getName()) + "-" + res.getString(Field.reference.getName()) + "-" + res.getString(Field.alternative.getName());
						if (!toKeep.containsKey(uniqueVariant)){
							toKeep.put(uniqueVariant, new ArrayList<Integer>());
						}				
						toKeep.get(uniqueVariant).add(res.getInt(Field.variant_sample_id.getName()));
					}
				}
				if (toKeepOperator.get(e.getKey()) == LogicalOperator.AND){
					for(Iterator<String> it = toKeep.keySet().iterator() ; it.hasNext() ; ){
						if (toKeep.get(it.next()).size() < e.getKey().size()) it.remove();
					}
				}
				Highlander.waitingPanel.setProgressValue(progress++);

				query = new StringBuilder();
				query.append(
						"SELECT "
								+ Field.chr.getQuerySelectName(analysis, false)
								+ ", " + Field.pos.getQuerySelectName(analysis, false)
								+ ", " + Field.length.getQuerySelectName(analysis, false)
								+ ", " + Field.reference.getQuerySelectName(analysis, false)
								+ ", " + Field.alternative.getQuerySelectName(analysis, false)
								+ " FROM " + analysis.getFromSampleAnnotations()
								+ analysis.getJoinStaticAnnotations()
								+ analysis.getJoinProjects()
								+ "WHERE "
								+ Field.sample.getQueryWhereName(analysis, false) + " IN ("+HighlanderDatabase.makeSqlList(e.getValue(), String.class)+")");
				if (!idsPreFilter.isEmpty()) 
						query.append(" AND "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN ("+HighlanderDatabase.makeSqlList(idsPreFilter, Integer.class)+")");
				
				Tools.print("Submitting query to the database ("+getFilterType().getName()+" filter - retreiving to remove variants): ");
				System.out.println(query.toString());
				Set<String> toRemove = new HashSet<String>();
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString(), true)) {
					while (res.next()){
						String uniqueVariant = res.getString(Field.chr.getName()) + "-" + res.getInt(Field.pos.getName()) + "-" + res.getInt(Field.length.getName()) + "-" + res.getString(Field.reference.getName()) + "-" + res.getString(Field.alternative.getName());
						toRemove.add(uniqueVariant);
					}
				}
				Highlander.waitingPanel.setProgressValue(progress++);			
				toKeep.keySet().removeAll(toRemove);
				for (String variant : toKeep.keySet()){
					for (int id : toKeep.get(variant)){
						ids.put(id, variant);
					}
				}
				Highlander.waitingPanel.setProgressValue(progress++);
			}
		}
		Highlander.waitingPanel.setProgressDone();
		Highlander.waitingPanel.stop();
		nVariants = ids.size();
		return ids;
	}

}
