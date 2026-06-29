package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.datatypes.BExtAddressEntry;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.io.LonOutputStream;

public class ExUpdateAddressReq extends ExNetMgmtCommand {
   private int addrIndex;
   private BIAddressEntry entry;

   public ExUpdateAddressReq(int addrIndex, BIAddressEntry entry) {
      this.code = 112;
      this.setAppCommand(37);
      this.setResource(3);
      this.addrIndex = addrIndex;
      this.entry = entry;
   }

   public int getAddrIndex() {
      return this.addrIndex;
   }

   public void setAddrIndex(int addrIndex) {
      this.addrIndex = addrIndex;
   }

   @Override
   public void writeMessageData(LonOutputStream out) {
      out.writeUnsigned16(this.addrIndex);
      BExtAddressEntry.make(this.entry).toOutputStream(out);
   }
}
