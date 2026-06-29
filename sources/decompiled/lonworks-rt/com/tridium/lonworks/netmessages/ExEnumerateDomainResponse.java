package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.io.LonInputStream;

public class ExEnumerateDomainResponse extends QueryDomainResponse implements NetMessages {
   int index;

   public ExEnumerateDomainResponse(LonInputStream in) throws LonException {
      this.fromInputStream(in);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      this.code = in.readUnsigned8();
      if (this.code != 48) {
         throw new InvalidResponseException(this.code);
      } else {
         this.index = in.readUnsigned16();
         this.readMessageData(in);
      }
   }

   @Override
   public boolean isExtended() {
      return true;
   }

   public int getIndex() {
      return this.index;
   }
}
