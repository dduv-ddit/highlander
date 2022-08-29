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

package be.uclouvain.ngs.highlander.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import be.uclouvain.ngs.highlander.Tools;

/**
* @author Raphael Helaers
*/

public class Gnomad {
	
	public enum Target {WES, WGS} 

	public static Map<String,String> getConversionMap(Target target){
		Map<String,String> map = new LinkedHashMap<>();
		map.put("chr","CHROM");
		map.put("pos","POS");
		map.put("reference","REF");
		map.put("alternative","ALT");
		switch(target) {
		case WES:
			map.put("gnomad_wes_flag","SPECIAL(lcr,segdup,decoy)");
			map.put("gnomad_wes_ac","AC");
			map.put("gnomad_wes_an","AN");
			map.put("gnomad_wes_af","AF");
			map.put("gnomad_wes_nhomalt","nhomalt");
			map.put("gnomad_wes_afr_ac","AC_afr");
			map.put("gnomad_wes_afr_an","AN_afr");
			map.put("gnomad_wes_afr_af","AF_afr");
			map.put("gnomad_wes_afr_nhomalt","nhomalt_afr");
			map.put("gnomad_wes_amr_ac","AC_amr");
			map.put("gnomad_wes_amr_an","AN_amr");
			map.put("gnomad_wes_amr_af","AF_amr");
			map.put("gnomad_wes_amr_nhomalt","nhomalt_amr");
			map.put("gnomad_wes_asj_ac","AC_asj");
			map.put("gnomad_wes_asj_an","AN_asj");
			map.put("gnomad_wes_asj_af","AF_asj");
			map.put("gnomad_wes_asj_nhomalt","nhomalt_asj");
			map.put("gnomad_wes_eas_ac","AC_eas");
			map.put("gnomad_wes_eas_an","AN_eas");
			map.put("gnomad_wes_eas_af","AF_eas");
			map.put("gnomad_wes_eas_nhomalt","nhomalt_eas");
			map.put("gnomad_wes_fin_ac","AC_fin");
			map.put("gnomad_wes_fin_an","AN_fin");
			map.put("gnomad_wes_fin_af","AF_fin");
			map.put("gnomad_wes_fin_nhomalt","nhomalt_fin");
			map.put("gnomad_wes_nfe_ac","AC_nfe");
			map.put("gnomad_wes_nfe_an","AN_nfe");
			map.put("gnomad_wes_nfe_af","AF_nfe");
			map.put("gnomad_wes_nfe_nhomalt","nhomalt_nfe");
			map.put("gnomad_wes_sas_ac","AC_sas");
			map.put("gnomad_wes_sas_an","AN_sas");
			map.put("gnomad_wes_sas_af","AF_sas");
			map.put("gnomad_wes_sas_nhomalt","nhomalt_sas");
			map.put("gnomad_wes_popmax_ac","AC_popmax");
			map.put("gnomad_wes_popmax_an","AN_popmax");
			map.put("gnomad_wes_popmax_af","AF_popmax");
			map.put("gnomad_wes_popmax_nhomalt","nhomalt_popmax");
			map.put("gnomad_wes_controls_ac","controls_AC");
			map.put("gnomad_wes_controls_an","controls_AN");
			map.put("gnomad_wes_controls_af","controls_AF");
			map.put("gnomad_wes_controls_nhomalt","controls_nhomalt");
			map.put("gnomad_wes_controls_afr_ac","controls_AC_afr");
			map.put("gnomad_wes_controls_afr_an","controls_AN_afr");
			map.put("gnomad_wes_controls_afr_af","controls_AF_afr");
			map.put("gnomad_wes_controls_afr_nhomalt","controls_nhomalt_afr");
			map.put("gnomad_wes_controls_amr_ac","controls_AC_amr");
			map.put("gnomad_wes_controls_amr_an","controls_AN_amr");
			map.put("gnomad_wes_controls_amr_af","controls_AF_amr");
			map.put("gnomad_wes_controls_amr_nhomalt","controls_nhomalt_amr");
			map.put("gnomad_wes_controls_asj_ac","controls_AC_asj");
			map.put("gnomad_wes_controls_asj_an","controls_AN_asj");
			map.put("gnomad_wes_controls_asj_af","controls_AF_asj");
			map.put("gnomad_wes_controls_asj_nhomalt","controls_nhomalt_asj");
			map.put("gnomad_wes_controls_eas_ac","controls_AC_eas");
			map.put("gnomad_wes_controls_eas_an","controls_AN_eas");
			map.put("gnomad_wes_controls_eas_af","controls_AF_eas");
			map.put("gnomad_wes_controls_eas_nhomalt","controls_nhomalt_eas");
			map.put("gnomad_wes_controls_fin_ac","controls_AC_fin");
			map.put("gnomad_wes_controls_fin_an","controls_AN_fin");
			map.put("gnomad_wes_controls_fin_af","controls_AF_fin");
			map.put("gnomad_wes_controls_fin_nhomalt","controls_nhomalt_fin");
			map.put("gnomad_wes_controls_nfe_ac","controls_AC_nfe");
			map.put("gnomad_wes_controls_nfe_an","controls_AN_nfe");
			map.put("gnomad_wes_controls_nfe_af","controls_AF_nfe");
			map.put("gnomad_wes_controls_nfe_nhomalt","controls_nhomalt_nfe");
			map.put("gnomad_wes_controls_sas_ac","controls_AC_sas");
			map.put("gnomad_wes_controls_sas_an","controls_AN_sas");
			map.put("gnomad_wes_controls_sas_af","controls_AF_sas");
			map.put("gnomad_wes_controls_sas_nhomalt","controls_nhomalt_sas");
			map.put("gnomad_wes_controls_popmax_ac","controls_AC_popmax");
			map.put("gnomad_wes_controls_popmax_an","controls_AN_popmax");
			map.put("gnomad_wes_controls_popmax_af","controls_AF_popmax");
			map.put("gnomad_wes_controls_popmax_nhomalt","controls_nhomalt_popmax");
			break;
		case WGS:
			map.put("gnomad_wgs_flag","SPECIAL(lcr,segdup,decoy)");
			map.put("gnomad_wgs_ac","AC");
			map.put("gnomad_wgs_an","AN");
			map.put("gnomad_wgs_af","AF");
			map.put("gnomad_wgs_nhomalt","nhomalt");
			map.put("gnomad_wgs_afr_ac","AC_afr");
			map.put("gnomad_wgs_afr_an","AN_afr");
			map.put("gnomad_wgs_afr_af","AF_afr");
			map.put("gnomad_wgs_afr_nhomalt","nhomalt_afr");
			map.put("gnomad_wgs_ami_ac","AC_ami");
			map.put("gnomad_wgs_ami_an","AN_ami");
			map.put("gnomad_wgs_ami_af","AF_ami");
			map.put("gnomad_wgs_ami_nhomalt","nhomalt_ami");
			map.put("gnomad_wgs_amr_ac","AC_amr");
			map.put("gnomad_wgs_amr_an","AN_amr");
			map.put("gnomad_wgs_amr_af","AF_amr");
			map.put("gnomad_wgs_amr_nhomalt","nhomalt_amr");
			map.put("gnomad_wgs_asj_ac","AC_asj");
			map.put("gnomad_wgs_asj_an","AN_asj");
			map.put("gnomad_wgs_asj_af","AF_asj");
			map.put("gnomad_wgs_asj_nhomalt","nhomalt_asj");
			map.put("gnomad_wgs_eas_ac","AC_eas");
			map.put("gnomad_wgs_eas_an","AN_eas");
			map.put("gnomad_wgs_eas_af","AF_eas");
			map.put("gnomad_wgs_eas_nhomalt","nhomalt_eas");
			map.put("gnomad_wgs_fin_ac","AC_fin");
			map.put("gnomad_wgs_fin_an","AN_fin");
			map.put("gnomad_wgs_fin_af","AF_fin");
			map.put("gnomad_wgs_fin_nhomalt","nhomalt_fin");
			map.put("gnomad_wgs_nfe_ac","AC_nfe");
			map.put("gnomad_wgs_nfe_an","AN_nfe");
			map.put("gnomad_wgs_nfe_af","AF_nfe");
			map.put("gnomad_wgs_nfe_nhomalt","nhomalt_nfe");
			map.put("gnomad_wgs_sas_ac","AC_sas");
			map.put("gnomad_wgs_sas_an","AN_sas");
			map.put("gnomad_wgs_sas_af","AF_sas");
			map.put("gnomad_wgs_sas_nhomalt","nhomalt_sas");
			map.put("gnomad_wgs_popmax_ac","AC_popmax");
			map.put("gnomad_wgs_popmax_an","AN_popmax");
			map.put("gnomad_wgs_popmax_af","AF_popmax");
			map.put("gnomad_wgs_popmax_nhomalt","nhomalt_popmax");
			map.put("gnomad_wgs_controls_ac","AC_controls_and_biobanks");
			map.put("gnomad_wgs_controls_an","AN_controls_and_biobanks");
			map.put("gnomad_wgs_controls_af","AF_controls_and_biobanks");
			map.put("gnomad_wgs_controls_nhomalt","nhomalt_controls_and_biobanks");
			map.put("gnomad_wgs_controls_afr_ac","AC_controls_and_biobanks_afr");
			map.put("gnomad_wgs_controls_afr_an","AN_controls_and_biobanks_afr");
			map.put("gnomad_wgs_controls_afr_af","AF_controls_and_biobanks_afr");
			map.put("gnomad_wgs_controls_afr_nhomalt","nhomalt_controls_and_biobanks_afr");
			map.put("gnomad_wgs_controls_ami_ac","AC_controls_and_biobanks_ami");
			map.put("gnomad_wgs_controls_ami_an","AN_controls_and_biobanks_ami");
			map.put("gnomad_wgs_controls_ami_af","AF_controls_and_biobanks_ami");
			map.put("gnomad_wgs_controls_ami_nhomalt","nhomalt_controls_and_biobanks_ami");
			map.put("gnomad_wgs_controls_amr_ac","AC_controls_and_biobanks_amr");
			map.put("gnomad_wgs_controls_amr_an","AN_controls_and_biobanks_amr");
			map.put("gnomad_wgs_controls_amr_af","AF_controls_and_biobanks_amr");
			map.put("gnomad_wgs_controls_amr_nhomalt","nhomalt_controls_and_biobanks_amr");
			map.put("gnomad_wgs_controls_asj_ac","AC_controls_and_biobanks_asj");
			map.put("gnomad_wgs_controls_asj_an","AN_controls_and_biobanks_asj");
			map.put("gnomad_wgs_controls_asj_af","AF_controls_and_biobanks_asj");
			map.put("gnomad_wgs_controls_asj_nhomalt","nhomalt_controls_and_biobanks_asj");
			map.put("gnomad_wgs_controls_eas_ac","AC_controls_and_biobanks_eas");
			map.put("gnomad_wgs_controls_eas_an","AN_controls_and_biobanks_eas");
			map.put("gnomad_wgs_controls_eas_af","AF_controls_and_biobanks_eas");
			map.put("gnomad_wgs_controls_eas_nhomalt","nhomalt_controls_and_biobanks_eas");
			map.put("gnomad_wgs_controls_fin_ac","AC_controls_and_biobanks_fin");
			map.put("gnomad_wgs_controls_fin_an","AN_controls_and_biobanks_fin");
			map.put("gnomad_wgs_controls_fin_af","AF_controls_and_biobanks_fin");
			map.put("gnomad_wgs_controls_fin_nhomalt","nhomalt_controls_and_biobanks_fin");
			map.put("gnomad_wgs_controls_nfe_ac","AC_controls_and_biobanks_nfe");
			map.put("gnomad_wgs_controls_nfe_an","AN_controls_and_biobanks_nfe");
			map.put("gnomad_wgs_controls_nfe_af","AF_controls_and_biobanks_nfe");
			map.put("gnomad_wgs_controls_nfe_nhomalt","nhomalt_controls_and_biobanks_nfe");
			map.put("gnomad_wgs_controls_sas_ac","AC_controls_and_biobanks_sas");
			map.put("gnomad_wgs_controls_sas_an","AN_controls_and_biobanks_sas");
			map.put("gnomad_wgs_controls_sas_af","AF_controls_and_biobanks_sas");
			map.put("gnomad_wgs_controls_sas_nhomalt","nhomalt_controls_and_biobanks_sas");
			map.put("gnomad_wgs_controls_popmax_ac","AC_controls_and_biobanks_popmax");
			map.put("gnomad_wgs_controls_popmax_an","AN_controls_and_biobanks_popmax");
			map.put("gnomad_wgs_controls_popmax_af","AF_controls_and_biobanks_popmax");
			map.put("gnomad_wgs_controls_popmax_nhomalt","nhomalt_controls_and_biobanks_popmax");
			break;
		}
		return map;
	}
	
