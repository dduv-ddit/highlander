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

package be.uclouvain.ngs.highlander.UI.misc;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
//import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.tools.BamViewer;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.tools.ViewBam;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

public class AlignmentPanel extends JPanel {

	public enum ColorBy {STRAND, SAMPLE, READ_GROUP}

	private final static int READ_LIMIT = 50_000; //Too long and useless to display so much reads. 

	private final static int A=0;
	private final static int C=1;
	private final static int G=2;
	private final static int T=3;
	private final static int N=4;
	private final static int DEL=5;
	private final static int INS=6;

	private int THRESHOLD_MAPQV = 60;
	private int THRESHOLD_PHRED = 30;

	private final static Color forward = new Color(230,150,150);
	private final static Color reverse = new Color(150,150,230);
	private final static Color insertion = new Color(118,24,220);
	private final static Map<Character, Color> colors = new HashMap<Character, Color>(); 
	private final static Color softClippedBase = Color.DARK_GRAY;
	private String reference;
	private Map<Integer, List<Read>> reads = new TreeMap<Integer, List<Read>>();
	private Interval interval;
	private Variant highlightedVariant;
	private int preferredWidth;
	private boolean showSoftClippedBases;
	private boolean squished;
	private boolean showModifiedAA;
	private ColorBy colorBy;
	private boolean drawReference = true;
	private String highlightPair = null;
	private Map<String, Color> readColors = new HashMap<String, Color>();
	private String highlightedPosTooltip;
	private Map<Rectangle,String> tooltipsBack = new HashMap<Rectangle, String>();
	private Map<Rectangle,String> tooltipsFront = new HashMap<Rectangle, String>();
	private Map<Rectangle,Read> readMap = new HashMap<Rectangle, Read>();
	private Map<Rectangle,Integer> readPosMap = new HashMap<Rectangle, Integer>();
	private Map<Point,Integer> positions = new HashMap<Point, Integer>();
	private Map<String, Gene> geneDataset = new LinkedHashMap<String, Gene>();

	private int baseWidth = 4;
	private int interWidth = 6;
	private int readBaseHeight;
	private int baseHeight = 12;
	private int totalHeight = baseHeight + 2;
	private int interReadHeight;
	private int readTotalHeight;

	private int neededWidth;
	private int neededHeight;

	private Image cachedImageOriginal = null;
	private Image cachedImageResized = null;
	private int cachedImageHeight = -1;
	private int cachedImageWidth = -1;

	public Color[] presetcolors = {
			new Color(0,191,255) /*DeepSkyBlue*/,
			new Color(255,127,0) /*coral*/,
			new Color(255,215,0) /*gold*/,
			new Color(255,20,147) /*DeepPink*/,
			new Color(0,206,209) /*dark turquoise*/,
			new Color(133,99,99) /*Light Wood*/,
			new Color(224,102,255) /*MediumOrchid1*/,
			new Color(255,48,48) /*firebrick1*/,
			new Color(0,250,154) /*MediumSpringGreen*/,
			new Color(16,78,139) /*DodgerBlue4*/,
			new Color(139,139,0) /*yellow4*/,
			Color.pink,
			Color.orange,
			Color.lightGray,
			new Color(46,139,87) /*SeaGreen*/,
			new Color(255,127,36) /*chocolate1*/,
			new Color(127,255,0) /*chartreuse*/,
			Color.yellow,
			new Color(152,245,255) /*CadetBlue1*/,
			Color.blue,
			new Color(139,69,19) /*SaddleBrown*/,
			Color.magenta,
			new Color(202,255,112) /*DarkOliveGreen1*/,
			new Color(138,43,226) /*BlueViolet*/,
			new Color(127,255,212) /*aquamarine*/,
			Color.red,
			new Color(221,160,221) /*plum*/,
			new Color(187,255,255) /*PaleTurquoise1*/,
			new Color(255,62,150) /*VioletRed1*/,
			new Color(255,160,122) /*light salmon*/,
			Color.green,
			new Color(205,201,165) /*LemonChiffon3*/,
			Color.cyan,
			new Color(255,36,0) /*Orange Red*/,
	};

	public AlignmentPanel(SAMRecordIterator SAMRecords, Interval interval, int width, JProgressBar progress){
		this(SAMRecords, interval, null, false, false, false, ColorBy.STRAND, true, width, progress);
	}

