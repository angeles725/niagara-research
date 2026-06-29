package com.tridium.bacnet.stack.link.sc.message;

public final class HeartbeatRequest extends ScBvlcMessage {
   HeartbeatRequest() {
   }

   private HeartbeatRequest(int messageId) {
      super(messageId);
   }

   public static HeartbeatRequest make(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return new HeartbeatRequest(messageId);
   }

   public static byte[] getBytes(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return ScBvlcMessage.getBytes(10, messageId);
   }

   @Override
   public int getFunction() {
      return 10;
   }
}
