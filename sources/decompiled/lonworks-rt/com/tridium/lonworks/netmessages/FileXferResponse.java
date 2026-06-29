package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonMessage;

public class FileXferResponse extends LonMessage implements NetMessages {
   public FileXferResponse(int code) {
      this.code = code;
   }

   @Override
   public String toString() {
      return "code = " + this.code;
   }

   public int getNextPacket() {
      return this.code;
   }

   public void setNextPacket(int nextPacket) {
      this.code = nextPacket;
   }
}
