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

package be.uclouvain.ngs.highlander.administration;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

public class Lifebamcopy {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] toProcess = new String[]{
				"11_2012_10_16_LM_LE",  //OK
				"1_2012_09_18_TER", //OK
				"13_2012_11_28_EXT_BJORN", //OK
				"16_2013_01_25_MAF_LM", //OK
				"17_2013_01_23_VM", //OK
				//"17_2013_02_21_BIS", //Inexistant dans LifeScope backup
				"18_2013_02_08_CLP_HHT_AVM", //OK
				"19_2013_02_11_VM_HHT_AVM", //OK
				"21_2013_06_03_CVM", //OK
				"21_2013_07_09_A_BIS", //OK
				"21_2013_07_09_B_BIS", //OK
				//"2_2013_01_11_QUATER", --> combinaison des bam sans duplicat de GATK
				"22_2013_05_03_CM", //OK
				"22_2013_05_07_POOL", //OK
				"24_2013_06_20_A_LE_OCD", //OK
				"24_2013_06_20_B_LE_OCD", //OK
				//"3_2012_12_14_BIS", --> combinaison des bam sans duplicat de GATK
				"4_2012_08_17_TER", //OK
				//"5_2012_10_30_BIS", --> combinaison des 2 runs 5
				//"6_2012_09_18_BIS", --> combinaison des 2 runs 6
				"8_2012_08_16_LE", //OK
				//"9_2012_11_23_BIS", --> combinaison des 2 runs 9
		};

		//Pour les merge ou inexistants, utiliser directement SAMPLE.checked.wodup.realign.bam et skipper les phases check / remove dup / realignment

		File backup = new File("/backup/lifescope/projects");
		File results = new File ("/data/highlander/results");
		for (File projFile : backup.listFiles()){
			String project = projFile.getName();
			File resultsTarget = null;
			for (File resFile : results.listFiles()){
				if (resFile.getName().equals(project)){
					resultsTarget = resFile;
					break;
				}
			}
			if (Arrays.binarySearch(toProcess, 0, toProcess.length, project) > -1){
				if (resultsTarget != null){
					System.out.println("Processing project " + project);
					for (File analysisFile : projFile.listFiles()){
						String analysis = analysisFile.getName();
						boolean hasTempDir = false;
						for (File lvl1 : analysisFile.listFiles()){
							if (lvl1.getName().equals("temp")){
								hasTempDir = true;
								break;
							}
						}
						for (File lvl1 : analysisFile.listFiles()){
							if (lvl1.getName().equals(hasTempDir?"temp":"outputs")){
								for (File lvl2 : lvl1.listFiles()){
									if (lvl2.getName().equals("enrichment")){
										for (File sampleFile : lvl2.listFiles()){
											String sample = sampleFile.getName();
											File target = new File(resultsTarget + "/" + analysis + "/" + sample);
											if (target.exists()){
												for (File source : sampleFile.listFiles()){
													try{
														if (source.toString().endsWith("bam")){
															System.out.println("Source : " + source);
															System.out.println("Target : " +  new File(target + "/" + sample + ".bam"));
															FileUtils.copyFile(source, new File(target + "/" + sample + ".bam"));														
														}else if (source.toString().endsWith("bai")){
															System.out.println("Source : " + source);
															System.out.println("Target : " +  new File(target + "/" + sample + ".bam.bai"));
															FileUtils.copyFile(source, new File(target + "/" + sample + ".bam.bai"));
														}
													}catch(Exception ex){
														ex.printStackTrace();
													}
												}
											}else{
												System.out.println("UNKNOWN TARGET : " + target);
											}
										}
									}
								}
							}
						}
					}
					System.out.println("\t-+-+-\t");
				}else{
					System.out.println("Unknown project " + project + " !");
				}
			}else{
				//System.out.println("Skipping " + project);
			}
		}
	}
}
