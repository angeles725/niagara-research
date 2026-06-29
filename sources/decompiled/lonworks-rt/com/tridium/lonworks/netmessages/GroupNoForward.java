package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class GroupNoForward extends LonMessage implements NetMessages {
   private int domain;
   private int ramOrEeprom;
   private int group;

   public GroupNoForward() {
      this.code = 121;
   }

   public GroupNoForward(int domain, int ramOrEeprom, int group) {
      this.code = 121;
      this.domain = domain;
      this.ramOrEeprom = ramOrEeprom;
      this.group = group;
   }

   public int getDomain() {
      return this.domain;
   }

   public int getRamOrEeprom() {
      return this.ramOrEeprom;
   }

   public int getGroup() {
      return this.group;
   }

   public void setDomain(int domain) {
      this.domain = domain;
   }

   public void setRamOrEeprom(int ramOrEeprom) {
      this.ramOrEeprom = ramOrEeprom;
   }

   public void setGroup(int group) {
      this.group = group;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.group);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 25) {
         throw new FailedResponseException();
      } else if (code != 57) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(57);
      }
   }
}
