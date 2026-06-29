package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.util.NameUtil;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.XLonFile;
import com.tridium.lonworks.xml.XConfigProperty;
import com.tridium.lonworks.xml.XDeviceData;
import com.tridium.lonworks.xml.XElementQualifier;
import com.tridium.lonworks.xml.XLonDataUtil;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XMessageTag;
import com.tridium.lonworks.xml.XNetVariable;
import com.tridium.lonworks.xml.XNetworkConfig;
import com.tridium.lonworks.xml.XNetworkVariable;
import java.util.Vector;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonScptType;
import javax.baja.lonworks.util.LonFile;

public class DocToXDevice {
   public static XLonDevice extract(LonAddress devAdr, LonComm lonComm, boolean auth) throws Exception {
      XLonDevice xdev = new XLonDevice();
      boolean extended = NmUtil.isExtended(lonComm, devAdr, auth);
      xdev.deviceData = getDeviceData(devAdr, lonComm, auth, extended);
      SelfDoc selfDoc = new SelfDoc(lonComm, devAdr, auth, extended);
      NodeSelfDoc nodeDoc = selfDoc.getNodeDoc();
      boolean validDoc = nodeDoc != null && nodeDoc.isValid();
      if (validDoc) {
         xdev.deviceData.nodeSelfID = nodeDoc.selfDocString;
      }

      if (selfDoc.hasNvs()) {
         NvDoc[] nvDocs = selfDoc.getNvDocs();

         for (int i = 0; i < nvDocs.length; i++) {
            addNetVariable(xdev, nvDocs[i], nodeDoc);
         }
      }

      LonFile file = null;

      try {
         file = XLonFile.createFile(devAdr, lonComm, xdev);
      } catch (Throwable var19) {
         System.out.println("Error attempting to access file in devices: " + var19);
         var19.printStackTrace();
      }

      if (file != null && file.findFileNum(2) == 0) {
         file.open(0, false, false);
         ConfigTemplateFile config = new ConfigTemplateFile(file.read());
         ConfigTemplateRecord[] records = config.getRecords();
         int fileNum = file.findFileNum(1);
         xdev.hasReadOnlyFile = file.findFileNum(1, fileNum) > 0;
         Vector<String> cpNames = new Vector<>(records.length);

         for (int i = 0; i < records.length; i++) {
            ConfigTemplateRecord tmplRec = records[i];
            XConfigProperty xcp = new XConfigProperty();
            xcp.scope = XLonDataUtil.scopeToString(tmplRec.getHdr());
            xcp.select = tmplRec.getSelect();
            xcp.modifyFlag = XLonDataUtil.flagToString(tmplRec.getFlags());
            xcp.length = tmplRec.getLength();
            xcp.dimension = tmplRec.getDimension();
            if (tmplRec.hasMin()) {
               xcp.min = tmplRec.getMin();
            }

            if (tmplRec.hasMax()) {
               xcp.max = tmplRec.getMax();
            }

            if (tmplRec.getTypeScope() == 0) {
               BLonScptType typ = null;

               try {
                  typ = BLonScptType.make(tmplRec.getConfigIndex());
               } catch (Throwable var18) {
                  System.out.println("Can't parse scpt type " + tmplRec.getConfigIndex());
               }

               if (typ != null) {
                  xcp.scptType = "Cp" + typ.getTag().substring(4);
               }
            }

            String name;
            if (xcp.scptType.length() > 0) {
               name = xcp.scptType;
            } else {
               name = "Config" + i;
            }

            xcp.setName(NameUtil.uniqueName(cpNames, name));
            xdev.addAttribute(xcp.getName(), xcp);
         }
      }

      for (int i = 0; i < selfDoc.getMsgtagCount(); i++) {
         XMessageTag mtag = new XMessageTag();
         mtag.index = i;
         mtag.setName("mtag" + i);
         xdev.addAttribute(mtag.getName(), mtag);
      }

      return xdev;
   }

