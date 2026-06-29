package com.tridium.lonworks.file;

import com.tridium.lonworks.netmessages.FileXferData;
import com.tridium.lonworks.netmessages.FileXferResponse;
import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import java.io.ByteArrayOutputStream;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonListener;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonFileRequestEnum;
import javax.baja.lonworks.enums.BLonFileStatusEnum;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.londata.BLonBoolean;
import javax.baja.lonworks.londata.BLonEnum;
import javax.baja.lonworks.londata.BLonFilePos;
import javax.baja.lonworks.londata.BLonFileReq;
import javax.baja.lonworks.londata.BLonFileStatus;
import javax.baja.lonworks.londata.BLonInteger;
import javax.baja.lonworks.londata.BLonSimple;
import javax.baja.lonworks.util.LonFile;
import javax.baja.util.Queue;
import javax.baja.util.QueueFullException;

public class LonFileTransfer extends LonFile {
   private boolean opened = false;
   private BAddressEntry adrEntry = null;
   private static final int MAX_DATA_LENGTH = 32;
   private static final int RECEIVE_TIMEOUT = 30000;
   private static final int DEFAULT_TIMEOUT = 240000;
   private LonAddress sendAddr;
   public LonComm lonComm;
   private String devName = "";
   private boolean authenticate;
   private BLocalLonDevice localDevice;
   int reqNvSel;
   int posNvSel = -1;
   int statNvNdx;
   private boolean randomAccess = false;
   private int fileNum;
   private boolean modified = false;
   private LonFileTransfer.XferDataReceive receive = null;
   private LonFileTransfer.FileDirectory dir;
   private byte[] data;
   private int offset = 0;

   public LonFileTransfer(BLonDevice dev, BNetworkVariable reqNv, BNetworkVariable statNv, BNetworkVariable posNv) throws LonException {
      this.sendAddr = NmUtil.getSendAddress(dev);
      this.lonComm = dev.lonComm();
      this.devName = dev.getDisplayName(null);
      this.authenticate = dev.getDeviceData().getAuthenticate();
      this.reqNvSel = reqNv.getNvConfigData().getSelector();
      this.statNvNdx = statNv.getNvIndex();
      if (posNv != null) {
         this.posNvSel = posNv.getNvConfigData().getSelector();
      }

      this.localDevice = dev.lonNetwork().getLocalLonDevice();
      this.init();
   }

   public LonFileTransfer(LonAddress devAdr, LonComm lonComm, int reqNv, int statNv, int posNv) throws LonException {
      this.sendAddr = devAdr;
      this.lonComm = lonComm;
      this.devName = "xdev";
      this.authenticate = Neuron.isNMAuthSet(lonComm, devAdr, false, false);
      this.statNvNdx = statNv;
      boolean extended = NmUtil.isExtended(lonComm, devAdr, this.authenticate);
      this.reqNvSel = NmUtil.queryNvConfigData(lonComm, this.sendAddr, reqNv, this.authenticate, extended).getSelector();
      if (posNv >= 0) {
         this.posNvSel = NmUtil.queryNvConfigData(lonComm, this.sendAddr, posNv, this.authenticate, extended).getSelector();
      }

      this.localDevice = lonComm.lonNetwork().getLocalLonDevice();
      this.init();
   }

   private void init() throws LonException {
      this.dir = new LonFileTransfer.FileDirectory(this.sendAddr, this.lonComm, this.devName);
      this.receive = new LonFileTransfer.XferDataReceive();
   }

   @Override
   public boolean supportsRandomAccess() {
      return this.posNvSel != -1;
   }

   @Override
   public LonFile copy() {
      return new LonFileTransfer(this);
   }

   private LonFileTransfer(LonFileTransfer orig) {
      this.sendAddr = orig.sendAddr;
      this.lonComm = orig.lonComm;
      this.devName = orig.devName;
      this.authenticate = orig.authenticate;
      this.localDevice = orig.localDevice;
      this.reqNvSel = orig.reqNvSel;
      this.posNvSel = orig.posNvSel;
      this.statNvNdx = orig.statNvNdx;
      this.dir = orig.dir;
      this.randomAccess = orig.randomAccess;
      this.receive = new LonFileTransfer.XferDataReceive();
   }

