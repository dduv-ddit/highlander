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

package be.uclouvain.ngs.highlander;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import be.uclouvain.ngs.highlander.database.HighlanderDataSource;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;

public class Parameters {

	public enum Platform {
		HISEQ("HiSeq"),
		X("HiSeq X"),
		NOVASEQ("NovaSeq"),
		MINISEQ("MiniSeq"),
		MISEQ("MiSeq"),
		NEXTSEQ("NextSeq"),
		PACBIO("PacBio"),
		MINION("MinIon"),
		ION_TORRENT("Ion Torrent"),  	
		PROTON("Proton"),  	
		SOLID("Solid");
		private final String name;
		Platform(String name){this.name = name;}
		public String getName(){return name;}
	}

	public enum PasswordPolicy {
		from_settings, ask_at_login, same_as_highlander
	}

	public enum Protocol {
		http, ssh
	}

	private Document doc;

	private String config = "Default";
	private String configPath = "config";

	private HighlanderDataSource dataSource;
	
	//Database
	private String dbMainHost;
	private String dbMainUser;
	private String dbMainPassword;
	private DBMS dbMainJdbc;
	private boolean dbMainCompression = false;

	//Database schemas
	private String schemaHighlander;

	//Server
	//File server
	private String serverFileHost;
	private Protocol serverFileProtocol = Protocol.http;
	private String serverFileSql;
	private String serverFilePhp;
	private String serverFileDbsnp;
	private String serverFileReports;

	//Pipeline server
	private String serverPipelineHost;
	private Protocol serverPipelineProtocol = Protocol.ssh;
	private String serverPipelineUsername;
	private String serverPipelinePrivateKey;
	private String serverPipelineScriptsPath;
	private String serverPipelineHdfsImportPath;

	//Sequencer servers
	private Platform[] availablePlatforms;
	private Map<Platform, String> serverSequencerHost = new HashMap<Platform, String>();
	private Map<Platform, Protocol> serverSequencerProtocol = new HashMap<Platform, Protocol>();
	private Map<Platform, String> serverSequencerUsername = new HashMap<Platform, String>();
	private Map<Platform, String> serverSequencerPrivateKey = new HashMap<Platform, String>();

	//Email
	private String smtp;
	private String adminMail;

	//External
	private String IGV;
	private String pavian;

	//Proxy
	private String httpProxyHost;
	private String httpProxyPort;
	private String httpProxyUser;
	private String httpProxyPassword;
	private String httpProxyBypass;
	private PasswordPolicy httpProxyPasswordPolicy;

	public class MissingParameterException extends Exception {

		public MissingParameterException() {}

		public MissingParameterException(String parent, String parameter) {
			super("The mandatory parameter '"+parameter+"' is missing in section '"+parent+"'. You must add it to your Highlander config/settings.xml file.");
		}

		public MissingParameterException(Throwable cause) {
			super(cause);
		}

		public MissingParameterException(String parent, String parameter, Throwable cause) {
			super("The mandatory parameter '"+parameter+"' is missing in section '"+parent+"'. You must add it to your Highlander config/settings.xml file.", cause);
		}

	}

