package javax.baja.bacnet.io;

import javax.baja.bacnet.datatypes.BBacnetOctetString;

public interface FileData {
   int STREAM_ACCESS = 0;
   int RECORD_ACCESS = 1;

   boolean isEndOfFile();

   int getAccessMethod();

   int getFileStart();

   long getRecordCount();

   byte[] getFileData();

   BBacnetOctetString[] getFileRecordData();
}
