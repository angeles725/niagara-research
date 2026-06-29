package com.tridium.lonworks.xml;

public abstract class XLonData {
   private String name;
   public String shortName = null;

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public void addAttribute(String name, Object obj) {
   }

   @Override
   public String toString() {
      return "XLonData[name=" + this.name + ",shortName=" + this.shortName + "]";
   }
}
