package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.io.LonOutputStream;

public class ExUpdateAliasRequest extends ExNetMgmtCommand {
   private int aliasIndex;
   private BAliasConfigData aliasConfigData = null;
   private int tgtNvIndex = 65535;
   private boolean nvWriteByIndex = true;
   private boolean nvRemoteNmAuth = false;
   private boolean nvReadByIndex = true;

   public ExUpdateAliasRequest() {
      this.code = 112;
      this.setAppCommand(37);
      this.setResource(6);
   }

   public ExUpdateAliasRequest(int aliasIndex, BAliasConfigData aliasConfigData) {
      this();
      this.aliasIndex = aliasIndex;
      this.aliasConfigData = aliasConfigData;
   }

   public void setAliasIndex(int aliasIndex) {
      this.aliasIndex = aliasIndex;
   }

   public void setTgtNvIndex(int tgtNvIndex) {
      this.tgtNvIndex = tgtNvIndex;
   }

   public void setAliasConfigData(BAliasConfigData aliasConfigData) {
      this.aliasConfigData = aliasConfigData;
   }

   public void setAliasWriteByIndex(boolean nvWriteByIndex) {
      this.nvWriteByIndex = nvWriteByIndex;
   }

   public void setAliasRemoteAliasAuth(boolean nvRemoteNmAuth) {
      this.nvRemoteNmAuth = nvRemoteNmAuth;
   }

   public void setAliasReadByIndex(boolean nvReadByIndex) {
      this.nvReadByIndex = nvReadByIndex;
   }

   @Override
   public void writeMessageData(LonOutputStream out) {
      out.writeUnsigned16(this.aliasIndex);
      int mrk = out.setBitFieldMark();
      out.writeBooleanBit(this.aliasConfigData.getPriority(), 0, 7, 1);
      out.writeBooleanBit(this.aliasConfigData.isOutput(), 0, 6, 1);
      int sel = this.aliasConfigData.getSelector();
      out.writeBit(sel >> 8, 0, 0, 6);
      out.writeUnsigned8(sel);
      out.writeBooleanBit(this.aliasConfigData.getTurnAround(), 2, 7, 1);
      out.writeBooleanBit(this.aliasConfigData.getAuthenticated(), 2, 6, 1);
      out.writeBooleanBit(this.nvWriteByIndex, 2, 5, 1);
      out.writeBooleanBit(this.nvRemoteNmAuth, 2, 4, 1);
      out.writeBooleanBit(this.nvReadByIndex, 3, 7, 1);
      out.writeBit(this.aliasConfigData.getServiceType().getOrdinal(), 3, 5, 2);
      out.writeUnsigned16(this.aliasConfigData.getAddrIndex());
      out.writeUnsigned16(this.tgtNvIndex);
      out.writeUnsigned16(this.aliasConfigData.getPrimary());
      out.resetBitFieldMark(mrk);
   }
}
