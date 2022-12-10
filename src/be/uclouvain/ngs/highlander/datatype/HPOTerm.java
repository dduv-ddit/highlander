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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public class HPOTerm implements Comparable<HPOTerm> {

	private final Reference reference; 
	private int id = -1;
	private String ontology_id = null;
	private String comment;
	private String name;
	private String definition;
	
	private Set<Integer> parents_ids = new TreeSet<Integer>();
	private Set<Integer> children_ids = new TreeSet<Integer>();
	private Set<String> synonyms = new TreeSet<String>();
	private Map<String,Set<String>> gene_diseases = new TreeMap<>();
	private Map<String,Set<String>> disease_genes = new TreeMap<>();
	
	public HPOTerm(int dbId, Reference reference) throws Exception {
		this.reference = reference;
		id = dbId;
		setFields();
	}

	public HPOTerm(String ontologyId, Reference reference) throws Exception {
		this.reference = reference;
		ontology_id = ontologyId;
		setFields();
	}
	
	private void setFields() throws Exception {
		if (id >= 0) {
			try (Results res = Highlander.getDB().select(reference, Schema.HPO, "SELECT * FROM `db_term` WHERE id = "+id)) {
				if (res.next()) {
					setOntologyId(res.getString("ontology_id"));
					setComment(res.getString("comment"));
					setName(res.getString("name"));
					setDefinition(res.getString("definition"));
				}
			}
		}else if (ontology_id != null){
			try (Results res = Highlander.getDB().select(reference, Schema.HPO, "SELECT * FROM `db_term` WHERE ontology_id = '"+ontology_id+"'")) {
				if (res.next()) {
					setId(res.getInt("id"));
					setComment(res.getString("comment"));
					setName(res.getString("name"));
					setDefinition(res.getString("definition"));
				}
			}			
		}else {
			throw new Exception("HPO term can only be fectch using database id or ontology_id, both are missing");
		}
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, "SELECT `term_parent_id` FROM `db_term_relationship` WHERE term_child_id = "+id)) {
			while (res.next()) {
				addParentId(res.getInt("term_parent_id"));
			}
		}
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, "SELECT `term_child_id` FROM `db_term_relationship` WHERE term_parent_id = "+id)) {
			while (res.next()) {
				addChildrenId(res.getInt("term_child_id"));
			}
		}
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, "SELECT `synonym` FROM `db_term_synonym` WHERE db_term_id = "+id)) {
			while (res.next()) {
				addSynonyms(res.getString("synonym"));
			}
		}
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, "SELECT gene_symbol, disease_name, disease_id "
				+ "FROM db_term_db_genes "
				+ "JOIN db_gene ON db_term_db_genes.db_gene_id = db_gene.id "
				+ "JOIN db_gene_db_diseases USING(db_gene_id) "
				+ "JOIN db_disease ON db_gene_db_diseases.db_disease_id = db_disease.id "
				+ "WHERE db_term_id = "+id)) {
			while (res.next()) {
				addAssociatedGeneAndDisease(res.getString("gene_symbol"), res.getString("disease_name") + " ["+res.getString("disease_id")+"]");
			}
		}
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setOntologyId(String ontology_id) {
		this.ontology_id = ontology_id;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public void addParentId(int dbId) {
		parents_ids.add(dbId);
	}

	public void addChildrenId(int dbId) {
		children_ids.add(dbId);
	}
	
	public void addSynonyms(String synonym) {
		synonyms.add(synonym);
	}

	public void addAssociatedGeneAndDisease(String gene, String disease) {
		if (!gene_diseases.containsKey(gene)) {
			gene_diseases.put(gene, new TreeSet<String>());
		}
		gene_diseases.get(gene).add(disease);
		if (!disease_genes.containsKey(disease)) {
			disease_genes.put(disease, new TreeSet<String>());
		}
		disease_genes.get(disease).add(gene);
	}
	
	public int getId() {
		return id;
	}

	public String getOntologyId() {
		return ontology_id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getComment() {
		return comment;
	}

	public String getDefinition() {
		return definition;
	}

	public int getParentsCount() {
		return parents_ids.size();
	}
	
	public Set<HPOTerm> getParents() throws Exception {
		Set<HPOTerm> parents = new TreeSet<HPOTerm>();
		for (int id : parents_ids) {
			parents.add(new HPOTerm(id, reference));
		}
		return parents;
	}

	public int getChildrenCount() {
		return children_ids.size();
	}
	
	public Set<HPOTerm> getChildren() throws Exception {
		Set<HPOTerm> children = new TreeSet<HPOTerm>();
		for (int id : children_ids) {
			children.add(new HPOTerm(id, reference));
		}
		return children;
	}
	
	public Set<String> getSynonyms() {
		return synonyms;
	}

	public Set<String> getAssociatedGenes() {
		return new TreeSet<>(gene_diseases.keySet());
	}

	public Set<String> getAssociatedDiseases() {
		return new TreeSet<>(disease_genes.keySet());
	}
	
	public Set<String> getAssociatedGenes(String disease) {
		if (disease_genes.containsKey(disease)) {
			return new TreeSet<>(disease_genes.get(disease));
		}else {
			return new TreeSet<>();
		}
	}
	
	public Set<String> getAssociatedDiseases(String gene) {
		if (gene_diseases.containsKey(gene)) {
			return new TreeSet<>(gene_diseases.get(gene));
		}else {
			return new TreeSet<>();
		}
	}
	
	@Override
	public String toString(){
		return "[" + ontology_id + "]" + " " + name;
	}

	@Override
	public int hashCode() {
		return ontology_id.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HPOTerm))
			return false;
		return compareTo((HPOTerm)obj) == 0;
	}

	@Override
	public int compareTo (HPOTerm r) {
		return ontology_id.compareTo(r.ontology_id);
	}
	
	public String getHTMLDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>"
				+ "<p style=\"font-family:Calibri;font-size:20px\"><b>"+name+"</b></p>"
				+ "<p style=\"font-family:Calibri;font-size:15px\">"+ontology_id+"</p>"
				+ "<p style=\"font-family:Calibri;font-size:13px\">"+ definition+"</p>");
		if (synonyms.size() > 0) {
			sb.append("<p style=\"font-family:Calibri;font-size:13px\"><b>Synonyms</b>: <i>");
			for (Iterator<String> it = synonyms.iterator() ; it.hasNext() ; ) {
				sb.append(it.next());
				if (it.hasNext()) sb.append(", ");
			}
			sb.append("</i></p>");
		}
		if (comment.length() > 0) {
			sb.append("<p style=\"font-family:Calibri;font-size:13px\"><b>Comment</b>: " + comment + "</p>");
		}
		sb.append("</html>");
		return sb.toString();
	}
	
	public static Map<String, Integer> searchNames(String query, Reference reference){
		Map<String, Integer> results = new TreeMap<>();
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, 
				"SELECT db_term.id, ontology_id, name "
				+ "FROM db_term "
				+ "WHERE INSTR(name,'"+query+"')")) {
			while (res.next()) {
				results.put("["+res.getString("ontology_id")+"] "+res.getString("name"), res.getInt("db_term.id"));
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}
	
	public static Map<String, Integer> searchSynonyms(String query, Reference reference){
		Map<String, Integer> results = new TreeMap<>();
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, 
				"SELECT db_term.id, ontology_id, synonym "
				+ "FROM db_term "
				+ "JOIN db_term_synonym ON db_term.id = db_term_synonym.db_term_id "
				+ "WHERE INSTR(synonym,'"+query+"')")) {
			while (res.next()) {
				results.put("["+res.getString("ontology_id")+"] "+res.getString("synonym"), res.getInt("db_term.id"));
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}
	
	public static Map<String, Integer> searchNamesAndSynonyms(String query, Reference reference){
		Map<String, Integer> results = new TreeMap<>();
		results.putAll(searchNames(query, reference));
		results.putAll(searchSynonyms(query, reference));
		return results;
	}
	
	public static Map<String, Map<String,Integer>> searchDiseases(String query, Reference reference){
		Map<String, Map<String,Integer>> results = new TreeMap<>();
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, 
				"SELECT disease_name, disease_id, db_term_id, ontology_id, name "
				+ "FROM db_term_db_genes "
				+ "JOIN db_gene ON db_term_db_genes.db_gene_id = db_gene.id "
				+ "JOIN db_gene_db_diseases USING(db_gene_id) "
				+ "JOIN db_disease ON db_gene_db_diseases.db_disease_id = db_disease.id "
				+ "JOIN db_term ON db_term.id = db_term_db_genes.db_term_id "
				+ "WHERE INSTR(disease_name,'"+query+"')")) {
			while (res.next()) {
				String disease = "["+res.getString("disease_id")+"] "+res.getString("disease_name");
				if (!results.containsKey(disease)) {
					results.put(disease, new TreeMap<>());
				}
				results.get(disease).put("["+res.getString("ontology_id")+"] "+res.getString("name"), res.getInt("db_term_id"));
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}
	
	public static Map<String, Map<String,Integer>> searchGenes(String query, Reference reference){
		Map<String, Map<String,Integer>> results = new TreeMap<>();
		try (Results res = Highlander.getDB().select(reference, Schema.HPO, 
				"SELECT gene_symbol, db_term_id, ontology_id, name "
				+ "FROM db_gene "
				+ "JOIN db_term_db_genes ON db_term_db_genes.db_gene_id = db_gene.id "
				+ "JOIN db_term ON db_term.id = db_term_db_genes.db_term_id "
				+ "WHERE INSTR(gene_symbol,'"+query+"')")) {
			while (res.next()) {
				String gene = res.getString("gene_symbol");
				if (!results.containsKey(gene)) {
					results.put(gene, new TreeMap<>());
				}
				results.get(gene).put("["+res.getString("ontology_id")+"] "+res.getString("name"), res.getInt("db_term_id"));
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}
}
