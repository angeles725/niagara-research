package com.tridium.bacnet.stack.client;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import com.tridium.bacnet.asn.EventNotificationParameters;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NBacnetPropertyStates;
import com.tridium.bacnet.asn.notificationParameters.BufferReady;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfCharacterString;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfReliability;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfState;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfStatusFlags;
import com.tridium.bacnet.asn.notificationParameters.CommandFailure;
import com.tridium.bacnet.asn.notificationParameters.DoubleOutOfRange;
import com.tridium.bacnet.asn.notificationParameters.FloatingLimit;
import com.tridium.bacnet.asn.notificationParameters.OutOfRange;
import com.tridium.bacnet.asn.notificationParameters.SignedOutOfRange;
import com.tridium.bacnet.asn.notificationParameters.UnsignedOutOfRange;
import com.tridium.bacnet.asn.notificationParameters.UnsignedRange;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.DeviceRegistry;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmDbConnection;
import javax.baja.alarm.BAckState;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BSourceState;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.alarm.BBacnetStatusAlgorithm;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetAny;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetPropertyValue;
import javax.baja.bacnet.datatypes.BBacnetRecipient;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetBinaryPv;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.export.BBacnetEventEnrollmentDescriptor;
import javax.baja.bacnet.export.BBacnetEventSource;
import javax.baja.bacnet.export.BBacnetNotificationClassDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.driver.BDevice;
import javax.baja.naming.BOrd;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;

public class AsyncEventNotificationRequest implements Runnable, BacnetAlarmConst {
   private final BAlarmRecord alarm;
   private final BComponent eventObject;
   private final long processId;
   private final BBacnetRecipient recipient;
   private final boolean useConfirmed;
   private final boolean ackAlarmAndNormal;
   private BBacnetEventSource eventSrc;
   private final BFacets alarmData;
   private BBacnetAddress address;
   private final BLocalBacnetDevice local = BBacnetNetwork.localDevice();
   private BAlarmService alarmService;
   private BBacnetObjectIdentifier eventObjectId;
   private BEnum toState;
   private BEnum fromState;
   private BEnum offnormalToState;
   private boolean isAlarm;
   private BBacnetTimeStamp ackTimestamp;
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private static final ConcurrentHashMap<BAlarmRecord, BBacnetTimeStamp> ackTimestamps = new ConcurrentHashMap<>();

   public AsyncEventNotificationRequest(AsyncEventNotificationRequest request) {
      this(
         request.getAlarm(),
         request.getEventObjectId(),
         request.getEventObject(),
         request.getProcessId(),
         request.getRecipient(),
         request.isUseConfirmed(),
         request.isAckAlarmAndNormal()
      );
   }

   public AsyncEventNotificationRequest(
      BAlarmRecord alarm, BComponent eventObject, long processId, BBacnetRecipient recipient, boolean useConfirmed, boolean ackAlarmAndNormal
   ) {
      this.alarm = alarm;
      this.eventObject = eventObject;
      this.processId = processId;
      this.recipient = recipient;
      this.useConfirmed = useConfirmed;
      this.ackAlarmAndNormal = ackAlarmAndNormal;
      this.alarmData = alarm.getAlarmData();
   }

   public AsyncEventNotificationRequest(
      BAlarmRecord alarm,
      BBacnetObjectIdentifier eventObjectId,
      BComponent eventObject,
      long processId,
      BBacnetRecipient recipient,
      boolean useConfirmed,
      boolean ackAlarmAndNormal
   ) {
      this(alarm, eventObject, processId, recipient, useConfirmed, ackAlarmAndNormal);
      this.eventObjectId = eventObjectId;
   }

   private static BBacnetTimeStamp getAckTimestamp(BAlarmRecord alarmRecord) {
      return ackTimestamps.computeIfAbsent(alarmRecord, k -> new BBacnetTimeStamp(BAbsTime.now()));
   }

   public static void removeAckTimestamp(BAlarmRecord alarmRecord) {
      if (alarmRecord != null) {
         ackTimestamps.remove(alarmRecord);
      }
   }

