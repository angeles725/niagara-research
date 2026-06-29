package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.message.ConnectAccept;
import com.tridium.bacnet.stack.link.sc.message.ConnectRequest;
import com.tridium.bacnet.stack.link.sc.message.DisconnectRequest;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcMessage;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcResult;
import com.tridium.bacnet.stack.link.sc.message.ScReadMessageException;
import com.tridium.bacnet.stack.link.sc.message.ScSendMessageException;
import java.util.logging.Level;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BAcceptingConnection extends BAbstractConnection {
   public static final Type TYPE = Sys.loadType(BAcceptingConnection.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public abstract BAcceptingConnectionState getSubState();

   public abstract void setSubState(BAcceptingConnectionState var1);

   @Override
   public final synchronized void doConnectWaitTimedOut() {
      this.handleWaitTimedOut(this.getSubState(), BAcceptingConnectionState.awaitingRequest);
   }

   @Override
   public final synchronized void doDisconnectWaitTimedOut() {
      this.handleWaitTimedOut(this.getSubState(), BAcceptingConnectionState.disconnecting);
   }

   @Override
   public final synchronized void doNoMessageReceived() {
      if (this.getSubState() != BAcceptingConnectionState.connected) {
         this.logTimeoutWrongState(this.getSubState(), BAcceptingConnectionState.connected);
      } else {
         super.doNoMessageReceived();
      }
   }

   @Override
   public final boolean isConnected() {
      return this.getSubState().equals(BAcceptingConnectionState.connected);
   }

   @Override
   public final boolean isIdle() {
      return this.getSubState().equals(BAcceptingConnectionState.idle);
   }

   @Override
   public final boolean isDisconnecting() {
      return this.getSubState().equals(BAcceptingConnectionState.disconnecting);
   }

   @Override
   public synchronized void disconnect() {
      if (this.getSubState().equals(BAcceptingConnectionState.connected)) {
         this.transitionToDisconnecting();

         try {
            this.expectedMessageId = this.getNextMessageId();
            this.send(DisconnectRequest.getBytes(this.expectedMessageId));
         } catch (Exception var2) {
            this.logException("send Disconnect-Request when transitioning to disconnecting", var2);
            this.updateConnectionStateToFailed(
               BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.disconnectingSendDisconnectRequestFailed")
            );
            this.transitionToIdle(1011, "Exception sending Disconnect-Request");
         }
      } else if (this.getSubState().equals(BAcceptingConnectionState.awaitingRequest)) {
         this.updateConnectionStateToFailed(
            BBacnetErrorCode.other,
            ScLinkLayerUtil.getLexiconText(
               "sc.connection.errorDetails.disconnectingBeforeConnected", BAcceptingConnectionState.awaitingRequest.getDisplayTag(null)
            )
         );
         this.transitionToIdle(1001, "Disconnect requested within server");
      } else if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(this.getLogInfo().append(": Disconnect call ignored while in the ").append(this.getSubState().getTag()).append(" state.").toString());
      }
   }

   public synchronized void kill() {
      if (!this.getSubState().equals(BAcceptingConnectionState.idle)) {
         this.logConnectionKilled(this.getSubState().getTag());
         this.updateConnectionStateToFailed(BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.killedConnection"));
         this.transitionToIdle(1001, "Disconnect requested within server");
      }
   }

   public final void doForceDisconnect() {
      BAcceptingConnectionState subState = this.getSubState();
      if (subState.equals(BAcceptingConnectionState.idle)) {
         throw new LocalizableRuntimeException("bacnet", "acceptingConnection.disconnect.improperState");
      } else {
         if (subState.equals(BAcceptingConnectionState.disconnecting)) {
            this.kill();
         } else {
            this.disconnect();
         }
      }
   }

   @Override
   public final synchronized void resetPeriodicWaitTicket() {
      if (this.getSubState().equals(BAcceptingConnectionState.connected)) {
         this.startPeriodicWaitTicket(this.scLinkLayer.getConfig().getAcceptingHeartbeatTimeout());
      }
   }

   @Override
   public final synchronized void webSocketConnected(IScWebSocket webSocket) {
      if (this.getSubState().equals(BAcceptingConnectionState.idle)) {
         this.webSocket = webSocket;
         this.setRemoteAddress(new BInternetAddress(webSocket.getRemoteHost(), webSocket.getRemotePort()));
         webSocket.setIdleTimeout(this.scLinkLayer.getConfig().getAcceptingSocketIdleTimeout());
         this.localMaxBvlcLength = this.getLocalMaxBvlcLength();
         this.localMaxNpduLength = this.scLinkLayer.getMaxNpduLength();
         this.transitionToAwaitingRequest();
      } else if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": WebSocketConnected called while not in the Idle state").toString());
      }
   }

   @Override
   protected final void handleMessageReceived(byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      switch (this.getSubState().getOrdinal()) {
         case 0:
            this.logUnexpectedMessageReceived("Idle", payload, offset, len);
            break;
         case 1:
            this.logMessageReceived("AwaitingRequest", payload, offset, len);
            this.handleMessageAwaitingRequest(payload, offset, len);
            break;
         case 2:
            this.logMessageReceived("Connected", payload, offset, len);
            this.handleMessageConnected(payload, offset, len);
            break;
         case 3:
            this.logMessageReceived("Disconnecting", payload, offset, len);
            this.handleMessageDisconnecting(payload, offset, len);
            break;
         default:
            this.logUnexpectedMessageReceived("Unknown", payload, offset, len);
      }
   }

   private void handleMessageAwaitingRequest(byte[] payload, int offset, int len) throws ScReadMessageException {
      int function = ScBvlcMessage.readFunction(payload, offset, len);
      if (function == 6) {
         ConnectRequest request = (ConnectRequest)this.readMessage(payload, offset, len);
         this.setRemoteInfo(request.getVmac(), request.getDeviceUuid(), request.getMaxBvlcLength());

         try {
            this.updateConnectionInfo(this.getRemoteVmac(), this.getRemoteUuid(), this.getRemoteMaxBvlcLength(), request.getMaxNpduLength());
            this.getConnectionManager().activateConnection(this);
         } catch (DuplicateVmacException var9) {
            this.sendConnectRequestNak(request.getMessageId(), 151, "Duplicate VMAC Address detected");
            this.getLogger()
               .log(
                  Level.FINE,
                  this.getLogInfo().append(": ConnectionRequest failed with DuplicateVmacException ").append(var9.getLocalizedMessage()).toString(),
                  (Throwable)var9
               );
            this.updateConnectionStateToFailed(BBacnetErrorCode.nodeDuplicateVmac, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.duplicateVmac"));
            this.transitionToIdle(1002, "Duplicate VMAC");
            return;
         } catch (Exception var10) {
            this.sendConnectRequestNak(request.getMessageId(), 0, "Failure when activating connection");
            this.getLogger()
               .log(
                  Level.FINE,
                  this.getLogInfo().append(": ConnectionRequest failed with exception ").append(var10.getLocalizedMessage()).toString(),
                  (Throwable)var10
               );
            this.updateConnectionStateToFailed(
               BBacnetErrorCode.other,
               ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.errorHandlingConnectMessage", ScBvlcMessage.functionToString(6))
            );
            this.transitionToIdle(1011, "Exception activating connection");
            return;
         }

         try {
            long localVmac = this.scLinkLayer.getLocalVmac();
            this.send(
               ConnectAccept.getBytes(request.getMessageId(), localVmac, ScLinkLayerUtil.getLocalDeviceUuid(), this.localMaxBvlcLength, this.localMaxNpduLength)
            );
            this.setLocalVmac(localVmac);
            this.updateConnectionStateToConnected();
            this.transitionToConnected();
         } catch (Exception var8) {
            this.getLogger()
               .log(
                  Level.FINE,
                  this.getLogInfo().append(": Send connectAccept failed with exception ").append(var8.getLocalizedMessage()).toString(),
                  (Throwable)var8
               );
            this.updateConnectionStateToFailed(
               BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.sendMessageFailed", ScBvlcMessage.functionToString(7))
            );
            this.transitionToIdle(1011, "Exception sending Connect-Accept");
            this.deactivateConnection("deactivateConnection when failing to send Connect-Accept");
         }
      } else {
         this.logUnexpectedMessageReceived("AwaitingRequest", payload, offset, len);
      }
   }

   private void sendConnectRequestNak(int messageId, int errorCode, String errorText) {
      try {
         this.send(ScBvlcResult.makeNak(messageId, 6, 0, 7, errorCode, errorText));
      } catch (Exception var5) {
         this.logException("send BvlcResult-NAK when activating connection", var5);
      }
   }

   @Override
   protected abstract IScConnectionManager getConnectionManager();

   private void transitionToAwaitingRequest() {
      this.setSubState(BAcceptingConnectionState.awaitingRequest);
      this.logTransition(BAcceptingConnectionState.awaitingRequest);
      this.resetWaitTicket(this.scLinkLayer.getConfig().getConnectWaitTimeout(), connectWaitTimedOut);
   }

   private void transitionToConnected() {
      this.setSubState(BAcceptingConnectionState.connected);
      this.logTransition(BAcceptingConnectionState.connected);
      this.startPeriodicWaitTicket(this.scLinkLayer.getConfig().getAcceptingHeartbeatTimeout());
   }

   private void transitionToDisconnecting() {
      this.setSubState(BAcceptingConnectionState.disconnecting);
      this.logTransition(BAcceptingConnectionState.disconnecting);
      this.resetWaitTicket(this.scLinkLayer.getConfig().getDisconnectWaitTimeout(), disconnectWaitTimedOut);
      this.deactivateConnection("deactivateConnection when transitioning from Connected to Disconnecting");
   }

   @Override
   protected final void transitionToIdle(int statusCode, String reason) {
      BAcceptingConnectionState oldState = this.getSubState();
      this.setSubState(BAcceptingConnectionState.idle);
      this.logTransition(BAcceptingConnectionState.idle);
      this.closeWebSocket(statusCode, reason);
      this.cancelWaitTicket();
      if (oldState.equals(BAcceptingConnectionState.connected)) {
         this.deactivateConnection("deactivateConnection when transitioning from Connected to Idle");
      }
   }

   private void deactivateConnection(String action) {
      try {
         this.getConnectionManager().deactivateConnection(this);
      } catch (Exception var3) {
         this.logException(action, var3);
      }
   }

   public abstract void validateLocalConnectionToken(String var1);
}
