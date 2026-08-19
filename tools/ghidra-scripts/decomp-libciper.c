/* rsdd ghidra C export
   program : libciper.so
   filter  : masterslave|SylkFile|buildFileBlock|crc_ccitt|receiveSerial|write_serial|handleTerminal|handlePublicVariable|serial_connect|open_port
*/

/* ---- receiveSerialSync @ 00012b0c ---- */

int receiveSerialSync(int fd,int timeoutSec)

{
  int iVar1;
  
  iVar1 = receiveSerialSync(fd,timeoutSec);
  return iVar1;
}



/* ---- handleTerminalPropertyMessage @ 00012b6c ---- */

void handleTerminalPropertyMessage(int status)

{
  handleTerminalPropertyMessage(status);
  return;
}



/* ---- buildFileBlockCRCRecord @ 00012b9c ---- */

int buildFileBlockCRCRecord
              (UBYTE ioCommand,UINT16 fileId,UBYTE addr,UINT32 fileLength,UINT32 fileCRC32)

{
  int iVar1;
  
  iVar1 = buildFileBlockCRCRecord(ioCommand,fileId,addr,fileLength,fileCRC32);
  return iVar1;
}



/* ---- buildFileBlockRecord @ 00012c08 ---- */

int buildFileBlockRecord
              (UBYTE ioCommand,UINT16 fileId,UBYTE addr,UBYTE sectorNum,UBYTE totalSectors,
              UBYTE blockNum,UBYTE totalBlocks,UBYTE *blockBuffer,UINT16 bufferSize)

{
  int iVar1;
  
  iVar1 = buildFileBlockRecord
                    (ioCommand,fileId,addr,sectorNum,totalSectors,blockNum,totalBlocks,blockBuffer,
                     bufferSize);
  return iVar1;
}



/* ---- masterslaveReadSylkFileData @ 00012c68 ---- */

SylkFileDataRecord *
masterslaveReadSylkFileData
          (SylkFileDataRecord *__return_storage_ptr__,UINT16 fileId,UBYTE addr,UINT32 fileposition,
          UINT32 datalength)

{
  SylkFileDataRecord *pSVar1;
  
  pSVar1 = masterslaveReadSylkFileData(__return_storage_ptr__,fileId,addr,fileposition,datalength);
  return pSVar1;
}



/* ---- crc_ccitt_add @ 00012c80 ---- */

uint16_t crc_ccitt_add(uint16_t crc,uchar *ptr,size_t num_bytes)

{
  uint16_t uVar1;
  
  uVar1 = crc_ccitt_add(crc,ptr,num_bytes);
  return uVar1;
}



/* ---- write_serial_record @ 00012cf8 ---- */

int write_serial_record(int fd,uchar *rec,int recSize)

{
  int iVar1;
  
  iVar1 = write_serial_record(fd,rec,recSize);
  return iVar1;
}



/* ---- serial_connect @ 00012db8 ---- */

int serial_connect(char *tty,int *fd)

{
  int iVar1;
  
  iVar1 = serial_connect(tty,fd);
  return iVar1;
}



/* ---- masterslavefilestatus @ 00012de8 ---- */

int masterslavefilestatus
              (UINT32 fileId,UINT16 filemode,UINT16 addr,UINT16 fileposition,UINT16 requesttype,
              UINT16 length)

{
  int iVar1;
  
  iVar1 = masterslavefilestatus(fileId,filemode,addr,fileposition,requesttype,length);
  return iVar1;
}



/* ---- masterslavefileopenv2 @ 00012e00 ---- */

int masterslavefileopenv2
              (UINT32 fileId,UINT16 filemode,UINT16 addr,UINT16 fileposition,UINT16 requesttype,
              UINT16 length)

{
  int iVar1;
  
  iVar1 = masterslavefileopenv2(fileId,filemode,addr,fileposition,requesttype,length);
  return iVar1;
}



/* ---- handlePublicVariableMessage @ 00012e3c ---- */

/* WARNING: Unknown calling convention -- yet parameter storage is locked */

void handlePublicVariableMessage(void)

{
  handlePublicVariableMessage();
  return;
}



/* ---- masterslavefileclosev2 @ 00012e60 ---- */

int masterslavefileclosev2
              (UINT32 fileId,UINT16 filemode,UINT16 addr,UINT16 fileposition,UINT16 requesttype,
              UINT16 length)

{
  int iVar1;
  
  iVar1 = masterslavefileclosev2(fileId,filemode,addr,fileposition,requesttype,length);
  return iVar1;
}



