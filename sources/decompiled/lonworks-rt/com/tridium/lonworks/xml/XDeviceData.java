package com.tridium.lonworks.xml;

import com.tridium.lonworks.util.LonByteArrayUtil;
import com.tridium.lonworks.util.LonStringUtil;

public class XDeviceData extends XLonData {
   public int majorVersion;
   public int minorVersion;
   public byte[] programID = new byte[8];
   public int domains = 2;
   public int addressTableEntries;
   public boolean handlesIncomingExplicitMessages = false;
   public int numNvDeclarations;
   public int numExplicitMessageTags;
   public int networkInputBuffers;
   public int networkOutputBuffers;
   public int priorityNetworkOutputBuffers;
   public int priorityApplicationOutputBuffers;
   public int applicationOutputBuffers;
   public int applicationInputBuffers;
   public int sizeNetworkInputBuffer;
   public int sizeNetworkOutputBuffer;
   public int sizeAppOutputBuffer;
   public int sizeAppInputBuffer;
   public String applicationType = "unknown";
   public int numNetworkVariablesNISelect;
   public int rcvTransactionBuffers;
   public int aliasCount = 0;
   public boolean bindingII = false;
   public boolean allowStatRelativeAddressing = false;
   public int maxSizeWrite = 11;
   public int maxNumNvSupported = 0;
   public int minNetMgmtVer = 0;
   public int maxNetMgmtVer = 0;
   public int bindingConstraintLevel = 0;
   public int[] ecsFlags = new int[]{3, 0, 0, 0, 0, 0};
   public int numEcsDomains = 0;
   public int numEcsAddressEntries = 0;
   public int numEcsMessageTags = 0;
   public int netMgmtVersion = 0;
   public int netMgmtCapabilities = 0;
   public int neuronChipType;
   public int clockRate;
   public int firmwareRevision;
   public int rcvTransactionBlockSize;
   public int transControlBlockSize;
   public int neuronFreeRam;
   public int offChipFreeRam;
   public int domainTableEntrySize;
   public int addressTableEntrySize;
   public int nvConfigTableEntrySize;
   public int domainToUserSize;
   public int nvAliasTableEntrySize;
   public int baseClockRateFactor = 0;
   public boolean standardTransceiverTypeUsed = true;
   public int standardTransceiverTypeId;
   public int transceiverType;
   public int transceiverInterfaceRate;
   public int numPrioritySlots;
   public int minimumClockRate;
   public int averagePacketSize;
   public int oscillatorAccuracy;
   public int oscillatorWakeupTime;
   public int channelBitRate;
   public boolean specialBitRate = false;
   public boolean specialPreambleControl = false;
   public String specialWakeupDirection = "input";
   public boolean overridesGenPurposeData = false;
   public int generalPurposeData1;
   public int generalPurposeData2;
   public int generalPurposeData3;
   public int generalPurposeData4;
   public int generalPurposeData5;
   public int generalPurposeData6;
   public int generalPurposeData7;
   public int rcvStartDelay;
   public int rcvEndDelay;
   public int indeterminateTime;
   public int minInterpacketTime;
   public int preambleLength;
   public int turnaroundTime;
   public int missedPreambleTime;
   public int packetQualificationTime;
   public boolean rawDataOverrides = false;
   public int rawDataClockRate;
   public int rawData1;
   public int rawData2;
   public int rawData3;
   public int rawData4;
   public int rawData5;
   public boolean freezeChannelPriorities = false;
   public String nodeSelfID = "";

   public XDeviceData() {
      this.setName("deviceData");
   }

   public String getProgramID() {
      return LonByteArrayUtil.toString(this.programID);
   }

   public void setProgramID(String s) {
      this.programID = LonByteArrayUtil.getBytes(s, 8);
   }

   public String getEcsFlags() {
      return LonStringUtil.toString(this.ecsFlags);
   }

   public void setEcsFlags(String s) {
      this.ecsFlags = LonStringUtil.getIntArray(s);
   }
}
