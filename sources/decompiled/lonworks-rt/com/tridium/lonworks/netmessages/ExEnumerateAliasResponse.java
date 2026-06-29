package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.io.LonInputStream;

public class ExEnumerateAliasResponse extends QueryAliasResponse implements NetMessages {
   int aliasIndex;
   int tgtNvIndex;
   boolean nvWriteByIndex;
   boolean nvRemoteNvAuth;
   boolean nvReadByIndex;

   public ExEnumerateAliasResponse(LonInputStream in) throws LonException {
      this.fromInputStream(in);
   }

   @Override
   public boolean isExtended() {
      return true;
   }

   public int getAliasIndex() {
      return this.aliasIndex;
   }

   public int getTgtNvIndex() {
      return this.tgtNvIndex;
   }

   public boolean getNvWriteByIndex() {
      return this.nvWriteByIndex;
   }

   public boolean getNvRemoteNvAuth() {
      return this.nvRemoteNvAuth;
   }

   public boolean getNvReadByIndex() {
      return this.nvReadByIndex;
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      this.code = in.readUnsigned8();
      if (this.code != 48) {
         throw new InvalidResponseException(this.code);
      } else {
         this.aliasIndex = in.readUnsigned16();
         int mrk = in.setBitFieldMark();
         if (this.aliasData == null) {
            this.aliasData = new BAliasConfigData();
         }

         this.aliasData.setPriority(in.readBooleanBit(0, 7, 1));
         BLonNvDirection dir = in.readBooleanBit(0, 6, 1) ? BLonNvDirection.output : BLonNvDirection.input;
         this.aliasData.setDirection(dir);
         this.aliasData.setSelector((in.readBit(0, 0, 6) << 8) + in.readUnsigned8());
         this.aliasData.setTurnAround(in.readBooleanBit(2, 7, 1));
         this.aliasData.setAuthenticated(in.readBooleanBit(2, 6, 1));
         this.nvWriteByIndex = in.readBooleanBit(2, 5, 1);
         this.nvRemoteNvAuth = in.readBooleanBit(2, 4, 1);
         this.nvReadByIndex = in.readBooleanBit(3, 7, 1);
         int ord = in.readBit(3, 5, 2);
         this.aliasData.setServiceType(BLonServiceType.make(ord));
         this.aliasData.setAddrIndex(in.readUnsigned16());
         this.tgtNvIndex = in.readUnsigned16();
         this.aliasData.setPrimary(in.readUnsigned16());
         in.resetBitFieldMark(mrk);
      }
   }
}
