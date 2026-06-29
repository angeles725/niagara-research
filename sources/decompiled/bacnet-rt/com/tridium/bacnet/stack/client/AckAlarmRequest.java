package com.tridium.bacnet.stack.client;

import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.DeviceRegistry;
import java.util.StringTokenizer;
import javax.baja.alarm.BAckState;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.log.Log;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;

public class AckAlarmRequest implements Runnable, BacnetAlarmConst {
   private BAlarmRecord ackRequest;
   private static final Log logger = Log.getLog("bacnet.client");

   public AckAlarmRequest(BAlarmRecord ackRequest) {
      this.ackRequest = ackRequest;
   }

   @Override
   public void run() {
      BBacnetDevice device = null;

      try {
         BBacnetNetwork bacnet = BBacnetNetwork.bacnet();
         BFacets alarmData = this.ackRequest.getAlarmData();
         long acknowledgingProcessId = Long.parseLong(((BString)alarmData.getFacet("processId")).getString());
         String s = ((BString)alarmData.getFacet("objectId")).getString();
         StringTokenizer st = new StringTokenizer(s, "_");
         BBacnetObjectIdentifier eventObjectId = BBacnetObjectIdentifier.make(BBacnetObjectType.ordinal(st.nextToken()), Integer.parseInt(st.nextToken()));
         String acknowledgementSource = this.ackRequest.getUser();
         if (acknowledgementSource == null || acknowledgementSource.length() == 0) {
            acknowledgementSource = "Niagara AX:" + Sys.getStation().getStationName();
         }

         BBacnetTimeStamp timeOfAcknowledgement = new BBacnetTimeStamp(this.ackRequest.getAckTime());
         s = ((BString)alarmData.getFacet("deviceId")).getString();
         st = new StringTokenizer(s, "_");
         BBacnetObjectIdentifier deviceId = BBacnetObjectIdentifier.make(BBacnetObjectType.ordinal(st.nextToken()), Integer.parseInt(st.nextToken()));
         BCharacterSetEncoding encoding = bacnet.getLocalDevice().getCharacterSet();
         device = bacnet.doLookupDeviceById(deviceId);
         if (device != null) {
            encoding = device.getCharacterSet();
         }

         BBacnetAddress address = DeviceRegistry.getDeviceAddress(deviceId);
         if (address == null) {
            logger.message("Device ID " + deviceId + " not found in device address table, unable to acknowledge alarm!");
            return;
         }

         BString acksReq = (BString)alarmData.getFacet("bacnetAcksRequired");
         if (acksReq == null) {
            logger.message("No states requiring BACnet acknowledgment for this alarm record!");

            try {
               BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
               this.ackRequest.ackAlarm();
               this.ackRequest.setAckState(BAckState.acked);
               as.routeAlarm(this.ackRequest);
            } catch (ServiceNotFoundException var23) {
               logger.error("AckAlarmRequest.run:Unable to find Alarm Service to clear alarm!", var23);
            }

            return;
         }

         String acksReqStr = acksReq.getString();
         st = new StringTokenizer(acksReqStr, ";");

         while (st.hasMoreTokens()) {
            String ackReqState = st.nextToken();
            int ndx = ackReqState.indexOf("@");
            String eventStateStr = ackReqState.substring(0, ndx);
            String timeStampStr = ackReqState.substring(ndx + 1);
            BBacnetEventState eventStateAcknowledged = BBacnetEventState.make(eventStateStr);
            BBacnetTimeStamp timestamp = BBacnetTimeStamp.fromText(timeStampStr);

            try {
               ((BBacnetStack)bacnet.getBacnetComm())
                  .getClient()
                  .acknowledgeAlarm(
                     address, acknowledgingProcessId, eventObjectId, eventStateAcknowledged, timestamp, acknowledgementSource, timeOfAcknowledgement, encoding
                  );
            } catch (BacnetException var24) {
               logger.error("BacnetException sending AcknowledgeAlarm:" + var24, var24);
            }
         }
      } catch (NullPointerException var25) {
         logger.error("Unable to send Bacnet Ack! Required Bacnet information not found in record!", var25);
      } catch (Exception var26) {
         logger.error("Exception in AckAlarmRequest! -- ACK not received?", var26);
      }
   }
}
