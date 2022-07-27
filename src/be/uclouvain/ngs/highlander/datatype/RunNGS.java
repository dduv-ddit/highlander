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

import java.sql.Date;

import java.util.Set;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.SampleType;

public class RunNGS implements Comparable<RunNGS> {

	private String sequencing_target;
	private String platform;
	private String outsourcing;
	private Set<String> pathologies = new TreeSet<String>();
	private Set<SampleType> sampleTypes = new TreeSet<SampleType>();
	private Set<String> kits = new TreeSet<String>();
	private String read_length;
	private boolean pair_end;
	private int run_id;
	private Date run_date;
	private String run_name;
	private String run_label;

	public RunNGS(){}

	public RunNGS(Results res) throws Exception {
		sequencing_target = res.getString("sequencing_target");
		platform = res.getString("platform");
		outsourcing = res.getString("outsourcing");
		pathologies.add(res.getString("pathology"));
		sampleTypes.add(SampleType.valueOf(res.getString("sample_type")));
		if (res.getString("kit") != null && res.getString("kit").length() > 0) kits.add(res.getString("kit"));
		read_length = res.getString("read_length");
		pair_end = res.getBoolean("pair_end");
		run_id = res.getInt("run_id");
		run_date = res.getDate("run_date");
		run_name = res.getString("run_name");
		run_label = res.getString("run_label");
	}

	public String getSequencingTarget() {
		return sequencing_target;
	}

	public void setSequencingTarget(String sequencing_target) {
		this.sequencing_target = sequencing_target;
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

	public Set<String> getPathologies() {
		return pathologies;
	}

	public void addPathology(String pathology) {
		this.pathologies.add(pathology);
	}

	public Set<SampleType> getSampleTypes() {
		return sampleTypes;
	}

	public void addSampleType(SampleType sampleType) {
		this.sampleTypes.add(sampleType);
	}

	public Set<String> getKits() {
		return kits;
	}

	public void addKit(String kit) {
		this.kits.add(kit);
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

	public Date getRun_date() {
		return run_date;
	}

	public void setRun_date(Date run_date) {
		this.run_date = run_date;
	}

	public String getRun_name() {
		return run_name;
	}

	public void setRun_name(String run_name) {
		this.run_name = run_name;
	}

	public void setRunLabel(String run_label) {
		this.run_label = run_label;
	}
	
	public String getRunLabel(){
		return run_label;
	}

	@Override
	public boolean equals(Object r){
		if (r instanceof RunNGS){
			return getRunLabel().equals(((RunNGS)r).getRunLabel());
		}
		return false;
	}

	@Override
	public int compareTo(RunNGS r){
		if (getRunLabel().equals(r.getRunLabel())) return 0;
		if (run_id == r.run_id){
			if (run_date.equals(r.run_date)){
				return run_name.compareTo(r.run_name);
			}else{
				return run_date.compareTo(r.run_date);
			}
		}else{
			return run_id - r.run_id;
		}
	}

	@Override
	public String toString(){
		return getRunLabel();
	}
}
