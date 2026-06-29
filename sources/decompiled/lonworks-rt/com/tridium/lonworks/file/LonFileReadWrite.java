package com.tridium.lonworks.file;

import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.lonworks.util.LonFile;

public class LonFileReadWrite extends LonFile implements NetMessages {
   private boolean opened = false;
   private static int SZ_FILE_DIR_HEADER = 2;
   private static int SZ_FILE_DESCRIPTOR_10 = 5;
   private static int SZ_FILE_DESCRIPTOR_20 = 6;
   private static final int MAX_DATA_SIZE = 16;
   private LonAddress sendAddr;
   private LonComm lonComm;
   private String devName;
   private boolean auth;
   private int filePtr;
   private LonFileReadWrite.FileDirectory dir;
   private int size;
   private int offset = 0;

   public LonFileReadWrite(BLonDevice dev, BNetworkVariable dirNv) throws LonException {
      this.sendAddr = NmUtil.getSendAddress(dev);
      this.lonComm = dev.lonComm();
      this.devName = dev.getDisplayName(null);
      this.auth = dev.getDeviceData().getAuthenticate();
      dirNv.doForceRead();
      this.filePtr = (int)((BLonPrimitive)dirNv.getData().get("address")).getDataAsDouble();
      this.dir = new LonFileReadWrite.FileDirectory(this.lonComm, this.sendAddr, this.filePtr);
   }

   public LonFileReadWrite(LonAddress devAdr, LonComm lonComm, int dirNv) throws LonException {
      this.sendAddr = devAdr;
      this.lonComm = lonComm;
      this.devName = "xdev";
      this.auth = Neuron.isNMAuthSet(lonComm, devAdr, false, false);
      byte[] a = NmUtil.fetchNv(lonComm, this.sendAddr, dirNv, this.auth);
      LonInputStream in = new LonInputStream(a);
      this.filePtr = in.readUnsigned16();
      this.dir = new LonFileReadWrite.FileDirectory(lonComm, this.sendAddr, this.filePtr);
   }

   @Override
   public LonFile copy() {
      return new LonFileReadWrite(this);
   }

   private LonFileReadWrite(LonFileReadWrite orig) {
      this.sendAddr = orig.sendAddr;
      this.lonComm = orig.lonComm;
      this.devName = orig.devName;
      this.filePtr = orig.filePtr;
      this.auth = orig.auth;
      this.dir = orig.dir;
   }

   @Override
   public void open(int fileNum, boolean newFile, boolean allowRandom) throws LonException {
      LonFileReadWrite.FileDescriptor file = this.dir.files[fileNum];
      this.filePtr = file.filePtr;
      this.size = file.size;
      this.opened = true;
   }

   @Override
   public byte[] read(int offset, int length) throws LonException {
      this.offset = offset;
      return this.read(length);
   }

   @Override
   public byte[] read() throws LonException {
      this.offset = 0;
      return this.read(this.size);
   }

   @Override
   public byte[] read(int length) throws LonException {
      if (length > this.size - this.offset) {
         throw new LonException("Attempt to access beyond end of file.  size=" + this.size + " offset=" + this.offset + "  length=" + length);
      } else {
         byte[] file = Neuron.readMemory(this.lonComm, 0, this.sendAddr, this.filePtr + this.offset, length, this.auth);
         this.offset += length;
         return file;
      }
   }

   @Override
   public void write(byte[] data, int offset) throws LonException {
      this.offset = offset;
      this.write(data);
   }

   @Override
   public void write(byte[] data) throws LonException {
      Neuron.writeMemory(this.lonComm, 0, this.sendAddr, this.filePtr + this.offset, 0, data, this.auth, false);
   }

   @Override
   public void close() throws LonException {
      this.opened = false;
   }

   @Override
   public boolean isOpen() {
      return this.opened;
   }

   @Override
   public void flush() throws LonException {
   }

   @Override
   public String getDirectoryString() {
      StringBuilder sb = new StringBuilder();
      sb.append("File Directory Structure for " + this.devName);
      sb.append("\n   File Version = " + ((this.dir.version & 240) >> 4) + "." + (this.dir.version & 15));
      sb.append("\n   Number of files =  " + this.dir.numFiles);

      for (int i = 0; i < this.dir.numFiles; i++) {
         LonFileReadWrite.FileDescriptor fd = this.dir.files[i];
         sb.append("\n\n   File #" + i);
         sb.append("\n   size " + fd.size);
         sb.append("\n   type " + fd.type);
         switch (fd.type) {
            case 1:
               sb.append(" (Configuration Parameter Value File)");
               break;
            case 2:
               sb.append(" (Configuration Parameter Template File)");
         }
      }

      return sb.toString();
   }

   @Override
   public int findFileNum(int type) {
      return this.findFileNum(type, -1);
   }

   @Override
   public int findFileNum(int type, int lastFile) {
      if (this.dir == null) {
         return -1;
      } else {
         for (int fileNum = lastFile + 1; fileNum < this.dir.numFiles; fileNum++) {
            if (this.dir.files[fileNum].type == type) {
               return fileNum;
            }
         }

         return -1;
      }
   }

   private boolean validateFileNum(int fileNum) {
      return this.dir == null ? false : fileNum < this.dir.numFiles;
   }

   private static class FileDescriptor {
      public int size;
      public int type;
      public int filePtr;

      public FileDescriptor(LonInputStream in, int version) throws LonException {
         this.size = in.readUnsigned16();
         if (version >= 32) {
            this.type = in.readUnsigned16();
         } else {
            this.type = in.read();
         }

         this.filePtr = in.readUnsigned16();
      }
   }

   private class FileDirectory {
      public int version;
      public int numFiles;
      public LonFileReadWrite.FileDescriptor[] files;

      public FileDirectory(LonComm lonComm, LonAddress sendAddr, int filePtr) throws LonException {
         byte[] b = Neuron.readMemory(lonComm, 0, sendAddr, filePtr, LonFileReadWrite.SZ_FILE_DIR_HEADER, LonFileReadWrite.this.auth);
         LonInputStream in = new LonInputStream(b);
         this.version = in.read();
         this.numFiles = in.read();
         int sizeDescriptor = this.version >= 32 ? LonFileReadWrite.SZ_FILE_DESCRIPTOR_20 : LonFileReadWrite.SZ_FILE_DESCRIPTOR_10;
         this.files = new LonFileReadWrite.FileDescriptor[this.numFiles];

         for (int i = 0; i < this.numFiles; i++) {
            int offset = filePtr + LonFileReadWrite.SZ_FILE_DIR_HEADER + sizeDescriptor * i;
            b = Neuron.readMemory(lonComm, 0, sendAddr, offset, sizeDescriptor, LonFileReadWrite.this.auth);
            in = new LonInputStream(b);
            this.files[i] = new LonFileReadWrite.FileDescriptor(in, this.version);
         }
      }
   }
}
