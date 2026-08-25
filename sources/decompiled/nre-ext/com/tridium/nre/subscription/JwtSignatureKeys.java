package com.tridium.nre.subscription;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.util.FileLock;
import com.tridium.nre.util.FileLockException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PrivilegedActionException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.FileUtil;

public final class JwtSignatureKeys {
   private final JwtSignatureKeys.KeyPairFile KEY_PAIR_ROTATION_FILE = new JwtSignatureKeys.KeyPairFile(
      SubscriptionLicenseUtil.getSubscriptionDirectory(), ".ecKeyPair.r1"
   );
   private final File keyPairBaseDirectory;
   private final JwtSignatureKeys.KeyPairFile keyPairFile;
   private static final Logger LOG = Logger.getLogger("licensing.subscription");
   private static final String KEY_ALIAS = "baja.licensing.subscription.ecKeyPair";
   private static final AesAlgorithmBundle ALGORITHM_BUNDLE = AesAlgorithmBundle.make(256);
   private static final String KEY_CURVE_NAME = "secp256r1";
   private static final int FILE_LOCK_WAIT_TIMEOUT = 5000;
   private static final NiagaraBasicPermission GET_ENTITLEMENT_PRIVATE_KEY_PERMISSION = new NiagaraBasicPermission("GET_ENTITLEMENT_PRIVATE_KEY");
   private static final NiagaraBasicPermission ROTATE_ENTITLEMENT_KEYS_PERMISSION = new NiagaraBasicPermission("ROTATE_ENTITLEMENT_KEYS");
   private static final JwtSignatureKeys DEFAULT_INSTANCE = new JwtSignatureKeys(SubscriptionLicenseUtil.getSubscriptionDirectory());

   private JwtSignatureKeys(File baseDir) {
      this.keyPairBaseDirectory = baseDir;
      this.keyPairFile = new JwtSignatureKeys.KeyPairFile(baseDir, ".ecKeyPair");
   }

   public static JwtSignatureKeys getInstance() {
      return DEFAULT_INSTANCE;
   }

   public static JwtSignatureKeys getInstance(File baseDir) {
      return new JwtSignatureKeys(baseDir);
   }

   synchronized boolean isGenerated() {
      return this.keyPairFile.exists();
   }

   public synchronized Instant getGenerationTime() {
      try {
         return this.keyPairFile.readGenerationTime();
      } catch (IOException e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.SEVERE, "Failed to read generation time", e);
         } else {
            LOG.severe("Failed to read generation time");
         }

