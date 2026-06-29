package com.tridium.lonworks.util;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.NetMessageReceiver;
import com.tridium.lonworks.datatypes.BLinkDescriptor;
import com.tridium.lonworks.device.DeviceFacets;
import com.tridium.lonworks.loncomm.NAppBuffer;
import com.tridium.lonworks.loncomm.NLonComm;
import com.tridium.lonworks.netmessages.CapacityInfoRequest;
import com.tridium.lonworks.netmessages.ChecksumRecalcRequest;
import com.tridium.lonworks.netmessages.ClearStatus;
import com.tridium.lonworks.netmessages.ExEnumerateAddressRequest;
import com.tridium.lonworks.netmessages.ExEnumerateAliasRequest;
import com.tridium.lonworks.netmessages.ExEnumerateDomainRequest;
import com.tridium.lonworks.netmessages.ExEnumerateNvConfigRequest;
import com.tridium.lonworks.netmessages.ExInitRequest;
import com.tridium.lonworks.netmessages.ExUpdateAddressReq;
import com.tridium.lonworks.netmessages.ExUpdateAliasRequest;
import com.tridium.lonworks.netmessages.ExUpdateDomainRequest;
import com.tridium.lonworks.netmessages.ExUpdateNvConfigRequest;
import com.tridium.lonworks.netmessages.FetchNvRequest;
import com.tridium.lonworks.netmessages.FetchNvResponse;
import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.netmessages.NoDataResponse;
import com.tridium.lonworks.netmessages.QueryAddrRequest;
import com.tridium.lonworks.netmessages.QueryAddrResponse;
import com.tridium.lonworks.netmessages.QueryAliasRequest;
import com.tridium.lonworks.netmessages.QueryAliasResponse;
import com.tridium.lonworks.netmessages.QueryDomainRequest;
import com.tridium.lonworks.netmessages.QueryDomainResponse;
import com.tridium.lonworks.netmessages.QueryIdRequest;
import com.tridium.lonworks.netmessages.QueryIdResponse;
import com.tridium.lonworks.netmessages.QueryNvConfigRequest;
import com.tridium.lonworks.netmessages.QueryNvConfigResponse;
import com.tridium.lonworks.netmessages.QueryStatusRequest;
import com.tridium.lonworks.netmessages.QueryStatusResponse;
import com.tridium.lonworks.netmessages.RespondToQueryRequest;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.netmessages.SetNodeModeRequest;
import com.tridium.lonworks.netmessages.UnprocessedNV;
import com.tridium.lonworks.netmessages.UpdateAddress;
import com.tridium.lonworks.netmessages.UpdateAliasRequest;
import com.tridium.lonworks.netmessages.UpdateDomainRequest;
import com.tridium.lonworks.netmessages.UpdateNvConfigRequest;
import com.tridium.lonworks.netmessages.WinkRequest;
import com.tridium.lonworks.netmgmt.BLonNetmgmt;
import com.tridium.lonworks.netmgmt.NetMgmtConst;
import java.util.Vector;
import java.util.logging.Level;
import javax.baja.driver.BIDeviceFolder;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BLonObject;
import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BAuthenticationKey;
import javax.baja.lonworks.datatypes.BBroadcast;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BImplicit;
import javax.baja.lonworks.datatypes.BLocal;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonConfigSourceEnum;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.londata.BLonEnum;
import javax.baja.nre.util.Array;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Type;

public class NmUtil implements NetMessages, NetMgmtConst {
   public static final BFacets showSecs = BFacets.make("showSeconds", BBoolean.TRUE);
   public static final BFacets showMilliSecs = BFacets.make("showSeconds", BBoolean.TRUE, "showMilliseconds", BBoolean.TRUE);

   public static void setDeviceState(BLonDevice dev, BLonNodeState state) throws LonException {
      setDeviceState(dev.lonComm(), getSendAddress(dev), state, dev.isLocal(), dev.getDeviceData().getAuthenticate(), false);
   }

   public static void setDeviceState(BLonRouter rtr, BLonNodeState state, boolean farSide) throws LonException {
      setDeviceState(rtr.lonComm(), getSendAddress(rtr), state, false, rtr.getNearDeviceData().getAuthenticate(), farSide);
   }

   public static void setDeviceState(LonComm lonComm, LonAddress destAddr, BLonNodeState state, boolean isLocal, boolean auth, boolean farSide) throws LonException {
      if (state != BLonNodeState.unknown) {
         boolean done = false;
         int passCount = 0;

         BLonNodeState newState;
         for (newState = null; !done; passCount++) {
            int nStatus;
            try {
               nStatus = queryStatus(lonComm, destAddr, 1, auth, farSide).nodeState;
            } catch (LonException var14) {
               nStatus = 0;
            }

            if (state == BLonNodeState.unconfigured) {
               if (nStatus == 2) {
                  return;
               }

               try {
                  setNodeState(lonComm, destAddr, 2, auth, farSide);
                  if (isLocal) {
                     return;
                  }
               } catch (LonException var16) {
                  SetNodeModeRequest setMode = new SetNodeModeRequest(0);
                  if (farSide) {
                     setMode.setFarSide();
                  }

                  lonComm.sendAcked(destAddr, setMode);
                  int cnt = 2;

                  for (int var20 = queryStatus(lonComm, destAddr, 1, auth, farSide).nodeState;
                     (var20 & 15) != 12 && cnt-- > 0;
                     var20 = queryStatus(lonComm, destAddr, 1, auth, farSide).nodeState
                  ) {
                     wait(100);
                  }

                  setNodeState(lonComm, destAddr, 2, auth, farSide);
               }
            } else if (state == BLonNodeState.applicationless) {
               if (nStatus == 3) {
                  return;
               }

               try {
                  setNodeState(lonComm, destAddr, 3, auth, farSide);
                  if (isLocal) {
                     return;
                  }
               } catch (LonException var15) {
                  SetNodeModeRequest setMode = new SetNodeModeRequest(0);
                  if (farSide) {
                     setMode.setFarSide();
                  }

                  lonComm.sendAcked(destAddr, setMode);
                  int cnt = 2;

                  for (int var21 = queryStatus(lonComm, destAddr, 1, auth, farSide).nodeState;
                     (var21 & 15) != 12 && cnt-- > 0;
                     var21 = queryStatus(lonComm, destAddr, 1, auth, farSide).nodeState
                  ) {
                     wait(100);
                  }

                  setNodeState(lonComm, destAddr, 3, auth, farSide);
               }
            } else if (state == BLonNodeState.configOffline) {
               nStatus = setStateConfigOnline(lonComm, destAddr, nStatus, auth, farSide);
               if (nStatus == 12) {
                  return;
               }

               SetNodeModeRequest setMode = new SetNodeModeRequest(0);
               if (farSide) {
                  setMode.setFarSide();
               }

               setMode.setAuthenticate(auth);
               lonComm.sendAcked(destAddr, setMode);
            } else if (state == BLonNodeState.configOnline) {
               nStatus = setStateConfigOnline(lonComm, destAddr, nStatus, auth, farSide);
               if (nStatus == 4) {
                  return;
               }

               SetNodeModeRequest setMode = new SetNodeModeRequest(1);
               if (farSide) {
                  setMode.setFarSide();
               }

               setMode.setAuthenticate(auth);
               lonComm.sendAcked(destAddr, setMode);
            } else if (state == BLonNodeState.hardOffline) {
               if (nStatus == 6) {
                  return;
               }

               setNodeState(lonComm, destAddr, 6, auth, farSide);
            }

            newState = null;
            int cnt = 15;

            while (newState == null || newState != state) {
               if (--cnt <= 0) {
                  break;
               }

               wait(1000);

               try {
                  newState = getDeviceState(lonComm, destAddr, auth, farSide);
               } catch (LonException var13) {
               }
            }

            if (newState != state && passCount < 5) {
               wait((int)(5000.0 * Math.pow(2.0, passCount)));
            } else {
               done = true;
            }
         }

         if (newState != state) {
            String err = "Unable to set state to " + state + " state now = " + newState;
            lonComm.lonNetwork().log().severe(err);
            throw new LonException(err);
         }
      }
   }

