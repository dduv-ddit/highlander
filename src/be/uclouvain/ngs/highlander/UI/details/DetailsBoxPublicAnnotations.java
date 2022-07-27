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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
 * Public user annotations, visible and modifiable by any user
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxPublicAnnotations extends DetailsBox {

	private int variantAnnotationId = -1;
	private int geneAnnotationId = -1;
	private int sampleAnnotationId = -1;

	private VariantsTable table;
	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;

	private JTextArea commentVariantTextArea = new JTextArea();
	private JTextArea commentGeneTextArea = new JTextArea();
	private JTextArea commentSampleTextArea = new JTextArea();
	
	private String val_chr;
	private int val_pos;
	private int val_length;
	private String val_reference;
	private String val_alternative;
	private String val_gene_symbol;
	private int val_project_id;
	private String val_sample;

	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	public DetailsBoxPublicAnnotations(int variantId, DetailsPanel mainPanel, VariantsTable table){
		this.variantSampleId = variantId;
		this.mainPanel = mainPanel;
		this.table = table;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	public String getTitle(){
		return "Public annotations";
	}

	public Palette getColor() {
		return Field.variant_comments_public.getCategory().getColor();
	}

	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	protected void loadDetails(){
		try{
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			String val_public_comments_variant = null;
			String val_public_comments_gene = null;
			String val_public_comments_sample = null;
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT "
							+ analysis.getFromUserAnnotationsVariants().replace("` ","_public`")+".`id` as var_annot_id, "
							+ analysis.getFromUserAnnotationsGenes().replace("` ","_public`")+".`id` as gene_annot_id, "
							+ analysis.getFromUserAnnotationsSamples().replace("` ","_public`")+".`id` as sample_annot_id, "
							+ Field.variant_comments_public.getQuerySelectName(analysis, true)+", "
							+ Field.gene_comments_public.getQuerySelectName(analysis, true)+", "
							+ Field.sample_comments_public.getQuerySelectName(analysis, true)+", "
							+ Field.chr.getQuerySelectName(analysis, true)+", "+Field.pos.getQuerySelectName(analysis, true)+", "+Field.length.getQuerySelectName(analysis, true)+", "
							+ Field.reference.getQuerySelectName(analysis, true)+", "+Field.alternative.getQuerySelectName(analysis, true)+", "+Field.gene_symbol.getQuerySelectName(analysis, true)+", "
							+ Field.project_id.getQuerySelectName(analysis, true) + ", " + Field.sample.getQuerySelectName(analysis, true)+", "
							+ Field.variant_sample_id.getQuerySelectName(analysis, true) + " "
							+	"FROM " + analysis.getFromSampleAnnotations()
							+ analysis.getJoinStaticAnnotations()
							+ analysis.getJoinProjects()
							+ analysis.getJoinGeneAnnotations()
							+ analysis.getJoinUserAnnotationsVariantsPublic()
							+ analysis.getJoinUserAnnotationsGenesPublic()
							+ analysis.getJoinUserAnnotationsSamplesPublic()
							+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, true)+" = " + variantSampleId)){
				if (res.next()){
					val_chr = res.getString(Field.chr.getName());						
					val_pos = res.getInt(Field.pos.getName());						
					val_length = res.getInt(Field.length.getName());						
					val_reference = res.getString(Field.reference.getName());						
					val_alternative = res.getString(Field.alternative.getName());						
					val_gene_symbol = res.getString(Field.gene_symbol.getName());						
					val_project_id = res.getInt(Field.project_id.getName());						
					val_sample = res.getString(Field.sample.getName());						
					if (res.getObject("var_annot_id") != null) {
						variantAnnotationId = res.getInt("var_annot_id");
						val_public_comments_variant = res.getString(Field.variant_comments_public.getName());
					}
					if (res.getObject("gene_annot_id") != null) {
						geneAnnotationId = res.getInt("gene_annot_id");
						val_public_comments_gene = res.getString(Field.gene_comments_public.getName());
					}
					if (res.getObject("sample_annot_id") != null) {
						sampleAnnotationId = res.getInt("sample_annot_id");
						val_public_comments_sample = res.getString(Field.sample_comments_public.getName());
					}
				}
			}

			JPanel panel = new JPanel(new GridBagLayout());
			int y=0;
			
			commentVariantTextArea.setToolTipText(Field.variant_comments_public.getHtmlTooltip());
			commentVariantTextArea.setLineWrap(true);
			commentVariantTextArea.setWrapStyleWord(true);
			commentVariantTextArea.setRows(3);
			commentVariantTextArea.setText(val_public_comments_variant);
			
			JButton submitCommentsButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 20));
			submitCommentsButton.setToolTipText("Update public comment on variant in the database");
			submitCommentsButton.setPreferredSize(new Dimension(28,28));
			submitCommentsButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						String txt = commentVariantTextArea.getText();
						int i = txt.indexOf("\nLast modified by "); 
						if (i >= 0){
							String newTxt = txt.substring(0, i);
							int j = txt.indexOf(".", i);
							if (j >= 0 && j+1 < txt.length()){
								newTxt += txt.substring(j+1);
							}
							txt = newTxt;
						}
						txt += "\nLast modified by " + Highlander.getLoggedUser().getUsername() + " on "+df.format(System.currentTimeMillis())+".";
						if (variantAnnotationId >= 0){
							Highlander.getDB().update(Schema.HIGHLANDER, 
									"UPDATE " + analysis.getFromUserAnnotationsVariantsPublic()
									+ "SET "+Field.variant_comments_public.getQueryWhereName(analysis, false)+" = '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "' "
									+ "WHERE id = " + variantAnnotationId);		
						}else{
							variantAnnotationId = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
									"INSERT INTO " + analysis.getFromUserAnnotationsVariants()
									+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_comments`) " +
									"VALUES ('"+val_chr+"', "+val_pos+", "+val_length+", '"+val_reference+"','"+val_alternative+"', '"+val_gene_symbol+"', 'PUBLIC', '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "')");
						}
						table.updateAnnotation(variantSampleId, Field.variant_comments_public, txt);
						table.updateAnnotation(val_chr, val_pos, val_length, val_reference, val_alternative, val_gene_symbol, Field.variant_comments_public, txt);
						commentVariantTextArea.setText(txt);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPublicAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			JPanel commentsPanel = new JPanel(new BorderLayout(0,0));
			commentsPanel.setBackground(Resources.getColor(getColor(), 200, false));
			commentsPanel.setToolTipText(Field.variant_comments_public.getHtmlTooltip());
			commentsPanel.setBorder(BorderFactory.createTitledBorder(Field.variant_comments_public.getName()));
			commentsPanel.add(commentVariantTextArea, BorderLayout.CENTER);
			commentsPanel.add(submitCommentsButton, BorderLayout.WEST);
			panel.add(commentsPanel, new GridBagConstraints(0, y++, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));

			if (val_gene_symbol != null && val_gene_symbol.length() > 0){	
				commentGeneTextArea.setToolTipText(Field.gene_comments_public.getHtmlTooltip());
				commentGeneTextArea.setLineWrap(true);
				commentGeneTextArea.setWrapStyleWord(true);
				commentGeneTextArea.setRows(3);
				commentGeneTextArea.setText(val_public_comments_gene);
				
				JButton submitGeneCommentsButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 20));
				submitGeneCommentsButton.setToolTipText("Update public comment on gene in the database");
				submitGeneCommentsButton.setPreferredSize(new Dimension(28,28));
				submitGeneCommentsButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try{
							String txt = commentGeneTextArea.getText();
							int i = txt.indexOf("\nLast modified by "); 
							if (i >= 0){
								String newTxt = txt.substring(0, i);
								int j = txt.indexOf(".", i);
								if (j >= 0 && j+1 < txt.length()){
									newTxt += txt.substring(j+1);
								}
								txt = newTxt;
							}
							txt += "\nLast modified by " + Highlander.getLoggedUser().getUsername() + " on "+df.format(System.currentTimeMillis())+".";
							if (geneAnnotationId >= 0){
								Highlander.getDB().update(Schema.HIGHLANDER, 
										"UPDATE " + analysis.getFromUserAnnotationsGenesPublic()
										+ "SET "+Field.gene_comments_public.getQueryWhereName(analysis, false)+" = '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "' "
										+ "WHERE id = " + geneAnnotationId);		
							}else{
								geneAnnotationId = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
										"INSERT INTO " + analysis.getFromUserAnnotationsGenes()
										+ "(`gene_symbol`, `username`, `gene_comments`) " +
										"VALUES ('"+val_gene_symbol+"', 'PUBLIC', '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "')");
							}
							table.updateAnnotation(variantSampleId, Field.gene_comments_public, txt);
							table.updateAnnotation(Field.gene_symbol, val_gene_symbol, Field.gene_comments_public, txt);
							commentGeneTextArea.setText(txt);
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(DetailsBoxPublicAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}
				});
				JPanel geneCommentsPanel = new JPanel(new BorderLayout(0,0));
				geneCommentsPanel.setBackground(Resources.getColor(getColor(), 200, false));
				geneCommentsPanel.setToolTipText(Field.gene_comments_public.getHtmlTooltip());
				geneCommentsPanel.setBorder(BorderFactory.createTitledBorder(Field.gene_comments_public.getName()));
				geneCommentsPanel.add(commentGeneTextArea, BorderLayout.CENTER);
				geneCommentsPanel.add(submitGeneCommentsButton, BorderLayout.WEST);
				panel.add(geneCommentsPanel, new GridBagConstraints(0, y++, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));
			}
			
			commentSampleTextArea.setToolTipText(Field.sample_comments_public.getHtmlTooltip());
			commentSampleTextArea.setLineWrap(true);
			commentSampleTextArea.setWrapStyleWord(true);
			commentSampleTextArea.setRows(3);
			commentSampleTextArea.setText(val_public_comments_sample);
			
			JButton submitSampleCommentsButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 20));
			submitSampleCommentsButton.setToolTipText("Update public comment on sample in the database");
			submitSampleCommentsButton.setPreferredSize(new Dimension(28,28));
			submitSampleCommentsButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						String txt = commentSampleTextArea.getText();
						int i = txt.indexOf("\nLast modified by "); 
						if (i >= 0){
							String newTxt = txt.substring(0, i);
							int j = txt.indexOf(".", i);
							if (j >= 0 && j+1 < txt.length()){
								newTxt += txt.substring(j+1);
							}
							txt = newTxt;
						}
						txt += "\nLast modified by " + Highlander.getLoggedUser().getUsername() + " on "+df.format(System.currentTimeMillis())+".";
						if (sampleAnnotationId >= 0){
							Highlander.getDB().update(Schema.HIGHLANDER, 
									"UPDATE " + analysis.getFromUserAnnotationsSamplesPublic()
									+ "SET "+Field.sample_comments_public.getQueryWhereName(analysis, false)+" = '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "' "
									+ "WHERE id = " + sampleAnnotationId);		
						}else{
							sampleAnnotationId = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
									"INSERT INTO " + analysis.getFromUserAnnotationsSamples()
									+ "(`project_id`, `username`, `sample_comments`) " +
									"VALUES ("+val_project_id+", 'PUBLIC', '"+ Highlander.getDB().format(Schema.HIGHLANDER, txt) + "')");
						}
						table.updateAnnotation(variantSampleId, Field.sample_comments_public, txt);
						table.updateAnnotation(Field.project_id, val_project_id, Field.sample_comments_public, txt);
						table.updateAnnotation(Field.sample, val_sample, Field.sample_comments_public, txt);
						commentSampleTextArea.setText(txt);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxPublicAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			JPanel sampleCommentsPanel = new JPanel(new BorderLayout(0,0));
			sampleCommentsPanel.setToolTipText(Field.sample_comments_public.getHtmlTooltip());
			sampleCommentsPanel.setBorder(BorderFactory.createTitledBorder(Field.sample_comments_public.getName()));
			sampleCommentsPanel.add(commentSampleTextArea, BorderLayout.CENTER);
			sampleCommentsPanel.add(submitSampleCommentsButton, BorderLayout.WEST);
			sampleCommentsPanel.setBackground(Resources.getColor(getColor(), 200, false));
			panel.add(sampleCommentsPanel, new GridBagConstraints(0, y++, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));

			panel.setBackground(Resources.getColor(getColor(), 200, false));
			
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