   @Override
   public void run() {
      EventNotificationParameters enp2 = null;

      try {
         this.alarmService = (BAlarmService)Sys.getService(BAlarmService.TYPE);
         boolean traceOn = logger.isLoggable(Level.FINE);
         BOrd objectOrd = this.eventObject.getOrdInSession();
         if (this.eventObjectId == null) {
            this.eventObjectId = this.local.lookupBacnetObjectId(objectOrd);
         }

         if (this.eventObjectId == null) {
            logger.warning(
               "Alarm " + this.alarm.getUuid() + " not sent to " + this.recipient + ": object " + this.eventObject.getName() + " not exposed to BACnet"
            );
            return;
         }

         if (this.alarm.getSourceState() == BSourceState.alert) {
            logger.warning(
               "Alarm "
                  + this.alarm.getUuid()
                  + " not sent to "
                  + this.recipient
                  + ": incorrect alarm state "
                  + this.alarm.getSourceState()
                  + "/"
                  + this.alarm.getAckState()
            );
            return;
         }

         try {
            this.eventSrc = (BBacnetEventSource)this.local.lookupBacnetObject(this.eventObjectId);
         } catch (ClassCastException var25) {
            logger.warning("Alarm " + this.alarm.getUuid() + " not sent to " + this.recipient + ": eventObjectId not a BacnetEventSource");
            return;
         }

         if (this.eventSrc == null) {
            logger.warning(
               "Alarm " + this.alarm.getUuid() + " not sent to " + this.recipient + ": object " + this.eventObject.getName() + " not exposed to BACnet"
            );
            return;
         }

         BObject alarmSource = this.alarm.getSource().get(0).get(this.eventObject);
         BBacnetNotificationClassDescriptor nc = this.getNotificationClassOfEventSource(this.eventSrc, alarmSource);
         if (nc == null) {
            logger.warning(
               "Alarm "
                  + this.alarm.getUuid()
                  + " not sent to "
                  + this.recipient
                  + ": object "
                  + this.eventObject.getName()
                  + " is using non-BACnet alarm class "
                  + this.alarm.getAlarmClass()
            );
            return;
         }

         long notificationClass = nc.getNotificationClass();
         if (this.recipient.getChoice() == 0) {
            this.address = DeviceRegistry.getDeviceAddress(this.recipient.getDevice());
            if (this.address == null) {
               addDevice(this.recipient.getDevice().getInstanceNumber());
               this.address = DeviceRegistry.getDeviceAddress(this.recipient.getDevice());
            }
         } else {
            this.address = this.recipient.getAddress();
         }

         if (this.address == null) {
            logger.warning("Alarm " + this.alarm.getUuid() + " not sent to " + this.recipient + ": address is unknown");
            return;
         }

         EventNotificationParameters enp = this.buildEventNotificationParameters(notificationClass);
         if (this.alarm.getAckState() == BAckState.acked) {
            if (traceOn) {
               logger.fine("AsEvtNot(" + this.recipient + "): BAC_STATE_ACKED=" + this.alarm.getAlarmFacet("stateAcked") + ", removing to prevent re-use...");
            }

            if (this.ackAlarmAndNormal) {
               BEnum toState = enp.getToState();
               int priority = enp.getPriority();
               BInteger toSt = (BInteger)this.alarmData.getFacet("offnormalToState");
               if (toSt != null) {
                  toState = BBacnetEventState.make(toSt.getInt());
               }

               if (BBacnetEventState.isNormal(toState)) {
                  BInteger i = (BInteger)this.alarmData.getFacet("normalPriority");
                  if (i != null) {
                     priority = i.getInt();
                  }
               } else if (BBacnetEventState.isFault(toState)) {
                  BInteger i = (BInteger)this.alarmData.getFacet("faultPriority");
                  if (i != null) {
                     priority = i.getInt();
                  }
               } else {
                  BInteger i = (BInteger)this.alarmData.getFacet("offnormalPriority");
                  if (i != null) {
                     priority = i.getInt();
                  }
               }

               enp2 = new EventNotificationParameters(
                  enp.getProcessId(),
                  enp.getInitiatingDeviceId(),
                  enp.getEventObjectId(),
                  (BBacnetTimeStamp)enp.getTimeStamp().newCopy(),
                  enp.getNotificationClass(),
                  priority,
                  enp.getEventType(),
                  enp.getMessageText(),
                  enp.getNotifyType(),
                  enp.getAckRequired(),
                  enp.getFromState(),
                  toState,
                  BacnetNotificationParameters.make(enp.getEventValues()),
                  enp.getEncoding()
               );
            }

            try {
               AlarmDbConnection conn = this.alarmService.getAlarmDb().getDbConnection(null);
               Throwable var31 = null;

               try {
                  if (conn.getRecord(this.alarm.getUuid()) == null) {
                     conn.append(this.alarm);
                  } else {
                     conn.update(this.alarm);
                  }
               } catch (Throwable var24) {
                  var31 = var24;
                  throw var24;
               } finally {
                  if (conn != null) {
                     if (var31 != null) {
                        try {
                           conn.close();
                        } catch (Throwable var23) {
                           var31.addSuppressed(var23);
                        }
                     } else {
                        conn.close();
                     }
                  }
               }
            } catch (Exception var27) {
               logger.warning("Cannot update alarm " + this.alarm.getUuid() + ": " + var27);
            }
         }

         if (traceOn) {
            logger.fine("Sending BACnet event notification to " + this.address);
         }

         this.sendNotification(this.address, enp, this.useConfirmed);
         if (enp2 != null) {
            if (traceOn) {
               logger.fine("Sending BACnet event notification(2) to " + this.address);
            }

            this.sendNotification(this.address, enp2, this.useConfirmed);
         }
      } catch (BacnetException var28) {
         logger.severe("BacnetException sending event notification: " + var28);
      } catch (Exception var29) {
         logger.log(Level.SEVERE, "Unable to send event notification to BACnet", (Throwable)var29);
      }
   }

   private BBacnetNotificationClassDescriptor getNotificationClassOfEventSource(BBacnetEventSource eventSource, BObject alarmSource) {
      BBacnetNotificationClassDescriptor nc = eventSource.getNotificationClass();
      if (alarmSource instanceof BAlarmSourceExt
         && !eventSource.isValidAlarmExt((BAlarmSourceExt)alarmSource)
         && !(((BAlarmSourceExt)alarmSource).getOffnormalAlgorithm() instanceof BBacnetStatusAlgorithm)) {
         logger.warning("Alarm " + this.alarm.getUuid() + " not sent to " + this.recipient + ": incorrect alarm algorithm for exported BACnet point");
         return null;
      } else {
         if (alarmSource instanceof BBacnetProxyExt) {
            BDevice bDevice = ((BBacnetProxyExt)alarmSource).getDevice();
            BBacnetDevice device = (BBacnetDevice)bDevice;
            String strAlarmClass = device.getAlarms().getAlarmClass();
            BValue alarmClass = ((BAlarmService)Sys.getService(BAlarmService.TYPE)).lookupAlarmClass(strAlarmClass);
            BBacnetObjectIdentifier alarmClassOid = BBacnetNetwork.localDevice().lookupBacnetObjectId(((BAlarmClass)alarmClass).getHandleOrd());
            if (alarmClassOid != null) {
               BIBacnetExportObject notificationClass = BBacnetNetwork.localDevice().lookupBacnetObject(alarmClassOid);
               nc = (BBacnetNotificationClassDescriptor)notificationClass;
               this.alarm.setAckRequired(false);
            }
         }

         return nc;
      }
   }

   private static BBacnetDevice addDevice(int deviceInstanceNum) {
      BBacnetNetwork network = BBacnetNetwork.bacnet();
      BBacnetObjectIdentifier oid = BBacnetObjectIdentifier.make(8, deviceInstanceNum);
      BBacnetDevice device = new BBacnetDevice();
      device.setObjectId(oid, null);
      network.add(device.getName(), device);
      return device;
   }

   protected void sendNotification(BBacnetAddress address, EventNotificationParameters enp, boolean useConfirmed) throws BacnetException {
      if (useConfirmed) {
         client().confirmedEventNotification(address, enp);
      } else {
         client().unconfirmedEventNotification(address, enp);
      }
   }

