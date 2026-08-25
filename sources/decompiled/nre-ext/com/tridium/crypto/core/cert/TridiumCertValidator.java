package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.io.ICoreCryptoManager;
import com.tridium.crypto.core.io.ICoreExemptionStore;
import com.tridium.crypto.core.io.ICoreTrustStore;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.baja.nre.security.IX509Certificate;

public class TridiumCertValidator {
   public static final Logger log = Logger.getLogger("crypto");
   private static boolean CERT_VALIDATOR_METRICS = true;

   public static CertValidationResult validateCertificate(ICoreCryptoManager mgr, X509Certificate[] chain, String hostName, String host) throws Exception {
      boolean validCertChain = true;

      try {
         validateCertChain(mgr, chain);
      } catch (ValidationException e) {
         log.finer(() -> String.format("Could not validate cert chain for host <%s>. Looking for exemption.", hostName));
         validCertChain = false;
      }

      boolean certChanged = false;
      boolean hostnameVerified = false;
      boolean approved = false;
      boolean certExpired = false;
      IX509Certificate origCert = null;
      if (host != null) {
         ICoreExemptionStore es = mgr.getExemptionStore();
         if (es != null) {
            NHostExemption exemption = AccessController.doPrivileged(() -> es.getExemption(host));
            if (exemption != null) {
               origCert = exemption.getCertificate();
               certChanged = !publicKeyMatches(exemption, chain[0]);
               if (!certChanged && exemption.getApproved()) {
                  log.finer(() -> String.format("Found approved exemption for host <%s>", hostName));
                  approved = true;
               }
            }
         }
      }

      hostnameVerified = new TridiumHostnameVerifier(mgr.getExemptionStore()).verify(hostName, chain);
      certExpired = isOutOfDate(chain);
      return new CertValidationResult(validCertChain, approved, certChanged, hostnameVerified, certExpired, origCert, chain[0]);
   }

   public static void validateCertChain(ICoreCryptoManager mgr, X509Certificate[] certs) throws ValidationException {
      try {
         ICoreTrustStore trustStore = mgr.getUserTrustStore();
         KeyStore store = AccessController.doPrivileged(trustStore::getKeyStore);
         validateCertChain(store, certs);
      } catch (Exception e) {
         try {
            ICoreTrustStore trustStore = mgr.getSystemTrustStore();
            KeyStore store = AccessController.doPrivileged(trustStore::getKeyStore);
            validateCertChain(store, certs);
         } catch (Exception f) {
            throw new ValidationException("unable to validate certPath chain: " + f.getLocalizedMessage());
         }
      }
   }

   private static void validateCertChain(KeyStore store, X509Certificate[] certs) throws ValidationException {
      try {
         List<X509Certificate> certList = new ArrayList<>();
         Collections.addAll(certList, certs);
         CertificateFactory fact = CertificateFactory.getInstance("X.509");
         CertPath path = fact.generateCertPath(certList);
         CertPathValidator validator = CertPathValidator.getInstance("PKIX");
         PKIXParameters params = new PKIXParameters(store);
         params.setRevocationEnabled(false);
         CertPathValidatorResult result = validator.validate(path, params);
      } catch (Exception ite) {
         throw new ValidationException("unable to validate certPath chain: " + ite.getLocalizedMessage());
      }
   }

   public static boolean isOutOfDate(X509Certificate[] chain) {
      Date now = new Date();

      for (X509Certificate aChain : chain) {
         if (now.before(aChain.getNotBefore()) || now.after(aChain.getNotAfter())) {
            return true;
         }
      }

      return false;
   }

   public static boolean publicKeyMatches(NHostExemption exemption, X509Certificate endCert) throws Exception {
      IX509Certificate iCert = NX509Certificate.make(endCert);
      return Arrays.equals(iCert.getPublicKeyHash(), exemption.getPublicKeyHash());
   }
}
