package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.connection.BAbstractConnection;
import com.tridium.bacnet.stack.link.sc.connection.BHubInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.IScConnectionInitiator;
import com.tridium.bacnet.stack.link.sc.message.AddressedMessage;
import com.tridium.bacnet.stack.link.sc.message.ScNpdu;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.stack.network.messages.NetworkNumberIs;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BIRestrictedComponent;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.down",
      flags = 67
   ), @NiagaraProperty(
      name = "state",
      type = "BScHubConnectorState",
      defaultValue = "BScHubConnectorState.noHubConnection",
      flags = 259
   ), @NiagaraProperty(
      name = "subState",
      type = "BHubConnectorSubState",
      defaultValue = "noHubConnection",
      flags = 259
   ), @NiagaraProperty(
      name = "health",
      type = "BHubConnectorHealth",
      defaultValue = "BHubConnectorHealth.make()"
   ), @NiagaraProperty(
      name = "primaryConnection",
      type = "BHubInitiatingConnection",
      defaultValue = "BHubInitiatingConnection.make(new BInternetAddress(\"primary.example.com\"), \"hub\", true)"
   ), @NiagaraProperty(
      name = "failoverConnection",
      type = "BHubInitiatingConnection",
      defaultValue = "BHubInitiatingConnection.make(new BInternetAddress(\"failover.example.com\"), \"hub\", false)"
   )})
@NiagaraActions({@NiagaraAction(
      name = "forceConnect",
      flags = 128
   ), @NiagaraAction(
      name = "forceDisconnect",
      flags = 128
   ), @NiagaraAction(
      name = "activatePrimary",
      flags = 20
   ), @NiagaraAction(
      name = "deactivatePrimary",
      flags = 20
   ), @NiagaraAction(
      name = "activateFailover",
      flags = 20
   ), @NiagaraAction(
      name = "deactivateFailover",
      flags = 20
   ), @NiagaraAction(
      name = "waitTimedOut",
      flags = 4
   )})
public final class BHubConnector extends BComponent implements IScConnectionInitiator, BIRestrictedComponent, BIStatus {
   public static final Property status = newProperty(67, BStatus.down, null);
   public static final Property state = newProperty(259, BScHubConnectorState.noHubConnection, null);
   public static final Property subState = newProperty(259, BHubConnectorSubState.noHubConnection, null);
   public static final Property health = newProperty(0, BHubConnectorHealth.make(), null);
   public static final Property primaryConnection = newProperty(
      0, BHubInitiatingConnection.make(new BInternetAddress("primary.example.com"), "hub", true), null
   );
   public static final Property failoverConnection = newProperty(
      0, BHubInitiatingConnection.make(new BInternetAddress("failover.example.com"), "hub", false), null
   );
   public static final Action forceConnect = newAction(128, null);
   public static final Action forceDisconnect = newAction(128, null);
   public static final Action activatePrimary = newAction(20, null);
   public static final Action deactivatePrimary = newAction(20, null);
   public static final Action activateFailover = newAction(20, null);
   public static final Action deactivateFailover = newAction(20, null);
   public static final Action waitTimedOut = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BHubConnector.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.hubConnector");
   private BScLinkLayer scLinkLayer;
   private Ticket waitTicket;
   private BRelTime reconnectTimeout;
   private final AtomicBoolean linkCommStarting = new AtomicBoolean();

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public BScHubConnectorState getState() {
      return (BScHubConnectorState)this.get(state);
   }

   public void setState(BScHubConnectorState v) {
      this.set(state, v, null);
   }

   public BHubConnectorSubState getSubState() {
      return (BHubConnectorSubState)this.get(subState);
   }

   public void setSubState(BHubConnectorSubState v) {
      this.set(subState, v, null);
   }

   public BHubConnectorHealth getHealth() {
      return (BHubConnectorHealth)this.get(health);
   }

   public void setHealth(BHubConnectorHealth v) {
      this.set(health, v, null);
   }

   public BHubInitiatingConnection getPrimaryConnection() {
      return (BHubInitiatingConnection)this.get(primaryConnection);
   }

   public void setPrimaryConnection(BHubInitiatingConnection v) {
      this.set(primaryConnection, v, null);
   }

   public BHubInitiatingConnection getFailoverConnection() {
      return (BHubInitiatingConnection)this.get(failoverConnection);
   }

   public void setFailoverConnection(BHubInitiatingConnection v) {
      this.set(failoverConnection, v, null);
   }

   public void forceConnect() {
      this.invoke(forceConnect, null, null);
   }

