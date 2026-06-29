package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.NAddressManager;
import com.tridium.lonworks.datatypes.BDeviceEntry;
import com.tridium.lonworks.datatypes.BLonRouteTable;
import com.tridium.lonworks.datatypes.BRouterEntry;
import com.tridium.lonworks.datatypes.BUtilCmdJob;
import com.tridium.lonworks.datatypes.BUtilitiesCommand;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import com.tridium.lonworks.loncomm.NLonComm;
import com.tridium.lonworks.netmessages.ExEnumerateAddressResponse;
import com.tridium.lonworks.netmessages.ExEnumerateAliasResponse;
import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.netmessages.QueryAddrResponse;
import com.tridium.lonworks.netmessages.QueryAliasResponse;
import com.tridium.lonworks.netmessages.QueryDomainResponse;
import com.tridium.lonworks.netmessages.QueryStatusResponse;
import com.tridium.lonworks.netmessages.RouterStatusResponse;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.util.LonByteArrayUtil;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import com.tridium.lonworks.util.selfdoc.ConfigTemplateFile;
import com.tridium.lonworks.util.selfdoc.ConfigTemplateRecord;
import com.tridium.lonworks.util.selfdoc.NvDoc;
import com.tridium.lonworks.util.selfdoc.ReadOnlyStruct;
import com.tridium.lonworks.util.selfdoc.SelfDoc;
import com.tridium.lonworks.util.selfdoc.SnvtStruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BAuthenticationKey;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BLocal;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.enums.BLonSnvtType;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.util.LonFile;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.SortUtil;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.Clock;
import javax.baja.sys.Sys;

public class LonUtilRequest implements Runnable, NetMgmtConst, NetMessages {
   private static final int MAX_DATA_SIZE = 16;
   private BUtilitiesCommand cmd;
   private BLonDevice lonDevice;
   private BLonRouter lonRouter;
   private BLonNetmgmt netmgmt;
   private BLonNetwork lonworks;
   private NLonComm lonComm;
   private LonAddress deviceAddr;
   private String deviceName;
   private boolean auth;
   private boolean isRouter;
   private boolean isExtended;
   private boolean checkExtended;
   private PrintWriter out;
   private BUtilCmdJob job;

   public LonUtilRequest(BUtilitiesCommand cmd, BLonNetmgmt netmgmt, PrintWriter out) {
      this(cmd, netmgmt, out, null);
   }

   public LonUtilRequest(BUtilitiesCommand cmd, BLonNetmgmt netmgmt, PrintWriter out, BUtilCmdJob job) {
      this.netmgmt = netmgmt;
      this.lonworks = netmgmt.lonNetwork();
      this.lonComm = (NLonComm)this.lonworks.lonComm();
      this.cmd = cmd;
      this.out = out;
      this.job = job;
      if (cmd.isDevice()) {
         this.deviceAddr = cmd.getNeuronId();
         this.lonDevice = this.lonworks.addressManager().getDeviceByAddress(cmd.getSubnetNodeId());
         if (this.lonDevice == null) {
            this.deviceName = SlotPath.unescape(cmd.getDisplayName());
            this.auth = false;
            this.checkExtended = true;
         } else {
            this.deviceName = this.getName(this.lonDevice);
            this.auth = this.lonDevice.authenticate();
            if (this.lonDevice.isLocal()) {
               this.deviceAddr = BLocal.local;
            }

            this.isExtended = this.lonDevice.isExtended();
            this.checkExtended = false;
         }

         this.isRouter = false;
      } else {
         this.deviceAddr = cmd.getNeuronId();
         this.lonRouter = this.lonworks.addressManager().getRouterByAddress(cmd.getSubnetNodeId());
         if (this.lonRouter == null) {
            this.deviceName = SlotPath.unescape(cmd.getDisplayName());
            this.auth = false;
         } else {
            this.deviceName = this.getName(this.lonRouter);
            this.auth = this.lonRouter.authenticate();
         }

         this.isRouter = true;
      }

      try {
         this.auth = Neuron.isNMAuthSet(this.lonComm, this.deviceAddr, this.auth, false);
      } catch (Throwable var6) {
      }
   }

   @Override
   public void run() {
      this.execute();
   }

   public void execute() {
      if (!this.cmd.getCommand().isNoDeviceCmd() && this.deviceAddr.getAddressType() == 2 && ((BNeuronId)this.deviceAddr).isZero()) {
         this.append("Can not execute utility on " + this.deviceName + ". NeuronId is zero.");
      } else {
         try {
            String resp = "Utility not implemented";
            boolean isLocal = this.lonDevice == null ? false : this.lonDevice.isLocal();
            switch (this.cmd.getCommand().getOrdinal()) {
               case 1:
                  resp = this.discover();
               case 2:
               case 3:
               case 4:
               case 6:
               default:
                  break;
               case 5:
                  resp = this.disableAuthentication();
                  break;
               case 7:
                  resp = this.readMemory();
                  break;
               case 8:
                  this.test();
                  break;
               case 9:
                  resp = this.getStatus();
                  break;
               case 10:
                  NmUtil.clearStatus(this.lonComm, this.deviceAddr, this.auth, false);
                  if (this.isRouter) {
                     NmUtil.clearStatus(this.lonComm, this.deviceAddr, this.auth, true);
                  }

                  resp = this.getStatus();
                  break;
               case 11:
                  NmUtil.setDeviceState(this.lonComm, this.deviceAddr, BLonNodeState.unconfigured, isLocal, this.auth, false);
                  if (this.isRouter) {
                     NmUtil.setDeviceState(this.lonComm, this.deviceAddr, BLonNodeState.unconfigured, isLocal, this.auth, true);
                  }

                  this.updateNodeState(BLonNodeState.unconfigured);
                  resp = this.getStatus();
                  break;
               case 12:
                  NmUtil.setDeviceState(this.lonComm, this.deviceAddr, BLonNodeState.configOnline, isLocal, this.auth, false);
                  if (this.isRouter) {
                     NmUtil.setDeviceState(this.lonComm, this.deviceAddr, BLonNodeState.configOnline, isLocal, this.auth, true);
                  }

                  this.updateNodeState(BLonNodeState.configOnline);
                  resp = this.getStatus();
                  break;
               case 13:
                  NmUtil.resetNode(this.lonComm, this.deviceAddr, this.auth, false);
                  if (this.isRouter) {
                     NmUtil.resetNode(this.lonComm, this.deviceAddr, this.auth, true);
                  }

                  resp = this.getStatus();
                  break;
               case 14:
                  resp = this.getFileDirectory();
                  break;
               case 15:
                  resp = this.getConfigTemplate();
                  break;
               case 16:
                  resp = this.getConfigValue();
                  break;
               case 17:
                  resp = this.getOtherFile();
                  break;
               case 18:
                  resp = this.wink();
                  break;
               case 19:
                  resp = this.servicePin();
                  break;
               case 20:
                  resp = this.clearServicePin();
                  break;
               case 21:
                  resp = this.getAddressTable();
                  break;
               case 22:
                  resp = this.getDomainTable();
                  break;
               case 23:
                  resp = this.getReadOnlyStruct();
                  break;
               case 24:
                  resp = this.getConfigStruct();
                  break;
               case 25:
                  resp = this.getAliasTable();
                  break;
               case 26:
                  resp = this.getNvConfig();
                  break;
               case 27:
                  resp = this.getNvValue();
                  break;
               case 28:
                  resp = this.getSelfDoc();
                  break;
               case 29:
                  resp = this.getRouteTable();
                  break;
               case 30:
                  resp = this.getNetMgmtSummary();
                  break;
               case 31:
                  resp = this.getProgramIdSummary();
                  break;
               case 32:
                  resp = this.getTransmitErrors(true);
                  break;
               case 33:
                  resp = this.getTransmitErrors(false);
                  break;
               case 34:
                  resp = this.verify();
                  break;
               case 35:
                  resp = this.getNetworkSummary();
                  break;
               case 36:
                  if (this.lonDevice != null) {
                     NmUtil.recalculateChecksum(this.lonDevice, 1);
                     resp = "Recalculated checksum on " + this.getName(this.lonDevice);
                  } else {
                     resp = "This action only allowed on database devices.";
                  }
                  break;
               case 37:
                  VerifyChannels.verify(this.lonworks, this.out);
                  resp = "";
            }

            if (resp.length() > 0) {
               this.append(resp);
            }
         } catch (JobCancelException var3) {
            this.job.canceled();
         } catch (Throwable var4) {
            String s = "LonUtil command failed.\n" + var4.toString();
            this.netmgmt.log().log(Level.SEVERE, s, var4);
            this.append(s);
         }
      }
   }

   private String getOtherFile() throws LonException {
      if (this.lonDevice == null) {
         return "Cannot access files on devices not added to database.";
      } else {
         int fileNum = this.cmd.getFileNum();
         LonFile file = this.lonDevice.getLonFileOpen(fileNum, false, false);
         if (file == null) {
            return "No files on " + this.deviceName;
         } else {
            this.append("Hex file dump for filenum ").append(fileNum).append("\n");

            byte[] data;
            try {
               data = file.read();
            } catch (LonException var6) {
               file.close();
               throw var6;
            }

            int cnt = 0;

            while (cnt < data.length) {
               int len = data.length - cnt;
               if (len > 16) {
                  len = 16;
               }

               this.append(LonByteArrayUtil.toString(data, 16, ' ', len, cnt, true));
               this.append("   ");
               this.bytesToString(data, cnt, len);
               this.append("\n");
               cnt += len;
            }

            file.close();
            return "";
         }
      }
   }

