package com.tridium.bacnet.stack.link.sc.message;

import java.util.UUID;

public final class ConnectRequest extends ConnectMessage {
   ConnectRequest() {
   }

   private ConnectRequest(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      super(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
   }

   public static ConnectRequest make(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      checkArgs(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
      return new ConnectRequest(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
   }

   public static byte[] getBytes(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      checkArgs(messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
      return ConnectMessage.getBytes(6, messageId, vmac, deviceUuid, maxBvlcLength, maxNpduLength);
   }

   @Override
   public int getFunction() {
      return 6;
   }
}
