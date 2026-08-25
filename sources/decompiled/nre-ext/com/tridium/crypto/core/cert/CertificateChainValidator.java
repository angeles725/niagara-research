package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.io.ICoreCryptoManager;
import com.tridium.crypto.core.io.ICoreTrustStore;
import com.tridium.nre.security.SecurityConstants;
import java.security.CodeSigner;
import java.security.KeyStore;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.IX509CertificateEntry;

public class CertificateChainValidator {
   private final Map<CertPath, CertificateChainValidator.CertificateEntry> verifiedCertMap = new HashMap<>();
   private final Set<TrustAnchor> trustAnchors;
   private final Object trustAnchorsLock = new Object();
   private CertificateFactory certFactory;
   private boolean laxValidation = false;
   private static final Logger LOG = Logger.getLogger("crypto.certificatechainvalidator");

   public CertificateChainValidator(Set<TrustAnchor> trustAnchors) {
      this.trustAnchors = trustAnchors;
   }

   public CertificateChainValidator() {
      this(new HashSet<>());
   }

   public static CertificateChainValidator make(ICoreCryptoManager cryptoManager) {
      CertificateChainValidator validator = new CertificateChainValidator();
      validator.rebuildTrustAnchors(cryptoManager);
      return validator;
   }

   public void rebuildTrustAnchors(ICoreCryptoManager cryptoManager) {
      synchronized (this.trustAnchorsLock) {
         this.trustAnchors.clear();

         try {
            for (IX509CertificateEntry systemCert : cryptoManager.getSystemTrustStore().getCertificateEntries()) {
               this.trustAnchors.add(new TrustAnchor(systemCert.getCertificate(0).getCertificate(), null));
            }

            for (IX509CertificateEntry userCert : cryptoManager.getUserTrustStore().getCertificateEntries()) {
               this.trustAnchors.add(new TrustAnchor(userCert.getCertificate(0).getCertificate(), null));
            }
         } catch (Exception e) {
            LOG.warning("Failed to create TrustAnchors from ICoreCryptoManager: " + e);
         }

         this.verifiedCertMap.clear();
      }
   }

   public void addTrustAnchors(ICoreTrustStore trustStore) {
      synchronized (this.trustAnchorsLock) {
         try {
            for (IX509CertificateEntry cert : trustStore.getCertificateEntries()) {
               this.trustAnchors.add(new TrustAnchor(cert.getCertificate(0).getCertificate(), null));
            }
         } catch (Exception e) {
            LOG.warning("Failed to add TrustAnchors from ICoreTrustStore: " + e);
         }
      }
   }

   public void addTrustAnchors(KeyStore keyStore) {
      synchronized (this.trustAnchorsLock) {
         try {
            Enumeration<String> e = keyStore.aliases();

            while (e.hasMoreElements()) {
               String alias = e.nextElement();
               Certificate cert = keyStore.getCertificate(alias);
               if (cert != null && cert.getType() != null && "X.509".equals(cert.getType())) {
                  this.trustAnchors.add(new TrustAnchor((X509Certificate)cert, null));
               }
            }
         } catch (Exception e) {
            LOG.warning("Failed to add TrustAnchors from KeyStore: " + e);
         }
      }
   }

   public Set<TrustAnchor> getTrustAnchors() {
      synchronized (this.trustAnchorsLock) {
         return Collections.unmodifiableSet(this.trustAnchors);
      }
   }

   public void setLaxValidation(boolean laxValidation) {
      this.laxValidation = laxValidation;
   }

   public boolean getLaxValidation() {
      return this.laxValidation;
   }

   public void validateCertChain(CodeSigner signer) throws ValidationException {
      this.validateCertChain(signer, false);
   }

   public void validateCertChain(CodeSigner signer, boolean checkTpk) throws ValidationException {
      synchronized (this.trustAnchorsLock) {
         CertificateChainValidator.CertificateEntry entry = this.verifiedCertMap.computeIfAbsent(signer.getSignerCertPath(), key -> this.mapCertPath(signer));
         if (entry.isValidated()) {
            if (checkTpk && SecurityConstants.canCheckTpk() && !entry.tpkChecked) {
               throw new ValidationException("TPK check failed");
            }

            this.checkTimestamp(signer);
         } else {
            throw new ValidationException("Could not validate cert chain");
         }
      }
   }

