package javax.baja.bacnet.config;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.datatypes.BReadFileConfig;
import com.tridium.bacnet.datatypes.BWriteFileConfig;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetFileAccessMethod;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.FileData;
import javax.baja.file.BIFile;
import javax.baja.file.BLocalFileStore;
import javax.baja.naming.BOrd;
import javax.baja.naming.NullOrdException;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Action;
import javax.baja.sys.BBlob;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BStruct;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.FILE)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.FILE, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "fileType",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.FILE_TYPE, ASN_CHARACTER_STRING)")}
   ), @NiagaraProperty(
      name = "fileSize",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.FILE_SIZE, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "modificationDate",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.MODIFICATION_DATE, ASN_CONSTRUCTED_DATA)")}
   ), @NiagaraProperty(
      name = "archive",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ARCHIVE, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "readOnly",
      type = "boolean",
      defaultValue = "true",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.READ_ONLY, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "fileAccessMethod",
      type = "BBacnetFileAccessMethod",
      defaultValue = "BBacnetFileAccessMethod.streamAccess",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.FILE_ACCESS_METHOD, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "fileOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 64,
      facets = {@Facet(
         name = "BFacets.TARGET_TYPE",
         value = "\"baja:IFile\""
      )}
   )})
@NiagaraActions({@NiagaraAction(
      name = "read",
      returnType = "BBlob"
   ), @NiagaraAction(
      name = "write",
      parameterType = "BBlob",
      defaultValue = "BBlob.DEFAULT"
   ), @NiagaraAction(
      name = "readFile",
      parameterType = "BStruct",
      defaultValue = "new BReadFileConfig()",
      flags = 4
   ), @NiagaraAction(
      name = "writeFile",
      parameterType = "BStruct",
      defaultValue = "new BWriteFileConfig()",
      flags = 4
   )})
