package com.tridium.lonworks.util;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.datatypes.BLonRouteTable;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.netmessages.QueryStatusResponse;
import com.tridium.lonworks.netmessages.RouterStatusRequest;
import com.tridium.lonworks.netmessages.RouterStatusResponse;
import com.tridium.lonworks.netmessages.SetRouterModeRequest;
import com.tridium.lonworks.netmessages.TableClear;
import com.tridium.lonworks.netmessages.TableDownload;
import com.tridium.lonworks.netmessages.TableReportRequest;
import com.tridium.lonworks.netmessages.TableReportResponse;
import com.tridium.lonworks.netmessages.UpdateDomainRequest;
import com.tridium.lonworks.netmessages.WriteMemRequest;
import com.tridium.lonworks.netmgmt.BLonNetmgmt;
import com.tridium.lonworks.netmgmt.NetMgmtConst;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAuthenticationKey;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonRepeatTimer;

public class RouterUtil implements NetMessages, NetMgmtConst {
   public static void setTemporaryBridge(BLonNetwork lon) {
      BLonRouter[] rtrs = lon.getLonRouters();
      boolean found = true;

      while (found) {
         found = false;

         for (int i = 0; i < rtrs.length; i++) {
            if (rtrs[i] != null) {
               try {
                  setRouterMode(rtrs[i], BLonRouterMode.temporaryBridge);
                  found = true;
                  rtrs[i] = null;
               } catch (Throwable var5) {
               }
            }
         }
      }
   }

   public static void clearTemporaryBridge(BLonNetwork lon) {
      BLonRouter[] rtrs = lon.addressManager().getRouterList();

      for (int i = 0; i < rtrs.length; i++) {
         try {
            setRouterMode(rtrs[i], rtrs[i].getRouterMode());
         } catch (Throwable var4) {
         }
      }
   }

   public static void setRouterType(BLonRouter rtr, BLonRouterType type) throws LonException {
      setRouterType(rtr.lonComm(), rtr.getNeuronIdAddress(), type, rtr.getNearDeviceData().getAuthenticate(), false);
      setRouterType(rtr.lonComm(), rtr.getNeuronIdAddress(), type, rtr.getNearDeviceData().getAuthenticate(), true);
   }

   public static void setRouterType(LonComm lonComm, LonAddress destAddr, BLonRouterType type, boolean auth, boolean farSide) throws LonException {
      byte[] a = new byte[]{(byte)type.getOrdinal()};
      WriteMemRequest req = new WriteMemRequest(2, 55, 1, 4, a);
      req.setAuthenticate(auth);
      if (farSide) {
         req.setFarSide();
      }

      req.setTransmitTimer(BLonRepeatTimer.milliSec256);
      lonComm.sendRequest(destAddr, req);
   }

   public static void setRouterMode(BLonRouter rtr, BLonRouterMode mode) throws LonException {
      setRouterMode(rtr.lonComm(), rtr.getNeuronIdAddress(), mode, rtr.getNearDeviceData().getAuthenticate());
   }

   public static void setRouterMode(LonComm lonComm, LonAddress destAddr, BLonRouterMode mode, boolean auth) throws LonException {
      SetRouterModeRequest req = new SetRouterModeRequest(mode.getOrdinal());
      req.setAuthenticate(auth);
      req.setTransmitTimer(BLonRepeatTimer.milliSec256);
      lonComm.sendRequest(destAddr, req);
   }

   public static BLonRouterType getRouterType(BLonRouter rtr, boolean farSide) throws LonException {
      int type = getRouterStatus(rtr, farSide).getType();
      return BLonRouterType.make(type);
   }

   public static BLonRouterMode getRouterMode(BLonRouter rtr, boolean farSide) throws LonException {
      int mode = getRouterStatus(rtr, farSide).getMode();
      return BLonRouterMode.make(mode);
   }

   public static void uploadTypeAndMode(BLonRouter rtr) throws LonException {
      RouterStatusResponse status = getRouterStatus(rtr, false);
      rtr.set(BLonRouter.routerType, BLonRouterType.make(status.getType()), BLonNetwork.lonNoWrite);
      rtr.set(BLonRouter.routerMode, BLonRouterMode.make(status.getMode()), BLonNetwork.lonNoWrite);
   }

   public static RouterStatusResponse getRouterStatus(BLonRouter rtr, boolean farSide) throws LonException {
      return getRouterStatus(rtr.lonComm(), rtr.getNeuronIdAddress(), rtr.getNearDeviceData().getAuthenticate(), farSide);
   }

