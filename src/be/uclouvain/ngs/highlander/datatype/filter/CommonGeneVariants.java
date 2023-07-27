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

public class CommonGeneVariants extends MagicFilter {

	public enum NumVariantThreshold {
		AT_MOST("at most","0"),  
		AT_LEAST("at least","1"), 
		EXACTLY("exactly","2");
		private final String text;
		private final String code;
		NumVariantThreshold(String text, String code){this.text = text;this.code = code;}
		public String getText(){return text;}
		public String getCode() {return code;}
		public static NumVariantThreshold getNumVariantThresholdFromCode(String code) {
			for (NumVariantThreshold n : values()){
				if (n.getCode().equals(code)) return n;
			}
			return AT_LEAST;
		}
		public static NumVariantThreshold getNumVariantThresholdFromText(String text) {
			for (NumVariantThreshold n : values()){
				if (n.getText().equals(text)) return n;
			}
			return AT_LEAST;
		}
		public static String[] getTextValues(){
			NumVariantThreshold[] vals = values(); 
			String[] res = new String[vals.length];
			for (int i=0 ; i < vals.length ; i++){
				res[i] = vals[i].getText();
			}
			return res;
		}
	}

	private Set<String> samples;
	int minCommon = -1;
	int minMaxVariants = -1;
	NumVariantThreshold numVariantsThreshold = NumVariantThreshold.AT_LEAST;

	private int nVariants = -1;

	public CommonGeneVariants(FilteringPanel filteringPanel, Set<String> samples, int minCommon, int minVariants, NumVariantThreshold numVariantsThreshold, ComboFilter preFiltering){
		this.filteringPanel = filteringPanel;
		this.samples = samples;
		this.minCommon = minCommon;
		this.minMaxVariants = minVariants;
		this.numVariantsThreshold = numVariantsThreshold;
		this.preFiltering = preFiltering;
		displayCriterionPanel();
	}

	public CommonGeneVariants(){
		samples = null;
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.COMMON_GENE_VARIANTS;
	}

	@Override
	public Set<String> getIncludedSamples(){
		return samples;
	}

	@Override
	public Set<String> getExcludedSamples(){
		return new TreeSet<String>();
	}

	public int getMinCommon() {
		return minCommon;
	}

	public int getMinMaxVariants() {
		return minMaxVariants;
	}

	public NumVariantThreshold getNumVariantsThreshold(){
		return numVariantsThreshold;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getFilterType().getName() + " (common in min "+minCommon+" samples, with "+(numVariantsThreshold.getText())+" "+minMaxVariants+" variants in each sample): ");
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
		sb.append("<br>Variants must be common in at least "+minCommon+" samples.");
		sb.append("<br>Genes must have "+(numVariantsThreshold.getText())+" "+minMaxVariants+" variants in each sample.");
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
		if (preFiltering != null){
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
		sb.append(minCommon);
		sb.append("#");
		sb.append(minMaxVariants);
		sb.append("#");
		sb.append(numVariantsThreshold.getCode());
		sb.append("#");
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
		int beginIndex = 0;
		int endIndex = saveString.indexOf("#");
		String samplesToParse = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		String commonInMin = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		String minVariantsInGene = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		String numVariantsThreshold = NumVariantThreshold.getNumVariantThresholdFromCode(saveString.substring(beginIndex, endIndex)).getText();
		String prefilter = saveString.substring(endIndex+1);
		sb.append(getFilterType().getName() + " (common in min "+commonInMin+" samples, with "+numVariantsThreshold+" "+minVariantsInGene+" variants in each sample): ");
		for (String sample :samplesToParse.split("\\|")){
			sb.append(sample + ", ");
		}
		if (samplesToParse.length() > 0) sb.delete(sb.length()-2, sb.length());
		if (!prefilter.equals("NO_PREFILTERING")){			
			sb.append(", with prefiltering: ");
			sb.append((new ComboFilter()).parseSaveString(prefilter));
		}
		return sb.toString();
	}

