package com.tridium.bacnet.services.error;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BBlob;

public class ConfirmedPrivateTransferError extends SimpleError {
   public static final int ERROR_TYPE_TAG = 0;
   public static final int VENDOR_ID_TAG = 1;
   public static final int SERVICE_NUMBER_TAG = 2;
   public static final int ERROR_PARAMETERS_TAG = 3;
   private long vendorId;
   private long serviceNumber;
   private byte[] rawErrorParameters = new byte[0];

   public ConfirmedPrivateTransferError(ErrorType error, long vendorId, long serviceNumber) {
      super(18, error);
      this.vendorId = vendorId;
      this.serviceNumber = serviceNumber;
   }

   public ConfirmedPrivateTransferError(ErrorType error, long vendorId, long serviceNumber, byte[] raw) {
      super(18, error);
      this.vendorId = vendorId;
      this.serviceNumber = serviceNumber;
      this.rawErrorParameters = raw != null ? raw : new byte[0];
   }

   public ConfirmedPrivateTransferError(int errorChoice, byte[] encodedError) throws AsnException {
      super(errorChoice);
      AsnInputStream is = new AsnInputStream(encodedError);
      this.readEncoded(is);
   }

   public long getVendorId() {
      return this.vendorId;
   }

   public long getServiceNumber() {
      return this.serviceNumber;
   }

   public byte[] getRawErrorParameters() {
      return this.rawErrorParameters;
   }

   @Override
   public Object[] getErrorParameters() {
      return new Object[]{this.vendorId, this.serviceNumber, BBlob.make(this.rawErrorParameters)};
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(0);
      this.error.writeEncoded(outputStream);
      outputStream.writeClosingTag(0);
      outputStream.writeUnsignedInteger(1, this.vendorId);
      outputStream.writeUnsignedInteger(2, this.serviceNumber);
      if (this.rawErrorParameters != null) {
         outputStream.writeEncodedValue(3, this.rawErrorParameters);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      inputStream.skipTag();
      this.error = new NErrorType();
      this.error.readEncoded(inputStream);
      inputStream.skipTag();
      this.vendorId = inputStream.readUnsignedInteger(1);
      this.serviceNumber = inputStream.readUnsignedInteger(2);
      inputStream.peekTag();
      if (inputStream.isOpeningTag(3)) {
         this.rawErrorParameters = inputStream.readEncodedValue(3);
      } else {
         this.rawErrorParameters = new byte[0];
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("CreateObjectError: ");
      sb.append("\n  errorType:" + this.error.toString());
      sb.append("\n  vendorId:" + this.vendorId);
      sb.append("\n  serviceNumber:" + this.serviceNumber);
      if (this.rawErrorParameters != null && this.rawErrorParameters.length > 0) {
         sb.append("\n  errorParameters:" + ByteArrayUtil.toHexString(this.rawErrorParameters));
      }

      return sb.toString();
   }

   @Override
   public String toUserString() {
      return this.error.toString();
   }
}
