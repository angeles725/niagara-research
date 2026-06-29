package com.tridium.bacnet.services;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;

public class BacnetAbort extends BacnetServicePrimitive {
   public BacnetAbort(int abortReason) {
      super(7, abortReason);
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) {
   }
}
