package com.tridium.bacnet.history;

import com.tridium.bacnet.BacUtil;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAckState;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.BSourceState;
import javax.baja.alarm.ext.BAlarmState;
import javax.baja.alarm.ext.BAlarmTimestamps;
import javax.baja.alarm.ext.BIAlarmMessages;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.control.BControlPoint;
import javax.baja.control.BPointExtension;
import javax.baja.data.BIDataValue;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BLong;
import javax.baja.sys.BString;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFormat;
import javax.baja.util.BUuid;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "notificationThreshold",
      type = "long",
      defaultValue = "BLong.make(10)"
   ), @NiagaraProperty(
      name = "recordsSinceNotification",
      type = "long",
      defaultValue = "BLong.make(0)",
      flags = 3
   ), @NiagaraProperty(
      name = "lastNotifyRecord",
      type = "long",
      defaultValue = "BLong.make(0)",
      flags = 3
   ), @NiagaraProperty(
      name = "alarmInhibit",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)"
   ), @NiagaraProperty(
      name = "alarmState",
      type = "BAlarmState",
      defaultValue = "BAlarmState.normal",
      flags = 3
   ), @NiagaraProperty(
      name = "alarmEnable",
      type = "BAlarmTransitionBits",
      defaultValue = "BAlarmTransitionBits.make(BAlarmTransitionBits.TO_NORMAL)"
   ), @NiagaraProperty(
      name = "ackedTransitions",
      type = "BAlarmTransitionBits",
      defaultValue = "BAlarmTransitionBits.ALL",
      flags = 3
   ), @NiagaraProperty(
      name = "notifyType",
      type = "BBacnetNotifyType",
      defaultValue = "BBacnetNotifyType.alarm",
      facets = {@Facet("BacUtil.makeBacnetNotifyTypeFacets()")}
   ), @NiagaraProperty(
      name = "toOffnormalTimes",
      type = "BAlarmTimestamps",
      defaultValue = "new BAlarmTimestamps()",
      flags = 3
   ), @NiagaraProperty(
      name = "toFaultTimes",
      type = "BAlarmTimestamps",
      defaultValue = "new BAlarmTimestamps()",
      flags = 3
   ), @NiagaraProperty(
      name = "toNormalTimes",
      type = "BAlarmTimestamps",
      defaultValue = "new BAlarmTimestamps()",
      flags = 3
   ), @NiagaraProperty(
      name = "toNormalText",
      type = "BFormat",
      defaultValue = "BFormat.make(\"BUFFER_READY\")"
   ), @NiagaraProperty(
      name = "hyperlinkOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      facets = {@Facet(
         name = "BFacets.ORD_RELATIVIZE",
         value = "false"
      )}
   ), @NiagaraProperty(
      name = "alarmClass",
      type = "String",
      defaultValue = "defaultAlarmClass",
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"alarm:AlarmClassFE\""
      ), @Facet(
         name = "BFacets.UX_FIELD_EDITOR",
         value = "\"alarm:AlarmClassEditor\""
      )}
   )})
@NiagaraAction(
   name = "ackAlarm",
   parameterType = "BAlarmRecord",
   defaultValue = "new BAlarmRecord()",
   returnType = "BBoolean",
   flags = 4
)
public class BBacnetTrendLogAlarmSourceExt extends BPointExtension implements BIAlarmSource, BIAlarmMessages, BacnetAlarmConst {
   public static final Property notificationThreshold = newProperty(0, BLong.make(10L), null);
   public static final Property recordsSinceNotification = newProperty(3, BLong.make(0L), null);
   public static final Property lastNotifyRecord = newProperty(3, BLong.make(0L), null);
   public static final Property alarmInhibit = newProperty(0, new BStatusBoolean(false), null);
   public static final Property alarmState = newProperty(3, BAlarmState.normal, null);
   public static final Property alarmEnable = newProperty(0, BAlarmTransitionBits.make(4), null);
   public static final Property ackedTransitions = newProperty(3, BAlarmTransitionBits.ALL, null);
   public static final Property notifyType = newProperty(0, BBacnetNotifyType.alarm, BacUtil.makeBacnetNotifyTypeFacets());
   public static final Property toOffnormalTimes = newProperty(3, new BAlarmTimestamps(), null);
   public static final Property toFaultTimes = newProperty(3, new BAlarmTimestamps(), null);
   public static final Property toNormalTimes = newProperty(3, new BAlarmTimestamps(), null);
   public static final Property toNormalText = newProperty(0, BFormat.make("BUFFER_READY"), null);
   public static final Property hyperlinkOrd = newProperty(0, BOrd.NULL, BFacets.make("ordRelativize", false));
   public static final Property alarmClass = newProperty(
      0, "defaultAlarmClass", BFacets.make(BFacets.make("fieldEditor", "alarm:AlarmClassFE"), BFacets.make("uxFieldEditor", "alarm:AlarmClassEditor"))
   );
   public static final Action ackAlarm = newAction(4, new BAlarmRecord(), null);
   public static final Type TYPE = Sys.loadType(BBacnetTrendLogAlarmSourceExt.class);
   private static final Context millis = BFacets.make("showMilliseconds", BBoolean.TRUE);
   private static Logger logger = Logger.getLogger("bacnet.server");

