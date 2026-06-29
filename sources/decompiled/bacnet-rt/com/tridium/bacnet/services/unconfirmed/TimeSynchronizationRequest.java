package com.tridium.bacnet.services.unconfirmed;

import javax.baja.sys.BAbsTime;

public class TimeSynchronizationRequest extends GenericTimeSyncRequest {
   public TimeSynchronizationRequest() {
      this(null);
   }

   public TimeSynchronizationRequest(BAbsTime dateTime) {
      super(6, dateTime);
   }
}