   private static BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   private EventNotificationParameters buildEventNotificationParameters(long notificationClass) throws Exception {
      BBacnetObjectIdentifier initiatingDeviceId = this.local.getObjectId();
      BBacnetNotifyType notifyType = this.getNotifyType();
      this.toState = this.parseToState();
      this.fromState = this.parseFromState(this.toState);
      int priority = this.alarm.getPriority();
      BObject alarmSource = this.alarm.getSource().get(0).get(this.eventObject);
      BEnum eventType;
      if (this.toState == BBacnetEventState.fault || this.fromState == BBacnetEventState.fault) {
         eventType = BBacnetEventType.changeOfReliability;
      } else if (alarmSource instanceof BAlarmSourceExt && ((BAlarmSourceExt)alarmSource).getOffnormalAlgorithm() instanceof BBacnetStatusAlgorithm) {
         eventType = BBacnetEventType.changeOfStatusFlags;
      } else {
         eventType = this.parseEventType();
      }

      boolean ackRequired = this.getAckRequired(this.toState);
      String messageText = null;
      BacnetNotificationParameters eventValues = null;
      int normalPriority = -1;
      int faultPriority = -1;
      int offnormalPriority = -1;
      this.offnormalToState = null;
      BBacnetTimeStamp timeStamp;
      if (notifyType.getOrdinal() == 2) {
         timeStamp = this.ackTimestamp;
         this.fromState = null;
         BInteger stateAcked = (BInteger)this.alarmData.getFacet("stateAcked");
         if (stateAcked != null) {
            this.toState = BBacnetEventState.make(stateAcked.getInt());
            if (BBacnetEventState.isNormal(this.toState)) {
               BInteger i = (BInteger)this.alarmData.getFacet("normalPriority");
               if (i != null) {
                  priority = i.getInt();
               }
            } else if (BBacnetEventState.isFault(this.toState)) {
               BInteger i = (BInteger)this.alarmData.getFacet("faultPriority");
               if (i != null) {
                  priority = i.getInt();
               }
            } else {
               BInteger i = (BInteger)this.alarmData.getFacet("offnormalPriority");
               if (i != null) {
                  priority = i.getInt();
               }
            }
         }
      } else {
         switch (this.toState.getOrdinal()) {
            case 0:
               normalPriority = priority;
               break;
            case 1:
               faultPriority = priority;
               this.offnormalToState = this.toState;
               break;
            default:
               offnormalPriority = priority;
               this.offnormalToState = this.toState;
         }

         if (this.alarm.getSourceState() == BSourceState.normal) {
            timeStamp = new BBacnetTimeStamp(this.alarm.getNormalTime());
         } else {
            timeStamp = new BBacnetTimeStamp(this.alarm.getTimestamp());
         }

         messageText = this.alarm.getFormattedAlarmDataValue("msgText", null);
         eventValues = this.buildEventValues(eventType);
      }

      BCharacterSetEncoding encoding = this.local.getCharacterSet();
      BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
      if (this.offnormalToState != null) {
         this.alarm.addAlarmFacet("offnormalToState", BInteger.make(this.offnormalToState.getOrdinal()));
      }

      if (normalPriority >= 0) {
         this.alarm.addAlarmFacet("normalPriority", BInteger.make(normalPriority));
      }

      if (faultPriority >= 0) {
         this.alarm.addAlarmFacet("faultPriority", BInteger.make(faultPriority));
      }

      if (offnormalPriority >= 0) {
         this.alarm.addAlarmFacet("offnormalPriority", BInteger.make(offnormalPriority));
      }

      this.alarm.setLastUpdate(BAbsTime.now());

      try {
         AlarmDbConnection conn = as.getAlarmDb().getDbConnection(null);
         Throwable var18 = null;

         try {
            if (conn.getRecord(this.alarm.getUuid()) == null) {
               conn.append(this.alarm);
            } else {
               conn.update(this.alarm);
            }
         } catch (Throwable var28) {
            var18 = var28;
            throw var28;
         } finally {
            if (conn != null) {
               if (var18 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var27) {
                     var18.addSuppressed(var27);
                  }
               } else {
                  conn.close();
               }
            }
         }
      } catch (Exception var30) {
         logger.log(Level.WARNING, "Cannot update alarm with BACnet facets", (Throwable)var30);
      }

