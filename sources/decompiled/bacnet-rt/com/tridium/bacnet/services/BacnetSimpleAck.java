package com.tridium.bacnet.services;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;

public class BacnetSimpleAck extends BacnetServicePrimitive implements BacnetConfirmedServiceChoice {
   public BacnetSimpleAck(int serviceAckChoice) {
      super(2, serviceAckChoice);
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) {
   }
}
