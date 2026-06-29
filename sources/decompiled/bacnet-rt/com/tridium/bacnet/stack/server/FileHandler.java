package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.confirmed.AtomicReadFileAck;
import com.tridium.bacnet.services.confirmed.AtomicReadFileRequest;
import com.tridium.bacnet.services.confirmed.AtomicWriteFileAck;
import com.tridium.bacnet.services.confirmed.AtomicWriteFileRequest;
import com.tridium.bacnet.services.error.SimpleError;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetBackupState;
import javax.baja.bacnet.export.BBacnetFileDescriptor;
import javax.baja.bacnet.export.BLocalBacnetDevice;

public class FileHandler implements ServiceHandler, BacnetConfirmedServiceChoice {
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private final BBacnetServerLayer server;

   FileHandler(BBacnetServerLayer server) {
      this.server = server;
   }

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      switch (serviceChoice) {
         case 6:
            return this.processAtomicReadFileRequest((AtomicReadFileRequest)request);
         case 7:
            return this.processAtomicWriteFileRequest((AtomicWriteFileRequest)request);
         default:
            logger.info("FileHandler.receiveRequest:Unknown request! " + request);
            return null;
      }
   }

   private BacnetServicePrimitive processAtomicReadFileRequest(AtomicReadFileRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier fileId = request.getFileId();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("FileHandler.AtomicReadFileRequest received: " + request);
      }

      BBacnetFileDescriptor file = (BBacnetFileDescriptor)local.lookupBacnetObject(fileId);
      if (file == null) {
         return new SimpleError(6, new NErrorType(1, 31));
      } else {
         if (fileId.equals(this.server.getBackupRestoreFileId()) && local.getBackupAndRestoreState() != BBacnetBackupState.idle) {
            this.server.updateLastBackupRestoreCommTime();
         }

         if (!request.isStreamAccess()) {
            return new SimpleError(6, new NErrorType(5, 10));
         } else {
            int start = request.getFileStartPosition();
            if (start >= 0 && start <= file.getFileSize()) {
               long count = request.getRequestedOctetCount();
               if (count > 2147483647L) {
                  return new BacnetReject(6);
               } else {
                  byte[] fileData = null;
                  boolean eof = false;

                  try {
                     synchronized (file) {
                        fileData = file.read(request.getFileStartPosition(), (int)count);
                        eof = file.isEOF();
                     }
                  } catch (IOException var13) {
                     logger.log(
                        Level.INFO,
                        "IOException attempting to read file " + file.getFileOrd() + " in response to Bacnet AtomicReadFileRequest!",
                        (Throwable)var13
                     );
                  }

                  return (BacnetServicePrimitive)(fileData == null
                     ? new SimpleError(6, new NErrorType(5, 5))
                     : new AtomicReadFileAck(eof, request.getFileStartPosition(), fileData));
               }
            } else {
               return new SimpleError(6, new NErrorType(5, 11));
            }
         }
      }
   }

   private BacnetServicePrimitive processAtomicWriteFileRequest(AtomicWriteFileRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier fileId = request.getFileId();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("FileHandler.AtomicWriteFileRequest received: " + request);
      }

      BBacnetFileDescriptor file = (BBacnetFileDescriptor)local.lookupBacnetObject(fileId);
      if (file == null) {
         return new SimpleError(7, new NErrorType(1, 31));
      } else {
         if (fileId.equals(this.server.getBackupRestoreFileId()) && local.getBackupAndRestoreState() != BBacnetBackupState.idle) {
            this.server.updateLastBackupRestoreCommTime();
         }

         if (!request.isStreamAccess()) {
            return new SimpleError(7, new NErrorType(5, 10));
         } else {
            int start = request.getFileStart();
            if (start >= 0 && start <= file.getFileSize()) {
               int writeResult = -1;

               try {
                  synchronized (file) {
                     writeResult = file.write(request.getFileStart(), request.getFileData());
                  }

                  return (BacnetServicePrimitive)(writeResult < 0
                     ? new AtomicWriteFileAck(0, request.getFileStart())
                     : new SimpleError(7, new NErrorType(5, writeResult)));
               } catch (IOException var10) {
                  logger.log(
                     Level.INFO,
                     "IOException attempting to write file " + file.getFileOrd() + " in response to Bacnet AtomicWriteFileRequest!",
                     (Throwable)var10
                  );
                  return new SimpleError(7, new NErrorType(5, 5));
               }
            } else {
               return new SimpleError(6, new NErrorType(5, 11));
            }
         }
      }
   }
}
