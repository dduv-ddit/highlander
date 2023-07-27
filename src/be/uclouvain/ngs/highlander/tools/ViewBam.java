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

package be.uclouvain.ngs.highlander.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.broad.igv.DirectoryManager;
import org.broad.igv.exceptions.DataLoadException;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.HttpUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.seekablestream.SeekableBufferedStream;
import net.sf.samtools.seekablestream.SeekableFTPStream;

public class ViewBam {

	public final static int A = 0;
	public final static int C = 1;
	public final static int G = 2;
	public final static int T = 3;
	public final static int N = 4;

	public static final long oneDay = 24 * 60 * 60 * 1000;

	private final Map<String,SAMFileReader> inputBams = new TreeMap<String, SAMFileReader>();
	static Hashtable<String, File> indexFileCache = new Hashtable<String, File>();

	public ViewBam(String directory, boolean recursive){
		File dir = new File(directory);
		List<File> bams = new ArrayList<File>();
		if (dir.isDirectory()){
			bams.addAll(getFiles(dir, recursive));
		}else{
			bams.add(dir);
		}
		for (File file : bams){
			if (file.getName().endsWith(".bam")) {
				System.err.print(".");
				SAMFileReader samfr= new SAMFileReader(file);
				samfr.setValidationStringency(ValidationStringency.SILENT);
				inputBams.put(file.getName(), samfr);
			}
		}
		System.err.println("!");
	}

	public ViewBam(Set<String> samples, File dir) {
		List<File> bams = new ArrayList<File>();
		for (File file : dir.listFiles()){
			if (!file.isDirectory()){
				String filename = file.getName();
				String sample = filename.substring(0,filename.lastIndexOf("."));
				if (filename.endsWith(".bam") && samples.contains(sample)) {
					bams.add(file);
				}			
			}
		}
		for (File file : bams){
			System.err.print(".");
			SAMFileReader samfr= new SAMFileReader(file);
			samfr.setValidationStringency(ValidationStringency.SILENT);
			inputBams.put(file.getName(), samfr);
		}
		System.err.println("!");
	}

	public ViewBam(Map<AnalysisFull, Set<String>> samples, String analyzesPath) {
		for (AnalysisFull analysis : samples.keySet()) {
			for (String sample : samples.get(analysis)) {
				File file = new File(analyzesPath + "/" + analysis + "/" + sample + ".bam");
				System.err.print(".");
				SAMFileReader samfr= new SAMFileReader(file);
				samfr.setValidationStringency(ValidationStringency.SILENT);
				inputBams.put(analysis + "|" + sample, samfr);
			}
		}
		System.err.println("!");
	}

	private List<File> getFiles(File directory, boolean recursive){
		List<File> output = new ArrayList<File>();
		for (File f : directory.listFiles()){
			if (f.isDirectory()){
				if (recursive){
					output.addAll(getFiles(f, recursive));
				}
			}else{
				output.add(f);
			}
		}
		return output;
	}

	public ViewBam(URL url) throws IOException {
		SAMFileReader samfr= (url.toString().startsWith("ftp")) ? new SAMFileReader(new SeekableBufferedStream(new SeekableFTPStream(url)), getIndexFile(url, null), false) : new SAMFileReader(url, getIndexFile(url, null), false);
		samfr.setValidationStringency(ValidationStringency.SILENT);
		inputBams.put(url.getFile(), samfr);
	}

	public ViewBam(Map<AnalysisFull, Set<String>> samples, URL urlPath) throws Exception {
		for (AnalysisFull analysis : samples.keySet()) {
			for (String sample : samples.get(analysis)) {
			System.err.print(".");
			URL url = new URL(urlPath + "/" + analysis + "/" + sample+".bam");
			SAMFileReader samfr= (url.toString().startsWith("ftp")) ? new SAMFileReader(new SeekableBufferedStream(new SeekableFTPStream(url)), getIndexFile(url, null), false) : new SAMFileReader(url, getIndexFile(url, null), false);
			samfr.setValidationStringency(ValidationStringency.SILENT);
			inputBams.put(analysis + "|" + sample, samfr);
		}
		}
		System.err.println("!");
	}	

