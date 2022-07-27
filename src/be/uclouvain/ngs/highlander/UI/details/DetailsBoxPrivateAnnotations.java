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

package be.uclouvain.ngs.highlander.UI.details;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
 * Private user annotations, visible an modifiable only by the user
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxPrivateAnnotations extends DetailsBox {

	private VariantsTable table;
	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;

	private Map<Integer, Integer> variant_sample_id_to_variant_annotation_id = new HashMap<Integer,Integer>();
	private Map<Integer, Integer> variant_sample_id_to_gene_annotation_id = new HashMap<Integer,Integer>();
	private Map<Integer, Integer> variant_sample_id_to_sample_annotation_id = new HashMap<Integer,Integer>();
	private Map<Integer, String> variant_sample_id_to_variant_key = new HashMap<Integer,String>();
	private Map<Integer, String> variant_sample_id_to_gene_symbol = new HashMap<Integer,String>();
	private Map<Integer, String> variant_sample_id_to_project_id = new HashMap<Integer,String>();
	private Map<Integer, String> variant_sample_id_to_sample = new HashMap<Integer,String>();
	private JButton statusOfInterestVariant = new JButton();
	private JButton statusOfInterestGene = new JButton();
	private JButton statusOfInterestSample = new JButton();
	private JTextArea commentVariantTextArea = new JTextArea();
	private JTextArea commentGeneTextArea = new JTextArea();
	private JTextArea commentSampleTextArea = new JTextArea();
	private String val_gene_symbol = null;

	public DetailsBoxPrivateAnnotations(List<Integer> variantIds, DetailsPanel mainPanel, VariantsTable table){
		for (int id : variantIds){
			variant_sample_id_to_variant_annotation_id.put(id, -1);
			variant_sample_id_to_gene_annotation_id.put(id, -1);
			variant_sample_id_to_sample_annotation_id.put(id, -1);
		}
		this.variantSampleId = variantIds.get(0);
		this.mainPanel = mainPanel;
		this.table = table;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	public String getTitle(){
		return "Private annotations";
	}

	public Palette getColor() {
		return Field.variant_comments_private.getCategory().getColor();
	}

	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	private void set_of_interest_variant(String val){
		if (val != null && (val.equals("1") || val.equalsIgnoreCase("true"))){
			statusOfInterestVariant.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusOfInterestVariant.setToolTipText("You have marked this variant as 'of interest'");
		}else if (val != null && (val.equals("0") || val.equalsIgnoreCase("false"))){
			statusOfInterestVariant.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusOfInterestVariant.setToolTipText("You have marked this variant as 'not interesting'");
		}else{
			statusOfInterestVariant.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusOfInterestVariant.setToolTipText("You don't have marked this variant ... yet");
		}
	}

	private void set_of_interest_gene(String val){
		if (val != null && (val.equals("1") || val.equalsIgnoreCase("true"))){
			statusOfInterestGene.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusOfInterestGene.setToolTipText("You have marked this gene as 'of interest'");
		}else if (val != null && (val.equals("0") || val.equalsIgnoreCase("false"))){
			statusOfInterestGene.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusOfInterestGene.setToolTipText("You have marked this gene as 'not interesting'");
		}else{
			statusOfInterestGene.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusOfInterestGene.setToolTipText("You don't have marked this gene ... yet");
		}
	}

	private void set_of_interest_sample(String val){
		if (val != null && (val.equals("1") || val.equalsIgnoreCase("true"))){
			statusOfInterestSample.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusOfInterestSample.setToolTipText("You have marked this sample as 'of interest'");
		}else if (val != null && (val.equals("0") || val.equalsIgnoreCase("false"))){
			statusOfInterestSample.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusOfInterestSample.setToolTipText("You have marked this sample as 'not interesting'");
		}else{
			statusOfInterestSample.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusOfInterestSample.setToolTipText("You don't have marked this sample ... yet");
		}
	}
	
	private void updateDatabaseVariant(Field field, Object value) throws Exception {
		AnalysisFull analysis = Highlander.getCurrentAnalysis();
		Map<Integer,String> insertedVariants = new HashMap<>();
		for (int var_id : variant_sample_id_to_variant_annotation_id.keySet()){
			if (variant_sample_id_to_variant_annotation_id.get(var_id) < 0){
				String chr = "";
				int pos = 0;
				int length = 0;
				String reference = "";
				String alternative = "";
				String gene_symbol = "";
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT chr, pos, length, reference, alternative, gene_symbol "
						+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations() 
						+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + var_id)){
					if (res.next()){
						chr = res.getString("chr");
						pos = res.getInt("pos");
						length = res.getInt("length");
						reference = res.getString("reference");
						alternative = res.getString("alternative");
						gene_symbol = res.getString("gene_symbol");
					}
				}
				//Check if variant is present multiple times in the selection, to avoid re-insert them in DB, which will result in an exception
				String variantString = chr+"-"+pos+"-"+length+"-"+reference+"-"+alternative+"-"+gene_symbol;
				boolean alreadyInserted = false;
				for (int insertedId : insertedVariants.keySet()) {
					if (insertedVariants.get(insertedId).equals(variantString)) {
						alreadyInserted = true;
						variant_sample_id_to_variant_annotation_id.put(var_id, variant_sample_id_to_variant_annotation_id.get(insertedId));
						break;						
					}
				}
				if (!alreadyInserted) {
					int annotation_id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT IGNORE INTO " + Highlander.getCurrentAnalysis().getFromUserAnnotationsVariants()
									+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_comments`) " +
									"VALUES ('"+chr+"', "+pos+", "+length+", '"+reference+"','"+alternative+"', '"+gene_symbol+"', '"+Highlander.getLoggedUser().getUsername()+"', '')");
					variant_sample_id_to_variant_annotation_id.put(var_id, annotation_id);
					insertedVariants.put(var_id, variantString);					
				}
			}
		}
		String updateValue = "NULL";
		if (value != null) {
			if (field.getFieldClass() == Boolean.class) {
				updateValue = value.toString();
			}else {
				updateValue = "'"+Highlander.getDB().format(Schema.HIGHLANDER, value.toString())+"'";
			}
		}
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"UPDATE " + Highlander.getCurrentAnalysis().getFromUserAnnotationsVariantsPrivate()
				+ "SET "+field.getQueryWhereName(analysis, false)+" = "+updateValue + " "
				+ "WHERE id IN (" + HighlanderDatabase.makeSqlList(new HashSet<Integer>(variant_sample_id_to_variant_annotation_id.values()), Integer.class) + ")");		
		for (int id : variant_sample_id_to_variant_annotation_id.keySet()) {
			table.updateAnnotation(id, field, value);
			String[] variant = variant_sample_id_to_variant_key.get(id).split("-");
			table.updateAnnotation(variant[0], Integer.parseInt(variant[1]), Integer.parseInt(variant[2]), variant[3], variant[4], variant_sample_id_to_gene_symbol.get(id), field, value);
		}
	}

	private void updateDatabaseGene(Field field, Object value) throws Exception {
		AnalysisFull analysis = Highlander.getCurrentAnalysis();
		Map<Integer,String> insertedGenes = new HashMap<>();
		for (int var_id : variant_sample_id_to_gene_annotation_id.keySet()){
			if (variant_sample_id_to_gene_annotation_id.get(var_id) < 0){
				String gene_symbol = "";
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT gene_symbol "
								+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations() 
								+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + var_id)){
					if (res.next()){
						gene_symbol = res.getString("gene_symbol");
					}
				}
				//Check if gene is present multiple times in the selection, to avoid re-insert them in DB, which will result in an exception
				boolean alreadyInserted = false;
				for (int insertedId : insertedGenes.keySet()) {
					if (insertedGenes.get(insertedId).equals(gene_symbol)) {
						alreadyInserted = true;
						variant_sample_id_to_gene_annotation_id.put(var_id, variant_sample_id_to_gene_annotation_id.get(insertedId));
						break;						
					}
				}
				if (!alreadyInserted) {
					int annotation_id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT INTO " + Highlander.getCurrentAnalysis().getFromUserAnnotationsGenes()
							+ "(`gene_symbol`, `username`, `gene_comments`) " +
							"VALUES ('"+gene_symbol+"', '"+Highlander.getLoggedUser().getUsername()+"', '')");
					variant_sample_id_to_gene_annotation_id.put(var_id, annotation_id);
					insertedGenes.put(var_id, gene_symbol);
				}
			}
		}
		String updateValue = "NULL";
		if (value != null) {
			if (field.getFieldClass() == Boolean.class) {
				updateValue = value.toString();
			}else {
				updateValue = "'"+Highlander.getDB().format(Schema.HIGHLANDER, value.toString())+"'";
			}
		}
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"UPDATE " + Highlander.getCurrentAnalysis().getFromUserAnnotationsGenesPrivate()
				+ "SET "+field.getQueryWhereName(analysis, false)+" = "+updateValue + " "
				+ "WHERE id IN (" + HighlanderDatabase.makeSqlList(new HashSet<Integer>(variant_sample_id_to_gene_annotation_id.values()), Integer.class) + ")");		
		for (int id : variant_sample_id_to_gene_annotation_id.keySet()) {
			table.updateAnnotation(id, field, value);
			table.updateAnnotation(Field.gene_symbol, variant_sample_id_to_gene_symbol.get(id), field, value);
		}
	}
	
	private void updateDatabaseSample(Field field, Object value) throws Exception {
		AnalysisFull analysis = Highlander.getCurrentAnalysis();
		Map<Integer,Integer> insertedSamples = new HashMap<>();
		for (int var_id : variant_sample_id_to_sample_annotation_id.keySet()){
			if (variant_sample_id_to_sample_annotation_id.get(var_id) < 0){
				int project_id = -1;
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT project_id "
								+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations() 
								+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + var_id)){
					if (res.next()){
						project_id = res.getInt("project_id");
					}
				}
				//Check if sample is present multiple times in the selection, to avoid re-insert them in DB, which will result in an exception
				boolean alreadyInserted = false;
				for (int insertedId : insertedSamples.keySet()) {
					if (insertedSamples.get(insertedId) == project_id) {
						alreadyInserted = true;
						variant_sample_id_to_sample_annotation_id.put(var_id, variant_sample_id_to_sample_annotation_id.get(insertedId));
						break;						
					}
				}
				if (!alreadyInserted) {
					int annotation_id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT INTO " + Highlander.getCurrentAnalysis().getFromUserAnnotationsSamples()
							+ "(`project_id`, `username`, `sample_comments`) " +
							"VALUES ("+project_id+", '"+Highlander.getLoggedUser().getUsername()+"', '')");
					variant_sample_id_to_sample_annotation_id.put(var_id, annotation_id);
					insertedSamples.put(var_id, project_id);
				}
			}
		}
		String updateValue = "NULL";
		if (value != null) {
			if (field.getFieldClass() == Boolean.class) {
				updateValue = value.toString();
			}else {
				updateValue = "'"+Highlander.getDB().format(Schema.HIGHLANDER, value.toString())+"'";
			}
		}
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"UPDATE " + Highlander.getCurrentAnalysis().getFromUserAnnotationsSamplesPrivate()
				+ "SET "+field.getQueryWhereName(analysis, false)+" = "+updateValue + " "
				+ "WHERE id IN (" + HighlanderDatabase.makeSqlList(new HashSet<Integer>(variant_sample_id_to_sample_annotation_id.values()), Integer.class) + ")");		
		for (int id : variant_sample_id_to_sample_annotation_id.keySet()) {
			table.updateAnnotation(id, field, value);
			table.updateAnnotation(Field.project_id, variant_sample_id_to_project_id.get(id), field, value);
			table.updateAnnotation(Field.sample, variant_sample_id_to_sample.get(id), field, value);
		}
	}
	
	protected void loadDetails(){
		try{
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			String val_of_interest_variant = null;
			String val_private_comments_variant = null;
			String val_of_interest_gene = null;
			String val_private_comments_gene = null;
			String val_of_interest_sample = null;
			String val_private_comments_sample = null;
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT "
							+ analysis.getFromUserAnnotationsVariants().trim()+".`id` as var_annot_id, "
							+ analysis.getFromUserAnnotationsGenes().trim()+".`id` as gene_annot_id, "
							+ analysis.getFromUserAnnotationsSamples().trim()+".`id` as sample_annot_id, "
							+ Field.variant_of_interest.getQuerySelectName(analysis, true)+", "+Field.variant_comments_private.getQuerySelectName(analysis, true)+", "
							+ Field.gene_of_interest.getQuerySelectName(analysis, true)+", "+Field.gene_comments_private.getQuerySelectName(analysis, true)+", "
							+ Field.sample_of_interest.getQuerySelectName(analysis, true)+", "+Field.sample_comments_private.getQuerySelectName(analysis, true)+", "
							+ Field.chr.getQuerySelectName(analysis, true)+", "+Field.pos.getQuerySelectName(analysis, true)+", "+Field.length.getQuerySelectName(analysis, true)+", "
							+ Field.reference.getQuerySelectName(analysis, true)+", "+Field.alternative.getQuerySelectName(analysis, true)+", "+Field.gene_symbol.getQuerySelectName(analysis, true)+", "
							+ Field.project_id.getQuerySelectName(analysis, true) + ", " + Field.sample.getQuerySelectName(analysis, true)+", "
							+ Field.variant_sample_id.getQuerySelectName(analysis, true) + " "
							+	"FROM " + analysis.getFromSampleAnnotations()
							+ analysis.getJoinStaticAnnotations()
							+ analysis.getJoinProjects()
							+ analysis.getJoinGeneAnnotations()
							+ analysis.getJoinUserAnnotationsVariantsPrivate()
							+ analysis.getJoinUserAnnotationsGenesPrivate()
							+ analysis.getJoinUserAnnotationsSamplesPrivate()
							+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, true)+" IN (" + HighlanderDatabase.makeSqlList(variant_sample_id_to_variant_annotation_id.keySet(), Integer.class) + ")")){
				while (res.next()){
					int var_id = res.getInt(Field.variant_sample_id.getName());
					variant_sample_id_to_variant_key.put(var_id, res.getString(Field.chr.getName())+"-"+res.getString(Field.pos.getName())+"-"+res.getString(Field.length.getName())+"-"+res.getString(Field.reference.getName())+"-"+res.getString(Field.alternative.getName()));
					variant_sample_id_to_gene_symbol.put(var_id, res.getString(Field.gene_symbol.getName()));
					variant_sample_id_to_project_id.put(var_id, res.getString(Field.project_id.getName()));
					variant_sample_id_to_sample.put(var_id, res.getString(Field.sample.getName()));
					if (var_id == variantSampleId){
						val_gene_symbol = res.getString(Field.gene_symbol.getName());						
					}
					if (res.getObject("var_annot_id") != null) {
						variant_sample_id_to_variant_annotation_id.put(var_id, res.getInt("var_annot_id"));
						if (var_id == variantSampleId){
							val_of_interest_variant = res.getString(Field.variant_of_interest.getName());
							val_private_comments_variant = res.getString(Field.variant_comments_private.getName());
						}
					}
					if (res.getObject("gene_annot_id") != null) {
						variant_sample_id_to_gene_annotation_id.put(var_id, res.getInt("gene_annot_id"));
						if (var_id == variantSampleId){
							val_of_interest_gene = res.getString(Field.gene_of_interest.getName());
							val_private_comments_gene = res.getString(Field.gene_comments_private.getName());
						}
					}
					if (res.getObject("sample_annot_id") != null) {
						variant_sample_id_to_sample_annotation_id.put(var_id, res.getInt("sample_annot_id"));
						if (var_id == variantSampleId){
							val_of_interest_sample = res.getString(Field.sample_of_interest.getName());
							val_private_comments_sample = res.getString(Field.sample_comments_private.getName());
						}
					}
					
				}
			}

			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBackground(Resources.getColor(getColor(), 200, false));
			int y=0;
			
			statusOfInterestVariant = new JButton();
			statusOfInterestVariant.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusInterestVariantPopupMenu = new JPopupMenu();
			JMenuItem itemInterestVariantNull = new JMenuItem("You don't have marked this variant ... yet",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemInterestVariantNull.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.variant_of_interest,null);
						set_of_interest_variant(null);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInterestVariantPopupMenu.add(itemInterestVariantNull);
			JMenuItem itemInterestVariantTrue = new JMenuItem("You have marked this variant as 'of interest' (in all samples)",Resources.getScaledIcon(Resources.iButtonApply, 24));
			itemInterestVariantTrue.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.variant_of_interest,true);
						set_of_interest_variant("1");
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInterestVariantPopupMenu.add(itemInterestVariantTrue);
			JMenuItem itemInterestVariantFalse = new JMenuItem("You have marked this variant as 'not interesting' (in all samples)",Resources.getScaledIcon(Resources.iCross, 24));
			itemInterestVariantFalse.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.variant_of_interest,false);
						set_of_interest_variant("0");
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInterestVariantPopupMenu.add(itemInterestVariantFalse);
			MouseListener statusInterestVariantPopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					statusInterestVariantPopupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			};
			statusOfInterestVariant.addMouseListener(statusInterestVariantPopupListener);
			panel.add(statusOfInterestVariant, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelInterestVariant = new JLabel(Field.variant_of_interest.getName(), SwingConstants.LEFT);
			labelInterestVariant.setToolTipText(Field.variant_of_interest.getHtmlTooltip());
			panel.add(labelInterestVariant, new GridBagConstraints(1, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			set_of_interest_variant(val_of_interest_variant);
			y++;

			if (val_gene_symbol != null && val_gene_symbol.length() > 0){
				statusOfInterestGene = new JButton();
				statusOfInterestGene.setPreferredSize(new Dimension(28,28));
				final JPopupMenu statusInterestGenePopupMenu = new JPopupMenu();
				JMenuItem itemInterestGeneNull = new JMenuItem("You don't have marked this gene ... yet",Resources.getScaledIcon(Resources.iQuestion, 24));
				itemInterestGeneNull.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try{
							updateDatabaseGene(Field.gene_of_interest,null);
							set_of_interest_gene(null);
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				});
				statusInterestGenePopupMenu.add(itemInterestGeneNull);
				JMenuItem itemInterestGeneTrue = new JMenuItem("You have marked this gene as 'of interest'",Resources.getScaledIcon(Resources.iButtonApply, 24));
				itemInterestGeneTrue.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try{
							updateDatabaseGene(Field.gene_of_interest,true);
							set_of_interest_gene("1");
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				});
				statusInterestGenePopupMenu.add(itemInterestGeneTrue);
				JMenuItem itemInterestGeneFalse = new JMenuItem("You have marked this gene as 'not interesting'",Resources.getScaledIcon(Resources.iCross, 24));
				itemInterestGeneFalse.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try{
							updateDatabaseGene(Field.gene_of_interest,false);
							set_of_interest_gene("0");
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				});
				statusInterestGenePopupMenu.add(itemInterestGeneFalse);
				MouseListener statusInterestGenePopupListener = new MouseListener() {
					public void mouseReleased(MouseEvent e) {}
					public void mousePressed(MouseEvent e) {}
					public void mouseExited(MouseEvent e) {}
					public void mouseEntered(MouseEvent e) {}
					public void mouseClicked(MouseEvent e) {
						statusInterestGenePopupMenu.show(e.getComponent(), e.getX(), e.getY());
					}
				};
				statusOfInterestGene.addMouseListener(statusInterestGenePopupListener);
				panel.add(statusOfInterestGene, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
				JLabel labelInterestGene = new JLabel(Field.gene_of_interest.getName(), SwingConstants.LEFT);
				labelInterestGene.setToolTipText(Field.gene_of_interest.getHtmlTooltip());
				panel.add(labelInterestGene, new GridBagConstraints(1, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
				set_of_interest_gene(val_of_interest_gene);
			}
			y++;
			
			statusOfInterestSample = new JButton();
			statusOfInterestSample.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusInterestSamplePopupMenu = new JPopupMenu();
			JMenuItem itemInterestSampleNull = new JMenuItem("You don't have marked this sample ... yet",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemInterestSampleNull.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseSample(Field.sample_of_interest,null);
						set_of_interest_sample(null);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInterestSamplePopupMenu.add(itemInterestSampleNull);
			JMenuItem itemInterestSampleTrue = new JMenuItem("You have marked this sample as 'of interest'",Resources.getScaledIcon(Resources.iButtonApply, 24));
			itemInterestSampleTrue.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseSample(Field.sample_of_interest,true);
						set_of_interest_sample("1");
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInterestSamplePopupMenu.add(itemInterestSampleTrue);
			JMenuItem itemInterestSampleFalse = new JMenuItem("You have marked this sample as 'not interesting'",Resources.getScaledIcon(Resources.iCross, 24));
			itemInterestSampleFalse.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseSample(Field.sample_of_interest,false);
						set_of_interest_sample("0");
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInterestSamplePopupMenu.add(itemInterestSampleFalse);
			MouseListener statusInterestSamplePopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					statusInterestSamplePopupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			};
			statusOfInterestSample.addMouseListener(statusInterestSamplePopupListener);
			panel.add(statusOfInterestSample, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelInterestSample = new JLabel(Field.sample_of_interest.getName(), SwingConstants.LEFT);
			labelInterestSample.setToolTipText(Field.sample_of_interest.getHtmlTooltip());
			panel.add(labelInterestSample, new GridBagConstraints(1, y, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			set_of_interest_sample(val_of_interest_sample);
			y++;
			
			commentVariantTextArea.setToolTipText("<html><b>[variant_comments_private]</b><br>Private comments about the variant in all samples, visible only by you.</html>");
			commentVariantTextArea.setLineWrap(true);
			commentVariantTextArea.setWrapStyleWord(true);
			commentVariantTextArea.setRows(3);
			commentVariantTextArea.setText(val_private_comments_variant);
			JButton submitCommentsVariantButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 20));
			submitCommentsVariantButton.setToolTipText("Update private comment for this variant in the database");
			submitCommentsVariantButton.setPreferredSize(new Dimension(28,28));
			submitCommentsVariantButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.variant_comments_private,commentVariantTextArea.getText());
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});

			JButton shareCommentsVariantButton = new JButton(Resources.getScaledIcon(Resources.iUsers, 20));
			shareCommentsVariantButton.setToolTipText("Share private comment for this variant with another user");
			shareCommentsVariantButton.setPreferredSize(new Dimension(28,28));
			shareCommentsVariantButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						User[] users = User.fetchList().toArray((new User[0]));
						User user = (User)JOptionPane.showInputDialog(null, "Select the user with whom you want to share comments: ", 
								"Sharing comments from profile", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUsers, 64), users, null);
						if (user != null){
							String[] variant = variant_sample_id_to_variant_key.get(variantSampleId).split("-");
							int otherUserAnnotationId = -1;
							try (Results res = DB.select(Schema.HIGHLANDER, 
									"SELECT id "
									+ "FROM " + analysis.getFromUserAnnotationsVariantsPrivate()
									+ "WHERE "
									+ "chr = '"+variant[0]+"' AND "
									+ "pos = "+variant[1]+" AND "
									+ "length = "+variant[2]+" AND "
									+ "reference = '"+variant[3]+"' AND "
									+ "alternative = '"+variant[4]+"' AND "
									+ "gene_symbol = '"+val_gene_symbol+"' AND "
									+ "username = '"+user.getUsername()+"'")){
								if (res.next()){
									otherUserAnnotationId = res.getInt("id");
								}
							}

							String txt = "\nFrom " + Highlander.getLoggedUser().getUsername() + " : " + commentVariantTextArea.getText();
							if (otherUserAnnotationId < 0){
								otherUserAnnotationId = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
										"INSERT INTO " + analysis.getFromUserAnnotationsVariants()
												+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_comments`) " +
												"VALUES ('"+variant[0]+"', "+variant[1]+", "+variant[2]+", '"+variant[3]+"','"+variant[4]+"', '"+val_gene_symbol+"', '"+user.getUsername()+"', '')");
							}
							Highlander.getDB().update(Schema.HIGHLANDER, 
									"UPDATE " + analysis.getFromUserAnnotationsVariantsPrivate()
									+ "SET "+Field.variant_comments_private.getQueryWhereName(analysis, false)+" = CONCAT("+Field.variant_comments_private.getQueryWhereName(analysis, false)+",'"+Highlander.getDB().format(Schema.HIGHLANDER, txt)+"') "
									+ "WHERE id = " + otherUserAnnotationId);
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			JPanel commentsPanelVariant = new JPanel(new BorderLayout(0,0));
			commentsPanelVariant.setBackground(Resources.getColor(getColor(), 200, false));
			commentsPanelVariant.setToolTipText("<html><b>[variant_comments_private]</b><br>Private comments about the variant, visible only by you.</html>");
			commentsPanelVariant.setBorder(BorderFactory.createTitledBorder("variant_comments_private"));
			commentsPanelVariant.add(commentVariantTextArea, BorderLayout.CENTER);
			commentsPanelVariant.add(submitCommentsVariantButton, BorderLayout.WEST);
			commentsPanelVariant.add(shareCommentsVariantButton, BorderLayout.EAST);
			panel.add(commentsPanelVariant, new GridBagConstraints(0, y, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));
			y++;
			
			if (val_gene_symbol != null && val_gene_symbol.length() > 0){
				commentGeneTextArea.setToolTipText("<html><b>[gene_comments_private]</b><br>Private comments about the gene, visible only by you.</html>");
				commentGeneTextArea.setLineWrap(true);
				commentGeneTextArea.setWrapStyleWord(true);
				commentGeneTextArea.setRows(3);
				commentGeneTextArea.setText(val_private_comments_gene);
				JButton submitCommentsGeneButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 20));
				submitCommentsGeneButton.setToolTipText("Update private comment in the database");
				submitCommentsGeneButton.setPreferredSize(new Dimension(28,28));
				submitCommentsGeneButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try{
							updateDatabaseGene(Field.gene_comments_private,commentGeneTextArea.getText());
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				});
				JButton shareCommentsGeneButton = new JButton(Resources.getScaledIcon(Resources.iUsers, 20));
				shareCommentsGeneButton.setToolTipText("Share private comment for this gene with another user");
				shareCommentsGeneButton.setPreferredSize(new Dimension(28,28));
				shareCommentsGeneButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try{
							User[] users = User.fetchList().toArray((new User[0]));
							User user = (User)JOptionPane.showInputDialog(null, "Select the user with whom you want to share comments: ", 
									"Sharing comments from profile", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUsers, 64), users, null);
							if (user != null){
								int otherUserAnnotationId = -1;
								try (Results res = DB.select(Schema.HIGHLANDER, 
										"SELECT id "
										+ "FROM " + analysis.getFromUserAnnotationsGenesPrivate()
										+ "WHERE "
										+ "gene_symbol = '"+val_gene_symbol+"' AND "
										+ "username = '"+user.getUsername()+"'")){
									if (res.next()){
										otherUserAnnotationId = res.getInt("id");
									}
								}

								String txt = "\nFrom " + Highlander.getLoggedUser().getUsername() + " : " + commentGeneTextArea.getText();
								if (otherUserAnnotationId < 0){
									otherUserAnnotationId = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
											"INSERT INTO " + analysis.getFromUserAnnotationsGenes()
													+ "(`gene_symbol`, `username`, `gene_comments`) " +
													"VALUES ('"+val_gene_symbol+"', '"+user.getUsername()+"', '')");
								}
								Highlander.getDB().update(Schema.HIGHLANDER, 
										"UPDATE " + analysis.getFromUserAnnotationsGenesPrivate()
										+ "SET "+Field.gene_comments_private.getQueryWhereName(analysis, false)+" = CONCAT("+Field.gene_comments_private.getQueryWhereName(analysis, false)+",'"+Highlander.getDB().format(Schema.HIGHLANDER, txt)+"') "
										+ "WHERE id = " + otherUserAnnotationId);
							}
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				});
				JPanel commentsPanelGene = new JPanel(new BorderLayout(0,0));
				commentsPanelGene.setBackground(Resources.getColor(getColor(), 200, false));
				commentsPanelGene.setToolTipText("<html><b>[gene_comments_private]</b><br>Private comments about the gene, visible only by you.</html>");
				commentsPanelGene.setBorder(BorderFactory.createTitledBorder("gene_comments_private"));
				commentsPanelGene.add(commentGeneTextArea, BorderLayout.CENTER);
				commentsPanelGene.add(submitCommentsGeneButton, BorderLayout.WEST);
				commentsPanelGene.add(shareCommentsGeneButton, BorderLayout.EAST);
				panel.add(commentsPanelGene, new GridBagConstraints(0, y, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));
				y++;
			}

			commentSampleTextArea.setToolTipText("<html><b>[sample_comments_private]</b><br>Private comments about the sample, visible only by you.</html>");
			commentSampleTextArea.setLineWrap(true);
			commentSampleTextArea.setWrapStyleWord(true);
			commentSampleTextArea.setRows(3);
			commentSampleTextArea.setText(val_private_comments_sample);
			JButton submitCommentsSampleButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 20));
			submitCommentsSampleButton.setToolTipText("Update private comment in the database");
			submitCommentsSampleButton.setPreferredSize(new Dimension(28,28));
			submitCommentsSampleButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseSample(Field.sample_comments_private,commentSampleTextArea.getText());
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			JButton shareCommentsSampleButton = new JButton(Resources.getScaledIcon(Resources.iUsers, 20));
			shareCommentsSampleButton.setToolTipText("Share private comment for this sample with another user");
			shareCommentsSampleButton.setPreferredSize(new Dimension(28,28));
			shareCommentsSampleButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						User[] users = User.fetchList().toArray((new User[0]));
						User user = (User)JOptionPane.showInputDialog(null, "Select the user with whom you want to share comments: ", 
								"Sharing comments from profile", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUsers, 64), users, null);
						if (user != null){
							int otherUserAnnotationId = -1;
							try (Results res = DB.select(Schema.HIGHLANDER, 
									"SELECT id "
									+ "FROM " + analysis.getFromUserAnnotationsSamplesPrivate()
									+ "WHERE "
									+ "project_id = '"+variant_sample_id_to_project_id.get(variantSampleId)+"' AND "
									+ "username = '"+user.getUsername()+"'")){
								if (res.next()){
									otherUserAnnotationId = res.getInt("id");
								}
							}

							String txt = "\nFrom " + Highlander.getLoggedUser().getUsername() + " : " + commentSampleTextArea.getText();
							if (otherUserAnnotationId < 0){
								otherUserAnnotationId = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
										"INSERT INTO " + analysis.getFromUserAnnotationsSamplesPrivate()
												+ "(`project_id`, `username`, `sample_comments`) " +
												"VALUES ('"+variant_sample_id_to_project_id.get(variantSampleId)+"', '"+user.getUsername()+"', '')");
							}
							Highlander.getDB().update(Schema.HIGHLANDER, 
									"UPDATE " + analysis.getFromUserAnnotationsSamplesPrivate()
									+ "SET "+Field.sample_comments_private.getQueryWhereName(analysis, false)+" = CONCAT("+Field.sample_comments_private.getQueryWhereName(analysis, false)+",'"+Highlander.getDB().format(Schema.HIGHLANDER, txt)+"') "
									+ "WHERE id = " + otherUserAnnotationId);
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPrivateAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			JPanel commentsPanelSample = new JPanel(new BorderLayout(0,0));
			commentsPanelSample.setBackground(Resources.getColor(getColor(), 200, false));
			commentsPanelSample.setToolTipText("<html><b>[sample_comments_private]</b><br>Private comments about the sample, visible only by you.</html>");
			commentsPanelSample.setBorder(BorderFactory.createTitledBorder("sample_comments_private"));
			commentsPanelSample.add(commentSampleTextArea, BorderLayout.CENTER);
			commentsPanelSample.add(submitCommentsSampleButton, BorderLayout.WEST);
			commentsPanelSample.add(shareCommentsSampleButton, BorderLayout.EAST);
			panel.add(commentsPanelSample, new GridBagConstraints(0, y, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));
			y++;
			
			detailsPanel.removeAll();
			detailsPanel.add(panel, BorderLayout.CENTER);
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

}
