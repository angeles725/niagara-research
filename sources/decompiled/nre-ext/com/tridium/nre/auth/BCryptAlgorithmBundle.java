package com.tridium.nre.auth;

public class BCryptAlgorithmBundle extends ScramAlgorithmBundle {
   private static BCryptAlgorithmBundle INSTANCE = null;
   private static final String TYPE = "bcrypt";
   private static final String VERSION = "1";

   private BCryptAlgorithmBundle() {
   }

   @Override
   public String getAlgorithmType() {
      return "bcrypt";
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
      return 248;
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
      return PasswordHashAlgorithm.bcrypt;
   }

   public static BCryptAlgorithmBundle getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new BCryptAlgorithmBundle();
      }

      return INSTANCE;
   }
}
