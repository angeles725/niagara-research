package com.tridium.nre.auth;

public class QnxPlatformAlgorithmBundle extends ScramAlgorithmBundle {
   private static QnxPlatformAlgorithmBundle INSTANCE = null;
   private static final String TYPE = "pbkdf2-sha512";
   private static final String VERSION = "1";

   private QnxPlatformAlgorithmBundle() {
   }

   @Override
   public String getAlgorithmType() {
      return "pbkdf2-sha512";
   }

   @Override
   public String getAlgorithmVersion() {
      return "1";
   }

   @Override
   public String getKeyDerivationAlgorithmName() {
      return "PBKDF2WithHmacSHA512";
   }

   @Override
   public int getKeyLength() {
      return 512;
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
      return PasswordHashAlgorithm.pbkdf2;
   }

   public static QnxPlatformAlgorithmBundle getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new QnxPlatformAlgorithmBundle();
      }

      return INSTANCE;
   }
}
