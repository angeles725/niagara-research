package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.io.AsnException;

public class GetAlarmSummaryRequest extends BacnetConfirmedRequest {
   public GetAlarmSummaryRequest() {
      super(3);
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new GetAlarmSummaryAck(serviceChoice, inputStream);
   }

   @Override
   public String toString() {
      return "GetAlarmSummaryRequest";
   }
}