   private String getConfigTemplate() throws LonException {
      if (this.lonDevice == null) {
         return "Cannot access files on devices not added to database.";
      } else {
         LonFile file = this.lonDevice.getLonFileOpen(0, false, false);
         if (file == null) {
            return "No files on " + this.deviceName;
         } else {
            this.append("Configuration Template File for " + this.deviceName + "\n");
            this.append("FILE index 0 type 2\n");

            ConfigTemplateFile config;
            try {
               config = new ConfigTemplateFile(file.read());
            } catch (LonException var4) {
               file.close();
               throw var4;
            }

            config.toString(this.out);
            file.close();
            return "";
         }
      }
   }

   private String getFileDirectory() throws LonException {
      if (this.lonDevice == null) {
         return "Cannot access files on devices not added to database.";
      } else {
         LonFile file = this.lonDevice.getLonFileOpen(0, false, false);
         return file == null ? "No files on " + this.deviceName : file.getDirectoryString();
      }
   }

   private String getConfigValue() throws LonException {
      if (this.lonDevice == null) {
         return "Cannot access files on devices not added to database.";
      } else {
         LonFile file = this.lonDevice.getLonFileOpen(0, false, false);
         if (file == null) {
            return "No files on " + this.deviceName;
         } else {
            int fileNum = file.findFileNum(2);
            if (fileNum == -1) {
               return "No template file on " + this.deviceName;
            } else {
               LonInputStream readOnly = null;
               this.append("Configuration Value File for " + this.deviceName + "\n");
               ConfigTemplateFile config = new ConfigTemplateFile(file.read());
               file.close();
               fileNum = file.findFileNum(1);
               file.open(fileNum, false, false);
               LonInputStream readWrite = new LonInputStream(file.read());
               file.close();
               fileNum = file.findFileNum(1, fileNum);
               if (fileNum > 0) {
                  file.open(fileNum, false, false);
                  readOnly = new LonInputStream(file.read());
                  file.close();
               }

               ConfigTemplateRecord[] records = config.getRecords();
               int rdWrOffset = 0;
               int rdOnlyOffset = 0;
               String[] hdrs = new String[]{"Select", "Type", "Access", "Offset", "Value"};
               PTable ptab = new PTable(hdrs, 2);
               ptab.setColumnAttribute(2, 0);
               StringBuilder dataBuf = new StringBuilder();

               for (ConfigTemplateRecord record : records) {
                  int numRec = record.getNumRecs();

                  for (int n = 0; n < numRec; n++) {
                     int hdr = record.getHdr();
                     if (hdr == 1) {
                        ptab.add("Obj " + record.getSelect());
                     } else if (hdr == 2) {
                        ptab.add("Nv " + record.getSelect());
                     } else {
                        ptab.add("Node");
                     }

                     if (record.isScpt()) {
                        ptab.add("Scpt" + record.getConfigIndex());
                     } else {
                        ptab.add("Ucpt" + record.getConfigIndex());
                     }

                     if (record.isReadWrite()) {
                        ptab.add("R/W");
                     } else {
                        ptab.add("R/O");
                     }

                     int len = record.getLength();
                     dataBuf.setLength(0);
                     if (!record.isReadWrite() && readOnly != null) {
                        ptab.add(rdOnlyOffset);

                        for (int j = 0; j < len; j++) {
                           dataBuf.append(Integer.toString(readOnly.read(), 16) + " ");
                        }

                        rdOnlyOffset += len;
                     } else {
                        ptab.add(rdWrOffset);

                        for (int j = 0; j < len; j++) {
                           dataBuf.append(Integer.toString(readWrite.read(), 16) + " ");
                        }

                        rdWrOffset += len;
                     }

                     ptab.add(dataBuf.toString());
                  }
               }

               ptab.toString(this.out);
               if (readOnly != null) {
                  this.append("NOTE: This device has seperate file for readOnly parameters.");
               }

               return "";
            }
         }
      }
   }

   private String getAddressTable() throws LonException {
      int tabLen = Neuron.getAddressCount(this.lonComm, this.deviceAddr, this.auth, this.isExtended());
      this.append("Address table for " + this.deviceName + "\n");
      String[] hdrs = new String[]{"Index", "Type", "Size", "Group/Subnet", "Member/Node", "Domain Ndx", "Repeat", "Retry", "Receive", "Transmit", "Restrict"};
      PTable ptab = new PTable(hdrs);

      for (int i = 0; i < tabLen; i++) {
         QueryAddrResponse qryRsp;
         try {
            qryRsp = NmUtil.getAddressTableEntry(this.lonComm, this.deviceAddr, i, this.auth, this.isExtended());
            if (qryRsp.isExtended()) {
               i = ((ExEnumerateAddressResponse)qryRsp).getAddressIndex();
            }
         } catch (FailedResponseException var7) {
            break;
         }

         ptab.add(i);
         switch (qryRsp.getAddressType().getOrdinal()) {
            case 0:
               ptab.addBanner("Not in use");
               continue;
            case 1:
               ptab.add("Group");
               ptab.add(qryRsp.getSize());
               break;
            case 2:
               ptab.add("Subnet");
               ptab.add("");
               break;
            case 3:
               ptab.add("Broadcast");
               ptab.add("");
               break;
            case 4:
               ptab.add("Turnaround");
               ptab.newRow();
               continue;
            default:
               ptab.add("error");
         }

         ptab.add(qryRsp.groupOrSubnet);
         ptab.add(qryRsp.memberOrNode);
         ptab.add(qryRsp.domainIndex);
         ptab.add(BLonRepeatTimer.make(qryRsp.repeatTimer).getTag());
         ptab.add(qryRsp.retryCount);
         ptab.add(BLonReceiveTimer.make(qryRsp.receiveTimer).getTag());
         ptab.add(BLonRepeatTimer.make(qryRsp.xmitTimer).getTag());
         ptab.add(qryRsp.getRestriction().getTag());
      }

      ptab.toString(this.out);
      return "";
   }

   private String getReadOnlyStruct() throws LonException {
      this.append("Read Only data structure for " + this.deviceName + "\n");
      this.getReadOnlyStruct(false);
      if (this.isRouter) {
         this.append("\n\nFar side");
         this.getReadOnlyStruct(true);
      }

      return "";
   }

   private void getReadOnlyStruct(boolean farSide) throws LonException {
      ReadOnlyStruct ro = ReadOnlyStruct.make(this.deviceAddr, this.lonComm, this.auth, farSide);
      this.append("\n  neuronId             =  " + ro.getNeuronId());
      int model = ro.getModelNumber();
      this.append("\n  model Number         =  " + (model == 0 ? "3150" : (model == 8 ? "3120" : model)));
      this.append("\n  minor Model Number   =  " + ro.getMinorModelNumber());
      this.append("\n  nvFixed pointer      =  " + Integer.toString(ro.getNvFixedPointer(), 16));
      this.append("\n  Read Write Protect   =  " + ro.getReadWriteProtect());
      this.append("\n  Network variables    =  " + ro.getNvCount());
      this.append("\n  Snvt structs pointer =  " + Integer.toString(ro.getSnvtStructsPointer(), 16));
      this.append("\n  Program Id           =  " + ro.getProgramId());
      byte[] a = ro.getProgramId().getByteArray();
      if ((a[0] & 128) == 0) {
         this.append(" \"");
         this.getString(a, 0, 8);
         this.append("\"");
      }

      this.append("\n  Nv Processing Off    =  " + ro.getNvProcessingOff());
      this.append("\n  Two Domains          =  " + ro.getTwoDomains());
      this.append("\n  Explicit Addressing  =  " + ro.getExplicitAddressing());
      this.append("\n  Address Count        =  " + ro.getAddressCount());
      this.append("\n  TX by address        =  " + ro.getTxByAddress());
      this.append("\n  Idempotent duplicate =  " + ro.getIdempotentDuplicate());
      this.append("\n  Alias Count          =  " + ro.getAliasCount());
      this.append("\n  Message Tag Count    =  " + ro.getMessageTagCount());
      this.append("\n  Compatibility Info   =  " + ro.getHasEcs());
      this.append("\n");
      this.append("\n  Receive Transaction Count                     =  " + (ro.getRcvTransCount() + 1));
      this.append("\n  Size of Output Application Buffer             =  " + this.getBufferSize(ro.getAppBufOutSize()));
      this.append("\n  Size of Input Application Buffer              =  " + this.getBufferSize(ro.getAppBufInSize()));
      this.append("\n  Size of Output Network Buffer                 =  " + this.getBufferSize(ro.getNetBufOutSize()));
      this.append("\n  Size of Input Network Buffer                  =  " + this.getBufferSize(ro.getNetBufInSize()));
      this.append("\n  Number of Priority Output Network Buffers     =  " + this.getBufferCount(ro.getNetBufOutPriorityCount()));
      this.append("\n  Number of Priority Output Application Buffers =  " + this.getBufferCount(ro.getAppBufOutPriorityCount()));
      this.append("\n  Number of Output Application Buffers          =  " + this.getBufferCount(ro.getAppBufOutCount()));
      this.append("\n  Number of Input Application Buffers           =  " + this.getBufferCount(ro.getAppBufInCount()));
      this.append("\n  Number of Output Network Buffers              =  " + this.getBufferCount(ro.getNetBufOutCount()));
      this.append("\n  Number of Input Network Buffers               =  " + this.getBufferCount(ro.getNetBufInCount()));
   }

