package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.BNodeSwitch;
import com.tridium.bacnet.stack.link.sc.BNodeSwitchConnections;
import com.tridium.bacnet.stack.link.sc.NoConnectionException;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.VmacUtil;
import com.tridium.bacnet.stack.link.sc.message.AddressResolution;
import com.tridium.bacnet.stack.link.sc.message.AddressResolutionAck;
import com.tridium.bacnet.stack.link.sc.message.Advertisement;
import com.tridium.bacnet.stack.link.sc.message.AdvertisementSolicitation;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcResult;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BErrorType;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.IPropertyValidator;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Validatable;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BUuid;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "state",
      type = "BBacnetScConnectionState",
      defaultValue = "BBacnetScConnectionState.notConnected",
      flags = 259
   ), @NiagaraProperty(
      name = "subState",
      type = "BInitiatingConnectionState",
      defaultValue = "BInitiatingConnectionState.DEFAULT",
      flags = 259
   ), @NiagaraProperty(
      name = "peerUriAddress",
      type = "BInternetAddress",
      defaultValue = "new BInternetAddress(\"example.com\")"
   ), @NiagaraProperty(
      name = "peerUriPath",
      type = "String",
      defaultValue = "BNodeSwitch.DEFAULT_SERVLET_NAME"
   ), @NiagaraProperty(
      name = "peerUriQuery",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "peerAddress",
      type = "BInternetAddress",
      defaultValue = "new BInternetAddress(\"null\")",
      flags = 3
   ), @NiagaraProperty(
      name = "peerVmac",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "peerUuid",
      type = "BUuid",
      defaultValue = "BUuid.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "peerMaxBvlcLength",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "peerMaxNpduLength",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "peerInfo",
      type = "BScNodeInfo",
      defaultValue = "new BScNodeInfo()"
   ), @NiagaraProperty(
      name = "lastConnect",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "lastDisconnect",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "lastFailureToConnect",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "error",
      type = "BErrorType",
      defaultValue = "new BErrorType(-1, -1)",
      flags = 1
   ), @NiagaraProperty(
      name = "errorDetails",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
@NiagaraActions({@NiagaraAction(
      name = "initiateConnection"
   ), @NiagaraAction(
      name = "forceDisconnect"
   ), @NiagaraAction(
      name = "changePeerVmac",
      parameterType = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.make(new byte[VMAC_LENGTH])"
   ), @NiagaraAction(
      name = "sendAddressResolution"
   ), @NiagaraAction(
      name = "sendAdvertisementSolicitation"
   ), @NiagaraAction(
      name = "addressResolutionTimedOut",
      flags = 4
   ), @NiagaraAction(
      name = "advertisementSolicitationTimedOut",
      flags = 4
   )})
public final class BDirectInitiatingConnection extends BInitiatingConnection implements IPropertyValidator {
   public static final Property state = newProperty(259, BBacnetScConnectionState.notConnected, null);
   public static final Property subState = newProperty(259, BInitiatingConnectionState.DEFAULT, null);
   public static final Property peerUriAddress = newProperty(0, new BInternetAddress("example.com"), null);
   public static final Property peerUriPath = newProperty(0, "nodeSwitch", null);
   public static final Property peerUriQuery = newProperty(0, "", null);
   public static final Property peerAddress = newProperty(3, new BInternetAddress("null"), null);
   public static final Property peerVmac = newProperty(1, BBacnetOctetString.DEFAULT, null);
   public static final Property peerUuid = newProperty(1, BUuid.DEFAULT, null);
   public static final Property peerMaxBvlcLength = newProperty(3, 0, null);
   public static final Property peerMaxNpduLength = newProperty(3, 0, null);
   public static final Property peerInfo = newProperty(0, new BScNodeInfo(), null);
   public static final Property lastConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastDisconnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastFailureToConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property error = newProperty(1, new BErrorType(-1, -1), null);
   public static final Property errorDetails = newProperty(1, "", null);
   public static final Action initiateConnection = newAction(0, null);
   public static final Action forceDisconnect = newAction(0, null);
   public static final Action changePeerVmac = newAction(0, BBacnetOctetString.make(new byte[6]), null);
   public static final Action sendAddressResolution = newAction(0, null);
   public static final Action sendAdvertisementSolicitation = newAction(0, null);
   public static final Action addressResolutionTimedOut = newAction(4, null);
   public static final Action advertisementSolicitationTimedOut = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BDirectInitiatingConnection.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.nodeSwitch");
   private BNodeSwitch nodeSwitch;
   private int addressResolutionId = -1;
   private Ticket addressResolutionTicket;
   private int advertisementSolicitationId = -1;
   private Ticket advertisementSolicitationTicket;

   @Override
   public BBacnetScConnectionState getState() {
      return (BBacnetScConnectionState)this.get(state);
   }

   @Override
   public void setState(BBacnetScConnectionState v) {
      this.set(state, v, null);
   }

   @Override
   public BInitiatingConnectionState getSubState() {
      return (BInitiatingConnectionState)this.get(subState);
   }

   @Override
   public void setSubState(BInitiatingConnectionState v) {
      this.set(subState, v, null);
   }

   public BInternetAddress getPeerUriAddress() {
      return (BInternetAddress)this.get(peerUriAddress);
   }

   public void setPeerUriAddress(BInternetAddress v) {
      this.set(peerUriAddress, v, null);
   }

   public String getPeerUriPath() {
      return this.getString(peerUriPath);
   }

   public void setPeerUriPath(String v) {
      this.setString(peerUriPath, v, null);
   }

   public String getPeerUriQuery() {
      return this.getString(peerUriQuery);
   }

   public void setPeerUriQuery(String v) {
      this.setString(peerUriQuery, v, null);
   }

   public BInternetAddress getPeerAddress() {
      return (BInternetAddress)this.get(peerAddress);
   }

   public void setPeerAddress(BInternetAddress v) {
      this.set(peerAddress, v, null);
   }

   public BBacnetOctetString getPeerVmac() {
      return (BBacnetOctetString)this.get(peerVmac);
   }

   public void setPeerVmac(BBacnetOctetString v) {
      this.set(peerVmac, v, null);
   }

   public BUuid getPeerUuid() {
      return (BUuid)this.get(peerUuid);
   }

   public void setPeerUuid(BUuid v) {
      this.set(peerUuid, v, null);
   }

   public int getPeerMaxBvlcLength() {
      return this.getInt(peerMaxBvlcLength);
   }

   public void setPeerMaxBvlcLength(int v) {
      this.setInt(peerMaxBvlcLength, v, null);
   }

   public int getPeerMaxNpduLength() {
      return this.getInt(peerMaxNpduLength);
   }

   public void setPeerMaxNpduLength(int v) {
      this.setInt(peerMaxNpduLength, v, null);
   }

   public BScNodeInfo getPeerInfo() {
      return (BScNodeInfo)this.get(peerInfo);
   }

   public void setPeerInfo(BScNodeInfo v) {
      this.set(peerInfo, v, null);
   }

   @Override
   public BAbsTime getLastConnect() {
      return (BAbsTime)this.get(lastConnect);
   }

   @Override
   public void setLastConnect(BAbsTime v) {
      this.set(lastConnect, v, null);
   }

   @Override
   public BAbsTime getLastDisconnect() {
      return (BAbsTime)this.get(lastDisconnect);
   }

   @Override
   public void setLastDisconnect(BAbsTime v) {
      this.set(lastDisconnect, v, null);
   }

   @Override
   public BAbsTime getLastFailureToConnect() {
      return (BAbsTime)this.get(lastFailureToConnect);
   }

   @Override
   public void setLastFailureToConnect(BAbsTime v) {
      this.set(lastFailureToConnect, v, null);
   }

   @Override
   public BErrorType getError() {
      return (BErrorType)this.get(error);
   }

   @Override
   public void setError(BErrorType v) {
      this.set(error, v, null);
   }

   @Override
   public String getErrorDetails() {
      return this.getString(errorDetails);
   }

   @Override
   public void setErrorDetails(String v) {
      this.setString(errorDetails, v, null);
   }

   public void initiateConnection() {
      this.invoke(initiateConnection, null, null);
   }

   public void forceDisconnect() {
      this.invoke(forceDisconnect, null, null);
   }

   public void changePeerVmac(BBacnetOctetString parameter) {
      this.invoke(changePeerVmac, parameter, null);
   }

   public void sendAddressResolution() {
      this.invoke(sendAddressResolution, null, null);
   }

   public void sendAdvertisementSolicitation() {
      this.invoke(sendAdvertisementSolicitation, null, null);
   }

   public void addressResolutionTimedOut() {
      this.invoke(addressResolutionTimedOut, null, null);
   }

   public void advertisementSolicitationTimedOut() {
      this.invoke(advertisementSolicitationTimedOut, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BDirectInitiatingConnection make(BInternetAddress address, String path, BBacnetOctetString vmac) {
      BDirectInitiatingConnection connection = new BDirectInitiatingConnection();
      connection.setPeerUriAddress(address);
      connection.setPeerUriPath(path);
      if (VmacUtil.isDeviceVmac(VmacUtil.octetStringToVmac(vmac))) {
         connection.setPeerVmac(vmac);
      }

      return connection;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BNodeSwitchConnections;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.nodeSwitch = (BNodeSwitch)this.getParent().getParent();
   }

   public void doInitiateConnection() {
      this.scLinkLayer.checkNetworkPortIsEnabled();
      this.nodeSwitch.checkInitiatesDirectConnections();
      BInitiatingConnectionState subState = this.getSubState();
      if (!subState.equals(BInitiatingConnectionState.idle)) {
         throw new LocalizableRuntimeException("bacnet", "directInitiatingConnection.connect.improperState", new Object[]{subState.getDisplayTag(null)});
      } else {
         AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
            this.connect();
            return null;
         }));
      }
   }

   public void doChangePeerVmac(BBacnetOctetString peerVmac) {
      BInitiatingConnectionState subState = this.getSubState();
      if (!subState.equals(BInitiatingConnectionState.idle)) {
         throw new LocalizableRuntimeException("bacnet", "directInitiatingConnection.changePeerVmac.improperState", new Object[]{subState.getDisplayTag(null)});
      } else if (!VmacUtil.isDeviceVmac(VmacUtil.octetStringToVmac(peerVmac))) {
         throw new LocalizableRuntimeException("bacnet", "changeVmacAddress.invalidVmac");
      } else {
         this.setPeerVmac(peerVmac);
      }
   }

   public void doSendAddressResolution() {
      this.cancelAddressResolutionTimer();
      long destinationVmac = VmacUtil.bytesToVmac(this.getPeerVmac().getBytes());
      if (!VmacUtil.isDeviceVmac(destinationVmac)) {
         throw new LocalizableRuntimeException("bacnet", "directInitiatingConnection.sendInfoMessage.invalidVmac", new Object[]{this.getPeerVmac()});
      } else {
         try {
            int messageId = this.getNextMessageId();
            AddressResolution message = AddressResolution.make(messageId);
            this.nodeSwitch.sendMessage(destinationVmac, message);
            this.getPeerInfo().setAddressResolutionStatus(ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.inProgress"));
            this.addressResolutionId = messageId;
            this.startAddressResolutionTimer();
         } catch (NoConnectionException var5) {
            throw new LocalizableRuntimeException(
               "bacnet", "directInitiatingConnection.sendInfoMessage.noConnection", new Object[]{VmacUtil.vmacToString(destinationVmac)}, var5
            );
         } catch (Exception var6) {
            throw new LocalizableRuntimeException(
               "bacnet", "directInitiatingConnection.sendInfoMessage.failed", new Object[]{VmacUtil.vmacToString(destinationVmac)}, var6
            );
         }
      }
   }

   public synchronized void doAddressResolutionTimedOut() {
      if (this.addressResolutionTicket != null) {
         this.cancelAddressResolutionTimer();
         this.getPeerInfo().setAddressResolutionStatus(ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.timedOut"));
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger().fine(this.getLogInfo().append(": Address resolution timed-out").toString());
         }
      }
   }

   public void doSendAdvertisementSolicitation() {
      this.cancelAdvertisementSolicitationTimer();
      long destinationVmac = VmacUtil.bytesToVmac(this.getPeerVmac().getBytes());
      if (!VmacUtil.isDeviceVmac(destinationVmac)) {
         throw new LocalizableRuntimeException("bacnet", "directInitiatingConnection.sendInfoMessage.invalidVmac", new Object[]{this.getPeerVmac()});
      } else {
         try {
            int messageId = this.getNextMessageId();
            AdvertisementSolicitation message = AdvertisementSolicitation.make(messageId);
            this.nodeSwitch.sendMessage(destinationVmac, message);
            this.getPeerInfo()
               .setAdvertisementSolicitationStatus(ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.inProgress"));
            this.advertisementSolicitationId = messageId;
            this.startAdvertisementSolicitationTimer();
         } catch (NoConnectionException var5) {
            throw new LocalizableRuntimeException(
               "bacnet", "directInitiatingConnection.sendInfoMessage.noConnection", new Object[]{VmacUtil.vmacToString(destinationVmac)}, var5
            );
         } catch (Exception var6) {
            throw new LocalizableRuntimeException(
               "bacnet", "directInitiatingConnection.sendInfoMessage.failed", new Object[]{VmacUtil.vmacToString(destinationVmac)}, var6
            );
         }
      }
   }

   public synchronized void doAdvertisementSolicitationTimedOut() {
      if (this.advertisementSolicitationTicket != null) {
         this.cancelAdvertisementSolicitationTimer();
         this.getPeerInfo().setAdvertisementSolicitationStatus(ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.timedOut"));
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger().fine(this.getLogInfo().append(": Advertisement Solicitation timed-out").toString());
         }
      }
   }

   public synchronized void handleAddressResolutionAck(AddressResolutionAck addressResolutionAck) {
      if (addressResolutionAck.getMessageId() != this.addressResolutionId) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Ignoring Address-Resolution-ACK that has message ID ")
                     .append(addressResolutionAck.getMessageId())
                     .append(" instead of expected ID ")
                     .append(this.addressResolutionId)
                     .append("; message: ")
                     .append(addressResolutionAck)
                     .toString()
               );
         }
      } else {
         this.cancelAddressResolutionTimer();
         BScNodeInfo peerInfo = this.getPeerInfo();
         peerInfo.setDirectUris(String.join("\n", addressResolutionAck.getWebSocketUris()));
         peerInfo.setAcceptsDirectConnections(true);
         peerInfo.setAddressResolutionStatus(ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.success"));
      }
   }

   public synchronized void handleAddressResolutionNak(ScBvlcResult bvlcResult) {
      if (bvlcResult.getMessageId() != this.addressResolutionId) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Ignoring Address-Resolution BVLC-Result-NAK that has message ID ")
                     .append(bvlcResult.getMessageId())
                     .append(" instead of expected ID ")
                     .append(this.addressResolutionId)
                     .append("; message: ")
                     .append(bvlcResult)
                     .toString()
               );
         }
      } else {
         this.cancelAddressResolutionTimer();
         BScNodeInfo peerInfo = this.getPeerInfo();
         int errorCode = bvlcResult.getErrorCode();
         if (errorCode == 45) {
            peerInfo.setAcceptsDirectConnections(false);
            peerInfo.setAddressResolutionStatus(ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.doesNotAcceptDirect"));
         } else {
            String tag = BBacnetErrorCode.tag(errorCode);
            peerInfo.setAddressResolutionStatus(
               ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.failed", errorCode, ScLinkLayerUtil.getLexiconText(tag))
            );
         }
      }
   }

   public synchronized void handleAdvertisement(Advertisement advertisement) {
      this.cancelAdvertisementSolicitationTimer();
      BScNodeInfo peerInfo = this.getPeerInfo();
      peerInfo.setHubConnectorStatus(advertisement.getHubConnectionStatus());
      peerInfo.setAcceptsDirectConnections(advertisement.acceptsDirectConnections());
      peerInfo.setMaxBvlcLength(advertisement.getMaxBvlcLength());
      peerInfo.setMaxNpduLength(advertisement.getMaxNpduLength());
      peerInfo.setAdvertisementSolicitationStatus(ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.success"));
   }

   public synchronized void handleAdvertisementNak(ScBvlcResult bvlcResult) {
      if (bvlcResult.getMessageId() != this.advertisementSolicitationId) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Ignoring Advertisement BVLC-Result-NAK that has message ID ")
                     .append(bvlcResult.getMessageId())
                     .append(" instead of expected ID ")
                     .append(this.advertisementSolicitationId)
                     .append("; message: ")
                     .append(bvlcResult)
                     .toString()
               );
         }
      } else {
         this.cancelAdvertisementSolicitationTimer();
         int errorCode = bvlcResult.getErrorCode();
         String tag = BBacnetErrorCode.tag(errorCode);
         this.getPeerInfo()
            .setAdvertisementSolicitationStatus(
               ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.failed", errorCode, ScLinkLayerUtil.getLexiconText(tag))
            );
      }
   }

   @Override
   protected void setRemoteAddress(BInternetAddress address) {
      this.setPeerAddress(address);
   }

   @Override
   protected void setRemoteVmac(long vmac) {
      this.setPeerVmac(VmacUtil.vmacToOctetString(vmac));
   }

   @Override
   protected void setRemoteUuid(UUID uuid) {
      this.setPeerUuid(BUuid.make(uuid));
   }

   @Override
   protected void setRemoteMaxBvlcLength(int maxBvlcLength) {
      this.setPeerMaxBvlcLength(maxBvlcLength);
   }

   @Override
   protected void setRemoteMaxNpduLength(int maxNpduLength) {
      this.setPeerMaxNpduLength(maxNpduLength);
   }

   @Override
   public URI getURI() throws URISyntaxException {
      return makeURI(this.getPeerUriAddress(), this.getPeerUriPath().trim(), this.getPeerUriQuery().trim());
   }

   @Override
   protected IScConnectionInitiator getConnectionInitiator() {
      return this.nodeSwitch;
   }

   @Override
   public String getLocalConnectionToken() {
      return null;
   }

   @Override
   public void clearLocalConnectionToken() {
   }

   public String toString(Context cx) {
      return this.getState().getDisplayTag(cx) + " [" + this.getPeerVmac().toString(cx) + ']';
   }

   public IPropertyValidator getPropertyValidator(Property property, Context context) {
      return (IPropertyValidator)(property.equals(peerUriQuery) ? this : super.getPropertyValidator(property, context));
   }

   public IPropertyValidator getPropertyValidator(Property[] properties, Context context) {
      for (Property property : properties) {
         if (property.equals(peerUriQuery)) {
            return this;
         }
      }

      return super.getPropertyValidator(properties, context);
   }

   public void validateSet(Validatable validatable, Context context) {
      for (Property property : validatable.getModifiedProperties()) {
         if (property.equals(peerUriQuery)) {
            checkUriQuery(((BString)validatable.getProposedValue(property)).getString());
         }
      }
   }

   public void validateSet(BComplex instance, Property property, BValue newValue, Context context) {
      if (property.equals(peerUriQuery)) {
         checkUriQuery(((BString)newValue).getString());
      }
   }

   @Override
   public Logger getLogger() {
      return logger;
   }

   @Override
   public StringBuilder getLogInfo() {
      long remoteVmac = this.getRemoteVmac();
      UUID remoteUuid = this.getRemoteUuid();
      return remoteVmac != -1L && remoteUuid != null
         ? new StringBuilder("Direct initiating connection, VMAC ").append(VmacUtil.vmacToString(remoteVmac)).append(", UUID ").append(remoteUuid)
         : new StringBuilder("Direct initiating connection");
   }

   private void startAddressResolutionTimer() {
      if (this.isRunning()) {
         this.addressResolutionTicket = Clock.schedule(this, this.getPeerInfo().getAddressResolutionTimeout(), addressResolutionTimedOut, null);
      }
   }

   private void cancelAddressResolutionTimer() {
      Ticket ticket = this.addressResolutionTicket;
      if (ticket != null) {
         ticket.cancel();
      }

      this.addressResolutionTicket = null;
   }

   private void startAdvertisementSolicitationTimer() {
      if (this.isRunning()) {
         this.advertisementSolicitationTicket = Clock.schedule(
            this, this.getPeerInfo().getAdvertisementSolicitationTimeout(), advertisementSolicitationTimedOut, null
         );
      }
   }

   private void cancelAdvertisementSolicitationTimer() {
      Ticket ticket = this.advertisementSolicitationTicket;
      if (ticket != null) {
         ticket.cancel();
      }

      this.advertisementSolicitationTicket = null;
   }
}
