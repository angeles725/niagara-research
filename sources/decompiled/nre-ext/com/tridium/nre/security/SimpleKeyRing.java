package com.tridium.nre.security;

import com.tridium.nre.security.km.BadKeyMaterialException;
import com.tridium.nre.security.km.KeyMaterial;
import com.tridium.nre.util.FileLock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.baja.nre.util.ByteArrayUtil;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SimpleKeyRing extends KeyRing {
   private File keyRingFile;
   private long lastModified = 0L;
   public static final int VERSION = 5;
   public static final Set<Integer> SUPPORTED_VERSIONS = Collections.unmodifiableSet(Stream.of(1, 3, 5).collect(Collectors.toSet()));
   public static final int MAGIC = 357109530;
   public static final int LEGACY_AES_VERSION = 3;
   public static final int INITIAL_EXPORTABLE_VERSION = 3;
   public static final int EXPORT_VERSION = 6;
   public static final Set<Integer> SUPPORTED_EXPORT_VERSIONS = Collections.unmodifiableSet(Stream.of(2, 4, 6).collect(Collectors.toSet()));
   public static final int EXPORT_MAGIC = 167662754;
   private static final NiagaraBasicPermission TRANSCODE_KEY_RING_PERMISSION = new NiagaraBasicPermission("TRANSCODE_KEY_RING");
   private static final String RECOVERY_FILE_MARKER = ".rec";

   public SimpleKeyRing(File securityDir, String keyRingName, KeyMaterial keyMaterial) throws Exception {
      super(keyMaterial);
      this.keyRingFile = new File(securityDir, keyRingName);
      this.load();
   }

   public SimpleKeyRing(SimpleKeyRing.SimpleKeyRingParameters keyParameters) throws Exception {
      super(keyParameters.keyMaterial);
      this.keyRingFile = keyParameters.keyRingFile;
      this.load();
   }

   protected SimpleKeyRing(File securityDir, String keyRingName, KeyMaterial keyMaterial, boolean load) throws Exception {
      super(keyMaterial);
      this.keyRingFile = new File(securityDir, keyRingName);
      if (load) {
         this.load();
      }
   }

   public final long keyRingFileLength() {
      return this.keyRingFile.length();
   }

   private synchronized void load() throws Exception {
      this.load(null);
   }

   private synchronized void load(KeyRing.KeyRingLock lock) throws Exception {
      FileLock flock = lock instanceof SimpleKeyRing.SimpleKeyRingLock ? ((SimpleKeyRing.SimpleKeyRingLock)lock).lock : null;

      try {
         AccessController.doPrivileged(() -> {
            this.loadPrivileged(flock);
            return null;
         });
      } catch (PrivilegedActionException e) {
         Exception innerException = e.getException();
         if (innerException instanceof BadPaddingException) {
            throw new BadKeyMaterialException(innerException);
         } else {
            throw innerException;
         }
      }
   }

   private synchronized void loadPrivileged(FileLock slock) throws Exception {
      FileLock lock = null;
      File parentDir = this.keyRingFile.getParentFile();
      if (!parentDir.exists() && !parentDir.mkdirs()) {
         throw new IOException("Couldn't create directories " + parentDir.getAbsolutePath());
      }

      if (!this.keyRingFile.exists()) {
         if (KeyRing.LOG.isLoggable(Level.FINE)) {
            KeyRing.LOG.fine(this.keyRingFile + " not found. Creating new one.");
         }

         if (!this.keyRingFile.createNewFile()) {
            throw new IOException("Couldn't create " + this.keyRingFile.getAbsolutePath());
         }

         try {
            if (slock == null) {
               lock = FileLock.lock(this.keyRingFile, 10000);
               this.save(lock);
            } else {
               this.save(slock);
            }
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }
      }

      long loadStarted = System.currentTimeMillis();
      if (KeyRing.LOG.isLoggable(Level.FINE)) {
         KeyRing.LOG.fine("loading " + this.keyRingFile);
      }

      try (FileInputStream fin = new FileInputStream(this.keyRingFile)) {
         if (slock == null) {
            lock = FileLock.lock(this.keyRingFile, 10000);
         }

         ObjectInputStream din = new ObjectInputStream(fin);
         int version = din.readInt();
         if (!SUPPORTED_VERSIONS.contains(version)) {
            throw new IOException("Invalid keyring format");
         }

         int magic = din.readInt();
         if (magic != 357109530) {
            throw new IOException("Invalid keyring format");
         }

         int size = din.readInt();
         if (size > 0) {
            byte[] blobIv = new byte[16];
            din.readFully(blobIv);
            byte[] blob = new byte[size];
            din.readFully(blob);
            Cipher aesCipher = null;
            AlgorithmParameterSpec params = null;
            if (version <= 3) {
               aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
               params = new IvParameterSpec(blobIv);
            } else {
               aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
               params = new GCMParameterSpec(128, blobIv);
            }

            SecretKey aesKey = new SecretKeySpec(this.keyMaterial.getKeyMaterial(), "AES");
            SecureRandom random = new SecureRandom();
            aesCipher.init(2, aesKey, params, random);
            byte[] data = aesCipher.doFinal(blob);
            ByteArrayInputStream bain = new ByteArrayInputStream(data);
            ObjectInputStream obin = new ObjectInputStream(bain);
            int count = obin.readInt();

            for (int i = 0; i < count; i++) {
               String alias = obin.readUTF();
               int cipherLen = obin.readInt();
               byte[] cipher = new byte[cipherLen];
               obin.readFully(cipher, 0, cipherLen);
               int ivLen = obin.readInt();
               byte[] iv = new byte[ivLen];
               obin.readFully(iv, 0, ivLen);
               boolean isKeyExportable;
               if ("javax.baja.security.BAes256PasswordEncoder.key".equals(alias)) {
                  isKeyExportable = false;
                  if (version >= 3) {
                     obin.readBoolean();
                  }
               } else {
                  isKeyExportable = version < 3 || obin.readBoolean();
               }

               SimpleKeyRing.SimpleKeyRingEntry entry;
               if (version <= 3) {
                  if (KeyRing.LOG.isLoggable(Level.FINE)) {
                     LOG.fine("Migrating key to AES-GCM: " + alias);
                  }

                  Cipher keyCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                  IvParameterSpec ips = new IvParameterSpec(iv);
                  keyCipher.init(2, aesKey, ips, random);
                  byte[] key = keyCipher.doFinal(cipher);
                  entry = new SimpleKeyRing.SimpleKeyRingEntry(key, isKeyExportable);
               } else {
                  entry = new SimpleKeyRing.SimpleKeyRingEntry(cipher, iv, isKeyExportable);
               }

               this.keys.put(alias, entry);
            }
         }

         if (version < 5) {
            this.save(slock != null ? slock : lock);
         }

         if (KeyRing.LOG.isLoggable(Level.FINE)) {
            KeyRing.LOG.fine(this.keyRingFile + " loaded (" + (System.currentTimeMillis() - loadStarted) + "ms)");
         }

         this.lastModified = this.keyRingFile.lastModified();
      } finally {
         if (lock != null) {
            lock.unlock();
         }
      }
   }

   @Override
   public synchronized void importKeyData(InputStream encryptedContents, int encryptedContentsLength, ISecretBytesSupplier keyInfo) throws Exception {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(TRANSCODE_KEY_RING_PERMISSION);
      }

      FileLock lock = FileLock.lock(this.keyRingFile, 10000);

      try (DataInputStream encryptedContentsData = new DataInputStream(encryptedContents)) {
         byte[] ivBytes = new byte[16];
         encryptedContentsData.readFully(ivBytes);
         byte[] encryptedData = new byte[encryptedContentsLength - 16];
         encryptedContentsData.readFully(encryptedData);

         byte[] decryptedData;
         try {
            decryptedData = Aes256PasswordManager.decrypt(keyInfo.get().get(), encryptedData, ivBytes);
         } catch (Exception e) {
            try {
               decryptedData = Aes256PasswordManager.decrypt(keyInfo.get().get(), encryptedData, ivBytes, "AES/CBC/PKCS5Padding");
            } catch (Exception e2) {
               throw e;
            }
         }

         try (
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(decryptedData) {
               @Override
               public void close() {
                  Arrays.fill(this.buf, (byte)0);
               }
            };
            ObjectInputStream in = new ObjectInputStream(bytesIn);
         ) {
            int version = in.readInt();
            int magic = in.readInt();
            if (magic != 167662754) {
               throw new IOException("Invalid keyring format");
            }

            int count = in.readInt();

            for (int i = 0; i < count; i++) {
               String alias = in.readUTF();
               int keyLen = in.readInt();
               byte[] key = new byte[keyLen];
               in.readFully(key, 0, keyLen);
               KeyRing.KeyRingEntry existingKey = this.keys.get(alias);
               if (existingKey == null || existingKey.isKeyExportable()) {
                  SimpleKeyRing.SimpleKeyRingEntry entry = new SimpleKeyRing.SimpleKeyRingEntry(key, true);
                  this.keys.put(alias, entry);
               }
            }

            this.save(lock);
         }
      } finally {
         lock.unlock();
      }
   }

   @Override
   public synchronized byte[] exportKeyData(ISecretBytesSupplier keyInfo) throws Exception {
      Objects.requireNonNull(keyInfo);
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(TRANSCODE_KEY_RING_PERMISSION);
      }

      try (ByteArrayOutputStream portableBytes = new ByteArrayOutputStream() {
            @Override
            public void close() {
               Arrays.fill(this.buf, (byte)0);
            }
         }) {
         ObjectOutputStream out = new ObjectOutputStream(portableBytes);
         out.writeInt(6);
         out.writeInt(167662754);
         out.writeInt((int)this.keys.values().stream().filter(KeyRing.KeyRingEntry::isKeyExportable).count());

         for (Entry<String, KeyRing.KeyRingEntry> entry : this.keys.entrySet()) {
            if (entry.getValue().isKeyExportable()) {
               out.writeUTF(entry.getKey());
               out.writeInt(entry.getValue().getKey().length);
               out.write(entry.getValue().getKey());
            }
         }

         out.flush();
         byte[] ivBytes = new byte[16];
         new SecureRandom().nextBytes(ivBytes);
         byte[] encryptedContents = Aes256PasswordManager.encrypt(portableBytes.toByteArray(), ivBytes, keyInfo.get().get());
         byte[] result = new byte[ivBytes.length + encryptedContents.length];
         System.arraycopy(ivBytes, 0, result, 0, ivBytes.length);
         System.arraycopy(encryptedContents, 0, result, ivBytes.length, encryptedContents.length);
         return result;
      }
   }

   synchronized void save(KeyRing.KeyRingLock lock) throws Exception {
      FileLock flock = null;
      if (lock instanceof SimpleKeyRing.SimpleKeyRingLock) {
         flock = ((SimpleKeyRing.SimpleKeyRingLock)lock).lock;
      }

      this.save(flock);
   }

   synchronized void save(FileLock lock) throws Exception {
      try {
         AccessController.doPrivileged(() -> {
            this.savePrivileged(lock);
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   private synchronized void savePrivileged(FileLock slock) throws Exception {
      FileLock lock = null;
      long saveStarted = System.currentTimeMillis();
      if (KeyRing.LOG.isLoggable(Level.FINE)) {
         KeyRing.LOG.fine("saving " + this.keyRingFile);
      }

      try (FileOutputStream fon = new FileOutputStream(this.keyRingFile)) {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         ObjectOutputStream obout = new ObjectOutputStream(bout);
         obout.writeInt(this.keys.size());

         for (Entry<String, KeyRing.KeyRingEntry> keysEntry : this.keys.entrySet()) {
            String alias = keysEntry.getKey();
            obout.writeUTF(alias);
            SimpleKeyRing.SimpleKeyRingEntry entry = (SimpleKeyRing.SimpleKeyRingEntry)keysEntry.getValue();
            byte[] cipher = entry.getCipher();
            obout.writeInt(cipher.length);
            obout.write(cipher);
            byte[] iv = entry.getIv();
            obout.writeInt(iv.length);
            obout.write(iv);
            obout.writeBoolean(entry.isKeyExportable());
         }

         obout.close();
         bout.close();

         try {
            if (slock == null) {
               lock = FileLock.lock(this.keyRingFile, 10000);
            }

            ObjectOutputStream oon = new ObjectOutputStream(fon);
            oon.writeInt(5);
            oon.writeInt(357109530);
            if (this.keys.size() > 0) {
               Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
               SecureRandom random = new SecureRandom();
               byte[] iv = new byte[16];
               random.nextBytes(iv);
               GCMParameterSpec params = new GCMParameterSpec(128, iv);
               SecretKey aesKey = new SecretKeySpec(this.keyMaterial.getKeyMaterial(), "AES");
               aesCipher.init(1, aesKey, params, random);
               byte[] data = aesCipher.doFinal(bout.toByteArray());
               oon.writeInt(data.length);
               oon.write(iv);
               oon.write(data);
            } else {
               oon.writeInt(0);
            }

            oon.flush();
            oon.close();
            this.lastModified = this.keyRingFile.lastModified();
            if (KeyRing.LOG.isLoggable(Level.FINE)) {
               KeyRing.LOG.fine(this.keyRingFile + " saved (" + (System.currentTimeMillis() - saveStarted) + "ms)");
            }
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }
      }
   }

   @Override
   protected KeyRing.KeyRingEntry createKeyRingEntry(byte[] key, boolean isExportable) throws Exception {
      if (key == null) {
         key = new byte[32];
         new SecureRandom().nextBytes(key);
      }

      return new SimpleKeyRing.SimpleKeyRingEntry(key, isExportable);
   }

   @Override
   protected void preChangeImpl(KeyRing.KeyRingLock lock) throws Exception {
      long fileLastModified = AccessController.doPrivileged(this.keyRingFile::lastModified);
      if (fileLastModified != this.lastModified) {
         this.load(lock);
      }
   }

   @Override
   protected void postChangeImpl(KeyRing.KeyRingLock lock) throws Exception {
      this.save(lock);
   }

   @Override
   protected void checkReload() {
      long fileLastModified = AccessController.doPrivileged(this.keyRingFile::lastModified);
      if (fileLastModified != this.lastModified) {
         try {
            if (KeyRing.LOG.isLoggable(Level.FINE)) {
               KeyRing.LOG.fine("Detected keyring change. Reloading...");
            }

            try {
               this.load();
            } catch (BadKeyMaterialException e) {
               try {
                  AccessController.doPrivileged(this.keyMaterial::refresh);
               } catch (PrivilegedActionException pae) {
                  throw pae.getException();
               }

               this.load();
            }
         } catch (Exception e) {
            KeyRing.LOG.warning("Error reloading keyring: " + e.getMessage());
            if (KeyRing.LOG.isLoggable(Level.FINE)) {
               KeyRing.LOG.log(Level.FINE, "Caused by", e);
            }
         }
      }
   }

   @Override
   protected boolean recoveryKeyExists() {
      File recoveryKeyRingFile = new File(this.keyRingFile.getPath() + ".rec");
      return recoveryKeyRingFile.exists();
   }

   @Override
   protected void createRecoveryKey() throws IOException {
      File recoveryKeyRingFile = new File(this.keyRingFile.getPath() + ".rec");
      if (recoveryKeyRingFile.exists()) {
         KeyRing.LOG.warning("Overwriting existing recovery key ring.");
      }

      Files.copy(this.keyRingFile.toPath(), recoveryKeyRingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
   }

   @Override
   protected RecoverableKeyData getRecoveryKey(KeyParameters keyParameters) {
      if (!(keyParameters instanceof SimpleKeyRing.SimpleKeyRingParameters)) {
         throw new IllegalArgumentException("SimpleKeyRing temporary key must be built with SimpleKeyRingParameters.");
      }

      try {
         return new SimpleKeyRing((SimpleKeyRing.SimpleKeyRingParameters)keyParameters);
      } catch (Exception e) {
         KeyRing.LOG.warning("Could not get recovery key ring.");
         return null;
      }
   }

   @Override
   protected void rollBackRecoveryKey() throws IOException {
      try {
         File recoveryKeyRingFile = new File(this.keyRingFile.getPath() + ".rec");
         Files.copy(recoveryKeyRingFile.toPath(), this.keyRingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
         this.load();
      } catch (Exception e) {
         throw new IOException("Could not roll back key ring. Cause is: " + e.getMessage());
      }
   }

   @Override
   protected void deleteRecoveryKey() throws IOException {
      File recoveryKeyRingFile = new File(this.keyRingFile.getPath() + ".rec");
      recoveryKeyRingFile.delete();
   }

   @Override
   protected KeyRing.KeyRingLock lock() throws Exception {
      return new SimpleKeyRing.SimpleKeyRingLock();
   }

   protected class SimpleKeyRingEntry extends KeyRing.KeyRingEntry {
      protected byte[] cipher;
      protected byte[] iv;
      protected boolean isKeyExportable;

      public SimpleKeyRingEntry(byte[] key, boolean isKeyExportable) throws Exception {
         try {
            AccessController.doPrivileged(() -> {
               Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
               SecureRandom random = new SecureRandom();
               this.iv = new byte[16];
               random.nextBytes(this.iv);
               GCMParameterSpec params = new GCMParameterSpec(128, this.iv);
               SecretKey aesKey = new SecretKeySpec(SimpleKeyRing.this.keyMaterial.getKeyMaterial(), "AES");
               aesCipher.init(1, aesKey, params, random);
               this.cipher = aesCipher.doFinal(key);
               this.isKeyExportable = isKeyExportable;
               return null;
            });
         } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (innerException instanceof BadPaddingException) {
               throw new BadKeyMaterialException(innerException);
            } else {
               throw innerException;
            }
         }
      }

      private SimpleKeyRingEntry(byte[] cipher, byte[] iv, boolean isKeyExportable) throws Exception {
         this.cipher = cipher;
         this.iv = iv;
         this.isKeyExportable = isKeyExportable;
      }

      @Override
      public byte[] getKey() throws Exception {
         try {
            return AccessController.doPrivileged(() -> {
               Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
               GCMParameterSpec params = new GCMParameterSpec(128, this.iv);
               SecretKey aesKey = new SecretKeySpec(SimpleKeyRing.this.keyMaterial.getKeyMaterial(), "AES");
               SecureRandom random = new SecureRandom();
               aesCipher.init(2, aesKey, params, random);
               return aesCipher.doFinal(this.cipher);
            });
         } catch (PrivilegedActionException e) {
            Exception innerException = e.getException();
            if (innerException instanceof BadPaddingException) {
               throw new BadKeyMaterialException(innerException);
            } else {
               throw innerException;
            }
         }
      }

      @Override
      public String toString() {
         try {
            return "cipher:"
               + ByteArrayUtil.toHexString(this.cipher)
               + ",iv="
               + ByteArrayUtil.toHexString(this.iv)
               + ",key="
               + ByteArrayUtil.toHexString(this.getKey());
         } catch (Exception e) {
            e.printStackTrace();
            return "<error> unknown";
         }
      }

      @Override
      public boolean isKeyExportable() {
         return this.isKeyExportable;
      }

      public byte[] getIv() {
         return this.iv;
      }

      private byte[] getCipher() {
         return this.cipher;
      }
   }

   protected class SimpleKeyRingLock extends KeyRing.KeyRingLock {
      FileLock lock = null;

      private SimpleKeyRingLock() throws Exception {
         try {
            AccessController.doPrivileged(() -> {
               this.lock = FileLock.lock(SimpleKeyRing.this.keyRingFile, 10000);
               return null;
            });
         } catch (PrivilegedActionException e) {
            throw e.getException();
         }
      }

      @Override
      protected void unlock() {
         AccessController.doPrivileged(() -> {
            if (this.lock != null) {
               this.lock.unlock();
            }

            return null;
         });
      }
   }

   public class SimpleKeyRingParameters implements KeyParameters {
      private final File keyRingFile;
      private final KeyMaterial keyMaterial;

      public SimpleKeyRingParameters(File keyRingFile, KeyMaterial keyMaterial) {
         this.keyRingFile = keyRingFile;
         this.keyMaterial = keyMaterial;
      }
   }
}
