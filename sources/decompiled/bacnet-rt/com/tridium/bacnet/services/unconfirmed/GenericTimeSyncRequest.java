package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BAbsTime;

public abstract class GenericTimeSyncRequest extends BacnetUnconfirmedRequest {
   private BBacnetDateTime dateTime;

   protected GenericTimeSyncRequest(int serviceChoice, BAbsTime bAbsTime) {
      super(serviceChoice);
      if (bAbsTime != null) {
         this.dateTime = new BBacnetDateTime(bAbsTime);
      }
   }

   public BBacnetDateTime getDateTime() {
      return this.dateTime;
   }

   public void setDateTime(BBacnetDateTime dateTime) {
      this.dateTime = dateTime;
   }

   public BAbsTime getBAbsTime() {
      return this.getDateTime().toBAbsTime();
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeDate(this.dateTime.getDate());
      outputStream.writeTime(this.dateTime.getTime());
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.dateTime = new BBacnetDateTime(inputStream.readDate(), inputStream.readTime());
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(BacnetUnconfirmedServiceChoice.TAGS[this.getServiceChoice()]);
      sb.append("\n  dateTime " + this.dateTime);
      return sb.toString();
   }
}
