package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.connection.BAbstractConnection;
import com.tridium.bacnet.stack.link.sc.connection.BAbstractScWebSocketAcceptor;
import com.tridium.bacnet.stack.link.sc.connection.BAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BHubAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BHubInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.jetty.BJettyScWebSocketAcceptor;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcMessage;
import com.tridium.bacnet.stack.link.sc.message.ScNpdu;
import com.tridium.bacnet.stack.link.sc.message.ScReadMessageException;
import com.tridium.bacnet.stack.link.sc.message.ScSendMessageException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BIRestrictedComponent;
import javax.baja.web.BWebService;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "hubMaxBvlcLength",
      type = "int",
      defaultValue = "MAX_SC_BVLC_LENGTH",
      facets = {@Facet(
         name = "BFacets.MAX",
         value = "MAX_SC_BVLC_LENGTH"
      ), @Facet(
         name = "BFacets.MIN",
         value = "HUB_MIN_BVLC_LENGTH"
      )}
   ), @NiagaraProperty(
      name = "webSocketAcceptor",
      type = "BAbstractScWebSocketAcceptor",
      defaultValue = "BJettyScWebSocketAcceptor.make(DEFAULT_SERVLET_NAME)"
   ), @NiagaraProperty(
      name = "acceptUris",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BString.TYPE)"
   ), @NiagaraProperty(
      name = "connections",
      type = "BHubFunctionConnections",
      defaultValue = "new BHubFunctionConnections()",
      flags = 1
   )})
