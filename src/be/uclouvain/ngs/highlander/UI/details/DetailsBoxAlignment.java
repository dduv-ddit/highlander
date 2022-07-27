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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel.ColorBy;
import be.uclouvain.ngs.highlander.UI.tools.BamViewer;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Variant;

/**
 * Alignment visualization
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxAlignment extends DetailsBox {

	protected DetailsPanel mainPanel;
	protected boolean detailsLoaded = false;

	protected int offset = 0;
	protected static int window = 40;
	protected static boolean softClipped = false;
	protected static boolean squished = false;
	protected static boolean frameShift = false;
	protected static ColorBy colorBy = ColorBy.STRAND;

	protected AlignmentPanel alignment;

	public DetailsBoxAlignment(int variantId, DetailsPanel mainPanel){
		this.variantSampleId = variantId;
		this.mainPanel = mainPanel;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	public String getTitle(){
		return "Alignment";
	}

	public Palette getColor() {
		return Field.read_depth.getCategory().getColor();
	}
	
	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	protected void loadDetails(){
		try{
			detailsPanel.removeAll();
			JProgressBar bar = new JProgressBar();
			detailsPanel.add(bar, BorderLayout.NORTH);
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			String sample = "";
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT sample "
					+ "FROM " +	analysis.getFromSampleAnnotations()
					+ analysis.getJoinProjects()
					+	"WHERE variant_sample_id = " + variantSampleId
					)) {
				if (res.next()){
					sample = res.getString("sample");
				}
			}
			if (sample.length() > 0){
				Variant variant = new Variant(variantSampleId);
				Interval interval = new Interval(analysis.getReference(), variant.getChromosome(), variant.getAlternativePosition()+offset-window, variant.getAlternativePosition()+offset+window);
				alignment = BamViewer.getAlignmentPanel(analysis, sample, interval, variant, softClipped, squished, frameShift, colorBy, true, mainPanel.getWidth(), bar);
				alignment.setBackground(Color.WHITE);
				detailsPanel.removeAll();
				detailsPanel.add(alignment, BorderLayout.CENTER);
				detailsPanel.add(getControlBar(), BorderLayout.NORTH);
			}else{
				detailsPanel.removeAll();
				detailsPanel.add(new JLabel("Variant was not found in the database"), BorderLayout.CENTER);			
			}
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

	protected JPanel getControlBar() {
		JPanel panel = new JPanel(new WrapLayout(WrapLayout.LEADING));
		panel.setBackground(Resources.getColor(getColor(), 200, false));
		
		final JButton zoomin = new JButton(Resources.getScaledIcon(Resources.iZoomIn, 24));
		final JButton zoomout = new JButton(Resources.getScaledIcon(Resources.iZoomOut, 24));

		JButton left10 = new JButton(Resources.getScaledIcon(Resources.iArrowLeft, 24));
		left10.setToolTipText("Move left");
		left10.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				offset -= window;
				loadDetails();
			}
		});
		panel.add(left10);

		JButton center = new JButton(Resources.getScaledIcon(Resources.iAlignmentCenterMutation, 24));
		center.setToolTipText("Center on variant");
		center.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				offset = 0;
				loadDetails();
			}
		});
		panel.add(center);

		JButton right10 = new JButton(Resources.getScaledIcon(Resources.iArrowRight, 24));
		right10.setToolTipText("Move right");
		right10.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				offset += window;
				loadDetails();
			}
		});
		panel.add(right10);

		zoomout.setToolTipText("Zoom out 2x");
		zoomout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//with a starting window of 40 and a x2 zoom out, we reach too big pictures with a window of 40960. Note that it depends also of the height of the picture, so the limit can be reached sooner with high coverage like Ion Torrent.
				if (window < 20480) {
					window *= 2;
					loadDetails();
				}
				if (window == 20480) {
					zoomout.setEnabled(false);
				}
				if (window == 10) {
					zoomin.setEnabled(true);
				}
			}
		});
		panel.add(zoomout);

		JButton zoomcenter = new JButton(Resources.getScaledIcon(Resources.iZoomOriginal, 24));
		zoomcenter.setToolTipText("Reset zoom");
		zoomcenter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window = 40;
				loadDetails();
				zoomout.setEnabled(true);
				zoomin.setEnabled(true);
			}
		});
		panel.add(zoomcenter);

		zoomin.setToolTipText("Zoom in 2x");
		zoomin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//with a starting window of 40 and a /2 zoom in, 5 is the minimum integer accessible
				if (window > 5) {
					window /= 2;
					loadDetails();
				}
				if (window == 5) {
					zoomin.setEnabled(false);
				}
				if (window == 10240) {
					zoomout.setEnabled(true);
				}
			}
		});
		panel.add(zoomin);

		JToggleButton softClippedButton = new JToggleButton(Resources.getScaledIcon(Resources.iAlignmentSoftclipOff, 24), softClipped);
		softClippedButton.setSelectedIcon(Resources.getScaledIcon(Resources.iAlignmentSoftclipOn, 24));
		softClippedButton.setRolloverEnabled(true);
		softClippedButton.setRolloverIcon(Resources.getScaledIcon(Resources.iAlignmentSoftclipOn, 24));
		softClippedButton.setRolloverSelectedIcon(Resources.getScaledIcon(Resources.iAlignmentSoftclipOff, 24));
		softClippedButton.setToolTipText("Show / Hide soft-clipped reads");
		softClippedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				softClipped = !softClipped;
				loadDetails();
			}
		});
		panel.add(softClippedButton);

		JToggleButton squishedButton = new JToggleButton(Resources.getScaledIcon(Resources.iAlignmentSquishedOff, 24), squished);
		squishedButton.setSelectedIcon(Resources.getScaledIcon(Resources.iAlignmentSquishedOn, 24));
		squishedButton.setRolloverEnabled(true);
		squishedButton.setRolloverIcon(Resources.getScaledIcon(Resources.iAlignmentSquishedOn, 24));
		squishedButton.setRolloverSelectedIcon(Resources.getScaledIcon(Resources.iAlignmentSquishedOff, 24));
		squishedButton.setToolTipText("Normal / Squished reads");
		squishedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				squished = !squished;
				//loadDetails();
				alignment.setSquished(squished);
			}
		});
		panel.add(squishedButton);

		JToggleButton frameShiftButton = new JToggleButton(Resources.getScaledIcon(Resources.iAlignmentFrameShiftOff, 24), frameShift);
		frameShiftButton.setSelectedIcon(Resources.getScaledIcon(Resources.iAlignmentFrameShiftOn, 24));
		frameShiftButton.setRolloverEnabled(true);
		frameShiftButton.setRolloverIcon(Resources.getScaledIcon(Resources.iAlignmentFrameShiftOn, 24));
		frameShiftButton.setRolloverSelectedIcon(Resources.getScaledIcon(Resources.iAlignmentFrameShiftOff, 24));
		frameShiftButton.setToolTipText("Show amino acids changes generated (only) by selected variant (substitutions and frame shifts)");
		frameShiftButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frameShift = !frameShift;
				//loadDetails();
				alignment.setFrameShift(frameShift);
			}
		});
		panel.add(frameShiftButton);

		final JComboBox<ColorBy> colorByBox = new JComboBox<ColorBy>(ColorBy.values());
		colorByBox.setSelectedItem(colorBy);
		colorByBox.setToolTipText("Color reads by ...");
		colorByBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				colorBy = (ColorBy)colorByBox.getSelectedItem();
				loadDetails();
			}
		});
		panel.add(colorByBox);

		JButton copy = new JButton(Resources.getScaledIcon(Resources.iCopy, 24));
		copy.setToolTipText("Copy alignment to clipboard");
		copy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (Component c : detailsPanel.getComponents()) {
					if (c instanceof AlignmentPanel){
						Tools.setClipboard(((AlignmentPanel)c).getImage());;
					}
				}
			}
		});
		panel.add(copy);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExportJpeg, 24));
		export.setToolTipText("Export alignment to image file");
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (Component c : detailsPanel.getComponents()) {
					if (c instanceof AlignmentPanel){
						((AlignmentPanel)c).export();
					}
				}
			}
		});
		panel.add(export);

		return panel;
	}


}