   public void forceDisconnect() {
      this.invoke(forceDisconnect, null, null);
   }

   public void activatePrimary() {
      this.invoke(activatePrimary, null, null);
   }

   public void deactivatePrimary() {
      this.invoke(deactivatePrimary, null, null);
   }

   public void activateFailover() {
      this.invoke(activateFailover, null, null);
   }

   public void deactivateFailover() {
      this.invoke(deactivateFailover, null, null);
   }

   public void waitTimedOut() {
      this.invoke(waitTimedOut, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void doForceConnect() {
      if (this.isRunning()) {
         this.scLinkLayer.checkNetworkPortIsEnabled();
         if (!this.scLinkLayer.isCommStarted()) {
            throw new LocalizableRuntimeException("bacnet", "hubConnector.forceConnect.scLinkLayerStopped");
         } else {
            synchronized (this) {
               this.cancelWaitTicket();
               if (this.getPrimaryConnection().getEnabled()) {
                  switch (this.getSubState().getOrdinal()) {
                     case 0:
                        this.changeState(BHubConnectorSubState.connectingToPrimary);
                        connect(this.getPrimaryConnection());
                        break;
                     case 4:
                        this.changeState(BHubConnectorSubState.reconnectingToPrimary);
                        connect(this.getPrimaryConnection());
                        break;
                     default:
                        throw new LocalizableRuntimeException(
                           "bacnet", "hubConnector.forceConnect.improperState", new String[]{this.getSubState().getDisplayTag(null)}
                        );
                  }
               } else {
                  if (!this.getFailoverConnection().getEnabled()) {
                     throw new LocalizableRuntimeException("bacnet", "hubConnector.forceConnect.bothConnectionsDisabled");
                  }

                  if (this.getSubState().getOrdinal() != 0) {
                     throw new LocalizableRuntimeException(
                        "bacnet", "hubConnector.forceConnect.improperState", new String[]{this.getSubState().getDisplayTag(null)}
                     );
                  }

                  this.changeState(BHubConnectorSubState.connectingToFailover);
                  connect(this.getFailoverConnection());
               }
            }
         }
      }
   }

   public void doForceDisconnect() {
      if (this.isRunning()) {
         synchronized (this) {
            if (this.getPrimaryConnection().isConnected()) {
               this.getPrimaryConnection().disconnect();
            }

            if (this.getFailoverConnection().isConnected()) {
               this.getFailoverConnection().disconnect();
            }
         }
      }
   }

   public synchronized void doActivatePrimary() {
      if (this.isRunning()) {
         int subState = this.getSubState().getOrdinal();
         switch (subState) {
            case 1:
            case 5:
               if (subState == 5) {
                  this.getFailoverConnection().disconnect();
               }

               this.getHealth().primaryConnectionActivated();
               this.resetReconnectTimeout();
               this.changeState(BHubConnectorSubState.connectedToPrimary);
               this.sendNetworkReadyMessages();
               break;
            default:
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Unexpected call to doActivatePrimary while in improper state " + this.getSubState().getTag() + "; disconnecting from primary");
               }

               this.getPrimaryConnection().disconnect();
         }
      }
   }

   public synchronized void doDeactivatePrimary() {
      if (this.isRunning()) {
         switch (this.getSubState().getOrdinal()) {
            case 1:
               this.getHealth().primaryConnectionFailed();
               this.incrementReconnectTimeout();
               if (this.getFailoverConnection().getEnabled()) {
                  this.changeState(BHubConnectorSubState.connectingToFailover);
                  connect(this.getFailoverConnection());
               } else {
                  this.changeState(BHubConnectorSubState.noHubConnection);
                  this.startTimer();
               }
               break;
            case 2:
               this.getHealth().primaryConnectionFailed();
               this.incrementReconnectTimeout();
               this.changeState(BHubConnectorSubState.noHubConnection);
               this.startTimer();
               break;
            case 3:
            case 4:
            default:
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Unexpected call to doDeactivatePrimary while in improper state " + this.getSubState().getTag());
               }
               break;
            case 5:
               this.getHealth().primaryConnectionFailed();
               this.incrementReconnectTimeout();
               this.changeState(BHubConnectorSubState.connectedToFailover);
               this.startTimer();
         }
      }
   }

