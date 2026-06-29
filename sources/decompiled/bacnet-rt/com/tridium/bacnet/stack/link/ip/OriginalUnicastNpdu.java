package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.network.NetworkPdu;
import java.io.ByteArrayOutputStream;

public class OriginalUnicastNpdu extends BvllMessage {
   private byte[] rawNpdu;
   private NetworkPdu npdu;

   public OriginalUnicastNpdu(NetworkPdu npdu) {
      super(10);
      this.npdu = npdu;
   }

   OriginalUnicastNpdu(int len) {
      super(10, len);
   }

   @Override
   public final byte[] encode(ByteArrayOutputStream out) {
      out.write(129);
      out.write(this.function);
      out.write(0);
      out.write(0);
      this.npdu.writeNetworkBytes(out);
      byte[] buf = out.toByteArray();
      buf[2] = (byte)((buf.length & 0xFF00) >> 8);
      buf[3] = (byte)(buf.length & 0xFF);
      return buf;
   }

   @Override
   public final void decode(BacnetInputStream in) {
      this.rawNpdu = new byte[this.len];
      in.read(this.rawNpdu);
   }
}
