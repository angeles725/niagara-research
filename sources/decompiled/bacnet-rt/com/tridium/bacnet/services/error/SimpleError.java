package com.tridium.bacnet.services.error;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetError;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;

public class SimpleError extends BacnetError {
   protected ErrorType error;

   public SimpleError(int errorChoice) {
      super(errorChoice);
   }

   public SimpleError(int errorChoice, ErrorType error) {
      super(errorChoice);
      this.error = error;
   }

   public SimpleError(int errorChoice, byte[] encodedError) throws AsnException {
      super(errorChoice);
      AsnInputStream is = new AsnInputStream(encodedError);
      this.readEncoded(is);
   }

   @Override
   public ErrorType getError() {
      return this.error;
   }

   @Override
   public Object[] getErrorParameters() {
      return null;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      this.error.writeEncoded(outputStream);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.error = new NErrorType();
      this.error.readEncoded(inputStream);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("BacnetError: ");
      sb.append("\n  serviceChoice:" + this.getServiceChoice());

      try {
         sb.append(' ').append(BacnetConfirmedServiceChoice.TAGS[this.getServiceChoice()]);
      } catch (Exception var3) {
         sb.append(" ???");
      }

      sb.append("\n  Error: " + this.error.toString());
      return sb.toString();
   }

   @Override
   public String toUserString() {
      return this.error.toString();
   }
}
