package com.tridium.nre.auth;

public class NiagaraStationAlgorithmBundle extends ScramAlgorithmBundle {
   private static NiagaraStationAlgorithmBundle INSTANCE = null;
   private static final String TYPE = "pbkdf2-sha256";
   private static final String VERSION = "1";

   private NiagaraStationAlgorithmBundle() {
   }

   @Override
   public String getAlgorithmType() {
      return "pbkdf2-sha256";
   }

   @Override
   public String getAlgorithmVersion() {
      return "1";
   }

   @Override
   public String getKeyDerivationAlgorithmName() {
      return "PBKDF2WithHmacSHA256";
   }

   @Override
   public int getKeyLength() {
      return 256;
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
      return PasswordHashAlgorithm.pbkdf2;
   }

   public static NiagaraStationAlgorithmBundle getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new NiagaraStationAlgorithmBundle();
      }

      return INSTANCE;
   }
}
