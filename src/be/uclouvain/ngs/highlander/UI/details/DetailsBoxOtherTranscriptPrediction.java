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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.table.MultiLineTableCellRenderer;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.Aloft;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.FitCons;
import be.uclouvain.ngs.highlander.database.Field.ImpactPrediction;
import be.uclouvain.ngs.highlander.database.Field.SplicingPrediction;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;

/**
 * Some predictor scores for other transcripts, when available in DBNSFP
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxOtherTranscriptPrediction extends DetailsBox {

	private DetailsPanel mainPanel;
	private Map<String, JTable> tables = new TreeMap<String, JTable>();
	private JComboBox<String> boxTranscripts;
	private boolean detailsLoaded = false;

	public DetailsBoxOtherTranscriptPrediction(int variantId, DetailsPanel mainPanel){
		this.variantSampleId = variantId;
		this.mainPanel = mainPanel;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	@Override
	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	@Override
	public String getTitle(){
		return "Effect prediction (non-canonical transcripts)";
	}

	@Override
	public Palette getColor() {
		return Field.snpeff_effect.getCategory().getColor();
	}

	@Override
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	@Override
	protected void loadDetails(){
		try{
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			final List<Field> fields = new ArrayList<Field>();
			for (Field field : Field.getAvailableFields(analysis, false)){
				if (field.getAnnotationCode() == Annotation.DBNSFP && field.hasMultipleAnnotations() 
						&& field.getAnnotationHeaders() != null && field.getAnnotationHeaders().length > 1
						&& !field.getMultipleAnnotationsField().equalsIgnoreCase("MutationTaster") //in DBNSFP 4.1, Mutation Taster predictions don't match listed transcripts. Easier to just use the external link button to the full prediction. 
						) {
					fields.add(field);
				}
			}
			if (fields.isEmpty()){
				detailsPanel.removeAll();
				detailsPanel.add(new JLabel("This category is empty (no field visible in this analysis has been assigned to it)"), BorderLayout.CENTER);
			}else{
				fields.add(0, Field.transcript_uniprot_id);
				fields.add(1, Field.transcript_refseq_mrna);
				fields.add(2, Field.snpeff_effect);
				String chr = "";
				int pos = 0;
				String alternative = "";
				String gene_symbol = "";
				String gene_ensembl = "";
				String transcript_ensembl = "";
				String snpeff_other_transcripts = "";
				VariantType variant_type = null;
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT chr, pos, alternative, gene_symbol, " 
								+ Field.transcript_ensembl.getQuerySelectName(analysis, false) + ", "
								+ Field.variant_type.getQuerySelectName(analysis, false) + ", " 
								+ Field.snpeff_other_transcripts.getQuerySelectName(analysis, false) + ", " 
								+ Field.gene_ensembl.getQuerySelectName(analysis, false) + " "
								+ "FROM " + analysis.getFromSampleAnnotations()
								+ analysis.getJoinStaticAnnotations()
								+ analysis.getJoinGeneAnnotations()
								+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + variantSampleId)){
					if (res.next()){
						chr = res.getString("chr");
						pos = res.getInt("pos");
						alternative = res.getString("alternative");
						gene_symbol = res.getString("gene_symbol");
						gene_ensembl = res.getString(Field.gene_ensembl.getName());
						transcript_ensembl = res.getString(Field.transcript_ensembl.getName());
						variant_type = VariantType.valueOf(res.getString(Field.variant_type.getName()));
						snpeff_other_transcripts = res.getString(Field.snpeff_other_transcripts.getName());
					}
				}
				Map<String, String> snpeff = new TreeMap<>();
				if (snpeff_other_transcripts != null && snpeff_other_transcripts.length() > 0) {
					for (String trans : snpeff_other_transcripts.split(";")) {
						snpeff.put(trans.split(":")[0], trans.split(":")[1]);
					}
				}
				if (variant_type == VariantType.SNV) {
					if (gene_symbol.length() > 0) {	
						String dbnsfpTable = null;
						for (Field field : fields) {
							if (field.getAnnotationCode() == Annotation.DBNSFP) {
								dbnsfpTable = field.getAnnotationHeaders()[0];
								dbnsfpTable = dbnsfpTable.replace("[chr]", chr);
								break;
							}
						}
						StringBuilder query = new StringBuilder();
						query.append("SELECT ");
						for (Field field : fields) {
							if (field.getAnnotationCode() == Annotation.DBNSFP) {
								query.append("`" + field.getAnnotationHeaders()[1] + "`, ");
							}
						}
						query.append("`Uniprot_acc`, ");
						query.append("`genename`, ");
						query.append("`Ensembl_geneid`, ");
						query.append("`Ensembl_transcriptid`");
						query.append(" FROM `"+dbnsfpTable+"` WHERE ");
						if (analysis.getReference().getGenomeVersion() == 37) {
							query.append("`hg19_pos` = '" + pos + "'");
						}else if (analysis.getReference().getGenomeVersion() == 36){
							query.append("`hg18_pos` = '" + pos + "'");
						}else {
							query.append("`pos` = " + pos);
						}
						query.append(" AND `alt` = '"+alternative+"'");
						query.append(" AND (INSTR(`genename`, '"+gene_symbol+"') OR INSTR(`Ensembl_geneid`, '"+gene_ensembl+"'))");
						String[] transcriptIds = new String[0];
						try (Results res = DB.select(analysis.getReference(), Schema.DBNSFP, query.toString())) {
							if (res.next()){
								transcriptIds = res.getString("Ensembl_transcriptid").split(";");
								for (int transcriptIndex=0 ; transcriptIndex < transcriptIds.length ; transcriptIndex++) {
									if (res.getString("genename").split(";")[transcriptIndex].equalsIgnoreCase(gene_symbol) || res.getString("Ensembl_geneid").split(";")[transcriptIndex].equalsIgnoreCase(gene_ensembl)) {
										Map<Field, Object> entries = new TreeMap<>();
										for (Field field : fields) {
											entries.put(field, null);
										}
										entries.put(Field.transcript_uniprot_id, res.getString("Uniprot_acc").split(";")[transcriptIndex]);
										for (Field field : fields) {
											if (field.getAnnotationCode() == Annotation.DBNSFP) {
												String column = field.getAnnotationHeaders()[1];
												String value = res.getString(column);
												if (value != null) {
													value = value.trim().split(field.getMultipleAnnotationsSeparator())[transcriptIndex];
													if (!value.equals(".")) {
														switch(column) {
														case "FATHMM_pred":
														case "SIFT_pred":
														case "SIFT4G_pred":
														case "M-CAP_pred":
														case "LIST-S2_pred":
														case "DEOGEN2_pred":
														case "ClinPred_pred":
														case "BayesDel_addAF_pred":
														case "BayesDel_noAF_pred":
														case "PrimateAI_pred":
														case "MetaSVM_pred":
														case "MetaLR_pred":
															if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING); 
															else if (value.equals("T")) entries.put(field, ImpactPrediction.TOLERATED); 
															break;
														case "fathmm-XF_coding_pred":
														case "fathmm-MKL_coding_pred":
														case "PROVEAN_pred":
															if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING); 
															else if (value.equals("N")) entries.put(field, ImpactPrediction.TOLERATED); 
															break;
														case "Polyphen2_HDIV_pred":
														case "Polyphen2_HVAR_pred":
															if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING);
															else if (value.equals("P")) entries.put(field, ImpactPrediction.DAMAGING); 
															else if (value.equals("B")) entries.put(field, ImpactPrediction.TOLERATED); 													
															break;
														case "MutationAssessor_pred":
															if (value.equals("H")) entries.put(field, ImpactPrediction.DAMAGING); 
															else if (value.equals("M")) entries.put(field, ImpactPrediction.DAMAGING); 
															else if (value.equals("L")) entries.put(field, ImpactPrediction.TOLERATED); 
															else if (value.equals("N")) entries.put(field, ImpactPrediction.TOLERATED); 
															break;
														case "LRT_pred":
															if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING); 
															else if (value.equals("N")) entries.put(field, ImpactPrediction.TOLERATED); 
															else if (value.equals("U")) entries.put(field, null); 
															break;
														case "integrated_confidence_value":
														case "GM12878_confidence_value":
														case "H1-hESC_confidence_value":
														case "HUVEC_confidence_value":
															if (value.equalsIgnoreCase("0")) entries.put(field, FitCons.HIGHLY_SIGNIFICANT); 
															else if (value.equalsIgnoreCase("1")) entries.put(field, FitCons.SIGNIFICANT); 
															else if (value.equalsIgnoreCase("2")) entries.put(field, FitCons.INFORMATIVE); 
															else if (value.equalsIgnoreCase("3")) entries.put(field, FitCons.OTHER); 
															break;
														case "Aloft_pred":
															if (value.equalsIgnoreCase("DOMINANT")) entries.put(field, Aloft.DOMINANT); 
															else if (value.equalsIgnoreCase("RECESSIVE")) entries.put(field, Aloft.RECESSIVE); 
															else if (value.equalsIgnoreCase("TOLERANT")) entries.put(field, Aloft.TOLERANT); 
															break;
														case "ada_score":
															double ada_score = Double.parseDouble(value);
															entries.put(field, ada_score);
															entries.put(Field.splicing_ada_pred, (ada_score > 0.6) ? SplicingPrediction.AFFECTING_SPLICING : SplicingPrediction.SPLICING_UNAFFECTED);
															break;													
														case "rf_score":
															double rf_score = Double.parseDouble(value);
															entries.put(field, rf_score);
															entries.put(Field.splicing_rf_pred, (rf_score > 0.6) ? SplicingPrediction.AFFECTING_SPLICING : SplicingPrediction.SPLICING_UNAFFECTED);
															break;
														case "HIPred":
														case "is_scSNV_RefSeq":
														case "is_scSNV_Ensembl":
														case "ExAC_cnv_flag":
															//Convert Y/N to boolean
															entries.put(field, (value.toUpperCase().equals("Y")) ? true : false);
															break;
														default:
															entries.put(field, value);
															break;
														}
													}
												}
											}
										}
										entries.put(Field.transcript_refseq_mrna, DBUtils.getAccessionRefSeqMRna(analysis.getReference(), transcriptIds[transcriptIndex]));
										entries.put(Field.snpeff_impact, snpeff.remove(transcriptIds[transcriptIndex]));
										List<Object> values = new ArrayList<Object>();
										for (Field field : fields) {
											values.add(entries.get(field));
										}
										addTable(transcriptIds[transcriptIndex], fields, values);
									}
								}
							}
						}						
					}
				}
				if (transcript_ensembl != null) tables.remove(transcript_ensembl);
				for (String transcript : snpeff.keySet()) {
					Map<Field, Object> entries = new TreeMap<>();
					for (Field field : fields) {
						entries.put(field, null);
					}
					entries.put(Field.transcript_uniprot_id, DBUtils.getAccessionUniprot(analysis.getReference(), transcript));
					entries.put(Field.transcript_refseq_mrna, DBUtils.getAccessionRefSeqMRna(analysis.getReference(), transcript));
					entries.put(Field.snpeff_effect, snpeff.get(transcript));
					List<Object> values = new ArrayList<Object>();
					for (Field field : fields) {
						values.add(entries.get(field));
					}
					addTable(transcript, fields, values);
				}
				if (!tables.isEmpty()) {
					detailsPanel.removeAll();
					String[] transcripts = tables.keySet().toArray(new String[0]);
					boxTranscripts = new JComboBox<String>(transcripts);
					detailsPanel.add(boxTranscripts, BorderLayout.NORTH);							
					detailsPanel.add(tables.get(transcripts[0]), BorderLayout.CENTER);
					boxTranscripts.addItemListener(new ItemListener() {
						@Override
						public void itemStateChanged(ItemEvent e) {
							detailsPanel.removeAll();
							detailsPanel.add(boxTranscripts, BorderLayout.NORTH);							
							detailsPanel.add(tables.get(boxTranscripts.getSelectedItem().toString()), BorderLayout.CENTER);
							detailsPanel.revalidate();
						}
					});
				}else {
					detailsPanel.removeAll();
					if (gene_symbol.length() > 0) {
						detailsPanel.add(new JLabel("This gene has only one transcript"), BorderLayout.CENTER);
					}else {
						detailsPanel.add(new JLabel("This variant is not within a gene"), BorderLayout.CENTER);
					}
				}
			}			
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

	private void addTable(String transcript, List<Field> fields, List<Object> values) {
		final DetailsTableModel model = new DetailsTableModel(fields, values);
		JTable table = new JTable(model){
			@Override
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				int colIndex = columnAtPoint(p);
				Object val = "";
				if (colIndex == 0){
					val = model.getRowDescription(rowIndex);
				}else{
					val = getValueAt(rowIndex, colIndex);
					if (val != null) {
						if (model.getRowField(rowIndex).getFieldClass() == Integer.class) val = Tools.intToString(Integer.parseInt(val.toString()));
						else if (model.getRowField(rowIndex).getFieldClass() == Long.class) val = Tools.longToString(Long.parseLong(val.toString()));
					}
				}
				tip = (val != null) ? val.toString() : "";
				return tip;
			}
		};
		Highlander.getCellRenderer().registerTableForHighlighting(getTitle(), table);
		table.setDefaultRenderer(String.class, new ColoredTableCellRenderer());	
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(true);			
		table.setCellSelectionEnabled(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		//Fit the first (field) column size to the biggest cell, letting all remaining space to the second (data) column
		if (table.getRowCount() > 0){
			int maxWidth = 0;
			for (int r = 0 ; r < table.getRowCount() ; r++){
				JTextArea textArea = (JTextArea)(table.getCellRenderer(r, 0).getTableCellRendererComponent(table, table.getValueAt(r, 0),false, false, r, 0));
				maxWidth = Math.max(maxWidth, new JLabel(textArea.getText()).getPreferredSize().width + 5);
			}
			TableColumn tc = table.getColumnModel().getColumn(0);
			tc.setPreferredWidth(maxWidth);
			tc.setMaxWidth(maxWidth);
		}
		tables.put(transcript, table);
	}
	
	private class ColoredTableCellRenderer extends MultiLineTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			JTextArea textArea = (JTextArea) comp;
			if (row%2 == 0) textArea.setBackground(Resources.getTableEvenRowBackgroundColor(getColor()));
			else textArea.setBackground(Color.WHITE);
			textArea.setForeground(Color.black);
			textArea.setBorder(new LineBorder(Color.WHITE));
			if (isSelected) {
				textArea.setBackground(new Color(51,153,255));
			}
			Field field = (column == 0) ? new Field("field") : ((DetailsTableModel)table.getModel()).getRowField(row);
			return Highlander.getCellRenderer().renderCell(textArea, value, field, SwingConstants.LEFT, row, isSelected, Resources.getTableEvenRowBackgroundColor(getColor()), Color.WHITE, false);
		}
	}

	public static class DetailsTableModel	extends AbstractTableModel {
		private List<Field> fields;
		private List<Object> values;

		public DetailsTableModel(List<Field> fields, List<Object> values) {    	
			this.fields = fields;
			this.values = values;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int col) {
			switch(col){
			case 0:
				return "Field";
			case 1:
				return "Value";
			default:
				return "Out of range columns";
			}
		}

		public int getRowIndex(Field field){
			for (int i=0 ; i < fields.size() ; i++){
				if (fields.get(i).getName().equals(field.getName())) return i;
			}
			return -1;
		}

		public Field getRowField(int row) {
			return fields.get(row);
		}

		public String getRowDescription(int row) {
			return fields.get(row).getHtmlTooltip();
		}
		
		@Override
		public int getRowCount() {
			return fields.size();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public Object getValueAt(int row, int col) {
			switch(col){
			case 0:
				return fields.get(row);
			case 1:
				return values.get(row);
			default:
				return null;
			}
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

	}

}
