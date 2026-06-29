package com.tridium.bacnet.stack.link.sc.message;

public final class AddressResolution extends AddressedMessage {
   AddressResolution() {
   }

   private AddressResolution(int messageId) {
      super(messageId);
   }

   public static AddressResolution make(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return new AddressResolution(messageId);
   }

   @Override
   public int getFunction() {
      return 2;
   }
}
