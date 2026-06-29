package com.tridium.bacnet.stack.link.sc;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Random;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.TextUtil;

public final class VmacUtil {
   private static final long MAX_VMAC = 281474976710655L;
   private static final long MIN_VMAC = 0L;
   private static final long VMAC_BIT_MASK = 281474976710655L;
   public static final long NULL_VMAC = -1L;
   public static final long BROADCAST_VMAC = 281474976710655L;

   private VmacUtil() {
   }

   public static byte[] regenerateVmacAddress() {
      Random random = new Random();

      long vmac;
      do {
         vmac = random.nextLong() & 281474976710655L;
      } while (!isDeviceVmac(vmac));

      return vmacToBytes(vmac);
   }

   private static boolean isLegalVmac(long vmac) {
      return vmac >= 0L && vmac <= 281474976710655L;
   }

   public static boolean isDeviceVmac(long vmac) {
      return vmac > 0L && vmac < 281474976710655L;
   }

   public static boolean isDestinationVmac(long vmac) {
      return vmac > 0L && vmac <= 281474976710655L;
   }

   public static void checkIsLegalVmac(long vmac) {
      if (!isLegalVmac(vmac)) {
         throw new IllegalArgumentException("VMAC value is not valid [0x" + Long.toHexString(vmac) + ']');
      }
   }

   public static void checkIsDeviceVmac(long vmac) {
      if (!isDeviceVmac(vmac)) {
         throw new IllegalArgumentException("Not a valid device VMAC: [0x" + Long.toHexString(vmac) + ']');
      }
   }

   public static void checkIsDestinationVmac(long vmac) {
      if (!isDestinationVmac(vmac)) {
         throw new IllegalArgumentException("Not a valid destination VMAC: [0x" + Long.toHexString(vmac) + ']');
      }
   }

   public static long octetStringToVmac(BBacnetOctetString vmac) {
      return bytesToVmac(vmac == null ? null : vmac.getAddr());
   }

   private static long bytesToVmac(int b0, int b1, int b2, int b3, int b4, int b5) {
      return (b0 & 255L) << 40 | (b1 & 255L) << 32 | (b2 & 255L) << 24 | (b3 & 255L) << 16 | (b4 & 255L) << 8 | b5 & 255L;
   }

   public static String vmacToString(long vmac) {
      return !isLegalVmac(vmac)
         ? "null"
         : TextUtil.byteToHexString((int)(vmac >>> 40))
            + ' '
            + TextUtil.byteToHexString((int)(vmac >>> 32))
            + ' '
            + TextUtil.byteToHexString((int)(vmac >>> 24))
            + ' '
            + TextUtil.byteToHexString((int)(vmac >>> 16))
            + ' '
            + TextUtil.byteToHexString((int)(vmac >>> 8))
            + ' '
            + TextUtil.byteToHexString((int)vmac);
   }

   public static BBacnetOctetString vmacToOctetString(long vmac) {
      return BBacnetOctetString.make(vmacToBytes(vmac));
   }

   public static byte[] vmacToBytes(long vmac) {
      return isLegalVmac(vmac)
         ? new byte[]{(byte)(vmac >>> 40), (byte)(vmac >>> 32), (byte)(vmac >>> 24), (byte)(vmac >>> 16), (byte)(vmac >>> 8), (byte)vmac}
         : null;
   }

   public static void writeVmac(byte[] bytes, int offset, long vmac) {
      checkIsLegalVmac(vmac);
      bytes[offset] = (byte)(vmac >>> 40);
      bytes[offset + 1] = (byte)(vmac >>> 32);
      bytes[offset + 2] = (byte)(vmac >>> 24);
      bytes[offset + 3] = (byte)(vmac >>> 16);
      bytes[offset + 4] = (byte)(vmac >>> 8);
      bytes[offset + 5] = (byte)vmac;
   }

   public static long bytesToVmac(byte[] b) {
      return b != null && b.length == 6 ? bytesToVmac(b[0], b[1], b[2], b[3], b[4], b[5]) : -1L;
   }

   public static long readVmac(byte[] payload, int offset) {
      return bytesToVmac(payload[offset], payload[offset + 1], payload[offset + 2], payload[offset + 3], payload[offset + 4], payload[offset + 5]);
   }

   public static long readVmac(ByteBuffer in) throws IOException {
      return bytesToVmac(
         in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte(), in.readUnsignedByte()
      );
   }

   public static void writeVmac(long vmac, DataOutput out) throws IOException {
      if (!isLegalVmac(vmac)) {
         throw new IOException("Illegal VMAC");
      } else {
         out.write((byte)(vmac >>> 40));
         out.write((byte)(vmac >>> 32));
         out.write((byte)(vmac >>> 24));
         out.write((byte)(vmac >>> 16));
         out.write((byte)(vmac >>> 8));
         out.write((byte)vmac);
      }
   }
}
