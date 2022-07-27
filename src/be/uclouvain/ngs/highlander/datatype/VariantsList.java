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
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.UI.toolbar.DatabasePanel;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.HighlightingPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.NavigationPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.SortingPanel;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.HighlightingRule.RuleType;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.ListOfVariants;

public class VariantsList {

	private final int SAFE_LENGTH = 60000;

	private String listName;

	private Analysis analysis;
	private List<Field> columnSelection = new ArrayList<Field>();
	private ComboFilter filter = null;
	private List<Field> columnMask = new ArrayList<Field>();
	private List<SortingCriterion> sorting = new ArrayList<SortingCriterion>();
	private List<HighlightingRule> highlighting = new ArrayList<HighlightingRule>();
	private List<Integer> variants = new ArrayList<Integer>();

	private FilteringPanel filteringPanel;
	private DatabasePanel databasePanel;
	private NavigationPanel navigationPanel;
	private SortingPanel sortingPanel;
	private HighlightingPanel highlightingPanel;
	private VariantsTable variantsTable;

	public VariantsList(Analysis analysis,  FilteringPanel filteringPanel, DatabasePanel databasePanel, NavigationPanel navigationPanel, 
			SortingPanel sortingPanel, HighlightingPanel highlightingPanel, VariantsTable variantsTable, String listName){
		this.analysis = analysis;
		this.filteringPanel = filteringPanel;
		this.databasePanel = databasePanel;
		this.navigationPanel = navigationPanel;
		this.sortingPanel = sortingPanel;
		this.highlightingPanel = highlightingPanel;
		this.variantsTable = variantsTable;
		this.listName = listName;
	}

	public VariantsList(Analysis analysis, List<Field> columnSelection,	ComboFilter filter,	
			List<Field> columnMask, List<SortingCriterion> sorting,	List<HighlightingRule> highlighting,	
			List<Integer> variants, String listName){
		this.analysis = analysis;
		this.columnSelection = new ArrayList<Field>(columnSelection);
		this.filter = filter;
		this.columnMask = new ArrayList<Field>(columnMask);
		this.sorting = new ArrayList<SortingCriterion>(sorting);
		this.highlighting = new ArrayList<HighlightingRule>(highlighting);
		this.variants = new ArrayList<Integer>(variants); 
		this.listName = listName;
	}

	public String toString(){
		return listName;
	}

	public Analysis getAnalysis(){
		return analysis;
	}

	public List<Field> getColumnSelection() {
		return columnSelection;
	}

	public ComboFilter getFilter() {
		return filter;
	}

	public List<Field> getColumnMask() {
		return columnMask;
	}

	public List<SortingCriterion> getSorting() {
		return sorting;
	}

	public List<HighlightingRule> getHighlighting() {
		return highlighting;
	}

	public List<Integer> getVariants() {
		return variants;
	}

	public List<String> getSaveStrings(){
		List<String> saveStrings = new ArrayList<String>();
		StringBuilder save = new StringBuilder();

		//COLUMN_MASK
		for (Field field : columnMask){
			save.append("°");
			save.append(field.getName());
			if (save.length() > SAFE_LENGTH){
				saveStrings.add(UserData.COLUMN_MASK + save.toString());
				save = new StringBuilder();
			}
		}
		if (save.length() > 0){
			saveStrings.add(UserData.COLUMN_MASK + save.toString());
			save = new StringBuilder();			
		}

		//COLUMN_SELECTION
		for (Field field : columnSelection){
			save.append("°");
			save.append(field.getName());
			if (save.length() > SAFE_LENGTH){
				saveStrings.add(UserData.COLUMN_SELECTION + save.toString());
				save = new StringBuilder();
			}
		}
		if (save.length() > 0){
			saveStrings.add(UserData.COLUMN_SELECTION + save.toString());
			save = new StringBuilder();			
		}

		//FILTER
		save.append("°");
		save.append(filter.getSaveString());
		saveStrings.add(UserData.FILTER + save.toString());
		save = new StringBuilder();

		//HIGHLIGHTING
		for (HighlightingRule crit : highlighting){
			save.append("°");
			save.append(crit.getSaveString());
			if (save.length() > SAFE_LENGTH){
				saveStrings.add(UserData.HIGHLIGHTING + save.toString());
				save = new StringBuilder();
			}
		}
		if (save.length() > 0){
			saveStrings.add(UserData.HIGHLIGHTING + save.toString());
			save = new StringBuilder();			
		}

		//SORTING
		for (SortingCriterion crit : sorting){
			save.append("°");
			save.append(crit.getFieldName() + "|" + crit.getSortOrder());
			if (save.length() > SAFE_LENGTH){
				saveStrings.add(UserData.SORTING + save.toString());
				save = new StringBuilder();
			}
		}
		if (save.length() > 0){
			saveStrings.add(UserData.SORTING + save.toString());
			save = new StringBuilder();			
		}

		//VALUES
		for (int id : variants){
			save.append("°");
			save.append(id);
			if (save.length() > SAFE_LENGTH){
				saveStrings.add(UserData.VALUES + save.toString());
				save = new StringBuilder();
			}
		}
		if (save.length() > 0){
			saveStrings.add(UserData.VALUES + save.toString());
			save = new StringBuilder();			
		}

		return saveStrings;
	}

