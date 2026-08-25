package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.CertValidationResult;
import com.tridium.crypto.core.cert.DefaultExemptionApprover;
import com.tridium.crypto.core.cert.NHostExemption;
import com.tridium.crypto.core.cert.NX509Certificate;
import com.tridium.crypto.core.cert.TridiumCertValidator;
import com.tridium.crypto.core.cert.TridiumHostnameVerifier;
import com.tridium.nre.security.ISecurityInfoProvider;
import java.net.Socket;
import java.security.AccessController;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.ExemptionApprover;
import javax.baja.nre.util.ByteArrayUtil;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

public class CoreClientTrustManager extends X509ExtendedTrustManager {
   private static final Logger LOG = Logger.getLogger("crypto");
   private List<X509TrustManager> trustManagerList = new ArrayList<>();
   private CoreCryptoManager coreCryptoManager;
   private Supplier<ExemptionApprover> exemptionApproverSupplier;
   private WeakHashMap<SSLSocket, CoreClientTrustManager.ExpectedHostInfo> expectedHostInfoMap = new WeakHashMap<>();
   public static final X509Certificate[] X_509_CERTIFICATES = new X509Certificate[0];

   private CoreClientTrustManager(ISecurityInfoProvider secInfo, Supplier<ExemptionApprover> exemptionApproverSupplier) {
      this(CoreCryptoManager.get(secInfo), exemptionApproverSupplier);
   }

   private CoreClientTrustManager(CoreCryptoManager coreCryptoManager, Supplier<ExemptionApprover> exemptionApproverSupplier) {
      this.exemptionApproverSupplier = exemptionApproverSupplier;
      this.coreCryptoManager = coreCryptoManager;
   }

   public static CoreClientTrustManager make(ISecurityInfoProvider secInfo, Supplier<ExemptionApprover> exemptionApproverSupplier) throws Exception {
      CoreClientTrustManager coreClientTrustManager = new CoreClientTrustManager(secInfo, exemptionApproverSupplier);
      coreClientTrustManager.initializeTrustManagers();
      return coreClientTrustManager;
   }

   public static CoreClientTrustManager make(CoreCryptoManager coreCryptoManager, Supplier<ExemptionApprover> exemptionApproverSupplier) throws Exception {
      CoreClientTrustManager coreClientTrustManager = new CoreClientTrustManager(coreCryptoManager, exemptionApproverSupplier);
      coreClientTrustManager.initializeTrustManagers();
      return coreClientTrustManager;
   }

