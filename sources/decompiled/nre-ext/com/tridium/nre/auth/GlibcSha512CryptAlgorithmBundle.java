package com.tridium.nre.auth;

public class GlibcSha512CryptAlgorithmBundle extends ScramAlgorithmBundle {
   private static GlibcSha512CryptAlgorithmBundle INSTANCE = null;
   private static final String TYPE = "glibc-sha512";
   private static final String VERSION = "1";

   private GlibcSha512CryptAlgorithmBundle() {
   }

   @Override
   public String getAlgorithmType() {
      return "glibc-sha512";
   }

   @Override
   public String getAlgorithmVersion() {
      return "1";
   }

   @Override
   public String getKeyDerivationAlgorithmName() {
      return this.getKeyDerivationAlgorithmType().toString();
   }

   @Override
   public int getKeyLength() {
      return 688;
   }

   @Override
   public String getMacAlgorithmName() {
      return "HmacSha512";
   }

   @Override
   public String getSecretKeySpecAlgorithmName() {
      return "HmacSHA512";
   }

   @Override
   public String getMessageDigestAlgorithmName() {
      return "Sha-512";
   }

   @Override
   public PasswordHashAlgorithm getKeyDerivationAlgorithmType() {
      return PasswordHashAlgorithm.glibc_sha512;
   }

   public static GlibcSha512CryptAlgorithmBundle getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new GlibcSha512CryptAlgorithmBundle();
      }

      return INSTANCE;
   }
}
