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

package be.uclouvain.ngs.highlander.datatype;

import java.util.ArrayList;
import java.util.List;

import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;

/**
* @author Raphael Helaers
*/

public class MutatedSequence {

	private int rangeAA;
	private Variant variant;
	private Gene gene;
	private Reference genome;
	private String reference = "";
	private String nucleotides = "";
	private String aminoacids = "";		
	
	public MutatedSequence(Variant variant, Gene gene, Reference genome, int rangeAA) throws Exception {
		this.variant = variant;
		this.gene = gene;
		this.genome = genome;
		this.rangeAA = rangeAA;
		List<Interval> intervals = new ArrayList<Interval>();			
		if (gene.isExonic(variant.getPosition(), false)) {
			int rangeLeft = rangeAA*3;
			int rangeRight = rangeAA*3;
			int codonPos = gene.getCodonPos(variant.getPosition(), variant);
			String AAs = gene.getAminoAcid(variant.getPosition(), variant);
			if (!AAs.equals("#") && AAs.length() == 1) {
				if((codonPos == 0 && gene.isStrandPositive()) || (codonPos == 2 && !gene.isStrandPositive())) {
					rangeRight +=2;
				}else if((codonPos == 2 && gene.isStrandPositive()) || (codonPos == 0 && !gene.isStrandPositive())) {
					rangeLeft +=2;
				}else {
					rangeRight +=1;
					rangeLeft +=1;
				}
			}else if (!AAs.equals("#") && AAs.length() > 1) {
				rangeRight -= (AAs.length()-1)*3;
				rangeRight += 2;
			}
			if (variant.getVariantType() == VariantType.DEL) {
				rangeRight += ((variant.getLength())%3);
			}else if (variant.getVariantType() == VariantType.INS) {
				if (variant.getLength()%3 == 1){
					rangeRight += 2;
				}else if (variant.getLength()%3 == 2){
					rangeRight += 1;
				}
			}
			int right = 0;
			for (int pos = variant.getPosition()+1 ; right < rangeRight ; pos++){
				if (gene.isExonic(pos, false) && pos < gene.getTranslationEnd()) {
					right++;
				}else {
					break;
				}
			}
			int left = 0;
			for (int pos = variant.getPosition()-1 ; left < rangeLeft ; pos--){
				if (gene.isExonic(pos, false) && pos > gene.getTranslationStart()) {
					left++;
				}else {
					break;
				}
			}
			intervals.add(new Interval(genome, variant.getChromosome(), variant.getPosition()-left, variant.getPosition()+right));
			if (left  < rangeLeft) {
				int thisleft=0;
				int exon = gene.getExonNum(variant.getPosition(), false);
				while(left < rangeLeft) {
					exon--;
					if (exon < 0) {
						break;
					}
					for (int pos = gene.getExonEnd(exon); left < rangeLeft ; pos--){
						if (gene.isExonic(pos, false) && pos > gene.getTranslationStart()) {
							left++;
							thisleft++;
						}else {
							break;
						}					
					}
					if (thisleft > 0) intervals.add(0, new Interval(genome, variant.getChromosome(), gene.getExonEnd(exon)-thisleft+1, gene.getExonEnd(exon)));
					thisleft=0;
				}
			}
			if (right  < rangeRight) {
				int thisright=0;
				int exon = gene.getExonNum(variant.getPosition(), false);
				while(right < rangeRight) {
					exon++;
					if (exon >= gene.getExonCount()) {
						break;
					}
					for (int pos = gene.getExonStart(exon); right < rangeRight ; pos++){
						if (gene.isExonic(pos, false) && pos < gene.getTranslationEnd()) {
							right++;
							thisright++;
						}else {
							break;
						}					
					}
					if (thisright > 0) intervals.add(new Interval(genome, variant.getChromosome(), gene.getExonStart(exon), gene.getExonStart(exon)+thisright-1));
					thisright=0;
				}
			}
			for (Interval interval : intervals) {
				reference += DBUtils.getSequence(interval.getReferenceGenome(), interval.getChromosome(), interval.getStart(), interval.getEnd());
				boolean insDone = false;
				for (int pos = interval.getStart() ; pos <= interval.getEnd() ; pos++){
					codonPos = gene.getCodonPos(pos, variant);
					AAs = gene.getAminoAcid(pos, variant);
					if (!AAs.equals("#") && AAs.length() == 1) {
						char AA = AAs.charAt(0);
						if((codonPos == 0 && gene.isStrandPositive()) || (codonPos == 2 && !gene.isStrandPositive())) {
							//codon pos 1 do nothing
						}else if((codonPos == 2 && gene.isStrandPositive()) || (codonPos == 0 && !gene.isStrandPositive())) {
							//codon pos 3 do nothing
						}else {
							aminoacids += AA;
						}
					}else if (!AAs.equals("#") && AAs.length() > 1 && !insDone) {
						aminoacids += AAs;
						insDone = true;
					}
					if (pos == variant.getPosition()) {
						nucleotides += variant.getAlternative();
					}else if (variant.getVariantType() == VariantType.DEL && pos > variant.getPosition() && pos <= variant.getPosition() + variant.getLength()) {
						//do nothing, deleted nucleotide
					}else {
						nucleotides += gene.getNucleotide(pos);
					}
				}				
			}
		}
	}

	public int getRangeAA() {
		return rangeAA;
	}

	public Variant getVariant() {
		return variant;
	}

	public Gene getGene() {
		return gene;
	}

	public Reference getGenome() {
		return genome;
	}

	public String getReference() {
		return reference;
	}

	public String getNucleotides() {
		return nucleotides;
	}

	public String getAminoacids() {
		return aminoacids;
	}

}