	@Override
	public Filter loadCriterion(FilteringPanel filteringPanel, String saveString) throws Exception {
		Set<String> set = new LinkedHashSet<String>();
		int beginIndex = 0;
		int endIndex = saveString.indexOf("#");
		String samplesToParse = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		String commonInMin = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		String minVariantsInGene = saveString.substring(beginIndex, endIndex);
		beginIndex = endIndex+1;
		endIndex = saveString.indexOf("#", beginIndex);
		NumVariantThreshold numVariantsThreshold = NumVariantThreshold.getNumVariantThresholdFromCode(saveString.substring(beginIndex, endIndex));
		String prefilter = saveString.substring(endIndex+1);
		for (String sample : samplesToParse.split("\\|")){
			set.add(sample);
		}		
		ComboFilter loadPreFiltering = null;
		if (!prefilter.equals("NO_PREFILTERING")){
			loadPreFiltering = (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, prefilter);
		}
		return new CommonGeneVariants(filteringPanel, set, Integer.parseInt(commonInMin), Integer.parseInt(minVariantsInGene), numVariantsThreshold, loadPreFiltering);
	}

	@Override
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
		CommonGeneVariants edited = (CommonGeneVariants)cmf.getCriterion();
		if(edited != null){			
			samples = edited.getIncludedSamples();		
			minCommon = edited.getMinCommon();
			minMaxVariants = edited.getMinMaxVariants();
			numVariantsThreshold = edited.getNumVariantsThreshold();
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
		Map<String, Map<String,Map<Integer,String>>> map = new HashMap<String, Map<String,Map<Integer,String>>>();
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
						+ Field.sample.getQueryWhereName(analysis, false) + " IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+")");
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
					map.put(gene, new HashMap<String, Map<Integer,String>>());
				}
				Map<String, Map<Integer,String>> genemap = map.get(gene);
				if (!genemap.containsKey(sample)){
					genemap.put(sample, new HashMap<Integer,String>());
				}
				genemap.get(sample).put(id, uniqueVariant);
			}
		}

		Highlander.waitingPanel.setProgressString("Magic filter '" + getFilterType().getName() +"' [4/4]", false);
		Highlander.waitingPanel.setProgressMaximum(map.size());
		progress = 0;
		Map<Integer,String> ids = new HashMap<Integer, String>();
		if (minCommon == -1) minCommon = samples.size();
		for (Map<String, Map<Integer,String>> genemap : map.values()){
			Highlander.waitingPanel.setProgressValue(progress++);				
			if (genemap.size() >= minCommon){
				switch(numVariantsThreshold){
				case AT_LEAST:
					int minSize = Integer.MAX_VALUE;
					for (Map<Integer,String> list : genemap.values()){
						if (list.size() < minSize) minSize = list.size();
					}
					if (minSize >= minMaxVariants){
						for (Map<Integer,String> list : genemap.values()){
							ids.putAll(list);
						}
					}
					break;
				case AT_MOST:
					int maxSize = 0;
					for (Map<Integer,String> list : genemap.values()){
						if (list.size() > maxSize) maxSize = list.size();
					}
					if (maxSize <= minMaxVariants){
						for (Map<Integer,String> list : genemap.values()){
							ids.putAll(list);
						}
					}					
					break;
				case EXACTLY:
					int listSize = genemap.values().iterator().next().size();
					for (Map<Integer,String> list : genemap.values()){
						if (list.size() != listSize) {
							listSize = -1;
							break;
						}
					}
					if (listSize == minMaxVariants){
						for (Map<Integer,String> list : genemap.values()){
							ids.putAll(list);
						}
					}
					break;
				}
			}
		}

		Highlander.waitingPanel.setProgressDone();
		Highlander.waitingPanel.stop();
		nVariants = ids.size();
		return ids;
	}
}
