package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.CovNotificationParameters;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class ConfirmedCovNotificationRequest extends BacnetConfirmedRequest {
   private CovNotificationParameters covNotificationParameters;

   public ConfirmedCovNotificationRequest() {
      super(1);
   }

   public ConfirmedCovNotificationRequest(CovNotificationParameters covNotificationParameters) {
      super(1);
      this.covNotificationParameters = covNotificationParameters;
   }

   public CovNotificationParameters getCovNotificationParameters() {
      return this.covNotificationParameters;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      this.covNotificationParameters.writeEncoded(outputStream);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.covNotificationParameters = new CovNotificationParameters();
      this.covNotificationParameters.readEncoded(inputStream);
   }

   @Override
   public String toString() {
      return "ConfirmedCovNotification:\n" + this.covNotificationParameters.toString();
   }
}