   private String getBufferCount(int val) {
      int count = Neuron.getBufferCount(val);
      return count + " (" + val + ")";
   }

   private String getBufferSize(int val) {
      int size = Neuron.getBufferSize(val);
      return size + " (" + val + ")";
   }

   private void getString(byte[] a, int offset, int len) {
      int cnt = 0;

      while (cnt < len) {
         byte b = a[offset + cnt++];
         this.append(b >= 48 && b <= 122 ? (char)b : '.');
      }
   }

   private String getConfigStruct() throws LonException {
      this.append("Config data structure for " + this.deviceName + "\n");
      this.getConfigStruct(false);
      if (this.isRouter) {
         this.append("Far side");
         this.getConfigStruct(true);
      }

      return "";
   }

   private void getConfigStruct(boolean farSide) throws LonException {
      byte[] config = Neuron.getConfigStruct(this.lonComm, this.deviceAddr, this.auth, farSide);
      this.append("\n  Channel Id              =  " + ((config[0] & 255) << 8 | config[1] & 0xFF));
      this.append("\n  Location                =  " + LonByteArrayUtil.toString(config, 16, ' ', 6, 2));
      this.append(" \"");
      this.getString(config, 2, 6);
      this.append("\"");
      this.append("\n  Comm clock              =  " + this.getCommClock((config[8] & 248) >> 3));
      this.append("\n  Input clock             =  " + this.getInputClock(config[8] & 7));
      this.append("\n  Comm type               =  " + this.getCommType((config[9] & 224) >> 5));
      int com_pin = config[9] & 31;
      this.append("\n  Comm pin direction      =  " + this.getCommPinDirection(com_pin));
      this.append("\n  Preamble length         =  " + config[10]);
      this.append("\n  Packet cycle            =  " + config[11]);
      this.append("\n  Beta2 control           =  " + config[12]);
      this.append("\n  Xmit interpacket        =  " + config[13]);
      this.append("\n  Recv interpacket        =  " + config[14]);
      this.append("\n  Node priority           =  " + config[15]);
      this.append("\n  Channel priorities      =  " + config[16]);
      int num_com_params = 7;
      int offset = 17;
      if (com_pin == 12 || com_pin == 14) {
         int b1 = config[17];
         int b2 = config[18];
         this.append("\n   Col Det               =  " + this.toBool(b1, 7));
         this.append("\n   Bit Sync              =  " + ((b1 & 96) >> 5));
         this.append("\n   Filter                =  " + ((b1 & 24) >> 3));
         this.append("\n   Hysteresis            =  " + (b1 & 7));
         this.append("\n   ColDet toEnd          =  " + ((b2 & 252) >> 3));
         this.append("\n   ColDet Tail           =  " + (b2 & 2));
         this.append("\n   ColDet Preamble       =  " + this.toBool(b2 & 1, 0));
         num_com_params -= 2;
         offset += 2;
      }

      this.append("\n  Transceiver parameters  =  " + LonByteArrayUtil.toString(config, 16, ' ', num_com_params, offset));
      this.append("\n  Nongroup timer          =  " + this.getNongroupTimer((config[24] & 240) >> 4));
      this.append("\n  Net mgmt authentication =  " + this.toBool(config[24], 3));
      this.append("\n  Preemption timeout        =  " + this.getPreemptionTimeout(config[24] & 7));
      this.append("\n\n");
   }

   private String getCommClock(int val) {
      switch (val) {
         case 0:
            return "8:1";
         case 1:
            return "16:1";
         case 2:
            return "32:1";
         case 3:
            return "64:1";
         case 4:
            return "128:1";
         case 5:
            return "256:1";
         case 6:
            return "512:1";
         case 7:
            return "1,024:1";
         case 8:
            return "2,048:1";
         default:
            return "Invalid value " + val;
      }
   }

   private String getInputClock(int val) {
      switch (val) {
         case 0:
            return "Not Used";
         case 1:
            return "625 kHz";
         case 2:
            return "1.25 MHz";
         case 3:
            return "2.5 MHz";
         case 4:
            return "5.0 MHz";
         case 5:
            return "10.0 MHz";
         case 6:
            return "20.0 MHz";
         case 7:
            return "40.0 MHz";
         default:
            return "Invalid value " + val;
      }
   }

   private String getCommType(int val) {
      switch (val) {
         case 0:
            return "Blank Neuron Chip";
         case 1:
            return "Single-ended";
         case 2:
            return "Special-purpose";
         case 3:
         case 4:
         default:
            return "Invalid value " + val;
         case 5:
            return "Differential";
      }
   }

   private String getCommPinDirection(int val) {
      switch (val) {
         case 0:
            return "Blank Neuron Chip";
         case 12:
            return "Direct mode - differential";
         case 14:
            return "Direct mode - single-ended";
         case 23:
            return "Special purpose - wake up pin is input";
         case 30:
            return "Special purpose - wake up pin is output";
         default:
            return "Invalid value " + val;
      }
   }

   private String getNongroupTimer(int val) {
      return BLonReceiveTimer.make(val).getTag();
   }

   private String getPreemptionTimeout(int val) {
      switch (val) {
         case 0:
            return "Forever";
         case 1:
            return "2 seconds";
         case 2:
            return "4 seconds";
         case 3:
            return "6 seconds";
         case 4:
            return "8 seconds";
         case 5:
            return "10 seconds";
         case 6:
            return "12 seconds";
         case 7:
            return "14 seconds";
         default:
            return "Invalid value " + val;
      }
   }

   private String wink() throws LonException {
      NmUtil.wink(this.lonComm, this.deviceAddr, this.auth);
      return "wink complete";
   }

   private String clearServicePin() throws LonException {
      this.lonworks.netMessageReceiver().clearServicePin();
      return "";
   }

   private String servicePin() throws LonException {
      this.lonDevice = null;
      this.append("Waiting on service pin.\n\n");
      ServicePin srvpin = NmUtil.receiveServicePin(this.netmgmt);
      if (srvpin == null) {
         return "Cancel service pin.";
      } else {
         this.append("Received service pin:");
         String nId = LonByteArrayUtil.toString(srvpin.getNeuronId().getByteArray(), ':');
         this.append("\n  NeuronId    = " + nId);
         byte[] pId = srvpin.getIdString().getByteArray();
         this.append("\n  Program Id  = " + LonByteArrayUtil.toString(pId, ':'));
         if ((pId[0] & 128) == 0) {
            this.append(" \"");
            this.getString(pId, 0, 8);
            this.append("\"");
         }

         this.deviceAddr = srvpin.getNeuronId();
         this.append("\n\n\n");
         this.deviceName = nId;

         try {
            this.append(this.getDomainTable());
            return "";
         } catch (LonException var6) {
            try {
               this.auth = true;
               this.append(this.getDomainTable());
               this.append("\n**** Authentication set ****");
            } catch (LonException var5) {
               this.append("Unable to read domain table.");
               this.append("\nDevice possibly authenticated with different authentication key.");
               this.append("\nMust set authentication key on LonNetwork\\LonNetmgmt and commision");
               this.append("\nthe LocalLondevice.");
            }

            return "";
         }
      }
   }

   private String getRouteTable() throws LonException {
      if (!this.isRouter) {
         return "Not a router.";
      } else {
         this.append("Routing table for " + this.deviceName + "\n");
         QueryStatusResponse status = NmUtil.queryStatus(this.lonComm, this.deviceAddr, 1, this.auth, false);
         if (status.versionNumber < 5) {
            return "Version " + status.versionNumber + " does not support table report.";
         } else {
            String[] hdrs = new String[]{"Domain", "Storage", "Type", "Side", "Table"};
            PTable ptab = new PTable(hdrs);
            int numDomains = this.getNumberDomains();

            for (int dom = 0; dom < numDomains; dom++) {
               this.getRouterTable(ptab, dom, 1, 0, this.auth, false);
               this.getRouterTable(ptab, dom, 0, 0, this.auth, false);
               this.getRouterTable(ptab, dom, 1, 0, this.auth, true);
               this.getRouterTable(ptab, dom, 0, 0, this.auth, true);
               this.getRouterTable(ptab, dom, 1, 1, this.auth, false);
               this.getRouterTable(ptab, dom, 0, 1, this.auth, false);
               this.getRouterTable(ptab, dom, 1, 1, this.auth, true);
               this.getRouterTable(ptab, dom, 0, 1, this.auth, true);
            }

            ptab.toString(this.out);
            return "";
         }
      }
   }

   private void getRouterTable(PTable ptab, int dom, int grp, int ram, boolean auth, boolean farSide) throws LonException {
      byte[] tab = RouterUtil.getRouterTable(this.lonComm, this.deviceAddr, dom, grp, ram, auth, farSide);
      ptab.add(dom);
      ptab.add(ram == 0 ? "RAM" : "EEPROM");
      ptab.add(grp == 1 ? "Group" : "Subnet");
      ptab.add(farSide ? "FarSide" : "NearSide");
      ptab.add(LonByteArrayUtil.toString(tab, true));
   }

