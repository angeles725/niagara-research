package com.tridium.bacnet.asn;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BEnum;

public class EventNotificationParameters {
   public static final int PROCESS_ID_TAG = 0;
   public static final int INITIATING_DEVICE_ID_TAG = 1;
   public static final int EVENT_OBJECT_ID_TAG = 2;
   public static final int TIMESTAMP_TAG = 3;
   public static final int NOTIFICATION_CLASS_TAG = 4;
   public static final int PRIORITY_TAG = 5;
   public static final int EVENT_TYPE_TAG = 6;
   public static final int MESSAGE_TEXT_TAG = 7;
   public static final int NOTIFY_TYPE_TAG = 8;
   public static final int ACK_REQUIRED_TAG = 9;
   public static final int FROM_STATE_TAG = 10;
   public static final int TO_STATE_TAG = 11;
   public static final int EVENT_VALUES_TAG = 12;
   private long processId;
   private BBacnetObjectIdentifier initiatingDeviceId;
   private BBacnetObjectIdentifier eventObjectId;
   private BBacnetTimeStamp timeStamp;
   private long notificationClass;
   private int priority;
   private BEnum eventType;
   private String messageText;
   private BBacnetNotifyType notifyType;
   private boolean ackRequired;
   private BEnum fromState;
   private BEnum toState;
   private BacnetNotificationParameters eventValues;
   private byte[] rawEventValues;
   private BCharacterSetEncoding encoding;
   private static final Logger logger = Logger.getLogger("bacnet.asn");

   public EventNotificationParameters() {
   }

   public EventNotificationParameters(
      long processId,
      BBacnetObjectIdentifier initiatingDeviceId,
      BBacnetObjectIdentifier eventObjectId,
      BBacnetTimeStamp timeStamp,
      long notificationClass,
      int priority,
      BEnum eventType,
      String messageText,
      BBacnetNotifyType notifyType,
      boolean ackRequired,
      BEnum fromState,
      BEnum toState,
      BacnetNotificationParameters eventValues,
      BCharacterSetEncoding encoding
   ) {
      this.processId = processId;
      this.initiatingDeviceId = initiatingDeviceId;
      this.eventObjectId = eventObjectId;
      this.timeStamp = timeStamp;
      this.notificationClass = notificationClass;
      this.priority = priority;
      this.eventType = eventType;
      this.messageText = messageText;
      this.notifyType = notifyType;
      this.ackRequired = ackRequired;
      this.fromState = fromState;
      this.toState = toState;
      this.eventValues = eventValues;
      this.encoding = encoding;
   }

   public EventNotificationParameters(EventNotificationParameters enp) {
      this.processId = enp.processId;
      this.initiatingDeviceId = enp.initiatingDeviceId;
      this.eventObjectId = enp.eventObjectId;
      this.timeStamp = (BBacnetTimeStamp)enp.timeStamp.newCopy();
      this.notificationClass = enp.notificationClass;
      this.priority = enp.priority;
      this.eventType = enp.eventType;
      this.messageText = enp.messageText;
      this.notifyType = enp.notifyType;
      this.ackRequired = enp.ackRequired;
      this.fromState = enp.fromState;
      this.toState = enp.toState;
      this.eventValues = BacnetNotificationParameters.make(enp.eventValues);
      this.encoding = enp.encoding;
   }

   public long getProcessId() {
      return this.processId;
   }

   public BBacnetObjectIdentifier getInitiatingDeviceId() {
      return this.initiatingDeviceId;
   }

   public BBacnetObjectIdentifier getEventObjectId() {
      return this.eventObjectId;
   }

   public BBacnetTimeStamp getTimeStamp() {
      return this.timeStamp;
   }

   public long getNotificationClass() {
      return this.notificationClass;
   }

   public int getPriority() {
      return this.priority;
   }

   public BEnum getEventType() {
      return this.eventType;
   }

   public String getMessageText() {
      return this.messageText;
   }

   public BBacnetNotifyType getNotifyType() {
      return this.notifyType;
   }

   public boolean getAckRequired() {
      return this.ackRequired;
   }

   public BEnum getFromState() {
      return this.fromState;
   }

   public BEnum getToState() {
      return this.toState;
   }

   public BacnetNotificationParameters getEventValues() {
      return this.eventValues;
   }

   public byte[] getRawEventValues() {
      return this.rawEventValues;
   }

   public BCharacterSetEncoding getEncoding() {
      return this.encoding;
   }

   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeUnsignedInteger(0, this.processId);
      outputStream.writeObjectIdentifier(1, this.initiatingDeviceId);
      outputStream.writeObjectIdentifier(2, this.eventObjectId);
      outputStream.writeOpeningTag(3);
      this.timeStamp.writeAsn(outputStream);
      outputStream.writeClosingTag(3);
      outputStream.writeUnsignedInteger(4, this.notificationClass);
      outputStream.writeUnsignedInteger(5, this.priority);
      outputStream.writeEnumerated(6, this.eventType.getOrdinal());
      if (this.messageText != null) {
         outputStream.writeCharacterString(7, this.messageText, this.encoding);
      }

