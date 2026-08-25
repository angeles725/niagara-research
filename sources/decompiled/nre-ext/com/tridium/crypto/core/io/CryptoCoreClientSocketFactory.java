package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.DefaultExemptionApprover;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.AccessController;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.baja.nre.security.ClientTlsParameters;
import javax.baja.nre.security.ExemptionApprover;
import javax.baja.nre.security.ExemptionHandler;
import javax.baja.nre.security.IClientCertSelector;
import javax.baja.nre.security.IClientCertSelectorHandler;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class CryptoCoreClientSocketFactory extends SSLSocketFactory implements IClientCertSelectorHandler, ExemptionHandler {
   private static final Logger logger = Logger.getLogger("crypto");
   private final ISecurityInfoProvider secInfo;
   private final SSLSocketFactory socketFactory;
   private final ClientTlsParameters tlsParams;
   private String[] protocols = CryptoSupport.TYPE_LISTS.get("tlsv1");
   private ExemptionApprover exemptionApprover = new DefaultExemptionApprover();
   private IClientCertSelector clientCertSelector = null;
   private final String[] defaultCipherSuites;
   private final String[] supportedCipherSuites;
   private CoreClientTrustManager coreClientTrustManager;

   public CryptoCoreClientSocketFactory() throws Exception {
      this(AccessController.doPrivileged(() -> SecurityInitializer.getInstance().getSecurityInfoProvider()), ClientTlsParameters.DEFAULT);
   }

   public CryptoCoreClientSocketFactory(ClientTlsParameters tlsParams) throws Exception {
      this(AccessController.doPrivileged(() -> SecurityInitializer.getInstance().getSecurityInfoProvider()), tlsParams);
   }

   public CryptoCoreClientSocketFactory(ISecurityInfoProvider secInfo, ClientTlsParameters tlsParams) throws Exception {
      this.secInfo = secInfo;
      this.tlsParams = tlsParams;
      SSLContext context = this.getSSLContext(tlsParams.getMinTlsProtocol());
      this.socketFactory = context.getSocketFactory();
      this.defaultCipherSuites = this.socketFactory.getDefaultCipherSuites();
      this.supportedCipherSuites = this.socketFactory.getSupportedCipherSuites();
      this.setType(tlsParams.getMinTlsProtocol());
   }

   @Override
   public Socket createSocket(InetAddress host, int port) throws IOException {
      SSLSocket socket = (SSLSocket)this.socketFactory.createSocket(host, port);
      this.initSocket(socket, getHostName(host), port);
      return socket;
   }

   public Socket createSocket(InetAddress host, int port, int timeout) throws IOException {
      SSLSocket socket = new CryptoCoreSecureSocket((SSLSocket)this.socketFactory.createSocket());
      socket.connect(new InetSocketAddress(host, port), timeout);
      this.initSocket(socket, getHostName(host), port);
      return socket;
   }

   @Override
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException {
      SSLSocket socket = (SSLSocket)this.socketFactory.createSocket(host, port, localAddress, localPort);
      this.initSocket(socket, getHostName(host), port);
      return socket;
   }

   @Override
   public Socket createSocket(String host, int port) throws IOException {
      SSLSocket socket = (SSLSocket)this.socketFactory.createSocket(host, port);
      this.initSocket(socket, host, port);
      return socket;
   }

   public Socket createSocket(String host, int port, int timeout) throws IOException {
      SSLSocket socket = new CryptoCoreSecureSocket((SSLSocket)this.socketFactory.createSocket());
      socket.connect(new InetSocketAddress(host, port), timeout);
      this.initSocket(socket, host, port);
      return socket;
   }

   @Override
   public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
      SSLSocket socket = (SSLSocket)this.socketFactory.createSocket(host, port, localAddress, localPort);
      this.initSocket(socket, host, port);
      return socket;
   }

   @Override
   public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
      SSLSocket socket = (SSLSocket)this.socketFactory.createSocket(s, host, port, autoClose);
      this.initSocket(socket, host, port);
      return socket;
   }

   @Override
   public Socket createSocket() throws IOException {
      SSLSocket socket = (SSLSocket)this.socketFactory.createSocket();
      this.initSocket(socket);
      return new CryptoCoreSecureSocket(socket);
   }

   @Override
   public String[] getDefaultCipherSuites() {
      return this.defaultCipherSuites;
   }

   @Override
   public String[] getSupportedCipherSuites() {
      return this.supportedCipherSuites;
   }

   protected static String getHostName(InetAddress host) {
      return AccessController.doPrivileged(() -> {
         try {
            Method holderGetter = InetAddress.class.getDeclaredMethod("holder");
            holderGetter.setAccessible(true);
            Object holder = holderGetter.invoke(host);
            Method getOriginalHostName = holder.getClass().getDeclaredMethod("getOriginalHostName");
            getOriginalHostName.setAccessible(true);
            String originalHostName = (String)getOriginalHostName.invoke(holder);
            return originalHostName != null ? originalHostName : host.getHostAddress();
         } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.warning("Error retrieving original hostname, falling back to reverse DNS hostname: " + e);
            return host.getHostName();
         }
      });
   }

   protected SSLContext getSSLContext(String type) throws Exception {
      SSLContext sslContext = SSLContext.getInstance(CryptoSupport.TYPES.get(type.toLowerCase()));
      KeyManager[] km = null;
      if (this.tlsParams.getCertAlias() != null) {
         km = new AliasedKeyManagerBuilder(
               this.secInfo,
               this.tlsParams.getCertAlias(),
               this.tlsParams.getKeyPassphrase() == null ? null : new SecretChars(this.tlsParams.getKeyPassphrase(), true)
            )
            .getKeyManagers();
      }

      this.coreClientTrustManager = CoreClientTrustManager.make(this.secInfo, () -> this.exemptionApprover);
      TrustManager[] tm = new TrustManager[]{this.coreClientTrustManager};
      sslContext.init(km, tm, new SecureRandom());
      return sslContext;
   }

   private void initSocket(SSLSocket socket, String host, int port) {
      this.initSocket(socket);
      this.coreClientTrustManager.setExpectedHostInfo(socket, host, port);
   }

   private void initSocket(SSLSocket socket) {
      socket.setEnabledProtocols(this.protocols);
      socket.setEnabledCipherSuites(this.tlsParams.getTlsCipherSuiteGroup().getEnabledCipherSuites());
   }

   protected void setType(String type) throws Exception {
      if (CryptoSupport.TYPES.get(type.toLowerCase()) == null) {
         throw new IllegalArgumentException("invalid protocol type: " + type);
      }

      this.protocols = CryptoSupport.TYPE_LISTS.get(type.toLowerCase());
   }

   @Override
   public ExemptionApprover getExemptionApprover() {
      return this.exemptionApprover;
   }

   @Override
   public void setExemptionApprover(ExemptionApprover approver) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("SET_EXEMPTION_APPROVER"));
      }

      this.exemptionApprover = approver;
   }

   @Override
   public void setClientCertSelector(IClientCertSelector selector) {
      this.clientCertSelector = selector;
   }

   @Override
   public IClientCertSelector getClientCertSelector() {
      return this.clientCertSelector;
   }

   private static class ContextCache extends LinkedHashMap<String, SSLContext> {
      private int maxSize;

      ContextCache(int maxSize) {
         super(4);
         this.maxSize = maxSize;
      }

      @Override
      protected boolean removeEldestEntry(Entry<String, SSLContext> eldest) {
         return this.size() > this.maxSize;
      }
   }
}
