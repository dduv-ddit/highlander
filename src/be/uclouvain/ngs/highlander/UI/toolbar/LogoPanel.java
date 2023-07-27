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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.AboutBox;
import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;

public class LogoPanel extends JPanel {

	public LogoPanel(){
		initUI();
	}

	private void initUI(){
		setLayout(new BorderLayout(0,0));

		JButton helpAbout = new JButton(Resources.getScaledIcon(Resources.iAbout, 40));
		helpAbout.setPreferredSize(new Dimension(54,54));
		helpAbout.setToolTipText("About");
		helpAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						about();
					}
				}, "LogoPanel.about").start();
			}
		});

		JButton linkGEHU = new JButton(Resources.getHeightScaledIcon(Resources.iLogoGEHU, 40));
		//linkGEHU.setPreferredSize(new Dimension(54,54));
		linkGEHU.setToolTipText("Highlander has been developped at the Laboratory of Human Molecular Genetics from the de Duve Institute (UCLouvain)");
		linkGEHU.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Tools.openURL("http://www.deduveinstitute.be/human-genetics");
					}
				}, "LogoPanel.openURL").start();
			}
		});
		
		JButton linkDeDuve = new JButton(Resources.getHeightScaledIcon(Resources.iLogoDeDuveHorizontal, 40));
		//linkDeDuve.setPreferredSize(new Dimension(54,54));
		linkDeDuve.setToolTipText("Highlander has been developped at the Laboratory of Human Molecular Genetics from the de Duve Institute (UCLouvain)");
		linkDeDuve.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Tools.openURL("http://www.deduveinstitute.be");
					}
				}, "LogoPanel.openURL").start();
			}
		});
		
		JButton linkUCLouvain = new JButton(Resources.getHeightScaledIcon(Resources.iLogoUCLouvainHorizontal, 40));
		//linkUCLouvain.setPreferredSize(new Dimension(54,54));
		linkUCLouvain.setToolTipText("Highlander has been developped at the Laboratory of Human Molecular Genetics from the de Duve Institute (UCLouvain)");
		linkUCLouvain.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Tools.openURL("https://uclouvain.be/");
					}
				}, "LogoPanel.openURL").start();
			}
		});
		
		JButton linkWelbio = new JButton(Resources.getHeightScaledIcon(Resources.iLogoWelbio, 40));
		//linkWelbio.setPreferredSize(new Dimension(54,54));
		linkWelbio.setToolTipText("Highlander has been funded thanks to Welbio");
		linkWelbio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Tools.openURL("http://welbio.org/");
					}
				}, "LogoPanel.openURL").start();
			}
		});
		
		JButton linkInnoviris = new JButton(Resources.getHeightScaledIcon(Resources.iLogoInnoviris, 40));
		//linkInnoviris.setPreferredSize(new Dimension(54,54));
		linkInnoviris.setToolTipText("Highlander has been funded thanks to Innovris");
		linkInnoviris.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Tools.openURL("https://innoviris.brussels/");
					}
				}, "LogoPanel.openURL").start();
			}
		});
		
		JButton linkFCE = new JButton(Resources.getHeightScaledIcon(Resources.iLogoFCE, 40));
		//linkFCE.setPreferredSize(new Dimension(54,54));
		linkFCE.setToolTipText("Highlander has been funded thanks to Fondation contre le Cancer");
		linkFCE.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Tools.openURL("https://www.cancer.be/");
					}
				}, "LogoPanel.openURL").start();
			}
		});
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));

		panel.add(helpAbout);
		panel.add(linkGEHU);
		panel.add(linkDeDuve);
		panel.add(linkUCLouvain);
		panel.add(linkWelbio);
		panel.add(linkInnoviris);
		panel.add(linkFCE);

		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
		add(scrollablePanel, BorderLayout.CENTER);
	}

	public void about(){
		AboutBox dlg = new AboutBox(new JFrame());
		dlg.setModal(true);
		dlg.pack();
		Tools.centerWindow(dlg, false);
		dlg.setVisible(true);
	}


}
