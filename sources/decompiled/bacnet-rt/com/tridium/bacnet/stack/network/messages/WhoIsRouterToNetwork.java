package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class WhoIsRouterToNetwork extends NetworkLayerMsg {
   private int networkNumber = -1;

   public WhoIsRouterToNetwork(ByteArrayInputStream is) throws BacnetStackException {
      super(0);
      this.readNetworkBytes(is);
   }

   public WhoIsRouterToNetwork() {
      super(0);
   }

   public WhoIsRouterToNetwork(int networkNumber) {
      super(0);
      this.networkNumber = networkNumber;
   }

   public boolean isNetworkNumber() {
      return this.networkNumber != -1;
   }

   public int getNetworkNumber() {
      return this.networkNumber;
   }

   @Override
   public String getMsgString() {
      return "WhoIsRouterToNetwork; network=" + this.networkNumber;
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      int avail = is.available();
      if (avail != 0) {
         if (avail >= 2) {
            this.networkNumber = is.read() << 8;
            this.networkNumber = this.networkNumber | is.read();
         }
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      if (this.networkNumber >= 0) {
         os.write(this.networkNumber >> 8);
         os.write(this.networkNumber & 0xFF);
      }
   }
}
