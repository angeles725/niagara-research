package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class TableReportRequest extends LonMessage implements NetMessages {
   private int groupOrSubnet;
   private int domainIndex;
   private int ramOrEeprom;
   private int index;

   public TableReportRequest() {
      this.code = 123;
   }

   public TableReportRequest(int groupOrSubnet, int domainIndex, int ramOrEeprom, int index) {
      this.code = 123;
      this.groupOrSubnet = groupOrSubnet;
      this.domainIndex = domainIndex;
      this.ramOrEeprom = ramOrEeprom;
      this.index = index;
   }

   public int getGroupOrSubnet() {
      return this.groupOrSubnet;
   }

   @Override
   public int getDomainIndex() {
      return this.domainIndex;
   }

   public int getRamOrEeprom() {
      return this.ramOrEeprom;
   }

   public int getIndex() {
      return this.index;
   }

   public void setGroupOrSubnet(int groupOrSubnet) {
      this.groupOrSubnet = groupOrSubnet;
   }

   @Override
   public void setDomainIndex(int domainIndex) {
      this.domainIndex = domainIndex;
   }

   public void setRamOrEeprom(int ramOrEeprom) {
      this.ramOrEeprom = ramOrEeprom;
   }

   public void setIndex(int index) {
      this.index = index;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      int workingInt = (1 & this.groupOrSubnet) << 7 | (1 & this.domainIndex) << 6 | (1 & this.ramOrEeprom) << 5 | 3 & this.index;
      out.write(workingInt);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 27) {
         throw new FailedResponseException();
      } else if (code != 59) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new TableReportResponse(in);
      }
   }
}
