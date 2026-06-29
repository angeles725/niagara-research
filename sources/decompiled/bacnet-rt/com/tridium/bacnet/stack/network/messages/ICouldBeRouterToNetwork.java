package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ICouldBeRouterToNetwork extends NetworkLayerMsg {
   private int networkNumber;
   private int performanceIndex;

   public ICouldBeRouterToNetwork(ByteArrayInputStream is) throws BacnetStackException {
      super(2);
      this.readNetworkBytes(is);
   }

   public ICouldBeRouterToNetwork(int networkNumber, int performanceIndex) {
      super(2);
      this.networkNumber = networkNumber;
      this.performanceIndex = performanceIndex;
   }

   public int getNetworkNumber() {
      return this.networkNumber;
   }

   public int getPerformanceIndex() {
      return this.performanceIndex;
   }

   @Override
   public String getMsgString() {
      return "ICouldBeRouterToNetwork; network=" + this.networkNumber + ", perfNdx=" + this.performanceIndex;
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      if (is.available() < 3) {
         throw new InvalidNetworkMsgException();
      } else {
         this.networkNumber = is.read() << 8;
         this.networkNumber = this.networkNumber | is.read();
         this.performanceIndex = is.read();
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      os.write(this.networkNumber >> 8 & 0xFF);
      os.write(this.networkNumber & 0xFF);
      os.write(this.performanceIndex);
   }
}