@NiagaraAction(
   name = "configureLocalHubConnector",
   parameterType = "BBoolean",
   defaultValue = "BBoolean.TRUE",
   flags = 128,
   facets = {@Facet(
      name = "BFacets.TRUE_TEXT",
      value = "\"%lexicon(bacnet:hubConnector.connectionName.primary)%\""
   ), @Facet(
      name = "BFacets.FALSE_TEXT",
      value = "\"%lexicon(bacnet:hubConnector.connectionName.failover)%\""
   )}
)
public final class BHubFunction extends BAbstractConnectionManager implements BIRestrictedComponent {
   public static final String DEFAULT_SERVLET_NAME = "hub";
   public static final Property enabled = newProperty(0, false, null);
   public static final Property hubMaxBvlcLength = newProperty(0, 65535, BFacets.make(BFacets.make("max", 65535), BFacets.make("min", 5705)));
   public static final Property webSocketAcceptor = newProperty(0, BJettyScWebSocketAcceptor.make("hub"), null);
   public static final Property acceptUris = newProperty(0, new BBacnetArray(BString.TYPE), null);
   public static final Property connections = newProperty(1, new BHubFunctionConnections(), null);
   public static final Action configureLocalHubConnector = newAction(
      128,
      BBoolean.TRUE,
      BFacets.make(
         BFacets.make("trueText", "%lexicon(bacnet:hubConnector.connectionName.primary)%"),
         BFacets.make("falseText", "%lexicon(bacnet:hubConnector.connectionName.failover)%")
      )
   );
   public static final Type TYPE = Sys.loadType(BHubFunction.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.hubFunction");
   private final AtomicInteger messageId = new AtomicInteger();
   private final AtomicReference<String> localConnectionToken = new AtomicReference<>();

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public int getHubMaxBvlcLength() {
      return this.getInt(hubMaxBvlcLength);
   }

   public void setHubMaxBvlcLength(int v) {
      this.setInt(hubMaxBvlcLength, v, null);
   }

   @Override
   public BAbstractScWebSocketAcceptor getWebSocketAcceptor() {
      return (BAbstractScWebSocketAcceptor)this.get(webSocketAcceptor);
   }

   public void setWebSocketAcceptor(BAbstractScWebSocketAcceptor v) {
      this.set(webSocketAcceptor, v, null);
   }

   public BBacnetArray getAcceptUris() {
      return (BBacnetArray)this.get(acceptUris);
   }

   public void setAcceptUris(BBacnetArray v) {
      this.set(acceptUris, v, null);
   }

   public BHubFunctionConnections getConnections() {
      return (BHubFunctionConnections)this.get(connections);
   }

   public void setConnections(BHubFunctionConnections v) {
      this.set(connections, v, null);
   }

   public void configureLocalHubConnector(BBoolean parameter) {
      this.invoke(configureLocalHubConnector, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean acceptEnabled() {
      return this.getEnabled();
   }

   @Override
   public boolean initiateEnabled() {
      return false;
   }

   public int getNextMessageId() {
      return this.messageId.updateAndGet(ScLinkLayerUtil::incrementUnsignedShort);
   }

   public void forwardMessage(long originatingVmac, long destinationVmac, byte[] payload, int offset, int len) throws ScSendMessageException, ScReadMessageException {
      if (!this.getEnabled()) {
         throw new ScSendMessageException("Disabled hub function cannot forward messages.");
      } else if (!VmacUtil.isDeviceVmac(originatingVmac)) {
         if (logger.isLoggable(Level.WARNING)) {
            logger.warning("Dropping message with non-device originating VMAC [" + VmacUtil.vmacToString(originatingVmac) + ']');
         }
      } else if (!this.hasActiveConnection(originatingVmac)) {
         if (logger.isLoggable(Level.WARNING)) {
            logger.warning("Dropping message from unrecognized originating VMAC [" + VmacUtil.vmacToString(originatingVmac) + ']');
         }
      } else if (!VmacUtil.isDestinationVmac(destinationVmac)) {
         if (logger.isLoggable(Level.WARNING)) {
            logger.warning("Unable to forward message with invalid destination VMAC [" + VmacUtil.vmacToString(destinationVmac) + ']');
         }
      } else {
         if (destinationVmac == 281474976710655L) {
            this.forwardBroadcastMessage(originatingVmac, payload, offset, len);
         } else {
            this.forwardUnicastMessage(originatingVmac, destinationVmac, payload, offset, len);
         }
      }
   }

   private void forwardBroadcastMessage(long originatingVmac, byte[] payload, int offset, int len) throws ScReadMessageException {
      byte[] message = ScBvlcMessage.addOriginatingVmac(originatingVmac, payload, offset, len);

      for (BAbstractConnection connection : this.getActiveConnections()) {
         if (connection.getRemoteVmac() != originatingVmac) {
            sendMessage(message, connection);
         }
      }
   }

   private void forwardUnicastMessage(long originatingVmac, long destinationVmac, byte[] payload, int offset, int len) throws ScReadMessageException {
      BAbstractConnection connection = this.getActiveConnection(destinationVmac);
      if (connection == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Dropping forwarded message to destination VMAC [" + VmacUtil.vmacToString(destinationVmac) + "]; destination is not connected.");
         }
      } else if (connection.getRemoteVmac() == originatingVmac) {
         if (logger.isLoggable(Level.WARNING)) {
            logger.warning(
               "Dropping forwarded message to destination VMAC [" + VmacUtil.vmacToString(destinationVmac) + "]; destination is also the message originator."
            );
         }
      } else {
         byte[] message = ScBvlcMessage.clearDestinationVmac(originatingVmac, payload, offset, len);
         sendMessage(message, connection);
      }
   }

   private static void sendMessage(byte[] message, BAbstractConnection connection) {
      try {
         connection.sendMessage(message);
      } catch (Exception var3) {
         ScLinkLayerUtil.logException(
            logger, new StringBuilder("Exception while forwarding message to device [").append(connection.getRemoteUuid()).append(']'), var3
         );
      }
   }

   @Override
   public void forwardNpdu(long originatingVmac, long destinationVmac, ScNpdu npdu) throws Exception {
      throw new BacnetException("Hub Function does not forward NPDU messages to the link layer.");
   }

   public String generateLocalConnectionToken(BHubInitiatingConnection connection) {
      if (!this.isLocalConnection(connection)) {
         return null;
      } else {
         SecureRandom secureRandom = new SecureRandom();
         byte[] token = new byte[16];
         secureRandom.nextBytes(token);
         String encodedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
         logger.fine("Generated local connection token");
         this.localConnectionToken.set(encodedToken);
         return encodedToken;
      }
   }

   private boolean isLocalConnection(BHubInitiatingConnection connection) {
      if (!this.getEnabled()) {
         return false;
      } else {
         BInternetAddress connectionAddress = connection.getHubUriAddress();
         if (!"localhost".equals(connectionAddress.getHost())) {
            return false;
         } else {
            BWebService webService = this.getWebSocketAcceptor().getWebService();
            if (webService == null) {
               if (connection.getLogger().isLoggable(Level.FINE)) {
                  connection.getLogger()
                     .fine(connection.getLogInfo().append("Cannot connect to the local HubFunction because it is not registered on a WebService").toString());
               }

               return false;
            } else if (!webService.getHttpsEnabled()) {
               if (connection.getLogger().isLoggable(Level.FINE)) {
                  connection.getLogger()
                     .fine(connection.getLogInfo().append("Cannot connect to the local HubFunction because HTTPS is disabled on its WebService").toString());
               }

               return false;
            } else {
               int connectionPort = connectionAddress.getPort();
               if (connectionPort == -1) {
                  connectionPort = 443;
               }

               if (webService.getHttpsPort().getPublicServerPort() != connectionPort) {
                  if (connection.getLogger().isLoggable(Level.FINE)) {
                     if (connectionAddress.getPort() == -1) {
                        connection.getLogger()
                           .fine(
                              connection.getLogInfo()
                                 .append("Cannot connect to the local HubFunction because its WebService HTTPS port ")
                                 .append(webService.getHttpsPort().getPublicServerPort())
                                 .append(" is not the default HTTPS port value 443")
                                 .toString()
                           );
                     } else {
                        connection.getLogger()
                           .fine(
                              connection.getLogInfo()
                                 .append("Cannot connect to the local HubFunction because its WebService HTTPS port ")
                                 .append(webService.getHttpsPort().getPublicServerPort())
                                 .append(" does not match the connection's port ")
                                 .append(connectionPort)
                                 .toString()
                           );
                     }
                  }

                  return false;
               } else {
                  String hubPath = '/' + this.getWebSocketAcceptor().getServletName();
                  String connectionPath = connection.getHubUriPath().startsWith("/") ? connection.getHubUriPath() : '/' + connection.getHubUriPath();
                  if (!hubPath.equals(connectionPath)) {
                     if (connection.getLogger().isLoggable(Level.FINE)) {
                        connection.getLogger()
                           .fine(
                              connection.getLogInfo()
                                 .append("Cannot connect to the local HubFunction because its path \"")
                                 .append(hubPath)
                                 .append("\" does not match the connection's path \"")
                                 .append(connectionPath)
                                 .toString()
                           );
                     }

                     return false;
                  } else {
                     return true;
                  }
               }
            }
         }
      }
   }

   public boolean isValidLocalConnectionToken(String connectionToken) {
      if (connectionToken == null) {
         logger.fine("Could not validate a null local connection token");
         return false;
      } else {
         String hubToken = this.localConnectionToken.get();
         if (hubToken == null) {
            logger.fine("No local connection token to validate against");
            return false;
         } else if (hubToken.equals(connectionToken)) {
            this.localConnectionToken.set(null);
            logger.fine("Local connection token matched");
            return true;
         } else {
            logger.fine("Local connection token does not match");
            return false;
         }
      }
   }

   public void clearLocalConnectionToken() {
      logger.fine("Clearing local connection token.");
      this.localConnectionToken.set(null);
   }

   public String toString(Context cx) {
      StringBuilder builder = new StringBuilder();
      if (!this.getEnabled()) {
         builder.append(ScLinkLayerUtil.LEXICON.getText("disabled", cx));
      } else {
         builder.append(ScLinkLayerUtil.LEXICON.getText("enabled", cx));
      }

      if (this.getStatus().isFault()) {
         builder.append(", ").append(ScLinkLayerUtil.LEXICON.getText("faulted", cx));
      }

      return builder.toString();
   }

   @Override
   public Object fw(int x, Object a, Object b, Object c, Object d) {
      if (x == 11) {
         Object result = super.fw(11, a, b, c, d);
         this.fwStarted();
         return result;
      } else {
         return super.fw(x, a, b, c, d);
      }
   }

   private void fwStarted() {
      if (Sys.isStationStarted()) {
         this.trustAnchorFault = this.scLinkLayer.getNodeSwitch().trustAnchorFault;
         this.updateStatus();
      }
   }

   public void descendantsStarted() throws Exception {
      super.descendantsStarted();
      if (Sys.isStationStarted() && this.scLinkLayer.isCommStarted() && this.getEnabled()) {
         this.linkCommStart();
      }
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(enabled)) {
            if (this.getEnabled()) {
               this.linkCommStart();
            } else {
               this.linkCommStop();
            }

            this.resetHubConnector();
         } else if (property.equals(hubMaxBvlcLength)) {
            this.getWebSocketAcceptor().updateWebSocketSettings();
         }
      }
   }