      return new EventNotificationParameters(
         this.processId,
         initiatingDeviceId,
         this.eventObjectId,
         timeStamp,
         notificationClass,
         priority,
         eventType,
         messageText,
         notifyType,
         ackRequired,
         this.fromState,
         this.toState,
         eventValues,
         encoding
      );
   }

   private boolean getAckRequired(BEnum toState) {
      BAlarmClass ac = this.alarmService.lookupAlarmClass(this.alarm.getAlarmClass());
      BAlarmTransitionBits acAckReq = ac.getAckRequired();
      if (BBacnetEventState.isFault(toState)) {
         return acAckReq.isToFault();
      } else {
         return BBacnetEventState.isNormal(toState) ? acAckReq.isToNormal() : acAckReq.isToOffnormal();
      }
   }

   private BEnum parseEventType() {
      BDynamicEnum eventTypeMS = this.local.getEnumerationList().getEventType();
      switch (this.eventObjectId.getObjectType()) {
         case 0:
         case 1:
         case 2:
            return BBacnetEventType.outOfRange;
         case 3:
         case 5:
         case 13:
         case 19:
            return BBacnetEventType.changeOfState;
         case 4:
         case 14:
            return BBacnetEventType.commandFailure;
         case 9:
            BIBacnetExportObject descriptor = this.local.lookupBacnetObject(this.eventObjectId);
            if (descriptor instanceof BBacnetEventEnrollmentDescriptor) {
               return ((BBacnetEventEnrollmentDescriptor)descriptor).getEventType();
            }
         case 6:
         case 7:
         case 8:
         case 10:
         case 11:
         case 15:
         case 16:
         case 17:
         case 18:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 41:
         case 42:
         case 43:
         case 44:
         case 47:
         default:
            return eventTypeMS;
         case 12:
            return BBacnetEventType.floatingLimit;
         case 20:
            return BBacnetEventType.bufferReady;
         case 40:
            return BBacnetEventType.changeOfCharacterstring;
         case 45:
            return BBacnetEventType.signedOutOfRange;
         case 46:
            return BBacnetEventType.doubleOutOfRange;
         case 48:
            return BBacnetEventType.unsignedOutOfRange;
      }
   }

   private BBacnetNotifyType getNotifyType() {
      BBacnetNotifyType objectNotifyType = BBacnetNotifyType.alarm;
      BIBacnetExportObject bacnetObject = this.local.lookupBacnetObject(this.eventObjectId);
      if (bacnetObject instanceof BBacnetEventSource) {
         objectNotifyType = ((BBacnetEventSource)bacnetObject).getNotifyType();
      }

      return this.isAlarm ? objectNotifyType : BBacnetNotifyType.ackNotification;
   }

   private BEnum parseFromState(BEnum toState) {
      BDynamicEnum eventStateMS = this.local.getEnumerationList().getEventState();
      if (toState.getOrdinal() == 0) {
         BInteger state = (BInteger)this.alarmData.getFacet("offnormalToState");
         if (state != null) {
            return eventStateMS.getRange().get(state.getInt());
         }
      }

      BString state = (BString)this.alarmData.getFacet("fromState");
      return state != null ? eventStateMS.getRange().get(eventStateMS.getRange().tagToOrdinal(state.getString())) : null;
   }

   private BEnum parseToState() {
      BDynamicEnum eventStateMS = this.local.getEnumerationList().getEventState();
      BString state = (BString)this.alarmData.getFacet("toState");

      try {
         if (state != null) {
            return eventStateMS.getRange().get(eventStateMS.getRange().tagToOrdinal(state.getString()));
         }
      } catch (InvalidEnumException var4) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("InvalidEnumException parsing toState: Event State " + state + " not found");
         }
      }

      return BBacnetEventState.make(this.alarm.getSourceState());
   }

   private BacnetNotificationParameters buildEventValues(BEnum eventType) throws Exception {
      BString val = (BString)this.alarmData.getFacet("presentValue");
      if (val == null) {
         return null;
      } else {
         String v = val.getString();
         boolean statusFlagsErr = false;
         int objType = this.eventObjectId.getObjectType();
         BBacnetBitString statusFlags = BacnetBitStringUtil.getBacnetStatusFlags(BStatus.ok);
         if (eventType.getOrdinal() != 10) {
            PropertyValue sfval = this.eventSrc.readProperty(new NBacnetPropertyReference(111));
            if (!sfval.isError()) {
               statusFlags = AsnUtil.fromAsnBitString(sfval.getPropertyValue());
            } else {
               statusFlagsErr = true;
            }
         }

         if (objType == 9) {
            switch (eventType.getOrdinal()) {
               case 4:
                  return this.getAlgorithmicFloatingLimitValues(v, statusFlags);
               default:
                  BBacnetObjectIdentifier objectId = this.local.lookupBacnetObjectId(this.eventObject.getHandleOrd());
                  if (objectId == null) {
                     if (this.eventObject instanceof BControlPoint) {
                        BAbstractProxyExt proxyExt = ((BControlPoint)this.eventObject).getProxyExt();
                        if (proxyExt instanceof BBacnetProxyExt) {
                           objType = ((BBacnetProxyExt)proxyExt).getObjectId().getObjectType();
                        } else {
                           PropertyValue opr = this.eventSrc.readProperty(new BBacnetPropertyReference(78));
                           AsnInputStream inputStream = new AsnInputStream();
                           inputStream.setBuffer(opr.getPropertyValue());
                           BBacnetDeviceObjectPropertyReference ref = new BBacnetDeviceObjectPropertyReference();
                           ref.readAsn(inputStream);
                           if (ref.getPropertyId() == 33) {
                              objType = 0;
                           }
                        }
                     }
                  } else {
                     objType = objectId.getObjectType();
                  }
            }
         }

         switch (eventType.getOrdinal()) {
            case 0:
            case 2:
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            case 12:
            case 13:
            default:
               throw new IllegalArgumentException("Invalid combination: EventType=" + eventType + "; object type=" + this.eventObjectId.getObjectType());
            case 1:
               switch (objType) {
                  case 3:
                  case 5:
                     if (this.toState != BBacnetEventState.fault && this.fromState != BBacnetEventState.fault) {
                        return this.getBinaryCOSValues(val, statusFlags);
                     }
                     break;
                  case 13:
                  case 19:
                     if (this.toState != BBacnetEventState.fault && this.fromState != BBacnetEventState.fault) {
                        return this.getMultistateCOSValues(val, statusFlags);
                     }
               }

               throw new IllegalArgumentException("Invalid object type: " + objType + " for ChangeOfState event type");
            case 3:
               switch (objType) {
                  case 4:
                     return this.getBinaryCFValues(val, statusFlags);
                  case 14:
                     return this.getMultistateCFValues(v, statusFlags);
                  default:
                     throw new IllegalArgumentException("Invalid object type: " + objType + " for CommandFailure event type");
               }
            case 4:
               switch (objType) {
                  case 12:
                     return this.getLoopFLValues(statusFlags, statusFlagsErr);
                  default:
                     throw new IllegalArgumentException("Invalid object type: " + objType + " for FloatingLimit event type");
               }
            case 5:
               switch (objType) {
                  case 0:
                  case 1:
                  case 2:
                     return this.getAnalogOORValues((BBacnetEventType)eventType, v, statusFlags, this.toState.getOrdinal(), this.fromState.getOrdinal());
                  default:
                     throw new IllegalArgumentException("Invalid object type: " + objType + " for OutOfRange event type");
               }
            case 10:
               switch (objType) {
                  case 20:
                     return this.getTLogBRValues(v);
                  default:
                     throw new IllegalArgumentException("Invalid object type: " + objType + " for BufferReady event type");
               }
            case 14:
               switch (objType) {
                  case 46:
                     return this.getAnalogOORValues((BBacnetEventType)eventType, v, statusFlags, this.toState.getOrdinal(), this.fromState.getOrdinal());
                  default:
                     throw new IllegalArgumentException("Invalid object type: " + objType + " for DoubleOutOfRange event type");
               }
            case 15:
               switch (objType) {
                  case 45:
                     return this.getAnalogOORValues((BBacnetEventType)eventType, v, statusFlags, this.toState.getOrdinal(), this.fromState.getOrdinal());
                  default:
                     throw new IllegalArgumentException("Invalid object type: " + objType + " for SignedOutOfRange event type");
               }
            case 16:
               switch (objType) {
                  case 48:
                     return this.getAnalogOORValues((BBacnetEventType)eventType, v, statusFlags, this.toState.getOrdinal(), this.fromState.getOrdinal());
                  default:
                     throw new IllegalArgumentException("Invalid object type: " + objType + " for UnsignedOutOfRange event type");
               }
            case 17:
               if (this.toState != BBacnetEventState.fault && this.fromState != BBacnetEventState.fault) {
                  return this.getChangeOfCSValues(v, statusFlags);
               }

               throw new IllegalArgumentException("Invalid combination: EventType=" + eventType + "; object type=" + this.eventObjectId.getObjectType());
            case 18:
               return this.getBacnetNotificationParametersForCOSF(val, v, objType, statusFlags);
            case 19:
               return this.getBacnetNotificationParametersForReliability(val, v, objType, statusFlags);
         }
      }
   }

   private BacnetNotificationParameters getBacnetNotificationParametersForCOSF(BString val, String v, int objType, BBacnetBitString statusFlags) throws RejectException, AsnException {
      if (this.eventSrc instanceof BBacnetEventEnrollmentDescriptor) {
         statusFlags = ((BBacnetStatusAlgorithm)((BAlarmSourceExt)this.eventSrc.getObject()).getOffnormalAlgorithm()).getLastMonitoredValue();
      } else {
         PropertyValue sfval = this.eventSrc.readProperty(new NBacnetPropertyReference(111));
         if (!sfval.isError()) {
            statusFlags = AsnUtil.fromAsnBitString(sfval.getPropertyValue());
         }
      }

      return new ChangeOfStatusFlags(this.getPresentValue(val, v, objType), BBacnetBitString.make(statusFlags.getBits()));
   }

   private BBacnetAny getPresentValue(BString val, String v, int objType) throws AsnException {
      switch (objType) {
         case 0:
         case 1:
         case 2:
            return BBacnetAny.make(AsnUtil.toAsn(BFloat.make(v)));
         case 3:
         case 5: {
            NBacnetPropertyStates ps = this.getBinaryBacnetPropertyStates(val);
            return BBacnetAny.make(AsnUtil.toAsn(ps.getBinaryValue()));
         }
         case 4:
            return BBacnetAny.make(AsnUtil.toAsn(BBacnetBinaryPv.make(AsnUtil.fromAsnEnumerated(this.getBinaryCFPresentValue(val)))));
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 15:
         case 16:
         case 17:
         case 18:
         case 20:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 47:
         default:
            return BBacnetAny.make(AsnUtil.toAsn(BString.make(v)));
         case 13:
         case 19: {
            NBacnetPropertyStates ps = this.getMultiStateBacnetPropertyStates(val);
            return BBacnetAny.make(AsnUtil.toAsn(BBacnetUnsigned.make(ps.getUnsignedValue())));
         }
         case 14:
            return BBacnetAny.make(AsnUtil.toAsn(AsnUtil.fromAsnUnsigned(this.getCommandFailurePresentValue(v))));
         case 45:
            return BBacnetAny.make(AsnUtil.toAsn(BInteger.make(v)));
         case 46:
            return BBacnetAny.make(AsnUtil.toAsn(BDouble.make(v)));
         case 48:
            return BBacnetAny.make(AsnUtil.toAsn(BBacnetUnsigned.make(BLong.make(v).getLong())));
      }
   }

   private BacnetNotificationParameters getBacnetNotificationParametersForReliability(BString val, String v, int objType, BBacnetBitString statusFlags) throws RejectException, AsnException {
      PropertyValue reliability = this.eventSrc.readProperty(new NBacnetPropertyReference(103));
      BBacnetPropertyValue[] value;
      switch (objType) {
         case 0:
         case 1:
         case 2:
            value = new BBacnetPropertyValue[]{new BBacnetPropertyValue(85, BFloat.make(parseFloat(v)))};
            break;
         case 3: {
            NBacnetPropertyStates ps = this.getBinaryBacnetPropertyStates(val);
            value = new BBacnetPropertyValue[]{new BBacnetPropertyValue(85, ps.getBinaryValue())};
            break;
         }
         case 4:
            value = new BBacnetPropertyValue[]{
               new BBacnetPropertyValue(85, BBacnetBinaryPv.make(AsnUtil.fromAsnEnumerated(this.getBinaryCFPresentValue(val)))),
               new BBacnetPropertyValue(40, BBacnetBinaryPv.make(AsnUtil.fromAsnEnumerated(this.getBinaryCFFeedBackValue())))
            };
            break;
         case 13:
         case 19: {
            NBacnetPropertyStates ps = this.getMultiStateBacnetPropertyStates(val);
            value = new BBacnetPropertyValue[]{new BBacnetPropertyValue(85, BBacnetUnsigned.make(ps.getUnsignedValue()))};
            break;
         }
         case 14:
            value = new BBacnetPropertyValue[]{
               new BBacnetPropertyValue(85, AsnUtil.fromAsnUnsigned(this.getCommandFailurePresentValue(v))),
               new BBacnetPropertyValue(40, AsnUtil.fromAsnUnsigned(this.getCommandFailureFeedbackValue()))
            };
            break;
         case 45:
            value = new BBacnetPropertyValue[]{new BBacnetPropertyValue(85, BInteger.make(parseSigned(v)))};
            break;
         case 46:
            value = new BBacnetPropertyValue[]{new BBacnetPropertyValue(85, BDouble.make(parseDouble(v)))};
            break;
         case 48:
            value = new BBacnetPropertyValue[]{new BBacnetPropertyValue(85, BBacnetUnsigned.make(BLong.make(parseUnsigned(v)).getLong()))};
            break;
         default:
            value = new BBacnetPropertyValue[]{new BBacnetPropertyValue(85, BString.make(v))};
      }

      return new ChangeOfReliability(BBacnetReliability.make(AsnUtil.fromAsnEnumerated(reliability.getPropertyValue())), statusFlags, value);
   }

   private static float parseFloat(String s) {
      int ndx = s.indexOf(" ");
      return ndx < 0 ? parseFloat(s, Locale.getDefault()) : parseFloat(s.substring(0, ndx), Locale.getDefault());
   }

   private static Double parseDouble(String s) {
      int ndx = s.indexOf(" ");
      return ndx < 0 ? parseDouble(s, Locale.getDefault()) : parseDouble(s.substring(0, ndx), Locale.getDefault());
   }

   private static Float parseFloat(String s, Locale locale) {
      try {
         return NumberFormat.getInstance(locale).parse(s).floatValue();
      } catch (ParseException var3) {
         logger.log(Level.SEVERE, "Unable to parse string to float", (Throwable)var3);
         throw new IllegalArgumentException("Illegal argument sent to parse: '" + s + "'");
      }
   }

   private static Double parseDouble(String s, Locale locale) {
      try {
         return NumberFormat.getInstance(locale).parse(s).doubleValue();
      } catch (ParseException var3) {
         logger.log(Level.SEVERE, "Unable to parse string to double", (Throwable)var3);
         throw new IllegalArgumentException("Illegal argument sent to parse: '" + s + "'");
      }
   }

   private static Integer parseSigned(String s) {
      int ndx = s.indexOf(" ");
      return ndx < 0 ? Integer.parseInt(s) : Integer.parseInt(s.substring(0, ndx));
   }

   private static Long parseUnsigned(String s) {
      int ndx = s.indexOf(" ");
      return ndx < 0 ? Long.parseLong(s) : Long.parseLong(s.substring(0, ndx));
   }

   private ChangeOfState getBinaryCOSValues(BString val, BBacnetBitString statusFlags) {
      NBacnetPropertyStates ps = this.getBinaryBacnetPropertyStates(val);
      if (ps != null) {
         return new ChangeOfState(ps, statusFlags);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("alarmData: " + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet COS Event Values for Binary point " + this.eventObjectId);
      }
   }

   private NBacnetPropertyStates getBinaryBacnetPropertyStates(BString val) {
      NBacnetPropertyStates ps = null;
      BInteger iv = (BInteger)this.alarmData.get("numericValue");
      if (iv != null) {
         ps = new NBacnetPropertyStates(iv.getInt() != 0 ? BBacnetBinaryPv.active : BBacnetBinaryPv.inactive);
      }

      if (ps == null) {
         BBoolean av = (BBoolean)this.alarmData.get("alarmValue");
         if (av != null) {
            ps = new NBacnetPropertyStates(av.getBoolean() ? BBacnetBinaryPv.active : BBacnetBinaryPv.inactive);
         }
      }

      if (ps == null) {
         BString tt = (BString)this.eventObject.getSlotFacets(BBooleanPoint.out).get("trueText");
         if (tt != null) {
            ps = new NBacnetPropertyStates(tt.equals(val) ? BBacnetBinaryPv.active : BBacnetBinaryPv.inactive);
         }
      }

      return ps;
   }

   private ChangeOfState getMultistateCOSValues(BString val, BBacnetBitString statusFlags) {
      NBacnetPropertyStates ps = this.getMultiStateBacnetPropertyStates(val);
      if (ps != null) {
         return new ChangeOfState(ps, statusFlags);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("alarmData: " + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet COS Event Values for Multistate point " + this.eventObjectId);
      }
   }

   private NBacnetPropertyStates getMultiStateBacnetPropertyStates(BString val) {
      NBacnetPropertyStates ps = null;
      BInteger iv = (BInteger)this.alarmData.get("numericValue");
      if (iv != null) {
         ps = new NBacnetPropertyStates(iv.getInt());
      }

      if (ps == null) {
         BEnumRange r = (BEnumRange)this.eventObject.getSlotFacets(BEnumPoint.out).get("range");
         BString av = (BString)this.alarmData.get("alarmValue");
         if (av != null && r != null && r.isTag(av.getString())) {
            ps = new NBacnetPropertyStates(r.get(av.getString()).getOrdinal());
         }

         if (ps == null && r != null && r.isTag(val.getString())) {
            ps = new NBacnetPropertyStates(r.get(val.getString()).getOrdinal());
         }
      }

      return ps;
   }

   private ChangeOfCharacterString getChangeOfCSValues(String presentValue, BBacnetBitString statusFlags) {
      BString av = (BString)this.alarmData.get("alarmValue");
      if (av != null) {
         return new ChangeOfCharacterString(presentValue, statusFlags, av.toString());
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("alarmData: " + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet COCS Event Values for CharacterStringValue point " + this.eventObjectId);
      }
   }

   private OutOfRange getAnalogOORValues(BBacnetEventType et, String vStr, BBacnetBitString statusFlags, int toState, int fromState) {
      BString dbf = (BString)this.alarmData.getFacet("deadband");
      BString elf = null;
      if (toState == 3) {
         elf = (BString)this.alarmData.getFacet("highLimit");
      } else if (toState == 4) {
         elf = (BString)this.alarmData.getFacet("lowLimit");
      } else if (toState == 0) {
         if (fromState == 3) {
            elf = (BString)this.alarmData.getFacet("highLimit");
         } else if (fromState == 4) {
            elf = (BString)this.alarmData.getFacet("lowLimit");
         }
      }

      if (vStr != null && dbf != null && elf != null) {
         double value = parseDouble(vStr);
         double deadband = parseDouble(dbf.getString());
         double elfValue = parseDouble(elf.getString());
         switch (et.getOrdinal()) {
            case 14:
               return new DoubleOutOfRange(value, statusFlags, deadband, elfValue);
            case 15:
               return new SignedOutOfRange(
                  Double.valueOf(value).intValue(), statusFlags, Double.valueOf(deadband).intValue(), Double.valueOf(elfValue).intValue()
               );
            case 16:
               return new UnsignedOutOfRange(
                  Double.valueOf(value).longValue(), statusFlags, Double.valueOf(deadband).longValue(), Double.valueOf(elfValue).longValue()
               );
            default:
               return new OutOfRange((float)value, statusFlags, parseFloat(dbf.getString()), parseFloat(elf.getString()));
         }
      } else if (toState != 1 && fromState != 1) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("alarmData: " + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet OOR Event Values: Missing value, deadband, or exceededLimit");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BACnet does not support Niagara HIGH/LOW FAULT status, not sending transistion :" + this.alarmData);
         }

         return null;
      }
   }

   private UnsignedRange getBinaryElapsedActiveTimeValues(BBacnetEventType et, String v, BBacnetBitString statusFlags, int toState, int fromState) {
      BString presentValue = (BString)this.alarmData.getFacet("presentValue");
      BString errorLimit = (BString)this.alarmData.getFacet("errorLimit");
      if (presentValue != null && !presentValue.getString().equals("") && errorLimit != null && !errorLimit.getString().equals("")) {
         BRelTime presentValueTime = null;
         BRelTime errorLimitTime = null;

         try {
            presentValueTime = parseRelTimeString(presentValue.toString());
            errorLimitTime = parseRelTimeString(errorLimit.toString());
            return new UnsignedRange(presentValueTime.getSeconds(), statusFlags, errorLimitTime.getSeconds());
         } catch (NumberFormatException var11) {
            throw new IllegalArgumentException("Cannot build BACnet Unsigned Range Event Values: Failed to parse String representation of RelTime");
         }
      } else if (toState != 1 && fromState != 1) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("alarmData: " + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet Unsigned Range Event Values: Missing value or exceededLimit");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BACnet does not support Niagara HIGH/LOW FAULT status, not sending transistion :" + this.alarmData);
         }

         return null;
      }
   }

   private static BRelTime parseRelTimeString(String input) {
      int seconds = 0;
      int minutes = 0;
      int hours = 0;
      int days = 0;
      if (!input.equals("0 sec") && !input.equals("0 minutes") && !input.equals("0 hours") && !input.equals("0 days")) {
         String[] strings = input.split(" ");

         for (int i = 0; i < strings.length; i++) {
            if (strings[i].endsWith("sec") || strings[i].endsWith("secs")) {
               int secondsStringIndex = strings[i].indexOf("sec");
               String secondsString = strings[i].substring(0, secondsStringIndex);
               String[] secondsStringsArray = secondsString.split("\\.");
               if (secondsStringsArray.length > 0) {
                  seconds = Integer.parseInt(secondsStringsArray[0]);
               }
            } else if (strings[i].endsWith("min") || strings[i].endsWith("mins")) {
               minutes = Integer.parseInt(strings[i].replaceAll("[^0-9]", ""));
            } else if (strings[i].endsWith("hour") || strings[i].endsWith("hours")) {
               hours = Integer.parseInt(strings[i].replaceAll("[^0-9]", ""));
            } else if (strings[i].endsWith("day") || strings[i].endsWith("days")) {
               days = Integer.parseInt(strings[i].replaceAll("[^0-9]", ""));
            }
         }

         return BRelTime.make(days, hours, minutes, seconds);
      } else {
         return BRelTime.make(days, hours, minutes, seconds);
      }
   }

   private FloatingLimit getLoopFLValues(BBacnetBitString statusFlags, boolean statusFlagsErr) {
      BString cvf = (BString)this.alarmData.get("controlledValue");
      BString spf = (BString)this.alarmData.get("setptValue");
      BString erf = (BString)this.alarmData.get("errorLimit");
      if (cvf != null && spf != null && erf != null) {
         float cv = parseFloat(cvf.getString());
         float sp = parseFloat(spf.getString());
         if (this.toState.getOrdinal() == 2) {
            if (cv > sp) {
               this.toState = BBacnetEventState.highLimit;
            } else if (cv < sp) {
               this.toState = BBacnetEventState.lowLimit;
            }

            this.offnormalToState = this.toState;
            if (statusFlagsErr) {
               statusFlags = BacnetBitStringUtil.getBacnetStatusFlags(BStatus.alarm);
            }
         } else if (this.fromState.getOrdinal() == 2) {
            BInteger on2s = (BInteger)this.alarmData.get("offnormalToState");
            if (on2s != null) {
               this.fromState = BBacnetEventState.make(on2s.getInt());
            }
         }

         return new FloatingLimit(parseFloat(cvf.getString()), statusFlags, parseFloat(spf.getString()), parseFloat(erf.getString()));
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("alarmData: " + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet FL Event Values: Missing referenced value, setpoint value, or error limit");
      }
   }

   private FloatingLimit getAlgorithmicFloatingLimitValues(String referenceValue, BBacnetBitString statusFlags) {
      BDouble setpointValue = (BDouble)this.alarmData.get("setpointNumeric");
      BString errorLimit = null;
      switch (this.toState.getOrdinal()) {
         case 0:
            if (this.fromState.getOrdinal() == 3) {
               errorLimit = (BString)this.alarmData.getFacet("highDiffLimit");
            } else if (this.fromState.getOrdinal() == 4) {
               errorLimit = (BString)this.alarmData.getFacet("lowDiffLimit");
            }
         case 1:
         case 2:
         default:
            break;
         case 3:
            errorLimit = (BString)this.alarmData.getFacet("highDiffLimit");
            break;
         case 4:
            errorLimit = (BString)this.alarmData.getFacet("lowDiffLimit");
      }

      if (referenceValue != null && setpointValue != null && errorLimit != null) {
         return new FloatingLimit(parseFloat(referenceValue), statusFlags, setpointValue.getFloat(), parseFloat(errorLimit.getString()));
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.eventObjectId
                  + ": error building event values for floating limit from alarm data: "
                  + this.alarmData
                  + "; reference value: "
                  + referenceValue
                  + ", setpoint value: "
                  + setpointValue
                  + ", error limit: "
                  + errorLimit
                  + ", toState: "
                  + this.toState
                  + ", fromState: "
                  + this.fromState
            );
         }

         throw new IllegalArgumentException(
            this.eventObjectId + ": error building event values for floating limit from alarm data: reference value, setpoint value or error limit is null"
         );
      }
   }

   private CommandFailure getBinaryCFValues(BString val, BBacnetBitString statusFlags) {
      byte[] cv = this.getBinaryCFPresentValue(val);
      byte[] fv = this.getBinaryCFFeedBackValue();
      if (cv != null && fv != null) {
         return new CommandFailure(cv, statusFlags, fv);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("alarmData: " + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet CF Event Values for Binary point " + this.eventObjectId);
      }
   }

   private byte[] getBinaryCFFeedBackValue() {
      byte[] fv = null;
      BString tt = (BString)this.eventObject.getSlotFacets(BBooleanPoint.out).get("trueText");
      if (tt != null) {
         fv = AsnUtil.toAsnEnumerated(tt.equals(this.alarmData.getFacet("feedbackValue")));
      }

      return fv;
   }

   private byte[] getBinaryCFPresentValue(BString val) {
      byte[] cv = null;
      BString tt = (BString)this.eventObject.getSlotFacets(BBooleanPoint.out).get("trueText");
      BInteger iv = (BInteger)this.alarmData.get("numericValue");
      if (iv != null) {
         cv = AsnUtil.toAsnEnumerated(iv.getInt() != 0);
      }

      if (cv == null) {
         BBoolean av = (BBoolean)this.alarmData.get("alarmValue");
         if (av != null) {
            cv = AsnUtil.toAsnEnumerated(av.getBoolean());
         }
      }

      if (cv == null && tt != null) {
         cv = AsnUtil.toAsnEnumerated(tt.equals(val));
      }

      return cv;
   }

   private CommandFailure getMultistateCFValues(String v, BBacnetBitString statusFlags) {
      byte[] cv = this.getCommandFailurePresentValue(v);
      byte[] fv = this.getCommandFailureFeedbackValue();
      if (cv != null && fv != null) {
         return new CommandFailure(cv, statusFlags, fv);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("cv=" + Arrays.toString(cv) + "; fv=" + Arrays.toString(fv) + "; nalarmData:" + this.alarmData);
         }

         throw new IllegalArgumentException("Cannot build BACnet CF Event Values for Multistate point " + this.eventObjectId);
      }
   }

   private byte[] getCommandFailurePresentValue(String v) {
      byte[] cv = null;
      BEnumRange msr = (BEnumRange)this.eventObject.getSlotFacets(BEnumPoint.out).get("range");
      BInteger iv = (BInteger)this.alarmData.get("numericValue");
      if (iv != null) {
         cv = AsnUtil.toAsnUnsigned(iv.getInt());
      }

      if (cv == null) {
         BString av = (BString)this.alarmData.get("alarmValue");
         if (av != null && msr != null && msr.isTag(av.getString())) {
            cv = AsnUtil.toAsnUnsigned(msr.get(av.getString()));
         }
      }

      if (cv == null && msr != null && msr.isTag(v)) {
         cv = AsnUtil.toAsnUnsigned(msr.get(v));
      }

      if (cv == null) {
         try {
            cv = AsnUtil.toAsnUnsigned(Integer.parseInt(v));
         } catch (Exception var6) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("MS CmdFailure: v=" + v);
            }
         }
      }

      return cv;
   }

   private byte[] getCommandFailureFeedbackValue() {
      byte[] fv = null;
      BEnumRange msr = (BEnumRange)this.eventObject.getSlotFacets(BEnumPoint.out).get("range");
      BString fvf = (BString)this.alarmData.getFacet("feedbackValue");
      if (fvf != null) {
         if (msr != null) {
            fv = AsnUtil.toAsnUnsigned(msr.get(fvf.getString()));
         } else {
            try {
               fv = AsnUtil.toAsnUnsigned(Integer.parseInt(fvf.getString()));
            } catch (Exception var5) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("MS CmdFailure: fvf=" + fvf);
               }
            }
         }
      }

      return fv;
   }

   private BufferReady getTLogBRValues(String v) throws Exception {
      BBacnetDeviceObjectPropertyReference opr = new BBacnetDeviceObjectPropertyReference();
      opr = (BBacnetDeviceObjectPropertyReference)opr.decodeFromString(v);
      long pn = Long.parseLong(((BString)this.alarmData.getFacet("previousNotification")).getString());
      long cn = Long.parseLong(((BString)this.alarmData.getFacet("currentNotification")).getString());
      return new BufferReady(opr, pn, cn);
   }

   public BAlarmRecord getAlarm() {
      return this.alarm;
   }

   public BComponent getEventObject() {
      return this.eventObject;
   }

   public long getProcessId() {
      return this.processId;
   }

   public BBacnetRecipient getRecipient() {
      return this.recipient;
   }

   public boolean isUseConfirmed() {
      return this.useConfirmed;
   }

   public boolean isAckAlarmAndNormal() {
      return this.ackAlarmAndNormal;
   }

   public BBacnetEventSource getEventSrc() {
      return this.eventSrc;
   }

   public BFacets getAlarmData() {
      return this.alarmData;
   }

   public BBacnetAddress getAddress() {
      return this.address;
   }

   public BLocalBacnetDevice getLocal() {
      return this.local;
   }

   public BAlarmService getAlarmService() {
      return this.alarmService;
   }

   public BBacnetObjectIdentifier getEventObjectId() {
      return this.eventObjectId;
   }

   public BEnum getToState() {
      return this.toState;
   }

   public BEnum getFromState() {
      return this.fromState;
   }

   public BEnum getOffnormalToState() {
      return this.offnormalToState;
   }

   public boolean isAlarm() {
      return this.isAlarm;
   }

   public void setAlarm(boolean isAlarm) {
      this.isAlarm = isAlarm;
      if (!isAlarm) {
         this.ackTimestamp = getAckTimestamp(this.alarm);
      }
   }
}
