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

package be.uclouvain.ngs.highlander.administration.iontorrent;

import java.io.File;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

public class IonUpdater {

	/**
	 * Replace the json parameters file in all Ion/Proton samples with the one in reference
	 * @param args
	 */
	public static void main(String[] args) {
		String projectLabel = null;
		String sampleLabel = null;
		try{
			Highlander.initialize(new Parameters(false), 5);

			final Map<String, File> callers = new HashMap<String, File>();
			final File reference = new File("/data/highlander/reference/iontorrent");
			final File results = new File("/data/highlander/results");

			callers.put("PGM_GERMLINE_LOW_STRINGENCY", new File(reference + "/pgm_germline_low_stringency.json"));
			callers.put("PGM_SOMATIC_LOW_STRINGENCY", new File(reference + "/pgm_somatic_low_stringency.json"));
			callers.put("PROTON_GERMLINE_LOW_STRINGENCY", new File(reference + "/proton_germline_low_stringency.json"));
			callers.put("PROTON_SOMATIC_LOW_STRINGENCY", new File(reference + "/proton_somatic_low_stringency.json"));

			for (File projFile : results.listFiles()){
				if (projFile.isDirectory()){
					sampleLabel = null;
					projectLabel = projFile.getName();
					String[] project = projectLabel.split("_");
					int pid = Integer.parseInt(project[0]);
					String pdate = project[1]+"-"+project[2]+"-"+project[3];
					String pname = "";
					for (int i=4 ; i < project.length ; i++){
						pname += project[i];
						if (i+1 < project.length) pname += "_";
					}		
					String platform = null;
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT platform FROM `projects` WHERE `run_id` = '"+pid+"' AND `run_date` = '"+pdate+"' AND `run_name` = '"+pname+"'")) {
						if (res.next()){
							platform = res.getString(1).toUpperCase();
						}else{
							System.out.println("Run " + projectLabel + " not found in projects");
						}
					}
					if (platform.contains("ION_TORRENT") || platform.contains("PROTON")){
						System.out.println("Processing run " + projectLabel);
						for (File analFile : projFile.listFiles()){
							sampleLabel = null;
							if (analFile.isDirectory()){
								for (File sampleFile : analFile.listFiles()){
									if (sampleFile.isDirectory()){
										sampleLabel = sampleFile.getName();
										String[] split = sampleLabel.split("\\."); 
										String sample = split[0];
										String panel = (split.length > 1) ? split[1] : "";
										String caller = null;
										try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT CALLER FROM `ion_importer` WHERE `RUN_ID` = '"+pid+"' AND `RUN_DATE` = '"+pdate.replace('-', '_')+"' AND `PANEL_NAME` = '"+panel+"' AND `SAMPLE` = '"+sample+"'")) {
											if (res.next()){
												caller = res.getString(1);
											}else{
												if (platform.contains("ION_TORRENT")){
													System.out.println("	Sample " + sampleLabel + " not found in ion_importer, use PGM_GERMLINE_LOW_STRINGENCY by default");
													caller = "PGM_GERMLINE_LOW_STRINGENCY";
												}else{
													System.out.println("	Sample " + sampleLabel + " not found in ion_importer, use PROTON_GERMLINE_LOW_STRINGENCY by default");
													caller = "PROTON_GERMLINE_LOW_STRINGENCY";												
												}
											}
										}
										if (!callers.containsKey(caller)){
											if (platform.contains("ION_TORRENT")){
												System.out.println("	Caller "+caller+" not recognized for sample " + sampleLabel + ", use PGM_GERMLINE_LOW_STRINGENCY by default)");
												caller = "PGM_GERMLINE_LOW_STRINGENCY";
											}else{
												System.out.println("	Caller "+caller+" not recognized for sample " + sampleLabel + ", use PROTON_GERMLINE_LOW_STRINGENCY by default)");
												caller = "PROTON_GERMLINE_LOW_STRINGENCY";
											}
										}
										File source = callers.get(caller);
										File target = new File(sampleFile.getPath() + "/" + sampleLabel + ".json");
										System.out.println("	Sample " + sampleLabel + " : copying " + source + " on " + target);
										FileUtils.copyFile(source, target);
									}
								}
							}
						}
					}else{
						System.out.println("Skipping run " + projectLabel + " (not ION/PROTON)");
					}
				}
			}
		}catch(Exception ex){
			System.err.println("Exception when processing " + projectLabel + " / " + sampleLabel);
			ex.printStackTrace();
		}
	}
}