   private String getDomainTable() throws LonException {
      this.getDomainTable(false);
      if (this.isRouter) {
         this.getDomainTable(true);
      }

      return "";
   }

   private int getNumberDomains() throws LonException {
      if (this.lonDevice != null) {
         return this.lonDevice.getDeviceData().getTwoDomains() ? 2 : 1;
      } else {
         return Neuron.isTwoDomains(this.lonComm, this.deviceAddr, this.auth, false) ? 2 : 1;
      }
   }

   private void getDomainTable(boolean farSide) throws LonException {
      int numDomains = this.getNumberDomains();
      if (!farSide) {
         this.append("Domain table for " + this.deviceName + "\n");
      } else {
         this.append("\nDomain table for far side\n");
      }

      String[] hdrs = new String[]{"Index", "Subnet", "Node", "Auth key", "Domain Len", "Domain Id"};
      PTable ptab = new PTable(hdrs);

      for (int index = 0; index < numDomains; index++) {
         QueryDomainResponse domain;
         try {
            domain = NmUtil.queryDomain(this.lonComm, this.deviceAddr, index, this.auth, farSide, this.isExtended());
         } catch (Throwable var8) {
            continue;
         }

         ptab.add(index);
         if (!domain.inUse()) {
            ptab.addBanner("Not in use");
         } else {
            ptab.add(domain.subnet);
            ptab.add(domain.node);
            ptab.add(LonByteArrayUtil.toString(domain.key));
            ptab.add(domain.len);
            if (domain.len > 0) {
               ptab.add(LonByteArrayUtil.toString(domain.id, 16, ' ', domain.len));
            }

            ptab.newRow();
         }
      }

      ptab.toString(this.out);
   }

   private String getNvConfig() throws LonException {
      this.append("Network Variable config table for " + this.deviceName + "\n");
      String[] hdrs = new String[]{"NvIndex", "Direction", "Selector", "Address Index", "Service", "Priority", "Authenticate", "Turnaround"};
      PTable ptab = new PTable(hdrs);
      int nvCount = this.lonDevice != null ? this.getNvCount(this.lonDevice) : this.getNvCount(this.deviceAddr);

      for (int index = 0; index < nvCount; index++) {
         BNvConfigData nvConfig;
         try {
            nvConfig = NmUtil.queryNvConfigData(this.lonComm, this.deviceAddr, index, this.auth, this.isExtended());
         } catch (FailedResponseException var7) {
            NmUtil.resetNode(this.lonComm, this.deviceAddr, this.auth, false);
            break;
         }

         ptab.add(index);
         ptab.add(nvConfig.getDirection());
         ptab.add(Integer.toString(nvConfig.getSelector(), 16));
         ptab.add(nvConfig.getAddrIndex());
         ptab.add(nvConfig.getServiceType());
         ptab.add(this.toBool(nvConfig.getPriority()));
         ptab.add(this.toBool(nvConfig.getAuthenticated()));
         ptab.add(this.toBool(nvConfig.getTurnAround()));
      }

      ptab.toString(this.out);
      return "";
   }

   private String getAliasTable() throws LonException {
      boolean ext = this.isExtended();
      int aliasCnt = this.lonDevice != null
         ? this.lonDevice.getDeviceData().getAliasTable().getAliasCount()
         : Neuron.getAliasCount(this.lonComm, this.deviceAddr, this.auth);
      if (aliasCnt <= 0) {
         return "Alias count = 0";
      } else {
         int aliasOffset = Neuron.getAliasOffset(this.lonComm, this.deviceAddr, this.auth, ext);
         this.append("Alias Network Variable config table for " + this.deviceName + "\n");
         String[] hdrs;
         if (ext) {
            hdrs = new String[]{
               "AliasIndex",
               "PrimaryNv",
               "Direction",
               "Selector",
               "Address Index",
               "Service",
               "Priority",
               "Authenticate",
               "Turnaround",
               "TargetNvIndex",
               "WriteByIndex",
               "RemoteNvAuth",
               "ReadByIndex"
            };
         } else {
            hdrs = new String[]{"AliasIndex", "PrimaryNv", "Direction", "Selector", "Address Index", "Service", "Priority", "Authenticate", "Turnaround"};
         }

         PTable ptab = new PTable(hdrs);

         for (int index = 0; index < aliasCnt; index++) {
            BAliasConfigData aliasConfig;
            QueryAliasResponse aliasRsp;
            try {
               aliasRsp = NmUtil.queryAliasConfigData(this.lonComm, this.deviceAddr, index, aliasOffset, this.auth, ext);
               aliasConfig = aliasRsp.getAliasConfigData();
               if (aliasRsp.isExtended()) {
                  index = ((ExEnumerateAliasResponse)aliasRsp).getAliasIndex();
               }
            } catch (FailedResponseException var10) {
               break;
            }

            ptab.add(index);
            ptab.add(aliasConfig.getPrimary());
            ptab.add(aliasConfig.getDirection());
            ptab.add(Integer.toString(aliasConfig.getSelector(), 16));
            ptab.add(aliasConfig.getAddrIndex());
            ptab.add(aliasConfig.getServiceType());
            ptab.add(this.toBool(aliasConfig.getPriority()));
            ptab.add(this.toBool(aliasConfig.getAuthenticated()));
            ptab.add(this.toBool(aliasConfig.getTurnAround()));
            if (ext) {
               ExEnumerateAliasResponse eAliasRsp = (ExEnumerateAliasResponse)aliasRsp;
               ptab.add(eAliasRsp.getTgtNvIndex());
               ptab.add(this.toBool(eAliasRsp.getNvWriteByIndex()));
               ptab.add(this.toBool(eAliasRsp.getNvRemoteNvAuth()));
               ptab.add(this.toBool(eAliasRsp.getNvReadByIndex()));
            }
         }

         ptab.toString(this.out);
         return "";
      }
   }

   private String getNvValue() throws LonException {
      this.append("Network Variable value table for " + this.deviceName + "\n");
      PTable ptab;
      if (this.lonDevice != null) {
         String[] hdrs = new String[]{"Ndx", "Name", "Data"};
         ptab = new PTable(hdrs, 2);
      } else {
         String[] hdrs = new String[]{"Ndx", "Data"};
         ptab = new PTable(hdrs, 2);
      }

      int numNvs = Neuron.getAliasOffset(this.lonComm, this.deviceAddr, this.auth, this.isExtended());
      BINetworkVariable[] nvs = this.lonDevice != null ? this.lonDevice.getNetworkVariables() : null;

      for (int index = 0; index < numNvs; index++) {
         String data;
         try {
            byte[] nvData = NmUtil.fetchNv(this.lonComm, this.deviceAddr, index, this.auth);
            data = LonByteArrayUtil.toString(nvData);
         } catch (FailedResponseException var8) {
            break;
         } catch (Throwable var9) {
            data = "*** ERROR:" + var9.getMessage();
         }

         ptab.add(index);
         if (this.lonDevice != null) {
            if (index < nvs.length && nvs[index] != null) {
               ptab.add(nvs[index].getName() + "  ");
            } else {
               ptab.add("");
            }
         }

         ptab.add(data);
      }

      ptab.toString(this.out);
      return "";
   }

   private String discover() throws LonException {
      BLonDiscoverJob dis = new BLonDiscoverJob(this.netmgmt);
      dis.doDiscover();
      boolean found = false;
      BDeviceEntry[] entries = this.netmgmt.getDeviceDiscoverTable().getDeviceEntries();
      if (entries.length > 0) {
         this.append("Discovered devices\n");
         String[] hdrs = new String[]{"Channel", "S/N", "Status", "Mfg", "Program Id", "NeuronId"};
         PTable ptab = new PTable(hdrs);

         for (int i = 0; i < entries.length; i++) {
            BDeviceEntry e = entries[i];
            ptab.add(e.getChannelId());
            ptab.add(e.getSubnet() + "/" + e.getNode());
            ptab.add(e.getState());
            ptab.add(e.getMfgId().getConvertedName());
            ptab.add(e.getProgramId());
            ptab.add(e.getNeuronId());
         }

         ptab.toString(this.out);
         found = true;
      }

      BRouterEntry[] rtrEntries = this.netmgmt.getRouterDiscoverTable().getRouterEntries();
      if (rtrEntries.length > 0) {
         this.append("Discovered routers\n");
         String[] hdrs = new String[]{"NearChannel", "Near S/N", "FarChannel", "Far S/N", "Status", "NeuronId"};
         PTable ptab = new PTable(hdrs);

         for (int i = 0; i < rtrEntries.length; i++) {
            BRouterEntry e = rtrEntries[i];
            ptab.add(e.getNearChannel());
            ptab.add(e.getNearAddress());
            ptab.add(e.getFarChannel());
            ptab.add(e.getFarAddress());
            ptab.add(e.getState());
            ptab.add(e.getNeuronId());
         }

         ptab.toString(this.out);
         found = true;
      }

      if (!found) {
         this.append("No new devices discovered.");
      }

      BLonDevice[] devs = this.lonworks.addressManager().getDeviceList(false);

      for (int i = 0; i < devs.length; i++) {
         BLonDevice dev = devs[i];

         try {
            StringBuilder sb = new StringBuilder();
            if (this.verifyDomainTable(dev, sb)) {
               this.append("\n" + this.getName(dev) + " domain table mismatch:" + sb.toString() + "\n");
            }
         } catch (LonException var9) {
            this.append("\nUnable to verify domain table for " + this.getName(dev) + ":" + var9.getMessage());
         }
      }

      return "";
   }

