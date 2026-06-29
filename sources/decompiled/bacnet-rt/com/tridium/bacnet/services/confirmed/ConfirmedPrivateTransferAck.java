package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetComplexAck;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.io.AsnException;
import javax.baja.nre.util.ByteArrayUtil;

public class ConfirmedPrivateTransferAck extends BacnetComplexAck {
   public static final int VENDOR_ID_TAG = 0;
   public static final int SERVICE_NUMBER_TAG = 1;
   public static final int RESULT_BLOCK_TAG = 2;
   private long vendorId;
   private long serviceNumber;
   private byte[] resultBlock;

   public ConfirmedPrivateTransferAck() {
      super(18);
   }

   public ConfirmedPrivateTransferAck(long vendorId, long serviceNumber) {
      this(vendorId, serviceNumber, null);
   }

   public ConfirmedPrivateTransferAck(long vendorId, long serviceNumber, byte[] resultBlock) {
      super(18);
      this.vendorId = vendorId;
      this.serviceNumber = serviceNumber;
      this.resultBlock = resultBlock;
   }

   public ConfirmedPrivateTransferAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.readEncoded(inputStream);
   }

   public byte[] getResultBlock() {
      return this.resultBlock;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeUnsignedInteger(0, this.vendorId);
      outputStream.writeUnsignedInteger(1, this.serviceNumber);
      if (this.resultBlock != null && this.resultBlock.length > 0) {
         outputStream.writeEncodedValue(2, this.resultBlock);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.vendorId = inputStream.readUnsignedInteger(0);
      this.serviceNumber = inputStream.readUnsignedInteger(1);
      inputStream.peekTag();
      if (BBacnetNetwork.bacnet().setAndGetPrivateTransferResultBlockFlag()) {
         if (inputStream.isOpeningTag(2)) {
            this.resultBlock = inputStream.readEncodedValue(2);
         }
      } else {
         AsnOutputStream asnOutputStream = new AsnOutputStream();

         while (inputStream.available() > 0) {
            asnOutputStream.write(inputStream.read());
         }

         this.resultBlock = asnOutputStream.toByteArray();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ConfirmedPrivateTransferACK: ");
      sb.append("\n  vId=" + this.vendorId);
      sb.append("\n  svc#=" + this.serviceNumber);
      if (this.resultBlock != null && this.resultBlock.length > 0) {
         sb.append(ByteArrayUtil.toHexString(this.resultBlock));
      }

      return sb.toString();
   }
}
