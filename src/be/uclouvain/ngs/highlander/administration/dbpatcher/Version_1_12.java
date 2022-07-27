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

package be.uclouvain.ngs.highlander.administration.dbpatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class Version_1_12 extends Version {

	public Version_1_12() {
		super("1.12");
	}

	@Override
	protected void makeUpdate() throws Exception {
		Map<String,Set<Integer>> outsourcing = new HashMap<String, Set<Integer>>();
		try (Results res = DB.select(Schema.HIGHLANDER,"SELECT id, outsourcing FROM projects WHERE outsourcing IS NOT NULL AND outsourcing != ''")) {
			while (res.next()){
				int project = res.getInt(1);
				String outsource = res.getString(2);
				if (!outsourcing.containsKey(outsource)){
					outsourcing.put(outsource, new HashSet<Integer>());
				}
				outsourcing.get(outsource).add(project);
			}
		}
		for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `exac_ac` INT DEFAULT NULL COMMENT 'Alternative allele counts (adjusted) in the ExAC database.' AFTER `siphy_29way_log_odds`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `exac_af` DOUBLE DEFAULT NULL COMMENT 'Alternative allele frequency (adjusted) in the ExAC database.' AFTER `exac_ac`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `consensus_MAC` INT DEFAULT NULL COMMENT 'Sum of the alternative allele counts from fields exac_ac, 1000G_AC, gonl_ac, ARIC5606_AA_AC and ARIC5606_EA_AC.' AFTER `ARIC5606_EA_AF`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `consensus_MAF` DOUBLE DEFAULT NULL COMMENT 'Maximum alternative allele frequency found in fields exac_af, 1000G_AF, ESP6500_AA_AF, ESP6500_EA_AF, gonl_af, ARIC5606_AA_AF and ARIC5606_EA_AF.' AFTER `consensus_MAC`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `outsourcing` `outsourcing` VARCHAR(50) DEFAULT NULL COMMENT 'Name of the external company where the sample has been outsourced if relevant.' AFTER `platform`");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `outsourcing` = NULL");
			for (String outsource : outsourcing.keySet()){
				updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `outsourcing` = '"+outsource+"' WHERE `project_id` IN ("+HighlanderDatabase.makeSqlList(outsourcing.get(outsource), Integer.class)+")");
			}
		}
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('exac_ac','Alternative allele counts (adjusted) in the ExAC database.','ExAC 0.3','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('exac_af','Alternative allele frequency (adjusted) in the ExAC database.','ExAC 0.3','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('consensus_MAC','Sum of the alternative allele counts from fields exac_ac, 1000G_AC, gonl_ac, ARIC5606_AA_AC and ARIC5606_EA_AC.','Highlander','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('consensus_MAF','Maximum alternative allele frequency found in fields exac_af, 1000G_AF, ESP6500_AA_AF, ESP6500_EA_AF, gonl_af, ARIC5606_AA_AF and ARIC5606_EA_AF.','Highlander','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `alignment`='LEFT', `size`='medium', `description`='Name of the exernal company where the sample has been outsourced.' WHERE `field`='outsourcing'");
	}

}
