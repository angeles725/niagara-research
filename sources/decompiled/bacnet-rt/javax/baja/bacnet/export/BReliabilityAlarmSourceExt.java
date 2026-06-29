package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmSupport;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmState;
import javax.baja.alarm.ext.BAlarmTimestamps;
import javax.baja.alarm.ext.BIAlarmMessages;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.control.BPointExtension;
import javax.baja.data.BIDataValue;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BFormat;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "alarmInhibit",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean(false)"
   ), @NiagaraProperty(
      name = "alarmState",
      type = "BAlarmState",
      defaultValue = "BAlarmState.normal",
      flags = 65
   ), @NiagaraProperty(
      name = "timeDelay",
      type = "BRelTime",
      defaultValue = "BRelTime.DEFAULT",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(0)"
      )}
   ), @NiagaraProperty(
      name = "alarmEnable",
      type = "BAlarmTransitionBits",
      defaultValue = "BAlarmTransitionBits.DEFAULT",
      facets = {@Facet("BFacets.make(\"showOffNormal\", false)"), @Facet("BFacets.make(\"showAlert\", false)")}
   ), @NiagaraProperty(
      name = "ackedTransitions",
      type = "BAlarmTransitionBits",
      defaultValue = "BAlarmTransitionBits.ALL",
      flags = 7
   ), @NiagaraProperty(
      name = "notifyType",
      type = "BBacnetNotifyType",
      defaultValue = "BBacnetNotifyType.alarm",
      facets = {@Facet("BacUtil.makeBacnetNotifyTypeFacets()")}
   ), @NiagaraProperty(
      name = "toFaultTimes",
      type = "BAlarmTimestamps",
      defaultValue = "new BAlarmTimestamps()",
      flags = 3
   ), @NiagaraProperty(
      name = "toFaultText",
      type = "BFormat",
      defaultValue = "BFormat.make(\"\")",
      facets = {@Facet(
         name = "BFacets.MULTI_LINE",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "toNormalText",
      type = "BFormat",
      defaultValue = "BFormat.make(\"\")",
      facets = {@Facet(
         name = "BFacets.MULTI_LINE",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "hyperlinkOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      facets = {@Facet(
         name = "BFacets.ORD_RELATIVIZE",
         value = "false"
      ), @Facet("BFacets.make(\"chooseView\", true)")}
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
   ), @NiagaraProperty(
      name = "reliability",
      type = "BBacnetReliability",
      defaultValue = "BBacnetReliability.noFaultDetected"
   )})
@NiagaraActions({@NiagaraAction(
      name = "ackAlarm",
      parameterType = "BAlarmRecord",
      defaultValue = "new BAlarmRecord()",
      returnType = "BBoolean",
      flags = 4
   ), @NiagaraAction(
      name = "timerElapsed",
      parameterType = "BBacnetReliability",
      defaultValue = "BBacnetReliability.noFaultDetected",
      flags = 4
   )})
public class BReliabilityAlarmSourceExt extends BPointExtension implements BIAlarmSource, BIAlarmMessages, BacnetAlarmConst {
   public static final Property alarmInhibit = newProperty(0, new BStatusBoolean(false), null);
   public static final Property alarmState = newProperty(65, BAlarmState.normal, null);
   public static final Property timeDelay = newProperty(0, BRelTime.DEFAULT, BFacets.make("min", BRelTime.make(0L)));
   public static final Property alarmEnable = newProperty(
      0, BAlarmTransitionBits.DEFAULT, BFacets.make(BFacets.make("showOffNormal", false), BFacets.make("showAlert", false))
   );
   public static final Property ackedTransitions = newProperty(7, BAlarmTransitionBits.ALL, null);
   public static final Property notifyType = newProperty(0, BBacnetNotifyType.alarm, BacUtil.makeBacnetNotifyTypeFacets());
   public static final Property toFaultTimes = newProperty(3, new BAlarmTimestamps(), null);
   public static final Property toFaultText = newProperty(0, BFormat.make(""), BFacets.make("multiLine", true));
   public static final Property toNormalText = newProperty(0, BFormat.make(""), BFacets.make("multiLine", true));
   public static final Property hyperlinkOrd = newProperty(0, BOrd.NULL, BFacets.make(BFacets.make("ordRelativize", false), BFacets.make("chooseView", true)));
   public static final Property alarmClass = newProperty(
      0, "defaultAlarmClass", BFacets.make(BFacets.make("fieldEditor", "alarm:AlarmClassFE"), BFacets.make("uxFieldEditor", "alarm:AlarmClassEditor"))
   );
   public static final Property reliability = newProperty(0, BBacnetReliability.noFaultDetected, null);
   public static final Action ackAlarm = newAction(4, new BAlarmRecord(), null);
   public static final Action timerElapsed = newAction(4, BBacnetReliability.noFaultDetected, null);
   public static final Type TYPE = Sys.loadType(BReliabilityAlarmSourceExt.class);
   long endTime;
   Ticket ticket;
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private BBacnetReliability reliabilityStored;
   private AlarmSupport support;

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

   public BRelTime getTimeDelay() {
      return (BRelTime)this.get(timeDelay);
   }

