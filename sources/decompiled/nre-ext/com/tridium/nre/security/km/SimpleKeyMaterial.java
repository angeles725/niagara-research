package com.tridium.nre.security.km;

import com.tridium.nre.security.KeyParameters;
import com.tridium.nre.security.RecoverableKeyData;
import com.tridium.nre.util.FileLock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.baja.nre.util.SecurityUtil;

final class SimpleKeyMaterial extends KeyMaterial {
   private File srcFile = null;
   private byte[] keyMaterial = null;
   private String keyName;

   SimpleKeyMaterial(String srcFile, String keyName) {
      this.srcFile = new File(srcFile, keyName);
      this.keyName = keyName;
   }

   SimpleKeyMaterial(File srcFile, String keyName) {
      this.srcFile = new File(srcFile, keyName);
      this.keyName = keyName;
   }

   @Override
   synchronized byte[] doGetKeyMaterial() throws Exception {
      if (this.keyMaterial != null) {
         return this.keyMaterial;
      }

      FileLock lock = null;
      if (this.srcFile != null && this.srcFile.exists()) {
         try {
            File parentDir = this.srcFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
               return null;
            }

            lock = FileLock.lock(this.srcFile, 10000);

            try (FileInputStream in = new FileInputStream(this.srcFile)) {
               byte[] data = new byte[(int)this.srcFile.length()];
               int bytesRead = in.read(data);
               if (bytesRead <= 0) {
                  return null;
               }

               this.keyMaterial = new byte[bytesRead];
               System.arraycopy(data, 0, this.keyMaterial, 0, bytesRead);
            }
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }

         return this.keyMaterial;
      } else {
         return null;
      }
   }

   @Override
   synchronized boolean doSetKeyMaterial(byte[] newKeyMaterial) throws Exception {
      FileLock lock = null;

      try {
         if (!this.srcFile.exists()) {
            if (newKeyMaterial == null) {
               return true;
            }

            File parentDir = this.srcFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
               return false;
            }

            if (!this.srcFile.createNewFile()) {
               return false;
            }
         }

         lock = FileLock.lock(this.srcFile, 10000);
         if (newKeyMaterial == null) {
            lock.unlock();
            lock = null;
            if (!this.srcFile.delete()) {
               return false;
            }

            if (this.keyMaterial != null) {
               SecurityUtil.zeroByteArray(this.keyMaterial);
               this.keyMaterial = null;
            }
         } else {
            try (FileOutputStream out = new FileOutputStream(this.srcFile)) {
               out.write(newKeyMaterial);
            }

            if (this.keyMaterial != null) {
               SecurityUtil.zeroByteArray(this.keyMaterial);
               this.keyMaterial = null;
            }

            this.keyMaterial = newKeyMaterial;
         }
      } finally {
         if (lock != null) {
            lock.unlock();
         }
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
      return this.srcFile.lastModified();
   }

   @Override
   protected boolean supportsKeyRecovery() {
      return false;
   }

   @Override
   protected boolean recoveryKeyExists() {
      throw new UnsupportedOperationException("SimpleKeyMaterial doesn't support key material recovery.");
   }

   @Override
   protected void createRecoveryKey() throws IOException {
      throw new UnsupportedOperationException("SimpleKeyMaterial doesn't support key material recovery.");
   }

   @Override
   protected RecoverableKeyData getRecoveryKey(KeyParameters keyParameters) {
      throw new UnsupportedOperationException("SimpleKeyMaterial doesn't support key material recovery.");
   }

   @Override
   protected void rollBackRecoveryKey() throws IOException {
      throw new UnsupportedOperationException("SimpleKeyMaterial doesn't support key material recovery.");
   }

   @Override
   protected void deleteRecoveryKey() throws IOException {
      throw new UnsupportedOperationException("SimpleKeyMaterial doesn't support key material recovery.");
   }
}
