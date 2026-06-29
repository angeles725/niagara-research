package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.datatypes.BPriorityFilter;
import com.tridium.bacnet.enums.BAcknowledgmentFilter;
import com.tridium.bacnet.enums.BEventStateFilter;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetRecipientProcess;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.sys.BEnum;

public class GetEnrollmentSummaryRequest extends BacnetConfirmedRequest {
   public static final int ACKNOWLEDGMENT_FILTER_TAG = 0;
   public static final int ENROLLMENT_FILTER_TAG = 1;
   public static final int EVENT_STATE_FILTER_TAG = 2;
   public static final int EVENT_TYPE_FILTER_TAG = 3;
   public static final int PRIORITY_FILTER_TAG = 4;
   public static final int NOTIFICATION_CLASS_FILTER_TAG = 5;
   public static final int MIN_PRIORITY_TAG = 0;
   public static final int MAX_PRIORITY_TAG = 1;
   private BAcknowledgmentFilter acknowledgmentFilter;
   private BBacnetRecipientProcess enrollmentFilter;
   private BEventStateFilter eventStateFilter;
   private BEnum eventTypeFilter;
   private BPriorityFilter priorityFilter;
   private long notificationClassFilter = -1L;

   public GetEnrollmentSummaryRequest() {
      super(4);
   }

   public GetEnrollmentSummaryRequest(
      int ackFilter, BBacnetRecipientProcess enrollmentFilter, int esFilter, BEnum eventTypeFilter, int[] priFilter, long notificationClassFilter
   ) {
      super(4);
      this.acknowledgmentFilter = BAcknowledgmentFilter.make(ackFilter);
      this.enrollmentFilter = enrollmentFilter;
      if (esFilter >= 0) {
         this.eventStateFilter = BEventStateFilter.make(esFilter);
      }

      this.eventTypeFilter = eventTypeFilter;
      if (priFilter != null && priFilter.length >= 2) {
         this.priorityFilter = new BPriorityFilter(priFilter[0], priFilter[1]);
      }

      this.notificationClassFilter = notificationClassFilter;
   }

   public GetEnrollmentSummaryRequest(
      BAcknowledgmentFilter acknowledgmentFilter,
      BBacnetRecipientProcess enrollmentFilter,
      BEventStateFilter eventStateFilter,
      BEnum eventTypeFilter,
      BPriorityFilter priorityFilter,
      long notificationClassFilter
   ) {
      super(4);
      this.acknowledgmentFilter = acknowledgmentFilter;
      this.enrollmentFilter = enrollmentFilter;
      this.eventStateFilter = eventStateFilter;
      this.eventTypeFilter = eventTypeFilter;
      this.priorityFilter = priorityFilter;
      this.notificationClassFilter = notificationClassFilter;
   }

   public boolean isEnrollmentFilter() {
      return this.enrollmentFilter != null;
   }

   public boolean isEventStateFilter() {
      return this.eventStateFilter != null;
   }

   public boolean isEventTypeFilter() {
      return this.eventTypeFilter != null;
   }

   public boolean isPriorityFilter() {
      return this.priorityFilter != null;
   }

   public boolean isNotificationClassFilter() {
      return this.notificationClassFilter >= 0L;
   }

   public BAcknowledgmentFilter getAcknowledgmentFilter() {
      return this.acknowledgmentFilter;
   }

   public void setAcknowledgmentFilter(BAcknowledgmentFilter acknowledgmentFilter) {
      this.acknowledgmentFilter = acknowledgmentFilter;
   }

   public BBacnetRecipientProcess getEnrollmentFilter() {
      return this.enrollmentFilter;
   }

   public void setEnrollmentFilter(BBacnetRecipientProcess enrollmentFilter) {
      this.enrollmentFilter = enrollmentFilter;
   }

   public BEventStateFilter getEventStateFilter() {
      return this.eventStateFilter;
   }

   public void setEventStateFilter(BEventStateFilter eventStateFilter) {
      this.eventStateFilter = eventStateFilter;
   }

   public BEnum getEventTypeFilter() {
      return this.eventTypeFilter;
   }

   public void setEventTypeFilter(BEnum eventTypeFilter) {
      this.eventTypeFilter = eventTypeFilter;
   }

   public BPriorityFilter getPriorityFilter() {
      return this.priorityFilter;
   }

   public void setPriorityFilter(BPriorityFilter priorityFilter) {
      this.priorityFilter = priorityFilter;
   }

   public long getNotificationClassFilter() {
      return this.notificationClassFilter;
   }

   public void setNotificationClassFilter(long notificationClassFilter) {
      this.notificationClassFilter = notificationClassFilter;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeEnumerated(0, this.acknowledgmentFilter);
      if (this.enrollmentFilter != null) {
         out.writeOpeningTag(1);
         this.enrollmentFilter.writeAsn(out);
         out.writeClosingTag(1);
      }

      if (this.eventStateFilter != null) {
         out.writeEnumerated(2, this.eventStateFilter);
      }

      if (this.eventTypeFilter != null) {
         out.writeEnumerated(3, this.eventTypeFilter);
      }

      if (this.priorityFilter != null) {
         out.writeOpeningTag(4);
         out.writeUnsignedInteger(0, this.priorityFilter.getMinPriority());
         out.writeUnsignedInteger(1, this.priorityFilter.getMaxPriority());
         out.writeClosingTag(4);
      }

      if (this.notificationClassFilter >= 0L) {
         out.writeUnsignedInteger(5, this.notificationClassFilter);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException, RejectException {
      this.readEncoded(null, in);
   }

   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream in) throws AsnException, RejectException {
      BExtensibleEnumList enumList = BBacnetNetwork.localDevice().getEnumerationList();
      if (deviceId != null) {
         BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(deviceId);
         if (device != null) {
            enumList = device.getEnumerationList();
         }
      }

      this.acknowledgmentFilter = BAcknowledgmentFilter.make(in.readEnumerated(0));
      int tag = in.peekTag();
      if (in.isOpeningTag(1)) {
         in.skipTag();
         this.enrollmentFilter = new BBacnetRecipientProcess();
         this.enrollmentFilter.readAsn(in);
         in.skipTag();
         tag = in.peekTag();
      }

      if (in.isValueTag(2)) {
         this.eventStateFilter = BEventStateFilter.make(in.readEnumerated(2));
         tag = in.peekTag();
      }

      if (in.isValueTag(3)) {
         int eventTypeOrdinal = in.readEnumerated(3);
         this.eventTypeFilter = enumList.getEventType().getRange().get(eventTypeOrdinal);
         tag = in.peekTag();
      }

      if (in.isOpeningTag(4)) {
         in.skipTag();
         this.priorityFilter = new BPriorityFilter(in.readUnsignedInt(0), in.readUnsignedInt(1));
         in.skipTag();
         tag = in.peekTag();
      }

      if (in.isValueTag(5)) {
         this.notificationClassFilter = in.readUnsignedInteger(5);
      } else if (tag != -1) {
         throw new RejectException(4);
      }
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new GetEnrollmentSummaryAck(serviceChoice, inputStream);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("GetEnrollmentSummaryRequest: ");
      sb.append("\n  " + this.acknowledgmentFilter);
      sb.append("\n  " + this.enrollmentFilter);
      sb.append("\n  " + this.eventStateFilter);
      sb.append("\n  " + this.eventTypeFilter);
      sb.append("\n  " + this.priorityFilter);
      sb.append("\n  " + this.notificationClassFilter);
      return sb.toString();
   }
}