	public void parseSaveString(String saveString) throws Exception {
		int cut = saveString.indexOf('°');
		UserData data = UserData.valueOf(saveString.substring(0, cut));
		String[] vals = saveString.substring(cut+1, saveString.length()).split("°");
		switch(data){
		case COLUMN_MASK:
			for (String val : vals){
				if (Field.exists(val)) columnMask.add(Field.getField(val));
			}
			break;
		case COLUMN_SELECTION:
			for (String val : vals){
				if (Field.exists(val)) columnSelection.add(Field.getField(val));
			}
			break;
		case FILTER:
			for (String val : vals){
				filter = (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, val);
			}
			break;
		case HIGHLIGHTING:
			for (String val : vals){
				RuleType ruleType = RuleType.valueOf(val.split("\\|")[0]);
				switch (ruleType) {
				case HIGHLIGHTING:
					highlighting.add(new HighlightCriterion().loadCriterion(highlightingPanel, val));				
					break;
				case HEATMAP:
					highlighting.add(new HeatMapCriterion().loadCriterion(highlightingPanel, val));
					break;
				default:
					System.err.println("Unknown Highlighting rule type : " + ruleType);
					break;
				}
			}
			break;
		case SORTING:
			for (String val : vals){
				String fieldName = val.split("\\|")[0];
				SortOrder sortOrder = SortOrder.valueOf(val.split("\\|")[1]);
				if (Field.exists(fieldName)) sorting.add(new SortingCriterion(sortingPanel, Field.getField(fieldName), sortOrder));
			}
			break;
		case VALUES:
			for (String val : vals){
				variants.add(Integer.parseInt(val));
			}
			break;
		default:
			throw new Exception(data + " is not supported for variant lists");
		}
	}

	public void restore() throws Exception {
		filteringPanel.setFilter(filter, "");
		
		databasePanel.showVariantListSelection(columnSelection);
		navigationPanel.showVariantListMask(columnMask);
		ListOfVariants pseudoFilter = new ListOfVariants(variants);
		try {
			Highlander.waitingPanel.start(true);
			variantsTable.fillTable(pseudoFilter.retreiveData(columnSelection, new HashSet<String>(), "Variant list"));
			SwingUtilities.invokeLater(new Runnable() {				
				@Override
				public void run() {
					try {
						highlightingPanel.setHighlightingRules(highlighting, null);
						sortingPanel.setSorting(sorting, null);
					}catch (Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(null, Tools.getMessage("Problem when restoring variants list", ex), "Restore variants list",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
		} catch (com.mysql.jdbc.exceptions.MySQLStatementCancelledException ex){
			Highlander.waitingPanel.setProgressString("Cancelling query", true);
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(null, Tools.getMessage("Problem when restoring variants list", ex), "Restore variants list",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}finally{
			Highlander.waitingPanel.forceStop();
		}
	}

}
