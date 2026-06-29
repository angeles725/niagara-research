package com.tridium.lonworks.loncomm;

import com.tridium.lonworks.enums.BLonCompletionCode;
import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BBroadcast;
import javax.baja.lonworks.datatypes.BImplicit;
import javax.baja.lonworks.datatypes.BLonCommConfig;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.io.AppBuffer;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.spy.SpyWriter;

public class NAppBuffer implements AppBuffer, LinkedQueue.Linkable {
   protected static final int NI_HDR_SIZE = 2;
   public static final int niTQ = 2;
   public static final int niTQ_P = 3;
   public static final int niNTQ = 4;
   public static final int niNTQ_P = 5;
   public static final int niRESPONSE = 6;
   public static final int niINCOMING = 8;
   public static final int niCOMM = 16;
   public static final int niNETMGMT = 32;
   public static final int niRESET = 80;
   public static final int niFLUSH_CANCEL = 96;
   public static final int niFLUSH_COMPLETE = 96;
   public static final int niONLINE = 112;
   public static final int niOFFLINE = 128;
   public static final int niFLUSH = 144;
   public static final int niFLUSH_IGN = 160;
   public static final int niSLEEP = 176;
   public static final int niSSTATUS = 224;
   public static final int niIRQENA = 229;
   public static final int niSERVICE = 230;
   public static final int QUEUE_MASK = 15;
   public static final int CMD_MASK = 240;
   protected static final int MSG_HDR_OFFSET = 2;
   protected static final int MSG_HDR_SIZE = 3;
   protected static final int NETVAR_BIT = 128;
   protected static final int AUTH_BIT = 16;
   protected static final int PRIORITY_BIT = 128;
   protected static final int PATH_BIT = 64;
   protected static final int ADDR_MODE_BIT = 8;
   protected static final int ALT_PATH_BIT = 4;
   protected static final int TRNARND_BIT = 4;
   protected static final int POOL_BIT = 2;
   protected static final int RESP_BIT = 1;
   protected static final int POLL_BIT = 64;
   protected static final int MSG_HDR_TYPE_MASK = 128;
   protected static final int SERVICE_TYPE_MASK = 96;
   protected static final int TAG_MASK = 15;
   protected static final int COMPL_CODE_MASK = 48;
   LonAddress destAddr = null;
   BLonRepeatTimer repeatTimer = null;
   BLonRepeatTimer transmitTimer = null;
   BLonServiceType serviceType = null;
   int retryCount = 0;
   protected static final int ADDR_SIZE = 11;
   protected static final int ADDR_OFFSET = 5;
   private static final int TYPE_INDEX = 5;
   private static final int RSP_DOMAIN_INDEX = 5;
   private static final int IMPLICIT_TAG = 6;
   private static final int DOMAIN_INDEX = 6;
   private static final int NODE_INDEX = 6;
   private static final int RETRY_COUNT_INDEX = 7;
   private static final int REPEAT_TIMER_INDEX = 7;
   private static final int TRANSMIT_TIMER_INDEX = 8;
   private static final int SUBNET_INDEX = 9;
   private static final int NEURON_ID_INDEX = 10;
   private static final int DOMAIN_MASK = 128;
   private static final int NODE_MASK = 127;
   private static final int RETRY_COUNT_MASK = 15;
   private static final int REPEAT_TIMER_MASK = 240;
   private static final int TRANSMIT_TIMER_MASK = 15;
   private static final int SRC_SUBNET_INDEX = 6;
   private static final int SRC_NODE_INDEX = 7;
   protected static final int MSG_OFFSET = 16;
   NAppBuffer.AppBuffInputStream inputStream = null;
   NAppBuffer.AppBuffOutputStream outputStream = null;
   public boolean farSide;
   private byte[] buf = new byte[255];
   private boolean freeBuf = false;
   private boolean localCommand = false;
   private boolean retryCountSet;
   private boolean repeatTimerSet;
   private boolean transmitTimerSet;
   private boolean noTransaction;
   public LonException exception = null;
   private NAppBuffer next = null;
   private boolean inQueue = false;
   public NAppBuffer nextAppBuffer = null;
   public static final int APP_BUFFER_HDR_LEN = 16;
   private static final int MAX_APP_BUFFERS = 32;
   private static NAppBuffer[] appPool = new NAppBuffer[32];
   private static int appCnt = 0;
   private static int appInstanceCnt = 0;
   static int cnt = 0;
   public final int id = cnt++;

