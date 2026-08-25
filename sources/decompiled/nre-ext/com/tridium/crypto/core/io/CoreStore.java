package com.tridium.crypto.core.io;

import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.KeyRing;
import java.io.File;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CoreStore implements ICoreStore {
   protected long lastModified = 0L;
   protected File storeFile = null;
   protected ICoreCryptoManager mgr;
   protected ISecurityInfoProvider secInfo;
   protected String keyName;
   protected final String storeName;
   protected boolean isReadOnly = true;
   protected Logger log = Logger.getLogger("crypto");

   public CoreStore(ICoreCryptoManager mgr, File storeFile, ISecurityInfoProvider secInfo, String keyName, String storeName) throws Exception {
      this.mgr = mgr;
      this.storeFile = storeFile;
      this.secInfo = secInfo;
      this.keyName = keyName;
      this.storeName = storeName;
      KeyRing kr = secInfo.getKeyRing();
      AccessController.doPrivileged(() -> {
         if (keyName != null && kr.getKey(keyName) == null) {
            kr.createKey(keyName, true);
         }

         return null;
      });
   }

   public final void checkLastModified() {
      if (this.storeFile.lastModified() != this.lastModified) {
         if (this.lastModified == 0L) {
            this.lastModified = this.storeFile.lastModified();
         } else {
            try {
               if (this.log != null && this.log.isLoggable(Level.FINE)) {
                  this.log.fine("detected exemption store change. Reloading...");
               }

               this.load();
               this.lastModified = this.storeFile.lastModified();
            } catch (Exception var2) {
            }
         }
      }
   }

   @Override
   public long getLastModified() {
      return this.storeFile.lastModified();
   }

   final byte[] getKeyRingKey() throws Exception {
      KeyRing ring = this.secInfo.getKeyRing();
      return ring.getKey(this.keyName);
   }

   protected abstract void load() throws Exception;

   protected abstract void save() throws Exception;

   @Override
   public boolean isReadOnly() {
      return this.isReadOnly;
   }

   void setReadOnly(boolean isReadOnly) {
      this.isReadOnly = isReadOnly;
   }
}