   public long getNotificationThreshold() {
      return this.getLong(notificationThreshold);
   }

   public void setNotificationThreshold(long v) {
      this.setLong(notificationThreshold, v, null);
   }

   public long getRecordsSinceNotification() {
      return this.getLong(recordsSinceNotification);
   }

   public void setRecordsSinceNotification(long v) {
      this.setLong(recordsSinceNotification, v, null);
   }

   public long getLastNotifyRecord() {
      return this.getLong(lastNotifyRecord);
   }

   public void setLastNotifyRecord(long v) {
      this.setLong(lastNotifyRecord, v, null);
   }

   public BStatusBoolean getAlarmInhibit() {
      return (BStatusBoolean)this.get(alarmInhibit);
   }

   public void setAlarmInhibit(BStatusBoolean v) {
      this.set(alarmInhibit, v, null);
   }

   public BAlarmState getAlarmState() {
      return (BAlarmState)this.get(alarmState);
   }

   public void setAlarmState(BAlarmState v) {
      this.set(alarmState, v, null);
   }

   public BAlarmTransitionBits getAlarmEnable() {
      return (BAlarmTransitionBits)this.get(alarmEnable);
   }

   public void setAlarmEnable(BAlarmTransitionBits v) {
      this.set(alarmEnable, v, null);
   }

   public BAlarmTransitionBits getAckedTransitions() {
      return (BAlarmTransitionBits)this.get(ackedTransitions);
   }

   public void setAckedTransitions(BAlarmTransitionBits v) {
      this.set(ackedTransitions, v, null);
   }

   public BBacnetNotifyType getNotifyType() {
      return (BBacnetNotifyType)this.get(notifyType);
   }

   public void setNotifyType(BBacnetNotifyType v) {
      this.set(notifyType, v, null);
   }

   public BAlarmTimestamps getToOffnormalTimes() {
      return (BAlarmTimestamps)this.get(toOffnormalTimes);
   }

   public void setToOffnormalTimes(BAlarmTimestamps v) {
      this.set(toOffnormalTimes, v, null);
   }

   public BAlarmTimestamps getToFaultTimes() {
      return (BAlarmTimestamps)this.get(toFaultTimes);
   }

   public void setToFaultTimes(BAlarmTimestamps v) {
      this.set(toFaultTimes, v, null);
   }

   public BAlarmTimestamps getToNormalTimes() {
      return (BAlarmTimestamps)this.get(toNormalTimes);
   }

   public void setToNormalTimes(BAlarmTimestamps v) {
      this.set(toNormalTimes, v, null);
   }

   public BFormat getToNormalText() {
      return (BFormat)this.get(toNormalText);
   }

   public void setToNormalText(BFormat v) {
      this.set(toNormalText, v, null);
   }

   public BOrd getHyperlinkOrd() {
      return (BOrd)this.get(hyperlinkOrd);
   }

   public void setHyperlinkOrd(BOrd v) {
      this.set(hyperlinkOrd, v, null);
   }

   public String getAlarmClass() {
      return this.getString(alarmClass);
   }

   public void setAlarmClass(String v) {
      this.setString(alarmClass, v, null);
   }

