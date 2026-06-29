package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.BacnetStackErrorCodes;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.util.ByteArrayUtil;

public class ConfirmedRequestPdu extends ApplicationPdu implements BacnetStackErrorCodes {
   public static final int SEG_FLAG = 8;
   public static final int MOR_FLAG = 4;
   public static final int SA_FLAG = 2;
   public static final int MAX_SEGS_MASK = 112;
   public static final int MAX_APDU_LEN_MASK = 15;
   public static final int SEG_HDR_LENGTH = 6;
   public static final int MAX_APDU_LENGTH_UP_TO_MIN_MSG_SIZE = 50;
   public static final int MAX_APDU_LENGTH_UP_TO_128_OCTETS = 128;
   public static final int MAX_APDU_LENGTH_UP_TO_206_OCTETS = 206;
   public static final int MAX_APDU_LENGTH_UP_TO_480_OCTETS = 480;
   public static final int MAX_APDU_LENGTH_UP_TO_1024_OCTETS = 1024;
   public static final int MAX_APDU_LENGTH_UP_TO_1476_OCTETS = 1476;
   private static final int[] MAX_APDU_LENGTHS = new int[]{50, 128, 206, 480, 1024, 1476};
   public static final int MAX_SEGS_UNSPECIFIED = 0;
   public static final int MAX_SEGS_2 = 2;
   public static final int MAX_SEGS_4 = 4;
   public static final int MAX_SEGS_8 = 8;
   public static final int MAX_SEGS_16 = 16;
   public static final int MAX_SEGS_32 = 32;
   public static final int MAX_SEGS_64 = 64;
   public static final int MAX_SEGS_GREATER_THAN_64 = 255;
   private static final int[] MAX_SEGS = new int[]{0, 2, 4, 8, 16, 32, 64, 255};
   private boolean segmentedMessage;
   private boolean moreFollows;
   private boolean segmentedResponseAccepted;
   private int maxSegmentsAccepted;
   private int maxAPDULengthAccepted;
   private int invokeId;
   private int sequenceNumber;
   private int proposedWindowSize;
   private int serviceChoice;
   private byte[] serviceRequest;
   private int segmentLength = -1;
   private int segmentOffset = -1;
   private int protocolRevision = 0;
   ApplicationPdu confirmationApdu = null;
   boolean timedOut = false;
   boolean abandon = false;
   boolean cannotSend = false;

   public ConfirmedRequestPdu(
      BBacnetAddress clientAddress,
      BBacnetAddress serverAddress,
      BNetworkPriority networkPriority,
      int invokeId,
      int serviceChoice,
      byte[] serviceRequest,
      int protocolRevision
   ) {
      super(clientAddress, serverAddress, networkPriority, 0);
      this.setDataExpectingReply(true);
      this.invokeId = invokeId;
      this.serviceChoice = serviceChoice;
      this.serviceRequest = serviceRequest;
      this.protocolRevision = protocolRevision;
   }

   public ConfirmedRequestPdu(ConfirmedRequestPdu pdu, int sequenceNumber, int segmentSize, int segmentCounter) {
      super(null, pdu.getServerAddress(), pdu.getPriority(), 0);
      this.invokeId = pdu.invokeId;
      this.serviceChoice = pdu.serviceChoice;
      this.serviceRequest = pdu.serviceRequest;
      this.maxAPDULengthAccepted = pdu.maxAPDULengthAccepted;
      this.maxSegmentsAccepted = pdu.maxSegmentsAccepted;
      this.segmentedResponseAccepted = pdu.segmentedResponseAccepted;
      this.protocolRevision = pdu.protocolRevision;
      this.segmentedMessage = true;
      this.setDataExpectingReply(pdu.getDataExpectingReply());
      this.segmentLength = segmentSize;
      this.segmentOffset = (segmentSize - 6) * segmentCounter;
      this.sequenceNumber = sequenceNumber;
   }

   public ConfirmedRequestPdu(ByteArrayInputStream is) throws BacnetStackException {
      super(0);
      this.readNetworkBytes(is);
   }

   public int getLength() {
      return this.serviceRequest.length;
   }

   public boolean isSegmentedMessage() {
      return this.segmentedMessage;
   }

   public boolean getSegmentedMessage() {
      return this.segmentedMessage;
   }

   public void setSegmentedMessage(boolean segmentedMessage) {
      this.segmentedMessage = segmentedMessage;
   }

   public boolean isMoreFollows() {
      return this.moreFollows;
   }

   public boolean getMoreFollows() {
      return this.moreFollows;
   }

   public void setMoreFollows(boolean moreFollows) {
      this.moreFollows = moreFollows;
   }

   public boolean isSegmentedResponseAccepted() {
      return this.segmentedResponseAccepted;
   }

   public boolean getSegmentedResponseAccepted() {
      return this.segmentedResponseAccepted;
   }

   public void setSegmentedResponseAccepted(boolean segmentedResponseAccepted) {
      this.segmentedResponseAccepted = segmentedResponseAccepted;
   }

