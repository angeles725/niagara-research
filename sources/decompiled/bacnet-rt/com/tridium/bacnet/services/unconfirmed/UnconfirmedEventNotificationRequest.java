package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.EventNotificationParameters;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import javax.baja.bacnet.io.AsnException;

public class UnconfirmedEventNotificationRequest extends BacnetUnconfirmedRequest {
   private EventNotificationParameters eventNotificationParameters;

   public UnconfirmedEventNotificationRequest() {
      super(3);
   }

   public UnconfirmedEventNotificationRequest(EventNotificationParameters eventNotificationParameters) {
      super(3);
      this.eventNotificationParameters = eventNotificationParameters;
   }

   public EventNotificationParameters getEventNotificationParameters() {
      return this.eventNotificationParameters;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      this.eventNotificationParameters.writeEncoded(outputStream);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.eventNotificationParameters = new EventNotificationParameters();
      this.eventNotificationParameters.readEncoded(inputStream);
   }

   @Override
   public String toString() {
      return "UnconfirmedEventNotification:\n" + this.eventNotificationParameters.toString();
   }
}
