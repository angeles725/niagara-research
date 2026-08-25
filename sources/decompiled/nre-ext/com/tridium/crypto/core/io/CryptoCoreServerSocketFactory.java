package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.crypto.core.cert.TridiumCertValidator;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.SecretChars;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Logger;
import javax.baja.nre.security.ServerTlsParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

public class CryptoCoreServerSocketFactory extends SSLServerSocketFactory {
   protected String[] protocols = CryptoSupport.TYPE_LISTS.get("tlsv1");
   protected final ISecurityInfoProvider secInfo;
   private final ServerTlsParameters tlsParams;
   private final SSLServerSocketFactory socketFactory;
   private final CoreCryptoManager mgr;
   private final Logger log = Logger.getLogger("crypto");

   public CryptoCoreServerSocketFactory(ISecurityInfoProvider secInfo, ServerTlsParameters tlsParams) throws Exception {
      if (tlsParams.getCertAlias() != null && tlsParams.getCertAlias().trim().length() > 0) {
         this.secInfo = secInfo;
         this.tlsParams = tlsParams;
         this.setType(tlsParams.getMinTlsProtocol());
         this.mgr = CoreCryptoManager.get(secInfo);
         SSLContext context = this.createSSLContext(tlsParams);
         this.socketFactory = context.getServerSocketFactory();
      } else {
         throw new IllegalArgumentException("server alias not provided");
      }
   }

   @Override
   public ServerSocket createServerSocket(int port) throws IOException {
      this.checkKeyAndCert();

      try {
         SSLServerSocket socket = (SSLServerSocket)this.socketFactory.createServerSocket(port);
         return new CryptoCoreServerSocketFactory.NSSLServerSocket(socket);
      } catch (IOException e) {
         throw e;
      } catch (Exception e) {
         throw new IOException(e.getMessage(), e);
      }
   }

   @Override
   public ServerSocket createServerSocket(int port, int backlog) throws IOException {
      this.checkKeyAndCert();

      try {
         SSLServerSocket socket = (SSLServerSocket)this.socketFactory.createServerSocket(port, backlog);
         return new CryptoCoreServerSocketFactory.NSSLServerSocket(socket);
      } catch (IOException e) {
         throw e;
      } catch (Exception e) {
         throw new IOException(e.getMessage(), e);
      }
   }

   @Override
   public ServerSocket createServerSocket(int port, int backlog, InetAddress ifc) throws IOException {
      this.checkKeyAndCert();

      try {
         SSLServerSocket socket = (SSLServerSocket)this.socketFactory.createServerSocket(port, backlog, ifc);
         return new CryptoCoreServerSocketFactory.NSSLServerSocket(socket);
      } catch (IOException e) {
         throw e;
      } catch (Exception e) {
         throw new IOException(e.getMessage(), e);
      }
   }

   @Override
   public String[] getDefaultCipherSuites() {
      return this.socketFactory.getDefaultCipherSuites();
   }

   @Override
   public String[] getSupportedCipherSuites() {
      return this.socketFactory.getSupportedCipherSuites();
   }

