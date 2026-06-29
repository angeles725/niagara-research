package com.tridium.bacnet.stack.link.sc.message;

public final class DisconnectRequest extends ScBvlcMessage {
   DisconnectRequest() {
   }

   private DisconnectRequest(int messageId) {
      super(messageId);
   }

   public static DisconnectRequest make(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return new DisconnectRequest(messageId);
   }

   public static byte[] getBytes(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return ScBvlcMessage.getBytes(8, messageId);
   }

   @Override
   public int getFunction() {
      return 8;
   }
}
