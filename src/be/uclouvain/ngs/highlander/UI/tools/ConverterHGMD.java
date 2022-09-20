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

package be.uclouvain.ngs.highlander.UI.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;


/**
 * Tools that will convert the HTML result from a submission to HGMD (using a VCF exported from Highlander) to public comments in Highlander
 * Developped for Erasme Hospital
 * 
 * @author Raphael Helaers
 */

public class ConverterHGMD {

	public static int converterHGMD(File fileHTML) {
		Highlander.waitingPanel.start();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		int records = 0;
		try {
			try (FileReader fr = new FileReader(fileHTML)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					boolean query = false;
					boolean queryDone = false;
					boolean result = false;
					boolean headersDone = false;
					List<String> queries = new ArrayList<String>();
					String allResults = "";
					String version = "";
					while ((line = br.readLine()) != null) {
						if (line.contains("<input type=\"hidden\" name=\"batch")){
							query = true;
						}					
						if (query && line.endsWith("\">")) {
							query = false;
							queryDone = true;
							Highlander.waitingPanel.setProgressString("Importing "+Tools.doubleToString(queries.size(), 0, false)+" results", false);
							Highlander.waitingPanel.setProgressMaximum(queries.size());
						}
						if (queryDone && line.contains("<tr")) {
							result = true;
						}
						if (result && line.contains("</table>")) {
							result = false;
						}
						if (line.contains("Professional")) {
							int start = line.indexOf("Professional");
							int end = line.indexOf("Professional");
							for (int i=start ; i > 0 ; i--) {
								if (line.charAt(i) == '>') {
									start = i;
									break;
								}
							}
							for (int i=end ; i < line.length() ; i++) {
								if (line.charAt(i) == '<') {
									end = i;
									break;
								}
							}
							version = line.substring(start+1,end);
							version = version.replaceAll("[^A-Za-z0-9\\.\\- ]","");
						}
						if (query) {
							if (line.startsWith("<")) {
								String thisLine = line;
								thisLine = thisLine.substring(thisLine.lastIndexOf("<"));
								queries.add(thisLine.substring(thisLine.indexOf("value=\"")+"value=\"".length()));														
							}else {
								queries.add(line);							
							}
						}
						if (result) {
							if (line.contains("</td>")) {
								if (!headersDone) {
									line = line.substring(line.indexOf("<td"));
									headersDone = true;
								}
								allResults += line;
							}
						}
					}
					System.out.println(version);
					System.out.println("------");
					for (String tr : allResults.split("</tr>")) {
						if (tr.startsWith("<tr")) {
							tr = tr.substring(tr.indexOf(">")+1); 
						}
						boolean queryMatch = false;
						boolean queryFound = false;
						String chr = "";
						String pos = "";
						String ref = "";
						String alt = "";
						int length = 1;
						String gene = "";
						String geneType = "";
						String hgvs = "";
						String mutType = "";
						String hgmd = "";
						String clinvarLink = "";
						String clinvarStatus = "";
						StringBuilder sb = new StringBuilder();
						int count = 0;
						if (tr.split("</td>").length == 2) {
							String td = tr.split("</td>")[0];
							if (td.startsWith("<td")) {
								td = td.substring(td.indexOf(">")+1); 
							}
							for (String q : queries) {
								if (td.length() > 0 && q.startsWith(td)) {
									String[] cols = q.split("\t");
									chr = cols[0];
									pos = cols[1];
									ref = cols[3];
									alt = cols[4];
									length = Math.abs(ref.length() - alt.length());
									gene = cols[7].substring(cols[7].indexOf("gene_symbol=")+"gene_symbol=".length(), cols[7].indexOf(";"));
									if (length == 0) length = 1;
									queryFound = true;
									break;
								}
							}
							sb.append(version + " : NOT FOUND");
						}else {
							for (String td : tr.split("</td>")) {						
								if (td.startsWith("<td")) {
									td = td.substring(td.indexOf(">")+1); 
								}
								if (!queryMatch) {
									for (String q : queries) {
										if (td.length() > 0 && q.startsWith(td)) {
											String[] cols = q.split("\t");
											chr = cols[0];
											pos = cols[1];
											ref = cols[3];
											alt = cols[4];
											length = Math.abs(ref.length() - alt.length());
											gene = cols[7].substring(cols[7].indexOf("gene_symbol=")+"gene_symbol=".length(), cols[7].indexOf(";"));
											if (length == 0) length = 1;
											queryFound = true;
											break;
										}
									}
									queryMatch = true;
								}
								switch(count) {
								case 1:
									//gene
									if (td.contains("</span>")) {
										int start = td.indexOf("</span>");
										int end = td.indexOf("</span>");
										for (int i=start ; i > 0 ; i--) {
											if (td.charAt(i) == '>') {
												start = i;
												break;
											}
										}
										geneType = td.substring(start+1,end);
									}
									break;
								case 3:
									//HGVS
									hgvs = td.replace("&gt;", ">");
									break;
								case 5:
									//Mutation type
									if (td.contains("</span>")) {
										int start = td.indexOf("</span>");
										int end = td.indexOf("</span>");
										for (int i=start ; i > 0 ; i--) {
											if (td.charAt(i) == '>') {
												start = i;
												break;
											}
										}
										mutType = td.substring(start+1,end);
									}								
									break;
								case 8:
									//hgmd id								
									hgmd = td.substring(td.indexOf("value=\"")+"value=\"".length());
									hgmd = hgmd.substring(0, hgmd.indexOf("\""));
									hgmd = "https://my.qiagendigitalinsights.com/bbp/view/hgmd/pro/mut.php?acc=" + hgmd;
									break;
								case 9:
									//clinvar
									clinvarLink = td.substring(td.indexOf("href=\"")+"href=\"".length());
									clinvarLink = clinvarLink.substring(0, clinvarLink.indexOf("\""));
									clinvarStatus = td.substring(td.indexOf("</a> ")+"</a> ".length());
									break;
								default:
									//don't care
								}
								//System.out.println((count) + "\t" + td);
								count++;
							}						
							sb.append(version + " : " + mutType);
							sb.append("\n" + hgmd);
							if (hgvs.length() > 0) {
								sb.append("\n" + hgvs + ((geneType.length() > 0) ? " - " + geneType : ""));							
							}
							if (clinvarStatus.length() > 0) {
								sb.append("\n" + "Clinvar : " + clinvarStatus);
								sb.append("\n" + clinvarLink);							
							}
						}
						if (queryFound) {
							Highlander.waitingPanel.setProgressValue(records++);
							for (Analysis analysis : Highlander.getAvailableAnalyses()) {
								try {
									int variantAnnotationId = -1;
									String variant_comments = "";
									try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
											"SELECT id, variant_comments "
													+ "FROM " + analysis.getFromUserAnnotationsVariantsPublic() + " "
													+ "WHERE chr = '"+chr+"' AND pos = "+pos+" AND length = "+length+" AND reference = '"+ref+"' AND alternative = '"+alt+"' AND gene_symbol = '"+gene+"' AND username = 'PUBLIC'")){
										if (res.next()) {
											variantAnnotationId = res.getInt("id");
											variant_comments = res.getString("variant_comments");
										}
									}
									String txt = variant_comments;
									int i = txt.indexOf("\nLast modified by "); 
									if (i >= 0){
										String newTxt = txt.substring(0, i);
										int j = txt.indexOf(".", i);
										if (j >= 0 && j+1 < txt.length()){
											newTxt += txt.substring(j+1);
										}
										txt = newTxt;
									}
									if (!txt.contains(version)) {
										if (txt.length() > 0) txt += "\n";
										txt += sb.toString();
									}
									txt += "\nLast modified by " + Highlander.getLoggedUser().getUsername() + " on "+df.format(System.currentTimeMillis())+".";
									if (variantAnnotationId >= 0){
										Highlander.getDB().update(Schema.HIGHLANDER, 
												"UPDATE " + analysis.getFromUserAnnotationsVariantsPublic()
												+ "SET "+Field.variant_comments_public.getQueryWhereName(analysis, false)+" = '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "' "
												+ "WHERE id = " + variantAnnotationId);		
									}else{
										variantAnnotationId = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
												"INSERT INTO " + analysis.getFromUserAnnotationsVariants()
												+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_comments`) " +
												"VALUES ('"+chr+"', "+pos+", "+length+", '"+ref+"','"+alt+"', '"+gene+"', 'PUBLIC', '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "')");
									}							
								}catch(Exception ex) {
									Tools.exception(ex);
									JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot import the following HGMD result:\n" + sb.toString(), ex), "Importing HGMD",
											JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
								}
							}
						}
						if (queryFound) {
							System.out.println("["+chr+"-"+pos+"-"+ref+"-"+alt+"-"+length+"-"+gene+"]");
						}else {
							System.out.println("[QUERY NOT FOUND]");					
						}
						System.out.println(sb.toString());
						System.out.println("------");
					}
				}
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during importation", ex), "Importing HGMD",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		Highlander.waitingPanel.stop();
		return records;
	}

	public static void main(String[] args) {
		try {
			converterHGMD(new File("D:\\Dropbox\\Projets\\Highlander\\tools\\HGMD to comments\\test DDUV.html"));
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