   private void initializeTrustManagers() throws Exception {
      ICoreTrustStore userTrustStore = this.coreCryptoManager.getUserTrustStore();
      if (userTrustStore.size() > 0) {
         TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
         trustManagerFactory.init(AccessController.doPrivileged(userTrustStore::getKeyStore));
         TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

         for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
               this.trustManagerList.add((X509TrustManager)trustManager);
               break;
            }
         }
      }

      ICoreTrustStore systemTrustStore = this.coreCryptoManager.getSystemTrustStore();
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
      trustManagerFactory.init(AccessController.doPrivileged(systemTrustStore::getKeyStore));
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

      for (TrustManager trustManager : trustManagers) {
         if (trustManager instanceof X509TrustManager) {
            this.trustManagerList.add((X509TrustManager)trustManager);
            break;
         }
      }
   }

   @Override
   public void checkClientTrusted(X509Certificate[] x509Chain, String authType, Socket socket) throws CertificateException {
      int expectedPort = -1;
      String hostKey;
      String hostName;
      if (socket instanceof SSLSocket && this.expectedHostInfoMap.get(socket) != null) {
         CoreClientTrustManager.ExpectedHostInfo expectedHostInfo = this.expectedHostInfoMap.get(socket);
         hostName = expectedHostInfo.expectedHostname;
         expectedPort = expectedHostInfo.expectedPort;
         hostKey = hostName + ':' + expectedPort;
         if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(String.format("Found expected host <%s> and port <%d> for socket", hostName, expectedPort));
         }
      } else {
         LOG.fine("Did not find stored host value for socket, getting hostname from InetAddress");
         hostName = CryptoCoreClientSocketFactory.getHostName(socket.getInetAddress());
         hostKey = hostName + ':' + socket.getPort();
      }

      try {
         this.checkClientTrusted(x509Chain, authType);
         this.verifyHostname(x509Chain, socket, hostName, expectedPort);
      } catch (CertificateException ce) {
         this.handleCertException(ce, x509Chain, hostName, hostKey, ((SSLSocket)socket).getHandshakeSession());
      }
   }

   @Override
   public void checkServerTrusted(X509Certificate[] x509Chain, String authType, Socket socket) throws CertificateException {
      int expectedPort = -1;
      String hostKey;
      String hostName;
      if (socket instanceof SSLSocket && this.expectedHostInfoMap.get(socket) != null) {
         CoreClientTrustManager.ExpectedHostInfo expectedHostInfo = this.expectedHostInfoMap.get(socket);
         hostName = expectedHostInfo.expectedHostname;
         expectedPort = expectedHostInfo.expectedPort;
         hostKey = hostName + ':' + expectedPort;
         if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(String.format("Found expected host <%s> and port <%d> for socket", hostName, expectedPort));
         }
      } else {
         LOG.fine("Did not find stored host value for socket, getting hostname from InetAddress");
         hostName = CryptoCoreClientSocketFactory.getHostName(socket.getInetAddress());
         hostKey = hostName + ':' + socket.getPort();
      }

      try {
         this.checkServerTrusted(x509Chain, authType);
         this.verifyHostname(x509Chain, socket, hostName, expectedPort);
      } catch (CertificateException ce) {
         this.handleCertException(ce, x509Chain, hostName, hostKey, ((SSLSocket)socket).getHandshakeSession());
      }
   }

   @Override
   public void checkClientTrusted(X509Certificate[] x509Chain, String authType, SSLEngine sslEngine) throws CertificateException {
      String hostName = sslEngine.getPeerHost();
      String hostAndPort = hostName + ':' + sslEngine.getPeerPort();

      try {
         this.checkClientTrusted(x509Chain, authType);
         if (!new TridiumHostnameVerifier(this.coreCryptoManager.getExemptionStore()).verify(hostName, x509Chain)) {
            throw new CertificateException("hostname didn't verify: " + hostName);
         }
      } catch (CertificateException ce) {
         this.handleCertException(ce, x509Chain, hostName, hostAndPort, sslEngine.getHandshakeSession());
      }
   }

   @Override
   public void checkServerTrusted(X509Certificate[] x509Chain, String authType, SSLEngine sslEngine) throws CertificateException {
      String hostName = sslEngine.getPeerHost();
      String hostAndPort = hostName + ':' + sslEngine.getPeerPort();

      try {
         this.checkServerTrusted(x509Chain, authType);
         if (!new TridiumHostnameVerifier(this.coreCryptoManager.getExemptionStore()).verify(hostName, x509Chain)) {
            throw new CertificateException("hostname didn't verify: " + hostName);
         }
      } catch (CertificateException ce) {
         this.handleCertException(ce, x509Chain, hostName, hostAndPort, sslEngine.getHandshakeSession());
      }
   }

   @Override
   public void checkClientTrusted(X509Certificate[] x509Chain, String authType) throws CertificateException {
      boolean trusted = false;
      CertificateException lastException = null;

      for (X509TrustManager trustManager : this.trustManagerList) {
         try {
            trustManager.checkClientTrusted(x509Chain, authType);
            trusted = true;
            break;
         } catch (CertificateException ce) {
            lastException = ce;
         }
      }

      if (!trusted && lastException != null) {
         throw lastException;
      }
   }

   @Override
   public void checkServerTrusted(X509Certificate[] x509Chain, String authType) throws CertificateException {
      boolean trusted = false;
      CertificateException lastException = null;

      for (X509TrustManager trustManager : this.trustManagerList) {
         try {
            trustManager.checkServerTrusted(x509Chain, authType);
            trusted = true;
            break;
         } catch (CertificateException ce) {
            lastException = ce;
         }
      }

      if (!trusted && lastException != null) {
         throw lastException;
      }
   }

   @Override
   public X509Certificate[] getAcceptedIssuers() {
      ArrayList<X509Certificate> acceptedIssuers = new ArrayList<>();

      for (X509TrustManager trustManager : this.trustManagerList) {
         Collections.addAll(acceptedIssuers, trustManager.getAcceptedIssuers());
      }

      return acceptedIssuers.toArray(X_509_CERTIFICATES);
   }

   public void checkServerTrusted(X509Certificate[] x509Chain, String authType, String hostName, int port) throws CertificateException {
      String hostKey = hostName + ':' + port;

      try {
         this.checkServerTrusted(x509Chain, authType);

         try {
            TridiumHostnameVerifier hostnameVerifier = new TridiumHostnameVerifier(this.coreCryptoManager.getExemptionStore());
            boolean verified = hostnameVerifier.verify(hostName, x509Chain, port);
            if (!verified) {
               throw new CertificateException(
                  String.format("Hostname validation failed. Expected <%s>, got <%s>", hostName, x509Chain[0].getSubjectX500Principal().getName())
               );
            }
         } catch (Exception e) {
            throw new CertificateException("hostname didn't verify: " + e.getMessage(), e);
         }
      } catch (CertificateException ce) {
         this.handleCertException(ce, x509Chain, hostName, hostKey, null);
      }
   }

   public void checkClientTrusted(X509Certificate[] x509Chain, String authType, String hostKey) throws CertificateException {
      try {
         this.checkClientTrusted(x509Chain, authType);
      } catch (CertificateException ce) {
         this.handleCertException(ce, x509Chain, null, hostKey, null);
      }
   }

   private void verifyHostname(X509Certificate[] x509Chain, Socket socket, String hostName, int expectedPort) throws CertificateException {
      try {
         TridiumHostnameVerifier hostnameVerifier = new TridiumHostnameVerifier(this.coreCryptoManager.getExemptionStore());
         boolean verified;
         if (expectedPort != -1) {
            verified = hostnameVerifier.verify(hostName, x509Chain, expectedPort);
         } else {
            verified = hostnameVerifier.verify(hostName, x509Chain, socket.getPort());
         }

         if (!verified) {
            throw new CertificateException(
               String.format("Hostname validation failed. Expected <%s>, got <%s>", hostName, x509Chain[0].getSubjectX500Principal().getName())
            );
         }
      } catch (Exception e) {
         throw new CertificateException("hostname didn't verify: " + e.getMessage(), e);
      }
   }

   private void handleCertException(CertificateException ce, X509Certificate[] x509Chain, String hostName, String hostKey, SSLSession session) throws CertificateException {
      LOG.fine(() -> "Cert exception in TLS handshake with <" + hostKey + ">. Cause is: " + ce.getMessage());
      LOG.log(Level.FINER, ce, () -> "Cert exception in TLS handshake with <" + hostKey + ">.");

      try {
         LOG.fine(() -> String.format("Attempting to create certificate exemption for host <%s>.", hostKey));
         CertValidationResult result = TridiumCertValidator.validateCertificate(this.coreCryptoManager, x509Chain, hostName, hostKey);
         if (!result.isApproved()) {
            this.handleRejectedCertificate(x509Chain, result, hostKey);
         }
      } catch (Exception e) {
         LOG.warning(String.format("TLS handshake with <%s> failed. Cause is: %s", hostKey, e.getMessage()));
         if (session != null) {
            session.putValue("tlsFailureCause", e);
         }

         if (e instanceof CertificateException) {
            throw (CertificateException)e;
         } else {
            throw new CertificateException(e);
         }
      }
   }

   private void handleRejectedCertificate(X509Certificate[] x509Chain, CertValidationResult result, String hostKey) throws Exception {
      LOG.finer(
         () -> "Certificate validation failed.\n\tHostname verified = "
            + result.isHostnameVerified()
            + "\n\tCertificate expired = "
            + result.isCertExpired()
            + "\n\tCert chain valid = "
            + result.isValidCertChain()
      );
      ExemptionApprover exemptionApprover = this.exemptionApproverSupplier.get();
      if (exemptionApprover == null) {
         exemptionApprover = new DefaultExemptionApprover();
      }

      boolean approved = exemptionApprover.approveExemption(result);
      if (approved) {
         LOG.finer(() -> String.format("Exemption for host <%s> was approved.", hostKey));
         this.addExemption(x509Chain[0], true, hostKey, exemptionApprover.isTransientApproval());
      } else {
         if (result.certChanged()) {
            LOG.finer(() -> String.format("Certificate private key for exemption <%s> has changed.", hostKey));
            ICoreExemptionStore exemptionStore = this.coreCryptoManager.getExemptionStore();
            NHostExemption exemption = AccessController.doPrivileged(() -> exemptionStore.getExemption(hostKey));
            exemption.setChanged(NX509Certificate.make(x509Chain[0]));
            if (exemption.isTransient() && exemptionApprover.isTransientApproval() && exemptionApprover.approveExemption(result)) {
               exemption.approveChanged();
               LOG.log(Level.INFO, () -> String.format("Certificate private key for exemption <%s> has changed, and an exemption has been approved", hostKey));
            }

            AccessController.doPrivileged(() -> {
               exemptionStore.setExemption(exemption);
               exemptionStore.save();
               return null;
            });
            if (exemption.getChanged() != null) {
               String message = String.format("Certificate private key for exemption for host <%s> has changed", hostKey);
               LOG.fine(message);
               throw new SSLException(message);
            }
         } else {
            LOG.finer(() -> String.format("Exemption for host <%s> was not approved", hostKey));
            this.addExemption(x509Chain[0], false, hostKey, false);
            certificateException(result.isValidCertChain(), result.isHostnameVerified(), result.isCertExpired());
         }
      }
   }

   private static void certificateException(boolean validated, boolean hostnameVerified, boolean expired) throws CertificateException {
      StringJoiner msg = new StringJoiner(", ");
      if (!validated) {
         msg.add("failed certificate validation");
      }

      if (!hostnameVerified) {
         msg.add("failed hostname validation");
      }

      if (expired) {
         msg.add("certificate expired");
      }

      throw new CertificateException(msg.toString());
   }

   private void addExemption(X509Certificate cert, boolean approved, String hostKey, boolean isTransient) throws Exception {
      if (hostKey != null && hostKey.length() > 0 && hostKey.indexOf(58) > 0) {
         boolean addExemption = false;
         NHostExemption newExemption = NHostExemption.make(NX509Certificate.make(cert), hostKey, approved, isTransient);
         ICoreExemptionStore exemptionStore = this.coreCryptoManager.getExemptionStore();
         NHostExemption currentExemption = AccessController.doPrivileged(() -> exemptionStore.getExemption(newExemption.getHost()));
         if (currentExemption == null
            || !newExemption.getSHA1Fingerprint().equals(currentExemption.getSHA1Fingerprint())
            || !ByteArrayUtil.equals(newExemption.getPublicKeyHash(), currentExemption.getPublicKeyHash())
            || newExemption.getApproved() != currentExemption.getApproved()
            || newExemption.isReverseDns() != currentExemption.isReverseDns()
            || newExemption.isTransient() != currentExemption.isTransient()) {
            addExemption = true;
         }

         if (addExemption) {
            LOG.fine(() -> "Adding new exemption entry for " + newExemption.getHost() + " certificate " + cert.getSubjectX500Principal().getName());
            AccessController.doPrivileged(() -> {
               exemptionStore.setExemption(newExemption);
               exemptionStore.save();
               return null;
            });
         } else {
            LOG.fine(
               () -> "Not adding exemption entry for "
                  + currentExemption.getHost()
                  + " certificate "
                  + cert.getSubjectX500Principal().getName()
                  + ", exemption status is up-to-date"
            );
         }
      } else {
         LOG.log(Level.SEVERE, "Could not add exemption.");
      }
   }

   void setExpectedHostInfo(SSLSocket socket, String hostname, int port) {
      this.expectedHostInfoMap.put(socket, new CoreClientTrustManager.ExpectedHostInfo(hostname, port));
   }

   private static class ExpectedHostInfo {
      private String expectedHostname;
      private int expectedPort;

      private ExpectedHostInfo(String expectedHostname, int expectedPort) {
         this.expectedHostname = expectedHostname;
         this.expectedPort = expectedPort;
      }
   }
}