	public AlignmentPanel(SAMRecordIterator SAMRecords, Interval interval, Variant highlightedVariant, boolean showSoftClippedBases, boolean squished, boolean frameShift, ColorBy colorBy, boolean drawReference, int width, JProgressBar progress){
		super();
		progress.setIndeterminate(true);
		this.highlightedVariant = highlightedVariant;
		this.showSoftClippedBases = showSoftClippedBases;
		this.squished = squished;
		this.showModifiedAA = frameShift;
		this.colorBy = colorBy;
		this.drawReference = drawReference;
		this.preferredWidth = width;
		colors.put('A', new Color(0,150,0));
		colors.put('C', new Color(0,0,255));
		colors.put('G', new Color(209,113,5));
		colors.put('T', new Color(255,0,0));
		colors.put('N',Color.LIGHT_GRAY);
		setToolTipText("");
		progress.setString("Loading reference sequence");
		progress.setStringPainted(true);		
		try {
			reference = DBUtils.getSequence(interval.getReferenceGenome(), interval.getChromosome(), interval.getStart(), interval.getEnd());
		} catch (Exception ex) {
			Tools.exception(ex);
		}
		String highlightedRef = 
				(highlightedVariant != null 
				&& highlightedVariant.getAlternativePosition()-interval.getStart() < reference.length() 
				&& highlightedVariant.getAlternativePosition()-interval.getStart() >= 0) 
				? ""+reference.charAt(highlightedVariant.getAlternativePosition()-interval.getStart()) : "";
				Map<String, List<Read>> readsPerPattern = new TreeMap<String, List<Read>>();
				progress.setString("Loading reads");
				int colorIndex = 0;
				while(SAMRecords.hasNext()){
					SAMRecord rec = SAMRecords.next();
					Read read = new Read(rec);
					if(!read.readGroup.equals("ArtificialHaplotype")) {
						Optional<String> optionalPattern = 
								(highlightedVariant != null 
								&& highlightedVariant.getAlternativePosition() >= interval.getStart() 
								&& (highlightedVariant.getAlternativePosition()+highlightedVariant.getAffectedReferenceLength()) < interval.getEnd()) 
								? ViewBam.getPattern(rec, highlightedVariant.getAlternativePosition(), highlightedVariant.getAlternativePosition()+highlightedVariant.getAffectedReferenceLength(), showSoftClippedBases) 
								: Optional.empty(); 
						String pattern = (optionalPattern.isPresent()) ? optionalPattern.get() : "";
						if (!readsPerPattern.containsKey(pattern)){
							readsPerPattern.put(pattern, new ArrayList<Read>());
						}
						readsPerPattern.get(pattern).add(read);
						if (colorBy == ColorBy.READ_GROUP) {
							if (!readColors.containsKey(read.readGroup)) {
								if (colorIndex < presetcolors.length) readColors.put(read.readGroup, presetcolors[colorIndex++]);
								else readColors.put(read.readGroup, new Color(new Random().nextInt(0xFFFFFF)));
							}
						}else if (colorBy == ColorBy.SAMPLE) {
							if (!readColors.containsKey(read.sample)) {
								if (colorIndex < presetcolors.length) readColors.put(read.sample, presetcolors[colorIndex++]);
								else readColors.put(read.sample, new Color(new Random().nextInt(0xFFFFFF)));
							}					
						}
					}
				}
				int[] numPatternsFwd = new int[7];
				int[] numPatternsRev = new int[7];
				for (String key : readsPerPattern.keySet()){
					int index = -1;
					switch(key){
					case "A":
						index = A;
						break;
					case "C":
						index = C;
						break;
					case "G":
						index = G;
						break;
					case "T":
						index = T;
						break;
					case "N":
						index = N;
						break;
					case "-":
						index = DEL;
						break;
					default:
						if (key.length() > 1){
							switch(key.replaceAll("[a-z]", "").trim()){
							case "A":
								index = A;
								break;
							case "C":
								index = C;
								break;
							case "G":
								index = G;
								break;
							case "T":
								index = T;
								break;
							case "N":
								index = N;
								break;
							case "-":
								index = DEL;
								break;
							}					
						}
						break;
					}
					if (index >= 0){
						for (Read read : readsPerPattern.get(key)){
							if (read.negativeStrandFlag){
								numPatternsRev[index]++;
							}else{
								numPatternsFwd[index]++;
							}
						}
					}
					if (key.length() > 1){
						for (Read read : readsPerPattern.get(key)){
							if (read.negativeStrandFlag){
								numPatternsRev[INS]++;
							}else{
								numPatternsFwd[INS]++;
							}
						}
					}
				}
				StringBuilder sb = new StringBuilder();
				sb.append("<html>");
				int total = numPatternsFwd[A] + numPatternsFwd[C] + numPatternsFwd[G] + numPatternsFwd[T] + numPatternsFwd[N] + 
						numPatternsRev[A] + numPatternsRev[C] + numPatternsRev[G] + numPatternsRev[T] + numPatternsRev[N];
				sb.append("Chr "+interval.getChromosome()+" : "+((highlightedVariant != null)?Tools.intToString(highlightedVariant.getAlternativePosition()):Tools.intToString(interval.getStart()))+"<br>");
				sb.append("Total count : " + total + "<br>");
				sb.append("A : " + (numPatternsFwd[A]+numPatternsRev[A]) + 
						((numPatternsFwd[A]+numPatternsRev[A] > 0)?" ("+Tools.doubleToPercent((double)(numPatternsFwd[A]+numPatternsRev[A])/(double)total, 0)+", "+numPatternsFwd[A]+"+, "+numPatternsRev[A]+"-)":"")
						+"<br>");
				sb.append("C : " + (numPatternsFwd[C]+numPatternsRev[C]) + 
						((numPatternsFwd[C]+numPatternsRev[C] > 0)?" ("+Tools.doubleToPercent((double)(numPatternsFwd[C]+numPatternsRev[C])/(double)total, 0)+", "+numPatternsFwd[C]+"+, "+numPatternsRev[C]+"-)":"")
						+"<br>");
				sb.append("G : " + (numPatternsFwd[G]+numPatternsRev[G]) + 
						((numPatternsFwd[G]+numPatternsRev[G] > 0)?" ("+Tools.doubleToPercent((double)(numPatternsFwd[G]+numPatternsRev[G])/(double)total, 0)+", "+numPatternsFwd[G]+"+, "+numPatternsRev[G]+"-)":"")
						+"<br>");
				sb.append("T : " + (numPatternsFwd[T]+numPatternsRev[T]) + 
						((numPatternsFwd[T]+numPatternsRev[T] > 0)?" ("+Tools.doubleToPercent((double)(numPatternsFwd[T]+numPatternsRev[T])/(double)total, 0)+", "+numPatternsFwd[T]+"+, "+numPatternsRev[T]+"-)":"")
						+"<br>");
				sb.append("N : " + (numPatternsFwd[N]+numPatternsRev[N]) + 
						((numPatternsFwd[N]+numPatternsRev[N] > 0)?" ("+Tools.doubleToPercent((double)(numPatternsFwd[N]+numPatternsRev[N])/(double)total, 0)+", "+numPatternsFwd[N]+"+, "+numPatternsRev[N]+"-)":"")
						+"<br>");
				sb.append("<br>");
				sb.append("DEL : " + (numPatternsFwd[DEL]+numPatternsRev[DEL]) + 
						((numPatternsFwd[DEL]+numPatternsRev[DEL] > 0)?" ("+numPatternsFwd[DEL]+"+, "+numPatternsRev[DEL]+"-)":"")
						+"<br>");
				sb.append("INS : " + (numPatternsFwd[INS]+numPatternsRev[INS]) + 
						((numPatternsFwd[INS]+numPatternsRev[INS] > 0)?" ("+numPatternsFwd[INS]+"+, "+numPatternsRev[INS]+"-)":"")
						+"<br>");
				sb.append("</html>");
				highlightedPosTooltip = sb.toString();
				int lastIndex = 1;
				progress.setIndeterminate(false);
				int count;
				//TODO LONGTERM - SV not shown in the alignment (not necessary, a dedicated visualization tool must be developped for SV)
				if (highlightedVariant != null && highlightedVariant.getAlternative() != null){
					//For INS, the pattern is in lower case, so we must search for it
					if (highlightedVariant.getVariantType() == VariantType.INS){
						for (Iterator<String> it = readsPerPattern.keySet().iterator() ; it.hasNext() ; ){
							String key = it.next();
							if (key.toUpperCase().equals(highlightedVariant.getAlternative())){
								progress.setString("Arrange insertions " + key);
								progress.setMaximum(readsPerPattern.get(key).size());
								count = 0;
								for(Read read : readsPerPattern.get(key)){
									progress.setValue(++count);
									if (!addReadToMap(read, lastIndex)) lastIndex++;
								}
								it.remove();
							}
						}		
					}
					//First place all reads showing the given alternative
					if (readsPerPattern.containsKey(highlightedVariant.getAlternativeChangedNucleotides())){
						progress.setString("Arrange reads with alternative " + highlightedVariant.getAlternativeChangedNucleotides());
						progress.setMaximum(readsPerPattern.get(highlightedVariant.getAlternativeChangedNucleotides()).size());
						count = 0;
						for(Read read : readsPerPattern.remove(highlightedVariant.getAlternativeChangedNucleotides())){
							progress.setValue(++count);
							if (!addReadToMap(read, lastIndex)) lastIndex++;
						}
					}
					//For SNVs, you can have an insertion just before the given alt, which is included in the pattern, but should count as a valid given alt
					if (highlightedVariant.getVariantType() == VariantType.SNV){
						for (Iterator<String> it = readsPerPattern.keySet().iterator() ; it.hasNext() ; ){
							String key = it.next();
							if (!key.equals("") && key.replaceAll("[a-z]", "").trim().equals(highlightedVariant.getAlternative())){
								progress.setString("Arrange reads with alternative and insertion " + key);
								progress.setMaximum(readsPerPattern.get(key).size());
								count = 0;
								for(Read read : readsPerPattern.get(key)){
									progress.setValue(++count);
									if (!addReadToMap(read, lastIndex)) lastIndex++;
								}
								it.remove();
							}
						}				
					}
				}
				//Place reads different from the given alt but different from reference
				for (Iterator<String> it = readsPerPattern.keySet().iterator() ; it.hasNext() ; ){
					String key = it.next();
					if (!key.equals("") && !key.equals(highlightedRef)){
						progress.setString("Arrange reads with other alternative " + key);
						progress.setMaximum(Math.min(readsPerPattern.get(key).size(), READ_LIMIT));
						count = 0;
						for(Read read : readsPerPattern.get(key)){
							progress.setValue(++count);
							if (!addReadToMap(read, lastIndex)) lastIndex++;
							if (count > READ_LIMIT) break; 
						}
						it.remove();
					}
				}
				//Place reads equals to reference at given pos
				if (readsPerPattern.containsKey(highlightedRef)){
					progress.setString("Arrange reads with reference " + highlightedRef);
					progress.setMaximum(Math.min(readsPerPattern.get(highlightedRef).size(), READ_LIMIT));
					count = 0;
					for(Read read : readsPerPattern.remove(highlightedRef)){
						progress.setValue(++count);
						if (!addReadToMap(read, lastIndex)) lastIndex++;
						if (count > READ_LIMIT) break; 
					}
				}
				//Place reads that do not cover the given pos
				if (readsPerPattern.containsKey("")){
					progress.setString("Arrange reads around position");
					progress.setMaximum(Math.min(readsPerPattern.get("").size(), READ_LIMIT));
					count = 0;
					for(Read read : readsPerPattern.remove("")){
						progress.setValue(++count);
						if (!addReadToMap(read, lastIndex)) lastIndex++;
						if (count > READ_LIMIT) break; 
					}
				}
				this.interval = interval;
				progress.setString("Done - Loading information from Ensembl DB");
				if (Highlander.getDB().hasSchema(interval.getReferenceGenome(), Schema.ENSEMBL)){
					try (Results res = Highlander.getDB().select(interval.getReferenceGenome(), Schema.ENSEMBL, 
							"SELECT transcript.stable_id "
									+ "FROM gene "
									+ "JOIN seq_region USING (seq_region_id) "
									+ "JOIN transcript ON gene.canonical_transcript_id = transcript.transcript_id "
									+ "WHERE `name` = '"+interval.getChromosome()+"' "
									+ "AND gene.seq_region_start <= "+interval.getEnd()+" "
									+ "AND gene.seq_region_end >= "+interval.getStart()+" "
									+ "ORDER BY gene.seq_region_start ASC")){
						while (res.next()){
							geneDataset.put(res.getString(1), new Gene(res.getString(1), interval.getReferenceGenome(), interval.getChromosome(), true));
						}
					}catch(Exception ex){
						Tools.exception(ex);
					}
				}else {
					System.err.println("ENSEMBL schema not present, cannot draw genes");
				}
				/*
		if (Highlander.getDB().hasSchema(Schema.UCSC)){
			try{
				try (Results res = Highlander.getDB().select(Schema.UCSC, "SELECT DISTINCT(name2) FROM refGene WHERE chrom = 'chr"+interval.getChromosome()+"' AND txStart <= "+interval.getStart()+" AND txEnd >= "+interval.getEnd())) {
				while (res.next()){
					geneData.put(res.getString(1), new GeneData(res.getString(1), interval.getChromosome(), null, null));
				}
				}
				for (String geneSymbol : geneData.keySet()){
					GeneData gd = geneData.get(geneSymbol);
					try (Results res = Highlander.getDB().select(Schema.UCSC, "SELECT * " +
							"FROM (SELECT * FROM refGene WHERE name2 = '"+geneSymbol+"' AND exonCount = (SELECT MAX(exonCount) FROM refGene WHERE name2 = '"+geneSymbol+"')) as t1 " +
							"WHERE txStart = (SELECT MIN(txStart) FROM (SELECT * FROM refGene WHERE name2 = '"+geneSymbol+"' AND chrom = 'chr"+gd.getChromosome()+"' AND exonCount = (SELECT MAX(exonCount) FROM refGene WHERE name2 = '"+geneSymbol+"')) as t2)")){
					if (res.next()){
						gd.setRefSeqTranscript(res.getString("name"));
						gd.setEnsemblTranscript("?");
					}
					}
					gd.setExonDataFromRefSeq();
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
				 */
				//Determine size of the component
				baseWidth = preferredWidth / interval.getSize();
				if (baseWidth < 4) baseWidth = 4;
				readBaseHeight = (squished) ? 1 : 12;
				interReadHeight = (squished) ? 0 : 2;
				readTotalHeight = readBaseHeight + interReadHeight;

				neededWidth = baseWidth * interval.getSize();
				neededHeight = (reads.size()+1)*readTotalHeight + geneDataset.size()*(readTotalHeight+5) + (readTotalHeight+5) + 10 + totalHeight*3;

				setSize(neededWidth, neededHeight);
				setPreferredSize(new Dimension(neededWidth, neededHeight));
				
				//add right-click popup menu
				MouseListener popupListener = new MouseListener() {
					public void mouseReleased(MouseEvent e) {}
					public void mousePressed(MouseEvent e) {}
					public void mouseExited(MouseEvent e) {}
					public void mouseEntered(MouseEvent e) {}
					public void mouseClicked(MouseEvent e) {
						if (e.getButton() == MouseEvent.BUTTON1) {
							getPopupMenu(e).show(e.getComponent(), e.getX(), e.getY());
						}
					}
				};
				this.addMouseListener(popupListener);
	}

