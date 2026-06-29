package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BLearnParameter;
import com.tridium.lonworks.enums.BLonLinkStatus;
import com.tridium.lonworks.netmessages.ExEnumerateAddressResponse;
import com.tridium.lonworks.netmessages.ExEnumerateAliasResponse;
import com.tridium.lonworks.netmessages.QueryAddrResponse;
import com.tridium.lonworks.netmessages.QueryAliasResponse;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonLink;
import javax.baja.lonworks.BMessageTag;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BLink;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonLearnLinksJob extends BLonNetmgmtJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonLearnLinksJob.class);
   private static final BIcon icon = BIcon.make(BIcon.std("apple.png"), BIcon.std("badges/clock.png"));
   private LonComm loncomm;
   private boolean debug;
   private boolean uploadDevData;
   private BLonDevice[] devList;
   private ConnectionTable connTable;
   private IntHashMap linkType = new IntHashMap();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonLearnLinksJob() {
   }

   public BLonLearnLinksJob(BLonNetmgmt netMgmt, BLearnParameter param) {
      super(netMgmt);
      this.loncomm = this.lon.lonComm();
      this.debug = netMgmt.getDebug();
      this.uploadDevData = true;
      this.devList = param.getDeviceList(this.lon);
   }

   public BLonLearnLinksJob(BLonNetmgmt netMgmt, BLonDevice[] devList, boolean uploadDevData) {
      super(netMgmt);
      this.devList = devList;
      this.loncomm = this.lon.lonComm();
      this.debug = netMgmt.getDebug();
      this.uploadDevData = uploadDevData;
   }

   @Override
   public void run() {
      try {
         this.doLearnLinks();
      } catch (JobCancelException var2) {
         this.canceled();
      } catch (Throwable var3) {
         this.fatal("LearnLinks failed ", var3);
      }

      this.end();
   }

   public void doLearnLinks() {
      this.log().start("Learn links");
      if (this.devList.length != 0) {
         int len = this.devList.length;
         this.percentFactor = len / 0.8;

         for (int i = 0; i < len; i++) {
            this.percentOffset = (int)(100.0 / this.percentFactor) * i;
            if (!this.upload(this.devList[i])) {
               this.devList[i] = null;
            }
         }

         this.connTable = new ConnectionTable(this.lon, true, false);
         this.percentOffset = 80;
         this.percentFactor = len / 0.2;

         for (int ix = 0; ix < len; ix++) {
            if (this.devList[ix] != null) {
               this.processLinks(this.devList[ix]);
               this.processMessageTags(this.devList[ix]);
               this.myProgress(ix / len);
            }
         }
      }
   }

   private boolean upload(BLonDevice lonDevice) {
      if (lonDevice.isLocal()) {
         return false;
      } else {
         this.log().start("Upload device data for " + lonDevice.getDisplayName(null));
         BDeviceData dd = lonDevice.getDeviceData();

         try {
            if (this.uploadDevData) {
               BProgramId devPid = Neuron.getProgramId(lonDevice);
               BProgramId dbPid = dd.getProgramId();
               if (!devPid.equals(dbPid)) {
                  this.fatal("Program id does not match");
                  return false;
               }

               this.myProgress(2);
               if (!NmUtil.verifyDomainTable(lonDevice, this.netMgmt)) {
                  this.fatal("Domain table error for " + lonDevice.getDisplayName(null));
                  return false;
               }

               dd.setProgramId(Neuron.getProgramId(lonDevice));
               dd.set(BDeviceData.nodeState, NmUtil.getDeviceState(lonDevice), AddressManager.noDeviceChange);
               dd.setChannelId(Neuron.getChannelId(lonDevice));
               Neuron.uploadDeviceData(lonDevice);
               this.myProgress(5);
            }

            this.readNvConfig(lonDevice);
            this.myProgress(85);
            this.readAliasTable(lonDevice);
            this.readAddressTable(lonDevice);
            this.myProgress(95);
            dd.setLocation(Neuron.getLocation(lonDevice));
            dd.setPrioritySlot(Neuron.getNodePriority(lonDevice));
         } catch (Throwable var5) {
            this.fatal("Upload device data failed for " + lonDevice.getDisplayName(null) + "\n" + var5.toString());
            var5.printStackTrace();
            return false;
         }

         this.pass("Success");
         return true;
      }
   }

   private void readNvConfig(BLonDevice lonDevice) throws LonException {
      this.linkType.clear();
      BDeviceData dd = lonDevice.getDeviceData();
      BINetworkVariable[] nvs = lonDevice.getNetworkVariables();

      for (int nvIndex = 0; nvIndex < nvs.length; nvIndex++) {
         if (nvIndex == nvs.length / 2) {
            this.myProgress(45);
         }

         if (nvs[nvIndex] != null && !nvs[nvIndex].isNetworkConfig()) {
            BNvConfigData deviceConfig = NmUtil.queryNvConfigData(lonDevice, nvIndex);
            BNetworkVariable nv = (BNetworkVariable)nvs[nvIndex];
            BNvConfigData dbConfig = nv.getNvConfigData();
            if (deviceConfig.getDirection() != dbConfig.getDirection()) {
               String err = "BINetworkVariable direction mismatch for " + nv.getDisplayName(null) + "(" + nvIndex + ")";
               if (this.netMgmt.getVerifyNvDir()) {
                  throw new LonException(err);
               }

               this.warning(err);
            }

            dbConfig.copyFrom(deviceConfig);
            int index = deviceConfig.getAddrIndex();
            if (index < dd.getAddressCount() && index >= 0 && dbConfig.isBoundNv() && this.requiresAddressEntry(nv)) {
               this.linkType.put(index, NmUtil.getLinkType(deviceConfig));
            }
         }
      }
   }

   private boolean requiresAddressEntry(BNetworkVariable nv) {
      BLonNvDirection dir = nv.getNvConfigData().getDirection();
      return nv.getNvProps().getPolled() ? dir == BLonNvDirection.input : dir == BLonNvDirection.output;
   }

   private void readAliasTable(BLonDevice lonDevice) throws LonException {
      BDeviceData dd = lonDevice.getDeviceData();
      BAliasTable aTab = dd.getAliasTable();
      int aliasCnt = aTab.getAliasCount();
      if (aliasCnt != 0) {
         int aliasOffset = aTab.getAliasOffset();
         LonAddress deviceAddr = NmUtil.getSendAddress(lonDevice);
         boolean auth = lonDevice.authenticate();

         for (int i = 0; i < aliasCnt; i++) {
            QueryAliasResponse aliasRsp;
            try {
               aliasRsp = NmUtil.queryAliasConfigData(this.loncomm, deviceAddr, i, aliasOffset, auth, lonDevice.isExtended());
               if (aliasRsp.isExtended()) {
                  i = ((ExEnumerateAliasResponse)aliasRsp).getAliasIndex();
               }
            } catch (FailedResponseException var12) {
               break;
            }

            BAliasConfigData aCnfigData = aliasRsp.getAliasConfigData();
            aTab.setAliasEntry(i, aCnfigData);
            int index = aCnfigData.getAddrIndex();
            if (index < dd.getAddressCount() && index >= 0 && aCnfigData.isBoundNv()) {
               this.linkType.put(index, NmUtil.getLinkType(aCnfigData));
            }
         }
      }
   }

   private void readAddressTable(BLonDevice lonDevice) throws LonException {
      BDeviceData dd = lonDevice.getDeviceData();
      int tabLen = dd.getAddressCount();
      int wrkDmn = dd.getWorkingDomain();
      LonAddress addr = NmUtil.getSendAddress(lonDevice);
      boolean wrkDomainMismatch = false;
      boolean ext = lonDevice.isExtended();

      for (int i = 0; i < tabLen; i++) {
         QueryAddrResponse qryRsp;
         try {
            qryRsp = NmUtil.getAddressTableEntry(this.loncomm, addr, i, lonDevice.authenticate(), ext);
            if (ext) {
               i = ((ExEnumerateAddressResponse)qryRsp).getAddressIndex();
            }
         } catch (FailedResponseException var11) {
            break;
         }

         if (qryRsp.getAddressType() == BAddressType.none) {
            dd.setAddressEntry(i, BAddressEntry.DEFAULT);
         } else {
            if (wrkDmn != qryRsp.getDomainIndex()) {
               wrkDomainMismatch = true;
            }

            int descriptor = NmUtil.linkTypeToDescriptor((BLonLinkType)this.linkType.get(i));
            dd.setAddressEntry(
               i,
               BAddressEntry.make(
                  qryRsp.getAddressType(),
                  qryRsp.getSize(),
                  qryRsp.getGroupOrSubnet(),
                  qryRsp.getMemberOrNode(),
                  descriptor,
                  qryRsp.getDomainIndex(),
                  BLonRepeatTimer.make(qryRsp.repeatTimer),
                  qryRsp.retryCount,
                  BLonReceiveTimer.make(qryRsp.receiveTimer),
                  BLonRepeatTimer.make(qryRsp.xmitTimer)
               )
            );
         }
      }

      if (wrkDomainMismatch) {
         this.warning("Mismatch between working domain and address table domain index in " + lonDevice.getDisplayName(null));
      }
   }

   private void processLinks(BLonDevice lonDevice) {
      BINvContainer[] nvcs = lonDevice.getNvContainers();

      for (int i = 0; i < nvcs.length; i++) {
         this.doProcessLinks(nvcs[i]);
      }
   }

   private void doProcessLinks(BINvContainer nvCntr) {
      BComponent nvcCmp = nvCntr.asComponent();
      String msg = "Learn links for " + nvcCmp.getDisplayName(null);
      this.log().message(msg);

      try {
         BLink[] origLinks = nvcCmp.getLinks();

         for (int i = 0; i < origLinks.length; i++) {
            if (!(origLinks[i] instanceof BLonLink)) {
               origLinks[i] = null;
            }
         }

         Connection[] table = this.connTable.getConnectionArray();

         for (int sel = 0; sel < table.length; sel++) {
            for (Connection cnctn = table[sel]; cnctn != null; cnctn = cnctn.getSecondary()) {
               LonPoint hub = cnctn.getHub();
               if (hub != null) {
                  boolean imaHub = hub.isSameNode(nvCntr);
                  LonPoint[] tgts = cnctn.getTargets();
                  BLonLinkType lnkTyp = this.getLinkType(cnctn);

                  for (int ix = 0; ix < tgts.length; ix++) {
                     LonPoint pnt = tgts[ix];
                     if (imaHub && !pnt.isProxy() || pnt.isSameNode(nvCntr) && !pnt.isOutput()) {
                        if (this.debug) {
                           System.out.println("process link from : " + hub + "\n   to:" + pnt);
                        }

                        boolean priority = pnt.requiresAddressEntry() ? pnt.isPriority() : hub.isPriority();
                        BOrd sourceOrd = hub.getNvContainer().asComponent().getOrdInSession();
                        Slot sourceSlot = hub.getNetworkVariable().getPropertyInParent();
                        Slot targetSlot = pnt.getNetworkVariable().getPropertyInParent();
                        BComponent tgtCmp = imaHub ? pnt.getNvContainer().asComponent() : nvcCmp;
                        if (pnt.isObsolete()) {
                           BLonLink newLink = new BLonLink(hub.getNvContainer().asComponent().getHandleOrd(), sourceSlot.getName(), targetSlot.getName(), true);
                           newLink.setLinkType(lnkTyp);
                           newLink.setPriority(priority);
                           tgtCmp.add(null, newLink);
                           newLink.activate();
                           if (this.debug) {
                              System.out.println(" Add new link: " + newLink + "\n");
                           }

                           pnt.setStatus(BLonLinkStatus.bound);
                        } else {
                           boolean matchFound = false;
                           BLonLink lnk = null;

                           for (int n = 0; n < origLinks.length; n++) {
                              if (origLinks[n] != null) {
                                 lnk = (BLonLink)origLinks[n];
                                 if (lnk.getSourceOrd().equals(sourceOrd)
                                    && lnk.getSourceSlotName().equals(sourceSlot.getName())
                                    && lnk.getTargetSlotName().equals(targetSlot.getName())) {
                                    origLinks[n] = null;
                                    matchFound = true;
                                    break;
                                 }
                              }
                           }

                           if (matchFound) {
                              if (pnt.isNew()) {
                                 if (this.debug) {
                                    System.out.println(" Remove link: " + lnk);
                                 }

                                 lnk.deactivate();
                                 tgtCmp.remove(lnk.getPropertyInParent());
                              } else {
                                 lnk.setLinkType(lnkTyp);
                                 lnk.setPriority(priority);
                              }

                              if (this.debug) {
                                 System.out.println();
                              }
                           }
                        }
                     } else if (pnt.isProxy() && hub.isBound()) {
                        boolean bndToLocal = this.isAddressedToLocal(hub.getAddressEntry());
                        BNetworkVariable nv = hub.getNetworkVariable();
                        BNvProps nvProps = nv.getNvProps();
                        if (nvProps.getBoundToLocal() != bndToLocal) {
                           if (this.debug) {
                              System.out.println(" Set boundToLocal to " + bndToLocal);
                           }

                           nvProps.setBoundToLocal(bndToLocal);
                           nv.bound();
                        }
                     }
                  }
               }
            }
         }
      } catch (Throwable var22) {
         if (var22 instanceof JobCancelException) {
            throw (JobCancelException)var22;
         }

         this.fatal("Learn links failed for " + nvcCmp.getDisplayName(null), var22);
      }
   }

   private boolean isAddressedToLocal(BIAddressEntry adrEntry) {
      if (adrEntry == null) {
         return false;
      } else {
         BLocalLonDevice ld = this.loncomm.lonNetwork().getLocalLonDevice();
         if (adrEntry.isSubnetNodeAddress()) {
            return adrEntry.getSubnetNodeAddress().equals(ld.getSubnetNodeAddress());
         } else {
            if (adrEntry.isGroupAddress()) {
               int grp = adrEntry.getGroupOrSubnet();
               BDeviceData dd = ld.getDeviceData();
               int cnt = dd.getAddressCount();

               for (int i = 0; i < cnt; i++) {
                  BIAddressEntry locAdrEntry = dd.getAddressEntry(i);
                  if (locAdrEntry.isGroupAddress() && locAdrEntry.getGroupOrSubnet() == grp) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   private void processMessageTags(BLonDevice lonDevice) {
      BDeviceData dd = lonDevice.getDeviceData();
      int tagCnt = dd.getMsgTagCount();
      if (tagCnt != 0) {
         for (int i = 0; i < tagCnt; i++) {
            BIAddressEntry e = dd.getAddressEntry(i);
            BMessageTag myTag = lonDevice.getMessageTag(i);
            if (myTag != null) {
               Property myTagProp = myTag.getPropertyInParent();
               switch (e.getAddressType().getOrdinal()) {
                  case 0:
                     this.deleteLinks(lonDevice.getKnobs(myTagProp));
                     BLink[] la = lonDevice.getLinks(myTagProp);

                     for (int x = 0; x < la.length; x++) {
                        this.deleteLink(la[x]);
                     }
                     break;
                  case 1:
                     int grpNum = e.getGroupOrSubnet();
                     Group grp = this.connTable.getGroupTable().getGroup(grpNum);
                     GroupMember[] ms = grp.getMembers();
                     int srcMemNdx = -1;
                     int myMem = -1;
                     int n = 0;

                     for (; n < ms.length; n++) {
                        GroupMember mem = ms[n];
                        BLonDevice dev = mem.dev = this.connTable.getDevice(mem.getDeviceIndex());
                        mem.mtag = mem.isMessageTag() ? dev.getMessageTag(mem.getAddressIndex()) : dev.getMessageIn();
                        mem.prop = mem.mtag.getPropertyInParent();
                        if (mem.dev == lonDevice) {
                           myMem = n;
                        }

                        Knob[] ka = mem.dev.getKnobs(mem.prop);
                        if (ka.length > 0) {
                           if (srcMemNdx == -1) {
                              srcMemNdx = n;

                              for (int k = 0; k < ka.length; k++) {
                                 Knob kb = ka[k];
                                 if (!this.linkTgtInGroup(kb, grp)) {
                                    this.deleteLink(kb.getLink());
                                 }
                              }
                           } else {
                              this.deleteLinks(ka);
                           }
                        }
                     }

                     if (myMem == -1) {
                        throw new BajaRuntimeException("Internal error : not member of my group.");
                     }

                     if (srcMemNdx == -1) {
                        srcMemNdx = myMem;
                     }

                     GroupMember srcMem = ms[srcMemNdx];

                     for (int nx = 0; nx < ms.length; nx++) {
                        if (nx != srcMemNdx) {
                           GroupMember memx = ms[nx];
                           BLink[] la = memx.dev.getLinks(memx.prop);
                           if (la.length <= 0) {
                              BLonLink newLink = new BLonLink(srcMem.dev.getHandleOrd(), srcMem.prop.getName(), memx.prop.getName(), true);
                              newLink.setMessageTag(true);
                              memx.dev.add(null, newLink);
                              newLink.activate();
                              if (this.debug) {
                                 System.out.println(" Add new link: " + newLink + "\n");
                              }
                           }
                        }
                     }
                     break;
                  case 2:
                     BSubnetNode addr = e.getSubnetNodeAddress();
                     BLonDevice tgtDev = this.lon.addressManager().getDeviceByAddress(addr);
                     Property tgtMtProp = tgtDev.getMessageIn().getPropertyInParent();
                     Knob[] ka = lonDevice.getKnobs(myTagProp);
                     boolean found = false;
                     if (ka.length > 0) {
                        Knob kx = ka[i];
                        if (kx.getTargetComponent() == tgtDev && kx.getTargetSlot() == tgtMtProp) {
                           found = true;
                        } else {
                           this.deleteLink(kx.getLink());
                        }
                     }

                     if (found) {
                        return;
                     }

                     BLonLink newLink = new BLonLink(lonDevice.getHandleOrd(), myTagProp.getName(), tgtMtProp.getName(), true);
                     newLink.setMessageTag(true);
                     tgtDev.add(null, newLink);
                     newLink.activate();
                     if (this.debug) {
                        System.out.println(" Add new link: " + newLink + "\n");
                     }
               }
            }
         }
      }
   }

   private void deleteLinks(Knob[] ka) {
      for (int k = 0; k < ka.length; k++) {
         this.deleteLink(ka[k].getLink());
      }
   }

   private void deleteLink(BLink lnk) {
      if (this.debug) {
         System.out.println(" Remove link: " + lnk);
      }

      lnk.deactivate();
      lnk.getTargetComponent().remove(lnk.getPropertyInParent());
   }

   private boolean linkTgtInGroup(Knob kb, Group grp) {
      BLonDevice tgtDev = (BLonDevice)kb.getTargetComponent();
      Slot ts = kb.getTargetSlot();
      if (!ts.asProperty().getType().is(BMessageTag.TYPE)) {
         return false;
      } else {
         BMessageTag mt = (BMessageTag)tgtDev.get(ts.asProperty());
         int ndx = this.connTable.getGroupTable().findDeviceIndex(tgtDev);
         GroupMember gm = grp.findMember(ndx);
         if (gm == null) {
            return false;
         } else {
            return mt.getIndex() >= 0 ? mt.getIndex() == gm.getAddressIndex() : gm.getAddressIndex() >= tgtDev.getDeviceData().getMsgTagCount();
         }
      }
   }

   private BLonLinkType getLinkType(Connection cnctn) {
      LonPoint pnt = cnctn.getAddressPnt(false);
      if (pnt == null) {
         return BLonLinkType.standard;
      } else {
         BIAddressEntry entry = pnt.getAddressEntry();
         return entry == null ? BLonLinkType.standard : NmUtil.getLinkType(entry.getDescriptor());
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
