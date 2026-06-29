package com.tridium.lonworks.loncomm;

import javax.baja.lonworks.LonMessage;

public class RawMessage extends LonMessage {
   byte[] rawData;

   RawMessage(byte[] a, int len) {
      this.rawData = new byte[len];
      System.arraycopy(a, 0, this.rawData, 0, len);
   }

   public byte[] getRawData() {
      return this.rawData;
   }
}
