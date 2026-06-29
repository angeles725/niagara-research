package javax.baja.bacnet.util;

import com.tridium.bacnet.asn.AsnUtil;
import javax.baja.bacnet.BacnetConst;
import javax.baja.xml.XElem;

public class PropertyInfo implements BacnetConst {
   private String name;
   private int id;
   private int pr;
   private int asnType;
   private boolean extensible;
   private String bs;
   private String type;
   private int size;
   private boolean facet;
   private String facetControl;

   public PropertyInfo(String name, int id, int asnType) {
      this.name = name;
      this.id = id;
      this.asnType = asnType;
      this.facet = false;
      this.facetControl = "no";
      this.pr = 8;
   }

   public PropertyInfo(String name, int id, int asnType, int pr) {
      this.name = name;
      this.id = id;
      this.asnType = asnType;
      this.facet = false;
      this.facetControl = "no";
      this.pr = pr;
   }

   public PropertyInfo(XElem x, int objectPR) {
      this.name = x.get("n");
      this.id = x.geti("i");
      this.pr = x.geti("pr", objectPR);
      this.asnType = x.geti("a", -6);
      switch (this.asnType) {
         case -4:
            this.type = "bacnet:BacnetAny";
         case -3:
         case -2:
         case -1:
         case 9:
         default:
            break;
         case 0:
            this.type = "bacnet:BacnetNull";
            break;
         case 1:
            this.type = "baja:Boolean";
            break;
         case 2:
            this.type = "bacnet:BacnetUnsigned";
            break;
         case 3:
            this.type = "baja:Integer";
            break;
         case 4:
            this.type = "baja:Float";
            break;
         case 5:
            this.type = "baja:Double";
            break;
         case 6:
            this.type = "bacnet:BacnetOctetString";
            break;
         case 7:
            this.type = "baja:String";
            break;
         case 8:
            this.type = "bacnet:BacnetBitString";
            break;
         case 10:
            this.type = "bacnet:BacnetDate";
            break;
         case 11:
            this.type = "bacnet:BacnetTime";
            break;
         case 12:
            this.type = "bacnet:BacnetObjectIdentifier";
      }

      this.facet = x.getb("f", false);
      this.facetControl = x.get("c", "no");
      if (this.isEnum()) {
         this.extensible = x.getb("e", false);
         this.type = x.get("t");
         this.facetControl = "enum";
      } else if (this.isBitString()) {
         this.bs = x.get("b", null);
      } else if (this.isConstructed()) {
         this.type = x.get("t");
      } else if (this.isArray()) {
         this.type = x.get("t");
         this.size = x.geti("s", -1);
      } else if (this.isList()) {
         this.type = x.get("t");
      } else if (this.isChoice()) {
         this.type = x.get("t");
      }
   }

   public PropertyInfo(String name, int id, int asnType, boolean facet, String facetControl) {
      this.name = name;
      this.id = id;
      this.asnType = asnType;
      this.facet = facet;
      this.facetControl = facetControl;
   }

   public PropertyInfo(String name, int id, int asnType, boolean extensible, String type, boolean facet, String facetControl) {
      this(name, id, asnType, facet, facetControl);
      this.extensible = extensible;
      this.type = type;
      this.facet = facet;
      this.facetControl = facetControl;
   }

   public PropertyInfo(String name, int id, int asnType, String s, boolean bitString, boolean facet, String facetControl) {
      this(name, id, asnType, facet, facetControl);
      if (bitString) {
         this.bs = s;
      } else {
         this.type = s;
      }
   }

   public PropertyInfo(String name, int id, int asnType, String type, int size, boolean facet, String facetControl) {
      this(name, id, asnType, false, type, facet, facetControl);
      this.size = size;
   }

   public String getName() {
      return this.name;
   }

   public int getId() {
      return this.id;
   }

   public int getPr() {
      return this.pr;
   }

   public int getAsnType() {
      return this.asnType;
   }

   public String getDataType() {
      return AsnUtil.getAsnTypeName(this.asnType);
   }

   public boolean isExtensible() {
      if (!this.isEnum()) {
         throw new IllegalStateException();
      } else {
         return this.extensible;
      }
   }

   public String getBitStringName() {
      if (!this.isBitString()) {
         throw new IllegalStateException();
      } else {
         return this.bs;
      }
   }

   public String getType() {
      return this.type;
   }

   public int getSize() {
      if (!this.isArray()) {
         throw new IllegalStateException();
      } else {
         return this.size;
      }
   }

   public boolean isEnum() {
      return this.asnType == 9;
   }

   public boolean isBitString() {
      return this.asnType == 8;
   }

   public boolean isConstructed() {
      return this.asnType == -1;
   }

   public boolean isArray() {
      return this.asnType == -2;
   }

   public boolean isList() {
      return this.asnType == -3;
   }

   public boolean isChoice() {
      return this.asnType == -5;
   }

   public boolean isFacet() {
      return this.facet;
   }

   public String getFacetControl() {
      return this.facetControl;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n n=" + this.name).append("\n i=" + this.id).append("\n a=" + this.asnType);
      if (this.facet) {
         sb.append("\n f=" + this.facet);
      }

      sb.append("\n c=" + this.facetControl);
      sb.append("\n t=" + this.type);
      if (this.isEnum()) {
         sb.append("\n e=" + this.extensible);
      } else if (this.isBitString()) {
         sb.append("\n b=" + this.bs);
      } else if (this.isArray()) {
         sb.append("\n s=" + this.size);
      }

      return sb.toString();
   }

   public boolean isAws() {
      return this.type != null && this.type.indexOf("Aws") >= 0;
   }
}
