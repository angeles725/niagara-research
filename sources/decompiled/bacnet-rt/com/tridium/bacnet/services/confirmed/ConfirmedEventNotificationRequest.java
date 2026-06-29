package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.EventNotificationParameters;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.io.AsnException;

public class ConfirmedEventNotificationRequest extends BacnetConfirmedRequest {
   private EventNotificationParameters eventNotificationParameters;

   public ConfirmedEventNotificationRequest() {
      super(2);
   }

   public ConfirmedEventNotificationRequest(EventNotificationParameters eventNotificationParameters) {
      super(2);
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
      return "ConfirmedEventNotification:\n" + this.eventNotificationParameters.toString();
   }
}
