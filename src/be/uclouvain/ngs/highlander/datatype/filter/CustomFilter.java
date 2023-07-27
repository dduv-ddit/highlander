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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
import javax.swing.JLabel;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateCustomFilter;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel.CancelException;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public class CustomFilter extends Filter {

	public enum ComparisonOperator {
		EQUAL("\u003D","\u003D","="),
		DIFFERENT("\u2260","\u2260","!="),
		GREATER("\u003E","&gt;",">"),
		GREATEROREQUAL("\u2265","\u2265",">="),
		SMALLER("\u003C","&lt;","<"),
		SMALLEROREQUAL("\u2264","\u2264","<="),
		CONTAINS("\u2248","\u2248",null),
		DOESNOTCONTAINS("\u2249","\u2249",null),
		RANGE_II("\u005B\u002E\u003B\u002E\u005D","\u005B\u002E\u002C\u002E\u005D",null),
		RANGE_IE("\u005B\u002E\u003B\u002E\u005B","\u005B\u002E\u002C\u002E\u005B",null),
		RANGE_EE("\u005D\u002E\u003B\u002E\u005B","\u005D\u002E\u002C\u002E\u005B",null),
		RANGE_EI("\u005D\u002E\u003B\u002E\u005D","\u005D\u002E\u002C\u002E\u005D",null),
		;
		private String unicode;
		private String html;
		private String sql;
		ComparisonOperator(String unicode, String html, String sql){this.unicode = unicode; this.html = html; this.sql = sql;}
		public String getUnicode() {return unicode;}
		public String getHtml() {return html;}
		public String getSql() {return sql;}
	}

	//A filtering criterion is a field name, comparison operator, value list (entered by user, and saved in profile) and null values behaviour (include them in the results or not)
	private boolean isSimple = false;
	private Field field = null;
	private ComparisonOperator compop = null;
	private boolean nullValue = false;
	private List<String> values = new ArrayList<String>();
	private List<String> profileValues = new ArrayList<String>();
	private boolean includeNulls = true;
	private JLabel label;

	//Or a logical operator linking a list of filtering criteria
	private boolean isComplex = false;
	private LogicalOperator logicop = null;
	private List<CustomFilter> criteria = new ArrayList<CustomFilter>();
	private JPanel buttonsPanel;

	private int nVariants = -1;

	public CustomFilter(FilteringPanel filteringPanel, Field field, ComparisonOperator compop, boolean nullValue, List<String> values, List<String> profileValues, boolean includeNulls){
		this.filteringPanel = filteringPanel;
		isSimple = true;
		this.field = field;
		this.compop = compop;
		this.nullValue = nullValue;
		this.values.addAll(values);
		this.profileValues.addAll(profileValues);
		this.includeNulls = includeNulls;
		displayCriterionPanel();
	}

	public CustomFilter(FilteringPanel filteringPanel, LogicalOperator logicop, List<CustomFilter> criteria){
		this.filteringPanel = filteringPanel;
		isComplex = true;
		this.logicop = logicop;
		for (CustomFilter crit : criteria){
			crit.parentFilter = this;
			addCriterion(crit);
		}
		displayCriterionPanel();
		displayCriteriaListPanel();
	}

	public CustomFilter(){

	}

	@Override
	public void setFilteringPanel(FilteringPanel filteringPanel){
		this.filteringPanel = filteringPanel;
		for (CustomFilter criterion : criteria) criterion.setFilteringPanel(filteringPanel);
	}

	public Field getField(){
		return field;
	}

	public ComparisonOperator getComparisonOperator(){
		return compop;
	}

	public boolean getNullValue(){
		return nullValue;
	}

	public List<String> getValues(){
		return values;
	}

	public List<String> getProfileValues(){
		return profileValues;
	}

	public boolean getIncludeNulls(){
		return includeNulls;
	}

	@Override
	public Filter getSubFilter(int index){
		if (isComplex){
			return criteria.get(index);
		}else{
			return null;
		}
	}

	@Override
	public int getSubFilterCount(){
		if (isComplex){
			return criteria.size();
		}else{
			return 0;
		}
	}

	@Override
	public FilterType getFilterType(){
		return FilterType.CUSTOM;
	}

	@Override
	public boolean hasSamples() {
		if (isSimple){
			return (field.equals(Field.sample) || field.equals(Field.individual) || field.equals(Field.pathology) || field.equals(Field.population));
		}else{
			for (CustomFilter cf : criteria){
				if (cf.hasSamples()) return true;
			}
			return false;
		}
	}

	@Override
	public Set<String> getIncludedSamples() {
		Set<String> samples = new HashSet<String>();
		try{
			String where = getSamplesWhereClause(false);
			if (where.length() > 0){
				String query = "SELECT DISTINCT("+Field.sample.getQuerySelectName(Highlander.getCurrentAnalysis(), true)+") "
						+ "FROM "+Highlander.getCurrentAnalysis().getFromProjects()		
						+ "INNER JOIN `projects_analyses` USING (`project_id`) "
						+ Highlander.getCurrentAnalysis().getJoinPathologies()
						+ Highlander.getCurrentAnalysis().getJoinPopulations()
						+ "LEFT JOIN " + Highlander.getCurrentAnalysis().getFromUserAnnotationsSamplesPublic() + " USING (`project_id`) "
						+ "LEFT JOIN " + Highlander.getCurrentAnalysis().getFromUserAnnotationsSamplesPrivate() + " USING (`project_id`) "
						+ "WHERE `analysis` = '"+Highlander.getCurrentAnalysis()+"' AND " + where
						; 
				Tools.print("Submitting query to the database (to find included samples for custom filter): ");
				System.out.println(query);
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						query, true)) {
					while (res.next()){
						samples.add(res.getString(Field.sample.getName()));
					}
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
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
			if (field.getName().equals("sample")) {
				samples.addAll(values);
			}
		}else {
			for (int i=0 ; i < criteria.size() ; i++){
				samples.addAll(criteria.get(i).getUserDefinedSamples(includeProfileList));
				if (includeProfileList) {
					try {
						for (String prof : profileValues){
							for (String val : Highlander.getLoggedUser().loadValues(field, prof)){
								samples.add(val.toUpperCase());
							}
						}
					} catch (Exception ex) {
						Tools.exception(ex);
					}
				}
			}		
		}
		return samples;
	}

	@Override
	public String getSaveString(){
		StringBuilder sb = new StringBuilder();
		if (isSimple){
			sb.append(field.getName());
			sb.append("!");
			sb.append(compop.toString());
			sb.append("!");
			for (int i=0 ; i < values.size() ; i++){
				sb.append(values.get(i));
				if (i < values.size()-1) sb.append("?");
			}
			sb.append("!");
			for (int i=0 ; i < profileValues.size() ; i++){
				sb.append(profileValues.get(i));
				if (i < profileValues.size()-1) sb.append("?");
			}
			sb.append("!");
			sb.append((includeNulls)?"1":"0");
			sb.append("!");
			sb.append((nullValue)?"1":"0");
		}else{			
			if (logicop == LogicalOperator.AND) sb.append("&[");
			else sb.append("|[");
			for (int i=0 ; i < criteria.size() ; i++){
				sb.append(criteria.get(i).getSaveString());
				sb.append("#");
			}	
			sb.append("]");
		}
		return sb.toString();
	}

	@Override
	public String parseSaveString(String saveString){
		if (!saveString.startsWith("&") && !saveString.startsWith("|")){
			String[] parts = saveString.split("\\!");
			String f = parts[0];
			ComparisonOperator c = ComparisonOperator.valueOf(parts[1]);
			List<String> v = new ArrayList<String>();
			for (String s : parts[2].split("\\?")){
				if (s.length() > 0) v.add(s);
			}
			List<String> p = new ArrayList<String>();
			for (String s : parts[3].split("\\?")){
				if (s.length() > 0) p.add(s);
			}
			String list = "(";
			if (!v.isEmpty()) list += v.toString().substring(1,v.toString().length()-1);
			if (!v.isEmpty()&&!p.isEmpty()) list += ", ";
			if (!p.isEmpty()) list += p.toString();
			list +=")";
			boolean in = (parts[4].equals("1"));
			boolean nval = (parts.length == 6) ? (parts[5].equals("1")) : false;
			return f + " " + c.getUnicode() + " " + (nval?"NULL":(list + " " + (in?"{+}":"{-}")));
		}else{			
			LogicalOperator lo;
			if (saveString.startsWith("&")) lo = LogicalOperator.AND;
			else lo = LogicalOperator.OR;
			List<String> crit = new ArrayList<String>();
			StringBuilder sb = new StringBuilder();
			int level = 0;
			for (int c=2 ; c < saveString.length() ; c++){
				if (saveString.charAt(c) == '&' || saveString.charAt(c) == '|'){
					level++;
					sb.append(saveString.charAt(c));
				}else if (saveString.charAt(c) == ']'){
					if (level > 0){
						level--;
						sb.append(saveString.charAt(c));
					}else{
						//end of string
						sb = new StringBuilder();
					}
				}else if (saveString.charAt(c) == '#'){
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
				if (crit.size() > 1) sb.append("(");
				sb.append(crit.get(i));
				if (crit.size() > 1) sb.append(")");
				if (i < crit.size()-1) sb.append(" " + lo + " ");
			}
			return sb.toString();
		}		
	}

	@Override
	public Filter loadCriterion(FilteringPanel filteringPanel, String saveString) {		
		if (!saveString.startsWith("&") && !saveString.startsWith("|")){
			String[] parts = saveString.split("\\!");
			Field f = Field.getField(parts[0]);
			ComparisonOperator c = ComparisonOperator.valueOf(parts[1]);
			List<String> v = new ArrayList<String>();
			for (String s : parts[2].split("\\?")){
				if (s.length() > 0) v.add(s);
			}
			List<String> p = new ArrayList<String>();
			for (String s : parts[3].split("\\?")){
				if (s.length() > 0) p.add(s);
			}
			boolean in = (parts[4].equals("1"));
			boolean nval = (parts.length == 6) ? (parts[5].equals("1")) : false;
			return new CustomFilter(filteringPanel, f, c, nval, v, p, in);
		}else{			
			LogicalOperator lo;
			if (saveString.startsWith("&")) lo = LogicalOperator.AND;
			else lo = LogicalOperator.OR;
			List<CustomFilter> crit = new ArrayList<CustomFilter>();
			StringBuilder sb = new StringBuilder();
			int level = 0;
			for (int c=2 ; c < saveString.length() ; c++){
				if (saveString.charAt(c) == '&' || saveString.charAt(c) == '|'){
					level++;
					sb.append(saveString.charAt(c));
				}else if (saveString.charAt(c) == ']'){
					if (level > 0){
						level--;
						sb.append(saveString.charAt(c));
					}else{
						//end of string
						sb = new StringBuilder();
					}
				}else if (saveString.charAt(c) == '#'){
					if (level > 0){
						sb.append(saveString.charAt(c));
					}else{
						crit.add((CustomFilter)loadCriterion(filteringPanel, sb.toString()));
						sb = new StringBuilder();
					}
				}else{
					sb.append(saveString.charAt(c));
				}
			}			
			return new CustomFilter(filteringPanel, lo, crit);
		}
	}

	@Override
	public boolean isFilterValid(){
		if (isSimple){
			if (!checkProfileValues()) return false;
			if (!checkFieldCompatibility(Highlander.getCurrentAnalysis())) return false;
			return true;
		}else{
			boolean ok = true;
			for (CustomFilter crit : criteria){
				if (!crit.isFilterValid()) ok = false;
			}
			return ok;
		}
	}

	@Override
	public List<String> getValidationProblems(){
		List<String> problems = new ArrayList<String>();
		if (isSimple){
			if (!checkProfileValues()) problems.add("A profile list used in a Custom Filter has not been found");
			if (!checkFieldCompatibility(Highlander.getCurrentAnalysis())) problems.add("A field used in a Custom Filter does not exist in this analysis");
		}else{
			for (CustomFilter crit : criteria){
				problems.addAll(crit.getValidationProblems());
			}
		}
		return problems;
	}

	public boolean checkProfileValues(){
		if (isSimple){
			boolean ok = true;
			for (String prof : profileValues){
				try{
					if (Highlander.getLoggedUser().loadValues(field, prof).isEmpty()){
						ok = false;
					}
				}catch(Exception ex){
					Tools.exception(ex);
					ok = false;
				}
			}
			return ok;
		}else{
			boolean ok = true;
			for (CustomFilter crit : criteria){
				if (!crit.checkProfileValues()) ok = false;
			}
			return ok;
		}
	}

	public String getFirstInexistantProfileList(){
		if (isSimple){
			for (String prof : profileValues){
				try{
					if (Highlander.getLoggedUser().loadValues(field, prof).isEmpty())
						return prof;
				}catch(Exception ex){
					Tools.exception(ex);
					return prof;
				}
			}
			return null;
		}else{
			for (CustomFilter crit : criteria){
				String guilty = crit.getFirstInexistantProfileList();
				if (crit.getFirstInexistantProfileList() != null) 
					return guilty;
			}
			return null;
		}
	}

	@Override
	public boolean checkFieldCompatibility(Analysis analysis){
		if (isSimple){
			return field.hasAnalysis(analysis);
		}else{
			boolean ok = true;
			for (CustomFilter crit : criteria){
				if (!crit.checkFieldCompatibility(analysis)) ok = false;
			}
			return ok;
		}
	}

	@Override
	public boolean changeAnalysis(Analysis analysis){
		try{
			if (isSimple){
				if (field.hasAnalysis(analysis)){
					return true;
				}else{
					return false;
				}
			}else{
				boolean ok = true;
				for (CustomFilter crit : criteria){
					if (!crit.changeAnalysis(analysis)) ok = false;
				}
				return ok;
			}
		}catch (Exception ex){
			Tools.exception(ex);
			return false;
		}
	}

	private void displayCriterionPanel(){
		removeAll();

		setLayout(new BorderLayout(6,1));
		String labtxt = ((field != null)?field.getName():"");
		label = new JLabel(labtxt);		
		label.setToolTipText(toHtmlString());
		add(label,BorderLayout.CENTER);
		JPanel west = new JPanel();
		west.setPreferredSize(new Dimension(2,2));
		add(west,BorderLayout.WEST);
		buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BorderLayout());
		add(buttonsPanel,BorderLayout.EAST);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editFilter();
				}
			}
		});
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editFilter();
				}
			}
		});
		//setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
	}

	private void displayCriteriaListPanel(){
		removeAll();

		setLayout(new BorderLayout(1,1));
		JPanel center = new JPanel();		
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEADING, 3, 0);
		center.setLayout(flowLayout);
		for (int i=0 ; i < criteria.size() ; i++){
			center.add(criteria.get(i));			
			switch(logicop){
			case AND:
				criteria.get(i).setBorder(BorderFactory.createLineBorder(new Color(96,74,123),2));
				if (i < criteria.size()-1){
					JLabel labAnd = new JLabel(Resources.getScaledIcon(Resources.iFilterAnd, 16));
					center.add(labAnd);
				}
				break;
			case OR:
				criteria.get(i).setBorder(BorderFactory.createLineBorder(new Color(244,116,20),2));
				if (i < criteria.size()-1){
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
		if (getParent() != null && getParent().getParent() instanceof CustomFilter){		
			buttonsPanel = new JPanel();
			buttonsPanel.setLayout(new BorderLayout());
			add(buttonsPanel,BorderLayout.EAST);

			JButton removeButton = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
			removeButton.setToolTipText("Delete criterion");	
			removeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable(){
						@Override
						public void run(){
							delete();
						}
					}, "CustomFilter.delete").start();
				}
			});
			removeButton.setBorder(null);
			removeButton.setBorderPainted(false);
			removeButton.setContentAreaFilled(false);
			removeButton.setMargin(new Insets(0, 0, 0, 0));
			buttonsPanel.add(removeButton,BorderLayout.SOUTH);

			JButton addButton = null;
			switch(logicop){
			case OR:
				addButton = new JButton(Resources.getScaledIcon(Resources.iFilterAddOr, 16));
				addButton.setToolTipText("Add a new sub-criterion to the filter, using the logical operator OR (i.e. filter will be the UNION of sub-criteria)");
				addButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addCriterion(LogicalOperator.OR);
							}
						}, "CustomFilter.addCriterion").start();
					}
				});
				break;
			case AND:
				addButton = new JButton(Resources.getScaledIcon(Resources.iFilterAddAnd, 16));
				addButton.setToolTipText("Add a new sub-criterion to the filter, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-criteria)");
				addButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								addCriterion(LogicalOperator.AND);
							}
						}, "CustomFilter.addCriterion").start();
					}
				});
				break;
			}
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
			details.append(field.getName());
			details.append(" ");
			details.append(compop.getUnicode());			
			details.append(" ");
			if (nullValue){
				details.append("NULL");
			}else{
				for (String s : profileValues) {
					details.append("["+s+"] ");
				}
				for (String s : values) {
					details.append(s+" ");
				}
				if (includeNulls)
					details.append("{+}");
				else
					details.append("{-}");				
			}
		}else{
			for (int i=0 ; i < criteria.size() ; i++){
				if (criteria.size() > 1) details.append("(");
				details.append(criteria.get(i).toString());
				if (criteria.size() > 1) details.append(")");
				if (i < criteria.size()-1){
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
		StringBuilder details = new StringBuilder();
		details.append("<html>");
		if (isSimple){
			details.append(field.getName());
			details.append(" ");
			details.append(compop.getHtml());
			details.append(" ");
			if (nullValue){
				details.append("[<i>NULL</i>] ");
			}else{
				for (String s : profileValues) {
					if (details.length() % 50 == 0) details.append("<br>");
					details.append("[<i>"+s+"</i>] ");
				}
				for (String s : values) {
					if (details.length() % 50 == 0) details.append("<br>");
					details.append(s+" ");
				}			
				details.append("<br>");
				if (includeNulls)
					details.append("Including variant with no values in results");
				else
					details.append("Excluding variant with no values from results");
			}
		}else{
			details.append(toString().replace(" AND ", "<br>AND<br>").replace(" OR ", "<br>OR<br>"));
		}
		details.append("</html>");
		return details.toString();
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

	public void addCriterion(LogicalOperator operator){
		CreateCustomFilter cfc = new CreateCustomFilter(Highlander.getCurrentAnalysis(), filteringPanel);
		Tools.centerWindow(cfc, false);
		cfc.setVisible(true);
		if(cfc.getCriterion() != null){			
			addCriterion(cfc.getCriterion(), operator);
		}
	}

	public void addCriterion(CustomFilter criterion, LogicalOperator operator){
		if (isSimple){
			CustomFilter crit1 = new CustomFilter(filteringPanel, field, compop, nullValue, values, profileValues, includeNulls);
			CustomFilter crit2 = criterion;
			//Destroy everything created for simple criterion
			isSimple = false;
			field = null;
			compop = null;
			nullValue = false;
			values.clear();
			profileValues.clear();
			includeNulls = true;
			removeAll();
			//Create everything for complex criterion
			isComplex = true;
			logicop = operator;
			addCriterion(crit1);
			addCriterion(crit2);
			displayCriteriaListPanel();
		}else{
			addCriterion(criterion);
			displayCriteriaListPanel();
		}
		//refresh main table
		filteringPanel.refresh();
	}

	private void addCriterion(final CustomFilter criterion){	
		if (isComplex){
			criterion.parentFilter = this;
			criteria.add(criterion);
			JButton addButton = null;
			switch(logicop){
			case AND:
				addButton = new JButton(Resources.getScaledIcon(Resources.iFilterAddOr, 16));
				addButton.setToolTipText("Add a new sub-criterion to the filter, using the logical operator OR (i.e. filter will be the UNION of sub-criteria)");
				addButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								criterion.addCriterion(LogicalOperator.OR);
							}
						}, "CustomFilter.addCriterion").start();
					}
				});
				break;
			case OR:
				addButton = new JButton(Resources.getScaledIcon(Resources.iFilterAddAnd, 16));
				addButton.setToolTipText("Add a new sub-criterion to the filter, using the logical operator AND (i.e. filter will be the INTERSECTION of sub-criteria)");
				addButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						new Thread(new Runnable(){
							@Override
							public void run(){
								criterion.addCriterion(LogicalOperator.AND);
							}
						}, "CustomFilter.addCriterion").start();
					}
				});
				break;
			}
			addButton.setBorder(null);
			addButton.setBorderPainted(false);
			addButton.setContentAreaFilled(false);
			addButton.setMargin(new Insets(0, 0, 0, 0));
			criterion.buttonsPanel.add(addButton,BorderLayout.NORTH);
			JButton removeButton = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
			removeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable(){
						@Override
						public void run(){
							criterion.delete();
						}
					}, "CustomFilter.delete").start();
				}
			});
			removeButton.setToolTipText("Delete criterion");	
			removeButton.setBorder(null);
			removeButton.setBorderPainted(false);
			removeButton.setContentAreaFilled(false);
			removeButton.setMargin(new Insets(0, 0, 0, 0));
			criterion.buttonsPanel.add(removeButton,BorderLayout.SOUTH);

		}
	}

	public void removeCriterion(CustomFilter criterion){
		if (isComplex){
			if(criteria.size() > 2){
				criteria.remove(criterion);				
				displayCriteriaListPanel();
			}else{
				criteria.remove(criterion);				
				CustomFilter orphan = criteria.get(0);
				if (getParentFilter().getFilterType() == FilterType.CUSTOM){
					CustomFilter parent = (CustomFilter)getParentFilter();
					CustomFilter newChild;
					if (orphan.isSimple){
						newChild = new CustomFilter(orphan.filteringPanel, orphan.field, orphan.compop, orphan.nullValue, orphan.values, orphan.profileValues, orphan.includeNulls);
					}else{
						newChild = new CustomFilter(orphan.filteringPanel, logicop, orphan.criteria);
					}
					parent.criteria.remove(this);
					parent.addCriterion(newChild);
					parent.displayCriteriaListPanel();
				}else if (getParentFilter().getFilterType() == FilterType.COMBO){
					ComboFilter parent = (ComboFilter)getParentFilter();
					CustomFilter newChild;
					if (orphan.isSimple){
						newChild = new CustomFilter(orphan.filteringPanel, orphan.field, orphan.compop, orphan.nullValue, orphan.values, orphan.profileValues, orphan.includeNulls);
					}else{
						newChild = new CustomFilter(orphan.filteringPanel, orphan.logicop, orphan.criteria);
					}
					if (parent.isSimple()){
						parent.filter = newChild;
						newChild.parentFilter = parent;
						parent.displayFilterPanel();
					}
				}
			}
		}else{
			System.err.println("Impossible filter situation in CustomFilter.removeCriterion(CustomFilter)");
		}
	}

	@Override
	public void editFilter(){
		if (isSimple){
			CreateCustomFilter cfc = new CreateCustomFilter(Highlander.getCurrentAnalysis(), filteringPanel, this);
			Tools.centerWindow(cfc, false);
			cfc.setVisible(true);
			CustomFilter edited = cfc.getCriterion();
			if(edited != null){			
				field = edited.getField();
				compop = edited.getComparisonOperator();
				nullValue = edited.getNullValue();
				values = edited.getValues();
				profileValues = edited.getProfileValues();
				includeNulls = edited.getIncludeNulls();
				filteringPanel.refresh();
				String labtxt = ((field != null)?field.getName():"");
				label.setText(labtxt);		
				label.setToolTipText(toHtmlString());
			}
		}
	}

	@Override
	public void delete(){
		if (getParentFilter().getFilterType() == FilterType.CUSTOM){
			//First case : we are in a complex CustomFilter
			CustomFilter parent = (CustomFilter)getParentFilter();
			parent.removeCriterion(this);
		}else	if (getParentFilter().getFilterType() == FilterType.COMBO){
			//Second case : we are in a simple CustomFilter, so it MUST be inside a simple ComboFilter
			ComboFilter parent = (ComboFilter)getParentFilter();
			parent.delete();
		}else{
			System.err.println("Impossible filter situation in CustomFilter.delete()");
		}
		//Other cases should not be possible
		filteringPanel.refresh();
	}

	@Override
	public int getNumberOfVariants() {
		return nVariants;
	}

	@Override
	public Map<Integer,String> getResultIds(Set<String> autoSamples) throws Exception {
		nVariants = -1;
		List<Field> headers = new ArrayList<Field>();
		headers.add(Field.variant_sample_id);
		headers.add(Field.chr);
		headers.add(Field.pos);
		headers.add(Field.length);
		headers.add(Field.reference);
		headers.add(Field.alternative);
		headers.add(Field.gene_symbol);
		VariantResults variantResults = retreiveData(headers, autoSamples, "Custom filter");
		Map<Integer,String> ids = new HashMap<Integer, String>();
		for (int i=0 ; i < variantResults.id.length ; i++){
			ids.put(variantResults.id[i], variantResults.variant[i]);
		}
		nVariants = ids.size();
		return ids;
	}

	@Override
	public List<Field> getQueryWhereFields() throws Exception {
		List<Field> list = new ArrayList<Field>();
		if (isSimple){
			list.add(field);
		}else{			
			for (int i=0 ; i < criteria.size() ; i++){
				list.addAll(criteria.get(i).getQueryWhereFields());
			}			
		}
		return list;	
	}

	@Override
	public String getQueryWhereClause(boolean includeTableWithJoinON) throws Exception {
		return getQueryWhereClause(false, null, includeTableWithJoinON);
	}

	private String getSamplesWhereClause(boolean includeTableWithJoinON) throws Exception {
		return getQueryWhereClause(true, null, includeTableWithJoinON);
	}
	
	public String getExternalSourceQueryWhereClause(String chromosome, Set<String> availableFields, boolean includeTableWithJoinON) throws Exception {
		//TODO BURDEN - potentiellement plus nécessaire ou à modifier
		return getQueryWhereClause(false, chromosome, includeTableWithJoinON);
	}
	
	private String getQueryWhereClause(boolean forSample, String chromosome, boolean includeTableWithJoinON) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (isSimple){
			if (!forSample || field.isSampleRelated()){
				if (nullValue){
					String queryField = (chromosome != null) ? "`chromosome_"+chromosome + "`.`" + field.getName() + "`" : field.getQueryWhereName(Highlander.getCurrentAnalysis(), includeTableWithJoinON);
					if (compop == ComparisonOperator.EQUAL){
						if (field.getDefaultValue() != null) sb.append("("+queryField + " = '"+field.getDefaultValue()+"' OR "+queryField + " IS NULL)");
						else sb.append(queryField + " IS NULL");
					}else{
						if (field.getDefaultValue() != null) sb.append("("+queryField + " != '"+field.getDefaultValue()+"' AND "+queryField + " IS NOT NULL)");
						else sb.append(queryField + " IS NOT NULL");
					}
				}else{
					Set<String> vals = new HashSet<String>();
					vals.addAll(values);
					for (String prof : profileValues){
						for (String val : Highlander.getLoggedUser().loadValues(field, prof)){
							vals.add(val.toUpperCase());
						}
					}
					String val = Highlander.getDB().format(Schema.HIGHLANDER, vals.iterator().next());
					if (includeNulls && field.getDefaultValue() == null) sb.append("(");
					String queryField = (chromosome != null) ? "`chromosome_"+chromosome + "`.`" + field.getName() + "`" : field.getQueryWhereName(Highlander.getCurrentAnalysis(), includeTableWithJoinON);
					if (field.getFieldClass() == Boolean.class){
						if (val.equalsIgnoreCase("1") || val.equalsIgnoreCase("true")){
							sb.append(queryField + " = TRUE");
						}else{
							sb.append(queryField + " = FALSE");
						}
					}else{
						switch (compop){
						case EQUAL:
							if (field.getDefaultValue() != null && (
									(field.getDefaultValue().equals("0") && vals.contains("0")) ||
									(field.getDefaultValue().equals("NOT_CHECKED") && vals.contains("NOT_CHECKED"))
									)) {
								sb.append("(" + queryField + " IN ("+HighlanderDatabase.makeSqlList(vals, field.getFieldClass())+") OR " + queryField + " IS NULL)");
							}else {
								sb.append(queryField + " IN ("+HighlanderDatabase.makeSqlList(vals, field.getFieldClass())+")");
							}
							break;
						case DIFFERENT:
							if (field.getDefaultValue() != null && (
									(field.getDefaultValue().equals("0") && vals.contains("0")) ||
									(field.getDefaultValue().equals("NOT_CHECKED") && vals.contains("NOT_CHECKED"))
									)) {
								sb.append("(" + queryField + " NOT IN ("+HighlanderDatabase.makeSqlList(vals, field.getFieldClass())+") AND " + queryField + " IS NOT NULL)");
							}else if (field.getDefaultValue() != null && (
									field.getDefaultValue().equals("0") ||	
									field.getDefaultValue().equals("NOT_CHECKED") || 
									field.getDefaultValue().equals("")
									)) {
								sb.append("(" + queryField + " NOT IN ("+HighlanderDatabase.makeSqlList(vals, field.getFieldClass())+") OR " + queryField + " IS NULL)");
							}else {
								sb.append(queryField + " NOT IN ("+HighlanderDatabase.makeSqlList(vals, field.getFieldClass())+")");
							}
							break;
						case GREATER:
							if (field.getFieldClass() == OffsetDateTime.class) sb.append(queryField + " > '" + val + "'");
							else sb.append(queryField + " > " + val);
							break;
						case GREATEROREQUAL:
							if (field.getFieldClass() == OffsetDateTime.class) {
								sb.append(queryField + " >= '" + val + "'");
							}else if (field.getDefaultValue() != null && field.getDefaultValue().equals("0")) {
								sb.append("(" + queryField + " >= " + val + " OR " + queryField + " IS NULL)");
							}else {
								sb.append(queryField + " >= " + val);
							}
							break;
						case SMALLER:
							if (field.getFieldClass() == OffsetDateTime.class) {
								sb.append(queryField + " < '" + val + "'");
							}else if (field.getDefaultValue() != null && field.getDefaultValue().equals("0")) {
								sb.append("(" + queryField + " < " + val + " OR " + queryField + " IS NULL)");
							}else {
								sb.append(queryField + " < " + val);
							}
							break;
						case SMALLEROREQUAL:
							if (field.getFieldClass() == OffsetDateTime.class) {
								sb.append(queryField + " <= '" + val + "'");
							}else if (field.getDefaultValue() != null && field.getDefaultValue().equals("0")) {
								sb.append("(" + queryField + " <= " + val + " OR " + queryField + " IS NULL)");
							}else {
								sb.append(queryField + " <= " + val);
							}
							break;
						case CONTAINS:
							sb.append("INSTR("+queryField+", '" + val + "') > 0");
							break;
						case DOESNOTCONTAINS:
							if (field.getDefaultValue() != null && field.getDefaultValue().equals("")) {
								sb.append("(INSTR("+queryField+", '" + val + "') <= 0 OR " + queryField + " IS NULL)");
							}else {
								sb.append("INSTR("+queryField+", '" + val + "') <= 0");
							}
							break;
						default:
							throw new Exception("Comparison operator " + compop + " not supported here");
						}
					}
					if (includeNulls && field.getDefaultValue() == null) sb.append(" OR " + queryField + " IS NULL)");
				}
			}
		}else{			
			for (int i=0 ; i < criteria.size() ; i++){
				sb.append("(");
				sb.append(criteria.get(i).getQueryWhereClause(forSample, chromosome, includeTableWithJoinON));
				sb.append(")");
				if (i < criteria.size()-1) sb.append(" " + logicop + " ");
			}			
		}
		String query = sb.toString();
		query = query.replaceAll("\\(\\) " + logicop + " ", "");
		query = query.replaceAll(" " + logicop + " \\(\\)", "");
		query = query.replaceAll("\\(\\)", "");
		return query;		
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
		res.close();
		if (Highlander.waitingPanel.isCancelled()) {
			throw new CancelException();
		}
		return new VariantResults(headers, dataMap, variants);
	}

}
