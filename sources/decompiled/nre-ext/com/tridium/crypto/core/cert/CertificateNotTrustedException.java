package com.tridium.crypto.core.cert;

import java.security.cert.Certificate;

public class CertificateNotTrustedException extends SecurityException {
   private final Certificate certificate;

   public CertificateNotTrustedException(String message, Throwable cause, Certificate untrusted) {
      super(message, cause);
      this.certificate = untrusted;
   }

   public Certificate getCertificate() {
      return this.certificate;
   }
}
