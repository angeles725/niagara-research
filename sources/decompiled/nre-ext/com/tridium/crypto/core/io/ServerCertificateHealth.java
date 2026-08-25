package com.tridium.crypto.core.io;

import java.util.Objects;

public class ServerCertificateHealth {
   private final int hashCode;
   private final String toStringValue;
   private final String requestedCert;
   private final String returnedCert;
   private final CertificateStatusEnum cause;

   public ServerCertificateHealth() {
      this("", "", CertificateStatusEnum.OK);
   }

   public ServerCertificateHealth(String requestedCert, String returnedCert, CertificateStatusEnum cause) {
      Objects.requireNonNull(requestedCert);
      Objects.requireNonNull(returnedCert);
      Objects.requireNonNull(cause);
      this.requestedCert = requestedCert;
      this.returnedCert = returnedCert;
      this.cause = cause;
      this.toStringValue = requestedCert + " => " + returnedCert + " : " + cause;
      this.hashCode = Objects.hash(requestedCert, returnedCert, cause.getVal());
   }

   public String getRequestedCert() {
      return this.requestedCert;
   }

   public String getReturnedCert() {
      return this.returnedCert;
   }

   public CertificateStatusEnum getCause() {
      return this.cause;
   }

   @Override
   public String toString() {
      return this.toStringValue;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      ServerCertificateHealth status = (ServerCertificateHealth)obj;
      return this.returnedCert.equalsIgnoreCase(status.returnedCert) && this.requestedCert.equalsIgnoreCase(status.requestedCert) && this.cause == status.cause;
   }

   @Override
   public int hashCode() {
      return this.hashCode;
   }
}