   public boolean isResponse() {
      return this.getQueue() == 6;
   }

   public boolean isIncoming() {
      return this.getQueue() == 8;
   }

   public int getQueue() {
      return this.buf[0] & 15;
   }

   public void setQueue(int queue) {
      this.buf[0] = (byte)(this.buf[0] & -16);
      this.buf[0] = (byte)(this.buf[0] | queue);
   }

   public int getCommand() {
      return this.buf[0] & 240;
   }

   public void setCommand(int command) {
      this.buf[0] = (byte)(this.buf[0] & -241);
      this.buf[0] = (byte)(this.buf[0] | command);
   }

   public void setLocalCommand(int command) {
      this.setCommand(command);
      this.localCommand = true;
   }

   public boolean isLocalCommand() {
      return this.localCommand;
   }

   public int getBufferLength() {
      return this.getByte(1);
   }

   public void setBufferLength(int i) {
      this.buf[1] = (byte)i;
   }

   public boolean isNetVar() {
      return (this.buf[2] & 128) != 0;
   }

   public int getTag() {
      return this.buf[2] & 15;
   }

   public void setTag(int tag) {
      this.buf[2] = (byte)(this.buf[2] & -16);
      this.buf[2] = (byte)(this.buf[2] | (byte)(tag & 15));
   }

   public boolean isPriority() {
      return (this.buf[3] & 128) != 0;
   }

   public void setPriority(boolean priority) {
      if (priority) {
         this.buf[3] = (byte)(this.buf[3] | 128);
      } else {
         this.buf[3] = (byte)(this.buf[3] & -129);
      }
   }

   public boolean isPath() {
      return (this.buf[3] & 64) != 0;
   }

   public void setPath(boolean path) {
      if (path) {
         this.buf[3] = (byte)(this.buf[3] | 64);
      } else {
         this.buf[3] = (byte)(this.buf[3] & -65);
      }
   }

   public BLonCompletionCode getComplCode() {
      return (BLonCompletionCode)BLonCompletionCode.notComp.getRange().get((this.buf[3] & 48) >> 4);
   }

   public boolean isCompletionEvent() {
      return (this.buf[3] & 48) != 0;
   }

   public void setComplCode(BLonCompletionCode complCode) {
      if (complCode == BLonCompletionCode.notComp || complCode == BLonCompletionCode.succeeds || complCode == BLonCompletionCode.fails) {
         this.buf[3] = (byte)(this.buf[3] & -49);
         this.buf[3] = (byte)(this.buf[3] | complCode.getOrdinal() << 4);
      }
   }

   public boolean isExplicitAddress() {
      return (this.buf[3] & 8) != 0;
   }

   public boolean isImplicitAddress() {
      return !this.isExplicitAddress();
   }

   public void setExplicitAddress(boolean explicitMsg) {
      if (explicitMsg) {
         this.buf[3] = (byte)(this.buf[3] | 8);
      } else {
         this.buf[3] = (byte)(this.buf[3] & -9);
      }
   }

   public boolean isPool() {
      return (this.buf[3] & 2) != 0;
   }

   public void setPool(boolean pool) {
      if (pool) {
         this.buf[3] = (byte)(this.buf[3] | 2);
      } else {
         this.buf[3] = (byte)(this.buf[3] & -3);
      }
   }

   public boolean isResp() {
      return (this.buf[3] & 1) != 0;
   }

   public void setResp(boolean resp) {
      if (resp) {
         this.buf[3] = (byte)(this.buf[3] | 1);
      } else {
         this.buf[3] = (byte)(this.buf[3] & -2);
      }
   }

   public int getDataLength() {
      return this.getByte(4);
   }

   public void setDataLength(int length) {
      this.buf[4] = (byte)length;
   }

   public BLonServiceType getServiceType() {
      return (BLonServiceType)BLonServiceType.unacked.getRange().get((this.buf[2] & 96) >> 5);
   }

   public boolean isRequest() {
      BLonServiceType type = this.getServiceType();
      return type == BLonServiceType.request;
   }

   public void setServiceType(BLonServiceType serviceType) {
      this.setServiceType(serviceType, false);
   }