   public String verify() {
      this.append("Verify physical device and database match.\n");
      this.append("======================================");

      try {
         if (this.isRouter) {
            this.verifyRouter(this.lonRouter);
         } else if (this.lonDevice != null) {
            if (!this.lonDevice.isLocal()) {
               this.verifyDevice(this.lonDevice);
            } else {
               NAddressManager am = (NAddressManager)this.lonworks.addressManager();
               BLonDevice[] devs = am.getDeviceList(true);

               for (int i = 0; i < devs.length; i++) {
                  BLonDevice dev = devs[i];
                  if (!dev.isLocal()) {
                     this.verifyDevice(dev);
                     if (this.job != null) {
                        this.job.progress((i + 1) * 100 / devs.length);
                     }
                  }
               }

               BLonRouter[] rtrs = am.getRouterList();

               for (int ix = 0; ix < rtrs.length; ix++) {
                  this.verifyRouter(rtrs[ix]);
               }
            }
         }

         this.append("\n\nReport run at " + Clock.time() + "\n");
         this.append("To correct discrepancies do replaceNode on device if device in error\n");
         this.append("   upload links if database in error.\n");
         this.append("\n");
         return "";
      } catch (Throwable var5) {
         String s = "Error accessing in verify.\n\n" + var5.toString();
         System.out.println(s);
         var5.printStackTrace();
         return s;
      }
   }

   private String getName(BComponent c) {
      return c.getDisplayName(null);
   }

   private void verifyRouter(BLonRouter rtr) {
      this.append("\n\nVerify " + this.getName(rtr));
      LonAddress adr = rtr.getNeuronIdAddress();

      try {
         QueryStatusResponse status = NmUtil.queryStatus(this.lonComm, adr, 1, this.auth, false);
         if (status.versionNumber < 5) {
            this.append("version does not support table report");
            return;
         }

         int dom = rtr.getNearDeviceData().getWorkingDomain();
         this.verifyRouterTable(adr, dom, 1, 0, this.auth, false, rtr.getNearGroupTable());
         this.verifyRouterTable(adr, dom, 0, 0, this.auth, false, rtr.getNearSubnetTable());
         this.verifyRouterTable(adr, dom, 1, 0, this.auth, true, rtr.getFarGroupTable());
         this.verifyRouterTable(adr, dom, 0, 0, this.auth, true, rtr.getFarSubnetTable());
         this.verifyRouterTable(adr, dom, 1, 1, this.auth, false, rtr.getNearGroupTable());
         this.verifyRouterTable(adr, dom, 0, 1, this.auth, false, rtr.getNearSubnetTable());
         this.verifyRouterTable(adr, dom, 1, 1, this.auth, true, rtr.getFarGroupTable());
         this.verifyRouterTable(adr, dom, 0, 1, this.auth, true, rtr.getFarSubnetTable());
      } catch (LonException var5) {
         this.append("\n  Router not responding");
      }
   }

   private void verifyRouterTable(LonAddress adr, int dom, int grp, int ram, boolean auth, boolean farSide, BLonRouteTable rtrTab) throws LonException {
      byte[] dbTab = rtrTab.getByteArrayCopy();
      byte[] devTab = RouterUtil.getRouterTable(this.lonComm, adr, dom, grp, ram, auth, farSide);

      for (int i = 0; i < dbTab.length; i++) {
         if (dbTab[i] != devTab[i]) {
            this.append("\nmismatch in ");
            this.append(farSide ? "FarSide:" : "NearSide:");
            this.append(ram == 0 ? "RAM " : "EEPROM ");
            this.append(grp == 1 ? "Group Table " : "Subnet Table ");
            this.append("\ndb  :").append(LonByteArrayUtil.toString(dbTab, true));
            this.append("\nrtr :").append(LonByteArrayUtil.toString(devTab, true));
            return;
         }
      }
   }

   public void verifyDevice(BLonDevice dev) throws LonException {
      this.append("\n\nVerify " + this.getName(dev));

      try {
         BDeviceData dd = dev.getDeviceData();
         boolean isAuth = dd.getAuthenticate();
         if (isAuth != Neuron.isNMAuthSet(dev)) {
            this.append(" Authentication mismatch:");
            if (isAuth) {
               this.append("db set device not set.\n");
            } else {
               this.append("device set db not set.\n");
            }

            return;
         }

         BProgramId progId = Neuron.getProgramId(dev);
         BProgramId devId = dd.getProgramId();
         if (!progId.equals(devId)) {
            this.append(" ProgramId mismatch:\n   device is - " + progId + "\n   db is - " + devId);
            return;
         }

         StringBuilder sb = new StringBuilder();
         if (this.verifyDomainTable(dev, sb)) {
            this.append(sb.toString());
         }

         this.verifyAddressTable(dev);

         try {
            BINetworkVariable[] nvs = dev.getNetworkVariables();
            BNvConfigData deviceConfig = NmUtil.queryNvConfigData(dev, 0);
            int nvCount = this.getNvCount(dev);

            for (int nvIndex = 0; nvIndex < nvCount; nvIndex++) {
               if (nvIndex > 0) {
                  deviceConfig = NmUtil.queryNvConfigData(dev, nvIndex);
               }

               if (nvIndex < nvs.length && nvs[nvIndex] != null) {
                  BNvConfigData dbConfig = nvs[nvIndex].getNvConfigData();
                  this.verifyNvConfigReport(deviceConfig, dbConfig, false, nvIndex);
               } else if (deviceConfig.isBoundNv()) {
                  this.append("\n  nv " + nvIndex + " is bound but not represented in db - selector = " + deviceConfig.getSelector());
               }
            }

            BAliasTable aliasTab = dd.getAliasTable();
            int aliasCnt = aliasTab.getAliasCount();
            BAliasConfigData[] aa = aliasTab.getAliasArray();

            for (int n = 0; n < aliasCnt; n++) {
               BNvConfigData var17 = NmUtil.queryAliasConfigData(dev, n);
               if (n < aa.length && aa[n] != null) {
                  this.verifyNvConfigReport(var17, aa[n], true, n);
               } else if (var17.isBoundNv()) {
                  this.append("\n  alias " + n + " is bound but not represented in db - selector = " + var17.getSelector());
               }
            }
         } catch (FailedResponseException var15) {
            NmUtil.resetNode(dev);
         }
      } catch (LonException var16) {
         this.append("\n  Device not responding");
      }
   }

   private int getNvCount(BLonDevice dev) throws LonException {
      int ao = dev.getDeviceData().getAliasTable().getAliasOffset();
      if (ao > 0) {
         return ao;
      } else {
         boolean ext = NmUtil.isExtended(dev);
         return Neuron.getAliasOffset(this.lonComm, NmUtil.getSendAddress(dev), this.auth, ext);
      }
   }

   private int getNvCount(LonAddress devAddr) throws LonException {
      boolean ext = NmUtil.isExtended(this.lonComm, devAddr, this.auth);
      return Neuron.getAliasOffset(this.lonComm, devAddr, this.auth, ext);
   }

   private boolean verifyDomainTable(BLonDevice lonDevice, StringBuilder sb) throws LonException {
      sb.setLength(0);
      BDeviceData dd = lonDevice.getDeviceData();
      int wrkDmn = dd.getWorkingDomain();
      QueryDomainResponse dmnRsp = NmUtil.queryDomain(lonDevice, wrkDmn);
      BSubnetNode sn = BSubnetNode.make(dmnRsp.getSubnet(), dmnRsp.getNodeId());
      if (!sn.equals(dd.getSubnetNodeId())) {
         sb.append("\n  subnet/node mismatch : device = " + sn + " db = " + dd.getSubnetNodeId());
      }

      if (!dmnRsp.sameDomain(this.netmgmt.getDomainId())) {
         sb.append("\n  domain mismatch : device = " + LonByteArrayUtil.toString(dmnRsp.getDomainId()) + " db = " + this.netmgmt.getDomainId());
      }

      BAuthenticationKey key = BAuthenticationKey.make(dmnRsp.getKey());
      if (!key.equals(this.netmgmt.getAuthenticationKey())) {
         sb.append("\n  authentication key mismatch : device = " + key + " db = " + this.netmgmt.getAuthenticationKey());
      }

      if (dd.getTwoDomains()) {
         int id = wrkDmn == 0 ? 1 : 0;
         dmnRsp = NmUtil.queryDomain(lonDevice, id);
         if (dmnRsp.inUse()) {
            sb.append("\n  domain mismatch : domain " + id + " not \"not in use\". " + dmnRsp);
         }
      }

      return sb.length() > 0;
   }

