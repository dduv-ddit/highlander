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

package be.uclouvain.ngs.highlander.administration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AlamutParser {

	FileReader fr;
	BufferedReader br;
	String[] headers;
	String[] lastLine;
	List<String[]> currentVariant = new ArrayList<String[]>();

	public AlamutParser(File alamutFile) throws Exception {
		fr = new FileReader(alamutFile);
		br = new BufferedReader(fr);
		headers = br.readLine().split("\t");
		lastLine = br.readLine().split("\t");
		nextVariant();
	}

	public boolean hasNext() {
		return (lastLine != null);
	}

	public void nextVariant() throws Exception {
		currentVariant.clear();
		if (lastLine == null) throw new Exception("End of Alamut file reached");
		String position = extract("inputPos",lastLine);
		do{
			currentVariant.add(lastLine);
			String line = br.readLine();
			if (line != null) {
				lastLine = line.split("\t");
			}else{
				lastLine = null;
			}
		}while(lastLine != null && position.equals(extract("inputPos",lastLine)));
		//In some alamut file unannotated variants have a line with an inputPos different from POS in VCF
		//As it cause problem (line skipping for whole file), we skip this line in the alamut file
		for (Iterator<String[]> it = currentVariant.iterator() ; it.hasNext() ; ){
			String[] line = it.next();
			String unannotatedReason = extract("unannotatedReason", line);
			if (unannotatedReason != null & !unannotatedReason.equalsIgnoreCase("-")){
				it.remove();
			}
		}
		if (currentVariant.isEmpty() && hasNext()){
			nextVariant();
		}
	}

	public boolean checkVariantPos(String pos) throws Exception {
		if (currentVariant.isEmpty()) return false;
		return extract("inputPos",currentVariant.get(0)).equals(pos);
	}

	public void comparePos(String pos) throws Exception {
		for (String[] v : currentVariant){
			System.err.println("VCF pos = "+pos+" - Alamut pos = "+extract("inputPos",v));
		}
	}

	public void setGene(String geneSymbol) throws Exception {
		for (Iterator<String[]> it = currentVariant.iterator() ; it.hasNext() ; ){
			String[] line = it.next();
			if (!geneSymbol.equalsIgnoreCase(extract("gene", line))){
				it.remove();
			}
		}
	}

	public int getNumAlt() throws Exception {
		return currentVariant.size();
	}

	public String extract(String header, int alt) throws Exception {
		if (currentVariant.size() <= alt) return null;
		return extract(header, currentVariant.get(alt));
	}

	private String extract(String header, String[] line) {
		int i=0;
		while(i < headers.length && !headers[i].equalsIgnoreCase(header)) i++;
		if (i >= headers.length){
			//System.err.println("Header '" +header+"' was not found in the Alamut file");
			return null;
		}
		return line[i];
	}
}
