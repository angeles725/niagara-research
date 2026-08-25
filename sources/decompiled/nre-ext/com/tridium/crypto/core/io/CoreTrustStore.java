package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.NX509CertificateEntry;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.KeyStorePermission;
import com.tridium.nre.util.FileLock;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.baja.nre.security.IX509CertificateEntry;
import javax.baja.nre.util.SecurityUtil;
import org.bouncycastle.util.Arrays;

public class CoreTrustStore extends CoreStore implements ICoreTrustStore {
   private static final Supplier<Date> DEFAULT_DATE_SUPPLIER = () -> new Date();
   private final Supplier<Date> dateSupplier;
   private final List<CoreTrustStore.CertificateAddedListener> certAddedListeners = new ArrayList<>();
   protected KeyStore keyStore;

   public CoreTrustStore(ICoreCryptoManager mgr, ISecurityInfoProvider secInfo, KeyStore keyStore, String storeName) throws Exception {
      this(mgr, secInfo, keyStore, DEFAULT_DATE_SUPPLIER, storeName);
   }

   CoreTrustStore(ICoreCryptoManager mgr, File file, ISecurityInfoProvider secInfo, String keyName, String storeType, String storeName) throws Exception {
      super(mgr, file, secInfo, keyName, storeName);
      this.dateSupplier = DEFAULT_DATE_SUPPLIER;
      this.keyStore = KeyStore.getInstance(storeType);
      this.load();
   }

   CoreTrustStore(ICoreCryptoManager mgr, ISecurityInfoProvider secInfo, KeyStore keyStore, Supplier<Date> dateSupplier, String storeName) throws Exception {
      super(mgr, null, secInfo, null, storeName);
      this.keyStore = keyStore;
      this.dateSupplier = dateSupplier;
   }

