package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.VmacUtil;
import com.tridium.bacnet.stack.link.sc.connection.ScDataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;

public final class ScProprietaryMessage extends AddressedMessage {
   private int vendorId;
   private int functionId;
   private byte[] data;

   ScProprietaryMessage() {
   }

   private ScProprietaryMessage(int messageId, int vendorId, int functionId, byte[] data) {
      super(messageId);
      this.vendorId = vendorId;
      this.functionId = functionId;
      this.data = data;
   }

   public static ScProprietaryMessage make(int messageId, int vendorId, int functionId) {
      return make(messageId, vendorId, functionId, null);
   }

   public static ScProprietaryMessage make(int messageId, int vendorId, int functionId, byte[] data) {
      checkArgs(messageId, vendorId, functionId);
      return new ScProprietaryMessage(messageId, vendorId, functionId, data);
   }

   private static void checkArgs(int messageId, int vendorId, int functionId) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      ScMessageUtil.checkUnsignedShort(vendorId, "vendorId");
      ScMessageUtil.checkUnsignedByte(functionId, "functionId");
   }

   @Override
   public int getFunction() {
      return 12;
   }

   public int getVendorId() {
      return this.vendorId;
   }

   public int getFunctionId() {
      return this.functionId;
   }

   public byte[] getData() {
      return this.data;
   }

   @Override
   public void setDestinationVmac(long vmac) {
      VmacUtil.checkIsDestinationVmac(vmac);
      this.destinationVmac = vmac;
   }

   public void setDestinationToBroadcastVmac() {
      this.setDestinationVmac(281474976710655L);
   }

   @Override
   public void encode(ScDataOutputStream out) throws IOException {
      super.encode(out);
      out.writeShort(this.vendorId);
      out.writeByte(this.functionId);
      if (this.data != null) {
         out.write(this.data);
      }
   }

   @Override
   protected void decodePayload(ByteBuffer in) throws ScReadMessageException {
      checkHasPayload(in);

      try {
         this.vendorId = in.readUnsignedShort();
         this.functionId = in.readUnsignedByte();
         int available = in.available();
         if (available != 0) {
            this.data = new byte[available];
            in.readFully(this.data);
         }
      } catch (IOException var3) {
         throw new ScReadMessageException("Message payload is incomplete", BBacnetErrorCode.messageIncomplete);
      }
   }

   @Override
   public String toString() {
      return super.toString()
         + "; vendor ID: "
         + this.vendorId
         + "; proprietary function ID: "
         + this.functionId
         + "; proprietary data: "
         + Arrays.toString(this.data);
   }
}
