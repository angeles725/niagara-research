package com.tridium.lonworks.xml;

import java.util.Vector;
import javax.baja.lonworks.londata.BLonEnum;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Sys;

public class XEnumDef extends XLonData {
   public String typeSpec = "";
   private Vector<String> tags = new Vector<>();
   private Vector<String> ids = new Vector<>();

   public void addEnum(String tag, String id) {
      this.tags.addElement(tag);
      this.ids.addElement(id);
   }

   public void insertEnum(int idx, String tag, String id) {
      this.tags.add(idx, tag);
      this.ids.add(idx, id);
   }

   public void removeEnum(int idx) {
      this.tags.remove(idx);
      this.ids.remove(idx);
   }

   public void setTag(int idx, String tag) {
      this.tags.set(idx, tag);
   }

   public void setId(int idx, String id) {
      this.ids.set(idx, id);
   }

   public void clearEnums() {
      this.tags.clear();
      this.ids.clear();
   }

   public String[] getEnumTags() {
      String[] a = new String[this.tags.size()];
      this.tags.copyInto(a);
      return a;
   }

   public int[] getEnumIds() {
      int[] a = new int[this.ids.size()];

      for (int i = 0; i < a.length; i++) {
         a[i] = Integer.parseInt(this.ids.elementAt(i));
      }

      return a;
   }

   public void rename(String origName, String newName) {
      for (int i = 0; i < this.tags.size(); i++) {
         if (this.tags.elementAt(i).equals(origName)) {
            this.tags.setElementAt(newName, i);
            return;
         }
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof XEnumDef)) {
         return false;
      } else if (this == obj) {
         return true;
      } else {
         XEnumDef oenum = (XEnumDef)obj;
         if (this.tags.size() != oenum.tags.size()) {
            return false;
         } else {
            for (int i = 0; i < this.tags.size(); i++) {
               if (!this.tags.elementAt(i).equals(oenum.tags.elementAt(i)) || !this.ids.elementAt(i).equals(oenum.ids.elementAt(i))) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   @Override
   public int hashCode() {
      StringBuilder encoding = new StringBuilder();

      for (int i = 0; i < this.tags.size(); i++) {
         encoding.append(this.tags.get(i)).append(":").append(this.ids.get(i)).append(";");
      }

      return encoding.toString().hashCode();
   }

   public BLonEnum getEnum() {
      if (this.typeSpec.length() > 0) {
         try {
            BEnum en = (BEnum)Sys.getType(this.typeSpec).getInstance();
            return BLonEnum.make(en);
         } catch (Throwable var4) {
            var4.printStackTrace();
         }
      }

      String[] tagArray = this.getEnumTags();
      int[] idArray = this.getEnumIds();
      if (tagArray.length != idArray.length) {
         throw new RuntimeException("Error in " + this.getName() + " file.");
      } else {
         BDynamicEnum ms = BDynamicEnum.make(idArray[0], BEnumRange.make(idArray, tagArray));
         return BLonEnum.make(ms);
      }
   }
}
