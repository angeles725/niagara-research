package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public class UnconfirmedRequestPdu extends ApplicationPdu {
   private int serviceChoice;
   private byte[] serviceRequest;

   public UnconfirmedRequestPdu(BBacnetAddress serverAddress, BNetworkPriority networkPriority, int serviceChoice, byte[] serviceRequest) {
      super(null, serverAddress, networkPriority, 1);
      this.serviceChoice = serviceChoice;
      this.serviceRequest = serviceRequest;
   }

   public UnconfirmedRequestPdu(ByteArrayInputStream is) throws BacnetStackException {
      super(1);
      this.readNetworkBytes(is);
   }

   public int getServiceChoice() {
      return this.serviceChoice;
   }

   public byte[] getServiceRequest() {
      return this.serviceRequest;
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
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      os.write(16);
      os.write(this.serviceChoice);
      os.write(this.serviceRequest, 0, this.serviceRequest.length);
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int pduType = (is.read() & 240) >> 4;
      if (pduType != 1) {
         throw new InvalidApduTypeException(1, pduType);
      } else {
         this.serviceChoice = is.read();
         this.serviceRequest = new byte[is.available()];
         is.read(this.serviceRequest, 0, this.serviceRequest.length);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("UnconfirmedRequestPdu:");
      sb.append("\n\tService Choice = " + BacnetUnconfirmedServiceChoice.TAGS[this.serviceChoice]);
      if (this.serviceRequest != null) {
         sb.append("\n\tService Request (length) = " + this.serviceRequest.length);
      }

      return sb.toString();
   }
}
