package com.tridium.bacnet.services;

import javax.baja.bacnet.BacnetConfirmedServiceChoice;

public abstract class BacnetComplexAck extends BacnetServicePrimitive implements BacnetConfirmedServiceChoice {
   public static final int MAX_TRANSPORT_BYTES = 5;

   protected BacnetComplexAck(int serviceAckChoice) {
      super(3, serviceAckChoice);
   }
}
