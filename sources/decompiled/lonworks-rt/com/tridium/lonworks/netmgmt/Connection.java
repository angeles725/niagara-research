package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonLinkStatus;
import java.util.Vector;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.enums.BLonNvDirection;

public class Connection implements NetMgmtConst {
   LonPoint[] tgtArray = null;
   private boolean foundExBitSet = false;
   private GroupBitSet groupBitSet = null;
   private GroupBitSet excludeBits = null;
   private GroupBitSet targetBitSet = null;
   private BLonLinkType linkType = BLonLinkType.unknown;
   private int addressGroup = -1;
   private boolean addressChange = true;
   private LonPoint hub = null;
   private Vector<LonPoint> targets = new Vector<>(10);
   private Vector<Connection> aliasConnections = null;
   private Connection primary = null;
   private boolean debug;
   private boolean priority = false;
   private int selector;
   private Connection root = this;
   private Connection secondary = null;

   public Connection(LonPoint pnt1, int selector, boolean debug) {
      if (pnt1.isOutput()) {
         this.hub = pnt1;
      } else {
         this.targets.addElement(pnt1);
      }

      this.debug = debug;
      this.selector = selector;
   }

   public Connection(LonPoint pnt1, LonPoint pnt2, int selector, BLonLinkType linkType, boolean priority, boolean debug) {
      if (pnt1.isOutput()) {
         this.hub = pnt1;
         this.targets.addElement(pnt2);
      } else {
         this.hub = pnt2;
         this.targets.addElement(pnt1);
      }

      this.debug = debug;
      this.selector = selector;
      this.linkType = linkType;
      this.priority = priority;
   }

   public int getTargetCount() {
      return this.targets.size();
   }

   public int getAddressGroup() {
      return this.addressGroup;
   }

   public BLonLinkType getLinkType() {
      return this.linkType;
   }

   public LonPoint getHub() {
      return this.hub;
   }

   public boolean isAddressChange() {
      return this.addressChange;
   }

   public int getSelector() {
      return this.selector;
   }

   public Connection getSecondary() {
      return this.secondary;
   }

   public Connection getPrimary() {
      return this.primary;
   }

   public Connection getRoot() {
      return this.root;
   }

   public boolean getPriority() {
      return this.priority;
   }

   public LonPoint[] getTargets() {
      if (this.tgtArray == null || this.tgtArray.length != this.targets.size()) {
         this.tgtArray = new LonPoint[this.targets.size()];
      }

      this.targets.copyInto(this.tgtArray);
      return this.tgtArray;
   }

   public LonPoint getTarget(int ndx) {
      return this.targets.elementAt(ndx);
   }

   public void setAddressChange(boolean addressChange) {
      this.addressChange = addressChange;
   }

   public void setAddressGroup(int addressGroup) {
      this.addressGroup = addressGroup;
   }

   public void setLinkType(BLonLinkType linkType) {
      this.linkType = linkType;
   }

   public void setHub(LonPoint newHub) {
      this.hub = newHub;
   }

   public void setSelector(int newSel) {
      this.selector = newSel;
   }

   public void setPriority(boolean pr) {
      this.priority = pr;
   }

   public void changeLinkType(BLonLinkType linkType, boolean priority) {
      this.doChangeLinkType(linkType, priority);
   }

   private void doChangeLinkType(BLonLinkType linkType, boolean priority) {
      this.linkType = linkType;
      if (this.hub != null) {
         this.hub.changeLinkType(linkType, priority);
      }

      int cnt = this.targets.size();

      for (int i = 0; i < cnt; i++) {
         this.targets.elementAt(i).changeLinkType(linkType, priority);
      }
   }

   public boolean isAcknowledged() {
      return this.linkType == BLonLinkType.critical || this.linkType == BLonLinkType.authenticated;
   }

   public boolean isAuthenticated() {
      return this.linkType == BLonLinkType.authenticated;
   }

   public int getActiveTargetCount() {
      int cnt = this.targets.size();
      int activeCnt = 0;
      boolean hasProxies = false;

      for (int i = 0; i < cnt; i++) {
         LonPoint lp = this.targets.elementAt(i);
         if (lp.isActive()) {
            if (lp.isProxy()) {
               if (hasProxies) {
                  continue;
               }

               hasProxies = true;
            }

            activeCnt++;
         }
      }

      return activeCnt;
   }

   public boolean hasActiveTarget() {
      for (int i = 0; i < this.targets.size(); i++) {
         if (this.targets.elementAt(i).isActive()) {
            return true;
         }
      }

      return false;
   }

   public LonPoint getActiveTargetNotTurnaround() {
      for (int i = 0; i < this.targets.size(); i++) {
         LonPoint lp = this.targets.elementAt(i);
         if ((this.hub == null || !this.hub.isSameNode(lp.getLonDevice())) && lp.isActive()) {
            return lp;
         }
      }

      return null;
   }

