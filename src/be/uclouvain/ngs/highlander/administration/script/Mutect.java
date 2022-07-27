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

package be.uclouvain.ngs.highlander.administration.script;

import javax.swing.JTextField;

import be.uclouvain.ngs.highlander.administration.UI.Project;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;

public class Mutect extends Script {

	/*
	 * Doesn't work, because each script normally launch analysis on a RUN, and Mutect script launch analysis on a pair of samples.
	 * Solution 1:
	 * - change getSpecificArguments to return an array, and change accordingly Script.java
	 * - change Script.java to not add the project path to the command line automatically (maybe add it with getSpecificArgument)
	 * Solution 2:
	 * - change the bash script to accept a run path; and let it retreive all the N/T pairs by itself
	 * - more complicated (need access the DB, doesn't work the first time because N/T is not present in the DB)
	 * Solution 3:
	 * - add a N/T pairing in the database (not in comment) and the need to set it when encoding samples
	 * - change the script like solution 2 
	 * - lot of changes but more robust
	 */
	public Mutect(ProjectManager pm) {
		super(pm);
		scriptFile = "submit_mutect2.sh";
		arguments.put("Skip variant calling stage", " -t");
		arguments.put("Skip VCF merging stage", " -u");
		arguments.put("Skip VCF annotations stage", " -v");
		arguments.put("Skip sample statistics stage", " -w");
		arguments.put("Skip importation stage", " -x");
		arguments.put("Skip Facets stage", " -y");
		arguments.put("Skip MSIsensor stage", " -z");
		arguments.put("BAM files extension", " -e");
		argumentsValues.put("BAM files extension", new JTextField("checked.wodup.realign.fixed.recal.bam"));
		arguments.put("Slurm partition", " -Q");
		argumentsValues.put("Slurm partition", new JTextField("computeGPU"));
		compatibleAnalysis.add("exomes_mutect");
		compatibleAnalysis.add("genomes_mutect");
		compatibleAnalysis.add("panels_mutect");
	}

	@Override
	protected boolean canGetSpecificArguments(Project project, String subProject){
		if (project.comments.indexOf("normal_sample[") < 0) {
			ProjectManager.toConsole("WARNING for "+project.run_path_full+": Normal sample not found in 'comments' from projetcs table. Please use the 'setnormal' tool of dbBuilder when importing MuTect data.");
			return false;
		}
		return true;
	}
	
	@Override
	protected String getSpecificArguments(Project project, String subProject){
		String commandLine = "";
		commandLine += " -T " + project.run_path_full;
		String normalPath = project.comments.substring(project.comments.indexOf("normal_sample[")+"normal_sample[".length(),project.comments.lastIndexOf("]"));
		commandLine += " -N " + normalPath;
		String anals = project.getAllAnalysesCompatibleWith(compatibleAnalysis);
		if (anals.length() > 0){
			commandLine += " -A " + anals;
		}
		commandLine += " -R " + project.references.get(subProject);
		commandLine += " -B " + project.kits.get(subProject).toLowerCase();
		return commandLine;
	}

}
