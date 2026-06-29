package com.tridium.bacnet.services.error;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;

public class CreateObjectError extends SimpleError {
   public static final int ERROR_TYPE_TAG = 0;
   public static final int FIRST_FAILED_ELEMENT_NUMBER_TAG = 1;
   private long firstFailedElementNumber;

   public CreateObjectError(ErrorType error, long firstFailedElementNumber) {
      super(10, error);
      this.firstFailedElementNumber = firstFailedElementNumber;
   }

   public CreateObjectError(int errorChoice, byte[] encodedError) throws AsnException {
      super(errorChoice);
      AsnInputStream is = new AsnInputStream(encodedError);
      this.readEncoded(is);
   }

   public long getFirstFailedElementNumber() {
      return this.firstFailedElementNumber;
   }

   @Override
   public Object[] getErrorParameters() {
      return new Object[]{this.firstFailedElementNumber};
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(0);
      this.error.writeEncoded(outputStream);
      outputStream.writeClosingTag(0);
      outputStream.writeUnsignedInteger(1, this.firstFailedElementNumber);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      inputStream.skipTag();
      this.error = new NErrorType();
      this.error.readEncoded(inputStream);
      inputStream.skipTag();
      this.firstFailedElementNumber = inputStream.readUnsignedInteger(1);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("CreateObjectError: ");
      sb.append("\n  errorType:" + this.error.toString());
      sb.append("\n  firstFailedElementNumber:" + this.firstFailedElementNumber);
      return sb.toString();
   }

   @Override
   public String toUserString() {
      return this.error.toString();
   }
}
