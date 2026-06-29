package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.enums.BBacnetAbortReason;

public class AbortPdu extends ApplicationPdu {
   public static final int SERVER_BIT = 1;
   public static final int ABORT_PDU_LENGTH = 3;
   private int originalInvokeId;
   private int abortReason;
   private boolean server;

   public AbortPdu(BBacnetAddress serverAddress, BNetworkPriority networkPriority, int originalInvokeId, int abortReason, boolean server) {
      super(null, serverAddress, networkPriority, 7);
      this.originalInvokeId = originalInvokeId;
      this.abortReason = abortReason;
      this.server = server;
   }

   public AbortPdu(ByteArrayInputStream is) throws BacnetStackException {
      super(7);
      this.readNetworkBytes(is);
   }

   public int originalInvokeId() {
      return this.originalInvokeId;
   }

   public int getAbortReason() {
      return this.abortReason;
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
      int temp = 112;
      if (this.server) {
         temp |= 1;
      }

      os.write(temp);
      os.write(this.originalInvokeId);
      os.write(this.abortReason);
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int byte0 = is.read();
      int pduType = (byte0 & 240) >> 4;
      if (pduType != 7) {
         throw new InvalidApduTypeException(7, pduType);
      } else if (is.available() != 2) {
         throw new InvalidApduLengthException(is.available(), 2);
      } else {
         if ((byte0 & 1) != 0) {
            this.server = true;
         }

         this.originalInvokeId = is.read();
         this.abortReason = is.read();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("AbortPdu:");
      sb.append("\n\toriginalInvokeId: " + this.originalInvokeId);
      sb.append("\n\tserver: " + this.server);
      sb.append("\n\tabort reason: " + BBacnetAbortReason.tag(this.abortReason));
      return sb.toString();
   }
}