	public static void convertVCFtoTSV(Target target, File vcfFile) throws Exception {
		String filename = vcfFile.getName();
		filename = filename.substring(0, filename.indexOf(".vcf")) + ".tsv";
		File output = new File(((vcfFile.getParent() != null) ? vcfFile.getParent() + "/" : "") + filename);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		Map<String,String> conversion = getConversionMap(target);
		String flagField = "gnomad_"+target.toString().toLowerCase()+"_flag";
		int counter = 0;
		try (FileWriter fw = new FileWriter(output)){
			for (Iterator<String> it = conversion.keySet().iterator() ; it.hasNext() ; ) {
				fw.write(it.next());
				if (it.hasNext()) fw.write("\t");
			}
			fw.write("\n");
			try (FileInputStream fis = new FileInputStream(vcfFile)){
				try (InputStream in = new GZIPInputStream(fis)){
					try (InputStreamReader isr = new InputStreamReader(in)){
						try (BufferedReader br = new BufferedReader(isr)){
							String line;
							int lineCount = 0;
							String[] header = null;
							while ((line = br.readLine()) != null){
								lineCount++;
								if (line.startsWith("#") && !line.startsWith("##")){
									header = line.split("\t");
								}
								if (!line.startsWith("#")){
									try{
										if (header == null) throw new Exception("VCF header columns were not found, need a line starting with ONE # followed by all headers");
										counter++;
										if (counter % 5000 == 0) System.out.println(vcfFile.getName() + " - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants annotated ...");
										Map<String,String> values = new LinkedHashMap<String, String>(); 
										for (String key : conversion.keySet()) {
											values.put(key, "\\N");
										}
										values.put(flagField, "");
										String[] cols = line.split("\t");
										for (int col=0 ; col < header.length ; col++){
											if (header[col].equalsIgnoreCase("#CHROM") || header[col].equalsIgnoreCase("CHROM")){
												//Need to remove the eventual "chr"
												values.put("chr", cols[col].replace("chr", ""));
											}else if (header[col].equalsIgnoreCase("POS")){
												values.put("pos", cols[col]);
											}else if (header[col].equalsIgnoreCase("REF")){
												values.put("reference", cols[col]);
											}else if (header[col].equalsIgnoreCase("ALT")){
												values.put("alternative", cols[col]);
											}else if (header[col].equalsIgnoreCase("INFO")){
												String[] infos = cols[col].split(";");
												for (int i=0 ; i < infos.length ; i++){
													if (infos[i].contains("=")) {
														String vcfId = infos[i].split("=")[0];
														for (String field : conversion.keySet()) {
															if (conversion.get(field).equals(vcfId)) {
																String value = infos[i].split("=")[1];
																//if (value.equals("0.00000e+00")) value = "0";
																values.put(field, value);
																break;
															}
														}
													}else if (infos[i].equals("lcr") || infos[i].equals("segdup") || infos[i].equals("decoy")) {
														if (values.get(flagField).length() == 0) {
															values.put(flagField, infos[i]);															
														}else {
															values.put(flagField, values.get(flagField) + ";" + infos[i]);
														}
													}
												}
											}
										}
										for (Iterator<String> it = values.keySet().iterator() ; it.hasNext() ; ) {
											fw.write(values.get(it.next()));
											if (it.hasNext()) fw.write("\t");
										}
										fw.write("\n");
									}catch (Exception ex){
										System.err.println("WARNING -- Problem with line " + lineCount + " of " + vcfFile.getName());
										System.err.println(line);
										Tools.exception(ex);
									}
								}
							}
						}
					}
				}
			}
			System.out.println(vcfFile.getName() + " - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants annotated ...");
			System.out.println("DONE");
		}
	}

	public static void main(String[] args) {
		try {
			Target target = Target.valueOf(args[0].toUpperCase());
			convertVCFtoTSV(target, new File(args[1]));
		} catch (Exception ex) {
			Tools.exception(ex);
		}	
		System.exit(0);
	}

}
