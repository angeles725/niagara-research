package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public class SimpleAckPdu extends ApplicationPdu {
   private int originalInvokeId;
   private int serviceAckChoice;

   public SimpleAckPdu() {
      super(2);
   }

   public SimpleAckPdu(BBacnetAddress sourceAddress, BNetworkPriority networkPriority, int originalInvokeId, int serviceAckChoice) {
      super(sourceAddress, null, networkPriority, 2);
      this.originalInvokeId = originalInvokeId;
      this.serviceAckChoice = serviceAckChoice;
   }

   public SimpleAckPdu(ByteArrayInputStream is) throws BacnetStackException {
      super(2);
      this.readNetworkBytes(is);
   }

   @Override
   public int getInvokeId() {
      return this.originalInvokeId;
   }

   public int getServiceAckChoice() {
      return this.serviceAckChoice;
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
      os.write(32);
      os.write(this.originalInvokeId);
      os.write(this.serviceAckChoice);
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int pduType = is.read() >> 4;
      if (pduType != 2) {
         throw new InvalidApduTypeException(2, pduType);
      } else {
         this.originalInvokeId = is.read();
         this.serviceAckChoice = is.read();
      }
   }

   @Override
   public String toString() {
      return this.msg();
   }

   @Override
   public String trace() {
      StringBuilder sb = new StringBuilder("SimpleAckPdu:");
      sb.append("\n\toriginalInvokeId = " + this.originalInvokeId);
      sb.append("\n\tserviceAckChoice = " + BacnetConfirmedServiceChoice.TAGS[this.serviceAckChoice]);
      this.addAPDUData(sb);
      return sb.toString();
   }

   public String msg() {
      StringBuilder sb = new StringBuilder("SimpleAckPDU:");
      sb.append(" originalInvokeId: ").append(this.originalInvokeId);
      sb.append(" serviceChoice: ").append(BacnetConfirmedServiceChoice.TAGS[this.serviceAckChoice]);
      this.addAPDUMsg(sb);
      return sb.toString();
   }
}
