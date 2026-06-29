package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.enums.BLonGroupRestrictionEnum;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class ExEnumerateAddressResponse extends QueryAddrResponse implements NetMessages {
   public int addressIndex;

   public ExEnumerateAddressResponse(LonInputStream in) throws LonException {
      this.code = 48;
      this.fromInputStream(in);
   }

   @Override
   public boolean isExtended() {
      return true;
   }

   public int getAddressIndex() {
      return this.addressIndex;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      throw new RuntimeException("ExEnumerateAddressResponse.toOutputStream not implemented");
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 48) {
         throw new InvalidResponseException(code);
      } else {
         this.addressIndex = in.readUnsigned16();
         in.setBitFieldMark();
         this.addrType = in.readUnsigned8();
         if ((this.addrType & 128) != 0) {
            this.restriction = BLonGroupRestrictionEnum.make(in.readBit(1, 6, 2));
            this.memberOrNode = in.readBit(1, 0, 6);
         } else {
            this.memberOrNode = in.readBit(1, 0, 7);
         }

         this.repeatTimer = in.readBit(2, 4, 4);
         this.retryCount = in.readBit(2, 0, 4);
         this.receiveTimer = in.readBit(3, 4, 4);
         this.xmitTimer = in.readBit(3, 0, 4);
         this.groupOrSubnet = in.readUnsigned8();
         this.domainIndex = in.readUnsigned16();
      }
   }
}