   public void setServiceType(BLonServiceType st, boolean incoming) {
      this.serviceType = st;
      if (incoming) {
         this.setQueue(8);
      } else if (this.serviceType == BLonServiceType.unacked) {
         this.setQueue(4);
      } else {
         this.setQueue(2);
      }

      this.buf[2] = (byte)(this.buf[2] & -97);
      this.buf[2] = (byte)(this.buf[2] | this.serviceType.getOrdinal() << 5);
   }

   public boolean isAuthenticate() {
      return (this.buf[2] & 16) != 0;
   }

   public void setAuthenticate(boolean auth) {
      if (auth) {
         this.buf[2] = (byte)(this.buf[2] | 16);
      } else {
         this.buf[2] = (byte)(this.buf[2] & -17);
      }
   }

   public boolean isAltPath() {
      return (this.buf[3] & 4) != 0;
   }

   public void setAltPath(boolean auth) {
      if (auth) {
         this.buf[3] = (byte)(this.buf[3] | 4);
      } else {
         this.buf[3] = (byte)(this.buf[3] & -5);
      }
   }

   public boolean isPoll() {
      return (this.buf[2] & 64) != 0;
   }

   public void setPoll(boolean poll) {
      if (poll) {
         this.buf[2] = (byte)(this.buf[2] | 64);
      } else {
         this.buf[2] = (byte)(this.buf[2] & -65);
      }
   }

   public boolean isTurnaround() {
      return (this.buf[3] & 4) != 0;
   }

   public void setTurnaround(boolean trnarnd) {
      if (trnarnd) {
         this.buf[3] = (byte)(this.buf[3] | 4);
      } else {
         this.buf[3] = (byte)(this.buf[3] & -5);
      }
   }

   public void setDestAddress(LonAddress adr) {
      this.destAddr = adr;
      boolean explicit = true;
      byte addressType = (byte)adr.getAddressType();
      this.buf[5] = addressType;
      int command = 16;
      switch (addressType) {
         case 0:
         default:
            break;
         case 1:
            this.buf[6] = (byte)(this.buf[6] | (byte)(((BSubnetNode)adr).getNodeId() & 127));
            this.buf[9] = (byte)((BSubnetNode)adr).getSubnetId();
            break;
         case 2:
            System.arraycopy(((BNeuronId)adr).getByteArray(), 0, this.buf, 10, 6);
            break;
         case 3:
            this.setDomainIndex(((BBroadcast)adr).getDomainIndex());
            break;
         case 126:
            this.setTag(((BImplicit)adr).getTag());
            explicit = false;
            this.disableSetDefaultTimer();
            break;
         case 127:
            command = 32;
      }

      this.setCommand(command);
      this.setExplicitAddress(explicit);
   }

   public LonAddress getDestAddress() {
      return this.destAddr;
   }

   public void setDomainIndex(int ndx) {
      if (ndx == 0) {
         this.buf[6] = (byte)(this.buf[6] & 127);
      } else {
         this.buf[6] = (byte)(this.buf[6] | -128);
      }
   }

   private int getDomainIndex() {
      int byteNdx = this.isResponse() ? 5 : 6;
      return (this.buf[byteNdx] & 128) == 0 ? 0 : 1;
   }

   public void setRetryCount(int count) {
      if (count > 15) {
         count = 15;
      }

      this.buf[7] = (byte)(count | this.buf[7] & 240);
      this.retryCountSet = true;
      this.retryCount = count;
   }

   public int getRetryCount() {
      return this.retryCount;
   }

   public void setRepeatTimer(BLonRepeatTimer rpt) {
      int timer = rpt.getOrdinal();
      this.buf[7] = (byte)(timer << 4 | this.buf[7] & 15);
      this.repeatTimerSet = true;
      this.repeatTimer = rpt;
   }

   public BLonRepeatTimer getRepeatTimer() {
      return this.repeatTimer;
   }

   public void setTransmitTimer(BLonRepeatTimer trt) {
      int index = trt.getOrdinal();
      this.buf[8] = (byte)(this.buf[8] | (byte)index);
      this.transmitTimerSet = true;
      this.transmitTimer = trt;
   }

   public BLonRepeatTimer getTransmitTimer() {
      return this.transmitTimer;
   }

   public long getMaxTransactionTime() {
      if (this.serviceType == null) {
         return 8000L;
      } else {
         int time = 8000;
         if (this.serviceType != BLonServiceType.request && this.serviceType != BLonServiceType.acked) {
            if (this.serviceType == BLonServiceType.unackedRpt && this.repeatTimer != null && this.retryCount != 0) {
               time = this.repeatTimer.getTime() * (this.retryCount + 1);
            }
         } else if (this.transmitTimer != null && this.retryCount != 0) {
            time = this.transmitTimer.getTime() * (this.retryCount + 1);
         }

         return time;
      }
   }