/* ---- receiveSerialMessage @ 00012ea8 ---- */

int receiveSerialMessage(int fd,Message *message,int timeoutSec)

{
  int iVar1;
  
  iVar1 = receiveSerialMessage(fd,message,timeoutSec);
  return iVar1;
}



/* ---- open_port @ 00012f08 ---- */

int open_port(char *tty)

{
  int iVar1;
  
  iVar1 = open_port(tty);
  return iVar1;
}



/* ---- validate_serial_connection @ 00012fbc ---- */

int validate_serial_connection(char *tty,int *fd)

{
  int iVar1;
  
  iVar1 = validate_serial_connection(tty,fd);
  return iVar1;
}



/* ---- masterslavesendFileBlockRecv2 @ 0001307c ---- */

int masterslavesendFileBlockRecv2
              (UINT16 fileId,UBYTE addr,UBYTE sectorNum,UBYTE totalSectors,UINT32 fileposition,
              UBYTE totalBlocks,UBYTE *blockBuffer,UINT16 datalength)

{
  int iVar1;
  
  iVar1 = masterslavesendFileBlockRecv2
                    (fileId,addr,sectorNum,totalSectors,fileposition,totalBlocks,blockBuffer,
                     datalength);
  return iVar1;
}



/* ---- Java_com_honeywell_comm_JNIRequest_jniReadSylkFileData @ 00013f90 ---- */

jbyteArray
Java_com_honeywell_comm_JNIRequest_jniReadSylkFileData
          (JNIEnv *env,jobject thisObj,jint fileId,jshort addr,jint fileposition,jint datalen)

{
  int iVar1;
  SylkFileDataRecord SStack_410;
  jshort addr_local;
  jint fileId_local;
  jobject thisObj_local;
  JNIEnv *env_local;
  jbyteArray retData;
  SylkFileDataRecord readResponseRecord;
  
  iVar1 = __stack_chk_guard;
  addr_local = addr;
  fileId_local = fileId;
  thisObj_local = thisObj;
  env_local = env;
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("jniReadSykFileData()  fileId=%d  addr=%d  fileposition=%d  datalen=%d\n",fileId,
                (int)addr,fileposition,datalen);
    fflush((FILE *)&_Stdout);
  }
  retData = (jbyteArray)0x0;
  memset(&readResponseRecord,0,0x1f0);
  if (env_local != (JNIEnv *)0x0) {
    masterslaveReadSylkFileData
              (&SStack_410,(UINT16)fileId_local,(UBYTE)addr_local,fileposition,datalen);
    memcpy(&readResponseRecord,&SStack_410,0x1f0);
    if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
      debugPrintf("jniReadSykFileData()  status=%hhd\n",(uint)readResponseRecord.blockHeader.status)
      ;
      fflush((FILE *)&_Stdout);
    }
    if (readResponseRecord.blockHeader.status == '\0') {
      retData = (*(*env_local)->NewByteArray)
                          (env_local,(jsize)readResponseRecord.blockHeader.length);
      (*(*env_local)->SetByteArrayRegion)
                (env_local,retData,0,(jsize)readResponseRecord.blockHeader.length,
                 (jbyte *)readResponseRecord.block);
      (*(*env_local)->ReleaseByteArrayElements)
                (env_local,retData,(jbyte *)readResponseRecord.block,1);
    }
  }
  if (iVar1 != __stack_chk_guard) {
                    /* WARNING: Subroutine does not return */
    __stack_chk_fail();
  }
  return retData;
}



/* ---- buildFileBlockRecord @ 00015060 ---- */

int buildFileBlockRecord
              (UBYTE ioCommand,UINT16 fileId,UBYTE addr,UBYTE sectorNum,UBYTE totalSectors,
              UBYTE blockNum,UBYTE totalBlocks,UBYTE *blockBuffer,UINT16 bufferSize)

