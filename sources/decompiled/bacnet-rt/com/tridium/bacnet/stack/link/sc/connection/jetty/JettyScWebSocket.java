package com.tridium.bacnet.stack.link.sc.connection.jetty;

import com.tridium.bacnet.stack.link.sc.connection.BInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.IScConnection;
import com.tridium.bacnet.stack.link.sc.connection.IScWebSocket;
import com.tridium.bacnet.stack.link.sc.connection.ScConnectionUtil;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.net.ssl.SSLException;
import org.bouncycastle.tls.TlsException;
import org.bouncycastle.tls.TlsFatalAlert;
import org.bouncycastle.tls.TlsFatalAlertReceived;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpStatus.Code;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WriteCallback;

public final class JettyScWebSocket extends WebSocketAdapter implements IScWebSocket {
   private final AtomicReference<IScConnection> connectionReference = new AtomicReference<>();
   private final JettyScWebSocket.SendCallback sendCallback = new JettyScWebSocket.SendCallback();
   private static final String CONNECTION_ERROR = "The Connection was not available";
   private static final String TEXT_ERROR = "WebSocket text data frames cannot be accepted";

   public JettyScWebSocket(IScConnection scConnection) {
      this.connectionReference.set(scConnection);
   }

   public void onWebSocketConnect(Session session) {
      super.onWebSocketConnect(session);
      IScConnection connection = this.connectionReference.get();
      if (connection != null) {
         if (connection instanceof BInitiatingConnection) {
            String acceptedSubProtocol = session.getUpgradeResponse().getAcceptedSubProtocol();
            String requiredSubProtocol = ((BInitiatingConnection)connection).getSubProtocol();
            if (!requiredSubProtocol.equals(acceptedSubProtocol)) {
               String reason = "Accepted sub-protocol " + acceptedSubProtocol + " does not match required value " + requiredSubProtocol;
               this.connectionReference.set(null);
               session.close(1002, reason);
               connection.webSocketFailed(BBacnetErrorCode.websocketProtocolError, reason);
               return;
            }
         }

         connection.webSocketConnected(this);
      } else {
         session.close(1011, "The Connection was not available");
      }
   }

   public void onWebSocketBinary(byte[] payload, int offset, int len) {
      super.onWebSocketBinary(payload, offset, len);
      IScConnection connection = this.connectionReference.get();
      if (connection != null) {
         connection.messageReceived(payload, offset, len);
      } else {
         Session session = this.getSession();
         if (session != null) {
            session.close(1011, "The Connection was not available");
         }
      }
   }

   public void onWebSocketText(String message) {
      super.onWebSocketText(message);
      IScConnection connection = this.connectionReference.getAndSet(null);
      Session session = this.getSession();
      if (session != null) {
         session.close(1003, "WebSocket text data frames cannot be accepted");
      }

      if (connection != null) {
         connection.webSocketFailed(BBacnetErrorCode.websocketDataNotAccepted, "WebSocket text data frames cannot be accepted");
      }
   }

   public void onWebSocketClose(int statusCode, String reason) {
      super.onWebSocketClose(statusCode, reason);
      IScConnection connection = this.connectionReference.getAndSet(null);
      if (connection != null) {
         connection.webSocketFailed(ScConnectionUtil.getWebSocketError(statusCode), reason);
      }
   }

   public void onWebSocketError(Throwable cause) {
      super.onWebSocketError(cause);
      IScConnection connection = this.connectionReference.getAndSet(null);
      Session session = this.getSession();
      if (session != null) {
         session.close(1011, cause.getClass().getSimpleName());
      }

      if (connection != null) {
         Logger logger = connection.getLogger();
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, connection.getLogInfo().append(": webSocketError").toString(), cause);
         }

         if (cause instanceof UpgradeException) {
            handleUpgradeException((UpgradeException)cause, connection);
            return;
         }

         if (cause instanceof SSLException || cause instanceof TlsException) {
            handleTlsException(cause, connection);
            return;
         }

         BBacnetErrorCode errorCode;
         if (cause instanceof UnknownHostException) {
            errorCode = BBacnetErrorCode.dnsError;
         } else if (cause instanceof ConnectException) {
            errorCode = BBacnetErrorCode.tcpConnectionRefused;
         } else if (cause instanceof SocketTimeoutException) {
            errorCode = BBacnetErrorCode.tcpConnectTimeout;
         } else if (cause instanceof SocketException) {
            errorCode = BBacnetErrorCode.ipAddressNotReachable;
         } else {
            errorCode = BBacnetErrorCode.ipError;
         }