   private void verifyNvConfigReport(BNvConfigData deviceConfig, BNvConfigData dbConfig, boolean alias, int ndx) {
      boolean isBound = deviceConfig.isBoundNv() || dbConfig.isBoundNv();
      boolean dirMm = deviceConfig.getDirection() != dbConfig.getDirection();
      boolean selMm = deviceConfig.getSelector() != dbConfig.getSelector();
      boolean adrMm = isBound && deviceConfig.getAddrIndex() != dbConfig.getAddrIndex();
      boolean authMm = isBound && deviceConfig.getAuthenticated() != dbConfig.getAuthenticated();
      boolean srvMm = isBound && deviceConfig.getServiceType() != dbConfig.getServiceType();
      boolean priMm = isBound && deviceConfig.getPriority() != dbConfig.getPriority();
      boolean trnMm = isBound && deviceConfig.getTurnAround() != dbConfig.getTurnAround();
      boolean pnvMm = alias && isBound && ((BAliasConfigData)deviceConfig).getPrimary() != ((BAliasConfigData)dbConfig).getPrimary();
      if (dirMm || selMm || adrMm || authMm || srvMm || priMm || trnMm || pnvMm) {
         if (alias) {
            this.append("\n\n alias");
         } else {
            this.append("\n\n nv");
         }

         this.append(ndx + " mismatch:");
      }

      if (dirMm) {
         this.append("\n  device direction = " + deviceConfig.getDirection() + " db direction = " + dbConfig.getDirection());
      }

      if (selMm) {
         this.append("\n  device selector = " + deviceConfig.getSelector() + " db selector = " + dbConfig.getSelector());
      }

      if (adrMm) {
         this.append("\n  device addrIndex = " + deviceConfig.getAddrIndex() + " db addrIndex = " + dbConfig.getAddrIndex());
      }

      if (authMm) {
         this.append("\n  device authenticate = " + deviceConfig.getAuthenticated() + " db authenticate = " + dbConfig.getAuthenticated());
      }

      if (srvMm) {
         this.append("\n  device service type = " + deviceConfig.getServiceType() + " db service type = " + dbConfig.getServiceType());
      }

      if (priMm) {
         this.append("\n  device priority = " + deviceConfig.getPriority() + " db priority = " + dbConfig.getPriority());
      }

      if (trnMm) {
         this.append("\n  device turn around = " + deviceConfig.getTurnAround() + " db turn around = " + dbConfig.getTurnAround());
      }

      if (pnvMm) {
         this.append(
            "\n  device primary nv = " + ((BAliasConfigData)deviceConfig).getPrimary() + " db primary nv = " + ((BAliasConfigData)dbConfig).getPrimary()
         );
      }
   }

   private void verifyAddressTable(BLonDevice lonDevice) throws LonException {
      int tabLen = lonDevice.getDeviceData().getAddressCount();
      BDeviceData dd = lonDevice.getDeviceData();
      boolean noEntry = true;

      for (int i = 0; i < tabLen; i++) {
         if (!dd.isExtended() || dd.getExtAddressTable().isEntryInUse(i)) {
            BIAddressEntry entry = dd.getAddressEntry(i);
            QueryAddrResponse qryRsp = NmUtil.getAddressTableEntry(lonDevice, i);
            this.addressError(entry, qryRsp, i);
            noEntry = false;
         }
      }

      if (dd.isExtended() && noEntry) {
         try {
            for (int ndx = 0; ndx < tabLen; ndx++) {
               QueryAddrResponse qryRsp = NmUtil.getAddressTableEntry(lonDevice, ndx);
               ndx = ((ExEnumerateAddressResponse)qryRsp).getAddressIndex();
               this.addressError(dd.getAddressEntry(ndx), qryRsp, ndx);
            }
         } catch (FailedResponseException var8) {
            return;
         }
      }
   }

   private void addressError(BIAddressEntry entry, QueryAddrResponse qryRsp, int ndx) {
      BIAddressEntry devAdr = BAddressEntry.make(
         qryRsp.getAddressType(),
         qryRsp.getSize(),
         qryRsp.getGroupOrSubnet(),
         qryRsp.getMemberOrNode(),
         entry.getDescriptor(),
         qryRsp.getDomainIndex(),
         BLonRepeatTimer.make(qryRsp.repeatTimer),
         qryRsp.retryCount,
         BLonReceiveTimer.make(qryRsp.receiveTimer),
         BLonRepeatTimer.make(qryRsp.xmitTimer)
      );
      if (!entry.equals(devAdr)) {
         this.append("\n\n address entry " + ndx + " mismatch:");
         this.append("\n   device entry = " + devAdr);

         try {
            this.append(" " + devAdr.encodeToString());
         } catch (IOException var7) {
         }

         this.append("\n   db entry = " + entry);

         try {
            this.append(" " + entry.encodeToString());
         } catch (IOException var6) {
         }
      }
   }

   public String getNetworkSummary() {
      NAddressManager am = (NAddressManager)this.lonworks.addressManager();
      this.append("Lonworks Network Summary\n");
      this.append("\nLon Routers\n");
      BLonRouter[] rtrs = am.getRouterList();
      String[] hdrs = new String[]{"Router", "Near Address", "Near Channel", "Far Address", "Far Channel", "NeuronId"};
      PTable ptab = new PTable(hdrs);

      for (int i = 0; i < rtrs.length; i++) {
         BDeviceData farDd = rtrs[i].getFarDeviceData();
         BDeviceData nearDd = rtrs[i].getNearDeviceData();
         ptab.add(this.getName(rtrs[i]));
         ptab.add(nearDd.getSubnetNodeId().toString());
         ptab.add(nearDd.getChannelId());
         ptab.add(farDd.getSubnetNodeId().toString());
         ptab.add(farDd.getChannelId());
         ptab.add(nearDd.getNeuronId().toString());
      }

      ptab.toString(this.out);
      String[] hdrsx = new String[]{"Device", "Channel", "Address", "NeuronId"};
      PTable ptabx = new PTable(hdrsx);
      BLonDevice[] d = am.getDeviceList(true);

      for (int j = 0; j < d.length; j++) {
         BDeviceData dd = d[j].getDeviceData();
         ptabx.add(this.getName(d[j]));
         ptabx.add(dd.getChannelId());
         ptabx.add(dd.getSubnetNodeId().toString());
         ptabx.add(dd.getNeuronId().toString());
      }

      ptabx.toString(this.out);
      return "";
   }

   public String getNetMgmtSummary() {
      ConnectionTable ct = new ConnectionTable(this.lonworks);
      this.append("Lonworks Network Management Summary \n");
      this.append("\nNetwork Variable Link Table\n");
      String[] hdrs = new String[]{"Sel", "Type", "Hub", "Status", "Addr", "Target", "Status", "Addr"};
      PTable ptab = new PTable(hdrs);
      Connection[] conn = ct.getConnectionArray();

      for (int i = 0; i < conn.length; i++) {
         for (Connection c = conn[i]; c != null; c = c.getSecondary()) {
            LonPoint hub = c.getHub();
            LonPoint[] tgts = c.getTargets();
            ptab.add(i);
            ptab.add(c.getLinkType() + (c.getPriority() ? "/Priority" : ""));
            if (hub == null) {
               ptab.add("", 2);
               if (tgts.length > 0) {
                  ptab.add("");
               }
            } else {
               ptab.add(hub.getSummaryString());
               ptab.add(hub.getStatus() + (hub.isPolled() ? "/Polled" : ""));
               ptab.add(hub.isLocal() ? "" : Integer.toString(hub.getAddressIndex()));
            }

            if (tgts.length > 0) {
               for (int j = 0; j < tgts.length; j++) {
                  LonPoint tgt = tgts[j];
                  ptab.add(tgt.getSummaryString());
                  ptab.add(tgt.getStatus() + (tgt.isPolled() ? "/Polled" : ""));
                  ptab.add(tgt.isLocal() ? "" : Integer.toString(tgt.getAddressIndex()));
                  ptab.newRow();
                  ptab.add("", 5);
               }
            }

            ptab.newRow();
         }
      }

      ptab.toString(this.out);
      this.append("\n");
      this.append("\nMessage Tag Link Table\n");
      String[] tagHdrs = new String[]{"AddrGrp", "Output", "AdrIndex", "Status", "Input", "Status"};
      ptab = new PTable(tagHdrs);
      TagConnection[] tags = ct.getMessageTagTable();

      for (int i = 0; i < tags.length; i++) {
         TagPoint[] ins = tags[i].getInputs();
         TagPoint out = tags[i].getOutput();
         int len = ins.length == 0 ? 1 : ins.length;

         for (int ndx = 0; ndx < len; ndx++) {
            if (ndx == 0) {
               ptab.add(tags[i].getAddressGroup());
               ptab.add(out.getSummaryString());
               ptab.add(out.getTagIndex());
               ptab.add(out.getStatus());
            } else {
               ptab.add("", 4);
            }

            if (ndx < ins.length) {
               ptab.add(ins[ndx].getSummaryString());
               ptab.add(ins[ndx].getStatus());
            } else {
               ptab.add("", 2);
            }
         }
      }

      ptab.toString(this.out);
      this.append("\n");
      BLonDevice[] devs = this.lonworks.addressManager().getDeviceList(true);
      GroupTable gt = ct.getGroupTable();
      Group[] grps = gt.getGroupArray();
      this.append("\nGroup Table\n");
      String[] grpHdr = new String[]{"Group", "Type", "Member", "Device", "Addr Ndx"};
      ptab = new PTable(grpHdr);

      for (int i = 0; i < grps.length; i++) {
         if (grps[i] != null) {
            Group grp = grps[i];
            ptab.add(i);
            ptab.add(grp.getLinkType());
            GroupMember[] members = grp.getMembers();

            for (int j = 0; j < members.length; j++) {
               GroupMember mem = members[j];
               ptab.add(j);
               ptab.add(this.getName(devs[mem.getDeviceIndex()]));
               ptab.add(mem.getAddressIndex() + (mem.isMessageTag() ? "(mtag)" : ""));
               ptab.newRow();
               ptab.add("", 2);
            }

            ptab.newRow();
         }
      }

      ptab.toString(this.out);
      return "";
   }

