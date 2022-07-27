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

package be.uclouvain.ngs.highlander.UI.tools;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.html.HTMLEditorKit;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.AskSamplesDialog;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.UI.misc.AlignmentPanel.ColorBy;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Variant;

public class AlignmentViewer extends JFrame {

	final private String SELECTION = "Selection";
	
	protected int offset = 0;
	protected static int window = 40;
	protected static boolean softClipped = false;
	protected static boolean squished = false;
	protected static boolean frameShift = false;
	protected static ColorBy colorBy = ColorBy.STRAND;

	private JSplitPane split;
	
	private JComboBox<Reference> boxReference;
	private JComboBox<AnalysisFull> boxAnalysis;
	private DefaultTableModel tableModelSamples;
	private JTable tableSamples;
	private TableRowSorter<DefaultTableModel> sorterSamples;
	private SearchField	searchFieldSamples = new SearchField(10);

	private JTextField  txtLocus;
	private Variant lastVariant = null;
	private JProgressBar bar;
		
	private GridLayout layoutAlignments;
	private JPanel panelAllAlignments;
	private Map<String,JPanel> mapPanels = new LinkedHashMap<>();
	private Map<String,String> mapSamples = new LinkedHashMap<>();
	private Map<String,AnalysisFull> mapAnalyses = new LinkedHashMap<>();
	private Map<String,JScrollPane> mapScrollPanes = new LinkedHashMap<>();
	private Map<String,JPanel> mapCachedAlignments = new LinkedHashMap<>();
	
	public AlignmentViewer() {
		this("", -1, new TreeSet<String>());
	}
	
