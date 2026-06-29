package com.tridium.bacnet.stack.link.sc.message;

import java.io.DataOutput;
import java.io.IOException;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;

public abstract class HeaderOption {
   private final int headerMarker;

   protected HeaderOption(int headerMarker) {
      this.headerMarker = headerMarker;
   }

   protected HeaderOption(int optionType, boolean mustUnderstand, boolean hasData) {
      int headerMarker = optionType;
      if (hasData) {
         headerMarker = optionType | 32;
      }

      if (mustUnderstand) {
         headerMarker |= 64;
      }

      this.headerMarker = headerMarker;
   }

   public static HeaderOption make(ByteBuffer in, boolean isDestinationOption) throws IOException, ScReadMessageException {
      int headerMarker = in.readUnsignedByte();

      try {
         HeaderOption headerOption;
         switch (readOptionType(headerMarker)) {
            case 1:
               if (isDestinationOption) {
                  throw new ScReadMessageException("Secure path destination header option not supported", BBacnetErrorCode.inconsistentParameters, headerMarker);
               }

               headerOption = new SecurePathHeaderOption(headerMarker);
               break;
            case 31:
               if (isDestinationOption && mustUnderstand(headerMarker)) {
                  throw new ScReadMessageException("Proprietary destination header option not understood", BBacnetErrorCode.headerNotUnderstood, headerMarker);
               }

               headerOption = new ProprietaryHeaderOption(headerMarker);
               break;
            default:
               if (isDestinationOption && mustUnderstand(headerMarker)) {
                  throw new ScReadMessageException("Unknown destination header option not understood", BBacnetErrorCode.headerNotUnderstood, headerMarker);
               }

               headerOption = new UnsupportedHeaderOption(headerMarker);
         }

         headerOption.checkMustUnderstandFlag(mustUnderstand(headerMarker), isDestinationOption);
         headerOption.checkDataFlag(hasData(headerMarker), isDestinationOption);
         headerOption.decodeHeaderData(in);
         return headerOption;
      } catch (IOException var4) {
         throw new ScReadMessageException("Message header is incomplete", BBacnetErrorCode.messageIncomplete, isDestinationOption ? headerMarker : 0);
      }
   }

   protected void checkMustUnderstandFlag(boolean mustUnderstand, boolean isDestinationOption) throws ScReadMessageException {
   }

   protected void checkDataFlag(boolean hasData, boolean isDestinationOption) throws ScReadMessageException {
      if (hasData) {
         throw new ScReadMessageException("Header Data flag should not be set", BBacnetErrorCode.inconsistentParameters);
      }
   }

   protected void decodeHeaderData(ByteBuffer in) throws IOException {
   }

   public final void encode(DataOutput out, boolean hasMore) throws IOException {
      int encodedHeaderMarker = this.headerMarker;
      if (hasMore) {
         encodedHeaderMarker |= 128;
      }

      out.writeByte(encodedHeaderMarker);
      this.encodeHeaderData(out);
   }

   protected void encodeHeaderData(DataOutput out) throws IOException {
   }

   private static int readOptionType(int headerMarker) {
      return headerMarker & 31;
   }

   private static boolean hasData(int headerMarker) {
      return (headerMarker & 32) > 0;
   }

   private static boolean mustUnderstand(int headerMarker) {
      return (headerMarker & 64) > 0;
   }

   public final int getHeaderMarker() {
      return this.headerMarker;
   }

   public final int getOptionType() {
      return readOptionType(this.headerMarker);
   }

   public final boolean hasData() {
      return hasData(this.headerMarker);
   }

   public final boolean mustUnderstand() {
      return mustUnderstand(this.headerMarker);
   }

   public final boolean hasMore() {
      return (this.headerMarker & 128) > 0;
   }

   @Override
   public String toString() {
      return optionToString(this.getOptionType())
         + "; hasData? "
         + this.hasData()
         + "; mustUnderstand? "
         + this.mustUnderstand()
         + "; hasMore? "
         + this.hasMore();
   }

   private static String optionToString(int optionType) {
      switch (optionType) {
         case 1:
            return "Secure Path (0x01)";
         case 31:
            return "Proprietary (0x31)";
         default:
            return String.format("Unknown - 0x%02X", optionType);
      }
   }
}