   @Override
   public String getSubProtocol() {
      return "hub.bsc.bacnet.org";
   }

   @Override
   public Logger getLogger() {
      return logger;
   }

   @Override
   BConnectionContainer getConnectionContainer() {
      return this.getConnections();
   }

   @Override
   public BAcceptingConnection fetchConnection() throws Exception {
      this.checkCanActivateConnection(true);
      BHubAcceptingConnection connection = new BHubAcceptingConnection();
      this.getConnections().add("Connection1?", connection, 2);
      return connection;
   }

   @Override
   public int getMaxBvlcLength() {
      int maxBvlcLength = this.getHubMaxBvlcLength();
      if (maxBvlcLength < 5705) {
         return 5705;
      } else {
         return maxBvlcLength > 65535 ? 65535 : maxBvlcLength;
      }
   }

   @Override
   protected void checkConnectionType(BAbstractConnection connection) {
      if (!(connection instanceof BHubAcceptingConnection)) {
         throw new IllegalArgumentException("connection must be a BHubAcceptingConnection");
      }
   }

   @Override
   protected String getConnectionLimitKey() {
      return "hubConnections.limit";
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      ScLinkLayerUtil.checkParentType(TYPE, parent.getType(), BScLinkLayer.TYPE);
      ScLinkLayerUtil.checkForDuplicate(this, parent);
   }

