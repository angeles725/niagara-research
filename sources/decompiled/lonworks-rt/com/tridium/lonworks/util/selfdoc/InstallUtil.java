package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.netmessages.QueryNodeInfoRequest;
import com.tridium.lonworks.netmessages.QueryNodeInfoResponse;
import com.tridium.lonworks.netmessages.QueryNvInfoRequest;
import com.tridium.lonworks.netmessages.QueryNvInfoResponse;
import com.tridium.lonworks.netmgmt.NetMgmtConst;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.LonAddress;

public class InstallUtil implements NetMessages, NetMgmtConst {
   private static final int SEGMENT_LENGTH = 32;

   public static byte[] getNvInfo(LonComm lonComm, LonAddress adr, boolean auth, int nvIndex, int nvInfo) throws LonException {
      QueryNvInfoRequest infoReq = new QueryNvInfoRequest(nvIndex, nvInfo);
      infoReq.setAuthenticate(auth);
      QueryNvInfoResponse infoResp = (QueryNvInfoResponse)lonComm.sendRequest(adr, infoReq);
      return infoResp.getData();
   }

   public static String getNvSdInfo(LonComm lonComm, LonAddress adr, boolean auth, int nvIndex) throws LonException {
      StringBuilder sb = new StringBuilder();
      boolean done = false;
      int offset = 0;

      while (!done) {
         QueryNvInfoRequest infoReq = new QueryNvInfoRequest(nvIndex, 3);
         infoReq.setLength(32);
         infoReq.setOffset(offset);
         offset += 32;
         infoReq.setAuthenticate(auth);
         QueryNvInfoResponse infoResp = (QueryNvInfoResponse)lonComm.sendRequest(adr, infoReq);
         byte[] data = infoResp.getData();

         for (int i = 1; i < data.length; i++) {
            if (data[i] == 0) {
               done = true;
               break;
            }

            sb.append((char)data[i]);
         }
      }

      return sb.toString();
   }

   public static String getNodeInfo(LonComm lonComm, LonAddress adr, boolean auth) throws LonException {
      StringBuilder sb = new StringBuilder();
      boolean done = false;
      int offset = 0;

      while (!done) {
         QueryNodeInfoRequest infoReq = new QueryNodeInfoRequest(offset, 32);
         offset += 32;
         infoReq.setAuthenticate(auth);
         QueryNodeInfoResponse infoResp = (QueryNodeInfoResponse)lonComm.sendRequest(adr, infoReq);
         byte[] data = infoResp.getData();

         for (int i = 1; i < data.length; i++) {
            if (data[i] == 0) {
               done = true;
               break;
            }

            sb.append((char)data[i]);
         }
      }

      return sb.toString();
   }
}
