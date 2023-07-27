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
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter.ComparisonOperator;

public class PathologyFrequency extends MagicFilter {

	private Set<String> samples;
	private String pathology;
	private Field field;
	private ComparisonOperator compop;
	private  String value;
	
	private int nVariants = -1;

	public PathologyFrequency(FilteringPanel filteringPanel, Set<String> samples, String pathology, Field field, ComparisonOperator compop, String value, ComboFilter preFiltering){
		this.filteringPanel = filteringPanel;
		this.samples = samples;
		this.pathology = pathology;
		this.field = field;
		this.compop = compop;
		this.value = value;
		this.preFiltering = preFiltering;
		displayCriterionPanel();
	}

	public PathologyFrequency(){
		samples = null;
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.PATHOLOGY_FREQUENCY;
	}

	@Override
	public Set<String> getIncludedSamples(){
		return samples;
	}

	public String getPathology() {
		return pathology;
	}
	
	public Field getField() {
		return field;
	}
	
	public ComparisonOperator getComparisonOperator() {
		return compop;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public Set<String> getExcludedSamples(){
		return new TreeSet<String>();
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getFilterType().getName() + " filter on "+pathology+"_"+field.getName()+" "+compop.getUnicode()+" "+value+" for samples: ");
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
		sb.append("<br>Filter on: "+pathology+"_"+field.getName()+" "+compop.getHtml()+" "+value);
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
			sb.append("|");
		}
		if (sb.length() > 0) sb.delete(sb.length()-1, sb.length());
		sb.append("#");
		sb.append(pathology);
		sb.append("#");
		sb.append(field.getName());
		sb.append("#");
		sb.append(compop.toString());
		sb.append("#");
		sb.append(value);
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
		int beginIndex = 0;
		int endIndex = filter.indexOf("#");
		String pathology = filter.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = filter.indexOf("#", beginIndex);
		String field = filter.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = filter.indexOf("#", beginIndex);
		ComparisonOperator compop = ComparisonOperator.valueOf(filter.substring(beginIndex, endIndex));
		beginIndex = endIndex+1;
		endIndex = filter.indexOf("#", beginIndex);
		String value = filter.substring(beginIndex, endIndex);
		sb.append(getFilterType().getName() + " filter on "+pathology+"_"+field+" "+compop.getUnicode()+" "+value+" for samples: ");
		for (String sample : filter.split("\\|")){
			sb.append(sample + ", ");
		}
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
		int beginIndex = 0;
		int endIndex = filter.indexOf("#");
		Set<String> set = new LinkedHashSet<String>();
		for (String sample : filter.substring(beginIndex, endIndex).split("\\|")){
			set.add(sample);
		}		
		beginIndex = endIndex+1;
		endIndex = filter.indexOf("#", beginIndex);
		String pathology = filter.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = filter.indexOf("#", beginIndex);
		Field field =  Field.getField(filter.substring(beginIndex, endIndex));
		beginIndex = endIndex+1;
		endIndex = filter.indexOf("#", beginIndex);
		ComparisonOperator compop = ComparisonOperator.valueOf(filter.substring(beginIndex, endIndex));
		beginIndex = endIndex+1;
		String value = filter.substring(beginIndex);
		ComboFilter loadPreFiltering = null;
		if (!prefilter.equals("NO_PREFILTERING")){
			loadPreFiltering = (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, prefilter);
		}
		return new PathologyFrequency(filteringPanel, set, pathology, field, compop, value, loadPreFiltering);
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
		PathologyFrequency edited = (PathologyFrequency)cmf.getCriterion();
		if(edited != null){			
			samples = edited.getIncludedSamples();		
			pathology = edited.getPathology();
			field = edited.getField();
			compop = edited.getComparisonOperator();
			value = edited.getValue();
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

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [1/3]", true);
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

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [2/3]", true);
		Map<Integer, String> ids = new HashMap<Integer, String>();

		StringBuilder query = new StringBuilder();
		query.append(
				"SELECT "
						+ Field.variant_sample_id.getQuerySelectName(analysis, false)
						+ ", " + Field.chr.getQuerySelectName(analysis, true)
						+ ", " + Field.pos.getQuerySelectName(analysis, true)
						+ ", " + Field.length.getQuerySelectName(analysis, true)
						+ ", " + Field.reference.getQuerySelectName(analysis, true)
						+ ", " + Field.alternative.getQuerySelectName(analysis, true)
						+ ", IFNULL(`" + field.getName() + "`,0) as `" + field.getName() + "`"
						+ " FROM " + analysis.getFromSampleAnnotations()
						+ analysis.getJoinStaticAnnotations()
						+ analysis.getJoinProjects()
						+ analysis.getJoinPathologies()
						/*
						 * Variants with a frequency of zero in a pathology have no record for that pathology in analysis.getTableAlleleFrequenciesPerPathology()
						 * So we need to first do a join with all pathologies
						 * then a left join with analysis.getTableAlleleFrequenciesPerPathology(), and replacing null values by zero for the filter to work
						 */ 
						+ "INNER JOIN `"+analysis.getTablePathologies()+"` as `FreqPatho` " 
						+ "LEFT JOIN `"+analysis.getTableAlleleFrequenciesPerPathology()+"` "
								+ "ON `FreqPatho`.`pathology_id` = `"+analysis.getTableAlleleFrequenciesPerPathology()+"`.`pathology_id` "
								+ "AND `"+analysis.getTableAlleleFrequenciesPerPathology()+"`.`pos` = `"+analysis.getTableSampleAnnotations()+"`.`pos` "
								+ "AND `"+analysis.getTableAlleleFrequenciesPerPathology()+"`.`alternative` = `"+analysis.getTableSampleAnnotations()+"`.`alternative` "
								+ "AND `"+analysis.getTableAlleleFrequenciesPerPathology()+"`.`reference` = `"+analysis.getTableSampleAnnotations()+"`.`reference` "
								+ "AND `"+analysis.getTableAlleleFrequenciesPerPathology()+"`.`chr` = `"+analysis.getTableSampleAnnotations()+"`.`chr` "
								+ "AND `"+analysis.getTableAlleleFrequenciesPerPathology()+"`.`length` = `"+analysis.getTableSampleAnnotations()+"`.`length` "
						+ "WHERE "
						+ Field.sample.getQueryWhereName(analysis, false) + " IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+") "
						+ "AND `FreqPatho`.`pathology` = '"+pathology+"' "
						+ "AND IFNULL(`"+field.getName()+"`,0) "+compop.getSql()+" " + value
						);
		if (!idsPreFilter.isEmpty()) 
				query.append(" AND "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN ("+HighlanderDatabase.makeSqlList(idsPreFilter, Integer.class)+")");

		Tools.print("Submitting query to the database ("+getFilterType().getName()+" filter): ");
		System.out.println(query.toString());
		int progress = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString(), true)) {
			int count = res.getResultSetSize();
			Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [3/3]", (count == -1));
			if (count != -1) Highlander.waitingPanel.setProgressMaximum(count);
			while (res.next()){
				Highlander.waitingPanel.setProgressValue(progress++);
				int id = res.getInt(Field.variant_sample_id.getName());
				String uniqueVariant = res.getString(Field.chr.getName()) + "-" + res.getInt(Field.pos.getName()) + "-" + res.getInt(Field.length.getName()) + "-" + res.getString(Field.reference.getName()) + "-" + res.getString(Field.alternative.getName());
				ids.put(id, uniqueVariant);
			}
		}

		Highlander.waitingPanel.setProgressDone();
		Highlander.waitingPanel.stop();
		nVariants = ids.size();
		return ids;
	}
}