      outputStream.writeEnumerated(8, this.notifyType.getOrdinal());
      boolean ackNotification = this.notifyType.getOrdinal() == 2;
      if (!ackNotification) {
         outputStream.writeBoolean(9, this.ackRequired);
      }

      if (!ackNotification && this.fromState != null) {
         outputStream.writeEnumerated(10, this.fromState.getOrdinal());
      }

      outputStream.writeEnumerated(11, this.toState.getOrdinal());
      if (!ackNotification) {
         if (this.rawEventValues != null) {
            outputStream.writeEncodedValue(12, this.rawEventValues);
         } else if (this.eventValues != null) {
            outputStream.writeOpeningTag(12);
            this.eventValues.writeEncoded(outputStream);
            outputStream.writeClosingTag(12);
         }
      }
   }

   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.processId = inputStream.readUnsignedInteger(0);
      this.initiatingDeviceId = inputStream.readObjectIdentifier(1);
      this.eventObjectId = inputStream.readObjectIdentifier(2);
      BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(this.initiatingDeviceId);
      BExtensibleEnumList enumList = BBacnetNetwork.localDevice().getEnumerationList();
      if (device != null) {
         enumList = device.getEnumerationList();
      }

      inputStream.skipTag();
      this.timeStamp = new BBacnetTimeStamp();
      this.timeStamp.readAsn(inputStream);
      inputStream.skipTag();
      this.notificationClass = inputStream.readUnsignedInteger(4);
      this.priority = inputStream.readUnsignedInt(5);
      int eventTypeOrdinal = inputStream.readEnumerated(6);
      this.eventType = enumList.getEventType().getRange().get(eventTypeOrdinal);
      inputStream.peekTag();
      if (inputStream.isValueTag(7)) {
         this.encoding = inputStream.peekEncoding(7);
         this.messageText = readMessageText(inputStream);
      } else {
         this.messageText = null;
      }

      this.notifyType = BBacnetNotifyType.make(inputStream.readEnumerated(8));
      inputStream.peekTag();
      if (inputStream.isValueTag(9)) {
         this.ackRequired = inputStream.readBoolean(9);
         inputStream.peekTag();
      } else {
         this.ackRequired = false;
      }

      if (inputStream.isValueTag(10)) {
         int fromStateOrdinal = inputStream.readEnumerated(10);
         this.fromState = enumList.getEventState().getRange().get(fromStateOrdinal);
         inputStream.peekTag();
      } else {
         this.fromState = null;
      }

      if (inputStream.isValueTag(11)) {
         int toStateOrdinal = inputStream.readEnumerated(11);
         this.toState = enumList.getEventState().getRange().get(toStateOrdinal);
         inputStream.peekTag();
      } else {
         this.toState = BBacnetEventState.normal;
      }

      if (inputStream.isOpeningTag(12)) {
         inputStream.mark(0);
         this.rawEventValues = inputStream.readEncodedValue(12);
         inputStream.reset();
         inputStream.skipTag();
         this.eventValues = BacnetNotificationParameters.parseEncoded(this.initiatingDeviceId, inputStream);
         inputStream.skipTag();
      } else {
         this.eventValues = null;
         this.rawEventValues = null;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(256);
      sb.append("\n  processId:         " + this.processId);
      sb.append("\n  initiatingDeviceId:" + this.initiatingDeviceId);
      sb.append("\n  eventObjectId:     " + this.eventObjectId);
      sb.append("\n  timeStamp:         " + this.timeStamp);
      sb.append("\n  notificationClass: " + this.notificationClass);
      sb.append("\n  priority:          " + this.priority);
      sb.append("\n  eventType:         " + this.eventType);
      sb.append("\n  messageText:       " + this.messageText);
      sb.append("\n  notifyType:        " + this.notifyType);
      sb.append("\n  ackRequired:       " + this.ackRequired);
      sb.append("\n  fromState:         " + this.fromState);
      sb.append("\n  toState:           " + this.toState);
      sb.append("\n  eventValues:       " + this.eventValues);
      sb.append("\n  encoding:          " + this.encoding);
      return sb.toString();
   }

   private static String readMessageText(AsnInputStream inputStream) throws AsnException {
      try {
         inputStream.mark(-1);
         return inputStream.readCharacterString(7);
      } catch (Exception var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Exception occurred during decoding of messageText", (Throwable)var2);
         }

         inputStream.reset();
         inputStream.skipCharacterString(7);
         return null;
      }
   }
}
