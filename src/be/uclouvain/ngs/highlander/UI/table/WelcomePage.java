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

package be.uclouvain.ngs.highlander.UI.table;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;

public class WelcomePage extends JTextPane {

	public WelcomePage(boolean integrated){
		super();
		setEditorKit(new HTMLEditorKit());
		//setBackground(Color.black);
		setFont(new java.awt.Font("Geneva", 0, 18));
		//setForeground(Color.green);
		setOpaque(true);
		//setCaretColor(Color.black);
		String text = 
			"<BODY style=\"font-family:geneva; font-size:10px \">" +
			"<table border=0 cellspacing=0 width=\"100%\" height=\"100%\">" +
			"<tr><td height=50 colspan=\""+(integrated?2:1)+"\"></td></tr>" +
			(integrated?
			
					/*
			"<tr style=\"text-align: center; font-size:18px;\"><td bgcolor=\"#FFE7AD\" style=\"vertical-align:middle\" colspan=\"2\">" +
			"Welcome to Highlander !<br>" +
			"Please generate a filter to start ..." +
			"</td></tr>" +
			 */
					
			"<tr><td height=200 colspan=\"2\"><table border=0 cellspacing=0 width=\"100%\" height=\"100%\"><tr>"+
			"<td width=\"30%\" align=\"center\"><img src=\""+Resources.iLogoDeDuveVertival.toString()+"\" width=\"190\" height=\"190\" style=\"background: #C6D580; display:block;\" /></a></td>"+
			"<td width=\"40%\" align=\"center\"><img src=\""+Resources.iLogoHighlander.toString()+"\" width=\"486\" height=\"191\" border=\"0\" /></a></td>"+
			"<td width=\"30%\" align=\"center\"><img src=\""+Resources.iLogoUCLouvainVertical.toString()+"\" width=\"190\" height=\"190\" style=\"background: #C6D580; display:block;\" /></a></td>"+
			"</tr></table></td></tr>"+
			
			"<tr><td colspan=\"2\" width=\"100%\" style=\"font-size:18px; text-align: center;\"><b>VIKKULA lab</b></td></tr>"+
			
			"<tr>" +
			"<td height=50 colspan=\"2\"></td></tr>"
			:"")+
			"<tr>" +
			"<td>" +	
			"<table border=0 cellspacing=0 width=\"100%\"><tr>" +
			"<td style=\"width:60px; text-align: center;\"><img src=\""+Resources.iUpdater.toString()+"\" height=\"48\" width=\"48\"></td>" +
			"<td style=\"font-size:18px;\">Last changes to Highlander</td>" +
			"</tr></table>" +
			"</td>" +
			(integrated?
					"<td>"+
					"<table border=0 cellspacing=0 width=\"100%\"><tr>" +
					"<td style=\"width:60px; text-align: center;\"><img src=\""+Resources.iLastDbAdditions.toString()+"\" height=\"48\" width=\"48\"></td>" +
					"<td style=\"font-size:18px;\">Imported samples associated with you</td>" +
					"</tr></table>"
					:"")+
			"</td>" +
			"</tr>" +

			"<tr>" +
			"<td valign=\"top\" style=\"width:50%;\">" +
			"<table border=0 cellspacing=0 width=\"100%\"><tr>" +
			"<td style=\"width:30px\"></td>"+
			"<td valign=\"top\">"+getLastChanges()+"</td>" +
			"</tr></table>" +
			"</td>" +
			(integrated?
					"<td valign=\"top\" style=\"width:50%;\">" +
					"<table border=0 cellspacing=0 width=\"100%\"><tr>" +
					"<td style=\"width:30px\"></td>"+
					"<td valign=\"top\">"+getLastAdditions()+"</td>" +
					"</tr></table>" +
					"</td>"
					:"")+
			"</tr>" +

			"</table>" +
			"</BODY>";
		setText(text);
		setCaretPosition(0);
		setEditable(false);
	}

	public String getLastChanges(){
		StringBuffer sb = new StringBuffer () ;
		try{
			URL url = Highlander.class.getResource("changelog.html");
			try (InputStream in=url.openStream()){
				try (InputStreamReader isr = new InputStreamReader(in)){
					try (BufferedReader dis = new BufferedReader(isr)){
						String line;
						while ((line = dis.readLine()) != null){
							sb.append(line);
						}
					}
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
			sb.append("Can't retreive last changes<br>");
			sb.append(ex.getMessage() + "<br>");
			sb.append("Java exception : "+ex.getCause());
			for (StackTraceElement el : ex.getStackTrace()){
				sb.append("<br>  " + el.toString());
			}
		}
		return sb.toString();
	}

	public String getLastAdditions(){
		StringBuilder sb = new StringBuilder();
		try{
			DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMMM yyyy");
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
					"SELECT p.project_id, analysis, p.platform, p.sample, pathology, p.sample_type, p.normal_id, p2.sample, p.run_date " +
					"FROM projects as p " +
					"JOIN pathologies USING (pathology_id) " +
					"JOIN projects_users as pu USING (project_id) " +
					"JOIN projects_analyses as a USING (project_id) " +
					"LEFT JOIN projects as p2 ON p.normal_id = p2.project_id " +
					"WHERE pu.username = '"+Highlander.getLoggedUser().getUsername()+"' " +
					"ORDER BY p.run_date DESC, analysis ASC")){
				LocalDate currentDate = null;
				String currentAnalysis = null;
				boolean first = true;
				sb.append("<br>");
				while(res.next()){
					AnalysisFull analysis = Highlander.getAnalysis(res.getString("analysis"));
					if (!res.getDate("run_date").equals(currentDate) || !res.getString("analysis").equals(currentAnalysis)){					
						currentDate = res.getDate("p.run_date");
						currentAnalysis = res.getString("analysis");
						if (!first) sb.append("</ul>");
						else first = false;
						sb.append("<b>"+currentDate.format(df)+"</b> in "+analysis+" ("+analysis.getSequencingTarget()+" aligned on "+analysis.getReference()+", variants called with "+analysis.getVariantCaller()+")<br><ul>");
					}
					String samp = res.getString("sample_type") + " sample with id " + res.getString("p.sample") + " ("+res.getString("pathology")+").";
					if (analysis.getVariantCaller() == VariantCaller.MUTECT && res.getString("p.normal_id") != null) {
						samp += " " + res.getString("p2.sample") + " has been used as 'normal' sample for calling.";
					}
					sb.append("<li>" + samp + "</li>");
				}
				if (!first) sb.append("</ul>");
			}
		}catch (Exception ex){
			Tools.exception(ex);
			sb.append("Can't fetch data from the projects database<br>");
			sb.append(ex.getMessage() + "<br>");
			sb.append("Java exception : "+ex.getCause());
			for (StackTraceElement el : ex.getStackTrace()){
				sb.append("<br>  " + el.toString());
			}
		}
		return sb.toString();
	}
}
