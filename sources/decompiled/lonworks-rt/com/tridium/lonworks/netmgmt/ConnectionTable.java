package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BLinkEntry;
import com.tridium.lonworks.datatypes.BLinkEntryTable;
import com.tridium.lonworks.datatypes.BTagLinkEntry;
import com.tridium.lonworks.datatypes.BTagLinkEntryTable;
import com.tridium.lonworks.enums.BLonLinkStatus;
import com.tridium.lonworks.local.BPseudoNV;
import com.tridium.lonworks.util.NmUtil;
import java.util.HashMap;
import java.util.Vector;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonLink;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BMessageTag;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.proxy.BLonProxyExt;
import javax.baja.sys.BComponent;
import javax.baja.sys.BLink;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Knob;

public class ConnectionTable implements NetMgmtConst {
   private Vector<ConnectionTable.UnresolvedLink> unresolved = new Vector<>();
   public static int INITIAL_CONNECTIONS = 200;
   private BLonDevice[] lonDevices;
   private BLocalLonDevice local;
   BLonNetwork lonworks;
   private boolean debug;
   private Connection[] table = new Connection[INITIAL_CONNECTIONS];
   private HashMap<Integer, Integer> pointHash = new HashMap<>(INITIAL_CONNECTIONS * 2);
   private Vector<TagConnection> tagTable = new Vector<>(10);
   private HashMap<Integer, TagConnection> tagHash = new HashMap<>(32);
   private HashMap<Integer, LonPointProxy> proxyHash = new HashMap<>(500);
   private GroupTable groupTable;

   public ConnectionTable(BLonNetwork lonworks) {
      this(lonworks, false, lonworks.getLonNetmgmt().getDebug());
   }

   public ConnectionTable(BLonNetwork lonworks, boolean uploadLearn, boolean debug) {
      this.lonworks = lonworks;
      AddressManager adMan = lonworks.addressManager();
      this.lonDevices = adMan.getDeviceList(true);
      this.local = lonworks.getLocalLonDevice();
      this.debug = debug;
      if (debug) {
         System.out.println("Create connection table.");
      }

      for (int ndx = 0; ndx < this.lonDevices.length; ndx++) {
         BLonDevice dev = this.lonDevices[ndx];
         if (dev != null) {
            BINvContainer[] nvCntrs = dev.getNvContainers();

            for (int i = 0; i < nvCntrs.length; i++) {
               this.addBoundNv(nvCntrs[i]);
            }

            this.addAliasNv(dev);
            this.addMessageTags(dev);
         }
      }

      if (debug) {
         System.out.println(this.toString());
      }

      if (debug) {
         System.out.println("\nadd proxy and pseudo points\n");
      }

      for (int ndxx = 0; ndxx < this.lonDevices.length; ndxx++) {
         BLonDevice dev = this.lonDevices[ndxx];
         if (dev != null) {
            this.addProxyLinks(dev);
         }
      }

      if (debug) {
         System.out.println(this.toString());
      }

      BPseudoNV[] pseudos = (BPseudoNV[])NmUtil.getDecendantsByClass(lonworks, BPseudoNV.class);

      for (int ndxxx = 0; ndxxx < pseudos.length; ndxxx++) {
         this.addPseudoNv(pseudos[ndxxx]);
      }

      if (debug) {
         System.out.println(this.toString());
      }

      GroupTable grpTbl = this.getGroupTable();
      if (debug) {
         System.out.println("\nparse overlapping targets\n");
      }

      for (int i = 0; i < this.table.length; i++) {
         if (this.table[i] != null) {
            this.table[i].parseTargets(grpTbl);
         }
      }

      if (debug) {
         System.out.println(this.toString());
      }

      for (int ndxxx = 0; ndxxx < this.lonDevices.length; ndxxx++) {
         BLonDevice dev = this.lonDevices[ndxxx];
         if (dev != null) {
            BINvContainer[] nvCntrs = dev.getNvContainers();

            for (int ix = 0; ix < nvCntrs.length; ix++) {
               this.addLinks(nvCntrs[ix]);
            }
         }
      }

      if (debug) {
         System.out.println(this.toString());
      }

      this.processUnresolved();
      if (debug) {
         System.out.println(this.toString());
      }

      if (!uploadLearn) {
         this.reviewOverlapps();
      }

      if (debug) {
         System.out.println(this.toString());
      }

      if (!uploadLearn) {
         this.processAliasConnections();
      }

      if (debug) {
         System.out.println(this.toString());
      }

      this.setMessageTagStatus();
      if (debug) {
         System.out.println(this.toString());
      }

      this.verifyConnection();
      if (debug) {
         System.out.println(this.toString());
      }
   }

   public Connection[] getConnectionArray() {
      return this.table;
   }

   public GroupTable getGroupTable() {
      if (this.groupTable == null) {
         this.groupTable = new GroupTable(this.lonworks, this.lonDevices, this, this.debug);
      }

      return this.groupTable;
   }

   public TagConnection[] getMessageTagTable() {
      TagConnection[] tags = new TagConnection[this.tagTable.size()];
      this.tagTable.copyInto(tags);
      return tags;
   }

   public Connection getConnection(int selector) {
      return this.table.length <= selector ? null : this.table[selector];
   }

   private void addBoundNv(BINvContainer nvCntr) {
      if (this.debug) {
         System.out.println("\nAnalyze nvProps for " + nvCntr.getDisplayName(null));
      }

      BINetworkVariable[] a = (BINetworkVariable[])((BComponent)nvCntr).getChildren(BINetworkVariable.class);

      for (int i = 0; i < a.length; i++) {
         if (a[i] != null && a[i].isNetworkVariable()) {
            BNetworkVariable nv = (BNetworkVariable)a[i];
            BNvConfigData nvCnfg = nv.getNvConfigData();
            if (nvCnfg.isBoundNv()) {
               LonPoint point = new LonPoint(nvCntr, nv, BLonLinkStatus.obsolete);
               point.setAddressIndex(nvCnfg.getAddrIndex());
               if (this.debug) {
                  System.out.println("\n found bound point " + point);
               }

               int selector = nvCnfg.getSelector();
               this.addPoint(point, selector);
            }
         }
      }
   }

