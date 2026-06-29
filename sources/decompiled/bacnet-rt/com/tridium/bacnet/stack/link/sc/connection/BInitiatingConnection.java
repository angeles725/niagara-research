package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.message.ConnectAccept;
import com.tridium.bacnet.stack.link.sc.message.ConnectRequest;
import com.tridium.bacnet.stack.link.sc.message.DisconnectRequest;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcMessage;
import com.tridium.bacnet.stack.link.sc.message.ScBvlcResult;
import com.tridium.bacnet.stack.link.sc.message.ScReadMessageException;
import com.tridium.bacnet.stack.link.sc.message.ScSendMessageException;
import com.tridium.bacnet.stack.network.BNetworkPort;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedActionException;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "webSocketWaitTimedOut",
   flags = 4
)
public abstract class BInitiatingConnection extends BAbstractConnection {
   public static final Action webSocketWaitTimedOut = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BInitiatingConnection.class);

   public void webSocketWaitTimedOut() {
      this.invoke(webSocketWaitTimedOut, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public abstract URI getURI() throws URISyntaxException;

   protected static URI makeURI(BInternetAddress address, String path, String query) throws URISyntaxException {
      if (!path.isEmpty() && !path.startsWith("/")) {
         path = '/' + path;
      }

      return new URI("wss", null, address.getHost(), address.getPort(), !path.isEmpty() ? path : null, !query.isEmpty() ? query : null, null);
   }

   protected static void checkUriQuery(String query) {
      if (query.contains("[") || query.contains("]")) {
         throw new LocalizableRuntimeException("bacnet", "initiatingConnection.illegalCharInQuery");
      }
   }

   public final String getSubProtocol() {
      return this.getConnectionManager().getSubProtocol();
   }

   public abstract BInitiatingConnectionState getSubState();

   public abstract void setSubState(BInitiatingConnectionState var1);

   public final synchronized void doWebSocketWaitTimedOut() {
      this.handleWaitTimedOut(this.getSubState(), BInitiatingConnectionState.awaitingWebSocket, BBacnetErrorCode.websocketError);
   }

   @Override
   public final synchronized void doConnectWaitTimedOut() {
      this.handleWaitTimedOut(this.getSubState(), BInitiatingConnectionState.awaitingAccept);
   }

   @Override
   public final synchronized void doDisconnectWaitTimedOut() {
      this.handleWaitTimedOut(this.getSubState(), BInitiatingConnectionState.disconnecting);
   }

   @Override
   public final synchronized void doNoMessageReceived() {
      if (this.getSubState() != BInitiatingConnectionState.connected) {
         this.logTimeoutWrongState(this.getSubState(), BInitiatingConnectionState.connected);
      } else {
         super.doNoMessageReceived();
      }
   }

   public synchronized void connect() {
      if (this.getSubState().equals(BInitiatingConnectionState.idle)) {
         this.transitionToAwaitingWebSocket();

         try {
            this.webSocket = this.scLinkLayer.getWebSocketInitiator().initiateWebSocket(this);
         } catch (Exception var3) {
            Exception actual = var3;
            if (var3 instanceof PrivilegedActionException) {
               actual = ((PrivilegedActionException)var3).getException();
            }

            this.getLogger()
               .log(
                  Level.FINE,
                  this.getLogInfo().append(": initiateWebSocket failed with exception ").append(var3.getLocalizedMessage()).toString(),
                  (Throwable)actual
               );
            this.updateConnectionStateToFailed(
               BBacnetErrorCode.websocketError,
               ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.initiateWebSocketFailed", actual.getLocalizedMessage())
            );
            this.transitionToIdle();
         }
      } else if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": Connect called while not in the Idle state").toString());
      }
   }

   @Override
   public final boolean isConnected() {
      return this.getSubState().equals(BInitiatingConnectionState.connected);
   }

   @Override
   public final boolean isIdle() {
      return this.getSubState().equals(BInitiatingConnectionState.idle);
   }

   @Override
   public final boolean isDisconnecting() {
      return this.getSubState().equals(BInitiatingConnectionState.disconnecting);
   }

   @Override
   public synchronized void disconnect() {
      if (this.getSubState().equals(BInitiatingConnectionState.connected)) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger().fine(this.getLogInfo().append(": Disconnect called while connected").toString());
         }

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
      } else if (!this.getSubState().equals(BInitiatingConnectionState.awaitingWebSocket)
         && !this.getSubState().equals(BInitiatingConnectionState.awaitingAccept)) {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(this.getLogInfo().append(": Disconnect call ignored while in the ").append(this.getSubState().getTag()).append(" state.").toString());
         }
      } else {
         if (this.getLogger().isLoggable(Level.FINE)) {
            this.getLogger()
               .fine(this.getLogInfo().append(": Disconnect called while in the ").append(this.getSubState().getTag()).append(" state.").toString());
         }

         this.updateConnectionStateToFailed(
            BBacnetErrorCode.other,
            ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.disconnectingBeforeConnected", this.getSubState().getDisplayTag(null))
         );
         this.transitionToIdle(1001, "Disconnect requested within server");
      }
   }

   public synchronized void kill() {
      if (!this.getSubState().equals(BInitiatingConnectionState.idle)) {
         this.logConnectionKilled(this.getSubState().getTag());
         this.updateConnectionStateToFailed(BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.killedConnection"));
         this.transitionToIdle(1001, "Disconnect requested within server");
      }
   }

   public void doForceDisconnect() {
      BInitiatingConnectionState subState = this.getSubState();
      if (subState.equals(BInitiatingConnectionState.idle)) {
         throw new LocalizableRuntimeException("bacnet", "initiatingConnection.disconnect.improperState");
      } else {
         if (subState.equals(BInitiatingConnectionState.disconnecting)) {
            this.kill();
         } else {
            this.disconnect();
         }
      }
   }

   @Override
   public final synchronized void resetPeriodicWaitTicket() {
      if (this.getSubState().equals(BInitiatingConnectionState.connected)) {
         this.startPeriodicWaitTicket(this.scLinkLayer.getConfig().getInitiatingHeartbeatTimeout());
      }
   }

   @Override
   public final synchronized void webSocketConnected(IScWebSocket webSocket) {
      if (this.getSubState().equals(BInitiatingConnectionState.awaitingWebSocket)) {
         if (webSocket != this.webSocket) {
            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger().fine(this.getLogInfo().append(": Connected webSocket does not match expected webSocket").toString());
            }

            return;
         }

         this.setRemoteAddress(new BInternetAddress(webSocket.getRemoteHost(), webSocket.getRemotePort()));
         webSocket.setIdleTimeout(this.scLinkLayer.getConfig().getInitiatingSocketIdleTimeout());

         try {
            this.expectedMessageId = this.getNextMessageId();
            long localVmac = this.scLinkLayer.getLocalVmac();
            this.localMaxBvlcLength = this.getLocalMaxBvlcLength();
            this.localMaxNpduLength = this.scLinkLayer.getMaxNpduLength();
            this.send(
               ConnectRequest.getBytes(
                  this.expectedMessageId, localVmac, ScLinkLayerUtil.getLocalDeviceUuid(), this.localMaxBvlcLength, this.localMaxNpduLength
               )
            );
            this.setLocalVmac(localVmac);
            this.transitionToAwaitingAccept();
         } catch (Exception var4) {
            this.logException("send Connect-Request", var4);
            this.updateConnectionStateToFailed(
               BBacnetErrorCode.other, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.sendMessageFailed", ScBvlcMessage.functionToString(6))
            );
            this.transitionToIdle(1011, "Exception sending Connect-Request");
         }
      } else if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine(this.getLogInfo().append(": WebSocketConnected called while not in the AwaitingWebSocket state").toString());
      }
   }

   @Override
   protected final void handleMessageReceived(byte[] payload, int offset, int len) throws ScReadMessageException, ScSendMessageException {
      switch (this.getSubState().getOrdinal()) {
         case 0:
         case 1:
            this.logUnexpectedMessageReceived(this.getSubState().toString(), payload, offset, len);
            break;
         case 2:
            this.logMessageReceived("AwaitingAccept", payload, offset, len);
            this.handleMessageAwaitingAccept(payload, offset, len);
            break;
         case 3:
            this.logMessageReceived("Connected", payload, offset, len);
            this.handleMessageConnected(payload, offset, len);
            break;
         case 4:
            this.logMessageReceived("Disconnecting", payload, offset, len);
            this.handleMessageDisconnecting(payload, offset, len);
            break;
         default:
            this.logUnexpectedMessageReceived("Unknown", payload, offset, len);
      }
   }

   private void handleMessageAwaitingAccept(byte[] payload, int offset, int len) throws ScReadMessageException {
      int function = ScBvlcMessage.readFunction(payload, offset, len);
      int messageId = ScBvlcMessage.readMessageId(payload, offset, len);
      switch (function) {
         case 0:
            if (messageId != this.expectedMessageId) {
               this.logMessageIdMismatch(BInitiatingConnectionState.awaitingAccept, this.expectedMessageId, payload, offset, len);
               return;
            }

            ScBvlcResult result = (ScBvlcResult)this.readMessage(payload, offset, len);
            if (result.isAck() || result.getResultFunction() != 6) {
               this.logAwaitingAcceptBadBvlcResult(result);
               return;
            }

            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger()
                  .fine(
                     this.getLogInfo()
                        .append(": BVLC Result NAK received in AwaitingAccept; error class: ")
                        .append(BBacnetErrorClass.tag(result.getErrorClass()))
                        .append(", error code: ")
                        .append(BBacnetErrorCode.tag(result.getErrorCode()))
                        .append(", error details: ")
                        .append(result.getErrorDetails())
                        .toString()
                  );
            }

            String errorDetails;
            if (result.getErrorCode() == 151) {
               errorDetails = ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.duplicateVmac");
            } else {
               errorDetails = ScLinkLayerUtil.getLexiconText(
                  "sc.connection.errorDetails.requestFailed", ScBvlcMessage.functionToString(6), result.getErrorDetails()
               );
            }

            this.updateConnectionStateToFailed(result.getErrorClass(), result.getErrorCode(), errorDetails);
            this.transitionToIdle(1001, errorDetails);
            if (result.getErrorCode() == 151) {
               BBacnetNetwork.bacnet().postAsync(() -> this.regenerateVmac());
            }
            break;
         case 7:
            if (messageId != this.expectedMessageId) {
               this.logMessageIdMismatch(BInitiatingConnectionState.awaitingAccept, this.expectedMessageId, payload, offset, len);
               return;
            }

            ConnectAccept accept = (ConnectAccept)this.readMessage(payload, offset, len);
            this.setRemoteInfo(accept.getVmac(), accept.getDeviceUuid(), accept.getMaxBvlcLength());

            try {
               this.updateConnectionInfo(this.getRemoteVmac(), this.getRemoteUuid(), this.getRemoteMaxBvlcLength(), accept.getMaxNpduLength());
               this.getConnectionInitiator().activateConnection(this);
               this.updateConnectionStateToConnected();
               this.transitionToConnected();
            } catch (DuplicateVmacException var9) {
               this.logException("connectAccept", var9);
               this.sendDisconnectRequest();
               this.updateConnectionStateToFailed(
                  BBacnetErrorCode.nodeDuplicateVmac, ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.duplicateVmac")
               );
               this.transitionToIdle(1002, "Duplicate VMAC");
            } catch (Exception var10) {
               this.logException("connectAccept", var10);
               this.sendDisconnectRequest();
               this.updateConnectionStateToFailed(
                  BBacnetErrorCode.other,
                  ScLinkLayerUtil.getLexiconText("sc.connection.errorDetails.errorHandlingConnectMessage", ScBvlcMessage.functionToString(7))
               );
               this.transitionToIdle(1011, "Exception activating connection");
            }
            break;
         default:
            this.logUnexpectedMessageReceived("AwaitingAccept", payload, offset, len);
      }
   }

   private void regenerateVmac() {
      BNetworkPort networkPort = (BNetworkPort)this.scLinkLayer.getParent();
      networkPort.disable();
      this.scLinkLayer.regenerateVmacAddress();
      networkPort.enable();
   }

   private void logAwaitingAcceptBadBvlcResult(ScBvlcResult result) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               this.getLogInfo()
                  .append(": BVLC-Result message received in state AWAITING_ACCEPT is not NAK or its result function is not Connect-Request (0x06); NAK?: ")
                  .append(!result.isAck())
                  .append("; result function: ")
                  .append(result.getResultFunction())
                  .toString()
            );
      }
   }

   private void sendDisconnectRequest() {
      try {
         this.expectedMessageId = this.getNextMessageId();
         this.send(DisconnectRequest.getBytes(this.expectedMessageId));
      } catch (Exception var2) {
         this.logException("send Disconnect-Request", var2);
      }
   }

   @Override
   protected final IScConnectionManager getConnectionManager() {
      return this.getConnectionInitiator();
   }

   protected abstract IScConnectionInitiator getConnectionInitiator();

   public abstract String getLocalConnectionToken();

   public abstract void clearLocalConnectionToken();

   private void transitionToAwaitingWebSocket() {
      this.setSubState(BInitiatingConnectionState.awaitingWebSocket);
      this.logTransition(BInitiatingConnectionState.awaitingWebSocket);
      this.resetWaitTicket(this.scLinkLayer.getConfig().getWebSocketWaitTimeout(), webSocketWaitTimedOut);
   }

   private void transitionToAwaitingAccept() {
      this.setSubState(BInitiatingConnectionState.awaitingAccept);
      this.logTransition(BInitiatingConnectionState.awaitingAccept);
      this.resetWaitTicket(this.scLinkLayer.getConfig().getConnectWaitTimeout(), connectWaitTimedOut);
   }

   @Override
   protected final void transitionToIdle(int statusCode, String reason) {
      int oldState = this.getSubState().getOrdinal();
      this.setSubState(BInitiatingConnectionState.idle);
      this.logTransition(BInitiatingConnectionState.idle);
      this.closeWebSocket(statusCode, reason);
      this.cancelWaitTicket();
      this.clearLocalConnectionToken();

      try {
         switch (oldState) {
            case 1:
            case 2:
               this.getConnectionInitiator().initiatedConnectionFailed(this);
               break;
            case 3:
               this.getConnectionInitiator().deactivateConnection(this);
         }
      } catch (Exception var5) {
         this.logException("initiatedConnectionFailed or deactivateConnection when going back to Idle", var5);
      }
   }

   private void transitionToConnected() {
      this.setSubState(BInitiatingConnectionState.connected);
      this.logTransition(BInitiatingConnectionState.connected);
      this.startPeriodicWaitTicket(this.scLinkLayer.getConfig().getInitiatingHeartbeatTimeout());
      this.clearLocalConnectionToken();
   }

   private void transitionToDisconnecting() {
      this.setSubState(BInitiatingConnectionState.disconnecting);
      this.logTransition(BInitiatingConnectionState.disconnecting);
      this.resetWaitTicket(this.scLinkLayer.getConfig().getDisconnectWaitTimeout(), disconnectWaitTimedOut);

      try {
         this.getConnectionInitiator().deactivateConnection(this);
      } catch (Exception var2) {
         this.logException("deactivateConnection when transitioning to disconnecting", var2);
      }
   }
}