   public static void wait(int mSeconds) {
      try {
         Thread.sleep(mSeconds);
      } catch (Exception var2) {
      }
   }

   public static BLonNodeState getDeviceState(BLonDevice dev) throws LonException {
      return getDeviceState(dev.lonComm(), getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public static BLonNodeState getDeviceState(LonComm lonComm, LonAddress destAddr, boolean auth) throws LonException {
      return getDeviceState(lonComm, destAddr, auth, false);
   }

   public static BLonNodeState getDeviceState(LonComm lonComm, LonAddress destAddr, boolean auth, boolean farSide) throws LonException {
      QueryStatusResponse status = queryStatus(lonComm, destAddr, 1, auth, farSide);
      int state = status.nodeState;
      if ((state & 7) == 3) {
         return BLonNodeState.applicationless;
      } else if ((state & 7) == 2) {
         return BLonNodeState.unconfigured;
      } else if ((state & 7) >= 4) {
         if ((state & 7) == 6) {
            return BLonNodeState.hardOffline;
         } else {
            return (state & 15) == 4 ? BLonNodeState.configOnline : BLonNodeState.configOffline;
         }
      } else {
         lonComm.lonNetwork().log().warning("could not decode state " + Integer.toString(state, 16) + " in " + destAddr);
         return BLonNodeState.unknown;
      }
   }

   private static boolean isStateCnfgOnline(int nStatus) {
      return (nStatus & 7) == 4;
   }

   private static int setStateConfigOnline(LonComm lonComm, LonAddress destAddr, int origStatus, boolean auth, boolean farSide) throws LonException {
      int nStatus = origStatus;
      int stateCnt = 0;

      while (!isStateCnfgOnline(nStatus) && stateCnt++ < 2) {
         setNodeState(lonComm, destAddr, 4, auth, farSide);
         int statusCnt = 0;

         do {
            int queryRetries = farSide ? 3 : 1;
            nStatus = queryStatus(lonComm, destAddr, queryRetries, auth, farSide).nodeState;
         } while (isStateCnfgOnline(nStatus) || statusCnt++ >= 3);
      }

      return nStatus;
   }

   private static void setNodeState(LonComm lonComm, LonAddress destAddr, int state, boolean auth, boolean farSide) throws LonException {
      SetNodeModeRequest setMode = new SetNodeModeRequest(3, state);
      if (farSide) {
         setMode.setFarSide();
      }

      setMode.setAuthenticate(auth);
      setMode.setTransmitTimer(BLonRepeatTimer.milliSec192);

      try {
         lonComm.sendAcked(destAddr, setMode);
      } catch (LonException var7) {
      }
   }

   public static void resetNode(BLonDevice dev) throws LonException {
      resetNode(dev.lonComm(), getSendAddress(dev), dev.getDeviceData().getAuthenticate(), false);
   }

   public static void resetNode(BLonRouter rtr) throws LonException {
      resetNode(rtr.lonComm(), getSendAddress(rtr), rtr.getNearDeviceData().getAuthenticate(), false);
      resetNode(rtr.lonComm(), getSendAddress(rtr), rtr.getNearDeviceData().getAuthenticate(), true);
   }

   public static void resetNode(LonComm lonComm, LonAddress destAddr, boolean auth, boolean farSide) throws LonException {
      if (BLocal.local.equals(destAddr)) {
         resetLocal(lonComm);
      } else {
         SetNodeModeRequest setMode = new SetNodeModeRequest(2);
         if (farSide) {
            setMode.setFarSide();
         }

         if (!auth) {
            lonComm.sendUnacknowledged(destAddr, setMode);
         } else {
            setMode.setRetryCount(0);
            setMode.setAuthenticate(true);

            try {
               lonComm.sendAcked(destAddr, setMode);
            } catch (LonException var6) {
            }
         }

         wait(100);
         queryStatus(lonComm, destAddr, 3, auth, farSide);
      }
   }

   public static void resetLocal(LonComm lonComm) throws LonException {
      ((NLonComm)lonComm).sendLocalCommand(80);
      wait(100);
      queryStatus(lonComm, BLocal.local, 3, false, false);
   }

   public static ServicePin receiveServicePin(BLonNetmgmt netMgmt) throws LonException {
      ServicePin srvpin = null;
      NetMessageReceiver rcv = netMgmt.lonNetwork().netMessageReceiver();
      rcv.clearServicePin();

      try {
         return rcv.getServicePin(netMgmt.getServicePinWait() * 1000);
      } catch (InterruptedException var4) {
         return null;
      }
   }

   public static void sendServicePin(BLocalLonDevice locDev) throws LonException {
      BDeviceData dd = locDev.getDeviceData();
      ServicePin spMsg = new ServicePin(dd.getNeuronId(), dd.getProgramId());
      boolean wrkDomZeroLen = locDev.lonNetwork().netmgmt().getDomainId().isZeroLength();
      boolean wrkDomNdxZero = dd.getWorkingDomain() == 0;
      int domNdx = (!wrkDomZeroLen || !wrkDomNdxZero) && (wrkDomZeroLen || wrkDomNdxZero) ? 1 : 0;
      locDev.lonComm().sendUnacknowledged(BBroadcast.make(domNdx), spMsg);
   }

   public static void updateDomainTable(BLonNetmgmt nm, BLonDevice dev) throws LonException {
      updateDomainTable(nm, dev, dev.getDeviceData().getAuthenticate());
   }

   public static void updateDomainTable(BLonNetmgmt nm, BLonDevice dev, boolean auth) throws LonException {
      BDomainId domain = nm.getDomainId();
      BAuthenticationKey key = nm.getAuthenticationKey();
      updateDomainTable(dev, domain, key, auth);
   }

   public static void updateDomainTable(BLonDevice dev, BDomainId domain, BAuthenticationKey key, boolean auth) throws LonException {
      BDeviceData dd = dev.getDeviceData();
      int subnet = dd.getSubnetNodeId().getSubnetId();
      int nodeId = dd.getSubnetNodeId().getNodeId();
      boolean extended = dev.isExtended();
      LonAddress sndAdr = getSendAddress(dev);
      int wrkDmn = dd.getWorkingDomain();
      LonMessage updateDomain;
      if (extended) {
         updateDomain = new ExUpdateDomainRequest(wrkDmn, domain.getDomainId(), subnet, nodeId, domain.getLength(), key.getByteArray());
      } else {
         updateDomain = new UpdateDomainRequest(wrkDmn, domain.getDomainId(), subnet, nodeId, domain.getLength(), key.getByteArray());
      }

      updateDomain.setAuthenticate(auth);

      try {
         dev.lonComm().sendRequest(sndAdr, updateDomain);
      } catch (FailedResponseException var14) {
         try {
            if (!extended && isExtended(dev.lonComm(), sndAdr, auth)) {
               dev.log().severe(dev.getDisplayName(null) + " requires extended netmgmt support. Need updated *.lnml or execute learnNv.");
            }
         } catch (Throwable var13) {
         }

         throw var14;
      }

      if (dd.getTwoDomains()) {
         int otherDmn = wrkDmn == 0 ? 1 : 0;
         if (!domain.isZeroLength() && (dev.isLocal() || dev.lonNetwork().getLonNetmgmt().getAlwaysInZeroLengthDomain())) {
            if (extended) {
               updateDomain = new ExUpdateDomainRequest(otherDmn, new byte[6], subnet, nodeId, 0, key.getByteArray());
            } else {
               updateDomain = new UpdateDomainRequest(otherDmn, new byte[6], subnet, nodeId, 0, key.getByteArray());
            }
         } else if (extended) {
            updateDomain = new ExUpdateDomainRequest(otherDmn);
         } else {
            updateDomain = new UpdateDomainRequest(otherDmn);
         }

         updateDomain.setAuthenticate(auth);
         dev.lonComm().sendRequest(sndAdr, updateDomain);
      }
   }

   public static boolean verifyDomainTable(BLonDevice dev, BLonNetmgmt nm) throws LonException {
      return verifyDomainTable(dev, nm, false);
   }

   public static boolean verifyDomainTable(BLonDevice dev, BLonNetmgmt nm, boolean verifyKey) throws LonException {
      int domNdx = 0;
      BDeviceData dd = dev.getDeviceData();
      boolean auth = dd.getAuthenticate();
      LonAddress sndAdr = getSendAddress(dev);
      LonComm lonComm = dev.lonComm();
      QueryDomainResponse qryDomRsp = queryDomain(lonComm, sndAdr, domNdx, auth);
      BDomainId domain = nm.getDomainId();
      BDomainId devDomainId = null;
      if (qryDomRsp.inUse()) {
         devDomainId = BDomainId.make(qryDomRsp.len, qryDomRsp.getDomainId());
      }

      if (devDomainId == null || !domain.equals(devDomainId)) {
         if (!dd.getTwoDomains()) {
            return false;
         }

         domNdx = 1;
         qryDomRsp = queryDomain(lonComm, sndAdr, domNdx, auth);
         devDomainId = BDomainId.make(qryDomRsp.len, qryDomRsp.getDomainId());
         if (!domain.equals(devDomainId)) {
            return false;
         }
      }

      BSubnetNode sn = dd.getSubnetNodeId();
      if (sn.getSubnetId() != qryDomRsp.getSubnet() || sn.getNodeId() != qryDomRsp.getNodeId()) {
         return false;
      } else if (verifyKey && !LonByteArrayUtil.equals(qryDomRsp.getKey(), nm.getAuthenticationKey().getByteArray())) {
         return false;
      } else {
         if (dd.getWorkingDomain() != domNdx) {
            dd.setWorkingDomain(domNdx);
         }

         return true;
      }
   }

   public static void setWorkingDomain(BLonDevice dev, BLonNetmgmt nm) throws LonException {
      BDeviceData dd = dev.getDeviceData();
      boolean auth = dd.getAuthenticate();
      LonAddress sndAdr = getSendAddress(dev);
      LonComm lonComm = dev.lonComm();
      BDomainId domain = nm.getDomainId();
      QueryDomainResponse qryDomRsp0 = queryDomain(lonComm, sndAdr, 0, auth);
      BDomainId devDomainId0 = null;
      if (qryDomRsp0.len <= 6) {
         devDomainId0 = BDomainId.make(qryDomRsp0.len, qryDomRsp0.getDomainId());
      }

      BDomainId devDomainId1 = null;

      try {
         QueryDomainResponse qryDomRsp1 = queryDomain(lonComm, sndAdr, 1, auth);
         if (qryDomRsp1.len <= 6) {
            devDomainId1 = BDomainId.make(qryDomRsp1.len, qryDomRsp1.getDomainId());
         }
      } catch (FailedResponseException var11) {
      }

      if (devDomainId0 != null && domain.equals(devDomainId0)) {
         dd.setWorkingDomain(0);
      } else if (devDomainId1 != null && domain.equals(devDomainId1)) {
         dd.setWorkingDomain(1);
      } else {
         dd.setNodeState(BLonNodeState.unknown);
      }
   }

   public static QueryDomainResponse queryDomain(BLonDevice dev, int ndx) throws LonException {
      return queryDomain(dev.lonComm(), getSendAddress(dev), ndx, dev.getDeviceData().getAuthenticate(), false, dev.isExtended());
   }

   public static QueryDomainResponse queryDomain(LonComm lonComm, LonAddress destAddr, int ndx, boolean auth) throws LonException {
      return queryDomain(lonComm, destAddr, ndx, auth, false);
   }

   public static QueryDomainResponse queryDomain(LonComm lonComm, LonAddress destAddr, int ndx, boolean auth, boolean farSide) throws LonException {
      try {
         return queryDomain(lonComm, destAddr, ndx, auth, farSide, false);
      } catch (FailedResponseException var6) {
         return queryDomain(lonComm, destAddr, ndx, auth, farSide, true);
      }
   }

   public static QueryDomainResponse queryDomain(LonComm lonComm, LonAddress destAddr, int ndx, boolean auth, boolean farSide, boolean extended) throws LonException {
      LonMessage qryDmn;
      if (extended) {
         qryDmn = new ExEnumerateDomainRequest(ndx);
      } else {
         qryDmn = new QueryDomainRequest(ndx);
      }

      if (farSide) {
         qryDmn.setFarSide();
      }

      qryDmn.setAuthenticate(auth);
      return (QueryDomainResponse)lonComm.sendRequest(destAddr, qryDmn);
   }

   public static void initDiscover(BLonNetwork lon, BLonNetmgmt nm, boolean bothDomains) throws LonException {
      BLonDevice[] devs = lon.addressManager().getDeviceList(false);
      int len = devs.length;
      int wrkDmn = lon.getLocalLonDevice().getDeviceData().getWorkingDomain();
      int othDmn = wrkDmn == 0 ? 1 : 0;
      setRespondToQuery(lon.lonComm(), 1, wrkDmn);
      if (bothDomains && nm.getDomainId().getLength() != 0) {
         setRespondToQuery(lon.lonComm(), 1, othDmn);
      }

      wait(500);

      for (int nodeId = 0; nodeId < len; nodeId++) {
         BLonDevice dev = devs[nodeId];
         if (dev != null && !dev.isLocal()) {
            try {
               BNeuronId nId = dev.getDeviceData().getNeuronId();
               if (!nId.isZero()) {
                  setRespondToQuery(lon.lonComm(), nId, 0);
               }
            } catch (Throwable var12) {
            }
         }
      }

      BLonRouter[] lonRouters = lon.addressManager().getRouterList();

      for (int i = 0; i < lonRouters.length; i++) {
         BLonRouter rtr = lonRouters[i];
         if (rtr != null) {
            try {
               BNeuronId nId = rtr.getNearDeviceData().getNeuronId();
               if (!nId.isZero()) {
                  setRespondToQuery(lon.lonComm(), nId, 0);
               }
            } catch (Throwable var11) {
            }
         }
      }
   }

   public static void setRespondToQuery(LonComm lonComm, int mode, int domainNdx) throws LonException {
      RespondToQueryRequest respond = new RespondToQueryRequest(mode);
      respond.setRetryCount(3);
      lonComm.sendUnackRepeat(BBroadcast.make(domainNdx), respond);
   }

   public static void setRespondToQuery(BLonDevice dev, int mode) throws LonException {
      setRespondToQuery(dev.lonComm(), getSendAddress(dev), mode);
   }

   public static void setRespondToQuery(LonComm lonComm, LonAddress destAddr, int mode) throws LonException {
      RespondToQueryRequest respond = new RespondToQueryRequest(mode);
      lonComm.sendRequest(destAddr, respond);
   }

   public static QueryStatusResponse queryStatus(BLonDevice dev, int retries) throws LonException {
      return queryStatus(dev.lonComm(), getSendAddress(dev), retries, dev.getDeviceData().getAuthenticate(), false);
   }

   public static QueryStatusResponse queryStatus(BLonRouter rtr, int retries, boolean farSide) throws LonException {
      return queryStatus(rtr.lonComm(), getSendAddress(rtr), retries, rtr.getNearDeviceData().getAuthenticate(), farSide);
   }

   public static QueryStatusResponse queryStatus(LonComm lonComm, LonAddress destAddr, int retries, boolean auth) throws LonException {
      return queryStatus(lonComm, destAddr, retries, auth, false);
   }

   public static QueryStatusResponse queryStatus(LonComm lonComm, LonAddress destAddr, int retries, boolean auth, boolean farSide) throws LonException {
      LonException ex = null;

      while (retries >= 0) {
         try {
            QueryStatusRequest req = new QueryStatusRequest();
            if (farSide) {
               req.setFarSide();
            }

            req.setAuthenticate(auth);
            if (lonComm.lonNetwork().getLonCommConfig().getRepeatTimer().getOrdinal() < 7) {
               req.setTransmitTimer(BLonRepeatTimer.milliSec192);
            }

            return (QueryStatusResponse)lonComm.sendRequest(destAddr, req);
         } catch (LonException var8) {
            ex = var8;
            wait(100);
            retries--;
         }
      }

      throw ex;
   }

   public static QueryIdResponse queryId(LonComm lonComm, int selector, int domainNdx) {
      return queryId(lonComm, domainNdx, new QueryIdRequest(selector));
   }

   public static QueryIdResponse queryId(LonComm lonComm, int domainNdx, QueryIdRequest qRqst) {
      try {
         return (QueryIdResponse)lonComm.sendRequest(BBroadcast.make(domainNdx), qRqst);
      } catch (LonException var5) {
         return null;
      }
   }

   public static void clearStatus(BLonDevice dev) throws LonException {
      clearStatus(dev.lonComm(), getSendAddress(dev), dev.getDeviceData().getAuthenticate(), false);
   }

   public static void clearStatus(BLonRouter rtr) throws LonException {
      clearStatus(rtr.lonComm(), getSendAddress(rtr), rtr.getNearDeviceData().getAuthenticate(), true);
      clearStatus(rtr.lonComm(), getSendAddress(rtr), rtr.getNearDeviceData().getAuthenticate(), false);
   }

   public static void clearStatus(LonComm lonComm, LonAddress destAddr, boolean auth, boolean farSide) throws LonException {
      ClearStatus cs = new ClearStatus();
      cs.setAuthenticate(auth);
      if (farSide) {
         cs.setFarSide();
      }

      lonComm.sendRequest(destAddr, cs);
   }

   public static void recalculateChecksum(BLonDevice dev, int type) throws LonException {
      ChecksumRecalcRequest req = new ChecksumRecalcRequest(type);
      req.setAuthenticate(dev.getDeviceData().getAuthenticate());
      req.setTransmitTimer(BLonRepeatTimer.milliSec384);
      dev.lonComm().sendRequest(getSendAddress(dev), req);
   }

   public static void clearAddressTable(BLonDevice dev) throws LonException {
      dev.getDeviceData().clearAddressTable();
      if (dev.isExtended()) {
         extAddressTableInitialize(dev, 0, 65535);
      } else {
         BDeviceData dda = dev.getDeviceData();
         int addressCount = dda.getAddressCount();

         for (int ndx = 0; ndx < addressCount; ndx++) {
            updateAddressTable(dev, ndx);
         }
      }
   }

   public static void extAddressTableInitialize(BLonDevice dev, int beginIndex, int endIndex) throws LonException {
      LonMessage msg = new ExInitRequest(3, beginIndex, endIndex);
      msg.setAuthenticate(dev.authenticate());
      msg.setRetryCount(15);
      dev.lonComm().sendRequest(getSendAddress(dev), msg);
   }

   public static void updateAddressTable(BLonDevice dev) throws LonException {
      int tabLen = dev.getDeviceData().getAddressCount();

      for (int i = 0; i < tabLen; i++) {
         updateAddressTable(dev, i);
      }
   }

   public static void updateAddressTable(BLonDevice dev, int index) throws LonException {
      BIAddressEntry entry = dev.getDeviceData().getAddressEntry(index);
      LonMessage updateAddress;
      if (dev.isExtended()) {
         updateAddress = new ExUpdateAddressReq(index, entry);
      } else {
         updateAddress = new UpdateAddress(index, entry);
      }

      updateAddress.setAuthenticate(dev.getDeviceData().getAuthenticate());
      dev.lonComm().sendRequest(getSendAddress(dev), updateAddress);
   }

   public static BIAddressEntry getBAddressTableEntry(BLonDevice dev, int index) throws LonException {
      QueryAddrResponse qryRsp = getAddressTableEntry(dev, index);
      return BAddressEntry.make(
         qryRsp.getAddressType(),
         qryRsp.getSize(),
         qryRsp.getGroupOrSubnet(),
         qryRsp.getMemberOrNode(),
         0,
         qryRsp.getDomainIndex(),
         BLonRepeatTimer.make(qryRsp.repeatTimer),
         qryRsp.retryCount,
         BLonReceiveTimer.make(qryRsp.receiveTimer),
         BLonRepeatTimer.make(qryRsp.xmitTimer)
      );
   }

   public static boolean verifyAddressEntry(BLonDevice dev, int index) {
      try {
         BDeviceData dd = dev.getDeviceData();
         BIAddressEntry entry = dd.getAddressEntry(index);
         BLinkDescriptor lnkDscpt = dev.lonNetwork().getLonNetmgmt().getLinkDescriptors().getDescriptor(entry.getDescriptor());
         QueryAddrResponse qryResp = getAddressTableEntry(dev, index);
         BAddressType adrTyp = qryResp.getAddressType();
         if (adrTyp != entry.getAddressType()) {
            return false;
         } else {
            switch (adrTyp.getOrdinal()) {
               case 1:
                  if (qryResp.getDomainIndex() != dd.getWorkingDomain()
                     || qryResp.getMemberOrNode() != entry.getMemberOrNode()
                     || qryResp.getRepeatTimer() != lnkDscpt.getRepeatTimer()
                     || qryResp.getRetryCount() != lnkDscpt.getRetries()
                     || qryResp.getReceiveTimer() != lnkDscpt.getReceiveTimer()
                     || qryResp.getXmitTimer() != lnkDscpt.getTransmitTimer()
                     || qryResp.getGroupOrSubnet() != entry.getGroupOrSubnet()) {
                     return false;
                  }
                  break;
               case 2:
                  if (qryResp.getDomainIndex() != dd.getWorkingDomain()
                     || qryResp.getMemberOrNode() != entry.getMemberOrNode()
                     || qryResp.getRepeatTimer() != lnkDscpt.getRepeatTimer()
                     || qryResp.getRetryCount() != lnkDscpt.getRetries()
                     || qryResp.getXmitTimer() != lnkDscpt.getTransmitTimer()
                     || qryResp.getGroupOrSubnet() != entry.getGroupOrSubnet()) {
                     return false;
                  }
                  break;
               case 3:
                  if (qryResp.getDomainIndex() != dd.getWorkingDomain()
                     || qryResp.getRepeatTimer() != lnkDscpt.getRepeatTimer()
                     || qryResp.getRetryCount() != lnkDscpt.getRetries()
                     || qryResp.getXmitTimer() != lnkDscpt.getTransmitTimer()
                     || qryResp.getGroupOrSubnet() != entry.getGroupOrSubnet()) {
                     return false;
                  }
                  break;
               case 4:
                  if (qryResp.getRepeatTimer() != lnkDscpt.getRepeatTimer()
                     || qryResp.getRetryCount() != lnkDscpt.getRetries()
                     || qryResp.getXmitTimer() != lnkDscpt.getTransmitTimer()) {
                     return false;
                  }
            }

            return true;
         }
      } catch (LonException var7) {
         return false;
      }
   }

   public static QueryAddrResponse getAddressTableEntry(BLonDevice dev, int index) throws LonException {
      return getAddressTableEntry(dev.lonComm(), getSendAddress(dev), index, dev.getDeviceData().getAuthenticate(), dev.isExtended());
   }

   public static QueryAddrResponse getAddressTableEntry(LonComm lonComm, LonAddress addr, int index, boolean auth, boolean extended) throws LonException {
      LonMessage qryRqst;
      if (extended) {
         qryRqst = new ExEnumerateAddressRequest(index);
      } else {
         qryRqst = new QueryAddrRequest(index);
      }

      qryRqst.setAuthenticate(auth);
      return (QueryAddrResponse)lonComm.sendRequest(addr, qryRqst);
   }

   public static BLonLinkType getLinkType(int descriptor) {
      switch (descriptor) {
         case 0:
            return BLonLinkType.unknown;
         case 1:
            return BLonLinkType.standard;
         case 2:
            return BLonLinkType.reliable;
         case 3:
            return BLonLinkType.critical;
         case 4:
            return BLonLinkType.authenticated;
         default:
            return BLonLinkType.unknown;
      }
   }

   public static int linkTypeToDescriptor(BLonLinkType type) {
      if (type == null) {
         return 0;
      } else {
         switch (type.getOrdinal()) {
            case 0:
               return 0;
            case 1:
               return 1;
            case 2:
               return 2;
            case 3:
               return 3;
            case 4:
               return 4;
            case 5:
               return 0;
            default:
               return 0;
         }
      }
   }

   public static BLonLinkType getLinkType(BNvConfigData cfg) {
      if (cfg.getAuthenticated()) {
         return BLonLinkType.authenticated;
      } else {
         switch (cfg.getServiceType().getOrdinal()) {
            case 0:
               return BLonLinkType.critical;
            case 1:
               return BLonLinkType.reliable;
            case 2:
               return BLonLinkType.standard;
            case 3:
               return BLonLinkType.critical;
            default:
               return BLonLinkType.standard;
         }
      }
   }

   public static BLonServiceType linkTypeToServiceType(BLonLinkType linkType) {
      switch (linkType.getOrdinal()) {
         case 0:
            return BLonServiceType.unacked;
         case 1:
            return BLonServiceType.unacked;
         case 2:
            return BLonServiceType.unackedRpt;
         case 3:
            return BLonServiceType.acked;
         case 4:
            return BLonServiceType.acked;
         case 5:
            return BLonServiceType.request;
         default:
            return BLonServiceType.unacked;
      }
   }

   public static BNvConfigData queryNvConfigData(BLonDevice dev, int nvIndex) throws LonException {
      return queryNvConfigData(dev.lonComm(), getSendAddress(dev), nvIndex, dev.getDeviceData().getAuthenticate(), dev.isExtended());
   }

   public static BNvConfigData queryNvConfigData(LonComm lonComm, LonAddress sendAddr, int nvIndex, boolean auth, boolean extended) throws LonException {
      LonMessage qryRqst;
      if (extended) {
         qryRqst = new ExEnumerateNvConfigRequest(nvIndex);
      } else {
         qryRqst = new QueryNvConfigRequest(nvIndex);
      }

      qryRqst.setAuthenticate(auth);
      QueryNvConfigResponse qryRsp = (QueryNvConfigResponse)lonComm.sendRequest(sendAddr, qryRqst);
      return qryRsp.getConfigData();
   }

   public static void extNvInitialize(BLonDevice dev, int beginIndex, int endIndex) throws LonException {
      LonMessage msg = new ExInitRequest(5, beginIndex, endIndex);
      msg.setAuthenticate(dev.authenticate());
      msg.setRetryCount(15);
      dev.lonComm().sendRequest(getSendAddress(dev), msg);
   }

   public static boolean verifyNvConfig(BLonDevice dev, int nvIndex, BNvConfigData configData) throws LonException {
      if (dev.isLocal()) {
         return true;
      } else {
         BNvConfigData devConfigData = queryNvConfigData(dev, nvIndex);
         return devConfigData.equivalent(configData);
      }
   }

   public static void updateNvConfig(BLonDevice dev, int nvIndex, BNvConfigData configData) throws LonException {
      if (!dev.isLocal()) {
         LonMessage updtNvCfg;
         if (dev.isExtended()) {
            updtNvCfg = new ExUpdateNvConfigRequest(nvIndex, configData);
         } else {
            updtNvCfg = new UpdateNvConfigRequest(nvIndex, configData);
         }

         updtNvCfg.setAuthenticate(dev.authenticate());
         dev.lonComm().sendRequest(getSendAddress(dev), updtNvCfg);
      }
   }

   public static void updateAliasConfig(BLonDevice dev, int aliasIndex, BAliasConfigData configData) throws LonException {
      if (!dev.isLocal()) {
         LonMessage updt;
         if (dev.isExtended()) {
            updt = new ExUpdateAliasRequest(aliasIndex, configData);
         } else {
            updt = new UpdateAliasRequest(aliasIndex + dev.getDeviceData().getAliasTable().getAliasOffset(), configData);
         }

         updt.setAuthenticate(dev.authenticate());
         dev.lonComm().sendRequest(getSendAddress(dev), updt);
      }
   }

   public static void extAliasInitialize(BLonDevice dev, int beginIndex, int endIndex) throws LonException {
      LonMessage msg = new ExInitRequest(6, beginIndex, endIndex);
      msg.setAuthenticate(dev.authenticate());
      msg.setRetryCount(15);
      msg.setTransmitTimer(BLonRepeatTimer.milliSec384);
      dev.lonComm().sendRequest(getSendAddress(dev), msg);
   }

   public static boolean verifyAliasNvConfig(BLonDevice dev, int aliasIndex, BAliasConfigData origConfigData) throws LonException {
      if (dev.isLocal()) {
         return true;
      } else {
         BDeviceData dd = dev.getDeviceData();

         QueryAliasResponse qryRsp;
         try {
            qryRsp = queryAliasConfigData(
               dev.lonComm(), getSendAddress(dev), aliasIndex, dd.getAliasTable().getAliasOffset(), dd.getAuthenticate(), dev.isExtended()
            );
         } catch (FailedResponseException var6) {
            return false;
         }

         BAliasConfigData devConfigData = qryRsp.getAliasConfigData();
         devConfigData.setState(origConfigData.getState());
         return devConfigData.equivalent(origConfigData);
      }
   }

   public static void updateAliasTable(BLonDevice dev) throws LonException {
      BDeviceData dd = dev.getDeviceData();
      LonAddress sendAdr = dd.getNeuronId();
      LonComm lonComm = dev.lonComm();
      boolean authenticate = dd.getAuthenticate();
      boolean extended = dev.isExtended();
      BAliasTable aliasTable = dd.getAliasTable();
      int aliasCnt = aliasTable.getAliasCount();
      int aliasOffset = aliasTable.getAliasOffset();
      BAliasConfigData[] a = aliasTable.getAliasArray();

      for (int i = 0; i < aliasCnt; i++) {
         LonMessage updt;
         if (extended) {
            updt = new ExUpdateAliasRequest(aliasOffset + i, a[i]);
         } else {
            updt = new UpdateAliasRequest(aliasOffset + i, a[i]);
         }

         updt.setAuthenticate(authenticate);
         lonComm.sendRequest(sendAdr, updt);
      }
   }

   public static BAliasConfigData queryAliasConfigData(BLonDevice dev, int aliasIndex) throws LonException {
      BDeviceData dd = dev.getDeviceData();
      QueryAliasResponse rsp = queryAliasConfigData(
         dev.lonComm(), getSendAddress(dev), aliasIndex, dd.getAliasTable().getAliasOffset(), dd.getAuthenticate(), dev.isExtended()
      );
      return rsp.getAliasConfigData();
   }

   public static QueryAliasResponse queryAliasConfigData(LonComm lonComm, LonAddress sendAddr, int index, int offset, boolean auth, boolean isExtended) throws LonException {
      LonMessage aliasRqst;
      if (isExtended) {
         aliasRqst = new ExEnumerateAliasRequest(index);
      } else {
         aliasRqst = new QueryAliasRequest(index + offset);
      }

      aliasRqst.setAuthenticate(auth);
      return (QueryAliasResponse)lonComm.sendRequest(sendAddr, aliasRqst);
   }

   public static void setOfflineInBind(BLonDevice dev, Vector<BLonDevice> offDevs) throws LonException {
      if (!dev.isLocal()) {
         if (dev.isConfigOnline() && !DeviceFacets.getDisableSetOfflineInBind(dev)) {
            setDeviceState(dev, BLonNodeState.configOffline);
            setDeviceState(dev, BLonNodeState.hardOffline);
            dev.getDeviceData().set(BDeviceData.nodeState, BLonNodeState.hardOffline, AddressManager.noDeviceChange);
            offDevs.addElement(dev);
         }
      }
   }

   public static int getNvLength(LonComm lonComm, LonAddress sendAddr, int nvIndex, boolean auth) throws LonException {
      FetchNvResponse resp = doFetchNv(lonComm, sendAddr, nvIndex, auth);
      return resp.getNvDataLength();
   }

   public static byte[] fetchNv(BLonDevice dev, int nvIndex) throws LonException {
      LonAddress adr = (LonAddress)(dev.isConfigOnline() ? dev.getSubnetNodeAddress() : dev.getNeuronIdAddress());
      return fetchNv(dev.lonComm(), adr, nvIndex, dev.getDeviceData().getAuthenticate());
   }

   public static byte[] fetchNv(LonComm lonComm, LonAddress sendAddr, int nvIndex, boolean auth) throws LonException {
      FetchNvResponse resp = doFetchNv(lonComm, sendAddr, nvIndex, auth);
      return resp.getNvData();
   }

   private static FetchNvResponse doFetchNv(LonComm lonComm, LonAddress sendAddr, int nvIndex, boolean auth) throws LonException {
      FetchNvRequest req = new FetchNvRequest(nvIndex);
      req.setAuthenticate(auth);
      FetchNvResponse resp = (FetchNvResponse)lonComm.sendRequest(sendAddr, req);
      if (resp.getNvIndex() != nvIndex) {
         throw new LonException("NvIndex mismatch in poll response:" + resp.getNvIndex() + "!=" + nvIndex);
      } else {
         return resp;
      }
   }

   public static byte[] pollNv(BLonDevice dev, BNvConfigData configData) throws LonException {
      LonAddress sendAddr = dev.getSubnetNodeAddress();
      UnprocessedNV req = new UnprocessedNV(configData.getDirection(), configData.getSelector(), new byte[0]);
      UnprocessedNV resp = (UnprocessedNV)dev.lonComm().sendRequest(sendAddr, req);
      return resp.getData();
   }

   public static void sendNvUpdate(BLocalLonDevice dev, BNvConfigData configData, byte[] data) throws LonException {
      int adrNdx = configData.getAddrIndex();
      if (adrNdx >= 0 && adrNdx <= 14) {
         UnprocessedNV msgData = new UnprocessedNV(configData.getDirection().reverse(), configData.getSelector(), data);
         BLonServiceType serviceType = configData.getWriteServiceType();
         NAppBuffer appBuffer = NAppBuffer.makeAppBuffer();
         appBuffer.setServiceType(serviceType);
         appBuffer.setAuthenticate(configData.getAuthenticated());
         appBuffer.setDestAddress(BImplicit.make(adrNdx));
         appBuffer.setMessage(msgData);
         sendAppBuffer(appBuffer, (NLonComm)dev.lonComm(), serviceType);
      } else {
         throw new LonException("Invalid adrNdx " + adrNdx + " in " + dev.getDisplayName(null) + ":" + configData.getParent().getDisplayName(null));
      }
   }

   private static void sendAppBuffer(NAppBuffer appBuffer, NLonComm lonComm, BLonServiceType serviceType) throws LonException {
      NAppBuffer resp = null;
      LonException e = null;

      try {
         if (serviceType == BLonServiceType.request) {
            resp = lonComm.doLonCommSendRequest(appBuffer, -1);
         } else {
            resp = lonComm.doLonCommSendNormal(appBuffer);
         }
      } catch (LonException var6) {
         e = var6;
      }

      if (!appBuffer.isNoTransaction()) {
         appBuffer.releaseAppBuffer();
      }

      if (resp != null) {
         resp.releaseAppBuffer();
      }

      if (e != null) {
         throw e;
      }
   }

   public static void setNvValue(BLonDevice dev, BNvConfigData configData, byte[] data) throws LonException {
      UnprocessedNV msgData = new UnprocessedNV(configData.getDirection(), configData.getSelector(), data);
      BLonServiceType serviceType = configData.getWriteServiceType();
      NAppBuffer appBuffer = NAppBuffer.makeAppBuffer();
      appBuffer.setDomainIndex(dev.lonNetwork().getLocalLonDevice().getWorkingDomain());
      appBuffer.setServiceType(serviceType);
      appBuffer.setAuthenticate(configData.getAuthenticated());
      appBuffer.setDestAddress(dev.getDeviceData().getSubnetNodeId());
      appBuffer.setMessage(msgData);
      sendAppBuffer(appBuffer, (NLonComm)dev.lonComm(), serviceType);
   }

   public static void setNvValue(
      LonAddress sendAddr, LonComm lonComm, BLonNvDirection direction, int selector, BLonServiceType serviceType, boolean authenticate, byte[] data
   ) throws LonException {
      UnprocessedNV msgData = new UnprocessedNV(direction, selector, data);
      serviceType = serviceType.getWriteServiceType();
      NAppBuffer appBuffer = NAppBuffer.makeAppBuffer();
      appBuffer.setDomainIndex(lonComm.lonNetwork().getLocalLonDevice().getWorkingDomain());
      appBuffer.setServiceType(serviceType);
      appBuffer.setAuthenticate(authenticate);
      appBuffer.setDestAddress(sendAddr);
      appBuffer.setMessage(msgData);
      sendAppBuffer(appBuffer, (NLonComm)lonComm, serviceType);
   }

   public static void setConfigSrc(BLonDevice dev, BLonConfigSourceEnum cfgSrc) {
      BLonComponent nv = (BLonComponent)dev.findSnvtType(69);
      if (nv != null) {
         try {
            nv.set("configSrc", BLonEnum.make(cfgSrc));
         } catch (Throwable var4) {
            dev.log().log(Level.SEVERE, "Error setting configSrc in " + dev.getDisplayName(null), var4);
         }
      }
   }

   public static void wink(BLonDevice dev) throws LonException {
      wink(dev.lonComm(), getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public static void wink(LonComm lonComm, LonAddress adr, boolean auth) throws LonException {
      WinkRequest winkReq = new WinkRequest();
      winkReq.setAuthenticate(auth);
      lonComm.sendAcked(adr, winkReq);
   }

   public static BLonDevice[] getLonDevices(BIDeviceFolder df) {
      Array<BLonDevice> a = new Array(BLonDevice.class);
      addDevices(df, a);
      return (BLonDevice[])a.trim();
   }

   private static void addDevices(BIDeviceFolder c, Array<BLonDevice> a) {
      SlotCursor<Property> sc = ((BComponent)c).getProperties();

      while (sc.nextObject()) {
         BObject obj = sc.get();
         if (obj.getType().is(BLonDevice.TYPE)) {
            a.add((BLonDevice)obj);
         }

         if (obj.getType().is(BIDeviceFolder.TYPE)) {
            addDevices((BIDeviceFolder)obj, a);
         }
      }
   }

   public static BLonRouter[] getLonRouters(BIDeviceFolder df) {
      Array<BLonRouter> a = new Array(BLonRouter.class);
      addRouters(df, a);
      return (BLonRouter[])a.trim();
   }

   private static void addRouters(BIDeviceFolder c, Array<BLonRouter> a) {
      SlotCursor<Property> sc = ((BComponent)c).getProperties();

      while (sc.nextObject()) {
         BObject obj = sc.get();
         if (obj.getType().is(BLonRouter.TYPE)) {
            a.add((BLonRouter)obj);
         }

         if (obj.getType().is(BIDeviceFolder.TYPE)) {
            addRouters((BIDeviceFolder)obj, a);
         }
      }
   }

   public static boolean isExtended(BLonDevice dev) throws LonException {
      return isExtended(dev.lonComm(), getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public static boolean isExtended(LonComm lonComm, LonAddress sndAdr, boolean auth) {
      try {
         CapacityInfoRequest capInf = new CapacityInfoRequest(0, 12);
         capInf.setAuthenticate(auth);
         LonMessage lm = lonComm.sendRequest(sndAdr, capInf);
         byte[] data = lm.getMessageData();
         int off = (data[0] & 63) == 63 ? 11 : 9;
         return data[off] > 0;
      } catch (Throwable var7) {
         return false;
      }
   }

   public static void validateNeuronId(BNeuronId nid, BComponent spDev) throws LonException {
      if (!nid.isZero()) {
         BLonNetwork lon = getLonNetwork(spDev);
         BLonDevice[] devs = lon.getLonDevices();

         for (int i = 0; i < devs.length; i++) {
            if (devs[i] != spDev && devs[i].getNeuronIdAddress().equals(nid)) {
               throw new LonException("NeuronId already in use by " + devs[i].getDisplayName(null));
            }
         }

         BLonRouter[] rtrs = lon.getLonRouters();

         for (int ix = 0; ix < rtrs.length; ix++) {
            if (rtrs[ix] != spDev && rtrs[ix].getNeuronIdAddress().equals(nid)) {
               throw new LonException("NeuronId already in use by " + rtrs[ix].getDisplayName(null));
            }
         }
      }
   }

   public static void threadState() {
      Thread t = Thread.currentThread();
      ThreadGroup tg = t.getThreadGroup();
      Thread[] ta = new Thread[tg.activeCount() * 2];
      tg.enumerate(ta);

      for (int i = 0; i < ta.length; i++) {
         if (ta[i] != null) {
            System.out.println(ta[i]);
         }
      }
   }

   public static boolean toBool(byte b, int offset) {
      return (b >> offset & 1) > 0;
   }

   public static void sendFailedResponse(NLonComm lonComm, LonMessage msg) {
      int msgCode = msg.getMessageCode();
      int respCode = msgCode;
      if (msgCode >= 96) {
         respCode = msgCode - 96;
      } else if (msgCode >= 80) {
         respCode = msgCode - 64;
      }

      lonComm.sendResponse(msg, new NoDataResponse(respCode));
   }

   public static BLonNetwork getLonNetwork(BComponent c) {
      return (BLonNetwork)getParent(c, BLonNetwork.TYPE);
   }

   public static BLonDevice getLonDevice(BComponent c) {
      return (BLonDevice)getParent(c, BLonDevice.TYPE);
   }

   public static BLonObject getLonObject(BComponent c) {
      return (BLonObject)getParent(c, BLonObject.TYPE);
   }

   public static BComponent getParent(BComponent child, Type parentType) {
      BComplex p = child.getParent();

      while (p != null && !p.getType().is(parentType)) {
         p = p.getParent();
      }

      return (BComponent)p;
   }

   public static Object[] getDecendantsByClass(BComponent c, Class<?> cls) {
      Array a = new Array(cls);
      getDecendantsByClass(a, c, cls);
      return a.trim();
   }

   private static void getDecendantsByClass(Array a, BComponent c, Class<?> cls) {
      SlotCursor<Property> sc = c.getProperties();

      while (sc.next()) {
         BObject o = sc.get();
         if (cls.isInstance(o)) {
            a.add(o);
         }

         if (o instanceof BComponent) {
            getDecendantsByClass(a, (BComponent)o, cls);
         }
      }
   }

   public static LonAddress getSendAddress(BLonDevice dev) {
      return (LonAddress)(dev.isLocal() ? BLocal.local : dev.getNeuronIdAddress());
   }

   public static LonAddress getSendAddress(BLonRouter rtr) {
      return rtr.getNeuronIdAddress();
   }

   public static String debugTimeStamp() {
      BAbsTime absTim = BAbsTime.make(System.currentTimeMillis());
      return absTim.toTimeString(null) + ":" + absTim.getMillisecond();
   }

   public static String timeStamp() {
      return BAbsTime.make(System.currentTimeMillis()).toTimeString(showMilliSecs);
   }
}