	public AlignmentViewer(String chr, int pos, final Set<String> samples) {
		super();
		if (pos > -1 && chr != null && chr.length() >0) lastVariant = new Variant(chr, pos);
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				updateTables();	
				if (!samples.isEmpty()) {
					String selection = samples.iterator().next();
					for (String sample : samples) {
						if (!sample.equals(selection)) pinSample(Highlander.getCurrentAnalysis(), sample);
					}
					for (int i=0 ; i < tableSamples.getRowCount() ; i++) {
						if (tableSamples.getValueAt(i, 0).equals(selection)) {
							tableSamples.setRowSelectionInterval(i, i);
							tableSamples.scrollRectToVisible(tableSamples.getCellRect(i,0, true));
							break;
						}
					}
				}
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}
			
			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});
	}
	
	private void initUI(){
		setTitle("Alignment Viewer");
		setIconImage(Resources.getScaledIcon(Resources.iAlignmentSquishedOff, 64).getImage());

		setLayout(new BorderLayout());

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getPanelSamples(), getAlignmentPanel());
		((BasicSplitPaneUI)split.getUI()).getDivider().addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				loadDetails();
			}
			@Override
			public void mousePressed(MouseEvent e) {
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});
		add(split, BorderLayout.CENTER);
	}
	
	private JPanel getPanelSamples(){
		JPanel panel = new JPanel(new BorderLayout());
		
		JPanel panel_analysis = new JPanel(new BorderLayout());
		boxReference = new JComboBox<Reference>(Reference.getAvailableReferences().toArray(new Reference[0]));
		boxReference.setToolTipText("Reference genomes");
		boxReference.setSelectedItem(Highlander.getCurrentAnalysis().getReference());
		boxReference.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED){
					filterByReference();
				}
			}
		});
		panel_analysis.add(boxReference, BorderLayout.NORTH);		
		boxAnalysis = new JComboBox<AnalysisFull>();
		boxAnalysis.setToolTipText("Analyses supporting selected reference genome");
		filterByReference();
		boxAnalysis.setSelectedItem(Highlander.getCurrentAnalysis());
		boxAnalysis.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED){
					updateTables();
				}
			}
		});
		panel_analysis.add(boxAnalysis, BorderLayout.SOUTH);
		
		JPanel panel_filters = new JPanel(new BorderLayout());
		panel.add(panel_filters, BorderLayout.NORTH);

		panel_filters.add(panel_analysis, BorderLayout.NORTH);
		
		searchFieldSamples.addFieldListener(new KeyListener() {
			public void keyReleased(KeyEvent arg0) {
				applyBothFilters();
			}
			public void keyTyped(KeyEvent arg0) {			}
			public void keyPressed(KeyEvent arg0) {			}
		});
		panel_filters.add(searchFieldSamples, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.CENTER);
		
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.rowWeights = new double[]{0.0};
		gbl_panel_1.columnWeights = new double[]{1.0, 0.0};
		panel_1.setLayout(gbl_panel_1);
		
		JPanel panel_0 = new JPanel(new BorderLayout(0,0));
		panel_0.setBorder(BorderFactory.createTitledBorder("Sample"));		
		GridBagConstraints gbc_scrollPaneSource = new GridBagConstraints();
		gbc_scrollPaneSource.weighty = 1.0;
		gbc_scrollPaneSource.weightx = 1.0;
		gbc_scrollPaneSource.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneSource.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSource.gridx = 0;
		gbc_scrollPaneSource.gridy = 0;
		panel_1.add(panel_0, gbc_scrollPaneSource);
		
		JScrollPane scrollPaneSource = new JScrollPane();
		panel_0.add(scrollPaneSource, BorderLayout.CENTER);
		
		tableSamples = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSamples.setTableHeader(null);
		tableSamples.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting())
					return;
				loadDetails();
			}
		});
		scrollPaneSource.setViewportView(tableSamples);
		
		JPanel panel_middle = new JPanel();
		GridBagConstraints gbc_panel_middle = new GridBagConstraints();
		gbc_panel_middle.gridx = 1;
		gbc_panel_middle.gridy = 0;
		panel_1.add(panel_middle, gbc_panel_middle);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_middle.setLayout(gbl_panel_2);
		
		JButton button = new JButton(Resources.getScaledIcon(Resources.iPin, 24));
		button.setToolTipText("Pin alignment of selected sample for a multi-alignment comparison");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String sample = tableSamples.getValueAt(tableSamples.getSelectedRow(), 0).toString();
				AnalysisFull analysis = (AnalysisFull)boxAnalysis.getSelectedItem();
				pinSample(analysis, sample);
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 0;
		panel_middle.add(button, gbc_button);
		
		return panel;
	}
	
	private void filterByReference(){
		boxAnalysis.removeAllItems();
		for (AnalysisFull analysis : Highlander.getAvailableAnalyses()){
			Reference reference = (Reference)boxReference.getSelectedItem();
			try {
				if (analysis.getReference().usesSameReferenceSequenceAs(reference)){
					boxAnalysis.addItem(analysis);
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		boxAnalysis.validate();
		boxAnalysis.repaint();
	}

	public void pinSample(AnalysisFull analysis, String sample) {
		final String label = sample + " [" + analysis + "]";
		final JPanel panel = new JPanel(new BorderLayout());
		mapPanels.put(label, panel);
		mapSamples.put(label, sample);
		mapAnalyses.put(label, analysis);
		
		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JButton button = new JButton(Resources.getScaledIcon(Resources.iUnpin, 18));
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapPanels.remove(label);	
				mapSamples.remove(label);	
				mapAnalyses.remove(label);	
				mapScrollPanes.remove(label);
				mapCachedAlignments.remove(label);
				panelAllAlignments.removeAll();
				for (JPanel p : mapPanels.values()) {
					panelAllAlignments.add(p);
				}
				panelAllAlignments.validate();
			}
		});
		north.add(button);
		north.add(new JLabel(label));
		panel.add(north, BorderLayout.NORTH);
		
		final JScrollPane scrollAlignment = new JScrollPane();
		scrollAlignment.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				for (JScrollPane scroll : mapScrollPanes.values()) {
					if (scroll != scrollAlignment) {
						scroll.getHorizontalScrollBar().setValue(e.getValue());
					}
				}					
			}
		});
		
		mapScrollPanes.put(label, scrollAlignment);
		panel.add(scrollAlignment, BorderLayout.CENTER);
		
		panelAllAlignments.add(panel);
		loadDetails();
		panelAllAlignments.validate();
	}
	
	private void applyBothFilters(){
		RowFilter<DefaultTableModel, Object> rf = null;
    //If current expression doesn't parse, don't update.
    try {
        rf = RowFilter.regexFilter("(?i)"+searchFieldSamples.getText());
    } catch (java.util.regex.PatternSyntaxException e) {
        return;
    }
    sorterSamples.setRowFilter(rf);
	}
	
	private void updateTables(){
		try{
			AnalysisFull analysis = boxAnalysis.getItemAt(boxAnalysis.getSelectedIndex());
			Set<String> samples = AskSamplesDialog.getAvailableSamples(analysis, null);
			
			Object[][] dataHasPermission = new Object[samples.size()][1];
			int row = 0;
			for (String o : samples){
				dataHasPermission[row++][0] = o;
			}
			tableModelSamples = new DefaultTableModel(dataHasPermission, new String[] {"Has permission to modify"});
			sorterSamples = new TableRowSorter<DefaultTableModel>(tableModelSamples);
			tableSamples.setModel(tableModelSamples);		
			tableSamples.setRowSorter(sorterSamples);
			searchFieldSamples.setSorter(sorterSamples);
			searchFieldSamples.applyFilter();
						
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}
	
	
	private JPanel getAlignmentPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		
		panel.add(getControlBar(), BorderLayout.NORTH);

		layoutAlignments = new GridLayout(0,1);
		panelAllAlignments = new JPanel(layoutAlignments);
		panel.add(panelAllAlignments, BorderLayout.CENTER);
		
		JPanel container = new JPanel(new BorderLayout());
		mapPanels.put(SELECTION, container);
		
		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEADING));
		north.add(new JLabel(SELECTION));
		container.add(north, BorderLayout.NORTH);
		
		final JScrollPane scrollAlignment = new JScrollPane();
		scrollAlignment.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				for (JScrollPane scroll : mapScrollPanes.values()) {
					if (scroll != scrollAlignment) {
						scroll.getHorizontalScrollBar().setValue(e.getValue());
					}
				}					
			}
		});
		mapScrollPanes.put(SELECTION, scrollAlignment);
		container.add(scrollAlignment, BorderLayout.CENTER);
		panelAllAlignments.add(container);
		
		

		
		bar = new JProgressBar();
		panel.add(bar, BorderLayout.SOUTH);
		
		return panel;
	}
	
	private JPanel getControlBar() {
		JPanel panel = new JPanel(new WrapLayout(WrapLayout.LEADING));

		JLabel labelChr = new JLabel("Locus");
		labelChr.setToolTipText("Enter a locus in the form chr:start-stop or chr:pos, a gene symbol (e.g. TEK) or an Ensembl gene id (e.g. ENSG00000120156)");
		panel.add(labelChr);
		txtLocus = new JTextField(20);
		updateLocusTextField();
		txtLocus.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (setCurrentLocus()) {
					loadDetails();
				}
			}
		});
		panel.add(txtLocus);
		JButton buttonGo = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		buttonGo.setToolTipText("Go to locus");
		buttonGo.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (setCurrentLocus()) {
					loadDetails();
				}
			}
		});
		panel.add(buttonGo);
		
		final JButton zoomin = new JButton(Resources.getScaledIcon(Resources.iZoomIn, 24));
		final JButton zoomout = new JButton(Resources.getScaledIcon(Resources.iZoomOut, 24));

		JButton left10 = new JButton(Resources.getScaledIcon(Resources.iArrowLeft, 24));
		left10.setToolTipText("Move left");
		left10.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				offset -= window;
				updateLocusTextField();
				loadDetails();
			}
		});
		panel.add(left10);
		
		JButton center = new JButton(Resources.getScaledIcon(Resources.iAlignmentCenterMutation, 24));
		center.setToolTipText("Center on variant");
		center.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (offset == 0) {			
					for (JScrollPane scrollAlignment : mapScrollPanes.values()) {
						scrollAlignment.getHorizontalScrollBar().setValue((scrollAlignment.getHorizontalScrollBar().getMaximum()-scrollAlignment.getHorizontalScrollBar().getVisibleAmount())/2);
					}
				}else {
					offset = 0;
					updateLocusTextField();
					loadDetails();					
				}
			}
		});
		panel.add(center);
		
		JButton right10 = new JButton(Resources.getScaledIcon(Resources.iArrowRight, 24));
		right10.setToolTipText("Move right");
		right10.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				offset += window;
				updateLocusTextField();
				loadDetails();
			}
		});
		panel.add(right10);
		
		zoomout.setToolTipText("Zoom out 2x");
		zoomout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				window *= 2;
				updateLocusTextField();
				loadDetails();
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
				updateLocusTextField();
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
					updateLocusTextField();
					loadDetails();
				}
				if (window == 5) {
					zoomin.setEnabled(false);
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
		
		JToggleButton squishedButton = new JToggleButton(Resources.getScaledIcon(Resources.iAlignmentSquishedOff, 24), squished);;
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
				for (JPanel cachedAlignment : mapCachedAlignments.values()) {
					if (cachedAlignment instanceof AlignmentPanel)
						((AlignmentPanel)cachedAlignment).setSquished(squished);
				}
			}
		});
		panel.add(squishedButton);
		
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
				JPanel cachedAlignment = mapCachedAlignments.get(SELECTION);
				if (cachedAlignment instanceof AlignmentPanel)
					Tools.setClipboard(((AlignmentPanel)cachedAlignment).getImage());;
			}
		});
		panel.add(copy);
		
		JButton export = new JButton(Resources.getScaledIcon(Resources.iExportJpeg, 24));
		export.setToolTipText("Export alignment to image file");
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JPanel cachedAlignment = mapCachedAlignments.get(SELECTION);
				if (cachedAlignment instanceof AlignmentPanel)
					((AlignmentPanel)cachedAlignment).export();
			}
		});
		panel.add(export);
		
		JButton help = new JButton(Resources.getScaledIcon(Resources.iHelp, 24));
		help.setToolTipText(getHelpText());
		help.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
		    JFrame dlg = new JFrame();
		    dlg.setTitle("Alignment Viewer");
		    dlg.setIconImage(Resources.getScaledIcon(Resources.iRegExp, 64).getImage());
		    JScrollPane scrollPane = new JScrollPane();
		    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
				scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);	
				JTextPane startTxt = new JTextPane();
				startTxt.setEditorKit(new HTMLEditorKit());
				startTxt.setOpaque(true);
				startTxt.setText(getHelpText());
				startTxt.setCaretPosition(0);
				startTxt.setEditable(false);
				scrollPane.setViewportView(startTxt);	
				dlg.getContentPane().add(scrollPane, BorderLayout.CENTER);
		    dlg.pack();
		    Tools.centerWindow(dlg, false);
		    dlg.setVisible(true);
			}
		});
		panel.add(help);
		
		return panel;
	}
	
	private void updateLocusTextField() {
		if (lastVariant != null) {
			int start = lastVariant.getPosition()+offset-window;
			int stop = lastVariant.getPosition()+offset+window;
			txtLocus.setText(lastVariant.getChromosome() + ":" + start + "-" + stop);
			txtLocus.setToolTipText(lastVariant.getChromosome() + " : " + Tools.intToString(start) + " - " + Tools.intToString(stop));			
		}		
	}
	
	private boolean setCurrentLocus() {
		String locus = txtLocus.getText().trim().replace(",", "").replace("chr", "").replace(" ", "");
		if (locus.length() == 0) {
			JOptionPane.showMessageDialog(this,"No locus entered", "Locus recognition", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			updateLocusTextField();
			return false;
		}
		String chr = "";
		int pos = -1;
		if (locus.contains(":") && locus.contains("-")) {
			//Interval
			chr = locus.split(":")[0];
			if (chr.toLowerCase().startsWith("chr")) chr = locus.substring(3);
			try {
				int start = Integer.parseInt(locus.split(":")[1].split("-")[0]);
				int stop = Integer.parseInt(locus.split(":")[1].split("-")[1]);
				if (stop < start) {
					JOptionPane.showMessageDialog(this,"End of interval is smaller than it's start", "Locus recognition", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					updateLocusTextField();
					return false;					
				}
				if (start < 1 || stop < 1) {
					JOptionPane.showMessageDialog(this,"Start/end of interval is smaller than 1", "Locus recognition", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					updateLocusTextField();
					return false;					
				}
				window = (stop - start) / 2;
				pos = start + window;
			}catch(Exception ex) {
				JOptionPane.showMessageDialog(this,"Start or end of interval is not a number", "Locus recognition", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				updateLocusTextField();
				return false;					
			}
		}else if (locus.contains(":")) {
			//Single Position
			chr = locus.split(":")[0];
			if (chr.toLowerCase().startsWith("chr")) chr = locus.substring(3);
			try {
				pos = Integer.parseInt(locus.split(":")[1]);
			}catch(Exception ex) {
				JOptionPane.showMessageDialog(this,"Start or end of interval is not a number", "Locus recognition", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				updateLocusTextField();
				return false;					
			}
		}else {
			//Gene
			Reference reference = (Reference)boxReference.getSelectedItem();
			try {
				String ensg;
				if (locus.toLowerCase().startsWith("ens")) {
					ensg = locus.toUpperCase();
				}else {
					ensg = DBUtils.getEnsemblGene(reference, locus);
				}
				String enst = DBUtils.getEnsemblCanonicalTranscript(reference, ensg);
				chr = DBUtils.getChromosome(reference, ensg);
				Gene gene = new Gene(enst, reference, chr, true);
				int start = gene.getTranscriptionStart();
				int stop = gene.getTranscriptionEnd();
				window = (stop - start) / 2;
				pos = start + window;
			}catch(Exception ex) {
				JOptionPane.showMessageDialog(this,"The gene '"+locus+"' you entered in locus field is not recognized", "Locus recognition", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				updateLocusTextField();
				return false;					
			}
		}
		offset = 0;
		lastVariant = new Variant(chr,pos);
		updateLocusTextField();
		return true;
	}
	
	private JPanel getAlignment(String sample, AnalysisFull analysis, boolean showReference) {
		if (lastVariant != null) {
			Interval interval = new Interval(analysis.getReference(), lastVariant.getChromosome(), lastVariant.getPosition()+offset-window, lastVariant.getPosition()+offset+window);
			try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						bar.setValue(0);
						bar.setString("Loading Alignment");
						bar.setStringPainted(true);
					}
				});
				int width = split.getRightComponent().getWidth()-30;
				if (width / interval.getSize() < 4) {
					width = interval.getSize() * 4;
				}
				final AlignmentPanel alignment = BamViewer.getAlignmentPanel(analysis, sample, interval, lastVariant, softClipped, squished, frameShift, colorBy, showReference, width, bar);
				alignment.addMouseListener(new MouseListener() {
					@Override
					public void mouseReleased(MouseEvent e) {
					}
					@Override
					public void mousePressed(MouseEvent e) {
					}
					@Override
					public void mouseExited(MouseEvent e) {
					}
					@Override
					public void mouseEntered(MouseEvent e) {
					}
					@Override
					public void mouseClicked(MouseEvent e) {					
						if (e.getButton() == MouseEvent.BUTTON3) {
							int newPos = alignment.getPosition(e.getPoint());
							if (newPos != -1) {
								txtLocus.setText(lastVariant.getChromosome()+":"+newPos);
								if (setCurrentLocus()) {
									loadDetails();
								}
							}
						}
						
					}
				});
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						bar.setString("Alignment loaded");
						bar.setStringPainted(true);
					}
				});
				return alignment;
			}catch(Exception ex) {
				return Tools.getMessage("Cannot get alignment panel", ex);
			}
		}else {
			return new JPanel();
		}
	}
	
	private void loadDetails() {
		if (tableSamples.getSelectedRow() != -1) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					mapCachedAlignments.clear();
					for (String label : mapPanels.keySet()) {
						String sample = (label.equals(SELECTION)) ? tableSamples.getValueAt(tableSamples.getSelectedRow(), 0).toString() : mapSamples.get(label);
						AnalysisFull analysis = (label.equals(SELECTION)) ? (AnalysisFull)boxAnalysis.getSelectedItem() : mapAnalyses.get(label);
						if (label.equals(SELECTION)) {
							JPanel panel = mapPanels.get(SELECTION);
							BorderLayout layout = (BorderLayout)panel.getLayout();
							JLabel jlabel = (JLabel)((JPanel)layout.getLayoutComponent(BorderLayout.NORTH)).getComponents()[0];
							jlabel.setText(sample + " [" + analysis + "]");
						}
						JPanel alignment = getAlignment(sample, analysis, label.equals(SELECTION));
						mapCachedAlignments.put(label, alignment);
						JScrollPane scrollAlignment = mapScrollPanes.get(label);
						scrollAlignment.setViewportView(null);
						scrollAlignment.setViewportView(alignment);
					}
					try{
						Thread.sleep(100);
					}catch (InterruptedException ex){
						Tools.exception(ex);
					}
					mapScrollPanes.get(SELECTION).getHorizontalScrollBar().setValue((mapScrollPanes.get(SELECTION).getHorizontalScrollBar().getMaximum()-mapScrollPanes.get(SELECTION).getHorizontalScrollBar().getVisibleAmount())/2);
				}
			}, "AlignmentViewer.loadDetails").start();
		}
	}
	
	private String getHelpText() {
		return "<html>"
				+ "<b>Help and tips:</b><br>"
				+ "<ul>"
				+ "<li>In the locus text box, you can enter a locus in the form <i>chr</i>:<i>start</i>-<i>stop</i> or <i>chr</i>:<i>pos</i>, a gene symbol (e.g. TEK) or an Ensembl gene id (e.g. ENSG00000120156).<br>Validate by pressing ENTER or pressing the validation button next to it.</li>"
				+ "<li><i>Right-click</i> under any position to re-center the viewer on it, and sort reads accordingly.</li>"
				+ "<li>Mouseover the reference nucleotide on which the viewer is centered to display information on bases and number of reads under it.</li>"
				+ "<li>Navigation buttons move of a number of bases equal to half the total bases displayed. Use the red button to recenter on the selected base.</li>"
				+ "<li>Read transparency reflects mapping quality (opaque means 60+). Mouseover a read to display its mapping quality.</li>"
				+ "<li>Base transparency reflects Phred score (opaque means 30+). Mouseover a base to display its Phred score.</li>"
				+ "<li>The 'pin' button will pin the selected sample in the bottom of the screen. Next you can select other samples to compare. Multiple samples can be pinned.</li>"
				+ "</ul>"
				+ "</html>";
	}
}
