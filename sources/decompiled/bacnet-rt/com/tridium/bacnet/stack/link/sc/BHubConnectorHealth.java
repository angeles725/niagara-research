package com.tridium.bacnet.stack.link.sc;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmSupport;
import javax.baja.alarm.BAckState;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmSourceInfo;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.BSourceState;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFormat;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "alarmOnFailure",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "startupAlarmDelay",
      type = "BRelTime",
      defaultValue = "BRelTime.make(5L*60L*1000L)"
   ), @NiagaraProperty(
      name = "primaryFailuresBeforeAlarm",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(null, 0, Integer.MAX_VALUE)")}
   ), @NiagaraProperty(
      name = "primaryFailureCount",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "alarmSourceInfo",
      type = "BAlarmSourceInfo",
      defaultValue = "initAlarmSourceInfo()"
   )})
@NiagaraAction(
   name = "ackAlarm",
   parameterType = "BAlarmRecord",
   defaultValue = "new BAlarmRecord()",
   returnType = "BBoolean",
   flags = 4
)
public final class BHubConnectorHealth extends BComponent implements BIAlarmSource {
   public static final Property alarmOnFailure = newProperty(0, true, null);
   public static final Property startupAlarmDelay = newProperty(0, BRelTime.make(300000L), null);
   public static final Property primaryFailuresBeforeAlarm = newProperty(0, 0, BFacets.makeInt(null, 0, Integer.MAX_VALUE));
   public static final Property primaryFailureCount = newProperty(3, 0, null);
   public static final Property alarmSourceInfo = newProperty(0, initAlarmSourceInfo(), null);
   public static final Action ackAlarm = newAction(4, new BAlarmRecord(), null);
   public static final Type TYPE = Sys.loadType(BHubConnectorHealth.class);
   private static final Logger logger = Logger.getLogger("bacnet.sc.hubConnector");
   private static final int HUB_CONNECTOR_INITIAL_STATE = 0;
   private static final int HUB_CONNECTOR_NORMAL_STATE = 1;
   private static final int HUB_CONNECTOR_OFFNORMAL_STATE = 2;
   private static final int HUB_CONNECTOR_FAULT_STATE = 3;
   private BHubConnector hubConnector;
   private AlarmSupport alarmSupport;
   private long startupTicks;
   private boolean delayExpired;
   private BAlarmRecord currentAlarmRecord;
   private int hubConnectorState = 0;

   public boolean getAlarmOnFailure() {
      return this.getBoolean(alarmOnFailure);
   }

   public void setAlarmOnFailure(boolean v) {
      this.setBoolean(alarmOnFailure, v, null);
   }

   public BRelTime getStartupAlarmDelay() {
      return (BRelTime)this.get(startupAlarmDelay);
   }

   public void setStartupAlarmDelay(BRelTime v) {
      this.set(startupAlarmDelay, v, null);
   }

   public int getPrimaryFailuresBeforeAlarm() {
      return this.getInt(primaryFailuresBeforeAlarm);
   }

   public void setPrimaryFailuresBeforeAlarm(int v) {
      this.setInt(primaryFailuresBeforeAlarm, v, null);
   }

   public int getPrimaryFailureCount() {
      return this.getInt(primaryFailureCount);
   }

   public void setPrimaryFailureCount(int v) {
      this.setInt(primaryFailureCount, v, null);
   }

   public BAlarmSourceInfo getAlarmSourceInfo() {
      return (BAlarmSourceInfo)this.get(alarmSourceInfo);
   }

   public void setAlarmSourceInfo(BAlarmSourceInfo v) {
      this.set(alarmSourceInfo, v, null);
   }