   public synchronized void doActivateFailover() {
      if (this.isRunning()) {
         if (this.getSubState().getOrdinal() == 3) {
            this.getHealth().failoverConnectionActivated();
            this.changeState(BHubConnectorSubState.connectedToFailover);
            this.sendNetworkReadyMessages();
            if (this.getPrimaryConnection().getEnabled()) {
               this.startTimer();
            }
         } else {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Unexpected call to doActivateFailover while in improper state " + this.getSubState().getTag() + "; disconnecting from failover");
            }

            this.getFailoverConnection().disconnect();
         }
      }
   }

   public synchronized void doDeactivateFailover() {
      if (this.isRunning()) {
         switch (this.getSubState().getOrdinal()) {
            case 3:
            case 4:
               this.getHealth().failoverConnectionFailed();
               this.changeState(BHubConnectorSubState.noHubConnection);
               this.startTimer();
               break;
            case 5:
               this.getHealth().failoverConnectionFailed();
               this.changeState(BHubConnectorSubState.connectingToPrimary);
               break;
            default:
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Unexpected call to doDeactivateFailover while in improper state " + this.getSubState().getTag());
               }
         }
      }
   }

   public synchronized void doWaitTimedOut() {
      if (this.isRunning()) {
         this.cancelWaitTicket();
         if (!this.scLinkLayer.isCommStarted()) {
            logger.fine("Wait timeout expiration ignored because the HubConnector's ScLinkLayer is not started");
         } else {
            switch (this.getSubState().getOrdinal()) {
               case 0:
                  if (this.getPrimaryConnection().getEnabled()) {
                     this.changeState(BHubConnectorSubState.connectingToPrimary);
                     connect(this.getPrimaryConnection());
                  } else {
                     this.getHealth().primaryConnectionDisabled();
                     if (this.getFailoverConnection().getEnabled()) {
                        this.changeState(BHubConnectorSubState.connectingToFailover);
                        this.incrementReconnectTimeout();
                        connect(this.getFailoverConnection());
                     }
                  }
                  break;
               case 4:
                  if (this.getPrimaryConnection().getEnabled()) {
                     this.changeState(BHubConnectorSubState.reconnectingToPrimary);
                     connect(this.getPrimaryConnection());
                  } else {
                     this.getHealth().primaryConnectionDisabled();
                  }
                  break;
               default:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("Wait timeout expired while in improper state " + this.getSubState().getTag());
                  }
            }
         }
      }
   }

   public void sendMessage(long destinationVmac, AddressedMessage message) throws BacnetException {
      BHubInitiatingConnection connection = this.getActiveConnection();
      if (connection == null) {
         if (!this.linkCommStarting.get()) {
            throw new NoConnectionException(
               "Hub Connector under Network Port " + this.scLinkLayer.getParent().getName() + " is not connected to a primary or failover hub."
            );
         }
      } else {
         VmacUtil.checkIsDestinationVmac(destinationVmac);
         Objects.requireNonNull(message, "message parameter");
         message.setDestinationVmac(destinationVmac);
         message.clearOriginatingVmac();
         connection.sendMessage(message);
      }
   }

   public void started() throws Exception {
      super.started();
      this.scLinkLayer = (BScLinkLayer)this.getParent();
   }

   @Override
   public String getSubProtocol() {
      return "hub.bsc.bacnet.org";
   }

   @Override
   public void initiatedConnectionFailed(BInitiatingConnection connection) {
      if (this.isPrimary(connection)) {
         this.deactivatePrimary();
      } else if (this.isFailover(connection)) {
         this.deactivateFailover();
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine("Unexpected call to initiatedConnectionFailed with " + this.toString(connection) + " connection in state " + this.getSubState().getTag());
      }
   }

   @Override
   public void activateConnection(BAbstractConnection connection) {
      if (this.isPrimary(connection)) {
         this.activatePrimary();
      } else if (this.isFailover(connection)) {
         this.activateFailover();
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine("Unexpected call to activateConnection with " + this.toString(connection) + " connection in state " + this.getSubState().getTag());
      }
   }

   @Override
   public void deactivateConnection(BAbstractConnection connection) {
      if (this.isPrimary(connection)) {
         this.deactivatePrimary();
      } else if (this.isFailover(connection)) {
         this.deactivateFailover();
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine("Unexpected call to deactivateConnection with " + this.toString(connection) + " connection in state " + this.getSubState().getTag());
      }
   }

   @Override
   public void forwardNpdu(long originatingVmac, long destinationVmac, ScNpdu npdu) throws Exception {
      this.scLinkLayer.rcvIndication(originatingVmac, destinationVmac, npdu);
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      ScLinkLayerUtil.checkParentType(TYPE, parent.getType(), BScLinkLayer.TYPE);
      ScLinkLayerUtil.checkForDuplicate(this, parent);
   }

   public synchronized void linkCommStart() {
      this.getPrimaryConnection().kill();
      this.getFailoverConnection().kill();
      this.changeState(BHubConnectorSubState.noHubConnection);
      this.resetReconnectTimeout();
      this.linkCommStarting.set(true);
      this.doWaitTimedOut();
   }

   public synchronized void linkCommStop() {
      this.linkCommStarting.set(false);
      this.cancelWaitTicket();
      this.resetReconnectTimeout();
      this.getPrimaryConnection().disconnect();
      this.getFailoverConnection().disconnect();
      this.changeState(BHubConnectorSubState.noHubConnection);
   }

   private static void connect(BHubInitiatingConnection connection) {
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         connection.connect();
         return null;
      }));
   }

   private BHubInitiatingConnection getActiveConnection() {
      switch (this.getSubState().getOrdinal()) {
         case 2:
            return this.getPrimaryConnection();
         case 3:
         default:
            return null;
         case 4:
         case 5:
            return this.getFailoverConnection();
      }
   }

   private void changeState(BHubConnectorSubState subState) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("HubConnector transitioned from " + this.getSubState().getTag() + " to " + subState.getTag());
      }

      this.setSubState(subState);
      switch (subState.getOrdinal()) {
         case 0:
            this.setStatus(BStatus.make(this.getStatus(), 4, true));
            this.setState(BScHubConnectorState.noHubConnection);
            this.linkCommStarting.set(false);
            break;
         case 1:
         case 3:
            this.setState(BScHubConnectorState.noHubConnection);
            break;
         case 2:
            this.setStatus(BStatus.make(this.getStatus(), 4, false));
            this.setState(BScHubConnectorState.connectedToPrimary);
            break;
         case 4:
            this.setStatus(BStatus.make(this.getStatus(), 4, false));
            this.setState(BScHubConnectorState.connectedToFailover);
            break;
         case 5:
            this.setState(BScHubConnectorState.connectedToFailover);
            break;
         default:
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Unexpected call to changeState with subState value " + subState.getTag());
            }
      }
   }

   private void resetReconnectTimeout() {
      this.reconnectTimeout = BRelTime.DEFAULT;
   }

   private void incrementReconnectTimeout() {
      if (this.reconnectTimeout == null) {
         this.resetReconnectTimeout();
      }

      long minMillis = this.scLinkLayer.getConfig().getMinimumReconnectTime().getMillis();
      long millis = Math.max(this.reconnectTimeout.getMillis() * 2L, minMillis);
      long maxMillis = this.scLinkLayer.getConfig().getMaximumReconnectTime().getMillis();
      millis = Math.min(millis, maxMillis);
      this.reconnectTimeout = BRelTime.make(millis);
   }

   private void startTimer() {
      if (this.waitTicket == null) {
         if (this.reconnectTimeout == null) {
            this.incrementReconnectTimeout();
         }

         this.waitTicket = Clock.schedule(this, this.reconnectTimeout.getSeconds() > 0 ? this.reconnectTimeout : BRelTime.SECOND, waitTimedOut, null);
      }
   }

   private void cancelWaitTicket() {
      if (this.waitTicket != null) {
         this.waitTicket.cancel();
      }

      this.waitTicket = null;
   }

   public boolean isPrimary(BAbstractConnection connection) {
      return this.getPrimaryConnection() == connection;
   }

   private boolean isFailover(BAbstractConnection connection) {
      return this.getFailoverConnection() == connection;
   }

   public String toString(Context cx) {
      return this.getState().getDisplayTag(cx) + ' ' + this.getStatus().toString(cx);
   }

   private String toString(BAbstractConnection connection) {
      return this.isPrimary(connection) ? "primary" : (this.isFailover(connection) ? "failover" : "unknown");
   }

   private void sendNetworkReadyMessages() {
      if (this.linkCommStarting.compareAndSet(true, false)) {
         BNetworkPort networkPort = (BNetworkPort)this.scLinkLayer.getParent();
         BBacnetNetworkLayer networkLayer = (BBacnetNetworkLayer)networkPort.getParent();
         networkLayer.issueIAmRouterToNetworks();
         networkLayer.issueWhoIsRouterToNetwork(-1);
         networkPort.sendToLink(null, new NetworkNumberIs(networkPort.getNetworkNumber()));
         BBacnetNetwork.localDevice().sendIAm();
      }
   }
}