	public Parameters(boolean GUI) {
		Map<String, File> available = new TreeMap<String, File>();
		File defaultFile = new File("config/settings.xml").getAbsoluteFile();
		if (defaultFile.exists()) available.put("Default", defaultFile);
		File configDir = new File("config").getAbsoluteFile();
		if (configDir.exists() && configDir.isDirectory()){
			for (File file : configDir.listFiles()){
				if (file.isDirectory()){
					File settings = new File(file.getAbsolutePath() + "/settings.xml");
					if (settings.exists()) available.put(file.getName(), settings);
				}
			}
		}
		if (available.isEmpty()){
			if (GUI){
				JOptionPane.showMessageDialog(new JFrame(), "Configuration file not found", "Can't initialize Highlander", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}else{
				System.err.println("Can't initialize Highlander, configuration file not found");
			}
			System.exit(1);  		
		}  	
		File selection = available.get(available.keySet().iterator().next());
		if (available.containsKey("Default")){
			config = "Default";
		}else{
			config = available.keySet().iterator().next();
		}
		if (available.size() > 1 && GUI){
			Object selectedOp = JOptionPane.showInputDialog(new JFrame(), "Which settings do you want to use ?", "Configuration selection", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDb,64), available.keySet().toArray(new String[0]), "Default");
			if (selectedOp != null) config = selectedOp.toString();
		}
		selection = available.get(config);
		configPath = selection.getParent();
		try{
			readSettingsFromXML(selection);
		}catch (Exception ex){
			Tools.exception(ex);
			if (GUI){
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem when reading configuration file", ex), "Reading Highlander parameters",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
			System.exit(-1);
		}	
	}

	public Parameters(boolean GUI, File settings) {
		configPath = settings.getParent();
		try{
			readSettingsFromXML(settings);
		}catch (Exception ex){
			Tools.exception(ex);
			if (GUI){
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem when reading configuration file", ex), "Reading Highlander parameters",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
			System.exit(-1);
		}	
	}

	public void readSettingsFromXML(File settingsFile) throws Exception {
		doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(settingsFile);

		//Database
		String dbms = readParameterInXml("database", "dbms", false);
		if (dbms == null) dbMainJdbc = DBMS.mysql;
		else dbMainJdbc = DBMS.valueOf(dbms);
		if (dbMainJdbc == DBMS.mysql){
			String compression = readParameterInXml("database", "compression", false);
			if (compression != null) dbMainCompression = compression.equalsIgnoreCase("on");
			dbMainHost = readParameterInXml("database", "host", true);
			dbMainUser = readParameterInXml("database", "user", true);
			dbMainPassword = readParameterInXml("database", "password", true);  		
		}

		schemaHighlander = readParameterInXml("database", "highlander", true);

		dataSource = new HighlanderDataSource(this, 5);
		
		//Server
		//Files 
		serverFileHost = readParameterInXml("server", "files", "host", false);
		String protocolFiles = readParameterInXml("server", "files", "protocol", false);
		if (protocolFiles == null) serverFileProtocol = Protocol.http;
		else serverFileProtocol = Protocol.valueOf(protocolFiles);
		serverFileSql = readParameterInXml("server", "files", "sql", false);
		serverFilePhp = readParameterInXml("server", "files", "php", false);
		serverFileDbsnp = readParameterInXml("server", "files", "dbsnp", false);
		serverFileReports = readParameterInXml("server", "files", "reports", false);

		//Pipeline
		serverPipelineHost = readParameterInXml("server", "pipeline", "host", false);
		String protocolPipeline = readParameterInXml("server", "pipeline", "protocol", false);
		if (protocolPipeline == null) serverPipelineProtocol = Protocol.ssh;
		else serverPipelineProtocol = Protocol.valueOf(protocolPipeline);
		serverPipelineUsername = readParameterInXml("server", "pipeline", "username", false);
		serverPipelinePrivateKey = readParameterInXml("server", "pipeline", "privatekey", false);
		serverPipelineScriptsPath = readParameterInXml("server", "pipeline", "scripts", false);
		serverPipelineHdfsImportPath = readParameterInXml("server", "pipeline", "hdfsimport", false);

		//Sequencer
		Set<String> sequencers = readParameterValuesInXml("server", "sequencer");
		availablePlatforms = new Platform[sequencers.size()];
		int p=0;
		for (String sequencer : sequencers) {
			Platform platform = Platform.valueOf(sequencer);
			availablePlatforms[p++] = platform;
			serverSequencerHost.put(platform, readParameterInXml("server", "sequencer", sequencer, "host", false));
			String protocolSequencer = readParameterInXml("server", "sequencer", sequencer, "protocol", false);
			if (protocolSequencer == null) serverSequencerProtocol.put(platform, Protocol.ssh);
			else serverSequencerProtocol.put(platform,Protocol.valueOf(protocolSequencer));
			serverSequencerUsername.put(platform, readParameterInXml("server", "sequencer", sequencer, "username", false));
			serverSequencerPrivateKey.put(platform, readParameterInXml("server", "sequencer", sequencer, "privatekey", false));
		}
		//Email
		smtp = readParameterInXml("email", "smtp", false);
		adminMail = readParameterInXml("email", "admin", false);

		//External
		IGV = readParameterInXml("external", "IGV", false);
		pavian = readParameterInXml("external", "pavian", false);

		//Proxy
		httpProxyHost = readParameterInXml("http_proxy", "host", false);
		httpProxyPort = readParameterInXml("http_proxy", "port", false);
		httpProxyBypass = readParameterInXml("http_proxy", "bypass", false);
		httpProxyUser = readParameterInXml("http_proxy", "user", false);
		String readPasswordPolicy = readParameterInXml("http_proxy", "passwordpolicy", false);
		if (readPasswordPolicy == null) {
			httpProxyPasswordPolicy = PasswordPolicy.from_settings;
			httpProxyPassword = readParameterInXml("http_proxy", "password", false);
		}else {
			switch(readPasswordPolicy) {
			case "ask_at_login":
				httpProxyPasswordPolicy = PasswordPolicy.ask_at_login;
				break;
			case "same_as_highlander":
				httpProxyPasswordPolicy = PasswordPolicy.same_as_highlander;
				break;
			default:
				httpProxyPasswordPolicy = PasswordPolicy.from_settings;
				httpProxyPassword = readParameterInXml("http_proxy", "password", false);
				break;		
			}
		}
		if (httpProxyHost != null) System.setProperty("http.proxyHost", httpProxyHost);
		if (httpProxyPort != null) System.setProperty("http.proxyPort", httpProxyPort);
		if (httpProxyBypass != null) System.setProperty("http.nonProxyHosts", httpProxyBypass);
		setProxyLogin();

		dataSource.close();
	}

	public void setProxyLogin() {
		if (httpProxyUser != null && httpProxyPassword != null) {
			System.setProperty("http.proxyUser", httpProxyUser);
			System.setProperty("http.proxyPassword", httpProxyPassword);
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					if (getRequestorType() == RequestorType.PROXY) {
						if (getRequestingHost().toLowerCase().equals(System.getProperty("http.proxyHost").toLowerCase())) {
							if (Integer.parseInt(System.getProperty("http.proxyPort")) == getRequestingPort()) {
								return new PasswordAuthentication(System.getProperty("http.proxyUser"),System.getProperty("http.proxyPassword").toCharArray());
							}
						}
					}
					return null;
				}});
		}
	}

	private String readParameterInDatabase(String section, String setting) {
		if (dataSource != null) {
			try {
				try(Connection con = dataSource.getConnection(getSchemaHighlander())){
					try(Statement stm = con.createStatement()){
						try(ResultSet res = stm.executeQuery("SELECT `value` FROM `settings` WHERE `section` = '"+section+"' AND `setting` = '"+setting+"'")){
							if(res.next()) {
								return res.getString("value");
							}
						}
					}
				}
			}catch(Exception ex) {
				//ex.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	private String readParameterInXml(String parent, String elem, boolean mandatory) throws Exception {
		NodeList nodeList = doc.getElementsByTagName(parent);
		if (nodeList.getLength() > 0){
			NodeList child = ((Element)nodeList.item(0)).getElementsByTagName(elem);
			if (child.getLength() > 0){
				return ((Element)((Element)nodeList.item(0)).getElementsByTagName(elem).item(0)).getAttribute("value");
			}
		}
		String fromDb = readParameterInDatabase(parent, elem);
		if (fromDb != null && fromDb.length() > 0) return fromDb;
		if (mandatory) throw new MissingParameterException("<"+parent+">", elem);
		return null;
	}

	private String readParameterInXml(String parent, String child, String elem, boolean mandatory) throws Exception {
		NodeList nodeList = doc.getElementsByTagName(parent);
		if (nodeList.getLength() > 0){
			NodeList child1 = ((Element)nodeList.item(0)).getElementsByTagName(child);
			if (child1.getLength() > 0){
				NodeList child2 = ((Element)child1.item(0)).getElementsByTagName(elem);
				if (child2.getLength() > 0){
					return ((Element)child2.item(0)).getAttribute("value");
				}
			}
		}
		String fromDb = readParameterInDatabase(parent+"|"+child, elem);			
		if (fromDb != null && fromDb.length() > 0) return fromDb;
		if (mandatory) throw new MissingParameterException("<"+parent+"><"+child+">", elem);
		return null;
	}

	private Set<String> readParameterValuesInXml(String parent, String child){
		Set<String> values = new TreeSet<>();
		NodeList nodeList = doc.getElementsByTagName(parent);
		if (nodeList.getLength() > 0){
			NodeList child1 = ((Element)nodeList.item(0)).getElementsByTagName(child);
			for (int i=0 ; i < child1.getLength() ; i++){
				values.add(((Element)child1.item(i)).getAttribute("value"));
			}
		}
		return values;
	}

	private String readParameterInXml(String parent, String child, String childValue, String elem, boolean mandatory) throws MissingParameterException {
		NodeList nodeList = doc.getElementsByTagName(parent);
		if (nodeList.getLength() > 0){
			NodeList child1 = ((Element)nodeList.item(0)).getElementsByTagName(child);
			for (int i=0 ; i < child1.getLength() ; i++){
				String child1Value = ((Element)child1.item(i)).getAttribute("value");
				if (child1Value.equalsIgnoreCase(childValue)) {
					NodeList child2 = ((Element)child1.item(i)).getElementsByTagName(elem);
					if (child2.getLength() > 0){
						return ((Element)child2.item(0)).getAttribute("value");
					}
				}
			}
		}
		if (mandatory) throw new MissingParameterException("<"+parent+"><"+child+" value=\""+childValue+"\">", elem);
		return null;
	}

	public String getUrlForSqlImportFiles() {
		if (serverFileProtocol == null || serverFileHost == null || serverFileSql == null) return null;
		return serverFileProtocol + "://" + serverFileHost + "/" + serverFileSql;
	}

	public String getUrlForPhpScripts() {
		if (serverFileProtocol == null || serverFileHost == null || serverFilePhp == null) return null;
		return serverFileProtocol + "://" + serverFileHost + "/" + serverFilePhp;
	}

	public String getUrlForDbsnpVcfs() {
		if (serverFileProtocol == null || serverFileHost == null || serverFileDbsnp == null) return null;
		return serverFileProtocol + "://" + serverFileHost + "/" + serverFileDbsnp;
	}

	public String getUrlForReports() {
		if (serverFileProtocol == null || serverFileHost == null || serverFileReports == null) return null;
		return serverFileProtocol + "://" + serverFileHost + "/" + serverFileReports;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getConfigPath() {
		return configPath;
	}
	
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}
	
	public String getDbMainHost() {
		return dbMainHost;
	}

	public void setDbMainHost(String dbMainHost) {
		this.dbMainHost = dbMainHost;
	}

	public String getDbMainUser() {
		return dbMainUser;
	}

	public void setDbMainUser(String dbMainUser) {
		this.dbMainUser = dbMainUser;
	}

	public String getDbMainPassword() {
		return dbMainPassword;
	}

	public void setDbMainPassword(String dbMainPassword) {
		this.dbMainPassword = dbMainPassword;
	}

	public DBMS getDbMainJdbc() {
		return dbMainJdbc;
	}

	public void setDbMainJdbc(DBMS dbMainJdbc) {
		this.dbMainJdbc = dbMainJdbc;
	}

	public boolean isDbMainCompression() {
		return dbMainCompression;
	}

	public void setDbMainCompression(boolean dbMainCompression) {
		this.dbMainCompression = dbMainCompression;
	}

	public String getSchemaHighlander() {
		return schemaHighlander;
	}

	public void setSchemaHighlander(String schemaHighlander) {
		this.schemaHighlander = schemaHighlander;
	}

	public String getServerFileHost() {
		return serverFileHost;
	}

	public void setServerFileHost(String serverFileHost) {
		this.serverFileHost = serverFileHost;
	}

	public Protocol getServerFileProtocol() {
		return serverFileProtocol;
	}

	public void setServerFileProtocol(Protocol serverFileProtocol) {
		this.serverFileProtocol = serverFileProtocol;
	}

	public String getServerFileSql() {
		return serverFileSql;
	}

	public void setServerFileSql(String serverFileSql) {
		this.serverFileSql = serverFileSql;
	}

	public String getServerFilePhp() {
		return serverFilePhp;
	}

	public void setServerFilePhp(String serverFilePhp) {
		this.serverFilePhp = serverFilePhp;
	}

	public String getServerFileDbsnp() {
		return serverFileDbsnp;
	}

	public void setServerFileDbsnp(String serverFileDbsnp) {
		this.serverFileDbsnp = serverFileDbsnp;
	}

	public String getServerFileReports() {
		return serverFileReports;
	}

	public void setServerFileReports(String serverFileReports) {
		this.serverFileReports = serverFileReports;
	}

	public String getServerPipelineHost() {
		return serverPipelineHost;
	}

	public void setServerPipelineHost(String serverPipelineHost) {
		this.serverPipelineHost = serverPipelineHost;
	}

	public Protocol getServerPipelineProtocol() {
		return serverPipelineProtocol;
	}

	public void setServerPipelineProtocol(Protocol serverPipelineProtocol) {
		this.serverPipelineProtocol = serverPipelineProtocol;
	}

	public String getServerPipelineUsername() {
		return serverPipelineUsername;
	}

	public void setServerPipelineUsername(String serverPipelineUsername) {
		this.serverPipelineUsername = serverPipelineUsername;
	}

	public String getServerPipelinePrivateKey() {
		return serverPipelinePrivateKey;
	}

	public void setServerPipelinePrivateKey(String serverPipelinePrivateKey) {
		this.serverPipelinePrivateKey = serverPipelinePrivateKey;
	}

	public String getServerPipelineScriptsPath() {
		return serverPipelineScriptsPath;
	}

	public void setServerPipelineScriptsPath(String serverPipelineScriptsPath) {
		this.serverPipelineScriptsPath = serverPipelineScriptsPath;
	}

	public String getServerPipelineHdfsImportPath() {
		return serverPipelineHdfsImportPath;
	}

	public void setServerPipelineHdfsImportPath(
			String serverPipelineHdfsImportPath) {
		this.serverPipelineHdfsImportPath = serverPipelineHdfsImportPath;
	}

	public Platform[] getAvailablePlatforms() {
		return availablePlatforms;
	}

	public void setAvailablePlatforms(Platform[] availablePlatforms) {
		this.availablePlatforms = availablePlatforms;
	}

	public Map<Platform, String> getServerSequencerHost() {
		return serverSequencerHost;
	}

	public void setServerSequencerHost(Map<Platform, String> serverSequencerHost) {
		this.serverSequencerHost = serverSequencerHost;
	}

	public Map<Platform, Protocol> getServerSequencerProtocol() {
		return serverSequencerProtocol;
	}

	public void setServerSequencerProtocol(
			Map<Platform, Protocol> serverSequencerProtocol) {
		this.serverSequencerProtocol = serverSequencerProtocol;
	}

	public Map<Platform, String> getServerSequencerUsername() {
		return serverSequencerUsername;
	}

	public void setServerSequencerUsername(
			Map<Platform, String> serverSequencerUsername) {
		this.serverSequencerUsername = serverSequencerUsername;
	}

	public Map<Platform, String> getServerSequencerPrivateKey() {
		return serverSequencerPrivateKey;
	}

	public void setServerSequencerPrivateKey(
			Map<Platform, String> serverSequencerPrivateKey) {
		this.serverSequencerPrivateKey = serverSequencerPrivateKey;
	}

	public String getSmtp() {
		return smtp;
	}

	public void setSmtp(String smtp) {
		this.smtp = smtp;
	}

	public String getAdminMail() {
		return adminMail;
	}

	public void setAdminMail(String adminMail) {
		this.adminMail = adminMail;
	}

	public String getIGV() {
		return IGV;
	}

	public void setIGV(String iGV) {
		IGV = iGV;
	}

	public String getPavian() {
		return pavian;
	}
	
	public void setPavian(String pavian) {
		this.pavian = pavian;
	}
	
	public String getHttpProxyHost() {
		return httpProxyHost;
	}

	public void setHttpProxyHost(String httpProxyHost) {
		this.httpProxyHost = httpProxyHost;
	}

	public String getHttpProxyPort() {
		return httpProxyPort;
	}

	public void setHttpProxyPort(String httpProxyPort) {
		this.httpProxyPort = httpProxyPort;
	}

	public String getHttpProxyUser() {
		return httpProxyUser;
	}

	public void setHttpProxyUser(String httpProxyUser) {
		this.httpProxyUser = httpProxyUser;
	}

	public String getHttpProxyPassword() {
		return httpProxyPassword;
	}

	public void setHttpProxyPassword(String httpProxyPassword) {
		this.httpProxyPassword = httpProxyPassword;
	}

	public String getHttpProxyBypass() {
		return httpProxyBypass;
	}

	public void setHttpProxyBypass(String httpProxyBypass) {
		this.httpProxyBypass = httpProxyBypass;
	}

	public PasswordPolicy getHttpProxyPasswordPolicy() {
		return httpProxyPasswordPolicy;
	}

	public void setHttpProxyPasswordPolicy(PasswordPolicy httpProxyPasswordPolicy) {
		this.httpProxyPasswordPolicy = httpProxyPasswordPolicy;
	}

}
