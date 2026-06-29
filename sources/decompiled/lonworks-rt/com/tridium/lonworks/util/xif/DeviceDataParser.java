package com.tridium.lonworks.util.xif;

import com.tridium.lonworks.xml.XDeviceData;
import com.tridium.lonworks.xml.XLonDataUtil;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class DeviceDataParser {
   private XifLineReader reader;
   private PrintStream out;
   private XDeviceData deviceData;

   public DeviceDataParser(XifLineReader reader, PrintStream out) {
      this.reader = reader;
      this.out = out;
   }

   public XDeviceData parse() {
      this.deviceData = new XDeviceData();
      this.deviceData.setName("deviceData");

      try {
         this.parseLn1(this.reader.readXifLine());
         this.reader.readXifLine();
         this.reader.readXifLine();
         String line = this.reader.readXifLine();
         this.deviceData.setProgramID(line);
         this.parseLn6(this.reader.readXifLine());
         this.parseLn7(this.reader.readXifLine());
         if (this.deviceData.majorVersion >= 3) {
            this.parseLn8(this.reader.readXifLine());
            this.parseLn9(this.reader.readXifLine());
            this.parseLn10(this.reader.readXifLine());
         }

         this.reader.readXifLine();
         StringBuilder sb = new StringBuilder();

         for (String var4 = this.reader.readLine(); var4 != null && var4.startsWith("\""); var4 = this.reader.readXifLine(false)) {
            sb.append(var4.substring(1));
         }

         this.deviceData.nodeSelfID = sb.toString();
         if (this.deviceData.nodeSelfID.startsWith("&")) {
            this.deviceData.nodeSelfID = this.deviceData.nodeSelfID.substring(1);
         }
      } catch (IOException var3) {
      }

      return this.deviceData;
   }

   private void parseLn1(String line) {
      int startAt = line.indexOf("XIF Version ") + "XIF Version ".length();
      String version = line.substring(startAt).trim();
      int separator = version.indexOf(".");

      try {
         if (separator != -1) {
            this.deviceData.majorVersion = Integer.parseInt(version.substring(0, separator));
            this.deviceData.minorVersion = Integer.parseInt(version.substring(separator + 1));
         } else {
            this.deviceData.majorVersion = Integer.parseInt(version);
            this.deviceData.minorVersion = 0;
         }
      } catch (NumberFormatException var6) {
         this.out.println("invalid characters " + version + " in place of version number");
      }
   }

   private void parseLn6(String line) {
      StringTokenizer st = new StringTokenizer(line, " ");

      try {
         this.deviceData.domains = Integer.parseInt(st.nextToken());
         this.deviceData.addressTableEntries = Integer.parseInt(st.nextToken());
         this.deviceData.handlesIncomingExplicitMessages = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.numNvDeclarations = Integer.parseInt(st.nextToken());
         this.deviceData.numExplicitMessageTags = Integer.parseInt(st.nextToken());
         this.deviceData.networkInputBuffers = Integer.parseInt(st.nextToken());
         this.deviceData.networkOutputBuffers = Integer.parseInt(st.nextToken());
         this.deviceData.priorityNetworkOutputBuffers = Integer.parseInt(st.nextToken());
         this.deviceData.priorityApplicationOutputBuffers = Integer.parseInt(st.nextToken());
         this.deviceData.applicationOutputBuffers = Integer.parseInt(st.nextToken());
         this.deviceData.applicationInputBuffers = Integer.parseInt(st.nextToken());
         this.deviceData.sizeNetworkInputBuffer = Integer.parseInt(st.nextToken());
         this.deviceData.sizeNetworkOutputBuffer = Integer.parseInt(st.nextToken());
         this.deviceData.sizeAppOutputBuffer = Integer.parseInt(st.nextToken());
         this.deviceData.sizeAppInputBuffer = Integer.parseInt(st.nextToken());
         if (this.deviceData.majorVersion < 2) {
            return;
         }

         this.deviceData.applicationType = XLonDataUtil.applicationTypeToString(Integer.parseInt(st.nextToken()));
         this.deviceData.numNetworkVariablesNISelect = Integer.parseInt(st.nextToken());
         this.deviceData.rcvTransactionBuffers = Integer.parseInt(st.nextToken());
         if (this.deviceData.majorVersion < 3 || this.deviceData.majorVersion == 3 && this.deviceData.minorVersion < 1) {
            return;
         }

         this.deviceData.aliasCount = Integer.parseInt(st.nextToken());
         this.deviceData.bindingII = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.allowStatRelativeAddressing = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.maxSizeWrite = Integer.parseInt(st.nextToken());
         if (this.deviceData.majorVersion < 4) {
            return;
         }

         try {
            this.deviceData.maxNumNvSupported = Integer.parseInt(st.nextToken());
            if (this.deviceData.majorVersion == 4 && this.deviceData.minorVersion < 2) {
               return;
            }

            this.deviceData.minNetMgmtVer = Integer.parseInt(st.nextToken());
            this.deviceData.maxNetMgmtVer = Integer.parseInt(st.nextToken());
            this.deviceData.bindingConstraintLevel = Integer.parseInt(st.nextToken());

            for (int i = 0; i < 6; i++) {
               this.deviceData.ecsFlags[i] = Integer.parseInt(st.nextToken());
            }

            this.deviceData.numEcsDomains = Integer.parseInt(st.nextToken());
            this.deviceData.numEcsAddressEntries = Integer.parseInt(st.nextToken());
            this.deviceData.numEcsMessageTags = Integer.parseInt(st.nextToken());
         } catch (NoSuchElementException var4) {
         }
      } catch (NoSuchElementException var5) {
         this.out.println("not all expected fields were present in line six of the header.");
      }
   }

   private void parseLn7(String line) {
      StringTokenizer st = new StringTokenizer(line);

      try {
         this.deviceData.neuronChipType = Integer.parseInt(st.nextToken());
         this.deviceData.clockRate = Integer.parseInt(st.nextToken());
         if (this.deviceData.majorVersion < 3) {
            return;
         }

         this.deviceData.firmwareRevision = Integer.parseInt(st.nextToken());
         this.deviceData.rcvTransactionBlockSize = Integer.parseInt(st.nextToken());
         this.deviceData.transControlBlockSize = Integer.parseInt(st.nextToken());
         this.deviceData.neuronFreeRam = Integer.parseInt(st.nextToken());
         this.deviceData.offChipFreeRam = Integer.parseInt(st.nextToken());
         this.deviceData.domainTableEntrySize = Integer.parseInt(st.nextToken());
         this.deviceData.addressTableEntrySize = Integer.parseInt(st.nextToken());
         this.deviceData.nvConfigTableEntrySize = Integer.parseInt(st.nextToken());
         this.deviceData.domainToUserSize = Integer.parseInt(st.nextToken());
         if (this.deviceData.majorVersion == 3 && this.deviceData.minorVersion < 1) {
            return;
         }

         this.deviceData.nvAliasTableEntrySize = Integer.parseInt(st.nextToken());
         if (this.deviceData.majorVersion == 4 && this.deviceData.minorVersion < 4) {
            return;
         }

         this.deviceData.baseClockRateFactor = Integer.parseInt(st.nextToken());
      } catch (NoSuchElementException var4) {
         this.out.println("not all expected fields were present in the neuron chip configuration");
      }
   }

   private void parseLn8(String line) {
      StringTokenizer st = new StringTokenizer(line);

      try {
         this.deviceData.standardTransceiverTypeUsed = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.standardTransceiverTypeId = Integer.parseInt(st.nextToken());
         st.nextToken();
         this.deviceData.transceiverType = Integer.parseInt(st.nextToken());
         this.deviceData.transceiverInterfaceRate = Integer.parseInt(st.nextToken());
         this.deviceData.numPrioritySlots = Integer.parseInt(st.nextToken());
         this.deviceData.minimumClockRate = Integer.parseInt(st.nextToken());
         this.deviceData.averagePacketSize = Integer.parseInt(st.nextToken());
         this.deviceData.oscillatorAccuracy = Integer.parseInt(st.nextToken());
         this.deviceData.oscillatorWakeupTime = Integer.parseInt(st.nextToken());
      } catch (NoSuchElementException var4) {
         this.out.println("not all expected fields were present in the channel parameters");
      }
   }

   private void parseLn9(String line) {
      StringTokenizer st = new StringTokenizer(line);

      try {
         this.deviceData.channelBitRate = Integer.parseInt(st.nextToken());
         this.deviceData.specialBitRate = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.specialPreambleControl = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.specialWakeupDirection = Integer.parseInt(st.nextToken()) == 1 ? "output" : "input";
         this.deviceData.overridesGenPurposeData = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.generalPurposeData1 = Integer.parseInt(st.nextToken());
         this.deviceData.generalPurposeData2 = Integer.parseInt(st.nextToken());
         this.deviceData.generalPurposeData3 = Integer.parseInt(st.nextToken());
         this.deviceData.generalPurposeData4 = Integer.parseInt(st.nextToken());
         this.deviceData.generalPurposeData5 = Integer.parseInt(st.nextToken());
         this.deviceData.generalPurposeData6 = Integer.parseInt(st.nextToken());
         this.deviceData.generalPurposeData7 = Integer.parseInt(st.nextToken());
      } catch (NoSuchElementException var4) {
         this.out.println("not all expected fields were present in the transciever parameters");
      }
   }

   private void parseLn10(String line) {
      StringTokenizer st = new StringTokenizer(line);

      try {
         this.deviceData.rcvStartDelay = Integer.parseInt(st.nextToken());
         this.deviceData.rcvEndDelay = Integer.parseInt(st.nextToken());
         this.deviceData.indeterminateTime = Integer.parseInt(st.nextToken());
         this.deviceData.minInterpacketTime = Integer.parseInt(st.nextToken());
         this.deviceData.preambleLength = Integer.parseInt(st.nextToken());
         this.deviceData.turnaroundTime = Integer.parseInt(st.nextToken());
         this.deviceData.missedPreambleTime = Integer.parseInt(st.nextToken());
         this.deviceData.packetQualificationTime = Integer.parseInt(st.nextToken());
         this.deviceData.rawDataOverrides = Integer.parseInt(st.nextToken()) == 1;
         this.deviceData.rawDataClockRate = Integer.parseInt(st.nextToken());
         this.deviceData.rawData1 = Integer.parseInt(st.nextToken());
         this.deviceData.rawData2 = Integer.parseInt(st.nextToken());
         this.deviceData.rawData3 = Integer.parseInt(st.nextToken());
         this.deviceData.rawData4 = Integer.parseInt(st.nextToken());
         this.deviceData.rawData5 = Integer.parseInt(st.nextToken());
      } catch (NoSuchElementException var4) {
         this.out.println("not all expected fields were present in the channel timing parameters");
      }
   }
}