{
  int iVar1;
  int iVar2;
  UBYTE *blockBuffer_local;
  UBYTE sectorNum_local;
  UINT16 fileId_local;
  UBYTE addr_local;
  UBYTE ioCommand_local;
  int fileBlockRecordPayloadSize;
  FileBlockRecord fileBlockRecord;
  Record record;
  
  iVar1 = __stack_chk_guard;
  if (bufferSize < 0x1e5) {
    memset(&fileBlockRecord,0,0x1f0);
    fileBlockRecord.blockHeader.file.blk.totalSectors = totalSectors;
    fileBlockRecord.blockHeader.file.blk.blockNum = blockNum;
    fileBlockRecord.blockHeader.file.blk.totalBlocks = totalBlocks;
    fileBlockRecord.blockHeader.file.blk.blockSize = bufferSize;
    fileBlockRecord.blockHeader.file.blk.fileId = fileId;
    fileBlockRecord.blockHeader.file.blk.ioCommand = ioCommand;
    fileBlockRecord.blockHeader.file.blk.addr = addr;
    fileBlockRecord.blockHeader.file.blk.sectorNum = sectorNum;
    memcpy(fileBlockRecord.block,blockBuffer,(uint)bufferSize);
    dataToRecord(&record,(uchar *)&fileBlockRecord,
                 fileBlockRecord.blockHeader.file.blk.blockSize + 0xc);
    buildMessage(mtFileBlock,&record,(ushort)addr,&sndMessage);
    iVar2 = 0;
  }
  else {
    if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel < DBG_INFO)) {
      debugPrintf("buildFileBlockRec ERROR: buffserSize exceeds FileBlockRecord payload  %d \n",
                  (uint)bufferSize);
      fflush((FILE *)&_Stdout);
    }
    iVar2 = -1;
  }
  if (iVar1 != __stack_chk_guard) {
                    /* WARNING: Subroutine does not return */
    __stack_chk_fail();
  }
  return iVar2;
}



/* ---- buildFileBlockCRCRecord @ 000151dc ---- */

int buildFileBlockCRCRecord
              (UBYTE ioCommand,UINT16 fileId,UBYTE addr,UINT32 fileLength,UINT32 fileCRC32)

{
  int iVar1;
  UINT32 fileLength_local;
  UINT16 fileId_local;
  UBYTE addr_local;
  UBYTE ioCommand_local;
  int fileBlockRecordPayloadSize;
  FileBlockRecord fileBlockRecord;
  Record record;
  
  iVar1 = __stack_chk_guard;
  memset(&fileBlockRecord,0,0x1f0);
  fileBlockRecord.blockHeader.file.crc.fileCRC32 = fileCRC32;
  fileBlockRecord.blockHeader.file.blk.fileId = fileId;
  fileBlockRecord.blockHeader.file.blk.ioCommand = ioCommand;
  fileBlockRecord.blockHeader.file.blk.addr = addr;
  fileBlockRecord.blockHeader.file.crc.fileLength = fileLength;
  dataToRecord(&record,(uchar *)&fileBlockRecord,0xc);
  buildMessage(mtFileBlock,&record,(ushort)addr,&sndMessage);
  if (iVar1 != __stack_chk_guard) {
                    /* WARNING: Subroutine does not return */
    __stack_chk_fail();
  }
  return 0;
}



/* ---- handleTerminalPropertyMessage @ 0001591c ---- */

void handleTerminalPropertyMessage(int status)

{
  int status_local;
  
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("handleTerminalPropertyMessage() bodySize %d \n",(uint)rcvMessage.header.bodySize);
    fflush((FILE *)&_Stdout);
  }
  receiveCurrItem = -1;
  rcvIOCommandStatus = 1;
  rcvDeviceFlags = 0;
  msgCompleteStatus = 1;
  return;
}



/* ---- handlePublicVariableMessage @ 00015f24 ---- */

/* WARNING: Unknown calling convention -- yet parameter storage is locked */

void handlePublicVariableMessage(void)

{
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("handlePublicVariableMessage() bodySize %d \n",(uint)rcvMessage.header.bodySize);
    fflush((FILE *)&_Stdout);
  }
  receivePVCurrItem = -1;
  rcvIOCommandStatus = 1;
  rcvDeviceFlags = 0;
  msgCompleteStatus = 1;
  return;
}



/* ---- open_port @ 000165d8 ---- */

int open_port(char *tty)

{
  int __fd;
  char *tty_local;
  int fd;
  
  __fd = open(tty,0x802);
  if (__fd == -1) {
    if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel < DBG_OFF)) {
      debugPrintf("open_port: Unable to open %s \n",tty);
      fflush((FILE *)&_Stdout);
    }
  }
  else {
    fcntl(__fd,4,0);
  }
  return __fd;
}



/* ---- serial_connect @ 000167b8 ---- */

int serial_connect(char *tty,int *fd)

{
  int iVar1;
  int *fd_local;
  char *tty_local;
  int ret;
  
  ret = -1;
  iVar1 = open_port(tty);
  *fd = iVar1;
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("serial_connect() open_port() %s  fd=%d\n",tty,*fd);
    fflush((FILE *)&_Stdout);
  }
  fflush((FILE *)&_Stdout);
  if (-1 < *fd) {
    iVar1 = tty_raw(*fd);
    if (iVar1 == 0) {
      ret = 0;
    }
    else {
      ret = -1;
      if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel < DBG_OFF)) {
        debugPrintf("tty_raw() failed!\n");
        fflush((FILE *)&_Stdout);
      }
      fflush((FILE *)&_Stdout);
    }
  }
  return ret;
}



