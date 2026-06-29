package com.tridium.lonworks.util;

import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import com.tridium.lonworks.xml.XNetworkVariable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.baja.file.BIFile;
import javax.baja.lonworks.BDynamicDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BImportParameters;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.io.LonOutputStream;

public final class NxeUtil {
   private static final int INTEL_LEAD_CHAR = 58;
   private static final int SRECORD_LEAD_CHAR = 83;

   private NxeUtil() {
   }

   public static byte[] nxeFileToByteArray(BIFile nxeFile) throws IOException {
      byte[] var5;
      try (
         InputStream in = nxeFile.getInputStream();
         ByteArrayOutputStream out = new ByteArrayOutputStream();
      ) {
         switch (in.read()) {
            case 58:
               sendIntelHexFile(in, out);
               break;
            case 83:
               sendSRecordFile(in, out);
               break;
            default:
               throw new IOException("Unknown file record format.");
         }

         var5 = out.toByteArray();
      }

      return var5;
   }

   private static void sendIntelHexFile(InputStream in, ByteArrayOutputStream out) throws IOException {
      while (in.available() > 0) {
         byte len = getByte(in);
         if (len > 0) {
            out.write(len);
         }

         if (len > 0) {
            out.write(getByte(in));
            out.write(getByte(in));
         } else {
            getByte(in);
            getByte(in);
         }

         if (getByte(in) == 1) {
            out.write(0);
            return;
         }

         for (int i = 0; i < len; i++) {
            out.write(getByte(in));
         }

         while (in.available() > 0 && in.read() != 58) {
         }
      }
   }

   private static void sendSRecordFile(InputStream in, OutputStream out) throws IOException {
      while (in.available() > 0) {
         if (in.read() == 57) {
            out.write(0);
            return;
         }

         int len = getByte(in) - 3;
         if (len > 0) {
            out.write(len);
         }

         if (len > 0) {
            out.write(getByte(in));
            out.write(getByte(in));
         } else {
            getByte(in);
            getByte(in);
         }

         for (int i = 0; i < len; i++) {
            out.write(getByte(in));
         }

         while (in.available() > 0 && in.read() != 83) {
         }
      }
   }

   private static byte getByte(InputStream in) throws IOException {
      int val = (Character.digit((char)in.read(), 16) << 4) + Character.digit((char)in.read(), 16);
      return (byte)val;
   }

   public static BProgramId findProgramId(byte[] data) {
      int pos = 0;

      while (pos < data.length) {
         int address = ((data[pos + 1] & 255) << 8) + (data[pos + 2] & 255);
         if (address == 61453) {
            byte[] b = new byte[8];
            System.arraycopy(data, pos + 3, b, 0, b.length);
            return BProgramId.make(b);
         }

         pos += data[pos] + 3;
      }

      return null;
   }

   public static byte[] getNvDirectionByteArray(XLonDevice xdev) throws IOException {
      LonOutputStream out = new LonOutputStream();
      XNetworkVariable[] nvs = xdev.getNetworkVariables();

      for (int i = 0; i < nvs.length; i++) {
         out.writeUnsigned16(nvs[i].index);
         out.writeUnsigned16(nvs[i].arraySize);
         out.write(nvs[i].direction.equalsIgnoreCase("input") ? 0 : 1);
      }

      return out.toByteArray();
   }

   public static void updateDevice(BLonDevice dev, XLonDevice xdev) {
      if (dev instanceof BDynamicDevice) {
         XLonInterfaceFile xfile = new XLonInterfaceFile();
         xfile.addAttribute("", xdev);
         BImportParameters param = new BImportParameters();
         param.setUseLonObjects(dev.getLonObjects().length > 0);
         DynaDev.importXLon((BDynamicDevice)dev, xfile, param);
      } else {
         System.out.println("NxeUtil.updateDevice() not implemented for " + dev.getType());
      }
   }

   public static void updateDevice(BLonDevice dev, XLonInterfaceFile xfile) {
      if (dev instanceof BDynamicDevice) {
         BImportParameters param = new BImportParameters();
         param.setUseLonObjects(dev.getLonObjects().length > 0);
         DynaDev.importXLon((BDynamicDevice)dev, xfile, param);
      } else {
         System.out.println("NxeUtil.updateDevice() not implemented for " + dev.getType());
      }
   }
}
