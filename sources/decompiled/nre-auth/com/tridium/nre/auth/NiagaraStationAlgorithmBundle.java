package com.tridium.nre.auth;

public class NiagaraStationAlgorithmBundle extends ScramAlgorithmBundle {
   private static NiagaraStationAlgorithmBundle INSTANCE = null;
   private static final String TYPE = "pbkdf2-sha256";
   private static final String VERSION = "1";

   private NiagaraStationAlgorithmBundle() {
   }

   public String getAlgorithmType() {
      return "pbkdf2-sha256";
   }

   public String getAlgorithmVersion() {
      return "1";
   }

   public String getKeyDerivationAlgorithmName() {
      return "PBKDF2WithHmacSHA256";
   }

   public int getKeyLength() {
      return 256;
   }

   public String getMacAlgorithmName() {
      return "HmacSha256";
   }

   public String getSecretKeySpecAlgorithmName() {
      return "HmacSHA256";
   }

   public String getMessageDigestAlgorithmName() {
      return "Sha-256";
   }

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
