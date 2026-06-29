package com.tridium.lonworks.util;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.netmessages.QuerySNVTRequest;
import com.tridium.lonworks.netmessages.QuerySNVTResponse;
import com.tridium.lonworks.netmessages.ReadMemRequest;
import com.tridium.lonworks.netmessages.ReadMemResponse;
import com.tridium.lonworks.netmessages.WriteMemRequest;
import com.tridium.lonworks.netmgmt.BLonNetmgmt;
import com.tridium.lonworks.netmgmt.NetMgmtConst;
import com.tridium.lonworks.util.selfdoc.ExtendedSelfDoc;
import com.tridium.lonworks.util.selfdoc.SelfDoc;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonRepeatTimer;

public class Neuron implements NetMessages, NetMgmtConst {
   private static int MAX_DATA_LENGTH = 8;
   public static final int LOCATION_LENGTH = 6;

   public static void uploadDeviceData(BLonDevice dev) throws LonException {
      int offset = 10;
      BDeviceData devDat = dev.getDeviceData();
      byte[] readOnlyData = readMemory(1, dev, offset, 16);
      int snvtPtr = (readOnlyData[11 - offset] & 255) << 8 | readOnlyData[12 - offset] & 255;
      devDat.setBoolean(BDeviceData.hosted, snvtPtr == 65535, AddressManager.noDeviceChange);
      devDat.setBoolean(BDeviceData.twoDomains, (readOnlyData[21 - offset] & 64) != 0, AddressManager.noDeviceChange);
      int addressCount = (readOnlyData[22 - offset] & 240) >> 4;
      if (dev.isExtended()) {
         addressCount = ExtendedSelfDoc.make(dev).getAddressCapacity();
      }

      devDat.setInt(BDeviceData.addressCount, addressCount, AddressManager.noDeviceChange);
   }

   public static BProgramId getProgramId(BLonDevice dev) throws LonException {
      byte[] a = readMemory(1, dev, 13, 8);
      return BProgramId.make(a);
   }

   public static BProgramId getProgramId(LonComm lonComm, LonAddress sendAddr, boolean auth) throws LonException {
      byte[] a = readMemory(lonComm, 1, sendAddr, 13, 8, auth);
      return BProgramId.make(a);
   }

   public static BProgramId getProgramId(BLonRouter rtr, boolean farSide) throws LonException {
      byte[] a = readMemory(rtr.lonComm(), 1, NmUtil.getSendAddress(rtr), 13, 8, isAuth(rtr, farSide), farSide);
      return BProgramId.make(a);
   }

   public static BNeuronId getNeuronId(BLonDevice dev) throws LonException {
      byte[] a = readMemory(1, dev, 0, 6);
      return BNeuronId.make(a);
   }

   public static BNeuronId getNeuronId(BLonRouter rtr, boolean farSide) throws LonException {
      return getNeuronId(rtr.lonComm(), NmUtil.getSendAddress(rtr), isAuth(rtr, farSide), farSide);
   }

   public static BNeuronId getNeuronId(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean farSide) throws LonException {
      byte[] a = readMemory(lonComm, 1, sendAddr, 0, 6, auth, farSide);
      return BNeuronId.make(a);
   }

   public static int getSnvtPtr(LonComm lonComm, LonAddress sendAddr, boolean auth) throws LonException {
      byte[] a = readMemory(lonComm, 1, sendAddr, 11, 2, auth);
      return (a[0] & 0xFF) << 8 | a[1] & 0xFF;
   }

   public static int getVersion(LonComm lonComm, LonAddress sendAddr, boolean auth) throws LonException {
      byte[] a = readMemory(lonComm, 0, sendAddr, 0, 1, auth);
      return a[0] & 0xFF;
   }

