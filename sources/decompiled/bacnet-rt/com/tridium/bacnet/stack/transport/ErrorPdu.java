package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.util.ByteArrayUtil;

public class ErrorPdu extends ApplicationPdu {
   private int originalInvokeId;
   private int errorChoice;
   private byte[] error;

   public ErrorPdu() {
      super(5);
   }

   public ErrorPdu(BBacnetAddress sourceAddress, BNetworkPriority networkPriority, int errorChoice, int originalInvokeId, byte[] error) {
      super(sourceAddress, null, networkPriority, 5);
      this.originalInvokeId = originalInvokeId;
      this.errorChoice = errorChoice;
      this.error = error;
   }

   public ErrorPdu(ByteArrayInputStream is) throws BacnetStackException {
      super(5);
      this.readNetworkBytes(is);
   }

   public int getOriginalInvokeId() {
      return this.originalInvokeId;
   }

   public int getErrorChoice() {
      return this.errorChoice;
   }

   public byte[] getError() {
      return this.error;
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
      os.write(80);
      os.write(this.originalInvokeId);
      os.write(this.errorChoice);
      os.write(this.error, 0, this.error.length);
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int byte0 = is.read();
      int pduType = (byte0 & 240) >> 4;
      if (pduType != 5) {
         throw new InvalidApduTypeException(5, pduType);
      } else {
         this.originalInvokeId = is.read();
         this.errorChoice = is.read();
         this.error = new byte[is.available()];
         is.read(this.error, 0, this.error.length);
      }
   }

   @Override
   public String toString() {
      return this.msg();
   }

   @Override
   public String trace() {
      StringBuilder sb = new StringBuilder("ErrorPdu:");
      sb.append("\n\tOriginalInvoke ID = " + this.originalInvokeId);
      sb.append("\n\tErrorChoice = " + BacnetConfirmedServiceChoice.TAGS[this.errorChoice]);
      if (this.error != null) {
         sb.append("\n\terror (length) = " + this.error.length);
      }

      this.addAPDUData(sb);
      return sb.toString();
   }

   public String msg() {
      StringBuilder sb = new StringBuilder("ErrorPDU:");
      sb.append(" originalInvokeId: ").append(this.originalInvokeId);
      sb.append(" errorChoice: ").append(BacnetConfirmedServiceChoice.TAGS[this.errorChoice]);
      sb.append(" error: ").append(ByteArrayUtil.toHexString(this.error));
      this.addAPDUMsg(sb);
      return sb.toString();
   }
}
