package com.tridium.modbusCore.messages;

import com.tridium.basicdriver.message.ReceivedMessage;
import javax.baja.nre.util.ByteArrayUtil;

public class ModbusReceivedMessage extends ReceivedMessage {
   private byte[] data;
   private int len;
   private int transactionId = 0;

   public ModbusReceivedMessage(byte[] data, int len) {
      this.data = data;
      this.len = len;
   }

   public byte[] getBytes() {
      return this.data;
   }

   public void setBytes(byte[] data) {
      this.data = data;
   }

   public int getLength() {
      return this.len;
   }

   public void setLength(int len) {
      this.len = len;
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(ByteArrayUtil.toHexString(this.data, 0, this.len));
      sb.append(" (tId = ").append(this.transactionId).append(")");
      return sb.toString();
   }

   public void setTransactionId(int transactionId) {
      this.transactionId = transactionId;
   }

   public int getTransactionId() {
      return this.transactionId;
   }
}
