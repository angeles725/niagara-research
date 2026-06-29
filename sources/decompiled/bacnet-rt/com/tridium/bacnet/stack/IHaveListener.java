package com.tridium.bacnet.stack;

import com.tridium.bacnet.services.unconfirmed.IHaveRequest;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.io.BacnetServiceListener;

public interface IHaveListener extends BacnetServiceListener {
   void receiveIHave(IHaveRequest var1, BBacnetAddress var2);
}
