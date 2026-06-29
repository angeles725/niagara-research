package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class EstablishConnectionToNetwork extends NetworkLayerMsg {
   private int networkNumber;
   private int terminationTimeValue;

   public EstablishConnectionToNetwork(ByteArrayInputStream is) throws BacnetStackException {
      super(8);
      this.readNetworkBytes(is);
   }

   public EstablishConnectionToNetwork(int networkNumber, int terminationTimeValue) {
      super(8);
      this.networkNumber = networkNumber;
      this.terminationTimeValue = terminationTimeValue;
   }

   public int getNetworkNumber() {
      return this.networkNumber;
   }

   public int getTerminationTimeValue() {
      return this.terminationTimeValue;
   }

   @Override
   public String getMsgString() {
      return "EstablishConnectionToNetwork; network=" + this.networkNumber + ", termTV=" + this.terminationTimeValue;
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      if (is.available() < 3) {
         throw new InvalidNetworkMsgException();
      } else {
         this.networkNumber = is.read() << 8;
         this.networkNumber = this.networkNumber | is.read();
         this.terminationTimeValue = is.read();
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      os.write(this.networkNumber >> 8 & 0xFF);
      os.write(this.networkNumber & 0xFF);
      os.write(this.terminationTimeValue);
   }
}
