package com.tridium.opcUaServer.util;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.cert.CertificateCheck;
import com.prosysopc.ua.stack.cert.ValidationResult;
import com.prosysopc.ua.stack.core.ApplicationDescription;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.transport.security.Cert;
import com.prosysopc.ua.stack.transport.security.CertificateValidator;
import com.prosysopc.ua.stack.utils.CertificateUtils;
import com.tridium.crypto.core.cert.DefaultExemptionApprover;
import com.tridium.crypto.core.cert.PemSource;
import com.tridium.crypto.core.io.CoreClientTrustManager;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.opcUaCore.enums.BCertificateType;
import com.tridium.opcUaCore.util.OpcUaCertificateValidationListener;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SignatureException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.ExemptionApprover;
import javax.security.auth.x500.X500Principal;
import org.jetbrains.annotations.NotNull;

public class OpcUaServerCertificateValidator implements CertificateValidator {
   private String hostKey;
   private final OpcUaCertificateValidationListener certificateValidationListener;
   private static final String INVALID_URI_ERROR = "invalid URI name:";
   private static final Logger LOGGER = Logger.getLogger("opcUaServer.certificateValidator");
   private final int port;

   public OpcUaServerCertificateValidator(int port) {
      this.port = port;
      this.certificateValidationListener = new OpcUaCertificateValidationListener(BCertificateType.client);
   }

   public StatusCode validateCertificate(ApplicationDescription applicationDescription, Cert cert) {
      this.hostKey = this.determineHostKey(cert, this.port);
      EnumSet<CertificateCheck> certificateCheckEnumSet = EnumSet.noneOf(CertificateCheck.class);
      X509Certificate certificate = cert.getCertificate();
      X509Certificate[] certChain = new X509Certificate[]{certificate};
      if (this.isCertificateTrusted(certChain)) {
         certificateCheckEnumSet.add(CertificateCheck.Trusted);
         certificateCheckEnumSet.add(CertificateCheck.Signature);
      }

      if (this.isCertificateSelfSigned(certificate)) {
         certificateCheckEnumSet.add(CertificateCheck.Signature);
         certificateCheckEnumSet.add(CertificateCheck.SelfSigned);
      }

      if (this.isCertificateValid(certificate)) {
         certificateCheckEnumSet.add(CertificateCheck.Validity);
      }

      try {
         if (this.isUriOk(applicationDescription, certificate, certificateCheckEnumSet)) {
            certificateCheckEnumSet.add(CertificateCheck.Uri);
            certificateCheckEnumSet.add(CertificateCheck.UriValid);
         }
      } catch (StatusException var7) {
         return var7.getStatusCode();
      }

      ValidationResult validationResult = this.certificateValidationListener.onValidate(cert, applicationDescription, certificateCheckEnumSet);
      return this.getValidationStatus(validationResult, certificateCheckEnumSet);
   }

   public StatusCode validateCertificate(Cert cert) {
      return cert == null ? StatusCode.GOOD : this.validateCertificate(null, cert);
   }

   @NotNull
   private StatusCode getValidationStatus(ValidationResult validationResult, EnumSet<CertificateCheck> certificateCheckEnumSet) {
      StatusCode statusCode;
      switch (validationResult) {
         case AcceptPermanently:
         case AcceptOnce:
            statusCode = StatusCode.GOOD;
            break;
         case Reject:
            if (!certificateCheckEnumSet.contains(CertificateCheck.Trusted)) {
               statusCode = StatusCode.valueOf(StatusCodes.Bad_CertificateUntrusted);
            } else if (!certificateCheckEnumSet.contains(CertificateCheck.Signature)) {
               statusCode = StatusCode.valueOf(StatusCodes.Bad_SecurityChecksFailed);
            } else if (!certificateCheckEnumSet.contains(CertificateCheck.Validity)) {
               statusCode = StatusCode.valueOf(StatusCodes.Bad_CertificateTimeInvalid);
            } else if (!certificateCheckEnumSet.contains(CertificateCheck.Uri)) {
               statusCode = StatusCode.valueOf(StatusCodes.Bad_CertificateUriInvalid);
            } else {
               LOGGER.log(Level.WARNING, "Rejected a client certificate which contained the following passed checks: " + certificateCheckEnumSet);
               statusCode = StatusCode.valueOf(StatusCodes.Bad_SecurityChecksFailed);
            }
            break;
         default:
            LOGGER.log(Level.SEVERE, "Unknown validation result returned by the certificate validation listener: " + validationResult);
            LOGGER.log(Level.SEVERE, "Rejected a client certificate which contained the following passed checks: " + certificateCheckEnumSet);
            statusCode = StatusCode.valueOf(StatusCodes.Bad_UnexpectedError);
      }

      return statusCode;
   }

