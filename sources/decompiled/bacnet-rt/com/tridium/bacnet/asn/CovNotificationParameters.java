package com.tridium.bacnet.asn;

import java.util.ArrayList;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;

public class CovNotificationParameters implements AsnConst {
   public static final int SUBSCRIBER_PROCESS_ID_TAG = 0;
   public static final int INITIATING_DEVICE_ID_TAG = 1;
   public static final int MONITORED_OBJECT_ID_TAG = 2;
   public static final int TIME_REMAINING_TAG = 3;
   public static final int LIST_OF_VALUES_TAG = 4;
   private long subscriberProcessId;
   private BBacnetObjectIdentifier initiatingDeviceId;
   private BBacnetObjectIdentifier monitoredObjectId;
   private long timeRemaining;
   private PropertyValue[] listOfValues;

   public CovNotificationParameters() {
      this.listOfValues = new PropertyValue[0];
   }

   public CovNotificationParameters(
      long subscriberProcessId,
      BBacnetObjectIdentifier initiatingDeviceId,
      BBacnetObjectIdentifier monitoredObjectId,
      long timeRemaining,
      PropertyValue[] listOfValues
   ) {
      this.subscriberProcessId = subscriberProcessId;
      this.initiatingDeviceId = initiatingDeviceId;
      this.monitoredObjectId = monitoredObjectId;
      this.timeRemaining = timeRemaining;
      this.listOfValues = listOfValues;
   }

   public long getSubscriberProcessId() {
      return this.subscriberProcessId;
   }

   public BBacnetObjectIdentifier getInitiatingDeviceId() {
      return this.initiatingDeviceId;
   }

   public BBacnetObjectIdentifier getMonitoredObjectId() {
      return this.monitoredObjectId;
   }

   public long getTimeRemaining() {
      return this.timeRemaining;
   }

   public PropertyValue[] getListOfValues() {
      return this.listOfValues;
   }

   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeUnsignedInteger(0, this.subscriberProcessId);
      outputStream.writeObjectIdentifier(1, this.initiatingDeviceId);
      outputStream.writeObjectIdentifier(2, this.monitoredObjectId);
      outputStream.writeUnsignedInteger(3, this.timeRemaining);
      outputStream.writeOpeningTag(4);

      for (int i = 0; i < this.listOfValues.length; i++) {
         this.listOfValues[i].writeAsn(outputStream);
      }

      outputStream.writeClosingTag(4);
   }

   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.subscriberProcessId = inputStream.readUnsignedInteger(0);
      this.initiatingDeviceId = inputStream.readObjectIdentifier(1);
      this.monitoredObjectId = inputStream.readObjectIdentifier(2);
      this.timeRemaining = inputStream.readUnsignedInteger(3);
      inputStream.peekTag();
      if (inputStream.isOpeningTag(4)) {
         inputStream.skipTag();
         ArrayList v = new ArrayList();
         inputStream.peekTag();

         while (!inputStream.isClosingTag(4)) {
            NBacnetPropertyValue value = new NBacnetPropertyValue();
            value.readAsn(inputStream);
            v.add(value);
            if (inputStream.peekTag() == -1) {
               throw new AsnException("No closing tag");
            }
         }

         inputStream.skipTag();
         this.listOfValues = v.toArray(new PropertyValue[0]);
      } else {
         throw new RejectException(5);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(256);
      sb.append("\n  subscriberProcessId: " + this.subscriberProcessId);
      sb.append("\n  initiatingDeviceId:  " + this.initiatingDeviceId);
      sb.append("\n  monitoredObjectId:   " + this.monitoredObjectId);
      sb.append("\n  timeRemaining:       " + this.timeRemaining);
      sb.append("\n  listOfValues:        " + this.listOfValues.length + " elements");

      for (int i = 0; i < this.listOfValues.length; i++) {
         sb.append("\n    listOfValues[" + i + "]: " + this.listOfValues[i]);
      }

      return sb.toString();
   }
}
