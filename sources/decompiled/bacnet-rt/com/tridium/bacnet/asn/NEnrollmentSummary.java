package com.tridium.bacnet.asn;

import java.util.StringTokenizer;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.sys.BEnum;
import javax.baja.sys.BString;

public class NEnrollmentSummary implements AsnConst {
   private BBacnetObjectIdentifier objectId;
   private BEnum eventType;
   private BEnum eventState;
   private int priority = -1;
   private long notificationClass = -1L;

   public NEnrollmentSummary() {
   }

   public NEnrollmentSummary(BBacnetObjectIdentifier objectId, BEnum eventType, BEnum eventState, int priority, long notificationClass) {
      this.objectId = objectId;
      this.eventType = eventType;
      this.eventState = eventState;
      this.priority = priority;
      this.notificationClass = notificationClass;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public BEnum getEventType() {
      return this.eventType;
   }

   public BEnum getEventState() {
      return this.eventState;
   }

   public int getPriority() {
      return this.priority;
   }

   public long getNotificationClass() {
      return this.notificationClass;
   }

   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(this.objectId);
      out.writeEnumerated(this.eventType.getOrdinal());
      out.writeEnumerated(this.eventState.getOrdinal());
      out.writeUnsignedInteger(this.priority);
      if (this.notificationClass >= 0L) {
         out.writeUnsignedInteger(this.notificationClass);
      }
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

      int eventTypeOrdinal = in.readEnumerated();
      this.eventType = enumList.getEventType().getRange().get(eventTypeOrdinal);
      int eventStateOrdinal = in.readEnumerated();
      this.eventState = enumList.getEventState().getRange().get(eventStateOrdinal);
      this.priority = in.readUnsignedInt();
      int tag = in.peekTag();
      if (tag != -1 && in.isValueTag(2)) {
         this.notificationClass = in.readUnsignedInteger();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("NEnrollmentSummary: ");
      sb.append("\n  objectId:" + this.objectId);
      sb.append("\n  eventType:" + this.eventType);
      sb.append("\n  eventState:" + this.eventState);
      sb.append("\n  priority:" + this.priority);
      sb.append("\n  notificationClass:" + this.notificationClass);
      return sb.toString();
   }

   public BString toJob() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.objectId.toString())
         .append('|')
         .append(this.eventType.toString())
         .append('|')
         .append(this.eventState.toString())
         .append('|')
         .append(this.priority)
         .append('|')
         .append(this.notificationClass)
         .append('|');
      return BString.make(sb.toString());
   }

   public static String fromJob(String jobString) {
      StringTokenizer st = new StringTokenizer(jobString, "|");
      if (st.countTokens() < 5) {
         return "Cannot parse:" + jobString;
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append("Object ID:")
            .append(st.nextToken())
            .append("; Event Type:")
            .append(st.nextToken())
            .append("; Event State:")
            .append(st.nextToken())
            .append("; Priority:")
            .append(st.nextToken())
            .append("; Notification Class:")
            .append(st.nextToken())
            .append("\n");
         return sb.toString();
      }
   }
}