   public void setTimeDelay(BRelTime v) {
      this.set(timeDelay, v, null);
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

   public BAlarmTimestamps getToFaultTimes() {
      return (BAlarmTimestamps)this.get(toFaultTimes);
   }

   public void setToFaultTimes(BAlarmTimestamps v) {
      this.set(toFaultTimes, v, null);
   }

   public BFormat getToFaultText() {
      return (BFormat)this.get(toFaultText);
   }

   public void setToFaultText(BFormat v) {
      this.set(toFaultText, v, null);
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

   public BBacnetReliability getReliability() {
      return (BBacnetReliability)this.get(reliability);
   }

   public void setReliability(BBacnetReliability v) {
      this.set(reliability, v, null);
   }

   public BBoolean ackAlarm(BAlarmRecord parameter) {
      return (BBoolean)this.invoke(ackAlarm, parameter, null);
   }

   public void timerElapsed(BBacnetReliability parameter) {
      this.invoke(timerElapsed, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BReliabilityAlarmSourceExt() {
      this.cancelTimer();
   }

   public final void started() throws Exception {
      super.started();
      this.cancelTimer();
      this.support = new AlarmSupport(this, "");
      if (this.reliabilityStored != null) {
         if (!this.reliabilityStored.equals(this.getReliability())) {
            this.reliabilityChanged(this.reliabilityStored);
         }

         this.reliabilityStored = null;
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      this.cancelTimer();
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      this.cancelTimer();
   }

   public BBoolean doAckAlarm(BAlarmRecord ackRequest) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Acknowledging the alarm " + ackRequest.getSourceState());
      }

      try {
         this.updateAlarmTimeProps(ackRequest, false, true);
         return BBoolean.make(this.support.ackAlarm(ackRequest));
      } catch (Exception var3) {
         logger.severe("Unable to acknowledge the alarm: " + var3);
         return BBoolean.make(false);
      }
   }

   public void doTimerElapsed(BBacnetReliability reliability) {
      if (this.isTimerExpired()) {
         this.alarmProcessing(reliability);
      }
   }

   public void alarmProcessing(BBacnetReliability reliability) {
      this.setReliability(reliability);

      try {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Reliability has changed to " + reliability);
         }

         if (reliability == BBacnetReliability.noFaultDetected) {
            HashMap<String, BIDataValue> map = this.getAlarmFacet(reliability, true);
            this.support.toNormal(BFacets.make(map), null);
            this.setAlarmState(BAlarmState.normal);
            this.updateAlarmTimeProps(null, true);
         } else {
            HashMap<String, BIDataValue> map = this.getAlarmFacet(reliability, false);
            BAlarmRecord alarmRecord = this.support.newFaultAlarm(BFacets.make(map));
            this.setAlarmState(BAlarmState.fault);
            this.updateAlarmTimeProps(alarmRecord, false);
         }
      } catch (Exception var4) {
         logger.severe("Unable to process tha alarm for Reliability changed: " + var4);
      }
   }

   public void reliabilityChanged(BBacnetReliability reliability) {
      if (!this.isRunning()) {
         this.reliabilityStored = reliability;
      } else if (this.getAlarmInhibit().getBoolean()) {
         logger.warning("Alarm inhibit is enabled, alarm cannot be processed.");
      } else {
         if (this.getTimeDelay().getMillis() > 0L) {
            this.startTimer(this.getTimeDelay().getMillis(), reliability);
         } else {
            this.alarmProcessing(reliability);
         }
      }
   }

   private void updateAlarmTimeProps(BAlarmRecord alarm, boolean isNormal) {
      this.updateAlarmTimeProps(alarm, isNormal, false);
   }

   private void updateAlarmTimeProps(BAlarmRecord alarm, boolean isNormal, boolean isAck) {
      Property alarmTimesProp = toFaultTimes;
      BAlarmTimestamps alarmTimes = (BAlarmTimestamps)this.get(alarmTimesProp);
      if (isNormal) {
         alarmTimes.setNormalTime(BAbsTime.now());
      } else {
         alarmTimes.setAlarmTime(alarm.getTimestamp());
         alarmTimes.setCount(alarmTimes.getCount() + 1);
      }

      if (isAck) {
         alarmTimes.setAckTime(alarm.getAckTime());
      }
   }

   private HashMap<String, BIDataValue> getAlarmFacet(BBacnetReliability reliability, boolean isNormalAlarm) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      BOrd ord = this.getHyperlinkOrd();
      map.put("alarmValue", BString.make(reliability.getTag()));
      map.put("fromState", BString.make(this.getAlarmState().getTag()));
      map.put("toState", BString.make(isNormalAlarm ? BAlarmState.normal.getTag() : BAlarmState.fault.getTag()));
      map.put("msgText", BString.make(isNormalAlarm ? this.getToNormalText().getFormat() : this.getToFaultText().getFormat()));
      map.put("hyperlinkOrd", BString.make(ord.toString()));
      map.put("notifyType", BString.make(this.getNotifyType().getTag()));
      return map;
   }

   public void onExecute(BStatusValue out, Context cx) {
   }

   public BFormat getToOffnormalText() {
      return null;
   }

   protected void startTimer(long timeDelay, BBacnetReliability reliability) {
      this.endTime = Clock.ticks() + timeDelay;
      if (this.isRunning()) {
         this.ticket = Clock.schedule(this, BRelTime.make(timeDelay), timerElapsed, reliability);
      }
   }

   protected void cancelTimer() {
      this.endTime = -1L;
      if (this.ticket != null) {
         this.ticket.cancel();
      }
   }

   protected boolean isTimerExpired() {
      long now = Clock.ticks();
      if (this.endTime == -1L) {
         throw new IllegalStateException();
      } else {
         return now >= this.endTime;
      }
   }
}
