package com.tridium.crypto.core.cert;

import java.security.cert.X509Certificate;
import javax.baja.nre.security.IX509Certificate;

public class CertValidationResult {
   private IX509Certificate newCert = null;
   private IX509Certificate origCert = null;
   private boolean approved = false;
   private boolean certChanged = false;
   private boolean hostnameVerified = false;
   private boolean validCertChain = false;
   private boolean certExpired = true;

   public CertValidationResult(
      boolean validCertChain,
      boolean approved,
      boolean certChanged,
      boolean hostnameVerified,
      boolean certExpired,
      IX509Certificate origCert,
      X509Certificate newCert
   ) {
      this.approved = approved;
      this.validCertChain = validCertChain;
      this.certChanged = certChanged;
      this.hostnameVerified = hostnameVerified;
      this.certExpired = certExpired;
      this.origCert = origCert;
      if (newCert != null) {
         this.newCert = NX509Certificate.make(newCert);
      }
   }

   public boolean isApproved() {
      return this.approved;
   }

   public boolean certChanged() {
      return this.certChanged;
   }

   public boolean isHostnameVerified() {
      return this.hostnameVerified;
   }

   public boolean isCertExpired() {
      return this.certExpired;
   }

   public boolean isValidCertChain() {
      return this.validCertChain;
   }

   public IX509Certificate getOrigCert() {
      return this.origCert;
   }

   public IX509Certificate getNewCert() {
      return this.newCert;
   }
}
