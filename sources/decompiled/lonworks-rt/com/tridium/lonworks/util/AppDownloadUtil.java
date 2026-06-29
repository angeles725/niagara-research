package com.tridium.lonworks.util;

import com.tridium.lonworks.datatypes.BAppDownloadParameter;
import com.tridium.lonworks.netmgmt.BLonNetmgmt;
import com.tridium.lonworks.xml.LonXMLReader;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import javax.baja.file.BIFile;
import javax.baja.lonworks.BDynamicDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.naming.BOrd;
import javax.baja.sys.BBlob;
import javax.baja.sys.LocalizableException;

public final class AppDownloadUtil {
   private AppDownloadUtil() {
   }

   public static BSubnetNode[] getSubnetNodes(BLonDevice[] devices) {
      BSubnetNode[] sns = new BSubnetNode[devices.length];

      for (int i = 0; i < devices.length; i++) {
         sns[i] = devices[i].getDeviceData().getSubnetNodeId();
      }

      return sns;
   }

   public static BAppDownloadParameter buildParameter(BLonDevice[] devices, BIFile appFile, BIFile xifFile, BIFile lnmlFile, boolean useZero, boolean bindLinks) throws Exception {
      BSubnetNode[] sns = getSubnetNodes(devices);
      BAppDownloadParameter param = new BAppDownloadParameter(sns);
      byte[] appFileContents = NxeUtil.nxeFileToByteArray(appFile);
      BProgramId appPid = NxeUtil.findProgramId(appFileContents);
      param.setAppFile(BBlob.make(appFileContents));
      if (xifFile != null) {
         XLonDevice lonDevice = XLonUtil.xifToXDevice(xifFile);
         lonDevice.useZeroBasedArrays = useZero;
         BProgramId xifPid = BProgramId.make(lonDevice.deviceData.programID);
         if (!appPid.equals(xifPid)) {
            throw new LocalizableException("lonworks", "appDownload.error.mismatch.deviceType");
         }

         param.setNvDirFile(BBlob.make(NxeUtil.getNvDirectionByteArray(lonDevice)));
      } else if (lnmlFile != null) {
         XLonInterfaceFile lonInterfaceFile = LonXMLReader.decode(lnmlFile);
         XLonDevice lonDevice = lonInterfaceFile.getLonDevice();
         BProgramId xmlPid = BProgramId.make(lonDevice.deviceData.programID);
         if (!appPid.equals(xmlPid)) {
            throw new LocalizableException("lonworks", "appDownload.error.mismatch.deviceType");
         }

         param.setNvDirFile(BBlob.make(NxeUtil.getNvDirectionByteArray(lonDevice)));
      }

      param.setBind(bindLinks);
      return param;
   }

   public static BOrd startJob(BAppDownloadParameter param, BLonNetmgmt netmgmt, BLonDevice[] devices, BIFile xifFile, BIFile lnmlFile) throws Exception {
      XLonInterfaceFile lonInterfaceFile = null;
      XLonDevice lonDevice = null;
      BProgramId appPid = NxeUtil.findProgramId(param.getAppFile().copyBytes());
      BOrd lnmlOrd = BOrd.DEFAULT;
      if (lnmlFile != null) {
         lnmlOrd = lnmlFile.getNavOrd();
      }

      if (xifFile != null) {
         lonDevice = XLonUtil.xifToXDevice(xifFile);
      } else if (lnmlFile != null) {
         lonInterfaceFile = LonXMLReader.decode(lnmlFile);
      }

      for (BLonDevice dev : devices) {
         BProgramId origPid = dev.getDeviceData().getProgramId();
         if (lonDevice != null) {
            NxeUtil.updateDevice(dev, lonDevice);
         } else if (lonInterfaceFile != null) {
            NxeUtil.updateDevice(dev, lonInterfaceFile);
         } else {
            dev.getDeviceData().setProgramId(appPid);
         }

         if (dev instanceof BDynamicDevice && !origPid.equals(appPid)) {
            ((BDynamicDevice)dev).setXmlFile(lnmlOrd);
         }
      }

      return netmgmt.appDownLoad(param);
   }
}
