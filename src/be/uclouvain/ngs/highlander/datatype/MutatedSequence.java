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

import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;

/**
* @author Raphael Helaers
*/

public class MutatedSequence {

	public enum Type {NUCLEOTIDES, AMINO_ACIDS}
	
	private int rangeAA;
	private Variant variant;
	private Gene gene;
	private Reference genome;
	private String nucl_ref = "";
	private String nucl_mut = "";
	private String nucl_rev_ref = "";
	private String nucl_rev_mut = "";
	private String aa_ref = "";		
	private String aa_mut = "";		
	
	private boolean stopReached = false;
	private boolean insDone = false;
	
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
			stopReached = false;
			insDone = false;
			if (gene.isStrandPositive()) {
				for (Interval interval : intervals) {
					for (int pos = interval.getStart() ; pos <= interval.getEnd() && !stopReached ; pos++){
						processReference(pos, true);
						processMutant(pos, true);
					}
				}
			}else {
				for (int i=intervals.size()-1 ; i >= 0 ; i--) {
					Interval interval = intervals.get(i);
					for (int pos = interval.getEnd() ; pos >= interval.getStart() && !stopReached ; pos--){
						processReference(pos, false);
						processMutant(pos, false);
					}									
				}
				nucl_rev_ref = Tools.reverseComplement(nucl_ref);
				nucl_rev_mut = Tools.reverseComplement(nucl_mut);
			}
		}
	}

	private void processReference(int pos, boolean fwd) {
		int codonPos = gene.getCodonPos(pos);
		String AAs = gene.getAminoAcid(pos);
		if (!AAs.equals("#") && codonPos == 2) {
			aa_ref += AAs.charAt(0);			
		}		
		if (fwd) {
			nucl_ref += gene.getNucleotide(pos);
		}else {
			nucl_ref = gene.getNucleotide(pos) + nucl_ref;
		}
	}
	
	private void processMutant(int pos, boolean fwd) {
		int codonPos = gene.getCodonPos(pos, variant);
		String AAs = gene.getAminoAcid(pos, variant);
		if (!AAs.equals("#") && AAs.length() == 1) {
			char AA = AAs.charAt(0);
			if(codonPos == 2) {
				aa_mut += AA;
				stopReached = (AA == '*');
			}
		} else if (!AAs.equals("#") && AAs.length() > 1 && !insDone) {
			aa_mut += AAs;
			insDone = true;
			stopReached = AAs.contains("*");
		}	
		if (pos == variant.getPosition()) {
			if (fwd) {
				nucl_mut += variant.getAlternative();
			} else  {
				nucl_mut = variant.getAlternative() + nucl_mut;
			}
		} else if (variant.getVariantType() == VariantType.DEL && pos > variant.getPosition() && pos <= variant.getPosition() + variant.getLength()) {
			//do nothing, deleted nucleotide
		} else {
			if (fwd) {
				nucl_mut += gene.getNucleotide(pos);
			} else {
				nucl_mut = gene.getNucleotide(pos) + nucl_mut;
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

	public String getSequence(Type type, boolean mutation, boolean reverseComplementIfGeneIsInReverse) {
		switch (type) {
		case AMINO_ACIDS:
			if (mutation) return aa_mut;
			else return aa_ref;
		case NUCLEOTIDES:
			if (mutation) {
				if (reverseComplementIfGeneIsInReverse) return nucl_rev_mut;
				else return nucl_mut;
			}else {
				if (reverseComplementIfGeneIsInReverse) return nucl_rev_ref;
				else return nucl_ref;				
			}
		default:
			return "?";
		}
	}

}