   private void addProxyLinks(BLonDevice device) {
      if (this.debug) {
         System.out.println("\nAnalyze proxyPoints for " + device.getDisplayName(null));
      }

      BControlPoint[] pxs = device.getLonProxies();

      for (int i = 0; i < pxs.length; i++) {
         BControlPoint cp = pxs[i];
         BAbstractProxyExt ext = cp.getProxyExt();
         if (ext != null && ext instanceof BLonProxyExt) {
            BLonProxyExt lext = (BLonProxyExt)ext;
            BLonComponent lc = null;

            try {
               lc = lext.getLonComponent();
            } catch (Throwable var18) {
            }

            if (lc != null && lc.isNetworkVariable()) {
               BNetworkVariable nv = (BNetworkVariable)lc;
               BNvConfigData nvCnfg = nv.getNvConfigData();
               BNvProps nvProps = nv.getNvProps();
               if (!nvCnfg.isInput() || cp.isWritablePoint()) {
                  BLonLinkStatus status = BLonLinkStatus.newLink;
                  if (nvCnfg.isBoundNv() && (nvCnfg.isInput() || nvProps.getBoundToLocal())) {
                     status = BLonLinkStatus.bound;
                  }

                  LonPoint lp = new LonPoint(device, nv, status);
                  LonPointProxy lpc = new LonPointProxy(device, nv, status, cp, this.local, (BLonProxyExt)ext);
                  int sel = this.getSelector(lp);
                  BLonLinkType linkType = lext.getLinkType();
                  boolean priority = lext.getPriority();
                  boolean isSecond = this.isSecondProxy(lpc);
                  if (sel < 0) {
                     if (this.debug) {
                        System.out.println(" addProxyLink sel < 0");
                     }

                     if (nvCnfg.isInput()) {
                        this.addConnection(lpc, lp, linkType, priority);
                     } else {
                        this.addConnection(lp, lpc, linkType, priority);
                     }
                  } else if (!isSecond) {
                     if (this.debug) {
                        System.out.println(" addProxyLink to sel = " + sel + "\n" + lpc);
                     }

                     this.table[sel].addLinkedProxy(lp, lpc, linkType, priority);
                     this.hash(lpc, sel);
                  }
               }
            }
         }
      }
   }

   private void addLinks(BINvContainer nvCntr) {
      BLink[] lnks = nvCntr.asComponent().getLinks();
      if (this.debug) {
         System.out.println("\nAnalyze " + lnks.length + " links for " + nvCntr.getDisplayName(null));
      }

      boolean onLine = nvCntr.getLonDevice().isConfigOnline();

      for (int ndx = 0; ndx < lnks.length; ndx++) {
         if (lnks[ndx].getType().is(BLonLink.TYPE)) {
            BLonLink link = (BLonLink)lnks[ndx];
            if (link.getMessageTag()) {
               this.addTagLink(nvCntr.getLonDevice(), link, !onLine);
            } else {
               try {
                  if (link.getRemoteLink()) {
                     this.addRemoteLinkToSrc(nvCntr, link, !onLine);
                  } else if (link.getPseudoLink()) {
                     this.addPseudoLinkToSrc(nvCntr, link, !onLine);
                  } else {
                     this.addLink(nvCntr, link, !onLine);
                  }
               } catch (Throwable var7) {
                  var7.printStackTrace();
                  System.out.println("Error in addLinks: " + var7);
               }
            }
         }
      }

      Knob[] kbs = nvCntr.asComponent().getKnobs();

      for (int i = 0; i < kbs.length; i++) {
         BLink lnk = kbs[i].getLink();
         if (lnk.getType().is(BLonLink.TYPE) && ((BLonLink)lnk).getRemoteLink()) {
            this.addRemoteLinkToTgt(nvCntr, (BLonLink)lnk, !onLine);
         }
      }
   }

   private void addLink(BINvContainer nvCntr, BLonLink link, boolean devError) {
      LonPoint p1 = null;
      LonPoint p2 = null;
      Connection conn = null;
      BLonLinkType linkType = link.getLinkType();
      boolean priority = link.getPriority();
      int selector = -1;
      if (this.debug) {
         System.out.println("\n addlink ->" + link);
      }

      BINvContainer srcDev = (BINvContainer)link.getSourceComponent();
      BNetworkVariable srcNv = (BNetworkVariable)srcDev.asComponent().get(link.getSourceSlot().asProperty());
      BNetworkVariable tgtNv = (BNetworkVariable)nvCntr.asComponent().get(link.getTargetSlot().asProperty());
      LonPoint srcPnt = new LonPoint(srcDev, srcNv);
      LonPoint tgtPnt = new LonPoint(nvCntr, tgtNv);
      if (this.debug) {
         System.out.println(" srcPnt =>" + srcPnt + "\n tgtPnt =>" + tgtPnt);
      }

      int srcSel = this.getSelector(srcPnt);
      int tgtSel = this.getSelector(tgtPnt);
      if (srcSel < 0 && tgtSel < 0) {
         if (this.debug) {
            System.out.println(" addLink srcSel < 0 && tgtSel < 0");
         }

         selector = this.addConnection(srcPnt, tgtPnt, linkType, priority);
         conn = this.table[selector];
      } else if (srcSel < 0) {
         conn = this.table[tgtSel];
         if (this.debug) {
            System.out.println(" addLinkedPoint selector = " + tgtSel);
         }

         this.table[tgtSel].addLinkedPoint(tgtPnt, srcPnt, linkType, priority);
         this.hash(srcPnt, tgtSel);
      } else if (tgtSel < 0) {
         conn = this.table[srcSel];
         if (this.debug) {
            System.out.println(" addLinkedPoint selector = " + srcSel);
         }

         this.table[srcSel].addLinkedPoint(srcPnt, tgtPnt, linkType, priority);
         this.hash(tgtPnt, srcSel);
      } else if (srcSel == tgtSel) {
         conn = this.table[srcSel];
         this.verifySelector(srcSel);
         if (this.debug) {
            System.out.println(" srcSel == tgtSel  selector = " + srcSel);
         }

         this.table[srcSel].addLink(tgtPnt, srcPnt, linkType, priority);
      } else if ((conn = this.table[srcSel].findAliasConnection(this.table[tgtSel], srcPnt)) != null) {
         if (this.debug) {
            System.out.println(" p1 in selector " + srcSel + " has alias in " + tgtSel);
         }

         p1 = conn.findAliasPoint(srcPnt);
         p2 = conn.findPoint(tgtPnt);
         if (p2 == null) {
            conn.addPoint(tgtPnt);
         }

         conn.setLinkType(linkType);
      } else {
         if ((conn = this.table[tgtSel].findAliasConnection(this.table[srcSel], tgtPnt)) == null) {
            if (this.debug) {
               System.out.println("\n******* unresolvedLink srcSel=" + srcSel + " tgtSel=" + tgtSel + " ********");
            }

            this.unresolvedLink(srcPnt, tgtPnt, linkType, priority);
            return;
         }

         if (this.debug) {
            System.out.println(" p2 in selector " + tgtSel + " has alias in " + srcSel);
         }

         p1 = conn.findPoint(srcPnt);
         p2 = conn.findAliasPoint(tgtPnt);
         if (p1 == null) {
            conn.addPoint(srcPnt);
         }

         conn.setLinkType(linkType);
      }

      if (p1 != null && p1.getStatus() == BLonLinkStatus.obsolete) {
         p1.setStatus(BLonLinkStatus.bound);
      }

      if (p2 != null && p2.getStatus() == BLonLinkStatus.obsolete) {
         p2.setStatus(BLonLinkStatus.bound);
      }

      if (conn != null && this.debug) {
         System.out.println(conn);
      }
   }