   public static RouterStatusResponse getRouterStatus(LonComm lonComm, LonAddress destAddr, boolean auth, boolean farSide) throws LonException {
      RouterStatusRequest req = new RouterStatusRequest();
      if (farSide) {
         req.setFarSide();
      }

      req.setAuthenticate(auth);
      req.setTransmitTimer(BLonRepeatTimer.milliSec256);
      return (RouterStatusResponse)lonComm.sendRequest(destAddr, req);
   }

   public static void uploadRouterTables(BLonRouter rtr) throws LonException {
      LonAddress destAddr = rtr.getNeuronIdAddress();
      LonComm lonComm = rtr.lonComm();
      boolean auth = rtr.getNearDeviceData().getAuthenticate();
      QueryStatusResponse status = NmUtil.queryStatus(lonComm, destAddr, 1, auth, false);
      if (status.versionNumber >= 5) {
         int dom = rtr.getNearDeviceData().getWorkingDomain();
         byte[] tab = getRouterTable(lonComm, destAddr, dom, 1, 0, auth, false);
         rtr.setNearGroupTable(BLonRouteTable.make(tab));
         tab = getRouterTable(lonComm, destAddr, dom, 0, 0, auth, false);
         rtr.setNearSubnetTable(BLonRouteTable.make(tab));
         tab = getRouterTable(lonComm, destAddr, dom, 1, 0, auth, true);
         rtr.setFarGroupTable(BLonRouteTable.make(tab));
         tab = getRouterTable(lonComm, destAddr, dom, 0, 0, auth, true);
         rtr.setFarSubnetTable(BLonRouteTable.make(tab));
      }
   }

   public static byte[] getRouterTable(LonComm lonComm, LonAddress destAddr, int domainIndex, int groupOrSubnet, int ramOrEeprom, boolean auth, boolean farSide) throws LonException {
      byte[] table = new byte[32];

      for (int i = 0; i < 4; i++) {
         TableReportRequest req = new TableReportRequest(groupOrSubnet, domainIndex, ramOrEeprom, i);
         if (farSide) {
            req.setFarSide();
         }

         req.setAuthenticate(auth);
         req.setTransmitTimer(BLonRepeatTimer.milliSec256);
         TableReportResponse rsp = (TableReportResponse)lonComm.sendRequest(destAddr, req);
         System.arraycopy(rsp.getTable(), 0, table, 8 * i, 8);
      }

      return table;
   }

   public static void downloadRouterTables(BLonRouter rtr) throws LonException {
      LonAddress destAddr = rtr.getNeuronIdAddress();
      boolean auth = rtr.getNearDeviceData().getAuthenticate();
      int dom = rtr.getNearDeviceData().getWorkingDomain();
      LonComm lonComm = rtr.lonComm();
      downloadTable(lonComm, destAddr, dom, 1, auth, false, rtr.getNearGroupTable());
      downloadTable(lonComm, destAddr, dom, 0, auth, false, rtr.getNearSubnetTable());
      downloadTable(lonComm, destAddr, dom, 1, auth, true, rtr.getFarGroupTable());
      downloadTable(lonComm, destAddr, dom, 0, auth, true, rtr.getFarSubnetTable());
      setRouterMode(rtr, BLonRouterMode.initRouterTable);
      setRouterMode(rtr, BLonRouterMode.normal);
   }

   public static void downloadTable(LonComm lonComm, LonAddress destAddr, int domainIndex, int groupOrSubnet, boolean auth, boolean farSide, BLonRouteTable tab) throws LonException {
      for (int i = 0; i < 4; i++) {
         byte[] section = tab.getSection(i);
         TableDownload req = new TableDownload(groupOrSubnet, domainIndex, i, section);
         if (farSide) {
            req.setFarSide();
         }

         req.setAuthenticate(auth);
         req.setTransmitTimer(BLonRepeatTimer.milliSec256);
         lonComm.sendRequest(destAddr, req);
      }
   }

   public static void updateTable(BLonRouter rtr, int groupOrSubnet, boolean farSide, BLonRouteTable newTab, BLonRouteTable origTab) throws LonException {
      LonAddress destAddr = rtr.getNeuronIdAddress();
      boolean auth = rtr.getNearDeviceData().getAuthenticate();
      int domainIndex = rtr.getNearDeviceData().getWorkingDomain();

      for (int i = 0; i < 4; i++) {
         if (!origTab.sectionsEqual(newTab, i)) {
            byte[] section = newTab.getSection(i);
            TableDownload req = new TableDownload(groupOrSubnet, domainIndex, i, section);
            if (farSide) {
               req.setFarSide();
            }

            req.setAuthenticate(auth);
            req.setTransmitTimer(BLonRepeatTimer.milliSec256);
            rtr.lonComm().sendRequest(destAddr, req);
         }
      }
   }

