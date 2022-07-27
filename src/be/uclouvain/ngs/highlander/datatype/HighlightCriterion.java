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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateHighlightCriterion;
import be.uclouvain.ngs.highlander.UI.toolbar.HighlightingPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter.ComparisonOperator;

public class HighlightCriterion extends HighlightingRule {

	private HighlightingPanel highlightingPanel;

	private ComparisonOperator compop = null;
	private boolean nullValue = false;
	private List<String> values = new ArrayList<String>();
	private List<String> profileValues = new ArrayList<String>();
	private Set<String> allValues = new HashSet<String>();
	private Color background;
	private Color foreground;
	private boolean bold;
	private boolean italic;
	private JLabel label;
	private boolean expandRow;

	public HighlightCriterion(HighlightingPanel highlightingPanel, Field field, ComparisonOperator compop, boolean nullValue, List<String> values, List<String> profileValues, 
			Color background, Color foreground, boolean bold, boolean italic, boolean expandRow){
		this.highlightingPanel = highlightingPanel;
		this.field = field;
		this.compop = compop;
		this.nullValue = nullValue;
		this.values.addAll(values);
		this.profileValues.addAll(profileValues);
		this.background = background;
		this.foreground = foreground;
		this.bold = bold;
		this.italic = italic;
		this.expandRow = expandRow;
		fetchValuesFromProfile();
		displayCriterionPanel();
	}

	public HighlightCriterion(){

	}

