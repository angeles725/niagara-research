package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.crypto.core.cert.NX509CertificateBuilder;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.KeyStorePermission;
import java.io.File;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.baja.nre.security.IX509CertificateEntry;
import javax.baja.nre.util.SecurityUtil;

public class CoreKeyStore extends CoreTrustStore implements ICoreKeyStore {
   public static final String WEAK_CERT_PASSWORD_ERROR = "Password strength not met.";

   public CoreKeyStore(ICoreCryptoManager mgr, ISecurityInfoProvider secInfo, KeyStore keyStore, String storeName) throws Exception {
      super(mgr, secInfo, keyStore, storeName);
   }

   public CoreKeyStore(ICoreCryptoManager mgr, File file, ISecurityInfoProvider secInfo, String keyName, String storeType, String storeName) throws Exception {
      super(mgr, file, secInfo, keyName, storeType, storeName);
   }

   private static void checkPasswordStrength(char[] password) throws Exception {
      int lowerCase = 0;
      int upperCase = 0;
      int digits = 0;
      int special = 0;
      if (password == null) {
         throw new Exception("Password strength not met.");
      }

      int len = password.length;

      for (char character : password) {
         if (Character.isLetter(character)) {
            if (Character.isUpperCase(character)) {
               upperCase++;
            } else {
               lowerCase++;
            }
         } else if (Character.isDigit(character)) {
            digits++;
         } else {
            special++;
         }
      }

      if (len < 10 || digits < 1 || lowerCase < 1 || upperCase < 1 || special < 0) {
         throw new Exception("Password strength not met.");
      }
   }

   @Override
   public final Key getKey(String alias, char[] pass) throws Exception {
      KeyStorePermission.checkRead(this.storeName);
      if (pass != null && pass.length != 0) {
         try {
            return AccessController.doPrivileged(() -> this.keyStore.getKey(alias, pass));
         } catch (PrivilegedActionException e) {
            throw e.getException();
         } finally {
            SecurityUtil.zeroCharArray(pass);
         }
      } else {
         byte[] bytes = null;
         char[] chars = null;

         try {
            bytes = AccessController.doPrivileged(this::getKeyRingKey);
            chars = SecurityUtil.toHexChars(bytes);
            char[] finalChars = chars;
            return AccessController.doPrivileged(() -> this.keyStore.getKey(alias, finalChars));
         } catch (PrivilegedActionException e) {
            throw e.getException();
         } finally {
            SecurityUtil.zeroCharArray(chars);
            SecurityUtil.zeroByteArray(bytes);
         }
      }
   }

   @Override
   public final void setCertificateEntry(String alias, X509Certificate cert) throws KeyStoreException {
      KeyStorePermission.checkWrite(this.storeName);
      if (this.isReadOnly) {
         throw new SecurityException("unable to set certificate entry, keystore is readonly");
      }

      this.keyStore.setCertificateEntry(alias, cert);
      Thread thread = new Thread(new CoreTrustStore.NotifyListenersRunnable(cert));
      thread.start();
   }

   @Override
   public final void setKeyEntry(String alias, byte[] key, X509Certificate[] chain) throws Exception {
      this.setKeyEntry(alias, key, chain, true);
   }

   public final void setKeyEntry(String alias, byte[] key, X509Certificate[] chain, boolean validateDate) throws Exception {
      KeyStorePermission.checkWrite(this.storeName);
      if ("default".equalsIgnoreCase(alias)) {
         throw new SecurityException("default entry cannot be set");
      }

      if (this.isReadOnly) {
         throw new SecurityException("unable to set key entry, key store is read only");
      }

      CertUtils.validateCertChain(chain, this.mgr.getUserTrustStore(), validateDate);
      X509Certificate cert = chain[0];
      if (CertUtils.isCACertificate(cert)) {
         checkPasswordStrength(null);
      }

      this.setKeyEntryInKeyStore(alias, key, chain);
   }

   @Override
   public final void setKeyEntry(String alias, Key key, char[] pass, X509Certificate[] chain) throws Exception {
      this.setKeyEntry(alias, key, pass, chain, true);
   }

   public final void setKeyEntry(String alias, Key key, char[] pass, X509Certificate[] chain, boolean validateDate) throws Exception {
      KeyStorePermission.checkWrite(this.storeName);
      if ("default".equalsIgnoreCase(alias)) {
         throw new SecurityException("default entry cannot be set");
      }

      if (this.isReadOnly) {
         throw new SecurityException("unable to set key entry, key store is read only");
      }

      CertUtils.validateCertChain(chain, this.mgr.getUserTrustStore(), validateDate);
      X509Certificate cert = chain[0];
      if (CertUtils.isCACertificate(cert)) {
         checkPasswordStrength(pass);
      }

      if (pass != null && pass.length > 0) {
         this.setKeyEntryInKeyStore(alias, key, pass, chain);
         SecurityUtil.zeroCharArray(pass);
      } else {
         byte[] bytes = null;
         char[] chars = null;

         try {
            bytes = AccessController.doPrivileged(this::getKeyRingKey);
            chars = SecurityUtil.toHexChars(bytes);
            this.setKeyEntryInKeyStore(alias, key, chars, chain);
         } finally {
            SecurityUtil.zeroCharArray(chars);
            SecurityUtil.zeroByteArray(bytes);
         }
      }
   }

   private void setKeyEntryInKeyStore(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
      try {
         AccessController.doPrivileged(() -> {
            this.keyStore.setKeyEntry(alias, key, chain);
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw (KeyStoreException)e.getException();
      }
   }

   private void setKeyEntryInKeyStore(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
      try {
         AccessController.doPrivileged(() -> {
            this.keyStore.setKeyEntry(alias, key, password, chain);
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw (KeyStoreException)e.getException();
      }
   }

   @Override
   public final void deleteEntry(String alias) throws KeyStoreException {
      KeyStorePermission.checkWrite(this.storeName);
      if ("default".equalsIgnoreCase(alias)) {
         throw new SecurityException("default entry cannot be deleted");
      }

      super.deleteEntry(alias);
   }

   @Override
   public void deleteEntries(String[] aliases) throws Exception {
      KeyStorePermission.checkWrite(this.storeName);
      if (this.isReadOnly) {
         throw new SecurityException("unable to delete entries, key store is readonly");
      }

      for (String alias : aliases) {
         if (!"default".equalsIgnoreCase(alias)) {
            this.keyStore.deleteEntry(alias);
         } else {
            this.log.fine("skipping delete of default entry");
         }
      }
   }

   public final void generateDefaultEntry(boolean force) throws Exception {
      KeyStorePermission.checkWrite(this.storeName);
      X509Certificate cert = (X509Certificate)this.keyStore.getCertificate("default");
      if (cert == null || force) {
         NX509CertificateBuilder builder = CertUtils.getStockBuilder();
         IX509CertificateEntry entry = builder.generateEntry(CertUtils.FACTORY_CERT_GENERATOR);
         if (cert != null) {
            this.keyStore.deleteEntry("default");
         }

         byte[] bytes = null;
         char[] chars = null;

         try {
            bytes = AccessController.doPrivileged(this::getKeyRingKey);
            chars = SecurityUtil.toHexChars(bytes);
            this.setKeyEntryInKeyStore(entry.getAlias(), entry.getPrivateKey(), chars, entry.getCertificates());
            this.save();
         } finally {
            SecurityUtil.zeroCharArray(chars);
            SecurityUtil.zeroByteArray(bytes);
         }
      }
   }
}