   private void addRemoteLinkToTgt(BINvContainer nvCntr, BLonLink lonLnk, boolean devError) {
      BINvContainer tgtDev = (BINvContainer)lonLnk.getTargetComponent();
      BNetworkVariable srcNv = (BNetworkVariable)nvCntr.asComponent().get(lonLnk.getSourceSlot().asProperty());
      BNetworkVariable tgtNv = (BNetworkVariable)tgtDev.asComponent().get(lonLnk.getTargetSlot().asProperty());
      BLonLinkStatus status = srcNv.getNvConfigData().isBoundNv() ? BLonLinkStatus.bound : BLonLinkStatus.newLink;
      LonPoint lp = new LonPoint(nvCntr, srcNv, status);
      LonPointLocal lpc = new LonPointRemote(nvCntr, srcNv, status, this.local, tgtDev, tgtNv);
      int sel = this.getSelector(lp);
      BLonLinkType linkType = lonLnk.getLinkType();
      boolean priority = lonLnk.getPriority();
      if (sel < 0) {
         if (this.debug) {
            System.out.println(" add remote link");
         }

         this.addConnection(lp, lpc, linkType, priority);
      } else {
         if (this.debug) {
            System.out.println(" add remote Link to sel = " + sel);
         }

         this.table[sel].addLinkedProxy(lp, lpc, linkType, priority);
         this.hash(lpc, sel);
      }
   }

   private void addRemoteLinkToSrc(BINvContainer nvCntr, BLonLink lonLnk, boolean devError) {
      BINvContainer srcDev = (BLonDevice)lonLnk.getSourceComponent();
      BNetworkVariable srcNv = (BNetworkVariable)srcDev.asComponent().get(lonLnk.getSourceSlot().asProperty());
      BNetworkVariable tgtNv = (BNetworkVariable)nvCntr.asComponent().get(lonLnk.getTargetSlot().asProperty());
      BLonLinkStatus status = tgtNv.getNvConfigData().isBoundNv() ? BLonLinkStatus.bound : BLonLinkStatus.newLink;
      LonPoint lp = new LonPoint(nvCntr, tgtNv, status);
      LonPointLocal lpc = new LonPointRemote(nvCntr, tgtNv, status, this.local, srcDev, srcNv);
      int sel = this.getSelector(lp);
      BLonLinkType linkType = lonLnk.getLinkType();
      boolean priority = lonLnk.getPriority();
      if (sel < 0) {
         if (this.debug) {
            System.out.println(" add remote link");
         }

         this.addConnection(lpc, lp, linkType, priority);
      } else {
         if (this.debug) {
            System.out.println(" add remote Link to sel = " + sel);
         }

         this.table[sel].addLinkedProxy(lp, lpc, linkType, priority);
         this.hash(lpc, sel);
      }
   }

   private void addPseudoNv(BPseudoNV pnv) {
      BNvConfigData nvCnfg = pnv.getNvConfigData();
      if (nvCnfg.isBoundNv()) {
         int pSelector = nvCnfg.getSelector();
         LonPointPseudo lpc = new LonPointPseudo(null, null, BLonLinkStatus.obsolete, this.local, (BComponent)pnv.getParent(), pnv);
         lpc.setAddressIndex(nvCnfg.getAddrIndex());
         if (this.debug) {
            System.out.println("\n found bound pseudo " + lpc);
         }

         this.addPoint(lpc, pSelector);
      }
   }

   private void addPseudoLinkToSrc(BINvContainer nvCntr, BLonLink lonLnk, boolean devError) {
      BComponent srcCmp = lonLnk.getSourceComponent();
      BPseudoNV srcNv = (BPseudoNV)srcCmp.asComponent().get(lonLnk.getSourceSlot().asProperty());
      BNetworkVariable tgtNv = (BNetworkVariable)nvCntr.asComponent().get(lonLnk.getTargetSlot().asProperty());
      BLonLinkStatus status = tgtNv.getNvConfigData().isBoundNv() ? BLonLinkStatus.bound : BLonLinkStatus.newLink;
      LonPoint lp = new LonPoint(nvCntr, tgtNv, status);
      LonPointPseudo lpc = new LonPointPseudo(nvCntr, tgtNv, BLonLinkStatus.newLink, this.local, srcCmp, srcNv);
      lpc.setAddressIndex(srcNv.getNvConfigData().getAddrIndex());
      int sel = this.getSelector(lp);
      int pSel = this.getSelector(lpc);
      BLonLinkType linkType = lonLnk.getLinkType();
      boolean priority = lonLnk.getPriority();
      if (sel < 0 && pSel < 0) {
         if (this.debug) {
            System.out.println(" add pseudo link");
         }

         this.addConnection(lpc, lp, linkType, priority);
      } else if (sel < 0) {
         if (this.debug) {
            System.out.println(" add linked pseudo Link to sel = " + sel);
         }

         this.table[pSel].addLinkedPoint(lpc, lp, linkType, priority);
         this.hash(lp, pSel);
      } else if (pSel < 0) {
         if (this.debug) {
            System.out.println(" add pseudo Link to sel = " + sel);
         }

         this.table[sel].addLinkedPoint(lp, lpc, linkType, priority);
         this.hash(lpc, sel);
      } else {
         if (sel != pSel) {
            if (this.debug) {
               System.out.println("\n******* unresolvedLink pseudo sel=" + pSel + " tgt sel=" + sel + " ********");
            }

            this.unresolvedLink(lpc, lp, linkType, priority);
            return;
         }

         if (this.debug) {
            System.out.println(" add linked pseudo Link to sel = " + sel);
         }

         this.table[sel].addLink(lp, lpc, linkType, priority);
      }
   }

   private int addConnection(LonPoint srcPnt, LonPoint tgtPnt, BLonLinkType linkType, boolean priority) {
      int selector = this.getNextSelector();
      if (selector == -1) {
         return -1;
      } else {
         this.hash(srcPnt, selector);
         this.hash(tgtPnt, selector);
         if (this.debug) {
            System.out.println("  Addconnection selector = " + selector + "\n   p1 =>" + srcPnt + "\n   p2 =>" + tgtPnt);
         }

         this.table[selector] = new Connection(srcPnt, tgtPnt, selector, linkType, priority, this.debug);
         return selector;
      }
   }

   private int getNextSelector() {
      int selector;
      for (selector = 1; selector < this.table.length; selector++) {
         if (this.table[selector] == null) {
            return selector;
         }
      }

      this.verifySelector(selector);
      return selector;
   }

   private void verifySelector(int selector) {
      if (selector >= this.table.length) {
         if (selector > 12287) {
            throw new BajaRuntimeException("Exceeded maximum selector.");
         } else {
            int newLength = selector + INITIAL_CONNECTIONS;
            if (newLength > 12287) {
               newLength = 12287;
            }

            Connection[] newTable = new Connection[newLength];
            System.arraycopy(this.table, 0, newTable, 0, this.table.length);
            this.table = newTable;
         }
      }
   }

   private void addPoint(LonPoint point, int selector) {
      this.verifySelector(selector);
      if (this.table[selector] == null) {
         if (this.debug) {
            System.out.println(" in addPoint - add new connection at selector " + selector + " " + point);
         }

         this.table[selector] = new Connection(point, selector, this.debug);
      } else {
         if (this.debug) {
            System.out.println(" in addPoint - addpoint at selector " + selector + " " + point);
         }

         this.table[selector].addPoint(point);
      }

      this.hash(point, selector);
   }

   private void addPoint(LonPoint point, Connection conn) {
      if (this.debug) {
         System.out.println(" in addPoint - addpoint at selector " + conn.getSelector() + " " + point);
      }

      conn.addPoint(point);
      this.hash(point, conn.getSelector());
   }

