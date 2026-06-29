package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetComplexAck;
import java.util.ArrayList;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.FileData;

public class AtomicReadFileAck extends BacnetComplexAck implements FileData {
   public static final int STREAM_ACCESS_TAG = 0;
   public static final int RECORD_ACCESS_TAG = 1;
   private boolean endOfFile;
   private int accessMethod;
   private int fileStartPosition;
   private byte[] fileData;
   private int fileStartRecord;
   private long returnedRecordCount;
   private BBacnetOctetString[] fileRecordData;

   public AtomicReadFileAck() {
      super(6);
   }

   public AtomicReadFileAck(boolean endOfFile, int fileStartPosition, byte[] fileData) {
      super(6);
      this.accessMethod = 0;
      this.endOfFile = endOfFile;
      this.fileStartPosition = fileStartPosition;
      this.fileData = new byte[fileData.length];
      System.arraycopy(fileData, 0, this.fileData, 0, fileData.length);
   }

   public AtomicReadFileAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.readEncoded(inputStream);
   }

   @Override
   public boolean isEndOfFile() {
      return this.endOfFile;
   }

   @Override
   public int getAccessMethod() {
      return this.accessMethod;
   }

   @Override
   public int getFileStart() {
      return this.fileStartPosition;
   }

   @Override
   public long getRecordCount() {
      if (!this.isStreamAccess()) {
         return this.returnedRecordCount;
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

   public boolean isStreamAccess() {
      return this.accessMethod == 0;
   }

   public void setFileData(byte[] fileData) {
      this.fileData = new byte[fileData.length];
      System.arraycopy(fileData, 0, this.fileData, 0, fileData.length);
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeBoolean(this.endOfFile);
      if (this.isStreamAccess()) {
         outputStream.writeOpeningTag(0);
         outputStream.writeSignedInteger(this.fileStartPosition);
         outputStream.writeOctetString(this.fileData);
         outputStream.writeClosingTag(0);
      } else {
         outputStream.writeOpeningTag(1);
         outputStream.writeSignedInteger(this.fileStartRecord);
         outputStream.writeUnsignedInteger(this.returnedRecordCount);

         for (int i = 0; i < this.fileRecordData.length; i++) {
            outputStream.writeOctetString(this.fileRecordData[i].getBytes());
         }

         outputStream.writeClosingTag(1);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.endOfFile = inputStream.readBoolean();
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(0)) {
         this.accessMethod = 0;
         inputStream.skipTag();
         this.fileStartPosition = inputStream.readSignedInteger();
         this.fileData = inputStream.readOctetString();
         inputStream.skipTag();
      } else {
         if (!inputStream.isOpeningTag(1)) {
            throw new AsnException("Invalid tag: " + tag);
         }

         this.accessMethod = 1;
         inputStream.skipTag();
         this.fileStartRecord = inputStream.readSignedInteger();
         this.returnedRecordCount = inputStream.readUnsignedInteger();
         ArrayList<BBacnetOctetString> v = new ArrayList<>();

         while (inputStream.peekTag() != -1 && !inputStream.isClosingTag(1)) {
            v.add(BBacnetOctetString.make(inputStream.readOctetString()));
         }

         this.fileRecordData = v.toArray(new BBacnetOctetString[0]);
         inputStream.skipTag();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("AtomicReadFileACK: ");
      sb.append("\n  " + this.endOfFile);
      sb.append("\n  access method:" + this.accessMethod);
      if (this.accessMethod == 0) {
         sb.append("\n  " + this.fileStartPosition);
         sb.append("\n  length=" + this.fileData.length);
      } else {
         sb.append("\n  " + this.fileStartRecord);
         sb.append("\n  records=" + this.returnedRecordCount);
      }

      return sb.toString();
   }
}
