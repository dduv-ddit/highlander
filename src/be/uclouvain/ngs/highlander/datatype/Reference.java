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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
 * A genome of reference
 * 
 * Map the 'reference' table in the database
 * 
 * @author Raphaël Helaers
 *
 */
public class Reference implements Comparable<Reference> {
	
	private String name;
	private List<String> chromosomes = new ArrayList<String>();
	private String description;
	private final Map<Schema, String> schemas = new EnumMap<Schema, String>(Schema.class);

	public Reference(String name) throws Exception {
		if (name == null) throw new Exception("Reference name is null");
		this.name = name;
		fetchInfo();
	}
			
	public Reference(Results res) throws Exception {
		setFields(res);
	}
	
	public Reference(String name, List<String> chromosomes, String description, Map<Schema, String> schemas){
		this.name = name;
		this.chromosomes.addAll(chromosomes);
		this.description = description;
		this.schemas.putAll(schemas);
	}

	public Reference(String name, List<String> chromosomes, String description, Schema schema, String schemaName){
		this.name = name;
		this.chromosomes.addAll(chromosomes);
		this.description = description;
		this.schemas.put(schema, schemaName);
	}
	
	public void fetchInfo() throws Exception {
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM `references` WHERE reference = '"+name+"'")) {
			setFields(res);
		}
	}

	private void setFields(Results res) throws Exception {
		while (res.next()) {
			name = res.getString("reference");
			if (res.getString("chromosomes") != null && chromosomes.isEmpty()) {
				for (String chr : res.getString("chromosomes").split(",")) {
					chromosomes.add(chr);
				}
			}
			description = res.getString("description");
			String schema_code = res.getString("annotation_code"); 
			String path = res.getString("annotation_schema");
			try {
				Schema schema = Schema.valueOf(schema_code.toUpperCase());  			
				schemas.put(schema,path);
			}catch(Exception ex) {
				System.err.println("Schema " + schema_code + " for reference " + name + " is not recognized by Highlander.");
			}
		}
	}

	/**
	 * Insert this reference in the database.
	 * The reference name MUST NOT already exist.
	 * 
	 * @throws Exception
	 */
	public void insert() throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `references` WHERE `reference` = '"+getName()+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count > 0){
			throw new Exception("Reference '"+getName()+"' already exists in the database");
		}else {
			if (!schemas.containsKey(Schema.ENSEMBL)) {
				throw new Exception("Reference '"+getName()+"' must define the ENSEMBL schema");
			}else{
				for (Schema schema : getAvailableSchemas()) {
					Highlander.getDB().update(Schema.HIGHLANDER, "INSERT INTO `references` (reference, annotation_code, annotation_schema, chromosomes, description) " +
							"VALUES (" +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, getName())+"', " +
							"'"+((schema == null)?"":schema.toString())+"', " +
							"'"+((schema == null)?"":getSchemaName(schema))+"', " +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, getChromosomesAsString())+"', " +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"')");		
				}
				Highlander.getDB().addReference(this);
			}			
		}
	}
	
	/**
	 * Add or modify given schema for this reference in the database
	 * 
	 * @param schema
	 * @param schemaName
	 */
	public void updateSchema(Schema schema, String schemaName) throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM `references` WHERE reference = '"+name+"' AND annotation_code = '"+schema.toString()+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "INSERT INTO `references` (`reference`,`chromosomes`,`description`,`annotation_code`,`annotation_schema`) "
				+ "VALUES ("
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getName())+"',"
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getChromosomesAsString())+"',"
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"',"
				+ "'"+schema.toString()+"',"
				+ "'"+schemaName+"')");
		schemas.put(schema,schemaName);
	}
	
	/**
	 * Delete given schema for this reference in the database
	 * @param schema
	 * @throws Exception
	 */
	public void deleteSchema(Schema schema) throws Exception {
		if (schema == Schema.ENSEMBL) {
			throw new Exception("The ENSEMBL schema cannot be deleted");
		}else{
			Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM `references` WHERE reference = '"+name+"' AND annotation_code = '"+schema.toString()+"'");
			schemas.remove(schema);
		}
	}
	
	/**
	 * Modify description for this reference in the database
	 * @param newDescription
	 * @throws Exception
	 */
	public void updateChromosomes(List<String> newChromosomes) throws Exception {
		this.chromosomes = new ArrayList<>(newChromosomes);
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `references` SET chromosomes = '"+Highlander.getDB().format(Schema.HIGHLANDER, getChromosomesAsString())+"' WHERE reference = '"+getName()+"'");
	}
	
	/**
	 * Modify description for this reference in the database
	 * @param newDescription
	 * @throws Exception
	 */
	public void updateDescription(String newDescription) throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `references` SET description = '"+Highlander.getDB().format(Schema.HIGHLANDER, newDescription)+"' WHERE reference = '"+getName()+"'");
		this.description = newDescription;
	}
	
	/**
	 * Modify name for this reference in the database
	 * The reference name MUST NOT already exist. 
	 * @param newName
	 * @throws Exception
	 */
	public void updateName(String newName) throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `references` WHERE `reference` = '"+newName+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count > 0){
			throw new Exception("Reference '"+newName+"' already exist in the database");
		}else {
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `references` SET reference = '"+newName+"' WHERE reference = '"+getName()+"'");
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `analyses` SET reference = '"+newName+"' WHERE reference = '"+getName()+"'");
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `users_data` SET analysis = '"+newName+"' WHERE analysis = '"+getName()+"' AND `type` = 'INTERVALS'");
			this.name = newName;
		}
	}
	
	public void delete() throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `analyses` WHERE `reference` = '"+getName()+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count == 0){
			Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM `references` WHERE reference = '"+getName()+"'");
			Highlander.getDB().removeReference(this);
		}else{
			throw new Exception(count + " analyses are still linked to this reference. Delete those analyses or link them to another reference.");
		}
	}

	public String getName() {
		return name;
	}
	
	public List<String> getChromosomes(){
		return new ArrayList<>(chromosomes);
	}
	
	public String getChromosomesAsString(){
		StringBuilder sb = new StringBuilder();
		for (String chr : chromosomes) {
			sb.append(chr);
			sb.append(",");
		}
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public String getDescription() {
		return description;
	}
	
	public boolean hasSchema(Schema schema) {
		return (schemas.containsKey(schema) && schemas.get(schema) != null);
	}
	
	public Set<Schema> getAvailableSchemas() {
		return EnumSet.copyOf(schemas.keySet());
	}

	public String getSchemaName(Schema schema) throws Exception {
		if (hasSchema(schema)) {
			return schemas.get(schema);
		}
		throw new Exception("Highlander database has no schema '" + schema + "' for reference '" + name + "'");
	}
	
	public boolean usesSameReferenceSequenceAs(Reference reference) {
		return schemas.get(Schema.ENSEMBL).equals(reference.schemas.get(Schema.ENSEMBL));
	}
	
	public String getSpecies() {
		return schemas.get(Schema.ENSEMBL).substring(0, schemas.get(Schema.ENSEMBL).indexOf("_core"));
	}

	public int getEnsemblVersion() {
		String[] parse = schemas.get(Schema.ENSEMBL).split("_");
		int version = -1;
		try {
			version = Integer.parseInt(parse[parse.length-2]);
		}catch(NumberFormatException ex) {
			ex.printStackTrace();
		}
		return version;
	}
	
	public int getGenomeVersion() {
		String[] parse = schemas.get(Schema.ENSEMBL).split("_");
		int version = -1;
		try {
			version = Integer.parseInt(parse[parse.length-1]);
		}catch(NumberFormatException ex) {
			ex.printStackTrace();
		}
		return version;
	}
	
	@Override
	public String toString(){
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		return name.equals(obj.toString());
	}

	@Override
	public int compareTo (Reference r) {
		return name.compareTo(r.name);
	}
	
	public static Set<Reference> getAvailableReferences() {
		return Highlander.getDB().getAvailableReferences();
	}

	public static Reference getReference(String name) throws Exception {
		for (Reference r : getAvailableReferences()){
			if (r.getName().equalsIgnoreCase(name)) return r;
		}
		throw new Exception("Reference " + name + " doesn't exist");
	}
	
}
