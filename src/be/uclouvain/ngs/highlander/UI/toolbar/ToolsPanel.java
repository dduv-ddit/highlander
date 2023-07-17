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

package be.uclouvain.ngs.highlander.UI.toolbar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.broad.igv.ui.IGVAccess;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.AskBamPosDialog;
import be.uclouvain.ngs.highlander.UI.dialog.AskSamplesDialog;
import be.uclouvain.ngs.highlander.UI.dialog.AskTumorNormalAssociation;
import be.uclouvain.ngs.highlander.UI.dialog.CreateRunSelection;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.tools.AlignmentViewer;
import be.uclouvain.ngs.highlander.UI.tools.BamViewer;
import be.uclouvain.ngs.highlander.UI.tools.BurdenTest;
import be.uclouvain.ngs.highlander.UI.tools.CoverageInfo;
import be.uclouvain.ngs.highlander.UI.tools.CtdnaEstimation;
import be.uclouvain.ngs.highlander.UI.tools.Exomiser;
import be.uclouvain.ngs.highlander.UI.tools.Exomiser.Mode;
import be.uclouvain.ngs.highlander.UI.tools.FastQCViewer;
import be.uclouvain.ngs.highlander.UI.tools.FileDownloader;
import be.uclouvain.ngs.highlander.UI.tools.Kraken;
import be.uclouvain.ngs.highlander.UI.tools.OpenIGV;
import be.uclouvain.ngs.highlander.UI.tools.PedigreeChecker;
import be.uclouvain.ngs.highlander.UI.tools.RunCharts;
import be.uclouvain.ngs.highlander.UI.tools.RunStatistics;
import be.uclouvain.ngs.highlander.UI.tools.VariantAnnotator;
import be.uclouvain.ngs.highlander.UI.tools.VariantDistributionCharts;
import be.uclouvain.ngs.highlander.UI.tools.BurdenTest.Source;
import be.uclouvain.ngs.highlander.UI.tools.ConverterHGMD;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.MutatedSequence;
import be.uclouvain.ngs.highlander.datatype.MutatedSequence.Type;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Report;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.FilterType;

public class ToolsPanel extends JPanel {

	final Highlander mainFrame;

	public ToolsPanel(Highlander mainFrame){
		this.mainFrame = mainFrame;
		initUI();
	}

