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

package be.uclouvain.ngs.highlander.database;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

public class Category implements Comparable<Category>, Serializable {

	private static final long serialVersionUID = 1L;

	private static List<Category> availableCategories = new ArrayList<Category>();

	private String name;
	private int ordering;
	private boolean has_generic_detail_box;
	private Palette color;

	public static List<Category> getAvailableCategories(){
		return new ArrayList<Category>(availableCategories);
	}

	public static String[] getAvailableCategories(boolean addCategoryAll, boolean alphabeticalOrder){
		Collection<Category> cats;
		if (alphabeticalOrder){
			cats = new TreeSet<Category>(availableCategories);
		}else{
			cats = availableCategories;
		}
		int size = cats.size();
		if (addCategoryAll) size++;
		String[] res = new String[size];
		int i=0;
		if (addCategoryAll) res[i++] = "all available fields";
		for (Category cat : cats){
			res[i++] = cat.getName();
		}
		return res;
	}

	public static Category valueOf(String string) throws Exception {
		if (availableCategories.isEmpty()) throw new Exception("Categories have not been retreive in the database. fetchAvailableCategories method must be used before this method.");
		for (Category cat : availableCategories){
			if (cat.name.equals(string)) return cat;
		}
		return null;
	}

	public static void fetchAvailableCategories(HighlanderDatabase DB) throws Exception {
		availableCategories.clear();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM field_categories ORDER BY ordering ASC")) {
			while (res.next()){
				availableCategories.add(new Category(res));
			}
		}
	}

	public Category(String name) {
		this.name = name;
	}
	
	public Category(Results res) throws Exception {
		name = res.getString("category");
		ordering = res.getInt("ordering");
		has_generic_detail_box = res.getBoolean("has_generic_detail_box");
		color = Palette.valueOf(res.getString("color"));
	}

	public void insert() throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `field_categories` WHERE `category` = '"+getName()+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count > 0){
			throw new Exception("Category '"+getName()+"' already exists in the database");
		}else {
			ordering = 0;
			for (Category cat : getAvailableCategories()) {
				if (cat.getOrdering() > ordering) ordering = cat.getOrdering();
			}
			ordering++;
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"INSERT INTO `field_categories` "
					+ "(`category`, `ordering`, `has_generic_detail_box`, `color`) "
					+ "VALUES ("
					+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getName())+"', "
					+ getOrdering()+", "
					+ ((hasGenericDetailBox())?"TRUE":"FALSE")+", "
					+ "'"+getColor()+"' "
					+ ")");		
		}
	}
	
	public void delete() throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `fields` WHERE `category` = '"+getName()+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count == 0){
			Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM `field_categories` WHERE `category` = '"+getName()+"'");
		}else{
			throw new Exception(count + " fields are still linked to this category. Delete those fields or link them to another category.");
		}
	}

	public void update() throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"UPDATE `field_categories` SET " +
				"`ordering` = "+getOrdering()+", " +
				"`has_generic_detail_box` = "+((hasGenericDetailBox())?"TRUE":"FALSE")+", " +
				"`color` = '"+getColor()+"' " +
				"WHERE `category` = '"+getName()+"'");	
	}

	public void updateName(String newName) throws Exception {
		newName = newName.toLowerCase();
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `field_categories` WHERE `category` = '"+newName+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count > 0){
			throw new Exception("Category '"+newName+"' already exist in the database");
		}else {
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"UPDATE `field_categories` SET " +
							"`category` = '"+Highlander.getDB().format(Schema.HIGHLANDER, newName)+"' " +
							"WHERE `category` = '"+getName()+"'");	
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"UPDATE `fields` SET " +
							"`category` = '"+Highlander.getDB().format(Schema.HIGHLANDER, newName)+"' " +
							"WHERE `category` = '"+getName()+"'");
			this.name = newName;
		}
	}
	public void setOrdering(int ordering) {
		this.ordering = ordering;
	}
	public void setGenericDetailBox(boolean has_generic_detail_box) {
		this.has_generic_detail_box = has_generic_detail_box;
	}
	public void setColor(Palette color) {
		this.color = color;
	}

	public String getName(){return name;}
	public int getOrdering(){return ordering;}
	public boolean hasGenericDetailBox(){return has_generic_detail_box;}
	public Palette getColor(){return color;}

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
	public int compareTo (Category a) {
		return name.compareTo(a.name);
	} 

}
