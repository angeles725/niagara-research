package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class RouterBusyToNetwork extends NetworkLayerMsg {
   private int[] networkNumbers;

   public RouterBusyToNetwork(ByteArrayInputStream is) throws BacnetStackException {
      super(4);
      this.readNetworkBytes(is);
   }

   public RouterBusyToNetwork() {
      super(4);
      this.networkNumbers = new int[0];
   }

   public RouterBusyToNetwork(int networkNumber) {
      super(4);
      this.networkNumbers = new int[]{networkNumber};
   }

   public RouterBusyToNetwork(int[] networkNumbers) {
      super(4);
      this.networkNumbers = networkNumbers;
   }

   public boolean isAllNetworks() {
      return this.networkNumbers == null || this.networkNumbers.length == 0;
   }

   public int[] getNetworkNumbers() {
      return this.networkNumbers;
   }

   @Override
   public String getMsgString() {
      StringBuilder sb = new StringBuilder("RouterBusyToNetwork; networks=");
      if (this.networkNumbers != null && this.networkNumbers.length != 0) {
         for (int i = 0; i < this.networkNumbers.length; i++) {
            sb.append(String.valueOf(this.networkNumbers[i])).append(',');
         }
      } else {
         sb.append("none ");
      }

      sb.setLength(sb.length() - 1);
      return sb.toString();
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      if (is.available() % 2 != 0) {
         throw new InvalidNetworkMsgException();
      } else {
         this.networkNumbers = new int[is.available() / 2];

         for (int i = 0; i < this.networkNumbers.length; i++) {
            this.networkNumbers[i] = is.read() << 8;
            this.networkNumbers[i] = this.networkNumbers[i] | is.read();
         }
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);

      for (int i = 0; i < this.networkNumbers.length; i++) {
         os.write(this.networkNumbers[i] >> 8);
         os.write(this.networkNumbers[i]);
      }
   }
}