   public void disableSetDefaultTimer() {
      this.retryCountSet = true;
      this.repeatTimerSet = true;
      this.transmitTimerSet = true;
   }

   public void setDefaultTimers(BLonNetwork lon) {
      BLonCommConfig cfg = lon.getLonCommConfig();
      if (!this.repeatTimerSet) {
         this.setRepeatTimer(cfg.getRepeatTimer());
      }

      if (!this.transmitTimerSet) {
         this.setTransmitTimer(cfg.getTransmitTimer());
      }

      if (!this.retryCountSet) {
         this.setRetryCount(cfg.getRetryCount());
      }
   }

   public boolean isLocalAddress() {
      return this.getCommand() == 32;
   }

   public int getHiddenImplicitTransactionTag() {
      return this.getByte(6);
   }

   public void hideImplicitTransactionTag(int tag) {
      this.buf[6] = (byte)tag;
   }

   public void setTransactionTag(int tag) {
      this.setTag(tag);
   }

   public int getTransactionTag() {
      return this.getTag();
   }

   public BSubnetNode getSourceAddress() {
      int subnet = this.buf[6] & 255;
      int node = this.buf[7] & 127;
      return BSubnetNode.make(subnet, node);
   }

   public void setSourceAddress(BSubnetNode sn) {
      this.buf[6] = (byte)sn.getSubnetId();
      this.buf[7] = (byte)sn.getNodeId();
   }

   public int getMessageCode() {
      return this.getByte(16);
   }

   public void setMessage(LonMessage msg) {
      LonOutputStream out = this.getMsgOutputStream();
      if (msg.isFarSide()) {
         out.writeUnsigned8(126);
      }

      msg.toOutputStream(out);
      this.setDataLength(out.size() - 16);
      if (msg.isPriority()) {
         this.setPriority(true);
      }

      if (msg.isAuthenticate()) {
         this.setAuthenticate(true);
      }

      int rtryCnt = msg.getRetryCount();
      if (rtryCnt > 15) {
         rtryCnt = 15;
      }

      if (rtryCnt >= 0) {
         this.setRetryCount(rtryCnt);
      }

      BLonRepeatTimer trt = msg.getTransmitTimer();
      if (trt != null) {
         this.setTransmitTimer(trt);
      }

      BLonRepeatTimer rpt = msg.getRepeatTimer();
      if (rpt != null) {
         this.setRepeatTimer(rpt);
      }
   }

   public LonMessage getLonMessage(Class<?> cls) throws LonException {
      LonMessage msg;
      try {
         if (cls != null) {
            if (cls == RawMessage.class) {
               return new RawMessage(this.buf, 16 + this.getDataLength());
            }

            msg = (LonMessage)cls.getDeclaredConstructor().newInstance();
            msg.fromInputStream(this.getMsgInputStream());
         } else {
            msg = new LonMessage(this.getMsgInputStream());
         }
      } catch (Throwable var4) {
         throw new LonException("Unable to create LonMessage.", var4);
      }

      this.setMessageAttributes(msg);
      return msg;
   }

   private void setMessageAttributes(LonMessage msg) {
      msg.setRequest(this.isRequest());
      msg.setPriority(this.isPriority());
      msg.setSourceAddress(this.getSourceAddress());
      msg.setTag(this.getTag());
      msg.setDomainIndex(this.getDomainIndex());
   }

   public LonMessage makeResponse(LonMessage lonRequest) throws LonException {
      LonMessage rsp = lonRequest.toResponse(this.getMsgInputStream());
      this.setMessageAttributes(rsp);
      return rsp;
   }

   private LonInputStream getMsgInputStream() {
      if (this.inputStream == null) {
         this.inputStream = new NAppBuffer.AppBuffInputStream(this.buf, this.getDataLength());
      } else {
         this.inputStream.init(this.buf, this.getDataLength());
      }

      return this.inputStream;
   }

   private LonOutputStream getMsgOutputStream() {
      if (this.outputStream == null) {
         this.outputStream = new NAppBuffer.AppBuffOutputStream(this.buf);
      } else {
         this.outputStream.init(this.buf);
      }

      return this.outputStream;
   }

