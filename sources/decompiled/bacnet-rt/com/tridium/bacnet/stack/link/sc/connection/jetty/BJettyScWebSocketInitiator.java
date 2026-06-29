package com.tridium.bacnet.stack.link.sc.connection.jetty;

import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.connection.BAbstractScWebSocketInitiator;
import com.tridium.bacnet.stack.link.sc.connection.BInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.IScWebSocket;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.TrustManagerBuilder;
import com.tridium.nre.security.SecretChars;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetException;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.security.ClientTlsParameters;
import javax.baja.security.AuthenticationException;
import javax.baja.security.BCertificateAliasAndPassword;
import javax.baja.security.BPassword;
import javax.baja.security.crypto.BSslTlsEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.net.ssl.TrustManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

@NiagaraType
@NiagaraProperty(
   name = "performHostnameValidation",
   type = "boolean",
   defaultValue = "false",
   facets = {@Facet(
      name = "BFacets.SECURITY",
      value = "true"
   )}
)
public final class BJettyScWebSocketInitiator extends BAbstractScWebSocketInitiator {
   public static final Property performHostnameValidation = newProperty(0, false, BFacets.make("security", true));
   public static final Type TYPE = Sys.loadType(BJettyScWebSocketInitiator.class);
   private final AtomicReference<WebSocketClient> clientRef = new AtomicReference<>();
   private static final Logger logger = Logger.getLogger("bacnet.sc.linkLayer");

   public boolean getPerformHostnameValidation() {
      return this.getBoolean(performHostnameValidation);
   }

   public void setPerformHostnameValidation(boolean v) {
      this.setBoolean(performHostnameValidation, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void stopped() throws Exception {
      super.stopped();
      this.stopClient();
   }

   @Override
   public IScWebSocket initiateWebSocket(BInitiatingConnection connection) throws Exception {
      IScWebSocket webSocket = new JettyScWebSocket(connection);
      URI uri = connection.getURI();
      ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
      clientUpgradeRequest.setSubProtocols(new String[]{connection.getSubProtocol()});
      String localConnectionToken = connection.getLocalConnectionToken();
      if (localConnectionToken != null) {
         clientUpgradeRequest.setHeader("Niagara-Local-Connection-Token", localConnectionToken);
      }

      this.clientRef.get().connect(webSocket, uri, clientUpgradeRequest);
      return webSocket;
   }

   @Override
   public void linkCommStart() throws Exception {
      this.stopClient();
      if (!this.isRunning()) {
         throw new BacnetException("linkCommStart called on JettyScWebSocketInitiator that is not running");
      } else {
         WebSocketClient client = null;

         try {
            client = AccessController.doPrivileged((PrivilegedExceptionAction<WebSocketClient>)(() -> {
               HttpClient httpClient = new HttpClient(this.makeSslContextFactory());
               return new WebSocketClient(httpClient);
            }));
         } catch (PrivilegedActionException var3) {
            throw new BacnetException("Failed to create WebSocketClient", var3.getException());
         }

         JettyScUtil.configurePolicy(client.getPolicy(), ((BScLinkLayer)this.getParent()).getMaxBvlcLength());
         if (this.clientRef.compareAndSet(null, client)) {
            client.start();
         }
      }
   }

   @Override
   public void linkCommStop() {
      this.stopClient();
   }

   @Override
   public void updateWebSocketSettings() {
      try {
         this.clientRef.get().getPolicy().setMaxBinaryMessageSize(((BScLinkLayer)this.getParent()).getMaxBvlcLength());
      } catch (Exception var2) {
         ScLinkLayerUtil.logException(logger, new StringBuilder("Failed to set the web socket initiator's policy max binary message size"), var2);
      }
   }

   private void stopClient() {
      try {
         WebSocketClient client = this.clientRef.getAndSet(null);
         if (client != null) {
            client.stop();
         }
      } catch (Exception var2) {
         ScLinkLayerUtil.logException(logger, new StringBuilder("Failed to stop the BACnet SC link layer's JettyScWebSocketInitiator WebSocketClient"), var2);
      }
   }

   private SslContextFactory makeSslContextFactory() throws BacnetException {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<SslContextFactory>)(() -> {
            BScLinkLayer scLinkLayer = ScLinkLayerUtil.getScLinkLayer(this);
            Set<TrustAnchor> trustAnchors = new HashSet<>();
            scLinkLayer.addTrustAnchors(trustAnchors);
            if (trustAnchors.isEmpty()) {
               throw new AuthenticationException("No trust anchors specified for bacnet sc");
            } else {
               Set<X509CRL> crls = new HashSet<>();
               scLinkLayer.addCRLs(crls);
               String protocol = BSslTlsEnum.tlsv1_3.getTag();
               BCertificateAliasAndPassword operationalCert = scLinkLayer.getCredentials().getOperationalCertificateAliasAndPassword();
               String certAlias = operationalCert.getAlias();
               TrustManager[] trustManagers = TrustManagerBuilder.getTrustManagers(trustAnchors, crls);
               ClientTlsParameters tlsParams = new ClientTlsParameters(protocol, certAlias);
               BPassword certPassword = operationalCert.getPassword();
               if (!certPassword.isDefault()) {
                  SecretChars passwordChars = AccessController.doPrivileged(certPassword::getSecretChars);
                  Throwable var11 = null;

                  try {
                     tlsParams.setKeyPassphrase(passwordChars.get());
                  } catch (Throwable var20) {
                     var11 = var20;
                     throw var20;
                  } finally {
                     if (passwordChars != null) {
                        if (var11 != null) {
                           try {
                              passwordChars.close();
                           } catch (Throwable var19) {
                              var11.addSuppressed(var19);
                           }
                        } else {
                           passwordChars.close();
                        }
                     }
                  }
               }

               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("The WebSocket client for SC port " + scLinkLayer.getParent().getName() + " is using these TLS params: " + tlsParams);
               }

               SslContextFactory factory = CoreCryptoManager.get().getSslContextFactory(tlsParams, trustManagers, !crls.isEmpty());
               if (!this.getPerformHostnameValidation()) {
                  factory.setEndpointIdentificationAlgorithm(null);
               }

               factory.setValidateCerts(false);
               return factory;
            }
         }));
      } catch (PrivilegedActionException var2) {
         ScLinkLayerUtil.logException(
            logger, new StringBuilder("Error creating SSL context factory for the JettyScWebSocketInitiator WebSocketClient"), var2.getException()
         );
         throw new BacnetException(var2.getException());
      }
   }
}
