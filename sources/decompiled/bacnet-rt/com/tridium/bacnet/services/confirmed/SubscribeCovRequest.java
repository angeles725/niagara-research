package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class SubscribeCovRequest extends BacnetConfirmedRequest {
   public static final int SUBSCRIBER_PROCESS_ID_TAG = 0;
   public static final int MONITORED_OBJECT_ID_TAG = 1;
   public static final int ISSUE_CONFIRMED_NOTIFICATIONS_TAG = 2;
   public static final int LIFETIME_TAG = 3;
   private long subscriberProcessId;
   private BBacnetObjectIdentifier monitoredObjectId;
   private boolean issueConfirmedNotifications;
   private long lifetime;
   private boolean isCancellation;

   public SubscribeCovRequest() {
      super(5);
   }

   public SubscribeCovRequest(long subscriberProcessId, BBacnetObjectIdentifier monitoredObjectId) {
      this(subscriberProcessId, monitoredObjectId, false, 0L, true);
   }

   public SubscribeCovRequest(long subscriberProcessId, BBacnetObjectIdentifier monitoredObjectId, boolean issueConfirmedNotifications) {
      this(subscriberProcessId, monitoredObjectId, issueConfirmedNotifications, 0L, false);
   }

   public SubscribeCovRequest(long subscriberProcessId, BBacnetObjectIdentifier monitoredObjectId, boolean issueConfirmedNotifications, long lifetime) {
      this(subscriberProcessId, monitoredObjectId, issueConfirmedNotifications, lifetime, false);
   }

   private SubscribeCovRequest(
      long subscriberProcessId, BBacnetObjectIdentifier monitoredObjectId, boolean issueConfirmedNotifications, long lifetime, boolean isCancellation
   ) {
      super(5);
      this.subscriberProcessId = subscriberProcessId;
      this.monitoredObjectId = monitoredObjectId;
      this.issueConfirmedNotifications = issueConfirmedNotifications;
      this.lifetime = lifetime;
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
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.subscriberProcessId = inputStream.readUnsignedInteger(0);
      this.monitoredObjectId = inputStream.readObjectIdentifier(1);
      int tag = inputStream.peekTag();
      if (tag == -1) {
         this.isCancellation = true;
      } else {
         if (!inputStream.isValueTag(2)) {
            throw new RejectException(2);
         }

         this.issueConfirmedNotifications = inputStream.readBoolean(2);
      }

      tag = inputStream.peekTag();
      if (tag == -1) {
         this.lifetime = 0L;
      } else {
         if (!inputStream.isValueTag(3)) {
            throw new RejectException(4);
         }

         try {
            this.lifetime = inputStream.readUnsignedInteger(3);
         } catch (AsnException var5) {
            String cause = var5.getMessage();
            if (cause != null && cause.equalsIgnoreCase("Integer overflow")) {
               throw new RejectException(6);
            }
         }
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

      return sb.toString();
   }
}
