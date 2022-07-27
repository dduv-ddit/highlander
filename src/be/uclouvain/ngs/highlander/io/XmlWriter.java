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

package be.uclouvain.ngs.highlander.io;

import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class XmlWriter {
  protected Document document;
  private Node currentNode;

  public XmlWriter() throws Exception {
    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    currentNode = document.createElement("highlander");
    document.appendChild(currentNode);
  }

  public XmlWriter(Document doc) throws Exception {
    document = doc;
  }

  public void write(File file) throws TransformerException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                                  "2");
    transformer.setOutputProperty("indent", "yes");
    transformer.transform(new DOMSource(document), new StreamResult(file));
  }

  public void write(OutputStream out) throws TransformerException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                                  "2");
    transformer.setOutputProperty("indent", "yes");
    transformer.transform(new DOMSource(document), new StreamResult(out));
  }

  public void importNode(Node n) {
    currentNode.appendChild(document.importNode(n, true));
  }

  public void child(Node n) {
    currentNode.appendChild(n);
    currentNode = n;
  }

  public void brother(Node n) {
    currentNode.getParentNode().appendChild(n);
    currentNode = n;
  }

  public void parent(Node n) {
    currentNode.getParentNode().getParentNode().appendChild(n);
    currentNode = n;
  }

  public void parent() {
    currentNode = currentNode.getParentNode();
  }

  public Node createState(String name, String value) {
    Element e = document.createElement("state");
    e.setAttribute("name", name);
    e.appendChild(document.createTextNode(value));
    return e;
  }

  public Node createEntry(String name, String value) {
    Element e = document.createElement("entry");
    e.setAttribute("name", name);
    e.setAttribute("value", value);
    return e;
  }

  public Node createEntry(String name, boolean value) {
    Element e = document.createElement("entry");
    e.setAttribute("name", name);
    e.setAttribute("value", Boolean.toString(value));
    return e;
  }

  public Node createEntry(String name, int value) {
    Element e = document.createElement("entry");
    e.setAttribute("name", name);
    e.setAttribute("value", Integer.toString(value));
    return e;
  }

  public Node createEntry(String name, float value) {
    Element e = document.createElement("entry");
    e.setAttribute("name", name);
    e.setAttribute("value", Float.toString(value));
    return e;
  }

}
