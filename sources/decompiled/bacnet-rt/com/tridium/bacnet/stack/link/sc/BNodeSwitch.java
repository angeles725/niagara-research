package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.connection.BAbstractConnection;
import com.tridium.bacnet.stack.link.sc.connection.BAbstractScWebSocketAcceptor;
import com.tridium.bacnet.stack.link.sc.connection.BAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BDirectAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BDirectInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.IScConnectionInitiator;
import com.tridium.bacnet.stack.link.sc.connection.jetty.BJettyScWebSocketAcceptor;
import com.tridium.bacnet.stack.link.sc.message.AddressResolutionAck;
import com.tridium.bacnet.stack.link.sc.message.AddressedMessage;
import com.tridium.bacnet.stack.link.sc.message.Advertisement;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcResult;
import com.tridium.bacnet.stack.link.sc.message.ScNpdu;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BIRestrictedComponent;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "acceptEnabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "initiateEnabled",
      type = "boolean",
      defaultValue = "false"
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
      type = "BNodeSwitchConnections",
      defaultValue = "new BNodeSwitchConnections()"
   )})
public final class BNodeSwitch extends BAbstractConnectionManager implements IScConnectionInitiator, BIRestrictedComponent {
   public static final String DEFAULT_SERVLET_NAME = "nodeSwitch";
   public static final Property acceptEnabled = newProperty(0, false, null);
   public static final Property initiateEnabled = newProperty(0, false, null);
   public static final Property webSocketAcceptor = newProperty(0, BJettyScWebSocketAcceptor.make("nodeSwitch"), null);
   public static final Property acceptUris = newProperty(0, new BBacnetArray(BString.TYPE), null);
   public static final Property connections = newProperty(0, new BNodeSwitchConnections(), null);
   public static final Type TYPE = Sys.loadType(BNodeSwitch.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.nodeSwitch");

   public boolean getAcceptEnabled() {
      return this.getBoolean(acceptEnabled);
   }

   public void setAcceptEnabled(boolean v) {
      this.setBoolean(acceptEnabled, v, null);
   }

   public boolean getInitiateEnabled() {
      return this.getBoolean(initiateEnabled);
   }

   public void setInitiateEnabled(boolean v) {
      this.setBoolean(initiateEnabled, v, null);
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

   public BNodeSwitchConnections getConnections() {
      return (BNodeSwitchConnections)this.get(connections);
   }

   public void setConnections(BNodeSwitchConnections v) {
      this.set(connections, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean acceptEnabled() {
      return this.getAcceptEnabled();
   }

   @Override
   public boolean initiateEnabled() {
      return this.getInitiateEnabled();
   }

   public void sendMessage(long destinationVmac, AddressedMessage message) throws BacnetException {
      VmacUtil.checkIsDestinationVmac(destinationVmac);
      Objects.requireNonNull(message, "message parameter");
      if (destinationVmac != 281474976710655L) {
         BAbstractConnection connection = this.getActiveConnection(destinationVmac);
         if (connection != null) {
            try {
               message.clearDestinationVmac();
               message.clearOriginatingVmac();
               connection.sendMessage(message);
               return;
            } catch (Exception var6) {
               logger.log(
                  Level.FINE,
                  "Exception while sending an address resolution message over a direct connection; destination VMAC: ["
                     + VmacUtil.vmacToString(destinationVmac)
                     + ']',
                  (Throwable)var6
               );
            }
         }
      }

      this.scLinkLayer.getHubConnector().sendMessage(destinationVmac, message);
   }

   public void checkInitiatesDirectConnections() {
      if (!this.initiateEnabled()) {
         throw new LocalizableRuntimeException("bacnet", "nodeSwitch.disabled.connectionsNotSupported");
      } else if (this.isConnectionLimitReached()) {
         throw new LocalizableRuntimeException("bacnet", "nodeSwitch.connectionLimitReached");
      }
   }

   public void handleAddressResolutionAck(long originatingVmac, AddressResolutionAck addressResolutionAck) {
      BDirectInitiatingConnection connection = this.getInitiatingConnection(originatingVmac);
      if (connection != null) {
         connection.handleAddressResolutionAck(addressResolutionAck);
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine("Did not find direct initiating connection for Address-Resolution-ACK to vmac [" + VmacUtil.vmacToString(originatingVmac) + ']');
      }
   }

   public void handleAddressResolutionNak(long originatingVmac, ScBvlcResult bvlcResult) {
      BDirectInitiatingConnection connection = this.getInitiatingConnection(originatingVmac);
      if (connection != null) {
         connection.handleAddressResolutionNak(bvlcResult);
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "Did not find direct initiating connection for Address-Resolution BVLC-Result-NAK to vmac [" + VmacUtil.vmacToString(originatingVmac) + ']'
         );
      }
   }

   public void handleAdvertisement(long originatingVmac, Advertisement advertisement) {
      BDirectInitiatingConnection connection = this.getInitiatingConnection(originatingVmac);
      if (connection != null) {
         connection.handleAdvertisement(advertisement);
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine("Did not find direct initiating connection for Advertisement to vmac [" + VmacUtil.vmacToString(originatingVmac) + ']');
      }
   }

   public void handleAdvertisementNak(long originatingVmac, ScBvlcResult bvlcResult) {
      BDirectInitiatingConnection connection = this.getInitiatingConnection(originatingVmac);
      if (connection != null) {
         connection.handleAdvertisementNak(bvlcResult);
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine("Did not find direct initiating connection for Advertisement BVLC-Result-NAK to vmac [" + VmacUtil.vmacToString(originatingVmac) + ']');
      }
   }

   private BDirectInitiatingConnection getInitiatingConnection(long peerVmac) {
      for (BDirectInitiatingConnection connection : (BDirectInitiatingConnection[])this.getConnections().getChildren(BDirectInitiatingConnection.class)) {
         long connectionPeerVmac = VmacUtil.bytesToVmac(connection.getPeerVmac().getBytes());
         if (connectionPeerVmac == peerVmac) {
            return connection;
         }
      }

      return null;
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(acceptEnabled)) {
            if (this.getAcceptEnabled()) {
               this.updateStatus();
               this.getWebSocketAcceptor().linkCommStart();
            } else {
               this.updateStatus();
               this.disconnectConnections(true);
               this.getWebSocketAcceptor().linkCommStop();
            }
         } else if (property.equals(initiateEnabled)) {
            this.updateStatus();
            if (!this.getInitiateEnabled()) {
               this.disconnectConnections(false);
            }
         }
      }
   }

   public String toString(Context cx) {
      boolean acceptEnabled = this.getAcceptEnabled();
      boolean initiateEnabled = this.getInitiateEnabled();
      StringBuilder builder;
      if (acceptEnabled && initiateEnabled) {
         builder = new StringBuilder(ScLinkLayerUtil.LEXICON.getText("nodeSwitch.bothEnabled", cx));
      } else {
         if (!acceptEnabled) {
            if (initiateEnabled) {
               return ScLinkLayerUtil.LEXICON.getText("nodeSwitch.initiateEnabled", cx);
            }

            return ScLinkLayerUtil.LEXICON.getText("nodeSwitch.disabled", cx);
         }

         builder = new StringBuilder(ScLinkLayerUtil.LEXICON.getText("nodeSwitch.acceptEnabled", cx));
      }

      if (this.getStatus().isFault()) {
         builder.append(", ").append(ScLinkLayerUtil.LEXICON.getText("faulted", cx));
      }

      return builder.toString();
   }

   @Override
   public String getSubProtocol() {
      return "dc.bsc.bacnet.org";
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
      BDirectAcceptingConnection acceptingConnection = new BDirectAcceptingConnection();
      this.getConnections().add("AcceptedConnection1?", acceptingConnection, 2);
      return acceptingConnection;
   }

   @Override
   public int getMaxBvlcLength() {
      return this.scLinkLayer.getMaxBvlcLength();
   }

   @Override
   protected void checkConnectionType(BAbstractConnection connection) {
      if (!(connection instanceof BDirectAcceptingConnection) && !(connection instanceof BDirectInitiatingConnection)) {
         throw new IllegalArgumentException("connection must be a BDirectAcceptingConnection or BDirectInitiatingConnection");
      }
   }

   @Override
   protected String getConnectionLimitKey() {
      return "nodeSwitchConnections.limit";
   }

   @Override
   public void forwardNpdu(long originatingVmac, long destinationVmac, ScNpdu npdu) throws Exception {
      this.scLinkLayer.rcvIndication(originatingVmac, destinationVmac, npdu);
   }

   @Override
   public void initiatedConnectionFailed(BInitiatingConnection connection) {
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      ScLinkLayerUtil.checkParentType(TYPE, parent.getType(), BScLinkLayer.TYPE);
      ScLinkLayerUtil.checkForDuplicate(this, parent);
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      if (Sys.isStation()) {
         out.startProps();
         out.trTitle("BACnet/SC Node Switch", 2);
         out.prop("Active Connection Count", this.getActiveConnectionCount());
         out.endProps();
         super.spy(out);
      }
   }
}
