package com.tridium.nre.security;

import java.lang.reflect.InvocationTargetException;
import java.security.Provider;

public class BouncyCastleCryptoProvider implements CryptoProvider {
   private static final String DEFAULT_KEYSTORE_TYPE = "jceks";
   private final Provider provider;

   public BouncyCastleCryptoProvider() {
      try {
         this.provider = (Provider)Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
         e.printStackTrace();
         throw new IllegalArgumentException("Could not instantiate BouncyCastleCryptoProvider. Cause is:" + e.getLocalizedMessage());
      }
   }

   @Override
   public Provider getProvider() {
      return this.provider;
   }

   @Override
   public String getDefaultKeyStoreType() {
      return "jceks";
   }

   @Override
   public boolean isFips() {
      return false;
   }

   @Override
   public CryptoProvider.CryptoError parseThrowable(Throwable throwable) {
      return null;
   }
}
