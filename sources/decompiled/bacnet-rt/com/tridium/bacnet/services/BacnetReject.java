package com.tridium.bacnet.services;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;

public class BacnetReject extends BacnetServicePrimitive {
   public BacnetReject(int rejectReason) {
      super(6, rejectReason);
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) {
   }
}
