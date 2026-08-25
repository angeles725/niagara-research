package com.tridium.nre.security;

public abstract class AbstractAesAlgorithmBundle extends EncryptionAlgorithmBundle {
   @Override
   public String getEncryptionAlgorithmName() {
      return "AES";
   }

   @Override
   public String getAlgorithmType() {
      return this.getName() + '-' + this.getKeySize();
   }

   public abstract int getIvIndex();

   public abstract int getCipherIndex();

   protected abstract String getName();

   public abstract String getAesTransformation();
}
