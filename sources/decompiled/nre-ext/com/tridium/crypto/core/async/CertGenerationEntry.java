package com.tridium.crypto.core.async;

import com.tridium.crypto.core.cert.NKeyPairGenerator;
import com.tridium.crypto.core.cert.NX509CertificateBuilder;
import com.tridium.crypto.core.io.ICoreKeyStore;
import com.tridium.nre.security.SecretChars;

public class CertGenerationEntry implements IAsyncCertRequestEntry {
   private final ICoreKeyStore keyStore;
   private final NX509CertificateBuilder certBuilder;
   private final SecretChars password;
   private final NKeyPairGenerator generator;

   public CertGenerationEntry(ICoreKeyStore keyStore, NX509CertificateBuilder certBuilder, SecretChars password, NKeyPairGenerator generator) {
      this.keyStore = keyStore;
      this.certBuilder = certBuilder;
      this.password = password != null ? password.newCopy() : null;
      this.generator = generator;
   }

   @Override
   public ICoreKeyStore getKeyStore() {
      return this.keyStore;
   }

   public NX509CertificateBuilder getCertificateBuilder() {
      return this.certBuilder;
   }

   public SecretChars getPassword() {
      return this.password;
   }

   public NKeyPairGenerator getGenerator() {
      return this.generator;
   }
}
