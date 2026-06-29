package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class TableDownload extends LonMessage implements NetMessages {
   private int groupOrSubnet;
   private int domainIndex;
   private int index;
   private byte[] table = null;

   public TableDownload() {
      this.code = 118;
   }

   public TableDownload(int groupOrSubnet, int domainIndex, int index, byte[] a) {
      this.code = 118;
      this.groupOrSubnet = groupOrSubnet;
      this.domainIndex = domainIndex;
      this.index = index;
      this.table = new byte[8];
      System.arraycopy(a, 0, this.table, 0, 8);
   }

   public int getGroupOrSubnet() {
      return this.groupOrSubnet;
   }

   @Override
   public int getDomainIndex() {
      return this.domainIndex;
   }

   public int getIndex() {
      return this.index;
   }

   public byte[] getTable() {
      return this.table;
   }

   public void setGroupOrSubnet(int groupOrSubnet) {
      this.groupOrSubnet = groupOrSubnet;
   }

   @Override
   public void setDomainIndex(int domainIndex) {
      this.domainIndex = domainIndex;
   }

   public void setIndex(int index) {
      this.index = index;
   }

   public void setTable(byte[] table) {
      this.table = table;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      int workingInt = (1 & this.groupOrSubnet) << 7 | (1 & this.domainIndex) << 6 | 3 & this.index;
      out.write(workingInt);
      out.write(this.table, 0, 8);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 22) {
         throw new FailedResponseException();
      } else if (code != 54) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(54);
      }
   }
}
