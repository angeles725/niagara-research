package com.tridium.niagarad.license;

import java.util.Properties;

public class Feature {
   String key;
   String vendorName;
   String featureName;
   long expiration;
   Properties props;

   public Feature(String pVendorName, String pFeatureName, long pExpiration) {
      this.vendorName = pVendorName;
      this.featureName = pFeatureName;
      if (this.vendorName != null && this.featureName != null) {
         this.key = LicenseUtil.toKey(this.vendorName, this.featureName);
      } else {
         this.key = null;
      }

      this.expiration = pExpiration;
      this.props = new Properties();
   }

   public String getVendorName() {
      return this.vendorName;
   }

   public String getFeatureName() {
      return this.featureName;
   }

   public boolean isExpired() {
      return System.currentTimeMillis() > this.getExpiration();
   }

   public boolean check() {
      return !this.isExpired();
   }

   public long getExpiration() {
      return this.expiration;
   }

   public String get(String key) {
      return this.props.getProperty(key);
   }

   public String get(String key, String def) {
      return this.props.getProperty(key, def);
   }

   public boolean getb(String key, boolean def) {
      return Boolean.valueOf(this.props.getProperty(key, String.valueOf(def)));
   }

   public int geti(String key, int def) {
      return Integer.valueOf(this.props.getProperty(key, String.valueOf(def)));
   }

   void merge(Feature x) {
      if (x.expiration > this.expiration) {
         this.expiration = x.expiration;
      }
   }
}
