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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class LogParser {

	public enum Level {STAGES, STEPS}
	public enum Sort {None, StoL, LtoS}
	
	/**
	 * Display information from Highlander pipeline logs
	 * -i		display Start/End lines for steps
	 * -I		display Start/End lines for stages
	 * -e		display Elapsed lines for steps
	 * -E		display Elpased lines for stages
	 * -s		sort by Elapsed lines by time (shortest step/stage to longest)
	 * -S		sort by Elapsed lines by time (longest step/stage to shortest)
	 * 
	 * Other arguments are files to parse (results are concatenated)
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		boolean iSteps = false;
		boolean iStages = false;
		boolean eSteps = false;
		boolean eStages = false;
		boolean sortsl = false;
		boolean sortls = false;
		List<File> files = new ArrayList<>();
		Set<Integer> jobIds = new TreeSet<>();
		Set<String> runs = new TreeSet<>();
		Set<String> samples = new TreeSet<>();
		for (int i=0 ; i < args.length ; i++){
			if (args[i].equals("-i")){
				iSteps = true;
			}else if (args[i].equals("-I")){
				iStages = true;
			}else if (args[i].equals("-e")){
				eSteps = true;
			}else if (args[i].equals("-E")){
				eStages = true;
			}else if (args[i].equals("-s")){
				sortsl = true;
			}else if (args[i].equals("-S")){
				sortls = true;
			}else {
				files.add(new File(args[i]));
				try {
					String[] path = args[i].split("/");
					String[] name = path[path.length-1].split("\\.");
					jobIds.add(Integer.parseInt(name[0]));
					runs.add(name[1]);
					samples.add(name[2]);
				}catch(Exception ex) {
					System.err.println("Cannot parse job name : '"+args[i]+"'");
				}
			}
		}

		System.out.print("Run\t: ");
		for (String run : runs) System.out.print(run + "\t");
		System.out.println();
		System.out.print("Sample\t: ");
		for (String sample : samples) System.out.print(sample + "\t");
		System.out.println();
		System.out.print("JobId\t: ");
		for (int id : jobIds) System.out.print(id + "\t");
		System.out.println();
		System.out.println();
		
		if (iSteps) {
			for (File file : files) {
				try (BufferedReader br = new BufferedReader(new FileReader(file))){
					String line;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("## Started") || line.startsWith("## Ended")) {
							System.out.println(line);
						}
					}
				}catch(IOException ex) {
					ex.printStackTrace();
				}
			}
			System.out.println();
		}

		if (iStages) {
			for (File file : files) {
				try (BufferedReader br = new BufferedReader(new FileReader(file))){
					String line;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("### Started") || line.startsWith("### Ended")) {
							System.out.println(line);
						}
					}
				}catch(IOException ex) {
					ex.printStackTrace();
				}
			}
			System.out.println();
		}

		Sort sort = Sort.None;
		if (sortls) sort = Sort.LtoS;
		else if (sortsl) sort = Sort.StoL;

		if (eSteps) {
			printElapsed(Level.STEPS, sort, files);
		}
		
		if (eStages) {
			printElapsed(Level.STAGES, sort, files);
		}
		
	}
	
	public static String formatTime(Integer time, String line) {
		if (time > 60*24) line=line.replace(time + " minutes", time + " minutes ("+(time/60/24)+" days "+((time/60)%24)+" hours "+(time%60)+" minutes)");
		else if (time > 60) line=line.replace(time + " minutes", time + " minutes ("+(time/60)+" hours "+(time%60)+" minutes)");
		return line;
	}
	
	public static void printElapsed(Level level, Sort sort, List<File> files) {
		String start = (level == Level.STAGES) ? "### Elapsed" : "## Elapsed";
		Map<Integer, List<String>> lines = (sort == Sort.LtoS) ? new TreeMap<Integer, List<String>>(Collections.reverseOrder()) : new TreeMap<Integer, List<String>>();
		Map<String, Map<String,Integer>> chrTimes = new HashMap<>();
		Map<String, Set<String>> chrNodes = new HashMap<>();
		int order = 0;
		int minutes = 0;
		for (File file : files) {
			try (BufferedReader br = new BufferedReader(new FileReader(file))){
				String line;
				while ((line = br.readLine()) != null) {
					if (line.startsWith(start)) {
						int time = Integer.parseInt(line.substring(line.indexOf(" : ")+3, line.indexOf(" minutes")));
						if (line.contains(" for chromosome ") || line.contains(" on chr ")) {
							String chrString = (line.contains(" for chromosome ")) ? " for chromosome " : " on chr ";
							//step has been launched in parallel for each chromosome, merge them in one line a keep the longest time only
							String action = line.substring(line.indexOf(" - ")+3, line.indexOf(chrString));
							String node = line.substring((start + " on ").length(),line.indexOf(" : "));
							String chr = line.substring(line.indexOf(chrString)+chrString.length());
							if (!chrTimes.containsKey(action)) {
								chrTimes.put(action, new TreeMap<>());
								//First time we see this action, should add it to the non sorted map to keep track of it's order
								if (sort == Sort.None) {
									lines.put(order, new ArrayList<String>());
									lines.get(order).add(action);
									order++;								
								}
							}
							if (!chrTimes.get(action).containsKey(chr)) {
								chrTimes.get(action).put(chr, time);
							}else {
								//Can happen when a sample has been relaunched or multiple samples are combined
								chrTimes.get(action).put(chr, chrTimes.get(action).get(chr)+time);
							}
							if (!chrNodes.containsKey(action)) chrNodes.put(action, new TreeSet<>());
							chrNodes.get(action).add(node);
						}else {
							line = formatTime(time, line);
							if (sort != Sort.None) {
								if (!lines.containsKey(time)) lines.put(time, new ArrayList<String>());
								lines.get(time).add(line);
							}else {
								lines.put(order, new ArrayList<String>());
								lines.get(order).add(line);
								order++;
							}
							minutes += time;							
						}
					}
				}
			}catch(IOException ex) {
				ex.printStackTrace();
			}
		}
		if (!chrTimes.isEmpty()) {
			for (String action : chrTimes.keySet()) {
				String chr = "?";
				int time = 0;
				for (String c : chrTimes.get(action).keySet()) {
					if (chrTimes.get(action).get(c) > time) {
						time = chrTimes.get(action).get(c);
						chr = c;
					}
				}
				String line = start + " on " + chrNodes.get(action) + " : " + time + " minutes - " + action + " (longest time, chromosome " + chr + ")";
				line = formatTime(time, line);
				if (sort != Sort.None) {
					if (!lines.containsKey(time)) lines.put(time, new ArrayList<String>());
					lines.get(time).add(line);
				}else {
					for (int o : lines.keySet()) {
						if (lines.get(o).contains(action)) {
							lines.get(o).remove(action);
							lines.get(o).add(line);
						}
					}
				}
				minutes += time;
			}
		}
		for (List<String> linearray : lines.values()) {
			for (String line : linearray) {
				System.out.println(line);
			}
		}
		System.out.println();
		if (minutes > 60*24) System.out.println("Total : " + minutes + " minutes ("+(minutes/60/24)+" days "+((minutes/60)%24)+" hours "+(minutes%60)+" minutes)");
		else if (minutes > 60) System.out.println("Total : " + minutes + " minutes ("+(minutes/60)+" hours "+(minutes%60)+" minutes)");
		else System.out.println("Total : " + minutes + " minutes");
		System.out.println();			
	}

}