   public static boolean isHostedNode(BLonDevice dev) throws LonException {
      return isHostedNode(dev.lonComm(), NmUtil.getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public static boolean isHostedNode(LonComm lonComm, LonAddress sendAddr, boolean auth) throws LonException {
      int snvtPtr = getSnvtPtr(lonComm, sendAddr, auth);
      return snvtPtr == 65535;
   }

   public static boolean isTwoDomains(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean farSide) throws LonException {
      byte[] a = readMemory(lonComm, 1, sendAddr, 21, 1, auth, farSide);
      return (a[0] & 64) != 0;
   }

   public static int getNvCount(BLonDevice dev) throws LonException {
      return getNvCount(dev.lonComm(), NmUtil.getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public static int getNvCount(LonComm lonComm, LonAddress sendAddr, boolean auth) throws LonException {
      byte[] a = readMemory(lonComm, 1, sendAddr, 10, 1, auth);
      return a[0] & 63;
   }

   public static int getAliasOffset(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean extended) throws LonException {
      if (getSnvtPtr(lonComm, sendAddr, auth) != 65535) {
         return getNvCount(lonComm, sendAddr, auth);
      } else {
         SelfDoc sdoc = new SelfDoc(lonComm, sendAddr, auth, true, extended);
         return sdoc.getAliasOffset();
      }
   }

   public static int getAddressCount(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean extended) throws LonException {
      if (extended) {
         return ExtendedSelfDoc.make(lonComm, sendAddr, auth).getAddressCapacity();
      } else {
         byte[] a = readMemory(lonComm, 1, sendAddr, 22, 1, auth);
         int adrCnt = (a[0] & 240) >> 4;
         if (getVersion(lonComm, sendAddr, auth) >= 21) {
            a = readMemory(lonComm, 1, sendAddr, 46, 1, auth);
            adrCnt = a[0] & 255;
         }

         return adrCnt;
      }
   }

   public static int getAliasCount(BLonDevice dev) throws LonException {
      return getAliasCount(dev.lonComm(), NmUtil.getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public static int getAliasCount(LonComm lonComm, LonAddress sendAddr, boolean auth) throws LonException {
      byte[] a = readMemory(lonComm, 1, sendAddr, 36, 1, auth);
      return a[0] & 63;
   }

   public static int getChannelId(BLonDevice dev) throws LonException {
      return getChannelId(dev.lonComm(), NmUtil.getSendAddress(dev), dev.getDeviceData().getAuthenticate(), false);
   }

   public static int getChannelId(BLonRouter rtr, boolean farSide) throws LonException {
      return getChannelId(rtr.lonComm(), NmUtil.getSendAddress(rtr), isAuth(rtr, farSide), farSide);
   }

   public static int getChannelId(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean farSide) throws LonException {
      byte[] a = readMemory(lonComm, 2, sendAddr, 0, 2, auth, farSide);
      return (0xFF & a[0]) << 8 | 0xFF & a[1];
   }

   public static byte[] getReadOnlyStruct(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean farSide) throws LonException {
      return readTable(lonComm, sendAddr, 1, 38, auth, farSide);
   }

   public static byte[] getConfigStruct(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean farSide) throws LonException {
      return readTable(lonComm, sendAddr, 2, 25, auth, farSide);
   }

   public static byte[] readTable(LonComm lonComm, LonAddress sendAddr, int memType, int len, boolean auth, boolean farSide) throws LonException {
      int readLen = 16;
      int bytesRead = 0;

      byte[] rdOnly;
      for (rdOnly = new byte[len]; bytesRead < len; bytesRead += readLen) {
         if (readLen > len - bytesRead) {
            readLen = len - bytesRead;
         }

         byte[] a = readMemory(lonComm, memType, sendAddr, bytesRead, readLen, auth, farSide);
         System.arraycopy(a, 0, rdOnly, bytesRead, readLen);
      }

      return rdOnly;
   }

   public static void updateConfigData(BLonNetmgmt nm, BLonDevice dev) throws LonException {
      updateConfigData(dev.lonComm(), NmUtil.getSendAddress(dev), nm, dev.getDeviceData(), false);
   }

   public static void updateConfigData(BLonNetmgmt nm, BLonRouter rtr, boolean farSide) throws LonException {
      BDeviceData devDat = farSide ? rtr.getFarDeviceData() : rtr.getNearDeviceData();
      updateConfigData(rtr.lonComm(), rtr.getNearDeviceData().getNeuronId(), nm, devDat, farSide);
   }

   public static void updateConfigData(LonComm lonComm, LonAddress sendAddr, BLonNetmgmt nm, BDeviceData devDat, boolean farSide) throws LonException {
      int chanId = devDat.getChannelId();
      boolean auth = devDat.getAuthenticate();
      byte[] channelId = new byte[]{(byte)(chanId >> 8 & 0xFF), (byte)(chanId & 0xFF)};
      writeMemory(lonComm, 2, sendAddr, 0, 4, channelId, auth, farSide);
      byte[] priorityInfo;
      if (devDat.getFreezeChannelPriorities()) {
         priorityInfo = new byte[1];
      } else {
         priorityInfo = new byte[2];
         priorityInfo[1] = (byte)nm.getChannelPriorities();
      }

      priorityInfo[0] = (byte)devDat.getPrioritySlot();
      writeMemory(lonComm, 2, sendAddr, 15, 4, priorityInfo, auth, farSide);
      setLocation(lonComm, sendAddr, devDat.getLocation(), auth, farSide);
      boolean authOn = nm.getAuthenticate();
      byte[] timer = new byte[]{(byte)(nm.getNonGroupTimer() << 4)};
      timer[0] = setNMAuthenticateBit(timer[0], authOn);
      writeMemory(lonComm, 2, sendAddr, 24, 4, timer, auth, farSide);
      devDat.setAuthenticate(authOn);
   }

   public static void updateChannelId(BLonDevice dev) throws LonException {
      BDeviceData devDat = dev.getDeviceData();
      int chanId = devDat.getChannelId();
      boolean auth = devDat.getAuthenticate();
      byte[] channelId = new byte[]{(byte)(chanId >> 8 & 0xFF), (byte)(chanId & 0xFF)};
      writeMemory(dev.lonComm(), 2, NmUtil.getSendAddress(dev), 0, 4, channelId, auth, false);
   }

   public static void setNMAuthenticate(BLonDevice dev, boolean authOn) throws LonException {
      setNMAuthenticate(dev.lonComm(), NmUtil.getSendAddress(dev), authOn);
   }

   public static void setNMAuthenticate(LonComm lonComm, LonAddress sendAddr, boolean authOn) throws LonException {
      byte[] a = readMemory(lonComm, 2, sendAddr, 24, 1, true);
      if (isAuthBitSet(a[0]) != authOn) {
         a[0] = setNMAuthenticateBit(a[0], authOn);
         writeMemory(lonComm, 2, sendAddr, 24, 4, a, true, false);
      }
   }

   public static void verifyAuthenticate(BLonDevice dev) throws LonException {
      dev.getDeviceData().setAuthenticate(isNMAuthSet(dev));
   }

   public static boolean isNMAuthSet(BLonDevice dev) throws LonException {
      return isNMAuthSet(dev.lonComm(), NmUtil.getSendAddress(dev), dev.getDeviceData().getAuthenticate(), false);
   }

   public static boolean isNMAuthSet(LonComm lonComm, LonAddress sendAddr, boolean auth, boolean farSide) throws LonException {
      byte[] a;
      try {
         a = readMemory(lonComm, 2, sendAddr, 24, 1, auth, farSide);
      } catch (LonException var6) {
         a = readMemory(lonComm, 2, sendAddr, 24, 1, !auth, farSide);
      }

      return isAuthBitSet(a[0]);
   }

   public static boolean isAuthBitSet(byte a) throws LonException {
      return (a & 8) != 0;
   }

   private static byte setNMAuthenticateBit(byte b, boolean auth) {
      if (auth) {
         b = (byte)(b | 8);
      } else {
         b = (byte)(b & -9);
      }

      return b;
   }

   public static void setNodePriority(BLonDevice dev, int priority) throws LonException {
      byte[] nodePriority = new byte[]{(byte)priority};
      writeMemory(2, dev, 15, 4, nodePriority);
   }

   public static int getNodePriority(BLonDevice dev) throws LonException {
      byte[] a = readMemory(2, dev, 15, 1);
      return a[0] & 0xFF;
   }

   public static void setNodePriorityInfo(BLonDevice dev, int priority, int numPriorities) throws LonException {
      byte[] priorityInfo = new byte[]{(byte)priority, (byte)numPriorities};
      writeMemory(2, dev, 15, 4, priorityInfo);
   }

   public static void setLocation(BLonDevice dev, String location) throws LonException {
      setLocation(dev.lonComm(), NmUtil.getSendAddress(dev), location, dev.getDeviceData().getAuthenticate(), false);
   }

   public static void setLocation(BLonRouter rtr, String location, boolean farSide) throws LonException {
      setLocation(rtr.lonComm(), NmUtil.getSendAddress(rtr), location, isAuth(rtr, farSide), farSide);
   }

   private static boolean isAuth(BLonRouter rtr, boolean farSide) {
      return farSide ? rtr.getFarDeviceData().getAuthenticate() : rtr.getNearDeviceData().getAuthenticate();
   }

   public static void setLocation(LonComm lonComm, LonAddress sendAddr, String location, boolean auth, boolean farSide) throws LonException {
      byte[] a = new byte[]{0, 0, 0, 0, 0, 0};
      byte[] loc = location.getBytes();
      System.arraycopy(loc, 0, a, 0, loc.length <= 6 ? loc.length : 6);
      writeMemory(lonComm, 2, sendAddr, 2, 4, a, auth, farSide);
   }

   public static String getLocation(BLonDevice dev) throws LonException {
      byte[] a = readMemory(2, dev, 2, 6);
      return getString(a, 0, 6);
   }

   public static String getLocation(LonComm lonComm, LonAddress sendAddr, boolean auth) throws LonException {
      byte[] a = readMemory(lonComm, 2, sendAddr, 2, 6, auth);
      return getString(a, 0, 6);
   }

   private static String getString(byte[] a, int offset, int len) {
      StringBuilder sb = new StringBuilder();
      int cnt = 0;

      while (cnt < len) {
         byte b = a[offset + cnt++];
         if (b >= 48 && b <= 122) {
            sb.append((char)b);
         }
      }

      return sb.length() > 0 ? sb.toString() : "";
   }

   public static byte[] readMemory(int memType, BLonDevice dev, int offset, int length) throws LonException {
      int maxLen = dev.getMaxMessageLengthOut() - 1;
      if (maxLen < MAX_DATA_LENGTH) {
         maxLen = MAX_DATA_LENGTH;
      }

      return readMemory(dev.lonComm(), memType, NmUtil.getSendAddress(dev), offset, length, dev.authenticate(), false, maxLen);
   }

   public static byte[] readMemory(LonComm lonComm, int memType, LonAddress sendAddr, int offset, int length, boolean auth) throws LonException {
      return readMemory(lonComm, memType, sendAddr, offset, length, auth, false, MAX_DATA_LENGTH);
   }

   public static byte[] readMemory(LonComm lonComm, int memType, LonAddress sendAddr, int offset, int length, boolean auth, boolean farSide) throws LonException {
      return readMemory(lonComm, memType, sendAddr, offset, length, auth, farSide, MAX_DATA_LENGTH);
   }

   public static byte[] readMemory(LonComm lonComm, int memType, LonAddress sendAddr, int offset, int length, boolean auth, boolean farSide, int maxLen) throws LonException {
      int bytesRead = 0;
      byte[] doc = new byte[length];

      while (bytesRead < length) {
         int len = length - bytesRead;
         if (len > maxLen) {
            len = maxLen;
         }

         ReadMemRequest readReq = new ReadMemRequest(memType, offset + bytesRead, len);
         if (farSide) {
            readReq.setFarSide();
         }

         readReq.setAuthenticate(auth);
         ReadMemResponse readResp = (ReadMemResponse)lonComm.sendRequest(sendAddr, readReq);
         System.arraycopy(readResp.getData(), 0, doc, bytesRead, len);
         bytesRead += len;
      }

      return doc;
   }

   public static void writeMemory(int memType, BLonDevice dev, int offset, int form, byte[] data) throws LonException {
      int maxLen = dev.getMaxMessageLengthIn() - 6;
      if (maxLen < MAX_DATA_LENGTH) {
         maxLen = MAX_DATA_LENGTH;
      }

      writeMemory(dev.lonComm(), memType, NmUtil.getSendAddress(dev), offset, form, data, dev.authenticate(), false, maxLen);
   }

   public static void writeMemory(LonComm lonComm, int memType, LonAddress sendAddr, int offset, int form, byte[] data, boolean auth, boolean farSide) throws LonException {
      writeMemory(lonComm, memType, sendAddr, offset, form, data, auth, farSide, MAX_DATA_LENGTH);
   }

   private static void writeMemory(
      LonComm lonComm, int memType, LonAddress sendAddr, int offset, int form, byte[] data, boolean auth, boolean farSide, int maxLen
   ) throws LonException {
      int bytesWritten = 0;

      while (bytesWritten < data.length) {
         int len = data.length - bytesWritten;
         if (len > maxLen) {
            len = maxLen;
         }

         WriteMemRequest writeReq = new WriteMemRequest(memType, offset + bytesWritten, len, form, data, len, bytesWritten);
         if (farSide) {
            writeReq.setFarSide();
         }

         writeReq.setAuthenticate(auth);
         writeReq.setTransmitTimer(BLonRepeatTimer.milliSec384);
         writeReq.setRetryCount(6);
         lonComm.sendRequest(sendAddr, writeReq);
         bytesWritten += len;
      }
   }

   public static byte[] querySnvt(LonComm lonComm, LonAddress sendAddr, int offset, int length, boolean auth, int maxLen) throws LonException {
      int bytesRead = 0;
      byte[] doc = new byte[length];

      while (bytesRead < length) {
         int len = length - bytesRead;
         if (len > maxLen) {
            len = maxLen;
         }

         QuerySNVTRequest queryReq = new QuerySNVTRequest(offset + bytesRead, len);
         queryReq.setAuthenticate(auth);
         QuerySNVTResponse queryResp = (QuerySNVTResponse)lonComm.sendRequest(sendAddr, queryReq);
         System.arraycopy(queryResp.getData(), 0, doc, bytesRead, len);
         bytesRead += len;
      }

      return doc;
   }

   public static int getBufferCount(int val) {
      int count = 0;
      switch (val) {
         case 0:
            count = 0;
         case 1:
         default:
            break;
         case 2:
            count = 1;
            break;
         case 3:
            count = 2;
            break;
         case 4:
            count = 3;
            break;
         case 5:
            count = 5;
            break;
         case 6:
            count = 7;
            break;
         case 7:
            count = 11;
            break;
         case 8:
            count = 15;
            break;
         case 9:
            count = 23;
            break;
         case 10:
            count = 31;
            break;
         case 11:
            count = 47;
            break;
         case 12:
            count = 63;
            break;
         case 13:
            count = 95;
            break;
         case 14:
            count = 127;
            break;
         case 15:
            count = 191;
      }

      return count;
   }

   public static int getBufferSize(int val) {
      int size = 0;
      switch (val) {
         case 0:
            size = 255;
         case 1:
         default:
            break;
         case 2:
            size = 20;
            break;
         case 3:
            size = 21;
            break;
         case 4:
            size = 22;
            break;
         case 5:
            size = 24;
            break;
         case 6:
            size = 26;
            break;
         case 7:
            size = 30;
            break;
         case 8:
            size = 34;
            break;
         case 9:
            size = 42;
            break;
         case 10:
            size = 50;
            break;
         case 11:
            size = 66;
            break;
         case 12:
            size = 82;
            break;
         case 13:
            size = 114;
            break;
         case 14:
            size = 146;
            break;
         case 15:
            size = 210;
      }

      return size;
   }

   public static void verifyChannelPriorities(BLonNetwork lonworks) {
      BLonNetmgmt nmService = lonworks.netmgmt();
      BLonDevice[] lonDevices = lonworks.getLonDevices();
      BLonDevice[] priority = new BLonDevice[255];
      int numPrioritySlots = 0;
      int maxCurrentSlot = 0;
      boolean duplicate = false;

      for (int i = 0; i < lonDevices.length; i++) {
         BLonDevice dev = lonDevices[i];
         int slot = dev.getDeviceData().getPrioritySlot();
         if (slot > 0) {
            numPrioritySlots++;
            if (priority[slot] != null) {
               duplicate = true;
            } else {
               priority[slot] = dev;
            }

            if (slot > maxCurrentSlot) {
               maxCurrentSlot = slot;
            }
         }
      }

      try {
         boolean needToReasign = duplicate || maxCurrentSlot != numPrioritySlots;
         boolean numChanged = numPrioritySlots != nmService.getChannelPriorities();
         if (numChanged) {
            nmService.setChannelPriorities(numPrioritySlots);
         }

         if (numChanged || needToReasign) {
            int nxtSlot = 2;

            for (int ix = 0; ix < lonDevices.length; ix++) {
               BLonDevice dev = lonDevices[ix];
               BDeviceData dd = dev.getDeviceData();
               if (!dev.isLocal() && dd.getPrioritySlot() > 0) {
                  dd.setPrioritySlot(nxtSlot++);
               }

               setNodePriorityInfo(dev, dd.getPrioritySlot(), numPrioritySlots);
            }
         }
      } catch (LonException var13) {
         System.out.println("Failure in verifyChannelPriorities()" + var13);
      }
   }
}
