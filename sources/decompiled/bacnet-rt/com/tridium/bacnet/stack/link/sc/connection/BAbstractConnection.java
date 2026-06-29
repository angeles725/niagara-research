package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.BNodeSwitch;
import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.message.AddressResolution;
import com.tridium.bacnet.stack.link.sc.message.AddressResolutionAck;
import com.tridium.bacnet.stack.link.sc.message.Advertisement;
import com.tridium.bacnet.stack.link.sc.message.DisconnectAck;
import com.tridium.bacnet.stack.link.sc.message.DisconnectRequest;
import com.tridium.bacnet.stack.link.sc.message.HeartbeatAck;
import com.tridium.bacnet.stack.link.sc.message.HeartbeatRequest;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcMessage;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcResult;
import com.tridium.bacnet.stack.link.sc.message.ScMessageUtil;
import com.tridium.bacnet.stack.link.sc.message.ScNpdu;
import com.tridium.bacnet.stack.link.sc.message.ScReadMessageException;
import com.tridium.bacnet.stack.link.sc.message.ScSendMessageException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BErrorType;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Clock;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "connectWaitTimedOut",
      flags = 4
   ), @NiagaraAction(
      name = "disconnectWaitTimedOut",
      flags = 4
   ), @NiagaraAction(
      name = "noMessageReceived",
      flags = 4
   )})
