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

package be.uclouvain.ngs.highlander.UI.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel.AddChoice;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.FilterType;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.LogicalOperator;

public class FilteringTree extends JDialog {

	public enum Result {DO_NOTHING, SUBMIT, COUNT}
	
	private FilterTreeModel model;
	private JTree tree;
	private JButton btnAddCustom;
	private JButton btnAddCustomAnd;
	private JButton btnAddCustomOr;
	private JButton btnAddmagic;
	private JButton btnAddmagicAnd;
	private JButton btnAddmagicOr;
	private JButton btnLoad;
	private JButton btnLoadAnd;
	private JButton btnLoadOr;
	private JButton btnFilterEdit;
	private FilteringPanel filteringPanel;
	
	private Result result = Result.DO_NOTHING;
	
	public FilteringTree(FilteringPanel filteringPanel){
		this.filteringPanel = filteringPanel;
		if (filteringPanel.getFilter() != null){
			model = new FilterTreeModel(filteringPanel.getFilter());
		}else{
			model = new FilterTreeModel(new ComboFilter());			
		}
		tree = new JTree(model);
		tree.setCellRenderer(new FilterTreeCellRenderer());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
      	refreshButtons() ;
      }
    }) ;
		tree.addMouseListener(new MouseListener() {
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
				if (e.getClickCount() == 2){
					FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
					if (node != null && node.isEditable()){
						node.getFilter().editFilter();
						//model.reload(node); //doesn't seem to work with prefilters
						refreshTree();
					}
				}
				
			}
		});
		initUI();
		refreshButtons();
		expandAllPath((FilterNode)model.getRoot());
	}
	
	public static BufferedImage getFilterImage(ComboFilter filter){
		JTree tree = new JTree(new FilterTreeModel(filter));
		tree.setCellRenderer(new FilterTreeCellRenderer());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		for (int r=0 ; r < tree.getRowCount() ; r++){
			tree.expandRow(r);
		}
		tree.setSize(new Dimension(tree.getPreferredScrollableViewportSize().width, tree.getPreferredScrollableViewportSize().height));		
		BufferedImage image = new BufferedImage(tree.getWidth(), tree.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		tree.paint(g);
		return image;
	}
	
	private void initUI(){
		setTitle("Filtering tree");
		setIconImage(Resources.getScaledIcon(Resources.iFilter, 64).getImage());
		setModal(true);

		JPanel southPanel = new JPanel();
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		
		JButton btnClose = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 32));
		btnClose.setToolTipText("Fetch variants from the database using current filters");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				result = Result.SUBMIT;
				dispose();
			}
		});
		southPanel.add(btnClose);
		
		JButton btnCount = new JButton(Resources.getScaledIcon(Resources.iCount, 32));
		btnCount.setToolTipText("Count variants from the database using current filters");
		btnCount.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				result = Result.COUNT;
				dispose();
			}
		});
		southPanel.add(btnCount);
		
		JScrollPane scroll = new JScrollPane(tree);
		getContentPane().add(scroll, BorderLayout.CENTER);
		
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		getContentPane().add(northPanel, BorderLayout.NORTH);
		
		JButton btnExpand = new JButton(Resources.getScaledIcon(Resources.iTreeExpand, 40));
		btnExpand.setToolTipText("Expand all below selected filter node");
		btnExpand.setPreferredSize(new Dimension(54,54));
		btnExpand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;
				if (node != null){
					expandAllPath(node);
				}else{
					expandAllPath((FilterNode)model.getRoot());
				}
			}
		});
		northPanel.add(btnExpand);
		
		JButton btnSave = new JButton(Resources.getScaledIcon(Resources.iDbSave, 40));
		btnSave.setToolTipText("Save current filter in your profile");
		btnSave.setPreferredSize(new Dimension(54,54));
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				filteringPanel.saveCurrentFilter(FilteringTree.this);
			}
		});
		northPanel.add(btnSave);
		
		btnLoad = new JButton(Resources.getScaledIcon(Resources.iDbLoad, 40));
		btnLoad.setToolTipText("Load a filter from your profile");
		btnLoad.setPreferredSize(new Dimension(54,54));
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				filteringPanel.addFilter(AddChoice.LoadCombo, null, FilteringTree.this);
				refreshTree();
			}
		});
		northPanel.add(btnLoad);

		btnLoadAnd = new JButton(Resources.getScaledIcon(Resources.iFilterLoadAnd, 40));
		btnLoadAnd.setToolTipText("Add a filter from your profile to the selected one, using the logical operator AND (i.e. results will be the INTERSECTION of filters)");
		btnLoadAnd.setPreferredSize(new Dimension(54,54));
		btnLoadAnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
				if (node == null || node.isRoot()){
					filteringPanel.addFilter(AddChoice.LoadCombo, LogicalOperator.AND, FilteringTree.this);
				}else{
					((ComboFilter)node.getFilter()).addProfileFilter(LogicalOperator.AND);
				}
				refreshTree();
			}
		});
		btnLoadAnd.setVisible(false);
		northPanel.add(btnLoadAnd);
		
		btnLoadOr = new JButton(Resources.getScaledIcon(Resources.iFilterLoadOr, 40));
		btnLoadOr.setToolTipText("Add a filter from your profile to the selected one, using the logical operator OR (i.e. results will be the UNION of filters)");
		btnLoadOr.setPreferredSize(new Dimension(54,54));
		btnLoadOr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
				if (node == null || node.isRoot()){
					filteringPanel.addFilter(AddChoice.LoadCombo, LogicalOperator.OR, FilteringTree.this);
				}else{
					((ComboFilter)node.getFilter()).addProfileFilter(LogicalOperator.OR);
				}
				refreshTree();
			}
		});
		btnLoadOr.setVisible(false);
		northPanel.add(btnLoadOr);
		
		btnAddCustom = new JButton(Resources.getScaledIcon(Resources.iFilterAddCustom, 40));
		btnAddCustom.setToolTipText("Add a new custom filter");
		btnAddCustom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
	  			public void run(){
	  				filteringPanel.addFilter(AddChoice.NewCustom, null, FilteringTree.this);
	  				refreshTree();
	  			}
	  		}, "FilteringTree.addCustom").start();				
			}
		});
		btnAddCustom.setPreferredSize(new Dimension(54,54));
		northPanel.add(btnAddCustom);
		
		btnAddCustomAnd = new JButton(Resources.getScaledIcon(Resources.iFilterAddCustomAnd, 40));
		btnAddCustomAnd.setToolTipText("Add a custom filter to the selected one, using the logical operator AND (i.e. results will be the INTERSECTION of filters)");
		btnAddCustomAnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
	  			public void run(){
	  				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
	  				if (node == null || node.isRoot()){
		  				filteringPanel.addFilter(AddChoice.NewCustom, LogicalOperator.AND, FilteringTree.this);
	  				}else if (node.getFilter().getFilterType() == FilterType.COMBO){
	  					((ComboFilter)node.getFilter()).addNewCustomFilter(LogicalOperator.AND);
	  				}else if (node.getFilter().getFilterType() == FilterType.CUSTOM){
	  					((CustomFilter)node.getFilter()).addCriterion(LogicalOperator.AND);
	  				}
	  				refreshTree();
	  			}
	  		}, "FilteringTree.addCustomAnd").start();
			}
		});
		btnAddCustomAnd.setPreferredSize(new Dimension(54,54));
		btnAddCustomAnd.setVisible(false);
		northPanel.add(btnAddCustomAnd);
		
		btnAddCustomOr = new JButton(Resources.getScaledIcon(Resources.iFilterAddCustomOr, 40));
		btnAddCustomOr.setToolTipText("Add a custom filter to the selected one, using the logical operator OR (i.e. results will be the UNION of filters)");
		btnAddCustomOr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
	  			public void run(){
	  				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
	  				if (node == null || node.isRoot()){
		  				filteringPanel.addFilter(AddChoice.NewCustom, LogicalOperator.OR, FilteringTree.this);
	  				}else if (node.getFilter().getFilterType() == FilterType.COMBO){
	  					((ComboFilter)node.getFilter()).addNewCustomFilter(LogicalOperator.OR);
	  				}else if (node.getFilter().getFilterType() == FilterType.CUSTOM){
	  					((CustomFilter)node.getFilter()).addCriterion(LogicalOperator.OR);
	  				}
	  				refreshTree();
	  			}
	  		}, "FilteringTree.addCustomOr").start();
			}
		});
		btnAddCustomOr.setPreferredSize(new Dimension(54,54));
		btnAddCustomOr.setVisible(false);
		northPanel.add(btnAddCustomOr);

		btnAddmagic = new JButton(Resources.getScaledIcon(Resources.iFilterAddMagic, 40));
		btnAddmagic.setToolTipText("Add a new magic filter");
		btnAddmagic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					new Thread(new Runnable(){
		  			public void run(){
		  				filteringPanel.addFilter(AddChoice.NewMagic, null, FilteringTree.this);
		  				refreshTree();
		  			}
		  		}, "FilteringTree.addMagic").start();
			}
		});
		btnAddmagic.setPreferredSize(new Dimension(54,54));
		northPanel.add(btnAddmagic);
		
		btnAddmagicAnd = new JButton(Resources.getScaledIcon(Resources.iFilterAddMagicAnd, 40));
		btnAddmagicAnd.setToolTipText("Add a magic filter to the selected one, using the logical operator AND (i.e. results will be the INTERSECTION of filters)");
		btnAddmagicAnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
	  				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
	  				if (node == null || node.isRoot()){
							filteringPanel.addFilter(AddChoice.NewMagic, LogicalOperator.AND, FilteringTree.this);
	  				}else if (node.getFilter().getFilterType() == FilterType.COMBO){
	  					((ComboFilter)node.getFilter()).addNewMagicFilter(LogicalOperator.AND);
	  				}
	  				refreshTree();
					}
				}, "FilteringTree.addMagicAnd").start();
			}
		});
		btnAddmagicAnd.setPreferredSize(new Dimension(54,54));
		btnAddmagicAnd.setVisible(false);
		northPanel.add(btnAddmagicAnd);

		btnAddmagicOr = new JButton(Resources.getScaledIcon(Resources.iFilterAddMagicOr, 40));
		btnAddmagicOr.setToolTipText("Add a magic filter to the selected one, using the logical operator OR (i.e. results will be the UNION of filters)");
		btnAddmagicOr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
	  				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
	  				if (node == null || node.isRoot()){
							filteringPanel.addFilter(AddChoice.NewMagic, LogicalOperator.OR, FilteringTree.this);
	  				}else if (node.getFilter().getFilterType() == FilterType.COMBO){
	  					((ComboFilter)node.getFilter()).addNewMagicFilter(LogicalOperator.OR);
	  				}
	  				refreshTree();
					}
				}, "FilteringTree.addMagicOr").start();
			}
		});
		btnAddmagicOr.setPreferredSize(new Dimension(54,54));
		btnAddmagicOr.setVisible(false);
		northPanel.add(btnAddmagicOr);

		btnFilterEdit = new JButton(Resources.getScaledIcon(Resources.iFilterEdit, 40));
		btnFilterEdit.setToolTipText("Edit selected filter");
		btnFilterEdit.setPreferredSize(new Dimension(54,54));
		btnFilterEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
				if (node != null){
					node.getFilter().editFilter();
					//model.reload(node); //doesn't seem to work with prefilters
					refreshTree();
				}
			}
		});
		northPanel.add(btnFilterEdit);
		
		JButton btnDelete = new JButton(Resources.getScaledIcon(Resources.iCross, 40));
		btnDelete.setToolTipText("Remove selected filter");
		btnDelete.setPreferredSize(new Dimension(54,54));
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
	  			public void run(){
	  				FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;	  				
	  				if (node == null || node.isRoot()){
	  					filteringPanel.clearFilter();
	  				}else{
	  					node.getFilter().delete();
	  				}
	  				refreshTree();
	  			}
	  		}, "FilteringTree.delete").start();
			}
		});
		northPanel.add(btnDelete);
	}
	
	void collapseAllPath(FilterNode node) {
		for (int i=0 ; i < node.getChildCount() ; i++ ) {
			collapseAllPath((FilterNode)node.getChildAt(i));
		}
		if (!node.isRoot()) tree.collapsePath(new TreePath(node.getPath()));
	}

	void expandAllPath(FilterNode node) {
		for (int i=0 ; i < node.getChildCount() ; i++ ) {
			expandAllPath((FilterNode)node.getChildAt(i));
		}
		if (!node.isLeaf()) tree.expandPath(new TreePath(node.getPath()));
	}
	
	public void refreshTree(){
		FilterNode root;
		if (filteringPanel.getFilter() != null){
			root = new FilterNode(filteringPanel.getFilter());
		}else{
			root = new FilterNode(new ComboFilter());
		}
		model.setRoot(root);
		model.buildTree(root);
		expandAllPath(root);
		refreshButtons();
	}
	
	public void refreshButtons(){	
		btnAddCustom.setVisible(false);
		btnAddCustomOr.setVisible(false);
		btnAddCustomAnd.setVisible(false);
		btnAddmagic.setVisible(false);
		btnAddmagicOr.setVisible(false);
		btnAddmagicAnd.setVisible(false);
		btnLoad.setVisible(false);
		btnLoadOr.setVisible(false);
		btnLoadAnd.setVisible(false);
		btnFilterEdit.setVisible(false);
		ComboFilter filter = filteringPanel.getFilter();
		if (filter == null){
			btnAddCustom.setVisible(true);
			btnAddmagic.setVisible(true);
			btnLoad.setVisible(true);
		}else {
			FilterNode node = (FilterNode) tree.getLastSelectedPathComponent() ;
			if(node == null || node.isRoot()){
				if (filter.isSimple()){
					if (filter.getFilter().getFilterType() != FilterType.CUSTOM || filter.getFilter().isSimple()){
						btnAddCustomOr.setVisible(true);
						btnAddCustomAnd.setVisible(true);
						btnAddmagicOr.setVisible(true);
						btnAddmagicAnd.setVisible(true);
						btnLoadOr.setVisible(true);
						btnLoadAnd.setVisible(true);
					}else{
						btnAddCustomOr.setVisible(((CustomFilter)filter.getFilter()).getLogicalOperator() == LogicalOperator.OR);
						btnAddCustomAnd.setVisible(((CustomFilter)filter.getFilter()).getLogicalOperator() == LogicalOperator.AND);
						btnAddmagicOr.setVisible(true);
						btnAddmagicAnd.setVisible(true);
						btnLoadOr.setVisible(true);
						btnLoadAnd.setVisible(true);
					}
				}else{
					btnAddCustomOr.setVisible(filter.getLogicalOperator() == LogicalOperator.OR);
					btnAddCustomAnd.setVisible(filter.getLogicalOperator() == LogicalOperator.AND);
					btnAddmagicOr.setVisible(filter.getLogicalOperator() == LogicalOperator.OR);
					btnAddmagicAnd.setVisible(filter.getLogicalOperator() == LogicalOperator.AND);
					btnLoadOr.setVisible(filter.getLogicalOperator() == LogicalOperator.OR);
					btnLoadAnd.setVisible(filter.getLogicalOperator() == LogicalOperator.AND);
				}
			}else{
				switch(node.getFilter().getFilterType()){
				case COMBO:
					if (node.getFilter().isComplex()){
						if (node.getFilter().getLogicalOperator() == LogicalOperator.OR){
							btnAddCustomOr.setVisible(true);
							btnAddmagicOr.setVisible(true);
							btnLoadOr.setVisible(true);
						}else{
							btnAddCustomAnd.setVisible(true);
							btnAddmagicAnd.setVisible(true);
							btnLoadAnd.setVisible(true);
						}
					}else{
						if (node.getFilter().getParentFilter().getLogicalOperator() == LogicalOperator.AND){
							btnAddCustomOr.setVisible(true);
							btnAddmagicOr.setVisible(true);
							btnLoadOr.setVisible(true);
						}else{
							btnAddCustomAnd.setVisible(true);
							btnAddmagicAnd.setVisible(true);
							btnLoadAnd.setVisible(true);
						}
					}
					break;
				case CUSTOM:
					if (node.getFilter().isComplex()){
						if (node.getFilter().getLogicalOperator() == LogicalOperator.OR){
							btnAddCustomOr.setVisible(true);
						}else{
							btnAddCustomAnd.setVisible(true);
						}
					}else{
						if (node.getFilter().getParentFilter().getLogicalOperator() == LogicalOperator.AND){
							btnAddCustomOr.setVisible(true);
						}else{
							btnAddCustomAnd.setVisible(true);
						}
						btnFilterEdit.setVisible(true);
					}
					break;
				default : //MAGIC
					btnFilterEdit.setVisible(true);
					break;
				}
			}
		}
		validate();
		repaint();
		Highlander.getHighlanderObserver().setControlName("RESIZE_TOOLBAR");
	}

	public static class FilterTreeModel extends DefaultTreeModel {
		
		public FilterTreeModel(ComboFilter filter){
			super(new FilterNode(filter));
			buildTree((FilterNode)root);
		}
		
		private void buildTree(FilterNode node){
			if (!node.isFilterLeaf()){
				for (int i=0 ; i < node.getFilterChildCount() ; i++){
					FilterNode child = new FilterNode(node.getFilterChild(i));
					node.add(child);
					buildTree(child);
				}
			}
		}
		
	}
	
	public static class FilterNode extends DefaultMutableTreeNode {
		private Filter filter;
		
		public FilterNode(Filter filter){
			super(filter, true);
			this.filter = filter;
		}
		
		public Filter getFilter(){
			return filter;
		}
		
		public Filter getFilterChild(int index) {
			return filter.getSubFilter(index);
		}

		public int getFilterChildCount() {
			return filter.getSubFilterCount();
		}

		public boolean isFilterLeaf() {
			if(filter.getFilterType() == FilterType.COMBO){
				return false;
			}else{
				return filter.isSimple();
			}
		}
		
		public int getIndexOfChild(Filter child) {
			for (int i=0 ; i < filter.getSubFilterCount() ; i++){
				if (filter.getSubFilter(i) == child) return i;
			}
			return -1;
		}
		
		public boolean isEditable(){
			boolean editable = isLeaf();
			if (filter.getFilterType() != FilterType.COMBO && filter.getFilterType() != FilterType.CUSTOM) editable = true;
			return editable;
		}
	}
	
	public static class FilterTreeCellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree,	Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			Filter f = ((FilterNode)value).getFilter();
			if (!f.isSimple() && !f.isComplex()){
				//Empty tree
				setIcon(Resources.getScaledIcon(Resources.iFilter, 24));
				setText("Empty filter");
			}else{
				switch(f.getFilterType()){
				case COMBO:
					if (f.isSimple()){
						setIcon(Resources.getScaledIcon(Resources.iFilter, 24));
						if (expanded){
							switch(((ComboFilter)f).getFilter().getFilterType()){
							case CUSTOM:
								setText("Custom filter");
								break;
							default:
								setText("Magic filter");
								break;
							}
						}
					}else{
						switch(((ComboFilter)f).getLogicalOperator()){
						case AND:
							setIcon(Resources.getScaledIcon(Resources.iFilterAnd, 24));
							if (expanded){
								setText("Intersection of filters");
							}
							break;
						case OR:
							setIcon(Resources.getScaledIcon(Resources.iFilterOr, 24));
							if (expanded){
								setText("Union of filters");
							}
							break;
						}
					}
					break;
				case CUSTOM:
					if (f.isSimple()){
						setIcon(Resources.getScaledIcon(Resources.iFilterCustom, 24));					
					}else{
						switch(((CustomFilter)f).getLogicalOperator()){
						case AND:
							setIcon(Resources.getScaledIcon(Resources.iFilterAnd, 24));
							if (expanded){
								setText("Intersection of custom filters");
							}
							break;
						case OR:
							setIcon(Resources.getScaledIcon(Resources.iFilterOr, 24));
							if (expanded){
								setText("Union of custom filters");
							}
							break;
						}
					}
					break;
				default:
					setIcon(Resources.getScaledIcon(Resources.iFilterMagic, 24));
					if (expanded){
						String txt = getText();
						String prefiltTxt = "with prefiltering:";
						int index = txt.indexOf(prefiltTxt);
						if (index > 0){
							txt = txt.substring(0, index+prefiltTxt.length());
						}
						setText(txt);
					}
					break;
				} 
			}
			return this;
		}
	}
	
	public Result getResult(){
		return result;
	}
}
