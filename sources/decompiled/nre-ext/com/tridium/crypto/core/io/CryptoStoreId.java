package com.tridium.crypto.core.io;

public enum CryptoStoreId {
   USER_KEY_STORE("userKeyStore"),
   USER_TRUST_STORE("userTrustStore"),
   USER_UNTRUSTED_STORE("userUntrustedStore"),
   SYSTEM_TRUST_STORE("systemTrustStore"),
   USER_EXEMPTION_STORE("userExemptionStore");

   private final String locName;

   CryptoStoreId(String locName) {
      this.locName = locName;
   }

   public String getValue() {
      return this.locName;
   }

   public static CryptoStoreId getEnum(String val) {
      for (CryptoStoreId v : values()) {
         if (v.getValue().equalsIgnoreCase(val)) {
            return v;
         }
      }

      throw new IllegalArgumentException("invalid key store location string");
   }

   @Override
   public String toString() {
      return this.locName;
   }
}