   private void unresolvedLink(LonPoint srcPnt, LonPoint tgtPnt, BLonLinkType linkType, boolean priority) {
      if (this.debug) {
         System.out.println(" add to unresolved:\n    p1: " + srcPnt + "\n    p2: " + tgtPnt);
      }

      ConnectionTable.UnresolvedLink urLnk = new ConnectionTable.UnresolvedLink(srcPnt, tgtPnt, linkType, priority);
      this.unresolved.addElement(urLnk);
   }

   private void processUnresolved() {
      for (int i = 0; i < this.unresolved.size(); i++) {
         ConnectionTable.UnresolvedLink u = this.unresolved.elementAt(i);
         int srcSel = this.getSelector(u.srcPnt);
         int tgtSel = this.getSelector(u.tgtPnt);
         if (this.debug) {
            System.out.println("\nprocessUnresolved:\n  srcSel=" + srcSel + " p1: " + u.srcPnt + "\n  tgtSel=" + tgtSel + " p2: " + u.tgtPnt);
         }

         if (srcSel == tgtSel) {
            this.table[srcSel].addLink(u.srcPnt, u.tgtPnt, u.linkType, u.priority);
            if (this.debug) {
               System.out.println("\n  addLink: " + this.table[srcSel]);
            }
         } else if (srcSel == -1 || this.table[srcSel].okayToRemove(u.srcPnt)) {
            this.removePointAll(u.srcPnt);
            this.table[tgtSel].addLinkedPoint(u.tgtPnt, u.srcPnt, u.linkType, u.priority);
            this.moveHash(u.srcPnt, tgtSel);
            if (this.debug) {
               System.out.println("\n  addLinkedPoint: " + this.table[tgtSel]);
            }
         } else if (tgtSel != -1 && !this.table[tgtSel].okayToRemove(u.tgtPnt)) {
            boolean hubIs1 = u.srcPnt.isOutput();
            int hubSel = hubIs1 ? srcSel : tgtSel;
            tgtSel = hubIs1 ? tgtSel : srcSel;
            LonPoint hub = hubIs1 ? u.srcPnt : u.tgtPnt;
            LonPoint tgt = hubIs1 ? u.tgtPnt : u.srcPnt;
            Connection hubCon = this.table[hubSel].findConnection(hub);
            Connection tgtCon = this.table[tgtSel].findConnection(tgt);
            if (!this.table[hubSel].okToMerge(this.table[tgtSel])) {
               Connection aliasConn = this.table[tgtSel].findAliasConnection(hub);
               if (aliasConn != null) {
                  aliasConn.addLinkedPoint(aliasConn.getHub(), tgt, u.linkType, u.priority);
               } else {
                  aliasConn = this.createNewAliasConnection(hubCon, tgtSel, false);
                  aliasConn.addPoint(tgt);
                  aliasConn.setLinkType(u.linkType);
                  hubCon.addAliasConnection(aliasConn);
               }

               if (this.debug) {
                  System.out.println("\n  after processing: hubCon:" + this.table[hubSel]);
               }

               if (this.debug) {
                  System.out.println("tgtConn:" + this.table[tgtSel]);
               }
            } else if (this.table[hubSel].countPoints() >= this.table[tgtSel].countPoints()) {
               this.mergeConnections(this.table[hubSel], this.table[tgtSel]);
               hubCon.addLink(hub, tgt, u.linkType, u.priority);
               if (this.debug) {
                  System.out.println("\n  after merge: " + this.table[hubSel]);
               }
            } else {
               this.mergeConnections(this.table[tgtSel], this.table[hubSel]);
               tgtCon.addLink(tgt, hub, u.linkType, u.priority);
               if (this.debug) {
                  System.out.println("\n  after merge: " + this.table[tgtSel]);
               }
            }
         } else {
            this.removePointAll(u.tgtPnt);
            this.table[srcSel].addLinkedPoint(u.srcPnt, u.tgtPnt, u.linkType, u.priority);
            this.moveHash(u.tgtPnt, srcSel);
            if (this.debug) {
               System.out.println("\n  addLinkedPoint: " + this.table[srcSel]);
            }
         }
      }
   }

   private void mergeConnections(Connection c1, Connection c2) {
      if (this.debug) {
         System.out.println("\n  mergeConnections:\nc1:" + c1 + "\nc2:" + c2);
      }

      this.table[c2.getSelector()] = null;
      int srcSel = c1.getSelector();

      for (Connection c = c2; c != null; c = c.getSecondary()) {
         this.moveConnection(c, srcSel);
      }

      c1.mergeConnection(c2);
   }

   private void moveConnection(Connection c, int newSel) {
      LonPoint p = c.getHub();
      this.moveHash(p, newSel);
      LonPoint[] tgts = c.getTargets();

      for (int n = 0; n < tgts.length; n++) {
         this.moveHash(tgts[n], newSel);
      }

      c.setSelector(newSel);
   }

   private void moveHash(LonPoint p, int sel) {
      if (p != null) {
         this.unhash(p);
         this.hash(p, sel);
         if (p.getStatus() == BLonLinkStatus.bound) {
            p.setStatus(BLonLinkStatus.newLink);
         }
      }
   }

   private void removePointAll(LonPoint point) {
      int sel = this.getSelector(point);
      if (sel >= 0) {
         Connection conn = this.table[sel];
         if (this.debug) {
            System.out.println(" remove from selector = " + conn.getSelector() + " point:" + point);
         }

         while (conn != null) {
            conn.removePoint(point);
            conn = conn.getSecondary();
         }

         this.unhash(point);
      }
   }

   private void removePoint(LonPoint point, Connection conn) {
      point.setStatus(BLonLinkStatus.newLink);
      if (this.debug) {
         System.out.println("\n remove from selector = " + conn.getSelector() + " point:" + point);
      }

      conn.removePoint(point);
      this.unhash(point);
   }

   private void addAliasNv(BLonDevice dev) {
      BAliasTable tab = dev.getDeviceData().getAliasTable();
      BAliasConfigData[] a = tab.getAliasArray();
      if (this.debug && tab.getAliasCount() > 0) {
         System.out.println("\nAnalyze aliases for " + dev.getDisplayName(null));
      }

      for (int i = 0; i < a.length; i++) {
         BAliasConfigData cnfig = a[i];
         if (cnfig != null) {
            if (cnfig.isBoundNv()) {
               int primNv = cnfig.getPrimary();
               BNetworkVariable nv = dev.getNetworkVariable(primNv);
               if (nv == null) {
                  System.out.println("Error: " + dev.getDisplayName(null) + " alias " + i + " has invalid primNv " + primNv);
               } else {
                  LonPointAlias point = new LonPointAlias((BINvContainer)nv.getParent(), nv, BLonLinkStatus.obsolete, i);
                  point.setAddressIndex(cnfig.getAddrIndex());
                  if (this.debug) {
                     System.out.println("\n found bound alias point" + point);
                  }

                  int selector = cnfig.getSelector();
                  this.verifySelector(selector);
                  this.addPoint(point, selector);
                  int primNvHash = point.primaryHashCode();
                  int primSel = this.getSelector(primNvHash);
                  if (primSel >= 0) {
                     this.verifySelector(primSel);
                     Connection primCon = this.table[this.getSelector(primNvHash)];
                     Connection aliasCon = this.table[selector].findConnection(point);
                     primCon = primCon.findAliasPrimary(primNvHash);
                     if (primCon != null && aliasCon != null) {
                        primCon.addAliasConnection(aliasCon);
                     }

                     cnfig.setBound();
                  }
               }
            } else {
               cnfig.setFree();
            }
         }
      }
   }

