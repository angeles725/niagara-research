package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.network.DataAttribute;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;

public final class ProprietaryHeaderOption extends HeaderOption implements DataAttribute {
   private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
   private int vendorId;
   private int proprietaryType;
   private byte[] proprietaryHeaderData;

   ProprietaryHeaderOption(int headerMarker) {
      super(headerMarker);
   }

   private ProprietaryHeaderOption(boolean mustUnderstand, int vendorId, int proprietaryType, byte[] proprietaryHeaderData) {
      super(31, mustUnderstand, true);
      this.vendorId = vendorId;
      this.proprietaryType = proprietaryType;
      this.proprietaryHeaderData = proprietaryHeaderData;
   }

   public static ProprietaryHeaderOption make(boolean mustUnderstand, int vendorId, int proprietaryType, byte[] proprietaryHeaderData) {
      if (proprietaryHeaderData == null) {
         proprietaryHeaderData = EMPTY_BYTE_ARRAY;
      }

      ScMessageUtil.checkUnsignedShort(proprietaryHeaderData.length + 3, "proprietaryHeaderData length");
      ScMessageUtil.checkUnsignedByte(proprietaryType, "proprietaryType");
      ScMessageUtil.checkUnsignedShort(vendorId, "vendorId");
      return new ProprietaryHeaderOption(mustUnderstand, vendorId, proprietaryType, proprietaryHeaderData);
   }

   public static ProprietaryHeaderOption copy(ProprietaryHeaderOption source) {
      return new ProprietaryHeaderOption(source.mustUnderstand(), source.vendorId, source.proprietaryType, source.proprietaryHeaderData);
   }

   @Override
   protected void checkDataFlag(boolean hasData, boolean isDestinationOption) throws ScReadMessageException {
      if (!hasData) {
         throw new ScReadMessageException(
            "Header Data flag must be set on a Proprietary Header Option",
            BBacnetErrorCode.inconsistentParameters,
            isDestinationOption ? this.getHeaderMarker() : 0
         );
      }
   }

   @Override
   protected void encodeHeaderData(DataOutput out) throws IOException {
      out.writeShort(this.proprietaryHeaderData.length + 3);
      out.writeShort(this.vendorId);
      out.writeByte(this.proprietaryType);
      out.write(this.proprietaryHeaderData);
   }

   @Override
   protected void decodeHeaderData(ByteBuffer in) throws IOException {
      int headerDataLength = in.readUnsignedShort();
      this.vendorId = in.readUnsignedShort();
      this.proprietaryType = in.readUnsignedByte();
      this.proprietaryHeaderData = new byte[headerDataLength - 3];
      in.readFully(this.proprietaryHeaderData);
   }

   public int getVendorId() {
      return this.vendorId;
   }

   public int getProprietaryType() {
      return this.proprietaryType;
   }

   public byte[] getProprietaryHeaderData() {
      return this.proprietaryHeaderData;
   }

   @Override
   public String toString() {
      return super.toString()
         + "; vendor ID: "
         + this.vendorId
         + "; proprietary type: "
         + this.proprietaryType
         + "; data: "
         + Arrays.toString(this.proprietaryHeaderData);
   }
}
