package com.tridium.bacnet.asn;

import java.util.StringTokenizer;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.sys.BEnum;
import javax.baja.sys.BString;

public class NAlarmSummary implements AsnConst {
   private BBacnetObjectIdentifier objectId;
   private BEnum alarmState;
   private BBacnetBitString acknowledgedTransitions;

   public NAlarmSummary() {
   }

   public NAlarmSummary(BBacnetObjectIdentifier objectId, BEnum alarmState, BBacnetBitString acknowledgedTransitions) {
      this.objectId = objectId;
      this.alarmState = alarmState;
      this.acknowledgedTransitions = acknowledgedTransitions;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public BEnum getAlarmState() {
      return this.alarmState;
   }

   public BBacnetBitString getAcknowledgedTransitions() {
      return this.acknowledgedTransitions;
   }

   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(this.objectId);
      out.writeEnumerated(this.alarmState.getOrdinal());
      out.writeBitString(this.acknowledgedTransitions);
   }

   public void readAsn(AsnInput in) throws AsnException {
      this.readAsn(null, in);
   }

   public void readAsn(BBacnetObjectIdentifier deviceId, AsnInput in) throws AsnException {
      this.objectId = in.readObjectIdentifier();
      BExtensibleEnumList enumList = BBacnetNetwork.localDevice().getEnumerationList();
      if (deviceId != null) {
         BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(deviceId);
         if (device != null) {
            enumList = device.getEnumerationList();
         }
      }

      int alarmStateOrdinal = in.readEnumerated();
      this.alarmState = enumList.getEventState().getRange().get(alarmStateOrdinal);
      this.acknowledgedTransitions = in.readBitString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("NAlarmSummary: ");
      sb.append("\n  objectId:" + this.objectId);
      sb.append("\n  alarmState:" + this.alarmState);
      sb.append("\n  ackedTransitions:" + this.acknowledgedTransitions);
      return sb.toString();
   }

   public BString toJob() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.objectId.toString())
         .append('|')
         .append(this.alarmState.toString())
         .append('|')
         .append(this.acknowledgedTransitions.toString(BacnetBitStringUtil.BacnetEventTransitionBits));
      return BString.make(sb.toString());
   }

   public static String fromJob(String jobString) {
      StringTokenizer st = new StringTokenizer(jobString, "|");
      if (st.countTokens() < 3) {
         return "Cannot parse:" + jobString;
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("Object ID:")
            .append(st.nextToken())
            .append("; Alarm State:")
            .append(st.nextToken())
            .append("; Acked Transitions:")
            .append(st.nextToken())
            .append("\n");
         return sb.toString();
      }
   }
}
