package com.tridium.bacnet.asn;

import java.util.StringTokenizer;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.sys.BEnum;
import javax.baja.sys.BString;

public class NEventSummary implements AsnConst {
   public static final int OBJECT_ID_TAG = 0;
   public static final int EVENT_STATE_TAG = 1;
   public static final int ACKNOWLEDGED_TRANSITIONS_TAG = 2;
   public static final int EVENT_TIME_STAMPS_TAG = 3;
   public static final int NOTIFY_TYPE_TAG = 4;
   public static final int EVENT_ENABLE_TAG = 5;
   public static final int EVENT_PRIORITIES_TAG = 6;
   private BBacnetObjectIdentifier objectId;
   private BEnum eventState;
   private BBacnetBitString acknowledgedTransitions;
   private BBacnetTimeStamp[] eventTimeStamps;
   private BBacnetNotifyType notifyType;
   private BBacnetBitString eventEnable;
   private int[] eventPriorities;

   public NEventSummary() {
   }

   public NEventSummary(
      BBacnetObjectIdentifier objectId,
      BEnum eventState,
      BBacnetBitString acknowledgedTransitions,
      BBacnetTimeStamp[] eventTimeStamps,
      BBacnetNotifyType notifyType,
      BBacnetBitString eventEnable,
      int[] eventPriorities
   ) {
      this.objectId = objectId;
      this.eventState = eventState;
      this.acknowledgedTransitions = acknowledgedTransitions;
      this.eventTimeStamps = eventTimeStamps;
      this.notifyType = notifyType;
      this.eventEnable = eventEnable;
      this.eventPriorities = eventPriorities;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public BEnum getEventState() {
      return this.eventState;
   }

   public BBacnetBitString getAcknowledgedTransitions() {
      return this.acknowledgedTransitions;
   }

   public BBacnetTimeStamp[] getEventTimeStamps() {
      return this.eventTimeStamps;
   }

   public BBacnetNotifyType getNotifyType() {
      return this.notifyType;
   }

   public BBacnetBitString getEventEnable() {
      return this.eventEnable;
   }

   public int[] getEventPriorities() {
      return this.eventPriorities;
   }

   public int getEncodedSize() {
      int encodedSize = 23;
      encodedSize += this.getEventState().getOrdinal() > 255 ? 3 : 2;

      for (int i = 0; i < 3; i++) {
         switch (this.eventTimeStamps[i].getChoice()) {
            case 0:
               encodedSize += 5;
               break;
            case 1:
               encodedSize += 1 + this.getEncodedIntegerSize(this.eventTimeStamps[i].getSequenceNumber().getUnsigned());
               break;
            case 2:
               encodedSize += 12;
         }
      }

      return encodedSize;
   }

   private int getEncodedIntegerSize(long unsignedInt) {
      if (unsignedInt <= 255L) {
         return 1;
      } else if (unsignedInt <= 65535L) {
         return 2;
      } else {
         return unsignedInt <= 16777215L ? 3 : 4;
      }
   }

   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.objectId);
      out.writeEnumerated(1, this.eventState.getOrdinal());
      out.writeBitString(2, this.acknowledgedTransitions);
      out.writeOpeningTag(3);

      for (int i = 0; i < 3; i++) {
         this.eventTimeStamps[i].writeAsn(out);
      }

      out.writeClosingTag(3);
      out.writeEnumerated(4, this.notifyType.getOrdinal());
      out.writeBitString(5, this.eventEnable);
      out.writeOpeningTag(6);

      for (int i = 0; i < 3; i++) {
         out.writeUnsignedInteger(this.eventPriorities[i]);
      }

      out.writeClosingTag(6);
   }

   public void readAsn(AsnInput in) throws AsnException {
      this.readAsn(null, in);
   }

   public void readAsn(BBacnetObjectIdentifier deviceId, AsnInput in) throws AsnException {
      this.objectId = in.readObjectIdentifier(0);
      BExtensibleEnumList enumList = BBacnetNetwork.localDevice().getEnumerationList();
      if (deviceId != null) {
         BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(deviceId);
         if (device != null) {
            enumList = device.getEnumerationList();
         }
      }

      int eventStateOrdinal = in.readEnumerated(1);
      this.eventState = enumList.getEventState().getRange().get(eventStateOrdinal);
      this.acknowledgedTransitions = in.readBitString(2);
      int tag = in.peekTag();
      if (!in.isOpeningTag(3)) {
         throw new AsnException("Invalid tag: " + tag);
      } else {
         in.skipTag();
         this.eventTimeStamps = new BBacnetTimeStamp[3];

         for (int i = 0; i < 3; i++) {
            this.eventTimeStamps[i] = new BBacnetTimeStamp();
            this.eventTimeStamps[i].readAsn(in);
         }

         in.skipTag();
         this.notifyType = BBacnetNotifyType.make(in.readEnumerated(4));
         this.eventEnable = in.readBitString(5);
         tag = in.peekTag();
         if (!in.isOpeningTag(6)) {
            throw new AsnException("Invalid tag: " + tag);
         } else {
            in.skipTag();
            this.eventPriorities = new int[3];

            for (int var9 = 0; var9 < 3; var9++) {
               this.eventPriorities[var9] = in.readUnsignedInt();
            }

            in.skipTag();
         }
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("NEventSummary: ");
      sb.append("\n  objectId:" + this.objectId);
      sb.append("\n  eventState:" + this.eventState);
      sb.append("\n  ackedTransitions:" + this.acknowledgedTransitions);
      sb.append("\n  eventTimeStamps:");

      for (int i = 0; i < 3; i++) {
         sb.append("\n    " + this.eventTimeStamps[i]);
      }

      sb.append("\n  notifyType:" + this.notifyType);
      sb.append("\n  eventEnable:" + this.eventEnable);
      sb.append("\n  eventPriorities:");

      for (int i = 0; i < 3; i++) {
         sb.append("  " + this.eventPriorities[i]);
      }

      sb.append("\n");
      return sb.toString();
   }

   public BString toJob() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.objectId.toString())
         .append('|')
         .append(this.eventState.toString())
         .append('|')
         .append(this.acknowledgedTransitions.toString(BacnetBitStringUtil.BacnetEventTransitionBits))
         .append('|')
         .append(this.eventTimeStamps[0])
         .append('|')
         .append(this.eventTimeStamps[1])
         .append('|')
         .append(this.eventTimeStamps[2])
         .append('|')
         .append(this.notifyType.toString())
         .append('|')
         .append(this.eventEnable.toString())
         .append('|')
         .append(this.eventPriorities[0])
         .append('|')
         .append(this.eventPriorities[1])
         .append('|')
         .append(this.eventPriorities[2]);
      return BString.make(sb.toString());
   }

   public static String fromJob(String jobString) {
      StringTokenizer st = new StringTokenizer(jobString, "|");
      if (st.countTokens() < 11) {
         return "Cannot parse:" + jobString;
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("Object ID:")
            .append(st.nextToken())
            .append("; Event State:")
            .append(st.nextToken())
            .append("; Acked Transitions:")
            .append(st.nextToken())
            .append("\n  ");
         sb.append("Event Time Stamps:").append(st.nextToken()).append("; ").append(st.nextToken()).append("; ").append(st.nextToken()).append("\n  ");
         sb.append("Notify Type:").append(st.nextToken()).append("; Event Enable:").append(st.nextToken()).append("\n  ");
         sb.append("Event Priorities:").append(st.nextToken()).append("; ").append(st.nextToken()).append("; ").append(st.nextToken()).append("\n");
         return sb.toString();
      }
   }
}
