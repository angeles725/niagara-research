package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.io.LonOutputStream;

public class ExUpdateNvConfigRequest extends ExNetMgmtCommand {
   private int nvIndex;
   private int tgtNvIndex = 65535;
   private BNvConfigData configData = null;
   private boolean nvWriteByIndex = false;
   private boolean nvRemoteNvAuth = false;
   private boolean nvReadByIndex = false;

   public ExUpdateNvConfigRequest() {
      this.code = 112;
      this.setAppCommand(37);
      this.setResource(5);
   }

   public ExUpdateNvConfigRequest(int nvIndex, BNvConfigData configData) {
      this();
      this.nvIndex = nvIndex;
      this.configData = configData;
   }

   public void setNvIndex(int nvIndex) {
      this.nvIndex = nvIndex;
   }

   public void setTgtNvIndex(int tgtNvIndex) {
      this.tgtNvIndex = tgtNvIndex;
   }

   public void setConfigData(BNvConfigData configData) {
      this.configData = configData;
   }

   public void setNvWriteByIndex(boolean nvWriteByIndex) {
      this.nvWriteByIndex = nvWriteByIndex;
   }

   public void setNvRemoteNvAuth(boolean nvRemoteNvAuth) {
      this.nvRemoteNvAuth = nvRemoteNvAuth;
   }

   public void setNvReadByIndex(boolean nvReadByIndex) {
      this.nvReadByIndex = nvReadByIndex;
   }

   @Override
   public void writeMessageData(LonOutputStream out) {
      out.writeUnsigned16(this.nvIndex);
      int mrk = out.setBitFieldMark();
      out.writeBooleanBit(this.configData.getPriority(), 0, 7, 1);
      out.writeBooleanBit(this.configData.isOutput(), 0, 6, 1);
      int sel = this.configData.getSelector();
      out.writeBit(sel >> 8, 0, 0, 6);
      out.writeUnsigned8(sel);
      out.writeBooleanBit(this.configData.getTurnAround(), 2, 7, 1);
      out.writeBooleanBit(this.configData.getAuthenticated(), 2, 6, 1);
      out.writeBooleanBit(this.nvWriteByIndex, 2, 5, 1);
      out.writeBooleanBit(this.nvRemoteNvAuth, 2, 4, 1);
      out.writeBooleanBit(this.nvReadByIndex, 3, 7, 1);
      out.writeBit(this.configData.getServiceType().getOrdinal(), 3, 5, 2);
      out.writeUnsigned16(this.configData.getAddrIndex());
      out.writeUnsigned16(this.tgtNvIndex);
      out.resetBitFieldMark(mrk);
   }
}