   public int discoverAddressGroup() {
      if (this.hub != null && !this.hub.isNew()) {
         int addressGroup = this.hub.getAddressGroup();
         if (addressGroup > 0) {
            return addressGroup;
         }
      }

      int cnt = this.targets.size();

      for (int i = 0; i < cnt; i++) {
         LonPoint target = this.targets.elementAt(i);
         if (!target.isNew()) {
            int addressGroup = target.getAddressGroup();
            if (addressGroup > 0) {
               return addressGroup;
            }
         }
      }

      return -1;
   }

   public boolean isNew() {
      if (this.hub != null && this.hub.isNew()) {
         return true;
      } else {
         int cnt = this.targets.size();

         for (int i = 0; i < cnt; i++) {
            if (this.targets.elementAt(i).isNew()) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isPollOnly() {
      return this.linkType != null && this.linkType == BLonLinkType.pollOnly;
   }

   public boolean isPolled() {
      return this.hub != null && this.hub.isOutput() ? this.hub.isPolled() : this.targets.size() > 0 && this.targets.elementAt(0).isPolled();
   }

   public LonPoint getPntRequiringAddressEntry() {
      if (this.hub == null) {
         return null;
      } else {
         return this.hub.requiresAddressEntry() ? this.hub : this.getActiveTargetNotTurnaround();
      }
   }

   public LonPoint getAddressPnt(boolean active) {
      if (this.hub != null && this.targets.size() != 0) {
         LonPoint tgt = this.targets.elementAt(0);
         if (this.hub.isOutput() && this.hub.isPolled() || tgt.isOutput() && !tgt.isPolled()) {
            for (int i = 0; i < this.targets.size(); i++) {
               tgt = this.targets.elementAt(i);
               if (active && tgt.isActive()) {
                  return tgt;
               }
            }

            return null;
         } else {
            return this.hub;
         }
      } else {
         return null;
      }
   }

   public void setError() {
      this.setStatus(BLonLinkStatus.error);
   }

   public void setStatus(BLonLinkStatus status) {
      if (this.hub != null) {
         this.hub.setStatus(status);
      }

      int cnt = this.targets.size();

      for (int i = 0; i < cnt; i++) {
         this.targets.elementAt(i).setStatus(status);
      }
   }

   public void setAllBoundNew() {
      if (this.hub != null) {
         this.setBoundNew(this.hub);
      }

      int cnt = this.targets.size();

      for (int i = 0; i < cnt; i++) {
         this.setBoundNew(this.targets.elementAt(i));
      }
   }

   public boolean isError() {
      if (this.hub != null && this.hub.isError()) {
         return true;
      } else {
         int cnt = this.targets.size();

         for (int i = 0; i < cnt; i++) {
            if (this.targets.elementAt(i).isError()) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isActive() {
      if (this.hub != null && this.hub.isActive()) {
         int cnt = this.targets.size();
         if (cnt == 0) {
            return false;
         } else {
            for (int i = 0; i < cnt; i++) {
               if (this.targets.elementAt(i).isActive()) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public LonPoint getSubnetNodeAddressTarget() {
      int cnt = this.targets.size();
      if (!this.hub.requiresAddressEntry()) {
         return null;
      } else {
         LonPoint singlePnt = null;

         for (int i = 0; i < cnt; i++) {
            LonPoint pnt = this.targets.elementAt(i);
            if (pnt.isActive()) {
               if (pnt.requiresAddressEntry()) {
                  return null;
               }

               if (!pnt.isSameNode(this.hub.getLonDevice())) {
                  if (singlePnt == null) {
                     singlePnt = pnt;
                  } else if (!pnt.isSameNode(singlePnt.getLonDevice())) {
                     return null;
                  }
               }
            }
         }

         return singlePnt;
      }
   }

   public boolean isTurnAroundAddressEntry() {
      if (this.hub != null && !this.hub.isLocal() && this.targets.size() >= 1) {
         BLonDevice hubDev = this.hub.getLonDevice();
         int cnt = this.targets.size();
         boolean found = false;

         for (int i = 0; i < cnt; i++) {
            LonPoint pnt = this.targets.elementAt(i);
            if (!pnt.isObsolete()) {
               if (pnt.isLocal() || !pnt.isSameNode(hubDev)) {
                  return false;
               }

               found = true;
            }
         }

         return found;
      } else {
         return false;
      }
   }

   public boolean hasTurnAround() {
      if (this.hub != null && this.targets.size() >= 1) {
         int cnt = this.targets.size();

         for (int i = 0; i < cnt; i++) {
            LonPoint pnt = this.targets.elementAt(i);
            if (!pnt.isLocal() && !pnt.isObsolete() && pnt.isSameNode(this.hub.getLonDevice())) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isLocal() {
      return this.getLocal() != null;
   }

   public LonPoint getLocal() {
      if (this.hub != null && this.hub.isLocal()) {
         return this.hub;
      } else {
         int cnt = this.targets.size();

         for (int i = 0; i < cnt; i++) {
            LonPoint t = this.targets.elementAt(i);
            if (t.isLocal()) {
               return t;
            }
         }

         return null;
      }
   }

   public void addPoint(LonPoint newPoint) {
      if (this.findPoint(newPoint) == null) {
         if (newPoint.getDirection() != BLonNvDirection.output) {
            this.targets.addElement(newPoint);
         } else if (this.hub == null) {
            this.hub = newPoint;
         } else {
            this.addSecondary(new Connection(newPoint, this.selector, this.debug));
         }
      }
   }

   public void parseTargets(GroupTable grpTbl) {
      if (this.secondary != null && this.targets.size() != 0) {
         boolean[] found = new boolean[this.targets.size()];

         for (Connection c = this.getSecondary(); c != null; c = c.getSecondary()) {
            LonPoint h = c.getHub();
            if (h != null) {
               int[] adrList = this.getAddressedList(h, grpTbl);
               if (adrList != null) {
                  for (int i = 0; i < this.targets.size(); i++) {
                     LonPoint tgt = this.targets.elementAt(i);
                     if (!tgt.isLocal() && this.isDeviceInList(tgt, adrList, grpTbl)) {
                        found[i] = true;
                        if (this.debug) {
                           System.out.println("\nCopy target to secondary:  sel=" + this.selector + "\n   " + tgt);
                        }

                        c.targets.addElement((LonPoint)tgt.cloneMe());
                     }
                  }
               }
            }
         }

         LonPoint h = this.getHub();
         if (h != null) {
            int[] adrList = this.getAddressedList(h, grpTbl);
            if (adrList != null) {
               for (int ix = this.targets.size() - 1; ix >= 0; ix--) {
                  LonPoint tgt = this.targets.elementAt(ix);
                  if (!this.isDeviceInList(tgt, adrList, grpTbl) && found[ix]) {
                     if (this.debug) {
                        System.out.println("\nDelete target from root:  sel=" + this.selector + "\n   " + tgt);
                     }

                     this.targets.removeElementAt(ix);
                  }
               }
            }
         }
      }
   }

   private boolean isDeviceInList(LonPoint pnt, int[] adrList, GroupTable grpTbl) {
      int index = grpTbl.findDeviceIndex(pnt.getLonDevice());

      for (int i = 0; i < adrList.length; i++) {
         if (adrList[i] == index) {
            return true;
         }
      }

      return false;
   }

   private int[] getAddressedList(LonPoint pnt, GroupTable grpTbl) {
      BIAddressEntry entry = pnt.getAddressEntry();
      if (entry == null) {
         return null;
      } else {
         int[] adrList = null;
         switch (entry.getAddressType().getOrdinal()) {
            case 0:
            case 3:
            default:
               break;
            case 1:
               Group grp = grpTbl.getGroup(entry.getGroupOrSubnet());
               if (grp != null) {
                  adrList = grp.getMemberIndices();
               }
               break;
            case 2:
               adrList = new int[1];
               BSubnetNode addr = BSubnetNode.make(entry.getGroupOrSubnet(), entry.getMemberOrNode());
               BLonDevice dev = grpTbl.lonworks.addressManager().getDeviceByAddress(addr);
               if (dev != null) {
                  adrList[0] = grpTbl.findDeviceIndex(dev);
               }
               break;
            case 4:
               return new int[]{grpTbl.findDeviceIndex(pnt.getLonDevice())};
         }

         if (adrList == null) {
            return null;
         } else if (!pnt.getNvConfigData().getTurnAround()) {
            return adrList;
         } else {
            int myNdx = grpTbl.findDeviceIndex(pnt.getLonDevice());
            int len = adrList.length;

            for (int i = 0; i < len; i++) {
               if (adrList[i] == myNdx) {
                  return adrList;
               }
            }

            int[] newList = new int[len + 1];
            System.arraycopy(adrList, 0, newList, 0, len);
            newList[len] = myNdx;
            return newList;
         }
      }
   }

   public void addLinkedPoint(LonPoint refPnt, LonPoint newPnt, BLonLinkType linkType, boolean priority) {
      if (refPnt.getDirection() == BLonNvDirection.output) {
         if (this.hub != null && this.hub.equals(refPnt)) {
            if (this.findTarget(newPnt) == null) {
               this.targets.addElement(newPnt);
               this.setObsoleteBound(this.hub);
            }

            this.updateLinktype(linkType);
            this.priority = priority;
         } else {
            if (this.secondary == null) {
               this.addSecondary(new Connection(refPnt, newPnt, this.selector, linkType, priority, this.debug));
            } else {
               this.secondary.addLinkedPoint(refPnt, newPnt, linkType, priority);
            }
         }
      } else {
         this.addSecondary(new Connection(refPnt, newPnt, this.selector, linkType, priority, this.debug));
      }
   }

   public void addLinkedProxy(LonPoint refPnt, LonPoint proxyPnt, BLonLinkType linkType, boolean priority) {
      LonPoint ref;
      if (refPnt.getDirection() == BLonNvDirection.output) {
         if (this.hub == null || !this.hub.equals(refPnt)) {
            if (this.secondary == null) {
               this.addSecondary(new Connection(refPnt, proxyPnt, this.selector, linkType, priority, this.debug));
            } else {
               this.secondary.addLinkedProxy(refPnt, proxyPnt, linkType, priority);
            }

            return;
         }

         ref = this.hub;
         this.targets.addElement(proxyPnt);
      } else {
         ref = this.findTarget(refPnt);
         if (this.hub != null || ref == null) {
            if (this.secondary == null) {
               this.addSecondary(new Connection(refPnt, proxyPnt, this.selector, linkType, priority, this.debug));
            } else {
               this.secondary.addLinkedProxy(refPnt, proxyPnt, linkType, priority);
            }

            return;
         }

         this.hub = proxyPnt;
      }

      this.setObsoleteBound(ref);
      this.setObsoleteBound(proxyPnt);
      this.updateLinktype(linkType);
      this.priority = priority;
   }

   public void addLink(LonPoint pnt1, LonPoint pnt2, BLonLinkType linkType, boolean priority) {
      boolean hub1 = pnt1.getDirection() == BLonNvDirection.output;
      LonPoint hubPnt = hub1 ? pnt1 : pnt2;
      LonPoint trgPnt = hub1 ? pnt2 : pnt1;
      Connection hubConn = this;

      while (hubConn != null && (hubConn.hub == null || !hubConn.hub.equals(hubPnt))) {
         hubConn = hubConn.getSecondary();
      }

      if (hubConn == null) {
         System.out.println("*** INTERNAL ERROR could not find hub connection in Connection.addLink ***\nhubPoint:" + hubPnt + "\n" + this.toString());
         hubConn = new Connection(pnt1, pnt2, this.selector, linkType, priority, this.debug);
         hubConn.setError();
         this.addSecondary(hubConn);
      } else {
         LonPoint trg = hubConn.findTarget(trgPnt);
         if (trg == null) {
            trg = this.findConnection(trgPnt).findTarget(trgPnt);
            if (trg == null) {
               trg = trgPnt;
            } else {
               trg = (LonPoint)trg.cloneMe();
            }

            hubConn.targets.addElement(trg);
         }

         this.setObsoleteBound(hubConn.hub);
         this.setObsoleteBound(trg);
         hubConn.updateLinktype(linkType);
         hubConn.priority = priority;
      }
   }

   public void removeObsoleteOverlappPnts() {
      if (this.secondary != null) {
         for (Connection c = this; c != null; c = c.secondary) {
            Vector<LonPoint> tgts = c.targets;

            for (int i = tgts.size() - 1; i >= 0; i--) {
               LonPoint tgt = tgts.elementAt(i);
               if (tgt.isObsolete()) {
                  boolean found = false;

                  for (Connection p = this.root; p != null; p = p.getSecondary()) {
                     if (p != c && p.findTarget(tgt) != null) {
                        found = true;
                        break;
                     }
                  }

                  if (found) {
                     if (this.debug) {
                        System.out.println("\nremove redundant tgt:" + tgt);
                     }

                     tgts.removeElementAt(i);
                  }
               }
            }
         }
      }
   }

   public Connection removeNoOverlappSecondary() {
      if (this.hub == null && this.targets.size() == 0) {
         return this;
      } else if (this.secondary == null) {
         return null;
      } else {
         Connection p = this;
         Connection c = this.secondary;

         boolean hasActive;
         for (hasActive = false; c != null; c = c.secondary) {
            if (c.isActive()) {
               hasActive = true;
               if (!c.hasAnyOverlappingTargets(null)) {
                  p.secondary = c.secondary;
                  c.secondary = null;
                  return c;
               }
            }

            p = c;
         }

         if (!hasActive) {
            return null;
         } else if (!this.hasAnyOverlappingTargets(null)) {
            c = this.secondary;
            this.secondary = null;
            return c;
         } else {
            return null;
         }
      }
   }

   public boolean hasAnyOverlappingTargets(LonPoint pnt) {
      Vector<LonPoint> tgts = this.targets;

      for (int i = 0; i < tgts.size(); i++) {
         LonPoint tgt = tgts.elementAt(i);
         if (pnt == null || !tgt.equals(pnt)) {
            for (Connection c = this.root; c != null; c = c.getSecondary()) {
               if (c != this && c.findTarget(tgt) != null) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean hasOverlappingTarget(LonPoint pnt, Connection tgtConn) {
      for (Connection c = this.root; c != null; c = c.getSecondary()) {
         if (c != tgtConn) {
            LonPoint tgt = c.findTarget(pnt);
            if (tgt != null && tgt.isActive()) {
               return true;
            }
         }
      }

      return false;
   }

   public void mergeConnection(Connection conn) {
      for (Connection c = conn; c != null; c = c.secondary) {
         c.selector = this.selector;
         c.root = this.root;
      }

      this.addSecondary(conn);
   }

   private void setObsoleteBound(LonPoint p) {
      if (p.getStatus() == BLonLinkStatus.obsolete) {
         p.setStatus(BLonLinkStatus.bound);
      }
   }

   private void setBoundNew(LonPoint p) {
      if (p.getStatus() == BLonLinkStatus.bound) {
         p.setStatus(BLonLinkStatus.newLink);
      }
   }

   public void addSecondary(Connection conn) {
      if (this.debug) {
         System.out.println("*** addSecondary to " + this.selector + " *** ");
      }

      if (this.secondary == null) {
         this.secondary = conn;
         conn.root = this.root;
         conn.selector = this.selector;
      } else {
         this.secondary.addSecondary(conn);
      }
   }

   public void removeSecondary(Connection conn) {
      if (this.debug) {
         System.out.println("*** removeSecondary from " + this.selector + " *** ");
      }

      if (this.secondary == conn) {
         this.secondary = conn.secondary;
         conn.root = conn;
         conn.secondary = null;
      } else if (this.secondary != null) {
         this.secondary.removeSecondary(conn);
      }
   }

   public Connection removeAllSecondaries() {
      Connection newRoot = this.secondary;
      this.secondary = null;

      for (Connection c = newRoot; c != null; c = c.getSecondary()) {
         c.root = newRoot;
      }

      return newRoot;
   }

   public void newSelector(int selector) {
      for (Connection c = this; c != null; c = c.secondary) {
         c.selector = selector;
         c.root = this;
         c.setAllBoundNew();
      }
   }

   public LonPoint findPoint(int hashCode) {
      if (this.hub != null && this.hub.hashCode() == hashCode) {
         return this.hub;
      } else {
         int cnt = this.targets.size();

         for (int i = 0; i < cnt; i++) {
            LonPoint tgt = this.targets.elementAt(i);
            if (tgt.hashCode() == hashCode) {
               return tgt;
            }
         }

         return null;
      }
   }

   public LonPoint findPoint(LonPoint pnt) {
      return this.hub != null && this.hub.equals(pnt) ? this.hub : this.findTarget(pnt);
   }

   public Connection findConnection(LonPoint pnt) {
      return this.findConnection(pnt, false);
   }

   public Connection findConnection(LonPoint pnt, boolean leastTargets) {
      Connection c = this;
      Connection r = null;

      for (int rCnt = 0; c != null; c = c.getSecondary()) {
         if (c.findPoint(pnt) != null) {
            if (!leastTargets) {
               return c;
            }

            int cCnt = c.getActiveTargetCount();
            if (r == null || cCnt < rCnt) {
               r = c;
               rCnt = cCnt;
            }
         }
      }

      return r;
   }

   public boolean okayToRemove(LonPoint pnt) {
      for (Connection c = this; c != null; c = c.getSecondary()) {
         LonPoint p = c.findPoint(pnt);
         if (p != null && !p.isObsolete()) {
            return false;
         }
      }

      return true;
   }

   public boolean primaryOverlappsSelector(int sel) {
      if (this.primary.selector == sel) {
         return true;
      } else {
         Vector<Connection> a = this.primary.aliasConnections;

         for (int i = 0; i < a.size(); i++) {
            if (a.elementAt(i).selector == sel) {
               return true;
            }
         }

         return false;
      }
   }

   public Connection findAliasPrimary(int primNvHash) {
      for (Connection c = this; c != null; c = c.getSecondary()) {
         if (c.hub != null && c.hub.hashCode() == primNvHash) {
            return c;
         }
      }

      return null;
   }

   public void finalRemove() {
      if (this.primary != null) {
         this.primary.aliasConnections.removeElement(this);
      }
   }

   public int countPoints() {
      int cnt = 0;

      for (Connection c = this; c != null; c = c.getSecondary()) {
         if (c.hub != null) {
            cnt++;
         }

         cnt += this.targets.size();
      }

      return cnt;
   }

   private LonPoint findTarget(LonPoint pnt) {
      int cnt = this.targets.size();

      for (int i = 0; i < cnt; i++) {
         if (this.targets.elementAt(i).equals(pnt)) {
            return this.targets.elementAt(i);
         }
      }

      return null;
   }

   public LonPoint findAliasPoint(LonPoint pnt) {
      return this.hub != null && this.hub.isAliasPoint() && pnt.hashCode() == ((LonPointAlias)this.hub).primaryHashCode() ? this.hub : null;
   }

   boolean containsDevice(BLonDevice dev) {
      if (this.hub != null && this.hub.isSameNode(dev)) {
         return true;
      } else {
         int cnt = this.targets.size();

         for (int i = 0; i < cnt; i++) {
            LonPoint pnt = this.targets.elementAt(i);
            if (pnt.isSameNode(dev)) {
               return true;
            }
         }

         return this.getSecondary() != null ? this.getSecondary().containsDevice(dev) : false;
      }
   }

   public LonPoint[] findAppDevice(BLonDevice dev) {
      return this.scanForDevice(dev, false);
   }

   public LonPoint[] findAppDeviceInTargets(BLonDevice dev) {
      return this.scanForDevice(dev, true);
   }

   private LonPoint[] scanForDevice(BLonDevice dev, boolean targetsOnly) {
      Vector<LonPoint> v = new Vector<>();
      if (!targetsOnly && this.hub != null && !this.hub.isObsolete() && this.hub.isSameNode(dev)) {
         v.addElement(this.hub);
      }

      int cnt = this.targets.size();

      for (int i = 0; i < cnt; i++) {
         LonPoint pnt = this.targets.elementAt(i);
         if (!pnt.isObsolete() && pnt.isSameNode(dev)) {
            v.addElement(pnt);
         }
      }

      LonPoint[] a = new LonPoint[v.size()];
      v.copyInto(a);
      return a;
   }

   public void removePoint(LonPoint pnt) {
      if (this.hub != null && this.hub.equals(pnt)) {
         this.hub = null;
      } else {
         int cnt = this.targets.size();

         for (int i = 0; i < cnt; i++) {
            if (this.targets.elementAt(i).equals(pnt)) {
               this.targets.removeElementAt(i);
               break;
            }
         }
      }
   }

   public boolean removeEmpty() {
      for (Connection s = this.secondary; s != null; s = s.secondary) {
         if (s.isEmpty()) {
            this.removeSecondary(s);
            s.finalRemove();
         }
      }

      if (this.isEmpty()) {
         this.finalRemove();
         return true;
      } else {
         return false;
      }
   }

   private boolean isEmpty() {
      return this.hub == null && this.targets.size() == 0;
   }

   void clearAddressEntry() {
      this.setAddressChange(true);
      this.setAddressGroup(-1);
      this.getHub().setAddressIndex(-1);
      this.getHub().setStatus(BLonLinkStatus.newLink);
   }

   public void verifyConnection() {
      if (this.isActive()) {
         LonPoint lp = this.getAddressPnt(true);
         if (lp != null) {
            int group = lp.getAddressGroup();
            if (this.debug) {
               System.out.println(" set group# " + group);
            }

            this.setAddressGroup(group);
         }

         if (this.isAcknowledged() && this.getActiveTargetCount() > 5) {
            this.setStatus(BLonLinkStatus.maxCritError);
         } else {
            if (this.hub != null && !this.hub.validatePoint(this.linkType, this.priority, this.debug, false) && this.debug) {
               System.out.println(" changed status " + this.hub);
            }

            int cnt = this.targets.size();
            if (cnt != 0) {
               LonPoint[] tgts = new LonPoint[cnt];
               this.targets.copyInto(tgts);
               boolean forceEvalDscr = this.hub != null && (this.hub.isProxy() || this.hub.isPseudo());

               for (int i = 0; i < cnt; i++) {
                  lp = tgts[i];
                  if (!lp.validatePoint(this.linkType, this.priority, this.debug, forceEvalDscr) && this.debug) {
                     System.out.println(" changed status  " + lp);
                  }
               }

               if (this.hub != null && (this.hub.isLocal() || this.hub.isProxy())) {
                  lp = tgts[0];
                  int snvtType = lp.getSnvtType();

                  for (int ix = 1; ix < cnt; ix++) {
                     lp = tgts[ix];
                     if (lp.getSnvtType() != snvtType) {
                        lp.setStatus(BLonLinkStatus.nvTypeError);
                     }
                  }
               }

               if (this.hub != null && this.hub.addressIndexIsLocal()) {
                  for (int ixx = 0; ixx < cnt; ixx++) {
                     lp = tgts[ixx];
                     if (lp.isLocal() && lp.isNew()) {
                        lp.setStatus(BLonLinkStatus.bound);
                     }
                  }
               }
            }
         }
      }
   }

   public boolean canHaveAlias() {
      return this.hub != null
            && !this.hub.isAliasPoint()
            && this.hub.requiresAddressEntry()
            && this.hub.getLonDevice().getDeviceData().getAliasTable().getAliasCount() != 0
         ? this.targets.size() >= 2 || this.aliasConnections != null
         : false;
   }

   public boolean isAliasConnection() {
      return this.hub != null && this.hub.isAliasPoint();
   }

   public boolean hasAliasConnection() {
      return this.aliasConnections != null && this.aliasConnections.size() > 0;
   }

   public boolean containsAliasConnection() {
      for (Connection c = this; c != null; c = c.secondary) {
         if (c.isAliasConnection()) {
            return true;
         }
      }

      return false;
   }

   public Connection[] getAliasConnections() {
      if (this.aliasConnections != null && this.aliasConnections.size() != 0) {
         Connection[] a = new Connection[this.aliasConnections.size()];
         this.aliasConnections.copyInto(a);
         return a;
      } else {
         return null;
      }
   }

   public Connection getActiveAliasConnection() {
      if (this.aliasConnections != null && this.aliasConnections.size() != 0) {
         for (int i = 0; i < this.aliasConnections.size(); i++) {
            Connection c = this.aliasConnections.elementAt(i);
            if (c.isActive()) {
               return c;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public boolean okToMerge(Connection con) {
      Vector<LonPoint> myPnts = this.getAliasSearchList();
      Vector<LonPoint> hisPnts = con.getAliasSearchList();

      for (int i = 0; i < myPnts.size(); i++) {
         LonPoint myPnt = myPnts.elementAt(i);

         for (int j = 0; j < hisPnts.size(); j++) {
            if (hisPnts.elementAt(j).isSameDevicePoint(myPnt)) {
               return false;
            }
         }
      }

      return true;
   }

   public Vector<LonPoint> getAllSameDevicePoints() {
      Vector<LonPoint> v = new Vector<>();
      Vector<LonPoint> pnts = this.getAliasSearchList();
      int cnt = pnts.size();

      for (int i = 0; i < cnt; i++) {
         LonPoint p1 = pnts.elementAt(i);
         p1.overlappCnt = 0;

         for (int j = i + 1; j < cnt; j++) {
            LonPoint p2 = pnts.elementAt(j);
            if (p2.equals(p1)) {
               p1.overlappCnt++;
            } else if (p2.isSameDevicePoint(p1)) {
               boolean ovrLap = false;

               for (int n = 0; n < v.size(); n++) {
                  if (p2.equals(v.elementAt(n))) {
                     v.elementAt(n).overlappCnt++;
                     ovrLap = true;
                     break;
                  }
               }

               if (!ovrLap) {
                  p2.overlappCnt = 0;
                  v.addElement(p2);
               }
            }
         }

         if (v.size() > 0) {
            v.addElement(p1);
            break;
         }
      }

      if (this.debug) {
         for (int i = 0; i < v.size(); i++) {
            System.out.println("overLapps=" + v.elementAt(i).overlappCnt + "  " + v.elementAt(i));
         }
      }

      return v;
   }

   private Vector<LonPoint> getAliasSearchList() {
      Vector<LonPoint> v = new Vector<>();

      for (Connection c = this; c != null; c = c.getSecondary()) {
         Vector<LonPoint> tgts = c.targets;
         int cnt = tgts.size();

         for (int i = 0; i < cnt; i++) {
            LonPoint a = tgts.elementAt(i);
            if (a.isActive()) {
               v.addElement(a);
            }
         }
      }

      return v;
   }

   public LonPoint[] getAliasPointsToCompress(Connection aliasCon) {
      Vector<LonPoint> v = new Vector<>();
      LonPoint[] aPnts = aliasCon.getTargets();

      for (int i = 0; i < aPnts.length; i++) {
         LonPoint a = aPnts[i];
         if (a.isActive() && !aliasCon.hasSameDevicePoints(a) && !this.hasSameDevicePoints(a)) {
            v.addElement(a);
         }
      }

      if (v.size() == 0) {
         return null;
      } else {
         LonPoint[] pnts = new LonPoint[v.size()];
         v.copyInto(pnts);
         return pnts;
      }
   }

   private boolean hasSameDevicePoints(LonPoint a) {
      boolean found = false;

      for (Connection c = this.root; c != null; c = c.getSecondary()) {
         Vector<LonPoint> t = c.targets;

         for (int j = 0; j < t.size(); j++) {
            LonPoint p = t.elementAt(j);
            if (p.equals(a)) {
               if (found) {
                  return true;
               }

               found = true;
            }

            if (p.isActive() && p.isSameDevicePoint(a)) {
               return true;
            }
         }
      }

      return false;
   }

   public Connection findAliasConnectionForPoint(LonPoint pnt, int selector, boolean useSel) {
      if (this.aliasConnections == null) {
         return null;
      } else {
         for (int i = 0; i < this.aliasConnections.size(); i++) {
            Connection c = this.aliasConnections.elementAt(i);
            if ((!useSel || c.getSelector() == selector) && !c.hasSameDevicePoints(pnt)) {
               return c;
            }
         }

         return null;
      }
   }

   public LonPoint[] getSameDevicePoints() {
      if (this.hub == null) {
         return null;
      } else {
         Vector<LonPoint> v = new Vector<>();
         int cnt = this.targets.size();
         boolean foundSelector = false;

         for (int i = 0; i < cnt; i++) {
            LonPoint a = this.targets.elementAt(i);
            if (!a.isLocal() && a.isActive()) {
               if (!foundSelector && a.isBound() && a.getOrigSelector() == this.selector) {
                  foundSelector = true;
               }

               for (int j = i + 1; j < cnt; j++) {
                  LonPoint b = this.targets.elementAt(j);
                  if (b.isActive() && a.getLonDevice() == b.getLonDevice()) {
                     if (foundSelector) {
                        v.addElement(b);
                     } else {
                        v.addElement(a);
                     }
                     break;
                  }
               }
            }
         }

         if (v.size() == 0) {
            return null;
         } else {
            LonPoint[] pnts = new LonPoint[v.size()];
            v.copyInto(pnts);
            return pnts;
         }
      }
   }

   public void addAliasConnection(Connection con) {
      if (this.aliasConnections == null) {
         this.aliasConnections = new Vector<>();
      }

      if (!this.aliasConnections.contains(con)) {
         con.primary = this;
         this.aliasConnections.addElement(con);
      }
   }

   public void removeAliasConnection(Connection con) {
      if (this.aliasConnections != null) {
         this.aliasConnections.removeElement(con);
      }
   }

   public Connection findAliasConnection(Connection con, LonPoint refPnt) {
      Connection c = con.findAliasConnection(refPnt);
      if (c == null) {
         return null;
      } else {
         return this.hasAliasConnection(c) ? c : null;
      }
   }

   public Connection findAliasConnection(LonPoint refPnt) {
      for (Connection c = this; c != null; c = c.getSecondary()) {
         if (c.findAliasPoint(refPnt) != null) {
            return c;
         }
      }

      return null;
   }

   public boolean hasAliasConnection(Connection c) {
      for (Connection p = this; p != null; p = p.getSecondary()) {
         if (p.aliasConnections != null && p.aliasConnections.contains(c)) {
            return true;
         }
      }

      return false;
   }

   public Connection getLocalAlias() {
      if (this.aliasConnections == null) {
         return null;
      } else {
         for (int i = 0; i < this.aliasConnections.size(); i++) {
            Connection con = this.aliasConnections.elementAt(i);
            if (con != null && con.isLocal()) {
               return con;
            }
         }

         return null;
      }
   }

   public GroupBitSet getGroupBitSet(BLonDevice[] devs) {
      if (this.groupBitSet == null) {
         this.groupBitSet = new GroupBitSet(devs, this);
      }

      return this.groupBitSet;
   }

   public GroupBitSet getExcludeBitSet(BLonDevice[] devs) {
      if (!this.foundExBitSet) {
         if (this.root != this || this.secondary != null) {
            this.excludeBits = new GroupBitSet(this.getTotalTargetBitSet(devs));
            this.excludeBits.xor(this.getTargetBitSet(devs));
            if (this.hub != null) {
               this.excludeBits.clear(GroupBitSet.findDeviceNdx(devs, this.hub.getLonDevice()));
            }

            if (this.excludeBits.getGroupSize() == 0) {
               this.excludeBits = null;
            }
         }

         this.foundExBitSet = true;
      }

      return this.excludeBits;
   }

   private GroupBitSet getTotalTargetBitSet(BLonDevice[] devs) {
      if (this.root != this) {
         return this.root.getTotalTargetBitSet(devs);
      } else {
         if (this.targetBitSet == null) {
            this.targetBitSet = this.getTargetBitSet(devs);
            if (this.secondary != null) {
               for (Connection c = this.secondary; c != null; c = c.getSecondary()) {
                  if (c.isActive()) {
                     this.targetBitSet.or(c.getTargetBitSet(devs));
                  }
               }
            }
         }

         return this.targetBitSet;
      }
   }

   private GroupBitSet getTargetBitSet(BLonDevice[] devs) {
      return new GroupBitSet(devs, this.getTargets());
   }

   @Override
   public String toString() {
      return this.toString(true);
   }

   public String toString(boolean includeOthers) {
      StringBuilder sb = new StringBuilder();
      sb.append(" Connection =>");
      Connection c = this;

      while (c != null) {
         c.doToString(sb, includeOthers);
         c = includeOthers ? c.getSecondary() : null;
         if (c != null) {
            sb.append("\n  Secondary :");
         }
      }

      return sb.toString();
   }

   private void doToString(StringBuilder sb, boolean includeOthers) {
      sb.append(" linkType = ").append(this.linkType);
      sb.append(" selector = ").append(this.selector);
      if (this.addressGroup != -1) {
         sb.append(" addressGroup = ").append(this.addressGroup);
      }

      sb.append("\n   Hub = ").append(this.hub);
      int numTargets = this.targets.size();
      if (numTargets == 0) {
         sb.append("\n   no targets");
      }

      for (int i = 0; i < numTargets; i++) {
         sb.append("\n   target #").append(i).append(" ").append(this.targets.elementAt(i));
      }

      if (includeOthers && this.aliasConnections != null) {
         for (int i = 0; i < this.aliasConnections.size(); i++) {
            Connection con = this.aliasConnections.elementAt(i);
            if (con != null) {
               sb.append("\n  Alias ").append(con.toString(false));
            }
         }
      }
   }

   String identify() {
      return this.hub != null ? this.hub.getSummaryString() : "";
   }

   private void updateLinktype(BLonLinkType lt) {
      if (this.linkType.getOrdinal() < lt.getOrdinal()) {
         this.linkType = lt;
      }
   }
}
