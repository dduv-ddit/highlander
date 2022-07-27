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

public class Solid extends Script {

	public Solid(ProjectManager pm) {
		super(pm);
		scriptFile = "submit_solid.sh";
		arguments.put("Skip duplicates removal stage", " -t");
		arguments.put("Skip realigner stage", " -u");
		arguments.put("Skip variant calling stage", " -v");
		arguments.put("Skip impact prediction on GATK variants", " -w");
		arguments.put("Skip impact prediction on LifeScope variants", " -x");
		arguments.put("Skip statistics computation", " -y");
		arguments.put("Skip importation in the Highlander database", " -z");
		arguments.put("Skip update of Highlander database statistics and user warning", " -Z");
		arguments.put("Slurm partition", " -Q");
		argumentsValues.put("Slurm partition", new JTextField("computeCPU"));
		compatibleAnalysis.add("exomes_solid");
	}

	@Override
	protected String getSpecificArguments(Project project, String subProject) {
		String commandLine = "";
		commandLine += " -U";		
		String anals = project.getAllAnalysesCompatibleWith(compatibleAnalysis);
		if (anals.length() > 0){
			commandLine += " -A " + anals;
		}
		String lsanal = subProject;
		commandLine += " -a " + lsanal;
		commandLine += " -R " + project.references.get(subProject);
		commandLine += " -B " + project.kits.get(subProject).toLowerCase();
		if (project.pair_end) commandLine += " -p";		
		return commandLine;
	}

}