   private int getByte(int index) {
      return this.buf[index] & 0xFF;
   }

   @Override
   public String toString() {
      return LonByteArrayUtil.toString(this.buf, this.getWriteBufferLen());
   }

   public byte[] getReadBuffer() {
      return this.buf;
   }

   byte[] getWriteBuffer() {
      if (!this.localCommand) {
         this.setBufferLength(this.getDataLength() + 3 + 11);
      }

      return this.buf;
   }

   public int getWriteBufferLen() {
      return this.localCommand ? 2 : 16 + this.getDataLength();
   }

   public byte[] toNetworkBytes() {
      byte[] a = new byte[this.getWriteBufferLen()];
      System.arraycopy(this.buf, 0, a, 0, a.length);
      return a;
   }

   public void writeMessage(LonOutputStream out) {
      if (this.farSide) {
         out.writeUnsigned8(126);
      }

      int hdrLen = 16;
      int msgLen = this.getDataLength();
      out.write(this.buf, hdrLen, msgLen);
   }

   public void readMessage(LonInputStream in, int msgLen) {
      this.farSide = in.read() == 126;
      if (this.farSide) {
         msgLen--;
      } else {
         in.reset(in.position() - 1);
      }

      this.setDataLength(msgLen);
      int pos = 16;

      for (int end = pos + msgLen; pos < end; pos++) {
         this.buf[pos] = (byte)in.read();
      }
   }

   @Override
   public LinkedQueue.Linkable getNext() {
      return this.next;
   }

   @Override
   public void setNext(LinkedQueue.Linkable nxt) {
      this.next = (NAppBuffer)nxt;
   }

   @Override
   public void setInQueue(boolean v) {
      this.inQueue = v;
   }

   @Override
   public boolean getInQueue() {
      return this.inQueue;
   }

   public void setNoTransaction(boolean b) {
      this.noTransaction = b;
   }

   public boolean isNoTransaction() {
      return this.noTransaction;
   }

   public static NAppBuffer makeAppBuffer() {
      NAppBuffer ab;
      synchronized (appPool) {
         if (appCnt > 0) {
            ab = appPool[--appCnt];
         } else {
            ab = new NAppBuffer();
            appInstanceCnt++;
         }

         ab.freeBuf = false;
      }

      for (int i = 0; i < ab.buf.length; i++) {
         ab.buf[i] = 0;
      }

      ab.nextAppBuffer = null;
      ab.exception = null;
      ab.localCommand = false;
      ab.retryCountSet = false;
      ab.repeatTimerSet = false;
      ab.transmitTimerSet = false;
      ab.noTransaction = false;
      ab.farSide = false;
      ab.repeatTimer = null;
      ab.transmitTimer = null;
      ab.serviceType = null;
      ab.retryCount = 0;
      return ab;
   }

   public static NAppBuffer makeAppBuffer(byte[] a) {
      NAppBuffer ab = makeAppBuffer();
      System.arraycopy(a, 0, ab.buf, 0, a.length);
      return ab;
   }

   public void releaseAppBuffer() {
      if (!this.getInQueue()) {
         synchronized (appPool) {
            for (NAppBuffer ab = this; ab != null; ab = ab.nextAppBuffer) {
               if (ab.freeBuf) {
                  System.out.println("already free ");
                  Thread.dumpStack();
                  return;
               }

               if (appCnt < appPool.length) {
                  appPool[appCnt++] = ab;
               }

               ab.freeBuf = true;
            }
         }
      }
   }

   public static void spy(SpyWriter out) throws Exception {
      out.trTitle("Application Buffers", 1);
      out.startProps("Application Buffers");
      out.prop("App Buffers created", appInstanceCnt);
      out.prop("App Buffers available", appCnt);
      out.endProps();
   }

   static class AppBuffInputStream extends LonInputStream {
      AppBuffInputStream(byte[] a, int msgLen) {
         super(a);
         this.init(a, msgLen);
      }

      private void init(byte[] a, int msgLen) {
         this.buf = a;
         this.pos = 16;
         this.count = 16 + msgLen;
      }

      @Override
      public void reset() {
         this.pos = 16;
      }
   }

   static class AppBuffOutputStream extends LonOutputStream {
      AppBuffOutputStream(byte[] a) {
         this.init(a);
      }

      private void init(byte[] a) {
         this.buf = a;
         this.count = 16;
      }
   }
}
