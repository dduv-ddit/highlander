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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;


import javax.swing.JLabel;
import javax.swing.JProgressBar;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.PostMethod;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.tools.BamViewer;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;

/**
 * Alignment visualization of the BamOut output of GATK HaplotypeCaller.
 * 
 * @author Raphaël Helaers
 *
 */
public class DetailsBoxBamOut extends DetailsBoxAlignment {

	public DetailsBoxBamOut(int variantId, DetailsPanel mainPanel){
		super(variantId, mainPanel);
	}

	public String getTitle(){
		return "Haplotype Caller BamOut";
	}

	protected void loadDetails(){
		try{
			detailsPanel.removeAll();
			JProgressBar bar = new JProgressBar();
			detailsPanel.add(bar, BorderLayout.NORTH);

			AnalysisFull analysis = Highlander.getCurrentAnalysis();
			if (analysis.getVariantCaller() == VariantCaller.GATK || analysis.getVariantCaller() == VariantCaller.MUTECT) {
				String sample = "";
				int project_id = -1;
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT sample, project_id "
						+ "FROM " +	analysis.getFromSampleAnnotations()
						+ analysis.getJoinProjects()
						+	" WHERE variant_sample_id = " + variantSampleId
						)) {
					if (res.next()){
						sample = res.getString("sample");
						project_id = res.getInt("project_id");
					}
				}
				if (sample.length() > 0){
					Variant variant = new Variant(variantSampleId);
					URL url = new URL(analysis.getBamURL(sample).replaceAll(analysis.toString(), "bamout").replace(".bam", "_"+variant.getChromosome()+"_"+variant.getPosition()+".bam"));
					if (!Tools.exists(url.toString())){
						bar.setIndeterminate(true);
						bar.setString("Waiting for GATK BamOut to finish on server");
						bar.setStringPainted(true);
						//launch script server side
						Reference reference = analysis.getReference();					
						String normal = "";
						try (Results res = DB.select(Schema.HIGHLANDER, 
								"SELECT p.sample, p2.sample as normal "
								+ "FROM projects as p "
								+ "LEFT JOIN projects as p2 ON p2.project_id = p.normal_id "
								+ "LEFT JOIN projects_analyses as pa ON pa.project_id = p.project_id "
								+ "WHERE p.project_id = "+project_id+" AND analysis = '"+analysis+"'"
								)) {
							if (res.next()){
								normal = res.getString("normal");
							}
						}	
						if (analysis.getVariantCaller() == VariantCaller.MUTECT) {
							if (normal == null) {						
								throw new Exception("Normal sample was not found, cannot launch Mutect");
							}
						}else{
							normal = "";
						}

						HttpClient httpClient = new HttpClient();
						boolean bypass = false;
						if (System.getProperty("http.nonProxyHosts") != null) {
							for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
								if ((Highlander.getParameters().getUrlForPhpScripts()+"/bamcheck.php").toLowerCase().contains(host.toLowerCase())) bypass = true;
							}
						}
						if (!bypass && System.getProperty("http.proxyHost") != null) {
							try {
								HostConfiguration hostConfiguration = httpClient.getHostConfiguration();
								hostConfiguration.setProxy(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")));
								httpClient.setHostConfiguration(hostConfiguration);
								if (System.getProperty("http.proxyUser") != null && System.getProperty("http.proxyPassword") != null) {
									// Credentials credentials = new UsernamePasswordCredentials(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword"));
									// Windows proxy needs specific credentials with domain ... if proxy user is in the form of domain\\user, consider it's windows
									String user = System.getProperty("http.proxyUser");
									Credentials credentials;
									if (user.contains("\\")) {
										credentials = new NTCredentials(user.split("\\\\")[1], System.getProperty("http.proxyPassword"), System.getProperty("http.proxyHost"), user.split("\\\\")[0]);
									}else {
										credentials = new UsernamePasswordCredentials(user, System.getProperty("http.proxyPassword"));
									}
									httpClient.getState().setProxyCredentials(null, System.getProperty("http.proxyHost"), credentials);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						PostMethod post = new PostMethod(Highlander.getParameters().getUrlForPhpScripts()+"/bamout.php");
						NameValuePair[] data = {
								new NameValuePair("analysis", analysis.toString()),
								new NameValuePair("reference", reference.getName()),
								new NameValuePair("sample", sample),
								new NameValuePair("chr", variant.getChromosome()),
								new NameValuePair("pos", ""+variant.getPosition()),
								new NameValuePair("normal", normal)
						};
						post.addParameters(data);
						int httpRes = httpClient.executeMethod(post); 
						if (httpRes == 200) {			
							try (InputStreamReader isr = new InputStreamReader(post.getResponseBodyAsStream())){
								try (BufferedReader br = new BufferedReader(isr)){
									String line = null;
									while(((line = br.readLine()) != null)) {
										System.out.println(line);
									}
								}
							}
						}else {
							detailsPanel.removeAll();
							detailsPanel.add(new JLabel("Cannot launch bamout on the server, HTTP error " + httpRes), BorderLayout.CENTER);	
						}
					}
					while (!Tools.exists(url.toString())){
						try{
							Thread.sleep(1000);
						}catch (InterruptedException ex){
							Tools.exception(ex);
						}
					}
					Interval interval = new Interval(analysis.getReference(), variant.getChromosome(), variant.getAlternativePosition()+offset-window, variant.getAlternativePosition()+offset+window);
					alignment = BamViewer.getAlignmentPanel(url, interval, variant, softClipped, squished, frameShift, colorBy, true, mainPanel.getWidth(), bar);
					detailsPanel.removeAll();
					detailsPanel.add(alignment, BorderLayout.CENTER);
					detailsPanel.add(getControlBar(), BorderLayout.NORTH);
				}else{
					detailsPanel.removeAll();
					detailsPanel.add(new JLabel("Variant was not found in the database"), BorderLayout.CENTER);			
				}				
			}else {
				detailsPanel.removeAll();
				detailsPanel.add(new JLabel("BamOut is only available for variants called by GATK or MUTECT"), BorderLayout.CENTER);							
			}
		}catch (Exception ex){
			Tools.exception(ex);
			detailsPanel.removeAll();
			detailsPanel.add(Tools.getMessage("Cannot create panel", ex), BorderLayout.CENTER);
		}
		detailsPanel.revalidate();
		detailsLoaded = true;
	}	

}
