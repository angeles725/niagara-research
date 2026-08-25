package javax.baja.nre.annotations.processors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.FileExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class ModuleInclude {
   private static final String DUMMY_OPEN_TAG = "<dummyroot>";
   private static final String DUMMY_CLOSE_TAG = "</dummyroot>";
   private static final String DUMMY_TAG_REPLACE = "<(/)?dummyroot>";
   private static final Pattern NEW_LINE_INDENT = Pattern.compile("(\\r?\\n)\\s{2}");
   private static final Pattern DUMMY_PATTERN = Pattern.compile("<(/)?dummyroot>");
   Document document;

   public ModuleInclude(String path) throws ModuleInclude.XmlException {
      try {
         this.document = openDocument(path);
      } catch (Exception e) {
         throw new ModuleInclude.XmlException(e.getMessage());
      }
   }

   private static Document openDocument(String documentPath) throws Exception {
      File documentFile = new File(documentPath);

      try {
         if (!documentFile.exists() && !documentFile.createNewFile()) {
            throw new Exception("Could not load the XML document at path " + documentPath);
         }
      } catch (IOException ioe) {
         throw new Exception("An error occurred while loading the XML document at path " + documentPath + ": " + ioe.getMessage(), ioe);
      }

      StringBuilder sb = new StringBuilder("<dummyroot>");

      try (Scanner s = new Scanner(documentFile)) {
         while (s.hasNextLine()) {
            String line = s.nextLine().trim();
            if (!line.startsWith("<")) {
               line = ' ' + line;
            }

            sb.append(line);
         }
      } catch (FileNotFoundException fnf) {
         throw new Exception("XML cannot be read, but this message should not occur.");
      }

      sb.append("</dummyroot>");
      Document d = parseXmlDocument(sb.toString());
      if (d.getElementsByTagName("types").getLength() == 0) {
         Element types = d.createElement("types");
         d.getFirstChild().appendChild(types);
      }

      return d;
   }

   private static Document parseXmlDocument(String xml) throws Exception {
      try {
         DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
         DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
         InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
         return dBuilder.parse(is);
      } catch (ParserConfigurationException | SAXException | IOException e) {
         throw new Exception("An error occurred while parsing XML into a Document.");
      }
   }

   public void save(String path) throws ModuleInclude.XmlException {
      try (FileWriter fw = new FileWriter(path, false)) {
         String rawXml = this.transformDoc();
         fw.write(rawXml);
         fw.flush();
      } catch (TransformerFactoryConfigurationError | IOException e) {
         throw new ModuleInclude.XmlException("An exception occurred and the XML document may not have saved. " + e.getMessage());
      }
   }

   public String transformDoc() {
      try {
         StringWriter stringWriter = new StringWriter();
         StreamResult xmlOutput = new StreamResult(stringWriter);
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
         transformerFactory.setAttribute("indent-number", 2);
         Transformer transformer = transformerFactory.newTransformer();
         transformer.setOutputProperty("indent", "yes");
         transformer.setOutputProperty("omit-xml-declaration", "yes");
         transformer.transform(new DOMSource(this.document), xmlOutput);
         String rawXml = xmlOutput.getWriter().toString();
         rawXml = DUMMY_PATTERN.matcher(rawXml).replaceAll("");
         rawXml = rawXml.trim();
         return NEW_LINE_INDENT.matcher(rawXml).replaceAll(System.lineSeparator());
      } catch (TransformerException e) {
         throw new ModuleInclude.XmlException("An exception occurred and the XML document may not have saved. " + e.getMessage());
      }
   }

   public void addTypeNode(NiagaraType annotation, String name, String cls) {
      Element type = this.document.createElement("type");
      this.setAttr(type, "class", cls);
      this.setAttr(type, "name", name);
      if (!annotation.ordScheme().isEmpty()) {
         this.setAttr(type, "ordScheme", annotation.ordScheme());
      }

      for (AgentOn agentAnnotation : annotation.agent()) {
         if (agentAnnotation.types().length > 0) {
            Element agent = this.document.createElement("agent");
            if (!agentAnnotation.requiredPermissions().isEmpty()) {
               this.setAttr(agent, "requiredPermissions", agentAnnotation.requiredPermissions());
            }

            if (!agentAnnotation.app().isEmpty()) {
               this.setAttr(agent, "app", agentAnnotation.app());
            }

            switch (agentAnnotation.defaultAgent()) {
               case PREFERRED:
                  this.setAttr(agent, "default", "true");
                  break;
               case NOT_PREFERRED:
                  this.setAttr(agent, "default", "false");
               case NORMAL:
            }

            Element on = null;

            for (String onType : agentAnnotation.types()) {
               if (!onType.isEmpty()) {
                  on = this.document.createElement("on");
                  this.setAttr(on, "type", onType);
                  agent.appendChild(on);
               }
            }

            if (on != null) {
               type.appendChild(agent);
            }
         }
      }

      String from = annotation.adapter().from();
      String to = annotation.adapter().to();
      if (!from.isEmpty() && !to.isEmpty()) {
         Element adapter = this.document.createElement("adapter");
         this.setAttr(adapter, "from", from);
         this.setAttr(adapter, "to", to);
         type.appendChild(adapter);
      }

      if (annotation.ext().length > 0) {
         Element file = this.document.createElement("file");
         Element ext = null;

         for (FileExt fe : annotation.ext()) {
            if (!fe.name().isEmpty()) {
               ext = this.document.createElement("ext");
               this.setAttr(ext, "name", fe.name());
               file.appendChild(ext);
            }
         }

         if (ext != null) {
            type.appendChild(file);
         }
      }

      this.placeTypeNode(type);
   }

   private void placeTypeNode(Element newTypeNode) {
      String typeName = newTypeNode.getAttribute("name");
      String typeClass = newTypeNode.getAttribute("class");
      Node oldTypeNode = this.getTypeNode(typeName, typeClass);
      Node typesContainerNode = this.getTypesNode();
      if (oldTypeNode == null) {
         Node lastMatch = null;
         XPathFactory xpf = XPathFactory.newInstance();
         XPath xpath = xpf.newXPath();
         String pkg = getPackageName(typeClass) + '.';
         String xpathExpr = "type[contains(@class, '" + pkg + "')]";

         try {
            NodeList classNodes = (NodeList)xpath.evaluate(xpathExpr, typesContainerNode, XPathConstants.NODESET);

            for (int x = 0; x < classNodes.getLength(); x++) {
               Node node = classNodes.item(x);
               if (node.getAttributes() != null
                  && node.getAttributes().getNamedItem("name") != null
                  && node.getAttributes().getNamedItem("class") != null
                  && node.getAttributes().getNamedItem("class").getNodeValue().equals(pkg + 'B' + node.getAttributes().getNamedItem("name").getNodeValue())) {
                  lastMatch = node;
               }
            }
         } catch (XPathExpressionException e) {
            e.printStackTrace();
            lastMatch = null;
         }

         if (lastMatch != null) {
            typesContainerNode.insertBefore(newTypeNode, lastMatch.getNextSibling());
         } else {
            typesContainerNode.appendChild(newTypeNode);
         }
      } else {
         typesContainerNode.replaceChild(newTypeNode, oldTypeNode);
      }
   }

   private Node getTypeNode(String annotatedName, String annotatedClass) {
      Node typesNode = this.getTypesNode();

      for (Node typeIter = typesNode.getFirstChild(); typeIter != null; typeIter = typeIter.getNextSibling()) {
         if (typeIter.getNodeName().equals("type")
            && typeIter.getAttributes() != null
            && typeIter.getAttributes().getNamedItem("name") != null
            && typeIter.getAttributes().getNamedItem("class") != null) {
            String name = typeIter.getAttributes().getNamedItem("name").getNodeValue();
            String cls = typeIter.getAttributes().getNamedItem("class").getNodeValue();
            if (name != null && name.equals(annotatedName) && cls != null && cls.equals(annotatedClass)) {
               return typeIter;
            }
         }
      }

      return null;
   }

   private Node getTypesNode() {
      Node typesNode = this.document.getElementsByTagName("types").item(0);
      if (typesNode == null) {
         throw new ModuleInclude.XmlException("Could not find <types> tag in module-include.xml");
      } else {
         return typesNode;
      }
   }

   public void stripWhitespace() {
      this.stripWhitespace(this.document.getChildNodes());
   }

   private void stripWhitespace(NodeList nodes) {
      LinkedList<Node> elements = new LinkedList<>();
      this.nodeListToListGeneric(nodes, elements);

      for (Node node : elements) {
         if (node.hasChildNodes()) {
            this.stripWhitespace(node.getChildNodes());
         }

         if (node instanceof Text) {
            Text textNode = (Text)node;
            if (textNode.getWholeText().trim().isEmpty()) {
               node.getParentNode().removeChild(node);
            }
         }
      }
   }

   public void format() throws ModuleInclude.XmlException {
      Node typeContainer = this.getTypesNode();
      LinkedList<Node> allElements = new LinkedList<>();
      this.nodeListToListGeneric(typeContainer.getChildNodes(), allElements);
      ArrayList<String> commentList = new ArrayList<>();

      for (int i = 0; i < allElements.size(); i++) {
         Node currentNode = allElements.get(i);
         if (currentNode instanceof Comment) {
            commentList.add(currentNode.getTextContent());
         } else if (currentNode instanceof Element) {
            Element currentElement = (Element)currentNode;
            String elementClass = currentElement.getAttribute("class");
            String elementName = currentElement.getAttribute("name");
            if (!elementClass.substring(elementClass.lastIndexOf(46) + 2).equals(elementName)) {
               typeContainer.removeChild(currentElement);
               continue;
            }

            if (!getPackageName(currentElement.getAttribute("class")).equals(this.getPreviousTypeClass(i, allElements).orElse(null))) {
               String commentText = getPackageName(currentElement.getAttribute("class"));
               if (!this.commentExists(commentText, commentList)) {
                  Node comment = this.document.createComment(commentText);
                  typeContainer.appendChild(comment);
                  commentList.add(commentText);
               }
            }
         }

         typeContainer.appendChild(currentNode);
      }
   }

   public <T extends Node> Optional<String> getPreviousTypeClass(int num, List<T> genericList) {
      Optional<String> result = Optional.empty();

      for (int x = num; x >= 0; x--) {
         Node current = genericList.get(x);
         if (current instanceof Element) {
            result = Optional.of(((Element)current).getAttribute("class"));
            break;
         }
      }

      return result;
   }

   public boolean commentExists(String commentText, List<String> comments) {
      for (String comment : comments) {
         if (comment.equals(commentText)) {
            return true;
         }
      }

      return false;
   }

   public boolean containsEntry(String typeName, String typeClass) {
      return this.getTypeNode(typeName, typeClass) != null;
   }

   public boolean entryMatches(NiagaraType annotation, String typeName, String typeClass) {
      Element included = (Element)this.getTypeNode(typeName, typeClass);
      if (included != null && annotation != null) {
         if (!annotation.ordScheme().equals(included.getAttribute("ordScheme"))) {
            return false;
         }

         Element adapterElem = (Element)included.getElementsByTagName("adapter").item(0);
         String to = annotation.adapter().to();
         String from = annotation.adapter().from();
         if (!from.isEmpty() && !to.isEmpty() && adapterElem != null) {
            String elemFrom = adapterElem.getAttribute("from");
            String elemTo = adapterElem.getAttribute("to");
            if (!from.equals(elemFrom) || !to.equals(elemTo)) {
               return false;
            }
         } else if (from.isEmpty() && to.isEmpty() && adapterElem != null || !from.isEmpty() && !to.isEmpty() && adapterElem == null) {
            return false;
         }

         NodeList agentNodes = included.getElementsByTagName("agent");

         for (int i = 0; i < agentNodes.getLength(); i++) {
            Element agentElem = (Element)agentNodes.item(i);
            boolean matched = false;

            for (AgentOn agentAnnotation : annotation.agent()) {
               if (agentAnnotation.types().length > 0 && agentElem != null) {
                  if (agentAnnotation.requiredPermissions().equals(agentElem.getAttribute("requiredPermissions"))
                     && agentAnnotation.app().equals(agentElem.getAttribute("app"))) {
                     NodeList agentOns = agentElem.getElementsByTagName("on");
                     if (agentAnnotation.types().length == agentOns.getLength()) {
                        for (int j = 0; j < agentOns.getLength(); j++) {
                           Element onElem = (Element)agentOns.item(j);
                           boolean found = false;

                           for (String agentOnType : agentAnnotation.types()) {
                              if (agentOnType.equals(onElem.getAttribute("type"))) {
                                 found = true;
                                 break;
                              }
                           }

                           if (found) {
                              matched = true;
                              break;
                           }
                        }
                     }
                  }
               } else if ((agentAnnotation.types().length <= 0 || agentElem != null)
                  && agentAnnotation.types().length == 0
                  && agentElem != null
                  && agentElem.getElementsByTagName("on").getLength() != 0) {
               }
            }

            if (!matched) {
               return false;
            }
         }

         Element fileElem = (Element)included.getElementsByTagName("file").item(0);
         if (annotation.ext().length > 0 && fileElem != null) {
            NodeList exts = fileElem.getElementsByTagName("ext");
            if (annotation.ext().length != exts.getLength()) {
               return false;
            }

            for (int i = 0; i < exts.getLength(); i++) {
               boolean found = false;
               Element extElem = (Element)exts.item(i);

               for (FileExt annExt : annotation.ext()) {
                  if (annExt.name().equals(extElem.getAttribute("name"))) {
                     found = true;
                     break;
                  }
               }

               if (!found) {
                  return false;
               }
            }
         } else if (annotation.ext().length > 0 && fileElem == null
            || annotation.ext().length == 0 && fileElem != null && fileElem.getElementsByTagName("ext").getLength() != 0) {
            return false;
         }

         return true;
      } else {
         return false;
      }
   }

   private static String getPackageName(String qualifiedClass) {
      return qualifiedClass.lastIndexOf(46) > 0 ? qualifiedClass.substring(0, qualifiedClass.lastIndexOf(46)) : "";
   }

   private <T extends Node> void nodeListToListGeneric(NodeList nodeList, List<T> genericList) {
      for (int i = 0; i < nodeList.getLength(); i++) {
         T item = (T)nodeList.item(i);
         genericList.add(item);
      }
   }

   private void setAttr(Element element, String name, String value) {
      Document d = element.getOwnerDocument();
      Attr attribute = d.createAttribute(name);
      attribute.setNodeValue(value);
      element.setAttributeNode(attribute);
   }

   public class XmlException extends RuntimeException {
      public XmlException(String message) {
         super(message);
      }
   }
}