	private void fetchValuesFromProfile(){
		if (!checkProfileValues()){
			JOptionPane.showMessageDialog(this, "Cannot retreive '"+getFirstInexistantProfileList()+"' value list from your profile." +
					"\nVerify that this list has not been deleted, renamed or is empty.", "Creating highlighting criterion",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserListDelete,64));
		}
		allValues = new HashSet<String>();
		allValues.addAll(values);
		for (String prof : profileValues){
			try{
				for (String val : Highlander.getLoggedUser().loadValues(field, prof)){
					allValues.add(val.toUpperCase());
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
	}

	public RuleType getRuleType(){
		return RuleType.HIGHLIGHTING;
	}

	public ComparisonOperator getComparisonOperator() {
		return compop;
	}

	public List<String> getValues() {
		return values;
	}

	public List<String> getProfileValues() {
		return profileValues;
	}

	public boolean getNullValue(){
		return nullValue;
	}

	public Color getBackground() {
		return background;
	}

	public Color getForeground() {
		return foreground;
	}

	public boolean isBold() {
		return bold;
	}

	public boolean isItalic() {
		return italic;
	}

	public boolean expandRow() {
		return expandRow;
	}

	public String getSaveString(){
		StringBuilder sb = new StringBuilder();
		sb.append(getRuleType()+"|");
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
		sb.append((background != null)?""+background.getRGB():"-");
		sb.append("!");
		sb.append((foreground != null)?""+foreground.getRGB():"-");
		sb.append("!");
		sb.append((bold)?"1":"0");
		sb.append("!");
		sb.append((italic)?"1":"0");
		sb.append("!");
		sb.append((nullValue)?"1":"0");
		sb.append("!");
		sb.append((expandRow)?"1":"0");
		return sb.toString();
	}

	public JLabel parseSaveString(String saveString){
		String[] main = saveString.split("\\|");
		if (!main[0].equals(getRuleType().toString())){
			System.err.println("The following string is not an "+ getRuleType() +" : " + saveString);
			return new JLabel("Error !");
		}
		String[] parts = main[1].split("\\!");
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
		boolean nval = (parts.length == 6) ? (parts[5].equals("1")) : false;
		JLabel label = new JLabel(f + " " + c.getUnicode() + " " + (nval?"NULL":list), Resources.getScaledIcon(Resources.iHighlighting, 16), JLabel.CENTER);
		if (!parts[4].equals("-")) label.setBackground(new Color(Integer.parseInt(parts[4])));
		if (!parts[5].equals("-")) label.setForeground(new Color(Integer.parseInt(parts[5])));
		if (parts[6].equals("1")) label.setFont(label.getFont().deriveFont(Font.BOLD));
		if (parts[7].equals("1")) label.setFont(label.getFont().deriveFont(Font.ITALIC));
		if (parts[6].equals("1") && parts[7].equals("1")) label.setFont(label.getFont().deriveFont(Font.ITALIC | Font.BOLD));
		return label;
	}

	public HighlightCriterion loadCriterion(HighlightingPanel highlightingPanel, String saveString) throws Exception {		
		String[] main = saveString.split("\\|");
		if (!main[0].equals(getRuleType().toString())){
			throw new Exception("The following string is not an "+ getRuleType() +" : " + saveString);
		}
		String[] parts = main[1].split("\\!");
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
		Color back = parts[4].equals("-") ? null : new Color(Integer.parseInt(parts[4]));
		Color fore = parts[5].equals("-") ? null : new Color(Integer.parseInt(parts[5]));
		boolean b = (parts[6].equals("1"));
		boolean i = (parts[7].equals("1"));
		boolean n = (parts.length >= 9) ? (parts[8].equals("1")) : false;
		boolean e = (parts.length >= 10) ? (parts[9].equals("1")) : false;
		return new HighlightCriterion(highlightingPanel, f, c, n, v, p, back, fore, b, i, e);
	}

	public boolean checkProfileValues(){
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
	}

	public String getFirstInexistantProfileList(){
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
	}

	private void displayCriterionPanel(){
		setLayout(new BorderLayout(6,1));
		setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
		String labtxt = ((field != null)?field.getName():"");
		label = new JLabel(labtxt);
		label.setOpaque(true);
		label.setToolTipText(toHtmlString());
		if (background != null) label.setBackground(background);
		if (foreground != null) label.setForeground(foreground);
		if (bold) label.setFont(label.getFont().deriveFont(Font.BOLD));
		if (italic) label.setFont(label.getFont().deriveFont(Font.ITALIC));
		if (bold && italic) label.setFont(label.getFont().deriveFont(Font.ITALIC | Font.BOLD));
		add(label,BorderLayout.CENTER);
		JPanel west = new JPanel(new GridBagLayout());
		JLabel iconLab = new JLabel(Resources.getScaledIcon(Resources.iHighlighting, 16));
		iconLab.setOpaque(true);
		west.add(iconLab, new GridBagConstraints(0, 0, 0, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
		add(west,BorderLayout.WEST);
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new GridBagLayout());
		JButton removeButton = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						delete();
					}
				}, "HighlightCriterion.delete").start();
			}
		});
		removeButton.setToolTipText("Delete criterion");	
		removeButton.setBorder(null);
		removeButton.setBorderPainted(false);
		removeButton.setContentAreaFilled(false);
		removeButton.setMargin(new Insets(0, 0, 0, 0));
		buttonsPanel.add(removeButton,new GridBagConstraints(0, 0, 0, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
		add(buttonsPanel,BorderLayout.EAST);
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editCriterion();
				}
			}
		});
		label.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editCriterion();
				}
			}
		});
	}

	public String toString(){
		StringBuilder details = new StringBuilder();
		details.append(field.getName());
		details.append(" ");
		if (compop.toString().startsWith("RANGE")){
			details.append(" in range ");
		}
		details.append(compop.getUnicode());			
		if (compop.toString().startsWith("RANGE")){
			details.replace(details.lastIndexOf("\u002E\u002C"), details.lastIndexOf("\u002E\u002C")+1, values.get(0));
			details.replace(details.lastIndexOf("\u002C\u002E")+1, details.lastIndexOf("\u002C\u002E")+2, values.get(1));
		}else{
			if (nullValue){
				details.append(" NULL");
			}else{
				details.append(" ");
				for (String s : profileValues) {
					details.append("["+s+"] ");
				}
				for (String s : values) {
					details.append(s+" ");
				}
			}
		}
		details.append("{");
		if (background != null) details.append("bg " + background.getRGB() + ", ");
		if (foreground != null) details.append("fg " + foreground.getRGB() + ", ");
		if (bold) details.append("bold, ");
		if (italic) details.append("italic, ");
		if (expandRow) details.append("row, "); 
		else details.append("cell, ");
		if (details.charAt(details.length()-1) == '{'){
		}else{
			details.delete(details.length()-2, details.length());
			details.append("}");
		}
		return details.toString();
	}

	public String toHtmlString(){
		StringBuilder details = new StringBuilder();
		details.append("<html>");
		StringBuilder formating = new StringBuilder();
		if (background != null) formating.append("background-color:rgb("+background.getRed()+","+background.getGreen()+","+background.getBlue()+");");
		if (foreground != null) formating.append("color:rgb("+foreground.getRed()+","+foreground.getGreen()+","+foreground.getBlue()+");");
		if (bold) formating.append("font-weight:bold;");
		if (italic) formating.append("font-style:italic;");
		if (formating.length() > 0)	details.append("<body style=\""+formating.toString()+"\">");
		if (expandRow) details.append("*");
		details.append(field.getName());
		details.append(" ");
		if (compop.toString().startsWith("RANGE")){
			details.append(" in range ");
		}
		details.append(compop.getHtml());
		if (compop.toString().startsWith("RANGE")){
			details.replace(details.lastIndexOf("\u002E\u002C"), details.lastIndexOf("\u002E\u002C")+1, values.get(0));
			details.replace(details.lastIndexOf("\u002C\u002E")+1, details.lastIndexOf("\u002C\u002E")+2, values.get(1));
		}else{
			if (nullValue){
				details.append("[<i>NULL</i>] ");
			}else{
				details.append(" ");
				for (String s : profileValues) {
					if (details.length() % 50 == 0) details.append("<br>");
					details.append("[<i>"+s+"</i>] ");
				}
				for (String s : values) {
					if (details.length() % 50 == 0) details.append("<br>");
					details.append(s+" ");
				}
			}
		}
		if (formating.length() > 0)	details.append("</body>");
		details.append("</html>");
		return details.toString();
	}

	public void editCriterion(){
		CreateHighlightCriterion cfc = new CreateHighlightCriterion(Highlander.getCurrentAnalysis(), highlightingPanel, this);
		Tools.centerWindow(cfc, false);
		cfc.setVisible(true);
		HighlightCriterion edited = cfc.getCriterion();
		Highlander.getCellRenderer().removeHighlighting(this);
		if(edited != null){			
			field = edited.getField();
			compop = edited.getComparisonOperator();
			nullValue = edited.getNullValue();
			values = edited.getValues();
			profileValues = edited.getProfileValues();
			background = edited.getBackground();
			foreground = edited.getForeground();
			bold = edited.isBold();
			italic = edited.isItalic();
			expandRow = edited.expandRow();
			String labtxt = ((field != null)?field.getName():"");
			label.setText(labtxt);		
			label.setToolTipText(toHtmlString());
			if (background != null) label.setBackground(background);
			if (foreground != null) label.setForeground(foreground);
			if (bold) label.setFont(label.getFont().deriveFont(Font.BOLD));
			if (italic) label.setFont(label.getFont().deriveFont(Font.ITALIC));
			if (bold && italic) label.setFont(label.getFont().deriveFont(Font.ITALIC | Font.BOLD));
			fetchValuesFromProfile();
		}
		Highlander.getCellRenderer().addHighlighting(this);
		highlightingPanel.refresh();		
	}

	public void delete(){
		highlightingPanel.getCriteriaPanel().remove(HighlightCriterion.this);
		Highlander.getCellRenderer().removeHighlighting(this);
		highlightingPanel.refresh();		
	}

	public boolean isHighlighted(Object value) throws Exception {
		if (nullValue){
			if (compop == ComparisonOperator.EQUAL) return (value == null);
			else return (value != null); 
		}
		if (value == null) return false;
		if (field.getFieldClass() == Boolean.class){				
			if (Boolean.parseBoolean(value.toString())){
				return (allValues.contains("1") || allValues.contains("TRUE"));
			}else{
				return (allValues.contains("0") || allValues.contains("FALSE"));
			}
		}else{					
			String val = value.toString().toUpperCase();
			String crit = allValues.iterator().next();
			String rangeStart = "", rangeEnd = "";
			if (values.size() > 0) rangeStart = values.get(0);
			if (values.size() > 1) rangeEnd = values.get(1);
			switch (compop){
			case EQUAL:		
				return allValues.contains(val);
			case DIFFERENT:
				return !allValues.contains(val);
			case GREATER:
				if (field.getFieldClass() == Double.class)
					return Double.parseDouble(val) > Double.parseDouble(crit);
					else if (field.getFieldClass() == Integer.class)	
						return Integer.parseInt(val) > Integer.parseInt(crit);
						else if (field.getFieldClass() == Long.class)
							return Long.parseLong(val) > Long.parseLong(crit);
							else if (field.getFieldClass() == Timestamp.class)
								return Timestamp.valueOf(crit).compareTo(Timestamp.valueOf(val)) > 0;
								else
									throw new Exception("Field class " + field.getFieldClass() + " not supported for >");
			case GREATEROREQUAL:
				if (field.getFieldClass() == Double.class)
					return Double.parseDouble(val) >= Double.parseDouble(crit);
					else if (field.getFieldClass() == Integer.class)	
						return Integer.parseInt(val) >= Integer.parseInt(crit);
						else if (field.getFieldClass() == Long.class)
							return Long.parseLong(val) >= Long.parseLong(crit);
							else if (field.getFieldClass() == Timestamp.class)
								return Timestamp.valueOf(crit).compareTo(Timestamp.valueOf(val)) >= 0;
								else
									throw new Exception("Field class " + field.getFieldClass() + " not supported for >=");
			case SMALLER:
				if (field.getFieldClass() == Double.class)
					return Double.parseDouble(val) < Double.parseDouble(crit);
				else if (field.getFieldClass() == Integer.class)	
					return Integer.parseInt(val) < Integer.parseInt(crit);
				else if (field.getFieldClass() == Long.class)
					return Long.parseLong(val) < Long.parseLong(crit);
				else if (field.getFieldClass() == Timestamp.class)
					return Timestamp.valueOf(crit).compareTo(Timestamp.valueOf(val)) < 0;
				else
					throw new Exception("Field class " + field.getFieldClass() + " not supported for <");
			case SMALLEROREQUAL:
				if (field.getFieldClass() == Double.class)
					return Double.parseDouble(val) <= Double.parseDouble(crit);
				else if (field.getFieldClass() == Integer.class)	
					return Integer.parseInt(val) <= Integer.parseInt(crit);
				else if (field.getFieldClass() == Long.class)
					return Long.parseLong(val) <= Long.parseLong(crit);
				else if (field.getFieldClass() == Timestamp.class)
					return Timestamp.valueOf(crit).compareTo(Timestamp.valueOf(val)) <= 0;
				else
					throw new Exception("Field class " + field.getFieldClass() + " not supported for <=");
			case CONTAINS:
				for (String cri : allValues){
					if (val.contains(cri)) return true;
				}			
				return false;
			case DOESNOTCONTAINS:
				for (String cri : allValues){
					if (val.contains(cri)) return false;
				}					
				return true;
			case RANGE_II:
				if (field.getFieldClass() == Double.class)
					return (Double.parseDouble(val) >= Double.parseDouble(rangeStart)) && Double.parseDouble(val) <= Double.parseDouble(rangeEnd);
				else if (field.getFieldClass() == Integer.class)	
					return (Integer.parseInt(val) >= Integer.parseInt(rangeStart)) && (Integer.parseInt(val) <= Integer.parseInt(rangeEnd));
				else if (field.getFieldClass() == Long.class)
					return (Long.parseLong(val) >= Long.parseLong(rangeStart)) && (Long.parseLong(val) <= Long.parseLong(rangeEnd));
				else if (field.getFieldClass() == Timestamp.class)
					return (Timestamp.valueOf(rangeStart).compareTo(Timestamp.valueOf(val)) >= 0) && (Timestamp.valueOf(rangeEnd).compareTo(Timestamp.valueOf(val)) <= 0);
				else
					throw new Exception("Field class " + field.getFieldClass() + " not supported for [.,.]");
			case RANGE_IE:
				if (field.getFieldClass() == Double.class)
					return (Double.parseDouble(val) >= Double.parseDouble(rangeStart)) && Double.parseDouble(val) < Double.parseDouble(rangeEnd);
				else if (field.getFieldClass() == Integer.class)	
					return (Integer.parseInt(val) >= Integer.parseInt(rangeStart)) && (Integer.parseInt(val) < Integer.parseInt(rangeEnd));
				else if (field.getFieldClass() == Long.class)
					return (Long.parseLong(val) >= Long.parseLong(rangeStart)) && (Long.parseLong(val) < Long.parseLong(rangeEnd));
				else if (field.getFieldClass() == Timestamp.class)
					return (Timestamp.valueOf(rangeStart).compareTo(Timestamp.valueOf(val)) >= 0) && (Timestamp.valueOf(rangeEnd).compareTo(Timestamp.valueOf(val)) < 0);
				else
					throw new Exception("Field class " + field.getFieldClass() + " not supported for [.,.[");
			case RANGE_EI:
				if (field.getFieldClass() == Double.class)
					return (Double.parseDouble(val) > Double.parseDouble(rangeStart)) && Double.parseDouble(val) <= Double.parseDouble(rangeEnd);
				else if (field.getFieldClass() == Integer.class)	
					return (Integer.parseInt(val) > Integer.parseInt(rangeStart)) && (Integer.parseInt(val) <= Integer.parseInt(rangeEnd));
				else if (field.getFieldClass() == Long.class)
					return (Long.parseLong(val) > Long.parseLong(rangeStart)) && (Long.parseLong(val) <= Long.parseLong(rangeEnd));
				else if (field.getFieldClass() == Timestamp.class)
					return (Timestamp.valueOf(rangeStart).compareTo(Timestamp.valueOf(val)) > 0) && (Timestamp.valueOf(rangeEnd).compareTo(Timestamp.valueOf(val)) <= 0);
				else
					throw new Exception("Field class " + field.getFieldClass() + " not supported for ].,.]");
			case RANGE_EE:
				if (field.getFieldClass() == Double.class)
					return (Double.parseDouble(val) > Double.parseDouble(rangeStart)) && Double.parseDouble(val) < Double.parseDouble(rangeEnd);
				else if (field.getFieldClass() == Integer.class)	
					return (Integer.parseInt(val) > Integer.parseInt(rangeStart)) && (Integer.parseInt(val) < Integer.parseInt(rangeEnd));
				else if (field.getFieldClass() == Long.class)
					return (Long.parseLong(val) > Long.parseLong(rangeStart)) && (Long.parseLong(val) < Long.parseLong(rangeEnd));
				else if (field.getFieldClass() == Timestamp.class)
					return (Timestamp.valueOf(rangeStart).compareTo(Timestamp.valueOf(val)) > 0) && (Timestamp.valueOf(rangeEnd).compareTo(Timestamp.valueOf(val)) < 0);
				else
					throw new Exception("Field class " + field.getFieldClass() + " not supported for ].,.[");
			default:
				throw new Exception("Comparison operator " + compop + " not supported");
			}
		}
	}
}
