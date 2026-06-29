package com.tridium.bacnet.stack;

import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.io.BacnetServiceListener;

public interface IAmListener extends BacnetServiceListener {
   void receiveIAm(IAmRequest var1, BBacnetAddress var2);
}