   @Override
   public void open(int fileNum, boolean newFile, boolean allowRandom) throws LonException {
      if (!this.opened) {
         this.adrEntry = null;
         this.fileNum = fileNum;
         if (fileNum >= this.dir.numFiles) {
            throw new LonException("Invalid fileNum " + Integer.toString(fileNum));
         } else {
            try {
               this.offset = 0;
               this.modified = false;
               this.randomAccess = allowRandom && this.posNvSel != -1;
               if (newFile) {
                  this.data = new byte[this.dir.files[fileNum].size];
               } else if (!this.randomAccess) {
                  this.readFile();
               }
            } catch (LonException var7) {
               try {
                  this.fileRequest(BLonFileRequestEnum.closeFile);
               } catch (LonException var6) {
               }

               throw var7;
            }

            this.opened = true;
         }
      }
   }

   @Override
   public byte[] read(int offset, int length) throws LonException {
      this.offset = offset;
      return this.read(length);
   }

   @Override
   public byte[] read(int len) throws LonException {
      byte[] a = new byte[len];
      if (this.randomAccess) {
         this.readFileRandom(len);
         System.arraycopy(this.data, 0, a, 0, len);
      } else {
         if (len + this.offset > this.data.length) {
            throw new IndexOutOfBoundsException(
               "Invalid length for available data. {fnum=" + this.fileNum + ":flen=" + this.data.length + ":off=" + this.offset + ":len=" + len
            );
         }

         System.arraycopy(this.data, this.offset, a, 0, len);
      }

      this.offset += len;
      return a;
   }

   @Override
   public byte[] read() throws LonException {
      this.offset = 0;
      return this.read(this.dir.files[this.fileNum].size);
   }

   @Override
   public void write(byte[] a, int offset) throws LonException {
      this.offset = offset;
      this.write(a);
   }

   @Override
   public void write(byte[] a) throws LonException {
      if (this.randomAccess) {
         this.writeFileRandom(a);
      } else {
         if (a.length + this.offset > this.data.length) {
            throw new IndexOutOfBoundsException("Invalid length for available data.");
         }

         System.arraycopy(a, 0, this.data, this.offset, a.length);
         this.offset += a.length;
         this.modified = true;
      }
   }

   @Override
   public void close() throws LonException {
      this.flush();
      this.opened = false;
   }

   @Override
   public boolean isOpen() {
      return this.opened;
   }

   @Override
   public void flush() throws LonException {
      if (this.modified) {
         try {
            this.writeFile();
         } catch (LonException var2) {
            this.fileRequest(BLonFileRequestEnum.closeFile);
            this.opened = false;
            throw var2;
         }

         this.modified = false;
      }
   }