   private static void addNetVariable(XLonDevice xdev, NvDoc nvDoc, NodeSelfDoc nodeDoc) {
      boolean config = nvDoc.isConfigNv();
      XNetVariable xnv;
      if (!config) {
         xnv = new XNetworkVariable();
      } else {
         xnv = new XNetworkConfig();
      }

      xnv.setName(nvDoc.getName());
      int snvtType = nvDoc.getSnvtType();
      if (snvtType > 0) {
         xnv.snvtType = XLonDataUtil.snvtTypeToString(snvtType);
      } else {
         XElementQualifier eq = new XElementQualifier("rawData", "na len=" + nvDoc.getNvLength());
         xnv.addAttribute("data", eq);
      }

      xnv.index = nvDoc.getNvIndex();
      xnv.arraySize = nvDoc.getArraySize();
      xnv.offline = nvDoc.isUpdateOffline();
      xnv.bindable = true;
      xnv.direction = nvDoc.getDirection().getTag();
      xnv.serviceType = nvDoc.getServiceType().getTag();
      xnv.serviceTypeConfigurable = nvDoc.isServTypeConfig();
      xnv.authenticated = nvDoc.getAuthenticated();
      xnv.authenticatedConfigurable = nvDoc.isAuthConfigurable();
      xnv.priority = nvDoc.getPriority();
      xnv.priorityConfigurable = nvDoc.isPriorityConfig();
      xnv.polled = nvDoc.isPolled();
      xnv.sync = nvDoc.isSychronous();
      xnv.config = config;
      if (!config) {
         XNetworkVariable xnvar = (XNetworkVariable)xnv;
         xnvar.objectIndex = nvDoc.getObjectIndexString();
         xnvar.memberIndex = nvDoc.getMemberIndex(0);
         xnvar.memberArraySize = nvDoc.getArraySize();
         xnvar.mfgMember = nvDoc.isMfrMember();
         xnvar.changeType = nvDoc.isChangeableType();
      } else {
         XNetworkConfig xnci = (XNetworkConfig)xnv;
         xnci.scptType = "";
         xnci.scope = nvDoc.getScope().getTag();
         xnci.select = nvDoc.getSelect();
         xnci.modifyFlag = nvDoc.getModifyFlag().encodeToString();
         xnci.max = "";
         xnci.min = "";
         xnci.changeType = nvDoc.isChangeableType();
      }

      xdev.addAttribute(xnv.getName(), xnv);
   }

