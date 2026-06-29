package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public interface ServiceHandler {
   BacnetServicePrimitive receiveRequest(int var1, BacnetServicePrimitive var2, BBacnetAddress var3);

   default BBacnetTransportLayer getTransportLayer() {
      return null;
   }
}
