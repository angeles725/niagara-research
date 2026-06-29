package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.BNodeSwitch;
import com.tridium.bacnet.stack.link.sc.BNodeSwitchConnections;
import com.tridium.bacnet.stack.link.sc.VmacUtil;
import java.util.UUID;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BErrorType;
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
      name = "peerAddress",
      type = "BInternetAddress",
      defaultValue = "new BInternetAddress(\"null\")",
      flags = 1
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
      flags = 1
   ), @NiagaraProperty(
      name = "peerMaxNpduLength",
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
public final class BDirectAcceptingConnection extends BAcceptingConnection {
   public static final Property state = newProperty(257, BBacnetScConnectionState.notConnected, null);
   public static final Property subState = newProperty(257, BAcceptingConnectionState.DEFAULT, null);
   public static final Property peerAddress = newProperty(1, new BInternetAddress("null"), null);
   public static final Property peerVmac = newProperty(1, BBacnetOctetString.DEFAULT, null);
   public static final Property peerUuid = newProperty(1, BUuid.DEFAULT, null);
   public static final Property peerMaxBvlcLength = newProperty(1, 0, null);
   public static final Property peerMaxNpduLength = newProperty(1, 0, null);
   public static final Property lastConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastDisconnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastFailureToConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property error = newProperty(1, new BErrorType(-1, -1), null);
   public static final Property errorDetails = newProperty(1, "", null);
   public static final Action forceDisconnect = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BDirectAcceptingConnection.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.nodeSwitch");
   private BNodeSwitch nodeSwitch;

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

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BNodeSwitchConnections;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.nodeSwitch = (BNodeSwitch)this.getParent().getParent();
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
   protected IScConnectionManager getConnectionManager() {
      return this.nodeSwitch;
   }

   @Override
   public void validateLocalConnectionToken(String connectionToken) {
   }

   public String toString(Context cx) {
      return this.getState().getDisplayTag(cx) + " [" + this.getPeerVmac().toString(cx) + ']';
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
         ? new StringBuilder("Direct accepting connection, VMAC ").append(VmacUtil.vmacToString(remoteVmac)).append(", UUID ").append(remoteUuid)
         : new StringBuilder("Direct accepting connection");
   }
}