   public void doConfigureLocalHubConnector(BBoolean isPrimary) {
      BHubConnector hubConnector = this.scLinkLayer.getHubConnector();
      BHubInitiatingConnection connection = isPrimary.getBoolean() ? hubConnector.getPrimaryConnection() : hubConnector.getFailoverConnection();
      BWebService webService = this.getWebSocketAcceptor().getWebService();
      if (webService == null) {
         throw new LocalizableRuntimeException("bacnet", "hubFunction.configureLocalHubConnector.noWebService");
      } else {
         int webServicePort = webService.getHttpsPort().getPublicServerPort();
         switch (webServicePort) {
            case 80:
            case 443:
               connection.setHubUriAddress(new BInternetAddress("localhost"));
               break;
            default:
               connection.setHubUriAddress(new BInternetAddress("localhost", webServicePort));
         }

         connection.setHubUriPath(this.getWebSocketAcceptor().getServletName());
         if (this.canAcceptConnections()) {
            if (connection.isConnected()) {
               connection.disconnect();
            }

            this.resetHubConnector();
         }
      }
   }

   private void resetHubConnector() {
      if (this.canAcceptConnections()) {
         try {
            this.scLinkLayer.getHubConnector().forceConnect();
         } catch (Exception var2) {
            logger.log(Level.FINE, "Exception forcing connection from local Hub Connector: " + var2.getLocalizedMessage(), (Throwable)var2);
         }
      }
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      if (Sys.isStation()) {
         out.startProps();
         out.trTitle("BACnet/SC Hub Function", 2);
         out.prop("Message ID Counter", this.messageId.get());
         out.prop("Active Connection Count", this.getActiveConnectionCount());
         out.endProps();
         super.spy(out);
      }
   }
}
