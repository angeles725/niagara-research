package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import com.tridium.bacnet.stack.network.NetworkPdu;
import com.tridium.bacnet.stack.transport.ApplicationPdu;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public class ApplicationMsg extends NetworkPdu {
   private BacnetInputStream rawApdu;
   private ApplicationPdu apdu;
   private byte[] rawApduBytes;

   public ApplicationMsg(BacnetInputStream rawApdu) {
      this.rawApdu = rawApdu;
   }

   public ApplicationMsg(BBacnetAddress destAddr, BBacnetAddress srcAddr, ApplicationPdu apdu, BNetworkPriority networkPriority, boolean dataExpectingReply) {
      super(destAddr, srcAddr, networkPriority, dataExpectingReply);
      this.apdu = apdu;
   }

   @Override
   public boolean isNetworkLayerMsg() {
      return false;
   }

   public ApplicationPdu getAPDU() {
      return this.apdu;
   }

   public BacnetInputStream getRawAPDU() {
      return this.rawApdu;
   }

   public void storeApdu() {
      this.rawApdu.mark(this.rawApdu.getPos());
      this.rawApduBytes = new byte[this.rawApdu.available()];
      this.rawApdu.read(this.rawApduBytes);
      this.rawApdu.reset();
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      if (this.apdu != null) {
         this.apdu.writeNetworkBytes(os);
      } else {
         os.write(this.rawApduBytes, 0, this.rawApduBytes.length);
      }
   }
}
