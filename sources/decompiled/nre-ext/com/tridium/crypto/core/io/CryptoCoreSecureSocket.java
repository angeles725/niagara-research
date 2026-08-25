package com.tridium.crypto.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.bouncycastle.util.IPAddress;

public class CryptoCoreSecureSocket extends SSLSocket {
   private final SSLSocket innerSocket;
   private static final Logger LOG = Logger.getLogger("crypto");

   public CryptoCoreSecureSocket(SSLSocket innerSocket) {
      this.innerSocket = innerSocket;
   }

   @Override
   public void connect(SocketAddress endpoint) throws IOException {
      this.setSNIHostName(endpoint);
      this.innerSocket.connect(endpoint);
   }

   @Override
   public void connect(SocketAddress endpoint, int timeout) throws IOException {
      this.setSNIHostName(endpoint);
      this.innerSocket.connect(endpoint, timeout);
   }

   private void setSNIHostName(SocketAddress endpoint) {
      if (endpoint instanceof InetSocketAddress) {
         String hostname = ((InetSocketAddress)endpoint).getHostString();
         if (null != hostname && hostname.indexOf(46) > 0 && !IPAddress.isValid(hostname)) {
            try {
               SNIHostName sniHostName = new SNIHostName(hostname);
               SSLParameters params = this.innerSocket.getSSLParameters();
               params.setServerNames(Collections.singletonList(sniHostName));
               this.innerSocket.setSSLParameters(params);
               if (LOG.isLoggable(Level.FINER)) {
                  LOG.finer("Set SNI hostname on socket: " + hostname);
               }
            } catch (RuntimeException var5) {
            }
         }
      }
   }

   @Override
   public String[] getSupportedCipherSuites() {
      return this.innerSocket.getSupportedCipherSuites();
   }

   @Override
   public String[] getEnabledCipherSuites() {
      return this.innerSocket.getEnabledCipherSuites();
   }

   @Override
   public void setEnabledCipherSuites(String[] strings) {
      this.innerSocket.setEnabledCipherSuites(strings);
   }

   @Override
   public String[] getSupportedProtocols() {
      return this.innerSocket.getSupportedProtocols();
   }

   @Override
   public String[] getEnabledProtocols() {
      return this.innerSocket.getEnabledProtocols();
   }

   @Override
   public void setEnabledProtocols(String[] strings) {
      this.innerSocket.setEnabledProtocols(strings);
   }

   @Override
   public SSLSession getSession() {
      return this.innerSocket.getSession();
   }

   @Override
   public SSLSession getHandshakeSession() {
      return this.innerSocket.getHandshakeSession();
   }

