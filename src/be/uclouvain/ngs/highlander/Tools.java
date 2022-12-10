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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.sf.samtools.util.ftp.FTPUtils;

public class Tools {

	public static final String OS = System.getProperty("os.name").toLowerCase();
	
	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);

	}

	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
	}
	
	public static void centerWindow(Window frame, boolean fillScreen){
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (fillScreen){
			int width = screenSize.width - (screenSize.width/5);
			int height = screenSize.height - (screenSize.height/5);
			frame.setSize(new Dimension(width,height));
		}
		Dimension windowSize = frame.getSize();
		int width = (int)windowSize.getWidth();
		int height = (int)windowSize.getHeight();
		if (width > screenSize.width){
			frame.setSize(new Dimension(screenSize.width-50,height));
		}
		if (height > screenSize.height){
			frame.setSize(new Dimension(width,screenSize.height-50));
		}
		frame.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
				Math.max(0, (screenSize.height - windowSize.height) / 2));
	}

	public static void print(String text){
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy - HH:mm:ss");
		System.out.println();
		System.out.println(df.format(System.currentTimeMillis()));
		System.out.println(text);
	}

	public static void exception(Exception ex){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		System.err.println(df.format(System.currentTimeMillis()));
		ex.printStackTrace();
		System.err.println();
	}

	public static JPanel getMessage(final String message, Exception e){
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JTextArea label = new JTextArea(message);
		Color bg = panel.getBackground();
		label.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue()));
		label.setBorder(null);
		label.setEditable(false);
		panel.add(label, BorderLayout.NORTH);
		final StringBuilder sb = new StringBuilder();
		sb.append(e.getMessage() + "\n");
		sb.append("Java exception : "+e.getCause());
		for (StackTraceElement el : e.getStackTrace()){
			sb.append("\n  " + el.toString());
		}
		JTextArea textArea = new JTextArea(sb.toString());
		textArea.setCaretPosition(0);
		textArea.setEditable(true);
		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane, BorderLayout.CENTER);
		JButton sendError = new JButton("Send this error to Highlander administrator");
		sendError.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JTextArea commentArea = new JTextArea(8,40);
				JPanel p = new JPanel(new BorderLayout());
				p.add(new JLabel("Please describe how this error happened"), BorderLayout.NORTH);
				p.add(new JScrollPane(commentArea), BorderLayout.CENTER);
				int res = JOptionPane.showConfirmDialog(null, p, "Send this error to Highlander administrator", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey, 64));
				if (res == JOptionPane.OK_OPTION){
					final StringBuilder header = new StringBuilder();
					DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
					header.append("Date : " + df.format(System.currentTimeMillis())+"\n");
					try {
						InetAddress addr = InetAddress.getLocalHost();
						header.append("User : " + ((Highlander.getLoggedUser() != null) ? Highlander.getLoggedUser() : System.getProperty("user.name")) +"\n");
						header.append("Email : " + ((Highlander.getLoggedUser() != null) ? Highlander.getLoggedUser().getEmail() : "user not logged") +"\n");
						header.append("Address : " + addr.getHostAddress() +"\n");
						header.append("Hostname : " + addr.getHostName()+"\n");
						header.append("OS : " + System.getProperty("os.name") +"("+ System.getProperty("os.arch") + ") version "+System.getProperty("os.version") +"\n");
						header.append("RAM : " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024)), 0, false)) + " Mb" +"\n");
						header.append("Java version : " + System.getProperty("java.version") +"\n");
						header.append("Current directory : " + System.getProperty("user.dir") +"\n");
						header.append("\nComments : \n---\n" + commentArea.getText() +"\n---\n");
					} catch (UnknownHostException ex) {
						Tools.exception(ex);
					}
					header.append("\n"+message+"\n\n");
					List<File> attachments = new ArrayList<File>();
					long availableSize = 20*1024*1024;
					File error = new File("output/error.log");
					if (error.exists() && (error.length() < availableSize)){
						attachments.add(error);			
						availableSize -= error.length();
					}
					File output = new File("output/output.log");
					if (output.exists() && (output.length() < availableSize)){
						attachments.add(output);	
						availableSize -= output.length();
					}
					try{
						if (attachments.isEmpty()){
							sendMail(Highlander.getParameters().getAdminMail(), "Error in Highlander", header.toString()+sb.toString());
						}else{
							sendMail(Highlander.getParameters().getAdminMail(), "Error in Highlander", header.toString()+sb.toString(), attachments);
						}
						JOptionPane.showMessageDialog(new JFrame(), "An email has been sent, thank you.", "Sending error to Highlander administrator",	JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iHighlander,64));
					}catch(Exception ex){
						Tools.exception(ex);
					}					
				}
			}
		});
		panel.add(sendError, BorderLayout.SOUTH);
		panel.setPreferredSize(new Dimension(500,300));
		return panel;
	}

	public static void sendQueryToAdministrator() {
		Set<String> runningSelect = Highlander.getDB().getRunningSelects();
		StringBuilder sb = new StringBuilder();
		for (String stm : runningSelect) {
			sb.append(stm.toString() + "\n");
		}
		JTextArea commentArea = new JTextArea(8,40);
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("Please add as much context as possible to your query.\nYou don't need to describe the filter you used (it's automatically sent),\nbut you can describe how long your are waiting, if it's 'stuck' or still running, if it's the first time you used that filter, etc."), BorderLayout.NORTH);
		p.add(new JScrollPane(commentArea), BorderLayout.CENTER);
		int res = JOptionPane.showConfirmDialog(null, p, "Send this query to Highlander administrator", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey, 64));
		if (res == JOptionPane.OK_OPTION){
			final StringBuilder header = new StringBuilder();
			DateFormat df = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
			header.append("Date : " + df.format(System.currentTimeMillis())+"\n");
			try {
				InetAddress addr = InetAddress.getLocalHost();
				header.append("User : " + ((Highlander.getLoggedUser() != null) ? Highlander.getLoggedUser() : System.getProperty("user.name")) +"\n");
				header.append("Email : " + ((Highlander.getLoggedUser() != null) ? Highlander.getLoggedUser().getEmail() : "user not logged") +"\n");
				header.append("Address : " + addr.getHostAddress() +"\n");
				header.append("Hostname : " + addr.getHostName()+"\n");
				header.append("OS : " + System.getProperty("os.name") +"("+ System.getProperty("os.arch") + ") version "+System.getProperty("os.version") +"\n");
				header.append("RAM : " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024)), 0, false)) + " Mb" +"\n");
				header.append("Java version : " + System.getProperty("java.version") +"\n");
				header.append("Current directory : " + System.getProperty("user.dir") +"\n");
				header.append("\nComments : \n---\n" + commentArea.getText() +"\n---\n");
			} catch (UnknownHostException ex) {
				Tools.exception(ex);
			}
			header.append("\nQueries\n\n");
			List<File> attachments = new ArrayList<File>();
			long availableSize = 20*1024*1024;
			File error = new File("output/error.log");
			if (error.exists() && (error.length() < availableSize)){
				attachments.add(error);			
				availableSize -= error.length();
			}
			File output = new File("output/output.log");
			if (output.exists() && (output.length() < availableSize)){
				attachments.add(output);	
				availableSize -= output.length();
			}
			try{
				if (attachments.isEmpty()){
					sendMail(Highlander.getParameters().getAdminMail(), "Slow query report from Highlander", header.toString()+sb.toString());
				}else{
					sendMail(Highlander.getParameters().getAdminMail(), "Slow query report from Highlander", header.toString()+sb.toString(), attachments);
				}
				JOptionPane.showMessageDialog(new JFrame(), "Your query has been sent to the administrator, thank you.", "Report slow query",	JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iHighlander,64));
			}catch(Exception ex){
				Tools.exception(ex);
			}					
		}
	}
	
	public static void openURL(String url){
		try{
			openURL(new URI(url));
		}catch (Exception ex) {
			exception(ex);
			JOptionPane.showMessageDialog(null, "Cannot open web browser" + ":\n" + ex.getLocalizedMessage(), "Opening web browser",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public static void openURL(String url, String param){
		try{
			String encodedParam = URLEncoder.encode(param,"UTF-8").replaceAll("\\+", "%20");
			openURL(new URI(url+encodedParam));
		}catch (Exception ex) {
			exception(ex);
			JOptionPane.showMessageDialog(null, "Cannot open web browser" + ":\n" + ex.getLocalizedMessage(), "Opening web browser",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public static void openURL(URI url) {
		try {
			java.awt.Desktop.getDesktop().browse(url);
		}
		catch (Exception ex) {
			exception(ex);
			JOptionPane.showMessageDialog(null, "Cannot open web browser" + ":\n" + ex.getLocalizedMessage(), "Opening web browser",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public static String md5Encryption(String input) throws NoSuchAlgorithmException {
		MessageDigest algorithm = MessageDigest.getInstance("MD5");
		byte messageDigest[] = algorithm.digest(input.getBytes());
		BigInteger number = new BigInteger(1,messageDigest);
		return String.format("%1$032X",number);
	}

	public static void sendMail(String recipient, String subject, String text) throws Exception {
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
		message.addRecipient(Message.RecipientType.TO,
				new InternetAddress(recipient));
		message.setContent(text, "text/plain");

		transport.connect();
		transport.sendMessage(message,
				message.getRecipients(Message.RecipientType.TO));
		transport.close();
	}

	public static void sendMail(String recipient, String subject, String text, List<File> attachments) throws Exception {
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
		message.addRecipient(Message.RecipientType.TO,
				new InternetAddress(recipient));

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
				message.getRecipients(Message.RecipientType.TO));
		transport.close();
	}

	public static File getHomeDirectory(){
		return new File(System.getProperty("user.home"));
		//return new JFileChooser().getFileSystemView().getDefaultDirectory();
	}

	public static String formatFilename(String filename){
		return filename.replace('/', '_').replace('\\', '_').replace(':', '_').replace('*', '_').replace('?', '_').replace('>', '_').replace('<', '_').replace('|', '_').replace('"', '_');
	}

	public static String doubleToString (double x, int d, boolean allowSciNot) {
		return doubleToString(x,d,allowSciNot,true);
	}

	public static String doubleToString (double x, int d, boolean allowSciNot, boolean allowGroupSeparator) {
		if (x != 0 && Math.abs(x) < Math.pow(10, -d)){
			if (allowSciNot){
				if (Double.isNaN(x) || Double.isInfinite(x)) {
					return "" + x;
				}
				DecimalFormatSymbols dfs = new DecimalFormatSymbols();
				dfs.setDecimalSeparator('.');
				dfs.setGroupingSeparator(',');
				String card = "";
				for (int i=0 ; i < d ; i++) card += "#";
				NumberFormat formatter = new DecimalFormat("0."+card+"E0", dfs);
				return formatter.format(x);
			}else{
				return "0";
			}
		}else{
			NumberFormat fmt = NumberFormat.getInstance(Locale.US);
			if (fmt instanceof DecimalFormat) { 		 
				DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
				symb.setGroupingSeparator(' ');
				((DecimalFormat) fmt).setDecimalFormatSymbols(symb);
				((DecimalFormat) fmt).setMaximumFractionDigits(d);
				//((DecimalFormat) fmt).setMinimumFractionDigits(d);
				((DecimalFormat) fmt).setGroupingUsed(allowGroupSeparator);
			}
			String s = fmt.format(x);
			return s;
		}
	}

	public static String doubleToPercent(double x, int d) {
		x *= 100;
		NumberFormat fmt = NumberFormat.getInstance(Locale.US);
		if (fmt instanceof DecimalFormat) { 		 
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
			symb.setGroupingSeparator(' ');
			((DecimalFormat) fmt).setDecimalFormatSymbols(symb);
			((DecimalFormat) fmt).setMaximumFractionDigits(d);
			//((DecimalFormat) fmt).setMinimumFractionDigits(d);
			((DecimalFormat) fmt).setGroupingUsed(true);
		}
		String s = fmt.format(x) + "%";		
		return s;		
	}

	public static String intToString (int x) {
		NumberFormat fmt = NumberFormat.getInstance(Locale.US);
		if (fmt instanceof DecimalFormat) { 		 
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
			symb.setGroupingSeparator(' ');
			((DecimalFormat) fmt).setDecimalFormatSymbols(symb);
			((DecimalFormat) fmt).setGroupingUsed(true);
		}
		String s = fmt.format(x);
		return s;
	}

	public static String longToString (long x) {
		NumberFormat fmt = NumberFormat.getInstance(Locale.US);
		if (fmt instanceof DecimalFormat) { 		 
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
			symb.setGroupingSeparator(' ');
			((DecimalFormat) fmt).setDecimalFormatSymbols(symb);
			((DecimalFormat) fmt).setGroupingUsed(true);
		}
		String s = fmt.format(x);
		return s;
	}

	public static int getUsedMemoryInMb(){
		return (int)((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()) / 1024 /1024);
	}

	/**
	 * Return the maximum memory allowable to the JVM in MegaBytes
	 * @return
	 */
	public static long getMaxPhysicalMemory(){
		com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		long max = os.getTotalPhysicalMemorySize() / 1024 /1024;
		//long max = SystemInfo.getPhysicalMemory() / 1024 /1024; --> let's try to avoid using install4j, so it doesn't complain when the system library is absent
		if (Integer.parseInt(System.getProperty( "sun.arch.data.model" )) == 32 && max > 2048 ) max = 2048;
		/* Not necessary if Install4j is not used
		if (IS_MAC && Integer.parseInt(System.getProperty( "sun.arch.data.model" )) == 64){
			//The Install4j getPhysicalMemory() doesn't seems to detect more than 2gb of RAM on 64-bit Mac OS X, so use another way.
			try{
				Process p = Runtime.getRuntime().exec("sysctl hw.memsize") ;
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())) ;
				String line = br.readLine();
				line = line.substring(12).trim();
				max = (Long.valueOf(line)) / 1024 /1024;
			}catch (Exception ex){
				Tools.exception(ex);
			}
		}
		*/
		return max;
	}

	public static String getApplicationDataFolder(){
		String appData = System.getProperty("user.home");
		if(isWindows()){
			//appData = WinFileSystem.getProgramDataDirectory() + "\\Highlander\\"; --> shared folder, everyone can read, write new files, but users can only modify THEIR files
			//appData = WinFileSystem.getSpecialFolder(SpecialFolder.APPDATA, false) + "\\Highlander\\"; --> avoid using install4j, for tools like dbBuilder it needs the DLL ...
			appData = System.getenv("APPDATA") + "\\Highlander\\";
		}else if (isMac()){
			appData = System.getProperty("user.home") + "/Library/Application Support/Highlander/";
		}else{
			appData = System.getProperty("user.home") + "/.Highlander/";
		}
		return appData;
	}

	public static boolean exists(String URLName){
		try {
			if (URLName.contains("ftp://")){
				//TODO LONGTERM - handle proxy
				return FTPUtils.resourceAvailable(new URL(URLName));
			}else{
				Proxy proxy = Proxy.NO_PROXY;
				boolean bypass = false;
				if (System.getProperty("http.nonProxyHosts") != null) {
					for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
						if (URLName.toLowerCase().contains(host.toLowerCase())) bypass = true;
					}
				}
				if (!bypass && System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null) {
					proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort"))));
				}
				HttpURLConnection.setFollowRedirects(false);
				//HttpURLConnection.setInstanceFollowRedirects(false)
				HttpURLConnection con = (HttpURLConnection) new URL(URLName).openConnection(proxy);
				con.setRequestMethod("HEAD");
				return (con.getResponseCode() == HttpURLConnection.HTTP_OK);		
			}
		}catch (Exception e) {
			//e.printStackTrace(); --> do not print, because some tools will throw it and user will be misled to an error
			return false;
		}
	}

	public static Document loadXMLFromString(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		//Ignore DTD downloading (because of Connection refused: connect errors - seems that http://www.biodas.org is down since 2016/04/08)
		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException {
				//System.out.println("Ignoring " + publicId + ", " + systemId);
				return new InputSource(new StringReader(""));
			}
		});
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	public static Document loadJSONFromString(String json) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		//Ignore DTD downloading (because of Connection refused: connect errors - seems that http://www.biodas.org is down since 2016/04/08)
		builder.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException {
				//System.out.println("Ignoring " + publicId + ", " + systemId);
				return new InputSource(new StringReader(""));
			}
		});
		InputSource is = new InputSource(new StringReader(json));
		return builder.parse(is);
	}

	public static String httpGet(String url) {
		String response = "";
		int res = 0;
		int attempts = 0;
		while (res != 200 && attempts < 20) {
			try {
				GetMethod getMethod = new GetMethod(url);
				HttpClient httpClient = new HttpClient();
				boolean bypass = false;
				if (System.getProperty("http.nonProxyHosts") != null) {
					for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
						if (url.toLowerCase().contains(host.toLowerCase())) bypass = true;
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
				res = httpClient.executeMethod(getMethod);
				if(res != 200) {
		      if(res == 429 && getMethod.getRequestHeader("Retry-After") != null) {
		        double sleepFloatingPoint = Double.valueOf(getMethod.getRequestHeader("Retry-After").getValue());
		        double sleepMillis = 1000 * sleepFloatingPoint;
		        try { System.err.println("Web service asking to wait for " + sleepFloatingPoint + " seconds ..."); Thread.sleep((long)sleepMillis); } catch (InterruptedException e) { e.printStackTrace(); }
		      }
		    }else {
		    	response = getMethod.getResponseBodyAsString();
		    }
				getMethod.releaseConnection();
			}
			catch (IOException ex) {
				System.err.println("Exception encountered with http GET of URL '"+url+"'");
				if (attempts > 5) Tools.exception(ex);
			}
			attempts++;
			if (attempts > 1) { try {	System.err.println("Waiting 2 second before retry ..."); Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); } }
			if (attempts > 3) { try {	System.err.println("Waiting 10 second before retry ..."); Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace(); } }
			if (attempts > 5) { try {	System.err.println("Waiting 20 second before retry ..."); Thread.sleep(20000); } catch (InterruptedException e) { e.printStackTrace(); } }
		}
		if (attempts >= 20) System.err.println("ERROR -- Abandon after more than 20 tries with '"+url+"'");
		return response;
	}

	public static void httpDownload(URL url, File output) throws IOException {
		System.out.println("Downloading '" + url + "' to '" + output + "'");
		ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
		try(FileOutputStream fileOutputStream = new FileOutputStream(output)){
			try(FileChannel fileChannel = fileOutputStream.getChannel()){
				fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			}
		}
		System.out.println(output + " download completed");
	}

	public static String reverseComplement(String seq) {
		char[] bases = seq.toCharArray();
		char[] rc = new char[bases.length];
		
		for (int i=0, j=bases.length-1 ; i < bases.length ; i++, j--){
			switch (bases[j]) {
			case 'A':
			case 'a':
				rc[i] = 'T';
				break;
			case 'C':
			case 'c':
				rc[i] = 'G';
				break;
			case 'G':
			case 'g':
				rc[i] = 'C';
				break;
			case 'T':
			case 't':
				rc[i] = 'A';
				break;
			case 'R':
			case 'r':
				rc[i] = 'Y';
				break;
			case 'Y':
			case 'y':
				rc[i] = 'R';
				break;
			case 'K':
			case 'k':
				rc[i] = 'M';
				break;
			case 'M':
			case 'm':
				rc[i] = 'K';
				break;
			case 'S':
			case 's':
				rc[i] = 'S';
				break;
			case 'W':
			case 'w':
				rc[i] = 'W';
				break;
			case 'B':
			case 'b':
				rc[i] = 'V';
				break;
			case 'D':
			case 'd':
				rc[i] = 'H';
				break;
			case 'H':
			case 'h':
				rc[i] = 'D';
				break;
			case 'V':
			case 'v':
				rc[i] = 'B';
				break;
			case 'N':
			case 'n':
			default:
				rc[i] = bases[j]; 
				break;
			}
		}
		return new String(rc);  	
	}

	public static char nucleotidesToProtein(String inputCodon){
		inputCodon = inputCodon.toUpperCase();
		List<Set<String>> possibleCodons = new ArrayList<Set<String>>();
		for (int i=0 ; i < 4 ; i++){
			possibleCodons.add(new HashSet<String>());
		}
		possibleCodons.get(0).add("");
		for (int i=0 ; i < 3 ; i++){
			for (String partialCodon : possibleCodons.get(i)){
				switch(inputCodon.charAt(i)){
				case 'A' :  			
				case 'C' :
				case 'T' :
				case 'G' :
					possibleCodons.get(i+1).add(partialCodon + inputCodon.charAt(i));
					break;
				case 'R' :
					possibleCodons.get(i+1).add(partialCodon + 'A');
					possibleCodons.get(i+1).add(partialCodon + 'G');
					break;
				case 'Y' :
					possibleCodons.get(i+1).add(partialCodon + 'C');
					possibleCodons.get(i+1).add(partialCodon + 'T');
					break;
				case 'K' :
					possibleCodons.get(i+1).add(partialCodon + 'G');
					possibleCodons.get(i+1).add(partialCodon + 'T');
					break;
				case 'M' :
					possibleCodons.get(i+1).add(partialCodon + 'A');
					possibleCodons.get(i+1).add(partialCodon + 'C');
					break;
				case 'S' :
					possibleCodons.get(i+1).add(partialCodon + 'G');
					possibleCodons.get(i+1).add(partialCodon + 'C');
					break;
				case 'W' :
					possibleCodons.get(i+1).add(partialCodon + 'A');
					possibleCodons.get(i+1).add(partialCodon + 'T');
					break;
				case 'B' :
					possibleCodons.get(i+1).add(partialCodon + 'C');
					possibleCodons.get(i+1).add(partialCodon + 'G');
					possibleCodons.get(i+1).add(partialCodon + 'T');
					break;
				case 'D' :
					possibleCodons.get(i+1).add(partialCodon + 'A');
					possibleCodons.get(i+1).add(partialCodon + 'G');
					possibleCodons.get(i+1).add(partialCodon + 'T');
					break;
				case 'H' :
					possibleCodons.get(i+1).add(partialCodon + 'A');
					possibleCodons.get(i+1).add(partialCodon + 'C');
					possibleCodons.get(i+1).add(partialCodon + 'T');
					break;
				case 'V' :
					possibleCodons.get(i+1).add(partialCodon + 'A');
					possibleCodons.get(i+1).add(partialCodon + 'C');
					possibleCodons.get(i+1).add(partialCodon + 'G');
					break;
				case 'N' :
				default :
					possibleCodons.get(i+1).add(partialCodon + 'A');
					possibleCodons.get(i+1).add(partialCodon + 'C');
					possibleCodons.get(i+1).add(partialCodon + 'G');
					possibleCodons.get(i+1).add(partialCodon + 'T');
					break;
				}
			}
		}
		Set<Character> aminoAcids = new HashSet<Character>();
		for (String codon : possibleCodons.get(3)){
			switch (codon.charAt(0)){
			case 'T' :
				switch (codon.charAt(1)){
				case 'T' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
						aminoAcids.add('F');
						break;
					case 'A' :
					case 'G' :
						aminoAcids.add('L');
						break;
					}
					break;
				case 'C' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('S');
						break;
					}
					break;
				case 'A' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
						aminoAcids.add('Y');
						break;
					case 'A' :
					case 'G' :
						aminoAcids.add('*');
						break;
					}
					break;
				case 'G' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
						aminoAcids.add('C');
						break;
					case 'A' :
						aminoAcids.add('*');
						break;
					case 'G' :
						aminoAcids.add('W');
						break;
					}
					break;
				}
				break;
			case 'C' :
				switch (codon.charAt(1)){
				case 'T' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('L');
						break;
					}
					break;
				case 'C' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('P');
						break;
					}
					break;
				case 'A' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
						aminoAcids.add('H');
						break;
					case 'A' :
					case 'G' :
						aminoAcids.add('Q');
						break;
					}
					break;
				case 'G' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('R');
						break;
					}
					break;
				}
				break;
			case 'A' :
				switch (codon.charAt(1)){
				case 'T' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
						aminoAcids.add('I');
						break;
					case 'G' :
						aminoAcids.add('M');
						break;
					}
					break;
				case 'C' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('T');
						break;
					}
					break;
				case 'A' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
						aminoAcids.add('N');
						break;
					case 'A' :
					case 'G' :
						aminoAcids.add('K');
						break;
					}
					break;
				case 'G' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
						aminoAcids.add('S');
						break;
					case 'A' :
					case 'G' :
						aminoAcids.add('R');
						break;
					}
					break;
				}
				break;
			case 'G' :
				switch (codon.charAt(1)){
				case 'T' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('V');
						break;
					}
					break;
				case 'C' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('A');
						break;
					}
					break;
				case 'A' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
						aminoAcids.add('D');
						break;
					case 'A' :
					case 'G' :
						aminoAcids.add('E');
						break;
					}
					break;
				case 'G' :
					switch (codon.charAt(2)){
					case 'T' :
					case 'C' :
					case 'A' :
					case 'G' :
						aminoAcids.add('G');
						break;
					}
					break;
				}
				break;
			}
		}
		if (aminoAcids.size() == 1){
			return aminoAcids.iterator().next();
		}else if (aminoAcids.size() == 2){
			if (aminoAcids.contains('N') && aminoAcids.contains('D')){
				return 'B';
			}else if (aminoAcids.contains('E') && aminoAcids.contains('Q')){
				return 'Z';
			}else if (aminoAcids.contains('I') && aminoAcids.contains('L')){
				return 'J';
			}else{
				return 'X';
			}
		}else{
			return 'X';
		}
	}

	public static String getAminoAcidName(char aa) {
		switch(aa) {
		case 'A':
			return "Alanine";
		case 'R':
			return "Arginine";
		case 'N':
			return "Asparagine";
		case 'D':
			return "Aspartic Acid";
		case 'C':
			return "Cystein";
		case 'E':
			return "Glutamic Acid";
		case 'Q':
			return "Glutamine";
		case 'G':
			return "Glycine";
		case 'H':
			return "Histidine";
		case 'I':
			return "Isoleucine";
		case 'L':
			return "Leucine";
		case 'K':
			return "Lysine";
		case 'M':
			return "Methionine";
		case 'F':
			return "Phenylalanine";
		case 'P':
			return "Proline";
		case 'S':
			return "Serine";
		case 'T':
			return "Threonine";
		case 'W':
			return "Tryptophan";
		case 'Y':
			return "Tyrosine";
		case 'V':
			return "Valine";
		case 'B':
			return "Asparagine or aspartic acid";
		case 'Z':
			return "Glutamine or glutamic acid";
		case 'J':
			return "Leucine or Isoleucine";
		case 'X':
			return "Any amino acid";
		case '*':
			return "STOP";
		default:
			return "";
		}
	}

	public static String getAminoAcid3LetterCode(char aa) {
		switch(aa) {
		case 'A':
			return "Ala";
		case 'R':
			return "Arg";
		case 'N':
			return "Asn";
		case 'D':
			return "Asp";
		case 'C':
			return "Cys";
		case 'E':
			return "Glu";
		case 'Q':
			return "Gln";
		case 'G':
			return "Gly";
		case 'H':
			return "His";
		case 'I':
			return "Ile";
		case 'L':
			return "Leu";
		case 'K':
			return "Lys";
		case 'M':
			return "Met";
		case 'F':
			return "Phe";
		case 'P':
			return "Pro";
		case 'S':
			return "Ser";
		case 'T':
			return "Thr";
		case 'W':
			return "Trp";
		case 'Y':
			return "Tyr";
		case 'V':
			return "Val";
		case 'B':
			return "Asx";
		case 'Z':
			return "Glx";
		case 'J':
			return "Xle";
		case 'X':
			return "Xaa";
		case '*':
			return "Stop";
		default:
			return "";
		}
	}

	public static Color getAminoAcidColor(char aa) {
		switch(aa) {
		case 'D':
		case 'E':
			return new Color(230,10,10);
		case 'C':
		case 'M':
			return new Color(230,230,0);
		case 'K':
		case 'R':
			return new Color(20,90,255);
		case 'S':
		case 'T':
			return new Color(250,150,0);
		case 'F':
		case 'Y':
			return new Color(50,50,170);
		case 'N':
		case 'Q':
			return new Color(0,220,220);
		case 'G':
			return new Color(235,235,235);
		case 'L':
		case 'V':
		case 'I':
			return new Color(15,130,15);
		case 'A':
			return new Color(200,200,200);
		case 'W':
			return new Color(180,90,180);
		case 'H':
			return new Color(130,130,210);
		case 'P':
			return new Color(220,150,130);
		case '*':
			return Color.BLACK;
		default:
			return new Color(160,255,160);
		}
	}

	/**
	 NaturalOrderComparator.java -- Perform 'natural order' comparisons of strings in Java.
	 Copyright (C) 2003 by Pierre-Luc Paour <natorder@paour.com>
	 Based on the C version by Martin Pool, of which this is more or less a straight conversion.
	 Copyright (C) 2000 by Martin Pool <mbp@humbug.org.au>
	 This software is provided 'as-is', without any express or implied
	 warranty.  In no event will the authors be held liable for any damages
	 arising from the use of this software.
	 Permission is granted to anyone to use this software for any purpose,
	 including commercial applications, and to alter it and redistribute it
	 freely, subject to the following restrictions:
	 1. The origin of this software must not be misrepresented; you must not
	 claim that you wrote the original software. If you use this software
	 in a product, an acknowledgment in the product documentation would be
	 appreciated but is not required.
	 2. Altered source versions must be plainly marked as such, and must not be
	 misrepresented as being the original software.
	 3. This notice may not be removed or altered from any source distribution.


	 Raphael Helaers -- added ignoreCase

	 */
	public static class NaturalOrderComparator implements Comparator<String> {

		boolean ignoreCase = false;

		public NaturalOrderComparator(boolean ignoreCase) {
			this.ignoreCase = ignoreCase;
		}

		int compareRight(String a, String b) {
			int bias = 0;
			int ia = 0;
			int ib = 0;

			// The longest run of digits wins. That aside, the greatest
			// value wins, but we can't know that it will until we've scanned
			// both numbers to know that they have the same magnitude, so we
			// remember it in BIAS.
			for (;; ia++, ib++) {
				char ca = charAt(a, ia);
				char cb = charAt(b, ib);

				if (!Character.isDigit(ca) && !Character.isDigit(cb)) {
					return bias;
				} else if (!Character.isDigit(ca)) {
					return -1;
				} else if (!Character.isDigit(cb)) {
					return +1;
				} else if (ca < cb) {
					if (bias == 0) {
						bias = -1;
					}
				} else if (ca > cb) {
					if (bias == 0)
						bias = +1;
				} else if (ca == 0 && cb == 0) {
					return bias;
				}
			}
		}

		public int compare(String a, String b) {
			int ia = 0, ib = 0;
			int nza = 0, nzb = 0;
			char ca, cb;
			int result;

			while (true) {
				// only count the number of zeroes leading the last number compared
				nza = nzb = 0;

				ca = charAt(a, ia);
				cb = charAt(b, ib);

				// skip over leading spaces or zeros
				while (Character.isSpaceChar(ca) || ca == '0') {
					if (ca == '0') {
						nza++;
					} else {
						// only count consecutive zeroes
						nza = 0;
					}

					ca = charAt(a, ++ia);
				}

				while (Character.isSpaceChar(cb) || cb == '0') {
					if (cb == '0') {
						nzb++;
					} else {
						// only count consecutive zeroes
						nzb = 0;
					}

					cb = charAt(b, ++ib);
				}

				// process run of digits
				if (Character.isDigit(ca) && Character.isDigit(cb)) {
					if ((result = compareRight(a.substring(ia), b.substring(ib))) != 0) {
						return result;
					}
				}

				if (ignoreCase) {
					ca = Character.toUpperCase(ca);
					cb = Character.toUpperCase(cb);
				}

				if (ca == 0 && cb == 0) {
					// The strings compare the same. Perhaps the caller
					// will want to call strcmp to break the tie.
					return nza - nzb;
				}

				if (ca < cb) {
					return -1;
				} else if (ca > cb) {
					return +1;
				}

				++ia;
				++ib;
			}
		}

		private char charAt(String s, int i) {
			if (i >= s.length()) {
				return 0;
			} else {
				return s.charAt(i);
			}
		}
	}


	/**
	 * This class is used to hold an image while on the clipboard.
	 *
	 */
	public static class ImageSelection implements Transferable {
		private Image image;

		public ImageSelection(Image image) {
			this.image = image;
		}

		// Returns supported flavors
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}

		// Returns true if flavor is supported
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}

		// Returns image
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (!DataFlavor.imageFlavor.equals(flavor))	{
				throw new UnsupportedFlavorException(flavor);
			}
			return image;
		}
	}

	/** 
	 * This method writes a image to the system clipboard, otherwise it returns null. 
	 */
	public static void setClipboard(Image image) {
		ImageSelection imgSel = new ImageSelection(image);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
	}


	/** 
	 * This method writes a string to the system clipboard, otherwise it returns null. 
	 */
	public static void setClipboard(String string) {
		StringSelection stringSel = new StringSelection(string);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSel, stringSel);
	}
	
	
	/*
	 * Replaced by DBUtils.getSequence() that uses Ensembl local database instead of UCSC DAS (no more internet connection needed)
	 * Don't delete it yet, could be used to validate getSequence() in DBUtils main()
	 * 
	public static String[] getReferenceSequenceUCSC(String genome, String[] chr, int[] start, int[] end){
		String[] response = new String[chr.length];
		StringBuilder query = new StringBuilder();
		query.append("http://genome.ucsc.edu/cgi-bin/das/");
		query.append(genome);	 //when handling other genomes than hg19, check for synomyms (like b37 should become hg19 here)
		query.append("/dna?");
		for (int i=0 ; i< chr.length ; i++) {
			query.append("segment=chr"+chr[i]+":"+start[i]+","+end[i]);
			if (i < chr.length-1) query.append(";");
		}
		try{
			String result = Tools.httpGet(query.toString());
			Document doc = Tools.loadXMLFromString(result);
			NodeList dasdna = doc.getElementsByTagName("DASDNA");
			if(dasdna != null && dasdna.getLength() > 0){
				NodeList sequence = ((Element)dasdna.item(0)).getElementsByTagName("SEQUENCE");
				if(sequence != null && sequence.getLength() > 0){
					for (int i=0 ; i < sequence.getLength() ; i++) {
						NodeList dna = ((Element)sequence.item(i)).getElementsByTagName("DNA");
						if(dna != null && dna.getLength() > 0){
							response[i] = ((Element)dna.item(0)).getTextContent().replace("\n", "").toUpperCase();
						}
					}
				}
			}
		}catch (Exception ex){
			Tools.exception(ex);
		}
		return response;
	}
	 */
}
