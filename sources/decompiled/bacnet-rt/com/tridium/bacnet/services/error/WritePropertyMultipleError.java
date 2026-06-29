package com.tridium.bacnet.services.error;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.io.AsnException;

public class WritePropertyMultipleError extends SimpleError {
   public static final int ERROR_TYPE_TAG = 0;
   public static final int FIRST_FAILED_WRITE_ATTEMPT_TAG = 1;
   private BBacnetObjectPropertyReference firstFailedWriteAttempt;

   public WritePropertyMultipleError(NErrorType error, BBacnetObjectPropertyReference firstFailedWriteAttempt) {
      super(16, error);
      this.firstFailedWriteAttempt = (BBacnetObjectPropertyReference)firstFailedWriteAttempt.newCopy();
   }

   public WritePropertyMultipleError(NErrorType error, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      super(16, error);
      this.firstFailedWriteAttempt = new BBacnetObjectPropertyReference(objectId, propertyId, propertyArrayIndex);
   }

   public WritePropertyMultipleError(int errorChoice, byte[] encodedError) throws AsnException {
      super(errorChoice);
      AsnInputStream is = new AsnInputStream(encodedError);
      this.readEncoded(is);
   }

   public BBacnetObjectPropertyReference getFirstFailedWriteAttempt() {
      return this.firstFailedWriteAttempt;
   }

   @Override
   public Object[] getErrorParameters() {
      return new Object[]{this.firstFailedWriteAttempt};
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(0);
      this.error.writeEncoded(outputStream);
      outputStream.writeClosingTag(0);
      outputStream.writeOpeningTag(1);
      this.firstFailedWriteAttempt.writeAsn(outputStream);
      outputStream.writeClosingTag(1);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      inputStream.skipTag();
      this.error = new NErrorType();
      this.error.readEncoded(inputStream);
      inputStream.skipTag();
      inputStream.skipTag();
      this.firstFailedWriteAttempt = new BBacnetObjectPropertyReference();
      this.firstFailedWriteAttempt.readAsn(inputStream);
      inputStream.skipTag();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("WritePropertyMultipleError: ");
      sb.append("\n  errorType:" + this.error.toString());
      sb.append("\n  firstFailedWriteAttempt:" + this.firstFailedWriteAttempt.toString());
      return sb.toString();
   }

   @Override
   public String toUserString() {
      return this.error.toString();
   }
}
