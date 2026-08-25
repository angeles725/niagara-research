package com.tridium.nre.security;

public final class NullAlgorithmBundle extends KeyDerivationAlgorithmBundle {
   private static final NullAlgorithmBundle INSTANCE = new NullAlgorithmBundle();

   @Override
   public String getAlgorithmType() {
      return "null";
   }

   @Override
   public String getAlgorithmVersion() {
      return "1";
   }

   @Override
   public int getDataElementCount() {
      return 0;
   }

   @Override
   public String getKeyDerivationAlgorithmName() {
      return "null";
   }

   @Override
   public int getKeyLength() {
      return 0;
   }

   public static NullAlgorithmBundle getInstance() {
      return INSTANCE;
   }
}
