package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetError;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.error.ConfirmedPrivateTransferError;
import javax.baja.bacnet.io.AsnException;
import javax.baja.nre.util.ByteArrayUtil;

public class ConfirmedPrivateTransferRequest extends BacnetConfirmedRequest {
   public static final int VENDOR_ID_TAG = 0;
   public static final int SERVICE_NUMBER_TAG = 1;
   public static final int SERVICE_PARAMETERS_TAG = 2;
   private long vendorId;
   private long serviceNumber;
   private byte[] serviceParameters;

   public ConfirmedPrivateTransferRequest() {
      this(-1L, -1L, null);
   }

   public ConfirmedPrivateTransferRequest(long vendorId, long serviceNumber) {
      this(vendorId, serviceNumber, null);
   }

   public ConfirmedPrivateTransferRequest(long vendorId, long serviceNumber, byte[] serviceParameters) {
      super(18);
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
      StringBuilder sb = new StringBuilder("ConfirmedPrivateTransferRequest: ");
      sb.append("\n  vId=" + this.vendorId);
      sb.append("\n  svc#=" + this.serviceNumber);
      if (this.serviceParameters != null && this.serviceParameters.length > 0) {
         sb.append(ByteArrayUtil.toHexString(this.serviceParameters));
      }

      return sb.toString();
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new ConfirmedPrivateTransferAck(serviceChoice, inputStream);
   }

   @Override
   protected BacnetError doParseError(int errorChoice, byte[] encodedError) throws AsnException {
      return new ConfirmedPrivateTransferError(errorChoice, encodedError);
   }
}
