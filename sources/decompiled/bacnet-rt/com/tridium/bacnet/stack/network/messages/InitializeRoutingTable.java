package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.BBacnetRouterEntry;
import com.tridium.bacnet.stack.network.BBacnetRouterTable;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class InitializeRoutingTable extends NetworkLayerMsg {
   private BBacnetRouterEntry[] table;
   private static final Logger logger = Logger.getLogger("bacnet.network");

   public InitializeRoutingTable(ByteArrayInputStream is) throws BacnetStackException {
      super(6);
      this.readNetworkBytes(is);
   }

   public InitializeRoutingTable() {
      super(6);
      this.table = new BBacnetRouterEntry[0];
   }

   public InitializeRoutingTable(BBacnetRouterTable rt) {
      super(6);
      this.table = (BBacnetRouterEntry[])rt.getChildren(BBacnetRouterEntry.class);
   }

   @Override
   public String getMsgString() {
      StringBuilder sb = new StringBuilder("InitializeRoutingTable; entries=");
      if (this.table != null && this.table.length != 0) {
         for (int i = 0; i < this.table.length; i++) {
            sb.append("\n ").append(this.table[i].toString());
         }
      } else {
         sb.append("none");
      }

      return sb.toString();
   }

   public boolean isEmpty() {
      return this.table == null || this.table.length == 0;
   }

   public int getNumberOfPorts() {
      return this.table == null ? 0 : this.table.length;
   }

   public BBacnetRouterEntry[] getTable() {
      return this.table;
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      try {
         int len = is.read();
         this.table = new BBacnetRouterEntry[len];

         for (int i = 0; i < len; i++) {
            this.table[i] = new BBacnetRouterEntry();
            this.table[i].readNetworkBytes(is);
         }
      } catch (IOException var4) {
         logger.warning("IOException parsing InitializeRoutingTable!");
         throw new InvalidNetworkMsgException();
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      os.write(this.table.length);

      for (int i = 0; i < this.table.length; i++) {
         this.table[i].writeNetworkBytes(os);
      }
   }
}
