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

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;

/**
* Used to open an external website on a browser, already on a given variant/gene/etc
* 
* @author Raphael Helaers
*/

public class ExternalLink implements Comparable<ExternalLink> {
	
	private final int HEIGHT = 36;

	private static Map<Reference, Variant> lastLiftOver = new HashMap<>();

	private int id = -1;
	private String name;
	private int ordering = -1;
	private String description;
	private String url;
	private String url_parameters;
	private ImageIcon icon;
	private Map<Reference, String> genomes = new HashMap<>();
	private boolean enable = true;

	public ExternalLink(String name, String description, String url, String url_parameters) {
		this.name = name;
		this.description = description;
		this.url = url;
		this.url_parameters = url_parameters;
	}
	
	public ExternalLink(int id) throws Exception {
		this.id = id;
		fetchInfo();
	}
	
	public ExternalLink(Results res) throws Exception {
		setFields(res);
	}
	
	public void fetchInfo() throws Exception {
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM external_links WHERE id = "+id)) {
			if (res.next()){
				setFields(res);
			}
		}
	}

	private void setFields(Results res) throws Exception {
		id = res.getInt("id");
		name = res.getString("name");
		ordering = res.getInt("ordering");
		description = res.getString("description");
		url = res.getString("url");
		url_parameters = res.getString("url_parameters");
		enable = res.getBoolean("enable");
		String references = res.getString("url_genome");
		if (references != null && references.length() > 0) {
			for (String reference : references.split(";")) {
				String ref = reference.split("=")[0];
				String genome = reference.split("=")[1];
				for (Reference r : Reference.getAvailableReferences()) {
					if (r.getName().equals(ref)) {
						genomes.put(r, genome);
						break;
					}
				}
			}
		}
		if (res.getObject("icon") != null) {
			try{
				Blob imagedata = res.getBlob("icon") ;
				//Won't work on a Unix without X11, put the try/catch for the dbBuilder to work on those
				Image img = Toolkit.getDefaultToolkit().createImage(imagedata.getBytes(1, (int)imagedata.length()));
				icon = new ImageIcon(img);	
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	public void insert(File iconFile) throws Exception {
		ordering = 0;
		for (ExternalLink el : getAvailableExternalLinks()) {
			if (el.getOrdering() > ordering) ordering = el.getOrdering();
		}
		ordering++;
		try (Connection con = Highlander.getDB().getConnection(null, Schema.HIGHLANDER)){
			try (PreparedStatement pstmt = con.prepareStatement(Highlander.getDB().formatQuery(Schema.HIGHLANDER, 
					"INSERT INTO external_links "
							+ "(`name`,`ordering`,`description`,`url`,`url_parameters`,`url_genome`,`enable`,`icon`) "
							+ "VALUES ("
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getName().toString())+"',"
							+ ""+ordering+","
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"',"
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getURL())+"',"
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getURLParameters())+"',"
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getReferencesNameInURLAsDatabaseString())+"',"
							+ enable +","
							+ "?)"))){
				FileInputStream input = new FileInputStream(iconFile);
				pstmt.setBinaryStream(1, input);
				pstmt.executeUpdate();
			}
		}
		Image img = ImageIO.read(iconFile);
		icon = new ImageIcon(img);	
	}
	
	public void insert(InputStream inputIcon) throws Exception {
		ordering = 0;
		for (ExternalLink el : getAvailableExternalLinks()) {
			if (el.getOrdering() > ordering) ordering = el.getOrdering();
		}
		ordering++;
		try (Connection con = Highlander.getDB().getConnection(null, Schema.HIGHLANDER)){
			try (PreparedStatement pstmt = con.prepareStatement(Highlander.getDB().formatQuery(Schema.HIGHLANDER, 
					"INSERT INTO external_links "
							+ "(`name`,`ordering`,`description`,`url`,`url_parameters`,`url_genome`,`enable`,`icon`) "
							+ "VALUES ("
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getName().toString())+"',"
							+ ""+ordering+","
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"',"
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getURL())+"',"
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getURLParameters())+"',"
							+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getReferencesNameInURLAsDatabaseString())+"',"
							+ enable +","
							+ "?)"))){
				pstmt.setBinaryStream(1, inputIcon);
				pstmt.executeUpdate();
			}
		}
	}
	
	public void insert() throws Exception {
		ordering = 0;
		for (ExternalLink el : getAvailableExternalLinks()) {
			if (el.getOrdering() > ordering) ordering = el.getOrdering();
		}
		ordering++;
		id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, "INSERT INTO external_links "
				+ "(`name`,`ordering`,`description`,`url`,`url_parameters`,`url_genome`,`enable`) "
				+ "VALUES ("
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getName().toString())+"',"
				+ ""+ordering+","
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"',"
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getURL())+"',"
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getURLParameters())+"',"
				+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getReferencesNameInURLAsDatabaseString())+"',"
				+ enable +")");
	}
	
	public void update() throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE external_links SET " +
				"name = '"+Highlander.getDB().format(Schema.HIGHLANDER, getName())+"', " +
				"ordering = "+getOrdering()+", " +
				"description = '"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"', " +
				"url = '"+Highlander.getDB().format(Schema.HIGHLANDER, getURL())+"', " +
				"url_parameters = '"+Highlander.getDB().format(Schema.HIGHLANDER, getURLParameters())+"', " +
				"url_genome = '"+Highlander.getDB().format(Schema.HIGHLANDER, getReferencesNameInURLAsDatabaseString())+"', " +
				"enable = "+enable+" " +
				"WHERE id = "+id);		
	}

	public void updateIcon(File file) throws Exception {
		try (Connection con = Highlander.getDB().getConnection(null, Schema.HIGHLANDER)){
			try (PreparedStatement pstmt = con.prepareStatement(Highlander.getDB().formatQuery(Schema.HIGHLANDER,"UPDATE external_links SET icon = ? WHERE id = "+id))){
				FileInputStream input = new FileInputStream(file);
				pstmt.setBinaryStream(1, input);
				pstmt.executeUpdate();
			}
		}
		Image img = ImageIO.read(file);
		icon = new ImageIcon(img);	
	}
	
	public void removeIcon() throws Exception {
		icon = null;
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE external_links SET " +
				"icon = NULL " +
				"WHERE id = "+id);	
	}
	
	public void delete() throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM external_links WHERE id = "+id);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getOrdering() {
		return ordering;
	}

	public void setOrdering(int ordering) {
		this.ordering = ordering;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getURL() {
		return url;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getURLParameters() {
		return url_parameters;
	}
	
	public void setURLParameters(String url_parameters) {
		this.url_parameters = url_parameters;
	}
	
	public ImageIcon getIcon() {
		return icon;
	}

	public ImageIcon getScaledIcon() {
		return Resources.getHeightScaledIcon(icon, HEIGHT);
	}
	
	public void addReference(Reference reference, String nameInURL) {
		if (reference != null && reference.getName().length() > 0) {
			if (nameInURL != null && nameInURL.length() > 0) {
				genomes.put(reference, nameInURL);
			}else {
				genomes.remove(reference);
			}
		}
	}
	
	public String getReferenceNameInURL(Reference reference) {
		if (!genomes.containsKey(reference)) return "";
		return genomes.get(reference);
	}
	
	private String getReferencesNameInURLAsDatabaseString() {
		StringBuilder sb = new StringBuilder();
		for (Reference reference : genomes.keySet()) {
			sb.append(";");
			sb.append(reference.getName());
			sb.append("=");
			sb.append(genomes.get(reference));
		}
		if (!genomes.isEmpty()) {
			sb.deleteCharAt(0);
		}
		return sb.toString();
	}
	
	public int getId() {
		return id;
	}

	public boolean isEnable() {
		return enable;
	}
	
	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	
	public Set<String> getNeededAnnotations() {
		Set<String> annotations = new HashSet<>();
		for (String string : new String[] {url, url_parameters}) {
			for (int i=0 ; i < string.length() ; i++) {
				if (string.charAt(i) == '[') {
					String annotation = "";
					i++;
					while(string.charAt(i) != ']') {
						annotation += string.charAt(i);
						i++;
					}
					annotations.add(annotation.split(":")[0]);
				}
			}
		}
		return annotations;
	}
	
	private Set<String> getAnnotationsToSplit() {
		Set<String> annotations = new HashSet<>();
		for (String string : new String[] {url, url_parameters}) {
			for (int i=0 ; i < string.length() ; i++) {
				if (string.charAt(i) == '[') {
					String annotation = "";
					i++;
					while(string.charAt(i) != ']') {
						annotation += string.charAt(i);
						i++;
					}
					if (annotation.contains(":")) {
						annotations.add(annotation);
					}
				}
			}
		}
		return annotations;
	}
	
	private Map<String, Map<String,String>> getAnnotations(List<Integer> variantIds){
		Map<String, Map<String,String>> annotations = new HashMap<>();
		Set<String> needed = getNeededAnnotations();		
		StringBuilder fields = new StringBuilder();
		AnalysisFull analysis = Highlander.getCurrentAnalysis();
		for (String annotation : needed) {
			if (annotation.equals("protein_change")){
				fields.append(Field.variant_type.getQuerySelectName(analysis, false)+", "+Field.hgvs_protein.getQuerySelectName(analysis, false)+", "+Field.transcript_uniprot_id.getQuerySelectName(analysis, false)+", ");
			}else if (annotation.equals("pos_grch37") || annotation.equals("chr_grch37") || annotation.equals("pos_grch38") || annotation.equals("chr_grch38")) {
				fields.append(Field.pos.getQuerySelectName(analysis, false)+", "+Field.chr.getQuerySelectName(analysis, false)+", ");
			}else if (annotation.equals("reference")) {
				fields.append(Field.variant_type.getQuerySelectName(analysis, false)+", "+Field.reference.getQuerySelectName(analysis, false)+", ");
			}else if (!annotation.equals("genome")) {
				Field field = Field.getField(annotation);
				if (field.hasAnalysis(analysis)) {
					fields.append(field.getQuerySelectName(analysis, false));
					fields.append(", ");
				}
			}
		}
		if (fields.length() > 0) {
			fields.delete(fields.length()-2, fields.length());
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT "+fields.toString()+" "
							+ "FROM " +	analysis.getFromSampleAnnotations()
							+ analysis.getJoinStaticAnnotations()
							+ analysis.getJoinCustomAnnotations()
							+ analysis.getJoinGeneAnnotations()
							+ analysis.getJoinProjects()
							+ analysis.getJoinPathologies()
							+ analysis.getJoinPopulations()
							+	"WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN (" + HighlanderDatabase.makeSqlList(variantIds, Integer.class) + ")")){
				while (res.next()){
					Map<String, String> variantAnnot = new TreeMap<>();
					boolean missingAnnotation = false;
					for (String annotation : needed) {
						if (annotation.equals("protein_change")){
							VariantType variantType = VariantType.valueOf(res.getString(Field.variant_type.getName()));
							String hgvsp = res.getString(Field.hgvs_protein.getName());
							String uprot = res.getString(Field.transcript_uniprot_id.getName());
							if (variantType == VariantType.SNV && uprot != null && uprot.length() > 0 && hgvsp != null && hgvsp.length() > 0 && hgvsp.startsWith("p.") && !hgvsp.contains("*") && !hgvsp.contains("?")) {
								try{
									String mutation = hgvsp.substring(2)
											.replace("Ala","A")
											.replace("Arg","R")
											.replace("Asn","N")
											.replace("Asp","D")
											.replace("Cys","C")
											.replace("Glu","E")
											.replace("Gln","Q")
											.replace("Gly","G")
											.replace("His","H")
											.replace("Ile","I")
											.replace("Leu","L")
											.replace("Lys","K")
											.replace("Met","M")
											.replace("Phe","F")
											.replace("Pro","P")
											.replace("Ser","S")
											.replace("Thr","T")
											.replace("Trp","W")
											.replace("Tyr","Y")
											.replace("Val","V")
											;
									if (mutation != null && mutation.length() > 0 && !mutation.substring(0, 1).equals(mutation.substring(mutation.length()-1,mutation.length()))){
										variantAnnot.put(annotation, mutation);
									}else{
										missingAnnotation = true;
									}
								}catch(Exception sub){
									sub.printStackTrace();
								}
							}
						}else if (annotation.equals("chr_grch37") || annotation.equals("pos_grch37")) {
							if (res.getString("pos") != null && res.getString("pos").length() > 0 && res.getString("chr") != null && res.getString("chr").length() > 0) {
								int pos = res.getInt("pos");
								String chr = res.getString("chr");
								if (analysis.getReference().getSpecies().equals("homo_sapiens") && analysis.getReference().getGenomeVersion() != 37){
										Variant variant = new Variant(chr, pos);
										Reference grch37 = null;
										for (Reference ref : Reference.getAvailableReferences()) {
											if (ref.getSpecies().equals("homo_sapiens") && ref.getGenomeVersion() == 37) {
												grch37 = ref;
												break;
											}
										}
										if (grch37 != null) {
											Variant liftover = liftOver(variant, analysis.getReference(), grch37);
											if (liftover != null) {
												variantAnnot.put("chr_grch37", liftover.getChromosome());											
												variantAnnot.put("pos_grch37", liftover.getPosition()+"");
											}else {
												missingAnnotation = true;
											}
										}else {
											missingAnnotation = true;
										}
								}else {
									variantAnnot.put("chr_grch37", chr);								
									variantAnnot.put("pos_grch37", pos+"");								
								}
							}else{
								missingAnnotation = true;
							}
						}else if (annotation.equals("chr_grch38") || annotation.equals("pos_grch38")) {
							if (res.getString("pos") != null && res.getString("pos").length() > 0 && res.getString("chr") != null && res.getString("chr").length() > 0) {
								int pos = res.getInt("pos");
								String chr = res.getString("chr");
								if (analysis.getReference().getSpecies().equals("homo_sapiens") && analysis.getReference().getGenomeVersion() != 38){
										Variant variant = new Variant(chr, pos);
										Reference grch38 = null;
										for (Reference ref : Reference.getAvailableReferences()) {
											if (ref.getSpecies().equals("homo_sapiens") && ref.getGenomeVersion() == 38) {
												grch38 = ref;
												break;
											}
										}
										if (grch38 != null) {
											Variant liftover = liftOver(variant, analysis.getReference(), grch38);
											if (liftover != null) {
												variantAnnot.put("chr_grch38", liftover.getChromosome());											
												variantAnnot.put("pos_grch38", liftover.getPosition()+"");
											}else {
												missingAnnotation = true;
											}
										}else {
											missingAnnotation = true;
										}
								}else {
									variantAnnot.put("chr_grch38", chr);								
									variantAnnot.put("pos_grch38", pos+"");								
								}
							}else{
								missingAnnotation = true;
							}
						}else if (annotation.equals("genome")) {
							String a = getReferenceNameInURL(analysis.getReference());
							if (a != null && a.length() > 0) {
								variantAnnot.put(annotation, a);
							}else {
								missingAnnotation = true;
							}
						}else if (annotation.equals("reference")) {
							VariantType variantType = VariantType.valueOf(res.getString(Field.variant_type.getName()));
							if (variantType == VariantType.SV) {
								missingAnnotation = true;
							}else {
								String a = res.getString(annotation);
								if (a != null && a.length() > 0) {
									variantAnnot.put(annotation, a);
								}else{
									missingAnnotation = true;
								}
							}
						}else if (annotation.equals("analysis")) {
							String a = analysis.toString();
							if (a != null && a.length() > 0) {
								variantAnnot.put(annotation, a);
							}else {
								missingAnnotation = true;
							}
						}else {
							Field field = Field.getField(annotation);
							if (field.hasAnalysis(analysis)) {
								String a = res.getString(annotation);
								if (a != null && a.length() > 0) {
									variantAnnot.put(annotation, a);
								}else{
									missingAnnotation = true;
								}
							}else {
								missingAnnotation = true;
							}
						}
					}
					if (!missingAnnotation) {
						StringBuilder key = new StringBuilder();
						for (String value : variantAnnot.values()) {
							key.append(value);
							key.append("-");
						}
						if (key.length() > 0) {
							annotations.put(key.toString(), variantAnnot);
						}
					}
				}
			}catch(Exception ex) {
				System.err.println("External link '"+name+"' probably has a problem in its URL");
				ex.printStackTrace();
			}
		}
		return annotations;
	}
	
	/**
	 * As liftover is performed using Ensembl web service (rest.ensembl.org), it can sometimes be slow.
	 * To avoid converting the same variant multiple times (for each external link that needs it), the last liftover is saved.
	 * This method first check if the saved liftover is the good one, and only call the liftover method if it's not the case.
	 * 
	 * @param variant variant to liftover
	 * @param from source reference
	 * @param to	destination reference
	 * @return the variant with position in destination reference or null if the position doesn't exist in destination reference
	 * @throws Exception
	 */
	private Variant liftOver(Variant variant, Reference from, Reference to) throws Exception {
		if (lastLiftOver.containsKey(from) && lastLiftOver.get(from).equals(variant)) {
			if (lastLiftOver.containsKey(to)) 
				return lastLiftOver.get(to);
		}		
		Variant liftover = variant.liftOver(from, to);
		lastLiftOver.clear();
		lastLiftOver.put(from, variant);
		lastLiftOver.put(to, liftover);
		return liftover;
	}
	
	public List<URI> getURIs(List<Integer> variantIds){
		List<URI> uris = new ArrayList<>();
		Set<String> annotSplit = getAnnotationsToSplit();
		for (Map<String, String> variantAnnot : getAnnotations(variantIds).values()) {	
			String finalURL = getURL();
			String finalParameters = getURLParameters();
			for (String annotation : variantAnnot.keySet()) {
				finalURL = finalURL.replaceAll("\\["+annotation+"\\]", variantAnnot.get(annotation));	
				finalParameters = finalParameters.replaceAll("\\["+annotation+"\\]", variantAnnot.get(annotation));	
				for (String toSplit : annotSplit) {
					if (annotation.equals(toSplit.split(":")[0])) {
						finalURL = finalURL.replaceAll("\\["+toSplit+"\\]", variantAnnot.get(toSplit.split(":")[0]).substring(Integer.parseInt(toSplit.split(":")[1])));	
						finalParameters = finalParameters.replaceAll("\\["+toSplit+"\\]", variantAnnot.get(toSplit.split(":")[0]).substring(Integer.parseInt(toSplit.split(":")[1])));	
					}
				}
			}
			if (finalURL.contains("www.ensembl.org") && Highlander.getCurrentAnalysis().getReference().getSpecies().equals("homo_sapiens") && Highlander.getCurrentAnalysis().getReference().getGenomeVersion() == 37) {
				finalURL = finalURL.replace("www.ensembl.org","grch37.ensembl.org");
			}
			try {
				if (url_parameters == null || url_parameters.length() == 0) {
					uris.add(new URI(finalURL));
				}else {
					String encodedParam = URLEncoder.encode(finalParameters,"UTF-8").replaceAll("\\+", "%20");
					uris.add(new URI(finalURL+encodedParam));
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return uris;
	}
	
	/**
	 * Check if at least one URL can be obtained for the given list of variants.
	 * If you need a listener for the button, the check is done in {@link ExternalLink#getActionListener(List)} method already, to avoid building 2 times the list of URLs.
	 * 
	 * NOTE: I tried to check if the URL exists before adding it to the list (using {@link Tools#exists(String)}).
	 * It can be usefull to avoid showing a button if there is nothing on the page.
	 * 
	 * It work for some websites that throws 404 errors like DBSnp or ClinVarMiner
	 * BUT for websites like Ensembl or NCBI, the {@link Tools#exists(String)} method always return false.
	 *  
	 * And it can cause 'lag' problems if the web site take some time to respond, 
	 * or worse if it is unreachable (taking 30 seconds before responding, an so blocking the URL list building method).
	 * 
	 * It still could be achieved 'case by case' by adding a boolean flag to each ExternalLink 
	 * (admin decides for each link if the check should be done or not, its only relevant for a few websites like DBSnp or ClinVarMiner).
	 * 
	 * But as it can be deceiving (if admin doesn't test extensively) I choosed to not include the feature (yet).
	 * 
	 * @param variantIds
	 * @return false if no web page can be open for the given variant ids
	 */
	public boolean hasURL(List<Integer> variantIds) {
		 return getURIs(variantIds).isEmpty();
	}
	
	public JButton getButton() {
		JButton button = new JButton();
		if (enable) {
			if (icon != null) {
				button.setIcon(getScaledIcon());
			}else {
				button.setFont(button.getFont().deriveFont(Font.BOLD, 26));
				button.setText(name);
			}
		}else {
			button.setFont(button.getFont().deriveFont(Font.BOLD, 26));
			button.setForeground(Color.GRAY);
			button.setText(name + " [DISABLED]");
		}
		button.setToolTipText(description);
		return button;
	}
	
	/**
	 * Return an action listener for the button, that open the web browser with a page for each variant in the given list
	 * If the URL needs some annotation that is missing for a variant, no web page is open for that variant
	 * If all given variants have missing annotation, and so no web page would be open at all, return an empty optional
	 * 
	 * @param variantIds
	 * @return an optional listener (empty if no web page can be open for the given variant ids)
	 */
	public Optional<ActionListener> getActionListener(List<Integer> variantIds) {
		List<URI> uris = getURIs(variantIds);
		if (uris.isEmpty()) return Optional.empty();
		return Optional.of(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){				
						for (URI uri : uris) {	
							Tools.openURL(uri);
						}
					}
				}, "ExternalLink.openURL").start();
			}
		});
	}
	
	@Override
	public String toString(){
		return name;
	}

	@Override
	public int hashCode() {
		if (id >= 0)
			return id;
		else
			return name.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ExternalLink))
			return false;
		ExternalLink el = (ExternalLink)obj;
		if (id >= 0 && el.id >= 0)
			return id == el.id;
		return name.equals(el.name) && description.equals(el.description) && url.equals(el.url) && url_parameters.equals(el.url_parameters);
	}

	@Override
	public int compareTo (ExternalLink el) {
		if (ordering >= 0 && el.ordering >= 0) {
			if (ordering != el.ordering) {
				return Integer.compare(ordering, el.ordering);				
			}else {
				return name.compareTo(el.name);	
			}
		}else if (ordering >= 0){
			return Integer.compare(ordering, Integer.MAX_VALUE);			
		}else if (el.ordering >= 0){
			return Integer.compare(Integer.MAX_VALUE, el.ordering);
		}else{
			return name.compareTo(el.name);
		}
	}

	public static List<ExternalLink> getAvailableExternalLinks() throws Exception {
		List<ExternalLink> els = new ArrayList<ExternalLink>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM external_links ORDER BY ordering")) {
			while (res.next()){
				els.add(new ExternalLink(res));
			}
		}
		return els;
	}
}
