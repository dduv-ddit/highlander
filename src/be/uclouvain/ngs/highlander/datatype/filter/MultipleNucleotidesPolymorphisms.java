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
import java.util.LinkedHashSet;
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
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;

public class MultipleNucleotidesPolymorphisms extends MagicFilter {

	private Set<String> samples;

	private int nVariants = -1;

	public MultipleNucleotidesPolymorphisms(FilteringPanel filteringPanel, Set<String> samples, ComboFilter preFiltering){
		this.filteringPanel = filteringPanel;
		this.samples = samples;
		this.preFiltering = preFiltering;
		displayCriterionPanel();
	}

	public MultipleNucleotidesPolymorphisms(){
		samples = null;
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.MULTIPLE_NUCLEOTIDES_VARIANTS;
	}

	@Override
	public Set<String> getIncludedSamples(){
		return samples;
	}

	@Override
	public Set<String> getExcludedSamples(){
		return new TreeSet<String>();
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getFilterType().getName() + ": ");
		for (String sample : samples){
			sb.append(sample + ", ");
		}
		if (!samples.isEmpty()) sb.delete(sb.length()-2, sb.length());
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
		sb.append("<br>Sample set: ");
		for (String sample : samples){
			sb.append(sample + ", ");
		}
		if (!samples.isEmpty()) sb.delete(sb.length()-2, sb.length());
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
		for (String sample : samples){
			sb.append(sample);
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
		sb.append(getFilterType().getName() + ": ");
		boolean hasPrefilter = saveString.indexOf('µ') > 0;
		String prefilter = (hasPrefilter) ? saveString.substring(saveString.indexOf('µ')+1) : "NO_PREFILTERING";		
		String filter = (hasPrefilter) ? saveString.substring(0,saveString.indexOf('µ')) : saveString;
		for (String sample : filter.split("#")){
			sb.append(sample + ", ");
		}
		if (filter.split("#").length > 0) sb.delete(sb.length()-2, sb.length());
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
		Set<String> set = new LinkedHashSet<String>();
		for (String sample : filter.split("#")){
			set.add(sample);
		}		
		ComboFilter loadPreFiltering = null;
		if (!prefilter.equals("NO_PREFILTERING")){
			loadPreFiltering = (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, prefilter);
		}
		return new MultipleNucleotidesPolymorphisms(filteringPanel, set, loadPreFiltering);
	}

	@Override
	public boolean checkFieldCompatibility(Analysis analysis){
		if (!Field.variant_sample_id.hasAnalysis(analysis)) return false;
		if (!Field.chr.hasAnalysis(analysis)) return false;
		if (!Field.pos.hasAnalysis(analysis)) return false;
		if (!Field.length.hasAnalysis(analysis)) return false;
		if (!Field.reference.hasAnalysis(analysis)) return false;
		if (!Field.alternative.hasAnalysis(analysis)) return false;
		if (!Field.variant_type.hasAnalysis(analysis)) return false;
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
		MultipleNucleotidesPolymorphisms edited = (MultipleNucleotidesPolymorphisms)cmf.getCriterion();
		if(edited != null){			
			samples = edited.getIncludedSamples();		
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
		if (samples.isEmpty()) {
			nVariants = 0;
			return new HashMap<Integer, String>();
		}

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
		Map<String, Map<String, Map<Integer,Integer>>> map = new HashMap<String, Map<String,Map<Integer,Integer>>>();
		Map<Integer, String> idVariants = new HashMap<Integer, String>();

		StringBuilder query = new StringBuilder();
		query.append(
				"SELECT "
						+ Field.variant_sample_id.getQuerySelectName(analysis, false)
						+ ", " + Field.chr.getQuerySelectName(analysis, false)
						+ ", " + Field.pos.getQuerySelectName(analysis, false)
						+ ", " + Field.length.getQuerySelectName(analysis, false)
						+ ", " + Field.reference.getQuerySelectName(analysis, false)
						+ ", " + Field.alternative.getQuerySelectName(analysis, false)
						+ ", " + Field.sample.getQuerySelectName(analysis, false)
						+ " FROM " + analysis.getFromSampleAnnotations()
						+ analysis.getJoinStaticAnnotations()
						+ analysis.getJoinProjects()
						+ "WHERE "
						+ Field.sample.getQueryWhereName(analysis, false) + " IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+") "
						+ "AND " + Field.variant_type.getQueryWhereName(analysis, false) + " = '"+VariantType.SNV+"'"
				);
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
				int id = res.getInt(Field.variant_sample_id.getName());
				String uniqueVariant = res.getString(Field.chr.getName()) + "-" + res.getInt(Field.pos.getName()) + "-" + res.getInt(Field.length.getName()) + "-" + res.getString(Field.reference.getName()) + "-" + res.getString(Field.alternative.getName());
				String sample = res.getString(Field.sample.getName()).toUpperCase();
				String chr = res.getString(Field.chr.getName());
				int pos = res.getInt(Field.pos.getName());
				idVariants.put(id, uniqueVariant);
				if (!map.containsKey(sample)){
					map.put(sample, new HashMap<String,Map<Integer,Integer>>());
				}
				Map<String,Map<Integer,Integer>> sampleMap = map.get(sample);
				if (!sampleMap.containsKey(chr)){
					sampleMap.put(chr, new HashMap<Integer,Integer>());
				}
				sampleMap.get(chr).put(pos, id);
			}
		}

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [4/4]", false);
		Highlander.waitingPanel.setProgressMaximum(map.size()*24);
		progress = 0;
		Map<Integer,String> ids = new HashMap<Integer, String>();
		for (Map<String,Map<Integer,Integer>> sampleMap : map.values()){
			for (Map<Integer,Integer> chrMap : sampleMap.values()){
				Highlander.waitingPanel.setProgressValue(progress++);
				for (int pos : chrMap.keySet()){
					int next = pos+1;
					while (chrMap.containsKey(next)){
						next++;
					}
					if (next > pos+1){
						for (int i=pos ; i < next ; i++){
							ids.put(chrMap.get(i), idVariants.get(chrMap.get(i)));
						}
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
