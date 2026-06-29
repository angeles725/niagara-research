package com.tridium.bacnet.stack.link.sc.message;

public final class AdvertisementSolicitation extends AddressedMessage {
   AdvertisementSolicitation() {
   }

   private AdvertisementSolicitation(int messageId) {
      super(messageId);
   }

   public static AdvertisementSolicitation make(int messageId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return new AdvertisementSolicitation(messageId);
   }

   @Override
   public int getFunction() {
      return 5;
   }
}
