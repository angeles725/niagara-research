package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.FileData;
import javax.baja.bacnet.io.RejectException;

public class AtomicWriteFileRequest extends BacnetConfirmedRequest implements FileData {
   public static final int STREAM_ACCESS_TAG = 0;
   public static final int RECORD_ACCESS_TAG = 1;
   private BBacnetObjectIdentifier fileId;
   private int accessMethod;
   private int start;
   private long count;
   private byte[] fileData;
   private BBacnetOctetString[] fileRecordData;

   public AtomicWriteFileRequest() {
      super(7);
   }

   public AtomicWriteFileRequest(BBacnetObjectIdentifier fileId, int fileStartPosition, byte[] fileData) {
      super(7);
      this.accessMethod = 0;
      this.fileId = fileId;
      this.start = fileStartPosition;
      if (fileData == null) {
         throw new IllegalArgumentException("fileData is null!");
      } else {
         this.fileData = fileData;
      }
   }

   public AtomicWriteFileRequest(BBacnetObjectIdentifier fileId, int fileStartRecord, long recordCount, BBacnetOctetString[] fileRecordData) {
      super(7);
      this.accessMethod = 1;
      this.fileId = fileId;
      this.start = fileStartRecord;
      this.count = recordCount;
      if (fileRecordData == null) {
         throw new IllegalArgumentException("fileRecordData is null!");
      } else if (this.start + this.count > fileRecordData.length) {
         throw new IllegalArgumentException(
            "start '" + this.start + "' and count '" + this.count + "' exceed end of record data (length " + fileRecordData.length
         );
      } else {
         this.fileRecordData = fileRecordData;
      }
   }

   @Override
   public boolean isEndOfFile() {
      return false;
   }

   @Override
   public int getAccessMethod() {
      return this.accessMethod;
   }

   @Override
   public int getFileStart() {
      return this.start;
   }

   @Override
   public long getRecordCount() {
      if (!this.isStreamAccess()) {
         return this.count;
      } else {
         throw new IllegalStateException("Not Record Access");
      }
   }

   @Override
   public byte[] getFileData() {
      if (this.isStreamAccess()) {
         return this.fileData;
      } else {
         throw new IllegalStateException("Not Stream Access");
      }
   }

   @Override
   public BBacnetOctetString[] getFileRecordData() {
      if (!this.isStreamAccess()) {
         return this.fileRecordData;
      } else {
         throw new IllegalStateException("Not Record Access");
      }
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

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(this.fileId);
      if (this.isStreamAccess()) {
         outputStream.writeOpeningTag(0);
         outputStream.writeSignedInteger(this.start);
         outputStream.writeOctetString(this.fileData);
         outputStream.writeClosingTag(0);
      } else {
         outputStream.writeOpeningTag(1);
         outputStream.writeSignedInteger(this.start);
         outputStream.writeUnsignedInteger(this.count);

         for (int i = 0; i < this.count; i++) {
            outputStream.writeOctetString(this.fileRecordData[i].getBytes());
         }

         outputStream.writeClosingTag(1);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.fileId = inputStream.readObjectIdentifier();
      inputStream.peekTag();
      if (inputStream.isOpeningTag(0)) {
         this.accessMethod = 0;
         inputStream.skipTag();
         this.start = inputStream.readSignedInteger();
         this.fileData = inputStream.readOctetString();
         inputStream.skipTag();
      } else {
         this.accessMethod = 1;
         inputStream.skipTag();
         this.start = inputStream.readSignedInteger();
         this.count = inputStream.readUnsignedInteger();
         if (this.count > 2147483647L) {
            throw new RejectException(6);
         }

         this.fileRecordData = new BBacnetOctetString[(int)this.count];

         for (int i = 0; i < this.count; i++) {
            this.fileRecordData[i] = inputStream.readBacnetOctetString();
         }

         inputStream.skipTag();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("AtomicWriteFileRequest: ");
      sb.append("\n  " + this.fileId);
      sb.append("\n  access method:" + this.accessMethod);
      sb.append("\n  start:" + this.start);
      if (this.isStreamAccess()) {
         sb.append("\n  data length=" + this.fileData.length);
      } else {
         sb.append("\n  record count=" + this.count);
      }

      return sb.toString();
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new AtomicWriteFileAck(serviceChoice, inputStream);
   }
}
