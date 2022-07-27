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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.html.HTMLEditorKit;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;

public class SearchField extends JPanel {

	private JTextField filterField = new JTextField();
	private JToggleButton btnRegExp;
	private TableRowSorter<DefaultTableModel> sorter;
	
	public SearchField(int size){
		setLayout(new BorderLayout());
		filterField = new JTextField();
		filterField.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent arg0) {
				keyListener(arg0);
			}
			public void keyTyped(KeyEvent arg0) {			}
			public void keyPressed(KeyEvent arg0) {			}
		});
		add(filterField, BorderLayout.CENTER);
		filterField.setColumns(size);
		
		JButton btnClear = new JButton(Resources.getScaledIcon(Resources.iCross, 16));
		btnClear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				filterField.setText("");
				applyFilter();
			}
		});		
		add(btnClear, BorderLayout.EAST);

		JPanel regexpPanel = new JPanel(new BorderLayout());
		add(regexpPanel, BorderLayout.WEST);

		btnRegExp = new JToggleButton("<html><div style=\"font-family:geneva; font-size:9px; color:#3104B4 \"><b>(.*)</b></div></html>");
		btnRegExp.setToolTipText("Activate to allow usage of regular expressions in the search field");
		btnRegExp.setSelected(false);
		btnRegExp.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				applyFilter();
			}
		});
		regexpPanel.add(btnRegExp, BorderLayout.WEST);
		
		JButton btnHelp = new JButton(Resources.getScaledIcon(Resources.iHelp, 16));
		btnHelp.setPreferredSize(new Dimension(28, 28));
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
		    JFrame dlg = new JFrame();
		    dlg.setTitle("Regular expressions");
		    dlg.setIconImage(Resources.getScaledIcon(Resources.iRegExp, 64).getImage());
		    JScrollPane scrollPane = new JScrollPane();
		    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
				scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);	
				JTextPane startTxt = new JTextPane();
				startTxt.setEditorKit(new HTMLEditorKit());
				startTxt.setOpaque(true);
				startTxt.setText(getHelpString());
				startTxt.setCaretPosition(0);
				startTxt.setEditable(false);
				scrollPane.setViewportView(startTxt);	
				dlg.getContentPane().add(scrollPane, BorderLayout.CENTER);
		    dlg.pack();
		    Tools.centerWindow(dlg, false);
		    dlg.setExtendedState(Highlander.MAXIMIZED_BOTH);
		    dlg.setVisible(true);
			}
		});		
		regexpPanel.add(btnHelp, BorderLayout.EAST);
}
	
	public SearchField(int size, TableRowSorter<DefaultTableModel> sorter){
		this(size);
		setSorter(sorter);
	}
	
	public void setSorter(TableRowSorter<DefaultTableModel> sorter){
		this.sorter = sorter;				
	}
	
	protected void keyListener(KeyEvent key){
		applyFilter();
	}
	
	public String getText(){
  	if (btnRegExp.isSelected()) {
  		return filterField.getText(); 
  	}else{
  		return formatForRegularExpressionSearch(filterField.getText());
  	}
	}
	
	protected String getTyppedText(){
		return filterField.getText(); 
	}
	
	public void addFieldListener(KeyListener listener){
		filterField.addKeyListener(listener);
	}
	
	public void applyFilter(){
    RowFilter<DefaultTableModel, Object> rf = null;
    //If current expression doesn't parse, don't update.
    try {
        rf = RowFilter.regexFilter("(?i)"+getText());
    } catch (java.util.regex.PatternSyntaxException e) {
        return;
    }
    sorter.setRowFilter(rf);
	}

	public static String formatForRegularExpressionSearch(String string){
		return string.replace(".", "\\.")
				.replace("[", "\\[")
				.replace("]", "\\]")
				.replace("^", "\\^")
				.replace("-", "\\-")
				.replace("&", "\\&")
				.replace("{", "\\{")
				.replace("}", "\\}")
				.replace("$", "\\$")
				.replace("?", "\\?")
				.replace("*", "\\*")
				.replace("+", "\\+")
				.replace("|", "\\|")
				.replace("(", "\\(")
				.replace(")", "\\)")
				.replace(":", "\\:")
				.replace("=", "\\=")
				.replace("!", "\\!")
				.replace("<", "\\<")
				.replace(">", "\\>")
				;
	}
	
	private String getHelpString(){
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<div class=\"block\">A compiled representation of a regular expression.");
		sb.append("");
		sb.append(" <a name=\"sum\">");
		sb.append(" </a></p><h4><a name=\"sum\"> Summary of regular-expression constructs </a></h4><a name=\"sum\">");
		sb.append("");
		sb.append(" ");
		sb.append(" * <table border=\"0\" cellpadding=\"1\" cellspacing=\"0\" summary=\"Regular expression constructs, and what they match\">");
		sb.append("");
		sb.append(" <tbody><tr align=\"left\">");
		sb.append(" <th bgcolor=\"#CCCCFF\" align=\"left\" id=\"construct\">Construct</th>");
		sb.append(" <th bgcolor=\"#CCCCFF\" align=\"left\" id=\"matches\">Matches</th>");
		sb.append(" </tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"characters\">Characters</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><i>x</i></td>");
		sb.append("     <td headers=\"matches\">The character <i>x</i></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\\\</tt></td>");
		sb.append("     <td headers=\"matches\">The backslash character</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\0</tt><i>n</i></td>");
		sb.append("     <td headers=\"matches\">The character with octal value <tt>0</tt><i>n</i>");
		sb.append("         (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\0</tt><i>nn</i></td>");
		sb.append("     <td headers=\"matches\">The character with octal value <tt>0</tt><i>nn</i>");
		sb.append("         (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\0</tt><i>mnn</i></td>");
		sb.append("     <td headers=\"matches\">The character with octal value <tt>0</tt><i>mnn</i>");
		sb.append("         (0&nbsp;<tt>&lt;=</tt>&nbsp;<i>m</i>&nbsp;<tt>&lt;=</tt>&nbsp;3,");
		sb.append("         0&nbsp;<tt>&lt;=</tt>&nbsp;<i>n</i>&nbsp;<tt>&lt;=</tt>&nbsp;7)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\x</tt><i>hh</i></td>");
		sb.append("     <td headers=\"matches\">The character with hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>hh</i></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\u</tt><i>hhhh</i></td>");
		sb.append("     <td headers=\"matches\">The character with hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>hhhh</i></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\x</tt><i>{h...h}</i></td>");
		sb.append("     <td headers=\"matches\">The character with hexadecimal&nbsp;value&nbsp;<tt>0x</tt><i>h...h</i>");
		sb.append("         (<a href=\"https://docs.oracle.com/javase/7/docs/api/java/lang/Character.html#MIN_CODE_POINT\"><code>Character.MIN_CODE_POINT</code></a>");
		sb.append("         &nbsp;&lt;=&nbsp;<tt>0x</tt><i>h...h</i>&nbsp;&lt;=&nbsp;");
		sb.append("          <a href=\"https://docs.oracle.com/javase/7/docs/api/java/lang/Character.html#MAX_CODE_POINT\"><code>Character.MAX_CODE_POINT</code></a>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"matches\"><tt>\\t</tt></td>");
		sb.append("     <td headers=\"matches\">The tab character (<tt>'\\u0009'</tt>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\n</tt></td>");
		sb.append("     <td headers=\"matches\">The newline (line feed) character (<tt>'\\u000A'</tt>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\r</tt></td>");
		sb.append("     <td headers=\"matches\">The carriage-return character (<tt>'\\u000D'</tt>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\f</tt></td>");
		sb.append("     <td headers=\"matches\">The form-feed character (<tt>'\\u000C'</tt>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\a</tt></td>");
		sb.append("     <td headers=\"matches\">The alert (bell) character (<tt>'\\u0007'</tt>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\e</tt></td>");
		sb.append("     <td headers=\"matches\">The escape character (<tt>'\\u001B'</tt>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct characters\"><tt>\\c</tt><i>x</i></td>");
		sb.append("     <td headers=\"matches\">The control character corresponding to <i>x</i></td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"classes\">Character classes</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct classes\"><tt>[abc]</tt></td>");
		sb.append("     <td headers=\"matches\"><tt>a</tt>, <tt>b</tt>, or <tt>c</tt> (simple class)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct classes\"><tt>[^abc]</tt></td>");
		sb.append("     <td headers=\"matches\">Any character except <tt>a</tt>, <tt>b</tt>, or <tt>c</tt> (negation)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-zA-Z]</tt></td>");
		sb.append("     <td headers=\"matches\"><tt>a</tt> through <tt>z</tt>");
		sb.append("         or <tt>A</tt> through <tt>Z</tt>, inclusive (range)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-d[m-p]]</tt></td>");
		sb.append("     <td headers=\"matches\"><tt>a</tt> through <tt>d</tt>,");
		sb.append("      or <tt>m</tt> through <tt>p</tt>: <tt>[a-dm-p]</tt> (union)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-z&amp;&amp;[def]]</tt></td>");
		sb.append("     <td headers=\"matches\"><tt>d</tt>, <tt>e</tt>, or <tt>f</tt> (intersection)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-z&amp;&amp;[^bc]]</tt></td>");
		sb.append("     <td headers=\"matches\"><tt>a</tt> through <tt>z</tt>,");
		sb.append("         except for <tt>b</tt> and <tt>c</tt>: <tt>[ad-z]</tt> (subtraction)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct classes\"><tt>[a-z&amp;&amp;[^m-p]]</tt></td>");
		sb.append("     <td headers=\"matches\"><tt>a</tt> through <tt>z</tt>,");
		sb.append("          and not <tt>m</tt> through <tt>p</tt>: <tt>[a-lq-z]</tt>(subtraction)</td></tr>");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append("");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"predef\">Predefined character classes</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct predef\"><tt>.</tt></td>");
		sb.append("     <td headers=\"matches\">Any character (may or may not match <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#lt\">line terminators</a>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\d</tt></td>");
		sb.append("     <td headers=\"matches\">A digit: <tt>[0-9]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\D</tt></td>");
		sb.append("     <td headers=\"matches\">A non-digit: <tt>[^0-9]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\s</tt></td>");
		sb.append("     <td headers=\"matches\">A whitespace character: <tt>[ \\t\\n\\x0B\\f\\r]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\S</tt></td>");
		sb.append("     <td headers=\"matches\">A non-whitespace character: <tt>[^\\s]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\w</tt></td>");
		sb.append("     <td headers=\"matches\">A word character: <tt>[a-zA-Z_0-9]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct predef\"><tt>\\W</tt></td>");
		sb.append("     <td headers=\"matches\">A non-word character: <tt>[^\\w]</tt></td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"posix\">POSIX character classes (US-ASCII only)<b></b></th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Lower}</tt></td>");
		sb.append("     <td headers=\"matches\">A lower-case alphabetic character: <tt>[a-z]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Upper}</tt></td>");
		sb.append("     <td headers=\"matches\">An upper-case alphabetic character:<tt>[A-Z]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{ASCII}</tt></td>");
		sb.append("     <td headers=\"matches\">All ASCII:<tt>[\\x00-\\x7F]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Alpha}</tt></td>");
		sb.append("     <td headers=\"matches\">An alphabetic character:<tt>[\\p{Lower}\\p{Upper}]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Digit}</tt></td>");
		sb.append("     <td headers=\"matches\">A decimal digit: <tt>[0-9]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Alnum}</tt></td>");
		sb.append("     <td headers=\"matches\">An alphanumeric character:<tt>[\\p{Alpha}\\p{Digit}]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Punct}</tt></td>");
		sb.append("     <td headers=\"matches\">Punctuation: One of <tt>!\"#$%&amp;'()*+,-./:;&lt;=&gt;?@[\\]^_`{|}~</tt></td></tr>");
		sb.append("     <!-- <tt>[\\!\"#\\$%&'\\(\\)\\*\\+,\\-\\./:;\\<=\\>\\?@\\[\\\\\\]\\^_`\\{\\|\\}~]</tt>");
		sb.append("          <tt>[\\X21-\\X2F\\X31-\\X40\\X5B-\\X60\\X7B-\\X7E]</tt> -->");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Graph}</tt></td>");
		sb.append("     <td headers=\"matches\">A visible character: <tt>[\\p{Alnum}\\p{Punct}]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Print}</tt></td>");
		sb.append("     <td headers=\"matches\">A printable character: <tt>[\\p{Graph}\\x20]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Blank}</tt></td>");
		sb.append("     <td headers=\"matches\">A space or a tab: <tt>[ \\t]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Cntrl}</tt></td>");
		sb.append("     <td headers=\"matches\">A control character: <tt>[\\x00-\\x1F\\x7F]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{XDigit}</tt></td>");
		sb.append("     <td headers=\"matches\">A hexadecimal digit: <tt>[0-9a-fA-F]</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct posix\"><tt>\\p{Space}</tt></td>");
		sb.append("     <td headers=\"matches\">A whitespace character: <tt>[ \\t\\n\\x0B\\f\\r]</tt></td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\">java.lang.Character classes (simple <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#jcc\">java character type</a>)</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\"><tt>\\p{javaLowerCase}</tt></td>");
		sb.append("     <td>Equivalent to java.lang.Character.isLowerCase()</td></tr>");
		sb.append(" <tr><td valign=\"top\"><tt>\\p{javaUpperCase}</tt></td>");
		sb.append("     <td>Equivalent to java.lang.Character.isUpperCase()</td></tr>");
		sb.append(" <tr><td valign=\"top\"><tt>\\p{javaWhitespace}</tt></td>");
		sb.append("     <td>Equivalent to java.lang.Character.isWhitespace()</td></tr>");
		sb.append(" <tr><td valign=\"top\"><tt>\\p{javaMirrored}</tt></td>");
		sb.append("     <td>Equivalent to java.lang.Character.isMirrored()</td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"unicode\">Classes for Unicode scripts, blocks, categories and binary properties</th></tr><tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{IsLatin}</tt></td>");
		sb.append("     <td headers=\"matches\">A Latin&nbsp;script character (<a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#usc\">script</a>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{InGreek}</tt></td>");
		sb.append("     <td headers=\"matches\">A character in the Greek&nbsp;block (<a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#ubc\">block</a>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{Lu}</tt></td>");
		sb.append("     <td headers=\"matches\">An uppercase letter (<a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#ucc\">category</a>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{IsAlphabetic}</tt></td>");
		sb.append("     <td headers=\"matches\">An alphabetic character (<a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#ubpc\">binary property</a>)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\p{Sc}</tt></td>");
		sb.append("     <td headers=\"matches\">A currency symbol</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct unicode\"><tt>\\P{InGreek}</tt></td>");
		sb.append("     <td headers=\"matches\">Any character except one in the Greek block (negation)</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct unicode\"><tt>[\\p{L}&amp;&amp;[^\\p{Lu}]]&nbsp;</tt></td>");
		sb.append("     <td headers=\"matches\">Any letter except an uppercase letter (subtraction)</td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"bounds\">Boundary matchers</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>^</tt></td>");
		sb.append("     <td headers=\"matches\">The beginning of a line</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>$</tt></td>");
		sb.append("     <td headers=\"matches\">The end of a line</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\b</tt></td>");
		sb.append("     <td headers=\"matches\">A word boundary</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\B</tt></td>");
		sb.append("     <td headers=\"matches\">A non-word boundary</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\A</tt></td>");
		sb.append("     <td headers=\"matches\">The beginning of the input</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\G</tt></td>");
		sb.append("     <td headers=\"matches\">The end of the previous match</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\Z</tt></td>");
		sb.append("     <td headers=\"matches\">The end of the input but for the final");
		sb.append("         <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#lt\">terminator</a>, if&nbsp;any</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct bounds\"><tt>\\z</tt></td>");
		sb.append("     <td headers=\"matches\">The end of the input</td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"greedy\">Greedy quantifiers</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>?</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, once or not at all</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>*</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, zero or more times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>+</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, one or more times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>{</tt><i>n</i><tt>}</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, exactly <i>n</i> times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>{</tt><i>n</i><tt>,}</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, at least <i>n</i> times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct greedy\"><i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"reluc\">Reluctant quantifiers</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>??</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, once or not at all</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>*?</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, zero or more times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>+?</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, one or more times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>{</tt><i>n</i><tt>}?</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, exactly <i>n</i> times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>{</tt><i>n</i><tt>,}?</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, at least <i>n</i> times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct reluc\"><i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}?</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"poss\">Possessive quantifiers</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>?+</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, once or not at all</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>*+</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, zero or more times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>++</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, one or more times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>{</tt><i>n</i><tt>}+</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, exactly <i>n</i> times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>{</tt><i>n</i><tt>,}+</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, at least <i>n</i> times</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct poss\"><i>X</i><tt>{</tt><i>n</i><tt>,</tt><i>m</i><tt>}+</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, at least <i>n</i> but not more than <i>m</i> times</td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"logical\">Logical operators</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct logical\"><i>XY</i></td>");
		sb.append("     <td headers=\"matches\"><i>X</i> followed by <i>Y</i></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct logical\"><i>X</i><tt>|</tt><i>Y</i></td>");
		sb.append("     <td headers=\"matches\">Either <i>X</i> or <i>Y</i></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct logical\"><tt>(</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\">X, as a <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#cg\">capturing group</a></td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"backref\">Back references</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"bottom\" headers=\"construct backref\"><tt>\\</tt><i>n</i></td>");
		sb.append("     <td valign=\"bottom\" headers=\"matches\">Whatever the <i>n</i><sup>th</sup>");
		sb.append("     <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#cg\">capturing group</a> matched</td></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"bottom\" headers=\"construct backref\"><tt>\\</tt><i>k</i>&lt;<i>name</i>&gt;</td>");
		sb.append("     <td valign=\"bottom\" headers=\"matches\">Whatever the");
		sb.append("     <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#groupname\">named-capturing group</a> \"name\" matched</td></tr>");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"quot\">Quotation</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct quot\"><tt>\\</tt></td>");
		sb.append("     <td headers=\"matches\">Nothing, but quotes the following character</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct quot\"><tt>\\Q</tt></td>");
		sb.append("     <td headers=\"matches\">Nothing, but quotes all characters until <tt>\\E</tt></td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct quot\"><tt>\\E</tt></td>");
		sb.append("     <td headers=\"matches\">Nothing, but ends quoting started by <tt>\\Q</tt></td></tr>");
		sb.append("     <!-- Metachars: !$()*+.<>?[\\]^{|} -->");
		sb.append("");
		sb.append(" <tr><th>&nbsp;</th></tr>");
		sb.append(" <tr align=\"left\"><th colspan=\"2\" id=\"special\">Special constructs (named-capturing and non-capturing)</th></tr>");
		sb.append("");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?&lt;<a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#groupname\">name</a>&gt;</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, as a named-capturing group</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?:</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, as a non-capturing group</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?idmsuxU-idmsuxU)&nbsp;</tt></td>");
		sb.append("     <td headers=\"matches\">Nothing, but turns match flags <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#CASE_INSENSITIVE\">i</a>");
		sb.append(" <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#UNIX_LINES\">d</a> <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#MULTILINE\">m</a> <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#DOTALL\">s</a>");
		sb.append(" <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#UNICODE_CASE\">u</a> <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#COMMENTS\">x</a> <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#UNICODE_CHARACTER_CLASS\">U</a>");
		sb.append(" on - off</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?idmsux-idmsux:</tt><i>X</i><tt>)</tt>&nbsp;&nbsp;</td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, as a <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#cg\">non-capturing group</a> with the");
		sb.append("         given flags <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#CASE_INSENSITIVE\">i</a> <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#UNIX_LINES\">d</a>");
		sb.append(" <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#MULTILINE\">m</a> <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#DOTALL\">s</a> <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#UNICODE_CASE\">u</a>");
		sb.append(" <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#COMMENTS\">x</a> on - off</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?=</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, via zero-width positive lookahead</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?!</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, via zero-width negative lookahead</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?&lt;=</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, via zero-width positive lookbehind</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?&lt;!</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, via zero-width negative lookbehind</td></tr>");
		sb.append(" <tr><td valign=\"top\" headers=\"construct special\"><tt>(?&gt;</tt><i>X</i><tt>)</tt></td>");
		sb.append("     <td headers=\"matches\"><i>X</i>, as an independent, non-capturing group</td></tr>");
		sb.append("");
		sb.append(" </tbody></table>");
		sb.append("");
		sb.append(" <hr>");
		sb.append("");
		sb.append("");
		sb.append(" </a><a name=\"bs\">");
		sb.append(" <h4> Backslashes, escapes, and quoting </h4>");
		sb.append("");
		sb.append(" <p> The backslash character (<tt>'\\'</tt>) serves to introduce escaped");
		sb.append(" constructs, as defined in the table above, as well as to quote characters");
		sb.append(" that otherwise would be interpreted as unescaped constructs.  Thus the");
		sb.append(" expression <tt>\\\\</tt> matches a single backslash and <tt>\\{</tt> matches a");
		sb.append(" left brace.");
		sb.append("");
		sb.append(" </p><p> It is an error to use a backslash prior to any alphabetic character that");
		sb.append(" does not denote an escaped construct; these are reserved for future");
		sb.append(" extensions to the regular-expression language.  A backslash may be used");
		sb.append(" prior to a non-alphabetic character regardless of whether that character is");
		sb.append(" part of an unescaped construct.");
		sb.append("");
		sb.append(" </p></a><p><a name=\"bs\"> Backslashes within string literals in Java source code are interpreted");
		sb.append(" as required by");
		sb.append(" <cite>The Java™ Language Specification</cite>");
		sb.append(" as either Unicode escapes (section 3.3) or other character escapes (section 3.10.6)");
		sb.append(" It is therefore necessary to double backslashes in string");
		sb.append(" literals that represent regular expressions to protect them from");
		sb.append(" interpretation by the Java bytecode compiler.  The string literal");
		sb.append(" <tt>\"\\b\"</tt>, for example, matches a single backspace character when");
		sb.append(" interpreted as a regular expression, while <tt>\"\\\\b\"</tt> matches a");
		sb.append(" word boundary.  The string literal <tt>\"\\(hello\\)\"</tt> is illegal");
		sb.append(" and leads to a compile-time error; in order to match the string");
		sb.append(" <tt>(hello)</tt> the string literal <tt>\"\\\\(hello\\\\)\"</tt>");
		sb.append(" must be used.");
		sb.append("");
		sb.append(" </a><a name=\"cc\">");
		sb.append(" </a></p><h4><a name=\"cc\"> Character Classes </a></h4><a name=\"cc\">");
		sb.append("");
		sb.append("    <p> Character classes may appear within other character classes, and");
		sb.append("    may be composed by the union operator (implicit) and the intersection");
		sb.append("    operator (<tt>&amp;&amp;</tt>).");
		sb.append("    The union operator denotes a class that contains every character that is");
		sb.append("    in at least one of its operand classes.  The intersection operator");
		sb.append("    denotes a class that contains every character that is in both of its");
		sb.append("    operand classes.");
		sb.append("");
		sb.append("    </p><p> The precedence of character-class operators is as follows, from");
		sb.append("    highest to lowest:");
		sb.append("");
		sb.append("    </p><blockquote><table border=\"0\" cellpadding=\"1\" cellspacing=\"0\" summary=\"Precedence of character class operators.\">");
		sb.append("      <tbody><tr><th>1&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("        <td>Literal escape&nbsp;&nbsp;&nbsp;&nbsp;</td>");
		sb.append("        <td><tt>\\x</tt></td></tr>");
		sb.append("     <tr><th>2&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("        <td>Grouping</td>");
		sb.append("        <td><tt>[...]</tt></td></tr>");
		sb.append("     <tr><th>3&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("        <td>Range</td>");
		sb.append("        <td><tt>a-z</tt></td></tr>");
		sb.append("      <tr><th>4&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("        <td>Union</td>");
		sb.append("        <td><tt>[a-e][i-u]</tt></td></tr>");
		sb.append("      <tr><th>5&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("        <td>Intersection</td>");
		sb.append("        <td><tt>[a-z&amp;&amp;[aeiou]]</tt></td></tr>");
		sb.append("    </tbody></table></blockquote>");
		sb.append("");
		sb.append("    </a><p><a name=\"cc\"> Note that a different set of metacharacters are in effect inside");
		sb.append("    a character class than outside a character class. For instance, the");
		sb.append("    regular expression <tt>.</tt> loses its special meaning inside a");
		sb.append("    character class, while the expression <tt>-</tt> becomes a range");
		sb.append("    forming metacharacter.");
		sb.append("");
		sb.append(" </a><a name=\"lt\">");
		sb.append(" </a></p><h4><a name=\"lt\"> Line terminators </a></h4><a name=\"lt\">");
		sb.append("");
		sb.append(" <p> A <i>line terminator</i> is a one- or two-character sequence that marks");
		sb.append(" the end of a line of the input character sequence.  The following are");
		sb.append(" recognized as line terminators:");
		sb.append("");
		sb.append(" </p><ul>");
		sb.append("");
		sb.append("   <li> A newline (line feed) character&nbsp;(<tt>'\\n'</tt>),");
		sb.append("");
		sb.append("   </li><li> A carriage-return character followed immediately by a newline");
		sb.append("   character&nbsp;(<tt>\"\\r\\n\"</tt>),");
		sb.append("");
		sb.append("   </li><li> A standalone carriage-return character&nbsp;(<tt>'\\r'</tt>),");
		sb.append("");
		sb.append("   </li><li> A next-line character&nbsp;(<tt>'\\u0085'</tt>),");
		sb.append("");
		sb.append("   </li><li> A line-separator character&nbsp;(<tt>'\\u2028'</tt>), or");
		sb.append("");
		sb.append("   </li><li> A paragraph-separator character&nbsp;(<tt>'\\u2029</tt>).");
		sb.append("");
		sb.append(" </li></ul>");
		sb.append(" </a><p><a name=\"lt\">If </a><a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#UNIX_LINES\"><code>UNIX_LINES</code></a> mode is activated, then the only line terminators");
		sb.append(" recognized are newline characters.");
		sb.append("");
		sb.append(" </p><p> The regular expression <tt>.</tt> matches any character except a line");
		sb.append(" terminator unless the <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#DOTALL\"><code>DOTALL</code></a> flag is specified.");
		sb.append("");
		sb.append(" </p><p> By default, the regular expressions <tt>^</tt> and <tt>$</tt> ignore");
		sb.append(" line terminators and only match at the beginning and the end, respectively,");
		sb.append(" of the entire input sequence. If <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#MULTILINE\"><code>MULTILINE</code></a> mode is activated then");
		sb.append(" <tt>^</tt> matches at the beginning of input and after any line terminator");
		sb.append(" except at the end of input. When in <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#MULTILINE\"><code>MULTILINE</code></a> mode <tt>$</tt>");
		sb.append(" matches just before a line terminator or the end of the input sequence.");
		sb.append("");
		sb.append(" <a name=\"cg\">");
		sb.append(" </a></p><h4><a name=\"cg\"> Groups and capturing </a></h4><a name=\"cg\">");
		sb.append("");
		sb.append(" </a><a name=\"gnumber\">");
		sb.append(" <h5> Group number </h5>");
		sb.append(" <p> Capturing groups are numbered by counting their opening parentheses from");
		sb.append(" left to right.  In the expression <tt>((A)(B(C)))</tt>, for example, there");
		sb.append(" are four such groups: </p>");
		sb.append("");
		sb.append(" <blockquote><table cellpadding=\"1\" cellspacing=\"0\" summary=\"Capturing group numberings\">");
		sb.append(" <tbody><tr><th>1&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("     <td><tt>((A)(B(C)))</tt></td></tr>");
		sb.append(" <tr><th>2&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("     <td><tt>(A)</tt></td></tr>");
		sb.append(" <tr><th>3&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("     <td><tt>(B(C))</tt></td></tr>");
		sb.append(" <tr><th>4&nbsp;&nbsp;&nbsp;&nbsp;</th>");
		sb.append("     <td><tt>(C)</tt></td></tr>");
		sb.append(" </tbody></table></blockquote>");
		sb.append("");
		sb.append(" <p> Group zero always stands for the entire expression.");
		sb.append("");
		sb.append(" </p></a><p><a name=\"gnumber\"> Capturing groups are so named because, during a match, each subsequence");
		sb.append(" of the input sequence that matches such a group is saved.  The captured");
		sb.append(" subsequence may be used later in the expression, via a back reference, and");
		sb.append(" may also be retrieved from the matcher once the match operation is complete.");
		sb.append("");
		sb.append(" </a><a name=\"groupname\">");
		sb.append(" </a></p><h5><a name=\"groupname\"> Group name </a></h5><a name=\"groupname\">");
		sb.append(" <p>A capturing group can also be assigned a \"name\", a <tt>named-capturing group</tt>,");
		sb.append(" and then be back-referenced later by the \"name\". Group names are composed of");
		sb.append(" the following characters. The first character must be a <tt>letter</tt>.");
		sb.append("");
		sb.append(" </p><ul>");
		sb.append("   <li> The uppercase letters <tt>'A'</tt> through <tt>'Z'</tt>");
		sb.append("        (<tt>'\\u0041'</tt>&nbsp;through&nbsp;<tt>'\\u005a'</tt>),");
		sb.append("   </li><li> The lowercase letters <tt>'a'</tt> through <tt>'z'</tt>");
		sb.append("        (<tt>'\\u0061'</tt>&nbsp;through&nbsp;<tt>'\\u007a'</tt>),");
		sb.append("   </li><li> The digits <tt>'0'</tt> through <tt>'9'</tt>");
		sb.append("        (<tt>'\\u0030'</tt>&nbsp;through&nbsp;<tt>'\\u0039'</tt>),");
		sb.append(" </li></ul>");
		sb.append("");
		sb.append(" </a><p><a name=\"groupname\"> A <tt>named-capturing group</tt> is still numbered as described in");
		sb.append(" </a><a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#gnumber\">Group number</a>.");
		sb.append("");
		sb.append(" </p><p> The captured input associated with a group is always the subsequence");
		sb.append(" that the group most recently matched.  If a group is evaluated a second time");
		sb.append(" because of quantification then its previously-captured value, if any, will");
		sb.append(" be retained if the second evaluation fails.  Matching the string");
		sb.append(" <tt>\"aba\"</tt> against the expression <tt>(a(b)?)+</tt>, for example, leaves");
		sb.append(" group two set to <tt>\"b\"</tt>.  All captured input is discarded at the");
		sb.append(" beginning of each match.");
		sb.append("");
		sb.append(" </p><p> Groups beginning with <tt>(?</tt> are either pure, <i>non-capturing</i> groups");
		sb.append(" that do not capture text and do not count towards the group total, or");
		sb.append(" <i>named-capturing</i> group.");
		sb.append("");
		sb.append(" </p><h4> Unicode support </h4>");
		sb.append("");
		sb.append(" <p> This class is in conformance with Level 1 of <a href=\"http://www.unicode.org/reports/tr18/\"><i>Unicode Technical");
		sb.append(" Standard #18: Unicode Regular Expression</i></a>, plus RL2.1");
		sb.append(" Canonical Equivalents.");
		sb.append(" </p><p>");
		sb.append(" <b>Unicode escape sequences</b> such as <tt>\\u2014</tt> in Java source code");
		sb.append(" are processed as described in section 3.3 of");
		sb.append(" <cite>The Java™ Language Specification</cite>.");
		sb.append(" Such escape sequences are also implemented directly by the regular-expression");
		sb.append(" parser so that Unicode escapes can be used in expressions that are read from");
		sb.append(" files or from the keyboard.  Thus the strings <tt>\"\\u2014\"</tt> and");
		sb.append(" <tt>\"\\\\u2014\"</tt>, while not equal, compile into the same pattern, which");
		sb.append(" matches the character with hexadecimal value <tt>0x2014</tt>.");
		sb.append(" </p><p>");
		sb.append(" A Unicode character can also be represented in a regular-expression by");
		sb.append(" using its <b>Hex notation</b>(hexadecimal code point value) directly as described in construct");
		sb.append(" <tt>\\x{...}</tt>, for example a supplementary character U+2011F");
		sb.append(" can be specified as <tt>\\x{2011F}</tt>, instead of two consecutive");
		sb.append(" Unicode escape sequences of the surrogate pair");
		sb.append(" <tt>\\uD840</tt><tt>\\uDD1F</tt>.");
		sb.append(" </p><p>");
		sb.append(" Unicode scripts, blocks, categories and binary properties are written with");
		sb.append(" the <tt>\\p</tt> and <tt>\\P</tt> constructs as in Perl.");
		sb.append(" <tt>\\p{</tt><i>prop</i><tt>}</tt> matches if");
		sb.append(" the input has the property <i>prop</i>, while <tt>\\P{</tt><i>prop</i><tt>}</tt>");
		sb.append(" does not match if the input has that property.");
		sb.append(" </p><p>");
		sb.append(" Scripts, blocks, categories and binary properties can be used both inside");
		sb.append(" and outside of a character class.");
		sb.append(" <a name=\"usc\">");
		sb.append(" </a></p><p><a name=\"usc\">");
		sb.append(" <b>Scripts</b> are specified either with the prefix <code>Is</code>, as in");
		sb.append(" <code>IsHiragana</code>, or by using  the <code>script</code> keyword (or its short");
		sb.append(" form <code>sc</code>)as in <code>script=Hiragana</code> or <code>sc=Hiragana</code>.");
		sb.append(" </a></p><p><a name=\"usc\">");
		sb.append(" The script names supported by <code>Pattern</code> are the valid script names");
		sb.append(" accepted and defined by");
		sb.append(" </a><a href=\"https://docs.oracle.com/javase/7/docs/api/java/lang/Character.UnicodeScript.html#forName(java.lang.String)\"><code>UnicodeScript.forName</code></a>.");
		sb.append(" <a name=\"ubc\">");
		sb.append(" </a></p><p><a name=\"ubc\">");
		sb.append(" <b>Blocks</b> are specified with the prefix <code>In</code>, as in");
		sb.append(" <code>InMongolian</code>, or by using the keyword <code>block</code> (or its short");
		sb.append(" form <code>blk</code>) as in <code>block=Mongolian</code> or <code>blk=Mongolian</code>.");
		sb.append(" </a></p><p><a name=\"ubc\">");
		sb.append(" The block names supported by <code>Pattern</code> are the valid block names");
		sb.append(" accepted and defined by");
		sb.append(" </a><a href=\"https://docs.oracle.com/javase/7/docs/api/java/lang/Character.UnicodeBlock.html#forName(java.lang.String)\"><code>UnicodeBlock.forName</code></a>.");
		sb.append(" </p><p>");
		sb.append(" <a name=\"ucc\">");
		sb.append(" <b>Categories</b> may be specified with the optional prefix <code>Is</code>:");
		sb.append(" Both <code>\\p{L}</code> and <code>\\p{IsL}</code> denote the category of Unicode");
		sb.append(" letters. Same as scripts and blocks, categories can also be specified");
		sb.append(" by using the keyword <code>general_category</code> (or its short form");
		sb.append(" <code>gc</code>) as in <code>general_category=Lu</code> or <code>gc=Lu</code>.");
		sb.append(" </a></p><p><a name=\"ucc\">");
		sb.append(" The supported categories are those of");
		sb.append(" </a><a href=\"http://www.unicode.org/unicode/standard/standard.html\">");
		sb.append(" <i>The Unicode Standard</i></a> in the version specified by the");
		sb.append(" <a href=\"https://docs.oracle.com/javase/7/docs/api/java/lang/Character.html\" title=\"class in java.lang\"><code>Character</code></a> class. The category names are those");
		sb.append(" defined in the Standard, both normative and informative.");
		sb.append(" </p><p>");
		sb.append(" <a name=\"ubpc\">");
		sb.append(" <b>Binary properties</b> are specified with the prefix <code>Is</code>, as in");
		sb.append(" <code>IsAlphabetic</code>. The supported binary properties by <code>Pattern</code>");
		sb.append(" are");
		sb.append(" </a></p><ul><a name=\"ubpc\">");
		sb.append("   <li> Alphabetic");
		sb.append("   </li><li> Ideographic");
		sb.append("   </li><li> Letter");
		sb.append("   </li><li> Lowercase");
		sb.append("   </li><li> Uppercase");
		sb.append("   </li><li> Titlecase");
		sb.append("   </li><li> Punctuation");
		sb.append("   </li><li> Control");
		sb.append("   </li><li> White_Space");
		sb.append("   </li><li> Digit");
		sb.append("   </li><li> Hex_Digit");
		sb.append("   </li><li> Noncharacter_Code_Point");
		sb.append("   </li><li> Assigned");
		sb.append(" </li></a></ul><a name=\"ubpc\">");
		sb.append("");
		sb.append("");
		sb.append(" </a><p><a name=\"ubpc\">");
		sb.append(" <b>Predefined Character classes</b> and <b>POSIX character classes</b> are in");
		sb.append(" conformance with the recommendation of <i>Annex C: Compatibility Properties</i>");
		sb.append(" of </a><a href=\"http://www.unicode.org/reports/tr18/\"><i>Unicode Regular Expression");
		sb.append(" </i></a>, when <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#UNICODE_CHARACTER_CLASS\"><code>UNICODE_CHARACTER_CLASS</code></a> flag is specified.");
		sb.append(" </p><p>");
		sb.append(" </p><table border=\"0\" cellpadding=\"1\" cellspacing=\"0\" summary=\"predefined and posix character classes in Unicode mode\">");
		sb.append(" <tbody><tr align=\"left\">");
		sb.append(" <th bgcolor=\"#CCCCFF\" align=\"left\" id=\"classes\">Classes</th>");
		sb.append(" <th bgcolor=\"#CCCCFF\" align=\"left\" id=\"matches\">Matches</th>");
		sb.append("</tr>");
		sb.append(" <tr><td><tt>\\p{Lower}</tt></td>");
		sb.append("     <td>A lowercase character:<tt>\\p{IsLowercase}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Upper}</tt></td>");
		sb.append("     <td>An uppercase character:<tt>\\p{IsUppercase}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{ASCII}</tt></td>");
		sb.append("     <td>All ASCII:<tt>[\\x00-\\x7F]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Alpha}</tt></td>");
		sb.append("     <td>An alphabetic character:<tt>\\p{IsAlphabetic}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Digit}</tt></td>");
		sb.append("     <td>A decimal digit character:<tt>p{IsDigit}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Alnum}</tt></td>");
		sb.append("     <td>An alphanumeric character:<tt>[\\p{IsAlphabetic}\\p{IsDigit}]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Punct}</tt></td>");
		sb.append("     <td>A punctuation character:<tt>p{IsPunctuation}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Graph}</tt></td>");
		sb.append("     <td>A visible character: <tt>[^\\p{IsWhite_Space}\\p{gc=Cc}\\p{gc=Cs}\\p{gc=Cn}]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Print}</tt></td>");
		sb.append("     <td>A printable character: <tt>[\\p{Graph}\\p{Blank}&amp;&amp;[^\\p{Cntrl}]]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Blank}</tt></td>");
		sb.append("     <td>A space or a tab: <tt>[\\p{IsWhite_Space}&amp;&amp;[^\\p{gc=Zl}\\p{gc=Zp}\\x0a\\x0b\\x0c\\x0d\\x85]]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Cntrl}</tt></td>");
		sb.append("     <td>A control character: <tt>\\p{gc=Cc}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{XDigit}</tt></td>");
		sb.append("     <td>A hexadecimal digit: <tt>[\\p{gc=Nd}\\p{IsHex_Digit}]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\p{Space}</tt></td>");
		sb.append("     <td>A whitespace character:<tt>\\p{IsWhite_Space}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\d</tt></td>");
		sb.append("     <td>A digit: <tt>\\p{IsDigit}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\D</tt></td>");
		sb.append("     <td>A non-digit: <tt>[^\\d]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\s</tt></td>");
		sb.append("     <td>A whitespace character: <tt>\\p{IsWhite_Space}</tt></td></tr>");
		sb.append(" <tr><td><tt>\\S</tt></td>");
		sb.append("     <td>A non-whitespace character: <tt>[^\\s]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\w</tt></td>");
		sb.append("     <td>A word character: <tt>[\\p{Alpha}\\p{gc=Mn}\\p{gc=Me}\\p{gc=Mc}\\p{Digit}\\p{gc=Pc}]</tt></td></tr>");
		sb.append(" <tr><td><tt>\\W</tt></td>");
		sb.append("     <td>A non-word character: <tt>[^\\w]</tt></td></tr>");
		sb.append(" </tbody></table>");
		sb.append(" <p>");
		sb.append(" <a name=\"jcc\">");
		sb.append(" Categories that behave like the java.lang.Character");
		sb.append(" boolean is<i>methodname</i> methods (except for the deprecated ones) are");
		sb.append(" available through the same <tt>\\p{</tt><i>prop</i><tt>}</tt> syntax where");
		sb.append(" the specified property has the name <tt>java<i>methodname</i></tt>.");
		sb.append("");
		sb.append(" </a></p><h4><a name=\"jcc\"> Comparison to Perl 5 </a></h4><a name=\"jcc\">");
		sb.append("");
		sb.append(" <p>The <code>Pattern</code> engine performs traditional NFA-based matching");
		sb.append(" with ordered alternation as occurs in Perl 5.");
		sb.append("");
		sb.append(" </p><p> Perl constructs not supported by this class: </p>");
		sb.append("");
		sb.append(" </a><ul><a name=\"jcc\">");
		sb.append("    </a><li><a name=\"jcc\"><p> Predefined character classes (Unicode character)");
		sb.append("    </p><p><tt>\\h&nbsp;&nbsp;&nbsp;&nbsp;</tt>A horizontal whitespace");
		sb.append("    </p><p><tt>\\H&nbsp;&nbsp;&nbsp;&nbsp;</tt>A non horizontal whitespace");
		sb.append("    </p><p><tt>\\v&nbsp;&nbsp;&nbsp;&nbsp;</tt>A vertical whitespace");
		sb.append("    </p><p><tt>\\V&nbsp;&nbsp;&nbsp;&nbsp;</tt>A non vertical whitespace");
		sb.append("    </p><p><tt>\\R&nbsp;&nbsp;&nbsp;&nbsp;</tt>Any Unicode linebreak sequence");
		sb.append("    <tt>\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]</tt>");
		sb.append("    </p></a><p><a name=\"jcc\"><tt>\\X&nbsp;&nbsp;&nbsp;&nbsp;</tt>Match Unicode");
		sb.append("    </a><a href=\"http://www.unicode.org/reports/tr18/#Default_Grapheme_Clusters\">");
		sb.append("    <i>extended grapheme cluster</i></a>");
		sb.append("    </p></li>");
		sb.append("");
		sb.append("    <li><p> The backreference constructs, <tt>\\g{</tt><i>n</i><tt>}</tt> for");
		sb.append("    the <i>n</i><sup>th</sup><a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#cg\">capturing group</a> and");
		sb.append("    <tt>\\g{</tt><i>name</i><tt>}</tt> for");
		sb.append("    <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#groupname\">named-capturing group</a>.");
		sb.append("    </p></li>");
		sb.append("");
		sb.append("    <li><p> The named character construct, <tt>\\N{</tt><i>name</i><tt>}</tt>");
		sb.append("    for a Unicode character by its name.");
		sb.append("    </p></li>");
		sb.append("");
		sb.append("    <li><p> The conditional constructs");
		sb.append("    <tt>(?(</tt><i>condition</i><tt>)</tt><i>X</i><tt>)</tt> and");
		sb.append("    <tt>(?(</tt><i>condition</i><tt>)</tt><i>X</i><tt>|</tt><i>Y</i><tt>)</tt>,");
		sb.append("    </p></li>");
		sb.append("");
		sb.append("    <li><p> The embedded code constructs <tt>(?{</tt><i>code</i><tt>})</tt>");
		sb.append("    and <tt>(??{</tt><i>code</i><tt>})</tt>,</p></li>");
		sb.append("");
		sb.append("    <li><p> The embedded comment syntax <tt>(?#comment)</tt>, and </p></li>");
		sb.append("");
		sb.append("    <li><p> The preprocessing operations <tt>\\l</tt> <tt>\\u</tt>,");
		sb.append("    <tt>\\L</tt>, and <tt>\\U</tt>.  </p></li>");
		sb.append("");
		sb.append(" </ul>");
		sb.append("");
		sb.append(" <p> Constructs supported by this class but not by Perl: </p>");
		sb.append("");
		sb.append(" <ul>");
		sb.append("");
		sb.append("    <li><p> Character-class union and intersection as described");
		sb.append("    <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#cc\">above</a>.</p></li>");
		sb.append("");
		sb.append(" </ul>");
		sb.append("");
		sb.append(" <p> Notable differences from Perl: </p>");
		sb.append("");
		sb.append(" <ul>");
		sb.append("");
		sb.append("    <li><p> In Perl, <tt>\\1</tt> through <tt>\\9</tt> are always interpreted");
		sb.append("    as back references; a backslash-escaped number greater than <tt>9</tt> is");
		sb.append("    treated as a back reference if at least that many subexpressions exist,");
		sb.append("    otherwise it is interpreted, if possible, as an octal escape.  In this");
		sb.append("    class octal escapes must always begin with a zero. In this class,");
		sb.append("    <tt>\\1</tt> through <tt>\\9</tt> are always interpreted as back");
		sb.append("    references, and a larger number is accepted as a back reference if at");
		sb.append("    least that many subexpressions exist at that point in the regular");
		sb.append("    expression, otherwise the parser will drop digits until the number is");
		sb.append("    smaller or equal to the existing number of groups or it is one digit.");
		sb.append("    </p></li>");
		sb.append("");
		sb.append("    <li><p> Perl uses the <tt>g</tt> flag to request a match that resumes");
		sb.append("    where the last match left off.  This functionality is provided implicitly");
		sb.append("    by the <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html\" title=\"class in java.util.regex\"><code>Matcher</code></a> class: Repeated invocations of the <a href=\"https://docs.oracle.com/javase/7/docs/api/java/util/regex/Matcher.html#find()\"><code>find</code></a> method will resume where the last match left off,");
		sb.append("    unless the matcher is reset.  </p></li>");
		sb.append("");
		sb.append("    <li><p> In Perl, embedded flags at the top level of an expression affect");
		sb.append("    the whole expression.  In this class, embedded flags always take effect");
		sb.append("    at the point at which they appear, whether they are at the top level or");
		sb.append("    within a group; in the latter case, flags are restored at the end of the");
		sb.append("    group just as in Perl.  </p></li>");
		sb.append("");
		sb.append(" </ul>");
		sb.append("");
		sb.append("");
		sb.append(" <p> For a more precise description of the behavior of regular expression");
		sb.append(" constructs, please see <a href=\"http://www.oreilly.com/catalog/regex3/\">");
		sb.append(" <i>Mastering Regular Expressions, 3nd Edition</i>, Jeffrey E. F. Friedl,");
		sb.append(" O'Reilly and Associates, 2006.</a>");
		sb.append(" </p></div>");
		sb.append("</li>");
		sb.append("</ul>");
		sb.append("</div>");
		sb.append("</body></html>");
		return sb.toString();
	}
}
