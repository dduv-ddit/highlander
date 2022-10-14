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

package be.uclouvain.ngs.highlander.UI.details;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.ExternalLink;

/**
 * External links to web ressources.
 * It uses information of selected variant(s) to open a web page directly on it.
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxExternalLinks extends DetailsBox {

	private DetailsPanel mainPanel;
	private boolean detailsLoaded = false;

	private List<Integer> variantIds;

	public DetailsBoxExternalLinks(List<Integer> variantIds, DetailsPanel mainPanel){
		this.variantIds = variantIds;
		this.mainPanel = mainPanel;
		boolean visible = mainPanel.isBoxVisible(getTitle());						
		initCommonUI(visible);
	}

	public DetailsPanel getDetailsPanel(){
		return mainPanel;
	}

	public String getTitle(){
		return "External links";
	}

	public Palette getColor() {
		return Field.dbsnp_id.getCategory().getColor();
	}

	protected boolean isDetailsLoaded(){
		return detailsLoaded;
	}

	/*
	 * Not used anymore
	 * With first implementation of WrapLayout, 
	 * they were bugs when public/private/evaluation annotation boxes were ON (not wrapping, all buttons put on one line)
	 * 
	 * But since last additions of WrapLayout, problem seems to be solved
	 * 
	 * Keep it just in case ...
	 * 
	private JPanel panel = new JPanel(new GridLayout(0,1,0,0));
	private JPanel currentLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 0));
	private int currentLength = 0;

	private void add(JButton button){
		if (currentLength + button.getPreferredSize().getWidth() > getSize().getWidth()){
			currentLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 0));
			panel.add(currentLine);
			currentLength = 10;
		}
		currentLine.add(button);			
		currentLength += button.getPreferredSize().getWidth() + 1;
	}
	 */
	
	protected void loadDetails(){
		final int HEIGHT = 36;

		JPanel panel = new JPanel(new WrapLayout());
		panel.setBackground(Resources.getColor(getColor(), 200, false));
		//panel.add(currentLine);
		
		for (ExternalLink link : Highlander.getAvailableExternalLinks()) {
			//System.out.println("Preparing external link: " + link);
			if (link.isEnable()) {
				Optional<ActionListener> listener = link.getActionListener(variantIds);
				if (listener.isPresent()) {
					JButton button = link.getButton();
					button.addActionListener(listener.get());
					//add(button);
					panel.add(button);
				}
			}
			//System.out.println("DONE external link: " + link);
		}
			
		try{
			Set<String> geneSymbols = new TreeSet<>();
			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT " + Field.gene_symbol.getQuerySelectName(analysis, false)
					+ "FROM " +	analysis.getFromSampleAnnotations()
					+ analysis.getJoinGeneAnnotations()
					+ "WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" IN (" + HighlanderDatabase.makeSqlList(variantIds, Integer.class) + ")")){
				while (res.next()){
					if (res.getString(Field.gene_symbol.toString()) != null && res.getString(Field.gene_symbol.toString()).length() > 0) geneSymbols.add(res.getString(Field.gene_symbol.toString()));
				}
				if (geneSymbols.contains("BRCA1")){
					JButton button = new JButton(Resources.getHeightScaledIcon(Resources.iExtNhgriBic, HEIGHT));
					button.setToolTipText("Gene BRCA1 in NHGRI Breast Cancer Information Core");
					button.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							new Thread(new Runnable(){
								public void run(){
									Tools.openURL("https://research.nhgri.nih.gov/projects/bic/Member/brca1_mutation_database.shtml");
								}
							}, "DetailsBoxExternalLinks.BRCA1").start();
						}
					});
					//add(button);
					panel.add(button);
				}
				if (geneSymbols.contains("BRCA2")){
					JButton button = new JButton(Resources.getHeightScaledIcon(Resources.iExtNhgriBic, HEIGHT));
					button.setToolTipText("Gene BRCA2 in NHGRI Breast Cancer Information Core");
					button.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							new Thread(new Runnable(){
								public void run(){
									Tools.openURL("https://research.nhgri.nih.gov/projects/bic/Member/brca2_mutation_database.shtml");
								}
							}, "DetailsBoxExternalLinks.BRCA2").start();
						}
					});
					//add(button);
					panel.add(button);
				}
			}
			
			/*
			 * InterVar
			 * 
			 * URL doesn't work, it has to be a POST
			 * This solution POST their example and stream the resulting HTML in a temporary file that then could be opened by a browser
			 * If users really want that external link, it can be added using this solution 
			 * 
			String urlParameters  = "queryType=position&chr=1&pos=115828756&ref=G&alt=A&build=hg19_update";
			URL url = new URL("http://wintervar.wglab.org/results.php");
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())){
				writer.write(urlParameters);
				writer.flush();
				String line;
				File temp = File.createTempFile("tempfile", ".html");
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
					try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))){
						while ((line = reader.readLine()) != null) {
							System.out.println(line);
							bw.write(line);
						}
					}
				}
			}
			Desktop.getDesktop().browse(temp.toURI());
			*/
			
			detailsPanel.removeAll();
			detailsPanel.add(panel, BorderLayout.CENTER);
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}

}