   private String getStatus() throws LonException {
      this.append("Device status for " + this.deviceName + "\n");
      this.getStatus(false);
      if (this.isRouter) {
         this.getRouterStatus(false);
         this.append("\n\nFar side");
         this.getStatus(true);
         this.getRouterStatus(true);
      }

      return "";
   }

   private void updateNodeState(BLonNodeState state) {
      if (this.lonDevice != null) {
         this.lonDevice.getDeviceData().set(BDeviceData.nodeState, state, AddressManager.noDeviceChange);
      }

      if (this.lonRouter != null) {
         this.lonRouter.getNearDeviceData().set(BDeviceData.nodeState, state, AddressManager.noDeviceChange);
         this.lonRouter.getFarDeviceData().set(BDeviceData.nodeState, state, AddressManager.noDeviceChange);
      }
   }

   private void getStatus(boolean farSide) throws LonException {
      QueryStatusResponse status = NmUtil.queryStatus(this.lonComm, this.deviceAddr, 1, this.auth, farSide);
      this.append("\n Transmit Errors               =  " + status.xmitErrors);
      this.append("\n Transaction Timeouts          =  " + status.transactionTimeouts);
      this.append("\n Receive Transaction Full      =  " + status.rcvTransactionFull);
      this.append("\n Lost Messages (no app buff)   =  " + status.lostMsgs);
      this.append("\n Missed Messages (no net buff) =  " + status.missedMsgs);
      this.append("\n Reset Cause                   =  ");
      int cause = status.resetCause;
      if ((cause & 1) != 0) {
         this.append("Power-up reset");
      } else if ((cause & 2) != 0) {
         this.append("External reset");
      } else if ((cause & 12) != 0) {
         this.append("Watchdog reset");
      } else if ((cause & 20) != 0) {
         this.append("Software reset");
      } else if (cause == 0) {
         this.append("Cleared");
      } else {
         this.append("0x" + Integer.toString(cause, 16));
      }

      this.append("\n Node State                    =  ");
      int state = status.nodeState;
      if ((state & 7) == 3) {
         this.append("Applicationless and unconfigured");
      } else if ((state & 7) == 2) {
         this.append("Unconfigured");
      } else if ((state & 143) == 6) {
         this.append("Configured, hard off-line");
      } else if ((state & 143) == 14) {
         this.append("Configured, hard off-line(0xe)");
      } else if ((state & 143) == 140) {
         this.append("Configured, bypass off-line");
      } else if ((state & 143) == 12) {
         this.append("Configured, soft off-line");
      } else if ((state & 143) == 4) {
         this.append("Configured, on-line");
      } else {
         this.append("0x" + Integer.toString(state, 16));
      }

      this.append("\n Version Number                =  " + status.versionNumber);
      this.append("\n Error Log                     =  " + (status.errorLog != 0 ? "ERROR: " : "") + this.getErrorText(status.errorLog));
      this.append("\n Neuron Chip model             =  ");
      if (status.modelNumber == 0) {
         this.append("3150");
      } else if (status.modelNumber == 8) {
         this.append("3120");
      } else {
         this.append("model number " + status.modelNumber);
      }

      try {
         byte[] a = Neuron.readMemory(this.lonComm, 3, this.deviceAddr, 10, 16, this.auth, farSide);
         LonInputStream stats = new LonInputStream(a);
         this.append("\n Messages received by node     =  " + stats.readUnsigned16());
         this.append("\n Messages addressed to node    =  " + stats.readUnsigned16());
         this.append("\n Messages sent to MAC layer    =  " + stats.readUnsigned16());
         this.append("\n Retries                       =  " + stats.readUnsigned16());
         this.append("\n Backlog overflows             =  " + stats.readUnsigned16());
         this.append("\n Late acks of responses        =  " + stats.readUnsigned16());
         this.append("\n Collisions detected           =  " + stats.readUnsigned16());
         this.append("\n EEPROM Lock                   =  " + this.toBool(stats.readUnsigned16() == 1));
      } catch (Throwable var7) {
         this.append("\n\n Unable to read statistics struct");
      }
   }

   private void getRouterStatus(boolean farSide) throws LonException {
      RouterStatusResponse status = RouterUtil.getRouterStatus(this.lonComm, this.deviceAddr, this.auth, farSide);
      this.append("\n\nRouter type - ").append(BLonRouterType.make(status.getType()));
      this.append(" | mode - ").append(BLonRouterMode.make(status.getMode()));
   }

   private String getErrorText(int err) {
      switch (err) {
         case 0:
            return "no_error";
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         case 19:
         case 20:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
         case 58:
         case 59:
         case 60:
         case 61:
         case 62:
         case 63:
         case 64:
         case 65:
         case 66:
         case 67:
         case 68:
         case 69:
         case 70:
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 76:
         case 77:
         case 78:
         case 79:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         case 85:
         case 86:
         case 87:
         case 88:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         case 97:
         case 98:
         case 99:
         case 100:
         case 101:
         case 102:
         case 103:
         case 104:
         case 105:
         case 106:
         case 107:
         case 108:
         case 109:
         case 110:
         case 111:
         case 112:
         case 113:
         case 114:
         case 115:
         case 116:
         case 117:
         case 118:
         case 119:
         case 120:
         case 121:
         case 122:
         case 123:
         case 124:
         case 125:
         case 126:
         case 127:
         case 128:
         default:
            return "unknown code " + Integer.toString(err);
         case 129:
            return "bad_event";
         case 130:
            return "nv_length_mismatch";
         case 131:
            return "mv_msg_too_short";
         case 132:
            return "eeprom_write_fail";
         case 133:
            return "bad_address_type";
         case 134:
            return "preemption_mode_timeout";
         case 135:
            return "already_preempted";
         case 136:
            return "sync_nv_update_lost";
         case 137:
            return "invalid_resp_alloc";
         case 138:
            return "invalid_domain";
         case 139:
            return "read_past_end_of_msg";
         case 140:
            return "write_past_end_of_msg";
         case 141:
            return "invalid_addr_table_index";
         case 142:
            return "incomplete_msg";
         case 143:
            return "nv_update_on_output_nv";
         case 144:
            return "no_msg_avail";
         case 145:
            return "illegal_send";
         case 146:
            return "unknown_pdu";
         case 147:
            return "invalid_nv_index";
         case 148:
            return "divide_by_zero";
         case 149:
            return "invalid_appl_error";
         case 150:
            return "memory_alloc_railure";
         case 151:
            return "write_past_end_of_net_buffer";
         case 152:
            return "appl_cs_error";
         case 153:
            return "cnfg_cs_error";
         case 154:
            return "invalid_xcvr_reg_addr";
         case 155:
            return "xcvr_reg_timeout";
         case 156:
            return "write_past_end_of_appl_buffer";
         case 157:
            return "io_ready";
         case 158:
            return "self_test_failed";
         case 159:
            return "subnet_router";
         case 160:
            return "authentication_mismatch";
         case 161:
            return "self_inst_semaphore_set";
         case 162:
            return "read_write_semaphore_seet";
         case 163:
            return "appl_signature_bad";
         case 164:
            return "router_firmware_version_mismatch";
      }
   }

   public String getProgramIdSummary() {
      this.append("Table of available lnml files for Lonworks devices.\n");

      try {
         String[] hdrs = new String[]{"Module", "Name", "ProgramId"};
         PTable ptab = new PTable(hdrs, 2);
         String[] keys = Sys.getRegistry().getDefs();
         Array<String> pidArray = new Array(String.class);
         Array<String> valArray = new Array(String.class);

         for (int i = 0; i < keys.length; i++) {
            if (keys[i].startsWith("lonworks")) {
               String[] vals = Sys.getRegistry().getDefs(keys[i]);

               for (int n = 0; n < vals.length; n++) {
                  pidArray.add(keys[i].substring(9));
                  valArray.add(vals[n].substring(vals[n].indexOf("=") + 1));
               }
            }
         }

         String[] pids = (String[])pidArray.trim();
         String[] vals = (String[])valArray.trim();
         SortUtil.sort(vals, pids);

         for (int ix = 0; ix < vals.length; ix++) {
            int pos = vals[ix].indexOf("/");
            if (pos < 0) {
               pos = vals[ix].indexOf(":");
            }

            if (pos >= 0) {
               ptab.add(vals[ix].substring(0, pos));
               ptab.add(vals[ix].substring(pos + 1));
               ptab.add(pids[ix]);
               ptab.newRow();
            }
         }

         ptab.toString(this.out);
      } catch (Throwable var10) {
         this.append("Error accessing londevices for available classes.\n\n");
         this.append(var10.toString());
      }

      return "";
   }