   private void processAliasConnections() {
      if (this.debug) {
         System.out.println("\n\nDo alias connection check.");
      }

      if (this.debug) {
         System.out.println("\nCreate new connections.");
      }

      for (int sel = 0; sel < this.table.length; sel++) {
         Connection cnctn = this.table[sel];
         if (cnctn != null) {
            this.discoverNewAlias(cnctn);
         }
      }

      if (this.debug) {
         System.out.println("\n\nCompress and free.");
      }

      for (int selx = 0; selx < this.table.length; selx++) {
         Connection cnctn = this.table[selx];

         for (Connection c = cnctn; c != null; c = c.getSecondary()) {
            if (c.hasAliasConnection()) {
               this.compressAliases(c);
            }

            if (c.hasAliasConnection()) {
               this.checkForObsoletePrimary(c);
            }
         }
      }

      if (this.debug) {
         System.out.println("\n\nMake obsolete aliases available.");
      }

      for (int selx = 0; selx < this.table.length; selx++) {
         Connection cnctn = this.table[selx];

         for (Connection c = cnctn; c != null; c = c.getSecondary()) {
            if (c.isAliasConnection()) {
               this.freeObsoleteAliases(c);
            }
         }
      }

      if (this.debug) {
         System.out.println("\n\nCheck for unresolved aliases.");
      }

      for (int selx = 0; selx < this.table.length; selx++) {
         Connection cnctn = this.table[selx];

         for (Connection cx = cnctn; cx != null; cx = cx.getSecondary()) {
            if (cx.isAliasConnection()) {
               this.checkInvalidAliasIndex(cx);
            }
         }
      }
   }

   private void freeObsoleteAliases(Connection cnctn) {
      LonPointAlias hub = (LonPointAlias)cnctn.getHub();
      if (hub != null && hub.isObsolete()) {
         BLonDevice dev = hub.getLonDevice();
         BAliasConfigData[] aliasTable = dev.getDeviceData().getAliasTable().getAliasArray();
         if (this.debug) {
            System.out.println("Clear alias ndx=" + hub.getAliasIndex() + "|" + aliasTable[hub.getAliasIndex()].toString());
         }

         aliasTable[hub.getAliasIndex()].makeAvailable();
      }
   }

   private void compressAliases(Connection cnctn) {
      Connection[] aliasCons = cnctn.getAliasConnections();
      if (aliasCons != null) {
         Connection base = cnctn;

         for (int i = 0; i < aliasCons.length; i++) {
            for (int j = aliasCons.length - 1; j >= i; j--) {
               LonPoint[] pnts = base.getAliasPointsToCompress(aliasCons[j]);
               if (pnts != null) {
                  if (this.debug) {
                     System.out.println(" compress " + pnts.length + " pnts from alias " + j + " to " + (i == 0 ? "primary" : Integer.toString(i - 1)));
                  }

                  this.movePoints(pnts, aliasCons[j], base);
               }
            }

            LonPoint[] pnts = base.getSameDevicePoints();
            if (pnts != null) {
               if (this.debug) {
                  System.out.println(" move " + pnts.length + " points up to next alias ");
               }

               this.movePoints(pnts, base, aliasCons[i]);
            }

            base = aliasCons[i];
         }

         for (int i = aliasCons.length - 1; i >= 0; i--) {
            if (aliasCons[i].getActiveTargetCount() == 0) {
               LonPointAlias pnt = (LonPointAlias)aliasCons[i].getHub();
               if (pnt != null) {
                  if (this.debug) {
                     System.out.println("detected alias to remove " + pnt);
                  }

                  if (pnt.isNew()) {
                     this.removeOriginalAliasPoint(pnt);
                  } else {
                     pnt.setStatus(BLonLinkStatus.obsolete);
                  }
               }
            }
         }
      }
   }

   private void movePoints(LonPoint[] pnts, Connection srcCon, Connection destCon) {
      for (int i = 0; i < pnts.length; i++) {
         LonPoint pnt = pnts[i];
         if (this.debug) {
            System.out.println("  move point to sel = " + destCon.getSelector() + ":\n   " + pnt);
         }

         this.removePoint(pnt, srcCon);
         this.addPoint(pnt, destCon);
         destCon.setLinkType(srcCon.getLinkType());
      }

      this.updateHubStatus(destCon, srcCon.getLinkType());
   }

   private void checkInvalidAliasIndex(Connection c) {
      LonPointAlias p = (LonPointAlias)c.getHub();
      if (p.getAliasIndex() < 0) {
         int aliasIndex = this.reserveAvailableAlias(p.getLonDevice());
         if (aliasIndex >= 0) {
            this.unhash(p);
            p.setAliasIndex(aliasIndex);
            this.removeOriginalAliasPoint(p);
            this.hash(p, c.getSelector());
         } else {
            c.setStatus(BLonLinkStatus.aliasError);
         }
      }
   }

   private void checkForObsoletePrimary(Connection c) {
      if (!c.isActive() && !c.isError()) {
         Connection aliasCon = c.getActiveAliasConnection();
         if (aliasCon != null) {
            if (this.debug) {
               System.out.println("Remove alias from " + aliasCon.getSelector() + " because of obsolete primary in " + c.getSelector());
            }

            LonPoint primHub = c.getHub();
            LonPoint aliasHub = aliasCon.getHub();
            this.unhash(primHub);
            this.unhash(aliasHub);
            primHub.setStatus(BLonLinkStatus.newLink);
            aliasHub.setStatus(BLonLinkStatus.obsolete);
            this.hash(primHub, aliasCon.getSelector());
            this.hash(aliasHub, c.getSelector());
            aliasCon.setHub(primHub);
            c.setHub(aliasHub);
            c.removeAliasConnection(aliasCon);
            Connection[] cons = c.getAliasConnections();
            if (cons != null) {
               for (int i = 0; i < cons.length; i++) {
                  c.removeAliasConnection(cons[i]);
                  aliasCon.addAliasConnection(cons[i]);
               }
            }
         }
      }
   }

