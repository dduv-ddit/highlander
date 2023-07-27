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

package be.uclouvain.ngs.highlander.datatype;

import java.io.StringWriter;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.StructuralVariantType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMTextHeaderCodec;

public class Variant implements Comparable<Variant> {

	private String chr;
	private int pos;
	private int length;
	private String ref;
	private String alt;
	private VariantType variantType;
	private StructuralVariantType svType;

	public Variant(String chr, int pos, int length, String reference, String alternative, StructuralVariantType svType){
		this.chr = chr;
		this.pos = pos;
		this.length = length;
		this.ref = reference;
		this.alt = alternative;
		this.variantType = VariantType.SV;
		this.svType = svType;
	}

	public Variant(String chr, int pos, int length, String reference, String alternative, VariantType variant_type){
		this.chr = chr;
		this.pos = pos;
		this.length = length;
		this.ref = reference;
		this.alt = alternative;
		this.variantType = variant_type;
		if (variantType != null) inferStructuralVariantType();
	}
	
	public Variant(String chr, int pos){
		this(chr,pos,null,null);
	}

	public Variant(String chr, int pos, String reference, String alternative, VariantType variant_type){
		this.chr = chr;
		this.pos = pos;
		this.ref = reference;
		this.alt = alternative;
		this.variantType = variant_type;
		inferStructuralVariantType();
		inferLength();
	}

	public Variant(String chr, int pos, int length, String reference, String alternative){
		this.chr = chr;
		this.pos = pos;
		this.length = length;
		this.ref = reference;
		this.alt = alternative;
		if (reference != null && alternative != null) {
			inferVariantType();
		}
	}
	
	public Variant(String chr, int pos, String reference, String alternative){
		this.chr = chr;
		this.pos = pos;
		this.ref = reference;
		this.alt = alternative;
		if (reference != null && alternative != null) {
			inferVariantType();
			inferLength();
		}
	}

	private void inferVariantType() {
		if (ref.equalsIgnoreCase("N") || ref.contains("<")) {
			variantType = VariantType.SV;
			inferStructuralVariantType();
		}else if (ref.length() == 1 && alt.length() == 1){
			variantType = VariantType.SNV;
		}else if (ref.length() > alt.length()){
			variantType = VariantType.DEL;
		}else if (ref.length() < alt.length()){
			variantType = VariantType.INS;
		}else{
			variantType = VariantType.MNV;
		}
	}
	
	private void inferStructuralVariantType() {
		switch(variantType) {
		case SV:
			switch(alt) {
			case "<DEL>":
				svType = StructuralVariantType.DEL;
				break;
			case "<INS>":
				svType = StructuralVariantType.INS;
				break;
			case "<DUP>":
				svType = StructuralVariantType.DUP;
				break;
			case "<INV>":
				svType = StructuralVariantType.INV;
				break;
			case "<BND>":
				svType = StructuralVariantType.BND;				
				break;
			case "<ALU>": //not sure about this one
				svType = StructuralVariantType.ALU;				
				break;
			case "<SVA>": //not sure about this one
				svType = StructuralVariantType.SVA;				
				break;
			case "<LINE1>": //not sure about this one
				svType = StructuralVariantType.LINE1;				
				break;
			case "<CNV>":
			default: //Also all <CN0>, <CN2>, <CN3>, ...
				svType = StructuralVariantType.CNV;
				break;
			}
			break;
		case SNV:
		case MNV:
		case DEL:
		case INS:
		default:
			svType = null;
			break;
		}
	}
	
	private void inferLength() {
		switch(variantType) {
		case SV:
			//Cannot infer length for SV, must be parsed from the VCF or AnnotSV
			break;
		case SNV:
		case MNV:
			length = ref.length();
			break;
		case DEL:
		case INS:
		default:
			length = Math.abs(ref.length()-alt.length());
			break;
		}
	}
	