         throw new EntitlementException("Failed to read subscription refresh key generation time");
      }
   }

   synchronized KeyPair getKeys() {
      try {
         return this.readKeysGenerateIfNeeded();
      } catch (IOException e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.SEVERE, "Failed to read keys", e);
         } else {
            LOG.severe("Failed to read keys");
         }

         throw new EntitlementException("Failed to read subscription refresh keys");
      }
   }

   synchronized PublicKey getPublicKey() {
      try {
         return this.readPublicKeyGenerateKeysIfNeeded();
      } catch (IOException e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.SEVERE, "Failed to read public key", e);
         } else {
            LOG.severe("Failed to read public key");
         }

         throw new EntitlementException("Failed to read subscription refresh public key");
      }
   }

   synchronized PrivateKey getPrivateKey() {
      try {
         return this.readPrivateKeyGenerateKeysIfNeeded();
      } catch (IOException e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.SEVERE, "Failed to read private key", e);
         } else {
            LOG.severe("Failed to read private key");
         }

         throw new EntitlementException("Failed to read subscription refresh private key");
      }
   }

   synchronized JwtSignatureKeys.KeyRotation initKeyRotation() throws KeyRotationException {
      return new JwtSignatureKeys.KeyRotation();
   }

   private KeyPair generateKeys() throws IOException {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(GET_ENTITLEMENT_PRIVATE_KEY_PERMISSION);
      }

      try {
         KeyPair keys = CertUtils.generateEcKeyPair("secp256r1");
         this.keyPairFile.write(keys);
         return keys;
      } catch (IOException e) {
         throw e;
      } catch (Exception e) {
         throw new IOException("Failed to generate keys", e);
      }
   }

   private KeyPair readKeysGenerateIfNeeded() throws IOException {
      return this.keyPairFile.exists() ? this.keyPairFile.readKeys() : this.generateKeys();
   }

   private PublicKey readPublicKeyGenerateKeysIfNeeded() throws IOException {
      if (this.keyPairFile.exists()) {
         return this.keyPairFile.readPublicKey();
      }

      try {
         return AccessController.doPrivileged(() -> this.generateKeys().getPublic());
      } catch (PrivilegedActionException pae) {
         if (pae.getException() instanceof IOException) {
            throw (IOException)pae.getException();
         } else {
            throw new IOException("Unknown error", pae);
         }
      }
   }

   private PrivateKey readPrivateKeyGenerateKeysIfNeeded() throws IOException {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(GET_ENTITLEMENT_PRIVATE_KEY_PERMISSION);
      }

      return this.keyPairFile.exists() ? this.keyPairFile.readPrivateKey() : this.generateKeys().getPrivate();
   }

   private final class KeyPairFile {
      private final File file;

      KeyPairFile(File baseDir, String filename) {
         this.file = new File(baseDir, filename);
      }

      File getFile() {
         return this.file;
      }

      boolean exists() {
         return this.file.exists();
      }

      KeyPair readKeys() throws IOException {
         try (SecretBytes data = this.read()) {
            return this.decodeKeys(data);
         }
      }

      PublicKey readPublicKey() throws IOException {
         try (SecretBytes data = this.read()) {
            return this.decodePublicKey(data);
         }
      }

      PrivateKey readPrivateKey() throws IOException {
         try (SecretBytes data = this.read()) {
            return this.decodePrivateKey(data);
         }
      }

      Instant readGenerationTime() throws IOException {
         if (this.file.exists()) {
            try (SecretBytes data = this.read()) {
               return this.decodeGenerationTime(data);
            }
         } else {
            return Instant.EPOCH;
         }
      }

      private SecretBytes read() throws IOException {
         if (!this.file.exists()) {
            throw new IOException("Key file is missing");
         }

         FileLock lock = null;

         try {
            lock = FileLock.lock(this.file, 5000);

            try (
               FileInputStream fis = new FileInputStream(this.file);
               DataInputStream in = new DataInputStream(fis);
            ) {
               String[] data = JwtSignatureKeys.ALGORITHM_BUNDLE.decode(in.readUTF());
               if (data != null && data.length >= 2) {
                  return this.decrypt(ByteArrayUtil.hexStringToBytes(data[1]), ByteArrayUtil.hexStringToBytes(data[0]));
               } else {
                  throw new IOException("Data length < 2");
               }
            }
         } catch (FileLockException e) {
            throw new IOException("Failed to lock key file", e);
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }
      }

      private SecretBytes decrypt(byte[] cipher, byte[] iv) throws IOException {
         try {
            KeyRing keyRing = SubscriptionLicenseUtil.getInstance().getKeyRing(JwtSignatureKeys.this.keyPairBaseDirectory);
            Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, "baja.licensing.subscription.ecKeyPair");
            return pwdMgr.decryptSecret(cipher, iv);
         } catch (Exception e) {
            throw new IOException("Failed to decrypt", e);
         }
      }

      void write(KeyPair keys) throws IOException {
         File subscriptionLicenseDirectory = SubscriptionLicenseUtil.getSubscriptionDirectory();
         if (!subscriptionLicenseDirectory.exists() && !subscriptionLicenseDirectory.mkdirs()) {
            throw new IOException("Failed to create subscription license directory '" + subscriptionLicenseDirectory + "'");
         }

         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Failed to create subscription key pair file");
         }

         FileLock lock = null;

         try {
            lock = FileLock.lock(this.file, 5000);

            try (
               FileOutputStream fos = new FileOutputStream(this.file);
               DataOutputStream out = new DataOutputStream(fos);
            ) {
               byte[] ivBytes = new byte[16];
               new SecureRandom().nextBytes(ivBytes);
               String[] data = new String[JwtSignatureKeys.ALGORITHM_BUNDLE.getDataElementCount()];
               data[1] = ByteArrayUtil.toHexString(this.encrypt(keys, ivBytes));
               data[0] = ByteArrayUtil.toHexString(ivBytes);
               out.writeUTF(JwtSignatureKeys.ALGORITHM_BUNDLE.encode(data));
            }
         } catch (IOException ioe) {
            try {
               this.file.delete();
            } catch (Exception var50) {
            }

            if (JwtSignatureKeys.LOG.isLoggable(Level.FINE)) {
               JwtSignatureKeys.LOG.log(Level.SEVERE, "Failed to write license refresh state", ioe);
            } else {
               JwtSignatureKeys.LOG.severe("Failed to write license refresh state");
            }

            throw ioe;
         } catch (FileLockException e) {
            throw new IOException("Failed to lock key file", e);
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }
      }

      private KeyPair decodeKeys(SecretBytes encoded) throws IOException {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(JwtSignatureKeys.GET_ENTITLEMENT_PRIVATE_KEY_PERMISSION);
         }

         try (
            ByteArrayInputStream bin = new ByteArrayInputStream(encoded.get());
            ObjectInputStream in = new ObjectInputStream(bin);
         ) {
            in.readObject();
            PublicKey publicKey = (PublicKey)in.readObject();
            PrivateKey privateKey = (PrivateKey)in.readObject();
            return new KeyPair(publicKey, privateKey);
         } catch (ClassNotFoundException e) {
            throw new IOException("Failed to decode keys", e);
         }
      }

      private PublicKey decodePublicKey(SecretBytes encoded) throws IOException {
         try (
            ByteArrayInputStream bin = new ByteArrayInputStream(encoded.get());
            ObjectInputStream in = new ObjectInputStream(bin);
         ) {
            in.readObject();
            return (PublicKey)in.readObject();
         } catch (ClassNotFoundException e) {
            throw new IOException("Failed to decode public key", e);
         }
      }

      private PrivateKey decodePrivateKey(SecretBytes encoded) throws IOException {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(JwtSignatureKeys.GET_ENTITLEMENT_PRIVATE_KEY_PERMISSION);
         }

         try (
            ByteArrayInputStream bin = new ByteArrayInputStream(encoded.get());
            ObjectInputStream in = new ObjectInputStream(bin);
         ) {
            in.readObject();
            in.readObject();
            return (PrivateKey)in.readObject();
         } catch (ClassNotFoundException e) {
            throw new IOException("Failed to decode private key", e);
         }
      }

      private Instant decodeGenerationTime(SecretBytes encoded) throws IOException {
         try (
            ByteArrayInputStream bin = new ByteArrayInputStream(encoded.get());
            ObjectInputStream in = new ObjectInputStream(bin);
         ) {
            return (Instant)in.readObject();
         } catch (ClassNotFoundException e) {
            throw new IOException("Failed to decode generation time", e);
         }
      }

      private SecretBytes encode(KeyPair keys) throws IOException {
         try (
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
         ) {
            out.writeObject(Instant.now());
            out.writeObject(keys.getPublic());
            out.writeObject(keys.getPrivate());
            return new SecretBytes(bos.toByteArray(), true);
         }
      }

      private byte[] encrypt(KeyPair keys, byte[] iv) throws IOException {
         try {
            KeyRing keyRing = SubscriptionLicenseUtil.getInstance().getKeyRing(JwtSignatureKeys.this.keyPairBaseDirectory);
            Aes256PasswordManager pwdMgr = Aes256PasswordManager.getManager(keyRing, "baja.licensing.subscription.ecKeyPair");

            try (SecretBytes encoded = this.encode(keys)) {
               return pwdMgr.encrypt(encoded.get(), iv);
            }
         } catch (Exception e) {
            throw new IOException("Failed to encrypt", e);
         }
      }
   }

   public final class KeyRotation implements AutoCloseable {
      private KeyPair keys;

      public KeyRotation() throws KeyRotationException {
         this.init();
      }

      PublicKey getNewPublicKey() throws KeyRotationException {
         this.checkOpen();
         return this.keys.getPublic();
      }

      synchronized void commit() throws KeyRotationException {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(JwtSignatureKeys.ROTATE_ENTITLEMENT_KEYS_PERMISSION);
         }

         this.checkOpen();

         try {
            FileUtil.copyFile(JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE.getFile(), JwtSignatureKeys.this.keyPairFile.getFile());
         } catch (IOException ioe) {
            throw new KeyRotationException("Failed to commit key rotation", ioe);
         }

         try {
            FileUtil.delete(JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE.getFile());
         } catch (IOException ioe) {
            if (JwtSignatureKeys.LOG.isLoggable(Level.FINE)) {
               JwtSignatureKeys.LOG.log(Level.WARNING, "Key rotation commit failed to delete: " + JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE, ioe);
            } else {
               JwtSignatureKeys.LOG.warning("Key rotation commit failed to delete: " + JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE);
            }
         }

         this.keys = null;
      }

      synchronized void rollback() throws KeyRotationException {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(JwtSignatureKeys.ROTATE_ENTITLEMENT_KEYS_PERMISSION);
         }

         this.checkOpen();

         try {
            FileUtil.delete(JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE.getFile());
         } catch (IOException ioe) {
            if (JwtSignatureKeys.LOG.isLoggable(Level.FINE)) {
               JwtSignatureKeys.LOG.log(Level.WARNING, "Key rotation rollback failed to delete: " + JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE, ioe);
            } else {
               JwtSignatureKeys.LOG.warning("Key rotation rollback failed to delete: " + JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE);
            }
         }

         this.keys = null;
      }

      @Override
      public void close() throws KeyRotationException {
         try {
            this.checkOpen();
         } catch (KeyRotationException ignore) {
            this.keys = null;
            if (JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE.exists() && !JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE.getFile().delete()) {
               JwtSignatureKeys.LOG.warning("Key rotation file (" + JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE + ") not cleaned up");
            }

            return;
         }

         this.rollback();
      }

      private void checkOpen() throws KeyRotationException {
         if (this.keys == null || !JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE.exists()) {
            throw new KeyRotationException("Key rotation not initialized or already finalized");
         }
      }

      private void init() throws KeyRotationException {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(JwtSignatureKeys.ROTATE_ENTITLEMENT_KEYS_PERMISSION);
         }

         if (this.keys != null) {
            throw new KeyRotationException("Key rotation already initialized");
         }

         try {
            this.keys = CertUtils.generateEcKeyPair("secp256r1");
            JwtSignatureKeys.this.KEY_PAIR_ROTATION_FILE.write(this.keys);
         } catch (Exception e) {
            throw new KeyRotationException("Failed to initialize key rotation", e);
         }
      }
   }
}