   @Override
   public void addHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
      this.innerSocket.addHandshakeCompletedListener(handshakeCompletedListener);
   }

   @Override
   public void removeHandshakeCompletedListener(HandshakeCompletedListener handshakeCompletedListener) {
      this.innerSocket.removeHandshakeCompletedListener(handshakeCompletedListener);
   }

   @Override
   public void startHandshake() throws IOException {
      this.innerSocket.startHandshake();
   }

   @Override
   public void setUseClientMode(boolean b) {
      this.innerSocket.setUseClientMode(b);
   }

   @Override
   public boolean getUseClientMode() {
      return this.innerSocket.getUseClientMode();
   }

   @Override
   public void setNeedClientAuth(boolean b) {
      this.innerSocket.setNeedClientAuth(b);
   }

   @Override
   public boolean getNeedClientAuth() {
      return this.innerSocket.getNeedClientAuth();
   }

   @Override
   public void setWantClientAuth(boolean b) {
      this.innerSocket.setWantClientAuth(b);
   }

   @Override
   public boolean getWantClientAuth() {
      return this.innerSocket.getWantClientAuth();
   }

   @Override
   public void setEnableSessionCreation(boolean b) {
      this.innerSocket.setEnableSessionCreation(b);
   }

   @Override
   public boolean getEnableSessionCreation() {
      return this.innerSocket.getEnableSessionCreation();
   }

   @Override
   public SSLParameters getSSLParameters() {
      return this.innerSocket.getSSLParameters();
   }

   @Override
   public void setSSLParameters(SSLParameters sslParameters) {
      this.innerSocket.setSSLParameters(sslParameters);
   }

   @Override
   public String getApplicationProtocol() {
      return this.innerSocket.getApplicationProtocol();
   }

   @Override
   public String getHandshakeApplicationProtocol() {
      return this.innerSocket.getHandshakeApplicationProtocol();
   }

   @Override
   public void setHandshakeApplicationProtocolSelector(BiFunction<SSLSocket, List<String>, String> biFunction) {
      this.innerSocket.setHandshakeApplicationProtocolSelector(biFunction);
   }

   @Override
   public BiFunction<SSLSocket, List<String>, String> getHandshakeApplicationProtocolSelector() {
      return this.innerSocket.getHandshakeApplicationProtocolSelector();
   }

   @Override
   public void bind(SocketAddress bindpoint) throws IOException {
      this.innerSocket.bind(bindpoint);
   }

   @Override
   public InetAddress getInetAddress() {
      return this.innerSocket.getInetAddress();
   }

   @Override
   public InetAddress getLocalAddress() {
      return this.innerSocket.getLocalAddress();
   }

   @Override
   public int getPort() {
      return this.innerSocket.getPort();
   }

   @Override
   public int getLocalPort() {
      return this.innerSocket.getLocalPort();
   }

   @Override
   public SocketAddress getRemoteSocketAddress() {
      return this.innerSocket.getRemoteSocketAddress();
   }

   @Override
   public SocketAddress getLocalSocketAddress() {
      return this.innerSocket.getLocalSocketAddress();
   }

   @Override
   public SocketChannel getChannel() {
      return this.innerSocket.getChannel();
   }

   @Override
   public InputStream getInputStream() throws IOException {
      return this.innerSocket.getInputStream();
   }

   @Override
   public OutputStream getOutputStream() throws IOException {
      return this.innerSocket.getOutputStream();
   }

   @Override
   public void setTcpNoDelay(boolean on) throws SocketException {
      this.innerSocket.setTcpNoDelay(on);
   }

   @Override
   public boolean getTcpNoDelay() throws SocketException {
      return this.innerSocket.getTcpNoDelay();
   }

   @Override
   public void setSoLinger(boolean on, int linger) throws SocketException {
      this.innerSocket.setSoLinger(on, linger);
   }

   @Override
   public int getSoLinger() throws SocketException {
      return this.innerSocket.getSoLinger();
   }

   @Override
   public void sendUrgentData(int data) throws IOException {
      this.innerSocket.sendUrgentData(data);
   }

   @Override
   public void setOOBInline(boolean on) throws SocketException {
      this.innerSocket.setOOBInline(on);
   }

   @Override
   public boolean getOOBInline() throws SocketException {
      return this.innerSocket.getOOBInline();
   }

   @Override
   public void setSoTimeout(int timeout) throws SocketException {
      this.innerSocket.setSoTimeout(timeout);
   }

   @Override
   public int getSoTimeout() throws SocketException {
      return this.innerSocket.getSoTimeout();
   }

   @Override
   public void setSendBufferSize(int size) throws SocketException {
      this.innerSocket.setSendBufferSize(size);
   }

   @Override
   public int getSendBufferSize() throws SocketException {
      return this.innerSocket.getSendBufferSize();
   }

   @Override
   public void setReceiveBufferSize(int size) throws SocketException {
      this.innerSocket.setReceiveBufferSize(size);
   }

   @Override
   public int getReceiveBufferSize() throws SocketException {
      return this.innerSocket.getReceiveBufferSize();
   }

   @Override
   public void setKeepAlive(boolean on) throws SocketException {
      this.innerSocket.setKeepAlive(on);
   }

   @Override
   public boolean getKeepAlive() throws SocketException {
      return this.innerSocket.getKeepAlive();
   }

   @Override
   public void setTrafficClass(int tc) throws SocketException {
      this.innerSocket.setTrafficClass(tc);
   }

   @Override
   public int getTrafficClass() throws SocketException {
      return this.innerSocket.getTrafficClass();
   }

   @Override
   public void setReuseAddress(boolean on) throws SocketException {
      this.innerSocket.setReuseAddress(on);
   }

   @Override
   public boolean getReuseAddress() throws SocketException {
      return this.innerSocket.getReuseAddress();
   }

   @Override
   public void close() throws IOException {
      this.innerSocket.close();
   }

   @Override
   public void shutdownInput() throws IOException {
      this.innerSocket.shutdownInput();
   }

   @Override
   public void shutdownOutput() throws IOException {
      this.innerSocket.shutdownOutput();
   }

   @Override
   public String toString() {
      return this.innerSocket.toString();
   }

   @Override
   public boolean isConnected() {
      return this.innerSocket.isConnected();
   }

   @Override
   public boolean isBound() {
      return this.innerSocket.isBound();
   }

   @Override
   public boolean isClosed() {
      return this.innerSocket.isClosed();
   }

   @Override
   public boolean isInputShutdown() {
      return this.innerSocket.isInputShutdown();
   }

   @Override
   public boolean isOutputShutdown() {
      return this.innerSocket.isOutputShutdown();
   }

   @Override
   public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
      this.innerSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
   }
}