/* ---- validate_serial_connection @ 00016894 ---- */

int validate_serial_connection(char *tty,int *fd)

{
  int iVar1;
  int iVar2;
  int *fd_local;
  char *tty_local;
  int ret;
  stat status;
  
  iVar1 = __stack_chk_guard;
  lockMutexSerial();
  iVar2 = fstat(*fd,(stat *)&status);
  if (iVar2 == 0) {
    ret = 0;
  }
  else {
    ret = serial_connect(tty,fd);
  }
  unlockMutexSerial();
  if (iVar1 != __stack_chk_guard) {
                    /* WARNING: Subroutine does not return */
    __stack_chk_fail();
  }
  return ret;
}



/* ---- write_serial_record @ 00016d24 ---- */

int write_serial_record(int fd,uchar *rec,int recSize)

{
  ssize_t sVar1;
  int iVar2;
  int recSize_local;
  uchar *rec_local;
  int fd_local;
  int n;
  
  lockMutexSerial();
  sVar1 = write(fd,rec,recSize);
  unlockMutexSerial();
  if (sVar1 < 0) {
    if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel < DBG_INFO)) {
      debugPrintf("write_serial_record() failed!  %d\r\n",sVar1);
      fflush((FILE *)&_Stdout);
    }
    iVar2 = -1;
  }
  else {
    iVar2 = 0;
  }
  return iVar2;
}



/* ---- masterslavesendFileBlockRecv2 @ 00016dbc ---- */

int masterslavesendFileBlockRecv2
              (UINT16 fileId,UBYTE addr,UBYTE sectorNum,UBYTE totalSectors,UINT32 fileposition,
              UBYTE totalBlocks,UBYTE *blockBuffer,UINT16 datalength)

{
  SerialSync SVar1;
  MessageId MVar2;
  undefined4 uVar3;
  undefined4 uVar4;
  int iVar5;
  undefined1 auStack_220 [507];
  UBYTE totalSectors_local;
  UBYTE sectorNum_local;
  UBYTE addr_local;
  UINT16 fileId_local;
  int fileBlockRecordPayloadSize;
  
  totalSectors_local = totalSectors;
  sectorNum_local = sectorNum;
  addr_local = addr;
  fileId_local = fileId;
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("masterslavesendFileBlockRecv2  filling send msg fileposition=%d\tdatalength=%d  payload size=%d\n"
                ,fileposition,(uint)datalength,0x1e4);
    fflush((FILE *)&_Stdout);
  }
  if (datalength < 0x1e5) {
    memset(&sendSylkFileDataRecord,0,0x1f0);
    memset(&fileblockrecordbuffer,0,500);
    sendFileBlockFileId = (int)fileId_local;
    sendSylkFileDataRecord.blockHeader.fileId = fileId_local;
    sendSylkFileDataRecord.blockHeader.ioCommand = '!';
    sendSylkFileDataRecord.blockHeader.addr = '\0';
    sendSylkFileDataRecord.blockHeader.status = 0xff;
    sendSylkFileDataRecord.blockHeader.request = '\0';
    sendSylkFileDataRecord.blockHeader.length = datalength;
    sendSylkFileDataRecord.blockHeader.fileposition = fileposition;
    memcpy(sendSylkFileDataRecord.block,blockBuffer,(uint)datalength);
    fileBlockRecordPayloadSize = sendSylkFileDataRecord.blockHeader.length + 0xc;
    dataToRecord(&fileblockrecordbuffer,(uchar *)&sendSylkFileDataRecord,
                 (short)fileBlockRecordPayloadSize);
    sendFileBlockMessageId =
         buildMessage(mtFileData,&fileblockrecordbuffer,(ushort)addr_local,&fileblockrecordmessage);
    if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
      debugPrintf("<<<<masterslavesendFileBlockRecv2() new FileBlockRecord  message bodySize=%d \n",
                  (uint)fileblockrecordmessage.header._4_4_ >> 0x10);
      fflush((FILE *)&_Stdout);
    }
    uVar4 = fileblockrecordmessage.header._8_4_;
    uVar3 = fileblockrecordmessage.header._4_4_;
    MVar2 = fileblockrecordmessage.header.messageId;
    SVar1.sync = fileblockrecordmessage.serialSync.sync;
    memcpy(auStack_220,&fileblockrecordmessage.body,0x1f8);
    SetSendMessage(SVar1.sync,MVar2,uVar3,uVar4);
    iVar5 = requestOneMessage();
  }
  else {
    if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel < DBG_INFO)) {
      debugPrintf("masterslavesendFileBlockRecv2 ERROR: buffserSize exceeds FileBlockRecord payload  %d \n"
                  ,(uint)datalength);
      fflush((FILE *)&_Stdout);
    }
    iVar5 = -1;
  }
  return iVar5;
}



