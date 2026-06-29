package com.tridium.lonworks.util;

import javax.baja.lonworks.BDynamicDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

public class DeviceDef {
   private static String[] wildCards = null;
   BProgramId pid;
   String[] values;
   String wildcard = null;

   public DeviceDef(BProgramId pid) {
      this.pid = pid;
      String defName = this.toKey(pid);
      this.values = Sys.getRegistry().getDefs(defName);
      if (this.values.length == 0) {
         this.values = this.checkForWildCards(defName);
      }
   }

   private String toKey(BProgramId pid) {
      return "lonworks." + pid.toString();
   }

   public boolean wildCardMatch(BProgramId pid) {
      return this.wildcard == null ? false : this.match(this.toKey(pid), this.wildcard);
   }

   private String[] checkForWildCards(String key) {
      String[] wc = this.getWildCards();

      for (int i = 0; i < wc.length; i++) {
         if (this.match(key, wc[i])) {
            this.wildcard = wc[i];
            return Sys.getRegistry().getDefs(wc[i]);
         }
      }

      return new String[0];
   }

   private boolean match(String key, String wc) {
      for (int n = 8; n < wc.length(); n++) {
         if (wc.charAt(n) != '*' && key.charAt(n) != wc.charAt(n)) {
            return false;
         }
      }

      return true;
   }

   private String[] getWildCards() {
      if (wildCards != null) {
         return wildCards;
      } else {
         String[] dfs = Sys.getRegistry().getDefs();
         Array<String> a = new Array(String.class);

         for (int i = 0; i < dfs.length; i++) {
            if (dfs[i].startsWith("lonworks.") && dfs[i].indexOf("*") > 0) {
               a.add(dfs[i]);
            }
         }

         wildCards = (String[])a.trim();
         return wildCards;
      }
   }

   public boolean isXml() {
      return this.isXml(0);
   }

   public boolean isXml(int ndx) {
      return ndx >= 0 && this.values.length > ndx && this.values[ndx].startsWith("xml");
   }

   public boolean isClass() {
      return this.isClass(0);
   }

   public boolean isClass(int ndx) {
      return ndx >= 0 && this.values.length > ndx && this.values[ndx].startsWith("cl");
   }

   public boolean isMultpleEntries() {
      return this.values.length > 1;
   }

   public boolean hasEntries() {
      return this.values.length > 0;
   }

   public int entryCount() {
      return this.values.length;
   }

   public BOrd getXmlOrd() {
      return this.getXmlOrd(0);
   }

   public BOrd getXmlOrd(int ndx) {
      return ndx >= 0 && this.values.length > ndx ? BOrd.make("module://" + this.name(ndx)) : null;
   }

   public BLonDevice getDevice() {
      return this.getDevice(0);
   }

   public BLonDevice getDevice(int ndx) {
      return ndx >= 0 && this.values.length > ndx ? (BLonDevice)Sys.getRegistry().getType(this.name(ndx)).getInstance() : null;
   }

   public String getName() {
      return this.getName(0);
   }

   public String getName(int ndx) {
      if (ndx >= 0 && this.values.length > ndx) {
         String v = this.values[ndx];
         if (this.isClass(ndx)) {
            return v.substring(v.indexOf(":") + 1);
         }

         if (this.isXml(ndx)) {
            return v.substring(v.indexOf("/") + 1, v.indexOf(".lnml"));
         }
      }

      return "LonDevice";
   }

   public String getModule() {
      return this.getModule(0);
   }

   public String getModule(int ndx) {
      if (ndx >= 0 && this.values.length > ndx) {
         String v = this.values[ndx];
         if (this.isClass(ndx)) {
            return v.substring(v.indexOf("=") + 1, v.indexOf(":"));
         }

         if (this.isXml(ndx)) {
            return v.substring(v.indexOf("=") + 1, v.indexOf("/"));
         }
      }

      return "lonworks";
   }

   public TypeInfo getTypeInfo(int ndx) {
      return this.getType(ndx).getTypeInfo();
   }

   public Type getType(int ndx) {
      return this.isClass(ndx) ? Sys.getType(this.name(ndx)) : BDynamicDevice.TYPE;
   }

   private String name(int ndx) {
      String name = this.values[ndx];
      return name.substring(name.indexOf(61) + 1);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("pid =").append(this.pid).append("\n");

      for (int i = 0; i < this.values.length; i++) {
         sb.append(i).append(" ").append(this.values[i]).append("\n");
      }

      sb.append("wildcard=").append(this.wildcard).append("\n");
      return sb.toString();
   }
}