   private boolean isCertificateTrusted(X509Certificate[] x509Chain) {
      try {
         ISecurityInfoProvider secInfo = AccessController.doPrivileged(
            (PrivilegedAction<ISecurityInfoProvider>)(() -> SecurityInitializer.getInstance().getSecurityInfoProvider())
         );
         ExemptionApprover exemptionApprover = new DefaultExemptionApprover();
         CoreClientTrustManager trustManager = CoreClientTrustManager.make(secInfo, () -> exemptionApprover);
         trustManager.checkClientTrusted(x509Chain, "KE:RSA", this.hostKey);
         return true;
      } catch (Exception var5) {
         if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.SEVERE, "Client certificate " + x509Chain[0].getSubjectX500Principal().getName() + " failed trust validation", (Throwable)var5);
         } else {
            LOGGER.log(Level.SEVERE, "Client certificate " + x509Chain[0].getSubjectX500Principal().getName() + " failed trust validation: " + var5);
         }

         return false;
      }
   }

   private boolean isUriOk(ApplicationDescription applicationDescription, X509Certificate certificate, EnumSet<CertificateCheck> certificateCheckEnumSet) throws StatusException {
      String applicationUriOfApplicationDescription = applicationDescription == null ? null : applicationDescription.getApplicationUri();
      boolean isUriOk = applicationUriOfApplicationDescription == null;
      if (!isUriOk) {
         try {
            String applicationUriOfCertificate = CertificateUtils.getApplicationUriOfCertificate(certificate);
            if (applicationUriOfCertificate.equals(applicationUriOfApplicationDescription)) {
               isUriOk = true;
            }
         } catch (CertificateParsingException var8) {
            if (var8.getCause().getMessage().contains("invalid URI name:")) {
               String[] causeSplit = var8.getCause().getMessage().split("invalid URI name:");
               if (causeSplit.length == 2 && causeSplit[1].equals(applicationUriOfApplicationDescription)) {
                  LOGGER.log(Level.WARNING, "The provided client certificate contains an invalid ApplicationURI: ", causeSplit[1]);
                  certificateCheckEnumSet.add(CertificateCheck.Uri);
                  return isUriOk;
               }

               LOGGER.log(Level.WARNING, "The provided client certificate does not define the ApplicationURI" + var8.getMessage());
               throw new StatusException(StatusCode.valueOf(StatusCodes.Bad_CertificateUriInvalid));
            }

            LOGGER.log(Level.WARNING, "The provided client certificate has an invalid SubjectAlternativeNames field" + var8.getMessage());
            throw new StatusException(StatusCode.valueOf(StatusCodes.Bad_CertificateInvalid));
         }
      }

      return isUriOk;
   }

   private boolean isCertificateSelfSigned(X509Certificate certificate) {
      try {
         certificate.verify(certificate.getPublicKey());
         return true;
      } catch (SignatureException var3) {
         LOGGER.log(Level.FINE, "Client certificate not self-signed");
      } catch (Exception var4) {
         if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.WARNING, "Client certificate not signed properly", (Throwable)var4);
         } else {
            LOGGER.log(Level.WARNING, "Client certificate not signed properly: " + var4);
         }
      }

      return false;
   }

   private boolean isCertificateValid(X509Certificate certificate) {
      try {
         certificate.checkValidity();
         return true;
      } catch (CertificateNotYetValidException | CertificateExpiredException var3) {
         if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.WARNING, "Client certificate is not valid for current date: " + this.hostKey, (Throwable)var3);
         } else {
            LOGGER.log(Level.WARNING, "Client certificate is not valid for current date: " + this.hostKey);
         }

         return false;
      }
   }

   private String getCommonNameOrDN(X500Principal principal) {
      return PemSource.extractCommonName(principal) != null ? PemSource.extractCommonName(principal) : principal.getName().trim();
   }

   private String determineHostKey(Cert cert, int port) {
      String host = this.getCommonNameOrDN(cert.getCertificate().getIssuerX500Principal());

      try {
         String applicationUri = CertificateUtils.getApplicationUriOfCertificate(cert);
         if (!applicationUri.isEmpty()) {
            String[] sections = applicationUri.substring(applicationUri.indexOf("urn:") + 4).split(":");
            StringBuilder hostKeyBuilder = new StringBuilder();
            Arrays.stream(sections).forEach(section -> hostKeyBuilder.append(section).append('.'));
            host = hostKeyBuilder.substring(0, hostKeyBuilder.lastIndexOf("."));
         }
      } catch (Exception var7) {
         LOGGER.log(Level.FINE, "Failed to determine host from application URI, using issuer DN/common name as host: " + host);
      }

      return host + ':' + port;
   }
}
