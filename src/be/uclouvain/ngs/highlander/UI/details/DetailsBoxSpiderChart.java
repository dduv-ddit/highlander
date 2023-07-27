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

package be.uclouvain.ngs.highlander.UI.details;

import java.awt.BorderLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JProgressBar;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.charts.SpiderChart;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.Tag;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

/**
 * Spider chart displaying ranked scores like impact predictions or conservation
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxSpiderChart extends DetailsBox {

	protected DetailsPanel mainPanel;
	protected boolean detailsLoaded = false;

	protected List<Integer> variantIds;
	
	private Tag rankScoreType;

	public DetailsBoxSpiderChart(int variantId, DetailsPanel mainPanel, Tag rankScoreType){
		this(Arrays.asList(variantId), mainPanel, rankScoreType);
	}

	public DetailsBoxSpiderChart(List<Integer> variantIds, DetailsPanel mainPanel, Tag rankScoreType){
		this.variantIds = variantIds;
		this.mainPanel = mainPanel;
		this.rankScoreType = rankScoreType;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	@Override
	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	@Override
	public String getTitle(){
		switch(rankScoreType) {
		case IMPACT_RANKSCORE:
			return "Predictions spider chart";
		case CONSERVATION_RANKSCORE:
			return "Conservation spider chart";
		default :
			return "Chart not supported";
		}
	}

	@Override
	public Palette getColor() {
		return Field.snpeff_effect.getCategory().getColor();
	}

	@Override
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	@Override
	protected void loadDetails(){
		try{
			detailsPanel.removeAll();
			JProgressBar bar = new JProgressBar();
			detailsPanel.add(bar, BorderLayout.NORTH);

			Analysis analysis = Highlander.getCurrentAnalysis();
			Map<String, Field> categories = new LinkedHashMap<>();
			for (Field field : Field.getAvailableFields(analysis, false)) {
				if (field.hasTag(rankScoreType)) {
					String software = field.getAnnotationHeaders()[1].split("_")[0];
					switch (field.getAnnotationHeaders()[1]) {
					case "phyloP17way_primate_rankscore":
						software = "PhyloP_primate";
						break;
					case "phyloP30way_mammalian_rankscore":
						software = "PhyloP_mammalian";
						break;
					case "phyloP100way_vertebrate_rankscore":
						software = "PhyloP_vertebrate";
						break;
					case "phastCons17way_primate_rankscore":
						software = "PhastCons_primate";
						break;
					case "phastCons30way_mammalian_rankscore":
						software = "PhastCons_mammalian";
						break;
					case "phastCons100way_vertebrate_rankscore":
						software = "PhastCons_vertebrate";
						break;
					default:
						break;
					}
					categories.put(software, field);
				}
			}

			if (!categories.isEmpty()) {
				StringBuilder query = new StringBuilder();
				query.append("SELECT ");
				query.append(Field.variant_sample_id.getQuerySelectName(analysis, false) + ", ");
				query.append(Field.chr.getQuerySelectName(analysis, false) + ", ");
				query.append(Field.pos.getQuerySelectName(analysis, false) + ", ");
				query.append(Field.reference.getQuerySelectName(analysis, false) + ", ");
				query.append(Field.alternative.getQuerySelectName(analysis, false));
				for (Field field : categories.values()) {
					query.append(", "+field.getQuerySelectName(analysis, false));
				}
				query.append(" FROM " +	analysis.getFromSampleAnnotations());
				query.append(analysis.getJoinStaticAnnotations());
				query.append("WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN (" + HighlanderDatabase.makeSqlList(variantIds, Integer.class) + ")");
				Map<String,Map<String, Double>> dataPoints = new LinkedHashMap<String, Map<String,Double>>();
				Map<Integer,String> idToLoc = new HashMap<>();
				try (Results res = DB.select(Schema.HIGHLANDER, query.toString())) {
					while (res.next()){
						String variant = res.getString(Field.chr.getName()) + ":" + res.getInt(Field.pos.getName()) + "-" + res.getString(Field.reference.getName()) + ">" + res.getString(Field.alternative.getName());
						idToLoc.put(res.getInt(Field.variant_sample_id.getName()), variant);
						Map<String,Double> points = new LinkedHashMap<String,Double>();
						for (String category : categories.keySet()) {
							if (res.getObject(categories.get(category).getName()) != null) {
								points.put(category, res.getDouble(categories.get(category).getName()));
							}
						}
						dataPoints.put(variant, points);
					}
				}
				if (!dataPoints.isEmpty()){		
					Map<String,Map<String, Double>> sortedDataPoints = new LinkedHashMap<String, Map<String,Double>>();
					for (int id : variantIds) {
						sortedDataPoints.put(idToLoc.get(id), dataPoints.get(idToLoc.get(id)));
					}
					SpiderChart chart = new SpiderChart("", new ArrayList<String>(categories.keySet()), 0.0, 1.0, 5, sortedDataPoints);
					detailsPanel.removeAll();
					detailsPanel.add(chart, BorderLayout.CENTER);
				}else{
					detailsPanel.removeAll();
					detailsPanel.add(new JLabel("No variant were found in the database"), BorderLayout.CENTER);			
				}
			}else{
				detailsPanel.removeAll();
				detailsPanel.add(new JLabel("Analysis " + analysis + " doesn't contain any "+rankScoreType+" (normally given in DBNSFP)."), BorderLayout.CENTER);			
			}
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

}