/* ---- masterslaveReadSylkFileData @ 00017004 ---- */

SylkFileDataRecord *
masterslaveReadSylkFileData
          (SylkFileDataRecord *__return_storage_ptr__,UINT16 fileId,UBYTE addr,UINT32 fileposition,
          UINT32 datalength)

{
  SerialSync SVar1;
  MessageId MVar2;
  undefined4 uVar3;
  undefined4 uVar4;
  int iVar5;
  undefined1 auStack_428 [508];
  UINT32 fileposition_local;
  UBYTE addr_local;
  UINT16 fileId_local;
  SylkFileDataRecord *local_224;
  int requestonereturnval;
  int fileBlockRecordPayloadSize;
  SylkFileDataRecord defaultdatarecord;
  
  iVar5 = __stack_chk_guard;
  requestonereturnval = -1;
  fileposition_local = fileposition;
  addr_local = addr;
  fileId_local = fileId;
  local_224 = __return_storage_ptr__;
  memset(&defaultdatarecord,0,0x1f0);
  defaultdatarecord.blockHeader.length = 0xffff;
  defaultdatarecord.blockHeader.fileposition = 0xffffffff;
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("masterslaveReadSylkFileData  fileposition=%d\tdatalength=%d  payload size=%d\n",
                fileposition_local,datalength,0x1e4);
    fflush((FILE *)&_Stdout);
  }
  if (datalength < 0x1e5) {
    memset(&readreqSylkFileDataRecord,0,0x1f0);
    memset(&fileblockrecordbuffer,0,500);
    recvFileBlockFileId = (int)fileId_local;
    readreqSylkFileDataRecord.blockHeader.fileId = fileId_local;
    readreqSylkFileDataRecord.blockHeader.ioCommand = '\x1f';
    readreqSylkFileDataRecord.blockHeader.addr = '\0';
    readreqSylkFileDataRecord.blockHeader.status = 0xff;
    readreqSylkFileDataRecord.blockHeader.request = '\x01';
    readreqSylkFileDataRecord.blockHeader.length = (UINT16)datalength;
    readreqSylkFileDataRecord.blockHeader.fileposition = fileposition_local;
    fileBlockRecordPayloadSize = (datalength & 0xffff) + 0xc;
    dataToRecord(&fileblockrecordbuffer,(uchar *)&readreqSylkFileDataRecord,
                 (short)fileBlockRecordPayloadSize);
    recvFileBlockMessageId =
         buildMessage(mtFileData,&fileblockrecordbuffer,(ushort)addr_local,&fileblockrecordmessage);
    uVar4 = fileblockrecordmessage.header._8_4_;
    uVar3 = fileblockrecordmessage.header._4_4_;
    MVar2 = fileblockrecordmessage.header.messageId;
    SVar1 = fileblockrecordmessage.serialSync;
    memcpy(auStack_428,&fileblockrecordmessage.body,0x1f8);
    SetSendMessage(SVar1.sync,MVar2,uVar3,uVar4);
    requestonereturnval = requestOneMessage();
    if (requestonereturnval == 1) {
      if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
        debugPrintf("masterslaveReadSylkFileData(): SUCCESS\n");
        fflush((FILE *)&_Stdout);
      }
      readrespSylkFileDataRecord.blockHeader.status = '\0';
      memcpy(local_224,&readrespSylkFileDataRecord,0x1f0);
    }
    else {
      if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
        debugPrintf("masterslaveReadSylkFileData(): ERROR\n");
        fflush((FILE *)&_Stdout);
      }
      defaultdatarecord.blockHeader.status = '\x02';
      memcpy(local_224,&defaultdatarecord,0x1f0);
    }
  }
  else {
    if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel < DBG_INFO)) {
      debugPrintf("masterslaveReadSylkFileData ERROR: buffserSize exceeds FileBlockRecord payload  %d \n"
                  ,datalength);
      fflush((FILE *)&_Stdout);
    }
    defaultdatarecord.blockHeader.status = '\x01';
    memcpy(local_224,&defaultdatarecord,0x1f0);
  }
  if (iVar5 != __stack_chk_guard) {
                    /* WARNING: Subroutine does not return */
    __stack_chk_fail();
  }
  return local_224;
}



