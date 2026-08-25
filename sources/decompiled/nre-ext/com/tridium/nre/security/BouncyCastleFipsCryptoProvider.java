package com.tridium.nre.security;

import java.security.Provider;
import java.security.Security;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

public class BouncyCastleFipsCryptoProvider implements CryptoProvider {
   private static final String DEFAULT_KEYSTORE_TYPE = "bcfks";
   private final Provider provider;

   public BouncyCastleFipsCryptoProvider() {
      Provider provider = Security.getProvider("BCFIPS");
      if (provider == null) {
         provider = new BouncyCastleFipsProvider();
      }

      this.provider = provider;
   }

   @Override
   public Provider getProvider() {
      return this.provider;
   }

   @Override
   public String getDefaultKeyStoreType() {
      return "bcfks";
   }

   @Override
   public boolean isFips() {
      return CryptoServicesRegistrar.isInApprovedOnlyMode();
   }

   @Override
   public CryptoProvider.CryptoError parseThrowable(Throwable throwable) {
      return null;
   }
}