   public static void clearRouterTables(BLonRouter rtr) throws LonException {
      LonAddress destAddr = rtr.getNeuronIdAddress();
      boolean auth = rtr.getNearDeviceData().getAuthenticate();
      int domainIndex = rtr.getNearDeviceData().getWorkingDomain();
      LonComm lonComm = rtr.lonComm();
      clearTable(lonComm, destAddr, domainIndex, 1, auth, false);
      clearTable(lonComm, destAddr, domainIndex, 0, auth, false);
      clearTable(lonComm, destAddr, domainIndex, 1, auth, true);
      clearTable(lonComm, destAddr, domainIndex, 0, auth, true);
      rtr.setNearGroupTable(BLonRouteTable.DEFAULT);
      rtr.setNearSubnetTable(BLonRouteTable.DEFAULT);
      rtr.setFarGroupTable(BLonRouteTable.DEFAULT);
      rtr.setFarSubnetTable(BLonRouteTable.DEFAULT);
   }

   public static void clearTable(LonComm lonComm, LonAddress destAddr, int domainIndex, int groupOrSubnet, boolean auth, boolean farSide) throws LonException {
      for (int i = 0; i < 4; i++) {
         TableClear req = new TableClear(groupOrSubnet, domainIndex, i);
         if (farSide) {
            req.setFarSide();
         }

         req.setAuthenticate(auth);
         req.setTransmitTimer(BLonRepeatTimer.milliSec256);
         lonComm.sendRequest(destAddr, req);
      }
   }

   public static void updateDomainTable(BLonNetmgmt nm, BLonRouter rtr) throws LonException {
      boolean auth = rtr.getNearDeviceData().getAuthenticate();
      BDomainId domain = nm.getDomainId();
      BAuthenticationKey key = nm.getAuthenticationKey();
      LonAddress destAdr = rtr.getNeuronIdAddress();
      LonComm lonComm = rtr.lonComm();
      BDeviceData dd = rtr.getNearDeviceData();
      int workingDomain = dd.getWorkingDomain();
      int otherDomain = workingDomain == 0 ? 1 : 0;
      boolean twoDomains = dd.getTwoDomains();
      int subnet = dd.getSubnetNodeId().getSubnetId();
      int nodeId = dd.getSubnetNodeId().getNodeId();
      UpdateDomainRequest updateDomain = new UpdateDomainRequest(workingDomain, domain.getDomainId(), subnet, nodeId, domain.getLength(), key.getByteArray());
      updateDomain.setAuthenticate(auth);
      updateDomain.setTransmitTimer(BLonRepeatTimer.milliSec256);
      lonComm.sendRequest(destAdr, updateDomain);
      if (twoDomains) {
         if (domain.isZeroLength()) {
            updateDomain = new UpdateDomainRequest(otherDomain);
         } else {
            updateDomain = new UpdateDomainRequest(otherDomain, new byte[6], subnet, nodeId, 0, key.getByteArray());
         }

         updateDomain.setAuthenticate(auth);
         updateDomain.setTransmitTimer(BLonRepeatTimer.milliSec256);
         lonComm.sendRequest(destAdr, updateDomain);
      }

      dd = rtr.getFarDeviceData();
      subnet = dd.getSubnetNodeId().getSubnetId();
      nodeId = dd.getSubnetNodeId().getNodeId();
      updateDomain = new UpdateDomainRequest(workingDomain, domain.getDomainId(), subnet, nodeId, domain.getLength(), key.getByteArray());
      updateDomain.setFarSide();
      updateDomain.setAuthenticate(auth);
      updateDomain.setTransmitTimer(BLonRepeatTimer.milliSec256);
      lonComm.sendRequest(destAdr, updateDomain);
      if (twoDomains) {
         if (domain.isZeroLength()) {
            updateDomain = new UpdateDomainRequest(otherDomain);
         } else {
            updateDomain = new UpdateDomainRequest(otherDomain, new byte[6], subnet, nodeId, 0, key.getByteArray());
         }

         updateDomain.setFarSide();
         updateDomain.setAuthenticate(auth);
         updateDomain.setTransmitTimer(BLonRepeatTimer.milliSec256);
         lonComm.sendRequest(destAdr, updateDomain);
      }
   }
}