/* ---- masterslavefileopenv2 @ 00017f30 ---- */

int masterslavefileopenv2
              (UINT32 fileId,UINT16 filemode,UINT16 addr,UINT16 fileposition,UINT16 requesttype,
              UINT16 length)

{
  SerialSync SVar1;
  MessageId MVar2;
  undefined4 uVar3;
  undefined4 uVar4;
  int iVar5;
  uint local_228;
  uint local_224;
  uint local_220;
  UINT16 fileposition_local;
  UINT16 addr_local;
  UINT16 filemode_local;
  UINT32 fileId_local;
  int fileBlockRecordPayloadSize;
  
  fileposition_local = fileposition;
  addr_local = addr;
  filemode_local = filemode;
  fileId_local = fileId;
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    local_228 = (uint)fileposition;
    local_224 = (uint)requesttype;
    local_220 = (uint)length;
    debugPrintf("masterslavefileopenv2() fileid %d filemode %d addr %d status %d requesttype %d timeout %d\n"
                ,fileId,(uint)filemode,(uint)addr);
    fflush((FILE *)&_Stdout);
  }
  memset(&sendSylkFileDataRecord,0,0x1f0);
  memset(&fileblockrecordbuffer,0,500);
  sendFileBlockFileId = fileId_local;
  sendSylkFileDataRecord.blockHeader.fileId = (UINT16)fileId_local;
  if (filemode_local == 0) {
    sendSylkFileDataRecord.blockHeader.ioCommand = '\x17';
  }
  else if (filemode_local == 1) {
    sendSylkFileDataRecord.blockHeader.ioCommand = '\x19';
  }
  sendSylkFileDataRecord.blockHeader.addr = '\0';
  sendSylkFileDataRecord.blockHeader.status = 0xff;
  sendSylkFileDataRecord.blockHeader.request = '\0';
  sendSylkFileDataRecord.blockHeader.length = length;
  sendSylkFileDataRecord.blockHeader.fileposition = (UINT32)fileposition_local;
  fileBlockRecordPayloadSize = 0xc;
  dataToRecord(&fileblockrecordbuffer,(uchar *)&sendSylkFileDataRecord,0xc);
  sendFileBlockMessageId =
       buildMessage(mtFileData,&fileblockrecordbuffer,addr_local,&fileblockrecordmessage);
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("masterslavefileopenv2() new FileBlockRecord  message bodySize=%d \n",
                (uint)fileblockrecordmessage.header._4_4_ >> 0x10);
    fflush((FILE *)&_Stdout);
  }
  uVar4 = fileblockrecordmessage.header._8_4_;
  uVar3 = fileblockrecordmessage.header._4_4_;
  MVar2 = fileblockrecordmessage.header.messageId;
  SVar1.sync = fileblockrecordmessage.serialSync.sync;
  memcpy(&local_228,&fileblockrecordmessage.body,0x1f8);
  SetSendMessage(SVar1.sync,MVar2,uVar3,uVar4);
  iVar5 = requestOneMessage();
  return iVar5;
}



/* ---- masterslavefilestatus @ 00018104 ---- */

int masterslavefilestatus
              (UINT32 fileId,UINT16 filemode,UINT16 addr,UINT16 fileposition,UINT16 requesttype,
              UINT16 length)

