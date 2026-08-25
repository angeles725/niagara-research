package com.tridium.nre.security.km;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.KeyParameters;
import com.tridium.nre.security.RecoverableKeyData;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.logging.Level;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;

public final class PlatformKeyMaterial extends KeyMaterial {
   private final File securityDirectory;
   private final String keyName;
   private byte[] keyMaterial;
   private final boolean isRecovery;
   private static final String RECOVERY_KEY_MARKER = ".rec";

   PlatformKeyMaterial(File securityDirectory, String keyName, boolean isRecovery) {
      if (KeyMaterial.LOG.isLoggable(Level.FINEST)) {
         KeyMaterial.LOG
            .finest(
               "Creating PlatformKeyMaterial with securityDirectory = '"
                  + securityDirectory
                  + "' keyName = '"
                  + keyName
                  + "' isRecovery = '"
                  + isRecovery
                  + "'"
            );
      }

      this.securityDirectory = securityDirectory;
      this.keyName = keyName;
      this.isRecovery = isRecovery;
   }

   @Override
   synchronized byte[] doGetKeyMaterial() throws Exception {
      if (this.keyMaterial != null) {
         return ByteArrayUtil.clone(this.keyMaterial);
      }

      this.keyMaterial = PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getKeyMaterial(this.securityDirectory, this.keyName);
      return this.keyMaterial != null ? ByteArrayUtil.clone(this.keyMaterial) : null;
   }

   @Override
   synchronized boolean doSetKeyMaterial(byte[] newKeyMaterial) throws Exception {
      if (!PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.setKeyMaterial(this.securityDirectory, this.keyName, newKeyMaterial)) {
         throw new Exception("Failed to set key material for '" + this.keyName + "'");
      }

      if (this.keyMaterial != null) {
         SecurityUtil.zeroByteArray(this.keyMaterial);
         this.keyMaterial = null;
      }

      if (newKeyMaterial != null) {
         this.keyMaterial = ByteArrayUtil.clone(newKeyMaterial);
      }

      return true;
   }

   @Override
   synchronized boolean doRefreshKeyMaterial() throws Exception {
      if (this.keyMaterial != null) {
         SecurityUtil.zeroByteArray(this.keyMaterial);
         this.keyMaterial = null;
      }

      return this.getKeyMaterial() != null;
   }

   @Override
   long doGetLastModified() throws Exception {
      return PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getKeyMaterialLastModified(this.securityDirectory, this.keyName);
   }

   @Override
   protected boolean supportsKeyRecovery() {
      return PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.supportsKeyMaterialRecovery();
   }

   @Override
   protected boolean recoveryKeyExists() {
      return PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getKeyMaterial(this.securityDirectory, this.keyName + ".rec") != null;
   }

   @Override
   protected void createRecoveryKey() throws IOException {
      try {
         if (this.isRecovery) {
            throw new Exception("Can not create recovery key for recovery key");
         }

         if (this.getKeyMaterial() == null) {
            throw new Exception("Failed to obtain key material");
         }

         if (PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getKeyMaterial(this.securityDirectory, this.keyName + ".rec") != null) {
            KeyMaterial.LOG.warning("Overwriting existing recovery key");
         }

         if (!PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE
            .setKeyMaterial(this.securityDirectory, this.keyName + ".rec", this.keyMaterial)) {
            throw new IOException("Failed to set recovery key material");
         }
      } catch (Exception e) {
         throw new IOException("Failed to create recovery key for '" + this.keyName + "'", e);
      }
   }

   @Override
   protected RecoverableKeyData getRecoveryKey(KeyParameters keyParameters) {
      return new PlatformKeyMaterial(this.securityDirectory, this.keyName + ".rec", true);
   }

   @Override
   protected void rollBackRecoveryKey() throws IOException {
      try {
         if (this.isRecovery) {
            throw new Exception("Can not roll recovery key for recovery key");
         }

         if (!this.recoveryKeyExists()) {
            throw new Exception("Recovery key does not exist");
         }

         byte[] recoveredKey = ((PlatformKeyMaterial)this.getRecoveryKey(null)).getKeyMaterial();
         if (recoveredKey == null) {
            throw new Exception("Recovery key was null");
         }

         if (!this.setKeyMaterial(recoveredKey)) {
            throw new Exception("Failed to set key material to recovery key");
         }
      } catch (Exception e) {
         throw new IOException("Could not roll back key material for '" + this.keyName + "':", e);
      }
   }

   @Override
   protected void deleteRecoveryKey() throws IOException {
      if (this.isRecovery) {
         throw new IOException("Can not delete recovery key for recovery key");
      }

      if (this.recoveryKeyExists()
         && !PlatformKeyMaterial.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.setKeyMaterial(this.securityDirectory, this.keyName + ".rec", null)) {
         throw new IOException("Failed to delete key material for '" + this.keyName + ".rec" + "'");
      }
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
