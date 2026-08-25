package com.tridium.nre.auth;

public class GlibcSha256CryptAlgorithmBundle extends ScramAlgorithmBundle {
   private static GlibcSha256CryptAlgorithmBundle INSTANCE = null;
   private static final String TYPE = "glibc-sha256";
   private static final String VERSION = "1";

   private GlibcSha256CryptAlgorithmBundle() {
   }

   @Override
   public String getAlgorithmType() {
      return "glibc-sha256";
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
      return 344;
   }

   @Override
   public String getMacAlgorithmName() {
      return "HmacSha256";
   }

   @Override
   public String getSecretKeySpecAlgorithmName() {
      return "HmacSHA256";
   }

   @Override
   public String getMessageDigestAlgorithmName() {
      return "Sha-256";
   }

   @Override
   public PasswordHashAlgorithm getKeyDerivationAlgorithmType() {
      return PasswordHashAlgorithm.glibc_sha256;
   }

   public static GlibcSha256CryptAlgorithmBundle getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new GlibcSha256CryptAlgorithmBundle();
      }

      return INSTANCE;
   }
}