	private void initUI(){
		setLayout(new BorderLayout(0,0));

		JButton showAlignment = new JButton(Resources.getScaledIcon(Resources.iAlignmentSquishedOff, 40));
		showAlignment.setPreferredSize(new Dimension(54,54));
		showAlignment.setToolTipText("Alignment Viewer");
		showAlignment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewAlignment(false);
					}
				}, "ToolsPanel.viewAlignment").start();
			}
		});

		JButton showAlignmentPinned = new JButton(Resources.getScaledIcon(Resources.iAlignmentPinned, 40));
		showAlignmentPinned.setPreferredSize(new Dimension(54,54));
		showAlignmentPinned.setToolTipText("Launch Alignment Viewer with a selection of samples already pinned");
		showAlignmentPinned.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewAlignment(true);
					}
				}, "ToolsPanel.viewAlignment").start();
			}
		});

		JButton showInIGV = new JButton(Resources.getScaledIcon(Resources.iIGV, 40));
		showInIGV.setPreferredSize(new Dimension(54,54));
		showInIGV.setToolTipText("View selected variant in IGV");
		showInIGV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewInIGV();
					}
				}, "ToolsPanel.viewInIGV").start();
			}
		});

		JButton posInIGV = new JButton(Resources.getScaledIcon(Resources.iIGVpos, 40));
		posInIGV.setPreferredSize(new Dimension(54,54));
		posInIGV.setToolTipText("View selected position in IGV");
		posInIGV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						if (!IGVAccess.isPresent()){
							viewInIGV();
						}else{
							try {
								String chr = null;
								int pos = -1;
								try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
										"SELECT chr, pos "
										+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
										+ "WHERE variant_sample_id = " +mainFrame.getVariantTable().getSelectedVariantsId().get(0)
										)) {
									if (res.next()){
										chr = res.getString("chr");
										pos = res.getInt("pos");
									}
								}
								if (chr != null && pos >= 0){
									IGVAccess.setPosition(chr, pos);
								}
							} catch (Exception ex) {
								Tools.exception(ex);
								JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive position for selected variant", ex), "View selected position in IGV",
										JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
							}
						}
					}
				}, "ToolsPanel.posInIGV").start();
			}
		});

		JButton viewBam = new JButton(Resources.getScaledIcon(Resources.iBamViewer, 40));
		viewBam.setPreferredSize(new Dimension(54,54));
		viewBam.setToolTipText("<html><b>BAM Check</b><br>"
				+ "View ALL selected positions in a selection of BAM files.<br>"
				+ "It will run on the cluster then produce a table with number of reads presenting each nucleotide/gap/insertion.<br>"
				+ "A z-score is also computed for each nucleotide/gap/insertion, using your selection of samples.</html>");
		viewBam.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						bamView();
					}
				}, "ToolsPanel.bamView").start();
			}
		});

		JButton exportSequence = new JButton(Resources.getScaledIcon(Resources.iExportSequence, 40));
		exportSequence.setPreferredSize(new Dimension(54,54));
		exportSequence.setToolTipText("Export mutated DNA/AA sequences of selected variant(s)");
		exportSequence.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						exportSequence();
					}
				}, "ToolsPanel.exportSequence").start();
			}
		});


		JButton exportToExcelWithNormalRC = new JButton(Resources.getScaledIcon(Resources.iExcelTN, 40));
		exportToExcelWithNormalRC.setPreferredSize(new Dimension(54,54));
		exportToExcelWithNormalRC.setToolTipText("Export current table content to Excel, adding columns with read count for Normal/Tumor pairs");
		exportToExcelWithNormalRC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						exportToExcelWithNormalRC();
					}
				}, "ToolsPanel.exportToExcelWithNormalRC").start();
			}
		});

		JButton burdenTest = new JButton(Resources.getScaledIcon(Resources.iBurdenTest, 40));
		burdenTest.setPreferredSize(new Dimension(54,54));
		burdenTest.setToolTipText("<html><b>Gene burden</b><br>"
				+ "Display a selection of genes with variants found in Highlander and a public frequency database (e.g. exac).<br>"
				+ "The variants displayed are defined by the current custom filters (magic filters are not taken into account).<br>"
				+ "The same filter is also applied on the selected frequency database.</html>");
		burdenTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						burdenTest();
					}
				}, "ToolsPanel.burdenTest").start();
			}
		});

		JButton pedigreeChecker = new JButton(Resources.getScaledIcon(Resources.iPedigreeChecker, 40));
		pedigreeChecker.setPreferredSize(new Dimension(54,54));
		pedigreeChecker.setToolTipText("<html><b>Pedigree checker</b><br>"
				+ "A table that displays familial relationships between a selection of samples.<br>"
				+ "For each pair of selected samples, the proportion of common SNPs is computed, and displayed with a heat map.<br>"
				+ "For each sample, the gender is also predicted, using homozygous SNP ratio of chromosome X.</html>");
		pedigreeChecker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						pedigreeChecker();
					}
				}, "ToolsPanel.pedigreeChecker").start();
			}
		});

		JButton coverage = new JButton(Resources.getScaledIcon(Resources.iCoverage, 40));
		coverage.setPreferredSize(new Dimension(54,54));
		coverage.setToolTipText("<html><b>Gene coverage</b><br>"
				+ "Select a set of genes and a set of sample to display their average read depth and coverage.<br>"
				+ "You can display coverage per gene or per exon, separately for each sample or as an average.</html>");
		coverage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						coverage();
					}
				}, "ToolsPanel.coverage").start();
			}
		});

		JButton variantAnnotator = new JButton(Resources.getScaledIcon(Resources.iDbSearch, 40));
		variantAnnotator.setPreferredSize(new Dimension(54,54));
		variantAnnotator.setToolTipText("<html><b>Annotator</b><br>"
				+ "Get all annotations for a specified variant.<br>"
				+ "You can enter any variant by 'chr:pos-alt', even if no sample in Highlander has it.<br>"
				+ "All annotations available in Highlander databases will be fetched (e.g. DBNSFP, COSMIC).</html>");
		variantAnnotator.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						variantAnnotator();
					}
				}, "ToolsPanel.variantAnnotator").start();
			}
		});
		
		JButton download = new JButton(Resources.getScaledIcon(Resources.iDownload, 40));
		download.setPreferredSize(new Dimension(54,54));
		download.setToolTipText("<html><b>Download data files</b><br>"
				+ "This tools allow you to download original data files linked to your samples.<br>"
				+ "It can be base files like BAM or VCF, but also results from other tools included in the Highlander pipeline (like structural variants or microsattelite detection).<br>"
				+ "Select which tools/software you want to the results from, a directory to download the files into and a selection of samples.<br>"
				+ "Note that when you select samples, <b>you must select them in the analysis compatible with the tool/software producing the results</b>.<br>"
				+ "For example, if you want STAR-Fusion results, you have to select your samples from the <i>rnaseq_hg38</i> analysis (even if the sample exists also in e.g. <i>exomes_hg38</i>).<br>"
				+ "You can mix samples from different analyses, and you may have to select the same sample multiple times if it has the same name in different analyses you want results from.<br>"
				+ "You can hover your mouse on each tool/software to see the list of files that will be download, and in which analyses they are available.</html>");
		download.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						download();
					}
				}, "ToolsPanel.download").start();
			}
		});

		JButton exportToExcel = new JButton(Resources.getScaledIcon(Resources.iExcel, 40));
		exportToExcel.setPreferredSize(new Dimension(54,54));
		exportToExcel.setToolTipText("Export current table content to Excel");
		exportToExcel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						exportToExcel();
					}
				}, "ToolsPanel.exportToExcel").start();
			}
		});

		JButton exportToTSV = new JButton(Resources.getScaledIcon(Resources.iTSV, 40));
		exportToTSV.setPreferredSize(new Dimension(54,54));
		exportToTSV.setToolTipText("Export current table content to a 'Tab Separated Values' file");
		exportToTSV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						exportToTSV();
					}
				}, "ToolsPanel.exportToTSV").start();
			}
		});

		JButton exportToVCF = new JButton(Resources.getScaledIcon(Resources.iVCF, 40));
		exportToVCF.setPreferredSize(new Dimension(54,54));
		exportToVCF.setToolTipText("Export current table content to a VCF (Variant Calling Format) file");
		exportToVCF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						exportToVCF();
					}
				}, "ToolsPanel.exportToVCF").start();
			}
		});

		JButton importHGMD = new JButton(Resources.getScaledIcon(Resources.iHGMD, 40));
		importHGMD.setPreferredSize(new Dimension(54,54));
		importHGMD.setToolTipText("<html><b>HGMD importation</b><br>"
				+ "HGMD license doesn't allow Highlander to have their annotations in columns, but they can be saved in <i>public comments</i>.<br>"
				+ "To import HGMD annotations for a set of variants, proceed as follow:<ul>"
				+ "<li>Export your Highlander results as a VCF (using <i>export to VCF</i> tool in Highlander)</li>"
				+ "<li>Submit that VCF to HGMD as <i>batch results</i></li>"
				+ "<li>Save the batch results as an HTML file (right click outside the table in your browser, and hit <i>save as...</i>)</li>"
				+ "<li>Use this tool to import the HTML file in Highlander</li>"
				+ "<li>HGMD annotations for those variants will be visible in Highlander public comments, for any matching variant</li>"
				+ "</ul></html>");
		importHGMD.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						importHGMD();
					}
				}, "ToolsPanel.importHGMD").start();
			}
		});
		
		JButton viewRunStatisticsDetails = new JButton(Resources.getScaledIcon(Resources.iRunStatisticsDetails, 40));
		viewRunStatisticsDetails.setPreferredSize(new Dimension(54,54));
		viewRunStatisticsDetails.setToolTipText("<html><b>Run statistics (table)</b><br>"
				+ "A table displaying all available samples with general information about them and run metrics.<br>"
				+ "You can filter the table with multiple criteria and/or limit it to a selection of NGS runs.</html>");
		viewRunStatisticsDetails.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewRunStatisticsDetails();
					}
				}, "ToolsPanel.viewRunStatisticsDetails").start();
			}
		});

		JButton viewRunStatisticsCharts = new JButton(Resources.getScaledIcon(Resources.iRunStatisticsCharts, 40));
		viewRunStatisticsCharts.setPreferredSize(new Dimension(54,54));
		viewRunStatisticsCharts.setToolTipText("<html><b>Run statistics (charts)</b><br>"
				+ "Make a selection of NGS runs to display series of charts (histograms) usefull for quality control.<br>"
				+ "You can display information per sample or group it by run. You can check:<ul>"
				+ "<li>Average depth of coverage, with or without duplicates, for different targets</li>"
				+ "<li>Proportion of target with at least X depth</li>"
				+ "<li>Coverage ratio for chromsomes X/Y</li>"
				+ "<li>Variant transitions/transverions ratio</li>"
				+ "<li>Variant heterozygous/homozygous ratio</li>"
				+ "<li>Variant count</li>"
				+ "<li>...</li>"
				+ "</ul></html>");
		viewRunStatisticsCharts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewRunStatisticsCharts();
					}
				}, "ToolsPanel.viewRunStatisticsCharts").start();
			}
		});

		JButton viewVariantsDistributionCharts = new JButton(Resources.getScaledIcon(Resources.iChartDouble, 40));
		viewVariantsDistributionCharts.setPreferredSize(new Dimension(54,54));
		viewVariantsDistributionCharts.setToolTipText("<html><b>Variant distribution</b><br>"
				+ "This tool allow to make histograms and pie charts display variant distribution for a specific feature.<br>"
				+ "Start by building a filter, to base the charts on.<br>"
				+ "Then select a feature (a field available in Highlander).<br>"
				+ "The chart will then show how many variants of your query belong to which category of the feature.<br>"
				+ "For example, you select the field <i>snpeff_effect</i>, you can see wich proportion of variants is NON_SYNONYMOUS_CODING, STOP_GAINED, FRAME_SHIFT, etc.<br>"
				+ "If you select a field with percentage values, like variant frequency in a population, categories will adapt (some categories below 1% + 1 category each 10%)<br>"
				+ "Note that depending on the number of samples in your query, the tool can take a <b>very long time</b> to build the charts.</html>");
		viewVariantsDistributionCharts.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewVariantsDistributionCharts();
					}
				}, "ToolsPanel.viewVariantsDistributionCharts").start();
			}
		});

		JButton viewFastQC = new JButton(Resources.getScaledIcon(Resources.iFastQC, 40));
		viewFastQC.setPreferredSize(new Dimension(54,54));
		viewFastQC.setToolTipText("<html><b>FastQC reports</b><br>"
				+ "Select one or more NGS runs then go through the FastQC charts of those runs.<br>"
				+ "You can aggregate all samples in one window, so then you just select the chart you want to check, and all samples will be displayed at once.<br>"
				+ "You can then modify the size of each chart using the % dropdown, to display more charts at once on your screen.<br>"
				+ "If you prefer to check all charts from one sample at a time, select '<i>Sample selection with one window per sample</i>'</html>");
		viewFastQC.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						viewFastQC();
					}
				}, "ToolsPanel.viewFastQC").start();
			}
		});

		JButton ctdnaEstimation = new JButton(Resources.getScaledIcon(Resources.iCTDNA, 40));
		ctdnaEstimation.setPreferredSize(new Dimension(54,54));
		ctdnaEstimation.setToolTipText("<html><b>ctDNA estimation</b><br>"
				+ "You can use this tool on pairs of normal/tumor samples to estimate the circulating tumoral DNA proportion (using FACETS data).</html>");
		ctdnaEstimation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						ctdnaEstimation();
					}
				}, "ToolsPanel.ctdnaEstimation").start();
			}
		});

		JButton exomiser = new JButton(Resources.getScaledIcon(Resources.iExomiser, 40));
		exomiser.setPreferredSize(new Dimension(54,54));
		exomiser.setToolTipText("<html><b>Exomiser</b><br>"
				+ "The Exomiser is a Java program that finds potential disease-causing variants from whole-exome or whole-genome sequencing data. <br>"
				+ "Starting from a VCF file and a set of phenotypes encoded using the Human Phenotype Ontology (HPO) it will annotate, <br>"
				+ "filter and prioritise likely causative variants. The program does this based on user-defined criteria such as a variant's predicted pathogenicity, <br>"
				+ "frequency of occurrence in a population and also how closely the given phenotype matches the known phenotype of diseased genes from human and model organism data.<br>"
				+ "<br>"
				+ "Note that you can use Exomiser in two ways:<ul>"
				+ "<li>Sample mode: each sample is analyzed separately, but you can run a batch of samples at once with the same HPO terms.</li>"
				+ "<li>Family mode: you analyze a family as a whole (specifying the pedigree), but one at a time.</li>"
				+ "</ul>After that select your samples or families.<br>"
				+ "For each one, you'll see if you already have Exomiser results or if you need to run it.<br>"
				+ "If you have to run it, it will be on the cluster, not on your computer. You'll get an email when results are ready.</html>");
		exomiser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						exomiser();
					}
				}, "ToolsPanel.exomiser").start();
			}
		});
		
		JButton kraken = new JButton(Resources.getScaledIcon(Resources.iKraken, 40));
		kraken.setPreferredSize(new Dimension(54,54));
		kraken.setToolTipText("<html><b>Kraken</b><br>"
				+ "Kraken is a system for assigning taxonomic labels to short DNA sequences.</html>");
		kraken.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						kraken();
					}
				}, "ToolsPanel.kraken").start();
			}
		});
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));

		//Alignment tools
		panel.add(showAlignment);
		panel.add(showAlignmentPinned);
		panel.add(showInIGV);
		//panel.add(posInIGV);
		panel.add(viewBam);
		panel.add(exportSequence);		
		panel.add(getSeparator());
		
		//Annotation Tools
		panel.add(exomiser);
		panel.add(kraken);
		panel.add(variantAnnotator);
		panel.add(importHGMD);
		panel.add(getSeparator());

		//Cancer Tools
		if(Highlander.getDB().isBetaFunctionalitiesActivated())  panel.add(ctdnaEstimation);
		panel.add(getSeparator());
		
		//Other Tools
		//if(Highlander.getDB().isBetaFunctionalitiesActivated())  panel.add(burdenTest);	//TODO BURDEN - broken since v17, must rebuild the gonl/exac/gnomad databases
		//panel.add(getSeparator());

		//Export Tools
		panel.add(download);
		panel.add(exportToExcel);
		if(Highlander.getDB().isBetaFunctionalitiesActivated())  panel.add(exportToExcelWithNormalRC);
		panel.add(exportToTSV);
		panel.add(exportToVCF);		
		panel.add(getSeparator());
		
		//QC Tools
		panel.add(pedigreeChecker);
		panel.add(coverage);
		panel.add(viewRunStatisticsDetails);
		panel.add(viewRunStatisticsCharts);
		panel.add(viewVariantsDistributionCharts);
		panel.add(viewFastQC);

		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
		add(scrollablePanel, BorderLayout.CENTER);
	}

	private JPanel getSeparator() {
		JPanel separator = new JPanel();
		separator.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		separator.setPreferredSize(new Dimension(2, 50));
		return separator;
	}
	
	/**
	 * Alignment tools
	 */
	
	public void viewAlignment(boolean pinned) {
		Set<String> pinnedSamples = new TreeSet<>();
		if (pinned) {
			AskSamplesDialog askP = new AskSamplesDialog(false, Highlander.getCurrentAnalysis());
			Tools.centerWindow(askP, false);
			askP.setVisible(true);
			if (askP.getSelection() != null){
				pinnedSamples = askP.getSelection();
			}
		}
		if (mainFrame.getVariantTable().getSelectedVariantsId().isEmpty()) {
			AlignmentViewer av = new AlignmentViewer(null, -1, pinnedSamples);
			Tools.centerWindow(av, true);
			av.setVisible(true);
		}else {
			Map<Integer, String> samples = new HashMap<Integer, String>();
			try {
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT variant_sample_id, sample "
						+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
						+ Highlander.getCurrentAnalysis().getJoinProjects()
						+ "WHERE variant_sample_id IN ("+HighlanderDatabase.makeSqlList(mainFrame.getVariantTable().getSelectedVariantsId(), Integer.class)+")"
						)) {
					while (res.next()) {
						samples.put(res.getInt("variant_sample_id"), res.getString("sample"));
					}
				}
			} catch (Exception ex) {
				Tools.exception(ex);
			}
			Map<Variant, Set<String>> viewers = new TreeMap<Variant, Set<String>>();
			for (int id : mainFrame.getVariantTable().getSelectedVariantsId()) {
				try {
					Variant variant = new Variant(id);
					if (!viewers.containsKey(variant)) {
						viewers.put(variant, new TreeSet<String>(pinnedSamples));
					}
					if (samples.containsKey(id)) viewers.get(variant).add(samples.get(id));
				} catch (Exception ex) {
					Tools.exception(ex);
				}
			}
			for (Variant variant : viewers.keySet()) {
				AlignmentViewer av = new AlignmentViewer(variant.getChromosome(), variant.getAlternativePosition(), viewers.get(variant));
				Tools.centerWindow(av, true);
				av.setVisible(true);
			}
		}
	}

	public void viewInIGV(){
		OpenIGV oigv = new OpenIGV(mainFrame);
		Tools.centerWindow(oigv, false);
		oigv.setVisible(true);
	}

	public void bamView(){
		Object res = JOptionPane.showInputDialog(mainFrame, "Select a reference genome", "Reference genome selection", 
				JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iReference,64), Reference.getAvailableReferences().toArray(new Reference[0]), Highlander.getCurrentAnalysis().getReference());
		if (res != null) {
			Reference reference = (Reference)res;
			AskBamPosDialog ask = new AskBamPosDialog(mainFrame, reference);
			Tools.centerWindow(ask, false);
			ask.setVisible(true);
			if (!ask.getSelectedPositions().isEmpty() && !ask.getSelectedBAMs().isEmpty()){
				BamViewer viewer = new BamViewer(reference, ask.getSelectedBAMs(), ask.getSelectedPositions());
				Tools.centerWindow(viewer, false);
				viewer.setVisible(true);
			}
		}
	}

	public void exportSequence() {
		if (mainFrame.getVariantTable().getSelectedVariantsId().isEmpty()) {
			JOptionPane.showMessageDialog(new JFrame(), "Please select at least 1 variant", "Export mutated sequences",
					JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iExportSequence,64));
		}else {
			Highlander.waitingPanel.start();
			int selection = mainFrame.getVariantTable().getSelectedVariantsId().size();
			Highlander.waitingPanel.setProgressString("Exporting "+Tools.doubleToString(selection, 0, false)+" sequences", false);
			Highlander.waitingPanel.setProgressMaximum(selection);
			Object range = JOptionPane.showInputDialog(mainFrame, "How many amino acids around the reference do you want?", "Sequence range", 
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iExportSequence,64), null, 12);
			if (range != null) {
				try {
					int rangeAA = Integer.parseInt(range.toString());
					FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					chooser.setFile("sequences_" + df.format(System.currentTimeMillis())+".xlsx");		
					Tools.centerWindow(chooser, false);
					chooser.setVisible(true) ;
					if (chooser.getFile() != null) {
						String filename = chooser.getDirectory() + chooser.getFile();
						if (!filename.endsWith(".xlsx")) filename += ".xlsx";
						File xls = new File(filename);
						try{
							Workbook wb = new SXSSFWorkbook(100);  		
							Sheet sheet = wb.createSheet(Highlander.getCurrentAnalysis() + " " + df.format(System.currentTimeMillis()));
							sheet.createFreezePane(0, 1);		
							int r = 0;
							Row row = sheet.createRow(r++);
							String[] headers = new String[] {
									"variantSampleId",
									"sample",
									"chr",
									"pos",
									"ref",
									"alt",
									"gene",
									"strand",
									"hgvs",
									"effect",
									"nucleotides reference (forward)",
									"nucleotides mutation (forward)",
									"nucleotides reference (reverse complement)",
									"nucleotides mutation (reverse complement)",
									"amino acids reference",
									"amino acids mutation",
							};
							for (int c = 0 ; c < headers.length ; c++){
								Cell cell = row.createCell(c);
								cell.setCellValue(headers[c]);
							}
							sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length));							
							for (int id : mainFrame.getVariantTable().getSelectedVariantsId()) {
								Highlander.waitingPanel.setProgressValue(r);
								String sample = "?";
								String ensg = "?";
								String hgvs = "?";
								String eff = "?";
								String strand = "?";
								try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
										"SELECT " + Field.sample + ", " + Field.transcript_ensembl + ", " + Field.hgvs_protein + ", " + Field.snpeff_effect + " "
												+ "FROM " + Highlander.getCurrentAnalysis().getFromSampleAnnotations()
												+ Highlander.getCurrentAnalysis().getJoinStaticAnnotations()
												+ Highlander.getCurrentAnalysis().getJoinGeneAnnotations()
												+ Highlander.getCurrentAnalysis().getJoinProjects()
												+ "WHERE "+Field.variant_sample_id.getQueryWhereName(Highlander.getCurrentAnalysis(), false)+" = " + id
										)) {
									if (res.next()){
										sample = res.getString(1);
										ensg = res.getString(2);
										hgvs = (res.getObject(3) != null) ? res.getString(3) : "";
										eff = (res.getObject(4) != null) ? res.getString(4) : "";
									}else{
										throw new Exception("Id " + id + " not found in the database");
									}
								}
								Variant variant = new Variant(id);
								Reference genome = Highlander.getCurrentAnalysis().getReference();
								if (ensg != null) {
									Gene gene = new Gene(ensg, genome, variant.getChromosome(), true);
									strand = (gene.isStrandPositive()) ? "+" : "-";
									MutatedSequence seq = new MutatedSequence(variant, gene, genome, rangeAA);
									System.out.println(id + "\t" + sample + "\t" + seq.getVariant().getChromosome() + "\t" + seq.getVariant().getPosition() + "\t" + seq.getVariant().getReference() + "\t" + seq.getVariant().getAlternative() + "\t" + seq.getGene().getGeneSymbol() + "\t" + strand + "\t" + hgvs + "\t" + eff + "\t" + seq.getSequence(Type.NUCLEOTIDES, false, false) + "\t" + seq.getSequence(Type.NUCLEOTIDES, true, false) + "\t" + seq.getSequence(Type.NUCLEOTIDES, false, true) + "\t" + seq.getSequence(Type.NUCLEOTIDES, true, true) + "\t" + seq.getSequence(Type.AMINO_ACIDS, false, false) + "\t" + seq.getSequence(Type.AMINO_ACIDS, true, false));
									row = sheet.createRow(r++);								
									int c=0;
									Cell cell = row.createCell(c++);
									cell.setCellValue(id);
									cell = row.createCell(c++);
									cell.setCellValue(sample);
									cell = row.createCell(c++);
									cell.setCellValue(seq.getVariant().getChromosome() );
									cell = row.createCell(c++);
									cell.setCellValue(seq.getVariant().getPosition());
									cell = row.createCell(c++);
									cell.setCellValue(seq.getVariant().getReference());
									cell = row.createCell(c++);
									cell.setCellValue(seq.getVariant().getAlternative());
									cell = row.createCell(c++);
									cell.setCellValue(seq.getGene().getGeneSymbol());
									cell = row.createCell(c++);
									cell.setCellValue(strand);
									cell = row.createCell(c++);
									cell.setCellValue(hgvs);
									cell = row.createCell(c++);
									cell.setCellValue(eff);
									cell = row.createCell(c++);
									cell.setCellValue(seq.getSequence(Type.NUCLEOTIDES, false, false));
									cell = row.createCell(c++);
									cell.setCellValue(seq.getSequence(Type.NUCLEOTIDES, true, false));
									cell = row.createCell(c++);
									cell.setCellValue(seq.getSequence(Type.NUCLEOTIDES, false, true));
									cell = row.createCell(c++);
									cell.setCellValue(seq.getSequence(Type.NUCLEOTIDES, true, true));
									cell = row.createCell(c++);
									cell.setCellValue(seq.getSequence(Type.AMINO_ACIDS, false, false));
									cell = row.createCell(c++);
									cell.setCellValue(seq.getSequence(Type.AMINO_ACIDS, true, false));
								}else {
									System.out.println(id + "\t" + sample + "\t" + variant.getChromosome() + "\t" + variant.getPosition() + "\t" + variant.getReference() + "\t" + variant.getAlternative() + "\t" + "NO GENE");
									row = sheet.createRow(r++);								
									int c=0;
									Cell cell = row.createCell(c++);
									cell.setCellValue(id);
									cell = row.createCell(c++);
									cell.setCellValue(sample);
									cell = row.createCell(c++);
									cell.setCellValue(variant.getChromosome() );
									cell = row.createCell(c++);
									cell.setCellValue(variant.getPosition());
									cell = row.createCell(c++);
									cell.setCellValue(variant.getReference());
									cell = row.createCell(c++);
									cell.setCellValue(variant.getAlternative());
								}
							}
							Highlander.waitingPanel.setProgressValue(selection);
							Highlander.waitingPanel.setProgressString("Writing file ...",true);		
							try (FileOutputStream fileOut = new FileOutputStream(xls)){
								wb.write(fileOut);
							}
							Highlander.waitingPanel.setProgressDone();
						}catch (IOException ex){
							Highlander.waitingPanel.forceStop();
							Tools.exception(ex);
							JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Export mutated sequences",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}catch (Exception ex){
							Highlander.waitingPanel.forceStop();
							Tools.exception(ex);
							JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Export mutated sequences",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
					}	
				}catch (NumberFormatException ex) {
					Highlander.waitingPanel.forceStop();
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  range + " is not a valid number of amino acids", "Export mutated sequences",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}	
			Highlander.waitingPanel.stop();
		}
	}
	
	/**
	 * Annotation Tools
	 */
	
	public void exomiser(){
		try {
			Report exomiserReport = null;
			for (Report report : Report.getAvailableReports()) {
				if (report.getSoftware().equalsIgnoreCase("exomiser")) {
					exomiserReport = report;
				}
			}
			if (exomiserReport != null) {
				AnalysisFull exomes = (Highlander.getCurrentAnalysis().getSequencingTarget().equals("WES")) ? Highlander.getCurrentAnalysis() : null;
				if (exomes == null) {
					for (AnalysisFull analysis : AnalysisFull.getAvailableAnalyses()) {
						if (analysis.getSequencingTarget().equals("WES")) {
							exomes = analysis;
							break;
						}
					}
				}
				if (exomes != null) {
					Object choice = JOptionPane.showInputDialog(null,  "In which mode do you want to use Exomiser ?", "Exomiser",
							JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iExomiser,64), 
							Mode.values(),	Mode.SAMPLE);
					if (choice != null){
						Mode mode = Mode.valueOf(choice.toString());
						AskSamplesDialog askSamples = new AskSamplesDialog((mode == Mode.FAMILY), exomes);
						Tools.centerWindow(askSamples, false);
						askSamples.setVisible(true);
						if (askSamples.getSelection() != null){
							Set<String> samples = askSamples.getSelection();
							Exomiser exomiser = new Exomiser(exomiserReport, exomes, samples, mode);
							Tools.centerWindow(exomiser, false);
							exomiser.setVisible(true);
						}					
					}
				}else {
					JOptionPane.showMessageDialog(new JFrame(),  "No analysis with sequencing target 'WES' found", "Exomiser",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}else {
				JOptionPane.showMessageDialog(new JFrame(),  "No report named 'exomiser' has been found in Highlander, please contact your administrator", "Exomiser",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Exomiser",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}
	
	public void kraken(){
		try {
			Report krakenReport = null;
			for (Report report : Report.getAvailableReports()) {
				if (report.getSoftware().equalsIgnoreCase("kraken")) {
					krakenReport = report;
				}
			}
			if (krakenReport != null) {
				AskSamplesDialog askSamples = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), null, new TreeMap<AnalysisFull, Set<String>>(), true);
				Tools.centerWindow(askSamples, false);
				askSamples.setVisible(true);
				if (askSamples.getSelection() != null){
					Map<AnalysisFull, Set<String>> samples = askSamples.getMultiAnalysisSelection();
					Kraken kraken = new Kraken(krakenReport, samples);
					Tools.centerWindow(kraken, false);
					kraken.setVisible(true);
				}
			}else {
				JOptionPane.showMessageDialog(new JFrame(),  "No report named 'kraken' has been found in Highlander, please contact your administrator", "Kraken",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Kraken",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}
	
	public void variantAnnotator(){
		VariantAnnotator viewer = new VariantAnnotator();
		Tools.centerWindow(viewer, false);
		//viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		viewer.setVisible(true);
	}

	public void importHGMD() {
		FileDialog chooser = new FileDialog(new JFrame(), "Choose an HTML file saved from HGMD", FileDialog.LOAD);
		chooser.setVisible(true);
		if (chooser.getFile() != null) {			
				int results = ConverterHGMD.converterHGMD(new File(chooser.getDirectory() + "/" + chooser.getFile()));
				JOptionPane.showMessageDialog(new JFrame(),  results + " annotations have been imported from HGMD into public comments", "Importing HGMD",
						JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iHGMD,64));
		}
	}
	

	/**
	 * Cancer Tools
	 */
	
	public void ctdnaEstimation() {
		JOptionPane.showMessageDialog(new JFrame(), "In the next dialog, please select all TUMOR samples.\nYou will be able to associate NORMAL samples to them after.", "Sample selection", JOptionPane.INFORMATION_MESSAGE);
		AskSamplesDialog ask = new AskSamplesDialog(false, Highlander.getCurrentAnalysis());
		Tools.centerWindow(ask, false);
		ask.setVisible(true);
		if (ask.getSelection() != null && !ask.getSelection().isEmpty()){
			AskTumorNormalAssociation ask2 = new AskTumorNormalAssociation(Highlander.getCurrentAnalysis(), ask.getSelection());
			Tools.centerWindow(ask2, false);
			ask2.setVisible(true);
			if (ask2.getAssociation() != null && !ask2.getAssociation().isEmpty()){
				CtdnaEstimation ctDNA = new CtdnaEstimation(Highlander.getCurrentAnalysis(), ask2.getAssociation());
				Tools.centerWindow(ctDNA, true);
				ctDNA.setExtendedState(JFrame.MAXIMIZED_BOTH);
				ctDNA.setVisible(true);
			}
		}
	}

	/**
	 * Other Tools
	 */
	
	public void burdenTest(){
		AnalysisFull analysis = Highlander.getCurrentAnalysis();
		if (!Highlander.getDB().hasSchema(analysis.getReference(), Schema.ENSEMBL)){
			JOptionPane.showMessageDialog(new JFrame(),  "ENSEMBL schema is not accessible and is mandatory for using Burden test tool", "Burden test tool",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return;			
		}
		if (mainFrame.getCurrentFilter() == null){
			JOptionPane.showMessageDialog(new JFrame(),  "You must first create a custom filter.", "Burden test tool",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return;
		}
		ComboFilter filter = mainFrame.getCurrentFilter();
		if (!filter.isSimple() || filter.getFilter().getFilterType() != FilterType.CUSTOM){
			JOptionPane.showMessageDialog(new JFrame(),  "You cannot use magic filters with burden test, please remove them first.", "Burden test tool",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return;
		}
		List<Source> sources = new ArrayList<>();
		if (Highlander.getDB().hasSchema(analysis.getReference(), Schema.EXAC)) sources.add(Source.EXAC);
		if (Highlander.getDB().hasSchema(analysis.getReference(), Schema.GONL)) sources.add(Source.GONL);
		if (sources.isEmpty()){
			JOptionPane.showMessageDialog(new JFrame(),  "No source schema is available in the Highlander database (e.g. EXAC)", "Burden test tool",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return;
		}
		Object source = JOptionPane.showInputDialog(new JFrame(), "Select a source database", "Source data", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iBurdenTest,64), sources.toArray(new Source[0]), Source.EXAC);
		if (source != null){
			BurdenTest bt = new BurdenTest(mainFrame, (Source)source, (CustomFilter)(filter.getFilter()));
			Tools.centerWindow(bt, true);
			bt.setVisible(true);
		}
	}



	/** 
	 * Export Tools
	 */
	
	public void download(){
		FileDownloader fd = new FileDownloader();
		Tools.centerWindow(fd, false);
		fd.setVisible(true);
	}

	public void exportToExcel(){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		if (mainFrame.getCurrentFilterName() != null) chooser.setFile(Tools.formatFilename(mainFrame.getCurrentFilterName()) + ".xlsx");
		else chooser.setFile(Highlander.getCurrentAnalysis() + "_" + df.format(System.currentTimeMillis())+".xlsx");		
		Tools.centerWindow(chooser, false);
		chooser.setVisible(true) ;
		if (chooser.getFile() != null) {
			String filename = chooser.getDirectory() + chooser.getFile();
			if (!filename.endsWith(".xlsx")) filename += ".xlsx";
			File xls = new File(filename);
			try{
				mainFrame.getVariantTable().toXlsx(xls);
			}catch (IOException ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Exporting to Excel",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}catch (Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Exporting to Excel",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public void exportToTSV(){
		FileDialog chooser = new FileDialog(new JFrame(), "Output TSV file", FileDialog.SAVE) ;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		if (mainFrame.getCurrentFilterName() != null) chooser.setFile(Tools.formatFilename(mainFrame.getCurrentFilterName()) + ".tsv");
		else chooser.setFile(Highlander.getCurrentAnalysis() + "_" + df.format(System.currentTimeMillis())+".tsv");		
		Tools.centerWindow(chooser, false);
		chooser.setVisible(true) ;
		if (chooser.getFile() != null) {
			String filename = chooser.getDirectory() + chooser.getFile();
			if (!filename.endsWith(".tsv")) filename += ".tsv";
			File tsv = new File(filename);
			try{
				mainFrame.getVariantTable().toTSV(tsv);
			}catch (IOException ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Exporting to TSV",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}catch (Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Exporting to TSV",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public void exportToVCF(){
		Object sampleSplit = JOptionPane.showInputDialog(null,  "Do you want one multi-sample VCF with all samples,\nor one VCF per sample ?", "Exporting to VCF",
				JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iVCF,64), new String[]{"Multi-sample VCF","One VCF per sample"}, "Multi-sample VCF");
		if (sampleSplit.toString().equals("Multi-sample VCF")){
			FileDialog chooser = new FileDialog(new JFrame(), "Output VCF file", FileDialog.SAVE) ;
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			if (mainFrame.getCurrentFilterName() != null) chooser.setFile(Tools.formatFilename(mainFrame.getCurrentFilterName()) + ".vcf");
			else chooser.setFile(Highlander.getCurrentAnalysis() + "_" + df.format(System.currentTimeMillis())+".vcf");		
			Tools.centerWindow(chooser, false);
			chooser.setVisible(true) ;
			if (chooser.getFile() != null) {
				String filename = chooser.getDirectory() + chooser.getFile();
				if (!filename.endsWith(".vcf")) filename += ".vcf";
				File vcf = new File(filename);
				try{
					mainFrame.getVariantTable().toVCF(vcf);
				}catch (IOException ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Exporting to VCF",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}catch (Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Exporting to VCF",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}			
		}else {
			FileDialog chooser = new FileDialog(new JFrame(), "Output VCF file prefix", FileDialog.SAVE) ;
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			if (mainFrame.getCurrentFilterName() != null) chooser.setFile(Tools.formatFilename(mainFrame.getCurrentFilterName()));
			else chooser.setFile(Highlander.getCurrentAnalysis() + "_" + df.format(System.currentTimeMillis()));		
			Tools.centerWindow(chooser, false);
			chooser.setVisible(true) ;
			if (chooser.getFile() != null) {
				String filename = chooser.getDirectory() + chooser.getFile();
				if (filename.endsWith(".vcf")) filename = filename.substring(0, filename.length()-4);
				try{
					Set<String> samples = mainFrame.getVariantTable().getDistinctValues(Field.sample);
					if (samples.isEmpty()) {
						try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
								"SELECT "+Field.sample.getQuerySelectName(Highlander.getCurrentAnalysis(), false)+" "
										+ "FROM "+Highlander.getCurrentAnalysis().getFromSampleAnnotations()
										+ Highlander.getCurrentAnalysis().getJoinProjects()
										+" WHERE "+Field.variant_sample_id.getQueryWhereName(Highlander.getCurrentAnalysis(), false)+" in ("+HighlanderDatabase.makeSqlList(mainFrame.getVariantTable().getAllVariantsIds(), Integer.class)+")"
								)) {
							while (res.next()) {
								samples.add(res.getString(Field.sample.getName()));
							}
						}
					}
					for (String sample : samples) {
						File vcf = new File(filename + "_" + sample + ".vcf");
						mainFrame.getVariantTable().toVCF(vcf, sample);						
					}
				}catch (IOException ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Exporting to VCF",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}catch (Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Exporting to VCF",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		}
	}

	public void exportToExcelWithNormalRC(){
		if (Highlander.getCurrentAnalysis().getVariantCaller() != VariantCaller.MUTECT) {
			JOptionPane.showMessageDialog(new JFrame(),  "You must be in an analysis using MuTect caller", "Exporting to Excel with normal/tumor read count",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}else {
			FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			if (mainFrame.getCurrentFilterName() != null) chooser.setFile(Tools.formatFilename(mainFrame.getCurrentFilterName()) + ".xlsx");
			else chooser.setFile(Highlander.getCurrentAnalysis() + "_" + df.format(System.currentTimeMillis())+".xlsx");		
			Tools.centerWindow(chooser, false);
			chooser.setVisible(true) ;
			if (chooser.getFile() != null) {
				String filename = chooser.getDirectory() + chooser.getFile();
				if (!filename.endsWith(".xlsx")) filename += ".xlsx";
				File xls = new File(filename);
				try{
					mainFrame.getVariantTable().toXlsx(xls, true);
				}catch (IOException ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Exporting to Excel",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}catch (Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Exporting to Excel",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		}
	}
	
	
	/**
	 * QC Tools
	 */

	public void pedigreeChecker(){
		AskSamplesDialog askP = new AskSamplesDialog(false, Highlander.getCurrentAnalysis());
		Tools.centerWindow(askP, false);
		askP.setVisible(true);
		if (askP.getSelection() != null){
			Set<String> samples = askP.getSelection();
			PedigreeChecker pc = new PedigreeChecker(samples);
			Tools.centerWindow(pc, false);
			pc.setVisible(true);
		}
	}

	public void coverage(){
		CoverageInfo viewer = new CoverageInfo();
		Tools.centerWindow(viewer, false);
		//viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		viewer.setVisible(true);
	}

	public void viewRunStatisticsDetails(){
		RunStatistics viewer = new RunStatistics();
		Tools.centerWindow(viewer, false);
		viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		viewer.setVisible(true);
	}

	public void viewRunStatisticsCharts(){
		RunCharts viewer = new RunCharts();
		Tools.centerWindow(viewer, true);
		viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		viewer.setVisible(true);
	}

	public void viewVariantsDistributionCharts(){
		VariantDistributionCharts viewer = new VariantDistributionCharts();
		Tools.centerWindow(viewer, true);
		viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		viewer.setVisible(true);
	}

	public void viewFastQC(){
		try {
			Report fastqcReport = null;
			for (Report report : Report.getAvailableReports()) {
				if (report.getSoftware().equalsIgnoreCase("FastQC")) {
					fastqcReport = report;
				}
			}
			if (fastqcReport != null) {
				if (fastqcReport.getPath() == null || fastqcReport.getPath().length() == 0) {
					JOptionPane.showMessageDialog(this, "FastQC doesn't seem to be present on the Highlander server. \nAdministrator should create a report named (exactly) 'FastQC', and unzip FastQC report in the corresponding path.", "FastQC viewer", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}else {
					Object choice = JOptionPane.showInputDialog(null,  "How do you want to display FastQC reports ?", "View FastQC reports",
							JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iFastQC,64), 
							new String[]{"Run selection with all samples aggregated on one window","Sample selection with one window per sample"}, 
							"Run selection with all samples aggregated on one window");
					if (choice != null){
						if (choice.toString().startsWith("Run")){
							CreateRunSelection select = new CreateRunSelection();
							Tools.centerWindow(select, false);
							select.setVisible(true);
							if (!select.getSelection().isEmpty()){
								FastQCViewer viewer = new FastQCViewer(select.getSelection(), fastqcReport);
								Tools.centerWindow(viewer, true);
								viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
								viewer.setVisible(true);
							}
						}else if (choice.toString().startsWith("Sample")){
							AskSamplesDialog ask = new AskSamplesDialog(false, Highlander.getCurrentAnalysis());
							Tools.centerWindow(ask, false);
							ask.setVisible(true);
							if (ask.getSelection() != null){
								for (String sample : ask.getSelection()){
									try {
										try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT run_label FROM projects WHERE sample = '"+sample+"'")) {
											while (res.next()){
												String project = res.getString("run_label");
												final String url = Highlander.getParameters().getUrlForReports()+"/"+project+"/fastqc/"+sample+"/fastqc_report.html";
												new Thread(new Runnable(){
													public void run(){
														Tools.openURL(url);
													}
												}, "ToolsPanel.openURL").start();								
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}		
							}
						}
					}
				}			
			}else {
				JOptionPane.showMessageDialog(new JFrame(),  "No report named 'FastQC' has been found in Highlander, please contact your administrator", "View FastQC reports",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "View FastQC reports",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}



}
