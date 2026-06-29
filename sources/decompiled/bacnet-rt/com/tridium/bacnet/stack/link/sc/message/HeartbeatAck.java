package com.tridium.bacnet.stack.link.sc.message;

public final class HeartbeatAck extends ScBvlcMessage {
   HeartbeatAck() {
   }

   private HeartbeatAck(int messageId) {
      super(messageId);
   }

   public static HeartbeatAck make(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return new HeartbeatAck(messageId);
   }

   public static byte[] getBytes(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return ScBvlcMessage.getBytes(11, messageId);
   }

   @Override
   public int getFunction() {
      return 11;
   }
}
