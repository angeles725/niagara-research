package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;

public class TableReportResponse extends LonMessage implements NetMessages {
   private byte[] table = new byte[8];

   public TableReportResponse() {
      this.code = 59;
   }

   public TableReportResponse(LonInputStream in) throws LonException {
      this.code = 59;

      try {
         this.fromInputStream(in);
      } catch (LonException var3) {
         throw var3;
      }
   }

   public byte[] getTable() {
      return this.table;
   }

   public void setTable(byte[] table) {
      this.table = table;
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 59) {
         throw new InvalidResponseException(code);
      } else {
         this.table = in.readByteArray(8);
      }
   }
}