   @Override
   public String getDirectoryString() {
      StringBuilder sb = new StringBuilder();
      sb.append("File Directory Structure for " + this.devName);
      sb.append("\n   Number of files =  " + this.dir.numFiles);

      for (int i = 0; i < this.dir.numFiles; i++) {
         LonFileTransfer.FileDescriptor fd = this.dir.files[i];
         sb.append("\n\n   File #" + i);
         sb.append("\n   size " + fd.size);
         sb.append("\n   type " + fd.type);
         String info = fd.fileInfo;
         int len = info.indexOf(0);
         if (len > 0) {
            sb.append("\n   " + info.substring(0, len));
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

   private void readFile() throws LonException {
      synchronized (this.sendAddr) {
         this.receive.reset(this.dir.files[this.fileNum].size);

         try {
            this.fileRequest(BLonFileRequestEnum.openToSend, this.fileNum);
            this.doReadFile(BLonFileRequestEnum.openToSend);
         } finally {
            this.receive.endSession();
         }
      }
   }

   private void readFileRandom(int length) throws LonException {
      synchronized (this.sendAddr) {
         this.receive.reset(length);

         try {
            this.fileRequest(BLonFileRequestEnum.openToSendRa, this.fileNum);
            this.verifySeekWake(BLonFileRequestEnum.openToSendRa);
            this.filePosition(this.offset, length);
            this.doReadFile(null);
         } finally {
            this.receive.endSession();
         }
      }
   }

   private void doReadFile(BLonFileRequestEnum openReq) throws LonException {
      BLonFileStatusEnum status = this.getFileStatus();
      int statusId = status.getOrdinal();
      if (statusId == -1 && openReq != null) {
         this.retryOpen(openReq);
         statusId = this.getFileStatus().getOrdinal();
      }

      if (statusId != 0 && statusId != 4 && statusId != 11) {
         this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
         throw new LonException("Invalid lon file status for read: " + status.toString());
      } else {
         this.data = this.receive.getFileXferData(240000);
         this.closeFile();
      }
   }

   private void writeFile() throws LonException {
      synchronized (this.sendAddr) {
         this.fileRequest(BLonFileRequestEnum.openToReceive, this.fileNum);
         this.doWriteFile(BLonFileRequestEnum.openToReceive);
      }
   }

   private void writeFileRandom(byte[] a) throws LonException {
      synchronized (this.sendAddr) {
         this.fileRequest(BLonFileRequestEnum.openToReceiveRa, this.fileNum);
         this.verifySeekWake(BLonFileRequestEnum.openToReceiveRa);
         this.filePosition(this.offset, a.length);
         this.data = a;
         this.doWriteFile(null);
      }
   }

   private void doWriteFile(BLonFileRequestEnum openReq) throws LonException {
      this.verifyTransferUnderWay(openReq);
      int packet = 0;
      int maxPacket = this.data.length / 32;
      byte[] dat = new byte[32];

      while (packet <= maxPacket) {
         int window = packet / 6;
         int pack = packet % 6;
         int length = 32;
         int offset = packet * 32;
         boolean lastPacket = false;
         FileXferData xferData;
         if (packet == maxPacket) {
            length = this.data.length - packet * 32;
            byte[] lastDat = new byte[length];
            System.arraycopy(this.data, offset, lastDat, 0, length);
            xferData = new FileXferData(window & 15, pack, lastDat);
            lastPacket = true;
         } else {
            System.arraycopy(this.data, offset, dat, 0, length);
            xferData = new FileXferData(window & 15, pack, dat);
         }

         packet++;
         if (pack != 5 && !lastPacket) {
            this.lonComm.sendUnacknowledged(this.sendAddr, xferData);
         } else {
            try {
               FileXferResponse xferResp = (FileXferResponse)this.lonComm.sendRequest(this.sendAddr, xferData);
               if (xferResp.getMessageCode() != 6) {
                  packet = window * 6 + xferResp.getMessageCode();
               }
            } catch (LonException var12) {
               this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
               throw var12;
            }
         }
      }

      this.closeFile();
   }

   private void verifySeekWake(BLonFileRequestEnum openReq) throws LonException {
      boolean okay = false;
      boolean openRetried = false;
      int retries = 30;

      BLonFileStatusEnum status;
      label28:
      while (true) {
         if (okay) {
            return;
         }

         status = this.getFileStatus();
         switch (status.getOrdinal()) {
            case -1:
               if (openRetried || openReq == null) {
                  break label28;
               }

               this.retryOpen(openReq);
               openRetried = true;
               break;
            case 0:
            case 1:
            case 4:
               NmUtil.wait(70);
               break;
            case 2:
            case 3:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            default:
               break label28;
            case 11:
               okay = true;
         }

         if (retries-- == 0) {
            throw new LonException("File status never enters FS_SEEK_WAKE.");
         }
      }

      this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
      throw new LonException("Invalid lon file status for access: " + status.toString());
   }

   private void retryOpen(BLonFileRequestEnum openReq) throws LonException {
      this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
      NmUtil.wait(10);
      this.fileRequest(openReq, this.fileNum);
   }

   private void verifyTransferUnderWay(BLonFileRequestEnum openReq) throws LonException {
      int tries = 0;
      boolean openRetried = false;
      boolean okay = false;

      BLonFileStatusEnum status;
      label28:
      while (true) {
         if (okay) {
            return;
         }

         status = this.getFileStatus();
         switch (status.getOrdinal()) {
            case -1:
               if (openRetried || openReq == null) {
                  break label28;
               }

               this.retryOpen(openReq);
               openRetried = true;
               break;
            case 0:
            case 1:
            case 11:
               if (tries++ > 20) {
                  this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
                  throw new LonException("Requested file transfer never initiated by " + this.devName);
               }

               NmUtil.wait(70);
               break;
            case 2:
            case 3:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            default:
               break label28;
            case 4:
               okay = true;
         }
      }

      this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
      throw new LonException("Invalid lon file status for access: " + status.toString());
   }

   private void closeFile() throws LonException {
      boolean finished = false;
      boolean closed = false;

      while (!finished) {
         BLonFileStatusEnum status = this.getFileStatus();
         switch (status.getOrdinal()) {
            case 0:
               finished = true;
               break;
            case 1:
            case 4:
            case 11:
               if (!closed) {
                  this.fileRequest(BLonFileRequestEnum.closeFile);
               } else {
                  NmUtil.wait(200);
               }

               closed = true;
               break;
            default:
               this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
               throw new LonException("Unable to close lon file. Invalid status " + status.toString());
         }
      }

      if (!closed) {
         this.fileRequest(BLonFileRequestEnum.closeFile);
      }
   }

   private BLonFileStatusEnum getFileStatus() throws LonException {
      return (BLonFileStatusEnum)this.getStatus().getFileStatus().getEnum();
   }

   private BLonFileStatus getStatus() throws LonException {
      LonException ex = null;

      for (int i = 0; i < 4; i++) {
         try {
            BLonFileStatus fs = new BLonFileStatus();
            fs.fromNetBytes(NmUtil.fetchNv(this.lonComm, this.sendAddr, this.statNvNdx, this.authenticate));
            return fs;
         } catch (LonException var4) {
            ex = var4;
            NmUtil.wait(2000);
         }
      }

      if (ex != null) {
         throw ex;
      } else {
         return null;
      }
   }

   private void fileRequest(BLonFileRequestEnum request) throws LonException {
      this.fileRequest(request, this.fileNum);
   }

   private void fileRequest(BLonFileRequestEnum request, int file) throws LonException {
      BLonFileReq fileReq = new BLonFileReq();
      fileReq.setRequest(BLonEnum.make(request));
      fileReq.setIndex(BLonInteger.make(file));
      fileReq.setRecvTimeout(BLonInteger.make(30000));
      fileReq.setAddress(BLonSimple.make(this.getMyDeviceAddressEntry()));
      fileReq.setAuthenticate(BLonBoolean.make(this.authenticate));
      fileReq.setPriority(BLonBoolean.FALSE);
      NmUtil.setNvValue(this.sendAddr, this.lonComm, BLonNvDirection.input, this.reqNvSel, BLonServiceType.acked, this.authenticate, fileReq.toNetBytes());
   }

   private BAddressEntry getMyDeviceAddressEntry() {
      if (this.adrEntry == null) {
         BDeviceData dd = this.localDevice.getDeviceData();
         BSubnetNode sn = dd.getSubnetNodeId();
         byte[] a = new byte[]{1, (byte)((dd.getWorkingDomain() << 7) + sn.getNodeId()), 127, 7, (byte)sn.getSubnetId()};
         this.adrEntry = BAddressEntry.make(a);
      }

      return this.adrEntry;
   }

   private void filePosition(int offset, int len) throws LonException {
      BLonFilePos filePos = new BLonFilePos();
      filePos.setPointer(BLonInteger.make(offset));
      filePos.setLength(BLonInteger.make(len));
      NmUtil.setNvValue(this.sendAddr, this.lonComm, BLonNvDirection.input, this.posNvSel, BLonServiceType.acked, this.authenticate, filePos.toNetBytes());
   }

   private static class FileDescriptor {
      public int size;
      public int type;
      public String fileInfo;

      public FileDescriptor(BLonFileStatus status) throws LonException {
         this.size = status.getSize().getInt();
         this.type = status.getFileType().getInt();
         this.fileInfo = status.getFileInfo().getString();
      }
   }

   private class FileDirectory {
      public int numFiles;
      public LonFileTransfer.FileDescriptor[] files;

      private BLonFileStatus getDirectoryStatus(int fileNum) throws LonException {
         int cnt = 0;
         LonFileTransfer.this.fileRequest(BLonFileRequestEnum.directoryLookup, fileNum);

         BLonFileStatus status;
         for (status = LonFileTransfer.this.getStatus();
            status.getFileStatus().getEnum().getOrdinal() == 0 && cnt++ != 20;
            status = LonFileTransfer.this.getStatus()
         ) {
            NmUtil.wait(500);
         }

         if (status.getFileStatus().getEnum().getOrdinal() == 3) {
            throw new NoDirectoryException();
         } else if (status.getFileStatus().getEnum().getOrdinal() != 1) {
            throw new LonException("Unable to access lon file directory.");
         } else {
            return status;
         }
      }

      public FileDirectory(LonAddress sendAddr, LonComm lonComm, String devName) throws LonException {
         BLonFileStatus status = this.getDirectoryStatus(0);
         this.numFiles = status.getNumberOfFiles().getInt();
         if (this.numFiles <= 0) {
            LonFileTransfer.this.fileRequest(BLonFileRequestEnum.closeFile);
            throw new LonException("Device reports no files.");
         } else {
            this.files = new LonFileTransfer.FileDescriptor[this.numFiles];
            this.files[0] = new LonFileTransfer.FileDescriptor(status);

            for (int i = 1; i < this.numFiles; i++) {
               status = this.getDirectoryStatus(i);
               this.files[i] = new LonFileTransfer.FileDescriptor(status);
            }

            LonFileTransfer.this.fileRequest(BLonFileRequestEnum.closeFile);
         }
      }
   }

   private static class FileTransferOutStream extends ByteArrayOutputStream {
      int maxCount;

      private FileTransferOutStream() {
      }

      public void setLength(int len) {
         this.maxCount = len;
      }

      public boolean doForceWrite(byte[] a) {
         int len = this.maxCount - this.count;
         if (len > a.length) {
            len = a.length;
         }

         if (len > 0) {
            this.write(a, 0, len);
         }

         return len >= a.length;
      }
   }

   private class XferDataReceive implements Runnable, LonListener, NetMessages {
      private int expectedWindow = 0;
      private int expectedPacket = 0;
      private Queue rcvQueue;
      private Thread receiveThread;
      private boolean done = true;
      private boolean dataComplete = false;
      private LonFileTransfer.FileTransferOutStream rcvData;
      private LonException error = null;

      public XferDataReceive() {
         this.rcvQueue = new Queue(16);
         this.rcvData = new LonFileTransfer.FileTransferOutStream();
      }

      @Override
      public void run() {
         boolean firstPass = true;
         this.expectedWindow = 0;
         this.expectedPacket = 0;
         this.rcvData.reset();
         LonFileTransfer.this.lonComm.registerLonListener(this, 62, null, FileXferData.class);

         while (!this.done) {
            try {
               FileXferData xferData = null;
               xferData = (FileXferData)this.rcvQueue.dequeue(firstPass ? 10000 : 3000);
               firstPass = false;
               if (this.done) {
                  break;
               }

               if (xferData == null) {
                  throw new RuntimeException("Timeout waiting for next transfer data.");
               }

               byte[] data = xferData.getData();
               int thisPacket = xferData.getPacket();
               int thisWindow = xferData.getWindow();
               if (thisPacket == this.expectedPacket && thisWindow == this.expectedWindow) {
                  if (!this.rcvData.doForceWrite(data)) {
                     this.endTransfer();
                     System.out.println("Device transferred too much data.");
                  }

                  this.expectedPacket++;
               } else {
                  System.out.println("\n*****Expected win " + this.expectedWindow + " pack = " + this.expectedPacket + "*****");
               }

               if (xferData.isRequest()) {
                  LonFileTransfer.this.lonComm.sendResponse(xferData, new FileXferResponse(this.expectedPacket));
                  if (this.expectedPacket == 6) {
                     this.expectedWindow = thisWindow + 1 & 15;
                     this.expectedPacket = 0;
                  }
               }

               if (data.length < 32) {
                  this.endTransfer();
               }
            } catch (Throwable var6) {
               this.error = new LonException("Lon File Transfer interrupted", var6);
               this.endTransfer();
               this.done = true;
            }
         }

         LonFileTransfer.this.lonComm.unregisterLonListener(this, 62, null);
         this.exitingThread();
      }

      @Override
      public void receiveLonMessage(LonMessage rcvMessage) {
         try {
            this.rcvQueue.enqueue(rcvMessage);
         } catch (QueueFullException var3) {
            this.error = new LonException("Queue full. File transfer data possibly lost.");
         }
      }

      public synchronized void reset(int length) {
         this.rcvData.setLength(length);
         this.error = null;
         if (this.done) {
            this.done = false;
            this.dataComplete = false;
            this.receiveThread = new Thread(
               LonFileTransfer.this.receive, LonFileTransfer.this.lonComm.lonNetwork().getLogName() + ".FileXfer\\" + LonFileTransfer.this.devName
            );
            this.receiveThread.start();
         }
      }

      public synchronized byte[] getFileXferData(int timeout) throws LonException {
         if (!this.dataComplete) {
            try {
               this.wait(timeout);
            } catch (Exception var3) {
            }
         }

         if (this.error != null) {
            LonFileTransfer.this.fileRequest(BLonFileRequestEnum.closeDeleteFile);
            throw this.error;
         } else {
            return this.rcvData.toByteArray();
         }
      }

      private synchronized void endTransfer() {
         this.dataComplete = true;
         this.notify();
      }

      public synchronized void endSession() {
         this.done = true;
         this.rcvQueue.clear();
         if (this.receiveThread.isAlive()) {
            try {
               this.wait(2000L);
            } catch (Exception var2) {
               System.out.println("timed out waiting for receiveThread to complete");
            }
         }
      }

      private synchronized void exitingThread() {
         this.notify();
      }

      public boolean isDone() {
         return this.done;
      }
   }
}
