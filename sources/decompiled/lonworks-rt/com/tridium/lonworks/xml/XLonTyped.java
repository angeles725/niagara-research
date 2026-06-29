package com.tridium.lonworks.xml;

import java.util.Vector;

public abstract class XLonTyped extends XLonData {
   public String typeDef = "";
   public String comment = "";
   public byte[] init = null;
   private XTypeDef xTypDef = null;

   @Override
   public void addAttribute(String name, Object obj) {
      if (obj instanceof XElementQualifier) {
         this.getElementQualifierVector().addElement((XElementQualifier)obj);
      }
   }

   public abstract Vector<XElementQualifier> getElementQualifierVector();

   public void setTypeDef(String t) {
      this.typeDef = t;
   }

   public String getTypeDef() {
      return this.typeDef;
   }

   public abstract void setInit(String var1);

   public abstract String getInit();

   public void setXTypeDef(XTypeDef t) {
      this.xTypDef = t;
   }

   public XTypeDef getXTypeDef() {
      return this.xTypDef;
   }

   @Override
   public String toString() {
      return "XLonTyped[typeDef=" + this.typeDef + ",comment=" + this.comment + "]\n" + super.toString();
   }

   public XElementQualifier[] getElementQualifiers(XLonInterfaceFile inFile) {
      if (this.xTypDef == null && this.typeDef.length() > 0) {
         this.xTypDef = inFile.resolveTypeDef(this.typeDef);
      }

      if (this.xTypDef != null) {
         return this.xTypDef.getElementQualifiers(inFile);
      } else {
         Vector<XElementQualifier> v = this.getElementQualifierVector();
         XElementQualifier[] a = new XElementQualifier[v.size()];
         v.copyInto(a);
         return a;
      }
   }

   public XUnion getUnion() {
      return this.xTypDef != null ? this.xTypDef.union : null;
   }

   public byte[] getInitBytes(XLonInterfaceFile inFile) {
      if (this.init != null) {
         return this.init;
      } else {
         if (this.xTypDef == null && this.typeDef.length() > 0) {
            this.xTypDef = inFile.resolveTypeDef(this.typeDef);
         }

         return this.xTypDef != null && this.xTypDef.isCpType() ? ((XCpTypeDef)this.xTypDef).init : null;
      }
   }

   public String getTypeSpec(XLonInterfaceFile inFile) {
      if (this.xTypDef == null && this.typeDef.length() > 0) {
         this.xTypDef = inFile.resolveTypeDef(this.typeDef);
      }

      return this.xTypDef == null ? "" : this.xTypDef.typeSpec;
   }
}
