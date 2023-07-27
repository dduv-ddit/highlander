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
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter.ComparisonOperator;

public class VariantsCommonToSamples extends MagicFilter {

	public class VCSCriterion {
		public String sample;

		public boolean useZigosity = false;
		public String zygosity;

		public boolean useAllelicDepthRef;
		public ComparisonOperator opAllelicDepthRef;
		public int allelic_depth_ref;

		public boolean useAllelicDepthAlt;
		public ComparisonOperator opAllelicDepthAlt;
		public int allelic_depth_alt;

		public boolean useAllelicDepthProportionRef;
		public ComparisonOperator opAllelicDepthProportionRef;
		public double allelic_depth_proportion_ref;

		public boolean useAllelicDepthProportionAlt;
		public ComparisonOperator opAllelicDepthProportionAlt;
		public double allelic_depth_proportion_alt;

		public VCSCriterion(){

		}

		public VCSCriterion(String saveString){
			String[] parts = saveString.split("\\|");		
			sample = parts[0];
			useZigosity = parts[1].equals("1");
			if(useZigosity) zygosity = parts[2];
			useAllelicDepthRef = parts[3].equals("1");
			if(useAllelicDepthRef) opAllelicDepthRef = ComparisonOperator.valueOf(parts[4]);
			if(useAllelicDepthRef) allelic_depth_ref = Integer.parseInt(parts[5]);
			useAllelicDepthAlt = parts[6].equals("1");
			if(useAllelicDepthAlt) opAllelicDepthAlt = ComparisonOperator.valueOf(parts[7]);
			if(useAllelicDepthAlt) allelic_depth_alt = Integer.parseInt(parts[8]);
			useAllelicDepthProportionRef = parts[9].equals("1");
			if(useAllelicDepthProportionRef) opAllelicDepthProportionRef = ComparisonOperator.valueOf(parts[10]);
			if(useAllelicDepthProportionRef) allelic_depth_proportion_ref = Double.parseDouble(parts[11]);
			useAllelicDepthProportionAlt = parts[12].equals("1");
			if(useAllelicDepthProportionAlt) opAllelicDepthProportionAlt = ComparisonOperator.valueOf(parts[13]);
			if(useAllelicDepthProportionAlt) allelic_depth_proportion_alt = Double.parseDouble(parts[14]);
		}

		public boolean hasCriteria(){
			return (useZigosity || useAllelicDepthRef || useAllelicDepthAlt || useAllelicDepthProportionRef || useAllelicDepthProportionAlt);
		}

		public String getCriteriaString(boolean html){
			StringBuilder sb = new StringBuilder();
			if (hasCriteria()){
				if (useZigosity) sb.append(zygosity.toString() + ", ");
				if (useAllelicDepthRef) sb.append("allelic_depth_ref " + ((html)?opAllelicDepthRef.getHtml():opAllelicDepthRef.getUnicode()) + " " + allelic_depth_ref + ", ");
				if (useAllelicDepthAlt) sb.append("allelic_depth_alt " + ((html)?opAllelicDepthAlt.getHtml():opAllelicDepthAlt.getUnicode()) + " " + allelic_depth_alt + ", ");
				if (useAllelicDepthProportionRef) sb.append("allelic_depth_proportion_ref " + ((html)?opAllelicDepthProportionRef.getHtml():opAllelicDepthProportionRef.getUnicode()) + " " + allelic_depth_proportion_ref + ", ");
				if (useAllelicDepthProportionAlt) sb.append("allelic_depth_proportion_alt " + ((html)?opAllelicDepthProportionAlt.getHtml():opAllelicDepthProportionAlt.getUnicode()) + " " + allelic_depth_proportion_alt + ", ");
				sb.delete(sb.length()-2, sb.length());
			}
			return sb.toString();
		}
	}

	private List<VCSCriterion> criteria;

	private int nVariants = -1;

	public VariantsCommonToSamples(FilteringPanel filteringPanel, List<VCSCriterion> criteria, ComboFilter preFiltering){
		this.filteringPanel = filteringPanel;
		this.criteria = criteria;
		this.preFiltering = preFiltering;
		displayCriterionPanel();
	}

	public VariantsCommonToSamples(){
		criteria = null;
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.VARIANTS_COMMON_TO_SAMPLES;
	}

	public List<VCSCriterion> getCriteria(){
		return criteria;
	}

	@Override
	public Set<String> getIncludedSamples(){
		Set<String> set = new HashSet<String>();
		for (VCSCriterion crit : criteria){
			set.add(crit.sample);
		}
		return set;
	}

