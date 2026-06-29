package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class IAmRouterToNetwork extends NetworkLayerMsg {
   private int[] networkNumbers;

   public IAmRouterToNetwork(ByteArrayInputStream is) throws BacnetStackException {
      super(1);
      this.readNetworkBytes(is);
   }

   public IAmRouterToNetwork(int networkNumber) {
      super(1);
      this.networkNumbers = new int[]{networkNumber};
   }

   public IAmRouterToNetwork(int[] networkNumbers) {
      super(1);
      this.networkNumbers = networkNumbers;
   }

   public int[] getNetworkNumbers() {
      return this.networkNumbers;
   }

   @Override
   public String getMsgString() {
      StringBuilder sb = new StringBuilder("IAmRouterToNetwork; networks=");
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
