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

public class Version_17_12 extends Version {

	public Version_17_12() {
		super("17.12");
	}

	@Override
	protected void makeUpdate() throws Exception {
		
		toConsole("---[ Add new fields ]---");
		for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE "+analysis.getFromSampleAnnotations()+" ADD COLUMN `symmetric_odds_ratio` DOUBLE DEFAULT NULL AFTER `mapping_quality`");
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields_analyses` VALUES ('symmetric_odds_ratio','"+analysis+"')");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE "+analysis.getFromStaticAnnotations()+" ADD COLUMN `snpeff_all_effects` VARCHAR(400) DEFAULT NULL AFTER `snpeff_effect`, ADD INDEX `snpeff_all_effects` (`snpeff_all_effects`)");
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields_analyses` VALUES ('snpeff_all_effects','"+analysis+"')");
		}
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` (`field`,`table`,`sql_datatype`,`json`,`description`,`annotation_code`,`annotation_header`,`source`,`ordering`,`category`,`size`,`alignment`) VALUES ('symmetric_odds_ratio','_sample_annotations','DOUBLE','INFO','Symmetric Odds Ratio of 2x2 contingency table to detect strand bias','VCF','INFO|SOR','GATK 4.2','6','confidence','small','CENTER');\n");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` (`field`,`table`,`sql_datatype`,`json`,`description`,`annotation_code`,`annotation_header`,`source`,`ordering`,`category`,`size`,`alignment`) VALUES ('snpeff_all_effects','_static_annotations','VARCHAR(400)','INFO','All effects of this variant predicted by SnpEff.','VCF','INFO|ANN','snpEff 5.0e','2','effect prediction','largest','LEFT');\n");		
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `ordering`='10' WHERE `field`='haplotype_score'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `ordering`='3' WHERE `field`='snpeff_impact'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Most damaging effect of this variant predicted by SnpEff.' WHERE `field`='snpeff_effect'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Consensus between prediction of all available software.  The base score reflects the number software that predict the variant to be damaging:  Each of the following software having a DAMAGING prediction add +1: Mutation Taster, FATHMM, FATHMM-XF, Polyphen2 (HDIV), Provean, SIFT4G, Mutation Assessor, MCAP, LRT, Lists2, Deogen, ClinPred, BayesDel (with MaxMAF), PrimateAI and MetaSVM. Each of the following scores add +1 when above a certain threshold: CADD phred > 20, VEST > 0.5, REVEL > 0.5, MVP > 0.75 and MutPred > 0.75  So max 20 if all software agree.  If the variant could affect splicing, a +1 or +2 could be added to the base score, if ada and/or rf predictions are AFFECTING_SPLICING.  To this, value is added depending on certain variant impact (annotations from SnpEff): +200 if any prediction indicates that splicing is affected. +300 for SPLICE_SITE_ACCEPTOR, SPLICE_SITE_DONOR or if a variant combines a SPLICE_SITE_REGION and exonic effect. +400 for FRAME_SHIFT, STOP_GAINED, STOP_LOST, START_LOST or RARE_AMINO_ACID. +500 for high impact structural variants. So, filtering on consensus_prediction > 0 should yield variants potentially damaging. Choosing a higher value like consensus_prediction > 5 should yield variants probably damaging.' WHERE `field`='consensus_prediction'");
	}

}