   private void validateCertChain(CertPath path, Timestamp ts, PKIXCertPathChecker checker) throws ValidationException {
      try {
         List<? extends Certificate> sorted = CertUtils.sortCertChain(path.getCertificates());
         if (this.certFactory == null) {
            this.certFactory = CertificateFactory.getInstance("X.509");
         }

         path = this.certFactory.generateCertPath(sorted);
         CertPathValidator validator = CertPathValidator.getInstance("PKIX");
         PKIXParameters params = new PKIXParameters(this.trustAnchors);
         params.addCertPathChecker(checker);
         params.setRevocationEnabled(false);
         if (ts != null) {
            params.setDate(ts.getTimestamp());
         } else {
            params.setDate(new Date());
         }

         validator.validate(path, params);
      } catch (Exception e) {
         throw new ValidationException("Could not validate cert chain", e);
      }
   }

   private void checkTimestamp(CodeSigner signer) throws ValidationException {
      Timestamp ts = signer.getTimestamp();
      X509Certificate codeSignerEnd = (X509Certificate)signer.getSignerCertPath().getCertificates().get(0);

      try {
         if (ts == null) {
            codeSignerEnd.checkValidity(new Date());
         } else {
            codeSignerEnd.checkValidity(ts.getTimestamp());
            CertPath tsCertPath = ts.getSignerCertPath();
            CertificateChainValidator.CertificateEntry entry = this.verifiedCertMap.computeIfAbsent(tsCertPath, key -> this.mapTsCertPath(ts));
            if (!entry.isValidated()) {
               throw new ValidationException("Could not validate timestamp certificate chain");
            }
         }
      } catch (CertificateException e) {
         throw new ValidationException("Could not validate certificate chain", e);
      }
   }

   private CertificateChainValidator.CertificateEntry mapTsCertPath(Timestamp ts) {
      CertPath tsCertPath = ts.getSignerCertPath();
      boolean tsValidated = false;

      try {
         this.validateCertChain(tsCertPath, ts, new NPKIXCertPathChecker("1.3.6.1.5.5.7.3.8"));
         tsValidated = true;
      } catch (Exception e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Failed to validate timestamp cert chain", e);
            if (LOG.isLoggable(Level.FINER)) {
               LOG.finer(tsCertPath.toString());
            }
         }

         tsValidated = false;
      }

      return new CertificateChainValidator.CertificateEntry(tsCertPath, tsValidated, false);
   }

   private CertificateChainValidator.CertificateEntry mapCertPath(CodeSigner codeSigner) {
      CertPath certPath = codeSigner.getSignerCertPath();
      Timestamp ts = codeSigner.getTimestamp();
      boolean validated = false;
      boolean tpkChecked = false;

      try {
         this.validateCertChain(certPath, ts, new NPKIXCertPathChecker());
         validated = true;
      } catch (Exception e) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Failed to validate cert chain", e);
            if (LOG.isLoggable(Level.FINER)) {
               LOG.finer(certPath.toString());
            }
         }

         validated = false;
      }

      if (validated && SecurityConstants.canCheckTpk()) {
         try {
            SecurityConstants.checkTpk("", (X509Certificate)certPath.getCertificates().get(0));
            tpkChecked = true;
         } catch (SecurityException var7) {
         }
      }

      return new CertificateChainValidator.CertificateEntry(certPath, validated, tpkChecked);
   }

   private static final class CertificateEntry {
      private final CertPath certPath;
      private final boolean validated;
      private final boolean tpkChecked;

      public CertificateEntry(CertPath certPath, boolean validated, boolean tpkChecked) {
         this.certPath = certPath;
         this.validated = validated;
         this.tpkChecked = tpkChecked;
      }

      public CertPath getCertPath() {
         return this.certPath;
      }

      public boolean isValidated() {
         return this.validated;
      }

      public boolean isTpkChecked() {
         return this.tpkChecked;
      }
   }
}
