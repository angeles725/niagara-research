package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import javax.baja.bacnet.io.AsnException;
import javax.baja.nre.util.ByteArrayUtil;

public class UnconfirmedPrivateTransferRequest extends BacnetUnconfirmedRequest {
   public static final int VENDOR_ID_TAG = 0;
   public static final int SERVICE_NUMBER_TAG = 1;
   public static final int SERVICE_PARAMETERS_TAG = 2;
   private long vendorId;
   private long serviceNumber;
   private byte[] serviceParameters;

   public UnconfirmedPrivateTransferRequest() {
      this(-1L, -1L, null);
   }

   public UnconfirmedPrivateTransferRequest(long vendorId, long serviceNumber) {
      this(vendorId, serviceNumber, null);
   }

   public UnconfirmedPrivateTransferRequest(long vendorId, long serviceNumber, byte[] serviceParameters) {
      super(4);
      this.vendorId = vendorId;
      this.serviceNumber = serviceNumber;
      this.serviceParameters = serviceParameters;
   }

   public long getVendorId() {
      return this.vendorId;
   }

   public void setVendorId(long vendorId) {
      this.vendorId = vendorId;
   }

   public long getServiceNumber() {
      return this.serviceNumber;
   }

   public void setServiceNumber(long serviceNumber) {
      this.serviceNumber = serviceNumber;
   }

   public byte[] getServiceParameters() {
      return this.serviceParameters;
   }

   public void setServiceParameters(byte[] serviceParameters) {
      this.serviceParameters = serviceParameters;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeUnsignedInteger(0, this.vendorId);
      outputStream.writeUnsignedInteger(1, this.serviceNumber);
      if (this.serviceParameters != null && this.serviceParameters.length > 0) {
         outputStream.writeEncodedValue(2, this.serviceParameters);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.vendorId = inputStream.readUnsignedInteger(0);
      this.serviceNumber = inputStream.readUnsignedInteger(1);
      inputStream.peekTag();
      if (inputStream.isOpeningTag(2)) {
         this.serviceParameters = inputStream.readEncodedValue(2);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("UnconfirmedPrivateTransferRequest: ");
      sb.append("\n  vId=" + this.vendorId);
      sb.append("\n  svc#=" + this.serviceNumber);
      if (this.serviceParameters != null && this.serviceParameters.length > 0) {
         sb.append(ByteArrayUtil.toHexString(this.serviceParameters));
      }

      return sb.toString();
   }
}