   private void discoverNewAlias(Connection cnctn) {
      Vector<LonPoint> pnts = cnctn.getAllSameDevicePoints();
      boolean hasSecondaries = cnctn.getSecondary() != null;

      while (pnts.size() > 1) {
         LonPoint pnt = pnts.elementAt(0);

         for (int i = 1; i < pnts.size(); i++) {
            LonPoint p = pnts.elementAt(i);
            if (p.overlappCnt < pnt.overlappCnt || p.overlappCnt == pnt.overlappCnt && pnt.isBound() && !p.isBound()) {
               pnt = p;
            }
         }

         pnts.removeElement(pnt);
         int selector = this.getNextSelector();
         boolean newSel = true;

         for (Connection srcCon = cnctn.findConnection(pnt, true); srcCon != null; newSel = false) {
            Connection aliasCon = srcCon.findAliasConnectionForPoint(pnt, selector, !newSel);
            if (aliasCon != null) {
               selector = aliasCon.getSelector();
               if (this.debug) {
                  System.out.println("found alias connection for point " + selector);
               }

               this.removePoint(pnt, srcCon);
               this.addPoint(pnt, aliasCon);
               this.updateHubStatus(aliasCon, srcCon.getLinkType());
            } else if (hasSecondaries && !srcCon.hasAnyOverlappingTargets(pnt)) {
               int origSel = cnctn.getSelector();
               if (srcCon == cnctn) {
                  cnctn = srcCon.removeAllSecondaries();
                  this.table[origSel] = cnctn;
               } else {
                  cnctn.removeSecondary(srcCon);
               }

               this.moveConnection(srcCon, selector);
               if (this.debug) {
                  System.out.println("moveConnection with no other overlapps from " + origSel + " to " + selector + "\n" + srcCon);
               }

               this.table[selector] = srcCon;
            } else {
               aliasCon = this.createNewAliasConnection(srcCon, selector, newSel);
               this.removePoint(pnt, srcCon);
               this.addPoint(pnt, aliasCon);
               this.updateHubStatus(aliasCon, srcCon.getLinkType());
               srcCon.addAliasConnection(aliasCon);
               if (this.debug) {
                  System.out.println(" create new alias " + aliasCon.toString(false));
               }
            }

            if (cnctn == null) {
               srcCon = null;
            } else {
               srcCon = cnctn.findConnection(pnt, true);
            }
         }

         if (cnctn != null && pnts.size() <= 1) {
            pnts = cnctn.getAllSameDevicePoints();
         }
      }
   }

   private void updateHubStatus(Connection destCon, BLonLinkType lnkTyp) {
      LonPoint hub;
      if (destCon.getActiveTargetCount() > 0 && (hub = destCon.getHub()) != null && hub.isObsolete()) {
         hub.setStatus(BLonLinkStatus.bound);
         if (destCon.getLinkType() == BLonLinkType.unknown) {
            destCon.setLinkType(lnkTyp);
         }
      }
   }

   private Connection createNewAliasConnection(Connection con, int selector, boolean newSel) {
      LonPoint hub = con.getHub();
      BLonDevice dev = hub.getLonDevice();
      int aliasIndex = this.reserveAvailableAlias(dev);
      LonPointAlias aPnt = new LonPointAlias(hub.getNvContainer(), hub.getNetworkVariable(), BLonLinkStatus.newLink, aliasIndex);
      this.hash(aPnt, selector);
      Connection aliasCon = new Connection(aPnt, selector, this.debug);
      aliasCon.setLinkType(con.getLinkType());
      if (newSel) {
         this.table[selector] = aliasCon;
      } else {
         this.table[selector].addSecondary(aliasCon);
      }

      return aliasCon;
   }

   private int reserveAvailableAlias(BLonDevice dev) {
      BAliasConfigData[] aliasTable = dev.getDeviceData().getAliasTable().getAliasArray();

      for (int i = 0; i < aliasTable.length; i++) {
         if (aliasTable[i].isAvailable()) {
            aliasTable[i].reserve();
            return i;
         }
      }

      return -1;
   }

   public Connection findConnectionForHash(int hashCode) {
      int sel = this.getSelector(hashCode);
      if (sel >= 0 && sel <= this.table.length) {
         Connection con;
         for (con = this.table[sel]; con != null; con = con.getSecondary()) {
            LonPoint hub = con.getHub();
            if (hub != null && hub.hashCode() == hashCode) {
               break;
            }
         }

         return con;
      } else {
         return null;
      }
   }

   private void removeOriginalAliasPoint(LonPointAlias aPnt) {
      int origSel = this.getSelector(aPnt);
      if (origSel != -1) {
         Connection c = this.table[origSel].findConnection(aPnt);
         if (c == null) {
            return;
         }

         if (this.debug) {
            System.out.println("Removing alias pnt from selector " + origSel + "\n" + aPnt);
         }

         LonPoint p = c.findPoint(aPnt);
         if (p.isObsolete() || p.isNew()) {
            this.unhash(p);
            c.removePoint(p);
         }
      }
   }

   public GroupTable.UseCnt[] addressEntryUsage(BLonDevice dev) {
      GroupTable.UseCnt[] useCnt = new GroupTable.UseCnt[dev.getDeviceData().getAddressCount()];

      for (int i = 0; i < useCnt.length; i++) {
         useCnt[i] = new GroupTable.UseCnt();
      }

      int devNdx = this.getDeviceIndex(dev);

      for (int sel = 0; sel < this.table.length; sel++) {
         for (Connection cnctn = this.table[sel]; cnctn != null; cnctn = cnctn.getSecondary()) {
            LonPoint[] a = cnctn.findAppDevice(dev);

            for (int i = 0; i < a.length; i++) {
               LonPoint pnt = a[i];
               int adrNdx = pnt.getAddressIndex();
               if (adrNdx == -1) {
                  int addressGroup = cnctn.getAddressGroup();
                  if (addressGroup < 0) {
                     continue;
                  }

                  Group grp = this.getGroupTable().getGroupArray()[addressGroup];
                  if (grp == null) {
                     continue;
                  }

                  GroupMember mem = grp.findMember(devNdx);
                  if (mem == null) {
                     continue;
                  }

                  adrNdx = mem.getAddressIndex();
               }

               if (adrNdx < useCnt.length) {
                  useCnt[adrNdx].cnt++;
                  GroupBitSet exclude = cnctn.getExcludeBitSet(this.lonDevices);
                  if (exclude != null) {
                     if (useCnt[adrNdx].exclude == null) {
                        useCnt[adrNdx].exclude = new GroupBitSet(exclude);
                     } else {
                        useCnt[adrNdx].exclude.or(exclude);
                     }
                  }

                  GroupBitSet include = cnctn.getGroupBitSet(this.lonDevices);
                  if (useCnt[adrNdx].include == null) {
                     useCnt[adrNdx].include = new GroupBitSet(include);
                  } else {
                     useCnt[adrNdx].include.or(include);
                  }
               }
            }
         }
      }

      for (int i = 0; i < this.tagTable.size(); i++) {
         TagConnection tCon = this.tagTable.elementAt(i);
         if (tCon.getAddressGroup() >= 0) {
            TagPoint tPt = tCon.findDevice(dev);
            if (tPt != null && tPt.getTagIndex() >= 0 && tPt.getTagIndex() < useCnt.length) {
               useCnt[tPt.getTagIndex()].cnt++;
               useCnt[tPt.getTagIndex()].mtag = true;
            }
         }
      }

      return useCnt;
   }

   GroupBitSet getExcludesForGroup(int groupNum) {
      GroupBitSet excludes = null;

      for (int sel = 0; sel < this.table.length; sel++) {
         for (Connection cnctn = this.table[sel]; cnctn != null; cnctn = cnctn.getSecondary()) {
            if (cnctn.getAddressGroup() == groupNum) {
               GroupBitSet ex = cnctn.getExcludeBitSet(this.lonDevices);
               if (ex != null) {
                  if (excludes == null) {
                     excludes = new GroupBitSet(ex);
                  } else {
                     excludes.or(ex);
                  }
               }
            }
         }
      }

      return excludes;
   }

