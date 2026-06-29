package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class AtomicReadFileRequest extends BacnetConfirmedRequest {
   private BBacnetObjectIdentifier fileId;
   private int accessMethod;
   private int start;
   private long count;

   public AtomicReadFileRequest() {
      this(0, null, 0, 0L);
   }

   public AtomicReadFileRequest(BBacnetObjectIdentifier fileId) {
      this(0, fileId, 0, 0L);
   }

   public AtomicReadFileRequest(BBacnetObjectIdentifier fileId, int fileStartPosition, long requestedOctetCount) {
      this(0, fileId, fileStartPosition, requestedOctetCount);
   }

   public AtomicReadFileRequest(int accessMethod, BBacnetObjectIdentifier fileId, int start, long count) {
      super(6);
      this.accessMethod = accessMethod;
      this.fileId = fileId;
      this.start = start;
      this.count = count;
   }

   public BBacnetObjectIdentifier getFileId() {
      return this.fileId;
   }

   public void setFileId(BBacnetObjectIdentifier fileId) {
      this.fileId = fileId;
   }

   public boolean isStreamAccess() {
      return this.accessMethod == 0;
   }

   public int getFileStartPosition() {
      return this.start;
   }

   public long getRequestedOctetCount() {
      return this.count;
   }

   public void setFileStartPosition(int start) {
      this.start = start;
   }

   public void setRequestedOctetCount(long count) {
      this.count = count;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(this.fileId);
      outputStream.writeOpeningTag(this.accessMethod);
      outputStream.writeSignedInteger(this.start);
      outputStream.writeUnsignedInteger(this.count);
      outputStream.writeClosingTag(this.accessMethod);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.fileId = inputStream.readObjectIdentifier();
      inputStream.peekTag();
      if (inputStream.isOpeningTag(0)) {
         this.accessMethod = 0;
      } else {
         this.accessMethod = 1;
      }

      inputStream.skipTag();
      this.start = inputStream.readSignedInteger();
      this.count = inputStream.readUnsignedInteger();
      inputStream.skipTag();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("AtomicReadFileRequest: ");
      sb.append("\n  " + this.fileId);
      sb.append("\n  access method:" + this.accessMethod);
      sb.append("\n  " + this.start);
      sb.append("\n  " + this.count);
      return sb.toString();
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new AtomicReadFileAck(serviceChoice, inputStream);
   }
}
