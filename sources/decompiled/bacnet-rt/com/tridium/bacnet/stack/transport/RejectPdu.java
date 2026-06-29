package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.enums.BBacnetRejectReason;

public class RejectPdu extends ApplicationPdu {
   private int originalInvokeId;
   private int rejectReason;

   public RejectPdu(BBacnetAddress clientAddress, BNetworkPriority networkPriority, int originalInvokeId, int rejectReason) {
      super(clientAddress, null, networkPriority, 6);
      this.originalInvokeId = originalInvokeId;
      this.rejectReason = rejectReason;
   }

   public RejectPdu(ByteArrayInputStream is) throws BacnetStackException {
      super(6);
      this.readNetworkBytes(is);
   }

   public int originalInvokeId() {
      return this.originalInvokeId;
   }

   public int getRejectReason() {
      return this.rejectReason;
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
   public int getInvokeId() {
      return this.originalInvokeId;
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      os.write(96);
      os.write(this.originalInvokeId);
      os.write(this.rejectReason);
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int pduType = (is.read() & 240) >> 4;
      if (pduType != 6) {
         throw new InvalidApduTypeException(6, pduType);
      } else {
         this.originalInvokeId = is.read();
         this.rejectReason = is.read();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("RejectPdu:");
      sb.append("\n\toriginalInvokeId= " + this.originalInvokeId);
      sb.append("\n\treject reason= " + BBacnetRejectReason.tag(this.rejectReason));
      this.addAPDUData(sb);
      return sb.toString();
   }
}