	private JPopupMenu getPopupMenu(MouseEvent me) {
		Read rd = null;
		for (Rectangle r : readMap.keySet()){
			if (me.getX() >= r.getX() && me.getX() <= r.getX()+r.getWidth() && me.getY() >= r.getY() && me.getY() <= r.getY()+r.getHeight()){
				rd = readMap.get(r);
				break;
			}
		}
		int rp = -1;
		for (Rectangle r : readPosMap.keySet()){
			if (me.getX() >= r.getX() && me.getX() <= r.getX()+r.getWidth() && me.getY() >= r.getY() && me.getY() <= r.getY()+r.getHeight()){
				rp = readPosMap.get(r);
				break;
			}
		}
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(new JLabel("<html><b>Reference</b> ("+reference.length()+" bases displayed)</html>"));
		
		JMenuItem itemCopyReference = new JMenuItem("Copy reference sequence");
		itemCopyReference.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Tools.setClipboard(reference);
			}
		});
		popupMenu.add(itemCopyReference);
		
		if (rd != null) {
			final Read read = rd;
			popupMenu.addSeparator();
			popupMenu.add(new JLabel("<html><b>Read</b> (MAPQV="+read.mapQV+")</html>"));
			
			JMenu menuReadInfo = new JMenu("Read info");
			popupMenu.add(menuReadInfo);
			JMenuItem itemCopyReadName = new JMenuItem("Copy name");
			itemCopyReadName.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.setClipboard(read.name);
				}
			});
			menuReadInfo.add(itemCopyReadName);
			JMenuItem itemCopyCigar = new JMenuItem("Copy CIGAR");
			itemCopyCigar.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.setClipboard(read.cigarString);
				}
			});
			menuReadInfo.add(itemCopyCigar);
			JMenuItem itemCopyAll = new JMenuItem("Copy all");
			itemCopyAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.setClipboard(read.toString());
				}
			});
			menuReadInfo.add(itemCopyAll);
			menuReadInfo.addSeparator();
			menuReadInfo.add(new JLabel(
					"<html><BODY style=\"font-family: Verdana; font-size: 9px; \"><table border=0 cellspacing=0>"
					+"<tr><td>"+"Name: " +"</td><td>"+ read.name + "</td></tr><br>"
					+"<tr><td>"+"Alignment start: " +"</td><td>" + read.alignmentStart + "</td></tr><br>"
					+"<tr><td>"+"Alignment end: " +"</td><td>" + read.alignmentEnd + "</td></tr><br>"
					+"<tr><td>"+"Cigar: " +"</td><td>" + read.cigarString + "</td></tr><br>"
					+"<tr><td>"+"Negative strand: " +"</td><td>" + read.negativeStrandFlag + "</td></tr><br>"
					+"<tr><td>"+"Mapping quality: " +"</td><td>" + read.mapQV + "</td></tr><br>"
					+"<tr><td>"+"Pair: " +"</td><td>" + (read.isPaired ? (read.isFirstInPair ? "first in pair" : (read.isSecondInPair ? "second in pair" : "paired")) : "not paired") + "</td></tr><br>"
					+"</table></BODY></html>"
					));
			
			JMenu menuCopySequence = new JMenu("Copy read sequence");
			popupMenu.add(menuCopySequence);
			JMenuItem itemCopySequence = new JMenuItem("without soft-clipped bases");
			itemCopySequence.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.setClipboard(read.getSequence(true, false, false, true));
				}
			});
			menuCopySequence.add(itemCopySequence);
			JMenuItem itemCopyFullRead = new JMenuItem("with soft-clipped bases");
			itemCopyFullRead.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.setClipboard(read.getSequence(true, true, false, true));
				}
			});
			menuCopySequence.add(itemCopyFullRead);
			JMenuItem itemCopySoftClipped = new JMenuItem("soft-clipped bases only");
			itemCopySoftClipped.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.setClipboard(read.getSequence(false, true, false, false));
				}
			});
			menuCopySequence.add(itemCopySoftClipped);
			
			JMenu menuBlatHg38 = new JMenu("BLAT on hg38");
			popupMenu.add(menuBlatHg38);
			JMenuItem itemBlatHg38Read = new JMenuItem("without soft-clipped bases");
			itemBlatHg38Read.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Human;db=hg38;type=DNA;userSeq="+read.getSequence(true, false, false, true));
				}
			});
			menuBlatHg38.add(itemBlatHg38Read);
			JMenuItem itemBlatHg38Full = new JMenuItem("with soft-clipped bases");
			itemBlatHg38Full.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Human;db=hg38;type=DNA;userSeq="+read.getSequence(true, true, false, true));
				}
			});
			menuBlatHg38.add(itemBlatHg38Full);
			JMenuItem itemBlatHg38SC = new JMenuItem("soft-clipped bases only");
			itemBlatHg38SC.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Human;db=hg38;type=DNA;userSeq="+read.getSequence(false, true, false, true));
				}
			});
			menuBlatHg38.add(itemBlatHg38SC);
			
			JMenu menuBlatHg19 = new JMenu("BLAT on hg19");
			popupMenu.add(menuBlatHg19);
			JMenuItem itemBlatHg19Read = new JMenuItem("without soft-clipped bases");
			itemBlatHg19Read.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Human;db=hg19;type=DNA;userSeq="+read.getSequence(true, false, false, true));
				}
			});
			menuBlatHg19.add(itemBlatHg19Read);
			JMenuItem itemBlatHg19Full = new JMenuItem("with soft-clipped bases");
			itemBlatHg19Full.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Human;db=hg19;type=DNA;userSeq="+read.getSequence(true, true, false, true));
				}
			});
			menuBlatHg19.add(itemBlatHg19Full);
			JMenuItem itemBlatHg19SC = new JMenuItem("soft-clipped bases only");
			itemBlatHg19SC.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Human;db=hg19;type=DNA;userSeq="+read.getSequence(false, true, false, true));
				}
			});
			menuBlatHg19.add(itemBlatHg19SC);
			
			JMenu menuBlatMm10 = new JMenu("BLAT on mm10");
			popupMenu.add(menuBlatMm10);
			JMenuItem itemBlatMm10Read = new JMenuItem("without soft-clipped bases");
			itemBlatMm10Read.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Mouse;db=mm10;type=DNA;userSeq="+read.getSequence(true, false, false, true));
				}
			});
			menuBlatMm10.add(itemBlatMm10Read);
			JMenuItem itemBlatMm10Full = new JMenuItem("with soft-clipped bases");
			itemBlatMm10Full.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Mouse;db=mm10;type=DNA;userSeq="+read.getSequence(true, true, false, true));
				}
			});
			menuBlatMm10.add(itemBlatMm10Full);
			JMenuItem itemBlatMm10SC = new JMenuItem("soft-clipped bases only");
			itemBlatMm10SC.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Tools.openURL(
							"https://genome.ucsc.edu/cgi-bin/hgBlat?command=start;org=Mouse;db=mm10;type=DNA;userSeq="+read.getSequence(false, true, false, true));
				}
			});
			menuBlatMm10.add(itemBlatMm10SC);
			
			JMenu menuPair = new JMenu("Pair");
			popupMenu.add(menuPair);
			JMenuItem itemPairHighlight = new JMenuItem("Highlight");
			itemPairHighlight.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					highlightPair = read.name;	
					repaint();
				}
			});
			menuPair.add(itemPairHighlight);
			JMenuItem itemPairHighlightStop = new JMenuItem("Clear highlight");
			itemPairHighlightStop.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					highlightPair = null;
					repaint();
				}
			});
			menuPair.add(itemPairHighlightStop);
			
			if (rp != -1) {
				final int readPos = rp;
				popupMenu.addSeparator();
				popupMenu.add(new JLabel("<html><b>Base</b> " + read.bases[readPos] + " (qual "+read.getPhredScore(readPos)+")</html>"));
				
				JMenuItem itemCopyReadPosition = new JMenuItem("Copy position on read");
				itemCopyReadPosition.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Tools.setClipboard(readPos+"");
					}
				});
				popupMenu.add(itemCopyReadPosition);
				
				JMenuItem itemCopyRefPosition = new JMenuItem("Copy position on chromosome");
				itemCopyRefPosition.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Tools.setClipboard(""+getPosition(me.getPoint()));
					}
				});
				popupMenu.add(itemCopyRefPosition);
				
				JMenuItem itemCopyChrPosition = new JMenuItem("Copy chr:pos");
				itemCopyChrPosition.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Tools.setClipboard(interval.getChromosome()+":"+getPosition(me.getPoint()));
					}
				});
				popupMenu.add(itemCopyChrPosition);
			}
		}
		return popupMenu;
	}
	
	private boolean addReadToMap(Read read, int lastIndex){
		int gap = 2; // minimum gap between 2 reads on the same line 
		boolean foundIndex = false;
		int i = 1;
		while (i < lastIndex && !foundIndex){
			foundIndex = true;
			for (Read r : reads.get(i)){
				if (!(read.alignmentStart < r.alignmentStart-gap && read.alignmentEnd < r.alignmentStart-gap) 
						&& !(read.alignmentStart > r.alignmentEnd+gap && read.alignmentEnd > r.alignmentEnd+gap)) {
					foundIndex = false;
					break;
				}
			}
			if (!foundIndex) i++;
		}
		if (!reads.containsKey(i)){
			reads.put(i, new ArrayList<Read>());
		}
		reads.get(i).add(read);
		return foundIndex;
	}

	private class Read{
		public String name;
		public String sample;
		public String readGroup;
		public int alignmentStart;
		public int alignmentEnd;
		public char[] bases;
		public String cigarString;
		public boolean negativeStrandFlag;
		public int mapQV;
		public char[] baseQual;
		public boolean isPaired;
		public boolean isFirstInPair;
		public boolean isSecondInPair;

		public Read(SAMRecord rec){
			name = rec.getReadName();
			sample = rec.getReadGroup().getSample();
			readGroup = rec.getReadGroup().getReadGroupId();
			alignmentStart = (showSoftClippedBases) ? rec.getUnclippedStart() : rec.getAlignmentStart();
			alignmentEnd = (showSoftClippedBases) ? rec.getUnclippedEnd() : rec.getAlignmentEnd();
			bases = rec.getReadString().toCharArray();
			cigarString = rec.getCigarString();
			negativeStrandFlag = rec.getReadNegativeStrandFlag();
			mapQV = rec.getMappingQuality();
			baseQual = rec.getBaseQualityString().toCharArray();
			isPaired = rec.getReadPairedFlag();
			if (isPaired) {
				isFirstInPair = rec.getFirstOfPairFlag();
				isSecondInPair = rec.getSecondOfPairFlag();
			}else {
				isFirstInPair = false;
				isSecondInPair = false;				
			}
		}

		public int getPhredScore(int readPos){
			return ((int)baseQual[readPos])-33;
		}

		public Color getBaseColor(int readPos, boolean shadeByPhred){
			char readBase = (readPos < bases.length) ? bases[readPos] : 'N';
			int trans = Math.min(255, (getPhredScore(readPos) * 205/THRESHOLD_PHRED)+50);
			if (shadeByPhred) return new Color(colors.get(readBase).getRed(), colors.get(readBase).getGreen(), colors.get(readBase).getBlue(), trans);
			else return colors.get(readBase);
		}

		public String getSequence(boolean includeNormal, boolean includeSoftClipped, boolean includeDeletionsAsGaps, boolean includeInsertions) {
			StringBuilder sequence = new StringBuilder();
			char[] cigar = cigarString.toCharArray();
			int readPos=0;
			if (cigar[0] != '*'){
				for (int c=0 ; c < cigar.length ; c++){
					String times = "";
					while (Character.isDigit(cigar[c])){
						times += cigar[c++];
					}
					for (int t=0 ; t < Integer.parseInt(times) ; t++){
						char readBase = (readPos < bases.length) ? bases[readPos] : 'N';
						switch(cigar[c]){
						case 'M':
						case '=':
						case 'X':
							//Alignment match (can be a sequence match or mismatch)
							if (includeNormal) {
								sequence.append(readBase);
							}
							readPos++;
							break;
						case 'I':
							//Insertion to the reference
							if (includeInsertions && t == 0){
								//first inserted base
								for (int u=0 ; u < Integer.parseInt(times) ; u++){
									if (readPos+u < bases.length) {
										sequence.append(bases[readPos+u]); 
									}else{ 
										sequence.append('N');
									}
								}
							}
							readPos++;
							break;
						case 'D':
							//Deletion from the reference
							if (includeDeletionsAsGaps) {
								sequence.append("-");
							}
							break;
						case 'S':
							//Soft clipping (base present in the read)
							//don't add the base (generate useless patterns), increase index in the read (not in ref or on screen, we get alignment start/end, not unclipped start/end)
							//If show soft-clipped bases has been selected, act as normal base
							if (includeSoftClipped) {
								sequence.append(readBase);
							}
							readPos++;
							break;
						case 'H':
							//Hard clipping (base NOT present in the read)
							break;
						case 'N':
							//skipped region from the reference
							//For mRNA-to-genome alignment, an N operation represents an intron. For other types of alignments, the interpretation of N is not defined.
							//!!!! Never encountered, so never tested !!!!
							break;
						case 'P':
							//Padding (silent deletion from padded reference)
							//Nothing to do, inexistant in the ref, inexistant in the read. Only useful with multiple alignments.
							//!!!! Never encountered, so never tested !!!!
							break;
						default:
							System.err.println("Unsupported CIGAR operation : " + cigar[c]);
							break;
						}
					}
				}
			}
			return sequence.toString();
		}
		
		public Color getReadColory(boolean shadeByMapQV){
			int trans = Math.min(255, (mapQV * 205/THRESHOLD_MAPQV)+50);
			if (highlightPair != null && highlightPair.equals(name)) {
				Color pairHighlightColor = Resources.getColor(Palette.LightGreen, 500, true);
				if (shadeByMapQV) return new Color(pairHighlightColor.getRed(), pairHighlightColor.getGreen(), pairHighlightColor.getBlue(), trans);
				else return pairHighlightColor;				
			}
			switch(colorBy) {
			case READ_GROUP:
				if (shadeByMapQV) return new Color(readColors.get(readGroup).getRed(), readColors.get(readGroup).getGreen(), readColors.get(readGroup).getBlue(), trans);
				else return readColors.get(readGroup);
			case SAMPLE:
				if (shadeByMapQV) return new Color(readColors.get(sample).getRed(), readColors.get(sample).getGreen(), readColors.get(sample).getBlue(), trans);
				else return readColors.get(sample);
			default:
			case STRAND:
				if (negativeStrandFlag == true){
					if (shadeByMapQV) return new Color(reverse.getRed(), reverse.getGreen(), reverse.getBlue(), trans);
					else return reverse;
				}else{
					if (shadeByMapQV) return new Color(forward.getRed(), forward.getGreen(), forward.getBlue(), trans);
					else return forward;
				}			
			}
		}

		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append(name + "\n");
			sb.append("Alignment start: " + alignmentStart + "\n");
			sb.append("Alignment end: " + alignmentEnd + "\n");
			sb.append("Read: "+ Arrays.toString(bases).replace(", ", "").replace("[", "").replace("]", "") + "\n");
			sb.append("Cigar: " + cigarString + "\n");
			sb.append("Negative strand: " + negativeStrandFlag + "\n");
			sb.append("Mapping quality: " + mapQV + "\n");
			sb.append("Pair: " + (isPaired ? (isFirstInPair ? "first in pair" : (isSecondInPair ? "second in pair" : "paired")) : "not paired") + "\n");
			sb.append("Phred scores: "+ Arrays.toString(baseQual).replace(", ", "").replace("[", "").replace("]", "") + "\n");
			return sb.toString();
		}
	}

	public void setSquished(boolean squished) {
		this.squished = squished;

		readBaseHeight = (squished) ? 1 : 12;
		interReadHeight = (squished) ? 0 : 2;
		readTotalHeight = readBaseHeight + interReadHeight;

		neededWidth = baseWidth * interval.getSize();
		neededHeight = (reads.size()+1)*readTotalHeight + geneDataset.size()*(readTotalHeight+5) + (readTotalHeight+5) + 10 + totalHeight*3;

		setSize(neededWidth, neededHeight);
		setPreferredSize(new Dimension(neededWidth, neededHeight));

		repaint();
	}

	public void setFrameShift(boolean shift) {
		this.showModifiedAA = shift;
		repaint();
	}

	public void export(){
		Object format = JOptionPane.showInputDialog(new JFrame(), "Choose an image format: ", "Export alignment to image file", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iExportJpeg, 64), ImageIO.getWriterFileSuffixes(), "png");
		if (format != null){
			FileDialog chooser = new FileDialog(new JFrame(), "Export alignment to image", FileDialog.SAVE) ;
			chooser.setFile(Tools.formatFilename(interval.getChromosome() + "-" + ((highlightedVariant != null)?highlightedVariant.getAlternativePosition():interval.getStart()) + "." + format));
			Tools.centerWindow(chooser, false);
			chooser.setVisible(true) ;
			if (chooser.getFile() != null) {
				try {
					String filename = chooser.getDirectory() + chooser.getFile();
					if (!filename.toLowerCase().endsWith("."+format.toString())) filename += "."+format.toString();      
					BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics2D g = image.createGraphics();
					paintComponent(g);
					ImageIO.write(image, format.toString(), new File(filename));
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(this, Tools.getMessage("Error when exporting alignment", ex), "Export alignment to image file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));  			
				}
			} 
		}
	}

	public Image getImage() {
		if (neededWidth * neededHeight > 500_000_000) {
			//Too big
			return null;
		}

		BufferedImage image = new BufferedImage(neededWidth, neededHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		drawAlignment(g);
		return image;
	}



	public void paintComponent(Graphics g1d) {
		super.paintComponent(g1d);
		Graphics2D g = (Graphics2D)g1d;
		//If the size of a base is smaller than 1 pixel, generate a picture and resize it
		if ((preferredWidth / interval.getSize()) < 4) {
			//Creating orginal image by drawing the full panel (paintComponent is called multiple times, and the first time the panel height is still unknown, set to 10)
			if (cachedImageOriginal == null || cachedImageWidth < 30 || cachedImageHeight < 30) {
				cachedImageOriginal = getImage();
				//Avoid creating tooltips with bad coordinates (linked to the panel, not the resized picture). Also each tooltip generate a call to paintComponent.
				tooltipsBack.clear();
				tooltipsFront.clear();
			}
			//Resizing the image to the frame size
			if (cachedImageOriginal != null && (cachedImageResized == null || cachedImageWidth != preferredWidth || cachedImageHeight != getHeight())) {
				cachedImageWidth = preferredWidth;
				cachedImageHeight = getHeight();
				cachedImageResized = cachedImageOriginal.getScaledInstance(cachedImageWidth, cachedImageHeight, java.awt.Image.SCALE_SMOOTH);
			}
			//Draw the cached resized image, or a red rectangle if image not available (e.g. too big)
			if (cachedImageResized != null) {
				g.drawImage(cachedImageResized, 0, 0, this);				
			}else {
				g.setColor(Color.RED);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		}else {
			drawAlignment(g);
		}
	}

	private void drawAlignment(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		tooltipsBack.clear();
		tooltipsFront.clear();

		Font baseFont = g.getFont();
		Font smallFont = baseFont.deriveFont(Font.PLAIN, baseFont.getSize()-2);
		g.setFont(smallFont);

		Stroke basicStroke = new BasicStroke();
		Stroke dashedStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[] {9,3,3,1}, 0.0f);

		g.clipRect(0, 0, neededWidth, neededHeight);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, neededWidth, neededHeight);
		g.setColor(Color.BLACK);

		int y = 5;

		int x = 0;

		if (drawReference) {
			for (char base : colors.keySet()){
				g.setColor(colors.get(base));
				g.fillRect(x, y, totalHeight/2, totalHeight);
				x+= totalHeight/2 + 5;
				String legend = " = " + base;
				g.drawString(legend, x, y + totalHeight);		
				x += (int)g.getFontMetrics().getStringBounds(legend,g).getWidth() + 15;
			}
			if (showSoftClippedBases) {
				g.setColor(softClippedBase);
				g.drawLine(x, y, x+totalHeight/2, y+totalHeight);
				g.drawLine(x, y+totalHeight, x+totalHeight/2, y);
				x+= totalHeight/2 + 5;
				String legend = " = soft clipped base";
				g.drawString(legend, x, y + totalHeight);		
				x += (int)g.getFontMetrics().getStringBounds(legend,g).getWidth() + 15;			
			}
			switch(colorBy) {
			case READ_GROUP:
			case SAMPLE:
				for (String key : readColors.keySet()){
					g.setColor(readColors.get(key));
					g.fillRect(x, y, totalHeight/2, totalHeight);
					x+= totalHeight/2 + 5;
					String legend = " = " + key;
					g.drawString(legend, x, y + totalHeight);		
					x += (int)g.getFontMetrics().getStringBounds(legend,g).getWidth() + 15;
				}
				break;
			case STRAND:
				g.setColor(forward);
				g.fillRect(x, y, totalHeight/2, totalHeight);
				x+= totalHeight/2 + 5;
				String legend = " = FORWARD";
				g.drawString(legend, x, y + totalHeight);		
				x += (int)g.getFontMetrics().getStringBounds(legend,g).getWidth() + 15;
				g.setColor(reverse);
				g.fillRect(x, y, totalHeight/2, totalHeight);
				x+= totalHeight/2 + 5;
				legend = " = REVERSE";
				g.drawString(legend, x, y + totalHeight);		
				x += (int)g.getFontMetrics().getStringBounds(legend,g).getWidth() + 15;
				break;
			default:
				break;
			}
			y+=totalHeight+5;

			Set<Point> drawnGeneData = new HashSet<>();
			for (Gene gene : geneDataset.values()){
				for (Point p : drawnGeneData) {
					if (gene.getTranscriptionStart() < p.getY() && gene.getTranscriptionEnd() > p.getX()) {
						y+=totalHeight+5;
						drawnGeneData.clear();
						break;
					}
				}
				drawGeneData(g, gene, y, baseWidth, baseHeight);
				drawnGeneData.add(new Point(gene.getTranscriptionStart(), gene.getTranscriptionEnd()));
			}
			y+=totalHeight+5;

			drawnGeneData.clear();
			for (Gene gene : geneDataset.values()){
				for (Point p : drawnGeneData) {
					if (gene.getTranslationStart() < p.getY() && gene.getTranslationEnd() > p.getX()) {
						y+=totalHeight+5;
						drawnGeneData.clear();
						break;
					}
				}
				drawCodons(g, gene, y, baseWidth, baseHeight);
				drawnGeneData.add(new Point(gene.getTranslationStart(), gene.getTranslationEnd()));
			}
			y+=totalHeight+5;
		}

		x = 0;
		int pos = interval.getStart();
		for (char base : reference.toCharArray()){
			if (drawReference) {
				g.setColor(colors.get(base));
				g.fillRect(x+1, y, baseWidth-2, baseHeight);
				int w = (int)g.getFontMetrics().getStringBounds(""+base,g).getWidth();
				int h = (int)g.getFontMetrics().getStringBounds(""+base,g).getHeight();
				if (w < baseWidth) {
					g.setColor(Color.WHITE);
					g.drawString(""+base, x+1+((baseWidth-2-w)/2), y+baseHeight-2-((baseHeight-h)/2));
				}
				if (highlightedVariant!= null && x == (highlightedVariant.getAlternativePosition()-interval.getStart())*baseWidth){
					tooltipsBack.put(new Rectangle(x+1, y, baseWidth-2, baseHeight), highlightedPosTooltip);
				}else {
					tooltipsBack.put(new Rectangle(x+1, y, baseWidth-2, baseHeight), "Chr "+interval.getChromosome()+" : "+Tools.intToString(pos));
				}
			}
			positions.put(new Point(x+1,x+1+baseWidth-2), pos++);
			x+=baseWidth;
		}
		if (drawReference) y+=totalHeight;

		for (int line : reads.keySet()){
			for(Read read : reads.get(line)){		
				int start = read.alignmentStart;
				int end = read.alignmentEnd;
				int size = (end - start + 1)*baseWidth;
				int drawStart = (start-interval.getStart())*baseWidth;
				int drawEnd = drawStart + size;
				g.setColor(read.getReadColory(true));
				g.fillRect(drawStart, y, size, readBaseHeight);
				if (read.negativeStrandFlag == true){
					g.fillPolygon(new int[]{drawStart,drawStart-interWidth,drawStart}, new int[]{y+readBaseHeight,y+(readBaseHeight/2),y}, 3);
					tooltipsBack.put(new Rectangle(drawStart-interWidth, y, size, readBaseHeight), "MAPQV="+read.mapQV);
					readMap.put(new Rectangle(drawStart-interWidth, y, size, readBaseHeight),read);
				}else{
					g.fillPolygon(new int[]{drawEnd,drawEnd+interWidth,drawEnd}, new int[]{y+readBaseHeight,y+(readBaseHeight/2),y}, 3);
					tooltipsBack.put(new Rectangle(drawStart+interWidth, y, size, readBaseHeight), "MAPQV="+read.mapQV);
					readMap.put(new Rectangle(drawStart+interWidth, y, size, readBaseHeight), read);
				}
				char[] cigar = read.cigarString.toCharArray();
				int drawPos=drawStart;
				int readPos=0;
				int refPos = start-interval.getStart();
				if (cigar[0] != '*'){
					for (int c=0 ; c < cigar.length ; c++){
						String times = "";
						while (Character.isDigit(cigar[c])){
							times += cigar[c++];
						}
						for (int t=0 ; t < Integer.parseInt(times) ; t++){
							char readBase = (readPos < read.bases.length) ? read.bases[readPos] : 'N';
							switch(cigar[c]){
							case 'M':
							case '=':
							case 'X':
								//Alignment match (can be a sequence match or mismatch)
								//Draw the base present in the read, increase index in the read, in the reference and on screen
								if (refPos >= 0 && refPos < reference.length() && readBase != reference.charAt(refPos)){
									g.setColor(read.getBaseColor(readPos, true));
									g.fillRect(drawPos+1, y, baseWidth-2, readBaseHeight);
									int w = (int)g.getFontMetrics().getStringBounds(""+readBase,g).getWidth();
									int h = (int)g.getFontMetrics().getStringBounds(""+readBase,g).getHeight();
									if (w < baseWidth && !squished) {
										g.setColor(Color.WHITE);
										g.drawString(""+readBase, drawPos+1+((baseWidth-2-w)/2), y+baseHeight-2-((baseHeight-h)/2));
									}
									tooltipsFront.put(new Rectangle(drawPos+1, y, baseWidth-2, readBaseHeight), readBase+" (qual "+read.getPhredScore(readPos)+")");
								}
								readPosMap.put(new Rectangle(drawPos+1, y, baseWidth-2, readBaseHeight), readPos);
								drawPos+= baseWidth;
								readPos++;
								refPos++;
								break;
							case 'I':
								//Insertion to the reference
								//Draw the insertion symbol, increase index in the reference
								if (t == 0){
									//first inserted base
									g.setColor(insertion);
									g.fillRect(drawPos-2, y, 4, readBaseHeight);
									g.fillRect(drawPos-4, y-1, 8, 2);
									g.fillRect(drawPos-4, y+readBaseHeight-1, 8, 2);
									String tooltip = "Insertion: ";
									for (int u=0 ; u < Integer.parseInt(times) ; u++){
										tooltip += (readPos+u < read.bases.length) ? read.bases[readPos+u] : 'N';
									}
									tooltipsFront.put(new Rectangle(drawPos-4,y,8,readBaseHeight), tooltip);
								}
								readPos++;
								break;
							case 'D':
								//Deletion from the reference
								//Draw "-", increase index in the reference and on screen
								g.setColor(Color.WHITE);
								g.fillRect(drawPos, y, baseWidth, readBaseHeight);
								g.setColor(Color.BLACK);
								g.fillRect(drawPos, y+(readBaseHeight/2)-1, baseWidth, 2);
								drawPos+= baseWidth;
								refPos++;
								break;
							case 'S':
								//Soft clipping (base present in the read)
								//don't add the base (generate useless patterns), increase index in the read (not in ref or on screen, we get alignment start/end, not unclipped start/end)
								//If show soft-clipped bases has been selected, act as normal base
								if (showSoftClippedBases) {
									if (refPos >= 0 && refPos < reference.length() && readBase != reference.charAt(refPos)){
										g.setColor(read.getBaseColor(readPos, true));
										g.fillRect(drawPos+1, y, baseWidth-2, readBaseHeight);
										int w = (int)g.getFontMetrics().getStringBounds(""+readBase,g).getWidth();
										int h = (int)g.getFontMetrics().getStringBounds(""+readBase,g).getHeight();
										if (w < baseWidth && !squished) {
											g.setColor(Color.WHITE);
											g.drawString(""+readBase, drawPos+1+((baseWidth-2-w)/2), y+baseHeight-2-((baseHeight-h)/2));
										}
										tooltipsFront.put(new Rectangle(drawPos+1, y, baseWidth-2, readBaseHeight), readBase+" (qual "+read.getPhredScore(readPos)+")");
										readPosMap.put(new Rectangle(drawPos+1, y, baseWidth-2, readBaseHeight), readPos);
									}
									g.setColor(softClippedBase);
									if (squished) {
										g.drawLine(drawPos+(baseWidth/2)-(baseWidth/4), y, drawPos+(baseWidth/2)+(baseWidth/4), y);
									}else {
										g.drawLine(drawPos+1, y, drawPos+1+baseWidth-2, y+readBaseHeight);
										g.drawLine(drawPos+1, y+readBaseHeight, drawPos+1+baseWidth-2, y);
									}
									drawPos+= baseWidth;
									refPos++;
								}
								readPos++;
								break;
							case 'H':
								//Hard clipping (base NOT present in the read)
								//Do nothing, bases are not present in the read and nothing has to be drawn
								break;
							case 'N':
								//skipped region from the reference
								//For mRNA-to-genome alignment, an N operation represents an intron. For other types of alignments, the interpretation of N is not defined.
								//increase index in the reference and on screen
								//!!!! Never encountered, so never tested !!!!
								drawPos+= baseWidth;
								refPos++;
								break;
							case 'P':
								//Padding (silent deletion from padded reference)
								//Nothing to do, inexistant in the ref, inexistant in the read. Only useful with multiple alignments.
								//!!!! Never encountered, so never tested !!!!
								break;
							default:
								System.err.println("Unsupported CIGAR operation : " + cigar[c]);
								break;
							}
						}
						if (cigar[c] == 'D' && Integer.parseInt(times) > 1) {
							int w = (int)g.getFontMetrics().getStringBounds(""+times,g).getWidth();
							int h = (int)g.getFontMetrics().getStringBounds(""+times,g).getHeight();
							int delSize = Integer.parseInt(times)*baseWidth;
							g.setColor(Color.WHITE);
							g.fillRect(drawPos-(delSize/2)-(w/2)-1, y, w+2, readBaseHeight);
							g.setColor(Color.BLACK);
							g.drawString(""+times, drawPos-(delSize/2)-(w/2), y+h-2);
						}
					}
				}
				/*
				g.setColor(Color.BLACK);
				g.drawString(read.name, drawStart, y+baseHeight-1);
				 */
			}
			y+=readTotalHeight;
		}
		
		g.setColor(Color.BLACK);
		g.setStroke(dashedStroke);
		if (highlightedVariant != null){
			int drawStart = (highlightedVariant.getAlternativePosition()-interval.getStart())*baseWidth;
			int drawEnd = drawStart + baseWidth;
			g.drawLine(drawStart-1, 0, drawStart-1, neededHeight);
			g.drawLine(drawEnd, 0, drawEnd, neededHeight);
		}
		g.setStroke(basicStroke);
	}

	private void drawGeneData(Graphics2D g, Gene gene, int y, int baseWidth, int height){
		int x=0;
		int arrow = baseWidth;
		for (int pos = interval.getStart() ; pos <= interval.getEnd() ; pos++,x+=baseWidth){
			int exon = gene.getExonNum(pos, false);			
			g.setColor(gene.getBiotypeColor());
			boolean drawArrow = false;
			String tooltip = "<html><b>" + gene.getGeneSymbol() + "</b><br>";
			if (exon >= 0){
				if (pos <= gene.getTranslationStart() || pos > gene.getTranslationEnd()){
					g.fillRect(x, y+height/4, baseWidth, height/2);		
					String UTR = ((gene.isStrandPositive() && gene.isPosInLeftUTR(pos)) || (!gene.isStrandPositive() && gene.isPosInRightUTR(pos))) 
							? "exon "+ gene.getExonRank(exon) + ((gene.isTranslated())?"<br>5' UTR":"<br>UTR") 
									: "exon "+ gene.getExonRank(exon) + ((gene.isTranslated())?"<br>3' UTR":"<br>UTR");
							tooltip += UTR + "<br>";
				}else{
					g.fillRect(x, y, baseWidth, height);
					tooltip += "exon "+ gene.getExonRank(exon) +"<br>CDS<br>";
				}
				g.setColor(Color.WHITE);
				drawArrow = true;
			}else if (pos > gene.getTranscriptionStart() && pos < gene.getTranscriptionEnd()){
				//gene intron
				g.drawLine(x, y+height/2, x+baseWidth, y+height/2);
				g.setColor(gene.getBiotypeColor());
				int intron = gene.getIntronNum(pos, false);
				tooltip += "intron " + gene.getIntronRank(intron) + "<br>";
				drawArrow = true;
			}
			if (drawArrow) {
				tooltip += "Ensembl gene: " + gene.getEnsemblGene() + "<br>"; 
				tooltip += "Ensembl transcript: " + gene.getEnsemblTranscript() + "<br>"; 
				tooltip += "RefSeq mRNA: " + ((gene.getRefSeqTranscript() != null)?gene.getRefSeqTranscript():"-") + "<br>"; 
				tooltip += "Biotype: <b><font color = \"rgb("+gene.getBiotypeColor().getRed()+","+gene.getBiotypeColor().getGreen()+","+gene.getBiotypeColor().getBlue()+")\">" + gene.getBiotype() + "</font></b></html>";
				tooltipsBack.put(new Rectangle(x, y, baseWidth, height), tooltip);				
			}
			if (x == arrow){
				if (drawArrow) {
					if (gene.isStrandPositive()){
						g.fillPolygon(new int[]{x,x+baseWidth,x}, new int[]{y+height/4,y+(height/2),y+height*3/4}, 3);
					}else{
						g.fillPolygon(new int[]{x+baseWidth,x,x+baseWidth}, new int[]{y+height/4,y+(height/2),y+height*3/4}, 3);
					}
				}
				arrow += baseWidth*5;
			}
		}
	}

	private void drawCodons(Graphics2D g, Gene gene, int y, int baseWidth, int height){
		int x=0;
		for (int pos = interval.getStart() ; pos <= interval.getEnd() ; pos++,x+=baseWidth){
			int codonPos = (showModifiedAA && highlightedVariant != null) ? gene.getCodonPos(pos, highlightedVariant) : gene.getCodonPos(pos);
			String AAs = (showModifiedAA && highlightedVariant != null) ? gene.getAminoAcid(pos, highlightedVariant) : gene.getAminoAcid(pos);
			if (!AAs.equals("#") && AAs.length() == 1) {
				char AA = AAs.charAt(0);
				if((codonPos == 0 && gene.isStrandPositive()) || (codonPos == 2 && !gene.isStrandPositive())) {
					g.setColor(Tools.getAminoAcidColor(AA));
					g.fillRect(x+2, y, baseWidth, height);
					tooltipsBack.put(new Rectangle(x+2, y, baseWidth, height), Tools.getAminoAcidName(AA));
				}else if((codonPos == 2 && gene.isStrandPositive()) || (codonPos == 0 && !gene.isStrandPositive())) {
					g.setColor(Tools.getAminoAcidColor(AA));
					g.fillRect(x, y, baseWidth-2, height);
					tooltipsBack.put(new Rectangle(x, y, baseWidth-2, height), Tools.getAminoAcidName(AA));
				}else {
					g.setColor(Tools.getAminoAcidColor(AA));
					g.fillRect(x, y, baseWidth, height);
					g.setColor(Color.WHITE);
					int w = (int)g.getFontMetrics().getStringBounds(""+AA,g).getWidth();
					int h = (int)g.getFontMetrics().getStringBounds(""+AA,g).getHeight();
					g.drawString(""+AA, x+((baseWidth-w)/2), y+h-2);
					tooltipsBack.put(new Rectangle(x, y, baseWidth, height), Tools.getAminoAcidName(AA));
				}
			}else if (!AAs.equals("#") && AAs.length() > 1) {
				g.setColor(Color.BLACK);
				int w = (int)g.getFontMetrics().getStringBounds("+",g).getWidth();
				int h = (int)g.getFontMetrics().getStringBounds("+",g).getHeight();
				g.drawString("+", x+((baseWidth-w)/2), y+h-2);
				tooltipsBack.put(new Rectangle(x, y, baseWidth, height), AAs);				
			}
		}
	}

	public int getPosition(Point e) {
		for (Point p : positions.keySet()){
			if (e.getX() >= p.x && e.getX() <= p.y){
				return positions.get(p);
			}
		}
		return -1;
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		for (Rectangle r : tooltipsFront.keySet()){
			if (e.getX() >= r.getX() && e.getX() <= r.getX()+r.getWidth() && e.getY() >= r.getY() && e.getY() <= r.getY()+r.getHeight()){
				return tooltipsFront.get(r);
			}
		}
		for (Rectangle r : tooltipsBack.keySet()){
			if (e.getX() >= r.getX() && e.getX() <= r.getX()+r.getWidth() && e.getY() >= r.getY() && e.getY() <= r.getY()+r.getHeight()){
				return tooltipsBack.get(r);
			}
		}
		return null;
	}

	@Override
	public Point getToolTipLocation(MouseEvent e) {
		Point p = e.getPoint();
		p.y += 15;
		return p;
	}

	public static void main(String[] args){
		try{
			Object[][] samples = new Object[][]{
				{"panels_torrent_caller","LE-80-10.pLELM","9",128_179_172,128_194_106},		//Ion Torrent
				{"panels_torrent_caller","VA-383-T.pCMAVM","19",3119239-40, 3119239+40},		//Ion Torrent
				{"panels_torrent_caller","AVM-12-10.pLELM","22",41537234-40, 41537234+40},		//Ion Torrent
				{"panels_torrent_caller","VA-78-B.pKPT","7",91_842_418, 91_842_792},		//Ion Torrent
				{"crap","CLP-800-3","3",128_202_653,128_202_885},												//Illumina old
				{"exomes_lifescope","ELA-89-100","3",128_202_653,128_202_885},					//Solid Lifescope
				{"exomes_haplotype_caller","ELA-89-100","3",128_202_653,128_202_885},	 //Solid HC
				{"exomes_haplotype_caller","LE-59-100","3",128_202_653,128_202_885},   //BGI illumina HC
				{"genomes_haplotype_caller","VA-1358","1",92762434,92766416},   //WGS illumina HC
			};
			int s = 8;
			Highlander.initialize(new Parameters(false),5);
			AnalysisFull analysis = null;
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM analyses WHERE analysis = '"+samples[s][0]+"'")) {
				res.next();
				analysis = new AnalysisFull(res);
			}
			AlignmentPanel alignment = BamViewer.getAlignmentPanel(analysis, samples[s][1].toString(), 
					new Interval(analysis.getReference(), samples[s][2].toString(), Integer.parseInt(samples[s][3].toString()),  Integer.parseInt(samples[s][4].toString())), 16000, new JProgressBar());
			//AlignmentPanel alignment = BamViewer.getAlignmentPanel(new URL("ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/phase3/data/NA06985/alignment/NA06985.mapped.ILLUMINA.bwa.CEU.low_coverage.20120522.bam"), 
			//		new Interval("1",144994638,144994678), 144994658, "A", false, false, false, ColorBy.STRAND, 1600, new JProgressBar());
			JFrame frame = new JFrame();
			frame.getContentPane().setLayout(new BorderLayout());
			JScrollPane scroll = new JScrollPane();
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scroll.setViewportView(alignment);
			frame.getContentPane().add(scroll, BorderLayout.CENTER);
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			Tools.centerWindow(frame, true);
			frame.setVisible(true);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}

