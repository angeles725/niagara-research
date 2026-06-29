package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.utils.XMLFactoryCache;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public final class XmlElement implements Cloneable {
   private static final Logger logger = LoggerFactory.getLogger(XmlElement.class);
   @Deprecated
   public static final NodeId ID = Identifiers.XmlElement;
   public static final XmlElement[] EMPTY_ARRAY = new XmlElement[0];
   public static final String UTF8_BOM = "\ufeff";
   private final String document;

   static String a(Node var0, boolean var1, int var2) {
      Transformer var3 = null;

      try {
         var3 = XMLFactoryCache.getTransformerFactory().newTransformer();
         if (var2 < 1) {
            var3.setOutputProperty("indent", "no");
         } else {
            var3.setOutputProperty("indent", "yes");
            var3.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
         }

         if (var1) {
            var3.setOutputProperty("omit-xml-declaration", "no");
         } else {
            var3.setOutputProperty("omit-xml-declaration", "yes");
         }

         var3.setOutputProperty("method", "xml");
         var3.setOutputProperty("encoding", "utf-8");
      } catch (TransformerConfigurationException var9) {
         throw new IllegalStateException("Unexpected failure when creating Transformer", var9);
      }

      StringWriter var4 = new StringWriter();
      DOMSource var5 = new DOMSource(var0);
      StreamResult var6 = new StreamResult(var4);

      try {
         var3.transform(var5, var6);
      } catch (TransformerException var8) {
         throw new IllegalStateException("Unexpected failure when transforming XML to String", var8);
      }

      return var4.toString();
   }

   static Node B(String var0) throws IllegalStateException {
      try {
         StringReader var1 = new StringReader(var0);
         char[] var2 = new char[2];
         var1.read(var2, 0, 1);
         if (var2[0] != "\ufeff".charAt(0)) {
            var1 = new StringReader(var0);
         }

         DocumentBuilder var3 = XMLFactoryCache.getDocumentBuilderFactory().newDocumentBuilder();
         return var3.parse(new InputSource(var1));
      } catch (Exception var4) {
         throw new IllegalStateException("The XmlElement data is not a valid XML document", var4);
      }
   }

   static String a(String var0, boolean var1, int var2) {
      try {
         return a(B(var0), var1, var2);
      } catch (Exception var4) {
         logger.trace("Cannot convert {} as pretty xml", var0, var4);
         return var0;
      }
   }

   public XmlElement(byte[] var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("value is null");
      } else {
         if (var1.length == 0) {
            this.document = "";
         } else {
            this.document = new String(var1, StandardCharsets.UTF_8);
         }
      }
   }

   public XmlElement(Node var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("value is null");
      } else {
         this.document = a(var1, false, 0);
      }
   }

   public XmlElement(String var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("value is null");
      } else {
         this.document = var1;
      }
   }

   public XmlElement clone() {
      try {
         return (XmlElement)super.clone();
      } catch (CloneNotSupportedException var2) {
         throw new Error("Every XmlElement implementation shall be Cloneable", var2);
      }
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof XmlElement)) {
         return false;
      } else {
         XmlElement var2 = (XmlElement)var1;
         return Objects.equals(this.document, var2.document);
      }
   }

   public synchronized byte[] getData() {
      return this.document.getBytes(StandardCharsets.UTF_8);
   }

   public synchronized Node getNode() throws IllegalStateException {
      return B(this.document);
   }

   public synchronized String getValue() {
      return this.document;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.document);
   }

   public String toPrettyString(boolean var1, int var2) {
      try {
         return a(B(this.document), var1, var2);
      } catch (Exception var4) {
         return this.document;
      }
   }

   @Override
   public String toString() {
      return this.getValue();
   }
}
