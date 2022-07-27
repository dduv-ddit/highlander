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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.Insilico;
import be.uclouvain.ngs.highlander.database.Field.Mosaicism;
import be.uclouvain.ngs.highlander.database.Field.Reporting;
import be.uclouvain.ngs.highlander.database.Field.Segregation;
import be.uclouvain.ngs.highlander.database.Field.Validation;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
 * User annotations, visible by all but modifiable only by users with permission on this sample
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxEvaluationAnnotations extends DetailsBox {

	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private VariantsTable table;
	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;

	private JButton statusEvaluation = new JButton();
	private JLabel userEvaluation = new JLabel("");
	private JButton statusInsilico = new JButton();
	private JLabel userInsilico = new JLabel("");
	private JButton statusReporting = new JButton();
	private JLabel userReporting = new JLabel("");
	private JButton statusValidated = new JButton();
	private JLabel userValidated = new JLabel("");
	private JButton statusSomatic = new JButton();
	private JLabel userSomatic = new JLabel("");
	private JButton statusSegregation = new JButton();
	private JLabel userSegregation = new JLabel("");
	private JPanel commentsPanel = new JPanel(new BorderLayout(0,0));
	private JTextArea commentTextArea = new JTextArea();
	private JTextArea historyTextArea = new JTextArea();

	private Map<Integer, Integer> var_id_to_annotation_id = new HashMap<Integer,Integer>();
	private Map<Integer, Integer> var_id_to_num_evaluation_id = new HashMap<Integer,Integer>();

	public DetailsBoxEvaluationAnnotations(List<Integer> variantIds, DetailsPanel mainPanel, VariantsTable table){
		for (int id : variantIds){
			var_id_to_annotation_id.put(id, -1);
			var_id_to_num_evaluation_id.put(id, -1);
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
		return "Variant evaluation";
	}

	public Palette getColor() {
		return Field.evaluation.getCategory().getColor();
	}

	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	private void set_Evaluation(String val, String user, String date, boolean newEvaluation){
		if (val != null && val.equals("1")){
			statusEvaluation.setIcon(Resources.getScaledIcon(Resources.iRoman1, 24));
			statusEvaluation.setToolTipText("Type I: Benign - Polymorphism");
			userEvaluation.setText("("+user+" - "+date+")");			
			if (newEvaluation){
				for (int id : var_id_to_annotation_id.keySet()) table.updateAnnotation(id, Field.evaluation, 1);
				historyTextArea.append("\nEvaluation set to type I ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
			}
		}else if (val != null && val.equals("2")){
			statusEvaluation.setIcon(Resources.getScaledIcon(Resources.iRoman2, 24));
			statusEvaluation.setToolTipText("Type II: Variant Likely Benign");
			userEvaluation.setText("("+user+" - "+date+")");
			if (newEvaluation){		
				for (int id : var_id_to_annotation_id.keySet()) table.updateAnnotation(id, Field.evaluation, 2);
				historyTextArea.append("\nEvaluation set to type II ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
			}
		}else if (val != null && val.equals("3")){
			statusEvaluation.setIcon(Resources.getScaledIcon(Resources.iRoman3, 24));
			statusEvaluation.setToolTipText("Type III: Variant of Unknown Significance");
			userEvaluation.setText("("+user+" - "+date+")");			
			if (newEvaluation){
				for (int id : var_id_to_annotation_id.keySet()) table.updateAnnotation(id, Field.evaluation, 3);
				historyTextArea.append("\nEvaluation set to type III ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
			}
		}else if (val != null && val.equals("4")){
			statusEvaluation.setIcon(Resources.getScaledIcon(Resources.iRoman4, 24));
			statusEvaluation.setToolTipText("Type IV: Variant Likely Pathogenic");
			userEvaluation.setText("("+user+" - "+date+")");			
			if (newEvaluation){
				for (int id : var_id_to_annotation_id.keySet()) table.updateAnnotation(id, Field.evaluation, 4);
				historyTextArea.append("\nEvaluation set to type IV ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
			}
		}else if (val != null && val.equals("5")){
			statusEvaluation.setIcon(Resources.getScaledIcon(Resources.iRoman5, 24));
			statusEvaluation.setToolTipText("Type V: Pathogenic Mutation");
			userEvaluation.setText("("+user+" - "+date+")");			
			if (newEvaluation){
				for (int id : var_id_to_annotation_id.keySet()) table.updateAnnotation(id, Field.evaluation, 5);
				historyTextArea.append("\nEvaluation set to type V ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
			}
		}else{
			statusEvaluation.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusEvaluation.setToolTipText("The variant has not been evaluated");
			userEvaluation.setText("");			
			if (newEvaluation){
				for (int id : var_id_to_annotation_id.keySet()) table.updateAnnotation(id, Field.evaluation, 0);
				historyTextArea.append("\nEvaluation set to not evaluated ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
			}
		}
	}

	private void set_check_insilico(Insilico val, String user, String date, boolean newEvaluation){
		switch(val){
		case OK:
			statusInsilico.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusInsilico.setToolTipText("OK - Variant has been checked insilico (e.g. by looking at the alignment) and it's likely a real variant");
			userInsilico.setText("("+user+" - "+date+")");			
			break;
		case NOT_OK:
			statusInsilico.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusInsilico.setToolTipText("NOT_OK - Variant has been checked insilico (e.g. by looking at the alignment) and it's likely a sequencing error (e.g. alternative allele not specific to pathology)");
			userInsilico.setText("("+user+" - "+date+")");			
			break;
		case SUSPECT:
			statusInsilico.setIcon(Resources.getScaledIcon(Resources.iAttention, 24));
			statusInsilico.setToolTipText("SUSPECT - the variant has been checked insilico (e.g. by looking at the alignment) but not sure if it's real or not");
			userInsilico.setText("("+user+" - "+date+")");			
			break;
		case NOT_CHECKED:
		default:
			statusInsilico.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusInsilico.setToolTipText("NOT_CHECKED - Variant has not been checked insilico (e.g. by looking at the alignment)");
			userInsilico.setText("");			
			break;		
		}
		if (newEvaluation){
			historyTextArea.append("\ncheck_insilico set to "+val+" ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
		}
	}

	private void set_reporting(Reporting val, String user, String date, boolean newEvaluation){
		switch(val){
		case YES:
			statusReporting.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusReporting.setToolTipText("YES - Variant has been checked and needs to be reported to the patient");
			userReporting.setText("("+user+" - "+date+")");			
			break;
		case NO:
			statusReporting.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusReporting.setToolTipText("NO - Variant has been checked and doesn’t need to be reported to the patient");
			userReporting.setText("("+user+" - "+date+")");			
			break;
		case NOT_CHECKED:
		default:
			statusReporting.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusReporting.setToolTipText("NOT_CHECKED - Variant has not been checked");
			userReporting.setText("");			
			break;		
		}
		if (newEvaluation){
			historyTextArea.append("\reporting set to "+val+" ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
		}
	}
	
	private void set_check_validated_variant(Validation val, String user, String date, boolean newEvaluation){
		switch(val){
		case VALIDATED:
			statusValidated.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusValidated.setToolTipText("VALIDATED - Variant has been confirmed with another lab technique (e.g. by Sanger sequencing)");
			userValidated.setText("("+user+" - "+date+")");			
			break;
		case INVALIDATED:
			statusValidated.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusValidated.setToolTipText("INVALIDATED - Variant has been tested with another lab technique (e.g. by Sanger sequencing) and was NOT found");
			userValidated.setText("("+user+" - "+date+")");			
			break;
		case SUSPECT:
			statusValidated.setIcon(Resources.getScaledIcon(Resources.iAttention, 24));
			statusValidated.setToolTipText("SUSPECT - the variant has been checked with another technique, but cannot be confirmed or invalidate");
			userValidated.setText("("+user+" - "+date+")");			
			break;
		case NOT_CHECKED:
		default:
			statusValidated.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusValidated.setToolTipText("NOT_CHECKED - Variant has not been tested with another lab technique (e.g. by Sanger sequencing)");
			userValidated.setText("");			
			break;		
		}
		if (newEvaluation){
			historyTextArea.append("\ncheck_validated set to "+val+" ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
		}
	}

	private void set_check_somatic_variant(Mosaicism val, String user, String date, boolean newEvaluation){
		switch(val){
		case SOMATIC:
			statusSomatic.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusSomatic.setToolTipText("SOMATIC - Variant has been checked for mosaicism and seems to be a somatic variant");
			userSomatic.setText("("+user+" - "+date+")");			
			break;
		case GERMLINE:
			statusSomatic.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusSomatic.setToolTipText("GERMLINE - Variant has been checked for mosaicism and seems to be a germline variant");
			userSomatic.setText("("+user+" - "+date+")");			
			break;
		case DUBIOUS:
			statusSomatic.setIcon(Resources.getScaledIcon(Resources.iAttention, 24));
			statusSomatic.setToolTipText("DUBIOUS - Variant has been checked for mosaicism but was impossible to differenciate between somatic or germline");
			userSomatic.setText("("+user+" - "+date+")");			
			break;
		case NOT_CHECKED:
		default:
			statusSomatic.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusSomatic.setToolTipText("NOT_CHECKED - Not checked for mosaicism");
			userSomatic.setText("");			
			break;		
		}
		if (newEvaluation){
			historyTextArea.append("\ncheck_somatic_variant set to "+val+" ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
		}
	}

	private void set_check_segregation(Segregation val, String user, String date, boolean newEvaluation){
		switch(val){
		case CARRIERS:
			statusSegregation.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusSegregation.setToolTipText("CARRIERS - Some unaffected carrier(s)");
			userSegregation.setText("("+user+" - "+date+")");			
			break;
		case COSEG:
			statusSegregation.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
			statusSegregation.setToolTipText("COSEG - Variant cosegregates");
			userSegregation.setText("("+user+" - "+date+")");			
			break;
		case NO_COSEG:
			statusSegregation.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			statusSegregation.setToolTipText("NO_COSEG - Not in other affected(s)");
			userSegregation.setText("("+user+" - "+date+")");			
			break;
		case NO_COSEG_OTHER:
			statusSegregation.setIcon(Resources.getScaledIcon(Resources.i2dMinus, 24));
			statusSegregation.setToolTipText("NO_COSEG_OTHER - Does not cosegregate in other families");
			userSegregation.setText("("+user+" - "+date+")");			
			break;
		case SINGLE:
			statusSegregation.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusSegregation.setToolTipText("SINGLE - No other sample in family");
			userSegregation.setText("("+user+" - "+date+")");			
			break;
		case NOT_CHECKED:
		default:
			statusSegregation.setIcon(Resources.getScaledIcon(Resources.iQuestion, 24));
			statusSegregation.setToolTipText("NOT_CHECKED - Not checked for segregation");
			userSegregation.setText("");			
			break;		
		}
		if (newEvaluation){
			historyTextArea.append("\ncheck_segregation set to "+val+" ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
		}
	}

	private void set_evaluation_comments(){
		commentsPanel.setBorder(BorderFactory.createTitledBorder(Field.evaluation_comments.getName() + " ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")"));
		historyTextArea.append("\nevaluation_comments modified to \'"+Highlander.getDB().format(Schema.HIGHLANDER, commentTextArea.getText())+"\' ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")");
	}

	private void updateDatabaseVariant(Field field, Object value, String historyAddUp) throws Exception {
		AnalysisFull analysis = Highlander.getCurrentAnalysis();
		for (int var_id : var_id_to_annotation_id.keySet()){
			if (var_id_to_annotation_id.get(var_id) < 0){
				int project_id = -1;
				String chr = "";
				int pos = 0;
				int length = 0;
				String reference = "";
				String alternative = "";
				String gene_symbol = "";
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT chr, pos, length, reference, alternative, gene_symbol, project_id, id "
						+ "FROM " + analysis.getFromSampleAnnotations() 
						+ analysis.getJoinUserAnnotationsNumEvaluations()
						+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + var_id)){
					if (res.next()){
						chr = res.getString("chr");
						pos = res.getInt("pos");
						length = res.getInt("length");
						reference = res.getString("reference");
						alternative = res.getString("alternative");
						gene_symbol = res.getString("gene_symbol");
						project_id = res.getInt("project_id");
						if (res.getObject("id") != null) {
							var_id_to_num_evaluation_id.put(var_id, res.getInt("id"));
						}
					}
				}
				int annotation_id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
						"INSERT INTO " + analysis.getFromUserAnnotationsEvaluations()
								+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `project_id`, `evaluation_comments`, `history`) " +
								"VALUES ('"+chr+"', "+pos+", "+length+", '"+reference+"','"+alternative+"', '"+gene_symbol+"', "+project_id+", '', '')");
				var_id_to_annotation_id.put(var_id, annotation_id);

				if (var_id_to_num_evaluation_id.get(var_id) < 0){
					int num_evaluation_id = Highlander.getDB().insertAndGetAutoId(Schema.HIGHLANDER, 
							"INSERT INTO " + analysis.getFromUserAnnotationsNumEvaluations()
									+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `num_evaluated_as_type_1`, `num_evaluated_as_type_2`, `num_evaluated_as_type_3`, `num_evaluated_as_type_4`, `num_evaluated_as_type_5`) " +
									"VALUES ('"+chr+"', "+pos+", "+length+", '"+reference+"','"+alternative+"', '"+gene_symbol+"', 0, 0, 0, 0, 0)");
					var_id_to_num_evaluation_id.put(var_id, num_evaluation_id);					
				}
			}
		}
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"UPDATE " + analysis.getFromUserAnnotationsEvaluations()
						+ "SET "+field+" = "+(value != null ? "'"+Highlander.getDB().format(Schema.HIGHLANDER, value.toString())+"'" : "NULL") + ", "
						+ field+"_username = '"+Highlander.getLoggedUser().getUsername()+"', "
						+ field+"_date = NOW(), "
						+ "history = CONCAT(history,'"+historyAddUp+"') "
						+ "WHERE id IN (" + HighlanderDatabase.makeSqlList(new HashSet<Integer>(var_id_to_annotation_id.values()), Integer.class) + ")");
		if (field.equals(Field.evaluation)) {
			for (int id : var_id_to_num_evaluation_id.keySet()) {
				try(Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT `chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, "
								+ "SUM(IF(`evaluation` = 1, 1, 0)) as `"+Field.num_evaluated_as_type_1+"`, "
								+ "SUM(IF(`evaluation` = 2, 1, 0)) as `"+Field.num_evaluated_as_type_2+"`, "
								+ "SUM(IF(`evaluation` = 3, 1, 0)) as `"+Field.num_evaluated_as_type_3+"`, "
								+ "SUM(IF(`evaluation` = 4, 1, 0)) as `"+Field.num_evaluated_as_type_4+"`, "
								+ "SUM(IF(`evaluation` = 5, 1, 0)) as `"+Field.num_evaluated_as_type_5+"` "
								+ "FROM " + analysis.getFromUserAnnotationsEvaluations()
								+ "JOIN ("
								+ "SELECT `chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol` "
								+ "FROM " + analysis.getFromUserAnnotationsEvaluations()
								+ "WHERE id = " + var_id_to_annotation_id.get(id)
								+ ") as tmp USING (`chr`, `pos`, `reference`, `length`, `alternative`, `gene_symbol`) "
								+ "GROUP BY `chr`, `pos`, `reference`, `length`, `alternative`, `gene_symbol`"
						)){
					if (res.next()) {
						Highlander.getDB().update(Schema.HIGHLANDER, 
								"UPDATE " + analysis.getFromUserAnnotationsNumEvaluations()
										+ "SET `"+Field.num_evaluated_as_type_1+"` = " + res.getInt(Field.num_evaluated_as_type_1.toString()) + ", "
										+ "`"+Field.num_evaluated_as_type_2+"` = " + res.getInt(Field.num_evaluated_as_type_2.toString()) + ", "
										+ "`"+Field.num_evaluated_as_type_3+"` = " + res.getInt(Field.num_evaluated_as_type_3.toString()) + ", "
										+ "`"+Field.num_evaluated_as_type_4+"` = " + res.getInt(Field.num_evaluated_as_type_4.toString()) + ", "
										+ "`"+Field.num_evaluated_as_type_5+"` = " + res.getInt(Field.num_evaluated_as_type_5.toString()) + " "
										+ "WHERE id = " + var_id_to_num_evaluation_id.get(id));
						table.updateAnnotation(res.getString("chr"), res.getInt("pos"), res.getInt("length"), res.getString("reference"), 
								res.getString("alternative"), res.getString("gene_symbol"), Field.num_evaluated_as_type_1, res.getInt(Field.num_evaluated_as_type_1.toString()));
						table.updateAnnotation(res.getString("chr"), res.getInt("pos"), res.getInt("length"), res.getString("reference"), 
								res.getString("alternative"), res.getString("gene_symbol"), Field.num_evaluated_as_type_2, res.getInt(Field.num_evaluated_as_type_2.toString()));
						table.updateAnnotation(res.getString("chr"), res.getInt("pos"), res.getInt("length"), res.getString("reference"), 
								res.getString("alternative"), res.getString("gene_symbol"), Field.num_evaluated_as_type_3, res.getInt(Field.num_evaluated_as_type_3.toString()));
						table.updateAnnotation(res.getString("chr"), res.getInt("pos"), res.getInt("length"), res.getString("reference"), 
								res.getString("alternative"), res.getString("gene_symbol"), Field.num_evaluated_as_type_4, res.getInt(Field.num_evaluated_as_type_4.toString()));
						table.updateAnnotation(res.getString("chr"), res.getInt("pos"), res.getInt("length"), res.getString("reference"), 
								res.getString("alternative"), res.getString("gene_symbol"), Field.num_evaluated_as_type_5, res.getInt(Field.num_evaluated_as_type_5.toString()));
					}
				}
			}			
		}
		for (int id : var_id_to_annotation_id.keySet()) {
			table.updateAnnotation(id, field, value);
			table.updateAnnotation(id, Field.getField(field.getName()+"_username"), Highlander.getLoggedUser().getUsername());
			table.updateAnnotation(id, Field.getField(field.getName()+"_date"), df.format(System.currentTimeMillis()));
			table.updateAnnotation(id, Field.history, historyTextArea.getText());
		}
	}

	protected void loadDetails(){
		try{
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			String val_evaluation = "0";
			String val_evaluation_username = null;
			String val_evaluation_date = null;
			Insilico val_check_insilico = Insilico.NOT_CHECKED;
			String val_check_insilico_username = null;
			String val_check_insilico_date = null;
			Reporting val_reporting = Reporting.NOT_CHECKED;
			String val_reporting_username = null;
			String val_reporting_date = null;
			Validation val_check_validated_variant = Validation.NOT_CHECKED;
			String val_check_validated_variant_username = null;
			String val_check_validated_variant_date = null;
			Mosaicism val_check_somatic_variant = Mosaicism.NOT_CHECKED;
			String val_check_somatic_variant_username = null;
			String val_check_somatic_variant_date = null;
			Segregation val_check_segregation = Segregation.NOT_CHECKED;
			String val_check_segregation_username = null;
			String val_check_segregation_date = null;
			String val_evaluation_comments = null;
			String val_evaluation_comments_username = null;
			String val_evaluation_comments_date = null;
			String val_history = null;
			int val_num_evaluated_as_type_1 = 0;
			int val_num_evaluated_as_type_2 = 0;
			int val_num_evaluated_as_type_3 = 0;
			int val_num_evaluated_as_type_4 = 0;
			int val_num_evaluated_as_type_5 = 0;
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT "
							+ analysis.getFromUserAnnotationsEvaluations().trim()+".`id` as annot_id, "
							+ analysis.getFromUserAnnotationsNumEvaluations().trim()+".`id` as num_eval_id, "
							+ Field.variant_sample_id.getQuerySelectName(analysis, false)+", "
							+ Field.evaluation.getQuerySelectName(analysis, false)+", "+Field.evaluation_username.getQuerySelectName(analysis, false)+", "+Field.evaluation_date.getQuerySelectName(analysis, false)+", "
							+ Field.check_insilico.getQuerySelectName(analysis, false)+", "+Field.check_insilico_username.getQuerySelectName(analysis, false)+", "+Field.check_insilico_date.getQuerySelectName(analysis, false)+", "
							+ Field.reporting.getQuerySelectName(analysis, false)+", "+Field.reporting_username.getQuerySelectName(analysis, false)+", "+Field.reporting_date.getQuerySelectName(analysis, false)+", "
							+ Field.check_validated_variant.getQuerySelectName(analysis, false)+", "+Field.check_validated_variant_username.getQuerySelectName(analysis, false)+", "+Field.check_validated_variant_date.getQuerySelectName(analysis, false)+", "
							+ Field.check_somatic_variant.getQuerySelectName(analysis, false)+", "+Field.check_somatic_variant_username.getQuerySelectName(analysis, false)+", "+Field.check_somatic_variant_date.getQuerySelectName(analysis, false)+", "
							+ Field.check_segregation.getQuerySelectName(analysis, false)+", "+Field.check_segregation_username.getQuerySelectName(analysis, false)+", "+Field.check_segregation_date.getQuerySelectName(analysis, false)+", "
							+ Field.evaluation_comments.getQuerySelectName(analysis, false)+", "+Field.evaluation_comments_username.getQuerySelectName(analysis, false)+", "+Field.evaluation_comments_date.getQuerySelectName(analysis, false)+", "
							+ Field.history.getQuerySelectName(analysis, false)+", "
							+ Field.num_evaluated_as_type_1.getQuerySelectName(analysis, false)+", "+Field.num_evaluated_as_type_2.getQuerySelectName(analysis, false)+", "+Field.num_evaluated_as_type_3.getQuerySelectName(analysis, false)+", "+Field.num_evaluated_as_type_4.getQuerySelectName(analysis, false)+", "+Field.num_evaluated_as_type_5.getQuerySelectName(analysis, false)
							+	"FROM " + analysis.getFromSampleAnnotations()
							+ analysis.getJoinUserAnnotationsEvaluations()
							+ analysis.getJoinUserAnnotationsNumEvaluations()
							+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN (" + HighlanderDatabase.makeSqlList(var_id_to_annotation_id.keySet(), Integer.class) + ")")){
				while (res.next()){
					int var_id = res.getInt(Field.variant_sample_id.getName());
					if (res.getObject("annot_id") != null) {
						var_id_to_annotation_id.put(var_id, res.getInt("annot_id"));
						if (var_id == variantSampleId){
							val_evaluation = res.getString(Field.evaluation.getName());
							val_evaluation_username = res.getString(Field.evaluation_username.getName());
							val_evaluation_date = df.format(res.getTimestamp(Field.evaluation_date.getName()));
							val_check_insilico = Insilico.valueOf(res.getString(Field.check_insilico.getName()));
							val_check_insilico_username = res.getString(Field.check_insilico_username.getName());
							val_check_insilico_date = df.format(res.getTimestamp(Field.check_insilico_date.getName()));
							val_reporting = Reporting.valueOf(res.getString(Field.reporting.getName()));
							val_reporting_username = res.getString(Field.reporting_username.getName());
							val_reporting_date = df.format(res.getTimestamp(Field.reporting_date.getName()));
							val_check_validated_variant = Validation.valueOf(res.getString(Field.check_validated_variant.getName()));
							val_check_validated_variant_username = res.getString(Field.check_validated_variant_username.getName());
							val_check_validated_variant_date = df.format(res.getTimestamp(Field.check_validated_variant_date.getName()));
							val_check_somatic_variant = Mosaicism.valueOf(res.getString(Field.check_somatic_variant.getName()));
							val_check_somatic_variant_username = res.getString(Field.check_somatic_variant_username.getName());
							val_check_somatic_variant_date = df.format(res.getTimestamp(Field.check_somatic_variant_date.getName()));
							val_check_segregation = Segregation.valueOf(res.getString(Field.check_segregation.getName()));
							val_check_segregation_username = res.getString(Field.check_segregation_username.getName());
							val_check_segregation_date = df.format(res.getTimestamp(Field.check_segregation_date.getName()));
							val_evaluation_comments = res.getString(Field.evaluation_comments.getName()).replace("|", "\n");
							val_evaluation_comments_username = res.getString(Field.evaluation_comments_username.getName());
							val_evaluation_comments_date = df.format(res.getTimestamp(Field.evaluation_comments_date.getName()));
							val_history = res.getString(Field.history.getName()).replace('|', '\n');
						}
					}
					if (res.getObject("num_eval_id") != null) {
						var_id_to_num_evaluation_id.put(var_id, res.getInt("num_eval_id"));						
						val_num_evaluated_as_type_1 = res.getInt(Field.num_evaluated_as_type_1.getName());
						val_num_evaluated_as_type_2 = res.getInt(Field.num_evaluated_as_type_2.getName());
						val_num_evaluated_as_type_3 = res.getInt(Field.num_evaluated_as_type_3.getName());
						val_num_evaluated_as_type_4 = res.getInt(Field.num_evaluated_as_type_4.getName());
						val_num_evaluated_as_type_5 = res.getInt(Field.num_evaluated_as_type_5.getName());
					}
				}
			}

			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBackground(Resources.getColor(getColor(), 200, false));
			
			statusEvaluation = new JButton();
			statusEvaluation.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusEvaluationPopupMenu = new JPopupMenu();
			JMenuItem itemEvaluationNull = new JMenuItem("Variant has not been evaluated",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemEvaluationNull.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.evaluation, 0, "Evaluation set to not evaluated ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_Evaluation(null,null,null,true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusEvaluationPopupMenu.add(itemEvaluationNull);
			JMenuItem itemEvaluation1 = new JMenuItem("Type I: Benign - Polymorphism ["+val_num_evaluated_as_type_1+"]",Resources.getScaledIcon(Resources.iRoman1, 24));
			itemEvaluation1.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.evaluation, 1, "Evaluation set to type I ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_Evaluation("1",Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusEvaluationPopupMenu.add(itemEvaluation1);
			JMenuItem itemEvaluation2 = new JMenuItem("Type II: Variant Likely Benign ["+val_num_evaluated_as_type_2+"]",Resources.getScaledIcon(Resources.iRoman2, 24));
			itemEvaluation2.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.evaluation, 2, "Evaluation set to type II ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_Evaluation("2",Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusEvaluationPopupMenu.add(itemEvaluation2);
			JMenuItem itemEvaluation3 = new JMenuItem("Type III: Variant of Unknown Significance ["+val_num_evaluated_as_type_3+"]",Resources.getScaledIcon(Resources.iRoman3, 24));
			itemEvaluation3.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.evaluation, 3, "Evaluation set to type III ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_Evaluation("3",Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusEvaluationPopupMenu.add(itemEvaluation3);
			JMenuItem itemEvaluation4 = new JMenuItem("Type IV: Variant Likely Pathogenic ["+val_num_evaluated_as_type_4+"]",Resources.getScaledIcon(Resources.iRoman4, 24));
			itemEvaluation4.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.evaluation, 4, "Evaluation set to type IV ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_Evaluation("4",Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusEvaluationPopupMenu.add(itemEvaluation4);
			JMenuItem itemEvaluation5 = new JMenuItem("Type V: Pathogenic Mutation ["+val_num_evaluated_as_type_5+"]",Resources.getScaledIcon(Resources.iRoman5, 24));
			itemEvaluation5.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.evaluation, 5, "Evaluation set to type V ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_Evaluation("5",Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusEvaluationPopupMenu.add(itemEvaluation5);
			MouseListener statusEvaluationPopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					try{
						if (Highlander.getLoggedUser().hasPermissionToModify(analysis, var_id_to_annotation_id.keySet())){
							statusEvaluationPopupMenu.show(e.getComponent(), e.getX(), e.getY());
						}else{
							JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this, 
									"You are not authorized to change this flag.", 
									"Changing evaluation", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserLock,64));
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot verify user permission on this variant", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			};
			statusEvaluation.addMouseListener(statusEvaluationPopupListener);
			panel.add(statusEvaluation, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelEvaluation = new JLabel(Field.evaluation.getName(), SwingConstants.LEFT);
			labelEvaluation.setToolTipText(Field.evaluation.getHtmlTooltip());
			panel.add(labelEvaluation, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			userEvaluation = new JLabel("", JLabel.LEFT);
			panel.add(userEvaluation, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 5), 0, 0));
			set_Evaluation(val_evaluation, val_evaluation_username, val_evaluation_date,false);

			statusInsilico = new JButton();
			statusInsilico.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusInsilicoPopupMenu = new JPopupMenu();
			JMenuItem itemInsilicoNotChecked = new JMenuItem("NOT_CHECKED - Variant has not been checked insilico",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemInsilicoNotChecked.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_insilico, Insilico.NOT_CHECKED, "check_insilico set to NOT_CHECKED ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_insilico(Insilico.NOT_CHECKED,null,df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInsilicoPopupMenu.add(itemInsilicoNotChecked);
			JMenuItem itemInsilicoOK = new JMenuItem("OK - Variant has been checked insilico and it's likely a real variant",Resources.getScaledIcon(Resources.iButtonApply, 24));
			itemInsilicoOK.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_insilico, Insilico.OK, "check_insilico set to OK ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_insilico(Insilico.OK,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInsilicoPopupMenu.add(itemInsilicoOK);
			JMenuItem itemInsilicoNotOk = new JMenuItem("NOT_OK - Variant has been checked insilico and it's likely a sequencing error",Resources.getScaledIcon(Resources.iCross, 24));
			itemInsilicoNotOk.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_insilico, Insilico.NOT_OK, "check_insilico set to NOT_OK ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_insilico(Insilico.NOT_OK,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInsilicoPopupMenu.add(itemInsilicoNotOk);
			JMenuItem itemInsilicoSuspect = new JMenuItem("SUSPECT - Variant has been checked insilico but not sure if it's real or not",Resources.getScaledIcon(Resources.iAttention, 24));
			itemInsilicoSuspect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_insilico, Insilico.SUSPECT, "check_insilico set to SUSPECT ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_insilico(Insilico.SUSPECT,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusInsilicoPopupMenu.add(itemInsilicoSuspect);
			MouseListener statusInsilicoPopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					try{
						if (Highlander.getLoggedUser().hasPermissionToModify(analysis, var_id_to_annotation_id.keySet())){
							statusInsilicoPopupMenu.show(e.getComponent(), e.getX(), e.getY());
						}else{
							JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this, 
									"You are not authorized to change this flag.", 
									"Changing check_insilico", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserLock,64));
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot verify user permission on this variant", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			};
			statusInsilico.addMouseListener(statusInsilicoPopupListener);
			panel.add(statusInsilico, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelInsilico = new JLabel(Field.check_insilico.getName(), SwingConstants.LEFT);
			labelInsilico.setToolTipText(Field.check_insilico.getHtmlTooltip());
			panel.add(labelInsilico, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			userInsilico = new JLabel("", JLabel.LEFT);
			panel.add(userInsilico, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 5), 0, 0));
			set_check_insilico(val_check_insilico, val_check_insilico_username, val_check_insilico_date,false);

			statusReporting = new JButton();
			statusReporting.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusReportingPopupMenu = new JPopupMenu();
			JMenuItem itemReportingNotChecked = new JMenuItem("NOT_CHECKED - Variant has not been checked",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemReportingNotChecked.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.reporting, Reporting.NOT_CHECKED, "reporting set to NOT_CHECKED ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_reporting(Reporting.NOT_CHECKED,null,df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusReportingPopupMenu.add(itemReportingNotChecked);
			JMenuItem iteReportingYes = new JMenuItem("YES - Variant has been checked and needs to be reported to the patient",Resources.getScaledIcon(Resources.iButtonApply, 24));
			iteReportingYes.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.reporting, Reporting.YES, "reporting set to YES ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_reporting(Reporting.YES,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusReportingPopupMenu.add(iteReportingYes);
			JMenuItem itemReportingNo = new JMenuItem("NO - Variant has been checked and doesn’t need to be reported to the patient",Resources.getScaledIcon(Resources.iCross, 24));
			itemReportingNo.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.reporting, Reporting.NO, "reporting set to NO ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_reporting(Reporting.NO,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusReportingPopupMenu.add(itemReportingNo);
			MouseListener statusReportingPopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					try{
						if (Highlander.getLoggedUser().hasPermissionToModify(analysis, var_id_to_annotation_id.keySet())){
							statusReportingPopupMenu.show(e.getComponent(), e.getX(), e.getY());
						}else{
							JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this, 
									"You are not authorized to change this flag.", 
									"Changing reporting", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserLock,64));
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot verify user permission on this variant", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			};
			statusReporting.addMouseListener(statusReportingPopupListener);
			panel.add(statusReporting, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelReporting = new JLabel(Field.reporting.getName(), SwingConstants.LEFT);
			labelReporting.setToolTipText(Field.reporting.getHtmlTooltip());
			panel.add(labelReporting, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			userReporting = new JLabel("", JLabel.LEFT);
			panel.add(userReporting, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 5), 0, 0));
			set_reporting(val_reporting, val_reporting_username, val_reporting_date,false);
			
			statusValidated = new JButton();
			statusValidated.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusValidatedPopupMenu = new JPopupMenu();
			JMenuItem itemValidatedNotChecked = new JMenuItem("NOT_CHECKED - Variant has not been checked in the lab",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemValidatedNotChecked.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_validated_variant, Validation.NOT_CHECKED, 
								"check_validated_variant set to NOT_CHECKED ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_validated_variant(Validation.NOT_CHECKED,null,null,true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusValidatedPopupMenu.add(itemValidatedNotChecked);
			JMenuItem itemValidatedValidated = new JMenuItem("VALIDATED - Variant has been tested in the lab and CONFIRMED to be true",Resources.getScaledIcon(Resources.iButtonApply, 24));
			itemValidatedValidated.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_validated_variant, Validation.VALIDATED, 
								"check_validated_variant set to VALIDATED ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_validated_variant(Validation.VALIDATED,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusValidatedPopupMenu.add(itemValidatedValidated);
			JMenuItem itemValidatedInvalidated = new JMenuItem("INVALIDATED - Variant has been tested in the lab and was NOT found",Resources.getScaledIcon(Resources.iCross, 24));
			itemValidatedInvalidated.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_validated_variant, Validation.INVALIDATED, 
								"check_validated_variant set to INVALIDATED ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_validated_variant(Validation.INVALIDATED,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusValidatedPopupMenu.add(itemValidatedInvalidated);
			JMenuItem itemValidatedSuspect = new JMenuItem("SUSPECT - Variant has been tested in the lab, but cannot be confirmed or invalidate",Resources.getScaledIcon(Resources.iAttention, 24));
			itemValidatedSuspect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_validated_variant, Validation.SUSPECT, 
								"check_validated_variant set to SUSPECT ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_validated_variant(Validation.SUSPECT,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusValidatedPopupMenu.add(itemValidatedSuspect);
			MouseListener statusValidatedPopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					try{
						if (Highlander.getLoggedUser().hasPermissionToModify(analysis, var_id_to_annotation_id.keySet())){
							statusValidatedPopupMenu.show(e.getComponent(), e.getX(), e.getY());
						}else{
							JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this, 
									"You are not authorized to change this flag.", 
									"Changing check_validated_variant", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserLock,64));
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot verify user permission on this variant", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			};
			statusValidated.addMouseListener(statusValidatedPopupListener);
			panel.add(statusValidated, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelValidated = new JLabel(Field.check_validated_variant.getName(), SwingConstants.LEFT);
			labelValidated.setToolTipText(Field.check_validated_variant.getHtmlTooltip());
			panel.add(labelValidated, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			userValidated = new JLabel("", JLabel.LEFT);
			panel.add(userValidated, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 5), 0, 0));
			set_check_validated_variant(val_check_validated_variant, val_check_validated_variant_username, val_check_validated_variant_date,false);

			statusSomatic = new JButton();
			statusSomatic.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusSomaticPopupMenu = new JPopupMenu();
			JMenuItem itemSomaticNotChecked = new JMenuItem("NOT_CHECKED - Variant has not been checked for mosaicism",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemSomaticNotChecked.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_somatic_variant, Mosaicism.NOT_CHECKED, 
								"check_somatic_variant set to NOT_CHECKED ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_somatic_variant(Mosaicism.NOT_CHECKED,null,null,true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSomaticPopupMenu.add(itemSomaticNotChecked);
			JMenuItem itemMosaicismSomatic = new JMenuItem("SOMATIC - Variant has been checked and seems to be a somatic variant",Resources.getScaledIcon(Resources.iButtonApply, 24));
			itemMosaicismSomatic.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_somatic_variant, Mosaicism.SOMATIC, 
								"check_somatic_variant set to SOMATIC ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_somatic_variant(Mosaicism.SOMATIC,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSomaticPopupMenu.add(itemMosaicismSomatic);
			JMenuItem itemMosaicismGermline = new JMenuItem("GERMLINE - Variant has been checked and seems to be a germline variant",Resources.getScaledIcon(Resources.iCross, 24));
			itemMosaicismGermline.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_somatic_variant, Mosaicism.GERMLINE, 
								"check_somatic_variant set to GERMLINE ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_somatic_variant(Mosaicism.GERMLINE,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSomaticPopupMenu.add(itemMosaicismGermline);
			JMenuItem itemMosaicismDubious = new JMenuItem("DUBIOUS - Variant has been checked for mosaicism but was impossible to differenciate between somatic or germline",Resources.getScaledIcon(Resources.iAttention, 24));
			itemMosaicismDubious.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_somatic_variant, Mosaicism.DUBIOUS, 
								"check_somatic_variant set to DUBIOUS ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_somatic_variant(Mosaicism.DUBIOUS,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSomaticPopupMenu.add(itemMosaicismDubious);
			MouseListener statusSomaticPopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					try{
						if (Highlander.getLoggedUser().hasPermissionToModify(analysis, var_id_to_annotation_id.keySet())){
							statusSomaticPopupMenu.show(e.getComponent(), e.getX(), e.getY());
						}else{
							JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this, 
									"You are not authorized to change this flag.", 
									"Changing check_somatic_variant", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserLock,64));
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot verify user permission on this variant", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			};
			statusSomatic.addMouseListener(statusSomaticPopupListener);
			panel.add(statusSomatic, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelSomatic = new JLabel(Field.check_somatic_variant.getName(), SwingConstants.LEFT);
			labelSomatic.setToolTipText(Field.check_somatic_variant.getHtmlTooltip());
			panel.add(labelSomatic, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			userSomatic = new JLabel("", JLabel.LEFT);
			panel.add(userSomatic, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 5), 0, 0));
			set_check_somatic_variant(val_check_somatic_variant, val_check_somatic_variant_username, val_check_somatic_variant_date,false);

			statusSegregation = new JButton();
			statusSegregation.setPreferredSize(new Dimension(28,28));
			final JPopupMenu statusSegregationPopupMenu = new JPopupMenu();
			JMenuItem itemSegregationNotChecked = new JMenuItem("NOT_CHECKED - Not checked for segregation",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemSegregationNotChecked.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_segregation, Segregation.NOT_CHECKED, 
								"check_segregation set to NOT_CHECKED ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_segregation(Segregation.NOT_CHECKED,null,null,true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSegregationPopupMenu.add(itemSegregationNotChecked);			
			JMenuItem itemSegregationSingle = new JMenuItem("SINGLE - No other sample in family",Resources.getScaledIcon(Resources.iQuestion, 24));
			itemSegregationSingle.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_segregation, Segregation.SINGLE, 
								"check_segregation set to SINGLE ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_segregation(Segregation.SINGLE,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSegregationPopupMenu.add(itemSegregationSingle);
			JMenuItem itemSegregationCoseg = new JMenuItem("COSEG - Variant cosegregates",Resources.getScaledIcon(Resources.iButtonApply, 24));
			itemSegregationCoseg.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_segregation, Segregation.COSEG, 
								"check_segregation set to COSEG ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_segregation(Segregation.COSEG,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSegregationPopupMenu.add(itemSegregationCoseg);
			JMenuItem itemSegregationCarriers = new JMenuItem("CARRIERS - Some unaffected carrier(s)",Resources.getScaledIcon(Resources.iButtonApply, 24));
			itemSegregationCarriers.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_segregation, Segregation.CARRIERS, 
								"check_segregation set to CARRIERS ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_segregation(Segregation.CARRIERS,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSegregationPopupMenu.add(itemSegregationCarriers);
			JMenuItem itemSegregationNoCoseg = new JMenuItem("NO_COSEG - Not in other affected(s)",Resources.getScaledIcon(Resources.iCross, 24));
			itemSegregationNoCoseg.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_segregation, Segregation.NO_COSEG, 
								"check_segregation set to NO_COSEG ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_segregation(Segregation.NO_COSEG,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSegregationPopupMenu.add(itemSegregationNoCoseg);
			JMenuItem itemSegregationNoCosegOther = new JMenuItem("NO_COSEG_OTHER - Does not cosegregate in other families",Resources.getScaledIcon(Resources.i2dMinus, 24));
			itemSegregationNoCosegOther.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.check_segregation, Segregation.NO_COSEG_OTHER, 
								"check_segregation set to NO_COSEG_OTHER ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|");
						set_check_segregation(Segregation.NO_COSEG_OTHER,Highlander.getLoggedUser().getUsername(),df.format(System.currentTimeMillis()),true);
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			statusSegregationPopupMenu.add(itemSegregationNoCosegOther);

			MouseListener statusSegregationPopupListener = new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					try{
						if (Highlander.getLoggedUser().hasPermissionToModify(analysis, var_id_to_annotation_id.keySet())){
							statusSegregationPopupMenu.show(e.getComponent(), e.getX(), e.getY());
						}else{
							JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this, 
									"You are not authorized to change this flag.", 
									"Changing check_segregation", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserLock,64));
						}
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot verify user permission on this variant", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			};
			statusSegregation.addMouseListener(statusSegregationPopupListener);
			panel.add(statusSegregation, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 3, 3), 0, 0));
			JLabel labelSegregation = new JLabel(Field.check_segregation.getName(), SwingConstants.LEFT);
			labelSegregation.setToolTipText(Field.check_segregation.getHtmlTooltip());
			panel.add(labelSegregation, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 3), 0, 0));
			userSegregation = new JLabel("", JLabel.LEFT);
			panel.add(userSegregation, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 3, 3, 5), 0, 0));
			set_check_segregation(val_check_segregation, val_check_segregation_username, val_check_segregation_date,false);

			commentTextArea.setToolTipText(Field.evaluation_comments.getHtmlTooltip());
			commentTextArea.setLineWrap(true);
			commentTextArea.setWrapStyleWord(true);
			commentTextArea.setRows(3);
			commentTextArea.setText(val_evaluation_comments);
			commentsPanel.setBackground(Resources.getColor(getColor(), 200, false));
			if (val_evaluation_comments_username != null) {
				commentsPanel.setBorder(BorderFactory.createTitledBorder(Field.evaluation_comments.getName() + " ("+val_evaluation_comments_username+" - "+val_evaluation_comments_date+")"));
			}else {
				commentsPanel.setBorder(BorderFactory.createTitledBorder(Field.evaluation_comments.getName()));
			}
			commentsPanel.revalidate();
			JButton submitCommentsButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 20));
			submitCommentsButton.setToolTipText("Update evaluation comment in the database");
			submitCommentsButton.setPreferredSize(new Dimension(28,28));
			submitCommentsButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try{
						updateDatabaseVariant(Field.evaluation_comments, commentTextArea.getText(),
								""+Highlander.getDB().format(Schema.HIGHLANDER, "evaluation_comments modified to \'"+commentTextArea.getText()+"\' ("+Highlander.getLoggedUser().getUsername()+" - "+df.format(System.currentTimeMillis())+")|"));
						set_evaluation_comments();
						/* possible_values is limited to a size of 1000 unlike comments fields.
						 * I don't import them either in dbBuilder (\n and \r potential problem).
						 * So I wouldn't import them at all
						Highlander.getDB().update(Schema.HIGHLANDER, 
								"INSERT INTO " + analysis + "_possible_values (`field`,`value`) "
										+ "VALUES('evaluation_comments','"+ Highlander.getDB().format(Schema.HIGHLANDER, commentTextArea.getText())+"')");
						 */
					}catch(Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(DetailsBoxEvaluationAnnotations.this,  Tools.getMessage("Cannot update database", ex), "Updating database field",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			});
			try{
				if (!Highlander.getLoggedUser().hasPermissionToModify(analysis, var_id_to_annotation_id.keySet())){
					commentTextArea.setEditable(false);
					submitCommentsButton.setEnabled(false);
				}
			}catch(Exception ex){
				Tools.exception(ex);
				commentTextArea.setEditable(false);
				submitCommentsButton.setEnabled(false);
			}
			commentsPanel.setToolTipText(Field.evaluation_comments.getHtmlTooltip());
			commentsPanel.add(commentTextArea, BorderLayout.CENTER);
			commentsPanel.add(submitCommentsButton, BorderLayout.WEST);
			panel.add(commentsPanel, new GridBagConstraints(0, 6, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));

			historyTextArea.setToolTipText(Field.history.getHtmlTooltip());
			historyTextArea.setLineWrap(true);
			historyTextArea.setWrapStyleWord(true);
			historyTextArea.setRows(3);
			historyTextArea.setEditable(false);
			historyTextArea.setText(val_history);
			JPanel historyPanel = new JPanel(new BorderLayout(0,0));
			historyPanel.setBackground(Resources.getColor(getColor(), 200, false));
			historyPanel.setToolTipText(Field.history.getHtmlTooltip());
			historyPanel.setBorder(BorderFactory.createTitledBorder(Field.history.getName()));
			historyPanel.add(historyTextArea, BorderLayout.CENTER);
			panel.add(historyPanel, new GridBagConstraints(0, 7, 3, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));

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