   public void changeEntry(BLonDevice dev, int newEntry, int oldEntry, int groupNum) {
      for (int sel = 0; sel < this.table.length; sel++) {
         for (Connection cnctn = this.table[sel]; cnctn != null; cnctn = cnctn.getSecondary()) {
            LonPoint[] a = cnctn.findAppDevice(dev);

            for (int i = 0; i < a.length; i++) {
               LonPoint pnt = a[i];
               if (pnt != null && pnt.getAddressIndex() == oldEntry && (pnt.isBound() || pnt.isNew())) {
                  if (this.debug) {
                     System.out.println(" changing address entry " + pnt + " to group " + groupNum);
                  }

                  pnt.setAddressIndex(newEntry);
                  pnt.setStatus(BLonLinkStatus.newLink);
                  cnctn.setAddressGroup(groupNum);
               }
            }
         }
      }
   }

   public void reviewOverlapps() {
      if (this.debug) {
         System.out.println("\n\nReview overlapping connections.");
      }

      for (int sel = 0; sel < this.table.length; sel++) {
         Connection cnctn = this.table[sel];
         if (cnctn != null) {
            for (Connection c = cnctn; c != null; c = c.getSecondary()) {
               c.removeObsoleteOverlappPnts();
            }

            for (Connection noOvlp = cnctn.removeNoOverlappSecondary(); noOvlp != null; noOvlp = cnctn.removeNoOverlappSecondary()) {
               if (cnctn == noOvlp) {
                  if (this.debug) {
                     System.out.println("\nRemove root in sel " + sel);
                  }

                  this.table[cnctn.getSelector()] = cnctn.getSecondary();
                  cnctn = cnctn.getSecondary();
                  noOvlp.finalRemove();
                  noOvlp.removeAllSecondaries();
                  if (cnctn == null) {
                     break;
                  }
               } else if (noOvlp.getHub() == null && noOvlp.getSecondary() == null) {
                  noOvlp.finalRemove();
               } else {
                  int newSel = this.getNextSelector();
                  if (this.debug) {
                     System.out.println("\nMove connection from sel " + sel + " to " + newSel);
                  }

                  noOvlp.newSelector(newSel);
                  this.table[newSel] = noOvlp;
               }
            }
         }
      }
   }

   public void verifyConnection() {
      if (this.debug) {
         System.out.println("\n\nDo connection verifications.");
      }

      for (int sel = 0; sel < this.table.length; sel++) {
         Connection cnctn = this.table[sel];
         if (cnctn != null) {
            if (cnctn.removeEmpty()) {
               this.table[sel] = cnctn.getSecondary();
               cnctn = this.table[sel];
            }

            if (cnctn != null) {
               for (; cnctn != null; cnctn = cnctn.getSecondary()) {
                  if (cnctn.isActive()) {
                     cnctn.verifyConnection();
                     this.verifyAddress(cnctn);
                  }
               }
            }
         }
      }
   }

