package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class UpdateAddress extends LonMessage implements NetMessages {
   private int addrIndex;
   private BIAddressEntry entry;

   public UpdateAddress() {
      this(15, BAddressEntry.DEFAULT);
   }

   public UpdateAddress(int addrIndex, BIAddressEntry entry) {
      this.code = 102;
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
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.addrIndex);
      BAddressEntry.make(this.entry).toOutputStream(out);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 6) {
         throw new FailedResponseException();
      } else if (code != 38) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(38);
      }
   }
}
