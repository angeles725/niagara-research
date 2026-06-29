package com.tridium.lonworks.util;

import com.tridium.lonworks.file.LonFileReadWrite;
import com.tridium.lonworks.file.LonFileTransfer;
import com.tridium.lonworks.file.NoDirectoryException;
import com.tridium.lonworks.xml.XLonDataUtil;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XNetworkVariable;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.util.LonFile;

public class XLonFile {
   public static LonFile createFile(LonAddress devAdr, LonComm lonComm, XLonDevice xdev) throws LonException {
      LonFile file = null;
      int dirNv;
      if ((dirNv = findLonObjectNvProperty(xdev, 0, 8, 114)) != -1) {
         file = new LonFileReadWrite(devAdr, lonComm, dirNv);
      } else {
         int reqNv;
         int statNv;
         if ((reqNv = findLonObjectNvProperty(xdev, 0, 5, 73)) == -1 || (statNv = findLonObjectNvProperty(xdev, 0, 6, 74)) == -1) {
            return null;
         }

         int posNv = findLonObjectNvProperty(xdev, 0, 7, 90);

         try {
            file = new LonFileTransfer(devAdr, lonComm, reqNv, statNv, posNv);
         } catch (NoDirectoryException var9) {
            lonComm.lonNetwork().log().warning("Device has file snvts but reports no file directory");
            return null;
         }
      }

      return file;
   }

   public static int findLonObjectNvProperty(XLonDevice xdev, int objectIndex, int memberIndex, int snvtType) throws LonException {
      XNetworkVariable[] nvs = xdev.getNetworkVariables();

      for (int i = 0; i < nvs.length; i++) {
         XNetworkVariable nv = nvs[i];

         try {
            if (nv.objectIndex.length() > 0
               && Integer.parseInt(nv.objectIndex) == objectIndex
               && nv.memberIndex == memberIndex
               && !nv.mfgMember
               && XLonDataUtil.snvtTypeFromString(nv.snvtType) == snvtType) {
               return nv.index;
            }
         } catch (Throwable var8) {
         }
      }

      return -1;
   }
}
