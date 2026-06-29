package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.VmacUtil;
import com.tridium.bacnet.stack.link.sc.connection.ScDataOutputStream;
import com.tridium.bacnet.stack.network.NetworkPdu;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;

public final class ScNpdu extends AddressedMessage {
   private List<HeaderOption> dataOptions = Collections.emptyList();
   private NetworkPdu npdu;
   private byte[] rawNpdu;

   ScNpdu() {
   }

   private ScNpdu(int messageId, NetworkPdu npdu) {
      super(messageId);
      this.npdu = npdu;
   }

   public static ScNpdu make(int messageId, NetworkPdu npdu) {
      Objects.requireNonNull(npdu, "Network PDU must not be null.");
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      return new ScNpdu(messageId, npdu);
   }

   @Override
   public int getFunction() {
      return 1;
   }

   @Override
   public void setDestinationVmac(long vmac) {
      VmacUtil.checkIsDestinationVmac(vmac);
      this.destinationVmac = vmac;
   }

   public void setDestinationToBroadcastVmac() {
      this.setDestinationVmac(281474976710655L);
   }

   public NetworkPdu getNpdu() {
      return this.npdu;
   }

   public byte[] getRawNpdu() {
      return this.rawNpdu;
   }

   @Override
   public void encode(ScDataOutputStream out) throws IOException {
      super.encode(out);
      if (this.npdu == null) {
         throw new IOException("NPDU is null when attempting to encode an ScNpdu message");
      } else {
         this.npdu.writeNetworkBytes(out.getUnderlyingStream());
      }
   }

   @Override
   protected void decodePayload(ByteBuffer in) throws ScReadMessageException {
      checkHasPayload(in);

      try {
         this.rawNpdu = new byte[in.available()];
         in.readFully(this.rawNpdu);
      } catch (IOException var3) {
         throw new ScReadMessageException("Message payload is incomplete", var3, BBacnetErrorCode.messageIncomplete);
      }
   }

   @Override
   protected boolean hasDataOptions(int controlFlags) throws ScReadMessageException {
      return (controlFlags & 1) > 0;
   }

   @Override
   protected void decodeDataOptions(ByteBuffer in) throws IOException, ScReadMessageException {
      this.dataOptions = decodeHeaderOptions(in, false);
   }

   public ScNpdu addDataOption(HeaderOption option) {
      if (this.dataOptions.isEmpty()) {
         this.dataOptions = new ArrayList<>();
      }

      this.dataOptions.add(option);
      return this;
   }

   @Override
   public List<HeaderOption> getDataOptions() {
      return this.dataOptions;
   }

   @Override
   public String toString() {
      String string = super.toString();
      if (this.npdu != null) {
         string = string + "; npdu: " + this.npdu;
      } else {
         string = string + "; npdu bytes: " + Arrays.toString(this.rawNpdu);
      }

      return string;
   }
}
