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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Resources;

public class ToolbarScrollablePanel extends JPanel implements Observer {

	private JButton buttonLeft;
	private JButton buttonRight;
	private JPanel jPanel1;
	private JScrollPane jScrollPane1;

	private int x;
	private int totalwidth; // total width of the toolbar menu
	private Point point;
	private final int MOVEX = 60;
	private JPanel toolbar;
	private int height;
	
	public ToolbarScrollablePanel(JPanel toolbar, HighlanderObserver highlanderObserver, int preferredButtonHeight) {
		super();
		this.toolbar = toolbar;
		this.height = preferredButtonHeight;
		initComponents();
		highlanderObserver.addObserver(this);
		point = new Point(0, 0);
		jPanel1.add(toolbar, BorderLayout.NORTH);
	}

	private void initComponents() {

		int width = Math.min(40, height);
		buttonLeft = new JButton(Resources.getScaledIcon(Resources.iArrowLeft, width));
		buttonLeft.setPreferredSize(new Dimension(width,height));
		buttonLeft.setToolTipText("Right click to direclty scroll to start");
		buttonRight = new JButton(Resources.getScaledIcon(Resources.iArrowRight, width));
		buttonRight.setPreferredSize(new Dimension(width,height));
		buttonRight.setToolTipText("Right click to direclty scroll to end");
		jScrollPane1 = new JScrollPane();
		jPanel1 = new JPanel();

		setLayout(new java.awt.BorderLayout());

		buttonLeft.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonLeftActionPerformed(false);
			}
		});
		buttonLeft.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)){
					buttonLeftActionPerformed(true);
				}
			}
		});
		add(buttonLeft, java.awt.BorderLayout.LINE_START);

		buttonRight.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonRightActionPerformed(false);
			}
		});
		buttonRight.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)){
					buttonRightActionPerformed(true);
				}
			}
		});
		add(buttonRight, java.awt.BorderLayout.LINE_END);

		jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		jScrollPane1.setHorizontalScrollBar(null);
		jScrollPane1.setBorder(null);

		jPanel1.setLayout(new BorderLayout(0,0));
		jScrollPane1.setViewportView(jPanel1);

		add(jScrollPane1, java.awt.BorderLayout.CENTER);
	}//                         

	private void buttonRightActionPerformed(boolean farRight) {
		if (totalwidth <= (x + jScrollPane1.getVisibleRect().width)) {
			// do nothing
		} else if (farRight) {
			x = totalwidth - jScrollPane1.getVisibleRect().width;
			point.x = x;
			jScrollPane1.getViewport().setViewPosition(point);
		} else {
			x += MOVEX;
			if (x > totalwidth) x = totalwidth;
			point.x = x;
			jScrollPane1.getViewport().setViewPosition(point);
		}
		validate();
		repaint();
	}                                        

	private void buttonLeftActionPerformed(boolean farLeft) {
		if (x > 0) {
			if (farLeft){
				x = 0;
			}else{
				x -= MOVEX;
				if (x < 0) x = 0;
			}
			point.x = x;
			jScrollPane1.getViewport().setViewPosition(point);
		}
	}                                        

	@Override
	public void update(Observable o, Object arg) {
		if (arg.toString().equalsIgnoreCase("RESIZE_TOOLBAR")) {
			totalwidth = toolbar.getBounds().width;
			if (getBounds().width < totalwidth) {
				// added left/right buttons for side scrolling
				buttonLeft.setVisible(true);
				buttonRight.setVisible(true);
			} else {
				buttonLeft.setVisible(false);
				buttonRight.setVisible(false);
				x = 0;
			}
			validate();
		}
	}
}

