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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.dialog.AskListOfHPOTermDialog;
import be.uclouvain.ngs.highlander.UI.dialog.AskListOfFreeValuesDialog;
import be.uclouvain.ngs.highlander.UI.dialog.AskListOfIntervalsDialog;
import be.uclouvain.ngs.highlander.UI.dialog.CommentsManager;
import be.uclouvain.ngs.highlander.UI.dialog.CreateTemplate;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.UseTemplate;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.administration.users.User.TargetColor;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;

public class ProfilePanel extends JPanel {

	final Highlander mainFrame;
	private JButton changeTableColor;
	private JButton changeSameVariantColor;
	
	public ProfilePanel(Highlander mainFrame){
		this.mainFrame = mainFrame;
		initUI();
	}
	
	private void initUI(){
		setLayout(new BorderLayout(0,0));
		
	  JButton userEditUserProfile = new JButton(Resources.getScaledIcon(Resources.iUserEdit, 40));
	  userEditUserProfile.setPreferredSize(new Dimension(54,54));
	  userEditUserProfile.setToolTipText("Edit user profile");
	  userEditUserProfile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	new Thread(new Runnable(){
      		public void run(){
      			User.editUserProfile();
      		}
      	}, "ProfilePanel.editUserProfile").start();
      }
    });
	  
	  JButton userCreateUser = new JButton(Resources.getScaledIcon(Resources.iUserAdd, 40));
	  userCreateUser.setPreferredSize(new Dimension(54,54));
	  userCreateUser.setToolTipText("Create new user");
	  userCreateUser.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				User.createUser();
	  			}
	  		}, "ProfilePanel.createUser").start();
	  	}
	  });
	  
	  JButton userDeleteUser = new JButton(Resources.getScaledIcon(Resources.iUserDelete, 40));
	  userDeleteUser.setPreferredSize(new Dimension(54,54));
	  userDeleteUser.setToolTipText("Delete existing user");
	  userDeleteUser.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				User.deleteUser(mainFrame);
	  			}
	  		}, "ProfilePanel.deleteUser").start();
	  	}
	  });

	  JButton userResetPassword = new JButton(Resources.getScaledIcon(Resources.iUserLock, 40));
	  userResetPassword.setPreferredSize(new Dimension(54,54));
	  userResetPassword.setToolTipText("Reset password of existing user");
	  userResetPassword.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				User.resetPassword(mainFrame);
	  			}
	  		}, "ProfilePanel.resetPassword").start();
	  	}
	  });
	  
	  JButton userPromoteUser = new JButton(Resources.getScaledIcon(Resources.iUserPromote, 40));
	  userPromoteUser.setPreferredSize(new Dimension(54,54));
	  userPromoteUser.setToolTipText("Promote existing user to administrator");
	  userPromoteUser.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				User.promoteUser(mainFrame);
	  			}
	  		}, "ProfilePanel.promoteUser").start();
	  	}
	  });
	  
	  JButton manageProfile = new JButton(Resources.getScaledIcon(Resources.iUserTree, 40));
	  manageProfile.setPreferredSize(new Dimension(54,54));
	  manageProfile.setToolTipText("Manage your profile");
	  manageProfile.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				manageProfile();
	  			}
	  		}, "ProfilePanel.manageProfile").start();
	  	}
	  });
	  
	  JButton manageComments = new JButton(Resources.getScaledIcon(Resources.iComments, 40));
	  manageComments.setPreferredSize(new Dimension(54,54));
	  manageComments.setToolTipText("Manage user annotation comments");
	  manageComments.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				manageComments();
	  			}
	  		}, "ProfilePanel.manageComments").start();
	  	}
	  });
	  
	  JButton checkShared = new JButton(Resources.getScaledIcon(Resources.iUserCheckShare, 40));
	  checkShared.setPreferredSize(new Dimension(54,54));
	  checkShared.setToolTipText("Check for shared elements");
	  checkShared.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				Highlander.getLoggedUser().checkForNewSharedElement(mainFrame);
	  			}
	  		}, "ProfilePanel.checkForNewSharedElement").start();
	  	}
	  });
	  
	  JButton createUserValueList = new JButton(Resources.getScaledIcon(Resources.iUserListNew, 40));
	  createUserValueList.setPreferredSize(new Dimension(54,54));
	  createUserValueList.setToolTipText("Create a list of values (like a gene list) in your profile");
	  createUserValueList.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				createValueList();
	  			}
	  		}, "ProfilePanel.createValueList").start();
	  	}
	  });
	    		
	  JButton createUserIntervalsList = new JButton(Resources.getScaledIcon(Resources.iUserIntervalsNew, 40));
	  createUserIntervalsList.setPreferredSize(new Dimension(54,54));
	  createUserIntervalsList.setToolTipText("Create a list of genomic intervals in your profile");
	  createUserIntervalsList.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				Set<Interval> set = new HashSet<>();
	  				try{
	  					set = Interval.fetchIntervals(mainFrame.getVariantTable().getSelectedVariantsId());
	  				}catch(Exception ex){
	  					ex.printStackTrace();
	  				}
	  				createIntervalsList(set);
	  			}
	  		}, "ProfilePanel.createIntervalsList").start();
	  	}
	  });
	  
	  JButton createUserPhenotypesList = new JButton(Resources.getScaledIcon(Resources.iUserHPONew, 40));
	  createUserPhenotypesList.setPreferredSize(new Dimension(54,54));
	  createUserPhenotypesList.setToolTipText("Create a list of HPO terms in your profile");
	  createUserPhenotypesList.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				createPhenotypesList();
	  			}
	  		}, "ProfilePanel.createPhenotypesList").start();
	  	}
	  });
	  
	  JButton createUserTemplateList = new JButton(Resources.getScaledIcon(Resources.iUserTemplateNew, 40));
	  createUserTemplateList.setPreferredSize(new Dimension(54,54));
	  createUserTemplateList.setToolTipText("Create a filters template in your profile");
	  createUserTemplateList.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){	  				
	  				createFiltersTemplate();
	  			}
	  		}, "ProfilePanel.createFiltersTemplate").start();
	  	}
	  });
	  
	  JButton userFiltersTemplate = new JButton(Resources.getScaledIcon(Resources.iTemplate, 40));
	  userFiltersTemplate.setPreferredSize(new Dimension(54,54));
	  userFiltersTemplate.setToolTipText("Create a set of filters using a template from your profile");
	  userFiltersTemplate.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){	  				
	  				useFiltersTemplate();
	  			}
	  		}, "ProfilePanel.useFiltersTemplate").start();
	  	}
	  });
	  	  
	  JButton resetColumnWidths = new JButton(Resources.getScaledIcon(Resources.iColumnSelection, 40));
	  resetColumnWidths.setPreferredSize(new Dimension(54,54));
	  resetColumnWidths.setToolTipText("Reset all column width to default values");
	  resetColumnWidths.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				resetColumnWidth(mainFrame);
	  			}
	  		}, "ProfilePanel.resetColumnWidth").start();
	  	}
	  });
	  
	  changeTableColor = new JButton();
	  setChangeColorButtonIcon(TargetColor.VARIANT_TABLE);
	  changeTableColor.setPreferredSize(new Dimension(54,54));
	  changeTableColor.setToolTipText("Change even rows background color of main table");
	  changeTableColor.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				changeTableColor(mainFrame, TargetColor.VARIANT_TABLE);
	  			}
	  		}, "ProfilePanel.changeTableColor").start();
	  	}
	  });
	  
	  changeSameVariantColor = new JButton();
	  setChangeColorButtonIcon(TargetColor.SAME_VARIANT);
	  changeSameVariantColor.setPreferredSize(new Dimension(54,54));
	  changeSameVariantColor.setToolTipText("Change rows background color of main table of selected unique variant");
	  changeSameVariantColor.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				changeTableColor(mainFrame, TargetColor.SAME_VARIANT);
	  			}
	  		}, "ProfilePanel.changeTableColor").start();
	  	}
	  });
	  
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);

	  panel.add(manageProfile);
	  panel.add(manageComments);
	  panel.add(checkShared);

	  addSeparator(panel);
	  
	  panel.add(createUserValueList);
	  panel.add(createUserIntervalsList);
	  panel.add(createUserPhenotypesList);
	  
	  addSeparator(panel);
	  
	  panel.add(createUserTemplateList);
	  panel.add(userFiltersTemplate);

	  addSeparator(panel);
	  
		panel.add(userEditUserProfile);
	  if (Highlander.getLoggedUser().isAdmin()) panel.add(userCreateUser);
	  if (Highlander.getLoggedUser().isAdmin()) panel.add(userDeleteUser);
	  if (Highlander.getLoggedUser().isAdmin()) panel.add(userResetPassword);
	  if (Highlander.getLoggedUser().isAdmin()) panel.add(userPromoteUser);

	  addSeparator(panel);
	  
	  panel.add(changeTableColor);
	  panel.add(changeSameVariantColor);
	  panel.add(resetColumnWidths);
	  
	  add(scrollablePanel, BorderLayout.CENTER);
	}
	
	private void addSeparator(JPanel parent){
	  JPanel panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel.setPreferredSize(new Dimension(2, 50));
		parent.add(panel);		
	}
	
	public void manageProfile(){
		ProfileTree.showProfileDialog(mainFrame, mainFrame);
	}
	
	public void manageComments(){
		CommentsManager cm = new CommentsManager();
		Tools.centerWindow(cm, false);
		cm.setVisible(true);
	}
	
	public static void createValueList(){
		AskListOfFreeValuesDialog ask = new AskListOfFreeValuesDialog();
		Tools.centerWindow(ask, false);
		ask.setVisible(true);
		if (!ask.getSelection().isEmpty()) ask.saveList();
	}
	
	public static void createIntervalsList(Set<Interval> intervals){
		Reference reference = null;
		if (intervals.isEmpty()) {
		reference = (Reference)JOptionPane.showInputDialog(new JFrame(), "Select a reference genome", "Create list of genomic intervals", 
				JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iReference, 64), 
				Reference.getAvailableReferences().toArray(new Reference[0]), 
				Highlander.getCurrentAnalysis().getReference());
		}
		if (!intervals.isEmpty() || reference != null) {
			AskListOfIntervalsDialog ask = (intervals.isEmpty()) ? new AskListOfIntervalsDialog(reference) : new AskListOfIntervalsDialog(Highlander.getCurrentAnalysis().getReference(), intervals);
			Tools.centerWindow(ask, false);
			ask.setVisible(true);
			if (!ask.getSelection().isEmpty()) ask.saveList();
		}
	}
	
	public static void createPhenotypesList(){
		Reference reference = null;
		reference = (Reference)JOptionPane.showInputDialog(new JFrame(), "Select a reference genome", "Create list of HPO terms", 
				JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iReference, 64), 
				Reference.getAvailableReferences().toArray(new Reference[0]), 
				Highlander.getCurrentAnalysis().getReference());
		if (reference != null) {
			AskListOfHPOTermDialog ask = new AskListOfHPOTermDialog(reference);
			Tools.centerWindow(ask, false);
			ask.setVisible(true);
			if (!ask.getSelection().isEmpty()) ask.saveList();
		}
	}
	
	public static void createFiltersTemplate(){
		CreateTemplate ask = new CreateTemplate(Highlander.getCurrentAnalysis());
		Tools.centerWindow(ask, false);
		ask.setVisible(true);
		if (ask.needToSave()) ask.saveTemplate();
	}
	
	public void useFiltersTemplate(){
		String name = ProfileTree.showProfileDialog(mainFrame, Action.LOAD, UserData.FILTERS_TEMPLATE, Highlander.getCurrentAnalysis().toString());
		if (name != null){
			try {
				UseTemplate ask = new UseTemplate(Highlander.getLoggedUser().loadFiltersTemplate(Highlander.getCurrentAnalysis(), name));
				Tools.centerWindow(ask, false);
				ask.setVisible(true);
			} catch (Exception ex) {
				Tools.exception(ex);
				JOptionPane.showMessageDialog(mainFrame, Tools.getMessage("Error", ex), "Load filters template from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}
	
	public void changeTableColor(Component parentComponent, TargetColor target){
		try{
			Palette color = mainFrame.getVariantTable().getColor(target);
			Object resu = JOptionPane.showInputDialog(parentComponent, "Select a color", "Table even rows color", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iHighlighting, 64), Palette.values(), color);
			if (resu != null){
				color = (Palette)resu;
				Highlander.getLoggedUser().saveSettings(Settings.COLOR, target.toString(), color.toString());
				mainFrame.getVariantTable().setColor(target, color);
				setChangeColorButtonIcon(target);
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(parentComponent, Tools.getMessage("Cannot share selected user list", ex), "Sharing element from profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
		}
	}

	public void setChangeColorButtonIcon(TargetColor target) {
		switch(target) {
		case VARIANT_TABLE:
			changeTableColor.setIcon(Resources.getScaledIcon(Resources.getColoredSquare(40, Resources.getColor(mainFrame.getVariantTable().getColor(target), 200, false)), 40));
			break;
		case SAME_VARIANT:
			changeSameVariantColor.setIcon(Resources.getScaledIcon(Resources.getColoredSquare(40, Resources.getColor(mainFrame.getVariantTable().getColor(target), 200, false)), 40));
			break;
		}
		
	}
	
	public static void resetColumnWidth(Component parentComponent){
		try{
			int answer = JOptionPane.showOptionDialog(parentComponent, "Are you SURE you want to reset all column widths to default values ?", "Reset column widths", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iColumnSelection, 64), null, null);
			if (answer == JOptionPane.YES_OPTION){
				for(String key : Highlander.getLoggedUser().loadSettings(Settings.WIDTH).keySet()){
					Highlander.getLoggedUser().deleteData(UserData.SETTINGS, key);
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(parentComponent, Tools.getMessage("Cannot share selected user list", ex), "Sharing element from profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
		}
	}
}
