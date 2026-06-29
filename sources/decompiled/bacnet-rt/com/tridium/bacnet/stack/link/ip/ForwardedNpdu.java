package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.network.NetworkPdu;
import java.io.ByteArrayOutputStream;

public class ForwardedNpdu extends BvllMessage {
   private byte[] rawNpdu;
   private byte[] originatingDeviceAddress;
   private NetworkPdu npdu;

   ForwardedNpdu(NetworkPdu npdu, byte[] orig) {
      super(4);
      this.npdu = npdu;
      this.originatingDeviceAddress = orig;
   }

   ForwardedNpdu(int len) {
      super(4, len);
   }

   ForwardedNpdu(BacnetInputStream in, byte[] orig) {
      super(4, in.available());
      this.rawNpdu = new byte[this.len];
      in.read(this.rawNpdu);
      this.originatingDeviceAddress = orig;
   }

   ForwardedNpdu(BacnetInputStream in) {
      super(4, in.available());
      this.decode(in);
   }

   @Override
   public final byte[] encode(ByteArrayOutputStream out) {
      out.write(129);
      out.write(this.function);
      out.write(0);
      out.write(0);
      out.write(this.originatingDeviceAddress, 0, this.originatingDeviceAddress.length);
      if (this.npdu == null) {
         out.write(this.rawNpdu, 0, this.rawNpdu.length);
      } else {
         this.npdu.writeNetworkBytes(out);
      }

      byte[] buf = out.toByteArray();
      buf[2] = (byte)((buf.length & 0xFF00) >> 8);
      buf[3] = (byte)(buf.length & 0xFF);
      return buf;
   }

   @Override
   public final void decode(BacnetInputStream in) {
      this.originatingDeviceAddress = new byte[6];
      in.read(this.originatingDeviceAddress);
      this.rawNpdu = new byte[this.len];
      in.read(this.rawNpdu);
   }
}
