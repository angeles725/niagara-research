package com.tridium.crypto.core.cert;

import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NPKIXCertPathChecker extends PKIXCertPathChecker {
   private final String validExtendedKeyUsage;
   public static final String EXTENDED_KEY_USAGE = "2.5.29.37";
   public static final String CODE_SIGNING_USAGE = "1.3.6.1.5.5.7.3.3";
   public static final String TIME_STAMPING_USAGE = "1.3.6.1.5.5.7.3.8";
   private static final HashSet<String> SUPPORTED_EXTENSIONS = new HashSet<>(Collections.singletonList("2.5.29.37"));

   public NPKIXCertPathChecker() {
      this.validExtendedKeyUsage = "1.3.6.1.5.5.7.3.3";
   }

   public NPKIXCertPathChecker(String validExtendedKeyUsage) {
      this.validExtendedKeyUsage = validExtendedKeyUsage;
   }

   @Override
   public void init(boolean forward) throws CertPathValidatorException {
   }

   @Override
   public boolean isForwardCheckingSupported() {
      return false;
   }

   @Override
   public Set<String> getSupportedExtensions() {
      return SUPPORTED_EXTENSIONS;
   }

   @Override
   public void check(Certificate certificate, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
      try {
         X509Certificate cert = (X509Certificate)certificate;
         byte[] extData = cert.getExtensionValue("2.5.29.37");
         if (extData != null) {
            if (cert.getCriticalExtensionOIDs().contains("2.5.29.37")) {
               List<String> extKeyUsage = cert.getExtendedKeyUsage();
               if (extKeyUsage.contains(this.validExtendedKeyUsage)) {
                  unresolvedCritExts.remove("2.5.29.37");
               }
            } else {
               unresolvedCritExts.remove("2.5.29.37");
            }
         }
      } catch (Exception var6) {
      }
   }
}