   public BBoolean ackAlarm(BAlarmRecord parameter) {
      return (BBoolean)this.invoke(ackAlarm, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.setLastNotifyRecord(this.getTrendLogExt().getTotalRecordCount());
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BIBacnetTrendLogExt;
   }

   protected boolean isSiblingLegal(BComponent sibling) {
      return true;
   }

   protected BIBacnetTrendLogExt getTrendLogExt() {
      return (BIBacnetTrendLogExt)this.getParent();
   }

   public BBoolean doAckAlarm(BAlarmRecord ackRequest) {
      if (!this.isRunning()) {
         return BBoolean.make(false);
      } else {
         ackRequest.setAckTime(BAbsTime.make());
         boolean validAck = false;
         ackRequest.setAckState(BAckState.acked);
         ackRequest.setAckRequired(false);
         BSourceState transition = ackRequest.getSourceState();
         Property alarmTimesProp;
         if (transition == BSourceState.normal) {
            alarmTimesProp = toNormalTimes;
         } else if (transition == BSourceState.offnormal) {
            alarmTimesProp = toOffnormalTimes;
         } else {
            if (transition != BSourceState.fault) {
               throw new IllegalStateException();
            }

            alarmTimesProp = toFaultTimes;
         }

         BAlarmTimestamps timestamps = (BAlarmTimestamps)this.get(alarmTimesProp);
         BAbsTime alarmTime = timestamps.getAlarmTime();
         if (alarmTime.equals(ackRequest.getTimestamp())) {
            timestamps.setAckTime(ackRequest.getAckTime());
            timestamps.setCount(0);
            BAlarmTransitionBits ackedTrans = this.getAckedTransitions();
            if (transition == BSourceState.offnormal) {
               this.setAckedTransitions(BAlarmTransitionBits.make(ackedTrans, BAlarmTransitionBits.toOffnormal, true));
            } else if (transition == BSourceState.fault) {
               this.setAckedTransitions(BAlarmTransitionBits.make(ackedTrans, BAlarmTransitionBits.toFault, true));
            } else if (transition == BSourceState.normal) {
               this.setAckedTransitions(BAlarmTransitionBits.make(ackedTrans, BAlarmTransitionBits.toNormal, true));
            } else if (transition == BSourceState.alert) {
               this.setAckedTransitions(BAlarmTransitionBits.make(ackedTrans, BAlarmTransitionBits.toAlert, true));
            }

            validAck = true;
         } else {
            logger.info("Ack timestamp does not match: stale ack! " + alarmTime.toString(millis) + " != " + ackRequest.getTimestamp().toString(millis));
            ackRequest.addAlarmFacet("staleAck", BBoolean.TRUE);
         }

         try {
            BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
            as.routeAlarm(ackRequest);
         } catch (Exception var8) {
            logger.log(Level.SEVERE, "Failed to route alarm to alarm service", (Throwable)var8);
         }

         return BBoolean.make(validAck);
      }
   }

   public void incrementRecordsSinceNotification() {
      this.setRecordsSinceNotification(this.getRecordsSinceNotification() + 1L);
   }

   public void onExecute(BStatusValue out, Context cx) {
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if ((p.equals(notificationThreshold) || p.equals(recordsSinceNotification)) && !(cx instanceof BBacnetTrendLogAlarmSourceExt.SkipCheckBufferContext)) {
            this.checkBufferReady(this.getTrendLogExt().getTotalRecordCount());
         }
      }
   }

   public void updateParameters(long notificationThreshold, long previousCount, Context cx) {
      BBacnetTrendLogAlarmSourceExt.SkipCheckBufferContext skipCheckBuffer = new BBacnetTrendLogAlarmSourceExt.SkipCheckBufferContext(cx);
      this.setLong(BBacnetTrendLogAlarmSourceExt.notificationThreshold, notificationThreshold, skipCheckBuffer);
      long oldPreviousCount = this.getLastNotifyRecord();
      long recordsSince = this.getRecordsSinceNotification() + oldPreviousCount - previousCount;
      this.setLong(recordsSinceNotification, recordsSince, skipCheckBuffer);
      this.setLong(lastNotifyRecord, previousCount, skipCheckBuffer);
      this.checkBufferReady(this.getTrendLogExt().getTotalRecordCount());
   }

