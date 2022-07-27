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

package be.uclouvain.ngs.highlander.database;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.uclouvain.ngs.highlander.Tools;

import java.util.Set;

public class VariantResults {

	public final Field[] headers;
	public final Object[][] data;
	public final int[] id;
	public final String[] variant;

	public VariantResults(List<Field> headers, Map<Integer, Object[]> results, Map<Integer, String> variants) throws Exception {
		this.headers = headers.toArray(new Field[0]);
		int nrow = results.size();
		data = new Object[nrow][headers.size()];
		id = new int[nrow];
		variant = new String[nrow];
		int i=0;
		for (Entry<Integer, Object[]> e : results.entrySet()){
			data[i] = e.getValue();
			id[i] = e.getKey();
			variant[i] = variants.get(e.getKey());
			i++;
		}	
	}

	public String getNumberUniqueVariants(){
		Set<String> set = new HashSet<String>(Arrays.asList(variant));
		return Tools.doubleToString(set.size(), 0, false);
	}

	public VariantResults(List<VariantResults> variantResults){
		headers = variantResults.get(0).headers;
		int nrow = 0;
		for (VariantResults r : variantResults){
			nrow += r.id.length;
		}
		data = new Object[nrow][variantResults.get(0).data[0].length];
		id = new int[nrow];
		variant = new String[nrow];

		int offset = 0;
		for (VariantResults r : variantResults){
			System.arraycopy(r.id, 0, id, offset, r.id.length);
			System.arraycopy(r.variant, 0, variant, offset, r.variant.length);
			for (int i=0 ; i < r.data.length ; i++){
				System.arraycopy(r.data[i], 0, data[i+offset], 0, r.data[i].length);
			}
			offset += r.id.length;
		}
	}

	public static VariantResults concatenate(List<VariantResults> variantResults){
		if (variantResults.size() == 1) return variantResults.get(0);
		return new VariantResults(variantResults);
	}
}
