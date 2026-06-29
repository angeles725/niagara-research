package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.CovNotificationParameters;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class UnconfirmedCovNotificationRequest extends BacnetUnconfirmedRequest {
   private CovNotificationParameters covNotificationParameters;

   public UnconfirmedCovNotificationRequest() {
      super(2);
   }

   public UnconfirmedCovNotificationRequest(CovNotificationParameters covNotificationParameters) {
      super(2);
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
      return "UnconfirmedCovNotification:\n" + this.covNotificationParameters.toString();
   }
}