{
  SerialSync SVar1;
  MessageId MVar2;
  undefined4 uVar3;
  undefined4 uVar4;
  int iVar5;
  uint local_228;
  uint local_224;
  uint local_220;
  UINT16 fileposition_local;
  UINT16 addr_local;
  UINT16 filemode_local;
  UINT32 fileId_local;
  int fileBlockRecordPayloadSize;
  
  fileposition_local = fileposition;
  addr_local = addr;
  filemode_local = filemode;
  fileId_local = fileId;
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    local_228 = (uint)fileposition;
    local_224 = (uint)requesttype;
    local_220 = (uint)length;
    debugPrintf("masterslavefilestatus() fileid %d filemode %d addr %d fileposition %d requesttype %d length %d\n"
                ,fileId,(uint)filemode,(uint)addr);
    fflush((FILE *)&_Stdout);
  }
  memset(&sendFileBlockRecord,0,0x1f0);
  memset(&fileblockrecordbuffer,0,500);
  sendFileBlockFileId = fileId_local;
  sendSylkFileDataRecord.blockHeader.ioCommand = '\x1d';
  sendSylkFileDataRecord.blockHeader.addr = '\0';
  sendSylkFileDataRecord.blockHeader.status = 0xff;
  sendSylkFileDataRecord.blockHeader.request = 0xff;
  sendSylkFileDataRecord.blockHeader.length = 0;
  sendSylkFileDataRecord.blockHeader.fileposition = 0;
  fileBlockRecordPayloadSize = 0xc;
  dataToRecord(&fileblockrecordbuffer,(uchar *)&sendSylkFileDataRecord,0xc);
  sendFileBlockMessageId =
       buildMessage(mtFileData,&fileblockrecordbuffer,addr_local,&fileblockrecordmessage);
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("masterslavefilestatusv2() new FileBlockRecord  message bodySize=%d \n",
                (uint)fileblockrecordmessage.header._4_4_ >> 0x10);
    fflush((FILE *)&_Stdout);
  }
  uVar4 = fileblockrecordmessage.header._8_4_;
  uVar3 = fileblockrecordmessage.header._4_4_;
  MVar2 = fileblockrecordmessage.header.messageId;
  SVar1.sync = fileblockrecordmessage.serialSync.sync;
  memcpy(&local_228,&fileblockrecordmessage.body,0x1f8);
  SetSendMessage(SVar1.sync,MVar2,uVar3,uVar4);
  iVar5 = requestOneMessage();
  return iVar5;
}



/* ---- masterslavefileclosev2 @ 000182b0 ---- */

int masterslavefileclosev2
              (UINT32 fileId,UINT16 filemode,UINT16 addr,UINT16 fileposition,UINT16 requesttype,
              UINT16 length)

{
  SerialSync SVar1;
  MessageId MVar2;
  undefined4 uVar3;
  undefined4 uVar4;
  int iVar5;
  uint local_228;
  uint local_224;
  uint local_220;
  UINT16 fileposition_local;
  UINT16 addr_local;
  UINT16 filemode_local;
  UINT32 fileId_local;
  int fileBlockRecordPayloadSize;
  
  fileposition_local = fileposition;
  addr_local = addr;
  filemode_local = filemode;
  fileId_local = fileId;
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    local_228 = (uint)fileposition;
    local_224 = (uint)requesttype;
    local_220 = (uint)length;
    debugPrintf("masterslavefileclosev2() fileid %d filemode %d addr %d fileposition %d requesttype %d length %d\n"
                ,fileId,(uint)filemode,(uint)addr);
    fflush((FILE *)&_Stdout);
  }
  memset(&sendFileBlockRecord,0,0x1f0);
  memset(&fileblockrecordbuffer,0,500);
  sendFileBlockFileId = fileId_local;
  sendSylkFileDataRecord.blockHeader.ioCommand = '\x1b';
  sendSylkFileDataRecord.blockHeader.addr = '\0';
  sendSylkFileDataRecord.blockHeader.status = 0xff;
  sendSylkFileDataRecord.blockHeader.request = '\x02';
  sendSylkFileDataRecord.blockHeader.length = length;
  sendSylkFileDataRecord.blockHeader.fileposition = (UINT32)fileposition_local;
  fileBlockRecordPayloadSize = 0xc;
  dataToRecord(&fileblockrecordbuffer,(uchar *)&sendSylkFileDataRecord,0xc);
  sendFileBlockMessageId =
       buildMessage(mtFileData,&fileblockrecordbuffer,addr_local,&fileblockrecordmessage);
  if (((g_DbgTopic & 8U) != 0) && (g_DbgLevel == DBG_FULL)) {
    debugPrintf("masterslavefileclosev2 new FileBlockRecord  message bodySize=%d \n",
                (uint)fileblockrecordmessage.header._4_4_ >> 0x10);
    fflush((FILE *)&_Stdout);
  }
  uVar4 = fileblockrecordmessage.header._8_4_;
  uVar3 = fileblockrecordmessage.header._4_4_;
  MVar2 = fileblockrecordmessage.header.messageId;
  SVar1.sync = fileblockrecordmessage.serialSync.sync;
  memcpy(&local_228,&fileblockrecordmessage.body,0x1f8);
  SetSendMessage(SVar1.sync,MVar2,uVar3,uVar4);
  iVar5 = requestOneMessage();
  return iVar5;
}



/* ---- crc_ccitt_add @ 0001845c ---- */

uint16_t crc_ccitt_add(uint16_t crc,uchar *ptr,size_t num_bytes)

