package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class SubnetForward extends LonMessage implements NetMessages {
   private int domain;
   private int ramOrEeprom;
   private int subnet;

   public SubnetForward() {
      this.code = 120;
   }

   public SubnetForward(int domain, int ramOrEeprom, int subnet) {
      this.code = 120;
      this.domain = domain;
      this.ramOrEeprom = ramOrEeprom;
      this.subnet = subnet;
   }

   public int getDomain() {
      return this.domain;
   }

   public int getRamOrEeprom() {
      return this.ramOrEeprom;
   }

   public int getSubnet() {
      return this.subnet;
   }

   public void setDomain(int domain) {
      this.domain = domain;
   }

   public void setRamOrEeprom(int ramOrEeprom) {
      this.ramOrEeprom = ramOrEeprom;
   }

   public void setSubnet(int subnet) {
      this.subnet = subnet;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.subnet);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 24) {
         throw new FailedResponseException();
      } else if (code != 56) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(56);
      }
   }
}
