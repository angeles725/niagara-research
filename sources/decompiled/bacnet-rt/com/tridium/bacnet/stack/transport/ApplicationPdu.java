package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public abstract class ApplicationPdu implements Runnable {
   public static final int CONFIRMED_REQUEST = 0;
   public static final int UNCONFIRMED_REQUEST = 1;
   public static final int SIMPLE_ACK = 2;
   public static final int COMPLEX_ACK = 3;
   public static final int SEGMENT_ACK = 4;
   public static final int ERROR = 5;
   public static final int REJECT = 6;
   public static final int ABORT = 7;
   private static final int PDU_TYPE_MASK = 240;
   public static final int APPLICATION = 0;
   public static final int NETWORK = 1;
   public static final int REQUEST_TIMEOUT = 2;
   public static final int SEGMENT_TIMEOUT = 3;
   public static final Logger logger = Logger.getLogger("bacnet.transport");
   private int type;
   private int source;
   private BBacnetAddress clientAddress;
   private BBacnetAddress serverAddress;
   private BNetworkPriority priority;
   private boolean dataExpectingReply;
   private boolean isBroadcast;
   private TransportStateMachine tsm;

   protected ApplicationPdu(int type) {
      this.type = type;
      this.priority = BNetworkPriority.normal;
   }

   protected ApplicationPdu(BBacnetAddress clientAddress, BBacnetAddress serverAddress, BNetworkPriority networkPriority, int type) {
      this.clientAddress = clientAddress;
      this.serverAddress = serverAddress;
      this.priority = networkPriority;
      this.type = type;
   }

   @Override
   public void run() {
      if (this.tsm != null) {
         if (logger.isLoggable(Level.FINE)) {
            switch (this.source) {
               case 0:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(this.tsm.getName() + ":application msg processed: " + this);
                  }
                  break;
               case 1:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(this.tsm.getName() + ":network msg processed: " + this);
                  }
                  break;
               case 2:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(this.tsm.getName() + ":request timeout processed: " + this);
                  }
                  break;
               case 3:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(this.tsm.getName() + ":segment timeout processed: " + this);
                  }
            }
         }

         this.tsm.route(this);
      }
   }

   public int getType() {
      return this.type;
   }

   public int getSource() {
      return this.source;
   }

   public void setSource(int source) {
      this.source = source;
   }

   public int getInvokeId() {
      return -1;
   }

   public BBacnetAddress getClientAddress() {
      return this.clientAddress;
   }

   public BBacnetAddress getServerAddress() {
      return this.serverAddress;
   }

   public BNetworkPriority getPriority() {
      return this.priority;
   }

   public void setPriority(BNetworkPriority priority) {
      this.priority = priority;
   }

   public boolean getDataExpectingReply() {
      return this.dataExpectingReply;
   }

   public void setDataExpectingReply(boolean der) {
      this.dataExpectingReply = der;
   }

   public abstract boolean isServerPDU();

   public abstract boolean isClientPDU();

   public abstract void writeNetworkBytes(ByteArrayOutputStream var1);

   public TransportStateMachine getTSM() {
      return this.tsm;
   }

   public void setTSM(TransportStateMachine tsm) {
      this.tsm = tsm;
   }

   public static ApplicationPdu parseEncodedBytes(
      BBacnetAddress srcAddress,
      BBacnetAddress destAddress,
      ByteArrayInputStream rawApdu,
      BNetworkPriority priority,
      boolean dataExpectingReply,
      boolean isBroadcast
   ) throws BacnetStackException {
      ApplicationPdu pdu = null;
      rawApdu.mark(0);
      int pduType = (rawApdu.read() & 240) >> 4;
      rawApdu.reset();
      switch (pduType) {
         case 0:
            pdu = new ConfirmedRequestPdu(rawApdu);
            break;
         case 1:
            pdu = new UnconfirmedRequestPdu(rawApdu);
            break;
         case 2:
            pdu = new SimpleAckPdu(rawApdu);
            break;
         case 3:
            pdu = new ComplexAckPdu(rawApdu);
            break;
         case 4:
            pdu = new SegmentAckPdu(rawApdu);
            break;
         case 5:
            pdu = new ErrorPdu(rawApdu);
            break;
         case 6:
            pdu = new RejectPdu(rawApdu);
            break;
         case 7:
            pdu = new AbortPdu(rawApdu);
            break;
         default:
            throw new InvalidApduTypeException(pduType);
      }

      if (pdu.isServerPDU()) {
         pdu.clientAddress = destAddress;
         pdu.serverAddress = srcAddress;
      } else {
         pdu.clientAddress = srcAddress;
         pdu.serverAddress = destAddress;
      }

      pdu.priority = priority;
      pdu.dataExpectingReply = dataExpectingReply;
      pdu.isBroadcast = isBroadcast;
      return pdu;
   }

   public boolean isBroadcast() {
      return this.isBroadcast;
   }

   @Override
   public String toString() {
      return "ApplicationPdu " + this.type;
   }

   public String trace() {
      return this.toString();
   }

   @Deprecated
   protected void addAPDUData(StringBuffer sb) {
      sb.append("\n\tcadr=" + this.clientAddress)
         .append("\n\tsadr=" + this.serverAddress)
         .append("\n\tsrc=" + this.source)
         .append("\n\tpri=" + this.priority)
         .append("\n\tder=" + this.dataExpectingReply);
   }

   @Deprecated
   protected void addAPDUMsg(StringBuffer sb) {
      sb.append(" c" + this.clientAddress).append(" s" + this.serverAddress).append(' ').append(this.source);
   }

   protected void addAPDUData(StringBuilder sb) {
      sb.append("\n\tclientAddress= " + this.clientAddress)
         .append("\n\tserverAddress= " + this.serverAddress)
         .append("\n\tsource= " + this.getSourceTag())
         .append("\n\tpriority= " + this.priority)
         .append("\n\tdataExpectingReply= " + this.dataExpectingReply);
   }

   protected void addAPDUMsg(StringBuilder sb) {
      sb.append(" clientAddress: ")
         .append(this.clientAddress)
         .append(" serverAddress: ")
         .append(this.serverAddress)
         .append(" source: ")
         .append(this.getSourceTag());
   }

   private String getSourceTag() {
      switch (this.source) {
         case 0:
            return "application";
         case 1:
            return "network";
         case 2:
            return "requestTimeout";
         case 3:
            return "segmentTimeout";
         default:
            return String.valueOf(this.source);
      }
   }
}
