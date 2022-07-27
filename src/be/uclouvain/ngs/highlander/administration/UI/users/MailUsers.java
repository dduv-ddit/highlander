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
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public class MailUsers extends ManagerPanel {

	private List<File> attachments = new ArrayList<File>();
	JTextField txtf_subject;
	JTextArea txta_message;
	
	public MailUsers(ProjectManager manager){
		super(manager);
		
		JPanel panel_center = new JPanel(new GridBagLayout());
		add(panel_center, BorderLayout.CENTER);
		
		JLabel label_subject = new JLabel("Subject");
		txtf_subject = new JTextField();
		JLabel label_attachments = new JLabel("Attachments");
		final JPanel panel_attachments = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JButton button_attachments = new JButton(Resources.getScaledIcon(Resources.i3dPlus, 16));
		button_attachments.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FileDialog d = new FileDialog(manager, "Select a file", FileDialog.LOAD);
				Tools.centerWindow(d, false);
				d.setVisible(true);
				if (d.getFile() != null){
					String filename = d.getDirectory() + d.getFile();
					File file = new File(filename);
					attachments.add(file);
					JButton newAttachment = new JButton(d.getFile(), Resources.getScaledIcon(Resources.i3dMinus, 16));
					newAttachment.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							attachments.remove(file);
							panel_attachments.remove(newAttachment);
							validate();
						}
					});
					panel_attachments.add(newAttachment);
					validate();
				}
			}
		});
		JLabel label_message = new JLabel("Message");
		txta_message = new JTextArea();
		txta_message.setWrapStyleWord(true);
		JScrollPane scroll = new JScrollPane(txta_message);
		
		int row=0;
		panel_center.add(label_subject, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		panel_center.add(txtf_subject, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		row++;
		panel_center.add(label_attachments, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		panel_center.add(button_attachments, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		panel_center.add(panel_attachments, new GridBagConstraints(2, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		row++;
		panel_center.add(label_message, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(15, 5, 5, 5), 0, 0));
		panel_center.add(scroll, new GridBagConstraints(1, row, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		row++;
		
		JPanel panel_south = new JPanel();
		add(panel_south, BorderLayout.SOUTH);
		
		JButton createNewButton = new JButton("Send mail to ALL active Highlander users", Resources.getScaledIcon(Resources.iAlignmentFrameShiftOff, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});
						try {
							sendMail();
							JOptionPane.showMessageDialog(manager, "Mail has been sent to all active users", "Mail users", JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iButtonApply, 128));
							txtf_subject.setText("");
							txta_message.setText("");
							panel_attachments.removeAll();
							validate();
							attachments.clear();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
							JOptionPane.showMessageDialog(manager, "Problem when sending email", "Mail users", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
						}
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.stop();
							}
						});
					}
				}, "MailUsers.send").start();

			}
		});
		panel_south.add(createNewButton);

	}
	
	private void sendMail() throws Exception {
		String subject = txtf_subject.getText();
		String text = txta_message.getText();
		
		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", Highlander.getParameters().getSmtp());
		/* 
		 * Not sure when to use proxy with emails
		 * It's cleary not necessary on the cluster, preventing mails to be sent
		 * But I think it was necessary before, when ddgw just transfer mail to the UCL proxy
		 * 
    if (System.getProperty("http.proxyHost") != null) {
    	props.setProperty("http.proxySet","true");
    	props.setProperty("http.proxyHost",System.getProperty("http.proxyHost"));
    	props.setProperty("http.proxyPort",System.getProperty("http.proxyPort"));
    	props.setProperty("mail.smtp.socks.host",System.getProperty("http.proxyHost"));
    	props.setProperty("mail.smtp.socks.port",System.getProperty("http.proxyPort"));
    	//What to do with proxy authentication ?
    	//props.setProperty("http.proxyUser",System.getProperty("http.proxyUser"));
    	//props.setProperty("http.proxyPassword",System.getProperty("http.proxyPassword"));
  	}
		 */

		Session mailSession = Session.getDefaultInstance(props, null);
		Transport transport = mailSession.getTransport();

		MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress(Highlander.getParameters().getAdminMail(), "Highlander"));
		message.setSubject(subject);
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `email` FROM `users` WHERE `rights` != 'inactive'")) {
			if (res.next()){
				String recipient = res.getString("email");
				message.addRecipient(Message.RecipientType.BCC,	new InternetAddress(recipient));
			}
		}

		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText(text);

		MimeMultipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);

		for (File file : attachments){
			messageBodyPart = new MimeBodyPart();
			DataSource source = new FileDataSource(file);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(file.getName());
			multipart.addBodyPart(messageBodyPart);
		}

		message.setContent(multipart); 

		transport.connect();
		transport.sendMessage(message,
				message.getRecipients(Message.RecipientType.BCC));
		transport.close();
	}
}
