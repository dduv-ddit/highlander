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

package be.uclouvain.ngs.highlander.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Variant;

public class ProteinToGenomicConversion {

	/**
	 * args[0] = xls file path
	 * args[1] = sheet name
	 * args[2] = column of gene symbol (0-start)
	 * args[3] = column of positions (0-start)
	 * args[4] = column of reference amino acid (0-start)
	 * args[5] = column of alternative amino acid (0-start)
	 * args[6] = first free column (0-start)
	 * args[7] = reference
	 * First row = column names
	 * 
	 * Example
	 * gene_symbol 	hgvs				ref		pos			alt
	 * TEK					p.R1099X		R			1099		X
	 * 
	 * becomes with "file" "Sheet1" 0 3 2 4 5
	 * gene_symbol 	hgvs				ref		pos			alt		chr		pos					ref		alt
	 * TEK					p.R1099X		R			1099		X			9			27212707		T			A
	 * 
	 * @param args
	 */
	public static void main(String[] args) {		
		if (args.length < 7) {
			System.err.println("filename and sheet needed, then column of gene symbol, positions, reference AA, alternative AA and first free column(all 0-start)");
			return;
		}
		File xls = new File(args[0]);
		if (xls.exists()){			
			try{
				Highlander.initialize(new Parameters(false), 5);
				InputStream is = new FileInputStream(xls);
				Workbook wb = WorkbookFactory.create(is);			
				Sheet sheet = wb.getSheet(args[1]);
				final int GENE=Integer.parseInt(args[2].toString());
				final int POS=Integer.parseInt(args[3].toString());
				final int REF=Integer.parseInt(args[4].toString());
				final int ALT=Integer.parseInt(args[5].toString());
				final int FREE=Integer.parseInt(args[6].toString());
				Reference genome = new Reference(args[7]);
				int c=FREE;
				Row row = sheet.getRow(0);
				row.createCell(c++).setCellValue("chr");
				row.createCell(c++).setCellValue("pos");
				row.createCell(c++).setCellValue("ref");
				row.createCell(c++).setCellValue("alt");
				for (int r=1 ; r <= sheet.getLastRowNum() ; r++){
					if (r%50 == 0) System.out.print(".");
					if (r%1000 == 0) System.out.println(r);
					row = sheet.getRow(r);
					try{
						String gene = row.getCell(GENE).getStringCellValue();
						int protpos = (int)row.getCell(POS).getNumericCellValue();
						String reference = row.getCell(REF).getStringCellValue();
						String alternative = row.getCell(ALT).getStringCellValue();

						String chr = null;
						String pos = null;
						String ref = null;
						String alt = null;
						List<Variant> results = DBUtils.convertProteinToGenomic(genome, gene, protpos, reference, alternative);
						if (results.size() > 0){
							chr = "";
							pos = "";
							ref = "";
							alt = "";							
						}
						for (int i=0 ; i < results.size() ; i++){
							Variant v = results.get(i);
							chr += v.getChromosome();
							pos += v.getPosition()+"";
							ref += v.getReference();
							alt += v.getAlternative();
							if (i < results.size()-1) {
								chr += ";";
								pos += ";";
								ref += ";";
								alt += ";";
							}
						}
						c=FREE;
						if (chr != null) row.createCell(c++).setCellValue(chr);
						else c++;
						if (pos != null) row.createCell(c++).setCellValue(pos);
						else c++;
						if (ref != null) row.createCell(c++).setCellValue(ref);
						else c++;
						if (alt != null) row.createCell(c++).setCellValue(alt);
						else c++;
					}catch(Exception ex){
						System.out.println("Cannot import line " + r + " : " + ex.getMessage());
						ex.printStackTrace();
					}
				}
				System.out.println();
				System.out.println("Writing file");
				try (FileOutputStream fileOut = new FileOutputStream(args[0])){
					wb.write(fileOut);
				}
				System.out.println("Done !");
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}else {
			System.err.println("File "+xls+" not found");
		}
	}

}