   public BBoolean ackAlarm(BAlarmRecord parameter) {
      return (BBoolean)this.invoke(ackAlarm, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BHubConnectorHealth make() {
      return new BHubConnectorHealth();
   }

   public void started() throws Exception {
      super.started();
      this.hubConnector = (BHubConnector)this.getParent();
      this.alarmSupport = new AlarmSupport(this, this.getAlarmSourceInfo());
      if (Sys.atSteadyState()) {
         this.startupTicks = Clock.ticks();
      }
   }

   public void atSteadyState() throws Exception {
      super.atSteadyState();
      this.startupTicks = Clock.ticks();
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(alarmOnFailure)) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("HubConnectorHealth#changed; alarmOnFailure: " + this.getAlarmOnFailure());
            }

            if (!this.getAlarmOnFailure()) {
               this.processAlarmNormal();
            }
         }
      }
   }

   void primaryConnectionActivated() {
      logger.fine("HubConnectorHealth#primaryConnectionActivated");
      this.setPrimaryFailureCount(0);
      if (this.getAlarmOnFailure()) {
         this.processAlarmNormal();
      }
   }

   void primaryConnectionFailed() {
      this.incrementPrimaryFailureCount();
      boolean delayExpired = this.delayHasExpired();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "HubConnectorHealth#primaryConnectionFailed; primaryFailureCount: "
               + this.getPrimaryFailureCount()
               + "; primaryFailuresBeforeAlarm: "
               + this.getPrimaryFailuresBeforeAlarm()
               + "; alarmOnFailure: "
               + this.getAlarmOnFailure()
               + "; delayExpired: "
               + delayExpired
               + "; hubConnectorState: "
               + this.getHubConnectorState()
         );
      }

      if (this.getAlarmOnFailure() && delayExpired && this.hasTooManyPrimaryFailures()) {
         if (!this.hubConnector.getFailoverConnection().getEnabled()) {
            this.processAlarmFault();
         } else if (this.isInitialState() || this.isNormalState()) {
            this.processAlarmOffnormal();
         }
      }
   }

   void primaryConnectionDisabled() {
      this.incrementPrimaryFailureCount();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "HubConnectorHealth#primaryConnectionDisabled; primaryFailureCount: "
               + this.getPrimaryFailureCount()
               + "; alarmOnFailure: "
               + this.getAlarmOnFailure()
               + "; hubConnectorState: "
               + this.getHubConnectorState()
         );
      }

      if (this.getAlarmOnFailure()) {
         if (!this.hubConnector.getFailoverConnection().getEnabled()) {
            this.processAlarmFault();
         } else if (this.isInitialState() || this.isNormalState()) {
            this.processAlarmOffnormal();
         }
      }
   }

   void failoverConnectionActivated() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "HubConnectorHealth#failoverConnectionActivated; alarmOnFailure: "
               + this.getAlarmOnFailure()
               + "; hubConnectorState: "
               + this.getHubConnectorState()
         );
      }

      if (this.getAlarmOnFailure() && this.isFaultState()) {
         this.processAlarmOffnormal();
      }
   }

   void failoverConnectionFailed() {
      boolean delayExpired = this.delayHasExpired();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "HubConnectorHealth#failoverConnectionFailed; primaryFailureCount: "
               + this.getPrimaryFailureCount()
               + "; primaryFailuresBeforeAlarm: "
               + this.getPrimaryFailuresBeforeAlarm()
               + "; alarmOnFailure: "
               + this.getAlarmOnFailure()
               + "; delayExpired: "
               + delayExpired
         );
      }

      if (this.getAlarmOnFailure() && (delayExpired && this.hasTooManyPrimaryFailures() || !this.hubConnector.getPrimaryConnection().getEnabled())) {
         this.processAlarmFault();
      }
   }

   private void incrementPrimaryFailureCount() {
      if (this.getPrimaryFailureCount() < Integer.MAX_VALUE) {
         this.setPrimaryFailureCount(this.getPrimaryFailureCount() + 1);
      }
   }

   private boolean hasTooManyPrimaryFailures() {
      return this.getPrimaryFailureCount() > this.getPrimaryFailuresBeforeAlarm();
   }

   private boolean delayHasExpired() {
      if (!Sys.atSteadyState()) {
         return false;
      } else {
         if (!this.delayExpired) {
            long delay = this.getStartupAlarmDelay().getMillis();
            this.delayExpired = Clock.ticks() - this.startupTicks > delay;
         }

         return this.delayExpired;
      }
   }

   private static BAlarmSourceInfo initAlarmSourceInfo() {
      BAlarmSourceInfo alarmSourceInfo = new BAlarmSourceInfo();
      alarmSourceInfo.setSourceName(BFormat.make("%parent.parent.displayName% %parent.displayName%"));
      alarmSourceInfo.setToNormalText(BFormat.make("%lexicon(bacnet:hubConnector.status.normal)%"));
      alarmSourceInfo.setToOffnormalText(BFormat.make("%lexicon(bacnet:hubConnector.status.offnormal)%"));
      alarmSourceInfo.setToFaultText(BFormat.make("%lexicon(bacnet:hubConnector.status.fault)%"));
      return alarmSourceInfo;
   }

   public BBoolean doAckAlarm(BAlarmRecord ackRequest) {
      if (!this.isRunning()) {
         return BBoolean.FALSE;
      } else {
         BBoolean alarmAck = BBoolean.FALSE;

         try {
            alarmAck = BBoolean.make(this.alarmSupport.ackAlarm(ackRequest));
         } catch (Throwable var4) {
            logger.log(Level.WARNING, "Failed to ack alarm for " + this.alarmSupport.getSourceOrd(), var4);
         }

         if (logger.isLoggable(Level.FINE)) {
            logger.fine("HubConnectorHealth#doAckAlarm; alarmAck: " + alarmAck);
         }

         if (alarmAck.getBoolean()) {
            this.hubConnector.setStatus(BStatus.make(this.hubConnector.getStatus(), 128, false));
         }

         return alarmAck;
      }
   }

   private void processAlarmNormal() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("HubConnectorHealth#processAlarmNormal; old hubConnectorState: " + this.getHubConnectorState());
      }

      if (!this.isInitialState() && !this.isNormalState()) {
         try {
            this.alarmSupport.toNormal(null);
            this.hubConnectorState = 1;
            this.currentAlarmRecord = null;
            this.resetAlarmStatus();
         } catch (Throwable var2) {
            logger.log(Level.WARNING, "Failed to send toNormal alarm for " + this.alarmSupport.getSourceOrd(), var2);
         }
      }
   }

   private void processAlarmOffnormal() {
      boolean ackRequired = this.alarmSupport.isAckRequired(BSourceState.offnormal);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "HubConnectorHealth#processAlarmOffNormal; old hubConnectorState: "
               + this.getHubConnectorState()
               + "; ackRequired: "
               + ackRequired
               + "; currentAlarmRecord: "
               + this.currentAlarmRecord
         );
      }

      try {
         switch (this.hubConnectorState) {
            case 0:
            case 1:
               BFacets alarmData = this.getAlarmSourceInfo().makeAlarmData(BSourceState.offnormal);
               this.currentAlarmRecord = this.alarmSupport.newOffnormalAlarm(alarmData);
            case 2:
            default:
               break;
            case 3:
               if (this.currentAlarmRecord == null) {
                  BFacets alarmDatax = this.getAlarmSourceInfo().makeAlarmData(BSourceState.offnormal);
                  this.currentAlarmRecord = this.alarmSupport.newOffnormalAlarm(alarmDatax);
               } else {
                  this.updateCurrentAlarmRecord(BSourceState.offnormal, ackRequired);
                  BAlarmService.getService().doRouteAlarm(this.currentAlarmRecord);
               }
         }

         this.hubConnectorState = 2;
         this.setAlarmStatus(ackRequired);
      } catch (Throwable var3) {
         logger.log(Level.WARNING, "Failed to send offNormal alarm for " + this.alarmSupport.getSourceOrd(), var3);
      }
   }

   private void processAlarmFault() {
      boolean ackRequired = this.alarmSupport.isAckRequired(BSourceState.fault);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "HubConnectorHealth#processAlarmFault; old hubConnectorState: "
               + this.getHubConnectorState()
               + "; ackRequired: "
               + ackRequired
               + "; currentAlarmRecord: "
               + this.currentAlarmRecord
         );
      }

      try {
         switch (this.hubConnectorState) {
            case 0:
            case 1:
               BFacets alarmData = this.getAlarmSourceInfo().makeAlarmData(BSourceState.fault);
               this.currentAlarmRecord = this.alarmSupport.newFaultAlarm(alarmData);
               break;
            case 2:
               if (this.currentAlarmRecord == null) {
                  BFacets alarmDatax = this.getAlarmSourceInfo().makeAlarmData(BSourceState.fault);
                  this.currentAlarmRecord = this.alarmSupport.newFaultAlarm(alarmDatax);
               } else {
                  this.updateCurrentAlarmRecord(BSourceState.fault, ackRequired);
                  BAlarmService.getService().doRouteAlarm(this.currentAlarmRecord);
               }
         }

         this.hubConnectorState = 3;
         this.setAlarmStatus(ackRequired);
      } catch (Throwable var3) {
         logger.log(Level.WARNING, "Failed to send fault alarm for " + this.alarmSupport.getSourceOrd(), var3);
      }
   }

   private void updateCurrentAlarmRecord(BSourceState state, boolean ackRequired) {
      if (ackRequired) {
         this.currentAlarmRecord.setAckState(BAckState.unacked);
      }

      String messageText = "";
      switch (state.getOrdinal()) {
         case 1:
            messageText = this.getAlarmSourceInfo().getToOffnormalText().toString();
            break;
         case 2:
            messageText = this.getAlarmSourceInfo().getToFaultText().toString();
      }

      BFacets alarmData = BFacets.make(this.currentAlarmRecord.getAlarmData(), "msgText", BString.make(messageText));
      this.currentAlarmRecord.setSourceState(state);
      this.currentAlarmRecord.setAckRequired(ackRequired);
      this.currentAlarmRecord.setAlarmData(alarmData);
   }

   private void setAlarmStatus(boolean ackRequired) {
      int newStatus = this.hubConnector.getStatus().getBits() | 8;
      if (ackRequired) {
         newStatus |= 128;
      }

      this.hubConnector.setStatus(BStatus.make(newStatus));
   }

   private void resetAlarmStatus() {
      this.hubConnector.setStatus(BStatus.make(this.hubConnector.getStatus(), 8, false));
   }

   private boolean isInitialState() {
      return this.hubConnectorState == 0;
   }

   private boolean isNormalState() {
      return this.hubConnectorState == 1;
   }

   private boolean isFaultState() {
      return this.hubConnectorState == 3;
   }

   private String getHubConnectorState() {
      switch (this.hubConnectorState) {
         case 0:
            return "Initial";
         case 1:
            return "Normal";
         case 2:
            return "Offnormal";
         case 3:
            return "Fault";
         default:
            return "Unknown";
      }
   }
}
