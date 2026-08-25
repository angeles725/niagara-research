package javax.baja.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class XPathElem {
   private static final Logger logger = Logger.getLogger(XPathElem.class.getName());
   private static final String ANY_VALUE = "\u0000";
   private final String elementName;
   private final boolean matchChildren;
   private Map<String, String> attributeValues;

   public XPathElem() {
      this(null, null, false);
   }

   public XPathElem(boolean matchChildren) {
      this(null, null, matchChildren);
   }

   public XPathElem(String elementName) {
      this(elementName, null, false);
   }

   public XPathElem(String elementName, boolean matchChildren) {
      this(elementName, null, matchChildren);
   }

   public XPathElem(String elementName, Map<String, String> attributeValues, boolean matchChildren) {
      this.elementName = elementName;
      this.attributeValues = attributeValues;
      this.matchChildren = matchChildren;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         XPathElem that = (XPathElem)o;
         if (this.matchChildren != that.matchChildren) {
            return false;
         } else if (this.elementName != null ? this.elementName.equals(that.elementName) : that.elementName == null) {
            return this.attributeValues != null ? this.attributeValues.equals(that.attributeValues) : that.attributeValues == null;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public String getName() {
      return this.elementName;
   }

   @Override
   public int hashCode() {
      int result = this.elementName != null ? this.elementName.hashCode() : 0;
      result = 31 * result + (this.attributeValues != null ? this.attributeValues.hashCode() : 0);
      return 31 * result + (this.matchChildren ? 1 : 0);
   }

   public boolean isMatchChildren() {
      return this.matchChildren;
   }

   public boolean matches(XElem element) {
      if (this.elementName != null && !this.elementName.equals(element.qname())) {
         return false;
      }

      if (this.attributeValues == null) {
         return true;
      }

      int matchCount = 0;

      for (int index = 0; index < element.attrSize(); index++) {
         XNs ns = element.attrNs(index);
         String name = ns == null ? element.attrName(index) : ns.prefix() + ":" + element.attrName(index);
         String requiredValue = this.attributeValues.get(name);
         String actualValue = element.attrValue(index);
         if (requiredValue != null) {
            if (!requiredValue.equals(actualValue) && !requiredValue.equals("\u0000")) {
               return false;
            }

            matchCount++;
         }
      }

      return matchCount >= this.attributeValues.size();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("XPathElem{");
      sb.append("elementName='").append(this.elementName).append('\'');
      sb.append(", attributeValues=").append(this.attributeValues);
      sb.append(", matchChildren=").append(this.matchChildren);
      sb.append('}');
      return sb.toString();
   }

   public XPathElem withAttr(String name, String value) {
      if (this.attributeValues == null) {
         this.attributeValues = new HashMap<>();
      }

      this.attributeValues.put(name, value);
      return this;
   }

   public XPathElem withAttr(String name) {
      if (this.attributeValues == null) {
         this.attributeValues = new HashMap<>();
      }

      this.attributeValues.put(name, "\u0000");
      return this;
   }
}
