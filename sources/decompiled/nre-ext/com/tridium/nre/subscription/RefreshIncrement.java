package com.tridium.nre.subscription;

import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.util.FileLock;
import com.tridium.nre.util.FileLockException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;

public final class RefreshIncrement {
   private static final Logger LOG = Logger.getLogger("licensing.subscription");
   private static final String KEY_ALIAS = "baja.licensing.subscription.refreshIncrement";
   private static final AesAlgorithmBundle ALGORITHM_BUNDLE = AesAlgorithmBundle.make(256);
   private static final File REFRESH_INCREMENT_FILE = new File(SubscriptionLicenseUtil.getSubscriptionDirectory(), ".refreshIncrement");
   private static final int FILE_LOCK_WAIT_TIMEOUT = 5000;
   private static final int REFRESH_INCREMENT_RANDOMNESS_BOUND = 1000;
   private static RefreshIncrement INSTANCE;

   private RefreshIncrement() {
   }

   public static synchronized RefreshIncrement getInstance() {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(EntitlementUtil.ENTITLEMENT_WORKFLOW_PERMISSION);
      }

      if (INSTANCE == null) {
         INSTANCE = new RefreshIncrement();
      }

      return INSTANCE;
   }

   public synchronized int getAndIncrement() {
      try {
         int value = this.read();
         this.write(value + 1, false);
         return value;
      } catch (IOException ioe) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.SEVERE, "Failed to update refresh increment", ioe);
         } else {
            LOG.severe("Failed to update refresh increment");
         }

         throw new EntitlementException("Failed to update subscription refresh increment");
      }
   }

   synchronized int reset() {
      int resetValue = 0;

      try {
         int currentRefreshIncrement = this.read();
         if (currentRefreshIncrement > 0) {
            resetValue = SubscriptionLicenseUtil.SECURE_RANDOM.nextInt(currentRefreshIncrement);
         }

         this.write(resetValue, true);
      } catch (IOException ioe) {
         if (REFRESH_INCREMENT_FILE.exists() && !REFRESH_INCREMENT_FILE.delete()) {
            throw new EntitlementException("Failed to reset subscription license refresh increment: " + ioe.getLocalizedMessage());
         }
      }

      return resetValue;
   }

   private void write(int value, boolean isReset) throws IOException {
      if (value < 0) {
         throw new IOException("Invalid refresh increment value");
      }

      SecurityManager sm = System.getSecurityManager();
      if (isReset && sm != null) {
         sm.checkPermission(EntitlementUtil.RESET_ENTITLEMENT_PERMISSION);
      } else if (sm != null) {
         sm.checkPermission(EntitlementUtil.ENTITLEMENT_WORKFLOW_PERMISSION);
      }

      try {
         AccessController.doPrivileged(() -> {
            File subscriptionLicenseDirectory = SubscriptionLicenseUtil.getSubscriptionDirectory();
            if (!subscriptionLicenseDirectory.exists() && !subscriptionLicenseDirectory.mkdirs()) {
               throw new IOException("Failed to create subscription license directory '" + subscriptionLicenseDirectory + "'");
            }

            FileLock lock = null;

            try {
               lock = FileLock.lock(REFRESH_INCREMENT_FILE, 5000);

               try (
                  FileOutputStream fos = new FileOutputStream(REFRESH_INCREMENT_FILE);
                  DataOutputStream out = new DataOutputStream(fos);
               ) {
                  byte[] ivBytes = new byte[16];
                  SubscriptionLicenseUtil.SECURE_RANDOM.nextBytes(ivBytes);
                  String[] data = new String[ALGORITHM_BUNDLE.getDataElementCount()];
                  data[1] = ByteArrayUtil.toHexString(this.encrypt(value, ivBytes));
                  data[0] = ByteArrayUtil.toHexString(ivBytes);
                  out.writeUTF(ALGORITHM_BUNDLE.encode(data));
                  return null;
               }
            } catch (FileLockException e) {
               throw new IOException("Failed to lock key file", ex);
            } finally {
               if (lock != null) {
                  lock.unlock();
               }
            }
         });
      } catch (PrivilegedActionException pae) {
         Exception e = pae.getException();
         if (e instanceof IOException) {
            throw (IOException)e;
         } else {
            throw new IOException(e.getLocalizedMessage(), e);
         }
      }
   }

   private byte[] encrypt(int value, byte[] iv) throws IOException {
      try {
         KeyRing keyRing = SubscriptionLicenseUtil.getInstance().getKeyRing();
         Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, "baja.licensing.subscription.refreshIncrement");
         byte[] valueBytes = new byte[4];
         ByteArrayUtil.writeInt(valueBytes, 0, value);
         return pwdMgr.encrypt(valueBytes, iv);
      } catch (Exception e) {
         throw new IOException("Failed to encrypt", e);
      }
   }

   private int read() throws IOException {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(EntitlementUtil.ENTITLEMENT_WORKFLOW_PERMISSION);
      }

      try {
         return AccessController.doPrivileged(() -> {
            if (!REFRESH_INCREMENT_FILE.exists()) {
               return SubscriptionLicenseUtil.SECURE_RANDOM.nextInt(1000);
            }

            try (
               FileInputStream fis = new FileInputStream(REFRESH_INCREMENT_FILE);
               DataInputStream in = new DataInputStream(fis);
            ) {
               String[] data = ALGORITHM_BUNDLE.decode(in.readUTF());
               if (data != null && data.length >= 2) {
                  try (SecretBytes secretBytes = this.decrypt(ByteArrayUtil.hexStringToBytes(data[1]), ByteArrayUtil.hexStringToBytes(data[0]))) {
                     return ByteArrayUtil.readInt(secretBytes.get(), 0);
                  }
               } else {
                  throw new IOException("Data length < 2");
               }
            }
         });
      } catch (PrivilegedActionException pae) {
         Exception e = pae.getException();
         if (e instanceof IOException) {
            throw (IOException)e;
         } else {
            throw new IOException(e.getLocalizedMessage(), e);
         }
      }
   }

   private SecretBytes decrypt(byte[] cipher, byte[] iv) throws IOException {
      try {
         KeyRing keyRing = SubscriptionLicenseUtil.getInstance().getKeyRing();
         Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, "baja.licensing.subscription.refreshIncrement");
         return pwdMgr.decryptSecret(cipher, iv);
      } catch (Exception e) {
         throw new IOException("Failed to decrypt", e);
      }
   }
}
