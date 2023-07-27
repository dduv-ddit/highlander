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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateCustomFilter;
import be.uclouvain.ngs.highlander.UI.dialog.CreateMagicFilter;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel.CancelException;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public class ComboFilter extends Filter {

	//A combo filter element is either a CustomFilter or a MagicFilter ...
	private boolean isSimple = false;
	protected Filter filter;

	//... or a logical operator linking a list of ComboFilters
	private boolean isComplex = false;
	private LogicalOperator logicop = null;
	private List<ComboFilter> comboList = new ArrayList<ComboFilter>();
	private JPanel buttonsPanel;

	private int nVariants = -1;

	public ComboFilter(FilteringPanel filteringPanel, Filter filter){
		this.filteringPanel = filteringPanel;
		isSimple = true;
		this.filter = filter;
		this.filter.parentFilter = this;
		displayFilterPanel();
	}

	public ComboFilter(FilteringPanel filteringPanel, LogicalOperator logicop, List<ComboFilter> comboList){
		this.filteringPanel = filteringPanel;
		isComplex = true;
		this.logicop = logicop;
		for (ComboFilter crit : comboList){
			crit.parentFilter = this;
			addComboFilter(crit);
		}
		displayFilterPanel();
		displayFilterListPanel();
	}

	public ComboFilter(){

	}

	@Override
	public void setFilteringPanel(FilteringPanel filteringPanel){
		this.filteringPanel = filteringPanel;
		for (ComboFilter criterion : comboList) criterion.setFilteringPanel(filteringPanel);
	}

	public Filter getFilter(){
		return filter;
	}

	@Override
	public Filter getSubFilter(int index){
		if (isSimple){
			return filter;
		}else{
			return comboList.get(index);
		}
	}

	@Override
	public int getSubFilterCount(){
		if (isSimple){
			return 1;
		}else{
			return comboList.size();
		}
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.COMBO;
	}

	@Override
	public boolean hasSamples() {
		if (isSimple){
			return filter.hasSamples();
		}else{
			for (ComboFilter cf : comboList){
				if (!cf.hasSamples()) return false;
			}
			return true;
		}
	}

	@Override
	public Set<String> getIncludedSamples() {
		Set<String> samples = new HashSet<String>();
		if (isSimple){
			samples = filter.getIncludedSamples();
		}else{
			for (ComboFilter cf : comboList){
				samples.addAll(cf.getIncludedSamples());
			}
		}
		return samples;
	}

	@Override
	public Set<String> getExcludedSamples(){
		return new TreeSet<String>();
	}

	@Override
	public Set<String> getUserDefinedSamples(boolean includeProfileList){
		Set<String> samples = new HashSet<String>();
		if (isSimple){
			samples = filter.getUserDefinedSamples(includeProfileList);
		}else{
			for (ComboFilter cf : comboList){
				samples.addAll(cf.getUserDefinedSamples(includeProfileList));
			}
		}
		return samples;
	}

	@Override
	public String getSaveString(){
		StringBuilder sb = new StringBuilder();
		if (isSimple){
			sb.append(filter.getFilterType().toString()); 
			sb.append("$");
			sb.append(filter.getSaveString());
		}else{			
			if (logicop == LogicalOperator.AND) sb.append("€{");
			else sb.append("^{");
			for (int i=0 ; i < comboList.size() ; i++){
				sb.append(comboList.get(i).getSaveString());
				sb.append("§");
			}	
			sb.append("}");
		}
		return sb.toString();
	}

	@Override
	public String parseSaveString(String saveString){
		if (!saveString.startsWith("€") && !saveString.startsWith("^")){
			int dollarPos = saveString.indexOf("$");
			String filterType = saveString.substring(0,dollarPos);
			String filter = saveString.substring(dollarPos+1);
			switch(FilterType.valueOf(filterType)){
			case CUSTOM:
				return new CustomFilter().parseSaveString(filter);
			case VARIANTS_COMMON_TO_SAMPLES:
				return new VariantsCommonToSamples().parseSaveString(filter);
			case SAMPLE_SPECIFIC_VARIANTS:
				return new SampleSpecificVariants().parseSaveString(filter);
			case COMMON_GENE_VARIANTS:
				return new CommonGeneVariants().parseSaveString(filter);
			case COMBINED_HETEROZYGOUS_VARIANTS:
				return new CombinedHeterozygousVariants().parseSaveString(filter);
			case PATHOLOGY_FREQUENCY:
				return new PathologyFrequency().parseSaveString(filter);
			case INTERVALS:
				return new Intervals().parseSaveString(filter);
			case SAME_CODON:
				return new SameCodon().parseSaveString(filter);
			case MULTIPLE_NUCLEOTIDES_VARIANTS:
				return new MultipleNucleotidesPolymorphisms().parseSaveString(filter);
			case COMBO:
				return new ComboFilter().parseSaveString(filter);
			case LIST_OF_VARIANTS:
			default:
				return "";
			}				
		}else{			
			LogicalOperator lo;
			if (saveString.startsWith("€")) lo = LogicalOperator.AND;
			else lo = LogicalOperator.OR;
			List<String> crit = new ArrayList<String>();
			StringBuilder sb = new StringBuilder();
			int level = 0;
			for (int c=2 ; c < saveString.length() ; c++){
				if (saveString.charAt(c) == '€' || saveString.charAt(c) == '^'){
					level++;
					sb.append(saveString.charAt(c));
				}else if (saveString.charAt(c) == '}'){
					if (level > 0){
						level--;
						sb.append(saveString.charAt(c));
					}else{
						//end of string
						sb = new StringBuilder();
					}
				}else if (saveString.charAt(c) == '§'){
					if (level > 0){
						sb.append(saveString.charAt(c));
					}else{
						crit.add(parseSaveString(sb.toString()));
						sb = new StringBuilder();
					}
				}else{
					sb.append(saveString.charAt(c));
				}
			}
			sb = new StringBuilder();
			for (int i=0 ; i < crit.size() ; i++){
				if (crit.size() > 1) sb.append("{");
				sb.append(crit.get(i));
				if (crit.size() > 1) sb.append("}");
				if (i < crit.size()-1) sb.append(" " + lo + " ");
			}
			return sb.toString();
		}		
	}

	@Override
	public Filter loadCriterion(FilteringPanel filteringPanel, String saveString) throws Exception {		
		if (!saveString.startsWith("€") && !saveString.startsWith("^")){
			int dollarPos = saveString.indexOf("$");
			String filterType = saveString.substring(0,dollarPos);
			String filter = saveString.substring(dollarPos+1);
			Filter temp;
			switch(FilterType.valueOf(filterType)){
			case CUSTOM:
				temp = new CustomFilter().loadCriterion(filteringPanel, filter);
				break;
			case VARIANTS_COMMON_TO_SAMPLES:
				temp =  new VariantsCommonToSamples().loadCriterion(filteringPanel, filter);
				break;
			case SAMPLE_SPECIFIC_VARIANTS:
				temp =  new SampleSpecificVariants().loadCriterion(filteringPanel, filter);
				break;
			case COMMON_GENE_VARIANTS:
				temp =  new CommonGeneVariants().loadCriterion(filteringPanel, filter);
				break;
			case COMBINED_HETEROZYGOUS_VARIANTS:
				temp =  new CombinedHeterozygousVariants().loadCriterion(filteringPanel, filter);
				break;
			case PATHOLOGY_FREQUENCY:
				temp =  new PathologyFrequency().loadCriterion(filteringPanel, filter);
				break;
			case INTERVALS:
				temp =  new Intervals().loadCriterion(filteringPanel, filter);
				break;
			case SAME_CODON:
				temp =  new SameCodon().loadCriterion(filteringPanel, filter);
				break;
			case MULTIPLE_NUCLEOTIDES_VARIANTS:
				temp =  new MultipleNucleotidesPolymorphisms().loadCriterion(filteringPanel, filter);
				break;
			case COMBO:
				temp =  new ComboFilter().loadCriterion(filteringPanel, filter);
				break;
			case LIST_OF_VARIANTS:
			default:
				temp = null;
				break;
			}
			return new ComboFilter(filteringPanel, temp);
		}else{			
			LogicalOperator lo;
			if (saveString.startsWith("€")) lo = LogicalOperator.AND;
			else lo = LogicalOperator.OR;
			List<ComboFilter> combos = new ArrayList<ComboFilter>();
			StringBuilder sb = new StringBuilder();
			int level = 0;
			for (int c=2 ; c < saveString.length() ; c++){
				if (saveString.charAt(c) == '€' || saveString.charAt(c) == '^'){
					level++;
					sb.append(saveString.charAt(c));
				}else if (saveString.charAt(c) == '}'){
					if (level > 0){
						level--;
						sb.append(saveString.charAt(c));
					}else{
						//end of string
						sb = new StringBuilder();
					}
				}else if (saveString.charAt(c) == '§'){
					if (level > 0){
						sb.append(saveString.charAt(c));
					}else{
						combos.add((ComboFilter)loadCriterion(filteringPanel, sb.toString()));
						sb = new StringBuilder();
					}
				}else{
					sb.append(saveString.charAt(c));
				}
			}			
			return new ComboFilter(filteringPanel, lo, combos);
		}
	}

	@Override
	public boolean isFilterValid(){
		if (isSimple){
			return filter.isFilterValid();
		}else{
			boolean ok = true;
			for (ComboFilter combo : comboList){
				if (!combo.isFilterValid()) ok = false;
			}
			return ok;
		}
	}

	@Override
	public List<String> getValidationProblems(){
		List<String> problems = new ArrayList<String>();
		if (isSimple){
			problems.addAll(filter.getValidationProblems());
		}else{
			for (ComboFilter combo : comboList){
				problems.addAll(combo.getValidationProblems());
			}
		}
		return problems;
	}

	public boolean checkProfileValues(){
		if (isSimple){
			if (filter.getFilterType() == FilterType.CUSTOM){
				return ((CustomFilter)filter).checkProfileValues();
			}else{
				return true;
			}
		}else{
			boolean ok = true;
			for (ComboFilter combo : comboList){
				if (!combo.checkProfileValues()) ok = false;
			}
			return ok;
		}
	}

	public String getFirstInexistantProfileList(){
		if (isSimple){
			if (filter.getFilterType() == FilterType.CUSTOM){
				return ((CustomFilter)filter).getFirstInexistantProfileList();
			}
		}else{
			for (ComboFilter combo : comboList){
				String guilty = combo.getFirstInexistantProfileList();
				if (combo.getFirstInexistantProfileList() != null) 
					return guilty;
			}
		}
		return null;
	}

	@Override
	public boolean checkFieldCompatibility(Analysis analysis){
		if (isSimple){
			return filter.checkFieldCompatibility(analysis);
		}else{
			boolean ok = true;
			for (ComboFilter combo : comboList){
				if (!combo.checkFieldCompatibility(analysis)) ok = false;
			}
			return ok;
		}
	}

	@Override
	public boolean changeAnalysis(Analysis analysis){
		if (isSimple){
			return filter.changeAnalysis(analysis);
		}else{
			boolean ok = true;
			for (ComboFilter combo : comboList){
				if (!combo.changeAnalysis(analysis)) ok = false;
			}
			return ok;
		}
	}

	protected void displayFilterPanel(){
		removeAll();

		setLayout(new BorderLayout(6,1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
		if (filter != null) add(filter,BorderLayout.CENTER);
		JPanel west = new JPanel();
		west.setPreferredSize(new Dimension(2,2));
		add(west,BorderLayout.WEST);
		buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BorderLayout());
		JButton removeButton = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						delete();
					}
				}, "ComboFilter.delete").start();
			}
		});
		removeButton.setToolTipText("Delete filter");	
		removeButton.setBorder(null);
		removeButton.setBorderPainted(false);
		removeButton.setContentAreaFilled(false);
		removeButton.setMargin(new Insets(0, 0, 0, 0));
		buttonsPanel.add(removeButton,BorderLayout.SOUTH);
		add(buttonsPanel,BorderLayout.EAST);
	}

	private void displayFilterListPanel(){
		removeAll();

		setLayout(new BorderLayout(1,1));
		JPanel center = new JPanel();		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEADING, 3, 0);
		center.setLayout(flowLayout);
		List<ComboFilter> toDisplay = new ArrayList<ComboFilter>();
		for (int i=0 ; i < comboList.size() ; i++){
			if (comboList.get(i).isComplex){
				toDisplay.add(comboList.get(i));
			}
		}
		for (int i=0 ; i < comboList.size() ; i++){
			if (comboList.get(i).isSimple && comboList.get(i).getFilter().getFilterType() != FilterType.CUSTOM){
				toDisplay.add(comboList.get(i));
			}
		}
		for (int i=0 ; i < comboList.size() ; i++){
			if (comboList.get(i).isSimple && comboList.get(i).getFilter().getFilterType() == FilterType.CUSTOM){
				toDisplay.add(comboList.get(i));
			}
		}
		for (int i=0 ; i < toDisplay.size() ; i++){
			center.add(toDisplay.get(i));			
			switch(logicop){
			case AND:
				toDisplay.get(i).setBorder(BorderFactory.createLineBorder(new Color(96,74,123),2));
				if (i < toDisplay.size()-1){
					JLabel labAnd = new JLabel(Resources.getScaledIcon(Resources.iFilterAnd, 16));
					center.add(labAnd);
				}
				break;
			case OR:
				toDisplay.get(i).setBorder(BorderFactory.createLineBorder(new Color(244,116,20),2));
				if (i < toDisplay.size()-1){
					JLabel labOr = new JLabel(Resources.getScaledIcon(Resources.iFilterOr, 16));
					center.add(labOr);
				}
				break;
			}
		}
		add(center,BorderLayout.CENTER);
		JPanel top = new JPanel();
		top.setPreferredSize(new Dimension(2,2));
		add(top,BorderLayout.NORTH);
		JPanel bottom = new JPanel();
		bottom.setPreferredSize(new Dimension(2,2));
		add(bottom,BorderLayout.SOUTH);
		if (getParent() == null || getParent().getParent() instanceof ComboFilter){		
			buttonsPanel = new JPanel();
			buttonsPanel.setLayout(new BorderLayout());
			add(buttonsPanel,BorderLayout.EAST);

			JButton removeButton = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
			removeButton.setToolTipText("Delete filter");	
			removeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable(){
						@Override
						public void run(){
							delete();
						}
					}, "ComboFilter.delete").start();
				}
			});
			removeButton.setBorder(null);
			removeButton.setBorderPainted(false);
			removeButton.setContentAreaFilled(false);
			removeButton.setMargin(new Insets(0, 0, 0, 0));
			buttonsPanel.add(removeButton,BorderLayout.SOUTH);

			JButton addButton = new JButton(Resources.getScaledIcon(Resources.i3dPlus, 16));
			final JPopupMenu addButtonPopupMenu = new JPopupMenu();
			switch(logicop){
			case OR:
				JMenuItem itemAddCustomOr = new JMenuItem("Add a Custom Filter, using the logical operator OR (i.e. filter will be the UNION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddCustomOr, 24));
				itemAddCustomOr.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addNewCustomFilter(LogicalOperator.OR);
							}
						}, "ComboFilter.addNewCustomFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddCustomOr);
				JMenuItem itemAddMagicOr = new JMenuItem("Add a Magic Filter, using the logical operator OR (i.e. filter will be the UNION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddMagicOr, 24));
				itemAddMagicOr.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addNewMagicFilter(LogicalOperator.OR);
							}
						}, "ComboFilter.addNewMagicFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddMagicOr);
				JMenuItem itemAddProfileOr = new JMenuItem("Load a filter from your profile and add it, using the logical operator OR (i.e. filter will be the UNION of sub-filters)",Resources.getScaledIcon(Resources.iFilterLoadOr, 24));
				itemAddProfileOr.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addProfileFilter(LogicalOperator.OR);
							}
						}, "ComboFilter.addProfileFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddProfileOr);
				break;
			case AND:
				JMenuItem itemAddCustomAnd = new JMenuItem("Add a Custom Filter, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddCustomAnd, 24));
				itemAddCustomAnd.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addNewCustomFilter(LogicalOperator.AND);
							}
						}, "ComboFilter.addNewCustomFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddCustomAnd);
				JMenuItem itemAddMagicAnd = new JMenuItem("Add a Magic Filter, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddMagicAnd, 24));
				itemAddMagicAnd.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addNewMagicFilter(LogicalOperator.AND);
							}
						}, "ComboFilter.addNewMagicFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddMagicAnd);
				JMenuItem itemAddProfileAnd = new JMenuItem("Load a filter from your profile and add it, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-filters)",Resources.getScaledIcon(Resources.iFilterLoadAnd, 24));
				itemAddProfileAnd.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addProfileFilter(LogicalOperator.AND);
							}
						}, "ComboFilter.addProfileFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddProfileAnd);
				break;
			}
			MouseListener statusAddButtonPopupListener = new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseExited(MouseEvent e) {}
				@Override
				public void mouseEntered(MouseEvent e) {}
				@Override
				public void mouseClicked(MouseEvent e) {
					addButtonPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			};
			addButton.addMouseListener(statusAddButtonPopupListener);
			addButton.setBorder(null);
			addButton.setBorderPainted(false);
			addButton.setContentAreaFilled(false);
			addButton.setMargin(new Insets(0, 0, 0, 0));
			buttonsPanel.add(addButton,BorderLayout.NORTH);
		}
	}

	@Override
	public String toString(){
		StringBuilder details = new StringBuilder();
		if (isSimple){
			return filter.toString();
		}else{
			for (int i=0 ; i < comboList.size() ; i++){
				if (comboList.size() > 1) details.append("{ ");
				details.append(comboList.get(i).toString());
				if (comboList.size() > 1) details.append(" }");
				if (i < comboList.size()-1){
					switch(logicop){
					case AND:
						details.append(" AND ");
						break;
					case OR:
						details.append(" OR ");
						break;
					}
				}
			}
		}
		return details.toString();
	}

	@Override
	public String toHtmlString(){
		if (isSimple){
			return filter.toHtmlString();
		}else{
			StringBuilder details = new StringBuilder();
			details.append("<html>");
			details.append(toString().replace(" AND ", "<br>AND<br>").replace(" OR ", "<br>OR<br>"));
			details.append("</html>");
			return details.toString();
		}
	}

	@Override
	public boolean isSimple(){
		return isSimple;
	}

	@Override
	public boolean isComplex(){
		return isComplex;
	}

	@Override
	public LogicalOperator getLogicalOperator(){
		return logicop;
	}

	public void addNewCustomFilter(LogicalOperator operator){
		CreateCustomFilter newCustom = new CreateCustomFilter(Highlander.getCurrentAnalysis(), filteringPanel);
		Tools.centerWindow(newCustom, false);
		newCustom.setVisible(true);
		if(newCustom.getCriterion() != null){		
			if (isSimple){
				if (filter.getFilterType() == FilterType.CUSTOM){
					((CustomFilter)filter).addCriterion(newCustom.getCriterion(), operator);
				}else{
					addFilter(new ComboFilter(filteringPanel, newCustom.getCriterion()), operator);
				}
			}else{
				boolean hasCustom = false;
				for (ComboFilter f : comboList){
					if (f.isSimple && f.getFilter().getFilterType() == FilterType.CUSTOM){
						((CustomFilter)f.getFilter()).addCriterion(newCustom.getCriterion(), operator);
						hasCustom = true;
					}
				}
				if (!hasCustom){
					addFilter(new ComboFilter(filteringPanel, newCustom.getCriterion()), operator);
				}
			}
		}
	}

	public void addNewMagicFilter(LogicalOperator operator){
		CreateMagicFilter newMagic = new CreateMagicFilter(filteringPanel);
		Tools.centerWindow(newMagic, false);
		newMagic.setVisible(true);
		if(newMagic.getCriterion() != null){
			addFilter(new ComboFilter(filteringPanel, newMagic.getCriterion()), operator);
		}
	}

	public void addProfileFilter(LogicalOperator operator){
		String name = ProfileTree.showProfileDialog(new JFrame(), Action.LOAD, UserData.FILTER, Highlander.getCurrentAnalysis().toString());
		if (name != null){
			try {
				ComboFilter profileFilter = Highlander.getLoggedUser().loadFilter(filteringPanel, Highlander.getCurrentAnalysis(), name);
				if (profileFilter.isSimple && profileFilter.getFilter().getFilterType() == FilterType.CUSTOM && profileFilter.getFilter().getLogicalOperator() == operator){
					CustomFilter profileCustom = (CustomFilter)profileFilter.getFilter();
					List<CustomFilter> profileCustomList = new ArrayList<CustomFilter>();
					if (profileCustom.isSimple()){
						profileCustomList.add(profileCustom);
					}else{
						for (int i=0 ; i < profileCustom.getSubFilterCount() ; i++){
							profileCustomList.add((CustomFilter)profileCustom.getSubFilter(i));
						}
					}
					if (isSimple){
						if (filter.getFilterType() == FilterType.CUSTOM){
							for (CustomFilter cf : profileCustomList){
								((CustomFilter)filter).addCriterion(cf, operator);
							}
						}else{
							addFilter(profileFilter, operator);
						}
					}else{
						boolean hasCustom = false;
						for (ComboFilter f : comboList){
							if (f.isSimple && f.getFilter().getFilterType() == FilterType.CUSTOM){
								for (CustomFilter cf : profileCustomList){
									((CustomFilter)f.getFilter()).addCriterion(cf, operator);
								}
								hasCustom = true;
							}
						}
						if (!hasCustom){
							addFilter(profileFilter, operator);
						}
					}
				}else{
					addFilter(profileFilter, operator);
				}
			} catch (Exception ex) {
				Tools.exception(ex);
				JOptionPane.showMessageDialog(filteringPanel, Tools.getMessage("Error", ex), "Load criteria list from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public void addFilter(ComboFilter comboFilter, LogicalOperator operator){
		if (isSimple){
			ComboFilter crit1 = new ComboFilter(filteringPanel, filter);
			ComboFilter crit2 = comboFilter;
			//Destroy everything created for simple criterion
			isSimple = false;
			filter = null;
			removeAll();
			//Create everything for complex criterion
			isComplex = true;
			logicop = operator;
			addComboFilter(crit1);
			addComboFilter(crit2);
			displayFilterListPanel();
		}else{
			addComboFilter(comboFilter);
			displayFilterListPanel();
		}
		//refresh main table
		filteringPanel.refresh();
	}

	private void addComboFilter(final ComboFilter comboFilter){	
		if (isComplex){
			comboFilter.parentFilter = this;
			if (comboFilter.isSimple && comboFilter.filter.getFilterType() != FilterType.CUSTOM && comboFilter.filter.getFilterType() != FilterType.COMBO){
				//Add magic filters at the start to avoid people thinking they take place after (it kind of simultaneous)
				comboList.add(0, comboFilter);
			}else{
				comboList.add(comboFilter);
			}

			JButton addButton = new JButton(Resources.getScaledIcon(Resources.i3dPlus, 16));
			final JPopupMenu addButtonPopupMenu = new JPopupMenu();
			switch(logicop){
			case AND:
				JMenuItem itemAddCustomOr = new JMenuItem("Add a Custom Filter, using the logical operator OR (i.e. filter will be the UNION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddCustomOr, 24));
				itemAddCustomOr.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								comboFilter.addNewCustomFilter(LogicalOperator.OR);
							}
						}, "ComboFilter.addNewCustomFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddCustomOr);
				JMenuItem itemAddMagicOr = new JMenuItem("Add a Magic Filter, using the logical operator OR (i.e. filter will be the UNION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddMagicOr, 24));
				itemAddMagicOr.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								comboFilter.addNewMagicFilter(LogicalOperator.OR);
							}
						}, "ComboFilter.addNewMagicFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddMagicOr);
				JMenuItem itemAddProfileOr = new JMenuItem("Load a filter from your profile and add it, using the logical operator OR (i.e. filter will be the UNION of sub-filters)",Resources.getScaledIcon(Resources.iFilterLoadOr, 24));
				itemAddProfileOr.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								comboFilter.addProfileFilter(LogicalOperator.OR);
							}
						}, "ComboFilter.addProfileFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddProfileOr);
				break;
			case OR:
				JMenuItem itemAddCustomAnd = new JMenuItem("Add a Custom Filter, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddCustomAnd, 24));
				itemAddCustomAnd.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								comboFilter.addNewCustomFilter(LogicalOperator.AND);
							}
						}, "ComboFilter.addNewCustomFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddCustomAnd);
				JMenuItem itemAddMagicAnd = new JMenuItem("Add a Magic Filter, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-filters)",Resources.getScaledIcon(Resources.iFilterAddMagicAnd, 24));
				itemAddMagicAnd.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								comboFilter.addNewMagicFilter(LogicalOperator.AND);
							}
						}, "ComboFilter.addNewMagicFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddMagicAnd);
				JMenuItem itemAddProfileAnd = new JMenuItem("Load a filter from your profile and add it, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-filters)",Resources.getScaledIcon(Resources.iFilterLoadAnd, 24));
				itemAddProfileAnd.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								comboFilter.addProfileFilter(LogicalOperator.AND);
							}
						}, "ComboFilter.addProfileFilter").start();
					}
				});
				addButtonPopupMenu.add(itemAddProfileAnd);
				break;
			}
			MouseListener statusAddButtonPopupListener = new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {}
				@Override
				public void mousePressed(MouseEvent e) {}
				@Override
				public void mouseExited(MouseEvent e) {}
				@Override
				public void mouseEntered(MouseEvent e) {}
				@Override
				public void mouseClicked(MouseEvent e) {
					addButtonPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			};
			addButton.addMouseListener(statusAddButtonPopupListener);

			addButton.setBorder(null);
			addButton.setBorderPainted(false);
			addButton.setContentAreaFilled(false);
			addButton.setMargin(new Insets(0, 0, 0, 0));

			comboFilter.buttonsPanel.add(addButton,BorderLayout.NORTH);
		}
	}

	public void removeFilter(ComboFilter criterion){
		if (isComplex){
			if(comboList.size() > 2){
				comboList.remove(criterion);				
				displayFilterListPanel();
			}else{
				comboList.remove(criterion);				
				ComboFilter orphan = comboList.get(0);
				if (getParentFilter() == null){
					ComboFilter newChild;
					if (orphan.isSimple){
						newChild = new ComboFilter(orphan.filteringPanel, orphan.filter);
					}else{
						newChild = new ComboFilter(orphan.filteringPanel, orphan.logicop, orphan.comboList);
					}
					filteringPanel.setFilter(newChild, null);					
				}else if (getParentFilter().getFilterType() == FilterType.COMBO){
					ComboFilter parent = (ComboFilter)getParentFilter();
					ComboFilter newChild;
					if (orphan.isSimple){
						newChild = new ComboFilter(orphan.filteringPanel, orphan.filter);
					}else{
						newChild = new ComboFilter(orphan.filteringPanel, logicop, orphan.comboList);
					}
					parent.comboList.remove(this);
					parent.addComboFilter(newChild);
					parent.displayFilterListPanel();
				}
			}
		}else{
			System.err.println("Impossible filter situation in ComboFilter.removeFilter(ComboFilter)");
		}
	}

	@Override
	public void delete(){
		if (getParentFilter() == null){
			//We are the main ComboFilter
			filteringPanel.clearFilter();
		}else if (getParentFilter().getFilterType() == FilterType.COMBO){
			//We are inside another ComboFilter
			ComboFilter parent = (ComboFilter)getParentFilter();
			parent.removeFilter(this);
		}else{
			System.err.println("Impossible filter situation in ComboFilter.delete()");
		}
		filteringPanel.refresh();
	}

	@Override
	public void editFilter(){
		//Should use the method directly from the Custom/Magic filter
	}

	@Override
	public int getNumberOfVariants() {
		return nVariants;
	}

	@Override
	public Map<Integer,String> getResultIds(Set<String> autoSamples) throws Exception {
		nVariants = -1;
		Map<Integer,String> ids;
		if (isSimple){
			ids = filter.getResultIds(autoSamples);
		}else{
			ids = new HashMap<Integer, String>();
			if (logicop == LogicalOperator.OR){
				Highlander.waitingPanel.setProgressString("Combo filters union", true);
				for (ComboFilter combo : comboList){
					ids.putAll(combo.getResultIds(getAllSamples()));
				}
			}else if (logicop == LogicalOperator.AND){
				Highlander.waitingPanel.setProgressString("Combo filters intersection", true);
				List<Map<Integer,String>> list = new ArrayList<Map<Integer,String>>();
				for (ComboFilter combo : comboList){
					list.add(combo.getResultIds(getAllSamples()));
				}
				Set<String> intersection = new HashSet<String>(list.get(0).values());
				for (int i=1 ; i < list.size() ; i++){
					intersection.retainAll(list.get(i).values());
				}
				for (Map<Integer,String> map : list){
					for (int id : map.keySet()){
						if (intersection.contains(map.get(id))){
							ids.put(id, map.get(id));
						}
					}
				}
			}
		}
		nVariants = ids.size();
		return ids;
	}

	@Override
	protected List<Field> getQueryWhereFields() throws Exception {
		List<Field> list = new ArrayList<Field>();
		list.add(Field.variant_sample_id);
		return list;
	}

	@Override
	protected String getQueryWhereClause(boolean includeTableWithJoinON) throws Exception {		
		Map<Integer,String> resultIds = getResultIds(getAllSamples());
		Field id = Field.variant_sample_id;
		if (resultIds.isEmpty()) return (id.getQueryWhereName(Highlander.getCurrentAnalysis(), false) + " IS NULL");
		return (id.getQueryWhereName(Highlander.getCurrentAnalysis(), false) + " IN ("+HighlanderDatabase.makeSqlList(resultIds.keySet(), Integer.class)+")");
	}

	@Override
	protected VariantResults extractResults(Results res, List<Field> headers, String progressTxt, boolean indeterminateProgress) throws Exception {		
		Map<Integer, Object[]> dataMap = new LinkedHashMap<Integer, Object[]>();
		Map<Integer, String> variants = new LinkedHashMap<Integer, String>();
		int resCount=0;
		while (res.next() && !Highlander.waitingPanel.isCancelled()){
			if (indeterminateProgress){
				Highlander.waitingPanel.setProgressString(progressTxt + " ("+(resCount++)+" variants retreived)", true);
			}else{
				Highlander.waitingPanel.setProgressValue(resCount++);
			}
			Object[] rowData = new Object[headers.size()];
			int col=0;
			for (Field field : headers){
				//when MySQL NULL is replaced by 0 for some fields (like evaluation), the 0 is a string and not an INT, causing type problems in VariantTable (for filtering on those columns)
				if (field.getDefaultValue() != null && field.getDefaultValue().equals("0")) {
					rowData[col] = res.getInt(field.getName());
				}else if (field.getFieldClass() == OffsetDateTime.class){
					rowData[col] = res.getTimestamp(field.getName());
				}else {
					rowData[col] = res.getObject(field.getName()); 					
				}
				col++;
			}
			int id = res.getInt("variant_sample_id");
			String variant = 	res.getString("chr") + "-" +
					res.getString("pos") + "-" +
					res.getString("length") + "-" +
					res.getString("reference") + "-" + 
					res.getString("alternative");
			dataMap.put(id, rowData);
			variants.put(id, variant);
		}
		if (Highlander.waitingPanel.isCancelled()) {
			throw new CancelException();
		}
		return new VariantResults(headers, dataMap, variants);
	}
}
