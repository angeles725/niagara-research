package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetComplexAck;
import javax.baja.bacnet.io.AsnException;

public class AtomicWriteFileAck extends BacnetComplexAck {
   public static final int STREAM_ACCESS_TAG = 0;
   public static final int RECORD_ACCESS_TAG = 1;
   private int accessMethod;
   private int start;

   public AtomicWriteFileAck() {
      super(7);
   }

   public AtomicWriteFileAck(int accessMethod, int start) {
      super(7);
      this.accessMethod = accessMethod;
      this.start = start;
   }

   public AtomicWriteFileAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.readEncoded(inputStream);
   }

   public int getFileStart() {
      return this.start;
   }

   public boolean isStreamAccess() {
      return this.accessMethod == 0;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeSignedInteger(this.accessMethod, this.start);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      inputStream.peekTag();
      if (inputStream.isValueTag(0)) {
         this.accessMethod = 0;
      } else {
         this.accessMethod = 1;
      }

      this.start = inputStream.readSignedInteger(this.accessMethod);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("AtomicWriteFileACK: ");
      sb.append("\n  access method:" + this.accessMethod);
      sb.append("\n  " + this.start);
      return sb.toString();
   }
}
