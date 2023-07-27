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

package be.uclouvain.ngs.highlander.UI.misc;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;

import javax.swing.JPanel;

public class WaitingPanel extends JPanel implements Runnable {

	private boolean cancelAsked = false;

	final JButton cancelButton;
	final JButton sendQueryButton;
	final CardLayout logoCardLayout;
	final JPanel logoPanel;
	int numImages = 9;
	int running = 0;
	final JProgressBar progress;
	//Finally the progress bar seems better
	JTextArea text;
	List<String> txtLines = new ArrayList<String>();

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(new Color(24,24,24,200));
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	public WaitingPanel() {
		setLayout(new BorderLayout(0,0));
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {	arg0.consume();	}
			@Override
			public void mousePressed(MouseEvent arg0) {	arg0.consume();	}
			@Override
			public void mouseExited(MouseEvent arg0) {	arg0.consume();	}
			@Override
			public void mouseEntered(MouseEvent arg0) {	arg0.consume();	}
			@Override
			public void mouseClicked(MouseEvent arg0) {	arg0.consume();	}
		});
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new GridBagLayout());
		centerPanel.setOpaque(false);
		add(centerPanel,BorderLayout.CENTER);

		logoPanel = new JPanel();
		logoPanel.setOpaque(false);
		centerPanel.add(logoPanel, new GridBagConstraints(0,0,1,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5,5,5,5), 0, 0));
		logoCardLayout = new CardLayout();
		logoPanel.setLayout(logoCardLayout);
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait8, 384)), "0");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait1, 384)), "1");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait2, 384)), "2");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait3, 384)), "3");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait4, 384)), "4");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait5, 384)), "5");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait6, 384)), "6");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait7, 384)), "7");
		logoPanel.add(new JLabel(Resources.getScaledIcon(Resources.iWait8, 384)), "8");

		progress = new JProgressBar();
		progress.setMinimum(0);
		centerPanel.add(progress, new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(30,50,5,50), 0, 0));
		progress.setVisible(false);

		//Progress bar seems better
		text = new JTextArea();
		text.setFont(text.getFont().deriveFont(Font.BOLD));
		text.setEditable(false);
		text.setOpaque(false);
		text.setBorder(null);
		text.setBackground(new Color(0,0,0,0));
		text.setForeground(Color.WHITE);
		//centerPanel.add(text, new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(30,55,5,55), 0, 0));
		//setText();

		cancelButton = new JButton("Cancel query", Resources.getScaledIcon(Resources.iCross, 32));
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try{
							cancelAsked = true;
							Highlander.getDB().cancelActiveSelects();
						}catch(Exception ex){
							Tools.exception(ex);
						}
					}
				}, "Cancel query").start();
			}
		});
		centerPanel.add(cancelButton, new GridBagConstraints(0,2,1,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(30,50,5,50), 0, 0));
		cancelButton.setVisible(false);
		
		sendQueryButton = new JButton("Report slow query", Resources.getScaledIcon(Resources.iEditPen, 32));
		sendQueryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try{
							Tools.sendQueryToAdministrator();
						}catch(Exception ex){
							Tools.exception(ex);
						}
					}
				}, "Cancel query").start();
			}
		});
		centerPanel.add(sendQueryButton, new GridBagConstraints(0,3,1,1,0.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(30,50,5,50), 0, 0));
		sendQueryButton.setVisible(false);
	}

	public boolean isCancelled(){
		return cancelAsked;
	}

	public void setProgressString(final String label, final boolean indeterminate){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progress.setVisible(true);
				progress.setString(label);
				progress.setIndeterminate(indeterminate);
				progress.setStringPainted(true);
			}
		});
	}

	public void setProgressMaximum(final int value){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progress.setVisible(true);
				progress.setMaximum(value);
			}
		});
	}

	public void setProgressValue(final int value){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progress.setValue(value);
			}
		});
	}

	public void setProgressDone(){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progress.setValue(progress.getMaximum());
				progress.setString("Done !");
				progress.setStringPainted(true);
			}
		});
	}

	public void clearText(){
		txtLines.clear();
		setText();
	}

	public void addTextLine(String line){
		txtLines.add(line);
		setText();		
	}

	public void replaceLastTextLine(String line){
		txtLines.remove(txtLines.size()-1);
		txtLines.add(line);
		setText();		
	}

	private void setText(){
		text.setText("");
		int count = 0;
		for (String line : txtLines){
			text.append(line + "\n");
			count++;
		}
		while (count < 5){
			text.append("\n");
			count++;			
		}
	}

	public void start(){
		start(false);
	}

	//TODO ici changer le boolean par un thread passé en paramètre, tuer le thread si le bouton est utilisé
	public void start(boolean allowCancelHighlanderQuery){
		cancelAsked = false;
		cancelButton.setVisible(allowCancelHighlanderQuery);
		if (Highlander.getDB().isBetaFunctionalitiesActivated()) sendQueryButton.setVisible(allowCancelHighlanderQuery);
		running++;
		if (running == 1){
			setVisible(true);
			clearText();
			(new Thread(this, "WaitingLogo-Animation")).start();
		}
	}

	public void stop(){			
		running--;
		if (running == 0){
			progress.setVisible(false);
			setVisible(false);
		}
	}

	public void forceStop(){
		running = 0;
		progress.setVisible(false);
		setVisible(false);
	}

	@Override
	public void run() {			
		int current = 0;
		while (running > 0){
			current++;
			if (current == numImages) current = 0;
			final int icurrent = current;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					logoCardLayout.show(logoPanel, icurrent + "");		
				}
			});
			try{
				Thread.sleep(500);
			}catch (InterruptedException ex){
				Tools.exception(ex);
			}
		}
	}

	public static class CancelException extends Exception {

		public CancelException() {}

		public CancelException(String message) {
			super(message);
		}

		public CancelException(Throwable cause) {
			super(cause);
		}

		public CancelException(String message, Throwable cause) {
			super(message, cause);
		}

	}


}