   public void checkBufferReady(long currentSeqNum) {
      if (!this.getAlarmInhibit().getBoolean()) {
         if (this.getRecordsSinceNotification() > this.getNotificationThreshold()) {
            boolean successful = this.initiateBufferReadyEvent(currentSeqNum, this.getLastNotifyRecord());
            if (successful) {
               this.setRecordsSinceNotification(1L);
               this.setLastNotifyRecord(currentSeqNum);
            }
         }
      }
   }

   private boolean initiateBufferReadyEvent(long currentNotification, long previousNotification) {
      if (!this.getAlarmEnable().includes(BSourceState.normal)) {
         return true;
      } else {
         HashMap<String, BIDataValue> map = new HashMap<>();
         String msgText = "";
         BOrd ord = BOrd.NULL;
         msgText = this.getToNormalText().getFormat();
         ord = this.getHyperlinkOrd();
         Property alarmTimesProp = toNormalTimes;

         try {
            BBacnetObjectIdentifier objectId = BBacnetNetwork.localDevice().lookupBacnetObjectId(((BComponent)this.getTrendLogExt()).getHandleOrd());
            if (objectId == null) {
               BComplex parent = ((BComponent)this.getTrendLogExt()).getParent();
               BControlPoint parentPoint = parent instanceof BControlPoint ? (BControlPoint)parent : null;
               if (parentPoint != null) {
                  objectId = BBacnetNetwork.localDevice().lookupBacnetObjectId(parentPoint.getHandleOrd());
               }

               if (objectId == null) {
                  logger.severe("Could not send the alarm as the object is not exported as a BACnet object.");
                  return false;
               }
            }

            BBacnetDeviceObjectPropertyReference opr = new BBacnetDeviceObjectPropertyReference(objectId, 131);
            map.put("presentValue", BString.make(opr.encodeToString()));
            map.put("fromState", BString.make(this.getAlarmState().getTag()));
            map.put("toState", BString.make(BAlarmState.normal.getTag()));
            map.put("msgText", BString.make(msgText));
            map.put("hyperlinkOrd", BString.make(ord.toString()));
            map.put("notifyType", BString.make(this.getNotifyType().getTag()));
            map.put("previousNotification", BString.make(String.valueOf(previousNotification)));
            map.put("currentNotification", BString.make(String.valueOf(currentNotification)));
         } catch (Exception var16) {
            logger.log(Level.SEVERE, "Buffer-Ready toNormal Transition Failed - Could not build alarm record facets.", (Throwable)var16);
            return false;
         }

         try {
            BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
            BAlarmClass ac = as.lookupAlarmClass(this.getAlarmClass());
            BAlarmRecord alarm = new BAlarmRecord(this, this.getAlarmClass(), BFacets.make(map), BUuid.make());
            alarm.setSourceState(BSourceState.normal);
            boolean ackRequired = ac.getAckRequired().includes(BSourceState.normal);
            alarm.setAckRequired(ackRequired);
            if (ackRequired) {
               BAlarmTransitionBits ackedTrans = this.getAckedTransitions();
               this.setAckedTransitions(BAlarmTransitionBits.make(ackedTrans, BAlarmTransitionBits.toNormal, false));
            }

            BAlarmTimestamps alarmTimes = (BAlarmTimestamps)this.get(alarmTimesProp);
            BAbsTime timestamp = alarm.getTimestamp();
            alarmTimes.setAlarmTime(timestamp);
            alarmTimes.setNormalTime(timestamp);
            alarm.setNormalTime(timestamp);
            alarmTimes.setAckTime(BAbsTime.DEFAULT);
            alarmTimes.setCount(alarmTimes.getCount() + 1);
            as.routeAlarm(alarm);
            return true;
         } catch (ServiceNotFoundException var15) {
            logger.severe("Buffer-Ready toNormal Transition Failed - AlarmService not found.");
            return false;
         }
      }
   }

   public BFormat getToFaultText() {
      return BFormat.DEFAULT;
   }

   public BFormat getToOffnormalText() {
      return BFormat.DEFAULT;
   }

   private static class SkipCheckBufferContext extends BasicContext {
      public SkipCheckBufferContext(Context base) {
         super(base);
      }
   }
}
