package com.tridium.bacnet.services.unconfirmed;

import javax.baja.sys.BAbsTime;

public class UtcTimeSynchronizationRequest extends GenericTimeSyncRequest {
   public UtcTimeSynchronizationRequest() {
      this(null);
   }

   public UtcTimeSynchronizationRequest(BAbsTime dateTime) {
      super(9, dateTime);
   }
}
