package com.tridium.lonworks.xml;

import com.tridium.lonworks.util.NameUtil;
import java.util.Vector;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.sys.Sys;

public class XTypeDef extends XLonData {
   public Vector<XElementQualifier> elems = new Vector<>();
   public String typeScope = "";
   public String typeSpec = "";
   public XUnion union = null;
   private static int enter = 0;

   @Override
   public void addAttribute(String name, Object obj) {
      if (obj instanceof XElementQualifier) {
         this.elems.addElement((XElementQualifier)obj);
      }
   }

   public String getTypeScope() {
      return this.typeScope;
   }

   public void setTypeScope(String ts) {
      this.typeScope = ts;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("typeSpec=").append(this.typeSpec).append(";typeScope=").append(this.typeScope);
      if (this.elems != null) {
         sb.append(";elems.size()=").append(this.elems.size());
         int i = 0;

         for (int eSize = this.elems.size(); i < eSize; i++) {
            sb.append(";elems[").append(i).append("]=").append(this.elems.elementAt(i));
         }
      } else {
         sb.append(";elems null");
      }

      return sb.toString();
   }

   public int getTypeIndex() {
      return this.typeScope.length() < 3 ? -1 : Integer.decode(this.typeScope.substring(this.typeScope.indexOf(44) + 1));
   }

   public boolean isCpType() {
      return false;
   }

   public XElementQualifier[] getElementQualifiers(XLonInterfaceFile inFile) {
      return this.getElementQualifiers(inFile, true);
   }

   private XElementQualifier[] getElementQualifiers(XLonInterfaceFile inFile, boolean topLevel) {
      enter++;
      if (enter > 10) {
         System.out.println("--- Debugging 10 --- this = \n" + this.toString());
         Thread.dumpStack();
      }

      this.resolveRefs(inFile, topLevel);
      XElementQualifier[] a = new XElementQualifier[this.elems.size()];
      this.elems.copyInto(a);
      enter = 0;
      return a;
   }

   private void resolveRefs(XLonInterfaceFile inFile, boolean topLevel) {
      boolean prependMyName = !topLevel || topLevel && this.elems.size() > 1;
      int ndx = 0;
      boolean forceByteOffset = false;
      int byteOffset = 0;

      while (ndx < this.elems.size()) {
         XElementQualifier eq = this.elems.elementAt(ndx);
         if (eq.hasByteOffset()) {
            forceByteOffset = true;
            byteOffset = eq.getByteOffset();
         }

         if (eq.isRef()) {
            XTypeDef typeDef = inFile.resolveTypeDef(this, eq.getTypeDef());
            if (typeDef == null) {
               throw new RuntimeException("Can't resolve reference element " + eq.getTypeDef() + " in " + this.getName() + "." + eq.getName());
            }

            if (typeDef.union != null) {
               if (!topLevel || this.elems.size() > 1) {
                  throw new RuntimeException("Can't handle union nested in struct");
               }

               this.union = typeDef.union;
            }

            XElementQualifier[] eqs = typeDef.getElementQualifiers(inFile, false);
            int byteCount = 0;

            for (int i = 0; i < eqs.length; i++) {
               eqs[i] = eqs[i].copy();
               if (eqs[i].hasByteOffset()) {
                  forceByteOffset = true;
               }

               int myCnt = byteCount;
               byteCount = eqs[i].accumulateByteCount(byteCount);
               if (forceByteOffset) {
                  if (eqs[i].hasByteOffset()) {
                     eqs[i].setByteOffset(eqs[i].getByteOffset() + byteOffset);
                  } else {
                     eqs[i].setByteOffset(myCnt + byteOffset);
                  }
               }

               if (eq.branch != null) {
                  eqs[i].branch = eq.branch;
               }
            }

            byteOffset += byteCount;
            if (eqs.length == 1) {
               eqs[0].setName(eq.getName());
            } else if (prependMyName) {
               for (int i = 0; i < eqs.length; i++) {
                  eqs[i].setName(eq.getName() + NameUtil.toJavaName(eqs[i].getName(), true));
               }
            }

            this.elems.setElementAt(eqs[0], ndx++);

            for (int i = 1; i < eqs.length; i++) {
               this.elems.insertElementAt(eqs[i], ndx++);
            }
         } else {
            byteOffset = eq.accumulateByteCount(byteOffset);
            ndx++;
         }
      }
   }

   public BLonData getLonData(XLonInterfaceFile inFile) {
      if (this.typeSpec.length() > 0) {
         try {
            return (BLonData)Sys.getType(this.typeSpec).getInstance();
         } catch (Throwable var3) {
            var3.printStackTrace();
         }
      }

      return XUtil.makeLonData(this.getElementQualifiers(inFile), this.union, inFile);
   }

   public void rename(String origName, String newName) {
      for (int i = 0; i < this.elems.size(); i++) {
         XElementQualifier eq = this.elems.elementAt(i);
         if (eq.getName().equals(origName)) {
            eq.setName(newName);
            return;
         }
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof XTypeDef)) {
         return false;
      } else if (this == obj) {
         return true;
      } else {
         XTypeDef otyp = (XTypeDef)obj;
         if (this.elems.size() != otyp.elems.size()) {
            return false;
         } else {
            for (int i = 0; i < this.elems.size(); i++) {
               if (!this.getElement(i).equals(otyp.getElement(i))) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   @Override
   public int hashCode() {
      return this.toString().hashCode();
   }

   public XElementQualifier getElement(int ndx) {
      return this.elems.elementAt(ndx);
   }
}
