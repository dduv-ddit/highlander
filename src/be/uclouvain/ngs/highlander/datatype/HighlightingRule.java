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

import javax.swing.JLabel;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.toolbar.HighlightingPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public abstract class HighlightingRule extends JPanel {

	public enum RuleType {HIGHLIGHTING, HEATMAP}

	protected Field field = null;

	public abstract RuleType getRuleType();

	public Field getField(){
		return field;
	}

	public String getFieldName(){
		return field.getName();
	}

	public abstract String getSaveString();

	public abstract JLabel parseSaveString(String saveString);

	public abstract HighlightingRule loadCriterion(HighlightingPanel highlightingPanel, String saveString) throws Exception;

	public boolean checkFieldCompatibility(Analysis analysis){
		return field.hasAnalysis(analysis);
	}

	public boolean changeAnalysis(Analysis analysis){
		try{
			if (field.hasAnalysis(analysis)){
				return true;
			}else{
				return false;
			}
		}catch (Exception ex){
			Tools.exception(ex);
			return false;
		}
	}

	public abstract String toHtmlString();

	public abstract void editCriterion();

	public abstract void delete();

}