	public Variant(int variantSampleId) throws Exception {
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT "+Field.chr+", "+Field.pos+", "+Field.length+", "+Field.reference+", "+Field.alternative+", "+Field.variant_type+" "
				+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
				+ Highlander.getCurrentAnalysis().getJoinStaticAnnotations()
				+ "WHERE "+Field.variant_sample_id.getQueryWhereName(Highlander.getCurrentAnalysis(), false)+" = " + variantSampleId
				)) {
			if (res.next()){
				this.chr = res.getString(Field.chr.getName());
				this.pos = res.getInt(Field.pos.getName());
				this.length = res.getInt(Field.length.getName());
				this.ref = res.getString(Field.reference.getName());
				this.alt = res.getString(Field.alternative.getName());
				this.variantType = VariantType.valueOf(res.getString(Field.variant_type.getName()));
				inferStructuralVariantType();
			}else{
				throw new Exception("Id " + variantSampleId + " not found in the database");
			}
		}
	}

	public static Set<Variant> fetchVariants(List<Integer> variantSampleIds) throws Exception {
		Set<Variant> set = new TreeSet<Variant>();
		if (!variantSampleIds.isEmpty()){
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT "+Field.chr+", "+Field.pos+", "+Field.length+", "+Field.reference+", "+Field.alternative+", "+Field.variant_type+" "
							+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
							+ Highlander.getCurrentAnalysis().getJoinStaticAnnotations()
							+ "WHERE "+Field.variant_sample_id.getQueryWhereName(Highlander.getCurrentAnalysis(), false)+" IN (" + HighlanderDatabase.makeSqlList(variantSampleIds, Integer.class) + ")"
					)) {
				while (res.next()){
					set.add(new Variant(res.getString(Field.chr.getName()), res.getInt(Field.pos.getName()), res.getInt(Field.length.getName()),  res.getString(Field.reference.getName()), res.getString(Field.alternative.getName()), VariantType.valueOf(res.getString(Field.variant_type.getName()))));
				}
			}
		}
		return set;
	}

	public String getChromosome() {
		return chr;
	}

	public String getChromosome(SAMFileHeader header){
		if (header.getTextHeader() != null){
			if (header.getTextHeader().contains("chr"+getChromosome())){
				return "chr"+getChromosome();
			}else if (header.getTextHeader().contains("CHR"+getChromosome())){
				return "CHR"+getChromosome();
			}else{
				return ""+getChromosome();
			}				
		}else{
			final SAMTextHeaderCodec codec = new SAMTextHeaderCodec();
			codec.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
			final StringWriter stringWriter = new StringWriter();
			codec.encode(stringWriter, header);
			if (stringWriter.toString().contains("chr"+getChromosome())){
				return "chr"+getChromosome();
			}else if (stringWriter.toString().contains("CHR"+getChromosome())){
				return "CHR"+getChromosome();
			}else{
				return ""+getChromosome();
			}	
		}
	}

	/**
	 * Position as encoded in the VCF.
	 * @return Position as encoded in the VCF.
	 */
	public int getPosition() {
		return pos;
	}

	public int getLength() {
		return length;
	}
	
	/**
	 * Position where the alternative starts.
	 * Same than getPosition() for SNV and MNV and INS, and getPosition()+1 for DEL.
	 * Note that some variants are problematics as encoded in the VCF, i.e. when REF and ALT are both greater than 1 for INDELs. 
	 * For DEL, the position then SHOULD BE shifted by the size of the ALT (so it's equal to the position of the first "missing" base).
	 * For INS, the position then SHOULD BE shifted by the size of the REF (so it's equal to the position of the first base before the insertion) ; 
	 * but there is no concordance between this notation in GATK and reads in the BAM for those cases.
	 * For example, take this INS from the VCF: pos=1000;ref=ACTG;alt=ACTGCTG.
	 * should be an INS of CTG at pos 1003 (the CTG in ref indicate a stretch of CTG and tells that the insertion is after the first CTG -- which seems odd, how the hell they know that ?)
	 * But in the BAM, the INS is at pos 1000 in the reads.
	 * So the notation should really have been pos=1000;ref=A;alt=ACTG.
	 * Same goes for similar DEL, they always start at pos+1, even if the ALT is bigger than 1.
	 * It means that this method and getAlternativeChangedNucleotides() should be safe to use when drawing alignments and computing codons and amino acids.
	 * 
	 * Update 09/2020: added a correction in the VCF importation that "reduces" the ref/alt to the minimum. Those cases should not happen anymore.
	 * 
	 * @return Position where the alternative starts
	 */
	public int getAlternativePosition() {
		if (variantType == null) return pos;
		switch(variantType) {
		case DEL:
			return pos+1;
			//return pos + alt.length();
		case INS:
			return pos;
			//return pos + ref.length() - 1;
		case SV: //Not tested, maybe not useful
			switch(svType) {
			case DEL:
				return pos+1;
			case INS:
			case DUP:
				return pos;
			case CNV:
			case INV:
			case BND:
			case ALU:
			case LINE1:
			case SVA:
			default:
				return pos;
			}
		case MNV:
		case SNV:
		default:
			return pos;
		}
	}

	/**
	 * Number of nucleotides affected by the variant in the reference, after the position.
	 * Always zero for INS and SNV, and length-1 for DEL and MNV
	 * 
	 * @return
	 */
	public int getAffectedReferenceLength() {
		if (variantType == null) return 0;
		switch(variantType) {
		case SV: //Not tested, maybe not useful
			return length;
		case INS:
		case SNV:
			return 0;
		case DEL:
		case MNV:
		default:
			return length-1;
		}
	}
	
	public String getReference() {
		return ref;
	}

	/**
	 * Alternative allele as encoded in the VCF.
	 * For example:
	 * SNV A>G: ref=A ; alt=G.
	 * DEL of GTTC: ref=AGTTC ; alt=A.
	 * INS of GGC: ref=A ; alt=AGGC.
	 * MNV AT>GC: ref=AT ; alt=GC.
	 * Note that some variants can be poorly expressed like:
	 * INS of CTG: ref=ACTG, alt=ACTGCTG.
	 * SNV T>G: ref=AT, alt=AG.
	 * So just "removing" the first nucleotide for INDEL can lead to errors.
	 * @return Alternative allele as encoded in the VCF
	 */
	public String getAlternative() {
		return alt;
	}

	/**
	 * Returns the changed nucleotide from the alternative allele.
	 * Correspond to the alternative allele for SNV and MNV, inserted bases for INS and stretch of '-'s for DEL (corresponding to the number of bases deleted). 
	 * For example:
	 * SNV A>G: ref=A ; alt=G will return "G".
	 * DEL of GTTC: ref=AGTTC ; alt=A will return "---".
	 * INS of GGC: ref=A ; alt=AGGC will return "GGC".
	 * MNV AT>GC: ref=AT ; alt=GC will return "GC".
	 * Note that some variants can be poorly expressed like: 
	 * INS of CTG: ref=ACTG, alt=ACTGCTG will correctly return "CTG" with this method.
	 * DEL of CTG: ref=ACTGCTG, alt=ACTG will correctly return "---" with this method.
	 * SNV T>G: ref=AT, alt=AG will return "AG" because it's annotated as a MNV, and the implications are not annoying as with INDEL.
	 * See also getAlternativePosition() method.
	 * @return Changed nucleotide from the alternative allele
	 */
	public String getAlternativeChangedNucleotides() {
		switch(variantType) {
		case DEL:
			String del = "";
			for (int i=alt.length(); i < ref.length() ; i++) {
				del += "-";
			}
			return del;
		case INS:
			return alt.substring(ref.length());
		case MNV:
			return alt;
		case SNV:
			return alt;
		case SV:
			return "";
		default:
			return null;
		}
	}

	public VariantType getVariantType() {
		return variantType;
	}

	public StructuralVariantType getStructuralVariantType() {
		return svType;
	}
	
	@Override
	public String toString(){
		return chr+":"+pos;
	}

	/**
	 * 
	 * @param from source reference
	 * @param to	destination reference
	 * @return the variant with position in destination reference or null if the position doesn't exist in destination reference
	 * @throws Exception
	 */
	public Variant liftOver(Reference from, Reference to) throws Exception {
		System.out.println("Submiting to rest.ensembl.org liftover of " + toString() + " from " + from + " to " + to);
		String result = Tools.httpGet("https://rest.ensembl.org/map/"+from.getSpecies()+"/GRC"+from.getSpecies().charAt(0)+from.getGenomeVersion()+"/"+chr+":"+pos+".."+pos+"/GRC"+to.getSpecies().charAt(0)+to.getGenomeVersion()+"?content-type=application/json");
		JsonObject json = new JsonParser().parse(result).getAsJsonObject();
		System.out.println("DONE: liftover of " + toString() + " from " + from + " to " + to);
		if (json.getAsJsonArray("mappings").size() > 0) {
			JsonObject mapped = json.getAsJsonArray("mappings").get(0).getAsJsonObject().getAsJsonObject("mapped");
			String chr = mapped.getAsJsonPrimitive("seq_region_name").getAsString();
			int pos = mapped.getAsJsonPrimitive("start").getAsInt();
			return new Variant(chr, pos, length, ref, alt, variantType);
		}else {
			return null;
		}
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Variant))
			return false;
		return compareTo((Variant)obj) == 0;
	}

	@Override
	public int compareTo(Variant v){
		if (chr.equals(v.chr)){
			if (pos == v.pos){
				if (variantType == v.variantType){
					if ((ref == null && v.ref == null) || ref.equals(v.ref)){
						if (alt == null && v.alt == null) return 0;
						else return alt.compareTo(v.alt);
					}else{
						return ref.compareTo(v.ref);
					}
				}else{
					return variantType.compareTo(variantType);
				}
			}else{
				Integer a = new Integer(pos);
				Integer b = new Integer(v.pos);
				return a.compareTo(b);
			}
		}else{
			return new Tools.NaturalOrderComparator(true).compare(chr, v.chr);
		}
	}
}
