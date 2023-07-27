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

import java.awt.*;
import javax.swing.*;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Parameters.PasswordPolicy;

import java.awt.event.*;
import java.security.NoSuchAlgorithmException;

public class LoginBox extends JDialog {
	public boolean OKCancel = false;

	JPanel mainPanel = new JPanel();
	JPanel jPanel1 = new JPanel();
	JPanel jPanel2 = new JPanel();
	JButton OKButton = new JButton();
	JButton cancelButton = new JButton();
	JButton resetButton = new JButton();
	JPanel jPanel4 = new JPanel();
	JLabel loginLabel = new JLabel();
	JLabel passwordLabel = new JLabel();
	JLabel proxyPasswordLabel = new JLabel();
	JTextField loginTextField = new JTextField();
	JPasswordField PasswordField = new JPasswordField();
	JPasswordField proxyPasswordField = new JPasswordField();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	GridBagLayout gridBagLayout3 = new GridBagLayout();
	private final JLabel label = new JLabel(Resources.getScaledIcon(Resources.iUser, 64));

	public LoginBox(Frame frame, String title, boolean modal) {
		super(frame, title, modal);
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			Tools.exception(ex);
		}
	}

	public LoginBox() {
		this(null, "", false);
	}

	private void jbInit() throws Exception {
		mainPanel.setLayout(gridBagLayout3);
		OKButton.setText("Login");
		OKButton.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 16));
		OKButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				OKButton_actionPerformed(e);
			}
		});
		cancelButton.setText("Exit");
		cancelButton.setIcon(Resources.getScaledIcon(Resources.iExit, 16));
		cancelButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelButton_actionPerformed(e);
			}
		});
		resetButton.setText("Reset my password");
		resetButton.setIcon(Resources.getScaledIcon(Resources.iUserLock, 16));
		resetButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				resetButton_actionPerformed(e);
			}
		});
		jPanel4.setLayout(gridBagLayout2);
		loginTextField.setColumns(15);
		loginTextField.setText("");
		loginTextField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				textFieldKeyPressed(e);
			}
		});
		PasswordField.setText("");
		PasswordField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				textFieldKeyPressed(e);
			}
		});
		proxyPasswordField.setText("");
		proxyPasswordField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				textFieldKeyPressed(e);
			}
		});
		getContentPane().add(mainPanel);
		mainPanel.add(jPanel1,  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 71, 0));

		jPanel1.add(label);
		jPanel1.add(jPanel4, null);
		GridBagConstraints gbc_loginLabel = new GridBagConstraints();
		gbc_loginLabel.fill = GridBagConstraints.VERTICAL;
		gbc_loginLabel.anchor = GridBagConstraints.EAST;
		gbc_loginLabel.insets = new Insets(0, 5, 5, 5);
		gbc_loginLabel.gridx = 0;
		gbc_loginLabel.gridy = 0;
		jPanel4.add(loginLabel, gbc_loginLabel);
		loginLabel.setText("Login");
		jPanel4.add(loginTextField,   new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		GridBagConstraints gbc_passwordLabel = new GridBagConstraints();
		gbc_passwordLabel.fill = GridBagConstraints.VERTICAL;
		gbc_passwordLabel.anchor = GridBagConstraints.EAST;
		gbc_passwordLabel.insets = new Insets(0, 5, 0, 5);
		gbc_passwordLabel.gridx = 0;
		gbc_passwordLabel.gridy = 1;
		jPanel4.add(passwordLabel, gbc_passwordLabel);
		passwordLabel.setText("Password");
		jPanel4.add(PasswordField,   new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));    
		proxyPasswordLabel.setText("Proxy password");
		if (Highlander.getParameters().getHttpProxyPasswordPolicy() == PasswordPolicy.ask_at_login) {
			jPanel4.add(proxyPasswordLabel,   new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
					,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
			jPanel4.add(proxyPasswordField,   new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
					,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
		}
		mainPanel.add(jPanel2,    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 151, 0));
		jPanel2.add(OKButton, null);
		jPanel2.add(cancelButton, null);
		jPanel2.add(resetButton, null);
	}

	void textFieldKeyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			OKCancel = true;
			dispose();
		}
	}

	void OKButton_actionPerformed(ActionEvent e) {
		OKCancel = true;
		dispose();
	}

	void cancelButton_actionPerformed(ActionEvent e) {
		OKCancel = false;
		loginTextField.setText("");
		PasswordField.setText("");
		dispose();
	}

	void resetButton_actionPerformed(ActionEvent e) {
		try {
			User[] users = User.fetchList().toArray((new User[0]));
			User user = (User)JOptionPane.showInputDialog(this, "Who are you ?", "Reset password", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserLock, 64), users, null);
			if (user != null){
				Object res = JOptionPane.showInputDialog(this, "What is your email address ?", "Reset password", JOptionPane.QUESTION_MESSAGE);
				if (res != null) {
					String email = res.toString();
					if (user.getEmail().equals(email)) {
						User.resetPassword(this, user);
						JOptionPane.showMessageDialog(this, "An email has been sent to " + email + " with a new password.", "Reset password", JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iUserLock,64));
					}else {
						JOptionPane.showMessageDialog(this, "Wrong email for " +user+" ("+user.getUsername()+")", "Reset password", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		}catch(Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), "Cannot fetch the user list: " + ex.getMessage(), "Reset password", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}
	
	public String getUsername(){
		return loginTextField.getText();
	}

	public String getEncryptedPassword() throws NoSuchAlgorithmException {
		return Tools.md5Encryption(new String(PasswordField.getPassword()));
	}

	public void setProxyPasswordIfNecessary() {
		if (Highlander.getParameters().getHttpProxyPasswordPolicy() == PasswordPolicy.same_as_highlander) {
			Highlander.getParameters().setHttpProxyPassword(new String(PasswordField.getPassword()));
			Highlander.getParameters().setProxyLogin();
		}else if (Highlander.getParameters().getHttpProxyPasswordPolicy() == PasswordPolicy.ask_at_login) {
			Highlander.getParameters().setHttpProxyPassword(new String(proxyPasswordField.getPassword()));
			Highlander.getParameters().setProxyLogin();  		
		}
	}
}

