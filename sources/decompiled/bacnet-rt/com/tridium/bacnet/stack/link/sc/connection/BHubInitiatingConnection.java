package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.BHubConnector;
import com.tridium.bacnet.stack.link.sc.BHubFunction;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.VmacUtil;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcMessage;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcResult;
import com.tridium.bacnet.stack.link.sc.message.ScReadMessageException;
import com.tridium.bacnet.stack.link.sc.message.ScSendMessageException;
import java.net.URI;
import java.net.URISyntaxException;
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
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.IPropertyValidator;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Validatable;
import javax.baja.util.BIRestrictedComponent;
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
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "hubUriAddress",
      type = "BInternetAddress",
      defaultValue = "new BInternetAddress(\"example.com\")"
   ), @NiagaraProperty(
      name = "hubUriPath",
      type = "String",
      defaultValue = "BHubFunction.DEFAULT_SERVLET_NAME"
   ), @NiagaraProperty(
      name = "hubUriQuery",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "hubAddress",
      type = "BInternetAddress",
      defaultValue = "new BInternetAddress(\"null\")",
      flags = 3
   ), @NiagaraProperty(
      name = "hubVmac",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.DEFAULT",
      flags = 3
   ), @NiagaraProperty(
      name = "hubUuid",
      type = "BUuid",
      defaultValue = "BUuid.DEFAULT",
      flags = 3
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
public final class BHubInitiatingConnection extends BInitiatingConnection implements BIRestrictedComponent, IPropertyValidator {
   public static final Property state = newProperty(259, BBacnetScConnectionState.notConnected, null);
   public static final Property subState = newProperty(259, BInitiatingConnectionState.DEFAULT, null);
   public static final Property enabled = newProperty(0, true, null);
   public static final Property hubUriAddress = newProperty(0, new BInternetAddress("example.com"), null);
   public static final Property hubUriPath = newProperty(0, "hub", null);
   public static final Property hubUriQuery = newProperty(0, "", null);
   public static final Property hubAddress = newProperty(3, new BInternetAddress("null"), null);
   public static final Property hubVmac = newProperty(3, BBacnetOctetString.DEFAULT, null);
   public static final Property hubUuid = newProperty(3, BUuid.DEFAULT, null);
   public static final Property lastConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastDisconnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property lastFailureToConnect = newProperty(1, BAbsTime.DEFAULT, null);
   public static final Property error = newProperty(1, new BErrorType(-1, -1), null);
   public static final Property errorDetails = newProperty(1, "", null);
   public static final Action forceDisconnect = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BHubInitiatingConnection.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.hubConnector");
   private BHubConnector hubConnector;

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

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public BInternetAddress getHubUriAddress() {
      return (BInternetAddress)this.get(hubUriAddress);
   }

   public void setHubUriAddress(BInternetAddress v) {
      this.set(hubUriAddress, v, null);
   }

   public String getHubUriPath() {
      return this.getString(hubUriPath);
   }

   public void setHubUriPath(String v) {
      this.setString(hubUriPath, v, null);
   }

   public String getHubUriQuery() {
      return this.getString(hubUriQuery);
   }

   public void setHubUriQuery(String v) {
      this.setString(hubUriQuery, v, null);
   }

   public BInternetAddress getHubAddress() {
      return (BInternetAddress)this.get(hubAddress);
   }

   public void setHubAddress(BInternetAddress v) {
      this.set(hubAddress, v, null);
   }

   public BBacnetOctetString getHubVmac() {
      return (BBacnetOctetString)this.get(hubVmac);
   }

   public void setHubVmac(BBacnetOctetString v) {
      this.set(hubVmac, v, null);
   }

   public BUuid getHubUuid() {
      return (BUuid)this.get(hubUuid);
   }

   public void setHubUuid(BUuid v) {
      this.set(hubUuid, v, null);
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

   public static BHubInitiatingConnection make(BInternetAddress address, String path, boolean enabled) {
      BHubInitiatingConnection connection = new BHubInitiatingConnection();
      connection.setHubUriAddress(address);
      connection.setHubUriPath(path);
      connection.setEnabled(enabled);
      return connection;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.hubConnector = (BHubConnector)this.getParent();
   }

   @Override
   protected void setRemoteAddress(BInternetAddress address) {
      this.setHubAddress(address);
   }

   @Override
   protected void setRemoteVmac(long vmac) {
      this.setHubVmac(VmacUtil.vmacToOctetString(vmac));
   }

   @Override
   protected void setRemoteUuid(UUID uuid) {
      this.setHubUuid(BUuid.make(uuid));
   }

   @Override
   protected void setRemoteMaxBvlcLength(int maxBvlcLength) {
   }

   @Override
   protected void setRemoteMaxNpduLength(int maxNpduLength) {
   }

   @Override
   public URI getURI() throws URISyntaxException {
      return makeURI(this.getHubUriAddress(), this.getHubUriPath().trim(), this.getHubUriQuery().trim());
   }

   @Override
   protected IScConnectionInitiator getConnectionInitiator() {
      return this.hubConnector;
   }

   @Override
   public String getLocalConnectionToken() {
      BHubFunction hubFunction = this.scLinkLayer.getHubFunction();
      return hubFunction == null ? null : hubFunction.generateLocalConnectionToken(this);
   }

   @Override
   public void clearLocalConnectionToken() {
      BHubFunction hubFunction = this.scLinkLayer.getHubFunction();
      if (hubFunction != null) {
         hubFunction.clearLocalConnectionToken();
      }
   }

   @Override
   protected void checkMessageSendSize(byte[] bytes) throws ScSendMessageException {
      if (bytes.length > 65535) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Send message length ")
                     .append(bytes.length)
                     .append(" exceeds the maximum BVLC message length of ")
                     .append(65535)
                     .append("; message: ")
                     .append(this.messageToString(bytes))
                     .toString()
               );
         }

         throw new ScSendMessageException("Send message length " + bytes.length + " exceeds the maximum BVLC message length of " + '\uffff');
      }
   }

   @Override
   protected void checkIsValidDestinationVmac(long vmac) throws ScReadMessageException {
      if (vmac != -1L && vmac != 281474976710655L) {
         throw new ScReadMessageException(
            "Destination VMAC of addressed message received from a hub must be absent or equal to the broadcast VMAC", BBacnetErrorCode.inconsistentParameters
         );
      }
   }

   @Override
   protected long getOriginatingVmac(byte[] payload, int offset, int len) throws ScReadMessageException {
      long vmac = ScBvlcMessage.readOriginatingVmac(payload, offset, len);
      if (!VmacUtil.isDeviceVmac(vmac)) {
         throw new ScReadMessageException(
            "Originating VMAC of an addressed message received on a hub connection must be present and a device VMAC", BBacnetErrorCode.inconsistentParameters
         );
      } else {
         return vmac;
      }
   }

   @Override
   protected void sendBvlcResultNak(byte[] payload, int offset, int len, ScBvlcResult bvlcResultNak) throws Exception {
      if (!ScBvlcMessage.isAddressedMessage(bvlcResultNak.getResultFunction())) {
         this.send(bvlcResultNak);
      } else {
         long destinationVmac = -1L;

         try {
            destinationVmac = ScBvlcMessage.readDestinationVmac(payload, offset, len);
         } catch (ScReadMessageException var10) {
         }

         if (destinationVmac == 281474976710655L) {
            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger()
                  .fine(this.getLogInfo().append(": Cannot send a read message exception BVLC-Result-NAK in response to a broadcast message").toString());
            }
         } else if (destinationVmac != -1L) {
            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger()
                  .fine(this.getLogInfo().append(": Sending read message exception BVLC-Result-NAK for non-null destination VMAC back to hub").toString());
            }

            this.send(bvlcResultNak);
         } else {
            long originatingVmac = -1L;

            try {
               originatingVmac = ScBvlcMessage.readOriginatingVmac(payload, offset, len);
            } catch (ScReadMessageException var11) {
               if (this.getLogger().isLoggable(Level.FINE)) {
                  this.getLogger()
                     .log(
                        Level.FINE,
                        this.getLogInfo()
                           .append(": Read message exception BVLC-Result-NAK in response to an addressed message contains an invalid originating VMAC")
                           .toString(),
                        (Throwable)var11
                     );
               }
            }

            if (VmacUtil.isDeviceVmac(originatingVmac)) {
               this.scLinkLayer.getNodeSwitch().sendMessage(originatingVmac, bvlcResultNak);
            } else {
               if (this.getLogger().isLoggable(Level.FINE)) {
                  this.getLogger()
                     .fine(
                        this.getLogInfo()
                           .append(
                              ": Cannot send a read message exception BVLC-Result-NAK in response to an addressed message with a non-device originating VMAC: ["
                           )
                           .append(VmacUtil.vmacToString(originatingVmac))
                           .append("]; sending to peer node (hub) instead")
                           .toString()
                     );
               }

               this.send(bvlcResultNak);
            }
         }
      }
   }

   public String toString(Context cx) {
      if (!this.getEnabled()) {
         return ScLinkLayerUtil.LEXICON.getText("disabled", cx);
      } else {
         StringBuilder builder = new StringBuilder(this.getState().getDisplayTag(cx)).append(" (wss://").append(this.getHubUriAddress());
         String path = this.getHubUriPath().trim();
         if (!path.startsWith("/")) {
            builder.append('/');
         }

         builder.append(path);
         String query = this.getHubUriQuery().trim();
         if (!query.isEmpty()) {
            builder.append('?').append(query);
         }

         builder.append(')');
         return builder.toString();
      }
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning() && property.equals(enabled)) {
         if (this.getEnabled()) {
            try {
               this.hubConnector.doForceConnect();
            } catch (Exception var4) {
            }
         } else {
            this.disconnect();
         }
      }
   }

   public void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      ScLinkLayerUtil.checkParentType(TYPE, parent.getType(), BHubConnector.TYPE);
      ScLinkLayerUtil.checkForTooMany(this, parent, 2);
   }

   public IPropertyValidator getPropertyValidator(Property property, Context context) {
      return (IPropertyValidator)(property.equals(hubUriQuery) ? this : super.getPropertyValidator(property, context));
   }

   public IPropertyValidator getPropertyValidator(Property[] properties, Context context) {
      for (Property property : properties) {
         if (property.equals(hubUriQuery)) {
            return this;
         }
      }

      return super.getPropertyValidator(properties, context);
   }

   public void validateSet(Validatable validatable, Context context) {
      for (Property property : validatable.getModifiedProperties()) {
         if (property.equals(hubUriQuery)) {
            checkUriQuery(((BString)validatable.getProposedValue(property)).getString());
         }
      }
   }

   public void validateSet(BComplex instance, Property property, BValue newValue, Context context) {
      if (property.equals(hubUriQuery)) {
         checkUriQuery(((BString)newValue).getString());
      }
   }

   @Override
   public Logger getLogger() {
      return logger;
   }

   @Override
   public StringBuilder getLogInfo() {
      return this.hubConnector.isPrimary(this) ? new StringBuilder("Primary Hub Connection") : new StringBuilder("Failover Hub Connection");
   }
}
