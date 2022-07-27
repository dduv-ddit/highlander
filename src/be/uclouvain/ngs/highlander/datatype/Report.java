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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
 * Software included in the analysis pipeline can generate files needed by Highlander.
 * Those files can be used for parsing and displaying (e.g. FastQC report), or just to be downloadable by the user.
 * Reports available can be defined by administrators in the Administration tools (Database management, reports).
 * The FileDownloader (UI.tools) in Highlander allows users to download all files linked to defined reports.
 * 
 * Map the 'reports' table in the database
 * 
 * @author Raphaël Helaers
 *
 */
public class Report implements Comparable<Report> {
	
	private String software;
	private String description = "";
	private String path = "";
	private Set<Analysis> analyses = new TreeSet<>();
	private final Map<String, String> files = new TreeMap<>();

	public Report(){
	}

	public Report(String software) throws Exception {
		if (software == null) throw new Exception("Software is null");
		this.software = software;
		fetchInfo();
	}
			
	public Report(Results res) throws Exception {
		setFields(res);
	}
	
	public void fetchInfo() throws Exception {
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM `reports` WHERE software = '"+software+"'")) {
			setFields(res);
		}
	}

	private void setFields(Results res) throws Exception {
		while (res.next()) {
			software = res.getString("software");
			description = res.getString("software_description");
			if (description == null) description = "";
			path = res.getString("path");
			if (path == null) path = "";
			String anal = res.getString("analysis");
			if (anal != null && anal.length() > 0) {
				analyses.add(new Analysis(anal));
			}
			String file = res.getString("file_extension");
			if (file == null) file = "";
			String fileDescription = res.getString("file_description");
			if (fileDescription == null) fileDescription = "";
			files.put(file, fileDescription);
		}
	}

	public void update() throws Exception {
		insert();
	}

	public void insert() throws Exception {
		delete();
		if (analyses.isEmpty()) {
			if (files.isEmpty()) {
				insert(null, "");
			}else {
				for (String file : getFiles()) {
					insert(null, file);
				}
			}
		}else {
			for (Analysis analysis : analyses) {
				if (files.isEmpty()) {
					insert(analysis, "");
				}else {
					for (String file : getFiles()) {
						insert(analysis, file);
					}
				}
			}
		}
	}

	private void insert(Analysis analysis, String file) throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "INSERT INTO `reports` (software, software_description, path, analysis, file_extension, file_description) " +
				"VALUES (" +
				"'"+Highlander.getDB().format(Schema.HIGHLANDER, getSoftware())+"', " +
				"'"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"', " +
				"'"+Highlander.getDB().format(Schema.HIGHLANDER, getPath())+"', " +
				"'"+((analysis == null)?"":analysis.toString())+"', " +
				"'"+Highlander.getDB().format(Schema.HIGHLANDER, file)+"', " +
				"'"+Highlander.getDB().format(Schema.HIGHLANDER, getFileDescription(file))+"')");		
	}
	
	public void delete() throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM `reports` WHERE software = '"+Highlander.getDB().format(Schema.HIGHLANDER, getSoftware())+"'");
	}
	
	public String getSoftware() {
		return software;
	}

	public void setSoftware(String software) {
		this.software = software;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Set<Analysis> getAnalyses() {
		return analyses;
	}

	public void addAnalysis(Analysis analysis) {
		analyses.add(analysis);
	}
	
	public void removeAnalysis(Analysis analysis) {
		analyses.remove(analysis);
	}
	
	public Set<String> getFiles() {
		return new TreeSet<>(files.keySet());
	}

	public String getFileDescription(String file) {
		if (files.containsKey(file)) {
			if (files.get(file) != null) {
				return files.get(file);
			}
		}
		return "";
	}
	
	public void addFile(String file, String description) {
		files.put(file, description);
	}
	
	public void removeFile(String file) {
		files.remove(file);
	}
	
	public void setFileDescription(String file, String description) {
		if (files.containsKey(file)) {
			files.put(file, description);
		}
	}
	
	public URL getUrlForFile(String file, String project, String sample) throws MalformedURLException {
		String url = Highlander.getParameters().getUrlForReports()+"/"+project+"/"+path+"/"+sample+"/";
		return new URL(url+sample+file);
	}
	
	@Override
	public String toString(){
		return software;
	}

	@Override
	public int hashCode() {
		return software.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return software.equals(obj.toString());
	}

	@Override
	public int compareTo (Report r) {
		return software.compareTo(r.software);
	}
	
	public static Set<Report> getAvailableReports() throws Exception {
		Set<Report> reports = new TreeSet<Report>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT DISTINCT(software) FROM `reports`")) {
			while (res.next()){
				reports.add(new Report(res.getString(1)));
			}
		}
		return reports;
	}

}
