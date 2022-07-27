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
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel.CancelException;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;

public abstract class MagicFilter extends Filter {

	protected JLabel label;
	protected ComboFilter preFiltering = null;

	public boolean isSimple(){
		//return true;
		return (preFiltering == null);
	}

	public boolean isComplex(){
		//return false;
		return (preFiltering != null);
	}

	public Filter getSubFilter(int index){
		//return null;
		if (preFiltering != null){
			return getPreFiltering();
		}else{
			return null;
		}
	}

	public int getSubFilterCount(){
		//return 0;
		if (preFiltering != null){
			return 1;
		}else{
			return 0;
		}
	}

	public LogicalOperator getLogicalOperator(){
		return null;
	}

	public ComboFilter getPreFiltering() {
		return preFiltering;
	}

	public boolean hasSamples() {
		if (preFiltering != null){			
			return preFiltering.hasSamples();
		}
		return true;
	}

	public Set<String> getUserDefinedSamples(boolean includeProfileList){
		Set<String> set = getIncludedSamples();
		set.addAll(getExcludedSamples());
		return set;
	}

	public void setFilteringPanel(FilteringPanel filteringPanel){
		this.filteringPanel = filteringPanel;
	}

	protected void displayCriterionPanel(){
		setLayout(new BorderLayout());		
		JPanel critPanel = new JPanel();
		critPanel.setLayout(new BorderLayout(6,1));
		label = new JLabel(getFilterType().getName());		
		label.setToolTipText(toHtmlString());
		critPanel.add(label,BorderLayout.CENTER);
		JPanel west = new JPanel();
		west.setPreferredSize(new Dimension(2,2));
		critPanel.add(west,BorderLayout.WEST);
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BorderLayout());
		critPanel.add(buttonsPanel,BorderLayout.EAST);
		add(critPanel, BorderLayout.CENTER);

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editFilter();
				}
			}
		});
		label.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					editFilter();
				}
			}
		});
		/* Not needed anymore, a Magic Filter is now always in a ComboFilter
		critPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK,2));
		JButton removeButton = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						delete();
					}
				}, "MagicFilter.delete").start();
			}
		});
		removeButton.setToolTipText("Delete criterion");	
		removeButton.setBorder(null);
		removeButton.setBorderPainted(false);
		removeButton.setContentAreaFilled(false);
		removeButton.setMargin(new Insets(0, 0, 0, 0));
		buttonsPanel.add(removeButton,BorderLayout.SOUTH);
		 */
	}

	public void delete(){
		if (getParentFilter().getFilterType() == FilterType.COMBO){
			//Only possible case : we are inside a simple ComboFilter
			ComboFilter parent = (ComboFilter)getParentFilter();
			parent.delete();
		}else{
			System.err.println("Impossible filter situation in MagicFilter.delete()");
		}
		filteringPanel.refresh();
	}

	protected List<Field> getQueryWhereFields() throws Exception {
		List<Field> list = new ArrayList<Field>();
		list.add(Field.variant_sample_id);
		return list;
	}

	protected String getQueryWhereClause(boolean includeTableWithJoinON) throws Exception {		
		Map<Integer,String> resultIds = getResultIds(getAllSamples());
		if (resultIds.isEmpty()) return (Field.variant_sample_id.getQueryWhereName(Highlander.getCurrentAnalysis(), false) + " IS NULL");
		return (Field.variant_sample_id.getQueryWhereName(Highlander.getCurrentAnalysis(), false) + " IN ("+HighlanderDatabase.makeSqlList(resultIds.keySet(), Integer.class)+")");
	}

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
				rowData[col] = (field.getDefaultValue() != null && field.getDefaultValue().equals("0")) ? res.getInt(field.getName()) : res.getObject(field.getName()); 
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
