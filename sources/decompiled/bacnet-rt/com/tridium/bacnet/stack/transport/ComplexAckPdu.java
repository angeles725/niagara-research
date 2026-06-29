package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.util.ByteArrayUtil;

public class ComplexAckPdu extends ApplicationPdu {
   public static final int SEG_FLAG_MASK = 8;
   public static final int MOR_FLAG_MASK = 4;
   public static final int SEG_FLAG = 8;
   public static final int MOR_FLAG = 4;
   private boolean segmentedMessage;
   private boolean moreFollows;
   private int originalInvokeId;
   private int sequenceNumber;
   private int proposedWindowSize;
   private int serviceAckChoice;
   private byte[] serviceAck;
   private int segmentLength = -1;
   private int segmentOffset = -1;

   public ComplexAckPdu(BBacnetAddress clientAddress, BNetworkPriority networkPriority, int originalInvokeId, int serviceAckChoice, byte[] serviceAck) {
      super(clientAddress, null, networkPriority, 3);
      this.serviceAckChoice = serviceAckChoice;
      this.originalInvokeId = originalInvokeId;
      this.serviceAck = serviceAck;
   }

   public ComplexAckPdu(ComplexAckPdu complexAck, int sequenceNumber, int segmentSize, int segmentCounter) {
      super(complexAck.getClientAddress(), null, complexAck.getPriority(), 3);
      this.serviceAckChoice = complexAck.serviceAckChoice;
      this.originalInvokeId = complexAck.originalInvokeId;
      this.serviceAck = complexAck.serviceAck;
      this.segmentLength = segmentSize;
      this.segmentOffset = (segmentSize - 5) * segmentCounter;
      this.sequenceNumber = sequenceNumber;
   }

   public ComplexAckPdu(ByteArrayInputStream is) throws BacnetStackException {
      super(3);
      this.readNetworkBytes(is);
   }

   public int getLength() {
      return this.serviceAck.length;
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

   @Override
   public int getInvokeId() {
      return this.originalInvokeId;
   }

   public void setInvokeId(int originalInvokeId) {
      this.originalInvokeId = originalInvokeId;
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

   public int getServiceAckChoice() {
      return this.serviceAckChoice;
   }

   public byte[] getServiceAck() {
      return this.serviceAck;
   }

   public void appendSegment(ComplexAckPdu segment) {
      byte[] seg = segment.serviceAck;
      byte[] x = new byte[this.serviceAck.length + seg.length];
      System.arraycopy(this.serviceAck, 0, x, 0, this.serviceAck.length);
      System.arraycopy(seg, 0, x, this.serviceAck.length, seg.length);
      this.serviceAck = x;
   }

   @Override
   public boolean isServerPDU() {
      return true;
   }

   @Override
   public boolean isClientPDU() {
      return false;
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      int value = 48;
      if (this.segmentedMessage) {
         value |= 8;
      }

      if (this.moreFollows) {
         value |= 4;
      }

      os.write(value);
      os.write(this.originalInvokeId);
      if (this.segmentedMessage) {
         os.write(this.sequenceNumber);
         os.write(this.proposedWindowSize);
      }

      os.write(this.serviceAckChoice);
      if (this.segmentLength == -1) {
         os.write(this.serviceAck, 0, this.serviceAck.length);
      } else {
         int len;
         if (this.segmentOffset + this.segmentLength - 5 < this.serviceAck.length) {
            len = this.segmentLength - 5;
         } else {
            len = this.serviceAck.length - this.segmentOffset;
         }

         os.write(this.serviceAck, this.segmentOffset, len);
      }
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int temp = is.read();
      int pduType = (temp & 240) >> 4;
      if (pduType != 3) {
         throw new InvalidApduTypeException(pduType);
      } else {
         if ((temp & 8) == 8) {
            this.segmentedMessage = true;
         }

         if ((temp & 4) == 4) {
            this.moreFollows = true;
         }

         this.originalInvokeId = is.read();
         if (this.segmentedMessage) {
            this.sequenceNumber = is.read();
            this.proposedWindowSize = is.read();
         }

         this.serviceAckChoice = is.read();
         this.serviceAck = new byte[is.available()];
         is.read(this.serviceAck, 0, this.serviceAck.length);
      }
   }

   @Override
   public String toString() {
      return this.msg();
   }

   @Override
   public String trace() {
      StringBuilder sb = new StringBuilder("Complex-ACK-PDU:");
      sb.append("\n\tSEG = " + this.segmentedMessage);
      sb.append("\n\tMOR = " + this.moreFollows);
      sb.append("\n\tOrig Invoke ID = " + this.originalInvokeId);
      if (this.segmentedMessage) {
         sb.append("\n\tSeq # = " + this.sequenceNumber);
         sb.append("\n\tPropWinSize = " + this.proposedWindowSize);
      }

      sb.append("\n\tServiceAck Choice = " + BacnetConfirmedServiceChoice.TAGS[this.serviceAckChoice]);
      if (this.serviceAck != null) {
         sb.append("\n\tService Ack (length) = ").append(this.serviceAck.length).append("\n\t");
         if (this.segmentedMessage) {
            sb.append(ByteArrayUtil.toHexString(this.serviceAck, this.segmentOffset, this.segmentLength));
         } else {
            sb.append(ByteArrayUtil.toHexString(this.serviceAck));
         }
      }

      this.addAPDUData(sb);
      return sb.toString();
   }

   public String msg() {
      StringBuilder sb = new StringBuilder("ComplexAckPDU:");
      sb.append(" originalInvokeId: ").append(this.originalInvokeId);
      sb.append(" segmented? ").append(this.segmentedMessage);
      sb.append(" moreFollows? ").append(this.moreFollows);
      if (this.segmentedMessage) {
         sb.append(" sequenceNumber: ").append(this.sequenceNumber).append(" proposedWindowSize: ").append(this.proposedWindowSize);
      }

      sb.append(" serviceAckChoice: ").append(BacnetConfirmedServiceChoice.TAGS[this.serviceAckChoice]);
      this.addAPDUMsg(sb);
      return sb.toString();
   }
}
