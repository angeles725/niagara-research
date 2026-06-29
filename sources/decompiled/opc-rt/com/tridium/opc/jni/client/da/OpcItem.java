package com.tridium.opc.jni.client.da;

import java.util.Vector;

public class OpcItem {
   public String id;
   public String name;
   private Vector<OpcItem.Property> properties;

   public OpcItem() {
   }

   public OpcItem(String name, String id) {
      this.name = name;
      this.id = id;
   }

   public OpcItem.Property getProperty(int pid) {
      Vector<OpcItem.Property> v = this.properties;
      int i = v.size();

      while (--i >= 0) {
         OpcItem.Property p = v.elementAt(i);
         if (p.pid == pid) {
            return p;
         }
      }

      return null;
   }

   public OpcItem.Property[] getProperties() {
      if (this.properties == null) {
         return new OpcItem.Property[0];
      } else {
         OpcItem.Property[] ret = new OpcItem.Property[this.properties.size()];
         this.properties.copyInto(ret);
         return ret;
      }
   }

   @Override
   public String toString() {
      return '[' + this.name + "] " + this.id;
   }

   private void addProperty(int pid, String desc, int dt) {
      if (this.properties == null) {
         this.properties = new Vector<>();
      }

      OpcItem.Property p = new OpcItem.Property(pid, desc, dt);
      if (!this.properties.contains(p)) {
         this.properties.addElement(p);
      }
   }

   public static class Property {
      public int pid;
      public String description;
      public int dataType;

      public Property(int pid, String desc, int dt) {
         this.pid = pid;
         if (desc != null) {
            this.description = desc;
         } else {
            this.description = "";
         }

         this.dataType = dt;
      }

      @Override
      public boolean equals(Object o) {
         if (!(o instanceof OpcItem.Property)) {
            return false;
         } else {
            OpcItem.Property p = (OpcItem.Property)o;
            return this.pid == p.pid;
         }
      }

      @Override
      public int hashCode() {
         return this.pid;
      }

      @Override
      public String toString() {
         return "[" + this.pid + "," + this.dataType + "] " + this.description;
      }
   }
}
