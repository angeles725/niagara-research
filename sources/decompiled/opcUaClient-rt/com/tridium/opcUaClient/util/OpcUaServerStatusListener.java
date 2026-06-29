package com.tridium.opcUaClient.util;

import com.prosysopc.ua.client.ServerStatusListener;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.core.ServerState;
import com.prosysopc.ua.stack.core.ServerStatusDataType;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaCore.enums.BServerState;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpcUaServerStatusListener implements ServerStatusListener {
   private final BOpcUaDevice opcUaDevice;
   private static final Logger logger = Logger.getLogger("opcUaClient.client");

   public OpcUaServerStatusListener(BOpcUaDevice device) {
      this.opcUaDevice = device;
   }

   public void onShutdown(UaClient uaClient, long secondsTillShutdown, LocalizedText shutdownReason) {
      if (!this.isDeviceDisabled(this.opcUaDevice)) {
         String msg = String.format(
            this.opcUaDevice.getDisplayName(null) + " Server shutdown in %d seconds. Reason: %s", secondsTillShutdown, shutdownReason.getText()
         );
         logger.info(msg);
         this.opcUaDevice.setServerState(BServerState.make(ServerState.Shutdown.getValue()));
      }
   }

   public void onStateChange(UaClient uaClient, ServerState oldState, ServerState newState) {
      if (!this.isDeviceDisabled(this.opcUaDevice)) {
         int newServerState = newState.getValue();
         if (newServerState != this.opcUaDevice.getServerState().getOrdinal()) {
            String msg = String.format("%s ServerState changed from %s to %s", this.opcUaDevice.getDisplayName(null), oldState, newState);
            logger.log(Level.INFO, msg);
            this.opcUaDevice.setServerState(BServerState.make(newState.getValue()));
            if (newState == ServerState.Running && this.opcUaDevice.getEnabled()) {
               this.opcUaDevice.ping();
            }

            if (newState == ServerState.Unknown && logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, "ServerStatusError: " + uaClient.getServerStatusError());
            }
         }
      }
   }

   public void onStatusChange(UaClient uaClient, ServerStatusDataType status, StatusCode statusCode) {
      if (this.isDeviceDisabled(this.opcUaDevice) && this.opcUaDevice.uaClient != null && this.opcUaDevice.uaClient.isConnected()) {
         this.opcUaDevice.uaClient.disconnect();
      } else {
         if (logger.isLoggable(Level.FINE)) {
            String msg = String.format("%s ServerStatus: %s, code: %s", this.opcUaDevice.getDisplayName(null), status, statusCode);
            logger.fine(msg);
         }
      }
   }

   private boolean isDeviceDisabled(BOpcUaDevice device) {
      if (!device.getEnabled()) {
         String msg = String.format(
            "%s device is down. So making the server state to unknown to block further communication with server.", device.getDisplayName(null)
         );
         logger.info(msg);
         device.setServerState(BServerState.make(7));
         return true;
      } else {
         return false;
      }
   }
}
