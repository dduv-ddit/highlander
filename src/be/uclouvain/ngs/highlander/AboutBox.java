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

package be.uclouvain.ngs.highlander ;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;


public class AboutBox extends JDialog implements ActionListener {

	private JPanel panel;
	JPanel panel1 = new JPanel();
	JPanel insetsPanel1 = new JPanel();
	JPanel insetsPanel3 = new JPanel();
	JButton buttonLicence = new JButton();
	JButton buttonCopying = new JButton();
	JButton buttonOK = new JButton();
	JLabel imageLabel = new JLabel();
	JLabel label1 = new JLabel();
	JLabel label3 = new JLabel();
	JLabel label4 = new JLabel();
	JLabel label5 = new JLabel();
	JLabel label6 = new JLabel();
	JLabel label7 = new JLabel();
	BorderLayout borderLayout1 = new BorderLayout();
	String product = "Highlander " + Highlander.version;
	String comments1 = "Raphael Helaers";
	String comments2 = "Laboratory of Human Molecular Genetics";
	String comments3 = "de Duve Institute";
	String comments4 = "Université catholique de Louvain (Belgium)";

	public AboutBox(Frame parent) {
		super(parent);
		setIconImage(Resources.getScaledIcon(Resources.iAbout, 16).getImage());
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
			this.pack();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	AboutBox() {
		this(null);
	}

	//Component initialization
	private void jbInit() throws Exception  {
		GridBagConstraints gridBagConstraints8 = new GridBagConstraints(0, 0, 1, 5, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(30, 0, 5, 30), 0, 0);
		gridBagConstraints8.gridx = 0;
		gridBagConstraints8.weighty = 0;
		gridBagConstraints8.weightx = 0;
		gridBagConstraints8.gridy = 0;
		GridBagConstraints gridBagConstraints61 = new GridBagConstraints();
		gridBagConstraints61.insets = new Insets(5, 5, 5, 5);
		gridBagConstraints61.gridx = 1;
		gridBagConstraints61.gridy = 4;
		gridBagConstraints61.weightx = 0;
		gridBagConstraints61.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints61.gridheight = 1;
		GridBagConstraints gridBagConstraints51 = new GridBagConstraints();
		gridBagConstraints51.insets = new Insets(5, 5, 5, 5);
		gridBagConstraints51.gridx = 1;
		gridBagConstraints51.gridy = 3;
		gridBagConstraints51.weightx = 0;
		gridBagConstraints51.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints51.gridheight = 1;
		GridBagConstraints gridBagConstraints41 = new GridBagConstraints();
		gridBagConstraints41.insets = new Insets(15, 5, 5, 5);
		gridBagConstraints41.gridx = 1;
		gridBagConstraints41.gridy = 2;
		gridBagConstraints41.weightx = 0;
		gridBagConstraints41.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints41.gridheight = 1;
		GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
		gridBagConstraints31.insets = new Insets(15, 5, 5, 5);
		gridBagConstraints31.gridx = 1;
		gridBagConstraints31.gridy = 1;
		gridBagConstraints31.weightx = 0;
		gridBagConstraints31.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints31.gridheight = 1;
		GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
		gridBagConstraints21.gridwidth = 2;
		gridBagConstraints21.insets = new Insets(5, 5, 5, 5);
		gridBagConstraints21.gridx = 0;
		gridBagConstraints21.gridy = 5;
		gridBagConstraints21.weightx = 0;
		gridBagConstraints21.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints21.gridheight = 1;
		GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
		gridBagConstraints11.insets = new Insets(25, 5, 10, 5);
		gridBagConstraints11.gridx = 1;
		gridBagConstraints11.gridy = 0;
		gridBagConstraints11.fill = GridBagConstraints.NONE;
		gridBagConstraints11.weightx = 0.0;
		gridBagConstraints11.anchor = GridBagConstraints.NORTHWEST;
		gridBagConstraints11.gridheight = 1;
		imageLabel.setIcon(Resources.getScaledIcon(Resources.iHighlander, 128));
		this.setTitle("About");
		label1.setText(product);
		label4.setText(comments1);
		label5.setText(comments2);
		label6.setText(comments3);
		label7.setText(comments4);
		buttonLicence.setText("LICENSE");
		buttonLicence.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showFileAndClose("LICENSE");
			}
		});
		buttonCopying.setText("COPYING");
		buttonCopying.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showFileAndClose("COPYING");
			}
		});
		buttonOK.setText("CLOSE");
		buttonOK.addActionListener(this);
		panel1.setLayout(borderLayout1);
		final GridBagLayout gridBagLayout = new GridBagLayout();
		insetsPanel3.setLayout(gridBagLayout);
		insetsPanel3.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
		insetsPanel3.add(label1, gridBagConstraints11);
		final GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.weighty = 0;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridx = 3;
		insetsPanel3.add(getPanel(), gridBagConstraints);
		insetsPanel3.add(label3, gridBagConstraints21);
		insetsPanel3.add(label4, gridBagConstraints31);
		insetsPanel3.add(label5, gridBagConstraints41);
		insetsPanel3.add(label6, gridBagConstraints51);
		insetsPanel3.add(label7, gridBagConstraints61);
		insetsPanel3.add(imageLabel, gridBagConstraints8);
		insetsPanel1.add(buttonLicence, null);
		insetsPanel1.add(buttonCopying, null);
		insetsPanel1.add(buttonOK, null);
		panel1.add(insetsPanel1, BorderLayout.SOUTH);
		panel1.add(insetsPanel3, BorderLayout.NORTH);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		gbc.insets = new Insets(2, 0, 5, 5);
		gbc.gridx = 0;
		gbc.gridy = 4;
		this.getContentPane().add(panel1, null);
		setResizable(true);
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancel();
		}
		super.processWindowEvent(e);
	}

	void showFileAndClose(String file) {
		JFrame dlg = new JFrame();
		dlg.setIconImage(Resources.getScaledIcon(Resources.iAbout, 64).getImage());
		dlg.setTitle(file);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);	
		JTextPane startTxt = new JTextPane();
		startTxt.setFont(new java.awt.Font("Geneva", 0, 12));
		startTxt.setOpaque(true);
		StringBuffer sb = new StringBuffer () ;
		try{
			URL url = Highlander.class.getResource("/"+file+".txt");
			try (InputStream in=url.openStream()){
				try (InputStreamReader isr = new InputStreamReader(in)){
					try (BufferedReader dis = new BufferedReader(isr)){
						String line;
						while ((line = dis.readLine()) != null){
							sb.append(line + "\n");
						}
					}
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
			sb.append("Can't retreive "+ file + "\n");
			sb.append(ex.getMessage() + "\n");
			sb.append("Java exception : "+ex.getCause());
			for (StackTraceElement el : ex.getStackTrace()){
				sb.append("\n  " + el.toString());
			}
		}
		startTxt.setText(sb.toString());
		startTxt.setCaretPosition(0);
		scrollPane.setViewportView(startTxt);	
		dlg.getContentPane().add(scrollPane, BorderLayout.CENTER);
		dlg.pack();
		Tools.centerWindow(dlg, false);
		//dlg.setExtendedState(Highlander.MAXIMIZED_BOTH);
		dispose();
		dlg.setVisible(true);
	}
	
	//Close the dialog
	void cancel() {
		dispose();
	}

	//Close the dialog on a button event
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonOK) {
			cancel();
		}
	}
	/**
	 * @return
	 */
	protected JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
		}
		return panel;
	}
	/**
	 * @return
	 */
}