   private SSLContext createSSLContext(ServerTlsParameters tlsParams) throws Exception {
      if (!CertUtils.isValidServerCert(tlsParams.getCertAlias(), this.mgr.getKeyStore())) {
         throw new IOException("invalid server certificate requested");
      }

      SSLContext sslContext = SSLContext.getInstance(CryptoSupport.TYPES.get(tlsParams.getMinTlsProtocol().toLowerCase()));
      KeyManager[] keyManagers = new AliasedKeyManagerBuilder(
            this.secInfo, tlsParams.getCertAlias(), tlsParams.getKeyPassphrase() == null ? null : new SecretChars(tlsParams.getKeyPassphrase(), true)
         )
         .getKeyManagers();
      TrustManager[] trustManagers;
      if (tlsParams.getWantClientCertAuth()) {
         trustManagers = TrustManagerBuilder.getTrustManagers(this.mgr.getTrustAnchors());
      } else {
         trustManagers = new TrustManager[0];
      }

      try {
         AccessController.doPrivileged(() -> {
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            return null;
         });
         return sslContext;
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   private void checkKeyAndCert() throws IOException {
      boolean hasKey = false;

      try {
         hasKey = this.mgr.getKeyStore().isKeyEntry(this.tlsParams.getCertAlias());
      } catch (Exception e1) {
         throw new IOException("error accessing keystore");
      }

      if (!hasKey) {
         throw new IllegalArgumentException("Unable to find key " + this.tlsParams.getCertAlias() + " in keystore");
      }

      try {
         X509Certificate[] chain = this.mgr.getKeyStore().getCertificateChain(this.tlsParams.getCertAlias());
         if (chain != null && TridiumCertValidator.isOutOfDate(chain)) {
            if (this.log != null) {
               this.log.warning("cert chain for " + this.tlsParams.getCertAlias() + " has an out of date certificate");
            } else {
               System.err.println("WARNING [" + new Date() + "][nre] cert chain for " + this.tlsParams.getCertAlias() + " has an out of date certificate");
            }
         }
      } catch (Exception var3) {
      }
   }

   protected void setType(String type) throws Exception {
      if (CryptoSupport.TYPES.get(type.toLowerCase()) == null) {
         throw new IllegalArgumentException("invalid protocol type: " + type);
      }

      this.protocols = CryptoSupport.TYPE_LISTS.get(type.toLowerCase());
   }

   public class NSSLServerSocket extends SSLServerSocket {
      private final SSLServerSocket serverSocket;

      public NSSLServerSocket(SSLServerSocket serverSocket) throws IOException {
         this.serverSocket = serverSocket;
         serverSocket.setNeedClientAuth(false);
         serverSocket.setWantClientAuth(CryptoCoreServerSocketFactory.this.tlsParams.getWantClientCertAuth());
         serverSocket.setEnabledCipherSuites(CryptoCoreServerSocketFactory.this.tlsParams.getTlsCipherSuiteGroup().getEnabledCipherSuites());
         serverSocket.setEnabledProtocols(CryptoCoreServerSocketFactory.this.protocols);
      }

      @Override
      public Socket accept() throws IOException {
         return this.serverSocket.accept();
      }

      @Override
      public void bind(SocketAddress endpoint, int backlog) throws IOException {
         this.serverSocket.bind(endpoint, backlog);
      }

      @Override
      public void bind(SocketAddress endpoint) throws IOException {
         this.serverSocket.bind(endpoint);
      }

      @Override
      public ServerSocketChannel getChannel() {
         return this.serverSocket.getChannel();
      }

      @Override
      public InetAddress getInetAddress() {
         return this.serverSocket.getInetAddress();
      }

      @Override
      public int getLocalPort() {
         return this.serverSocket.getLocalPort();
      }

      @Override
      public SocketAddress getLocalSocketAddress() {
         return this.serverSocket.getLocalSocketAddress();
      }

      @Override
      public synchronized int getReceiveBufferSize() throws SocketException {
         return this.serverSocket.getReceiveBufferSize();
      }

      @Override
      public boolean getReuseAddress() throws SocketException {
         return this.serverSocket.getReuseAddress();
      }

      @Override
      public synchronized int getSoTimeout() throws IOException {
         return this.serverSocket.getSoTimeout();
      }

      @Override
      public boolean isBound() {
         return this.serverSocket.isBound();
      }

      @Override
      public boolean isClosed() {
         return this.serverSocket.isClosed();
      }

      @Override
      public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
         this.serverSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
      }

      @Override
      public synchronized void setReceiveBufferSize(int size) throws SocketException {
         this.serverSocket.setReceiveBufferSize(size);
      }

      @Override
      public void setReuseAddress(boolean on) throws SocketException {
         this.serverSocket.setReuseAddress(on);
      }

      @Override
      public synchronized void setSoTimeout(int timeout) throws SocketException {
         this.serverSocket.setSoTimeout(timeout);
      }

      @Override
      public void close() throws IOException {
         this.serverSocket.close();
      }

      @Override
      public String toString() {
         return this.serverSocket.toString();
      }

      @Override
      public boolean getEnableSessionCreation() {
         return this.serverSocket.getEnableSessionCreation();
      }

      @Override
      public String[] getEnabledCipherSuites() {
         return this.serverSocket.getEnabledCipherSuites();
      }

      @Override
      public String[] getEnabledProtocols() {
         return this.serverSocket.getEnabledProtocols();
      }

      @Override
      public boolean getNeedClientAuth() {
         return this.serverSocket.getNeedClientAuth();
      }

      @Override
      public String[] getSupportedCipherSuites() {
         return this.serverSocket.getSupportedCipherSuites();
      }

      @Override
      public String[] getSupportedProtocols() {
         return this.serverSocket.getSupportedProtocols();
      }

      @Override
      public boolean getUseClientMode() {
         return this.serverSocket.getUseClientMode();
      }

      @Override
      public boolean getWantClientAuth() {
         return this.serverSocket.getWantClientAuth();
      }

      @Override
      public void setEnableSessionCreation(boolean flag) {
         this.serverSocket.setEnableSessionCreation(flag);
      }

      @Override
      public void setEnabledCipherSuites(String[] suites) {
         this.serverSocket.setEnabledCipherSuites(suites);
      }

      @Override
      public void setEnabledProtocols(String[] protocols) {
         this.serverSocket.setEnabledProtocols(protocols);
      }

      @Override
      public void setNeedClientAuth(boolean need) {
         this.serverSocket.setNeedClientAuth(need);
      }

      @Override
      public void setUseClientMode(boolean mode) {
         this.serverSocket.setUseClientMode(mode);
      }

      @Override
      public void setWantClientAuth(boolean want) {
         this.serverSocket.setWantClientAuth(want);
      }
   }
}
