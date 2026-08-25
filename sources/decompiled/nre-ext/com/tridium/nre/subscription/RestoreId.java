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
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;

public class RestoreId {
   private static File RESTORE_ID_FILE = new File(SubscriptionLicenseUtil.getSubscriptionDirectory(), ".restoreId");
   private static File restoreIdFileBaseDirectory;
   private static RestoreId INSTANCE;
   private static final Logger LOG = Logger.getLogger("licensing.subscription");
   private static final String KEY_ALIAS = "baja.licensing.subscription.restoreId";
   private static final AesAlgorithmBundle ALGORITHM_BUNDLE = AesAlgorithmBundle.make(256);
   private static final int FILE_LOCK_WAIT_TIMEOUT = 5000;

   private RestoreId() {
   }

   public static synchronized RestoreId getInstance(File baseDir) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(EntitlementUtil.ENTITLEMENT_WORKFLOW_PERMISSION);
      }

      restoreIdFileBaseDirectory = baseDir;
      RESTORE_ID_FILE = new File(baseDir, ".restoreId");
      if (INSTANCE == null) {
         INSTANCE = new RestoreId();
      }

      return INSTANCE;
   }

   public static synchronized RestoreId getInstance() {
      return getInstance(SubscriptionLicenseUtil.getSubscriptionDirectory());
   }

   public synchronized String get() {
      try {
         String value = this.read();
         if (value == null) {
            value = UUID.randomUUID().toString();
            String writeValue = value;

            try {
               AccessController.doPrivileged(() -> {
                  this.write(writeValue);
                  return null;
               });
            } catch (PrivilegedActionException pae) {
               Exception rootException = pae.getException();
               if (rootException instanceof IOException) {
                  throw (IOException)rootException;
               }

               throw new IOException("Failed to write initial restoreId", pae);
            }
         }

         return value;
      } catch (IOException ioe) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.SEVERE, "Failed to read restoreId", ioe);
         } else {
            LOG.severe("Failed to read restoreId");
         }

         throw new EntitlementException("Failed to read restoreId");
      }
   }

   synchronized String regenerate() {
      try {
         this.write(UUID.randomUUID().toString());
      } catch (IOException ioe) {
         if (RESTORE_ID_FILE.exists() && !RESTORE_ID_FILE.delete()) {
            throw new EntitlementException("Failed to regenerate subscription license restore id: " + ioe.getLocalizedMessage());
         }
      }

      return this.get();
   }

   private void write(String value) throws IOException {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         if (RESTORE_ID_FILE.exists()) {
            sm.checkPermission(EntitlementUtil.RESET_ENTITLEMENT_PERMISSION);
         } else {
            sm.checkPermission(EntitlementUtil.ENTITLEMENT_WORKFLOW_PERMISSION);
         }
      }

      File subscriptionLicenseDirectory = SubscriptionLicenseUtil.getSubscriptionDirectory();
      if (!subscriptionLicenseDirectory.exists() && !subscriptionLicenseDirectory.mkdirs()) {
         throw new IOException("Failed to create subscription license directory '" + subscriptionLicenseDirectory + "'");
      }

      FileLock lock = null;

      try {
         lock = FileLock.lock(RESTORE_ID_FILE, 5000);

         try (
            FileOutputStream fos = new FileOutputStream(RESTORE_ID_FILE);
            DataOutputStream out = new DataOutputStream(fos);
         ) {
            byte[] ivBytes = new byte[16];
            new SecureRandom().nextBytes(ivBytes);

            try {
               AccessController.doPrivileged(() -> {
                  String[] data = new String[ALGORITHM_BUNDLE.getDataElementCount()];
                  data[1] = ByteArrayUtil.toHexString(this.encrypt(value, ivBytes));
                  data[0] = ByteArrayUtil.toHexString(ivBytes);
                  out.writeUTF(ALGORITHM_BUNDLE.encode(data));
                  return null;
               });
            } catch (PrivilegedActionException pae) {
               Exception e = pae.getException();
               if (e instanceof IOException) {
                  throw (IOException)e;
               }

               throw new IOException(e.getLocalizedMessage(), e);
            }
         }
      } catch (FileLockException e) {
         throw new IOException("Failed to lock restoreId file", e);
      } finally {
         if (lock != null) {
            lock.unlock();
         }
      }
   }

   private byte[] encrypt(String value, byte[] iv) throws IOException {
      try {
         KeyRing keyRing = SubscriptionLicenseUtil.getInstance().getKeyRing(restoreIdFileBaseDirectory);
         Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, "baja.licensing.subscription.restoreId");
         return pwdMgr.encrypt(value.getBytes(StandardCharsets.UTF_8), iv);
      } catch (Exception e) {
         throw new IOException("Failed to encrypt", e);
      }
   }

   private String read() throws IOException {
      if (!RESTORE_ID_FILE.exists()) {
         return null;
      }

      try (
         FileInputStream fis = new FileInputStream(RESTORE_ID_FILE);
         DataInputStream in = new DataInputStream(fis);
      ) {
         String[] data = ALGORITHM_BUNDLE.decode(in.readUTF());
         if (data != null && data.length >= 2) {
            try {
               return AccessController.doPrivileged(() -> {
                  try (SecretBytes secretBytes = this.decrypt(ByteArrayUtil.hexStringToBytes(data[1]), ByteArrayUtil.hexStringToBytes(data[0]))) {
                     return new String(secretBytes.get(), StandardCharsets.UTF_8);
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
         } else {
            throw new IOException("Data length < 2");
         }
      }
   }

   private SecretBytes decrypt(byte[] cipher, byte[] iv) throws IOException {
      try {
         KeyRing keyRing = SubscriptionLicenseUtil.getInstance().getKeyRing();
         Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, "baja.licensing.subscription.restoreId");
         return pwdMgr.decryptSecret(cipher, iv);
      } catch (Exception e) {
         throw new IOException("Failed to decrypt", e);
      }
   }
}
