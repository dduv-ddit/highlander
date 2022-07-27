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

public class Version_14_8 extends Version {

	public Version_14_8() {
		super("14.8");
	}

	@Override
	protected void makeUpdate() throws Exception {
		for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
			//if (DB.getDataSource(Schema.HIGHLANDER).getDBMS() != DBMS.impala){
				try{
					updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` REMOVE PARTITIONING;");
					updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `partition`");
					updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD INDEX `pathology` (`pathology`(5))");
				}catch(Exception ex){
					ex.printStackTrace();
				}
				updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `dann_score` DOUBLE DEFAULT NULL COMMENT 'DANN score for functional prediction of a variant. Please refer to Quang D et al.(2015) Bioinformatics. 2015;31(5):761-763. doi:10.1093/bioinformatics/btu703 for details. The higher the score (ranging from 0 to 1) the more likely the variant is pathogenic. Please note the following licensing statement: DANN scores and the DANN algorithm are freely available for all non-commercial applications. If you are planning on using them in a commercial application, please contact Daniel Quang (dxquang@uci.edu).' AFTER `cadd_phred`");
				updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `eigen_score` DOUBLE DEFAULT NULL COMMENT 'Eigen is an aggregated meta-score for the functional annotation of genetic variants in coding and noncoding regions. Please refer to Nature Genetics 48, 214–220 (2016) doi:10.1038/ng.3477 for details. The higher the score the more likely the variant has damaging effects.' AFTER `dann_score`");
				updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `eigen_pc_score` DOUBLE DEFAULT NULL COMMENT 'Related to Eigen, conceptually simpler meta-score. Please refer to Quang D et al.(2015) Bioinformatics. 2015;31(5):761-763. doi:10.1093/bioinformatics/btu703 for details.' AFTER `eigen_score`");
			//}
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_annotations` MODIFY `comments_gene` LONGTEXT NOT NULL COMMENT 'Comments about the gene, if any.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_private_annotations` MODIFY `private_comments_variant` LONGTEXT NOT NULL COMMENT 'Private comments about the variant, visible only by you.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_private_annotations` MODIFY `private_comments_variant` LONGTEXT NOT NULL COMMENT 'Private comments about the variant, visible only by you.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_public_annotations` MODIFY `evaluation_comments` LONGTEXT NOT NULL COMMENT 'Comments visible to all users but modifiable only by users associated with the sample.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_public_annotations` MODIFY `history` LONGTEXT NOT NULL COMMENT 'History of evaluation modifications, including username and date.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_public_annotations` MODIFY `public_comments_variant` LONGTEXT NOT NULL COMMENT 'Public comments about the variant, visible and modifiable by all users.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` MODIFY `evaluation_comments` LONGTEXT NOT NULL COMMENT 'Comments visible to all users but modifiable only by users associated with the sample.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` MODIFY `history` LONGTEXT NOT NULL COMMENT 'History of evaluation modifications, including username and date.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` MODIFY `public_comments_variant` LONGTEXT NOT NULL COMMENT 'Public comments about the variant, visible and modifiable by all users.'");
		}
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `pathologies` DROP COLUMN `partition`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `partition`");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'partition'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` ADD COLUMN `stats` VARCHAR(14) NOT NULL COMMENT 'Current statistics version (generated by the dbBuilder module)' AFTER `icon`");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `analyses` SET `stats` = '20160629000000'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `main` DROP COLUMN `stats`");
		//if (DB.getDataSource(Schema.HIGHLANDER).getDBMS() != DBMS.impala){
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('dann_score','effect DANN score for functional prediction of a variant. Please refer to Quang D et al.(2015) Bioinformatics. 2015;31(5):761-763. doi:10.1093/bioinformatics/btu703 for details. The higher the score (ranging from 0 to 1) the more likely the variant is pathogenic. Please note the following licensing statement: DANN scores and the DANN algorithm are freely available for all non-commercial applications. If you are planning on using them in a commercial application, please contact Daniel Quang (dxquang@uci.edu).','DANN 05/2016','effect prediction','small','LEFT')");
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('eigen_score','Eigen is an aggregated meta-score for the functional annotation of genetic variants in coding and noncoding regions. Please refer to Nature Genetics 48, 214–220 (2016) doi:10.1038/ng.3477 for details. The higher the score the more likely the variant has damaging effects.','Eigen 05/2016','effect prediction','small','LEFT')");
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('eigen_pc_score','Related to Eigen, conceptually simpler meta-score. Please refer to Quang D et al.(2015) Bioinformatics. 2015;31(5):761-763. doi:10.1093/bioinformatics/btu703 for details.','Eigen 05/2016','effect prediction','small','LEFT')");
		//}
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `value` = 'partition'");
	}

}