{
  size_t num_bytes_local;
  uchar *ptr_local;
  uint16_t crc_local;
  uint16_t short_c;
  uint16_t tmp;
  size_t a;
  
  crc_local = crc;
  if (ptr != (uchar *)0x0) {
    ptr_local = ptr;
    for (a = 0; a < num_bytes; a = a + 1) {
      crc_local = crc_tabccitt[(byte)(*ptr_local ^ (byte)(crc_local >> 8))] ^ crc_local << 8;
      ptr_local = ptr_local + 1;
    }
  }
  return crc_local;
}



/* ---- receiveSerialSync @ 00018848 ---- */

int receiveSerialSync(int fd,int timeoutSec)

{
  int iVar1;
  int timeoutSec_local;
  int fd_local;
  uchar c;
  int offset;
  int totalCount;
  int local_14;
  
  local_14 = __stack_chk_guard;
  offset = 0;
  totalCount = 0;
  do {
    iVar1 = read_serial_record(fd,&c,1,timeoutSec);
    if (iVar1 != 0) {
      iVar1 = -1;
      goto LAB_000188c6;
    }
    if (defaultSerialSync.sync[offset] == c) {
      offset = offset + 1;
    }
    else {
      offset = 0;
    }
    if (offset == 4) {
      iVar1 = 0;
      goto LAB_000188c6;
    }
    totalCount = totalCount + 1;
    if (0x5d0 < totalCount) {
      iVar1 = -1;
      goto LAB_000188c6;
    }
  } while ((uint)offset < 4);
  iVar1 = -1;
LAB_000188c6:
  if (local_14 != __stack_chk_guard) {
                    /* WARNING: Subroutine does not return */
    __stack_chk_fail();
  }
  return iVar1;
}



/* ---- receiveSerialMessage @ 000188ec ---- */

int receiveSerialMessage(int fd,Message *message,int timeoutSec)

{
  bool bVar1;
  uint16_t uVar2;
  int iVar3;
  int timeoutSec_local;
  Message *message_local;
  int fd_local;
  _Bool crcSuccess;
  _Bool crcSuccess_1;
  UINT16 rcvHeaderCRC;
  UINT16 rcvRecordCRC;
  int retMsgStatus;
  size_t crc_num_bytes;
  int select_result;
  
  memset(message,0,0x208);
  iVar3 = select_serial(fd,timeoutSec);
  if (iVar3 == 0) {
    retMsgStatus = 0x32;
  }
  else if (iVar3 < 0) {
    retMsgStatus = 0x33;
  }
  else {
    lockMutexSerial();
    iVar3 = receiveSerialSync(fd,timeoutSec);
    if (iVar3 == -1) {
      retMsgStatus = 0x34;
    }
    else {
      iVar3 = read_serial_record(fd,(uchar *)&message->header,0xc,timeoutSec);
      if (iVar3 == 0) {
        bVar1 = true;
        if ((message->header).protocolVersion != '\0') {
          uVar2 = crc_ccitt_add(0xf1f1,(uchar *)&message->header,10);
          if (uVar2 != (message->header).headerCRC) {
            bVar1 = false;
          }
        }
        if (bVar1) {
          if ((message->header).bodySize < 0x1f9) {
            iVar3 = read_serial_record(fd,(uchar *)&message->body,(uint)(message->header).bodySize,
                                       timeoutSec);
            if (iVar3 == 0) {
              bVar1 = true;
              if ((message->header).protocolVersion != '\0') {
                crc_num_bytes = (message->body).record.dataSize + 4;
                if (500 < crc_num_bytes) {
                  crc_num_bytes = 500;
                }
                uVar2 = crc_ccitt_add(0xf1f1,(uchar *)&(message->body).record,crc_num_bytes);
                if (uVar2 != (message->body).recordCRC) {
                  bVar1 = false;
                }
              }
              if (bVar1) {
                if ((uint)(message->header).bodySize < (message->body).record.dataSize + 2) {
                  retMsgStatus = 0x38;
                }
                else {
                  retMsgStatus = 1;
                  if (1 < (message->header).msgStatus) {
                    retMsgStatus = (int)(message->header).msgStatus;
                  }
                }
              }
              else {
                retMsgStatus = 0x37;
              }
            }
            else {
              retMsgStatus = 0x39;
            }
          }
          else {
            retMsgStatus = 0x36;
          }
        }
        else {
          retMsgStatus = 0x35;
        }
      }
      else {
        retMsgStatus = 0x3a;
      }
    }
    unlockMutexSerial();
  }
  return retMsgStatus;
}


