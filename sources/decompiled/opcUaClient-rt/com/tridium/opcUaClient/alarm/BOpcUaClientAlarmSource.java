package com.tridium.opcUaClient.alarm;

import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.opcUaClient.BOpcUaDevice;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmSupport;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmSourceInfo;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.driver.BDevice;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 75,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "uaNodeId",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "subscribed",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "lastEvent",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "alarmSourceInfo",
      type = "BAlarmSourceInfo",
      defaultValue = "new BAlarmSourceInfo()"
   ), @NiagaraProperty(
      name = "lastSubscribed",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL"
   )})
@NiagaraActions({@NiagaraAction(
      name = "subscribeForEvents",
      flags = 16
   ), @NiagaraAction(
      name = "unsubscribeForEvents",
      flags = 16
   )})
public class BOpcUaClientAlarmSource extends BComponent implements BIAlarmSource, BIStatus {
   public static final Property status = newProperty(75, BStatus.ok, SfUtil.incl());
   public static final Property enabled = newProperty(0, true, null);
   public static final Property uaNodeId = newProperty(0, "", SfUtil.incl());
   public static final Property subscribed = newProperty(0, false, SfUtil.incl());
   public static final Property lastEvent = newProperty(0, "", SfUtil.incl());
   public static final Property alarmSourceInfo = newProperty(0, new BAlarmSourceInfo(), null);
   public static final Property lastSubscribed = newProperty(0, BAbsTime.NULL, null);
   public static final Action subscribeForEvents = newAction(16, null);
   public static final Action unsubscribeForEvents = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientAlarmSource.class);
   private AlarmSupport alarmSupport;
   private int oldStatus = 0;
   private BOpcUaDevice device;
   private BOpcUaClientAlarmDeviceExt alarmDeviceExt;
   private NodeId nodeId;
   private static final Logger logger = Logger.getLogger("opcUaClient.alarm");

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public String getUaNodeId() {
      return this.getString(uaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   public boolean getSubscribed() {
      return this.getBoolean(subscribed);
   }

   public void setSubscribed(boolean v) {
      this.setBoolean(subscribed, v, null);
   }

   public String getLastEvent() {
      return this.getString(lastEvent);
   }

   public void setLastEvent(String v) {
      this.setString(lastEvent, v, null);
   }

   public BAlarmSourceInfo getAlarmSourceInfo() {
      return (BAlarmSourceInfo)this.get(alarmSourceInfo);
   }

   public void setAlarmSourceInfo(BAlarmSourceInfo v) {
      this.set(alarmSourceInfo, v, null);
   }

   public BAbsTime getLastSubscribed() {
      return (BAbsTime)this.get(lastSubscribed);
   }

   public void setLastSubscribed(BAbsTime v) {
      this.set(lastSubscribed, v, null);
   }

   public void subscribeForEvents() {
      this.invoke(subscribeForEvents, null, null);
   }

   public void unsubscribeForEvents() {
      this.invoke(unsubscribeForEvents, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.alarmSupport = new AlarmSupport(this, this.getAlarmSourceInfo());
      this.device = this.getDevice();
      this.alarmDeviceExt = this.getAlarmDeviceExt();
      this.nodeId = NodeId.parseNodeId(this.getUaNodeId());
      if (this.needsSubscription()) {
         this.doSubscribeForEvents();
         this.setLastSubscribed(BAbsTime.now());
      }
   }

   public void stopped() throws Exception {
      super.stopped();
      this.doUnsubscribeForEvents();
   }

   public AlarmSupport getAlarmSupport() {
      return this.alarmSupport;
   }

   public void changed(Property p, Context cx) {
      if (this.isRunning() && !Context.decoding.equals(cx)) {
         if (p.equals(uaNodeId)) {
            this.nodeId = NodeId.parseNodeId(this.getUaNodeId());
         }

         if (p.equals(enabled)) {
            this.updateEventAndStatus(this.getEnabled());
         } else if (p.equals(subscribed)) {
            this.updateStatus();
         }
      } else {
         super.changed(p, cx);
      }
   }

   private void updateEventAndStatus(boolean isSubscribeForEvents) {
      if (isSubscribeForEvents) {
         this.doSubscribeForEvents();
      } else {
         this.doUnsubscribeForEvents();
      }

      this.updateStatus();
   }

   public BOpcUaDevice getDevice() {
      if (this.device != null) {
         return this.device;
      } else {
         BDevice dvc = this.getAlarmDeviceExt().getDevice();
         if (dvc instanceof BOpcUaDevice) {
            this.device = (BOpcUaDevice)dvc;
         }

         return this.device;
      }
   }

   public BOpcUaClientAlarmDeviceExt getAlarmDeviceExt() {
      BComplex parent = this.getParent();
      return parent instanceof BOpcUaClientAlarmDeviceExt ? (BOpcUaClientAlarmDeviceExt)parent : null;
   }

   public void doSubscribeForEvents() {
      if (this.alarmDeviceExt != null) {
         try {
            this.alarmDeviceExt.addMonitorEvent(this.nodeId);
            this.setSubscribed(this.alarmDeviceExt.isEventSubscribed(this.nodeId));
         } catch (Exception var2) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.SEVERE, "Exception occurred when subscribing to an alarm event", (Throwable)var2);
            } else {
               logger.log(Level.SEVERE, "Exception occurred when subscribing to an alarm event: " + var2);
            }
         }
      }
   }

   public void doUnsubscribeForEvents() {
      try {
         this.alarmDeviceExt.removeMonitorEvent(this.nodeId);
         this.setSubscribed(this.alarmDeviceExt.isEventSubscribed(this.nodeId));
      } catch (Exception var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred when unsubscribing to an alarm event", (Throwable)var2);
         } else {
            logger.log(Level.SEVERE, "Exception occurred when unsubscribing to an alarm event: " + var2);
         }
      }
   }

   public BBoolean ackAlarm(BAlarmRecord ackRequest) {
      try {
         return this.getAlarmDeviceExt().doAckAlarm(ackRequest);
      } catch (Exception var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred when acknowledging an alarm event", (Throwable)var3);
         } else {
            logger.log(Level.SEVERE, "Exception occurred when acknowledging an alarm event: " + var3);
         }

         return BBoolean.FALSE;
      }
   }

   public void setLastEvent(BOpcUaClientAlarmEntry lastEvent) {
      this.setLastEvent(BAbsTime.now() + " " + lastEvent.getAlarmMessage());
   }

   public void updateStatus() {
      int newStatus = this.getStatus().getBits();
      BStatus device = this.device == null ? BStatus.ok : this.device.getStatus();
      if (this.getEnabled() && !device.isDisabled()) {
         newStatus &= -2;
      } else {
         newStatus |= 1;
      }

      if (device.isDown()) {
         newStatus |= 4;
      } else {
         newStatus &= -5;
      }

      if (this.oldStatus != newStatus) {
         this.setStatus(BStatus.make(newStatus));
         this.oldStatus = newStatus;
      }
   }

   private boolean needsSubscription() {
      boolean subscribed = this.getSubscribed();
      if (!subscribed && this.getLastSubscribed() == BAbsTime.NULL) {
         BOpcUaClientAlarmDeviceExt alarmExt = this.device.getAlarmExt();
         subscribed = alarmExt.getAutoSubscribeEnable();
      }

      return subscribed;
   }
}
