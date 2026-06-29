package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.sys.BDouble;

public class SubscribeCovPropertyRequest extends BacnetConfirmedRequest {
   public static final int SUBSCRIBER_PROCESS_ID_TAG = 0;
   public static final int MONITORED_OBJECT_ID_TAG = 1;
   public static final int ISSUE_CONFIRMED_NOTIFICATIONS_TAG = 2;
   public static final int LIFETIME_TAG = 3;
   public static final int MONITORED_PROPERTY_ID_TAG = 4;
   public static final int COV_INCREMENT_TAG = 5;
   private long subscriberProcessId;
   private BBacnetObjectIdentifier monitoredObjectId;
   private boolean issueConfirmedNotifications;
   private long lifetime;
   private boolean isCancellation;
   private PropertyReference monitoredPropertyId;
   private BDouble covIncrement;

   public SubscribeCovPropertyRequest() {
      super(28);
   }

   public SubscribeCovPropertyRequest(long subscriberProcessId, BBacnetObjectIdentifier monitoredObjectId, PropertyReference monitoredPropertyId) {
      this(subscriberProcessId, monitoredObjectId, false, 0L, monitoredPropertyId, null, true);
   }

   public SubscribeCovPropertyRequest(
      long subscriberProcessId,
      BBacnetObjectIdentifier monitoredObjectId,
      boolean issueConfirmedNotifications,
      PropertyReference monitoredPropertyId,
      BDouble covIncrement
   ) {
      this(subscriberProcessId, monitoredObjectId, issueConfirmedNotifications, 0L, monitoredPropertyId, covIncrement, false);
   }

   public SubscribeCovPropertyRequest(
      long subscriberProcessId,
      BBacnetObjectIdentifier monitoredObjectId,
      boolean issueConfirmedNotifications,
      long lifetime,
      PropertyReference monitoredPropertyId,
      BDouble covIncrement
   ) {
      this(subscriberProcessId, monitoredObjectId, issueConfirmedNotifications, lifetime, monitoredPropertyId, covIncrement, false);
   }

   private SubscribeCovPropertyRequest(
      long subscriberProcessId,
      BBacnetObjectIdentifier monitoredObjectId,
      boolean issueConfirmedNotifications,
      long lifetime,
      PropertyReference monitoredPropertyId,
      BDouble covIncrement,
      boolean isCancellation
   ) {
      super(28);
      this.subscriberProcessId = subscriberProcessId;
      this.monitoredObjectId = monitoredObjectId;
      this.issueConfirmedNotifications = issueConfirmedNotifications;
      this.lifetime = lifetime;
      this.monitoredPropertyId = monitoredPropertyId;
      this.covIncrement = covIncrement;
      this.isCancellation = isCancellation;
   }

   public long getSubscriberProcessId() {
      return this.subscriberProcessId;
   }

   public void setSubscriberProcessId(long subscriberProcessId) {
      this.subscriberProcessId = subscriberProcessId;
   }

   public BBacnetObjectIdentifier getMonitoredObjectId() {
      return this.monitoredObjectId;
   }

   public void setMonitoredObjectId(BBacnetObjectIdentifier monitoredObjectId) {
      this.monitoredObjectId = monitoredObjectId;
   }

   public boolean getIssueConfirmedNotifications() {
      return this.issueConfirmedNotifications;
   }

   public void setIssueConfirmedNotifications(boolean issueConfirmedNotifications) {
      this.issueConfirmedNotifications = issueConfirmedNotifications;
   }

   public long getLifetime() {
      return this.lifetime;
   }

   public void setLifetime(long lifetime) {
      this.lifetime = lifetime;
   }

   public PropertyReference getMonitoredPropertyId() {
      return this.monitoredPropertyId;
   }

   public void setMonitoredPropertyId(PropertyReference monitoredPropertyId) {
      this.monitoredPropertyId = monitoredPropertyId;
   }

   public int getMonitoredPropertyIdentifier() {
      return this.monitoredPropertyId.getPropertyId();
   }

   public int getMonitoredPropertyArrayIndex() {
      return this.monitoredPropertyId.getPropertyArrayIndex();
   }

   public BDouble getCovIncrement() {
      return this.covIncrement;
   }

   public void setCovIncrement(BDouble covIncrement) {
      this.covIncrement = covIncrement;
   }

   public boolean isCancellation() {
      return this.isCancellation;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeUnsignedInteger(0, this.subscriberProcessId);
      outputStream.writeObjectIdentifier(1, this.monitoredObjectId);
      if (!this.isCancellation) {
         outputStream.writeBoolean(2, this.issueConfirmedNotifications);
         outputStream.writeUnsignedInteger(3, this.lifetime);
      }

      outputStream.writeOpeningTag(4);
      this.monitoredPropertyId.writeAsn(outputStream);
      outputStream.writeClosingTag(4);
      if (this.covIncrement != null) {
         outputStream.writeReal(5, this.covIncrement.getFloat());
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.subscriberProcessId = inputStream.readUnsignedInteger(0);
      this.monitoredObjectId = inputStream.readObjectIdentifier(1);
      inputStream.peekTag();
      if (inputStream.isOpeningTag(4)) {
         this.isCancellation = true;
      } else {
         if (!inputStream.isValueTag(2)) {
            throw new RejectException(2);
         }

         this.issueConfirmedNotifications = inputStream.readBoolean(2);
      }

      inputStream.peekTag();
      if (inputStream.isOpeningTag(4)) {
         this.lifetime = 0L;
      } else {
         if (!inputStream.isValueTag(3)) {
            throw new RejectException(4);
         }

         this.lifetime = inputStream.readUnsignedInteger(3);
      }

      inputStream.peekTag();
      if (inputStream.isOpeningTag(4)) {
         inputStream.skipTag();
         this.monitoredPropertyId = new NBacnetPropertyReference();
         this.monitoredPropertyId.readAsn(inputStream);
         inputStream.skipTag();
      }

      inputStream.peekTag();
      if (inputStream.isValueTag(5)) {
         this.covIncrement = BDouble.make(inputStream.readReal(5));
      } else {
         this.covIncrement = null;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("SubscribeCovRequest: ");
      sb.append("\n processId: " + this.subscriberProcessId);
      sb.append("\n objectId: " + this.monitoredObjectId);
      if (this.isCancellation) {
         sb.append("\n CANCELLATION");
      } else {
         sb.append("\n issueConf: " + this.issueConfirmedNotifications);
         sb.append("\n lifetime: " + this.lifetime);
      }

      sb.append("\n propertyId: " + this.monitoredPropertyId);
      if (this.covIncrement != null) {
         sb.append("\n covIncrement: " + this.covIncrement);
      }

      return sb.toString();
   }
}
