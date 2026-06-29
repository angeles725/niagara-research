package com.tridium.bacnet.stack.link.sc.message;

import java.util.UUID;

public final class ConnectAccept extends ConnectMessage {
   ConnectAccept() {
   }

   private ConnectAccept(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      super(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
   }

   public static ConnectAccept make(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      checkArgs(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
      return new ConnectAccept(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
   }

   public static byte[] getBytes(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      checkArgs(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
      return ConnectMessage.getBytes(7, messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
   }

   @Override
   public int getFunction() {
      return 7;
   }
}
