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

import java.util.Set;

import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

public class GonlVariant extends AnnotatedVariant {

	//Changes details
	private int ac = -1;
	private int an = -1;
	private double af = -1.0;
	private String set;
	private boolean inaccessible;	

	public GonlVariant(AnalysisFull analysis) {
		super(analysis);
	}

	/**
	 * 
	 * @param analysis
	 * @param line
	 * @param altIdx index of the allele. First alternative allele is 0, second alternative allele is 1, etc. Do not take into account reference allele (when ref is also in a list, we use altidx+1).
	 */
	@Override
	public void setVCFLine(String[] header, String[] line, int altIdx, String sample){
		for (int col=0 ; col < header.length ; col++){
			if (header[col].equalsIgnoreCase("#CHROM") || header[col].equalsIgnoreCase("CHROM")){
				entries.put(Field.chr, line[col].replace("chr", ""));
			}else if (header[col].equalsIgnoreCase("POS")){
				entries.put(Field.pos, Integer.parseInt(line[col]));
				setRefAlt();
			}else if (header[col].equalsIgnoreCase("ID")){
				String dbsnp_id = line[col];
				entries.put(Field.dbsnp_id, dbsnp_id); 
				if (dbsnp_id.length() == 0) entries.put(Field.dbsnp_id, null); 
				else if (dbsnp_id.equals(".")) entries.put(Field.dbsnp_id, null); 
				//else dbsnp_flagged = true;
			}else if (header[col].equalsIgnoreCase("REF")){
				entries.put(Field.reference, line[col]); 
				setRefAlt();
			}else if (header[col].equalsIgnoreCase("ALT")){
				entries.put(Field.alternative, line[col].split(",")[altIdx]); 
				int nAlt = line[col].split(",").length;
				entries.put(Field.allele_num, nAlt+1); 
				setRefAlt();			
			}else if (header[col].equalsIgnoreCase("QUAL")){
				// not used
			}else if (header[col].equalsIgnoreCase("FILTER")){
				inaccessible = !line[col].equals("PASS");
			}else if (header[col].equalsIgnoreCase("INFO")){
				String[] infos = line[col].split(";");
				for (int i=0 ; i < infos.length ; i++){
					if (infos[i].startsWith("AC=")){
						ac = Integer.parseInt(infos[i].substring(3).split(",")[altIdx]);		
						entries.put(Field.gonl_ac, ac);
					}else if (infos[i].startsWith("AF=")){
						af = Double.parseDouble(infos[i].substring(3).split(",")[altIdx]);
						entries.put(Field.gonl_af, af);
					}else if (infos[i].startsWith("AN=")){
						an = Integer.parseInt(infos[i].substring(3).split(",")[altIdx]);
					}else if (infos[i].startsWith("set=")){
						set = infos[i].substring("set=".length());					
					}else if (infos[i].startsWith("ANN=")){
						parseSnpEffANN(infos[i]);
					}
				}
			}
		}
	}

	public int getAc() {
		return ac;
	}

	public int getAn() {
		return an;
	}

	public double getAf() {
		return af;
	}

	public String getSet() {
		return set;
	}

	public boolean isInaccessible() {
		return inaccessible;
	}

	//TODO BURDEN - on peut probablement s'en sortir avec des champs dans la table custom
	@Override
	public String getInsertionString(DBMS dbms, String table){
		String nullStr = HighlanderDatabase.getNullString(dbms);
		StringBuilder sb = new StringBuilder();
		for (Field f : Field.getAvailableFields(analysis, false)){
			//if (f.isAvailableInGonl()){
			if ((f.getTable(analysis).equalsIgnoreCase(table) || f.isForeignKey(table))
					&& !(f.equals(Field.variant_sample_id) || f.equals(Field.variant_static_id) || f.equals(Field.gene_id) || f.equals(Field.variant_custom_id))
					){				
				if (f.getFieldClass() == String.class){
					if (entries.get(f) != null && entries.get(f).toString().length() > 0)	sb.append(HighlanderDatabase.format(dbms, Schema.HIGHLANDER, entries.get(f).toString())+"\t");
					else sb.append(nullStr+"\t");
				}else if (f.getFieldClass() == Boolean.class){
					if (dbms == DBMS.hsqldb) sb.append(((boolean)entries.get(f)?"true":"false")+"\t");
					else sb.append(((boolean)entries.get(f)?"1":"0")+"\t");
				}else{
					if (entries.get(f) != null)	sb.append(entries.get(f)+"\t");
					else sb.append(nullStr+"\t");
				}
			}
		}
		if (ac != -1)	sb.append(ac+"\t");
		else sb.append(nullStr+"\t");
		if (an != -1)	sb.append(an+"\t");
		else sb.append(nullStr+"\t");
		if (af != -1)	sb.append(af+"\t");
		else sb.append(nullStr+"\t");
		if (set != null)	sb.append(DB.format(Schema.HIGHLANDER, set)+"\t");
		else sb.append(nullStr+"\t");
		if (dbms == DBMS.hsqldb) sb.append((inaccessible?"true":"false")+"\t");
		else sb.append((inaccessible?"1":"0")+"\t");
		sb.append("\n");
		return sb.toString();
	}

	public static String getInsertionColumnsString(Analysis analysis, String table){
		StringBuilder out  = new StringBuilder();
		for (Field f : Field.getAvailableFields(analysis, false)){
			//if (f.isAvailableInGonl()){
			if ((f.getTable(analysis).equalsIgnoreCase(table) || f.isForeignKey(table))
					&& !(f.equals(Field.variant_sample_id) || f.equals(Field.variant_static_id) || f.equals(Field.gene_id) || f.equals(Field.variant_custom_id))
					){
				out.append(f.getName() + ", ");
			}
		}
		out.append("`ac`, `an`, `af`, `set`, `inaccessible`");
		return out.toString();		
	}
	
	//TODO BURDEN - fonctionnerait si ca devient une analyse comme les autres ?
	@Override
	public void updateAnnotations(Set<Annotation> annotations) throws Exception {	}

	@Override
	public void removeFromDatabase() throws Exception { }

}
