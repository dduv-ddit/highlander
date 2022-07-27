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

package be.uclouvain.ngs.highlander.administration.users;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JDialog;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.users.User.Rights;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JRadioButton;
import java.awt.FlowLayout;
import javax.swing.ButtonGroup;

public class UserDataDialog extends JDialog {
	public enum UserDataDialogType {EDIT_USER, EDIT_OTHER_USER, CREATE_USER}

	private final UserDataDialogType type;
	private User user;
	private final String title;
	private final String validateButton;
	private final ImageIcon icon;
	private final String firstName;
	private final String lastName;
	private final String email;
	private final String group;
	private final Rights rights;
	private final String error;

	private JPasswordField fieldCurrentPassword;
	private JPasswordField fieldNewPassword;
	private JPasswordField fieldRetypedPassword;
	private JComboBox<String> boxGroup;
	private JTextField fieldEmail;
	private JTextField fieldLastName;
	private JTextField fieldFirstName;
	private JTextField fieldUsername;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JRadioButton rdbtnUser;
	private JRadioButton rdbtnAdministrator;

	public UserDataDialog(final Frame frame, UserDataDialogType dialogType) {
		this(frame, dialogType, null);
	}
	
	public UserDataDialog(final Frame frame, UserDataDialogType dialogType, User userToEdit) {
		super(frame, "", true);
		type = dialogType;
		switch(type){
		case CREATE_USER:
			user = null; 
			title = "Create new user";
			validateButton = "Create user";
			icon = Resources.iUserAdd;
			firstName = "";
			lastName = "";
			email = "";
			group = "";
			rights = Rights.user;
			error = "Error: cannot create new user on the database.";
			break;
		case EDIT_OTHER_USER:
			user = userToEdit;
			title = "Modify user";
			validateButton = "Save changes";
			icon = Resources.iUserEdit;
			firstName = userToEdit.getFirstName();
			lastName = userToEdit.getLastName();
			email = userToEdit.getEmail();
			group = userToEdit.getGroup();
			rights = userToEdit.getRights();
			error = "Error: cannot update user '"+userToEdit+"' profile on the database.";
			break;
		case EDIT_USER:
		default:
			user = Highlander.getLoggedUser();
			title = "User profile";
			validateButton = "Save changes";
			icon = Resources.iUserEdit;
			firstName = Highlander.getLoggedUser().getFirstName();
			lastName = Highlander.getLoggedUser().getLastName();
			email = Highlander.getLoggedUser().getEmail();
			group = Highlander.getLoggedUser().getGroup();
			rights = Highlander.getLoggedUser().getRights();
			error = "Error: cannot update your profile on the database.";
			break;
		}
		setTitle(title);
		frame.setIconImage(Resources.getScaledIcon(icon, 32).getImage());
		getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnNewButton = new JButton(validateButton, Resources.getScaledIcon(Resources.iButtonApply, 16));
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					String password = validateInput();
					String encryptedPassword = Tools.md5Encryption(password);
					try {
						if (type == UserDataDialogType.CREATE_USER){
							user = new User(fieldUsername.getText());
						}
						if (type != UserDataDialogType.EDIT_OTHER_USER){
							user.setPassword(encryptedPassword);
							if (rdbtnAdministrator.isSelected()){
								user.setRights(Rights.administrator);
							}else{
								user.setRights(Rights.user);
							}
						}
						user.setFirstName(fieldFirstName.getText());
						user.setLastName(fieldLastName.getText());
						user.setEmail(fieldEmail.getText());
						user.setGroup(boxGroup.getSelectedItem().toString());
						switch (type){
						case CREATE_USER:
							user.insert();
							StringBuilder sb = new StringBuilder();
							sb.append("Dear "+user.getFirstName() + " " + user.getLastName() + ",\n\n");
							sb.append("Your account on Highlander has been created.\n");
							sb.append("Your login: "+user.getUsername() + "\n");
							sb.append("Your password: "+password + "\n");
							sb.append("You can change it in the profile tab within Highlander.\n");
							sb.append("\n");
							sb.append("Installation:\n");
							sb.append("Download the Highlander client software available at http://sites.uclouvain.be/highlander/download.html (there are 3 versions : Mac OS X, Windows and Linux) and install it on your computer. \n");
							sb.append("See https://sites.uclouvain.be/highlander/install.html for installation instructions and troubleshooting.\n");
							sb.append("The software needs Java 8 or above installed (64 bits version). Java could already be installed on your computer, otherwise it can be downloaded from Oracle website (you need to create a free account) or from https://www.java.com/ (download the latest JRE for you system). On MacOs X, you need version 10.7.3 (Lion) or above. On Windows, you need Windows 7 or above.\n");
							sb.append("\n");
							sb.append("If you found any bugs, errors, strange behaviour, just report it to Administrator (if you have an error message, please use the 'Send to Administrator' button below it, or copy/paste it).\n");
							sb.append("\n");
							sb.append("The software has an auto-update function, so when bugs are corrected, the software will propose you to install the last version when it's available.\n");
							sb.append("\n");
							Tools.sendMail(user.getEmail(), "Account created for Highlander", sb.toString());
							break;
						case EDIT_USER:
						case EDIT_OTHER_USER:
						default:
							user.update();
							break;
						}
						dispose();
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(frame, error, "Saving user profile", JOptionPane.ERROR_MESSAGE);
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(frame, ex.getMessage(), "Saving user profile", JOptionPane.ERROR_MESSAGE);
				}

			}
		});
		panel.add(btnNewButton);

		JButton btnCancel = new JButton("Cancel", Resources.getScaledIcon(Resources.iCross, 16));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				user = null;
				dispose();
			}
		});
		panel.add(btnCancel);

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new GridBagLayout());

		JLabel lblBla = new JLabel(Resources.getScaledIcon(icon, 80));
		panel_1.add(lblBla, 
				new GridBagConstraints(0, 0, 1, (type == UserDataDialogType.EDIT_USER)?8:5, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(25, 15, 25, 5), 0, 0));

		JLabel lblUsername = new JLabel("Username");
		if(type == UserDataDialogType.CREATE_USER) panel_1.add(lblUsername, 
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		fieldUsername = new JTextField();
		if(type == UserDataDialogType.CREATE_USER) panel_1.add(fieldUsername, 
				new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));
		fieldUsername.setColumns(20);

		JLabel lblFirstName = new JLabel("First name");
		panel_1.add(lblFirstName, 
				new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		fieldFirstName = new JTextField(firstName);
		panel_1.add(fieldFirstName, 
				new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));
		fieldFirstName.setColumns(20);

		JLabel lblLastName = new JLabel("Last name");
		panel_1.add(lblLastName, 
				new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		fieldLastName = new JTextField(lastName);
		panel_1.add(fieldLastName, 
				new GridBagConstraints(2, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));
		fieldLastName.setColumns(20);

		JLabel lblGroup = new JLabel("Group");
		panel_1.add(lblGroup, 
				new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

		JPanel panel_5 = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
		panel_1.add(panel_5, 
				new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));

		boxGroup = new JComboBox<String>(User.getExistingGroups());
		boxGroup.setPrototypeDisplayValue("AZERTYUIOPMLKJHGFDSQWXCVBN");
		boxGroup.setSelectedItem(group);
		panel_5.add(boxGroup);
		
		JButton buttonAddGroup = new JButton(Resources.getScaledIcon(Resources.i3dPlus, 18));
		buttonAddGroup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(UserDataDialog.this, "Enter the name of the new group", "Add group", JOptionPane.QUESTION_MESSAGE);
				if (res != null) {
					boxGroup.addItem(res.toString());
					boxGroup.setSelectedItem(res.toString());
				}
			}
		});
		if (Highlander.getLoggedUser().isAdmin()) {
			buttonAddGroup.setToolTipText("Add new group to the list");
		}else {
			buttonAddGroup.setToolTipText("Only administrator can add new groups");
			buttonAddGroup.setEnabled(false);
		}
		panel_5.add(buttonAddGroup);
		
		JLabel lblEmail = new JLabel("Email");
		panel_1.add(lblEmail, 
				new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		fieldEmail = new JTextField(email);
		panel_1.add(fieldEmail, 
				new GridBagConstraints(2, 4, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));
		fieldEmail.setColumns(20);
		
		if (type == UserDataDialogType.CREATE_USER || type == UserDataDialogType.EDIT_USER){
			JLabel lblRights = new JLabel("Rights");
			panel_1.add(lblRights, 
					new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			JPanel panel_2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
			panel_1.add(panel_2, 
					new GridBagConstraints(2, 5, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));

			rdbtnUser = new JRadioButton("user");
			buttonGroup.add(rdbtnUser);
			if (rights == Rights.user) rdbtnUser.setSelected(true);
			panel_2.add(rdbtnUser);

			rdbtnAdministrator = new JRadioButton("administrator");
			buttonGroup.add(rdbtnAdministrator);
			if (rights == Rights.administrator) rdbtnAdministrator.setSelected(true);
			panel_2.add(rdbtnAdministrator);
			rdbtnAdministrator.setEnabled(Highlander.getLoggedUser().isAdmin());
		}
		
		if (type == UserDataDialogType.EDIT_USER){
			JLabel lblCurrentPassword = new JLabel("Current password");
			panel_1.add(lblCurrentPassword, 
					new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			fieldCurrentPassword = new JPasswordField();
			panel_1.add(fieldCurrentPassword, 
					new GridBagConstraints(2, 6, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));

			JLabel lblPassword = new JLabel("New password");
			panel_1.add(lblPassword, 
					new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			fieldNewPassword = new JPasswordField();
			panel_1.add(fieldNewPassword, 
					new GridBagConstraints(2, 7, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));

			JLabel lblRetypePassword = new JLabel("Re-type new password");
			panel_1.add(lblRetypePassword, 
					new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

			fieldRetypedPassword = new JPasswordField();
			panel_1.add(fieldRetypedPassword, 
					new GridBagConstraints(2, 8, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 20), 0, 0));
		}
		
		panel_1.add(new JPanel(), 
				new GridBagConstraints(0, (type == UserDataDialogType.EDIT_USER)?8:5, 3, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 5, 5, 20), 0, 0));

		pack();
	}

	/**
	 * Validates user profile and returns the MD5 encrypted password 
	 * @return
	 * @throws Exception
	 */
	private String validateInput() throws Exception {
		String currentpassword;
		String newpassword;
		String retypedpassword;
		if (type == UserDataDialogType.EDIT_USER){
			currentpassword = new String(fieldCurrentPassword.getPassword());
			newpassword = new String(fieldNewPassword.getPassword());
			retypedpassword = new String(fieldRetypedPassword.getPassword());
		}else {
			currentpassword = "";
			newpassword = User.generatePassword(8);
			retypedpassword = newpassword;
		}
		if (type == UserDataDialogType.EDIT_USER){
			if (currentpassword.length() == 0) {
				throw new Exception("Please enter your current password to prove your identity (leave new password field blank if you don't want to change it).");
			}else {
				if (!user.getPassword().equals(Tools.md5Encryption(currentpassword))) {
					fieldCurrentPassword.setText("");
					throw new Exception("Error: your current password is wrong.");
				}
				if (newpassword.length() == 0) {
					newpassword = currentpassword;					
				}else {
					if (newpassword.length() < 4){
						fieldNewPassword.setText("");
						fieldRetypedPassword.setText("");
						throw new Exception("Error: new password must be at least 4 character long.");
					}
					if (!newpassword.equals(retypedpassword)){
						fieldNewPassword.setText("");
						fieldRetypedPassword.setText("");
						throw new Exception("Error: re-typed password is different, please re-enter both.");
					}
				}
			}
		}
		if (type == UserDataDialogType.CREATE_USER){
			if (fieldUsername.getText().length() < 4)
				throw new Exception("Error: username must be at least 4 character long.");
			if (User.doesUserExist(fieldUsername.getText()))
				throw new Exception("Error: username '"+fieldUsername.getText()+"' already exists.");
			if (fieldUsername.getText().equalsIgnoreCase("default"))
				throw new Exception("Error: username cannot be 'default'.");
		}
		if (fieldFirstName.getText().length() == 0)
			throw new Exception("Error: you must give your first name.");
		if (fieldLastName.getText().length() == 0)
			throw new Exception("Error: you must give your last name.");
		if (fieldEmail.getText().length() == 0 || fieldEmail.getText().indexOf("@") == -1)
			throw new Exception("Error: you must give a valid email address.");
		if (type != UserDataDialogType.EDIT_OTHER_USER && user != null && !rdbtnAdministrator.isSelected() && !user.isAnotherUserAdmin())
			throw new Exception("Error: at least one user must have the administrative rights.");
		if (newpassword.length() < 4){
			throw new Exception("Error: password must be at least 4 character long.");
		}else{
			return newpassword;
		}

	}

	public User getUser() {
		return user;
	}

}
