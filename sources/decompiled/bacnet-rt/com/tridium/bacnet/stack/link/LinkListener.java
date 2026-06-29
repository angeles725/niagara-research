package com.tridium.bacnet.stack.link;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.network.DataAttributes;

public interface LinkListener {
   void rcvIndication(byte[] var1, byte[] var2, BacnetInputStream var3, boolean var4);

   default void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is, boolean isBroadcast, DataAttributes dataAttributes) {
      this.rcvIndication(srcMacAddress, destMacAddress, is, isBroadcast);
   }
}
