package com.tridium.bacnet.stack.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SegmentAckPdu extends ApplicationPdu {
   public static final int SERVER_BIT = 1;
   public static final int NEGATIVE_ACK_BIT = 2;
   private boolean negativeAck;
   private boolean server;
   private int originalInvokeId;
   private int sequenceNumber;
   private int actualWindowSize;

   public SegmentAckPdu() {
      super(4);
   }

   public SegmentAckPdu(boolean negativeAck, boolean server, int originalInvokeId, int sequenceNumber, int actualWindowSize) {
      super(4);
      this.negativeAck = negativeAck;
      this.server = server;
      this.originalInvokeId = originalInvokeId;
      this.sequenceNumber = sequenceNumber;
      this.actualWindowSize = actualWindowSize;
   }

   public SegmentAckPdu(ByteArrayInputStream is) {
      super(4);
      this.readNetworkBytes(is);
   }

   public boolean getNegativeAck() {
      return this.negativeAck;
   }

   public boolean getServer() {
      return this.server;
   }

   public int getOriginalInvokeId() {
      return this.originalInvokeId;
   }

   public int getSequenceNumber() {
      return this.sequenceNumber;
   }

   public int getActualWindowSize() {
      return this.actualWindowSize;
   }

   @Override
   public boolean isServerPDU() {
      return this.server;
   }

   @Override
   public boolean isClientPDU() {
      return !this.server;
   }

   @Override
   public int getInvokeId() {
      return this.originalInvokeId;
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      int byte0 = 64;
      if (this.server) {
         byte0 |= 1;
      }

      if (this.negativeAck) {
         byte0 |= 2;
      }

      os.write(byte0);
      os.write(this.originalInvokeId);
      os.write(this.sequenceNumber);
      os.write(this.actualWindowSize);
   }

   public void readNetworkBytes(ByteArrayInputStream is) {
      int byte0 = is.read();
      if ((byte0 & 1) != 0) {
         this.server = true;
      }

      if ((byte0 & 2) != 0) {
         this.negativeAck = true;
      }

      this.originalInvokeId = is.read();
      this.sequenceNumber = is.read();
      this.actualWindowSize = is.read();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("SegmentAckPdu:");
      sb.append("\n\tOriginalInvoke ID = " + this.originalInvokeId);
      sb.append("\n\tNAK? " + this.negativeAck);
      sb.append("\n\tserver? " + this.server);
      sb.append("\n\tSequence # = " + this.sequenceNumber);
      sb.append("\n\tActual Window Size = " + this.actualWindowSize);
      this.addAPDUData(sb);
      return sb.toString();
   }
}