   public static XDeviceData getDeviceData(LonAddress devAdr, LonComm lonComm, boolean auth, boolean extended) throws LonException {
      ReadOnlyStruct rdOnly = ReadOnlyStruct.make(devAdr, lonComm, auth, false);
      ConfigStruct cfgStruct = ConfigStruct.make(devAdr, lonComm, auth, false);
      XDeviceData devDat = new XDeviceData();
      devDat.setName("deviceData");
      devDat.majorVersion = 0;
      devDat.minorVersion = 0;
      devDat.programID = rdOnly.getProgramId().getByteArray();
      devDat.domains = rdOnly.getTwoDomains() ? 2 : 1;
      devDat.addressTableEntries = rdOnly.getAddressCount();
      devDat.handlesIncomingExplicitMessages = rdOnly.getExplicitAddressing();
      devDat.numNvDeclarations = rdOnly.getNvCount();
      devDat.numExplicitMessageTags = rdOnly.getMessageTagCount();
      devDat.networkInputBuffers = rdOnly.getNetBufInCount();
      devDat.networkOutputBuffers = rdOnly.getNetBufOutCount();
      devDat.priorityNetworkOutputBuffers = rdOnly.getNetBufOutPriorityCount();
      devDat.priorityApplicationOutputBuffers = rdOnly.getAppBufOutPriorityCount();
      devDat.applicationOutputBuffers = rdOnly.getAppBufOutCount();
      devDat.applicationInputBuffers = rdOnly.getAppBufInCount();
      devDat.sizeNetworkInputBuffer = rdOnly.getNetBufInSize();
      devDat.sizeNetworkOutputBuffer = rdOnly.getNetBufOutSize();
      devDat.sizeAppOutputBuffer = rdOnly.getAppBufOutSize();
      devDat.sizeAppInputBuffer = rdOnly.getAppBufInSize();
      devDat.applicationType = "unknown";
      devDat.numNetworkVariablesNISelect = 0;
      devDat.rcvTransactionBuffers = rdOnly.getRcvTransCount() + 1;
      devDat.aliasCount = rdOnly.getAliasCount();
      devDat.bindingII = false;
      devDat.allowStatRelativeAddressing = false;
      devDat.maxSizeWrite = 11;
      devDat.maxNumNvSupported = 0;
      devDat.neuronChipType = rdOnly.getModelNumber();
      devDat.clockRate = cfgStruct.getInputClock();
      devDat.firmwareRevision = Neuron.getVersion(lonComm, devAdr, auth);
      devDat.rcvTransactionBlockSize = 13;
      devDat.transControlBlockSize = 28;
      devDat.neuronFreeRam = 0;
      devDat.offChipFreeRam = 0;
      devDat.domainTableEntrySize = 15;
      devDat.addressTableEntrySize = 5;
      devDat.nvConfigTableEntrySize = 3;
      devDat.domainToUserSize = 112;
      devDat.nvAliasTableEntrySize = 0;
      devDat.standardTransceiverTypeUsed = false;
      devDat.standardTransceiverTypeId = 0;
      devDat.transceiverType = cfgStruct.getCommType();
      devDat.transceiverInterfaceRate = 4;
      devDat.numPrioritySlots = 0;
      devDat.minimumClockRate = 4;
      devDat.averagePacketSize = 15;
      devDat.oscillatorAccuracy = 200;
      devDat.oscillatorWakeupTime = 0;
      devDat.channelBitRate = cfgStruct.getInputClockFreq() / cfgStruct.getCommClockRatio();
      devDat.specialBitRate = false;
      devDat.specialPreambleControl = false;
      devDat.specialWakeupDirection = "input";
      devDat.overridesGenPurposeData = false;
      devDat.generalPurposeData1 = 0;
      devDat.generalPurposeData2 = 0;
      devDat.generalPurposeData3 = 0;
      devDat.generalPurposeData4 = 0;
      devDat.generalPurposeData5 = 0;
      devDat.generalPurposeData6 = 0;
      devDat.generalPurposeData7 = 0;
      devDat.rcvStartDelay = 0;
      devDat.rcvEndDelay = 0;
      devDat.indeterminateTime = 0;
      devDat.minInterpacketTime = 0;
      devDat.preambleLength = cfgStruct.getPreambleLength();
      devDat.turnaroundTime = 0;
      devDat.missedPreambleTime = 0;
      devDat.packetQualificationTime = 0;
      devDat.rawDataOverrides = true;
      devDat.rawDataClockRate = cfgStruct.getInputClock();
      devDat.rawData1 = 50;
      devDat.rawData2 = 19;
      devDat.rawData3 = 54;
      devDat.rawData4 = 100;
      devDat.rawData5 = 103;

      try {
         ExtendedSelfDoc extSdoc = ExtendedSelfDoc.make(lonComm, devAdr, auth);
         devDat.minNetMgmtVer = extSdoc.getVerNmMin();
         devDat.maxNetMgmtVer = extSdoc.getVerNmMax();
         devDat.bindingConstraintLevel = extSdoc.getVerBinding();
         devDat.ecsFlags = extSdoc.getFlags();
         devDat.numEcsDomains = extSdoc.getDomainCapacity();
         devDat.numEcsAddressEntries = extSdoc.getAddressCapacity();
         devDat.numEcsMessageTags = extSdoc.getStaticMtagCapacity();
         devDat.aliasCount = extSdoc.getAliasCount();
      } catch (Exception var8) {
      }

      return devDat;
   }
}
