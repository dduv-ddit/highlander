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
import be.uclouvain.ngs.highlander.UI.tools.FastQCViewer;
import be.uclouvain.ngs.highlander.UI.tools.FileDownloader;
import be.uclouvain.ngs.highlander.UI.tools.OpenIGV;
import be.uclouvain.ngs.highlander.UI.tools.PedigreeChecker;
import be.uclouvain.ngs.highlander.UI.tools.RunCharts;
import be.uclouvain.ngs.highlander.UI.tools.RunStatistics;
import be.uclouvain.ngs.highlander.UI.tools.VariantAnnotator;
import be.uclouvain.ngs.highlander.UI.tools.VariantDistributionCharts;
import be.uclouvain.ngs.highlander.UI.tools.BurdenTest.Source;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
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
		viewBam.setToolTipText("View ALL selected positions in a selection of BAM files");
		viewBam.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						bamView();
					}
				}, "ToolsPanel.bamView").start();
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
		burdenTest.setToolTipText("Burden test using current custom filters");
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
		pedigreeChecker.setToolTipText("Check familial relationships between samples");
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
		coverage.setToolTipText("Get average coverage (vertical and horizontal) for a given set of genes");
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
		variantAnnotator.setToolTipText("Get all annotations for a specified variant");
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
		download.setToolTipText("Download original data files, like BAM or VCF (for current analysis)");
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

		JButton viewRunStatisticsDetails = new JButton(Resources.getScaledIcon(Resources.iRunStatisticsDetails, 40));
		viewRunStatisticsDetails.setPreferredSize(new Dimension(54,54));
		viewRunStatisticsDetails.setToolTipText("View ALL runs detailled statistics");
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
		viewRunStatisticsCharts.setToolTipText("View ALL runs statistics charts");
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
		viewVariantsDistributionCharts.setToolTipText("View variants distribution charts");
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
		viewFastQC.setToolTipText("View FastQC reports");
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
		ctdnaEstimation.setToolTipText("ctDNA estimation");
		ctdnaEstimation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						ctdnaEstimation();
					}
				}, "ToolsPanel.ctdnaEstimation").start();
			}
		});

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));

		panel.add(showAlignment);
		panel.add(showAlignmentPinned);
		panel.add(showInIGV);
		//panel.add(posInIGV);
		panel.add(viewBam);
		if(Highlander.getDB().isBetaFunctionalitiesActivated())  panel.add(burdenTest);	//TODO BURDEN - broken since v17, must rebuild the gonl/exac/gnomad databases
		if(Highlander.getDB().isBetaFunctionalitiesActivated())  panel.add(ctdnaEstimation);
		panel.add(pedigreeChecker);
		panel.add(coverage);
		panel.add(variantAnnotator);
		panel.add(download);
		panel.add(exportToExcel);
		if(Highlander.getDB().isBetaFunctionalitiesActivated())  panel.add(exportToExcelWithNormalRC);
		panel.add(exportToTSV);
		panel.add(exportToVCF);
		panel.add(viewRunStatisticsDetails);
		panel.add(viewRunStatisticsCharts);
		panel.add(viewVariantsDistributionCharts);
		panel.add(viewFastQC);

		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
		add(scrollablePanel, BorderLayout.CENTER);
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

	public void variantAnnotator(){
		VariantAnnotator viewer = new VariantAnnotator();
		Tools.centerWindow(viewer, false);
		//viewer.setExtendedState(JFrame.MAXIMIZED_BOTH);
		viewer.setVisible(true);
	}
	
	public void download(){
		FileDownloader fd = new FileDownloader();
		Tools.centerWindow(fd, false);
		fd.setVisible(true);
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
		Report fastqcReport = null;
		try {
			fastqcReport = new Report("FastQC");
		}catch(Exception ex) {
			ex.printStackTrace();
		}
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
	}

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

}
