package com.tridium.nre.security;

import java.security.Provider;

public interface CryptoProvider {
   Provider getProvider();

   String getDefaultKeyStoreType();

   boolean isFips();

   CryptoProvider.CryptoError parseThrowable(Throwable var1);

   enum CryptoError {
      FIPS_PASSWORD_LENGTH;
   }
}