   public int getMaxAPDULengthAccepted() {
      return this.maxAPDULengthAccepted;
   }

   public void setMaxAPDULengthAccepted(int maxAPDULengthAccepted) {
      this.maxAPDULengthAccepted = maxAPDULengthAccepted;
   }

   public int getMaxSegmentsAccepted() {
      return this.maxSegmentsAccepted;
   }

   public void setMaxSegmentsAccepted(int maxSegmentsAccepted) {
      this.maxSegmentsAccepted = maxSegmentsAccepted;
   }

   @Override
   public int getInvokeId() {
      return this.invokeId;
   }

   public int getSequenceNumber() {
      return this.sequenceNumber;
   }

   public void setSequenceNumber(int sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
      if (this.segmentLength != -1) {
         this.segmentOffset = this.segmentLength * sequenceNumber;
      }
   }

   public int getProposedWindowSize() {
      return this.proposedWindowSize;
   }

   public void setProposedWindowSize(int proposedWindowSize) {
      this.proposedWindowSize = proposedWindowSize;
   }

   public int getServiceChoice() {
      return this.serviceChoice;
   }

   public byte[] getServiceRequest() {
      return this.serviceRequest;
   }

   public void appendSegment(ConfirmedRequestPdu segment) {
      byte[] seg = segment.serviceRequest;
      byte[] x = new byte[this.serviceRequest.length + seg.length];
      System.arraycopy(this.serviceRequest, 0, x, 0, this.serviceRequest.length);
      System.arraycopy(seg, 0, x, this.serviceRequest.length, seg.length);
      this.serviceRequest = x;
   }

   @Override
   public boolean isServerPDU() {
      return false;
   }

   @Override
   public boolean isClientPDU() {
      return true;
   }

   @Override
   public String toString() {
      return this.msg();
   }

   @Override
   public String trace() {
      StringBuilder sb = new StringBuilder("Confirmed-Request-PDU:");
      sb.append("\n\tSEG = " + this.segmentedMessage);
      sb.append("\n\tMOR = " + this.moreFollows);
      sb.append("\n\tSA = " + this.segmentedResponseAccepted);
      sb.append("\n\tMax Resp = " + this.maxAPDULengthAccepted);
      sb.append("\n\tInvoke ID = " + this.invokeId);
      if (this.segmentedMessage) {
         sb.append("\n\tSeq # = " + this.sequenceNumber);
         sb.append("\n\tPropWinSize = " + this.proposedWindowSize);
         sb.append("\n\tsegOff = " + this.segmentOffset);
         sb.append("\n\tsegLen = " + this.segmentLength);
      }

      sb.append("\n\tService Choice = " + BacnetConfirmedServiceChoice.TAGS[this.serviceChoice]);
      if (this.serviceRequest != null) {
         sb.append("\n\tService Request (length) = ").append(this.serviceRequest.length).append("\n\t");
         if (this.segmentedMessage) {
            sb.append(
               ByteArrayUtil.toHexString(this.serviceRequest, this.segmentOffset < 0 ? 0 : this.segmentOffset, this.segmentLength < 0 ? 0 : this.segmentLength)
            );
         } else {
            sb.append(ByteArrayUtil.toHexString(this.serviceRequest));
         }
      }

      this.addAPDUData(sb);
      return sb.toString();
   }

   public String msg() {
      StringBuilder sb = new StringBuilder("ConfReqPDU:");
      sb.append(" invokeId: ").append(this.invokeId);
      sb.append(" segmented? ").append(this.segmentedMessage);
      sb.append(" moreFollows? ").append(this.moreFollows);
      sb.append(" segmentedResponseAccepted? ").append(this.segmentedResponseAccepted);
      sb.append(" maxAPDULengthAccepted: ").append(this.maxAPDULengthAccepted);
      if (this.segmentedMessage) {
         sb.append(" sequenceNumber: ").append(this.sequenceNumber).append(" proposedWindowSize: ").append(this.proposedWindowSize);
      }

      sb.append(" serviceChoice: ").append(BacnetConfirmedServiceChoice.TAGS[this.serviceChoice]);
      this.addAPDUMsg(sb);
      return sb.toString();
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      int byte0 = 0;
      if (this.segmentedMessage) {
         byte0 |= 8;
      }

      if (this.moreFollows) {
         byte0 |= 4;
      }

      if (this.segmentedResponseAccepted) {
         byte0 |= 2;
      }

      os.write(byte0);
      int byte1 = 0;
      if (this.protocolRevision >= 2) {
         byte1 = getMaxSegsCode(this.maxSegmentsAccepted) << 4;
      }

      byte1 |= getMaxAPDULengthCode(this.maxAPDULengthAccepted);
      os.write(byte1);
      os.write(this.invokeId);
      if (this.segmentedMessage) {
         os.write(this.sequenceNumber);
         os.write(this.proposedWindowSize);
      }

      os.write(this.serviceChoice);
      if (this.segmentLength == -1) {
         os.write(this.serviceRequest, 0, this.serviceRequest.length);
      } else {
         int len;
         if (this.segmentOffset + this.segmentLength - 6 < this.serviceRequest.length) {
            len = this.segmentLength - 6;
         } else {
            len = this.serviceRequest.length - this.segmentOffset;
         }

         os.write(this.serviceRequest, this.segmentOffset, len);
      }
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int byte0 = is.read();
      int pduType = (byte0 & 240) >> 4;
      if (pduType != 0) {
         throw new InvalidApduTypeException(0, pduType);
      } else {
         if ((byte0 & 8) != 0) {
            this.segmentedMessage = true;
         }

         if ((byte0 & 4) != 0) {
            this.moreFollows = true;
         }

         if ((byte0 & 2) != 0) {
            this.segmentedResponseAccepted = true;
         }

         int byte1 = is.read();
         this.maxSegmentsAccepted = getMaxSegs((byte1 & 112) >> 4);
         this.maxAPDULengthAccepted = getMaxAPDULength(byte1 & 15);
         this.invokeId = is.read();
         if (this.segmentedMessage) {
            this.sequenceNumber = is.read();
            this.proposedWindowSize = is.read();
         }

         this.serviceChoice = is.read();
         this.serviceRequest = new byte[is.available()];
         is.read(this.serviceRequest, 0, this.serviceRequest.length);
      }
   }

