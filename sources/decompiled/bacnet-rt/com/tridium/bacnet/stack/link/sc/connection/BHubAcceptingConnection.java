package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.BHubFunction;
import com.tridium.bacnet.stack.link.sc.BHubFunctionConnections;
import com.tridium.bacnet.stack.link.sc.VmacUtil;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcMessage;
import com.tridium.bacnet.stack.link.sc.message.ScReadMessageException;
import com.tridium.bacnet.stack.link.sc.message.ScSendMessageException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BErrorType;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BUuid;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "state",
      type = "BBacnetScConnectionState",
      defaultValue = "BBacnetScConnectionState.notConnected",
      flags = 257
   ), @NiagaraProperty(
      name = "subState",
      type = "BAcceptingConnectionState",
      defaultValue = "BAcceptingConnectionState.DEFAULT",
      flags = 257
   ), @NiagaraProperty(
      name = "nodeAddress",
      type = "BInternetAddress",
      defaultValue = "new BInternetAddress(\"null\")",
      flags = 1
   ), @NiagaraProperty(
      name = "nodeVmac",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "nodeUuid",
      type = "BUuid",
      defaultValue = "BUuid.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "nodeMaxBvlcLength",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "nodeMaxNpduLength",
      type = "int",
      defaultValue = "0",
      flags = 1
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
@NiagaraAction(
   name = "forceDisconnect"
)
public final class BHubAcceptingConnection extends BAcceptingConnection {
   public static final Property state = newProperty(257, BBacnetScConnectionState.notConnected, null);
   public static final Property subState = newProperty(257, BAcceptingConnectionState.DEFAULT, null);
   public static final Property nodeAddress = newProperty(1, new BInternetAddress("null"), null);
   public static final Property nodeVmac = newProperty(1, BBacnetOctetString.DEFAULT, null);
   public static final Property nodeUuid = newProperty(1, BUuid.DEFAULT, null);
   public static final Property nodeMaxBvlcLength = newProperty(1, 0, null);
   public static final Property nodeMaxNpduLength = newProperty(1, 0, null);
   public static final Property lastConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastDisconnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastFailureToConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property error = newProperty(1, new BErrorType(-1, -1), null);
   public static final Property errorDetails = newProperty(1, "", null);
   public static final Action forceDisconnect = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BHubAcceptingConnection.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.hubFunction");
   private BHubFunction hubFunction;
   private boolean isLocal;

   @Override
   public BBacnetScConnectionState getState() {
      return (BBacnetScConnectionState)this.get(state);
   }

   @Override
   public void setState(BBacnetScConnectionState v) {
      this.set(state, v, null);
   }

   @Override
   public BAcceptingConnectionState getSubState() {
      return (BAcceptingConnectionState)this.get(subState);
   }

   @Override
   public void setSubState(BAcceptingConnectionState v) {
      this.set(subState, v, null);
   }

   public BInternetAddress getNodeAddress() {
      return (BInternetAddress)this.get(nodeAddress);
   }

   public void setNodeAddress(BInternetAddress v) {
      this.set(nodeAddress, v, null);
   }

   public BBacnetOctetString getNodeVmac() {
      return (BBacnetOctetString)this.get(nodeVmac);
   }

   public void setNodeVmac(BBacnetOctetString v) {
      this.set(nodeVmac, v, null);
   }

   public BUuid getNodeUuid() {
      return (BUuid)this.get(nodeUuid);
   }

   public void setNodeUuid(BUuid v) {
      this.set(nodeUuid, v, null);
   }

   public int getNodeMaxBvlcLength() {
      return this.getInt(nodeMaxBvlcLength);
   }

   public void setNodeMaxBvlcLength(int v) {
      this.setInt(nodeMaxBvlcLength, v, null);
   }

   public int getNodeMaxNpduLength() {
      return this.getInt(nodeMaxNpduLength);
   }

   public void setNodeMaxNpduLength(int v) {
      this.setInt(nodeMaxNpduLength, v, null);
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

   public void forceDisconnect() {
      this.invoke(forceDisconnect, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.hubFunction = (BHubFunction)this.getParent().getParent();
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BHubFunctionConnections;
   }

   @Override
   protected void setRemoteAddress(BInternetAddress address) {
      this.setNodeAddress(address);
   }

   @Override
   protected void setRemoteVmac(long vmac) {
      this.setNodeVmac(VmacUtil.vmacToOctetString(vmac));
   }

   @Override
   protected void setRemoteUuid(UUID uuid) {
      this.setNodeUuid(BUuid.make(uuid));
   }

   @Override
   protected void setRemoteMaxBvlcLength(int maxBvlcLength) {
      this.setNodeMaxBvlcLength(maxBvlcLength);
   }

   @Override
   protected void setRemoteMaxNpduLength(int maxNpduLength) {
      this.setNodeMaxNpduLength(maxNpduLength);
   }

   @Override
   protected int getLocalMaxBvlcLength() {
      return this.hubFunction.getMaxBvlcLength();
   }

   @Override
   protected int getNextMessageId() {
      return this.hubFunction.getNextMessageId();
   }

   @Override
   protected IScConnectionManager getConnectionManager() {
      return this.hubFunction;
   }

   @Override
   protected void checkMessageReceivedSize(byte[] payload, int offset, int len) throws ScReadMessageException {
      if (len > 65535) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Received message length ")
                     .append(len)
                     .append(" exceeds the maximum BVLC message length of ")
                     .append(65535)
                     .append("; message: ")
                     .append(this.messageToString(payload, offset, len))
                     .toString()
               );
         }

         throw new ScReadMessageException(
            "Received message length " + len + " exceeds the maximum BVLC message length of " + '\uffff', BBacnetErrorCode.messageTooLong
         );
      }
   }

   @Override
   protected void handleAddressedMessage(int function, byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      long destinationVmac = ScBvlcMessage.readDestinationVmac(payload, offset, len);
      if (function != 1 && function != 12) {
         checkIsUnicast(destinationVmac);
      }

      this.hubFunction.forwardMessage(this.getRemoteVmac(), destinationVmac, payload, offset, len);
   }

   @Override
   protected void handleBvlcResult(byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      int resultFunction = ScBvlcMessage.readResultFunction(payload, offset, len);
      if (ScBvlcMessage.isAddressedMessage(resultFunction)) {
         long destinationVmac = ScBvlcMessage.readDestinationVmac(payload, offset, len);
         checkIsUnicast(destinationVmac);
         this.hubFunction.forwardMessage(this.getRemoteVmac(), destinationVmac, payload, offset, len);
      } else {
         this.logIgnoredBvlcResult(payload, offset, len);
      }
   }

   @Override
   public void validateLocalConnectionToken(String connectionToken) {
      this.isLocal = this.hubFunction.isValidLocalConnectionToken(connectionToken);
   }

   public boolean isLocal() {
      return this.isLocal;
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
         ? new StringBuilder("Hub accepting connection, VMAC ").append(VmacUtil.vmacToString(remoteVmac)).append(", UUID ").append(remoteUuid)
         : new StringBuilder("Hub accepting connection");
   }

   public String toString(Context cx) {
      return this.getState().getDisplayTag(cx) + " [" + this.getNodeVmac().toString(cx) + ']';
   }
}
