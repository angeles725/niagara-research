package com.tridium.crypto.core.async;

import com.tridium.crypto.core.io.ICoreKeyStore;

public class ResetUserKeyStoreEntry implements IAsyncCertRequestEntry {
   private final ICoreKeyStore keyStore;

   public ResetUserKeyStoreEntry(ICoreKeyStore keyStore) {
      this.keyStore = keyStore;
   }

   @Override
   public ICoreKeyStore getKeyStore() {
      return this.keyStore;
   }
}