   private static int getMaxAPDULengthCode(int maxAPDULength) {
      if (maxAPDULength >= 1476) {
         return 5;
      } else if (maxAPDULength >= 1024) {
         return 4;
      } else if (maxAPDULength >= 480) {
         return 3;
      } else if (maxAPDULength >= 206) {
         return 2;
      } else {
         return maxAPDULength >= 128 ? 1 : 0;
      }
   }

   private static int getMaxAPDULength(int maxAPDULengthCode) {
      return maxAPDULengthCode >= 0 && maxAPDULengthCode < MAX_APDU_LENGTHS.length ? MAX_APDU_LENGTHS[maxAPDULengthCode] : 0;
   }

   private static int getMaxSegsCode(int maxSegs) {
      if (maxSegs > 64) {
         return 7;
      } else if (maxSegs == 64) {
         return 6;
      } else if (maxSegs >= 32) {
         return 5;
      } else if (maxSegs >= 16) {
         return 4;
      } else if (maxSegs >= 8) {
         return 3;
      } else if (maxSegs >= 4) {
         return 2;
      } else {
         return maxSegs >= 2 ? 1 : 0;
      }
   }

   private static int getMaxSegs(int maxSegsCode) {
      return maxSegsCode >= 0 && maxSegsCode < MAX_SEGS.length ? MAX_SEGS[maxSegsCode] : 0;
   }

   public static boolean canFit(int maxSegs, int numSegs) {
      return maxSegs <= 0 ? true : maxSegs >= numSegs;
   }

   public ApplicationPdu waitForConfirmation() throws BacnetStackException {
      synchronized (this) {
         ApplicationPdu var10000;
         try {
            if (this.cannotSend) {
               throw new BacnetStackException("Cannot send packet: invoke ID " + this.invokeId);
            }

            if (this.confirmationApdu == null) {
               this.wait(this.getLockupThreshold());
            }

            if (this.confirmationApdu == null) {
               if (this.abandon) {
                  throw new TransactionException("abandoned: invoke ID " + this.invokeId);
               }

               if (this.cannotSend) {
                  throw new BacnetStackException("Cannot send packet: invoke ID " + this.invokeId);
               }

               if (!this.timedOut) {
                  throw new TransactionException("lockup: invoke ID " + this.invokeId);
               }

               throw new TransactionTimeoutException("timeout: invoke ID " + this.invokeId);
            }

            var10000 = this.confirmationApdu;
         } catch (InterruptedException var4) {
            logger.warning("Timeout due to interruption waiting for response!");
            throw new TransactionException("interrupted: invoke ID " + this.invokeId);
         }

         return var10000;
      }
   }

   public synchronized void postConfirmation(ApplicationPdu apdu) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Post confirmation: invokeId: " + apdu.getInvokeId() + " apdu: " + this);
      }

      this.confirmationApdu = apdu;
      this.notifyAll();
   }

   public synchronized void timeout() {
      logger.severe("Transaction Timed out! invokeId: " + this.invokeId + " apdu: " + this);
      this.timedOut = true;
      this.notifyAll();
   }

   public synchronized void abandon(Exception e) {
      logger.severe("Transaction abandoned! invokeId: " + this.invokeId + " apdu: " + this + " exception: " + e);
      this.abandon = true;
      this.notifyAll();
   }

   public synchronized void cannotSend(Exception e) {
      logger.severe("Cannot send packet: invoke ID  invokeId: " + this.invokeId + " apdu: " + this + " exception:" + e.getMessage());
      this.cannotSend = true;
      this.notifyAll();
   }

   private long getLockupThreshold() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getTransport().getLockupThreshold().getMillis();
   }
}
