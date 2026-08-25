package com.tridium.nre.auth;

public enum PasswordHashAlgorithm {
   pbkdf2("Standard PBKDF2 key derivation function"),
   glibc_sha256("Glibc 2.7 crypt implementation based on SHA-256"),
   glibc_sha512("Glibc 2.7 crypt implementation based on SHA-512"),
   bcrypt("Key derivation function based on Blowfish cipher");

   private String desc;

   PasswordHashAlgorithm(String desc) {
      this.desc = desc;
   }

   public String getDescription() {
      return this.desc;
   }

   public static PasswordHashAlgorithm valueOf(String name, PasswordHashAlgorithm defaultValue) {
      if (name == null) {
         return defaultValue;
      }

      try {
         return valueOf(name);
      } catch (IllegalArgumentException iae) {
         return defaultValue;
      }
   }
}