	@Override
	public Set<String> getExcludedSamples(){
		return new TreeSet<String>();
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getFilterType().getName() + ": ");
		for (VCSCriterion crit : criteria){
			sb.append(crit.sample);
			if (crit.hasCriteria()) sb.append(" ("+crit.getCriteriaString(false) + ")");
			sb.append(" AND ");
		}
		if (!criteria.isEmpty()) sb.delete(sb.length()-5, sb.length());
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
		for (VCSCriterion crit : criteria){
			sb.append("<br>"+crit.sample);
			if (crit.hasCriteria()) sb.append(" ("+crit.getCriteriaString(true) + ")");
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
		if (criteria.isEmpty()){
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
		if (criteria.isEmpty()){
			problems.add("No criterion has been defined for a Magic Filter of type " + getFilterType());
		}
		return problems;
	}

	@Override
	public String getSaveString(){
		StringBuilder sb = new StringBuilder();
		for (VCSCriterion crit : criteria){
			sb.append(crit.sample);
			sb.append("|");
			sb.append(crit.useZigosity?"1":"0");
			sb.append("|");
			sb.append(crit.useZigosity?crit.zygosity:"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthRef?"1":"0");
			sb.append("|");
			sb.append(crit.useAllelicDepthRef?crit.opAllelicDepthRef.toString():"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthRef?crit.allelic_depth_ref:"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthAlt?"1":"0");
			sb.append("|");
			sb.append(crit.useAllelicDepthAlt?crit.opAllelicDepthAlt.toString():"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthAlt?crit.allelic_depth_alt:"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthProportionRef?"1":"0");
			sb.append("|");
			sb.append(crit.useAllelicDepthProportionRef?crit.opAllelicDepthProportionRef.toString():"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthProportionRef?crit.allelic_depth_proportion_ref:"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthProportionAlt?"1":"0");
			sb.append("|");
			sb.append(crit.useAllelicDepthProportionAlt?crit.opAllelicDepthProportionAlt.toString():"-");
			sb.append("|");
			sb.append(crit.useAllelicDepthProportionAlt?crit.allelic_depth_proportion_alt:"-");
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
		String[] parts = filter.split("#");		
		for (String part : parts){
			VCSCriterion crit = new VCSCriterion(part);			
			sb.append(crit.sample);
			if (crit.hasCriteria()) sb.append(" ("+crit.getCriteriaString(false) + ")");
			sb.append(" AND ");
		}	
		if (parts.length > 0) sb.delete(sb.length()-5, sb.length());
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
		List<VCSCriterion> list = new ArrayList<VariantsCommonToSamples.VCSCriterion>();
		String[] parts = filter.split("#");
		for (String part : parts){
			list.add(new VCSCriterion(part));
		}		
		ComboFilter loadPreFiltering = null;
		if (!prefilter.equals("NO_PREFILTERING")){
			loadPreFiltering = (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, prefilter);
		}
		return new VariantsCommonToSamples(filteringPanel, list, loadPreFiltering);
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
		for (VCSCriterion crit : criteria){
			if (crit.useZigosity && !Field.zygosity.hasAnalysis(analysis)) return false;
			if (crit.useAllelicDepthRef && !Field.allelic_depth_ref.hasAnalysis(analysis)) return false;
			if (crit.useAllelicDepthAlt && !Field.allelic_depth_alt.hasAnalysis(analysis)) return false;
			if (crit.useAllelicDepthProportionRef && !Field.allelic_depth_proportion_ref.hasAnalysis(analysis)) return false;
			if (crit.useAllelicDepthProportionAlt && !Field.allelic_depth_proportion_alt.hasAnalysis(analysis)) return false;
		}
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
		VariantsCommonToSamples edited = (VariantsCommonToSamples)cmf.getCriterion();
		if(edited != null){			
			criteria = edited.getCriteria();		
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
		Highlander.waitingPanel.setProgressMaximum(criteria.size()+2);
		Map<String, Map<String,Integer>> map = new HashMap<String, Map<String,Integer>>();
		int progress = 0;
		for (VCSCriterion crit : criteria){
			Highlander.waitingPanel.setProgressValue(progress++);
			
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
							+ Field.sample.getQueryWhereName(analysis, false) + " = '"+crit.sample+"'");
			if (crit.useZigosity && !crit.zygosity.equals("Any")) 
				query.append(" AND "+Field.zygosity.getQueryWhereName(analysis, false)+" = '"+crit.zygosity+"'");
			if (crit.useAllelicDepthRef) 
				query.append(" AND "+Field.allelic_depth_ref.getQueryWhereName(analysis, false)+" "+crit.opAllelicDepthRef.getSql()+" "+crit.allelic_depth_ref);
			if (crit.useAllelicDepthAlt) 
				query.append(" AND "+Field.allelic_depth_alt.getQueryWhereName(analysis, false)+" "+crit.opAllelicDepthAlt.getSql()+" "+crit.allelic_depth_alt);
			if (crit.useAllelicDepthProportionRef) 
				query.append(" AND "+Field.allelic_depth_proportion_ref.getQueryWhereName(analysis, false)+" "+crit.opAllelicDepthProportionRef.getSql()+" "+crit.allelic_depth_proportion_ref);
			if (crit.useAllelicDepthProportionAlt) 
				query.append(" AND "+Field.allelic_depth_proportion_alt.getQueryWhereName(analysis, false)+" "+crit.opAllelicDepthProportionAlt.getSql()+" "+crit.allelic_depth_proportion_alt);
			if (!idsPreFilter.isEmpty()) 
					query.append(" AND "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN ("+HighlanderDatabase.makeSqlList(idsPreFilter, Integer.class)+")");

			Tools.print("Submitting query to the database ("+getFilterType().getName()+" filter): ");
			System.out.println(query.toString());
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString(), true)) {
				while (res.next()){
					int id = res.getInt(Field.variant_sample_id.getName());
					String uniqueVariant = res.getString(Field.chr.getName()) + "-" + res.getInt(Field.pos.getName()) + "-" + res.getInt(Field.length.getName()) + "-" + res.getString(Field.reference.getName()) + "-" + res.getString(Field.alternative.getName());
					if (!map.containsKey(uniqueVariant)){
						map.put(uniqueVariant, new HashMap<String,Integer>());
					}
					map.get(uniqueVariant).put(crit.sample, id);
				}
			}
		}
		Highlander.waitingPanel.setProgressValue(progress++);
		Map<Integer,String> ids = new HashMap<Integer, String>();
		for (String variant : map.keySet()){
			Map<String,Integer> variants = map.get(variant);
			if (variants.size() == criteria.size()){
				for (int id : variants.values()){
					ids.put(id, variant);
				}
			}
		}
		Highlander.waitingPanel.setProgressDone();
		Highlander.waitingPanel.stop();
		nVariants = ids.size();
		return ids;
	}
}
