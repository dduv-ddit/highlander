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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.apache.batik.ext.swing.GridBagConstants;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Field.Tag;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class DetailsPanel extends JScrollPane implements DropTargetListener {

	private DropTarget target;
	private List<DetailsBox> detailsBoxes;
	private List<DetailsBox> defaultOrder = new ArrayList<DetailsBox>();	
	private Map<String,String> userSettingsPosition = new HashMap<String, String>();
	private Map<String,String> userSettingsVisibility = new HashMap<String, String>();
	private JPanel mainPanel;
	private AutoCompleteSupport<Field> support;
	private VariantsTable variantsTable;
	
	public DetailsPanel(VariantsTable variantsTable) {
		this.variantsTable = variantsTable;
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		if(target==null) target = new DropTarget(this,this);		
		setTransferHandler(new DetailsBoxTransferHandler());
	}

	public void dragEnter(DropTargetDragEvent dtde) {}
	public void dragOver(DropTargetDragEvent dtde) {}
	public void dropActionchanged(DropTargetDragEvent dtde) {}
	public void dragExit(DropTargetEvent dte) {}
	public void dropActionChanged(DropTargetDragEvent arg0) {}

	public void drop(DropTargetDropEvent dtde) {
		try {
			Point loc = dtde.getLocation(); 
			Transferable t = dtde.getTransferable();
			DataFlavor[] d = t.getTransferDataFlavors();
			if(getTransferHandler().canImport(this, d)){
				((DetailsBoxTransferHandler)getTransferHandler()).importData(this, t, loc);
			}
			else return;
		} catch (Exception ex) {
			Tools.exception(ex);
		}
		finally{ dtde.dropComplete(true); }
	}

	class DetailsBoxTransferHandler extends TransferHandler {

		public boolean canImport(JComponent c, DataFlavor[] f){
			DataFlavor temp = new DataFlavor(String.class, "DetailsBox");
			for(DataFlavor d:f){
				if(d.equals(temp))
					return true;

			}
			return false;
		}

		public boolean importData(JComponent comp, Transferable t, Point p){
			try {
				String boxTitle = (String)t.getTransferData(new DataFlavor(String.class, "DetailsBox"));
				SwingUtilities.convertPointToScreen(p, comp);
				return moveDetailsBox(boxTitle, p);
			} catch (UnsupportedFlavorException ex) {
				Tools.exception(ex);
			} catch (IOException ex) {
				Tools.exception(ex);
			}
			return false;
		}

	}

	public boolean moveDetailsBox(String boxTitle, Point screenCoordinates){
		int oldPos;
		DetailsBox box = null;
		for (oldPos = 0 ; oldPos < mainPanel.getComponentCount() ; oldPos++){
			DetailsBox db = (DetailsBox)(mainPanel.getComponent(oldPos));
			if (db.getTitle().equals(boxTitle)){
				box = db;
				break;
			}
		}
		int newPos;
		for (newPos = 0 ; newPos < mainPanel.getComponentCount() ; newPos++){
			Component c = mainPanel.getComponent(newPos);
			if (screenCoordinates.getY() <= (c.getLocationOnScreen().getY()+c.getHeight())) break;
		}
		if (oldPos == newPos) return false;
		if (oldPos < newPos)	newPos--;			
		detailsBoxes.remove(oldPos);
		detailsBoxes.add(newPos, box);			
		for (int i=0 ; i < detailsBoxes.size() ; i++){
			try{
				Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.POSITION, detailsBoxes.get(i).getTitle(), ""+i);
			}catch (Exception ex){
				Tools.exception(ex);
			}
		}
		orderDialogBoxes();
		validate();
		return true;
	}

	public void setSelection(final int variantId, final VariantsTable table){
		new Thread(new Runnable(){
			public void run(){
				try{	
					int currentScrollPos = getVerticalScrollBar().getValue();
					try{
						userSettingsPosition = Highlander.getLoggedUser().loadSettings(Highlander.getCurrentAnalysis().toString(), Settings.POSITION);						
						userSettingsVisibility = Highlander.getLoggedUser().loadSettings(Highlander.getCurrentAnalysis().toString(), Settings.VISIBLE);						
					}catch (Exception ex){
						Tools.exception(ex);
					}
					DetailsBox[] knownOrder = new DetailsBox[userSettingsPosition.size()];
					List<DetailsBox> unknownOrder = new ArrayList<DetailsBox>();
					addBox(new DetailsBoxAlignment(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxBamOut(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxExternalLinks(table.getSelectedVariantsId(), DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxEvaluationAnnotations(table.getSelectedVariantsId(), DetailsPanel.this, table), knownOrder, unknownOrder);
					addBox(new DetailsBoxPrivateAnnotations(table.getSelectedVariantsId(), DetailsPanel.this, table), knownOrder, unknownOrder);
					addBox(new DetailsBoxPublicAnnotations(variantId, DetailsPanel.this, table), knownOrder, unknownOrder);
					addBox(new DetailsBoxOtherEvaluations(variantId, DetailsPanel.this, false), knownOrder, unknownOrder);					
					addBox(new DetailsBoxOtherEvaluations(variantId, DetailsPanel.this, true), knownOrder, unknownOrder);		
					addBox(new DetailsBoxAlleleFrequencyTable(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxAlleleFrequencyDetailled(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxSpiderChart(table.getSelectedVariantsId(), DetailsPanel.this, Tag.IMPACT_RANKSCORE), knownOrder, unknownOrder);
					addBox(new DetailsBoxSpiderChart(table.getSelectedVariantsId(), DetailsPanel.this, Tag.CONSERVATION_RANKSCORE), knownOrder, unknownOrder);
					for (Category category : Category.getAvailableCategories()){
						if (category.hasGenericDetailBox()){
							DetailsBox box = new DetailsBoxFields(variantId, DetailsPanel.this, category);
							addBox(box, knownOrder, unknownOrder);
						}
					}
					addBox(new DetailsBoxVariantInFamily(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxOtherTranscriptPrediction(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxCoverage(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					addBox(new DetailsBoxRun(variantId, DetailsPanel.this), knownOrder, unknownOrder);
					detailsBoxes = new ArrayList<DetailsBox>();
					for (DetailsBox box : knownOrder){
						if (box != null) detailsBoxes.add(box);
					}
					for (DetailsBox box : unknownOrder){
						detailsBoxes.add(box);
					}
					JPanel borderPanel = new JPanel(new BorderLayout(0,0));
					int y=0;
					int x=0;
					JPanel bringOnTopPanel = new JPanel(new GridBagLayout());
					JLabel categoriesLabel = new JLabel("Categories", Resources.getScaledIcon(Resources.iSearch, 24), SwingConstants.LEFT);
					bringOnTopPanel.add(categoriesLabel, new GridBagConstraints(x++, y, 1, 1, 0.0, 0.0, GridBagConstants.WEST, GridBagConstraints.NONE, new Insets(3, 5, 3, 5), 0, 0));
					DetailsBox[] categoriesArr = new TreeSet<DetailsBox>(detailsBoxes).toArray(new DetailsBox[0]);
					EventList<DetailsBox> categories = GlazedLists.eventListOf(categoriesArr);
					final JComboBox<DetailsBox> detailsBoxesComboBox = new JComboBox<DetailsBox>(categoriesArr);
					detailsBoxesComboBox.setPreferredSize(new Dimension(100,26));
					detailsBoxesComboBox.setMaximumRowCount(20);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							AutoCompleteSupport<DetailsBox> support = AutoCompleteSupport.install(detailsBoxesComboBox, categories);
							support.setCorrectsCase(true);
							support.setFilterMode(TextMatcherEditor.CONTAINS);
							support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
							support.setStrict(false);
						}
					});		
					bringOnTopPanel.add(detailsBoxesComboBox, new GridBagConstraints(x++, y, 1, 1, 1.0, 0.0, GridBagConstants.WEST, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));
					JButton bringOnTopButton = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleUp, 20));
					bringOnTopButton.setToolTipText("Bring selected box on top");
					bringOnTopButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									moveDetailsBox(detailsBoxesComboBox.getItemAt(detailsBoxesComboBox.getSelectedIndex()).getTitle(), new Point(0,0));
								}
							});
						}
					});
					bringOnTopPanel.add(bringOnTopButton, new GridBagConstraints(x++, y, 1, 1, 0.0, 0.0, GridBagConstants.WEST, GridBagConstraints.NONE, new Insets(3, 0, 3, 5), 0, 0));
					JButton resetButton = new JButton(Resources.getScaledIcon(Resources.iReset, 20));
					resetButton.setToolTipText("Reset boxes to default order");
					resetButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you sure you want to reset all boxes to their default ordering ?", "Reset boxes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iReset,64));
							if (res == JOptionPane.YES_OPTION){
								new Thread(new Runnable() {
									@Override
									public void run() {
										detailsBoxes.clear();
										for (int i=0 ; i < defaultOrder.size() ; i++){
											detailsBoxes.add(defaultOrder.get(i));
											try {
												Highlander.getLoggedUser().deleteData(UserData.SETTINGS, Highlander.getCurrentAnalysis().toString(), Settings.POSITION+"|"+defaultOrder.get(i).getTitle());
											}catch(Exception ex) {
												ex.printStackTrace();
											}
										}
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												orderDialogBoxes();
												validate();
											}
										});
									}
								}).start(); 
							}
						}
					});
					bringOnTopPanel.add(resetButton, new GridBagConstraints(x++, y, 1, 1, 0.0, 0.0, GridBagConstants.WEST, GridBagConstraints.NONE, new Insets(3, 0, 3, 5), 0, 0));
					y++;
					x=0;
					JLabel fieldsLabel = new JLabel("Fields", Resources.getScaledIcon(Resources.iSearch, 24), SwingConstants.LEFT);
					bringOnTopPanel.add(fieldsLabel, new GridBagConstraints(x++, y, 1, 1, 0.0, 0.0, GridBagConstants.WEST, GridBagConstraints.NONE, new Insets(3, 5, 3, 5), 0, 0));
					Field[] fieldsArr = Field.getAvailableFields(Highlander.getCurrentAnalysis(), true).toArray(new Field[0]);
					EventList<Field> fields = GlazedLists.eventListOf(fieldsArr);
					final JComboBox<Field> comboBox_field = new JComboBox<Field>(fieldsArr);
					comboBox_field.setMaximumRowCount(20);
					comboBox_field.setPreferredSize(new Dimension(100,26));
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							support = AutoCompleteSupport.install(comboBox_field, fields);
							support.setCorrectsCase(true);
							support.setFilterMode(TextMatcherEditor.CONTAINS);
							support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
							support.setStrict(false);
						}
					});		
					comboBox_field.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							if (arg0.getActionCommand().equals("comboBoxEdited")){
								if (comboBox_field.getSelectedIndex() < 0) comboBox_field.setSelectedItem(null);
							}
							ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
							renderer.setTooltips(support.getItemList());
							comboBox_field.setRenderer(renderer);
						}
					});
					comboBox_field.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent arg0) {
							if (arg0.getStateChange() == ItemEvent.SELECTED){
								if (comboBox_field.getSelectedIndex() >= 0){
									Field field = (Field)comboBox_field.getSelectedItem();
									for (DetailsBox b : detailsBoxes) {
										if (b instanceof DetailsBoxFields) {
											DetailsBoxFields box = (DetailsBoxFields)b;	
											if (box.getCategory().equals(field.getCategory())) {
												box.highlight(field);
											}
										}
									}
								}
							}else {
								for (DetailsBox b : detailsBoxes) {
									if (b instanceof DetailsBoxFields) {
										DetailsBoxFields box = (DetailsBoxFields)b;	
										box.highlight(null);
									}
								}
							}
						}
					});
					ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
					renderer.setTooltips(Field.getAvailableFields(Highlander.getCurrentAnalysis(), true));
					comboBox_field.setRenderer(renderer);
					bringOnTopPanel.add(comboBox_field, new GridBagConstraints(x++, y, 1, 1, 1.0, 0.0, GridBagConstants.WEST, GridBagConstraints.HORIZONTAL, new Insets(3, 5, 3, 5), 0, 0));
					JButton gotoButton = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleDown, 20));
					gotoButton.setToolTipText("Scroll to field");
					gotoButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (comboBox_field.getSelectedIndex() >= 0){
								new Thread(new Runnable() {
									@Override
									public void run() {
										Field field = (Field)comboBox_field.getSelectedItem();
										variantsTable.scrollToColumn(field);
										for (DetailsBox b : detailsBoxes) {
											if (field.getTableSuffix().startsWith("_user_annotations")) {
												if (field.getName().contains("public")) {
													if (b instanceof DetailsBoxPublicAnnotations) {
														SwingUtilities.invokeLater(new Runnable() {
															public void run() {
																b.expand();
																getVerticalScrollBar().setValue(b.getLocation().y+80);
															}
														});
														return;
													}
												}else if (field.getName().contains("private") || field.getName().contains("of_interest")) {
													if (b instanceof DetailsBoxPrivateAnnotations) {
														SwingUtilities.invokeLater(new Runnable() {
															public void run() {
																b.expand();
																getVerticalScrollBar().setValue(b.getLocation().y+80);
															}
														});
														return;
													}
												}else {
													if (b instanceof DetailsBoxEvaluationAnnotations) {
														SwingUtilities.invokeLater(new Runnable() {
															public void run() {
																b.expand();
																getVerticalScrollBar().setValue(b.getLocation().y+80);
															}
														});
														return;
													}
												}
											}else {
												if (b instanceof DetailsBoxFields) {
													DetailsBoxFields box = (DetailsBoxFields)b;	
													if (box.getCategory().equals(field.getCategory())) {
														SwingUtilities.invokeLater(new Runnable() {
															public void run() {
																b.expand();
																getVerticalScrollBar().setValue(box.getLocation().y+80);
															}
														});
														return;
													}
												}										
											}
										}
									}
								}).start();
							}
						}
					});
					bringOnTopPanel.add(gotoButton, new GridBagConstraints(x++, y, 1, 1, 0.0, 0.0, GridBagConstants.WEST, GridBagConstraints.NONE, new Insets(3, 0, 3, 5), 0, 0));
					JButton collapseButton = new JButton(Resources.getScaledIcon(Resources.i2dMinus, 20));
					collapseButton.setToolTipText("Collapse all boxes");
					collapseButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									for (DetailsBox b : detailsBoxes) {
										b.collapse();
									}
								}
							}).start();
						}
					});
					bringOnTopPanel.add(collapseButton, new GridBagConstraints(x++, y, 1, 1, 0.0, 0.0, GridBagConstants.WEST, GridBagConstraints.NONE, new Insets(3, 0, 3, 5), 0, 0));
					y++;
					x=0;
					borderPanel.add(bringOnTopPanel, BorderLayout.NORTH);
					mainPanel = new JPanel();
					mainPanel.setLayout(new GridBagLayout());
					orderDialogBoxes();
					borderPanel.add(mainPanel, BorderLayout.CENTER);
					setViewportView(borderPanel);					
					getVerticalScrollBar().setValue(currentScrollPos);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		}, "DetailsPanel.setSelection").start();
	}

	private void addBox(DetailsBox box, DetailsBox[] knownOrder, List<DetailsBox> unknownOrder){
		int pos = -1;
		if (userSettingsPosition.containsKey(Settings.POSITION+"|"+box.getTitle())){ 
			try{
				pos = Integer.parseInt(userSettingsPosition.get(Settings.POSITION+"|"+box.getTitle()));
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
		if (pos > -1 && knownOrder[pos] == null){
			knownOrder[pos] = box;
		}else{
			unknownOrder.add(box);
		}
		defaultOrder.add(box);
	}

	private void orderDialogBoxes(){
		mainPanel.removeAll();
		for (int i=0 ; i < detailsBoxes.size() ; i++){
			mainPanel.add(detailsBoxes.get(i), new GridBagConstraints(0, i, 1, 1, 1.0, 0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		}
	}

	public boolean isBoxVisible(String boxTitle){
		if (userSettingsVisibility.containsKey(Settings.VISIBLE+"|"+boxTitle)){ 
			return userSettingsVisibility.get(Settings.VISIBLE+"|"+boxTitle).equals("1");
		}
		return false;
	}
	
	public class ComboboxToolTipRenderer extends DefaultListCellRenderer {
		List<Field> tooltips;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			JComponent comp = (JComponent) super.getListCellRendererComponent(list,
					value, index, isSelected, cellHasFocus);

			if (-1 < index && null != value && null != tooltips) {
				list.setToolTipText(tooltips.get(index).getHtmlTooltip());
			}
			return comp;
		}

		public void setTooltips(List<Field> tooltips) {
			this.tooltips = tooltips;
		}
	}


}