   private void verifyAddress(Connection connection) {
      LonPoint hub = connection.getHub();
      if (hub != null) {
         BIAddressEntry adr = hub.getAddressEntry();
         if (adr != null) {
            if (adr.getDomain() != hub.getLonDevice().getWorkingDomain()) {
               hub.setStatus(BLonLinkStatus.newLink);
               if (this.debug) {
                  System.out.println("address entry domain mismatch ");
               }
            } else if (hub.getStatus() == BLonLinkStatus.descriptorError) {
               if (adr.isGroupAddress()) {
                  this.groupTable.getGroup(adr.getGroupOrSubnet()).setStatus(1);
               }

               hub.setStatus(BLonLinkStatus.newLink);
               if (this.debug) {
                  System.out.println("descriptor mismatch: force update of " + adr);
               }
            } else if (hub.requiresAddressEntry() && adr.isSubnetNodeAddress()) {
               LonPoint aTgt = connection.getActiveTargetNotTurnaround();
               if (aTgt != null) {
                  BSubnetNode sn = aTgt.getLonDevice().getDeviceData().getSubnetNodeId();
                  if (!sn.equals(adr.getSubnetNodeAddress())) {
                     hub.setStatus(BLonLinkStatus.newLink);
                     if (this.debug) {
                        System.out.println("verifyAddress failed: hub entry " + adr + " tgt actual " + sn + "\n for " + connection.toString(false));
                     }
                  }
               }
            } else {
               GroupBitSet groupBits = connection.getGroupBitSet(this.lonDevices);
               GroupBitSet excludeBits = connection.getExcludeBitSet(this.lonDevices);
               if (!groupBits.excludes(excludeBits)) {
                  hub.setStatus(BLonLinkStatus.groupExcludeError);
                  if (this.debug) {
                     System.out
                        .println("group exclude error \n must contain:" + groupBits + "\n must exclude:" + excludeBits + "\n for " + connection.toString(false));
                  }
               } else {
                  int groupNum = connection.getAddressGroup();
                  if (groupNum != -1) {
                     Group group = this.getGroupTable().getGroup(groupNum);
                     if (!group.getBitSet().contains(groupBits, excludeBits)) {
                        hub.setStatus(BLonLinkStatus.newLink);
                        if (this.debug) {
                           System.out
                              .println(
                                 "verifyAddress failed for "
                                    + groupNum
                                    + "\ngroup set="
                                    + group.getBitSet()
                                    + "\n must contain:"
                                    + groupBits
                                    + "\n must exclude:"
                                    + excludeBits
                                    + "\n for "
                                    + connection.toString(false)
                              );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public BLinkEntryTable getLonLinkTable() {
      BLinkEntryTable lnkTable = new BLinkEntryTable();
      int entryNdx = 0;

      for (int sel = 0; sel < this.table.length; sel++) {
         for (Connection connection = this.table[sel]; connection != null; connection = connection.getSecondary()) {
            LonPoint hub = connection.getHub();
            String hubName;
            String hubNvName;
            BLonLinkStatus status;
            if (hub == null) {
               hubName = "none";
               hubNvName = "none";
               status = BLonLinkStatus.error;
            } else {
               hubName = hub.getDeviceName();
               hubNvName = hub.getNvName();
               status = hub.getStatus();
            }

            LonPoint[] targets = connection.getTargets();
            String service = connection.getLinkType().getTag() + (connection.getPriority() ? " Priority" : "");
            if (targets.length == 0) {
               lnkTable.addEntry(new BLinkEntry(sel, status, hubName, hubNvName, "none", "none", service), entryNdx++);
            }

            for (int ndx = 0; ndx < targets.length; ndx++) {
               LonPoint target = targets[ndx];

               for (BLonLinkStatus var13 = this.getLinkStatus(hub, target, connection);
                  target != null;
                  target = target.isProxy() ? ((LonPointProxy)target).sec : null
               ) {
                  lnkTable.addEntry(new BLinkEntry(sel, var13, hubName, hubNvName, target.getDeviceName(), target.getNvName(), service), entryNdx++);
               }
            }
         }
      }

      if (this.debug) {
         System.out.println("\n dump network variable link table");
         BLinkEntry[] table = lnkTable.getLinkEntries();

         for (int i = 0; i < table.length; i++) {
            System.out.println(table[i].toString());
         }
      }

      return lnkTable;
   }

   public BTagLinkEntryTable getLonMessageTagTable() {
      Vector<BTagLinkEntry> v = new Vector<>(50);
      TagConnection[] tags = this.getMessageTagTable();

      for (int i = 0; i < tags.length; i++) {
         TagPoint out = tags[i].getOutput();
         TagPoint[] ins = tags[i].getInputs();
         if (ins.length == 0) {
            v.addElement(new BTagLinkEntry(out.getStatus(), out.getDeviceName(), out.getTagName(), "n/a", "n/a"));
         } else {
            for (int ndx = 0; ndx < ins.length; ndx++) {
               v.addElement(new BTagLinkEntry(out.getStatus(), out.getDeviceName(), out.getTagName(), ins[ndx].getDeviceName(), ins[ndx].getTagName()));
            }
         }
      }

      BTagLinkEntry[] table = new BTagLinkEntry[v.size()];
      v.copyInto(table);
      if (this.debug) {
         System.out.println("\n dump message tag link table");

         for (int ix = 0; ix < table.length; ix++) {
            System.out.println(ix + " " + table[ix].toString());
         }
      }

      return new BTagLinkEntryTable(table);
   }

   private BLonLinkStatus getLinkStatus(LonPoint hub, LonPoint target, Connection connection) {
      if (connection.isPollOnly()) {
         return BLonLinkStatus.pollOnly;
      } else {
         BLonLinkStatus status;
         if (hub == null || (!hub.isNew() || target == null || !target.isBound()) && !hub.isError() && target != null) {
            status = target.getStatus();
         } else {
            status = hub.getStatus();
         }

         int group = connection.getAddressGroup();
         Group[] grpTbl = this.getGroupTable().getGroupArray();
         if (status == BLonLinkStatus.bound && group >= 0 && (grpTbl[group] == null || grpTbl[group].getStatus() != 2)) {
            status = BLonLinkStatus.dirtyGroup;
         }

         return status;
      }
   }

   public int groupUse(int groupNum) {
      return this.groupUse(groupNum, false);
   }

   public int groupUse(int groupNum, boolean check) {
      int cnt = 0;

      for (int i = 0; i < this.table.length; i++) {
         for (Connection c = this.table[i]; c != null; c = c.getSecondary()) {
            if (c.getAddressGroup() == groupNum && (!check || c.isActive())) {
               cnt++;
            }
         }
      }

      TagConnection[] tags = this.getMessageTagTable();

      for (int i = 0; i < tags.length; i++) {
         if (tags[i].getAddressGroup() == groupNum && (!check || tags[i].isActive())) {
            cnt++;
         }
      }

      return cnt;
   }

   public int getSelector(LonPoint lp) {
      return this.getSelector(lp.hashCode());
   }

   public int getSelector(int hashCode) {
      Object obj = this.pointHash.get(hashCode);
      return obj == null ? -1 : (Integer)obj;
   }

   public void hash(LonPoint lp, int sel) {
      this.pointHash.put(lp.hashCode(), sel);
   }

   public void unhash(LonPoint lp) {
      int code = lp.hashCode();
      this.pointHash.remove(code);
   }

   private boolean isSecondProxy(LonPointProxy lp) {
      int hcd = lp.hashCode();
      LonPointProxy orig = this.proxyHash.get(hcd);
      if (orig != null) {
         lp.sec = orig.sec;
         orig.sec = lp;
         return true;
      } else {
         this.proxyHash.put(hcd, lp);
         return false;
      }
   }

   private void addTagLink(BLonDevice tgtDev, BLonLink link, boolean devError) {
      if (this.debug) {
         System.out.println("\n addTaglink ->" + link);
      }

      BLonDevice srcDev = (BLonDevice)link.getSourceComponent();
      BMessageTag srcMt = (BMessageTag)srcDev.get(link.getSourceSlot().asProperty());
      BMessageTag tgtMt = (BMessageTag)tgtDev.get(link.getTargetSlot().asProperty());
      TagConnection conn = this.tagHash.get(TagPoint.hashCode(srcDev, srcMt.getIndex()));
      if (conn == null) {
         TagPoint outPnt = new TagPoint(srcDev, srcMt.getIndex(), BLonLinkStatus.newLink, srcMt.getName());
         conn = this.addTagConnection(outPnt);
         if (this.debug) {
            System.out.println("\n  added new tagConnection");
         }
      } else {
         TagPoint out = conn.getOutput();
         out.setName(srcMt.getName());
      }

      TagPoint inPnt = new TagPoint(tgtDev, tgtMt.getIndex(), BLonLinkStatus.newLink, tgtMt.getName());
      conn.addInput(inPnt);
      int tgtCode = TagPoint.hashCode(tgtDev, tgtMt.getIndex());
      TagConnection tcon = this.tagHash.get(tgtCode);
      if (tcon != null) {
         this.tagHash.remove(tgtCode);
         this.tagTable.removeElement(tcon);
         inPnt.setStatus(BLonLinkStatus.bound);
         if (this.debug) {
            System.out.println("\n  mtag moved to new conn - remove tagConnection" + tcon);
         }
      }

      if (this.debug) {
         System.out.println("\n  add input tag point " + inPnt + "\n" + conn);
      }
   }

   private void addMessageTags(BLonDevice device) {
      int tagCount = device.getDeviceData().getMsgTagCount();
      if (tagCount > 0) {
         BDeviceData dd = device.getDeviceData();

         for (int i = 0; i < tagCount; i++) {
            BIAddressEntry entry = dd.getAddressEntry(i);
            if (entry.getAddressType() != BAddressType.none) {
               TagPoint pnt = new TagPoint(device, i, BLonLinkStatus.obsolete);
               TagConnection conn = this.addTagConnection(pnt);
               conn.setAddressEntry(entry);
               if (this.debug) {
                  System.out.println("\n  added new tagConnection \n    " + conn);
               }
            }
         }
      }
   }

   private void setMessageTagStatus() {
      TagConnection[] mtagTable = this.getMessageTagTable();

      for (int i = 0; i < mtagTable.length; i++) {
         mtagTable[i].setMessageTagStatus(this, this.getGroupTable());
      }
   }

   private TagConnection addTagConnection(TagPoint pnt) {
      TagConnection conn = new TagConnection(pnt, this.debug);
      this.tagTable.addElement(conn);
      this.tagHash.put(pnt.hashCode(), conn);
      return conn;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\nConnection Table\n=============================================== \n");

      for (int i = 0; i < this.table.length; i++) {
         if (this.table[i] != null) {
            sb.append(i).append(" ").append(this.table[i]).append("\n");
         }
      }

      for (int ix = 0; ix < this.tagTable.size(); ix++) {
         sb.append(ix).append(" ").append(this.tagTable.elementAt(ix)).append("\n");
      }

      return sb.toString();
   }

   public BLonDevice getDevice(int index) {
      return this.lonDevices[index];
   }

   public int getDeviceIndex(BLonDevice dev) {
      return getDeviceIndex(this.lonDevices, dev);
   }

   public static int getDeviceIndex(BLonDevice[] lonDevices, BLonDevice dev) {
      for (int i = 0; i < lonDevices.length; i++) {
         if (lonDevices[i] == dev) {
            return i;
         }
      }

      return -1;
   }

   private static class UnresolvedLink {
      LonPoint srcPnt;
      LonPoint tgtPnt;
      BLonLinkType linkType;
      boolean priority;

      UnresolvedLink(LonPoint p1, LonPoint p2, BLonLinkType d, boolean pr) {
         this.srcPnt = p1;
         this.tgtPnt = p2;
         this.linkType = d;
         this.priority = pr;
      }
   }
}
