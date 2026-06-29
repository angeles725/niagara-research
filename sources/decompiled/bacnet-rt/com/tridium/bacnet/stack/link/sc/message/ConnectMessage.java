package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.VmacUtil;
import com.tridium.bacnet.stack.link.sc.connection.ScDataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.ByteBuffer;

public abstract class ConnectMessage extends ScBvlcMessage {
   private long vmac;
   private UUID deviceUuid;
   private int maxBvlcLength;
   private int maxNpduLength;

   protected ConnectMessage() {
   }

   protected ConnectMessage(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      super(messageId);
      this.vmac = vmac;
      this.deviceUuid = deviceUuid;
      this.maxBvlcLength = maxBvlcLength;
      this.maxNpduLength = maxNpduLength;
   }

   protected static void checkArgs(int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      VmacUtil.checkIsDeviceVmac(vmac);
      Objects.requireNonNull(deviceUuid, "deviceUuid");
      ScMessageUtil.checkNpduLength(maxNpduLength);
      ScMessageUtil.checkBvlcLength(maxBvlcLength, maxNpduLength);
   }

   @Override
   public final void encode(ScDataOutputStream out) throws IOException {
      super.encode(out);
      VmacUtil.writeVmac(this.vmac, out);
      out.writeLong(this.deviceUuid.getMostSignificantBits());
      out.writeLong(this.deviceUuid.getLeastSignificantBits());
      out.writeShort(this.maxBvlcLength);
      out.writeShort(this.maxNpduLength);
   }

   @Override
   protected final void decodePayload(ByteBuffer in) throws ScReadMessageException {
      checkHasPayload(in);

      try {
         this.vmac = VmacUtil.readVmac(in);
         if (!VmacUtil.isDeviceVmac(this.vmac)) {
            throw new ScReadMessageException(
               functionToString(this.getFunction()) + " VMAC [" + VmacUtil.vmacToString(this.vmac) + "] is not a valid device VMAC",
               BBacnetErrorCode.parameterOutOfRange
            );
         } else {
            long msb = in.readLong();
            long lsb = in.readLong();
            this.deviceUuid = new UUID(msb, lsb);
            this.maxBvlcLength = in.readUnsignedShort();
            this.maxNpduLength = in.readUnsignedShort();

            try {
               ScMessageUtil.checkNpduLength(this.maxNpduLength);
               ScMessageUtil.checkBvlcLength(this.maxBvlcLength, this.maxNpduLength);
            } catch (IllegalArgumentException var7) {
               throw new ScReadMessageException(var7.getMessage(), BBacnetErrorCode.parameterOutOfRange);
            }
         }
      } catch (IOException var8) {
         throw new ScReadMessageException("Message payload is incomplete", BBacnetErrorCode.messageIncomplete);
      }
   }

   protected static byte[] getBytes(int function, int messageId, long vmac, UUID deviceUuid, int maxBvlcLength, int maxNpduLength) {
      byte[] bytes = new byte[30];
      bytes[0] = (byte)function;
      bytes[1] = 0;
      ByteArrayUtil.writeShort(bytes, 2, messageId);
      VmacUtil.writeVmac(bytes, 4, vmac);
      ByteArrayUtil.writeLong(bytes, 10, deviceUuid.getMostSignificantBits());
      ByteArrayUtil.writeLong(bytes, 18, deviceUuid.getLeastSignificantBits());
      ByteArrayUtil.writeShort(bytes, 26, maxBvlcLength);
      ByteArrayUtil.writeShort(bytes, 28, maxNpduLength);
      return bytes;
   }

   public final long getVmac() {
      return this.vmac;
   }

   public final UUID getDeviceUuid() {
      return this.deviceUuid;
   }

   public final int getMaxBvlcLength() {
      return this.maxBvlcLength;
   }

   public final int getMaxNpduLength() {
      return this.maxNpduLength;
   }

   @Override
   public final String toString() {
      return super.toString()
         + "; vmac: ["
         + VmacUtil.vmacToString(this.vmac)
         + ']'
         + "; uuid: ["
         + this.deviceUuid
         + ']'
         + "; max BVLC length: "
         + this.maxBvlcLength
         + "; max NPDU length: "
         + this.maxNpduLength;
   }
}