public abstract class BAbstractConnection extends BComponent implements IScConnection {
   public static final Action connectWaitTimedOut = newAction(4, null);
   public static final Action disconnectWaitTimedOut = newAction(4, null);
   public static final Action noMessageReceived = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BAbstractConnection.class);
   private Ticket waitTicket;
   private static final int MESSAGE_RECEIVED = 0;
   private static final int NO_MESSAGE_RECEIVED = 1;
   private static final int HEARTBEAT_REQUEST_SENT = 2;
   private int keepAliveState = 1;
   protected IScWebSocket webSocket;
   private final ByteArrayOutputStream out = new ByteArrayOutputStream();
   private final ScDataOutputStream dataOut = new ScDataOutputStream(this.out);
   private final ByteBuffer in = new ByteBuffer(0);
   private long localVmac = -1L;
   protected int localMaxBvlcLength;
   protected int localMaxNpduLength;
   private UUID remoteUuid;
   private long remoteVmac = -1L;
   private int remoteMaxBvlcLength = 65535;
   protected int expectedMessageId;
   protected BScLinkLayer scLinkLayer;

   public void connectWaitTimedOut() {
      this.invoke(connectWaitTimedOut, null, null);
   }

   public void disconnectWaitTimedOut() {
      this.invoke(disconnectWaitTimedOut, null, null);
   }

   public void noMessageReceived() {
      this.invoke(noMessageReceived, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public abstract void doConnectWaitTimedOut();

   public abstract void doDisconnectWaitTimedOut();

   public synchronized void doNoMessageReceived() {
      switch (this.keepAliveState) {
         case 0:
            this.keepAliveState = 1;
            break;
         case 1:
            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger().fine(this.getLogInfo().append(": Sending Heartbeat-Request").toString());
            }

            try {
               this.expectedMessageId = this.getNextMessageId();
               this.send(HeartbeatRequest.getBytes(this.expectedMessageId));
               this.keepAliveState = 2;
            } catch (Exception var3) {
               this.logException("send Heartbeat-Request", var3);
               this.updateConnectionStateToFailed(
                  BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.sendMessageFailed", ScBvlcMessage.functionToString(10))
               );
               this.transitionToIdle(1011, "Exception sending Heartbeat-Request");
            }
            break;
         case 2:
         default:
            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger().fine(this.getLogInfo().append(": No message received after sending Heartbeat-Request").toString());
            }

            try {
               this.send(DisconnectRequest.getBytes(this.getNextMessageId()));
               this.updateConnectionStateToFailed(BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.heartbeatTimeoutExpired"));
            } catch (Exception var2) {
               this.logException("send Disconnect-Request when no-message-received timeout expired", var2);
               this.updateConnectionStateToFailed(
                  BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.heartbeatTimeoutSendDisconnectRequestFailed")
               );
            }

            this.transitionToIdle(1001, "No message received after sending Heartbeat-Request");
      }
   }

   protected final void handleWaitTimedOut(BFrozenEnum subState, BFrozenEnum expected) {
      this.handleWaitTimedOut(subState, expected, BBacnetErrorCode.timeout);
   }

   protected final void handleWaitTimedOut(BFrozenEnum subState, BFrozenEnum expected, BBacnetErrorCode errorCode) {
      if (subState != expected) {
         this.logTimeoutWrongState(subState, expected);
      } else {
         this.logTimeoutExpired(expected);
         this.updateConnectionStateToFailed(
            errorCode, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.waitTimeoutExpired", expected.getDisplayTag(null))
         );
         this.transitionToIdle(1001, "Wait timeout expired");
      }
   }

   protected abstract void resetPeriodicWaitTicket();

   protected abstract void setRemoteAddress(BInternetAddress var1);

   protected abstract void setRemoteVmac(long var1);

   protected abstract void setRemoteUuid(UUID var1);

   protected abstract void setRemoteMaxBvlcLength(int var1);

   protected abstract void setRemoteMaxNpduLength(int var1);

   protected abstract void setState(BBacnetScConnectionState var1);

   public abstract BBacnetScConnectionState getState();

   protected abstract void setError(BErrorType var1);

   public abstract BErrorType getError();

   protected abstract void setErrorDetails(String var1);

   public abstract String getErrorDetails();

   protected abstract void setLastConnect(BAbsTime var1);

   public abstract BAbsTime getLastConnect();

   protected abstract void setLastDisconnect(BAbsTime var1);

   public abstract BAbsTime getLastDisconnect();

   protected abstract void setLastFailureToConnect(BAbsTime var1);

   public abstract BAbsTime getLastFailureToConnect();

   public abstract boolean isConnected();

   public abstract boolean isIdle();

   public abstract boolean isDisconnecting();

   public abstract void disconnect();

   protected final void transitionToIdle() {
      this.transitionToIdle(1000, null);
   }

   protected abstract void transitionToIdle(int var1, String var2);

   public void started() throws Exception {
      super.started();
      this.scLinkLayer = ScLinkLayerUtil.getScLinkLayer(this);
   }

   public final void stopped() throws Exception {
      super.stopped();
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": Calling disconnect on stopped connection").toString());
      }

      this.disconnect();
   }

   public void sendMessage(ScBvlcMessage message) throws BacnetException {
      if (this.isConnected()) {
         this.logSendMessage(message);

         try {
            this.send(message);
         } catch (Exception var3) {
            this.logException("send message: " + message, var3);
            this.updateConnectionStateToFailed(
               BBacnetErrorCode.websocketError,
               ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.sendMessageFailed", ScBvlcMessage.functionToString(message.getFunction()))
            );
            this.transitionToIdle(1011, "Exception while sending message");
            throw new BacnetException(this.getLogInfo().append(": sendMessage failed with exception: ").append(var3.getMessage()).toString(), var3);
         }
      } else {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger().fine(this.getLogInfo().append(": Attempted to send message while not connected; message: ").append(message).toString());
         }

         throw new NotConnectedException("sendMessage called while not in the Connected state");
      }
   }

   public void sendMessage(byte[] bytes) throws Exception {
      if (this.isConnected()) {
         this.logSendMessage(bytes);

         try {
            this.sendBytes(bytes);
         } catch (Exception var3) {
            this.logException("send message bytes: " + this.messageToString(bytes), var3);
            this.updateConnectionStateToFailed(
               BBacnetErrorCode.websocketError, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.sendMessageBytesFailed")
            );
            this.transitionToIdle(1011, "Exception while sending message");
            throw new BacnetException(this.getLogInfo().append(": sendMessage bytes failed with exception: ").append(var3.getMessage()).toString(), var3);
         }
      } else {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(this.getLogInfo().append(": Attempted to send message bytes while not connected; bytes: ").append(Arrays.toString(bytes)).toString());
         }

         throw new NotConnectedException("sendMessage bytes called while not in the Connected state");
      }
   }

   protected final void send(ScBvlcMessage message) throws ScSendMessageException {
      this.logWebSocketSend(message);

      byte[] bytes;
      try {
         synchronized (this.dataOut) {
            this.dataOut.reset();
            message.encode(this.dataOut);
            bytes = this.out.toByteArray();
         }
      } catch (Exception var6) {
         throw new ScSendMessageException(message, "Exception encoding message to bytes: " + var6.getMessage(), var6);
      }

      this.sendBytes(bytes);
   }

   protected final void send(byte[] bytes) throws ScSendMessageException {
      this.logWebSocketSend(bytes);
      this.sendBytes(bytes);
   }

   private void sendBytes(byte[] bytes) throws ScSendMessageException {
      this.checkMessageSendSize(bytes);

      try {
         this.webSocket.sendBytes(bytes);
      } catch (Exception var3) {
         throw new ScSendMessageException(bytes, "Exception sending bytes on web socket: " + var3.getMessage(), var3);
      }
   }

   @Override
   public final synchronized void messageReceived(byte[] payload, int offset, int len) {
      try {
         this.checkPayloadOffsetLength(payload, offset, len);
         this.checkMessageReceivedSize(payload, offset, len);
         this.handleMessageReceived(payload, offset, len);
      } catch (ScReadMessageException var5) {
         this.handleReadMessageException(payload, offset, len, var5);
      } catch (ScSendMessageException var6) {
         this.handleSendMessageException(payload, offset, len, var6);
      }
   }

   protected abstract void handleMessageReceived(byte[] var1, int var2, int var3) throws ScReadMessageException, ScSendMessageException;

   private void handleReadMessageException(byte[] payload, int offset, int len, ScReadMessageException e) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .log(
               Level.FINE,
               this.getLogInfo().append(": Exception reading message: ").append(this.messageToString(payload, offset, len)).toString(),
               (Throwable)e
            );
      }

      int resultFunction;
      try {
         ScBvlcMessage.checkHasFunctionByte(payload, offset, len);
         resultFunction = ByteArrayUtil.readUnsignedByte(payload, offset);
      } catch (ScReadMessageException var11) {
         this.getLogger()
            .log(
               Level.FINE,
               this.getLogInfo().append(": Exception reading the resultFunction for a read message exception BVLC-Result-NAK").toString(),
               (Throwable)var11
            );
         resultFunction = 0;
      }

      int messageId;
      try {
         messageId = ScBvlcMessage.readMessageId(payload, offset, len);
      } catch (ScReadMessageException var10) {
         this.getLogger()
            .log(
               Level.FINE,
               this.getLogInfo().append(": Exception reading the messageId for a read message exception BVLC-Result-NAK").toString(),
               (Throwable)var10
            );
         messageId = this.getNextMessageId();
      }

      try {
         ScBvlcResult bvlcResultNak = ScBvlcResult.makeNak(messageId, resultFunction, e.getHeaderMarker(), 7, e.getErrorCode().getOrdinal(), e.getMessage());
         this.sendBvlcResultNak(payload, offset, len, bvlcResultNak);
      } catch (ScSendMessageException var8) {
         this.getLogger().log(Level.FINE, this.getLogInfo().append(": Exception sending a read message exception BVLC-Result-NAK").toString(), (Throwable)var8);
         this.updateConnectionStateToFailed(BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.sendReadErrorNakFailed"));
         this.transitionToIdle(1011, "Exception sending BVLC-Result-NAK for read message exception");
      } catch (Exception var9) {
         this.getLogger()
            .log(
               Level.FINE,
               this.getLogInfo().append(": Other exception encountered when sending a read message exception BVLC-Result-NAK").toString(),
               (Throwable)var9
            );
      }
   }

   protected void sendBvlcResultNak(byte[] payload, int offset, int len, ScBvlcResult bvlcResultNak) throws Exception {
      this.send(bvlcResultNak);
   }

   private void handleSendMessageException(byte[] payload, int offset, int len, ScSendMessageException e) {
      int function = -1;
      ScBvlcMessage message = e.getScMessage();
      if (message != null) {
         function = message.getFunction();
         this.logException("send message in response to message: " + message, e);
      } else {
         try {
            function = ScBvlcMessage.readFunction(e.getScMessageBytes(), 0, e.getScMessageBytes().length);
         } catch (ScReadMessageException var8) {
         }

         this.logException("send message in response to message: " + this.messageToString(payload, offset, len), e);
      }

      this.updateConnectionStateToFailed(
         BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.sendMessageFailed", ScBvlcMessage.functionToString(function))
      );
      this.transitionToIdle(1011, "Exception sending message");
   }

   protected final ScBvlcMessage readMessage(byte[] payload, int offset, int len) throws ScReadMessageException {
      try {
         this.in.reset();
         this.in.setBuffer(payload);
         this.in.setLength(offset + len);
         this.in.skipBytes(offset);
         return ScBvlcMessage.make(this.in);
      } catch (IOException var5) {
         throw new ScReadMessageException("Error initializing the ByteBuffer", var5, BBacnetErrorCode.internalError);
      }
   }

   protected final void handleMessageConnected(byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      this.keepAliveState = 0;
      int function = ScBvlcMessage.readFunction(payload, offset, len);
      switch (function) {
         case 0:
            this.handleBvlcResult(payload, offset, len);
            break;
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
            this.handleAddressedMessage(function, payload, offset, len);
            break;
         case 6:
         case 7:
         case 9:
         default:
            this.logUnexpectedMessageReceived("Connected", payload, offset, len);
            break;
         case 8:
            this.handleDisconnectRequest(payload, offset, len);
            break;
         case 10:
            this.handleHeartbeatRequest(payload, offset, len);
            break;
         case 11:
            this.handleHeartbeatAck(payload, offset, len);
      }
   }

   private void handleDisconnectRequest(byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      DisconnectRequest disconnectRequest = (DisconnectRequest)this.readMessage(payload, offset, len);
      this.send(DisconnectAck.getBytes(disconnectRequest.getMessageId()));
      this.updateConnectionStateToNotConnected();
      this.transitionToIdle();
   }

   private void handleHeartbeatRequest(byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      HeartbeatRequest heartbeatRequest = (HeartbeatRequest)this.readMessage(payload, offset, len);
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": Sending Heartbeat-ACK").toString());
      }

      this.send(HeartbeatAck.getBytes(heartbeatRequest.getMessageId()));
   }

   private void handleHeartbeatAck(byte[] payload, int offset, int len) throws ScReadMessageException {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": Received Heartbeat-ACK").toString());
      }

      HeartbeatAck heartbeatAck = (HeartbeatAck)this.readMessage(payload, offset, len);
      if (heartbeatAck.getMessageId() != this.expectedMessageId) {
         this.logMessageIdMismatch(BInitiatingConnectionState.connected, this.expectedMessageId, payload, offset, len);
      } else {
         this.keepAliveState = 1;
      }
   }

   protected void handleAddressedMessage(int function, byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      long destinationVmac = ScBvlcMessage.readDestinationVmac(payload, offset, len);
      this.checkIsValidDestinationVmac(destinationVmac);
      long originatingVmac = this.getOriginatingVmac(payload, offset, len);
      switch (function) {
         case 1:
            ScNpdu scNpdu = (ScNpdu)this.readMessage(payload, offset, len);
            this.checkNpduLength(scNpdu);
            destinationVmac = destinationVmac == -1L ? this.localVmac : destinationVmac;
            this.forwardNpdu(originatingVmac, destinationVmac, scNpdu);
            break;
         case 2:
            checkIsUnicast(destinationVmac);
            AddressResolution addressResolution = (AddressResolution)this.readMessage(payload, offset, len);
            this.sendAddressResolutionAck(addressResolution.getMessageId(), originatingVmac);
            break;
         case 3:
            checkIsUnicast(destinationVmac);
            AddressResolutionAck addressResolutionAck = (AddressResolutionAck)this.readMessage(payload, offset, len);
            this.scLinkLayer.getNodeSwitch().handleAddressResolutionAck(originatingVmac, addressResolutionAck);
            break;
         case 4:
            checkIsUnicast(destinationVmac);
            Advertisement advertisement = (Advertisement)this.readMessage(payload, offset, len);
            this.scLinkLayer.getNodeSwitch().handleAdvertisement(originatingVmac, advertisement);
            break;
         case 5:
            checkIsUnicast(destinationVmac);
            this.readMessage(payload, offset, len);
            this.sendAdvertisement(originatingVmac);
            break;
         default:
            throw new ScReadMessageException("Not an addressed message", BBacnetErrorCode.internalError);
      }
   }

   protected void checkIsValidDestinationVmac(long vmac) throws ScReadMessageException {
      if (vmac != -1L && vmac != this.localVmac) {
         throw new ScReadMessageException(
            "Destination VMAC of a message received on a direct connection must be either absent or equal to the local VMAC",
            BBacnetErrorCode.inconsistentParameters
         );
      }
   }

   protected long getOriginatingVmac(byte[] payload, int offset, int len) throws ScReadMessageException {
      long vmac = ScBvlcMessage.readOriginatingVmac(payload, offset, len);
      if (vmac != -1L && vmac != this.remoteVmac) {
         throw new ScReadMessageException(
            "Originating VMAC of a message received on a direct connection must be either absent or equal to the remote VMAC",
            BBacnetErrorCode.inconsistentParameters
         );
      } else {
         return this.remoteVmac;
      }
   }

   protected static void checkIsUnicast(long destinationVmac) throws ScReadMessageException {
      if (destinationVmac == 281474976710655L) {
         throw new ScReadMessageException("May not be a broadcast message", BBacnetErrorCode.inconsistentParameters);
      }
   }

   private void checkNpduLength(ScNpdu scNpdu) throws ScReadMessageException {
      int npduLength = scNpdu.getRawNpdu().length;
      if (npduLength > this.localMaxNpduLength) {
         throw new ScReadMessageException(
            "NPDU length of " + npduLength + " exceeds local maximum of " + this.localMaxNpduLength, BBacnetErrorCode.messageTooLong
         );
      }
   }

   protected final void sendAddressResolutionAck(int messageId, long destinationVmac) {
      try {
         BNodeSwitch nodeSwitch = this.scLinkLayer.getNodeSwitch();
         if (!nodeSwitch.acceptEnabled()) {
            ScBvlcResult bvlcResultNak = ScBvlcResult.makeNak(messageId, 2, 0, 7, 45, "Direct connections are not supported");
            nodeSwitch.sendMessage(destinationVmac, bvlcResultNak);
            return;
         }

         BBacnetArray acceptUris = nodeSwitch.getAcceptUris();
         List<String> uriStrings = new ArrayList<>(acceptUris.getSize());

         for (int i = 0; i < acceptUris.getSize(); i++) {
            String acceptUri = ((BString)acceptUris.getElement(i + 1)).getString();
            String error = ScMessageUtil.checkWebSocketUri(acceptUri);
            if (error == null) {
               uriStrings.add(acceptUri);
            } else if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger().fine(this.getLogInfo().append(": Invalid URI \"").append(acceptUri).append("\" not being sent: ").append(error).toString());
            }
         }

         AddressResolutionAck ack = AddressResolutionAck.make(messageId, uriStrings);
         nodeSwitch.sendMessage(destinationVmac, ack);
      } catch (Exception var10) {
         this.getLogger().log(Level.FINE, this.getLogInfo().append(": Failed to send response to Address-Resolution").toString(), (Throwable)var10);
      }
   }

   protected final void sendAdvertisement(long destinationVmac) {
      BNodeSwitch nodeSwitch = this.scLinkLayer.getNodeSwitch();
      Advertisement advertisement = Advertisement.make(
         this.getNextMessageId(),
         this.scLinkLayer.getHubConnector().getState(),
         nodeSwitch.acceptEnabled(),
         this.scLinkLayer.getMaxBvlcLength(),
         this.scLinkLayer.getMaxNpduLength()
      );

      try {
         nodeSwitch.sendMessage(destinationVmac, advertisement);
      } catch (Exception var6) {
         this.getLogger().log(Level.FINE, this.getLogInfo().append(": Failed to send response to Address-Solicitation").toString(), (Throwable)var6);
      }
   }

   protected final void forwardNpdu(long originatingVmac, long destinationVmac, ScNpdu npdu) {
      try {
         this.getConnectionManager().forwardNpdu(originatingVmac, destinationVmac, npdu);
      } catch (Exception var7) {
         this.logException("forward NPDU", var7);
      }
   }

   protected void handleBvlcResult(byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      int resultFunction = ScBvlcMessage.readResultFunction(payload, offset, len);
      if (resultFunction == 2 || resultFunction == 5) {
         long destinationVmac = ScBvlcMessage.readDestinationVmac(payload, offset, len);
         this.checkIsValidDestinationVmac(destinationVmac);
         checkIsUnicast(destinationVmac);
         long originatingVmac = this.getOriginatingVmac(payload, offset, len);
         ScBvlcResult bvlcResult = (ScBvlcResult)this.readMessage(payload, offset, len);
         if (bvlcResult.isNak()) {
            if (resultFunction == 2) {
               this.scLinkLayer.getNodeSwitch().handleAddressResolutionNak(originatingVmac, bvlcResult);
            } else {
               this.scLinkLayer.getNodeSwitch().handleAdvertisementNak(originatingVmac, bvlcResult);
            }

            return;
         }
      }

      this.logIgnoredBvlcResult(payload, offset, len);
   }

   protected final void handleMessageDisconnecting(byte[] payload, int offset, int len) throws ScReadMessageException {
      int function = ScBvlcMessage.readFunction(payload, offset, len);
      int messageId = ScBvlcMessage.readMessageId(payload, offset, len);
      switch (function) {
         case 0:
            if (messageId != this.expectedMessageId) {
               this.logMessageIdMismatch(BInitiatingConnectionState.disconnecting, this.expectedMessageId, payload, offset, len);
               return;
            }

            ScBvlcResult result = (ScBvlcResult)this.readMessage(payload, offset, len);
            if (result.isAck() || result.getResultFunction() != 8) {
               this.logDisconnectingBadBvlcResult(result);
               return;
            }

            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger()
                  .fine(
                     this.getLogInfo().append(": BVLC Result NAK received in Disconnecting with error details: ").append(result.getErrorDetails()).toString()
                  );
            }

            this.updateConnectionStateToFailed(
               result.getErrorClass(),
               result.getErrorCode(),
               ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.requestFailed", ScBvlcMessage.functionToString(8), result.getErrorDetails())
            );
            this.transitionToIdle(1001, "Received BVLC-Result-NAK for Disconnect-Request");
            break;
         case 9:
            if (messageId != this.expectedMessageId) {
               this.logMessageIdMismatch(BInitiatingConnectionState.disconnecting, this.expectedMessageId, payload, offset, len);
               return;
            }

            this.readMessage(payload, offset, len);
            this.updateConnectionStateToNotConnected();
            this.transitionToIdle();
            break;
         default:
            this.logUnexpectedMessageReceived("Disconnecting", payload, offset, len);
      }
   }

   protected int getNextMessageId() {
      return this.scLinkLayer.getNextMessageId();
   }

   @Override
   public abstract Logger getLogger();

   @Override
   public abstract StringBuilder getLogInfo();

   protected abstract IScConnectionManager getConnectionManager();

   protected final void setLocalVmac(long vmac) {
      this.localVmac = vmac;
   }

   protected int getLocalMaxBvlcLength() {
      return this.scLinkLayer.getMaxBvlcLength();
   }

   protected final void setRemoteInfo(long vmac, UUID uuid, int maxBvlcLength) {
      this.remoteVmac = vmac;
      this.remoteUuid = uuid;
      this.remoteMaxBvlcLength = maxBvlcLength;
   }

   public final long getRemoteVmac() {
      return this.remoteVmac;
   }

   public final UUID getRemoteUuid() {
      return this.remoteUuid;
   }

   public final int getRemoteMaxBvlcLength() {
      return this.remoteMaxBvlcLength;
   }

   @Override
   public final synchronized void webSocketFailed(BBacnetErrorCode errorCode, String errorDetails) {
      this.logWebSocketFailure(errorCode, errorDetails);
      if (!this.isIdle()) {
         if (this.scLinkLayer.isCommStarted()) {
            this.updateConnectionStateToFailed(errorCode, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.webSocketFailed", errorDetails));
         } else {
            this.updateConnectionStateToNotConnected();
         }

         this.transitionToIdle();
      }
   }

   protected final void updateConnectionInfo(long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      this.setRemoteVmac(vmac);
      this.setRemoteUuid(deviceUuid);
      this.setRemoteMaxBvlcLength(maxBvlcLength);
      this.setRemoteMaxNpduLength(maxNpduLength);
   }

   protected final void updateConnectionStateToNotConnected() {
      this.updateConnectionState(BBacnetScConnectionState.notConnected, -1, -1, "");
      this.setLastDisconnect(BAbsTime.now());
   }

   protected final void updateConnectionStateToConnected() {
      this.updateConnectionState(BBacnetScConnectionState.connected, -1, -1, "");
      this.setLastConnect(BAbsTime.now());
   }

   protected final void updateConnectionStateToFailed(BBacnetErrorCode errorCode, String errorDetails) {
      this.updateConnectionStateToFailed(7, errorCode.getOrdinal(), errorDetails);
   }

   protected final void updateConnectionStateToFailed(int errorClass, int errorCode, String errorDetails) {
      BBacnetScConnectionState newState;
      if (!this.isConnected() && !this.isDisconnecting()) {
         newState = BBacnetScConnectionState.failedToConnect;
         this.setLastFailureToConnect(BAbsTime.now());
      } else {
         newState = BBacnetScConnectionState.disconnectedWithErrors;
         this.setLastDisconnect(BAbsTime.now());
      }

      this.updateConnectionState(newState, errorClass, errorCode, errorDetails);
   }

   private void updateConnectionState(BBacnetScConnectionState state, int errorClass, int errorCode, String errorDetails) {
      this.setState(state);
      this.setError(new BErrorType(errorClass, errorCode));
      this.setErrorDetails(errorDetails);
   }

   protected void checkMessageSendSize(byte[] bytes) throws ScSendMessageException {
      if (bytes.length > this.remoteMaxBvlcLength) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Send message length ")
                     .append(bytes.length)
                     .append(" exceeds the remote maximum BVLC length of ")
                     .append(this.remoteMaxBvlcLength)
                     .append("; message: ")
                     .append(this.messageToString(bytes))
                     .toString()
               );
         }

         throw new ScSendMessageException(
            "Send message length " + bytes.length + " exceeds the remote node's maximum BVLC length of " + this.remoteMaxBvlcLength
         );
      }
   }

   private void checkPayloadOffsetLength(byte[] payload, int offset, int len) throws ScReadMessageException {
      if (payload == null || offset < 0 || len < 1 || offset + len > payload.length) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Received message not valid; payload == null ? ")
                     .append(payload == null)
                     .append(", offset: ")
                     .append(offset)
                     .append(", length: ")
                     .append(len)
                     .toString()
               );
         }

         throw new ScReadMessageException("Message payload, offset, and/or length values are not valid", BBacnetErrorCode.messageIncomplete);
      }
   }

   protected void checkMessageReceivedSize(byte[] payload, int offset, int len) throws ScReadMessageException {
      if (len > this.localMaxBvlcLength) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(
                  this.getLogInfo()
                     .append(": Received message length ")
                     .append(len)
                     .append(" exceeds the local maximum BVLC length of ")
                     .append(this.localMaxBvlcLength)
                     .append("; message: ")
                     .append(this.messageToString(payload, offset, len))
                     .toString()
               );
         }

         throw new ScReadMessageException(
            "Received message length " + len + " exceeds the link layer's maximum BVLC length of " + this.localMaxBvlcLength, BBacnetErrorCode.messageTooLong
         );
      }
   }

   protected final void closeWebSocket(int statusCode, String reason) {
      if (this.webSocket != null) {
         this.webSocket.close(statusCode, reason);
      }

      this.webSocket = null;
   }

   protected final void cancelWaitTicket() {
      if (this.waitTicket != null) {
         this.waitTicket.cancel();
      }

      this.waitTicket = null;
   }

   protected final void resetWaitTicket(BRelTime time, Action action) {
      this.cancelWaitTicket();
      if (this.isRunning()) {
         this.waitTicket = Clock.schedule(this, time.getSeconds() > 0 ? time : BRelTime.SECOND, action, null);
      }
   }

   protected final void startPeriodicWaitTicket(BRelTime time) {
      this.cancelWaitTicket();
      if (this.isRunning()) {
         this.keepAliveState = 1;
         this.waitTicket = Clock.schedulePeriodically(this, time.getSeconds() > 0 ? time : BRelTime.SECOND, noMessageReceived, null);
      }
   }

   public void setSocketIdleTimeout(long timeout) {
      IScWebSocket webSocket = this.webSocket;
      if (webSocket != null) {
         webSocket.setIdleTimeout(timeout);
      }
   }

   protected final void logTransition(BFrozenEnum state) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": Entered state ").append(state.getTag()).toString());
      }
   }

   protected final void logMessageReceived(String state, byte[] payload, int offset, int len) {
      if (this.getLogger().isLoggable(Level.FINER)) {
         this.getLogger()
            .finer(
               this.getLogInfo()
                  .append(": Message received while in the ")
                  .append(state)
                  .append(" state; message: ")
                  .append(this.messageToString(payload, offset, len))
                  .toString()
            );
      }
   }

   protected final void logUnexpectedMessageReceived(String state, byte[] payload, int offset, int len) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               this.getLogInfo()
                  .append(": Unexpected message received while in ")
                  .append(state)
                  .append(" state; message: ")
                  .append(this.messageToString(payload, offset, len))
                  .toString()
            );
      }
   }

   protected final String messageToString(byte[] payload) {
      return this.messageToString(payload, 0, payload.length);
   }

   protected final String messageToString(byte[] payload, int offset, int len) {
      try {
         return this.readMessage(payload, offset, len).toString();
      } catch (Exception var5) {
         return "(exception " + var5.getMessage() + " while reading message) " + Arrays.toString(copyOfRange(payload, offset, len));
      }
   }

   private static byte[] copyOfRange(byte[] payload, int offset, int len) {
      try {
         return Arrays.copyOfRange(payload, offset, offset + len);
      } catch (Exception var4) {
         return payload;
      }
   }

   protected final void logConnectionKilled(String state) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": Connection was killed while in the state ").append(state).toString());
      }
   }

   protected final void logWebSocketFailure(BBacnetErrorCode errorCode, String errorDetails) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               this.getLogInfo()
                  .append(": WebSocketFailure was received. Error:")
                  .append(errorCode.getDisplayTag(null))
                  .append("; details: ")
                  .append(errorDetails)
                  .toString()
            );
      }
   }

   protected final void logMessageIdMismatch(BFrozenEnum state, int expectedMessageId, byte[] payload, int offset, int len) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               this.getLogInfo()
                  .append(": Wrong message ID in state ")
                  .append(state.getTag())
                  .append("; expected: ")
                  .append(expectedMessageId)
                  .append("; received: ")
                  .append(this.messageToString(payload, offset, len))
                  .toString()
            );
      }
   }

   protected final void logDisconnectingBadBvlcResult(ScBvlcResult result) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               this.getLogInfo()
                  .append(": BVLC-Result message received in state Disconnecting is not NAK or result function is not Disconnect-Request (0x08): NAK?: ")
                  .append(!result.isAck())
                  .append("; result function: ")
                  .append(result.getResultFunction())
                  .toString()
            );
      }
   }

   protected final void logIgnoredBvlcResult(byte[] payload, int offset, int len) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               this.getLogInfo()
                  .append(": Unexpected BVLC-Result message received while Connected: ")
                  .append(this.messageToString(payload, offset, len))
                  .toString()
            );
      }
   }

   protected final void logException(String action, Exception e) {
      ScLinkLayerUtil.logException(this.getLogger(), this.getLogInfo().append(": ").append(action).append(" failed"), e);
   }

   protected final void logTimeoutExpired(BFrozenEnum state) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": Wait timeout expired while in state ").append(state.getTag()).toString());
      }
   }

   protected final void logTimeoutWrongState(BFrozenEnum currentState, BFrozenEnum expectedState) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               this.getLogInfo()
                  .append(": Unexpected wait timeout occurred while in state ")
                  .append(currentState.getTag())
                  .append(", expected state is ")
                  .append(expectedState.getTag())
                  .toString()
            );
      }
   }

   private void logSendMessage(ScBvlcMessage message) {
      if (this.getLogger().isLoggable(Level.FINER)) {
         this.getLogger().finer(this.getLogInfo().append(": Sending message: ").append(message).toString());
      }
   }

   private void logSendMessage(byte[] bytes) {
      if (this.getLogger().isLoggable(Level.FINER)) {
         this.getLogger().finer(this.getLogInfo().append(": Sending message bytes: ").append(this.messageToString(bytes)).toString());
      }
   }

   private void logWebSocketSend(ScBvlcMessage message) {
      if (this.getLogger().isLoggable(Level.FINEST)) {
         this.getLogger().finest(this.getLogInfo().append(": Sending message over web socket: ").append(message).toString());
      }
   }

   private void logWebSocketSend(byte[] bytes) {
      if (this.getLogger().isLoggable(Level.FINEST)) {
         this.getLogger().finest(this.getLogInfo().append(": Sending message bytes over web socket: ").append(this.messageToString(bytes)).toString());
      }
   }
}
