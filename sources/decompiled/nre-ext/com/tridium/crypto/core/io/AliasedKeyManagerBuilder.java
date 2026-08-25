package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.KeyStorePermission;
import com.tridium.nre.security.SecretChars;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.baja.nre.util.SecurityUtil;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

public class AliasedKeyManagerBuilder {
   private final ISecurityInfoProvider securityInfoProvider;
   private final String alias;
   private final SecretChars keyPassphrase;
   private static final String DEFAULT_ROOT_CERT_EXCLUSION_SIG_ALGS = "SHA1WITHRSA";

   public AliasedKeyManagerBuilder(ISecurityInfoProvider securityInfoProvider, String alias) {
      this.securityInfoProvider = securityInfoProvider;
      this.alias = alias;
      this.keyPassphrase = null;
   }

   public AliasedKeyManagerBuilder(ISecurityInfoProvider securityInfoProvider, String alias, SecretChars keyPassphrase) {
      this.securityInfoProvider = securityInfoProvider;
      this.alias = alias;
      this.keyPassphrase = keyPassphrase;
   }

   public KeyManager[] getKeyManagers() throws SecurityException {
      try {
         CoreCryptoManager mgr = CoreCryptoManager.get(this.securityInfoProvider);
         CoreKeyStore coreKeyStore = (CoreKeyStore)mgr.getKeyStore();
         KeyStorePermission.checkRead(coreKeyStore.storeName);
         return AccessController.doPrivileged(
            () -> {
               KeyStore keyStore = mgr.getKeyStore().getKeyStore();
               byte[] passBytes = coreKeyStore.getKeyRingKey();
               ProtectionParameter ksParam = new PasswordProtection(SecurityUtil.toHexChars(passBytes));
               Entry ksEntry;
               if (this.keyPassphrase != null && this.keyPassphrase.size() > 0) {
                  ProtectionParameter tempParam = new PasswordProtection(this.keyPassphrase.get());
                  ksEntry = keyStore.getEntry(this.alias, tempParam);
               } else {
                  ksEntry = keyStore.getEntry(this.alias, ksParam);
               }

               List<String> signatureAlgorithmsToExclude = getSignatureAlgorithmsForRootCACertExclusion();
               boolean excludeCertChainRoot = false;
               Certificate[] effectiveChain = new Certificate[0];
               PrivateKey certPrivateKey = null;
               if (keyStore.entryInstanceOf(this.alias, PrivateKeyEntry.class)) {
                  PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry)ksEntry;
                  certPrivateKey = privateKeyEntry.getPrivateKey();
                  Certificate[] originalChain = privateKeyEntry.getCertificateChain();
                  X509Certificate certChainRootCert = (X509Certificate)originalChain[originalChain.length - 1];
                  boolean isCertChainRootCertCA = CertUtils.isCACertificate(certChainRootCert);
                  boolean isCertChainRootCertSigAlgSHA1withRSA = signatureAlgorithmsToExclude.contains(certChainRootCert.getSigAlgName());
                  excludeCertChainRoot = isCertChainRootCertCA && isCertChainRootCertSigAlgSHA1withRSA;
                  effectiveChain = excludeCertChainRoot
                     ? Arrays.copyOf(originalChain, originalChain.length - 1)
                     : Arrays.copyOf(originalChain, originalChain.length);
               }

               KeyStore tempKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
               tempKeyStore.load(null, null);
               if (excludeCertChainRoot) {
                  tempKeyStore.setKeyEntry(this.alias, certPrivateKey, SecurityUtil.toHexChars(passBytes), effectiveChain);
               } else {
                  tempKeyStore.setEntry(this.alias, ksEntry, ksParam);
               }

               KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("PKIX");
               keyManagerFactory.init(tempKeyStore, SecurityUtil.toHexChars(passBytes));
               return keyManagerFactory.getKeyManagers();
            }
         );
      } catch (Exception e) {
         throw new SecurityException("Could not build KeyManagers.", e);
      }
   }

   private static List<String> getSignatureAlgorithmsForRootCACertExclusion() {
      String sigAlgsProp = AccessController.doPrivileged(() -> {
         String systemPropertyVal = System.getProperty("niagara.web.excludeRootCAFromCertChainForSigAlgs");
         if (systemPropertyVal == null) {
            systemPropertyVal = "SHA1WITHRSA";
         }

         return systemPropertyVal;
      });
      return Arrays.asList(sigAlgsProp.split(";"));
   }
}
