package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.NX509CertificateEntry;
import com.tridium.crypto.core.cert.ValidationException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import javax.baja.nre.security.IX509CertificateEntry;

public interface ICoreTrustStore extends ICoreStore {
   Enumeration<String> aliases() throws Exception;

   boolean containsAlias(String var1) throws Exception;

   void deleteEntry(String var1) throws Exception;

   X509Certificate getCertificate(String var1) throws Exception;

   String getCertificateAlias(X509Certificate var1) throws Exception;

   X509Certificate[] getCertificateChain(String var1) throws Exception;

   Date getCreationDate(String var1) throws Exception;

   boolean isKeyEntry(String var1) throws Exception;

   boolean isCertificateEntry(String var1) throws Exception;

   void setCertificateEntry(String var1, X509Certificate var2) throws Exception;

   int size() throws Exception;

   void load() throws Exception;

   void save() throws Exception;

   Iterable<IX509CertificateEntry> getCertificateEntries() throws Exception;

   String findCertificate(X509Certificate var1) throws Exception;

   void deleteEntries(String[] var1) throws Exception;

   KeyStore getKeyStore() throws Exception;

   default void validateJarCertChain(JarEntry entry, boolean checkTpk) throws ValidationException {
      throw new UnsupportedOperationException("Jar entry certificate validation is not supported");
   }

   default void validateClassCertChain(Class<?> cls, boolean checkTpk) throws ValidationException {
      throw new UnsupportedOperationException("Class certificate validation is not supported");
   }

   @Deprecated
   default Enumeration<NX509CertificateEntry> getCertificates() throws Exception {
      List<NX509CertificateEntry> nx509CertificateEntries = new ArrayList<>();

      for (IX509CertificateEntry ix509CertificateEntry : this.getCertificateEntries()) {
         if (ix509CertificateEntry instanceof NX509CertificateEntry) {
            nx509CertificateEntries.add((NX509CertificateEntry)ix509CertificateEntry);
         }
      }

      return Collections.enumeration(nx509CertificateEntries);
   }
}
