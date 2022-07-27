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

package be.uclouvain.ngs.highlander.administration.UI.users;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.AdministrationTableModel;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.UserDataDialog;
import be.uclouvain.ngs.highlander.administration.users.User.Rights;
import be.uclouvain.ngs.highlander.administration.users.UserDataDialog.UserDataDialogType;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public class UserManagementPanel extends ManagerPanel {
	
	private AdministrationTableModel usersTableModel;
	private JTable usersTable;

	public UserManagementPanel(ProjectManager manager){
		super(manager);
		usersTable = new JTable(usersTableModel){
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						if (index >= 0){
							int realIndex = columnModel.getColumn(index).getModelIndex();
							return (table.getModel()).getColumnName(realIndex);
						}else{
							return null;
						}
					}
				};
			}
		};
		usersTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		usersTable.setCellSelectionEnabled(false);
		usersTable.setRowSelectionAllowed(true);
		usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroll = new JScrollPane(usersTable);
		add(scroll, BorderLayout.CENTER);

		fill();

		JPanel users = new JPanel(new WrapLayout(WrapLayout.LEADING));

		JButton userCreateUser = new JButton("Create new user", Resources.getScaledIcon(Resources.iUserAdd, 24));
		userCreateUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						User newUser = User.createUser();
						if (newUser != null) {
							manager.getUserList().add(newUser);
							fill();
						}
					}
				}, "UserManagementPanel.createUser").start();
			}
		});

		JButton userDeleteUser = new JButton("Delete selected user", Resources.getScaledIcon(Resources.iUserDelete, 24));
		userDeleteUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						User user = getSelectedUser();
						if (user != null) {
							if (user.isAnotherUserAdmin()){
								int answer = JOptionPane.showOptionDialog(manager, "Are you SURE you want to delete this user:\n"+user+" ("+user.getUsername()+")", "Delete user", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserDelete, 64), null, null);
								if (answer == JOptionPane.YES_OPTION){
									try {
										user.delete();
										manager.getUserList().remove(user);
										fill();
									} catch (Exception ex) {
										Tools.exception(ex);
										JOptionPane.showMessageDialog(manager, "Cannot delete '"+user+"': " + ex.getMessage(), "Delete user", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
									}
								}
							}else{
								JOptionPane.showMessageDialog(manager, "Error: at least one user must have the administrative rights.", "Delete user",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
							}
						}
					}
				}, "UserManagementPanel.deleteUser").start();
			}
		});

		JButton userPromoteUser = new JButton("Change rights of selected user", Resources.getScaledIcon(Resources.iUserPromote, 24));
		userPromoteUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						User user = getSelectedUser();
						if (user != null) {
							Object res = JOptionPane.showInputDialog(manager, "Select the new rights for "+user+" ("+user.getUsername()+")", "Change rights of user", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserPromote,64), Rights.values(), user.getRights());
							if (res != null) {
								if (!user.isAdmin() || user.isAnotherUserAdmin()) {
									try {
										user.setRights((Rights)res);
										user.update();
										fill();
									} catch (Exception ex) {
										Tools.exception(ex);
										JOptionPane.showMessageDialog(manager, "Cannot change rights for '"+user+"': " + ex.getMessage(), "Change rights of user", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
									}
								}else {
									JOptionPane.showMessageDialog(manager, "Error: at least one user must have the administrative rights.", "Change rights of user",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
								}
							}
						}
					}
				}, "UserManagementPanel.promoteUser").start();
			}
		});

		JButton userResetPassword = new JButton("Reset password of selected user", Resources.getScaledIcon(Resources.iUserLock, 24));
		userResetPassword.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						User user = getSelectedUser();
						if (user != null) {
							User.resetPassword(manager, user);
						}
					}
				}, "UserManagementPanel.resetPassword").start();
			}
		});

		JButton userModify = new JButton("Modify selected user", Resources.getScaledIcon(Resources.iUserEdit, 24));
		userModify.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						User user = getSelectedUser();
						if (user != null) {
							UserDataDialog dialog = new UserDataDialog(new JFrame(), UserDataDialogType.EDIT_OTHER_USER, user);
							Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
							Dimension windowSize = dialog.getSize();
							dialog.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
									Math.max(0, (screenSize.height - windowSize.height) / 2));
							dialog.setVisible(true);
							fill();
						}
					}
				}, "UserManagementPanel.editUser").start();
			}
		});
		

		users.add(userCreateUser);
		users.add(userPromoteUser);
		users.add(userDeleteUser);
		users.add(userResetPassword);
		users.add(userModify);

		add(users, BorderLayout.NORTH);
	}

	private User getSelectedUser() {
		int row = usersTable.getSelectedRow();
		if (row > -1) {
			String username = usersTable.getValueAt(row, 0).toString();
			try {
				for (User user : User.fetchList()) {
					if (user.getUsername().equals(username)) {
						return user;
					}
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	private void fill(){
		try{
			String[] headers = new String[] {
					"Username",
					"First name",
					"Last name",
					"Group",
					"Email",
					"Rights",
			};
			Object[][] data;
			List<Object[]> arrayList = new ArrayList<Object[]>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM `users` ORDER BY `rights`,`username`")) {
				while(res.next()){
					Object[] array = new Object[headers.length];
					array[0] = res.getObject("username");
					array[1] = res.getObject("first_name");
					array[2] = res.getObject("last_name");
					array[3] = res.getObject("group");
					array[4] = res.getObject("email");
					array[5] = res.getObject("rights");
					arrayList.add(array);
				}
			}			
			data = new Object[arrayList.size()][headers.length];
			int row = 0;
			for (Object[] array : arrayList){
				data[row] = array;
				row++;
			}
			usersTableModel = new AdministrationTableModel(data, headers);
			usersTable.setModel(usersTableModel);
			refresh();
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	private void refresh(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					usersTableModel.fireTableRowsUpdated(0,usersTableModel.getRowCount()-1);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		});	
	}
}
