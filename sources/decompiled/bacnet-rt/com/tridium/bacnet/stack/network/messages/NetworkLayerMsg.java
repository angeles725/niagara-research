package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.NetworkPdu;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class NetworkLayerMsg extends NetworkPdu {
   private int messageType;
   private int vendorId;
   private byte[] msgData;
   private static final Logger logger = Logger.getLogger("bacnet.network");

   protected NetworkLayerMsg(int messageType) {
      this.messageType = messageType;
      this.vendorId = 0;
   }

   public NetworkLayerMsg(int messageType, BacnetInputStream is) throws BacnetStackException {
      this.messageType = messageType;
      this.vendorId = 0;
      this.readNetworkBytes(is);
   }

   public NetworkLayerMsg(int messageType, int vendorId, BacnetInputStream is) throws BacnetStackException {
      this.messageType = messageType;
      this.vendorId = vendorId;
      this.readNetworkBytes(is);
   }

   @Override
   public boolean isNetworkLayerMsg() {
      return true;
   }

   @Override
   public int getMessageType() {
      return this.messageType;
   }

   public int getVendorId() {
      return this.vendorId;
   }

   public String getMsgString() {
      return "msgType=" + this.messageType + ", vID=" + this.vendorId;
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      this.msgData = new byte[is.available()];

      try {
         is.read(this.msgData);
      } catch (IOException var3) {
         logger.warning("IOException parsing proprietary network layer message!");
         throw new BacnetStackException("Unsupported Network Layer Message");
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      os.write(this.messageType);
      if ((this.messageType & 128) != 0) {
         os.write(this.vendorId);
         os.write(this.msgData, 0, this.msgData.length);
      }
   }
}
