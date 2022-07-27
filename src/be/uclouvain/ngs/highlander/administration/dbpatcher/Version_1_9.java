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

import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class Version_1_9 extends Version {

	public Version_1_9() {
		super("1.9");
	}

	@Override
	protected void makeUpdate() throws Exception {
		for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `check_segregation` ENUM('NOT_CHECKED','SINGLE','COSEG','CARRIERS','NO_COSEG','NO_COSEG_OTHER') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated for segregation: SINGLE (No other sample in family), COSEG (Variant cosegregates), CARRIERS (Some unaffected carrier(s)), NO_COSEG (Not in other affected(s)), NO_COSEG_OTHER (Does not cosegregate in other families) or NOT_CHECKED (Not checked for segregation, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.' AFTER `check_somatic_change_username`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `check_segregation_username` VARCHAR(16) DEFAULT NULL COMMENT 'User who has set the check_segregation field.' AFTER `check_segregation`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` ADD COLUMN `check_segregation` ENUM('NOT_CHECKED','SINGLE','COSEG','CARRIERS','NO_COSEG','NO_COSEG_OTHER') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated for segregation: SINGLE (No other sample in family), COSEG (Variant cosegregates), CARRIERS (Some unaffected carrier(s)), NO_COSEG (Not in other affected(s)), NO_COSEG_OTHER (Does not cosegregate in other families) or NOT_CHECKED (Not checked for segregation, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.' AFTER `check_somatic_change_username`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` ADD COLUMN `check_segregation_username` VARCHAR(16) DEFAULT NULL COMMENT 'User who has set the check_segregation field.' AFTER `check_segregation`");
		}
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('check_segregation','This field can be used if this variant has been evaluated for segregation: SINGLE (No other sample in family), COSEG (Variant cosegregates), CARRIERS (Some unaffected carrier(s)), NO_COSEG (Not in other affected(s)), NO_COSEG_OTHER (Does not cosegregate in other families) or NOT_CHECKED (Not checked for segregation, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.','Highlander users','user annotations','large','LEFT'),('check_segregation_username','User who has set the check_segregation field.','Highlander users','user annotations','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `source`='Highlander' WHERE `field`='num_genes'");
	}

}