public class BBacnetFile extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(10), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(10, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property fileType = newProperty(1, "", makeFacets(43, 7));
   public static final Property fileSize = newProperty(0, BBacnetUnsigned.make(0L), makeFacets(42, 2));
   public static final Property modificationDate = newProperty(1, new BBacnetDateTime(), makeFacets(71, -1));
   public static final Property archive = newProperty(0, false, makeFacets(13, 1));
   public static final Property readOnly = newProperty(1, true, makeFacets(99, 1));
   public static final Property fileAccessMethod = newProperty(1, BBacnetFileAccessMethod.streamAccess, makeFacets(41, 9));
   public static final Property fileOrd = newProperty(64, BOrd.NULL, BFacets.make("targetType", "baja:IFile"));
   public static final Action read = newAction(0, null);
   public static final Action write = newAction(0, BBlob.DEFAULT, null);
   public static final Action readFile = newAction(4, new BReadFileConfig(), null);
   public static final Action writeFile = newAction(4, new BWriteFileConfig(), null);
   public static final Type TYPE = Sys.loadType(BBacnetFile.class);
   private static final int ACK_HEADER_SIZE = 30;
   private BIFile file;

   public String getFileType() {
      return this.getString(fileType);
   }

   public void setFileType(String v) {
      this.setString(fileType, v, null);
   }

   public BBacnetUnsigned getFileSize() {
      return (BBacnetUnsigned)this.get(fileSize);
   }

   public void setFileSize(BBacnetUnsigned v) {
      this.set(fileSize, v, null);
   }

   public BBacnetDateTime getModificationDate() {
      return (BBacnetDateTime)this.get(modificationDate);
   }

   public void setModificationDate(BBacnetDateTime v) {
      this.set(modificationDate, v, null);
   }

   public boolean getArchive() {
      return this.getBoolean(archive);
   }

   public void setArchive(boolean v) {
      this.setBoolean(archive, v, null);
   }

   public boolean getReadOnly() {
      return this.getBoolean(readOnly);
   }

   public void setReadOnly(boolean v) {
      this.setBoolean(readOnly, v, null);
   }

   public BBacnetFileAccessMethod getFileAccessMethod() {
      return (BBacnetFileAccessMethod)this.get(fileAccessMethod);
   }

   public void setFileAccessMethod(BBacnetFileAccessMethod v) {
      this.set(fileAccessMethod, v, null);
   }

   public BOrd getFileOrd() {
      return (BOrd)this.get(fileOrd);
   }

   public void setFileOrd(BOrd v) {
      this.set(fileOrd, v, null);
   }

   public BBlob read() {
      return (BBlob)this.invoke(read, null, null);
   }

   public void write(BBlob parameter) {
      this.invoke(write, parameter, null);
   }

   public void readFile(BStruct parameter) {
      this.invoke(readFile, parameter, null);
   }

   public void writeFile(BStruct parameter) {
      this.invoke(writeFile, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.getFile();
   }

   @Override
   public void stopped() {
      this.file = null;
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(fileOrd)) {
            this.getFile();
         }
      }
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context)).append(" local: " + this.getFileOrd());
      return sb.toString();
   }

   public static byte[] readFile(BBacnetDevice device, BBacnetObjectIdentifier objectId) throws BacnetException {
      int fileSize = AsnUtil.fromAsnUnsignedInt(client().readProperty(device.getAddress(), objectId, 42));
      int fileAccessMethod = AsnUtil.fromAsnEnumerated(client().readProperty(device.getAddress(), objectId, 41));
      if (fileAccessMethod == 1) {
         return readFileDataStream(device, objectId, fileSize, 0, fileSize);
      } else {
         int recordCount = AsnUtil.fromAsnUnsignedInt(client().readProperty(device.getAddress(), objectId, 141));
         BBacnetOctetString[] recs = readFileDataRecord(device, objectId, fileSize, 0, recordCount);
         ByteArrayOutputStream os = new ByteArrayOutputStream(fileSize);

         for (int i = 0; i < recs.length; i++) {
            if (recs[i] != null) {
               os.write(recs[i].getBytes(), 0, recs[i].length());
            }
         }

         return os.toByteArray();
      }
   }

   public BBlob doRead() {
      try {
         byte[] fileData = readFile(this.device(), this.getObjectId());
         return BBlob.make(fileData);
      } catch (BacnetException var2) {
         log.log(Level.SEVERE, "Unable to read file contents for " + this.getObjectId() + " : " + var2, (Throwable)var2);
         throw new BajaRuntimeException(var2);
      }
   }

   public void doReadFile(BStruct arg) {
      BBacnetNetwork.bacnet().postAsync(new BBacnetFile.ReadFileReq((BReadFileConfig)arg, this));
   }

   public void doWriteFile(BStruct arg) {
      BBacnetNetwork.bacnet().postAsync(new BBacnetFile.WriteFileReq((BWriteFileConfig)arg, this));
   }

   public static void writeFile(BBacnetDevice device, BBacnetObjectIdentifier objectId, byte[] fileData) throws BacnetException {
      writeFileDataStream(device, objectId, 0, fileData);
   }

   public static void writeFile(BBacnetDevice device, BBacnetObjectIdentifier objectId, int count, BBacnetOctetString[] fileRecordData) throws BacnetException {
      writeFileDataRecord(device, objectId, 0, count, fileRecordData);
   }

   public void doWrite(BBlob arg) {
      byte[] fileData = arg.copyBytes();
      if (this.getFileAccessMethod() == BBacnetFileAccessMethod.streamAccess) {
         try {
            writeFileDataStream(this.device(), this.getObjectId(), 0, fileData);
         } catch (BacnetException var6) {
            log.log(Level.SEVERE, "Unable to write file contents for " + this.getObjectId() + " : " + var6, (Throwable)var6);
            throw new BajaRuntimeException(var6);
         }
      } else {
         Array<BBacnetOctetString> a = new Array(BBacnetOctetString.class);
         AsnInputStream asn = new AsnInputStream(fileData);

         try {
            while (asn.available() > 0) {
               a.add(asn.readBacnetOctetString());
            }

            BBacnetOctetString[] fileRecordData = (BBacnetOctetString[])a.trim();
            writeFileDataRecord(this.device(), this.getObjectId(), 0, fileRecordData.length, fileRecordData);
         } catch (AsnException var7) {
            log.severe("File data is not in array of encoded BACnetOctetStrings");
            throw new BajaRuntimeException(var7);
         } catch (BacnetException var8) {
            log.log(Level.SEVERE, "Unable to write file record contents for " + this.getObjectId() + ": " + var8, (Throwable)var8);
            throw new BajaRuntimeException(var8);
         }
      }
   }

   private BIFile getFile() {
      try {
         if (!fileOrd.isEquivalentToDefaultValue(this.getFileOrd())) {
            BObject o = this.getFileOrd().get(this);
            if (o instanceof BIFile) {
               this.file = (BIFile)o;
            } else {
               this.file = null;
            }
         }
      } catch (Exception var2) {
         log.log(Level.WARNING, "Unable to resolve file ord for " + this + ": " + this.getFileOrd(), (Throwable)var2);
         this.file = null;
      }

      return this.file;
   }

   private static BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   private static BBacnetOctetString[] readFileDataRecord(
      BBacnetDevice device, BBacnetObjectIdentifier objectId, int fileSize, int fileStartRecord, int requestedRecordCount
   ) throws BacnetException {
      if (!device.isServiceSupported("atomicReadFile")) {
         throw new UnsupportedOperationException(lex.getText("serviceNotSupported.atomicReadFile"));
      } else {
         BBacnetOctetString[] data = new BBacnetOctetString[requestedRecordCount];

         for (int i = 0; i < requestedRecordCount; i++) {
            FileData ack = client().atomicReadFile(device.getAddress(), objectId, 1, fileStartRecord + i, 1L);
            data[i] = ack.getFileRecordData()[0];
            if (ack.isEndOfFile()) {
               break;
            }
         }

         return data;
      }
   }

   private static byte[] readFileDataStream(
      BBacnetDevice device, BBacnetObjectIdentifier objectId, int fileSize, int fileStartPosition, int requestedOctetCount
   ) throws BacnetException {
      if (!device.isServiceSupported("atomicReadFile")) {
         throw new UnsupportedOperationException(lex.getText("serviceNotSupported.atomicReadFile"));
      } else {
         byte[] data = new byte[fileSize];
         if (fileSize < requestedOctetCount) {
            requestedOctetCount = fileSize;
         }

         int maxReturnableFileSize = device.getMaxAPDULengthAccepted();
         int myMax = BBacnetNetwork.localDevice().getMaxAPDULengthAccepted();
         if (myMax < maxReturnableFileSize) {
            maxReturnableFileSize = myMax;
         }

         maxReturnableFileSize -= 30;
         int start = fileStartPosition;
         int len = maxReturnableFileSize;
         int lastByte = fileStartPosition + requestedOctetCount;

         FileData ack;
         do {
            ack = client().atomicReadFile(device.getAddress(), objectId, 0, start, len);
            byte[] b = ack.getFileData();
            System.arraycopy(b, 0, data, start, b.length);
            start += len;
         } while (!ack.isEndOfFile() && start < lastByte);

         return data;
      }
   }

   private static void writeFileDataRecord(
      BBacnetDevice device, BBacnetObjectIdentifier objectId, int fileStartRecord, int recordCount, BBacnetOctetString[] fileRecordData
   ) throws BacnetException {
      if (!device.isServiceSupported("atomicWriteFile")) {
         throw new UnsupportedOperationException(lex.getText("serviceNotSupported.atomicWriteFile"));
      } else if (fileRecordData == null) {
         throw new IllegalArgumentException("fileRecordData is null!");
      } else {
         int writeCount = recordCount;
         if (fileStartRecord + recordCount > fileRecordData.length) {
            writeCount = fileRecordData.length - fileStartRecord;
         }

         int maxApdu = device.getMaxAPDULengthAccepted();
         int myMax = BBacnetNetwork.localDevice().getMaxAPDULengthAccepted();
         if (myMax < maxApdu) {
            maxApdu = myMax;
         }

         maxApdu -= 30;
         int recNdx = fileStartRecord;
         int start = fileStartRecord;

         do {
            int len = 0;
            Array<BBacnetOctetString> a = new Array(BBacnetOctetString.class);

            do {
               a.add(fileRecordData[recNdx]);
               len += fileRecordData[recNdx].length();
               recNdx++;
            } while (recNdx < writeCount && len + fileRecordData[recNdx].length() < maxApdu);

            BBacnetOctetString[] recData = (BBacnetOctetString[])a.trim();
            client().atomicWriteFileRecord(device.getAddress(), objectId, start, recData.length, recData);
            start = recNdx;
         } while (recNdx < writeCount);
      }
   }

   private static void writeFileDataStream(BBacnetDevice device, BBacnetObjectIdentifier objectId, int fileStartPosition, byte[] fileData) throws BacnetException {
      if (!device.isServiceSupported("atomicWriteFile")) {
         throw new UnsupportedOperationException(lex.getText("serviceNotSupported.atomicWriteFile"));
      } else {
         int writeLength = fileData.length;
         int maxApdu = device.getMaxAPDULengthAccepted();
         int myMax = BBacnetNetwork.localDevice().getMaxAPDULengthAccepted();
         if (myMax < maxApdu) {
            maxApdu = myMax;
         }

         maxApdu -= 30;
         int start = fileStartPosition;
         int len = maxApdu;

         do {
            int copylen = len;
            if (start + len > writeLength) {
               copylen = writeLength - start;
            }

            byte[] b = new byte[copylen];
            System.arraycopy(fileData, start, b, 0, copylen);
            client().atomicWriteFileStream(device.getAddress(), objectId, start, b);
            start += copylen;
         } while (start < writeLength);
      }
   }

   static class ReadFileReq implements Runnable {
      BReadFileConfig parms;
      BBacnetFile bacnetFile;

      ReadFileReq(BReadFileConfig arg, BBacnetFile f) {
         this.parms = arg;
         this.bacnetFile = f;
      }

      @Override
      public void run() {
         int start = this.parms.getStart();
         int count = this.parms.getCount();
         byte[] fileData = null;

         try {
            int fileSize = AsnUtil.fromAsnUnsignedInt(
               BBacnetFile.client().readProperty(this.bacnetFile.device().getAddress(), this.bacnetFile.getObjectId(), 42)
            );
            int fileAccessMethod = AsnUtil.fromAsnEnumerated(
               BBacnetFile.client().readProperty(this.bacnetFile.device().getAddress(), this.bacnetFile.getObjectId(), 41)
            );
            if (fileAccessMethod == 1) {
               fileData = BBacnetFile.readFileDataStream(this.bacnetFile.device(), this.bacnetFile.getObjectId(), fileSize, start, count);
            } else {
               BBacnetOctetString[] recs = BBacnetFile.readFileDataRecord(this.bacnetFile.device(), this.bacnetFile.getObjectId(), fileSize, start, count);
               ByteArrayOutputStream os = new ByteArrayOutputStream(fileSize);

               for (int i = 0; i < recs.length; i++) {
                  if (recs[i] != null) {
                     os.write(recs[i].getBytes(), 0, recs[i].length());
                  }
               }

               fileData = os.toByteArray();
            }
         } catch (BacnetException var19) {
            BBacnetObject.log.log(Level.SEVERE, "Unable to read file contents for " + this.bacnetFile.getObjectId() + " : " + var19, (Throwable)var19);
            throw new BajaRuntimeException(var19);
         }

         if (fileData != null) {
            RandomAccessFile out = null;

            try {
               if (this.bacnetFile.file == null) {
                  throw new NullOrdException("No local target file specified for BACnet File " + this.bacnetFile);
               }

               if (this.bacnetFile.file.isReadonly()) {
                  throw new IllegalStateException("Unable to write to file " + this.bacnetFile.getFileOrd());
               }

               File f = ((BLocalFileStore)this.bacnetFile.file.getStore()).getLocalFile();
               out = new RandomAccessFile(f, "rw");
               out.write(fileData);
            } catch (IOException var17) {
               BBacnetObject.log.log(Level.SEVERE, "IOException writing to local file " + this.bacnetFile.file, (Throwable)var17);
               throw new BajaRuntimeException(var17);
            } finally {
               if (out != null) {
                  try {
                     out.close();
                  } catch (IOException var16) {
                  }
               }
            }
         }
      }
   }

   class WriteFileReq implements Runnable {
      BWriteFileConfig parms;
      BBacnetFile bacnetFile;

      WriteFileReq(BWriteFileConfig arg, BBacnetFile f) {
         this.parms = arg;
         this.bacnetFile = f;
      }

      @Override
      public void run() {
         int remoteStart = this.parms.getRemoteStart();
         int localStart = this.parms.getLocalStart();
         byte[] fileData = null;
         RandomAccessFile src = null;

         try {
            if (this.bacnetFile.file == null) {
               throw new NullOrdException("No local source file specified for BACnet File " + this.bacnetFile);
            }

            File f = ((BLocalFileStore)this.bacnetFile.file.getStore()).getLocalFile();
            long flen = f.length() - localStart;
            if (flen > 2147483647L) {
               throw new BajaRuntimeException("Local file data length " + flen + " is too long to write to BACnet!");
            }

            int len = (int)flen;
            fileData = new byte[len];
            src = new RandomAccessFile(f, "r");
            src.seek(localStart);
            src.read(fileData, 0, len);
         } catch (IOException var20) {
            BBacnetObject.log.log(Level.SEVERE, "IOException reading from local file " + this.bacnetFile.file, (Throwable)var20);
            throw new BajaRuntimeException(var20);
         } finally {
            if (src != null) {
               try {
                  src.close();
               } catch (IOException var17) {
               }
            }
         }

         try {
            int fileAccessMethod = AsnUtil.fromAsnEnumerated(
               BBacnetFile.client().readProperty(this.bacnetFile.device().getAddress(), this.bacnetFile.getObjectId(), 41)
            );
            if (fileAccessMethod == 1) {
               BBacnetFile.writeFileDataStream(this.bacnetFile.device(), this.bacnetFile.getObjectId(), remoteStart, fileData);
            } else {
               Array<BBacnetOctetString> a = new Array(BBacnetOctetString.class);
               AsnInputStream asn = new AsnInputStream(fileData);

               try {
                  while (asn.available() > 0) {
                     a.add(asn.readBacnetOctetString());
                  }

                  BBacnetOctetString[] fileRecordData = (BBacnetOctetString[])a.trim();
                  BBacnetFile.writeFileDataRecord(BBacnetFile.this.device(), BBacnetFile.this.getObjectId(), remoteStart, fileRecordData.length, fileRecordData);
               } catch (AsnException var18) {
                  BBacnetObject.log.severe("File data is not in array of encoded BACnetOctetStrings");
                  throw new BajaRuntimeException(var18);
               }
            }
         } catch (BacnetException var19) {
            BBacnetObject.log.log(Level.SEVERE, "Unable to write file record contents for " + BBacnetFile.this.getObjectId() + ": " + var19, (Throwable)var19);
            throw new BajaRuntimeException(var19);
         }
      }
   }
}
