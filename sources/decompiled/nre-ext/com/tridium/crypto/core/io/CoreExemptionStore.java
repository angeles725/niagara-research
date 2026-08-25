package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.NHostExemption;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.KeyStorePermission;
import com.tridium.nre.util.FileLock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CoreExemptionStore extends CoreStore implements ICoreExemptionStore {
   public static final int MAGIC = 18535938;
   public static final int VERSION = 3;
   private Hashtable<String, NHostExemption> exemptions = new Hashtable<>();
   private byte[] ivBytes;
   private Logger log = Logger.getLogger("crypto");

   public CoreExemptionStore(ICoreCryptoManager mgr, File file, ISecurityInfoProvider secInfo, String keyName, String storeName) throws Exception {
      super(mgr, file, secInfo, keyName, storeName);
      this.load();
   }

   @Override
   public synchronized Enumeration<NHostExemption> exemptions() {
      KeyStorePermission.checkRead(this.storeName);
      return this.exemptions.elements();
   }

   @Override
   public synchronized NHostExemption getExemption(String host) throws Exception {
      KeyStorePermission.checkRead(this.storeName);
      return this.exemptions.get(host.toLowerCase());
   }

   @Override
   public synchronized void setExemption(NHostExemption exemption) {
      if (this.isReadOnly) {
         throw new SecurityException("unable to set exemption entry");
      }

      this.exemptions.put(exemption.getHost().toLowerCase(), exemption);
   }

   @Override
   public synchronized int size() throws Exception {
      return this.exemptions.size();
   }

   @Override
   public synchronized void deleteExemption(String host) {
      if (this.isReadOnly) {
         throw new SecurityException("unable to set exemption entry");
      }

      String hostname = host.toLowerCase();
      NHostExemption exemption = this.exemptions.get(hostname);
      if (exemption != null) {
         this.exemptions.remove(hostname);
      }
   }

   @Override
   public synchronized void load() throws Exception {
      KeyStorePermission.checkRead(this.storeName);

      try {
         AccessController.doPrivileged(() -> {
            this.loadPrivileged();
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   public synchronized void loadPrivileged() throws Exception {
      FileLock lock = null;
      if (this.storeFile.exists()) {
         try {
            lock = FileLock.lock(this.storeFile, 5000);
            long loadStarted = System.currentTimeMillis();
            if (this.log != null && this.log.isLoggable(Level.FINE)) {
               this.log.fine("loading " + this.storeFile);
            }

            byte[] bytes = null;

            try (FileInputStream in = new FileInputStream(this.storeFile)) {
               bytes = this.getKeyRingKey();
               this.load(in, bytes);
               this.lastModified = this.storeFile.lastModified();
               if (this.log != null && this.log.isLoggable(Level.FINE)) {
                  this.log.fine(this.storeFile + " loaded (" + (System.currentTimeMillis() - loadStarted) + "ms)");
               }
            } finally {
               SecurityUtil.zeroByteArray(bytes);
            }
         } catch (FileNotFoundException e) {
            if (this.log != null && this.log.isLoggable(Level.FINE)) {
               this.log.fine(this.storeFile + " not found. Creating new one.");
            }
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }
      } else if (this.log != null && this.log.isLoggable(Level.FINE)) {
         this.log.fine(this.storeFile + " not found. Creating new one.");
      }
   }

   @Override
   public synchronized void save() throws Exception {
      try {
         AccessController.doPrivileged(() -> {
            this.savePrivileged();
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   public synchronized void savePrivileged() throws Exception {
      if (this.isReadOnly) {
         throw new SecurityException("unable to save exemption store");
      }

      FileLock lock = null;
      this.storeFile.createNewFile();

      try {
         lock = FileLock.lock(this.storeFile, 5000);
         long saveStarted = System.currentTimeMillis();
         if (this.log != null && this.log.isLoggable(Level.FINE)) {
            this.log.fine("saving " + this.storeFile);
         }

         byte[] bytes = null;

         try (FileOutputStream out = new FileOutputStream(this.storeFile)) {
            bytes = this.getKeyRingKey();
            this.store(out, bytes);
            out.flush();
            this.lastModified = this.storeFile.lastModified();
            if (this.log != null && this.log.isLoggable(Level.FINE)) {
               this.log.fine(this.storeFile + " saved (" + (System.currentTimeMillis() - saveStarted) + "ms)");
            }
         } finally {
            SecurityUtil.zeroByteArray(bytes);
         }
      } finally {
         if (lock != null) {
            lock.unlock();
         }
      }
   }

   private synchronized void load(InputStream stream, byte[] password) throws IOException {
      KeyStorePermission.checkRead(this.storeName);
      this.exemptions.entrySet().removeIf(it -> !it.getValue().isTransient());
      if (stream != null) {
         try {
            int magic = this.readInt(stream);
            if (magic != 18535938) {
               throw new IOException("Not a tridium exemption store.");
            }

            int ivBytesLen = this.readInt(stream);
            if (ivBytesLen < 0) {
               throw new IOException("Corrupt exemption store.");
            }

            this.ivBytes = new byte[ivBytesLen];
            if (stream.read(this.ivBytes) != ivBytesLen) {
               throw new IOException("Corrupt exemption store.");
            }

            int len = this.readInt(stream);
            if (len < 0) {
               throw new IOException("Corrupt exemption store.");
            }

            byte[] msg = new byte[len];
            if (stream.read(msg) != len) {
               throw new IOException("Corrupt exemption store.");
            }

            msg = this.decrypt(msg, password);
            ByteArrayInputStream bin = new ByteArrayInputStream(msg);
            DataInputStream in = new DataInputStream(bin);
            int version = in.readInt();
            if (version != 3) {
               throw new IOException("Invalid version in exemption store: " + version);
            }

            int entries = in.readInt();
            if (entries < 0) {
               throw new IOException("Corrupt exemption store.");
            }

            while (--entries >= 0) {
               NHostExemption exemption = this.readExemption(in);
               this.exemptions.put(exemption.getHost(), exemption);
            }
         } catch (Exception e) {
            this.log.log(Level.SEVERE, "failed to load exemption store", e);
            this.exemptions.clear();
         }
      }
   }

   private synchronized void store(OutputStream stream, byte[] password) throws Exception {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bout);
      out.writeInt(3);
      List<NHostExemption> exemptionsToSave = new LinkedList<>();
      Enumeration<NHostExemption> certExemptions = this.exemptions.elements();

      while (certExemptions.hasMoreElements()) {
         NHostExemption exemption = certExemptions.nextElement();
         if (!exemption.isTransient()) {
            exemptionsToSave.add(exemption);
         }
      }

      out.writeInt(exemptionsToSave.size());

      for (NHostExemption exemption : exemptionsToSave) {
         this.writeExemption(exemption, out);
      }

      out.close();
      byte[] bytes = bout.toByteArray();
      bytes = this.encrypt(bytes, password);
      this.writeInt(stream, 18535938);
      this.writeInt(stream, this.ivBytes.length);
      stream.write(this.ivBytes);
      this.writeInt(stream, bytes.length);
      stream.write(bytes);
   }

   private void writeExemption(NHostExemption exemption, DataOutputStream out) throws Exception {
      String encoded = exemption.encodeToString();
      out.writeUTF(encoded);
   }

   private NHostExemption readExemption(DataInputStream in) throws Exception {
      String encoded = in.readUTF();
      return NHostExemption.make(encoded);
   }

   private int readInt(InputStream in) throws IOException {
      return (in.read() << 24) + (in.read() << 16) + (in.read() << 8) + in.read();
   }

   private void writeInt(OutputStream out, int i) throws IOException {
      out.write(i >>> 24 & 0xFF);
      out.write(i >>> 16 & 0xFF);
      out.write(i >>> 8 & 0xFF);
      out.write(i & 0xFF);
   }

   byte[] decrypt(byte[] arg, byte[] pass) throws Exception {
      if (pass != null && pass.length != 0) {
         SecretKeySpec key = new SecretKeySpec(pass, "AES");

         try {
            Cipher decryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec params = new GCMParameterSpec(128, this.ivBytes);
            decryptCipher.init(2, key, params);
            return decryptCipher.doFinal(arg);
         } catch (Exception e) {
            try {
               Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
               IvParameterSpec ivspec = new IvParameterSpec(this.ivBytes);
               decryptCipher.init(2, key, ivspec);
               return decryptCipher.doFinal(arg);
            } catch (Exception e2) {
               throw e;
            }
         }
      } else {
         return arg;
      }
   }

   byte[] encrypt(byte[] arg, byte[] pass) throws Exception {
      if (pass != null && pass.length != 0) {
         this.ivBytes = new byte[16];
         new SecureRandom().nextBytes(this.ivBytes);
         SecretKeySpec key = new SecretKeySpec(pass, "AES");
         Cipher encryptCipher = Cipher.getInstance("AES/GCM/NoPadding");
         GCMParameterSpec params = new GCMParameterSpec(128, this.ivBytes);
         encryptCipher.init(1, key, params);
         return encryptCipher.doFinal(arg);
      } else {
         return arg;
      }
   }
}
