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
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMTextHeaderCodec;

public class Interval implements Comparable<Interval> {

	private Reference referenceGenome;
	private String chr;
	private int start;
	private int end;

	public Interval(Reference reference, String chromosome, int start, int end){
		this.referenceGenome = reference;
		if (chromosome.startsWith("chr")) this.chr = chromosome.substring(3); 
		else this.chr = chromosome;
		this.start = start;
		this.end = end;
	}

	public Interval(Reference reference, String interval) throws NumberFormatException {
		this.referenceGenome = reference;
		String[] parts = interval.split(":");
		if (parts[0].startsWith("chr")) this.chr = parts[0].substring(3); 
		else this.chr = parts[0];
		if (parts[1].contains("-")){
			this.start = Integer.parseInt(parts[1].split("-")[0]);
			this.end = Integer.parseInt(parts[1].split("-")[1]);
		}else{
			this.start = this.end = Integer.parseInt(parts[1]);
		}
	}

	public static Set<Interval> fetchIntervals(List<Integer> variantSampleIds) throws Exception {
		Set<Interval> set = new TreeSet<Interval>();
		if (!variantSampleIds.isEmpty()){
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT chr, pos, reference "
					+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations() 
					+ "WHERE variant_sample_id IN (" + HighlanderDatabase.makeSqlList(variantSampleIds, Integer.class) + ")"
					)) {
				while (res.next()){
					String chr = res.getString("chr");
					int pos = res.getInt("pos");
					String ref = res.getString("reference");
					set.add(new Interval(Highlander.getCurrentAnalysis().getReference(), chr, pos, pos+ref.length()-1));
				}
			}
		}
		return set;
	}

	public String toString(){
		return (start != end) ? (chr + ":" + start + "-" + end) : (chr + ":" + start);
	}

	public Reference getReferenceGenome() {
		return referenceGenome;
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

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int getSize(){
		return end-start+1;
	}

	public String getReferenceSequence() throws Exception {
		return DBUtils.getSequence(getReferenceGenome(), getChromosome(), getStart(), getEnd());
	}

	public Interval liftOver(Reference to) throws Exception {
		String result = Tools.httpGet("https://rest.ensembl.org/map/"+referenceGenome.getSpecies()+"/GRC"+referenceGenome.getSpecies().charAt(0)+referenceGenome.getGenomeVersion()+"/"+chr+":"+start+".."+end+"/GRC"+to.getSpecies().charAt(0)+to.getGenomeVersion()+"?content-type=application/json");
		JsonObject json = new JsonParser().parse(result).getAsJsonObject();
		JsonObject mapped = json.getAsJsonArray("mappings").get(0).getAsJsonObject().getAsJsonObject("mapped");
		String chr = mapped.getAsJsonPrimitive("seq_region_name").getAsString();
		int start = mapped.getAsJsonPrimitive("start").getAsInt();
		int end = mapped.getAsJsonPrimitive("end").getAsInt();
		return new Interval(to, chr, start, end);
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
		if (!(obj instanceof Interval))
			return false;
		return compareTo((Interval)obj) == 0;
	}

	@Override
	public int compareTo(Interval i){
		if (chr.equals(i.chr)){
			if (start == i.start){
				Integer a = new Integer(end);
				Integer b = new Integer(i.end);
				return a.compareTo(b);
			}else{
				Integer a = new Integer(start);
				Integer b = new Integer(i.start);
				return a.compareTo(b);
			}				
		}else{
			try {
				Integer a;
				if (chr.equalsIgnoreCase("X")) a = new Integer(23);
				else if (chr.equalsIgnoreCase("Y")) a = new Integer(24);
				else a = Integer.parseInt(chr);
				Integer b;
				if (i.chr.equalsIgnoreCase("X")) b = new Integer(23);
				else if (i.chr.equalsIgnoreCase("Y")) b = new Integer(24);
				else b = Integer.parseInt(i.chr);
				return a.compareTo(b);
			}catch (NumberFormatException ex) {
				//non-numbered contig or X/Y (e.g. MT, GLxxx, non-human species)
				return chr.compareTo(i.chr);
			}
		}
	}
}
