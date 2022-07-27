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

/**
 * @author Raphael Helaers
 */

public class Version_17_13 extends Version {

	public Version_17_13() {
		super("17.13");
	}

	@Override
	protected void makeUpdate() throws Exception {
		
		toConsole("---[ Update FastQC columns in projects ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE COLUMN `per_base_GC_content` `per_tile_sequence_quality` ENUM('pass', 'warn', 'fail') NULL DEFAULT NULL AFTER `per_base_sequence_quality`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE COLUMN `kmer_content` `adapter_content` ENUM('pass', 'warn', 'fail') NULL DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` SET adapter_content = NULL, per_tile_sequence_quality = NULL");
		toConsole("---[ Update consensus_prediction description ]---");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Consensus between prediction of all available software.  The base score reflects the number software that predict the variant to be damaging:  Each of the following software having a DAMAGING prediction add +1: Mutation Taster, FATHMM, FATHMM-XF, Polyphen2 (HDIV), Provean, SIFT4G, Mutation Assessor, MCAP, LRT, Lists2, Deogen, ClinPred, BayesDel (with MaxMAF), PrimateAI and MetaSVM. Each of the following scores add +1 when above a certain threshold: CADD phred > 20, VEST > 0.5, REVEL > 0.5, MVP > 0.75 and MutPred > 0.75  So max 20 if all software agree.  If the variant could affect splicing, a +1 or +2 could be added to the base score, if ada and/or rf predictions are AFFECTING_SPLICING.  To this, value is added depending on certain variant impact (annotations from SnpEff): +200 if any prediction indicates that splicing is affected. +300 for SPLICE_SITE_ACCEPTOR, SPLICE_SITE_DONOR, the 2 first or 2 last positions of an exon, the 3rd/4th/5th positions in 3\\' intron of an exon (before the STOP). +400 for FRAME_SHIFT, STOP_GAINED, STOP_LOST, START_LOST or RARE_AMINO_ACID. +500 for high impact structural variants. So, filtering on consensus_prediction > 0 should yield variants potentially damaging. Choosing a higher value like consensus_prediction > 5 should yield variants probably damaging.' WHERE `field`='consensus_prediction'");

	}

}
