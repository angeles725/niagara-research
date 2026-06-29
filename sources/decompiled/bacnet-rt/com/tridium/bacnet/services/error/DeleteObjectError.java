package com.tridium.bacnet.services.error;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;

public class DeleteObjectError extends SimpleError {
   public DeleteObjectError(ErrorType error) {
      super(11, error);
   }

   public DeleteObjectError(int errorChoice, byte[] encodedError) throws AsnException {
      super(errorChoice);
      AsnInputStream is = new AsnInputStream(encodedError);
      this.readEncoded(is);
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      this.error.writeEncoded(outputStream);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      inputStream.skipTag();
      this.error = new NErrorType();
      this.error.readEncoded(inputStream);
      inputStream.skipTag();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("DeleteObjectError: ");
      sb.append("\n  errorType:" + this.error.toString());
      return sb.toString();
   }

   @Override
   public String toUserString() {
      return this.error.toString();
   }
}
