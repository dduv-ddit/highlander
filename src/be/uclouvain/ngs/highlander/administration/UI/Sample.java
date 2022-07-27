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

package be.uclouvain.ngs.highlander.administration.UI;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.ChannelExec;

import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.SampleType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public class Sample {

	static private HighlanderDatabase DB = ProjectManager.getDB();
	
	String platform;
	String sequencing_target;
	String outsourcing;
	String family;
	String individual;
	String sample;
	boolean index_case = false;
	String pathology;
	int pathology_id;
	String population;
	int population_id = -1;
	SampleType sample_type;
	int barcode;
	String kit;
	String read_length;
	boolean pair_end = true;
	boolean trim = false;
	boolean remove_duplicates = true;
	int normal_id = -1;
	String normal_sample;
	int run_id;
	String run_date;
	String run_name;
	String run_label;
	String comments;

	String[] users;
	String[] analyses;

	public Sample(AdministrationTableModel model, int row){
		platform = (model.getValueAt(row, model.getColumn("platform")) == null) ? "" : model.getValueAt(row, model.getColumn("platform")).toString().trim();
		sequencing_target = (model.getValueAt(row, model.getColumn("sequencing_target")) == null) ? "" : model.getValueAt(row, model.getColumn("sequencing_target")).toString().trim();
		outsourcing = (model.getValueAt(row, model.getColumn("outsourcing")) == null) ? "" : model.getValueAt(row, model.getColumn("outsourcing")).toString().trim();
		family = model.getValueAt(row, model.getColumn("family")).toString().trim();
		individual = model.getValueAt(row, model.getColumn("individual")).toString().trim();
		sample = model.getValueAt(row, model.getColumn("sample")).toString().trim();
		try{
			index_case = Boolean.parseBoolean(model.getValueAt(row, model.getColumn("index_case")).toString().trim());
		}catch(Exception ex){
			index_case = false;
		}
		pathology = model.getValueAt(row, model.getColumn("pathology")).toString().trim();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT pathology_id FROM pathologies WHERE pathology = '"+pathology+"'")){
			if (res.next()) {
				pathology_id = res.getInt("pathology_id");
			}else {
				pathology_id = -1;
			}
		}catch(Exception ex){
			pathology_id = -1;
		}
		if (model.getValueAt(row, model.getColumn("population")) == null) {
			population = "";		
			population_id = -1;
		}else {
			population = model.getValueAt(row, model.getColumn("population")).toString();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT population_id FROM populations WHERE population = '"+population+"'")){
				if (res.next()) {
					population_id = res.getInt("population_id");
				}else {
					population_id = -1;
				}
			}catch(Exception ex){
				population_id = -1;
			}
		}
		sample_type = SampleType.valueOf(model.getValueAt(row, model.getColumn("sample_type")).toString().trim());
		try{
			barcode = Integer.parseInt(model.getValueAt(row, model.getColumn("barcode")).toString().trim());
		}catch(Exception ex){
			barcode = 0;
		}
		kit = (model.getValueAt(row, model.getColumn("kit")) != null) ? model.getValueAt(row, model.getColumn("kit")).toString().trim() : null;
		read_length = (model.getValueAt(row, model.getColumn("read_length")) != null) ? model.getValueAt(row, model.getColumn("read_length")).toString().trim() : null;
		try{
			pair_end = Boolean.parseBoolean(model.getValueAt(row, model.getColumn("pair_end")).toString().trim());
		}catch(Exception ex){
			pair_end = true;
		}
		try{
			trim = Boolean.parseBoolean(model.getValueAt(row, model.getColumn("trim")).toString().trim());
		}catch(Exception ex){
			trim = false;
		}
		try{
			remove_duplicates = Boolean.parseBoolean(model.getValueAt(row, model.getColumn("remove_duplicates")).toString().trim());
		}catch(Exception ex){
			remove_duplicates = true;
		}
		try{
			normal_id = (model.getValueAt(row, model.getColumn("normal_id")) != null) ? Integer.parseInt(model.getValueAt(row, model.getColumn("normal_id")).toString().trim()) : -1;
		}catch(Exception ex){
			normal_id = -1;
		}
		normal_sample = (model.getValueAt(row, model.getColumn("normal_sample")) != null) ? model.getValueAt(row, model.getColumn("normal_sample")).toString().trim() : null;
		run_id = Integer.parseInt(model.getValueAt(row, model.getColumn("run_id")).toString().trim());
		run_date = model.getValueAt(row, model.getColumn("run_date")).toString().trim();
		run_name = model.getValueAt(row, model.getColumn("run_name")).toString().trim();
		run_label = run_id + "_" + run_date.replace("-", "_") + "_" + run_name;
		users = (model.getValueAt(row, model.getColumn("users")) == null) ? users = new String[0] : model.getValueAt(row, model.getColumn("users")).toString().split(",");
		analyses = (model.getValueAt(row, model.getColumn("analyses")) == null) ? analyses = new String[0] : model.getValueAt(row, model.getColumn("analyses")).toString().split(",");
		comments = (model.getValueAt(row, model.getColumn("comments")) == null) ? "" : model.getValueAt(row, model.getColumn("comments")).toString();
	}

	public Sample(int dbId) throws Exception {
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT p.*, p2.sample as normal_sample, pathologies.pathology, populations.population "
				+ "FROM projects as p "
				+ "LEFT JOIN pathologies USING (pathology_id) "
				+ "LEFT JOIN populations USING (population_id) "
				+ "LEFT JOIN projects as p2 ON p.normal_id = p2.project_id "
				+ "WHERE p.project_id = " + dbId)) {
			if (res.next()){
				platform = res.getString("platform");
				sequencing_target = res.getString("sequencing_target");
				outsourcing = res.getString("outsourcing");
				family = res.getString("family");
				individual = res.getString("individual");
				sample = res.getString("sample");
				pathology_id = res.getInt("pathology_id");
				pathology = res.getString("pathology");
				if (res.getObject("population_id") != null) {
					population_id = res.getInt("population_id");
					population = res.getString("population");
				}
				kit = res.getString("kit");
				run_id = res.getInt("run_id");
				run_date = res.getString("run_date");
				run_name = res.getString("run_name");
				run_label = res.getString("run_label");
				comments = res.getString("comments");
				barcode = res.getInt("barcode");
				read_length = res.getString("read_length");
				if (res.getObject("sample_type") != null) sample_type = SampleType.valueOf(res.getString("sample_type"));
				if (res.getObject("index_case") != null) index_case = res.getBoolean("index_case");
				if (res.getObject("pair_end") != null) pair_end = res.getBoolean("pair_end");
				if (res.getObject("trim") != null) trim = res.getBoolean("trim");
				if (res.getObject("remove_duplicates") != null) remove_duplicates = res.getBoolean("remove_duplicates");
				if (res.getObject("normal_id") != null) normal_id = res.getInt("normal_id");
				if (res.getObject("normal_sample") != null) normal_sample = res.getString("normal_sample");
			}else{
				throw new Exception("Id "+dbId+" not found in the database");
			}
		}
		List<String> listAnalyses = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM projects_analyses WHERE project_id = " + dbId)) {
			while (res.next()){
				listAnalyses.add(res.getString("analysis"));
			}
		}
		analyses = listAnalyses.toArray(new String[0]);
		List<String> listUsers = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM projects_users WHERE project_id = " + dbId)) {
			while (res.next()){
				listUsers.add(res.getString("username"));
			}
		}
		users = listUsers.toArray(new String[0]);
	}

	public String toString(){
		return sample;
	}

	public String getProject(){
		return run_label;
	}

	public int insertInDb() throws Exception {
		int id = DB.insertAndGetAutoId(Schema.HIGHLANDER, "INSERT INTO `projects` SET" +
				" `platform` = '"+platform.toString()+"'," +
				" `sequencing_target` = '"+DB.format(Schema.HIGHLANDER, sequencing_target)+"'," +
				" `outsourcing` = '"+DB.format(Schema.HIGHLANDER, outsourcing)+"'," +
				" `family` = '"+DB.format(Schema.HIGHLANDER, family)+"'," +
				" `individual` = '"+DB.format(Schema.HIGHLANDER, individual)+"'," +
				" `sample` = '"+DB.format(Schema.HIGHLANDER, sample)+"'," +
				" `index_case` = "+index_case+"," +
				" `pathology_id` = "+pathology_id+"," +
				((population_id != -1)?" `population_id` = '"+population_id+"',":"") +
				" `sample_type` = '"+sample_type+"'," +
				" `barcode` = '"+barcode+"'," +
				((kit != null)?" `kit` = '"+DB.format(Schema.HIGHLANDER, kit.toString())+"',":"") +
				((read_length != null)?" `read_length` = '"+DB.format(Schema.HIGHLANDER, read_length.toString())+"',":"") +
				" `pair_end` = "+pair_end+"," +
				" `trim` = "+trim+"," +
				" `remove_duplicates` = "+remove_duplicates+"," +
				((normal_id != -1)?" `normal_id` = '"+normal_id+"',":"") +
				" `run_id` = '"+run_id+"'," +
				" `run_date` = '"+run_date+"'," +
				" `run_name` = '"+DB.format(Schema.HIGHLANDER, run_name)+"'," +
				" `run_label` = '"+DB.format(Schema.HIGHLANDER, run_label)+"'," +
				" `comments` = '"+DB.format(Schema.HIGHLANDER, comments)+"'");
		insertInDbUsers(id);
		return id;
	}

	public void insertInDbUsers(int id) throws Exception {
		for (String user : users){
			DB.insert(Schema.HIGHLANDER, "INSERT INTO `projects_users` SET `project_id` = "+id+", `username` = '"+user+"'");
		}
	}

	public void updateInDb(ProjectManager manager, int id) throws Exception {
		ProjectManager.toConsole("-----------------------------------------------------");
		Sample old = new Sample(id);
		ProjectManager.toConsole("Modify data in projects table");
		DB.update(Schema.HIGHLANDER, "UPDATE `projects` SET" +
				" `platform` = '"+platform.toString()+"'," +
				" `sequencing_target` = '"+DB.format(Schema.HIGHLANDER, sequencing_target)+"'," +
				" `outsourcing` = '"+DB.format(Schema.HIGHLANDER, outsourcing)+"'," +
				" `family` = '"+DB.format(Schema.HIGHLANDER, family)+"'," +
				" `individual` = '"+DB.format(Schema.HIGHLANDER, individual)+"'," +
				" `sample` = '"+DB.format(Schema.HIGHLANDER, sample)+"'," +
				" `index_case` = "+index_case+"," +
				" `pathology_id` = "+pathology_id+"," +
				((population_id != -1)?" `population_id` = "+population_id+",":"") +
				" `sample_type` = '"+sample_type+"'," +
				" `barcode` = '"+barcode+"'," +
				((kit != null)?" `kit` = '"+DB.format(Schema.HIGHLANDER, kit.toString())+"',":"") +
				((read_length != null)?" `read_length` = '"+DB.format(Schema.HIGHLANDER, read_length.toString())+"',":"") +
				" `pair_end` = "+pair_end+"," +
				" `trim` = "+trim+"," +
				" `remove_duplicates` = "+remove_duplicates+"," +
				((normal_id != -1)?" `normal_id` = "+normal_id+",":"") +
				" `run_id` = '"+run_id+"'," +
				" `run_date` = '"+run_date+"'," +
				" `run_name` = '"+DB.format(Schema.HIGHLANDER, run_name)+"'," +
				" `run_label` = '"+DB.format(Schema.HIGHLANDER, run_label)+"'," +
				" `comments` = '"+DB.format(Schema.HIGHLANDER, comments)+"'" +
				" WHERE project_id = " + id);
		ProjectManager.toConsole("Changing projects_users table");
		DB.update(Schema.HIGHLANDER, "DELETE FROM projects_users WHERE project_id = " + id);
		insertInDbUsers(id);
		for (String analysis : analyses) {
			DB.update(Schema.HIGHLANDER, "UPDATE `"+analysis+"_possible_values` SET `value` = '"+DB.format(Schema.HIGHLANDER, sample)+"' WHERE `field` = 'sample' AND `value` = '"+old.sample+"'");
		}
		if (!sample.equals(old.sample)){
			renameOnServer(manager, id, old.sample, analyses);
		}
	}

	public static void renameOnServer(ProjectManager manager, int projectId, String oldName, String[] analyses) {
		ProjectManager.toConsole("Updating file names on the server");
		for (String analysis : analyses){
			try{
				String run_path = "";
				String newName = "";
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sample, run_path FROM `projects` JOIN projects_analyses USING (project_id) WHERE `analysis` = '"+analysis+"' AND project_id = "+projectId)) {
					if (res.next()){
						newName = res.getString("sample");
						run_path = res.getString("run_path");
					}
				}
				if (run_path != null && run_path.length() > 0){
					//Rename files on server
					manager.connectToHighlander();
					int pos = run_path.lastIndexOf('/');
					String commandLine = "rename_sample.sh "+ run_path.substring(0, pos) + " " + oldName + " " + newName + " " + analysis;
					ProjectManager.toConsole(commandLine);
					ChannelExec channelExec = (ChannelExec)manager.getHighlanderSftpSession().openChannel("exec");
					commandLine = ProjectManager.getParameters().getServerPipelineScriptsPath()+"/"+commandLine;
					channelExec.setCommand(commandLine);
					channelExec.connect();
					channelExec.setInputStream(null);
					channelExec.setOutputStream(System.out);
					channelExec.setErrStream(System.err);
					InputStream in=channelExec.getInputStream();
					channelExec.connect();
					byte[] tmp=new byte[1024];
					while(true){
						while(in.available()>0){
							int i=in.read(tmp, 0, 1024);
							if(i<0)break;
							ProjectManager.toConsole(new String(tmp, 0, i));
						}
						if(channelExec.isClosed()){
							if(in.available()>0) continue;
							ProjectManager.toConsole("exit-status: "+channelExec.getExitStatus());
							break;
						}
						try{Thread.sleep(1000);}catch(Exception ee){}
					}
					channelExec.disconnect();
					manager.disconnectFromHighlander();
					//Rename path in database
					DB.update(Schema.HIGHLANDER, "UPDATE projects_analyses SET run_path = '"+run_path.replace(oldName, newName)+"' WHERE `analysis` = '"+analysis+"' AND project_id = "+projectId);
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
	}
	
	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getOutsourcing() {
		return outsourcing;
	}

	public void setOutsourcing(String outsourcing) {
		this.outsourcing = outsourcing;
	}

	public String getFamily() {
		return family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	public String getIndividual() {
		return individual;
	}
	
	public void setIndividual(String individual) {
		this.individual = individual;
	}
	
	public String getSample() {
		return sample;
	}
	
	public void setSample(String sample) {
		this.sample = sample;
	}
	
	public boolean isIndex_case() {
		return index_case;
	}

	public void setIndex_case(boolean index_case) {
		this.index_case = index_case;
	}

	public String getPathology() {
		return pathology;
	}

	public void setPathology(String pathology) {
		this.pathology = pathology;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT pathology_id FROM pathologies WHERE pathology = '"+pathology+"'")){
			if (res.next()) {
				pathology_id = res.getInt("pathology_id");
			}else {
				pathology_id = -1;
			}
		}catch(Exception ex){
			pathology_id = -1;
		}
	}

	public int getPathology_id() {
		return pathology_id;
	}
	
	public void setPathology_id(int pathology_id) {
		this.pathology_id = pathology_id;
	}
	
	public String getPopulation() {
		return population;
	}
	
	public void setPopulation(String population) {
		this.population = population;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT population_id FROM populations WHERE population = '"+population+"'")){
			if (res.next()) {
				population_id = res.getInt("population_id");
			}else {
				population_id = -1;
			}
		}catch(Exception ex){
			population_id = -1;
		}
	}
	
	public int getPopulation_id() {
		return population_id;
	}
	
	public void setPopulation_id(int population_id) {
		this.population_id = population_id;
	}
	
	public SampleType getSample_type() {
		return sample_type;
	}

	public void setSample_type(SampleType sample_type) {
		this.sample_type = sample_type;
	}

	public int getBarcode() {
		return barcode;
	}

	public void setBarcode(int barcode) {
		this.barcode = barcode;
	}

	public String getKit() {
		return kit;
	}

	public void setKit(String kit) {
		this.kit = kit;
	}

	public String getRead_length() {
		return read_length;
	}

	public void setRead_length(String read_length) {
		this.read_length = read_length;
	}

	public boolean isPair_end() {
		return pair_end;
	}
	
	public void setPair_end(boolean pair_end) {
		this.pair_end = pair_end;
	}
	
	public int getRun_id() {
		return run_id;
	}

	public void setRun_id(int run_id) {
		this.run_id = run_id;
	}

	public String getRun_date() {
		return run_date;
	}

	public void setRun_date(String run_date) {
		this.run_date = run_date;
	}

	public String getRun_name() {
		return run_name;
	}

	public void setRun_name(String run_name) {
		this.run_name = run_name;
	}

	public String getRun_label() {
		return run_label;
	}
	
	public void setRun_label(String run_label) {
		this.run_label = run_label;
	}
	
	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String[] getUsers() {
		return users;
	}

	public void setUsers(String[] users) {
		this.users = users;
	}

	public String[] getAnalyses() {
		return analyses;
	}

	public void setAnalyses(String[] analyses) {
		this.analyses = analyses;
	}

	public String getSequencing_target() {
		return sequencing_target;
	}

	public void setSequencing_target(String sequencing_target) {
		this.sequencing_target = sequencing_target;
	}

	public boolean isTrim() {
		return trim;
	}

	public void setTrim(boolean trim) {
		this.trim = trim;
	}

	public boolean isRemove_duplicates() {
		return remove_duplicates;
	}

	public void setRemove_duplicates(boolean remove_duplicates) {
		this.remove_duplicates = remove_duplicates;
	}

	public int getNormal_id() {
		return normal_id;
	}

	public String getNormal_sample() {
		return normal_sample;
	}
	
	public void setNormal(int normal_id, String normal_sample) {
		this.normal_id = normal_id;
		this.normal_sample = normal_sample;
	}	
	
	
}
