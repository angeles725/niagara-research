package com.tridium.bacnet.stack;

import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public interface AppDebugListener {
   void receive(BBacnetAddress var1, BacnetServicePrimitive var2);

   void send(BBacnetAddress var1, BacnetServicePrimitive var2);
}