         connection.webSocketFailed(errorCode, getCauseInfo(cause));
      }
   }

   private static void handleTlsException(Throwable cause, IScConnection connection) {
      Throwable tlsFatalAlert = null;
      if (cause instanceof TlsFatalAlert || cause instanceof TlsFatalAlertReceived) {
         tlsFatalAlert = cause;
      } else if (cause.getCause() instanceof TlsFatalAlert || cause.getCause() instanceof TlsFatalAlertReceived) {
         tlsFatalAlert = cause.getCause();
      }

      if (tlsFatalAlert instanceof TlsFatalAlert) {
         BBacnetErrorCode errorCode = getTlsFatalAlertErrorCode(((TlsFatalAlert)tlsFatalAlert).getAlertDescription(), false);
         connection.webSocketFailed(errorCode, getCauseInfo(tlsFatalAlert));
      } else if (tlsFatalAlert instanceof TlsFatalAlertReceived) {
         BBacnetErrorCode errorCode = getTlsFatalAlertErrorCode(((TlsFatalAlertReceived)tlsFatalAlert).getAlertDescription(), true);
         connection.webSocketFailed(errorCode, getCauseInfo(tlsFatalAlert));
      } else {
         connection.webSocketFailed(BBacnetErrorCode.tlsError, getCauseInfo(cause));
      }
   }

   private static BBacnetErrorCode getTlsFatalAlertErrorCode(short alertDescription, boolean isClient) {
      switch (alertDescription) {
         case 20:
         case 42:
         case 43:
         case 46:
         case 47:
            return isClient ? BBacnetErrorCode.tlsClientCertificateError : BBacnetErrorCode.tlsServerCertificateError;
         case 40:
         case 48:
         case 49:
         case 70:
         case 71:
         case 111:
         case 115:
         case 116:
            return isClient ? BBacnetErrorCode.tlsClientAuthenticationFailed : BBacnetErrorCode.tlsServerAuthenticationFailed;
         case 44:
            return isClient ? BBacnetErrorCode.tlsClientCertificateRevoked : BBacnetErrorCode.tlsServerCertificateRevoked;
         case 45:
            return isClient ? BBacnetErrorCode.tlsClientCertificateExpired : BBacnetErrorCode.tlsServerCertificateExpired;
         default:
            return BBacnetErrorCode.tlsError;
      }
   }

   private static String getCauseInfo(Throwable cause) {
      StringBuilder causeInfo = new StringBuilder(cause.getClass().getSimpleName());
      String message = cause.getMessage();
      if (message != null && !message.isEmpty()) {
         causeInfo.append(": ").append(message);
      }

      return causeInfo.toString();
   }

   private static void handleUpgradeException(UpgradeException upgradeException, IScConnection connection) {
      int statusCode = upgradeException.getResponseStatusCode();
      BBacnetErrorCode errorCode;
      switch (statusCode) {
         case 100:
         case 101:
         case 102:
         case 200:
         case 201:
         case 202:
         case 203:
         case 204:
         case 205:
         case 206:
         case 207:
         case 208:
         case 226:
         case 402:
         case 405:
         case 406:
         case 409:
         case 410:
         case 413:
         case 414:
         case 415:
         case 421:
         case 422:
         case 423:
         case 424:
         case 425:
         case 428:
         case 451:
         case 503:
         case 504:
         case 506:
         case 507:
         case 508:
         case 510:
         case 511:
            errorCode = BBacnetErrorCode.httpUnexpectedResponseCode;
            break;
         case 300:
         case 301:
         case 302:
         case 303:
         case 304:
         case 305:
         case 307:
         case 308:
            errorCode = BBacnetErrorCode.httpResourceNotLocal;
            break;
         case 401:
            errorCode = BBacnetErrorCode.tlsClientAuthenticationFailed;
            break;
         case 407:
            errorCode = BBacnetErrorCode.httpProxyAuthenticationFailed;
            break;
         default:
            errorCode = BBacnetErrorCode.httpError;
      }

      StringBuilder errorDetails = new StringBuilder("UpgradeException (HTTP status code: ").append(statusCode);
      Code statusCodeEnum = HttpStatus.getCode(statusCode);
      if (statusCodeEnum != null) {
         errorDetails.append(' ').append(statusCodeEnum.getMessage());
      }

      errorDetails.append(')');
      String message = upgradeException.getMessage();
      if (message != null && !message.isEmpty()) {
         errorDetails.append(": ").append(message);
      }

      connection.webSocketFailed(errorCode, errorDetails.toString());
   }

   @Override
   public void sendBytes(byte[] data) throws Exception {
      Session session = this.getSession();
      if (session != null && session.isOpen()) {
         session.getRemote().sendBytes(ByteBuffer.wrap(data), this.sendCallback);
      } else {
         throw new Exception("session is not connected");
      }
   }

   @Override
   public void close(int statusCode, String reason) {
      this.connectionReference.set(null);
      Session session = this.getSession();
      if (session != null) {
         session.close(statusCode, reason);
      }
   }

   @Override
   public void setIdleTimeout(long timeout) {
      Session session = this.getSession();
      if (session != null) {
         this.getSession().setIdleTimeout(timeout);
      }
   }

   @Override
   public String getRemoteHost() {
      InetSocketAddress remoteAddress = this.getRemoteAddress();
      return remoteAddress != null ? remoteAddress.getHostString() : "";
   }

   @Override
   public int getRemotePort() {
      InetSocketAddress remoteAddress = this.getRemoteAddress();
      return remoteAddress != null ? remoteAddress.getPort() : 0;
   }

   private InetSocketAddress getRemoteAddress() {
      RemoteEndpoint remoteEndpoint = this.getRemote();
      return remoteEndpoint != null ? remoteEndpoint.getInetSocketAddress() : null;
   }

   private class SendCallback implements WriteCallback {
      private SendCallback() {
      }

      public void writeFailed(Throwable x) {
         IScConnection connection = JettyScWebSocket.this.connectionReference.getAndSet(null);
         Session session = JettyScWebSocket.this.getSession();
         if (session != null) {
            session.close(1011, "Failed to write message: " + x.getClass().getSimpleName());
         }

         if (connection != null) {
            connection.webSocketFailed(BBacnetErrorCode.websocketRequestUnavailable, x.getClass().getSimpleName());
         }
      }

      public void writeSuccess() {
      }
   }
}
