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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;


import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Reference;

public class IntervalGonl {

	/**
	 * args[0] = input bed file path
	 * args[1] = output xls file path
	 * args[2] = reference genome
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			Highlander.initialize(new Parameters(false), 5);
			Reference referenceGenome = new Reference(args[2]);
			String filename = args[1];
			if (!filename.endsWith(".xlsx")) filename += ".xlsx";
			File xls = new File(filename);
			Workbook wb = new SXSSFWorkbook(100); 
			File bed = new File(args[0]);
			try (FileReader fr = new FileReader(bed)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					Sheet sheet = wb.createSheet(bed.getName());
					sheet.createFreezePane(0, 1);		
					int r = 0;
					Row row = sheet.createRow(r++);
					row.setHeightInPoints(50);
					int c = 0;
					row.createCell(c++).setCellValue("chr");
					row.createCell(c++).setCellValue("pos");
					row.createCell(c++).setCellValue("reference");
					row.createCell(c++).setCellValue("alternative");
					row.createCell(c++).setCellValue("ac");
					row.createCell(c++).setCellValue("an");
					row.createCell(c++).setCellValue("af");
					row.createCell(c++).setCellValue("set");
					row.createCell(c++).setCellValue("inaccessible");
					row.createCell(c++).setCellValue("gene");
					int colCount = c;
					sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, colCount-1));
					XSSFCellStyle cs = (XSSFCellStyle)sheet.getWorkbook().createCellStyle();
					cs.setDataFormat(HSSFDataFormat.getBuiltinFormat("0.00%"));

					while ((line = br.readLine()) != null){
						if (!line.startsWith("track")){
							String[] bedsplit = line.split("\t");
							String chr = bedsplit[0];
							if (chr.startsWith("chr")) chr = chr.substring(3);
							String start = bedsplit[1];
							String stop = bedsplit[2];
							String gene = bedsplit[5];
							try (Results res = Highlander.getDB().select(referenceGenome, Schema.GONL, "SELECT chr, pos, reference, alternative, ac, an, af, set, inaccessible FROM chromosome_" + chr + " WHERE pos >= " + start + " AND pos <= " + stop)) {
								while(res.next()){
									row = sheet.createRow(r++);
									c=0;
									row.createCell(c++).setCellValue(res.getString("chr"));
									row.createCell(c++).setCellValue(res.getInt("pos"));
									row.createCell(c++).setCellValue(res.getString("reference"));
									row.createCell(c++).setCellValue(res.getString("alternative"));
									row.createCell(c++).setCellValue(res.getInt("ac"));
									row.createCell(c++).setCellValue(res.getInt("an"));
									Cell cell = row.createCell(c++);
									cell.setCellStyle(cs);
									cell.setCellValue(res.getDouble("af"));
									row.createCell(c++).setCellValue(res.getString("set"));
									row.createCell(c++).setCellValue(res.getBoolean("inaccessible"));
									row.createCell(c++).setCellValue(gene);
								}
							}
						}
					}
				}
			}
			try (FileOutputStream fileOut = new FileOutputStream(xls)){
				wb.write(fileOut);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		System.out.println("Done !");
	}

}
