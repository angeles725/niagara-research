package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.NCertificateParameters;
import com.tridium.crypto.core.cert.NKeyPairGenerator;
import com.tridium.crypto.core.cert.NPKCS10CertificationRequest;
import com.tridium.crypto.core.cert.NX509CertificateBuilder;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.util.Version;

public interface ICoreCryptoManager {
   Version ADVANCED_CERT_GEN_VERSION = new Version("4.13.0");

   ICoreKeyStore getKeyStore() throws Exception;

   ICoreTrustStore getUserTrustStore() throws Exception;

   ICoreTrustStore getUserUntrustedStore() throws Exception;

   ICoreTrustStore getSystemTrustStore() throws Exception;

   ICoreExemptionStore getExemptionStore() throws Exception;

   ICoreProviderInfo getProviderInfo() throws Exception;

   boolean isSecure();

   int generateSelfSignedCert(NX509CertificateBuilder var1, NKeyPairGenerator var2, SecretChars var3) throws Exception;

   default int generateSelfSignedCert(NX509CertificateBuilder builder, NKeyPairGenerator generator, SecretChars existingPassword, SecretChars newPassword) throws Exception {
      return this.generateSelfSignedCert(builder, generator, newPassword);
   }

   int resetUserKeyStore() throws Exception;

   int getCertGenerationStatus(int var1) throws Exception;

   NPKCS10CertificationRequest generateCSR(String var1, String var2) throws Exception;

   boolean canGenerateCertificate();

   @Deprecated
   int generateSelfSignedCert(NCertificateParameters var1) throws Exception;

   Version getCryptoManagerVersion() throws Exception;
}