   @Override
   public final Enumeration<String> aliases() throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return this.keyStore.aliases();
   }

   @Override
   public final boolean containsAlias(String alias) throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return this.keyStore.containsAlias(alias);
   }

   @Override
   public void deleteEntry(String alias) throws KeyStoreException {
      KeyStorePermission.checkWrite(this.storeName);
      if (this.isReadOnly) {
         throw new SecurityException("unable to delete entry, trust store is read only");
      }

      this.keyStore.deleteEntry(alias);
      if (this.mgr instanceof CoreCryptoManager) {
         ((CoreCryptoManager)this.mgr).trustStoreModified(this);
      }
   }

   @Override
   public final X509Certificate getCertificate(String alias) throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return (X509Certificate)this.keyStore.getCertificate(alias);
   }

   @Override
   public final String getCertificateAlias(X509Certificate cert) throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return this.keyStore.getCertificateAlias(cert);
   }

   @Override
   public final X509Certificate[] getCertificateChain(String alias) throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      Certificate[] certs = this.keyStore.getCertificateChain(alias);
      if (certs == null) {
         return null;
      }

      X509Certificate[] x509Certs = new X509Certificate[certs.length];

      for (int i = 0; i < certs.length; i++) {
         x509Certs[i] = (X509Certificate)certs[i];
      }

      return x509Certs;
   }

   @Override
   public final Date getCreationDate(String alias) throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return this.keyStore.getCreationDate(alias);
   }

   public final Provider getProvider() {
      return this.keyStore.getProvider();
   }

   public final String getType() {
      return this.keyStore.getType();
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.keyStore);
   }

   @Override
   public final boolean isCertificateEntry(String alias) throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return this.keyStore.isCertificateEntry(alias);
   }

   @Override
   public final boolean isKeyEntry(String alias) throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return this.keyStore.isKeyEntry(alias);
   }

   @Override
   public void setCertificateEntry(String alias, X509Certificate cert) throws KeyStoreException {
      KeyStorePermission.checkWrite(this.storeName);
      if (this.isReadOnly) {
         throw new SecurityException("unable to set certificate entry, trust store is read only");
      }

      this.keyStore.setCertificateEntry(alias, cert);
      if (this.mgr instanceof CoreCryptoManager) {
         ((CoreCryptoManager)this.mgr).trustStoreModified(this);
      }

      Thread thread = new Thread(new CoreTrustStore.NotifyListenersRunnable(cert));
      thread.start();
   }

   @Override
   public final int size() throws KeyStoreException {
      KeyStorePermission.checkRead(this.storeName);
      return this.keyStore.size();
   }

   @Override
   public Iterable<IX509CertificateEntry> getCertificateEntries() throws Exception {
      KeyStorePermission.checkRead(this.storeName);
      List<IX509CertificateEntry> certs = new ArrayList<>();
      Enumeration<String> e = this.keyStore.aliases();

      while (e.hasMoreElements()) {
         String alias = e.nextElement();
         Certificate cert = this.keyStore.getCertificate(alias);
         if (cert != null && cert.getType() != null && cert.getType().equals("X.509")) {
            X509Certificate x509cert = (X509Certificate)cert;
            certs.add(NX509CertificateEntry.make(alias, new X509Certificate[]{x509cert}, null));
         }
      }

      return certs;
   }

   @Override
   public void deleteEntries(String[] aliases) throws Exception {
      KeyStorePermission.checkWrite(this.storeName);
      if (this.isReadOnly) {
         throw new SecurityException("unable to delete entries, trust store is readonly");
      }

      for (String alias : aliases) {
         this.keyStore.deleteEntry(alias);
      }
   }

   @Override
   public String findCertificate(X509Certificate cert) throws Exception {
      KeyStorePermission.checkRead(this.storeName);
      if (cert.getPublicKey() != null) {
         Enumeration<String> e = this.keyStore.aliases();

         while (e.hasMoreElements()) {
            String alias = e.nextElement();
            Certificate keyStoreCert = this.keyStore.getCertificate(alias);
            if (keyStoreCert != null
               && keyStoreCert.getType().equals("X.509")
               && keyStoreCert.getPublicKey() != null
               && Arrays.areEqual(cert.getPublicKey().getEncoded(), keyStoreCert.getPublicKey().getEncoded())) {
               return alias;
            }
         }
      }

      return null;
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

   private void loadPrivileged() throws Exception {
      FileLock lock = null;
      if (this.storeFile.exists()) {
         try {
            lock = FileLock.lock(this.storeFile, 5000);
            long loadStarted = System.currentTimeMillis();
            if (this.log != null && this.log.isLoggable(Level.FINE)) {
               this.log.fine("loading " + this.storeFile);
            }

            byte[] bytes = null;
            char[] chars = null;

            try (FileInputStream in = new FileInputStream(this.storeFile)) {
               bytes = this.getKeyRingKey();
               chars = SecurityUtil.toHexChars(bytes);
               this.keyStore.load(in, chars);
               this.lastModified = this.storeFile.lastModified();
               if (this.log != null && this.log.isLoggable(Level.FINE)) {
                  this.log.fine(this.storeFile + " loaded (" + (System.currentTimeMillis() - loadStarted) + "ms)");
               }
            } finally {
               SecurityUtil.zeroCharArray(chars);
               SecurityUtil.zeroByteArray(bytes);
            }
         } catch (FileNotFoundException fnfe) {
            if (this.log != null && this.log.isLoggable(Level.FINE)) {
               this.log.fine(this.storeFile + " not found. Creating new one.");
            }

            this.keyStore.load(null, null);
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }
      } else {
         if (this.log != null && this.log.isLoggable(Level.FINE)) {
            this.log.fine(this.storeFile + " not found. Creating new one.");
         }

         this.keyStore.load(null, null);
      }

      if (this.mgr instanceof CoreCryptoManager) {
         ((CoreCryptoManager)this.mgr).trustStoreModified(this);
      }
   }

   @Override
   public synchronized void save() throws Exception {
      KeyStorePermission.checkWrite(this.storeName);

      try {
         AccessController.doPrivileged(() -> {
            this.savePrivileged();
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   private void savePrivileged() throws Exception {
      FileLock lock = null;
      if (this.storeFile == null) {
         throw new IllegalArgumentException("not file name provider");
      }

      this.storeFile.createNewFile();

      try {
         lock = FileLock.lock(this.storeFile, 5000);
         long saveStarted = System.currentTimeMillis();
         if (this.log != null && this.log.isLoggable(Level.FINE)) {
            this.log.fine("saving " + this.storeFile);
         }

         byte[] bytes = null;
         char[] chars = null;

         try (FileOutputStream out = new FileOutputStream(this.storeFile)) {
            bytes = this.getKeyRingKey();
            chars = SecurityUtil.toHexChars(bytes);
            this.keyStore.store(out, SecurityUtil.toHexChars(this.getKeyRingKey()));
            out.flush();
            this.lastModified = this.storeFile.lastModified();
            if (this.log != null && this.log.isLoggable(Level.FINE)) {
               this.log.fine(this.storeFile + " saved (" + (System.currentTimeMillis() - saveStarted) + "ms)");
            }
         } finally {
            SecurityUtil.zeroCharArray(chars);
            SecurityUtil.zeroByteArray(bytes);
         }
      } finally {
         if (lock != null) {
            lock.unlock();
         }
      }
   }

   public void registerCertAddedListener(CoreTrustStore.CertificateAddedListener listener) {
      KeyStorePermission.checkRead(this.storeName);
      synchronized (this.certAddedListeners) {
         this.certAddedListeners.add(listener);
      }
   }

   public void unregisterCertAddedListener(CoreTrustStore.CertificateAddedListener listener) {
      synchronized (this.certAddedListeners) {
         this.certAddedListeners.remove(listener);
      }
   }

   @Override
   public KeyStore getKeyStore() {
      KeyStorePermission.checkRead(this.storeName);
      KeyStorePermission.checkWrite(this.storeName);
      return this.keyStore;
   }

   void setKeyStore(KeyStore keyStore) {
      this.keyStore = keyStore;
   }

   public interface CertificateAddedListener {
      void certificateAdded(X509Certificate var1);
   }

   class NotifyListenersRunnable implements Runnable {
      X509Certificate cert;

      NotifyListenersRunnable(X509Certificate certificate) {
         this.cert = certificate;
      }

      @Override
      public void run() {
         List<CoreTrustStore.CertificateAddedListener> listeners;
         synchronized (CoreTrustStore.this.certAddedListeners) {
            listeners = new ArrayList<>(CoreTrustStore.this.certAddedListeners);
         }

         for (CoreTrustStore.CertificateAddedListener listener : listeners) {
            listener.certificateAdded(this.cert);
         }
      }
   }
}
