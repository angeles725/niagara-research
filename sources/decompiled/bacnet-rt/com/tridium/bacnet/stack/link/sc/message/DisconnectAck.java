package com.tridium.bacnet.stack.link.sc.message;

public final class DisconnectAck extends ScBvlcMessage {
   DisconnectAck() {
   }

   private DisconnectAck(int messageId) {
      super(messageId);
   }

   public static DisconnectAck make(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return new DisconnectAck(messageId);
   }

   public static byte[] getBytes(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return ScBvlcMessage.getBytes(9, messageId);
   }

   @Override
   public int getFunction() {
      return 9;
   }
}