   public String getTransmitErrors(boolean clear) {
      this.append("Table of lon device transmission errors.\n");

      try {
         String[] hdrs = new String[]{"Device", "Transmit", "Timeout", "ReceiveFull", "LostMsg", "MissedMsg", "LastError"};
         PTable ptab = new PTable(hdrs);
         BLonDevice[] devs = this.lonworks.addressManager().getDeviceList(true);

         for (int i = 0; i < devs.length; i++) {
            BLonDevice dev = devs[i];
            ptab.add(this.getName(dev));

            QueryStatusResponse status;
            try {
               status = NmUtil.queryStatus(dev, 1);
               if (clear) {
                  NmUtil.clearStatus(dev);
               }
            } catch (LonException var9) {
               ptab.addBanner("Device not responding");
               continue;
            }

            ptab.add(status.xmitErrors);
            ptab.add(status.transactionTimeouts);
            ptab.add(status.rcvTransactionFull);
            ptab.add(status.lostMsgs);
            ptab.add(status.missedMsgs);
            ptab.add(this.getErrorText(status.errorLog));
            ptab.newRow();
         }

         ptab.toString(this.out);
         this.append("\n");
         this.append("Report run at " + BAbsTime.make().toString(null) + "\n");
         if (clear) {
            this.append("NOTE: Running this report performs a clearStatus on specified devices.\n\n");
         }

         this.append("Transmit    - number of CRC errors detected during packet reception.\n");
         this.append("Timeout     - number of times node failed to receive expected msg response.\n");
         this.append("ReceiveFull - number of incoming messages discarded due to no room in transaction db.\n");
         this.append("LostMsg     - number of incoming messages discarded due to no app buffers.\n");
         this.append("MissedMsg   - number of incoming messages discarded due to no net buffers.\n");
         this.append("\n");
      } catch (Throwable var10) {
         this.append("Error creating transmission error report.\n\n");
         this.append(var10.toString());
      }

      return "";
   }

   public String readMemory() {
      this.append("Read memory:\n");
      int memAdr = this.cmd.getMemAddr();
      int count = this.cmd.getCount();
      int bytesRead = 0;

      try {
         while (bytesRead < count) {
            int lenToRead = count - bytesRead;
            int startAddress = memAdr + bytesRead;
            if (lenToRead > 16) {
               lenToRead = 16;
            }

            byte[] data = Neuron.readMemory(this.lonComm, 0, this.deviceAddr, startAddress, lenToRead, this.auth);
            bytesRead += data.length;
            this.append(this.toHex(startAddress >> 8 & 0xFF) + this.toHex(startAddress & 0xFF) + "   ");

            for (int i = 0; i < 16; i++) {
               this.append((i < data.length ? this.toHex(data[i]) : "  ") + " ");
            }

            this.append("  ");
            this.getString(data, 0, data.length);
            this.append("\n");
         }
      } catch (Throwable var8) {
         this.append("\nERROR: reading memory");
      }

      return "";
   }

   private String disableAuthentication() {
      try {
         Neuron.setNMAuthenticate(this.lonComm, this.deviceAddr, false);
      } catch (Throwable var2) {
         return "Unable to disable authentication on " + this.deviceName + ".";
      }

      if (this.lonDevice != null) {
         this.lonDevice.getDeviceData().setAuthenticate(false);
      }

      return "Disabled authentication on " + this.deviceName + ".";
   }

   private String getSelfDoc() {
      if (this.deviceAddr == BLocal.local) {
         return "Cannot access selfDocumentation on local device.";
      } else {
         this.append("Self Documentation for " + this.deviceName + ":\n");

         try {
            SelfDoc selfDoc = new SelfDoc(this.lonComm, this.deviceAddr, this.auth, this.isExtended());
            this.append("\nNode Self Documentation:\n");
            this.append(selfDoc.nodeDocString);
            this.append("\n");
            if (selfDoc.getBindingII()) {
               this.append("\nuses bindingII constraints");
            }

            if (selfDoc.getQueryStats()) {
               this.append("\nhas extended statistics");
            }

            this.append("\naliasCnt = ").append(selfDoc.getAliasCnt());
            this.append("\nmtagCnt = ").append(selfDoc.getMsgtagCount());
            this.append("\n\nSnvt self documentation:\n");
            String[] hdrs = new String[]{"Index", "Name", "Snvt", "Descriptor", "Array", "String"};
            PTable ptab = new PTable(hdrs);
            ptab.setColumnAttribute(1, 2);
            ptab.setColumnAttribute(2, 2);
            ptab.setColumnAttribute(3, 2);
            ptab.setColumnAttribute(5, 2);
            NvDoc[] nvs = selfDoc.nvs;

            for (int i = 0; i < nvs.length; i++) {
               NvDoc nv = nvs[i];
               ptab.add(i);
               ptab.add(nv.getName());
               ptab.add(BLonSnvtType.make(nv.getSnvtType()) + "{" + nv.getSnvtType() + "}");
               ptab.add(this.getDescFlags(nv));
               ptab.add(nv.getArraySize());
               if (nv.selfDoc != null) {
                  ptab.add(this.fixString(nv.selfDoc.selfDoc));
               } else {
                  ptab.newRow();
               }
            }

            ptab.toString(this.out);
            if (selfDoc.doc == null) {
               return "";
            }

            this.append("\nRaw data:\n");
            int len = selfDoc.doc.length;
            int offset = 0;
            int numNvDocs = selfDoc.getSnvtStruct().numNvsDocs;
            byte[] doc = selfDoc.doc;
            this.append("Snvt struct:\n");
            SnvtStruct ss = selfDoc.getSnvtStruct();
            this.append(LonByteArrayUtil.toString(ss.data, 16, ' ', selfDoc.hdrLength));
            this.append("\n\nDescriptor structs:\n");

            for (int ix = 0; ix < numNvDocs; ix++) {
               offset += this.getBytes(doc, offset, 2);
               this.append("\n");
            }

            this.append("\nNode self doc:\n");
            offset += this.getStringBytes(doc, offset);
            this.append("\n\nNv self doc:\n");

            for (int ix = 0; ix < numNvDocs; ix++) {
               int mask = selfDoc.doc[offset++];
               this.append(this.toHex(mask)).append(' ');
               if ((mask & 128) != 0) {
                  offset += this.getBytes(doc, offset, 1);
               }

               if ((mask & 64) != 0) {
                  offset += this.getBytes(doc, offset, 1);
               }

               if ((mask & 32) != 0) {
                  offset += this.getStringBytes(doc, offset);
               }

               if ((mask & 16) != 0) {
                  offset += this.getStringBytes(doc, offset);
               }

               if ((mask & 8) != 0) {
                  offset += this.getBytes(doc, offset, 2);
               }

               this.append('\n');
            }

            if (offset < len) {
               this.append("\nAlias field descriptions:\n");
               this.getBytes(doc, offset, len - offset);
            }
         } catch (Throwable var12) {
            var12.printStackTrace();
            this.append("\n\nUnable to read self documentation.");
         }

         return "";
      }
   }

   private String getDescFlags(NvDoc nv) {
      StringBuilder sb = new StringBuilder();
      if (nv.hasExtension()) {
         sb.append("ext,");
      }

      if (nv.isSychronous()) {
         sb.append("sync,");
      }

      if (nv.isPolled()) {
         sb.append("polled,");
      }

      if (nv.isUpdateOffline()) {
         sb.append("updOfLn,");
      }

      if (nv.isServTypeConfig()) {
         sb.append("svrCnf,");
      }

      if (nv.isPriorityConfig()) {
         sb.append("priCnf,");
      }

      if (nv.isAuthConfigurable()) {
         sb.append("authCnf,");
      }

      if (nv.isConfigNv()) {
         sb.append("cnfgNv");
      }

      String s = sb.toString();
      return s.substring(0, s.length() - 1);
   }

   private String fixString(String st) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < st.length(); i++) {
         char c = st.charAt(i);
         if (c <= ' ') {
            sb.append('\\').append(this.toHex(c + '@'));
         } else if (c >= 127) {
            sb.append("\\x").append(this.toHex(c));
         } else {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   private int getBytes(byte[] a, int offset, int len) {
      this.append(LonByteArrayUtil.toString(a, 16, ' ', len, offset));
      return len;
   }

   private int getStringBytes(byte[] a, int offset) {
      char c;
      int len;
      for (len = 0; (c = (char)a[offset + len]) != 0; len++) {
         this.append(this.toHex(c)).append(' ');
      }

      this.append(this.toHex(c)).append(' ');
      return len + 1;
   }

   private void bytesToString(byte[] a, int offset, int len) {
      int cnt = 0;

      while (cnt < len && offset + cnt < a.length) {
         byte b = a[offset + cnt++];
         if (b >= 32 && b <= 126) {
            this.append((char)b);
         } else {
            this.append('.');
         }
      }
   }

   private String test() throws LonException {
      return "Test complete.";
   }

   private LonUtilRequest append(String s) {
      this.out.print(s);
      this.out.flush();
      return this;
   }

   private LonUtilRequest append(int i) {
      this.out.print(i);
      return this;
   }

   private LonUtilRequest append(char c) {
      this.out.print(c);
      return this;
   }

   private LonUtilRequest append(Object o) {
      return this.append(o.toString());
   }

   private boolean isExtended() {
      if (this.checkExtended) {
         this.isExtended = NmUtil.isExtended(this.lonComm, this.deviceAddr, this.auth);
         this.checkExtended = false;
      }

      return this.isExtended;
   }

   private String toHex(int b) {
      b &= 255;
      return (b > 15 ? "" : "0") + Integer.toString(b, 16);
   }

   private String toBool(int b, int bit) {
      return (b >>> bit & 1) == 1 ? "Yes" : "No";
   }

   private String toBool(boolean b) {
      return b ? "Yes" : "No";
   }
}
