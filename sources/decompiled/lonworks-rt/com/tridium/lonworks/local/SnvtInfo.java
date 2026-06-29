package com.tridium.lonworks.local;

import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.io.LonOutputStream;

public class SnvtInfo {
   public static final int NV_DESC_EXT_REC = 128;
   public static final int NV_DESC_NV_SYNC = 64;
   public static final int NV_DESC_NV_POLLED = 32;
   public static final int NV_DESC_NV_OFFLINE = 16;
   public static final int NV_DESC_NV_SERVICE = 8;
   public static final int NV_DESC_NV_PRIORITY = 4;
   public static final int NV_DESC_NV_AUTH = 2;
   public static final int NV_DESC_NV_CONFIG = 1;
   public static final int SIZE = 2;
   public static final int NV_MAX_RATE = 128;
   public static final int NV_RATE_EST = 64;
   public static final int NV_NAME = 32;
   public static final int NV_SELF_DOC = 16;
   public static final int NV_COUNT = 8;
   public static final int NV_MASK_FOR_ALL = 16;
   public static final int VER1_HEADER_LEN = 6;
   public static final int LSB_MASK = 255;
   private byte[] networkImage;
   private int networkImageLen = 0;
   private BLocalLonDevice local;

   public SnvtInfo(BLocalLonDevice local) {
      this.local = local;
      this.calcNetworkImage();
   }

   public byte[] getInfoData(int offset, int count) {
      if (offset >= this.networkImageLen) {
         return null;
      } else {
         if (offset + count > this.networkImageLen) {
            count = this.networkImageLen - offset;
         }

         byte[] response = new byte[count];
         System.arraycopy(this.networkImage, offset, response, 0, count);
         return response;
      }
   }

   private void calcNetworkImage() {
      BINetworkVariable[] nvs = this.local.getNetworkVariables();
      int numNetVars = nvs.length;
      int version = 1;
      int msgTagCount = 0;
      String nodeSelfDoc = this.local.getSelfDoc();
      if (nodeSelfDoc == null || nodeSelfDoc.length() == 0) {
         nodeSelfDoc = "&3.0@;Niagara Server Node";
      }

      LonOutputStream outStream = new LonOutputStream();
      outStream.writeUnsigned16(0);
      outStream.writeUnsigned8(numNetVars & 0xFF);
      outStream.writeUnsigned8(version);
      outStream.writeUnsigned8(numNetVars >> 8 & 0xFF);
      outStream.writeUnsigned8(msgTagCount);

      for (int index = 0; index < numNetVars; index++) {
         if (nvs[index] != null) {
            this.getDescriptor(outStream, nvs[index]);
         }
      }

      byte[] a = nodeSelfDoc.getBytes();
      int nodeSdLen = a.length;
      outStream.writeByteArray(a);
      if (a[nodeSdLen - 1] != 0) {
         outStream.writeUnsigned8(0);
      }

      for (int indexx = 0; indexx < numNetVars; indexx++) {
         if (nvs[indexx] != null) {
            this.getSelfDocs(outStream, nvs[indexx]);
         }
      }

      outStream.writeUnsigned8(0);
      outStream.writeUnsigned16(nodeSdLen);
      outStream.writeUnsigned16(numNetVars);
      outStream.writeUnsigned16(18);
      outStream.writeUnsigned8(1);
      outStream.writeUnsigned8(0);
      outStream.writeUnsigned8(0);
      outStream.writeUnsigned8(1);

      for (int i = 0; i < 6; i++) {
         outStream.writeUnsigned8(0);
      }

      outStream.writeUnsigned16(2);
      outStream.writeUnsigned16(this.local.getDeviceData().getAddressCount());
      outStream.writeUnsigned16(msgTagCount);
      this.networkImage = outStream.toByteArray();
      this.networkImageLen = this.networkImage.length;
      this.networkImage[0] = (byte)(this.networkImageLen >> 8 & 0xFF);
      this.networkImage[1] = (byte)(this.networkImageLen & 0xFF);
   }

   private void getDescriptor(LonOutputStream outStream, BINetworkVariable nv) {
      int descriptor = 0;
      int snvtType = 0;
      if (nv.isLocalNv()) {
         BNvProps nvProps = ((BLocalNv)nv).getNvProps();
         descriptor = 128;
         if (nvProps.getPolled()) {
            descriptor |= 32;
         }

         if (nvProps.getServiceConf()) {
            descriptor |= 8;
         }

         if (nvProps.getPriorityConf()) {
            descriptor |= 4;
         }

         if (nvProps.getAuthConf()) {
            descriptor |= 2;
         }

         snvtType = nvProps.getSnvtType();
      } else if (nv.isLocalNci()) {
         BNcProps ncProps = ((BLocalNci)nv).getNcProps();
         int var7 = 128;
         descriptor = var7 | 1;
         BModifyFlags flgs = ncProps.getModifyFlag();
         if (flgs.isOffline()) {
            descriptor |= 16;
         }

         snvtType = ncProps.getSnvtType();
      } else {
         System.out.println("INTERNAL ERROR: in SnvtInfo.getDescriptor().");
      }

      outStream.writeUnsigned8(descriptor);
      outStream.writeUnsigned8(snvtType);
   }

   private void getSelfDocs(LonOutputStream outStream, BINetworkVariable nv) {
      StringBuilder sb = new StringBuilder();
      int mask = 0;
      if (nv.isLocalNv()) {
         mask = 32;
         sb.append(((BLocalNv)nv).getDisplayName(null)).append("\u0000");
         String sdoc = ((BLocalNv)nv).getSelfDoc();
         if (sdoc.length() > 0) {
            mask |= 16;
            sb.append(sdoc);
            sb.append("\u0000");
         }
      } else if (nv.isLocalNci()) {
         mask = 32;
         sb.append(((BLocalNci)nv).getDisplayName(null)).append("\u0000");
         String sdoc = ((BLocalNci)nv).getSelfDoc();
         if (sdoc.length() > 0) {
            mask |= 16;
            sb.append(sdoc);
            sb.append("\u0000");
         }
      } else {
         System.out.println("INTERNAL ERROR: in SnvtInfo.getDescriptor().");
      }

      outStream.writeUnsigned8(mask);
      outStream.writeByteArray(sb.toString().getBytes());
   }
}
