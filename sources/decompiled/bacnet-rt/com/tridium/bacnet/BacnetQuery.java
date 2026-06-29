package com.tridium.bacnet;

import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdQueryList;

public class BacnetQuery implements OrdQuery {
   private final String body;
   private String device = null;
   private String domain = null;
   private String object = null;
   private String property = null;
   private String index = null;
   private boolean hasDomain = false;
   private boolean hasObject = false;
   private boolean hasProperty = false;
   private boolean hasIndex = false;

   BacnetQuery(String body) {
      this.body = body;
      this.parse();
   }

   public String getScheme() {
      return "bac";
   }

   public String getBody() {
      return this.body;
   }

   public boolean isHost() {
      return false;
   }

   public boolean isSession() {
      return false;
   }

   public void normalize(OrdQueryList list, int index) {
      if (list.isSameScheme(index, index + 1)) {
         list.remove(index);
      }
   }

   @Override
   public String toString() {
      return "bac:" + this.body;
   }

   private void parse() {
      if (this.body != null) {
         int obj = this.body.indexOf(";");
         int prop = this.body.indexOf("/");
         int ndxS = this.body.indexOf("[");
         int ndxF = this.body.indexOf("]");
         int domS = this.body.indexOf("{");
         int domF = this.body.indexOf("}");
         if (domS < 0) {
            this.hasDomain = false;
            this.domain = null;
         } else {
            this.hasDomain = true;
            this.domain = this.body.substring(domS + 1, domF);
         }

         if (obj < 0) {
            this.hasObject = false;
            this.hasProperty = false;
            this.hasIndex = false;
            this.device = this.hasDomain ? this.body.substring(0, domS) : this.body;
            this.object = null;
            this.property = null;
            this.index = null;
         } else {
            this.hasObject = true;
            this.device = this.body.substring(0, obj);
            if (prop < 0) {
               this.hasProperty = false;
               this.hasIndex = false;
               this.object = this.hasDomain ? this.body.substring(obj + 1, domS) : this.body.substring(obj + 1);
               this.property = null;
               this.index = null;
            } else {
               this.hasProperty = true;
               this.object = this.body.substring(obj + 1, prop);
               if (ndxS >= 0 && ndxF >= 0 && ndxF > ndxS) {
                  this.hasIndex = true;
                  this.property = this.body.substring(prop + 1, ndxS);
                  this.index = this.body.substring(ndxS + 1, ndxF);
               } else {
                  this.hasIndex = false;
                  this.property = this.hasDomain ? this.body.substring(prop + 1, domS) : this.body.substring(prop + 1);
                  this.index = null;
               }
            }
         }
      }
   }

   public boolean hasDomain() {
      return this.hasDomain;
   }

   public boolean hasObject() {
      return this.hasObject;
   }

   public boolean hasProperty() {
      return this.hasProperty;
   }

   public boolean hasIndex() {
      return this.hasIndex;
   }

   public String getDevice() {
      return this.device;
   }

   public String getDomain() {
      return this.domain;
   }

   public String getObject() {
      return this.object;
   }

   public String getProperty() {
      return this.property;
   }

   public String getIndex() {
      return this.index;
   }

   public void dump() {
      System.out.println("body:" + this.body);
      System.out.println("  device=" + this.device);
      System.out.println("  object=" + this.object);
      System.out.println("  property=" + this.property);
      System.out.println("  index=" + this.index);
      System.out.println("  domain=" + this.domain);
   }
}
