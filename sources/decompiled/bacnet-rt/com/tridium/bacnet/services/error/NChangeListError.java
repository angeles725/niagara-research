package com.tridium.bacnet.services.error;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ChangeListError;

public class NChangeListError extends SimpleError implements ChangeListError {
   private long firstFailedElementNumber;

   public NChangeListError(int errorChoice, NErrorType error, long firstFailedElementNumber) {
      super(errorChoice, error);
      this.firstFailedElementNumber = firstFailedElementNumber;
   }

   public NChangeListError(int errorChoice, byte[] encodedError) throws AsnException {
      super(errorChoice);
      AsnInputStream is = new AsnInputStream(encodedError);
      this.readEncoded(is);
   }

   @Override
   public long getFirstFailedElementNumber() {
      return this.firstFailedElementNumber;
   }

   @Override
   public Object[] getErrorParameters() {
      return new Object[]{this.firstFailedElementNumber};
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      this.error.writeEncoded(out);
      out.writeClosingTag(0);
      out.writeUnsignedInteger(1, this.firstFailedElementNumber);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      in.skipTag();
      this.error = new NErrorType();
      this.error.readEncoded(in);
      in.skipTag();
      this.firstFailedElementNumber = in.readUnsignedInteger(1);
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      this.writeAsn(outputStream);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readAsn(inputStream);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ChangeListError: ");
      sb.append("\n  errorType = " + this.error);
      sb.append("\n  firstFailedElementNumber = " + this.firstFailedElementNumber);
      return sb.toString();
   }

   @Override
   public String toUserString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.error.toString());
      sb.append(" ");
      sb.append(this.firstFailedElementNumber);
      return sb.toString();
   }
}