	File getIndexFile(URL url, String indexPath) throws IOException {

		String urlString = url.toString();
		File indexFile = getTmpIndexFile(urlString);

		// Crude staleness check -- if more than a day old discard
		long age = System.currentTimeMillis() - indexFile.lastModified();
		if (age > oneDay) {
			indexFile.delete();
		}

		if (!indexFile.exists() || indexFile.length() < 1) {
			loadIndexFile(urlString, indexPath, indexFile);
			indexFile.deleteOnExit();
		}

		return indexFile;

	}

	private File getTmpIndexFile(String bamURL) throws IOException {
		File indexFile = indexFileCache.get(bamURL);
		if (indexFile == null) {
			indexFile = File.createTempFile("index_", ".bai", DirectoryManager.getCacheDirectory());
			indexFile.deleteOnExit();
			indexFileCache.put(bamURL, indexFile);
		}
		return indexFile;
	}

	private void loadIndexFile(String path, String indexPath, File indexFile) throws IOException {
		InputStream is = null;
		OutputStream os = null;

		try {
			String idx = (indexPath != null && indexPath.length() > 0) ? indexPath : path + ".bai";
			URL indexURL = new URL(idx);
			os = new FileOutputStream(indexFile);
			try {
				is = HttpUtils.getInstance().openConnectionStream(indexURL);
			} catch (FileNotFoundException e) {
				// Try other index convention
				String baseName = path.substring(0, path.length() - 4);
				indexURL = new URL(baseName + ".bai");

				try {
					is = org.broad.igv.util.HttpUtils.getInstance().openConnectionStream(indexURL);
				} catch (FileNotFoundException e1) {
					MessageUtils.showMessage("Index file not found for file: " + path);
					throw new DataLoadException("Index file not found for file: " + path, path);
				}
			}
			byte[] buf = new byte[512000];
			int bytesRead;
			while ((bytesRead = is.read(buf)) != -1) {
				os.write(buf, 0, bytesRead);
			}

		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
				}
			}

		}
	}

	/**
	 * 
	 * Bam per bam
	 * Seems faster !
	 * 
	 */
	public Set<Variant> checkCandidatesForSpecErrors(Set<Variant> candidates){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		Set<Variant> validated = new HashSet<Variant>();
		int counter = 0;
		Map<Variant,Integer> founds = new HashMap<Variant, Integer>();
		for (Variant candidate : candidates){
			founds.put(candidate, 0);
		}
		int nbams = inputBams.size();
		for (SAMFileReader bam : inputBams.values()){
			for (Variant candidate : candidates){
				counter++;
				int startPos = candidate.getPosition();
				int endPos = startPos + candidate.getReference().length() -1;
				String reference = candidate.getReference();					
				String alternative = candidate.getAlternative();
				if (candidate.getVariantType() == VariantType.DEL){
					for (int i = alternative.length() ; i < reference.length() ; i++){
						alternative += "-";
					}
				}
				SAMRecordIterator it =  bam.query(candidate.getChromosome(bam.getFileHeader()), startPos, endPos, false);
				int reads = 0;
				int altfound = 0;
				while(it.hasNext()){
					Optional<String> optionalPattern = ViewBam.getPattern(it.next(), startPos, endPos);
					if (optionalPattern.isPresent()){
						String pattern = optionalPattern.get();
						reads++;
						if(alternative.equalsIgnoreCase(pattern)){
							altfound++;
						}
					}
				}
				it.close();
				if (reads > 4 && altfound > 1) founds.put(candidate, founds.get(candidate)+1);
				if (counter % (500*nbams) == 0) System.out.println(df.format(System.currentTimeMillis()) + " - " + (int)(counter/(double)nbams) + "/" + candidates.size() + " candidates checked on all bams ...");
			}
		}
		for (Variant candidate : candidates){
			double prop = (double)founds.get(candidate)/(double)nbams;
			if (prop > 0.3) validated.add(candidate);
		}
		return validated;
	}  

	public Map<Interval, Object[][]> getPatterns(Set<Interval> positions) throws Exception {
		Map<Interval,Map<String,Map<String,Integer>>> intervals = new TreeMap<Interval, Map<String,Map<String,Integer>>>();
		String NREADS="#reads";
		for (String bam : inputBams.keySet()){
			System.err.print("+");
			try {
				for (Interval pos : positions){
					if (!intervals.containsKey(pos)){
						intervals.put(pos, new TreeMap<String, Map<String,Integer>>());
					}
					Map<String,Map<String,Integer>> bams = intervals.get(pos); 
					if (!bams.containsKey(bam)){
						Map<String, Integer> patterns = new HashMap<String, Integer>();
						if(pos.getSize() == 1){
							patterns.put("A", 0);
							patterns.put("C", 0);
							patterns.put("G", 0);
							patterns.put("T", 0);
						}
						bams.put(bam, patterns);
					}
					Map<String,Integer> patterns = bams.get(bam);								
					SAMRecordIterator it =  inputBams.get(bam).query(pos.getChromosome(inputBams.get(bam).getFileHeader()), pos.getStart(), pos.getEnd(), false);
					int total = 0;
					while(it.hasNext()){
						//Check if the interval falls completely inside the read, drop it if not									
						Optional<String> optionalPattern = ViewBam.getPattern(it.next(), pos.getStart(), pos.getEnd());
						if (optionalPattern.isPresent()){
							String pattern = optionalPattern.get();
							if (!patterns.containsKey(pattern)){
								patterns.put(pattern, 0);
							}
							patterns.put(pattern, patterns.get(pattern)+1);
							total++;
						}
					}
					it.close();
					patterns.put(NREADS, total);
				}
				inputBams.get(bam).close();
			}catch(Exception ex){
				System.err.println("BamViewer: problem with bam " + bam);
				ex.printStackTrace();
			}
		}
		System.err.println();
		System.err.println("Creating matrix");
		Map<Interval, Object[][]> res = new TreeMap<Interval, Object[][]>();
		for (Interval pos : positions){
			Map<String,Map<String,Integer>> bams = intervals.get(pos);
			Map<String,Integer> totalPatterns = new HashMap<String,Integer>();
			for (Map<String,Integer> patterns : bams.values()){
				for (String pattern : patterns.keySet()){
					if (!totalPatterns.containsKey(pattern)){
						totalPatterns.put(pattern, 0);
					}
					totalPatterns.put(pattern, totalPatterns.get(pattern)+patterns.get(pattern));						
				}
			}
			List<String> headers = new ArrayList<String>();
			for (String pattern : totalPatterns.keySet()){
				int i = 0;
				while (!pattern.equals(NREADS) && i < headers.size() && totalPatterns.get(headers.get(i)) > totalPatterns.get(pattern)){
					i++;
				}
				headers.add(i, pattern);
			}				
			String ref = pos.getReferenceSequence();
			if (headers.remove(ref)) {
				headers.add(1, ref);
			}
			headers.add(0, "Analysis");
			headers.add(1, "BAM");
			headers.add(2, "Pathology");
			Object[][] data = new Object[bams.size()+1][headers.size()];
			data[0] = headers.toArray(new String[0]);
			int row = 1;
			for (String key : bams.keySet()){
				int col = 0;
				for (String pattern : headers){
					if (pattern.equals("BAM")){
						if (key.contains("|")) {
							data[row][col] = key.split("\\|")[1];
						}else {
							data[row][col] = key;
						}
					}else if (pattern.equals("Analysis")) {
						if (key.contains("|")) {
							data[row][col] = key.split("\\|")[0];
						}else {
							data[row][col] = "UNKNOWN";
						}
					}else if (pattern.equals("Pathology")) {
						String pathology = "UNKNOWN";
						String sample = (key.contains("|")) ? key.split("\\|")[1] : key;
						try (Results r = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT pathology FROM projects JOIN pathologies USING (pathology_id) WHERE sample = '"+sample+"'")){
							if (r.next()) {
								pathology = r.getString(1);
							}
						}
						data[row][col] = pathology;
					}else if (bams.get(key).containsKey(pattern)){
						data[row][col] = bams.get(key).get(pattern);
					}else{
						data[row][col] = 0;
					}
					col++;
				}
				row++;
			}
			res.put(pos, data);
		}			
		return res;
	}

	public void toExcel(Map<Interval, Object[][]> patterns, String filename){
		if (!filename.endsWith(".xlsx")) filename += ".xlsx";
		File xls = new File(filename);
		try{
			try(Workbook wb = new SXSSFWorkbook(100)){
				System.out.println("Preparing file ...");		
				for (Interval pos : patterns.keySet()){
					Object[][] table = patterns.get(pos);
					String sheetname = pos.toString().replace(':', '-');
					if (sheetname.length() > 30) sheetname = sheetname.substring(sheetname.length()-30, sheetname.length());
					Sheet sheet = wb.createSheet(sheetname);
					sheet.createFreezePane(0, 1);		
					int r = 0;
					Row row = sheet.createRow(r++);
					row.setHeightInPoints(50);
					for (int c = 0 ; c < table[0].length ; c++){
						row.createCell(c).setCellValue(table[0][c].toString());
					}
					sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table[0].length-1));
					for (int i=1 ; i < table.length ; i++ ){
						row = sheet.createRow(r++);
						for (int c = 0 ; c < table[i].length ; c++){
							if (table[i][c] == null)
								row.createCell(c);
							else if (c > 0)
								row.createCell(c).setCellValue(Integer.parseInt(table[i][c].toString()));
							else 
								row.createCell(c).setCellValue(table[i][c].toString());
						}
					}		
				}
				System.out.println("Writing file ...");		
				try (FileOutputStream fileOut = new FileOutputStream(xls)){
					wb.write(fileOut);
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public void toBamCheck(Map<Interval, Object[][]> patterns, String filename){
		System.err.println("Creating output file");
		File output = new File(filename);
		try(FileWriter fw = new FileWriter(output)){
			for (Interval pos : patterns.keySet()){
				Object[][] table = patterns.get(pos);
				fw.write(pos.toString() + "\n");
				fw.write("##\n");
				for (int i=0 ; i < table.length ; i++ ){
					for (int c = 0 ; c < table[i].length ; c++){
						fw.write(table[i][c] + "\t");
					}
					fw.write("\n");
					if (i == 0) fw.write("##\n");
				}
				fw.write("####\n");
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}		
	}

	/**
	 * Return the pattern found in given read at interval [startPos,endPos] (inclusive).
	 * Return null if the interval is not entirely contained in the read.
	 * 
	 * @param rec
	 * @param startPos
	 * @param endPos
	 * @return
	 */
	public static Optional<String> getPattern(SAMRecord rec, int startPos, int endPos, boolean includeSoftClipped){
		int start = rec.getUnclippedStart();
		String read = rec.getReadString();
		int offsetStart = startPos-start;
		int offsetEnd = endPos+1-start;
		StringBuilder sb = new StringBuilder();
		char[] cigar = rec.getCigarString().toCharArray();
		int readPos=0;
		int refPos = start;
		if (cigar[0] == '*') return Optional.empty(); //cigar unavailable
		for (int c=0 ; c < cigar.length ; c++){
			String times = "";
			while (Character.isDigit(cigar[c])){
				times += cigar[c++];
			}
			for (int t=0 ; t < Integer.parseInt(times) ; t++){
				switch(cigar[c]){
				case 'M':
				case '=':
				case 'X':
					//Alignment match (can be a sequence match or mismatch)
					//Add the base in upper case, increase the position relative to the reference
					sb.append(read.charAt(readPos++));
					refPos++;
					break;
				case 'I':
					//Insertion to the reference
					//Add the base in lower case, increase the offset
					sb.append((""+read.charAt(readPos++)).toLowerCase());
					if (refPos < startPos) offsetStart++;
					if (refPos <= endPos+1) offsetEnd++;
					break;
				case 'D':
					//Deletion from the reference
					//add "-", increase the position relative to the reference
					sb.append("-");
					refPos++;
					break;
				case 'S':
					//Soft clipping (base present in the read)
					//don't add the base (generate useless patterns), increase the position relative to the reference and decrease the offset if we are not at the end of the read
					if (!includeSoftClipped) {
						if (c+1 < cigar.length) {
							offsetStart--;
							offsetEnd--;
						}
						readPos++;
					}else {
						sb.append(read.charAt(readPos++));
					}
					refPos++;
					break;
				case 'H':
					//Hard clipping (base NOT present in the read)
					//increase the position relative to the reference and decrease the offset if we are not at the end of the read
					if (c+1 < cigar.length) {
						offsetStart--;
						offsetEnd--;
					}
					refPos++;
					break;
				case 'N':
					//skipped region from the reference
					//For mRNA-to-genome alignment, an N operation represents an intron. For other types of alignments, the interpretation of N is not defined.
					//increase the position relative to the reference and decrease the offset
					//!!!! Never encountered, so never tested !!!!
					if (c+1 < cigar.length) return Optional.of("SHOW");
					if (refPos < startPos) offsetStart--;
					if (refPos <= endPos+1) offsetEnd--;
					if (c+1 < cigar.length) {
						offsetStart--;
						offsetEnd--;
					}
					refPos++;
					break;
				case 'P':
					//Padding (silent deletion from padded reference)
					//Nothing to do, inexistant in the ref, inexistant in the read. Only useful with multiple alignments.
					//!!!! Never encountered, so never tested !!!!
					if (c+1 < cigar.length) return Optional.of("SHOW");
					break;
				default:
					System.err.println("Unsupported CIGAR operation : " + cigar[c]);
					break;
				}
			}
		}
		String fullRead = sb.toString();
		//Check if the interval falls completely inside the read, drop it if not
		if (offsetStart >= 0 && offsetEnd <= fullRead.length()){
			String pattern = fullRead.substring(offsetStart,offsetEnd);
			return Optional.of(pattern);
		}
		return Optional.empty();
	}

	public static Optional<String> getPattern(SAMRecord rec, int startPos, int endPos){
		return getPattern(rec, startPos, endPos, false);
	}

	public Object[][] cisCheck(Interval pos){
		String NREADS="#reads";
		Map<String,Map<String,Integer>> bams = new TreeMap<String, Map<String,Integer>>();
		for (String bam : inputBams.keySet()){
			System.err.print("+");
			try {
				Map<String, Integer> patterns = new HashMap<String, Integer>();
				bams.put(bam, patterns);
				SAMRecordIterator it =  inputBams.get(bam).query(pos.getChromosome(inputBams.get(bam).getFileHeader()), pos.getStart(), pos.getEnd(), false);
				int total = 0;
				while(it.hasNext()){
					//Check if the interval falls completely inside the read, drop it if not		
					SAMRecord record = it.next();
					Optional<String> optionalPattern = ViewBam.getPattern(record, pos.getStart(), pos.getEnd());
					if (optionalPattern.isPresent()){
						String pattern = optionalPattern.get();
						int c=0;
						while (!pattern.substring(c,c+1).equals("-") && pattern.substring(c,c+1).equals(pattern.substring(c,c+1).toLowerCase())) c++;
						String pat = "" + pattern.charAt(c);
						c=pattern.length();
						while (!pattern.substring(c-1,c).equals("-") && pattern.substring(c-1,c).equals(pattern.substring(c-1,c).toLowerCase())) c--;
						pat += pattern.charAt(c-1);
						pat += (record.getReadNegativeStrandFlag()) ? "-" : "+";
						if (!patterns.containsKey(pat)){
							patterns.put(pat, 0);
						}
						patterns.put(pat, patterns.get(pat)+1);
						total++;
					}
				}
				it.close();
				patterns.put(NREADS, total);
				inputBams.get(bam).close();
			}catch(Exception ex){
				System.err.println("BamViewer: problem with bam " + bam);
				ex.printStackTrace();
			}
		}
		System.err.println();
		Map<String,Integer> totalPatterns = new HashMap<String,Integer>();
		for (Map<String,Integer> patterns : bams.values()){
			for (String pattern : patterns.keySet()){
				if (!totalPatterns.containsKey(pattern)){
					totalPatterns.put(pattern, 0);
				}
				totalPatterns.put(pattern, totalPatterns.get(pattern)+patterns.get(pattern));						
			}
		}
		List<String> headers = new ArrayList<String>();
		for (String pattern : totalPatterns.keySet()){
			int i = 0;
			while (!pattern.equals(NREADS) && i < headers.size() && totalPatterns.get(headers.get(i)) > totalPatterns.get(pattern)){
				i++;
			}
			headers.add(i, pattern);
		}				
		headers.add(0, "BAM");
		Object[][] data = new Object[bams.size()+1][headers.size()];
		data[0] = headers.toArray(new String[0]);
		int row = 1;
		for (String sample : bams.keySet()){
			int col = 0;
			for (String pattern : headers){
				if (pattern.equals("BAM")){
					data[row][col] = sample;
				}else if (bams.get(sample).containsKey(pattern)){
					data[row][col] = bams.get(sample).get(pattern);
				}else{
					data[row][col] = 0;
				}
				col++;
			}
			row++;
		}
		return data;

	}

	//TODO LONGTERM - SV not supported
	public static void main(String[] args) {
		String argConfig = null;
		String tool = "show";
		String input = null;
		String output = "bamview.xlsx";
		String interval = null;
		boolean snv = false;
		String list = null;
		String url = null;
		String samples = null;
		String genome = null;
		boolean recursive = false;
		for (int i=0 ; i < args.length ; i++){
			if (args[i].equals("--input") || args[i].equals("-I")){
				input = args[++i];
			}else if (args[i].equals("--output") || args[i].equals("-O")){
				output = args[++i];
			}else if (args[i].equals("--list") || args[i].equals("-l")){
				list = args[++i];
			}else if (args[i].equals("--url") || args[i].equals("-u")){
				url = args[++i];
			}else if (args[i].equals("--samples") || args[i].equals("-S")){
				samples = args[++i];
			}else if (args[i].equals("--reference") || args[i].equals("-R")){
				genome = args[++i];
			}else if (args[i].equals("--recursive") || args[i].equals("-r")){
				recursive = true;			
			}else if (args[i].equals("--int") || args[i].equals("-i")){
				interval = args[++i];				
			}else if (args[i].equals("--snv") || args[i].equals("-s")){
				snv = true;				
			}else if (args[i].equals("--tool") || args[i].equals("-T")){
				tool = args[++i];				
			}else if (args[i].equals("--config") || args[i].equals("-c")){
				argConfig = args[++i];
			}else if (args[i].equals("--help") || args[i].equals("-h")){
				System.out.println("Usage : you MUST give a list of interval using --list and/or --int arguments");
				System.out.println("--tool/-T [tool]: possible tools are "
						+ "'show' (default, display nucleotide pattern count on screen), "
						+ "'excel' (1 interval per sheet), "
						+ "'table' (1 row per interval, bams in columns), "
						+ "'error' (determine if position is likely a systematic error), "
						+ "'bamcheck' (1 interval per plain text file, used for Highlander BamCheck), "
						+ "'cis' (give ONE interval and check when start and end are on the same read)");
				System.out.println("--list/-l [file]: plain text file containing a chr{tab}start[{tab}end] per line ; or chr{tab}pos{tab}ref{tab}alt{tab}type for error tool (type = "+VariantType.values()+")");
				System.out.println("--int/-i: interval(s) as \"chr:start[-end];chr:start[-end];...\" ; or as \"chr:pos:ref:alt:type;...\" for error tool");
				System.out.println("--snv/-s: interval(s) of size >1 are considered as a succession of intervals of size 1 (not relevant for error tool)");
				System.out.println("--output/-O [file]: excel output filename (default is bamview.xlsx)");
				System.out.println("--input/-I: directory containing bam files or analyses for bamcheck (default is /data/highlander/bam)");
				System.out.println("--reference/-R: reference genome to use for all bam files. Reference exact name must exist in Highlander. Without this argument, GRCh37 is used by default.");
				System.out.println("--samples/-S: list of samples to check, with their analysis, as \"analysis|sample;analysis|sample;...\". All analyses must have the same reference (you don't need to add a --reference argument).");
				System.out.println("--url/-u: url pointing to a directory containing bam files");
				System.out.println("--recursive/-r: take bam files from subdirectories too");
				System.out.println("--config/-c [filename] : give config file to use as parameter");
				return;			
			}
		}
		try {
			Highlander.initialize((argConfig == null) ? new Parameters(false) : new Parameters(false, new File(argConfig)), 5);
			//BAM to check
			ViewBam vb;
			Reference reference = null;
			if (genome != null) {
				reference = new Reference(genome);
			}
			if (samples != null && (input != null || url != null)) {
				List<AnalysisFull> availableAnalyses = AnalysisFull.getAvailableAnalyses();
				Map<AnalysisFull, Set<String>> P = new TreeMap<>();
				String[] parse = samples.split(";");
				for (String ap : parse) {
					String analysisString = ap.split("\\|")[0];
					String sample =  ap.split("\\|")[1];
					AnalysisFull analysis = null;
					for (AnalysisFull a : availableAnalyses) {
						if (a.toString().equals(analysisString)) analysis = a;
					}
					if (analysis != null) {
						if (reference == null) {
							reference = analysis.getReference();
						}
						if (!P.containsKey(analysis)) P.put(analysis, new TreeSet<String>());
						P.get(analysis).add(sample);
					}
				}
				if (input != null){
					vb = new ViewBam(P, input);
				}else {
					vb = new ViewBam(P, new URL(url));
				}
			}else if (input != null){
				vb = new ViewBam(input, recursive);
			}else if (url != null){
				try{
					vb = new ViewBam(new URL(url));
				}catch(Exception ex){
					ex.printStackTrace();
					return;
				}
			}else{
				vb = new ViewBam("/data/highlander/bam", recursive);
			}
			if (reference == null) {
				reference = new Reference("GRCh37");
			}
			//positions to check
			Set<Interval> intervals = new TreeSet<Interval>();
			Set<Variant> variants = new TreeSet<Variant>();
			if (list != null){
				try{
					File file = new File(list);
					try (FileReader fr = new FileReader(file)){
						try (BufferedReader br = new BufferedReader(fr)){
							String line;
							while ((line = br.readLine()) != null){
								String[] parse = line.split("\t");		
								if (tool.equalsIgnoreCase("error")){
									variants.add(new Variant(parse[0], Integer.parseInt(parse[1]), parse[2], parse[3], VariantType.valueOf(parse[4])));
								}else{
									int end = (parse.length > 2) ? 2 : 1;
									Interval positions = new Interval(reference, parse[0],Integer.parseInt(parse[1]),Integer.parseInt(parse[end]));
									if (snv){
										for (int pos = positions.getStart() ; pos <= positions.getEnd() ; pos++){
											intervals.add(new Interval(reference, positions.getChromosome(), pos, pos));
										}
									}else{
										intervals.add(positions);							
									}						
								}
							}
						}
					}
				}catch (Exception ex){
					ex.printStackTrace();
				}
			}
			if (interval != null){
				if (tool.equalsIgnoreCase("error")){
					String[] split = interval.split(";");
					for (String variation : split){
						String[] parse = variation.split(":");
						variants.add(new Variant(parse[0], Integer.parseInt(parse[1]), parse[2], parse[3], VariantType.valueOf(parse[4])));
					}
				}else{
					String[] parse = interval.split(";");
					for (String posString : parse){
						Interval positions = new Interval(reference, posString);
						if (snv){
							for (int pos = positions.getStart() ; pos <= positions.getEnd() ; pos++){
								intervals.add(new Interval(reference, positions.getChromosome(), pos, pos));
							}
						}else{
							intervals.add(positions);							
						}
					}				
				}
			}
			if (tool.equalsIgnoreCase("show") || tool.equalsIgnoreCase("excel") || tool.equalsIgnoreCase("table") || tool.equalsIgnoreCase("bamcheck")){
				Map<Interval, Object[][]> patterns = vb.getPatterns(intervals);
				if (tool.equalsIgnoreCase("show")){
					for (Interval pos : patterns.keySet()){
						System.out.println(pos.toString());
						Object[][] data = patterns.get(pos);
						for (int i=0 ; i < data.length ; i++){
							for (int j=0 ; j < data[i].length ; j++){
								System.out.print(data[i][j]+"\t");
							}
							System.out.println();
						}
						System.out.println();
					}
				}else if (tool.equalsIgnoreCase("excel")){
					vb.toExcel(patterns, output);
				}else if (tool.equalsIgnoreCase("table")){
					Set<String> availablePatterns = new TreeSet<String>();					
					for (Object[][] data : patterns.values()){
						for (int i=1 ; i < data[0].length ; i++){
							availablePatterns.add(data[0][i].toString());
						}
					}
					availablePatterns.remove("BAM");
					availablePatterns.remove("Pathology");
					System.out.print("\t");
					for (String bam : vb.inputBams.keySet()){
						System.out.print(bam);
						for (int i=0 ; i < availablePatterns.size() ; i++){
							System.out.print("\t");
						}
					}
					System.out.println();
					System.out.print("interval" + "\t");
					for (int i=0 ; i < vb.inputBams.keySet().size() ; i++){
						for (String pattern : availablePatterns){
							System.out.print(pattern  + "\t");	
						}						
					}
					System.out.println();
					for (Interval pos : patterns.keySet()){
						System.out.print(pos + "\t");
						Object[][] data = patterns.get(pos);
						for (int i=1 ; i < data.length ; i++){
							for (String pattern : availablePatterns){
								int col = 1;
								while (col < data[i].length && !data[0][col].equals(pattern)){
									col++;
								}
								if (col >= data[i].length){
									System.out.print(0  + "\t");	
								}else{
									System.out.print(data[i][col]  + "\t");									
								}
							}
						}
						System.out.println();
					}
				}else if (tool.equalsIgnoreCase("bamcheck")){
					vb.toBamCheck(patterns, output);
				}
			}else if (tool.equalsIgnoreCase("error")){
				Set<Variant> validated = vb.checkCandidatesForSpecErrors(new TreeSet<Variant>(variants));
				for (Variant candidate : variants){
					if (validated.contains(candidate))
						System.out.println(candidate + "\tERROR validated");
					else
						System.out.println(candidate + "\terror NOT validated");
				}				
			}else if (tool.equalsIgnoreCase("cis")){
				Object[][] data = vb.cisCheck(intervals.iterator().next());
				for (int i=0 ; i < data.length ; i++){
					for (int j=0 ; j < data[i].length ; j++){
						System.out.print(data[i][j]+"\t");
					}
					System.out.println();
				}
				System.out.println();
			}else{
				System.out.println("Unknown tool : " + tool);
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
